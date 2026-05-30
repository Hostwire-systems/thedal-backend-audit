package com.thedal.thedal_app.settings.electionsettings;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.thedal.thedal_app.cpanel.VoterHistoryReorderRequest;
import com.thedal.thedal_app.cpanel.VoterHistoryRequest;
import com.thedal.thedal_app.cpanel.VoterHistoryService;
import com.thedal.thedal_app.cpanel.dtos.VoterHistoryEntity;
import com.thedal.thedal_app.general.RequestDetailsService;
import com.thedal.thedal_app.response.ThedalResponse;
import com.thedal.thedal_app.response.ThedalSuccess;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;
import org.springframework.web.multipart.MultipartFile;


import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/voter-history")
public class VoterHistoryController {

    @Autowired
    private VoterHistoryService voterHistoryService;

    @Autowired
    private RequestDetailsService requestDetails;

    @Operation(
        summary = "Create a new voter history",
        description = "Saves a new voter history with an image for a given election ID.",
        tags = { "Voter History" }
    )
    @PostMapping(value = "/{electionId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ThedalResponse<VoterHistoryEntity> createVoterHistory(
        @PathVariable("electionId") Long electionId,
        @RequestPart("voterHistoryName") String voterHistoryName,
        @RequestPart("voterHistoryImage") MultipartFile voterHistoryImage) {

        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account ID not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        VoterHistoryRequest voterHistoryRequest = new VoterHistoryRequest();
        voterHistoryRequest.setVoterHistoryName(voterHistoryName);
        voterHistoryRequest.setVoterHistoryImage(voterHistoryImage);

        return voterHistoryService.createVoterHistory(voterHistoryRequest, electionId, accountId);
    }

//    @Operation(
//        summary = "Get all voter history records",
//        description = "Fetches a list of all voter history records for a specific election and account.",
//        tags = { "Voter History" }
//    )
//    @GetMapping("/{electionId}")
//    public ThedalResponse<List<Map<String, Object>>> getAllVoterHistory(@PathVariable Long electionId) {
//
//        Long accountId = requestDetails.getCurrentAccountId();
//        if (accountId == null) {
//            log.error("Account ID not found, unauthorized access.");
//            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
//        }
//
//        log.info("Fetching all voter history records for account ID: {} and election ID: {}", accountId, electionId);
//
//        List<VoterHistoryEntity> voterHistories = voterHistoryService.getAllVoterHistories(accountId, electionId);
//
//        if (voterHistories.isEmpty()) {
//            log.error("No voter history records found for account ID: {} and election ID: {}", accountId, electionId);
//            throw new ThedalException(ThedalError.VOTER_HISTORY_NOT_FOUND, HttpStatus.NOT_FOUND);
//        }
//
//        List<Map<String, Object>> voterHistoryDetails = voterHistories.stream()
//                .map(voterHistory -> {
//                    Map<String, Object> voterHistoryData = new HashMap<>();
//                    voterHistoryData.put("voterHistoryId", voterHistory.getId());
//                    voterHistoryData.put("voterHistoryName", voterHistory.getVoterHistoryName());
//                    voterHistoryData.put("voterHistoryImage", voterHistory.getVoterHistoryImage());
//                    voterHistoryData.put("orderIndex", voterHistory.getOrderIndex()); // if applicable
//                    return voterHistoryData;
//                }).collect(Collectors.toList());
//
//        return new ThedalResponse<>(ThedalSuccess.VOTER_HISTORY_FETCHED, voterHistoryDetails);
//    }
    @Operation(
            summary = "Get all voter history records",
            description = "Fetches a list of all voter history records with voter counts for a specific election and account.",
            tags = { "Voter History" }
        )
        @GetMapping("/{electionId}")
        public ThedalResponse<List<Map<String, Object>>> getAllVoterHistory(@PathVariable Long electionId) {
            Long accountId = requestDetails.getCurrentAccountId();
            if (accountId == null) {
                log.error("Account ID not found, unauthorized access.");
                throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
            }

            log.info("Fetching all voter history records from PostgreSQL for account ID: {} and election ID: {}", accountId, electionId);
            // Use PostgreSQL service for consistency
            List<Map<String, Object>> voterHistoryDetails = voterHistoryService.getAllVoterHistories1(accountId, electionId);

            if (voterHistoryDetails.isEmpty()) {
                log.error("No voter history records found for account ID: {} and election ID: {}", accountId, electionId);
                throw new ThedalException(ThedalError.VOTER_HISTORY_NOT_FOUND, HttpStatus.NOT_FOUND);
            }

            return new ThedalResponse<>(ThedalSuccess.VOTER_HISTORY_FETCHED, voterHistoryDetails);
        }
    

    @Operation(
        summary = "Update a voter history",
        description = "Updates an existing voter history record for a given election ID and voterHistoryId.",
        tags = { "Voter History" }
    )
    @PutMapping(value = "/{electionId}/{voterHistoryId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ThedalResponse<VoterHistoryEntity> updateVoterHistory(
            @PathVariable("electionId") Long electionId,
            @PathVariable("voterHistoryId") Long voterHistoryId,
            @RequestPart("voterHistoryName") String voterHistoryName,
            @RequestPart(value = "voterHistoryImage", required = false) MultipartFile voterHistoryImage) {

        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        VoterHistoryRequest voterHistoryRequest = new VoterHistoryRequest();
        voterHistoryRequest.setVoterHistoryName(voterHistoryName);
        voterHistoryRequest.setVoterHistoryImage(voterHistoryImage);

        VoterHistoryEntity updatedVoterHistory = voterHistoryService.updateVoterHistory(
                voterHistoryId, electionId, accountId, voterHistoryRequest);

        return new ThedalResponse<>(ThedalSuccess.VOTER_HISTORY_UPDATED, updatedVoterHistory);
    }

    @Operation(
        summary = "Delete voter history",
        description = "Deletes voter history records by their IDs for a specific election.",
        tags = { "Voter History" }
    )
    @DeleteMapping("/{electionId}")
    public ThedalResponse<Void> deleteVoterHistory(
            @PathVariable("electionId") Long electionId,
            @RequestParam(value = "voterHistoryIds", required = false) List<Long> voterHistoryIds) {

        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account ID not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        log.info("Received request to delete voter history with IDs: {} for election ID: {} and account ID: {}", voterHistoryIds, electionId, accountId);

        voterHistoryService.deleteVoterHistories(voterHistoryIds, accountId, electionId);

        return new ThedalResponse<>(ThedalSuccess.VOTER_HISTORY_DELETED, null);
    }

    @Operation(
        summary = "Reorder voter history records",
        description = "Updates the order of voter history records based on the provided list of reorder requests.",
        tags = { "Voter History" }
    )
    @PutMapping("/{electionId}/reorder")
    public ResponseEntity<ThedalResponse<String>> reorderVoterHistories(
            @PathVariable Long electionId,
            @RequestBody List<VoterHistoryReorderRequest> reorderRequests) {
        try {
            Long accountId = requestDetails.getCurrentAccountId();
            if (accountId == null) {
                log.error("Account ID not found, unauthorized access.");
                throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
            }

            voterHistoryService.updateVoterHistoryOrder(reorderRequests, accountId, electionId);
            return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.VOTER_HISTORY_ORDER_UPDATED));

        } catch (Exception e) {
            log.error("Error reordering voter histories: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ThedalResponse<>(ThedalError.VOTER_HISTORY_ORDER_UPDATE_FAILED));
        }
    }
    
    @Operation(
    	    summary = "Get all voter histories from MongoDB",
    	    description = "Retrieves all voter history records for a given account and election from MongoDB only.",
    	    tags = { "Voter History" }
    	)
    @GetMapping("/{electionId}/mongo")
    public ResponseEntity<ThedalResponse<List<Map<String, Object>>>> getVoterHistoriesFromMongo(
    		@PathVariable Long electionId) {
    	Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }
        log.info("Received GET request for voter histories - accountId: {}, electionId: {}", accountId, electionId);

        List<Map<String, Object>> histories = voterHistoryService.getAllVoterHistoriesFromMongo(accountId, electionId);
        ThedalResponse<List<Map<String, Object>>> response =
                new ThedalResponse<>(ThedalSuccess.VOTER_HISTORY_FETCHED, histories);

        return ResponseEntity.ok(response);
    }

}
