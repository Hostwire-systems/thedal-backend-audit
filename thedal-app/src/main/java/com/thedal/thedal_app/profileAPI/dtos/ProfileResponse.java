package com.thedal.thedal_app.profileAPI.dtos;

import lombok.Data;

@Data
public class ProfileResponse {
    private String fullName;
    private String email;
    private String mobileNumber;
    private String organizationName;
    private String billingAddress;
    private String state;
    private String pincode;
    private String countryCode;
    private String gst;
    private SubscriptionType subscription;
    private String profilePicture;
    private String alternateMobileNumber;
    private String alternateEmailId;
    
    
    
}
