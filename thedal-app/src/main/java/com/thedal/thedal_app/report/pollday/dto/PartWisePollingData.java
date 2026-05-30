package com.thedal.thedal_app.report.pollday.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PartWisePollingData {
    private Integer partNumber;
    private Long totalVoters;
    private Long polled2025;
    private Long polled2024;
    private Long didNotVote;
    private Double turnoutPercentage;
    private OffsetDateTime lastUpdated;
}
