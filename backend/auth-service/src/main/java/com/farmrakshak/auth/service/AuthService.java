package com.farmrakshak.auth.service;

import com.farmrakshak.auth.dto.*;
import com.farmrakshak.auth.entity.AuthUser;
import com.farmrakshak.auth.entity.RefreshToken;
import com.farmrakshak.auth.repository.AuthUserRepository;
import com.farmrakshak.auth.repository.RefreshTokenRepository;
import com.farmrakshak.shared.exception.BadRequestException;
import com.farmrakshak.shared.exception.DuplicateResourceException;
import com.farmrakshak.shared.exception.UnauthorizedException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthUserRepository authUserRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (request.getEmail() != null && authUserRepository.existsByEmailAndDeletedAtIsNull(request.getEmail())) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }
        if (request.getMobile() != null && authUserRepository.existsByMobileAndDeletedAtIsNull(request.getMobile())) {
            throw new DuplicateResourceException("User", "mobile", request.getMobile());
        }

        if (request.getEmail() == null && request.getMobile() == null) {
            throw new BadRequestException("Either email or mobile is required");
        }

        AuthUser user = AuthUser.builder()
                .email(request.getEmail())
                .mobile(request.getMobile())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role("FARMER")
                .build();

        user = authUserRepository.save(user);
        log.info("User registered: id={}, email={}", user.getId(), user.getEmail());

        return generateTokens(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        AuthUser user = findByIdentifier(request.getIdentifier());

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        if (!user.getEnabled()) {
            throw new UnauthorizedException("Account is disabled");
        }

        log.info("User logged in: id={}", user.getId());
        return generateTokens(user);
    }

    /**
     * Authenticate via Firebase ID token (Google Sign-In or Phone OTP).
     * - Verifies the token with Firebase Admin SDK
     * - Finds existing user by firebaseUid, email, or phone
     * - Creates a new user if none exists
     * - Links Firebase UID to existing accounts
     */
    @Transactional
    public AuthResponse firebaseAuth(FirebaseAuthRequest request) {
        FirebaseToken firebaseToken;
        try {
            firebaseToken = FirebaseAuth.getInstance().verifyIdToken(request.getFirebaseToken());
        } catch (Exception e) {
            log.error("Firebase token verification failed: {}", e.getMessage());
            throw new UnauthorizedException("Invalid Firebase token");
        }

        String uid = firebaseToken.getUid();
        String email = firebaseToken.getEmail();
        String phone = (String) firebaseToken.getClaims().get("phone_number");
        String name = firebaseToken.getName();
        String provider = request.getProvider(); // "google" or "phone"

        log.info("Firebase auth: provider={}, uid={}, email={}, phone={}", provider, uid, email, phone);

        // 1. Try to find by Firebase UID first
        AuthUser user = authUserRepository.findByFirebaseUidAndDeletedAtIsNull(uid).orElse(null);

        // 2. If not found by UID, try to find by email or phone (link existing account)
        if (user == null && email != null) {
            user = authUserRepository.findByEmailAndDeletedAtIsNull(email).orElse(null);
            if (user != null) {
                user.setFirebaseUid(uid);
                user.setAuthProvider(provider);
                user = authUserRepository.save(user);
                log.info("Linked Firebase UID to existing email user: id={}", user.getId());
            }
        }
        if (user == null && phone != null) {
            // Normalize phone: Firebase returns +91XXXXXXXXXX, DB may store just XXXXXXXXXX
            String normalizedPhone = phone.startsWith("+91") ? phone.substring(3) : phone;
            user = authUserRepository.findByMobileAndDeletedAtIsNull(normalizedPhone).orElse(null);
            if (user == null) {
                user = authUserRepository.findByMobileAndDeletedAtIsNull(phone).orElse(null);
            }
            if (user != null) {
                user.setFirebaseUid(uid);
                user.setAuthProvider(provider);
                user = authUserRepository.save(user);
                log.info("Linked Firebase UID to existing phone user: id={}", user.getId());
            }
        }

        // 3. If still no user found, create new
        if (user == null) {
            String normalizedPhone = phone != null && phone.startsWith("+91") ? phone.substring(3) : phone;
            user = AuthUser.builder()
                    .email(email)
                    .mobile(normalizedPhone)
                    .passwordHash(null) // No password for OAuth users
                    .firebaseUid(uid)
                    .authProvider(provider)
                    .role("FARMER")
                    .build();
            user = authUserRepository.save(user);
            log.info("Created new Firebase user: id={}, provider={}", user.getId(), provider);
        }

        if (!user.getEnabled()) {
            throw new UnauthorizedException("Account is disabled");
        }

        return generateTokens(user);
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String tokenHash = hashToken(request.getRefreshToken());
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (storedToken.getExpiresAt().isBefore(Instant.now())) {
            refreshTokenRepository.delete(storedToken);
            throw new UnauthorizedException("Refresh token expired");
        }

        // Validate the JWT itself
        try {
            jwtService.validateToken(request.getRefreshToken());
        } catch (Exception e) {
            refreshTokenRepository.delete(storedToken);
            throw new UnauthorizedException("Invalid refresh token");
        }

        AuthUser user = authUserRepository.findById(storedToken.getUserId())
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        // Rotate: delete old, create new
        refreshTokenRepository.delete(storedToken);
        log.info("Token refreshed for user: id={}", user.getId());

        return generateTokens(user);
    }

    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken != null) {
            String tokenHash = hashToken(refreshToken);
            refreshTokenRepository.findByTokenHash(tokenHash)
                    .ifPresent(refreshTokenRepository::delete);
        }
        log.info("User logged out");
    }

    private AuthResponse generateTokens(AuthUser user) {
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getRole());
        String refreshToken = jwtService.generateRefreshToken(user.getId());

        // Store refresh token hash
        RefreshToken tokenEntity = RefreshToken.builder()
                .userId(user.getId())
                .tokenHash(hashToken(refreshToken))
                .expiresAt(Instant.now().plusMillis(jwtService.getRefreshTokenExpiry()))
                .build();
        refreshTokenRepository.save(tokenEntity);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getId().toString())
                .role(user.getRole())
                .expiresIn(jwtService.getAccessTokenExpiry() / 1000)
                .build();
    }

    private AuthUser findByIdentifier(String identifier) {
        if (identifier.contains("@")) {
            return authUserRepository.findByEmailAndDeletedAtIsNull(identifier)
                    .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));
        }
        return authUserRepository.findByMobileAndDeletedAtIsNull(identifier)
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
