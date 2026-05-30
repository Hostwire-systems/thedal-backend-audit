package com.thedal.thedal_app.settings.electionsettings;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.thedal.thedal_app.general.RequestDetailsService;
import com.thedal.thedal_app.response.ThedalResponse;
import com.thedal.thedal_app.response.ThedalSuccess;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;

import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
@RequestMapping("/api/sections")
public class SectionController {

    @Autowired
    private SectionService sectionService;

    @Autowired
    private RequestDetailsService requestDetails;

    @PostMapping("/{electionId}")
    public ThedalResponse<SectionResponseDTO> createSection(
        @PathVariable Long electionId,  // This is for getting electionId from URL
        @RequestBody SectionDTO sectionDTO) {

            Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account ID not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }
        
        return sectionService.createSection(sectionDTO, electionId);
    }

    @GetMapping("/{electionId}")
    public ThedalResponse<List<SectionResponseDTO>> getAllSections(
    		 @PathVariable Long electionId) {
        Long accountId = requestDetails.getCurrentAccountId();
            if (accountId == null) {
                log.error("Account ID not found, unauthorized access.");
                throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
            }
        return sectionService.getAllSections(electionId, accountId);
    }

    @GetMapping("/{electionId}/{id}")
    public ThedalResponse<SectionResponseDTO> getSectionById(
    		@PathVariable Long electionId,
    		@PathVariable Long id) {
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account ID not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }
        return sectionService.getSectionById(electionId, accountId, id);
    }

    @PutMapping("/{electionId}/{id}")
    public ThedalResponse<SectionResponseDTO> updateSection(
    		 @PathVariable Long electionId,
             @PathVariable Long id,
             @RequestBody SectionDTO sectionDTO) {

    Long accountId = requestDetails.getCurrentAccountId();
     if (accountId == null) {
         log.error("Account ID not found, unauthorized access.");
         throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
    }

     return sectionService.updateSection(electionId, accountId, id, sectionDTO);
}
    
    
    @DeleteMapping("/{electionId}/{id}")
    public ThedalResponse<String> deleteSection(
    		@PathVariable Long electionId,
    		@PathVariable Long id) {

    Long accountId = requestDetails.getCurrentAccountId();
    if (accountId == null) {
        log.error("Account ID not found, unauthorized access.");
        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
    }
    sectionService.deleteSection(electionId, accountId, id);
    return new ThedalResponse<>(ThedalSuccess.SECTION_DELETED, "Section with ID " + id + " has been deleted.");
   }
    
    @DeleteMapping("/election/{electionId}")
    public ThedalResponse<String> deleteAllSections(
            @PathVariable Long electionId,
            @RequestParam(value = "sectionIds", required = false) List<Long> sectionIds) {

        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account ID not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        sectionService.deleteSections(electionId, accountId, sectionIds);
        return new ThedalResponse<>(ThedalSuccess.SECTION_DELETED);
    }
    
    
//   @Operation(summary = "Upload bulk Section Data", description = "Upload bulk Section data using xlsx or csv files.")
//   @PostMapping(value = "/election/{electionId}/section-upload", consumes = "multipart/form-data")
//   public ResponseEntity<ThedalResponse<Void>> uploadSectionsFromXlsxOrCsv(
//       @RequestParam("file") MultipartFile file, 
//       @PathVariable Long electionId) throws IOException {
//   
//       ThedalResponse<Void> response = sectionService.uploadSectionsFromXlsxOrCsv(file, electionId);
//       return ResponseEntity.ok(response);
//   }
    @Operation(summary = "Upload bulk Section Data", description = "Upload bulk Section data using xlsx or csv files.")
    @PostMapping(value = "/election/{electionId}/section-upload", consumes = "multipart/form-data")
    public ResponseEntity<ThedalResponse<SectionBulkUploadEntity>> uploadSectionsFromXlsxOrCsv(
        @RequestParam("file") MultipartFile file, 
        @PathVariable Long electionId) throws IOException {
        
        // Optional: Add file size validation like PartManager
        if (file.getSize() > 100 * 1024 * 1024) { // 100 MB
            throw new ThedalException(ThedalError.FILE_TOO_LARGE, HttpStatus.BAD_REQUEST);
        }

        ThedalResponse<SectionBulkUploadEntity> response = sectionService.uploadSectionsFromXlsxOrCsv(file, electionId);
        return ResponseEntity.ok(response);
    }
    
}
