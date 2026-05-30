package com.thedal.thedal_app.migration.migrators;

import com.thedal.thedal_app.migration.GlobalMigrationJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Advanced migrator for entities that have both PostgreSQL and MongoDB implementations
 * but require custom mapping logic. This is a simplified version to resolve compilation issues.
 */
@Component
@Slf4j
public class AdvancedMigrator {

    @Autowired
    private SectionMigrator sectionMigrator;
    
    /**
     * Migrate all members - temporarily disabled due to entity method incompatibilities
     */
    public void migrateAllMembers(GlobalMigrationJob job) {
        log.info("Member migration temporarily disabled - needs entity structure verification");
    }
    
    /**
     * Migrate all religions - temporarily disabled due to entity method incompatibilities
     */
    public void migrateAllReligions(GlobalMigrationJob job) {
        log.info("Religion migration temporarily disabled - needs entity structure verification");
    }
    
    /**
     * Migrate all castes - temporarily disabled due to entity method incompatibilities
     */
    public void migrateAllCastes(GlobalMigrationJob job) {
        log.info("Caste migration temporarily disabled - needs entity structure verification");
    }
    
    /**
     * Migrate all caste categories - temporarily disabled due to entity method incompatibilities
     */
    public void migrateAllCasteCategories(GlobalMigrationJob job) {
        log.info("Caste category migration temporarily disabled - needs entity structure verification");
    }
    
    /**
     * Migrate all sub castes - temporarily disabled due to entity method incompatibilities
     */
    public void migrateAllSubCastes(GlobalMigrationJob job) {
        log.info("Sub caste migration temporarily disabled - needs entity structure verification");
    }
    
    /**
     * Migrate all sections - now fully implemented
     */
    public void migrateAllSections(GlobalMigrationJob job) {
        log.info("Starting Section migration for all accounts");
        
        try {
            long totalMigrated = sectionMigrator.migrateAllSections(job.getBatchSize(), job.getParallelism());
            
            job.getProcessedRecords().addAndGet(totalMigrated);
            
            log.info("Completed Section migration. Total migrated: {}", totalMigrated);
            
        } catch (Exception e) {
            log.error("Error during Section migration", e);
            job.setError(e.getMessage());
            throw new RuntimeException("Failed to migrate Section data", e);
        }
    }
}
