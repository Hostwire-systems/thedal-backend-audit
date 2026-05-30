package com.thedal.thedal_app.report;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ElectionDashboardRepository extends JpaRepository<ElectionDashboardEntity, Long> {
    //List<ElectionDashboardEntity> findByElectionIdAndUserId(Long electionId, Long userId);
	
	Optional<ElectionDashboardEntity> findByElectionId(Long electionId);
    Optional<ElectionDashboardEntity> findByElectionIdAndBoothNumber(Long electionId, String boothNumber);
	Optional<ElectionDashboardEntity> findByElectionIdAndBoothNumberIsNull(Long electionId);
	long countByElectionId(Long electionId);
	
}