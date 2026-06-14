package com.farmrakshak.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UserProfileResponse {
    private String id;
    private String authUserId;
    private String name;
    private String mobile;
    private String email;
    private String village;
    private String district;
    private String state;
    private String primaryCrops;
    private String languagePreference;
    private String profileImageUrl;
}
