package com.farmrakshak.advisory.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "advisories")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Advisory {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(length = 100)
    private String crop;

    @Column(length = 50)
    private String season;

    @Column(length = 100)
    private String region;

    @Column(nullable = false, length = 5) @Builder.Default
    private String language = "en";

    @Column(nullable = false, length = 50)
    private String type;

    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
