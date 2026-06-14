package com.farmrakshak.crop.scheduler;

import com.farmrakshak.crop.entity.FarmCrop;
import com.farmrakshak.crop.entity.FarmCropTask;
import com.farmrakshak.crop.repository.FarmCropRepository;
import com.farmrakshak.crop.repository.FarmCropTaskRepository;
import com.farmrakshak.shared.constants.KafkaTopics;
import com.farmrakshak.shared.event.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailyTaskNotifier {

    private final FarmCropTaskRepository taskRepository;
    private final FarmCropRepository cropRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Runs every day at 6:00 AM IST. Finds all pending tasks due today
     * and sends a consolidated notification to each user.
     */
    @Scheduled(cron = "0 0 6 * * *", zone = "Asia/Kolkata")
    public void notifyDailyTasks() {
        log.info("Running daily task notification job...");

        LocalDate today = LocalDate.now();
        List<FarmCropTask> todaysTasks = taskRepository.findAllPendingByDueDate(today);

        if (todaysTasks.isEmpty()) {
            log.info("No tasks due today");
            return;
        }

        // Group by userId
        Map<UUID, List<FarmCropTask>> byUser = todaysTasks.stream()
                .collect(Collectors.groupingBy(FarmCropTask::getUserId));

        for (Map.Entry<UUID, List<FarmCropTask>> entry : byUser.entrySet()) {
            UUID userId = entry.getKey();
            List<FarmCropTask> tasks = entry.getValue();

            // Build notification body with crop names
            Set<String> cropNames = new LinkedHashSet<>();
            for (FarmCropTask task : tasks) {
                cropRepository.findByIdAndDeletedFalse(task.getFarmCropId())
                        .ifPresent(crop -> cropNames.add(crop.getCropName()));
            }

            StringBuilder body = new StringBuilder();
            body.append(String.format("You have %d task(s) for today:\n", tasks.size()));
            for (int i = 0; i < Math.min(tasks.size(), 5); i++) {
                FarmCropTask t = tasks.get(i);
                body.append(String.format("• %s", t.getTitle()));
                if (t.getPriority() != null && "HIGH".equals(t.getPriority())) {
                    body.append(" [HIGH PRIORITY]");
                }
                body.append("\n");
            }
            if (tasks.size() > 5) {
                body.append(String.format("...and %d more tasks.", tasks.size() - 5));
            }

            try {
                NotificationEvent notification = NotificationEvent.builder()
                        .userId(userId.toString())
                        .type("TASK")
                        .title(String.format("Today's Farm Tasks (%d)", tasks.size()))
                        .body(body.toString())
                        .build();
                kafkaTemplate.send(KafkaTopics.NOTIFICATION_TOPIC, userId.toString(), notification);
                log.info("Sent daily task notification to user {} with {} tasks", userId, tasks.size());
            } catch (Exception e) {
                log.error("Failed to send daily notification to user {}: {}", userId, e.getMessage());
            }
        }

        log.info("Daily task notification job completed. Notified {} users.", byUser.size());
    }
}
