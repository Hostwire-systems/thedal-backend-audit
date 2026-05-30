package com.thedal.thedal_app.election;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PartManagerDownloadJobRepository extends JpaRepository<PartManagerDownloadJob, Long> {
    
    List<PartManagerDownloadJob> findByAccountIdAndElectionId(Long accountId, Long electionId);
    
    List<PartManagerDownloadJob> findByAccountIdAndElectionIdAndStatus(Long accountId, Long electionId, String status);
    
    @Query("SELECT j FROM PartManagerDownloadJob j WHERE j.accountId = :accountId AND j.electionId = :electionId " +
           "AND (:status IS NULL OR j.status = :status) " +
           "AND (:startDate IS NULL OR j.timeStarted >= :startDate) " +
           "AND (:endDate IS NULL OR j.timeStarted <= :endDate) " +
           "ORDER BY j.timeStarted DESC")
    Page<PartManagerDownloadJob> findByFilters(
            @Param("accountId") Long accountId,
            @Param("electionId") Long electionId,
            @Param("status") String status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);
    
    Optional<PartManagerDownloadJob> findByIdAndAccountId(Long id, Long accountId);
}
