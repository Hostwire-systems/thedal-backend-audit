package com.thedal.reporting.aggregates;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Placeholder controller for contact status reporting
 */
@RestController
@RequestMapping("/api/aggregates/election/contact-status")
@RequiredArgsConstructor
@Slf4j
public class ElectionDashboardContactStatusController {

    @GetMapping("/{accountId}/{electionId}")
    public ResponseEntity<?> get(@PathVariable Long accountId, @PathVariable Long electionId) {
        log.info("Contact status requested for account={} election={}", accountId, electionId);
        
        Map<String, Object> response = Map.of(
            "accountId", accountId,
            "electionId", electionId,
            "contactStatusJson", "[]",
            "computedAt", OffsetDateTime.now().toString(),
            "refreshedAt", OffsetDateTime.now().toString(),
            "freshnessSeconds", 0L,
            "message", "Contact status data will be available when database is fully connected"
        );
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{accountId}/{electionId}/recompute")
    public ResponseEntity<?> recompute(@PathVariable Long accountId, @PathVariable Long electionId) {
        log.info("Contact status recompute requested for account={} election={}", accountId, electionId);
        return ResponseEntity.accepted().body(Map.of("message", "Recompute accepted - not implemented yet"));
    }
}
