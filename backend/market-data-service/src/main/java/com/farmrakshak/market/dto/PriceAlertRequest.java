package com.farmrakshak.market.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PriceAlertRequest {
    @NotBlank private String cropName;
    private String mandiId;
    @NotNull private BigDecimal thresholdPrice;
    private String direction; // ABOVE, BELOW (default ABOVE)
}
