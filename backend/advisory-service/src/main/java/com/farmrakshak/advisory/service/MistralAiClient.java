package com.farmrakshak.advisory.service;

import com.farmrakshak.advisory.dto.CropContext;
import com.farmrakshak.advisory.dto.Suggestion;
import com.farmrakshak.advisory.dto.WeatherContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Mistral AI client for generating intelligent crop advisory suggestions.
 * Falls back gracefully to empty list on API failure.
 */
@Slf4j
@Service
public class MistralAiClient {

    @Value("${mistral.api-key:}")
    private String apiKey;

    @Value("${mistral.model:mistral-small-latest}")
    private String model;

    @Value("${mistral.url:https://api.mistral.ai/v1/chat/completions}")
    private String apiUrl;

    private final WebClient webClient;

    public MistralAiClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public List<Suggestion> generateSuggestions(List<CropContext> crops, WeatherContext weather) {
        if (!isConfigured()) {
            log.info("Mistral API key not configured — skipping AI suggestions");
            return Collections.emptyList();
        }
        try {
            String prompt = buildPrompt(crops, weather);
            String response = callApi(prompt);
            return parseResponse(response);
        } catch (Exception e) {
            log.error("Mistral AI call failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    String buildPrompt(List<CropContext> crops, WeatherContext weather) {
        StringBuilder sb = new StringBuilder();
        sb.append("Act as an agriculture expert and provide practical farming advice based on the COMPLETE farmer context below.\n\n");

        sb.append("═══ FARMER'S FARMS & CROPS ═══\n");

        // Group crops by farm
        Map<String, List<CropContext>> byFarm = new java.util.LinkedHashMap<>();
        for (CropContext c : crops) {
            String farmKey = c.getFarmName() != null ? c.getFarmName() : "Unknown Farm";
            byFarm.computeIfAbsent(farmKey, k -> new ArrayList<>()).add(c);
        }

        int cropNum = 1;
        for (Map.Entry<String, List<CropContext>> entry : byFarm.entrySet()) {
            CropContext first = entry.getValue().get(0);
            sb.append(String.format("\n── Farm: %s ──\n", entry.getKey()));
            if (first.getVillage() != null || first.getDistrict() != null || first.getState() != null) {
                sb.append(String.format("   Location: %s%s%s\n",
                        first.getVillage() != null ? first.getVillage() : "",
                        first.getDistrict() != null ? ", " + first.getDistrict() : "",
                        first.getState() != null ? ", " + first.getState() : ""));
            }
            if (first.getFarmTotalArea() > 0) {
                sb.append(String.format("   Farm Size: %.1f %s | Soil: %s\n",
                        first.getFarmTotalArea(),
                        first.getAreaUnit() != null ? first.getAreaUnit() : "acres",
                        first.getSoilType() != null ? first.getSoilType() : "Unknown"));
            }

            for (CropContext c : entry.getValue()) {
                sb.append(String.format("%d. %s — Stage: %s (Day %d since sowing)\n",
                        cropNum++, c.getCropName(), c.getCropStage(), c.getDaysSinceSowing()));
                if (c.getAreaAllocated() > 0) {
                    sb.append(String.format("   Area: %.1f %s | Irrigation: %s\n",
                            c.getAreaAllocated(),
                            first.getAreaUnit() != null ? first.getAreaUnit() : "acres",
                            c.getIrrigationType() != null ? c.getIrrigationType() : "Unknown"));
                } else if (c.getIrrigationType() != null) {
                    sb.append(String.format("   Irrigation: %s\n", c.getIrrigationType()));
                }

                if (c.getDiseaseHistory() != null && !c.getDiseaseHistory().isEmpty()) {
                    sb.append("   Disease History:\n");
                    for (CropContext.DiseaseRecord d : c.getDiseaseHistory()) {
                        sb.append(String.format("   - %s: %s (Confidence: %.0f%%)\n",
                                d.getAnalyzedAt() != null ? d.getAnalyzedAt() : "Recent",
                                d.getDiseaseName(), d.getConfidence() * 100));
                        if (d.getTreatment() != null) {
                            sb.append("     Treatment given: ").append(d.getTreatment()).append("\n");
                        }
                    }
                }
            }
            sb.append("\n");
        }

        sb.append("═══ CURRENT WEATHER ═══\n");
        if (weather != null) {
            sb.append(String.format("Temperature: %.1f°C (Feels like: %.1f°C) | Humidity: %.0f%% | Wind: %.1f km/h\n",
                    weather.getTemperature(), weather.getFeelsLike(), weather.getHumidity(), weather.getWindSpeed()));
            sb.append(String.format("Condition: %s | Rain: %s | Clouds: %d%%\n",
                    weather.getCondition() != null ? weather.getCondition() : "Unknown",
                    weather.isRainForecast() ? "Yes" : "No", weather.getCloudCover()));
            sb.append(String.format("Location: %s | Lat %.3f, Lon %.3f\n\n",
                    weather.getCityName() != null ? weather.getCityName() : "Unknown", weather.getLat(), weather.getLon()));
        } else {
            sb.append("Weather data unavailable.\n\n");
        }

        sb.append("Provide response in EXACTLY this structured format for EACH crop. Use these exact category labels:\n\n");
        for (CropContext c : crops) {
            sb.append(String.format("[CROP: %s]\n", c.getCropName()));
            sb.append("Irrigation Advice: <advice>\n");
            sb.append("Fertilizer Advice: <advice>\n");
            sb.append("Disease Risk: <risk level HIGH/MEDIUM/LOW> - <advice>\n");
            sb.append("Pest Risk: <risk level HIGH/MEDIUM/LOW> - <advice>\n");
            sb.append("Weather Precaution: <advice>\n");
            sb.append("General Recommendation: <advice>\n\n");
        }

        sb.append("Rules:\n");
        sb.append("- Keep advice short and practical for Indian farming conditions.\n");
        sb.append("- Consider each farm's specific location, soil type, and area when advising.\n");
        sb.append("- If a disease was previously detected, advise ongoing monitoring.\n");
        sb.append("- Factor in the crop's allocated area — larger areas may need different approaches.\n");
        sb.append("- Provide region-specific advice based on the farm's village/district/state.\n");
        sb.append("- Provide actionable recommendations. Avoid technical jargon.\n");

        return sb.toString();
    }

    private String callApi(String prompt) {
        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "temperature", 0.3,
                "max_tokens", 2000
        );

        Map<?, ?> response = webClient.post()
                .uri(apiUrl)
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null) return "";

        List<?> choices = (List<?>) response.get("choices");
        if (choices == null || choices.isEmpty()) return "";

        Map<?, ?> choice = (Map<?, ?>) choices.get(0);
        Map<?, ?> message = (Map<?, ?>) choice.get("message");
        return message != null ? (String) message.get("content") : "";
    }

    @SuppressWarnings("unchecked")
    List<Suggestion> parseResponse(String response) {
        if (response == null || response.isBlank()) return Collections.emptyList();

        List<Suggestion> suggestions = new ArrayList<>();
        String[] lines = response.split("\n");

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            String category = null;
            String message = null;
            String risk = "MEDIUM";

            if (line.startsWith("Irrigation Advice:")) {
                category = "IRRIGATION";
                message = line.substring("Irrigation Advice:".length()).trim();
            } else if (line.startsWith("Fertilizer Advice:")) {
                category = "FERTILIZER";
                message = line.substring("Fertilizer Advice:".length()).trim();
            } else if (line.startsWith("Disease Risk:")) {
                category = "DISEASE_RISK";
                message = line.substring("Disease Risk:".length()).trim();
                risk = extractRisk(message);
            } else if (line.startsWith("Pest Risk:")) {
                category = "PEST_RISK";
                message = line.substring("Pest Risk:".length()).trim();
                risk = extractRisk(message);
            } else if (line.startsWith("Weather Precaution:")) {
                category = "WEATHER";
                message = line.substring("Weather Precaution:".length()).trim();
            } else if (line.startsWith("General Recommendation:")) {
                category = "GENERAL";
                message = line.substring("General Recommendation:".length()).trim();
            }

            if (category != null && message != null && !message.isEmpty()) {
                suggestions.add(Suggestion.builder()
                        .category(category).message(message).riskLevel(risk).build());
            }
        }

        return suggestions;
    }

    private String extractRisk(String text) {
        if (text == null) return "MEDIUM";
        String upper = text.toUpperCase();
        if (upper.contains("HIGH")) return "HIGH";
        if (upper.contains("LOW")) return "LOW";
        return "MEDIUM";
    }
}
