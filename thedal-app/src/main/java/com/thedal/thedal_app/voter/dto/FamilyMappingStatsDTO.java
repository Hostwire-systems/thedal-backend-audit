package com.thedal.thedal_app.voter.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FamilyMappingStatsDTO {
    private Long unmappedVoterCount;        // Voters with null family_id
    private Long singleVoterFamilyCount;    // Number of families with exactly 1 member
    private Long totalCount;                 // Total voters
}
