package com.farmrakshak.market.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "crops_master")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CropMasterEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "crop_name", nullable = false, unique = true, length = 100)
    private String cropName;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(name = "scientific_name", length = 200)
    private String scientificName;

    @Column(length = 30)
    @Builder.Default
    private String unit = "kg";

    @Column(name = "local_names", columnDefinition = "text[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private String[] localNames;

    @Column(columnDefinition = "text[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private String[] synonyms;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
