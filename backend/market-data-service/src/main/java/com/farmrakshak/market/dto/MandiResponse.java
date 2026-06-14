package com.farmrakshak.market.dto;

import lombok.*;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class MandiResponse {
    private String id;
    private String name;
    private String state;
    private String district;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private double distanceKm;
}
