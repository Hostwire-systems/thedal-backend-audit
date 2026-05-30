package com.thedal.thedal_app.settings.electionsettings;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.util.Streamable;
import org.springframework.stereotype.Repository;

import com.thedal.thedal_app.settings.electionsettings.dto.PartyResponseDTO;

@Repository
public interface PartyRepository extends JpaRepository<Party, Long>{
	
	 Optional<Party> findByIdAndAccountId(Long partyId, Long accountId);

	List<Party> findAllByElectionIdAndAccountId(Long electionId, Long accountId);

	boolean existsByPartyNameAndElectionIdAndIdNot(String partyName, Long electionId, Long id);
	
	boolean existsByPartyNameAndElectionId(String partyName, Long electionId);

	// Added for merge name lookups
	boolean existsByPartyNameAndAccountIdAndElectionId(String partyName, Long accountId, Long electionId);

	Optional<Party> findByIdAndElectionIdAndAccountId(Long partyId, Long electionId, Long accountId);

	Optional<Party> findByIdAndAccountIdAndElectionId(Long partyId, Long accountId, Long electionId);
	List<Party> findByIdInAndAccountIdAndElectionId(List<Long> partyIds, Long accountId, Long electionId);

	List<Party> findByAccountIdAndElectionId(Long accountId, Long electionId);
	@Query("SELECT MAX(p.orderIndex) FROM Party p WHERE p.electionId = :electionId")
	Integer findMaxOrderIndexByElectionId(@Param("electionId") Long electionId);
	
	@Query("SELECT p FROM Party p WHERE p.electionId = :electionId AND p.accountId = :accountId ORDER BY p.orderIndex ASC")
	List<Party> findAllByElectionIdAndAccountIdOrderByOrderIndexAsc(Long electionId, Long accountId);

	List<Party> findByElectionIdAndAccountIdOrderByOrderIndexAsc(Long electionId, Long accountId);

	List<Party> findByAccountIdAndElectionIdOrderByOrderIndexAsc(Long accountId, Long electionId);

	Optional<Party> findByAccountIdAndElectionIdAndId(Long accountId, Long electionId, Long partyId);

	//Optional<Party> findByIdAndAccountIdAndElectionId(Long id, Long accountId, Long electionId);

	int deleteByAccountIdAndElectionId(Long accountId, Long electionId);

	@Query("SELECT MIN(p.orderIndex) FROM Party p WHERE p.electionId = :electionId")
	Integer findMinOrderIndexByElectionId(@Param("electionId") Long electionId);
	List<Party> findByElectionIdAndAccountId(Long electionId, Long accountId);


    @Modifying
    @Query("DELETE FROM Party p WHERE p.accountId = :accountId AND p.electionId = :electionId AND p.id IN :partyIds")
    int deleteByAccountIdAndElectionIdAndIds(@Param("accountId") Long accountId, 
                                             @Param("electionId") Long electionId, 
                                             @Param("partyIds") List<Long> partyIds);

    @Query("SELECT p, COUNT(v.id) as voterCount " +
            "FROM Party p " +
            "LEFT JOIN p.voters v " +
            "WHERE p.accountId = :accountId AND p.electionId = :electionId " +
            "GROUP BY p")
     List<Object[]> findPartiesWithVoterCount(Long accountId, Long electionId);

     @Query("SELECT p FROM Party p WHERE LOWER(p.partyName) IN :names AND p.accountId = :accountId AND p.electionId = :electionId")
     List<Party> findByPartyNamesAndAccountIdAndElectionId(@Param("names") List<String> names, @Param("accountId") Long accountId, @Param("electionId") Long electionId);

	long countByAccountIdAndElectionId(Long accountId, Long electionId);
	
     
     
}
