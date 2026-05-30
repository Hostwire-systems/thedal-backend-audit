package com.thedal.thedal_app.cpanel.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CasteCategoryUpdateCpanelRequest {
    
    @NotNull(message = "Caste category ID is required")
    private Long casteCategoryId;
    
    @NotBlank(message = "Caste category name is required")
    private String casteCategoryName;
}