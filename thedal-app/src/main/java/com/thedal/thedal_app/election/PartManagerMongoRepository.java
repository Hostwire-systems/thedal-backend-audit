package com.thedal.thedal_app.election;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PartManagerMongoRepository extends MongoRepository<PartManagerMongo, Long> {
    
    @Query("{ 'accountId': ?0, 'electionId': ?1 }")
    List<PartManagerMongo> findByAccountIdAndElectionId(Long accountId, Long electionId);
    
    @Query("{ 'accountId': ?0, 'electionId': ?1 }")
    Page<PartManagerMongo> findByAccountIdAndElectionId(Long accountId, Long electionId, Pageable pageable);
    
    @Query("{ 'electionId': ?0, 'accountId': ?1, 'partNo': { $in: ?2 } }")
    Page<PartManagerMongo> findByElectionIdAndAccountIdAndPartNoIn(Long electionId, Long accountId, List<String> partNos, Pageable pageable);
    
    @Query("{ 'electionId': ?0, 'accountId': ?1, 'partNo': { $in: ?2 } }")
    List<PartManagerMongo> findByElectionIdAndAccountIdAndPartNoIn(Long electionId, Long accountId, List<String> partNos);
    
    @Query("{ 'partNo': ?0, 'accountId': ?1, 'electionId': ?2 }")
    Optional<PartManagerMongo> findByPartNoAndAccountIdAndElectionId(String partNo, Long accountId, Long electionId);
    
    @Query("{ 'accountId': ?0 }")
    List<PartManagerMongo> findByAccountId(Long accountId);
    
    boolean existsByPartNoAndAccountIdAndElectionId(String partNo, Long accountId, Long electionId);
    
    void deleteByAccountIdAndElectionId(Long accountId, Long electionId);
    
    void deleteByIdIn(List<Long> ids);
}
