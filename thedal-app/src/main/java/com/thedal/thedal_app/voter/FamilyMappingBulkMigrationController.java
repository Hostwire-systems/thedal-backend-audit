package com.thedal.thedal_app.voter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/migration/family-mapping/bulk")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Bulk Family Mapping Migration", description = "APIs for bulk migration and repair of family mapping data")
public class FamilyMappingBulkMigrationController {

    @Autowired
    private FamilyMappingBulkMigrationService bulkMigrationService;

    @Operation(summary = "Perform Bulk Family Mapping Migration", 
               description = "Migrates ALL family mapping data from PostgreSQL to MongoDB across all accounts and elections")
    @PostMapping("/migrate-all")
    public ResponseEntity<String> performBulkMigration() {
        
        log.info("Starting bulk family mapping migration for all data");
        
        try {
            String result = bulkMigrationService.performBulkFamilyMappingMigration();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Bulk family mapping migration failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body("Bulk migration failed: " + e.getMessage());
        }
    }

    @Operation(summary = "Repair Inconsistent Family Mappings", 
               description = "Identifies and repairs inconsistent family mapping data between PostgreSQL and MongoDB")
    @PostMapping("/repair")
    public ResponseEntity<String> repairInconsistentMappings() {
        
        log.info("Starting repair of inconsistent family mapping data");
        
        try {
            String result = bulkMigrationService.repairInconsistentFamilyMappings();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Family mapping repair failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body("Repair failed: " + e.getMessage());
        }
    }

    @Operation(summary = "Comprehensive Family Mapping Validation", 
               description = "Validates ALL family mapping data for consistency between PostgreSQL and MongoDB")
    @GetMapping("/validate-all")
    public ResponseEntity<String> performComprehensiveValidation() {
        
        log.info("Starting comprehensive family mapping validation");
        
        try {
            String result = bulkMigrationService.performComprehensiveValidation();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Comprehensive validation failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body("Validation failed: " + e.getMessage());
        }
    }
}
