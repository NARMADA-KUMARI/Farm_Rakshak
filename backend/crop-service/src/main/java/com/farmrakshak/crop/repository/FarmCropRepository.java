package com.farmrakshak.crop.repository;

import com.farmrakshak.crop.entity.FarmCrop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FarmCropRepository extends JpaRepository<FarmCrop, UUID> {

    List<FarmCrop> findByFarmIdAndDeletedFalseOrderByCreatedAtDesc(UUID farmId);

    Optional<FarmCrop> findByIdAndDeletedFalse(UUID id);

    long countByFarmIdAndDeletedFalse(UUID farmId);

    long countByFarmIdAndDeletedFalseAndStatus(UUID farmId, String status);

    // Only ACTIVE crops occupy farm area — HARVESTED/FAILED crops free the land
    @Query("SELECT COALESCE(SUM(fc.areaAllocated), 0) FROM FarmCrop fc WHERE fc.farmId = :farmId AND fc.deleted = false AND fc.status = 'ACTIVE'")
    BigDecimal sumAllocatedAreaByFarmId(@Param("farmId") UUID farmId);

    @Query("SELECT COALESCE(SUM(fc.areaAllocated), 0) FROM FarmCrop fc WHERE fc.farmId = :farmId AND fc.deleted = false AND fc.status = 'ACTIVE' AND fc.id <> :excludeCropId")
    BigDecimal sumAllocatedAreaByFarmIdExcluding(@Param("farmId") UUID farmId, @Param("excludeCropId") UUID excludeCropId);

    @Modifying
    @Query("UPDATE FarmCrop fc SET fc.deleted = true WHERE fc.farmId = :farmId")
    void softDeleteAllByFarmId(@Param("farmId") UUID farmId);
}
