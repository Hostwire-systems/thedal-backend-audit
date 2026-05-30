package com.thedal.thedal_app.settings.electionsettings;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface LanguageMongoRepository extends MongoRepository<LanguageMongo, Long> {
    List<LanguageMongo> findByAccountIdAndElectionId(Long accountId, Long electionId);
    boolean existsByLanguageNameAndElectionId(String languageName, Long electionId);
    boolean existsByLanguageNameAndElectionIdAndIdNot(String languageName, Long electionId, Long id);
    
    void deleteByAccountIdAndElectionId(Long accountId, Long electionId);
    void deleteByIdIn(List<Long> languageIds);
    List<LanguageMongo> findAllById(Iterable<Long> ids);
    
    @Query("{ '_id': { $in: ?0 } }")
    List<LanguageMongo> findByVoterIds(List<Long> voterIds);
}
