package com.farmrakshak.crop.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "farm_crop_tasks", indexes = {
        @Index(name = "idx_fct_farm_crop_id", columnList = "farm_crop_id"),
        @Index(name = "idx_fct_due_date", columnList = "due_date"),
        @Index(name = "idx_fct_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FarmCropTask {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "farm_crop_id", nullable = false)
    private UUID farmCropId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "stage", length = 30)
    private String stage;

    @Column(name = "title", nullable = false, length = 300)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "day_number")
    private int dayNumber;

    @Column(name = "priority", length = 20)
    @Builder.Default
    private String priority = "MEDIUM";

    @Column(name = "status", length = 20)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "completed_at")
    private Instant completedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
