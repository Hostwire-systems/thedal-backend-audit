package com.thedal.thedal_app.report.pollday.export.dto;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.thedal.thedal_app.report.pollday.export.ExportFilters;
import com.thedal.thedal_app.report.pollday.export.PollDayExportJob.ChartType;
import com.thedal.thedal_app.report.pollday.export.PollDayExportJob.ExportFormat;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExportJobRequest {

    private Long accountId; // Will be set from JWT token

    @NotNull(message = "Election ID is required")
    private Long electionId;

    @NotNull(message = "Format is required")
    private ExportFormat format;

    @NotNull(message = "Chart type is required")
    private ChartType chartType;

    @NotEmpty(message = "At least one part must be selected")
    private List<Integer> selectedParts;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate pollingDate;

    private ExportFilters filters;
}
