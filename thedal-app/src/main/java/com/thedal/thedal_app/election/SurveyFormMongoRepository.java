package com.thedal.thedal_app.election;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface SurveyFormMongoRepository extends MongoRepository<SurveyFormMongo, String> {

    List<SurveyFormMongo> findByAccountIdAndElectionId(Long accountId, Long electionId);
    
    List<SurveyFormMongo> findByAccountIdAndElectionIdAndIsActive(Long accountId, Long electionId, Boolean isActive);
    
    Optional<SurveyFormMongo> findBySurveyFormIdAndAccountIdAndElectionId(Long surveyFormId, Long accountId, Long electionId);
    
    List<SurveyFormMongo> findBySurveyFormIdInAndAccountIdAndElectionId(List<Long> surveyFormIds, Long accountId, Long electionId);
    
    void deleteBySurveyFormIdAndAccountIdAndElectionId(Long surveyFormId, Long accountId, Long electionId);
    
    void deleteBySurveyFormIdIn(List<Long> surveyFormIds);
    
    void deleteByAccountIdAndElectionId(Long accountId, Long electionId);
    
    void deleteBySurveyFormId(Long surveyFormId);
    
    long countByAccountIdAndElectionId(Long accountId, Long electionId);
    
    long countByAccountIdAndElectionIdAndIsActive(Long accountId, Long electionId, Boolean isActive);
    
    // Custom query to search survey forms with filters
    @Query("{ 'accountId': ?0, 'electionId': ?1, " +
           "$and: [ " +
           "  { $or: [ " +
           "    { 'formName': { $exists: false } }, " +
           "    { 'formName': null }, " +
           "    { 'formName': { $regex: ?2, $options: 'i' } } " +
           "  ] }, " +
           "  { $or: [ " +
           "    { 'isActive': { $exists: false } }, " +
           "    { 'isActive': null }, " +
           "    { 'isActive': ?3 } " +
           "  ] } " +
           "] }")
    List<SurveyFormMongo> findByAccountIdAndElectionIdWithFilters(Long accountId, Long electionId, String formName, Boolean isActive);
    
    // Search by form name or description
    @Query("{ 'accountId': ?0, 'electionId': ?1, " +
           "$or: [ " +
           "  { 'formName': { $regex: ?2, $options: 'i' } }, " +
           "  { 'formDescription': { $regex: ?2, $options: 'i' } } " +
           "] }")
    List<SurveyFormMongo> searchSurveyForms(Long accountId, Long electionId, String searchTerm);
    
    Optional<SurveyFormMongo> findBySurveyFormId(Long surveyFormId);
    
    
    
    
}
