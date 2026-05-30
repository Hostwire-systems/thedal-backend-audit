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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/reporting/api/aggregates/election/demographics")
@RequiredArgsConstructor
@Slf4j
public class ElectionDemographicsController {

    private final ElectionDashboardDemographicsRepository repository;
    private final ElectionDemographicsAggregationService aggregationService;
    private final RecomputeRateLimiter rateLimiter;
    private final RequestDetailsService requestDetails;

    @GetMapping("/{electionId}")
    public ResponseEntity<?> getDemographics(
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
                List<ElectionDashboardDemographics> results = new ArrayList<>();
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
            
            Optional<ElectionDashboardDemographics> demographicsOpt = partNumber != null ?
                repository.findByAccountIdAndElectionIdAndPartNo(accountId, electionId, partNumber) :
                repository.findByAccountIdAndElectionId(accountId, electionId);
                
            if (demographicsOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            ElectionDashboardDemographics demo = demographicsOpt.get();
            ElectionDashboardDemographicsResponse response = new ElectionDashboardDemographicsResponse(
                    demo.getAccountId(),
                    demo.getElectionId(),
                    demo.getCasteCategoryJson(),
                    demo.getCasteJson(),
                    demo.getSubCasteJson(),
                    demo.getReligionJson(),
                    demo.getLanguageJson(),
                    demo.getAvailabilityJson(),
                    demo.getSchemesJson(),
                    demo.getRelationJson(),
                    demo.getComputedAt(),
                    demo.getRefreshedAt(),
                    java.time.Duration.between(demo.getRefreshedAt(), java.time.OffsetDateTime.now()).getSeconds()
            );

            return ResponseEntity.ok()
                    .header("ETag", String.valueOf(demo.getRefreshedAt().toEpochSecond()))
                    .header("Cache-Control", "public, max-age=30")
                    .body(response);
        } catch (ThedalException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error retrieving demographics for account {} election {}", accountId, electionId, e);
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
            String key = "demographics";
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
            Optional<ElectionDashboardDemographics> updated = partNumber != null ?
                repository.findByAccountIdAndElectionIdAndPartNo(accountId, electionId, partNumber) :
                repository.findByAccountIdAndElectionId(accountId, electionId);
                
            if (updated.isEmpty()) {
                throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
            }

            ElectionDashboardDemographics demo = updated.get();
            ElectionDashboardDemographicsResponse response = new ElectionDashboardDemographicsResponse(
                    demo.getAccountId(),
                    demo.getElectionId(),
                    demo.getCasteCategoryJson(),
                    demo.getCasteJson(),
                    demo.getSubCasteJson(),
                    demo.getReligionJson(),
                    demo.getLanguageJson(),
                    demo.getAvailabilityJson(),
                    demo.getSchemesJson(),
                    demo.getRelationJson(),
                    demo.getComputedAt(),
                    demo.getRefreshedAt(),
                    0L // Fresh data
            );

            return ResponseEntity.ok(response);
        } catch (ThedalException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error recomputing demographics for account {} election {}", accountId, electionId, e);
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    record ElectionDashboardDemographicsResponse(
            Long accountId,
            Long electionId,
            String casteCategoryJson,
            String casteJson,
            String subCasteJson,
            String religionJson,
            String languageJson,
            String availabilityJson,
            String schemesJson,
            String relationJson,
            java.time.OffsetDateTime computedAt,
            java.time.OffsetDateTime refreshedAt,
            Long freshnessSeconds
    ) {}
}
