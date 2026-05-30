package com.thedal.thedal_app.election;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import io.lettuce.core.dynamic.annotation.Param;

public interface SurveyFormRepository extends JpaRepository<SurveyFormEntity, Long> {

//    @Query("SELECT f FROM SurveyFormEntity f " +
//           "WHERE f.accountId = :accountId " +
//           "AND f.electionId = :electionId " +
//          // "AND (:formName IS NULL OR LOWER(f.formName) LIKE LOWER(CONCAT('%', :formName, '%'))) " +
//           "AND (:formName IS NULL OR LOWER(f.formName) LIKE LOWER(:formName)) " +
//           "AND (:isActive IS NULL OR f.isActive = :isActive)")
//    Page<SurveyFormEntity> findByAccountIdAndElectionIdAndFilters(
//            @Param("accountId") Long accountId,
//            @Param("electionId") Long electionId,
//            @Param("formName") String formName,
//            @Param("isActive") Boolean isActive,
//            Pageable pageable);
	 @Query("SELECT f FROM SurveyFormEntity f " +
	           "WHERE f.accountId = :accountId " +
	           "AND f.electionId = :electionId " +
	           "AND (:formName IS NULL OR LOWER(f.formName) = LOWER(CAST(:formName AS string))) " +
	           "AND (:isActive IS NULL OR f.isActive = :isActive)")
	    Page<SurveyFormEntity> findByAccountIdAndElectionIdAndFilters(
	            @Param("accountId") Long accountId,
	            @Param("electionId") Long electionId,
	            @Param("formName") String formName,
	            @Param("isActive") Boolean isActive,
	            Pageable pageable);

    Optional<SurveyFormEntity> findByIdAndAccountIdAndElectionId(
            Long id, Long accountId, Long electionId);

	boolean existsByIdAndAccountIdAndElectionId(Long formId, Long accountId, Long electionId);

	int deleteByAccountIdAndElectionId(Long accountId, Long electionId);
    List<SurveyFormEntity> findByIdInAndAccountIdAndElectionId(List<Long> ids, Long accountId, Long electionId);
    
    @Query("SELECT s.formId, COUNT(s) FROM SurveyFormSubmissionEntity s " +
            "WHERE s.electionId = :electionId AND s.accountId = :accountId " +
            "AND s.formId IN :formIds GROUP BY s.formId")
     List<Object[]> countSubmissionsByFormIds(@Param("accountId") Long accountId, 
                                            @Param("electionId") Long electionId, 
                                            @Param("formIds") List<Long> formIds);

     @Query("SELECT COALESCE(MAX(sf.orderIndex), -1) FROM SurveyFormEntity sf WHERE sf.electionId = :electionId AND sf.accountId = :accountId")
     Integer findMaxOrderIndexByElectionIdAndAccountId(@Param("electionId") Long electionId, @Param("accountId") Long accountId);

	List<SurveyFormEntity> findByElectionIdAndAccountIdOrderByOrderIndexAsc(Long electionId, Long accountId);
    
}