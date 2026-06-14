package com.farmrakshak.crop.service;

import com.farmrakshak.crop.dto.LifecycleDashboardResponse;
import com.farmrakshak.crop.dto.SowCropRequest;
import com.farmrakshak.crop.entity.*;
import com.farmrakshak.crop.repository.*;
import com.farmrakshak.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserCropService {

    private final UserCropRepository userCropRepo;
    private final CropMasterRepository cropMasterRepo;
    private final CropStageMasterRepository stageRepo;
    private final CropTaskRepository taskRepo;
    private final CropStageHistoryRepository historyRepo;
    private final LifecycleEngine lifecycleEngine;

    /**
     * Sow a new crop — auto-calculates expected harvest date.
     */
    @Transactional
    public LifecycleDashboardResponse sowCrop(UUID userId, SowCropRequest request) {
        CropMaster crop = cropMasterRepo.findById(request.getCropId())
                .orElseThrow(() -> new ResourceNotFoundException("CropMaster", "id", request.getCropId().toString()));

        UserCrop userCrop = UserCrop.builder()
                .userId(userId)
                .cropId(crop.getId())
                .sowingDate(request.getSowingDate())
                .expectedHarvestDate(request.getSowingDate().plusDays(crop.getAvgGrowthDays()))
                .landArea(request.getLandArea())
                .landAreaUnit(request.getLandAreaUnit() != null ? request.getLandAreaUnit() : "acres")
                .soilType(request.getSoilType())
                .irrigationType(request.getIrrigationType())
                .build();

        userCrop = userCropRepo.save(userCrop);
        log.info("Crop sown: userId={}, cropId={}, userCropId={}", userId, crop.getId(), userCrop.getId());

        // Build dashboard (triggers initial stage calculation + task generation)
        return lifecycleEngine.buildDashboard(userCrop);
    }

    /**
     * List all active crops for a user with current stage info.
     */
    public List<Map<String, Object>> getUserCrops(UUID userId) {
        List<UserCrop> crops = userCropRepo.findByUserIdAndStatusOrderByCreatedAtDesc(userId, "ACTIVE");
        return crops.stream().map(uc -> {
            CropMaster crop = cropMasterRepo.findById(uc.getCropId()).orElse(null);
            String stageName = uc.getCurrentStageId() != null
                    ? stageRepo.findById(uc.getCurrentStageId()).map(CropStageMaster::getStageName).orElse("Unknown")
                    : "Not Started";

            var progressPercent = crop != null
                    ? lifecycleEngine.calculateProgressPercent(uc, crop)
                    : java.math.BigDecimal.ZERO;

            return Map.<String, Object>of(
                    "userCropId", uc.getId().toString(),
                    "cropName", crop != null ? crop.getCropName() : "Unknown",
                    "sowingDate", uc.getSowingDate().toString(),
                    "expectedHarvestDate", uc.getExpectedHarvestDate().toString(),
                    "currentStage", stageName,
                    "progressPercent", progressPercent,
                    "status", uc.getStatus()
            );
        }).collect(Collectors.toList());
    }

    /**
     * Get full lifecycle dashboard for a specific user crop.
     */
    @Transactional
    public LifecycleDashboardResponse getDashboard(UUID userId, UUID userCropId) {
        UserCrop userCrop = userCropRepo.findById(userCropId)
                .orElseThrow(() -> new ResourceNotFoundException("UserCrop", "id", userCropId.toString()));

        if (!userCrop.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("UserCrop", "id", userCropId.toString());
        }

        return lifecycleEngine.buildDashboard(userCrop);
    }

    /**
     * Complete a task.
     */
    @Transactional
    public void completeTask(UUID userId, UUID taskId) {
        CropTask task = taskRepo.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("CropTask", "id", taskId.toString()));

        UserCrop userCrop = userCropRepo.findById(task.getUserCropId())
                .orElseThrow(() -> new ResourceNotFoundException("UserCrop", "id", task.getUserCropId().toString()));

        if (!userCrop.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("CropTask", "id", taskId.toString());
        }

        task.setStatus("COMPLETED");
        task.setCompletedAt(Instant.now());
        taskRepo.save(task);

        log.info("Task completed: taskId={}, userId={}", taskId, userId);
    }

    /**
     * Get stage transition history for a user crop.
     */
    public List<LifecycleDashboardResponse.StageHistoryInfo> getStageHistory(UUID userId, UUID userCropId) {
        UserCrop userCrop = userCropRepo.findById(userCropId)
                .orElseThrow(() -> new ResourceNotFoundException("UserCrop", "id", userCropId.toString()));

        if (!userCrop.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("UserCrop", "id", userCropId.toString());
        }

        return historyRepo.findByUserCropIdOrderByStartDateAsc(userCropId).stream()
                .map(h -> {
                    String stageName = stageRepo.findById(h.getStageId())
                            .map(CropStageMaster::getStageName).orElse("Unknown");
                    return LifecycleDashboardResponse.StageHistoryInfo.builder()
                            .stageName(stageName)
                            .startDate(h.getStartDate())
                            .endDate(h.getEndDate())
                            .build();
                }).toList();
    }
}
