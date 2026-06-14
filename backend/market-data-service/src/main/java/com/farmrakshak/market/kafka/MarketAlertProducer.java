package com.farmrakshak.market.kafka;

import com.farmrakshak.shared.constants.KafkaTopics;
import com.farmrakshak.shared.event.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketAlertProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendAlert(String userId, String cropName, BigDecimal price, String mandiName, String direction) {
        try {
            String title = String.format("Price Alert: %s", cropName);
            String body = String.format("%s crossed ₹%s in %s (%s threshold)",
                    cropName, price.toPlainString(), mandiName, direction.toLowerCase());

            NotificationEvent event = NotificationEvent.builder()
                    .userId(userId)
                    .type("MARKET")
                    .title(title)
                    .body(body)
                    .build();

            kafkaTemplate.send(KafkaTopics.NOTIFICATION_TOPIC, userId, event);
            log.info("Market alert sent: userId={}, crop={}, price={}", userId, cropName, price);
        } catch (Exception e) {
            log.error("Failed to send market alert for userId={}: {}", userId, e.getMessage());
        }
    }
}
