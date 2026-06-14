package com.farmrakshak.advisory.controller;

import com.farmrakshak.advisory.entity.Advisory;
import com.farmrakshak.advisory.repository.AdvisoryRepository;
import com.farmrakshak.shared.dto.ApiResponse;
import com.farmrakshak.shared.dto.PagedResponse;
import com.farmrakshak.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/advisories")
@RequiredArgsConstructor
public class AdvisoryController {

    private final AdvisoryRepository repository;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<Advisory>>> list(
            @RequestParam(required = false) String crop,
            @RequestParam(required = false) String season,
            @RequestParam(required = false) String language,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<Advisory> result = repository.findFiltered(crop, season, language, PageRequest.of(page, Math.min(size, 50)));
        PagedResponse<Advisory> paged = PagedResponse.<Advisory>builder()
                .content(result.getContent()).page(result.getNumber()).size(result.getSize())
                .totalElements(result.getTotalElements()).totalPages(result.getTotalPages()).build();
        return ResponseEntity.ok(ApiResponse.success(paged));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Advisory>> getById(@PathVariable UUID id) {
        Advisory a = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Advisory", "id", id.toString()));
        return ResponseEntity.ok(ApiResponse.success(a));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Advisory>> create(@RequestBody Advisory advisory,
            @RequestHeader("X-User-Id") String userId) {
        advisory.setAuthorId(UUID.fromString(userId));
        Advisory saved = repository.save(advisory);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Advisory>> update(@PathVariable UUID id, @RequestBody Advisory request) {
        Advisory existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Advisory", "id", id.toString()));
        existing.setTitle(request.getTitle());
        existing.setContent(request.getContent());
        existing.setCrop(request.getCrop());
        existing.setSeason(request.getSeason());
        existing.setRegion(request.getRegion());
        existing.setLanguage(request.getLanguage());
        existing.setType(request.getType());
        return ResponseEntity.ok(ApiResponse.success(repository.save(existing)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        Advisory a = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Advisory", "id", id.toString()));
        a.setDeletedAt(Instant.now());
        repository.save(a);
        return ResponseEntity.ok(ApiResponse.success(null, "Advisory deleted"));
    }
}
