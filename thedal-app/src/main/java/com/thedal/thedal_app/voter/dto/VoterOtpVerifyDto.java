package com.thedal.thedal_app.voter.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class VoterOtpVerifyDto {
	
	@NotBlank(message = "Mobile number is mandatory")
    private String mobileNo;
    @NotBlank(message = "OTP is mandatory")
    private String otp;

}
