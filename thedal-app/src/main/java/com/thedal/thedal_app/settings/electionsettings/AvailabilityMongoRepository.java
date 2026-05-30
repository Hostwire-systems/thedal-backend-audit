package com.thedal.thedal_app.settings.electionsettings;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AvailabilityMongoRepository extends MongoRepository<AvailabilityMongo, Long> {
    
    // Duplicate check methods
    boolean existsByCategoryNameAndElectionId(String categoryName, Long electionId);
    boolean existsByDescriptionAndElectionId(String description, Long electionId);
    boolean existsByCategoryNameAndElectionIdAndIdNot(String categoryName, Long electionId, Long id);
    boolean existsByDescriptionAndElectionIdAndIdNot(String description, Long electionId, Long id);
    
    // Find methods
    List<AvailabilityMongo> findByAccountIdAndElectionId(Long accountId, Long electionId);
    List<AvailabilityMongo> findByAccountIdAndElectionIdOrderByOrderIndexAsc(Long accountId, Long electionId);
    Optional<AvailabilityMongo> findByIdAndAccountIdAndElectionId(Long id, Long accountId, Long electionId);
    Optional<AvailabilityMongo> findByDescription(String description);
    List<AvailabilityMongo> findByIdIn(List<Long> ids);
    
    // Delete methods
    long deleteByAccountIdAndElectionId(Long accountId, Long electionId);
    void deleteByIdIn(List<Long> ids);
    long deleteByIdInAndAccountIdAndElectionId(List<Long> ids, Long accountId, Long electionId);
}