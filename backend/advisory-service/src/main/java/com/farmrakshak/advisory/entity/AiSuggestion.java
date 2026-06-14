package com.farmrakshak.advisory.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "ai_suggestions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AiSuggestion {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "crop_name", length = 100)
    private String cropName;

    @Column(name = "crop_stage", length = 50)
    private String cropStage;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String suggestions;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "weather_snapshot", columnDefinition = "jsonb")
    private String weatherSnapshot;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "disease_context", columnDefinition = "jsonb")
    private String diseaseContext;

    @Column(nullable = false, length = 20) @Builder.Default
    private String source = "RULE";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at")
    private Instant expiresAt;
}
