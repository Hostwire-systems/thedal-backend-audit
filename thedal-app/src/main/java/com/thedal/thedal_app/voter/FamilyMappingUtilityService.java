package com.thedal.thedal_app.voter;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class FamilyMappingUtilityService {

    @Autowired
    private VoterRepo voterRepository;
    
    @Autowired
    private VoterMongoRepository voterMongoRepository;

    /**
     * Ensures that a specific voter's family mapping is synchronized between PostgreSQL and MongoDB
     */
    @Transactional
    public boolean ensureFamilyMappingSync(String epicNumber, Long electionId, Long accountId) {
        try {
            VoterEntity voter = voterRepository.findByEpicNumberAndElectionIdAndAccountId(epicNumber, accountId, electionId)
                    .orElse(null);
            
            if (voter == null) {
                log.warn("Voter not found for family mapping sync: epicNumber={}, electionId={}, accountId={}", 
                        epicNumber, electionId, accountId);
                return false;
            }
            
            if (voter.getFamilyId() != null) {
                VoterMongo voterMongo = new VoterMongo(voter);
                voterMongoRepository.saveVoterMongoWithNullFields(voterMongo);
                log.debug("Family mapping synced for voter: epicNumber={}, familyId={}", 
                         epicNumber, voter.getFamilyId());
                return true;
            }
            
            return true; // No family mapping to sync
            
        } catch (Exception e) {
            log.error("Failed to ensure family mapping sync for voter: epicNumber={}, electionId={}, accountId={}, error={}", 
                     epicNumber, electionId, accountId, e.getMessage());
            return false;
        }
    }

    /**
     * Ensures that all voters in a family have consistent family mapping in MongoDB
     */
    @Transactional
    public boolean ensureFamilyConsistency(UUID familyId, Long electionId, Long accountId) {
        try {
            var familyMembers = voterRepository.findAllByFamilyIdAndElectionIdAndAccountId(familyId, electionId, accountId);
            
            if (familyMembers.isEmpty()) {
                log.warn("No family members found for familyId: {}, electionId: {}, accountId: {}", 
                        familyId, electionId, accountId);
                return false;
            }
            
            int successCount = 0;
            for (VoterEntity voter : familyMembers) {
                try {
                    VoterMongo voterMongo = new VoterMongo(voter);
                    voterMongoRepository.saveVoterMongoWithNullFields(voterMongo);
                    successCount++;
                } catch (Exception ex) {
                    log.error("Failed to sync family member: epicNumber={}, familyId={}, error={}", 
                             voter.getEpicNumber(), familyId, ex.getMessage());
                }
            }
            
            log.debug("Family consistency ensured: familyId={}, members synced: {}/{}", 
                     familyId, successCount, familyMembers.size());
            
            return successCount == familyMembers.size();
            
        } catch (Exception e) {
            log.error("Failed to ensure family consistency: familyId={}, electionId={}, accountId={}, error={}", 
                     familyId, electionId, accountId, e.getMessage());
            return false;
        }
    }

    /**
     * Validates that a voter's family mapping is consistent between PostgreSQL and MongoDB
     */
    @Transactional(readOnly = true)
    public boolean validateVoterFamilyMapping(String epicNumber, Long electionId, Long accountId) {
        try {
            VoterEntity voter = voterRepository.findByEpicNumberAndElectionIdAndAccountId(epicNumber, accountId, electionId)
                    .orElse(null);
            
            if (voter == null) {
                log.warn("Voter not found for validation: epicNumber={}, electionId={}, accountId={}", 
                        epicNumber, electionId, accountId);
                return false;
            }
            
            VoterMongo voterMongo = voterMongoRepository.findByAccountIdAndElectionIdAndEpicNumber(
                    accountId, electionId, epicNumber);
            
            if (voter.getFamilyId() == null) {
                // No family mapping in PostgreSQL
                return voterMongo == null || voterMongo.getFamilyId() == null;
            }
            
            if (voterMongo == null) {
                log.warn("Voter missing in MongoDB: epicNumber={}, familyId={}", 
                        epicNumber, voter.getFamilyId());
                return false;
            }
            
            boolean familyIdMatch = voter.getFamilyId().equals(voterMongo.getFamilyId());
            boolean familyCountMatch = voter.getFamilyCount() != null ? 
                    voter.getFamilyCount().equals(voterMongo.getFamilyCount()) : 
                    voterMongo.getFamilyCount() == null;
            
            if (!familyIdMatch || !familyCountMatch) {
                log.warn("Family mapping mismatch: epicNumber={}, PG familyId={}, Mongo familyId={}, PG count={}, Mongo count={}", 
                        epicNumber, voter.getFamilyId(), voterMongo.getFamilyId(), 
                        voter.getFamilyCount(), voterMongo.getFamilyCount());
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            log.error("Failed to validate voter family mapping: epicNumber={}, electionId={}, accountId={}, error={}", 
                     epicNumber, electionId, accountId, e.getMessage());
            return false;
        }
    }

    /**
     * Gets the current status of family mapping synchronization for statistics
     */
    @Transactional(readOnly = true)
    public String getFamilyMappingSyncStatus() {
        try {
            long totalVotersWithFamilyMapping = voterRepository.countVotersWithFamilyMappings();
            long totalVotersInMongo = voterMongoRepository.countVotersWithFamilyMappings();
            
            double syncPercentage = totalVotersWithFamilyMapping > 0 ? 
                    (double) totalVotersInMongo / totalVotersWithFamilyMapping * 100 : 100;
            
            return String.format(
                "Family Mapping Sync Status: %.1f%% (%d/%d voters synchronized)", 
                syncPercentage, totalVotersInMongo, totalVotersWithFamilyMapping
            );
            
        } catch (Exception e) {
            log.error("Failed to get family mapping sync status: {}", e.getMessage());
            return "Family Mapping Sync Status: Unable to determine (error occurred)";
        }
    }
}
