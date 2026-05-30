package com.thedal.thedal_app.report.election;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ElectionDashboardOverviewRepository extends JpaRepository<ElectionDashboardOverviewEntity, Long> {

	Optional<ElectionDashboardOverviewEntity> findByElectionIdAndAccountId(Long electionId, Long accountId);

}