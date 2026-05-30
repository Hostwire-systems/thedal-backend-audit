package com.thedal.thedal_app.settings.electionsettings;

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

import com.thedal.thedal_app.general.RequestDetailsService;
import com.thedal.thedal_app.response.ThedalResponse;
import com.thedal.thedal_app.response.ThedalSuccess;
import com.thedal.thedal_app.settings.electionsettings.dto.BoothVulnerabilityRequest;
import com.thedal.thedal_app.settings.electionsettings.dto.CasteReorderRequest;
import com.thedal.thedal_app.settings.electionsettings.dto.CasteRequest;
import com.thedal.thedal_app.settings.electionsettings.dto.CasteResponseDTO;
import com.thedal.thedal_app.settings.electionsettings.dto.CasteUpdateRequest;
import com.thedal.thedal_app.settings.electionsettings.dto.ElectionTypeRequest;
import com.thedal.thedal_app.settings.electionsettings.dto.PartyReorderRequest;
import com.thedal.thedal_app.settings.electionsettings.dto.PartyRequest;
import com.thedal.thedal_app.settings.electionsettings.dto.PartyResponseDTO;
import com.thedal.thedal_app.settings.electionsettings.dto.ReligionReorderRequest;
import com.thedal.thedal_app.settings.electionsettings.dto.ReligionRequest;
import com.thedal.thedal_app.settings.electionsettings.dto.ReligionResponseDTO;
import com.thedal.thedal_app.settings.electionsettings.dto.ReligionUpdateRequest;
import com.thedal.thedal_app.settings.electionsettings.dto.SubCasteReorderRequest;
import com.thedal.thedal_app.settings.electionsettings.dto.SubCasteRequest;
import com.thedal.thedal_app.settings.electionsettings.dto.SubCasteResponseDTO;
import com.thedal.thedal_app.settings.electionsettings.dto.SubCasteUpdateRequest;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/election-settings")
@Slf4j
public class ElectionSettingController {

    @Autowired
    private ReligionService religionService;
    @Autowired
    private CasteService casteService;
    @Autowired
    private SubCasteService subCasteService;
    
    @Autowired
    private RequestDetailsService requestDetails;
    @Autowired
	private ElectionTypeService electionTypeService;
    @Autowired
	private PartyService partyService;
    @Autowired
	private BoothVulnerabilityService boothVulnerabilityService;
    
    @Operation(summary = "Create a new religion", description = "Saves a new religion with an image", tags = { "Election Settings" })
    @PostMapping(value = "/religions/{electionId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ThedalResponse<ReligionEntity> createReligion(
    		@PathVariable("electionId") Long electionId, 
            @RequestPart("religionName") String religionName,
            @RequestPart("religionImage") MultipartFile religionImage,
            @RequestPart("religionColor") String religionColor) {

                Long accountId = requestDetails.getCurrentAccountId();
                if (accountId == null) {
                    log.error("Account ID not found, unauthorized access.");
                    throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
                }

        ReligionRequest religionRequest = new ReligionRequest();
        religionRequest.setReligionName(religionName);
        religionRequest.setReligionImage(religionImage);
        religionRequest.setReligionColor(religionColor);

        return religionService.createReligion(religionRequest, electionId,accountId);
    }

    
//    @Operation(summary = "Get all religions", description = "Fetch all religions for a specific election", tags = { "Election Settings" }) 
//    @GetMapping("/religions/{electionId}")
//    public ThedalResponse<List<Map<String, Object>>> getAllReligions(@PathVariable Long electionId) {
//
//        Long accountId = requestDetails.getCurrentAccountId();
//        if (accountId == null) {
//            log.error("Account ID not found, unauthorized access.");
//            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
//        }
//
//        log.info("Fetching all religions for account ID: {} and election ID: {}", accountId, electionId);
//
//        List<ReligionEntity> religions = religionService.getAllReligions(accountId, electionId);
//
//        // If no religions are found, throw an error
//        if (religions.isEmpty()) {
//            log.error("No religions found for account ID: {} and election ID: {}", accountId, electionId);
//            throw new ThedalException(ThedalError.RELIGIONS_NOT_FOUND, HttpStatus.NOT_FOUND);
//        }
//
//        List<Map<String, Object>> religionDetails = religions.stream()
//                .map(religion -> {
//                    Map<String, Object> religionData = new HashMap<>();
//                    religionData.put("religionId", religion.getId());
//                    religionData.put("religionName", religion.getReligionName());
//                    religionData.put("religionImage", religion.getReligionImage());
//                    religionData.put("orderIndex", religion.getOrderIndex());
//                    return religionData;
//                }).collect(Collectors.toList());
//
//        return new ThedalResponse<>(ThedalSuccess.RELIGIONS_FETCHED, religionDetails);
//    }
    @Operation(summary = "Get all religions", description = "Fetch all religions with voter counts for a specific election", tags = {"Election Settings"})
    @GetMapping("/religions/{electionId}")
    public ThedalResponse<List<Map<String, Object>>> getAllReligions(@PathVariable Long electionId) {
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account ID not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        log.info("Fetching all religions from PostgreSQL for account ID: {} and election ID: {}", accountId, electionId);
        // Use PostgreSQL service for consistency
        return religionService.getAllReligionsWithVoterCount(accountId, electionId);
    }
    
    @Operation(summary = "Delete religion", description = "Delete a religion by ID and Election ID", tags = { "Election Settings" }) 
    @DeleteMapping("/religions/{electionId}")
    public ThedalResponse<Void> deleteReligion(@PathVariable("electionId") Long electionId, 
    @RequestParam(value = "religionIds", required = false) List<Long> religionIds) {
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account ID not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        log.info("Received request to delete religion with ID: {} for election ID: {} and account ID: {}", religionIds, electionId, accountId);

        religionService.deleteReligionsByIdsAndAccountIdAndElectionId(religionIds, accountId, electionId);
        
        return new ThedalResponse<>(ThedalSuccess.RELIGION_DELETED, null);
    }
    
    @Operation(summary = "Update a religion", description = "Updates an existing religion", tags = { "Election Settings" }) 
    @PutMapping(value = "/religions/{electionId}/{religionId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ThedalResponse<ReligionResponseDTO> updateReligion(           
            @PathVariable("electionId") Long electionId,
            @PathVariable("religionId") Long religionId,
            @RequestPart("religionName") String religionName,
            @RequestPart(value = "religionImage", required = false) MultipartFile religionImage,
            @RequestPart(value = "religionColor", required = false) String religionColor) {

        // Create a ReligionUpdateRequest object to pass the updated data
        ReligionUpdateRequest religionRequest = new ReligionUpdateRequest();
        religionRequest.setReligionName(religionName);
        religionRequest.setReligionImage(religionImage);
        religionRequest.setReligionColor(religionColor);

        Long accountId = requestDetails.getCurrentAccountId();

        if (accountId == null) {
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        // Call the service method to update religion
        ReligionResponseDTO updatedReligion = religionService.updateReligion(religionId, electionId, accountId, religionRequest);
        return new ThedalResponse<>(ThedalSuccess.RELIGION_UPDATED, updatedReligion);
    }

    
//    @Operation(summary = "Reorder religion", description = "Update religion with new index value", tags = { "Election Settings" })  
//    @PutMapping("/reorder/{electionId}")
//    public ResponseEntity<ThedalResponse<String>> updateReligionOrder(
//            @PathVariable Long electionId,
//            @RequestBody ReligionReorderDTO request) {
//
//        log.info("Received religion order update for electionId: {} with religionId: {} to new index: {}", 
//                 electionId, request.getReligionId(), request.getNewIndex());
//
//        Long accountId = requestDetails.getCurrentAccountId();
//        if (accountId == null) {
//            log.error("Account ID not found, unauthorized access.");
//            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
//        }
//
//        religionService.updateReligionOrder(electionId, accountId, request.getReligionId(), request.getNewIndex());
//
//        // Return success message only
//        return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.RELIGION_REORDERING));
//    }
    @Operation(
    	    summary = "Reorder religions", 
    	    description = "Update the order of religions for a specific election", 
    	    tags = { "Election Settings" }
    	)
    	@PutMapping("/reorder/{electionId}")
    	public ResponseEntity<ThedalResponse<List<ReligionEntity>>> updateReligionOrder(
    	        @PathVariable Long electionId,
    	        @RequestBody List<ReligionReorderRequest> reorderRequests) {

    	    log.info("Received request to reorder religions for electionId: {}", electionId);

    	    // Get current account ID
    	    Long accountId = requestDetails.getCurrentAccountId();
    	    if (accountId == null) {
    	        log.error("Account ID not found, unauthorized access.");
    	        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
    	    }

    	    // Call the service method to update the religion order
    	    List<ReligionEntity> reorderedReligions = religionService.updateReligionOrder(reorderRequests, electionId, accountId);

    	    // Return the updated list of religions in the response
    	    return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.RELIGION_REORDERING, reorderedReligions));
    	}

    @Operation(summary = "Get all religions from MongoDB", description = "Fetch all religions from MongoDB for a specific election",
    		 tags = { "Election Settings" })
    @GetMapping("/religions/{electionId}/mongo")
    public ThedalResponse<List<Map<String, Object>>> getAllReligionsFromMongo(@PathVariable Long electionId) {
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account ID not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        log.info("Fetching all religions from MongoDB for account ID: {} and election ID: {}", accountId, electionId);
        return religionService.getAllReligionsWithVoterCountFromMongo(accountId, electionId);
    }
    
 ////////////////////////////////////////   

    @Operation(summary = "Create a new caste", description = "Saves a new caste", tags = { "Election Settings" })
    @PostMapping("/castes/{electionId}")
    public ThedalResponse<CasteEntity> createCaste(
            @PathVariable Long electionId,
            @RequestBody @Valid CasteRequest casteRequest) {
        
        CasteEntity casteEntity = casteService.createCaste(casteRequest, electionId);
        return new ThedalResponse<>(ThedalSuccess.CASTE_CREATED, casteEntity);
    }
    
//    @Operation(summary = "Get castes", description = "Fetch castes for a specific religion and election", tags = { "Election Settings" }) 
//    @GetMapping("/castes/{electionId}")
//    public ThedalResponse<List<Map<String, Object>>> getCasteByReligion(
//            @PathVariable Long electionId,
//            @RequestParam(required = false) Long religionId) {
//        
//        List<CasteEntity> castes = casteService.getCasteByReligion(religionId, electionId);
//        
//        if (castes.isEmpty()) {
//            log.error("No castes found for religionId: {} and electionId: {}", religionId, electionId);
//            throw new ThedalException(ThedalError.CASTES_NOT_FOUND, HttpStatus.NOT_FOUND);
//        }
//        
//        List<Map<String, Object>> casteDetails = castes.stream()
//                .map(caste -> {
//                    Map<String, Object> casteData = new HashMap<>();
//                    casteData.put("casteId", caste.getId());
//                    casteData.put("religionId", caste.getReligion().getId());
//                    casteData.put("religionName", caste.getReligion().getReligionName());
//                    casteData.put("casteName", caste.getCasteName());
//                    casteData.put("electionId", caste.getElectionId());  // Include electionId in response
//                    casteData.put("orderIndex", caste.getOrderIndex());
//                    return casteData;
//                }).collect(Collectors.toList());
//
//        return new ThedalResponse<>(ThedalSuccess.CASTES_FETCHED, casteDetails);
//    }
    @Operation(summary = "Get castes", description = "Fetch castes with voter counts for a specific religion and election", tags = {"Election Settings"})
    @GetMapping("/castes/{electionId}")
    public ThedalResponse<List<Map<String, Object>>> getCasteByReligion(
            @PathVariable Long electionId,
            @RequestParam(required = false) Long religionId) {
        
        log.info("Fetching castes from PostgreSQL for religionId: {}, electionId: {}", religionId, electionId);
        // Use PostgreSQL service for consistency
        return casteService.getCasteByReligion(religionId, electionId);
    }
    
    @Operation(summary = "Delete caste", description = "Delete a caste by ID and election", tags = { "Election Settings" }) 
@DeleteMapping("/castes/{electionId}")
public ThedalResponse<Void> deleteCaste(
        @PathVariable("electionId") Long electionId,
        @RequestParam(value = "casteIds", required = false) List<Long> casteIds) {

    Long accountId = requestDetails.getCurrentAccountId();
    if (accountId == null) {
        log.error("Account ID not found, unauthorized access.");
        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
    }

    log.info("Deleting castes {} for accountId: {} and electionId: {}", casteIds, accountId, electionId);

    casteService.deleteCasteByIdAndAccountId(casteIds, accountId, electionId);
    return new ThedalResponse<>(ThedalSuccess.CASTE_DELETED, null);
}
    
    @Operation(summary = "Update caste", description = "Update an existing caste for a specific election", tags = { "Election Settings" }) 
    @PutMapping("/castes/{electionId}/{casteId}")
    public ThedalResponse<CasteResponseDTO> updateCaste(
            @PathVariable("electionId") Long electionId,
            @PathVariable("casteId") Long casteId,
            @RequestBody CasteUpdateRequest request) {

        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account ID not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        CasteResponseDTO updatedCaste = casteService.updateCaste(casteId, accountId, request, electionId);
        return new ThedalResponse<>(ThedalSuccess.CASTE_UPDATED, updatedCaste);
    }   
   
    @Operation(summary = "Reorder Castes", description = "Update multiple Castes with new order indices", tags = { "Election Settings" })
    @PutMapping("/castes/reorder/{electionId}")
    public ResponseEntity<ThedalResponse<String>> updateCasteOrder(
            @PathVariable Long electionId,
            @RequestBody List<CasteReorderRequest> reorderRequests) {

        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        casteService.updateCasteOrder(reorderRequests, electionId, accountId);

        return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.CASTE_REORDERING));
    }
    
    @Operation(summary = "Get castes from MongoDB", description = "Fetch castes from MongoDB for a specific religion and election", tags = { "Election Settings" })
    @GetMapping("/castes/{electionId}/mongo")
    public ThedalResponse<List<Map<String, Object>>> getCasteByReligionFromMongo(
            @PathVariable Long electionId,
            @RequestParam(required = false) Long religionId) {
        log.info("Fetching castes from MongoDB for religionId: {}, electionId: {}", religionId, electionId);
        return casteService.getCasteByReligionFromMongo(religionId, electionId);
    }

    @Operation(summary = "Upload bulk Caste Data", description = "Upload bulk Caste data using xlsx or csv files.", tags = {"Election Settings"})
    @PostMapping(value = "/castes/{electionId}/bulk-upload", consumes = "multipart/form-data")
    public ResponseEntity<ThedalResponse<Map<String, Object>>> uploadCastesFromXlsxOrCsv(
            @RequestParam("file") MultipartFile file,
            @PathVariable Long electionId) throws IOException {

        // Check file size (max 100 MB, consistent with PartManager)
        if (file.getSize() > 100 * 1024 * 1024) {
            throw new ThedalException(ThedalError.FILE_TOO_LARGE, HttpStatus.BAD_REQUEST);
        }

        // Call the service to handle the file processing and upload
        ThedalResponse<Map<String, Object>> response = casteService.bulkUploadCastesFromXlsxOrCsv(file, electionId);
        return ResponseEntity.ok(response);
    }
    
///////////////////////////////////////
    
    
    @Operation(summary = "Create a new sub-caste", description = "Saves a new sub-caste", tags = { "Election Settings" })
    @PostMapping("/subcastes/{electionId}")
    public ThedalResponse<SubCasteEntity> createSubCaste(
            @PathVariable("electionId") Long electionId,
            @RequestBody @Valid SubCasteRequest subCasteRequest) {
        
        ThedalResponse<SubCasteEntity> subCasteEntity = subCasteService.createSubCaste(subCasteRequest, electionId);
        return new ThedalResponse<>(ThedalSuccess.SUB_CASTE_CREATED);
    }

//    @Operation(summary = "Get subcastes", description = "Fetch subcastes based on religion and caste", tags = { "Election Settings" })
//    @GetMapping("/elections/{electionId}/subcastes")
//    public ThedalResponse<List<Map<String, Object>>> getSubCasteByReligionAndCaste(
//            @PathVariable Long electionId,
//            @RequestParam(required = false) Long religionId,
//            @RequestParam(required = false) Long casteId) {
//
//        List<SubCasteEntity> subcastes = subCasteService.getSubCasteByReligionAndCaste(religionId, casteId, electionId);
//
//        if (subcastes.isEmpty()) {
//            log.error("No subcastes found for electionId: {}, religionId: {}, and casteId: {}", electionId, religionId, casteId);
//            throw new ThedalException(ThedalError.SUBCASTES_NOT_FOUND, HttpStatus.NOT_FOUND);
//        }
//
//        List<Map<String, Object>> subCasteDetails = subcastes.stream()
//                .map(subCaste -> {
//                    Map<String, Object> subCasteData = new HashMap<>();
//                    subCasteData.put("subCasteId", subCaste.getId());
//                    subCasteData.put("subCasteName", subCaste.getSubCasteName());
//                    subCasteData.put("casteId", subCaste.getCaste() != null ? subCaste.getCaste().getId() : null);
//                    subCasteData.put("casteName", subCaste.getCaste().getCasteName());
//                    subCasteData.put("religionId", subCaste.getReligion() != null ? subCaste.getReligion().getId() : null);
//                    subCasteData.put("religionName", subCaste.getReligion().getReligionName());
//                    subCasteData.put("orderIndex", subCaste.getOrderIndex());
//                    return subCasteData;
//                })
//                .sorted(Comparator.comparingInt(subCaste -> (Integer) subCaste.get("orderIndex")))
//                .collect(Collectors.toList());
//
//        return new ThedalResponse<>(ThedalSuccess.SUBCASTES_FETCHED, subCasteDetails);
//    }
    
    @Operation(summary = "Get subcastes", description = "Fetch subcastes with voter counts based on religion and caste", tags = {"Election Settings"})
    @GetMapping("/elections/{electionId}/subcastes")
    public ThedalResponse<List<Map<String, Object>>> getSubCasteByReligionAndCaste(
            @PathVariable Long electionId,
            @RequestParam(required = false) Long religionId,
            @RequestParam(required = false) Long casteId) {

        log.info("Fetching subcastes from PostgreSQL for electionId: {}, religionId: {}, casteId: {}", electionId, religionId, casteId);
        // Use PostgreSQL service for consistency
        return subCasteService.getSubCasteByReligionAndCaste(religionId, casteId, electionId);
    }
    
    @Operation(summary = "Delete subcaste", description = "Delete a subcaste by ID and election ID", tags = { "Election Settings" })
    @DeleteMapping("/subcastes/{electionId}")
    public ThedalResponse<Void> deleteSubCaste(
            @PathVariable("electionId") Long electionId,
            @RequestParam(value = "subCasteIds", required = false) List<Long> subCasteIds) {
    
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account ID not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }
    
        log.info("Request received to delete subcastes: {} for Account ID: {} and Election ID: {}", 
                subCasteIds, accountId, electionId);
    
        subCasteService.deleteSubCasteByIdAndAccountIdAndElectionId(subCasteIds, accountId, electionId);
    
        return new ThedalResponse<>(ThedalSuccess.SUBCASTE_DELETED, null);
    }
    

    @Operation(summary = "Update sub-caste", description = "Update an existing sub-caste with election ID", tags = { "Election Settings" })
    @PutMapping("/subcastes/{electionId}/{subCasteId}")
    public ThedalResponse<SubCasteResponseDTO> updateSubCaste(
    		@PathVariable("electionId") Long electionId,
            @PathVariable("subCasteId") Long subCasteId,            
            @RequestBody SubCasteUpdateRequest request) {

        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account ID not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        SubCasteResponseDTO updatedSubCaste = subCasteService.updateSubCaste(subCasteId, accountId, electionId, request);
        return new ThedalResponse<>(ThedalSuccess.SUBCASTE_UPDATED, updatedSubCaste);
    }

    @Operation(summary = "Reorder Sub-Castes", description = "Update Sub-Castes with new index values", tags = { "Election Settings" })   
    @PutMapping("/subcastes/reorder/{electionId}")
    public ResponseEntity<ThedalResponse<String>> updateSubCasteOrder(
            @PathVariable Long electionId,
            @RequestBody List<SubCasteReorderRequest> request) {

        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        subCasteService.updateSubCasteOrder(electionId, accountId, request);

        return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.SUBCASTE_REORDERED));
    }

    @Operation(summary = "Get subcastes from MongoDB", description = "Fetch subcastes from MongoDB based on religion and caste", tags = {"Election Settings"})
    @GetMapping("/elections/{electionId}/subcastes/mongo")
    public ThedalResponse<List<Map<String, Object>>> getSubCasteByReligionAndCasteFromMongo(
            @PathVariable Long electionId,
            @RequestParam(required = false) Long religionId,
            @RequestParam(required = false) Long casteId) {
        log.info("Fetching subcastes from MongoDB for electionId: {}, religionId: {}, casteId: {}", electionId, religionId, casteId);
        return subCasteService.getSubCasteByReligionAndCasteFromMongo(religionId, casteId, electionId);
    }

    @Operation(summary = "Upload bulk Sub-Caste Data", description = "Upload bulk Sub-Caste data using xlsx or csv files.", tags = {"Election Settings"})
    @PostMapping(value = "/subcastes/{electionId}/bulk-upload", consumes = "multipart/form-data")
    public ResponseEntity<ThedalResponse<Map<String, Object>>> uploadSubCastesFromXlsxOrCsv(
            @RequestParam("file") MultipartFile file,
            @PathVariable Long electionId) throws IOException {

        // Check file size (max 100 MB, consistent with Caste and PartManager)
        if (file.getSize() > 100 * 1024 * 1024) {
            throw new ThedalException(ThedalError.FILE_TOO_LARGE, HttpStatus.BAD_REQUEST);
        }

        // Call the service to handle the file processing and upload
        ThedalResponse<Map<String, Object>> response = subCasteService.bulkUploadSubCastesFromXlsxOrCsv(file, electionId);
        return ResponseEntity.ok(response);
    }
    
/////// Election Type api's
    
    @Operation(summary = "Create a new Election Type", description = "Saves a new election type")
    @PostMapping("/election-types")
    public ThedalResponse<ElectionType> createElectionType(@RequestBody @Valid ElectionTypeRequest electionTypeRequest) {
        ThedalResponse<ElectionType> response = electionTypeService.createElectionType(electionTypeRequest);
        return new ThedalResponse<>(ThedalSuccess.ELECTION_TYPE_CREATED, response.getData());
    }    

    @Operation(summary = "Get Election Type by ID", description = "Fetches the election type by ID")
    @GetMapping("/election-types/{id}")
    public ThedalResponse<ElectionType> getElectionTypeById(@PathVariable Long id) {
    	Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account ID not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }
        
    	ElectionType electionType = electionTypeService.getElectionTypeById(id, accountId);

        if (electionType == null) {
            throw new ThedalException(ThedalError.ELECTION_TYPE_NOT_FOUND, HttpStatus.NOT_FOUND);
        }
        return new ThedalResponse<>(ThedalSuccess.ELECTION_TYPE_FETCHED, electionType);
    }
    
    @Operation(summary = "Get All Election Types", description = "Fetches all election types for the current account")
    @GetMapping("/election-types")
    public ThedalResponse<List<ElectionType>> getAllElectionTypes() {
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account ID not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        List<ElectionType> electionTypes = electionTypeService.getAllElectionTypes(accountId);

        if (electionTypes.isEmpty()) {
            throw new ThedalException(ThedalError.ELECTION_TYPES_NOT_FOUND, HttpStatus.NOT_FOUND);
        }

        return new ThedalResponse<>(ThedalSuccess.ELECTION_TYPES_FETCHED, electionTypes);
    }


    @Operation(summary = "Update an existing ElectionType", description = "Updates an existing ElectionType")
    @PutMapping("/election-types/{id}")
    public ThedalResponse<ElectionType> updateElectionType(@PathVariable Long id, @RequestBody @Valid ElectionTypeRequest electionTypeRequest) {
        ElectionType electionType = electionTypeService.updateElectionType(id, electionTypeRequest);
        return new ThedalResponse<>(ThedalSuccess.ELECTION_TYPE_UPDATED, electionType);
    }

    @Operation(summary = "Delete an ElectionType", description = "Deletes an ElectionType by ID and account ID")
    @DeleteMapping("/election-types/{id}")
    public ThedalResponse<Void> deleteElectionType(@PathVariable Long id) {
    	return electionTypeService.deleteElectionType(id);
         
    }

 ///////// Party api's 
    
    @Operation(summary = "Create a new party", description = "Saves a new party")
    @PostMapping(value = "/parties/{electionId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ThedalResponse<Party> createParty(
    		@PathVariable("electionId") Long electionId,
            @RequestPart("partyName") String partyName,
            @RequestPart("partyShortName") String partyShortName,
            @RequestPart("partyImage") MultipartFile partyImage,
            @RequestPart(value = "partyColor", required = false) String partyColor,
            @RequestPart(value = "allianceName", required = false) String allianceName) {

        PartyRequest partyRequest = new PartyRequest();
        partyRequest.setPartyName(partyName);
        partyRequest.setPartyShortName(partyShortName);
        partyRequest.setPartyImage(partyImage);
        partyRequest.setPartyColor(partyColor);
        partyRequest.setAllianceName(allianceName);
        partyRequest.setElectionId(electionId);

        return partyService.createParty(partyRequest);
    }

//    @Operation(summary = "Get a party by ID", description = "Retrieve party details by ID")
//    @GetMapping("/parties/{partyId}/election/{electionId}")
//    public ThedalResponse<Party> getPartyById(@PathVariable("partyId") Long partyId, @PathVariable Long electionId) {
//        ThedalResponse<Party> partyResponse = partyService.getPartyById(partyId, electionId);
//        return partyResponse;
//    }
//    
//    @Operation(summary = "Get all parties by Election ID", description = "Retrieve party details by Election ID")
//    @GetMapping("/parties/election/{electionId}")
//    public ThedalResponse<List<Party>> getPartiesByElectionId(@PathVariable Long electionId) {
//        log.debug("Received request to get parties for electionId: {}", electionId);
//        return partyService.getPartiesByElectionId(electionId);
//    }
    @Operation(summary = "Get a party by ID", description = "Retrieve party details by ID")
    @GetMapping("/parties/{partyId}/election/{electionId}")
    public ThedalResponse<PartyResponseDTO> getPartyById(@PathVariable("partyId") Long partyId, @PathVariable Long electionId) {
        return partyService.getPartyById(partyId, electionId);
    }
    
    @Operation(summary = "Get all parties by Election ID", description = "Retrieve party details by Election ID")
    @GetMapping("/parties/election/{electionId}")
    public ThedalResponse<List<PartyResponseDTO>> getPartiesByElectionId(@PathVariable Long electionId) {
        log.debug("Received request to get parties for electionId: {}", electionId);
        return partyService.getPartiesByElectionId(electionId);
    }

    @Operation(summary = "Update a party", description = "Updates an existing party")
    @PutMapping(value = "/parties/{partyId}/elections/{electionId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ThedalResponse<Party> updateParty(
    		@PathVariable("electionId") Long electionId,
            @PathVariable("partyId") Long partyId,            
            @RequestPart("partyName") String partyName,
            @RequestPart(value = "partyShortName", required = false) String partyShortName,
            @RequestPart(value = "partyImage", required = false) MultipartFile partyImage,
            @RequestPart(value = "partyColor", required = false) String partyColor,
            @RequestPart(value = "allianceName", required = false) String allianceName) {

        PartyRequest partyRequest = new PartyRequest();
        partyRequest.setPartyName(partyName);
        partyRequest.setPartyShortName(partyShortName);
        partyRequest.setPartyImage(partyImage);
        partyRequest.setPartyColor(partyColor);
        partyRequest.setAllianceName(allianceName);

        // Pass electionId to the service method
        return partyService.updateParty(partyId, electionId, partyRequest);
    }

    @Operation(summary = "Delete a party", description = "Deletes a party by ID")
    @DeleteMapping("/parties/elections/{electionId}")
    public ThedalResponse<Void> deleteParty(
    		@PathVariable("electionId") Long electionId,
            @RequestParam(value = "partyIds", required = false) List<Long> partyIds
            ) {
    	
       return partyService.deleteParty(partyIds, electionId);  
        //return new ThedalResponse<>(ThedalSuccess.PARTY_DELETED);
    }
    
    @Operation(summary = "Party reorder api", description = "we can reorder party rows and it will in db")
    @PutMapping("/{electionId}/reorder")
    public ResponseEntity<ThedalResponse<String>> reorderParties(
            @PathVariable Long electionId,
            @RequestBody List<PartyReorderRequest> reorderRequests) {
        try {
            Long accountId = requestDetails.getCurrentAccountId();
            if (accountId == null) {
                log.error("Account ID not found, unauthorized access.");
                throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
            }

            partyService.updatePartyOrder(reorderRequests, accountId, electionId);
            return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.PARTY_ORDER_UPDATED));

        } catch (Exception e) {
            log.error("Error reordering parties: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ThedalResponse<>(ThedalError.PARTY_ORDER_UPDATE_FAILED));
        }
    }

 /////// booth_vulnerability api's
    
    @Operation(summary = "Create a new Booth Vulnerability", description = "Saves a new Booth Vulnerability")
    @PostMapping("/booth-vulnerabilities")
    public ThedalResponse<BoothVulnerability> createBoothVulnerability(@RequestBody @Valid BoothVulnerabilityRequest boothVulnerabilityRequest) {
        ThedalResponse<BoothVulnerability> boothVulnerabilityResponse = boothVulnerabilityService.createBoothVulnerability(boothVulnerabilityRequest);
        return boothVulnerabilityResponse;
    }

    @Operation(summary = "Get Booth Vulnerability by ID", description = "Fetches a Booth Vulnerability by ID for a specific account")
    @GetMapping("/booth-vulnerabilities/{id}")
    public ThedalResponse<BoothVulnerability> getBoothVulnerabilityById(@PathVariable Long id) {
        ThedalResponse<BoothVulnerability> boothVulnerabilityResponse = boothVulnerabilityService.getBoothVulnerabilityById(id);
        return boothVulnerabilityResponse;
    }

    @Operation(summary = "Update Booth Vulnerability", description = "Updates an existing Booth Vulnerability")
    @PutMapping("/booth-vulnerabilities/{id}")
    public ThedalResponse<BoothVulnerability> updateBoothVulnerability(@PathVariable Long id, @RequestBody @Valid BoothVulnerabilityRequest boothVulnerabilityRequest) {
        ThedalResponse<BoothVulnerability> updatedBoothVulnerabilityResponse = boothVulnerabilityService.updateBoothVulnerability(id, boothVulnerabilityRequest);
        return updatedBoothVulnerabilityResponse;
    }

    @Operation(summary = "Delete Booth Vulnerability", description = "Deletes an existing Booth Vulnerability")
    @DeleteMapping("/booth-vulnerabilities/{id}")
    public ThedalResponse<String> deleteBoothVulnerability(@PathVariable Long id) {
        ThedalResponse<String> deleteResponse = boothVulnerabilityService.deleteBoothVulnerability(id);
        return deleteResponse;
    }

    
    
}


