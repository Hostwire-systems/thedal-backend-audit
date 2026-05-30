package com.thedal.thedal_app.settings.electionsettings.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor 
public class PartyResponseDTO {
    private Long id;
    private String partyName;
    private String partyShortName;
    private String partyImage;
    private String partyColor;
    private String allianceName;
    private Integer orderIndex;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long voterCount;
}