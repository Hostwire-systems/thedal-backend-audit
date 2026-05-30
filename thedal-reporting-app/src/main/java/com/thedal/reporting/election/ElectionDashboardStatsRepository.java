package com.thedal.reporting.election;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ElectionDashboardStatsRepository extends JpaRepository<ElectionDashboardStats, Long> {
    Optional<ElectionDashboardStats> findByAccountIdAndElectionId(Long accountId, Long electionId);
}
