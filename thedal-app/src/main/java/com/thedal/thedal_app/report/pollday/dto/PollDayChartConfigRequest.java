package com.thedal.thedal_app.report.pollday.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PollDayChartConfigRequest {
    // AccountId will be set from JWT token by the controller
    // Client should NOT send this field, but we keep it here for backend usage
    private Long accountId;
    
    @NotNull(message = "Election ID is required")
    private Long electionId;
    
    @NotNull(message = "Charts array is required")
    @Size(min = 1, max = 20, message = "You must have between 1 and 20 charts")
    private List<ChartConfig> charts;
}
