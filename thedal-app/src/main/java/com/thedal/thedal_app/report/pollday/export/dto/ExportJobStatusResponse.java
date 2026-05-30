package com.thedal.thedal_app.report.pollday.export.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.thedal.thedal_app.report.pollday.export.PollDayExportJob.ChartType;
import com.thedal.thedal_app.report.pollday.export.PollDayExportJob.ExportFormat;
import com.thedal.thedal_app.report.pollday.export.PollDayExportJob.ExportStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExportJobStatusResponse {
    private Long jobId;
    private ExportStatus status;
    private ExportFormat format;
    private ChartType chartType;
    private String s3Url;
    private Integer rowCount;
    private String errorMessage;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime finishedAt;
}
