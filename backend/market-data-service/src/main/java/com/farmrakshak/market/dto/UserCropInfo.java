package com.farmrakshak.market.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

/** DTO received from crop-service internal API */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UserCropInfo {
    private String cropId;
    private String cropName;
    private String farmId;
    private String farmName;
    private String village;
    private String district;
    private String state;
    private BigDecimal latitude;
    private BigDecimal longitude;
}
