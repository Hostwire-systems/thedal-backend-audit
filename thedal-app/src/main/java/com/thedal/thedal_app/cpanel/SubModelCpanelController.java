package com.thedal.thedal_app.cpanel;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
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

import com.thedal.thedal_app.cpanel.dtos.ActivateUserRequestDto;
import com.thedal.thedal_app.cpanel.dtos.CasteUpdateCpanelRequest;
import com.thedal.thedal_app.cpanel.dtos.FeedbackIssueRequest;
import com.thedal.thedal_app.cpanel.dtos.FeedbackIssueResponseDTO;
import com.thedal.thedal_app.cpanel.dtos.FeedbackIssueUpdateRequest;
import com.thedal.thedal_app.cpanel.dtos.SlipBoxDTO;
import com.thedal.thedal_app.cpanel.dtos.SubCasteCpanelUpdateRequest;
import com.thedal.thedal_app.election.ElectionRepository;
import com.thedal.thedal_app.general.RequestDetailsService;
import com.thedal.thedal_app.response.ThedalResponse;
import com.thedal.thedal_app.response.ThedalSuccess;
import com.thedal.thedal_app.settings.electionsettings.AvailabilityResponse;
import com.thedal.thedal_app.settings.electionsettings.CasteEntity;
import com.thedal.thedal_app.settings.electionsettings.LanguageResponse;
import com.thedal.thedal_app.settings.electionsettings.LanguageService;
import com.thedal.thedal_app.settings.electionsettings.Party;
import com.thedal.thedal_app.settings.electionsettings.SectionBulkUploadEntity;
import com.thedal.thedal_app.settings.electionsettings.SubCasteEntity;
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
import com.thedal.thedal_app.settings.electionsettings.dto.SubCasteRequest;
import com.thedal.thedal_app.settings.electionsettings.dto.SubCasteResponseDTO;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api")
public class SubModelCpanelController {
	
	
	@Autowired
	private RequestDetailsService requestDetails;
	@Autowired
    private SubModelCpanelService subModelCpanelService;
	@Autowired
	private ElectionRepository electionRepository;
	@Autowired
	private LanguageService languageService;
	@Autowired
    private SlipBoxRepository slipBoxRepository;
	
	@Operation(
		    summary = "Create languages for an election",
		    description = "Saves multiple languages for a specific election ID.",
		    tags = { "Sub Model Cpanel Controller" })
	@PostMapping("/cpanel/language") // Removed electionId from the path
	public ThedalResponse<List<LanguageResponseDTO>> createLanguages(
	    @RequestBody List<LanguageDTO> languageDTOList,
	    HttpServletRequest request
	) {
	    return subModelCpanelService.createLanguages(languageDTOList, request);
	} 

	@Operation(
		    summary = "Get languages for an election",
		    description = "Get multiple languages for a specific election ID.",
		    tags = { "Sub Model Cpanel Controller" })
	@GetMapping("/cpanel/language") // Removed electionId from the path
	public ThedalResponse<List<LanguageResponse>> getLanguage() {
	    Long accountId = requestDetails.getCurrentAccountId();

	    if (accountId == null) {
	        log.error("Account ID not found, unauthorized access.");
	        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
	    }

	    // Force electionId = 0 and accountId = 0 for control panel requests
	    Long electionId = 0L;
	    accountId = 0L;

	    List<LanguageResponse> languageDTO = subModelCpanelService.getLanguage(accountId, electionId);
	    return new ThedalResponse<>(ThedalSuccess.LANGUAGE_FOUND, languageDTO);
	}
	
	@Operation(
		    summary = "Update languages for the control panel",
		    description = "Update multiple languages for election ID 0.",
		    tags = { "Sub Model Cpanel Controller" }
		)
		@PutMapping("/cpanel/language") // Removed electionId from the path
		public ThedalResponse<List<LanguageResponseDTO>> updateLanguages(
		        @RequestBody List<LanguageDTO> languageUpdateDTOList) {

		    Long accountId = requestDetails.getCurrentAccountId();

		    if (accountId == null) {
		        log.error("Account ID not found, unauthorized access.");
		        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
		    }

		    // Force electionId = 0 and accountId = 0 for control panel requests
		    Long electionId = 0L;
		    accountId = 0L;

		    List<LanguageResponseDTO> updatedLanguages = subModelCpanelService.updateLanguages(accountId, electionId, languageUpdateDTOList);
		    return new ThedalResponse<>(ThedalSuccess.LANGUAGE_UPDATED, updatedLanguages);
		}
	
	@Operation(
		    summary = "Delete a language for the control panel",
		    description = "Delete a language for election ID 0.",
		    tags = { "Sub Model Cpanel Controller" }
		)
		//@DeleteMapping("/cpanel/language/{languageId}") 
	    @DeleteMapping("/cpanel/language")
		public ThedalResponse<Void> deleteLanguage(
				//@PathVariable Long languageId
				@RequestParam(value = "languageIds", required = false) List<Long> languageIds
				) {
		    Long accountId = requestDetails.getCurrentAccountId();

		    if (accountId == null) {
		        log.error("Account ID not found, unauthorized access.");
		        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
		    }

		    // Force electionId = 0 and accountId = 0 for control panel requests
		    Long electionId = 0L;
		    accountId = 0L;

		    subModelCpanelService.deleteLanguage(accountId, electionId, languageIds);

		    return new ThedalResponse<>(ThedalSuccess.LANGUAGE_DELETED, null);
		}
	
////////////////// caste api's
	
	@Operation(
		    summary = "Create castes for the control panel",
		    description = "Saves multiple castes for election ID 0.",
		    tags = { "Sub Model Cpanel Controller" }
		)
		@PostMapping("/cpanel/castes") // Removed electionId from the path
		public ThedalResponse<List<CasteEntity>> createMultipleCastes(@RequestBody @Valid List<CasteRequest> casteRequests) {
		    Long accountId = requestDetails.getCurrentAccountId();

		    if (accountId == null) {
		        log.error("Account ID not found, unauthorized access.");
		        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
		    }

		    // Force electionId = 0 and accountId = 0 for control panel requests
		    Long electionId = 0L;
		    accountId = 0L; 

		    List<CasteEntity> casteEntities = subModelCpanelService.createMultipleCastes(casteRequests, electionId, accountId);
		    return new ThedalResponse<>(ThedalSuccess.CASTE_CREATED, casteEntities);
		}
	
	@Operation(
		    summary = "Get castes for CPanel",
		    description = "Fetch castes for the control panel (electionId is always 0).",
		    tags = { "Sub Model Cpanel Controller" }
		)
		@GetMapping("/cpanel/caste")  
		public ThedalResponse<List<Map<String, Object>>> getCasteForCpanel(
				 @RequestParam(required = false) Long religionId) {
		    Long accountId = requestDetails.getCurrentAccountId();

		    if (accountId == null) {
		        log.error("Account ID not found, unauthorized access.");
		        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
		    }

		    // Hardcoding electionId = 0 and accountId = 0 for control panel requests
		    Long electionId = 0L;
		    accountId = 0L;

		    List<Map<String, Object>> casteDetails = subModelCpanelService.getCasteForCpanel(accountId, electionId, religionId);
		    return new ThedalResponse<>(ThedalSuccess.CASTES_FETCHED, casteDetails);
		}
	
	@Operation(
		    summary = "Delete caste for CPanel",
		    description = "Delete a caste by ID in the control panel (electionId is always 0).",
		    tags = { "Sub Model Cpanel Controller" }
		)
		@DeleteMapping("/cpanel/castes")  // Removed {electionId} from the path
		public ThedalResponse<Void> deleteCaste(
			@RequestParam(value = "casteIds", required = false) List<Long> casteIds) {

		    Long accountId = requestDetails.getCurrentAccountId();

		    if (accountId == null) {
		        log.error("Account ID not found, unauthorized access.");
		        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
		    }

		    // Hardcoding electionId = 0 and accountId = 0 for control panel requests
		    Long electionId = 0L;
		    accountId = 0L;

		    subModelCpanelService.deleteCaste(accountId, electionId, casteIds);

		    return new ThedalResponse<>(ThedalSuccess.CASTE_DELETED, null);
		}

	@Operation(
		    summary = "Update multiple castes",
		    description = "Update multiple castes for the control panel (electionId is always 0).",
		    tags = { "Sub Model Cpanel Controller" }
		)
		@PutMapping("/cpanel/castes")  // Removed {electionId} from the path
		public ThedalResponse<List<CasteResponseDTO>> updateMultipleCastes(
		        @RequestBody List<CasteUpdateCpanelRequest> casteUpdateRequests) {

		    Long accountId = requestDetails.getCurrentAccountId();

		    if (accountId == null) {
		        log.error("Account ID not found, unauthorized access.");
		        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
		    }

		    // Hardcoding electionId = 0 and accountId = 0 for control panel requests
		    Long electionId = 0L;
		    accountId = 0L;

		    List<CasteResponseDTO> updatedCastes = subModelCpanelService.updateMultipleCastes(accountId, electionId, casteUpdateRequests);

		    return new ThedalResponse<>(ThedalSuccess.CASTE_UPDATED, updatedCastes);
		}
	
	@Operation(summary = "Upload bulk Castes for cPanel", description = "Upload bulk Castes with casteName and religionId for cPanel with electionId=0 and accountId=0 using xlsx or csv files.",
			tags = { "Sub Model Cpanel Controller" })
    @PostMapping(value = "/caste-upload", consumes = "multipart/form-data")
    public ResponseEntity<ThedalResponse<SectionBulkUploadEntity>> uploadCpanelCastes(
            @RequestParam("file") MultipartFile file) throws IOException {
        if (file.getSize() > 100 * 1024 * 1024) { // 100 MB limit
            throw new ThedalException(ThedalError.FILE_TOO_LARGE, HttpStatus.BAD_REQUEST);
        }
        ThedalResponse<SectionBulkUploadEntity> response = subModelCpanelService.uploadCpanelCastes(file);
        return ResponseEntity.ok(response);
    }


//////// SubCaste api's
	
	@Operation(
		    summary = "Create multiple sub-castes",
		    description = "Saves multiple sub-castes for the control panel (electionId is always 0).",
		    tags = { "Sub Model Cpanel Controller" }
		)
		@PostMapping("/cpanel/subcastes")  // Removed {electionId} from the path
		public ThedalResponse<List<SubCasteEntity>> createMultipleSubCastes(
		        @RequestBody @Valid List<SubCasteRequest> subCasteRequests) {

		    Long accountId = requestDetails.getCurrentAccountId();

		    if (accountId == null) {
		        log.error("Account ID not found, unauthorized access.");
		        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
		    }

		    // Hardcoding electionId = 0 and accountId = 0 for control panel requests
		    Long electionId = 0L;
		    accountId = 0L;

		    List<SubCasteEntity> subCasteEntities = subModelCpanelService.createMultipleSubCastes(subCasteRequests, electionId, accountId);
		    return new ThedalResponse<>(ThedalSuccess.SUB_CASTE_CREATED, subCasteEntities);
		}


	@Operation( 
		    summary = "Get subcastes for CPanel",
		    description = "Fetch subcastes for the control panel.",
		    tags = { "Sub Model Cpanel Controller" }
		)
		@GetMapping("/cpanel/subcaste")
		public ThedalResponse<List<Map<String, Object>>> getSubCasteForCpanel(
				@RequestParam(required = false) Long casteId,
		        @RequestParam(required = false) Long religionId) {
		    Long accountId = requestDetails.getCurrentAccountId();

		    if (accountId == null) {
		        log.error("Account ID not found, unauthorized access.");
		        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
		    }

		    // Set accountId to 0 for control panel requests
		    accountId = 0L;
		    Long electionId = 0L; // Hardcoded to 0 internally

		    List<Map<String, Object>> subCasteDetails = subModelCpanelService.getSubCasteForCpanel(accountId, electionId, casteId, religionId);
		    return new ThedalResponse<>(ThedalSuccess.SUBCASTES_FETCHED, subCasteDetails);
		}

	
	@Operation( 
		    summary = "Delete subcaste for CPanel",
		    description = "Delete a subcaste by ID for the control panel.",
		    tags = { "Sub Model Cpanel Controller" }
		)
		@DeleteMapping("/cpanel/subcastes")
		public ThedalResponse<Void> deleteSubCaste( @RequestParam(value = "subCasteIds", required = false) List<Long> subCasteIds) {
		    Long accountId = requestDetails.getCurrentAccountId();

		    if (accountId == null) {
		        log.error("Account ID not found, unauthorized access.");
		        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
		    }

		    // Set accountId and electionId to 0 for control panel requests
		    accountId = 0L;
		    Long electionId = 0L; // Hardcoded

		    subModelCpanelService.deleteSubCaste(accountId, electionId, subCasteIds);

		    return new ThedalResponse<>(ThedalSuccess.SUBCASTE_DELETED, null);
		}

	
	@Operation( 
		    summary = "Update multiple sub-castes",
		    description = "Update multiple sub-castes for the control panel.",
		    tags = { "Sub Model Cpanel Controller" }
		)
		@PutMapping("/cpanel/subcastes")
		public ThedalResponse<List<SubCasteResponseDTO>> updateMultipleSubCastes(
		        @RequestBody List<SubCasteCpanelUpdateRequest> subCasteUpdateRequests) {

		    Long accountId = requestDetails.getCurrentAccountId();

		    if (accountId == null) {
		        log.error("Account ID not found, unauthorized access.");
		        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
		    }

		    // Set accountId and electionId to 0 for control panel requests
		    accountId = 0L;
		    Long electionId = 0L; // Hardcoded

		    List<SubCasteResponseDTO> updatedSubCastes = subModelCpanelService.updateMultipleSubCastes(accountId, electionId, subCasteUpdateRequests);

		    return new ThedalResponse<>(ThedalSuccess.SUBCASTE_UPDATED, updatedSubCastes);
		}

	@Operation(summary = "Upload bulk Sub-Castes for cPanel", description = "Upload bulk Sub-Castes with subCasteName, casteId, and religionId for cPanel with electionId=0 and accountId=0 using xlsx or csv files.",
			tags = { "Sub Model Cpanel Controller" })
    @PostMapping(value = "/subcaste-upload", consumes = "multipart/form-data")
    public ResponseEntity<ThedalResponse<SectionBulkUploadEntity>> uploadCpanelSubCastes(
            @RequestParam("file") MultipartFile file) throws IOException {
        if (file.getSize() > 100 * 1024 * 1024) { // 100 MB limit
            throw new ThedalException(ThedalError.FILE_TOO_LARGE, HttpStatus.BAD_REQUEST);
        }
        ThedalResponse<SectionBulkUploadEntity> response = subModelCpanelService.uploadCpanelSubCastes(file);
        return ResponseEntity.ok(response);
    }
	
/////////////// Party api's
	
	@Operation(summary = "Create multiple parties", description = "Saves multiple parties in a single request", 
		    tags = { "Sub Model Cpanel Controller" })
	@PostMapping(value = "/cpanel/parties", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ThedalResponse<Party> createSingleParty(
	        @RequestPart("partyImage") MultipartFile partyImage, 
	        @RequestParam("partyName") String partyName,      
	        @RequestParam("partyShortName") String partyShortName,
	        @RequestParam(value = "partyColor", required = false) String partyColor,
	        @RequestParam(value = "allianceName", required = false) String allianceName) {  

	    // Build a single PartyRequest object
	    PartyRequest partyRequest = new PartyRequest();
	    partyRequest.setPartyName(partyName);
	    partyRequest.setPartyShortName(partyShortName);
	    partyRequest.setPartyImage(partyImage);
	    partyRequest.setPartyColor(partyColor);
	    partyRequest.setAllianceName(allianceName);
	    partyRequest.setElectionId(0L); // Hardcoded as in original

	    return subModelCpanelService.createSingleParty(partyRequest);
	}


	@Operation(
		    summary = "Get parties for control panel",
		    description = "Retrieve parties from the control panel.",
		    tags = { "Sub Model Cpanel Controller" }
		)
		@GetMapping("/cpanel/parties")
		public ThedalResponse<List<PartyResponse>> getPartiesForCpanel() {
		    Long accountId = requestDetails.getCurrentAccountId();

		    if (accountId == null) {
		        log.error("Account ID not found, unauthorized access.");
		        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
		    }

		    // Hardcoded electionId = 0
		    accountId = 0L;
		    Long electionId = 0L;

		    List<PartyResponse> parties = subModelCpanelService.getParties(accountId, electionId);
		    return new ThedalResponse<>(ThedalSuccess.PARTIES_FETCHED, parties);
		}



	@Operation(
       summary = "Update multiple parties",  
       description = "Updates multiple parties in a single request",
         tags = { "Sub Model Cpanel Controller" }
     )
	@PutMapping(value = "/cpanel/parties", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ThedalResponse<Party> updateSingleParty(
	        @RequestPart(value = "partyImage", required = false) MultipartFile partyImage,  
	        @RequestParam("partyId") Long partyId,                                     
	        @RequestParam(value = "partyShortName", required = false) String partyShortName,  
	        @RequestParam(value = "partyName", required = false) String partyName,
	        @RequestParam(value = "partyColor", required = false) String partyColor,
	        @RequestParam(value = "allianceName", required = false) String allianceName) {   

	    Long accountId = requestDetails.getCurrentAccountId();
	    if (accountId == null) {
	        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
	    }

	    // Hardcoded values
	    accountId = 0L;
	    Long electionId = 0L;

	    // Build a single PartyRequest object
	    PartyRequest partyRequest = new PartyRequest();
	    partyRequest.setPartyId(partyId);

	    if (partyShortName != null) {
	        partyRequest.setPartyShortName(partyShortName);
	    }
	    if (partyName != null) {
	        partyRequest.setPartyName(partyName);
	    }
	    if (partyImage != null) {
	        partyRequest.setPartyImage(partyImage);
	    }
	    if (partyColor != null) {
	        partyRequest.setPartyColor(partyColor);
	    }
	    if (allianceName != null) {
	        partyRequest.setAllianceName(allianceName);
	    }

	    return subModelCpanelService.updateSingleParty(partyRequest);
	}



	@Operation(
		    summary = "Delete a party",
		    description = "Deletes a party by ID for the control panel",
		    tags = { "Sub Model Cpanel Controller" }
		)
		@DeleteMapping("/cpanel/party")
		public ThedalResponse<Void> deleteParty( @RequestParam(value = "partyIds", required = false) List<Long> partyIds) {
		    Long accountId = requestDetails.getCurrentAccountId();

		    if (accountId == null) {
		        log.error("Account ID not found, unauthorized access.");
		        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
		    }

		    // Hardcode accountId and electionId to 0
		    accountId = 0L;
		    Long electionId = 0L;

		    subModelCpanelService.deleteParty(accountId, electionId, partyIds);

		    return new ThedalResponse<>(ThedalSuccess.PARTY_DELETED, null);
		}


////////////////// availability api's
	
	@Operation(
		    summary = "Create availability",
		    description = "Saves a single availability with an optional image. accountId and electionId are set to 0 unless the request comes from /api/cpanel",
		    tags = { "Sub Model Cpanel Controller" }
		)
	@PostMapping("/cpanel/availability")
	public ThedalResponse<AvailabilityReponseDTO> createAvailability(
	        @RequestPart("DTO") AvailabilityDTO availabilityDTO,
	        @RequestPart(value = "file", required = false) MultipartFile file,
	        HttpServletRequest request) {

	    return subModelCpanelService.createAvailability(availabilityDTO, file, request);
	}


	@Operation(
		    summary = "Get availability",
		    description = "Retrieves availability list. Only returns data where accountId and electionId are 0.",
		    tags = { "Sub Model Cpanel Controller" }
		)
		@GetMapping("/cpanel/availability")
		public ThedalResponse<List<AvailabilityResponse>> getAvailability(HttpServletRequest request) {
		    return subModelCpanelService.getAvailability(request);
		}

	@Operation(
		    summary = "Update availability for the control panel",
		    description = "Update an availability entry for election ID 0.",
		    		tags = { "Sub Model Cpanel Controller" }
		)
	@PutMapping("/cpanel/availability/{availabilityId}")
	public ThedalResponse<AvailabilityReponseDTO> updateAvailabilityCpanel(
	        @PathVariable Long availabilityId,
	        @RequestPart("DTO") AvailabilityDTO availabilityDTO,
	        @RequestPart(value = "file", required = false) MultipartFile file) {

	    Long accountId = requestDetails.getCurrentAccountId();

	    if (accountId == null) {
	        log.error("Account ID not found, unauthorized access.");
	        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
	    }

	    // Force electionId = 0 and accountId = 0 for control panel requests
	    Long electionId = 0L;
	    accountId = 0L;

	    AvailabilityReponseDTO updatedAvailability = subModelCpanelService.updateAvailability(accountId, electionId, availabilityId, availabilityDTO, file);
	    return new ThedalResponse<>(ThedalSuccess.AVAILABILITY_UPDATED, updatedAvailability);
	}


//	@Operation(
//		    summary = "Delete availability for the control panel",
//		    description = "Delete an availability entry for election ID 0.",
//		    		tags = { "Sub Model Cpanel Controller" }
//		)
//		@DeleteMapping("/cpanel/availability/{availabilityId}")
//		public ThedalResponse<Void> deleteAvailabilityCpanel(@PathVariable Long availabilityId) {
//
//		    Long accountId = requestDetails.getCurrentAccountId();
//
//		    if (accountId == null) {
//		        log.error("Account ID not found, unauthorized access.");
//		        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
//		    }
//
//		    // Force electionId = 0 and accountId = 0 for control panel requests
//		    Long electionId = 0L;
//		    accountId = 0L;
//
//		    subModelCpanelService.deleteAvailability(accountId, electionId, availabilityId);
//
//		    return new ThedalResponse<>(ThedalSuccess.AVAILABILITY_DELETED, null);
//		}
	@Operation(
		    summary = "Delete availabilities for the control panel",
		    description = "Delete one, multiple, or all availability entries for election ID 0. Use availabilityIds parameter for specific availabilities or omit for all.",
		    tags = { "Sub Model Cpanel Controller" }
		)
		@DeleteMapping("/cpanel/availability")
		public ThedalResponse<Void> deleteAvailabilityCpanel(
		        @RequestParam(value = "availabilityIds", required = false) List<Long> availabilityIds) {

		    Long accountId = requestDetails.getCurrentAccountId();

		    if (accountId == null) {
		        log.error("Account ID not found, unauthorized access.");
		        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
		    }

		    // Force electionId = 0 and accountId = 0 for control panel requests
		    Long electionId = 0L;
		    accountId = 0L;

		    subModelCpanelService.deleteAvailability(accountId, electionId, availabilityIds);

		    return new ThedalResponse<>(ThedalSuccess.AVAILABILITY_DELETED, null);
		}
	

//////////  benefit schemes api's
	
	@Operation(
		    summary = "Create Benefit Scheme",
		    description = "Saves a benefit scheme with an optional image. accountId and electionId are set to 0 unless the request comes from /api/cpanel",
		    tags = { "Sub Model Cpanel Controller" }
		)
		@PostMapping("/cpanel/benefit-scheme")
		public ThedalResponse<BenefitSchemesDTO> createBenefitScheme(
		        @RequestPart("benefit") BenefitSchemesUpdateDTO benefitSchemesDTO,
		        @RequestPart(value = "file", required = false) MultipartFile file,
		        HttpServletRequest request) {

		    return subModelCpanelService.createBenefitScheme(benefitSchemesDTO, file, request);
		}

	@Operation(
		    summary = "Get Benefit Scheme",
		    description = "Get a benefit scheme with an optional image. accountId and electionId are set to 0 unless the request comes from /api/cpanel",
		    tags = { "Sub Model Cpanel Controller" }
		)
	@GetMapping("/cpanel/benefit-schemes")
	public ResponseEntity<ThedalResponse<List<BenefitSchemesDTO>>> getAllBenefitSchemes(HttpServletRequest request) {
	    
	    // Ensure the request comes from "/api/cpanel"
	    if (!request.getRequestURI().contains("/api/cpanel")) {
	        log.error("Unauthorized access to benefit schemes. Only accessible from /api/cpanel");
	        throw new ThedalException(ThedalError.UNAUTHORIZED_ACCESS, HttpStatus.FORBIDDEN);
	    }

	    // Hardcoded values for cpanel access
	    Long accountId = 0L;
	    Long electionId = 0L;

	    ThedalResponse<List<BenefitSchemesDTO>> response = subModelCpanelService.getAll(accountId, electionId);

	    return ResponseEntity.ok(response);
	}

	@Operation(
		    summary = "Update Benefit Scheme",
		    description = "Update a benefit scheme with an optional image. accountId and electionId are set to 0 unless the request comes from /api/cpanel",
		    tags = { "Sub Model Cpanel Controller" }
		)
	@PutMapping("/cpanel/benefit-schemes/{benefitSchemeId}")
	public ThedalResponse<BenefitSchemesDTO> updateBenefitScheme(
	        HttpServletRequest request,
	        @PathVariable Long benefitSchemeId,
	        @RequestPart("benefit") BenefitSchemesUpdateDTO benefitSchemesupdateDTO,
	        @RequestPart(value = "file", required = false) MultipartFile file) {
	    
	    // Ensure the request is from /api/cpanel
	    if (!request.getRequestURI().contains("/api/cpanel")) {
	        log.error("Unauthorized access to update benefit schemes. Only accessible from /api/cpanel");
	        throw new ThedalException(ThedalError.UNAUTHORIZED_ACCESS, HttpStatus.FORBIDDEN);
	    }

	    // Hardcoded values for cpanel access
	    Long accountId = 0L;
	    Long electionId = 0L;

	    return subModelCpanelService.updateBenefitScheme(benefitSchemeId, electionId, benefitSchemesupdateDTO, file);
	}

	@Operation(
		    summary = "Delete Benefit Scheme",
		    description = "Delete a benefit scheme with an optional image. accountId and electionId are set to 0 unless the request comes from /api/cpanel",
		    tags = { "Sub Model Cpanel Controller" }
		)
	@DeleteMapping("/cpanel/benefit-schemes")
	public ThedalResponse<Void> deleteBenefitScheme(HttpServletRequest request,
	@RequestParam(value = "benefitSchemeIds", required = false) List<Long> benefitSchemeIds) {
	    
	    // Ensure the request is from /api/cpanel
	    if (!request.getRequestURI().contains("/api/cpanel")) {
	        log.error("Unauthorized access to delete benefit schemes. Only accessible from /api/cpanel");
	        throw new ThedalException(ThedalError.UNAUTHORIZED_ACCESS, HttpStatus.FORBIDDEN);
	    }

	    // Hardcoded values for cpanel access
	    Long accountId = 0L;
	    Long electionId = 0L;

	    subModelCpanelService.deleteBenefitScheme(accountId, electionId, benefitSchemeIds);

	    return new ThedalResponse<>(ThedalSuccess.BENEFITSCHEME_DELETED, null);
	}
	
	@Operation(
	        summary = "Create feedback issues for cPanel",
	        description = "Saves multiple feedback issues for election ID 0 and account ID 0.",
	        		tags = { "Sub Model Cpanel Controller" }
	    )
	    @PostMapping("/cpanel/feedback")
	    public ThedalResponse<List<FeedbackIssueResponseDTO>> createFeedbackIssues(
	            @RequestBody List<FeedbackIssueRequest> feedbackRequests) {
	        Long accountId = requestDetails.getCurrentAccountId();
	        if (accountId == null) {
	            log.error("Account ID not found, unauthorized access.");
	            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
	        }

	        Long electionId = 0L;
	        accountId = 0L;

	        List<FeedbackIssueResponseDTO> createdFeedbackIssues = subModelCpanelService.createFeedbackIssues(feedbackRequests, electionId, accountId);
	        return new ThedalResponse<>(ThedalSuccess.ISSUE_CREATED, createdFeedbackIssues);
	    }
	
	@Operation(
	        summary = "Get feedback issues for cPanel",
	        description = "Fetches feedback issues for election ID 0 and account ID 0.",
	        		tags = { "Sub Model Cpanel Controller" }
	    )
	    @GetMapping("/cpanel/feedback")
	    public ThedalResponse<List<FeedbackIssueResponseDTO>> getFeedbackIssues() {
	        Long accountId = requestDetails.getCurrentAccountId();
	        if (accountId == null) {
	            log.error("Account ID not found, unauthorized access.");
	            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
	        }

	        Long electionId = 0L;
	        accountId = 0L;

	        List<FeedbackIssueResponseDTO> feedbackIssues = subModelCpanelService.getFeedbackIssues(accountId, electionId);
	        return new ThedalResponse<>(ThedalSuccess.ISSUE_FOUND, feedbackIssues);
	    }
	
	@Operation(
	        summary = "Update feedback issues for cPanel",
	        description = "Updates multiple feedback issues for election ID 0 and account ID 0.",
	        		tags = { "Sub Model Cpanel Controller" }
	    )
	    @PutMapping("/cpanel/feedback")
	    public ThedalResponse<List<FeedbackIssueResponseDTO>> updateFeedbackIssues(
	            @RequestBody List<FeedbackIssueUpdateRequest> feedbackUpdateRequests) {
	        Long accountId = requestDetails.getCurrentAccountId();
	        if (accountId == null) {
	            log.error("Account ID not found, unauthorized access.");
	            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
	        }

	        Long electionId = 0L;
	        accountId = 0L;

	        List<FeedbackIssueResponseDTO> updatedFeedbackIssues = subModelCpanelService.updateFeedbackIssues(accountId, electionId, feedbackUpdateRequests);
	        return new ThedalResponse<>(ThedalSuccess.ISSUE_UPDATED, updatedFeedbackIssues);
	    }
	
	@Operation(
	        summary = "Delete feedback issues for cPanel",
	        description = "Deletes feedback issues by IDs or all issues for election ID 0 and account ID 0.",
	        		tags = { "Sub Model Cpanel Controller" }
	    )
	    @DeleteMapping("/cpanel/feedback")
	    public ThedalResponse<Void> deleteFeedbackIssues(
	            @RequestParam(value = "issueIds", required = false) List<Long> issueIds) {
	        Long accountId = requestDetails.getCurrentAccountId();
	        if (accountId == null) {
	            log.error("Account ID not found, unauthorized access.");
	            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
	        }

	        Long electionId = 0L;
	        accountId = 0L;

	        subModelCpanelService.deleteFeedbackIssues(accountId, electionId, issueIds);
	        return new ThedalResponse<>(ThedalSuccess.ISSUE_DELETED, null);
	    }
	
//	@Operation(summary = "Activate User Cpanel",
//	 description = "Activates the user control panel for a given user.",
//	 tags = { "Sub Model Cpanel Controller" }
//	 )
//	@PutMapping("cpanel/{userId}/activate")
//    public ThedalResponse<String> activateUserCpanel(@PathVariable Long userId) {
//		Long accountId = requestDetails.getCurrentAccountId();
//
//	    if (accountId == null) {
//	        log.error("Account ID not found, unauthorized access.");
//	        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
//	    }
//
//		 accountId = 0L;
//	    Long electionId = 0L;
//        return subModelCpanelService.activateUserCpanel(userId);
//    }
	@Operation(summary = "Activate User Cpanel",
            description = "Activates the user control panel for a given user with an optional expiry date.",
            tags = { "Sub Model Cpanel Controller" })
    @PutMapping("cpanel/{userId}/activate")
    public ThedalResponse<String> activateUserCpanel(@PathVariable Long userId, 
                                             @RequestBody ActivateUserRequestDto request) {
        Long accountId = requestDetails.getCurrentAccountId();

        if (accountId == null) {
            log.error("Account ID not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        log.info("Activating Cpanel for userId: {} with expiryAt: {}", userId, request.getExpiryAt());
        return subModelCpanelService.activateUserCpanel(userId, request.getExpiryAt());
    }

	
//////////// slipBox api's

	@Operation(summary = "Add slip boxes",
            description = "Adds multiple slip boxes for cPanel.",
            tags = { "Sub Model Cpanel Controller" })
 @PostMapping("/cpanel/slip-box")
 public ThedalResponse<List<SlipBoxDTO>> createSlipBoxes(@Valid @RequestBody List<SlipBoxDTO> slipBoxDTOs) {
     Long accountId = requestDetails.getCurrentAccountId();
     if (accountId == null) {
         log.error("Account ID not found, unauthorized access.");
         throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
     }

     return subModelCpanelService.createSlipBoxes(slipBoxDTOs);
 }

// @Operation(summary = "Bulk upload slip boxes",
//            description = "Uploads slip boxes via CSV file.",
//            tags = { "Sub Model Cpanel Controller" })
// @PostMapping("/cpanel/slip-box/bulk-upload")
// public ThedalResponse<List<SlipBoxDTO>> bulkUploadSlipBoxes(@RequestParam("file") MultipartFile file) {
//     Long accountId = requestDetails.getCurrentAccountId();
//     if (accountId == null) {
//         log.error("Account ID not found, unauthorized access.");
//         throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
//     }
//
//     if (file.isEmpty()) {
//         log.error("Uploaded file is empty");
//         throw new ThedalException(ThedalError.INVALID_FILE, HttpStatus.BAD_REQUEST);
//     }
//
//     return subModelCpanelService.bulkUploadSlipBoxes(file);
// }

 @Operation(summary = "Get all slip boxes",
            description = "Retrieves all slip boxes for cPanel.",
            tags = { "Sub Model Cpanel Controller" })
 @GetMapping("/cpanel/slip-box")
 public ThedalResponse<List<SlipBoxDTO>> getSlipBoxes() {
     Long accountId = requestDetails.getCurrentAccountId();
     if (accountId == null) {
         log.error("Account ID not found, unauthorized access.");
         throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
     }

     return subModelCpanelService.getSlipBoxes();
 }

 @Operation(summary = "Update a slip box",
            description = "Updates a slip box by ID for cPanel.",
            tags = { "Sub Model Cpanel Controller" })
 @PutMapping("/cpanel/slip-box/{slipBoxId}")
 public ThedalResponse<SlipBoxDTO> updateSlipBox(
         @PathVariable Long slipBoxId,
         @Valid @RequestBody SlipBoxDTO slipBoxDTO) {
     Long accountId = requestDetails.getCurrentAccountId();
     if (accountId == null) {
         log.error("Account ID not found, unauthorized access.");
         throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
     }

     return subModelCpanelService.updateSlipBox(slipBoxId, slipBoxDTO);
 }

 @Operation(summary = "Delete slip boxes",
            description = "Deletes all or specific slip boxes for cPanel.",
            tags = { "Sub Model Cpanel Controller" })
 @DeleteMapping("/cpanel/slip-box")
 public ThedalResponse<Void> deleteSlipBoxes(
         @RequestParam(value = "slipBoxIds", required = false) List<Long> slipBoxIds) {
     Long accountId = requestDetails.getCurrentAccountId();
     if (accountId == null) {
         log.error("Account ID not found, unauthorized access.");
         throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
     }

     return subModelCpanelService.deleteSlipBoxes(slipBoxIds);
 }
 
 @Operation(summary = "Upload bulk Slip Boxes for cPanel", description = "Upload bulk Slip Boxes with mobile_number, slip_box_name, and slip_box_id for cPanel with electionId=0 and accountId=0 using xlsx or csv files.",
         tags = { "Sub Model Cpanel Controller" })
 @PostMapping(value = "/cpanel/slip-box-upload", consumes = "multipart/form-data")
 public ResponseEntity<ThedalResponse<SectionBulkUploadEntity>> uploadCpanelSlipBoxes(
         @RequestParam("file") MultipartFile file) throws IOException {
     if (file.getSize() > 100 * 1024 * 1024) { // 100 MB limit
         throw new ThedalException(ThedalError.FILE_TOO_LARGE, HttpStatus.BAD_REQUEST);
     }
     ThedalResponse<SectionBulkUploadEntity> response = subModelCpanelService.uploadCpanelSlipBoxesService(file);
     return ResponseEntity.ok(response);
 }

	
	
}
