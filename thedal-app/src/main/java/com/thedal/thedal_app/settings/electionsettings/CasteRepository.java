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
public interface CasteRepository extends JpaRepository<CasteEntity, Long>{

	List<CasteEntity> findByReligionId(Long religionId);

	Optional<CasteEntity> findByCasteName(String caste);

	Optional<CasteEntity> findByIdAndReligionIdAndAccountId(Long casteId, Long religionId, Long accountId);

	Optional<CasteEntity> findByIdAndAccountId(Long casteId, Long accountId);

    @Transactional
    void deleteByReligionId(Long religionId);

	Optional<CasteEntity> findByCasteNameAndReligionAndAccountId(String caste, ReligionEntity religion, Long accountId);

	List<CasteEntity> findAllByReligion_AccountId(Long accountId);

	List<CasteEntity> findByReligionIdAndReligion_AccountId(Long religionId, Long accountId);

	List<CasteEntity> findByReligionAndAccountId(ReligionEntity religion, Long accountId);

	Optional<CasteEntity> findByAccountId(Long accountId);

	//Optional<CasteEntity> findByCasteNameAndAccountIdAndElectionId(String casteName, Long accountId, Long electionId);
	Optional<CasteEntity> findByCasteNameAndReligion_IdAndElectionId(String casteName, Long religionId, Long electionId);

	//Optional<CasteEntity> findByCasteNameAndReligion_IdAndAccountId(String casteName, Long religionId, Long accountId);
	Optional<CasteEntity> findByCasteNameAndReligion_IdAndAccountIdAndElectionId(
            String casteName, Long religionId, Long accountId, Long electionId);
    List<CasteEntity> findAllByReligion_AccountIdAndElectionId(Long accountId, Long electionId);
    
    List<CasteEntity> findByReligionIdAndReligion_AccountIdAndElectionId(Long religionId, Long accountId, Long electionId);
    Optional<CasteEntity> findByIdAndAccountIdAndElectionId(Long casteId, Long accountId, Long electionId);

    Optional<CasteEntity> findByCasteNameAndReligionAndAccountIdAndElectionId(String casteName, ReligionEntity religion, Long accountId, Long electionId);
    
//    @Query("SELECT COALESCE(MAX(c.orderIndex), 0) FROM CasteEntity c WHERE c.religion.id = :religionId")
//    Integer findMaxOrderIndexByReligionId(@Param("religionId") Long religionId);
    @Query("SELECT COALESCE(MAX(c.orderIndex), 0) FROM CasteEntity c WHERE c.religion.id = :religionId AND c.electionId = :electionId")
    Integer findMaxOrderIndexByReligionIdAndElectionId(@Param("religionId") Long religionId, @Param("electionId") Long electionId);
    List<CasteEntity> findByReligion_IdAndReligion_AccountIdAndElectionId(Long religionId, Long accountId, Long electionId);

	List<CasteEntity> findByReligion_ElectionIdAndReligion_AccountId(Long electionId, Long accountId);

	@Query("SELECT c FROM CasteEntity c WHERE c.religion.id = :religionId AND c.religion.accountId = :accountId AND c.electionId = :electionId ORDER BY c.orderIndex")
    List<CasteEntity> findByReligionIdOrdered(@Param("religionId") Long religionId, 
                                              @Param("accountId") Long accountId, 
                                              @Param("electionId") Long electionId);

    @Query("SELECT c FROM CasteEntity c WHERE c.religion.accountId = :accountId AND c.electionId = :electionId ORDER BY c.orderIndex")
    List<CasteEntity> findAllByReligion_AccountIdAndElectionIdOrdered(@Param("accountId") Long accountId, 
                                                                      @Param("electionId") Long electionId);

	List<CasteEntity> findByReligion_ElectionIdAndReligion_AccountIdOrderByOrderIndexAsc(Long electionId,
			Long accountId);

//	@Query("SELECT MAX(c.orderIndex) FROM CasteEntity c WHERE c.electionId = :electionId") 
//	Integer findMaxOrderIndexByElectionId(Long electionId);
	@Query("SELECT MAX(s.orderIndex) FROM CasteEntity s WHERE s.electionId = :electionId")
	Integer findMaxOrderIndexByElectionId(@Param("electionId") Long electionId);

	@Query("SELECT MIN(c.orderIndex) FROM CasteEntity c WHERE c.religion.id = :religionId AND c.electionId = :electionId")
Integer findMinOrderIndexByReligionIdAndElectionId(@Param("religionId") Long religionId, 
                                                   @Param("electionId") Long electionId);


	List<CasteEntity> findByElectionIdAndAccountIdOrderByOrderIndexAsc(Long electionId, Long accountId);

	List<CasteEntity> findByAccountIdAndElectionIdOrderByOrderIndexAsc(Long accountId, Long electionId);

	Optional<CasteEntity> findByAccountIdAndElectionIdAndId(Long accountId, Long electionId, Long casteId);

	Integer findMaxOrderIndexByReligionId(Long religionId);

	List<CasteEntity> findByAccountIdAndElectionIdAndReligionIdOrderByOrderIndexAsc(Long accountId, Long electionId,
			Long religionId);

	boolean existsByReligionIdAndAccountIdAndElectionId(Long religionId, Long accountId, Long electionId);

	// Added for merge name lookups
	boolean existsByCasteNameAndAccountIdAndElectionId(String casteName, Long accountId, Long electionId);

	List<CasteEntity> findByReligionIdAndAccountIdAndElectionId(Long religionId, Long accountId, Long electionId);

	int deleteByAccountIdAndElectionId(Long accountId, Long electionId);

	@Modifying
    @Query("DELETE FROM CasteEntity c WHERE c.accountId = :accountId AND c.electionId = :electionId AND c.id IN :casteIds")
    int deleteByAccountIdAndElectionIdAndIds(@Param("accountId") Long accountId, 
                                             @Param("electionId") Long electionId, 
                                             @Param("casteIds") List<Long> casteIds);
	List<CasteEntity> findByIdInAndAccountIdAndElectionId(List<Long> casteIds, Long accountId, Long electionId);
	List<CasteEntity> findByAccountIdAndElectionId(Long accountId, Long electionId);

	List<CasteEntity> findByCasteNameInAndReligionIdInAndAccountIdAndElectionId(
            List<String> casteNames, List<Long> religionIds, Long accountId, Long electionId);

	@Query("SELECT c, COUNT(v.id) as voterCount " +
	           "FROM CasteEntity c " +
	           "LEFT JOIN c.voters v " +
	           "WHERE c.religion.accountId = :accountId AND c.electionId = :electionId AND (:religionId IS NULL OR c.religion.id = :religionId) " +
	           "GROUP BY c")
	    List<Object[]> findCastesWithVoterCount(Long accountId, Long electionId, Long religionId);

	// Add count method for migration validation and stats
	long countByAccountIdAndElectionId(Long accountId, Long electionId);

	List<CasteEntity> findByElectionIdAndAccountIdAndCasteNameIn(Long id, Long id2, List<String> casteNamesToProcess);
	
}
