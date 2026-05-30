package com.thedal.thedal_app.sirreport;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SirTempBaseVoterRepository extends JpaRepository<SirTempBaseVoterEntity, SirTempBaseVoterEntity.CompositeId> {
    
    Optional<SirTempBaseVoterEntity> findByJobIdAndEpicNumber(UUID jobId, String epicNumber);
    
    @Modifying
    @Query(value = "DELETE FROM sir_temp_base_voters WHERE job_id = :jobId", nativeQuery = true)
    void deleteByJobId(@Param("jobId") UUID jobId);
    
    @Modifying
    @Query(value = "DELETE FROM sir_temp_base_voters WHERE job_id IN (SELECT DISTINCT job_id FROM sir_report_job WHERE completed_at < :cutoffDate)", nativeQuery = true)
    int deleteOldRecords(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    @Query(value = "SELECT COUNT(*) FROM sir_temp_base_voters WHERE job_id = :jobId", nativeQuery = true)
    long countByJobId(@Param("jobId") UUID jobId);
    
    @Query("SELECT t FROM SirTempBaseVoterEntity t WHERE t.jobId = :jobId")
    List<SirTempBaseVoterEntity> findAllByJobId(@Param("jobId") UUID jobId);
}
