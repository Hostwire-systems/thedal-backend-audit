package com.thedal.thedal_app.sirreport;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SirReportDeletionRepository extends JpaRepository<SirReportDeletionEntity, Long> {
    
    Page<SirReportDeletionEntity> findByJobId(UUID jobId, Pageable pageable);
    
    Long countByJobId(UUID jobId);
    
    List<SirReportDeletionEntity> findByJobId(UUID jobId);
    
    void deleteByJobId(UUID jobId);
}
