package com.farmrakshak.crop.service;

import com.farmrakshak.crop.entity.Farm;
import com.farmrakshak.crop.entity.FarmCrop;
import com.farmrakshak.crop.entity.FarmCropTask;
import com.farmrakshak.crop.repository.FarmCropRepository;
import com.farmrakshak.crop.repository.FarmCropTaskRepository;
import com.farmrakshak.crop.repository.FarmRepository;
import com.farmrakshak.shared.constants.KafkaTopics;
import com.farmrakshak.shared.event.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CropPlanService {

    private final FarmCropTaskRepository taskRepository;
    private final FarmCropRepository cropRepository;
    private final FarmRepository farmRepository;
    private final OwnershipValidator ownershipValidator;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final WebClient.Builder webClientBuilder;

    @Value("${services.advisory-url:http://localhost:8086}")
    private String advisoryServiceUrl;

    /**
     * Generate plan synchronously on demand. Tries advisory-service AI first,
     * falls back to built-in default plan if unavailable.
     */
    @Transactional
    public List<Map<String, Object>> generatePlanOnDemand(UUID cropId, UUID userId) {
        FarmCrop crop = ownershipValidator.validateCropOwnership(cropId, userId);
        Farm farm = farmRepository.findByIdAndDeletedFalse(crop.getFarmId())
                .orElseThrow(() -> new RuntimeException("Farm not found"));

        // Delete existing tasks
        taskRepository.deleteAllByFarmCropId(cropId);

        // Try AI plan from advisory-service, fallback to built-in
        List<TaskData> planTasks = fetchAiPlan(crop, farm);
        if (planTasks.isEmpty()) {
            int totalDays = estimateCropDuration(crop);
            log.info("Advisory service unavailable, using built-in plan for crop: {} ({} days)", crop.getCropName(), totalDays);
            planTasks = generateBuiltInPlan(crop.getCropName(), totalDays);
        }

        LocalDate baseDate = crop.getSowingDate() != null ? crop.getSowingDate() : LocalDate.now();

        for (TaskData t : planTasks) {
            taskRepository.save(FarmCropTask.builder()
                    .farmCropId(crop.getId())
                    .userId(farm.getUserId())
                    .stage(t.stage)
                    .title(t.title)
                    .description(t.description)
                    .dayNumber(t.dayNumber)
                    .dueDate(baseDate.plusDays(t.dayNumber))
                    .priority(t.priority)
                    .build());
        }

        log.info("Generated {} tasks for crop {} (id={})", planTasks.size(), crop.getCropName(), cropId);

        // Notify
        try {
            kafkaTemplate.send(KafkaTopics.NOTIFICATION_TOPIC, farm.getUserId().toString(),
                    NotificationEvent.builder()
                            .userId(farm.getUserId().toString())
                            .type("CROP")
                            .title("Lifecycle Plan Ready — " + crop.getCropName())
                            .body(String.format("%d tasks generated for \"%s\" in \"%s\", from sowing to harvest.",
                                    planTasks.size(), crop.getCropName(), farm.getFarmName()))
                            .build());
        } catch (Exception e) {
            log.warn("Failed to send plan notification: {}", e.getMessage());
        }

        return taskRepository.findByFarmCropIdOrderByDayNumberAsc(cropId)
                .stream().map(this::taskToMap).toList();
    }

    /**
     * Called asynchronously after a crop is added.
     */
    @Async
    public void generatePlanForCrop(FarmCrop crop, Farm farm) {
        try {
            // Small delay to let the transaction commit
            Thread.sleep(2000);

            List<TaskData> planTasks = fetchAiPlan(crop, farm);
            if (planTasks.isEmpty()) {
                int totalDays = estimateCropDuration(crop);
                planTasks = generateBuiltInPlan(crop.getCropName(), totalDays);
            }

            LocalDate baseDate = crop.getSowingDate() != null ? crop.getSowingDate() : LocalDate.now();

            for (TaskData t : planTasks) {
                taskRepository.save(FarmCropTask.builder()
                        .farmCropId(crop.getId())
                        .userId(farm.getUserId())
                        .stage(t.stage)
                        .title(t.title)
                        .description(t.description)
                        .dayNumber(t.dayNumber)
                        .dueDate(baseDate.plusDays(t.dayNumber))
                        .priority(t.priority)
                        .build());
            }

            log.info("Async: saved {} tasks for crop: {}", planTasks.size(), crop.getCropName());

            kafkaTemplate.send(KafkaTopics.NOTIFICATION_TOPIC, farm.getUserId().toString(),
                    NotificationEvent.builder()
                            .userId(farm.getUserId().toString())
                            .type("CROP")
                            .title("Lifecycle Plan Ready — " + crop.getCropName())
                            .body(String.format("%d tasks generated for \"%s\" in \"%s\".",
                                    planTasks.size(), crop.getCropName(), farm.getFarmName()))
                            .build());
        } catch (Exception e) {
            log.error("Async plan generation failed for crop {}: {}", crop.getId(), e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTasksForCrop(UUID cropId, UUID userId) {
        ownershipValidator.validateCropOwnership(cropId, userId);
        return taskRepository.findByFarmCropIdOrderByDayNumberAsc(cropId)
                .stream().map(this::taskToMap).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTodaysTasks(UUID userId) {
        return taskRepository.findPendingUpToDate(userId, LocalDate.now())
                .stream().map(this::taskToMap).toList();
    }

    private static final List<String> STAGES_ORDER = List.of(
            "PLANNED", "SOWN", "GERMINATION", "VEGETATIVE", "FLOWERING",
            "FRUITING", "HARVEST_READY", "HARVESTED"
    );
    private static final Set<String> TERMINAL_STAGES = Set.of("HARVESTED", "FAILED");

    @Transactional
    public Map<String, Object> completeTask(UUID taskId, UUID userId) {
        FarmCropTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        if (!task.getUserId().equals(userId)) {
            throw new RuntimeException("Access denied");
        }
        task.setStatus("COMPLETED");
        task.setCompletedAt(Instant.now());
        task = taskRepository.save(task);

        // Auto-advance crop stage if all tasks in the task's stage are completed
        if (task.getStage() != null) {
            autoAdvanceCropStage(task.getFarmCropId(), task.getStage(), userId);
        }

        Map<String, Object> result = taskToMap(task);
        // Include updated crop stage so frontend can refresh
        cropRepository.findByIdAndDeletedFalse(task.getFarmCropId())
                .ifPresent(crop -> {
                    result.put("cropStage", crop.getCropStage());
                    result.put("cropStatus", crop.getStatus());
                });
        return result;
    }

    private void autoAdvanceCropStage(UUID farmCropId, String completedTaskStage, UUID userId) {
        try {
            // Get all tasks for this crop in the completed task's stage
            List<FarmCropTask> stageTasks = taskRepository.findByFarmCropIdOrderByDayNumberAsc(farmCropId)
                    .stream()
                    .filter(t -> completedTaskStage.equals(t.getStage()))
                    .toList();

            // Check if ALL tasks in this stage are now completed
            boolean allDone = stageTasks.stream().allMatch(t -> "COMPLETED".equals(t.getStatus()));
            if (!allDone) return;

            // Get the crop
            FarmCrop crop = cropRepository.findByIdAndDeletedFalse(farmCropId).orElse(null);
            if (crop == null) return;

            // Only advance if the crop is currently at the completed stage (or earlier)
            int currentIdx = STAGES_ORDER.indexOf(crop.getCropStage());
            int completedIdx = STAGES_ORDER.indexOf(completedTaskStage);
            if (completedIdx < 0 || currentIdx > completedIdx) return;

            // Find the next stage
            int nextIdx = completedIdx + 1;
            if (nextIdx >= STAGES_ORDER.size()) return;

            String nextStage = STAGES_ORDER.get(nextIdx);
            String oldStage = crop.getCropStage();
            crop.setCropStage(nextStage);

            // Terminal stages free the area
            if (TERMINAL_STAGES.contains(nextStage)) {
                crop.setStatus("COMPLETED");
            }

            crop = cropRepository.save(crop);
            log.info("Auto-advanced crop {} from {} to {} (all {} tasks completed)", farmCropId, oldStage, nextStage, completedTaskStage);

            // Send notification
            Farm farm = farmRepository.findByIdAndDeletedFalse(crop.getFarmId()).orElse(null);
            String farmName = farm != null ? farm.getFarmName() : "farm";

            String areaInfo = "";
            if (TERMINAL_STAGES.contains(nextStage) && crop.getAreaAllocated() != null && farm != null && farm.getTotalArea() != null) {
                var remaining = farm.getTotalArea().subtract(
                        taskRepository.findByFarmCropIdOrderByDayNumberAsc(farmCropId).isEmpty() ?
                                java.math.BigDecimal.ZERO : cropRepository.sumAllocatedAreaByFarmId(crop.getFarmId()));
                areaInfo = String.format(" %s %s freed.", crop.getAreaAllocated(), farm.getAreaUnit());
            }

            kafkaTemplate.send(KafkaTopics.NOTIFICATION_TOPIC, userId.toString(),
                    NotificationEvent.builder()
                            .userId(userId.toString())
                            .type("CROP")
                            .title(crop.getCropName() + " — Stage Advanced!")
                            .body(String.format("All %s tasks completed! \"%s\" in \"%s\" moved from %s to %s.%s",
                                    completedTaskStage, crop.getCropName(), farmName, oldStage, nextStage, areaInfo))
                            .build());
        } catch (Exception e) {
            log.warn("Auto-advance failed for crop {}: {}", farmCropId, e.getMessage());
        }
    }

    @Transactional
    public void deleteTasksForCrop(UUID farmCropId) {
        taskRepository.deleteAllByFarmCropId(farmCropId);
    }

    // ──────────────────────────────────────────────
    // AI Plan Fetch
    // ──────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private List<TaskData> fetchAiPlan(FarmCrop crop, Farm farm) {
        try {
            Map<String, Object> request = new LinkedHashMap<>();
            request.put("cropName", crop.getCropName());
            request.put("variety", crop.getVariety());
            request.put("sowingDate", crop.getSowingDate() != null ? crop.getSowingDate().toString() : null);
            request.put("expectedHarvest", crop.getExpectedHarvest() != null ? crop.getExpectedHarvest().toString() : null);
            request.put("soilType", farm.getSoilType());
            request.put("irrigationType", crop.getIrrigationType());
            request.put("areaAllocated", crop.getAreaAllocated());
            request.put("areaUnit", farm.getAreaUnit());
            request.put("farmName", farm.getFarmName());
            request.put("village", farm.getVillage());
            request.put("district", farm.getDistrict());
            request.put("state", farm.getState());

            Map<?, ?> response = webClientBuilder.build().post()
                    .uri(advisoryServiceUrl + "/api/v1/advisories/ai/crop-plan")
                    .header("Content-Type", "application/json")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null || response.get("data") == null) return Collections.emptyList();

            Map<String, Object> planData = (Map<String, Object>) response.get("data");
            List<Map<String, Object>> tasks = (List<Map<String, Object>>) planData.get("tasks");
            if (tasks == null) return Collections.emptyList();

            List<TaskData> result = new ArrayList<>();
            for (Map<String, Object> t : tasks) {
                result.add(new TaskData(
                        t.get("dayNumber") instanceof Number ? ((Number) t.get("dayNumber")).intValue() : 0,
                        t.get("stage") != null ? t.get("stage").toString() : null,
                        t.get("title") != null ? t.get("title").toString() : "Task",
                        t.get("description") != null ? t.get("description").toString() : null,
                        t.get("priority") != null ? t.get("priority").toString() : "MEDIUM"
                ));
            }
            return result;
        } catch (Exception e) {
            log.warn("Failed to fetch AI plan from advisory-service: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // ──────────────────────────────────────────────
    // Built-in Fallback Plan
    // ──────────────────────────────────────────────

    /**
     * Estimate crop duration from sowing/harvest dates.
     * Falls back to common crop durations if dates aren't set.
     */
    private int estimateCropDuration(FarmCrop crop) {
        // First try: calculate from sowing and expected harvest dates
        if (crop.getSowingDate() != null && crop.getExpectedHarvest() != null) {
            long days = java.time.temporal.ChronoUnit.DAYS.between(crop.getSowingDate(), crop.getExpectedHarvest());
            if (days > 10 && days < 730) return (int) days;
        }

        // Second try: lookup common crop durations
        Map<String, Integer> CROP_DURATIONS = Map.ofEntries(
                Map.entry("rice", 120), Map.entry("wheat", 150), Map.entry("maize", 100),
                Map.entry("bajra", 90), Map.entry("jowar", 110), Map.entry("ragi", 120),
                Map.entry("barley", 130), Map.entry("oats", 120),
                Map.entry("chickpea", 120), Map.entry("chana", 120), Map.entry("pigeon pea", 180),
                Map.entry("tur", 180), Map.entry("arhar", 180),
                Map.entry("moong", 70), Map.entry("urad", 80), Map.entry("masoor", 120),
                Map.entry("soybean", 100), Map.entry("groundnut", 120), Map.entry("mustard", 130),
                Map.entry("sunflower", 100), Map.entry("sesame", 90), Map.entry("castor", 150),
                Map.entry("tomato", 90), Map.entry("potato", 100), Map.entry("onion", 120),
                Map.entry("brinjal", 80), Map.entry("okra", 60), Map.entry("bhindi", 60),
                Map.entry("cauliflower", 90), Map.entry("cabbage", 90),
                Map.entry("capsicum", 75), Map.entry("bell pepper", 75),
                Map.entry("chili", 90), Map.entry("bottle gourd", 70), Map.entry("bitter gourd", 65),
                Map.entry("cucumber", 55), Map.entry("pumpkin", 100),
                Map.entry("radish", 40), Map.entry("carrot", 75), Map.entry("spinach", 40),
                Map.entry("peas", 90), Map.entry("beans", 60),
                Map.entry("banana", 365), Map.entry("papaya", 270), Map.entry("watermelon", 80),
                Map.entry("turmeric", 240), Map.entry("ginger", 240),
                Map.entry("cotton", 180), Map.entry("jute", 120),
                Map.entry("sugarcane", 365), Map.entry("tobacco", 120),
                Map.entry("marigold", 60), Map.entry("rose", 90),
                Map.entry("strawberry", 90), Map.entry("pomegranate", 180), Map.entry("grapes", 150)
        );

        String name = crop.getCropName() != null ? crop.getCropName().toLowerCase().trim() : "";
        for (Map.Entry<String, Integer> e : CROP_DURATIONS.entrySet()) {
            if (name.contains(e.getKey())) return e.getValue();
        }

        return 90; // Default fallback
    }

    /**
     * Generate a built-in plan scaled to the crop's actual duration.
     * Task day numbers are proportionally scaled from a base 85-day template.
     */
    private List<TaskData> generateBuiltInPlan(String cropName, int totalDays) {
        String c = cropName != null ? cropName : "Crop";
        double scale = totalDays / 85.0; // Base template is 85 days
        List<TaskData> tasks = new ArrayList<>();

        tasks.add(new TaskData(d(0, scale),  "PLANNED",       "HIGH",   "Prepare soil for " + c, "Plough and level the field. Add organic manure or compost. Ensure proper drainage."));
        tasks.add(new TaskData(d(1, scale),  "SOWN",          "HIGH",   "Sow " + c + " seeds", "Sow seeds at recommended depth and spacing. Apply light irrigation after sowing."));
        tasks.add(new TaskData(d(3, scale),  "SOWN",          "MEDIUM", "Check soil moisture", "Ensure soil is moist but not waterlogged. Apply light irrigation if dry."));
        tasks.add(new TaskData(d(5, scale),  "SOWN",          "MEDIUM", "Monitor germination", "Check for seed emergence. Resow gaps if germination is poor."));
        tasks.add(new TaskData(d(8, scale),  "GERMINATION",   "HIGH",   "First irrigation after germination", "Apply irrigation once seedlings emerge. Maintain consistent moisture."));
        tasks.add(new TaskData(d(10, scale), "GERMINATION",   "MEDIUM", "Thin seedlings", "Remove weak seedlings to maintain recommended plant spacing."));
        tasks.add(new TaskData(d(12, scale), "GERMINATION",   "HIGH",   "Apply basal fertilizer", "Apply NPK fertilizer at recommended dose for " + c + "."));
        tasks.add(new TaskData(d(15, scale), "GERMINATION",   "MEDIUM", "First weeding", "Remove weeds manually or apply pre-emergence herbicide."));
        tasks.add(new TaskData(d(20, scale), "VEGETATIVE",    "HIGH",   "Nitrogen top dressing", "Apply urea or ammonium sulphate to boost vegetative growth."));
        tasks.add(new TaskData(d(25, scale), "VEGETATIVE",    "MEDIUM", "Pest inspection", "Check for aphids, borers, and other common pests. Spray neem oil if needed."));
        tasks.add(new TaskData(d(28, scale), "VEGETATIVE",    "MEDIUM", "Second weeding", "Remove weeds and loosen soil around plants with light hoeing."));
        tasks.add(new TaskData(d(30, scale), "VEGETATIVE",    "LOW",    "Check for nutrient deficiency", "Look for yellowing leaves (nitrogen), purple stems (phosphorus), or brown edges (potassium)."));
        tasks.add(new TaskData(d(35, scale), "VEGETATIVE",    "MEDIUM", "Irrigation schedule check", "Ensure regular irrigation every 5-7 days depending on weather."));
        tasks.add(new TaskData(d(40, scale), "FLOWERING",     "HIGH",   "Flowering stage nutrition", "Apply phosphorus-rich fertilizer (DAP or SSP) to support flowering."));
        tasks.add(new TaskData(d(43, scale), "FLOWERING",     "MEDIUM", "Monitor pollination", "Check for proper flower development. Ensure pollinator-friendly conditions."));
        tasks.add(new TaskData(d(45, scale), "FLOWERING",     "HIGH",   "Disease check during flowering", "Inspect for fungal diseases (powdery mildew, blight). Apply fungicide if spotted."));
        tasks.add(new TaskData(d(50, scale), "FLOWERING",     "MEDIUM", "Reduce excess nitrogen", "Stop nitrogen application to prevent excessive leaf growth over fruiting."));
        tasks.add(new TaskData(d(55, scale), "FRUITING",      "MEDIUM", "Support fruit-bearing branches", "Install stakes or supports if plants are heavy with fruit."));
        tasks.add(new TaskData(d(58, scale), "FRUITING",      "HIGH",   "Potassium application", "Apply potassium-rich fertilizer (MOP) for fruit quality, size, and sweetness."));
        tasks.add(new TaskData(d(60, scale), "FRUITING",      "HIGH",   "Pest control for fruiting", "Monitor for fruit borers and sucking pests. Apply appropriate pesticide."));
        tasks.add(new TaskData(d(65, scale), "FRUITING",      "MEDIUM", "Maintain irrigation", "Keep consistent watering. Avoid water stress which causes fruit drop."));
        tasks.add(new TaskData(d(70, scale), "FRUITING",      "LOW",    "Monitor fruit development", "Check fruit size and color development. Note any abnormalities."));
        tasks.add(new TaskData(d(75, scale), "HARVEST_READY", "HIGH",   "Pre-harvest assessment", "Check maturity indicators — color, firmness, size. Plan harvest date."));
        tasks.add(new TaskData(d(78, scale), "HARVEST_READY", "MEDIUM", "Stop irrigation", "Reduce or stop irrigation 7-10 days before harvest to improve quality."));
        tasks.add(new TaskData(d(80, scale), "HARVEST_READY", "MEDIUM", "Stop all chemical sprays", "Maintain pre-harvest interval. No pesticides/fertilizers before harvest."));
        tasks.add(new TaskData(d(83, scale), "HARVEST_READY", "MEDIUM", "Arrange harvest resources", "Arrange labor, tools, transport, and storage for harvest day."));
        tasks.add(new TaskData(Math.max(d(85, scale), totalDays - 2), "HARVESTED", "HIGH", "Harvest " + c, "Harvest at optimal maturity. Use clean tools. Handle produce carefully."));
        tasks.add(new TaskData(Math.max(d(87, scale), totalDays),     "HARVESTED", "MEDIUM", "Post-harvest handling", "Sort, grade, and store produce properly. Clean field of crop residue."));

        return tasks;
    }

    /** Scale a base day number (from 85-day template) to the actual crop duration */
    private int d(int baseDay, double scale) {
        return Math.max(0, (int) Math.round(baseDay * scale));
    }

    // ──────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────

    private record TaskData(int dayNumber, String stage, String priority, String title, String description) {}

    private Map<String, Object> taskToMap(FarmCropTask t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId().toString());
        m.put("farmCropId", t.getFarmCropId().toString());
        m.put("stage", t.getStage());
        m.put("title", t.getTitle());
        m.put("description", t.getDescription());
        m.put("dueDate", t.getDueDate() != null ? t.getDueDate().toString() : null);
        m.put("dayNumber", t.getDayNumber());
        m.put("priority", t.getPriority());
        m.put("status", t.getStatus());
        m.put("isOverdue", t.getDueDate() != null && t.getDueDate().isBefore(LocalDate.now()) && "PENDING".equals(t.getStatus()));
        m.put("isDueToday", t.getDueDate() != null && t.getDueDate().equals(LocalDate.now()));
        return m;
    }
}
