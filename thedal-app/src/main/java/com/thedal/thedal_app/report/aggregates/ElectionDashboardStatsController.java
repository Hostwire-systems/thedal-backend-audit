package com.thedal.thedal_app.report.aggregates;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.thedal.thedal_app.general.RequestDetailsService;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reporting/api/aggregates/election")
@RequiredArgsConstructor
public class ElectionDashboardStatsController {

    private final ElectionDashboardStatsRepository repository;
    private final ElectionStatsAggregationService aggregationService;
    private final AsyncAggregationService asyncAggregationService;
    private final RecomputeRateLimiter rateLimiter;
    private final RequestDetailsService requestDetails;

    @GetMapping("/{electionId}")
    public ResponseEntity<?> getStats(
            @PathVariable Long electionId,
            @RequestParam(required = false) String partNumber,
            @RequestParam(required = false, defaultValue = "false") boolean breakdown) {
        // Get account ID from JWT token
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }
        
        // Handle breakdown mode - return detailed part-level stats
        if (breakdown && partNumber != null && !partNumber.trim().isEmpty()) {
            String[] parts = partNumber.split(",");
            List<ElectionDashboardStatsResponse> results = new ArrayList<>();
            for (String part : parts) {
                String trimmedPart = part.trim();
                if (!trimmedPart.isEmpty()) {
                    Optional<ElectionDashboardStats> statsOpt = repository.findByAccountIdAndElectionIdAndPartNo(accountId, electionId, trimmedPart);
                    if (statsOpt.isPresent()) {
                        ElectionDashboardStats s = statsOpt.get();
                        long secondsOld = Duration.between(s.getRefreshedAt(), java.time.OffsetDateTime.now()).getSeconds();
                        results.add(new ElectionDashboardStatsResponse(s, secondsOld));
                    }
                }
            }
            if (results.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(results);
        }
        
        // Handle multiple part numbers (must check BEFORE single part)
        if (partNumber != null && partNumber.contains(",")) {
            String[] parts = partNumber.split(",");
            List<ElectionDashboardStats> results = new ArrayList<>();
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
        
        // Single part or no part
        Optional<ElectionDashboardStats> statsOpt = partNumber != null ?
            repository.findByAccountIdAndElectionIdAndPartNo(accountId, electionId, partNumber) :
            repository.findByAccountIdAndElectionId(accountId, electionId);
            
        if (statsOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        ElectionDashboardStats s = statsOpt.get();
        long secondsOld = Duration.between(s.getRefreshedAt(), java.time.OffsetDateTime.now()).getSeconds();
        String etag = String.valueOf(s.getRefreshedAt().toEpochSecond());
        return ResponseEntity.ok()
                .eTag(etag)
                .header("Cache-Control", "public, max-age=30")
                .body(new ElectionDashboardStatsResponse(s, secondsOld));
    }

    @PostMapping("/{electionId}/recompute")
    public ResponseEntity<?> forceRecompute(
            @PathVariable Long electionId,
            @RequestParam(required = false) String partNumber,
            @RequestParam(required = false, defaultValue = "false") boolean async) {
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
        
        if (!rateLimiter.allow("stats", accountId, electionId)) {
            return ResponseEntity.status(429).body("Too Many Requests - wait before recompute");
        }
        
        // Use async processing for full election or large batch
        boolean shouldAsync = async || partNumber == null || partNumber.split(",").length > 5;
        
        if (shouldAsync) {
            // Start async job and return immediately with jobId
            String jobId = asyncAggregationService.startAsyncRecompute(accountId, electionId, 
                partNumber != null && !partNumber.contains(",") ? partNumber : null);
            return ResponseEntity.accepted().body(Map.of(
                "jobId", jobId,
                "message", "Async recompute started. Use GET /reporting/api/aggregates/jobs/" + jobId + "/status to check progress"
            ));
        }
        
        // Handle small synchronous requests (backward compatibility)
        // Handle multiple part numbers
        if (partNumber != null && partNumber.contains(",")) {
            String[] parts = partNumber.split(",");
            for (String part : parts) {
                String trimmedPart = part.trim();
                if (!trimmedPart.isEmpty()) {
                    aggregationService.forceAggregate(accountId, electionId, trimmedPart);
                }
            }
            return ResponseEntity.ok(Map.of("message", "Recompute triggered for " + parts.length + " part(s)"));
        }
        
        aggregationService.forceAggregate(accountId, electionId, partNumber);
        
        Optional<ElectionDashboardStats> statsOpt = partNumber != null ?
            repository.findByAccountIdAndElectionIdAndPartNo(accountId, electionId, partNumber) :
            repository.findByAccountIdAndElectionId(accountId, electionId);
            
        if (statsOpt.isEmpty()) {
            return ResponseEntity.accepted().build();
        }
        ElectionDashboardStats s = statsOpt.get();
        long secondsOld = java.time.Duration.between(s.getRefreshedAt(), java.time.OffsetDateTime.now()).getSeconds();
        return ResponseEntity.ok(new ElectionDashboardStatsResponse(s, secondsOld));
    }

    record ElectionDashboardStatsResponse(
            Long accountId,
            Long electionId,
            int totalBooth,
            int totalVoters,
            int totalFamily,
            int distinctPincodeCount,
            int distinctMobileCount,
            int male,
            int female,
            int transgender,
            int age18To30,
            int age30To40,
            int age40To50,
            int age50To60,
            int age60To70,
            int ageGreaterThan70,
            int firstTimeVoters,
            int seniorCitizens,
            int superSeniors,
            int dateOfBirth,
            int starVoters,
            int religionCount,
            int casteCount,
            int totalMobileCount,
            int maleMobileCount,
            int femaleMobileCount,
            int transgenderMobileCount,
            int maleDateOfBirthCount,
            int femaleDateOfBirthCount,
            int transgenderDateOfBirthCount,
            int totalSchool,
            int crossBoothFamily,
            int oneVoterFamily,
            int casteCategoryCount,
            int subCasteCount,
            int languageCount,
            int partyAffiliationCount,
            int schemesCount,
            String computedAt,
            String refreshedAt,
            long freshnessSeconds
    ) {
        ElectionDashboardStatsResponse(ElectionDashboardStats s, long freshnessSeconds) {
            this(
                    s.getAccountId(),
                    s.getElectionId(),
                    s.getTotalBooth(),
                    s.getTotalVoters(),
                    s.getTotalFamily(),
                    s.getDistinctPincodeCount(),
                    s.getDistinctMobileCount(),
                    s.getMale(),
                    s.getFemale(),
                    s.getTransgender(),
                    s.getAge18To30(),
                    s.getAge30To40(),
                    s.getAge40To50(),
                    s.getAge50To60(),
                    s.getAge60To70(),
                    s.getAgeGreaterThan70(),
                    s.getFirstTimeVoters(),
                    s.getSeniorCitizens(),
                    s.getSuperSeniors(),
                    s.getDateOfBirth(),
                    s.getStarVoters(),
                    s.getReligionCount(),
                    s.getCasteCount(),
                    s.getTotalMobileCount(),
                    s.getMaleMobileCount(),
                    s.getFemaleMobileCount(),
                    s.getTransgenderMobileCount(),
                    s.getMaleDateOfBirthCount(),
                    s.getFemaleDateOfBirthCount(),
                    s.getTransgenderDateOfBirthCount(),
                    s.getTotalSchool(),
                    s.getCrossBoothFamily(),
                    s.getOneVoterFamily(),
                    s.getCasteCategoryCount(),
                    s.getSubCasteCount(),
                    s.getLanguageCount(),
                    s.getPartyAffiliationCount(),
                    s.getSchemesCount(),
                    s.getComputedAt().toString(),
                    s.getRefreshedAt().toString(),
                    freshnessSeconds
            );
        }
    }
}
