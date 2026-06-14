package com.farmrakshak.user.controller;

import com.farmrakshak.shared.dto.ApiResponse;
import com.farmrakshak.shared.dto.PagedResponse;
import com.farmrakshak.user.dto.CreateProfileRequest;
import com.farmrakshak.user.dto.UserProfileResponse;
import com.farmrakshak.user.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserProfileService service;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getMyProfile(
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(ApiResponse.success(service.getProfile(UUID.fromString(userId))));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateMyProfile(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody CreateProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                service.createOrUpdateProfile(UUID.fromString(userId), request)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUser(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(service.getProfileById(id)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<UserProfileResponse>>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<UserProfileResponse> result = service.listProfiles(PageRequest.of(page, Math.min(size, 50)));
        PagedResponse<UserProfileResponse> paged = PagedResponse.<UserProfileResponse>builder()
                .content(result.getContent())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
        return ResponseEntity.ok(ApiResponse.success(paged));
    }
}
