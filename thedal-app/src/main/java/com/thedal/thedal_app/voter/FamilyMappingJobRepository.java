package com.thedal.thedal_app.voter;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FamilyMappingJobRepository extends JpaRepository<FamilyMappingJobEntity, Long> {

    Optional<FamilyMappingJobEntity> findByAccountIdAndElectionId(Long accountId, Long electionId);

    List<FamilyMappingJobEntity> findByAccountIdOrderByCreatedAtDesc(Long accountId);

    List<FamilyMappingJobEntity> findByElectionIdOrderByCreatedAtDesc(Long electionId);

    @Query("SELECT f FROM FamilyMappingJobEntity f WHERE f.accountId = :accountId AND f.electionId = :electionId ORDER BY f.createdAt DESC")
    List<FamilyMappingJobEntity> findByAccountIdAndElectionIdOrderByCreatedAtDesc(
            @Param("accountId") Long accountId, 
            @Param("electionId") Long electionId);

    @Query("SELECT f FROM FamilyMappingJobEntity f WHERE f.status = :status ORDER BY f.createdAt DESC")
    List<FamilyMappingJobEntity> findByStatusOrderByCreatedAtDesc(@Param("status") BulkUploadStatus status);

    boolean existsByAccountIdAndElectionIdAndStatus(Long accountId, Long electionId, BulkUploadStatus status);

    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM FamilyMappingJobEntity f WHERE f.accountId = :accountId AND f.electionId = :electionId AND f.run = true")
    boolean existsByAccountIdAndElectionIdAndRunTrue(@Param("accountId") Long accountId, @Param("electionId") Long electionId);

    Optional<FamilyMappingJobEntity> findByAccountIdAndElectionIdAndRunTrue(Long accountId, Long electionId);
}
