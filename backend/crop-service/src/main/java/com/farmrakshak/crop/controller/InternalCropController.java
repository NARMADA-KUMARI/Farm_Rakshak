package com.farmrakshak.crop.controller;

import com.farmrakshak.crop.dto.UserCropInfoResponse;
import com.farmrakshak.crop.entity.Farm;
import com.farmrakshak.crop.entity.FarmCrop;
import com.farmrakshak.crop.repository.FarmCropRepository;
import com.farmrakshak.crop.repository.FarmRepository;
import com.farmrakshak.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Internal API for inter-service communication.
 * Called by market-data-service to fetch user crops and farm locations.
 * NOT exposed through the gateway — accessed via Eureka service discovery.
 */
@RestController
@RequestMapping("/internal/crops")
@RequiredArgsConstructor
public class InternalCropController {

    private final FarmRepository farmRepository;
    private final FarmCropRepository farmCropRepository;

    /**
     * Returns all active crops for a user with their farm location data.
     * Used by market-data-service to:
     * 1. Know which crops to fetch prices for
     * 2. Get farm coordinates for nearest mandi calculation
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<UserCropInfoResponse>>> getUserCrops(
            @PathVariable UUID userId) {

        List<Farm> farms = farmRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId);

        List<UserCropInfoResponse> result = new ArrayList<>();

        for (Farm farm : farms) {
            List<FarmCrop> crops = farmCropRepository
                    .findByFarmIdAndDeletedFalseOrderByCreatedAtDesc(farm.getId());

            for (FarmCrop crop : crops) {
                if (!"ACTIVE".equals(crop.getStatus())) continue;

                result.add(UserCropInfoResponse.builder()
                        .cropId(crop.getId().toString())
                        .cropName(crop.getCropName())
                        .farmId(farm.getId().toString())
                        .farmName(farm.getFarmName())
                        .village(farm.getVillage())
                        .district(farm.getDistrict())
                        .state(farm.getState())
                        .latitude(farm.getLatitude())
                        .longitude(farm.getLongitude())
                        .build());
            }
        }

        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
