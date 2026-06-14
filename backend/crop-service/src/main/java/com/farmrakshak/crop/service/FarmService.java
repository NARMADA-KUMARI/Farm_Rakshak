package com.farmrakshak.crop.service;

import com.farmrakshak.crop.dto.*;
import com.farmrakshak.crop.entity.Farm;
import com.farmrakshak.crop.repository.FarmCropRepository;
import com.farmrakshak.crop.repository.FarmRepository;
import com.farmrakshak.shared.constants.KafkaTopics;
import com.farmrakshak.shared.event.FarmEvent;
import com.farmrakshak.shared.event.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FarmService {

    private final FarmRepository farmRepository;
    private final FarmCropRepository farmCropRepository;
    private final OwnershipValidator ownershipValidator;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public FarmResponse createFarm(UUID userId, CreateFarmRequest request) {
        Farm farm = Farm.builder()
                .userId(userId)
                .farmName(request.getFarmName())
                .village(request.getVillage())
                .district(request.getDistrict())
                .state(request.getState())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .totalArea(request.getTotalArea())
                .areaUnit(request.getAreaUnit() != null ? request.getAreaUnit() : "acres")
                .soilType(request.getSoilType())
                .build();

        farm = farmRepository.save(farm);
        log.info("Farm created: id={}, userId={}, name={}", farm.getId(), userId, farm.getFarmName());

        publishFarmEvent("FARM_CREATED", farm);
        publishNotification(farm.getUserId(), "FARM",
                "Farm Created",
                String.format("Your farm \"%s\" has been created successfully.%s",
                        farm.getFarmName(),
                        farm.getTotalArea() != null ? String.format(" Total area: %s %s.", farm.getTotalArea(), farm.getAreaUnit()) : ""));

        return toResponse(farm);
    }

    @Transactional(readOnly = true)
    public List<FarmSummaryResponse> getMyFarms(UUID userId) {
        List<Farm> farms = farmRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId);
        return farms.stream().map(farm -> {
            long cropCount = farmCropRepository.countByFarmIdAndDeletedFalse(farm.getId());
            BigDecimal allocated = farmCropRepository.sumAllocatedAreaByFarmId(farm.getId());
            BigDecimal remaining = farm.getTotalArea() != null ? farm.getTotalArea().subtract(allocated) : null;

            return FarmSummaryResponse.builder()
                    .id(farm.getId().toString())
                    .farmName(farm.getFarmName())
                    .village(farm.getVillage())
                    .district(farm.getDistrict())
                    .state(farm.getState())
                    .totalArea(farm.getTotalArea())
                    .areaUnit(farm.getAreaUnit())
                    .allocatedArea(allocated)
                    .remainingArea(remaining)
                    .cropCount(cropCount)
                    .build();
        }).toList();
    }

    @Transactional(readOnly = true)
    public FarmResponse getFarmById(UUID farmId, UUID userId) {
        Farm farm = ownershipValidator.validateFarmOwnership(farmId, userId);
        return toResponse(farm);
    }

    @Transactional
    public FarmResponse updateFarm(UUID farmId, UUID userId, UpdateFarmRequest request) {
        Farm farm = ownershipValidator.validateFarmOwnership(farmId, userId);

        if (request.getFarmName() != null) farm.setFarmName(request.getFarmName());
        if (request.getVillage() != null) farm.setVillage(request.getVillage());
        if (request.getDistrict() != null) farm.setDistrict(request.getDistrict());
        if (request.getState() != null) farm.setState(request.getState());
        if (request.getLatitude() != null) farm.setLatitude(request.getLatitude());
        if (request.getLongitude() != null) farm.setLongitude(request.getLongitude());
        if (request.getTotalArea() != null) farm.setTotalArea(request.getTotalArea());
        if (request.getAreaUnit() != null) farm.setAreaUnit(request.getAreaUnit());
        if (request.getSoilType() != null) farm.setSoilType(request.getSoilType());

        farm = farmRepository.save(farm);
        log.info("Farm updated: id={}, userId={}", farm.getId(), userId);

        publishFarmEvent("FARM_UPDATED", farm);
        publishNotification(farm.getUserId(), "FARM",
                "Farm Updated",
                String.format("Your farm \"%s\" has been updated.", farm.getFarmName()));

        return toResponse(farm);
    }

    @Transactional
    public void deleteFarm(UUID farmId, UUID userId) {
        Farm farm = ownershipValidator.validateFarmOwnership(farmId, userId);
        long cropCount = farmCropRepository.countByFarmIdAndDeletedFalse(farmId);

        // Cascade soft-delete all crops in the farm
        farmCropRepository.softDeleteAllByFarmId(farmId);

        farm.setDeleted(true);
        farmRepository.save(farm);
        log.info("Farm soft-deleted (with crops): id={}, userId={}", farmId, userId);

        publishFarmEvent("FARM_DELETED", farm);
        publishNotification(farm.getUserId(), "FARM",
                "Farm Deleted",
                String.format("Your farm \"%s\" and its %d crop(s) have been removed.", farm.getFarmName(), cropCount));
    }

    private FarmResponse toResponse(Farm farm) {
        BigDecimal allocated = farmCropRepository.sumAllocatedAreaByFarmId(farm.getId());
        BigDecimal remaining = farm.getTotalArea() != null ? farm.getTotalArea().subtract(allocated) : null;
        long cropCount = farmCropRepository.countByFarmIdAndDeletedFalse(farm.getId());

        return FarmResponse.builder()
                .id(farm.getId().toString())
                .userId(farm.getUserId().toString())
                .farmName(farm.getFarmName())
                .village(farm.getVillage())
                .district(farm.getDistrict())
                .state(farm.getState())
                .latitude(farm.getLatitude())
                .longitude(farm.getLongitude())
                .totalArea(farm.getTotalArea())
                .areaUnit(farm.getAreaUnit())
                .soilType(farm.getSoilType())
                .allocatedArea(allocated)
                .remainingArea(remaining)
                .cropCount(cropCount)
                .createdAt(farm.getCreatedAt() != null ? farm.getCreatedAt().toString() : null)
                .updatedAt(farm.getUpdatedAt() != null ? farm.getUpdatedAt().toString() : null)
                .build();
    }

    private void publishFarmEvent(String eventType, Farm farm) {
        try {
            FarmEvent event = FarmEvent.builder()
                    .eventType(eventType)
                    .userId(farm.getUserId().toString())
                    .farmId(farm.getId().toString())
                    .farmName(farm.getFarmName())
                    .build();
            kafkaTemplate.send(KafkaTopics.FARM_EVENT_TOPIC, farm.getUserId().toString(), event);
        } catch (Exception e) {
            log.error("Failed to publish farm event: {}", eventType, e);
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
