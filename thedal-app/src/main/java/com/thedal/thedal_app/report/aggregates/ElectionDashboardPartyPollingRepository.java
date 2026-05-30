package com.thedal.thedal_app.report.aggregates;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ElectionDashboardPartyPollingRepository extends JpaRepository<ElectionDashboardPartyPolling, Long> {
    
    // Find election-wide party polling (part_no is NULL)
    @Query("SELECT p FROM ElectionDashboardPartyPolling p WHERE p.accountId = :accountId AND p.electionId = :electionId AND p.partNo IS NULL")
    Optional<ElectionDashboardPartyPolling> findByAccountIdAndElectionId(@Param("accountId") Long accountId, @Param("electionId") Long electionId);
    
    // Find part-specific party polling
    Optional<ElectionDashboardPartyPolling> findByAccountIdAndElectionIdAndPartNo(Long accountId, Long electionId, String partNo);
    
    // Find all parts for an election (excluding election-wide)
    @Query("SELECT p FROM ElectionDashboardPartyPolling p WHERE p.accountId = :accountId AND p.electionId = :electionId AND p.partNo IS NOT NULL")
    List<ElectionDashboardPartyPolling> findAllPartsByAccountIdAndElectionId(@Param("accountId") Long accountId, @Param("electionId") Long electionId);
}