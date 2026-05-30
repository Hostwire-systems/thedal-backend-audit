package com.thedal.thedal_app.election;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.thedal.thedal_app.election.dtos.DynamicFieldDTO;
import com.thedal.thedal_app.election.dtos.DynamicFieldReorderRequest;
import com.thedal.thedal_app.election.dtos.DynamicFieldResponseDTO;
import com.thedal.thedal_app.election.dtos.DynamicFieldStatusDTO;
import com.thedal.thedal_app.general.RequestDetailsService;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class DynamicFieldService {

    @Autowired
    private DynamicFieldRepository dynamicFieldRepository;

    @Autowired
    private ElectionRepository electionRepository;

    @Autowired
    private RequestDetailsService requestDetails;

    private void validateElectionOwnership(Long electionId, Long accountId) {
        electionRepository.findByIdAndAccountId(electionId, accountId)
                .orElseThrow(() -> new ThedalException(ThedalError.INVALID_ELECTION, HttpStatus.FORBIDDEN));
    }

    private void validateField(@Valid DynamicFieldDTO fieldDTO, boolean isUpdate) {
        List<String> allowedTypes = List.of("string", "number", "boolean", "dropdown", "radio", "check-box", "multi-select", "image", "file");
        if (!allowedTypes.contains(fieldDTO.getType())) {
            log.error("Invalid field type: {}. Allowed types are {}", fieldDTO.getType(), allowedTypes);
            throw new ThedalException(ThedalError.INVALID_FORM_FIELDS, HttpStatus.BAD_REQUEST);
        }

        if (List.of("dropdown", "radio", "check-box", "multi-select").contains(fieldDTO.getType())) {
            if (fieldDTO.getOptions() == null || fieldDTO.getOptions().isEmpty()) {
                log.error("Missing or invalid options for field type {} in field: {}", fieldDTO.getType(), fieldDTO.getLabel());
                throw new ThedalException(ThedalError.INVALID_FORM_FIELDS, HttpStatus.BAD_REQUEST);
            }
            for (String option : fieldDTO.getOptions()) {
                if (option == null || option.isBlank()) {
                    log.error("Invalid option in field: {}. Options must be non-empty strings", fieldDTO.getLabel());
                    throw new ThedalException(ThedalError.INVALID_FORM_FIELDS, HttpStatus.BAD_REQUEST);
                }
            }
        }

        if (List.of("string", "number", "boolean", "image", "file").contains(fieldDTO.getType())) {
            if (fieldDTO.getOptions() != null && !fieldDTO.getOptions().isEmpty()) {
                log.warn("Options provided for field type {} in field: {}. Ignoring options.", fieldDTO.getType(), fieldDTO.getLabel());
                fieldDTO.setOptions(null); // Clear options for non-option-based types
            }
        } else if (isUpdate && fieldDTO.getOptions() == null) {
            // For updates, allow options to be null for option-based types; will be handled in update logic
            log.debug("Options not provided for field type {} in update for field: {}. Will retain existing options if applicable.", fieldDTO.getType(), fieldDTO.getLabel());
        }
        
     // Validate name field
        if (fieldDTO.getName() == null || fieldDTO.getName().isBlank()) {
            log.error("Name is mandatory for field: {}", fieldDTO.getLabel());
            throw new ThedalException(ThedalError.INVALID_FORM_FIELDS, HttpStatus.BAD_REQUEST);
        }
    }

    @Transactional
    public DynamicFieldDTO createDynamicField(Long accountId, Long electionId, DynamicFieldDTO fieldDTO) {
        log.debug("Creating dynamic field for accountId={}, electionId={}", accountId, electionId);

        validateElectionOwnership(electionId, accountId);
        validateField(fieldDTO, false);

        int fieldCount = dynamicFieldRepository.countByElectionIdAndAccountId(electionId, accountId);
        if (fieldCount >= 5) {
            log.error("Maximum of 5 dynamic fields allowed for electionId={}", electionId);
            throw new ThedalException(ThedalError.MAX_FIELDS_EXCEEDED, HttpStatus.BAD_REQUEST);
        }

        Integer maxOrderIndex = dynamicFieldRepository.findMaxOrderIndexByElectionIdAndAccountId(electionId, accountId);
        int newOrderIndex = maxOrderIndex + 1;

        DynamicFieldEntity entity = new DynamicFieldEntity();
        entity.setLabel(fieldDTO.getLabel());
        entity.setName(fieldDTO.getName());
        entity.setType(fieldDTO.getType());
        entity.setRequired(fieldDTO.getRequired());
        entity.setStatus(fieldDTO.getStatus() != null ? fieldDTO.getStatus() : true);
        entity.setOptions(fieldDTO.getOptions());
        entity.setOrderIndex(newOrderIndex);
        entity.setAccountId(accountId);
        entity.setElectionId(electionId);

        try {
            DynamicFieldEntity savedEntity = dynamicFieldRepository.save(entity);
            log.info("Dynamic field created with id={}", savedEntity.getId());

            return new DynamicFieldDTO(
                    savedEntity.getId(),
                    savedEntity.getLabel(),
                    savedEntity.getName(),
                    savedEntity.getType(),
                    savedEntity.getRequired(),
                    savedEntity.getStatus(),
                    savedEntity.getOptions(),
                    savedEntity.getOrderIndex(),
                    savedEntity.getCreatedTime(),
                    savedEntity.getModifiedTime(),
                    electionId
            );
        } catch (Exception ex) {
            log.error("Failed to create dynamic field: {}", fieldDTO.getLabel(), ex);
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional
    public DynamicFieldResponseDTO getDynamicFields(Long accountId, Long electionId, Pageable pageable) {
        log.debug("Fetching dynamic fields for accountId={}, electionId={}", accountId, electionId);

        validateElectionOwnership(electionId, accountId);

        List<DynamicFieldEntity> fields = dynamicFieldRepository.findByElectionIdAndAccountIdOrderByOrderIndexAsc(electionId, accountId);

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), fields.size());
        List<DynamicFieldEntity> pagedFields = fields.subList(start, end);

        Page<DynamicFieldEntity> fieldsPage = new PageImpl<>(pagedFields, pageable, fields.size());

        log.info("Successfully fetched {} dynamic fields for electionId: {}", fields.size(), electionId);
        return new DynamicFieldResponseDTO(fieldsPage);
    }

    @Transactional
    public DynamicFieldDTO updateDynamicField(Long accountId, Long electionId, Long fieldId, DynamicFieldDTO fieldDTO) {
        log.debug("Updating dynamic field id={} for accountId={}, electionId={}", fieldId, accountId, electionId);

        validateElectionOwnership(electionId, accountId);
        validateField(fieldDTO, true);

        DynamicFieldEntity entity = dynamicFieldRepository.findByIdAndAccountIdAndElectionId(fieldId, accountId, electionId)
                .orElseThrow(() -> new ThedalException(ThedalError.DYNAMIC_FIELD_NOT_FOUND, HttpStatus.NOT_FOUND));

        // Enforce immutability of the technical 'name' once created to avoid breaking existing voter data keys
        if (entity.getName() != null && !entity.getName().equals(fieldDTO.getName())) {
            log.error("Attempt to change immutable dynamic field name from '{}' to '{}' (id={})", entity.getName(), fieldDTO.getName(), fieldId);
            throw new ThedalException(ThedalError.INVALID_FORM_FIELDS, HttpStatus.BAD_REQUEST);
        }

        entity.setLabel(fieldDTO.getLabel());
        entity.setType(fieldDTO.getType());
        entity.setRequired(fieldDTO.getRequired());
        entity.setStatus(fieldDTO.getStatus() != null ? fieldDTO.getStatus() : entity.getStatus());
        // For types that don't support options, always set to null; otherwise, use provided options or retain existing
        if (List.of("string", "number", "boolean", "image", "file").contains(fieldDTO.getType())) {
            entity.setOptions(null);
        } else {
            entity.setOptions(fieldDTO.getOptions() != null ? fieldDTO.getOptions() : entity.getOptions());
        }

        try {
            DynamicFieldEntity updatedEntity = dynamicFieldRepository.save(entity);
            log.info("Dynamic field updated with id={}", updatedEntity.getId());

            return new DynamicFieldDTO(
                    updatedEntity.getId(),
                    updatedEntity.getLabel(),
                    updatedEntity.getName(),
                    updatedEntity.getType(),
                    updatedEntity.getRequired(),
                    updatedEntity.getStatus(),
                    updatedEntity.getOptions(),
                    updatedEntity.getOrderIndex(),
                    updatedEntity.getCreatedTime(),
                    updatedEntity.getModifiedTime(),
                    electionId
            );
        } catch (Exception ex) {
            log.error("Failed to update dynamic field id={}: {}", fieldId, ex.getMessage(), ex);
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional
    public void deleteDynamicField(Long accountId, Long electionId, Long fieldId) {
        log.debug("Deleting dynamic field id={} for accountId={}, electionId={}", fieldId, accountId, electionId);

        validateElectionOwnership(electionId, accountId);

        DynamicFieldEntity entity = dynamicFieldRepository.findByIdAndAccountIdAndElectionId(fieldId, accountId, electionId)
                .orElseThrow(() -> new ThedalException(ThedalError.DYNAMIC_FIELD_NOT_FOUND, HttpStatus.NOT_FOUND));

        try {
            dynamicFieldRepository.delete(entity);
            log.info("Dynamic field deleted with id={}", fieldId);

            // Reorder remaining fields to maintain contiguous order indices
            List<DynamicFieldEntity> remainingFields = dynamicFieldRepository.findByElectionIdAndAccountIdOrderByOrderIndexAsc(electionId, accountId);
            for (int i = 0; i < remainingFields.size(); i++) {
                remainingFields.get(i).setOrderIndex(i);
            }
            dynamicFieldRepository.saveAll(remainingFields);
            log.info("Reordered remaining fields for electionId={}", electionId);
        } catch (Exception ex) {
            log.error("Failed to delete dynamic field: id={}", fieldId, ex);
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional
    public void deleteDynamicFields(Long accountId, Long electionId, List<Long> fieldIds) {
        log.debug("Deleting dynamic fields for accountId={}, electionId={}, fieldIds={}", accountId, electionId, fieldIds);

        validateElectionOwnership(electionId, accountId);

        try {
            if (fieldIds == null || fieldIds.isEmpty()) {
                int deletedCount = dynamicFieldRepository.deleteByAccountIdAndElectionId(accountId, electionId);
                if (deletedCount == 0) {
                    log.warn("No dynamic fields found to delete for accountId: {}, electionId: {}", accountId, electionId);
                    throw new ThedalException(ThedalError.DYNAMIC_FIELD_NOT_FOUND, HttpStatus.NOT_FOUND);
                }
                log.info("Deleted {} dynamic fields for accountId: {}, electionId: {}", deletedCount, accountId, electionId);
            } else {
                List<DynamicFieldEntity> fields = dynamicFieldRepository.findByIdInAndAccountIdAndElectionId(fieldIds, accountId, electionId);
                if (fields.isEmpty()) {
                    log.warn("No dynamic fields found for given IDs: {}", fieldIds);
                    throw new ThedalException(ThedalError.DYNAMIC_FIELD_NOT_FOUND, HttpStatus.NOT_FOUND);
                }
                dynamicFieldRepository.deleteAll(fields);
                log.info("Deleted dynamic fields with IDs: {}", fieldIds);

                // Reorder remaining fields
                List<DynamicFieldEntity> remainingFields = dynamicFieldRepository.findByElectionIdAndAccountIdOrderByOrderIndexAsc(electionId, accountId);
                for (int i = 0; i < remainingFields.size(); i++) {
                    remainingFields.get(i).setOrderIndex(i);
                }
                dynamicFieldRepository.saveAll(remainingFields);
                log.info("Reordered remaining fields for electionId={}", electionId);
            }
        } catch (Exception ex) {
            log.error("Failed to delete dynamic fields: ids={}", fieldIds, ex);
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional
    public DynamicFieldResponseDTO reorderDynamicFields(Long accountId, Long electionId, List<DynamicFieldReorderRequest> reorderRequests) {
        log.debug("Reordering dynamic fields for accountId={}, electionId={}", accountId, electionId);

        validateElectionOwnership(electionId, accountId);

        List<DynamicFieldEntity> fields = dynamicFieldRepository.findByElectionIdAndAccountIdOrderByOrderIndexAsc(electionId, accountId);
        if (fields.isEmpty()) {
            log.error("No dynamic fields found for electionId={} and accountId={}", electionId, accountId);
            throw new ThedalException(ThedalError.DYNAMIC_FIELD_NOT_FOUND, HttpStatus.NOT_FOUND);
        }

        Map<Long, Integer> newOrderMap = reorderRequests.stream()
                .collect(Collectors.toMap(DynamicFieldReorderRequest::getFieldId, DynamicFieldReorderRequest::getNewOrderIndex));

        reorderRequests.sort(Comparator.comparingInt(DynamicFieldReorderRequest::getNewOrderIndex));

        List<DynamicFieldEntity> remainingFields = new ArrayList<>(fields);
        remainingFields.removeIf(field -> newOrderMap.containsKey(field.getId()));

        List<DynamicFieldEntity> reorderedFields = new ArrayList<>(remainingFields);

        for (DynamicFieldReorderRequest request : reorderRequests) {
            DynamicFieldEntity field = fields.stream()
                    .filter(f -> f.getId().equals(request.getFieldId()))
                    .findFirst()
                    .orElseThrow(() -> new ThedalException(ThedalError.DYNAMIC_FIELD_NOT_FOUND, HttpStatus.NOT_FOUND));

            int newIndex = Math.min(request.getNewOrderIndex(), reorderedFields.size());
            reorderedFields.add(newIndex, field);
        }

        for (int i = 0; i < reorderedFields.size(); i++) {
            reorderedFields.get(i).setOrderIndex(i);
            log.info("Updated dynamic field order: {} -> {}", reorderedFields.get(i).getLabel(), i);
        }

        try {
            dynamicFieldRepository.saveAll(reorderedFields);
            log.info("Dynamic field order updated successfully for electionId: {}", electionId);

            Page<DynamicFieldEntity> fieldsPage = new PageImpl<>(
                    dynamicFieldRepository.findByElectionIdAndAccountIdOrderByOrderIndexAsc(electionId, accountId),
                    PageRequest.of(0, reorderedFields.size()),
                    reorderedFields.size()
            );
            return new DynamicFieldResponseDTO(fieldsPage);
        } catch (Exception ex) {
            log.error("Failed to update dynamic field order for electionId: {}", electionId, ex);
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional
    public DynamicFieldDTO updateDynamicFieldStatus(Long accountId, Long electionId, Long fieldId, DynamicFieldStatusDTO statusDTO) {
        log.debug("Updating status for dynamic field id={} for accountId={}, electionId={}", fieldId, accountId, electionId);

        validateElectionOwnership(electionId, accountId);

        DynamicFieldEntity entity = dynamicFieldRepository.findByIdAndAccountIdAndElectionId(fieldId, accountId, electionId)
                .orElseThrow(() -> new ThedalException(ThedalError.DYNAMIC_FIELD_NOT_FOUND, HttpStatus.NOT_FOUND));

        if (statusDTO.getStatus() == null) {
            log.error("Status field cannot be null for field id={}", fieldId);
            throw new ThedalException(ThedalError.INVALID_FORM_FIELDS, HttpStatus.BAD_REQUEST);
        }

        entity.setStatus(statusDTO.getStatus());

        try {
            DynamicFieldEntity updatedEntity = dynamicFieldRepository.save(entity);
            log.info("Dynamic field status updated with id={}", updatedEntity.getId());

            return new DynamicFieldDTO(
                    updatedEntity.getId(),
                    updatedEntity.getLabel(),
                    updatedEntity.getName(),
                    updatedEntity.getType(),
                    updatedEntity.getRequired(),
                    updatedEntity.getStatus(),
                    updatedEntity.getOptions(),
                    updatedEntity.getOrderIndex(),
                    updatedEntity.getCreatedTime(),
                    updatedEntity.getModifiedTime(),
                    electionId
            );
        } catch (DataIntegrityViolationException ex) {
            log.error("Data integrity violation while updating dynamic field status id={}: {}", fieldId, ex.getMessage(), ex);
            throw new ThedalException(ThedalError.DATABASE_CONSTRAINT_VIOLATION, HttpStatus.BAD_REQUEST);
        } catch (Exception ex) {
            log.error("Failed to update dynamic field status id={}: {}", fieldId, ex.getMessage(), ex);
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional
    public int enableAllFields(Long accountId, Long electionId) {
        log.debug("Enabling all dynamic fields for accountId={}, electionId={}", accountId, electionId);
        
        validateElectionOwnership(electionId, accountId);
        
        List<DynamicFieldEntity> fields = dynamicFieldRepository.findByElectionIdAndAccountIdOrderByOrderIndexAsc(electionId, accountId);
        int updatedCount = 0;
        
        for (DynamicFieldEntity field : fields) {
            if (!field.getStatus()) {
                field.setStatus(true);
                dynamicFieldRepository.save(field);
                updatedCount++;
                log.debug("Enabled dynamic field: {}", field.getName());
            }
        }
        
        log.info("Enabled {} dynamic fields for electionId={}", updatedCount, electionId);
        return updatedCount;
    }

    @Transactional
    public int disableAllFields(Long accountId, Long electionId) {
        log.debug("Disabling all dynamic fields for accountId={}, electionId={}", accountId, electionId);
        
        validateElectionOwnership(electionId, accountId);
        
        List<DynamicFieldEntity> fields = dynamicFieldRepository.findByElectionIdAndAccountIdOrderByOrderIndexAsc(electionId, accountId);
        int updatedCount = 0;
        
        for (DynamicFieldEntity field : fields) {
            if (field.getStatus()) {
                field.setStatus(false);
                dynamicFieldRepository.save(field);
                updatedCount++;
                log.debug("Disabled dynamic field: {}", field.getName());
            }
        }
        
        log.info("Disabled {} dynamic fields for electionId={}", updatedCount, electionId);
        return updatedCount;
    }

    @Transactional
    public int requireAllFields(Long accountId, Long electionId) {
        log.debug("Making all dynamic fields required for accountId={}, electionId={}", accountId, electionId);
        
        validateElectionOwnership(electionId, accountId);
        
        List<DynamicFieldEntity> fields = dynamicFieldRepository.findByElectionIdAndAccountIdOrderByOrderIndexAsc(electionId, accountId);
        int updatedCount = 0;
        
        for (DynamicFieldEntity field : fields) {
            if (!field.getRequired()) {
                field.setRequired(true);
                dynamicFieldRepository.save(field);
                updatedCount++;
                log.debug("Made dynamic field required: {}", field.getName());
            }
        }
        
        log.info("Made {} dynamic fields required for electionId={}", updatedCount, electionId);
        return updatedCount;
    }

    @Transactional
    public int optionalAllFields(Long accountId, Long electionId) {
        log.debug("Making all dynamic fields optional for accountId={}, electionId={}", accountId, electionId);
        
        validateElectionOwnership(electionId, accountId);
        
        List<DynamicFieldEntity> fields = dynamicFieldRepository.findByElectionIdAndAccountIdOrderByOrderIndexAsc(electionId, accountId);
        int updatedCount = 0;
        
        for (DynamicFieldEntity field : fields) {
            if (field.getRequired()) {
                field.setRequired(false);
                dynamicFieldRepository.save(field);
                updatedCount++;
                log.debug("Made dynamic field optional: {}", field.getName());
            }
        }
        
        log.info("Made {} dynamic fields optional for electionId={}", updatedCount, electionId);
        return updatedCount;
    }

   
}