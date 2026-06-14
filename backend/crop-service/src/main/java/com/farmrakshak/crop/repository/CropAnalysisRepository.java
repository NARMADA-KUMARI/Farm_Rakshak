package com.farmrakshak.crop.repository;

import com.farmrakshak.crop.entity.CropAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface CropAnalysisRepository extends JpaRepository<CropAnalysis, UUID> {
    Optional<CropAnalysis> findByUploadId(UUID uploadId);
}
