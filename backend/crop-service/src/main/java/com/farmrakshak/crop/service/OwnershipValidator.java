package com.farmrakshak.crop.service;

import com.farmrakshak.crop.entity.Farm;
import com.farmrakshak.crop.entity.FarmCrop;
import com.farmrakshak.crop.repository.FarmCropRepository;
import com.farmrakshak.crop.repository.FarmRepository;
import com.farmrakshak.shared.exception.ForbiddenException;
import com.farmrakshak.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OwnershipValidator {

    private final FarmRepository farmRepository;
    private final FarmCropRepository farmCropRepository;

    /**
     * Validates that the farm exists, is not deleted, and belongs to the given user.
     * Returns the farm if valid.
     */
    public Farm validateFarmOwnership(UUID farmId, UUID userId) {
        Farm farm = farmRepository.findByIdAndDeletedFalse(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm", "id", farmId.toString()));

        if (!farm.getUserId().equals(userId)) {
            throw new ForbiddenException("You do not have access to this farm");
        }
        return farm;
    }

    /**
     * Validates that the crop exists and its parent farm belongs to the given user.
     * Returns the crop if valid.
     */
    public FarmCrop validateCropOwnership(UUID cropId, UUID userId) {
        FarmCrop crop = farmCropRepository.findByIdAndDeletedFalse(cropId)
                .orElseThrow(() -> new ResourceNotFoundException("Crop", "id", cropId.toString()));

        // Validate ownership through the farm
        validateFarmOwnership(crop.getFarmId(), userId);
        return crop;
    }
}
