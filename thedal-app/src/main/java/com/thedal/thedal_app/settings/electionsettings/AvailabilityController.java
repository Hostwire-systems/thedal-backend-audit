package com.thedal.thedal_app.settings.electionsettings;

import java.util.List;
import java.util.Map;

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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.thedal.thedal_app.general.RequestDetailsService;
import com.thedal.thedal_app.response.ThedalResponse;
import com.thedal.thedal_app.response.ThedalSuccess;
import com.thedal.thedal_app.settings.electionsettings.dto.AvailabilityDTO;
import com.thedal.thedal_app.settings.electionsettings.dto.AvailabilityReorderRequest;
import com.thedal.thedal_app.settings.electionsettings.dto.AvailabilityReponseDTO;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;

import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/availability")
@Slf4j
public class AvailabilityController {

    @Autowired
    private RequestDetailsService requestDetails;

    @Autowired
    private AvailabilityService availabilityService;

    @PostMapping("/{electionId}")
    public ThedalResponse<AvailabilityReponseDTO> createAvailability(@RequestPart("DTO") AvailabilityDTO availabilityDTO,@PathVariable Long electionId,@RequestPart("file") MultipartFile file) {
        
        ThedalResponse<AvailabilityReponseDTO> availability =availabilityService.createAvailability(availabilityDTO,electionId,file); 
        new ThedalResponse<>(ThedalSuccess.AVAILABILITY_CREATED, availability);
		return availability;
    }
    
//    @GetMapping("/{electionId}")
//    public ThedalResponse<List<AvailabilityResponse>> getAvailability(@PathVariable Long electionId) {
//    Long accountId = requestDetails.getCurrentAccountId();
//
//    if (accountId == null) {
//        log.error("Account ID not found, unauthorized access.");
//        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
//    }
//
//    List<AvailabilityResponse> availabilityDTOs = availabilityService.getAvailability(accountId, electionId);
//    return new ThedalResponse<>(ThedalSuccess.AVAILABILITY_FOUND, availabilityDTOs);
//    }
   // @Operation(summary = "Get availabilities", description = "Fetch availabilities with voter counts for a specific election", tags = {"Election Settings"})
    @GetMapping("/{electionId}")
    public ThedalResponse<List<Map<String, Object>>> getAvailability(@PathVariable Long electionId) {
        Long accountId = requestDetails.getCurrentAccountId();

        if (accountId == null) {
            log.error("Account ID not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        log.info("Fetching availabilities from MongoDB for electionId: {}", electionId);
        // Use MongoDB service instead of PostgreSQL service for better performance
        return availabilityService.getAvailabilityFromMongo(accountId, electionId);
    }

    @PutMapping("{electionId}/{availabilityId}")
    public ThedalResponse<AvailabilityReponseDTO> updateAvailability(@PathVariable Long electionId,@PathVariable Long availabilityId, @RequestPart("DTO") AvailabilityDTO availabilityDTO, @RequestPart(value = "file", required = false) MultipartFile file) {
    Long accountId = requestDetails.getCurrentAccountId();

    if (accountId == null) {
        log.error("Account ID not found, unauthorized access.");

        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
    }            

    AvailabilityReponseDTO updatedAvailability = availabilityService.updateAvailability(accountId,electionId,availabilityId, availabilityDTO,file);
        return new ThedalResponse<>(ThedalSuccess.AVAILABILITY_UPDATED, updatedAvailability);
    }
    
//    @DeleteMapping("{electionId}/availabilities")
//    public ThedalResponse<Void> deleteAvailability(
//        @PathVariable Long electionId,
//        @RequestParam(value = "availabilityIds", required = false) List<Long> availabilityIds) {
//    
//    Long accountId = requestDetails.getCurrentAccountId();
//    if (accountId == null) {
//        log.error("Account ID not found, unauthorized access.");
//        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
//    }
//
//    availabilityService.deleteAvailability(accountId, electionId, availabilityIds);
//
//    return new ThedalResponse<>(ThedalSuccess.AVAILABILITY_DELETED, null);
//}
    @DeleteMapping("/{electionId}/availabilities")
    public ResponseEntity<ThedalResponse<Map<String, Object>>> deleteAvailability(
            @PathVariable Long electionId,
            @RequestParam(value = "availabilityIds", required = false) List<Long> availabilityIds) {
        log.info("Received DELETE request for electionId: {}, availabilityIds: {}", electionId, availabilityIds);
        Long accountId = requestDetails.getCurrentAccountId();
        log.info("Retrieved accountId: {}", accountId);
        if (accountId == null) {
            log.error("Account ID not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED, "Account ID not found, unauthorized access.");
        }

        availabilityService.deleteAvailability(accountId, electionId, availabilityIds);
        return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.AVAILABILITY_DELETED, null));
    }


    @PutMapping("/{electionId}/reorder")
    public ResponseEntity<ThedalResponse<String>> reorderAvailabilities(
            @PathVariable Long electionId,
            @RequestBody List<AvailabilityReorderRequest> reorderRequests) {
        try {
            Long accountId = requestDetails.getCurrentAccountId();
            if (accountId == null) {
                log.error("Account ID not found, unauthorized access.");
                throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
            }

            availabilityService.updateAvailabilityOrder(reorderRequests, accountId, electionId);
            return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.AVAILABILITY_ORDER_UPDATED));

        } catch (Exception e) {
            log.error("Error reordering availabilities: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ThedalResponse<>(ThedalError.AVAILABILITY_ORDER_UPDATE_FAILED));
        }
    }
    
    @Operation(summary = "Get availabilities from MongoDB", description = "Fetch availabilities from MongoDB for a specific election")
    @GetMapping("/mongo/{electionId}")
    public ThedalResponse<List<Map<String, Object>>> getAvailabilityFromMongo(@PathVariable Long electionId) {
        Long accountId = requestDetails.getCurrentAccountId();

        if (accountId == null) {
            log.error("Account ID not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        log.info("Fetching availabilities from MongoDB for electionId: {}", electionId);
        return availabilityService.getAvailabilityFromMongo(accountId, electionId);
    }


}
