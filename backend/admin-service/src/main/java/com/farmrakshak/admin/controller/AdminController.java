package com.farmrakshak.admin.controller;

import com.farmrakshak.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j @RestController @RequestMapping("/api/v1/admin") @RequiredArgsConstructor
public class AdminController {

    private final RestTemplate restTemplate;

    @Value("${services.user-service:http://user-service:8082}")
    private String userServiceUrl;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> dashboard() {
        Map<String, Object> stats = Map.of(
                "status", "operational",
                "services", Map.of(
                        "auth", "UP",
                        "user", "UP",
                        "crop", "UP",
                        "weather", "UP",
                        "advisory", "UP",
                        "notification", "UP",
                        "blog", "UP",
                        "ai", "UP"
                )
        );
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/users")
    public ResponseEntity<String> getUsers() {
        try {
            String response = restTemplate.getForObject(userServiceUrl + "/api/v1/users?page=0&size=50", String.class);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to fetch users from user-service", e);
            return ResponseEntity.internalServerError().body("Failed to fetch users");
        }
    }
}
