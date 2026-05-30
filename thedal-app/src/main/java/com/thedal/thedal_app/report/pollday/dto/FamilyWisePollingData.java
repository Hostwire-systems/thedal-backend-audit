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
public class FamilyWisePollingData {
    private Integer partNumber;
    private Long totalFamilies;
    private Long fullyVotedFamilies;     // All members voted
    private Long partiallyVotedFamilies; // Some members voted
    private Long notVotedFamilies;       // No members voted
    private Double votingPercentage;     // Percentage based on fully voted families
    private OffsetDateTime timestamp;
}
