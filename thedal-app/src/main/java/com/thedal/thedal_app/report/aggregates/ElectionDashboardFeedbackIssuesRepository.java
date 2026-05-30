package com.thedal.thedal_app.report.aggregates;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ElectionDashboardFeedbackIssuesRepository extends JpaRepository<ElectionDashboardFeedbackIssues, Long> {
    Optional<ElectionDashboardFeedbackIssues> findByAccountIdAndElectionId(Long accountId, Long electionId);
}