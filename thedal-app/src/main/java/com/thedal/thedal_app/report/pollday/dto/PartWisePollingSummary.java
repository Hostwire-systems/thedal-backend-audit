package com.thedal.thedal_app.report.pollday.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PartWisePollingSummary {
    private Integer totalParts;
    private Long totalVoters;
    private Long totalPolled2025;
    private Long totalPolled2024;
    private Double overallTurnoutPercentage;
    private OffsetDateTime computedAt;
}
