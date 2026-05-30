package com.thedal.thedal_app.settings.electionsettings.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PartyResponse {
    private Long id;
    private String partyName;
    private String partyShortName;
    private String partyImage;
    private String partyColor;
    private String allianceName;
    private Integer orderIndex;
}
