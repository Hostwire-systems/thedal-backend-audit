package com.thedal.thedal_app.settings.electionsettings;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface SubCasteMongoRepository extends MongoRepository<SubCasteMongo, Long> {
    List<SubCasteMongo> findByAccountIdAndElectionId(Long accountId, Long electionId);
    List<SubCasteMongo> findByReligionIdAndCasteIdAndAccountIdAndElectionId(Long religionId, Long casteId, Long accountId, Long electionId);
    List<SubCasteMongo> findByCasteIdAndAccountIdAndElectionId(Long casteId, Long accountId, Long electionId);
    List<SubCasteMongo> findByReligionIdAndAccountIdAndElectionId(Long religionId, Long accountId, Long electionId);
    void deleteByIdIn(List<Long> ids);
    void deleteByAccountIdAndElectionId(Long accountId, Long electionId);
}