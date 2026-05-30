package com.thedal.thedal_app.report.aggregates;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Simple in-memory rate limiter for recompute endpoints.
 * Keyed by controller-slice + accountId + electionId.
 * Not clustered; sufficient for single-node or low concurrency Phase 1.
 */
@Component
public class RecomputeRateLimiter {

    private final Map<String, Instant> lastInvocation = new ConcurrentHashMap<>();

    // Minimum seconds between recomputes for same key
    private final long minIntervalSeconds;

    public RecomputeRateLimiter(@Value("${REPORTING_RECOMPUTE_MIN_INTERVAL_SEC:30}") long minIntervalSeconds) {
        this.minIntervalSeconds = minIntervalSeconds;
    }

    public boolean allow(String slice, Long accountId, Long electionId) {
        String key = slice + ":" + accountId + ":" + electionId;
        Instant now = Instant.now();
        Instant prev = lastInvocation.get(key);
        if (prev == null || now.isAfter(prev.plusSeconds(minIntervalSeconds))) {
            lastInvocation.put(key, now);
            return true;
        }
        return false;
    }
}
