package com.thedal.thedal_app.report.aggregates;

import com.thedal.thedal_app.general.RequestDetailsService;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Map;

@RestController
@RequestMapping("/reporting/api/aggregates/cadre")
@RequiredArgsConstructor
@Slf4j
public class CadreDashboardController {

    private final CadreDashboardStatsRepository repository;
    private final CadreDashboardAggregationService aggregationService;
    private final RecomputeRateLimiter rateLimiter;
    private final RequestDetailsService requestDetailsService;

    @GetMapping("/{electionId}")
    public ResponseEntity<?> getStats(@PathVariable Long electionId) {
        // Get account ID from JWT token
        Long accountId = requestDetailsService.getCurrentAccountId();
        if (accountId == null) {
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        var statsOpt = repository.findByAccountIdAndElectionId(accountId, electionId);
        if (statsOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        CadreDashboardStats s = statsOpt.get();
        long secondsOld = java.time.Duration.between(s.getRefreshedAt(), OffsetDateTime.now()).getSeconds();
        String etag = String.valueOf(s.getRefreshedAt().toEpochSecond());
        
        return ResponseEntity.ok()
                .eTag(etag)
                .header("Cache-Control", "public, max-age=30")
                .body(new CadreDashboardStatsResponse(s, secondsOld));
    }

    @PostMapping("/{electionId}/recompute")
    public ResponseEntity<?> recomputeStats(@PathVariable Long electionId) {
        // Get account ID from JWT token
        Long accountId = requestDetailsService.getCurrentAccountId();
        if (accountId == null) {
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        // Rate limiting
        if (!rateLimiter.allow("cadre-dashboard", accountId, electionId)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Rate limit exceeded. Please wait 30 seconds between recompute requests."));
        }

        try {
            CadreDashboardStats stats = aggregationService.aggregateOne(accountId, electionId);
            long secondsOld = java.time.Duration.between(stats.getRefreshedAt(), OffsetDateTime.now()).getSeconds();
            return ResponseEntity.ok().body(new CadreDashboardStatsResponse(stats, secondsOld));
        } catch (Exception e) {
            log.error("Error recomputing cadre stats for accountId={}, electionId={}", accountId, electionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error during recomputation"));
        }
    }

    public record CadreDashboardStatsResponse(
            Long accountId,
            Long electionId,
            Integer totalCadres,
            Integer cadresLogged,
            Integer cadresNotLogged,
            Integer boothsAssigned,
            Integer totalMobileUpdated,
            Integer totalDobUpdated,
            Integer totalPartyUpdated,
            Integer totalCasteUpdated,
            Integer totalReligionUpdated,
            Integer totalLanguageUpdated,
            String top10Cadres,
            String least10Cadres,
            String computedAt,
            String refreshedAt,
            long freshnessSeconds
    ) {
        CadreDashboardStatsResponse(CadreDashboardStats s, long freshnessSeconds) {
            this(
                    s.getAccountId(),
                    s.getElectionId(),
                    s.getTotalCadres(),
                    s.getCadresLogged(),
                    s.getCadresNotLogged(),
                    s.getBoothsAssigned(),
                    s.getTotalMobileUpdated(),
                    s.getTotalDobUpdated(),
                    s.getTotalPartyUpdated(),
                    s.getTotalCasteUpdated(),
                    s.getTotalReligionUpdated(),
                    s.getTotalLanguageUpdated(),
                    s.getTop10Cadres(),
                    s.getLeast10Cadres(),
                    s.getComputedAt().toString(),
                    s.getRefreshedAt().toString(),
                    freshnessSeconds
            );
        }
    }
}
