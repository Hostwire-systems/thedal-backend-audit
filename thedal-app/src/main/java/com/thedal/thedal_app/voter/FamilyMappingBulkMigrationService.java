package com.thedal.thedal_app.voter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class FamilyMappingBulkMigrationService {

    @Autowired
    private VoterRepo voterRepository;
    
    @Autowired
    private VoterMongoRepository voterMongoRepository;
    
    private static final int BATCH_SIZE = 1000;
    private static final int MONGO_BATCH_SIZE = 500;

    /**
     * Performs bulk migration of all family mapping data from PostgreSQL to MongoDB
     */
    @Transactional(readOnly = true)
    public String performBulkFamilyMappingMigration() {
        log.info("Starting bulk family mapping migration for all accounts and elections");
        
        AtomicInteger totalVoters = new AtomicInteger(0);
        AtomicInteger familyMappedVoters = new AtomicInteger(0);
        AtomicInteger mongoUpdates = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        
        long startTime = System.currentTimeMillis();
        
        try {
            int page = 0;
            boolean hasMoreData = true;

            // Log initial progress
            log.info("=== FAMILY MAPPING MIGRATION STARTED ===");
            log.info("Batch size: {}, MongoDB batch size: {}", BATCH_SIZE, MONGO_BATCH_SIZE);

            while (hasMoreData) {
                long pageStartTime = System.currentTimeMillis();
                
                Pageable pageable = PageRequest.of(page, BATCH_SIZE);
                Page<VoterEntity> voterPage = voterRepository.findVotersWithFamilyMappings(pageable);

                if (voterPage.isEmpty()) {
                    hasMoreData = false;
                    break;
                }

                // Process batch in smaller chunks for MongoDB
                List<VoterEntity> voters = voterPage.getContent();
                List<VoterMongo> mongoVoters = new ArrayList<>();
                
                for (VoterEntity voter : voters) {
                    totalVoters.incrementAndGet();
                    
                    if (voter.getFamilyId() != null) {
                        familyMappedVoters.incrementAndGet();
                        
                        try {
                            VoterMongo voterMongo = new VoterMongo(voter);
                            mongoVoters.add(voterMongo);
                            
                            // Process in smaller batches
                            if (mongoVoters.size() >= MONGO_BATCH_SIZE) {
                                processMongoBatch(mongoVoters, mongoUpdates, errorCount);
                                mongoVoters.clear();
                            }
                            
                        } catch (Exception ex) {
                            errorCount.incrementAndGet();
                            log.error("Failed to prepare voter for MongoDB: epicNumber={}, familyId={}, error={}", 
                                     voter.getEpicNumber(), voter.getFamilyId(), ex.getMessage());
                        }
                    }
                }
                
                // Process remaining voters in batch
                if (!mongoVoters.isEmpty()) {
                    processMongoBatch(mongoVoters, mongoUpdates, errorCount);
                    mongoVoters.clear();
                }

                page++;
                long pageTime = System.currentTimeMillis() - pageStartTime;
                
                // Enhanced progress logging - every 5 pages instead of 10 for better visibility
                if (page % 5 == 0) {
                    long elapsedTime = System.currentTimeMillis() - startTime;
                    double avgTimePerPage = (double) elapsedTime / page;
                    
                    log.info("=== MIGRATION PROGRESS ===");
                    log.info("Pages processed: {} | Current page time: {}ms | Avg time per page: {:.2f}ms", 
                             page, pageTime, avgTimePerPage);
                    log.info("Total voters processed: {} | Family mappings found: {} | MongoDB updates: {} | Errors: {}", 
                             totalVoters.get(), familyMappedVoters.get(), mongoUpdates.get(), errorCount.get());
                    log.info("Migration rate: {:.2f} voters/second", 
                             totalVoters.get() / (elapsedTime / 1000.0));
                    log.info("===========================");
                }
                
                // Force garbage collection every 50 pages to help with memory management
                if (page % 50 == 0) {
                    System.gc();
                    log.debug("Performed garbage collection at page {}", page);
                }
            }

            long totalTime = System.currentTimeMillis() - startTime;
            double avgVotersPerSecond = totalVoters.get() / (totalTime / 1000.0);
            
            String result = String.format(
                "Bulk family mapping migration completed successfully!\n" +
                "=== FINAL RESULTS ===\n" +
                "Total execution time: %.2f seconds\n" +
                "Pages processed: %d\n" +
                "Total voters processed: %d\n" +
                "Family mappings found: %d\n" +
                "MongoDB updates: %d\n" +
                "Errors encountered: %d\n" +
                "Average processing rate: %.2f voters/second\n" +
                "====================",
                totalTime / 1000.0, page, totalVoters.get(), familyMappedVoters.get(), 
                mongoUpdates.get(), errorCount.get(), avgVotersPerSecond
            );
            
            log.info(result);
            return result;
            
        } catch (Exception e) {
            String error = String.format("Bulk family mapping migration failed: %s", e.getMessage());
            log.error(error, e);
            throw new RuntimeException(error, e);
        }
    }
    
    private void processMongoBatch(List<VoterMongo> mongoVoters, 
                                   AtomicInteger mongoUpdates, 
                                   AtomicInteger errorCount) {
        if (mongoVoters.isEmpty()) {
            return;
        }
        
        long batchStartTime = System.currentTimeMillis();
        
        try {
            log.debug("Processing MongoDB batch of {} voters", mongoVoters.size());
            
            // Use the new bulk upsert method to prevent duplicates
            voterMongoRepository.bulkUpsertVoterMongoWithDeduplication(mongoVoters);
            mongoUpdates.addAndGet(mongoVoters.size());
            
            long batchTime = System.currentTimeMillis() - batchStartTime;
            log.debug("Successfully processed MongoDB batch of {} voters in {}ms", 
                     mongoVoters.size(), batchTime);
                     
        } catch (Exception e) {
            long batchTime = System.currentTimeMillis() - batchStartTime;
            log.warn("Batch processing failed after {}ms, falling back to individual processing for {} voters: {}", 
                    batchTime, mongoVoters.size(), e.getMessage());
            
            // Fallback to individual processing
            int individualSuccesses = 0;
            int individualFailures = 0;
            
            for (VoterMongo voterMongo : mongoVoters) {
                try {
                    voterMongoRepository.saveVoterMongoWithNullFields(voterMongo);
                    mongoUpdates.incrementAndGet();
                    individualSuccesses++;
                } catch (Exception ex) {
                    errorCount.incrementAndGet();
                    individualFailures++;
                    log.error("Failed to save individual voter to MongoDB: epicNumber={}, familyId={}, error={}", 
                             voterMongo.getEpicNumber(), voterMongo.getFamilyId(), ex.getMessage());
                }
            }
            
            log.info("Individual processing completed: {} successes, {} failures out of {} voters", 
                    individualSuccesses, individualFailures, mongoVoters.size());
        }
    }

    /**
     * Repairs inconsistent family mapping data between PostgreSQL and MongoDB
     */
    @Transactional
    public String repairInconsistentFamilyMappings() {
        log.info("Starting repair of inconsistent family mapping data");
        
        AtomicInteger totalChecked = new AtomicInteger(0);
        AtomicInteger inconsistencies = new AtomicInteger(0);
        AtomicInteger repaired = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        
        try {
            int page = 0;
            boolean hasMoreData = true;

            while (hasMoreData) {
                Pageable pageable = PageRequest.of(page, BATCH_SIZE);
                Page<VoterEntity> voterPage = voterRepository.findVotersWithFamilyMappings(pageable);

                if (voterPage.isEmpty()) {
                    hasMoreData = false;
                    break;
                }

                for (VoterEntity voter : voterPage.getContent()) {
                    totalChecked.incrementAndGet();
                    
                    if (voter.getFamilyId() != null) {
                        try {
                            // Check MongoDB
                            VoterMongo voterMongo = voterMongoRepository.findByAccountIdAndElectionIdAndEpicNumber(
                                voter.getAccountId(), voter.getElectionId(), voter.getEpicNumber());
                            
                            boolean needsRepair = false;
                            
                            if (voterMongo == null) {
                                // Missing in MongoDB
                                voterMongo = new VoterMongo(voter);
                                needsRepair = true;
                                inconsistencies.incrementAndGet();
                                log.debug("Voter missing in MongoDB: epicNumber={}", voter.getEpicNumber());
                                
                            } else {
                                // Check for inconsistencies
                                UUID pgFamilyId = voter.getFamilyId();
                                UUID mongoFamilyId = voterMongo.getFamilyId();
                                Integer pgFamilyCount = voter.getFamilyCount();
                                Integer mongoFamilyCount = voterMongo.getFamilyCount();
                                
                                if (!pgFamilyId.equals(mongoFamilyId) || 
                                    (pgFamilyCount != null && !pgFamilyCount.equals(mongoFamilyCount))) {
                                    
                                    // Update MongoDB with PostgreSQL data
                                    voterMongo.setFamilyId(pgFamilyId);
                                    voterMongo.setFamilyCount(pgFamilyCount != null ? pgFamilyCount : 1);
                                    needsRepair = true;
                                    inconsistencies.incrementAndGet();
                                    log.debug("Inconsistent data found: epicNumber={}, PG familyId={}, Mongo familyId={}", 
                                            voter.getEpicNumber(), pgFamilyId, mongoFamilyId);
                                }
                            }
                            
                            if (needsRepair) {
                                voterMongoRepository.saveVoterMongoWithNullFields(voterMongo);
                                repaired.incrementAndGet();
                            }
                            
                        } catch (Exception ex) {
                            errorCount.incrementAndGet();
                            log.error("Failed to repair voter: epicNumber={}, familyId={}, error={}", 
                                     voter.getEpicNumber(), voter.getFamilyId(), ex.getMessage());
                        }
                    }
                }

                page++;
                
                if (page % 10 == 0) {
                    log.info("Repair progress: Checked {} voters, Found {} inconsistencies, Repaired {}", 
                           totalChecked.get(), inconsistencies.get(), repaired.get());
                }
            }

            String result = String.format(
                "Family mapping repair completed. " +
                "Total checked: %d, Inconsistencies found: %d, Repaired: %d, Errors: %d",
                totalChecked.get(), inconsistencies.get(), repaired.get(), errorCount.get()
            );
            
            log.info(result);
            return result;
            
        } catch (Exception e) {
            String error = String.format("Family mapping repair failed: %s", e.getMessage());
            log.error(error, e);
            throw new RuntimeException(error, e);
        }
    }

    /**
     * Validates all family mapping data for consistency
     */
    @Transactional(readOnly = true)
    public String performComprehensiveValidation() {
        log.info("Starting comprehensive family mapping validation");
        
        AtomicInteger totalVoters = new AtomicInteger(0);
        AtomicInteger familyMappedVoters = new AtomicInteger(0);
        AtomicInteger consistentMappings = new AtomicInteger(0);
        AtomicInteger inconsistentMappings = new AtomicInteger(0);
        AtomicInteger missingInMongo = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        
        try {
            int page = 0;
            boolean hasMoreData = true;

            while (hasMoreData) {
                Pageable pageable = PageRequest.of(page, BATCH_SIZE);
                Page<VoterEntity> voterPage = voterRepository.findAll(pageable);

                if (voterPage.isEmpty()) {
                    hasMoreData = false;
                    break;
                }

                for (VoterEntity voter : voterPage.getContent()) {
                    totalVoters.incrementAndGet();
                    
                    if (voter.getFamilyId() != null) {
                        familyMappedVoters.incrementAndGet();
                        
                        try {
                            VoterMongo voterMongo = voterMongoRepository.findByAccountIdAndElectionIdAndEpicNumber(
                                voter.getAccountId(), voter.getElectionId(), voter.getEpicNumber());
                            
                            if (voterMongo == null) {
                                missingInMongo.incrementAndGet();
                            } else {
                                UUID pgFamilyId = voter.getFamilyId();
                                UUID mongoFamilyId = voterMongo.getFamilyId();
                                Integer pgFamilyCount = voter.getFamilyCount();
                                Integer mongoFamilyCount = voterMongo.getFamilyCount();
                                
                                if (pgFamilyId.equals(mongoFamilyId) && 
                                    (pgFamilyCount != null ? pgFamilyCount.equals(mongoFamilyCount) : mongoFamilyCount == null)) {
                                    consistentMappings.incrementAndGet();
                                } else {
                                    inconsistentMappings.incrementAndGet();
                                }
                            }
                            
                        } catch (Exception ex) {
                            errorCount.incrementAndGet();
                            log.error("Validation error for voter: epicNumber={}, error={}", 
                                     voter.getEpicNumber(), ex.getMessage());
                        }
                    }
                }

                page++;
                
                if (page % 20 == 0) {
                    log.info("Validation progress: {} voters checked, {} family mappings validated", 
                           totalVoters.get(), familyMappedVoters.get());
                }
            }

            String result = String.format(
                "Comprehensive family mapping validation completed. " +
                "Total voters: %d, Family mapped: %d, Consistent: %d, Inconsistent: %d, " +
                "Missing in MongoDB: %d, Errors: %d",
                totalVoters.get(), familyMappedVoters.get(), consistentMappings.get(), 
                inconsistentMappings.get(), missingInMongo.get(), errorCount.get()
            );
            
            log.info(result);
            return result;
            
        } catch (Exception e) {
            String error = String.format("Comprehensive validation failed: %s", e.getMessage());
            log.error(error, e);
            throw new RuntimeException(error, e);
        }
    }
}
