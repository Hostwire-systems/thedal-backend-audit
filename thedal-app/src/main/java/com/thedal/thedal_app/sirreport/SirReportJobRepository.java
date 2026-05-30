package com.thedal.thedal_app.sirreport;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SirReportJobRepository extends JpaRepository<SirReportJobEntity, Long> {
    
    Optional<SirReportJobEntity> findByJobId(UUID jobId);
    
    Page<SirReportJobEntity> findByAccountIdAndElectionIdOrderByCreatedAtDesc(
        Long accountId, Long electionId, Pageable pageable);
    
    Page<SirReportJobEntity> findByAccountIdOrderByCreatedAtDesc(
        Long accountId, Pageable pageable);
}
