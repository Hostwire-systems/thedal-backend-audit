package com.thedal.thedal_app.report.aggregates;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ElectionDashboardContactStatusRepository extends JpaRepository<ElectionDashboardContactStatus, Long> {
    Optional<ElectionDashboardContactStatus> findByAccountIdAndElectionId(Long accountId, Long electionId);
}
