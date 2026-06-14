package com.farmrakshak.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "Email or mobile is required")
    private String identifier;

    @NotBlank(message = "Password is required")
    private String password;
}
