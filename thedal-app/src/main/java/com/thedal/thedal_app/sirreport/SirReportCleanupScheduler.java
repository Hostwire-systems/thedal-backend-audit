package com.thedal.thedal_app.sirreport;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Scheduled cleanup for SIR report temporary data
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SirReportCleanupScheduler {
    
    private final SirTempBaseVoterRepository tempBaseVoterRepository;
    private final SirTempNewVoterRepository tempNewVoterRepository;
    private final SirReportExportJobRepository exportJobRepository;
    
    /**
     * Clean up temp base voter data older than 7 days
     * Runs daily at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupOldTempData() {
        log.info("Starting cleanup of old SIR temp data");
        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(7);
            int deletedBase = tempBaseVoterRepository.deleteOldRecords(cutoffDate);
            int deletedNew = tempNewVoterRepository.deleteOldRecords(cutoffDate);
            log.info("Cleaned up {} base and {} new temp voter records", deletedBase, deletedNew);
        } catch (Exception e) {
            log.error("Error cleaning up temp data", e);
        }
    }
    
    /**
     * Clean up expired export jobs (older than 24 hours past expiration)
     * Runs every 6 hours
     */
    @Scheduled(fixedRate = 21600000) // 6 hours in milliseconds
    @Transactional
    public void cleanupExpiredExports() {
        log.info("Starting cleanup of expired SIR export jobs");
        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusHours(24);
            var expiredJobs = exportJobRepository.findByExpiresAtBefore(cutoffDate);
            if (!expiredJobs.isEmpty()) {
                exportJobRepository.deleteAll(expiredJobs);
                log.info("Cleaned up {} expired export jobs", expiredJobs.size());
            }
        } catch (Exception e) {
            log.error("Error cleaning up expired exports", e);
        }
    }
}
