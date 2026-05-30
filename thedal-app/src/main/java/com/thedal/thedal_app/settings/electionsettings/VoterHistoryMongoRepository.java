package com.thedal.thedal_app.settings.electionsettings;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface VoterHistoryMongoRepository extends MongoRepository<VoterHistoryMongo, Long> {
    List<VoterHistoryMongo> findByAccountIdAndElectionId(Long accountId, Long electionId);
    List<VoterHistoryMongo> findByAccountIdAndElectionIdOrderByOrderIndexAsc(Long accountId, Long electionId);
    
    void deleteByAccountIdAndElectionId(Long accountId, Long electionId);
    void deleteByIdIn(List<Long> ids);
    
    List<VoterHistoryMongo> findByVoterHistoryNameInAndAccountIdAndElectionId(List<String> collect, Long accountId, Long electionId);
    
    // Enhanced methods for robust dual write operations
    Optional<VoterHistoryMongo> findByVoterHistoryNameAndAccountIdAndElectionId(String voterHistoryName, Long accountId, Long electionId);
    List<VoterHistoryMongo> findByIdInAndAccountIdAndElectionId(List<Long> ids, Long accountId, Long electionId);
    boolean existsByVoterHistoryNameAndAccountIdAndElectionId(String voterHistoryName, Long accountId, Long electionId);
    
    @Query("{ '_id': { $in: ?0 }, 'accountId': ?1, 'electionId': ?2 }")
    List<VoterHistoryMongo> findByVoterIdsAndAccountIdAndElectionId(List<Long> voterIds, Long accountId, Long electionId);

    @Query("{ 'voterHistoryName': { $in: ?0 } }")
    Collection<VoterHistoryMongo> findByVoterHistoryNameIn(List<String> voterHistoryNames);
    
    // For reorder operations
    List<VoterHistoryMongo> findByElectionIdAndAccountIdOrderByOrderIndexAsc(Long electionId, Long accountId);
}