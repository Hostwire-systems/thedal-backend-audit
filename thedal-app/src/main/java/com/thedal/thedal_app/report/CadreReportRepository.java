package com.thedal.thedal_app.report;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CadreReportRepository extends JpaRepository<CadreReportEntity, Long> {
    List<CadreReportEntity> findByElectionIdAndAccountId(Long electionId, Long accountId);
}