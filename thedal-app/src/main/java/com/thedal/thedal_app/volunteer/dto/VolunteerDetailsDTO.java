package com.thedal.thedal_app.volunteer.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VolunteerDetailsDTO {
	
	private Long volunteerId;
	private Long userId;
    private String firstName;
    private String lastName;
    private String email;
    private String mobileNumber;
    //private String role;
    private AddressDTO address;
    private List<Long> assignedBooths;
    private List<Long> assignedFamilies;
    private String status;
    private String photoUrl;
    private String remarks;
    @JsonIgnore
    private Long accountId;
    
    private String gender;
    private String whatsAppNumber;
    //private Long roleId; 
    private String roleName; 
    
    // Optional field for device tracking - only populated when requested
    @JsonProperty("active_device_count")
    private Integer activeDeviceCount;
    
}
