package com.thedal.thedal_app.election;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.transaction.Transactional;

@Repository
public interface TemplateRepository extends JpaRepository<TemplateEntity, Long>{

    List<TemplateEntity> findByElectionId(Long electionId);
    Optional<TemplateEntity> findByAccountIdAndElectionIdAndTemplateId(Long accountId, Long electionId, Long templateId);
	Optional<TemplateEntity> findByAccountIdAndElectionIdAndTemplateIdAndIsActive(Long accountId, Long electionId,
			Long templateId, boolean b);
	List<TemplateEntity> findByElectionIdAndIsActive(Long electionId, boolean b);
	Optional<TemplateEntity> findByTemplateId(Long templateId);
	Optional<TemplateEntity> findByElectionIdAndTemplateNameAndAccountId(Long electionId, String templateName,
			Long accountId);
	//List<TemplateEntity> findByElectionIdAndAccountId(Long electionId, Long accountId);
	Optional<TemplateEntity> findByElectionIdAndTemplateIdAndAccountIdAndTemplateName(Long electionId, Long templateId,
			Long accountId, String templateName);
	Optional<TemplateEntity> findByElectionIdAndTemplateIdAndAccountId(Long electionId, Long templateId,
			Long accountId);
	
	
//	 @Modifying
//	    @Transactional
//	    //@Query("UPDATE TemplateEntity t SET t.isActive = false WHERE t.electionId = :electionId AND t.accountId = :accountId AND t.isActive = true")
//	    @Query("UPDATE TemplateEntity t SET t.isActive = false WHERE t.electionId = :electionId AND t.accountId = :accountId")
//	    int deactivateAllTemplatesForElection(@Param("electionId") Long electionId, @Param("accountId") Long accountId);
//		 
//	 
	    @Modifying
	    @Transactional
	    @Query("UPDATE TemplateEntity t SET t.isActive = false WHERE t.electionId = :electionId AND t.templateId <> :templateId")
	    void updateIsActiveFalseForOtherTemplates(@Param("electionId") Long electionId, @Param("templateId") Long templateId);
		
	    @Query("SELECT t FROM TemplateEntity t WHERE t.electionId = :electionId ORDER BY t.templateId ASC")
	    List<TemplateEntity> findTemplatesByElection(@Param("electionId") Long electionId);

	    @Query("SELECT t FROM TemplateEntity t WHERE t.electionId = :electionId AND t.accountId = :accountId ORDER BY t.isActive DESC, t.templateId ASC")
	    List<TemplateEntity> findTemplatesOrdered(@Param("electionId") Long electionId, @Param("accountId") Long accountId);
		
	    boolean existsByElectionId(Long electionId);
	    
	    @Query("SELECT MAX(t.orderIndex) FROM TemplateEntity t WHERE t.accountId = :accountId")
	    Integer findMaxOrderIndexByAccountId(@Param("accountId") Long accountId);
	    @Query("SELECT MAX(t.orderIndex) FROM TemplateEntity t WHERE t.electionId = :electionId AND t.accountId = :accountId")
	    Integer findMaxOrderIndexByElectionIdAndAccountId(@Param("electionId") Long electionId, @Param("accountId") Long accountId);

	    List<TemplateEntity> findByElectionIdAndAccountIdOrderByOrderIndexAsc(Long electionId, Long accountId);
		Optional<TemplateEntity> findBySlipId(String slipId);
		Optional<TemplateEntity> findByAccountIdAndElectionIdAndTemplateName(Long accountId, Long electionId,
				String templateName);

		
		List<TemplateEntity> findByElectionIdAndAccountId(Long electionId, Long accountId);
	    
	    // Updated method to set all other templates inactive
		@Transactional
	    @Modifying
	    @Query("UPDATE TemplateEntity t SET t.isActive = false WHERE t.electionId = :electionId AND t.accountId = :accountId AND t.id != :excludeTemplateId")
	    void updateIsActiveFalseForOtherTemplates(@Param("electionId") Long electionId, 
	                                            @Param("accountId") Long accountId, 
	                                            @Param("excludeTemplateId") Long excludeTemplateId);

		@Modifying
		@Query("DELETE FROM TemplateEntity t WHERE t.accountId = :accountId AND t.electionId = :electionId")
		int deleteByAccountIdAndElectionId(@Param("accountId") Long accountId, @Param("electionId") Long electionId);

		@Modifying
		@Query("DELETE FROM TemplateEntity t WHERE t.accountId = :accountId AND t.electionId = :electionId AND t.templateName IN :templateNames")
		int deleteByAccountIdAndElectionIdAndTemplateNameIn(
		        @Param("accountId") Long accountId, 
		        @Param("electionId") Long electionId,
		        @Param("templateNames") List<String> templateNames);
		
		
		@Modifying
		@Query("DELETE FROM TemplateEntity t WHERE t.accountId = :accountId AND t.electionId = :electionId AND t.templateName != :templateName")
		int deleteByAccountIdAndElectionIdAndTemplateNameNot(@Param("accountId") Long accountId, 
		                                                    @Param("electionId") Long electionId, 
		                                                    @Param("templateName") String templateName);
		
		
		Optional<TemplateEntity> findByElectionIdAndAccountIdAndTemplateName(Long electionId, Long accountId, String templateName);

	    List<TemplateEntity> findByElectionIdAndAccountIdAndIsActive(Long electionId, Long accountId, Boolean isActive);

	    Optional<TemplateEntity> findByElectionIdAndAccountIdAndOrderIndex(Long electionId, Long accountId, Integer orderIndex);
	

}
		


