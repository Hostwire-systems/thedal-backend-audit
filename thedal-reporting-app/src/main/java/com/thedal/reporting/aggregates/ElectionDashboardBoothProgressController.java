package com.thedal.reporting.aggregates;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Placeholder controller for booth progress reporting
 * Returns sample data until full implementation with proper database queries
 */
@RestController
@RequestMapping("/api/aggregates/election/booth-progress")
@RequiredArgsConstructor
@Slf4j
public class ElectionDashboardBoothProgressController {

    @GetMapping("/{accountId}/{electionId}")
    public ResponseEntity<?> get(@PathVariable Long accountId, @PathVariable Long electionId) {
        log.info("Booth progress requested for account={} election={}", accountId, electionId);
        
        // Return placeholder data structure matching expected format
        Map<String, Object> response = Map.of(
            "accountId", accountId,
            "electionId", electionId,
            "boothProgressJson", "[]", // Empty array for now
            "computedAt", OffsetDateTime.now().toString(),
            "refreshedAt", OffsetDateTime.now().toString(),
            "freshnessSeconds", 0L,
            "message", "Booth progress data will be available when database is fully connected"
        );
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{accountId}/{electionId}/recompute")
    public ResponseEntity<?> recompute(@PathVariable Long accountId, @PathVariable Long electionId) {
        log.info("Booth progress recompute requested for account={} election={}", accountId, electionId);
        return ResponseEntity.accepted().body(Map.of("message", "Recompute accepted - not implemented yet"));
    }
}
