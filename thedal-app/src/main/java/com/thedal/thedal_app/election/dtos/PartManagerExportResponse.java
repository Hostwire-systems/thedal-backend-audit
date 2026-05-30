package com.thedal.thedal_app.election.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PartManagerExportResponse {
    private Long jobId;
    private String status;
    private String message;
    private String format;
}
