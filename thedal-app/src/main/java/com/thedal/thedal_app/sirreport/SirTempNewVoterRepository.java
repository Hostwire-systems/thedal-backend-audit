package com.thedal.thedal_app.sirreport;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface SirTempNewVoterRepository extends JpaRepository<SirTempNewVoterEntity, SirTempNewVoterEntity.CompositeId> {
    
    @Modifying
    @Query(value = "DELETE FROM sir_temp_new_voters WHERE job_id = :jobId", nativeQuery = true)
    void deleteByJobId(@Param("jobId") UUID jobId);
    
    @Modifying
    @Query(value = "DELETE FROM sir_temp_new_voters WHERE job_id IN (SELECT DISTINCT job_id FROM sir_report_job WHERE completed_at < :cutoffDate)", nativeQuery = true)
    int deleteOldRecords(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    @Query(value = "SELECT COUNT(*) FROM sir_temp_new_voters WHERE job_id = :jobId", nativeQuery = true)
    long countByJobId(@Param("jobId") UUID jobId);
}
