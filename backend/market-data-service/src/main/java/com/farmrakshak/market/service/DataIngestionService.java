package com.farmrakshak.market.service;

import com.farmrakshak.market.entity.CropMasterEntity;
import com.farmrakshak.market.entity.CropPrice;
import com.farmrakshak.market.entity.Mandi;
import com.farmrakshak.market.repository.CropMasterRepository;
import com.farmrakshak.market.repository.CropPriceRepository;
import com.farmrakshak.market.repository.MandiRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Simulated data ingestion pipeline that generates realistic Indian mandi price data.
 * Architecture: swappable — replace body of ingestAll() with real API calls later.
 *
 * Pipeline: Generate → Clean → Normalize → Store → (Redis cache done by MarketService)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataIngestionService {

    private final CropPriceRepository cropPriceRepository;
    private final CropMasterRepository cropMasterRepository;
    private final MandiRepository mandiRepository;

    // Base prices per crop (₹/unit) — realistic seasonal ranges
    private static final Map<String, double[]> CROP_PRICE_RANGES = Map.ofEntries(
            Map.entry("Tomato", new double[]{15, 60}),
            Map.entry("Onion", new double[]{10, 50}),
            Map.entry("Potato", new double[]{12, 35}),
            Map.entry("Cotton", new double[]{5500, 8500}),
            Map.entry("Wheat", new double[]{1800, 2800}),
            Map.entry("Rice", new double[]{2000, 3500}),
            Map.entry("Soybean", new double[]{3500, 5500}),
            Map.entry("Chili", new double[]{30, 120}),
            Map.entry("Sugarcane", new double[]{2500, 3500}),
            Map.entry("Maize", new double[]{1200, 2200}),
            Map.entry("Groundnut", new double[]{4000, 6500}),
            Map.entry("Turmeric", new double[]{6000, 12000}),
            Map.entry("Garlic", new double[]{40, 200}),
            Map.entry("Ginger", new double[]{50, 250}),
            Map.entry("Cauliflower", new double[]{10, 45}),
            Map.entry("Cabbage", new double[]{8, 30}),
            Map.entry("Brinjal", new double[]{15, 50}),
            Map.entry("Banana", new double[]{20, 60}),
            Map.entry("Mango", new double[]{30, 120}),
            Map.entry("Grape", new double[]{30, 100}),
            Map.entry("Pomegranate", new double[]{60, 180}),
            Map.entry("Cumin", new double[]{100, 350}),
            Map.entry("Coriander", new double[]{40, 150}),
            Map.entry("Mustard", new double[]{4000, 6000}),
            Map.entry("Green Pea", new double[]{25, 80}),
            Map.entry("Lady Finger", new double[]{15, 60}),
            Map.entry("Capsicum", new double[]{20, 80}),
            Map.entry("Lemon", new double[]{30, 120}),
            Map.entry("Coconut", new double[]{15, 35}),
            Map.entry("Jowar", new double[]{1500, 3000})
    );

    /**
     * Ingest prices for all crops across all mandis for a given date range.
     * Generates realistic data with seasonal patterns and regional variation.
     */
    @Transactional
    public int ingestAll(LocalDate fromDate, LocalDate toDate) {
        List<CropMasterEntity> crops = cropMasterRepository.findAll();
        List<Mandi> mandis = mandiRepository.findByIsActiveTrue();
        int count = 0;

        for (LocalDate date = fromDate; !date.isAfter(toDate); date = date.plusDays(1)) {
            for (CropMasterEntity crop : crops) {
                // Each crop appears in ~60-80% of mandis on any given day
                List<Mandi> availableMandis = randomSubset(mandis, 0.6, 0.8);

                for (Mandi mandi : availableMandis) {
                    if (cropPriceRepository.existsByCropNameIgnoreCaseAndMandiIdAndPriceDate(
                            crop.getCropName(), mandi.getId(), date)) {
                        continue; // Skip if already exists
                    }

                    CropPrice price = generatePrice(crop.getCropName(), mandi, date);
                    cropPriceRepository.save(price);
                    count++;
                }
            }
        }

        log.info("Ingested {} price records from {} to {}", count, fromDate, toDate);
        return count;
    }

    /**
     * Quick ingest for just today (called by scheduler every 6 hours).
     */
    @Transactional
    public int ingestToday() {
        return ingestAll(LocalDate.now(), LocalDate.now());
    }

    private CropPrice generatePrice(String cropName, Mandi mandi, LocalDate date) {
        double[] range = CROP_PRICE_RANGES.getOrDefault(cropName, new double[]{20, 100});
        double baseMin = range[0];
        double baseMax = range[1];

        // Seasonal factor (summer prices higher for vegetables, winter for grains)
        double seasonFactor = getSeasonalFactor(cropName, date.getMonth());

        // Regional variation (±10%)
        double regionFactor = 0.9 + (hashCode(mandi.getName()) % 20) / 100.0;

        // Daily random variation (±8%)
        double dailyNoise = 0.92 + ThreadLocalRandom.current().nextDouble() * 0.16;

        // Trend factor — simulate gradual price movements
        double trendFactor = 1.0 + Math.sin(date.getDayOfYear() / 30.0) * 0.05;

        double mid = (baseMin + baseMax) / 2;
        double spread = (baseMax - baseMin) / 2;

        double modalPrice = mid * seasonFactor * regionFactor * dailyNoise * trendFactor;
        double priceMin = modalPrice * (0.85 + ThreadLocalRandom.current().nextDouble() * 0.05);
        double priceMax = modalPrice * (1.05 + ThreadLocalRandom.current().nextDouble() * 0.10);
        double arrivalQty = 50 + ThreadLocalRandom.current().nextDouble() * 500;

        return CropPrice.builder()
                .cropName(cropName)
                .mandiId(mandi.getId())
                .priceModal(BigDecimal.valueOf(modalPrice).setScale(2, RoundingMode.HALF_UP))
                .priceMin(BigDecimal.valueOf(priceMin).setScale(2, RoundingMode.HALF_UP))
                .priceMax(BigDecimal.valueOf(priceMax).setScale(2, RoundingMode.HALF_UP))
                .arrivalQuantity(BigDecimal.valueOf(arrivalQty).setScale(2, RoundingMode.HALF_UP))
                .priceDate(date)
                .createdAt(Instant.now())
                .build();
    }

    private double getSeasonalFactor(String cropName, Month month) {
        // Vegetables peak in summer, grains peak post-harvest
        boolean isVegetable = CROP_PRICE_RANGES.getOrDefault(cropName, new double[]{0, 100})[1] < 500;
        int m = month.getValue(); // 1-12

        if (isVegetable) {
            // Vegetables: higher in summer (Apr-Jun), lower in winter harvest
            if (m >= 4 && m <= 6) return 1.15 + ThreadLocalRandom.current().nextDouble() * 0.10;
            if (m >= 10 && m <= 12) return 0.85 + ThreadLocalRandom.current().nextDouble() * 0.05;
            return 0.95 + ThreadLocalRandom.current().nextDouble() * 0.10;
        } else {
            // Commodities: higher pre-harvest, lower post-harvest
            if (m >= 3 && m <= 5) return 1.10 + ThreadLocalRandom.current().nextDouble() * 0.05;
            if (m >= 10 && m <= 12) return 0.90 + ThreadLocalRandom.current().nextDouble() * 0.05;
            return 0.95 + ThreadLocalRandom.current().nextDouble() * 0.10;
        }
    }

    private int hashCode(String s) {
        return Math.abs(s.hashCode());
    }

    private <T> List<T> randomSubset(List<T> list, double minFraction, double maxFraction) {
        double fraction = minFraction + ThreadLocalRandom.current().nextDouble() * (maxFraction - minFraction);
        int count = Math.max(1, (int) (list.size() * fraction));
        List<T> shuffled = new ArrayList<>(list);
        Collections.shuffle(shuffled);
        return shuffled.subList(0, Math.min(count, shuffled.size()));
    }
}
