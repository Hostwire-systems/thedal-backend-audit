package com.thedal.thedal_app.sirreport.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SirReportExportInitiateResponse {
    private Long exportJobId;
    private String status;
    private String message;
}
