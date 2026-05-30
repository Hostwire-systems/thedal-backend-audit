package com.thedal.thedal_app.sirreport.dto;

import com.thedal.thedal_app.sirreport.SirReportStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SirReportUploadResponse {
    private UUID jobId;
    private SirReportStatus status;
    private String message;
}
