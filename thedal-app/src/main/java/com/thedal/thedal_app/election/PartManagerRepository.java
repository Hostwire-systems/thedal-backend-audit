package com.thedal.thedal_app.election;


import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.transaction.Transactional;

@Repository
public interface PartManagerRepository extends JpaRepository<PartManager, Long>{

   Optional<PartManager> findByElectionIdAndPartNoAndAccountId(Long electionId,String part_no,Long accountId);
   
   // Custom query to handle non-breaking spaces (ASCII 160) in part_no values for exact match
   @Query("SELECT pm FROM PartManager pm WHERE pm.electionId = :electionId AND pm.accountId = :accountId AND (TRIM(BOTH ' ' FROM REPLACE(pm.partNo, CHR(160), '')) = :partNo OR pm.partNo = :partNo)")
   Optional<PartManager> findByElectionIdAndPartNoAndAccountIdTrimmed(@Param("electionId") Long electionId, 
                                                                      @Param("partNo") String partNo, 
                                                                      @Param("accountId") Long accountId);
   List<PartManager> findByElectionId(Long electionId);
   @Query(value = "SELECT * FROM part_manager pm WHERE pm.election_id = :electionId AND pm.account_id = :accountId " +
          "ORDER BY CASE WHEN pm.part_no ~ '^[0-9]+$' THEN CAST(pm.part_no AS INTEGER) ELSE 999999 END ASC, pm.part_no ASC",
          nativeQuery = true)
   List<PartManager> findByElectionIdAndAccountId(@Param("electionId") Long electionId, @Param("accountId") Long accountId);
   
   @Query("SELECT pm FROM PartManager pm WHERE pm.accountId = :accountId AND pm.electionId = :electionId AND pm.id = :id")
   Optional<PartManager> findByAccountIdAndElectionIdAndId(@Param("accountId") Long accountId, @Param("electionId") Long electionId, @Param("id") Long id);
   
   @Query("SELECT pm FROM PartManager pm WHERE pm.partNo = :partNo AND pm.electionId = :electionId AND pm.accountId = :accountId")
   Optional<PartManager> findByPartNoAndElectionIdAndAccountId(@Param("partNo") String partNo, @Param("electionId") Long electionId, @Param("accountId") Long accountId);
   
   @Query("SELECT DISTINCT pm.partNo FROM PartManager pm WHERE pm.electionId = :electionId AND pm.accountId = :accountId AND pm.partNo IN :partNos")
   Set<String> findExistingPartNos(@Param("electionId") Long electionId, @Param("accountId") Long accountId, @Param("partNos") Set<Integer> partNos);
   
   @Query("SELECT pm FROM PartManager pm WHERE pm.electionId = :electionId AND pm.accountId = :accountId AND pm.partNo = :partNo")
   Optional<PartManager> findByElectionIdAndAccountIdAndPartNo(@Param("electionId") Long electionId, @Param("accountId") Long accountId, @Param("partNo") String partNo);
   
   @Query("SELECT pm FROM PartManager pm WHERE pm.electionId = :electionId AND pm.accountId = :accountId AND pm.partNo IN :partNos")
   List<PartManager> findByElectionIdAndAccountIdAndPartNoIn(@Param("electionId") Long electionId, @Param("accountId") Long accountId, @Param("partNos") List<String> partNos);
   
   // Custom query to handle non-breaking spaces (ASCII 160) in part_no values with numerical sorting
   @Query(value = "SELECT * FROM part_manager pm WHERE pm.election_id = :electionId AND pm.account_id = :accountId AND (TRIM(BOTH ' ' FROM REPLACE(pm.part_no, CHR(160), '')) IN :partNos OR pm.part_no IN :partNos) " +
          "ORDER BY CASE WHEN pm.part_no ~ '^[0-9]+$' THEN CAST(pm.part_no AS INTEGER) ELSE 999999 END ASC, pm.part_no ASC",
          nativeQuery = true)
   List<PartManager> findByElectionIdAndAccountIdAndTrimmedPartNoIn(@Param("electionId") Long electionId, 
                                                                    @Param("accountId") Long accountId, 
                                                                    @Param("partNos") List<String> partNos);
   
   @Modifying
   @Transactional
   @Query("DELETE FROM PartManager pm WHERE pm.accountId = :accountId AND pm.electionId = :electionId")
   int deleteByAccountIdAndElectionId(@Param("accountId") Long accountId, 
                                      @Param("electionId") Long electionId);

   @Modifying
   @Transactional
   @Query("DELETE FROM PartManager pm WHERE pm.accountId = :accountId AND pm.electionId = :electionId AND pm.id IN :partManagerIds")
   int deleteByAccountIdAndElectionIdAndIdIn(@Param("accountId") Long accountId, 
                                             @Param("electionId") Long electionId, 
                                             @Param("partManagerIds") List<Long> partManagerIds);

@Query("SELECT CASE WHEN COUNT(pm) > 0 THEN true ELSE false END FROM PartManager pm WHERE pm.electionId = :electionId AND pm.accountId = :accountId AND pm.partNo = :partNo")
boolean existsByElectionIdAndAccountIdAndPartNo(@Param("electionId") Long electionId, @Param("accountId") Long accountId, @Param("partNo") String partNoStr);

@Query("SELECT pm FROM PartManager pm WHERE pm.id = :partId AND pm.electionId = :electionId AND pm.accountId = :accountId")
Optional<PartManager> findByIdAndElectionIdAndAccountId(@Param("partId") Long partId, @Param("electionId") Long electionId, @Param("accountId") Long accountId);

@Query(value = "SELECT * FROM part_manager pm WHERE pm.election_id = :electionId AND pm.account_id = :accountId AND pm.part_no IN :assignedParts " +
       "ORDER BY CASE WHEN pm.part_no ~ '^[0-9]+$' THEN CAST(pm.part_no AS INTEGER) ELSE 999999 END ASC, pm.part_no ASC",
       nativeQuery = true,
       countQuery = "SELECT COUNT(*) FROM part_manager pm WHERE pm.election_id = :electionId AND pm.account_id = :accountId AND pm.part_no IN :assignedParts")
Page<PartManager> findByElectionIdAndAccountIdAndPartNoIn(@Param("electionId") Long electionId, @Param("accountId") Long accountId, @Param("assignedParts") List<String> assignedParts,
		Pageable pageable);
		
   // Custom paginated query to handle non-breaking spaces (ASCII 160) in part_no values with numerical sorting
   @Query(value = "SELECT * FROM part_manager pm WHERE pm.election_id = :electionId AND pm.account_id = :accountId AND (TRIM(BOTH ' ' FROM REPLACE(pm.part_no, CHR(160), '')) IN :partNos OR pm.part_no IN :partNos) " +
          "ORDER BY CASE WHEN pm.part_no ~ '^[0-9]+$' THEN CAST(pm.part_no AS INTEGER) ELSE 999999 END ASC, pm.part_no ASC",
          nativeQuery = true,
          countQuery = "SELECT COUNT(*) FROM part_manager pm WHERE pm.election_id = :electionId AND pm.account_id = :accountId AND (TRIM(BOTH ' ' FROM REPLACE(pm.part_no, CHR(160), '')) IN :partNos OR pm.part_no IN :partNos)")
   Page<PartManager> findByElectionIdAndAccountIdAndTrimmedPartNoInPaginated(@Param("electionId") Long electionId, 
                                                                             @Param("accountId") Long accountId, 
                                                                             @Param("partNos") List<String> partNos,
                                                                             Pageable pageable);
@Query(value = "SELECT * FROM part_manager pm WHERE pm.election_id = :electionId AND pm.account_id = :accountId " +
       "ORDER BY CASE WHEN pm.part_no ~ '^[0-9]+$' THEN CAST(pm.part_no AS INTEGER) ELSE 999999 END ASC, pm.part_no ASC",
       nativeQuery = true,
       countQuery = "SELECT COUNT(*) FROM part_manager pm WHERE pm.election_id = :electionId AND pm.account_id = :accountId")
Page<PartManager> findByElectionIdAndAccountId(@Param("electionId") Long electionId, @Param("accountId") Long accountId, Pageable pageable);
   
@Query(value = "SELECT * FROM part_manager pm WHERE pm.account_id = :accountId AND pm.election_id = :electionId " +
       "ORDER BY CASE WHEN pm.part_no ~ '^[0-9]+$' THEN CAST(pm.part_no AS INTEGER) ELSE 999999 END ASC, pm.part_no ASC",
       nativeQuery = true)
List<PartManager> findByAccountIdAndElectionId(@Param("accountId") Long accountId, @Param("electionId") Long electionId);

@Query("SELECT pm FROM PartManager pm WHERE pm.accountId = :accountId AND pm.electionId = :electionId AND pm.id IN :ids")
List<PartManager> findByAccountIdAndElectionIdAndIdIn(Long accountId, Long electionId, List<Long> ids);

@Query("SELECT pm FROM PartManager pm WHERE pm.id = :partManagerId AND pm.accountId = :accountId AND pm.electionId = :electionId")
Optional<PartManager> findByIdAndAccountIdAndElectionId(@Param("partManagerId") Long partManagerId, @Param("accountId") Long accountId, @Param("electionId") Long electionId);

@Query("SELECT MAX(p.orderIndex) FROM PartManager p WHERE p.electionId = :electionId")
Integer findMaxOrderIndexByElectionId(Long electionId);

@Query("SELECT pm FROM PartManager pm WHERE pm.id IN :assignedParts")
Optional<PartManager> findByIdIn(@Param("assignedParts") List<Long> assignedParts);

@Query("SELECT pm FROM PartManager pm WHERE (TRIM(BOTH ' ' FROM REPLACE(pm.partNo, CHR(160), '')) = :partNo OR pm.partNo = :partNo) AND pm.accountId = :accountId AND pm.electionId = :electionId")
Optional<PartManager> findByPartNoAndAccountIdAndElectionId(@Param("partNo") String partNo, @Param("accountId") Long accountId, @Param("electionId") Long electionId);

@Query(value = "SELECT * FROM part_manager pm " +
       "WHERE pm.account_id = :accountId " +
       "AND pm.election_id = :electionId " +
       "ORDER BY " +
       "CASE WHEN pm.part_no ~ '^[0-9]+$' THEN CAST(pm.part_no AS INTEGER) ELSE 999999 END ASC, " +
       "pm.part_no ASC",
       nativeQuery = true,
       countQuery = "SELECT COUNT(*) FROM part_manager pm " +
                   "WHERE pm.account_id = :accountId " +
                   "AND pm.election_id = :electionId")
Page<PartManager> findByAccountIdAndElectionIdOptimized(
    @Param("accountId") Long accountId,
    @Param("electionId") Long electionId,
    Pageable pageable);

@Query(value = "SELECT * FROM part_manager pm " +
       "WHERE pm.account_id = :accountId " +
       "AND pm.election_id = :electionId " +
       "AND (:partNo IS NULL OR pm.part_no = :partNo) " +
       "AND (:partNameEn IS NULL OR LOWER(pm.part_name_english) LIKE LOWER(CONCAT('%', :partNameEn, '%'))) " +
       "AND (:schoolName IS NULL OR LOWER(pm.school_name) LIKE LOWER(CONCAT('%', :schoolName, '%'))) " +
       "AND (:pincode IS NULL OR pm.pincode = :pincode) " +
       "AND (:vulnerability IS NULL OR pm.booth_vulnerability = :vulnerability) " +
       "ORDER BY CASE WHEN pm.part_no ~ '^[0-9]+$' THEN CAST(pm.part_no AS INTEGER) ELSE 999999 END ASC, pm.part_no ASC",
       nativeQuery = true,
       countQuery = "SELECT COUNT(*) FROM part_manager pm " +
                   "WHERE pm.account_id = :accountId " +
                   "AND pm.election_id = :electionId " +
                   "AND (:partNo IS NULL OR pm.part_no = :partNo) " +
                   "AND (:partNameEn IS NULL OR LOWER(pm.part_name_english) LIKE LOWER(CONCAT('%', :partNameEn, '%'))) " +
                   "AND (:schoolName IS NULL OR LOWER(pm.school_name) LIKE LOWER(CONCAT('%', :schoolName, '%'))) " +
                   "AND (:pincode IS NULL OR pm.pincode = :pincode) " +
                   "AND (:vulnerability IS NULL OR pm.booth_vulnerability = :vulnerability)")
Page<PartManager> findByAccountIdAndElectionIdWithFiltersOptimized(
    @Param("accountId") Long accountId,
    @Param("electionId") Long electionId,
    @Param("partNo") String partNo,
    @Param("partNameEn") String partNameEn,
    @Param("schoolName") String schoolName,
    @Param("pincode") String pincode,
    @Param("vulnerability") String vulnerability,
    Pageable pageable);

@Query("SELECT " +
       "COALESCE(COUNT(pm), 0) as totalCount, " +
       "COALESCE(SUM(CASE WHEN UPPER(pm.boothVulnerability) = 'HIGH' THEN 1 ELSE 0 END), 0) as highVulnerabilityCount, " +
       "COALESCE(SUM(CASE WHEN UPPER(pm.boothVulnerability) = 'MEDIUM' THEN 1 ELSE 0 END), 0) as mediumVulnerabilityCount, " +
       "COALESCE(SUM(CASE WHEN UPPER(pm.boothVulnerability) = 'LOW' THEN 1 ELSE 0 END), 0) as lowVulnerabilityCount " +
       "FROM PartManager pm " +
       "WHERE pm.accountId = :accountId " +
       "AND pm.electionId = :electionId " +
       "AND (:partNo IS NULL OR pm.partNo = :partNo) " +
       "AND (:partNameEn IS NULL OR LOWER(pm.partNameEnglish) LIKE LOWER(CONCAT('%', :partNameEn, '%'))) " +
       "AND (:schoolName IS NULL OR LOWER(pm.schoolName) LIKE LOWER(CONCAT('%', :schoolName, '%'))) " +
       "AND (:pincode IS NULL OR pm.pincode = :pincode) " +
       "AND (:vulnerability IS NULL OR pm.boothVulnerability = :vulnerability)")
PartManagerStatsProjection getPartManagerStatsOptimized(
    @Param("accountId") Long accountId,
    @Param("electionId") Long electionId,
    @Param("partNo") String partNo,
    @Param("partNameEn") String partNameEn,
    @Param("schoolName") String schoolName,
    @Param("pincode") String pincode,
    @Param("vulnerability") String vulnerability);

@Query("SELECT COUNT(pm) > 0 FROM PartManager pm " +
       "WHERE pm.accountId = :accountId AND pm.electionId = :electionId")
boolean existsByAccountIdAndElectionIdFast(
    @Param("accountId") Long accountId, 
    @Param("electionId") Long electionId);

@Query("SELECT COUNT(pm) FROM PartManager pm " +
       "WHERE pm.accountId = :accountId AND pm.electionId = :electionId")
Long countByAccountIdAndElectionId(
    @Param("accountId") Long accountId, 
    @Param("electionId") Long electionId);

@Query(value = "SELECT * FROM part_manager pm " +
       "WHERE pm.election_id = :electionId " +
       "AND pm.account_id = :accountId " +
       "AND pm.part_no IN :assignedPartNos " +
       "ORDER BY CASE WHEN pm.part_no ~ '^[0-9]+$' THEN CAST(pm.part_no AS INTEGER) ELSE 999999 END ASC, pm.part_no ASC",
       nativeQuery = true,
       countQuery = "SELECT COUNT(*) FROM part_manager pm " +
                   "WHERE pm.election_id = :electionId " +
                   "AND pm.account_id = :accountId " +
                   "AND pm.part_no IN :assignedPartNos")
Page<PartManager> findByElectionIdAndAccountIdAndPartNoInOptimized(
    @Param("electionId") Long electionId,
    @Param("accountId") Long accountId, 
    @Param("assignedPartNos") List<String> assignedPartNos,
    Pageable pageable);


//@Query("SELECT pm.partNo FROM PartManager pm WHERE pm.electionId = :electionId AND pm.accountId = :accountId")
//Page<String> findPartNosByElectionIdAndAccountId(@Param("electionId") Long electionId, 
//                                                    @Param("accountId") Long accountId, 
//                                                    Pageable pageable);

@Query("SELECT pm.partNo FROM PartManager pm WHERE pm.electionId = :electionId AND pm.accountId = :accountId")
Page<String> findPartNumbersByElectionIdAndAccountId(
    @Param("electionId") Long electionId, 
    @Param("accountId") Long accountId, 
    Pageable pageable);

@Query("SELECT pm FROM PartManager pm WHERE pm.accountId = :accountId")
Page<PartManager> findByAccountIdOptimized(@Param("accountId") Long accountId, Pageable pageable);

@Query("SELECT COUNT(pm) FROM PartManager pm WHERE pm.accountId = :accountId")
Long countByAccountId(@Param("accountId") Long accountId);

@Query("SELECT pm FROM PartManager pm WHERE pm.electionId = :electionId AND pm.accountId = :accountId ORDER BY pm.orderIndex ASC")
List<PartManager> findByElectionIdAndAccountIdOrderByOrderIndexAsc(@Param("electionId") Long electionId, @Param("accountId") Long accountId);


}
