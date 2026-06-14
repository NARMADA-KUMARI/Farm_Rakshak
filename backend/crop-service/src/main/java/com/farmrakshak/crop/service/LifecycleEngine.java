package com.farmrakshak.crop.service;

import com.farmrakshak.crop.dto.LifecycleDashboardResponse;
import com.farmrakshak.crop.entity.*;
import com.farmrakshak.crop.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Core Lifecycle Engine — All calculations are data-driven.
 * Zero hardcoded agriculture logic.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LifecycleEngine {

    private final CropMasterRepository cropMasterRepo;
    private final CropStageMasterRepository stageRepo;
    private final CropStageHistoryRepository historyRepo;
    private final CropTaskTemplateRepository templateRepo;
    private final CropTaskRepository taskRepo;
    private final UserCropRepository userCropRepo;

    /**
     * Calculate the current progress percentage for a user crop.
     */
    public BigDecimal calculateProgressPercent(UserCrop userCrop, CropMaster cropMaster) {
        long daysSinceSowing = ChronoUnit.DAYS.between(userCrop.getSowingDate(), LocalDate.now());
        if (daysSinceSowing < 0) daysSinceSowing = 0;

        int totalDays = (int) (cropMaster.getAvgGrowthDays()
                * userCrop.getGrowthAdjustmentFactor().doubleValue());

        if (totalDays <= 0) return BigDecimal.ZERO;

        BigDecimal progress = BigDecimal.valueOf(daysSinceSowing)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalDays), 2, RoundingMode.HALF_UP);

        return progress.min(BigDecimal.valueOf(100));
    }

    /**
     * Determine the current lifecycle stage based on progress percentage.
     */
    public Optional<CropStageMaster> determineCurrentStage(UUID cropId, BigDecimal progressPercent) {
        // Handle 100% edge case — should match the last stage
        if (progressPercent.compareTo(BigDecimal.valueOf(100)) >= 0) {
            List<CropStageMaster> stages = stageRepo.findByCropIdOrderByStageOrderAsc(cropId);
            return stages.isEmpty() ? Optional.empty() : Optional.of(stages.get(stages.size() - 1));
        }
        return stageRepo.findStageByProgress(cropId, progressPercent);
    }

    /**
     * Update stage if it has changed — logs history and generates tasks.
     */
    @Transactional
    public void updateStageIfChanged(UserCrop userCrop, CropStageMaster newStage) {
        UUID oldStageId = userCrop.getCurrentStageId();

        if (newStage == null || newStage.getId().equals(oldStageId)) {
            return; // No change
        }

        // Close existing history entry
        historyRepo.findByUserCropIdAndEndDateIsNull(userCrop.getId())
                .ifPresent(h -> {
                    h.setEndDate(LocalDate.now());
                    historyRepo.save(h);
                });

        // Create new history entry
        CropStageHistory history = CropStageHistory.builder()
                .userCropId(userCrop.getId())
                .stageId(newStage.getId())
                .startDate(LocalDate.now())
                .build();
        historyRepo.save(history);

        // Update user crop
        userCrop.setCurrentStageId(newStage.getId());
        userCropRepo.save(userCrop);

        // Generate tasks for the new stage
        generateTasksForStage(userCrop, newStage);

        log.info("Stage transition: userCrop={}, {} → {}", userCrop.getId(), oldStageId, newStage.getStageName());
    }

    /**
     * Generate tasks from templates for a given stage.
     * Skips tasks that have already been generated (idempotent).
     */
    @Transactional
    public void generateTasksForStage(UserCrop userCrop, CropStageMaster stage) {
        CropMaster crop = cropMasterRepo.findById(userCrop.getCropId()).orElse(null);
        if (crop == null) return;

        List<CropTaskTemplate> templates = templateRepo.findByCropIdAndStageId(crop.getId(), stage.getId());

        // Calculate when this stage started (in real days)
        int totalDays = (int) (crop.getAvgGrowthDays() * userCrop.getGrowthAdjustmentFactor().doubleValue());
        int stageStartDay = stage.getStartDayPercentage()
                .multiply(BigDecimal.valueOf(totalDays))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
                .intValue();

        for (CropTaskTemplate template : templates) {
            // Skip if already generated
            if (taskRepo.existsByUserCropIdAndTemplateId(userCrop.getId(), template.getId())) {
                continue;
            }

            LocalDate dueDate = userCrop.getSowingDate()
                    .plusDays(stageStartDay + template.getDaysAfterStageStart());

            CropTask task = CropTask.builder()
                    .userCropId(userCrop.getId())
                    .templateId(template.getId())
                    .taskTitle(template.getTaskTitle())
                    .taskDescription(template.getTaskDescription())
                    .dueDate(dueDate)
                    .priority(template.getPriority())
                    .build();
            taskRepo.save(task);
        }

        log.info("Generated {} tasks for stage '{}' of userCrop {}", templates.size(), stage.getStageName(), userCrop.getId());
    }

    /**
     * Build the full lifecycle dashboard for a user crop.
     */
    public LifecycleDashboardResponse buildDashboard(UserCrop userCrop) {
        CropMaster crop = cropMasterRepo.findById(userCrop.getCropId())
                .orElseThrow(() -> new RuntimeException("Crop master not found"));

        BigDecimal progressPercent = calculateProgressPercent(userCrop, crop);
        Optional<CropStageMaster> currentStageOpt = determineCurrentStage(crop.getId(), progressPercent);

        // Auto-update stage if changed
        currentStageOpt.ifPresent(stage -> updateStageIfChanged(userCrop, stage));

        List<CropStageMaster> allStages = stageRepo.findByCropIdOrderByStageOrderAsc(crop.getId());
        List<CropStageHistory> history = historyRepo.findByUserCropIdOrderByStartDateAsc(userCrop.getId());
        List<CropTask> tasks = taskRepo.findByUserCropIdAndStatus(userCrop.getId(), "PENDING");

        long daysSinceSowing = ChronoUnit.DAYS.between(userCrop.getSowingDate(), LocalDate.now());

        return LifecycleDashboardResponse.builder()
                .userCropId(userCrop.getId())
                .cropName(crop.getCropName())
                .cropCategory(crop.getCropCategory())
                .sowingDate(userCrop.getSowingDate())
                .expectedHarvestDate(userCrop.getExpectedHarvestDate())
                .status(userCrop.getStatus())
                .daysSinceSowing(Math.max(0, daysSinceSowing))
                .totalGrowthDays(crop.getAvgGrowthDays())
                .progressPercent(progressPercent)
                .currentStageName(currentStageOpt.map(CropStageMaster::getStageName).orElse("Unknown"))
                .currentStageOrder(currentStageOpt.map(CropStageMaster::getStageOrder).orElse(0))
                .totalStages(allStages.size())
                .stageDescription(currentStageOpt.map(CropStageMaster::getDescription).orElse(null))
                .stages(allStages.stream().map(s -> LifecycleDashboardResponse.StageInfo.builder()
                        .stageId(s.getId())
                        .stageName(s.getStageName())
                        .stageOrder(s.getStageOrder())
                        .startPercent(s.getStartDayPercentage())
                        .endPercent(s.getEndDayPercentage())
                        .description(s.getDescription())
                        .isCurrent(currentStageOpt.map(c -> c.getId().equals(s.getId())).orElse(false))
                        .isCompleted(s.getEndDayPercentage().compareTo(progressPercent) <= 0)
                        .build()).toList())
                .pendingTasks(tasks.stream().map(t -> LifecycleDashboardResponse.TaskInfo.builder()
                        .taskId(t.getId())
                        .title(t.getTaskTitle())
                        .description(t.getTaskDescription())
                        .dueDate(t.getDueDate())
                        .priority(t.getPriority())
                        .status(t.getStatus())
                        .build()).toList())
                .stageHistory(history.stream().map(h -> {
                    String stageName = stageRepo.findById(h.getStageId())
                            .map(CropStageMaster::getStageName).orElse("Unknown");
                    return LifecycleDashboardResponse.StageHistoryInfo.builder()
                            .stageName(stageName)
                            .startDate(h.getStartDate())
                            .endDate(h.getEndDate())
                            .build();
                }).toList())
                .build();
    }
}
