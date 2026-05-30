package com.thedal.thedal_app.voter;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class FamilyMappingSyncScheduler {

    @Autowired
    private VoterRepo voterRepository;
    
    @Autowired
    private VoterMongoRepository voterMongoRepository;
    
    @Value("${family.mapping.sync.enabled:true}")
    private boolean syncEnabled;
    
    @Value("${family.mapping.sync.batch.size:200}")
    private int batchSize;
    
    @Value("${family.mapping.sync.hours.lookback:24}")
    private int hoursLookback;
    
    /**
     * Scheduled job that runs every 2 hours to sync recent family mapping changes
     */
    @Scheduled(cron = "0 0 */2 * * ?") // Every 2 hours
    @Transactional
    public void syncRecentFamilyMappingChanges() {
        if (!syncEnabled) {
            log.debug("Family mapping sync is disabled");
            return;
        }
        
        log.info("Starting scheduled family mapping sync for changes in last {} hours", hoursLookback);
        
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(hoursLookback);
        AtomicInteger totalProcessed = new AtomicInteger(0);
        AtomicInteger familyMappingsSynced = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        
        try {
            int page = 0;
            boolean hasMoreData = true;

            while (hasMoreData) {
                Pageable pageable = PageRequest.of(page, batchSize);
                Page<VoterEntity> voterPage = voterRepository.findRecentlyModifiedVotersWithFamilyMappings(cutoffTime, pageable);

                if (voterPage.isEmpty()) {
                    hasMoreData = false;
                    break;
                }

                for (VoterEntity voter : voterPage.getContent()) {
                    totalProcessed.incrementAndGet();
                    
                    if (voter.getFamilyId() != null) {
                        try {
                            // Sync to MongoDB
                            VoterMongo voterMongo = new VoterMongo(voter);
                            voterMongoRepository.saveVoterMongoWithNullFields(voterMongo);
                            familyMappingsSynced.incrementAndGet();
                            
                        } catch (Exception ex) {
                            errorCount.incrementAndGet();
                            log.error("Failed to sync voter family mapping: epicNumber={}, familyId={}, error={}", 
                                     voter.getEpicNumber(), voter.getFamilyId(), ex.getMessage());
                        }
                    }
                }

                page++;
            }
            
            if (familyMappingsSynced.get() > 0 || errorCount.get() > 0) {
                log.info("Family mapping sync completed. Processed: {}, Synced: {}, Errors: {}", 
                        totalProcessed.get(), familyMappingsSynced.get(), errorCount.get());
            } else {
                log.debug("No family mapping changes to sync");
            }
            
        } catch (Exception e) {
            log.error("Scheduled family mapping sync failed: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Scheduled job that runs daily to perform consistency checks
     */
    @Scheduled(cron = "0 0 3 * * ?") // Daily at 3 AM
    @Transactional(readOnly = true)
    public void performDailyConsistencyCheck() {
        if (!syncEnabled) {
            log.debug("Family mapping sync is disabled");
            return;
        }
        
        log.info("Starting daily family mapping consistency check");
        
        try {
            AtomicInteger checkedCount = new AtomicInteger(0);
            AtomicInteger inconsistencyCount = new AtomicInteger(0);
            
            int page = 0;
            boolean hasMoreData = true;
            int maxPagesToCheck = 100; // Limit to avoid long-running jobs

            while (hasMoreData && page < maxPagesToCheck) {
                Pageable pageable = PageRequest.of(page, batchSize);
                Page<VoterEntity> voterPage = voterRepository.findVotersWithFamilyMappings(pageable);

                if (voterPage.isEmpty()) {
                    hasMoreData = false;
                    break;
                }

                for (VoterEntity voter : voterPage.getContent()) {
                    checkedCount.incrementAndGet();
                    
                    try {
                        VoterMongo voterMongo = voterMongoRepository.findByAccountIdAndElectionIdAndEpicNumber(
                            voter.getAccountId(), voter.getElectionId(), voter.getEpicNumber());
                        
                        if (voterMongo == null || 
                            !voter.getFamilyId().equals(voterMongo.getFamilyId()) ||
                            (voter.getFamilyCount() != null && !voter.getFamilyCount().equals(voterMongo.getFamilyCount()))) {
                            
                            inconsistencyCount.incrementAndGet();
                            
                            if (inconsistencyCount.get() <= 10) { // Log first 10 inconsistencies
                                log.warn("Family mapping inconsistency detected: epicNumber={}, PG familyId={}, Mongo familyId={}", 
                                        voter.getEpicNumber(), voter.getFamilyId(), 
                                        voterMongo != null ? voterMongo.getFamilyId() : "null");
                            }
                        }
                        
                    } catch (Exception ex) {
                        log.error("Error during consistency check for voter: epicNumber={}, error={}", 
                                 voter.getEpicNumber(), ex.getMessage());
                    }
                }

                page++;
            }
            
            if (inconsistencyCount.get() > 0) {
                log.warn("Family mapping consistency check completed. Checked: {}, Inconsistencies found: {}. " +
                        "Consider running the repair migration.", checkedCount.get(), inconsistencyCount.get());
            } else {
                log.info("Family mapping consistency check completed. All {} checked mappings are consistent.", 
                        checkedCount.get());
            }
            
        } catch (Exception e) {
            log.error("Daily consistency check failed: {}", e.getMessage(), e);
        }
    }
}
