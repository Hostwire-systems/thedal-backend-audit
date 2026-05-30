package com.thedal.thedal_app.report.pollday;

import com.thedal.thedal_app.general.RequestDetailsService;
import com.thedal.thedal_app.report.pollday.dto.ApiResponse;
import com.thedal.thedal_app.report.pollday.dto.PollDayChartConfigRequest;
import com.thedal.thedal_app.report.pollday.dto.PollDayChartConfigResponse;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/reporting/poll-day/chart-config")
@RequiredArgsConstructor
@Slf4j
public class PollDayChartConfigController {

    private final PollDayChartConfigService service;
    private final RequestDetailsService requestDetails;

    @PostMapping
    public ResponseEntity<ApiResponse<PollDayChartConfigResponse>> saveChartConfig(
            @Valid @RequestBody PollDayChartConfigRequest request) {
        try {
            // Get account ID from JWT token
            Long accountId = requestDetails.getCurrentAccountId();
            if (accountId == null) {
                throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
            }
            
            // Set accountId from JWT token (override any value in request body)
            request.setAccountId(accountId);
            
            log.info("Received request to save chart config for accountId={}, electionId={}", 
                request.getAccountId(), request.getElectionId());
            
            PollDayChartConfigResponse response = service.saveChartConfig(request);
            
            return ResponseEntity.ok(
                ApiResponse.success("Chart configuration saved successfully", response)
            );
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid chart configuration: {}", e.getMessage());
            return ResponseEntity
                .badRequest()
                .body(ApiResponse.error(e.getMessage()));
                
        } catch (Exception e) {
            log.error("Error saving chart configuration", e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to save chart configuration"));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PollDayChartConfigResponse>> getChartConfig(
            @RequestParam Long electionId) {
        try {
            // Get account ID from JWT token
            Long accountId = requestDetails.getCurrentAccountId();
            if (accountId == null) {
                throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
            }
            
            log.info("Received request to get chart config for accountId={}, electionId={}", 
                accountId, electionId);
            
            Optional<PollDayChartConfigResponse> response = service.getChartConfig(accountId, electionId);
            
            if (response.isPresent()) {
                return ResponseEntity.ok(
                    ApiResponse.success("Chart configuration retrieved successfully", response.get())
                );
            } else {
                return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("No chart configuration found for this election"));
            }
            
        } catch (Exception e) {
            log.error("Error retrieving chart configuration", e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to retrieve chart configuration"));
        }
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deleteChartConfig(
            @RequestParam Long electionId) {
        try {
            // Get account ID from JWT token
            Long accountId = requestDetails.getCurrentAccountId();
            if (accountId == null) {
                throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
            }
            
            log.info("Received request to delete chart config for accountId={}, electionId={}", 
                accountId, electionId);
            
            service.deleteChartConfig(accountId, electionId);
            
            return ResponseEntity.ok(
                ApiResponse.success("Chart configuration deleted successfully", null)
            );
            
        } catch (Exception e) {
            log.error("Error deleting chart configuration", e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to delete chart configuration"));
        }
    }
}
