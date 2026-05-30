package com.thedal.thedal_app.volunteer.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateVolunteerDto {
	private double latitude;
    private double longitude;
    private String currentRoute;
    private int hoursWorkedToday;
    private int hoursWorkedWeek;
    private String remarks;
}