package com.thedal.thedal_app.report.pollday.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FamilyWisePollingSummary {
    private Integer totalParts;
    private Long totalFamilies;
    private Long totalVotedFamilies;        // All members voted
    private Long partiallyVotedFamilies;    // Some members voted
    private Long notVotedFamilies;          // No members voted
    private Double overallVotingPercentage;
    private OffsetDateTime timestamp;
}
