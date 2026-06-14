package com.farmrakshak.crop.kafka;

import com.farmrakshak.crop.service.CropService;
import com.farmrakshak.shared.constants.KafkaTopics;
import com.farmrakshak.shared.event.AnalysisResultEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalysisResultConsumer {

    private final CropService cropService;

    @KafkaListener(topics = KafkaTopics.ANALYSIS_RESULT_TOPIC, groupId = "crop-result-group")
    public void consume(AnalysisResultEvent event) {
        log.info("Received analysis result: analysisId={}", event.getAnalysisId());
        try {
            cropService.processAnalysisResult(event);
        } catch (Exception e) {
            log.error("Failed to process analysis result", e);
        }
    }
}
