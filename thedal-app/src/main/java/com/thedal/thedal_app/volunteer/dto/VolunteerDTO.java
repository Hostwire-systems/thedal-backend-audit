package com.thedal.thedal_app.volunteer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.thedal.thedal_app.voter.Address;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class VolunteerDTO {
    private String volunteerId;
    private String firstName;
    private String lastName;
    private String email;
    private String mobileNumber; 
    private String role;
    private AddressDTO address;
    private LocationDto location;
    private String assignedBooth;
    private String status;
    private String photoUrl;
    private String remarks;
    
    private double currentLatitude;
    private double currentLongitude;
    private String currentRoute;
    
    // Optional field for device tracking - only populated when requested
    @JsonProperty("active_device_count")
    private Integer activeDeviceCount;
}
