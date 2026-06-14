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
public class FarmCropDetailResponse {

    private String id;
    private String farmId;
    private String farmName;
    private String cropName;
    private String variety;
    private String sowingDate;
    private String expectedHarvest;
    private String cropStage;
    private BigDecimal areaAllocated;
    private String irrigationType;
    private String status;
    private long daysSinceSowing;
    private String createdAt;
    private String updatedAt;

    // Farm location context for AI integration
    private String village;
    private String district;
    private String state;
}
