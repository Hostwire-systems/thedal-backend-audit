package com.thedal.thedal_app.election;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thedal.thedal_app.awsfilestore.AwsFileUpload;
import com.thedal.thedal_app.election.dtos.FieldReorderRequest;
import com.thedal.thedal_app.election.dtos.SurveyExportJobDetailsDTO;
import com.thedal.thedal_app.election.dtos.SurveyFormDTO;
import com.thedal.thedal_app.election.dtos.SurveyFormReorderRequest;
import com.thedal.thedal_app.election.dtos.SurveyFormResponseDTO;
import com.thedal.thedal_app.election.dtos.SurveyFormSubmissionDTO;
import com.thedal.thedal_app.election.dtos.SurveyFormSubmissionsPageDTO;
import com.thedal.thedal_app.election.dtos.UpdateSurveyFormStatusRequest;
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
@RequestMapping("/api/survey-forms")
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Survey Form Management")
public class SurveyFormController {

	@Autowired
    private SurveyFormService surveyFormService;
	@Autowired
    private RequestDetailsService requestDetails;
	@Autowired
	private AwsFileUpload awsFileUpload;
	@Value("${aws.s3.files.bucket}")
	private String s3bucket;
	@Autowired
    private SurveyFormRepository surveyFormRepository;
	@Autowired
	private ObjectMapper objectMapper;

    @Operation(summary = "Create a new survey form",
               description = "Creates a survey form with the provided fields for a specific election.")
    @PostMapping("/election/{electionId}")
    public ThedalResponse<SurveyFormDTO> createSurveyForm(
            @PathVariable("electionId") Long electionId,
            @Valid @RequestBody SurveyFormDTO formDTO) {
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account id not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        SurveyFormDTO createdForm = surveyFormService.createSurveyForm(accountId, electionId, formDTO);
        return new ThedalResponse<>(ThedalSuccess.SURVEY_FORM_CREATED, createdForm);
    }

    @Operation(summary = "Retrieve survey forms",
               description = "Fetches a paginated list of survey forms based on electionId, with optional filters for formName and isActive.")
    @GetMapping("/election/{electionId}")
    public ThedalResponse<SurveyFormResponseDTO> getSurveyForms(
            @PathVariable("electionId") Long electionId,
            @RequestParam(value = "formName", required = false) String formName,
            @RequestParam(value = "isActive", required = false) Boolean isActive,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sortBy", defaultValue = "formName") String sortBy,
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
        Sort sort = Sort.by(direction, sortBy.equalsIgnoreCase("formName") ? "formName" : "createdTime");
        Pageable pageable = PageRequest.of(page, size, sort);

        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account id not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        SurveyFormResponseDTO result = surveyFormService.getSurveyForms(accountId, electionId, formName, isActive, pageable);
        return new ThedalResponse<>(ThedalSuccess.SURVEY_FORM_FOUND, result);
    }
    
    @Operation(summary = "Update a survey form",
               description = "Updates the specified survey form with new fields and status.")
    @PutMapping("/election/{electionId}/form/{formId}")
    public ThedalResponse<SurveyFormDTO> updateSurveyForm(
            @PathVariable("electionId") Long electionId,
            @PathVariable("formId") Long formId,
            @Valid @RequestBody SurveyFormDTO formDTO) {
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account id not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        SurveyFormDTO updatedForm = surveyFormService.updateSurveyForm(accountId, electionId, formId, formDTO);
        return new ThedalResponse<>(ThedalSuccess.SURVEY_FORM_UPDATED, updatedForm);
    }

    @Operation(summary = "Delete a survey form",
               description = "Deletes the specified survey form.")
    @DeleteMapping("/election/{electionId}/form/{formId}")
    public ThedalResponse<Void> deleteSurveyForm(
            @PathVariable("electionId") Long electionId,
            @PathVariable("formId") Long formId) {
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account id not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        surveyFormService.deleteSurveyForm(accountId, electionId, formId);
        return new ThedalResponse<>(ThedalSuccess.SURVEY_FORM_DELETED, null);
    }
    
    @Operation(summary = "Delete survey forms",
            description = "Deletes specific survey forms or all forms for an election.")
 @DeleteMapping("/election/{electionId}/forms")
 public ThedalResponse<Void> deleteSurveyForms(
         @PathVariable("electionId") Long electionId,
         @RequestParam(value = "formIds", required = false) List<Long> formIds) {
     Long accountId = requestDetails.getCurrentAccountId();
     if (accountId == null) {
         log.error("Account id not found, unauthorized access.");
         throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
     }

     surveyFormService.deleteSurveyForms(accountId, electionId, formIds);
     return new ThedalResponse<>(ThedalSuccess.SURVEY_FORM_DELETED, null);
 }

    @Operation(
    	    summary = "Update survey form status",
    	    description = "Changes the active status of the specified survey form."
    	)
    	@PutMapping("/election/{electionId}/form/{formId}/status")
    	public ThedalResponse<SurveyFormDTO> updateFormStatus(
    	        @PathVariable("electionId") Long electionId,
    	        @PathVariable("formId") Long formId,
    	        @RequestBody @Valid UpdateSurveyFormStatusRequest statusRequest) {

    	    Long accountId = requestDetails.getCurrentAccountId();
    	    if (accountId == null) {
    	        log.error("Account id not found, unauthorized access.");
    	        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
    	    }

    	    SurveyFormDTO updatedForm = surveyFormService.updateFormStatus(
    	            accountId, electionId, formId, statusRequest.getIsActive());

    	    return new ThedalResponse<>(ThedalSuccess.SURVEY_FORM_STATUS_UPDATED, updatedForm);
    	}
    
    @Operation(summary = "Reorder survey forms",
            description = "Reorders survey forms for a specific election based on provided form IDs and new order indices.")
 @PutMapping("/{electionId}/reorder")
 public ResponseEntity<ThedalResponse<String>> reorderSurveyForms(
         @PathVariable Long electionId,
         @RequestBody List<SurveyFormReorderRequest> reorderRequests) {
     try {
         Long accountId = requestDetails.getCurrentAccountId();
         if (accountId == null) {
             log.error("Account ID not found, unauthorized access.");
             throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
         }

         surveyFormService.updateSurveyFormOrder(reorderRequests, accountId, electionId);
         return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.SURVEY_FORM_ORDER_UPDATED));

     } catch (Exception e) {
         log.error("Error reordering survey forms: {}", e.getMessage());
         return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                 .body(new ThedalResponse<>(ThedalError.SURVEY_FORM_ORDER_UPDATE_FAILED));
     }
 }
    
    @Operation(summary = "Reorder fields within a survey form",
            description = "Reorders fields within a specific survey form based on provided field labels and new order indices.")
 @PutMapping("/election/{electionId}/form/{formId}/fields/reorder")
 public ResponseEntity<ThedalResponse<SurveyFormDTO>> reorderFormFields(
         @PathVariable Long electionId,
         @PathVariable Long formId,
         @RequestBody List<FieldReorderRequest> reorderRequests) {
     try {
         Long accountId = requestDetails.getCurrentAccountId();
         if (accountId == null) {
             log.error("Account ID not found, unauthorized access.");
             throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
         }

         SurveyFormDTO updatedForm = surveyFormService.reorderFormFields(accountId, electionId, formId, reorderRequests);
         return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.SURVEY_FORM_FIELDS_REORDERED, updatedForm));
     } catch (Exception e) {
         log.error("Error reordering form fields: {}", e.getMessage());
         return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                 .body(new ThedalResponse<>(ThedalError.SURVEY_FORM_FIELDS_REORDER_FAILED));
     }
 }
    
    
 ///////////////////////////////////////////////////////   
    
    @Operation(summary = "Submit form data",
            description = "Stores user-submitted data for a specific survey form.")
 //@PostMapping("/election/{electionId}/form/{formId}/submit")
    @PostMapping(value = "/election/{electionId}/form/{formId}/submit", consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE})
 public ThedalResponse<SurveyFormSubmissionDTO> submitForm(
         @PathVariable("electionId") Long electionId,
         @PathVariable("formId") Long formId,
         @RequestBody SurveyFormSubmissionDTO submissionDTO,
         @RequestPart(name = "images", required = false) List<MultipartFile> imageFiles) {
    	 Long accountId = requestDetails.getCurrentAccountId();
 	    if (accountId == null) {
 	        log.error("Account id not found, unauthorized access.");
 	        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
 	    }

 	   if (imageFiles != null && !imageFiles.isEmpty()) {
 	        Map<String, Object> submissionData = submissionDTO.getSubmissionData();
 	        SurveyFormEntity form = surveyFormRepository.findByIdAndAccountIdAndElectionId(formId, accountId, electionId)
 	                .orElseThrow(() -> new ThedalException(ThedalError.SURVEY_FORM_NOT_FOUND, HttpStatus.NOT_FOUND));

 	        // Map image files to form fields
 	        for (MultipartFile image : imageFiles) {
 	            String fieldLabel = image.getName(); // Assumes field name matches form field label
 	            if (form.getCustomFields().stream().anyMatch(f -> f.get("label").equals(fieldLabel) && f.get("type").equals("image"))) {
 	                String s3Url = uploadImageToAWS(image, accountId, electionId, formId, fieldLabel);
 	                submissionData.put(fieldLabel, s3Url);
 	            } else {
 	                log.error("Invalid image field: {}", fieldLabel);
 	                throw new ThedalException(ThedalError.INVALID_IMAGE_FIELD, HttpStatus.BAD_REQUEST);
 	            }
 	        }
 	        submissionDTO.setSubmissionData(submissionData);
 	    }
 	    
     SurveyFormSubmissionDTO savedSubmission = surveyFormService.submitForm(accountId, electionId, formId, submissionDTO);
     return new ThedalResponse<>(ThedalSuccess.SURVEY_FORM_SUBMISSION_SAVED, savedSubmission);
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
    
    @Operation(summary = "Retrieve form submissions",
            description = "Fetches a paginated list of submissions for a specific survey form.")
 @GetMapping("/election/{electionId}/form/{formId}/submissions")
 public ThedalResponse<SurveyFormSubmissionsPageDTO> getFormSubmissions(
         @PathVariable("electionId") Long electionId,
         @PathVariable("formId") Long formId,
         @RequestParam(value = "page", defaultValue = "0") int page,
         @RequestParam(value = "size", defaultValue = "10") int size,
         @RequestParam(value = "sortBy", defaultValue = "submittedAt") String sortBy,
         @RequestParam(value = "order", defaultValue = "desc") String order) {

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
     Sort sort = Sort.by(direction, sortBy.equalsIgnoreCase("submittedAt") ? "submittedAt" : "id");
     Pageable pageable = PageRequest.of(page, size, sort);

     Long accountId = requestDetails.getCurrentAccountId();
     if (accountId == null) {
         log.error("Account id not found, unauthorized access.");
         throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
     }

     SurveyFormSubmissionsPageDTO result = surveyFormService.getFormSubmissions(accountId, electionId, formId, pageable);
     return new ThedalResponse<>(ThedalSuccess.SUBMISSIONS_FOUND, result);
 }
    
    @Operation(summary = "Export Survey Submissions to Excel",
            description = "Exports survey submissions for a specific form and election to Excel and stores in AWS S3.")
 @PostMapping("/election/{electionId}/form/{formId}/export")
 public ThedalResponse<SurveyExportResponse> exportSurveySubmissions(
         @PathVariable("electionId") Long electionId,
         @PathVariable("formId") Long formId,
        // @RequestParam(value = "voterIds", required = false) List<String> voterIds,
         @RequestParam(value = "limit", required = false) Integer limit) {

     Long accountId = requestDetails.getCurrentAccountId();
     if (accountId == null) {
         log.error("Account id not found, unauthorized access.");
         throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
     }

//     if (voterIds != null && voterIds.isEmpty()) {
//         throw new ThedalException(ThedalError.INVALID_VOTER_IDS, HttpStatus.BAD_REQUEST);
//     }

     SurveyExportResponse response = surveyFormService.initiateSurveyExport(accountId, electionId, formId, limit);
     return new ThedalResponse<>(ThedalSuccess.SURVEY_EXPORT_INITIATED, response);
 }
    
   
    @Operation(summary = "Get Survey Export Job Details",
            description = "Retrieves details of all survey export jobs for an election or a specific job by jobId.")
 @GetMapping("/election/{electionId}/export-jobs")
 public ThedalResponse<List<SurveyExportJobDetailsDTO>> getSurveyExportJobs(
         @PathVariable("electionId") Long electionId,
         @RequestParam(value = "jobId", required = false) Long jobId) {

     Long accountId = requestDetails.getCurrentAccountId();
     if (accountId == null) {
         log.error("Account id not found, unauthorized access.");
         throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
     }

     List<SurveyExportJobDetailsDTO> jobDetails = surveyFormService.getSurveyExportJobs(accountId, electionId, jobId);
     return new ThedalResponse<>(ThedalSuccess.SURVEY_EXPORT_JOBS_FOUND, jobDetails);
 }
    
    @Operation(summary = "Delete survey export jobs",
            description = "Deletes specific survey export jobs or all export jobs for an election.")
    @DeleteMapping("/election/{electionId}/export-jobs")
    public ThedalResponse<Void> deleteSurveyExportJobs(
            @PathVariable("electionId") Long electionId,
            @RequestParam(value = "jobIds", required = false) List<Long> jobIds) {
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account id not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        surveyFormService.deleteSurveyExportJobs(accountId, electionId, jobIds);
        return new ThedalResponse<>(ThedalSuccess.SURVEY_EXPORT_JOBS_DELETED, null);
    }
    

    @Operation(summary = "Delete survey form submissions",
            description = "Deletes specific survey form submissions or all submissions for a form.")
 @DeleteMapping("/election/{electionId}/form/{formId}/submissions")
 public ThedalResponse<Void> deleteSurveyFormSubmissions(
         @PathVariable("electionId") Long electionId,
         @PathVariable("formId") Long formId,
         @RequestParam(value = "submissionIds", required = false) List<Long> submissionIds) {
     Long accountId = requestDetails.getCurrentAccountId();
     if (accountId == null) {
         log.error("Account id not found, unauthorized access.");
         throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
     }

     surveyFormService.deleteSurveyFormSubmissions(accountId, electionId, formId, submissionIds);
     return new ThedalResponse<>(ThedalSuccess.SUBMISSIONS_DELETED, null);
 }
    
    
}