package com.thedal.thedal_app.election;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.thedal.thedal_app.election.dtos.BannerActiveStatusRequest;
import com.thedal.thedal_app.election.dtos.ElectionDTO;
import com.thedal.thedal_app.election.dtos.ElectionIdImageDTO;
import com.thedal.thedal_app.election.dtos.ElectionReorderRequest;
import com.thedal.thedal_app.election.dtos.ElectionResponseDTO;
import com.thedal.thedal_app.files.FileReorderRequest;
import com.thedal.thedal_app.files.Files;
import com.thedal.thedal_app.files.WhatsappForwardUpdateRequest;
import com.thedal.thedal_app.general.RequestDetailsService;
import com.thedal.thedal_app.response.ThedalResponse;
import com.thedal.thedal_app.response.ThedalSuccess;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;


@RestController
@RequestMapping("/elections")
@Slf4j
public class ElectionController {
    @Autowired
    private ElectionService electionService;
    @Autowired
    private RequestDetailsService requestDetails;
    @Autowired
    private ElectionRepository electionRepository;

    @Operation(summary = "Create Election", description = "Create an election without image.")
    @PostMapping
    public ResponseEntity<ThedalResponse<Long>> createElection(@Valid @RequestBody ElectionDTO request) {
        System.out.println("Received request body: " + request);
        return electionService.createElection(request);
    }

    @Operation(summary = "Add Election Image and create Election", description = "Create an election by adding an image.")
    @PostMapping(value = "/election-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ThedalResponse<ElectionIdImageDTO>> createElectionWithImage(
            @RequestParam("file") MultipartFile file) {
        return electionService.createElectionWithImage(file);
    }

    @Operation(summary = "Update Election Image by ID", description = "Update an election image by ID.")
    @PutMapping(value = "/{electionId}/election-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ThedalResponse<String>> uploadElectionImage(
            @PathVariable Long electionId,
            @RequestParam("file") MultipartFile file) {
        return electionService.updateElectionImage(electionId,file);
    }

    @Operation(summary = "Update Election by Id", description = "Update an election by providing an Election ID.")
    @PutMapping("/{electionId}")
    public ResponseEntity<ThedalResponse<String>> updateElection(
            @PathVariable Long electionId,
            @RequestBody ElectionDTO electionDTO) {
        return electionService.updateElectionFields(electionId, electionDTO);
    }

    @Operation(summary = "Get All Elections", description = "Get all elections of the current account.")
    @GetMapping
    public ResponseEntity<ThedalResponse<List<ElectionResponseDTO>>> getElections() {
        return electionService.getElections();
    }

    @Operation(summary = "Get Election by Id", description = "Get an election by providing an Election ID.")
    @GetMapping("/{electionId}")
    public ResponseEntity<ThedalResponse<ElectionResponseDTO>> getElectionById(@PathVariable Long electionId) {
        return electionService.getElectionById(electionId);
    }
    
//    @Operation(summary = "Request OTP for Election Deletion", description = "Sends an OTP to the user's mobile number to authorize election deletion.")
//    @PostMapping("/{electionId}/delete-otp")
//    public ResponseEntity<ThedalResponse<Long>> requestElectionDeleteOtp(@PathVariable Long electionId) {
//        return ResponseEntity.ok(electionService.requestElectionDeleteOtp(electionId));
//    }
//    
//    @Operation(summary = "Delete Election with OTP Verification", description = "Deletes an election after verifying the provided OTP.")
//    @DeleteMapping("/{electionId}/otp-deletion")
//    public ResponseEntity<ThedalResponse<Void>> deleteElectionOtp(@PathVariable Long electionId, @RequestParam String otp) {
//        return ResponseEntity.ok(electionService.verifyElectionDeleteOtp(electionId, otp));
//    }

    @Operation(summary = "Delete Election", description = "Delete an election from the system by providing the Election ID.")
	@DeleteMapping("/{electionId}")
	public ThedalResponse<?> deleteElection(@PathVariable Long electionId) {
		return electionService.deleteElectionById(electionId);
	}
    
    @Operation(summary = "Verify OTP and Delete Election", description = "Verify OTP for a user and delete the associated election.")
    @PostMapping("/verify-otp")
    public ResponseEntity<ThedalResponse<Void>> verifyElectionDeleteOtp(
            @RequestParam Long userId,
            @RequestParam String otp) {
        log.info("Received request to verify OTP for userId: {}", userId);
        return ResponseEntity.ok(electionService.verifyElectionDeleteOtp(userId, otp));
    }
    
    @Operation(summary = "Add Banner image",description = "upload Banner image.Here we can upload Banner image corresponding to an election.Supported formats are JPEG,JPG,GIF,PNG")
	@PostMapping(value="/banner-image/{electionId}",consumes = { "multipart/form-data" })
	public ThedalResponse<String> addBannerImageToElection(@PathVariable Long electionId,
			@RequestPart("banner-image") MultipartFile multipartFile){
		return electionService.addBannerImageToElection(electionId,multipartFile);
    }
   
	@Operation(summary = "view banner images",description="get list of banner images for the election id ")
	@GetMapping("/banner-image/{electionId}")
	public ThedalResponse<List<Files>> getAllBannerImagesByElectionId(@PathVariable Long electionId){
		return electionService.getBannerImagesByElectionId(electionId);
	}
	
	@Operation(summary = "Update WhatsApp Footer", description = "Update WhatsApp Footer content for a specific election.")
@PutMapping("/whatsapp-footer/{electionId}")
public ThedalResponse<String> updateWhatsAppFooter(
        @PathVariable Long electionId,
        @RequestBody WhatsAppFooterRequest request) {
    return electionService.saveWhatsAppFooter(electionId, request);
}


    @Operation(summary = "Get WhatsApp Footer", description = "Get WhatsApp Footer content for a specific election.")
    @GetMapping("/whatsapp-footer/{electionId}")
    public ThedalResponse<String> getWhatsAppFooter(@PathVariable Long electionId) {
        return electionService.getWhatsAppFooter(electionId);
    }

	@Operation(summary = "Update WhatsApp Forward flag for banner")
	@PutMapping("/banner-image/whatsapp-forward/{electionId}")
	public ThedalResponse<String> updateWhatsappForwardFlag(
	        @PathVariable Long electionId,
	        @RequestBody WhatsappForwardUpdateRequest request) {
	    return electionService.updateWhatsappForwardFlag(electionId, request.getFileId(), request.isWhatsappForward());
	}


//	@Operation(summary = "delete Banner image",description="delete banner image. Individual banner image can be deleted by using fileId which you get from getAllBannerImagesByElectionId api ")
//	@DeleteMapping("/banner-image/{fileId}")
//	public ThedalResponse<Void> deleteBannerImage(@PathVariable Long fileId){
//		return electionService.deleteBannerImage(fileId);
//	}
	@Operation(
		    summary = "Delete Banner image",
		    description = "Delete banner image using electionId and fileId. Ensures file belongs to the current account and election."
		)
		@DeleteMapping("/banner-image/{electionId}/{fileId}")
		public ThedalResponse<Void> deleteBannerImage(
		        @PathVariable Long electionId,
		        @PathVariable Long fileId) {
		    return electionService.deleteBannerImage(electionId, fileId);
		}
	
	@Operation(
		    summary = "Delete All Banner images",
		    description = "Delete one or multiple banner images using electionId and optional fileIds. If fileIds are not provided, all banner images for the election are deleted."
		)
		@DeleteMapping("/banner-image/{electionId}")
		public ThedalResponse<Void> deleteBannerImages(
		        @PathVariable Long electionId,
		        @RequestParam(required = false) List<Long> fileIds) {
		    return electionService.deleteBannerImages(electionId, fileIds);
		}

	
	@PatchMapping("/election/{electionId}/booth-slip-templates")
	public ResponseEntity<ThedalResponse<Void>> updateBoothSlipTemplates(
	        @PathVariable Long electionId,
	        @RequestBody List<Long> templateIds) throws JsonProcessingException {
	    
	    Long accountId = requestDetails.getCurrentAccountId();
	    if (accountId == null) {
	        log.error("Account ID not found, unauthorized access.");
	        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
	    }

	    // Call service to update templates
	    electionService.updateBoothSlipTemplates(electionId, templateIds, accountId);

	    // Return a standardized response
	    ThedalResponse<Void> response = new ThedalResponse<>();
	    response.setResponse(ThedalSuccess.TEMPLATES_UPDATED_SUCCESSFULLY, null);
	    return ResponseEntity.ok(response);
	}


//	@PutMapping("/elections/reorder")
//	public ResponseEntity<String> reorderElections(@RequestBody List<ElectionReorderDTO> reorderedElections) {
//	    electionService.reorderElections(reorderedElections);
//	    return ResponseEntity.ok("Elections reordered successfully");
//	}

	@Operation(summary = "Update Multiple Elections Order", description = "Updates order for multiple elections.")
	@PutMapping("/reorder")
	public ResponseEntity<ThedalResponse<String>> updateMultipleElectionOrder(@RequestBody List<ElectionReorderRequest> requests) {
	    log.info("Received order updates for multiple elections");

	    Long accountId = requestDetails.getCurrentAccountId();
	    if (accountId == null) {
	        log.error("Account ID not found, unauthorized access.");
	        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
	    }

	    electionService.updateMultipleElectionOrder(requests, accountId);

	    return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.ELECTION_REORDING));
	}

	@Operation(summary = "ReOrder Banner", description = "Reorder banner")
	@PutMapping("/{electionId}/files/reorder")
	public ResponseEntity<ThedalResponse<String>> reorderFiles(
	        @PathVariable Long electionId,
	        @RequestBody List<FileReorderRequest> reorderRequests) {
	    try {
	        Long accountId = requestDetails.getCurrentAccountId();
	        if (accountId == null) {
	            log.error("Account ID not found, unauthorized access.");
	            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
	        }

	        electionService.updateFileOrder(reorderRequests, accountId, electionId);

	        return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.BANNER_ORDER_UPDATED));

	    } catch (Exception e) {
	        log.error("Unexpected error occurred while reordering files: {}", e.getMessage());
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                .body(new ThedalResponse<>(ThedalError.BANNER_ORDER_UPDATE_FAILED));
	    }
	}

	///////////////////////////////////////////
	
	@Operation(summary = "Update Banner Active Status", description = "Update the active/inactive status of a banner for a specific election.")
	@PutMapping("/banner-image/active-status/{electionId}")
	public ThedalResponse<String> updateBannerActiveStatus(
	        @PathVariable Long electionId,
	        @RequestBody BannerActiveStatusRequest request) {
	    return electionService.updateBannerActiveStatus(electionId, request.getFileId(), request.getIsActive());
	}

	@Operation(summary = "Migrate Elections to MongoDB", description = "Migrate all elections from PostgreSQL to MongoDB")
	@PostMapping("/migrate-to-mongodb")
	public ResponseEntity<ThedalResponse<String>> migrateElectionsToMongoDB() {
	    try {
	        Long accountId = requestDetails.getCurrentAccountId();
	        if (accountId == null) {
	            log.error("Account ID not found, unauthorized access.");
	            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
	        }

	        ThedalResponse<String> response = electionService.migrateElectionsToMongoDB(accountId);
	        return ResponseEntity.ok(response);

	    } catch (Exception e) {
	        log.error("Unexpected error occurred while migrating elections: {}", e.getMessage());
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                .body(new ThedalResponse<>(ThedalError.INTERNAL_SERVER_ERROR));
	    }
	}

	@Operation(summary = "Migrate All Elections to MongoDB (Admin)", description = "Migrate all elections from PostgreSQL to MongoDB across all accounts")
	@PostMapping("/admin/migrate-all-to-mongodb")
	public ResponseEntity<ThedalResponse<String>> migrateAllElectionsToMongoDB() {
	    try {
	        // Note: Add admin role check here if needed
	        // requestDetails.checkUserRolePermission(RolePermission.ADMIN_ACCESS);

	        ThedalResponse<String> response = electionService.migrateAllElectionsToMongoDB();
	        return ResponseEntity.ok(response);

	    } catch (Exception e) {
	        log.error("Unexpected error occurred while migrating all elections: {}", e.getMessage());
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                .body(new ThedalResponse<>(ThedalError.INTERNAL_SERVER_ERROR));
	    }
	}
	
	@Operation(summary = "Set Default Party", description = "Set the default party for a specific election")
	@PostMapping("/{electionId}/default-party")
	public ResponseEntity<ThedalResponse<String>> setDefaultParty(
	        @PathVariable Long electionId,
	        @Valid @RequestBody com.thedal.thedal_app.election.dtos.SetDefaultPartyRequest request) {
	    log.info("Setting default party for electionId: {}, partyId: {}", electionId, request.getPartyId());
	    try {
	        ThedalResponse<String> response = electionService.setDefaultParty(electionId, request.getPartyId());
	        return ResponseEntity.ok(response);
	    } catch (ThedalException e) {
	        log.error("Error setting default party: {}", e.getMessage());
	        throw e;
	    } catch (Exception e) {
	        log.error("Unexpected error setting default party: {}", e.getMessage());
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                .body(new ThedalResponse<>(ThedalError.INTERNAL_SERVER_ERROR));
	    }
	}
	
	@Operation(summary = "Get Default Party", description = "Retrieve the default party for a specific election")
	@GetMapping("/{electionId}/default-party")
	public ResponseEntity<ThedalResponse<com.thedal.thedal_app.election.dtos.DefaultPartyResponse>> getDefaultParty(
	        @PathVariable Long electionId) {
	    log.info("Getting default party for electionId: {}", electionId);
	    try {
	        ThedalResponse<com.thedal.thedal_app.election.dtos.DefaultPartyResponse> response = 
	                electionService.getDefaultParty(electionId);
	        return ResponseEntity.ok(response);
	    } catch (ThedalException e) {
	        log.error("Error getting default party: {}", e.getMessage());
	        throw e;
	    } catch (Exception e) {
	        log.error("Unexpected error getting default party: {}", e.getMessage());
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                .body(new ThedalResponse<>(ThedalError.INTERNAL_SERVER_ERROR));
	    }
	}
	
	@Operation(summary = "Request OTP for Election Freeze", description = "Sends an OTP to the admin's mobile number to authorize election freeze operation.")
	@PostMapping("/{electionId}/freeze/request-otp")
	public ResponseEntity<ThedalResponse<String>> requestElectionFreezeOtp(@PathVariable Long electionId) {
	    log.info("Requesting OTP for freezing electionId: {}", electionId);
	    try {
	        ThedalResponse<String> response = electionService.requestElectionFreezeOtp(electionId);
	        return ResponseEntity.ok(response);
	    } catch (ThedalException e) {
	        log.error("Error requesting freeze OTP: {}", e.getMessage());
	        throw e;
	    } catch (Exception e) {
	        log.error("Unexpected error requesting freeze OTP: {}", e.getMessage());
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                .body(new ThedalResponse<>(ThedalError.INTERNAL_SERVER_ERROR));
	    }
	}
	
	@Operation(summary = "Verify OTP and Freeze Election", description = "Verifies the OTP and freezes the election, making it read-only.")
	@PostMapping("/{electionId}/freeze/verify-otp")
	public ResponseEntity<ThedalResponse<String>> verifyFreezeOtpAndFreezeElection(
	        @PathVariable Long electionId,
	        @RequestParam String otp) {
	    log.info("Verifying OTP and freezing electionId: {}", electionId);
	    try {
	        ThedalResponse<String> response = electionService.verifyFreezeOtpAndFreezeElection(electionId, otp);
	        return ResponseEntity.ok(response);
	    } catch (ThedalException e) {
	        log.error("Error verifying freeze OTP: {}", e.getMessage());
	        throw e;
	    } catch (Exception e) {
	        log.error("Unexpected error verifying freeze OTP: {}", e.getMessage());
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                .body(new ThedalResponse<>(ThedalError.INTERNAL_SERVER_ERROR));
	    }
	}
	
	@Operation(summary = "Request OTP for Election Unfreeze", description = "Sends an OTP to the admin's mobile number to authorize election unfreeze operation.")
	@PostMapping("/{electionId}/unfreeze/request-otp")
	public ResponseEntity<ThedalResponse<String>> requestElectionUnfreezeOtp(@PathVariable Long electionId) {
	    log.info("Requesting OTP for unfreezing electionId: {}", electionId);
	    try {
	        ThedalResponse<String> response = electionService.requestElectionUnfreezeOtp(electionId);
	        return ResponseEntity.ok(response);
	    } catch (ThedalException e) {
	        log.error("Error requesting unfreeze OTP: {}", e.getMessage());
	        throw e;
	    } catch (Exception e) {
	        log.error("Unexpected error requesting unfreeze OTP: {}", e.getMessage());
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                .body(new ThedalResponse<>(ThedalError.INTERNAL_SERVER_ERROR));
	    }
	}
	
	@Operation(summary = "Verify OTP and Unfreeze Election", description = "Verifies the OTP and unfreezes the election, restoring write permissions.")
	@PostMapping("/{electionId}/unfreeze/verify-otp")
	public ResponseEntity<ThedalResponse<String>> verifyUnfreezeOtpAndUnfreezeElection(
	        @PathVariable Long electionId,
	        @RequestParam String otp) {
	    log.info("Verifying OTP and unfreezing electionId: {}", electionId);
	    try {
	        ThedalResponse<String> response = electionService.verifyUnfreezeOtpAndUnfreezeElection(electionId, otp);
	        return ResponseEntity.ok(response);
	    } catch (ThedalException e) {
	        log.error("Error verifying unfreeze OTP: {}", e.getMessage());
	        throw e;
	    } catch (Exception e) {
	        log.error("Unexpected error verifying unfreeze OTP: {}", e.getMessage());
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                .body(new ThedalResponse<>(ThedalError.INTERNAL_SERVER_ERROR));
	    }
	}

}

