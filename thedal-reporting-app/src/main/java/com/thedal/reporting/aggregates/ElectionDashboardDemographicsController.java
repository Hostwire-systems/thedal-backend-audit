package com.thedal.reporting.aggregates;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Placeholder controller for demographics reporting
 */
@RestController
@RequestMapping("/api/aggregates/election/demographics")
@RequiredArgsConstructor
@Slf4j
public class ElectionDashboardDemographicsController {

    @GetMapping("/{accountId}/{electionId}")
    public ResponseEntity<?> get(@PathVariable Long accountId, @PathVariable Long electionId) {
        log.info("Demographics requested for account={} election={}", accountId, electionId);
        
        Map<String, Object> response = Map.of(
            "accountId", accountId,
            "electionId", electionId,
            "demographicsJson", "[]",
            "computedAt", OffsetDateTime.now().toString(),
            "refreshedAt", OffsetDateTime.now().toString(),
            "freshnessSeconds", 0L,
            "message", "Demographics data will be available when database is fully connected"
        );
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{accountId}/{electionId}/recompute")
    public ResponseEntity<?> recompute(@PathVariable Long accountId, @PathVariable Long electionId) {
        log.info("Demographics recompute requested for account={} election={}", accountId, electionId);
        return ResponseEntity.accepted().body(Map.of("message", "Recompute accepted - not implemented yet"));
    }
}
