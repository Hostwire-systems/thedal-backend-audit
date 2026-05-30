package com.thedal.thedal_app.migration.migrators;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.thedal.thedal_app.migration.MigrationJob;
import com.thedal.thedal_app.migration.MigrationJobStatus;
import com.thedal.thedal_app.migration.MigrationStats;
import com.thedal.thedal_app.migration.ValidationReport;
import com.thedal.thedal_app.user.MongoUser;
import com.thedal.thedal_app.user.MongoUserRepository;
import com.thedal.thedal_app.user.UserEntity;
import com.thedal.thedal_app.user.UserRepo;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class UserMigrator {

    @Autowired
    private UserRepo userRepository;
    
    @Autowired
    private MongoUserRepository mongoUserRepository;

    @Transactional(readOnly = true)
    public void migrate(MigrationJob job, int batchSize, boolean overwriteExisting) {
        log.info("Starting user migration for accountId: {}", job.getAccountId());
        
        try {
            // Get total count for progress tracking
            long totalUsers = userRepository.countByAccountEntityId(job.getAccountId());
            job.setTotalRecords(totalUsers);
            
            log.info("Total users to migrate: {}", totalUsers);
            
            if (totalUsers == 0) {
                log.info("No users found to migrate for accountId: {}", job.getAccountId());
                return;
            }
            
            // Clear existing MongoDB data if overwrite is enabled
            if (overwriteExisting) {
                log.info("Clearing existing MongoDB user data for accountId: {}", job.getAccountId());
                mongoUserRepository.deleteByAccountId(job.getAccountId());
            }
            
            // Process in batches
            int pageNumber = 0;
            long processedCount = 0;
            
            while (processedCount < totalUsers && !job.isCancelled()) {
                Pageable pageable = PageRequest.of(pageNumber, batchSize);
                Page<UserEntity> userPage = userRepository.findByAccountEntityId(job.getAccountId(), pageable);
                
                if (userPage.isEmpty()) {
                    break;
                }
                
                List<UserEntity> users = userPage.getContent();
                log.info("Processing user batch {}: {} users", pageNumber + 1, users.size());
                
                try {
                    // Convert and save to MongoDB
                    for (UserEntity user : users) {
                        if (job.isCancelled()) {
                            log.info("Migration cancelled, stopping user migration");
                            return;
                        }
                        
                        try {
                            // Check if user already exists in MongoDB
                            if (!overwriteExisting && mongoUserRepository.existsById(user.getId().toString())) {
                                log.debug("User with ID {} already exists in MongoDB, skipping", user.getId());
                                continue;
                            }
                            
                            // Create MongoDB document using proper constructor
                            MongoUser mongoUser = new MongoUser(user);
                            
                            mongoUserRepository.save(mongoUser);
                            
                            processedCount++;
                            job.setProcessedRecords(processedCount);
                            
                            // Log progress every 50 records
                            if (processedCount % 50 == 0) {
                                log.info("Migrated {} of {} users ({:.1f}%)", 
                                    processedCount, totalUsers, (processedCount * 100.0) / totalUsers);
                            }
                            
                        } catch (Exception e) {
                            log.error("Failed to migrate user with ID {}: {}", user.getId(), e.getMessage());
                            job.setFailedRecords(job.getFailedRecords() + 1);
                        }
                    }
                    
                } catch (Exception e) {
                    log.error("Error processing user batch {}: {}", pageNumber + 1, e.getMessage(), e);
                    job.setFailedRecords(job.getFailedRecords() + users.size());
                }
                
                pageNumber++;
            }
            
            if (job.isCancelled()) {
                job.setStatus(MigrationJobStatus.CANCELLED);
            } else {
                log.info("User migration completed. Processed: {}, Failed: {}", 
                    job.getProcessedRecords(), job.getFailedRecords());
            }
            
        } catch (Exception e) {
            log.error("Error during user migration: {}", e.getMessage(), e);
            job.setStatus(MigrationJobStatus.FAILED);
            job.setErrorMessage("User migration failed: " + e.getMessage());
            throw e;
        }
    }

    public ValidationReport.EntityValidationResult validateConsistency(Long accountId) {
        try {
            long postgresCount = userRepository.countByAccountEntityId(accountId);
            long mongoCount = mongoUserRepository.countByAccountId(accountId);
            
            boolean isConsistent = (postgresCount == mongoCount);
            String discrepancyDetails = isConsistent ? "Counts match" : 
                String.format("PostgreSQL: %d, MongoDB: %d (difference: %d)", 
                    postgresCount, mongoCount, Math.abs(postgresCount - mongoCount));
            
            return new ValidationReport.EntityValidationResult("users", postgresCount, mongoCount, 
                isConsistent, discrepancyDetails);
            
        } catch (Exception e) {
            log.error("Error validating user consistency: {}", e.getMessage(), e);
            return new ValidationReport.EntityValidationResult("users", -1, -1, false, 
                "Validation failed: " + e.getMessage());
        }
    }

    public MigrationStats.EntityStats getEntityStats(Long accountId) {
        try {
            long postgresCount = userRepository.countByAccountEntityId(accountId);
            long mongoCount = mongoUserRepository.countByAccountId(accountId);
            
            return new MigrationStats.EntityStats("users", postgresCount, mongoCount, 
                mongoCount > 0, LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
        } catch (Exception e) {
            log.error("Error getting user stats: {}", e.getMessage(), e);
            return new MigrationStats.EntityStats("users", 0, 0, false, "Error");
        }
    }

    public long cleanupMongoData(Long accountId) {
        try {
            long countBefore = mongoUserRepository.countByAccountId(accountId);
            mongoUserRepository.deleteByAccountId(accountId);
            
            log.info("Cleaned up {} user records from MongoDB for accountId: {}", 
                countBefore, accountId);
            
            return countBefore;
            
        } catch (Exception e) {
            log.error("Error cleaning up user MongoDB data: {}", e.getMessage(), e);
            return 0;
        }
    }

    public long countMongoRecords(Long accountId) {
        try {
            return mongoUserRepository.countByAccountId(accountId);
        } catch (Exception e) {
            log.error("Error counting user MongoDB records: {}", e.getMessage(), e);
            return 0;
        }
    }
}
