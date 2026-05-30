package com.thedal.thedal_app.report.aggregates;

import com.thedal.thedal_app.general.RequestDetailsService;
import com.thedal.thedal_app.response.ThedalResponse;
import com.thedal.thedal_app.response.ThedalSuccess;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/reporting/api/aggregates/election/contact-status")
@RequiredArgsConstructor
@Slf4j
public class ElectionContactStatusController {

    private final ElectionDashboardContactStatusRepository repository;
    private final ElectionContactStatusAggregationService aggregationService;
    private final RecomputeRateLimiter rateLimiter;
    private final RequestDetailsService requestDetails;

    @GetMapping("/{electionId}")
    public ResponseEntity<?> getContactStatus(@PathVariable Long electionId) {
        // Get account ID from JWT token
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }
        
        try {
            Optional<ElectionDashboardContactStatus> contactStatus = repository.findByAccountIdAndElectionId(accountId, electionId);
            if (contactStatus.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            ElectionDashboardContactStatus status = contactStatus.get();
            ElectionDashboardContactStatusResponse response = new ElectionDashboardContactStatusResponse(
                    status.getAccountId(),
                    status.getElectionId(),
                    status.getContactStatusJson(),
                    status.getComputedAt(),
                    status.getRefreshedAt(),
                    java.time.Duration.between(status.getRefreshedAt(), java.time.OffsetDateTime.now()).getSeconds()
            );

            return ResponseEntity.ok()
                    .header("ETag", String.valueOf(status.getRefreshedAt().toEpochSecond()))
                    .header("Cache-Control", "public, max-age=30")
                    .body(response);
        } catch (ThedalException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error retrieving contact status for account {} election {}", accountId, electionId, e);
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/{electionId}/recompute")
    public ResponseEntity<?> forceRecompute(@PathVariable Long electionId) {
        // Get account ID from JWT token
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }
        
        try {
            // Apply rate limiting
            String key = "contact-status";
            if (!rateLimiter.allow(key, accountId, electionId)) {
                return ResponseEntity.status(429)
                        .body(new ThedalResponse<>(ThedalError.INVALID_REQUEST, 
                            "Recompute rate limit exceeded. Please wait before retrying."));
            }

            // Trigger aggregation
            aggregationService.forceAggregate(accountId, electionId);

            // Return updated data
            Optional<ElectionDashboardContactStatus> updated = repository.findByAccountIdAndElectionId(accountId, electionId);
            if (updated.isEmpty()) {
                throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
            }

            ElectionDashboardContactStatus status = updated.get();
            ElectionDashboardContactStatusResponse response = new ElectionDashboardContactStatusResponse(
                    status.getAccountId(),
                    status.getElectionId(),
                    status.getContactStatusJson(),
                    status.getComputedAt(),
                    status.getRefreshedAt(),
                    0L // Fresh data
            );

            return ResponseEntity.ok(response);
        } catch (ThedalException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error recomputing contact status for account {} election {}", accountId, electionId, e);
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    record ElectionDashboardContactStatusResponse(
            Long accountId,
            Long electionId,
            String contactStatusJson,
            java.time.OffsetDateTime computedAt,
            java.time.OffsetDateTime refreshedAt,
            Long freshnessSeconds
    ) {}
}
