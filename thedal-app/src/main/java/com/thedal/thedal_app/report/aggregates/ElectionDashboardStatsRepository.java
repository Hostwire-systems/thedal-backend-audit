package com.thedal.thedal_app.report.aggregates;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ElectionDashboardStatsRepository extends JpaRepository<ElectionDashboardStats, Long> {
    
    // Find election-wide stats (part_no is NULL)
    @Query("SELECT s FROM ElectionDashboardStats s WHERE s.accountId = :accountId AND s.electionId = :electionId AND s.partNo IS NULL")
    Optional<ElectionDashboardStats> findByAccountIdAndElectionId(@Param("accountId") Long accountId, @Param("electionId") Long electionId);
    
    // Find part-specific stats
    Optional<ElectionDashboardStats> findByAccountIdAndElectionIdAndPartNo(Long accountId, Long electionId, String partNo);
    
    // Find all parts for an election (excluding election-wide)
    @Query("SELECT s FROM ElectionDashboardStats s WHERE s.accountId = :accountId AND s.electionId = :electionId AND s.partNo IS NOT NULL")
    List<ElectionDashboardStats> findAllPartsByAccountIdAndElectionId(@Param("accountId") Long accountId, @Param("electionId") Long electionId);
}
