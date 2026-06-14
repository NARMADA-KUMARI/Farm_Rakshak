package com.farmrakshak.blog.controller;

import com.farmrakshak.blog.entity.BlogPost;
import com.farmrakshak.blog.repository.BlogPostRepository;
import com.farmrakshak.shared.dto.ApiResponse;
import com.farmrakshak.shared.dto.PagedResponse;
import com.farmrakshak.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.Normalizer;
import java.time.Instant;
import java.util.UUID;

@RestController @RequestMapping("/api/v1/blogs") @RequiredArgsConstructor
public class BlogController {
    private final BlogPostRepository repository;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<BlogPost>>> list(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        Page<BlogPost> result = repository.findByStatusAndDeletedAtIsNull("PUBLISHED",
                PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "publishedAt")));
        PagedResponse<BlogPost> paged = PagedResponse.<BlogPost>builder()
                .content(result.getContent()).page(result.getNumber()).size(result.getSize())
                .totalElements(result.getTotalElements()).totalPages(result.getTotalPages()).build();
        return ResponseEntity.ok(ApiResponse.success(paged));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<ApiResponse<BlogPost>> getBySlug(@PathVariable String slug) {
        BlogPost post = repository.findBySlugAndDeletedAtIsNull(slug)
                .orElseThrow(() -> new ResourceNotFoundException("BlogPost", "slug", slug));
        return ResponseEntity.ok(ApiResponse.success(post));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BlogPost>> create(@RequestBody BlogPost blog,
            @RequestHeader("X-User-Id") String userId) {
        blog.setAuthorId(UUID.fromString(userId));
        blog.setSlug(generateSlug(blog.getTitle()));
        if ("PUBLISHED".equals(blog.getStatus())) blog.setPublishedAt(Instant.now());
        BlogPost saved = repository.save(blog);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BlogPost>> update(@PathVariable UUID id, @RequestBody BlogPost request) {
        BlogPost existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BlogPost", "id", id.toString()));
        existing.setTitle(request.getTitle());
        existing.setContent(request.getContent());
        existing.setExcerpt(request.getExcerpt());
        existing.setCoverImageUrl(request.getCoverImageUrl());
        existing.setSeoTitle(request.getSeoTitle());
        existing.setSeoDescription(request.getSeoDescription());
        existing.setTags(request.getTags());
        if ("PUBLISHED".equals(request.getStatus()) && existing.getPublishedAt() == null) {
            existing.setPublishedAt(Instant.now());
        }
        existing.setStatus(request.getStatus());
        return ResponseEntity.ok(ApiResponse.success(repository.save(existing)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        BlogPost post = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BlogPost", "id", id.toString()));
        post.setDeletedAt(Instant.now());
        repository.save(post);
        return ResponseEntity.ok(ApiResponse.success(null, "Blog deleted"));
    }

    private String generateSlug(String title) {
        String slug = Normalizer.normalize(title, Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "")
                .toLowerCase().replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
        if (repository.existsBySlug(slug)) slug = slug + "-" + System.currentTimeMillis();
        return slug;
    }
}
