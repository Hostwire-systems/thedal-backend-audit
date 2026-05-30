package com.thedal.thedal_app.report.pollday;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PollDayChartConfigRepository extends JpaRepository<PollDayChartConfig, Long> {
    
    @Query("SELECT p FROM PollDayChartConfig p WHERE p.accountId = :accountId AND p.electionId = :electionId")
    Optional<PollDayChartConfig> findByAccountIdAndElectionId(
        @Param("accountId") Long accountId, 
        @Param("electionId") Long electionId
    );
    
    void deleteByAccountIdAndElectionId(Long accountId, Long electionId);
}
