package com.farmrakshak.market.controller;

import com.farmrakshak.market.dto.*;
import com.farmrakshak.market.service.*;
import com.farmrakshak.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/market")
@RequiredArgsConstructor
public class MarketController {

    private final MarketService marketService;
    private final TrendAnalysisService trendAnalysisService;
    private final PricePredictionEngine predictionEngine;
    private final RecommendationEngine recommendationEngine;
    private final AlertService alertService;
    private final MandiLocatorService mandiLocatorService;

    /**
     * GET /api/v1/market/my-crops
     * Returns prices for all crops in user's farms.
     */
    @GetMapping("/my-crops")
    public ResponseEntity<ApiResponse<List<MyCropPricesResponse>>> getMyCropPrices(
            @RequestHeader("X-User-Id") String userId) {
        List<MyCropPricesResponse> prices = marketService.getMyCropPrices(UUID.fromString(userId));
        return ResponseEntity.ok(ApiResponse.success(prices));
    }

    /**
     * GET /api/v1/market/search?crop=tomato&lat=17.96&lon=79.59
     * Search any crop's prices across mandis.
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<MyCropPricesResponse>> searchCropPrices(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam String crop,
            @RequestParam(required = false) BigDecimal lat,
            @RequestParam(required = false) BigDecimal lon) {
        MyCropPricesResponse response = marketService.searchCropPrices(
                crop, lat, lon, UUID.fromString(userId));
        if (response == null) {
            return ResponseEntity.ok(ApiResponse.success(null, "No price data found for: " + crop));
        }
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * GET /api/v1/market/search/crops?q=tam
     * Auto-complete crop search with synonyms and farm ownership status.
     */
    @GetMapping("/search/crops")
    public ResponseEntity<ApiResponse<List<CropSearchResponse>>> searchCrops(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam String q) {
        List<CropSearchResponse> results = marketService.searchCrops(q, UUID.fromString(userId));
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    /**
     * GET /api/v1/market/trends/{crop}?days=7
     * Trend data for charts.
     */
    @GetMapping("/trends/{crop}")
    public ResponseEntity<ApiResponse<TrendResponse>> getTrends(
            @PathVariable String crop,
            @RequestParam(defaultValue = "7") int days) {
        String resolvedCrop = marketService.resolveCropName(crop);
        TrendResponse trend = trendAnalysisService.analyzeTrend(resolvedCrop, days);

        // Attach prediction
        TrendResponse.PredictionDto prediction = predictionEngine.predict(resolvedCrop, trend);
        trend.setPrediction(prediction);

        return ResponseEntity.ok(ApiResponse.success(trend));
    }

    /**
     * GET /api/v1/market/recommendation/{crop}
     * AI sell recommendation.
     */
    @GetMapping("/recommendation/{crop}")
    public ResponseEntity<ApiResponse<RecommendationResponse>> getRecommendation(
            @PathVariable String crop) {
        String resolvedCrop = marketService.resolveCropName(crop);
        RecommendationResponse rec = recommendationEngine.recommend(resolvedCrop);
        return ResponseEntity.ok(ApiResponse.success(rec));
    }

    /**
     * POST /api/v1/market/alerts
     * Create price alert subscription.
     */
    @PostMapping("/alerts")
    public ResponseEntity<ApiResponse<PriceAlertResponse>> createAlert(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody PriceAlertRequest request) {
        PriceAlertResponse response = alertService.createAlert(UUID.fromString(userId), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response));
    }

    /**
     * GET /api/v1/market/alerts
     * List user's price alerts.
     */
    @GetMapping("/alerts")
    public ResponseEntity<ApiResponse<List<PriceAlertResponse>>> getUserAlerts(
            @RequestHeader("X-User-Id") String userId) {
        List<PriceAlertResponse> alerts = alertService.getUserAlerts(UUID.fromString(userId));
        return ResponseEntity.ok(ApiResponse.success(alerts));
    }

    /**
     * GET /api/v1/market/nearest-mandis?lat=17.96&lon=79.59
     * Find nearest 5 mandis.
     */
    @GetMapping("/nearest-mandis")
    public ResponseEntity<ApiResponse<List<MandiResponse>>> getNearestMandis(
            @RequestParam(required = false) BigDecimal lat,
            @RequestParam(required = false) BigDecimal lon) {
        List<MandiResponse> mandis = mandiLocatorService.findNearest(lat, lon, 5);
        return ResponseEntity.ok(ApiResponse.success(mandis));
    }
}
