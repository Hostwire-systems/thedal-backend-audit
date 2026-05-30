package com.thedal.thedal_app.volunteer.dto;

import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VolunteerDetailsUpdate {
	
	    private String firstName;
	    private String lastName;
	    @Email(message = "Invalid email format")
	    private String email;
	    @NotBlank(message = "40319")
	    @Pattern(
	        regexp = "^[0-9]{10}$",
	        message = "40352"
	    )
	    private String mobileNumber;
	    private String role;
	    private AddressDTO address;
	    //private String assignedBooth;
	    //private List<Long> assignedBooth;
	    private List<Long> assignedFamilies;
	    private String status;
	    private String photoUrl;
	    private String remarks;
		private String whatsAppNumber;
        private String gender;
        
        //private Long roleId;
        private String roleName;

}
