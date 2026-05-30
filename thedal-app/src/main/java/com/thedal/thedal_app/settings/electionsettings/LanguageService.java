package com.thedal.thedal_app.settings.electionsettings;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.thedal.thedal_app.election.ElectionRepository;
import com.thedal.thedal_app.general.RequestDetailsService;
import com.thedal.thedal_app.response.ThedalResponse;
import com.thedal.thedal_app.response.ThedalSuccess;
import com.thedal.thedal_app.settings.electionsettings.dto.LanguageDTO;
import com.thedal.thedal_app.settings.electionsettings.dto.LanguageReorderRequest;
import com.thedal.thedal_app.settings.electionsettings.dto.LanguageResponseDTO;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class LanguageService {
    @Autowired
    private RequestDetailsService requestDetails;   

    @Autowired
    private LanguageRepository languageRepository;
    @Autowired
    private ElectionRepository electionRepository;
    @Autowired
    private LanguageMongoRepository languageMongoRepository;

    

@Transactional
public ThedalResponse<LanguageResponseDTO> createLanguage(LanguageDTO languageDTO, Long electionId) {
    	
        Long accountId = requestDetails.getCurrentAccountId();
	     //Long accountId = (electionId == 0) ? 0L : requestDetails.getCurrentAccountId();

        if (accountId == null) {
            log.error("Account ID not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }
        
     // Bypass election check when electionId and accountId are 0
        if (electionId != 0) {
            boolean electionExists = electionRepository.existsByIdAndAccountId(electionId, accountId);
            if (!electionExists) {
                log.error("Election ID '{}' not found for account '{}'.", electionId, accountId);
                throw new ThedalException(ThedalError.ELECTION_NOT_FOUND, HttpStatus.NOT_FOUND);
            }
        }

        if (languageRepository.existsByLanguageNameAndElectionId(languageDTO.getLanguageName(), electionId)) {
            String errorMessage = String.format("Language with name '%s' already exists in election '%d'.", languageDTO.getLanguageName(), electionId);
            log.error(errorMessage);
            throw new ThedalException(ThedalError.LANGUAGE_ALREADY_EXITS, HttpStatus.UNAUTHORIZED, errorMessage);

        }
        
     // Check for duplicate language in MongoDB
        if (languageMongoRepository.existsByLanguageNameAndElectionId(languageDTO.getLanguageName(), electionId)) {
            String errorMessage = String.format("Language with name '%s' already exists in MongoDB for election '%d'.", languageDTO.getLanguageName(), electionId);
            log.error(errorMessage);
            throw new ThedalException(ThedalError.LANGUAGE_ALREADY_EXITS, HttpStatus.UNAUTHORIZED, errorMessage);
        }
        
     // Find the highest order index for this election
        Integer maxOrderIndex = languageRepository.findMaxOrderIndexByElectionId(electionId);
        int newOrderIndex = (maxOrderIndex != null) ? maxOrderIndex + 1 : 0;

        Language language= new Language();
        language.setLanguageName(languageDTO.getLanguageName());
        language.setAccountId(accountId);
        language.setElectionId(electionId);
        language.setOrderIndex(newOrderIndex);
        language.setState(languageDTO.getState());
    
        log.info("Saving new language: {}",language );

        // Dual write implementation
        try {
            Language savedLanguage = languageRepository.save(language);

            if (savedLanguage.getId() == null) {
                log.error("Failed to save language. Entity: {}", language);
                throw new ThedalException(ThedalError.LANGUAGE_NOT_FOUND, HttpStatus.INTERNAL_SERVER_ERROR);
            }
            
            try {
                LanguageMongo languageMongo = new LanguageMongo(savedLanguage);
                languageMongoRepository.save(languageMongo);
                log.info("Successfully saved language to MongoDB: id={}, name={}", savedLanguage.getId(), savedLanguage.getLanguageName());
            } catch (Exception mongoEx) {
                log.error("Failed to save language to MongoDB: id={}, name={}", savedLanguage.getId(), savedLanguage.getLanguageName(), mongoEx);
                throw new RuntimeException("MongoDB save failed, triggering rollback", mongoEx);
            }

            log.info("Language saved successfully with ID: {}", savedLanguage.getId());

            // Create the response DTO
            LanguageResponseDTO languageDTOResponse = new LanguageResponseDTO(
                savedLanguage.getId(),
                savedLanguage.getLanguageName(),
                savedLanguage.getAccountId(),
                savedLanguage.getElectionId(), 
                savedLanguage.getOrderIndex(),
                savedLanguage.getState()
            );

            return new ThedalResponse<>(ThedalSuccess.LANGUAGE_CREATED, languageDTOResponse);
        } catch (Exception ex) {
            log.error("Failed to create language: {}", languageDTO.getLanguageName(), ex);
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

@Transactional
public ThedalResponse<List<Map<String, Object>>> getLanguage(Long accountId, Long electionId) {
    log.info("Fetching languages with voter count for accountId: {}, electionId: {}", accountId, electionId);

    List<Object[]> results = languageRepository.findLanguagesWithVoterCount(accountId, electionId);
    if (results.isEmpty()) {
        log.warn("No languages found for accountId: {}, electionId: {}", accountId, electionId);
        throw new ThedalException(ThedalError.LANGUAGE_NOT_FOUND, HttpStatus.NOT_FOUND);
    }

    List<Map<String, Object>> languageDetails = results.stream()
            .map(result -> {
                Language language = (Language) result[0];
                Long voterCount = (Long) result[1];
                Map<String, Object> languageData = new HashMap<>();
                languageData.put("id", language.getId());
                languageData.put("languageName", language.getLanguageName() != null ? language.getLanguageName() : "");
                languageData.put("orderIndex", language.getOrderIndex() != null ? language.getOrderIndex() : 0);
                languageData.put("state", language.getState() != null ? language.getState() : "");
                languageData.put("voterCount", voterCount);
                languageData.put("updatedAt", language.getUpdatedAt()); // For sorting
                languageData.put("createdAt", language.getCreatedAt()); // For sorting
                return languageData;
            })
            .sorted(Comparator
                    .comparing((Map<String, Object> m) -> (LocalDateTime) m.get("updatedAt"), Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing((Map<String, Object> m) -> (LocalDateTime) m.get("createdAt"), Comparator.nullsLast(Comparator.reverseOrder())))
            .map(map -> {
                // Remove temporary sorting fields
                map.remove("updatedAt");
                map.remove("createdAt");
                return map;
            })
            .collect(Collectors.toList());

    log.info("Successfully fetched {} languages for electionId: {}", languageDetails.size(), electionId);
    return new ThedalResponse<>(ThedalSuccess.LANGUAGE_FOUND, languageDetails);
}


    @Transactional
    public LanguageResponseDTO updateLanguage(Long accountId, Long electionId, Long languageId,
            LanguageDTO languageDTO) {
        
        Language language = languageRepository.findByAccountIdAndElectionIdAndId(accountId, electionId, languageId)
                .orElseThrow(() -> new ThedalException(ThedalError.LANGUAGE_NOT_FOUND, HttpStatus.NOT_FOUND));
        
        // Check for duplicate language name in PostgreSQL (excluding current language)
        if (languageRepository.existsByLanguageNameAndElectionIdAndIdNot(languageDTO.getLanguageName(), electionId, languageId)) {
            String errorMessage = String.format("Language with name '%s' already exists in election '%d'.", 
                                                languageDTO.getLanguageName(), electionId);
            log.error(errorMessage);
            throw new ThedalException(ThedalError.LANGUAGE_ALREADY_EXITS, HttpStatus.UNAUTHORIZED, errorMessage);
        }
        
        // Check for duplicate language name in MongoDB (excluding current language)
        if (languageMongoRepository.existsByLanguageNameAndElectionIdAndIdNot(languageDTO.getLanguageName(), electionId, languageId)) {
            String errorMessage = String.format("Language with name '%s' already exists in MongoDB for election '%d'.", 
                                                languageDTO.getLanguageName(), electionId);
            log.error(errorMessage);
            throw new ThedalException(ThedalError.LANGUAGE_ALREADY_EXITS, HttpStatus.UNAUTHORIZED, errorMessage);
        }
        
        // Dual write implementation with error handling
        try {
            language.setLanguageName(languageDTO.getLanguageName());
            language.setState(languageDTO.getState());
            
            Language savedLanguage = languageRepository.save(language);
            
            if (savedLanguage.getId() == null) {
                log.error("Failed to update language in PostgreSQL. Entity: {}", language);
                throw new ThedalException(ThedalError.LANGUAGE_NOT_FOUND, HttpStatus.INTERNAL_SERVER_ERROR);
            }
            
            try {
                // Update MongoDB entity
                LanguageMongo languageMongo = languageMongoRepository.findById(languageId)
                        .orElse(new LanguageMongo(savedLanguage));
                languageMongo.setLanguageName(languageDTO.getLanguageName());
                languageMongo.setState(languageDTO.getState());
                languageMongo.setUpdatedAt(LocalDateTime.now());
                languageMongoRepository.save(languageMongo);
                log.info("Language updated successfully in both PostgreSQL and MongoDB with ID: {}", languageMongo.getId());
            } catch (Exception mongoEx) {
                log.error("Failed to update language in MongoDB: id={}, name={}", savedLanguage.getId(), savedLanguage.getLanguageName(), mongoEx);
                throw new RuntimeException("MongoDB update failed, triggering rollback", mongoEx);
            }
            
            return new LanguageResponseDTO(savedLanguage.getId(),
                    savedLanguage.getLanguageName(),
                    savedLanguage.getAccountId(),
                    savedLanguage.getElectionId(),
                    savedLanguage.getOrderIndex(),
                    savedLanguage.getState());
        } catch (Exception ex) {
            log.error("Failed to update language: id={}, name={}", languageId, languageDTO.getLanguageName(), ex);
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

//    @Transactional
//public void deleteLanguage(Long accountId, Long electionId, List<Long> languageIds) {
//    if (languageIds == null || languageIds.isEmpty()) {
//        // Delete all languages for the given account and election
//        int deletedCount = languageRepository.deleteByAccountIdAndElectionId(accountId, electionId);
//        if (deletedCount == 0) {
//            log.warn("No languages found to delete for accountId: {}, electionId: {}", accountId, electionId);
//            throw new ThedalException(ThedalError.LANGUAGE_NOT_FOUND, HttpStatus.NOT_FOUND);
//        }
//        log.info("Deleted all languages for accountId: {}, electionId: {}", accountId, electionId);
//    } else {
//        // Delete specific languages by ID
//        List<Language> languages = languageRepository.findByIdInAndAccountIdAndElectionId(languageIds, accountId, electionId);
//        if (languages.isEmpty()) {
//            log.warn("Languages not found for given IDs: {}", languageIds);
//            throw new ThedalException(ThedalError.LANGUAGE_NOT_FOUND, HttpStatus.NOT_FOUND);
//        }
//        languageRepository.deleteAll(languages);
//        log.info("Deleted specific languages: {}", languageIds);
//    }
//}

//    @Transactional
//    public void deleteLanguage(Long accountId, Long electionId, List<Long> languageIds) {
//        try {
//            if (languageIds == null || languageIds.isEmpty()) {
//                // Delete all languages
//                int deletedCount = languageRepository.deleteByAccountIdAndElectionId(accountId, electionId);
//                if (deletedCount == 0) {
//                    log.warn("No languages found to delete for accountId: {}, electionId: {}", accountId, electionId);
//                    throw new ThedalException(ThedalError.LANGUAGE_NOT_FOUND, HttpStatus.NOT_FOUND);
//                }
//                try {
//                    languageMongoRepository.deleteByAccountIdAndElectionId(accountId, electionId);
//                } catch (Exception mongoEx) {
//                    log.error("Failed to delete from MongoDB for accountId: {}, electionId: {}", accountId, electionId, mongoEx);
//                    throw new RuntimeException("MongoDB deletion failed, triggering rollback", mongoEx);
//                }
//                log.info("Deleted all languages for accountId: {}, electionId: {} from both PostgreSQL and MongoDB", accountId, electionId);
//            } else {
//                // Delete specific languages
//                List<Language> languages = languageRepository.findByIdInAndAccountIdAndElectionId(languageIds, accountId, electionId);
//                if (languages.isEmpty()) {
//                    log.warn("Languages not found for given IDs: {}", languageIds);
//                    throw new ThedalException(ThedalError.LANGUAGE_NOT_FOUND, HttpStatus.NOT_FOUND);
//                }
//                languageRepository.deleteAll(languages);
//                try {
//                    languageMongoRepository.deleteByIdIn(languageIds);
//                } catch (Exception mongoEx) {
//                    log.error("Failed to delete from MongoDB for languageIds: {}", languageIds, mongoEx);
//                    throw new RuntimeException("MongoDB deletion failed, triggering rollback", mongoEx);
//                }
//                log.info("Deleted specific languages: {} from both PostgreSQL and MongoDB", languageIds);
//            }
//        } catch (Exception e) {
//            log.error("Error deleting languages for accountId: {}, electionId: {}, languageIds: {}. Reason: {}", 
//                      accountId, electionId, languageIds, e.getMessage());
//            throw new ThedalException(ThedalError.LANGUAGE_DELETION_FAILED, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
//        }
//    }
//    
    
    @Transactional
    public ThedalResponse<Map<String, Object>> deleteLanguage(Long accountId, Long electionId, List<Long> languageIds) {
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> errors = new ArrayList<>();
        List<Long> deletedLanguageIds = new ArrayList<>();

        try {
            // Verify MongoDB repository
            if (languageMongoRepository == null) {
                log.error("LanguageMongoRepository is not initialized!");
                throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, "MongoDB repository not available");
            }

            if (languageIds == null || languageIds.isEmpty()) {
                // Delete all languages
                List<Language> languages = languageRepository.findByAccountIdAndElectionId(accountId, electionId);
                if (languages.isEmpty()) {
                    log.warn("No languages found in PostgreSQL for accountId: {}, electionId: {}", accountId, electionId);
                    throw new ThedalException(ThedalError.LANGUAGE_NOT_FOUND, HttpStatus.NOT_FOUND);
                }

                // Check for assigned voters
                List<Long> allLanguageIds = languages.stream().map(Language::getId).collect(Collectors.toList());
                Map<Long, Long> voterCounts = languageRepository.getVoterCountsByLanguageIds(accountId, electionId, allLanguageIds);

                // Identify languages with assigned voters
                languages.forEach(language -> {
                    Long voterCount = voterCounts.getOrDefault(language.getId(), 0L);
                    if (voterCount > 0) {
                        Map<String, Object> error = new HashMap<>();
                        error.put("languageId", language.getId());
                        error.put("languageName", language.getLanguageName());
                        error.put("voterCount", voterCount);
                        error.put("message", String.format("Language '%s' (ID: %d) cannot be deleted as it is assigned to %d voter(s)", 
                                language.getLanguageName(), language.getId(), voterCount));
                        errors.add(error);
                    }
                });

                if (!errors.isEmpty()) {
                    response.put("status", "failure");
                    response.put("errors", errors);
                    response.put("deletedLanguageIds", deletedLanguageIds);
                    log.error("Cannot delete all languages due to voter assignments: {}", errors);
                    ThedalResponse<Map<String, Object>> errorResponse = new ThedalResponse<>(ThedalError.LANGUAGE_DELETION_FAILED, response);
                    errorResponse.setCode(HttpStatus.BAD_REQUEST.value()); // 400
                    return errorResponse;
                }

                // No assigned voters, proceed with deletion
                int deletedCount = languageRepository.deleteByAccountIdAndElectionId(accountId, electionId);
                try {
                    languageMongoRepository.deleteByAccountIdAndElectionId(accountId, electionId);
                } catch (Exception mongoEx) {
                    log.error("Failed to delete languages from MongoDB: for accountId: {}, electionId: {}", accountId, electionId, mongoEx);
                    throw new RuntimeException("MongoDB deletion failed, triggering rollback", mongoEx);
                }
                response.put("status", "success");
                response.put("message", String.format("Successfully deleted %d languages from both PostgreSQL and MongoDB", deletedCount));
                response.put("deletedLanguageIds", allLanguageIds);
                log.info("Successfully deleted all languages for accountId: {}, electionId: {} from both PostgreSQL and MongoDB", accountId, electionId);
                return new ThedalResponse<>(ThedalSuccess.LANGUAGE_DELETED, response);
            } else {
                // Delete specific languages
                List<Language> languages = languageRepository.findByIdInAndAccountIdAndElectionId(languageIds, accountId, electionId);
                if (languages.isEmpty()) {
                    log.warn("No languages found in PostgreSQL for IDs: {}, accountId: {}, electionId: {}", languageIds, accountId, electionId);
                    throw new ThedalException(ThedalError.LANGUAGE_NOT_FOUND, HttpStatus.NOT_FOUND);
                }

                // Check for assigned voters
                Map<Long, Long> voterCounts = languageRepository.getVoterCountsByLanguageIds(accountId, electionId, languageIds);
                List<Long> languagesToDelete = new ArrayList<>();

                languages.forEach(language -> {
                    Long voterCount = voterCounts.getOrDefault(language.getId(), 0L);
                    if (voterCount > 0) {
                        Map<String, Object> error = new HashMap<>();
                        error.put("languageId", language.getId());
                        error.put("languageName", language.getLanguageName());
                        error.put("voterCount", voterCount);
                        error.put("message", String.format("Language '%s' (ID: %d) cannot be deleted as it is assigned to %d voter(s)", 
                                language.getLanguageName(), language.getId(), voterCount));
                        errors.add(error);
                    } else {
                        languagesToDelete.add(language.getId());
                    }
                });

                // Delete unassigned languages
                if (!languagesToDelete.isEmpty()) {
                    try {
                        languageRepository.deleteAll(languages.stream()
                                .filter(lang -> languagesToDelete.contains(lang.getId()))
                                .collect(Collectors.toList()));
                        languageMongoRepository.deleteByIdIn(languagesToDelete);
                        deletedLanguageIds.addAll(languagesToDelete);
                        log.info("Deleted {} specific languages from both PostgreSQL and MongoDB: {}", languagesToDelete.size(), languagesToDelete);
                    } catch (Exception ex) {
                        log.error("Failed to delete languages: {} from PostgreSQL or MongoDB", languagesToDelete, ex);
                        throw new RuntimeException("Deletion failed, triggering rollback", ex);
                    }
                }

                // Prepare response
                if (!errors.isEmpty()) {
                    response.put("status", errors.size() == languageIds.size() ? "failure" : "partial_success");
                    response.put("errors", errors);
                    response.put("deletedLanguageIds", deletedLanguageIds);
                    log.warn("Partial deletion: deleted languages: {}, errors: {}", deletedLanguageIds, errors);
                    ThedalResponse<Map<String, Object>> errorResponse = new ThedalResponse<>(ThedalError.LANGUAGE_DELETION_FAILED, response);
                    errorResponse.setCode(errors.size() == languageIds.size() ? HttpStatus.BAD_REQUEST.value() : HttpStatus.OK.value());
                    return errorResponse;
                } else {
                    response.put("status", "success");
                    response.put("message", String.format("Successfully deleted %d language(s)", deletedLanguageIds.size()));
                    response.put("deletedLanguageIds", deletedLanguageIds);
                    return new ThedalResponse<>(ThedalSuccess.LANGUAGE_DELETED, response);
                }
            }
        } catch (ThedalException te) {
            log.error("ThedalException deleting languages for accountId: {}, electionId: {}, languageIds: {}. Reason: {}", 
                    accountId, electionId, languageIds, te.getMessage(), te);
            response.put("status", "error");
            response.put("message", te.getMessage());
            ThedalResponse<Map<String, Object>> errorResponse = new ThedalResponse<>(te.getThedalError(), response);
            errorResponse.setCode(te.getHttpStatus().value());
            return errorResponse;
        } catch (Exception e) {
            log.error("Unexpected error deleting languages for accountId: {}, electionId: {}, languageIds: {}. Reason: {}", 
                    accountId, electionId, languageIds, e.getMessage(), e);
            response.put("status", "error");
            response.put("message", "Failed to delete languages due to an unexpected error: " + e.getMessage());
            ThedalResponse<Map<String, Object>> errorResponse = new ThedalResponse<>(ThedalError.INTERNAL_SERVER_ERROR, response);
            errorResponse.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            return errorResponse;
        }
    }

    
    @Transactional
    public void updateLanguageOrder(List<LanguageReorderRequest> reorderRequests, Long accountId, Long electionId) {
        try {
            List<Language> languages = languageRepository.findByElectionIdAndAccountIdOrderByOrderIndexAsc(electionId, accountId);

            if (languages.isEmpty()) {
                log.error("No languages found for election ID {} and account ID {}", electionId, accountId);
                throw new ThedalException(ThedalError.LANGUAGE_NOT_FOUND, HttpStatus.NOT_FOUND);
            }

            // Create a map of languageId -> newOrderIndex
            Map<Long, Integer> newOrderMap = reorderRequests.stream()
                    .collect(Collectors.toMap(LanguageReorderRequest::getLanguageId, LanguageReorderRequest::getNewOrderIndex));

            // Sort the reorderRequests by newOrderIndex to avoid conflicts
            reorderRequests.sort(Comparator.comparingInt(LanguageReorderRequest::getNewOrderIndex));

            // Remove languages that are being reordered
            List<Language> remainingLanguages = new ArrayList<>(languages);
            remainingLanguages.removeIf(lang -> newOrderMap.containsKey(lang.getId()));

            // Temporary list to hold reordered elements
            List<Language> reorderedLanguages = new ArrayList<>(remainingLanguages);

            // Insert languages at their new positions
            for (LanguageReorderRequest request : reorderRequests) {
                Language lang = languages.stream()
                        .filter(l -> l.getId().equals(request.getLanguageId()))
                        .findFirst()
                        .orElseThrow(() -> new ThedalException(ThedalError.LANGUAGE_NOT_FOUND, HttpStatus.NOT_FOUND));

                // Ensure the new index is within bounds
                int newIndex = Math.min(request.getNewOrderIndex(), reorderedLanguages.size());
                reorderedLanguages.add(newIndex, lang);
            }

            // Update `orderIndex` for all languages
            for (int i = 0; i < reorderedLanguages.size(); i++) {
                reorderedLanguages.get(i).setOrderIndex(i);
                log.info("Updated language order: {} -> {}", reorderedLanguages.get(i).getLanguageName(), i);
            }

            // Save updated order to PostgreSQL
            languageRepository.saveAll(reorderedLanguages);
            
            // Update MongoDB with new order indices
            try {
                for (Language lang : reorderedLanguages) {
                    LanguageMongo languageMongo = languageMongoRepository.findById(lang.getId())
                            .orElse(new LanguageMongo(lang));
                    languageMongo.setOrderIndex(lang.getOrderIndex());
                    languageMongo.setUpdatedAt(LocalDateTime.now());
                    languageMongoRepository.save(languageMongo);
                }
                log.info("Language order updated successfully in both PostgreSQL and MongoDB for electionId: {}", electionId);
            } catch (Exception mongoEx) {
                log.error("Failed to update language order in MongoDB for electionId: {}", electionId, mongoEx);
                throw new RuntimeException("MongoDB reorder failed, triggering rollback", mongoEx);
            }
        } catch (Exception ex) {
            log.error("Failed to update language order for electionId: {}", electionId, ex);
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @Transactional
    public ThedalResponse<List<Map<String, Object>>> getLanguageFromMongo(Long accountId, Long electionId) {
        log.info("Fetching languages from PostgreSQL for accountId: {}, electionId: {}", accountId, electionId);

        // Read from PostgreSQL instead of MongoDB
        List<Language> languages = languageRepository.findByAccountIdAndElectionIdOrderByOrderIndexAsc(accountId, electionId);
        if (languages.isEmpty()) {
            log.warn("No languages found in PostgreSQL for accountId: {}, electionId: {}", accountId, electionId);
            throw new ThedalException(ThedalError.LANGUAGE_NOT_FOUND, HttpStatus.NOT_FOUND);
        }
        
        List<Map<String, Object>> languageDetails = languages.stream()
                .map(language -> {
                    Map<String, Object> languageData = new HashMap<>();
                    languageData.put("id", language.getId());
                    languageData.put("languageName", language.getLanguageName() != null ? language.getLanguageName() : "");
                    languageData.put("orderIndex", language.getOrderIndex() != null ? language.getOrderIndex() : 0);
                    languageData.put("state", language.getState() != null ? language.getState() : "");
                    //languageData.put("voterCount", language.getVoterIds().size());
                   // languageData.put("voterCount", voterCounts.getOrDefault(language.getId(), 0L));
                    return languageData;
                })
                .sorted(Comparator.comparingInt(m -> (Integer) m.get("orderIndex")))
                .collect(Collectors.toList());

        log.info("Successfully fetched {} languages from PostgreSQL for electionId: {}", languageDetails.size(), electionId);
        return new ThedalResponse<>(ThedalSuccess.LANGUAGE_FOUND, languageDetails);
    }
    
    
    
}
        
    

