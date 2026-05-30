//package com.thedal.thedal_app.settings.electionsettings;
//
//
//import java.util.List;
//import java.util.Map;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.HttpStatus;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.PutMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import com.thedal.thedal_app.general.RequestDetailsService;
//import com.thedal.thedal_app.response.ThedalResponse;
//import com.thedal.thedal_app.settings.electionsettings.dto.LanguageDTO;
//import com.thedal.thedal_app.settings.electionsettings.dto.LanguageResponseDTO;
//import com.thedal.thedal_app.thedal_exception.ThedalError;
//import com.thedal.thedal_app.thedal_exception.ThedalException;
//
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import lombok.extern.slf4j.Slf4j;
//
//@RestController
//@RequestMapping("/api/language/mongo")
//@Slf4j
//@Tag(name = "MongoDB Language Management", description = "APIs for managing languages in MongoDB")
//public class LanguageMongoController {
//
//    @Autowired
//    private LanguageMongoService languageMongoService;
//
//    @Autowired
//    private RequestDetailsService requestDetails;
//
//    @Operation(summary = "Create a language in MongoDB and PostgreSQL", 
//            description = "Creates a new language for the specified election, saving to both databases")
//    @PostMapping("/{electionId}")
//    public ThedalResponse<LanguageResponseDTO> createLanguage(
//            @RequestBody LanguageDTO languageDTO,
//            @PathVariable Long electionId) {
//        Long accountId = requestDetails.getCurrentUserId();
//        if (accountId == null) {
//            log.error("Account ID not found, unauthorized access.");
//            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
//        }
//
//        log.info("Creating language for electionId: {}", electionId);
//        return languageMongoService.createLanguage(languageDTO, electionId);
//    }
//
//    @Operation(summary = "Update a language in MongoDB and PostgreSQL", 
//            description = "Updates an existing language for the specified election in both databases")
//    @PutMapping("/{electionId}/{languageId}")
//    public ThedalResponse<LanguageResponseDTO> updateLanguage(
//            @PathVariable Long electionId,
//            @PathVariable Long languageId,
//            @RequestBody LanguageDTO languageDTO) {
//    	Long accountId = requestDetails.getCurrentUserId();
//        if (accountId == null) {
//            log.error("Account ID not found, unauthorized access.");
//            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
//        }
//
//        log.info("Updating languageId: {} for electionId: {}", languageId, electionId);
//        return languageMongoService.updateLanguage(accountId, electionId, languageId, languageDTO);
//    }
//
//    @Operation(summary = "Get languages from MongoDB", 
//            description = "Fetches languages from MongoDB for a specific election")
//    @GetMapping("/{electionId}")
//    public ThedalResponse<List<Map<String, Object>>> getLanguages(
//            @PathVariable Long electionId) {
//    	Long accountId = requestDetails.getCurrentUserId();
//        if (accountId == null) {
//            log.error("Account ID not found, unauthorized access.");
//            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
//        }
//
//        log.info("Fetching languages from MongoDB for electionId: {}", electionId);
//        return languageMongoService.getLanguages(accountId, electionId);
//    }
//}