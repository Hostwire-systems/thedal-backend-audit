package com.thedal.thedal_app.settings.electionsettings;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface FeedbackIssueMongoRepository extends MongoRepository<FeedbackIssueMongo, Long> {
    
    // Find by account and election with ordering
    List<FeedbackIssueMongo> findByAccountIdAndElectionIdOrderByOrderIndexAsc(Long accountId, Long electionId);
    
    // Find by account and election
    List<FeedbackIssueMongo> findByAccountIdAndElectionId(Long accountId, Long electionId);
    
    // Find specific issue by ID, account, and election
    Optional<FeedbackIssueMongo> findByIdAndAccountIdAndElectionId(Long id, Long accountId, Long electionId);
    
    // Check if issue exists by name and election
    boolean existsByIssueNameAndElectionId(String issueName, Long electionId);
    
    // Delete operations
    void deleteByAccountIdAndElectionId(Long accountId, Long electionId);
    void deleteByIdIn(List<Long> ids);
    
    // Count for validation
    long countByAccountIdAndElectionId(Long accountId, Long electionId);
    
    // Custom query for voter relationships
    @Query("{ '_id': { $in: ?0 }, 'accountId': ?1, 'electionId': ?2 }")
    List<FeedbackIssueMongo> findByVoterIdsAndAccountIdAndElectionId(List<Long> voterIds, Long accountId, Long electionId);
    
}
