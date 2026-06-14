package com.farmrakshak.crop.repository;

import com.farmrakshak.crop.entity.CropMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface CropMasterRepository extends JpaRepository<CropMaster, UUID> {
    Optional<CropMaster> findByCropNameIgnoreCase(String cropName);
}
