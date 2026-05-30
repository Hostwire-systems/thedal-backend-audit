package com.thedal.thedal_app.election;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface ElectionMongoRepository extends MongoRepository<ElectionMongo, Long> {
    
    // Find elections by account
    List<ElectionMongo> findByAccountId(Long accountId);
    
    // Find active elections by account
    List<ElectionMongo> findByAccountIdAndIsDeletedFalse(Long accountId);
    
    // Find election by ID and account
    Optional<ElectionMongo> findByIdAndAccountId(Long id, Long accountId);
    
    // Find election by ID, account, and not deleted
    Optional<ElectionMongo> findByIdAndAccountIdAndIsDeletedFalse(Long id, Long accountId);
    
    // Find elections by account ordered by orderIndex
    List<ElectionMongo> findByAccountIdAndIsDeletedFalseOrderByOrderIndexAsc(Long accountId);
    
    // Check if election exists by name and account
    boolean existsByElectionNameAndAccountIdAndIsDeletedFalse(String electionName, Long accountId);
    
    // Check if election exists by name, account excluding specific ID
    boolean existsByElectionNameAndAccountIdAndIsDeletedFalseAndIdNot(String electionName, Long accountId, Long excludeId);
    
    // Delete operations
    void deleteByIdIn(List<Long> ids);
    void deleteByAccountId(Long accountId);
    
    // Find elections by IDs and account
    List<ElectionMongo> findByIdInAndAccountId(List<Long> ids, Long accountId);
    
    // Custom queries for complex operations
    @Query("{'accountId': ?0, 'isDeleted': false}")
    List<ElectionMongo> findAllActiveElectionsByAccount(Long accountId);
    
    @Query("{'accountId': ?0, 'electionName': {$regex: ?1, $options: 'i'}, 'isDeleted': false}")
    List<ElectionMongo> findByElectionNameContainingIgnoreCaseAndAccountId(String electionName, Long accountId);
    
    // Count methods
    long countByAccountIdAndIsDeletedFalse(Long accountId);
}
