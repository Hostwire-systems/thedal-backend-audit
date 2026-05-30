package com.thedal.thedal_app.report.pollday.export;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.thedal.thedal_app.general.RequestDetailsService;
import com.thedal.thedal_app.report.pollday.dto.ApiResponse;
import com.thedal.thedal_app.report.pollday.export.dto.ExportJobRequest;
import com.thedal.thedal_app.report.pollday.export.dto.ExportJobResponse;
import com.thedal.thedal_app.report.pollday.export.dto.ExportJobStatusResponse;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/poll-day/chart/export")
@RequiredArgsConstructor
@Slf4j
public class PollDayExportController {

    private final PollDayExportService exportService;
    private final RequestDetailsService requestDetails;

    /**
     * Initialize export job
     * POST /api/v1/poll-day/chart/export
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ExportJobResponse>> createExportJob(
            @Valid @RequestBody ExportJobRequest request) {
        try {
            // Get account ID from JWT token
            Long accountId = requestDetails.getCurrentAccountId();
            if (accountId == null) {
                throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
            }

            // Set account ID from JWT token
            request.setAccountId(accountId);

            log.info("Creating export job for accountId={}, electionId={}, chartType={}, format={}", 
                accountId, request.getElectionId(), request.getChartType(), request.getFormat());

            ExportJobResponse response = exportService.createExportJob(request);

            return ResponseEntity.ok(
                ApiResponse.success("Export job created successfully", response)
            );

        } catch (IllegalArgumentException e) {
            log.warn("Invalid export request: {}", e.getMessage());
            return ResponseEntity
                .badRequest()
                .body(ApiResponse.error(e.getMessage()));

        } catch (ThedalException e) {
            log.error("ThedalException creating export job", e);
            throw e;

        } catch (Exception e) {
            log.error("Error creating export job", e);
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Check export job status
     * GET /api/v1/poll-day/chart/export/{jobId}/status
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<ExportJobStatusResponse>> getJobStatus(
            @RequestParam Long jobId) {
        try {
            // Get account ID from JWT token
            Long accountId = requestDetails.getCurrentAccountId();
            if (accountId == null) {
                throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
            }

            log.info("Getting job status for jobId={}, accountId={}", jobId, accountId);

            ExportJobStatusResponse response = exportService.getJobStatus(jobId, accountId);

            return ResponseEntity.ok(
                ApiResponse.success("Export job status retrieved", response)
            );

        } catch (IllegalArgumentException e) {
            log.warn("Export job not found: jobId={}", jobId);
            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Export job not found"));

        } catch (ThedalException e) {
            log.error("ThedalException getting job status", e);
            throw e;

        } catch (Exception e) {
            log.error("Error retrieving job status for jobId={}", jobId, e);
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
