package com.thedal.thedal_app.user;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface MongoUserRepository extends MongoRepository<MongoUser, String> {
    
    // Find by PostgreSQL user ID
    Optional<MongoUser> findByUserId(Long userId);
    List<MongoUser> findAllByUserId(Long userId); // For finding duplicates
    MongoUser findFirstByUserId(Long userId); // Get first match to handle duplicates
    void deleteByUserId(Long userId);
    
    // Existing methods
    MongoUser findByUsername(String username);
    Page<MongoUser> findByUsernameContainingIgnoreCase(String username, Pageable pageable);
    Page<MongoUser> findByIsActive(Boolean isActive, Pageable pageable);
    Page<MongoUser> findByUsernameContainingIgnoreCaseAndIsActive(String username, Boolean isActive, Pageable pageable);
    Page<MongoUser> findAll(Pageable pageable);
    
    Page<MongoUser> findByMobileNumberContainingIgnoreCase(String mobileNumber, Pageable pageable);
    Page<MongoUser> findByMobileNumberContainingIgnoreCaseAndIsActive(String mobileNumber, Boolean isActive, Pageable pageable);
    
    // Account-based queries
    List<MongoUser> findByAccountId(Long accountId);
    Page<MongoUser> findByAccountId(Long accountId, Pageable pageable);
    Page<MongoUser> findByAccountIdAndIsActive(Long accountId, Boolean isActive, Pageable pageable);
    
    // Search queries with filters
    @Query("{ 'accountId': ?0, $and: [ " +
           "{ $or: [ " +
           "  { 'firstName': { $regex: ?1, $options: 'i' } }, " +
           "  { 'lastName': { $regex: ?1, $options: 'i' } }, " +
           "  { 'username': { $regex: ?1, $options: 'i' } } " +
           "] }, " +
           "{ $or: [ { 'isActive': ?2 }, { $expr: { $eq: [?2, null] } } ] } " +
           "] }")
    Page<MongoUser> findByAccountIdWithFilters(Long accountId, String searchTerm, Boolean isActive, Pageable pageable);
    
    // Migration methods
    long countByAccountId(Long accountId);
    void deleteByAccountId(Long accountId);
    
    
    Page<MongoUser> findByFirstNameOrLastNameContainingIgnoreCase(String filter, Pageable pageable);
    
	Page<MongoUser> findByFirstNameOrLastNameContainingIgnoreCaseAndIsActive(
            String filter, Boolean isActive, Pageable pageable);
	
	List<MongoUser> findByCreatedBy(String string);
    
}
