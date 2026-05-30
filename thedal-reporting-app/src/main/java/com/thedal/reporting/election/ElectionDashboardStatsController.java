package com.thedal.reporting.election;

import java.time.Duration;
import java.util.Optional;

import com.thedal.reporting.aggregates.RecomputeRateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reporting/api/aggregates/election")
@RequiredArgsConstructor
public class ElectionDashboardStatsController {

    private final ElectionDashboardStatsRepository repository;
    private final ElectionStatsAggregationService aggregationService;
    private final RecomputeRateLimiter rateLimiter;

    @GetMapping("/{accountId}/{electionId}")
    public ResponseEntity<?> getStats(@PathVariable Long accountId, @PathVariable Long electionId) {
        Optional<ElectionDashboardStats> statsOpt = repository.findByAccountIdAndElectionId(accountId, electionId);
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

    @PostMapping("/{accountId}/{electionId}/recompute")
    public ResponseEntity<?> forceRecompute(@PathVariable Long accountId, @PathVariable Long electionId) {
        if (!rateLimiter.allow("stats", accountId, electionId)) {
            return ResponseEntity.status(429).body("Too Many Requests - wait before recompute");
        }
        aggregationService.forceAggregate(accountId, electionId);
        Optional<ElectionDashboardStats> statsOpt = repository.findByAccountIdAndElectionId(accountId, electionId);
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
