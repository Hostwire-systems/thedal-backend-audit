package com.thedal.thedal_app.settings.electionsettings;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReligionRepository extends JpaRepository<ReligionEntity, Long>{

	Optional<ReligionEntity> findByReligionName(String religion);

	List<ReligionEntity> findByAccountId(Long accountId);

	Optional<ReligionEntity> findByIdAndAccountId(Long religionId, Long accountId);

	//@Query("SELECT r FROM ReligionEntity r WHERE r.religionName = :religionName AND r.accountId = :accountId")
	Optional<ReligionEntity> findByReligionNameAndAccountId(String religion, Long accountId);

	List<ReligionEntity> findByAccountIdAndElectionId(Long accountId, Long electionId);

	Optional<ReligionEntity> findByIdAndAccountIdAndElectionId(Long religionId, Long accountId, Long electionId);

	//boolean existsByReligionNameAndAccountId(String religionName, Long accountId);

	boolean existsByReligionNameAndAccountIdAndElectionId(String religionName, Long accountId, Long electionId);

	Optional<ReligionEntity> findByReligionNameAndAccountIdAndElectionId(String religionName, Long accountId, Long electionId);
	
	
    @Query("SELECT MAX(r.orderIndex) FROM ReligionEntity r WHERE r.electionId = :electionId")
    Integer findMaxOrderIndexByElectionId(@Param("electionId") Long electionId);

    List<ReligionEntity> findByElectionIdAndAccountIdOrderByOrderIndex(Long electionId, Long accountId);

    List<ReligionEntity> findByElectionIdAndAccountId(Long electionId, Long accountId);

	List<ReligionEntity> findByIdInAndAccountIdAndElectionId(List<Long> religionIds, Long accountId, Long electionId);

	List<ReligionEntity> findByAccountIdAndReligionNameIn(Long accountId, List<String> religionNames);

	Optional<ReligionEntity> findByIdAndElectionIdAndAccountId(Long id, Long electionId, Long accountId);
	
	@Modifying
	@Query("UPDATE ReligionEntity r SET r.orderIndex = r.orderIndex + 1 WHERE r.electionId = :electionId")
	void incrementOrderIndexes(@Param("electionId") Long electionId);

	
	Optional<ReligionEntity> findByReligionNameAndAccountIdAndElectionIdAndIdNot(String religionName, Long accountId, Long electionId, Long id);

	@Query("SELECT r, COUNT(v.id) as voterCount " +
	           "FROM ReligionEntity r " +
	           "LEFT JOIN r.voters v " +
	           "WHERE r.accountId = :accountId AND r.electionId = :electionId " +
	           "GROUP BY r")
	    List<Object[]> findReligionsWithVoterCount(Long accountId, Long electionId);

	// Add count method for migration validation and stats
	long countByAccountIdAndElectionId(Long accountId, Long electionId);
	
}
