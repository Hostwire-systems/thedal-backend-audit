package com.thedal.thedal_app.familycaptain;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
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

import com.thedal.thedal_app.familycaptain.dto.FamilyCaptainDetailsDTO;
import com.thedal.thedal_app.familycaptain.dto.FamilyCaptainDetailsUpdate;
import com.thedal.thedal_app.familycaptain.dto.FamilyCaptainUploadSummary;
import com.thedal.thedal_app.familycaptain.dto.FamilyDetailsDTO;
import com.thedal.thedal_app.familycaptain.dto.FamilyUpdateRequest;
import com.thedal.thedal_app.familycaptain.dto.SaveFamilyCaptainDetailsDTO;
import com.thedal.thedal_app.response.ThedalResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("api/v1/family-captains")
@Slf4j
@Tag(name = "Family Captain Management")
public class FamilyCaptainController {

    @Autowired
    private FamilyCaptainService familyCaptainService;

    @Operation(summary = "Create a new family captain", 
               description = "Creates a new family captain with assigned families for the specified election", 
               tags = {"Family Captain Management"})
    @PostMapping("/election/{electionId}")
    public ThedalResponse<Void> saveFamilyCaptain(
            @PathVariable("electionId") Long electionId,
            @RequestBody @Valid SaveFamilyCaptainDetailsDTO familyCaptainDto) {
        
        log.info("Creating family captain for election {} with {} assigned families", 
                electionId, familyCaptainDto.getAssignedFamilies() != null ? familyCaptainDto.getAssignedFamilies().size() : 0);
        
        return familyCaptainService.saveFamilyCaptain(familyCaptainDto, electionId);
    }

    @Operation(summary = "Get family captain details", 
               description = "Retrieves family captain details by user ID and election ID", 
               tags = {"Family Captain Management"})
    @GetMapping("/election/{electionId}/user/{userId}")
    public ThedalResponse<FamilyCaptainDetailsDTO> getFamilyCaptain(
            @PathVariable("electionId") Long electionId,
            @PathVariable("userId") Long userId) {
        
        return familyCaptainService.getFamilyCaptainByUserId(userId, electionId);
    }

    @Operation(summary = "Update family captain details", 
               description = "Updates family captain information (excluding family assignments)", 
               tags = {"Family Captain Management"})
    @PutMapping("/election/{electionId}/user/{userId}")
    public ThedalResponse<Void> updateFamilyCaptain(
            @PathVariable("electionId") Long electionId,
            @PathVariable("userId") Long userId,
            @RequestBody @Valid FamilyCaptainDetailsUpdate update) {
        
        return familyCaptainService.updateFamilyCaptain(userId, electionId, update);
    }

    @Operation(summary = "Delete family captain", 
               description = "Deletes a family captain from the specified election", 
               tags = {"Family Captain Management"})
    @DeleteMapping("/election/{electionId}/user/{userId}")
    public ThedalResponse<Void> deleteFamilyCaptain(
            @PathVariable("electionId") Long electionId,
            @PathVariable("userId") Long userId) {
        
        return familyCaptainService.deleteFamilyCaptain(userId, electionId);
    }

    @Operation(summary = "Delete multiple family captains", 
               description = "Deletes multiple family captains from the specified election", 
               tags = {"Family Captain Management"})
    @DeleteMapping("/election/{electionId}")
    public ThedalResponse<Void> deleteFamilyCaptains(
            @PathVariable("electionId") Long electionId,
            @RequestParam("userIds") List<Long> userIds) {
        
        return familyCaptainService.deleteFamilyCaptains(electionId, userIds);
    }

    @Operation(summary = "Update assigned families", 
               description = "Updates the families assigned to a specific family captain", 
               tags = {"Family Captain Management"})
    @PutMapping("/election/{electionId}/user/{userId}/families")
    public ThedalResponse<Void> updateAssignedFamilies(
            @PathVariable("electionId") Long electionId,
            @PathVariable("userId") Long userId,
            @RequestBody @Valid FamilyUpdateRequest familyUpdateRequest) {
        
        return familyCaptainService.updateAssignedFamilies(electionId, userId, familyUpdateRequest);
    }

    @Operation(summary = "Search family captains with filters", 
               description = "Retrieves family captains with optional filters for assigned families, mobile number, and search term", 
               tags = {"Family Captain Management"})
    @GetMapping("/election/{electionId}")
    public ThedalResponse<Page<FamilyCaptainDetailsDTO>> getFamilyCaptains(
            @PathVariable("electionId") Long electionId,
            @RequestParam(value = "assignedFamilies", required = false) List<UUID> assignedFamilies,
            @RequestParam(value = "mobileNumber", required = false) String mobileNumber,
            @RequestParam(value = "searchTerm", required = false) String searchTerm,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sortBy", defaultValue = "firstName") String sortBy,
            @RequestParam(value = "direction", defaultValue = "asc") String direction) {
        
        return familyCaptainService.getFamilyCaptainsByAssignedFamiliesAndMobileNumber(
                electionId, assignedFamilies, mobileNumber, searchTerm, page, size, sortBy, direction);
    }

    @Operation(summary = "Bulk upload family captains", 
               description = "Uploads multiple family captains from Excel or CSV file", 
               tags = {"Family Captain Management"})
    @PostMapping("/election/{electionId}/upload")
    public ResponseEntity<ThedalResponse<FamilyCaptainUploadSummary>> uploadFamilyCaptains(
            @PathVariable("electionId") Long electionId,
            @RequestParam("file") MultipartFile file) {
        
        // Check file size (100 MB limit)
        if (file.getSize() > 100 * 1024 * 1024) {
            throw new RuntimeException("File too large. Maximum size is 100 MB.");
        }
        
        ThedalResponse<FamilyCaptainUploadSummary> response = familyCaptainService.uploadFamilyCaptainsFromXlsxOrCsv(file, electionId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get family captains by assigned family", 
               description = "Retrieves all family captains assigned to a specific family", 
               tags = {"Family Captain Management"})
    @GetMapping("/election/{electionId}/family/{familyId}")
    public ThedalResponse<List<FamilyCaptainDetailsDTO>> getFamilyCaptainsByFamily(
            @PathVariable("electionId") Long electionId,
            @PathVariable("familyId") UUID familyId) {
        
        return familyCaptainService.getFamilyCaptainsByAssignedFamily(familyId, electionId);
    }

    @Operation(summary = "Get family options for dropdown", 
               description = "Retrieves available families for family captain assignment dropdown", 
               tags = {"Family Captain Management"})
    @GetMapping("/election/{electionId}/family-options")
    public ResponseEntity<ThedalResponse<Page<FamilyDetailsDTO>>> getFamilyOptions(
            @PathVariable("electionId") Long electionId,
            @RequestParam(value = "searchTerm", required = false) String searchTerm,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        
        try {
            ThedalResponse<Page<FamilyDetailsDTO>> response = familyCaptainService.getFamilyOptions(electionId, searchTerm, page, size);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ThedalResponse<>("Error retrieving family options: " + e.getMessage(), null, false));
        }
    }
}
