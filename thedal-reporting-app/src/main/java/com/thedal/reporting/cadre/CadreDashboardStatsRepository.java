package com.thedal.reporting.cadre;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CadreDashboardStatsRepository extends JpaRepository<CadreDashboardStats, Long> {
    Optional<CadreDashboardStats> findByAccountIdAndElectionId(Long accountId, Long electionId);
}
