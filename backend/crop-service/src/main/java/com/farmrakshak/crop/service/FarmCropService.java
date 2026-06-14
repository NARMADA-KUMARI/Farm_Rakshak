package com.farmrakshak.crop.service;

import com.farmrakshak.crop.dto.*;
import com.farmrakshak.crop.entity.Farm;
import com.farmrakshak.crop.entity.FarmCrop;
import com.farmrakshak.crop.repository.FarmCropRepository;
import com.farmrakshak.shared.constants.KafkaTopics;
import com.farmrakshak.shared.event.FarmCropEvent;
import com.farmrakshak.shared.event.NotificationEvent;
import com.farmrakshak.shared.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FarmCropService {

    private static final Set<String> VALID_STAGES = Set.of(
            "PLANNED", "SOWN", "GERMINATION", "VEGETATIVE", "FLOWERING",
            "FRUITING", "HARVEST_READY", "HARVESTED", "FAILED"
    );

    private final FarmCropRepository farmCropRepository;
    private final OwnershipValidator ownershipValidator;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final CropPlanService cropPlanService;

    @Transactional
    public FarmCropResponse addCrop(UUID farmId, UUID userId, AddFarmCropRequest request) {
        Farm farm = ownershipValidator.validateFarmOwnership(farmId, userId);

        // Validate crop stage
        String stage = request.getCropStage() != null ? request.getCropStage() : "PLANNED";
        if (!VALID_STAGES.contains(stage)) {
            throw new BadRequestException("Invalid crop stage: " + stage + ". Valid stages: " + VALID_STAGES);
        }

        // Validate sowing date
        if (request.getSowingDate() != null && request.getSowingDate().isAfter(LocalDate.now().plusDays(365))) {
            throw new BadRequestException("Sowing date cannot be more than 1 year in the future");
        }

        // Validate area does not exceed farm total
        BigDecimal currentAllocated = farmCropRepository.sumAllocatedAreaByFarmId(farmId);
        if (request.getAreaAllocated() != null && farm.getTotalArea() != null) {
            BigDecimal newTotal = currentAllocated.add(request.getAreaAllocated());
            if (newTotal.compareTo(farm.getTotalArea()) > 0) {
                BigDecimal remaining = farm.getTotalArea().subtract(currentAllocated);
                throw new BadRequestException(
                        String.format("Area exceeds farm capacity. Farm: %s %s, Already allocated: %s, Remaining: %s, Requested: %s",
                                farm.getTotalArea(), farm.getAreaUnit(), currentAllocated, remaining, request.getAreaAllocated())
                );
            }
        }

        FarmCrop crop = FarmCrop.builder()
                .farmId(farmId)
                .cropName(request.getCropName())
                .variety(request.getVariety())
                .sowingDate(request.getSowingDate())
                .expectedHarvest(request.getExpectedHarvest())
                .cropStage(stage)
                .areaAllocated(request.getAreaAllocated())
                .irrigationType(request.getIrrigationType())
                .build();

        crop = farmCropRepository.save(crop);
        log.info("Crop added: id={}, farmId={}, name={}", crop.getId(), farmId, crop.getCropName());

        publishCropEvent("CROP_CREATED", crop, farm);

        // Build area info for notification
        BigDecimal newAllocated = farmCropRepository.sumAllocatedAreaByFarmId(farmId);
        BigDecimal remaining = farm.getTotalArea() != null ? farm.getTotalArea().subtract(newAllocated) : null;
        String areaInfo = "";
        if (crop.getAreaAllocated() != null && farm.getTotalArea() != null) {
            areaInfo = String.format(" Area: %s %s allocated. Remaining: %s %s.",
                    crop.getAreaAllocated(), farm.getAreaUnit(), remaining, farm.getAreaUnit());
        }

        publishNotification(farm.getUserId(), "CROP",
                "Crop Added to " + farm.getFarmName(),
                String.format("\"%s\" has been added to farm \"%s\".%s", crop.getCropName(), farm.getFarmName(), areaInfo));

        // Generate AI lifecycle plan asynchronously
        cropPlanService.generatePlanForCrop(crop, farm);

        return toResponse(crop);
    }

    @Transactional(readOnly = true)
    public List<FarmCropResponse> listCrops(UUID farmId, UUID userId) {
        ownershipValidator.validateFarmOwnership(farmId, userId);
        return farmCropRepository.findByFarmIdAndDeletedFalseOrderByCreatedAtDesc(farmId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public FarmCropDetailResponse getCropDetail(UUID cropId, UUID userId) {
        FarmCrop crop = ownershipValidator.validateCropOwnership(cropId, userId);
        Farm farm = ownershipValidator.validateFarmOwnership(crop.getFarmId(), userId);

        long daysSinceSowing = 0;
        if (crop.getSowingDate() != null && !crop.getSowingDate().isAfter(LocalDate.now())) {
            daysSinceSowing = ChronoUnit.DAYS.between(crop.getSowingDate(), LocalDate.now());
        }

        return FarmCropDetailResponse.builder()
                .id(crop.getId().toString())
                .farmId(crop.getFarmId().toString())
                .farmName(farm.getFarmName())
                .cropName(crop.getCropName())
                .variety(crop.getVariety())
                .sowingDate(crop.getSowingDate() != null ? crop.getSowingDate().toString() : null)
                .expectedHarvest(crop.getExpectedHarvest() != null ? crop.getExpectedHarvest().toString() : null)
                .cropStage(crop.getCropStage())
                .areaAllocated(crop.getAreaAllocated())
                .irrigationType(crop.getIrrigationType())
                .status(crop.getStatus())
                .daysSinceSowing(daysSinceSowing)
                .village(farm.getVillage())
                .district(farm.getDistrict())
                .state(farm.getState())
                .createdAt(crop.getCreatedAt() != null ? crop.getCreatedAt().toString() : null)
                .updatedAt(crop.getUpdatedAt() != null ? crop.getUpdatedAt().toString() : null)
                .build();
    }

    @Transactional
    public FarmCropResponse updateCrop(UUID cropId, UUID userId, UpdateFarmCropRequest request) {
        FarmCrop crop = ownershipValidator.validateCropOwnership(cropId, userId);
        Farm farm = ownershipValidator.validateFarmOwnership(crop.getFarmId(), userId);

        // Validate crop stage if provided
        if (request.getCropStage() != null && !VALID_STAGES.contains(request.getCropStage())) {
            throw new BadRequestException("Invalid crop stage: " + request.getCropStage());
        }

        // Validate area if being updated
        if (request.getAreaAllocated() != null && farm.getTotalArea() != null) {
            BigDecimal otherCropsArea = farmCropRepository.sumAllocatedAreaByFarmIdExcluding(crop.getFarmId(), cropId);
            BigDecimal newTotal = otherCropsArea.add(request.getAreaAllocated());
            if (newTotal.compareTo(farm.getTotalArea()) > 0) {
                BigDecimal remaining = farm.getTotalArea().subtract(otherCropsArea);
                throw new BadRequestException(
                        String.format("Area exceeds farm capacity. Farm: %s %s, Other crops: %s, Available: %s, Requested: %s",
                                farm.getTotalArea(), farm.getAreaUnit(), otherCropsArea, remaining, request.getAreaAllocated())
                );
            }
        }

        String oldStage = crop.getCropStage();

        if (request.getCropName() != null) crop.setCropName(request.getCropName());
        if (request.getVariety() != null) crop.setVariety(request.getVariety());
        if (request.getSowingDate() != null) crop.setSowingDate(request.getSowingDate());
        if (request.getExpectedHarvest() != null) crop.setExpectedHarvest(request.getExpectedHarvest());
        if (request.getCropStage() != null) crop.setCropStage(request.getCropStage());
        if (request.getAreaAllocated() != null) crop.setAreaAllocated(request.getAreaAllocated());
        if (request.getIrrigationType() != null) crop.setIrrigationType(request.getIrrigationType());
        if (request.getStatus() != null) crop.setStatus(request.getStatus());

        crop = farmCropRepository.save(crop);
        log.info("Crop updated: id={}, farmId={}", cropId, crop.getFarmId());

        publishCropEvent("CROP_UPDATED", crop, farm);

        // Notify with stage change info if applicable
        String stageInfo = "";
        if (request.getCropStage() != null && !request.getCropStage().equals(oldStage)) {
            stageInfo = String.format(" Stage changed: %s → %s.", oldStage, crop.getCropStage());
        }
        publishNotification(farm.getUserId(), "CROP",
                "Crop Updated — " + crop.getCropName(),
                String.format("\"%s\" in farm \"%s\" has been updated.%s", crop.getCropName(), farm.getFarmName(), stageInfo));

        return toResponse(crop);
    }

    @Transactional
    public void deleteCrop(UUID cropId, UUID userId) {
        FarmCrop crop = ownershipValidator.validateCropOwnership(cropId, userId);
        Farm farm = ownershipValidator.validateFarmOwnership(crop.getFarmId(), userId);

        crop.setDeleted(true);
        farmCropRepository.save(crop);
        log.info("Crop soft-deleted: id={}, farmId={}", cropId, crop.getFarmId());

        publishCropEvent("CROP_REMOVED", crop, farm);

        // Compute remaining area after deletion
        BigDecimal newAllocated = farmCropRepository.sumAllocatedAreaByFarmId(crop.getFarmId());
        BigDecimal remaining = farm.getTotalArea() != null ? farm.getTotalArea().subtract(newAllocated) : null;
        String areaInfo = "";
        if (crop.getAreaAllocated() != null && remaining != null) {
            areaInfo = String.format(" %s %s freed. Remaining available: %s %s.",
                    crop.getAreaAllocated(), farm.getAreaUnit(), remaining, farm.getAreaUnit());
        }

        publishNotification(farm.getUserId(), "CROP",
                "Crop Removed — " + crop.getCropName(),
                String.format("\"%s\" has been removed from farm \"%s\".%s", crop.getCropName(), farm.getFarmName(), areaInfo));
    }

    private static final Set<String> TERMINAL_STAGES = Set.of("HARVESTED", "FAILED");

    @Transactional
    public FarmCropResponse changeCropStage(UUID cropId, UUID userId, String newStage) {
        if (!VALID_STAGES.contains(newStage)) {
            throw new BadRequestException("Invalid crop stage: " + newStage + ". Valid stages: " + VALID_STAGES);
        }

        FarmCrop crop = ownershipValidator.validateCropOwnership(cropId, userId);
        Farm farm = ownershipValidator.validateFarmOwnership(crop.getFarmId(), userId);

        String oldStage = crop.getCropStage();
        if (oldStage.equals(newStage)) {
            return toResponse(crop);
        }

        crop.setCropStage(newStage);

        // Terminal stages (HARVESTED/FAILED) → mark crop COMPLETED to free area
        if (TERMINAL_STAGES.contains(newStage)) {
            crop.setStatus("COMPLETED");
        } else if ("COMPLETED".equals(crop.getStatus())) {
            // If moving back from terminal to active stage, reactivate
            crop.setStatus("ACTIVE");
        }

        crop = farmCropRepository.save(crop);
        log.info("Crop stage changed: id={}, {} → {}, status={}", cropId, oldStage, newStage, crop.getStatus());

        publishCropEvent("CROP_UPDATED", crop, farm);

        // Build notification with area info for terminal stages
        String areaInfo = "";
        if (TERMINAL_STAGES.contains(newStage) && crop.getAreaAllocated() != null && farm.getTotalArea() != null) {
            BigDecimal newAllocated = farmCropRepository.sumAllocatedAreaByFarmId(crop.getFarmId());
            BigDecimal remaining = farm.getTotalArea().subtract(newAllocated);
            areaInfo = String.format(" %s %s freed. Available: %s %s.",
                    crop.getAreaAllocated(), farm.getAreaUnit(), remaining, farm.getAreaUnit());
        }

        String stageEmoji = switch (newStage) {
            case "SOWN" -> "Sown";
            case "GERMINATION" -> "Germinating";
            case "VEGETATIVE" -> "Growing";
            case "FLOWERING" -> "Flowering";
            case "FRUITING" -> "Fruiting";
            case "HARVEST_READY" -> "Ready to Harvest!";
            case "HARVESTED" -> "Harvested!";
            case "FAILED" -> "Failed";
            default -> newStage;
        };

        publishNotification(farm.getUserId(), "CROP",
                crop.getCropName() + " — " + stageEmoji,
                String.format("\"%s\" in \"%s\" moved from %s to %s.%s",
                        crop.getCropName(), farm.getFarmName(), oldStage, newStage, areaInfo));

        return toResponse(crop);
    }

    private FarmCropResponse toResponse(FarmCrop crop) {
        long daysSinceSowing = 0;
        if (crop.getSowingDate() != null && !crop.getSowingDate().isAfter(LocalDate.now())) {
            daysSinceSowing = ChronoUnit.DAYS.between(crop.getSowingDate(), LocalDate.now());
        }

        return FarmCropResponse.builder()
                .id(crop.getId().toString())
                .farmId(crop.getFarmId().toString())
                .cropName(crop.getCropName())
                .variety(crop.getVariety())
                .sowingDate(crop.getSowingDate() != null ? crop.getSowingDate().toString() : null)
                .expectedHarvest(crop.getExpectedHarvest() != null ? crop.getExpectedHarvest().toString() : null)
                .cropStage(crop.getCropStage())
                .areaAllocated(crop.getAreaAllocated())
                .irrigationType(crop.getIrrigationType())
                .status(crop.getStatus())
                .daysSinceSowing(daysSinceSowing)
                .createdAt(crop.getCreatedAt() != null ? crop.getCreatedAt().toString() : null)
                .updatedAt(crop.getUpdatedAt() != null ? crop.getUpdatedAt().toString() : null)
                .build();
    }

    private void publishCropEvent(String eventType, FarmCrop crop, Farm farm) {
        try {
            FarmCropEvent event = FarmCropEvent.builder()
                    .eventType(eventType)
                    .userId(farm.getUserId().toString())
                    .farmId(farm.getId().toString())
                    .cropId(crop.getId().toString())
                    .cropName(crop.getCropName())
                    .cropStage(crop.getCropStage())
                    .farmName(farm.getFarmName())
                    .village(farm.getVillage())
                    .district(farm.getDistrict())
                    .state(farm.getState())
                    .sowingDate(crop.getSowingDate() != null ? crop.getSowingDate().toString() : null)
                    .build();
            kafkaTemplate.send(KafkaTopics.FARM_CROP_EVENT_TOPIC, farm.getUserId().toString(), event);
        } catch (Exception e) {
            log.error("Failed to publish crop event: {}", eventType, e);
        }
    }

    private void publishNotification(UUID userId, String type, String title, String body) {
        try {
            NotificationEvent notification = NotificationEvent.builder()
                    .userId(userId.toString())
                    .type(type)
                    .title(title)
                    .body(body)
                    .build();
            kafkaTemplate.send(KafkaTopics.NOTIFICATION_TOPIC, userId.toString(), notification);
        } catch (Exception e) {
            log.error("Failed to publish notification: {}", title, e);
        }
    }
}
