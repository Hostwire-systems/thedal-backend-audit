package com.thedal.thedal_app.profileAPI.dtos;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class LicenseKeyVerificationResponse {
	
	private MessagingService smsMessagingService;
    private String smsLicenseKey;
    
 // Constructor
    public LicenseKeyVerificationResponse(MessagingService messagingService, String smsLicenseKey)
                                            {     
        this.smsMessagingService = messagingService;
        this.smsLicenseKey = smsLicenseKey;
    }

}
