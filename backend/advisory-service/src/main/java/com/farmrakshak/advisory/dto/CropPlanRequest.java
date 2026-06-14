package com.farmrakshak.advisory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CropPlanRequest {
    private String cropName;
    private String variety;
    private String sowingDate;
    private String expectedHarvest;
    private String soilType;
    private String irrigationType;
    private double areaAllocated;
    private String areaUnit;
    private String farmName;
    private String village;
    private String district;
    private String state;
}
