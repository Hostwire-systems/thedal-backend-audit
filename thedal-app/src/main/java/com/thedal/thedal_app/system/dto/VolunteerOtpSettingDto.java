package com.thedal.thedal_app.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VolunteerOtpSettingDto {
    @NotBlank(message = "Setting value is required")
    private String settingValue; // "enabled" or "disabled"
    
    private String description;
}
