package com.thedal.thedal_app.report.aggregates;

import com.thedal.thedal_app.general.RequestDetailsService;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;
import com.thedal.thedal_app.response.ThedalResponse;
import com.thedal.thedal_app.response.ThedalSuccess;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * REST controller for managing async aggregation jobs.
 * Provides APIs to check job status, list jobs, and cancel running jobs.
 */
@RestController
@RequestMapping("/reporting/api/aggregates/jobs")
@RequiredArgsConstructor
@Slf4j
public class AggregationJobController {
    
    private final AggregationJobRepository jobRepository;
    private final AsyncAggregationService asyncService;
    private final RequestDetailsService requestDetailsService;
    
    /**
     * Get status of a specific job
     * GET /reporting/api/aggregates/jobs/{jobId}/status
     */
    @GetMapping("/{jobId}/status")
    public ResponseEntity<ThedalResponse<JobStatusResponse>> getJobStatus(@PathVariable String jobId) {
        Optional<AggregationJob> jobOpt = jobRepository.findByJobId(jobId);
        
        if (jobOpt.isEmpty()) {
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.NOT_FOUND);
        }
        
        AggregationJob job = jobOpt.get();
        JobStatusResponse response = JobStatusResponse.fromEntity(job);
        
        return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.SUCCESS, response));
    }
    
    /**
     * List recent jobs for an election
     * GET /reporting/api/aggregates/jobs?electionId={electionId}&limit={limit}
     */
    @GetMapping
    public ResponseEntity<ThedalResponse<List<JobStatusResponse>>> listJobs(
            @RequestParam Long electionId,
            @RequestParam(defaultValue = "10") int limit) {
        
        Long accountId = requestDetailsService.getCurrentAccountId();
        List<AggregationJob> jobs = jobRepository.findRecentJobs(accountId, electionId, 
            org.springframework.data.domain.PageRequest.of(0, limit));
        
        List<JobStatusResponse> responses = jobs.stream()
            .map(JobStatusResponse::fromEntity)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.SUCCESS, responses));
    }
    
    /**
     * Cancel a running job
     * DELETE /reporting/api/aggregates/jobs/{jobId}
     */
    @DeleteMapping("/{jobId}")
    public ResponseEntity<ThedalResponse<CancelJobResponse>> cancelJob(@PathVariable String jobId) {
        boolean cancelled = asyncService.cancelJob(jobId);
        
        if (!cancelled) {
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.BAD_REQUEST);
        }
        
        CancelJobResponse response = new CancelJobResponse(jobId, "Job cancelled successfully");
        return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.SUCCESS, response));
    }
    
    /**
     * Response DTO for job status
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JobStatusResponse {
        private String jobId;
        private Long accountId;
        private Long electionId;
        private String jobType;
        private String status;
        private String partNumber;
        private Integer totalParts;
        private Integer completedParts;
        private Double progressPercent;
        private LocalDateTime startedAt;
        private LocalDateTime completedAt;
        private Long elapsedSeconds;
        private String errorMessage;
        
        public static JobStatusResponse fromEntity(AggregationJob job) {
            JobStatusResponse response = new JobStatusResponse();
            response.setJobId(job.getJobId());
            response.setAccountId(job.getAccountId());
            response.setElectionId(job.getElectionId());
            response.setJobType(job.getJobType());
            response.setStatus(job.getStatus() != null ? job.getStatus().name() : null);
            response.setPartNumber(job.getPartNumber());
            response.setTotalParts(job.getTotalParts());
            response.setCompletedParts(job.getCompletedParts());
            
            // Calculate progress percentage
            if (job.getTotalParts() != null && job.getTotalParts() > 0) {
                double percent = (job.getCompletedParts() * 100.0) / job.getTotalParts();
                response.setProgressPercent(Math.round(percent * 100.0) / 100.0); // Round to 2 decimals
            } else {
                response.setProgressPercent(0.0);
            }
            
            response.setStartedAt(job.getStartedAt());
            response.setCompletedAt(job.getCompletedAt());
            response.setElapsedSeconds(job.getElapsedSeconds());
            response.setErrorMessage(job.getErrorMessage());
            
            return response;
        }
    }
    
    /**
     * Response DTO for cancel operation
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CancelJobResponse {
        private String jobId;
        private String message;
    }
}
