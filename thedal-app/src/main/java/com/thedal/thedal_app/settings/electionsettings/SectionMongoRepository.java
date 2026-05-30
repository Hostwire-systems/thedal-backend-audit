package com.thedal.thedal_app.settings.electionsettings;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface SectionMongoRepository extends MongoRepository<SectionMongo, Long> {
    List<SectionMongo> findByAccountIdAndElectionId(Long accountId, Long electionId);
    List<SectionMongo> findByIdInAndAccountIdAndElectionId(List<Long> ids, Long accountId, Long electionId);
    void deleteByAccountIdAndElectionId(Long accountId, Long electionId);
    void deleteByIdIn(List<Long> ids);
    
    @Query("{ 'sectionNo': ?0, 'partNo': ?1, 'accountId': ?2, 'electionId': ?3 }")
    List<SectionMongo> findBySectionNoAndPartNoAndAccountIdAndElectionId(Integer sectionNo, Integer partNo, Long accountId, Long electionId);
    
    @Query("{ 'sectionNo': ?0, 'partNo': ?1, 'accountId': ?2, 'electionId': ?3, 'id': { $ne: ?4 } }")
    List<SectionMongo> findBySectionNoAndPartNoAndAccountIdAndElectionIdAndIdNot(Integer sectionNo, Integer partNo, Long accountId, Long electionId, Long id);
    
    @Query("{ 'electionId': ?0, 'accountId': ?1, 'partNo': { $in: ?2 }, 'sectionNo': { $in: ?3 } }")
    List<SectionMongo> findByElectionIdAndAccountIdAndPartNoInAndSectionNoIn(Long electionId, Long accountId, List<Integer> partNos, List<Integer> sectionNos);
}
