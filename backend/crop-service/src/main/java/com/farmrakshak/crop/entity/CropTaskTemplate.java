package com.farmrakshak.crop.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "crop_task_templates")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CropTaskTemplate {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "crop_id", nullable = false)
    private UUID cropId;

    @Column(name = "stage_id", nullable = false)
    private UUID stageId;

    @Column(name = "task_title", nullable = false, length = 200)
    private String taskTitle;

    @Column(name = "task_description", columnDefinition = "TEXT")
    private String taskDescription;

    @Column(name = "days_after_stage_start", nullable = false)
    @Builder.Default
    private Integer daysAfterStageStart = 0;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String priority = "MEDIUM";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
