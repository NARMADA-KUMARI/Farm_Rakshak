package com.farmrakshak.crop.dto;

import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateFarmCropRequest {

    private String cropName;
    private String variety;
    private LocalDate sowingDate;
    private LocalDate expectedHarvest;
    private String cropStage;

    @Positive(message = "Area allocated must be positive")
    private BigDecimal areaAllocated;

    private String irrigationType;
    private String status;
}
