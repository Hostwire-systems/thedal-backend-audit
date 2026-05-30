package com.thedal.thedal_app.session;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.thedal.thedal_app.session.dto.DeviceSessionDTO;
import com.thedal.thedal_app.volunteer.dto.VolunteerDetailsDTO;
import com.thedal.thedal_app.volunteer.dto.VolunteerDTO;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class VolunteerSessionEnhancementService {

    @Autowired
    private UserSessionService sessionService;

    /**
     * Enhance VolunteerDetailsDTO with device count if requested
     */
    public void enrichVolunteerWithDeviceCount(VolunteerDetailsDTO volunteer, boolean includeDeviceCount) {
        if (includeDeviceCount && volunteer.getUserId() != null) {
            try {
                int deviceCount = sessionService.getActiveDeviceCount(volunteer.getUserId());
                volunteer.setActiveDeviceCount(deviceCount);
                log.debug("Enhanced volunteer {} with device count: {}", volunteer.getVolunteerId(), deviceCount);
            } catch (Exception e) {
                log.warn("Failed to get device count for volunteer {}: {}", volunteer.getVolunteerId(), e.getMessage());
                volunteer.setActiveDeviceCount(0);
            }
        }
    }

    /**
     * Enhance VolunteerDTO with device count if requested
     */
    public void enrichVolunteerWithDeviceCount(VolunteerDTO volunteer, Long userId, boolean includeDeviceCount) {
        if (includeDeviceCount && userId != null) {
            try {
                int deviceCount = sessionService.getActiveDeviceCount(userId);
                volunteer.setActiveDeviceCount(deviceCount);
                log.debug("Enhanced volunteer with device count: {}", deviceCount);
            } catch (Exception e) {
                log.warn("Failed to get device count for volunteer: {}", e.getMessage());
                volunteer.setActiveDeviceCount(0);
            }
        }
    }

    /**
     * Enhance list of volunteers with device counts if requested
     */
    public void enrichVolunteersWithDeviceCount(List<VolunteerDetailsDTO> volunteers, boolean includeDeviceCount) {
        if (includeDeviceCount) {
            volunteers.forEach(volunteer -> enrichVolunteerWithDeviceCount(volunteer, true));
        }
    }

    /**
     * Get detailed device sessions for a volunteer
     */
    public List<DeviceSessionDTO> getVolunteerDeviceSessions(Long userId) {
        try {
            return sessionService.getActiveSessionsForUser(userId);
        } catch (Exception e) {
            log.error("Failed to get device sessions for user {}: {}", userId, e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Get device count for a volunteer
     */
    public int getVolunteerDeviceCount(Long userId) {
        try {
            return sessionService.getActiveDeviceCount(userId);
        } catch (Exception e) {
            log.warn("Failed to get device count for user {}: {}", userId, e.getMessage());
            return 0;
        }
    }
}