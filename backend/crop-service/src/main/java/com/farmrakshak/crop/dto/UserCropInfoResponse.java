package com.farmrakshak.crop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCropInfoResponse {
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
