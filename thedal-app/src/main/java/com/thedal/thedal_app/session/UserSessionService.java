package com.thedal.thedal_app.session;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.thedal.thedal_app.session.dto.DeviceInfo;
import com.thedal.thedal_app.session.dto.DeviceSessionDTO;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class UserSessionService {

    @Autowired
    private UserSessionRepository sessionRepository;

    /**
     * Create a new session for a user
     */
    @Transactional
    public UserSessionEntity createSession(Long userId, String jwtToken, DeviceInfo deviceInfo, LocalDateTime expiresAt) {
        log.info("Creating new session for user: {}", userId);
        
        try {
            String tokenHash = hashToken(jwtToken);
            
            UserSessionEntity session = new UserSessionEntity();
            session.setUserId(userId);
            session.setSessionTokenHash(tokenHash);
            session.setIpAddress(deviceInfo.getIpAddress());
            session.setUserAgent(deviceInfo.getUserAgent());
            session.setDeviceType(deviceInfo.getDeviceType());
            session.setBrowserName(deviceInfo.getBrowserName());
            session.setOperatingSystem(deviceInfo.getOperatingSystem());
            session.setLocationCountry(deviceInfo.getLocationCountry());
            session.setLocationCity(deviceInfo.getLocationCity());
            session.setLocationRegion(deviceInfo.getLocationRegion());
            session.setExpiresAt(expiresAt);
            
            UserSessionEntity savedSession = sessionRepository.save(session);
            log.info("Session created successfully with ID: {}", savedSession.getId());
            
            return savedSession;
        } catch (Exception e) {
            log.error("Error creating session for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to create session", e);
        }
    }

    /**
     * Get active sessions for a user as DTOs
     */
    public List<DeviceSessionDTO> getActiveSessionsForUser(Long userId) {
        log.info("Fetching active sessions for user: {}", userId);
        
        List<UserSessionEntity> sessions = sessionRepository.findActiveSessionsByUserId(userId);
        
        return sessions.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    /**
     * Get active device count for a user
     */
    public int getActiveDeviceCount(Long userId) {
        int count = sessionRepository.countActiveSessionsByUserId(userId);
        log.debug("Active device count for user {}: {}", userId, count);
        return count;
    }

    /**
     * Update last access time for a session
     */
    @Transactional
    public void updateSessionAccess(String jwtToken) {
        try {
            String tokenHash = hashToken(jwtToken);
            LocalDateTime now = LocalDateTime.now();
            
            int updated = sessionRepository.updateLastAccessTime(tokenHash, now, now);
            if (updated > 0) {
                log.debug("Updated last access time for session");
            }
        } catch (Exception e) {
            log.warn("Failed to update session access time: {}", e.getMessage());
        }
    }

    /**
     * Invalidate a specific session
     */
    @Transactional
    public void invalidateSession(String jwtToken) {
        try {
            String tokenHash = hashToken(jwtToken);
            LocalDateTime now = LocalDateTime.now();
            
            int updated = sessionRepository.deactivateSessionByTokenHash(tokenHash, now);
            if (updated > 0) {
                log.info("Session invalidated successfully");
            }
        } catch (Exception e) {
            log.error("Failed to invalidate session: {}", e.getMessage(), e);
        }
    }

    /**
     * Invalidate all sessions for a user (logout from all devices)
     */
    @Transactional
    public void invalidateAllUserSessions(Long userId) {
        log.info("Invalidating all sessions for user: {}", userId);
        
        LocalDateTime now = LocalDateTime.now();
        int updated = sessionRepository.deactivateAllUserSessions(userId, now);
        
        log.info("Invalidated {} sessions for user: {}", updated, userId);
    }

    /**
     * Clean up expired sessions
     */
    @Transactional
    public void cleanupExpiredSessions() {
        log.info("Starting cleanup of expired sessions");
        
        LocalDateTime now = LocalDateTime.now();
        int updated = sessionRepository.deactivateExpiredSessions(now, now);
        
        log.info("Deactivated {} expired sessions", updated);
        
        // Delete old inactive sessions (older than 30 days)
        LocalDateTime cutoffDate = now.minusDays(30);
        int deleted = sessionRepository.deleteOldInactiveSessions(cutoffDate);
        
        log.info("Deleted {} old inactive sessions", deleted);
    }

    /**
     * Check if a session exists and is valid
     */
    public boolean isSessionValid(String jwtToken) {
        try {
            String tokenHash = hashToken(jwtToken);
            return sessionRepository.findBySessionTokenHashAndIsActive(tokenHash, true).isPresent();
        } catch (Exception e) {
            log.warn("Error checking session validity: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get session info by JWT token
     */
    public UserSessionEntity getSessionByToken(String jwtToken) {
        try {
            String tokenHash = hashToken(jwtToken);
            return sessionRepository.findBySessionTokenHashAndIsActive(tokenHash, true).orElse(null);
        } catch (Exception e) {
            log.warn("Error getting session by token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Convert entity to DTO
     */
    private DeviceSessionDTO convertToDTO(UserSessionEntity session) {
        return new DeviceSessionDTO(
            session.getId(),
            session.getDeviceType(),
            session.getBrowserName(),
            session.getOperatingSystem(),
            session.getIpAddress(),
            session.getLocationCountry(),
            session.getLocationCity(),
            session.getLoginTime(),
            session.getLastAccessTime()
        );
    }

    /**
     * Hash JWT token for secure storage
     */
    private String hashToken(String token) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(token.getBytes());
        
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        
        return hexString.toString();
    }
}