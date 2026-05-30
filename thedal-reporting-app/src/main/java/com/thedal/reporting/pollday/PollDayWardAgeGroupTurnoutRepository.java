package com.thedal.reporting.pollday;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface PollDayWardAgeGroupTurnoutRepository extends JpaRepository<PollDayWardAgeGroupTurnout, Long> {
    Optional<PollDayWardAgeGroupTurnout> findByAccountIdAndElectionIdAndPartNumberAndPollingDate(
            Long accountId, Long electionId, String partNumber, LocalDate pollingDate);
}
