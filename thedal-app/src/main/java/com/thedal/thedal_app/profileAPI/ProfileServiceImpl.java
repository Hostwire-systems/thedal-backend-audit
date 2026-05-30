package com.thedal.thedal_app.profileAPI;


import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.thedal.thedal_app.account.AccountEntity;
import com.thedal.thedal_app.account.AccountOnBoardStatus;
import com.thedal.thedal_app.account.AccountRepository;
import com.thedal.thedal_app.general.RequestDetailsService;
import com.thedal.thedal_app.profileAPI.dtos.BasicProfileRequest;
import com.thedal.thedal_app.profileAPI.dtos.FullProfileRequest;
import com.thedal.thedal_app.profileAPI.dtos.LicenseKeyVerificationResponse;
import com.thedal.thedal_app.profileAPI.dtos.MessagingService;
import com.thedal.thedal_app.profileAPI.dtos.ProfileResponse;
import com.thedal.thedal_app.response.ThedalResponse;
import com.thedal.thedal_app.response.ThedalSuccess;
import com.thedal.thedal_app.role.RolePermission;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;
import com.thedal.thedal_app.user.UserEntity;
import com.thedal.thedal_app.user.UserRepo;
import com.thedal.thedal_app.util.CountryCodeUtil;
import com.thedal.thedal_app.util.ValidationUtil;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ProfileServiceImpl implements ProfileService{

	private static final Long PROTECTED_PROFILES = -1L;

    private final ProfileRepository profileRepository;
    //private final AccountService accountService;
    private final CampaignSettingsRepo campaignRepo;
    private final AccountRepository accountRepository;
    private final CountryCodeUtil countryCodeUtil;
    private final UserRepo userRepo;
    private final RequestDetailsService requestDetails;
    
    @Autowired
    public ProfileServiceImpl(ProfileRepository profileRepository,
    		CampaignSettingsRepo campaignRepo, AccountRepository accountRepository ,CountryCodeUtil countryCodeUtil, UserRepo userRepo,
    		RequestDetailsService requestDetails) {
        this.profileRepository = profileRepository;
        //this.accountService = accountService;
        this.campaignRepo=campaignRepo;
        this.accountRepository=accountRepository;
        this.countryCodeUtil=countryCodeUtil;
        this.userRepo=userRepo;
        this.requestDetails=requestDetails;
    }
    
    /**
     * Saves the basic profile information for the current account.
     *
     * This method retrieves the current account from the request, validates the provided 
     * email and mobile number, and updates or creates a profile associated with the account. 
     * It also updates the onboarding status of the account upon successful profile saving.
     *
     * @param request the request object containing the basic profile information to be saved
     * @return a ThedalResponse object indicating the success of the operation
     * @throws ThedalException if the account is not found or if validation fails, including 
     *                         scenarios where the email or mobile number is invalid.
     */
    
    @Override
    @Transactional
    public ThedalResponse<Void> saveProfile(BasicProfileRequest request) {
    	log.info("Attempting to save profile for request: {}", request);
        requestDetails.checkUserRolePermission(RolePermission.SETTINGS_MANAGEMENT);

        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account id not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        // Use ValidationUtil for validation
        ValidationUtil.validateEmail(request.getEmail());
        //ValidationUtil.validateEmail(request.getAlternateEmailId());
        ValidationUtil.validateMobileNumber(request.getMobileNumber());
        //ValidationUtil.validateMobileNumber(request.getAlternateMobileNumber());
        
        // Validate optional fields if present
        if (request.getAlternateEmailId() != null) {
            ValidationUtil.validateEmail(request.getAlternateEmailId());
        }

        if (request.getAlternateMobileNumber() != null) {
            ValidationUtil.validateMobileNumber(request.getAlternateMobileNumber());
        }

        // Extract country code and mobile number
        CountryCodeUtil.PhoneNumberInfo phoneInfo = countryCodeUtil.extractCountryCodeAndMobile(request.getMobileNumber());
        String countryCode = phoneInfo.getCountryCode();
        String mobileNumber = phoneInfo.getMobileNumber();
       // String alternateMobileNumber=phoneInfo.getAlternateMobileNumber();
     
        // Validate mobile number without country code
        // Check for missing required fields
        if (request.getOrganizationName() == null || request.getOrganizationName().isEmpty()) {
            log.error("Organization name is required but not provided.");
            throw new ThedalException(ThedalError.MISSING_REQUIRED_FIELD, HttpStatus.BAD_REQUEST);
        }
        
        ProfileEntity profile = getOrCreateProfile(accountId); // Pass accountId

        updateProfileDetails(profile, request, mobileNumber, countryCode);

        profileRepository.save(profile);
        log.info("Profile saved successfully for account ID: {}", accountId);

        // Assuming accountRepository has a method to find by account ID
        AccountEntity account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.NOT_FOUND));
        
        account.setOnBoardStatus(AccountOnBoardStatus.PROFILE_SETUP_1.getValue());
        accountRepository.save(account);
        log.info("Onboarding status updated for account ID: {}", accountId);

        return new ThedalResponse<>(ThedalSuccess.BASIC_PROFILE_ADDED);
    }   
    
    private ProfileEntity getOrCreateProfile(Long accountId) {
    	// Attempt to find an existing profile associated with the account ID
        Optional<ProfileEntity> optionalProfile = profileRepository.findByAccountId(accountId);
        
        // If a profile exists, return it
        if (optionalProfile.isPresent()) {
            return optionalProfile.get();
        }
        
        // If no profile exists, create a new one
        ProfileEntity newProfile = new ProfileEntity();
        
        // Set necessary properties for the new profile
        newProfile.setAccountId(accountId);
        // Set other default properties as needed
        newProfile.setFullName(""); 
        newProfile.setEmail(""); 
        newProfile.setMobileNumber(""); 
        newProfile.setCountryCode(""); 
        newProfile.setAlternateEmailId("");
        newProfile.setAlternateMobileNumber("");
        newProfile.setOrganizationName("");
       
        profileRepository.save(newProfile);

        return newProfile;
	}
    
    
    // Helper method to save basic profile details
       private void updateProfileDetails(ProfileEntity profile, BasicProfileRequest request, String mobileNumber, String countryCode) {
           profile.setFullName(request.getFullName());
           profile.setEmail(request.getEmail());
           profile.setMobileNumber(mobileNumber); // Set extracted mobile number
           profile.setCountryCode(countryCode);   // Set extracted country code
           profile.setOrganizationName(request.getOrganizationName());
//           profile.setAlternateMobileNumber(request.getAlternateMobileNumber());
//           profile.setAlternateEmailId(request.getAlternateEmailId());
           // Update optional fields only if present
           if (request.getAlternateMobileNumber() != null) {
               profile.setAlternateMobileNumber(request.getAlternateMobileNumber());
           }

           if (request.getAlternateEmailId() != null) {
               profile.setAlternateEmailId(request.getAlternateEmailId());
           }
       }
       

	/**
     * Saves the full profile information for the current account.
     *
     * This method retrieves the current account, validates the country code provided in 
     * the request, and updates the full profile details associated with the account. 
     * It also updates the onboarding status of the account upon successful full profile saving.
     *
     * @param request the request object containing the full profile information to be saved
     * @return a ThedalResponse object indicating the success of the operation
     * @throws ThedalException if the account is not found or if the country code is invalid.
     */

    @Override
    public ThedalResponse<Void> saveFullProfile(FullProfileRequest request) {
        log.info("Attempting to save full profile for request: {}", request);

        // Fetch the account ID from the current request
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account ID not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        // Use the account ID to get or create a profile
        ProfileEntity profile = getOrCreateProfile(accountId);

        // Validate the country code
        if (!countryCodeUtil.isValidCountryCode(request.getCountryCode())) {
            log.error("Invalid country code: {}", request.getCountryCode());
            throw new ThedalException(ThedalError.INVALID_COUNTRY_CODE, HttpStatus.BAD_REQUEST);
        }

        updateFullProfileDetails(profile, request);

        profileRepository.save(profile);
        log.info("Full profile saved successfully for account ID: {}", accountId);

        AccountEntity account = accountRepository.findById(accountId)
            .orElseThrow(() -> new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.NOT_FOUND));
        
        account.setOnBoardStatus(AccountOnBoardStatus.PROFILE_SETUP_2.getValue());
        accountRepository.save(account);
        log.info("Onboarding status updated to PROFILE_SETUP_2 for account ID: {}", accountId);

        return new ThedalResponse<>(ThedalSuccess.FULL_PROFILE_ADDED);
    }

 
 // Helper method to update full profile details
    private void updateFullProfileDetails(ProfileEntity profile, FullProfileRequest request) {
        profile.setBillingAddress(request.getBillingAddress());
        profile.setState(request.getState());
        profile.setPincode(request.getPincode());
        profile.setCountryCode(request.getCountryCode());
        profile.setGst(request.getGst());
        profile.setSubscription(request.getSubscription());
    }
	
    /**
     * Selects and saves the SMS communication service settings for the current account.
     *
     * This method validates the provided SMS messaging service and license key, retrieves 
     * the current account, and either updates an existing SMS service setting or creates a new 
     * one associated with the account. It also updates the onboarding status of the account.
     *
     * @param communicationService the campaign settings entity containing the SMS service details
     * @return the saved CampaignSettingsEntity with updated settings
     * @throws ThedalException if the messaging service or license key is invalid, 
     *                         or if the account is not found.
     */

    @Override
    public CampaignSettingsEntity selectSmsService(CampaignSettingsEntity communicationService) {
        log.info("Selecting SMS service: {}", communicationService);

        // Validate messaging service
        if (communicationService.getSmsMessagingService() == null) {
            log.error("Invalid messaging service provided.");
            throw new ThedalException(ThedalError.INVALID_MESSAGING_SERVICE, HttpStatus.BAD_REQUEST);
        }

        // Validate SMS License Key
        if (communicationService.getSmsLicenseKey() == null || communicationService.getSmsLicenseKey().trim().isEmpty()) {
            log.error("Invalid license key provided.");
            throw new ThedalException(ThedalError.INVALID_LICENSE_KEY, HttpStatus.BAD_REQUEST);
        }

        // Get the current account ID from the request
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account ID not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        communicationService.setAccountId(accountId);
        log.info("Account ID set to: {}", accountId);

        // Check if the service already exists for the account
        Optional<CampaignSettingsEntity> existingService = campaignRepo.findByAccountId(accountId);

        // Update account onboarding status
        AccountEntity account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.NOT_FOUND));
        
        account.setOnBoardStatus(AccountOnBoardStatus.CAMPAIGN_SETTINGS.getValue());
        accountRepository.save(account);
        log.info("Onboarding status updated to CAMPAIGN_SETTINGS for account ID: {}", accountId);

        CampaignSettingsEntity serviceToSave;

        // If the service exists, update it; otherwise, create a new one
        if (existingService.isPresent()) {
            CampaignSettingsEntity serviceToUpdate = existingService.get();
            serviceToUpdate.setSmsMessagingService(communicationService.getSmsMessagingService());
            serviceToUpdate.setSmsLicenseKey(communicationService.getSmsLicenseKey());
            serviceToUpdate.setSmsVerificationStatus("Not Verified");  // Set status to "Not Verified"
            serviceToSave = campaignRepo.save(serviceToUpdate);
            log.info("Existing SMS service updated for account ID: {}", accountId);
        } else {
            communicationService.setSmsVerificationStatus("Not Verified");  // Set status to "Not Verified"
            serviceToSave = campaignRepo.save(communicationService);
            log.info("New SMS service saved for account ID: {}", accountId);
        }

        log.info("SMS service verified successfully for account ID: {}", accountId);
        return serviceToSave;  
    }

    
    
	/**
	 * Verifies the license key for the specified messaging service.
	 *
	 * This method checks if the provided license key matches the one stored in the campaign settings for the
	 * current account and messaging service. If the license key is valid, it updates the verification status and
	 * returns a successful response with the messaging service and license key. If invalid, it throws an exception.
	 *
	 * @param smsLicenseKey       the license key to be verified
	 * @param smsMessagingService the messaging service associated with the license key
	 * @return a {@link ThedalResponse} containing the response details including the messaging service and license key
	 * @throws ThedalException if the account is not found, the service is not found, or the license key is invalid
	 */
	
    @Override
    public ThedalResponse<LicenseKeyVerificationResponse> verifyLicenseKey(String smsLicenseKey, MessagingService smsMessagingService) {
        log.info("Verifying license key for messaging service: {}", smsMessagingService);

        // Get the current account ID from the request
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        // Retrieve the campaign settings for the account and messaging service
        Optional<CampaignSettingsEntity> serviceOpt = campaignRepo.findByAccountIdAndSmsMessagingService(accountId, smsMessagingService);

        if (serviceOpt.isEmpty()) {
            log.error("Service not found for account ID: {} and messaging service: {}", accountId, smsMessagingService);
            throw new ThedalException(ThedalError.SERVICE_NOT_FOUND, HttpStatus.NOT_FOUND);
        }

        CampaignSettingsEntity service = serviceOpt.get();

        // Verify the license key
        if (service.getSmsLicenseKey().equals(smsLicenseKey)) {
            service.setSmsVerificationStatus("Valid");
            campaignRepo.save(service);
            log.info("License key verified successfully for account ID: {}", accountId);

            LicenseKeyVerificationResponse response = new LicenseKeyVerificationResponse(
                service.getSmsMessagingService(), service.getSmsLicenseKey());

            return new ThedalResponse<>(ThedalSuccess.LICENSE_KEY_VERIFIED, response);
        } else {
            service.setSmsVerificationStatus("Invalid");
            campaignRepo.save(service);
            log.warn("Invalid license key for account ID: {}", accountId);
            throw new ThedalException(ThedalError.INVALID_LICENSE_KEY, HttpStatus.BAD_REQUEST);
        }
    }
 
	
	/**
	 * Retrieves the profile information for the current account.
	 *
	 * This method retrieves the current account, checks for an associated profile,
	 * and returns the profile information. If no profile exists, it returns a response 
	 * indicating that no profile is found.
	 *
	 * @return a ThedalResponse containing the ProfileResponse with profile details, 
	 *         or an error message if no profile is found.
	 * @throws ThedalException if the account is not found or if there is an error retrieving the profile.
	 */
    @Override
    public ThedalResponse<ProfileResponse> getProfileDetails(Long userId) {
        log.info("Retrieving profile for current account.");

        // Get the current account from the request
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }


        // Fetch the UserEntity associated with the account
//        UserEntity user = userRepo.findByAccountEntityId(accountId)
//                .orElseThrow(() -> new ThedalException(ThedalError.USER_NOT_FOUND, HttpStatus.NOT_FOUND));
//        UserEntity user = userRepo.findByAccountEntityIdAndEmail(accountId, email)
//                .orElseThrow(() -> {
//                    log.error("No user found for account ID: {} and email: {}", accountId, email);
//                    return new ThedalException(ThedalError.USER_NOT_FOUND, HttpStatus.NOT_FOUND);
//                });
        UserEntity user = userRepo.findByIdAndAccountEntityId(userId, accountId)
                .orElseThrow(() -> {
                    log.error("No user found for user ID: {} and account ID: {}", userId, accountId);
                    return new ThedalException(ThedalError.USER_NOT_FOUND, HttpStatus.NOT_FOUND);
                });

//        if (user.getAccountId() == null) {
//            log.error("Account ID is null for user ID: {}", userId);
//            throw new ThedalException(ThedalError.USER_ACCOUNT_MISMATCH, HttpStatus.FORBIDDEN);
//        }
        
     // Fetch the profile associated with the account
        ProfileEntity profile = profileRepository.findByAccountId(accountId)
                .orElseThrow(() -> {
                    log.error("No profile found for account ID: {}", accountId);
                    return new ThedalException(ThedalError.PROFILE_NOT_FOUND, HttpStatus.NOT_FOUND);
                });
        
        
        ProfileResponse response = new ProfileResponse();
        response.setFullName(profile.getFullName());
        //response.setEmail(user.getEmail()); // Get email from UserEntity
        response.setEmail(profile.getEmail());
        response.setMobileNumber(profile.getMobileNumber());
        response.setCountryCode(profile.getCountryCode());
        response.setOrganizationName(profile.getOrganizationName());
        response.setBillingAddress(profile.getBillingAddress());
        response.setState(profile.getState());
        response.setPincode(profile.getPincode());
        response.setGst(profile.getGst());
        response.setSubscription(profile.getSubscription());
        response.setProfilePicture(user.getProfilePicture()); // Get the profile picture URL from UserEntity
        response.setAlternateEmailId(profile.getAlternateEmailId());
        response.setAlternateMobileNumber(profile.getAlternateMobileNumber());
        
        log.info("Profile retrieved successfully for account ID: {}", accountId);

        return new ThedalResponse<>(ThedalSuccess.PROFILE_RETRIEVED, response);
    }



}
