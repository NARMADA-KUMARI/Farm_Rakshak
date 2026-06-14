package com.farmrakshak.market.service;

import com.farmrakshak.market.dto.RecommendationResponse;
import com.farmrakshak.market.dto.TrendResponse;
import com.farmrakshak.market.entity.CropPrice;
import com.farmrakshak.market.repository.CropPriceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * AI Sell Recommendation Engine.
 * Analyzes trends, supply, and seasonality to recommend SELL / HOLD / WAIT.
 */
@Service
@RequiredArgsConstructor
public class RecommendationEngine {

    private final TrendAnalysisService trendAnalysisService;
    private final PricePredictionEngine predictionEngine;

    public RecommendationResponse recommend(String cropName) {
        TrendResponse trend7 = trendAnalysisService.analyzeTrend(cropName, 7);
        TrendResponse trend30 = trendAnalysisService.analyzeTrend(cropName, 30);
        TrendResponse.PredictionDto prediction = predictionEngine.predict(cropName, trend7);

        BigDecimal currentPrice = trend7.getEndPrice() != null ? trend7.getEndPrice() : BigDecimal.ZERO;
        BigDecimal change7 = trend7.getChangePercent() != null ? trend7.getChangePercent() : BigDecimal.ZERO;
        BigDecimal change30 = trend30.getChangePercent() != null ? trend30.getChangePercent() : BigDecimal.ZERO;

        String recommendation;
        int waitDays = 0;
        List<String> reasons = new ArrayList<>();
        BigDecimal predictedPrice = currentPrice;

        // Decision logic
        boolean risingSteeply = change7.compareTo(BigDecimal.valueOf(8)) > 0;
        boolean risingModerate = change7.compareTo(BigDecimal.valueOf(3)) > 0;
        boolean atPeak = change7.compareTo(BigDecimal.valueOf(0)) > 0 && change7.compareTo(BigDecimal.valueOf(3)) <= 0
                && change30.compareTo(BigDecimal.valueOf(10)) > 0;
        boolean declining = change7.compareTo(BigDecimal.valueOf(-3)) < 0;
        boolean steeplyDeclining = change7.compareTo(BigDecimal.valueOf(-8)) < 0;

        // Check arrival trends for supply insight
        String supplyTrend = analyzeSupplyTrend(trend7);
        boolean lowArrivals = "DECREASING".equals(supplyTrend);
        boolean highArrivals = "INCREASING".equals(supplyTrend);

        if (steeplyDeclining) {
            recommendation = "SELL";
            reasons.add("Prices declining sharply (-" + change7.abs() + "% this week)");
            reasons.add("Further decline likely if not sold soon");
            if (highArrivals) reasons.add("High market arrivals adding downward pressure");
        } else if (atPeak || (declining && highArrivals)) {
            recommendation = "SELL";
            reasons.add("Price appears near peak (+" + change30.abs() + "% over 30 days)");
            reasons.add("Trend flattening — optimal selling window");
            if (highArrivals) reasons.add("Increasing supply indicates price correction ahead");
        } else if (risingSteeply && lowArrivals) {
            recommendation = "WAIT";
            waitDays = 5;
            reasons.add("Strong upward momentum (+" + change7 + "% this week)");
            reasons.add("Low market arrivals — supply shortage supporting prices");
            reasons.add("Prices likely to continue rising for " + waitDays + " more days");
            predictedPrice = currentPrice.multiply(BigDecimal.valueOf(1.05)).setScale(2, RoundingMode.HALF_UP);
        } else if (risingModerate) {
            recommendation = "HOLD";
            reasons.add("Prices rising steadily (+" + change7 + "% this week)");
            reasons.add("Monitor for 2-3 more days before selling");
            if (lowArrivals) reasons.add("Low arrivals suggest continued price support");
        } else if (declining && !steeplyDeclining) {
            recommendation = "SELL";
            reasons.add("Mild price decline detected");
            reasons.add("Better to sell before further correction");
        } else {
            recommendation = "HOLD";
            reasons.add("Market is stable — no urgent action needed");
            reasons.add("Watch price trends over the next few days");
        }

        // Market insight
        String demandTrend = guessDemanTrend(change7, supplyTrend);
        String narrative = buildNarrative(cropName, recommendation, supplyTrend, demandTrend);

        RecommendationResponse.MarketInsight insight = RecommendationResponse.MarketInsight.builder()
                .supplyTrend(supplyTrend)
                .demandTrend(demandTrend)
                .seasonalNote(getSeasonalNote(cropName))
                .narrative(narrative)
                .build();

        return RecommendationResponse.builder()
                .cropName(cropName)
                .recommendation(recommendation)
                .waitDays(waitDays)
                .reasons(reasons)
                .trend(trend7.getTrend())
                .currentPrice(currentPrice)
                .predictedPrice(predictedPrice)
                .confidencePercent(prediction.getProbabilityPercent())
                .insight(insight)
                .build();
    }

    private String analyzeSupplyTrend(TrendResponse trend) {
        List<TrendResponse.TrendDataPoint> points = trend.getDataPoints();
        if (points.size() < 3) return "STABLE";

        BigDecimal recent = points.get(points.size() - 1).getArrivalQuantity();
        BigDecimal older = points.get(0).getArrivalQuantity();
        if (recent == null || older == null || older.compareTo(BigDecimal.ZERO) == 0) return "STABLE";

        BigDecimal change = recent.subtract(older).divide(older, 4, RoundingMode.HALF_UP);
        if (change.compareTo(BigDecimal.valueOf(0.1)) > 0) return "INCREASING";
        if (change.compareTo(BigDecimal.valueOf(-0.1)) < 0) return "DECREASING";
        return "STABLE";
    }

    private String guessDemanTrend(BigDecimal priceChange, String supplyTrend) {
        // If price rising and supply stable/decreasing → demand increasing
        if (priceChange.compareTo(BigDecimal.valueOf(3)) > 0) {
            if (!"INCREASING".equals(supplyTrend)) return "INCREASING";
        }
        if (priceChange.compareTo(BigDecimal.valueOf(-3)) < 0) {
            if (!"DECREASING".equals(supplyTrend)) return "DECREASING";
        }
        return "STABLE";
    }

    private String buildNarrative(String cropName, String recommendation, String supplyTrend, String demandTrend) {
        if ("SELL".equals(recommendation)) {
            return String.format("Market conditions suggest selling %s now. %s supply with %s demand creates a favorable selling window.",
                    cropName, supplyTrend.toLowerCase(), demandTrend.toLowerCase());
        }
        if ("WAIT".equals(recommendation)) {
            return String.format("%s prices are rising with %s supply. Hold your harvest for better returns in the coming days.",
                    cropName, supplyTrend.toLowerCase());
        }
        return String.format("%s market is currently stable. Monitor daily prices and sell when you see a favorable spike.",
                cropName);
    }

    private String getSeasonalNote(String cropName) {
        LocalDate now = LocalDate.now();
        int month = now.getMonthValue();
        if (List.of("Tomato", "Onion", "Potato").contains(cropName) && month >= 4 && month <= 6) {
            return "Summer peak season — prices typically higher due to reduced supply";
        }
        if (List.of("Wheat", "Rice").contains(cropName) && (month >= 4 && month <= 5)) {
            return "Post-Rabi harvest — higher supply may suppress prices temporarily";
        }
        return "Normal seasonal period for " + cropName;
    }
}
