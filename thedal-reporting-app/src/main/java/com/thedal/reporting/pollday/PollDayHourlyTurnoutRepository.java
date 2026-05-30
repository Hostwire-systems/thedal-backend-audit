package com.thedal.reporting.pollday;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.Optional;

public interface PollDayHourlyTurnoutRepository extends JpaRepository<PollDayHourlyTurnout, Long> {
    Optional<PollDayHourlyTurnout> findByAccountIdAndElectionIdAndPollingDate(Long accountId, Long electionId, LocalDate pollingDate);
}
