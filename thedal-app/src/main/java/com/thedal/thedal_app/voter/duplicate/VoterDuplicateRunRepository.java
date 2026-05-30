package com.thedal.thedal_app.voter.duplicate;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoterDuplicateRunRepository extends JpaRepository<VoterDuplicateRun, Long> {
    Optional<VoterDuplicateRun> findTopByAccountIdAndElectionIdOrderByStartedAtDesc(Long accountId, Long electionId);
    long countByAccountIdAndElectionIdAndScopeAndStartedAtAfter(Long accountId, Long electionId, VoterDuplicateRun.Scope scope, LocalDateTime after);
}
