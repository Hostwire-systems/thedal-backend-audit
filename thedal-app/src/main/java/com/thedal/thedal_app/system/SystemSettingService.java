package com.thedal.thedal_app.system;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.thedal.thedal_app.election.OtpService;
import com.thedal.thedal_app.general.RequestDetailsService;
import com.thedal.thedal_app.system.dto.SystemSettingOtpResponse;
import com.thedal.thedal_app.system.dto.VolunteerOtpSettingDto;
import com.thedal.thedal_app.system.dto.VolunteerOtpStatusDto;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;
import com.thedal.thedal_app.user.UserEntity;
import com.thedal.thedal_app.user.UserRepo;
import com.thedal.thedal_app.util.Response;
import com.thedal.thedal_app.volunteer.VolunteerEntity;
import com.thedal.thedal_app.volunteer.VolunteerRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SystemSettingService {
    
    public static final String VOLUNTEER_OTP_REQUIRED = "volunteer.otp.required";
    
    @Autowired
    private SystemSettingRepository systemSettingRepository;
    
    @Autowired
    private RequestDetailsService requestDetails;
    
    @Autowired
    private UserRepo userRepo;
    @Autowired
    private VolunteerRepository volunteerRepository;
    @Autowired
    private SystemSettingOtpAttemptRepository systemSettingOtpAttemptRepository;
    @Autowired
    private OtpService otpService;
    
    @Transactional
    public SystemSettingEntity updateVolunteerOtpSetting(VolunteerOtpSettingDto dto) {
        // Check if user is admin or super admin
        validateAdminAccess();
        
        String settingValue = dto.getSettingValue().toLowerCase();
        if (!settingValue.equals("enabled") && !settingValue.equals("disabled")) {
            throw new ThedalException(ThedalError.INVALID_INPUT, HttpStatus.BAD_REQUEST, 
                "Setting value must be 'enabled' or 'disabled'");
        }
        
        SystemSettingEntity setting = systemSettingRepository.findBySettingKey(VOLUNTEER_OTP_REQUIRED)
            .orElse(new SystemSettingEntity());
        
        boolean isNewSetting = setting.getId() == null;
        
        setting.setSettingKey(VOLUNTEER_OTP_REQUIRED);
        setting.setSettingValue(settingValue);
        setting.setDescription(dto.getDescription() != null ? dto.getDescription() : 
            "Controls whether volunteers require admin OTP approval for login");
        setting.setUpdatedAt(LocalDateTime.now());
        setting.setUpdatedBy("UserID: " + requestDetails.getCurrentUserId());
        
        if (isNewSetting) {
            setting.setCreatedAt(LocalDateTime.now());
        }
        
        SystemSettingEntity savedSetting = systemSettingRepository.save(setting);
        
        log.info("Volunteer OTP setting updated to: {} by user: {}", 
            settingValue, requestDetails.getCurrentUserId());
        
        return savedSetting;
    }
    
    public boolean isVolunteerOtpRequired() {
        try {
            SystemSettingEntity userAwareSetting = getUserAwareVolunteerOtpSetting();
            boolean isRequired = "enabled".equals(userAwareSetting.getSettingValue());
            log.info("User-aware OTP required check result: {}", isRequired);
            return isRequired;
        } catch (Exception e) {
            log.error("Error checking user-aware volunteer OTP setting, defaulting to enabled: {}", e.getMessage());
            return true; // Default to enabled for security
        }
    }
    
    public SystemSettingEntity getVolunteerOtpSetting() {
        return systemSettingRepository.findBySettingKey(VOLUNTEER_OTP_REQUIRED)
            .orElse(createDefaultSetting());
    }
    
    /**
     * Get user-aware volunteer OTP setting for a specific user (used during login when no auth context exists)
     * Logic: Prioritize global volunteer OTP system setting over admin preferences
     */
    public SystemSettingEntity getUserAwareVolunteerOtpSetting(Long userId) {
        log.info("Checking OTP requirement for user {}", userId);
        
        try {
            // First, check the global volunteer OTP system setting
            SystemSettingEntity globalSetting = getVolunteerOtpSetting();
            boolean globalOtpEnabled = "enabled".equals(globalSetting.getSettingValue());
            
            log.info("Global volunteer OTP setting: {}", globalSetting.getSettingValue());
            
            // If global volunteer OTP is disabled, return disabled for all volunteers
            if (!globalOtpEnabled) {
                log.info("Global volunteer OTP is disabled - no OTP required for volunteer {}", userId);
                return globalSetting;
            }
            
            // Global OTP is enabled, now check if user is a volunteer
            UserEntity user = userRepo.findById(userId).orElse(null);
            if (user == null) {
                log.warn("User {} not found, defaulting to global setting", userId);
                return globalSetting;
            }
            
            // Check if this user is a volunteer/cadre
            VolunteerEntity volunteer = volunteerRepository.findByUserEntityId(userId).orElse(null);
            if (volunteer == null) {
                log.info("User {} is not a volunteer, using global setting", userId);
                return globalSetting;
            }
            
            // User is a volunteer and global OTP is enabled
            // Check if admin is assigned (required for OTP flow)
            Long adminUserId = volunteer.getAdminUserId();
            if (adminUserId == null) {
                log.error("Volunteer {} has no admin assigned but global OTP is enabled. Cannot proceed with OTP login.", userId);
                // Return error setting that will be handled in AuthService
                SystemSettingEntity errorSetting = new SystemSettingEntity();
                errorSetting.setSettingKey(VOLUNTEER_OTP_REQUIRED);
                errorSetting.setSettingValue("error");
                errorSetting.setDescription("No admin assigned for volunteer - cannot send OTP");
                return errorSetting;
            }
            
            // Verify admin exists
            UserEntity admin = userRepo.findById(adminUserId).orElse(null);
            if (admin == null) {
                log.error("Admin {} not found for volunteer {} but global OTP is enabled", adminUserId, userId);
                // Return error setting that will be handled in AuthService
                SystemSettingEntity errorSetting = new SystemSettingEntity();
                errorSetting.setSettingKey(VOLUNTEER_OTP_REQUIRED);
                errorSetting.setSettingValue("error");
                errorSetting.setDescription("Admin not found for volunteer - cannot send OTP");
                return errorSetting;
            }
            
            // Global OTP is enabled and admin is available - require OTP
            log.info("Global volunteer OTP is enabled and admin {} is available for volunteer {} - OTP required", adminUserId, userId);
            return globalSetting;
            
        } catch (Exception e) {
            log.error("Error getting volunteer OTP setting for user {}, defaulting to enabled: {}", userId, e.getMessage());
            return createDefaultSetting();
        }
    }
    
    /**
     * Get user-aware volunteer OTP setting that considers both system-wide and user-specific settings.
     * Logic: 
     * - If system-wide OTP is disabled, return disabled for all users
     * - If system-wide OTP is enabled, check user's is_otp_required flag
     */
    public SystemSettingEntity getUserAwareVolunteerOtpSetting() {
        // Get system-wide setting first
        SystemSettingEntity systemSetting = getVolunteerOtpSetting();
        boolean systemOtpEnabled = "enabled".equals(systemSetting.getSettingValue());
        
        log.info("System-wide OTP setting: {}", systemSetting.getSettingValue());
        
        if (!systemOtpEnabled) {
            // If system OTP is disabled, return disabled for all users
            log.info("System OTP disabled - returning disabled for all users");
            return systemSetting;
        }
        
        // System OTP is enabled, check user-specific setting
        try {
            Long currentUserId = requestDetails.getCurrentUserId();
            log.info("Current user ID from request context: {}", currentUserId);
            
            if (currentUserId == null) {
                log.warn("Current user ID is null, defaulting to system setting");
                return systemSetting;
            }
            
            UserEntity user = userRepo.findById(currentUserId)
                .orElse(null);
            
            if (user == null) {
                log.warn("User not found for ID: {}, defaulting to system setting", currentUserId);
                return systemSetting;
            }
            
            Boolean userOtpRequired = user.getIsOtpRequired();
            log.info("User {} (mobile: {}) OTP required flag: {}", currentUserId, user.getMobileNumber(), userOtpRequired);
            
            if (userOtpRequired != null && !userOtpRequired) {
                // User has OTP disabled specifically
                SystemSettingEntity userSetting = new SystemSettingEntity();
                userSetting.setSettingKey(VOLUNTEER_OTP_REQUIRED);
                userSetting.setSettingValue("disabled");
                userSetting.setDescription("User-specific OTP requirement override");
                log.info("User {} has OTP disabled, returning disabled setting", currentUserId);
                return userSetting;
            }
            
            // User either has no override (null) or has OTP enabled (true), use system setting
            log.info("User {} uses system OTP setting: {}", currentUserId, systemSetting.getSettingValue());
            return systemSetting;
            
        } catch (Exception e) {
            log.error("Error getting user-specific OTP setting, falling back to system setting: {}", e.getMessage());
            return systemSetting;
        }
    }
    
    private SystemSettingEntity createDefaultSetting() {
        SystemSettingEntity defaultSetting = new SystemSettingEntity();
        defaultSetting.setSettingKey(VOLUNTEER_OTP_REQUIRED);
        defaultSetting.setSettingValue("enabled");
        defaultSetting.setDescription("Controls whether volunteers require admin OTP approval for login");
        return defaultSetting;
    }
    
    private void validateAdminAccess() {
        Long userId = requestDetails.getCurrentUserId();
        UserEntity user = userRepo.findById(userId)
            .orElseThrow(() -> new ThedalException(ThedalError.USER_NOT_FOUND, HttpStatus.NOT_FOUND));
        
        String roleName = user.getRole().getRoleName();
        if (!"SUPER_ADMIN".equalsIgnoreCase(roleName) && !"ADMIN".equalsIgnoreCase(roleName)) {
            throw new ThedalException(ThedalError.UNAUTHORIZED_ACCESS, HttpStatus.FORBIDDEN,
                "Only Admin and Super Admin users can modify volunteer OTP settings");
        }
    }
    
//    @Transactional
//    public Response<?> initiateUpdateVolunteerOtpSetting(VolunteerOtpSettingDto dto) {
//        log.debug("Initiating volunteer OTP setting update to: {}", dto.getSettingValue());
//
//        // Validate admin access
//        validateAdminAccess();
//
//        String settingValue = dto.getSettingValue().toLowerCase();
//        if (!settingValue.equals("enabled") && !settingValue.equals("disabled")) {
//            throw new ThedalException(ThedalError.INVALID_INPUT, HttpStatus.BAD_REQUEST, 
//                "Setting value must be 'enabled' or 'disabled'");
//        }
//
//        // Get user details
//        Long userId = requestDetails.getCurrentUserId();
//        UserEntity user = userRepo.findById(userId)
//                .orElseThrow(() -> new ThedalException(ThedalError.USER_NOT_FOUND, HttpStatus.NOT_FOUND));
//
//        // Validate mobile number
//        if (!Boolean.TRUE.equals(user.getIsMobileVerified()) || user.getMobileNumber() == null) {
//            throw new IllegalArgumentException("Mobile number must be verified to update system settings");
//        }
//
//        // Generate and send OTP
//        String otp = String.valueOf((int) ((Math.random() * 900000) + 100000)); // 6-digit OTP
//
//        SystemSettingOtpAttempt attempt = new SystemSettingOtpAttempt();
//        attempt.setUserId(userId);
//        attempt.setOtp(otp);
//        attempt.setIsActive(true);
//        attempt.setCreatedAt(LocalDateTime.now());
//        attempt.setExpiresAt(LocalDateTime.now().plusMinutes(5));
//        attempt.setSettingKey(VOLUNTEER_OTP_REQUIRED);
//        attempt.setNewSettingValue(settingValue);
//
//        // Save the OTP attempt
//        systemSettingOtpAttemptRepository.save(attempt);
//
//        // Send OTP to user's mobile
//        otpService.sendOtp(user.getMobileNumber(), otp);
//
//        // Prepare response
//        SystemSettingOtpResponse response = new SystemSettingOtpResponse();
//        response.setUserId(userId);
//        response.setMessage("OTP sent to registered mobile number");
//
//        Response<SystemSettingOtpResponse> otpResponse = new Response<>();
//        otpResponse.setMessage("OTP required for volunteer OTP setting update");
//        otpResponse.setSuccess(true);
//        otpResponse.setData(response);
//        return otpResponse;
//    }
    @Transactional
    public Response<SystemSettingOtpResponse> initiateUpdateVolunteerOtpSetting(VolunteerOtpSettingDto dto) {
        log.info("Initiating volunteer OTP setting update to: {}", dto.getSettingValue());

        // Validate admin access
        validateAdminAccess();

        // Validate setting value
        String settingValue = dto.getSettingValue().toLowerCase();
        if (!settingValue.equals("enabled") && !settingValue.equals("disabled")) {
            log.error("Invalid setting value: {}", settingValue);
            throw new ThedalException(ThedalError.INVALID_INPUT, HttpStatus.BAD_REQUEST, 
                "Setting value must be 'enabled' or 'disabled'");
        }

        // Get user details
        Long userId = requestDetails.getCurrentUserId();
        UserEntity user = userRepo.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found for userId={}", userId);
                    return new ThedalException(ThedalError.USER_NOT_FOUND, HttpStatus.NOT_FOUND);
                });

        // Validate mobile number
        if (!Boolean.TRUE.equals(user.getIsMobileVerified()) || user.getMobileNumber() == null) {
            log.error("Mobile not verified or missing for userId={}", userId);
            throw new IllegalArgumentException("Mobile number must be verified to update system settings");
        }

        // Check for existing attempt
        SystemSettingOtpAttempt attempt = systemSettingOtpAttemptRepository
                .findByUserIdAndSettingKey(userId, VOLUNTEER_OTP_REQUIRED)
                .orElse(new SystemSettingOtpAttempt());

        // Update or initialize attempt
        attempt.setUserId(userId);
        attempt.setSettingKey(VOLUNTEER_OTP_REQUIRED);
        attempt.setOtp(String.format("%06d", (int) (Math.random() * 900000 + 100000)));
        attempt.setIsActive(true);
        attempt.setCreatedAt(LocalDateTime.now());
        attempt.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        attempt.setNewSettingValue(settingValue);

        // Save the attempt (updates if exists, creates if new)
        systemSettingOtpAttemptRepository.save(attempt);
        log.info("Saved/Updated OTP attempt for userId={}, settingKey={}", userId, VOLUNTEER_OTP_REQUIRED);

        // Send OTP to user's mobile
        try {
            otpService.sendOtp(user.getMobileNumber(), attempt.getOtp());
            log.info("OTP sent to mobile {} for userId={}", user.getMobileNumber(), userId);
        } catch (Exception e) {
            log.error("Failed to send OTP for userId={}: {}", userId, e.getMessage());
            throw new RuntimeException("Failed to send OTP. Please try again.");
        }

        // Prepare response
        SystemSettingOtpResponse responseData = new SystemSettingOtpResponse();
        responseData.setUserId(userId);
        responseData.setMessage("OTP sent to registered mobile number");

        Response<SystemSettingOtpResponse> response = new Response<>();
        response.setMessage("OTP required for volunteer OTP setting update");
        response.setSuccess(true);
        response.setData(responseData);
        return response;
    }

//    @Transactional
//    public SystemSettingEntity verifyVolunteerOtpSettingOtp(Long userId, String otp) {
//        log.debug("Verifying OTP for volunteer OTP setting update: userId={}", userId);
//
//        // Validate admin access
//        validateAdminAccess();
//
//        // Find active OTP attempt
//        SystemSettingOtpAttempt attempt = systemSettingOtpAttemptRepository
//                .findByUserIdAndSettingKeyAndIsActiveTrue(userId, VOLUNTEER_OTP_REQUIRED)
//                .orElseThrow(() -> new IllegalArgumentException("No active OTP request found for volunteer OTP setting"));
//
//        // Check OTP expiration
//        if (LocalDateTime.now().isAfter(attempt.getExpiresAt())) {
//            throw new IllegalArgumentException("OTP has expired");
//        }
//
//        // Verify OTP
//        if (!attempt.getOtp().equals(otp)) {
//            throw new IllegalArgumentException("Invalid OTP");
//        }
//
//        // Mark OTP attempt as inactive
//        attempt.setIsActive(false);
//        systemSettingOtpAttemptRepository.save(attempt);
//
//        // Update the setting
//        SystemSettingEntity setting = systemSettingRepository.findBySettingKey(VOLUNTEER_OTP_REQUIRED)
//                .orElse(new SystemSettingEntity());
//        
//        boolean isNewSetting = setting.getId() == null;
//        
//        setting.setSettingKey(VOLUNTEER_OTP_REQUIRED);
//        setting.setSettingValue(attempt.getNewSettingValue());
//        setting.setDescription("Controls whether volunteers require admin OTP approval for login");
//        setting.setUpdatedAt(LocalDateTime.now());
//        setting.setUpdatedBy("UserID: " + userId);
//        
//        if (isNewSetting) {
//            setting.setCreatedAt(LocalDateTime.now());
//        }
//
//    SystemSettingEntity savedSetting = systemSettingRepository.save(setting);
//        
//        log.info("Volunteer OTP setting updated to: {} by user: {}", attempt.getNewSettingValue(), userId);
//        
//        return savedSetting;
//    }
    @Transactional
    public SystemSettingEntity verifyVolunteerOtpSettingOtp(Long userId, String otp) {
        log.info("Verifying OTP for volunteer OTP setting update: userId={}", userId);

        // Validate admin access
        validateAdminAccess();

        // Fetch the attempt
        SystemSettingOtpAttempt attempt = systemSettingOtpAttemptRepository
                .findByUserIdAndSettingKey(userId, VOLUNTEER_OTP_REQUIRED)
                .orElseThrow(() -> {
                    log.error("No OTP request found for userId={}, settingKey={}", userId, VOLUNTEER_OTP_REQUIRED);
                    return new IllegalArgumentException("No active OTP request found for volunteer OTP setting");
                });

        // Check if attempt is active
        if (!attempt.getIsActive()) {
            log.error("No active OTP request found for userId={}, settingKey={}", userId, VOLUNTEER_OTP_REQUIRED);
            throw new IllegalArgumentException("No active OTP request found for volunteer OTP setting");
        }

        // Check if OTP is expired
        if (LocalDateTime.now().isAfter(attempt.getExpiresAt())) {
            attempt.setIsActive(false);
            systemSettingOtpAttemptRepository.save(attempt);
            log.error("OTP expired for userId={}, settingKey={}", userId, VOLUNTEER_OTP_REQUIRED);
            throw new IllegalArgumentException("OTP has expired");
        }

        // Verify OTP
        if (!attempt.getOtp().equals(otp)) {
            log.error("Invalid OTP provided for userId={}. Provided: {}, Expected: {}", userId, otp, attempt.getOtp());
            throw new IllegalArgumentException("Invalid OTP");
        }

        // Mark OTP attempt as inactive
        attempt.setIsActive(false);
        systemSettingOtpAttemptRepository.save(attempt);
        log.info("Deactivated OTP attempt for userId={}, settingKey={}", userId, VOLUNTEER_OTP_REQUIRED);

        // Toggle the admin's personal OTP preference (the revolutionary feature!)
        UserEntity admin = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Admin user not found: " + userId));
        
        // Handle null values - default to false if null
        Boolean currentOtpRequiredObj = admin.getIsOtpRequired();
        boolean currentOtpRequired = currentOtpRequiredObj != null ? currentOtpRequiredObj : false;
        boolean newOtpRequired = !currentOtpRequired; // Toggle the setting
        admin.setIsOtpRequired(newOtpRequired);
        userRepo.save(admin);
        
        log.info("🔄 Toggled admin OTP preference for userId={}: {} → {} (was null: {})", 
                userId, currentOtpRequired, newOtpRequired, currentOtpRequiredObj == null);

        // Update the system setting (for backwards compatibility)
        SystemSettingEntity setting = systemSettingRepository.findBySettingKey(VOLUNTEER_OTP_REQUIRED)
                .orElse(new SystemSettingEntity());

        boolean isNewSetting = setting.getId() == null;

        setting.setSettingKey(VOLUNTEER_OTP_REQUIRED);
        setting.setSettingValue(newOtpRequired ? "enabled" : "disabled"); // Use the toggled value
        setting.setDescription("Controls whether volunteers require admin OTP approval for login");
        setting.setUpdatedAt(LocalDateTime.now());
        setting.setUpdatedBy("UserID: " + userId);

        if (isNewSetting) {
            setting.setCreatedAt(LocalDateTime.now());
        }

        SystemSettingEntity savedSetting = systemSettingRepository.save(setting);
        log.info("Volunteer OTP setting updated to: {} by userId={} (Admin OTP: {})", 
                savedSetting.getSettingValue(), userId, newOtpRequired);

        return savedSetting;
    }
}
