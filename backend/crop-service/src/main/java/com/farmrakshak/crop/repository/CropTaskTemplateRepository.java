package com.farmrakshak.crop.repository;

import com.farmrakshak.crop.entity.CropTaskTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface CropTaskTemplateRepository extends JpaRepository<CropTaskTemplate, UUID> {
    List<CropTaskTemplate> findByCropIdAndStageId(UUID cropId, UUID stageId);
    List<CropTaskTemplate> findByCropId(UUID cropId);
}
