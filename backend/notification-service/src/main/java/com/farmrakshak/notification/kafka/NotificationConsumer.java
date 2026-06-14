package com.farmrakshak.notification.kafka;

import com.farmrakshak.notification.entity.Notification;
import com.farmrakshak.notification.repository.NotificationRepository;
import com.farmrakshak.shared.constants.KafkaTopics;
import com.farmrakshak.shared.event.DiseaseAlertEvent;
import com.farmrakshak.shared.event.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Slf4j @Component @RequiredArgsConstructor
public class NotificationConsumer {
    private final NotificationRepository repository;

    @KafkaListener(topics = KafkaTopics.NOTIFICATION_TOPIC, groupId = "notification-group")
    public void consume(NotificationEvent event) {
        log.info("Received notification event for user: {}", event.getUserId());
        Notification notification = Notification.builder()
                .userId(UUID.fromString(event.getUserId()))
                .type(event.getType())
                .title(event.getTitle())
                .body(event.getBody())
                .build();
        repository.save(notification);
    }

    /**
     * Consumes disease alert events and creates notifications for nearby farmers.
     * Each notification includes: disease name, crop, reported location, distance, and prevention advice.
     */
    @KafkaListener(topics = KafkaTopics.DISEASE_ALERT_TOPIC, groupId = "notification-group")
    public void consumeDiseaseAlert(DiseaseAlertEvent event) {
        log.info("Received disease alert for user: {}, disease: {} on {}",
                event.getTargetUserId(), event.getDiseaseName(), event.getCropName());

        String title = "⚠️ Disease Alert: " + event.getDiseaseName() + " on " + event.getCropName();

        StringBuilder body = new StringBuilder();
        body.append(event.getDiseaseName())
            .append(" has been detected on ").append(event.getCropName())
            .append(" crops");

        // Add location info
        if (event.getReportedVillage() != null && !event.getReportedVillage().isEmpty()) {
            body.append(" near ").append(event.getReportedVillage());
        }
        if (event.getReportedDistrict() != null && !event.getReportedDistrict().isEmpty()) {
            body.append(", ").append(event.getReportedDistrict());
        }

        body.append(" (").append(String.format("%.1f", event.getDistanceKm())).append(" km from your farm).");

        // Add prevention advice
        if (event.getPrevention() != null && !event.getPrevention().isEmpty()) {
            body.append(" Prevention: ").append(String.join("; ", event.getPrevention())).append(".");
        }

        Notification notification = Notification.builder()
                .userId(UUID.fromString(event.getTargetUserId()))
                .type("DISEASE_ALERT")
                .title(title)
                .body(body.toString())
                .build();
        repository.save(notification);

        log.info("Disease alert notification saved for user: {}", event.getTargetUserId());
    }
}

