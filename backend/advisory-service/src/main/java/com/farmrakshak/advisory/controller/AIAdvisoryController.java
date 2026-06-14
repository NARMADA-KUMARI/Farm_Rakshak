package com.farmrakshak.advisory.controller;

import com.farmrakshak.advisory.dto.AdvisoryResponse;
import com.farmrakshak.advisory.dto.CropPlanRequest;
import com.farmrakshak.advisory.dto.CropPlanResponse;
import com.farmrakshak.advisory.dto.CropRecommendationRequest;
import com.farmrakshak.advisory.dto.CropRecommendationResponse;
import com.farmrakshak.advisory.service.AIAdvisoryService;
import com.farmrakshak.advisory.service.CropPlanGenerator;
import com.farmrakshak.advisory.service.CropRecommendationService;
import com.farmrakshak.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/advisories/ai")
@RequiredArgsConstructor
public class AIAdvisoryController {

    private final AIAdvisoryService aiAdvisoryService;
    private final CropPlanGenerator cropPlanGenerator;
    private final CropRecommendationService cropRecommendationService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AdvisoryResponse>> getAdvisory(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(defaultValue = "19.076") double lat,
            @RequestParam(defaultValue = "72.8777") double lon) {
        AdvisoryResponse response = aiAdvisoryService.getAdvisory(UUID.fromString(userId), lat, lon, false);
        return ResponseEntity.ok(ApiResponse.success(response, "Advisory generated successfully"));
    }

    @GetMapping("/me/refresh")
    public ResponseEntity<ApiResponse<AdvisoryResponse>> refreshAdvisory(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(defaultValue = "19.076") double lat,
            @RequestParam(defaultValue = "72.8777") double lon) {
        AdvisoryResponse response = aiAdvisoryService.getAdvisory(UUID.fromString(userId), lat, lon, true);
        return ResponseEntity.ok(ApiResponse.success(response, "Advisory refreshed successfully"));
    }

    @PostMapping("/crop-plan")
    public ResponseEntity<ApiResponse<CropPlanResponse>> generateCropPlan(
            @RequestBody CropPlanRequest request) {
        CropPlanResponse plan = cropPlanGenerator.generatePlan(request);
        return ResponseEntity.ok(ApiResponse.success(plan, "Crop lifecycle plan generated"));
    }

    /**
     * AI-powered crop recommendations based on farm location, season, weather, and soil.
     */
    @PostMapping("/crop-recommendations")
    public ResponseEntity<ApiResponse<CropRecommendationResponse>> getCropRecommendations(
            @RequestBody CropRecommendationRequest request) {
        CropRecommendationResponse response = cropRecommendationService.getRecommendations(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Crop recommendations generated"));
    }
}
