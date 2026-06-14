package com.farmrakshak.market.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TrendResponse {
    private String cropName;
    private int days;
    private String trend; // UP, DOWN, STABLE
    private BigDecimal startPrice;
    private BigDecimal endPrice;
    private BigDecimal changePercent;
    private BigDecimal movingAverage;
    private BigDecimal volatility;
    private List<TrendDataPoint> dataPoints;
    private PredictionDto prediction;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TrendDataPoint {
        private String date;
        private BigDecimal priceModal;
        private BigDecimal priceMin;
        private BigDecimal priceMax;
        private BigDecimal arrivalQuantity;
        private BigDecimal movingAvg;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class PredictionDto {
        private String direction; // UP, DOWN
        private int probabilityPercent;
        private String reason;
    }
}
