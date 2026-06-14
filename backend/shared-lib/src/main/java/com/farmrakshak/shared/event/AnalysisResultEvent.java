package com.farmrakshak.shared.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisResultEvent {
    private String analysisId;
    private String userId;
    private String status;
    private String cropName;
    private String diseaseName;
    private Double confidence;
    private String description;
    private List<String> treatment;
    private List<String> prevention;
}
