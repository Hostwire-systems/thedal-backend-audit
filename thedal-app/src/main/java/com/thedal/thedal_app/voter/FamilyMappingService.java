package com.thedal.thedal_app.voter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.thedal.thedal_app.election.ElectionEntity;
import com.thedal.thedal_app.election.ElectionRepository;
import com.thedal.thedal_app.general.RequestDetailsService;
import com.thedal.thedal_app.response.ThedalResponse;
import com.thedal.thedal_app.response.ThedalSuccess;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;
import com.thedal.thedal_app.voter.dto.FamilyMappingJobResponseDto;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class FamilyMappingService {

    @Autowired
    private FamilyMappingJobRepository familyMappingJobRepository;

    @Autowired
    private VoterRepo voterRepository;

    @Autowired
    private VoterMongoRepository voterMongoRepository;

    @Autowired
    private ElectionRepository electionRepository;

    @Autowired
    private RequestDetailsService requestDetails;

    @Autowired
    private VoterServiceImpl voterService;

    @Autowired
    private ApplicationContext applicationContext;

    /**
     * Check if a bulk upload is currently in progress for the given election
     */
    public boolean isBulkUploadInProgress(Long electionId, Long accountId) {
        // Check if there's any bulk upload in progress for this election
        // This would need to be implemented based on your bulk upload tracking system
        // For now, returning false as a placeholder
        return false;
    }

    /**
     * Check if family mapping has ever been run for the given election
     */
    public boolean hasFamilyMappingBeenRun(Long electionId, Long accountId) {
        return familyMappingJobRepository.existsByAccountIdAndElectionIdAndRunTrue(accountId, electionId);
    }

    /**
     * Start async family mapping for an entire election
     */
    @Transactional
    public FamilyMappingJobResponseDto startFamilyMapping(Long electionId, Long accountId) {
        // Validate election ownership
        validateElectionOwnership(electionId, accountId);

        // Check if family mapping has ever been run for this election (run=true means it was started at least once)
        boolean hasEverBeenRun = familyMappingJobRepository.existsByAccountIdAndElectionIdAndRunTrue(accountId, electionId);

        if (hasEverBeenRun) {
            log.warn("Family mapping has already been executed for accountId: {}, electionId: {} - not allowed to run again", accountId, electionId);
            throw new ThedalException(ThedalError.BULK_UPLOAD_IN_PROGRESS, HttpStatus.CONFLICT);
        }

        // Get total voter count for progress tracking
        long totalVoters = voterRepository.countByAccountIdAndElectionId(accountId, electionId);
        log.info("Starting family mapping for electionId: {}, accountId: {}, totalVoters: {}", 
                electionId, accountId, totalVoters);

        // Create new family mapping job
        FamilyMappingJobEntity job = new FamilyMappingJobEntity();
        job.setAccountId(accountId);
        job.setElectionId(electionId);
        job.setStatus(BulkUploadStatus.IN_PROGRESS);
        job.setRun(true); // Mark as running - will never be allowed again
        job.setTotalVoters(totalVoters);
        job.setStartTime(LocalDateTime.now());

        job = familyMappingJobRepository.save(job);
        log.info("Created family mapping job with ID: {}", job.getId());

        // Start async processing using Spring proxy to ensure @Async works
        FamilyMappingService proxy = applicationContext.getBean(FamilyMappingService.class);
        proxy.processFamilyMappingAsync(job.getId());

        // Return job response immediately
        FamilyMappingJobResponseDto response = mapToResponseDto(job);
        return response;
    }

    /**
     * Get family mapping job status
     */
    @Transactional(readOnly = true)
    public FamilyMappingJobResponseDto getFamilyMappingJobStatus(Long jobId, Long accountId) {
        FamilyMappingJobEntity job = familyMappingJobRepository.findById(jobId)
                .orElseThrow(() -> new ThedalException(ThedalError.FAMILY_MAPPING_JOB_NOT_FOUND, HttpStatus.NOT_FOUND));

        // Verify ownership
        if (!job.getAccountId().equals(accountId)) {
            log.error("Unauthorized access to family mapping job. JobId: {}, AccountId: {}", jobId, accountId);
            throw new ThedalException(ThedalError.UNAUTHORIZED_ACCESS, HttpStatus.FORBIDDEN);
        }

        FamilyMappingJobResponseDto response = mapToResponseDto(job);
        return response;
    }

    /**
     * Async method to process family mapping
     */
    @Async
    public void processFamilyMappingAsync(Long jobId) {
        FamilyMappingJobEntity job = familyMappingJobRepository.findById(jobId)
                .orElse(null);

        if (job == null) {
            log.error("Family mapping job not found: {}", jobId);
            return;
        }

        try {
            log.info("Starting async family mapping process for jobId: {}", jobId);
            processFamilyMappingForElection(job);
        } catch (Exception e) {
            log.error("Error in async family mapping for jobId: {}", jobId, e);
            markJobAsFailed(job, e.getMessage());
        }
    }

    /**
     * Core family mapping logic - processes voters by house number
     */
    @Transactional
    public void processFamilyMappingForElection(FamilyMappingJobEntity job) {
        Long accountId = job.getAccountId();
        Long electionId = job.getElectionId();
        Long jobId = job.getId();

        log.info("Processing family mapping for accountId: {}, electionId: {}, jobId: {}", accountId, electionId, jobId);

        try {
            // Delegate to the updated VoterServiceImpl method with job tracking
            voterService.mapFamiliesByHouseNumber(electionId, accountId, jobId);
            
            log.info("Family mapping completed successfully for jobId: {}", jobId);
            
        } catch (Exception e) {
            log.error("Error processing family mapping for jobId: {}", jobId, e);
            markJobAsFailed(job, e.getMessage());
            throw e;
        }
    }

    /**
     * Process voters in a family - assign family ID, count, and sequence number
     */
    private UUID processVoterFamily(List<VoterEntity> voters, Long accountId, Long electionId) {
        if (voters.isEmpty()) {
            return null;
        }

        // Check if any voter already has a family ID
        UUID familyId = voters.stream()
                .filter(v -> v.getFamilyId() != null)
                .findFirst()
                .map(VoterEntity::getFamilyId)
                .orElse(UUID.randomUUID());

        // Check if family already has a sequence number, otherwise assign new one
        Integer sequenceNumber = voters.stream()
                .filter(v -> v.getFamilySequenceNumber() != null)
                .findFirst()
                .map(VoterEntity::getFamilySequenceNumber)
                .orElseGet(() -> {
                    // Get next sequence number for new families
                    Integer maxSequence = voterRepository.getMaxFamilySequenceNumber(accountId, electionId);
                    return maxSequence + 1;
                });

        int familyCount = voters.size();

        // Update all voters with family ID, count, and sequence number
        for (VoterEntity voter : voters) {
            voter.setFamilyId(familyId);
            voter.setFamilyCount(familyCount);
            voter.setFamilySequenceNumber(sequenceNumber);  // NEW: Assign sequence number
            voterRepository.save(voter);

            // TODO: Sync to MongoDB - commented out for performance
            // syncVoterToMongoDB(voter);
        }

        return familyId;
    }

    /**
     * Sync voter data to MongoDB
     */
    private void syncVoterToMongoDB(VoterEntity voter) {
        try {
            Optional<VoterMongo> voterMongoOpt = voterMongoRepository.findById(voter.getId().toString());
            VoterMongo voterMongo = voterMongoOpt.orElse(new VoterMongo(voter));
            
            // Update family fields
            voterMongo.setFamilyId(voter.getFamilyId());
            voterMongo.setFamilyCount(voter.getFamilyCount());
            
            voterMongoRepository.save(voterMongo);
            log.debug("Synced voter {} to MongoDB with familyId: {}", voter.getEpicNumber(), voter.getFamilyId());
        } catch (Exception e) {
            log.error("Failed to sync voter {} to MongoDB: {}", voter.getEpicNumber(), e.getMessage());
        }
    }

    /**
     * Update job progress
     */
    private void updateJobProgress(FamilyMappingJobEntity job, long processedVoters, long familiesCreated, long votersWithFamilies) {
        job.setProcessedVoters(processedVoters);
        job.setFamiliesCreated(familiesCreated);
        job.setVotersWithFamilies(votersWithFamilies);
        familyMappingJobRepository.save(job);
    }

    /**
     * Mark job as failed
     */
    private void markJobAsFailed(FamilyMappingJobEntity job, String errorMessage) {
        job.setStatus(BulkUploadStatus.FAILED);
        job.setErrorMessage(errorMessage);
        job.setEndTime(LocalDateTime.now());
        familyMappingJobRepository.save(job);
    }

    /**
     * Validate election ownership
     */
    private void validateElectionOwnership(Long electionId, Long accountId) {
        Optional<ElectionEntity> electionOpt = electionRepository.findByIdAndAccountId(electionId, accountId);
        if (!electionOpt.isPresent()) {
            log.error("Election ID {} does not belong to Account ID {}", electionId, accountId);
            throw new ThedalException(ThedalError.INVALID_ELECTION, HttpStatus.FORBIDDEN);
        }
    }

    /**
     * Map entity to response DTO
     */
    private FamilyMappingJobResponseDto mapToResponseDto(FamilyMappingJobEntity job) {
        FamilyMappingJobResponseDto dto = new FamilyMappingJobResponseDto();
        dto.setJobId(job.getId());
        dto.setAccountId(job.getAccountId());
        dto.setElectionId(job.getElectionId());
        dto.setStatus(job.getStatus());
        dto.setRun(job.getRun());
        dto.setStartTime(job.getStartTime());
        dto.setEndTime(job.getEndTime());
        dto.setTotalVoters(job.getTotalVoters());
        dto.setProcessedVoters(job.getProcessedVoters());
        dto.setFamiliesCreated(job.getFamiliesCreated());
        dto.setVotersWithFamilies(job.getVotersWithFamilies());
        dto.setUniqueHouseNumbers(job.getUniqueHouseNumbers());
        dto.setProgressPercentage(job.getProgressPercentage());
        dto.setErrorMessage(job.getErrorMessage());
        dto.setCreatedAt(job.getCreatedAt());
        dto.setUpdatedAt(job.getUpdatedAt());
        return dto;
    }
}
