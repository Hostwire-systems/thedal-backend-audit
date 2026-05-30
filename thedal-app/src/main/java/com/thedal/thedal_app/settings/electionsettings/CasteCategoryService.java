package com.thedal.thedal_app.settings.electionsettings;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.thedal.thedal_app.election.ElectionRepository;
import com.thedal.thedal_app.general.RequestDetailsService;
import com.thedal.thedal_app.response.ThedalResponse;
import com.thedal.thedal_app.response.ThedalSuccess;
import com.thedal.thedal_app.settings.electionsettings.dto.CasteCategoryReorderRequest;
import com.thedal.thedal_app.settings.electionsettings.dto.CasteCategoryRequest;
import com.thedal.thedal_app.settings.electionsettings.dto.CasteCategoryResponseDTO;
import com.thedal.thedal_app.settings.electionsettings.dto.CasteCategoryUpdateRequest;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CasteCategoryService {

    @Autowired
    private CasteCategoryRepository casteCategoryRepository;

    @Autowired
    private CasteCategoryMongoRepository casteCategoryMongoRepository;

    @Autowired
    private ElectionRepository electionRepository;

    @Autowired
    private RequestDetailsService requestDetails;

    @Transactional
    public CasteCategoryEntity createCasteCategory(CasteCategoryRequest casteCategoryRequest, Long electionId) {
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account ID not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        boolean electionExists = electionRepository.existsByIdAndAccountId(electionId, accountId);
        if (!electionExists) {
            log.error("Election ID {} not found for Account ID {}", electionId, accountId);
            throw new ThedalException(ThedalError.ELECTION_NOT_FOUND, HttpStatus.NOT_FOUND);
        }

        if (casteCategoryRequest.getCasteCategoryName() == null) {
            log.error("Missing required field: casteCategoryName for account ID: {}", accountId);
            throw new ThedalException(ThedalError.MISSING_REQUIRED_FIELDS, HttpStatus.BAD_REQUEST);
        }

        log.info("Creating caste category with name: {} for election ID: {}", 
                 casteCategoryRequest.getCasteCategoryName(), electionId);

        // Check for duplicates in PostgreSQL
        Optional<CasteCategoryEntity> existingCasteCategory = casteCategoryRepository.findByCasteCategoryNameAndAccountIdAndElectionId(
                casteCategoryRequest.getCasteCategoryName(), accountId, electionId);
        if (existingCasteCategory.isPresent()) {
            log.error("Duplicate caste category detected in PostgreSQL: '{}' for election ID: {}", 
                      casteCategoryRequest.getCasteCategoryName(), electionId);
            throw new ThedalException(
                    ThedalError.DUPLICATE_CASTE_CATEGORY, 
                    HttpStatus.CONFLICT, 
                    "Caste category with name '" + casteCategoryRequest.getCasteCategoryName() + "' already exists."
            );
        }

        // Check for duplicates in MongoDB
        if (casteCategoryMongoRepository.existsByCasteCategoryNameAndAccountIdAndElectionId(
                casteCategoryRequest.getCasteCategoryName(), accountId, electionId)) {
            log.error("Duplicate caste category detected in MongoDB: '{}' for election ID: {}", 
                      casteCategoryRequest.getCasteCategoryName(), electionId);
            throw new ThedalException(
                    ThedalError.DUPLICATE_CASTE_CATEGORY, 
                    HttpStatus.CONFLICT, 
                    "Caste category with name '" + casteCategoryRequest.getCasteCategoryName() + "' already exists in MongoDB."
            );
        }

        Integer maxOrderIndex = casteCategoryRepository.findMaxOrderIndexByElectionId(electionId);
        int newOrderIndex = (maxOrderIndex != null) ? maxOrderIndex + 1 : 0;

        CasteCategoryEntity casteCategoryEntity = new CasteCategoryEntity();
        casteCategoryEntity.setCasteCategoryName(casteCategoryRequest.getCasteCategoryName());
        casteCategoryEntity.setAccountId(accountId);
        casteCategoryEntity.setElectionId(electionId);
        casteCategoryEntity.setOrderIndex(newOrderIndex);

        // Dual write implementation with error handling
        try {
            CasteCategoryEntity savedCasteCategory = casteCategoryRepository.save(casteCategoryEntity);
            
            if (savedCasteCategory.getId() == null) {
                log.error("Failed to save caste category to PostgreSQL. Entity: {}", casteCategoryEntity);
                throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
            }
            
            try {
                CasteCategoryMongo casteCategoryMongo = new CasteCategoryMongo(savedCasteCategory);
                casteCategoryMongoRepository.save(casteCategoryMongo);
                log.info("Successfully saved caste category to MongoDB: id={}, name={}", 
                         savedCasteCategory.getId(), savedCasteCategory.getCasteCategoryName());
            } catch (Exception mongoEx) {
                log.error("Failed to save caste category to MongoDB: id={}, name={}", 
                         savedCasteCategory.getId(), savedCasteCategory.getCasteCategoryName(), mongoEx);
                throw new RuntimeException("MongoDB save failed, triggering rollback", mongoEx);
            }
            
            log.info("Caste category created successfully with orderIndex {}: {}", 
                     newOrderIndex, casteCategoryEntity.getCasteCategoryName());
            return savedCasteCategory;
        } catch (Exception ex) {
            log.error("Failed to create caste category: {}", casteCategoryRequest.getCasteCategoryName(), ex);
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional
    public ThedalResponse<List<Map<String, Object>>> getCasteCategories(Long electionId) {
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account ID not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        log.info("Fetching caste categories from PostgreSQL for accountId: {}, and electionId: {}", 
                 accountId, electionId);

        List<CasteCategoryEntity> casteCategories = casteCategoryRepository.findByAccountIdAndElectionIdOrderByOrderIndexAsc(accountId, electionId);
        if (casteCategories.isEmpty()) {
            log.warn("No caste categories found in PostgreSQL for accountId: {}, and electionId: {}", 
                     accountId, electionId);
            throw new ThedalException(ThedalError.CASTE_CATEGORIES_NOT_FOUND, HttpStatus.NOT_FOUND);
        }

        List<Map<String, Object>> casteCategoryDetails = casteCategories.stream()
                .map(casteCategory -> {
                    Map<String, Object> casteCategoryData = new HashMap<>();
                    casteCategoryData.put("casteCategoryId", casteCategory.getId());
                    casteCategoryData.put("casteCategoryName", casteCategory.getCasteCategoryName());
                    casteCategoryData.put("electionId", casteCategory.getElectionId());
                    casteCategoryData.put("orderIndex", casteCategory.getOrderIndex());
                    return casteCategoryData;
                })
                .collect(Collectors.toList());

        log.info("Successfully fetched {} caste categories from PostgreSQL for electionId: {}", casteCategoryDetails.size(), electionId);
        return new ThedalResponse<>(ThedalSuccess.CASTE_CATEGORIES_FETCHED, casteCategoryDetails);
    }

    @Transactional
    public void deleteCasteCategoryByIdAndAccountId(List<Long> casteCategoryIds, Long accountId, Long electionId) {
        List<CasteCategoryEntity> casteCategories;

        if (casteCategoryIds == null || casteCategoryIds.isEmpty()) {
            log.info("No caste category IDs provided, fetching all caste categories for electionId: {}", electionId);
            casteCategories = casteCategoryRepository.findByAccountIdAndElectionId(accountId, electionId);
        } else {
            log.info("Fetching specified caste categories for deletion");
            casteCategories = casteCategoryRepository.findByIdInAndAccountIdAndElectionId(casteCategoryIds, accountId, electionId);
        }

        if (casteCategories.isEmpty()) {
            throw new ThedalException(ThedalError.CASTE_CATEGORY_NOT_FOUND, HttpStatus.NOT_FOUND);
        }

        // Dual write deletion with error handling
        try {
            log.info("Deleting {} caste categories for electionId: {}", casteCategories.size(), electionId);
            casteCategoryRepository.deleteAll(casteCategories);
            casteCategoryRepository.flush();
            
            try {
                if (casteCategoryIds == null || casteCategoryIds.isEmpty()) {
                    casteCategoryMongoRepository.deleteByAccountIdAndElectionId(accountId, electionId);
                    log.info("Deleted all caste categories from MongoDB for accountId: {}, electionId: {}", accountId, electionId);
                } else {
                    casteCategoryMongoRepository.deleteByIdIn(casteCategoryIds);
                    log.info("Deleted caste categories from MongoDB: ids={}", casteCategoryIds);
                }
            } catch (Exception mongoEx) {
                log.error("Failed to delete caste categories from MongoDB: ids={}", casteCategoryIds, mongoEx);
                throw new RuntimeException("MongoDB deletion failed, triggering rollback", mongoEx);
            }
            
            log.info("Caste categories deleted successfully from both PostgreSQL and MongoDB: ids={}", casteCategoryIds);
        } catch (Exception ex) {
            log.error("Failed to delete caste categories: ids={}", casteCategoryIds, ex);
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional
    public CasteCategoryResponseDTO updateCasteCategory(Long casteCategoryId, Long accountId, CasteCategoryUpdateRequest request, Long electionId) {
        CasteCategoryEntity casteCategory = casteCategoryRepository.findByIdAndAccountIdAndElectionId(casteCategoryId, accountId, electionId)
                .orElseThrow(() -> new ThedalException(ThedalError.CASTE_CATEGORY_NOT_FOUND, HttpStatus.NOT_FOUND));

        if (request.getCasteCategoryName() != null && !request.getCasteCategoryName().isEmpty()) {
            // Check for duplicates in PostgreSQL (excluding current category)
            Optional<CasteCategoryEntity> existingCasteCategory = casteCategoryRepository.findByCasteCategoryNameAndAccountIdAndElectionIdAndIdNot(
                    request.getCasteCategoryName(), accountId, electionId, casteCategoryId);
            if (existingCasteCategory.isPresent()) {
                throw new ThedalException(
                        ThedalError.DUPLICATE_CASTE_CATEGORY, 
                        HttpStatus.CONFLICT, 
                        "Caste category with name '" + request.getCasteCategoryName() + "' already exists."
                );
            }
            
            // Check for duplicates in MongoDB (excluding current category)
            if (casteCategoryMongoRepository.existsByCasteCategoryNameAndAccountIdAndElectionIdAndIdNot(
                    request.getCasteCategoryName(), accountId, electionId, casteCategoryId)) {
                throw new ThedalException(
                        ThedalError.DUPLICATE_CASTE_CATEGORY, 
                        HttpStatus.CONFLICT, 
                        "Caste category with name '" + request.getCasteCategoryName() + "' already exists in MongoDB."
                );
            }
            
            casteCategory.setCasteCategoryName(request.getCasteCategoryName());
        }

        // Dual write implementation with error handling
        try {
            CasteCategoryEntity updatedCasteCategory = casteCategoryRepository.save(casteCategory);
            
            try {
                CasteCategoryMongo casteCategoryMongo = casteCategoryMongoRepository.findByIdAndAccountIdAndElectionId(casteCategoryId, accountId, electionId)
                        .orElse(new CasteCategoryMongo(updatedCasteCategory));
                casteCategoryMongo.setCasteCategoryName(updatedCasteCategory.getCasteCategoryName());
                casteCategoryMongo.setUpdatedAt(LocalDateTime.now());
                casteCategoryMongoRepository.save(casteCategoryMongo);
                log.info("Successfully updated caste category in MongoDB: id={}, name={}", 
                         casteCategoryId, updatedCasteCategory.getCasteCategoryName());
            } catch (Exception mongoEx) {
                log.error("Failed to update caste category in MongoDB: id={}, name={}", 
                         casteCategoryId, updatedCasteCategory.getCasteCategoryName(), mongoEx);
                throw new RuntimeException("MongoDB update failed, triggering rollback", mongoEx);
            }
            
            log.info("Caste category updated successfully in both PostgreSQL and MongoDB: id={}, name={}", 
                     casteCategoryId, updatedCasteCategory.getCasteCategoryName());
            return new CasteCategoryResponseDTO(updatedCasteCategory.getId(), updatedCasteCategory.getCasteCategoryName());
        } catch (Exception ex) {
            log.error("Failed to update caste category: id={}", casteCategoryId, ex);
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional
    public void updateCasteCategoryOrder(List<CasteCategoryReorderRequest> reorderRequests, Long electionId, Long accountId) {
        try {
            List<CasteCategoryEntity> casteCategories = casteCategoryRepository.findByAccountIdAndElectionIdOrderByOrderIndexAsc(accountId, electionId);

            if (casteCategories.isEmpty()) {
                log.error("No caste categories found for election ID {} and account ID {}", electionId, accountId);
                throw new ThedalException(ThedalError.CASTE_CATEGORY_NOT_FOUND, HttpStatus.NOT_FOUND);
            }

            Map<Long, Integer> newOrderMap = reorderRequests.stream()
                    .collect(Collectors.toMap(CasteCategoryReorderRequest::getCasteCategoryId, CasteCategoryReorderRequest::getNewOrderIndex));

            casteCategories.sort(Comparator.comparing(CasteCategoryEntity::getOrderIndex));

            List<CasteCategoryEntity> reorderedCasteCategories = new ArrayList<>(casteCategories);

            reorderedCasteCategories.removeIf(casteCategory -> newOrderMap.containsKey(casteCategory.getId()));

            for (CasteCategoryReorderRequest request : reorderRequests) {
                CasteCategoryEntity casteCategory = casteCategories.stream()
                        .filter(c -> c.getId().equals(request.getCasteCategoryId()))
                        .findFirst()
                        .orElseThrow(() -> new ThedalException(ThedalError.CASTE_CATEGORY_NOT_FOUND, HttpStatus.NOT_FOUND));

                casteCategory.setOrderIndex(request.getNewOrderIndex());
                reorderedCasteCategories.add(request.getNewOrderIndex(), casteCategory);
            }

            for (int i = 0; i < reorderedCasteCategories.size(); i++) {
                CasteCategoryEntity casteCategory = reorderedCasteCategories.get(i);
                casteCategory.setOrderIndex(i);
                log.info("Updated caste category order: {} -> {}", casteCategory.getCasteCategoryName(), i);
            }

            // Save updated order to PostgreSQL
            casteCategoryRepository.saveAll(reorderedCasteCategories);
            
            // Update MongoDB with new order indices
            try {
                for (CasteCategoryEntity casteCategory : reorderedCasteCategories) {
                    CasteCategoryMongo casteCategoryMongo = casteCategoryMongoRepository.findByIdAndAccountIdAndElectionId(
                            casteCategory.getId(), accountId, electionId)
                            .orElse(new CasteCategoryMongo(casteCategory));
                    casteCategoryMongo.setOrderIndex(casteCategory.getOrderIndex());
                    casteCategoryMongo.setUpdatedAt(LocalDateTime.now());
                    casteCategoryMongoRepository.save(casteCategoryMongo);
                }
                log.info("Caste category order updated successfully in both PostgreSQL and MongoDB for electionId: {}", electionId);
            } catch (Exception mongoEx) {
                log.error("Failed to update caste category order in MongoDB for electionId: {}", electionId, mongoEx);
                throw new RuntimeException("MongoDB reorder failed, triggering rollback", mongoEx);
            }
        } catch (Exception ex) {
            log.error("Failed to update caste category order for electionId: {}", electionId, ex);
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}