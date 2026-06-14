package com.farmrakshak.advisory.service;

import com.farmrakshak.advisory.dto.*;
import com.farmrakshak.advisory.entity.AiSuggestion;
import com.farmrakshak.advisory.repository.AiSuggestionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIAdvisoryService {

    private final RuleEngine ruleEngine;
    private final MistralAiClient mistralAiClient;
    private final AiSuggestionRepository suggestionRepo;
    private final ObjectMapper objectMapper;

    @Value("${services.crop-url:http://crop-service}")
    private String cropServiceUrl;

    @Value("${services.weather-url:http://weather-service}")
    private String weatherServiceUrl;

    private final WebClient.Builder webClientBuilder;

    // ──────────────────────────────────────────────
    //  PUBLIC API
    // ──────────────────────────────────────────────

    public AdvisoryResponse getAdvisory(UUID userId, double lat, double lon, boolean forceRefresh) {
        if (!forceRefresh) {
            List<AiSuggestion> cached = suggestionRepo.findFreshByUserId(userId, Instant.now());
            if (!cached.isEmpty()) {
                log.info("Returning cached advisory for user {}", userId);
                return buildFromCache(cached);
            }
        }

        // Try farm-based crops first, fall back to legacy lifecycle crops
        List<CropContext> crops = fetchFarmCropContext(userId);
        if (crops.isEmpty()) {
            log.info("No farm crops found, falling back to lifecycle crops for user {}", userId);
            crops = fetchLegacyCropContext(userId);
        }

        if (crops.isEmpty()) {
            log.info("No crops found for user {}", userId);
            return AdvisoryResponse.builder()
                    .crops(Collections.emptyList()).source("RULE").build();
        }

        enrichWithDiseaseHistory(userId, crops);
        WeatherContext weather = fetchWeather(lat, lon);

        List<AdvisoryResponse.CropAdvisory> cropAdvisories = new ArrayList<>();
        for (CropContext crop : crops) {
            List<Suggestion> ruleSuggestions = ruleEngine.generateSuggestions(crop, weather);
            List<Suggestion> aiSuggestions = mistralAiClient.generateSuggestions(List.of(crop), weather);

            List<Suggestion> merged = mergeSuggestions(ruleSuggestions, aiSuggestions);
            String overallRisk = calculateOverallRisk(merged);

            cropAdvisories.add(AdvisoryResponse.CropAdvisory.builder()
                    .cropId(crop.getCropId())
                    .cropName(crop.getCropName())
                    .cropStage(crop.getCropStage())
                    .daysSinceSowing(crop.getDaysSinceSowing())
                    .progressPercent(crop.getProgressPercent())
                    .areaAllocated(crop.getAreaAllocated())
                    .overallRisk(overallRisk)
                    .suggestions(merged)
                    .recentDiseases(crop.getDiseaseHistory())
                    .farmId(crop.getFarmId())
                    .farmName(crop.getFarmName())
                    .village(crop.getVillage())
                    .district(crop.getDistrict())
                    .state(crop.getState())
                    .farmTotalArea(crop.getFarmTotalArea())
                    .areaUnit(crop.getAreaUnit())
                    .build());

            cacheResult(userId, crop, merged, weather, aiSuggestions.isEmpty() ? "RULE" : "MERGED");
        }

        String source = mistralAiClient.isConfigured() ? "MERGED" : "RULE";
        return AdvisoryResponse.builder()
                .crops(cropAdvisories)
                .weather(weather)
                .source(source)
                .build();
    }

    // ──────────────────────────────────────────────
    //  INTER-SERVICE CALLS — Farm-aware + Legacy fallback
    // ──────────────────────────────────────────────

    /**
     * Fetch crops grouped by farm via the new farms API.
     */
    @SuppressWarnings("unchecked")
    private List<CropContext> fetchFarmCropContext(UUID userId) {
        try {
            WebClient client = webClientBuilder.build();

            Map<?, ?> farmsResp = client.get()
                    .uri(cropServiceUrl + "/api/v1/farms/my")
                    .header("X-User-Id", userId.toString())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (farmsResp == null || farmsResp.get("data") == null) return Collections.emptyList();

            List<Map<String, Object>> farms = (List<Map<String, Object>>) farmsResp.get("data");
            List<CropContext> allCrops = new ArrayList<>();

            for (Map<String, Object> farm : farms) {
                String farmId = getStr(farm, "id");
                String farmName = getStr(farm, "farmName");
                String village = getStr(farm, "village");
                String district = getStr(farm, "district");
                String state = getStr(farm, "state");
                double farmTotalArea = getDbl(farm, "totalArea");
                String areaUnit = getStr(farm, "areaUnit");
                String soilType = getStr(farm, "soilType");

                try {
                    Map<?, ?> cropsResp = client.get()
                            .uri(cropServiceUrl + "/api/v1/farms/" + farmId + "/crops")
                            .header("X-User-Id", userId.toString())
                            .retrieve()
                            .bodyToMono(Map.class)
                            .block();

                    if (cropsResp == null || cropsResp.get("data") == null) continue;

                    List<Map<String, Object>> cropsData = (List<Map<String, Object>>) cropsResp.get("data");
                    for (Map<String, Object> c : cropsData) {
                        allCrops.add(CropContext.builder()
                                .cropId(getStr(c, "id"))
                                .cropName(getStr(c, "cropName"))
                                .cropStage(getStr(c, "cropStage"))
                                .daysSinceSowing(getInt(c, "daysSinceSowing"))
                                .totalGrowthDays(0)
                                .progressPercent(0)
                                .soilType(soilType)
                                .irrigationType(getStr(c, "irrigationType"))
                                .areaAllocated(getDbl(c, "areaAllocated"))
                                .farmId(farmId)
                                .farmName(farmName)
                                .village(village)
                                .district(district)
                                .state(state)
                                .farmTotalArea(farmTotalArea)
                                .areaUnit(areaUnit != null ? areaUnit : "acres")
                                .diseaseHistory(new ArrayList<>())
                                .build());
                    }
                } catch (Exception e) {
                    log.warn("Failed to fetch crops for farm {}: {}", farmId, e.getMessage());
                }
            }

            return allCrops;
        } catch (Exception e) {
            log.warn("Failed to fetch farm context: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Fallback: fetch crops from the legacy lifecycle API (no farm context).
     */
    @SuppressWarnings("unchecked")
    private List<CropContext> fetchLegacyCropContext(UUID userId) {
        try {
            WebClient client = webClientBuilder.build();
            Map<?, ?> response = client.get()
                    .uri(cropServiceUrl + "/api/v1/crops/lifecycle/my-crops")
                    .header("X-User-Id", userId.toString())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null || response.get("data") == null) return Collections.emptyList();

            List<Map<String, Object>> cropsData = (List<Map<String, Object>>) response.get("data");
            return cropsData.stream().map(m -> CropContext.builder()
                    .cropId(getStr(m, "userCropId"))
                    .cropName(getStr(m, "cropName"))
                    .cropStage(getStr(m, "currentStageName"))
                    .daysSinceSowing(getInt(m, "daysSinceSowing"))
                    .totalGrowthDays(getInt(m, "totalGrowthDays"))
                    .progressPercent(getDbl(m, "progressPercent"))
                    .soilType(getStr(m, "soilType"))
                    .irrigationType(getStr(m, "irrigationType"))
                    .diseaseHistory(new ArrayList<>())
                    .build()
            ).collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Failed to fetch legacy crops: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @SuppressWarnings("unchecked")
    private void enrichWithDiseaseHistory(UUID userId, List<CropContext> crops) {
        try {
            WebClient client = webClientBuilder.build();
            Map<?, ?> response = client.get()
                    .uri(cropServiceUrl + "/api/v1/crops/internal/disease-history?userId=" + userId)
                    .header("X-User-Id", userId.toString())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null || response.get("data") == null) return;

            List<Map<String, Object>> diseases = (List<Map<String, Object>>) response.get("data");
            for (CropContext crop : crops) {
                List<CropContext.DiseaseRecord> records = diseases.stream()
                        .filter(d -> d.get("diseaseName") != null)
                        .filter(d -> {
                            // Match by cropId (exact)
                            if (crop.getCropId() != null && crop.getCropId().equals(getStr(d, "userCropId"))) {
                                return true;
                            }
                            // Match by crop name (fuzzy — "Rice" matches "Rice (Oryza sativa)")
                            String diseaseCropName = getStr(d, "cropName");
                            if (diseaseCropName != null && crop.getCropName() != null) {
                                String a = crop.getCropName().trim().toLowerCase();
                                String b = diseaseCropName.trim().toLowerCase();
                                return a.equals(b) || b.contains(a) || a.contains(b);
                            }
                            return false;
                        })
                        .map(d -> CropContext.DiseaseRecord.builder()
                                .diseaseName(getStr(d, "diseaseName"))
                                .confidence(getDbl(d, "confidence"))
                                .treatment(getStr(d, "treatment"))
                                .prevention(getStr(d, "prevention"))
                                .analyzedAt(getStr(d, "analyzedAt"))
                                .build())
                        .collect(Collectors.toList());
                crop.setDiseaseHistory(records);
            }
        } catch (Exception e) {
            log.warn("Failed to fetch disease history: {}", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private WeatherContext fetchWeather(double lat, double lon) {
        try {
            WebClient client = webClientBuilder.build();
            Map<?, ?> response = client.get()
                    .uri(weatherServiceUrl + "/api/v1/weather?lat=" + lat + "&lon=" + lon)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null || response.get("data") == null) return null;

            Map<String, Object> data = (Map<String, Object>) response.get("data");
            Object inner = data.get("data");
            Map<String, Object> wd;
            if (inner instanceof String) {
                wd = objectMapper.readValue((String) inner, new TypeReference<>() {});
            } else {
                wd = (Map<String, Object>) inner;
            }

            return WeatherContext.builder()
                    .temperature(getDbl(wd, "temperature"))
                    .feelsLike(getDbl(wd, "feelsLike"))
                    .tempMin(getDbl(wd, "tempMin"))
                    .tempMax(getDbl(wd, "tempMax"))
                    .humidity(getDbl(wd, "humidity"))
                    .pressure(getInt(wd, "pressure"))
                    .windSpeed(getDbl(wd, "windSpeed"))
                    .windDeg(getInt(wd, "windDeg"))
                    .condition(getStr(wd, "description"))
                    .mainWeather(getStr(wd, "mainWeather"))
                    .cloudCover(getInt(wd, "cloudCover"))
                    .visibility(getInt(wd, "visibility"))
                    .rainVolume(getDbl(wd, "rainVolume"))
                    .rainForecast(Boolean.TRUE.equals(wd.get("rainForecast")))
                    .cityName(getStr(wd, "cityName"))
                    .country(getStr(wd, "country"))
                    .lat(lat).lon(lon)
                    .build();
        } catch (Exception e) {
            log.error("Failed to fetch weather: {}", e.getMessage());
            return null;
        }
    }

    // ──────────────────────────────────────────────
    //  HELPERS
    // ──────────────────────────────────────────────

    private List<Suggestion> mergeSuggestions(List<Suggestion> rules, List<Suggestion> ai) {
        Map<String, Suggestion> merged = new LinkedHashMap<>();
        for (Suggestion s : ai) {
            merged.put(s.getCategory() + ":" + s.getMessage().substring(0, Math.min(30, s.getMessage().length())), s);
        }
        for (Suggestion s : rules) {
            String key = s.getCategory() + ":" + s.getMessage().substring(0, Math.min(30, s.getMessage().length()));
            merged.putIfAbsent(key, s);
        }
        return new ArrayList<>(merged.values());
    }

    private String calculateOverallRisk(List<Suggestion> suggestions) {
        boolean hasHigh = suggestions.stream().anyMatch(s -> "HIGH".equals(s.getRiskLevel()));
        boolean hasMedium = suggestions.stream().anyMatch(s -> "MEDIUM".equals(s.getRiskLevel()));
        if (hasHigh) return "HIGH";
        if (hasMedium) return "MEDIUM";
        return "LOW";
    }

    private void cacheResult(UUID userId, CropContext crop, List<Suggestion> suggestions,
                              WeatherContext weather, String source) {
        try {
            AiSuggestion entity = AiSuggestion.builder()
                    .userId(userId)
                    .cropName(crop.getCropName())
                    .cropStage(crop.getCropStage())
                    .suggestions(objectMapper.writeValueAsString(suggestions))
                    .weatherSnapshot(weather != null ? objectMapper.writeValueAsString(weather) : null)
                    .diseaseContext(crop.getDiseaseHistory() != null ? objectMapper.writeValueAsString(crop.getDiseaseHistory()) : null)
                    .source(source)
                    .expiresAt(Instant.now().plus(6, ChronoUnit.HOURS))
                    .build();
            suggestionRepo.save(entity);
        } catch (JsonProcessingException e) {
            log.warn("Failed to cache suggestion: {}", e.getMessage());
        }
    }

    private AdvisoryResponse buildFromCache(List<AiSuggestion> cached) {
        List<AdvisoryResponse.CropAdvisory> cropAdvisories = new ArrayList<>();
        for (AiSuggestion s : cached) {
            try {
                List<Suggestion> suggestions = objectMapper.readValue(s.getSuggestions(), new TypeReference<>() {});
                List<CropContext.DiseaseRecord> diseases = s.getDiseaseContext() != null ?
                        objectMapper.readValue(s.getDiseaseContext(), new TypeReference<>() {}) :
                        Collections.emptyList();

                cropAdvisories.add(AdvisoryResponse.CropAdvisory.builder()
                        .cropName(s.getCropName())
                        .cropStage(s.getCropStage())
                        .overallRisk(calculateOverallRisk(suggestions))
                        .suggestions(suggestions)
                        .recentDiseases(diseases)
                        .build());
            } catch (JsonProcessingException e) {
                log.warn("Failed to deserialize cached suggestion: {}", e.getMessage());
            }
        }

        WeatherContext cachedWeather = null;
        if (!cached.isEmpty() && cached.get(0).getWeatherSnapshot() != null) {
            try { cachedWeather = objectMapper.readValue(cached.get(0).getWeatherSnapshot(), WeatherContext.class); }
            catch (Exception e) { /* ignore */ }
        }

        return AdvisoryResponse.builder()
                .crops(cropAdvisories)
                .weather(cachedWeather)
                .source(cached.get(0).getSource())
                .build();
    }

    private String getStr(Map<?, ?> m, String key) {
        Object v = m.get(key);
        return v != null ? v.toString() : null;
    }

    private int getInt(Map<?, ?> m, String key) {
        Object v = m.get(key);
        if (v instanceof Number) return ((Number) v).intValue();
        return 0;
    }

    private double getDbl(Map<?, ?> m, String key) {
        Object v = m.get(key);
        if (v instanceof Number) return ((Number) v).doubleValue();
        return 0.0;
    }
}
