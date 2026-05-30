package com.thedal.thedal_app.cpanel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.thedal.thedal_app.cpanel.dtos.CasteCategoryUpdateCpanelRequest;
import com.thedal.thedal_app.settings.electionsettings.CasteCategoryEntity;
import com.thedal.thedal_app.settings.electionsettings.CasteCategoryRepository;
import com.thedal.thedal_app.settings.electionsettings.dto.CasteCategoryRequest;
import com.thedal.thedal_app.settings.electionsettings.dto.CasteCategoryResponseDTO;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CasteCategoryCpanelService {

    @Autowired
    private CasteCategoryRepository casteCategoryRepository;

    @Transactional
    public List<CasteCategoryEntity> createMultipleCasteCategories(List<CasteCategoryRequest> casteCategoryRequests, Long electionId, Long accountId) {
        List<CasteCategoryEntity> casteCategoryEntities = new ArrayList<>();

        for (CasteCategoryRequest casteCategoryRequest : casteCategoryRequests) {
            if (casteCategoryRequest.getCasteCategoryName() == null) {
                log.error("Missing required field: casteCategoryName for account ID: {}", accountId);
                throw new ThedalException(ThedalError.MISSING_REQUIRED_FIELDS, HttpStatus.BAD_REQUEST);
            }

            log.info("Creating caste category '{}' for election ID: {}", 
                     casteCategoryRequest.getCasteCategoryName(), electionId);

            Optional<CasteCategoryEntity> existingCasteCategory = casteCategoryRepository.findByCasteCategoryNameAndAccountIdAndElectionId(
                    casteCategoryRequest.getCasteCategoryName(), accountId, electionId);
            if (existingCasteCategory.isPresent()) {
                log.error("Duplicate caste category detected: '{}' for election ID: {}", 
                          casteCategoryRequest.getCasteCategoryName(), electionId);
                throw new ThedalException(ThedalError.DUPLICATE_CASTE_CATEGORY, HttpStatus.CONFLICT);
            }

            Integer minOrderIndex = casteCategoryRepository.findMinOrderIndexByElectionId(electionId);
            int newOrderIndex = (minOrderIndex != null) ? minOrderIndex - 1 : 0;

            CasteCategoryEntity casteCategoryEntity = new CasteCategoryEntity();
            casteCategoryEntity.setCasteCategoryName(casteCategoryRequest.getCasteCategoryName());
            casteCategoryEntity.setAccountId(accountId);
            casteCategoryEntity.setElectionId(electionId);
            casteCategoryEntity.setOrderIndex(newOrderIndex);

            casteCategoryEntities.add(casteCategoryRepository.save(casteCategoryEntity));

            log.info("Caste category '{}' created successfully with orderIndex {}.", casteCategoryEntity.getCasteCategoryName(), newOrderIndex);
        }

        return casteCategoryEntities;
    }

    public List<Map<String, Object>> getCasteCategoriesForCpanel(Long accountId, Long electionId) {
        List<CasteCategoryEntity> casteCategories = casteCategoryRepository.findByAccountIdAndElectionIdOrderByOrderIndexAsc(accountId, electionId);

        if (casteCategories.isEmpty()) {
            log.error("No caste categories found for accountId: {} and electionId: {}", accountId, electionId);
            throw new ThedalException(ThedalError.CASTE_CATEGORIES_NOT_FOUND, HttpStatus.NOT_FOUND);
        }

        return casteCategories.stream()
                .map(casteCategory -> {
                    Map<String, Object> casteCategoryData = new HashMap<>();
                    casteCategoryData.put("casteCategoryId", casteCategory.getId());
                    casteCategoryData.put("casteCategoryName", casteCategory.getCasteCategoryName());
                    casteCategoryData.put("orderIndex", casteCategory.getOrderIndex());
                    return casteCategoryData;
                }).collect(Collectors.toList());
    }

    @Transactional
    public void deleteCasteCategory(Long accountId, Long electionId, List<Long> casteCategoryIds) {
        try {
            int deletedCount;

            if (casteCategoryIds == null || casteCategoryIds.isEmpty()) {
                log.info("Deleting all caste categories for accountId: {}, electionId: {}", accountId, electionId);
                deletedCount = casteCategoryRepository.deleteByAccountIdAndElectionId(accountId, electionId);
            } else {
                log.info("Deleting specific caste categories for accountId: {}, electionId: {}, casteCategoryIds: {}", 
                        accountId, electionId, casteCategoryIds);

                deletedCount = casteCategoryRepository.deleteByAccountIdAndElectionIdAndIds(accountId, electionId, casteCategoryIds);
            }

            if (deletedCount == 0) {
                log.warn("No caste categories found for accountId: {}, electionId: {}, casteCategoryIds: {}", 
                        accountId, electionId, casteCategoryIds);
                throw new ThedalException(ThedalError.CASTE_CATEGORY_NOT_FOUND, HttpStatus.NOT_FOUND);
            }

            log.info("Successfully deleted {} caste categories", deletedCount);

        } catch (ThedalException e) {
            log.error("Error while deleting caste categories: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error deleting caste categories: {}", e.getMessage());
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @Transactional
    public List<CasteCategoryResponseDTO> updateMultipleCasteCategories(Long accountId, Long electionId, List<CasteCategoryUpdateCpanelRequest> casteCategoryUpdateRequests) {
        List<CasteCategoryResponseDTO> updatedCasteCategories = new ArrayList<>();

        for (CasteCategoryUpdateCpanelRequest request : casteCategoryUpdateRequests) {
            Long casteCategoryId = request.getCasteCategoryId();

            CasteCategoryEntity casteCategory = casteCategoryRepository.findByIdAndAccountIdAndElectionId(casteCategoryId, accountId, electionId)
                    .orElseThrow(() -> new ThedalException(ThedalError.CASTE_CATEGORY_NOT_FOUND, HttpStatus.NOT_FOUND));

            if (request.getCasteCategoryName() != null && !request.getCasteCategoryName().isEmpty()) {
                Optional<CasteCategoryEntity> existingCasteCategory = casteCategoryRepository.findByCasteCategoryNameAndAccountIdAndElectionId(
                        request.getCasteCategoryName(), accountId, electionId);
                if (existingCasteCategory.isPresent() && !existingCasteCategory.get().getId().equals(casteCategoryId)) {
                    throw new ThedalException(
                            ThedalError.DUPLICATE_CASTE_CATEGORY, 
                            HttpStatus.CONFLICT, 
                            "Caste category with name '" + request.getCasteCategoryName() + "' already exists."
                    );
                }
                casteCategory.setCasteCategoryName(request.getCasteCategoryName());
            }

            Integer minOrderIndex = casteCategoryRepository.findMinOrderIndexByElectionId(electionId);
            int newOrderIndex = (minOrderIndex != null) ? Math.max(0, minOrderIndex - 1) : 0;

            casteCategory.setOrderIndex(newOrderIndex);
            casteCategory = casteCategoryRepository.save(casteCategory);

            updatedCasteCategories.add(new CasteCategoryResponseDTO(casteCategory.getId(), casteCategory.getCasteCategoryName()));
        }

        return updatedCasteCategories;
    }
}