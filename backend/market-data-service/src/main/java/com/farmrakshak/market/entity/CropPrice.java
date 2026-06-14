package com.farmrakshak.market.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "crop_prices", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"crop_name", "mandi_id", "price_date"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CropPrice {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "crop_name", nullable = false, length = 100)
    private String cropName;

    @Column(name = "mandi_id", nullable = false)
    private UUID mandiId;

    @Column(name = "price_min", precision = 12, scale = 2)
    private BigDecimal priceMin;

    @Column(name = "price_max", precision = 12, scale = 2)
    private BigDecimal priceMax;

    @Column(name = "price_modal", nullable = false, precision = 12, scale = 2)
    private BigDecimal priceModal;

    @Column(name = "arrival_quantity", precision = 12, scale = 2)
    private BigDecimal arrivalQuantity;

    @Column(name = "price_date", nullable = false)
    private LocalDate priceDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
