package com.thedal.thedal_app.election.dtos;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PartManagerExportStatusResponse {
    private Long jobId;
    private String status;
    private String format;
    private LocalDateTime timeStarted;
    private LocalDateTime timeCompleted;
    private String awsS3DownloadUrl;
    private String localFilePath;
    private String message;
    private Integer totalRecords;
}
