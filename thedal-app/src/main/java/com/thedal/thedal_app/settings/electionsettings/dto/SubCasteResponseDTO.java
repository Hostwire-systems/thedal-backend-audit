package com.thedal.thedal_app.settings.electionsettings.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubCasteResponseDTO {
    private Long id;
    private String subCasteName;
    private CasteResponseDTO caste;

}