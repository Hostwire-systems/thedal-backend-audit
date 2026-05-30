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
import com.thedal.thedal_app.settings.electionsettings.dto.BenefitSchemeReorderRequest;
import com.thedal.thedal_app.settings.electionsettings.dto.BenefitSchemesDTO;
import com.thedal.thedal_app.settings.electionsettings.dto.BenefitSchemesUpdateDTO;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;

import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/user/benefit-schemes")
@Slf4j
public class BenefitSchemesController {

    @Autowired
    private RequestDetailsService requestDetails;
    
    @Autowired
    private BenefitSchemesService benefitSchemesService;


    @PostMapping("/{electionId}")
    public ThedalResponse<BenefitSchemesDTO> createBenefitScheme(@RequestPart("benefit") BenefitSchemesUpdateDTO benefitSchemesDTO,@PathVariable Long electionId,MultipartFile file) {
        
        ThedalResponse<BenefitSchemesDTO> benefitSchemes =benefitSchemesService.createBenefitScheme(benefitSchemesDTO,electionId,file); 
        new ThedalResponse<>(ThedalSuccess.BENEFITSCHEME_CREATED, benefitSchemes);
		return benefitSchemes;
    }

//    @GetMapping("/{electionId}")
//    public  ResponseEntity<ThedalResponse<List<BenefitSchemesDTO>>> getAllBenefitSchemes(@PathVariable Long electionId) {
//      
//         Long accountId = requestDetails.getCurrentAccountId();
//
//        if (accountId == null) {
//            log.error("Account ID not found, unauthorized access.");
//            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
//        }
//
//        ThedalResponse<List<BenefitSchemesDTO>> response = benefitSchemesService.getAll(accountId,electionId);
//
//        return ResponseEntity.ok(response);
//    }
    
    @GetMapping("/{electionId}")
    public ResponseEntity<ThedalResponse<List<Map<String, Object>>>> getAllBenefitSchemes(@PathVariable Long electionId) {
        Long accountId = requestDetails.getCurrentAccountId();

        if (accountId == null) {
            log.error("Account ID not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        log.info("Fetching benefit schemes from PostgreSQL for electionId: {}", electionId);
        // Use PostgreSQL service for consistency
        ThedalResponse<List<Map<String, Object>>> response = benefitSchemesService.getAllFromMongo(accountId, electionId);
        return ResponseEntity.ok(response);
    }


    @PutMapping("/{electionId}/{benefitSchemeId}")
    public ThedalResponse<BenefitSchemesDTO> updateBenefitScheme(
        @PathVariable Long electionId,@PathVariable Long benefitSchemeId,@RequestPart("benefit") BenefitSchemesUpdateDTO benefitSchemesupdateDTO,@RequestPart(value = "file", required = false) MultipartFile file) {
            ThedalResponse<BenefitSchemesDTO> benefitSchemesUpdate= benefitSchemesService.updateBenefitScheme(benefitSchemeId,electionId,benefitSchemesupdateDTO ,file);
        new ThedalResponse<>(ThedalSuccess.BENEFITSCHEME_CREATED, benefitSchemesupdateDTO);
		return benefitSchemesUpdate;
        
    }

    @DeleteMapping("/{electionId}")
public ResponseEntity<ThedalResponse<Void>> deleteBenefitScheme(
        @PathVariable Long electionId,
        @RequestParam(required = false) List<Long> benefitSchemeIds) {
    	log.info("Received DELETE request for electionId: {}, benefitSchemeIds: {}", electionId, benefitSchemeIds);
    Long accountId = requestDetails.getCurrentAccountId();

    if (accountId == null) {
        log.error("Account ID not found, unauthorized access.");
        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
    }

    //benefitSchemesService.deleteBenefitScheme(accountId, electionId, benefitSchemeIds);

    //return new ThedalResponse<>(ThedalSuccess.BENEFITSCHEME_DELETED, null);
    benefitSchemesService.deleteBenefitScheme(accountId, electionId, benefitSchemeIds);
    log.info("Returning success response for deletion of benefit schemes");
    return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.BENEFITSCHEME_DELETED));
    
}

    
    @PutMapping("/{electionId}/reorder")
    public ResponseEntity<ThedalResponse<String>> reorderBenefitSchemes(
            @PathVariable Long electionId,
            @RequestBody List<BenefitSchemeReorderRequest> reorderRequests) {
        try {
            Long accountId = requestDetails.getCurrentAccountId();
            if (accountId == null) {
                log.error("Account ID not found, unauthorized access.");
                throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
            }

            benefitSchemesService.updateBenefitSchemeOrder(reorderRequests, accountId, electionId);
            return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.BENEFITSCHEME_ORDER_UPDATED));

        } catch (Exception e) {
            log.error("Error reordering benefit schemes: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ThedalResponse<>(ThedalError.BENEFITSCHEME_ORDER_UPDATE_FAILED));
        }
    }

    @Operation(summary = "Get benefit schemes from MongoDB", description = "Fetch benefit schemes from MongoDB for a specific election")
    @GetMapping("/mongo/{electionId}")
    public ResponseEntity<ThedalResponse<List<Map<String, Object>>>> getAllBenefitSchemesFromMongo(@PathVariable Long electionId) {
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account ID not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }
        log.info("Fetching benefit schemes from MongoDB for electionId: {}", electionId);
        ThedalResponse<List<Map<String, Object>>> response = benefitSchemesService.getAllFromMongo(accountId, electionId);
        return ResponseEntity.ok(response);
    }
    
//    @PostMapping("/migrate-benefit-schemes")
//    public ThedalResponse<String> migrateVoterBenefitSchemes() {
//        try {
//            int migratedCount = benefitSchemesService.migrateVoterBenefitSchemes();
//            String message = String.format("Successfully migrated %d voter benefit scheme records for account", migratedCount);
//            return new ThedalResponse<>(ThedalSuccess.DATA_MIGRATED, message);
//        } catch (ThedalException ex) {
//            throw ex; // Propagate custom exceptions
//        } catch (Exception ex) {
//            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, 
//                "Failed to migrate voter benefit schemes: " + ex.getMessage());
//        }
//    }

}
