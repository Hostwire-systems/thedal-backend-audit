package com.thedal.thedal_app.election.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class DefaultPartyResponse {
    
    private Long electionId;
    private Long defaultPartyId;
    private String partyName;
    private String partyShortName;
    private String partyImage;
    private String partyColor;
    private String message;
}
