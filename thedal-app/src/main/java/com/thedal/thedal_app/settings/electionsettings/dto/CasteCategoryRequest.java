package com.thedal.thedal_app.settings.electionsettings.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CasteCategoryRequest {
    
    @NotBlank(message = "Caste category name is required")
    private String casteCategoryName;
}
