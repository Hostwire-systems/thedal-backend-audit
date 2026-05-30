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
public class SirReportSummaryResponse {
    private UUID jobId;
    private SirReportStatus status;
    private SummaryData summary;
    private LocalDateTime processedAt;
    private String errorMessage;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SummaryData {
        private Integer totalBaseRecords;
        private Integer totalNewRecords;
        private Integer additions;
        private Integer deletions;
        private Integer shifts;
    }
}
