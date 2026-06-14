package com.farmrakshak.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateProfileRequest {
    @NotBlank(message = "Name is required")
    private String name;
    private String mobile;
    private String email;
    private String village;
    private String district;
    private String state;
    private String primaryCrops;
    private String languagePreference;
}
