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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/reporting/api/aggregates/election/party-polling")
@RequiredArgsConstructor
@Slf4j
public class ElectionPartyPollingController {

    private final ElectionDashboardPartyPollingRepository repository;
    private final ElectionPartyPollingAggregationService aggregationService;
    private final RecomputeRateLimiter rateLimiter;
    private final RequestDetailsService requestDetails;

    @GetMapping("/{electionId}")
    public ResponseEntity<?> getPartyPolling(
            @PathVariable Long electionId,
            @RequestParam(required = false) String partNumber) {
        // Get account ID from JWT token
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }
        
        try {
            // Handle multiple part numbers
            if (partNumber != null && partNumber.contains(",")) {
                String[] parts = partNumber.split(",");
                List<ElectionDashboardPartyPolling> results = new ArrayList<>();
                for (String part : parts) {
                    String trimmedPart = part.trim();
                    if (!trimmedPart.isEmpty()) {
                        repository.findByAccountIdAndElectionIdAndPartNo(accountId, electionId, trimmedPart)
                            .ifPresent(results::add);
                    }
                }
                if (results.isEmpty()) {
                    return ResponseEntity.notFound().build();
                }
                return ResponseEntity.ok(results);
            }
            
            Optional<ElectionDashboardPartyPolling> partyPollingOpt = partNumber != null ?
                repository.findByAccountIdAndElectionIdAndPartNo(accountId, electionId, partNumber) :
                repository.findByAccountIdAndElectionId(accountId, electionId);
                
            if (partyPollingOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            ElectionDashboardPartyPolling pp = partyPollingOpt.get();
            ElectionDashboardPartyPollingResponse response = new ElectionDashboardPartyPollingResponse(
                    pp.getAccountId(),
                    pp.getElectionId(),
                    pp.getPartyCountsJson(),
                    pp.getComputedAt(),
                    pp.getRefreshedAt(),
                    java.time.Duration.between(pp.getRefreshedAt(), java.time.OffsetDateTime.now()).getSeconds()
            );

            return ResponseEntity.ok()
                    .header("ETag", String.valueOf(pp.getRefreshedAt().toEpochSecond()))
                    .header("Cache-Control", "public, max-age=30")
                    .body(response);
        } catch (ThedalException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error retrieving party polling for account {} election {}", accountId, electionId, e);
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/{electionId}/recompute")
    public ResponseEntity<?> forceRecompute(
            @PathVariable Long electionId,
            @RequestParam(required = false) String partNumber) {
        // Get account ID from JWT token
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }
        
        // Validate partNumber is numeric if provided (supports comma-separated)
        if (partNumber != null && !partNumber.trim().isEmpty()) {
            String[] parts = partNumber.split(",");
            for (String part : parts) {
                String trimmedPart = part.trim();
                if (!trimmedPart.isEmpty() && !trimmedPart.matches("\\d+")) {
                    return ResponseEntity.badRequest().body("partNumber must be valid numeric booth number(s)");
                }
            }
        }
        
        try {
            // Apply rate limiting
            String key = "party-polling";
            if (!rateLimiter.allow(key, accountId, electionId)) {
                return ResponseEntity.status(429)
                        .body(new ThedalResponse<>(ThedalError.INVALID_REQUEST, 
                            "Recompute rate limit exceeded. Please wait before retrying."));
            }

            // Handle multiple part numbers
            if (partNumber != null && partNumber.contains(",")) {
                String[] parts = partNumber.split(",");
                for (String part : parts) {
                    String trimmedPart = part.trim();
                    if (!trimmedPart.isEmpty()) {
                        aggregationService.forceAggregate(accountId, electionId, trimmedPart);
                    }
                }
                return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.SUCCESS, 
                    "Recompute triggered for " + parts.length + " part(s)"));
            }

            // Trigger aggregation
            aggregationService.forceAggregate(accountId, electionId, partNumber);

            // Return updated data
            Optional<ElectionDashboardPartyPolling> updated = partNumber != null ?
                repository.findByAccountIdAndElectionIdAndPartNo(accountId, electionId, partNumber) :
                repository.findByAccountIdAndElectionId(accountId, electionId);
                
            if (updated.isEmpty()) {
                throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
            }

            ElectionDashboardPartyPolling polling = updated.get();
            ElectionDashboardPartyPollingResponse response = new ElectionDashboardPartyPollingResponse(
                    polling.getAccountId(),
                    polling.getElectionId(),
                    polling.getPartyCountsJson(),
                    polling.getComputedAt(),
                    polling.getRefreshedAt(),
                    0L // Fresh data
            );

            return ResponseEntity.ok(response);
        } catch (ThedalException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error recomputing party polling for account {} election {}", accountId, electionId, e);
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    record ElectionDashboardPartyPollingResponse(
            Long accountId,
            Long electionId,
            String partyCountsJson,
            java.time.OffsetDateTime computedAt,
            java.time.OffsetDateTime refreshedAt,
            Long freshnessSeconds
    ) {}
}
