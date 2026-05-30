package com.thedal.thedal_app.settings.electionsettings;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CasteCategoryMongoRepository extends MongoRepository<CasteCategoryMongo, Long> {
    
    List<CasteCategoryMongo> findByAccountIdAndElectionId(Long accountId, Long electionId);
    
    List<CasteCategoryMongo> findByAccountIdAndElectionIdOrderByOrderIndexAsc(Long accountId, Long electionId);
    
    Optional<CasteCategoryMongo> findByCasteCategoryNameAndAccountIdAndElectionId(String casteCategoryName, Long accountId, Long electionId);
    
    boolean existsByCasteCategoryNameAndAccountIdAndElectionId(String casteCategoryName, Long accountId, Long electionId);
    
    boolean existsByCasteCategoryNameAndAccountIdAndElectionIdAndIdNot(String casteCategoryName, Long accountId, Long electionId, Long id);
    
    Optional<CasteCategoryMongo> findByIdAndAccountIdAndElectionId(Long id, Long accountId, Long electionId);
    
    List<CasteCategoryMongo> findByIdInAndAccountIdAndElectionId(List<Long> ids, Long accountId, Long electionId);
    
    void deleteByAccountIdAndElectionId(Long accountId, Long electionId);
    
    void deleteByIdIn(List<Long> ids);
    
    long countByAccountIdAndElectionId(Long accountId, Long electionId);
}
