package com.farmrakshak.crop.repository;

import com.farmrakshak.crop.entity.UserCrop;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface UserCropRepository extends JpaRepository<UserCrop, UUID> {
    List<UserCrop> findByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, String status);
    Page<UserCrop> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    List<UserCrop> findByStatus(String status);
}
