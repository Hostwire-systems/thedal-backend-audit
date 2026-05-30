package com.thedal.thedal_app.voter.activity;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.thedal.thedal_app.general.RequestDetailsService;
import com.thedal.thedal_app.response.ThedalResponse;
import com.thedal.thedal_app.response.ThedalSuccess;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;
import com.thedal.thedal_app.voter.activity.dto.ActivityCountResponse;
import com.thedal.thedal_app.voter.activity.dto.ActivityHistoryResponse;
import com.thedal.thedal_app.voter.activity.dto.ElectionActivitySummary;
import com.thedal.thedal_app.voter.activity.dto.RecordActivityRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/voter-activity")
@Slf4j
@Tag(name = "Voter Activity Tracking", description = "APIs for tracking voter slip prints, WhatsApp/SMS/Voice shares and other activities")
public class VoterActivityController {
    
    @Autowired
    private VoterActivityService activityService;
    
    /**
     * Record a single voter activity
     * Performance: Fast - uses async logging, only updates counter synchronously
     */
    @Operation(
        summary = "Record voter activity",
        description = "Records a single activity for a voter (slip print, WhatsApp share, etc.). " +
                     "Updates counter immediately and logs asynchronously for maximum performance."
    )
    @PostMapping("/election/{electionId}/record")
    public ResponseEntity<ThedalResponse<Void>> recordActivity(
            @PathVariable Long electionId,
            @Valid @RequestBody RecordActivityRequest request) {
        
        activityService.recordActivity(electionId, request);
        
        return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.SUCCESS));
    }
    
    /**
     * Record multiple activities in batch (e.g., bulk WhatsApp send)
     * Performance: Optimized for bulk operations with grouped updates
     */
    @Operation(
        summary = "Record batch activities",
        description = "Records multiple activities at once for bulk operations like mass WhatsApp sends. " +
                     "Optimized for performance with grouped voter updates."
    )
    @PostMapping("/election/{electionId}/record-batch")
    public ResponseEntity<ThedalResponse<Void>> recordBatchActivities(
            @PathVariable Long electionId,
            @Valid @RequestBody List<RecordActivityRequest> requests) {
        
        activityService.recordBatchActivities(electionId, requests);
        
        return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.SUCCESS));
    }
    
    /**
     * Get activity counts for a specific voter
     * Performance: Fastest - reads directly from VoterEntity counters (no joins)
     */
    @Operation(
        summary = "Get voter activity counts",
        description = "Retrieves activity counts for a specific voter. " +
                     "Ultra-fast - reads from cached counters in voter table."
    )
    @GetMapping("/election/{electionId}/voter/{voterId}/counts")
    public ResponseEntity<ThedalResponse<ActivityCountResponse>> getActivityCounts(
            @PathVariable Long electionId,
            @PathVariable String voterId) {
        
        ActivityCountResponse response = activityService.getActivityCounts(electionId, voterId);
        
        return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.SUCCESS, response));
    }
    
    /**
     * Get activity history for a voter with pagination
     * Performance: Uses indexed query on activity_log table
     */
    @Operation(
        summary = "Get voter activity history",
        description = "Retrieves detailed activity history for a voter with timestamps, volunteer info, etc. " +
                     "Supports pagination and filtering by activity type."
    )
    @GetMapping("/election/{electionId}/voter/{voterId}/history")
    public ResponseEntity<ThedalResponse<ActivityHistoryResponse>> getActivityHistory(
            @PathVariable Long electionId,
            @PathVariable String voterId,
            @Parameter(description = "Filter by activity type (optional)")
            @RequestParam(required = false) ActivityType activityType,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "50") int size) {
        
        ActivityHistoryResponse response = activityService.getActivityHistory(
            electionId, voterId, activityType, page, size);
        
        return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.SUCCESS, response));
    }
    
    /**
     * Get election-wide activity summary
     * Performance: Uses aggregated queries with GROUP BY
     */
    @Operation(
        summary = "Get election activity summary",
        description = "Retrieves election-wide activity statistics including: " +
                     "total counts by type, unique voters, most active voters. " +
                     "Useful for dashboards and reports."
    )
    @GetMapping("/election/{electionId}/summary")
    public ResponseEntity<ThedalResponse<ElectionActivitySummary>> getElectionActivitySummary(
            @PathVariable Long electionId,
            @Parameter(description = "Number of top active voters to return (default: 10)")
            @RequestParam(required = false, defaultValue = "10") Integer topVotersLimit) {
        
        ElectionActivitySummary response = activityService.getElectionActivitySummary(
            electionId, topVotersLimit);
        
        return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.SUCCESS, response));
    }
}
