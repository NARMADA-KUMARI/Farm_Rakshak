package com.farmrakshak.advisory.service;

import com.farmrakshak.advisory.dto.CropRecommendationRequest;
import com.farmrakshak.advisory.dto.CropRecommendationResponse;
import com.farmrakshak.advisory.dto.CropRecommendationResponse.RecommendedCrop;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.time.Month;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class CropRecommendationService {

    @Value("${mistral.api-key:}")
    private String apiKey;

    @Value("${mistral.model:mistral-small-latest}")
    private String model;

    @Value("${mistral.url:https://api.mistral.ai/v1/chat/completions}")
    private String apiUrl;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public CropRecommendationService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    public CropRecommendationResponse getRecommendations(CropRecommendationRequest request) {
        String season = detectSeason();

        if (apiKey == null || apiKey.isBlank()) {
            log.info("Mistral API key not configured — returning default recommendations");
            return getDefaultRecommendations(request, season);
        }

        try {
            String prompt = buildPrompt(request, season);
            String response = callApi(prompt);
            CropRecommendationResponse result = parseResponse(response, season);
            if (result.getRecommendations() == null || result.getRecommendations().isEmpty()) {
                return getDefaultRecommendations(request, season);
            }
            result.setSource("AI");
            return result;
        } catch (Exception e) {
            log.error("AI crop recommendation failed: {}", e.getMessage());
            return getDefaultRecommendations(request, season);
        }
    }

    private String detectSeason() {
        Month month = LocalDate.now().getMonth();
        return switch (month) {
            case JUNE, JULY, AUGUST, SEPTEMBER -> "Kharif";
            case OCTOBER, NOVEMBER, DECEMBER, JANUARY -> "Rabi";
            case FEBRUARY, MARCH, APRIL, MAY -> "Zaid/Summer";
        };
    }

    private String buildPrompt(CropRecommendationRequest req, String season) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are an expert Indian agriculture advisor. Recommend the top 5 best crops for a farmer.\n\n");
        sb.append("FARM DETAILS:\n");
        sb.append(String.format("- Location: %s, %s, %s\n",
                req.getVillage() != null ? req.getVillage() : "N/A",
                req.getDistrict() != null ? req.getDistrict() : "N/A",
                req.getState() != null ? req.getState() : "India"));
        sb.append(String.format("- Current Season: %s\n", season));
        if (req.getSoilType() != null) sb.append(String.format("- Soil Type: %s\n", req.getSoilType()));
        if (req.getIrrigationType() != null) sb.append(String.format("- Irrigation: %s\n", req.getIrrigationType()));
        if (req.getTotalArea() != null) sb.append(String.format("- Available Area: %.1f %s\n", req.getTotalArea(), req.getAreaUnit() != null ? req.getAreaUnit() : "acres"));
        if (req.getTemperature() != null) sb.append(String.format("- Current Temperature: %.1f°C\n", req.getTemperature()));
        if (req.getHumidity() != null) sb.append(String.format("- Humidity: %.0f%%\n", req.getHumidity()));
        if (req.getExistingCrops() != null && !req.getExistingCrops().isEmpty()) {
            sb.append(String.format("- Already Growing: %s\n", String.join(", ", req.getExistingCrops())));
            sb.append("- Recommend DIFFERENT crops that complement these (consider crop rotation).\n");
        }

        sb.append("\nRespond ONLY with a JSON array (no markdown). Each object must have:\n");
        sb.append("cropName, reason (2-3 sentences), bestSowingTime, expectedYield, waterRequirement (Low/Medium/High), marketDemand (Low/Medium/High), suitabilityScore (1-100)\n\n");
        sb.append("Example:\n[{\"cropName\":\"Wheat\",\"reason\":\"Ideal for Rabi season in Maharashtra with black soil.\",\"bestSowingTime\":\"October-November\",\"expectedYield\":\"20-25 quintals/acre\",\"waterRequirement\":\"Medium\",\"marketDemand\":\"High\",\"suitabilityScore\":92}]");

        return sb.toString();
    }

    private String callApi(String prompt) {
        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", "You are a crop recommendation expert for Indian agriculture. Always respond with valid JSON arrays."),
                        Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.3,
                "max_tokens", 2000
        );

        String raw = webClient.post()
                .uri(apiUrl)
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        Matcher m = Pattern.compile("\"content\"\\s*:\\s*\"(.*?)\"", Pattern.DOTALL).matcher(raw);
        if (m.find()) {
            return m.group(1)
                    .replace("\\n", "\n")
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\")
                    .replaceAll("^```json\\s*", "")
                    .replaceAll("^```\\s*", "")
                    .replaceAll("\\s*```$", "")
                    .trim();
        }
        throw new RuntimeException("Could not extract content from AI response");
    }

    private CropRecommendationResponse parseResponse(String json, String season) {
        try {
            List<RecommendedCrop> crops = objectMapper.readValue(json, new TypeReference<>() {});
            return CropRecommendationResponse.builder()
                    .recommendations(crops)
                    .season(season)
                    .source("AI")
                    .build();
        } catch (Exception e) {
            log.error("Failed to parse AI recommendation response: {}", e.getMessage());
            throw new RuntimeException("Parse failed", e);
        }
    }

    private CropRecommendationResponse getDefaultRecommendations(CropRecommendationRequest req, String season) {
        List<RecommendedCrop> defaults;

        switch (season) {
            case "Kharif" -> defaults = List.of(
                    RecommendedCrop.builder().cropName("Rice").reason("Excellent Kharif crop suitable for monsoon conditions with high humidity.").bestSowingTime("June-July").expectedYield("20-30 quintals/acre").waterRequirement("High").marketDemand("High").suitabilityScore(90).build(),
                    RecommendedCrop.builder().cropName("Cotton").reason("Major Kharif cash crop with good market value. Suitable for black soil.").bestSowingTime("May-June").expectedYield("8-12 quintals/acre").waterRequirement("Medium").marketDemand("High").suitabilityScore(85).build(),
                    RecommendedCrop.builder().cropName("Soybean").reason("Short duration crop with nitrogen-fixing properties. Improves soil health.").bestSowingTime("June-July").expectedYield("10-15 quintals/acre").waterRequirement("Medium").marketDemand("High").suitabilityScore(82).build(),
                    RecommendedCrop.builder().cropName("Groundnut").reason("Good oilseed crop for sandy-loam soil. Drought tolerant.").bestSowingTime("June-July").expectedYield("12-18 quintals/acre").waterRequirement("Low").marketDemand("Medium").suitabilityScore(78).build(),
                    RecommendedCrop.builder().cropName("Maize").reason("Fast-growing cereal crop with diverse uses — food, feed, and industrial.").bestSowingTime("June-July").expectedYield("25-35 quintals/acre").waterRequirement("Medium").marketDemand("Medium").suitabilityScore(75).build()
            );
            case "Rabi" -> defaults = List.of(
                    RecommendedCrop.builder().cropName("Wheat").reason("India's top Rabi crop. Thrives in cool weather with moderate irrigation.").bestSowingTime("October-November").expectedYield("20-25 quintals/acre").waterRequirement("Medium").marketDemand("High").suitabilityScore(92).build(),
                    RecommendedCrop.builder().cropName("Chickpea (Chana)").reason("Low-water pulse crop ideal for Rabi. Fixes nitrogen in soil.").bestSowingTime("October-November").expectedYield("8-12 quintals/acre").waterRequirement("Low").marketDemand("High").suitabilityScore(88).build(),
                    RecommendedCrop.builder().cropName("Mustard").reason("Important oilseed crop for Rabi season. Good for crop rotation.").bestSowingTime("October").expectedYield("6-10 quintals/acre").waterRequirement("Low").marketDemand("Medium").suitabilityScore(80).build(),
                    RecommendedCrop.builder().cropName("Onion").reason("High market demand vegetable crop. Excellent return on investment.").bestSowingTime("November-December").expectedYield("100-150 quintals/acre").waterRequirement("Medium").marketDemand("High").suitabilityScore(85).build(),
                    RecommendedCrop.builder().cropName("Potato").reason("High-yield tuber crop. Short duration with excellent market value.").bestSowingTime("October-November").expectedYield("80-120 quintals/acre").waterRequirement("Medium").marketDemand("High").suitabilityScore(83).build()
            );
            default -> defaults = List.of(
                    RecommendedCrop.builder().cropName("Watermelon").reason("Excellent summer crop with high returns. Low maintenance.").bestSowingTime("February-March").expectedYield("150-200 quintals/acre").waterRequirement("Medium").marketDemand("High").suitabilityScore(85).build(),
                    RecommendedCrop.builder().cropName("Cucumber").reason("Fast-growing summer vegetable. Short harvest cycle of 45-55 days.").bestSowingTime("February-March").expectedYield("60-80 quintals/acre").waterRequirement("Medium").marketDemand("Medium").suitabilityScore(80).build(),
                    RecommendedCrop.builder().cropName("Sunflower").reason("Oilseed crop suitable for summer. Good for intercropping.").bestSowingTime("January-February").expectedYield("6-10 quintals/acre").waterRequirement("Low").marketDemand("Medium").suitabilityScore(75).build(),
                    RecommendedCrop.builder().cropName("Muskmelon").reason("High-value summer fruit crop with good market demand.").bestSowingTime("February-March").expectedYield("80-100 quintals/acre").waterRequirement("Medium").marketDemand("High").suitabilityScore(78).build(),
                    RecommendedCrop.builder().cropName("Green Gram (Moong)").reason("Short-duration pulse crop. Fixes nitrogen and improves soil.").bestSowingTime("March-April").expectedYield("4-6 quintals/acre").waterRequirement("Low").marketDemand("High").suitabilityScore(82).build()
            );
        }

        return CropRecommendationResponse.builder()
                .recommendations(defaults)
                .season(season)
                .source("RULE_ENGINE")
                .build();
    }
}
