package com.thedal.thedal_app.report.pollday;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface PollDayFamilyWisePollingRepository extends JpaRepository<PollDayFamilyWisePolling, Long> {
    Optional<PollDayFamilyWisePolling> findByAccountIdAndElectionIdAndPollingDate(
        Long accountId, Long electionId, LocalDate pollingDate);
    
    int deleteByAccountIdAndElectionIdAndPollingDate(
        Long accountId, Long electionId, LocalDate pollingDate);
}
