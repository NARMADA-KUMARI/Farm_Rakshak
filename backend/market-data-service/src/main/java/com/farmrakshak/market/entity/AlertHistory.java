package com.farmrakshak.market.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "alert_history")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AlertHistory {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "alert_id", nullable = false)
    private UUID alertId;

    @Column(name = "triggered_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal triggeredPrice;

    @Column(name = "mandi_name", length = 150)
    private String mandiName;

    @Column(name = "triggered_at", nullable = false)
    @Builder.Default
    private Instant triggeredAt = Instant.now();
}
