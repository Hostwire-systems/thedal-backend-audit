package com.thedal.thedal_app.election.dtos;

import lombok.Data;

@Data
public class UpdateOtpRequiredRequest {
    private Boolean isOtpRequired;
}