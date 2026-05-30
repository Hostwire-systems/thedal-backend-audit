package com.thedal.thedal_app.cpanel;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.thedal.thedal_app.cpanel.dtos.SlipBoxDTO;
import com.thedal.thedal_app.general.RequestDetailsService;
import com.thedal.thedal_app.response.ThedalResponse;
import com.thedal.thedal_app.response.ThedalSuccess;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/slip-boxes")
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Slip Box Management")
public class SlipBoxController {

    @Autowired
    private SlipBoxService slipBoxService;

    @Autowired
    private RequestDetailsService requestDetails;

//    @Operation(summary = "Add a slip box",
//               description = "Adds a slip box for a customer, validated against cPanel slip box IDs.")
//    @PostMapping("/election/{electionId}")
//    public ThedalResponse<SlipBoxDTO> createSlipBox(
//            @PathVariable Long electionId,
//            @Valid @RequestBody SlipBoxDTO slipBoxDTO) {
//        Long accountId = requestDetails.getCurrentAccountId();
//        if (accountId == null) {
//            log.error("Account ID not found, unauthorized access.");
//            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
//        }
//        slipBoxDTO.setIsDefault(false);
//
//        return slipBoxService.createSlipBox(accountId, electionId, slipBoxDTO);
//    }
    @Operation(summary = "Add a slip box",
            description = "Adds a slip box for a customer, validated against cPanel slip box IDs.")
 @PostMapping
 public ThedalResponse<SlipBoxDTO> createSlipBox(
         @Valid @RequestBody SlipBoxDTO slipBoxDTO) {
     Long accountId = requestDetails.getCurrentAccountId();
     if (accountId == null) {
         log.error("Account ID not found, unauthorized access.");
         throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
     }
     slipBoxDTO.setIsDefault(false);

     return slipBoxService.createSlipBox(accountId, slipBoxDTO);
 }
    
//    @Operation(summary = "Get all slip boxes",
//               description = "Retrieves all slip boxes for the authenticated user and election.")
//    @GetMapping("/election/{electionId}")
//    public ThedalResponse<List<SlipBoxDTO>> getSlipBoxes(@PathVariable Long electionId) {
//        Long accountId = requestDetails.getCurrentAccountId();
//        if (accountId == null) {
//            log.error("Account ID not found, unauthorized access.");
//            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
//        }
//
//        return slipBoxService.getSlipBoxes(accountId, electionId);
//    }
    @Operation(summary = "Get all slip boxes",
            description = "Retrieves all slip boxes for the authenticated user.")
 @GetMapping
 public ThedalResponse<List<SlipBoxDTO>> getSlipBoxes() {
     Long accountId = requestDetails.getCurrentAccountId();
     if (accountId == null) {
         log.error("Account ID not found, unauthorized access.");
         throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
     }

     return slipBoxService.getSlipBoxes(accountId);
 }

//    @Operation(summary = "Delete all slip boxes",
//    description = "Deletes all slip boxes for the authenticated user and election.")
//    @DeleteMapping("/election/{electionId}")
//    public ThedalResponse<Void> deleteSlipBoxes(
//            @PathVariable("electionId") Long electionId,
//            @RequestParam(value = "slipBoxIds", required = false) List<Long> slipBoxIds) {
//
//        Long accountId = requestDetails.getCurrentAccountId();
//        if (accountId == null) {
//            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
//        }
//
//        List<Long> slipBoxIdList = (slipBoxIds != null && !slipBoxIds.isEmpty()) 
//                ? slipBoxIds 
//                : Collections.emptyList();
//
//        return slipBoxService.deleteSlipBoxes(electionId, accountId, slipBoxIdList);
//    }
    @Operation(summary = "Delete all slip boxes",
            description = "Deletes all slip boxes for the authenticated user.")
 @DeleteMapping
 public ThedalResponse<Void> deleteSlipBoxes(
         @RequestParam(value = "slipBoxIds", required = false) List<Long> slipBoxIds) {
     Long accountId = requestDetails.getCurrentAccountId();
     if (accountId == null) {
         throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
     }

     List<Long> slipBoxIdList = (slipBoxIds != null && !slipBoxIds.isEmpty()) 
             ? slipBoxIds 
             : Collections.emptyList();

     return slipBoxService.deleteSlipBoxes(accountId, slipBoxIdList);
 }

//    @Operation(summary = "Delete a single slip box",
//            description = "Deletes a single slip box by slipBoxId for the authenticated user and election.")
//    @DeleteMapping("/{electionId}/{slipBoxId}")
//    @Transactional
//    public ResponseEntity<ThedalResponse<Void>> deleteSlipBox(
//        @PathVariable("electionId") Long electionId,
//        @PathVariable("slipBoxId") Long slipBoxId) {
//
//        Long accountId = requestDetails.getCurrentAccountId();
//        if (accountId == null) {
//            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
//        }
//
//        slipBoxService.deleteSlipBox(electionId, accountId, slipBoxId);
//        return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.SLIP_BOX_DELETED, null));
//    }
    @Operation(summary = "Delete a single slip box",
            description = "Deletes a single slip box by slipBoxId for the authenticated user.")
 @DeleteMapping("/{slipBoxId}")
 @Transactional
 public ResponseEntity<ThedalResponse<Void>> deleteSlipBox(
         @PathVariable("slipBoxId") Long slipBoxId) {
     Long accountId = requestDetails.getCurrentAccountId();
     if (accountId == null) {
         throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
     }

     slipBoxService.deleteSlipBox(accountId, slipBoxId);
     return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.SLIP_BOX_DELETED, null));
 }
    
    @Operation(summary = "Create default slip boxes for existing elections",
            description = "Creates a default slip box for all existing elections under the authenticated account.")
 @PostMapping("/migrate-default-slip-boxes")
 public ResponseEntity<ThedalResponse<Void>> createDefaultSlipBoxesForExistingElections() {
     Long accountId = requestDetails.getCurrentAccountId();
     if (accountId == null) {
         log.error("Account ID not found, unauthorized access.");
         throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
     }

     slipBoxService.createDefaultSlipBoxesForExistingElections(accountId);
     return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.DEFAULT_SLIP_BOXES_CREATED, null));
 }

//    @Operation(summary = "Update a slip box",
//               description = "Updates a slip box by ID for the authenticated user and election.")
//    @PutMapping("/election/{electionId}/slipbox/{slipBoxId}")
//    public ThedalResponse<SlipBoxDTO> updateSlipBox(
//            @PathVariable Long electionId,
//            @PathVariable Long slipBoxId,
//            @Valid @RequestBody SlipBoxDTO slipBoxDTO) {
//        Long accountId = requestDetails.getCurrentAccountId();
//        if (accountId == null) {
//            log.error("Account ID not found, unauthorized access.");
//            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
//        }
//
//        return slipBoxService.updateSlipBox(accountId, electionId, slipBoxId, slipBoxDTO);
//    }
    @Operation(summary = "Update a slip box",
            description = "Updates a slip box by ID for the authenticated user.")
 @PutMapping("/slipbox/{slipBoxId}")
 public ThedalResponse<SlipBoxDTO> updateSlipBox(
         @PathVariable Long slipBoxId,
         @Valid @RequestBody SlipBoxDTO slipBoxDTO) {
     Long accountId = requestDetails.getCurrentAccountId();
     if (accountId == null) {
         log.error("Account ID not found, unauthorized access.");
         throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
     }

     return slipBoxService.updateSlipBox(accountId, slipBoxId, slipBoxDTO);
 }

}
