package com.thedal.thedal_app.election;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.thedal.thedal_app.election.VoterFieldOrderEntity.FieldOrderItem;
import com.thedal.thedal_app.election.dtos.VoterFieldOrderRequestDTO;
import com.thedal.thedal_app.election.dtos.VoterFieldOrderResponseDTO;
import com.thedal.thedal_app.general.RequestDetailsService;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class VoterFieldOrderService {

    @Autowired
    private VoterFieldOrderRepository voterFieldOrderRepository;

    @Autowired
    private DynamicFieldRepository dynamicFieldRepository;

    @Autowired
    private ElectionRepository electionRepository;

    @Autowired
    private RequestDetailsService requestDetails;

    private static final List<String> ALLOWED_VOTER_FIELDS = Arrays.asList(
        "firstName", "lastName", "age", "dateOfBirth", "mobileNumber", "whatsappNumber",
        "email", "voterLatitude", "voterLongitude", "star", "aadhaarNumber", "panNumber",
        "partyRegistrationNumber", "religion", "caste", "subCaste", "category", "casteCategory",
        "partyAffiliation", "schemes", "votingHistory", "feedback", "languages", "remarks"
    );

    private void validateElectionOwnership(Long electionId, Long accountId) {
        electionRepository.findByIdAndAccountId(electionId, accountId)
                .orElseThrow(() -> new ThedalException(ThedalError.INVALID_ELECTION, HttpStatus.FORBIDDEN));
    }

    private void validateFieldNames(List<String> fieldNames, Long electionId, Long accountId) {
        List<String> dynamicFieldLabels = dynamicFieldRepository
            .findByElectionIdAndAccountIdOrderByOrderIndexAsc(electionId, accountId)
            .stream()
            .map(DynamicFieldEntity::getLabel)
            .collect(Collectors.toList());

        List<String> allowedFields = new ArrayList<>(ALLOWED_VOTER_FIELDS);
        allowedFields.addAll(dynamicFieldLabels);

        for (String fieldName : fieldNames) {
            if (!allowedFields.contains(fieldName)) {
                log.error("Invalid field name: {}. Allowed fields are {}", fieldName, allowedFields);
                throw new ThedalException(ThedalError.INVALID_FORM_FIELDS, HttpStatus.BAD_REQUEST);
            }
        }
    }

    @Transactional
    public VoterFieldOrderResponseDTO updateFieldOrder(Long accountId, Long electionId, VoterFieldOrderRequestDTO requestDTO) {
        if (requestDTO.getFields() == null || requestDTO.getFields().isEmpty()) {
            log.error("Field order request cannot be empty");
            throw new ThedalException(ThedalError.INVALID_REQUEST, HttpStatus.BAD_REQUEST, "Field order request cannot be empty");
        }

        validateElectionOwnership(electionId, accountId);

        List<String> fieldNames = requestDTO.getFields().stream()
            .map(VoterFieldOrderRequestDTO.FieldOrderItem::getName)
            .collect(Collectors.toList());
        //validateFieldNames(fieldNames, electionId, accountId);

        // Ensure orderIndex values are contiguous and start from 1
        List<VoterFieldOrderRequestDTO.FieldOrderItem> sortedFields = requestDTO.getFields().stream()
            .sorted((a, b) -> Integer.compare(a.getOrderIndex(), b.getOrderIndex()))
            .collect(Collectors.toList());

        for (int i = 0; i < sortedFields.size(); i++) {
            sortedFields.get(i).setOrderIndex(i + 1);
        }

        VoterFieldOrderEntity entity = voterFieldOrderRepository
            .findByElectionIdAndAccountId(electionId, accountId)
            .orElse(new VoterFieldOrderEntity());

        entity.setAccountId(accountId);
        entity.setElectionId(electionId);
        entity.setOrderedFields(sortedFields.stream()
            .map(f -> new FieldOrderItem(f.getName(), f.getOrderIndex()))
            .collect(Collectors.toList()));

        try {
            VoterFieldOrderEntity savedEntity = voterFieldOrderRepository.save(entity);
            log.info("Successfully updated voter field order for electionId: {}", electionId);

            return new VoterFieldOrderResponseDTO(
                savedEntity.getId(),
                savedEntity.getElectionId(),
                savedEntity.getOrderedFields().stream()
                    .map(f -> new VoterFieldOrderResponseDTO.FieldOrderItem(f.getName(), f.getOrderIndex()))
                    .collect(Collectors.toList()),
                savedEntity.getCreatedTime(),
                savedEntity.getModifiedTime()
            );
        } catch (Exception ex) {
            log.error("Failed to update voter field order for electionId: {}", electionId, ex);
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @Transactional
    public VoterFieldOrderResponseDTO getFieldOrder(Long accountId, Long electionId) {
        log.debug("Fetching voter field order for accountId={}, electionId={}", accountId, electionId);

        validateElectionOwnership(electionId, accountId);

        VoterFieldOrderEntity entity = voterFieldOrderRepository
            .findByElectionIdAndAccountId(electionId, accountId)
            .orElse(null);

        if (entity == null) {
            // Return default order (predefined fields + dynamic fields)
            List<DynamicFieldEntity> dynamicFields = dynamicFieldRepository
                .findByElectionIdAndAccountIdOrderByOrderIndexAsc(electionId, accountId);
            List<String> defaultFields = new ArrayList<>(ALLOWED_VOTER_FIELDS);
            defaultFields.addAll(dynamicFields.stream()
                .map(DynamicFieldEntity::getLabel)
                .collect(Collectors.toList()));

            List<VoterFieldOrderResponseDTO.FieldOrderItem> defaultOrder = new ArrayList<>();
            for (int i = 0; i < defaultFields.size(); i++) {
                defaultOrder.add(new VoterFieldOrderResponseDTO.FieldOrderItem(defaultFields.get(i), i + 1));
            }

            return new VoterFieldOrderResponseDTO(null, electionId, defaultOrder, null, null);
        }

        return new VoterFieldOrderResponseDTO(
            entity.getId(),
            entity.getElectionId(),
            entity.getOrderedFields().stream()
                .map(f -> new VoterFieldOrderResponseDTO.FieldOrderItem(f.getName(), f.getOrderIndex()))
                .collect(Collectors.toList()),
            entity.getCreatedTime(),
            entity.getModifiedTime()
        );
    }
}