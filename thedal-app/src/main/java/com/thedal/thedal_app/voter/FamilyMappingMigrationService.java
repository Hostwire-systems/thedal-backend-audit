package com.thedal.thedal_app.voter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class FamilyMappingMigrationService {

    @Autowired
    private VoterRepo voterRepository;
    
    @Autowired
    private VoterMongoRepository voterMongoRepository;
    
    private static final int BATCH_SIZE = 500;

    /**
     * Migrates all family mapping data from PostgreSQL to MongoDB for a specific election
     */
    @Transactional
    public String migrateFamilyMappingForElection(Long electionId, Long accountId) {
        log.info("Starting family mapping migration for electionId: {}, accountId: {}", electionId, accountId);
        
        AtomicInteger totalProcessed = new AtomicInteger(0);
        AtomicInteger familyMappingsFound = new AtomicInteger(0);
        AtomicInteger mongoUpdates = new AtomicInteger(0);
        
        try {
            int page = 0;
            boolean hasMoreData = true;

            while (hasMoreData) {
                Pageable pageable = PageRequest.of(page, BATCH_SIZE);
                Page<VoterEntity> voterPage = voterRepository.findByAccountIdAndElectionId(accountId, electionId, pageable);

                if (voterPage.isEmpty()) {
                    hasMoreData = false;
                    break;
                }

                // Process batch
                for (VoterEntity voter : voterPage.getContent()) {
                    totalProcessed.incrementAndGet();
                    
                    // Only process voters that have family mapping
                    if (voter.getFamilyId() != null) {
                        familyMappingsFound.incrementAndGet();
                        
                        try {
                            // Create/Update MongoDB document
                            VoterMongo voterMongo = new VoterMongo(voter);
                            voterMongoRepository.saveVoterMongoWithNullFields(voterMongo);
                            mongoUpdates.incrementAndGet();
                            
                            if (mongoUpdates.get() % 100 == 0) {
                                log.info("Processed {} family mappings so far...", mongoUpdates.get());
                            }
                            
                        } catch (Exception ex) {
                            log.error("Failed to sync voter to MongoDB: epicNumber={}, familyId={}, error={}", 
                                     voter.getEpicNumber(), voter.getFamilyId(), ex.getMessage());
                        }
                    }
                }

                page++;
                log.debug("Completed page {} with {} voters", page, voterPage.getNumberOfElements());
            }

            String result = String.format(
                "Family mapping migration completed successfully. " +
                "Total voters processed: %d, Family mappings found: %d, MongoDB updates: %d",
                totalProcessed.get(), familyMappingsFound.get(), mongoUpdates.get()
            );
            
            log.info(result);
            return result;
            
        } catch (Exception e) {
            String error = String.format("Family mapping migration failed: %s", e.getMessage());
            log.error(error, e);
            throw new RuntimeException(error, e);
        }
    }

    /**
     * Migrates family mapping data for all elections under an account
     */
    @Transactional
    public String migrateFamilyMappingForAccount(Long accountId) {
        log.info("Starting family mapping migration for accountId: {}", accountId);
        
        try {
            // Get all unique election IDs for this account that have family mappings
            List<Object[]> electionIds = voterRepository.findDistinctElectionIdsWithFamilyMappings(accountId);
            
            StringBuilder results = new StringBuilder();
            int totalElections = electionIds.size();
            
            for (int i = 0; i < totalElections; i++) {
                Long electionId = (Long) electionIds.get(i)[0];
                log.info("Processing election {}/{}: electionId={}", i + 1, totalElections, electionId);
                
                String electionResult = migrateFamilyMappingForElection(electionId, accountId);
                results.append(String.format("Election %d: %s\n", electionId, electionResult));
            }
            
            String finalResult = String.format(
                "Family mapping migration completed for account %d. Processed %d elections.\n%s",
                accountId, totalElections, results.toString()
            );
            
            log.info(finalResult);
            return finalResult;
            
        } catch (Exception e) {
            String error = String.format("Family mapping migration failed for account %d: %s", accountId, e.getMessage());
            log.error(error, e);
            throw new RuntimeException(error, e);
        }
    }

    /**
     * Validates family mapping consistency between PostgreSQL and MongoDB
     */
    @Transactional
    public String validateFamilyMappingConsistency(Long electionId, Long accountId) {
        log.info("Validating family mapping consistency for electionId: {}, accountId: {}", electionId, accountId);
        
        try {
            AtomicInteger totalVoters = new AtomicInteger(0);
            AtomicInteger familyMappedVoters = new AtomicInteger(0);
            AtomicInteger consistentMappings = new AtomicInteger(0);
            AtomicInteger inconsistentMappings = new AtomicInteger(0);
            AtomicInteger missingInMongo = new AtomicInteger(0);
            
            int page = 0;
            boolean hasMoreData = true;

            while (hasMoreData) {
                Pageable pageable = PageRequest.of(page, BATCH_SIZE);
                Page<VoterEntity> voterPage = voterRepository.findByAccountIdAndElectionId(accountId, electionId, pageable);

                if (voterPage.isEmpty()) {
                    hasMoreData = false;
                    break;
                }

                for (VoterEntity voter : voterPage.getContent()) {
                    totalVoters.incrementAndGet();
                    
                    if (voter.getFamilyId() != null) {
                        familyMappedVoters.incrementAndGet();
                        
                        // Check MongoDB
                        VoterMongo voterMongo = voterMongoRepository.findByAccountIdAndElectionIdAndEpicNumber(
                            accountId, electionId, voter.getEpicNumber());
                        
                        if (voterMongo == null) {
                            missingInMongo.incrementAndGet();
                            log.warn("Voter missing in MongoDB: epicNumber={}, familyId={}", 
                                   voter.getEpicNumber(), voter.getFamilyId());
                        } else {
                            UUID pgFamilyId = voter.getFamilyId();
                            UUID mongoFamilyId = voterMongo.getFamilyId();
                            Integer pgFamilyCount = voter.getFamilyCount();
                            Integer mongoFamilyCount = voterMongo.getFamilyCount();
                            
                            if (pgFamilyId.equals(mongoFamilyId) && 
                                (pgFamilyCount != null ? pgFamilyCount.equals(mongoFamilyCount) : mongoFamilyCount == null)) {
                                consistentMappings.incrementAndGet();
                            } else {
                                inconsistentMappings.incrementAndGet();
                                log.warn("Inconsistent family mapping: epicNumber={}, PG familyId={}, Mongo familyId={}, PG count={}, Mongo count={}", 
                                       voter.getEpicNumber(), pgFamilyId, mongoFamilyId, pgFamilyCount, mongoFamilyCount);
                            }
                        }
                    }
                }
                page++;
            }

            String result = String.format(
                "Family mapping validation completed. Total voters: %d, Family mapped: %d, " +
                "Consistent: %d, Inconsistent: %d, Missing in MongoDB: %d",
                totalVoters.get(), familyMappedVoters.get(), consistentMappings.get(), 
                inconsistentMappings.get(), missingInMongo.get()
            );
            
            log.info(result);
            return result;
            
        } catch (Exception e) {
            String error = String.format("Family mapping validation failed: %s", e.getMessage());
            log.error(error, e);
            throw new RuntimeException(error, e);
        }
    }
}