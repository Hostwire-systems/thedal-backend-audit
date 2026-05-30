package com.thedal.thedal_app.volunteer.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VolunteerExportResponse {
    private Long jobId;
    
    public VolunteerExportResponse(Long jobId) {
        this.jobId = jobId;
    }
    
}