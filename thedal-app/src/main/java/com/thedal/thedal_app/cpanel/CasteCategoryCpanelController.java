package com.thedal.thedal_app.cpanel;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.thedal.thedal_app.cpanel.dtos.CasteCategoryUpdateCpanelRequest;
import com.thedal.thedal_app.general.RequestDetailsService;
import com.thedal.thedal_app.response.ThedalResponse;
import com.thedal.thedal_app.response.ThedalSuccess;
import com.thedal.thedal_app.settings.electionsettings.CasteCategoryEntity;
import com.thedal.thedal_app.settings.electionsettings.dto.CasteCategoryRequest;
import com.thedal.thedal_app.settings.electionsettings.dto.CasteCategoryResponseDTO;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/cpanel")
//@Tag(name = "Sub Model Cpanel Controller")
@Slf4j
public class CasteCategoryCpanelController {

    @Autowired
    private CasteCategoryCpanelService casteCategoryCpanelService;

    @Autowired
    private RequestDetailsService requestDetails;

    @Operation(summary = "Create caste categories for the control panel", description = "Saves multiple caste categories for election ID 0.")
    @PostMapping("/caste-categories")
    public ThedalResponse<List<CasteCategoryEntity>> createMultipleCasteCategories(@RequestBody @Valid List<CasteCategoryRequest> casteCategoryRequests) {
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account ID not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }
        Long electionId = 0L;
        accountId = 0L;
        List<CasteCategoryEntity> casteCategoryEntities = casteCategoryCpanelService.createMultipleCasteCategories(casteCategoryRequests, electionId, accountId);
        return new ThedalResponse<>(ThedalSuccess.CASTE_CATEGORY_CREATED, casteCategoryEntities);
    }

    @Operation(summary = "Get caste categories for CPanel", description = "Fetch caste categories for the control panel (electionId is always 0).")
    @GetMapping("/caste-categories")
    public ThedalResponse<List<Map<String, Object>>> getCasteCategoriesForCpanel() {
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account ID not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }
        Long electionId = 0L;
        accountId = 0L;
        List<Map<String, Object>> casteCategoryDetails = casteCategoryCpanelService.getCasteCategoriesForCpanel(accountId, electionId);
        return new ThedalResponse<>(ThedalSuccess.CASTE_CATEGORIES_FETCHED, casteCategoryDetails);
    }

    @Operation(summary = "Delete caste category for CPanel", description = "Delete caste categories by IDs in the control panel (electionId is always 0).")
    @DeleteMapping("/caste-categories")
    public ThedalResponse<Void> deleteCasteCategory(
            @RequestParam(value = "casteCategoryIds", required = false) List<Long> casteCategoryIds) {
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account ID not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }
        Long electionId = 0L;
        accountId = 0L;
        casteCategoryCpanelService.deleteCasteCategory(accountId, electionId, casteCategoryIds);
        return new ThedalResponse<>(ThedalSuccess.CASTE_CATEGORY_DELETED, null);
    }

    @Operation(summary = "Update multiple caste categories", description = "Update multiple caste categories for the control panel (electionId is always 0).")
    @PutMapping("/caste-categories")
    public ThedalResponse<List<CasteCategoryResponseDTO>> updateMultipleCasteCategories(
            @RequestBody List<CasteCategoryUpdateCpanelRequest> casteCategoryUpdateRequests) {
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account ID not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }
        Long electionId = 0L;
        accountId = 0L;
        List<CasteCategoryResponseDTO> updatedCasteCategories = casteCategoryCpanelService.updateMultipleCasteCategories(accountId, electionId, casteCategoryUpdateRequests);
        return new ThedalResponse<>(ThedalSuccess.CASTE_CATEGORY_UPDATED, updatedCasteCategories);
    }
}