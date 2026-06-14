package com.farmrakshak.aiclient.kafka;

import com.farmrakshak.aiclient.service.AiAnalysisService;
import com.farmrakshak.shared.constants.KafkaTopics;
import com.farmrakshak.shared.event.CropAnalysisEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CropAnalysisConsumer {

    private final AiAnalysisService aiAnalysisService;

    @KafkaListener(topics = KafkaTopics.CROP_ANALYSIS_TOPIC, groupId = "ai-client-group")
    public void consume(CropAnalysisEvent event) {
        log.info("Received crop analysis event: {}", event.getAnalysisId());
        aiAnalysisService.analyzeImage(event.getAnalysisId(), event.getImageUrl(), event.getUserId());
    }
}
