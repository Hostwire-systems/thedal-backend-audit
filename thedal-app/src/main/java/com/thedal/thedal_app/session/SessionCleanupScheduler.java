package com.thedal.thedal_app.session;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class SessionCleanupScheduler {

    @Autowired
    private SessionIntegrationService sessionIntegrationService;

    /**
     * Clean up expired sessions every hour
     */
    @Scheduled(fixedRate = 3600000) // 1 hour = 3,600,000 milliseconds
    public void cleanupExpiredSessions() {
        try {
            log.info("Starting scheduled session cleanup");
            sessionIntegrationService.cleanupExpiredSessions();
            log.info("Completed scheduled session cleanup");
        } catch (Exception e) {
            log.error("Error during scheduled session cleanup: {}", e.getMessage(), e);
        }
    }

    /**
     * Deep cleanup of old inactive sessions every day at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * ?") // Every day at 2:00 AM
    public void deepCleanupSessions() {
        try {
            log.info("Starting deep session cleanup");
            sessionIntegrationService.cleanupExpiredSessions();
            log.info("Completed deep session cleanup");
        } catch (Exception e) {
            log.error("Error during deep session cleanup: {}", e.getMessage(), e);
        }
    }
}