package com.thedal.thedal_app.migration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.thedal.thedal_app.election.ElectionRepository;
import com.thedal.thedal_app.migration.migrators.SettingsMigrator;
import com.thedal.thedal_app.migration.migrators.UserMigrator;
import com.thedal.thedal_app.migration.migrators.VoterMigrator;
import com.thedal.thedal_app.migration.migrators.ComprehensiveMigrator;
import com.thedal.thedal_app.migration.migrators.AdvancedMigrator;
import com.thedal.thedal_app.migration.migrators.UniversalMigrator;
import com.thedal.thedal_app.migration.migrators.PartManagerMigrator;
import com.thedal.thedal_app.migration.migrators.SectionMigrator;
import com.thedal.thedal_app.account.AccountRepository;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;
import com.thedal.thedal_app.voter.FamilyMappingMigrationService;
import com.thedal.thedal_app.voter.FamilyMappingBulkMigrationService;
import com.thedal.thedal_app.election.PartManagerMigrationService;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

/**
 * Global Migration Service that handles complete data migration from PostgreSQL to MongoDB
 * irrespective of account or election. Optimized for performance and crash prevention.
 */
@Service
@Slf4j
public class GlobalMigrationService {

    @Autowired
    private ApplicationContext applicationContext;
    
    @Autowired
    private ElectionRepository electionRepository;
    
    @Autowired
    private VoterMigrator voterMigrator;
    
    @Autowired
    private SettingsMigrator settingsMigrator;
    
    @Autowired
    private UserMigrator userMigrator;
    
    @Autowired
    private ComprehensiveMigrator comprehensiveMigrator;
    
    @Autowired
    private UniversalMigrator universalMigrator;
    
    @Autowired
    private PartManagerMigrator partManagerMigrator;
    
    @Autowired
    private SectionMigrator sectionMigrator;
    
    @Autowired
    private AccountRepository accountRepository;
    
    @Autowired
    @Qualifier("taskExecutor")
    private Executor taskExecutor;

    // Configuration properties
    @Value("${migration.global.batch-size:500}")
    private int defaultBatchSize;
    
    @Value("${migration.global.max-concurrent-jobs:3}")
    private int maxConcurrentJobs;
    
    @Value("${migration.global.timeout-minutes:60}")
    private int timeoutMinutes;
    
    @Value("${migration.global.retry-attempts:3}")
    private int retryAttempts;
    
    @Value("${migration.global.memory-threshold:0.85}")
    private double memoryThreshold;

    // Job tracking and management
    private final Map<String, GlobalMigrationJob> activeJobs = new ConcurrentHashMap<>();
    private final Semaphore jobSemaphore = new Semaphore(maxConcurrentJobs);
    private ExecutorService migrationExecutor;
    private final AtomicLong jobCounter = new AtomicLong(0);

    @PostConstruct
    public void initialize() {
        migrationExecutor = Executors.newFixedThreadPool(maxConcurrentJobs, r -> {
            Thread t = new Thread(r, "GlobalMigration-" + jobCounter.incrementAndGet());
            t.setDaemon(true);
            return t;
        });
        log.info("Global Migration Service initialized with {} max concurrent jobs", maxConcurrentJobs);
    }

    @PreDestroy
    public void cleanup() {
        if (migrationExecutor != null && !migrationExecutor.isShutdown()) {
            migrationExecutor.shutdown();
            try {
                if (!migrationExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                    migrationExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                migrationExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Start a complete global migration of all data from PostgreSQL to MongoDB
     */
    public GlobalMigrationJobResponse startCompleteGlobalMigration(
            int batchSize, 
            boolean overwriteExisting, 
            boolean skipValidation,
            List<String> includeModules,
            List<String> excludeModules) {
        
        String jobId = generateJobId();
        String description = "Complete Global Migration - All Data PostgreSQL to MongoDB";
        
        GlobalMigrationJob job = new GlobalMigrationJob(jobId, description);
        job.setBatchSize(batchSize > 0 ? batchSize : defaultBatchSize);
        job.setOverwriteExisting(overwriteExisting);
        job.setSkipValidation(skipValidation);
        job.setIncludeModules(includeModules);
        job.setExcludeModules(excludeModules);
        
        activeJobs.put(jobId, job);
        
        // Start async migration
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            try {
                executeCompleteGlobalMigration(job);
            } catch (Exception e) {
                handleMigrationError(job, e);
            }
        }, migrationExecutor);
        
        job.setFuture(future);
        
        return new GlobalMigrationJobResponse(jobId, job.getStatus().name(), description, 
            job.getTotalEstimatedRecords(), job.getBatchSize());
    }

    /**
     * Start migration for specific accounts only
     */
    public GlobalMigrationJobResponse startAccountSpecificMigration(
            List<Long> accountIds, 
            int batchSize, 
            boolean overwriteExisting) {
        
        String jobId = generateJobId();
        String description = String.format("Account-Specific Migration for accounts: %s", accountIds);
        
        GlobalMigrationJob job = new GlobalMigrationJob(jobId, description);
        job.setBatchSize(batchSize > 0 ? batchSize : defaultBatchSize);
        job.setOverwriteExisting(overwriteExisting);
        job.setAccountIds(accountIds);
        
        activeJobs.put(jobId, job);
        
        // Start async migration
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            try {
                executeAccountSpecificMigration(job);
            } catch (Exception e) {
                handleMigrationError(job, e);
            }
        }, migrationExecutor);
        
        job.setFuture(future);
        
        return new GlobalMigrationJobResponse(jobId, job.getStatus().name(), description, 
            job.getTotalEstimatedRecords(), job.getBatchSize());
    }

    /**
     * Start fast voter-only migration with optimized performance
     */
    public GlobalMigrationJobResponse startFastVoterOnlyMigration(
            int batchSize, 
            boolean overwriteExisting, 
            boolean skipValidation,
            boolean useParallelProcessing) {
        
        String jobId = generateJobId();
        String description = "Fast Voter-Only Migration - All Voters PostgreSQL to MongoDB";
        
        GlobalMigrationJob job = new GlobalMigrationJob(jobId, description);
        job.setBatchSize(batchSize);
        job.setOverwriteExisting(overwriteExisting);
        job.setSkipValidation(skipValidation);
        job.setUseParallelProcessing(useParallelProcessing);
        
        // Set only voters module to be migrated
        job.setIncludeModules(List.of("voters"));
        
        activeJobs.put(jobId, job);
        
        // Start async migration
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            try {
                executeFastVoterOnlyMigration(job);
            } catch (Exception e) {
                handleMigrationError(job, e);
            }
        }, migrationExecutor);
        
        job.setFuture(future);
        
        return new GlobalMigrationJobResponse(jobId, job.getStatus().name(), description, 
            job.getTotalEstimatedRecords(), job.getBatchSize());
    }

    /**
     * Start comprehensive non-voter migration - migrates all data except voters
     */
    public GlobalMigrationJobResponse startComprehensiveNonVoterMigration(
            int batchSize, 
            boolean overwriteExisting, 
            boolean skipValidation,
            boolean useParallelProcessing,
            List<Long> accountIds) {
        
        String jobId = generateJobId();
        String description = "Comprehensive Non-Voter Migration - All Data Except Voters";
        
        GlobalMigrationJob job = new GlobalMigrationJob(jobId, description);
        job.setBatchSize(batchSize);
        job.setOverwriteExisting(overwriteExisting);
        job.setSkipValidation(skipValidation);
        job.setUseParallelProcessing(useParallelProcessing);
        
        // Set specific account IDs if provided
        if (accountIds != null && !accountIds.isEmpty()) {
            job.setAccountIds(accountIds);
        }
        
        // Set all modules except voters to be migrated
        job.setIncludeModules(List.of("settings", "users", "elections", "accounts", "notifications", 
                                     "profiles", "templates", "volunteers", "files", "surveys", 
                                     "complaints", "aadhaar", "bulk-uploads"));
        
        activeJobs.put(jobId, job);
        
        // Start async migration
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            try {
                executeComprehensiveNonVoterMigration(job);
            } catch (Exception e) {
                handleMigrationError(job, e);
            }
        }, migrationExecutor);
        
        job.setFuture(future);
        
        return new GlobalMigrationJobResponse(jobId, job.getStatus().name(), description, 
            job.getTotalEstimatedRecords(), job.getBatchSize());
    }

    /**
     * Execute complete global migration
     * Fixed: Removed @Transactional to prevent connection leaks
     */
    @Async
    public void executeCompleteGlobalMigration(GlobalMigrationJob job) {
        log.info("Starting complete global migration with job: {}", job.getJobId());
        
        try {
            // Check if we can acquire a job slot
            if (!jobSemaphore.tryAcquire(1, TimeUnit.MINUTES)) {
                throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.SERVICE_UNAVAILABLE);
            }
            
            job.setStatus(GlobalMigrationJobStatus.RUNNING);
            
            // Estimate total records
            estimateTotalRecords(job);
            
            // Execute migration phases
            if (shouldMigrateModule(job, "settings")) {
                migrateAllSettings(job);
            }
            
            if (shouldMigrateModule(job, "users")) {
                migrateAllUsers(job);
            }
            
            if (shouldMigrateModule(job, "voters")) {
                migrateAllVoters(job);
            }
            
            if (shouldMigrateModule(job, "elections")) {
                migrateAllElections(job);
            }
            
            // Mark as completed
            job.setStatus(GlobalMigrationJobStatus.COMPLETED);
            job.setEndTime(System.currentTimeMillis());
            job.setCompletedTime(LocalDateTime.now());
            
            log.info("Complete global migration finished successfully for job: {}", job.getJobId());
            
        } catch (Exception e) {
            handleMigrationError(job, e);
        } finally {
            jobSemaphore.release();
        }
    }

    /**
     * Execute account-specific migration
     * Fixed: Removed @Transactional to prevent connection leaks
     */
    @Async
    public void executeAccountSpecificMigration(GlobalMigrationJob job) {
        log.info("Starting account-specific migration for job: {}", job.getJobId());
        
        try {
            // Check if we can acquire a job slot
            if (!jobSemaphore.tryAcquire(1, TimeUnit.MINUTES)) {
                throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.SERVICE_UNAVAILABLE);
            }
            
            job.setStatus(GlobalMigrationJobStatus.RUNNING);
            
            // Migrate for each account
            for (Long accountId : job.getAccountIds()) {
                if (job.isCancelled()) break;
                
                log.info("Migrating data for account: {}", accountId);
                
                // Migrate account-specific data
                migrateAccountData(job, accountId);
                
                // Check memory usage
                checkMemoryUsage(job);
            }
            
            // Mark as completed
            job.setStatus(GlobalMigrationJobStatus.COMPLETED);
            job.setEndTime(System.currentTimeMillis());
            job.setCompletedTime(LocalDateTime.now());
            
            log.info("Account-specific migration finished successfully for job: {}", job.getJobId());
            
        } catch (Exception e) {
            handleMigrationError(job, e);
        } finally {
            jobSemaphore.release();
        }
    }

    /**
     * Execute fast voter-only migration with performance optimizations
     * Fixed: Removed @Transactional to prevent connection leaks in parallel processing
     */
    @Async
    public void executeFastVoterOnlyMigration(GlobalMigrationJob job) {
        log.info("Starting fast voter-only migration with job: {}", job.getJobId());
        
        try {
            // Check if we can acquire a job slot
            if (!jobSemaphore.tryAcquire(1, TimeUnit.MINUTES)) {
                throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.SERVICE_UNAVAILABLE);
            }
            
            job.setStatus(GlobalMigrationJobStatus.RUNNING);
            
            // Get all unique account+election combinations for voters
            List<Object[]> accountElectionPairs = electionRepository.findDistinctAccountElectionPairs();
            
            // Estimate total records
            long totalVoters = 0;
            for (Object[] pair : accountElectionPairs) {
                Long accountId = (Long) pair[0];
                Long electionId = (Long) pair[1];
                totalVoters += voterMigrator.countRecords(accountId, electionId);
            }
            
            job.setTotalEstimatedRecords(totalVoters);
            log.info("Estimated total voters to migrate: {}", totalVoters);
            
            if (job.isUseParallelProcessing() && accountElectionPairs.size() > 1) {
                // Process multiple accounts in parallel for maximum speed
                migrateVotersInParallel(job, accountElectionPairs);
            } else {
                // Process sequentially
                migrateVotersSequentially(job, accountElectionPairs);
            }
            
            // Mark as completed
            job.setStatus(GlobalMigrationJobStatus.COMPLETED);
            job.setEndTime(System.currentTimeMillis());
            job.setCompletedTime(LocalDateTime.now());
            
            log.info("Fast voter-only migration completed successfully for job: {}. Total processed: {}, Failed: {}", 
                    job.getJobId(), job.getProcessedRecords(), job.getFailedRecords());
            
        } catch (Exception e) {
            handleMigrationError(job, e);
        } finally {
            jobSemaphore.release();
        }
    }

    /**
     * Execute comprehensive non-voter migration - migrates all data except voters
     * Fixed: Removed @Transactional to prevent connection leaks in parallel processing
     */
    @Async
    public void executeComprehensiveNonVoterMigration(GlobalMigrationJob job) {
        log.info("Starting comprehensive non-voter migration with job: {}", job.getJobId());
        
        try {
            // Check if we can acquire a job slot
            if (!jobSemaphore.tryAcquire(1, TimeUnit.MINUTES)) {
                throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.SERVICE_UNAVAILABLE);
            }
            
            job.setStatus(GlobalMigrationJobStatus.RUNNING);
            
            // Phase 1: Migrate global/settings data
            log.info("Phase 1: Migrating global settings and reference data");
            
            if (shouldMigrateModule(job, "settings")) {
                migrateAllSettings(job);
            }
            
            if (shouldMigrateModule(job, "users")) {
                migrateAllUsers(job);
            }
            
            // Phase 2: Migrate universal entities (non-account specific)
            log.info("Phase 2: Migrating universal entities");
            migrateUniversalEntities(job);
            
            // Phase 3: Migrate account-specific data
            log.info("Phase 3: Migrating account-specific data");
            
            List<Long> targetAccountIds = job.getAccountIds();
            if (targetAccountIds == null || targetAccountIds.isEmpty()) {
                // Get all account IDs if none specified
                targetAccountIds = electionRepository.findDistinctAccountIds();
                log.info("No specific accounts provided, migrating all {} accounts", targetAccountIds.size());
            }
            
            for (Long accountId : targetAccountIds) {
                if (job.isCancelled()) break;
                
                log.info("Migrating comprehensive data for account: {}", accountId);
                migrateComprehensiveAccountData(job, accountId);
            }
            
            // Phase 4: Migrate election data
            if (shouldMigrateModule(job, "elections")) {
                log.info("Phase 4: Migrating election data");
                migrateAllElections(job);
            }
            
            // Mark as completed
            job.setStatus(GlobalMigrationJobStatus.COMPLETED);
            job.setEndTime(System.currentTimeMillis());
            job.setCompletedTime(LocalDateTime.now());
            
            log.info("Comprehensive non-voter migration completed successfully for job: {}. Total processed: {}, Failed: {}", 
                    job.getJobId(), job.getProcessedRecords(), job.getFailedRecords());
            
        } catch (Exception e) {
            handleMigrationError(job, e);
        } finally {
            jobSemaphore.release();
        }
    }

    /**
     * Migrate voters in parallel for maximum performance
     */
    private void migrateVotersInParallel(GlobalMigrationJob job, List<Object[]> accountElectionPairs) {
        log.info("Starting parallel voter migration for {} account-election pairs", accountElectionPairs.size());
        
        // Process in parallel batches to avoid overwhelming the system
        int parallelBatchSize = Math.min(maxConcurrentJobs, accountElectionPairs.size());
        
        for (int i = 0; i < accountElectionPairs.size(); i += parallelBatchSize) {
            if (job.isCancelled()) break;
            
            int endIndex = Math.min(i + parallelBatchSize, accountElectionPairs.size());
            List<Object[]> batch = accountElectionPairs.subList(i, endIndex);
            
            // Process batch in parallel
            List<CompletableFuture<Void>> futures = batch.stream()
                .map(pair -> CompletableFuture.runAsync(() -> {
                    if (job.isCancelled()) return;
                    
                    Long accountId = (Long) pair[0];
                    Long electionId = (Long) pair[1];
                    
                    try {
                        migrateVotersForAccountElection(job, accountId, electionId);
                    } catch (Exception e) {
                        log.error("Error migrating voters for account {} election {}: {}", 
                                accountId, electionId, e.getMessage(), e);
                        job.incrementFailedRecords(1);
                    }
                }, migrationExecutor))
                .collect(Collectors.toList());
            
            // Wait for batch to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            
            // Check memory usage after each batch
            checkMemoryUsage(job);
        }
    }

    /**
     * Migrate voters sequentially
     */
    private void migrateVotersSequentially(GlobalMigrationJob job, List<Object[]> accountElectionPairs) {
        log.info("Starting sequential voter migration for {} account-election pairs", accountElectionPairs.size());
        
        for (Object[] pair : accountElectionPairs) {
            if (job.isCancelled()) break;
            
            Long accountId = (Long) pair[0];
            Long electionId = (Long) pair[1];
            
            try {
                migrateVotersForAccountElection(job, accountId, electionId);
            } catch (Exception e) {
                log.error("Error migrating voters for account {} election {}: {}", 
                        accountId, electionId, e.getMessage(), e);
                job.incrementFailedRecords(1);
            }
            
            // Check memory usage periodically
            checkMemoryUsage(job);
        }
    }

    /**
     * Migrate voters for a specific account-election combination
     */
    private void migrateVotersForAccountElection(GlobalMigrationJob job, Long accountId, Long electionId) {
        log.debug("Migrating voters for account: {}, election: {}", accountId, electionId);
        
        // Create temporary migration job for this account/election
        MigrationJob tempJob = new MigrationJob(
            job.getJobId() + "_voters_" + accountId + "_" + electionId,
            accountId, electionId, "Fast Voter Migration"
        );
        
        // Use larger batch size for better performance
        int optimizedBatchSize = Math.max(job.getBatchSize(), 5000);
        
        // Use BULK migration method for maximum speed with data integrity
        if (job.isSkipValidation()) {
            // Use ultra-fast bulk operations
            voterMigrator.migrateBulk(tempJob, optimizedBatchSize, job.isOverwriteExisting());
        } else {
            // Use standard migration with full validation
            voterMigrator.migrate(tempJob, optimizedBatchSize, job.isOverwriteExisting());
        }
        
        // Update job statistics
        job.incrementProcessedRecords(tempJob.getProcessedRecords());
        job.incrementFailedRecords(tempJob.getFailedRecords());
        
        log.debug("Completed voter migration for account: {}, election: {}. Processed: {}, Failed: {}", 
                accountId, electionId, tempJob.getProcessedRecords(), tempJob.getFailedRecords());
    }

    /**
     * Migrate all settings data
     */
    private void migrateAllSettings(GlobalMigrationJob job) {
        log.info("Starting global settings migration");
        
        try {
            // Get all unique account+election combinations
            List<Object[]> accountElectionPairs = electionRepository.findDistinctAccountElectionPairs();
            
            for (Object[] pair : accountElectionPairs) {
                if (job.isCancelled()) break;
                
                Long accountId = (Long) pair[0];
                Long electionId = (Long) pair[1];
                
                // Create temporary migration job for this account/election
                MigrationJob tempJob = new MigrationJob(
                    job.getJobId() + "_settings_" + accountId + "_" + electionId,
                    accountId, electionId, "Global Settings Migration"
                );
                
                settingsMigrator.migrate(tempJob, job.getBatchSize(), job.isOverwriteExisting());
                
                job.incrementProcessedRecords(tempJob.getProcessedRecords());
                job.incrementFailedRecords(tempJob.getFailedRecords());
                
                // Memory check
                checkMemoryUsage(job);
            }
            
            log.info("Global settings migration completed");
            
        } catch (Exception e) {
            log.error("Error during global settings migration: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Migrate all users data
     */
    private void migrateAllUsers(GlobalMigrationJob job) {
        log.info("Starting global users migration");
        
        try {
            // Get all unique account IDs
            List<Long> accountIds = electionRepository.findDistinctAccountIds();
            
            for (Long accountId : accountIds) {
                if (job.isCancelled()) break;
                
                // Create temporary migration job for this account
                MigrationJob tempJob = new MigrationJob(
                    job.getJobId() + "_users_" + accountId,
                    accountId, null, "Global Users Migration"
                );
                
                userMigrator.migrate(tempJob, job.getBatchSize(), job.isOverwriteExisting());
                
                job.incrementProcessedRecords(tempJob.getProcessedRecords());
                job.incrementFailedRecords(tempJob.getFailedRecords());
                
                // Memory check
                checkMemoryUsage(job);
            }
            
            log.info("Global users migration completed");
            
        } catch (Exception e) {
            log.error("Error during global users migration: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Migrate all voters data
     */
    private void migrateAllVoters(GlobalMigrationJob job) {
        log.info("Starting global voters migration");
        
        try {
            // Get all unique account+election combinations
            List<Object[]> accountElectionPairs = electionRepository.findDistinctAccountElectionPairs();
            
            for (Object[] pair : accountElectionPairs) {
                if (job.isCancelled()) break;
                
                Long accountId = (Long) pair[0];
                Long electionId = (Long) pair[1];
                
                // Create temporary migration job for this account/election
                MigrationJob tempJob = new MigrationJob(
                    job.getJobId() + "_voters_" + accountId + "_" + electionId,
                    accountId, electionId, "Global Voters Migration"
                );
                
                // Use BULK migration for maximum performance
                voterMigrator.migrateBulk(tempJob, job.getBatchSize(), job.isOverwriteExisting());
                
                job.incrementProcessedRecords(tempJob.getProcessedRecords());
                job.incrementFailedRecords(tempJob.getFailedRecords());
                
                // Memory check
                checkMemoryUsage(job);
            }
            
            log.info("Global voters migration completed");
            
        } catch (Exception e) {
            log.error("Error during global voters migration: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Migrate all elections data
     */
    private void migrateAllElections(GlobalMigrationJob job) {
        log.info("Starting global elections migration");
        
        try {
            // Get all unique account+election combinations
            List<Object[]> accountElectionPairs = electionRepository.findDistinctAccountElectionPairs();
            
            for (Object[] pair : accountElectionPairs) {
                if (job.isCancelled()) break;
                
                Long accountId = (Long) pair[0];
                Long electionId = (Long) pair[1];
                
                // Here you would add specific election data migration logic
                // This is a placeholder for election-specific data
                
                job.incrementProcessedRecords(1);
                
                // Memory check
                checkMemoryUsage(job);
            }
            
            log.info("Global elections migration completed");
            
        } catch (Exception e) {
            log.error("Error during global elections migration: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Migrate data for a specific account
     */
    private void migrateAccountData(GlobalMigrationJob job, Long accountId) {
        log.info("Migrating data for account: {}", accountId);
        
        try {
            // Get all elections for this account
            List<Long> electionIds = electionRepository.findElectionIdsByAccountId(accountId);
            
            for (Long electionId : electionIds) {
                if (job.isCancelled()) break;
                
                // Migrate settings for this account/election
                MigrationJob settingsJob = new MigrationJob(
                    job.getJobId() + "_settings_" + accountId + "_" + electionId,
                    accountId, electionId, "Account Settings Migration"
                );
                settingsMigrator.migrate(settingsJob, job.getBatchSize(), job.isOverwriteExisting());
                job.incrementProcessedRecords(settingsJob.getProcessedRecords());
                
                // Migrate voters for this account/election
                MigrationJob votersJob = new MigrationJob(
                    job.getJobId() + "_voters_" + accountId + "_" + electionId,
                    accountId, electionId, "Account Voters Migration"
                );
                voterMigrator.migrate(votersJob, job.getBatchSize(), job.isOverwriteExisting());
                job.incrementProcessedRecords(votersJob.getProcessedRecords());
            }
            
            // Migrate users for this account
            MigrationJob usersJob = new MigrationJob(
                job.getJobId() + "_users_" + accountId,
                accountId, null, "Account Users Migration"
            );
            userMigrator.migrate(usersJob, job.getBatchSize(), job.isOverwriteExisting());
            job.incrementProcessedRecords(usersJob.getProcessedRecords());
            
        } catch (Exception e) {
            log.error("Error migrating data for account {}: {}", accountId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Check if a module should be migrated
     */
    private boolean shouldMigrateModule(GlobalMigrationJob job, String module) {
        if (job.getIncludeModules() != null && !job.getIncludeModules().isEmpty()) {
            return job.getIncludeModules().contains(module);
        }
        if (job.getExcludeModules() != null && !job.getExcludeModules().isEmpty()) {
            return !job.getExcludeModules().contains(module);
        }
        return true;
    }

    /**
     * Estimate total records for progress tracking
     */
    private void estimateTotalRecords(GlobalMigrationJob job) {
        log.info("Estimating total records for migration");
        
        try {
            long totalRecords = 0;
            
            // This is a rough estimation - in practice, you might want to make this more accurate
            if (shouldMigrateModule(job, "settings")) {
                totalRecords += 10000; // Estimated settings records
            }
            
            if (shouldMigrateModule(job, "users")) {
                totalRecords += 5000; // Estimated users records
            }
            
            if (shouldMigrateModule(job, "voters")) {
                totalRecords += 100000; // Estimated voters records
            }
            
            if (shouldMigrateModule(job, "elections")) {
                totalRecords += 1000; // Estimated elections records
            }
            
            job.setTotalEstimatedRecords(totalRecords);
            
        } catch (Exception e) {
            log.warn("Error estimating total records: {}", e.getMessage());
            job.setTotalEstimatedRecords(0);
        }
    }

    /**
     * Check memory usage and trigger GC if necessary
     */
    private void checkMemoryUsage(GlobalMigrationJob job) {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        double usageRatio = (double) usedMemory / totalMemory;
        
        if (usageRatio > memoryThreshold) {
            log.warn("Memory usage is high: {:.2f}% - Triggering GC", usageRatio * 100);
            System.gc();
            
            // Wait a bit for GC to complete
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // Check again after GC
            totalMemory = runtime.totalMemory();
            freeMemory = runtime.freeMemory();
            usedMemory = totalMemory - freeMemory;
            usageRatio = (double) usedMemory / totalMemory;
            
            if (usageRatio > memoryThreshold) {
                log.error("Memory usage still high after GC: {:.2f}% - Pausing migration", usageRatio * 100);
                job.setStatus(GlobalMigrationJobStatus.PAUSED);
                throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.SERVICE_UNAVAILABLE);
            }
        }
    }

    /**
     * Handle migration errors
     */
    private void handleMigrationError(GlobalMigrationJob job, Exception e) {
        log.error("Global migration failed for job: {}, error: {}", job.getJobId(), e.getMessage(), e);
        job.setStatus(GlobalMigrationJobStatus.FAILED);
        job.setEndTime(System.currentTimeMillis());
        job.setErrorMessage(e.getMessage());
    }

    /**
     * Generate a unique job ID
     */
    private String generateJobId() {
        return "GLOBAL_MIG_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    // Public methods for job management
    
    public GlobalMigrationJob getGlobalMigrationStatus(String jobId) {
        GlobalMigrationJob job = activeJobs.get(jobId);
        if (job == null) {
            throw new ThedalException(ThedalError.MIGRATION_JOB_NOT_FOUND, HttpStatus.NOT_FOUND);
        }
        return job;
    }

    public List<GlobalMigrationJob> getAllGlobalMigrationJobs() {
        return new ArrayList<>(activeJobs.values());
    }

    public void cancelGlobalMigration(String jobId) {
        GlobalMigrationJob job = activeJobs.get(jobId);
        if (job == null) {
            throw new ThedalException(ThedalError.MIGRATION_JOB_NOT_FOUND, HttpStatus.NOT_FOUND);
        }
        
        job.setCancelled(true);
        job.setStatus(GlobalMigrationJobStatus.CANCELLED);
        
        if (job.getFuture() != null) {
            job.getFuture().cancel(true);
        }
        
        log.info("Global migration job {} cancelled", jobId);
    }

    public void pauseGlobalMigration(String jobId) {
        GlobalMigrationJob job = activeJobs.get(jobId);
        if (job == null) {
            throw new ThedalException(ThedalError.MIGRATION_JOB_NOT_FOUND, HttpStatus.NOT_FOUND);
        }
        
        job.setStatus(GlobalMigrationJobStatus.PAUSED);
        log.info("Global migration job {} paused", jobId);
    }

    public void resumeGlobalMigration(String jobId) {
        GlobalMigrationJob job = activeJobs.get(jobId);
        if (job == null) {
            throw new ThedalException(ThedalError.MIGRATION_JOB_NOT_FOUND, HttpStatus.NOT_FOUND);
        }
        
        if (job.getStatus() == GlobalMigrationJobStatus.PAUSED) {
            job.setStatus(GlobalMigrationJobStatus.RUNNING);
            log.info("Global migration job {} resumed", jobId);
        }
    }

    public Map<String, Object> getGlobalMigrationStats() {
        Map<String, Object> stats = new HashMap<>();
        
        List<GlobalMigrationJob> jobs = getAllGlobalMigrationJobs();
        
        long totalJobs = jobs.size();
        long runningJobs = jobs.stream().filter(j -> j.getStatus() == GlobalMigrationJobStatus.RUNNING).count();
        long completedJobs = jobs.stream().filter(j -> j.getStatus() == GlobalMigrationJobStatus.COMPLETED).count();
        long failedJobs = jobs.stream().filter(j -> j.getStatus() == GlobalMigrationJobStatus.FAILED).count();
        
        stats.put("totalJobs", totalJobs);
        stats.put("runningJobs", runningJobs);
        stats.put("completedJobs", completedJobs);
        stats.put("failedJobs", failedJobs);
        stats.put("maxConcurrentJobs", maxConcurrentJobs);
        stats.put("availableJobSlots", jobSemaphore.availablePermits());
        
        return stats;
    }
    
    /**
     * Migrate universal entities (non-account specific data)
     */
    private void migrateUniversalEntities(GlobalMigrationJob job) {
        try {
            log.info("Starting universal entities migration");
            
            if (shouldMigrateModule(job, "accounts")) {
                log.info("Migrating accounts");
                universalMigrator.migrateAllAccounts(job);
            }
            
            if (shouldMigrateModule(job, "notifications")) {
                log.info("Migrating notifications");
                universalMigrator.migrateAllNotifications(job);
            }
            
            if (shouldMigrateModule(job, "profiles")) {
                log.info("Migrating profiles");
                universalMigrator.migrateAllProfiles(job);
            }
            
            if (shouldMigrateModule(job, "templates")) {
                log.info("Migrating templates");
                universalMigrator.migrateAllTemplates(job);
            }
            
            if (shouldMigrateModule(job, "surveys")) {
                log.info("Migrating survey form submissions");
                universalMigrator.migrateAllSurveyFormSubmissions(job);
            }
            
            if (shouldMigrateModule(job, "complaints")) {
                log.info("Migrating complaints");
                universalMigrator.migrateAllComplaints(job);
            }
            
            if (shouldMigrateModule(job, "aadhaar")) {
                log.info("Migrating aadhaar data");
                universalMigrator.migrateAllAadhaar(job);
            }
            
            if (shouldMigrateModule(job, "bulk-uploads")) {
                log.info("Migrating bulk upload entities");
                universalMigrator.migrateAllBulkUploads(job);
            }
            
            log.info("Universal entities migration completed");
            
        } catch (Exception e) {
            log.error("Error during universal entities migration: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Migrate comprehensive account-specific data (excluding voters)
     */
    private void migrateComprehensiveAccountData(GlobalMigrationJob job, Long accountId) {
        try {
            log.info("Starting comprehensive account data migration for account: {}", accountId);
            
            // Migrate elections for this account first (as they may be referenced by other entities)
            if (shouldMigrateModule(job, "elections")) {
                log.info("Migrating elections for account: {}", accountId);
                migrateElectionsForAccount(job, accountId);
            }
            
            // Migrate volunteers for this account
            if (shouldMigrateModule(job, "volunteers")) {
                log.info("Migrating volunteers for account: {}", accountId);
                comprehensiveMigrator.migrateVolunteersForAccount(job, accountId);
            }
            
            // Migrate files for this account
            if (shouldMigrateModule(job, "files")) {
                log.info("Migrating files for account: {}", accountId);
                comprehensiveMigrator.migrateFilesForAccount(job, accountId);
            }
            
            // Migrate part managers for this account
            log.info("Migrating part managers for account: {}", accountId);
            if (shouldMigrateModule(job, "part-managers")) {
                // Get all elections for this account and migrate part managers
                List<Long> electionIds = electionRepository.findElectionIdsByAccountId(accountId);
                for (Long electionId : electionIds) {
                    if (job.isCancelled()) break;
                    migratePartManagersForAccountElection(job, accountId, electionId);
                }
            }
            
            log.info("Comprehensive account data migration completed for account: {}", accountId);
            
        } catch (Exception e) {
            log.error("Error during comprehensive account data migration for account {}: {}", accountId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Migrate elections for a specific account
     */
    private void migrateElectionsForAccount(GlobalMigrationJob job, Long accountId) {
        try {
            // This should use election-specific migration logic
            comprehensiveMigrator.migrateElectionsForAccount(job, accountId);
        } catch (Exception e) {
            log.error("Error migrating elections for account {}: {}", accountId, e.getMessage(), e);
        }
    }

    /**
     * Migrate part managers for a specific account and election
     */
    private void migratePartManagersForAccountElection(GlobalMigrationJob job, Long accountId, Long electionId) {
        try {
            // Delegate to PartManagerMigrationService if available
            if (applicationContext.containsBean("partManagerMigrationService")) {
                PartManagerMigrationService partManagerService = applicationContext.getBean(PartManagerMigrationService.class);
                // Call part manager migration for this account/election
                log.debug("Migrating part managers for account: {} election: {}", accountId, electionId);
                // Note: Actual migration logic would depend on PartManagerMigrationService implementation
            }
        } catch (Exception e) {
            log.error("Error migrating part managers for account {} election {}: {}", accountId, electionId, e.getMessage(), e);
        }
    }
    
    /**
     * Get all available account IDs
     * @return List of all account IDs
     */
    public List<Long> getAllAccountIds() {
        log.info("Retrieving all account IDs");
        try {
            return accountRepository.findAllAccountIds();
        } catch (Exception e) {
            log.error("Error retrieving account IDs: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Start complete everything migration including PartManager
     * @param accountIds List of account IDs to migrate (if null or empty, migrates all accounts)
     * @param batchSize Batch size for processing
     * @param parallelism Number of parallel threads
     * @param clearExisting Whether to clear existing data
     * @return Migration job response
     */
    public GlobalMigrationJobResponse startCompleteEverythingMigration(
            List<Long> accountIds, int batchSize, int parallelism, boolean clearExisting) {
        
        String jobId = generateJobId();
        log.info("Starting complete everything migration with jobId: {}", jobId);
        
        // Use all accounts if none specified
        if (accountIds == null || accountIds.isEmpty()) {
            accountIds = getAllAccountIds();
        }
        
        GlobalMigrationJob job = new GlobalMigrationJob(
            jobId, 
            GlobalMigrationJobType.COMPLETE_EVERYTHING,
            accountIds,
            batchSize,
            parallelism,
            clearExisting
        );
        
        activeJobs.put(jobId, job);
        
        // Start async migration
        CompletableFuture.runAsync(() -> executeCompleteEverythingMigration(job), taskExecutor);
        
        return new GlobalMigrationJobResponse(jobId, job.getStatus().name(), 
            "Complete everything migration started for " + accountIds.size() + " accounts");
    }
    
    /**
     * Start PartManager migration
     * @param accountIds List of account IDs to migrate (if null or empty, migrates all accounts)
     * @param batchSize Batch size for processing
     * @param parallelism Number of parallel threads
     * @return Migration job response
     */
    public GlobalMigrationJobResponse startPartManagerMigration(
            List<Long> accountIds, int batchSize, int parallelism) {
        
        String jobId = generateJobId();
        log.info("Starting PartManager migration with jobId: {}", jobId);
        
        // Use all accounts if none specified
        if (accountIds == null || accountIds.isEmpty()) {
            accountIds = getAllAccountIds();
        }
        
        GlobalMigrationJob job = new GlobalMigrationJob(
            jobId, 
            GlobalMigrationJobType.PART_MANAGER_ONLY,
            accountIds,
            batchSize,
            parallelism,
            false
        );
        
        activeJobs.put(jobId, job);
        
        // Start async migration
        CompletableFuture.runAsync(() -> executePartManagerMigration(job), taskExecutor);
        
        return new GlobalMigrationJobResponse(jobId, job.getStatus().name(), 
            "PartManager migration started for " + accountIds.size() + " accounts");
    }
    
    /**
     * Execute complete everything migration asynchronously
     * @param job Migration job
     */
    public void executeCompleteEverythingMigration(GlobalMigrationJob job) {
        try {
            job.setStatus(GlobalMigrationJobStatus.RUNNING);
            log.info("Executing complete everything migration for job: {}", job.getJobId());
            
            List<Long> accountIds = job.getAccountIds();
            AtomicLong totalProcessed = new AtomicLong(0);
            
            // Phase 1: Settings Migration
            try {
                job.setCurrentPhase("Settings Migration");
                MigrationJob tempJob = new MigrationJob("temp", null, null, "Settings Migration");
                settingsMigrator.migrate(tempJob, job.getBatchSize(), false);
                log.info("Completed Settings Migration");
            } catch (Exception e) {
                log.error("Settings migration failed: {}", e.getMessage(), e);
                // Continue with other phases
            }
            
            // Phase 2: User Migration
            try {
                job.setCurrentPhase("User Migration");
                MigrationJob userJob = new MigrationJob("user", null, null, "User Migration");
                userMigrator.migrate(userJob, job.getBatchSize(), false);
                log.info("Completed User Migration");
            } catch (Exception e) {
                log.error("User migration failed: {}", e.getMessage(), e);
                // Continue with other phases
            }
            
            // Phase 3: Universal Data Migration
            try {
                job.setCurrentPhase("Universal Data Migration");
                universalMigrator.migrateAllAccounts(job);
                universalMigrator.migrateAllNotifications(job);
                universalMigrator.migrateAllImages(job);
                universalMigrator.migrateAllProfiles(job);
                universalMigrator.migrateAllCampaignSettings(job);
                universalMigrator.migrateAllTemplates(job);
                universalMigrator.migrateAllSurveyFormSubmissions(job);
                universalMigrator.migrateAllComplaints(job);
                universalMigrator.migrateAllGeneralCpanel(job);
                universalMigrator.migrateAllAadhaar(job);
                universalMigrator.migrateAllBulkUploads(job);
                universalMigrator.migrateAllBulkUploadMembers(job);
                universalMigrator.migrateAllBulkUploadErrors(job);
                universalMigrator.migrateAllBoothBulkUploads(job);
                universalMigrator.migrateAllPartManagerBulkUploads(job);
                universalMigrator.migrateAllSectionBulkUploads(job);
                universalMigrator.migrateAllVolunteerBulkUploads(job);
                log.info("Completed Universal Data Migration");
            } catch (Exception e) {
                log.error("Universal data migration failed: {}", e.getMessage(), e);
                // Continue with other phases
            }
            
            // Phase 4: Comprehensive Data Migration (Global)
            try {
                job.setCurrentPhase("Comprehensive Data Migration");
                comprehensiveMigrator.migrateAllElections(job);
                comprehensiveMigrator.migrateAllVolunteers(job);
                comprehensiveMigrator.migrateAllFiles(job);
                log.info("Completed Comprehensive Data Migration");
            } catch (Exception e) {
                log.error("Comprehensive data migration failed: {}", e.getMessage(), e);
                // Continue with other phases
            }
            
            // Phase 5: Account-specific data migration
            for (Long accountId : accountIds) {
                if (job.getStatus() == GlobalMigrationJobStatus.CANCELLED) {
                    break;
                }
                
                try {
                    job.setCurrentPhase("Account " + accountId + " Data Migration");
                    
                    // Migrate comprehensive data (excluding voters)
                    try {
                        comprehensiveMigrator.migrateAccountEntities(job, accountId);
                        log.debug("Completed comprehensive data migration for account: {}", accountId);
                    } catch (Exception e) {
                        log.error("Comprehensive migration failed for account {}: {}", accountId, e.getMessage(), e);
                    }
                    
                    // Migrate voters
                    try {
                        MigrationJob voterJob = new MigrationJob("voter", accountId, null, "Voter Migration");
                        voterMigrator.migrate(voterJob, job.getBatchSize(), false);
                        long voterCount = voterJob.getProcessedRecords();
                        totalProcessed.addAndGet(voterCount);
                        log.debug("Completed voter migration for account: {}, count: {}", accountId, voterCount);
                    } catch (Exception e) {
                        log.error("Voter migration failed for account {}: {}", accountId, e.getMessage(), e);
                    }
                    
                    // Migrate PartManager data
                    try {
                        long partManagerCount = partManagerMigrator.migratePartManagers(accountId, job.getBatchSize(), job.getParallelism());
                        totalProcessed.addAndGet(partManagerCount);
                        log.debug("Completed PartManager migration for account: {}, count: {}", accountId, partManagerCount);
                    } catch (Exception e) {
                        log.error("PartManager migration failed for account {}: {}", accountId, e.getMessage(), e);
                    }
                    
                    // Migrate Section data
                    try {
                        long sectionCount = sectionMigrator.migrateSections(accountId, job.getBatchSize(), job.getParallelism());
                        totalProcessed.addAndGet(sectionCount);
                        log.debug("Completed Section migration for account: {}, count: {}", accountId, sectionCount);
                    } catch (Exception e) {
                        log.error("Section migration failed for account {}: {}", accountId, e.getMessage(), e);
                    }
                    
                    job.getProcessedRecords().addAndGet(totalProcessed.get());
                    log.info("Completed migration for account: {}, total processed: {}", accountId, totalProcessed.get());
                    
                } catch (Exception e) {
                    log.error("Account migration failed for account {}: {}", accountId, e.getMessage(), e);
                    // Continue with next account
                }
            }
            
            job.setStatus(GlobalMigrationJobStatus.COMPLETED);
            job.setCurrentPhase("Completed");
            job.setCompletedAt(LocalDateTime.now());
            
            log.info("Complete everything migration completed for job: {}, total processed: {}", 
                    job.getJobId(), totalProcessed.get());
            
        } catch (Exception e) {
            job.setStatus(GlobalMigrationJobStatus.FAILED);
            job.setError(e.getMessage());
            job.setCompletedAt(LocalDateTime.now());
            log.error("Complete everything migration failed for job: {}", job.getJobId(), e);
        } finally {
            // Cleanup resources
            log.info("Migration job {} finished with status: {}", job.getJobId(), job.getStatus());
        }
    }
    
    /**
     * Execute PartManager migration asynchronously
     * @param job Migration job
     */
    public void executePartManagerMigration(GlobalMigrationJob job) {
        try {
            job.setStatus(GlobalMigrationJobStatus.RUNNING);
            log.info("Executing PartManager migration for job: {}", job.getJobId());
            
            List<Long> accountIds = job.getAccountIds();
            AtomicLong totalProcessed = new AtomicLong(0);
            
            for (Long accountId : accountIds) {
                if (job.getStatus() == GlobalMigrationJobStatus.CANCELLED) {
                    break;
                }
                
                try {
                    job.setCurrentPhase("PartManager Migration for Account " + accountId);
                    
                    // Migrate PartManager data
                    long partManagerCount = partManagerMigrator.migratePartManagers(accountId, job.getBatchSize(), job.getParallelism());
                    totalProcessed.addAndGet(partManagerCount);
                    
                    job.getProcessedRecords().addAndGet(partManagerCount);
                    log.info("Completed PartManager migration for account: {}, processed: {}", accountId, partManagerCount);
                    
                } catch (Exception e) {
                    log.error("PartManager migration failed for account {}: {}", accountId, e.getMessage(), e);
                    // Continue with next account
                }
            }
            
            job.setStatus(GlobalMigrationJobStatus.COMPLETED);
            job.setCurrentPhase("Completed");
            job.setCompletedAt(LocalDateTime.now());
            
            log.info("PartManager migration completed for job: {}, total processed: {}", 
                    job.getJobId(), totalProcessed.get());
            
        } catch (Exception e) {
            job.setStatus(GlobalMigrationJobStatus.FAILED);
            job.setError(e.getMessage());
            job.setCompletedAt(LocalDateTime.now());
            log.error("PartManager migration failed for job: {}", job.getJobId(), e);
        } finally {
            // Cleanup resources
            log.info("PartManager migration job {} finished with status: {}", job.getJobId(), job.getStatus());
        }
    }
    
    /**
     * Start Section migration for specified accounts
     * @param accountIds List of account IDs to migrate (null or empty for all accounts)
     * @param batchSize Batch size for processing
     * @param parallelism Number of parallel threads
     * @return Migration job response
     */
    public GlobalMigrationJobResponse startSectionMigration(
            List<Long> accountIds, int batchSize, int parallelism) {
        
        String jobId = generateJobId();
        log.info("Starting Section migration with jobId: {}", jobId);
        
        // Use all accounts if none specified
        if (accountIds == null || accountIds.isEmpty()) {
            accountIds = getAllAccountIds();
        }
        
        GlobalMigrationJob job = new GlobalMigrationJob(
            jobId, 
            GlobalMigrationJobType.SECTION_ONLY,
            accountIds,
            batchSize,
            parallelism,
            false
        );
        
        activeJobs.put(jobId, job);
        
        // Start async migration
        CompletableFuture.runAsync(() -> executeSectionMigration(job), taskExecutor);
        
        return new GlobalMigrationJobResponse(jobId, job.getStatus().name(), 
            "Section migration started for " + accountIds.size() + " accounts");
    }
    
    /**
     * Execute Section migration asynchronously
     * @param job Migration job
     */
    public void executeSectionMigration(GlobalMigrationJob job) {
        try {
            job.setStatus(GlobalMigrationJobStatus.RUNNING);
            log.info("Executing Section migration for job: {}", job.getJobId());
            
            List<Long> accountIds = job.getAccountIds();
            AtomicLong totalProcessed = new AtomicLong(0);
            
            for (Long accountId : accountIds) {
                if (job.getStatus() == GlobalMigrationJobStatus.CANCELLED) {
                    break;
                }
                
                try {
                    job.setCurrentPhase("Section Migration for Account " + accountId);
                    
                    // Migrate Section data
                    long sectionCount = sectionMigrator.migrateSections(accountId, job.getBatchSize(), job.getParallelism());
                    totalProcessed.addAndGet(sectionCount);
                    
                    job.getProcessedRecords().addAndGet(sectionCount);
                    log.info("Completed Section migration for account: {}, processed: {}", accountId, sectionCount);
                    
                } catch (Exception e) {
                    log.error("Section migration failed for account {}: {}", accountId, e.getMessage(), e);
                    // Continue with next account
                }
            }
            
            job.setStatus(GlobalMigrationJobStatus.COMPLETED);
            job.setCurrentPhase("Completed");
            job.setCompletedAt(LocalDateTime.now());
            
            log.info("Section migration completed for job: {}, total processed: {}", 
                    job.getJobId(), totalProcessed.get());
            
        } catch (Exception e) {
            job.setStatus(GlobalMigrationJobStatus.FAILED);
            job.setError(e.getMessage());
            job.setCompletedAt(LocalDateTime.now());
            log.error("Section migration failed for job: {}", job.getJobId(), e);
        } finally {
            // Cleanup resources
            log.info("Section migration job {} finished with status: {}", job.getJobId(), job.getStatus());
        }
    }
    
    /**
     * Start combined PartManager and Section migration for specified accounts
     * @param accountIds List of account IDs to migrate (null or empty for all accounts)
     * @param batchSize Batch size for processing
     * @param parallelism Number of parallel threads
     * @return Migration job response
     */
    public GlobalMigrationJobResponse startPartManagerAndSectionMigration(
            List<Long> accountIds, int batchSize, int parallelism) {
        
        String jobId = generateJobId();
        log.info("Starting PartManager and Section migration with jobId: {}", jobId);
        
        // Use all accounts if none specified
        if (accountIds == null || accountIds.isEmpty()) {
            accountIds = getAllAccountIds();
        }
        
        GlobalMigrationJob job = new GlobalMigrationJob(
            jobId, 
            GlobalMigrationJobType.PART_MANAGER_AND_SECTION,
            accountIds,
            batchSize,
            parallelism,
            false
        );
        
        activeJobs.put(jobId, job);
        
        // Start async migration
        CompletableFuture.runAsync(() -> executePartManagerAndSectionMigration(job), taskExecutor);
        
        return new GlobalMigrationJobResponse(jobId, job.getStatus().name(), 
            "PartManager and Section migration started for " + accountIds.size() + " accounts");
    }
    
    /**
     * Execute combined PartManager and Section migration asynchronously
     * @param job Migration job
     */
    public void executePartManagerAndSectionMigration(GlobalMigrationJob job) {
        try {
            job.setStatus(GlobalMigrationJobStatus.RUNNING);
            log.info("Executing PartManager and Section migration for job: {}", job.getJobId());
            
            List<Long> accountIds = job.getAccountIds();
            AtomicLong totalProcessed = new AtomicLong(0);
            
            for (Long accountId : accountIds) {
                if (job.getStatus() == GlobalMigrationJobStatus.CANCELLED) {
                    break;
                }
                
                try {
                    job.setCurrentPhase("PartManager and Section Migration for Account " + accountId);
                    
                    // Migrate PartManager data
                    long partManagerCount = partManagerMigrator.migratePartManagers(accountId, job.getBatchSize(), job.getParallelism());
                    totalProcessed.addAndGet(partManagerCount);
                    
                    // Migrate Section data
                    long sectionCount = sectionMigrator.migrateSections(accountId, job.getBatchSize(), job.getParallelism());
                    totalProcessed.addAndGet(sectionCount);
                    
                    job.getProcessedRecords().addAndGet(partManagerCount + sectionCount);
                    log.info("Completed PartManager and Section migration for account: {}, PartManager: {}, Sections: {}", 
                            accountId, partManagerCount, sectionCount);
                    
                } catch (Exception e) {
                    log.error("PartManager and Section migration failed for account {}: {}", accountId, e.getMessage(), e);
                    // Continue with next account
                }
            }
            
            job.setStatus(GlobalMigrationJobStatus.COMPLETED);
            job.setCurrentPhase("Completed");
            job.setCompletedAt(LocalDateTime.now());
            
            log.info("PartManager and Section migration completed for job: {}, total processed: {}", 
                    job.getJobId(), totalProcessed.get());
            
        } catch (Exception e) {
            job.setStatus(GlobalMigrationJobStatus.FAILED);
            job.setError(e.getMessage());
            job.setCompletedAt(LocalDateTime.now());
            log.error("PartManager and Section migration failed for job: {}", job.getJobId(), e);
        } finally {
            // Cleanup resources
            log.info("PartManager and Section migration job {} finished with status: {}", job.getJobId(), job.getStatus());
        }
    }
}
