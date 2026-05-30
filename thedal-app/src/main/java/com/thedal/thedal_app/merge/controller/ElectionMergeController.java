package com.thedal.thedal_app.merge.controller;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.thedal.thedal_app.general.RequestDetailsService;
import com.thedal.thedal_app.merge.dto.MergeRequestDTO;
import com.thedal.thedal_app.merge.dto.MergeJobDtos.MergeJobDetail;
import com.thedal.thedal_app.merge.dto.MergeJobDtos.MergeJobSummary;
import com.thedal.thedal_app.merge.service.ElectionDataMergeService;
import com.thedal.thedal_app.response.ThedalResponse;
import com.thedal.thedal_app.response.ThedalSuccess;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/elections/{targetElectionId}/merge")
@RequiredArgsConstructor
public class ElectionMergeController {

    private final ElectionDataMergeService mergeService;
    private final RequestDetailsService requestDetails;

    @PostMapping("/dry-run")
    public ResponseEntity<ThedalResponse<Object>> dryRun(@PathVariable Long targetElectionId, @RequestBody MergeRequestDTO request) {
        UUID jobId = mergeService.enqueueDryRun(requestDetails.getCurrentAccountId(), requestDetails.getCurrentUserId(), targetElectionId, request);
        mergeService.startAsyncDryRun(jobId);
        ThedalResponse<Object> resp = new ThedalResponse<>();
        resp.setResponse(ThedalSuccess.SUCCESS, Map.of("jobId", jobId));
        return ResponseEntity.accepted().body(resp);
    }

    @PostMapping
    public ResponseEntity<ThedalResponse<Object>> enqueue(@PathVariable Long targetElectionId, @RequestBody MergeRequestDTO request) {
        if (request.isDryRun()) {
            throw new ThedalException(ThedalError.INVALID_REQUEST, HttpStatus.BAD_REQUEST, "Use /dry-run endpoint for dry runs");
        }
    UUID jobId = mergeService.enqueueMerge(requestDetails.getCurrentAccountId(), requestDetails.getCurrentUserId(), targetElectionId, request);
    // Trigger async processing after enqueue transaction commits
    mergeService.startAsyncProcessing(jobId);
        ThedalResponse<Object> resp = new ThedalResponse<>();
    resp.setResponse(ThedalSuccess.SUCCESS, Map.of("jobId", jobId));
        return ResponseEntity.accepted().body(resp);
    }

    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<ThedalResponse<Object>> jobStatus(@PathVariable Long targetElectionId, @PathVariable UUID jobId) {
        var job = mergeService.getJob(jobId);
        if (!job.getTargetElectionId().equals(targetElectionId)) {
            throw new ThedalException(ThedalError.INVALID_REQUEST, HttpStatus.BAD_REQUEST, "Job does not belong to election");
        }
        ThedalResponse<Object> resp = new ThedalResponse<>();
    resp.setResponse(ThedalSuccess.SUCCESS, job);
        return ResponseEntity.ok(resp);
    }

    // List jobs with pagination (new)
    @GetMapping("/jobs")
    public ResponseEntity<ThedalResponse<Object>> listJobs(@PathVariable Long targetElectionId, Pageable pageable) {
        var page = mergeService.listJobs(targetElectionId, pageable).map(MergeJobSummary::from);
        var body = Map.of(
                "items", page.getContent(),
                "page", Map.of(
                        "number", page.getNumber(),
                        "size", page.getSize(),
                        "totalElements", page.getTotalElements(),
                        "totalPages", page.getTotalPages()
                )
        );
        ThedalResponse<Object> resp = new ThedalResponse<>();
        resp.setResponse(ThedalSuccess.SUCCESS, body);
        return ResponseEntity.ok(resp);
    }

    // List ALL active jobs (PENDING/RUNNING) - for debugging
    @GetMapping("/jobs/active")
    public ResponseEntity<ThedalResponse<Object>> listActiveJobs(@PathVariable Long targetElectionId) {
        var jobs = mergeService.listActiveJobs(targetElectionId).stream()
                .map(MergeJobSummary::from)
                .collect(Collectors.toList());
        ThedalResponse<Object> resp = new ThedalResponse<>();
        resp.setResponse(ThedalSuccess.SUCCESS, jobs);
        return ResponseEntity.ok(resp);
    }

    // Job detail with fields (new)
    @GetMapping("/jobs/{jobId}/detail")
    public ResponseEntity<ThedalResponse<Object>> jobDetail(@PathVariable Long targetElectionId, @PathVariable UUID jobId) {
        var jobOpt = mergeService.getJobDetail(jobId, targetElectionId);
        var job = jobOpt.orElseThrow(() -> new ThedalException(ThedalError.JOB_NOT_FOUND, HttpStatus.NOT_FOUND, "Merge job not found"));
        var detail = MergeJobDetail.from(job, job.getFields().stream().map(Enum::name).collect(Collectors.toList()));
        ThedalResponse<Object> resp = new ThedalResponse<>();
        resp.setResponse(ThedalSuccess.SUCCESS, detail);
        return ResponseEntity.ok(resp);
    }

    // Force cancel a stuck job
    @PostMapping("/jobs/{jobId}/cancel")
    public ResponseEntity<ThedalResponse<Object>> cancelJob(@PathVariable Long targetElectionId, @PathVariable UUID jobId) {
        mergeService.cancelJob(jobId, targetElectionId);
        ThedalResponse<Object> resp = new ThedalResponse<>();
        resp.setResponse(ThedalSuccess.SUCCESS, Map.of("message", "Job cancelled successfully"));
        return ResponseEntity.ok(resp);
    }

    // Force-fail a stuck job (new)
    @PostMapping("/jobs/{jobId}/force-fail")
    public ResponseEntity<ThedalResponse<Object>> forceFail(@PathVariable Long targetElectionId, @PathVariable UUID jobId, @RequestParam(required = false) String reason) {
        mergeService.forceFailJob(jobId, targetElectionId, reason);
        var updated = mergeService.getJob(jobId);
        ThedalResponse<Object> resp = new ThedalResponse<>();
        resp.setResponse(ThedalSuccess.SUCCESS, MergeJobSummary.from(updated));
        return ResponseEntity.ok(resp);
    }
}
