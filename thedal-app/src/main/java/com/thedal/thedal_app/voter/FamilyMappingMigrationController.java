package com.thedal.thedal_app.voter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/migration/family-mapping")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Family Mapping Migration", description = "APIs for migrating family mapping data from PostgreSQL to MongoDB")
public class FamilyMappingMigrationController {

    @Autowired
    private FamilyMappingMigrationService migrationService;

    @Autowired
    private FamilyMappingBulkMigrationService bulkMigrationService;

    @Operation(summary = "Migrate Family Mapping for Specific Election", 
               description = "Migrates all family mapping data from PostgreSQL to MongoDB for a specific election")
    @PostMapping("/election/{electionId}/account/{accountId}")
    public ResponseEntity<String> migrateFamilyMappingForElection(
            @PathVariable("electionId") Long electionId,
            @PathVariable("accountId") Long accountId) {
        
        log.info("Starting family mapping migration for electionId: {}, accountId: {}", electionId, accountId);
        
        try {
            String result = migrationService.migrateFamilyMappingForElection(electionId, accountId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Family mapping migration failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body("Migration failed: " + e.getMessage());
        }
    }

    @Operation(summary = "Migrate Family Mapping for All Elections in Account", 
               description = "Migrates all family mapping data from PostgreSQL to MongoDB for all elections under an account")
    @PostMapping("/account/{accountId}")
    public ResponseEntity<String> migrateFamilyMappingForAccount(
            @PathVariable("accountId") Long accountId) {
        
        log.info("Starting family mapping migration for accountId: {}", accountId);
        
        try {
            String result = migrationService.migrateFamilyMappingForAccount(accountId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Family mapping migration failed for account {}: {}", accountId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body("Migration failed: " + e.getMessage());
        }
    }

    @Operation(summary = "Migrate All Family Mapping Data", 
               description = "Migrates all family mapping data from PostgreSQL to MongoDB for all accounts and elections")
    @PostMapping("/migrate-all")
    public ResponseEntity<String> migrateAllFamilyMapping() {
        
        log.info("Starting bulk family mapping migration for all accounts and elections");
        
        try {
            String result = bulkMigrationService.performBulkFamilyMappingMigration();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Bulk family mapping migration failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body("Migration failed: " + e.getMessage());
        }
    }

    @Operation(summary = "Validate Family Mapping Consistency", 
               description = "Validates family mapping data consistency between PostgreSQL and MongoDB")
    @GetMapping("/validate/election/{electionId}/account/{accountId}")
    public ResponseEntity<String> validateFamilyMappingConsistency(
            @PathVariable("electionId") Long electionId,
            @PathVariable("accountId") Long accountId) {
        
        log.info("Validating family mapping consistency for electionId: {}, accountId: {}", electionId, accountId);
        
        try {
            String result = migrationService.validateFamilyMappingConsistency(electionId, accountId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Family mapping validation failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body("Validation failed: " + e.getMessage());
        }
    }
}
