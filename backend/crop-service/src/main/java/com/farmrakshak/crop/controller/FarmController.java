package com.farmrakshak.crop.controller;

import com.farmrakshak.crop.dto.*;
import com.farmrakshak.crop.service.FarmService;
import com.farmrakshak.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/farms")
@RequiredArgsConstructor
public class FarmController {

    private final FarmService farmService;

    @PostMapping
    public ResponseEntity<ApiResponse<FarmResponse>> createFarm(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody CreateFarmRequest request) {
        FarmResponse response = farmService.createFarm(UUID.fromString(userId), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<FarmSummaryResponse>>> getMyFarms(
            @RequestHeader("X-User-Id") String userId) {
        List<FarmSummaryResponse> farms = farmService.getMyFarms(UUID.fromString(userId));
        return ResponseEntity.ok(ApiResponse.success(farms));
    }

    @GetMapping("/{farmId}")
    public ResponseEntity<ApiResponse<FarmResponse>> getFarm(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID farmId) {
        FarmResponse response = farmService.getFarmById(farmId, UUID.fromString(userId));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{farmId}")
    public ResponseEntity<ApiResponse<FarmResponse>> updateFarm(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID farmId,
            @Valid @RequestBody UpdateFarmRequest request) {
        FarmResponse response = farmService.updateFarm(farmId, UUID.fromString(userId), request);
        return ResponseEntity.ok(ApiResponse.success(response, "Farm updated successfully"));
    }

    @DeleteMapping("/{farmId}")
    public ResponseEntity<ApiResponse<Void>> deleteFarm(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID farmId) {
        farmService.deleteFarm(farmId, UUID.fromString(userId));
        return ResponseEntity.ok(ApiResponse.success(null, "Farm deleted successfully"));
    }
}
