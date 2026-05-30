package com.thedal.thedal_app.election;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.thedal.thedal_app.awsfilestore.AwsFileUpload;
import com.thedal.thedal_app.awsfilestore.ImageUpload;
import com.thedal.thedal_app.cpanel.SlipBoxEntity;
import com.thedal.thedal_app.cpanel.SlipBoxRepository;
import com.thedal.thedal_app.election.dtos.ElectionBody;
import com.thedal.thedal_app.election.dtos.ElectionCategory;
import com.thedal.thedal_app.election.dtos.ElectionDTO;
import com.thedal.thedal_app.election.dtos.ElectionIdImageDTO;
import com.thedal.thedal_app.election.dtos.ElectionReorderRequest;
import com.thedal.thedal_app.election.dtos.ElectionResponseDTO;
import com.thedal.thedal_app.election.dtos.ElectionType;
import com.thedal.thedal_app.election.dtos.OtpSentResponse;
import com.thedal.thedal_app.files.FileReorderRequest;
import com.thedal.thedal_app.files.Files;
import com.thedal.thedal_app.files.FilesMongo;
import com.thedal.thedal_app.files.FilesMongoRepository;
import com.thedal.thedal_app.files.FilesRepository;
import com.thedal.thedal_app.files.HandlerType;
import com.thedal.thedal_app.general.RequestDetailsService;
import com.thedal.thedal_app.notification.NotificationService;
import com.thedal.thedal_app.notification.NotificationTemplate;
import com.thedal.thedal_app.notification.SmsNotification;
import com.thedal.thedal_app.response.ThedalResponse;
import com.thedal.thedal_app.response.ThedalSuccess;
import com.thedal.thedal_app.role.RolePermission;
import com.thedal.thedal_app.settings.electionsettings.ComplaintEntity;
import com.thedal.thedal_app.settings.electionsettings.ComplaintRepository;
import com.thedal.thedal_app.settings.electionsettings.ElectionTypeRepository;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;
import com.thedal.thedal_app.user.UserEntity;
import com.thedal.thedal_app.user.UserRepo;
import com.thedal.thedal_app.util.RandomTokenGenerator;
import com.thedal.thedal_app.volunteer.VolunteerEntity;
import com.thedal.thedal_app.volunteer.VolunteerRepository;
import com.thedal.thedal_app.election.ElectionFreezeInterceptor.CheckElectionNotFrozen;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ElectionService {

    @Autowired
    private ElectionRepository electionRepository;

    @Autowired
    private ElectionStateRepository electionStateRepository;

    @Autowired
    private ElectionBoothRepository electionBoothRepository;

    @Autowired
    private ImageUpload imageUpload;

    @Autowired
    private NotificationTemplate notificationTemplate;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private RequestDetailsService requestDetails;   
    @Autowired
    private AwsFileUpload awsFileUpload;    
    @Autowired
    private FilesRepository filesRepository;
    @Autowired
    private ElectionTypeRepository electionTypeRepository;
    @Autowired
    private ComplaintRepository complaintRepository;
    @Autowired
    private ElectionFreezeOtpRepository electionFreezeOtpRepository;
    @Autowired
    private VolunteerRepository volunteerRepository;

    @Autowired
    private UserRepo userRepo;
    @Autowired
    private TemplateRepository templateRepository;
    @Autowired
    private VolunteerRepository volunteerRepo;
    @Autowired
    private SlipBoxRepository slipBoxRepository;
    
    // MongoDB repositories for dual write
    @Autowired
    private ElectionMongoRepository electionMongoRepository;
    @Autowired
    private FilesMongoRepository filesMongoRepository;
    @Autowired
    private TemplateMongoRepository templateMongoRepository;
    @Autowired
    private ElectionDeleteOtpRepository electionDeleteOtpRepository;
    @Autowired
    private SmsNotification smsNotification;
    @Autowired
    private OtpService otpService;
    
    @Autowired
    private StaticFieldStatusService staticFieldStatusService;
    @Autowired
    private com.thedal.thedal_app.role.RoleInitializationService roleInitializationService;
    @Autowired
    private com.thedal.thedal_app.settings.electionsettings.PartyRepository partyRepository;
    @Autowired
    private com.thedal.thedal_app.settings.electionsettings.PartyMongoRepository partyMongoRepository;

    
    
    private static final Set<Long> ALLOWED_TEMPLATE_IDS = Set.of(1L, 2L, 3L, 4L);

    @Value("${aws.s3.banner.bucket}")
	private String s3bucket;
    
    private void ensureOneActiveTemplate(Long electionId, Long accountId) {
        List<TemplateEntity> activeTemplates = templateRepository.findByElectionIdAndAccountIdAndIsActive(electionId, accountId, true);
        if (activeTemplates.size() > 1) {
            // Keep the first active template, deactivate others
            for (int i = 1; i < activeTemplates.size(); i++) {
                TemplateEntity template = activeTemplates.get(i);
                template.setIsActive(false);
                template.setImageStatus(false); // Enforce imageStatus = false when isActive = false
                templateRepository.save(template);
                log.info("Deactivated extra active template: {} for electionId: {}", template.getTemplateName(), electionId);
            }
        } else if (activeTemplates.isEmpty()) {
            // Activate the default template or create a new one
            Optional<TemplateEntity> defaultTemplateOpt = templateRepository.findByElectionIdAndAccountIdAndTemplateName(electionId, accountId, "Default");
            if (defaultTemplateOpt.isPresent()) {
                TemplateEntity defaultTemplate = defaultTemplateOpt.get();
                defaultTemplate.setIsActive(true);
                defaultTemplate.setImageStatus(false); // Ensure imageStatus is false for default
                templateRepository.save(defaultTemplate);
                log.info("Activated default template for electionId: {}", electionId);
            } else {
                log.warn("No default template found for electionId: {}. Creating a new default one.", electionId);
                TemplateEntity newDefaultTemplate = new TemplateEntity();
                newDefaultTemplate.setTemplateId(1L);
                newDefaultTemplate.setAccountId(accountId);
                newDefaultTemplate.setElectionId(electionId);
                newDefaultTemplate.setTemplateName("Default");
                newDefaultTemplate.setIsActive(true);
                newDefaultTemplate.setImageStatus(false);
                newDefaultTemplate.setOrderIndex(0);
                newDefaultTemplate.setSlipId(UUID.randomUUID().toString());
                newDefaultTemplate.setVoterSlipHeader("Default Voter Slip Header");
                newDefaultTemplate.setCandidateInfoImageFooter("Default Candidate Info Footer");
                templateRepository.save(newDefaultTemplate);
            }
        }
    }
    
    public ResponseEntity<ThedalResponse<Long>> createElection(ElectionDTO request) {
    	
    	log.info("Received request to create election: {}", request);

        //requestDetails.checkUserRolePermission(RolePermission.SETTINGS_MANAGEMENT);

        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account ID not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }
        
     // Get the highest order index for elections under the same account
        Integer maxOrderIndex = electionRepository.findMaxOrderIndexByAccountId(accountId);
        int newOrderIndex = (maxOrderIndex != null) ? maxOrderIndex + 1 : 0;
//        Integer maxTemplateOrderIndex = templateRepository.findMaxOrderIndexByAccountId(accountId);
//        int newTemplateOrderIndex = (maxTemplateOrderIndex != null) ? maxTemplateOrderIndex + 1 : 0;

//        if (!ALLOWED_TEMPLATE_IDS.contains(request.getTemplateId())) {
//            log.error("Invalid Template ID: {}", request.getTemplateId());
//            throw new ThedalException(ThedalError.INVALID_TEMPLATE_ID, HttpStatus.BAD_REQUEST);
//        }

        // Validation for election category and body
        validateElectionRequest(request);
        
        ThedalResponse<Long> response = new ThedalResponse<>();

        // Create a new ElectionEntity from the DTO
        ElectionEntity election = new ElectionEntity();
        election.setAccountId(accountId);
        election.setElectionName(request.getElectionName());
        election.setElectionType(request.getElectionType());
        election.setStartDate(request.getStartDate());
        election.setEndDate(request.getEndDate());
        election.setCategory(request.getCategory());
        election.setStateName(request.getStateName());
        election.setYear(request.getYear());
        election.setMonth(request.getMonth());
        election.setStatus(request.getStatus());
        election.setNumberOfPollingStations(request.getNumberOfPollingStations());
        election.setNumberOfPhases(request.getNumberOfPhases());
        election.setNumberOfPinkBooths(request.getNumberOfPinkBooths());
        election.setNumberOfVoters(request.getNumberOfVoters());
        election.setNumberOfMaleVoters(request.getNumberOfMaleVoters());
        election.setNumberOfFemaleVoters(request.getNumberOfFemaleVoters());
        election.setNumberOfTransgenderVoters(request.getNumberOfTransgenderVoters());
        election.setRemarks(request.getRemarks());
        election.setBoothCount(request.getBoothCount());
        //election.setBoothNumber(request.getBoothNumber());
        election.setNotificationDate(request.getNotificationDate());
        election.setLastDateForFillingNomination(request.getLastDateForFillingNomination());
        election.setDateOfPoll(request.getDateOfPoll());
        election.setScrutinyNominationDate(request.getScrutinyNominationDate());
        election.setLastDateForWithdrawalOfNomination(request.getLastDateForWithdrawalOfNomination());
        election.setDateOfCountingOfVotes(request.getDateOfCountingOfVotes());
        election.setElectionDescription(request.getElectionDescription());
        //election.setBody(request.getBody());
        election.setElectionCategory(request.getElectionCategory());
        election.setType(request.getType());
        election.setPcName(request.getPcName());
        election.setAcName(request.getAcName());
        election.setUrbanName(request.getUrbanName());
        election.setRuralName(request.getRuralName());
        election.setPhaseNo(request.getPhaseNo());
        election.setCountry(request.getCountry()); 
        election.setState(request.getState());
        election.setOrderIndex(newOrderIndex);
        // Set newly added fields
        election.setGazetteNotificationDate(request.getGazetteNotificationDate());
        log.info("Gazette Notification Date: {}", request.getGazetteNotificationDate());
        election.setCompletionDeadlineDate(request.getCompletionDeadlineDate());
        log.info("Completion Deadline Date: {}", request.getCompletionDeadlineDate());
        
        if (request.getElectionCategory() == ElectionCategory.POLITICAL) {
            // Check if the body provided is valid for POLITICAL
            if (request.getBody() == null || 
               !(request.getBody() == ElectionBody.UNION_BODY || 
                 request.getBody() == ElectionBody.STATE_BODY || 
                 request.getBody() == ElectionBody.URBAN_LOCAL || 
                 request.getBody() == ElectionBody.RURAL_LOCAL)) {

                log.error("Invalid Election Body for POLITICAL category: {}", request.getBody());
                throw new ThedalException(ThedalError.INVALID_ELECTION_BODY, HttpStatus.BAD_REQUEST);
            }
            election.setBody(request.getBody());
        } else if (request.getElectionCategory() == ElectionCategory.NON_POLITICAL) {
            election.setBodyString(request.getBodyString()); // Allow bodyString for NON_POLITICAL
            election.setBody(null); // Ensure enum body is null for NON_POLITICAL
        }
        
        election.setElectoralReleaseDate(request.getElectoralReleaseDate());
       
        // Save the election entity
        ElectionEntity savedElection = electionRepository.save(election);
        
        // Initialize default static field statuses for the new election
        try {
            staticFieldStatusService.initializeDefaultStaticFields(accountId, savedElection.getId());
            log.info("Successfully initialized static field statuses for election ID: {}", savedElection.getId());
        } catch (Exception e) {
            log.error("Failed to initialize static field statuses for election ID: {}, error: {}", 
                    savedElection.getId(), e.getMessage());
            // Note: We don't fail the entire election creation if static field initialization fails
        }

        // Initialize default roles for the account (if not already created)
        try {
            roleInitializationService.initializeDefaultRoles(accountId, savedElection.getId());
            log.info("Successfully initialized default roles for election ID: {}", savedElection.getId());
        } catch (Exception e) {
            log.error("Failed to initialize default roles for election ID: {}, error: {}", 
                    savedElection.getId(), e.getMessage());
            // Note: We don't fail the entire election creation if role initialization fails
        }

     // Handle complaint if provided
        if (request.getComplaint() != null) {
            ComplaintEntity complaintEntity = new ComplaintEntity();
            complaintEntity.setComplaintName(request.getComplaint());
            complaintEntity.setAccountId(accountId);  // Set account ID if needed

            // Save the complaint entity
            ComplaintEntity savedComplaint = complaintRepository.save(complaintEntity);

            // Associate the complaint ID with the election
            savedElection.setComplaint(savedComplaint);
            electionRepository.save(savedElection);  
        }
        
     // Create and Associate Template with the Election
        TemplateEntity template = new TemplateEntity();
        //template.setTemplateId(request.getTemplateId());
        template.setTemplateId(1L); 
        template.setAccountId(accountId);
        template.setElectionId(savedElection.getId()); 
        template.setTemplateName("Default"); 
        template.setImageUrl(null); 
        template.setIsActive(true);
        template.setImageStatus(false);
//        template.setOrderIndex(newTemplateOrderIndex);
        template.setOrderIndex(0);
        template.setSlipId(UUID.randomUUID().toString());
        template.setVoterSlipHeader("Default Voter Slip Header");
        template.setCandidateInfoImageFooter("Default Candidate Info Footer");
       // templateRepository.save(template);
     
        TemplateEntity savedTemplate = templateRepository.save(template);
        
        // Save to MongoDB
        try {
            TemplateMongo templateMongo = new TemplateMongo(savedTemplate);
            templateMongoRepository.save(templateMongo);
            log.info("Successfully saved default template to MongoDB: id={}, name={}", savedTemplate.getId(), savedTemplate.getTemplateName());
        } catch (Exception mongoEx) {
            log.error("Failed to save default template to MongoDB: id={}, name={}", savedTemplate.getId(), savedTemplate.getTemplateName(), mongoEx);
            throw new RuntimeException("MongoDB save failed, triggering rollback", mongoEx);
        }
        
     // Ensure only one active template
        ensureOneActiveTemplate(savedElection.getId(), accountId);
        
     // Create Default SlipBox
        SlipBoxEntity defaultSlipBox = new SlipBoxEntity();
        defaultSlipBox.setMobileNumber("0000000000"); // Placeholder, adjust as needed
        defaultSlipBox.setSlipBoxName("TEMP"); // Temporary name
        defaultSlipBox.setSlipBoxId(UUID.randomUUID().toString());
        defaultSlipBox.setAccountId(accountId);
        defaultSlipBox.setElectionId(savedElection.getId());
        defaultSlipBox.setIsDefault(true); // Mark as default
        SlipBoxEntity savedSlipBox = slipBoxRepository.save(defaultSlipBox);

        // Update slipBoxName with TEAM + last 4 digits of id
        String slipBoxName = String.format("TEAM%04d", savedSlipBox.getId() % 10000);
        savedSlipBox.setSlipBoxName(slipBoxName);
        slipBoxRepository.save(savedSlipBox);

        response.setResponse(ThedalSuccess.ELECTION_CREATED, savedElection.getId());
        log.info("Election created successfully with ID: {}", savedElection.getId());
        return ResponseEntity.ok(response);
        
    }
    private void validateElectionRequest(ElectionDTO request) {
        log.info("Validating election request: {}", request);

        // Check if the election category is POLITICAL
        if (request.getElectionCategory() == ElectionCategory.POLITICAL) {
            // Ensure 'body' is provided and valid
            if (request.getBody() == null || 
                !(request.getBody() == ElectionBody.UNION_BODY || 
                  request.getBody() == ElectionBody.STATE_BODY || 
                  request.getBody() == ElectionBody.URBAN_LOCAL || 
                  request.getBody() == ElectionBody.RURAL_LOCAL)) {
                log.error("Invalid or missing Election Body for POLITICAL category: {}", request.getBody());
                throw new ThedalException(ThedalError.INVALID_ELECTION_BODY, HttpStatus.BAD_REQUEST);
            }

            // Ensure 'bodyString' is null
            if (request.getBodyString() != null) {
                log.error("bodyString must be null for POLITICAL category.");
                throw new ThedalException(ThedalError.INVALID_ELECTION_BODY, HttpStatus.BAD_REQUEST);
            }
        } 
        // Check if the election category is NON_POLITICAL
        else if (request.getElectionCategory() == ElectionCategory.NON_POLITICAL) {
            // Ensure 'bodyString' is provided
            if (request.getBodyString() == null || request.getBodyString().isEmpty()) {
                log.error("Missing bodyString for NON_POLITICAL category.");
                throw new ThedalException(ThedalError.INVALID_ELECTION_BODY, HttpStatus.BAD_REQUEST);
            }

            // Ensure 'body' is null
            if (request.getBody() != null) {
                log.error("body must be null for NON_POLITICAL category.");
                throw new ThedalException(ThedalError.INVALID_ELECTION_BODY, HttpStatus.BAD_REQUEST);
            }
        } 
        // Handle invalid category
        else {
            log.error("Invalid election category: {}", request.getElectionCategory());
            throw new ThedalException(ThedalError.INVALID_ELECTION_CATEGORY, HttpStatus.BAD_REQUEST);
        }
    }

   
    
    

    @Transactional
    public ResponseEntity<ThedalResponse<String>> updateElectionImage(Long electionId,
            MultipartFile multipartFile) {
    	
    	ThedalResponse<String> response = new ThedalResponse<>();

        Optional<ElectionEntity> optionalElection = electionRepository.findByIdAndAccountIdAndIsDeletedFalse(electionId, requestDetails.getCurrentAccountId());
        if (!optionalElection.isPresent()) {
            response.setResponse(ThedalError.ELECTION_NOT_FOUND);
            return ResponseEntity.badRequest().body(response);
        }

        String uploadUrl = imageUpload.uploadElectionImageToAWS(multipartFile);
        ElectionEntity election = optionalElection.get();
        election.setImageUrl(uploadUrl);
        
        try {
            ElectionEntity updatedElection = electionRepository.saveAndFlush(election);
            try {
                ElectionMongo electionMongo = new ElectionMongo(updatedElection);
                electionMongoRepository.save(electionMongo);
                log.info("Successfully updated election image in MongoDB: id={}", updatedElection.getId());
            } catch (Exception mongoEx) {
                log.error("Failed to update election image in MongoDB: id={}", updatedElection.getId(), mongoEx);
                throw new RuntimeException("MongoDB update failed, triggering rollback", mongoEx);
            }
        } catch (Exception ex) {
            log.error("Failed to update election image: id={}", electionId, ex);
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        response.setResponse(ThedalSuccess.ELECTION_UPDATED_IMAGE, uploadUrl);
        return ResponseEntity.ok(response);

    }

//    public ResponseEntity<ThedalResponse<ElectionIdImageDTO>> createElectionWithImage(MultipartFile multipartFile) {
//    	
//    	ThedalResponse<ElectionIdImageDTO> response = new ThedalResponse<>();
//
//        String uploadUrl = imageUpload.uploadElectionImageToAWS(multipartFile);
//        ElectionEntity electionEntity = new ElectionEntity();
//        electionEntity.setAccountId(requestDetails.getCurrentAccountId());
//        electionEntity.setImageUrl(uploadUrl);
//     // Set the body field (choose a default or dynamic value based on your logic)
//        electionEntity.setBody(ElectionBody.UNION_BODY);
//        electionEntity.setElectionCategory(ElectionCategory.POLITICAL);
//        electionEntity.setType(ElectionType.GENERAL_ELECTION);
//        
//        ElectionEntity savedElectionEntity = electionRepository.save(electionEntity);
//
//        ElectionIdImageDTO electionIdImageDTO = new ElectionIdImageDTO();
//        electionIdImageDTO.setElectionId(savedElectionEntity.getId());
//        electionIdImageDTO.setImageUrl(uploadUrl);
//
//        response.setResponse(ThedalSuccess.ELECTION_CREATED_IMAGE, electionIdImageDTO);
//        return ResponseEntity.ok(response);
//    }
    @Transactional
    public ResponseEntity<ThedalResponse<ElectionIdImageDTO>> createElectionWithImage(MultipartFile multipartFile) {
        
        ThedalResponse<ElectionIdImageDTO> response = new ThedalResponse<>();
    
        String uploadUrl = imageUpload.uploadElectionImageToAWS(multipartFile);
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account ID not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        // Determine the new order index
        Integer maxOrderIndex = electionRepository.findMaxOrderIndexByAccountId(accountId);
        int newOrderIndex = (maxOrderIndex != null) ? maxOrderIndex + 1 : 0;
//        // Determine the new template order index
//        Integer maxTemplateOrderIndex = templateRepository.findMaxOrderIndexByAccountId(accountId);
//        int newTemplateOrderIndex = (maxTemplateOrderIndex != null) ? maxTemplateOrderIndex + 1 : 0;        
        
        ElectionEntity electionEntity = new ElectionEntity();
        electionEntity.setAccountId(accountId);
        electionEntity.setImageUrl(uploadUrl);
        electionEntity.setBody(ElectionBody.UNION_BODY);
        electionEntity.setElectionCategory(ElectionCategory.POLITICAL);
        electionEntity.setType(ElectionType.GENERAL_ELECTION);
        electionEntity.setOrderIndex(newOrderIndex);  

        try {
            ElectionEntity savedElectionEntity = electionRepository.saveAndFlush(electionEntity);
            try {
                ElectionMongo electionMongo = new ElectionMongo(savedElectionEntity);
                electionMongoRepository.save(electionMongo);
                log.info("Successfully saved new election to MongoDB: id={}, name={}", savedElectionEntity.getId(), savedElectionEntity.getElectionName());
            } catch (Exception mongoEx) {
                log.error("Failed to save new election to MongoDB: id={}, name={}", savedElectionEntity.getId(), savedElectionEntity.getElectionName(), mongoEx);
                throw new RuntimeException("MongoDB save failed, triggering rollback", mongoEx);
            }

            // Create a Default Template for the Election
            TemplateEntity template = new TemplateEntity();
            template.setTemplateId(1L); 
            template.setAccountId(accountId);
            template.setElectionId(savedElectionEntity.getId());
            template.setTemplateName("Default"); 
            template.setImageUrl(null);
            template.setIsActive(true);
            template.setImageStatus(false);
            //template.setOrderIndex(newTemplateOrderIndex); 
            template.setOrderIndex(0);
            template.setSlipId(UUID.randomUUID().toString());
            template.setVoterSlipHeader("Default Voter Slip Header");
            template.setCandidateInfoImageFooter("Default Candidate Info Footer");
            //templateRepository.save(template); 
            TemplateEntity savedTemplate = templateRepository.save(template);
            
            // Save to MongoDB
            try {
                TemplateMongo templateMongo = new TemplateMongo(savedTemplate);
                templateMongoRepository.save(templateMongo);
                log.info("Successfully saved default template to MongoDB: id={}, name={}", savedTemplate.getId(), savedTemplate.getTemplateName());
            } catch (Exception mongoEx) {
                log.error("Failed to save default template to MongoDB: id={}, name={}", savedTemplate.getId(), savedTemplate.getTemplateName(), mongoEx);
                throw new RuntimeException("MongoDB save failed, triggering rollback", mongoEx);
            }
            
            // Ensure only one active template
            ensureOneActiveTemplate(savedElectionEntity.getId(), accountId);
            
            // Create Default SlipBox
            SlipBoxEntity defaultSlipBox = new SlipBoxEntity();
            defaultSlipBox.setMobileNumber("0000000000"); // Placeholder, adjust as needed
            defaultSlipBox.setSlipBoxName("TEMP"); // Temporary name
            defaultSlipBox.setSlipBoxId(UUID.randomUUID().toString());
            defaultSlipBox.setAccountId(accountId);
            defaultSlipBox.setElectionId(savedElectionEntity.getId());
            defaultSlipBox.setIsDefault(true); // Mark as default
            SlipBoxEntity savedSlipBox = slipBoxRepository.save(defaultSlipBox);

            // Update slipBoxName with TEAM + last 4 digits of id
            String slipBoxName = String.format("TEAM%04d", savedSlipBox.getId() % 10000);
            savedSlipBox.setSlipBoxName(slipBoxName);
            slipBoxRepository.save(savedSlipBox);

            ElectionIdImageDTO electionIdImageDTO = new ElectionIdImageDTO();
            electionIdImageDTO.setElectionId(savedElectionEntity.getId());
            electionIdImageDTO.setImageUrl(uploadUrl);
            //electionIdImageDTO.setOrderIndex(maxTemplateOrderIndex);

            response.setResponse(ThedalSuccess.ELECTION_CREATED_IMAGE, electionIdImageDTO);
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            log.error("Failed to create election with image", ex);
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional
    @CheckElectionNotFrozen(electionIdParamIndex = 0)
    public ResponseEntity<ThedalResponse<String>> updateElectionFields(Long electionId, ElectionDTO request) {
    	
    	log.info("Received request to update election with ID {}: {}", electionId, request);

        requestDetails.checkUserRolePermission(RolePermission.SETTINGS_MANAGEMENT);

        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account ID not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        // Fetch existing election entity
        ElectionEntity existingElection = electionRepository.findByIdAndAccountId(electionId, accountId)
                .orElseThrow(() -> new ThedalException(ThedalError.ELECTION_NOT_FOUND, HttpStatus.NOT_FOUND));

        // Validate the election request
        validateElectionRequest(request);

        // Update fields only if provided in the request
        if (request.getElectionName() != null) existingElection.setElectionName(request.getElectionName());
        if (request.getElectionType() != null) existingElection.setElectionType(request.getElectionType());
        if (request.getStartDate() != null) existingElection.setStartDate(request.getStartDate());
        if (request.getEndDate() != null) existingElection.setEndDate(request.getEndDate());
        if (request.getCategory() != null) existingElection.setCategory(request.getCategory());
        if (request.getStateName() != null) existingElection.setStateName(request.getStateName());
        if (request.getYear() != null) existingElection.setYear(request.getYear());
        if (request.getMonth() != null) existingElection.setMonth(request.getMonth());
        if (request.getStatus() != null) existingElection.setStatus(request.getStatus());
        if (request.getNumberOfPollingStations() != null)
            existingElection.setNumberOfPollingStations(request.getNumberOfPollingStations());
        if (request.getNumberOfPhases() != null) existingElection.setNumberOfPhases(request.getNumberOfPhases());
        if (request.getNumberOfPinkBooths() != null)
            existingElection.setNumberOfPinkBooths(request.getNumberOfPinkBooths());
        if (request.getNumberOfVoters() != null) existingElection.setNumberOfVoters(request.getNumberOfVoters());
        if (request.getNumberOfMaleVoters() != null)
            existingElection.setNumberOfMaleVoters(request.getNumberOfMaleVoters());
        if (request.getNumberOfFemaleVoters() != null)
            existingElection.setNumberOfFemaleVoters(request.getNumberOfFemaleVoters());
        if (request.getNumberOfTransgenderVoters() != null)
            existingElection.setNumberOfTransgenderVoters(request.getNumberOfTransgenderVoters());
        if (request.getRemarks() != null) existingElection.setRemarks(request.getRemarks());
        if (request.getBoothCount() != null) existingElection.setBoothCount(request.getBoothCount());
        if (request.getNotificationDate() != null)
            existingElection.setNotificationDate(request.getNotificationDate());
        if (request.getLastDateForFillingNomination() != null)
            existingElection.setLastDateForFillingNomination(request.getLastDateForFillingNomination());
        if (request.getDateOfPoll() != null) existingElection.setDateOfPoll(request.getDateOfPoll());
        if (request.getScrutinyNominationDate() != null)
            existingElection.setScrutinyNominationDate(request.getScrutinyNominationDate());
        if (request.getLastDateForWithdrawalOfNomination() != null)
            existingElection.setLastDateForWithdrawalOfNomination(request.getLastDateForWithdrawalOfNomination());
        if (request.getDateOfCountingOfVotes() != null)
            existingElection.setDateOfCountingOfVotes(request.getDateOfCountingOfVotes());
        if (request.getElectoralReleaseDate() != null)
            existingElection.setElectoralReleaseDate(request.getElectoralReleaseDate());
        if (request.getElectionDescription() != null)
            existingElection.setElectionDescription(request.getElectionDescription());
        
        if (request.getPcName() != null) {existingElection.setPcName(request.getPcName());}
        if (request.getAcName() != null) {existingElection.setAcName(request.getAcName());}
        if (request.getUrbanName() != null) {existingElection.setUrbanName(request.getUrbanName());}
        if (request.getRuralName() != null) { existingElection.setRuralName(request.getRuralName());}
        if (request.getPhaseNo() != null) {existingElection.setPhaseNo(request.getPhaseNo());} 
        if (request.getCountry() != null) {existingElection.setCountry(request.getCountry());} 
        if (request.getState() != null) {existingElection.setState(request.getState());} 
          // Handle newly added fields
          if (request.getGazetteNotificationDate() != null)
              existingElection.setGazetteNotificationDate(request.getGazetteNotificationDate());
          if (request.getCompletionDeadlineDate() != null)
              existingElection.setCompletionDeadlineDate(request.getCompletionDeadlineDate());

          if (request.getComplaint() != null) {
              ComplaintEntity complaintEntity = new ComplaintEntity();
              complaintEntity.setComplaintName(request.getComplaint());
              complaintEntity.setAccountId(accountId);

              ComplaintEntity savedComplaint = complaintRepository.save(complaintEntity);
              existingElection.setComplaint(savedComplaint);
          }
        

//        // Handle election category-specific updates
//        if (request.getElectionCategory() == ElectionCategory.POLITICAL) {
//            if (request.getBody() != null) existingElection.setBody(request.getBody());
//            existingElection.setBodyString(null); // Ensure bodyString is null for POLITICAL
//        } else if (request.getElectionCategory() == ElectionCategory.NON_POLITICAL) {
//            if (request.getBodyString() != null) existingElection.setBodyString(request.getBodyString());
//            existingElection.setBody(null); // Ensure body is null for NON_POLITICAL
//        }
                    
        if (request.getBody() != null) {existingElection.setBody(request.getBody());}
        if (request.getElectionCategory() != null) {existingElection.setElectionCategory(request.getElectionCategory());}
          
        if (request.getElectionCategory() == ElectionCategory.POLITICAL) {
            // Check if the body provided is valid for POLITICAL
            if (request.getBody() == null || 
               !(request.getBody() == ElectionBody.UNION_BODY || 
                 request.getBody() == ElectionBody.STATE_BODY || 
                 request.getBody() == ElectionBody.URBAN_LOCAL || 
                 request.getBody() == ElectionBody.RURAL_LOCAL)) {

                log.error("Invalid Election Body for POLITICAL category: {}", request.getBody());
                throw new ThedalException(ThedalError.INVALID_ELECTION_BODY, HttpStatus.BAD_REQUEST);
            }
            existingElection.setBody(request.getBody());
        } else if (request.getElectionCategory() == ElectionCategory.NON_POLITICAL) {
        	existingElection.setBodyString(request.getBodyString()); // Allow bodyString for NON_POLITICAL
        	existingElection.setBody(null); // Ensure enum body is null for NON_POLITICAL
        }

        // Save the updated election entity
        try {
            ElectionEntity updatedElection = electionRepository.saveAndFlush(existingElection);
            try {
                ElectionMongo electionMongo = new ElectionMongo(updatedElection);
                electionMongoRepository.save(electionMongo);
                log.info("Successfully updated election in MongoDB: id={}, name={}", updatedElection.getId(), updatedElection.getElectionName());
            } catch (Exception mongoEx) {
                log.error("Failed to update election in MongoDB: id={}, name={}", updatedElection.getId(), updatedElection.getElectionName(), mongoEx);
                throw new RuntimeException("MongoDB update failed, triggering rollback", mongoEx);
            }
            log.info("Election updated successfully: {}", updatedElection.getElectionName());
            
            ThedalResponse<String> response = new ThedalResponse<>();
            response.setResponse(ThedalSuccess.ELECTION_UPDATED);
            log.info("Election updated successfully with ID: {}", updatedElection.getId());
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            log.error("Failed to update election: id={}", electionId, ex);
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    
    
    
//    public ResponseEntity<ThedalResponse<List<ElectionResponseDTO>>> getElections() {
//    	
//    	log.info("Entering getElections method.");
//    	ThedalResponse<List<ElectionResponseDTO>> response = new ThedalResponse<>();
//        Long accountId = requestDetails.getCurrentAccountId();
//        log.debug("Fetching elections for accountId: {}", accountId);
//        Long userId = requestDetails.getCurrentUserId(); // Get current user ID
//        log.debug("Fetching elections for userId: {}", userId);
//       UserEntity currentUser = userRepo.findById(userId)
//                                .orElseThrow(() -> new RuntimeException("User not found"));
//       Role userRole = currentUser.getRole();
//       log.debug("Fetching elections for accountId: {}, userRole: {}", accountId, userRole);
//
//       List<ElectionEntity> elections;
//        
//        try {  
//
//        if ("VOLUNTEER".equalsIgnoreCase(userRole.getRoleName())) {
//            // Fetch elections assigned to the volunteer
//            elections = electionRepository.findElectionsByVolunteer(userId, accountId);
//            log.info("Fetching assigned elections for volunteer with accountId: {}", accountId);
//        }else {
//          //List<ElectionEntity> elections = electionRepository.findByAccountId(accountId);
//            elections = electionRepository.findAllActiveElections(accountId);
//
//        }
//        
//        elections.sort(Comparator.comparing(ElectionEntity::getOrderIndex));
//            if (elections.isEmpty()) {
//               log.warn("No elections found for accountId: {}", accountId);
//            }       
//        
//          List<ElectionResponseDTO> electionDTOs = elections.stream()
//                .map(this::convertToElectionDTO)
//                .collect(Collectors.toList());
//
//           response.setResponse(ThedalSuccess.ELECTION_FETCHED, electionDTOs);
//           log.info("Successfully fetched elections for accountId: {}", accountId);
//        } catch (Exception e) {
//            log.error("Error occurred while fetching elections for accountId: {}", accountId, e);
//            response.setResponse(ThedalError.ELECTION_FETCH_FAILED);
//            return ResponseEntity.status(500).body(response);
//        } 
//        
//        return ResponseEntity.ok(response);
//    }
    public ResponseEntity<ThedalResponse<List<ElectionResponseDTO>>> getElections() {
        log.info("Entering getElections method - reading from PostgreSQL.");
        ThedalResponse<List<ElectionResponseDTO>> response = new ThedalResponse<>();
        Long accountId = requestDetails.getCurrentAccountId();
        Long userId = requestDetails.getCurrentUserId();
        UserEntity currentUser = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<ElectionMongo> elections;

        try {
            // Check if the user is associated with a volunteer entity
            Optional<VolunteerEntity> volunteerOpt = volunteerRepo.findByUserEntityId(userId);
            if (volunteerOpt.isPresent()) {
                // If user is a volunteer, show only their associated election
                ElectionEntity volunteerElection = volunteerOpt.get().getElectionEntity();
                if (volunteerElection == null || volunteerElection.getIsDeleted()) {
                    log.warn("No active election found for volunteer with userId: {}", userId);
                    elections = Collections.emptyList();
                } else {
                    // Find the specific election in PostgreSQL instead of MongoDB
                    Optional<ElectionEntity> pgElection = electionRepository.findByIdAndAccountIdAndIsDeletedFalse(
                            volunteerElection.getId(), accountId);
                    elections = pgElection.map(entity -> Collections.singletonList(new ElectionMongo(entity))).orElse(Collections.emptyList());
                }
                log.info("Fetched election from PostgreSQL for volunteer with userId: {}", userId);
            } else {
                // For non-volunteers, fetch all active elections from PostgreSQL instead of MongoDB
                List<ElectionEntity> pgElections = electionRepository.findAllActiveElections(accountId);
                elections = pgElections.stream().map(ElectionMongo::new).collect(Collectors.toList());
            }

            if (elections.isEmpty()) {
                log.warn("No elections found in MongoDB for accountId: {}", accountId);
            }

            List<ElectionResponseDTO> electionDTOs = elections.stream()
                    .map(this::convertToElectionDTOFromMongo)
                    .collect(Collectors.toList());

            response.setResponse(ThedalSuccess.ELECTION_FETCHED, electionDTOs);
            log.info("Successfully fetched {} elections from MongoDB for accountId: {}", electionDTOs.size(), accountId);
        } catch (Exception e) {
            log.error("Error occurred while fetching elections from MongoDB for accountId: {}", accountId, e);
            response.setResponse(ThedalError.ELECTION_FETCH_FAILED);
            return ResponseEntity.status(500).body(response);
        }

        return ResponseEntity.ok(response);
    }
    
    
    private ElectionResponseDTO convertToElectionDTO(ElectionEntity election) {
        log.debug("Converting election entity to DTO for electionId: {}", election.getId());
        ElectionResponseDTO dto = new ElectionResponseDTO();
        
        // Map fields
        BeanUtils.copyProperties(election, dto); 
        
        // Explicitly set isFrozen due to BeanUtils.copyProperties issues with Boolean fields starting with "is"
        dto.setIsFrozen(election.getIsFrozen());

        // Fetch associated states
//        List<ElectionState> states = electionStateRepository.findByElection(election);
//        dto.setStates(states.stream()
//                            .map(ElectionState::getState)
//                            .collect(Collectors.toList()));
        
        log.debug("Converted election entity to DTO for electionId: {}", election.getId());
        return dto;
    }


    public ResponseEntity<ThedalResponse<ElectionResponseDTO>> getElectionById(Long electionId) {
    	
    	log.info("Entering getElectionById method with electionId: {}", electionId);
    	ThedalResponse<ElectionResponseDTO> response = new ThedalResponse<>();

        // Read from PostgreSQL for GET operations instead of MongoDB
        Optional<ElectionEntity> optionalElection = electionRepository.findByIdAndAccountIdAndIsDeletedFalse(electionId, requestDetails.getCurrentAccountId());
        if (!optionalElection.isPresent()) {
        	log.warn("Election not found for electionId: {} and accountId: {}", electionId, requestDetails.getCurrentAccountId());
            response.setResponse(ThedalError.ELECTION_NOT_FOUND);
            return ResponseEntity.badRequest().body(response);
        }

        ElectionEntity election = optionalElection.get();
        ElectionResponseDTO electionResponseDTO = convertToElectionDTO(election);

        response.setResponse(ThedalSuccess.ELECTION_FETCHED, electionResponseDTO);
        log.info("Successfully fetched election details for electionId: {}", electionId);
        return ResponseEntity.ok(response);
    }
        
//    @Transactional
//    public ThedalResponse<Long> requestElectionDeleteOtp(Long electionId) {
//        log.info("Requesting OTP for election deletion: electionId={}", electionId);
//
//        Long accountId = requestDetails.getCurrentAccountId();
//        Long userId = requestDetails.getCurrentUserId();
//
//        // Validate election
//        ElectionEntity election = electionRepository.findByIdAndAccountIdAndIsDeletedFalse(electionId, accountId)
//                .orElseThrow(() -> new ThedalException(ThedalError.ELECTION_NOT_FOUND, HttpStatus.NOT_FOUND));
//
//        // Get user
//        UserEntity user = userRepo.findById(userId)
//                .orElseThrow(() -> new ThedalException(ThedalError.USER_NOT_FOUND, HttpStatus.NOT_FOUND));
//
//        if (!user.getIsMobileVerified()) {
//            throw new ThedalException(ThedalError.MOBILE_NOT_VERIFIED, HttpStatus.BAD_REQUEST);
//        }
//
//        // Generate OTP
//        String otp = RandomTokenGenerator.generateOTP(6);
//
//        // Save OTP
//        ElectionDeleteOtp deleteOtp = new ElectionDeleteOtp();
//        deleteOtp.setElectionId(electionId);
//        deleteOtp.setMobileNumber(user.getMobileNumber());
//        deleteOtp.setOtp(otp);
//        deleteOtp.setUser(user);
//        electionDeleteOtpRepository.save(deleteOtp);
//
//        // Send OTP
//        boolean smsSent = smsNotification.sendTransactionalOTP(user.getMobileNumber(), otp);
//        if (!smsSent) {
//            throw new ThedalException(ThedalError.OTP_SEND_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//
//        log.info("OTP sent for election deletion: electionId={}, mobileNumber={}", electionId, user.getMobileNumber());
//        return new ThedalResponse<>(ThedalSuccess.OTP_SENT, electionId);
//    }

    /**
	 * Deletes an election by their ID.
	 *
	 * @param electionId the ID of the election to be deleted.
	 * @return a ThedalResponse indicating the success of the deletion.
	 * @throws ThedalException if the election is not found or an error occurs during deletion.
	 */
//    @Transactional
//    public ThedalResponse<Void> deleteElectionById(Long electionId) {
//
//        try {
//            log.info("Deleting election with ID: {}", electionId);
//            ElectionEntity existingElection = electionRepository.findByIdAndAccountIdAndIsDeletedFalse(electionId, requestDetails.getCurrentAccountId())
//                .orElseThrow(() -> {
//                    log.warn("Election not found with ID: {}", electionId);
//                    return new ThedalException(ThedalError.ELECTION_DELETE_FAILED, HttpStatus.NOT_FOUND);
//                });
//
//                existingElection.setIsDeleted(true);
//                existingElection.setModifiedAt(new Date());
//
//            electionRepository.save(existingElection);
//           // notificationService.saveNotification(false, notificationTemplate.deleteElection(existingElection));
//            log.info("Election with ID: {} successfully deleted", electionId);
//            return new ThedalResponse<>(ThedalSuccess.ELECTION_DELETED);
//        } catch (Exception e) {
//            log.error("Error deleting election with ID: {}: {}", electionId, e.getMessage());
//            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
//        }
//    }

//    @Transactional
//    public ThedalResponse<Void> deleteElectionById(Long electionId) {
//    	
//    	try {
//            log.info("Attempting to delete election with ID: {}", electionId);
//
//            // Check if the election exists and is not already deleted
//            ElectionEntity existingElection = electionRepository.findByIdAndAccountIdAndIsDeletedFalse(electionId, requestDetails.getCurrentAccountId())
//                    .orElseThrow(() -> {
//                        log.warn("Election not found with ID: {}", electionId);
//                        return new ThedalException(ThedalError.ELECTION_NOT_FOUND, HttpStatus.NOT_FOUND);
//                    });
//
//            // If the election is already deleted, throw an error
//            if (existingElection.getIsDeleted()) {
//                log.warn("Election with ID: {} has already been deleted", electionId);
//                throw new ThedalException(ThedalError.ELECTION_ALREADY_DELETED, HttpStatus.BAD_REQUEST);
//            }
//            // Mark the election as deleted and set the modified date
//            existingElection.setIsDeleted(true);
//            existingElection.setModifiedAt(new Date());
//            
//            try {
//                ElectionEntity deletedElection = electionRepository.saveAndFlush(existingElection);
//                try {
//                    ElectionMongo electionMongo = new ElectionMongo(deletedElection);
//                    electionMongoRepository.save(electionMongo);
//                    log.info("Successfully updated election deletion status in MongoDB: id={}", deletedElection.getId());
//                } catch (Exception mongoEx) {
//                    log.error("Failed to update election deletion status in MongoDB: id={}", deletedElection.getId(), mongoEx);
//                    throw new RuntimeException("MongoDB update failed, triggering rollback", mongoEx);
//                }
//            } catch (Exception ex) {
//                log.error("Failed to delete election: id={}", electionId, ex);
//                throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
//            }
//
//            NotificationType notification = notificationTemplate.deleteElection(existingElection);
//            notificationService.saveNotification(true, notification); 
//
//            log.info("Election with ID: {} successfully marked as deleted", electionId);
//            return new ThedalResponse<>(ThedalSuccess.ELECTION_DELETED);
//
//        } catch (ThedalException te) {
//            log.error("Error deleting election with ID: {}: {}", electionId, te.getMessage());
//            throw te; 
//        } catch (Exception e) {
//            log.error("Unexpected error deleting election with ID: {}: {}", electionId, e.getMessage());
//            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
//        }
//    }

//    @Transactional
//    public ThedalResponse<Void> verifyElectionDeleteOtp(Long electionId, String otp) {
//        log.info("Verifying OTP for election deletion: electionId={}, otp={}", electionId, otp);
//
//        Long accountId = requestDetails.getCurrentAccountId();
//        Long userId = requestDetails.getCurrentUserId();
//
//        // Validate election
//        ElectionEntity election = electionRepository.findByIdAndAccountIdAndIsDeletedFalse(electionId, accountId)
//                .orElseThrow(() -> new ThedalException(ThedalError.ELECTION_NOT_FOUND, HttpStatus.NOT_FOUND));
//
//        // Get user
//        UserEntity user = userRepo.findById(userId)
//                .orElseThrow(() -> new ThedalException(ThedalError.USER_NOT_FOUND, HttpStatus.NOT_FOUND));
//
//        // Find and validate OTP
//        ElectionDeleteOtp deleteOtp = electionDeleteOtpRepository
//                .findByElectionIdAndMobileNumberAndOtpAndIsActiveTrue(electionId, user.getMobileNumber(), otp)
//                .orElseThrow(() -> new ThedalException(ThedalError.INVALID_OTP, HttpStatus.BAD_REQUEST));
//
//        if (LocalDateTime.now().isAfter(deleteOtp.getExpiresAt())) {
//            throw new ThedalException(ThedalError.OTP_EXPIRED, HttpStatus.BAD_REQUEST);
//        }
//
//        // Mark OTP as used
//        deleteOtp.setIsActive(false);
//        electionDeleteOtpRepository.save(deleteOtp);
//
//        // Proceed with deletion
//        election.setIsDeleted(true);
//        election.setModifiedAt(new Date());
//
//        try {
//            ElectionEntity deletedElection = electionRepository.saveAndFlush(election);
//            try {
//                ElectionMongo electionMongo = new ElectionMongo(deletedElection);
//                electionMongoRepository.save(electionMongo);
//                log.info("Successfully updated election deletion status in MongoDB: id={}", deletedElection.getId());
//            } catch (Exception mongoEx) {
//                log.error("Failed to update election deletion status in MongoDB: id={}", deletedElection.getId(), mongoEx);
//                throw new RuntimeException("MongoDB update failed, triggering rollback", mongoEx);
//            }
//
//            // Send notification
//            NotificationType notification = notificationTemplate.deleteElection(deletedElection);
//            notificationService.saveNotification(true, notification);
//
//            log.info("Election deleted successfully: electionId={}", electionId);
//            return new ThedalResponse<>(ThedalSuccess.ELECTION_DELETED);
//        } catch (Exception ex) {
//            log.error("Failed to delete election: id={}", electionId, ex);
//            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//    }

    @Transactional
    public ThedalResponse<?> deleteElectionById(Long electionId) {
        log.info("Attempting to delete election: electionId={}", electionId);

        Long accountId = requestDetails.getCurrentAccountId();
        Long userId = requestDetails.getCurrentUserId();
        if (accountId == null || userId == null) {
            log.error("Account ID or User ID not found");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        // Validate election
        ElectionEntity election = electionRepository.findByIdAndAccountIdAndIsDeletedFalse(electionId, accountId)
                .orElseThrow(() -> {
                    log.warn("Election not found: electionId={}", electionId);
                    return new ThedalException(ThedalError.ELECTION_NOT_FOUND, HttpStatus.NOT_FOUND);
                });

        // Check user
        UserEntity user = userRepo.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found: userId={}", userId);
                    return new ThedalException(ThedalError.USER_NOT_FOUND, HttpStatus.NOT_FOUND);
                });

        // Check OTP requirement
        if (Boolean.TRUE.equals(user.getIsOtpRequired())) {
            // Verify mobile
            if (!Boolean.TRUE.equals(user.getIsMobileVerified()) || user.getMobileNumber() == null) {
                log.warn("Mobile not verified for userId: {}", userId);
                throw new ThedalException(ThedalError.MOBILE_NOT_VERIFIED, HttpStatus.BAD_REQUEST);
            }

            // Generate and send OTP
            ElectionDeleteOtp otpEntity = otpService.generateOtp(userId, electionId, user.getMobileNumber());
            otpService.sendOtp(user.getMobileNumber(), otpEntity.getOtp());

            String message = String.format("OTP sent to mobile number for userId: %d", userId);
            log.info(message);
            return new ThedalResponse<OtpSentResponse>(ThedalSuccess.OTP_SENT, new OtpSentResponse(userId));
        }

        // Direct deletion
        return performDeletion(election);
    }
    
    @Transactional
    public ThedalResponse<Void> verifyElectionDeleteOtp(Long userId, String otp) {
        log.info("Verifying OTP for election deletion: userId={}", userId);

        // Check permissions
        requestDetails.checkUserRolePermission(RolePermission.SETTINGS_MANAGEMENT);

        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account ID not found");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        // Verify OTP and get electionId
        Long electionId = otpService.verifyOtp(userId, otp);
        if (electionId == null) {
            log.warn("No valid election found for OTP verification: userId={}", userId);
            throw new ThedalException(ThedalError.INVALID_OTP, HttpStatus.BAD_REQUEST);
        }

        // Validate election
        ElectionEntity election = electionRepository.findByIdAndAccountIdAndIsDeletedFalse(electionId, accountId)
                .orElseThrow(() -> {
                    log.warn("Election not found: electionId={}", electionId);
                    return new ThedalException(ThedalError.ELECTION_NOT_FOUND, HttpStatus.NOT_FOUND);
                });

        // Delete election
        return performDeletion(election);
    }

    private ThedalResponse<Void> performDeletion(ElectionEntity election) {
        try {
            election.setIsDeleted(true);
            election.setModifiedAt(new Date());
            ElectionEntity deletedElection = electionRepository.saveAndFlush(election);
            try {
                ElectionMongo electionMongo = new ElectionMongo(deletedElection);
                electionMongoRepository.save(electionMongo);
                log.info("Updated election deletion in MongoDB: electionId={}", deletedElection.getId());
            } catch (Exception e) {
                log.error("Failed to update MongoDB for electionId: {}", deletedElection.getId(), e);
                throw new RuntimeException("MongoDB update failed, triggering rollback", e);
            }

            notificationService.saveNotification(true, notificationTemplate.deleteElection(deletedElection));
            log.info("Election deleted: electionId={}", deletedElection.getId());
            return new ThedalResponse<>(ThedalSuccess.ELECTION_DELETED);
        } catch (Exception e) {
            log.error("Failed to delete election: electionId={}", election.getId(), e);
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    
    /**
 	 * Add Banner Image to Election.
 	 *
 	 * @param electionId the ID of the election to which banner image to be added.
 	 * @return a ThedalResponse indicating the success of image upload with the image url.
 	 * @throws ThedalException if the election is not found .
 	 */
	public ThedalResponse<String> addBannerImageToElection(Long electionId, MultipartFile multipartFile) {
		
		log.info("inside addBannerImageToElection method: election with ID: {}", electionId);
		electionRepository.findByIdAndAccountIdAndIsDeletedFalse(electionId, requestDetails.getCurrentAccountId()).orElseThrow(() -> {
			log.warn("Election not found with ID: {}", electionId);
			return new ThedalException(ThedalError.ELECTION_NOT_FOUND, HttpStatus.NOT_FOUND);
		});

		String contentType = multipartFile.getContentType();
		if (!(MediaType.APPLICATION_PDF_VALUE.equals(contentType) || MediaType.IMAGE_JPEG_VALUE.equals(contentType)
				|| MediaType.IMAGE_PNG_VALUE.equals(contentType) || MediaType.IMAGE_GIF_VALUE.equals(contentType)))
			throw new ThedalException(ThedalError.IMAGE_TYPE_NOT_SUPPORTED, HttpStatus.BAD_REQUEST);

		long maxFileSize = 2 * 1024 * 1024;
		if (multipartFile.getSize() > maxFileSize)
			throw new ThedalException(ThedalError.IMAGE_SIZE_LIMIT, HttpStatus.BAD_REQUEST);

		// File file=new File(multipartFile.getOriginalFilename());
		String fileExtension = awsFileUpload.getFileExtension(multipartFile.getOriginalFilename());
		String safeFileName = RandomTokenGenerator.generateToken(10) + System.currentTimeMillis();

		// Use a secure directory (application-specific temp folder, for instance)
		File secureTempDir = new File(System.getProperty("java.io.tmpdir"));
		if (!secureTempDir.exists()) {
			secureTempDir.mkdirs(); // Ensure directory exists
			secureTempDir.setWritable(true, true); // Restrict writable access to owner only
		}

		File tempFile;
		try {
			tempFile = File.createTempFile(safeFileName, "." + fileExtension, secureTempDir);
			// Ensure that the file is only readable and writable by the owner
			tempFile.setReadable(true, true);
			tempFile.setWritable(true, true);
		} catch (IOException e) {
			log.error("Error creating temp file for image upload", e);
			throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);

		}

		try (FileOutputStream fileOutputStream = new FileOutputStream(tempFile)) {
			fileOutputStream.write(multipartFile.getBytes());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw new ThedalException(ThedalError.FILE_NOT_FOUND, HttpStatus.BAD_REQUEST);
		} catch (IOException e) {
			log.error("File output stream :IO exception", e);
			e.printStackTrace();
			throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
		}

		// Name the file
		String fileName = "BANNER_" + RandomTokenGenerator.generateToken(10) + System.currentTimeMillis()
				+ multipartFile.getOriginalFilename();

		// Save the file
		String awsUrl = awsFileUpload.uploadToAWS(tempFile, fileName, s3bucket);

		// Delete the temperory file
		// file.delete();
		// Delete the temporary file after upload
		if (!tempFile.delete()) {
			log.warn("Temporary file deletion failed: {}", tempFile.getName());
		}

//		Files bannerImage = filesRepository
//				.save(new Files(HandlerType.BANNER_IMAGES, electionId, multipartFile.getOriginalFilename(), awsUrl));
//
//		log.info("end of addBannerImageToElection method : election with ID: {}", electionId);
//		return new ThedalResponse<>(ThedalSuccess.IMAGE_UPLOADED_SUCCESSFULLY, bannerImage.getUrl());
	
		 // Get the maximum orderIndex for the given electionId
		Integer maxOrderIndex = filesRepository.findMaxOrderIndexByElectionId(electionId);
		//Integer maxOrderIndex = filesRepository.findMaxOrderIndex();
	    int newOrderIndex = (maxOrderIndex == null) ? 0 : maxOrderIndex + 1;
  
	    Files bannerImage = new Files(HandlerType.BANNER_IMAGES, electionId, multipartFile.getOriginalFilename(), awsUrl);
	    bannerImage.setOrderIndex(newOrderIndex);  
	    bannerImage.setWhatsappForward(false);
	    bannerImage.setIsActive(true);

	    // Dual write implementation with error handling
	    try {
	        Files savedBanner = filesRepository.save(bannerImage);
	        
	        try {
	            FilesMongo bannerMongo = new FilesMongo(savedBanner);
	            filesMongoRepository.save(bannerMongo);
	            log.info("Successfully saved banner image to MongoDB: id={}, fileName={}", savedBanner.getId(), savedBanner.getFileName());
	        } catch (Exception mongoEx) {
	            log.error("Failed to save banner image to MongoDB: id={}, fileName={}. Rolling back transaction.", 
	                     savedBanner.getId(), savedBanner.getFileName(), mongoEx);
	            throw new RuntimeException("MongoDB save failed, triggering rollback", mongoEx);
	        }
	        
	        log.info("Banner image uploaded successfully with dual write: electionId={}, fileId={}", electionId, savedBanner.getId());
	        return new ThedalResponse<>(ThedalSuccess.IMAGE_UPLOADED_SUCCESSFULLY, savedBanner.getUrl());
	        
	    } catch (Exception ex) {
	        log.error("Failed to save banner image for electionId: {}", electionId, ex);
	        throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
	    }
	
	}

	
    /**
 	 * Get Banner images by electionId.
 	 *
 	 * @param electionId the ID of the election to which banner images to be fetched.
 	 * @return a ThedalResponse indicating the success of image retrieval along with the image urls with file details.
 	 * @throws ThedalException if the election is not found .
 	 */
	public ThedalResponse<List<Files>> getBannerImagesByElectionId(Long electionId) {
		
		log.info("inside getBannerImagesByElectionId method : election with ID: {}", electionId);
		electionRepository.findByIdAndAccountIdAndIsDeletedFalse(electionId, requestDetails.getCurrentAccountId()).orElseThrow(() -> {
			log.warn("Election not found with ID: {}", electionId);
			return new ThedalException(ThedalError.ELECTION_NOT_FOUND, HttpStatus.NOT_FOUND);
		});
		
		// Read banner images from PostgreSQL for optimized performance
		List<Files> files = filesRepository.findAllByHandlerTypeAndHandlerFileIdOrderByOrderIndexAsc(
		        HandlerType.BANNER_IMAGES, electionId);
		        
		log.info("Successfully fetched {} banner images from PostgreSQL for electionId: {}", files.size(), electionId);
		return new ThedalResponse<>(ThedalSuccess.IMAGE_FETCHED_SUCCESSFULLY, files);
	}

    @Transactional
public ThedalResponse<String> saveWhatsAppFooter(Long electionId, WhatsAppFooterRequest request) {
    // Check if the election exists
    Optional<ElectionEntity> electionOpt = electionRepository.findById(electionId);
    if (electionOpt.isPresent()) {
        ElectionEntity election = electionOpt.get();
        
        // Dual write implementation with error handling
        try {
            // Set WhatsApp Footer content
            election.setWhatsappFooter(request.getWhatsappFooter());
            ElectionEntity savedElection = electionRepository.save(election);
            
            try {
                // Update MongoDB as well
                Optional<ElectionMongo> electionMongo = electionMongoRepository.findById(electionId);
                if (electionMongo.isPresent()) {
                    ElectionMongo mongoDoc = electionMongo.get();
                    mongoDoc.setWhatsappFooter(request.getWhatsappFooter());
                    mongoDoc.setUpdatedAt(LocalDateTime.now());
                    electionMongoRepository.save(mongoDoc);
                    log.info("Successfully updated WhatsApp footer in MongoDB: electionId={}", electionId);
                } else {
                    // Create new MongoDB document if not exists
                    ElectionMongo newMongoDoc = new ElectionMongo(savedElection);
                    electionMongoRepository.save(newMongoDoc);
                    log.info("Created new MongoDB document for WhatsApp footer: electionId={}", electionId);
                }
            } catch (Exception mongoEx) {
                log.error("Failed to update WhatsApp footer in MongoDB: electionId={}", electionId, mongoEx);
                throw new RuntimeException("MongoDB update failed, triggering rollback", mongoEx);
            }
            
            log.info("WhatsApp footer updated successfully with dual write: electionId={}", electionId);
            return new ThedalResponse<String>(ThedalSuccess.WHATSAPP_FOOTER_UPDATED_SUCCESSFULLY);
            
        } catch (Exception ex) {
            log.error("Failed to update WhatsApp footer for electionId: {}", electionId, ex);
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    } else {
        throw new ThedalException(ThedalError.ELECTION_NOT_FOUND, HttpStatus.NOT_FOUND);
    }
}


    public ThedalResponse<String> getWhatsAppFooter(Long electionId) {
        log.info("Fetching WhatsApp Footer for electionId={}", electionId);
    
        // Read from PostgreSQL for GET operations
        ElectionEntity election = electionRepository.findByIdAndAccountIdAndIsDeletedFalse(
            electionId, requestDetails.getCurrentAccountId()
        ).orElseThrow(() -> {
            log.warn("Election not found with ID: {}", electionId);
            throw new ThedalException(ThedalError.ELECTION_NOT_FOUND, HttpStatus.NOT_FOUND);
        });
    
        String footer = election.getWhatsappFooter();
        return new ThedalResponse<>(ThedalSuccess.WHATSAPPFOOTER_GET_SUCCESSFULLY, footer);
    }
    
	
	@Transactional
	public ThedalResponse<String> updateWhatsappForwardFlag(Long electionId, Long fileId, boolean forwardStatus) {
	    // Validate election existence
	    electionRepository.findByIdAndAccountIdAndIsDeletedFalse(electionId, requestDetails.getCurrentAccountId())
	        .orElseThrow(() -> new ThedalException(ThedalError.ELECTION_NOT_FOUND, HttpStatus.NOT_FOUND));

	    // Get the file to update
	    Files fileToUpdate = filesRepository.findById(fileId)
	        .orElseThrow(() -> new ThedalException(ThedalError.FILE_NOT_FOUND, HttpStatus.NOT_FOUND));

	    // Check it's a banner for the correct election
	    if (!fileToUpdate.getHandlerType().equals(HandlerType.BANNER_IMAGES) || !fileToUpdate.getHandlerFileId().equals(electionId)) {
	        throw new ThedalException(ThedalError.INVALID_BANNER_FILE, HttpStatus.BAD_REQUEST);
	    }

	    // If forwardStatus = true, set all other banners to false
	    if (forwardStatus) {
	        List<Files> banners = filesRepository.findAllByHandlerTypeAndHandlerFileId(HandlerType.BANNER_IMAGES, electionId);
	        for (Files banner : banners) {
	            if (!banner.getId().equals(fileId) && Boolean.TRUE.equals(banner.getWhatsappForward())) {
	                banner.setWhatsappForward(false);
	                filesRepository.save(banner);
	                
	                // Update MongoDB as well
	                try {
	                    Optional<FilesMongo> bannerMongo = filesMongoRepository.findById(banner.getId());
	                    if (bannerMongo.isPresent()) {
	                        FilesMongo mongoDoc = bannerMongo.get();
	                        mongoDoc.setWhatsappForward(false);
	                        mongoDoc.setUpdatedAt(LocalDateTime.now());
	                        filesMongoRepository.save(mongoDoc);
	                    }
	                } catch (Exception mongoEx) {
	                    log.error("Failed to update banner WhatsApp flag in MongoDB for fileId: {}", banner.getId(), mongoEx);
	                    throw new RuntimeException("MongoDB update failed, triggering rollback", mongoEx);
	                }
	            }
	        }
	    }

	    // Update selected file with dual write
	    try {
	        fileToUpdate.setWhatsappForward(forwardStatus);
	        Files savedFile = filesRepository.save(fileToUpdate);
	        
	        try {
	            Optional<FilesMongo> fileMongo = filesMongoRepository.findById(fileId);
	            if (fileMongo.isPresent()) {
	                FilesMongo mongoDoc = fileMongo.get();
	                mongoDoc.setWhatsappForward(forwardStatus);
	                mongoDoc.setUpdatedAt(LocalDateTime.now());
	                filesMongoRepository.save(mongoDoc);
	                log.info("Successfully updated WhatsApp forward flag in MongoDB: fileId={}, status={}", fileId, forwardStatus);
	            } else {
	                // Create new MongoDB document if not exists
	                FilesMongo newMongoDoc = new FilesMongo(savedFile);
	                filesMongoRepository.save(newMongoDoc);
	                log.info("Created new MongoDB document for WhatsApp forward flag: fileId={}, status={}", fileId, forwardStatus);
	            }
	        } catch (Exception mongoEx) {
	            log.error("Failed to update WhatsApp forward flag in MongoDB for fileId: {}", fileId, mongoEx);
	            throw new RuntimeException("MongoDB update failed, triggering rollback", mongoEx);
	        }
	        
	        log.info("WhatsApp forward flag updated successfully with dual write: fileId={}, status={}", fileId, forwardStatus);
	        return new ThedalResponse<>(ThedalSuccess.FLAG_UPDATED_SUCCESSFULLY);
	        
	    } catch (Exception ex) {
	        log.error("Failed to update WhatsApp forward flag for fileId: {}", fileId, ex);
	        throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
	    }
	}


    /**
 	 * Delete Banner images by fileId.
 	 *
 	 * @param fileId the ID of the banner images to be deleted.
 	 * @return a ThedalResponse indicating the success of banner image deletion.
 	 * @throws ThedalException if the file is not found or an error occurs during deletion.
 	 */
//	@Transactional(rollbackFor = {Exception.class})
//	public ThedalResponse<Void> deleteBannerImage(Long fileId) {
//		
//		log.info("inside deleteBannerImage method : file with ID: {}", fileId);
//		Files files = filesRepository.findById(fileId).orElseThrow(()-> new ThedalException(ThedalError.FILE_NOT_FOUND, HttpStatus.NOT_FOUND));
//		String objectKey = AwsFileUpload.getKeyFromUrl(files.getUrl());
//		awsFileUpload.deleteS3Object(s3bucket, objectKey);
//		filesRepository.delete(files);
//		
//		log.info("end of deleteBannerImage method : file with ID: {}", fileId);
//		return new ThedalResponse<>(ThedalSuccess.IMAGE_DELETED_SUCCESSFULLY);
//	}
	@Transactional(rollbackFor = {Exception.class})
	public ThedalResponse<Void> deleteBannerImage(Long electionId, Long fileId) {
	    Long accountId = requestDetails.getCurrentAccountId();
	    log.info("Deleting banner image: fileId={}, electionId={}, accountId={}", fileId, electionId, accountId);

	    // Validate that election exists and belongs to the current account
	    electionRepository.findByIdAndAccountIdAndIsDeletedFalse(electionId, accountId)
	        .orElseThrow(() -> {
	            log.warn("Election not found or unauthorized access: electionId={}, accountId={}", electionId, accountId);
	            return new ThedalException(ThedalError.ELECTION_NOT_FOUND, HttpStatus.NOT_FOUND);
	        });

	    // Find the file with matching fileId, handler type, and handlerFileId (i.e., electionId)
	    Files file = filesRepository.findByIdAndHandlerTypeAndHandlerFileId(
	            fileId, HandlerType.BANNER_IMAGES, electionId)
	        .orElseThrow(() -> {
	            log.warn("Banner image not found for fileId={} and electionId={}", fileId, electionId);
	            return new ThedalException(ThedalError.FILE_NOT_FOUND, HttpStatus.NOT_FOUND);
	        });

	    // Delete from S3 and DB
	    String objectKey = AwsFileUpload.getKeyFromUrl(file.getUrl());
	    awsFileUpload.deleteS3Object(s3bucket, objectKey);
	    filesRepository.delete(file);

	    log.info("Banner image deleted: fileId={}, electionId={}", fileId, electionId);
	    return new ThedalResponse<>(ThedalSuccess.IMAGE_DELETED_SUCCESSFULLY);
	}

	@Transactional(rollbackFor = {Exception.class})
	public ThedalResponse<Void> deleteBannerImages(Long electionId, List<Long> fileIds) {
	    Long accountId = requestDetails.getCurrentAccountId();
	    log.info("Deleting banner images: electionId={}, fileIds={}, accountId={}", electionId, fileIds, accountId);

	    // Validate that election exists and belongs to the current account
	    electionRepository.findByIdAndAccountIdAndIsDeletedFalse(electionId, accountId)
	        .orElseThrow(() -> {
	            log.warn("Election not found or unauthorized access: electionId={}, accountId={}", electionId, accountId);
	            return new ThedalException(ThedalError.ELECTION_NOT_FOUND, HttpStatus.NOT_FOUND);
	        });

	    List<Files> filesToDelete;

	    if (fileIds == null || fileIds.isEmpty()) {
	        // Delete all banner images for this election
	        filesToDelete = filesRepository.findAllByHandlerTypeAndHandlerFileId(HandlerType.BANNER_IMAGES, electionId);
	    } else {
	        // Delete only the specified fileIds
	        filesToDelete = filesRepository.findAllByIdInAndHandlerTypeAndHandlerFileId(fileIds, HandlerType.BANNER_IMAGES, electionId);
	        if (filesToDelete.size() != fileIds.size()) {
	            log.warn("Some fileIds not found or not matching the electionId");
	            throw new ThedalException(ThedalError.FILE_NOT_FOUND, HttpStatus.NOT_FOUND);
	        }
	    }

	    for (Files file : filesToDelete) {
	        String objectKey = AwsFileUpload.getKeyFromUrl(file.getUrl());
	        awsFileUpload.deleteS3Object(s3bucket, objectKey);
	    }

	    // Dual write implementation with error handling
	    try {
	        filesRepository.deleteAll(filesToDelete);
	        filesRepository.flush();
	        
	        try {
	            if (fileIds == null || fileIds.isEmpty()) {
	                filesMongoRepository.deleteByHandlerTypeAndHandlerFileId(HandlerType.BANNER_IMAGES, electionId);
	                log.info("Deleted all banner images from MongoDB for electionId: {}", electionId);
	            } else {
	                filesMongoRepository.deleteByIdIn(fileIds);
	                log.info("Deleted banner images from MongoDB: fileIds={}", fileIds);
	            }
	        } catch (Exception mongoEx) {
	            log.error("Failed to delete banner images from MongoDB: fileIds={}", fileIds, mongoEx);
	            throw new RuntimeException("MongoDB deletion failed, triggering rollback", mongoEx);
	        }
	        
	        log.info("Banner images deleted successfully with dual write: electionId={}, count={}", electionId, filesToDelete.size());
	    } catch (Exception ex) {
	        log.error("Failed to delete banner images for electionId: {}", electionId, ex);
	        throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
	    }

	    return new ThedalResponse<>(ThedalSuccess.IMAGE_DELETED_SUCCESSFULLY);
	}
	
	public void updateBoothSlipTemplates(Long electionId, List<Long> templateIds, Long accountId) {
		
		ElectionEntity election = electionRepository.findByIdAndAccountId(electionId, accountId)
	            .orElseThrow(() -> new ThedalException(ThedalError.ELECTION_NOT_FOUND, HttpStatus.NOT_FOUND));

	    // Convert the list of template IDs to a JSON array
	    ObjectMapper objectMapper = new ObjectMapper();
	    ArrayNode templatesJson = objectMapper.createArrayNode();
	    
	    // Add each template ID to the ArrayNode
	    for (Long templateId : templateIds) {
	        templatesJson.add(templateId); // Add individual templateId as a JSON number
	    }
	    // Set the templates field as a string representation of the JSON array
	    election.setTemplates(templatesJson.toString()); // Convert to string for storage in the database

	    electionRepository.save(election);
	}
	

//	@Transactional
//	public void reorderElections(List<ElectionReorderDTO> reorderedElections) {
//	    // Validate input
//	    if (reorderedElections == null || reorderedElections.isEmpty()) {
//	        throw new ThedalException(ThedalError.ORDER_INDEX_NOT_SET, HttpStatus.BAD_REQUEST);
//	    }
//
//	    List<Long> electionIds = reorderedElections.stream()
//	        .map(ElectionReorderDTO::getId)
//	        .collect(Collectors.toList());
//
//	    // Fetch elections that need updating
//	    List<ElectionEntity> elections = electionRepository.findAllById(electionIds);
//	    if (elections.size() != reorderedElections.size()) {
//	        throw new ThedalException(ThedalError.ELECTION_ID_NOT_FOUND, HttpStatus.BAD_REQUEST);
//	    }
//
//	    // Update order index
//	    for (ElectionEntity election : elections) {
//	        reorderedElections.stream()
//	            .filter(dto -> dto.getId().equals(election.getId()))
//	            .findFirst()
//	            .ifPresent(dto -> election.setOrderIndex(dto.getOrderIndex()));
//	    }
//
//	    electionRepository.saveAll(elections);  // Batch update
//	}
	@Transactional
	public void updateMultipleElectionOrder(List<ElectionReorderRequest> requests, Long accountId) {
	    if (requests.isEmpty()) {
	        log.warn("No reorder requests received.");
	        throw new ThedalException(ThedalError.INVALID_REQUEST, HttpStatus.BAD_REQUEST);
	    }

	    // Get all active elections for the account
	    List<ElectionEntity> elections = electionRepository.findAllActiveElections(accountId);
	    elections.sort(Comparator.comparing(ElectionEntity::getOrderIndex)); // Ensure it's sorted before updating

	    // Iterate over each reorder request
	    for (ElectionReorderRequest request : requests) {
	        // Find the election to be moved
	        ElectionEntity movedElection = elections.stream()
	                .filter(e -> e.getId().equals(request.getElectionId()))
	                .findFirst()
	                .orElseThrow(() -> new ThedalException(ThedalError.ELECTION_NOT_FOUND, HttpStatus.NOT_FOUND));

	        // Remove the moved election
	        elections.remove(movedElection);

	        // Ensure new index is within bounds
	        int newIndex = Math.max(0, Math.min(request.getNewIndex(), elections.size()));

	        // Insert at the new position
	        elections.add(newIndex, movedElection);
	    }

	    // Reassign orderIndex values
	    for (int i = 0; i < elections.size(); i++) {
	        elections.get(i).setOrderIndex(i);
	    }

	    // Save updated order
	    electionRepository.saveAll(elections);
	    log.info("Election order updated successfully for multiple elections");
	}

	@Transactional
	public void updateFileOrder(List<FileReorderRequest> reorderRequests, Long accountId, Long electionId) {
	    List<Files> files = filesRepository.findByHandlerFileIdAndHandlerTypeOrderByOrderIndex(electionId, HandlerType.BANNER_IMAGES);

	    if (files.isEmpty()) {
	        log.error("No files found for election ID {} and account ID {}", electionId, accountId);
	        throw new ThedalException(ThedalError.FILE_NOT_FOUND, HttpStatus.NOT_FOUND);
	    }

	    // Sort files by current orderIndex before modifying
	    files.sort(Comparator.comparing(Files::getOrderIndex));

	    // Create a map of fileId -> newOrderIndex
	    Map<Long, Integer> newOrderMap = reorderRequests.stream()
	            .collect(Collectors.toMap(FileReorderRequest::getFileId, FileReorderRequest::getNewOrderIndex));

	    // Remove files that are being moved
	    List<Files> reorderedFiles = new ArrayList<>(files);
	    reorderedFiles.removeIf(f -> newOrderMap.containsKey(f.getId()));

	    // Insert files at their new positions
	    for (FileReorderRequest request : reorderRequests) {
	        Files file = files.stream()
	                .filter(f -> f.getId().equals(request.getFileId()))
	                .findFirst()
	                .orElseThrow(() -> new ThedalException(ThedalError.FILE_NOT_FOUND, HttpStatus.NOT_FOUND));

	        reorderedFiles.add(request.getNewOrderIndex(), file);
	    }

	    // Update orderIndex for all files
	    for (int i = 0; i < reorderedFiles.size(); i++) {
	        reorderedFiles.get(i).setOrderIndex(i);
	    }

	    // Save the updated order with dual write
	    try {
	        List<Files> savedFiles = filesRepository.saveAll(reorderedFiles);
	        
	        // Update MongoDB as well
	        try {
	            List<FilesMongo> mongoFiles = savedFiles.stream()
	                    .map(file -> {
	                        Optional<FilesMongo> existingMongo = filesMongoRepository.findById(file.getId());
	                        FilesMongo mongoDoc = existingMongo.orElse(new FilesMongo(file));
	                        mongoDoc.setOrderIndex(file.getOrderIndex());
	                        mongoDoc.setUpdatedAt(LocalDateTime.now());
	                        return mongoDoc;
	                    })
	                    .collect(Collectors.toList());
	            
	            filesMongoRepository.saveAll(mongoFiles);
	            log.info("Successfully updated file order in MongoDB for electionId: {}", electionId);
	        } catch (Exception mongoEx) {
	            log.error("Failed to update file order in MongoDB for electionId: {}", electionId, mongoEx);
	            throw new RuntimeException("MongoDB reorder failed, triggering rollback", mongoEx);
	        }
	        
	        log.info("File order updated successfully with dual write for electionId: {}", electionId);
	    } catch (Exception ex) {
	        log.error("Failed to update file order for electionId: {}", electionId, ex);
	        throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
	    }
	}
	
	@Transactional
	public ThedalResponse<String> updateBannerActiveStatus(Long electionId, Long fileId, Boolean isActive) {
	    log.info("Updating banner active status: electionId={}, fileId={}, isActive={}", electionId, fileId, isActive);
	    
	    // Validate election existence
	    electionRepository.findByIdAndAccountIdAndIsDeletedFalse(electionId, requestDetails.getCurrentAccountId())
	        .orElseThrow(() -> {
	            log.warn("Election not found with ID: {}", electionId);
	            return new ThedalException(ThedalError.ELECTION_NOT_FOUND, HttpStatus.NOT_FOUND);
	        });

	    // Get the file to update
	    Files fileToUpdate = filesRepository.findById(fileId)
	        .orElseThrow(() -> {
	            log.warn("File not found with ID: {}", fileId);
	            return new ThedalException(ThedalError.FILE_NOT_FOUND, HttpStatus.NOT_FOUND);
	        });

	    // Check if it's a banner for the correct election
	    if (!fileToUpdate.getHandlerType().equals(HandlerType.BANNER_IMAGES) || !fileToUpdate.getHandlerFileId().equals(electionId)) {
	        log.warn("Invalid banner file: fileId={} does not belong to electionId={}", fileId, electionId);
	        throw new ThedalException(ThedalError.INVALID_BANNER_FILE, HttpStatus.BAD_REQUEST);
	    }

	    // Update isActive status with dual write
	    try {
	        fileToUpdate.setIsActive(isActive);
	        Files savedFile = filesRepository.save(fileToUpdate);
	        
	        try {
	            Optional<FilesMongo> fileMongo = filesMongoRepository.findById(fileId);
	            if (fileMongo.isPresent()) {
	                FilesMongo mongoDoc = fileMongo.get();
	                mongoDoc.setIsActive(isActive);
	                mongoDoc.setUpdatedAt(LocalDateTime.now());
	                filesMongoRepository.save(mongoDoc);
	                log.info("Successfully updated banner active status in MongoDB: fileId={}, isActive={}", fileId, isActive);
	            } else {
	                // Create new MongoDB document if not exists
	                FilesMongo newMongoDoc = new FilesMongo(savedFile);
	                filesMongoRepository.save(newMongoDoc);
	                log.info("Created new MongoDB document for banner active status: fileId={}, isActive={}", fileId, isActive);
	            }
	        } catch (Exception mongoEx) {
	            log.error("Failed to update banner active status in MongoDB for fileId: {}", fileId, mongoEx);
	            throw new RuntimeException("MongoDB update failed, triggering rollback", mongoEx);
	        }
	        
	        log.info("Banner active status updated successfully with dual write: fileId={}, isActive={}", fileId, isActive);
	        return new ThedalResponse<>(ThedalSuccess.BANNER_ACTIVE_STATUS_UPDATED_SUCCESSFULLY);
	        
	    } catch (Exception ex) {
	        log.error("Failed to update banner active status for fileId: {}", fileId, ex);
	        throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
	    }
	}


    /**
     * Convert ElectionMongo to ElectionResponseDTO
     */
    private ElectionResponseDTO convertToElectionDTOFromMongo(ElectionMongo election) {
        log.debug("Converting election MongoDB document to DTO for electionId: {}", election.getId());
        ElectionResponseDTO dto = new ElectionResponseDTO();
        
        // Map fields from MongoDB document
        dto.setId(election.getId());
        dto.setElectionName(election.getElectionName());
        dto.setElectionType(election.getElectionType());
        dto.setStartDate(election.getStartDate());
        dto.setEndDate(election.getEndDate());
        dto.setImageUrl(election.getImageUrl());
        dto.setCategory(election.getCategory());
        dto.setStateName(election.getStateName());
        dto.setYear(election.getYear());
        dto.setMonth(election.getMonth());
        dto.setStatus(election.getStatus());
        dto.setIsFrozen(election.getIsFrozen());
        dto.setNumberOfPollingStations(election.getNumberOfPollingStations());
        dto.setNumberOfPhases(election.getNumberOfPhases());
        dto.setNumberOfPinkBooths(election.getNumberOfPinkBooths());
        dto.setNumberOfVoters(election.getNumberOfVoters());
        dto.setNumberOfMaleVoters(election.getNumberOfMaleVoters());
        dto.setNumberOfFemaleVoters(election.getNumberOfFemaleVoters());
        dto.setNumberOfTransgenderVoters(election.getNumberOfTransgenderVoters());
        dto.setRemarks(election.getRemarks());
        dto.setBoothCount(election.getBoothCount());
        dto.setNotificationDate(election.getNotificationDate());
        dto.setLastDateForFillingNomination(election.getLastDateForFillingNomination());
        dto.setDateOfPoll(election.getDateOfPoll());
        dto.setScrutinyNominationDate(election.getScrutinyNominationDate());
        dto.setLastDateForWithdrawalOfNomination(election.getLastDateForWithdrawalOfNomination());
        dto.setDateOfCountingOfVotes(election.getDateOfCountingOfVotes());
        dto.setElectionDescription(election.getElectionDescription());
        dto.setElectionCategory(election.getElectionCategory());
        dto.setType(election.getType());
        dto.setPcName(election.getPcName());
        dto.setAcName(election.getAcName());
        dto.setUrbanName(election.getUrbanName());
        dto.setRuralName(election.getRuralName());
        dto.setPhaseNo(election.getPhaseNo());
        dto.setCountry(election.getCountry());
        dto.setState(election.getState());
        dto.setGazetteNotificationDate(election.getGazetteNotificationDate());
        dto.setCompletionDeadlineDate(election.getCompletionDeadlineDate());
        dto.setElectoralReleaseDate(election.getElectoralReleaseDate());
        dto.setBodyString(election.getBodyString());
        dto.setTemplates(election.getTemplates());
        dto.setOrderIndex(election.getOrderIndex());
        
        // Convert timestamps
        if (election.getCreatedAt() != null) {
            dto.setCreatedAt(java.sql.Timestamp.valueOf(election.getCreatedAt()));
        }
        if (election.getUpdatedAt() != null) {
            dto.setModifiedAt(java.sql.Timestamp.valueOf(election.getUpdatedAt()));
        }
        
        log.debug("Converted election MongoDB document to DTO for electionId: {}", election.getId());
        return dto;
    }
    
    /**
     * Convert FilesMongo to Files entity for response compatibility
     */
    private Files convertToFilesFromMongo(FilesMongo fileMongo) {
        Files file = new Files();
        file.setId(fileMongo.getId());
        file.setHandlerType(fileMongo.getHandlerType());
        file.setHandlerFileId(fileMongo.getHandlerFileId());
        file.setFileName(fileMongo.getFileName());
        file.setUrl(fileMongo.getUrl());
        file.setOrderIndex(fileMongo.getOrderIndex());
        file.setWhatsappForward(fileMongo.getWhatsappForward());
        file.setIsActive(fileMongo.getIsActive());
        return file;
    }

    /**
     * Migrate all elections from PostgreSQL to MongoDB
     */
    @Transactional
    public ThedalResponse<String> migrateElectionsToMongoDB(Long accountId) {
        log.info("Starting migration of elections from PostgreSQL to MongoDB for accountId: {}", accountId);
        
        try {
            // Fetch all elections from PostgreSQL
            List<ElectionEntity> elections = electionRepository.findByAccountId(accountId);
            
            if (elections.isEmpty()) {
                log.warn("No elections found for accountId: {}", accountId);
                return new ThedalResponse<>(ThedalSuccess.DATA_MIGRATION_COMPLETED, "No elections found to migrate");
            }
            
            int totalElections = elections.size();
            int migratedElections = 0;
            int skippedElections = 0;
            
            log.info("Found {} elections to migrate for accountId: {}", totalElections, accountId);
            
            for (ElectionEntity election : elections) {
                try {
                    // Check if election already exists in MongoDB
                    Optional<ElectionMongo> existingElection = electionMongoRepository.findById(election.getId());
                    
                    if (existingElection.isPresent()) {
                        log.debug("Election {} already exists in MongoDB, updating...", election.getId());
                        // Update existing election
                        ElectionMongo electionMongo = new ElectionMongo(election);
                        electionMongoRepository.save(electionMongo);
                        migratedElections++;
                    } else {
                        log.debug("Migrating new election {} to MongoDB", election.getId());
                        // Create new election in MongoDB
                        ElectionMongo electionMongo = new ElectionMongo(election);
                        electionMongoRepository.save(electionMongo);
                        migratedElections++;
                    }
                    
                    // Migrate associated files/banners
                    List<Files> files = filesRepository.findAllByHandlerTypeAndHandlerFileId(
                            HandlerType.BANNER_IMAGES, election.getId());
                    
                    if (!files.isEmpty()) {
                        log.debug("Migrating {} files for election {}", files.size(), election.getId());
                        for (Files file : files) {
                            try {
                                // Check if file already exists in MongoDB
                                Optional<FilesMongo> existingFile = filesMongoRepository.findById(file.getId());
                                
                                if (existingFile.isPresent()) {
                                    // Update existing file
                                    FilesMongo fileMongo = new FilesMongo(file);
                                    filesMongoRepository.save(fileMongo);
                                } else {
                                    // Create new file in MongoDB
                                    FilesMongo fileMongo = new FilesMongo(file);
                                    filesMongoRepository.save(fileMongo);
                                }
                            } catch (Exception fileEx) {
                                log.error("Failed to migrate file {} for election {}: {}", 
                                        file.getId(), election.getId(), fileEx.getMessage());
                                // Continue with other files
                            }
                        }
                    }
                    
                } catch (Exception electionEx) {
                    log.error("Failed to migrate election {}: {}", election.getId(), electionEx.getMessage());
                    skippedElections++;
                    // Continue with other elections
                }
            }
            
            String resultMessage = String.format(
                    "Migration completed: %d elections migrated, %d skipped out of %d total elections",
                    migratedElections, skippedElections, totalElections);
            
            log.info("Election migration completed for accountId: {} - {}", accountId, resultMessage);
            
            return new ThedalResponse<>(ThedalSuccess.DATA_MIGRATION_COMPLETED, resultMessage);
            
        } catch (Exception ex) {
            log.error("Failed to migrate elections for accountId: {}", accountId, ex);
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to migrate elections: " + ex.getMessage());
        }
    }

    /**
     * Migrate all elections from PostgreSQL to MongoDB (Admin endpoint - migrates all accounts)
     */
    @Transactional
    public ThedalResponse<String> migrateAllElectionsToMongoDB() {
        log.info("Starting migration of ALL elections from PostgreSQL to MongoDB");
        
        try {
            // Fetch all elections from PostgreSQL
            List<ElectionEntity> elections = electionRepository.findAll();
            
            if (elections.isEmpty()) {
                log.warn("No elections found in PostgreSQL");
                return new ThedalResponse<>(ThedalSuccess.DATA_MIGRATION_COMPLETED, "No elections found to migrate");
            }
            
            int totalElections = elections.size();
            int migratedElections = 0;
            int skippedElections = 0;
            
            log.info("Found {} elections to migrate across all accounts", totalElections);
            
            for (ElectionEntity election : elections) {
                try {
                    // Check if election already exists in MongoDB
                    Optional<ElectionMongo> existingElection = electionMongoRepository.findById(election.getId());
                    
                    if (existingElection.isPresent()) {
                        log.debug("Election {} already exists in MongoDB, updating...", election.getId());
                        // Update existing election
                        ElectionMongo electionMongo = new ElectionMongo(election);
                        electionMongoRepository.save(electionMongo);
                        migratedElections++;
                    } else {
                        log.debug("Migrating new election {} to MongoDB", election.getId());
                        // Create new election in MongoDB
                        ElectionMongo electionMongo = new ElectionMongo(election);
                        electionMongoRepository.save(electionMongo);
                        migratedElections++;
                    }
                    
                    // Migrate associated files/banners
                    List<Files> files = filesRepository.findAllByHandlerTypeAndHandlerFileId(
                            HandlerType.BANNER_IMAGES, election.getId());
                    
                    if (!files.isEmpty()) {
                        log.debug("Migrating {} files for election {}", files.size(), election.getId());
                        for (Files file : files) {
                            try {
                                // Check if file already exists in MongoDB
                                Optional<FilesMongo> existingFile = filesMongoRepository.findById(file.getId());
                                
                                if (existingFile.isPresent()) {
                                    // Update existing file
                                    FilesMongo fileMongo = new FilesMongo(file);
                                    filesMongoRepository.save(fileMongo);
                                } else {
                                    // Create new file in MongoDB
                                    FilesMongo fileMongo = new FilesMongo(file);
                                    filesMongoRepository.save(fileMongo);
                                }
                            } catch (Exception fileEx) {
                                log.error("Failed to migrate file {} for election {}: {}", 
                                        file.getId(), election.getId(), fileEx.getMessage());
                                // Continue with other files
                            }
                        }
                    }
                    
                } catch (Exception electionEx) {
                    log.error("Failed to migrate election {}: {}", election.getId(), electionEx.getMessage());
                    skippedElections++;
                    // Continue with other elections
                }
            }
            
            String resultMessage = String.format(
                    "Global migration completed: %d elections migrated, %d skipped out of %d total elections",
                    migratedElections, skippedElections, totalElections);
            
            log.info("Global election migration completed - {}", resultMessage);
            
            return new ThedalResponse<>(ThedalSuccess.DATA_MIGRATION_COMPLETED, resultMessage);
            
        } catch (Exception ex) {
            log.error("Failed to migrate all elections", ex);
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to migrate elections: " + ex.getMessage());
        }
    }
    
    /**
     * Set default party for an election
     * 
     * @param electionId the ID of the election
     * @param partyId the ID of the party to set as default
     * @return ThedalResponse with success message
     */
    @Transactional
    public ThedalResponse<String> setDefaultParty(Long electionId, Long partyId) {
        log.info("Setting default party for electionId: {}, partyId: {}", electionId, partyId);
        
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account ID not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }
        
        // Validate election exists and belongs to account
        ElectionEntity election = electionRepository.findByIdAndAccountIdAndIsDeletedFalse(electionId, accountId)
                .orElseThrow(() -> {
                    log.error("Election not found: electionId={}, accountId={}", electionId, accountId);
                    return new ThedalException(ThedalError.ELECTION_NOT_FOUND, HttpStatus.NOT_FOUND);
                });
        
        // Validate party exists and belongs to the same election and account
        com.thedal.thedal_app.settings.electionsettings.PartyMongo party = partyMongoRepository.findById(partyId)
                .filter(p -> p.getAccountId().equals(accountId) && p.getElectionId().equals(electionId))
                .orElseThrow(() -> {
                    log.error("Party not found or doesn't belong to election: partyId={}, electionId={}, accountId={}", 
                            partyId, electionId, accountId);
                    return new ThedalException(ThedalError.PARTY_NOT_FOUND, HttpStatus.NOT_FOUND);
                });
        
        // Update election with default party
        election.setDefaultPartyId(partyId);
        election.setModifiedAt(new Date());
        
        try {
            ElectionEntity updatedElection = electionRepository.saveAndFlush(election);
            
            // Sync to MongoDB
            try {
                ElectionMongo electionMongo = new ElectionMongo(updatedElection);
                electionMongoRepository.save(electionMongo);
                log.info("Successfully updated default party in MongoDB: electionId={}, partyId={}", electionId, partyId);
            } catch (Exception mongoEx) {
                log.error("Failed to update election in MongoDB: electionId={}, partyId={}", electionId, partyId, mongoEx);
                throw new RuntimeException("MongoDB update failed, triggering rollback", mongoEx);
            }
            
            log.info("Default party set successfully: electionId={}, partyId={}, partyName={}", 
                    electionId, partyId, party.getPartyName());
            return new ThedalResponse<>(ThedalSuccess.PARTY_UPDATED, 
                    "Default party set successfully: " + party.getPartyName());
                    
        } catch (Exception ex) {
            log.error("Failed to set default party: electionId={}, partyId={}", electionId, partyId, ex);
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * Get default party for an election
     * 
     * @param electionId the ID of the election
     * @return ThedalResponse with default party details
     */
    @Transactional(readOnly = true)
    public ThedalResponse<com.thedal.thedal_app.election.dtos.DefaultPartyResponse> getDefaultParty(Long electionId) {
        log.info("Getting default party for electionId: {}", electionId);
        
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account ID not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }
        
        // Validate election exists and belongs to account
        ElectionEntity election = electionRepository.findByIdAndAccountIdAndIsDeletedFalse(electionId, accountId)
                .orElseThrow(() -> {
                    log.error("Election not found: electionId={}, accountId={}", electionId, accountId);
                    return new ThedalException(ThedalError.ELECTION_NOT_FOUND, HttpStatus.NOT_FOUND);
                });
        
        com.thedal.thedal_app.election.dtos.DefaultPartyResponse response = 
                new com.thedal.thedal_app.election.dtos.DefaultPartyResponse();
        response.setElectionId(electionId);
        
        // Check if default party is set
        if (election.getDefaultPartyId() != null) {
            // Fetch party details from MongoDB
            Optional<com.thedal.thedal_app.settings.electionsettings.PartyMongo> partyOpt = 
                    partyMongoRepository.findById(election.getDefaultPartyId());
            
            if (partyOpt.isPresent()) {
                com.thedal.thedal_app.settings.electionsettings.PartyMongo party = partyOpt.get();
                response.setDefaultPartyId(party.getId());
                response.setPartyName(party.getPartyName());
                response.setPartyShortName(party.getPartyShortName());
                response.setPartyImage(party.getPartyImage());
                response.setPartyColor(party.getPartyColor());
                response.setMessage("Default party found");
                
                log.info("Default party retrieved: electionId={}, partyId={}, partyName={}", 
                        electionId, party.getId(), party.getPartyName());
            } else {
                // Default party ID exists but party not found (data inconsistency)
                log.warn("Default party ID exists but party not found: electionId={}, partyId={}", 
                        electionId, election.getDefaultPartyId());
                response.setDefaultPartyId(null);
                response.setMessage("No default party set (data inconsistency detected)");
            }
        } else {
            log.info("No default party set for electionId: {}", electionId);
            response.setDefaultPartyId(null);
            response.setMessage("No default party set for this election");
        }
        
        return new ThedalResponse<>(ThedalSuccess.PARTY_FETCHED, response);
    }
    
    // ==================== FREEZE/UNFREEZE ELECTION METHODS ====================
    
    @Transactional
    public ThedalResponse<String> requestElectionFreezeOtp(Long electionId) {
        Long accountId = requestDetails.getCurrentAccountId();
        Long userId = requestDetails.getCurrentUserId();
        
        if (accountId == null) {
            log.error("Account ID not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }
        
        // Verify election exists and belongs to account
        ElectionEntity election = electionRepository.findById(electionId)
                .orElseThrow(() -> new ThedalException(ThedalError.ELECTION_NOT_FOUND, HttpStatus.NOT_FOUND));
        
        if (!election.getAccountId().equals(accountId)) {
            throw new ThedalException(ThedalError.ELECTION_NOT_FOUND, HttpStatus.NOT_FOUND);
        }
        
        // Check if election is already frozen
        if (Boolean.TRUE.equals(election.getIsFrozen())) {
            throw new ThedalException(ThedalError.ELECTION_ALREADY_FROZEN, HttpStatus.BAD_REQUEST);
        }
        
        // Get user details for mobile number
        UserEntity user = userRepo.findById(userId)
                .orElseThrow(() -> new ThedalException(ThedalError.USER_NOT_FOUND, HttpStatus.NOT_FOUND));
        
        String mobileNumber = user.getMobileNumber();
        if (mobileNumber == null || mobileNumber.isEmpty()) {
            throw new ThedalException(ThedalError.MOBILE_NOT_VERIFIED, HttpStatus.BAD_REQUEST);
        }
        
        // Deactivate all previous OTPs for this election
        electionFreezeOtpRepository.deactivateAllOtpsForElection(electionId);
        
        // Generate OTP
        String otp = RandomTokenGenerator.generateOTP(6);
        
        // Save OTP
        ElectionFreezeOtp freezeOtp = new ElectionFreezeOtp();
        freezeOtp.setElectionId(electionId);
        freezeOtp.setMobileNumber(mobileNumber);
        freezeOtp.setOtp(otp);
        freezeOtp.setAction("FREEZE");
        freezeOtp.setUser(user);
        electionFreezeOtpRepository.save(freezeOtp);
        
        // Send OTP via SMS
        smsNotification.sendTransactionalOTP(mobileNumber, otp);
        
        log.info("Freeze OTP sent for electionId: {}, userId: {}", electionId, userId);
        
        return new ThedalResponse<>(ThedalSuccess.OTP_SENT, 
                "OTP sent successfully to " + maskMobileNumber(mobileNumber));
    }
    
    @Transactional
    public ThedalResponse<String> verifyFreezeOtpAndFreezeElection(Long electionId, String otp) {
        Long accountId = requestDetails.getCurrentAccountId();
        
        if (accountId == null) {
            log.error("Account ID not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }
        
        // Verify election exists and belongs to account
        ElectionEntity election = electionRepository.findById(electionId)
                .orElseThrow(() -> new ThedalException(ThedalError.ELECTION_NOT_FOUND, HttpStatus.NOT_FOUND));
        
        if (!election.getAccountId().equals(accountId)) {
            throw new ThedalException(ThedalError.ELECTION_NOT_FOUND, HttpStatus.NOT_FOUND);
        }
        
        // Check if election is already frozen
        if (Boolean.TRUE.equals(election.getIsFrozen())) {
            throw new ThedalException(ThedalError.ELECTION_ALREADY_FROZEN, HttpStatus.BAD_REQUEST);
        }
        
        // Verify OTP
        ElectionFreezeOtp freezeOtp = electionFreezeOtpRepository
                .findByElectionIdAndOtpAndIsActiveTrue(electionId, otp)
                .orElseThrow(() -> new ThedalException(ThedalError.INVALID_OTP, HttpStatus.BAD_REQUEST));
        
        // Check if OTP is expired
        if (freezeOtp.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ThedalException(ThedalError.OTP_EXPIRED, HttpStatus.BAD_REQUEST);
        }
        
        // Check action is FREEZE
        if (!"FREEZE".equals(freezeOtp.getAction())) {
            throw new ThedalException(ThedalError.INVALID_OTP, HttpStatus.BAD_REQUEST);
        }
        
        // Freeze the election
        election.setIsFrozen(true);
        electionRepository.save(election);
        
        // Deactivate the OTP
        freezeOtp.setIsActive(false);
        electionFreezeOtpRepository.save(freezeOtp);
        
        log.info("Election frozen successfully: electionId={}, electionName={}", 
                electionId, election.getElectionName());
        
        return new ThedalResponse<>(ThedalSuccess.ELECTION_UPDATED, 
                "Election '" + election.getElectionName() + "' has been frozen successfully");
    }
    
    @Transactional
    public ThedalResponse<String> requestElectionUnfreezeOtp(Long electionId) {
        Long accountId = requestDetails.getCurrentAccountId();
        Long userId = requestDetails.getCurrentUserId();
        
        if (accountId == null) {
            log.error("Account ID not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }
        
        // Verify election exists and belongs to account
        ElectionEntity election = electionRepository.findById(electionId)
                .orElseThrow(() -> new ThedalException(ThedalError.ELECTION_NOT_FOUND, HttpStatus.NOT_FOUND));
        
        if (!election.getAccountId().equals(accountId)) {
            throw new ThedalException(ThedalError.ELECTION_NOT_FOUND, HttpStatus.NOT_FOUND);
        }
        
        // Check if election is not frozen
        if (!Boolean.TRUE.equals(election.getIsFrozen())) {
            throw new ThedalException(ThedalError.ELECTION_NOT_FROZEN, HttpStatus.BAD_REQUEST);
        }
        
        // Get user details for mobile number
        UserEntity user = userRepo.findById(userId)
                .orElseThrow(() -> new ThedalException(ThedalError.USER_NOT_FOUND, HttpStatus.NOT_FOUND));
        
        String mobileNumber = user.getMobileNumber();
        if (mobileNumber == null || mobileNumber.isEmpty()) {
            throw new ThedalException(ThedalError.MOBILE_NOT_VERIFIED, HttpStatus.BAD_REQUEST);
        }
        
        // Deactivate all previous OTPs for this election
        electionFreezeOtpRepository.deactivateAllOtpsForElection(electionId);
        
        // Generate OTP
        String otp = RandomTokenGenerator.generateOTP(6);
        
        // Save OTP
        ElectionFreezeOtp freezeOtp = new ElectionFreezeOtp();
        freezeOtp.setElectionId(electionId);
        freezeOtp.setMobileNumber(mobileNumber);
        freezeOtp.setOtp(otp);
        freezeOtp.setAction("UNFREEZE");
        freezeOtp.setUser(user);
        electionFreezeOtpRepository.save(freezeOtp);
        
        // Send OTP via SMS
        smsNotification.sendTransactionalOTP(mobileNumber, otp);
        
        log.info("Unfreeze OTP sent for electionId: {}, userId: {}", electionId, userId);
        
        return new ThedalResponse<>(ThedalSuccess.OTP_SENT, 
                "OTP sent successfully to " + maskMobileNumber(mobileNumber));
    }
    
    @Transactional
    public ThedalResponse<String> verifyUnfreezeOtpAndUnfreezeElection(Long electionId, String otp) {
        Long accountId = requestDetails.getCurrentAccountId();
        
        if (accountId == null) {
            log.error("Account ID not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }
        
        // Verify election exists and belongs to account
        ElectionEntity election = electionRepository.findById(electionId)
                .orElseThrow(() -> new ThedalException(ThedalError.ELECTION_NOT_FOUND, HttpStatus.NOT_FOUND));
        
        if (!election.getAccountId().equals(accountId)) {
            throw new ThedalException(ThedalError.ELECTION_NOT_FOUND, HttpStatus.NOT_FOUND);
        }
        
        // Check if election is not frozen
        if (!Boolean.TRUE.equals(election.getIsFrozen())) {
            throw new ThedalException(ThedalError.ELECTION_NOT_FROZEN, HttpStatus.BAD_REQUEST);
        }
        
        // Verify OTP
        ElectionFreezeOtp freezeOtp = electionFreezeOtpRepository
                .findByElectionIdAndOtpAndIsActiveTrue(electionId, otp)
                .orElseThrow(() -> new ThedalException(ThedalError.INVALID_OTP, HttpStatus.BAD_REQUEST));
        
        // Check if OTP is expired
        if (freezeOtp.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ThedalException(ThedalError.OTP_EXPIRED, HttpStatus.BAD_REQUEST);
        }
        
        // Check action is UNFREEZE
        if (!"UNFREEZE".equals(freezeOtp.getAction())) {
            throw new ThedalException(ThedalError.INVALID_OTP, HttpStatus.BAD_REQUEST);
        }
        
        // Unfreeze the election
        election.setIsFrozen(false);
        electionRepository.save(election);
        
        // Deactivate the OTP
        freezeOtp.setIsActive(false);
        electionFreezeOtpRepository.save(freezeOtp);
        
        log.info("Election unfrozen successfully: electionId={}, electionName={}", 
                electionId, election.getElectionName());
        
        return new ThedalResponse<>(ThedalSuccess.ELECTION_UPDATED, 
                "Election '" + election.getElectionName() + "' has been unfrozen successfully");
    }
    
    private String maskMobileNumber(String mobileNumber) {
        if (mobileNumber == null || mobileNumber.length() < 4) {
            return "****";
        }
        return "******" + mobileNumber.substring(mobileNumber.length() - 4);
    }
}
