package com.thedal.thedal_app.settings.electionsettings.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReligionResponseDTO {
    private Long id;
    private String religionName;
    private String religionImage;
    private String religionColor;
    
    
}