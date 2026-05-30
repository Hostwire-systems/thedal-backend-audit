package com.thedal.thedal_app.dto;

import lombok.Data;

@Data
public class FamilyMappingOtpVerificationDTO {
    private Long electionId;
    private String mobileNumber;
    private String otp;
}