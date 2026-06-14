package com.farmrakshak.market.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "price_alerts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PriceAlert {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "crop_name", nullable = false, length = 100)
    private String cropName;

    @Column(name = "mandi_id")
    private UUID mandiId;

    @Column(name = "threshold_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal thresholdPrice;

    @Column(length = 10)
    @Builder.Default
    private String direction = "ABOVE";

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (updatedAt == null) updatedAt = Instant.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
