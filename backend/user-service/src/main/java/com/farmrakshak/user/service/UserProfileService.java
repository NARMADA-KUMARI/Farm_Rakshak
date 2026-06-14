package com.farmrakshak.user.service;

import com.farmrakshak.shared.exception.ResourceNotFoundException;
import com.farmrakshak.user.dto.CreateProfileRequest;
import com.farmrakshak.user.dto.UserProfileResponse;
import com.farmrakshak.user.entity.UserProfile;
import com.farmrakshak.user.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserProfileRepository repository;

    @Transactional
    public UserProfileResponse createOrUpdateProfile(UUID authUserId, CreateProfileRequest request) {
        UserProfile profile = repository.findByAuthUserIdAndDeletedAtIsNull(authUserId)
                .orElse(UserProfile.builder().authUserId(authUserId).build());

        profile.setName(request.getName());
        profile.setMobile(request.getMobile());
        profile.setEmail(request.getEmail());
        profile.setVillage(request.getVillage());
        profile.setDistrict(request.getDistrict());
        profile.setState(request.getState());
        profile.setPrimaryCrops(request.getPrimaryCrops());
        if (request.getLanguagePreference() != null) {
            profile.setLanguagePreference(request.getLanguagePreference());
        }

        profile = repository.save(profile);
        log.info("Profile saved for user: {}", authUserId);
        return toResponse(profile);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(UUID authUserId) {
        UserProfile profile = repository.findByAuthUserIdAndDeletedAtIsNull(authUserId)
                .orElseThrow(() -> new ResourceNotFoundException("UserProfile", "authUserId", authUserId.toString()));
        return toResponse(profile);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfileById(UUID id) {
        UserProfile profile = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("UserProfile", "id", id.toString()));
        return toResponse(profile);
    }

    @Transactional(readOnly = true)
    public Page<UserProfileResponse> listProfiles(Pageable pageable) {
        return repository.findAllByDeletedAtIsNull(pageable).map(this::toResponse);
    }

    private UserProfileResponse toResponse(UserProfile p) {
        return UserProfileResponse.builder()
                .id(p.getId().toString())
                .authUserId(p.getAuthUserId().toString())
                .name(p.getName())
                .mobile(p.getMobile())
                .email(p.getEmail())
                .village(p.getVillage())
                .district(p.getDistrict())
                .state(p.getState())
                .primaryCrops(p.getPrimaryCrops())
                .languagePreference(p.getLanguagePreference())
                .profileImageUrl(p.getProfileImageUrl())
                .build();
    }
}
