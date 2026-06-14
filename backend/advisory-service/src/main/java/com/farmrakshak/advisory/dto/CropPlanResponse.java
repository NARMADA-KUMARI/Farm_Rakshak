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
public class CropPlanResponse {
    private String cropName;
    private int totalDays;
    private List<PlanTask> tasks;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlanTask {
        private int dayNumber;
        private String stage;
        private String title;
        private String description;
        private String priority; // HIGH, MEDIUM, LOW
    }
}
