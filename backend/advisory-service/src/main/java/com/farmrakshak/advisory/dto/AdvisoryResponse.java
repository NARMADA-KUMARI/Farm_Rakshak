package com.farmrakshak.advisory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AdvisoryResponse {
    private List<CropAdvisory> crops;
    private WeatherContext weather;
    private String source; // RULE, AI, MERGED

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CropAdvisory {
        private String cropId;
        private String cropName;
        private String cropStage;
        private int daysSinceSowing;
        private double progressPercent;
        private double areaAllocated;
        private String overallRisk; // HIGH, MEDIUM, LOW
        private List<Suggestion> suggestions;
        private List<CropContext.DiseaseRecord> recentDiseases;

        // Farm context
        private String farmId;
        private String farmName;
        private String village;
        private String district;
        private String state;
        private double farmTotalArea;
        private String areaUnit;
    }
}
