package com.thedal.thedal_app.report.pollday.export;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PollDayExportJobRepository extends JpaRepository<PollDayExportJob, Long> {

    @Query("SELECT j FROM PollDayExportJob j WHERE j.id = :jobId AND j.accountId = :accountId")
    Optional<PollDayExportJob> findByIdAndAccountId(
        @Param("jobId") Long jobId, 
        @Param("accountId") Long accountId
    );

    @Query("SELECT j FROM PollDayExportJob j WHERE j.accountId = :accountId AND j.electionId = :electionId ORDER BY j.createdAt DESC")
    List<PollDayExportJob> findByAccountIdAndElectionIdOrderByCreatedAtDesc(
        @Param("accountId") Long accountId,
        @Param("electionId") Long electionId
    );

    @Query("SELECT j FROM PollDayExportJob j WHERE j.status = :status AND j.createdAt < :threshold")
    List<PollDayExportJob> findOldJobsByStatus(
        @Param("status") PollDayExportJob.ExportStatus status,
        @Param("threshold") LocalDateTime threshold
    );
}
