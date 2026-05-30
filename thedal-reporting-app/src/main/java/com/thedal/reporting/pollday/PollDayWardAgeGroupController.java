package com.thedal.reporting.pollday;

import com.thedal.reporting.aggregates.RecomputeRateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;

@RestController
@RequestMapping("/api/reporting/poll-day/ward-age-groups")
@RequiredArgsConstructor
public class PollDayWardAgeGroupController {

    private final PollDayWardAgeGroupTurnoutRepository repository;
    private final PollDayDashboardAggregationService aggregationService;
    private final RecomputeRateLimiter rateLimiter;

    @GetMapping
    public ResponseEntity<PollDayWardAgeGroupTurnout> get(
            @RequestParam Long accountId, 
            @RequestParam Long electionId,
            @RequestParam String partNumber,
            @RequestParam(required = false) String pollingDate) {
        
        LocalDate date = pollingDate != null ? LocalDate.parse(pollingDate) : LocalDate.now(ZoneId.of("Asia/Kolkata"));
        PollDayWardAgeGroupTurnout data = repository.findByAccountIdAndElectionIdAndPartNumberAndPollingDate(
                accountId, electionId, partNumber, date)
                .orElse(new PollDayWardAgeGroupTurnout());

        String etag = "\"" + (data.getRefreshedAt() != null ? data.getRefreshedAt().toInstant().toEpochMilli() : System.currentTimeMillis()) + "\"";
        return ResponseEntity.ok()
                .header("ETag", etag)
                .header("Cache-Control", "public, max-age=30")
                .body(data);
    }

    @PostMapping("/recompute")
    public ResponseEntity<PollDayWardAgeGroupTurnout> recompute(
            @RequestParam Long accountId, 
            @RequestParam Long electionId,
            @RequestParam String partNumber,
            @RequestParam(required = false) String pollingDate) {
        
        if (!rateLimiter.allow("poll-day-ward-age-groups", accountId, electionId)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }

        LocalDate date = pollingDate != null ? LocalDate.parse(pollingDate) : LocalDate.now(ZoneId.of("Asia/Kolkata"));
        PollDayWardAgeGroupTurnout data = aggregationService.recomputeWardAgeGroups(accountId, electionId, partNumber, date);
        
        String etag = "\"" + data.getRefreshedAt().toInstant().toEpochMilli() + "\"";
        return ResponseEntity.ok()
                .header("ETag", etag)
                .header("Cache-Control", "public, max-age=30")
                .body(data);
    }
}
