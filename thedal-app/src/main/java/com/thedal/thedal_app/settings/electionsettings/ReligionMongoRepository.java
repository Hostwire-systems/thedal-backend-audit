package com.thedal.thedal_app.settings.electionsettings;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface ReligionMongoRepository extends MongoRepository<ReligionMongo, Long> {
    List<ReligionMongo> findByAccountIdAndElectionId(Long accountId, Long electionId);
    List<ReligionMongo> findByIdInAndAccountIdAndElectionId(List<Long> ids, Long accountId, Long electionId);
    void deleteByAccountIdAndElectionId(Long accountId, Long electionId);
    void deleteByIdIn(List<Long> ids);
    List<ReligionMongo> findByIdIn(List<Long> ids);
    
    @Query("{ 'religionName': ?0, 'accountId': ?1, 'electionId': ?2 }")
    List<ReligionMongo> findByReligionNameAndAccountIdAndElectionId(String religionName, Long accountId, Long electionId);
    
	Optional<ReligionMongo> findByReligionNameIgnoreCase(String religionName);
}