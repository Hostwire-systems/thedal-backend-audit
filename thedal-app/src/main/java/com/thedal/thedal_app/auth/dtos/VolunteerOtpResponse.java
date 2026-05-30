package com.thedal.thedal_app.auth.dtos;

import lombok.Data;

@Data
public class VolunteerOtpResponse {
    private Long volunteerUserId;
    
    public VolunteerOtpResponse(Long volunteerUserId) {
        this.volunteerUserId = volunteerUserId;
    }
}