package com.farmrakshak.market.service;

import com.farmrakshak.market.dto.PriceAlertRequest;
import com.farmrakshak.market.dto.PriceAlertResponse;
import com.farmrakshak.market.entity.*;
import com.farmrakshak.market.kafka.MarketAlertProducer;
import com.farmrakshak.market.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private final PriceAlertRepository alertRepository;
    private final AlertHistoryRepository historyRepository;
    private final CropPriceRepository cropPriceRepository;
    private final MandiRepository mandiRepository;
    private final MarketAlertProducer alertProducer;

    @Transactional
    public PriceAlertResponse createAlert(UUID userId, PriceAlertRequest request) {
        PriceAlert alert = PriceAlert.builder()
                .userId(userId)
                .cropName(request.getCropName())
                .mandiId(request.getMandiId() != null ? UUID.fromString(request.getMandiId()) : null)
                .thresholdPrice(request.getThresholdPrice())
                .direction(request.getDirection() != null ? request.getDirection() : "ABOVE")
                .build();

        alert = alertRepository.save(alert);
        log.info("Price alert created: id={}, userId={}, crop={}, threshold={}",
                alert.getId(), userId, request.getCropName(), request.getThresholdPrice());

        String mandiName = null;
        if (alert.getMandiId() != null) {
            mandiName = mandiRepository.findById(alert.getMandiId())
                    .map(Mandi::getName).orElse(null);
        }

        return toResponse(alert, mandiName);
    }

    public List<PriceAlertResponse> getUserAlerts(UUID userId) {
        return alertRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(a -> {
                    String mandiName = null;
                    if (a.getMandiId() != null) {
                        mandiName = mandiRepository.findById(a.getMandiId())
                                .map(Mandi::getName).orElse(null);
                    }
                    return toResponse(a, mandiName);
                })
                .toList();
    }

    /**
     * Check all active alerts against current prices and trigger notifications.
     * Called by the scheduler after each ingestion.
     */
    @Transactional
    public int checkAndTriggerAlerts() {
        List<PriceAlert> active = alertRepository.findByIsActiveTrue();
        int triggered = 0;
        LocalDate today = LocalDate.now();

        for (PriceAlert alert : active) {
            List<CropPrice> prices = cropPriceRepository
                    .findByCropAndDate(alert.getCropName(), today);

            if (prices.isEmpty()) {
                prices = cropPriceRepository.findByCropAndDate(alert.getCropName(), today.minusDays(1));
            }

            for (CropPrice price : prices) {
                // Check mandi filter
                if (alert.getMandiId() != null && !alert.getMandiId().equals(price.getMandiId())) {
                    continue;
                }

                boolean shouldTrigger = false;
                if ("ABOVE".equals(alert.getDirection())) {
                    shouldTrigger = price.getPriceModal().compareTo(alert.getThresholdPrice()) >= 0;
                } else {
                    shouldTrigger = price.getPriceModal().compareTo(alert.getThresholdPrice()) <= 0;
                }

                if (shouldTrigger) {
                    String mandiName = mandiRepository.findById(price.getMandiId())
                            .map(Mandi::getName).orElse("Unknown Mandi");

                    // Record history
                    historyRepository.save(AlertHistory.builder()
                            .alertId(alert.getId())
                            .triggeredPrice(price.getPriceModal())
                            .mandiName(mandiName)
                            .build());

                    // Publish Kafka notification
                    alertProducer.sendAlert(alert.getUserId().toString(), alert.getCropName(),
                            price.getPriceModal(), mandiName, alert.getDirection());

                    triggered++;
                    log.info("Alert triggered: alertId={}, crop={}, price={}, mandi={}",
                            alert.getId(), alert.getCropName(), price.getPriceModal(), mandiName);
                    break; // One trigger per alert per check
                }
            }
        }
        return triggered;
    }

    private PriceAlertResponse toResponse(PriceAlert a, String mandiName) {
        return PriceAlertResponse.builder()
                .id(a.getId().toString())
                .cropName(a.getCropName())
                .mandiId(a.getMandiId() != null ? a.getMandiId().toString() : null)
                .mandiName(mandiName)
                .thresholdPrice(a.getThresholdPrice())
                .direction(a.getDirection())
                .isActive(a.getIsActive())
                .createdAt(a.getCreatedAt() != null ? a.getCreatedAt().toString() : null)
                .build();
    }
}
