package com.thedal.thedal_app.report;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PollDayReportRepository extends JpaRepository<PollDayReportEntity, Long> {
    List<PollDayReportEntity> findByElectionIdAndAccountId(Long electionId, Long accountId);
}