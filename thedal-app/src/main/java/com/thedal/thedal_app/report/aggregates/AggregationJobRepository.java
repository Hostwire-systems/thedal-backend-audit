package com.thedal.thedal_app.report.aggregates;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AggregationJobRepository extends JpaRepository<AggregationJob, Long> {
    
    Optional<AggregationJob> findByJobId(String jobId);
    
    List<AggregationJob> findByAccountIdAndElectionIdOrderByStartedAtDesc(Long accountId, Long electionId);
    
    @Query("SELECT j FROM AggregationJob j WHERE j.accountId = :accountId AND j.electionId = :electionId " +
           "ORDER BY j.startedAt DESC")
    List<AggregationJob> findRecentJobs(@Param("accountId") Long accountId, 
                                        @Param("electionId") Long electionId,
                                        org.springframework.data.domain.Pageable pageable);
    
    @Query("SELECT j FROM AggregationJob j WHERE j.status = :status " +
           "AND j.startedAt < :before ORDER BY j.startedAt ASC")
    List<AggregationJob> findStaleJobs(@Param("status") AggregationJobStatus status, 
                                       @Param("before") LocalDateTime before);
    
    Long countByAccountIdAndElectionIdAndStatus(Long accountId, Long electionId, AggregationJobStatus status);
}
