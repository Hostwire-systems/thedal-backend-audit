package com.thedal.thedal_app.settings.electionsettings.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CasteCategoryReorderRequest {
    
    @NotNull(message = "Caste category ID is required")
    private Long casteCategoryId;
    
    @NotNull(message = "New order index is required")
    private Integer newOrderIndex;
}