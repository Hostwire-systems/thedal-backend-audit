package com.thedal.thedal_app.voter.activity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.thedal.thedal_app.general.RequestDetailsService;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;
import com.thedal.thedal_app.voter.VoterEntity;
import com.thedal.thedal_app.voter.VoterRepo;
import com.thedal.thedal_app.voter.activity.dto.ActivityCountResponse;
import com.thedal.thedal_app.voter.activity.dto.ActivityHistoryItem;
import com.thedal.thedal_app.voter.activity.dto.ActivityHistoryResponse;
import com.thedal.thedal_app.voter.activity.dto.ActivityTypeSummary;
import com.thedal.thedal_app.voter.activity.dto.ElectionActivitySummary;
import com.thedal.thedal_app.voter.activity.dto.MostActiveVoter;
import com.thedal.thedal_app.voter.activity.dto.RecordActivityRequest;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class VoterActivityService {
    
    @Autowired
    private VoterActivityLogRepository activityLogRepository;
    
    @Autowired
    private VoterRepo voterRepository;
    
    @Autowired
    private RequestDetailsService requestDetails;
    
    /**
     * Record activity - Uses async logging for maximum performance
     * Updates both counter in VoterEntity and creates log entry
     */
    @Transactional
    public void recordActivity(Long electionId, RecordActivityRequest request) {
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account ID is missing in the request header.");
            throw new ThedalException(ThedalError.UNAUTHORIZED_ACCESS, HttpStatus.UNAUTHORIZED);
        }
        
        // Find voter
        VoterEntity voter = voterRepository.findByAccountIdAndElectionIdAndVoterId(
            accountId, electionId, request.getVoterId())
            .orElseThrow(() -> new ThedalException(ThedalError.VOTER_NOT_FOUND, HttpStatus.NOT_FOUND));
        
        // Update counter in VoterEntity for fast reads
        incrementActivityCounter(voter, request.getActivityType());
        voterRepository.save(voter);
        
        // Async log activity for audit trail (doesn't block response)
        logActivityAsync(accountId, electionId, request);
        
        log.info("Activity recorded: voter={}, type={}, election={}", 
            request.getVoterId(), request.getActivityType(), electionId);
    }
    
    /**
     * Increment the appropriate counter based on activity type
     */
    private void incrementActivityCounter(VoterEntity voter, ActivityType activityType) {
        switch (activityType) {
            case VOTER_SLIP_PRINT:
                voter.setVoterSlipPrintCount(voter.getVoterSlipPrintCount() + 1);
                break;
            case FAMILY_SLIP_PRINT:
                voter.setFamilySlipPrintCount(voter.getFamilySlipPrintCount() + 1);
                break;
            case BENEFIT_SLIP_PRINT:
                voter.setBenefitSlipPrintCount(voter.getBenefitSlipPrintCount() + 1);
                break;
            case WHATSAPP_SHARE:
                voter.setWhatsappShareCount(voter.getWhatsappShareCount() + 1);
                break;
            case SMS_SHARE:
                voter.setSmsShareCount(voter.getSmsShareCount() + 1);
                break;
            case VOICE_SHARE:
                voter.setVoiceShareCount(voter.getVoiceShareCount() + 1);
                break;
        }
    }
    
    /**
     * Async method to log activity - doesn't block the main request
     */
    @Async
    @Transactional
    protected void logActivityAsync(Long accountId, Long electionId, RecordActivityRequest request) {
        try {
            VoterActivityLog log = new VoterActivityLog();
            log.setAccountId(accountId);
            log.setElectionId(electionId);
            log.setVoterId(request.getVoterId());
            log.setActivityType(request.getActivityType());
            log.setActivityTime(LocalDateTime.now());
            log.setVolunteerId(request.getVolunteerId());
            log.setTemplateId(request.getTemplateId());
            log.setMetadata(request.getMetadata());
            
            activityLogRepository.save(log);
        } catch (Exception e) {
            log.error("Failed to log activity asynchronously: voter={}, type={}", 
                request.getVoterId(), request.getActivityType(), e);
            // Don't throw - async logging failure shouldn't affect main operation
        }
    }
    
    /**
     * Get activity counts from VoterEntity (fastest - no joins)
     */
    public ActivityCountResponse getActivityCounts(Long electionId, String voterId) {
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            throw new ThedalException(ThedalError.UNAUTHORIZED_ACCESS, HttpStatus.UNAUTHORIZED);
        }
        
        VoterEntity voter = voterRepository.findByAccountIdAndElectionIdAndVoterId(
            accountId, electionId, voterId)
            .orElseThrow(() -> new ThedalException(ThedalError.VOTER_NOT_FOUND, HttpStatus.NOT_FOUND));
        
        Integer total = voter.getVoterSlipPrintCount() + 
                       voter.getFamilySlipPrintCount() + 
                       voter.getBenefitSlipPrintCount() + 
                       voter.getWhatsappShareCount() + 
                       voter.getSmsShareCount() + 
                       voter.getVoiceShareCount();
        
        return new ActivityCountResponse(
            voterId,
            voter.getVoterSlipPrintCount(),
            voter.getFamilySlipPrintCount(),
            voter.getBenefitSlipPrintCount(),
            voter.getWhatsappShareCount(),
            voter.getSmsShareCount(),
            voter.getVoiceShareCount(),
            total
        );
    }
    
    /**
     * Get activity history with pagination
     */
    public ActivityHistoryResponse getActivityHistory(
            Long electionId, 
            String voterId, 
            ActivityType activityType,
            int page, 
            int size) {
        
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            throw new ThedalException(ThedalError.UNAUTHORIZED_ACCESS, HttpStatus.UNAUTHORIZED);
        }
        
        Pageable pageable = PageRequest.of(page, size);
        Page<VoterActivityLog> activityPage = activityLogRepository.getActivityHistory(
            accountId, electionId, voterId, activityType, pageable);
        
        List<ActivityHistoryItem> items = activityPage.getContent().stream()
            .map(log -> new ActivityHistoryItem(
                log.getId(),
                log.getVoterId(),
                log.getActivityType(),
                log.getActivityTime(),
                log.getVolunteerId(),
                log.getTemplateId(),
                log.getMetadata()
            ))
            .collect(Collectors.toList());
        
        return new ActivityHistoryResponse(
            voterId,
            items,
            (int) activityPage.getTotalElements(),
            page,
            size,
            activityPage.getTotalPages()
        );
    }
    
    /**
     * Get election-wide activity summary
     */
    public ElectionActivitySummary getElectionActivitySummary(Long electionId, Integer topVotersLimit) {
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            throw new ThedalException(ThedalError.UNAUTHORIZED_ACCESS, HttpStatus.UNAUTHORIZED);
        }
        
        // Get activity summary by type
        List<Object[]> summaryData = activityLogRepository.getElectionActivitySummary(accountId, electionId);
        
        List<ActivityTypeSummary> activitySummaries = summaryData.stream()
            .map(row -> new ActivityTypeSummary(
                (String) row[0],  // activity_type
                ((Number) row[1]).longValue(),  // count
                ((Number) row[2]).longValue()   // unique_voters
            ))
            .collect(Collectors.toList());
        
        // Get most active voters
        int limit = topVotersLimit != null ? topVotersLimit : 10;
        List<Object[]> mostActiveData = activityLogRepository.getMostActiveVoters(
            accountId, electionId, null, limit);
        
        List<MostActiveVoter> mostActiveVoters = mostActiveData.stream()
            .map(row -> new MostActiveVoter(
                (String) row[0],  // voter_id
                ((Number) row[1]).longValue()  // activity_count
            ))
            .collect(Collectors.toList());
        
        // Calculate totals
        Long totalActivities = activitySummaries.stream()
            .mapToLong(ActivityTypeSummary::getTotalCount)
            .sum();
        
        Long totalUniqueVoters = activitySummaries.stream()
            .mapToLong(ActivityTypeSummary::getUniqueVoters)
            .max()
            .orElse(0L);
        
        return new ElectionActivitySummary(
            electionId,
            activitySummaries,
            mostActiveVoters,
            totalActivities,
            totalUniqueVoters
        );
    }
    
    /**
     * Batch record activities for bulk operations (e.g., bulk WhatsApp send)
     */
    @Transactional
    public void recordBatchActivities(Long electionId, List<RecordActivityRequest> requests) {
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            throw new ThedalException(ThedalError.UNAUTHORIZED_ACCESS, HttpStatus.UNAUTHORIZED);
        }
        
        // Group by voter ID for efficient updates
        Map<String, List<RecordActivityRequest>> voterActivities = requests.stream()
            .collect(Collectors.groupingBy(RecordActivityRequest::getVoterId));
        
        // Process each voter's activities
        for (Map.Entry<String, List<RecordActivityRequest>> entry : voterActivities.entrySet()) {
            String voterId = entry.getKey();
            List<RecordActivityRequest> voterRequests = entry.getValue();
            
            try {
                VoterEntity voter = voterRepository.findByAccountIdAndElectionIdAndVoterId(
                    accountId, electionId, voterId)
                    .orElseThrow(() -> new ThedalException(ThedalError.VOTER_NOT_FOUND, HttpStatus.NOT_FOUND));
                
                // Increment all counters for this voter
                for (RecordActivityRequest request : voterRequests) {
                    incrementActivityCounter(voter, request.getActivityType());
                }
                
                voterRepository.save(voter);
                
                // Async log all activities
                logBatchActivitiesAsync(accountId, electionId, voterRequests);
                
            } catch (Exception e) {
                log.error("Failed to record batch activities for voter={}", voterId, e);
                // Continue with next voter
            }
        }
        
        log.info("Batch activities recorded: {} voters, {} total activities", 
            voterActivities.size(), requests.size());
    }
    
    @Async
    @Transactional
    protected void logBatchActivitiesAsync(Long accountId, Long electionId, List<RecordActivityRequest> requests) {
        try {
            List<VoterActivityLog> logs = requests.stream()
                .map(request -> {
                    VoterActivityLog log = new VoterActivityLog();
                    log.setAccountId(accountId);
                    log.setElectionId(electionId);
                    log.setVoterId(request.getVoterId());
                    log.setActivityType(request.getActivityType());
                    log.setActivityTime(LocalDateTime.now());
                    log.setVolunteerId(request.getVolunteerId());
                    log.setTemplateId(request.getTemplateId());
                    log.setMetadata(request.getMetadata());
                    return log;
                })
                .collect(Collectors.toList());
            
            activityLogRepository.saveAll(logs);
        } catch (Exception e) {
            log.error("Failed to log batch activities asynchronously", e);
        }
    }
}
