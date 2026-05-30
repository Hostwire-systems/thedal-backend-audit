package com.thedal.reporting.pollday;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.Optional;

public interface PollDayAgeGroupTurnoutRepository extends JpaRepository<PollDayAgeGroupTurnout, Long> {
    Optional<PollDayAgeGroupTurnout> findByAccountIdAndElectionIdAndPollingDate(Long accountId, Long electionId, LocalDate pollingDate);
}
