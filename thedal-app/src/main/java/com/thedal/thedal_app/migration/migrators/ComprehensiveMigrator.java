package com.thedal.thedal_app.migration.migrators;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.thedal.thedal_app.migration.GlobalMigrationJob;
import com.thedal.thedal_app.migration.GlobalMigrationJobStatus;
import com.thedal.thedal_app.election.ElectionEntity;
import com.thedal.thedal_app.election.ElectionRepository;
import com.thedal.thedal_app.election.ElectionMongo;
import com.thedal.thedal_app.election.ElectionMongoRepository;
import com.thedal.thedal_app.files.Files;
import com.thedal.thedal_app.files.FilesMongo;
import com.thedal.thedal_app.files.FilesMongoRepository;
import com.thedal.thedal_app.files.FilesRepository;
import com.thedal.thedal_app.volunteer.VolunteerEntity;
import com.thedal.thedal_app.volunteer.VolunteerRepository;
import com.thedal.thedal_app.volunteer.MongoVolunteer;
import com.thedal.thedal_app.volunteer.MongoVolunteerRepository;
import com.thedal.thedal_app.user.UserEntity;
import com.thedal.thedal_app.volunteer.VolunteerDailyActivityEntity;
import com.thedal.thedal_app.volunteer.VolunteerDailyActivityRepository;
import com.thedal.thedal_app.volunteer.MongoVolunteerDailyActivityRepository;
import com.thedal.thedal_app.volunteer.VolunteerActivityLogsEntity;
import com.thedal.thedal_app.volunteer.VolunteerActivityLogsRepository;
import com.thedal.thedal_app.volunteer.MongoVolunteerActivityLogRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Comprehensive migrator for all additional entities not covered by existing migrators
 */
@Component
@Slf4j
public class ComprehensiveMigrator {

    @Autowired
    private ElectionRepository electionRepository;
    
    @Autowired
    private ElectionMongoRepository electionMongoRepository;
    
    @Autowired
    private VolunteerRepository volunteerRepository;
    
    @Autowired
    private MongoVolunteerRepository mongoVolunteerRepository;
    
    @Autowired
    private FilesRepository filesRepository;
    
    @Autowired
    private FilesMongoRepository filesMongoRepository;

    /**
     * Migrate all elections data
     */
    @Transactional(readOnly = true)
    public void migrateAllElections(GlobalMigrationJob job) {
        log.info("Starting comprehensive elections migration");
        
        try {
            job.setCurrentPhase("Elections Migration");
            
            int batchSize = job.getBatchSize();
            int pageNumber = 0;
            AtomicLong processedCount = new AtomicLong(0);
            
            // Get total count for progress tracking
            long totalElections = electionRepository.countAllActiveElections();
            log.info("Total elections to migrate: {}", totalElections);
            
            while (!job.isCancelled() && job.isRunning()) {
                Pageable pageable = PageRequest.of(pageNumber, batchSize);
                Page<ElectionEntity> electionPage = electionRepository.findAll(pageable);
                
                List<ElectionEntity> elections = electionPage.getContent();
                
                // Check if we have no more content to process
                if (elections.isEmpty()) {
                    log.info("No more elections to migrate");
                    break;
                }
                
                for (ElectionEntity election : elections) {
                    if (job.isCancelled()) break;
                    
                    try {
                        // Convert ElectionEntity to MongoDB equivalent
                        ElectionMongo mongoElection = new ElectionMongo(election);
                        
                        if (job.isOverwriteExisting()) {
                            electionMongoRepository.save(mongoElection);
                        } else {
                            // Check if exists first
                            if (!electionMongoRepository.existsById(election.getId())) {
                                electionMongoRepository.save(mongoElection);
                            }
                        }
                        
                        processedCount.incrementAndGet();
                        
                        if (processedCount.get() % 100 == 0) {
                            log.info("Migrated {} elections", processedCount.get());
                        }
                        
                    } catch (Exception e) {
                        log.error("Error migrating election ID {}: {}", election.getId(), e.getMessage());
                        job.incrementFailedRecords(1);
                    }
                }
                
                job.incrementProcessedRecords(elections.size());
                pageNumber++;
                
                // Check if this was the last page
                if (electionPage.isLast()) {
                    log.info("Reached last page of elections");
                    break;
                }
            }
            
            job.completePhase("Elections Migration");
            log.info("Elections migration completed. Processed: {}", processedCount.get());
            
        } catch (Exception e) {
            log.error("Error during elections migration: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Migrate all volunteers data
     */
    @Transactional(readOnly = true)
    public void migrateAllVolunteers(GlobalMigrationJob job) {
        log.info("Starting comprehensive volunteers migration");
        
        try {
            job.setCurrentPhase("Volunteers Migration");
            
            int batchSize = job.getBatchSize();
            int pageNumber = 0;
            AtomicLong processedCount = new AtomicLong(0);
            
            // Get total count for progress tracking
            long totalVolunteers = volunteerRepository.count();
            log.info("Total volunteers to migrate: {}", totalVolunteers);
            
            while (!job.isCancelled() && job.isRunning()) {
                Pageable pageable = PageRequest.of(pageNumber, batchSize);
                Page<VolunteerEntity> volunteerPage = volunteerRepository.findAll(pageable);
                
                List<VolunteerEntity> volunteers = volunteerPage.getContent();
                
                // Check if we have no more content to process
                if (volunteers.isEmpty()) {
                    log.info("No more volunteers to migrate");
                    break;
                }
                
                for (VolunteerEntity volunteer : volunteers) {
                    if (job.isCancelled()) break;
                    
                    try {
                        // Convert and save to MongoDB
                        MongoVolunteer mongoVolunteer = mapVolunteerToMongo(volunteer);
                        
                        if (job.isOverwriteExisting()) {
                            mongoVolunteerRepository.save(mongoVolunteer);
                        } else {
                            // Check if exists first - use findById since we converted to string
                            if (!mongoVolunteerRepository.existsById(volunteer.getId().toString())) {
                                mongoVolunteerRepository.save(mongoVolunteer);
                            }
                        }
                        
                        processedCount.incrementAndGet();
                        
                        if (processedCount.get() % 100 == 0) {
                            log.info("Migrated {} volunteers", processedCount.get());
                        }
                        
                    } catch (Exception e) {
                        log.error("Error migrating volunteer ID {}: {}", volunteer.getId(), e.getMessage());
                        job.incrementFailedRecords(1);
                    }
                }
                
                job.incrementProcessedRecords(volunteers.size());
                pageNumber++;
                
                // Check if this was the last page
                if (volunteerPage.isLast()) {
                    log.info("Reached last page of volunteers");
                    break;
                }
                
                // Memory check every batch
                if (pageNumber % 10 == 0) {
                    checkMemoryUsage();
                }
            }
            
            job.completePhase("Volunteers Migration");
            log.info("Volunteers migration completed. Processed: {}", processedCount.get());
            
        } catch (Exception e) {
            log.error("Error during volunteers migration: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Migrate all files data
     */
    @Transactional(readOnly = true)
    public void migrateAllFiles(GlobalMigrationJob job) {
        log.info("Starting comprehensive files migration");
        
        try {
            job.setCurrentPhase("Files Migration");
            
            int batchSize = job.getBatchSize();
            int pageNumber = 0;
            AtomicLong processedCount = new AtomicLong(0);
            
            // Get total count for progress tracking
            long totalFiles = filesRepository.count();
            log.info("Total files to migrate: {}", totalFiles);
            
            while (!job.isCancelled() && job.isRunning()) {
                Pageable pageable = PageRequest.of(pageNumber, batchSize);
                Page<Files> filesPage = filesRepository.findAll(pageable);
                
                List<Files> files = filesPage.getContent();
                
                // Check if we have no more content to process
                if (files.isEmpty()) {
                    log.info("No more files to migrate");
                    break;
                }
                
                for (Files file : files) {
                    if (job.isCancelled()) break;
                    
                    try {
                        // Convert and save to MongoDB
                        FilesMongo mongoFile = new FilesMongo(file);
                        
                        if (job.isOverwriteExisting()) {
                            filesMongoRepository.save(mongoFile);
                        } else {
                            // Check if exists first
                            if (!filesMongoRepository.existsById(file.getId())) {
                                filesMongoRepository.save(mongoFile);
                            }
                        }
                        
                        processedCount.incrementAndGet();
                        
                        if (processedCount.get() % 100 == 0) {
                            log.info("Migrated {} files", processedCount.get());
                        }
                        
                    } catch (Exception e) {
                        log.error("Error migrating file ID {}: {}", file.getId(), e.getMessage());
                        job.incrementFailedRecords(1);
                    }
                }
                
                job.incrementProcessedRecords(files.size());
                pageNumber++;
                
                // Check if this was the last page
                if (filesPage.isLast()) {
                    log.info("Reached last page of files");
                    break;
                }
                
                // Memory check every batch
                if (pageNumber % 10 == 0) {
                    checkMemoryUsage();
                }
            }
            
            job.completePhase("Files Migration");
            log.info("Files migration completed. Processed: {}", processedCount.get());
            
        } catch (Exception e) {
            log.error("Error during files migration: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Migrate all entity types for a specific account
     */
    @Transactional(readOnly = true)
    public void migrateAccountEntities(GlobalMigrationJob job, Long accountId) {
        log.info("Starting comprehensive migration for account: {}", accountId);
        
        try {
            // Migrate volunteers for this account
            migrateVolunteersForAccount(job, accountId);
            
            // Migrate files for this account
            migrateFilesForAccount(job, accountId);
            
            // Migrate elections for this account
            migrateElectionsForAccount(job, accountId);
            
            // Add more entity types as needed
            
        } catch (Exception e) {
            log.error("Error during account entities migration for account {}: {}", accountId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Migrate volunteers for a specific account
     */
    @Transactional(readOnly = true)
    public void migrateVolunteersForAccount(GlobalMigrationJob job, Long accountId) {
        log.info("Migrating volunteers for account: {}", accountId);
        
        try {
            int batchSize = job.getBatchSize();
            int pageNumber = 0;
            long processedCount = 0;
            
            while (!job.isCancelled() && job.isRunning()) {
                Pageable pageable = PageRequest.of(pageNumber, batchSize);
                Page<VolunteerEntity> volunteerPage = volunteerRepository.findByAccountId(accountId, pageable);
                
                List<VolunteerEntity> volunteers = volunteerPage.getContent();
                
                // Check if we have no more content to process
                if (volunteers.isEmpty()) {
                    log.debug("No more volunteers to migrate for account: {}", accountId);
                    break;
                }
                
                // Process batch of volunteers - convert to mongo objects
                List<MongoVolunteer> mongoVolunteers = new ArrayList<>();
                for (VolunteerEntity volunteer : volunteers) {
                    if (job.isCancelled()) break;
                    
                    try {
                        MongoVolunteer mongoVolunteer = mapVolunteerToMongo(volunteer);
                        
                        if (job.isOverwriteExisting()) {
                            mongoVolunteers.add(mongoVolunteer);
                        } else {
                            if (!mongoVolunteerRepository.existsById(volunteer.getId().toString())) {
                                mongoVolunteers.add(mongoVolunteer);
                            }
                        }
                        
                        processedCount++;
                        
                    } catch (Exception e) {
                        log.error("Error preparing volunteer ID {}: {}", volunteer.getId(), e.getMessage());
                        job.incrementFailedRecords(1);
                    }
                }
                
                // Batch save to MongoDB
                if (!mongoVolunteers.isEmpty()) {
                    try {
                        mongoVolunteerRepository.saveAll(mongoVolunteers);
                        log.debug("Batch saved {} volunteers for account: {}", mongoVolunteers.size(), accountId);
                    } catch (Exception e) {
                        log.error("Error batch saving volunteers for account {}: {}", accountId, e.getMessage());
                        job.incrementFailedRecords(mongoVolunteers.size());
                    }
                }
                
                job.incrementProcessedRecords(volunteers.size());
                pageNumber++;
                
                // Check if this was the last page
                if (volunteerPage.isLast()) {
                    log.debug("Reached last page of volunteers for account: {}", accountId);
                    break;
                }
            }
            
            log.info("Completed volunteers migration for account: {}, processed: {}", accountId, processedCount);
            
        } catch (Exception e) {
            log.error("Error migrating volunteers for account {}: {}", accountId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Migrate files for a specific account
     */
    @Transactional(readOnly = true)
    public void migrateFilesForAccount(GlobalMigrationJob job, Long accountId) {
        log.info("Migrating files for account: {}", accountId);
        
        try {
            int batchSize = job.getBatchSize();
            int pageNumber = 0;
            long processedCount = 0;
            
            // First, migrate files from bulk upload
            while (!job.isCancelled() && job.isRunning()) {
                Pageable pageable = PageRequest.of(pageNumber, batchSize);
                Page<Files> filesPage = filesRepository.findByBulkUploadAccountId(accountId, pageable);
                
                List<Files> files = filesPage.getContent();
                
                if (files.isEmpty()) {
                    log.debug("No more bulk upload files to migrate for account: {}", accountId);
                    break;
                }
                
                // Process batch of files - convert to mongo objects
                List<FilesMongo> mongoFiles = new ArrayList<>();
                for (Files file : files) {
                    if (job.isCancelled()) break;
                    
                    try {
                        FilesMongo mongoFile = new FilesMongo(file);
                        
                        if (job.isOverwriteExisting()) {
                            mongoFiles.add(mongoFile);
                        } else {
                            if (!filesMongoRepository.existsById(file.getId())) {
                                mongoFiles.add(mongoFile);
                            }
                        }
                        
                        processedCount++;
                        
                    } catch (Exception e) {
                        log.error("Error preparing file ID {}: {}", file.getId(), e.getMessage());
                        job.incrementFailedRecords(1);
                    }
                }
                
                // Batch save to MongoDB
                if (!mongoFiles.isEmpty()) {
                    try {
                        filesMongoRepository.saveAll(mongoFiles);
                        log.debug("Batch saved {} files for account: {}", mongoFiles.size(), accountId);
                    } catch (Exception e) {
                        log.error("Error batch saving files for account {}: {}", accountId, e.getMessage());
                        job.incrementFailedRecords(mongoFiles.size());
                    }
                }
                
                job.incrementProcessedRecords(files.size());
                pageNumber++;
                
                if (filesPage.isLast()) {
                    log.debug("Reached last page of bulk upload files for account: {}", accountId);
                    break;
                }
            }
            
            // Second, migrate files from bulk upload member
            pageNumber = 0;
            while (!job.isCancelled() && job.isRunning()) {
                Pageable pageable = PageRequest.of(pageNumber, batchSize);
                Page<Files> filesPage = filesRepository.findByBulkUploadMemberAccountId(accountId, pageable);
                
                List<Files> files = filesPage.getContent();
                
                if (files.isEmpty()) {
                    log.debug("No more bulk upload member files to migrate for account: {}", accountId);
                    break;
                }
                
                // Process batch of files - convert to mongo objects
                List<FilesMongo> mongoFiles = new ArrayList<>();
                for (Files file : files) {
                    if (job.isCancelled()) break;
                    
                    try {
                        FilesMongo mongoFile = new FilesMongo(file);
                        
                        if (job.isOverwriteExisting()) {
                            mongoFiles.add(mongoFile);
                        } else {
                            if (!filesMongoRepository.existsById(file.getId())) {
                                mongoFiles.add(mongoFile);
                            }
                        }
                        
                        processedCount++;
                        
                    } catch (Exception e) {
                        log.error("Error preparing file ID {}: {}", file.getId(), e.getMessage());
                        job.incrementFailedRecords(1);
                    }
                }
                
                // Batch save to MongoDB
                if (!mongoFiles.isEmpty()) {
                    try {
                        filesMongoRepository.saveAll(mongoFiles);
                        log.debug("Batch saved {} member files for account: {}", mongoFiles.size(), accountId);
                    } catch (Exception e) {
                        log.error("Error batch saving member files for account {}: {}", accountId, e.getMessage());
                        job.incrementFailedRecords(mongoFiles.size());
                    }
                }
                
                job.incrementProcessedRecords(files.size());
                pageNumber++;
                
                if (filesPage.isLast()) {
                    log.debug("Reached last page of bulk upload member files for account: {}", accountId);
                    break;
                }
            }
            
            log.info("Completed files migration for account: {}, processed: {}", accountId, processedCount);
            
        } catch (Exception e) {
            log.error("Error migrating files for account {}: {}", accountId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Migrate elections for a specific account
     */
    public void migrateElectionsForAccount(GlobalMigrationJob job, Long accountId) {
        log.info("Migrating elections for account: {}", accountId);
        
        try {
            int batchSize = job.getBatchSize();
            int pageNumber = 0;
            
            while (!job.isCancelled() && job.isRunning()) {
                Pageable pageable = PageRequest.of(pageNumber, batchSize);
                Page<ElectionEntity> electionPage = electionRepository.findByAccountIdAndIsDeletedFalse(accountId, pageable);
                
                List<ElectionEntity> elections = electionPage.getContent();
                
                // Check if we have no more content to process
                if (elections.isEmpty()) {
                    log.debug("No more elections to migrate for account: {}", accountId);
                    break;
                }
                
                // Process batch of elections - convert to mongo objects
                List<ElectionMongo> mongoElections = new ArrayList<>();
                for (ElectionEntity election : elections) {
                    if (job.isCancelled()) break;
                    
                    try {
                        // Convert ElectionEntity to MongoDB equivalent
                        ElectionMongo mongoElection = new ElectionMongo(election);
                        
                        if (job.isOverwriteExisting()) {
                            mongoElections.add(mongoElection);
                        } else {
                            // Check if exists first
                            if (!electionMongoRepository.existsById(election.getId())) {
                                mongoElections.add(mongoElection);
                            }
                        }
                        
                        log.debug("Prepared election ID: {} for account: {}", election.getId(), accountId);
                        
                    } catch (Exception e) {
                        log.error("Error preparing election ID {} for account {}: {}", election.getId(), accountId, e.getMessage());
                        job.incrementFailedRecords(1);
                    }
                }
                
                // Batch save to MongoDB
                if (!mongoElections.isEmpty()) {
                    try {
                        electionMongoRepository.saveAll(mongoElections);
                        log.debug("Batch saved {} elections for account: {}", mongoElections.size(), accountId);
                    } catch (Exception e) {
                        log.error("Error batch saving elections for account {}: {}", accountId, e.getMessage());
                        job.incrementFailedRecords(mongoElections.size());
                    }
                }
                
                job.incrementProcessedRecords(elections.size());
                pageNumber++;
                
                // Check if this was the last page
                if (electionPage.isLast()) {
                    log.info("Reached last page of elections for account: {}", accountId);
                    break;
                }
            }
            
        } catch (Exception e) {
            log.error("Error during elections migration for account {}: {}", accountId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Check memory usage and log warning if high
     */
    private void checkMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        double usageRatio = (double) usedMemory / totalMemory;
        
        if (usageRatio > 0.8) {
            log.warn("High memory usage detected: {:.2f}% - Consider reducing batch size", usageRatio * 100);
            
            if (usageRatio > 0.9) {
                log.warn("Critical memory usage: {:.2f}% - Triggering GC", usageRatio * 100);
                System.gc();
            }
        }
    }
    
    /**
     * Map VolunteerEntity to MongoVolunteer
     */
    private MongoVolunteer mapVolunteerToMongo(VolunteerEntity volunteer) {
        MongoVolunteer mongoVolunteer = new MongoVolunteer();
        
        mongoVolunteer.setId(volunteer.getId() != null ? volunteer.getId().toString() : null);
        mongoVolunteer.setLastName(volunteer.getLastName());
        mongoVolunteer.setEmail(volunteer.getEmail());
        mongoVolunteer.setMobileNumber(volunteer.getMobileNumber());
        mongoVolunteer.setAssignedBooth(volunteer.getAssignedBooth());
        mongoVolunteer.setStatus(volunteer.getStatus());
        mongoVolunteer.setPhotoUrl(volunteer.getPhotoUrl());
        mongoVolunteer.setRemarks(volunteer.getRemarks());
        mongoVolunteer.setVolunteerAddress(volunteer.getVolunteerAddress());
        mongoVolunteer.setAccountId(volunteer.getAccountId());
        mongoVolunteer.setCreatedTime(volunteer.getCreatedTime());
        mongoVolunteer.setModifiedTime(volunteer.getModifiedTime());
        
        // Store only IDs instead of full objects to avoid circular references and data duplication
        if (volunteer.getUserEntity() != null) {
            // Create a minimal user object with just the ID
            UserEntity minimalUser = new UserEntity();
            minimalUser.setId(volunteer.getUserEntity().getId());
            mongoVolunteer.setUserEntity(minimalUser);
        }
        
        if (volunteer.getElectionEntity() != null) {
            // Create a minimal election object with just the ID  
            ElectionEntity minimalElection = new ElectionEntity();
            minimalElection.setId(volunteer.getElectionEntity().getId());
            mongoVolunteer.setElectionEntity(minimalElection);
        }
        
        mongoVolunteer.setWhatsAppNumber(volunteer.getWhatsAppNumber());
        mongoVolunteer.setGender(volunteer.getGender());
        mongoVolunteer.setRoleId(volunteer.getRoleId());
        
        return mongoVolunteer;
    }
}