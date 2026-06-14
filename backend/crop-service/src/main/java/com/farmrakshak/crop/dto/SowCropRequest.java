package com.farmrakshak.crop.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SowCropRequest {
    @NotNull(message = "cropId is required")
    private UUID cropId;

    @NotNull(message = "sowingDate is required")
    private LocalDate sowingDate;

    private BigDecimal landArea;
    private String landAreaUnit;
    private String soilType;
    private String irrigationType;
}
