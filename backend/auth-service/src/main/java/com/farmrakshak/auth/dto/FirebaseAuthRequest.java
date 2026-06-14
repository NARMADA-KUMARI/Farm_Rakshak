package com.farmrakshak.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FirebaseAuthRequest {

    @NotBlank(message = "Firebase token is required")
    private String firebaseToken;

    @NotBlank(message = "Provider is required")
    private String provider; // "google" or "phone"
}
