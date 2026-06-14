package com.farmrakshak.market.repository;

import com.farmrakshak.market.entity.AlertHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface AlertHistoryRepository extends JpaRepository<AlertHistory, UUID> {
    List<AlertHistory> findByAlertIdOrderByTriggeredAtDesc(UUID alertId);
}
