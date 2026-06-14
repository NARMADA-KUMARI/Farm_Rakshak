package com.farmrakshak.crop.repository;

import com.farmrakshak.crop.entity.CropTask;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface CropTaskRepository extends JpaRepository<CropTask, UUID> {
    List<CropTask> findByUserCropIdOrderByDueDateAsc(UUID userCropId);
    List<CropTask> findByUserCropIdAndStatus(UUID userCropId, String status);
    boolean existsByUserCropIdAndTemplateId(UUID userCropId, UUID templateId);
}
