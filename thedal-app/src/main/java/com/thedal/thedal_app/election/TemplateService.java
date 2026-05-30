package com.thedal.thedal_app.election;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.thedal.thedal_app.awsfilestore.AwsFileUpload;
import com.thedal.thedal_app.election.dtos.TemplateDTO;
import com.thedal.thedal_app.election.dtos.TemplateReorderRequest;
import com.thedal.thedal_app.election.dtos.TemplateUpdateDto;
import com.thedal.thedal_app.election.dtos.UpdateTemplateDTO;
import com.thedal.thedal_app.election.dtos.UpdateTemplateStatusRequest;
import com.thedal.thedal_app.response.ThedalResponse;
import com.thedal.thedal_app.response.ThedalSuccess;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;
import com.thedal.thedal_app.util.RandomTokenGenerator;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class TemplateService {

    @Autowired
    private AwsFileUpload awsFileUpload;
    @Value("${aws.s3.files.bucket}")
	private String s3bucket; 
    @Autowired
    private TemplateRepository templateRepository;
    @Autowired
    private ElectionRepository electionRepository;
    @Autowired
    private TemplateMongoRepository templateMongoRepository;
    
    private void validateElectionOwnership(Long electionId, Long accountId) {
        Optional<ElectionEntity> electionOpt = electionRepository.findByIdAndAccountId(electionId, accountId);
        if (!electionOpt.isPresent()) {
            log.error("Election ID {} does not belong to Account ID {}", electionId, accountId);
            throw new ThedalException(ThedalError.INVALID_ELECTION, HttpStatus.FORBIDDEN);  
        }
    }
    
    private void ensureOneActiveTemplate(Long electionId, Long accountId) {
        // Check PostgreSQL for active templates
        List<TemplateEntity> activeTemplates = templateRepository.findByElectionIdAndAccountIdAndIsActive(electionId, accountId, true);
        if (activeTemplates.size() > 1) {
            // Keep the first active template, deactivate others
            for (int i = 1; i < activeTemplates.size(); i++) {
                TemplateEntity template = activeTemplates.get(i);
                template.setIsActive(false);
                template.setImageStatus(false);
                TemplateEntity savedTemplate = templateRepository.save(template);
                // Sync with MongoDB
                try {
                    TemplateMongo templateMongo = new TemplateMongo(savedTemplate);
                    templateMongoRepository.save(templateMongo);
                    log.info("Deactivated extra template in MongoDB: id={}, name={}", savedTemplate.getId(), savedTemplate.getTemplateName());
                } catch (Exception mongoEx) {
                    log.error("Failed to deactivate extra template in MongoDB: id={}, name={}", savedTemplate.getId(), savedTemplate.getTemplateName(), mongoEx);
                    throw new RuntimeException("MongoDB deactivation failed, triggering rollback", mongoEx);
                }
            }
        } else if (activeTemplates.isEmpty()) {
            // Activate or create a default template
            Optional<TemplateEntity> defaultTemplateOpt = templateRepository.findByElectionIdAndAccountIdAndTemplateName(electionId, accountId, "Default");
            TemplateEntity defaultTemplate;
            if (defaultTemplateOpt.isPresent()) {
                defaultTemplate = defaultTemplateOpt.get();
                defaultTemplate.setIsActive(true);
                defaultTemplate.setImageStatus(false);
            } else {
                log.warn("No default template found for electionId: {}. Creating a new default template.", electionId);
                defaultTemplate = new TemplateEntity();
                defaultTemplate.setTemplateId(1L);
                defaultTemplate.setAccountId(accountId);
                defaultTemplate.setElectionId(electionId);
                defaultTemplate.setTemplateName("Default");
                defaultTemplate.setIsActive(true);
                defaultTemplate.setImageStatus(false);
                defaultTemplate.setOrderIndex(0);
                defaultTemplate.setSlipId(UUID.randomUUID().toString());
                defaultTemplate.setVoterSlipHeader("Default Voter Slip Header");
                defaultTemplate.setCandidateInfoImageFooter("Default Candidate Info Footer");
            }
            TemplateEntity savedTemplate = templateRepository.save(defaultTemplate);
            // Sync with MongoDB
            try {
                TemplateMongo templateMongo = new TemplateMongo(savedTemplate);
                templateMongoRepository.save(templateMongo);
                log.info("Activated/Created default template in MongoDB: id={}, name={}", savedTemplate.getId(), savedTemplate.getTemplateName());
            } catch (Exception mongoEx) {
                log.error("Failed to sync default template to MongoDB: id={}, name={}", savedTemplate.getId(), savedTemplate.getTemplateName(), mongoEx);
                throw new RuntimeException("MongoDB sync failed, triggering rollback", mongoEx);
            }
        }

        // Validate MongoDB state
        List<TemplateMongo> activeMongoTemplates = templateMongoRepository.findByAccountIdAndElectionIdAndIsActive(accountId, electionId, true);
        if (activeMongoTemplates.size() > 1) {
            // Deactivate extra active templates in MongoDB
            for (int i = 1; i < activeMongoTemplates.size(); i++) {
                TemplateMongo mongoTemplate = activeMongoTemplates.get(i);
                mongoTemplate.setIsActive(false);
                mongoTemplate.setImageStatus(false);
                try {
                    templateMongoRepository.save(mongoTemplate);
                    log.info("Deactivated extra MongoDB template: id={}, name={}", mongoTemplate.getId(), mongoTemplate.getTemplateName());
                } catch (Exception mongoEx) {
                    log.error("Failed to deactivate extra MongoDB template: id={}, name={}", mongoTemplate.getId(), mongoTemplate.getTemplateName(), mongoEx);
                    throw new RuntimeException("MongoDB deactivation failed, triggering rollback", mongoEx);
                }
            }
        }
    }
    
    @Transactional
    public TemplateDTO createTemplate(Long electionId, Long accountId, TemplateDTO templateDTO, MultipartFile file) {
        validateElectionOwnership(electionId, accountId);

        // Generate a unique slipId (e.g., UUID)
        String slipId = templateDTO.getSlipId() != null ? templateDTO.getSlipId() : UUID.randomUUID().toString();

        // Check if slipId already exists
        Optional<TemplateEntity> existingSlipById = templateRepository.findBySlipId(slipId);
        if (existingSlipById.isPresent()) {
            log.error("A voter slip with slipId '{}' already exists", slipId);
            throw new ThedalException(ThedalError.SLIP_ID_ALREADY_EXISTS, HttpStatus.CONFLICT);
        }

        // Check if templateName is unique within electionId and accountId
        Optional<TemplateEntity> existingTemplateByName = templateRepository
                .findByElectionIdAndTemplateNameAndAccountId(electionId, templateDTO.getTemplateName(), accountId);
        if (existingTemplateByName.isPresent()) {
            log.error("A template with templateName '{}' already exists for electionId: {}, accountId: {}", 
                      templateDTO.getTemplateName(), electionId, accountId);
            throw new ThedalException(ThedalError.TEMPLATE_NAME_ALREADY_EXISTS, HttpStatus.CONFLICT);
        }

        // Validate templateId is within allowed range (1, 2, 3, 4)
//        if (!List.of(1L, 2L, 3L, 4L).contains(templateDTO.getTemplateId())) {
//            throw new ThedalException(ThedalError.INVALID_TEMPLATE_ID, HttpStatus.BAD_REQUEST);
//        }
        if (!List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L).contains(templateDTO.getTemplateId())) {
            throw new ThedalException(ThedalError.INVALID_TEMPLATE_ID, HttpStatus.BAD_REQUEST);
        }

        // Generate orderIndex
        Integer maxTemplateOrderIndex = templateRepository.findMaxOrderIndexByElectionIdAndAccountId(electionId, accountId);
        int newTemplateOrderIndex = (maxTemplateOrderIndex != null) ? maxTemplateOrderIndex + 1 : 0;

        // Upload image if provided
        String uploadUrl = null;
        if (file != null && !file.isEmpty()) {
            try {
                uploadUrl = uploadTemplateImageToAWS(file);
            } catch (Exception e) {
                log.error("Error uploading image: ", e);
                throw new ThedalException(ThedalError.IMAGE_UPLOAD_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        // Determine isActive status (default to false if not provided)
        boolean isActive = templateDTO.getIsActive() != null ? templateDTO.getIsActive() : false;

        // Determine imageStatus (if isActive is false, imageStatus must be false)
        boolean imageStatus = isActive && templateDTO.getImageStatus() != null ? templateDTO.getImageStatus() : false;

        // Create the new template
        TemplateEntity template = new TemplateEntity();
        template.setAccountId(accountId);
        template.setElectionId(electionId);
        template.setTemplateId(templateDTO.getTemplateId());
        template.setSlipId(slipId);
        template.setTemplateName(templateDTO.getTemplateName());
        template.setImageUrl(uploadUrl);
        template.setIsActive(isActive);
        template.setImageStatus(imageStatus);
        template.setOrderIndex(newTemplateOrderIndex);
        template.setVoterSlipHeader(templateDTO.getVoterSlipHeader());
        template.setCandidateInfoImageFooter(templateDTO.getCandidateInfoImageFooter());

        // If the new template is active, deactivate all other templates in both PostgreSQL and MongoDB
        if (isActive) {
            // Deactivate in PostgreSQL
            List<TemplateEntity> activeTemplates = templateRepository.findByElectionIdAndAccountIdAndIsActive(electionId, accountId, true);
            for (TemplateEntity otherTemplate : activeTemplates) {
                otherTemplate.setIsActive(false);
                otherTemplate.setImageStatus(false);
                TemplateEntity savedOtherTemplate = templateRepository.save(otherTemplate);
                // Sync to MongoDB
                try {
                    TemplateMongo templateMongo = new TemplateMongo(savedOtherTemplate);
                    templateMongoRepository.save(templateMongo);
                    log.info("Deactivated template in MongoDB: id={}, name={}", savedOtherTemplate.getId(), savedOtherTemplate.getTemplateName());
                } catch (Exception mongoEx) {
                    log.error("Failed to deactivate template in MongoDB: id={}, name={}", savedOtherTemplate.getId(), savedOtherTemplate.getTemplateName(), mongoEx);
                    throw new RuntimeException("MongoDB deactivation failed, triggering rollback", mongoEx);
                }
            }

            // Deactivate in MongoDB
            List<TemplateMongo> activeMongoTemplates = templateMongoRepository.findByAccountIdAndElectionIdAndIsActive(accountId, electionId, true);
            for (TemplateMongo mongoTemplate : activeMongoTemplates) {
                mongoTemplate.setIsActive(false);
                mongoTemplate.setImageStatus(false);
                try {
                    templateMongoRepository.save(mongoTemplate);
                    log.info("Deactivated MongoDB template: id={}, name={}", mongoTemplate.getId(), mongoTemplate.getTemplateName());
                } catch (Exception mongoEx) {
                    log.error("Failed to deactivate MongoDB template: id={}, name={}", mongoTemplate.getId(), mongoTemplate.getTemplateName(), mongoEx);
                    throw new RuntimeException("MongoDB deactivation failed, triggering rollback", mongoEx);
                }
            }
        }

        try {
            // Save to PostgreSQL
            TemplateEntity savedTemplate = templateRepository.saveAndFlush(template);
            
            // Save to MongoDB
            TemplateMongo templateMongo = new TemplateMongo(savedTemplate);
            try {
                templateMongoRepository.save(templateMongo);
                log.info("Successfully saved template to MongoDB: id={}, name={}", savedTemplate.getId(), savedTemplate.getTemplateName());
            } catch (Exception mongoEx) {
                log.error("Failed to save template to MongoDB: id={}, name={}", savedTemplate.getId(), savedTemplate.getTemplateName(), mongoEx);
                throw new RuntimeException("MongoDB save failed, triggering rollback", mongoEx);
            }

            // Ensure at least one template is active
            ensureOneActiveTemplate(electionId, accountId);

            // Update DTO with response values
            templateDTO.setSlipId(slipId);
            templateDTO.setImageUrl(uploadUrl);
            templateDTO.setOrderIndex(newTemplateOrderIndex);
            templateDTO.setIsActive(savedTemplate.getIsActive());
            templateDTO.setImageStatus(savedTemplate.getImageStatus());
            log.info("Template created successfully: {}", savedTemplate.getTemplateName());
            return templateDTO;
        } catch (Exception ex) {
            log.error("Failed to create template: {}", templateDTO.getTemplateName(), ex);
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public String uploadTemplateImageToAWS(MultipartFile imageFile) {
			    String contentType = imageFile.getContentType();

			    // Validate content type (only JPEG and PNG allowed)
			    if (!(MediaType.IMAGE_JPEG_VALUE.equals(contentType) ||
			            MediaType.IMAGE_PNG_VALUE.equals(contentType))) {
			        throw new ThedalException(ThedalError.INVALID_IMAGE_FORMAT, HttpStatus.BAD_REQUEST);
			    }

			    // Validate file size (max 5 MB)
			    long maxFileSize = 5 * 1024 * 1024; // 5 MB
			    if (imageFile.getSize() > maxFileSize) {
			        throw new ThedalException(ThedalError.INVALID_IMAGE_SIZE, HttpStatus.BAD_REQUEST);
			    }

			    // Generate unique file name
			    String fileExtension = "." + awsFileUpload.getFileExtension(imageFile.getOriginalFilename());
			    String fileName = "template_" + System.currentTimeMillis() + "_" + RandomTokenGenerator.generateToken(10) + fileExtension;

			    // Upload to AWS S3
			    try {
			        // Create a temporary file
			        File tempFile = File.createTempFile("temp", fileExtension);
			        try (FileOutputStream fileOutputStream = new FileOutputStream(tempFile)) {
			            fileOutputStream.write(imageFile.getBytes());
			        }

			        // Upload the file to AWS S3
			        String awsUrl = awsFileUpload.uploadToAWS(tempFile, fileName, s3bucket);

			        // Clean up the temporary file
			        if (!tempFile.delete()) {
			            log.warn("Temporary file deletion failed: {}", tempFile.getName());
			        }

			        return awsUrl;
			    } catch (IOException e) {
			        log.error("Error uploading voter image to AWS S3", e);
			        throw new ThedalException(ThedalError.IMAGE_UPLOAD_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
			    }
			}


            public TemplateDTOResponse getTemplateById(Long accountId, Long electionId, String templateName) {
            	log.info("Fetching template from MongoDB for accountId: {}, electionId: {}, templateName: {}", accountId, electionId, templateName);
            	
            	// Read from PostgreSQL for better performance
            	TemplateEntity templateEntity = templateRepository.findByAccountIdAndElectionIdAndTemplateName(accountId, electionId, templateName)
                        .orElseThrow(() -> new ThedalException(ThedalError.TEMPLATE_NOT_FOUND, HttpStatus.NOT_FOUND));

                return mapToTemplateDTO(templateEntity);
            }
        
            private TemplateDTOResponse mapToTemplateDTO(TemplateEntity templateEntity) {
                TemplateDTOResponse templateDTO = new TemplateDTOResponse();
                templateDTO.setTemplateId(templateEntity.getTemplateId());
                templateDTO.setSlipId(templateEntity.getSlipId());
                templateDTO.setTemplateName(templateEntity.getTemplateName());
                //templateDTO.setImageUrl(templateEntity.getImageUrl());
                                
                if (Boolean.TRUE.equals(templateEntity.getImageStatus())) {
                    templateDTO.setImageUrl(templateEntity.getImageUrl());
                } else {
                    templateDTO.setImageUrl(null); 
                }
                
                templateDTO.setIsActive(templateEntity.getIsActive());
                templateDTO.setElectionId(templateEntity.getElectionId());
                templateDTO.setAccountId(templateEntity.getAccountId());
                templateDTO.setImageStatus(templateEntity.getImageStatus());
                templateDTO.setOrderIndex(templateEntity.getOrderIndex());
                templateDTO.setVoterSlipHeader(templateEntity.getVoterSlipHeader());
                templateDTO.setCandidateInfoImageFooter(templateEntity.getCandidateInfoImageFooter());
                return templateDTO;
            }

      public List<TemplateDTOResponse> getTemplatesByElectionId(Long electionId, Long accountId) {               
    	  log.info("Fetching templates from PostgreSQL for electionId: {} and accountId: {}", electionId, accountId);
    	  
    	  // Read from PostgreSQL for better performance
    	  List<TemplateEntity> templates = templateRepository.findByElectionIdAndAccountIdOrderByOrderIndexAsc(electionId, accountId);
   	
           return templates.stream()
                .map(this::mapToTemplateDTO)
                .collect(Collectors.toList());
       }       

      @Transactional
      public TemplateDTO updateTemplate(Long electionId, Long accountId, Long templateId, UpdateTemplateDTO templateDTO) {
          validateElectionOwnership(electionId, accountId);

          TemplateEntity templateEntity = templateRepository.findByAccountIdAndElectionIdAndTemplateIdAndIsActive(accountId, electionId, templateId, true)
                  .orElseThrow(() -> new ThedalException(ThedalError.TEMPLATE_NOT_FOUND, HttpStatus.NOT_FOUND));
          
          // Check if templateName is unique if changed
          if (!templateEntity.getTemplateName().equals(templateDTO.getTemplateName())) {
              Optional<TemplateEntity> existingTemplateByName = templateRepository
                      .findByElectionIdAndTemplateNameAndAccountId(electionId, templateDTO.getTemplateName(), accountId);
              if (existingTemplateByName.isPresent()) {
                  log.error("A template with templateName '{}' already exists for electionId: {}, accountId: {}", 
                            templateDTO.getTemplateName(), electionId, accountId);
                  throw new ThedalException(ThedalError.TEMPLATE_NAME_ALREADY_EXISTS, HttpStatus.CONFLICT);
              }
          }

          // Enforce: imageStatus cannot be ON if isActive is OFF
          boolean isActive = templateDTO.getIsActive() != null ? templateDTO.getIsActive() : templateEntity.getIsActive();
          boolean imageStatus = templateDTO.getImageStatus() != null ? templateDTO.getImageStatus() : templateEntity.getImageStatus();
          if (!isActive && imageStatus) {
              log.error("Cannot set imageStatus to ON when isActive is OFF for template: {}", templateDTO.getTemplateName());
              throw new ThedalException(ThedalError.INVALID_IMAGE_STATUS, HttpStatus.BAD_REQUEST);
          }

          templateEntity.setTemplateName(templateDTO.getTemplateName());
          templateEntity.setIsActive(isActive);
          templateEntity.setImageStatus(imageStatus);

          try {
              TemplateEntity updatedTemplate = templateRepository.saveAndFlush(templateEntity);
              try {
                  TemplateMongo templateMongo = new TemplateMongo(updatedTemplate);
                  templateMongoRepository.save(templateMongo);
                  log.info("Successfully updated template in MongoDB: id={}, name={}", updatedTemplate.getId(), updatedTemplate.getTemplateName());
              } catch (Exception mongoEx) {
                  log.error("Failed to update template in MongoDB: id={}, name={}", updatedTemplate.getId(), updatedTemplate.getTemplateName(), mongoEx);
                  throw new RuntimeException("MongoDB update failed, triggering rollback", mongoEx);
              }

              // Ensure only one template is active
              if (isActive) {
                  ensureOneActiveTemplate(electionId, accountId);
              }

              TemplateDTO dto = new TemplateDTO();
              dto.setImageUrl(updatedTemplate.getImageUrl());
              dto.setIsActive(updatedTemplate.getIsActive());
              dto.setTemplateId(templateId);
              dto.setTemplateName(updatedTemplate.getTemplateName());
              dto.setImageStatus(updatedTemplate.getImageStatus());
              log.info("Template updated successfully: {}", updatedTemplate.getTemplateName());
              return dto;
          } catch (Exception ex) {
              log.error("Failed to update template: id={}", templateId, ex);
              throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
          }
      }
      
	public String updateTemplateImage(Long electionId, String templateName, Long accountId, MultipartFile file) {
	    log.info("Inside updateTemplateImage service method. Updating template image for templateName: {}", templateName);
	    validateElectionOwnership(electionId, accountId);

	    TemplateEntity templateEntity = templateRepository.findByAccountIdAndElectionIdAndTemplateName(accountId, electionId, templateName)
	            .orElseThrow(() -> {
	                log.error("Template not found for accountId: {}, electionId: {}, templateName: {}", accountId, electionId, templateName);
	                return new ThedalException(ThedalError.TEMPLATE_NOT_FOUND, HttpStatus.NOT_FOUND);
	            });

	    log.debug("Template found: {}", templateEntity);

	    if (templateEntity.getImageUrl() != null) {
	        log.info("Inside updateTemplateImage service method. Deleting existing image");
	        String objectKey = AwsFileUpload.getKeyFromUrl(templateEntity.getImageUrl());
	        awsFileUpload.deleteS3Object(s3bucket, objectKey);
	    }

	    String uploadUrl;
	    try {
	        uploadUrl = uploadTemplateImageToAWS(file);
	    } catch (Exception e) {
	        log.error("Error uploading image: ", e);
	        throw new ThedalException(ThedalError.IMAGE_UPLOAD_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
	    }

	    templateEntity.setImageUrl(uploadUrl);
	    
	    try {
	        TemplateEntity updatedTemplate = templateRepository.saveAndFlush(templateEntity);
	        try {
	            TemplateMongo templateMongo = new TemplateMongo(updatedTemplate);
	            templateMongoRepository.save(templateMongo);
	            log.info("Successfully updated template image in MongoDB: id={}, name={}", updatedTemplate.getId(), updatedTemplate.getTemplateName());
	        } catch (Exception mongoEx) {
	            log.error("Failed to update template image in MongoDB: id={}, name={}", updatedTemplate.getId(), updatedTemplate.getTemplateName(), mongoEx);
	            throw new RuntimeException("MongoDB update failed, triggering rollback", mongoEx);
	        }
	        log.info("End of updateTemplateImage service method. Updated template image for templateName: {}", templateName);
	        return uploadUrl;
	    } catch (Exception ex) {
	        log.error("Failed to update template image for templateName: {}", templateName, ex);
	        throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
	    }
	}
	
//	@Transactional
//	public TemplateUpdateDto updateTemplateStatus(Long electionId, Long accountId, String templateName, UpdateTemplateStatusRequest request) {
//	    // Validate election ownership
//	    validateElectionOwnership(electionId, accountId);
//
//	    // Find the template to update
//	    TemplateEntity template = templateRepository.findByAccountIdAndElectionIdAndTemplateName(accountId, electionId, templateName)
//	            .orElseThrow(() -> {
//	                log.error("Template {} not found for election ID {} and account ID {}", templateName, electionId, accountId);
//	                return new ThedalException(ThedalError.TEMPLATE_NOT_FOUND, HttpStatus.NOT_FOUND);
//	            });
//
//	    Boolean newIsActive = request.getIsActive();
//
//	    // If newIsActive is true, deactivate all other templates
//	    if (Boolean.TRUE.equals(newIsActive)) {
//	        List<TemplateEntity> activeTemplates = templateRepository.findByElectionIdAndAccountIdAndIsActive(electionId, accountId, true);
//	        for (TemplateEntity otherTemplate : activeTemplates) {
//	            if (!otherTemplate.getTemplateName().equals(templateName)) {
//	                otherTemplate.setIsActive(false);
//	                otherTemplate.setImageStatus(false); // Enforce imageStatus = false when isActive = false
//	                templateRepository.save(otherTemplate);
//	            }
//	        }
//	    }
//
//	    // If newIsActive is false, ensure imageStatus is also false
//	    if (Boolean.FALSE.equals(newIsActive)) {
//	        template.setImageStatus(false);
//	    }
//
//	    // Update isActive status
//	    template.setIsActive(newIsActive);
//
//	    try {
//	        TemplateEntity updatedTemplate = templateRepository.saveAndFlush(template);
//	        try {
//	            TemplateMongo templateMongo = new TemplateMongo(updatedTemplate);
//	            templateMongoRepository.save(templateMongo);
//	            log.info("Successfully updated template status in MongoDB: id={}, name={}", updatedTemplate.getId(), updatedTemplate.getTemplateName());
//	        } catch (Exception mongoEx) {
//	            log.error("Failed to update template status in MongoDB: id={}, name={}", updatedTemplate.getId(), updatedTemplate.getTemplateName(), mongoEx);
//	            throw new RuntimeException("MongoDB update failed, triggering rollback", mongoEx);
//	        }
//
//	        // Ensure at least one template is active
//	        ensureOneActiveTemplate(electionId, accountId);
//
//	        // Prepare response
//	        TemplateUpdateDto response = new TemplateUpdateDto();
//	        response.setTemplateName(updatedTemplate.getTemplateName());
//	        response.setIsActive(updatedTemplate.getIsActive());
//	        response.setImageStatus(updatedTemplate.getImageStatus());
//	        log.info("Template status updated successfully: {}", updatedTemplate.getTemplateName());
//	        return response;
//	    } catch (Exception ex) {
//	        log.error("Failed to update template status for templateName: {}", templateName, ex);
//	        throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
//	    }
//	}
	@Transactional
	public TemplateUpdateDto updateTemplateStatus(Long electionId, Long accountId, String templateName, UpdateTemplateStatusRequest request) {
	    validateElectionOwnership(electionId, accountId);

	    // Find the template to update
	    TemplateEntity template = templateRepository.findByAccountIdAndElectionIdAndTemplateName(accountId, electionId, templateName)
	            .orElseThrow(() -> {
	                log.error("Template {} not found for election ID {} and account ID {}", templateName, electionId, accountId);
	                return new ThedalException(ThedalError.TEMPLATE_NOT_FOUND, HttpStatus.NOT_FOUND);
	            });

	    Boolean newIsActive = request.getIsActive();

	    // If newIsActive is true, deactivate all other templates in both PostgreSQL and MongoDB
	    if (Boolean.TRUE.equals(newIsActive)) {
	        // Deactivate in PostgreSQL
	        List<TemplateEntity> activeTemplates = templateRepository.findByElectionIdAndAccountIdAndIsActive(electionId, accountId, true);
	        for (TemplateEntity otherTemplate : activeTemplates) {
	            if (!otherTemplate.getTemplateName().equals(templateName)) {
	                otherTemplate.setIsActive(false);
	                otherTemplate.setImageStatus(false);
	                TemplateEntity savedOtherTemplate = templateRepository.save(otherTemplate);
	                // Sync to MongoDB
	                try {
	                    TemplateMongo templateMongo = new TemplateMongo(savedOtherTemplate);
	                    templateMongoRepository.save(templateMongo);
	                    log.info("Deactivated template in MongoDB: id={}, name={}", savedOtherTemplate.getId(), savedOtherTemplate.getTemplateName());
	                } catch (Exception mongoEx) {
	                    log.error("Failed to deactivate template in MongoDB: id={}, name={}", savedOtherTemplate.getId(), savedOtherTemplate.getTemplateName(), mongoEx);
	                    throw new RuntimeException("MongoDB deactivation failed, triggering rollback", mongoEx);
	                }
	            }
	        }

	        // Deactivate in MongoDB
	        List<TemplateMongo> activeMongoTemplates = templateMongoRepository.findByAccountIdAndElectionIdAndIsActive(accountId, electionId, true);
	        for (TemplateMongo mongoTemplate : activeMongoTemplates) {
	            if (!mongoTemplate.getTemplateName().equals(templateName)) {
	                mongoTemplate.setIsActive(false);
	                mongoTemplate.setImageStatus(false);
	                try {
	                    templateMongoRepository.save(mongoTemplate);
	                    log.info("Deactivated MongoDB template: id={}, name={}", mongoTemplate.getId(), mongoTemplate.getTemplateName());
	                } catch (Exception mongoEx) {
	                    log.error("Failed to deactivate MongoDB template: id={}, name={}", mongoTemplate.getId(), mongoTemplate.getTemplateName(), mongoEx);
	                    throw new RuntimeException("MongoDB deactivation failed, triggering rollback", mongoEx);
	                }
	            }
	        }
	    }

	    // If newIsActive is false, ensure imageStatus is also false
	    if (Boolean.FALSE.equals(newIsActive)) {
	        template.setImageStatus(false);
	    }

	    // Update isActive status
	    template.setIsActive(newIsActive);

	    try {
	        TemplateEntity updatedTemplate = templateRepository.saveAndFlush(template);
	        try {
	            TemplateMongo templateMongo = new TemplateMongo(updatedTemplate);
	            templateMongoRepository.save(templateMongo);
	            log.info("Successfully updated template status in MongoDB: id={}, name={}", updatedTemplate.getId(), updatedTemplate.getTemplateName());
	        } catch (Exception mongoEx) {
	            log.error("Failed to update template status in MongoDB: id={}, name={}", updatedTemplate.getId(), updatedTemplate.getTemplateName(), mongoEx);
	            throw new RuntimeException("MongoDB update failed, triggering rollback", mongoEx);
	        }

	        // Ensure at least one template is active
	        ensureOneActiveTemplate(electionId, accountId);

	        // Prepare response
	        TemplateUpdateDto response = new TemplateUpdateDto();
	        response.setTemplateName(updatedTemplate.getTemplateName());
	        response.setIsActive(updatedTemplate.getIsActive());
	        response.setImageStatus(updatedTemplate.getImageStatus());
	        log.info("Template status updated successfully: {}", updatedTemplate.getTemplateName());
	        return response;
	    } catch (Exception ex) {
	        log.error("Failed to update template status for templateName: {}", templateName, ex);
	        throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
	    }
	}

	@Transactional
	public void toggleImageStatus(Long electionId, String templateName, Long accountId, Boolean newImageStatus) {
	    log.info("Inside toggleImageStatus service method. Toggling status for templateName: {}", templateName);

	    // Validate election ownership
	    validateElectionOwnership(electionId, accountId);

	    // Find the template
	    TemplateEntity templateEntity = templateRepository.findByAccountIdAndElectionIdAndTemplateName(accountId, electionId, templateName)
	            .orElseThrow(() -> {
	                log.error("Template {} not found for election ID {} and account ID {}", templateName, electionId, accountId);
	                return new ThedalException(ThedalError.TEMPLATE_NOT_FOUND, HttpStatus.NOT_FOUND);
	            });

	    // Enforce: imageStatus cannot be ON if isActive is OFF
	    if (Boolean.TRUE.equals(newImageStatus) && !templateEntity.getIsActive()) {
	        log.error("Cannot set imageStatus to ON when isActive is OFF for template: {}", templateName);
	        throw new ThedalException(ThedalError.INVALID_IMAGE_STATUS, HttpStatus.BAD_REQUEST);
	    }

	    // Update imageStatus
	    templateEntity.setImageStatus(newImageStatus);
	    templateRepository.save(templateEntity);

	    log.info("End of toggleImageStatus service method. New image status for templateName {}: {}", 
	            templateName, newImageStatus);
	}
	
	@Transactional
	public void updateTemplateOrder(List<TemplateReorderRequest> reorderRequests, Long accountId, Long electionId) {
	    List<TemplateEntity> templates = templateRepository.findByElectionIdAndAccountIdOrderByOrderIndexAsc(electionId, accountId);

	    if (templates.isEmpty()) {
	        log.error("No templates found for election ID {} and account ID {}", electionId, accountId);
	        throw new ThedalException(ThedalError.TEMPLATE_NOT_FOUND, HttpStatus.NOT_FOUND);
	    }

	    // Create a map of templateName -> newOrderIndex
	    Map<String, Integer> newOrderMap = reorderRequests.stream()
	            .collect(Collectors.toMap(TemplateReorderRequest::getTemplateName, TemplateReorderRequest::getNewOrderIndex));

	    // Validate all requested templates exist
	    for (TemplateReorderRequest request : reorderRequests) {
	        if (templates.stream().noneMatch(t -> t.getTemplateName().equals(request.getTemplateName()))) {
	            log.error("Template with name {} not found for electionId: {}, accountId: {}", 
	                      request.getTemplateName(), electionId, accountId);
	            throw new ThedalException(ThedalError.TEMPLATE_NOT_FOUND, HttpStatus.NOT_FOUND);
	        }
	    }

	    // Sort the existing templates list before modifying
	    templates.sort(Comparator.comparing(TemplateEntity::getOrderIndex));

	    // Remove templates that are being reordered
	    List<TemplateEntity> reorderedTemplates = new ArrayList<>(templates);
	    reorderedTemplates.removeIf(template -> newOrderMap.containsKey(template.getTemplateName()));

	    // Insert templates at their new positions
	    for (TemplateReorderRequest request : reorderRequests) {
	        TemplateEntity template = templates.stream()
	                .filter(t -> t.getTemplateName().equals(request.getTemplateName()))
	                .findFirst()
	                .get(); // Safe due to prior validation
	        reorderedTemplates.add(request.getNewOrderIndex(), template);
	    }

	    // Update `order_index` for all templates
	    for (int i = 0; i < reorderedTemplates.size(); i++) {
	        reorderedTemplates.get(i).setOrderIndex(i);
	        log.info("Updated template order: {} -> {}", reorderedTemplates.get(i).getTemplateName(), i);
	    }

	    // Save updated order to DB with dual write
	    try {
	        List<TemplateEntity> savedTemplates = templateRepository.saveAll(reorderedTemplates);
	        
	        // Update MongoDB as well
	        try {
	            List<TemplateMongo> mongoTemplates = savedTemplates.stream()
	                    .map(TemplateMongo::new)
	                    .collect(Collectors.toList());
	            templateMongoRepository.saveAll(mongoTemplates);
	            log.info("Updated template order in MongoDB for electionId: {}", electionId);
	        } catch (Exception mongoEx) {
	            log.error("Failed to update template order in MongoDB for electionId: {}", electionId, mongoEx);
	            throw new RuntimeException("MongoDB reorder failed, triggering rollback", mongoEx);
	        }
	        
	        log.info("Template order updated successfully for electionId: {}", electionId);
	    } catch (Exception ex) {
	        log.error("Failed to update template order for electionId: {}", electionId, ex);
	        throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
	    }
	}

	@Transactional
	public ThedalResponse<Void> deleteTemplates(Long electionId, Long accountId, List<String> templateNames) {
	    

	    validateElectionOwnership(electionId, accountId);
	    
	    // Check if default template is being targeted
	    if (templateNames != null && templateNames.contains("Default")) {
	        log.error("Attempt to delete default template for electionId: {}, accountId: {}", electionId, accountId);
	        throw new ThedalException(ThedalError.DEFAULT_TEMPLATE_CANNOT_BE_DELETED, HttpStatus.BAD_REQUEST);
	    }
	    
//	    int deletedCount;
//	    if (templateNames == null || templateNames.isEmpty()) {
//	        log.info("Deleting all non-default templates for electionId: {}, accountId: {}", electionId, accountId);
//	        // Delete all templates except the default one
//	        deletedCount = templateRepository.deleteByAccountIdAndElectionIdAndTemplateNameNot(accountId, electionId, "Default");
//	    } else {
//	        log.info("Deleting specific templates for electionId: {}, accountId: {} with names: {}", 
//	                 electionId, accountId, templateNames);
//	        deletedCount = templateRepository.deleteByAccountIdAndElectionIdAndTemplateNameIn(accountId, electionId, templateNames);
//	    }
	    int deletedCount;
	    try {
	        if (templateNames == null || templateNames.isEmpty()) {
	            log.info("Deleting all non-default templates for electionId: {}, accountId: {}", electionId, accountId);
	            // Delete all templates except the default one
	            deletedCount = templateRepository.deleteByAccountIdAndElectionIdAndTemplateNameNot(accountId, electionId, "Default");
	            
	            try {
	                templateMongoRepository.deleteByAccountIdAndElectionIdAndTemplateNameNot(accountId, electionId, "Default");
	                log.info("Deleted all non-default templates from MongoDB for accountId: {}, electionId: {}", accountId, electionId);
	            } catch (Exception mongoEx) {
	                log.error("Failed to delete templates from MongoDB for accountId: {}, electionId: {}", accountId, electionId, mongoEx);
	                throw new RuntimeException("MongoDB deletion failed, triggering rollback", mongoEx);
	            }
	        } else {
	            log.info("Deleting specific templates for electionId: {}, accountId: {} with names: {}", 
	                     electionId, accountId, templateNames);
	            deletedCount = templateRepository.deleteByAccountIdAndElectionIdAndTemplateNameIn(accountId, electionId, templateNames);
	            
	            try {
	                templateMongoRepository.deleteByAccountIdAndElectionIdAndTemplateNameIn(accountId, electionId, templateNames);
	                log.info("Deleted specific templates from MongoDB: names={}", templateNames);
	            } catch (Exception mongoEx) {
	                log.error("Failed to delete specific templates from MongoDB: names={}", templateNames, mongoEx);
	                throw new RuntimeException("MongoDB deletion failed, triggering rollback", mongoEx);
	            }
	        }

	        if (deletedCount == 0 && (templateNames != null && !templateNames.isEmpty())) {
	            log.error("No templates found for deletion with names: {} for electionId: {}", templateNames, electionId);
	            throw new ThedalException(ThedalError.TEMPLATE_NOT_FOUND, HttpStatus.NOT_FOUND);
	        }

	        ensureOneActiveTemplate(electionId, accountId);
	        log.info("Templates deleted successfully: names={}", templateNames);
	        return new ThedalResponse<>(ThedalSuccess.TEMPLATE_DELETED);
	    } catch (Exception ex) {
	        log.error("Failed to delete templates: names={}", templateNames, ex);
	        throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
	    }
	}
	
	public UpdateTemplateDetailsResponse updateTemplateDetails(Long electionId, Long accountId, String templateName, UpdateTemplateDetailsRequest request) {
		TemplateEntity template = templateRepository.findByAccountIdAndElectionIdAndTemplateName(accountId, electionId, templateName)
				.orElseThrow(() -> new ThedalException(ThedalError.TEMPLATE_NOT_FOUND, HttpStatus.NOT_FOUND));
	
//		if (request.getTemplateName() != null) {
//			template.setTemplateName(request.getTemplateName());
//		}
		if (request.getTemplateName() != null) {
            if (!template.getTemplateName().equals(request.getTemplateName())) {
                Optional<TemplateEntity> existingTemplateByName = templateRepository
                        .findByElectionIdAndTemplateNameAndAccountId(electionId, request.getTemplateName(), accountId);
                if (existingTemplateByName.isPresent()) {
                    log.error("A template with templateName '{}' already exists for electionId: {}, accountId: {}", 
                              request.getTemplateName(), electionId, accountId);
                    throw new ThedalException(ThedalError.TEMPLATE_NAME_ALREADY_EXISTS, HttpStatus.CONFLICT);
                }
            }
            template.setTemplateName(request.getTemplateName());
        }
	
		if (request.getVoterSlipHeader() != null) {
			template.setVoterSlipHeader(request.getVoterSlipHeader());
		}
	
		if (request.getCandidateInfoImageFooter() != null) {
			template.setCandidateInfoImageFooter(request.getCandidateInfoImageFooter());
		}
	
		try {
		    TemplateEntity updatedTemplate = templateRepository.saveAndFlush(template);
		    try {
		        TemplateMongo templateMongo = new TemplateMongo(updatedTemplate);
		        templateMongoRepository.save(templateMongo);
		        log.info("Successfully updated template details in MongoDB: id={}, name={}", updatedTemplate.getId(), updatedTemplate.getTemplateName());
		    } catch (Exception mongoEx) {
		        log.error("Failed to update template details in MongoDB: id={}, name={}", updatedTemplate.getId(), updatedTemplate.getTemplateName(), mongoEx);
		        throw new RuntimeException("MongoDB update failed, triggering rollback", mongoEx);
		    }
		
		    // prepare response
		    UpdateTemplateDetailsResponse response = new UpdateTemplateDetailsResponse();
		    response.setTemplateName(updatedTemplate.getTemplateName());
		    response.setVoterSlipHeader(updatedTemplate.getVoterSlipHeader());
		    response.setCandidateInfoImageFooter(updatedTemplate.getCandidateInfoImageFooter());
		    log.info("Template details updated successfully: {}", updatedTemplate.getTemplateName());
		    return response;
		} catch (Exception ex) {
		    log.error("Failed to update template details for templateName: {}", templateName, ex);
		    throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	
            private TemplateDTOResponse mapToTemplateDTOFromMongo(TemplateMongo templateMongo) {
                TemplateDTOResponse templateDTO = new TemplateDTOResponse();
                templateDTO.setTemplateId(templateMongo.getTemplateId());
                templateDTO.setSlipId(templateMongo.getSlipId());
                templateDTO.setTemplateName(templateMongo.getTemplateName());
                                
                if (Boolean.TRUE.equals(templateMongo.getImageStatus())) {
                    templateDTO.setImageUrl(templateMongo.getImageUrl());
                } else {
                    templateDTO.setImageUrl(null); 
                }
                
                templateDTO.setIsActive(templateMongo.getIsActive());
                templateDTO.setElectionId(templateMongo.getElectionId());
                templateDTO.setAccountId(templateMongo.getAccountId());
                templateDTO.setImageStatus(templateMongo.getImageStatus());
                templateDTO.setOrderIndex(templateMongo.getOrderIndex());
                templateDTO.setVoterSlipHeader(templateMongo.getVoterSlipHeader());
                templateDTO.setCandidateInfoImageFooter(templateMongo.getCandidateInfoImageFooter());
                return templateDTO;
            }
}
