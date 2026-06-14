package com.farmrakshak.advisory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Suggestion {
    private String category;   // IRRIGATION, FERTILIZER, DISEASE_RISK, PEST_RISK, WEATHER, GENERAL
    private String message;
    private String riskLevel;  // HIGH, MEDIUM, LOW
}
