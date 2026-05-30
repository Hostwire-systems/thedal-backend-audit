package com.thedal.thedal_app.report.aggregates;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CadreDashboardStatsRepository extends JpaRepository<CadreDashboardStats, Long> {
    Optional<CadreDashboardStats> findByAccountIdAndElectionId(Long accountId, Long electionId);
}
