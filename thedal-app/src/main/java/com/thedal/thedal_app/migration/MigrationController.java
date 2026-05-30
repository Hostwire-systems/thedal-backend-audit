package com.thedal.thedal_app.migration;

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
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/migration")
@Slf4j
@Tag(name = "Data Migration", description = "PostgreSQL to MongoDB migration endpoints")
public class MigrationController {

    @Autowired
    private MigrationService migrationService;

    @Operation(summary = "Migrate all data for an account and election from PostgreSQL to MongoDB")
    @PostMapping("/full/{accountId}/{electionId}")
    public ResponseEntity<ThedalResponse<MigrationJobResponse>> migrateFullData(
            @PathVariable Long accountId,
            @PathVariable Long electionId,
            @RequestParam(defaultValue = "1000") int batchSize,
            @RequestParam(defaultValue = "false") boolean overwriteExisting) {
        
        log.info("Starting full migration for accountId: {}, electionId: {}", accountId, electionId);
        
        MigrationJobResponse jobResponse = migrationService.startFullMigration(
            accountId, electionId, batchSize, overwriteExisting);
        
        return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.MIGRATION_STARTED, jobResponse));
    }

    @Operation(summary = "Migrate specific modules from PostgreSQL to MongoDB")
    @PostMapping("/selective")
    public ResponseEntity<ThedalResponse<MigrationJobResponse>> migrateSelectiveData(
            @RequestBody SelectiveMigrationRequest request) {
        
        log.info("Starting selective migration for accountId: {}, electionId: {}, modules: {}", 
            request.getAccountId(), request.getElectionId(), request.getModules());
        
        MigrationJobResponse jobResponse = migrationService.startSelectiveMigration(request);
        
        return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.MIGRATION_STARTED, jobResponse));
    }

    @Operation(summary = "Migrate only voters data from PostgreSQL to MongoDB")
    @PostMapping("/voters/{accountId}/{electionId}")
    public ResponseEntity<ThedalResponse<MigrationJobResponse>> migrateVotersOnly(
            @PathVariable Long accountId,
            @PathVariable Long electionId,
            @RequestParam(defaultValue = "1000") int batchSize,
            @RequestParam(defaultValue = "false") boolean overwriteExisting) {
        
        log.info("Starting voters-only migration for accountId: {}, electionId: {}", accountId, electionId);
        
        MigrationJobResponse jobResponse = migrationService.startVotersMigration(
            accountId, electionId, batchSize, overwriteExisting);
        
        return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.MIGRATION_STARTED, jobResponse));
    }

    @Operation(summary = "Migrate only election settings from PostgreSQL to MongoDB")
    @PostMapping("/settings/{accountId}/{electionId}")
    public ResponseEntity<ThedalResponse<MigrationJobResponse>> migrateSettingsOnly(
            @PathVariable Long accountId,
            @PathVariable Long electionId,
            @RequestParam(defaultValue = "1000") int batchSize,
            @RequestParam(defaultValue = "false") boolean overwriteExisting) {
        
        log.info("Starting settings-only migration for accountId: {}, electionId: {}", accountId, electionId);
        
        MigrationJobResponse jobResponse = migrationService.startSettingsMigration(
            accountId, electionId, batchSize, overwriteExisting);
        
        return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.MIGRATION_STARTED, jobResponse));
    }

    @Operation(summary = "Migrate only users data from PostgreSQL to MongoDB")
    @PostMapping("/users/{accountId}")
    public ResponseEntity<ThedalResponse<MigrationJobResponse>> migrateUsersOnly(
            @PathVariable Long accountId,
            @RequestParam(defaultValue = "1000") int batchSize,
            @RequestParam(defaultValue = "false") boolean overwriteExisting) {
        
        log.info("Starting users-only migration for accountId: {}", accountId);
        
        MigrationJobResponse jobResponse = migrationService.startUsersMigration(
            accountId, batchSize, overwriteExisting);
        
        return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.MIGRATION_STARTED, jobResponse));
    }

    @Operation(summary = "Get migration job status and progress")
    @GetMapping("/status/{jobId}")
    public ResponseEntity<ThedalResponse<MigrationJob>> getMigrationStatus(@PathVariable String jobId) {
        
        MigrationJob job = migrationService.getMigrationStatus(jobId);
        
        return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.MIGRATION_STATUS_RETRIEVED, job));
    }

    @Operation(summary = "Get all migration jobs for an account and election")
    @GetMapping("/jobs/{accountId}/{electionId}")
    public ResponseEntity<ThedalResponse<List<MigrationJob>>> getMigrationJobs(
            @PathVariable Long accountId,
            @PathVariable Long electionId) {
        
        List<MigrationJob> jobs = migrationService.getMigrationJobs(accountId, electionId);
        
        return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.MIGRATION_JOBS_RETRIEVED, jobs));
    }

    @Operation(summary = "Cancel a running migration job")
    @PostMapping("/cancel/{jobId}")
    public ResponseEntity<ThedalResponse<String>> cancelMigration(@PathVariable String jobId) {
        
        migrationService.cancelMigration(jobId);
        
        return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.MIGRATION_CANCELLED, 
            "Migration job " + jobId + " has been cancelled"));
    }

    @Operation(summary = "Clean up MongoDB data for an account and election")
    @PostMapping("/cleanup/{accountId}/{electionId}")
    public ResponseEntity<ThedalResponse<Map<String, Long>>> cleanupMongoData(
            @PathVariable Long accountId,
            @PathVariable Long electionId,
            @RequestParam(defaultValue = "false") boolean dryRun) {
        
        log.info("Starting MongoDB cleanup for accountId: {}, electionId: {}, dryRun: {}", 
            accountId, electionId, dryRun);
        
        Map<String, Long> deletedCounts = migrationService.cleanupMongoData(accountId, electionId, dryRun);
        
        return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.CLEANUP_COMPLETED, deletedCounts));
    }

    @Operation(summary = "Validate data consistency between PostgreSQL and MongoDB")
    @GetMapping("/validate/{accountId}/{electionId}")
    public ResponseEntity<ThedalResponse<ValidationReport>> validateDataConsistency(
            @PathVariable Long accountId,
            @PathVariable Long electionId) {
        
        log.info("Starting data validation for accountId: {}, electionId: {}", accountId, electionId);
        
        ValidationReport report = migrationService.validateDataConsistency(accountId, electionId);
        
        return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.VALIDATION_COMPLETED, report));
    }

    @Operation(summary = "Get migration statistics and summary")
    @GetMapping("/stats/{accountId}/{electionId}")
    public ResponseEntity<ThedalResponse<MigrationStats>> getMigrationStats(
            @PathVariable Long accountId,
            @PathVariable Long electionId) {
        
        MigrationStats stats = migrationService.getMigrationStats(accountId, electionId);
        
        return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.MIGRATION_STATS_RETRIEVED, stats));
    }

    @Operation(summary = "Migrate family mapping data from PostgreSQL to MongoDB")
    @PostMapping("/family-mapping/{accountId}/{electionId}")
    public ResponseEntity<ThedalResponse<MigrationJobResponse>> migrateFamilyMapping(
            @PathVariable Long accountId,
            @PathVariable Long electionId,
            @RequestParam(defaultValue = "1000") int batchSize,
            @RequestParam(defaultValue = "false") boolean overwriteExisting,
            @RequestParam(defaultValue = "true") boolean recalculateFamilyCount) {
        
        log.info("Starting family mapping migration for accountId: {}, electionId: {}, batchSize: {}, overwrite: {}, recalculate: {}", 
                 accountId, electionId, batchSize, overwriteExisting, recalculateFamilyCount);
        
        // TODO: This method doesn't exist in MigrationService - needs to be implemented
        // MigrationJobResponse jobResponse = migrationService.startFamilyMappingMigration(
        //     accountId, electionId, batchSize, overwriteExisting, recalculateFamilyCount);
        
        // Temporary response until method is implemented
        MigrationJobResponse jobResponse = new MigrationJobResponse(
            "temp-job-id", 
            "PENDING", 
            "Family mapping migration temporarily disabled", 
            accountId, 
            electionId, 
            0L);
        
        return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.MIGRATION_STARTED, jobResponse));
    }
}
