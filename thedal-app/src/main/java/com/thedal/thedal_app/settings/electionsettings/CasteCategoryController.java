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
import com.thedal.thedal_app.settings.electionsettings.dto.CasteCategoryReorderRequest;
import com.thedal.thedal_app.settings.electionsettings.dto.CasteCategoryRequest;
import com.thedal.thedal_app.settings.electionsettings.dto.CasteCategoryResponseDTO;
import com.thedal.thedal_app.settings.electionsettings.dto.CasteCategoryUpdateRequest;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/election-settings")
//@Tag(name = "Election Settings")
@Slf4j
public class CasteCategoryController {

    @Autowired
    private CasteCategoryService casteCategoryService;

    @Autowired
    private RequestDetailsService requestDetails;

    @Operation(summary = "Create a new caste category", description = "Saves a new caste category")
    @PostMapping("/caste-categories/{electionId}")
    public ThedalResponse<CasteCategoryEntity> createCasteCategory(
            @PathVariable Long electionId,
            @RequestBody @Valid CasteCategoryRequest casteCategoryRequest) {
        CasteCategoryEntity casteCategoryEntity = casteCategoryService.createCasteCategory(casteCategoryRequest, electionId);
        return new ThedalResponse<>(ThedalSuccess.CASTE_CATEGORY_CREATED, casteCategoryEntity);
    }

    @Operation(summary = "Get caste categories", description = "Fetch caste categories with voter counts for a specific election")
    @GetMapping("/caste-categories/{electionId}")
    public ThedalResponse<List<Map<String, Object>>> getCasteCategories(
            @PathVariable Long electionId) {
        log.info("Fetching caste categories for electionId: {}", electionId);
        return casteCategoryService.getCasteCategories(electionId);
    }

    @Operation(summary = "Delete caste category", description = "Delete caste categories by IDs and election")
    @DeleteMapping("/caste-categories/{electionId}")
    public ThedalResponse<Void> deleteCasteCategory(
            @PathVariable("electionId") Long electionId,
            @RequestParam(value = "casteCategoryIds", required = false) List<Long> casteCategoryIds) {
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account ID not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }
        log.info("Deleting caste categories {} for accountId: {} and electionId: {}", casteCategoryIds, accountId, electionId);
        casteCategoryService.deleteCasteCategoryByIdAndAccountId(casteCategoryIds, accountId, electionId);
        return new ThedalResponse<>(ThedalSuccess.CASTE_CATEGORY_DELETED, null);
    }

    @Operation(summary = "Update caste category", description = "Update an existing caste category for a specific election")
    @PutMapping("/caste-categories/{electionId}/{casteCategoryId}")
    public ThedalResponse<CasteCategoryResponseDTO> updateCasteCategory(
            @PathVariable("electionId") Long electionId,
            @PathVariable("casteCategoryId") Long casteCategoryId,
            @RequestBody CasteCategoryUpdateRequest request) {
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account ID not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }
        CasteCategoryResponseDTO updatedCasteCategory = casteCategoryService.updateCasteCategory(casteCategoryId, accountId, request, electionId);
        return new ThedalResponse<>(ThedalSuccess.CASTE_CATEGORY_UPDATED, updatedCasteCategory);
    }

    @Operation(summary = "Reorder caste categories", description = "Update multiple caste categories with new order indices")
    @PutMapping("/caste-categories/reorder/{electionId}")
    public ResponseEntity<ThedalResponse<String>> updateCasteCategoryOrder(
            @PathVariable Long electionId,
            @RequestBody List<CasteCategoryReorderRequest> reorderRequests) {
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }
        casteCategoryService.updateCasteCategoryOrder(reorderRequests, electionId, accountId);
        return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.CASTE_CATEGORY_REORDERING));
    }
}