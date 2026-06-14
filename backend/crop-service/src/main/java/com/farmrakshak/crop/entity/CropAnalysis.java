package com.farmrakshak.crop.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "crop_analyses")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CropAnalysis {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "upload_id", nullable = false, unique = true)
    private UUID uploadId;

    @Column(name = "crop_name", length = 100)
    private String cropName;

    @Column(name = "farm_crop_id")
    private UUID farmCropId;

    @Column(name = "disease_name", length = 200)
    private String diseaseName;

    @Column(precision = 5, scale = 4)
    private BigDecimal confidence;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String treatment; // JSON array stored as text

    @Column(columnDefinition = "TEXT")
    private String prevention; // JSON array stored as text

    @Column(name = "analyzed_at")
    private Instant analyzedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
