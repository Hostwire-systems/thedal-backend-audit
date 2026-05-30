package com.thedal.thedal_app.settings.electionsettings.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReligionResponseDTO1 {
    private Long religionId;
    private String religionName;
    private String religionImage;
    private Integer orderIndex;
    private Long voterCount;
}