package com.thedal.thedal_app.settings.electionsettings;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.thedal.thedal_app.general.RequestDetailsService;
import com.thedal.thedal_app.response.ThedalResponse;
import com.thedal.thedal_app.response.ThedalSuccess;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class FeedbackIssueService {

    @Autowired
    private FeedbackIssueRepository feedbackIssueRepository;
    
    @Autowired
    private FeedbackIssueMongoRepository feedbackIssueMongoRepository;

    @Autowired
    private RequestDetailsService requestDetails;

    /**
     * Create a new feedback issue with robust dual write
     */
    @Transactional
    public ThedalResponse<FeedbackIssueResponseDTO> createIssue(FeedbackIssueDTO dto, Long electionId) {
        Long accountId = requestDetails.getCurrentAccountId();
        
        if (accountId == null) {
            log.error("Account ID not found, unauthorized access");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        // Validate input
        if (dto.getIssueName() == null || dto.getIssueName().trim().isEmpty()) {
            log.error("Issue name is required for accountId: {}, electionId: {}", accountId, electionId);
            throw new ThedalException(ThedalError.MISSING_REQUIRED_FIELDS, HttpStatus.BAD_REQUEST);
        }

        // Check for duplicates
        if (feedbackIssueRepository.existsByIssueNameAndElectionId(dto.getIssueName(), electionId)) {
            log.error("Feedback issue already exists with name: '{}' for electionId: {}", dto.getIssueName(), electionId);
            throw new ThedalException(ThedalError.ISSUE_ALREADY_EXISTS, HttpStatus.CONFLICT);
        }

        try {
            // Update existing issues' order index
            List<FeedbackIssue> existingIssues = feedbackIssueRepository
                .findByElectionIdAndAccountIdOrderByOrderIndexAsc(electionId, accountId);
            
            for (FeedbackIssue issue : existingIssues) {
                issue.setOrderIndex(issue.getOrderIndex() + 1);
            }
            
            if (!existingIssues.isEmpty()) {
                feedbackIssueRepository.saveAll(existingIssues);
                log.debug("Updated order index for {} existing issues", existingIssues.size());
            }

            // Create new issue
            FeedbackIssue newIssue = new FeedbackIssue();
            newIssue.setIssueName(dto.getIssueName().trim());
            newIssue.setElectionId(electionId);
            newIssue.setAccountId(accountId);
            newIssue.setOrderIndex(0);
            newIssue.setCreatedAt(LocalDateTime.now());

            // Save to PostgreSQL first
            FeedbackIssue savedIssue = feedbackIssueRepository.save(newIssue);
            log.info("Created feedback issue in PostgreSQL: id={}, name='{}', accountId={}, electionId={}", 
                savedIssue.getId(), savedIssue.getIssueName(), accountId, electionId);

            // Save to MongoDB with error handling
            try {
                FeedbackIssueMongo mongoIssue = new FeedbackIssueMongo(savedIssue);
                feedbackIssueMongoRepository.save(mongoIssue);
                log.info("Successfully saved feedback issue to MongoDB: id={}", savedIssue.getId());
                
                // Also update existing issues in MongoDB
                if (!existingIssues.isEmpty()) {
                    List<FeedbackIssueMongo> existingMongoIssues = existingIssues.stream()
                        .map(FeedbackIssueMongo::new)
                        .collect(Collectors.toList());
                    feedbackIssueMongoRepository.saveAll(existingMongoIssues);
                    log.debug("Updated {} existing issues in MongoDB", existingMongoIssues.size());
                }
                
            } catch (Exception mongoEx) {
                log.error("Failed to save feedback issue to MongoDB: id={}, name='{}'. Rolling back transaction.", 
                    savedIssue.getId(), savedIssue.getIssueName(), mongoEx);
                throw new RuntimeException("MongoDB save failed for feedback issue: " + savedIssue.getId(), mongoEx);
            }

            FeedbackIssueResponseDTO response = new FeedbackIssueResponseDTO(
                savedIssue.getId(), 
                savedIssue.getIssueName(), 
                savedIssue.getOrderIndex(), 
                0L
            );
            
            return new ThedalResponse<>(ThedalSuccess.ISSUE_CREATED, response);
            
        } catch (Exception ex) {
            log.error("Failed to create feedback issue: name='{}', accountId={}, electionId={}", 
                dto.getIssueName(), accountId, electionId, ex);
            
            if (ex instanceof ThedalException) {
                throw ex;
            }
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get all feedback issues - reads from MongoDB
     */
    public List<FeedbackIssueResponseDTO> getIssues(Long accountId, Long electionId) {
        log.debug("Fetching feedback issues from MongoDB for accountId: {}, electionId: {}", accountId, electionId);
        
        try {
            List<FeedbackIssueMongo> issuesMongo = feedbackIssueMongoRepository
                .findByAccountIdAndElectionIdOrderByOrderIndexAsc(accountId, electionId);
            
            List<FeedbackIssueResponseDTO> issues = issuesMongo.stream()
                .map(issue -> new FeedbackIssueResponseDTO(
                    issue.getId(),
                    Optional.ofNullable(issue.getIssueName()).orElse(""),
                    Optional.ofNullable(issue.getOrderIndex()).orElse(0),
                    0L // Note: Voter count not available in MongoDB version
                ))
                .collect(Collectors.toList());
            
            log.info("Successfully fetched {} feedback issues from MongoDB", issues.size());
            return issues;
            
        } catch (Exception ex) {
            log.error("Failed to fetch feedback issues from MongoDB for accountId: {}, electionId: {}", 
                accountId, electionId, ex);
            
            // Fallback to PostgreSQL
            log.warn("Falling back to PostgreSQL for feedback issues");
            return feedbackIssueRepository.findByAccountIdAndElectionIdOrderByOrderIndexAsc(accountId, electionId)
                .stream()
                .map(issue -> new FeedbackIssueResponseDTO(
                    issue.getId(),
                    issue.getIssueName(),
                    issue.getOrderIndex(),
                    0L
                ))
                .collect(Collectors.toList());
        }
    }

    /**
     * Get all feedback issues - reads from PostgreSQL only
     */
    public List<FeedbackIssueResponseDTO> getIssuesFromPostgreSQL(Long accountId, Long electionId) {
        log.debug("Fetching feedback issues from PostgreSQL for accountId: {}, electionId: {}", accountId, electionId);
        
        List<FeedbackIssue> issues = feedbackIssueRepository.findByAccountIdAndElectionIdOrderByOrderIndexAsc(accountId, electionId);
        
        List<FeedbackIssueResponseDTO> result = issues.stream()
            .map(issue -> new FeedbackIssueResponseDTO(
                issue.getId(),
                issue.getIssueName() != null ? issue.getIssueName() : "",
                issue.getOrderIndex() != null ? issue.getOrderIndex() : 0,
                0L // TODO: Add voter count calculation if needed
            ))
            .collect(Collectors.toList());
        
        log.info("Successfully fetched {} feedback issues from PostgreSQL", result.size());
        return result;
    }

    /**
     * Update a feedback issue with robust dual write
     */
    @Transactional
    public FeedbackIssueResponseDTO updateIssue(Long accountId, Long electionId, Long issueId, FeedbackIssueDTO dto) {
        
        // Validate input
        if (dto.getIssueName() == null || dto.getIssueName().trim().isEmpty()) {
            log.error("Issue name is required for update: issueId={}, accountId={}, electionId={}", 
                issueId, accountId, electionId);
            throw new ThedalException(ThedalError.MISSING_REQUIRED_FIELDS, HttpStatus.BAD_REQUEST);
        }

        try {
            // Find existing issue
            FeedbackIssue existingIssue = feedbackIssueRepository
                .findByAccountIdAndElectionIdAndId(accountId, electionId, issueId)
                .orElseThrow(() -> {
                    log.error("Feedback issue not found: issueId={}, accountId={}, electionId={}", 
                        issueId, accountId, electionId);
                    return new ThedalException(ThedalError.ISSUE_NOT_FOUND, HttpStatus.NOT_FOUND);
                });

            // Check for duplicate name (excluding current issue)
            if (!existingIssue.getIssueName().equals(dto.getIssueName()) && 
                feedbackIssueRepository.existsByIssueNameAndElectionId(dto.getIssueName(), electionId)) {
                log.error("Feedback issue already exists with name: '{}' for electionId: {}", 
                    dto.getIssueName(), electionId);
                throw new ThedalException(ThedalError.ISSUE_ALREADY_EXISTS, HttpStatus.CONFLICT);
            }

            // Update issue
            existingIssue.setIssueName(dto.getIssueName().trim());
            existingIssue.setCreatedAt(LocalDateTime.now()); // Update timestamp
            
            // Save to PostgreSQL
            FeedbackIssue updatedIssue = feedbackIssueRepository.save(existingIssue);
            log.info("Updated feedback issue in PostgreSQL: id={}, name='{}', accountId={}, electionId={}", 
                updatedIssue.getId(), updatedIssue.getIssueName(), accountId, electionId);

            // Save to MongoDB with error handling
            try {
                FeedbackIssueMongo mongoIssue = new FeedbackIssueMongo(updatedIssue);
                feedbackIssueMongoRepository.save(mongoIssue);
                log.info("Successfully updated feedback issue in MongoDB: id={}", updatedIssue.getId());
                
            } catch (Exception mongoEx) {
                log.error("Failed to update feedback issue in MongoDB: id={}, name='{}'. Rolling back transaction.", 
                    updatedIssue.getId(), updatedIssue.getIssueName(), mongoEx);
                throw new RuntimeException("MongoDB update failed for feedback issue: " + updatedIssue.getId(), mongoEx);
            }

            return new FeedbackIssueResponseDTO(
                updatedIssue.getId(), 
                updatedIssue.getIssueName(), 
                updatedIssue.getOrderIndex(), 
                0L
            );
            
        } catch (Exception ex) {
            log.error("Failed to update feedback issue: issueId={}, name='{}', accountId={}, electionId={}", 
                issueId, dto.getIssueName(), accountId, electionId, ex);
            
            if (ex instanceof ThedalException) {
                throw ex;
            }
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Delete feedback issues with robust dual write
     */
    @Transactional
    public void deleteIssues(Long accountId, Long electionId, List<Long> issueIds) {
        try {
            int pgDeletedCount;
            
            if (issueIds == null || issueIds.isEmpty()) {
                log.info("Deleting all feedback issues for accountId: {}, electionId: {}", accountId, electionId);
                
                // Delete from PostgreSQL
                pgDeletedCount = feedbackIssueRepository.deleteByAccountIdAndElectionId(accountId, electionId);
                
                // Delete from MongoDB
                try {
                    feedbackIssueMongoRepository.deleteByAccountIdAndElectionId(accountId, electionId);
                    log.info("Successfully deleted all feedback issues from both databases: deletedCount={}", pgDeletedCount);
                } catch (Exception mongoEx) {
                    log.error("Failed to delete feedback issues from MongoDB. Rolling back transaction.", mongoEx);
                    throw new RuntimeException("MongoDB delete failed", mongoEx);
                }
                
            } else {
                log.info("Deleting specific feedback issues for accountId: {}, electionId: {}, issueIds: {}", 
                    accountId, electionId, issueIds);
                
                // Delete from PostgreSQL
                pgDeletedCount = feedbackIssueRepository.deleteByAccountIdAndElectionIdAndIdIn(accountId, electionId, issueIds);
                
                // Delete from MongoDB
                try {
                    feedbackIssueMongoRepository.deleteByIdIn(issueIds);
                    log.info("Successfully deleted specific feedback issues from both databases: deletedCount={}, issueIds={}", 
                        pgDeletedCount, issueIds);
                } catch (Exception mongoEx) {
                    log.error("Failed to delete feedback issues from MongoDB: issueIds={}. Rolling back transaction.", 
                        issueIds, mongoEx);
                    throw new RuntimeException("MongoDB delete failed", mongoEx);
                }
            }

            if (pgDeletedCount == 0) {
                log.warn("No feedback issues found to delete for accountId: {}, electionId: {}, issueIds: {}", 
                    accountId, electionId, issueIds);
            }
            
        } catch (Exception ex) {
            log.error("Failed to delete feedback issues: accountId={}, electionId={}, issueIds={}", 
                accountId, electionId, issueIds, ex);
            
            if (ex instanceof RuntimeException && ex.getMessage().contains("MongoDB")) {
                throw ex; // Re-throw to trigger rollback
            }
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Reorder feedback issues with robust dual write
     */
    @Transactional
    public void reorderIssues(List<FeedbackIssueReorderRequest> reorderRequests, Long accountId, Long electionId) {
        if (reorderRequests == null || reorderRequests.isEmpty()) {
            log.error("Reorder requests cannot be null or empty");
            throw new ThedalException(ThedalError.INVALID_REQUEST, HttpStatus.BAD_REQUEST);
        }

        try {
            // Fetch existing issues from PostgreSQL
            List<FeedbackIssue> existingIssues = feedbackIssueRepository
                .findByAccountIdAndElectionIdOrderByOrderIndexAsc(accountId, electionId);

            if (existingIssues.isEmpty()) {
                log.error("No feedback issues found for reordering: accountId={}, electionId={}", accountId, electionId);
                throw new ThedalException(ThedalError.ISSUE_NOT_FOUND, HttpStatus.NOT_FOUND);
            }

            // Create mapping for quick lookup
            Map<Long, Integer> newOrderMap = reorderRequests.stream()
                .collect(Collectors.toMap(
                    FeedbackIssueReorderRequest::getIssueId, 
                    FeedbackIssueReorderRequest::getNewOrderIndex
                ));

            // Sort by new order index
            reorderRequests.sort(Comparator.comparingInt(FeedbackIssueReorderRequest::getNewOrderIndex));

            // Create reordered list
            List<FeedbackIssue> reorderedList = new ArrayList<>(existingIssues);
            reorderedList.removeIf(issue -> newOrderMap.containsKey(issue.getId()));

            // Insert issues at new positions
            for (FeedbackIssueReorderRequest request : reorderRequests) {
                FeedbackIssue issueToMove = existingIssues.stream()
                    .filter(i -> i.getId().equals(request.getIssueId()))
                    .findFirst()
                    .orElseThrow(() -> {
                        log.error("Issue not found for reordering: issueId={}, accountId={}, electionId={}", 
                            request.getIssueId(), accountId, electionId);
                        return new ThedalException(ThedalError.ISSUE_NOT_FOUND, HttpStatus.NOT_FOUND);
                    });

                int newIndex = Math.min(request.getNewOrderIndex(), reorderedList.size());
                reorderedList.add(newIndex, issueToMove);
            }

            // Reassign order indexes
            for (int i = 0; i < reorderedList.size(); i++) {
                reorderedList.get(i).setOrderIndex(i);
            }

            // Save to PostgreSQL
            List<FeedbackIssue> savedIssues = feedbackIssueRepository.saveAll(reorderedList);
            log.info("Successfully reordered {} feedback issues in PostgreSQL", savedIssues.size());

            // Save to MongoDB with error handling
            try {
                List<FeedbackIssueMongo> mongoIssues = savedIssues.stream()
                    .map(FeedbackIssueMongo::new)
                    .collect(Collectors.toList());
                
                feedbackIssueMongoRepository.saveAll(mongoIssues);
                log.info("Successfully reordered {} feedback issues in MongoDB", mongoIssues.size());
                
            } catch (Exception mongoEx) {
                log.error("Failed to reorder feedback issues in MongoDB. Rolling back transaction.", mongoEx);
                throw new RuntimeException("MongoDB reorder failed", mongoEx);
            }

        } catch (Exception ex) {
            log.error("Failed to reorder feedback issues: accountId={}, electionId={}, requests={}", 
                accountId, electionId, reorderRequests.size(), ex);
            
            if (ex instanceof ThedalException) {
                throw ex;
            }
            if (ex instanceof RuntimeException && ex.getMessage().contains("MongoDB")) {
                throw ex; // Re-throw to trigger rollback
            }
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get feedback issues from PostgreSQL (redirected from MongoDB)
     */
    public ThedalResponse<List<Map<String, Object>>> getIssuesFromMongo(Long accountId, Long electionId) {
        log.info("Fetching feedback issues from PostgreSQL for accountId: {}, electionId: {}", accountId, electionId);

        if (accountId == null) {
            log.error("Account ID not found, unauthorized access");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        try {
            List<FeedbackIssue> issues = feedbackIssueRepository
                .findByAccountIdAndElectionIdOrderByOrderIndexAsc(accountId, electionId);

            if (issues.isEmpty()) {
                log.warn("No feedback issues found in PostgreSQL for accountId: {}, electionId: {}", accountId, electionId);
                return new ThedalResponse<>(ThedalSuccess.ISSUES_FETCHED, new ArrayList<>());
            }

            List<Map<String, Object>> issueDetails = issues.stream()
                .map(issue -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("issueId", issue.getId());
                    map.put("issueName", Optional.ofNullable(issue.getIssueName()).orElse(""));
                    map.put("orderIndex", Optional.ofNullable(issue.getOrderIndex()).orElse(0));
                    map.put("accountId", issue.getAccountId());
                    map.put("electionId", issue.getElectionId());
                    map.put("createdAt", issue.getCreatedAt());
                    return map;
                })
                .collect(Collectors.toList());

            log.info("Successfully fetched {} feedback issues from PostgreSQL", issueDetails.size());
            return new ThedalResponse<>(ThedalSuccess.ISSUES_FETCHED, issueDetails);
            
        } catch (Exception ex) {
            log.error("Failed to fetch feedback issues from MongoDB: accountId={}, electionId={}", 
                accountId, electionId, ex);
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}

