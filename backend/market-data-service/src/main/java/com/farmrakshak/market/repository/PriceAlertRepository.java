package com.farmrakshak.market.repository;

import com.farmrakshak.market.entity.PriceAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface PriceAlertRepository extends JpaRepository<PriceAlert, UUID> {
    List<PriceAlert> findByUserIdOrderByCreatedAtDesc(UUID userId);
    List<PriceAlert> findByIsActiveTrueAndCropNameIgnoreCase(String cropName);
    List<PriceAlert> findByIsActiveTrue();
}
