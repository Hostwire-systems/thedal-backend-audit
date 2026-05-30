package com.thedal.thedal_app.settings.electionsettings.dto;

import lombok.Data;

@Data
public class CasteCategoryResponseDTO {
    
    private Long id;
    private String casteCategoryName;

    public CasteCategoryResponseDTO(Long id, String casteCategoryName) {
        this.id = id;
        this.casteCategoryName = casteCategoryName;
    }
}