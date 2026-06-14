package com.farmrakshak.market.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class MyCropPricesResponse {
    private String cropName;
    private String unit;
    private String farmName;
    private String trend; // UP, DOWN, STABLE
    private String recommendation; // SELL, HOLD, WAIT
    private String recommendationReason;
    private MandiPriceDto bestPriceMandi;
    private List<MandiPriceDto> mandiPrices;
    private BigDecimal sevenDayChange;
    private boolean inUserFarm;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class MandiPriceDto {
        private String mandiId;
        private String mandiName;
        private String district;
        private String state;
        private BigDecimal priceMin;
        private BigDecimal priceMax;
        private BigDecimal priceModal;
        private BigDecimal arrivalQuantity;
        private double distanceKm;
        private String priceDate;
    }
}
