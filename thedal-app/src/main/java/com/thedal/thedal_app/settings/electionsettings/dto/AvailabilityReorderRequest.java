package com.thedal.thedal_app.settings.electionsettings.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AvailabilityReorderRequest {
    private Long availabilityId;
    private Integer newOrderIndex;
}
