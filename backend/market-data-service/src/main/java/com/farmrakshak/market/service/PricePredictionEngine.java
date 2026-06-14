package com.farmrakshak.market.service;

import com.farmrakshak.market.dto.TrendResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;

/**
 * Rule-based price prediction engine.
 * ML-ready interface — swap this implementation for ML model later.
 *
 * Factors: trend slope, arrival volumes, seasonality, volatility.
 */
@Service
@RequiredArgsConstructor
public class PricePredictionEngine {

    private final TrendAnalysisService trendAnalysisService;

    public TrendResponse.PredictionDto predict(String cropName, TrendResponse trendData) {
        if (trendData == null || trendData.getDataPoints().isEmpty()) {
            return TrendResponse.PredictionDto.builder()
                    .direction("STABLE")
                    .probabilityPercent(50)
                    .reason("Insufficient data for prediction")
                    .build();
        }

        int score = 50; // Start neutral
        StringBuilder reason = new StringBuilder();

        // Factor 1: Current trend momentum
        BigDecimal changePercent = trendData.getChangePercent() != null ? trendData.getChangePercent() : BigDecimal.ZERO;
        if (changePercent.compareTo(BigDecimal.valueOf(5)) > 0) {
            score += 15;
            reason.append("Strong upward momentum. ");
        } else if (changePercent.compareTo(BigDecimal.valueOf(2)) > 0) {
            score += 8;
            reason.append("Moderate price increase. ");
        } else if (changePercent.compareTo(BigDecimal.valueOf(-5)) < 0) {
            score -= 15;
            reason.append("Significant price decline. ");
        } else if (changePercent.compareTo(BigDecimal.valueOf(-2)) < 0) {
            score -= 8;
            reason.append("Moderate price decrease. ");
        }

        // Factor 2: Arrival volume trend
        List<TrendResponse.TrendDataPoint> points = trendData.getDataPoints();
        if (points.size() >= 3) {
            BigDecimal recentArrival = points.get(points.size() - 1).getArrivalQuantity();
            BigDecimal olderArrival = points.get(points.size() - 3).getArrivalQuantity();
            if (recentArrival != null && olderArrival != null && olderArrival.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal arrivalChange = recentArrival.subtract(olderArrival)
                        .divide(olderArrival, 4, RoundingMode.HALF_UP);
                if (arrivalChange.compareTo(BigDecimal.valueOf(-0.15)) < 0) {
                    score += 10; // Lower supply → price up
                    reason.append("Declining arrivals signal supply shortage. ");
                } else if (arrivalChange.compareTo(BigDecimal.valueOf(0.15)) > 0) {
                    score -= 10; // Higher supply → price down
                    reason.append("Increasing arrivals may push prices down. ");
                }
            }
        }

        // Factor 3: Seasonality
        Month currentMonth = LocalDate.now().getMonth();
        int seasonalAdjust = getSeasonalScore(cropName, currentMonth);
        score += seasonalAdjust;
        if (seasonalAdjust > 5) {
            reason.append("Seasonally favorable for price increase. ");
        } else if (seasonalAdjust < -5) {
            reason.append("Seasonal harvest pressure likely. ");
        }

        // Factor 4: Volatility
        BigDecimal volatility = trendData.getVolatility() != null ? trendData.getVolatility() : BigDecimal.ZERO;
        if (volatility.compareTo(BigDecimal.valueOf(15)) > 0) {
            // High volatility—reduce confidence slightly
            score = (int) (score * 0.9);
            reason.append("High price volatility noted. ");
        }

        // Clamp score to [10, 90]
        score = Math.max(10, Math.min(90, score));

        String direction = score >= 50 ? "UP" : "DOWN";
        int probability = score >= 50 ? score : (100 - score);

        return TrendResponse.PredictionDto.builder()
                .direction(direction)
                .probabilityPercent(probability)
                .reason(reason.toString().trim())
                .build();
    }

    private int getSeasonalScore(String cropName, Month month) {
        int m = month.getValue();
        // Vegetables: prices up in summer, down in winter harvest
        boolean isVegetable = List.of("Tomato", "Onion", "Potato", "Cauliflower", "Cabbage",
                "Brinjal", "Lady Finger", "Capsicum", "Green Pea").contains(cropName);

        if (isVegetable) {
            if (m >= 4 && m <= 6) return 8;   // Summer scarcity
            if (m >= 11 || m <= 1) return -6;  // Winter harvest
        }

        // Grains: prices up pre-harvest, down post-harvest
        if (List.of("Wheat", "Rice", "Maize", "Jowar").contains(cropName)) {
            if (m >= 2 && m <= 4) return 5;    // Pre-Rabi harvest
            if (m >= 8 && m <= 10) return 5;   // Pre-Kharif harvest
            if (m >= 5 && m <= 6) return -5;   // Post-Rabi harvest
        }

        return 0;
    }
}
