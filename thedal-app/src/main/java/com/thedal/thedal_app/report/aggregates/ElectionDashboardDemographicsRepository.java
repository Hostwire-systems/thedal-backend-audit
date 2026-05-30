package com.thedal.thedal_app.report.aggregates;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ElectionDashboardDemographicsRepository extends JpaRepository<ElectionDashboardDemographics, Long> {
    
    // Find election-wide demographics (part_no is NULL)
    @Query("SELECT d FROM ElectionDashboardDemographics d WHERE d.accountId = :accountId AND d.electionId = :electionId AND d.partNo IS NULL")
    Optional<ElectionDashboardDemographics> findByAccountIdAndElectionId(@Param("accountId") Long accountId, @Param("electionId") Long electionId);
    
    // Find part-specific demographics
    Optional<ElectionDashboardDemographics> findByAccountIdAndElectionIdAndPartNo(Long accountId, Long electionId, String partNo);
    
    // Find all parts for an election (excluding election-wide)
    @Query("SELECT d FROM ElectionDashboardDemographics d WHERE d.accountId = :accountId AND d.electionId = :electionId AND d.partNo IS NOT NULL")
    List<ElectionDashboardDemographics> findAllPartsByAccountIdAndElectionId(@Param("accountId") Long accountId, @Param("electionId") Long electionId);
}