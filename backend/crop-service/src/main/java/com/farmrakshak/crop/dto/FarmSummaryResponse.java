package com.farmrakshak.crop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FarmSummaryResponse {

    private String id;
    private String farmName;
    private String village;
    private String district;
    private String state;
    private BigDecimal totalArea;
    private String areaUnit;
    private BigDecimal allocatedArea;
    private BigDecimal remainingArea;
    private long cropCount;
}
