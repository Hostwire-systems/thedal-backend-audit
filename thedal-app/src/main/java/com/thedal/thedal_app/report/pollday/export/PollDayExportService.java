package com.thedal.thedal_app.report.pollday.export;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.thedal.thedal_app.report.pollday.export.PollDayExportJob.ExportStatus;
import com.thedal.thedal_app.report.pollday.export.dto.ExportJobRequest;
import com.thedal.thedal_app.report.pollday.export.dto.ExportJobResponse;
import com.thedal.thedal_app.report.pollday.export.dto.ExportJobStatusResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PollDayExportService {

    private final PollDayExportJobRepository jobRepository;
    private final PollDayExportAsyncService asyncService;

    /**
     * Create export job and start async processing
     */
    @Transactional
    public ExportJobResponse createExportJob(ExportJobRequest request) {
        log.info("Creating export job for accountId={}, electionId={}, chartType={}, format={}", 
            request.getAccountId(), request.getElectionId(), request.getChartType(), request.getFormat());

        // Create job entity
        PollDayExportJob job = new PollDayExportJob();
        job.setAccountId(request.getAccountId());
        job.setElectionId(request.getElectionId());
        job.setFormat(request.getFormat());
        job.setChartType(request.getChartType());
        job.setStatus(ExportStatus.PENDING);
        job.setSelectedParts(request.getSelectedParts().stream().mapToInt(Integer::intValue).toArray());
        job.setPollingDate(request.getPollingDate());
        job.setFilters(request.getFilters() != null ? request.getFilters() : new ExportFilters());

        job = jobRepository.save(job);
        log.info("Created export job with ID: {}", job.getId());

        // Start async processing
        asyncService.execute(job.getId());

        return mapToResponse(job);
    }

    /**
     * Get job status
     */
    @Transactional(readOnly = true)
    public ExportJobStatusResponse getJobStatus(Long jobId, Long accountId) {
        log.info("Getting job status for jobId={}, accountId={}", jobId, accountId);

        PollDayExportJob job = jobRepository.findByIdAndAccountId(jobId, accountId)
            .orElseThrow(() -> new IllegalArgumentException("Export job not found"));

        return mapToStatusResponse(job);
    }

    /**
     * Map entity to response
     */
    private ExportJobResponse mapToResponse(PollDayExportJob job) {
        return new ExportJobResponse(
            job.getId(),
            job.getStatus(),
            job.getFormat(),
            job.getChartType(),
            job.getCreatedAt()
        );
    }

    /**
     * Map entity to status response
     */
    private ExportJobStatusResponse mapToStatusResponse(PollDayExportJob job) {
        return new ExportJobStatusResponse(
            job.getId(),
            job.getStatus(),
            job.getFormat(),
            job.getChartType(),
            job.getS3Url(),
            job.getRowCount(),
            job.getErrorMessage(),
            job.getCreatedAt(),
            job.getFinishedAt()
        );
    }
}
