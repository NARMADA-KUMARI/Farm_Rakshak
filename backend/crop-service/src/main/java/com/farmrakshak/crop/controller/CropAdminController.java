package com.farmrakshak.crop.controller;

import com.farmrakshak.crop.entity.*;
import com.farmrakshak.crop.repository.*;
import com.farmrakshak.shared.dto.ApiResponse;
import com.farmrakshak.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Admin CRUD APIs for managing crop lifecycle configuration.
 * All lifecycle data comes from database — never hardcoded.
 */
@RestController
@RequestMapping("/api/v1/crops/admin")
@RequiredArgsConstructor
public class CropAdminController {

    private final CropMasterRepository cropMasterRepo;
    private final CropStageMasterRepository stageRepo;
    private final CropTaskTemplateRepository templateRepo;

    // ── Crop Master CRUD ─────────────────────────────────────
    @PostMapping("/crops")
    public ResponseEntity<ApiResponse<CropMaster>> createCrop(@RequestBody CropMaster crop) {
        return ResponseEntity.ok(ApiResponse.success(cropMasterRepo.save(crop), "Crop created"));
    }

    @GetMapping("/crops")
    public ResponseEntity<ApiResponse<List<CropMaster>>> listCrops() {
        return ResponseEntity.ok(ApiResponse.success(cropMasterRepo.findAll()));
    }

    @PutMapping("/crops/{id}")
    public ResponseEntity<ApiResponse<CropMaster>> updateCrop(@PathVariable UUID id, @RequestBody Map<String, Object> updates) {
        CropMaster crop = cropMasterRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CropMaster", "id", id.toString()));
        if (updates.containsKey("avgGrowthDays")) crop.setAvgGrowthDays((Integer) updates.get("avgGrowthDays"));
        if (updates.containsKey("minGrowthDays")) crop.setMinGrowthDays((Integer) updates.get("minGrowthDays"));
        if (updates.containsKey("maxGrowthDays")) crop.setMaxGrowthDays((Integer) updates.get("maxGrowthDays"));
        if (updates.containsKey("region")) crop.setRegion((String) updates.get("region"));
        if (updates.containsKey("seedVariety")) crop.setSeedVariety((String) updates.get("seedVariety"));
        return ResponseEntity.ok(ApiResponse.success(cropMasterRepo.save(crop), "Crop updated"));
    }

    // ── Stage Master CRUD ────────────────────────────────────
    @PostMapping("/crops/{cropId}/stages")
    public ResponseEntity<ApiResponse<CropStageMaster>> addStage(@PathVariable UUID cropId, @RequestBody CropStageMaster stage) {
        cropMasterRepo.findById(cropId)
                .orElseThrow(() -> new ResourceNotFoundException("CropMaster", "id", cropId.toString()));
        stage.setCropId(cropId);
        return ResponseEntity.ok(ApiResponse.success(stageRepo.save(stage), "Stage added"));
    }

    @GetMapping("/crops/{cropId}/stages")
    public ResponseEntity<ApiResponse<List<CropStageMaster>>> getStages(@PathVariable UUID cropId) {
        return ResponseEntity.ok(ApiResponse.success(stageRepo.findByCropIdOrderByStageOrderAsc(cropId)));
    }

    @PutMapping("/stages/{id}")
    public ResponseEntity<ApiResponse<CropStageMaster>> updateStage(@PathVariable UUID id, @RequestBody Map<String, Object> updates) {
        CropStageMaster stage = stageRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CropStageMaster", "id", id.toString()));
        if (updates.containsKey("stageName")) stage.setStageName((String) updates.get("stageName"));
        if (updates.containsKey("startDayPercentage")) stage.setStartDayPercentage(new java.math.BigDecimal(updates.get("startDayPercentage").toString()));
        if (updates.containsKey("endDayPercentage")) stage.setEndDayPercentage(new java.math.BigDecimal(updates.get("endDayPercentage").toString()));
        if (updates.containsKey("description")) stage.setDescription((String) updates.get("description"));
        return ResponseEntity.ok(ApiResponse.success(stageRepo.save(stage), "Stage updated"));
    }

    // ── Task Template CRUD ───────────────────────────────────
    @PostMapping("/crops/{cropId}/tasks")
    public ResponseEntity<ApiResponse<CropTaskTemplate>> addTaskTemplate(@PathVariable UUID cropId, @RequestBody CropTaskTemplate template) {
        cropMasterRepo.findById(cropId)
                .orElseThrow(() -> new ResourceNotFoundException("CropMaster", "id", cropId.toString()));
        template.setCropId(cropId);
        return ResponseEntity.ok(ApiResponse.success(templateRepo.save(template), "Task template added"));
    }

    @GetMapping("/crops/{cropId}/tasks")
    public ResponseEntity<ApiResponse<List<CropTaskTemplate>>> getTaskTemplates(@PathVariable UUID cropId) {
        return ResponseEntity.ok(ApiResponse.success(templateRepo.findByCropId(cropId)));
    }

    @PutMapping("/tasks/{id}")
    public ResponseEntity<ApiResponse<CropTaskTemplate>> updateTaskTemplate(@PathVariable UUID id, @RequestBody Map<String, Object> updates) {
        CropTaskTemplate template = templateRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CropTaskTemplate", "id", id.toString()));
        if (updates.containsKey("taskTitle")) template.setTaskTitle((String) updates.get("taskTitle"));
        if (updates.containsKey("taskDescription")) template.setTaskDescription((String) updates.get("taskDescription"));
        if (updates.containsKey("daysAfterStageStart")) template.setDaysAfterStageStart((Integer) updates.get("daysAfterStageStart"));
        if (updates.containsKey("priority")) template.setPriority((String) updates.get("priority"));
        return ResponseEntity.ok(ApiResponse.success(templateRepo.save(template), "Task template updated"));
    }
}
