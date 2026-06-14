package com.farmrakshak.shared.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FarmCropEvent {

    @Builder.Default
    private String eventId = UUID.randomUUID().toString();

    private String eventType;
    private String userId;
    private String farmId;
    private String cropId;
    private String cropName;
    private String cropStage;
    private String farmName;
    private String village;
    private String district;
    private String state;
    private String sowingDate;

    @Builder.Default
    private String timestamp = Instant.now().toString();
}
