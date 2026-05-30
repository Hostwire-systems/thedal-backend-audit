package com.thedal.thedal_app.cpanel;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface SlipBoxMongoRepository extends MongoRepository<SlipBoxMongo, Long> {
    List<SlipBoxMongo> findByAccountIdAndElectionId(Long accountId, Long electionId);
    List<SlipBoxMongo> findByAccountIdAndElectionIdOrderByIdAsc(Long accountId, Long electionId);
    
    void deleteByAccountIdAndElectionId(Long accountId, Long electionId);
    void deleteByIdIn(List<Long> ids);
    
    List<SlipBoxMongo> findBySlipBoxIdInAndAccountIdAndElectionId(List<String> slipBoxIds, Long accountId, Long electionId);
    
    // Enhanced methods for robust dual write operations
    Optional<SlipBoxMongo> findBySlipBoxIdAndAccountIdAndElectionId(String slipBoxId, Long accountId, Long electionId);
    List<SlipBoxMongo> findByIdInAndAccountIdAndElectionId(List<Long> ids, Long accountId, Long electionId);
    boolean existsBySlipBoxIdAndAccountIdAndElectionId(String slipBoxId, Long accountId, Long electionId);
    boolean existsBySlipBoxIdAndElectionId(String slipBoxId, Long electionId);
    
    @Query("{ '_id': { $in: ?0 }, 'accountId': ?1, 'electionId': ?2 }")
    List<SlipBoxMongo> findBySlipBoxIdsAndAccountIdAndElectionId(List<Long> slipBoxIds, Long accountId, Long electionId);

    @Query("{ 'slipBoxId': { $in: ?0 } }")
    List<SlipBoxMongo> findBySlipBoxIdIn(List<String> slipBoxIds);
    
    // For finding default slip boxes
    List<SlipBoxMongo> findByAccountIdAndElectionIdAndIsDefaultTrue(Long accountId, Long electionId);
    boolean existsByAccountIdAndElectionIdAndIsDefault(Long accountId, Long electionId, boolean isDefault);
    
    // For cPanel operations (accountId = 0, electionId = 0)
    List<SlipBoxMongo> findBySlipBoxIdInAndElectionId(List<String> slipBoxIds, Long electionId);
    
    Optional<SlipBoxMongo> findByIdAndAccountIdAndElectionId(Long id, Long accountId, Long electionId);
    ///////////////////////////////////
    
    boolean existsBySlipBoxIdAndAccountId(String slipBoxId, Long accountId);

    List<SlipBoxMongo> findByAccountId(Long accountId);

    
    
}
