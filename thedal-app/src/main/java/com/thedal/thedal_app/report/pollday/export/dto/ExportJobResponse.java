package com.thedal.thedal_app.report.pollday.export.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.thedal.thedal_app.report.pollday.export.PollDayExportJob.ChartType;
import com.thedal.thedal_app.report.pollday.export.PollDayExportJob.ExportFormat;
import com.thedal.thedal_app.report.pollday.export.PollDayExportJob.ExportStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExportJobResponse {
    private Long jobId;
    private ExportStatus status;
    private ExportFormat format;
    private ChartType chartType;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime createdAt;
}
