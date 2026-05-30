package com.thedal.thedal_app.voter.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VoterExportResponse {
    private Long jobId; 
    private String status; 
    private String message; 
    
    public VoterExportResponse(Long jobId) {
        this.jobId = jobId;
        this.status = "IN_PROGRESS"; 
        this.message = "Export job has been initiated.";
    }
}