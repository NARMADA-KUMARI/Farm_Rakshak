package com.farmrakshak.crop.repository;

import com.farmrakshak.crop.entity.Farm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FarmRepository extends JpaRepository<Farm, UUID> {

    List<Farm> findByUserIdAndDeletedFalseOrderByCreatedAtDesc(UUID userId);

    Optional<Farm> findByIdAndDeletedFalse(UUID id);

    Optional<Farm> findByIdAndUserIdAndDeletedFalse(UUID id, UUID userId);

    boolean existsByIdAndUserIdAndDeletedFalse(UUID id, UUID userId);

    /**
     * Find all farms that grow a specific crop (ACTIVE), have GPS coordinates,
     * and belong to users other than the reporter.
     * Used by DiseaseAlertService to find farms within 100km radius.
     */
    @Query("SELECT DISTINCT f FROM Farm f JOIN FarmCrop fc ON fc.farmId = f.id " +
           "WHERE LOWER(fc.cropName) = LOWER(:cropName) " +
           "AND fc.status = 'ACTIVE' AND fc.deleted = false " +
           "AND f.deleted = false " +
           "AND f.latitude IS NOT NULL AND f.longitude IS NOT NULL " +
           "AND f.userId <> :excludeUserId")
    List<Farm> findFarmsWithCropExcludingUser(
            @Param("cropName") String cropName,
            @Param("excludeUserId") UUID excludeUserId);
}

