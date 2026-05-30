package com.thedal.thedal_app.session;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.thedal.thedal_app.JwtService;
import com.thedal.thedal_app.session.dto.DeviceInfo;
import com.thedal.thedal_app.user.UserEntity;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SessionIntegrationService {

    @Autowired
    private UserSessionService sessionService;
    
    @Autowired
    private DeviceInfoExtractor deviceInfoExtractor;
    
    @Autowired
    private JwtService jwtService;

    @Value("${jwt.access-token.expiration}")
    private long accessTokenExpireDays;

    /**
     * Enhanced token generation with session tracking
     * This method can be called from AuthService after successful login
     */
    public String generateTokenWithSession(UserEntity user, HttpServletRequest request) {
        log.info("Generating token with session tracking for user: {}", user.getId());
        
        try {
            // Extract device information from request
            DeviceInfo deviceInfo = deviceInfoExtractor.extractFromRequest(request);
            
            // Generate unique session ID for this token
            String sessionId = UUID.randomUUID().toString();
            
            // Generate JWT token with session ID
            String jwtToken = jwtService.generateAccessToken(user, deviceInfo.getDeviceType(), sessionId);
            
            // Calculate expiration time
            LocalDateTime expiresAt = LocalDateTime.now().plusDays(accessTokenExpireDays);
            
            // Create session record
            sessionService.createSession(user.getId(), jwtToken, deviceInfo, expiresAt);
            
            log.info("Successfully created session for user: {} with device type: {}", 
                    user.getId(), deviceInfo.getDeviceType());
            
            return jwtToken;
            
        } catch (Exception e) {
            log.error("Error creating session for user {}: {}", user.getId(), e.getMessage(), e);
            // Fallback to regular token generation if session creation fails
            return jwtService.generateAccessToken(user);
        }
    }

    /**
     * Update session access time (called from security filter on each request)
     */
    public void updateSessionAccess(String jwtToken) {
        if (jwtToken != null && !jwtToken.isEmpty()) {
            // Remove "Bearer " prefix if present
            if (jwtToken.startsWith("Bearer ")) {
                jwtToken = jwtToken.substring(7);
            }
            sessionService.updateSessionAccess(jwtToken);
        }
    }

    /**
     * Invalidate session on logout
     */
    public void invalidateSession(String jwtToken) {
        if (jwtToken != null && !jwtToken.isEmpty()) {
            // Remove "Bearer " prefix if present
            if (jwtToken.startsWith("Bearer ")) {
                jwtToken = jwtToken.substring(7);
            }
            sessionService.invalidateSession(jwtToken);
            log.info("Session invalidated for logout");
        }
    }

    /**
     * Check if session is valid (optional security enhancement)
     */
    public boolean isSessionValid(String jwtToken) {
        if (jwtToken != null && !jwtToken.isEmpty()) {
            // Remove "Bearer " prefix if present
            if (jwtToken.startsWith("Bearer ")) {
                jwtToken = jwtToken.substring(7);
            }
            return sessionService.isSessionValid(jwtToken);
        }
        return false;
    }

    /**
     * Clean up expired sessions (scheduled task)
     */
    public void cleanupExpiredSessions() {
        log.info("Running scheduled cleanup of expired sessions");
        sessionService.cleanupExpiredSessions();
    }
}