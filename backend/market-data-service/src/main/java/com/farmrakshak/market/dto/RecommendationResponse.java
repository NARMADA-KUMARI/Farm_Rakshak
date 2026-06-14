package com.farmrakshak.market.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RecommendationResponse {
    private String cropName;
    private String recommendation; // SELL, HOLD, WAIT
    private int waitDays;
    private List<String> reasons;
    private String trend;
    private BigDecimal currentPrice;
    private BigDecimal predictedPrice;
    private int confidencePercent;
    private MarketInsight insight;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class MarketInsight {
        private String supplyTrend;   // INCREASING, DECREASING, STABLE
        private String demandTrend;   // INCREASING, DECREASING, STABLE
        private String seasonalNote;
        private String narrative;     // Human readable market insight
    }
}
