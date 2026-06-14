package com.farmrakshak.shared.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Event published when a disease is detected and nearby farmers need to be alerted.
 * One event is published per affected farmer.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiseaseAlertEvent {
    private String targetUserId;
    private String reporterUserId;
    private String cropName;
    private String diseaseName;
    private String description;
    private List<String> treatment;
    private List<String> prevention;
    private String reportedVillage;
    private String reportedDistrict;
    private String reportedState;
    private double distanceKm;
}
