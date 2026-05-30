package com.thedal.thedal_app.voter;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thedal.thedal_app.cpanel.VoterHistoryRepository;
import com.thedal.thedal_app.cpanel.dtos.VoterHistoryEntity;
import com.thedal.thedal_app.election.ElectionEntity;
import com.thedal.thedal_app.election.ElectionRepository;
import com.thedal.thedal_app.election.PartManager;
import com.thedal.thedal_app.election.PartManagerMongo;
import com.thedal.thedal_app.general.RequestDetailsService;
import com.thedal.thedal_app.response.ServiceResponse;
import com.thedal.thedal_app.response.ThedalResponse;
import com.thedal.thedal_app.response.ThedalSuccess;
import com.thedal.thedal_app.settings.electionsettings.Availability;
import com.thedal.thedal_app.settings.electionsettings.AvailabilityMongo;
import com.thedal.thedal_app.settings.electionsettings.BenefitSchemes;
import com.thedal.thedal_app.settings.electionsettings.BenefitSchemesMongo;
import com.thedal.thedal_app.settings.electionsettings.CasteCategoryEntity;
import com.thedal.thedal_app.settings.electionsettings.CasteCategoryMongo;
import com.thedal.thedal_app.settings.electionsettings.CasteEntity;
import com.thedal.thedal_app.settings.electionsettings.CasteMongo;
import com.thedal.thedal_app.settings.electionsettings.FeedbackIssue;
import com.thedal.thedal_app.settings.electionsettings.FeedbackIssueMongo;
import com.thedal.thedal_app.settings.electionsettings.Language;
import com.thedal.thedal_app.settings.electionsettings.LanguageMongo;
import com.thedal.thedal_app.settings.electionsettings.Party;
import com.thedal.thedal_app.settings.electionsettings.PartyMongo;
import com.thedal.thedal_app.settings.electionsettings.ReligionEntity;
import com.thedal.thedal_app.settings.electionsettings.ReligionMongo;
import com.thedal.thedal_app.settings.electionsettings.SubCasteEntity;
import com.thedal.thedal_app.settings.electionsettings.SubCasteMongo;
import com.thedal.thedal_app.settings.electionsettings.VoterHistoryMongo;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;
import com.thedal.thedal_app.util.Response;
import com.thedal.thedal_app.voter.dto.BulkUploadDto;
import com.thedal.thedal_app.voter.dto.BulkPhotoUploadResponse;
import com.thedal.thedal_app.voter.dto.BulkUploadErrorResponseDTO;
import com.thedal.thedal_app.voter.dto.BulkUploadResponse;
import com.thedal.thedal_app.voter.dto.BulkUploadStatusDto;
import com.thedal.thedal_app.voter.dto.BulkVoterUpdateResponse;
import com.thedal.thedal_app.voter.dto.EpicNumberRequest;
import com.thedal.thedal_app.voter.dto.FamilyResponseDTO;
import com.thedal.thedal_app.voter.dto.FamilySummaryResponseDTO;
import com.thedal.thedal_app.voter.dto.FamilyMembersResponseDTO;
import com.thedal.thedal_app.voter.dto.FamilySequenceReorderRequest;
import com.thedal.thedal_app.voter.dto.FriendGroupResponseDTO;
import com.thedal.thedal_app.voter.dto.FriendMappingRequest;
import com.thedal.thedal_app.voter.dto.PartVoterStatsDTO;
import com.thedal.thedal_app.voter.dto.VoterDTO;
import com.thedal.thedal_app.voter.dto.VoterExportJobsResponse;
import com.thedal.thedal_app.voter.dto.VoterExportResponse;
import com.thedal.thedal_app.voter.dto.VoterExportStatusResponse;
import com.thedal.thedal_app.voter.dto.VoterMongoDTO;
import com.thedal.thedal_app.voter.dto.VoterOtpRequestDto;
import com.thedal.thedal_app.voter.dto.VoterOtpVerifyDto;
import com.thedal.thedal_app.voter.dto.VoterResponseDTO;
import com.thedal.thedal_app.voter.dto.VoterResponseDTO1;
import com.thedal.thedal_app.voter.dto.VoterResponseMongoDTO;
import com.thedal.thedal_app.voter.dto.VoterUpdateDTO;
import com.thedal.thedal_app.voter.dto.VoterVoteRequest;
import com.thedal.thedal_app.voter.dto.VoterVotingRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("api/v1/voters")
@Slf4j
@Tag(name = "Sub Model Cpanel")
public class VoterController {
	
	@Autowired
    private VoterService voterService;	
	@Autowired
    private VoterMongoService voterMongoService;
	@Autowired
    private RequestDetailsService requestDetails;
	@Autowired
    private VoterDownloadJobRepository voterDownloadJobRepository;
	@Autowired
    private VoterRepo voterRepository; 
	@Autowired
    private ElectionRepository electionRepository;
	@Autowired
	private VoterHistoryRepository voterHistoryRepository;
	@Autowired
	private ObjectMapper objectMapper;
	@Autowired
	private VoterPhotoUrlUpdateService voterPhotoUrlUpdateService;
	@Autowired
	private VoterPhotoDirectS3Service voterPhotoDirectS3Service;


	@Value("${thedal.photo.bulk-zip.max-bytes:500000000}")
	private long bulkPhotoZipMaxBytes;

	private static final Set<String> PART_NO_VARIATIONS = Set.of("part_number", "partno", "partnumber", "partNo", "part_no");
    private static final Set<String> SERIAL_NO_VARIATIONS = Set.of("serial_number", "serialno", "serialnumber", "serialNo", "serial_no");
    private static final Set<String> FNAME_VARIATIONS = Set.of("voter_fname_en", "firstname", "fname");
    private static final Set<String> LNAME_VARIATIONS = Set.of("voter_lname_en", "lastname", "lname");
    private static final Set<String> FULL_ADDRESS_VARIATIONS = Set.of("full_address", "fulladdress", "address");
    private static final Set<String> AGE_VARIATIONS = Set.of("age");

    
    private void validateElectionOwnership(Long electionId, Long accountId) {
        Optional<ElectionEntity> electionOpt = electionRepository.findByIdAndAccountId(electionId, accountId);
        if (!electionOpt.isPresent()) {
            log.error("Election ID {} does not belong to Account ID {}", electionId, accountId);
            throw new ThedalException(ThedalError.INVALID_ELECTION, HttpStatus.FORBIDDEN);  
        }
    }
    
	@Operation(summary = "Create a new voter", description = "Saves a new voter with the specified election ID and booth number", tags = { "Voter Management" })        
	@PostMapping("/election/{electionId}")
	public ThedalResponse<VoterDTO> saveVoter(@PathVariable("electionId") Long electionId,
		     @RequestBody @Valid VoterDTO voterDto) {
		 
		voterDto.setElectionId(electionId);
        
       ThedalResponse<VoterDTO> savedVoter = voterService.saveVoter(voterDto); 
        new ThedalResponse<>(ThedalSuccess.VOTER_CREATED, savedVoter);
		return savedVoter;
	}


	@Operation(summary = "Retrieve a list of voters with gender stats", 
	          description = "Fetches a list of voters based on query parameters: voter-id, epic-number, booth-number, family-id, friend-id, voterName, voterFirstName, voterLastName, voterFnameEn, voterLnameEn, voterFnameL1, voterFnameL2, voterLnameL1, voterLnameL2, relationName, relationFirstName, relationLastName, relationFirstNameEn, relationLastNameEn, party, religion, age, gender, and pollStatus. Use pollStatus to filter by voting status: 'voted' for voters who have voted, 'notVoted' for voters who haven't voted, 'all' for all voters. Includes gender statistics (male, female, other, total). Supports sorting with sortBy (comma-separated list: part_number, partNo, partNumber, serial_number, serialNo, serialNumber, voter_fname_en, voter_lname_en, firstname, lastname, fname, lname, age, full_address, fulladdress, address, case-insensitive) and order (asc, desc). Single or multiple sort fields are supported. If only electionId is provided, all voters for that election will be returned.",
	          tags = {"Voter Management"})
	@GetMapping("/election/{electionId}")
	public ThedalResponse<VoterResponseDTO> getVoters(
	    @PathVariable("electionId") Long electionId,
	    @RequestParam(value = "voter-id", required = false) String voterId,
	    @RequestParam(value = "epic-number", required = false) String epicNumber,
	    @RequestParam(value = "booth-number", required = false) String boothNumbers,
	    @RequestParam(value = "family-id", required = false) UUID familyId,
	    @RequestParam(value = "friend-id", required = false) UUID friendId,
	    @RequestParam(value = "voterName", required = false) String voterName,
	    @RequestParam(value = "voterFirstName", required = false) String voterFirstName,
	    @RequestParam(value = "voterLastName", required = false) String voterLastName,
	    @RequestParam(value = "voterFnameEn", required = false) String voterFnameEn,
	    @RequestParam(value = "voterLnameEn", required = false) String voterLnameEn,
	    @RequestParam(value = "voterFnameL1", required = false) String voterFnameL1,
	    @RequestParam(value = "voterFnameL2", required = false) String voterFnameL2,
	    @RequestParam(value = "voterLnameL1", required = false) String voterLnameL1,
	    @RequestParam(value = "voterLnameL2", required = false) String voterLnameL2,
	    @RequestParam(value = "relationName", required = false) String relationName,
	    @RequestParam(value = "relationFirstName", required = false) String relationFirstName, // New parameter
	    @RequestParam(value = "relationLastName", required = false) String relationLastName, // New parameter
	    @RequestParam(value = "relationFirstNameEn", required = false) String relationFirstNameEn,
	    @RequestParam(value = "relationLastNameEn", required = false) String relationLastNameEn,
	    @RequestParam(value = "party", required = false) String partyName,
	    @RequestParam(value = "religion", required = false) String religionName,
	    @RequestParam(value = "voterHistoryName", required = false) String voterHistoryName,
	    @RequestParam(value = "age", required = false) Integer age,
	    @RequestParam(value = "minAge", required = false) Integer minAge,
	    @RequestParam(value = "maxAge", required = false) Integer maxAge,
	@RequestParam(value = "includeUnknownAge", required = false) Boolean includeUnknownAge,
	    @RequestParam(value = "gender", required = false) String gender,
	    @RequestParam(value = "hasDob", required = false) String hasDob,
	    @RequestParam(value = "starNumber", required = false) Boolean starNumber,
	    @RequestParam(value = "catagoryDescription", required = false) String description,
	    @RequestParam(value = "categoryName", required = false) String categoryName,
	    @RequestParam(value = "casteCategoryName", required = false) String casteCategoryName,
	    @RequestParam(value = "casteName", required = false) String casteName,
	    @RequestParam(value = "subCaste", required = false) String subCaste,
	    @RequestParam(value = "duplicate", required = false) String duplicate,
	    @RequestParam(value = "serial-no", required = false) Long serialNo,
	    @RequestParam(value = "overseas", required = false) Boolean overseas,
	    @RequestParam(value = "fatherless", required = false) Boolean fatherless,
	    @RequestParam(value = "guardian", required = false) Boolean guardian,
	    @RequestParam(value = "birthdayMonth", required = false) Integer birthdayMonth,
	    @RequestParam(value = "birthdayDay", required = false) Integer birthdayDay,
	    @RequestParam(value = "hasMobileNo", required = false) Boolean hasMobileNo,
	    @RequestParam(value = "mobileNo", required = false) String mobileNo,
	    @RequestParam(value = "singleVoterFamily", required = false) Boolean singleVoterFamily,
	    @RequestParam(value = "pollStatus", required = false) String pollStatus,
	    @RequestParam(value = "isFamily", required = false) Boolean isFamily,
	    @RequestParam(value = "page", defaultValue = "0") int page,
	    @RequestParam(value = "size", defaultValue = "10") int size,
	    @RequestParam(value = "sortBy", defaultValue = "part_number,serial_number") String sortBy,
	    @RequestParam(value = "order", defaultValue = "asc") String order) {

	    if (size < 10 || size > 100) {
	        throw new ThedalException(ThedalError.INVALID_PAGE_SIZE, HttpStatus.BAD_REQUEST);
	    }
	    
	    if (Boolean.TRUE.equals(fatherless) && Boolean.TRUE.equals(guardian)) {
	        log.error("Cannot apply both fatherless and guardian filters simultaneously");
	        throw new ThedalException(ThedalError.INVALID_FILTER_COMBINATION, HttpStatus.BAD_REQUEST);
	    }
	    
	    // Validate pollStatus parameter
	    if (pollStatus != null && !pollStatus.isEmpty()) {
	        String pollStatusLower = pollStatus.toLowerCase();
	        if (!pollStatusLower.equals("voted") && !pollStatusLower.equals("notvoted") && !pollStatusLower.equals("all")) {
	            log.error("Invalid pollStatus parameter: {}. Must be 'voted', 'notVoted', or 'all' (case-insensitive)", pollStatus);
	            throw new ThedalException(ThedalError.INVALID_REQUEST, HttpStatus.BAD_REQUEST);
	        }
	    }
	    
	    // Sort validation
	    List<String> sortFields = Arrays.stream(sortBy.split(","))
	            .map(String::trim)
	            .map(String::toLowerCase)
	            .collect(Collectors.toList());

	    List<String> mappedSortFields = new ArrayList<>();
	    for (String sortField : sortFields) {
	        if (PART_NO_VARIATIONS.contains(sortField)) {
	            mappedSortFields.add("partNo");
	        } else if (SERIAL_NO_VARIATIONS.contains(sortField)) {
	            mappedSortFields.add("serialNo");
	        } else if (FNAME_VARIATIONS.contains(sortField)) {
	            mappedSortFields.add("voterFnameEn");
	        } else if (LNAME_VARIATIONS.contains(sortField)) {
	            mappedSortFields.add("voterLnameEn");
	        } else if (FULL_ADDRESS_VARIATIONS.contains(sortField)) {
	            mappedSortFields.add("fullAddress");
	        } else if (AGE_VARIATIONS.contains(sortField)) {
	            mappedSortFields.add("age");
	        } else {
	            log.error("Invalid sortBy parameter: {}. Must be one of: part_number, partNo, partNumber, serial_number, serialNo, serialNumber, voter_fname_en, voter_lname_en, firstname, lastname, fname, lname, age, full_address, fulladdress, address (case-insensitive)", sortField);
	            throw new ThedalException(ThedalError.INVALID_SORT_FIELD, HttpStatus.BAD_REQUEST);
	        }
	    }

	    String orderLower = order.toLowerCase();
	    if (!orderLower.equals("asc") && !orderLower.equals("desc")) {
	        log.error("Invalid order parameter: {}. Must be 'asc' or 'desc' (case-insensitive)", order);
	        throw new ThedalException(ThedalError.INVALID_SORT_FIELD, HttpStatus.BAD_REQUEST);
	    }

//	    // Validate and convert hasDob parameter
//	    Boolean dobFilter = null;
//	    Integer tomorrowMonth = null;
//	    Integer tomorrowDay = null;
//	    if (hasDob != null) {
//	        String hasDobLower = hasDob.toLowerCase();
//	        if ("yes".equals(hasDobLower)) {
//	            dobFilter = true;
//	            LocalDate tomorrow = LocalDate.now().plusDays(1);
//	            tomorrowMonth = tomorrow.getMonthValue();
//	            tomorrowDay = tomorrow.getDayOfMonth();
//	        } else {
//	            log.error("Invalid hasDob parameter: {}. Must be 'yes' (case-insensitive)", hasDob);
//	            throw new ThedalException(ThedalError.INVALID_DOB, HttpStatus.BAD_REQUEST);
//	        }
//	    }
	 // Replace the existing hasDob validation with this:
	    LocalDate today = LocalDate.now();
	    LocalDate tomorrow = today.plusDays(1);
	    Integer todayMonth = null;
	    Integer todayDay = null;
	    Integer tomorrowMonth = null;
	    Integer tomorrowDay = null;
	    Boolean filterToday = false;
		Boolean filterTomorrow = false;
		// New custom birthday (specific month/day) filters; only used when both provided via query params
		Integer customBirthdayMonth = null;
		Integer customBirthdayDay = null;

		if (hasDob != null) {
	        String hasDobLower = hasDob.toLowerCase();
	        switch (hasDobLower) {
	            case "today":
	                filterToday = true;
	                todayMonth = today.getMonthValue();
	                todayDay = today.getDayOfMonth();
	                break;
	            case "tomorrow":
	                filterTomorrow = true;
	                tomorrowMonth = tomorrow.getMonthValue();
	                tomorrowDay = tomorrow.getDayOfMonth();
	                break;
	            case "all":
	                filterToday = true;
	                filterTomorrow = true;
	                todayMonth = today.getMonthValue();
	                todayDay = today.getDayOfMonth();
	                tomorrowMonth = tomorrow.getMonthValue();
	                tomorrowDay = tomorrow.getDayOfMonth();
	                break;
	            default:
	                log.error("Invalid hasDob parameter: {}. Must be 'today', 'tomorrow', or 'all' (case-insensitive)", hasDob);
	                throw new ThedalException(ThedalError.INVALID_DOB, HttpStatus.BAD_REQUEST);
	        }
	    }

		// Validate direct birthdayMonth/birthdayDay params if supplied
		if (birthdayMonth != null || birthdayDay != null) {
			if (birthdayMonth == null || birthdayDay == null) {
				log.error("Both birthdayMonth and birthdayDay must be provided together");
				throw new ThedalException(ThedalError.INVALID_DOB, HttpStatus.BAD_REQUEST);
			}
			if (birthdayMonth < 1 || birthdayMonth > 12) {
				log.error("birthdayMonth out of range: {}", birthdayMonth);
				throw new ThedalException(ThedalError.INVALID_DOB, HttpStatus.BAD_REQUEST);
			}
			if (birthdayDay < 1 || birthdayDay > 31) {
				log.error("birthdayDay out of range: {}", birthdayDay);
				throw new ThedalException(ThedalError.INVALID_DOB, HttpStatus.BAD_REQUEST);
			}
			customBirthdayMonth = birthdayMonth;
			customBirthdayDay = birthdayDay;
			// Override today/tomorrow logic if custom date supplied
			filterToday = false;
			filterTomorrow = false;
			todayMonth = null; todayDay = null; tomorrowMonth = null; tomorrowDay = null;
		}

	    // Parse and validate gender parameter
	    List<String> genderList = null;
	    if (gender != null && !gender.isEmpty()) {
	        genderList = Arrays.stream(gender.split(","))
	                .map(String::trim)
	                .map(String::toLowerCase)
	                .map(g -> {
	                    switch (g) {
	                        case "male":
	                        case "m":
	                            return "male";
	                        case "female":
	                        case "f":
	                            return "female";
	                        case "other":
	                        case "o":
	                            return "other";
	                        default:
	                            return g;
	                    }
	                })
	                .collect(Collectors.toList());

	        Set<String> validGenders = Set.of("male", "female", "other");
	        List<String> invalidGenders = genderList.stream()
	                .filter(g -> !validGenders.contains(g))
	                .collect(Collectors.toList());

	        if (!invalidGenders.isEmpty()) {
	            log.error("Invalid gender values: {}. Must be one or more of: male, female, other (case-insensitive)", String.join(", ", invalidGenders));
	            throw new ThedalException(ThedalError.INVALID_GENDER, HttpStatus.BAD_REQUEST);
	        }
	    }

	    // Parse voterName
	    List<String> voterNameList = null;
	    if (voterName != null && !voterName.isEmpty()) {
	        voterNameList = Arrays.stream(voterName.split(","))
	                .map(String::trim)
	                .filter(name -> !name.isEmpty())
	                .map(String::toLowerCase)
	                .collect(Collectors.toList());
	        // Override all voter name parameters
	        voterFnameEn = null;
	        voterLnameEn = null;
	        voterFnameL1 = null;
	        voterFnameL2 = null;
	        voterLnameL1 = null;
	        voterLnameL2 = null;
	    }

	    // Parse voterFirstName
	    List<String> voterFirstNameList = null;
	    if (voterFirstName != null && !voterFirstName.isEmpty()) {
	        voterFirstNameList = Arrays.stream(voterFirstName.split(","))
	                .map(String::trim)
	                .filter(name -> !name.isEmpty())
	                .map(String::toLowerCase)
	                .collect(Collectors.toList());
	        // Override voter first name parameters
	        voterFnameEn = null;
	        voterFnameL1 = null;
	        voterFnameL2 = null;
	    }

	    // Parse voterLastName
	    List<String> voterLastNameList = null;
	    if (voterLastName != null && !voterLastName.isEmpty()) {
	        voterLastNameList = Arrays.stream(voterLastName.split(","))
	                .map(String::trim)
	                .filter(name -> !name.isEmpty())
	                .map(String::toLowerCase)
	                .collect(Collectors.toList());
	        // Override voter last name parameters
	        voterLnameEn = null;
	        voterLnameL1 = null;
	        voterLnameL2 = null;
	    }

	    // Parse voterFnameEn
	    List<String> voterFnameEnList = null;
	    if (voterFnameEn != null && !voterFnameEn.isEmpty()) {
	        voterFnameEnList = Arrays.stream(voterFnameEn.split(","))
	                .map(String::trim)
	                .filter(name -> !name.isEmpty())
	                .map(String::toLowerCase)
	                .collect(Collectors.toList());
	    }

	    // Parse voterLnameEn
	    List<String> voterLnameEnList = null;
	    if (voterLnameEn != null && !voterLnameEn.isEmpty()) {
	        voterLnameEnList = Arrays.stream(voterLnameEn.split(","))
	                .map(String::trim)
	                .filter(name -> !name.isEmpty())
	                .map(String::toLowerCase)
	                .collect(Collectors.toList());
	    }

	    // Parse voterFnameL1
	    List<String> voterFnameL1List = null;
	    if (voterFnameL1 != null && !voterFnameL1.isEmpty()) {
	        voterFnameL1List = Arrays.stream(voterFnameL1.split(","))
	                .map(String::trim)
	                .filter(name -> !name.isEmpty())
	                .map(String::toLowerCase)
	                .collect(Collectors.toList());
	    }

	    // Parse voterFnameL2
	    List<String> voterFnameL2List = null;
	    if (voterFnameL2 != null && !voterFnameL2.isEmpty()) {
	        voterFnameL2List = Arrays.stream(voterFnameL2.split(","))
	                .map(String::trim)
	                .filter(name -> !name.isEmpty())
	                .map(String::toLowerCase)
	                .collect(Collectors.toList());
	    }

	    // Parse voterLnameL1
	    List<String> voterLnameL1List = null;
	    if (voterLnameL1 != null && !voterLnameL1.isEmpty()) {
	        voterLnameL1List = Arrays.stream(voterLnameL1.split(","))
	                .map(String::trim)
	                .filter(name -> !name.isEmpty())
	                .map(String::toLowerCase)
	                .collect(Collectors.toList());
	    }

	    // Parse voterLnameL2
	    List<String> voterLnameL2List = null;
	    if (voterLnameL2 != null && !voterLnameL2.isEmpty()) {
	        voterLnameL2List = Arrays.stream(voterLnameL2.split(","))
	                .map(String::trim)
	                .filter(name -> !name.isEmpty())
	                .map(String::toLowerCase)
	                .collect(Collectors.toList());
	    }

	    // Parse relationName
	    List<String> relationNameList = null;
	    if (relationName != null && !relationName.isEmpty()) {
	        relationNameList = Arrays.stream(relationName.split(","))
	                .map(String::trim)
	                .filter(name -> !name.isEmpty())
	                .map(String::toLowerCase)
	                .collect(Collectors.toList());
	        // Override all relation name parameters
	        relationFirstName = null;
	        relationLastName = null;
	        relationFirstNameEn = null;
	        relationLastNameEn = null;
	    }

	    // Parse relationFirstName
	    List<String> relationFirstNameList = null;
	    if (relationFirstName != null && !relationFirstName.isEmpty()) {
	        relationFirstNameList = Arrays.stream(relationFirstName.split(","))
	                .map(String::trim)
	                .filter(name -> !name.isEmpty())
	                .map(String::toLowerCase)
	                .collect(Collectors.toList());
	        // Override relation first name parameters
	        relationFirstNameEn = null;
	    }

	    // Parse relationLastName
	    List<String> relationLastNameList = null;
	    if (relationLastName != null && !relationLastName.isEmpty()) {
	        relationLastNameList = Arrays.stream(relationLastName.split(","))
	                .map(String::trim)
	                .filter(name -> !name.isEmpty())
	                .map(String::toLowerCase)
	                .collect(Collectors.toList());
	        // Override relation last name parameters
	        relationLastNameEn = null;
	    }

	    // Parse relationFirstNameEn
	    List<String> relationFirstNameEnList = null;
	    if (relationFirstNameEn != null && !relationFirstNameEn.isEmpty()) {
	        relationFirstNameEnList = Arrays.stream(relationFirstNameEn.split(","))
	                .map(String::trim)
	                .filter(name -> !name.isEmpty())
	                .map(String::toLowerCase)
	                .collect(Collectors.toList());
	        log.info("DEBUG FIX - Parsed relationFirstNameEn parameter: {} -> list: {}", relationFirstNameEn, relationFirstNameEnList);
	    }

	    // Parse relationLastNameEn
	    List<String> relationLastNameEnList = null;
	    if (relationLastNameEn != null && !relationLastNameEn.isEmpty()) {
	        relationLastNameEnList = Arrays.stream(relationLastNameEn.split(","))
	                .map(String::trim)
	                .filter(name -> !name.isEmpty())
	                .map(String::toLowerCase)
	                .collect(Collectors.toList());
	    }

	    // Initialize L1/L2 lists (populated later in override logic section)
	    List<String> rlnFnameL1List = null;
	    List<String> rlnFnameL2List = null;
	    List<String> rlnLnameL1List = null;
	    List<String> rlnLnameL2List = null;

	    // Parse booth numbers
	    List<Integer> boothNumberList = null;
	    if (boothNumbers != null && !boothNumbers.isEmpty()) {
	        try {
	            boothNumberList = Arrays.stream(boothNumbers.split(","))
	                    .map(String::trim)
	                    .map(Integer::valueOf)
	                    .collect(Collectors.toList());
	        } catch (NumberFormatException e) {
	            throw new ThedalException(ThedalError.INVALID_BOOTH_NUMBER_FORMAT, HttpStatus.BAD_REQUEST);
	        }
	    }

    // Parse categoryName (availability category)
    List<String> categoryNameList = null;
    if (categoryName != null && !categoryName.isEmpty()) {
        categoryNameList = Arrays.stream(categoryName.split(","))
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toList());
        log.debug("Parsed categoryNameList: {}", categoryNameList);
    }

    // Parse casteCategoryName
    List<String> casteCategoryNameList = null;
    if (casteCategoryName != null && !casteCategoryName.isEmpty()) {
        casteCategoryNameList = Arrays.stream(casteCategoryName.split(","))
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toList());
        log.debug("Parsed casteCategoryNameList: {}", casteCategoryNameList);
    }
    
    // Parse casteName
    List<String> casteNameList = null;
    if (casteName != null && !casteName.isEmpty()) {
        casteNameList = Arrays.stream(casteName.split(","))
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toList());
        log.debug("Parsed casteNameList: {}", casteNameList);
    }
    
    // Parse subCasteName
    List<String> subCasteNameList = null;
    if (subCaste != null && !subCaste.isEmpty()) {
        subCasteNameList = Arrays.stream(subCaste.split(","))
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toList());
        log.debug("Parsed subCasteNameList: {}", subCasteNameList);
    }

    // Parse religionName
    List<String> religionNameList = null;
    if (religionName != null && !religionName.isEmpty()) {
        religionNameList = Arrays.stream(religionName.split(","))
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toList());
        log.debug("Parsed religionNameList: {}", religionNameList);
    }

    // Parse catagoryDescription (availability description)
    List<String> descriptionList = null;
    if (description != null && !description.isEmpty()) {
        descriptionList = Arrays.stream(description.split(","))
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toList());
        log.debug("Parsed descriptionList: {}", descriptionList);
    }
	    // Parse partyName
	    List<String> partyNameList = null;
	    if (partyName != null && !partyName.isEmpty()) {
	        partyNameList = Arrays.stream(partyName.split(","))
	                .map(String::trim)
	                .filter(name -> !name.isEmpty())
	                .map(String::toLowerCase)
	                .collect(Collectors.toList());
	        log.debug("Parsed partyNameList: {}", partyNameList);
	        if (partyNameList.isEmpty()) {
	            log.warn("partyName parameter provided but resulted in empty list: {}", partyName);
	        }
	    } else {
	        log.debug("No partyName parameter provided");
	    }

	    // Parse voterHistoryName
	    List<String> voterHistoryNameList = null;
	    if (voterHistoryName != null && !voterHistoryName.isEmpty()) {
	        voterHistoryNameList = Arrays.stream(voterHistoryName.split(","))
	                .map(String::trim)
	                .filter(name -> !name.isEmpty())
	                .map(String::toLowerCase)
	                .collect(Collectors.toList());
	        log.debug("Parsed voterHistoryNameList: {}", voterHistoryNameList);
	        if (voterHistoryNameList.isEmpty()) {
	            log.warn("voterHistoryName parameter provided but resulted in empty list: {}", voterHistoryName);
	        }
	    } else {
	        log.debug("No voterHistoryName parameter provided");
	    }

	    // Override logic: voterName takes precedence over voterFirstName and voterLastName
	    if (voterNameList != null) {
	        // voterName searches all language fields (full name in any language)
	        voterFnameEnList = voterNameList;
	        voterLnameEnList = voterNameList;
	        voterFnameL1List = voterNameList;
	        voterFnameL2List = voterNameList;
	        voterLnameL1List = voterNameList;
	        voterLnameL2List = voterNameList;
	    } else {
	        // Generic parameters (voterFirstName, voterLastName) only search English fields
	        // This makes them equivalent to voterFnameEn, voterLnameEn for better usability
	        if (voterFirstNameList != null && voterFnameEnList == null) {
	            voterFnameEnList = voterFirstNameList;
	        }
	        if (voterLastNameList != null && voterLnameEnList == null) {
	            voterLnameEnList = voterLastNameList;
	        }
	        // DO NOT set L1/L2 from generic parameters - those are for other language scripts
	    }

	    // Override logic: relationName takes precedence over relationFirstName and relationLastName
	    if (relationNameList != null) {
	        relationFirstNameEnList = relationNameList;
	        relationLastNameEnList = relationNameList;
	        rlnFnameL1List = relationNameList;
	        rlnFnameL2List = relationNameList;
	        rlnLnameL1List = relationNameList;
	        rlnLnameL2List = relationNameList;
    } else {
        // Generic parameters (relationFirstName, relationLastName) only search English fields
        // This makes them equivalent to relationFirstNameEn, relationLastNameEn for better usability
        if (relationFirstNameList != null && relationFirstNameEnList == null) {
            relationFirstNameEnList = relationFirstNameList;
        }
        if (relationLastNameList != null && relationLastNameEnList == null) {
            relationLastNameEnList = relationLastNameList;
        }
        // DO NOT set L1/L2 from generic parameters - those are for other language scripts
    }	    // Debug logging for parameters
	    log.info("DEBUG FIX - Parameters after parsing: voterFirstNameList={}, voterLastNameList={}, voterFnameEnList={}, voterLnameEnList={}, voterFnameL1List={}, voterFnameL2List={}, voterLnameL1List={}, voterLnameL2List={}",
	             voterFirstNameList, voterLastNameList, voterFnameEnList, voterLnameEnList, voterFnameL1List, voterFnameL2List, voterLnameL1List, voterLnameL2List);
	    log.info("DEBUG FIX - Relation parameters FINAL: relationFirstNameList={}, relationLastNameList={}, relationFirstNameEnList={}, relationLastNameEnList={}, rlnFnameL1List={}, rlnFnameL2List={}, rlnLnameL1List={}, rlnLnameL2List={}",
	             relationFirstNameList, relationLastNameList, relationFirstNameEnList, relationLastNameEnList, rlnFnameL1List, rlnFnameL2List, rlnLnameL1List, rlnLnameL2List);

	    Sort.Direction direction = orderLower.equals("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
	    Sort sort = Sort.by(direction, mappedSortFields.toArray(new String[0]));
	    Pageable pageable = PageRequest.of(page, size, sort);

	    Long accountId = requestDetails.getCurrentAccountId();
	    if (accountId == null) {
	        log.error("Account id not found, unauthorized access.");
	        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
	    }

	    Document sortDocument = new Document();
	    for (String field : mappedSortFields) {
	        sortDocument.append(field, direction == Sort.Direction.ASC ? 1 : -1);
	    }

	    Boolean findDuplicates = "yes".equalsIgnoreCase(duplicate);
	    
	////VoterResponseMongoDTO mongoResult = voterMongoService.getVoters(
////  accountId, voterId, epicNumber, electionId, boothNumberList, familyId, voterFnameEnList, voterLnameEnList,
////  voterFnameL1List, voterFnameL2List, relationFirstNameEnList, relationLastNameEnList, voterHistoryNameList,            
////  partyNameList, religionName, age, minAge, maxAge, includeUnknownAge, genderList, dobFilter,
////  starNumber, description, casteCategoryNameTrimmed, findDuplicates, serialNo, pageable, sortDocument);
//
////Convert MongoDB response to PostgreSQL response format
//VoterResponseDTO result = convertMongoToPostgresResponse(mongoResult);

//	    VoterResponseDTO result = voterService.getVoters(
//	            accountId, voterId, epicNumber, electionId, boothNumberList, familyId, friendId, 
//	            voterFnameEnList, voterLnameEnList, voterFnameL1List, voterFnameL2List, 
//	            voterLnameL1List, voterLnameL2List, relationFirstNameEnList, relationLastNameEnList, 
//	            rlnFnameL1List, rlnFnameL2List, rlnLnameL1List, rlnLnameL2List,
//	            partyNameList, voterHistoryNameList, religionName, age, minAge, maxAge, includeUnknownAge, 
//	            genderList, dobFilter, todayMonth, todayDay, tomorrowMonth, tomorrowDay, starNumber, description, 
//	            categoryNameTrimmed, casteCategoryNameTrimmed, casteNameTrimmed, subCasteNameTrimmed, 
//	            findDuplicates, serialNo, overseas, fatherless, guardian,hasMobileNo, singleVoterFamily, pageable);
	    VoterResponseDTO result = voterService.getVoters(
	    	    accountId, voterId, epicNumber, electionId, boothNumberList, familyId, friendId, 
	    	    voterFnameEnList, voterLnameEnList, voterFnameL1List, voterFnameL2List, 
	    	    voterLnameL1List, voterLnameL2List, relationFirstNameEnList, relationLastNameEnList, 
	    	    rlnFnameL1List, rlnFnameL2List, rlnLnameL1List, rlnLnameL2List,
	    	    partyNameList, voterHistoryNameList, religionNameList, age, minAge, maxAge, includeUnknownAge, 
			genderList, filterToday, filterTomorrow, todayMonth, todayDay, tomorrowMonth, tomorrowDay, customBirthdayMonth, customBirthdayDay,
	    	    starNumber, descriptionList, categoryNameList, casteCategoryNameList, casteNameList, 
	    	    subCasteNameList, findDuplicates, serialNo, overseas, fatherless, guardian, hasMobileNo, 
	    	    mobileNo, singleVoterFamily, pollStatus, isFamily, pageable);	    return new ThedalResponse<>(ThedalSuccess.VOTER_FOUND, result);
	}
	

	@Operation(summary = "Search voters by name", description = "Fuzzy search on voter first/last name. Returns voter details plus gender stats. Supports pagination and optional sorting via sortBy (comma separated: part_number, serial_number, voter_fname_en, voter_lname_en) and order (asc/desc). Defaults to part number ascending (then serial number ascending) when no sort provided.", tags = {"Voter Management" })
	@GetMapping("/election/{electionId}/search")
	public ThedalResponse<VoterResponseDTO> searchVoters(
		@PathVariable("electionId") Long electionId,
		@RequestParam("query") String searchQuery,
		@RequestParam(value = "isFamily", required = false) Boolean isFamily,
		@RequestParam(value = "page", defaultValue = "0") int page,
		@RequestParam(value = "size", defaultValue = "10") int size,
		@RequestParam(value = "sortBy", required = false) String sortBy,
		@RequestParam(value = "order", defaultValue = "asc") String order) {

		if (searchQuery == null || searchQuery.trim().isEmpty()) {
			log.error("Search query cannot be empty");
			throw new ThedalException(ThedalError.INVALID_SEARCH_QUERY, HttpStatus.BAD_REQUEST);
		}

		Long accountId = requestDetails.getCurrentAccountId();
		if (accountId == null) {
			log.error("Account id not found, unauthorized access.");
			throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
		}

		// Default sorting: partNo ASC, then serialNo ASC (deterministic)
		List<String> mappedSortFields = new ArrayList<>();
		if (sortBy != null && !sortBy.trim().isEmpty()) {
			List<String> sortFields = Arrays.stream(sortBy.split(","))
				.map(String::trim)
				.map(String::toLowerCase)
				.collect(Collectors.toList());
			for (String sf : sortFields) {
				if (PART_NO_VARIATIONS.contains(sf)) {
					if (!mappedSortFields.contains("partNo")) mappedSortFields.add("partNo");
				} else if (SERIAL_NO_VARIATIONS.contains(sf)) {
					if (!mappedSortFields.contains("serialNo")) mappedSortFields.add("serialNo");
				} else if (FNAME_VARIATIONS.contains(sf)) {
					if (!mappedSortFields.contains("voterFnameEn")) mappedSortFields.add("voterFnameEn");
				} else if (LNAME_VARIATIONS.contains(sf)) {
					if (!mappedSortFields.contains("voterLnameEn")) mappedSortFields.add("voterLnameEn");
				} else {
					log.error("Invalid sortBy parameter in search: {}", sf);
					throw new ThedalException(ThedalError.INVALID_SORT_FIELD, HttpStatus.BAD_REQUEST);
				}
			}
		}
		if (mappedSortFields.isEmpty()) {
			mappedSortFields.add("partNo");
			mappedSortFields.add("serialNo");
		}

		String orderLower = order == null ? "asc" : order.toLowerCase();
		if (!orderLower.equals("asc") && !orderLower.equals("desc")) {
			log.error("Invalid order parameter: {}. Must be 'asc' or 'desc'", order);
			throw new ThedalException(ThedalError.INVALID_SORT_FIELD, HttpStatus.BAD_REQUEST);
		}
		Sort.Direction direction = orderLower.equals("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;

		Sort sort = Sort.by(direction, mappedSortFields.get(0));
		for (int i = 1; i < mappedSortFields.size(); i++) {
			sort = sort.and(Sort.by(direction, mappedSortFields.get(i)));
		}
		Pageable pageable = PageRequest.of(page, size, sort);

		// For potential Mongo parity / logging (not used directly by current Postgres search service)
		Document sortDocument = new Document();
		int mongoDir = direction == Sort.Direction.ASC ? 1 : -1;
		mappedSortFields.forEach(f -> sortDocument.append(f, mongoDir));

		VoterResponseDTO result = voterService.searchVotersByName(accountId, electionId, searchQuery, isFamily, pageable);
		return new ThedalResponse<>(ThedalSuccess.VOTER_FOUND, result);
	}


	

//	@Operation(summary = "Update Voter", description = "Update an existing voter's details by providing the voter ID and new information.", tags = { "Voter Management" })
//	@PutMapping("/{voterId}/election/{electionId}")
//	public ThedalResponse<VoterUpdateDTO> updateVoter(
//			@PathVariable("voterId") String voterId,
//	        @PathVariable("electionId") Long electionId,
//	        @Valid @RequestBody VoterUpdateDTO voterUpdateDTO
//	        ) {
//		
//	    VoterUpdateDTO updatedVoterDto = voterService.updateVoter(voterId, electionId, voterUpdateDTO);
//	    return new ThedalResponse<>(ThedalSuccess.VOTER_UPDATED_SUCCESSFULLY, updatedVoterDto);
//	}
	@Operation(summary = "Update Voter", description = "Update an existing voter's details by providing the EPIC number and election ID.", tags = { "Voter Management" })
	@PutMapping("/election/{electionId}")
	public ThedalResponse<VoterUpdateDTO> updateVoter(
	        @PathVariable("electionId") Long electionId,
	        @RequestParam(value = "epicNumber", required = true) @NotBlank(message = "EPIC number is mandatory") String epicNumber,
	        @Valid @RequestBody VoterUpdateDTO voterUpdateDTO) {
	    log.info("Controller: Starting voter update for EPIC: {}", epicNumber);
	    
	    try {
	        VoterUpdateDTO updatedVoterDto = voterService.updateVoter(epicNumber, electionId, voterUpdateDTO);
	        log.info("Controller: Service method completed for EPIC: {}", epicNumber);
	        
	        ThedalResponse<VoterUpdateDTO> response = new ThedalResponse<>(ThedalSuccess.VOTER_UPDATED_SUCCESSFULLY, updatedVoterDto);
	        log.info("Controller: Response created for EPIC: {}", epicNumber);
	        
	        return response;
	    } catch (Exception e) {
	        log.error("Controller: Error in voter update for EPIC: {}, error: {}", epicNumber, e.getMessage(), e);
	        throw e;
	    }
	}
	
	
	@Operation(summary = "Delete Voter", 
	          description = "Delete a voter from the system by providing their Epic Number.", 
	          tags = { "Voter Management" })
	@DeleteMapping("/election/{electionId}/singleVoter")
	public ThedalResponse<Void> deleteVoter(
	        @PathVariable("electionId") Long electionId,
	        @RequestParam("epicNumber") String epicNumber) {
	    return voterService.deleteById(epicNumber, electionId);	     
	}
//	@Operation(summary = "Delete Voter", description = "Delete a voter from the system by providing their EPIC number and election ID.", tags = { "Voter Management" })
//	@DeleteMapping("/election/{electionId}/singleVoter")
//	public ThedalResponse<Void> deleteVoter(
//	        @PathVariable("electionId") Long electionId,
//	        //@RequestParam(value = "epicNumber", required = true) @NotBlank(message = "EPIC number is mandatory") String epicNumber
//	        @RequestParam("epicNumber") String epicNumber) {
//	    return voterService.deleteById(epicNumber, electionId);
//	}

	// @Operation(
	// 	    summary = "Map Voter Locations", 
	// 	    description = "Retrieve geographic locations of all voters for mapping purposes.", 
	// 	    tags = { "Voter Management" }
	// 	)
	// 	@GetMapping("/map-location/{electionId}")
	// 	public ThedalResponse<Page<VoterLocationDTO>> mapVoterLocations(
	// 			@PathVariable("electionId") Long electionId,
	// 	        //@RequestParam(required = false) Long electionId,
	// 	        @RequestParam(required = false) Long boothNumber,
	// 	        @RequestParam(defaultValue = "0") int page,
	// 	        @RequestParam(defaultValue = "10") int size
	// 	) {
	// 	    log.info("Retrieving voter locations with electionId: {}, boothNumber: {}, page: {}, size: {}", electionId, boothNumber, page, size);
		    
	// 	    // Validate that electionId is mandatory
	// 	    if (electionId == null) {
	// 	    	throw new ThedalException(ThedalError.VOTER_LOCATIONS_ELECTION_ID, HttpStatus.NOT_FOUND);
	// 	    }

	// 	    ThedalResponse<Page<VoterLocationDTO>> response = voterService.getAllVoterLocations(electionId, boothNumber, page, size);
	// 	    return response;
	// 	}

	@Operation(
    summary = "Get Voter Locations URL",
    description = "Retrieve the URL to the S3 file containing geographic locations of all voters for the specified election.",
    tags = { "Voter Management" }
)
@GetMapping("/map-location/{electionId}")
public ServiceResponse<String> mapVoterLocations(
    @PathVariable("electionId") Long electionId
) {
    log.info("Retrieving voter locations URL for electionId: {}", electionId);
    ServiceResponse<String> response = voterService.getAllVoterLocations(electionId);
    return response;
}

	
	@Operation(summary = "Upload bulk Voter Data", description = "Upload bulk voter data using xlsx or csv files.",
			tags = { "Voter Management" })
	@PostMapping(value = "/{electionId}/upload", consumes = "multipart/form-data")
	public ResponseEntity<ThedalResponse<BulkUploadResponse>> uploadVotersFromXlsxOrCsv(
			@PathVariable("electionId") Long electionId,
			@RequestParam("file") MultipartFile file)throws IOException {
		
		 // Check file size
	    if (file.getSize() > 100 * 1024 * 1024) { // 100 MB
	        throw new ThedalException(ThedalError.FILE_TOO_LARGE, HttpStatus.BAD_REQUEST);
	    }
		
	    ThedalResponse<BulkUploadResponse> response = voterService.uploadVotersFromXlsxOrCsv(file, electionId);
	    return ResponseEntity.ok(response);
	}
	
	
	@Operation(summary = "Third-party voter details", description = "Using EPIC number we will get third-party voter details.",
			tags = { "Voter Management" })
	@GetMapping("/third-party")
    public ResponseEntity<?> getVoterDetails(@RequestParam("epicNumber") String epicNumber) {
        try {
            String voterDetails = voterService.getVoterDetails(epicNumber);
            return ResponseEntity.ok(voterDetails);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error fetching voter details: " + e.getMessage());
        }
    }
	
	@Operation(summary = "Get Bulk Upload Data", description = "Retrieve bulk upload data sorted by date or ID, with optional filtering by status.",
	        tags = { "Voter Management" })
    @GetMapping("/{electionId}/uploads")
    public ResponseEntity<ThedalResponse<Page<BulkUploadDto>>> getBulkUploads(
        @PathVariable("electionId") Long electionId,
        @RequestParam(required = false) String status,
        @RequestParam(defaultValue = "0") Integer page,
        @RequestParam(defaultValue = "10") Integer size,
        @RequestParam(defaultValue = "startTime") String sortBy) {
		
		//Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
		Page<BulkUploadDto> uploads = voterService.getBulkUploads(electionId, status, page, size, sortBy);
        return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.BULK_UPLOAD_DATA_FOUND, uploads));
    }


	@Operation(summary = "Get Bulk Upload Status", description = "Retrieve the status of a bulk upload using its ID.",
	        tags = { "Voter Management" })
	@GetMapping("/uploads/{bulkUploadId}/status")
	public ResponseEntity<BulkUploadStatusDto> getBulkUploadStatus(
			@PathVariable("bulkUploadId") Long bulkUploadId,
			@RequestParam("electionId") Long electionId) {
	    BulkUploadStatusDto statusDto = voterService.getBulkUploadStatus(bulkUploadId, electionId);
	    return ResponseEntity.ok(statusDto);
	}

//	@Operation(summary = "Update Voter Voting Status", 
//	           description = "Marks a voter as voted or not voted based on election ID and voter ID.", 
//	           tags = { "Voter Management" })
//	@PostMapping("/election/{electionId}/{epicNumber}/hasVoted")
//	public ThedalResponse<Map<String, Object>> updateVoterVotingStatus(
//	        @PathVariable Long electionId,
//	        @PathVariable String epicNumber,
//	        @RequestBody VoterVotingRequest request) {  
//
//	    return voterService.updateVoterVotingStatus(electionId, epicNumber, request);
//	}
	@Operation(summary = "Update Voter Voting Status", 
	           description = "Marks a voter as voted or not voted based on election ID and voter ID.", 
	           tags = { "Voter Management" })
	@PostMapping("/election/{electionId}/hasVoted")
	public ThedalResponse<Map<String, Object>> updateVoterVotingStatus(
	        @PathVariable Long electionId,
	        //@PathVariable String epicNumber,
	        @RequestParam("epicNumber") String epicNumber,
	        @RequestBody VoterVotingRequest request) {  

	    return voterService.updateVoterVotingStatus(electionId, epicNumber, request);
	}

	@Operation(summary = "Mark multiple voters as voted or not voted",  
	           description = "Marks multiple voters as voted or not voted based on their election ID and voter IDs.", 
	           tags = { "Voter Management" })
	@PostMapping("/election/{electionId}/33vote")
	public ThedalResponse<BulkVoterUpdateResponse> markMultipleVotersAsVoted(
	        @PathVariable Long electionId,
	        @RequestBody List<VoterVoteRequest> voterVoteRequests) {

	    return voterService.markMultipleVotersAsVoted(electionId, voterVoteRequests);
	}
	


	
	@Operation(summary = "Update Voter Image by ID", description = "Update a voter image by ID.", tags = { "Voter Management" })
	//@PutMapping(value = "{electionId}/{epicNumber}/voter-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PutMapping(value = "{electionId}/voter-image", 
    consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ThedalResponse<String>> uploadVoterImage(
			@RequestParam("epicNumber") String epicNumber,
	        @PathVariable Long electionId,
	        @RequestParam("file") MultipartFile file) {
	    return voterService.updateVoterImage(epicNumber, electionId, file);
	}
	
	@Operation(summary = "Bulk Upload Voter Photos", description = "Upload multiple voter photos from a ZIP file. Photo filenames should match EPIC numbers (e.g., ABC123456.jpg).", tags = { "Voter Management" })
	@PostMapping(value = "{electionId}/bulk-photo-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ThedalResponse<BulkPhotoUploadResponse>> uploadVoterPhotos(
			@PathVariable Long electionId,
			@RequestParam("zipFile") MultipartFile zipFile) {
		
		// Validate ZIP file
		if (zipFile.isEmpty()) {
			throw new ThedalException(ThedalError.FILE_EMPTY, HttpStatus.BAD_REQUEST);
		}
		
		String contentType = zipFile.getContentType();
		if (!"application/zip".equals(contentType) && !"application/x-zip-compressed".equals(contentType)) {
			throw new ThedalException(ThedalError.INVALID_FILE_FORMAT, HttpStatus.BAD_REQUEST);
		}
		
		// Check file size using configurable limit
		if (zipFile.getSize() > bulkPhotoZipMaxBytes) {
			throw new ThedalException(ThedalError.FILE_TOO_LARGE, HttpStatus.BAD_REQUEST,
				"Max allowed: " + (bulkPhotoZipMaxBytes / (1024 * 1024)) + " MB");
		}
		
		ThedalResponse<BulkPhotoUploadResponse> response = voterService.uploadVoterPhotosFromZip(zipFile, electionId);
		return ResponseEntity.ok(response);
	}
	
	@Operation(summary = "Get Bulk Photo Upload Status", description = "Get the status of a bulk photo upload operation.", tags = { "Voter Management" })
	@GetMapping("bulk-photo-upload/{bulkUploadId}/status")
	public ResponseEntity<ThedalResponse<BulkPhotoUploadEntity>> getBulkPhotoUploadStatus(
			@PathVariable Long bulkUploadId) {
		ThedalResponse<BulkPhotoUploadEntity> response = voterService.getBulkPhotoUploadStatus(bulkUploadId);
		return ResponseEntity.ok(response);
	}
	
	@Operation(summary = "Get All Bulk Photo Uploads", description = "Get all bulk photo upload operations for an election.", tags = { "Voter Management" })
	@GetMapping("{electionId}/bulk-photo-uploads")
	public ResponseEntity<ThedalResponse<List<BulkPhotoUploadEntity>>> getAllBulkPhotoUploads(
			@PathVariable Long electionId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size) {
		ThedalResponse<List<BulkPhotoUploadEntity>> response = voterService.getAllBulkPhotoUploads(electionId, page, size);
		return ResponseEntity.ok(response);
	}
	
	// =========================================================================
	// DIRECT S3 UPLOAD ENDPOINTS (NEW APPROACH)
	// =========================================================================
	
	@Operation(summary = "Upload Photos Directly to S3 from ZIP", 
	          description = "Uploads photos from ZIP directly to S3 with original filenames preserved. " +
	                       "No file size limit on ZIP. Files uploaded as-is with their original names. " +
	                       "Example: If ZIP contains 'IPR1840586.png', it uploads to S3 as 'IPR1840586.png'", 
	          tags = { "Voter Management" })
	@PostMapping(value = "{electionId}/upload-photos-direct-s3", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ThedalResponse<VoterPhotoDirectS3Service.DirectUploadResult>> uploadPhotosDirectToS3(
			@PathVariable Long electionId,
			@RequestParam("file") MultipartFile zipFile) {
		
		Long accountId = requestDetails.getCurrentAccountId();
		validateElectionOwnership(electionId, accountId);
		
		log.info("Direct S3 upload request for election {} from file: {}", 
		        electionId, zipFile.getOriginalFilename());
		
		// Validate file type
		String filename = zipFile.getOriginalFilename();
		if (filename == null || !filename.toLowerCase().endsWith(".zip")) {
			return ResponseEntity.badRequest()
			    .body(new ThedalResponse<>(ThedalError.INVALID_FILE_FORMAT));
		}
		
		try {
			VoterPhotoDirectS3Service.DirectUploadResult result = 
			    voterPhotoDirectS3Service.uploadPhotosDirectlyToS3(zipFile, electionId);
			
			return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.SUCCESS, result));
			
		} catch (Exception e) {
			log.error("Error uploading photos directly to S3: {}", e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
			    .body(new ThedalResponse<>(ThedalError.INTERNAL_SERVER_ERROR));
		}
	}
	
	@Operation(summary = "Update Voter Photo URLs from S3 Files", 
	          description = "Updates voter photo URLs by matching EPIC numbers with uploaded filenames in S3. " +
	                       "Supports multiple filename patterns: " +
	                       "1. {EPIC}.png (e.g., IPR1840586.png) - exact match" +
	                       "2. voter_photo_{EPIC}.ext" +
	                       "3. {EPIC}_photo.ext" +
	                       "4. Any filename containing the EPIC number. " +
	                       "Constructs full S3 URL: https://bucket.s3.region.amazonaws.com/folder/{filename}", 
	          tags = { "Voter Management" })
	@PostMapping("{electionId}/update-photo-urls-from-s3")
	public ResponseEntity<ThedalResponse<String>> updatePhotoUrlsFromS3Files(
			@PathVariable Long electionId,
			@RequestParam(defaultValue = ".png") String fileExtension,
			@RequestParam(defaultValue = "true") boolean useSimplePattern) {
		
		Long accountId = requestDetails.getCurrentAccountId();
		validateElectionOwnership(electionId, accountId);
		
		log.info("Photo URL update request for election {} with extension {} and pattern simple={}", 
		        electionId, fileExtension, useSimplePattern);
		
		try {
			// This runs asynchronously
			java.util.concurrent.CompletableFuture<VoterPhotoUrlUpdateService.PhotoUrlUpdateResult> future = 
			    voterPhotoUrlUpdateService.updateVoterPhotoUrls(electionId, accountId, fileExtension, useSimplePattern);
			
			String message = String.format(
			    "Photo URL update started in background for election %d. " +
			    "URLs will be constructed using pattern: %s%s. " +
			    "This will update all voters without photos.",
			    electionId, 
			    useSimplePattern ? "{EPIC}" : "voter_photo_{EPIC}_*",
			    fileExtension
			);
			
			return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.SUCCESS, message));
			
		} catch (Exception e) {
			log.error("Error starting photo URL update: {}", e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
			    .body(new ThedalResponse<>(ThedalError.INTERNAL_SERVER_ERROR));
		}
	}
	
	@Operation(summary = "Remove Voter Image by epic number", description = "Remove a voter's image by epic number.", tags = { "Voter Management" })
	@DeleteMapping(value = "{electionId}/voter-image")
	public ResponseEntity<ThedalResponse<String>> removeVoterImage(
	        @RequestParam("epicNumber") String epicNumber,
	        @PathVariable Long electionId) {
	    return voterService.removeVoterImage(epicNumber, electionId);
	}

//	@Operation(summary = "Post mapping for familyId to Voter", description = "Using these api we can map familyId to voters, every voter for specific familyId", tags = { "Voter Management" })
//	@PostMapping("/family-mapping/{electionId}/{epicNumber}")
//	public ThedalResponse<String> mapFamily(
//	        @PathVariable("electionId") Long electionId,
//	        @PathVariable("epicNumber") String epicNumber,
//	        @RequestBody(required = false) EpicNumberRequest request) {
//
//	    // Extract familyId and otherEpicNumber from the request
//	    UUID familyId = request != null ? request.getFamilyId() : null;
//	    String otherEpicNumber = request != null ? request.getOtherEpicNumber() : null;
//
//	    return voterService.mapFamily(electionId, epicNumber, otherEpicNumber, familyId);
//	}
	@Operation(summary = "Post mapping for familyId to Voter", description = "Using these api we can map familyId to voters, every voter for specific familyId", tags = { "Voter Management" })
	@PostMapping("/family-mapping/election/{electionId}")
	public ThedalResponse<String> mapFamily(
	        @PathVariable("electionId") Long electionId,
	        @RequestParam(value = "epicNumber", required = true) @NotBlank(message = "EPIC number is mandatory") String epicNumber,
	        @RequestBody(required = false) EpicNumberRequest request) {

	    // Extract familyId and otherEpicNumber from the request
	    UUID familyId = request != null ? request.getFamilyId() : null;
	    String otherEpicNumber = request != null ? request.getOtherEpicNumber() : null;

	    return voterService.mapFamily(electionId, epicNumber, otherEpicNumber, familyId);
	}

//	@Operation(summary = "delete api for familyId to mapping Voter", description = "Using these api we can remove familyId to voters", tags = { "Voter Management" })
//	@DeleteMapping("/family-mapping/{electionId}/{epicNumber}")
//	public ThedalResponse<String> deleteFamilyId(
//	        @PathVariable("electionId") Long electionId,
//	        @PathVariable("epicNumber") String epicNumber) {
//	    return voterService.deleteFamilyId(electionId, epicNumber);
//	}
	@Operation(summary = "delete api for familyId to mapping Voter", description = "Using these api we can remove familyId to voters", tags = { "Voter Management" })
	@DeleteMapping("/family-mapping/election/{electionId}")
	public ThedalResponse<String> deleteFamilyId(
	        @PathVariable("electionId") Long electionId,
	        @RequestParam(value = "epicNumber", required = true) @NotBlank(message = "EPIC number is mandatory") String epicNumber) {
	    return voterService.deleteFamilyId(electionId, epicNumber);
	}

	@Operation(summary = "Delete All Voters of Election", description = "Delete all voters for an election or specific voters by EPIC numbers provided as an array.", tags = { "Voter Management" })
	@DeleteMapping("/election/{electionId}")
	public ThedalResponse<Object> deleteVoters(
	        @PathVariable("electionId") Long electionId,
	        //@RequestParam(value = "epicNumbers", required = false) String epicNumbers
	        @RequestParam(value = "epicNumbers", required = false) List<String> epicNumbers) {

//		// If epicNumbers is null, convert to empty list
//	    List<String> epicNumberList = (epicNumbers != null && !epicNumbers.isEmpty()) 
//	        ? epicNumbers
//	        : Collections.emptyList();
		 // Validate electionId
	    if (electionId == null || electionId <= 0) {
	        throw new ThedalException(ThedalError.INVALID_INPUT, HttpStatus.NOT_FOUND);
	    }

	    // Convert null to empty list
	    List<String> epicNumberList = (epicNumbers != null) ? epicNumbers : Collections.emptyList();
	    
		
	   
	    return voterService.deleteVoters(electionId, epicNumberList);
	}




///////////////////////////	
	
//	@Operation(summary = "Export Voter Data to Excel", description = "Export voter data to an Excel file and store it in AWS S3.", tags = { "Voter Management" })
//	@PostMapping("/{electionId}/export")
//	public ResponseEntity<ThedalResponse<VoterExportResponse>> exportVotersToExcel(
//	        @PathVariable("electionId") Long electionId,
//	        @RequestParam(value = "limit", required = false) Integer limit) {
//
//	    Long accountId = requestDetails.getCurrentAccountId();
//	    validateElectionOwnership(electionId, accountId);
//	    VoterExportResponse response = voterService.initiateVoterExport(accountId, electionId, limit);
//	    return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.VOTER_EXPORT_INITIATED, response));
//	}
	@Operation(summary = "Export Voter Data to Excel with Comprehensive Filters", 
		    description = "Export voter data to Excel with comprehensive filters (same as GET voters API) and store in AWS S3.",
		    tags = {"Voter Management"})
		@PostMapping("/{electionId}/export")
		public ResponseEntity<ThedalResponse<VoterExportResponse>> exportVotersToExcel(
		        @PathVariable("electionId") Long electionId,
		        @RequestParam(value = "voter-id", required = false) String voterId,
		        @RequestParam(value = "epic-number", required = false) String epicNumber,
		        @RequestParam(value = "booth-number", required = false) String boothNumbers,
		        @RequestParam(value = "partNos", required = false) String partNos,
		        @RequestParam(value = "family-id", required = false) UUID familyId,
		        @RequestParam(value = "friend-id", required = false) UUID friendId,
		        @RequestParam(value = "voterName", required = false) String voterName,
		        @RequestParam(value = "voterFirstName", required = false) String voterFirstName,
		        @RequestParam(value = "voterLastName", required = false) String voterLastName,
		        @RequestParam(value = "voterFnameEn", required = false) String voterFnameEn,
		        @RequestParam(value = "voterLnameEn", required = false) String voterLnameEn,
		        @RequestParam(value = "voterFnameL1", required = false) String voterFnameL1,
		        @RequestParam(value = "voterFnameL2", required = false) String voterFnameL2,
		        @RequestParam(value = "voterLnameL1", required = false) String voterLnameL1,
		        @RequestParam(value = "voterLnameL2", required = false) String voterLnameL2,
		        @RequestParam(value = "relationName", required = false) String relationName,
		        @RequestParam(value = "relationFirstName", required = false) String relationFirstName,
		        @RequestParam(value = "relationLastName", required = false) String relationLastName,
		        @RequestParam(value = "relationFirstNameEn", required = false) String relationFirstNameEn,
		        @RequestParam(value = "relationLastNameEn", required = false) String relationLastNameEn,
		        @RequestParam(value = "party", required = false) String partyName,
		        @RequestParam(value = "religion", required = false) String religionName,
		        @RequestParam(value = "voterHistoryName", required = false) String voterHistoryName,
		        @RequestParam(value = "age", required = false) Integer age,
		        @RequestParam(value = "minAge", required = false) Integer minAge,
		        @RequestParam(value = "maxAge", required = false) Integer maxAge,
				@RequestParam(value = "includeUnknownAge", required = false) Boolean includeUnknownAge,
		        @RequestParam(value = "gender", required = false) String gender,
		        @RequestParam(value = "hasDob", required = false) String hasDob,
		        @RequestParam(value = "starNumber", required = false) Boolean starNumber,
		        @RequestParam(value = "catagoryDescription", required = false) String description,
		        @RequestParam(value = "categoryName", required = false) String categoryName,
		        @RequestParam(value = "casteCategoryName", required = false) String casteCategoryName,
		        @RequestParam(value = "casteName", required = false) String casteName,
		        @RequestParam(value = "subCaste", required = false) String subCaste,
		        @RequestParam(value = "duplicate", required = false) String duplicate,
		        @RequestParam(value = "serial-no", required = false) Long serialNo,
		        @RequestParam(value = "overseas", required = false) Boolean overseas,
		        @RequestParam(value = "fatherless", required = false) Boolean fatherless,
		        @RequestParam(value = "guardian", required = false) Boolean guardian,
		        @RequestParam(value = "birthdayMonth", required = false) Integer birthdayMonth,
		        @RequestParam(value = "birthdayDay", required = false) Integer birthdayDay,
	        @RequestParam(value = "hasMobileNo", required = false) Boolean hasMobileNo,
	        @RequestParam(value = "mobileNo", required = false) String mobileNo,
	        @RequestParam(value = "singleVoterFamily", required = false) Boolean singleVoterFamily,
	        @RequestParam(value = "columns", required = false) List<String> columns,
	        @RequestParam(value = "limit", required = false) Integer limit) {	    Long accountId = requestDetails.getCurrentAccountId();
	    Long userId = requestDetails.getCurrentUserId();
	    validateElectionOwnership(electionId, accountId);
	    
		// Convert booth numbers (or fallback to partNos) string to list if provided
		List<Integer> boothNumberList = null;
		String numbersToParse = null;
		if (boothNumbers != null && !boothNumbers.trim().isEmpty()) {
		    numbersToParse = boothNumbers;
		} else if (partNos != null && !partNos.trim().isEmpty()) {
		    numbersToParse = partNos; // backward-compat: treat partNos as booth numbers
		}
		if (numbersToParse != null) {
		    try {
		        boothNumberList = Arrays.stream(numbersToParse.split(","))
		                .map(String::trim)
		                .filter(s -> !s.isEmpty())
		                .map(Integer::parseInt)
		                .collect(Collectors.toList());
		    } catch (NumberFormatException e) {
		        throw new ThedalException(ThedalError.INVALID_REQUEST, HttpStatus.BAD_REQUEST);
		    }
		}		    // Basic validation - same as existing export
		    if (gender != null && !gender.matches("(?i)^(male|female|other)$")) {
		        throw new ThedalException(ThedalError.INVALID_GENDER, HttpStatus.BAD_REQUEST);
		    }
		    if (minAge != null && minAge < 0) {
		        throw new ThedalException(ThedalError.INVALID_AGE_RANGE, HttpStatus.BAD_REQUEST);
		    }
		    if (maxAge != null && maxAge < 0) {
		        throw new ThedalException(ThedalError.INVALID_AGE_RANGE, HttpStatus.BAD_REQUEST);
		    }
		    if (minAge != null && maxAge != null && minAge > maxAge) {
		        throw new ThedalException(ThedalError.INVALID_AGE_RANGE, HttpStatus.BAD_REQUEST);
		    }
		    if (Boolean.TRUE.equals(fatherless) && Boolean.TRUE.equals(guardian)) {
		        throw new ThedalException(ThedalError.INVALID_FILTER_COMBINATION, HttpStatus.BAD_REQUEST);
		    }

		    // Trim category parameters to match GET voters API behavior
		    String categoryNameTrimmed = null;
		    if (categoryName != null && !categoryName.trim().isEmpty()) {
		        categoryNameTrimmed = categoryName.trim();
		        log.debug("Export: Parsed categoryName: {}", categoryNameTrimmed);
		    }
		    
		    String casteCategoryNameTrimmed = null;
		    if (casteCategoryName != null && !casteCategoryName.trim().isEmpty()) {
		        casteCategoryNameTrimmed = casteCategoryName.trim();
		        log.debug("Export: Parsed casteCategoryName: {}", casteCategoryNameTrimmed);
		    }
		    
		    String casteNameTrimmed = null;
		    if (casteName != null && !casteName.trim().isEmpty()) {
		        casteNameTrimmed = casteName.trim();
		        log.debug("Export: Parsed casteName: {}", casteNameTrimmed);
		    }
		    
		    String subCasteTrimmed = null;
		    if (subCaste != null && !subCaste.trim().isEmpty()) {
		        subCasteTrimmed = subCaste.trim();
		        log.debug("Export: Parsed subCaste: {}", subCasteTrimmed);
		    }

	    VoterExportResponse response = voterService.initiateVoterExportWithFilters(
	        accountId, userId, electionId, voterId, epicNumber, boothNumberList, familyId, friendId,
	        voterName, voterFirstName, voterLastName, voterFnameEn, voterLnameEn,
	        voterFnameL1, voterFnameL2, voterLnameL1, voterLnameL2,
	        relationName, relationFirstName, relationLastName, relationFirstNameEn, relationLastNameEn,
	        partyName, religionName, voterHistoryName, age, minAge, maxAge, includeUnknownAge,
	        gender, hasDob, starNumber, description, categoryNameTrimmed, casteCategoryNameTrimmed,
	        casteNameTrimmed, subCasteTrimmed, duplicate, serialNo, overseas, fatherless, guardian,
	        birthdayMonth, birthdayDay, hasMobileNo, mobileNo, singleVoterFamily, columns, limit
	    );
	    return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.VOTER_EXPORT_INITIATED, response));
	}	
//	@Operation(summary = "Check Export Job Status", description = "Check the status of a voter data export job.",
//	        tags = { "Voter Management" })
//	@GetMapping("/export/status/{jobId}")
//	public ResponseEntity<ThedalResponse<VoterExportStatusResponse>> checkExportJobStatus(
//	        @PathVariable("jobId") Long jobId) {
//
//	    VoterExportStatusResponse response = voterService.getExportJobStatus(jobId);
//	    return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.VOTER_EXPORT_STATUS_FETCHED, response));
//	}
	@Operation(summary = "Check Export Job Status", description = "Check the status of a voter data export job.",
    tags = { "Voter Management" })
	@GetMapping("/{electionId}/export/status/{jobId}")
    public ResponseEntity<ThedalResponse<VoterExportStatusResponse>> checkExportJobStatus(
            @PathVariable("electionId") Long electionId,
            @PathVariable("jobId") Long jobId) {
        
        Long accountId = requestDetails.getCurrentAccountId();
        
        VoterExportStatusResponse response = voterService.getExportJobStatus(accountId, electionId, jobId);
        return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.VOTER_EXPORT_STATUS_FETCHED, response));
    }
	
	@Operation(summary = "Check Export Job Status (without electionId)", 
	    description = "Check the status of a voter data export job using only jobId.",
	    tags = { "Voter Management" })
	@GetMapping("/export/status/{jobId}")
	public ResponseEntity<ThedalResponse<VoterExportStatusResponse>> checkExportJobStatusByJobId(
	        @PathVariable("jobId") Long jobId) {
	    
	    Long accountId = requestDetails.getCurrentAccountId();
	    
	    VoterExportStatusResponse response = voterService.getExportJobStatusByJobId(accountId, jobId);
	    return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.VOTER_EXPORT_STATUS_FETCHED, response));
	}
	
	@Operation(summary = "Download Export File", description = "Download the completed voter export file.",
            tags = { "Voter Management" })
    @GetMapping("/{electionId}/export/download/{jobId}")
    public ResponseEntity<Resource> downloadExportFile(
            @PathVariable("electionId") Long electionId,
            @PathVariable("jobId") Long jobId) {
        
        Long accountId = requestDetails.getCurrentAccountId();
        
        return voterService.downloadExportFile(jobId, accountId, electionId);
    }
	
//	@Operation(summary = "Get all export jobs for election", 
//	        description = "Retrieve all voter data export jobs for a specific election",
//	        tags = {"Voter Management"})
//	    @GetMapping("/{electionId}/exports")
//	    public ResponseEntity<ThedalResponse<List<VoterExportStatusResponse>>> getExportJobsForElection(
//	            @PathVariable("electionId") Long electionId,
//	            @RequestParam(value = "page", required = false) Integer page,
//	            @RequestParam(value = "size", required = false) Integer size) {
//	        
//	        Long accountId = requestDetails.getCurrentAccountId();
//	        
//	        if (page != null && size != null) {
//	            // Paginated version
//	            Page<VoterExportStatusResponse> response = voterService
//	                .getExportJobsByElectionPaginated(accountId, electionId, 
//	                    PageRequest.of(page, size, Sort.by("timeStarted").descending()));
//	            return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.EXPORT_JOBS_FETCHED, response.getContent()));
//	        } else {
//	            // Non-paginated version
//	            List<VoterExportStatusResponse> response = voterService
//	                .getExportJobsByElection(accountId, electionId);
//	            return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.EXPORT_JOBS_FETCHED, response));
//	        }
//	    }
	@Operation(summary = "List Voter Export Jobs", 
	          description = "Retrieve voter export jobs for an election, filtered by status and date range (default: past 30 days).",
	          tags = {"Voter Management"})
	@GetMapping("/{electionId}/exports")
	public ResponseEntity<ThedalResponse<VoterExportJobsResponse>> getExportJobsForElection(
	        @PathVariable("electionId") Long electionId,
	        @RequestParam(value = "page", required = false) Integer page,
	        @RequestParam(value = "size", required = false) Integer size,
	        @RequestParam(value = "status", required = false) String status,
	        @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
	        @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
//	        @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
//	        @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

	        
	    Long accountId = requestDetails.getCurrentAccountId();
	    
	    LocalDateTime defaultStartDate = startDate != null ? startDate : LocalDateTime.now().minusDays(30);
	    LocalDateTime defaultEndDate = endDate != null ? endDate : LocalDateTime.now();
//	    LocalDateTime defaultStartDate = startDate != null ? startDate.atStartOfDay() : LocalDateTime.now().minusDays(30);
//	    LocalDateTime defaultEndDate = endDate != null ? endDate.atTime(LocalTime.MAX) : LocalDateTime.now();

	    VoterExportJobsResponse response;
	    if (page != null && size != null) {
	        // Paginated version
	    	Page<VoterExportStatusResponse> pageResult = voterService.getExportJobsByElectionPaginated(
	            accountId, electionId, status, defaultStartDate, defaultEndDate,
	            PageRequest.of(page, size, Sort.by("timeStarted").descending()));
	       // return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.EXPORT_JOBS_FETCHED, response.getContent()));
	    	response = new VoterExportJobsResponse(pageResult.getContent(), pageResult.getTotalElements());
	    } 
	    else {
            // Non-paginated version
            response = (VoterExportJobsResponse) voterService.getExportJobsByElection(
                accountId, electionId, status, defaultStartDate, defaultEndDate);
        }

        return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.EXPORT_JOBS_FETCHED, response));
    }
	
	
	@Operation(summary = "Delete Voter Export Job", 
	          description = "Delete a voter export job and its associated S3 file.",
	          tags = {"Voter Management"})
	@DeleteMapping("/{electionId}/export/{jobId}")
	public ResponseEntity<ThedalResponse<Void>> deleteExportJob(
	        @PathVariable("electionId") Long electionId,
	        @PathVariable("jobId") Long jobId) {
	    
	    Long accountId = requestDetails.getCurrentAccountId();
	    voterService.deleteExportJob(accountId, electionId, jobId);
	    return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.VOTER_EXPORT_DELETED, null));
	}
	

////////-----------------------	
	@Operation(
		    summary = "Generate Voter Data Excel",
		    description = "Generate an Excel file containing voter data with an optional limit.",
		    tags = { "Voter Management" }
		)
		@GetMapping("/{electionId}/download")
		public ResponseEntity<ByteArrayResource> generateVoterExcel(
		        @PathVariable("electionId") Long electionId,
		        @RequestParam(value = "columns", required = false) List<String> columns,
		        @RequestParam(value = "limit", required = false) Integer limit) {

		    Long accountId = requestDetails.getCurrentAccountId();
		    validateElectionOwnership(electionId, accountId);
		    // Fetch total voters count
		    long totalVoters = voterRepository.countByElectionIdAndAccountId(electionId, accountId);

		    // Handle case where no voters exist
		    if (totalVoters == 0) {
		        throw new ThedalException(ThedalError.VOTER_NOT_FOUND, HttpStatus.NOT_FOUND);
		    }

		    // If limit is provided and exceeds the available voters, throw an error
		    if (limit != null && limit > totalVoters) {
		        throw new ThedalException(ThedalError.EXCEEDS_VOTER_LIMIT, HttpStatus.BAD_REQUEST,
		        		"Requested limit (" + limit + ") exceeds the available voters (" + totalVoters + ")");
		    }

		    // Fetch voters with applied limit (or all if limit is not provided)
		    int fetchLimit = (limit != null) ? limit : (int) totalVoters;
		    Pageable pageable = PageRequest.of(0, fetchLimit);
		    List<VoterEntity> voters = voterRepository.findByElectionIdAndAccountIdLimited(electionId, accountId, pageable);

		    // Generate Excel file with selective columns
		    ByteArrayResource resource = generateExcelFileAsResource(voters, columns);
		    
		    ThedalResponse<String> successResponse = new ThedalResponse<>(ThedalSuccess.EXPORT_SUCCESS,
		            "Voter data Excel generated successfully for election ID " + electionId + ". Total voters: " + voters.size());

		    return ResponseEntity.ok()
		           .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=voter_export.xlsx")
		           .contentType(MediaType.APPLICATION_OCTET_STREAM)
		           .contentLength(resource.contentLength())
		           .body(resource);
		}

	private ByteArrayResource generateExcelFileAsResource(List<VoterEntity> voters, List<String> columns) {
	    // Validate and prepare columns
	    List<String> selectedColumns = VoterColumnMapper.validateAndFilterFields(columns);
	    boolean useSelectiveExport = columns != null && !columns.isEmpty();
	    
	    try (Workbook workbook = new XSSFWorkbook()) {
	        workbook.createSheet("HeaderExplanation"); // Empty Sheet 1
	        org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Voters"); // Sheet 2 with voter data

	        // Create header row (selective or full)
	        Row headerRow = sheet.createRow(0);
	        if (useSelectiveExport) {
	            VoterExcelHeader.createSelectiveHeaderRow(headerRow, selectedColumns);
	        } else {
	            VoterExcelHeader.createHeaderRow(headerRow);
	        }

	        // Populate voter data rows
	        int rowNum = 1;
	        for (VoterEntity voter : voters) {
	            Row row = sheet.createRow(rowNum++);
	            if (useSelectiveExport) {
	                VoterExcelDataRow.populateSelectiveDataRow(row, voter, selectedColumns);
	            } else {
	                VoterExcelDataRow.populateDataRow(row, voter);
	            }
	        }

	        // Write to a byte array output stream
	        ByteArrayOutputStream out = new ByteArrayOutputStream();
	        workbook.write(out);
	        return new ByteArrayResource(out.toByteArray());
	    } catch (IOException e) {
	        throw new ThedalException(ThedalError.EXPORT_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
	    }
	}

	@Operation(summary = "Get Bulk Upload Errors", description = "Retrieve errors for a specific bulk upload using its ID.", tags = { "Voter Management" })
	@GetMapping("/uploads/{bulkUploadId}/errors")
	public ResponseEntity<List<BulkUploadErrorResponseDTO>> getBulkUploadErrors(
	        @PathVariable("bulkUploadId") Long bulkUploadId,
	        @RequestParam("electionId") Long electionId) {
	    
	    List<BulkUploadErrorResponseDTO> errors = voterService.getBulkUploadErrors(bulkUploadId, electionId);
	    return ResponseEntity.ok(errors);
	}
	
	@Operation(summary = "Upload Voter Video by EPIC Number", description = "Upload a small video file for a voter by EPIC number.", tags = { "Voter Management" })
	@PostMapping(value = "{electionId}/voter-video", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ThedalResponse<String>> uploadVoterVideo(
	        @RequestParam("epicNumber") String epicNumber,
	        @PathVariable Long electionId,
	        @RequestParam("file") MultipartFile file) {
	    return voterService.uploadVoterVideo(epicNumber, electionId, file);
	}
	
	@Operation(
		    summary = "Request OTP for Voter", 
		    description = "Sends an OTP to the voter’s mobile number based on electionId and mobileNo from the request body.",
		    tags = {"Voter Management"}
		)
		@PostMapping("/election/{electionId}/invoke")
		public ResponseEntity<Response<String>> requestVoterOtp(
		       @PathVariable("electionId") Long electionId,
		       @Valid @RequestBody VoterOtpRequestDto request) {
		    return voterService.sendVoterOtp(electionId, request.getMobileNo(), request);
		}
	
	@Operation(
		    summary = "Verify OTP for Voter",
		    description = "Verifies the OTP and updates mobile verification status based on electionId and mobileNo from request body.",
		    tags = {"Voter Management"}
		)
		@PostMapping("/election/{electionId}/verify")
		public ResponseEntity<Response<VoterDTO>> verifyVoterOtp(
		        @PathVariable("electionId") Long electionId,
		        @Valid @RequestBody VoterOtpVerifyDto request) {
		    return voterService.verifyVoterOtp(electionId, request.getMobileNo(), request);
		}

	@Operation(summary = "Retrieve voters grouped by family",
	          description = "Fetches voters grouped by familyId for a given election. Optionally filters by booth-number. Returns an array of families, each containing an array of family members.",
	          tags = {"Voter Management"})
	@GetMapping("/election/{electionId}/families")
	public ThedalResponse<FamilyResponseDTO> getFamilyVoters(
	        @PathVariable("electionId") Long electionId,
	        @RequestParam(value = "booth-number", required = false) String boothNumbers,
	        @RequestParam(value = "page", defaultValue = "0") int page,
	        @RequestParam(value = "size", defaultValue = "10") int size,
	        @RequestParam(value = "sortBy", defaultValue = "part_number,serial_number") String sortBy,
	        @RequestParam(value = "order", defaultValue = "asc") String order) {

	    // Validate and map sortBy fields
	    List<String> sortFields = Arrays.stream(sortBy.split(","))
	            .map(String::trim)
	            .map(String::toLowerCase)
	            .collect(Collectors.toList());

	    List<String> mappedSortFields = new ArrayList<>();
	    for (String sortField : sortFields) {
	        if (PART_NO_VARIATIONS.contains(sortField)) {
	            mappedSortFields.add("partNo");
	        } else if (SERIAL_NO_VARIATIONS.contains(sortField)) {
	            mappedSortFields.add("serialNo");
	        } else if (FNAME_VARIATIONS.contains(sortField)) {
	            mappedSortFields.add("voterFnameEn");
	        } else if (LNAME_VARIATIONS.contains(sortField)) {
	            mappedSortFields.add("voterLnameEn");
	        } else {
	            log.error("Invalid sortBy parameter: {}. Must be one of: part_number, partNo, partNumber, serial_number, serialNo, serialNumber, voter_fname_en, voter_lname_en (case-insensitive)", sortField);
	            throw new ThedalException(ThedalError.INVALID_SORT_FIELD, HttpStatus.BAD_REQUEST);
	        }
	    }

	    // Validate order
	    String orderLower = order.toLowerCase();
	    if (!orderLower.equals("asc") && !orderLower.equals("desc")) {
	        log.error("Invalid order parameter: {}. Must be 'asc' or 'desc' (case-insensitive)", order);
	        throw new ThedalException(ThedalError.INVALID_SORT_FIELD, HttpStatus.BAD_REQUEST);
	    }

	    // Parse booth numbers
//	    List<Integer> boothNumberList = null;
//	    if (boothNumbers != null && !boothNumbers.isEmpty()) {
//	        try {
//	            boothNumberList = Arrays.stream(boothNumbers.split(","))
//	                    .map(String::trim)
//	                    .map(Integer::valueOf)
//	                    .collect(Collectors.toList());
//	        } catch (NumberFormatException e) {
//	            log.error("Invalid booth-number format: {}", boothNumbers);
//	            throw new ThedalException(ThedalError.INVALID_BOOTH_NUMBER_FORMAT, HttpStatus.BAD_REQUEST);
//	        }
//	    }
	    List<Integer> boothNumberList = null;
	    if (boothNumbers != null && !boothNumbers.isEmpty()) {
	        try {
	            boothNumberList = Arrays.stream(boothNumbers.split(","))
	                    .map(Integer::parseInt)
	                    .collect(Collectors.toList());
	            log.debug("Parsed boothNumbers: {}", boothNumberList);
	        } catch (NumberFormatException e) {
	            log.error("Invalid booth-number format: {}", boothNumbers);
	            throw new ThedalException(ThedalError.INVALID_REQUEST, HttpStatus.BAD_REQUEST);
	        }
	    }
	    
	    // Create pageable
	    Sort.Direction direction = orderLower.equals("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
	    Sort sort = Sort.by(direction, mappedSortFields.toArray(new String[0]));
	    Pageable pageable = PageRequest.of(page, size, sort);

	    // Get accountId
	    Long accountId = requestDetails.getCurrentAccountId();
	    if (accountId == null) {
	        log.error("Account ID not found, unauthorized access.");
	        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
	    }

	    // Call service
	    FamilyResponseDTO result = voterService.getFamilyVotersByElection(accountId, electionId, boothNumberList, pageable);

	    return new ThedalResponse<>(ThedalSuccess.VOTER_FOUND, result);
	}

	@Operation(summary = "Retrieve fast family summary for initial loading",
	          description = "Fetches a lightweight family summary with family count and basic first member info for a given election. Optimized for fast initial page loading (~100ms). Optionally filters by booth-number or part-number. Returns family count and first member basic details only. Use crossfamily=true to get only families with members in different parts.",
	          tags = {"Voter Management"})
	@GetMapping("/election/{electionId}/families/summary")
	public ThedalResponse<FamilySummaryResponseDTO> getFamilySummary(
	        @PathVariable("electionId") Long electionId,
	        @RequestParam(value = "booth-number", required = false) String boothNumbers,
	        @RequestParam(value = "part-number", required = false) String partNumbers,
	        @RequestParam(value = "name", required = false) String nameFilter,
	        @RequestParam(value = "crossfamily", required = false) Boolean crossFamily,
	        @RequestParam(value = "page", defaultValue = "0") int page,
	        @RequestParam(value = "size", defaultValue = "20") int size) {

	    // Parse booth numbers
	    List<Integer> boothNumberList = null;
	    if (boothNumbers != null && !boothNumbers.isEmpty()) {
	        try {
	            boothNumberList = Arrays.stream(boothNumbers.split(","))
	                    .map(Integer::parseInt)
	                    .collect(Collectors.toList());
	            log.debug("Parsed boothNumbers: {}", boothNumberList);
	        } catch (NumberFormatException e) {
	            log.error("Invalid booth-number format: {}", boothNumbers);
	            throw new ThedalException(ThedalError.INVALID_REQUEST, HttpStatus.BAD_REQUEST);
	        }
	    }

	    // Parse part numbers
	    List<Integer> partNumberList = null;
	    if (partNumbers != null && !partNumbers.isEmpty()) {
	        try {
	            partNumberList = Arrays.stream(partNumbers.split(","))
	                    .map(Integer::parseInt)
	                    .collect(Collectors.toList());
	            log.debug("Parsed partNumbers: {}", partNumberList);
	        } catch (NumberFormatException e) {
	            log.error("Invalid part-number format: {}", partNumbers);
	            throw new ThedalException(ThedalError.INVALID_REQUEST, HttpStatus.BAD_REQUEST);
	        }
	    }

	    // Create pageable (simple pagination for fast response)
	    Pageable pageable = PageRequest.of(page, size);

	    // Get accountId
	    Long accountId = requestDetails.getCurrentAccountId();
	    if (accountId == null) {
	        log.error("Account ID not found, unauthorized access.");
	        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
	    }

	    // Create a request fingerprint for deduplication
	    String requestKey = String.format("family_summary_%d_%d_%s_%s_%s_%s_%d_%d", 
	            accountId, electionId, 
	            boothNumberList != null ? boothNumberList.toString() : "null",
	            partNumberList != null ? partNumberList.toString() : "null", 
	            nameFilter != null ? nameFilter : "null",
	            crossFamily != null ? crossFamily.toString() : "null",
	            page, size);
	    
	    log.debug("Processing family summary request with key: {}", requestKey);

	    // Call service for fast family summary
	    FamilySummaryResponseDTO result = voterService.getFamilySummary(accountId, electionId, boothNumberList, partNumberList, nameFilter, crossFamily, pageable);

	    return new ThedalResponse<>(ThedalSuccess.VOTER_FOUND, result);
	}

	@Operation(summary = "Retrieve detailed family members with pagination",
	          description = "Fetches complete member details for a specific family with pagination support. Optimized for fast detailed loading (~50ms) when user selects a family. Returns paginated family members with full voter information.",
	          tags = {"Voter Management"})
	@GetMapping("/election/{electionId}/families/{familyId}/members")
	public ThedalResponse<FamilyMembersResponseDTO> getFamilyMembers(
	        @PathVariable("electionId") Long electionId,
	        @PathVariable("familyId") String familyId,
	        // Default to eldest-first ordering to stay consistent with family summary representative (age DESC)
	        @RequestParam(value = "sortBy", defaultValue = "age") String sortBy,
	        @RequestParam(value = "order", defaultValue = "desc") String order,
	        @RequestParam(value = "page", defaultValue = "0") int page,
	        @RequestParam(value = "size", defaultValue = "20") int size) {

	    // Validate and map sortBy field
	    String mappedSortField;
	    String sortFieldLower = sortBy.trim().toLowerCase();
	    if (PART_NO_VARIATIONS.contains(sortFieldLower)) {
	        mappedSortField = "partNo";
	    } else if (SERIAL_NO_VARIATIONS.contains(sortFieldLower)) {
	        mappedSortField = "serialNo";
	    } else if (FNAME_VARIATIONS.contains(sortFieldLower)) {
	        mappedSortField = "voterFnameEn";
	    } else if (LNAME_VARIATIONS.contains(sortFieldLower)) {
	        mappedSortField = "voterLnameEn";
	    } 
	    else if ("age".equals(sortFieldLower)) {
	        mappedSortField = "age";
	    }	    
	    else {
	        log.error("Invalid sortBy parameter: {}. Must be one of: part_number, partNo, partNumber, serial_number, serialNo, serialNumber, voter_fname_en, voter_lname_en, age (case-insensitive)", sortBy);
	        throw new ThedalException(ThedalError.INVALID_SORT_FIELD, HttpStatus.BAD_REQUEST);
	    }

	    // Validate order
	    String orderLower = order.toLowerCase();
	    if (!orderLower.equals("asc") && !orderLower.equals("desc")) {
	        log.error("Invalid order parameter: {}. Must be 'asc' or 'desc' (case-insensitive)", order);
	        throw new ThedalException(ThedalError.INVALID_SORT_FIELD, HttpStatus.BAD_REQUEST);
	    }

	    // Validate pagination parameters
	    if (page < 0) {
	        log.error("Invalid page parameter: {}. Must be >= 0", page);
	        throw new ThedalException(ThedalError.INVALID_REQUEST, HttpStatus.BAD_REQUEST);
	    }
	    if (size <= 0 || size > 100) {
	        log.error("Invalid size parameter: {}. Must be > 0 and <= 100", size);
	        throw new ThedalException(ThedalError.INVALID_REQUEST, HttpStatus.BAD_REQUEST);
	    }

	    // Get accountId
	    Long accountId = requestDetails.getCurrentAccountId();
	    if (accountId == null) {
	        log.error("Account ID not found, unauthorized access.");
	        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
	    }

	    // Create Pageable
	    Pageable pageable = PageRequest.of(page, size);

	    // Call service for detailed family members with pagination
	    FamilyMembersResponseDTO result = voterService.getFamilyMembers(accountId, electionId, familyId, mappedSortField, orderLower, pageable);

	    return new ThedalResponse<>(ThedalSuccess.VOTER_FOUND, result);
	}

	@Operation(summary = "Renumber all families in an election",
	          description = "Reassigns sequential numbers to all families based on the selected strategy. " +
	                       "Strategies: AGE_DESC (eldest member age), NAME_ASC (eldest member name), " +
	                       "PART_ASC (eldest member part), SIZE_DESC (family size), RESET (current order). " +
	                       "Allows custom starting number.",
	          tags = {"Family Management"})
	@PostMapping("/election/{electionId}/families/renumber")
	public ThedalResponse<String> renumberFamilies(
	        @PathVariable("electionId") Long electionId,
	        @RequestParam(value = "strategy", defaultValue = "RESET") String strategy,
	        @RequestParam(value = "startNumber", defaultValue = "1") Integer startNumber) {

	    // Validate parameters
	    if (startNumber < 1) {
	        throw new ThedalException(ThedalError.INVALID_REQUEST, HttpStatus.BAD_REQUEST, 
	                "Start number must be greater than 0");
	    }

	    log.info("Renumbering families for election {} with strategy {} starting from {}", 
	             electionId, strategy, startNumber);

	    return voterService.renumberFamilies(electionId, strategy, startNumber);
	}

	@Operation(summary = "Update specific family sequence number",
	          description = "Changes the sequence number of a specific family. " +
	                       "Validates that the new number is not already taken by another family.",
	          tags = {"Family Management"})
	@PutMapping("/election/{electionId}/families/{familyId}/number")
	public ThedalResponse<String> updateFamilyNumber(
	        @PathVariable("electionId") Long electionId,
	        @PathVariable("familyId") String familyId,
	        @RequestParam("sequenceNumber") Integer sequenceNumber) {

	    // Validate parameters
	    if (sequenceNumber < 1) {
	        throw new ThedalException(ThedalError.INVALID_REQUEST, HttpStatus.BAD_REQUEST,
	                "Sequence number must be greater than 0");
	    }

	    UUID familyUUID;
	    try {
	        familyUUID = UUID.fromString(familyId);
	    } catch (IllegalArgumentException e) {
	        throw new ThedalException(ThedalError.INVALID_REQUEST, HttpStatus.BAD_REQUEST,
	                "Invalid family ID format");
	    }

	    log.info("Updating family {} sequence number to {} for election {}", 
	             familyId, sequenceNumber, electionId);

	    return voterService.updateSingleFamilyNumber(electionId, familyUUID, sequenceNumber);
	}

	@Operation(summary = "Reorder family sequence numbers",
	          description = "Updates multiple families with new sequence numbers in a single transaction. Similar to other reorder APIs (religion, caste, etc.). Used for drag-and-drop functionality.",
	          tags = {"Voter Management"})
	@PutMapping("/election/{electionId}/families/reorder")
	public ThedalResponse<String> reorderFamilySequences(
	        @PathVariable("electionId") Long electionId,
	        @RequestBody @Valid List<FamilySequenceReorderRequest> reorderRequests) {
	    
	    log.info("Reordering {} families for election {}", reorderRequests.size(), electionId);
	    return voterService.reorderFamilySequences(electionId, reorderRequests);
	}

	@Operation(summary = "Set family part override",
	          description = "Override the part number where a family is displayed. By default, families appear in the part where their eldest member is located. This allows manual assignment to a different part.",
	          tags = {"Family Management"})
	@PutMapping("/election/{electionId}/families/{familyId}/part")
	public ThedalResponse<String> setFamilyPartOverride(
	        @PathVariable("electionId") Long electionId,
	        @PathVariable("familyId") String familyId,
	        @RequestParam("partNumber") Integer partNumber) {
	    
	    UUID familyUUID;
	    try {
	        familyUUID = UUID.fromString(familyId);
	    } catch (IllegalArgumentException e) {
	        throw new ThedalException(ThedalError.INVALID_REQUEST, HttpStatus.BAD_REQUEST,
	                "Invalid family ID format");
	    }
	    
	    if (partNumber < 1) {
	        throw new ThedalException(ThedalError.INVALID_REQUEST, HttpStatus.BAD_REQUEST,
	                "Part number must be greater than 0");
	    }
	    
	    log.info("Setting family {} part override to {} for election {}", 
	            familyId, partNumber, electionId);
	    
	    return voterService.setFamilyPartOverride(electionId, familyUUID, partNumber);
	}

	@Operation(summary = "Remove family part override",
	          description = "Remove the part number override for a family, reverting to the default behavior where families appear in the part of their eldest member.",
	          tags = {"Family Management"})
	@DeleteMapping("/election/{electionId}/families/{familyId}/part")
	public ThedalResponse<String> removeFamilyPartOverride(
	        @PathVariable("electionId") Long electionId,
	        @PathVariable("familyId") String familyId) {
	    
	    UUID familyUUID;
	    try {
	        familyUUID = UUID.fromString(familyId);
	    } catch (IllegalArgumentException e) {
	        throw new ThedalException(ThedalError.INVALID_REQUEST, HttpStatus.BAD_REQUEST,
	                "Invalid family ID format");
	    }
	    
	    log.info("Removing family {} part override for election {}", familyId, electionId);
	    
	    return voterService.removeFamilyPartOverride(electionId, familyUUID);
	}

	@Operation(summary = "Set family head",
	          description = "Designate a specific family member as the family head. The family head will appear first in family member listings, regardless of age.",
	          tags = {"Family Management"})
	@PutMapping("/election/{electionId}/families/{familyId}/head")
	public ThedalResponse<String> setFamilyHead(
	        @PathVariable("electionId") Long electionId,
	        @PathVariable("familyId") String familyId,
	        @RequestBody Map<String, String> request) {
	    
	    String voterId = request.get("voterId");
	    if (voterId == null || voterId.trim().isEmpty()) {
	        throw new ThedalException(ThedalError.INVALID_REQUEST, HttpStatus.BAD_REQUEST,
	                "voterId is required in request body");
	    }
	    
	    UUID familyUUID;
	    try {
	        familyUUID = UUID.fromString(familyId);
	    } catch (IllegalArgumentException e) {
	        throw new ThedalException(ThedalError.INVALID_REQUEST, HttpStatus.BAD_REQUEST,
	                "Invalid family ID format");
	    }
	    
	    log.info("Setting voter {} as family head for family {} in election {}", 
	            voterId, familyId, electionId);
	    
	    return voterService.setFamilyHead(electionId, familyUUID, voterId.trim());
	}

	@Operation(summary = "Remove family head",
	          description = "Remove the family head designation from a family. Family members will be ordered by age (eldest first) as the default behavior.",
	          tags = {"Family Management"})
	@DeleteMapping("/election/{electionId}/families/{familyId}/head")
	public ThedalResponse<String> removeFamilyHead(
	        @PathVariable("electionId") Long electionId,
	        @PathVariable("familyId") String familyId) {
	    
	    UUID familyUUID;
	    try {
	        familyUUID = UUID.fromString(familyId);
	    } catch (IllegalArgumentException e) {
	        throw new ThedalException(ThedalError.INVALID_REQUEST, HttpStatus.BAD_REQUEST,
	                "Invalid family ID format");
	    }
	    
	    log.info("Removing family head designation for family {} in election {}", 
	            familyId, electionId);
	    
	    return voterService.removeFamilyHead(electionId, familyUUID);
	}

//	@Operation(summary = "Retrieve voters with family ID",
//	          description = "Fetches voters with a non-null familyId for a given election. Optionally filters by booth-number (or part_no). Returns voters grouped by familyId with gender statistics. Supports sorting (part_number, serial_number, voter_fname_en, voter_lname_en) and pagination.",
//	          tags = {"Voter Management"})
//	@GetMapping("/election/{electionId}/family-voters")
//	public ThedalResponse<FamilyResponseDTO> getFamilyVoters1(
//	        @PathVariable("electionId") Long electionId,
//	        @RequestParam(value = "booth-number", required = false) String boothNumbers,
//	        @RequestParam(value = "page", defaultValue = "0") int page,
//	        @RequestParam(value = "size", defaultValue = "10") int size,
//	        @RequestParam(value = "sortBy", defaultValue = "part_number,serial_number") String sortBy,
//	        @RequestParam(value = "order", defaultValue = "asc") String order) {
//
//	    // Validate and map sortBy fields
//	    List<String> sortFields = Arrays.stream(sortBy.split(","))
//	            .map(String::trim)
//	            .map(String::toLowerCase)
//	            .collect(Collectors.toList());
//
//	    List<String> mappedSortFields = new ArrayList<>();
//	    for (String sortField : sortFields) {
//	        if (PART_NO_VARIATIONS.contains(sortField)) {
//	            mappedSortFields.add("partNo");
//	        } else if (SERIAL_NO_VARIATIONS.contains(sortField)) {
//	            mappedSortFields.add("serialNo");
//	        } else if (FNAME_VARIATIONS.contains(sortField)) {
//	            mappedSortFields.add("voterFnameEn");
//	        } else if (LNAME_VARIATIONS.contains(sortField)) {
//	            mappedSortFields.add("voterLnameEn");
//	        } else {
//	            log.error("Invalid sortBy parameter: {}. Must be one of: part_number, partNo, partNumber, serial_number, serialNo, serialNumber, voter_fname_en, voter_lname_en (case-insensitive)", sortField);
//	            throw new ThedalException(ThedalError.INVALID_SORT_FIELD, HttpStatus.BAD_REQUEST);
//	        }
//	    }
//
//	    // Validate order
//	    String orderLower = order.toLowerCase();
//	    if (!orderLower.equals("asc") && !orderLower.equals("desc")) {
//	        log.error("Invalid order parameter: {}. Must be 'asc' or 'desc' (case-insensitive)", order);
//	        throw new ThedalException(ThedalError.INVALID_SORT_FIELD, HttpStatus.BAD_REQUEST);
//	    }
//
//	    // Parse booth numbers
//	    List<Integer> boothNumberList = null;
//	    if (boothNumbers != null && !boothNumbers.isEmpty()) {
//	        try {
//	            boothNumberList = Arrays.stream(boothNumbers.split(","))
//	                    .map(String::trim)
//	                    .map(Integer::valueOf)
//	                    .collect(Collectors.toList());
//	        } catch (NumberFormatException e) {
//	            log.error("Invalid booth-number format: {}", boothNumbers);
//	            throw new ThedalException(ThedalError.INVALID_BOOTH_NUMBER_FORMAT, HttpStatus.BAD_REQUEST);
//	        }
//	    }
//
//	    // Create pageable
//	    Sort.Direction direction = orderLower.equals("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
//	    Sort sort = Sort.by(direction, mappedSortFields.toArray(new String[0]));
//	    Pageable pageable = PageRequest.of(page, size, sort);
//
//	    // Get accountId
//	    Long accountId = requestDetails.getCurrentAccountId();
//	    if (accountId == null) {
//	        log.error("Account ID not found, unauthorized access.");
//	        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
//	    }
//
//	    // Call service
//	    FamilyResponseDTO result = voterService.getFamilyVotersByElection(accountId, electionId, boothNumberList, pageable);
//
//	    return new ThedalResponse<>(ThedalSuccess.VOTER_FOUND, result);
//	}
	
//	@Operation(
//		    summary = "Retrieve voters count for booth number",
//		    description = "Voter count for boothNumber with serial number",
//		    tags = {"Voter Management"}
//		)
//		@GetMapping("/election/{electionId}/booth-status")
//		public ResponseEntity<Map<String, Object>> getBoothVoterStatuses(
//		    @PathVariable Long electionId,
//		    @RequestParam Integer boothNumber) {
//
//		    List<VoterStatusDTO> voterStatuses = voterRepository.findSerialNosAndHasVotedByElectionIdAndBoothNumber(electionId, boothNumber);
//
//		    long votedCount = voterStatuses.stream()
//		        .filter(v -> Boolean.TRUE.equals(v.getHasVoted()))
//		        .count();
//
//		    long notVotedCount = voterStatuses.size() - votedCount;
//
//		    Map<String, Object> response = new HashMap<>();
//		    response.put("status", "success");
//		    response.put("message", "Fetched voter statuses successfully");
//
//		    Map<String, Object> data = new HashMap<>();
//		    data.put("votedCount", votedCount);
//		    data.put("notVotedCount", notVotedCount);
//		    data.put("totalVoters", voterStatuses.size());
//		    data.put("voters", voterStatuses);
//
//		    response.put("data", data);
//
//		    return ResponseEntity.ok(response);
//		}
	@Operation(
            summary = "Retrieve voters count for booth number",
            description = "Get voter count and statuses for a specific booth number, including EPIC numbers. Use pollStatus to filter by voting status: 'voted' for voters who have voted, 'notVoted' for voters who haven't voted, 'all' for all voters.",
            tags = {"Voter Management"}
    )
    @GetMapping("/election/{electionId}/booth-status")
    public ResponseEntity<Object> getBoothVoterStatuses(
            @PathVariable Long electionId,
            @RequestParam Integer boothNumber,
            @RequestParam(value = "pollStatus", required = false) String pollStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "asc") String sortDirection){
        // Validate pollStatus parameter
        if (pollStatus != null && !pollStatus.isEmpty()) {
            String pollStatusLower = pollStatus.toLowerCase();
            if (!pollStatusLower.equals("voted") && !pollStatusLower.equals("notvoted") && !pollStatusLower.equals("all")) {
                log.error("Invalid pollStatus parameter: {}. Must be 'voted', 'notVoted', or 'all' (case-insensitive)", pollStatus);
                throw new ThedalException(ThedalError.INVALID_REQUEST, HttpStatus.BAD_REQUEST);
            }
        }
        return ResponseEntity.ok(voterService.getBoothVoterStatuses(electionId, boothNumber, pollStatus, page, size, sortDirection));
    }
	
	@Operation(
		    summary = "Get voter stats by part number",
		    description = "Retrieve counts of voters by gender (male, female, other) and voting status (voted, not voted) for a specific part number in an election",
		    tags = {"Voter Management"}
		)
		@GetMapping("/election/{electionId}/{partNo}/part-stats")
		public ThedalResponse<PartVoterStatsDTO> getPartVoterStats(
		    @PathVariable("electionId") Long electionId,
		    @PathVariable("partNo") Integer partNo) {
		    
		    PartVoterStatsDTO stats = voterService.getPartVoterStats(electionId, partNo);
		    return new ThedalResponse<>(ThedalSuccess.VOTER_STATS_RETRIEVED, stats);
		}

	@Operation(summary = "Get voters without family with comprehensive filtering",
	           description = "Retrieves voters who have no family ID (no-family voters) for a given election with the same comprehensive filtering as the main voters API. Supports all filters except family-id. Includes filtering by voter details, demographics, categories, dates, and more.",
	           tags = {"Voter Management"})
	@GetMapping("/election/{electionId}/no-family-voters")
	public ThedalResponse<VoterResponseDTO> getNoFamilyVoters(
	        @PathVariable("electionId") Long electionId,
	        @RequestParam(value = "voter-id", required = false) String voterId,
	        @RequestParam(value = "epic-number", required = false) String epicNumber,
	        @RequestParam(value = "booth-number", required = false) String boothNumbers,
	        @RequestParam(value = "friend-id", required = false) UUID friendId,
	        @RequestParam(value = "voterName", required = false) String voterName,
	        @RequestParam(value = "voterFirstName", required = false) String voterFirstName,
	        @RequestParam(value = "voterLastName", required = false) String voterLastName,
	        @RequestParam(value = "voterFnameEn", required = false) String voterFnameEn,
	        @RequestParam(value = "voterLnameEn", required = false) String voterLnameEn,
	        @RequestParam(value = "voterFnameL1", required = false) String voterFnameL1,
	        @RequestParam(value = "voterFnameL2", required = false) String voterFnameL2,
	        @RequestParam(value = "voterLnameL1", required = false) String voterLnameL1,
	        @RequestParam(value = "voterLnameL2", required = false) String voterLnameL2,
	        @RequestParam(value = "relationName", required = false) String relationName,
	        @RequestParam(value = "relationFirstName", required = false) String relationFirstName,
	        @RequestParam(value = "relationLastName", required = false) String relationLastName,
	        @RequestParam(value = "relationFirstNameEn", required = false) String relationFirstNameEn,
	        @RequestParam(value = "relationLastNameEn", required = false) String relationLastNameEn,
	        @RequestParam(value = "party", required = false) String partyName,
	        @RequestParam(value = "religion", required = false) String religionName,
	        @RequestParam(value = "voterHistoryName", required = false) String voterHistoryName,
	        @RequestParam(value = "age", required = false) Integer age,
	        @RequestParam(value = "minAge", required = false) Integer minAge,
	        @RequestParam(value = "maxAge", required = false) Integer maxAge,
	        @RequestParam(value = "includeUnknownAge", defaultValue = "false") Boolean includeUnknownAge,
	        @RequestParam(value = "gender", required = false) String gender,
	        @RequestParam(value = "hasDob", required = false) String hasDob,
	        @RequestParam(value = "starNumber", required = false) Boolean starNumber,
	        @RequestParam(value = "catagoryDescription", required = false) String description,
	        @RequestParam(value = "categoryName", required = false) String categoryName,
	        @RequestParam(value = "casteCategoryName", required = false) String casteCategoryName,
	        @RequestParam(value = "casteName", required = false) String casteName,
	        @RequestParam(value = "subCaste", required = false) String subCaste,
	        @RequestParam(value = "duplicate", required = false) String duplicate,
	        @RequestParam(value = "serial-no", required = false) Long serialNo,
	        @RequestParam(value = "overseas", required = false) Boolean overseas,
	        @RequestParam(value = "fatherless", required = false) Boolean fatherless,
	        @RequestParam(value = "guardian", required = false) Boolean guardian,
	        @RequestParam(value = "birthdayMonth", required = false) Integer birthdayMonth,
	        @RequestParam(value = "birthdayDay", required = false) Integer birthdayDay,
	        @RequestParam(value = "hasMobileNo", required = false) Boolean hasMobileNo,
	        @RequestParam(value = "mobileNo", required = false) String mobileNo,
	        @RequestParam(value = "pollStatus", required = false) String pollStatus,
	        @RequestParam(value = "page", defaultValue = "0") int page,
	        @RequestParam(value = "size", defaultValue = "10") int size,
	        @RequestParam(value = "sortBy", defaultValue = "part_number,serial_number") String sortBy,
	        @RequestParam(value = "order", defaultValue = "asc") String order) {

	    // Get accountId
	    Long accountId = requestDetails.getCurrentAccountId();
	    if (accountId == null) {
	        log.error("Account ID not found, unauthorized access.");
	        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
	    }

	    // Validate election ownership
	    validateElectionOwnership(electionId, accountId);

	    // Validate size parameter
	    if (size < 10 || size > 100) {
	        throw new ThedalException(ThedalError.INVALID_PAGE_SIZE, HttpStatus.BAD_REQUEST);
	    }
	    
	    // Validate filter combinations
	    if (Boolean.TRUE.equals(fatherless) && Boolean.TRUE.equals(guardian)) {
	        log.error("Cannot apply both fatherless and guardian filters simultaneously");
	        throw new ThedalException(ThedalError.INVALID_FILTER_COMBINATION, HttpStatus.BAD_REQUEST);
	    }
	    
	    // Sort validation (same as main voters API)
	    List<String> sortFields = Arrays.stream(sortBy.split(","))
	            .map(String::trim)
	            .map(String::toLowerCase)
	            .collect(Collectors.toList());

	    List<String> mappedSortFields = new ArrayList<>();
	    for (String sortField : sortFields) {
	        if (PART_NO_VARIATIONS.contains(sortField)) {
	            mappedSortFields.add("partNo");
	        } else if (SERIAL_NO_VARIATIONS.contains(sortField)) {
	            mappedSortFields.add("serialNo");
	        } else if (FNAME_VARIATIONS.contains(sortField)) {
	            mappedSortFields.add("voterFnameEn");
	        } else if (LNAME_VARIATIONS.contains(sortField)) {
	            mappedSortFields.add("voterLnameEn");
	        } else if (FULL_ADDRESS_VARIATIONS.contains(sortField)) {
	            mappedSortFields.add("fullAddress");
	        } else if (AGE_VARIATIONS.contains(sortField)) {
	            mappedSortFields.add("age");
	        } else {
	            log.error("Invalid sortBy parameter: {}. Must be one of: part_number, partNo, partNumber, serial_number, serialNo, serialNumber, voter_fname_en, voter_lname_en (case-insensitive)", sortField);
	            throw new ThedalException(ThedalError.INVALID_SORT_FIELD, HttpStatus.BAD_REQUEST);
	        }
	    }

	    String orderLower = order.toLowerCase();
	    if (!orderLower.equals("asc") && !orderLower.equals("desc")) {
	        log.error("Invalid order parameter: {}. Must be 'asc' or 'desc' (case-insensitive)", order);
	        throw new ThedalException(ThedalError.INVALID_SORT_FIELD, HttpStatus.BAD_REQUEST);
	    }

	    // Date filtering logic (same as main voters API)
	    LocalDate today = LocalDate.now();
	    LocalDate tomorrow = today.plusDays(1);
	    Integer todayMonth = null;
	    Integer todayDay = null;
	    Integer tomorrowMonth = null;
	    Integer tomorrowDay = null;
	    Boolean filterToday = false;
	    Boolean filterTomorrow = false;
	    Integer customBirthdayMonth = null;
	    Integer customBirthdayDay = null;

	    if (hasDob != null) {
	        String hasDobLower = hasDob.toLowerCase();
	        switch (hasDobLower) {
	            case "today":
	                filterToday = true;
	                todayMonth = today.getMonthValue();
	                todayDay = today.getDayOfMonth();
	                break;
	            case "tomorrow":
	                filterTomorrow = true;
	                tomorrowMonth = tomorrow.getMonthValue();
	                tomorrowDay = tomorrow.getDayOfMonth();
	                break;
	            case "yes":
	                // General DOB filter without specific date
	                break;
	            default:
	                log.error("Invalid hasDob parameter: {}. Must be 'today', 'tomorrow', or 'yes' (case-insensitive)", hasDob);
	                throw new ThedalException(ThedalError.INVALID_DOB, HttpStatus.BAD_REQUEST);
	        }
	    }

	    // Use custom birthday if both month and day are provided
	    if (birthdayMonth != null && birthdayDay != null) {
	        customBirthdayMonth = birthdayMonth;
	        customBirthdayDay = birthdayDay;
	    }

	    // Call the voter service with no-family filter
	    VoterResponseDTO noFamilyVoters = voterService.getVoters(
	        electionId, voterId, epicNumber, boothNumbers, null, // familyId set to null for no-family filter
	        friendId, voterName, voterFirstName, voterLastName, voterFnameEn, voterLnameEn,
	        voterFnameL1, voterFnameL2, voterLnameL1, voterLnameL2, relationName,
	        relationFirstName, relationLastName, relationFirstNameEn, relationLastNameEn,
	        partyName, religionName, voterHistoryName, age, minAge, maxAge, includeUnknownAge,
	        gender, filterToday, filterTomorrow, starNumber, description, categoryName,
	        casteCategoryName, casteName, subCaste, duplicate, serialNo, overseas,
	        fatherless, guardian, todayMonth, todayDay, tomorrowMonth, tomorrowDay,
	        customBirthdayMonth, customBirthdayDay, hasMobileNo, mobileNo,
	        false, // singleVoterFamily not applicable for no-family voters
	        pollStatus, page, size, mappedSortFields, orderLower, 
	        false, // isFamily = false (no family)
	        true); // noFamilyOnly = true

	    log.info("Retrieved {} no-family voters for election {} with filters applied (page {}/{}, size {})",
	            noFamilyVoters.getVoters().getNumberOfElements(), electionId, 
	            page, noFamilyVoters.getVoters().getTotalPages(), size);
	    
	    return new ThedalResponse<>(ThedalSuccess.SUCCESS, noFamilyVoters);
	}

	@Operation(summary = "Generate family ID for a voter",
	           description = "Generates a new family ID for a voter who doesn't have one. Creates a single-member family.",
	           tags = {"Voter Management"})
	@PostMapping("/election/{electionId}/voters/{epicNumber}/generate-family")
	public ThedalResponse<String> generateFamilyId(
	        @PathVariable("electionId") Long electionId,
	        @PathVariable("epicNumber") String epicNumber) {

	    return voterService.generateFamilyId(electionId, epicNumber);
	}

	@Operation(summary = "Get voter from MongoDB", 
	           description = "Retrieves voter details from MongoDB by voter ID and election ID", 
	           tags = { "Voter Management" })
	@GetMapping("/mongo/election/{electionId}")
	public ThedalResponse<?> getVoterFromMongo(
	        @PathVariable("electionId") Long electionId,
	        @RequestParam(value = "epicNumber", required = false) String epicNumber,
	        @RequestParam(value = "page", defaultValue = "0") int page,
	        @RequestParam(value = "size", defaultValue = "20") int size) {
	    if (epicNumber != null && !epicNumber.trim().isEmpty()) {
	        VoterMongoDTO voterDto = voterService.getVoterFromMongo(epicNumber, electionId);
	        return new ThedalResponse<>(ThedalSuccess.VOTER_FOUND, voterDto);
	    } else {
	        Pageable pageable = PageRequest.of(page, size);
	        Page<VoterMongoDTO> voterDtos = voterService.getAllVotersFromMongo(electionId, pageable);
	        return new ThedalResponse<>(ThedalSuccess.VOTER_FOUND, voterDtos);
	    }
	}
	
	@Operation(summary = "Map a friend ID to a voter", description = "Maps a friend relationship where one voter is added as a friend of another, incrementing only the target voter's friend count.", tags = {"Voter Management"})
    @PostMapping("/friend-mapping/election/{electionId}")
    public ThedalResponse<String> mapFriendId(
            @PathVariable("electionId") Long electionId,
            @RequestParam(value = "epicNumber", required = true) @NotBlank(message = "EPIC number is mandatory") String epicNumber,
            @RequestBody(required = false) FriendMappingRequest request) {
        UUID friendId = request != null ? request.getFriendId() : null;
        String friendEpicNumber = request != null ? request.getFriendEpicNumber() : null;
        return voterService.mapFriendId(electionId, epicNumber, friendEpicNumber, friendId);
    }

	@Operation(summary = "Delete API for friendId to mapping Voter", description = "Using this API we can remove friendId from a voter", tags = { "Voter Management" })
	@DeleteMapping("/friend-mapping/election/{electionId}")
	public ThedalResponse<String> deleteFriendId(
	        @PathVariable("electionId") Long electionId,
	        @RequestParam(value = "epicNumber", required = true) @NotBlank(message = "EPIC number is mandatory") String epicNumber) {
	    return voterService.deleteFriendId(electionId, epicNumber);
	}
	
	 @Operation(summary = "Delete multiple friends from a voter's friend list",
	            description = "Removes multiple friends identified by a JSON array of friendEpicNumbers from the voter's friendsDetails list, updates friendCount, and syncs to MongoDB.",
	            tags = {"Voter Management"})
	    @DeleteMapping("/delete-friend-mapping/election/{electionId}")
	    public ThedalResponse<String> deleteFriends(
	            @PathVariable("electionId") Long electionId,
	            @RequestParam(value = "epicNumber", required = true) @NotBlank(message = "EPIC number is mandatory") String epicNumber,
	            @RequestBody @NotEmpty(message = "Friend EPIC numbers array cannot be empty") List<String> friendEpicNumbers) {
	        return voterService.deleteFriends(electionId, epicNumber, friendEpicNumbers);
	    }
	
	@Operation(summary = "Map voters to families by house number", description = "Groups all voters with the same house number into a single family for the specified election and account", tags = {"Voter Management"})
	@PostMapping("/family-mapping-by-house/election/{electionId}")
	public ThedalResponse<String> mapFamiliesByHouseNumber(
	        @PathVariable("electionId") Long electionId,
	        @RequestParam("accountId") Long accountId) {
	    
	    // Validate request in controller (where request context is available)
	    Long currentAccountId = requestDetails.getCurrentAccountId();
	    if (currentAccountId == null) {
	        log.error("Account ID not found, unauthorized access.");
	        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
	    }
	    
	    if (!currentAccountId.equals(accountId)) {
	        log.error("Unauthorized access. Account ID mismatch: provided {}, current {}", accountId, currentAccountId);
	        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
	    }
	    
	    // Call async service with validated parameters
	    voterService.mapFamiliesByHouseNumber(electionId, accountId);
	    return new ThedalResponse<>(ThedalSuccess.FAMILY_MAPPING_COMPLETED);
	}
	
	@Operation(summary = "Retrieve voters grouped by friendId",
	           description = "Fetches voters grouped by friendId for a given election. Optionally filters by booth-number. Returns an array of friend groups, each containing an array of group members with full voter details, along with gender statistics.",
	           tags = {"Voter Management"})
	@GetMapping("/election/{electionId}/friend-groups")
	public ThedalResponse<FriendGroupResponseDTO> getFriendVoters(
	        @PathVariable("electionId") Long electionId,
	        @RequestParam(value = "friendId", required = false) UUID friendId,
	        @RequestParam(value = "booth-number", required = false) String boothNumbers,
	        @RequestParam(value = "page", defaultValue = "0") int page,
	        @RequestParam(value = "size", defaultValue = "10") int size,
	        @RequestParam(value = "sortBy", defaultValue = "part_number,serial_number") String sortBy,
	        @RequestParam(value = "order", defaultValue = "asc") String order) {

	    // Validate page size
	    if (size < 10 || size > 100) {
	        throw new ThedalException(ThedalError.INVALID_PAGE_SIZE, HttpStatus.BAD_REQUEST);
	    }

	    // Validate and map sortBy fields
	    List<String> sortFields = Arrays.stream(sortBy.split(","))
	            .map(String::trim)
	            .map(String::toLowerCase)
	            .collect(Collectors.toList());

	    List<String> mappedSortFields = new ArrayList<>();
	    for (String sortField : sortFields) {
	        if (PART_NO_VARIATIONS.contains(sortField)) {
	            mappedSortFields.add("partNo");
	        } else if (SERIAL_NO_VARIATIONS.contains(sortField)) {
	            mappedSortFields.add("serialNo");
	        } else if (FNAME_VARIATIONS.contains(sortField)) {
	            mappedSortFields.add("voterFnameEn");
	        } else if (LNAME_VARIATIONS.contains(sortField)) {
	            mappedSortFields.add("voterLnameEn");
	        } else {
	            log.error("Invalid sortBy parameter: {}. Must be one of: part_number, partNo, partNumber, serial_number, serialNo, serialNumber, voter_fname_en, voter_lname_en (case-insensitive)", sortField);
	            throw new ThedalException(ThedalError.INVALID_SORT_FIELD, HttpStatus.BAD_REQUEST);
	        }
	    }

	    // Validate order
	    String orderLower = order.toLowerCase();
	    if (!orderLower.equals("asc") && !orderLower.equals("desc")) {
	        log.error("Invalid order parameter: {}. Must be 'asc' or 'desc' (case-insensitive)", order);
	        throw new ThedalException(ThedalError.INVALID_SORT_FIELD, HttpStatus.BAD_REQUEST);
	    }

	    // Parse booth numbers
	    List<Integer> boothNumberList = null;
	    if (boothNumbers != null && !boothNumbers.isEmpty()) {
	        try {
	            boothNumberList = Arrays.stream(boothNumbers.split(","))
	                    .map(Integer::parseInt)
	                    .collect(Collectors.toList());
	            log.debug("Parsed boothNumbers: {}", boothNumberList);
	        } catch (NumberFormatException e) {
	            log.error("Invalid booth-number format: {}", boothNumbers);
	            throw new ThedalException(ThedalError.INVALID_REQUEST, HttpStatus.BAD_REQUEST);
	        }
	    }

	    // Create pageable
	    Sort.Direction direction = orderLower.equals("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
	    Sort sort = Sort.by(direction, mappedSortFields.toArray(new String[0]));
	    Pageable pageable = PageRequest.of(page, size, sort);

	    // Get accountId
	    Long accountId = requestDetails.getCurrentAccountId();
	    if (accountId == null) {
	        log.error("Account ID not found, unauthorized access.");
	        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
	    }

	    // Call service
	    FriendGroupResponseDTO result = voterService.getFriendVotersByElection(accountId, electionId, friendId, boothNumberList, pageable);

	    return new ThedalResponse<>(ThedalSuccess.VOTER_FOUND, result);
	}
	

	/**
	 * Convert MongoDB response to PostgreSQL response format
	 */
	private VoterResponseDTO convertMongoToPostgresResponse(VoterResponseMongoDTO mongoResult) {
        if (mongoResult == null) {
            return null;
        }
        
        VoterResponseDTO result = new VoterResponseDTO();
        
        // Convert List<VoterResponseDTO1> to Page<VoterEntity>
        List<VoterEntity> voterEntities = mongoResult.getVoters().stream()
            .map(this::convertVoterResponseDTO1ToVoterEntity)
            .collect(Collectors.toList());
            
        PageImpl<VoterEntity> voterPage = new PageImpl<>(
            voterEntities, 
            PageRequest.of(mongoResult.getCurrentPage(), voterEntities.size()), 
            mongoResult.getTotalElements()
        );
        
        // Copy basic fields
        result.setVoters(voterPage);
        result.setGenderStats(mongoResult.getGenderStats());
        result.setBoothGenderStats(mongoResult.getBoothGenderStats());
        result.setAadhaarStats(mongoResult.getAadhaarStats());
        result.setMembershipStats(mongoResult.getMembershipStats());
        result.setBoothAadhaarStats(mongoResult.getBoothAadhaarStats());
        result.setBoothMembershipStats(mongoResult.getBoothMembershipStats());
        
        return result;
    }
	
	/**
	 * Convert VoterResponseDTO1 (MongoDB format) to VoterEntity (PostgreSQL format)
	 */
	private VoterEntity convertVoterResponseDTO1ToVoterEntity(VoterResponseDTO1 dto) {
	    log.debug("Converting voter DTO - ID: {}, Name: {} {}", 
	        dto.getId(), dto.getVoterFnameEn(), dto.getVoterLnameEn());
	    
	    VoterEntity entity = new VoterEntity();
	    
	    // Map all basic fields from MongoDB response to entity
	    // Convert String ID back to Long if possible
	    try {
	        if (dto.getId() != null && !dto.getId().isEmpty()) {
	            entity.setId(Long.parseLong(dto.getId()));
	        }
	    } catch (NumberFormatException e) {
	        log.warn("Could not convert MongoDB ID {} to Long: {}", dto.getId(), e.getMessage());
	        // Set ID to null - this might be an ObjectId that can't be converted
	        entity.setId(null);
	    }
	    entity.setVoterId(dto.getVoterId());
	    entity.setEpicNumber(dto.getEpicNumber());
	    entity.setVoterFnameEn(dto.getVoterFnameEn());
	    entity.setVoterLnameEn(dto.getVoterLnameEn());
	    entity.setVoterFnameL1(dto.getVoterFnameL1());
	    entity.setVoterFnameL2(dto.getVoterFnameL2());
	    entity.setVoterLnameL1(dto.getVoterLnameL1());
	    entity.setVoterLnameL2(dto.getVoterLnameL2());
	    entity.setRlnType(dto.getRlnType());
	    entity.setRlnFnameEn(dto.getRlnFnameEn());
	    entity.setRlnLnameEn(dto.getRlnLnameEn());
	    entity.setRlnFnameL1(dto.getRlnFnameL1());
	    entity.setRlnFnameL2(dto.getRlnFnameL2());
	    entity.setRlnLnameL1(dto.getRlnLnameL1());
	    entity.setRlnLnameL2(dto.getRlnLnameL2());
	    entity.setGender(dto.getGender());
	    entity.setAge(dto.getAge());
	    entity.setDob(dto.getDob());
	    entity.setMobileNo(dto.getMobileNo());
	    entity.setWhatsappNo(dto.getWhatsappNo());
	    entity.setEMail(dto.getEMail());
	    entity.setPhotoUrl(dto.getPhotoUrl());
	    entity.setBoothNumber(dto.getBoothNumber());
	    entity.setElectionId(dto.getElectionId());
	    entity.setAccountId(dto.getAccountId());
	    entity.setPartNo(dto.getPartNo());
	    entity.setSectionNo(dto.getSectionNo());
	    entity.setSerialNo(dto.getSerialNo());
	    entity.setHouseNoEn(dto.getHouseNoEn());
	    entity.setHouseNoL1(dto.getHouseNoL1());
	    entity.setHouseNoL2(dto.getHouseNoL2());
	    entity.setSectionNameEn(dto.getSectionNameEn());
	    entity.setSectionNameL1(dto.getSectionNameL1());
	    entity.setSectionNameL2(dto.getSectionNameL2());
	    entity.setFullAddress(dto.getFullAddress());
	    entity.setPartNameEn(dto.getPartNameEn());
	    entity.setPartNameL1(dto.getPartNameL1());
	    entity.setPartNameL2(dto.getPartNameL2());
	    entity.setPincode(dto.getPincode());
	    entity.setPartLati(dto.getPartLati());
	    entity.setPartLong(dto.getPartLong());
	    entity.setVoterLati(dto.getVoterLati());
	    entity.setVoterLongi(dto.getVoterLongi());
	    entity.setHasVoted(dto.getHasVoted());
	    entity.setVotedTimestamp(dto.getVotedTimestamp());
	    entity.setCreatedTime(dto.getCreatedTime());
	    entity.setModifiedTime(dto.getModifiedTime());
	    entity.setStarNumber(dto.getStarNumber());
	    entity.setAadhaarNumber(dto.getAadhaarNumber());
	    entity.setPanNumber(dto.getPanNumber());
	    entity.setPartyRegistrationNumber(dto.getPartyRegistrationNumber());
	    // Note: VoterResponseDTO1 doesn't have dynamicFields, setting to null
	    entity.setDynamicFields(null);
	    entity.setFamilyId(dto.getFamilyId());
	    entity.setFamilyCount(dto.getFamilyCount());
	    entity.setFriendId(dto.getFriendId());
	    entity.setFriendCount(dto.getFriendCount());
//	    try {
//	        List<FriendDetail> friendsDetailsList = dto.getFriendsDetails() != null 
//	            ? objectMapper.readValue(dto.getFriendsDetails(), new TypeReference<List<FriendDetail>>(){})
//	            : new ArrayList<>();
//	        entity.setFriendsDetails(friendsDetailsList);
//	    } catch (Exception e) {
//	        log.error("Failed to deserialize friendsDetails for voterId={}: {}", 
//	            dto.getVoterId(), e.getMessage());
//	        entity.setFriendsDetails(new ArrayList<>());
//	    }
//	 // In convertVoterResponseDTO1ToVoterEntity() method
//	    try {
//	        if (dto.getFriendsDetails() != null && !dto.getFriendsDetails().isEmpty() && !dto.getFriendsDetails().equals("[]")) {
//	            List<FriendDetail> friendsDetailsList = objectMapper.readValue(
//	                dto.getFriendsDetails(), 
//	                new TypeReference<List<FriendDetail>>(){}
//	            );
//	            entity.setFriendsDetails(friendsDetailsList);
//	            log.debug("Converted friendsDetails for voter {}: {}", dto.getVoterId(), friendsDetailsList);
//	        } else {
//	            entity.setFriendsDetails(new ArrayList<>());
//	        }
//	    } catch (Exception e) {
//	        log.error("Failed to convert friendsDetails for voter {}: {}", dto.getVoterId(), e.getMessage());
//	        entity.setFriendsDetails(new ArrayList<>());
//	    }
	    try {
	        if (dto.getFriendsDetails() != null && !dto.getFriendsDetails().isEmpty() && !dto.getFriendsDetails().equals("[]")) {
	            List<FriendDetail> friendsDetailsList = objectMapper.readValue(
	                dto.getFriendsDetails(), 
	                new TypeReference<List<FriendDetail>>(){}
	            );
	            entity.setFriendsDetails(friendsDetailsList);
	            log.debug("Converted friendsDetails for voter {}: {}", dto.getVoterId(), friendsDetailsList);
	        } else {
	            entity.setFriendsDetails(new ArrayList<>());
	            log.debug("No friendsDetails found for voter {}", dto.getVoterId());
	        }
	    } catch (Exception e) {
	        log.error("Failed to convert friendsDetails for voter {}: {}", dto.getVoterId(), e.getMessage(), e);
	        entity.setFriendsDetails(new ArrayList<>());
	    }
	    entity.setPageNumber(dto.getPageNumber());
	    entity.setRemarks(dto.getRemarks());
	    entity.setVideoUrl(dto.getVideoUrl());
	    entity.setOtp(dto.getOtp());
	    entity.setOtpCreatedAt(dto.getOtpCreatedAt());
	    entity.setMobileVerified(dto.getMobileVerified());
	    entity.setAadhaarVerified(dto.getAadhaarVerified());
	    entity.setMemberVerified(dto.getMemberVerified());
	    
	    // Set location and administrative fields
	    entity.setStateCode(dto.getStateCode());
	    entity.setStateNameEn(dto.getStateNameEn());
	    entity.setStateNameL1(dto.getStateNameL1());
	    entity.setStateNameL2(dto.getStateNameL2());
	    entity.setDistrictCode(dto.getDistrictCode());
	    entity.setDistrictNameEn(dto.getDistrictNameEn());
	    entity.setDistrictNameL1(dto.getDistrictNameL1());
	    entity.setDistrictNameL2(dto.getDistrictNameL2());
	    entity.setPcNo(dto.getPcNo());
	    entity.setPcNameEn(dto.getPcNameEn());
	    entity.setPcNameL1(dto.getPcNameL1());
	    entity.setPcNameL2(dto.getPcNameL2());
	    entity.setAcNo(dto.getAcNo());
	    entity.setAcNameEn(dto.getAcNameEn());
	    entity.setAcNameL1(dto.getAcNameL1());
	    entity.setAcNameL2(dto.getAcNameL2());
	    entity.setUrbanNo(dto.getUrbanNo());
	    entity.setUrbanNameEn(dto.getUrbanNameEn());
	    entity.setUrbanNameL1(dto.getUrbanNameL1());
	    entity.setUrbanWardNo(dto.getUrbanWardNo());
	    entity.setRurDistrictUnionNo(dto.getRurDistrictUnionNo());
	    entity.setRurDistrictUnionNameEn(dto.getRurDistrictUnionNameEn());
	    entity.setRurDistrictUnionNameL1(dto.getRurDistrictUnionNameL1());
	    entity.setRurDistrictUnionNameL2(dto.getRurDistrictUnionNameL2());
	    entity.setRurDistrictUnionWardNo(dto.getRurDistrictUnionWardNo());
	    entity.setPanUnionNo(dto.getPanUnionNo());
	    entity.setPanUnionNameEn(dto.getPanUnionNameEn());
	    entity.setPanUnionNameL1(dto.getPanUnionNameL1());
	    entity.setPanUnionNameL2(dto.getPanUnionNameL2());
	    entity.setPanUnionWardNo(dto.getPanUnionWardNo());
	    entity.setVillPanNo(dto.getVillPanNo());
	    entity.setVillPanNameEn(dto.getVillPanNameEn());
	    entity.setVillPanNameL1(dto.getVillPanNameL1());
	    entity.setVillPanWardNo(dto.getVillPanWardNo());
	    entity.setAvailability(dto.getAvailability());
	    entity.setScheme(dto.getScheme());
	    entity.setPartyAffiliation(dto.getPartyAffiliation());
	    
	    // Convert MongoDB relationship objects to PostgreSQL format for frontend compatibility
	    entity.setReligion(convertReligionMongoToEntity(dto.getReligion()));
	    
	    // Set caste and subcaste with null safety
	    if (dto.getCaste() != null) {
	        entity.setCaste(convertCasteMongoToEntity(dto.getCaste()));
	    } else {
	        entity.setCaste(null);
	    }
	    
	    if (dto.getSubCaste() != null) {
	        entity.setSubCaste(convertSubCasteMongoToEntity(dto.getSubCaste()));
	    } else {
	        entity.setSubCaste(null);
	    }
	    
	    if (dto.getCasteCategory() != null) {
	        entity.setCasteCategory(convertCasteCategoryMongoToEntity(dto.getCasteCategory()));
	    } else {
	        entity.setCasteCategory(null);
	    }
	    
	    entity.setParty(convertPartyMongoToEntity(dto.getParty()));
	    entity.setAvailability1(convertAvailabilityMongoToEntity(dto.getAvailability1()));
	    entity.setPartManager(convertPartManagerMongoToEntity(dto.getPartManager()));
	    entity.setLanguages(convertLanguagesMongoToEntity(dto.getLanguages()));
	    //entity.setBenefitSchemes(convertBenefitSchemesMongoToEntity(dto.getBenefitSchemes()));
	    entity.setFeedbackIssues(convertFeedbackIssuesMongoToEntity(dto.getFeedbackIssues()));
	    entity.setVoterHistories(convertVoterHistoriesMongoToEntity(dto.getVoterHistories()));
	    
	    return entity;
	}
	
	/**
	 * Convert ReligionMongo to ReligionEntity for frontend compatibility
	 */
	private ReligionEntity convertReligionMongoToEntity(ReligionMongo mongo) {
	    if (mongo == null) return null;
	    
	    ReligionEntity entity = new ReligionEntity();
	    entity.setId(mongo.getId());
	    entity.setReligionName(mongo.getReligionName());
	    entity.setReligionImage(mongo.getReligionImage());
	    entity.setAccountId(mongo.getAccountId());
	    entity.setElectionId(mongo.getElectionId());
	    entity.setOrderIndex(mongo.getOrderIndex());
	    entity.setCreatedAt(mongo.getCreatedAt());
	    entity.setUpdatedAt(mongo.getUpdatedAt());
	    return entity;
	}
	
	/**
	 * Convert CasteMongo to CasteEntity for frontend compatibility
	 */
	private CasteEntity convertCasteMongoToEntity(CasteMongo mongo) {
	    if (mongo == null) {
	        log.debug("CasteMongo is null, returning null CasteEntity");
	        return null;
	    }
	    
	    log.debug("Converting CasteMongo - ID: {}, Name: {}", mongo.getId(), mongo.getCasteName());
	    
	    CasteEntity entity = new CasteEntity();
	    entity.setId(mongo.getId());
	    entity.setCasteName(mongo.getCasteName());
	    entity.setAccountId(mongo.getAccountId());
	    entity.setElectionId(mongo.getElectionId());
	    entity.setOrderIndex(mongo.getOrderIndex());
	    entity.setCreatedAt(mongo.getCreatedAt());
	    entity.setUpdatedAt(mongo.getUpdatedAt());
	    // Note: We don't set the religion object to avoid circular dependencies in response
	    return entity;
	}
	
	/**
	 * Convert SubCasteMongo to SubCasteEntity for frontend compatibility
	 */
	private SubCasteEntity convertSubCasteMongoToEntity(SubCasteMongo mongo) {
	    if (mongo == null) {
	        log.debug("SubCasteMongo is null, returning null SubCasteEntity");
	        return null;
	    }
	    
	    log.debug("Converting SubCasteMongo - ID: {}, Name: {}", mongo.getId(), mongo.getSubCasteName());
	    
	    SubCasteEntity entity = new SubCasteEntity();
	    entity.setId(mongo.getId());
	    entity.setSubCasteName(mongo.getSubCasteName());
	    entity.setAccountId(mongo.getAccountId());
	    entity.setElectionId(mongo.getElectionId());
	    entity.setOrderIndex(mongo.getOrderIndex());
	    entity.setCreatedAt(mongo.getCreatedAt());
	    entity.setUpdatedAt(mongo.getUpdatedAt());
	    // Note: We don't set the caste/religion objects to avoid circular dependencies in response
	    return entity;
	}
	
	/**
	 * Convert CasteCategoryMongo to CasteCategoryEntity for frontend compatibility
	 */
	private CasteCategoryEntity convertCasteCategoryMongoToEntity(CasteCategoryMongo mongo) {
	    if (mongo == null) return null;
	    
	    CasteCategoryEntity entity = new CasteCategoryEntity();
	    entity.setId(mongo.getId());
	    entity.setCasteCategoryName(mongo.getCasteCategoryName());
	    entity.setAccountId(mongo.getAccountId());
	    entity.setElectionId(mongo.getElectionId());
	    entity.setOrderIndex(mongo.getOrderIndex());
	    entity.setCreatedAt(mongo.getCreatedAt());
	    entity.setUpdatedAt(mongo.getUpdatedAt());
	    return entity;
	}
	
	/**
	 * Convert PartyMongo to Party for frontend compatibility
	 */
	private Party convertPartyMongoToEntity(PartyMongo mongo) {
	    if (mongo == null) return null;
	    
	    Party entity = new Party();
	    entity.setId(mongo.getId());
	    entity.setPartyName(mongo.getPartyName());
	    entity.setPartyShortName(mongo.getPartyShortName());
	    entity.setPartyImage(mongo.getPartyImage());
	    entity.setAccountId(mongo.getAccountId());
	    entity.setElectionId(mongo.getElectionId());
	    entity.setOrderIndex(mongo.getOrderIndex());
	    entity.setCreatedAt(mongo.getCreatedAt());
	    entity.setUpdatedAt(mongo.getUpdatedAt());
	    return entity;
	}
	
	/**
	 * Convert AvailabilityMongo to Availability for frontend compatibility
	 */
	private Availability convertAvailabilityMongoToEntity(AvailabilityMongo mongo) {
	    if (mongo == null) return null;
	    
	    Availability entity = new Availability();
	    entity.setId(mongo.getId());
	    entity.setCategoryName(mongo.getCategoryName());
	    entity.setAccountId(mongo.getAccountId());
	    entity.setElectionId(mongo.getElectionId());
	    entity.setOrderIndex(mongo.getOrderIndex());
	    entity.setCreatedAt(mongo.getCreatedAt());
	    entity.setUpdatedAt(mongo.getUpdatedAt());
	    return entity;
	}
	
	/**
	 * Convert PartManagerMongo to PartManager for frontend compatibility
	 */
	private PartManager convertPartManagerMongoToEntity(PartManagerMongo mongo) {
	    if (mongo == null) return null;
	    
	    PartManager entity = new PartManager();
	    entity.setId(mongo.getId());
	    entity.setPartNo(mongo.getPartNo());
	    entity.setPartNameEnglish(mongo.getPartNameEnglish());
	    entity.setPartNameL1(mongo.getPartNameL1());
	    entity.setSchoolName(mongo.getSchoolName());
	    
	    // Handle nullable Double fields safely
	    entity.setPartLat(mongo.getPartLat());
	    entity.setPartLong(mongo.getPartLong());
	    entity.setSchoolLat(mongo.getSchoolLat());
	    entity.setSchoolLong(mongo.getSchoolLong());
	    
	    entity.setPincode(mongo.getPincode());
	    entity.setAccountId(mongo.getAccountId());
	    entity.setElectionId(mongo.getElectionId());
	    entity.setBoothVulnerability(mongo.getBoothVulnerability());
	    entity.setOrderIndex(mongo.getOrderIndex());
	    return entity;
	}
	
	/**
	 * Convert Set<LanguageMongo> to Set<Language> for frontend compatibility
	 */
	private Set<Language> convertLanguagesMongoToEntity(Set<LanguageMongo> mongoSet) {
	    if (mongoSet == null) return null;
	    
	    return mongoSet.stream()
	        .map(this::convertLanguageMongoToEntity)
	        .collect(Collectors.toSet());
	}
	
	private Language convertLanguageMongoToEntity(LanguageMongo mongo) {
	    if (mongo == null) return null;
	    
	    Language entity = new Language();
	    entity.setId(mongo.getId());
	    entity.setLanguageName(mongo.getLanguageName());
	    entity.setAccountId(mongo.getAccountId());
	    entity.setElectionId(mongo.getElectionId());
	    entity.setOrderIndex(mongo.getOrderIndex());
	    entity.setState(mongo.getState());
	    entity.setCreatedAt(mongo.getCreatedAt());
	    entity.setUpdatedAt(mongo.getUpdatedAt());
	    return entity;
	}
	
	/**
	 * Convert List<BenefitSchemesMongo> to List<BenefitSchemes> for frontend compatibility
	 */
	private List<BenefitSchemes> convertBenefitSchemesMongoToEntity(List<BenefitSchemesMongo> mongoList) {
	    if (mongoList == null) return null;
	    
	    return mongoList.stream()
	        .map(this::convertBenefitSchemeMongoToEntity)
	        .collect(Collectors.toList());
	}
	
	private BenefitSchemes convertBenefitSchemeMongoToEntity(BenefitSchemesMongo mongo) {
	    if (mongo == null) return null;
	    
	    BenefitSchemes entity = new BenefitSchemes();
	    entity.setId(mongo.getId());
	    entity.setSchemeName(mongo.getSchemeName());
	    entity.setAccountId(mongo.getAccountId());
	    entity.setElectionId(mongo.getElectionId());
	    entity.setOrderIndex(mongo.getOrderIndex());
	    entity.setCreatedAt(mongo.getCreatedAt());
	    entity.setUpdatedAt(mongo.getUpdatedAt());
	    return entity;
	}
	
	/**
	 * Convert Set<FeedbackIssueMongo> to Set<FeedbackIssue> for frontend compatibility
	 */
	private Set<FeedbackIssue> convertFeedbackIssuesMongoToEntity(Set<FeedbackIssueMongo> mongoSet) {
	    if (mongoSet == null) return null;
	    
	    return mongoSet.stream()
	        .map(this::convertFeedbackIssueMongoToEntity)
	        .collect(Collectors.toSet());
	}
	
	private FeedbackIssue convertFeedbackIssueMongoToEntity(FeedbackIssueMongo mongo) {
	    if (mongo == null) return null;
	    
	    FeedbackIssue entity = new FeedbackIssue();
	    entity.setId(mongo.getId());
	    entity.setIssueName(mongo.getIssueName());
	    entity.setAccountId(mongo.getAccountId());
	    entity.setElectionId(mongo.getElectionId());
	    entity.setOrderIndex(mongo.getOrderIndex());
	    entity.setCreatedAt(mongo.getCreatedAt());
	    // Note: FeedbackIssueMongo doesn't have updatedAt field
	    return entity;
	}
	
	/**
	 * Convert Set<VoterHistoryMongo> to Set<VoterHistoryEntity> for frontend compatibility
	 */
	private Set<VoterHistoryEntity> convertVoterHistoriesMongoToEntity(Set<VoterHistoryMongo> mongoSet) {
	    if (mongoSet == null) return null;
	    
	    return mongoSet.stream()
	        .map(this::convertVoterHistoryMongoToEntity)
	        .collect(Collectors.toSet());
	}
	
	private VoterHistoryEntity convertVoterHistoryMongoToEntity(VoterHistoryMongo mongo) {
	    if (mongo == null) return null;
	    
	    VoterHistoryEntity entity = new VoterHistoryEntity();
	    entity.setId(mongo.getId());
	    entity.setVoterHistoryName(mongo.getVoterHistoryName());
	    entity.setVoterHistoryImage(mongo.getVoterHistoryImage());
	    entity.setAccountId(mongo.getAccountId());
	    entity.setElectionId(mongo.getElectionId());
	    entity.setOrderIndex(mongo.getOrderIndex());
	    // Note: VoterHistoryMongo doesn't have createdAt/updatedAt fields
	    return entity;
	}
	

 
	@Operation(summary = "Get election voter statistics",
	          description = "Retrieves aggregate statistics for an election including total voters, voted count, not voted count, and turnout percentage",
	          tags = {"Voter Management"})
	@GetMapping("/election/{electionId}/stats")
	public ThedalResponse<com.thedal.thedal_app.voter.dto.ElectionVoterStatsDTO> getElectionStats(
	        @PathVariable("electionId") Long electionId) {
	    
	    log.info("Fetching election voter stats for electionId: {}", electionId);
	    com.thedal.thedal_app.voter.dto.ElectionVoterStatsDTO stats = voterService.getElectionVoterStats(electionId);
	    return new ThedalResponse<>(ThedalSuccess.VOTER_STATS_RETRIEVED, stats);
	}

	@Operation(summary = "Get winning probability for default party",
	          description = "Calculates the winning probability for the default party in the election based on voter support and turnout statistics",
	          tags = {"Voter Management"})
	@GetMapping("/election/{electionId}/winning-probability")
	public ThedalResponse<com.thedal.thedal_app.voter.dto.WinningProbabilityDTO> getWinningProbability(
	        @PathVariable("electionId") Long electionId) {
	    
	    log.info("Calculating winning probability for electionId: {}", electionId);
	    com.thedal.thedal_app.voter.dto.WinningProbabilityDTO probability = voterService.getWinningProbability(electionId);
	    return new ThedalResponse<>(ThedalSuccess.SUCCESS, probability);
	}

}

