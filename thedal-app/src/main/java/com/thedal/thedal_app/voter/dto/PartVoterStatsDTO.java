package com.thedal.thedal_app.voter.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PartVoterStatsDTO {
    private long maleCount;
    private long femaleCount;
    private long otherCount;
    private long totalVoters;
    private long votedCount;
    private long notVotedCount;
}