package com.farmrakshak.crop.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Tracks disease outbreaks reported by farmers.
 * Used for deduplication, admin analytics, and geographic heatmaps.
 */
@Entity
@Table(name = "disease_alerts", indexes = {
        @Index(name = "idx_disease_alerts_crop", columnList = "crop_name"),
        @Index(name = "idx_disease_alerts_date", columnList = "created_at"),
        @Index(name = "idx_disease_alerts_location", columnList = "district, state")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiseaseAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "reporter_user_id", nullable = false)
    private UUID reporterUserId;

    @Column(name = "crop_name", nullable = false, length = 100)
    private String cropName;

    @Column(name = "disease_name", nullable = false, length = 200)
    private String diseaseName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String treatment;

    @Column(columnDefinition = "TEXT")
    private String prevention;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(length = 100)
    private String village;

    @Column(length = 100)
    private String district;

    @Column(length = 50)
    private String state;

    @Column(name = "radius_km", nullable = false)
    @Builder.Default
    private Integer radiusKm = 100;

    @Column(name = "farmers_notified", nullable = false)
    @Builder.Default
    private Integer farmersNotified = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
