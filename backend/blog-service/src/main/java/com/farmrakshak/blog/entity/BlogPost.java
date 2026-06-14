package com.farmrakshak.blog.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "blog_posts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BlogPost {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(nullable = false, length = 200) private String title;
    @Column(nullable = false, unique = true, length = 250) private String slug;
    @Column(nullable = false, columnDefinition = "TEXT") private String content;
    @Column(length = 300) private String excerpt;
    @Column(name = "cover_image_url") private String coverImageUrl;
    @Column(name = "seo_title", length = 60) private String seoTitle;
    @Column(name = "seo_description", length = 160) private String seoDescription;
    @Column(columnDefinition = "TEXT") private String tags; // comma-separated
    @Column(name = "author_id", nullable = false) private UUID authorId;
    @Column(nullable = false, length = 20) @Builder.Default private String status = "DRAFT";
    @Column(name = "published_at") private Instant publishedAt;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Column(name = "deleted_at") private Instant deletedAt;
}
