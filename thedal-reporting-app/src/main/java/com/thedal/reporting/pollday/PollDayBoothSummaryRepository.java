package com.thedal.reporting.pollday;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.Optional;

public interface PollDayBoothSummaryRepository extends JpaRepository<PollDayBoothSummary, Long> {
    Optional<PollDayBoothSummary> findByAccountIdAndElectionIdAndPollingDate(Long accountId, Long electionId, LocalDate pollingDate);
}
