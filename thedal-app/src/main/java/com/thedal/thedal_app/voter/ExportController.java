package com.thedal.thedal_app.voter;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.thedal.thedal_app.general.RequestDetailsService;
import com.thedal.thedal_app.response.ThedalResponse;
import com.thedal.thedal_app.response.ThedalSuccess;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;
import com.thedal.thedal_app.voter.dto.ExportJobsResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/exports")
@Tag(name = "Export Management")
public class ExportController {

    @Autowired
    private ExportService exportService;

    @Autowired
    private RequestDetailsService requestDetails;

    @Operation(summary = "List All Export Jobs",
            description = "Retrieve voter, survey, poll day, and volunteer export jobs for an election, filtered by type (VOTER/SURVEY/POLL_DAY/VOLUNTEER/CADRE), status and date range (default: past 30 days).",
            tags = {"Export Management"})
    @GetMapping("/{electionId}")
    public ResponseEntity<ThedalResponse<ExportJobsResponse>> getAllExportJobs(
            @PathVariable("electionId") Long electionId,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }
        LocalDateTime defaultStartDate = startDate != null ? startDate : LocalDateTime.now().minusDays(30);
        LocalDateTime defaultEndDate = endDate != null ? endDate : LocalDateTime.now();

        ExportJobsResponse response;
        if (page != null && size != null) {
            Page<ExportJobDTO> pageResult = exportService.getAllExportJobsPaginated(
                    accountId, electionId, type, status, defaultStartDate, defaultEndDate,
                    PageRequest.of(page, size, Sort.by("timeStarted").descending()));
            response = new ExportJobsResponse(pageResult.getContent(), pageResult.getTotalElements());
        } else {
            response = exportService.getAllExportJobs(accountId, electionId, type, status, defaultStartDate, defaultEndDate);
        }

        return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.EXPORT_JOBS_FETCHED, response));
    }
}