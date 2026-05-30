package com.thedal.thedal_app.voter.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ElectionVoterStatsDTO {
    private Long electionId;
    private Long totalVoters;
    private Long votedCount;
    private Long notVotedCount;
    private Double turnoutPercentage;
}
