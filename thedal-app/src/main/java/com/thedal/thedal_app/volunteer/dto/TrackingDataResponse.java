package com.thedal.thedal_app.volunteer.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class TrackingDataResponse {
	
	private String volunteerId;
    private List<ActivityDto> activities;
    private LocationDto currentLocation;
    private String currentRoute;
    private int totalHoursWorkedToday;
    private int totalHoursWorkedWeek;

}
