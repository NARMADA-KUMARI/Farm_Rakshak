package com.farmrakshak.market.dto;

import lombok.*;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CropSearchResponse {
    private String cropName;
    private String category;
    private String unit;
    private String scientificName;
    private List<String> localNames;
    private boolean inUserFarm;
    private String farmName; // If in user farm, which farm
}
