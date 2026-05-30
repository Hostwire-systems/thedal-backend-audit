package com.thedal.thedal_app.settings.electionsettings;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.transaction.Transactional;

@Repository
public interface SubCasteRepository extends JpaRepository<SubCasteEntity, Long>{

	List<SubCasteEntity> findByCasteId(Long casteId);

	Optional<SubCasteEntity> findByIdAndCasteIdAndAccountId(Long subCasteId, Long casteId, Long accountId);

	List<SubCasteEntity> findByReligionId(Long religionId);
	
	List<SubCasteEntity> findByReligionIdAndCasteId(Long religionId, Long casteId);
	
	Optional<SubCasteEntity> findByIdAndAccountId(Long subCasteId, Long accountId);

    @Transactional
    void deleteByCasteId(Long casteId);

	Optional<SubCasteEntity> findBySubCasteNameAndCasteAndReligionAndAccountId(String subCaste, CasteEntity caste,
			ReligionEntity religion, Long accountId);
	
	 // Fetch all sub-castes for a specific religion and account
    List<SubCasteEntity> findByReligionIdAndReligion_AccountId(Long religionId, Long accountId);

    // Fetch all sub-castes for a specific caste and account
    List<SubCasteEntity> findByCasteIdAndCaste_Religion_AccountId(Long casteId, Long accountId);

    // Fetch all sub-castes for a specific religion, caste, and account
    List<SubCasteEntity> findByReligionIdAndCasteIdAndReligion_AccountId(Long religionId, Long casteId, Long accountId);

    // Fetch all sub-castes for the current account
    List<SubCasteEntity> findAllByReligion_AccountId(Long accountId);

	List<SubCasteEntity> findByCasteAndReligionAndAccountId(CasteEntity caste, ReligionEntity religion, Long accountId);

	Optional<SubCasteEntity> findByAccountId(Long accountId);

	Optional<SubCasteEntity> findBySubCasteNameAndCaste_IdAndReligion_IdAndAccountId(String subCasteName, Long casteId,
			Long religionId, Long accountId);

	void deleteByCasteIdAndElectionId(Long casteId, Long electionId);
	@Modifying
	@Query("DELETE FROM SubCasteEntity s WHERE s.caste.id IN :casteIds AND s.electionId = :electionId")
	void deleteByCasteIdInAndElectionId(@Param("casteIds") List<Long> casteIds, @Param("electionId") Long electionId);
	// List<SubCasteEntity> findAllByReligion_AccountIdAndElectionId(Long accountId, Long electionId);
	// List<SubCasteEntity> findByReligionIdAndReligion_AccountIdAndElectionId(Long religionId, Long accountId, Long electionId);
	// List<SubCasteEntity> findByCasteIdAndCaste_Religion_AccountIdAndElectionId(Long casteId, Long accountId, Long electionId);
	// List<SubCasteEntity> findByReligionIdAndCasteIdAndReligion_AccountIdAndElectionId(Long religionId, Long casteId, Long accountId, Long electionId);
	List<SubCasteEntity> findAllByReligion_AccountIdAndElectionIdOrderByUpdatedAtDescCreatedAtDesc(Long accountId, Long electionId);

	List<SubCasteEntity> findByReligionIdAndReligion_AccountIdAndElectionIdOrderByUpdatedAtDescCreatedAtDesc(Long religionId, Long accountId, Long electionId);

	List<SubCasteEntity> findByCasteIdAndCaste_Religion_AccountIdAndElectionIdOrderByUpdatedAtDescCreatedAtDesc(Long casteId, Long accountId, Long electionId);

	List<SubCasteEntity> findByReligionIdAndCasteIdAndReligion_AccountIdAndElectionIdOrderByUpdatedAtDescCreatedAtDesc(Long religionId, Long casteId, Long accountId, Long electionId);


	Optional<SubCasteEntity> findByIdAndAccountIdAndElectionId(Long subCasteId, Long accountId, Long electionId);

	Optional<SubCasteEntity> findBySubCasteNameAndCasteAndReligionAndAccountIdAndElectionId(String subCasteName, CasteEntity caste, ReligionEntity religion, Long accountId, Long electionId);
	
	 @Query("SELECT COALESCE(MAX(s.orderIndex), 0) FROM SubCasteEntity s WHERE s.caste.id = :casteId AND s.electionId = :electionId")
	 Integer findMaxOrderIndexByCasteIdAndElectionId(@Param("casteId") Long casteId, @Param("electionId") Long electionId);
	 
	List<SubCasteEntity> findByCaste_ElectionIdAndCaste_Religion_AccountId(Long electionId, Long accountId);

//	Integer findMaxOrderIndexByElectionId(Long electionId);
//	@Query("SELECT MAX(s.orderIndex) FROM SubCasteEntity s WHERE s.electionId = :electionId")
//	Integer findMaxOrderIndexByElectionId(@Param("electionId") Long electionId);
	@Query("SELECT COALESCE(MAX(s.orderIndex), -1) FROM SubCasteEntity s WHERE s.electionId = :electionId")
	Integer findMaxOrderIndexByElectionId(@Param("electionId") Long electionId);

	Optional<SubCasteEntity> findBySubCasteNameAndCaste_IdAndReligion_IdAndElectionIdAndAccountId(
		    String subCasteName, Long casteId, Long religionId, Long electionId, Long accountId);
	@Query("SELECT sc FROM SubCasteEntity sc WHERE sc.subCasteName = :subCasteName AND sc.caste.id = :casteId AND sc.accountId = :accountId")
			Optional<SubCasteEntity> findBySubCasteNameAndCasteIdAndAccountId(
				@Param("subCasteName") String subCasteName,
				@Param("casteId") Long casteId,
				@Param("accountId") Long accountId
			);
			
	List<SubCasteEntity> findByAccountIdAndElectionId(Long accountId, Long electionId);		
	List<SubCasteEntity> findByElectionIdAndAccountIdOrderByOrderIndexAsc(Long electionId, Long accountId);

	List<SubCasteEntity> findByAccountIdAndElectionIdOrderByOrderIndexAsc(Long accountId, Long electionId);

	Optional<SubCasteEntity> findByAccountIdAndElectionIdAndId(Long accountId, Long electionId, Long subCasteId);

	List<SubCasteEntity> findByAccountIdAndElectionIdAndCasteIdAndReligionIdOrderByOrderIndexAsc(
		Long accountId, Long electionId, Long casteId, Long religionId);
 
		@Query("SELECT s FROM SubCasteEntity s WHERE s.id IN :subCasteIds AND s.accountId = :accountId AND s.electionId = :electionId")
		List<SubCasteEntity> findAllByIdAndAccountIdAndElectionId(@Param("subCasteIds") List<Long> subCasteIds,
																  @Param("accountId") Long accountId,
																  @Param("electionId") Long electionId);	


	 List<SubCasteEntity> findByAccountIdAndElectionIdAndCasteIdOrderByOrderIndexAsc(Long accountId, Long electionId,
			Long casteId);

	 List<SubCasteEntity> findByAccountIdAndElectionIdAndReligionIdOrderByOrderIndexAsc(Long accountId, Long electionId,
			Long religionId);

	boolean existsByReligionIdAndAccountIdAndElectionId(Long religionId, Long accountId, Long electionId);

	boolean existsByCasteIdAndAccountIdAndElectionId(Long casteId, Long accountId, Long electionId);

	// Added for merge name lookups
	boolean existsBySubCasteNameAndAccountIdAndElectionId(String subCasteName, Long accountId, Long electionId);

	List<SubCasteEntity> findByReligionIdAndAccountIdAndElectionId(Long religionId, Long accountId, Long electionId);

	//boolean existsByCasteIdInAndAccountIdAndElectionId(List<Long> casteIds, Long accountId, Long electionId);
	int deleteByAccountIdAndElectionId(Long accountId, Long electionId);

	@Modifying
	@Query("DELETE FROM SubCasteEntity sc WHERE sc.accountId = :accountId AND sc.electionId = :electionId AND sc.id IN :subCasteIds")
	int deleteByAccountIdAndElectionIdAndIds(@Param("accountId") Long accountId,
											@Param("electionId") Long electionId,
											@Param("subCasteIds") List<Long> subCasteIds);
	List<SubCasteEntity> findByCasteIdInAndAccountIdAndElectionId(List<Long> casteIds, Long accountId, Long electionId);

	List<SubCasteEntity> findBySubCasteNameInAndCasteIdInAndReligionIdInAndAccountId(
            List<String> subCasteNames, List<Long> casteIds, List<Long> religionIds, Long accountId);
	@Query("SELECT MIN(s.orderIndex) FROM SubCasteEntity s WHERE s.caste.id = :casteId AND s.electionId = :electionId")
	Integer findMinOrderIndexByCasteIdAndElectionId(@Param("casteId") Long casteId, @Param("electionId") Long electionId);

	@Query("SELECT s, COUNT(v.id) as voterCount " +
	           "FROM SubCasteEntity s " +
	           "LEFT JOIN s.voters v " +
	           "WHERE s.accountId = :accountId AND s.electionId = :electionId " +
	           "AND (:religionId IS NULL OR s.religion.id = :religionId) " +
	           "AND (:casteId IS NULL OR s.caste.id = :casteId) " +
	           "GROUP BY s")
	    List<Object[]> findSubCastesWithVoterCount(Long accountId, Long electionId, Long religionId, Long casteId);	

	// Add count method for migration validation and stats
	long countByAccountIdAndElectionId(Long accountId, Long electionId);

	List<SubCasteEntity> findBySubCasteNameInAndCaste_ElectionIdAndCaste_Religion_AccountId(
	        List<String> subCasteNames, Long electionId, Long accountId);
	
}
