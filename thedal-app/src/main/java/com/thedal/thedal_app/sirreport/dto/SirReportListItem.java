package com.thedal.thedal_app.sirreport.dto;

import com.thedal.thedal_app.sirreport.SirReportStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SirReportListItem {
    private UUID jobId;
    private String baseFileName;
    private String newFileName;
    private SirReportStatus status;
    private Integer additions;
    private Integer deletions;
    private Integer shifts;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
