package com.farmrakshak.crop.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangeCropStageRequest {

    @NotBlank(message = "Crop stage is required")
    private String cropStage;
}
