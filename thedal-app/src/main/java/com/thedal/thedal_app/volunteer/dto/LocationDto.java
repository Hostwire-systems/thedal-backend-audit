package com.thedal.thedal_app.volunteer.dto;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

@Embeddable
@Getter
@Setter
public class LocationDto {
	
	private double latitude;
    private double longitude;

    public LocationDto() {}

    public LocationDto(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }
	
	
}
