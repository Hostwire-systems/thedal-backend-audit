package com.thedal.thedal_app.report.pollday.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PollDayChartConfigResponse {
    private Long id;
    private Long accountId;
    private Long electionId;
    private List<ChartConfig> charts;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
