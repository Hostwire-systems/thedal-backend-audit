package com.thedal.thedal_app.settings.electionsettings;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface CasteMongoRepository extends MongoRepository<CasteMongo, Long> {
    List<CasteMongo> findByAccountIdAndElectionId(Long accountId, Long electionId);
    List<CasteMongo> findByIdInAndAccountIdAndElectionId(List<Long> ids, Long accountId, Long electionId);
    List<CasteMongo> findByReligionIdAndAccountIdAndElectionId(Long religionId, Long accountId, Long electionId);
    void deleteByAccountIdAndElectionId(Long accountId, Long electionId);
    void deleteByIdIn(List<Long> ids);
	List<CasteMongo> findByIdIn(ArrayList arrayList);
}