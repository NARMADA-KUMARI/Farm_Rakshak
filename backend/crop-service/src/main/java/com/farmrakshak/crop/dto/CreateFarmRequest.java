package com.farmrakshak.crop.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateFarmRequest {

    @NotBlank(message = "Farm name is required")
    private String farmName;

    private String village;
    private String district;
    private String state;
    private BigDecimal latitude;
    private BigDecimal longitude;

    @Positive(message = "Total area must be positive")
    private BigDecimal totalArea;

    private String areaUnit;
    private String soilType;
}
