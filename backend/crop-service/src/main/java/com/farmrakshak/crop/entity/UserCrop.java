package com.farmrakshak.crop.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity @Table(name = "user_crops")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserCrop {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "crop_id", nullable = false)
    private UUID cropId;

    @Column(name = "sowing_date", nullable = false)
    private LocalDate sowingDate;

    @Column(name = "expected_harvest_date", nullable = false)
    private LocalDate expectedHarvestDate;

    @Column(name = "current_stage_id")
    private UUID currentStageId;

    @Column(name = "land_area", precision = 10, scale = 2)
    private BigDecimal landArea;

    @Column(name = "land_area_unit", length = 20)
    @Builder.Default
    private String landAreaUnit = "acres";

    @Column(name = "soil_type", length = 50)
    private String soilType;

    @Column(name = "irrigation_type", length = 50)
    private String irrigationType;

    @Column(name = "growth_adjustment_factor", precision = 4, scale = 2)
    @Builder.Default
    private BigDecimal growthAdjustmentFactor = BigDecimal.ONE;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
