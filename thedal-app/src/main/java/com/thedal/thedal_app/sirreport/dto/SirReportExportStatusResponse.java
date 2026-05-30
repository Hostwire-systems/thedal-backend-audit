package com.thedal.thedal_app.sirreport.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SirReportExportStatusResponse {
    private Long exportJobId;
    private String status;
    private String message;
    private String downloadUrl;
    private Integer recordCount;
    private LocalDateTime timeStarted;
    private LocalDateTime timeCompleted;
    private LocalDateTime expiresAt;
}
