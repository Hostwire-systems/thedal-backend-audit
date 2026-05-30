package com.thedal.thedal_app.election;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.thedal.thedal_app.general.RequestDetailsService;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;
import com.thedal.thedal_app.response.ThedalResponse;
import com.thedal.thedal_app.response.ThedalSuccess;
import com.thedal.thedal_app.election.dtos.StaticFieldStatusDTO;
import com.thedal.thedal_app.election.dtos.UpdateStaticFieldStatusRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/elections/{electionId}/static-fields")
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Static Field Management", description = "APIs for managing static voter field on/off status per election")
public class StaticFieldStatusController {

    @Autowired
    private StaticFieldStatusService staticFieldStatusService;
    
    @Autowired
    private RequestDetailsService requestDetails;

    @Operation(
        summary = "Initialize default static fields for election",
        description = "Creates default static field status entries for an election. All fields are enabled by default."
    )
    @PostMapping("/initialize")
    public ThedalResponse<String> initializeDefaultStaticFields(@PathVariable("electionId") Long electionId) {
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account id not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        try {
            staticFieldStatusService.initializeDefaultStaticFields(accountId, electionId);
            return new ThedalResponse<>(ThedalSuccess.SUCCESS, "Default static fields initialized successfully");
        } catch (Exception e) {
            log.error("Error initializing default static fields for electionId={}: {}", electionId, e.getMessage());
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
        summary = "Get all static field statuses for election",
        description = "Retrieves all static field statuses for a specific election with optional mandatory filter. Returns default enabled status if not configured."
    )
    @GetMapping("")
    public ThedalResponse<List<StaticFieldStatusDTO>> getStaticFieldStatuses(
            @PathVariable("electionId") Long electionId,
            @RequestParam(value = "mandatory", required = false) Boolean mandatory) {
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account id not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        try {
            List<StaticFieldStatusDTO> statuses = staticFieldStatusService.getFieldStatuses(accountId, electionId, mandatory);
            return new ThedalResponse<>(ThedalSuccess.SUCCESS, statuses);
        } catch (Exception e) {
            log.error("Error getting static field statuses for electionId={}: {}", electionId, e.getMessage());
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
        summary = "Get static field statuses by category",
        description = "Retrieves static field statuses grouped by category (basic, contact, address, etc.)"
    )
    @GetMapping("/by-category")
    public ThedalResponse<Map<String, List<StaticFieldStatusDTO>>> getStaticFieldStatusesByCategory(
            @PathVariable("electionId") Long electionId) {
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account id not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        try {
            Map<String, List<StaticFieldStatusDTO>> statusesByCategory = 
                staticFieldStatusService.getFieldStatusesByCategory(accountId, electionId);
            return new ThedalResponse<>(ThedalSuccess.SUCCESS, statusesByCategory);
        } catch (Exception e) {
            log.error("Error getting static field statuses by category for electionId={}: {}", electionId, e.getMessage());
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
        summary = "Get enabled static field names",
        description = "Retrieves list of field names that are currently enabled for the election"
    )
    @GetMapping("/enabled")
    public ThedalResponse<List<String>> getEnabledFieldNames(@PathVariable("electionId") Long electionId) {
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account id not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        try {
            List<String> enabledFields = staticFieldStatusService.getEnabledFieldNames(accountId, electionId);
            return new ThedalResponse<>(ThedalSuccess.SUCCESS, enabledFields);
        } catch (Exception e) {
            log.error("Error getting enabled field names for electionId={}: {}", electionId, e.getMessage());
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
        summary = "Check if specific field is enabled",
        description = "Checks if a specific static field is enabled for the election"
    )
    @GetMapping("/field/{fieldName}/status")
    public ThedalResponse<Boolean> isFieldEnabled(
            @PathVariable("electionId") Long electionId,
            @PathVariable("fieldName") String fieldName) {
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account id not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        try {
            Boolean isEnabled = staticFieldStatusService.isFieldEnabled(accountId, electionId, fieldName);
            return new ThedalResponse<>(ThedalSuccess.SUCCESS, isEnabled);
        } catch (Exception e) {
            log.error("Error checking field status for electionId={}, fieldName={}: {}", electionId, fieldName, e.getMessage());
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
        summary = "Update static field status",
        description = "Updates the status (enabled/disabled) for a specific static field"
    )
    @PutMapping("/field/{fieldName}/status")
    public ThedalResponse<String> updateFieldStatus(
            @PathVariable("electionId") Long electionId,
            @PathVariable("fieldName") String fieldName,
            @RequestParam("status") Boolean status) {
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account id not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        try {
            staticFieldStatusService.updateFieldStatus(accountId, electionId, fieldName, status);
            return new ThedalResponse<>(ThedalSuccess.SUCCESS, "Field status updated successfully");
        } catch (Exception e) {
            log.error("Error updating field status for electionId={}, fieldName={}: {}", 
                    electionId, fieldName, e.getMessage());
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
        summary = "Update static field mandatory status",
        description = "Updates the mandatory (required/optional) status for a specific static field"
    )
    @PutMapping("/field/{fieldName}/mandatory")
    public ThedalResponse<String> updateFieldMandatory(
            @PathVariable("electionId") Long electionId,
            @PathVariable("fieldName") String fieldName,
            @RequestParam("mandatory") Boolean mandatory) {
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account id not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        try {
            staticFieldStatusService.updateFieldMandatory(accountId, electionId, fieldName, mandatory);
            return new ThedalResponse<>(ThedalSuccess.SUCCESS, "Field mandatory status updated successfully");
        } catch (Exception e) {
            log.error("Error updating field mandatory status for electionId={}, fieldName={}: {}", 
                    electionId, fieldName, e.getMessage());
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
        summary = "Bulk update static field statuses",
        description = "Updates multiple static field statuses in a single request"
    )
    @PutMapping("/bulk-update")
    public ThedalResponse<String> updateFieldStatuses(
            @PathVariable("electionId") Long electionId,
            @RequestBody @Valid UpdateStaticFieldStatusRequest request) {
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account id not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        try {
            staticFieldStatusService.updateFieldStatuses(accountId, electionId, request.getFieldStatuses());
            return new ThedalResponse<>(ThedalSuccess.SUCCESS, "Field statuses updated successfully");
        } catch (Exception e) {
            log.error("Error bulk updating field statuses for electionId={}: {}", electionId, e.getMessage());
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
        summary = "Initialize static fields for all existing elections",
        description = "Batch initialize static field statuses for all existing elections that don't have them configured yet. All fields are enabled by default."
    )
    @PostMapping("/initialize-all-elections")
    public ThedalResponse<String> initializeAllExistingElections() {
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account id not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        try {
            int initializedCount = staticFieldStatusService.initializeAllExistingElections(accountId);
            return new ThedalResponse<>(ThedalSuccess.SUCCESS, "Static fields initialized for " + initializedCount + " existing elections");
        } catch (Exception e) {
            log.error("Error initializing static fields for all elections: {}", e.getMessage());
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
        summary = "Reconcile missing static fields for this election",
        description = "Adds any missing static fields from the complete definition list to this specific election. Useful for updating elections to include newly added field types."
    )
    @PostMapping("/reconcile-missing-fields")
    public ThedalResponse<String> reconcileMissingFields(@PathVariable("electionId") Long electionId) {
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account id not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        try {
            int addedCount = staticFieldStatusService.reconcileMissingFieldsForElection(accountId, electionId);
            return new ThedalResponse<>(ThedalSuccess.SUCCESS, "Added " + addedCount + " missing static fields to election " + electionId);
        } catch (Exception e) {
            log.error("Error reconciling missing fields for electionId={}: {}", electionId, e.getMessage());
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
        summary = "Reconcile missing static fields for all elections",
        description = "Adds any missing static fields from the complete definition list to all elections in this account. Useful for ensuring all elections have the latest field definitions."
    )
    @PostMapping("/reconcile-all-elections")
    public ThedalResponse<String> reconcileAllElections() {
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account id not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        try {
            int totalAddedFields = staticFieldStatusService.reconcileMissingFieldsForAllElections(accountId);
            return new ThedalResponse<>(ThedalSuccess.SUCCESS, "Added " + totalAddedFields + " missing static fields across all elections");
        } catch (Exception e) {
            log.error("Error reconciling missing fields for all elections: {}", e.getMessage());
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
        summary = "Enable all static fields",
        description = "Sets status to true (enabled) for all static fields in the election"
    )
    @PutMapping("/enable-all")
    public ThedalResponse<String> enableAllFields(@PathVariable("electionId") Long electionId) {
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account id not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        try {
            int updatedCount = staticFieldStatusService.enableAllFields(accountId, electionId);
            return new ThedalResponse<>(ThedalSuccess.SUCCESS, "Enabled " + updatedCount + " static fields");
        } catch (Exception e) {
            log.error("Error enabling all static fields for electionId={}: {}", electionId, e.getMessage());
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
        summary = "Disable all static fields",
        description = "Sets status to false (disabled) for all static fields in the election"
    )
    @PutMapping("/disable-all")
    public ThedalResponse<String> disableAllFields(@PathVariable("electionId") Long electionId) {
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account id not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        try {
            int updatedCount = staticFieldStatusService.disableAllFields(accountId, electionId);
            return new ThedalResponse<>(ThedalSuccess.SUCCESS, "Disabled " + updatedCount + " static fields");
        } catch (Exception e) {
            log.error("Error disabling all static fields for electionId={}: {}", electionId, e.getMessage());
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
        summary = "Make all static fields required",
        description = "Sets mandatory to true (required) for all static fields in the election"
    )
    @PutMapping("/require-all")
    public ThedalResponse<String> requireAllFields(@PathVariable("electionId") Long electionId) {
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account id not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        try {
            int updatedCount = staticFieldStatusService.requireAllFields(accountId, electionId);
            return new ThedalResponse<>(ThedalSuccess.SUCCESS, "Made " + updatedCount + " static fields required");
        } catch (Exception e) {
            log.error("Error making all static fields required for electionId={}: {}", electionId, e.getMessage());
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
        summary = "Make all static fields optional",
        description = "Sets mandatory to false (optional) for all static fields in the election"
    )
    @PutMapping("/optional-all")
    public ThedalResponse<String> optionalAllFields(@PathVariable("electionId") Long electionId) {
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account id not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        try {
            int updatedCount = staticFieldStatusService.optionalAllFields(accountId, electionId);
            return new ThedalResponse<>(ThedalSuccess.SUCCESS, "Made " + updatedCount + " static fields optional");
        } catch (Exception e) {
            log.error("Error making all static fields optional for electionId={}: {}", electionId, e.getMessage());
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}

/**
 * Admin Controller for Static Field Management
 * Separate controller for admin-level operations not tied to specific elections
 */
@RestController
@RequestMapping("/api/admin/static-fields")
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Static Field Admin", description = "Admin APIs for managing static field configurations across all elections")
class StaticFieldAdminController {

    @Autowired
    private StaticFieldStatusService staticFieldStatusService;
    
    @Autowired
    private RequestDetailsService requestDetails;

    @Operation(
        summary = "Initialize static fields for all existing elections (Admin)",
        description = "Admin endpoint to batch initialize static field statuses for all existing elections across the account. All fields are enabled by default."
    )
    @PostMapping("/initialize-all-elections")
    public ThedalResponse<String> adminInitializeAllExistingElections() {
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account id not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        try {
            int initializedCount = staticFieldStatusService.initializeAllExistingElections(accountId);
            return new ThedalResponse<>(ThedalSuccess.SUCCESS, "Successfully initialized static fields for " + initializedCount + " existing elections. All static fields are now enabled by default for existing elections.");
        } catch (Exception e) {
            log.error("Error initializing static fields for all elections: {}", e.getMessage());
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}