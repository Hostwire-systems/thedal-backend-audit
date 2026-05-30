package com.thedal.thedal_app.report.pollday;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface PollDayPartWisePollingRepository extends JpaRepository<PollDayPartWisePolling, Long> {
    Optional<PollDayPartWisePolling> findByAccountIdAndElectionIdAndPollingDate(
            Long accountId, Long electionId, LocalDate pollingDate);
}
