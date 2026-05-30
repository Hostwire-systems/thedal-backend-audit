package com.thedal.reporting.pollday;

import com.thedal.reporting.aggregates.RecomputeRateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;

@RestController
@RequestMapping("/api/reporting/poll-day/hourly")
@RequiredArgsConstructor
public class PollDayHourlyTurnoutController {

    private final PollDayHourlyTurnoutRepository repository;
    private final PollDayDashboardAggregationService aggregationService;
    private final RecomputeRateLimiter rateLimiter;

    @GetMapping
    public ResponseEntity<PollDayHourlyTurnout> get(
            @RequestParam Long accountId, 
            @RequestParam Long electionId,
            @RequestParam(required = false) String pollingDate) {
        
        LocalDate date = pollingDate != null ? LocalDate.parse(pollingDate) : LocalDate.now(ZoneId.of("Asia/Kolkata"));
        PollDayHourlyTurnout data = repository.findByAccountIdAndElectionIdAndPollingDate(accountId, electionId, date)
                .orElse(new PollDayHourlyTurnout());

        String etag = "\"" + (data.getRefreshedAt() != null ? data.getRefreshedAt().toInstant().toEpochMilli() : System.currentTimeMillis()) + "\"";
        return ResponseEntity.ok()
                .header("ETag", etag)
                .header("Cache-Control", "public, max-age=30")
                .body(data);
    }

    @PostMapping("/recompute")
    public ResponseEntity<PollDayHourlyTurnout> recompute(
            @RequestParam Long accountId, 
            @RequestParam Long electionId,
            @RequestParam(required = false) String pollingDate) {
        
        if (!rateLimiter.allow("poll-day-hourly", accountId, electionId)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }

        LocalDate date = pollingDate != null ? LocalDate.parse(pollingDate) : LocalDate.now(ZoneId.of("Asia/Kolkata"));
        PollDayHourlyTurnout data = aggregationService.recomputeHourly(accountId, electionId, date);
        
        String etag = "\"" + data.getRefreshedAt().toInstant().toEpochMilli() + "\"";
        return ResponseEntity.ok()
                .header("ETag", etag)
                .header("Cache-Control", "public, max-age=30")
                .body(data);
    }
}
