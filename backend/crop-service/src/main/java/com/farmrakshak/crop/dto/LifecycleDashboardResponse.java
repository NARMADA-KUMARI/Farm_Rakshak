package com.farmrakshak.crop.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LifecycleDashboardResponse {
    private UUID userCropId;
    private String cropName;
    private String cropCategory;
    private LocalDate sowingDate;
    private LocalDate expectedHarvestDate;
    private String status;

    // Progress
    private long daysSinceSowing;
    private int totalGrowthDays;
    private BigDecimal progressPercent;

    // Current Stage
    private String currentStageName;
    private int currentStageOrder;
    private int totalStages;
    private String stageDescription;

    // Stages overview
    private List<StageInfo> stages;

    // Tasks
    private List<TaskInfo> pendingTasks;

    // History
    private List<StageHistoryInfo> stageHistory;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class StageInfo {
        private UUID stageId;
        private String stageName;
        private int stageOrder;
        private BigDecimal startPercent;
        private BigDecimal endPercent;
        private String description;
        private boolean isCurrent;
        private boolean isCompleted;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class TaskInfo {
        private UUID taskId;
        private String title;
        private String description;
        private LocalDate dueDate;
        private String priority;
        private String status;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class StageHistoryInfo {
        private String stageName;
        private LocalDate startDate;
        private LocalDate endDate;
    }
}
