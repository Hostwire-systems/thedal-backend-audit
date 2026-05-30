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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.thedal.thedal_app.election.ElectionRepository;
import com.thedal.thedal_app.general.RequestDetailsService;
import com.thedal.thedal_app.migration.migrators.SettingsMigrator;
import com.thedal.thedal_app.migration.migrators.UserMigrator;
import com.thedal.thedal_app.migration.migrators.VoterMigrator;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MigrationService {

    @Autowired
    private ElectionRepository electionRepository;
    
    @Autowired
    private RequestDetailsService requestDetails;
    
    @Autowired
    private VoterMigrator voterMigrator;
    
    @Autowired
    private SettingsMigrator settingsMigrator;
    
    @Autowired
    private UserMigrator userMigrator;
    
    @Autowired
    @Qualifier("taskExecutor")
    private Executor taskExecutor;

    // In-memory job tracking (in production, consider using Redis or database)
    private final Map<String, MigrationJob> activeJobs = new ConcurrentHashMap<>();

    public MigrationJobResponse startFullMigration(Long accountId, Long electionId, int batchSize, boolean overwriteExisting) {
        validateElectionAccess(accountId, electionId);
        
        String jobId = generateJobId();
        String description = String.format("Full migration for account %d, election %d", accountId, electionId);
        
        MigrationJob job = new MigrationJob(jobId, accountId, electionId, description);
        activeJobs.put(jobId, job);
        
        // Start async migration
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            try {
                executeFullMigration(job, batchSize, overwriteExisting);
            } catch (Exception e) {
                handleMigrationError(job, e);
            }
        }, taskExecutor);
        
        job.setFuture(future);
        
        return new MigrationJobResponse(jobId, job.getStatus().name(), description, accountId, electionId, 0L);
    }

    public MigrationJobResponse startSelectiveMigration(SelectiveMigrationRequest request) {
        validateElectionAccess(request.getAccountId(), request.getElectionId());
        
        String jobId = generateJobId();
        String description = String.format("Selective migration for account %d, election %d, modules: %s", 
            request.getAccountId(), request.getElectionId(), request.getModules());
        
        MigrationJob job = new MigrationJob(jobId, request.getAccountId(), request.getElectionId(), description);
        activeJobs.put(jobId, job);
        
        // Start async migration
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            try {
                executeSelectiveMigration(job, request);
            } catch (Exception e) {
                handleMigrationError(job, e);
            }
        }, taskExecutor);
        
        job.setFuture(future);
        
        return new MigrationJobResponse(jobId, job.getStatus().name(), description, 
            request.getAccountId(), request.getElectionId(), 0L);
    }

    public MigrationJobResponse startVotersMigration(Long accountId, Long electionId, int batchSize, boolean overwriteExisting) {
        validateElectionAccess(accountId, electionId);
        
        String jobId = generateJobId();
        String description = String.format("Voters migration for account %d, election %d", accountId, electionId);
        
        MigrationJob job = new MigrationJob(jobId, accountId, electionId, description);
        activeJobs.put(jobId, job);
        
        // Start async migration
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            try {
                executeVotersMigration(job, batchSize, overwriteExisting);
            } catch (Exception e) {
                handleMigrationError(job, e);
            }
        }, taskExecutor);
        
        job.setFuture(future);
        
        return new MigrationJobResponse(jobId, job.getStatus().name(), description, accountId, electionId, 0L);
    }

    public MigrationJobResponse startSettingsMigration(Long accountId, Long electionId, int batchSize, boolean overwriteExisting) {
        validateElectionAccess(accountId, electionId);
        
        String jobId = generateJobId();
        String description = String.format("Settings migration for account %d, election %d", accountId, electionId);
        
        MigrationJob job = new MigrationJob(jobId, accountId, electionId, description);
        activeJobs.put(jobId, job);
        
        // Start async migration
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            try {
                executeSettingsMigration(job, batchSize, overwriteExisting);
            } catch (Exception e) {
                handleMigrationError(job, e);
            }
        }, taskExecutor);
        
        job.setFuture(future);
        
        return new MigrationJobResponse(jobId, job.getStatus().name(), description, accountId, electionId, 0L);
    }

    public MigrationJobResponse startUsersMigration(Long accountId, int batchSize, boolean overwriteExisting) {
        String jobId = generateJobId();
        String description = String.format("Users migration for account %d", accountId);
        
        MigrationJob job = new MigrationJob(jobId, accountId, null, description);
        activeJobs.put(jobId, job);
        
        // Start async migration
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            try {
                executeUsersMigration(job, batchSize, overwriteExisting);
            } catch (Exception e) {
                handleMigrationError(job, e);
            }
        }, taskExecutor);
        
        job.setFuture(future);
        
        return new MigrationJobResponse(jobId, job.getStatus().name(), description, accountId, null, 0L);
    }

    public MigrationJob getMigrationStatus(String jobId) {
        MigrationJob job = activeJobs.get(jobId);
        if (job == null) {
            throw new ThedalException(ThedalError.MIGRATION_JOB_NOT_FOUND, HttpStatus.NOT_FOUND);
        }
        return job;
    }

    public List<MigrationJob> getMigrationJobs(Long accountId, Long electionId) {
        return activeJobs.values().stream()
            .filter(job -> job.getAccountId().equals(accountId) && 
                          (electionId == null || electionId.equals(job.getElectionId())))
            .toList();
    }

    public void cancelMigration(String jobId) {
        MigrationJob job = activeJobs.get(jobId);
        if (job == null) {
            throw new ThedalException(ThedalError.MIGRATION_JOB_NOT_FOUND, HttpStatus.NOT_FOUND);
        }
        
        job.setCancelled(true);
        job.setStatus(MigrationJobStatus.CANCELLED);
        
        if (job.getFuture() != null) {
            job.getFuture().cancel(true);
        }
        
        log.info("Migration job {} cancelled", jobId);
    }

    public Map<String, Long> cleanupMongoData(Long accountId, Long electionId, boolean dryRun) {
        validateElectionAccess(accountId, electionId);
        
        Map<String, Long> deletedCounts = new HashMap<>();
        
        try {
            // Cleanup voters
            if (!dryRun) {
                deletedCounts.put("voters", voterMigrator.cleanupMongoData(accountId, electionId));
            } else {
                deletedCounts.put("voters", voterMigrator.countMongoRecords(accountId, electionId));
            }
            
            // Cleanup settings
            Map<String, Long> settingsCounts = settingsMigrator.cleanupMongoData(accountId, electionId, dryRun);
            deletedCounts.putAll(settingsCounts);
            
            // Cleanup users (account-wide)
            if (!dryRun) {
                deletedCounts.put("users", userMigrator.cleanupMongoData(accountId));
            } else {
                deletedCounts.put("users", userMigrator.countMongoRecords(accountId));
            }
            
            log.info("MongoDB cleanup completed for accountId: {}, electionId: {}, dryRun: {}, counts: {}", 
                accountId, electionId, dryRun, deletedCounts);
            
        } catch (Exception e) {
            log.error("Error during MongoDB cleanup: {}", e.getMessage(), e);
            throw new ThedalException(ThedalError.CLEANUP_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        
        return deletedCounts;
    }

    public ValidationReport validateDataConsistency(Long accountId, Long electionId) {
        validateElectionAccess(accountId, electionId);
        
        long startTime = System.currentTimeMillis();
        Map<String, ValidationReport.EntityValidationResult> results = new HashMap<>();
        
        try {
            // Validate voters
            ValidationReport.EntityValidationResult voterResult = voterMigrator.validateConsistency(accountId, electionId);
            results.put("voters", voterResult);
            
            // Validate settings
            Map<String, ValidationReport.EntityValidationResult> settingsResults = 
                settingsMigrator.validateConsistency(accountId, electionId);
            results.putAll(settingsResults);
            
            // Validate users
            ValidationReport.EntityValidationResult userResult = userMigrator.validateConsistency(accountId);
            results.put("users", userResult);
            
            boolean overallStatus = results.values().stream().allMatch(ValidationReport.EntityValidationResult::isConsistent);
            String summary = String.format("Validation completed. %d entities checked, %d consistent, %d inconsistent", 
                results.size(), 
                (int) results.values().stream().filter(ValidationReport.EntityValidationResult::isConsistent).count(),
                (int) results.values().stream().filter(r -> !r.isConsistent()).count());
            
            return new ValidationReport(accountId, electionId, results, overallStatus, summary, 
                System.currentTimeMillis() - startTime);
            
        } catch (Exception e) {
            log.error("Error during data validation: {}", e.getMessage(), e);
            throw new ThedalException(ThedalError.VALIDATION_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public MigrationStats getMigrationStats(Long accountId, Long electionId) {
        validateElectionAccess(accountId, electionId);
        
        Map<String, MigrationStats.EntityStats> entityStats = new HashMap<>();
        long totalPostgres = 0;
        long totalMongo = 0;
        
        try {
            // Voter stats
            MigrationStats.EntityStats voterStats = voterMigrator.getEntityStats(accountId, electionId);
            entityStats.put("voters", voterStats);
            totalPostgres += voterStats.getPostgresCount();
            totalMongo += voterStats.getMongoCount();
            
            // Settings stats
            Map<String, MigrationStats.EntityStats> settingsStats = settingsMigrator.getEntityStats(accountId, electionId);
            entityStats.putAll(settingsStats);
            totalPostgres += settingsStats.values().stream().mapToLong(MigrationStats.EntityStats::getPostgresCount).sum();
            totalMongo += settingsStats.values().stream().mapToLong(MigrationStats.EntityStats::getMongoCount).sum();
            
            // User stats
            MigrationStats.EntityStats userStats = userMigrator.getEntityStats(accountId);
            entityStats.put("users", userStats);
            totalPostgres += userStats.getPostgresCount();
            totalMongo += userStats.getMongoCount();
            
            double migrationCompleteness = totalPostgres > 0 ? (double) totalMongo / totalPostgres * 100 : 0;
            
            return new MigrationStats(accountId, electionId, entityStats, totalPostgres, totalMongo, 
                migrationCompleteness, LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
        } catch (Exception e) {
            log.error("Error getting migration stats: {}", e.getMessage(), e);
            throw new ThedalException(ThedalError.STATS_RETRIEVAL_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Private methods for executing migrations
    
    private void executeFullMigration(MigrationJob job, int batchSize, boolean overwriteExisting) {
        log.info("Starting full migration for job: {}", job.getJobId());
        
        try {
            // Execute in dependency order
            executeSettingsMigration(job, batchSize, overwriteExisting);
            if (job.isCancelled()) return;
            
            executeUsersMigration(job, batchSize, overwriteExisting);
            if (job.isCancelled()) return;
            
            executeVotersMigration(job, batchSize, overwriteExisting);
            if (job.isCancelled()) return;
            
            job.setStatus(MigrationJobStatus.COMPLETED);
            job.setEndTime(System.currentTimeMillis());
            job.setCompletedTime(LocalDateTime.now());
            
            log.info("Full migration completed for job: {}", job.getJobId());
            
        } catch (Exception e) {
            handleMigrationError(job, e);
        }
    }

    private void executeSelectiveMigration(MigrationJob job, SelectiveMigrationRequest request) {
        log.info("Starting selective migration for job: {}, modules: {}", job.getJobId(), request.getModules());
        
        try {
            for (String module : request.getModules()) {
                if (job.isCancelled()) return;
                
                switch (module.toLowerCase()) {
                    case "voters":
                        voterMigrator.migrate(job, request.getBatchSize(), request.isOverwriteExisting());
                        break;
                    case "users":
                        userMigrator.migrate(job, request.getBatchSize(), request.isOverwriteExisting());
                        break;
                    case "settings":
                    case "religion":
                    case "caste":
                    case "subcaste":
                    case "party":
                    case "language":
                    case "benefitschemes":
                    case "feedbackissue":
                    case "availability":
                    case "voterhistory":
                        settingsMigrator.migrateModule(job, module, request.getBatchSize(), request.isOverwriteExisting());
                        break;
                    default:
                        log.warn("Unknown module for migration: {}", module);
                }
            }
            
            job.setStatus(MigrationJobStatus.COMPLETED);
            job.setEndTime(System.currentTimeMillis());
            job.setCompletedTime(LocalDateTime.now());
            
            log.info("Selective migration completed for job: {}", job.getJobId());
            
        } catch (Exception e) {
            handleMigrationError(job, e);
        }
    }

    private void executeVotersMigration(MigrationJob job, int batchSize, boolean overwriteExisting) {
        log.info("Starting voters migration for job: {}", job.getJobId());
        voterMigrator.migrate(job, batchSize, overwriteExisting);
    }

    private void executeSettingsMigration(MigrationJob job, int batchSize, boolean overwriteExisting) {
        log.info("Starting settings migration for job: {}", job.getJobId());
        settingsMigrator.migrate(job, batchSize, overwriteExisting);
    }

    private void executeUsersMigration(MigrationJob job, int batchSize, boolean overwriteExisting) {
        log.info("Starting users migration for job: {}", job.getJobId());
        userMigrator.migrate(job, batchSize, overwriteExisting);
    }

    private void handleMigrationError(MigrationJob job, Exception e) {
        log.error("Migration failed for job: {}, error: {}", job.getJobId(), e.getMessage(), e);
        job.setStatus(MigrationJobStatus.FAILED);
        job.setEndTime(System.currentTimeMillis());
        job.setErrorMessage(e.getMessage());
    }

    private void validateElectionAccess(Long accountId, Long electionId) {
        if (electionId != null && !electionRepository.existsByIdAndAccountId(electionId, accountId)) {
            throw new ThedalException(ThedalError.INVALID_ELECTION, HttpStatus.FORBIDDEN);
        }
    }

    private String generateJobId() {
        return "MIG_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }
}
