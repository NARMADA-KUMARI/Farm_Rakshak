package com.farmrakshak.crop.controller;

import com.farmrakshak.crop.dto.LifecycleDashboardResponse;
import com.farmrakshak.crop.dto.SowCropRequest;
import com.farmrakshak.crop.entity.CropMaster;
import com.farmrakshak.crop.repository.CropMasterRepository;
import com.farmrakshak.crop.service.UserCropService;
import com.farmrakshak.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/crops/lifecycle")
@RequiredArgsConstructor
public class LifecycleController {

    private final UserCropService userCropService;
    private final CropMasterRepository cropMasterRepo;

    /**
     * List all available crops from the master database.
     */
    @GetMapping("/crops")
    public ResponseEntity<ApiResponse<List<CropMaster>>> listAvailableCrops() {
        return ResponseEntity.ok(ApiResponse.success(cropMasterRepo.findAll()));
    }

    /**
     * Sow a new crop — auto-calculates harvest date and initializes lifecycle.
     */
    @PostMapping("/sow")
    public ResponseEntity<ApiResponse<LifecycleDashboardResponse>> sowCrop(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody SowCropRequest request) {
        LifecycleDashboardResponse dashboard = userCropService.sowCrop(UUID.fromString(userId), request);
        return ResponseEntity.ok(ApiResponse.success(dashboard, "Crop sown successfully"));
    }

    /**
     * List all active crops for the current user.
     */
    @GetMapping("/my-crops")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMyCrops(
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(ApiResponse.success(userCropService.getUserCrops(UUID.fromString(userId))));
    }

    /**
     * Get the full lifecycle dashboard for a specific crop.
     */
    @GetMapping("/my-crops/{id}")
    public ResponseEntity<ApiResponse<LifecycleDashboardResponse>> getDashboard(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                userCropService.getDashboard(UUID.fromString(userId), id)));
    }

    /**
     * Get stage transition history for a crop.
     */
    @GetMapping("/my-crops/{id}/history")
    public ResponseEntity<ApiResponse<List<LifecycleDashboardResponse.StageHistoryInfo>>> getHistory(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                userCropService.getStageHistory(UUID.fromString(userId), id)));
    }

    /**
     * Mark a task as completed.
     */
    @PutMapping("/tasks/{taskId}/complete")
    public ResponseEntity<ApiResponse<Void>> completeTask(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID taskId) {
        userCropService.completeTask(UUID.fromString(userId), taskId);
        return ResponseEntity.ok(ApiResponse.success(null, "Task completed"));
    }
}
