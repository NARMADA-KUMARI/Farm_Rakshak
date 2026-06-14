package com.farmrakshak.crop.repository;

import com.farmrakshak.crop.entity.CropStageMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CropStageMasterRepository extends JpaRepository<CropStageMaster, UUID> {
    List<CropStageMaster> findByCropIdOrderByStageOrderAsc(UUID cropId);

    @Query("SELECT s FROM CropStageMaster s WHERE s.cropId = :cropId " +
           "AND :progressPercent >= s.startDayPercentage AND :progressPercent < s.endDayPercentage")
    Optional<CropStageMaster> findStageByProgress(@Param("cropId") UUID cropId,
                                                   @Param("progressPercent") BigDecimal progressPercent);
}
