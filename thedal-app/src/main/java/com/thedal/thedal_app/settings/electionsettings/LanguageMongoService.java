//package com.thedal.thedal_app.settings.electionsettings;
//
//import java.time.LocalDateTime;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.stream.Collectors;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.HttpStatus;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import com.thedal.thedal_app.election.ElectionRepository;
//import com.thedal.thedal_app.general.RequestDetailsService;
//import com.thedal.thedal_app.response.ThedalResponse;
//import com.thedal.thedal_app.response.ThedalSuccess;
//import com.thedal.thedal_app.settings.electionsettings.dto.LanguageDTO;
//import com.thedal.thedal_app.settings.electionsettings.dto.LanguageResponseDTO;
//import com.thedal.thedal_app.thedal_exception.ThedalError;
//import com.thedal.thedal_app.thedal_exception.ThedalException;
//
//import lombok.extern.slf4j.Slf4j;
//
//@Service
//@Slf4j
//public class LanguageMongoService {
//
//    @Autowired
//    private RequestDetailsService requestDetails;
//
//    @Autowired
//    private LanguageRepository languageRepository;
//
//    @Autowired
//    private LanguageMongoRepository languageMongoRepository;
//
//    @Autowired
//    private ElectionRepository electionRepository;
//
//    @Transactional
//    public ThedalResponse<LanguageResponseDTO> createLanguage(LanguageDTO languageDTO, Long electionId) {
//    	Long accountId = requestDetails.getCurrentUserId();
//        if (accountId == null) {
//            log.error("Account ID not found, unauthorized access.");
//            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
//        }
//
//        // Validate election existence
//        if (electionId != 0) {
//            boolean electionExists = electionRepository.existsByIdAndAccountId(electionId, accountId);
//            if (!electionExists) {
//                log.error("Election ID '{}' not found for user '{}'.", electionId, accountId);
//                throw new ThedalException(ThedalError.ELECTION_NOT_FOUND, HttpStatus.NOT_FOUND);
//            }
//        }
//
//        // Check for duplicate language in PostgreSQL
//        if (languageRepository.existsByLanguageNameAndElectionId(languageDTO.getLanguageName(), electionId)) {
//            String errorMessage = String.format("Language with name '%s' already exists in election '%d'.", 
//                    languageDTO.getLanguageName(), electionId);
//            log.error(errorMessage);
//            throw new ThedalException(ThedalError.LANGUAGE_ALREADY_EXITS, HttpStatus.CONFLICT, errorMessage);
//        }
//
////        // Check for duplicate language in MongoDB
////        if (languageMongoRepository.existsByLanguageNameAndElectionId(languageDTO.getLanguageName(), electionId)) {
////            String errorMessage = String.format("Language with name '%s' already exists in MongoDB for election '%d'.", 
////                    languageDTO.getLanguageName(), electionId);
////            log.error(errorMessage);
////            throw new ThedalException(ThedalError.LANGUAGE_ALREADY_EXITS, HttpStatus.CONFLICT, errorMessage);
////        }
//
//        // Determine order index
//        Integer maxOrderIndex = languageRepository.findMaxOrderIndexByElectionId(electionId);
//        int newOrderIndex = (maxOrderIndex != null) ? maxOrderIndex + 1 : 0;
//
//        // Create PostgreSQL entity
//        Language language = new Language();
//        language.setLanguageName(languageDTO.getLanguageName());
//        language.setAccountId(accountId);
//        language.setElectionId(electionId);
//        language.setOrderIndex(newOrderIndex);
//        language.setState(languageDTO.getState());
//
//        // Save to PostgreSQL
//        Language savedLanguage = languageRepository.save(language);
//        if (savedLanguage.getId() == null) {
//            log.error("Failed to save language to PostgreSQL. Entity: {}", language);
//            throw new ThedalException(ThedalError.LANGUAGE_NOT_FOUND, HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//
//        // Create and save MongoDB entity
//        LanguageMongo languageMongo = new LanguageMongo(savedLanguage);
//        try {
//            languageMongoRepository.save(languageMongo);
//            log.info("Language saved to MongoDB with ID: {}", languageMongo.getId());
//        } catch (Exception e) {
//            log.error("Failed to save language to MongoDB: {}", e.getMessage());
//            // Optionally, queue for retry or notify admin
//            throw new ThedalException(ThedalError.MONGO_WRITE_FAILED, HttpStatus.INTERNAL_SERVER_ERROR, 
//                    "Failed to save to MongoDB");
//        }
//
//        // Create response DTO
//        LanguageResponseDTO responseDTO = new LanguageResponseDTO(
//                savedLanguage.getId(),
//                savedLanguage.getLanguageName(),
//                savedLanguage.getAccountId(),
//                savedLanguage.getElectionId(),
//                savedLanguage.getOrderIndex(),
//                savedLanguage.getState()
//        );
//
//        return new ThedalResponse<>(ThedalSuccess.LANGUAGE_CREATED, responseDTO);
//    }
//
//    @Transactional
//    public ThedalResponse<LanguageResponseDTO> updateLanguage(Long accountId, Long electionId, 
//            Long languageId, LanguageDTO languageDTO) {
//        // Fetch from PostgreSQL
//        Language language = languageRepository.findByAccountIdAndElectionIdAndId(accountId, electionId, languageId)
//                .orElseThrow(() -> new ThedalException(ThedalError.LANGUAGE_NOT_FOUND, HttpStatus.NOT_FOUND));
//
//        // Check for duplicate language in PostgreSQL
//        if (!language.getLanguageName().equals(languageDTO.getLanguageName()) && 
//                languageRepository.existsByLanguageNameAndElectionId(languageDTO.getLanguageName(), electionId)) {
//            String errorMessage = String.format("Language with name '%s' already exists in election '%d'.", 
//                    languageDTO.getLanguageName(), electionId);
//            log.error(errorMessage);
//            throw new ThedalException(ThedalError.LANGUAGE_ALREADY_EXITS, HttpStatus.CONFLICT, errorMessage);
//        }
//
//        // Check for duplicate language in MongoDB
//        if (!language.getLanguageName().equals(languageDTO.getLanguageName()) && 
//                languageMongoRepository.existsByLanguageNameAndElectionId(languageDTO.getLanguageName(), electionId)) {
//            String errorMessage = String.format("Language with name '%s' already exists in MongoDB for election '%d'.", 
//                    languageDTO.getLanguageName(), electionId);
//            log.error(errorMessage);
//            throw new ThedalException(ThedalError.LANGUAGE_ALREADY_EXITS, HttpStatus.CONFLICT, errorMessage);
//        }
//
//        // Update PostgreSQL entity
//        language.setLanguageName(languageDTO.getLanguageName());
//        language.setState(languageDTO.getState());
//        Language savedLanguage = languageRepository.save(language);
//
//        // Update MongoDB entity
//        LanguageMongo languageMongo = languageMongoRepository.findById(languageId)
//                .orElse(new LanguageMongo(savedLanguage));
//        languageMongo.setLanguageName(languageDTO.getLanguageName());
//        languageMongo.setState(languageDTO.getState());
//        languageMongo.setUpdatedAt(LocalDateTime.now());
//        try {
//            languageMongoRepository.save(languageMongo);
//            log.info("Language updated in MongoDB with ID: {}", languageMongo.getId());
//        } catch (Exception e) {
//            log.error("Failed to update language in MongoDB: {}", e.getMessage());
//            throw new ThedalException(ThedalError.MONGO_WRITE_FAILED, HttpStatus.INTERNAL_SERVER_ERROR, 
//                    "Failed to update MongoDB");
//        }
//
//        // Create response DTO
//        LanguageResponseDTO responseDTO = new LanguageResponseDTO(
//                savedLanguage.getId(),
//                savedLanguage.getLanguageName(),
//                savedLanguage.getAccountId(),
//                savedLanguage.getElectionId(),
//                savedLanguage.getOrderIndex(),
//                savedLanguage.getState()
//        );
//
//        return new ThedalResponse<>(ThedalSuccess.LANGUAGE_UPDATED, responseDTO);
//    }
//
//    public ThedalResponse<List<Map<String, Object>>> getLanguages(Long accountId, Long electionId) {
//        log.info("Fetching languages from MongoDB for userId: {}, electionId: {}", accountId, electionId);
//
//        List<LanguageMongo> languages = languageMongoRepository.findByAccountIdAndElectionId(accountId, electionId);
//        if (languages.isEmpty()) {
//            log.warn("No languages found in MongoDB for userId: {}, electionId: {}", accountId, electionId);
//            throw new ThedalException(ThedalError.LANGUAGE_NOT_FOUND, HttpStatus.NOT_FOUND);
//        }
//
//        List<Map<String, Object>> languageDetails = languages.stream()
//                .map(language -> {
//                    Map<String, Object> data = new HashMap<>();
//                    data.put("id", language.getId());
//                    data.put("languageName", language.getLanguageName() != null ? language.getLanguageName() : "");
//                    data.put("orderIndex", language.getOrderIndex());
//                    data.put("state", language.getState() != null ? language.getState() : "");
//                    //data.put("voterCount", language.getVoterIds().size());
//                    return data;
//                })
//                .sorted((a, b) -> Integer.compare((Integer) a.get("orderIndex"), (Integer) b.get("orderIndex")))
//                .collect(Collectors.toList());
//
//        log.info("Fetched {} languages from MongoDB for electionId: {}", languageDetails.size(), electionId);
//        return new ThedalResponse<>(ThedalSuccess.LANGUAGE_FOUND, languageDetails);
//    }
//}