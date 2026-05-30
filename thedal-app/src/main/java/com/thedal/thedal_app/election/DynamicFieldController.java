package com.thedal.thedal_app.election;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

import com.thedal.thedal_app.election.dtos.DynamicFieldDTO;
import com.thedal.thedal_app.election.dtos.DynamicFieldReorderRequest;
import com.thedal.thedal_app.election.dtos.DynamicFieldResponseDTO;
import com.thedal.thedal_app.election.dtos.DynamicFieldStatusDTO;
import com.thedal.thedal_app.general.RequestDetailsService;
import com.thedal.thedal_app.response.ThedalResponse;
import com.thedal.thedal_app.response.ThedalSuccess;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/dynamic-fields")
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Dynamic Field Management")
public class DynamicFieldController {

    @Autowired
    private DynamicFieldService dynamicFieldService;

    @Autowired
    private RequestDetailsService requestDetails;

    @Operation(summary = "Create a new dynamic field",
               description = "Creates a dynamic field for a specific election with a maximum limit of 5 fields.")
    @PostMapping("/election/{electionId}")
    public ThedalResponse<DynamicFieldDTO> createDynamicField(
            @PathVariable("electionId") Long electionId,
            @Valid @RequestBody DynamicFieldDTO fieldDTO) {
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account id not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        DynamicFieldDTO createdField = dynamicFieldService.createDynamicField(accountId, electionId, fieldDTO);
        return new ThedalResponse<>(ThedalSuccess.DYNAMIC_FIELD_CREATED, createdField);
    }

    @Operation(summary = "Retrieve dynamic fields",
               description = "Fetches a paginated list of dynamic fields for a specific election.")
    @GetMapping("/election/{electionId}")
    public ThedalResponse<DynamicFieldResponseDTO> getDynamicFields(
            @PathVariable("electionId") Long electionId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sortBy", defaultValue = "orderIndex") String sortBy,
            @RequestParam(value = "order", defaultValue = "asc") String order) {

        if (size < 10 || size > 100) {
            log.error("Invalid size parameter: {}. Must be between 10 and 100", size);
            throw new ThedalException(ThedalError.INVALID_PAGE_SIZE, HttpStatus.BAD_REQUEST);
        }

        String orderLower = order.toLowerCase();
        if (!orderLower.equals("asc") && !orderLower.equals("desc")) {
            log.error("Invalid order parameter: {}. Must be 'asc' or 'desc' (case-insensitive)", order);
            throw new ThedalException(ThedalError.INVALID_SORT_FIELD, HttpStatus.BAD_REQUEST);
        }

        Sort.Direction direction = orderLower.equals("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, sortBy.equalsIgnoreCase("orderIndex") ? "orderIndex" : "createdTime");
        Pageable pageable = PageRequest.of(page, size, sort);

        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account id not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        DynamicFieldResponseDTO result = dynamicFieldService.getDynamicFields(accountId, electionId, pageable);
        return new ThedalResponse<>(ThedalSuccess.DYNAMIC_FIELD_FOUND, result);
    }

    @Operation(summary = "Update a dynamic field",
               description = "Updates the specified dynamic field with new attributes.")
    @PutMapping("/election/{electionId}/field/{fieldId}")
    public ThedalResponse<DynamicFieldDTO> updateDynamicField(
            @PathVariable("electionId") Long electionId,
            @PathVariable("fieldId") Long fieldId,
            @Valid @RequestBody DynamicFieldDTO fieldDTO) {
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account id not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        DynamicFieldDTO updatedField = dynamicFieldService.updateDynamicField(accountId, electionId, fieldId, fieldDTO);
        return new ThedalResponse<>(ThedalSuccess.DYNAMIC_FIELD_UPDATED, updatedField);
    }

    @Operation(summary = "Delete a dynamic field",
               description = "Deletes the specified dynamic field.")
    @DeleteMapping("/election/{electionId}/field/{fieldId}")
    public ThedalResponse<Void> deleteDynamicField(
            @PathVariable("electionId") Long electionId,
            @PathVariable("fieldId") Long fieldId) {
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account id not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        dynamicFieldService.deleteDynamicField(accountId, electionId, fieldId);
        return new ThedalResponse<>(ThedalSuccess.DYNAMIC_FIELD_DELETED, null);
    }

    @Operation(summary = "Delete dynamic fields",
               description = "Deletes specific dynamic fields or all fields for an election.")
    @DeleteMapping("/election/{electionId}/fields")
    public ThedalResponse<Void> deleteDynamicFields(
            @PathVariable("electionId") Long electionId,
            @RequestParam(value = "fieldIds", required = false) List<Long> fieldIds) {
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account id not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        dynamicFieldService.deleteDynamicFields(accountId, electionId, fieldIds);
        return new ThedalResponse<>(ThedalSuccess.DYNAMIC_FIELD_DELETED, null);
    }

    @Operation(summary = "Reorder dynamic fields",
               description = "Reorders dynamic fields for a specific election based on provided field IDs and new order indices.")
    @PutMapping("/{electionId}/reorder")
    public ResponseEntity<ThedalResponse<DynamicFieldResponseDTO>> reorderDynamicFields(
            @PathVariable Long electionId,
            @RequestBody List<DynamicFieldReorderRequest> reorderRequests) {
        try {
            Long accountId = requestDetails.getCurrentAccountId();
            if (accountId == null) {
                log.error("Account ID not found, unauthorized access.");
                throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
            }

            DynamicFieldResponseDTO updatedFields = dynamicFieldService.reorderDynamicFields(accountId, electionId, reorderRequests);
            return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.DYNAMIC_FIELD_ORDER_UPDATED, updatedFields));
        } catch (Exception e) {
            log.error("Error reordering dynamic fields: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ThedalResponse<>(ThedalError.DYNAMIC_FIELD_ORDER_UPDATE_FAILED));
        }
    }
    
    @Operation(summary = "Update dynamic field status",
            description = "Updates the status of the specified dynamic field.")
    @PutMapping("/election/{electionId}/field/{fieldId}/status")
    public ThedalResponse<DynamicFieldDTO> updateDynamicFieldStatus(
         @PathVariable("electionId") Long electionId,
         @PathVariable("fieldId") Long fieldId,
         @Valid @RequestBody DynamicFieldStatusDTO statusDTO) {
     Long accountId = requestDetails.getCurrentAccountId();
     if (accountId == null) {
         log.error("Account id not found, unauthorized access.");
         throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
     }

     DynamicFieldDTO updatedField = dynamicFieldService.updateDynamicFieldStatus(accountId, electionId, fieldId, statusDTO);
     return new ThedalResponse<>(ThedalSuccess.DYNAMIC_FIELD_UPDATED, updatedField);
 }

    @Operation(summary = "Enable all dynamic fields",
               description = "Sets status to true (enabled) for all dynamic fields in the election")
    @PutMapping("/election/{electionId}/enable-all")
    public ThedalResponse<String> enableAllFields(@PathVariable("electionId") Long electionId) {
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account id not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        try {
            int updatedCount = dynamicFieldService.enableAllFields(accountId, electionId);
            return new ThedalResponse<>(ThedalSuccess.SUCCESS, "Enabled " + updatedCount + " dynamic fields");
        } catch (Exception e) {
            log.error("Error enabling all dynamic fields for electionId={}: {}", electionId, e.getMessage());
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(summary = "Disable all dynamic fields",
               description = "Sets status to false (disabled) for all dynamic fields in the election")
    @PutMapping("/election/{electionId}/disable-all")
    public ThedalResponse<String> disableAllFields(@PathVariable("electionId") Long electionId) {
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account id not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        try {
            int updatedCount = dynamicFieldService.disableAllFields(accountId, electionId);
            return new ThedalResponse<>(ThedalSuccess.SUCCESS, "Disabled " + updatedCount + " dynamic fields");
        } catch (Exception e) {
            log.error("Error disabling all dynamic fields for electionId={}: {}", electionId, e.getMessage());
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(summary = "Make all dynamic fields required",
               description = "Sets mandatory to true (required) for all dynamic fields in the election")
    @PutMapping("/election/{electionId}/require-all")
    public ThedalResponse<String> requireAllFields(@PathVariable("electionId") Long electionId) {
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account id not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        try {
            int updatedCount = dynamicFieldService.requireAllFields(accountId, electionId);
            return new ThedalResponse<>(ThedalSuccess.SUCCESS, "Made " + updatedCount + " dynamic fields required");
        } catch (Exception e) {
            log.error("Error making all dynamic fields required for electionId={}: {}", electionId, e.getMessage());
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(summary = "Make all dynamic fields optional",
               description = "Sets mandatory to false (optional) for all dynamic fields in the election")
    @PutMapping("/election/{electionId}/optional-all")
    public ThedalResponse<String> optionalAllFields(@PathVariable("electionId") Long electionId) {
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account id not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        try {
            int updatedCount = dynamicFieldService.optionalAllFields(accountId, electionId);
            return new ThedalResponse<>(ThedalSuccess.SUCCESS, "Made " + updatedCount + " dynamic fields optional");
        } catch (Exception e) {
            log.error("Error making all dynamic fields optional for electionId={}: {}", electionId, e.getMessage());
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    
    
    
}