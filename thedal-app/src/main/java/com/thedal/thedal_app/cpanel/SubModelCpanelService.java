package com.thedal.thedal_app.cpanel;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.thedal.thedal_app.awsfilestore.AwsFileUpload;
import com.thedal.thedal_app.awsfilestore.ImageUpload;
import com.thedal.thedal_app.cpanel.dtos.CasteUpdateCpanelRequest;
import com.thedal.thedal_app.cpanel.dtos.FeedbackIssueRequest;
import com.thedal.thedal_app.cpanel.dtos.FeedbackIssueResponseDTO;
import com.thedal.thedal_app.cpanel.dtos.FeedbackIssueUpdateRequest;
import com.thedal.thedal_app.cpanel.dtos.SlipBoxDTO;
import com.thedal.thedal_app.cpanel.dtos.SubCasteCpanelUpdateRequest;
import com.thedal.thedal_app.election.ElectionRepository;
import com.thedal.thedal_app.files.Files;
import com.thedal.thedal_app.files.FilesRepository;
import com.thedal.thedal_app.files.HandlerType;
import com.thedal.thedal_app.general.RequestDetailsService;
import com.thedal.thedal_app.response.ThedalResponse;
import com.thedal.thedal_app.response.ThedalSuccess;
import com.thedal.thedal_app.settings.electionsettings.Availability;
import com.thedal.thedal_app.settings.electionsettings.AvailabilityRepository;
import com.thedal.thedal_app.settings.electionsettings.AvailabilityResponse;
import com.thedal.thedal_app.settings.electionsettings.BenefitSchemes;
import com.thedal.thedal_app.settings.electionsettings.BenefitSchemesRepository;
import com.thedal.thedal_app.settings.electionsettings.CasteEntity;
import com.thedal.thedal_app.settings.electionsettings.CasteRepository;
import com.thedal.thedal_app.settings.electionsettings.FeedbackIssue;
import com.thedal.thedal_app.settings.electionsettings.FeedbackIssueRepository;
import com.thedal.thedal_app.settings.electionsettings.Language;
import com.thedal.thedal_app.settings.electionsettings.LanguageRepository;
import com.thedal.thedal_app.settings.electionsettings.LanguageResponse;
import com.thedal.thedal_app.settings.electionsettings.Party;
import com.thedal.thedal_app.settings.electionsettings.PartyRepository;
import com.thedal.thedal_app.settings.electionsettings.ReligionEntity;
import com.thedal.thedal_app.settings.electionsettings.ReligionRepository;
import com.thedal.thedal_app.settings.electionsettings.SchemeBy;
import com.thedal.thedal_app.settings.electionsettings.SectionBulkUploadEntity;
import com.thedal.thedal_app.settings.electionsettings.SectionBulkUploadRepository;
import com.thedal.thedal_app.settings.electionsettings.SubCasteEntity;
import com.thedal.thedal_app.settings.electionsettings.SubCasteRepository;
import com.thedal.thedal_app.settings.electionsettings.dto.AvailabilityDTO;
import com.thedal.thedal_app.settings.electionsettings.dto.AvailabilityReponseDTO;
import com.thedal.thedal_app.settings.electionsettings.dto.BenefitSchemesDTO;
import com.thedal.thedal_app.settings.electionsettings.dto.BenefitSchemesUpdateDTO;
import com.thedal.thedal_app.settings.electionsettings.dto.CasteRequest;
import com.thedal.thedal_app.settings.electionsettings.dto.CasteResponseDTO;
import com.thedal.thedal_app.settings.electionsettings.dto.LanguageDTO;
import com.thedal.thedal_app.settings.electionsettings.dto.LanguageResponseDTO;
import com.thedal.thedal_app.settings.electionsettings.dto.PartyRequest;
import com.thedal.thedal_app.settings.electionsettings.dto.PartyResponse;
import com.thedal.thedal_app.settings.electionsettings.dto.ReligionResponseDTO;
import com.thedal.thedal_app.settings.electionsettings.dto.SubCasteRequest;
import com.thedal.thedal_app.settings.electionsettings.dto.SubCasteResponseDTO;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;
import com.thedal.thedal_app.user.MongoUser;
import com.thedal.thedal_app.user.MongoUserRepository;
import com.thedal.thedal_app.user.UserEntity;
import com.thedal.thedal_app.user.UserRepo;
import com.thedal.thedal_app.util.RandomTokenGenerator;
import com.thedal.thedal_app.util.Response;
import com.thedal.thedal_app.voter.BulkUploadStatus;
import com.thedal.thedal_app.voter.VoterEntity;
import com.thedal.thedal_app.voter.VoterRepo;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SubModelCpanelService {
	
	@Autowired
    private RequestDetailsService requestDetails;
	@Autowired
    private LanguageRepository languageRepository;
	@Autowired
    private CasteRepository casteRepository; 
	@Autowired
    private ReligionRepository religionRepository;
	@Autowired
    private SubCasteRepository subCasteRepository;
	@Autowired
	private PartyRepository partyRepository;
	@Autowired
    private ImageUpload imageUpload;
	@Autowired
    private AwsFileUpload awsFileUpload;
	@Value("${aws.s3.banner.bucket}")
	private String s3bucket;
	@Autowired
	private ElectionRepository electionRepository;
	@Autowired
    private AvailabilityRepository availabilityRepository;
	@Autowired 
    private VoterRepo voterRepo;
	@Autowired
    private BenefitSchemesRepository benefitSchemesRepository;
	@Autowired
    private UserRepo userRepository;
	@Autowired
    private SectionBulkUploadRepository sectionBulkUploadRepository;
    @Autowired
    private FilesRepository filesRepository;
    @Autowired
    private CasteFileUploadService casteFileUploadService;
    @Autowired
    private SubCasteFileUploadService subCasteFileUploadService;
    @Autowired
    private FeedbackIssueRepository feedbackIssueRepository;
    @Value("${aws.s3.files.bucket}")
	private String s3Filesbucket;
    @Autowired
    private SlipBoxRepository slipBoxRepository;
    @Autowired
    private SlipBoxMongoRepository slipBoxMongoRepository;
    @Autowired
    private SlipBoxFileUploadService slipBoxFileUploadService;
    @Autowired
    private MongoUserRepository mongoUserRepository;
	
	public ThedalResponse<List<LanguageResponseDTO>> createLanguages(List<LanguageDTO> languageDTOList, HttpServletRequest request) {
	    Long accountId = requestDetails.getCurrentAccountId();

	    if (accountId == null) {
	        log.error("Account ID not found, unauthorized access.");
	        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
	    }

	    // Force electionId = 0 and accountId = 0 for control panel requests
	    Long electionId = 0L;
	    accountId = 0L;

	    List<LanguageResponseDTO> createdLanguages = new ArrayList<>();

	    // Find the highest order index for this election
	    // Integer maxOrderIndex = languageRepository.findMaxOrderIndexByElectionId(electionId);
	    // int orderIndex = (maxOrderIndex != null) ? maxOrderIndex + 1 : 0;
		Integer minOrderIndex = languageRepository.findMinOrderIndexByElectionId(electionId);
		int orderIndex = (minOrderIndex != null) ? minOrderIndex - 1 : 0;


	    for (LanguageDTO languageDTO : languageDTOList) {
	        if (languageRepository.existsByLanguageNameAndElectionId(languageDTO.getLanguageName(), electionId)) {
	            log.warn("Skipping duplicate language '{}'", languageDTO.getLanguageName());
	            continue; 
	        }

	        Language language = new Language();
	        language.setLanguageName(languageDTO.getLanguageName());
	        language.setAccountId(accountId);
	        language.setElectionId(electionId);
	        language.setOrderIndex(orderIndex++);
			language.setState(languageDTO.getState()); 

	        log.info("Saving language: {}", language);
	        Language savedLanguage = languageRepository.save(language);

	        if (savedLanguage.getId() == null) {
	            log.error("Failed to save language: {}", language);
	            throw new ThedalException(ThedalError.LANGUAGE_NOT_FOUND, HttpStatus.NOT_FOUND);
	        }

	        log.info("Language saved successfully with ID: {}", savedLanguage.getId());

	        // Add to response list
	        createdLanguages.add(new LanguageResponseDTO(
	            savedLanguage.getId(),
	            savedLanguage.getLanguageName(),
	            savedLanguage.getAccountId(),
	            savedLanguage.getElectionId(),
	            savedLanguage.getOrderIndex(),
				savedLanguage.getState()
	        ));
	    }

	    return new ThedalResponse<>(ThedalSuccess.LANGUAGE_CREATED, createdLanguages);
	}
	
	public List<LanguageResponse> getLanguage(Long accountId, Long electionId) {
	    // Fetch languages only for electionId = 0 and accountId = 0
	    List<Language> language = languageRepository.findByAccountIdAndElectionIdOrderByOrderIndexAsc(accountId, electionId);

	    if (language.isEmpty()) {
	        throw new ThedalException(ThedalError.LANGUAGE_NOT_FOUND, HttpStatus.NOT_FOUND);
	    }

	    return language.stream()
	            .map(lang -> new LanguageResponse(lang.getId(), lang.getLanguageName(), lang.getOrderIndex(),lang.getState()))
	            .collect(Collectors.toList());
	}
	
	public List<LanguageResponseDTO> updateLanguages(Long accountId, Long electionId, List<LanguageDTO> languageUpdateDTOList) {
	    List<LanguageResponseDTO> updatedLanguages = new ArrayList<>();

	    for (LanguageDTO languageUpdateDTO : languageUpdateDTOList) {
	        Long languageId = languageUpdateDTO.getId();

	        // Ensure we are only updating languages for electionId = 0
	        Language language = languageRepository.findByAccountIdAndElectionIdAndId(accountId, electionId, languageId)
	                .orElseThrow(() -> new ThedalException(ThedalError.LANGUAGE_NOT_FOUND, HttpStatus.NOT_FOUND));

	        if (languageRepository.existsByLanguageNameAndElectionId(languageUpdateDTO.getLanguageName(), electionId)) {
	            log.error("Language with name '{}' already exists in election '{}'.", languageUpdateDTO.getLanguageName(), electionId);
	            throw new ThedalException(ThedalError.LANGUAGE_ALREADY_EXITS, HttpStatus.UNAUTHORIZED);
	        }

	        language.setLanguageName(languageUpdateDTO.getLanguageName());
			language.setState(languageUpdateDTO.getState()); 

			Integer minOrderIndex = languageRepository.findMinOrderIndexByElectionId(electionId);
        	language.setOrderIndex((minOrderIndex != null) ? minOrderIndex - 1 : 0);

	        Language savedLanguage = languageRepository.save(language);

	        updatedLanguages.add(new LanguageResponseDTO(
	            savedLanguage.getId(),
	            savedLanguage.getLanguageName(),
	            savedLanguage.getAccountId(),
	            savedLanguage.getElectionId(),
	            savedLanguage.getOrderIndex(),
				savedLanguage.getState()
	        ));
	    }

	    return updatedLanguages;
	}

//	public void deleteLanguage(Long accountId, Long electionId, Long languageId) { 
//	    Language language = languageRepository.findByAccountIdAndElectionIdAndId(accountId, electionId, languageId)
//	            .orElseThrow(() -> new ThedalException(ThedalError.LANGUAGE_NOT_FOUND, HttpStatus.NOT_FOUND));
//
//	    languageRepository.delete(language);
//	}
	@Transactional
	public void deleteLanguage(Long accountId, Long electionId, List<Long> languageIds) {
	    try {
	        int deletedCount;

	        if (languageIds == null || languageIds.isEmpty()) {
	            log.info("Deleting all languages for accountId: {}, electionId: {}", accountId, electionId);
	            deletedCount = languageRepository.deleteByAccountIdAndElectionId(accountId, electionId);
	        } else {
	            log.info("Deleting specific languages for accountId: {}, electionId: {}, languageIds: {}", 
	                    accountId, electionId, languageIds);
	            deletedCount = languageRepository.deleteByAccountIdAndElectionIdAndIds(accountId, electionId, languageIds);
	        }

	        if (deletedCount == 0) {
	            log.warn("No languages found for accountId: {}, electionId: {}, languageIds: {}", 
	                    accountId, electionId, languageIds);
	            throw new ThedalException(ThedalError.LANGUAGE_NOT_FOUND, HttpStatus.NOT_FOUND);
	        }

	        log.info("Successfully deleted {} languages", deletedCount);

	    } catch (ThedalException e) {
	        log.error("Error while deleting languages: {}", e.getMessage());
	        throw e;
	    } catch (Exception e) {
	        log.error("Unexpected error deleting languages: {}", e.getMessage());
	        throw new ThedalException(ThedalError.ACCESS_DENIED, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
	    }
	}
	
///////// caste api's
	
	@Transactional
	public List<CasteEntity> createMultipleCastes(List<CasteRequest> casteRequests, Long electionId, Long accountId) {
	    List<CasteEntity> casteEntities = new ArrayList<>();

	    for (CasteRequest casteRequest : casteRequests) {
	        if (casteRequest.getCasteName() == null || casteRequest.getReligionId() == null) {
	            log.error("Missing required fields: casteName or religionId for account ID: {}", accountId);
	            throw new ThedalException(ThedalError.MISSING_REQUIRED_FIELDS, HttpStatus.BAD_REQUEST);
	        }

	        log.info("Creating caste '{}' under religion ID: {} for election ID: {}", 
	                 casteRequest.getCasteName(), casteRequest.getReligionId(), electionId);

//	        ReligionEntity religionEntity = religionRepository.findById(casteRequest.getReligionId())
//	                .orElseThrow(() -> {
//	                    log.error("Religion not found for ID: {}", casteRequest.getReligionId());
//	                    return new ThedalException(ThedalError.RELIGION_NOT_FOUND, HttpStatus.BAD_REQUEST);
//	                });
	        ReligionEntity religionEntity = religionRepository.findByIdAndElectionIdAndAccountId(
	                casteRequest.getReligionId(), 0L, 0L).orElseThrow(() -> {
	            log.error("Religion with ID {} not found for electionId = 0 and accountId = 0", casteRequest.getReligionId());
	            return new ThedalException(ThedalError.RELIGION_NOT_FOUND, HttpStatus.BAD_REQUEST);
	        });

	        // Keep existing query (pass electionId = 0 explicitly)
	        Optional<CasteEntity> existingCaste = casteRepository.findByCasteNameAndReligion_IdAndAccountIdAndElectionId(
	                casteRequest.getCasteName(), casteRequest.getReligionId(), accountId, electionId);
	        if (existingCaste.isPresent()) {
	            log.error("Duplicate caste detected: '{}' for religion ID: {} and election ID: {}", 
	                      casteRequest.getCasteName(), casteRequest.getReligionId(), electionId);
	            throw new ThedalException(ThedalError.DUPLICATE_CASTE, HttpStatus.CONFLICT);
	        }

	        // // Keep existing query (pass electionId = 0 explicitly)
	        // Integer maxOrderIndex = casteRepository.findMaxOrderIndexByReligionIdAndElectionId(
	        //         casteRequest.getReligionId(), electionId);
	        // int newOrderIndex = (maxOrderIndex != null) ? maxOrderIndex + 1 : 0;
			Integer minOrderIndex = casteRepository.findMinOrderIndexByReligionIdAndElectionId(
                casteRequest.getReligionId(), electionId);
        	int newOrderIndex = (minOrderIndex != null) ? minOrderIndex - 1 : 0; 

	        // Create and save CasteEntity
	        CasteEntity casteEntity = new CasteEntity();
	        casteEntity.setCasteName(casteRequest.getCasteName());
	        casteEntity.setReligion(religionEntity);
	        casteEntity.setAccountId(accountId);
	        casteEntity.setElectionId(electionId);
	        casteEntity.setOrderIndex(newOrderIndex);

	        casteEntities.add(casteRepository.save(casteEntity));

	        log.info("Caste '{}' created successfully with orderIndex {}.", casteEntity.getCasteName(), newOrderIndex);
	    }

	    return casteEntities;
	}
	
	public List<Map<String, Object>> getCasteForCpanel(Long accountId, Long electionId, Long religionId) {
	    // Fetch castes only for electionId = 0 and accountId = 0
	    //List<CasteEntity> castes = casteRepository.findByAccountIdAndElectionIdOrderByOrderIndexAsc(accountId, electionId);

		List<CasteEntity> castes;

	    if (religionId != null) {
	        castes = casteRepository.findByAccountIdAndElectionIdAndReligionIdOrderByOrderIndexAsc(
	                accountId, electionId, religionId);
	    } else {
	        castes = casteRepository.findByAccountIdAndElectionIdOrderByOrderIndexAsc(accountId, electionId);
	    }
		
	    if (castes.isEmpty()) {
	        log.error("No castes found for accountId: {} and electionId: {}", accountId, electionId);
	        throw new ThedalException(ThedalError.CASTES_NOT_FOUND, HttpStatus.NOT_FOUND);
	    }

	    return castes.stream()
	            .map(caste -> {
	                Map<String, Object> casteData = new HashMap<>();
	                casteData.put("casteId", caste.getId());
	                casteData.put("casteName", caste.getCasteName());
	                casteData.put("orderIndex", caste.getOrderIndex());
	                casteData.put("religionId", caste.getReligion().getId());
	                casteData.put("religionName", caste.getReligion().getReligionName());
	                return casteData;
	            }).collect(Collectors.toList());
	}

	// @Transactional
	// public void deleteCaste(Long accountId, Long electionId, Long casteId) {
	//     CasteEntity caste = casteRepository.findByAccountIdAndElectionIdAndId(accountId, electionId, casteId)
	//         .orElseThrow(() -> new ThedalException(ThedalError.CASTE_NOT_FOUND, HttpStatus.NOT_FOUND));
	//  // Check for linked subcastes
	//     boolean hasLinkedSubCastes = subCasteRepository.existsByCasteIdAndAccountIdAndElectionId(casteId, accountId, electionId);
	//     if (hasLinkedSubCastes) {
	//         log.error("Cannot delete caste ID: {} as it is linked to subcastes.", casteId);
	//         throw new ThedalException(ThedalError.CASTE_LINKED_TO_SUBCASTE, HttpStatus.CONFLICT);
	//     }
	//     casteRepository.delete(caste);
	// }
	@Transactional
public void deleteCaste(Long accountId, Long electionId, List<Long> casteIds) {
    try {
        int deletedCount;

        if (casteIds == null || casteIds.isEmpty()) {
            log.info("Deleting all castes for accountId: {}, electionId: {}", accountId, electionId);
            deletedCount = casteRepository.deleteByAccountIdAndElectionId(accountId, electionId);
        } else {
            log.info("Deleting specific castes for accountId: {}, electionId: {}, casteIds: {}", 
                    accountId, electionId, casteIds);

            // // Check if any caste has linked subcastes
            // boolean hasLinkedSubCastes = subCasteRepository.existsByCasteIdInAndAccountIdAndElectionId(casteIds, accountId, electionId);
            // if (hasLinkedSubCastes) {
            //     log.error("Cannot delete caste IDs: {} as they are linked to subcastes.", casteIds);
            //     throw new ThedalException(ThedalError.CASTE_LINKED_TO_SUBCASTE, HttpStatus.CONFLICT);
            // }
			List<SubCasteEntity> linkedSubCastes = subCasteRepository.findByCasteIdInAndAccountIdAndElectionId(casteIds, accountId, electionId);
            if (!linkedSubCastes.isEmpty()) {
                String subCasteNames = linkedSubCastes.stream()
                        .map(SubCasteEntity::getSubCasteName)
                        .collect(Collectors.joining(", "));
                log.error("Cannot delete caste IDs: {} because they are linked to subcastes: {}.", casteIds, subCasteNames);
                throw new ThedalException(ThedalError.CASTE_LINKED_TO_SUBCASTE, HttpStatus.CONFLICT, 
                        String.format("Cannot delete caste IDs: %s because they are linked to subcastes: %s.", casteIds, subCasteNames));
            }

            deletedCount = casteRepository.deleteByAccountIdAndElectionIdAndIds(accountId, electionId, casteIds);
        }

        if (deletedCount == 0) {
            log.warn("No castes found for accountId: {}, electionId: {}, casteIds: {}", 
                    accountId, electionId, casteIds);
            throw new ThedalException(ThedalError.CASTE_NOT_FOUND, HttpStatus.NOT_FOUND);
        }

        log.info("Successfully deleted {} castes", deletedCount);

    } catch (ThedalException e) {
        log.error("Error while deleting castes: {}", e.getMessage());
        throw e;
    } catch (Exception e) {
        log.error("Unexpected error deleting castes: {}", e.getMessage());
        throw new ThedalException(ThedalError.ACCESS_DENIED, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }
}


	@Transactional
	public List<CasteResponseDTO> updateMultipleCastes(Long accountId, Long electionId, List<CasteUpdateCpanelRequest> casteUpdateRequests) {
	    List<CasteResponseDTO> updatedCastes = new ArrayList<>();

	    for (CasteUpdateCpanelRequest request : casteUpdateRequests) {
	        Long casteId = request.getCasteId();

	        CasteEntity caste = casteRepository.findByAccountIdAndElectionIdAndId(accountId, electionId, casteId)
	                .orElseThrow(() -> new ThedalException(ThedalError.CASTE_NOT_FOUND, HttpStatus.NOT_FOUND));

	        caste.setCasteName(request.getCasteName());
			if (request.getReligionId() != null) {
				ReligionEntity religion = religionRepository.findById(request.getReligionId())
						.orElseThrow(() -> new ThedalException(ThedalError.RELIGION_NOT_FOUND, HttpStatus.BAD_REQUEST));
				caste.setReligion(religion);
			}

			// Fetch the minimum orderIndex and assign updated caste to top
			Integer minOrderIndex = casteRepository.findMinOrderIndexByReligionIdAndElectionId(
                caste.getReligion().getId(), electionId);
				int newOrderIndex = (minOrderIndex != null) ? Math.max(0, minOrderIndex - 1) : 0;


			caste.setOrderIndex(newOrderIndex);
			caste = casteRepository.save(caste);

	        ReligionResponseDTO religionDTO = new ReligionResponseDTO(
	                caste.getReligion().getId(),
	                caste.getReligion().getReligionName(),
	                caste.getReligion().getReligionImage(),
	                caste.getReligion().getReligionColor()
	        );

	        updatedCastes.add(new CasteResponseDTO(caste.getId(), caste.getCasteName(), religionDTO));
	    }

	    return updatedCastes;
	}
//	@Transactional
//	public List<CasteResponseDTO> updateMultipleCastes(Long accountId, Long electionId, List<CasteUpdateCpanelRequest> casteUpdateRequests) {
//	    List<CasteResponseDTO> updatedCastes = new ArrayList<>();
//
//	    for (CasteUpdateCpanelRequest request : casteUpdateRequests) {
//	        Long casteId = request.getCasteId();
//
//	        CasteEntity caste = casteRepository.findByAccountIdAndElectionIdAndId(accountId, electionId, casteId)
//	                .orElseThrow(() -> new ThedalException(ThedalError.CASTE_NOT_FOUND, HttpStatus.NOT_FOUND));
//
//	        caste.setCasteName(request.getCasteName());
//
//	        // Update religion if religionId is provided
//	        if (request.getReligionId() != null) {
//	            ReligionEntity religionEntity = religionRepository.findById(request.getReligionId())
//	                .orElseThrow(() -> new ThedalException(ThedalError.RELIGION_NOT_FOUND, HttpStatus.BAD_REQUEST));
//	            caste.setReligion(religionEntity);
//	        }
//
//	        caste = casteRepository.save(caste);
//
//	        ReligionResponseDTO religionDTO = new ReligionResponseDTO(
//	                caste.getReligion().getId(),
//	                caste.getReligion().getReligionName(),
//	                caste.getReligion().getReligionImage()
//	        );
//
//	        updatedCastes.add(new CasteResponseDTO(caste.getId(), caste.getCasteName(), religionDTO));
//	    }
//
//	    return updatedCastes;
//	}

	@Transactional
    public ThedalResponse<SectionBulkUploadEntity> uploadCpanelCastes(MultipartFile file) {
        long startTime = System.currentTimeMillis();
        Long electionId = 0L; // Hardcoded for cPanel
        Long accountId = 0L;  // Hardcoded for cPanel

        if (!isSupportedFormat(file.getOriginalFilename()) || file.isEmpty()) {
            throw new ThedalException(ThedalError.INVALID_FILE_FORMAT, HttpStatus.BAD_REQUEST);
        }

        Map<String, Integer> headerMapping;
        try {
            if (file.getOriginalFilename().endsWith(".xlsx")) {
                Workbook workbook = new XSSFWorkbook(file.getInputStream());
                Sheet sheet = workbook.getSheetAt(0);
                headerMapping = casteFileUploadService.buildHeaderMapping(sheet.getRow(0));
            } else {
                BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream()));
                String[] headers = br.readLine().split(",");
                headerMapping = casteFileUploadService.buildCsvHeaderMapping(headers);
            }
            List<String> headerErrors = validateMandatoryHeaders(headerMapping);
            if (!headerErrors.isEmpty()) {
                throw new ThedalException(ThedalError.INVALID_FILE_FORMAT, HttpStatus.BAD_REQUEST,
                        "Missing mandatory headers: " + String.join(", ", headerErrors));
            }
        } catch (IOException e) {
            throw new ThedalException(ThedalError.FILE_PROCESSING_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        String folder = "cpanel_caste_uploads";
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        String originalFileName = file.getOriginalFilename();
        String fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        String uniqueFileName = folder + "/caste_" + System.currentTimeMillis() + "_" + uniqueId + fileExtension;

        String fileUrl = null;
        SectionBulkUploadEntity bulkUploadEntity = null;

        try {
            fileUrl = awsFileUpload.uploadMultipartFile(file, uniqueFileName, s3Filesbucket);
            log.info("File uploaded to S3 at: {}", fileUrl);

            bulkUploadEntity = new SectionBulkUploadEntity();
            bulkUploadEntity.setAccountId(accountId);
            bulkUploadEntity.setElectionId(electionId);
            bulkUploadEntity.setStartTime(LocalDateTime.now());
            bulkUploadEntity.setStatus(BulkUploadStatus.IN_PROGRESS);
            bulkUploadEntity.setTotalRecords(0L);
            bulkUploadEntity.setTotalProcessedRecords(0L);
            bulkUploadEntity.setTotalSuccessRecords(0L);
            bulkUploadEntity.setTotalFailedRecords(0L);
            sectionBulkUploadRepository.save(bulkUploadEntity);

            Files fileEntity = new Files(HandlerType.BULKUPLOAD_FILES, bulkUploadEntity.getId(), originalFileName, fileUrl);
            filesRepository.save(fileEntity);
            bulkUploadEntity.setFile(fileEntity);

            if (fileExtension.equalsIgnoreCase(".xlsx")) {
                casteFileUploadService.processCpanelCasteExcelFile(bulkUploadEntity, fileUrl);
            } else if (fileExtension.equalsIgnoreCase(".csv")) {
                casteFileUploadService.processCpanelCasteCsvFile(bulkUploadEntity, fileUrl);
            }

            long endTime = System.currentTimeMillis();
            bulkUploadEntity.setTotalTimeTaken(endTime - startTime);
            bulkUploadEntity.setEndTime(LocalDateTime.now());
            bulkUploadEntity.setStatus(BulkUploadStatus.COMPLETED);
            sectionBulkUploadRepository.save(bulkUploadEntity);

            return new ThedalResponse<>(ThedalSuccess.BULK_CASTES_UPLOADED, bulkUploadEntity);

        } catch (IOException e) {
            log.error("Error uploading file to S3", e);
            if (bulkUploadEntity != null) {
                bulkUploadEntity.setStatus(BulkUploadStatus.FAILED);
                bulkUploadEntity.setEndTime(LocalDateTime.now());
                bulkUploadEntity.setTotalTimeTaken(System.currentTimeMillis() - startTime);
                sectionBulkUploadRepository.save(bulkUploadEntity);
            }
            throw new ThedalException(ThedalError.FILE_PROCESSING_FAILED, HttpStatus.INTERNAL_SERVER_ERROR, "File upload to S3 failed.");
        } catch (Exception e) {
            log.error("Unexpected error processing file '{}': {}", originalFileName, e.getMessage(), e);
            if (bulkUploadEntity != null) {
                bulkUploadEntity.setStatus(BulkUploadStatus.FAILED);
                bulkUploadEntity.setEndTime(LocalDateTime.now());
                bulkUploadEntity.setTotalTimeTaken(System.currentTimeMillis() - startTime);
                sectionBulkUploadRepository.save(bulkUploadEntity);
            }
            throw new ThedalException(ThedalError.FILE_PROCESSING_FAILED, HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred: " + e.getMessage());
        }
    }

    private List<String> validateMandatoryHeaders(Map<String, Integer> headerMapping) {
        List<String> missingHeaders = new ArrayList<>();
        
        // caste_name is always mandatory
        if (!headerMapping.containsKey("caste_name")) {
            missingHeaders.add("caste_name");
        }
        
        // Either religion_id OR religion_name is required (support both old and new formats)
        if (!headerMapping.containsKey("religion_id") && !headerMapping.containsKey("religion_name")) {
            missingHeaders.add("religion_id or religion_name");
        }
        
        return missingHeaders;
    }

    private boolean isSupportedFormat(String originalFileName) {
        return originalFileName != null && (originalFileName.endsWith(".xlsx") || originalFileName.endsWith(".csv"));
    }
	
///////// subCaste api's
	
	@Transactional
	public List<SubCasteEntity> createMultipleSubCastes(List<SubCasteRequest> subCasteRequests, Long electionId, Long accountId) {
	    
	    List<SubCasteEntity> subCasteEntities = new ArrayList<>();

	    for (SubCasteRequest subCasteRequest : subCasteRequests) {
	        
	        if (subCasteRequest.getSubCasteName() == null || subCasteRequest.getCasteId() == null || subCasteRequest.getReligionId() == null) {
	            log.error("Missing required fields: subCasteName, casteId, or religionId for account ID: {}", accountId);
	            throw new ThedalException(ThedalError.MISSING_REQUIRED_FIELDS, HttpStatus.BAD_REQUEST);
	        }

	        log.info("Creating sub-caste '{}' under caste ID: {} and religion ID: {} for election ID: {}", 
	                subCasteRequest.getSubCasteName(), subCasteRequest.getCasteId(), subCasteRequest.getReligionId(), electionId);

	        CasteEntity casteEntity = casteRepository.findById(subCasteRequest.getCasteId())
	                .orElseThrow(() -> {
	                    log.error("Caste not found for ID: {}", subCasteRequest.getCasteId());
	                    return new ThedalException(ThedalError.CASTE_NOT_FOUND, HttpStatus.BAD_REQUEST);
	                });

	        ReligionEntity religionEntity = religionRepository.findById(subCasteRequest.getReligionId())
	                .orElseThrow(() -> {
	                    log.error("Religion not found for ID: {}", subCasteRequest.getReligionId());
	                    return new ThedalException(ThedalError.RELIGION_NOT_FOUND, HttpStatus.BAD_REQUEST);
	                });

	        // // **Find Max Order Index for SubCaste (No electionId)**
	        // Integer maxOrderIndex = subCasteRepository.findMaxOrderIndexByCasteIdAndElectionId(
	        // 	    subCasteRequest.getCasteId(), electionId);  // electionId is hardcoded as 0

	        // // Ensure orderIndex starts at 0
	        // int newOrderIndex = (maxOrderIndex == null || maxOrderIndex == -1) ? 0 : maxOrderIndex + 1;

			Integer minOrderIndex = subCasteRepository.findMinOrderIndexByCasteIdAndElectionId(subCasteRequest.getCasteId(), electionId);
        int newOrderIndex = (minOrderIndex != null) ? minOrderIndex - 1 : 0;
	        
	        // **Duplicate Sub-Caste Validation**
	        Optional<SubCasteEntity> existingSubCaste = subCasteRepository.findBySubCasteNameAndCaste_IdAndReligion_IdAndAccountId(
	            subCasteRequest.getSubCasteName(), subCasteRequest.getCasteId(), subCasteRequest.getReligionId(), accountId);

	        if (existingSubCaste.isPresent()) {
	            log.error("Duplicate sub_caste detected: '{}' for caste ID: {} and religion ID: {}",
	                    subCasteRequest.getSubCasteName(), subCasteRequest.getCasteId(), subCasteRequest.getReligionId());
	            throw new ThedalException(ThedalError.DUPLICATE_SUB_CASTE, HttpStatus.CONFLICT);
	        }

	        SubCasteEntity subCasteEntity = new SubCasteEntity();
	        subCasteEntity.setSubCasteName(subCasteRequest.getSubCasteName());
	        subCasteEntity.setCaste(casteEntity);
	        subCasteEntity.setReligion(religionEntity);
	        subCasteEntity.setAccountId(accountId);
	        subCasteEntity.setElectionId(electionId);  // Always 0
	        subCasteEntity.setOrderIndex(newOrderIndex);

	        subCasteEntities.add(subCasteRepository.save(subCasteEntity));

	        log.info("Sub-caste '{}' created successfully with orderIndex {}.", subCasteEntity.getSubCasteName(), newOrderIndex);
	    }

	    return subCasteEntities;
	}

	public List<Map<String, Object>> getSubCasteForCpanel(Long accountId, Long electionId, Long casteId, Long religionId) {
	    // Hardcode electionId as 0
	    electionId = 0L;

//	    // Fetch subcastes only for cPanel requests (where electionId = 0 and accountId = 0)
//	    List<SubCasteEntity> subcastes = subCasteRepository.findByAccountIdAndElectionIdOrderByOrderIndexAsc(accountId, electionId);
//
//	    if (subcastes.isEmpty()) {
//	        log.error("No subcastes found for accountId: {} and electionId: {}", accountId, electionId);
//	        throw new ThedalException(ThedalError.SUBCASTES_NOT_FOUND, HttpStatus.NOT_FOUND);
//	    }
	    List<SubCasteEntity> subcastes;
	    
	    if (casteId != null && religionId != null) {
	        subcastes = subCasteRepository.findByAccountIdAndElectionIdAndCasteIdAndReligionIdOrderByOrderIndexAsc(
	            accountId, electionId, casteId, religionId);
	    } else if (casteId != null) {
	        subcastes = subCasteRepository.findByAccountIdAndElectionIdAndCasteIdOrderByOrderIndexAsc(
	            accountId, electionId, casteId);
	    } else if (religionId != null) {
	        subcastes = subCasteRepository.findByAccountIdAndElectionIdAndReligionIdOrderByOrderIndexAsc(
	            accountId, electionId, religionId);
	    } else {
	        subcastes = subCasteRepository.findByAccountIdAndElectionIdOrderByOrderIndexAsc(accountId, electionId);
	    }

	    if (subcastes.isEmpty()) {
	        log.error("No subcastes found for filters - accountId: {}, electionId: {}, casteId: {}, religionId: {}", 
	                  accountId, electionId, casteId, religionId);
	        throw new ThedalException(ThedalError.SUBCASTES_NOT_FOUND, HttpStatus.NOT_FOUND);
	    }
	    
	    return subcastes.stream()
	            .map(subCaste -> {
	                Map<String, Object> subCasteData = new HashMap<>();
	                subCasteData.put("subCasteId", subCaste.getId());
	                subCasteData.put("subCasteName", subCaste.getSubCasteName());
	                subCasteData.put("casteName", subCaste.getCaste().getCasteName());
	                subCasteData.put("religionName", subCaste.getReligion().getReligionName());
	                subCasteData.put("orderIndex", subCaste.getOrderIndex());
	                return subCasteData;
	            }).collect(Collectors.toList());
	}

	
	// @Transactional
	// public void deleteSubCaste(Long accountId, Long electionId, Long subCasteId) {
	//     // Hardcoding electionId to 0
	//     electionId = 0L;

	//     SubCasteEntity subCaste = subCasteRepository.findByAccountIdAndElectionIdAndId(accountId, electionId, subCasteId)
	//             .orElseThrow(() -> new ThedalException(ThedalError.SUBCASTE_NOT_FOUND, HttpStatus.NOT_FOUND));

	//     subCasteRepository.delete(subCaste);
	// }
	@Transactional
public void deleteSubCaste(Long accountId, Long electionId, List<Long> subCasteIds) {
    if (subCasteIds == null || subCasteIds.isEmpty()) {
        log.info("Deleting all subcastes for accountId: {}, electionId: {}", accountId, electionId);
        int deletedCount = subCasteRepository.deleteByAccountIdAndElectionId(accountId, electionId);

        if (deletedCount == 0) {
            throw new ThedalException(ThedalError.SUBCASTE_NOT_FOUND, HttpStatus.NOT_FOUND);
        }
    } else {
        log.info("Deleting specific subcastes for accountId: {}, electionId: {}, subCasteIds: {}",
                accountId, electionId, subCasteIds);
        int deletedCount = subCasteRepository.deleteByAccountIdAndElectionIdAndIds(accountId, electionId, subCasteIds);

        if (deletedCount == 0) {
            throw new ThedalException(ThedalError.SUBCASTE_NOT_FOUND, HttpStatus.NOT_FOUND);
        }
    }

    log.info("Successfully deleted subcastes: {}", subCasteIds);
}

	
	@Transactional
	public List<SubCasteResponseDTO> updateMultipleSubCastes(Long accountId, Long electionId, List<SubCasteCpanelUpdateRequest> subCasteUpdateRequests) {
	    // Hardcoding electionId to 0
	    electionId = 0L;

	    List<SubCasteResponseDTO> updatedSubCastes = new ArrayList<>();

	    for (SubCasteCpanelUpdateRequest request : subCasteUpdateRequests) {
	        Long subCasteId = request.getSubCasteId();

	        // Validate that the subCaste exists
	        SubCasteEntity subCaste = subCasteRepository.findByAccountIdAndElectionIdAndId(accountId, electionId, subCasteId)
	                .orElseThrow(() -> new ThedalException(ThedalError.SUBCASTE_NOT_FOUND, HttpStatus.NOT_FOUND));

	        // Update the subCaste entity with the new details
	        subCaste.setSubCasteName(request.getSubCasteName());

			if (request.getCasteId() != null) {
				CasteEntity casteEntity = casteRepository.findById(request.getCasteId())
					.orElseThrow(() -> new ThedalException(ThedalError.CASTE_NOT_FOUND, HttpStatus.BAD_REQUEST));
				subCaste.setCaste(casteEntity);
			}
			if (request.getReligionId() != null) {
				ReligionEntity religionEntity = religionRepository.findById(request.getReligionId())
					.orElseThrow(() -> new ThedalException(ThedalError.RELIGION_NOT_FOUND, HttpStatus.BAD_REQUEST));
				subCaste.setReligion(religionEntity);
			}
			Integer minOrderIndex = subCasteRepository.findMinOrderIndexByCasteIdAndElectionId(
            subCaste.getCaste().getId(), electionId);
        	int newOrderIndex = (minOrderIndex != null) ? minOrderIndex - 1 : 0;

        	subCaste.setOrderIndex(newOrderIndex);
	        // Save the updated subCaste entity
	        subCaste = subCasteRepository.saveAndFlush(subCaste);
			// subCaste = subCasteRepository.saveAndFlush(subCaste);
			log.info("Updated SubCaste ID: {} with new name: {} and orderIndex: {}", 
					  subCaste.getId(), subCaste.getSubCasteName(), subCaste.getOrderIndex());
			
	        // Prepare the DTOs for response
	        ReligionResponseDTO religionDTO = new ReligionResponseDTO(
	                subCaste.getCaste().getReligion().getId(),
	                subCaste.getCaste().getReligion().getReligionName(),
	                subCaste.getCaste().getReligion().getReligionImage(),
	                subCaste.getCaste().getReligion().getReligionColor()
	        );

	        CasteResponseDTO casteDTO = new CasteResponseDTO(
	                subCaste.getCaste().getId(),
	                subCaste.getCaste().getCasteName(),
	                religionDTO
	        );

	        updatedSubCastes.add(new SubCasteResponseDTO(subCaste.getId(), subCaste.getSubCasteName(), casteDTO));
	    }

	    return updatedSubCastes;
	}
	
	@Transactional
    public ThedalResponse<SectionBulkUploadEntity> uploadCpanelSubCastes(MultipartFile file) {
        long startTime = System.currentTimeMillis();
        Long electionId = 0L; // Hardcoded for cPanel
        Long accountId = 0L;  // Hardcoded for cPanel

        if (!isSupportedFormat(file.getOriginalFilename()) || file.isEmpty()) {
            throw new ThedalException(ThedalError.INVALID_FILE_FORMAT, HttpStatus.BAD_REQUEST);
        }

        Map<String, Integer> headerMapping;
        try {
            if (file.getOriginalFilename().endsWith(".xlsx")) {
                Workbook workbook = new XSSFWorkbook(file.getInputStream());
                Sheet sheet = workbook.getSheetAt(0);
                headerMapping = subCasteFileUploadService.buildHeaderMapping(sheet.getRow(0));
            } else {
                BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream()));
                String[] headers = br.readLine().split(",");
                headerMapping = subCasteFileUploadService.buildCsvHeaderMapping(headers);
            }
            List<String> headerErrors = validateMandatoryHeaders1(headerMapping);
            if (!headerErrors.isEmpty()) {
                throw new ThedalException(ThedalError.INVALID_FILE_FORMAT, HttpStatus.BAD_REQUEST,
                        "Missing mandatory headers: " + String.join(", ", headerErrors));
            }
        } catch (IOException e) {
            throw new ThedalException(ThedalError.FILE_PROCESSING_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        String folder = "cpanel_subcaste_uploads";
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        String originalFileName = file.getOriginalFilename();
        String fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        String uniqueFileName = folder + "/subcaste_" + System.currentTimeMillis() + "_" + uniqueId + fileExtension;

        String fileUrl = null;
        SectionBulkUploadEntity bulkUploadEntity = null;

        try {
            fileUrl = awsFileUpload.uploadMultipartFile(file, uniqueFileName, s3Filesbucket);
            log.info("File uploaded to S3 at: {}", fileUrl);

            bulkUploadEntity = new SectionBulkUploadEntity();
            bulkUploadEntity.setAccountId(accountId);
            bulkUploadEntity.setElectionId(electionId);
            bulkUploadEntity.setStartTime(LocalDateTime.now());
            bulkUploadEntity.setStatus(BulkUploadStatus.IN_PROGRESS);
            bulkUploadEntity.setTotalRecords(0L);
            bulkUploadEntity.setTotalProcessedRecords(0L);
            bulkUploadEntity.setTotalSuccessRecords(0L);
            bulkUploadEntity.setTotalFailedRecords(0L);
            sectionBulkUploadRepository.save(bulkUploadEntity);

            Files fileEntity = new Files(HandlerType.BULKUPLOAD_FILES, bulkUploadEntity.getId(), originalFileName, fileUrl);
            filesRepository.save(fileEntity);
            bulkUploadEntity.setFile(fileEntity);

            if (fileExtension.equalsIgnoreCase(".xlsx")) {
                subCasteFileUploadService.processCpanelSubCasteExcelFile(bulkUploadEntity, fileUrl);
            } else if (fileExtension.equalsIgnoreCase(".csv")) {
                subCasteFileUploadService.processCpanelSubCasteCsvFile(bulkUploadEntity, fileUrl);
            }

            long endTime = System.currentTimeMillis();
            bulkUploadEntity.setTotalTimeTaken(endTime - startTime);
            bulkUploadEntity.setEndTime(LocalDateTime.now());
            bulkUploadEntity.setStatus(BulkUploadStatus.COMPLETED);
            sectionBulkUploadRepository.save(bulkUploadEntity);

            return new ThedalResponse<>(ThedalSuccess.BULK_SUBCASTES_UPLOADED, bulkUploadEntity);

        } catch (IOException e) {
            log.error("Error uploading file to S3", e);
            if (bulkUploadEntity != null) {
                bulkUploadEntity.setStatus(BulkUploadStatus.FAILED);
                bulkUploadEntity.setEndTime(LocalDateTime.now());
                bulkUploadEntity.setTotalTimeTaken(System.currentTimeMillis() - startTime);
                sectionBulkUploadRepository.save(bulkUploadEntity);
            }
            throw new ThedalException(ThedalError.FILE_PROCESSING_FAILED, HttpStatus.INTERNAL_SERVER_ERROR, "File upload to S3 failed.");
        } catch (Exception e) {
            log.error("Unexpected error processing file '{}': {}", originalFileName, e.getMessage(), e);
            if (bulkUploadEntity != null) {
                bulkUploadEntity.setStatus(BulkUploadStatus.FAILED);
                bulkUploadEntity.setEndTime(LocalDateTime.now());
                bulkUploadEntity.setTotalTimeTaken(System.currentTimeMillis() - startTime);
                sectionBulkUploadRepository.save(bulkUploadEntity);
            }
            throw new ThedalException(ThedalError.FILE_PROCESSING_FAILED, HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred: " + e.getMessage());
        }
    }

    private List<String> validateMandatoryHeaders1(Map<String, Integer> headerMapping) {
        List<String> missingHeaders = new ArrayList<>();
        
        // sub_caste_name is always mandatory
        if (!headerMapping.containsKey("sub_caste_name")) {
            missingHeaders.add("sub_caste_name");
        }
        
        // Either caste_id OR caste_name is required (support both old and new formats)
        if (!headerMapping.containsKey("caste_id") && !headerMapping.containsKey("caste_name")) {
            missingHeaders.add("caste_id or caste_name");
        }
        
        // Either religion_id OR religion_name is required (support both old and new formats)
        if (!headerMapping.containsKey("religion_id") && !headerMapping.containsKey("religion_name")) {
            missingHeaders.add("religion_id or religion_name");
        }
        
        return missingHeaders;
    }

//    private boolean isSupportedFormat(String originalFileName) {
//        return originalFileName != null && (originalFileName.endsWith(".xlsx") || originalFileName.endsWith(".csv"));
//    }

/////////////// Party api's
	

	@Transactional
	public ThedalResponse<Party> createSingleParty(PartyRequest partyRequest) {
	    Long accountId = requestDetails.getCurrentAccountId();
	    if (accountId == null) {
	        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
	    }

	    accountId = 0L; 
	    Long electionId = 0L; 
	    partyRequest.setElectionId(electionId);

	    // Validate the party request
	    validatePartyRequest(partyRequest);

	    String imageUrl;
	    try {
	        imageUrl = uploadPartyImageToAWS(partyRequest.getPartyImage());
	    } catch (Exception ex) {
	        throw new ThedalException(ThedalError.UPLOAD_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
	    }

	    // Integer maxOrderIndex = partyRepository.findMaxOrderIndexByElectionId(electionId);
	    // int newOrderIndex = (maxOrderIndex != null) ? maxOrderIndex + 1 : 0;
		 // 🔹 Find the minimum order index and assign a new lowest value
		 Integer minOrderIndex = partyRepository.findMinOrderIndexByElectionId(electionId);
		 int newOrderIndex = (minOrderIndex != null) ? minOrderIndex - 1 : 0;

	    Party party = new Party();
	    party.setPartyName(partyRequest.getPartyName());
	    party.setPartyShortName(partyRequest.getPartyShortName());
	    party.setPartyImage(imageUrl);
	    party.setPartyColor(partyRequest.getPartyColor());
	    party.setAllianceName(partyRequest.getAllianceName());
	    party.setAccountId(accountId);
	    party.setElectionId(electionId);
	    party.setOrderIndex(newOrderIndex);

	    try {
	        partyRepository.saveAndFlush(party);
	    } catch (DataIntegrityViolationException ex) {
	        throw new ThedalException(ThedalError.DUPLICATE_PARTY_NAME, HttpStatus.CONFLICT);
	    }

	    return new ThedalResponse<>(ThedalSuccess.PARTY_CREATED, party);
	}

	
	private void validatePartyRequest(PartyRequest partyRequest) {
	    if (partyRequest.getPartyName() == null || partyRequest.getPartyName().isEmpty()) {
	        throw new ThedalException(ThedalError.PARTY_NAME_NOT_FOUND, HttpStatus.BAD_REQUEST);
	    }
	    if (partyRequest.getPartyImage() == null || !isValidImageFormat(partyRequest.getPartyImage())) {
	        throw new ThedalException(ThedalError.INVALID_IMAGE_FORMAT, HttpStatus.BAD_REQUEST);
	    }
	    if (partyRequest.getElectionId() == null) {
	        throw new ThedalException(ThedalError.ELECTION_ID_NOT_FOUND, HttpStatus.BAD_REQUEST);
	    }
	    if (partyRepository.existsByPartyNameAndElectionId(partyRequest.getPartyName(), partyRequest.getElectionId())) {
	        throw new ThedalException(ThedalError.DUPLICATE_PARTY_NAME, HttpStatus.CONFLICT);
	    }
	}

	private boolean isValidImageFormat(MultipartFile image) {
	    String contentType = image.getContentType();
	    return contentType != null && (contentType.equals("image/jpeg") || contentType.equals("image/png"));
	}

	private String uploadPartyImageToAWS(MultipartFile imageFile) {
	    String contentType = imageFile.getContentType();
	    if (!(MediaType.IMAGE_JPEG_VALUE.equals(contentType) ||
	            MediaType.IMAGE_PNG_VALUE.equals(contentType))) {
	        throw new ThedalException(ThedalError.INVALID_IMAGE_FORMAT, HttpStatus.BAD_REQUEST);
	    }

	    long maxFileSize = 5 * 1024 * 1024; // 5MB
	    if (imageFile.getSize() > maxFileSize) {
	        throw new ThedalException(ThedalError.INVALID_IMAGE_FORMAT, HttpStatus.BAD_REQUEST);
	    }

	    // Generate unique file name
	    String fileExtension = "." + awsFileUpload.getFileExtension(imageFile.getOriginalFilename());
	    String fileName = "party_" + System.currentTimeMillis() + "_" + RandomTokenGenerator.generateToken(10) + fileExtension;

	    // Upload to AWS S3
	    try {
	        File tempFile = File.createTempFile("temp", fileExtension);
	        try (FileOutputStream fileOutputStream = new FileOutputStream(tempFile)) {
	            fileOutputStream.write(imageFile.getBytes());
	        }

	        String awsUrl = awsFileUpload.uploadToAWS(tempFile, fileName, s3bucket);

	        if (!tempFile.delete()) {
	            log.warn("Temporary file deletion failed: {}", tempFile.getName());
	        }

	        return awsUrl;
	    } catch (IOException e) {
	        log.error("Error uploading image to AWS S3", e);
	        throw new ThedalException(ThedalError.INVALID_IMAGE_FORMAT, HttpStatus.INTERNAL_SERVER_ERROR);
	    }
	}

	
	public List<PartyResponse> getParties(Long accountId, Long electionId) { 
	    // Hardcoded values
	    accountId = 0L;
	    electionId = 0L;

	    List<Party> parties = partyRepository.findByAccountIdAndElectionIdOrderByOrderIndexAsc(accountId, electionId);

	    if (parties.isEmpty()) {
	        throw new ThedalException(ThedalError.PARTY_NOT_FOUND, HttpStatus.NOT_FOUND);
	    }

	    return parties.stream()
	            .map(party -> new PartyResponse(
	                    party.getId(), 
	                    party.getPartyName(), 
	                    party.getPartyShortName(), 
	                    party.getPartyImage(),
	                    party.getPartyColor(),
	                    party.getAllianceName(),
	                    party.getOrderIndex()
	            ))
	            .collect(Collectors.toList());
	}



	@Transactional
	public ThedalResponse<Party> updateSingleParty(PartyRequest partyRequest) {
	    Long accountId = requestDetails.getCurrentAccountId();
	    if (accountId == null) {
	        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
	    }

	    // Hardcoded values
	    accountId = 0L;
	    Long electionId = 0L;

	    Long partyId = partyRequest.getPartyId();
	    if (partyId == null) {
	        throw new ThedalException(ThedalError.INVALID_PARTY_ID, HttpStatus.BAD_REQUEST);
	    }

	    // Find existing party with accountId = 0 and electionId = 0
	    Party party = partyRepository.findByIdAndAccountIdAndElectionId(partyId, 0L, 0L)
	            .orElseThrow(() -> new ThedalException(ThedalError.PARTY_NOT_FOUND, HttpStatus.NOT_FOUND));

		boolean isUpdated = false;
	    // Update only `partyShortName` if provided
	    if (partyRequest.getPartyShortName() != null && !partyRequest.getPartyShortName().isEmpty()) {
	        party.setPartyShortName(partyRequest.getPartyShortName());
	    }

	    // Update `partyName` if provided
	    if (partyRequest.getPartyName() != null && !partyRequest.getPartyName().isEmpty()) {
	        party.setPartyName(partyRequest.getPartyName());
	    }

	    // Handle image update
	    if (partyRequest.getPartyImage() != null) {
	        try {
	            String newImageUrl = uploadPartyImageToAWS(partyRequest.getPartyImage());
	            party.setPartyImage(newImageUrl);
	        } catch (Exception ex) {
	            throw new ThedalException(ThedalError.UPLOAD_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
	        }
	    }
	    
	    // Update party color if provided
	    if (partyRequest.getPartyColor() != null && !partyRequest.getPartyColor().isEmpty()) {
	        party.setPartyColor(partyRequest.getPartyColor());
	    }
	    
	    // Update alliance name if provided
	    if (partyRequest.getAllianceName() != null && !partyRequest.getAllianceName().isEmpty()) {
	        party.setAllianceName(partyRequest.getAllianceName());
	    }
		if (isUpdated) {
			Integer minOrderIndex = partyRepository.findMinOrderIndexByElectionId(electionId);
			party.setOrderIndex((minOrderIndex != null) ? minOrderIndex - 1 : 0);
		}

	    partyRepository.saveAndFlush(party);

	    return new ThedalResponse<>(ThedalSuccess.PARTY_UPDATED, party);
	}



	
	@Transactional
	public void deleteParty(Long accountId, Long electionId, List<Long> partyIds) {
    try {
        int deletedCount;

        if (partyIds == null || partyIds.isEmpty()) {
            log.info("Deleting all parties for accountId: {}, electionId: {}", accountId, electionId);
            deletedCount = partyRepository.deleteByAccountIdAndElectionId(accountId, electionId);
        } else {
            log.info("Deleting specific parties for accountId: {}, electionId: {}, partyIds: {}", 
                    accountId, electionId, partyIds);
            deletedCount = partyRepository.deleteByAccountIdAndElectionIdAndIds(accountId, electionId, partyIds);
        }

        if (deletedCount == 0) {
            log.warn("No parties found for accountId: {}, electionId: {}, partyIds: {}", 
                    accountId, electionId, partyIds);
            throw new ThedalException(ThedalError.PARTY_NOT_FOUND, HttpStatus.NOT_FOUND);
        }

        log.info("Successfully deleted {} parties", deletedCount);

    } catch (ThedalException e) {
        log.error("Error while deleting parties: {}", e.getMessage());
        throw e;
    } catch (Exception e) {
        log.error("Unexpected error deleting parties: {}", e.getMessage());
        throw new ThedalException(ThedalError.ACCESS_DENIED, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }
}


///////////////////// Availbility api's
	
	@Transactional
	public ThedalResponse<AvailabilityReponseDTO> createAvailability(AvailabilityDTO availabilityDTO, MultipartFile file, HttpServletRequest request) {
	    Long accountId = 0L;
	    Long electionId = 0L; // Default hardcoded values

	    // 🔹 If the request is NOT from "/api/cpanel", set the actual account ID
	    if (!request.getRequestURI().contains("/api/cpanel")) {
	        accountId = requestDetails.getCurrentAccountId();
	        if (accountId == null) {
	            log.error("Account ID not found, unauthorized access.");
	            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
	        }
	    }

	    // 🔹 Ensure unique description
	    // if (availabilityRepository.existsByDescriptionAndElectionId(availabilityDTO.getDescription(), electionId)) {
	    //     log.warn("Skipping duplicate availability '{}'", availabilityDTO.getDescription());
	    //     throw new ThedalException(ThedalError.AVAILABILITY_ALREADY_EXIST, HttpStatus.CONFLICT);
	    // }
		if (availabilityRepository.existsByCategoryNameAndElectionId(availabilityDTO.getCategoryName(), electionId)) {
			log.warn("Category '{}' already exists", availabilityDTO.getCategoryName());
			throw new ThedalException(ThedalError.AVAILABILITY_CATEGORY_ALREADY_EXIST, HttpStatus.CONFLICT);
		}
	
		// Validate description
		if (availabilityRepository.existsByDescriptionAndElectionId(availabilityDTO.getDescription(), electionId)) {
			log.warn("Description '{}' already exists", availabilityDTO.getDescription());
			throw new ThedalException(ThedalError.AVAILABILITY_DESCRIPTION_ALREADY_EXIST, HttpStatus.CONFLICT);
		}
	    // 🔹 Upload file if available
	    String uploadUrl = null;
	    if (file != null && !file.isEmpty()) {
	        try {
	            uploadUrl = uploadAvaiablityImageToAWS(file);
	        } catch (Exception e) {
	            log.error("Error uploading image: ", e);
	            throw new ThedalException(ThedalError.IMAGE_UPLOAD_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
	        }
	    }

	    // // 🔹 Find the highest order index
	    Integer maxOrderIndex = availabilityRepository.findMaxOrderIndexByElectionId(electionId);
	    int orderIndex = (maxOrderIndex != null) ? maxOrderIndex + 1 : 0;
	// 	// Find the minimum orderIndex to bring the new record to the top
	// Integer minOrderIndex = availabilityRepository.findMinOrderIndexByElectionId(electionId);
	// int orderIndex = (minOrderIndex != null) ? minOrderIndex - 1 : 0;  // Ensures top position


	    // 🔹 Create and save availability entity
	    Availability availability = new Availability();
		availability.setCategoryName(availabilityDTO.getCategoryName());
	    availability.setDescription(availabilityDTO.getDescription());
	    availability.setAccountId(accountId);  // 🔹 Ensures `0` for cpanel requests
	    availability.setElectionId(electionId); // 🔹 Ensures `0` for cpanel requests
	    availability.setAvailabilityImage(uploadUrl);
	    availability.setOrderIndex(orderIndex);

	    Availability savedAvailability = availabilityRepository.save(availability);

	    if (savedAvailability.getId() == null) {
	        log.error("Failed to save availability: {}", availability);
	        throw new ThedalException(ThedalError.AVAILABILITY_NOT_FOUND, HttpStatus.INTERNAL_SERVER_ERROR);
	    }

	    log.info("Availability saved successfully with ID: {}", savedAvailability.getId());

	    AvailabilityReponseDTO responseDTO = new AvailabilityReponseDTO(
	            savedAvailability.getId(),
				savedAvailability.getCategoryName(),
	            savedAvailability.getDescription(),
	            savedAvailability.getAvailabilityImage(),
	            savedAvailability.getOrderIndex()
	    );

	    return new ThedalResponse<>(ThedalSuccess.AVAILABILITY_CREATED, responseDTO);
	}



	private String uploadAvaiablityImageToAWS(MultipartFile imageFile) {
        String contentType = imageFile.getContentType();
        if (!(MediaType.IMAGE_JPEG_VALUE.equals(contentType) ||
                MediaType.IMAGE_PNG_VALUE.equals(contentType))) {
            throw new ThedalException(ThedalError.INVALID_IMAGE_FORMAT, HttpStatus.BAD_REQUEST);
        }

        long maxFileSize = 5 * 1024 * 1024;  //less than 500KB
        if (imageFile.getSize() > maxFileSize) {
            throw new ThedalException(ThedalError.INVALID_IMAGE_FORMAT, HttpStatus.BAD_REQUEST);
        }

        // try {
        //     BufferedImage image = ImageIO.read(imageFile.getInputStream());
        //     if (image == null || image.getWidth() != 512 || image.getHeight() != 512) {
        //         throw new ThedalException(ThedalError.INVALID_IMAGE_DIMENSIONS, HttpStatus.BAD_REQUEST);
        //     }
        // } catch (IOException e) {
        //     throw new ThedalException(ThedalError.UPLOAD_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
        // }

        // Generate a unique file name
        String fileExtension = "." + awsFileUpload.getFileExtension(imageFile.getOriginalFilename());
        String fileName = "availabilty_" + System.currentTimeMillis() + "_" + RandomTokenGenerator.generateToken(10) + fileExtension;

        // Upload to AWS S3
        try {
            File tempFile = File.createTempFile("temp", fileExtension);
            try (FileOutputStream fileOutputStream = new FileOutputStream(tempFile)) {
                fileOutputStream.write(imageFile.getBytes());
            }

            String awsUrl = awsFileUpload.uploadToAWS(tempFile, fileName, s3bucket);

            if (!tempFile.delete()) {
                log.warn("Temporary file deletion failed: {}", tempFile.getName());
            }

            return awsUrl;
        } catch (IOException e) {
            log.error("Error uploading image to AWS S3", e);
            throw new ThedalException(ThedalError.UPLOAD_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
 
	
	public ThedalResponse<List<AvailabilityResponse>> getAvailability(HttpServletRequest request) {
	    Long accountId = 0L;
	    Long electionId = 0L; // Hardcoded filter values

	    // Ensure filtering only for accountId = 0 and electionId = 0
	    List<Availability> availabilities = availabilityRepository.findByAccountIdAndElectionIdOrderByCreatedAtDesc(accountId, electionId);

	    if (availabilities.isEmpty()) {
	        throw new ThedalException(ThedalError.AVAILABILITY_NOT_FOUND, HttpStatus.NOT_FOUND);
	    }

	    List<AvailabilityResponse> availabilityResponses = availabilities.stream()
	            .map(availability -> new AvailabilityResponse(
	                    availability.getId(),
						availability.getCategoryName(),
	                    availability.getDescription(),
	                    availability.getAvailabilityImage(),
	                    availability.getOrderIndex()))
	            .collect(Collectors.toList());

	    return new ThedalResponse<>(ThedalSuccess.AVAILABILITY_FOUND, availabilityResponses);
	}


	public AvailabilityReponseDTO updateAvailability(Long accountId, Long electionId, Long availabilityId, AvailabilityDTO availabilityDTO, MultipartFile file) {
	    Availability availability = availabilityRepository.findByAccountIdAndElectionIdAndId(accountId, electionId, availabilityId)
	            .orElseThrow(() -> new ThedalException(ThedalError.AVAILABILITY_NOT_FOUND, HttpStatus.NOT_FOUND));

	    // boolean descriptionExists = availabilityRepository.existsByDescriptionAndElectionId(availabilityDTO.getDescription(), electionId);

	    // // Ensure unique description (allow same description if updating the same record)
	    // if (descriptionExists && !availability.getDescription().equals(availabilityDTO.getDescription())) {
	    //     log.error("Availability with description '{}' already exists.", availabilityDTO.getDescription());
	    //     throw new ThedalException(ThedalError.AVAILABILITY_ALREADY_EXIST, HttpStatus.CONFLICT);
	    // }
 // 🔹 Validate category name uniqueness (exclude current record)
	boolean categoryExists = availabilityRepository.existsByCategoryNameAndElectionId(availabilityDTO.getCategoryName(), electionId);
	if (categoryExists && !availability.getCategoryName().equals(availabilityDTO.getCategoryName())) {
		log.error("Category name '{}' already exists.", availabilityDTO.getCategoryName());
		throw new ThedalException(ThedalError.AVAILABILITY_CATEGORY_ALREADY_EXIST, HttpStatus.CONFLICT);
	}

	// 🔹 Validate description uniqueness (exclude current record)
	boolean descriptionExists = availabilityRepository.existsByDescriptionAndElectionId(availabilityDTO.getDescription(), electionId);
	if (descriptionExists && !availability.getDescription().equals(availabilityDTO.getDescription())) {
		log.error("Description '{}' already exists.", availabilityDTO.getDescription());
		throw new ThedalException(ThedalError.AVAILABILITY_DESCRIPTION_ALREADY_EXIST, HttpStatus.CONFLICT);
	}
	availability.setCategoryName(availabilityDTO.getCategoryName());
	    availability.setDescription(availabilityDTO.getDescription());

	    // 🔹 Handle Image Upload if Provided
	    if (file != null && !file.isEmpty()) {
	        try {
	            log.info("Uploading file: {}", file.getOriginalFilename());
	            String uploadUrl = uploadAvaiablityImageToAWS(file);  // 🔹 Upload to AWS
	            log.info("File uploaded successfully, URL: {}", uploadUrl);
	            availability.setAvailabilityImage(uploadUrl);
	        } catch (Exception e) {
	            log.error("Error uploading image: ", e);
	            throw new ThedalException(ThedalError.IMAGE_UPLOAD_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
	        }
	    }
		// Integer minOrderIndex = availabilityRepository.findMinOrderIndexByElectionId(electionId);
   		// int newOrderIndex = (minOrderIndex != null) ? minOrderIndex - 1 : 0;
		   Integer maxOrderIndex = availabilityRepository.findMaxOrderIndexByElectionId(electionId);
		   int newOrderIndex = (maxOrderIndex != null) ? maxOrderIndex + 1 : 0;
    	availability.setOrderIndex(newOrderIndex);
	    // Save the updated entity
	    Availability savedAvailability = availabilityRepository.save(availability);

	    return new AvailabilityReponseDTO(
	            savedAvailability.getId(),
				savedAvailability.getCategoryName(),
	            savedAvailability.getDescription(),
	            savedAvailability.getAvailabilityImage(),
				savedAvailability.getOrderIndex()
	    );
	}


//	public void deleteAvailability(Long accountId, Long electionId, Long availabilityId) {
//	    // Ensure we are only deleting availability for the correct electionId
//	    Availability availability = availabilityRepository.findByAccountIdAndElectionIdAndId(accountId, electionId, availabilityId)
//	            .orElseThrow(() -> new ThedalException(ThedalError.AVAILABILITY_NOT_FOUND, HttpStatus.NOT_FOUND));
//
//	    // Check if availability is linked to any voter
//	    boolean isLinkedToVoter = voterRepo.existsByAvailabilityAndElectionId(availability.getDescription(), electionId);
//	    //boolean isLinkedToVoter = voterRepo.existsByAvailabilityDescriptionAndElectionId(availability.getDescription(), electionId);
//	    
//	    if (isLinkedToVoter) {
//	        log.error("Cannot delete availability '{}': It is linked to a voter.", availability.getDescription());
//	        throw new ThedalException(ThedalError.AVAILABILITY_LINKED_TO_VOTER, HttpStatus.BAD_REQUEST);
//	    }
//
//	    // Delete the availability from the table
//	    availabilityRepository.delete(availability);
//	}
	@Transactional
	public void deleteAvailability(Long accountId, Long electionId, List<Long> availabilityIds) {
	    try {
	        int deletedCount;

	        if (availabilityIds == null || availabilityIds.isEmpty()) {
	            log.info("Deleting all availabilities for accountId: {}, electionId: {}", accountId, electionId);
	            // Fetch all availabilities to check for linked voters before deletion
	            List<Availability> allAvailabilities = availabilityRepository.findByAccountIdAndElectionId(accountId, electionId);
	            
	            // Check if any availability is linked to a voter
	            for (Availability availability : allAvailabilities) {
	                boolean isLinkedToVoter = voterRepo.existsByAvailabilityAndElectionId(availability.getDescription(), electionId);
	                if (isLinkedToVoter) {
	                    log.error("Cannot delete availability '{}': It is linked to a voter.", availability.getDescription());
	                    throw new ThedalException(ThedalError.AVAILABILITY_LINKED_TO_VOTER, HttpStatus.BAD_REQUEST);
	                }
	            }

	            deletedCount = availabilityRepository.deleteByAccountIdAndElectionId(accountId, electionId);
	        } else {
	            log.info("Deleting specific availabilities for accountId: {}, electionId: {}, availabilityIds: {}", 
	                    accountId, electionId, availabilityIds);
	            
	            // Fetch the specific availabilities to check for linked voters
	            List<Availability> availabilities = availabilityRepository.findByAccountIdAndElectionIdAndIdIn(accountId, electionId, availabilityIds);
	            if (availabilities.size() != availabilityIds.size()) {
	                log.warn("Some availability IDs not found for accountId: {}, electionId: {}", accountId, electionId);
	                throw new ThedalException(ThedalError.AVAILABILITY_NOT_FOUND, HttpStatus.NOT_FOUND);
	            }

	            // Check if any availability is linked to a voter
	            for (Availability availability : availabilities) {
	                boolean isLinkedToVoter = voterRepo.existsByAvailabilityAndElectionId(availability.getDescription(), electionId);
	                if (isLinkedToVoter) {
	                    log.error("Cannot delete availability '{}': It is linked to a voter.", availability.getDescription());
	                    throw new ThedalException(ThedalError.AVAILABILITY_LINKED_TO_VOTER, HttpStatus.BAD_REQUEST);
	                }
	            }

	            deletedCount = availabilityRepository.deleteByAccountIdAndElectionIdAndIdIn(accountId, electionId, availabilityIds);
	        }

	        if (deletedCount == 0) {
	            log.warn("No availabilities found for accountId: {}, electionId: {}, availabilityIds: {}", 
	                    accountId, electionId, availabilityIds);
	            throw new ThedalException(ThedalError.AVAILABILITY_NOT_FOUND, HttpStatus.NOT_FOUND);
	        }

	        log.info("Successfully deleted {} availabilities", deletedCount);

	    } catch (ThedalException e) {
	        log.error("Error while deleting availabilities: {}", e.getMessage());
	        throw e;
	    } catch (Exception e) {
	        log.error("Unexpected error deleting availabilities: {}", e.getMessage());
	        throw new ThedalException(ThedalError.ACCESS_DENIED, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
	    }
	}

//////////benefit schemes api's

	@Transactional
	public ThedalResponse<BenefitSchemesDTO> createBenefitScheme(
	        BenefitSchemesUpdateDTO benefitSchemesDTO, MultipartFile file, HttpServletRequest request) {
	    
	    Long accountId = 0L;
	    Long electionId = 0L;

	    // 🔹 If the request is NOT from "/api/cpanel", use the actual accountId
	    if (!request.getRequestURI().contains("/api/cpanel")) {
	        accountId = requestDetails.getCurrentAccountId();
	        if (accountId == null) {
	            log.error("Account ID not found, unauthorized access.");
	            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
	        }
	    }

	   
	    SchemeBy schemeBy;
	    try {
	        schemeBy = SchemeBy.valueOf(benefitSchemesDTO.getSchemeBy().toUpperCase());
	    } catch (IllegalArgumentException e) {
	        log.error("Invalid schemeBy value: {}", benefitSchemesDTO.getSchemeBy());
	        throw new ThedalException(ThedalError.INVALID_SCHEME_BY, HttpStatus.BAD_REQUEST);
	    }

	    // 🔹 Handle file upload (if present)
	    String uploadUrl = null;
	    if (file != null && !file.isEmpty()) {
	        try {
	            uploadUrl = uploadAvaiablityImageToAWS(file);
	        } catch (Exception e) {
	            log.error("Error uploading image: ", e);
	            throw new ThedalException(ThedalError.IMAGE_UPLOAD_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
	        }
	    }

	   
	    Integer maxOrderIndex = benefitSchemesRepository.findMaxOrderIndexByElectionId(electionId);
	    int newOrderIndex = (maxOrderIndex != null) ? maxOrderIndex + 1 : 0;
		// Integer minOrderIndex = benefitSchemesRepository.findMinOrderIndexByElectionId(electionId);
    	// int newOrderIndex = (minOrderIndex != null) ? minOrderIndex - 1 : 0;

	    BenefitSchemes benefitSchemes = new BenefitSchemes();
	    benefitSchemes.setSchemeName(benefitSchemesDTO.getSchemeName());
	    benefitSchemes.setImageUrl(uploadUrl);
	    benefitSchemes.setSchemeBy(schemeBy);
	    benefitSchemes.setAccountId(accountId);  
	    benefitSchemes.setElectionId(electionId); 
	    benefitSchemes.setOrderIndex(newOrderIndex);
	    benefitSchemes.setSchemeValue(benefitSchemesDTO.getSchemeValue());

	    benefitSchemesRepository.save(benefitSchemes);

	    BenefitSchemesDTO benefitSchemesDTOResponse = new BenefitSchemesDTO(
	        benefitSchemes.getId(),
	        benefitSchemes.getSchemeName(),
	        benefitSchemes.getImageUrl(),
	        benefitSchemes.getSchemeBy().getValue(),
	        benefitSchemes.getOrderIndex(),
	        benefitSchemes.getSchemeValue(),
	        benefitSchemes.getUserSelection()
	    );

	    return new ThedalResponse<>(ThedalSuccess.BENEFITSCHEME_CREATED, benefitSchemesDTOResponse);
	}

	public ThedalResponse<List<BenefitSchemesDTO>> getAll(Long accountId, Long electionId) {
	    
	    // Fetch only benefit schemes where accountId = 0 and electionId = 0
	    List<BenefitSchemes> benefitSchemesList = benefitSchemesRepository
        .findByElectionIdAndAccountIdOrderByCreatedAtDesc(electionId, accountId);

	    List<BenefitSchemesDTO> benefitSchemesDTOList = benefitSchemesList.stream()
	            .map(benefitSchemes -> new BenefitSchemesDTO(
	                    benefitSchemes.getId(),
	                    benefitSchemes.getSchemeName(),
	                    benefitSchemes.getImageUrl(),
	                    benefitSchemes.getSchemeBy().getValue(),
	                    benefitSchemes.getOrderIndex(),
	                    benefitSchemes.getSchemeValue(),
	                    benefitSchemes.getUserSelection()
	            		))
	            .collect(Collectors.toList());

	    log.info("Successfully fetched {} benefit schemes for accountId: {}, electionId: {}", 
	            benefitSchemesDTOList.size(), accountId, electionId);

	    return new ThedalResponse<>(ThedalSuccess.BENEFITSCHEMES_FETCHED, benefitSchemesDTOList);
	}

	public ThedalResponse<BenefitSchemesDTO> updateBenefitScheme(
	        Long benefitSchemeId, Long electionId, BenefitSchemesUpdateDTO benefitSchemesDTO, MultipartFile file) {
	    
	    // Hardcoded accountId = 0
	    Long accountId = 0L;

	    BenefitSchemes existingBenefitScheme = benefitSchemesRepository
	            .findByIdAndAccountIdAndElectionId(benefitSchemeId, accountId, electionId)
	            .orElseThrow(() -> {
	                log.error("BenefitScheme not found with ID: {}, AccountID: {}, ElectionID: {}",
	                        benefitSchemeId, accountId, electionId);
	                return new ThedalException(ThedalError.BENEFITSCHEME_NOT_FOUND, HttpStatus.NOT_FOUND);
	            });

	    // Validate schemeBy field
	    SchemeBy schemeBy;
	    try {
	        schemeBy = SchemeBy.valueOf(benefitSchemesDTO.getSchemeBy().toUpperCase());
	    } catch (IllegalArgumentException e) {
	        log.error("Invalid schemeBy value: {}", benefitSchemesDTO.getSchemeBy());
	        throw new ThedalException(ThedalError.INVALID_SCHEME_BY, HttpStatus.BAD_REQUEST);
	    }
	     
	    // Update fields
	    existingBenefitScheme.setSchemeName(benefitSchemesDTO.getSchemeName());
	    existingBenefitScheme.setSchemeBy(schemeBy);
	    existingBenefitScheme.setSchemeValue(benefitSchemesDTO.getSchemeValue());
	  
	    // Handle file upload
	    String uploadUrl = null;
	    if (file != null && !file.isEmpty()) {
	        try {
	            uploadUrl = uploadAvaiablityImageToAWS(file);
	            existingBenefitScheme.setImageUrl(uploadUrl);
	        } catch (Exception e) {
	            log.error("Error uploading image: ", e);
	            throw new ThedalException(ThedalError.IMAGE_UPLOAD_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
	        }
	    }
		Integer maxOrderIndex = benefitSchemesRepository.findMaxOrderIndexByElectionId(electionId);
		int newOrderIndex = (maxOrderIndex != null) ? maxOrderIndex + 1 : 0;  // Ensure top position if no records
	
		existingBenefitScheme.setOrderIndex(newOrderIndex);
	    // Save updated entity
	    benefitSchemesRepository.save(existingBenefitScheme);

	    // Response DTO
	    BenefitSchemesDTO benefitSchemesDTOResponse = new BenefitSchemesDTO(
	            existingBenefitScheme.getId(),
	            existingBenefitScheme.getSchemeName(),
	            existingBenefitScheme.getImageUrl(),
	            existingBenefitScheme.getSchemeBy().getValue(),
	            existingBenefitScheme.getOrderIndex(),
	            existingBenefitScheme.getSchemeValue(),
	            existingBenefitScheme.getUserSelection()
	    );

	    log.info("BenefitScheme with ID {} successfully updated for account ID {}", benefitSchemeId, accountId);

	    return new ThedalResponse<>(ThedalSuccess.BENEFITSCHEME_UPDATED, benefitSchemesDTOResponse);
	}
	@Transactional
public void deleteBenefitScheme(Long accountId, Long electionId, List<Long> benefitSchemeIds) {
    accountId = 0L;
    electionId = 0L;

    if (benefitSchemeIds == null || benefitSchemeIds.isEmpty()) {
        log.info("Deleting all benefit schemes for accountId: {}, electionId: {}", accountId, electionId);
        int deletedCount = benefitSchemesRepository.deleteByAccountIdAndElectionId(accountId, electionId);

        if (deletedCount == 0) {
            throw new ThedalException(ThedalError.BENEFITSCHEME_NOT_FOUND, HttpStatus.NOT_FOUND);
        }
    } else {
        log.info("Deleting specific benefit schemes for accountId: {}, electionId: {}, benefitSchemeIds: {}",
                accountId, electionId, benefitSchemeIds);

        // Ensure no benefit scheme is linked to any voter before deletion
        List<BenefitSchemes> benefitSchemesList = benefitSchemesRepository.findAllById(benefitSchemeIds);
        for (BenefitSchemes scheme : benefitSchemesList) {
            Optional<VoterEntity> voterUsingScheme = voterRepo.findBySchemeAndElectionId(scheme.getSchemeName(), electionId);
            if (voterUsingScheme.isPresent()) {
                log.error("Cannot delete benefit scheme '{}': It is linked to a voter.", scheme.getSchemeName());
                throw new ThedalException(ThedalError.BENEFIT_SCHEME_LINKED_TO_VOTER, HttpStatus.BAD_REQUEST);
            }
        }

        int deletedCount = benefitSchemesRepository.deleteByAccountIdAndElectionIdAndIds(accountId, electionId, benefitSchemeIds);

        if (deletedCount == 0) {
            throw new ThedalException(ThedalError.BENEFITSCHEME_NOT_FOUND, HttpStatus.NOT_FOUND);
        }
    }

    log.info("Successfully deleted benefit schemes: {}", benefitSchemeIds);
}
	
	@Transactional
    public List<FeedbackIssueResponseDTO> createFeedbackIssues(List<FeedbackIssueRequest> feedbackRequests, Long electionId, Long accountId) {
        List<FeedbackIssueResponseDTO> createdIssues = new ArrayList<>();

        // Fetch existing issues to update their orderIndex
        List<FeedbackIssue> existingIssues = feedbackIssueRepository.findByElectionIdAndAccountIdOrderByOrderIndexAsc(electionId, accountId);
        for (FeedbackIssue issue : existingIssues) {
            issue.setOrderIndex(issue.getOrderIndex() + 1);
        }
        feedbackIssueRepository.saveAll(existingIssues);

        for (FeedbackIssueRequest request : feedbackRequests) {
            if (request.getIssueName() == null || request.getIssueName().isEmpty()) {
                log.error("Missing issueName for account ID: {}", accountId);
                throw new ThedalException(ThedalError.MISSING_REQUIRED_FIELDS, HttpStatus.BAD_REQUEST);
            }

            if (feedbackIssueRepository.existsByIssueNameAndElectionId(request.getIssueName(), electionId)) {
                log.warn("Skipping duplicate feedback issue '{}'", request.getIssueName());
                continue;
            }

            FeedbackIssue issue = new FeedbackIssue();
            issue.setIssueName(request.getIssueName());
            issue.setElectionId(electionId);
            issue.setAccountId(accountId);
            issue.setOrderIndex(0); // New issues get orderIndex 0

            FeedbackIssue savedIssue = feedbackIssueRepository.save(issue);
            createdIssues.add(new FeedbackIssueResponseDTO(savedIssue.getId(), savedIssue.getIssueName(), savedIssue.getOrderIndex()));
            log.info("Feedback issue '{}' created with ID: {}", savedIssue.getIssueName(), savedIssue.getId());
        }

        return createdIssues;
    }

    public List<FeedbackIssueResponseDTO> getFeedbackIssues(Long accountId, Long electionId) {
        List<FeedbackIssue> issues = feedbackIssueRepository.findByAccountIdAndElectionIdOrderByOrderIndexAsc(accountId, electionId);
        if (issues.isEmpty()) {
            log.error("No feedback issues found for accountId: {}, electionId: {}", accountId, electionId);
            throw new ThedalException(ThedalError.ISSUE_NOT_FOUND, HttpStatus.NOT_FOUND);
        }

        return issues.stream()
                .map(issue -> new FeedbackIssueResponseDTO(issue.getId(), issue.getIssueName(), issue.getOrderIndex()))
                .collect(Collectors.toList());
    }
//	public List<FeedbackIssueResponseDTO> getFeedbackIssues(Long accountId, Long electionId) {
//        return feedbackIssueRepository.getIssues(accountId, electionId); // Uses updated method with voter count
//    }

    @Transactional
    public List<FeedbackIssueResponseDTO> updateFeedbackIssues(Long accountId, Long electionId, List<FeedbackIssueUpdateRequest> feedbackUpdateRequests) {
        List<FeedbackIssueResponseDTO> updatedIssues = new ArrayList<>();

        // Fetch existing issues to update their orderIndex
        List<FeedbackIssue> existingIssues = feedbackIssueRepository.findByElectionIdAndAccountIdOrderByOrderIndexAsc(electionId, accountId);
        for (FeedbackIssue issue : existingIssues) {
            issue.setOrderIndex(issue.getOrderIndex() + 1);
        }
        feedbackIssueRepository.saveAll(existingIssues);

        for (FeedbackIssueUpdateRequest request : feedbackUpdateRequests) {
            FeedbackIssue issue = feedbackIssueRepository.findByAccountIdAndElectionIdAndId(accountId, electionId, request.getIssueId())
                    .orElseThrow(() -> new ThedalException(ThedalError.ISSUE_NOT_FOUND, HttpStatus.NOT_FOUND));

            if (request.getIssueName() != null && !request.getIssueName().equals(issue.getIssueName()) &&
                    feedbackIssueRepository.existsByIssueNameAndElectionId(request.getIssueName(), electionId)) {
                log.error("Feedback issue with name '{}' already exists for electionId: {}", request.getIssueName(), electionId);
                throw new ThedalException(ThedalError.ISSUE_ALREADY_EXISTS, HttpStatus.CONFLICT);
            }

            issue.setIssueName(request.getIssueName());
            issue.setOrderIndex(0); // Updated issues get orderIndex 0

            FeedbackIssue savedIssue = feedbackIssueRepository.save(issue);
            updatedIssues.add(new FeedbackIssueResponseDTO(savedIssue.getId(), savedIssue.getIssueName(), savedIssue.getOrderIndex()));
            log.info("Feedback issue ID: {} updated to name: {}", savedIssue.getId(), savedIssue.getIssueName());
        }

        return updatedIssues;
    }

    @Transactional
    public void deleteFeedbackIssues(Long accountId, Long electionId, List<Long> issueIds) {
        try {
            int deletedCount;

            if (issueIds == null || issueIds.isEmpty()) {
                log.info("Deleting all feedback issues for accountId: {}, electionId: {}", accountId, electionId);
                deletedCount = feedbackIssueRepository.deleteByAccountIdAndElectionId(accountId, electionId);
            } else {
                log.info("Deleting specific feedback issues for accountId: {}, electionId: {}, issueIds: {}", accountId, electionId, issueIds);
                deletedCount = feedbackIssueRepository.deleteByAccountIdAndElectionIdAndIdIn(accountId, electionId, issueIds);
            }

            if (deletedCount == 0) {
                log.warn("No feedback issues found for accountId: {}, electionId: {}, issueIds: {}", accountId, electionId, issueIds);
                throw new ThedalException(ThedalError.ISSUE_NOT_FOUND, HttpStatus.NOT_FOUND);
            }

            log.info("Successfully deleted {} feedback issues", deletedCount);
        } catch (ThedalException e) {
            log.error("Error while deleting feedback issues: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error deleting feedback issues: {}", e.getMessage());
            throw new ThedalException(ThedalError.ACCESS_DENIED, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

	
//public ThedalResponse<String> activateUserCpanel(@PathVariable Long userId) {
//    Long accountId = requestDetails.getCurrentAccountId();
//
//    if (accountId == null) {
//        log.error("Account ID not found, unauthorized access.");
//        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
//    }
//
//    // Set control panel defaults
//    Long electionId = 0L;
//   // accountId = 0L;
//
//
//    log.info("Activating Cpanel for userId: {}", userId);
//	
//    UserEntity user = userRepository.findById(userId)
//            .orElseThrow(() -> new ThedalException(ThedalError.USER_NOT_FOUND, HttpStatus.NOT_FOUND));
//	log.info("User isActive before activation check: {}", user.getIsActive());
//    if (user.getIsActive()) {
//        return new ThedalResponse<>(ThedalSuccess.USER_ALREADY_ACTIVE, "User is already active");
//    }
//
//  
//    user.setIsActive(true);
//    userRepository.save(user);
//	userRepository.flush();  
//   
//	log.info("User with userId: {} has been activated successfully", userId);
//    return new ThedalResponse<>(ThedalSuccess.USER_ACTIVATED, "User activated successfully from Control Panel");
//}
    @Transactional
    public ThedalResponse<String> activateUserCpanel(Long userId, LocalDateTime expiryAt) {
        Long accountId = requestDetails.getCurrentAccountId();

        if (accountId == null) {
            log.error("Account ID not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

		log.info("Activating Cpanel for userId: {} with requested expiryAt: {}", userId, expiryAt);

		UserEntity user = userRepository.findById(userId)
				.orElseThrow(() -> new ThedalException(ThedalError.USER_NOT_FOUND, HttpStatus.NOT_FOUND));

		// Always validate provided expiry date first
		if (expiryAt != null && expiryAt.isBefore(LocalDateTime.now())) {
			throw new ThedalException(ThedalError.INVALID_INPUT, HttpStatus.BAD_REQUEST, "Expiry date must be in the future");
		}

		log.info("User current state: isActive={} existingExpiryAt={}", user.getIsActive(), user.getExpiryAt());

		// If already active, allow updating expiryAt (new requirement)
		if (Boolean.TRUE.equals(user.getIsActive())) {
			if (expiryAt != null && (user.getExpiryAt() == null || !user.getExpiryAt().equals(expiryAt))) {
				user.setExpiryAt(expiryAt);
				user.setUpdatedAt(LocalDateTime.now());
				user.setUpdatedBy("Admin");
				userRepository.saveAndFlush(user);
				log.info("Updated expiryAt for already active userId: {} -> {}", userId, expiryAt);
				// Mongo sync intentionally skipped (Mongo not in active use per current requirement)
				return new ThedalResponse<>(ThedalSuccess.USER_ALREADY_ACTIVE, "User already active; expiry date updated");
			}
			return new ThedalResponse<>(ThedalSuccess.USER_ALREADY_ACTIVE, "User is already active");
		}

		// Activate and set expiry date
		user.setIsActive(true);
		user.setExpiryAt(expiryAt);
		user.setUpdatedAt(LocalDateTime.now());
		user.setUpdatedBy("Admin");
		userRepository.saveAndFlush(user);

		// Mongo sync disabled temporarily per current requirement. Previous implementation retained below for reference.
		/*
		try {
			MongoUser existingMongoUser = mongoUserRepository.findFirstByUserId(userId);
			if (existingMongoUser != null) {
				existingMongoUser.setIsActive(true);
				existingMongoUser.setExpiryAt(expiryAt);
				existingMongoUser.setUpdatedAt(user.getUpdatedAt());
				existingMongoUser.setUpdatedBy(user.getUpdatedBy());
				mongoUserRepository.save(existingMongoUser);
				log.info("Successfully updated user in MongoDB: id={}, mongoId={}", userId, existingMongoUser.getId());
			} else {
				MongoUser mongoUser = new MongoUser(user);
				mongoUserRepository.save(mongoUser);
				log.info("Created new user in MongoDB: id={}", userId);
			}
		} catch (Exception mongoEx) {
			log.error("Failed to update user in MongoDB (non-fatal as per current usage): id={}", userId, mongoEx);
		}
		*/

		log.info("User with userId: {} activated successfully with expiryAt: {}", userId, expiryAt);
		return new ThedalResponse<>(ThedalSuccess.USER_ACTIVATED, "User activated successfully from Control Panel");
    }
	
////////////// slipBox api's'

@Transactional
public ThedalResponse<List<SlipBoxDTO>> createSlipBoxes(List<SlipBoxDTO> slipBoxDTOs) {
    Long accountId = 0L; // cPanel uses accountId = 0
    Long electionId = 0L; // cPanel uses electionId = 0

    List<SlipBoxDTO> createdSlipBoxes = new ArrayList<>();

    for (SlipBoxDTO dto : slipBoxDTOs) {
        if (slipBoxRepository.existsBySlipBoxIdAndElectionId(dto.getSlipBoxId(), electionId)) {
            log.warn("Skipping duplicate slip box ID '{}'", dto.getSlipBoxId());
            continue;
        }

        SlipBoxEntity entity = new SlipBoxEntity();
        entity.setMobileNumber(dto.getMobileNumber());
        entity.setSlipBoxName(dto.getSlipBoxName());
        entity.setSlipBoxId(dto.getSlipBoxId());
        entity.setAccountId(accountId);
        entity.setElectionId(electionId);
        entity.setIsDefault(dto.getIsDefault());

        try {
            // Save to PostgreSQL first
            SlipBoxEntity savedEntity = slipBoxRepository.save(entity);
            log.debug("Created slip box in PostgreSQL with id={}", savedEntity.getId());

            try {
                // Save to MongoDB
                SlipBoxMongo mongoEntity = new SlipBoxMongo(savedEntity);
                slipBoxMongoRepository.save(mongoEntity);
                log.debug("Synced slip box to MongoDB with id={}", mongoEntity.getId());
            } catch (Exception mongoEx) {
                log.error("Failed to sync slip box to MongoDB: {}", mongoEx.getMessage());
                // Continue with other slip boxes
            }

            createdSlipBoxes.add(new SlipBoxDTO(
                    savedEntity.getId(),
                    savedEntity.getMobileNumber(),
                    savedEntity.getSlipBoxName(),
                    savedEntity.getSlipBoxId(),
                    savedEntity.getCreatedTime(),
                    savedEntity.getModifiedTime(),
                    savedEntity.isDefault()
            ));
        } catch (Exception ex) {
            log.error("Failed to create slip box '{}': {}", dto.getSlipBoxId(), ex.getMessage());
            // Continue with other slip boxes
        }
    }

    log.info("Created {} slip boxes", createdSlipBoxes.size());
    return new ThedalResponse<>(ThedalSuccess.SLIP_BOX_CREATED, createdSlipBoxes);
}

//@Transactional
//public ThedalResponse<List<SlipBoxDTO>> bulkUploadSlipBoxes(MultipartFile file) {
//    Long accountId = 0L;
//    Long electionId = 0L;
//
//    List<SlipBoxDTO> createdSlipBoxes = new ArrayList<>();
//
//    try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
//        String line;
//        boolean firstLine = true;
//
//        while ((line = reader.readLine()) != null) {
//            if (firstLine) {
//                firstLine = false; // Skip header
//                continue;
//            }
//
//            String[] parts = line.split(",");
//            if (parts.length != 3) {
//                log.warn("Invalid CSV line: {}", line);
//                continue;
//            }
//
//            String mobileNumber = parts[0].trim();
//            String slipBoxName = parts[1].trim();
//            String slipBoxId = parts[2].trim();
//
//            if (slipBoxRepository.existsBySlipBoxIdAndElectionId(slipBoxId, electionId)) {
//                log.warn("Skipping duplicate slip box ID '{}'", slipBoxId);
//                continue;
//            }
//
//            SlipBoxEntity entity = new SlipBoxEntity();
//            entity.setMobileNumber(mobileNumber);
//            entity.setSlipBoxName(slipBoxName);
//            entity.setSlipBoxId(slipBoxId);
//            entity.setAccountId(accountId);
//            entity.setElectionId(electionId);
//
//            SlipBoxEntity savedEntity = slipBoxRepository.save(entity);
//            createdSlipBoxes.add(new SlipBoxDTO(
//                    savedEntity.getId(),
//                    savedEntity.getMobileNumber(),
//                    savedEntity.getSlipBoxName(),
//                    savedEntity.getSlipBoxId(),
//                    savedEntity.getCreatedTime(),
//                    savedEntity.getModifiedTime()
//            ));
//        }
//    } catch (IOException e) {
//        log.error("Error processing CSV file: {}", e.getMessage());
//        throw new ThedalException(ThedalError.INVALID_FILE, HttpStatus.BAD_REQUEST);
//    }
//
//    log.info("Bulk uploaded {} slip boxes", createdSlipBoxes.size());
//    return new ThedalResponse<>(ThedalSuccess.SLIP_BOX_CREATED, createdSlipBoxes);
//}

@Transactional
public ThedalResponse<List<SlipBoxDTO>> getSlipBoxes() {
    Long accountId = 0L;
    Long electionId = 0L;

    // Read from PostgreSQL for better performance
    List<SlipBoxEntity> entities = slipBoxRepository.findByAccountIdAndElectionId(accountId, electionId);
    // Sort by ID manually if needed
    entities.sort((a, b) -> a.getId().compareTo(b.getId()));
    List<SlipBoxDTO> dtos = entities.stream()
            .map(entity -> new SlipBoxDTO(
                    entity.getId(),
                    entity.getMobileNumber(),
                    entity.getSlipBoxName(),
                    entity.getSlipBoxId(),
                    null, // createdTime not needed for read operations
                    null, // modifiedTime not needed for read operations
                    entity.getIsDefault()
            ))
            .collect(Collectors.toList());

    log.info("Fetched {} slip boxes from PostgreSQL", dtos.size());
    return new ThedalResponse<>(ThedalSuccess.SLIP_BOXES_FOUND, dtos);
}

@Transactional
public ThedalResponse<SlipBoxDTO> updateSlipBox(Long slipBoxId, SlipBoxDTO slipBoxDTO) {
    Long accountId = 0L;
    Long electionId = 0L;

    SlipBoxEntity entity = slipBoxRepository.findByIdAndAccountIdAndElectionId(slipBoxId, accountId, electionId)
            .orElseThrow(() -> new ThedalException(ThedalError.SLIP_BOX_NOT_FOUND, HttpStatus.NOT_FOUND));

    if (!entity.getSlipBoxId().equals(slipBoxDTO.getSlipBoxId()) &&
            slipBoxRepository.existsBySlipBoxIdAndElectionId(slipBoxDTO.getSlipBoxId(), electionId)) {
        log.error("Slip box ID '{}' already exists", slipBoxDTO.getSlipBoxId());
        throw new ThedalException(ThedalError.SLIP_BOX_ALREADY_EXISTS, HttpStatus.BAD_REQUEST);
    }

    entity.setMobileNumber(slipBoxDTO.getMobileNumber());
    entity.setSlipBoxName(slipBoxDTO.getSlipBoxName());
    entity.setSlipBoxId(slipBoxDTO.getSlipBoxId());

    try {
        // Update PostgreSQL first
        SlipBoxEntity savedEntity = slipBoxRepository.save(entity);
        log.debug("Updated slip box in PostgreSQL with id={}", savedEntity.getId());

        try {
            // Update MongoDB
            SlipBoxMongo mongoEntity = new SlipBoxMongo(savedEntity);
            slipBoxMongoRepository.save(mongoEntity);
            log.debug("Updated slip box in MongoDB with id={}", mongoEntity.getId());
        } catch (Exception mongoEx) {
            log.error("Failed to update slip box in MongoDB: {}", mongoEx.getMessage());
            // Continue as PostgreSQL update was successful
        }

        SlipBoxDTO responseDTO = new SlipBoxDTO(
                savedEntity.getId(),
                savedEntity.getMobileNumber(),
                savedEntity.getSlipBoxName(),
                savedEntity.getSlipBoxId(),
                savedEntity.getCreatedTime(),
                savedEntity.getModifiedTime(),
                savedEntity.isDefault()
        );

        log.info("Updated slip box with id={}", savedEntity.getId());
        return new ThedalResponse<>(ThedalSuccess.SLIP_BOX_UPDATED, responseDTO);        } catch (Exception ex) {
            log.error("Failed to update slip box: {}", ex.getMessage());
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
}

@Transactional
public ThedalResponse<Void> deleteSlipBoxes(List<Long> slipBoxIds) {
    Long accountId = 0L;
    Long electionId = 0L;

    try {
        if (slipBoxIds == null || slipBoxIds.isEmpty()) {
            // Delete all slip boxes for cPanel
            slipBoxRepository.deleteByAccountIdAndElectionId(accountId, electionId);
            log.info("Deleted all slip boxes from PostgreSQL for cPanel");
            
            try {
                slipBoxMongoRepository.deleteByAccountIdAndElectionId(accountId, electionId);
                log.info("Deleted all slip boxes from MongoDB for cPanel");
            } catch (Exception mongoEx) {
                log.error("Failed to delete all slip boxes from MongoDB: {}", mongoEx.getMessage());
            }
        } else {
            // Delete specific slip boxes
            slipBoxRepository.deleteAllByIdInAndAccountIdAndElectionId(slipBoxIds, accountId, electionId);
            log.info("Deleted slip boxes from PostgreSQL with ids={}", slipBoxIds);
            
            try {
                slipBoxMongoRepository.deleteByIdIn(slipBoxIds);
                log.info("Deleted slip boxes from MongoDB with ids={}", slipBoxIds);
            } catch (Exception mongoEx) {
                log.error("Failed to delete slip boxes from MongoDB: {}", mongoEx.getMessage());
            }
        }

        return new ThedalResponse<>(ThedalSuccess.SLIP_BOXES_DELETED, null);
        
    } catch (Exception ex) {
        log.error("Failed to delete slip boxes: {}", ex.getMessage());
        throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

@Transactional
public ThedalResponse<SectionBulkUploadEntity> uploadCpanelSlipBoxesService(MultipartFile file) {
    long startTime = System.currentTimeMillis();
    Long electionId = 0L; // Hardcoded for cPanel
    Long accountId = 0L;  // Hardcoded for cPanel

    if (!isSupportedFormat(file.getOriginalFilename()) || file.isEmpty()) {
        throw new ThedalException(ThedalError.INVALID_FILE_FORMAT, HttpStatus.BAD_REQUEST);
    }

    Map<String, Integer> headerMapping;
    try {
        if (file.getOriginalFilename().endsWith(".xlsx")) {
            Workbook workbook = new XSSFWorkbook(file.getInputStream());
            Sheet sheet = workbook.getSheetAt(0);
            headerMapping = slipBoxFileUploadService.buildHeaderMapping(sheet.getRow(0));
        } else {
            BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream()));
            String[] headers = br.readLine().split(",");
            headerMapping = slipBoxFileUploadService.buildCsvHeaderMapping(headers);
        }
        List<String> headerErrors = validateMandatoryHeaders125(headerMapping);
        if (!headerErrors.isEmpty()) {
            throw new ThedalException(ThedalError.INVALID_FILE_FORMAT, HttpStatus.BAD_REQUEST,
                    "Missing mandatory headers: " + String.join(", ", headerErrors));
        }
    } catch (IOException e) {
        throw new ThedalException(ThedalError.FILE_PROCESSING_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    String folder = "cpanel_slipbox_uploads";
    String uniqueId = UUID.randomUUID().toString().substring(0, 8);
    String originalFileName = file.getOriginalFilename();
    String fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
    String uniqueFileName = folder + "/slipbox_" + System.currentTimeMillis() + "_" + uniqueId + fileExtension;

    String fileUrl = null;
    SectionBulkUploadEntity bulkUploadEntity = null;

    try {
        fileUrl = awsFileUpload.uploadMultipartFile(file, uniqueFileName, s3Filesbucket);
        log.info("File uploaded to S3 at: {}", fileUrl);

        bulkUploadEntity = new SectionBulkUploadEntity();
        bulkUploadEntity.setAccountId(accountId);
        bulkUploadEntity.setElectionId(electionId);
        bulkUploadEntity.setStartTime(LocalDateTime.now());
        bulkUploadEntity.setStatus(BulkUploadStatus.IN_PROGRESS);
        bulkUploadEntity.setTotalRecords(0L);
        bulkUploadEntity.setTotalProcessedRecords(0L);
        bulkUploadEntity.setTotalSuccessRecords(0L);
        bulkUploadEntity.setTotalFailedRecords(0L);
        sectionBulkUploadRepository.save(bulkUploadEntity);

        Files fileEntity = new Files(HandlerType.BULKUPLOAD_FILES, bulkUploadEntity.getId(), originalFileName, fileUrl);
        filesRepository.save(fileEntity);
        bulkUploadEntity.setFile(fileEntity);

        if (fileExtension.equalsIgnoreCase(".xlsx")) {
            slipBoxFileUploadService.processCpanelSlipBoxExcelFile(bulkUploadEntity, fileUrl);
        } else if (fileExtension.equalsIgnoreCase(".csv")) {
            slipBoxFileUploadService.processCpanelSlipBoxCsvFile(bulkUploadEntity, fileUrl);
        }

        long endTime = System.currentTimeMillis();
        bulkUploadEntity.setTotalTimeTaken(endTime - startTime);
        bulkUploadEntity.setEndTime(LocalDateTime.now());
        bulkUploadEntity.setStatus(BulkUploadStatus.COMPLETED);
        sectionBulkUploadRepository.save(bulkUploadEntity);

        return new ThedalResponse<>(ThedalSuccess.BULK_SLIPBOXES_UPLOADED, bulkUploadEntity);

    } catch (IOException e) {
        log.error("Error uploading file to S3", e);
        if (bulkUploadEntity != null) {
            bulkUploadEntity.setStatus(BulkUploadStatus.FAILED);
            bulkUploadEntity.setEndTime(LocalDateTime.now());
            bulkUploadEntity.setTotalTimeTaken(System.currentTimeMillis() - startTime);
            sectionBulkUploadRepository.save(bulkUploadEntity);
        }
        throw new ThedalException(ThedalError.FILE_PROCESSING_FAILED, HttpStatus.INTERNAL_SERVER_ERROR, "File upload to S3 failed.");
    } catch (Exception e) {
        log.error("Unexpected error processing file '{}': {}", originalFileName, e.getMessage(), e);
        if (bulkUploadEntity != null) {
            bulkUploadEntity.setStatus(BulkUploadStatus.FAILED);
            bulkUploadEntity.setEndTime(LocalDateTime.now());
            bulkUploadEntity.setTotalTimeTaken(System.currentTimeMillis() - startTime);
            sectionBulkUploadRepository.save(bulkUploadEntity);
        }
        throw new ThedalException(ThedalError.FILE_PROCESSING_FAILED, HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred: " + e.getMessage());
    }
}

private List<String> validateMandatoryHeaders125(Map<String, Integer> headerMapping) {
    List<String> missingHeaders = new ArrayList<>();
    String[] mandatoryHeaders = {"mobile_number", "slip_box_name", "slip_box_id"};
    for (String header : mandatoryHeaders) {
        if (!headerMapping.containsKey(header)) {
            missingHeaders.add(header);
        }
    }
    return missingHeaders;
}





}
