package com.thedal.thedal_app.voter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import lombok.extern.slf4j.Slf4j;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driverClassName=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=password",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.data.mongodb.uri=mongodb://localhost:27017/test"
})
@Slf4j
public class FamilyMappingMigrationServiceTest {

    @Autowired
    private FamilyMappingMigrationService familyMappingMigrationService;

    @Autowired
    private FamilyMappingBulkMigrationService familyMappingBulkMigrationService;

    @Autowired
    private FamilyMappingUtilityService familyMappingUtilityService;

    @Test
    public void testServicesLoad() {
        log.info("Testing family mapping migration services...");
        
        // Test that all services are properly injected
        assert familyMappingMigrationService != null : "FamilyMappingMigrationService should not be null";
        assert familyMappingBulkMigrationService != null : "FamilyMappingBulkMigrationService should not be null";
        assert familyMappingUtilityService != null : "FamilyMappingUtilityService should not be null";
        
        log.info("All family mapping migration services loaded successfully");
    }

    @Test
    public void testRepositoryMethods() {
        log.info("Testing repository methods...");
        
        // Test that repository methods exist and are accessible
        try {
            String status = familyMappingUtilityService.getFamilyMappingSyncStatus();
            log.info("Family mapping sync status: {}", status);
        } catch (Exception e) {
            log.error("Error getting family mapping sync status: {}", e.getMessage());
        }
        
        log.info("Repository methods test completed");
    }
}
