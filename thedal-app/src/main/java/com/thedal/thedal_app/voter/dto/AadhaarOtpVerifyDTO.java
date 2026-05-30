package com.thedal.thedal_app.voter.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AadhaarOtpVerifyDTO {
    @NotBlank
    private String otp;

    @NotBlank
    private String refId;
}
