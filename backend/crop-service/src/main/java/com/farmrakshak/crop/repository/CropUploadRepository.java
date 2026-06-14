package com.farmrakshak.crop.repository;

import com.farmrakshak.crop.entity.CropUpload;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface CropUploadRepository extends JpaRepository<CropUpload, UUID> {
    Page<CropUpload> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    long countByUserId(UUID userId);
}
