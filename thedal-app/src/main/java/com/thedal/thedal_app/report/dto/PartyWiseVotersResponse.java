package com.thedal.thedal_app.report.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PartyWiseVotersResponse {
    
    private List<PartyVoterCount> parties;
    private Long totalCount;
}
