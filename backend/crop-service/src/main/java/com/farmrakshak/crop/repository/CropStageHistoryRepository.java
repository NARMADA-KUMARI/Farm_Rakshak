package com.farmrakshak.crop.repository;

import com.farmrakshak.crop.entity.CropStageHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CropStageHistoryRepository extends JpaRepository<CropStageHistory, UUID> {
    List<CropStageHistory> findByUserCropIdOrderByStartDateAsc(UUID userCropId);
    Optional<CropStageHistory> findByUserCropIdAndEndDateIsNull(UUID userCropId);
}
