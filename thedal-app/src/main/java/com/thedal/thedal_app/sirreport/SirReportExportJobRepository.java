package com.thedal.thedal_app.sirreport;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface SirReportExportJobRepository extends JpaRepository<SirReportExportJob, Long> {
    
    List<SirReportExportJob> findByAccountIdOrderByTimeStartedDesc(Long accountId);
    
    List<SirReportExportJob> findByAccountIdAndElectionIdOrderByTimeStartedDesc(Long accountId, Long electionId);
    
    List<SirReportExportJob> findByJobIdOrderByTimeStartedDesc(UUID jobId);
    
    List<SirReportExportJob> findByExpiresAtBefore(LocalDateTime dateTime);
}
