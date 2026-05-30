package com.thedal.thedal_app.election;



import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.thedal.thedal_app.awsfilestore.AwsFileUpload;
import com.thedal.thedal_app.election.dtos.FieldReorderRequest;
import com.thedal.thedal_app.election.dtos.SurveyExportJobDetailsDTO;
import com.thedal.thedal_app.election.dtos.SurveyFormDTO;
import com.thedal.thedal_app.election.dtos.SurveyFormReorderRequest;
import com.thedal.thedal_app.election.dtos.SurveyFormResponseDTO;
import com.thedal.thedal_app.election.dtos.SurveyFormSubmissionDTO;
import com.thedal.thedal_app.election.dtos.SurveyFormSubmissionsPageDTO;
import com.thedal.thedal_app.general.RequestDetailsService;
import com.thedal.thedal_app.quartz.JobSchedulerService;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class SurveyFormService {

	@Autowired
    private SurveyFormRepository surveyFormRepository;
    @Autowired
    private RequestDetailsService requestDetails;
    @Autowired
    private ElectionRepository electionRepository;
    @Autowired
    private SurveyFormSubmissionRepository surveyFormSubmissionRepository;
    @Autowired
    private SurveyDownloadJobRepository surveyDownloadJobRepository;
    @Autowired
    private JobSchedulerService jobSchedulerService;
    @Autowired
	private AwsFileUpload awsFileUpload;
	@Value("${aws.s3.files.bucket}")
	private String s3bucket;
	@Autowired
	private SurveyFormMongoRepository surveyFormMongoRepository;
	
    private static final int BATCH_SIZE = 100;
    
    
    private void validateElectionOwnership(Long electionId, Long accountId) {
        Optional<ElectionEntity> electionOpt = electionRepository.findByIdAndAccountId(electionId, accountId);
        if (!electionOpt.isPresent()) {
            log.error("Election ID {} does not belong to Account ID {}", electionId, accountId);
            throw new ThedalException(ThedalError.INVALID_ELECTION, HttpStatus.FORBIDDEN);  
        }
    }

    @Transactional
    public SurveyFormDTO createSurveyForm(Long accountId, Long electionId, SurveyFormDTO formDTO) {
        log.debug("Creating survey form for accountId={}, electionId={}", accountId, electionId);

        validateFormFields(formDTO.getCustomFields());
        validateElectionOwnership(electionId, accountId);
        
     // Assign orderIndex if not provided
        List<Map<String, Object>> customFields = formDTO.getCustomFields();
        for (int i = 0; i < customFields.size(); i++) {
            customFields.get(i).putIfAbsent("orderIndex", i);
        }
        
        Integer maxOrderIndex = surveyFormRepository.findMaxOrderIndexByElectionIdAndAccountId(electionId, accountId);
        int newOrderIndex = maxOrderIndex + 1;

        SurveyFormEntity entity = new SurveyFormEntity();
        entity.setFormName(formDTO.getFormName());
        entity.setFormDescription(formDTO.getFormDescription());
        entity.setCustomFields(formDTO.getCustomFields());
        //entity.setIsActive(formDTO.getIsActive());
        entity.setIsActive(formDTO.getIsActive() != null ? formDTO.getIsActive() : true);
        entity.setAccountId(accountId);
        entity.setElectionId(electionId);
        entity.setOrderIndex(newOrderIndex);

        // Save to PostgreSQL and MongoDB with dual-write pattern
        try {
            SurveyFormEntity savedEntity = surveyFormRepository.save(entity);
            try {
                SurveyFormMongo surveyFormMongo = new SurveyFormMongo(savedEntity);
                surveyFormMongoRepository.save(surveyFormMongo);
                log.info("Successfully saved survey form to MongoDB: id={}, name={}", savedEntity.getId(), savedEntity.getFormName());
            } catch (Exception mongoEx) {
                log.error("Failed to save survey form to MongoDB: id={}, name={}", savedEntity.getId(), savedEntity.getFormName(), mongoEx);
                throw new RuntimeException("MongoDB save failed, triggering rollback", mongoEx);
            }
            log.info("Survey form created with id={}", savedEntity.getId());

            return new SurveyFormDTO(
                    savedEntity.getId(),
                    savedEntity.getFormName(),
                    savedEntity.getFormDescription(),
                    savedEntity.getCustomFields(),
                    savedEntity.getIsActive(),
                    savedEntity.getOrderIndex(),
                    savedEntity.getCreatedTime(),
                    savedEntity.getModifiedTime(),
                    electionId
            );
        } catch (Exception ex) {
            log.error("Failed to create survey form: {}", formDTO.getFormName(), ex);
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    //@Transactional(readOnly = true)
    @Transactional
    public SurveyFormResponseDTO getSurveyForms(Long accountId, Long electionId, String formName,
                                               Boolean isActive, Pageable pageable) {
        log.debug("Fetching survey forms from MongoDB for accountId={}, electionId={}, formName={}, isActive={}",
                accountId, electionId, formName, isActive);

        if (formName == null && isActive == null) {
            log.debug("No filters provided; retrieving all survey forms for electionId={} and accountId={}",
                    electionId, accountId);
        }

        validateElectionOwnership(electionId, accountId);

        // Fetch from MongoDB
        List<SurveyFormMongo> mongoForms;
        if (formName != null || isActive != null) {
            mongoForms = surveyFormMongoRepository.findByAccountIdAndElectionIdWithFilters(accountId, electionId, formName, isActive);
        } else {
            mongoForms = surveyFormMongoRepository.findByAccountIdAndElectionId(accountId, electionId);
        }

        // Convert MongoDB entities to PostgreSQL entities for compatibility with existing response structure
        List<SurveyFormEntity> forms = mongoForms.stream()
                .map(mongo -> {
                    SurveyFormEntity entity = new SurveyFormEntity();
                    entity.setId(mongo.getSurveyFormId());
                    entity.setFormName(mongo.getFormName());
                    entity.setFormDescription(mongo.getFormDescription());
                    entity.setCustomFields(mongo.getCustomFields());
                    entity.setIsActive(mongo.getIsActive());
                    entity.setOrderIndex(mongo.getOrderIndex());
                    entity.setAccountId(mongo.getAccountId());
                    entity.setElectionId(mongo.getElectionId());
                    entity.setCreatedTime(mongo.getCreatedTime());
                    entity.setModifiedTime(mongo.getModifiedTime());
                    return entity;
                })
                .collect(Collectors.toList());

        // Apply pagination manually since MongoDB doesn't return Page directly
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), forms.size());
        List<SurveyFormEntity> pagedForms = forms.subList(start, end);
        
        // Create Page manually
        Page<SurveyFormEntity> formsPage = new PageImpl<>(pagedForms, pageable, forms.size());
        
        List<Long> formIds = pagedForms.stream()
                .map(SurveyFormEntity::getId)
                .collect(Collectors.toList());
        List<Object[]> submissionCountResults = surveyFormRepository.countSubmissionsByFormIds(accountId, electionId, formIds);
        Map<Long, Long> submissionCounts = submissionCountResults.stream()
                .collect(Collectors.toMap(
                        result -> (Long) result[0], // formId
                        result -> (Long) result[1]  // count
                ));

        log.info("Successfully fetched {} survey forms from MongoDB for electionId: {}", forms.size(), electionId);
        return new SurveyFormResponseDTO(formsPage, submissionCounts);
    }

//    @Transactional
//    public SurveyFormDTO updateSurveyForm(Long accountId, Long electionId, Long formId, SurveyFormDTO formDTO) {
//        log.debug("Updating survey form id={} for accountId={}, electionId={}", formId, accountId, electionId);
//
//        // Validate election ownership
//        validateElectionOwnership(electionId, accountId);
//
//        // Fetch the existing form from PostgreSQL
//        SurveyFormEntity entity = surveyFormRepository.findByIdAndAccountIdAndElectionId(formId, accountId, electionId)
//                .orElseThrow(() -> new ThedalException(ThedalError.SURVEY_FORM_NOT_FOUND, HttpStatus.NOT_FOUND));
//
//        // Validate and update formName (required)
//        if (formDTO.getFormName() == null || formDTO.getFormName().trim().isEmpty()) {
//            log.error("Form name is null or empty for formId={}", formId);
//            throw new ThedalException(ThedalError.INVALID_FORM_NAME, HttpStatus.BAD_REQUEST);
//        }
//        entity.setFormName(formDTO.getFormName());
//
//        // Update formDescription (optional)
//        entity.setFormDescription(formDTO.getFormDescription() != null ? formDTO.getFormDescription() : entity.getFormDescription());
//
//        // Validate and update customFields (optional)
//        if (formDTO.getCustomFields() != null && !formDTO.getCustomFields().isEmpty()) {
//            validateFormFields(formDTO.getCustomFields());
//            List<Map<String, Object>> customFields = formDTO.getCustomFields();
//            for (int i = 0; i < customFields.size(); i++) {
//                customFields.get(i).putIfAbsent("orderIndex", i);
//            }
//            entity.setCustomFields(customFields);
//        } else {
//            log.debug("No customFields provided for formId={}, retaining existing fields", formId);
//        }
//
//        // Update isActive (optional)
//        entity.setIsActive(formDTO.getIsActive() != null ? formDTO.getIsActive() : entity.getIsActive());
//
//        // Save the updated entity with dual-write pattern
//        try {
//            SurveyFormEntity updatedEntity = surveyFormRepository.save(entity);
//            
//            // Check if a MongoDB document exists for this surveyFormId
//            Optional<SurveyFormMongo> existingMongoForm = surveyFormMongoRepository.findBySurveyFormId(formId);
//            SurveyFormMongo surveyFormMongo;
//            
//            if (existingMongoForm.isPresent()) {
//                // Update existing MongoDB document
//                surveyFormMongo = new SurveyFormMongo(existingMongoForm.get().getId(), updatedEntity);
//            } else {
//                // Create new MongoDB document (should be rare, as it should exist from createSurveyForm)
//                surveyFormMongo = new SurveyFormMongo(updatedEntity);
//            }
//            
//            try {
//                surveyFormMongoRepository.save(surveyFormMongo);
//                log.info("Successfully updated survey form in MongoDB: id={}, name={}", formId, updatedEntity.getFormName());
//            } catch (Exception mongoEx) {
//                log.error("Failed to update survey form in MongoDB: id={}, name={}", formId, updatedEntity.getFormName(), mongoEx);
//                throw new RuntimeException("MongoDB update failed, triggering rollback", mongoEx);
//            }
//            log.info("Survey form updated with id={}", updatedEntity.getId());
//
//            return new SurveyFormDTO(
//                    updatedEntity.getId(),
//                    updatedEntity.getFormName(),
//                    updatedEntity.getFormDescription(),
//                    updatedEntity.getCustomFields(),
//                    updatedEntity.getIsActive(),
//                    updatedEntity.getOrderIndex(),
//                    updatedEntity.getCreatedTime(),
//                    updatedEntity.getModifiedTime(),
//                    electionId
//            );
//        } catch (Exception e) {
//            log.error("Failed to update survey form id={}: {}", formId, e.getMessage(), e);
//            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//    }
    @Transactional
    public SurveyFormDTO updateSurveyForm(Long accountId, Long electionId, Long formId, SurveyFormDTO formDTO) {
        log.debug("Updating survey form id={} for accountId={}, electionId={}", formId, accountId, electionId);

        // Validate election ownership
        validateElectionOwnership(electionId, accountId);

        // Fetch the existing form from PostgreSQL
        SurveyFormEntity entity = surveyFormRepository.findByIdAndAccountIdAndElectionId(formId, accountId, electionId)
                .orElseThrow(() -> new ThedalException(ThedalError.SURVEY_FORM_NOT_FOUND, HttpStatus.NOT_FOUND));

        // Validate and update formName (required)
        if (formDTO.getFormName() == null || formDTO.getFormName().trim().isEmpty()) {
            log.error("Form name is null or empty for formId={}", formId);
            throw new ThedalException(ThedalError.INVALID_FORM_NAME, HttpStatus.BAD_REQUEST);
        }
        entity.setFormName(formDTO.getFormName());

        // Update formDescription (optional)
        entity.setFormDescription(formDTO.getFormDescription() != null ? formDTO.getFormDescription() : entity.getFormDescription());

        // Validate and update customFields (optional)
        if (formDTO.getCustomFields() != null && !formDTO.getCustomFields().isEmpty()) {
        	validateFormFieldsUpdate(formDTO.getCustomFields());
            List<Map<String, Object>> customFields = formDTO.getCustomFields();
            // Automatically reassign orderIndex to be contiguous (0 to n-1)
            for (int i = 0; i < customFields.size(); i++) {
                customFields.get(i).put("orderIndex", i);
            }
            entity.setCustomFields(customFields);
        } else {
            log.debug("No customFields provided for formId={}, retaining existing fields", formId);
        }

        // Update isActive (optional)
        entity.setIsActive(formDTO.getIsActive() != null ? formDTO.getIsActive() : entity.getIsActive());

        // Save the updated entity with dual-write pattern
        try {
            SurveyFormEntity updatedEntity = surveyFormRepository.save(entity);
            
            // Check if a MongoDB document exists for this surveyFormId
            Optional<SurveyFormMongo> existingMongoForm = surveyFormMongoRepository.findBySurveyFormId(formId);
            SurveyFormMongo surveyFormMongo;
            
            if (existingMongoForm.isPresent()) {
                // Update existing MongoDB document
                surveyFormMongo = new SurveyFormMongo(existingMongoForm.get().getId(), updatedEntity);
            } else {
                // Create new MongoDB document
                surveyFormMongo = new SurveyFormMongo(updatedEntity);
            }
            
            try {
                surveyFormMongoRepository.save(surveyFormMongo);
                log.info("Successfully updated survey form in MongoDB: id={}, name={}", formId, updatedEntity.getFormName());
            } catch (Exception mongoEx) {
                log.error("Failed to update survey form in MongoDB: id={}, name={}", formId, updatedEntity.getFormName(), mongoEx);
                throw new RuntimeException("MongoDB update failed, triggering rollback", mongoEx);
            }
            log.info("Survey form updated with id={}", updatedEntity.getId());

            return new SurveyFormDTO(
                    updatedEntity.getId(),
                    updatedEntity.getFormName(),
                    updatedEntity.getFormDescription(),
                    updatedEntity.getCustomFields(),
                    updatedEntity.getIsActive(),
                    updatedEntity.getOrderIndex(),
                    updatedEntity.getCreatedTime(),
                    updatedEntity.getModifiedTime(),
                    electionId
            );
        } catch (Exception e) {
            log.error("Failed to update survey form id={}: {}", formId, e.getMessage(), e);
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void validateFormFieldsUpdate(List<Map<String, Object>> customFields) {
        if (customFields == null || customFields.isEmpty()) {
            log.error("Custom fields are null or empty");
            throw new ThedalException(ThedalError.INVALID_FORM_FIELDS, HttpStatus.BAD_REQUEST);
        }

        List<String> allowedTypes = List.of("string", "number", "boolean", "dropdown", "radio", "check-box", "multi-select", "image", "file");
        log.info("Validating fields with allowed types: {}", allowedTypes);

        for (int i = 0; i < customFields.size(); i++) {
            Map<String, Object> field = customFields.get(i);
            // Validate label
            if (!field.containsKey("label") || field.get("label") == null || ((String) field.get("label")).isBlank()) {
                log.error("Invalid or missing label in field at index {}: {}", i, field);
                throw new ThedalException(ThedalError.INVALID_FORM_FIELDS, HttpStatus.BAD_REQUEST);
            }

            // Validate type
            String type = (String) field.get("type");
            if (!allowedTypes.contains(type)) {
                log.error("Invalid field type: {}. Allowed types are {}", type, allowedTypes);
                throw new ThedalException(ThedalError.INVALID_FORM_FIELDS, HttpStatus.BAD_REQUEST);
            }

            // Validate required field
            Boolean required = field.containsKey("required") ? (Boolean) field.get("required") : false;
            if (!(required instanceof Boolean)) {
                log.error("Invalid 'required' value for field '{}'. Must be boolean", field.get("label"));
                throw new ThedalException(ThedalError.INVALID_FORM_FIELDS, HttpStatus.BAD_REQUEST);
            }

            // Validate options for dropdown, radio, check-box, and multi-select
            if (List.of("dropdown", "radio", "check-box", "multi-select").contains(type)) {
                if (!field.containsKey("options") || field.get("options") == null || 
                    !(field.get("options") instanceof List) || ((List<?>) field.get("options")).isEmpty()) {
                    log.error("Missing or invalid options for field type {} in field at index {}: {}", type, i, field);
                    throw new ThedalException(ThedalError.INVALID_FORM_FIELDS, HttpStatus.BAD_REQUEST);
                }
                List<?> options = (List<?>) field.get("options");
                for (Object option : options) {
                    if (!(option instanceof String) || ((String) option).isBlank()) {
                        log.error("Invalid option in field at index {}: {}. Options must be non-empty strings", i, field);
                        throw new ThedalException(ThedalError.INVALID_FORM_FIELDS, HttpStatus.BAD_REQUEST);
                    }
                }
            }

            // Ignore options for string, number, boolean, image, or file
            if (List.of("string", "number", "boolean", "image", "file").contains(type) && field.containsKey("options")) {
                log.warn("Options provided for field type {} in field at index {}: {}. Ignoring options.", type, i, field);
                field.remove("options");
            }
        }
    }
    
    @Transactional
    public void deleteSurveyForm(Long accountId, Long electionId, Long formId) {
        log.debug("Deleting survey form id={} for accountId={}, electionId={}", formId, accountId, electionId);

        validateElectionOwnership(electionId, accountId);

        SurveyFormEntity entity = surveyFormRepository.findByIdAndAccountIdAndElectionId(formId, accountId, electionId)
                .orElseThrow(() -> new ThedalException(ThedalError.SURVEY_FORM_NOT_FOUND, HttpStatus.NOT_FOUND));

        try {
            surveyFormRepository.delete(entity);
            try {
                surveyFormMongoRepository.deleteBySurveyFormId(formId);
                log.info("Deleted survey form from MongoDB: id={}", formId);
            } catch (Exception mongoEx) {
                log.error("Failed to delete survey form from MongoDB: id={}", formId, mongoEx);
                throw new RuntimeException("MongoDB deletion failed, triggering rollback", mongoEx);
            }
            log.info("Survey form deleted with id={}", formId);
        } catch (Exception ex) {
            log.error("Failed to delete survey form: id={}", formId, ex);
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @Transactional
    public void deleteSurveyForms(Long accountId, Long electionId, List<Long> formIds) {
        log.debug("Deleting survey forms for accountId={}, electionId={}, formIds={}", accountId, electionId, formIds);

        validateElectionOwnership(electionId, accountId);

        try {
            if (formIds == null || formIds.isEmpty()) {
                // Delete all forms and their submissions
                int deletedSubmissions = surveyFormSubmissionRepository.deleteByElectionIdAndAccountId(electionId, accountId);
                int deletedForms = surveyFormRepository.deleteByAccountIdAndElectionId(accountId, electionId);
                if (deletedForms == 0) {
                    log.warn("No survey forms found to delete for accountId: {}, electionId: {}", accountId, electionId);
                    throw new ThedalException(ThedalError.SURVEY_FORM_NOT_FOUND, HttpStatus.NOT_FOUND);
                }
                
                try {
                    surveyFormMongoRepository.deleteByAccountIdAndElectionId(accountId, electionId);
                    log.info("Deleted all survey forms from MongoDB for accountId: {}, electionId: {}", accountId, electionId);
                } catch (Exception mongoEx) {
                    log.error("Failed to delete all survey forms from MongoDB for accountId: {}, electionId: {}", accountId, electionId, mongoEx);
                    throw new RuntimeException("MongoDB deletion failed, triggering rollback", mongoEx);
                }
                
                log.info("Deleted {} survey forms and {} submissions for accountId: {}, electionId: {}", 
                         deletedForms, deletedSubmissions, accountId, electionId);
            } else {
                // Delete specific forms and their submissions
                List<SurveyFormEntity> forms = surveyFormRepository.findByIdInAndAccountIdAndElectionId(formIds, accountId, electionId);
                if (forms.isEmpty()) {
                    log.warn("No survey forms found for given IDs: {}", formIds);
                    throw new ThedalException(ThedalError.SURVEY_FORM_NOT_FOUND, HttpStatus.NOT_FOUND);
                }
                for (SurveyFormEntity form : forms) {
                    surveyFormSubmissionRepository.deleteByFormIdAndElectionIdAndAccountId(form.getId(), electionId, accountId);
                    surveyFormRepository.delete(form);
                }
                
                try {
                    surveyFormMongoRepository.deleteBySurveyFormIdIn(formIds);
                    log.info("Deleted survey forms from MongoDB: ids={}", formIds);
                } catch (Exception mongoEx) {
                    log.error("Failed to delete survey forms from MongoDB: ids={}", formIds, mongoEx);
                    throw new RuntimeException("MongoDB deletion failed, triggering rollback", mongoEx);
                }
                
                log.info("Deleted survey forms with IDs: {}", formIds);
            }
        } catch (Exception ex) {
            log.error("Failed to delete survey forms: ids={}", formIds, ex);
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional
    public SurveyFormDTO updateFormStatus(Long accountId, Long electionId, Long formId, Boolean isActive) {
        log.debug("Updating status of survey form id={} to isActive={} for accountId={}, electionId={}",
                formId, isActive, accountId, electionId);

        validateElectionOwnership(electionId, accountId);
        if (isActive == null) {
            throw new ThedalException(ThedalError.INVALID_FORM_STATUS, HttpStatus.BAD_REQUEST);
        }

        SurveyFormEntity entity = surveyFormRepository.findByIdAndAccountIdAndElectionId(formId, accountId, electionId)
                .orElseThrow(() -> new ThedalException(ThedalError.SURVEY_FORM_NOT_FOUND, HttpStatus.NOT_FOUND));

        entity.setIsActive(isActive);
        try {
            SurveyFormEntity updatedEntity = surveyFormRepository.save(entity);
            try {
                SurveyFormMongo surveyFormMongo = new SurveyFormMongo(updatedEntity);
                surveyFormMongoRepository.save(surveyFormMongo);
                log.info("Successfully updated survey form status in MongoDB: id={}", formId);
            } catch (Exception mongoEx) {
                log.error("Failed to update survey form status in MongoDB: id={}", formId, mongoEx);
                throw new RuntimeException("MongoDB update failed, triggering rollback", mongoEx);
            }
            log.info("Survey form status updated for id={}", updatedEntity.getId());

            return new SurveyFormDTO(
                    updatedEntity.getId(),
                    updatedEntity.getFormName(),
                    updatedEntity.getFormDescription(),
                    updatedEntity.getCustomFields(),
                    updatedEntity.getIsActive(),
                    updatedEntity.getCreatedTime(),
                    updatedEntity.getModifiedTime()
                   
            );
        } catch (Exception ex) {
            log.error("Failed to update survey form status: id={}", formId, ex);
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    private void validateFormFields(List<Map<String, Object>> customFields) {
        if (customFields == null || customFields.isEmpty()) {
            log.error("Custom fields are null or empty");
            throw new ThedalException(ThedalError.INVALID_FORM_FIELDS, HttpStatus.BAD_REQUEST);
        }

        List<String> allowedTypes = List.of("string", "number", "boolean", "dropdown", "radio", "check-box", "multi-select", "image", "file");
        log.info("Validating fields with allowed types: {}", allowedTypes);

        Set<Integer> orderIndexes = new HashSet<>();
        for (int i = 0; i < customFields.size(); i++) {
            Map<String, Object> field = customFields.get(i);
            // Validate label
            if (!field.containsKey("label") || field.get("label") == null || ((String) field.get("label")).isBlank()) {
                log.error("Invalid or missing label in field at index {}: {}", i, field);
                throw new ThedalException(ThedalError.INVALID_FORM_FIELDS, HttpStatus.BAD_REQUEST);
            }

            // Validate type
            String type = (String) field.get("type");
            if (!allowedTypes.contains(type)) {
                log.error("Invalid field type: {}. Allowed types are {}", type, allowedTypes);
                throw new ThedalException(ThedalError.INVALID_FORM_FIELDS, HttpStatus.BAD_REQUEST);
            }

            // Validate required field
            Boolean required = field.containsKey("required") ? (Boolean) field.get("required") : false;
            if (!(required instanceof Boolean)) {
                log.error("Invalid 'required' value for field '{}'. Must be boolean", field.get("label"));
                throw new ThedalException(ThedalError.INVALID_FORM_FIELDS, HttpStatus.BAD_REQUEST);
            }

            // Validate orderIndex if present
            if (field.containsKey("orderIndex") && field.get("orderIndex") != null) {
                Integer orderIndex = (Integer) field.get("orderIndex");
                if (orderIndex < 0 || !orderIndexes.add(orderIndex)) {
                    log.error("Invalid or duplicate orderIndex {} in field at index {}: {}", orderIndex, i, field);
                    throw new ThedalException(ThedalError.INVALID_FORM_FIELDS, HttpStatus.BAD_REQUEST);
                }
            }

            // Validate options for dropdown, radio, check-box, and multi-select
            if (List.of("dropdown", "radio", "check-box", "multi-select").contains(type)) {
                if (!field.containsKey("options") || field.get("options") == null || 
                    !(field.get("options") instanceof List) || ((List<?>) field.get("options")).isEmpty()) {
                    log.error("Missing or invalid options for field type {} in field at index {}: {}", type, i, field);
                    throw new ThedalException(ThedalError.INVALID_FORM_FIELDS, HttpStatus.BAD_REQUEST);
                }
                List<?> options = (List<?>) field.get("options");
                for (Object option : options) {
                    if (!(option instanceof String) || ((String) option).isBlank()) {
                        log.error("Invalid option in field at index {}: {}. Options must be non-empty strings", i, field);
                        throw new ThedalException(ThedalError.INVALID_FORM_FIELDS, HttpStatus.BAD_REQUEST);
                    }
                }
            }

            // Ignore options for string, number, boolean, image, or file
            if (List.of("string", "number", "boolean", "image", "file").contains(type) && field.containsKey("options")) {
                log.warn("Options provided for field type {} in field at index {}: {}. Ignoring options.", type, i, field);
                field.remove("options");
            }
        }

        // Skip contiguity check if orderIndexes is empty (will be assigned later)
        if (!orderIndexes.isEmpty() && !orderIndexes.equals(new HashSet<>(IntStream.range(0, customFields.size()).boxed().collect(Collectors.toSet())))) {
            log.error("Order indexes must be contiguous from 0 to {}", customFields.size() - 1);
            throw new ThedalException(ThedalError.INVALID_FORM_FIELDS, HttpStatus.BAD_REQUEST);
        }
    }
    
    /**
     * Dedicated method to get all survey forms from MongoDB
     * Similar to ReligionService's getAllReligionsWithVoterCountFromMongo
     */
    @Transactional
    public List<Map<String, Object>> getAllSurveyFormsFromMongo(Long accountId, Long electionId) {
        log.info("Fetching all survey forms from PostgreSQL for account ID: {} and election ID: {}", accountId, electionId);

        validateElectionOwnership(electionId, accountId);

        // Read from PostgreSQL instead of MongoDB
        List<SurveyFormEntity> surveyForms = surveyFormRepository.findByElectionIdAndAccountIdOrderByOrderIndexAsc(electionId, accountId);
        if (surveyForms.isEmpty()) {
            log.warn("No survey forms found in PostgreSQL for account ID: {} and election ID: {}", accountId, electionId);
            return new ArrayList<>();
        }

        List<Map<String, Object>> surveyFormDetails = surveyForms.stream()
                .map(form -> {
                    Map<String, Object> formData = new HashMap<>();
                    formData.put("formId", form.getId());
                    formData.put("formName", form.getFormName() != null ? form.getFormName() : "");
                    formData.put("formDescription", form.getFormDescription() != null ? form.getFormDescription() : "");
                    formData.put("customFields", form.getCustomFields() != null ? form.getCustomFields() : new ArrayList<>());
                    formData.put("isActive", form.getIsActive() != null ? form.getIsActive() : true);
                    formData.put("orderIndex", form.getOrderIndex() != null ? form.getOrderIndex() : 0);
                    formData.put("createdTime", form.getCreatedTime());
                    formData.put("modifiedTime", form.getModifiedTime());
                    
                    // Get submission count from PostgreSQL using existing method with single-item list
                    List<Object[]> submissionCountResults = surveyFormRepository.countSubmissionsByFormIds(
                            accountId, electionId, List.of(form.getId()));
                    Long submissionCount = submissionCountResults.isEmpty() ? 0L : (Long) submissionCountResults.get(0)[1];
                    formData.put("submissionCount", submissionCount != null ? submissionCount : 0L);
                    
                    return formData;
                })
                .sorted(Comparator.comparing(m -> (Integer) m.get("orderIndex")))
                .collect(Collectors.toList());

        log.info("Successfully fetched {} survey forms from PostgreSQL for electionId: {}", surveyFormDetails.size(), electionId);
        return surveyFormDetails;
    }
    
    
//    private void validateFormFields(List<Map<String, Object>> customFields) {
//        if (customFields == null || customFields.isEmpty()) {
//            log.error("Custom fields are null or empty");
//            throw new ThedalException(ThedalError.INVALID_FORM_FIELDS, HttpStatus.BAD_REQUEST);
//        }
//
//        // Updated list of allowed types
//        List<String> allowedTypes = List.of("string", "number", "boolean", "dropdown", "radio", "check-box", "multi-select", "image", "file");
//        log.info("Validating fields with allowed types: {}", allowedTypes);
//
//        Set<Integer> orderIndexes = new HashSet<>();
//        for (Map<String, Object> field : customFields) {
//            // Validate label
//            if (!field.containsKey("label") || field.get("label") == null || ((String) field.get("label")).isBlank()) {
//                log.error("Invalid or missing label in field: {}", field);
//                throw new ThedalException(ThedalError.INVALID_FORM_FIELDS, HttpStatus.BAD_REQUEST);
//            }
//
//            // Validate type
//            String type = (String) field.get("type");
//            if (!allowedTypes.contains(type)) {
//                log.error("Invalid field type: {}. Allowed types are {}", type, allowedTypes);
//                throw new ThedalException(ThedalError.INVALID_FORM_FIELDS, HttpStatus.BAD_REQUEST);
//            }
//
//            // Validate required field (optional, defaults to false)
//            Boolean required = field.containsKey("required") ? (Boolean) field.get("required") : false;
//            if (!(required instanceof Boolean)) {
//                log.error("Invalid 'required' value for field '{}'. Must be boolean", field.get("label"));
//                throw new ThedalException(ThedalError.INVALID_FORM_FIELDS, HttpStatus.BAD_REQUEST);
//            }
//            
//         // Validate orderIndex
//            if (!field.containsKey("orderIndex") || field.get("orderIndex") == null) {
//                log.error("Missing orderIndex in field: {}", field);
//                throw new ThedalException(ThedalError.INVALID_FORM_FIELDS, HttpStatus.BAD_REQUEST);
//            }
//            Integer orderIndex = (Integer) field.get("orderIndex");
//            if (orderIndex < 0 || !orderIndexes.add(orderIndex)) {
//                log.error("Invalid or duplicate orderIndex {} in field: {}", orderIndex, field);
//                throw new ThedalException(ThedalError.INVALID_FORM_FIELDS, HttpStatus.BAD_REQUEST);
//            }
//
//            // Validate options for dropdown, radio, check-box, and multi-select
//            if (List.of("dropdown", "radio", "check-box", "multi-select").contains(type)) {
//                if (!field.containsKey("options") || field.get("options") == null || 
//                    !(field.get("options") instanceof List) || ((List<?>) field.get("options")).isEmpty()) {
//                    log.error("Missing or invalid options for field type {} in field: {}", type, field);
//                    throw new ThedalException(ThedalError.INVALID_FORM_FIELDS, HttpStatus.BAD_REQUEST);
//                }
//                // Ensure options are non-empty strings
//                List<?> options = (List<?>) field.get("options");
//                for (Object option : options) {
//                    if (!(option instanceof String) || ((String) option).isBlank()) {
//                        log.error("Invalid option in field: {}. Options must be non-empty strings", field);
//                        throw new ThedalException(ThedalError.INVALID_FORM_FIELDS, HttpStatus.BAD_REQUEST);
//                    }
//                }
//            }
//
//            // No options required for string, number, boolean, image, or file
//            if (List.of("string", "number", "boolean", "image", "file").contains(type) && field.containsKey("options")) {
//                log.warn("Options provided for field type {} in field: {}. Ignoring options.", type, field);
//                field.remove("options"); // Clean up unnecessary options
//            }
//        }
//        
//     // Ensure orderIndexes are contiguous (0 to n-1)
//        if (!orderIndexes.equals(new HashSet<>(IntStream.range(0, customFields.size()).boxed().collect(Collectors.toSet())))) {
//            log.error("Order indexes must be contiguous from 0 to {}", customFields.size() - 1);
//            throw new ThedalException(ThedalError.INVALID_FORM_FIELDS, HttpStatus.BAD_REQUEST);
//        }
//        
//    }

    @Transactional
    public void updateSurveyFormOrder(List<SurveyFormReorderRequest> reorderRequests, Long accountId, Long electionId) {
        log.debug("Reordering survey forms for accountId={}, electionId={}", accountId, electionId);

        // Fetch survey forms sorted by orderIndex
        List<SurveyFormEntity> forms = surveyFormRepository.findByElectionIdAndAccountIdOrderByOrderIndexAsc(electionId, accountId);

        if (forms.isEmpty()) {
            log.error("No survey forms found for election ID {} and account ID {}", electionId, accountId);
            throw new ThedalException(ThedalError.SURVEY_FORM_NOT_FOUND, HttpStatus.NOT_FOUND);
        }

        // Create a map of formId -> newOrderIndex
        Map<Long, Integer> newOrderMap = reorderRequests.stream()
                .collect(Collectors.toMap(SurveyFormReorderRequest::getFormId, SurveyFormReorderRequest::getNewOrderIndex));

        // Sort reorder requests by newOrderIndex to avoid conflicts
        reorderRequests.sort(Comparator.comparingInt(SurveyFormReorderRequest::getNewOrderIndex));

        // Remove forms that are being reordered
        List<SurveyFormEntity> remainingForms = new ArrayList<>(forms);
        remainingForms.removeIf(form -> newOrderMap.containsKey(form.getId()));
        
     // Temporary list to hold reordered forms
        List<SurveyFormEntity> reorderedForms = new ArrayList<>(remainingForms);

        // Insert forms at their new positions
        for (SurveyFormReorderRequest request : reorderRequests) {
            SurveyFormEntity form = forms.stream()
                    .filter(f -> f.getId().equals(request.getFormId()))
                    .findFirst()
                    .orElseThrow(() -> new ThedalException(ThedalError.SURVEY_FORM_NOT_FOUND, HttpStatus.NOT_FOUND));

            // Ensure the new index is within bounds
            int newIndex = Math.min(request.getNewOrderIndex(), reorderedForms.size());
            reorderedForms.add(newIndex, form);
        }

        // Update orderIndex for all forms
        for (int i = 0; i < reorderedForms.size(); i++) {
            reorderedForms.get(i).setOrderIndex(i);
            log.info("Updated survey form order: {} -> {}", reorderedForms.get(i).getFormName(), i);
        }
        
        // Save updated order to both PostgreSQL and MongoDB with dual-write pattern
        try {
            surveyFormRepository.saveAll(reorderedForms);
            try {
                // Convert to MongoDB entities and save
                List<SurveyFormMongo> mongoForms = reorderedForms.stream()
                        .map(SurveyFormMongo::new)
                        .collect(Collectors.toList());
                surveyFormMongoRepository.saveAll(mongoForms);
                log.info("Successfully updated survey form order in MongoDB for electionId: {}", electionId);
            } catch (Exception mongoEx) {
                log.error("Failed to update survey form order in MongoDB for electionId: {}", electionId, mongoEx);
                throw new RuntimeException("MongoDB bulk update failed, triggering rollback", mongoEx);
            }
            log.info("Survey form order updated successfully for electionId: {}", electionId);
        } catch (Exception ex) {
            log.error("Failed to update survey form order for electionId: {}", electionId, ex);
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    
    @Transactional
    public SurveyFormDTO reorderFormFields(Long accountId, Long electionId, Long formId, List<FieldReorderRequest> reorderRequests) {
        log.debug("Reordering fields for formId={}, accountId={}, electionId={}", formId, accountId, electionId);

        validateElectionOwnership(electionId, accountId);

        SurveyFormEntity form = surveyFormRepository.findByIdAndAccountIdAndElectionId(formId, accountId, electionId)
                .orElseThrow(() -> new ThedalException(ThedalError.SURVEY_FORM_NOT_FOUND, HttpStatus.NOT_FOUND));

        List<Map<String, Object>> customFields = form.getCustomFields();
        if (customFields == null || customFields.isEmpty()) {
            log.error("No fields found for formId={}", formId);
            throw new ThedalException(ThedalError.INVALID_FORM_FIELDS, HttpStatus.BAD_REQUEST);
        }

        // Create a map of fieldLabel -> newOrderIndex
        Map<String, Integer> newOrderMap = reorderRequests.stream()
                .collect(Collectors.toMap(FieldReorderRequest::getFieldLabel, FieldReorderRequest::getNewOrderIndex));

        // Validate all requested fields exist
        for (FieldReorderRequest request : reorderRequests) {
            if (customFields.stream().noneMatch(field -> request.getFieldLabel().equals(field.get("label")))) {
                log.error("Field with label {} not found in formId={}", request.getFieldLabel(), formId);
                throw new ThedalException(ThedalError.INVALID_FORM_FIELDS, HttpStatus.BAD_REQUEST);
            }
        }

        // Sort reorder requests by newOrderIndex
        reorderRequests.sort(Comparator.comparingInt(FieldReorderRequest::getNewOrderIndex));

        // Create a new list for reordered fields
        List<Map<String, Object>> reorderedFields = new ArrayList<>();
        Set<String> processedLabels = new HashSet<>();

        // Insert fields at their new positions
        for (FieldReorderRequest request : reorderRequests) {
            Map<String, Object> field = customFields.stream()
                    .filter(f -> request.getFieldLabel().equals(f.get("label")))
                    .findFirst()
                    .orElseThrow(() -> new ThedalException(ThedalError.INVALID_FORM_FIELDS, HttpStatus.BAD_REQUEST));
            field.put("orderIndex", request.getNewOrderIndex());
            reorderedFields.add(field);
            processedLabels.add(request.getFieldLabel());
        }

        // Add remaining fields (not reordered) in their original order
        for (Map<String, Object> field : customFields) {
            if (!processedLabels.contains(field.get("label"))) {
                reorderedFields.add(field);
            }
        }

        // Reassign orderIndex to ensure contiguous values (0 to n-1)
        for (int i = 0; i < reorderedFields.size(); i++) {
            reorderedFields.get(i).put("orderIndex", i);
        }

        // Update and save the form with dual-write pattern
        form.setCustomFields(reorderedFields);
        try {
            SurveyFormEntity updatedEntity = surveyFormRepository.save(form);
            try {
                SurveyFormMongo surveyFormMongo = new SurveyFormMongo(updatedEntity);
                surveyFormMongoRepository.save(surveyFormMongo);
                log.info("Successfully updated form fields in MongoDB: formId={}", formId);
            } catch (Exception mongoEx) {
                log.error("Failed to update form fields in MongoDB: formId={}", formId, mongoEx);
                throw new RuntimeException("MongoDB update failed, triggering rollback", mongoEx);
            }
            log.info("Fields reordered for formId={}", formId);

            return new SurveyFormDTO(
                updatedEntity.getId(),
                updatedEntity.getFormName(),
                updatedEntity.getFormDescription(),
                updatedEntity.getCustomFields(),
                updatedEntity.getIsActive(),
                updatedEntity.getOrderIndex(),
                updatedEntity.getCreatedTime(),
                updatedEntity.getModifiedTime(),
                electionId
            );
        } catch (Exception ex) {
            log.error("Failed to reorder form fields: formId={}", formId, ex);
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
////////////////////////////////////////////////////////////////    

    
    @Transactional
    public SurveyFormSubmissionDTO submitForm(Long accountId, Long electionId, Long formId, SurveyFormSubmissionDTO submissionDTO) {
        log.debug("Submitting form data for accountId={}, electionId={}, formId={}", accountId, electionId, formId);
        log.debug("Input submissionData: {}", submissionDTO.getSubmissionData());

        // Validate election ownership
        validateElectionOwnership(electionId, accountId);

        // Fetch the form to validate submission data
        SurveyFormEntity form = surveyFormRepository.findByIdAndAccountIdAndElectionId(formId, accountId, electionId)
                .orElseThrow(() -> new ThedalException(ThedalError.SURVEY_FORM_NOT_FOUND, HttpStatus.NOT_FOUND));

        // Validate submission data against form's custom fields
        validateSubmissionData(form.getCustomFields(), submissionDTO.getSubmissionData());

        // Process image fields (if any)
        Map<String, Object> submissionData = submissionDTO.getSubmissionData();
        for (Map<String, Object> field : form.getCustomFields()) {
            String fieldType = (String) field.get("type");
            String fieldLabel = (String) field.get("label");
            if ("image".equals(fieldType) && submissionData.containsKey(fieldLabel)) {
                Object imageData = submissionData.get(fieldLabel);
                String s3Url = uploadImageToAWS(imageData, accountId, electionId, formId, fieldLabel);
                submissionData.put(fieldLabel, s3Url);
            }
        }

        // Create and save submission entity
        SurveyFormSubmissionEntity submissionEntity = new SurveyFormSubmissionEntity();
        submissionEntity.setFormId(formId);
        submissionEntity.setElectionId(electionId);
        submissionEntity.setAccountId(accountId);
        submissionEntity.setSubmissionData(submissionDTO.getSubmissionData());

        log.debug("Submission data before saving: {}", submissionEntity.getSubmissionData());

        SurveyFormSubmissionEntity savedEntity = surveyFormSubmissionRepository.save(submissionEntity);
        log.info("Form submission saved with id={}", savedEntity.getId());
        log.debug("Saved submission data: {}", savedEntity.getSubmissionData());

        return new SurveyFormSubmissionDTO(savedEntity.getSubmissionData());
    }

    private void validateSubmissionData(List<Map<String, Object>> customFields, Map<String, Object> submissionData) {
        // Check if submissionData is null
        if (submissionData == null) {
            log.error("Submission data is null");
            throw new ThedalException(ThedalError.INVALID_SUBMISSION_DATA, HttpStatus.BAD_REQUEST);
        }

        // Log all submitted fields for debugging
        log.debug("Submitted fields: {}", submissionData.keySet());

        // Validate each custom field
        for (Map<String, Object> field : customFields) {
            String label = (String) field.get("label");
            String type = (String) field.get("type");
            Boolean required = field.containsKey("required") ? (Boolean) field.get("required") : false;
            Object value = submissionData.get(label);

            // Check required fields
            if (required) {
                if (value == null) {
                    log.error("Required field '{}' is null", label);
                    throw new ThedalException(ThedalError.INVALID_SUBMISSION_DATA, HttpStatus.BAD_REQUEST);
                }
                if (value instanceof String && ((String) value).isBlank()) {
                    log.error("Required field '{}' is blank", label);
                    throw new ThedalException(ThedalError.INVALID_SUBMISSION_DATA, HttpStatus.BAD_REQUEST);
                }
                if (value instanceof List && ((List<?>) value).isEmpty()) {
                    log.error("Required field '{}' is an empty list", label);
                    throw new ThedalException(ThedalError.INVALID_SUBMISSION_DATA, HttpStatus.BAD_REQUEST);
                }
            }

            // Validate field types if value is provided
            if (value != null) {
                switch (type) {
                    case "string":
                        if (!(value instanceof String)) {
                            log.error("Field '{}' must be a string, got {}", label, value.getClass().getSimpleName());
                            throw new ThedalException(ThedalError.INVALID_SUBMISSION_DATA, HttpStatus.BAD_REQUEST);
                        }
                        break;
                    case "number":
                        if (!(value instanceof Number)) {
                            log.error("Field '{}' must be a number, got {}", label, value.getClass().getSimpleName());
                            throw new ThedalException(ThedalError.INVALID_SUBMISSION_DATA, HttpStatus.BAD_REQUEST);
                        }
                        break;
                    case "boolean":
                        if (!(value instanceof Boolean)) {
                            log.error("Field '{}' must be a boolean, got {}", label, value.getClass().getSimpleName());
                            throw new ThedalException(ThedalError.INVALID_SUBMISSION_DATA, HttpStatus.BAD_REQUEST);
                        }
                        break;
                    case "radio":
                    case "dropdown":
                        if (!(value instanceof String)) {
                            log.error("Field '{}' must be a string, got {}", label, value.getClass().getSimpleName());
                            throw new ThedalException(ThedalError.INVALID_SUBMISSION_DATA, HttpStatus.BAD_REQUEST);
                        }
                        List<String> options = (List<String>) field.get("options");
                        if (options == null || !options.contains(value)) {
                            log.error("Field '{}' has invalid option: {}. Valid options: {}", label, value, options);
                            throw new ThedalException(ThedalError.INVALID_SUBMISSION_DATA, HttpStatus.BAD_REQUEST);
                        }
                        break;
                    case "check-box":
                    case "multi-select":
                        if (!(value instanceof List)) {
                            log.error("Field '{}' must be a list, got {}", label, value.getClass().getSimpleName());
                            throw new ThedalException(ThedalError.INVALID_SUBMISSION_DATA, HttpStatus.BAD_REQUEST);
                        }
                        List<?> submittedOptions = (List<?>) value;
                        List<String> validOptions = (List<String>) field.get("options");
                        if (validOptions == null) {
                            log.error("Field '{}' has no valid options defined", label);
                            throw new ThedalException(ThedalError.INVALID_FORM_FIELDS, HttpStatus.BAD_REQUEST);
                        }
                        for (Object option : submittedOptions) {
                            if (!(option instanceof String) || !validOptions.contains(option)) {
                                log.error("Field '{}' has invalid option: {}. Valid options: {}", label, option, validOptions);
                                throw new ThedalException(ThedalError.INVALID_SUBMISSION_DATA, HttpStatus.BAD_REQUEST);
                            }
                        }
                        break;
                    case "image":
                    case "file":
                        if (!(value instanceof String)) {
                            log.error("Field '{}' must be a string (URL or base64), got {}", label, value.getClass().getSimpleName());
                            throw new ThedalException(ThedalError.INVALID_SUBMISSION_DATA, HttpStatus.BAD_REQUEST);
                        }
                        break;
                    default:
                        log.error("Unsupported field type: {} for field '{}'", type, label);
                        throw new ThedalException(ThedalError.INVALID_FORM_FIELDS, HttpStatus.BAD_REQUEST);
                }
            }
        }

        // Log unknown fields for debugging (do not remove them)
        List<String> validFieldLabels = customFields.stream()
                .map(field -> (String) field.get("label"))
                .collect(Collectors.toList());
        submissionData.keySet().stream()
                .filter(key -> !validFieldLabels.contains(key))
                .forEach(key -> log.warn("Unknown field in submission: {}", key));
    }

    
    
    @Transactional
    public SurveyFormSubmissionsPageDTO getFormSubmissions(Long accountId, Long electionId, Long formId, Pageable pageable) {
        log.debug("Fetching submissions for accountId={}, electionId={}, formId={}", accountId, electionId, formId);

        // Validate election ownership
        validateElectionOwnership(electionId, accountId);

        // Fetch the form to ensure it exists
        SurveyFormEntity form = surveyFormRepository.findByIdAndAccountIdAndElectionId(formId, accountId, electionId)
                .orElseThrow(() -> new ThedalException(ThedalError.SURVEY_FORM_NOT_FOUND, HttpStatus.NOT_FOUND));

        // Fetch submissions
        Page<SurveyFormSubmissionEntity> submissionsPage = surveyFormSubmissionRepository
                .findByFormIdAndElectionIdAndAccountId(formId, electionId, accountId, pageable);

        return new SurveyFormSubmissionsPageDTO(submissionsPage);
    }
    
//    @Transactional
//    public SurveyExportResponse initiateSurveyExport(Long accountId, Long electionId, Long formId,
//                                                    Integer limit) {
//        validateElectionOwnership(electionId, accountId);
//
//        if (!surveyFormRepository.existsByIdAndAccountIdAndElectionId(formId, accountId, electionId)) {
//            throw new ThedalException(ThedalError.SURVEY_FORM_NOT_FOUND, HttpStatus.NOT_FOUND);
//        }
//
//        SurveyDownloadJob job = new SurveyDownloadJob();
//        job.setAccountId(accountId);
//        job.setElectionId(electionId);
//        job.setFormId(formId);
//        job.setTimeStarted(LocalDateTime.now());
//        job.setStatus("IN_PROGRESS");
//        surveyDownloadJobRepository.saveAndFlush(job);
//
//        jobSchedulerService.scheduleSurveyExportJob(job.getId(), accountId, electionId, formId, limit);
//
//        return new SurveyExportResponse(job.getId());
//    }
//
//    @Transactional
//    public void processSurveyExport(Long jobId, Long accountId, Long electionId, Long formId,
//                                   List<String> voterIds, Integer limit) {
//        SurveyDownloadJob job = surveyDownloadJobRepository.findById(jobId)
//                .orElseThrow(() -> new ThedalException(ThedalError.JOB_NOT_FOUND, HttpStatus.NOT_FOUND));
//
//        File tempFile = null;
//        try {
//            log.info("Starting survey export for job {}", jobId);
//
//            Specification<SurveyFormSubmissionEntity> spec = buildSurveySpecification(electionId, accountId, formId);
//            tempFile = generateSurveyExcelFileStreamed(spec, formId, limit);
//
//            try (FileInputStream inputStream = new FileInputStream(tempFile)) {
//                String fileKey = "survey_exports/survey_export_" + jobId + ".xlsx";
////                String fileUrl = awsFileUpload.uploadToAWS(inputStream, fileKey, s3bucket);
////
////                job.setAwsS3DownloadUrl(fileUrl);
//                String presignedUrl = awsFileUpload.generatePresignedUrl(s3bucket, fileKey, 24 * 3600);
//                job.setAwsS3DownloadUrl(presignedUrl);
//                job.setStatus("COMPLETED");
//                job.setTimeCompleted(LocalDateTime.now());
//                surveyDownloadJobRepository.saveAndFlush(job);
//
//                log.info("Completed survey export for job {}", jobId);
//            }
//        } catch (Exception e) {
//            String errorMsg = "Survey export failed: " + e.getMessage();
//            job.setStatus("FAILED");
//            job.setErrorMessage(errorMsg);
//            job.setTimeCompleted(LocalDateTime.now());
//            surveyDownloadJobRepository.saveAndFlush(job);
//            log.error("Survey export failed for job {}: {}", jobId, e.getMessage(), e);
//            throw new ThedalException(ThedalError.EXPORT_FAILED, HttpStatus.INTERNAL_SERVER_ERROR, errorMsg);
//        } finally {
//            if (tempFile != null && tempFile.exists()) {
//                if (!tempFile.delete()) {
//                    log.warn("Failed to delete temporary file: {}", tempFile.getAbsolutePath());
//                }
//            }
//        }
//    }
    
    @Transactional
    public SurveyExportResponse initiateSurveyExport(Long accountId, Long electionId, Long formId,
                                                    Integer limit) {
        validateElectionOwnership(electionId, accountId);

        if (!surveyFormRepository.existsByIdAndAccountIdAndElectionId(formId, accountId, electionId)) {
            throw new ThedalException(ThedalError.SURVEY_FORM_NOT_FOUND, HttpStatus.NOT_FOUND);
        }

        SurveyDownloadJob job = new SurveyDownloadJob();
        job.setAccountId(accountId);
        job.setElectionId(electionId);
        job.setFormId(formId);
        job.setTimeStarted(LocalDateTime.now());
        job.setStatus("IN_PROGRESS");
        surveyDownloadJobRepository.saveAndFlush(job);

        jobSchedulerService.scheduleSurveyExportJob(job.getId(), accountId, electionId, formId, limit);

        return new SurveyExportResponse(job.getId());
    }

    @Transactional
    public void processSurveyExport(Long jobId, Long accountId, Long electionId, Long formId,
                                   List<String> voterIds, Integer limit) {
        SurveyDownloadJob job = surveyDownloadJobRepository.findById(jobId)
                .orElseThrow(() -> new ThedalException(ThedalError.JOB_NOT_FOUND, HttpStatus.NOT_FOUND));

        File tempFile = null;
        try {
            log.info("Starting survey export for job {}", jobId);

            Specification<SurveyFormSubmissionEntity> spec = buildSurveySpecification(electionId, accountId, formId);
            tempFile = generateSurveyExcelFileStreamed(spec, formId, limit);

            try (FileInputStream inputStream = new FileInputStream(tempFile)) {
                String fileKey = "survey_exports/survey_export_" + jobId + ".xlsx";
                String fileUrl = awsFileUpload.uploadToAWS(inputStream, fileKey, s3bucket);

                job.setAwsS3DownloadUrl(fileUrl);
                job.setStatus("COMPLETED");
                job.setTimeCompleted(LocalDateTime.now());
                surveyDownloadJobRepository.saveAndFlush(job);

                log.info("Completed survey export for job {}", jobId);
            }
        } catch (Exception e) {
            String errorMsg = "Survey export failed: " + e.getMessage();
            job.setStatus("FAILED");
            job.setErrorMessage(errorMsg);
            job.setTimeCompleted(LocalDateTime.now());
            surveyDownloadJobRepository.saveAndFlush(job);
            log.error("Survey export failed for job {}: {}", jobId, e.getMessage(), e);
            throw new ThedalException(ThedalError.EXPORT_FAILED, HttpStatus.INTERNAL_SERVER_ERROR, errorMsg);
        } finally {
            if (tempFile != null && tempFile.exists()) {
                if (!tempFile.delete()) {
                    log.warn("Failed to delete temporary file: {}", tempFile.getAbsolutePath());
                }
            }
        }
    }

//    private Specification<SurveyFormSubmissionEntity> buildSurveySpecification(Long electionId, Long accountId,
//                                                                             Long formId, List<String> voterIds) {
//        Specification<SurveyFormSubmissionEntity> spec = Specification.where(
//                SurveySpecifications.hasElectionId(electionId))
//                .and(SurveySpecifications.hasAccountId(accountId))
//                .and(SurveySpecifications.hasFormId(formId));
//        if (voterIds != null) {
//            spec = spec.and(SurveySpecifications.hasVoterIds(voterIds));
//        }
//        return spec;
//    }
    private Specification<SurveyFormSubmissionEntity> buildSurveySpecification(Long electionId, Long accountId,
            Long formId) {
     Specification<SurveyFormSubmissionEntity> spec = Specification.where(
        SurveySpecifications.hasElectionId(electionId))
        .and(SurveySpecifications.hasAccountId(accountId))
        .and(SurveySpecifications.hasFormId(formId));
        
        return spec;
        }

//    private File generateSurveyExcelFileStreamed(Specification<SurveyFormSubmissionEntity> spec, Long formId, Integer limit) throws IOException {
//        System.setProperty("java.awt.headless", "true");
//
//        File outputFile = File.createTempFile("surveyexport", ".xlsx");
//        int processed = 0;
//        int page = 0;
//
//        SurveyFormEntity form = surveyFormRepository.findById(formId)
//                .orElseThrow(() -> new ThedalException(ThedalError.SURVEY_FORM_NOT_FOUND, HttpStatus.NOT_FOUND));
//        List<Map<String, Object>> customFields = form.getCustomFields();
//
////        int totalRecords;
////        if (limit == null) {
////            totalRecords = (int) surveyFormSubmissionRepository.count();
////        } else {
////            totalRecords = Math.min(limit, (int) surveyFormSubmissionRepository.count());
////        }
//        int totalRecords = limit == null ? (int) surveyFormSubmissionRepository.count(spec) 
//                : Math.min(limit, (int) surveyFormSubmissionRepository.count(spec));
//        
//        if (totalRecords == 0) {
//            throw new ThedalException(ThedalError.SUBMISSIONS_NOT_FOUND, HttpStatus.NOT_FOUND);
//        }
//
//        log.info("Starting export of {} survey submission records", totalRecords);
//
//        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100);
//             FileOutputStream fos = new FileOutputStream(outputFile)) {
//
//            workbook.setCompressTempFiles(true);
//            Sheet sheet = workbook.createSheet("SurveySubmissions");
//
//            int columnCount = 4 + (customFields != null ? customFields.size() : 0);
//            for (int i = 0; i < columnCount; i++) {
//                sheet.setColumnWidth(i, 15 * 256);
//            }
//
//            Row headerRow = sheet.createRow(0);
//            SurveyExcelHeader.createHeaderRow(headerRow, customFields);
//
//            int rowNum = 1;
//            while (processed < totalRecords) {
//                Pageable pageable = PageRequest.of(page, BATCH_SIZE);
//                //Page<SurveyFormSubmissionEntity> submissionPage = surveyFormSubmissionRepository.findAll(pageable);
//                Page<SurveyFormSubmissionEntity> submissionPage = surveyFormSubmissionRepository.findAll(spec, pageable);
//
//                if (submissionPage.isEmpty()) {
//                    break;
//                }
//
//                for (SurveyFormSubmissionEntity submission : submissionPage.getContent()) {
//                    Row row = sheet.createRow(rowNum++);
//                    SurveyExcelDataRow.populateDataRow(row, submission, customFields);
//                    processed++;
//
//                    if (rowNum % 100 == 0) {
//                        ((SXSSFSheet) sheet).flushRows();
//                    }
//
//                    if (limit != null && processed >= limit) {
//                        break;
//                    }
//                }
//
//                page++;
//            }
//
//            ((SXSSFSheet) sheet).flushRows();
//            workbook.write(fos);
//            fos.flush();
//            workbook.dispose();
//
//            log.info("Generated Excel file with {} records", processed);
//            return outputFile;
//        } catch (Exception e) {
//            if (outputFile != null && outputFile.exists()) {
//                if (!outputFile.delete()) {
//                    log.warn("Failed to delete temporary file: {}", outputFile.getAbsolutePath());
//                }
//            }
//            throw new IOException("Failed to generate Excel file: " + e.getMessage(), e);
//        }
//    }
    private File generateSurveyExcelFileStreamed(Specification<SurveyFormSubmissionEntity> spec, Long formId, Integer limit) throws IOException {
        File outputFile = File.createTempFile("surveyexport", ".xlsx");
        int processed = 0;
        int page = 0;

        SurveyFormEntity form = surveyFormRepository.findById(formId)
                .orElseThrow(() -> new ThedalException(ThedalError.SURVEY_FORM_NOT_FOUND, HttpStatus.NOT_FOUND));
        List<Map<String, Object>> customFields = form.getCustomFields();

        int totalRecords = limit == null ? (int) surveyFormSubmissionRepository.count(spec)
                : Math.min(limit, (int) surveyFormSubmissionRepository.count(spec));

        if (totalRecords == 0) {
            throw new ThedalException(ThedalError.SUBMISSIONS_NOT_FOUND, HttpStatus.NOT_FOUND);
        }

        // Collect extra fields (optional)
        Set<String> extraFields = new HashSet<>();
        Pageable allPageable = PageRequest.of(0, Integer.MAX_VALUE);
        Page<SurveyFormSubmissionEntity> allSubmissions = surveyFormSubmissionRepository.findAll(spec, allPageable);
        for (SurveyFormSubmissionEntity submission : allSubmissions.getContent()) {
            if (submission.getSubmissionData() != null) {
                submission.getSubmissionData().keySet().forEach(key -> extraFields.add(key.trim()));
            }
        }

        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100);
             FileOutputStream fos = new FileOutputStream(outputFile)) {
            workbook.setCompressTempFiles(true);
            Sheet sheet = workbook.createSheet("SurveySubmissions");

            int columnCount = 3 + (customFields != null ? customFields.size() : 0) + extraFields.size();
            for (int i = 0; i < columnCount; i++) {
                sheet.setColumnWidth(i, 15 * 256);
            }

            Row headerRow = sheet.createRow(0);
            SurveyExcelHeader.createHeaderRow(headerRow, customFields, extraFields);

            int rowNum = 1;
            while (processed < totalRecords) {
                Pageable pageable = PageRequest.of(page, BATCH_SIZE);
                Page<SurveyFormSubmissionEntity> submissionPage = surveyFormSubmissionRepository.findAll(spec, pageable);

                if (submissionPage.isEmpty()) {
                    break;
                }

                for (SurveyFormSubmissionEntity submission : submissionPage.getContent()) {
                    Row row = sheet.createRow(rowNum++);
                    SurveyExcelDataRow.populateDataRow(row, submission, customFields, extraFields);
                    processed++;

                    if (rowNum % 100 == 0) {
                        ((SXSSFSheet) sheet).flushRows();
                    }

                    if (limit != null && processed >= limit) {
                        break;
                    }
                }
                page++;
            }

            ((SXSSFSheet) sheet).flushRows();
            workbook.write(fos);
            fos.flush();
            workbook.dispose();

            log.info("Generated Excel file with {} records", processed);
            return outputFile;
        } catch (Exception e) {
            if (outputFile.exists()) {
                if (!outputFile.delete()) {
                    log.warn("Failed to delete temporary file: {}", outputFile.getAbsolutePath());
                }
            }
            throw new IOException("Failed to generate Excel file: " + e.getMessage(), e);
        }
    }

    
    
    @Transactional
    public List<SurveyExportJobDetailsDTO> getSurveyExportJobs(Long accountId, Long electionId, Long jobId) {
        log.debug("Fetching survey export jobs for accountId={}, electionId={}, jobId={}", accountId, electionId, jobId);

        validateElectionOwnership(electionId, accountId);

        if (jobId != null) {
            SurveyDownloadJob job = surveyDownloadJobRepository.findByIdAndAccountIdAndElectionId(jobId, accountId, electionId)
                    .orElseThrow(() -> new ThedalException(ThedalError.JOB_NOT_FOUND, HttpStatus.NOT_FOUND));
            return List.of(mapToJobDetailsDTO(job));
        } else {
            List<SurveyDownloadJob> jobs = surveyDownloadJobRepository.findByElectionIdAndAccountId(electionId, accountId);
            return jobs.stream()
                    .map(this::mapToJobDetailsDTO)
                    .collect(Collectors.toList());
        }
    }

    private SurveyExportJobDetailsDTO mapToJobDetailsDTO(SurveyDownloadJob job) {
        return new SurveyExportJobDetailsDTO(
                job.getId(),
                job.getAccountId(),
                job.getElectionId(),
                job.getFormId(),
                job.getStatus(),
                job.getTimeStarted(),
                job.getTimeCompleted(),
                job.getAwsS3DownloadUrl(),
                job.getErrorMessage()
        );
    }

    private String uploadImageToAWS(Object imageData, Long accountId, Long electionId, Long formId, String fieldLabel) {
        String fileName;
        InputStream inputStream;
        String contentType;

        // Generate a unique file name
        String fileExtension = ".jpg"; // Default to JPEG, adjust based on content type
        fileName = String.format("survey_images/%d/%d/%d/%s_%s", accountId, electionId, formId, fieldLabel, UUID.randomUUID().toString() + fileExtension);

        try {
            if (imageData instanceof MultipartFile) {
                // Handle web upload (MultipartFile)
                MultipartFile imageFile = (MultipartFile) imageData;
                contentType = imageFile.getContentType();
                if (!(MediaType.IMAGE_JPEG_VALUE.equals(contentType) || MediaType.IMAGE_PNG_VALUE.equals(contentType))) {
                    log.error("Invalid image format: {}", contentType);
                    throw new ThedalException(ThedalError.INVALID_IMAGE_FORMAT, HttpStatus.BAD_REQUEST);
                }

                long maxFileSize = 5 * 1024 * 1024; // 5MB
                if (imageFile.getSize() > maxFileSize) {
                    log.error("Image size exceeds 5MB: {}", imageFile.getSize());
                    throw new ThedalException(ThedalError.INVALID_IMAGE_SIZE, HttpStatus.BAD_REQUEST);
                }

                fileExtension = "." + awsFileUpload.getFileExtension(imageFile.getOriginalFilename());
                fileName = fileName.replace(".jpg", fileExtension);
                inputStream = imageFile.getInputStream();
            } else if (imageData instanceof String) {
                // Handle Android upload (base64-encoded string)
                String base64Data = (String) imageData;
                if (base64Data.startsWith("data:image")) {
                    String[] parts = base64Data.split(",");
                    contentType = parts[0].split(";")[0].replace("data:", "");
                    if (!(contentType.equals(MediaType.IMAGE_JPEG_VALUE) || contentType.equals(MediaType.IMAGE_PNG_VALUE))) {
                        log.error("Invalid base64 image format: {}", contentType);
                        throw new ThedalException(ThedalError.INVALID_IMAGE_FORMAT, HttpStatus.BAD_REQUEST);
                    }

                    byte[] decodedBytes = Base64.getDecoder().decode(parts[1]);
                    if (decodedBytes.length > 5 * 1024 * 1024) { // 5MB
                        log.error("Base64 image size exceeds 5MB");
                        throw new ThedalException(ThedalError.INVALID_IMAGE_SIZE, HttpStatus.BAD_REQUEST);
                    }

                    fileExtension = contentType.equals(MediaType.IMAGE_PNG_VALUE) ? ".png" : ".jpg";
                    fileName = fileName.replace(".jpg", fileExtension);
                    inputStream = new ByteArrayInputStream(decodedBytes);
                } else {
                    log.error("Invalid image data: not a base64 string");
                    throw new ThedalException(ThedalError.INVALID_IMAGE_DATA, HttpStatus.BAD_REQUEST);
                }
            } else {
                log.error("Unsupported image data type: {}", imageData.getClass().getName());
                throw new ThedalException(ThedalError.INVALID_IMAGE_DATA, HttpStatus.BAD_REQUEST);
            }

            // Upload to AWS S3
            String awsUrl = awsFileUpload.uploadToAWS(inputStream, fileName, s3bucket);
            log.info("Uploaded image to S3: {}", awsUrl);
            return awsUrl;

        } catch (IOException e) {
            log.error("Error uploading image to AWS S3: {}", e.getMessage());
            throw new ThedalException(ThedalError.UPLOAD_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @Transactional
    public void deleteSurveyExportJobs(Long accountId, Long electionId, List<Long> jobIds) {
        log.debug("Deleting survey export jobs for accountId={}, electionId={}, jobIds={}", accountId, electionId, jobIds);

        validateElectionOwnership(electionId, accountId);

        if (jobIds == null || jobIds.isEmpty()) {
            // Delete all export jobs for the election
            int deletedCount = surveyDownloadJobRepository.deleteByElectionIdAndAccountId(electionId, accountId);
            if (deletedCount == 0) {
                log.warn("No survey export jobs found to delete for accountId: {}, electionId: {}", accountId, electionId);
                throw new ThedalException(ThedalError.JOB_NOT_FOUND, HttpStatus.NOT_FOUND);
            }
            log.info("Deleted {} survey export jobs for accountId: {}, electionId: {}", deletedCount, accountId, electionId);
        } else {
            // Delete specific export jobs
            List<SurveyDownloadJob> jobs = surveyDownloadJobRepository.findByIdInAndElectionIdAndAccountId(jobIds, electionId, accountId);
            if (jobs.isEmpty()) {
                log.warn("No survey export jobs found for given IDs: {}", jobIds);
                throw new ThedalException(ThedalError.JOB_NOT_FOUND, HttpStatus.NOT_FOUND);
            }
            surveyDownloadJobRepository.deleteAll(jobs);
            log.info("Deleted survey export jobs with IDs: {}", jobIds);
        }
    }
    
    @Transactional
    public void deleteSurveyFormSubmissions(Long accountId, Long electionId, Long formId, List<Long> submissionIds) {
        log.debug("Deleting survey form submissions for accountId={}, electionId={}, formId={}, submissionIds={}", 
                  accountId, electionId, formId, submissionIds);

        validateElectionOwnership(electionId, accountId);

        SurveyFormEntity form = surveyFormRepository.findByIdAndAccountIdAndElectionId(formId, accountId, electionId)
                .orElseThrow(() -> new ThedalException(ThedalError.SURVEY_FORM_NOT_FOUND, HttpStatus.NOT_FOUND));

        if (submissionIds == null || submissionIds.isEmpty()) {
            // Delete all submissions for the form
            int deletedCount = surveyFormSubmissionRepository.deleteByFormIdAndElectionIdAndAccountId(formId, electionId, accountId);
            if (deletedCount == 0) {
                log.warn("No submissions found to delete for formId: {}, electionId: {}, accountId: {}", 
                         formId, electionId, accountId);
                throw new ThedalException(ThedalError.SUBMISSIONS_NOT_FOUND, HttpStatus.NOT_FOUND);
            }
            log.info("Deleted {} submissions for formId: {}, electionId: {}, accountId: {}", 
                     deletedCount, formId, electionId, accountId);
        } else {
            // Delete specific submissions
            List<SurveyFormSubmissionEntity> submissions = surveyFormSubmissionRepository
                    .findByIdInAndFormIdAndElectionIdAndAccountId(submissionIds, formId, electionId, accountId);
            if (submissions.isEmpty()) {
                log.warn("No submissions found for given IDs: {}", submissionIds);
                throw new ThedalException(ThedalError.SUBMISSIONS_NOT_FOUND, HttpStatus.NOT_FOUND);
            }
            surveyFormSubmissionRepository.deleteAll(submissions);
            log.info("Deleted submissions with IDs: {}", submissionIds);
        }
    }
    
}