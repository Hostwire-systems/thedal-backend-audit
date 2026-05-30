package com.thedal.reporting.cadre;

import com.thedal.reporting.aggregates.RecomputeRateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/reporting/cadre-dashboard")
@RequiredArgsConstructor
public class CadreDashboardStatsController {

    private final CadreDashboardStatsRepository repository;
    private final CadreDashboardAggregationService aggregationService;
    private final RecomputeRateLimiter rateLimiter;

    @GetMapping
    public ResponseEntity<CadreDashboardStats> get(@RequestParam Long accountId, @RequestParam Long electionId) {
        CadreDashboardStats stats = repository.findByAccountIdAndElectionId(accountId, electionId)
                .orElse(null);
        if (stats == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        String etag = Long.toString(stats.getRefreshedAt().toInstant().toEpochMilli());
        CacheControl cc = CacheControl.maxAge(30, TimeUnit.SECONDS).cachePublic();
        return ResponseEntity.ok()
                .cacheControl(cc)
                .eTag(etag)
                .body(stats);
    }

    @PostMapping("/recompute")
    public ResponseEntity<CadreDashboardStats> recompute(@RequestParam Long accountId, @RequestParam Long electionId) {
        if (!rateLimiter.allow("cadre-dashboard", accountId, electionId)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }
        CadreDashboardStats stats = aggregationService.recompute(accountId, electionId);
        String etag = Long.toString(stats.getRefreshedAt().toInstant().toEpochMilli());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofSeconds(30)))
                .eTag(etag)
                .body(stats);
    }
}
