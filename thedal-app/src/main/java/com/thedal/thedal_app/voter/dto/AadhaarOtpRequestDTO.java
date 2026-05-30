package com.thedal.thedal_app.voter.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AadhaarOtpRequestDTO {
    @NotBlank
    private String aadhaarNumber;
}
