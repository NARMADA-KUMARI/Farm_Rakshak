package com.farmrakshak.crop.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "crop_stage_master")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CropStageMaster {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "crop_id", nullable = false)
    private UUID cropId;

    @Column(name = "stage_name", nullable = false, length = 100)
    private String stageName;

    @Column(name = "stage_order", nullable = false)
    private Integer stageOrder;

    @Column(name = "start_day_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal startDayPercentage;

    @Column(name = "end_day_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal endDayPercentage;

    @Column(columnDefinition = "TEXT")
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
