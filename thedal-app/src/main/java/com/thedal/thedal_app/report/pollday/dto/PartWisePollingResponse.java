package com.thedal.thedal_app.report.pollday.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PartWisePollingResponse {
    private List<PartWisePollingData> parts;
    private PartWisePollingSummary summary;
}
