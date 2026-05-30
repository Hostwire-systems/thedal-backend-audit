package com.thedal.thedal_app.settings.electionsettings;

import java.util.Collection;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface PartyMongoRepository extends MongoRepository<PartyMongo, Long> {
    List<PartyMongo> findByAccountIdAndElectionId(Long accountId, Long electionId);
    List<PartyMongo> findByIdInAndAccountIdAndElectionId(List<Long> ids, Long accountId, Long electionId);
    void deleteByAccountIdAndElectionId(Long accountId, Long electionId);
    void deleteByIdIn(List<Long> ids);
    
    @Query("{ 'partyName': { $in: ?0 }, 'accountId': ?1, 'electionId': ?2 }")
    List<PartyMongo> findByPartyNameInAndAccountIdAndElectionId(List<String> partyNames, Long accountId, Long electionId);
	
    
    Collection<PartyMongo> findByPartyNameIn(List<String> partyNames);
}