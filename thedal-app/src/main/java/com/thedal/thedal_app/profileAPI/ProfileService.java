package com.thedal.thedal_app.profileAPI;


import java.io.IOException;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.thedal.thedal_app.profileAPI.dto.AddRoleDTO;
import com.thedal.thedal_app.profileAPI.dto.RoleResponseDTO;
import com.thedal.thedal_app.profileAPI.dtos.BasicProfileRequest;
import com.thedal.thedal_app.profileAPI.dtos.FullProfileRequest;
import com.thedal.thedal_app.profileAPI.dtos.LicenseKeyVerificationResponse;
import com.thedal.thedal_app.profileAPI.dtos.MessagingService;
import com.thedal.thedal_app.profileAPI.dtos.ProfileResponse;
import com.thedal.thedal_app.response.ThedalResponse;

import jakarta.validation.Valid;


import java.util.Optional;

public interface ProfileService {
    ThedalResponse<Void> saveProfile(BasicProfileRequest request);
	ThedalResponse<Void> saveFullProfile(FullProfileRequest request);	
	//byte[] getProfilePicture(Long accountId);
	CampaignSettingsEntity selectSmsService(CampaignSettingsEntity communicationService);
	ThedalResponse<LicenseKeyVerificationResponse> verifyLicenseKey(String smslicenseKey, MessagingService smsmessagingService);

	ThedalResponse<ProfileResponse> getProfileDetails(Long userId);
		
}
