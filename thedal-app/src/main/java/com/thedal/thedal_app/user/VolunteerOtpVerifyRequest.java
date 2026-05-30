package com.thedal.thedal_app.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class VolunteerOtpVerifyRequest {
    @NotNull
    private Long volunteerUserId;
    
    @NotBlank
    @Size(min = 6, max = 6)
    private String otp;
}