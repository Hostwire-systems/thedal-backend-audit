package com.thedal.thedal_app.settings.electionsettings;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface BenefitSchemesMongoRepository extends MongoRepository<BenefitSchemesMongo, Long> {
    boolean existsBySchemeNameAndElectionId(String schemeName, Long electionId);
    Optional<BenefitSchemesMongo> findBySchemeNameAndElectionId(String schemeName, Long electionId);
    List<BenefitSchemesMongo> findByAccountIdAndElectionId(Long accountId, Long electionId);
    Optional<BenefitSchemesMongo> findByIdAndAccountIdAndElectionId(Long id, Long accountId, Long electionId);
    void deleteByAccountIdAndElectionId(Long accountId, Long electionId);
    void deleteByIdIn(List<Long> ids);
    
    @Query("{ 'id': { $in: ?0 }, 'accountId': ?1, 'electionId': ?2 }")
    List<BenefitSchemesMongo> findByVoterIdsAndAccountIdAndElectionId(List<Long> voterIds, Long accountId, Long electionId);
    
}