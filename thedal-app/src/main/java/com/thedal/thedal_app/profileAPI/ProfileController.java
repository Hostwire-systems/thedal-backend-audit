package com.thedal.thedal_app.profileAPI;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.thedal.thedal_app.profileAPI.dtos.BasicProfileRequest;
import com.thedal.thedal_app.profileAPI.dtos.FullProfileRequest;
import com.thedal.thedal_app.profileAPI.dtos.LicenseKeyVerificationResponse;
import com.thedal.thedal_app.profileAPI.dtos.ProfileResponse;
import com.thedal.thedal_app.response.ThedalResponse;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/profiles/settings")
@Validated
public class ProfileController {
	
	@Autowired
	private ProfileService profileService;
	
	
	@Operation(summary = "Add Basic Profile Details", description = "Allows users to add or update basic profile information such as name, email, mobile number, and organization name.")    
    @PostMapping("/basic-profile")
    public ThedalResponse<Void> createBasicProfile(@Valid @RequestBody BasicProfileRequest request) {
        return profileService.saveProfile(request);
    }

	@Operation(summary = "Update Full Profile Details", description = "Allows users to update full profile information including address and subscription details.")
    @PostMapping("/full-profile")
    public ThedalResponse<Void> updateFullProfile(@Valid
           //@PathVariable Long accountId,
            @RequestBody FullProfileRequest request) {
        return profileService.saveFullProfile(request);
    }


    @Operation(summary = "Configure Campaign Settings", description = "Adds the campaign settings for communication services.")
    @PostMapping("/campaign-settings")
    public CampaignSettingsEntity selectSmsService(@Valid @RequestBody CampaignSettingsEntity communicationService) {
        return profileService.selectSmsService(communicationService);
    }
    
//    @Operation(summary = "Verify SMS License Key", description = "Verifies the SMS license key associated with the given campaign settings")
//    @PostMapping("/campaign-settings/sms/verify-license-key")
//    public ThedalResponse<LicenseKeyVerificationResponse> verifyLicenseKey(@RequestBody VerifyLicenseRequest request) {
//        return profileService.verifyLicenseKey(request.getSmsLicenseKey(), request.getSmsMessagingService());
//    }
   
    @Operation(summary = "Verify SMS License Key", description = "Verifies the SMS license key associated with the given campaign settings")
    @PostMapping("/campaign-settings/sms/verify-license-key")
    public ThedalResponse<LicenseKeyVerificationResponse> verifyLicenseKey(@RequestBody LicenseKeyVerificationResponse request) {
        return profileService.verifyLicenseKey(request.getSmsLicenseKey(), request.getSmsMessagingService());
    }
    
    @Operation(summary = "Get Profile Details", description = "Retrieves all profile details including basic and full profile information.")
    @GetMapping("/get-profile-details/{userId}")
    public ThedalResponse<ProfileResponse> getProfileDetails(@PathVariable Long userId) {
        return profileService.getProfileDetails(userId);
    }
    
}
