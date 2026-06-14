package com.farmrakshak.aiclient.service;

import com.farmrakshak.shared.constants.KafkaTopics;
import com.farmrakshak.shared.event.AnalysisResultEvent;
import com.farmrakshak.shared.event.NotificationEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AiAnalysisService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final WebClient mistralWebClient;

    @Value("${mistral.api.key}")
    private String mistralApiKey;

    @Value("${mistral.model:pixtral-12b-latest}")
    private String mistralModel;

    private static final int MAX_IMAGE_DIMENSION = 800;

    public AiAnalysisService(KafkaTemplate<String, Object> kafkaTemplate,
                             RestTemplate restTemplate,
                             ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.mistralWebClient = WebClient.builder()
                .baseUrl("https://api.mistral.ai")
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
    }

    public void analyzeImage(String analysisId, String imageUrl, String userId) {
        log.info("Starting Mistral Pixtral analysis: {} with model: {}", analysisId, mistralModel);

        try {
            // 1. Download the image bytes from MinIO
            byte[] imageBytes = restTemplate.getForObject(URI.create(imageUrl), byte[].class);
            if (imageBytes == null) throw new RuntimeException("Failed to download image from: " + imageUrl);
            log.info("Image downloaded: {} bytes", imageBytes.length);

            // 2. Compress/resize the image to reduce payload (free-tier token limits)
            byte[] compressedBytes = compressImage(imageBytes);
            log.info("Image compressed: {} bytes -> {} bytes", imageBytes.length, compressedBytes.length);

            String base64Image = Base64.getEncoder().encodeToString(compressedBytes);

            // 3. Build Mistral vision API request
            String promptText = """
                    You are an expert plant pathologist and crop identifier. Analyze this crop image.
                    First identify the crop/plant species, then check for diseases, pests, or nutrient deficiencies.
                    Respond ONLY with a valid JSON object (no markdown, no backticks):
                    {
                      "cropName": "Name of the crop/plant (e.g., Tomato, Rice, Wheat, Cotton)",
                      "disease": "Name of disease or Healthy",
                      "confidence": 0.95,
                      "description": "Short description of symptoms observed",
                      "treatment": ["Treatment step 1", "Treatment step 2"],
                      "prevention": ["Prevention step 1", "Prevention step 2"]
                    }
                    """;

            Map<String, Object> requestBody = Map.of(
                "model", mistralModel,
                "messages", List.of(
                    Map.of("role", "user", "content", List.of(
                        Map.of("type", "text", "text", promptText),
                        Map.of("type", "image_url", "image_url", Map.of(
                            "url", "data:image/jpeg;base64," + base64Image
                        ))
                    ))
                ),
                "max_tokens", 512,
                "temperature", 0.2
            );

            // 4. Call Mistral API with retry for 429
            String responseJson = callMistralWithRetry(requestBody, 3);
            log.info("Mistral API responded for: {}", analysisId);

            // 5. Parse the Mistral response
            @SuppressWarnings("unchecked")
            Map<String, Object> mistralResponse = objectMapper.readValue(responseJson, Map.class);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices = (List<Map<String, Object>>) mistralResponse.get("choices");
            if (choices == null || choices.isEmpty()) {
                throw new RuntimeException("Empty response from Mistral API");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            String responseText = (String) message.get("content");

            // Strip markdown code fences
            responseText = responseText.replaceAll("^```json\\s*", "").replaceAll("^```\\s*", "").replaceAll("\\s*```$", "").trim();
            log.info("Parsed response: {}", responseText);

            @SuppressWarnings("unchecked")
            Map<String, Object> parsedRes = objectMapper.readValue(responseText, Map.class);

            // 6. Publish result
            String detectedCrop = (String) parsedRes.get("cropName");

            AnalysisResultEvent resultEvent = AnalysisResultEvent.builder()
                    .analysisId(analysisId)
                    .userId(userId)
                    .status("COMPLETED")
                    .cropName(detectedCrop)
                    .diseaseName((String) parsedRes.get("disease"))
                    .confidence(parsedRes.get("confidence") != null ? ((Number) parsedRes.get("confidence")).doubleValue() : null)
                    .description((String) parsedRes.get("description"))
                    .treatment(parsedRes.get("treatment") instanceof List ? (List<String>) parsedRes.get("treatment") : null)
                    .prevention(parsedRes.get("prevention") instanceof List ? (List<String>) parsedRes.get("prevention") : null)
                    .build();

            kafkaTemplate.send(KafkaTopics.ANALYSIS_RESULT_TOPIC, userId, resultEvent);

            String notifBody = detectedCrop != null
                    ? "Crop: " + detectedCrop + " — " + parsedRes.get("disease")
                    : "Disease detected: " + parsedRes.get("disease");

            NotificationEvent notification = NotificationEvent.builder()
                    .userId(userId)
                    .type("ANALYSIS_COMPLETE")
                    .title("Crop Analysis Complete")
                    .body(notifBody)
                    .build();
            kafkaTemplate.send(KafkaTopics.NOTIFICATION_TOPIC, userId, notification);

            log.info("Analysis completed: {} -> {}", analysisId, parsedRes.get("disease"));
        } catch (Exception e) {
            log.error("Failed analysis for {}: {}", analysisId, e.getMessage(), e);
            AnalysisResultEvent failedEvent = AnalysisResultEvent.builder()
                    .analysisId(analysisId)
                    .userId(userId)
                    .status("FAILED")
                    .build();
            kafkaTemplate.send(KafkaTopics.ANALYSIS_RESULT_TOPIC, userId, failedEvent);
        }
    }

    /**
     * Compress and resize the image to reduce payload size.
     * Resizes to max 800x800 and re-encodes as JPEG at ~0.7 quality.
     */
    private byte[] compressImage(byte[] originalBytes) {
        try {
            BufferedImage original = ImageIO.read(new ByteArrayInputStream(originalBytes));
            if (original == null) {
                log.warn("Could not decode image, using original bytes");
                return originalBytes;
            }

            int width = original.getWidth();
            int height = original.getHeight();

            // Only resize if larger than MAX_IMAGE_DIMENSION
            if (width > MAX_IMAGE_DIMENSION || height > MAX_IMAGE_DIMENSION) {
                double scale = Math.min((double) MAX_IMAGE_DIMENSION / width, (double) MAX_IMAGE_DIMENSION / height);
                width = (int) (width * scale);
                height = (int) (height * scale);

                BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = resized.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.drawImage(original, 0, 0, width, height, null);
                g.dispose();
                original = resized;
            }

            // Re-encode as JPEG
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(original, "jpg", baos);
            return baos.toByteArray();

        } catch (Exception e) {
            log.warn("Image compression failed, using original: {}", e.getMessage());
            return originalBytes;
        }
    }

    /**
     * Calls Mistral API with manual retry for 429 rate limits.
     * Waits progressively: 15s, 30s, 60s.
     */
    private String callMistralWithRetry(Map<String, Object> requestBody, int maxRetries) {
        Exception lastException = null;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                if (attempt > 0) {
                    long waitSeconds = 15L * (long) Math.pow(2, attempt - 1);
                    log.info("Rate limited, waiting {}s before retry {}/{}", waitSeconds, attempt, maxRetries);
                    Thread.sleep(waitSeconds * 1000);
                }

                return mistralWebClient.post()
                        .uri("/v1/chat/completions")
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + mistralApiKey)
                        .bodyValue(requestBody)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();

            } catch (WebClientResponseException.TooManyRequests e) {
                log.warn("429 rate limited, attempt {}/{}", attempt + 1, maxRetries + 1);
                lastException = e;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Retry interrupted", e);
            }
        }

        throw new RuntimeException("Mistral API rate limited after " + (maxRetries + 1) + " attempts", lastException);
    }
}
