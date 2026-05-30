package com.thedal.thedal_app.volunteer.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VolunteerLocationDto {
	private Long volunteerId;
	private Long userId;
    private String firstName;
    private String lastName;
    private String mobileNumber;
//    private double latitude;
//    private double longitude;
    private LocationDto location;
//    private double currentLatitude;
//    private double currentLongitude;
//    private String assignedBooth;
    private List<Long> assignedBooth;
    private String status;
	}