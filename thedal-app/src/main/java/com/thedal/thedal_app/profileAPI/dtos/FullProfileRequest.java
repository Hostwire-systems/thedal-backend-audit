package com.thedal.thedal_app.profileAPI.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FullProfileRequest{
	
	@NotBlank(message = "Billing address cannot be empty")
	private String billingAddress;
	
	@NotBlank(message = "State cannot be empty")
    private String state;
	
	@Pattern(regexp = "^[0-9]{6}$", message = "Pincode must be 6 digits")
    private String pincode;
	
    private String countryCode;
    private String gst; // Optional
    
    private SubscriptionType subscription;

}
