package com.farmrakshak.auth.controller;

import com.farmrakshak.auth.dto.*;
import com.farmrakshak.auth.service.AuthService;
import com.farmrakshak.shared.dto.ApiResponse;
import com.farmrakshak.shared.exception.BaseException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(ApiResponse.success(response, "Login successful"));
        } catch (BaseException e) {
            return ResponseEntity.status(e.getStatus())
                    .body(ApiResponse.error(e.getCode(), e.getMessage()));
        }
    }

    @PostMapping("/firebase")
    public ResponseEntity<?> firebaseAuth(@Valid @RequestBody FirebaseAuthRequest request) {
        try {
            AuthResponse response = authService.firebaseAuth(request);
            return ResponseEntity.ok(ApiResponse.success(response, "Firebase authentication successful"));
        } catch (BaseException e) {
            log.error("Firebase auth error: {}", e.getMessage());
            return ResponseEntity.status(e.getStatus())
                    .body(ApiResponse.error(e.getCode(), e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected Firebase auth error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("FIREBASE_ERROR", e.getMessage()));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Token refreshed"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestBody(required = false) RefreshTokenRequest request) {
        authService.logout(request != null ? request.getRefreshToken() : null);
        return ResponseEntity.ok(ApiResponse.success(null, "Logged out successfully"));
    }
}
