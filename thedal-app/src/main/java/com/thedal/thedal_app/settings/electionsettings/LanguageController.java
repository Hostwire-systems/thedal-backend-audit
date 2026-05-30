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
import org.springframework.web.bind.annotation.RestController;

import com.thedal.thedal_app.general.RequestDetailsService;
import com.thedal.thedal_app.response.ThedalResponse;
import com.thedal.thedal_app.response.ThedalSuccess;
import com.thedal.thedal_app.settings.electionsettings.dto.LanguageDTO;
import com.thedal.thedal_app.settings.electionsettings.dto.LanguageReorderRequest;
import com.thedal.thedal_app.settings.electionsettings.dto.LanguageResponseDTO;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;

import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/language")
@Slf4j
public class LanguageController {

     @Autowired
    private RequestDetailsService requestDetails;

    @Autowired
    private LanguageService languageService;

    @PostMapping("/{electionId}")
    public ThedalResponse<LanguageResponseDTO> createLanguage(@RequestBody LanguageDTO languageDTO,@PathVariable Long electionId) {
        
        ThedalResponse<LanguageResponseDTO> language =languageService.createLanguage(languageDTO,electionId); 
        new ThedalResponse<>(ThedalSuccess.LANGUAGE_CREATED, language);
		return language;
    }

//    @GetMapping("/{electionId}")
//    public ThedalResponse<List<LanguageResponse>>getLanguage(@PathVariable Long electionId) {
//    Long accountId = requestDetails.getCurrentAccountId();
//
//    if (accountId == null) {
//        log.error("Account ID not found, unauthorized access.");
//        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
//    }
//
//    List<LanguageResponse> languageDTO = languageService.getLanguage(accountId, electionId);
//    return new ThedalResponse<>(ThedalSuccess.LANGUAGE_FOUND, languageDTO);
//    }
    //@Operation(summary = "Get languages", description = "Fetch languages with voter counts for a specific election", tags = {"Election Settings"})
    @GetMapping("/{electionId}")
    public ThedalResponse<List<Map<String, Object>>> getLanguage(@PathVariable Long electionId) {
        Long accountId = requestDetails.getCurrentAccountId();

        if (accountId == null) {
            log.error("Account ID not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        log.info("Fetching languages from PostgreSQL for electionId: {}", electionId);
        // Use PostgreSQL service for consistency
        return languageService.getLanguage(accountId, electionId);
    }
    
    
    @PutMapping("{electionId}/{languageId}")
    public ThedalResponse<LanguageResponseDTO> updateLanguage(@PathVariable Long electionId, @PathVariable Long languageId, @RequestBody LanguageDTO languageDTO) {
    Long accountId = requestDetails.getCurrentAccountId();

    if (accountId == null) {
        log.error("Account ID not found, unauthorized access.");
        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
    }            

    LanguageResponseDTO updatedLanguage = languageService.updateLanguage(accountId, electionId, languageId, languageDTO);
    return new ThedalResponse<>(ThedalSuccess.LANGUAGE_UPDATED, updatedLanguage);
    }
    
//    @DeleteMapping("{electionId}/languages")
//    public ThedalResponse<Void> deleteLanguage(
//            @PathVariable Long electionId,
//            @RequestParam(value = "languageIds", required = false) List<Long> languageIds) {
//    
//        Long accountId = requestDetails.getCurrentAccountId();
//        if (accountId == null) {
//            log.error("Account ID not found, unauthorized access.");
//            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
//        }
//    
//        languageService.deleteLanguage(accountId, electionId, languageIds);
//        return new ThedalResponse<>(ThedalSuccess.LANGUAGE_DELETED, null);
//    }
    
    @DeleteMapping("/{electionId}/languages")
    public ThedalResponse<Map<String, Object>> deleteLanguage(
            @PathVariable Long electionId,
            @RequestParam(value = "languageIds", required = false) List<Long> languageIds) {
        
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account ID not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        return languageService.deleteLanguage(accountId, electionId, languageIds);
    }

    @PutMapping("/{electionId}/reorder")
    public ResponseEntity<ThedalResponse<String>> reorderLanguages(
            @PathVariable Long electionId,
            @RequestBody List<LanguageReorderRequest> reorderRequests) {
        try {
            Long accountId = requestDetails.getCurrentAccountId();
            if (accountId == null) {
                log.error("Account ID not found, unauthorized access.");
                throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
            }

            languageService.updateLanguageOrder(reorderRequests, accountId, electionId);
            return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.LANGUAGE_ORDER_UPDATED));

        } catch (Exception e) {
            log.error("Error reordering languages: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ThedalResponse<>(ThedalError.LANGUAGE_ORDER_UPDATE_FAILED));
        }
    }
    
    @Operation(summary = "Get languages from MongoDB", description = "Fetch languages from MongoDB for a specific election")
    @GetMapping("/mongo/{electionId}")
    public ThedalResponse<List<Map<String, Object>>> getLanguageFromMongo(@PathVariable Long electionId) {
        Long accountId = requestDetails.getCurrentAccountId();

        if (accountId == null) {
            log.error("Account ID not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        log.info("Fetching languages from MongoDB for electionId: {}", electionId);
        return languageService.getLanguageFromMongo(accountId, electionId);
    }

    
}
