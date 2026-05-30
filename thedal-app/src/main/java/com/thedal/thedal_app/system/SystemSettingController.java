package com.thedal.thedal_app.system;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.thedal.thedal_app.system.dto.VolunteerOtpSettingDto;
import com.thedal.thedal_app.system.dto.VolunteerOtpStatusDto;
import com.thedal.thedal_app.system.dto.VolunteerOtpToggleDto;
import com.thedal.thedal_app.util.Response;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/system/settings")
@Tag(name = "System Settings", description = "System configuration management")
public class SystemSettingController {
    
    @Autowired
    private SystemSettingService systemSettingService;
    
    @Operation(summary = "Get Volunteer OTP Setting", 
               description = "Get the current volunteer OTP requirement setting for the authenticated user. Considers both system-wide and user-specific settings.")
    @GetMapping("/volunteer-otp")
    public ResponseEntity<Response<SystemSettingEntity>> getVolunteerOtpSetting() {
        Response<SystemSettingEntity> response = new Response<>();
        
        try {
            SystemSettingEntity setting = systemSettingService.getUserAwareVolunteerOtpSetting();
            response.setSuccess(true);
            response.setMessage("User-aware volunteer OTP setting retrieved successfully");
            response.setData(setting);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting user-aware volunteer OTP setting: {}", e.getMessage());
            response.setSuccess(false);
            response.setMessage("Failed to retrieve volunteer OTP setting: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @Operation(summary = "Update Volunteer OTP Setting", 
               description = "Enable or disable volunteer OTP requirement. Only Admin and Super Admin can modify.")
    @PutMapping("/volunteer-otp")
    public ResponseEntity<Response<SystemSettingEntity>> updateVolunteerOtpSetting(
            @Valid @RequestBody VolunteerOtpToggleDto dto) {
        Response<SystemSettingEntity> response = new Response<>();
        
        try {
            // Convert boolean to string format expected by service
            VolunteerOtpSettingDto settingDto = new VolunteerOtpSettingDto();
            settingDto.setSettingValue(dto.getEnabled() ? "enabled" : "disabled");
            settingDto.setDescription("Controls whether volunteer login requires OTP verification");
            
            SystemSettingEntity updatedSetting = systemSettingService.updateVolunteerOtpSetting(settingDto);
            response.setSuccess(true);
            response.setMessage("Volunteer OTP setting updated successfully");
            response.setData(updatedSetting);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating volunteer OTP setting: {}", e.getMessage());
            response.setSuccess(false);
            response.setMessage("Failed to update volunteer OTP setting: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
//    @Operation(summary = "Get Volunteer OTP Status", 
//               description = "Get a simple boolean indicating if volunteer OTP is required")
//    @GetMapping("/volunteer-otp/status")
//    public ResponseEntity<Response<Boolean>> getVolunteerOtpStatus() {
//        Response<Boolean> response = new Response<>();
//        
//        try {
//            boolean isRequired = systemSettingService.isVolunteerOtpRequired();
//            response.setSuccess(true);
//            response.setMessage("Volunteer OTP status retrieved successfully");
//            response.setData(isRequired);
//            
//            return ResponseEntity.ok(response);
//        } catch (Exception e) {
//            log.error("Error getting volunteer OTP status: {}", e.getMessage());
//            response.setSuccess(false);
//            response.setMessage("Failed to retrieve volunteer OTP status: " + e.getMessage());
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
//        }
//    }
    @Operation(summary = "Get Volunteer OTP Requirement Status", 
            description = "Returns whether OTP verification is required for volunteer logins based on the system setting.")
 @GetMapping("/volunteer-otp/status")
 public ResponseEntity<Response<Boolean>> getVolunteerOtpStatus() {
     Response<Boolean> response = new Response<>();
     
     try {
         boolean isRequired = systemSettingService.isVolunteerOtpRequired();
         log.info("Retrieved volunteer OTP requirement status: {}", isRequired);
         response.setSuccess(true);
         response.setMessage("Volunteer OTP requirement status retrieved successfully");
         response.setData(isRequired);
         
         return ResponseEntity.ok(response);
     } catch (Exception e) {
         log.error("Error retrieving volunteer OTP requirement status: {}", e.getMessage());
         response.setSuccess(false);
         response.setMessage("Failed to retrieve volunteer OTP requirement status: " + e.getMessage());
         return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
     }
 }
    
    @Operation(summary = "Initiate Update Volunteer OTP Setting", 
            description = "Initiate enabling or disabling volunteer OTP requirement with OTP verification. Only Admin and Super Admin can modify.")
 @PutMapping("/volunteer-otp/user")
 public ResponseEntity<Response<?>> initiateUpdateVolunteerOtpSetting(
         @Valid @RequestBody VolunteerOtpToggleDto dto) {
     Response<?> response = new Response<>();
     
     try {
         // Convert boolean to string format expected by service
         VolunteerOtpSettingDto settingDto = new VolunteerOtpSettingDto();
         settingDto.setSettingValue(dto.getEnabled() ? "enabled" : "disabled");
         settingDto.setDescription("Controls whether volunteer login requires OTP verification");
         
         // Initiate OTP process
         Response<?> otpResponse = systemSettingService.initiateUpdateVolunteerOtpSetting(settingDto);
         return ResponseEntity.ok(otpResponse);
     } catch (Exception e) {
         log.error("Error initiating volunteer OTP setting update: {}", e.getMessage());
         response.setSuccess(false);
         response.setMessage("Failed to initiate volunteer OTP setting update: " + e.getMessage());
         return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
     }
 }

 @Operation(summary = "Verify OTP for Volunteer OTP Setting Update", 
            description = "Verify OTP to complete volunteer OTP setting update")
 @PostMapping("/volunteer-otp/verify-otp")
 public ResponseEntity<Response<SystemSettingEntity>> verifyVolunteerOtpSettingOtp(
         @RequestParam Long userId,
         @RequestParam String otp) {
     Response<SystemSettingEntity> response = new Response<>();
     
     try {
         SystemSettingEntity updatedSetting = systemSettingService.verifyVolunteerOtpSettingOtp(userId, otp);
         response.setSuccess(true);
         response.setMessage("Volunteer OTP setting updated successfully");
         response.setData(updatedSetting);
         return ResponseEntity.ok(response);
     } catch (IllegalArgumentException e) {
         response.setSuccess(false);
         response.setMessage(e.getMessage());
         return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
     } catch (Exception e) {
         log.error("Error verifying OTP for volunteer OTP setting: {}", e.getMessage());
         response.setSuccess(false);
         response.setMessage("Failed to verify OTP: " + e.getMessage());
         return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
     }
 }
    
    
}
