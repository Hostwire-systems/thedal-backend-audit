package com.thedal.thedal_app.election;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface SurveyFormSubmissionRepository extends JpaRepository<SurveyFormSubmissionEntity, Long> {

	Page<SurveyFormSubmissionEntity> findByFormIdAndElectionIdAndAccountId(
            Long formId, Long electionId, Long accountId, Pageable pageable);

	int count(Specification<SurveyFormSubmissionEntity> spec);
	Page<SurveyFormSubmissionEntity> findAll(Specification<SurveyFormSubmissionEntity> spec, Pageable pageable);
	
	int deleteByElectionIdAndAccountId(Long electionId, Long accountId);
    List<SurveyFormSubmissionEntity> findByIdInAndFormIdAndElectionIdAndAccountId(List<Long> ids, Long formId, Long electionId, Long accountId);

	int deleteByFormIdAndElectionIdAndAccountId(Long id, Long electionId, Long accountId);
	
//	@Query("SELECT s.formId, COUNT(s) FROM SurveyFormSubmissionEntity s " +
//	           "WHERE s.formId IN :formIds AND s.electionId = :electionId AND s.accountId = :accountId " +
//	           "GROUP BY s.formId")
//	    Map<Long, Long> countByFormIdsAndElectionIdAndAccountId(List<Long> formIds, Long electionId, Long accountId);
//
//	@Query("SELECT COUNT(s) FROM SurveyFormSubmissionEntity s " +
//		       "WHERE s.formId = :formId AND s.electionId = :electionId AND s.accountId = :accountId")
//		long countByFormIdAndElectionIdAndAccountId(Long formId, Long electionId, Long accountId);
	
//	@Query("SELECT COUNT(s) FROM SurveyFormSubmissionEntity s " +
//	           "WHERE s.formId = :formId AND s.electionId = :electionId AND s.accountId = :accountId")
//	    long countByFormIdAndElectionIdAndAccountId(Long formId, Long electionId, Long accountId);
//
//	    @Query("SELECT CAST(s.formId AS java.lang.Long) AS formId, CAST(COUNT(s) AS java.lang.Long) AS count " +
//	           "FROM SurveyFormSubmissionEntity s " +
//	           "WHERE s.formId IN :formIds AND s.electionId = :electionId AND s.accountId = :accountId " +
//	           "GROUP BY s.formId")
//	    Map<Long, Long> countByFormIdsAndElectionIdAndAccountId(List<Long> formIds, Long electionId, Long accountId);
//	
}