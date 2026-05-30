package com.thedal.thedal_app.election.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PartManagerExportRequest {
    private String format; // "PDF" or "EXCEL"
}
