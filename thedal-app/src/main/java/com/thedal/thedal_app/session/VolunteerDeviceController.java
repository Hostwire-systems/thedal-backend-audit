package com.thedal.thedal_app.session;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.thedal.thedal_app.general.RequestDetailsService;
import com.thedal.thedal_app.session.dto.DeviceSessionDTO;
import com.thedal.thedal_app.util.Response;
import com.thedal.thedal_app.volunteer.VolunteerEntity;
import com.thedal.thedal_app.volunteer.VolunteerRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/volunteers")
public class VolunteerDeviceController {

    @Autowired
    private UserSessionService sessionService;
    
    @Autowired
    private RequestDetailsService requestDetails;
    
    @Autowired
    private VolunteerRepository volunteerRepository;

    /**
     * Get active device sessions for a specific volunteer
     * GET /api/volunteers/{volunteerId}/devices
     */
    @GetMapping("/{volunteerId}/devices")
    public ResponseEntity<Response<List<DeviceSessionDTO>>> getVolunteerDevices(@PathVariable Long volunteerId) {
        log.info("Fetching active devices for volunteer: {}", volunteerId);
        
        try {
            // Verify the volunteer exists and user has permission
            Long currentUserId = requestDetails.getCurrentUserId();
            Long currentAccountId = requestDetails.getCurrentAccountId();
            
            VolunteerEntity volunteer = volunteerRepository.findById(volunteerId).orElse(null);
            if (volunteer == null) {
                log.warn("Volunteer not found with ID: {}", volunteerId);
                Response<List<DeviceSessionDTO>> response = new Response<>();
                response.setSuccess(false);
                response.setMessage("Volunteer not found");
                response.setData(null);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
            // Check if user has permission (same account or volunteer themselves)
            if (!volunteer.getAccountId().equals(currentAccountId) && 
                !volunteer.getUserEntity().getId().equals(currentUserId)) {
                log.warn("User {} does not have permission to view devices for volunteer {}", 
                        currentUserId, volunteerId);
                Response<List<DeviceSessionDTO>> response = new Response<>();
                response.setSuccess(false);
                response.setMessage("Access denied");
                response.setData(null);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
            
            // Get active sessions for the volunteer's user ID
            Long volunteerUserId = volunteer.getUserEntity().getId();
            List<DeviceSessionDTO> activeDevices = sessionService.getActiveSessionsForUser(volunteerUserId);
            
            log.info("Found {} active devices for volunteer: {}", activeDevices.size(), volunteerId);
            
            Response<List<DeviceSessionDTO>> response = new Response<>();
            response.setSuccess(true);
            response.setMessage("Success");
            response.setData(activeDevices);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error fetching devices for volunteer {}: {}", volunteerId, e.getMessage(), e);
            Response<List<DeviceSessionDTO>> response = new Response<>();
            response.setSuccess(false);
            response.setMessage("Failed to fetch devices");
            response.setData(null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Get active device count for a specific volunteer
     * GET /api/volunteers/{volunteerId}/device-count
     */
    @GetMapping("/{volunteerId}/device-count")
    public ResponseEntity<Response<Integer>> getVolunteerDeviceCount(@PathVariable Long volunteerId) {
        log.info("Fetching active device count for volunteer: {}", volunteerId);
        
        try {
            Long currentUserId = requestDetails.getCurrentUserId();
            Long currentAccountId = requestDetails.getCurrentAccountId();
            
            VolunteerEntity volunteer = volunteerRepository.findById(volunteerId).orElse(null);
            if (volunteer == null) {
                log.warn("Volunteer not found with ID: {}", volunteerId);
                Response<Integer> response = new Response<>();
                response.setSuccess(false);
                response.setMessage("Volunteer not found");
                response.setData(null);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
            // Check permissions
            if (!volunteer.getAccountId().equals(currentAccountId) && 
                !volunteer.getUserEntity().getId().equals(currentUserId)) {
                log.warn("User {} does not have permission to view device count for volunteer {}", 
                        currentUserId, volunteerId);
                Response<Integer> response = new Response<>();
                response.setSuccess(false);
                response.setMessage("Access denied");
                response.setData(null);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
            
            Long volunteerUserId = volunteer.getUserEntity().getId();
            int deviceCount = sessionService.getActiveDeviceCount(volunteerUserId);
            
            log.info("Volunteer {} has {} active devices", volunteerId, deviceCount);
            
            Response<Integer> response = new Response<>();
            response.setSuccess(true);
            response.setMessage("Success");
            response.setData(deviceCount);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error fetching device count for volunteer {}: {}", volunteerId, e.getMessage(), e);
            Response<Integer> response = new Response<>();
            response.setSuccess(false);
            response.setMessage("Failed to fetch device count");
            response.setData(null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Logout from all devices for a volunteer (admin action)
     * DELETE /api/volunteers/{volunteerId}/devices
     */
    @DeleteMapping("/{volunteerId}/devices")
    public ResponseEntity<Response<String>> logoutAllDevices(@PathVariable Long volunteerId) {
        log.info("Logging out all devices for volunteer: {}", volunteerId);
        
        try {
            Long currentUserId = requestDetails.getCurrentUserId();
            Long currentAccountId = requestDetails.getCurrentAccountId();
            
            VolunteerEntity volunteer = volunteerRepository.findById(volunteerId).orElse(null);
            if (volunteer == null) {
                log.warn("Volunteer not found with ID: {}", volunteerId);
                Response<String> response = new Response<>();
                response.setSuccess(false);
                response.setMessage("Volunteer not found");
                response.setData(null);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
            // Check permissions (only admin or the volunteer themselves)
            if (!volunteer.getAccountId().equals(currentAccountId) && 
                !volunteer.getUserEntity().getId().equals(currentUserId)) {
                log.warn("User {} does not have permission to logout devices for volunteer {}", 
                        currentUserId, volunteerId);
                Response<String> response = new Response<>();
                response.setSuccess(false);
                response.setMessage("Access denied");
                response.setData(null);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
            
            Long volunteerUserId = volunteer.getUserEntity().getId();
            sessionService.invalidateAllUserSessions(volunteerUserId);
            
            log.info("All devices logged out for volunteer: {}", volunteerId);
            
            Response<String> response = new Response<>();
            response.setSuccess(true);
            response.setMessage("All devices logged out successfully");
            response.setData(null);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error logging out devices for volunteer {}: {}", volunteerId, e.getMessage(), e);
            Response<String> response = new Response<>();
            response.setSuccess(false);
            response.setMessage("Failed to logout devices");
            response.setData(null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}