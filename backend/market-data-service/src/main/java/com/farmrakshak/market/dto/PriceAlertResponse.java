package com.farmrakshak.market.dto;

import lombok.*;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PriceAlertResponse {
    private String id;
    private String cropName;
    private String mandiId;
    private String mandiName;
    private BigDecimal thresholdPrice;
    private String direction;
    private boolean isActive;
    private String createdAt;
}
