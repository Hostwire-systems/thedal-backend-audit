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
public class SirReportStatusResponse {
    private UUID jobId;
    private SirReportStatus status;
    private Integer progress;
    private String message;
}
