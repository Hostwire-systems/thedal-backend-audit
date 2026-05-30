package com.thedal.thedal_app.auth.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class VerifyMobileOtpDto {

    @NotNull(message = "Mobile number is required")
    @Size(min = 10, max = 10, message = "Invalid Mobile number(length must be 10)")
    @Pattern(regexp = "\\d+", message = "Mobile number must contain only numbers")
    private String mobileNumber;

    @NotNull(message = "OTP is required")
    @Size(min = 6, max = 6, message = "Invalid OTP (length must be 6)")
    @Pattern(regexp = "\\d+", message = "OTP must contain only numbers")
    private String otp;

}
