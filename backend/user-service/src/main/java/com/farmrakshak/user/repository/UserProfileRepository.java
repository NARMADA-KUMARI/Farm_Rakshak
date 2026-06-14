package com.farmrakshak.user.repository;

import com.farmrakshak.user.entity.UserProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {
    Optional<UserProfile> findByAuthUserIdAndDeletedAtIsNull(UUID authUserId);
    Page<UserProfile> findAllByDeletedAtIsNull(Pageable pageable);
    boolean existsByAuthUserIdAndDeletedAtIsNull(UUID authUserId);
}
