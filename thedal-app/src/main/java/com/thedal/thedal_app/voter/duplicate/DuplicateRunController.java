package com.thedal.thedal_app.voter.duplicate;

import com.thedal.thedal_app.response.ThedalResponse;
import com.thedal.thedal_app.response.ThedalSuccess;
import com.thedal.thedal_app.voter.VoterEntity;
import com.thedal.thedal_app.voter.VoterRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/voters/election/{electionId}/duplicates")
@RequiredArgsConstructor
public class DuplicateRunController {
    private final DuplicateRunService service;
    private final VoterDuplicateRunRepository runRepo;
    private final VoterDuplicateGroupRepository groupRepo;
    private final VoterDuplicateMemberRepository memberRepo;
    private final VoterRepo voterRepo;
    private final DuplicateVoterExcelExportService exportService; // retained (could be removed if unused)
    private final DuplicateVoterExportAsyncService exportAsyncService;
    private final DuplicateVoterExportJobRepository exportJobRepo;

    @PostMapping("/run")
    public ResponseEntity<ThedalResponse<VoterDuplicateRun>> runElection(@PathVariable Long electionId,
                                                                         @RequestParam Long accountId,
                                                                         @RequestParam(required = false) Long triggeredBy) {
        VoterDuplicateRun run = service.startElectionRun(accountId, electionId, triggeredBy);
        return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.SUCCESS, run));
    }

    @GetMapping("/status")
    public ResponseEntity<ThedalResponse<VoterDuplicateRun>> status(@PathVariable Long electionId,
                                                                    @RequestParam Long accountId,
                                                                    @RequestParam(required = false) Long runId) {
        VoterDuplicateRun run = runId != null ? runRepo.findById(runId).orElse(null)
                : runRepo.findTopByAccountIdAndElectionIdOrderByStartedAtDesc(accountId, electionId).orElse(null);
        return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.SUCCESS, run));
    }

    @GetMapping
    public ResponseEntity<ThedalResponse<PagedResponse<DuplicateVoterDTO>>> getDuplicates(@PathVariable Long electionId,
                                                                    @RequestParam Long accountId,
                                                                    @RequestParam(required = false) Long runId,
                                                                    @RequestParam(required = false) String partNo,
                                                                    @RequestParam(defaultValue = "0") int page,
                                                                    @RequestParam(defaultValue = "10") int size) {
        VoterDuplicateRun run = runId != null ? runRepo.findById(runId).orElse(null)
                : runRepo.findTopByAccountIdAndElectionIdOrderByStartedAtDesc(accountId, electionId).orElse(null);
        if (run == null) {
            return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.SUCCESS, new PagedResponse<>(List.of(), page, size, 0, 0)));
        }
        
        // Parse optional CSV part filter
        java.util.Set<Integer> partFilter = parsePartNos(partNo);
        
        // Use paginated database query instead of loading everything into memory
        org.springframework.data.domain.Page<DuplicateVoterDTO> dbPage;
        if (partFilter == null || partFilter.isEmpty()) {
            dbPage = memberRepo.findDuplicateVotersByRunId(run.getId(), PageRequest.of(page, size));
        } else {
            dbPage = memberRepo.findDuplicateVotersByRunIdAndParts(run.getId(), partFilter, PageRequest.of(page, size));
        }
        
        PagedResponse<DuplicateVoterDTO> result = new PagedResponse<>(
            dbPage.getContent(), 
            page, 
            size, 
            dbPage.getTotalElements(), 
            dbPage.getTotalPages()
        );
        return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.SUCCESS, result));
    }

    @PostMapping("/export-jobs")
    public ResponseEntity<ThedalResponse<DuplicateVoterExportJob>> createExportJob(@PathVariable Long electionId,
                                                                                   @RequestParam Long accountId,
                                                                                   @RequestParam(required = false) Long runId,
                                                                                   @RequestParam(required = false) String partNo) {
        VoterDuplicateRun run = runId != null ? runRepo.findById(runId).orElse(null)
                : runRepo.findTopByAccountIdAndElectionIdOrderByStartedAtDesc(accountId, electionId).orElse(null);
        if (run == null) {
            return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.SUCCESS, null));
        }
        // Parse CSV; create a single job. If multiple parts, store CSV in job.partNos so async service exports one sheet.
        java.util.Set<Integer> parts = parsePartNos(partNo);
        DuplicateVoterExportJob job = new DuplicateVoterExportJob();
        job.setRunId(run.getId());
        job.setAccountId(accountId);
        job.setElectionId(electionId);
        if (parts == null || parts.isEmpty()) {
            job.setPartNo(null); // all parts
        } else if (parts.size() == 1) {
            job.setPartNo(parts.iterator().next());
        } else {
            String csv = parts.stream().map(String::valueOf).collect(Collectors.joining(","));
            job.setPartNos(csv);
        }
        job = exportJobRepo.save(job);
        exportAsyncService.execute(job.getId());
        return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.SUCCESS, job));
    }

    @GetMapping("/export-jobs/{jobId}")
    public ResponseEntity<ThedalResponse<DuplicateVoterExportJob>> getExportJob(@PathVariable Long electionId,
                                                                                @RequestParam Long accountId,
                                                                                @PathVariable Long jobId) {
        DuplicateVoterExportJob job = exportJobRepo.findById(jobId).orElse(null);
        if (job == null || !job.getElectionId().equals(electionId) || !job.getAccountId().equals(accountId)) {
            return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.SUCCESS, null));
        }
        return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.SUCCESS, job));
    }

    // Helper(s)
    // Keeping simple parsing here to avoid spreading utility classes; accepts CSV like "1,2, 3"
    private static java.util.Set<Integer> parsePartNos(String csv) {
        if (csv == null || csv.trim().isEmpty()) return null;
        java.util.Set<Integer> set = new java.util.LinkedHashSet<>();
        for (String s : csv.split(",")) {
            String t = s.trim();
            if (t.isEmpty()) continue;
            try {
                set.add(Integer.parseInt(t));
            } catch (NumberFormatException ignored) {
                // skip invalid entries silently; could log if needed
            }
        }
        return set;
    }
}
