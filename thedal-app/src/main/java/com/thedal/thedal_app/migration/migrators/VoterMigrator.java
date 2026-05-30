package com.thedal.thedal_app.migration.migrators;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.WriteConcern;

import com.thedal.thedal_app.migration.MigrationJob;
import com.thedal.thedal_app.migration.MigrationJobStatus;
import com.thedal.thedal_app.migration.MigrationStats;
import com.thedal.thedal_app.migration.ValidationReport;
import com.thedal.thedal_app.voter.VoterEntity;
import com.thedal.thedal_app.voter.VoterMongo;
import com.thedal.thedal_app.voter.VoterMongoRepository;
import com.thedal.thedal_app.voter.VoterRepo;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class VoterMigrator {

    @Autowired
    private VoterRepo voterRepository;
    
    @Autowired
    private VoterMongoRepository voterMongoRepository;
    
    @Autowired
    private MongoTemplate mongoTemplate;
    
    @Autowired
    private TransactionTemplate transactionTemplate;

    /**
     * Standard migration method with proper connection management
     */
    public void migrate(MigrationJob job, int batchSize, boolean overwriteExisting) {
        log.info("Starting voter migration for accountId: {}, electionId: {}", job.getAccountId(), job.getElectionId());
        
        try {
            // Get total count for progress tracking in a separate transaction
            long totalVoters = executeInTransaction(() -> 
                voterRepository.countByAccountIdAndElectionId(job.getAccountId(), job.getElectionId()));
            job.setTotalRecords(totalVoters);
            
            log.info("Total voters to migrate: {}", totalVoters);
            
            if (totalVoters == 0) {
                log.info("No voters found to migrate for accountId: {}, electionId: {}", 
                    job.getAccountId(), job.getElectionId());
                return;
            }
            
            // Clear existing MongoDB data if overwrite is enabled
            if (overwriteExisting) {
                log.info("Clearing existing MongoDB voter data for accountId: {}, electionId: {}", 
                    job.getAccountId(), job.getElectionId());
                voterMongoRepository.deleteByAccountIdAndElectionId(job.getAccountId(), job.getElectionId());
            }
            
            // Process in batches
            int pageNumber = 0;
            long processedCount = 0;
            
            while (processedCount < totalVoters && !job.isCancelled()) {
                Pageable pageable = PageRequest.of(pageNumber, batchSize);
                
                // Use separate transaction for each batch
                List<VoterEntity> voters = executeInTransaction(() -> {
                    Page<VoterEntity> voterPage = voterRepository.findByAccountIdAndElectionId(
                        job.getAccountId(), job.getElectionId(), pageable);
                    
                    // Initialize lazy collections within the transaction
                    List<VoterEntity> voterList = voterPage.getContent();
                    for (VoterEntity voter : voterList) {
                        if (voter.getDynamicFields() != null) {
                            voter.getDynamicFields().size(); // Force lazy loading
                        }
                    }
                    return voterList;
                });
                
                if (voters.isEmpty()) {
                    break;
                }
                
                log.info("Processing batch {}: {} voters", pageNumber + 1, voters.size());
                
                try {
                    // Convert and save to MongoDB
                    for (VoterEntity voter : voters) {
                        if (job.isCancelled()) {
                            log.info("Migration cancelled, stopping voter migration");
                            return;
                        }
                        
                        try {
                            // Check if voter already exists in MongoDB
                            if (!overwriteExisting && voterMongoRepository.existsByEpicNumberAndElectionIdAndAccountId(
                                    voter.getEpicNumber(), voter.getElectionId(), voter.getAccountId())) {
                                log.debug("Voter with EPIC {} already exists in MongoDB, skipping", voter.getEpicNumber());
                                continue;
                            }
                            
                            // Create MongoDB document
                            VoterMongo voterMongo;
                            try {
                                voterMongo = new VoterMongo(voter);
                            } catch (Exception conversionEx) {
                                log.debug("Error converting voter {} using constructor, trying fallback: {}", 
                                         voter.getVoterId(), conversionEx.getMessage());
                                voterMongo = createVoterMongoSafely(voter);
                            }
                            voterMongoRepository.save(voterMongo);
                            
                            processedCount++;
                            job.setProcessedRecords(processedCount);
                            
                            // Log progress every 100 records
                            if (processedCount % 100 == 0) {
                                log.info("Migrated {} of {} voters ({:.1f}%)", 
                                    processedCount, totalVoters, (processedCount * 100.0) / totalVoters);
                            }
                            
                        } catch (Exception e) {
                            log.error("Failed to migrate voter with EPIC {}: {}", voter.getEpicNumber(), e.getMessage());
                            job.setFailedRecords(job.getFailedRecords() + 1);
                        }
                    }
                    
                } catch (Exception e) {
                    log.error("Error processing voter batch {}: {}", pageNumber + 1, e.getMessage(), e);
                    job.setFailedRecords(job.getFailedRecords() + voters.size());
                }
                
                pageNumber++;
            }
            
            if (job.isCancelled()) {
                job.setStatus(MigrationJobStatus.CANCELLED);
            } else {
                log.info("Voter migration completed. Processed: {}, Failed: {}", 
                    job.getProcessedRecords(), job.getFailedRecords());
            }
            
        } catch (Exception e) {
            log.error("Error during voter migration: {}", e.getMessage(), e);
            job.setStatus(MigrationJobStatus.FAILED);
            job.setErrorMessage("Voter migration failed: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Ultra-fast bulk migration using MongoDB native operations
     * Maintains data integrity while achieving maximum performance
     * Fixed: Removed @Transactional to prevent connection leaks in parallel processing
     */
    public void migrateBulk(MigrationJob job, int batchSize, boolean overwriteExisting) {
        log.info("Starting BULK voter migration for accountId: {}, electionId: {}", 
                job.getAccountId(), job.getElectionId());
        
        try {
            // Get total count for progress tracking
            long totalVoters = voterRepository.countByAccountIdAndElectionId(job.getAccountId(), job.getElectionId());
            job.setTotalRecords(totalVoters);
            
            log.info("Total voters to migrate: {} (using bulk operations)", totalVoters);
            
            if (totalVoters == 0) {
                log.info("No voters found to migrate for accountId: {}, electionId: {}", 
                    job.getAccountId(), job.getElectionId());
                return;
            }
            
            // Clear existing MongoDB data if overwrite is enabled
            if (overwriteExisting) {
                log.info("Clearing existing MongoDB voter data for accountId: {}, electionId: {}", 
                    job.getAccountId(), job.getElectionId());
                voterMongoRepository.deleteByAccountIdAndElectionId(job.getAccountId(), job.getElectionId());
            }
            
            // Use larger batch size for bulk operations (10x faster)
            int bulkBatchSize = Math.max(batchSize, 5000); // Minimum 5000 for bulk operations
            int pageNumber = 0;
            long processedCount = 0;
            long totalBatches = (totalVoters + bulkBatchSize - 1) / bulkBatchSize;
            
            log.info("Processing {} voters in {} bulk batches of {} records each", 
                    totalVoters, totalBatches, bulkBatchSize);
            
            while (processedCount < totalVoters && !job.isCancelled()) {
                Pageable pageable = PageRequest.of(pageNumber, bulkBatchSize);
                
                // Use separate transaction for each batch to prevent connection leaks
                List<VoterEntity> voters = executeInTransaction(() -> {
                    // Use the new eager loading method to prevent lazy loading issues
                    Page<VoterEntity> voterPage = voterRepository.findByAccountIdAndElectionIdWithEagerLoading(
                        job.getAccountId(), job.getElectionId(), pageable);
                    
                    // All relationships are now eagerly loaded, no need for manual initialization
                    List<VoterEntity> voterList = voterPage.getContent();
                    return voterList;
                });
                
                if (voters.isEmpty()) {
                    break;
                }
                log.info("Processing bulk batch {}/{}: {} voters", 
                        pageNumber + 1, totalBatches, voters.size());
                
                try {
                    // BULK OPERATION: Process entire batch in single MongoDB operation
                    long batchProcessed = processBulkBatch(voters, overwriteExisting, job);
                    
                    processedCount += batchProcessed;
                    job.setProcessedRecords(processedCount);
                    
                    // Log progress
                    double progressPercent = (processedCount * 100.0) / totalVoters;
                    log.info("Bulk migration progress: {}/{} voters ({:.1f}%) - Batch {}/{}", 
                            processedCount, totalVoters, progressPercent, pageNumber + 1, totalBatches);
                    
                } catch (Exception e) {
                    log.error("Error processing bulk batch {}: {}", pageNumber + 1, e.getMessage(), e);
                    job.setFailedRecords(job.getFailedRecords() + voters.size());
                    
                    // Fallback to individual processing for this batch to maintain data integrity
                    log.info("Falling back to individual processing for batch {}", pageNumber + 1);
                    processBatchIndividually(voters, overwriteExisting, job);
                }
                
                pageNumber++;
                
                // Memory management: Force garbage collection after large batches
                if (pageNumber % 10 == 0) {
                    System.gc();
                }
            }
            
            job.setStatus(MigrationJobStatus.COMPLETED);
            log.info("BULK voter migration completed successfully. Processed: {}, Failed: {}", 
                    processedCount, job.getFailedRecords());
            
        } catch (Exception e) {
            log.error("Error in bulk voter migration: {}", e.getMessage(), e);
            job.setStatus(MigrationJobStatus.FAILED);
            job.setErrorMessage(e.getMessage());
            throw e;
        }
    }

    /**
     * Process a batch using MongoDB's native bulk operations
     * This is where the real speed comes from - single network call for thousands of records
     */
    private long processBulkBatch(List<VoterEntity> voters, boolean overwriteExisting, MigrationJob job) {
        if (voters.isEmpty()) {
            return 0;
        }
        
        try {
            // Create MongoDB bulk operations (UNORDERED for maximum speed)
            BulkOperations bulkOps = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, VoterMongo.class);
            
            int validRecords = 0;
            int skippedRecords = 0;
            
            for (VoterEntity voter : voters) {
                if (job.isCancelled()) {
                    break;
                }
                
                try {
                    // Data integrity check: Validate essential fields
                    if (voter.getEpicNumber() == null || voter.getEpicNumber().trim().isEmpty()) {
                        log.warn("Skipping voter with null/empty EPIC number: {}", voter.getVoterId());
                        skippedRecords++;
                        continue;
                    }
                    
                    if (voter.getAccountId() == null || voter.getElectionId() == null) {
                        log.warn("Skipping voter with null accountId/electionId: {}", voter.getVoterId());
                        skippedRecords++;
                        continue;
                    }
                    
                    // Create MongoDB document with proper handling of lazy fields
                    VoterMongo voterMongo;
                    try {
                        voterMongo = new VoterMongo(voter);
                    } catch (Exception conversionEx) {
                        // Handle lazy loading issues gracefully
                        log.warn("Error converting voter {} to MongoDB document, trying fallback: {}", 
                                voter.getVoterId(), conversionEx.getMessage());
                        
                        // Create VoterMongo manually without problematic fields
                        voterMongo = createVoterMongoSafely(voter);
                    }
                    
                    // Validate the converted document
                    if (voterMongo.getEpicNumber() == null || voterMongo.getAccountId() == null) {
                        log.warn("Skipping voter with conversion issues: {}", voter.getVoterId());
                        skippedRecords++;
                        continue;
                    }
                    
                    // For bulk operations, use insert mode for maximum performance
                    // If overwriteExisting is true, we clear the collection before bulk insert
                    bulkOps.insert(voterMongo);
                    
                    validRecords++;
                    
                } catch (Exception e) {
                    log.warn("Error preparing voter {} for bulk operation: {}", 
                            voter.getVoterId(), e.getMessage());
                    skippedRecords++;
                }
            }
            
            // Execute the bulk operation (SINGLE NETWORK CALL)
            if (validRecords > 0) {
                long startTime = System.currentTimeMillis();
                BulkWriteResult result = bulkOps.execute();
                long endTime = System.currentTimeMillis();
                
                log.info("Bulk operation completed in {}ms: {} records processed, {} inserted, {} modified, {} skipped", 
                        (endTime - startTime), validRecords, 
                        result.getInsertedCount(), result.getModifiedCount(), skippedRecords);
                
                return validRecords;
            } else {
                log.warn("No valid records to process in this batch");
                return 0;
            }
            
        } catch (Exception e) {
            log.error("Error in bulk batch processing: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Fallback method: Process batch individually if bulk operation fails
     * Ensures data integrity even if bulk operations encounter issues
     */
    private void processBatchIndividually(List<VoterEntity> voters, boolean overwriteExisting, MigrationJob job) {
        log.info("Processing {} voters individually as fallback", voters.size());
        
        for (VoterEntity voter : voters) {
            if (job.isCancelled()) {
                break;
            }
            
            try {
                // Data integrity checks
                if (voter.getEpicNumber() == null || voter.getEpicNumber().trim().isEmpty()) {
                    log.warn("Skipping voter with null/empty EPIC number: {}", voter.getVoterId());
                    continue;
                }
                
                // Check if voter already exists in MongoDB
                if (!overwriteExisting && voterMongoRepository.existsByEpicNumberAndElectionIdAndAccountId(
                        voter.getEpicNumber(), voter.getElectionId(), voter.getAccountId())) {
                    log.debug("Voter with EPIC {} already exists in MongoDB, skipping", voter.getEpicNumber());
                    continue;
                }
                
                // Create and save MongoDB document
                VoterMongo voterMongo;
                try {
                    voterMongo = new VoterMongo(voter);
                } catch (Exception conversionEx) {
                    log.debug("Error converting voter {} using constructor, trying fallback: {}", 
                             voter.getVoterId(), conversionEx.getMessage());
                    voterMongo = createVoterMongoSafely(voter);
                }
                
                // Final validation before save
                if (voterMongo.getEpicNumber() != null && voterMongo.getAccountId() != null) {
                    voterMongoRepository.save(voterMongo);
                    job.setProcessedRecords(job.getProcessedRecords() + 1);
                } else {
                    log.warn("Skipping voter with conversion issues: {}", voter.getVoterId());
                }
                
            } catch (Exception e) {
                log.error("Error migrating individual voter {}: {}", voter.getVoterId(), e.getMessage());
                job.setFailedRecords(job.getFailedRecords() + 1);
            }
        }
    }

    public ValidationReport.EntityValidationResult validateConsistency(Long accountId, Long electionId) {
        try {
            long postgresCount = voterRepository.countByAccountIdAndElectionId(accountId, electionId);
            long mongoCount = voterMongoRepository.countByAccountIdAndElectionId(accountId, electionId);
            
            boolean isConsistent = (postgresCount == mongoCount);
            String discrepancyDetails = isConsistent ? "Counts match" : 
                String.format("PostgreSQL: %d, MongoDB: %d (difference: %d)", 
                    postgresCount, mongoCount, Math.abs(postgresCount - mongoCount));
            
            return new ValidationReport.EntityValidationResult("voters", postgresCount, mongoCount, 
                isConsistent, discrepancyDetails);
            
        } catch (Exception e) {
            log.error("Error validating voter consistency: {}", e.getMessage(), e);
            return new ValidationReport.EntityValidationResult("voters", -1, -1, false, 
                "Validation failed: " + e.getMessage());
        }
    }

    public MigrationStats.EntityStats getEntityStats(Long accountId, Long electionId) {
        try {
            long postgresCount = voterRepository.countByAccountIdAndElectionId(accountId, electionId);
            long mongoCount = voterMongoRepository.countByAccountIdAndElectionId(accountId, electionId);
            
            return new MigrationStats.EntityStats("voters", postgresCount, mongoCount, 
                mongoCount > 0, LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
        } catch (Exception e) {
            log.error("Error getting voter stats: {}", e.getMessage(), e);
            return new MigrationStats.EntityStats("voters", 0, 0, false, "Error");
        }
    }

    public long cleanupMongoData(Long accountId, Long electionId) {
        try {
            long countBefore = voterMongoRepository.countByAccountIdAndElectionId(accountId, electionId);
            voterMongoRepository.deleteByAccountIdAndElectionId(accountId, electionId);
            
            log.info("Cleaned up {} voter records from MongoDB for accountId: {}, electionId: {}", 
                countBefore, accountId, electionId);
            
            return countBefore;
            
        } catch (Exception e) {
            log.error("Error cleaning up voter MongoDB data: {}", e.getMessage(), e);
            return 0;
        }
    }

    public long countMongoRecords(Long accountId, Long electionId) {
        try {
            return voterMongoRepository.countByAccountIdAndElectionId(accountId, electionId);
        } catch (Exception e) {
            log.error("Error counting voter MongoDB records: {}", e.getMessage(), e);
            return 0;
        }
    }

    public long countRecords(Long accountId, Long electionId) {
        try {
            return executeInTransaction(() -> 
                voterRepository.countByAccountIdAndElectionId(accountId, electionId));
        } catch (Exception e) {
            log.error("Error counting voter records: {}", e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Helper method to execute database operations in separate, short-lived transactions
     * Prevents connection leaks in parallel processing and ensures proper resource cleanup
     */
    private <T> T executeInTransaction(java.util.function.Supplier<T> operation) {
        return transactionTemplate.execute(status -> {
            try {
                T result = operation.get();
                // Transaction will be committed automatically when this lambda returns
                return result;
            } catch (Exception e) {
                log.error("Transaction failed, rolling back: {}", e.getMessage());
                status.setRollbackOnly();
                throw new RuntimeException("Transaction failed", e);
            }
        });
    }

    /**
     * Create VoterMongo safely, handling lazy loading issues
     * This is a fallback when the main constructor fails due to session issues
     */
    private VoterMongo createVoterMongoSafely(VoterEntity voter) {
        try {
            VoterMongo voterMongo = new VoterMongo();
            
            // Copy essential fields manually to avoid lazy loading issues
            voterMongo.setVoterId(voter.getVoterId());
            voterMongo.setEpicNumber(voter.getEpicNumber());
            voterMongo.setAccountId(voter.getAccountId());
            voterMongo.setElectionId(voter.getElectionId());
            
            // Name fields - use actual field names from VoterEntity
            voterMongo.setVoterFnameEn(voter.getVoterFnameEn());
            voterMongo.setVoterLnameEn(voter.getVoterLnameEn());
            voterMongo.setVoterFnameL1(voter.getVoterFnameL1());
            voterMongo.setVoterFnameL2(voter.getVoterFnameL2());
            voterMongo.setVoterLnameL1(voter.getVoterLnameL1());
            voterMongo.setVoterLnameL2(voter.getVoterLnameL2());
            
            // Relation fields
            voterMongo.setRlnType(voter.getRlnType());
            voterMongo.setRlnFnameEn(voter.getRlnFnameEn());
            voterMongo.setRlnLnameEn(voter.getRlnLnameEn());
            voterMongo.setRlnFnameL1(voter.getRlnFnameL1());
            voterMongo.setRlnFnameL2(voter.getRlnFnameL2());
            voterMongo.setRlnLnameL1(voter.getRlnLnameL1());
            voterMongo.setRlnLnameL2(voter.getRlnLnameL2());
            
            // Basic fields
            voterMongo.setGender(voter.getGender());
            voterMongo.setAge(voter.getAge());
            voterMongo.setDob(voter.getDob());
            voterMongo.setHouseNoEn(voter.getHouseNoEn());
            voterMongo.setHouseNoL1(voter.getHouseNoL1());
            voterMongo.setHouseNoL2(voter.getHouseNoL2());
            voterMongo.setPartNo(voter.getPartNo());
            voterMongo.setSerialNo(voter.getSerialNo());
            voterMongo.setBoothNumber(voter.getBoothNumber());
            voterMongo.setMobileNo(voter.getMobileNo());
            voterMongo.setWhatsappNo(voter.getWhatsappNo());
            voterMongo.setEMail(voter.getEMail());
            voterMongo.setCreatedTime(voter.getCreatedTime());
            voterMongo.setModifiedTime(voter.getModifiedTime());
            
            // Location fields
            voterMongo.setFullAddress(voter.getFullAddress());
            voterMongo.setPincode(voter.getPincode());
            voterMongo.setVoterLati(voter.getVoterLati());
            voterMongo.setVoterLongi(voter.getVoterLongi());
            
            // Skip dynamicFields if it causes issues - set to empty map
            try {
                if (voter.getDynamicFields() != null) {
                    voterMongo.setDynamicFields(voter.getDynamicFields());
                } else {
                    voterMongo.setDynamicFields(new java.util.HashMap<>());
                }
            } catch (Exception dynamicFieldsEx) {
                log.debug("Could not load dynamicFields for voter {}, setting to empty: {}", 
                         voter.getVoterId(), dynamicFieldsEx.getMessage());
                voterMongo.setDynamicFields(new java.util.HashMap<>());
            }
            
            return voterMongo;
            
        } catch (Exception e) {
            log.error("Failed to create VoterMongo safely for voter {}: {}", voter.getVoterId(), e.getMessage());
            throw new RuntimeException("Cannot convert voter to MongoDB document", e);
        }
    }
}
