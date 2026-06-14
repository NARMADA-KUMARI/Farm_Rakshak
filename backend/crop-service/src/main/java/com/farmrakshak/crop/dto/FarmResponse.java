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
public class FarmResponse {

    private String id;
    private String userId;
    private String farmName;
    private String village;
    private String district;
    private String state;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private BigDecimal totalArea;
    private String areaUnit;
    private String soilType;
    private BigDecimal allocatedArea;
    private BigDecimal remainingArea;
    private long cropCount;
    private String createdAt;
    private String updatedAt;
}
