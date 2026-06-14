package com.farmrakshak.auth.repository;

import com.farmrakshak.auth.entity.AuthUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AuthUserRepository extends JpaRepository<AuthUser, UUID> {
    Optional<AuthUser> findByEmailAndDeletedAtIsNull(String email);
    Optional<AuthUser> findByMobileAndDeletedAtIsNull(String mobile);
    Optional<AuthUser> findByFirebaseUidAndDeletedAtIsNull(String firebaseUid);
    boolean existsByEmailAndDeletedAtIsNull(String email);
    boolean existsByMobileAndDeletedAtIsNull(String mobile);
}
