package com.thedal.thedal_app.migration;

import java.util.HashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.thedal.thedal_app.response.ThedalResponse;
import com.thedal.thedal_app.response.ThedalSuccess;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller for managing global data migration from PostgreSQL to MongoDB
 */
@RestController
@RequestMapping("/api/global-migration")
@Slf4j
@Tag(name = "Global Data Migration", description = "Complete PostgreSQL to MongoDB migration endpoints - irrespective of account/election")
public class GlobalMigrationController {

    @Autowired
    private GlobalMigrationService globalMigrationService;

    @Operation(summary = "Start Complete Global Migration", 
               description = "Migrates ALL data from PostgreSQL to MongoDB irrespective of account or election")
    @PostMapping("/complete")
    public ResponseEntity<ThedalResponse<GlobalMigrationJobResponse>> startCompleteGlobalMigration(
            @Parameter(description = "Batch size for processing records (default: 500)", example = "500")
            @RequestParam(defaultValue = "500") int batchSize,
            
            @Parameter(description = "Whether to overwrite existing MongoDB data", example = "false")
            @RequestParam(defaultValue = "false") boolean overwriteExisting,
            
            @Parameter(description = "Skip validation checks for faster migration", example = "false")
            @RequestParam(defaultValue = "false") boolean skipValidation,
            
            @Parameter(description = "Include only specific modules (comma-separated: settings,users,voters,elections)")
            @RequestParam(required = false) List<String> includeModules,
            
            @Parameter(description = "Exclude specific modules (comma-separated: settings,users,voters,elections)")
            @RequestParam(required = false) List<String> excludeModules) {
        
        log.info("Starting complete global migration with batchSize: {}, overwrite: {}, skipValidation: {}", 
                batchSize, overwriteExisting, skipValidation);
        
        GlobalMigrationJobResponse jobResponse = globalMigrationService.startCompleteGlobalMigration(
            batchSize, overwriteExisting, skipValidation, includeModules, excludeModules);
        
        return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.MIGRATION_STARTED, jobResponse));
    }

    @Operation(summary = "Start Account-Specific Global Migration", 
               description = "Migrates data for specific accounts from PostgreSQL to MongoDB")
    @PostMapping("/accounts")
    public ResponseEntity<ThedalResponse<GlobalMigrationJobResponse>> startAccountSpecificMigration(
            @Parameter(description = "List of account IDs to migrate", required = true)
            @RequestBody List<Long> accountIds,
            
            @Parameter(description = "Batch size for processing records (default: 500)", example = "500")
            @RequestParam(defaultValue = "500") int batchSize,
            
            @Parameter(description = "Whether to overwrite existing MongoDB data", example = "false")
            @RequestParam(defaultValue = "false") boolean overwriteExisting) {
        
        log.info("Starting account-specific global migration for accounts: {}", accountIds);
        
        GlobalMigrationJobResponse jobResponse = globalMigrationService.startAccountSpecificMigration(
            accountIds, batchSize, overwriteExisting);
        
        return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.MIGRATION_STARTED, jobResponse));
    }

    @Operation(summary = "Get Global Migration Job Status", 
               description = "Retrieve detailed status and progress of a global migration job")
    @GetMapping("/status/{jobId}")
    public ResponseEntity<ThedalResponse<GlobalMigrationJob>> getGlobalMigrationStatus(
            @Parameter(description = "Global migration job ID", required = true)
            @PathVariable String jobId) {
        
        log.debug("Getting global migration status for jobId: {}", jobId);
        
        GlobalMigrationJob job = globalMigrationService.getGlobalMigrationStatus(jobId);
        
        return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.MIGRATION_STATUS_RETRIEVED, job));
    }

    @Operation(summary = "Get All Global Migration Jobs", 
               description = "Retrieve list of all global migration jobs")
    @GetMapping("/jobs")
    public ResponseEntity<ThedalResponse<List<GlobalMigrationJob>>> getAllGlobalMigrationJobs() {
        
        log.debug("Getting all global migration jobs");
        
        List<GlobalMigrationJob> jobs = globalMigrationService.getAllGlobalMigrationJobs();
        
        return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.MIGRATION_JOBS_RETRIEVED, jobs));
    }

    @Operation(summary = "Cancel Global Migration Job", 
               description = "Cancel a running global migration job")
    @PostMapping("/cancel/{jobId}")
    public ResponseEntity<ThedalResponse<String>> cancelGlobalMigration(
            @Parameter(description = "Global migration job ID", required = true)
            @PathVariable String jobId) {
        
        log.info("Cancelling global migration job: {}", jobId);
        
        globalMigrationService.cancelGlobalMigration(jobId);
        
        return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.MIGRATION_CANCELLED, 
            "Global migration job " + jobId + " has been cancelled"));
    }

    @Operation(summary = "Pause Global Migration Job", 
               description = "Pause a running global migration job")
    @PostMapping("/pause/{jobId}")
    public ResponseEntity<ThedalResponse<String>> pauseGlobalMigration(
            @Parameter(description = "Global migration job ID", required = true)
            @PathVariable String jobId) {
        
        log.info("Pausing global migration job: {}", jobId);
        
        globalMigrationService.pauseGlobalMigration(jobId);
        
        return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.SUCCESS, 
            "Global migration job " + jobId + " has been paused"));
    }

    @Operation(summary = "Resume Global Migration Job", 
               description = "Resume a paused global migration job")
    @PostMapping("/resume/{jobId}")
    public ResponseEntity<ThedalResponse<String>> resumeGlobalMigration(
            @Parameter(description = "Global migration job ID", required = true)
            @PathVariable String jobId) {
        
        log.info("Resuming global migration job: {}", jobId);
        
        globalMigrationService.resumeGlobalMigration(jobId);
        
        return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.SUCCESS, 
            "Global migration job " + jobId + " has been resumed"));
    }

    @Operation(summary = "Get Global Migration Statistics", 
               description = "Get overall statistics for global migration system")
    @GetMapping("/stats")
    public ResponseEntity<ThedalResponse<Map<String, Object>>> getGlobalMigrationStats() {
        
        log.debug("Getting global migration statistics");
        
        Map<String, Object> stats = globalMigrationService.getGlobalMigrationStats();
        
        return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.STATS_RETRIEVED, stats));
    }

    @Operation(summary = "Health Check", 
               description = "Check if the global migration service is healthy")
    @GetMapping("/health")
    public ResponseEntity<ThedalResponse<Map<String, Object>>> healthCheck() {
        
        Map<String, Object> health = Map.of(
            "status", "healthy",
            "timestamp", System.currentTimeMillis(),
            "service", "GlobalMigrationService"
        );
        
        return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.SUCCESS, health));
    }

    @Operation(summary = "Debug Migration Services", 
               description = "Check which migration services are available")
    @GetMapping("/debug/services")
    public ResponseEntity<ThedalResponse<Map<String, Object>>> debugMigrationServices() {
        
        Map<String, Object> serviceStatus = new HashMap<>();
        
        try {
            serviceStatus.put("globalMigrationService", globalMigrationService != null ? "Available" : "NULL");
            serviceStatus.put("timestamp", System.currentTimeMillis());
            serviceStatus.put("activeJobs", globalMigrationService.getAllGlobalMigrationJobs().size());
            
            log.info("Debug: Migration services status checked");
            
            return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.SUCCESS, serviceStatus));
        } catch (Exception e) {
            log.error("Debug: Error checking migration services", e);
            serviceStatus.put("error", e.getMessage());
            serviceStatus.put("stackTrace", java.util.Arrays.toString(e.getStackTrace()));
            return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.SUCCESS, serviceStatus));
        }
    }

    @Operation(summary = "Fast Voter Migration - All Voters", 
               description = "Migrates ALL voters from PostgreSQL to MongoDB with optimized batch processing for maximum speed")
    @PostMapping("/voters-only")
    public ResponseEntity<ThedalResponse<GlobalMigrationJobResponse>> startFastVoterMigration(
            @Parameter(description = "Batch size for processing records (default: 1000, max: 5000)", example = "1000")
            @RequestParam(defaultValue = "1000") int batchSize,
            
            @Parameter(description = "Whether to overwrite existing MongoDB data", example = "false")
            @RequestParam(defaultValue = "false") boolean overwriteExisting,
            
            @Parameter(description = "Skip validation checks for faster migration", example = "true")
            @RequestParam(defaultValue = "true") boolean skipValidation,
            
            @Parameter(description = "Use parallel processing for multiple accounts", example = "true")
            @RequestParam(defaultValue = "true") boolean useParallelProcessing) {
        
        log.info("Starting fast voter-only migration with batchSize: {}, overwrite: {}, skipValidation: {}, parallel: {}", 
                batchSize, overwriteExisting, skipValidation, useParallelProcessing);
        
        // Validate batch size for performance optimization
        if (batchSize > 5000) {
            log.warn("Batch size {} exceeds maximum recommended value of 5000, setting to 5000", batchSize);
            batchSize = 5000;
        }
        
        GlobalMigrationJobResponse jobResponse = globalMigrationService.startFastVoterOnlyMigration(
            batchSize, overwriteExisting, skipValidation, useParallelProcessing);
        
        return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.MIGRATION_STARTED, jobResponse));
    }

    @Operation(summary = "Comprehensive Data Migration (Exclude Voters)", 
               description = "Migrates ALL data except voters from PostgreSQL to MongoDB - optimized for complete non-voter data migration")
    @PostMapping("/comprehensive-exclude-voters")
    public ResponseEntity<ThedalResponse<GlobalMigrationJobResponse>> startComprehensiveNonVoterMigration(
            @Parameter(description = "Batch size for processing records (default: 1000)", example = "1000")
            @RequestParam(defaultValue = "1000") int batchSize,
            
            @Parameter(description = "Whether to overwrite existing MongoDB data", example = "false")
            @RequestParam(defaultValue = "false") boolean overwriteExisting,
            
            @Parameter(description = "Skip validation checks for faster migration", example = "true")
            @RequestParam(defaultValue = "true") boolean skipValidation,
            
            @Parameter(description = "Use parallel processing for multiple accounts", example = "true")
            @RequestParam(defaultValue = "true") boolean useParallelProcessing,
            
            @Parameter(description = "Specific account IDs to migrate (optional - if not provided, migrates all accounts)")
            @RequestParam(required = false) List<Long> accountIds) {
        
        log.info("Starting comprehensive non-voter migration with batchSize: {}, overwrite: {}, skipValidation: {}, parallel: {}, accountIds: {}", 
                batchSize, overwriteExisting, skipValidation, useParallelProcessing, accountIds);
        
        // Validate batch size for performance optimization
        if (batchSize > 5000) {
            log.warn("Batch size {} exceeds maximum recommended value of 5000, setting to 5000", batchSize);
            batchSize = 5000;
        }
        
        GlobalMigrationJobResponse jobResponse = globalMigrationService.startComprehensiveNonVoterMigration(
            batchSize, overwriteExisting, skipValidation, useParallelProcessing, accountIds);
        
        return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.MIGRATION_STARTED, jobResponse));
    }

    @Operation(summary = "Get All Available Account IDs", 
               description = "Retrieve all account IDs available in the system for migration")
    @GetMapping("/accounts/list")
    public ResponseEntity<ThedalResponse<List<Long>>> getAllAccountIds() {
        log.info("Retrieving all available account IDs");
        
        List<Long> accountIds = globalMigrationService.getAllAccountIds();
        
        Map<String, Object> response = new HashMap<>();
        response.put("accountIds", accountIds);
        response.put("totalCount", accountIds.size());
        
        return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.MIGRATION_STATUS_RETRIEVED, accountIds));
    }

    @Operation(summary = "Start Complete Migration Including Everything", 
               description = "Migrates ALL data including PartManager, Voters, Elections, and everything else for specific accounts")
    @PostMapping("/complete-everything")
    public ResponseEntity<ThedalResponse<GlobalMigrationJobResponse>> startCompleteEverythingMigration(
            @Parameter(description = "List of account IDs to migrate (if empty, migrates all accounts)")
            @RequestBody(required = false) List<Long> accountIds,
            
            @Parameter(description = "Batch size for processing records (default: 1000)", example = "1000")
            @RequestParam(defaultValue = "1000") int batchSize,
            
            @Parameter(description = "Number of parallel threads (default: 4)", example = "4")
            @RequestParam(defaultValue = "4") int parallelism,
            
            @Parameter(description = "Whether to clear existing MongoDB data before migration", example = "false")
            @RequestParam(defaultValue = "false") boolean clearExisting) {
        
        log.info("Starting complete everything migration for accounts: {}, batchSize: {}, parallelism: {}", 
                accountIds, batchSize, parallelism);
        
        GlobalMigrationJobResponse jobResponse = globalMigrationService.startCompleteEverythingMigration(
            accountIds, batchSize, parallelism, clearExisting);
        
        return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.MIGRATION_STARTED, jobResponse));
    }

    @Operation(summary = "Start PartManager Migration", 
               description = "Migrates PartManager data for specific accounts")
    @PostMapping("/partmanager")
    public ResponseEntity<ThedalResponse<GlobalMigrationJobResponse>> startPartManagerMigration(
            @Parameter(description = "List of account IDs to migrate (if empty, migrates all accounts)")
            @RequestBody(required = false) List<Long> accountIds,
            
            @Parameter(description = "Batch size for processing records (default: 1000)", example = "1000")
            @RequestParam(defaultValue = "1000") int batchSize,
            
            @Parameter(description = "Number of parallel threads (default: 4)", example = "4")
            @RequestParam(defaultValue = "4") int parallelism) {
        
        log.info("Starting PartManager migration for accounts: {}", accountIds);
        
        GlobalMigrationJobResponse jobResponse = globalMigrationService.startPartManagerMigration(
            accountIds, batchSize, parallelism);
        
        return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.MIGRATION_STARTED, jobResponse));
    }
    
    @Operation(summary = "Start Section Migration", 
               description = "Migrates Section data for specific accounts")
    @PostMapping("/sections")
    public ResponseEntity<ThedalResponse<GlobalMigrationJobResponse>> startSectionMigration(
            @Parameter(description = "List of account IDs to migrate (if empty, migrates all accounts)")
            @RequestBody(required = false) List<Long> accountIds,
            
            @Parameter(description = "Batch size for processing records (default: 1000)", example = "1000")
            @RequestParam(defaultValue = "1000") int batchSize,
            
            @Parameter(description = "Number of parallel threads (default: 4)", example = "4")
            @RequestParam(defaultValue = "4") int parallelism) {
        
        log.info("Starting Section migration for accounts: {}", accountIds);
        
        GlobalMigrationJobResponse jobResponse = globalMigrationService.startSectionMigration(
            accountIds, batchSize, parallelism);
        
        return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.MIGRATION_STARTED, jobResponse));
    }
    
    @Operation(summary = "Start Combined PartManager and Section Migration", 
               description = "Migrates both PartManager and Section data for specific accounts - fully comprehensive for both data types")
    @PostMapping("/partmanager-and-sections")
    public ResponseEntity<ThedalResponse<GlobalMigrationJobResponse>> startPartManagerAndSectionMigration(
            @Parameter(description = "List of account IDs to migrate (if empty, migrates all accounts)")
            @RequestBody(required = false) List<Long> accountIds,
            
            @Parameter(description = "Batch size for processing records (default: 1000)", example = "1000")
            @RequestParam(defaultValue = "1000") int batchSize,
            
            @Parameter(description = "Number of parallel threads (default: 4)", example = "4")
            @RequestParam(defaultValue = "4") int parallelism) {
        
        log.info("Starting PartManager and Section migration for accounts: {}", accountIds);
        
        GlobalMigrationJobResponse jobResponse = globalMigrationService.startPartManagerAndSectionMigration(
            accountIds, batchSize, parallelism);
        
        return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.MIGRATION_STARTED, jobResponse));
    }
}
