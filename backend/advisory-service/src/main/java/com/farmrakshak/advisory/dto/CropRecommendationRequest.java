package com.farmrakshak.advisory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CropRecommendationRequest {
    private String state;
    private String district;
    private String village;
    private String soilType;
    private String irrigationType;
    private Double totalArea;
    private String areaUnit;
    private Double temperature;
    private Double humidity;
    private List<String> existingCrops;
}
