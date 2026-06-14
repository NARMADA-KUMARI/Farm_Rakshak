package com.farmrakshak.market.scheduler;

import com.farmrakshak.market.service.AlertService;
import com.farmrakshak.market.service.DataIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Scheduled data ingestion pipeline — runs every 6 hours + seeds historical data on startup.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PriceIngestionScheduler {

    private final DataIngestionService ingestionService;
    private final AlertService alertService;

    /**
     * On application startup, seed 30 days of historical data if missing.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void seedHistoricalData() {
        try {
            log.info("Seeding historical price data (30 days)...");
            int count = ingestionService.ingestAll(LocalDate.now().minusDays(30), LocalDate.now());
            log.info("Historical data seeding complete: {} records", count);
        } catch (Exception e) {
            log.error("Failed to seed historical data: {}", e.getMessage(), e);
        }
    }

    /**
     * Fetch fresh prices every 6 hours.
     */
    @Scheduled(cron = "${market.ingestion.cron:0 0 */6 * * *}")
    public void scheduledIngestion() {
        try {
            log.info("Scheduled price ingestion starting...");
            int count = ingestionService.ingestToday();
            log.info("Scheduled ingestion complete: {} new records", count);

            // Check price alerts after ingestion
            int alerts = alertService.checkAndTriggerAlerts();
            if (alerts > 0) {
                log.info("Triggered {} price alerts", alerts);
            }
        } catch (Exception e) {
            log.error("Scheduled ingestion failed: {}", e.getMessage(), e);
        }
    }
}
