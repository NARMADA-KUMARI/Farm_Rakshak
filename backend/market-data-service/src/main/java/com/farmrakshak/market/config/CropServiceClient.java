package com.farmrakshak.market.config;

import com.farmrakshak.market.dto.UserCropInfo;
import com.farmrakshak.shared.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST client for inter-service communication with crop-service.
 * Uses RestTemplate with Eureka service discovery URL.
 */
@Slf4j
@Component
public class CropServiceClient {

    private final RestTemplate restTemplate;
    private final String cropServiceUrl;

    public CropServiceClient(@Value("${services.crop-service-url}") String cropServiceUrl) {
        this.restTemplate = new RestTemplate();
        this.cropServiceUrl = cropServiceUrl;
    }

    /**
     * Fetches all active crops for a user with their farm locations.
     * Calls: GET /internal/crops/user/{userId}
     */
    public List<UserCropInfo> getUserCrops(UUID userId) {
        try {
            String url = cropServiceUrl + "/internal/crops/user/" + userId;
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            if (response.getBody() != null && Boolean.TRUE.equals(response.getBody().get("success"))) {
                List<Map<String, Object>> data = (List<Map<String, Object>>) response.getBody().get("data");
                if (data == null) return Collections.emptyList();

                return data.stream().map(m -> UserCropInfo.builder()
                        .cropId(str(m, "cropId"))
                        .cropName(str(m, "cropName"))
                        .farmId(str(m, "farmId"))
                        .farmName(str(m, "farmName"))
                        .village(str(m, "village"))
                        .district(str(m, "district"))
                        .state(str(m, "state"))
                        .latitude(m.get("latitude") != null ? new java.math.BigDecimal(m.get("latitude").toString()) : null)
                        .longitude(m.get("longitude") != null ? new java.math.BigDecimal(m.get("longitude").toString()) : null)
                        .build()).toList();
            }
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Failed to fetch user crops from crop-service for userId={}: {}", userId, e.getMessage());
            return Collections.emptyList();
        }
    }

    private String str(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v != null ? v.toString() : null;
    }
}
