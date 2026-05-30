package com.thedal.thedal_app.voter.duplicate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;

@Slf4j
@Service
@RequiredArgsConstructor
public class DuplicateRunService {
    private final VoterDuplicateRunRepository runRepo;
    private final DuplicateRunProcessor processor;

    // Create a run and process asynchronously to avoid blocking the request thread
    @Transactional
    public VoterDuplicateRun startElectionRun(Long accountId, Long electionId, Long triggeredBy) {
        enforceCooldown(accountId, electionId);
        VoterDuplicateRun run = new VoterDuplicateRun();
        run.setAccountId(accountId);
        run.setElectionId(electionId);
        run.setScope(VoterDuplicateRun.Scope.ELECTION);
        run.setStatus(VoterDuplicateRun.Status.RUNNING);
        run.setStartedAt(LocalDateTime.now());
        run.setTriggeredBy(triggeredBy);
        run = runRepo.save(run);

        // Kick off async processing
        processor.processElectionRunAsync(run.getId());
        return run;
    }

    @Transactional
    public VoterDuplicateRun startBatchRun(Long accountId, Long electionId, Long bulkUploadId, Collection<Long> voterIds, Long triggeredBy) {
        VoterDuplicateRun run = new VoterDuplicateRun();
        run.setAccountId(accountId);
        run.setElectionId(electionId);
        run.setScope(VoterDuplicateRun.Scope.BATCH);
        run.setStatus(VoterDuplicateRun.Status.RUNNING);
        run.setStartedAt(LocalDateTime.now());
        run.setBulkUploadId(bulkUploadId);
        run.setTriggeredBy(triggeredBy);
        run = runRepo.save(run);

        // Kick off async processing for batch
        processor.processBatchRunAsync(run.getId(), voterIds);
        return run;
    }

    private void enforceCooldown(Long accountId, Long electionId) {
        LocalDateTime since = LocalDateTime.now().minusDays(1);
        long runs = runRepo.countByAccountIdAndElectionIdAndScopeAndStartedAtAfter(accountId, electionId, VoterDuplicateRun.Scope.ELECTION, since);
        if (runs > 0) {
            throw new IllegalStateException("Duplicate run already executed in last 24 hours");
        }
    }
}
