package com.farmrakshak.crop.controller;

import com.farmrakshak.crop.dto.*;
import com.farmrakshak.crop.service.FarmCropService;
import com.farmrakshak.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class FarmCropController {

    private final FarmCropService farmCropService;

    @PostMapping("/api/v1/farms/{farmId}/crops")
    public ResponseEntity<ApiResponse<FarmCropResponse>> addCrop(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID farmId,
            @Valid @RequestBody AddFarmCropRequest request) {
        FarmCropResponse response = farmCropService.addCrop(farmId, UUID.fromString(userId), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response));
    }

    @GetMapping("/api/v1/farms/{farmId}/crops")
    public ResponseEntity<ApiResponse<List<FarmCropResponse>>> listCrops(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID farmId) {
        List<FarmCropResponse> crops = farmCropService.listCrops(farmId, UUID.fromString(userId));
        return ResponseEntity.ok(ApiResponse.success(crops));
    }

    @GetMapping("/api/v1/crops/{cropId}")
    public ResponseEntity<ApiResponse<FarmCropDetailResponse>> getCropDetail(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID cropId) {
        FarmCropDetailResponse response = farmCropService.getCropDetail(cropId, UUID.fromString(userId));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/api/v1/crops/{cropId}")
    public ResponseEntity<ApiResponse<FarmCropResponse>> updateCrop(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID cropId,
            @Valid @RequestBody UpdateFarmCropRequest request) {
        FarmCropResponse response = farmCropService.updateCrop(cropId, UUID.fromString(userId), request);
        return ResponseEntity.ok(ApiResponse.success(response, "Crop updated successfully"));
    }

    @PutMapping("/api/v1/crops/{cropId}/stage")
    public ResponseEntity<ApiResponse<FarmCropResponse>> changeCropStage(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID cropId,
            @Valid @RequestBody ChangeCropStageRequest request) {
        FarmCropResponse response = farmCropService.changeCropStage(cropId, UUID.fromString(userId), request.getCropStage());
        return ResponseEntity.ok(ApiResponse.success(response, "Crop stage updated"));
    }

    @DeleteMapping("/api/v1/crops/{cropId}")
    public ResponseEntity<ApiResponse<Void>> deleteCrop(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID cropId) {
        farmCropService.deleteCrop(cropId, UUID.fromString(userId));
        return ResponseEntity.ok(ApiResponse.success(null, "Crop deleted successfully"));
    }
}
