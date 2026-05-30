package com.thedal.thedal_app.election;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.thedal.thedal_app.election.dtos.ImageStatusRequest;
import com.thedal.thedal_app.election.dtos.TemplateDTO;
import com.thedal.thedal_app.election.dtos.TemplateReorderRequest;
import com.thedal.thedal_app.election.dtos.TemplateUpdateDto;
import com.thedal.thedal_app.election.dtos.UpdateTemplateDTO;
import com.thedal.thedal_app.election.dtos.UpdateTemplateStatusRequest;
import com.thedal.thedal_app.general.RequestDetailsService;
import com.thedal.thedal_app.response.ThedalResponse;
import com.thedal.thedal_app.response.ThedalSuccess;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/elections/templates")
@Slf4j
public class TemplateController {
    @Autowired
    private TemplateService templateService;
    @Autowired
    private RequestDetailsService requestDetails;
    @Autowired
    private ElectionRepository electionRepository;

    
    private void validateElectionOwnership(Long electionId, Long accountId) {
        Optional<ElectionEntity> electionOpt = electionRepository.findByIdAndAccountId(electionId, accountId);
        if (!electionOpt.isPresent()) {
            log.error("Election ID {} does not belong to Account ID {}", electionId, accountId);
            throw new ThedalException(ThedalError.INVALID_ELECTION, HttpStatus.FORBIDDEN);  
        }
    }
    
    @PostMapping("/{electionId}")
    public ResponseEntity<ThedalResponse<TemplateDTO>> createTemplate(@PathVariable("electionId") Long electionId,
    		//@RequestPart("file") MultipartFile file,
    		@RequestPart(value = "file", required = false) MultipartFile file,
    		@RequestPart("template") TemplateDTO template) {
        log.info("Received template: {}", template);
        //log.info("Received file: {}", file.getOriginalFilename());
        if (file != null) {
            log.info("Received file: {}", file.getOriginalFilename());
        } else {
            log.info("No image provided.");
        }
    	Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account ID not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }
        TemplateDTO templateResponse = templateService.createTemplate(electionId,accountId,template,file);
        ThedalResponse<TemplateDTO> response = new ThedalResponse<>(ThedalSuccess.TEMPLATE_CREATED, templateResponse);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{electionId}/{templateName}")
    public ResponseEntity<ThedalResponse<TemplateDTOResponse>> getTemplateById(@PathVariable("electionId") Long electionId, @PathVariable("templateName") String templateName) {
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account ID not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }
        TemplateDTOResponse templateDTO = templateService.getTemplateById(accountId,electionId,templateName);
        ThedalResponse<TemplateDTOResponse> response = new ThedalResponse<>(ThedalSuccess.TEMPLATE_FETCHED, templateDTO);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/election/{electionId}")
    public ResponseEntity<ThedalResponse<List<TemplateDTOResponse>>> getTemplatesByElectionId(@PathVariable("electionId") Long electionId) {
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account ID not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }
        List<TemplateDTOResponse> templates = templateService.getTemplatesByElectionId(electionId, accountId);
        ThedalResponse<List<TemplateDTOResponse>> response = new ThedalResponse<>(ThedalSuccess.TEMPLATE_FETCHED, templates);
        return ResponseEntity.ok(response);
    }
    

    @PatchMapping("/{electionId}/{templateId}")
    public ResponseEntity<ThedalResponse<TemplateDTO>> updateTemplate(@PathVariable("electionId") Long electionId,
    		@PathVariable("templateId") Long templateId,
    		@RequestBody UpdateTemplateDTO template) {
        log.info("Received template: {}", template);
    	Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account ID not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }
        TemplateDTO templateResponse = templateService.updateTemplate(electionId,accountId,templateId,template);
        ThedalResponse<TemplateDTO> response = new ThedalResponse<>(ThedalSuccess.TEMPLATE_UPDATED, templateResponse);
        return ResponseEntity.ok(response);
    }
    
//    //@PatchMapping(value="/{electionId}/{templateId}/image",consumes = { "multipart/form-data" })
//    @PutMapping(value="/{electionId}/{templateId}/image",consumes = { "multipart/form-data" })
//    public ResponseEntity<ThedalResponse<String>> updateTemplateImage(
//            @PathVariable("electionId") Long electionId,
//            @PathVariable("templateId") Long templateId,
//            @RequestPart("file") MultipartFile file) {
//        
//        log.info("Updating template image for template ID: {}", templateId);
//
//        Long accountId = requestDetails.getCurrentAccountId();
//        if (accountId == null) {
//            log.error("Account ID not found, unauthorized access.");
//            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
//        }
//
//        String updatedImageUrl = templateService.updateTemplateImage(electionId, templateId, accountId, file);
//
//        ThedalResponse<String> response = new ThedalResponse<>(ThedalSuccess.IMAGE_UPLOADED_SUCCESSFULLY, updatedImageUrl);
//        return ResponseEntity.ok(response);
//    }
    @PutMapping(value="/{electionId}/name/{templateName}/image", consumes = { "multipart/form-data" })
    public ResponseEntity<ThedalResponse<String>> updateTemplateImage(
            @PathVariable("electionId") Long electionId,
            @PathVariable("templateName") String templateName,  // Changed from templateId
            @RequestPart("file") MultipartFile file) {
        
        log.info("Updating template image for templateName: {}", templateName);

        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account ID not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        String updatedImageUrl = templateService.updateTemplateImage(electionId, templateName, accountId, file);

        ThedalResponse<String> response = new ThedalResponse<>(ThedalSuccess.IMAGE_UPLOADED_SUCCESSFULLY, updatedImageUrl);
        return ResponseEntity.ok(response);
    }
    
//    @PutMapping("/{electionId}/{templateId}")
//    public ResponseEntity<ThedalResponse<TemplateUpdateDto>> deleteTemplate(@PathVariable("electionId") Long electionId,
//    		@PathVariable("templateId") Long templateId,
//    		@RequestBody UpdateTemplateStatusRequest request) {
//        log.info("delete template: {}", templateId);
//    	Long accountId = requestDetails.getCurrentAccountId();
//        if (accountId == null) {
//            log.error("Account ID not found, unauthorized access.");
//            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
//        }
//        TemplateUpdateDto templateResponse = templateService.deleteTemplate(electionId,accountId,templateId, request);
//        ThedalResponse<TemplateUpdateDto> response = new ThedalResponse<>(ThedalSuccess.TEMPLATE_DELETED, templateResponse);
//        return ResponseEntity.ok(response);
//    }
    @PutMapping("/{electionId}/name/{templateName}")
    public ResponseEntity<ThedalResponse<TemplateUpdateDto>> updateTemplateStatus(
            @PathVariable("electionId") Long electionId,
            @PathVariable("templateName") String templateName,  // Changed from templateId
            @RequestBody UpdateTemplateStatusRequest request) {
        log.info("Delete template: {}", templateName);

        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account ID not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }
        validateElectionOwnership(electionId, accountId);  
        TemplateUpdateDto templateResponse = templateService.updateTemplateStatus(electionId, accountId, templateName, request);
        ThedalResponse<TemplateUpdateDto> response = new ThedalResponse<>(ThedalSuccess.TEMPLATE_UPDATED, templateResponse);
        return ResponseEntity.ok(response);
    }
    
//    @PutMapping("/{electionId}/{templateId}/image/status")
//    public ResponseEntity<ThedalResponse<String>> toggleImageStatus(
//            @PathVariable Long electionId,
//            @PathVariable Long templateId,
//            @RequestBody ImageStatusRequest request) {
//
//    	log.info("Updating image status for template ID: {}, New Status: {}", templateId, request.getImageStatus());
//
//        Long accountId = requestDetails.getCurrentAccountId();
//        if (accountId == null) {
//            log.error("Account ID not found, unauthorized access.");
//            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
//        }
//
//        templateService.toggleImageStatus(electionId, templateId, accountId, request.getImageStatus());
//
//        ThedalResponse<String> response = new ThedalResponse<>(ThedalSuccess.IMAGE_STATUS_UPDATED);
//        return ResponseEntity.ok(response);
//    }
    @PutMapping("/{electionId}/name/{templateName}/image/status")
    public ResponseEntity<ThedalResponse<String>> toggleImageStatus(
            @PathVariable Long electionId,
            @PathVariable String templateName,  // Changed from templateId to templateName
            @RequestBody ImageStatusRequest request) {

        log.info("Updating image status for templateName: {}, New Status: {}", templateName, request.getImageStatus());

        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account ID not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        templateService.toggleImageStatus(electionId, templateName, accountId, request.getImageStatus());

        ThedalResponse<String> response = new ThedalResponse<>(ThedalSuccess.IMAGE_STATUS_UPDATED);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{electionId}/templates/reorder")
    public ResponseEntity<ThedalResponse<String>> reorderTemplates(
            @PathVariable Long electionId,
            @RequestBody List<TemplateReorderRequest> reorderRequests) {
        try {
            Long accountId = requestDetails.getCurrentAccountId();
            if (accountId == null) {
                log.error("Account ID not found, unauthorized access.");
                throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
            }

            templateService.updateTemplateOrder(reorderRequests, accountId, electionId);
            return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.TEMPLATE_ORDER_UPDATED));

        } catch (Exception e) {
            log.error("Error reordering templates: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ThedalResponse<>(ThedalError.TEMPLATE_ORDER_UPDATE_FAILED));
        }
    }

    @DeleteMapping("/{electionId}")
    public ThedalResponse<Void> deleteTemplates(
            @PathVariable("electionId") Long electionId,
            @RequestParam(value = "templateNames", required = false) List<String> templateNames) {
        
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }
        
        // Trim whitespace from template names to handle URL encoding issues (e.g., trailing + becomes space)
        List<String> templateNameList = (templateNames != null && !templateNames.isEmpty()) 
                ? templateNames.stream()
                    .map(String::trim)
                    .filter(name -> !name.isEmpty())
                    .collect(Collectors.toList())
                : Collections.emptyList();

        return templateService.deleteTemplates(electionId, accountId, templateNameList);
    }
//    @DeleteMapping("/{electionId}")
//    public ThedalResponse<Void> deleteTemplates(
//            @PathVariable("electionId") Long electionId,
//            @RequestParam(value = "templateNames", required = false) List<String> templateNames) {
//        
//        Long accountId = requestDetails.getCurrentAccountId();
//        if (accountId == null) {
//            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
//        }
//
//        // Validate that "Default" is not in the templateNames list
//        if (templateNames != null && templateNames.contains("Default")) {
//            log.error("Cannot delete default template for electionId: {}, accountId: {}", electionId, accountId);
//            throw new ThedalException(ThedalError.DEFAULT_TEMPLATE_CANNOT_BE_DELETED, HttpStatus.BAD_REQUEST);
//        }
//
//        List<String> templateNameList = (templateNames != null && !templateNames.isEmpty()) 
//                ? templateNames 
//                : Collections.emptyList();
//
//        return templateService.deleteTemplates(electionId, accountId, templateNameList);
//    }
    
    @PutMapping("/{electionId}/templates/{templateName}/details")
    public ResponseEntity<ThedalResponse<UpdateTemplateDetailsResponse>> updateTemplateDetails(
            @PathVariable("electionId") Long electionId,
            @PathVariable("templateName") String templateName,
            @RequestBody UpdateTemplateDetailsRequest request) {
    
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }
    
        validateElectionOwnership(electionId, accountId);
    
        UpdateTemplateDetailsResponse responseDto = templateService.updateTemplateDetails(electionId, accountId, templateName, request);
    
        ThedalResponse<UpdateTemplateDetailsResponse> response = new ThedalResponse<>(ThedalSuccess.TEMPLATE_UPDATED, responseDto);
        return ResponseEntity.ok(response);
    }
    

    
}
