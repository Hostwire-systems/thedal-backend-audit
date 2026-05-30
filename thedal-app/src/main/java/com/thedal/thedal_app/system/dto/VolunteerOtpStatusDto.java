package com.thedal.thedal_app.system.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VolunteerOtpStatusDto {
    private Long userId;
    private String firstName;
    private String lastName;
    private Boolean isOtpRequired;
}