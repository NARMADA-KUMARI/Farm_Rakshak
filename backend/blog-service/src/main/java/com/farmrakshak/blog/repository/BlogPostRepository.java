package com.farmrakshak.blog.repository;

import com.farmrakshak.blog.entity.BlogPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface BlogPostRepository extends JpaRepository<BlogPost, UUID> {
    Page<BlogPost> findByStatusAndDeletedAtIsNull(String status, Pageable pageable);
    Optional<BlogPost> findBySlugAndDeletedAtIsNull(String slug);
    Page<BlogPost> findByDeletedAtIsNull(Pageable pageable);
    boolean existsBySlug(String slug);
}
