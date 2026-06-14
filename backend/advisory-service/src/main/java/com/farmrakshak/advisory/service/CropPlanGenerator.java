package com.farmrakshak.advisory.service;

import com.farmrakshak.advisory.dto.CropPlanRequest;
import com.farmrakshak.advisory.dto.CropPlanResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class CropPlanGenerator {

    @Value("${mistral.api-key:}")
    private String apiKey;

    @Value("${mistral.model:mistral-small-latest}")
    private String model;

    @Value("${mistral.url:https://api.mistral.ai/v1/chat/completions}")
    private String apiUrl;

    private final WebClient webClient;

    public CropPlanGenerator(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public CropPlanResponse generatePlan(CropPlanRequest request) {
        if (apiKey == null || apiKey.isBlank()) {
            log.info("Mistral API key not configured — generating default plan");
            return generateDefaultPlan(request);
        }

        try {
            String prompt = buildPrompt(request);
            String response = callApi(prompt);
            CropPlanResponse plan = parseResponse(response, request);
            if (plan.getTasks() == null || plan.getTasks().isEmpty()) {
                log.warn("AI returned empty plan, falling back to default");
                return generateDefaultPlan(request);
            }
            return plan;
        } catch (Exception e) {
            log.error("AI crop plan generation failed: {}", e.getMessage());
            return generateDefaultPlan(request);
        }
    }

    private String buildPrompt(CropPlanRequest req) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are an expert Indian agriculture planner. Generate a COMPLETE day-by-day crop lifecycle plan.\n\n");
        sb.append("CROP DETAILS:\n");
        sb.append(String.format("- Crop: %s", req.getCropName()));
        if (req.getVariety() != null) sb.append(String.format(" (Variety: %s)", req.getVariety()));
        sb.append("\n");
        if (req.getSowingDate() != null) sb.append(String.format("- Sowing Date: %s\n", req.getSowingDate()));
        if (req.getSoilType() != null) sb.append(String.format("- Soil: %s\n", req.getSoilType()));
        if (req.getIrrigationType() != null) sb.append(String.format("- Irrigation: %s\n", req.getIrrigationType()));
        if (req.getAreaAllocated() > 0) sb.append(String.format("- Area: %.1f %s\n", req.getAreaAllocated(), req.getAreaUnit() != null ? req.getAreaUnit() : "acres"));

        sb.append("\nFARM LOCATION:\n");
        if (req.getVillage() != null) sb.append(String.format("- %s", req.getVillage()));
        if (req.getDistrict() != null) sb.append(String.format(", %s", req.getDistrict()));
        if (req.getState() != null) sb.append(String.format(", %s", req.getState()));
        sb.append("\n");

        sb.append("\nGenerate tasks from Day 1 (sowing) through the full lifecycle to harvest.\n");
        sb.append("Include tasks for: soil prep, sowing, watering schedule, fertilizer applications, ");
        sb.append("weeding, pest monitoring, disease checks, growth monitoring, pruning/training, ");
        sb.append("harvest preparation, and harvest.\n\n");

        sb.append("OUTPUT FORMAT — one task per line, EXACTLY like this:\n");
        sb.append("DAY <number> | STAGE <stage_name> | PRIORITY <HIGH/MEDIUM/LOW> | TITLE <short title> | DESC <description>\n\n");
        sb.append("STAGE must be one of: PLANNED, SOWN, GERMINATION, VEGETATIVE, FLOWERING, FRUITING, HARVEST_READY, HARVESTED\n\n");
        sb.append("Generate 25-40 tasks covering the ENTIRE lifecycle. Be specific to Indian farming conditions and this crop.\n");
        sb.append("Space tasks across the full growth period — don't cluster everything in the first week.\n");

        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private String callApi(String prompt) {
        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "temperature", 0.3,
                "max_tokens", 4000
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

    private CropPlanResponse parseResponse(String response, CropPlanRequest request) {
        List<CropPlanResponse.PlanTask> tasks = new ArrayList<>();
        if (response == null || response.isBlank()) return CropPlanResponse.builder().cropName(request.getCropName()).tasks(tasks).build();

        Pattern pattern = Pattern.compile(
                "DAY\\s+(\\d+)\\s*\\|\\s*STAGE\\s+(\\w+)\\s*\\|\\s*PRIORITY\\s+(HIGH|MEDIUM|LOW)\\s*\\|\\s*TITLE\\s+(.+?)\\s*\\|\\s*DESC\\s+(.+)",
                Pattern.CASE_INSENSITIVE
        );

        int maxDay = 0;
        for (String line : response.split("\n")) {
            Matcher m = pattern.matcher(line.trim());
            if (m.find()) {
                int day = Integer.parseInt(m.group(1));
                if (day > maxDay) maxDay = day;
                tasks.add(CropPlanResponse.PlanTask.builder()
                        .dayNumber(day)
                        .stage(m.group(2).toUpperCase())
                        .priority(m.group(3).toUpperCase())
                        .title(m.group(4).trim())
                        .description(m.group(5).trim())
                        .build());
            }
        }

        tasks.sort(Comparator.comparingInt(CropPlanResponse.PlanTask::getDayNumber));

        return CropPlanResponse.builder()
                .cropName(request.getCropName())
                .totalDays(maxDay)
                .tasks(tasks)
                .build();
    }

    /**
     * Fallback: generate a sensible default plan when AI is unavailable.
     */
    private CropPlanResponse generateDefaultPlan(CropPlanRequest req) {
        String crop = req.getCropName() != null ? req.getCropName() : "Crop";
        List<CropPlanResponse.PlanTask> tasks = new ArrayList<>();

        tasks.add(task(1, "SOWN", "HIGH", "Sow " + crop + " seeds", "Prepare soil and sow seeds at recommended depth and spacing for " + crop + "."));
        tasks.add(task(2, "SOWN", "HIGH", "First irrigation after sowing", "Apply light irrigation immediately after sowing to settle the soil."));
        tasks.add(task(5, "SOWN", "MEDIUM", "Check seed germination", "Inspect the field for germination. Re-sow gaps if needed."));
        tasks.add(task(8, "GERMINATION", "MEDIUM", "Thin seedlings", "Remove weak seedlings to maintain recommended spacing."));
        tasks.add(task(10, "GERMINATION", "HIGH", "Apply basal fertilizer", "Apply NPK fertilizer at recommended dose for " + crop + "."));
        tasks.add(task(14, "GERMINATION", "MEDIUM", "First weeding", "Remove weeds manually or with appropriate herbicide."));
        tasks.add(task(20, "VEGETATIVE", "HIGH", "Top dressing fertilizer", "Apply nitrogen-rich top dressing to boost vegetative growth."));
        tasks.add(task(25, "VEGETATIVE", "MEDIUM", "Pest inspection", "Check for common pests of " + crop + ". Apply organic pesticide if needed."));
        tasks.add(task(30, "VEGETATIVE", "MEDIUM", "Second weeding", "Remove weeds and loosen soil around plants."));
        tasks.add(task(35, "VEGETATIVE", "LOW", "Monitor plant growth", "Check for nutrient deficiencies. Yellowing leaves indicate nitrogen shortage."));
        tasks.add(task(40, "FLOWERING", "HIGH", "Flowering stage nutrition", "Switch to phosphorus-rich fertilizer to support flowering."));
        tasks.add(task(45, "FLOWERING", "MEDIUM", "Pollination check", "Verify proper pollination. Hand-pollinate if insect activity is low."));
        tasks.add(task(50, "FLOWERING", "HIGH", "Disease monitoring", "Check for fungal diseases common during flowering. Apply fungicide if needed."));
        tasks.add(task(55, "FRUITING", "MEDIUM", "Support heavy branches", "Install stakes or supports for fruit-bearing branches."));
        tasks.add(task(60, "FRUITING", "HIGH", "Fruit development nutrition", "Apply potassium-rich fertilizer for fruit quality and size."));
        tasks.add(task(65, "FRUITING", "MEDIUM", "Pest control", "Monitor and control fruit borers and sucking pests."));
        tasks.add(task(70, "FRUITING", "LOW", "Irrigation management", "Maintain consistent moisture. Avoid waterlogging."));
        tasks.add(task(80, "HARVEST_READY", "HIGH", "Pre-harvest assessment", "Check crop maturity indicators. Plan harvest timing and labor."));
        tasks.add(task(85, "HARVEST_READY", "MEDIUM", "Stop fertilizer application", "Discontinue fertilizer 10-15 days before harvest."));
        tasks.add(task(90, "HARVESTED", "HIGH", "Harvest crop", "Harvest at optimal maturity. Use proper tools to minimize damage."));

        return CropPlanResponse.builder()
                .cropName(crop)
                .totalDays(90)
                .tasks(tasks)
                .build();
    }

    private CropPlanResponse.PlanTask task(int day, String stage, String priority, String title, String desc) {
        return CropPlanResponse.PlanTask.builder()
                .dayNumber(day).stage(stage).priority(priority).title(title).description(desc).build();
    }
}
