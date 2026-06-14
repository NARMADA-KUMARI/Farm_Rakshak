package com.farmrakshak.crop.controller;

import com.farmrakshak.crop.service.CropPlanService;
import com.farmrakshak.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class CropPlanController {

    private final CropPlanService cropPlanService;

    @PostMapping("/api/v1/crops/{cropId}/generate-plan")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> generatePlan(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID cropId) {
        List<Map<String, Object>> tasks = cropPlanService.generatePlanOnDemand(cropId, UUID.fromString(userId));
        return ResponseEntity.ok(ApiResponse.success(tasks, "Lifecycle plan generated"));
    }

    @GetMapping("/api/v1/crops/{cropId}/tasks")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getCropTasks(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID cropId) {
        List<Map<String, Object>> tasks = cropPlanService.getTasksForCrop(cropId, UUID.fromString(userId));
        return ResponseEntity.ok(ApiResponse.success(tasks));
    }

    @GetMapping("/api/v1/crops/tasks/today")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTodaysTasks(
            @RequestHeader("X-User-Id") String userId) {
        List<Map<String, Object>> tasks = cropPlanService.getTodaysTasks(UUID.fromString(userId));
        return ResponseEntity.ok(ApiResponse.success(tasks));
    }

    @PutMapping("/api/v1/crops/tasks/{taskId}/complete")
    public ResponseEntity<ApiResponse<Map<String, Object>>> completeTask(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID taskId) {
        Map<String, Object> task = cropPlanService.completeTask(taskId, UUID.fromString(userId));
        return ResponseEntity.ok(ApiResponse.success(task, "Task completed"));
    }
}
