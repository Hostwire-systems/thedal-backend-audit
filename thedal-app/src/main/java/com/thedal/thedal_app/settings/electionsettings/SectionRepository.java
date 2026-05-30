package com.thedal.thedal_app.settings.electionsettings;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.transaction.Transactional;

@Repository
public interface SectionRepository extends JpaRepository<SectionEntity, Long> {
    
    // Find Section by electionId
    List<SectionEntity> findByElectionId(Long electionId);

	boolean existsByPartNoAndElectionIdAndAccountId(Integer partNo, Long electionId, Long accountId);

	boolean existsBySectionNoAndElectionIdAndAccountId(Integer sectionNo, Long electionId, Long accountId);

	Optional<SectionEntity> findByElectionIdAndPartNoAndSectionNoAndAccountId(Long electionId, Integer partNo,
			Integer sectionNo, Long accountId);

	List<SectionEntity> findByElectionIdAndAccountId(Long electionId, Long accountId);

	Optional<SectionEntity> findByIdAndElectionIdAndAccountId(Long electionId, Long id, Long accountId);

	@Query("SELECT DISTINCT s.partNo FROM SectionEntity s WHERE s.election.id = :electionId AND s.accountId = :accountId AND s.partNo IN :partNos")
    Set<Integer> findExistingPartNos(@Param("electionId") Long electionId, @Param("accountId") Long accountId, @Param("partNos") Set<Integer> partNos);
    // Find Section by electionId and partNo
    //Optional<SectionEntity> findByElectionIdAndPartNo(Long electionId, String partNo);
    
    
//    boolean existsByPartNoAndElectionId(String partNo, Long electionId);
    //boolean existsByPartNoAndElectionId(Integer partNo, Long electionId);

	    @Modifying
	    @Transactional
	    @Query("DELETE FROM SectionEntity s WHERE s.election.id = :electionId AND s.accountId = :accountId")
	    int deleteByElectionIdAndAccountId(@Param("electionId") Long electionId, 
	                                       @Param("accountId") Long accountId);

	    @Modifying
	    @Transactional
	    @Query("DELETE FROM SectionEntity s WHERE s.election.id = :electionId AND s.accountId = :accountId AND s.id IN :sectionIds")
	    int deleteByElectionIdAndAccountIdAndIdIn(@Param("electionId") Long electionId, 
	                                              @Param("accountId") Long accountId, 
	                                              @Param("sectionIds") List<Long> sectionIds);

	    List<SectionEntity> findByElectionIdAndAccountIdAndSectionNoIn(Long electionId, Long accountId, List<String> sectionNos);
		boolean existsBySectionNoAndPartNoAndElectionIdAndAccountIdAndIdNot(
			Integer sectionNo, Integer partNo, Long electionId, Long accountId, Long id
		);
		boolean existsBySectionNoAndPartNoAndElectionIdAndAccountId(Integer sectionNo, Integer partNo, Long electionId, Long accountId);

		@Query("SELECT s FROM SectionEntity s WHERE s.election.id = :electionId AND s.accountId = :accountId " +
			       "AND s.partNo IN :partNos AND s.sectionNo IN :sectionNos")
			List<SectionEntity> findByElectionIdAndAccountIdAndPartNoInAndSectionNoIn(
			    Long electionId, 
			    Long accountId, 
			    List<Integer> partNos, 
			    List<Integer> sectionNos
			);


}
