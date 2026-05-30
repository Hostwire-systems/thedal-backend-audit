package com.thedal.thedal_app.election;


import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
import org.springframework.web.multipart.MultipartFile;

import com.thedal.thedal_app.election.dtos.BoothReorderRequest;
import com.thedal.thedal_app.election.dtos.BoothSlipPrintRequest;
import com.thedal.thedal_app.election.dtos.BoothSlipPrintResponse;
import com.thedal.thedal_app.election.dtos.ElectionBoothRequest;
import com.thedal.thedal_app.election.dtos.ElectionBoothResponse;
import com.thedal.thedal_app.general.RequestDetailsService;
import com.thedal.thedal_app.response.ThedalResponse;
import com.thedal.thedal_app.response.ThedalSuccess;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/booths")
@Slf4j
public class BoothController {

    @Autowired
    private BoothService boothService;
    @Autowired
    private ElectionRepository electionRepository;
    @Autowired
    private RequestDetailsService requestDetails;
    
    private void validateElectionOwnership(Long electionId, Long accountId) {
        Optional<ElectionEntity> electionOpt = electionRepository.findByIdAndAccountId(electionId, accountId);
        if (!electionOpt.isPresent()) {
            log.error("Election ID {} does not belong to Account ID {}", electionId, accountId);
            throw new ThedalException(ThedalError.INVALID_ELECTION, HttpStatus.FORBIDDEN);  
        }
    }    

    @PostMapping("/{electionId}")
    public ResponseEntity<ThedalResponse<ElectionBoothResponse>> createBooth(
            @PathVariable("electionId") Long electionId,
            @RequestBody ElectionBoothRequest boothRequest) {
    	
    	Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account ID not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }
        validateElectionOwnership(electionId, accountId);
        
        ElectionBoothResponse boothResponse = boothService.createBooth(accountId, electionId, boothRequest);

        ThedalResponse<ElectionBoothResponse> response = new ThedalResponse<>(ThedalSuccess.BOOTH_CREATED, boothResponse);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{electionId}")
    public ResponseEntity<ThedalResponse<Page<ElectionBoothResponse>>> getBoothsByElectionId(
            @PathVariable("electionId") Long electionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Long accountId = requestDetails.getCurrentAccountId();
            if (accountId == null) {
                log.error("Account ID not found, unauthorized access.");
                throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
            }
            validateElectionOwnership(electionId, accountId);
            
            //requestDetails.checkUserRolePermission("booth_list", "R");

            Pageable pageable = PageRequest.of(page, size);
            Page<ElectionBoothResponse> booths = boothService.findAllByElectionIdAndAccountId(electionId, accountId, pageable);

            // Success response
            ThedalResponse<Page<ElectionBoothResponse>> response = new ThedalResponse<>(ThedalSuccess.BOOTH_FETCHED, booths);
            return ResponseEntity.ok(response);

        } catch (EntityNotFoundException e) {
            log.error("Election not found for election ID {}: {}", electionId, e.getMessage());

            // In case of error, return an empty page as data
            Page<ElectionBoothResponse> emptyPage = Page.empty();
            ThedalResponse<Page<ElectionBoothResponse>> errorResponse = new ThedalResponse<>(ThedalError.ELECTION_NOT_FOUND, emptyPage);

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        } catch (Exception e) {
            log.error("Unexpected error occurred: {}", e.getMessage());

            // In case of an unexpected error, return an empty page as data
            Page<ElectionBoothResponse> emptyPage = Page.empty();
            ThedalResponse<Page<ElectionBoothResponse>> errorResponse = new ThedalResponse<>(ThedalError.BOOTH_FETCH_FAILED, emptyPage);

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
    }
 

    @GetMapping("/{electionId}/{boothNumber}")
    public ResponseEntity<ThedalResponse<ElectionBoothResponse>> getBoothByNumber(
            @PathVariable("electionId") Long electionId,
            @PathVariable("boothNumber") Integer boothNumber) {

        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account ID not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }
        validateElectionOwnership(electionId, accountId);

        try {
            ElectionBoothResponse booth = boothService.findByElectionIdAndBoothNumberAndAccountId(electionId, boothNumber, accountId);

            ThedalResponse<ElectionBoothResponse> response = new ThedalResponse<>(ThedalSuccess.BOOTH_FETCHED, booth);
            return ResponseEntity.ok(response);

        } catch (ThedalException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error occurred while fetching booth with number {} for election ID {}: {}", boothNumber, electionId, e.getMessage());
            ThedalResponse<ElectionBoothResponse> errorResponse = new ThedalResponse<>(ThedalError.BOOTH_FETCH_FAILED, null);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
    }
    
    @DeleteMapping("/{electionId}/{boothNumber}")
    public ResponseEntity<ThedalResponse<String>> deleteBooth(
            @PathVariable("electionId") Long electionId,
            @PathVariable("boothNumber") Integer boothNumber) {

        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account ID not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }
        validateElectionOwnership(electionId, accountId);

        boothService.deleteBoothByElectionIdAndBoothNumberAndAccountId(electionId, boothNumber, accountId);

        ThedalResponse<String> response = new ThedalResponse<>(ThedalSuccess.BOOTH_DELETED);
        return ResponseEntity.ok(response);
    }


    @PutMapping("/{electionId}/{boothNumber}")
    public ResponseEntity<ThedalResponse<ElectionBoothResponse>> updateBooth(
            @PathVariable("electionId") Long electionId,
            @PathVariable("boothNumber") Integer boothNumber,
            @RequestBody ElectionBoothRequest boothRequest) {
        
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account ID not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }
        validateElectionOwnership(electionId, accountId);

        // Correct the method name here
        ElectionBoothResponse boothResponse = boothService.updateBoothByElectionIdAndBoothNumber(electionId, boothNumber, boothRequest, accountId);

        ThedalResponse<ElectionBoothResponse> response = new ThedalResponse<>(ThedalSuccess.BOOTH_UPDATED, boothResponse);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{electionId}/booth-slips/print")
    public ResponseEntity<ThedalResponse<BoothSlipPrintResponse>> printBoothSlip(
            @PathVariable Long electionId,
            //@RequestHeader("Volunteer-ID") Long volunteerId,
            @RequestBody BoothSlipPrintRequest request) {
    	
        ThedalResponse<BoothSlipPrintResponse> response = boothService.printBoothSlip(electionId, request);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{electionId}/booth-slips/{voterId}")
    public ResponseEntity<ThedalResponse<List<BoothSlipPrintResponse>>> getBoothSlip(
            @PathVariable Long electionId,
            @PathVariable String voterId) {

        ThedalResponse<List<BoothSlipPrintResponse>> response = boothService.getBoothSlip(electionId, voterId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{electionId}/booths/reorder")
    public ResponseEntity<ThedalResponse<String>> reorderBooths(
            @PathVariable Long electionId,
            @RequestBody List<BoothReorderRequest> reorderRequests) {
        try {
            Long accountId = requestDetails.getCurrentAccountId();
            if (accountId == null) {
                log.error("Account ID not found, unauthorized access.");
                throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
            }
            validateElectionOwnership(electionId, accountId);
            
            boothService.updateBoothOrder(reorderRequests, accountId, electionId);

            return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.BOOTH_ORDER_UPDATED));

        } catch (Exception e) {
            log.error("Unexpected error occurred while reordering booths: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ThedalResponse<>(ThedalError.BOOTH_ORDER_UPDATE_FAILED));
        }
    }


    @Operation(summary = "Upload bulk Booth Data", description = "Upload bulk Booth data using xlsx or csv files.")
    @PostMapping(value = "/election/{electionId}/booth-upload", consumes = "multipart/form-data")
    public ResponseEntity<ThedalResponse<Void>> uploadBoothsFromXlsxOrCsv(
        @RequestParam("file") MultipartFile file, 
        @PathVariable Long electionId) throws IOException {
    
    ThedalResponse<Void> response = boothService.uploadBoothFromXlsxOrCsv(file, electionId);
    return ResponseEntity.ok(response);
}



    
}