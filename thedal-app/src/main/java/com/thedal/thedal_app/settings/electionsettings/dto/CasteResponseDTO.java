package com.thedal.thedal_app.settings.electionsettings.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CasteResponseDTO {
    private Long id;
    private String casteName;
    private ReligionResponseDTO religion;
}