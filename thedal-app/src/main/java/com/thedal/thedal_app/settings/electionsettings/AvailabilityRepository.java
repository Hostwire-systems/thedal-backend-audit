package com.thedal.thedal_app.settings.electionsettings;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.thedal.thedal_app.election.ElectionEntity;

@Repository
public interface AvailabilityRepository extends JpaRepository<Availability, Long>{

    List<Availability> findByAccountIdAndElectionId(Long accountId, Long electionId);
    Optional<Availability> findByAccountIdAndElectionIdAndId(Long accountId, Long electionId, Long id);
    // boolean existsByDescription(String description);
   
    
    boolean existsByAvailabilityNameAndAccountIdAndElectionId(String availabilityName, Long accountId, Long electionId);
    boolean existsByDescriptionAndElectionId(String description, Long electionId);
	
    @Query("SELECT MAX(a.orderIndex) FROM Availability a WHERE a.electionId = :electionId")
    Integer findMaxOrderIndexByElectionId(@Param("electionId") Long electionId);

	List<Availability> findByElectionIdAndAccountIdOrderByOrderIndexAsc(Long electionId, Long accountId);
	List<Availability> findByAccountIdAndElectionIdOrderByOrderIndexAsc(Long accountId, Long electionId);
	
	Optional<Availability> findByIdAndAccountIdAndElectionId(Long id, Long accountId, Long electionId);
	int deleteByAccountIdAndElectionId(Long accountId, Long electionId);
	List<Availability> findByAccountIdAndElectionIdAndIdIn(Long accountId, Long electionId, List<Long> availabilityIds);
	int deleteByAccountIdAndElectionIdAndIdIn(Long accountId, Long electionId, List<Long> availabilityIds);
	List<Availability> findByIdInAndAccountIdAndElectionId(List<Long> ids, Long accountId, Long electionId);
    
    @Query("SELECT MIN(a.orderIndex) FROM Availability a WHERE a.electionId = :electionId")
    Integer findMinOrderIndexByElectionId(@Param("electionId") Long electionId);
    boolean existsByCategoryNameAndElectionId(String categoryName, Long electionId);
    List<Availability> findAllByElectionIdOrderByCreatedAtDesc(Long electionId);

List<Availability> findByAccountIdAndElectionIdOrderByCreatedAtDesc(Long accountId, Long electionId);

@Query("SELECT a, COUNT(v.id) as voterCount " +
        "FROM Availability a " +
        "LEFT JOIN a.voters v " +
        "WHERE a.accountId = :accountId AND a.electionId = :electionId " +
        "GROUP BY a")
 List<Object[]> findAvailabilitiesWithVoterCount(Long accountId, Long electionId);
 
 @Query("SELECT a.id, COUNT(v) FROM Availability a LEFT JOIN a.voters v " +
         "WHERE a.accountId = :accountId AND a.electionId = :electionId AND a.id IN :availabilityIds " +
         "GROUP BY a.id")
  Map<Long, Long> getVoterCountsByAvailabilityIds(Long accountId, Long electionId, List<Long> availabilityIds);
Optional<Availability> findByIdIn(List<Long> linkedIds);

Optional<Availability> findByDescriptionAndAccountIdAndElectionId(String description, Long accountId, Long electionId);
Optional<Availability> findByCategoryNameAndAccountIdAndElectionId(String categoryName, Long accountId, Long electionId);

long countByAccountIdAndElectionId(Long accountId, Long electionId);

}
