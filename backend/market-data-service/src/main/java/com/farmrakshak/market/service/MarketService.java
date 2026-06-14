package com.farmrakshak.market.service;

import com.farmrakshak.market.config.CropServiceClient;
import com.farmrakshak.market.dto.*;
import com.farmrakshak.market.entity.*;
import com.farmrakshak.market.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Main market intelligence service — orchestrates all sub-services.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MarketService {

    private final CropServiceClient cropServiceClient;
    private final MandiLocatorService mandiLocatorService;
    private final TrendAnalysisService trendAnalysisService;
    private final RecommendationEngine recommendationEngine;
    private final PricePredictionEngine predictionEngine;
    private final CropPriceRepository cropPriceRepository;
    private final CropMasterRepository cropMasterRepository;
    private final MandiRepository mandiRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String CACHE_PREFIX = "market:";

    /**
     * Get prices for all crops in user's farms.
     */
    public List<MyCropPricesResponse> getMyCropPrices(UUID userId) {
        List<UserCropInfo> userCrops = cropServiceClient.getUserCrops(userId);
        if (userCrops.isEmpty()) return List.of();

        // Deduplicate by crop name (user may have same crop in multiple farms)
        Map<String, UserCropInfo> uniqueCrops = new LinkedHashMap<>();
        for (UserCropInfo crop : userCrops) {
            uniqueCrops.putIfAbsent(crop.getCropName().toLowerCase(), crop);
        }

        return uniqueCrops.values().stream()
                .map(crop -> buildCropPrices(crop, true))
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Search crop prices — works for any crop, not just user's.
     */
    public MyCropPricesResponse searchCropPrices(String cropName, BigDecimal lat, BigDecimal lon, UUID userId) {
        // Resolve crop name via synonyms
        String resolvedName = resolveCropName(cropName);

        UserCropInfo virtualCrop = UserCropInfo.builder()
                .cropName(resolvedName)
                .latitude(lat)
                .longitude(lon)
                .build();

        MyCropPricesResponse response = buildCropPrices(virtualCrop, false);
        if (response == null) return null;

        // Check if crop is in user's farms
        if (userId != null) {
            List<UserCropInfo> userCrops = cropServiceClient.getUserCrops(userId);
            for (UserCropInfo uc : userCrops) {
                if (uc.getCropName().equalsIgnoreCase(resolvedName)) {
                    response.setInUserFarm(true);
                    response.setFarmName(uc.getFarmName());
                    break;
                }
            }
        }

        return response;
    }

    /**
     * Get crop search suggestions with farm ownership status.
     */
    public List<CropSearchResponse> searchCrops(String query, UUID userId) {
        List<CropMasterEntity> results = cropMasterRepository.fuzzySearch(query);
        List<UserCropInfo> userCrops = userId != null ? cropServiceClient.getUserCrops(userId) : List.of();
        Set<String> userCropNames = userCrops.stream()
                .map(c -> c.getCropName().toLowerCase())
                .collect(Collectors.toSet());

        return results.stream().map(cm -> {
            boolean inFarm = userCropNames.contains(cm.getCropName().toLowerCase());
            String farmName = null;
            if (inFarm) {
                farmName = userCrops.stream()
                        .filter(uc -> uc.getCropName().equalsIgnoreCase(cm.getCropName()))
                        .findFirst()
                        .map(UserCropInfo::getFarmName)
                        .orElse(null);
            }

            return CropSearchResponse.builder()
                    .cropName(cm.getCropName())
                    .category(cm.getCategory())
                    .unit(cm.getUnit())
                    .scientificName(cm.getScientificName())
                    .localNames(cm.getLocalNames() != null ? Arrays.asList(cm.getLocalNames()) : List.of())
                    .inUserFarm(inFarm)
                    .farmName(farmName)
                    .build();
        }).toList();
    }

    /**
     * Resolve crop name from synonyms/local names to canonical name.
     */
    public String resolveCropName(String input) {
        // First try exact match
        Optional<CropMasterEntity> exact = cropMasterRepository.findByCropNameIgnoreCase(input);
        if (exact.isPresent()) return exact.get().getCropName();

        // Then try synonyms
        List<CropMasterEntity> matches = cropMasterRepository.searchByCropNameOrSynonym(input);
        if (!matches.isEmpty()) return matches.get(0).getCropName();

        // Fallback to input
        return input;
    }

    private MyCropPricesResponse buildCropPrices(UserCropInfo crop, boolean isUserCrop) {
        LocalDate today = LocalDate.now();
        String cropName = crop.getCropName();

        // Find nearest mandis if location available
        List<MandiResponse> nearestMandis = mandiLocatorService.findNearest(
                crop.getLatitude(), crop.getLongitude(), 10);

        List<UUID> mandiIds = nearestMandis.stream()
                .map(m -> UUID.fromString(m.getId()))
                .toList();

        // Try today, then yesterday, then 2 days ago (data may be delayed)
        List<CropPrice> prices = List.of();
        LocalDate priceDate = today;
        for (int i = 0; i < 3; i++) {
            priceDate = today.minusDays(i);
            if (mandiIds.isEmpty()) {
                prices = cropPriceRepository.findByCropAndDate(cropName, priceDate);
            } else {
                prices = cropPriceRepository.findByCropAndMandisAndDate(cropName, mandiIds, priceDate);
            }
            if (!prices.isEmpty()) break;
        }

        if (prices.isEmpty()) return null;

        // Build mandi distance lookup
        Map<UUID, Double> mandiDistances = nearestMandis.stream()
                .collect(Collectors.toMap(m -> UUID.fromString(m.getId()), MandiResponse::getDistanceKm, (a, b) -> a));
        Map<UUID, Mandi> mandiMap = mandiRepository.findAllById(
                prices.stream().map(CropPrice::getMandiId).distinct().toList()
        ).stream().collect(Collectors.toMap(Mandi::getId, m -> m));

        List<MyCropPricesResponse.MandiPriceDto> mandiPrices = prices.stream()
                .map(p -> {
                    Mandi mandi = mandiMap.get(p.getMandiId());
                    return MyCropPricesResponse.MandiPriceDto.builder()
                            .mandiId(p.getMandiId().toString())
                            .mandiName(mandi != null ? mandi.getName() : "Unknown")
                            .district(mandi != null ? mandi.getDistrict() : "")
                            .state(mandi != null ? mandi.getState() : "")
                            .priceMin(p.getPriceMin())
                            .priceMax(p.getPriceMax())
                            .priceModal(p.getPriceModal())
                            .arrivalQuantity(p.getArrivalQuantity())
                            .distanceKm(mandiDistances.getOrDefault(p.getMandiId(), 0.0))
                            .priceDate(p.getPriceDate().toString())
                            .build();
                })
                .sorted(Comparator.comparing(MyCropPricesResponse.MandiPriceDto::getPriceModal).reversed())
                .toList();

        MyCropPricesResponse.MandiPriceDto bestPrice = mandiPrices.isEmpty() ? null : mandiPrices.get(0);

        // 7-day trend
        TrendResponse trend7 = trendAnalysisService.analyzeTrend(cropName, 7);

        // Recommendation
        RecommendationResponse rec = recommendationEngine.recommend(cropName);

        // Get unit
        String unit = cropMasterRepository.findByCropNameIgnoreCase(cropName)
                .map(CropMasterEntity::getUnit).orElse("kg");

        return MyCropPricesResponse.builder()
                .cropName(cropName)
                .unit(unit)
                .farmName(isUserCrop ? crop.getFarmName() : null)
                .trend(trend7.getTrend())
                .recommendation(rec.getRecommendation())
                .recommendationReason(rec.getReasons().isEmpty() ? "" : rec.getReasons().get(0))
                .bestPriceMandi(bestPrice)
                .mandiPrices(mandiPrices)
                .sevenDayChange(trend7.getChangePercent())
                .inUserFarm(isUserCrop)
                .build();
    }
}
