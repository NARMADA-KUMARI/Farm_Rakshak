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
public class CropRecommendationResponse {
    private List<RecommendedCrop> recommendations;
    private String season;
    private String source;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecommendedCrop {
        private String cropName;
        private String reason;
        private String bestSowingTime;
        private String expectedYield;
        private String waterRequirement;
        private String marketDemand;
        private int suitabilityScore; // 1-100
    }
}
