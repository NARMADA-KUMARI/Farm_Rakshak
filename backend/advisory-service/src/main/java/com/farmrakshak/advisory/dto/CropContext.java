package com.farmrakshak.advisory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CropContext {
    private String cropId;
    private String cropName;
    private String cropStage;
    private int daysSinceSowing;
    private int totalGrowthDays;
    private double progressPercent;
    private String soilType;
    private String irrigationType;
    private double areaAllocated;
    private List<DiseaseRecord> diseaseHistory;

    // Farm context
    private String farmId;
    private String farmName;
    private String village;
    private String district;
    private String state;
    private double farmTotalArea;
    private String areaUnit;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DiseaseRecord {
        private String diseaseName;
        private double confidence;
        private String treatment;
        private String prevention;
        private String analyzedAt;
    }
}
