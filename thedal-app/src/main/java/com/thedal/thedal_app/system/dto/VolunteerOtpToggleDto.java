package com.thedal.thedal_app.system.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VolunteerOtpToggleDto {
    @NotNull(message = "Enabled field is required")
    private Boolean enabled;
}
