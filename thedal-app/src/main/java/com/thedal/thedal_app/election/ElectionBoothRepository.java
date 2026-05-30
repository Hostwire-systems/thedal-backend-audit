package com.thedal.thedal_app.election;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.util.Streamable;

import com.thedal.thedal_app.role.Role;

public interface ElectionBoothRepository extends JpaRepository<ElectionBooth, Long> {

    List<ElectionBooth> findByElection(ElectionEntity election);
    boolean existsByBoothNumber(Integer boothNumber);


	ElectionBooth findByBoothNumber(Integer boothNumber);

	Optional<ElectionBooth> findByElectionAndBoothNumber(ElectionEntity election, ElectionBooth boothNumber);

	Optional<ElectionBooth> findByElectionIdAndBoothNumber(Long electionId, Integer boothNumber);

	//Set<Integer> findBoothNumbersByElectionId(Long electionId);
	@Query("SELECT eb.boothNumber FROM ElectionBooth eb WHERE eb.election.id = :electionId")
    Set<Integer> findBoothNumbersByElectionId(@Param("electionId") Long electionId);

	//List<ElectionBooth> findByElectionId(Long electionId);

	//Page<ElectionBooth> findByElectionId(Long electionId, Pageable pageable);

	Page<ElectionBooth> findByElectionIdAndAccountId(Long electionId, Long accountId, Pageable pageable);

	Optional<ElectionBooth> findByIdAndElectionIdAndAccountId(Long boothId, Long electionId, Long accountId);

	Optional<ElectionBooth> findByIdAndAccountId(Long boothId, Long accountId);

	Optional<ElectionBooth> findByElectionIdAndBoothNumberAndAccountId(Long electionId, Integer boothNumber,
			Long accountId);
	Optional<ElectionBooth> findByBoothNumberAndElectionId(Long boothNumber, Long electionId);
	
	Optional<ElectionBooth> findByElectionIdAndAccountIdAndBoothNumber(Long electionId, Long accountId,
			Integer boothNumber);
	List<ElectionBooth> findByElectionIdAndBoothNumberInAndAccountId(Long electionId, ArrayList arrayList,
			Long accountId);
	
//	Page<ElectionBooth> findByElectionIdAndAccountIdAndBoothNumberIn(Long electionId, Long accountId,
//			List<Integer> assignedBooths, Pageable pageable);
	Page<ElectionBooth> findByElectionIdAndAccountIdAndBoothNumberIn(Long electionId, Long accountId, List<Integer> boothNumbers, Pageable pageable);
	
	@Query("SELECT COALESCE(MAX(b.orderIndex), -1) FROM ElectionBooth b WHERE b.election.id = :electionId")
	Integer findMaxOrderIndexByElectionId(@Param("electionId") Long electionId);
//	 @Query("SELECT MAX(b.orderIndex) FROM ElectionBooth b WHERE b.accountId = :accountId")
//	 Integer findMaxOrderIndexByAccountId(@Param("accountId") Long accountId);
	
	List<ElectionBooth> findByElectionIdAndAccountIdOrderByOrderIndex(Long electionId, Long accountId);
	Page<ElectionBooth> findByElectionIdAndAccountIdOrderByOrderIndexAsc(Long electionId, Long accountId,
			Pageable pageable);
	
	@Query("SELECT COUNT(e) > 0 FROM ElectionBooth e WHERE e.boothNumber = :boothNumber AND e.election.id = :electionId")
	boolean existsByBoothNumberAndElectionId(@Param("boothNumber") Integer boothNumber, @Param("electionId") Long electionId);
	
	Optional<ElectionBooth> findByBoothNumberAndElectionIdAndAccountId(Long boothNumber, Long electionId, Long accountId);
	List<ElectionBooth> findByElectionIdAndAccountId(Long electionId, Long accountId);
	
	@Query("SELECT eb FROM ElectionBooth eb " +
		       "INNER JOIN VoterEntity v ON eb.boothNumber = v.boothNumber AND eb.election.id = v.electionId AND eb.accountId = v.accountId " +
		       "WHERE eb.election.id = :electionId AND eb.accountId = :accountId " +
		       "GROUP BY eb.id, eb.boothNumber, eb.boothVulnerability, eb.orderIndex " +
		       "HAVING COUNT(v.id) > 0")
		Page<ElectionBooth> findByElectionIdAndAccountIdWithVoters(
		        @Param("electionId") Long electionId,
		        @Param("accountId") Long accountId,
		        Pageable pageable);
	
	@Query("SELECT eb FROM ElectionBooth eb " +
		       "INNER JOIN VoterEntity v ON eb.boothNumber = v.boothNumber AND eb.election.id = v.electionId AND eb.accountId = v.accountId " +
		       "WHERE eb.election.id = :electionId AND eb.accountId = :accountId AND eb.boothNumber IN :boothNumbers " +
		       "GROUP BY eb.id, eb.boothNumber, eb.boothVulnerability, eb.orderIndex " +
		       "HAVING COUNT(v.id) > 0")
		Page<ElectionBooth> findByElectionIdAndAccountIdAndBoothNumberInWithVoters(
		        @Param("electionId") Long electionId,
		        @Param("accountId") Long accountId,
		        @Param("boothNumbers") List<Integer> boothNumbers,
		        Pageable pageable);
	
	@Query("SELECT eb FROM ElectionBooth eb " +
		       "INNER JOIN VoterEntity v ON eb.boothNumber = v.boothNumber AND eb.election.id = v.electionId AND eb.accountId = v.accountId " +
		       "WHERE eb.election.id = :electionId AND eb.accountId = :accountId AND eb.boothNumber = :boothNumber " +
		       "GROUP BY eb.id, eb.boothNumber, eb.boothVulnerability, eb.orderIndex " +
		       "HAVING COUNT(v.id) > 0")
		Optional<ElectionBooth> findByElectionIdAndBoothNumberAndAccountIdWithVoters(
		        @Param("electionId") Long electionId,
		        @Param("accountId") Long accountId,
		        @Param("boothNumber") Integer boothNumber);
	
}
