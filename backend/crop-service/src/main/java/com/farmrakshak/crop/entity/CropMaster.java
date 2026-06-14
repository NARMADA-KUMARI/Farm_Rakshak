package com.farmrakshak.crop.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "crop_master")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CropMaster {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "crop_name", nullable = false, unique = true, length = 100)
    private String cropName;

    @Column(name = "crop_category", nullable = false, length = 50)
    private String cropCategory;

    @Column(name = "avg_growth_days", nullable = false)
    private Integer avgGrowthDays;

    @Column(name = "min_growth_days", nullable = false)
    private Integer minGrowthDays;

    @Column(name = "max_growth_days", nullable = false)
    private Integer maxGrowthDays;

    @Column(length = 100)
    private String region;

    @Column(name = "seed_variety", length = 100)
    private String seedVariety;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
