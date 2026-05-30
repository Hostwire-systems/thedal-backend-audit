package com.thedal.thedal_app.election;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ElectionRepository extends JpaRepository<ElectionEntity, Long> {
    
    Optional<ElectionEntity> findByIdAndAccountIdAndIsDeletedFalse(Long id, Long accountId);
    List<ElectionEntity> findByAccountId(Long accountId);
	Object findElectionById(Long electionId);
    
	@Query("SELECT e FROM ElectionEntity e WHERE e.accountId = :accountId AND e.isDeleted = false ORDER BY e.orderIndex ASC")
    //@Query("SELECT e FROM ElectionEntity e WHERE e.accountId = :accountId AND e.isDeleted = false")
    List<ElectionEntity> findAllActiveElections(@Param("accountId") Long accountId);
	//Optional<ElectionEntity> findByIdAndAccountId(Long electionId, Long currentAccountId);
    @Query("SELECT e FROM ElectionEntity e WHERE e.id = :id AND e.accountId = :accountId AND e.isDeleted = false")
    Optional<ElectionEntity> findByIdAndAccountId(@Param("id") Long id, @Param("accountId") Long accountId);

    @Query("SELECT e FROM ElectionEntity e WHERE e.id IN " +
    	       "(SELECT v.electionEntity.id FROM VolunteerEntity v " +
    	       "WHERE v.userEntity.id = :userId AND v.accountId = :accountId)")
    	List<ElectionEntity> findElectionsByUserId(Long userId, Long accountId);


//    @Query("SELECT e FROM ElectionEntity e " +
//    	       "JOIN VolunteerEntity v ON e.id = v.electionEntity.id " +
//    	       "WHERE v.userEntity.id = :userId AND v.accountId = :accountId " +
//    	       "ORDER BY e.orderIndex ASC")
    @Query("SELECT e FROM ElectionEntity e " +
           "JOIN VolunteerEntity v ON e.id = v.electionEntity.id " +
           "WHERE v.userEntity.id = :userId AND v.accountId = :accountId")
    List<ElectionEntity> findElectionsByVolunteer(@Param("userId") Long userId, @Param("accountId") Long accountId);

    @Modifying
    @Query("UPDATE ElectionEntity e SET e.orderIndex = e.orderIndex - 1 WHERE e.orderIndex BETWEEN :start AND :end")
    void shiftOrderUp(@Param("start") int start, @Param("end") int end);

    @Modifying
    @Query("UPDATE ElectionEntity e SET e.orderIndex = e.orderIndex + 1 WHERE e.orderIndex BETWEEN :start AND :end")
    void shiftOrderDown(@Param("start") int start, @Param("end") int end);
    
    @Query("SELECT MAX(e.orderIndex) FROM ElectionEntity e WHERE e.accountId = :accountId")
    Integer findMaxOrderIndexByAccountId(@Param("accountId") Long accountId);
    
    @Modifying
    @Query("UPDATE ElectionEntity e SET e.orderIndex = CASE e.id " +
           "WHEN :id1 THEN :orderIndex1 " +
           "WHEN :id2 THEN :orderIndex2 " +
           "ELSE e.orderIndex END WHERE e.id IN (:ids)")
    void batchUpdateOrderIndexes(
        @Param("ids") List<Long> ids,
        @Param("id1") Long id1, @Param("orderIndex1") Integer orderIndex1,
        @Param("id2") Long id2, @Param("orderIndex2") Integer orderIndex2
    );
	boolean existsByIdAndAccountId(Long electionId, Long accountId);
	
	@Query("SELECT e.id FROM ElectionEntity e WHERE e.accountId = :accountId")
    Optional<Long> findElectionIdByAccountId(@Param("accountId") Long accountId);
	
	List<ElectionEntity> findByAccountId(Long accountId, PageRequest of);
	
	@Query("SELECT e FROM ElectionEntity e WHERE e.accountId = :accountId AND e.isDeleted = false ORDER BY e.id")
	Page<ElectionEntity> findByAccountIdAndIsDeletedFalse(@Param("accountId") Long accountId, Pageable pageable);
	
	// Global migration support methods
	@Query("SELECT DISTINCT e.accountId, e.id FROM ElectionEntity e WHERE e.isDeleted = false ORDER BY e.accountId, e.id")
	List<Object[]> findDistinctAccountElectionPairs();
	
	@Query("SELECT DISTINCT e.accountId FROM ElectionEntity e WHERE e.isDeleted = false ORDER BY e.accountId")
	List<Long> findDistinctAccountIds();
	
	@Query("SELECT e.id FROM ElectionEntity e WHERE e.accountId = :accountId AND e.isDeleted = false ORDER BY e.id")
	List<Long> findElectionIdsByAccountId(@Param("accountId") Long accountId);
	
	@Query("SELECT COUNT(e) FROM ElectionEntity e WHERE e.isDeleted = false")
	long countAllActiveElections();
	
	@Query("SELECT COUNT(DISTINCT e.accountId) FROM ElectionEntity e WHERE e.isDeleted = false")
	long countDistinctAccounts();
}
