package com.thedal.thedal_app.report.aggregates;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ElectionDashboardBoothProgressRepository extends JpaRepository<ElectionDashboardBoothProgress, Long> {
    Optional<ElectionDashboardBoothProgress> findByAccountIdAndElectionId(Long accountId, Long electionId);
}