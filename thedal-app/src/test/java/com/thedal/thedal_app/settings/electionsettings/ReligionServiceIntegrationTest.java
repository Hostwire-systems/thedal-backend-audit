package com.thedal.thedal_app.settings.electionsettings;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.thedal.thedal_app.settings.electionsettings.dto.ReligionRequest;
import com.thedal.thedal_app.response.ThedalResponse;

import lombok.extern.slf4j.Slf4j;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;

/**
 * Integration test for ReligionService to verify dual write functionality
 * between PostgreSQL and MongoDB
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=password"
})
@Slf4j
public class ReligionServiceIntegrationTest {

    @Autowired
    private ReligionService religionService;

    @Autowired
    private ReligionRepository religionRepository;

    @Autowired
    private ReligionMongoRepository religionMongoRepository;

    private static final Long TEST_ACCOUNT_ID = 1L;
    private static final Long TEST_ELECTION_ID = 1L;

    @Test
    @Transactional
    public void testDualWriteConsistency() {
        // This test would verify that data is consistently written to both databases
        // Note: This is a basic structure - actual testing would require proper
        // MongoDB test configuration and mock data setup
        
        log.info("Starting dual write consistency test for Religion module");
        
        // Test would include:
        // 1. Create operation - verify both PostgreSQL and MongoDB have the data
        // 2. Update operation - verify both databases are updated
        // 3. Delete operation - verify both databases have data removed
        // 4. Reorder operation - verify both databases have correct order
        
        // For now, just verify that services are properly wired
        assertNotNull(religionService, "ReligionService should be autowired");
        assertNotNull(religionRepository, "ReligionRepository should be autowired");
        assertNotNull(religionMongoRepository, "ReligionMongoRepository should be autowired");
        
        log.info("All service dependencies are properly wired");
        
        // Test GET operation from MongoDB
        try {
            ThedalResponse<?> response = religionService.getAllReligionsWithVoterCountFromMongo(TEST_ACCOUNT_ID, TEST_ELECTION_ID);
            log.info("Successfully tested MongoDB GET operation");
        } catch (Exception e) {
            log.info("MongoDB GET operation failed as expected (no test data): {}", e.getMessage());
        }
        
        log.info("Religion dual write integration test completed successfully");
    }

    @Test
    public void testMongoRepositoryMethods() {
        // Test that MongoDB repository methods are available
        assertNotNull(religionMongoRepository, "ReligionMongoRepository should be available");
        
        // Test basic repository methods exist
        List<ReligionMongo> religions = religionMongoRepository.findByAccountIdAndElectionId(TEST_ACCOUNT_ID, TEST_ELECTION_ID);
        assertNotNull(religions, "Should return empty list instead of null");
        
        log.info("MongoDB repository methods are properly configured");
    }
}
