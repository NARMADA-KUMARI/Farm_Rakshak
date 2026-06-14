package com.farmrakshak.notification.controller;

import com.farmrakshak.notification.entity.Notification;
import com.farmrakshak.notification.repository.NotificationRepository;
import com.farmrakshak.shared.dto.ApiResponse;
import com.farmrakshak.shared.dto.PagedResponse;
import com.farmrakshak.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController @RequestMapping("/api/v1/notifications") @RequiredArgsConstructor
public class NotificationController {
    private final NotificationRepository repository;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<Notification>>> list(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        Page<Notification> result = repository.findByUserIdOrderByCreatedAtDesc(UUID.fromString(userId), PageRequest.of(page, Math.min(size, 50)));
        PagedResponse<Notification> paged = PagedResponse.<Notification>builder()
                .content(result.getContent()).page(result.getNumber()).size(result.getSize())
                .totalElements(result.getTotalElements()).totalPages(result.getTotalPages()).build();
        return ResponseEntity.ok(ApiResponse.success(paged));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Notification>> create(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody Map<String, String> body) {
        String type = body.getOrDefault("type", "SYSTEM");
        String title = body.getOrDefault("title", "Notification");
        String msg = body.get("body");

        UUID uid = UUID.fromString(userId);

        // Dedup: skip if same title was created for this user in last 6 hours
        Instant sixHoursAgo = Instant.now().minus(6, ChronoUnit.HOURS);
        List<Notification> recent = repository.findByUserIdAndTitleAndCreatedAtAfter(uid, title, sixHoursAgo);
        if (!recent.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success(recent.get(0), "Already exists"));
        }

        Notification n = Notification.builder()
                .userId(uid).type(type).title(title).body(msg).build();
        repository.save(n);
        return ResponseEntity.ok(ApiResponse.success(n, "Created"));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markRead(@PathVariable UUID id) {
        Notification n = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", id.toString()));
        n.setIsRead(true);
        repository.save(n);
        return ResponseEntity.ok(ApiResponse.success(null, "Marked as read"));
    }

    @PutMapping("/read-all") @Transactional
    public ResponseEntity<ApiResponse<Void>> markAllRead(@RequestHeader("X-User-Id") String userId) {
        repository.markAllAsRead(UUID.fromString(userId));
        return ResponseEntity.ok(ApiResponse.success(null, "All marked as read"));
    }
}
