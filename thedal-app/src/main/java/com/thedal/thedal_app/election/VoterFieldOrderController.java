package com.thedal.thedal_app.election;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.thedal.thedal_app.election.dtos.VoterFieldOrderRequestDTO;
import com.thedal.thedal_app.election.dtos.VoterFieldOrderResponseDTO;
import com.thedal.thedal_app.general.RequestDetailsService;
import com.thedal.thedal_app.response.ThedalResponse;
import com.thedal.thedal_app.response.ThedalSuccess;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/voter-field-order")
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Voter Field Order Management")
public class VoterFieldOrderController {

    @Autowired
    private VoterFieldOrderService voterFieldOrderService;

    @Autowired
    private RequestDetailsService requestDetails;
    
    @Operation(summary = "Reorder voter fields",
            description = "Reorders fields for the Add Voters and Edit Voters interfaces for a specific election.")
 @PutMapping("/election/{electionId}/reorder")
 public ResponseEntity<ThedalResponse<VoterFieldOrderResponseDTO>> reorderVoterFields(
         @PathVariable Long electionId,
         @Valid @RequestBody VoterFieldOrderRequestDTO requestDTO) {
     try {
         Long accountId = requestDetails.getCurrentAccountId();
         if (accountId == null) {
             log.error("Account ID not found, unauthorized access.");
             throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
         }

         VoterFieldOrderResponseDTO result = voterFieldOrderService.updateFieldOrder(accountId, electionId, requestDTO);
         return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.VOTER_FIELD_ORDER_UPDATED, result));
     } catch (ThedalException ex) {
         log.error("Error reordering voter fields: {}", ex.getMessage());
         return ResponseEntity.status(ex.getHttpStatus())
                 .body(new ThedalResponse<>(ex.getThedalError()));
     } catch (Exception ex) {
         log.error("Unexpected error reordering voter fields: {}", ex.getMessage());
         return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                 .body(new ThedalResponse<>(ThedalError.INTERNAL_SERVER_ERROR));
     }
     
    }

    @Operation(summary = "Get voter field order",
               description = "Retrieves the ordered list of fields for the Add Voters and Edit Voters interfaces.")
    @GetMapping("/election/{electionId}")
    public ResponseEntity<ThedalResponse<VoterFieldOrderResponseDTO>> getVoterFieldOrder(
            @PathVariable Long electionId) {
        try {
            Long accountId = requestDetails.getCurrentAccountId();
            if (accountId == null) {
                log.error("Account ID not found, unauthorized access.");
                throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
            }

            VoterFieldOrderResponseDTO result = voterFieldOrderService.getFieldOrder(accountId, electionId);
            return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.VOTER_FIELD_ORDER_FOUND, result));
        } catch (ThedalException ex) {
            log.error("Error fetching voter field order: {}", ex.getMessage());
            return ResponseEntity.status(ex.getHttpStatus())
                    .body(new ThedalResponse<>(ex.getThedalError()));
        }catch (Exception ex) {
            log.error("Unexpected error fetching voter field order: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ThedalResponse<>(ThedalError.INTERNAL_SERVER_ERROR));
        }
    }
}