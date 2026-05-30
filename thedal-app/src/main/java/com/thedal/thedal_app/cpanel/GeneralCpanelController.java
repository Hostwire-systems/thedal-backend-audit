package com.thedal.thedal_app.cpanel;



import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

import com.thedal.thedal_app.cpanel.dtos.GeneralCpanelRequest;
import com.thedal.thedal_app.cpanel.dtos.UserStatisticsDTO;
import com.thedal.thedal_app.response.ThedalResponse;
import com.thedal.thedal_app.response.ThedalSuccess;
import com.thedal.thedal_app.settings.electionsettings.ReligionEntity;
import com.thedal.thedal_app.settings.electionsettings.ReligionService;
import com.thedal.thedal_app.settings.electionsettings.SectionBulkUploadEntity;
import com.thedal.thedal_app.settings.electionsettings.dto.ReligionRequest;
import com.thedal.thedal_app.settings.electionsettings.dto.ReligionResponseDTO;
import com.thedal.thedal_app.settings.electionsettings.dto.ReligionUpdateRequest;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;
import com.thedal.thedal_app.cpanel.dtos.VoterHistoryEntity;
import com.thedal.thedal_app.cpanel.VoterHistoryRequest;



import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/cpanel")
@Slf4j
public class GeneralCpanelController {


    @Autowired
    private ReligionService religionService;
	
	@Autowired
	private GeneralCpanelService cpanelService;

	@Autowired
    private VoterHistoryService voterHistoryService;
	
	@Autowired
	private com.thedal.thedal_app.user.UserRepo userRepo;
	
	@Operation(summary = "Terms and conditions", description = "Edit terms and conditions.")
    @PutMapping("/terms-and-conditions")
    public ThedalResponse<Void> updateTnC(@Valid @RequestBody GeneralCpanelRequest request) {
        return cpanelService.saveTnC(request);
    }

    @Operation(summary = "Terms and conditions", description = "Retrieves terms and conditions.")
    @GetMapping("/terms-and-conditions")
    public ThedalResponse<String> getTnC() {
        return cpanelService.getTnC();
    }

    //Get privacy policy
    @Operation(summary = "Privacy policy", description = "Retrieves privacy policy.")
    @GetMapping("/privacy-policy")
    public ThedalResponse<String> getPrivacyPolicy() {
        return cpanelService.getPrivacyPolicy();
    }

    //Get faq
    @Operation(summary = "FAQ", description = "Retrieves FAQ.")
    @GetMapping("/faq")
    public ThedalResponse<String> getFaq() {
        return cpanelService.getFaq();
    }
    
    @Operation(summary = "About", description = "Retrieves About content.")
    @GetMapping("/about")
    public ThedalResponse<String> getAbout() {
    return cpanelService.getAbout();
}


    @Operation(summary = "Create common religion data", description = "Adds sample religion data with electionId=0 and accountId=0")
    @PostMapping(value = "/sample", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ThedalResponse<ReligionEntity> createSampleReligion(
            @RequestPart("religionName") String religionName,
            @RequestPart("religionImage") MultipartFile religionImage,
            @RequestPart("religionColor") String religionColor) {

        // Create ReligionRequest object
        ReligionRequest religionRequest = new ReligionRequest();
        religionRequest.setReligionName(religionName);
        religionRequest.setReligionImage(religionImage);
        religionRequest.setReligionColor(religionColor);
        Long accountId = 0L;

        // Call ReligionService with fixed electionId = 0
        return religionService.createReligion(religionRequest, 0L,0L);
    }




    @Operation(summary = "Get all religions", description = "Fetches a list of all religions for a given account and election")
    @GetMapping("/religions")
    public ThedalResponse<List<Map<String, Object>>>  getAllReligions(){
        Long accountId = 0L;
        Long electionId = 0L;

        List<ReligionEntity> religions = religionService.getAllReligions(accountId, electionId);
      if (religions.isEmpty()) {
            log.error("No religions found for account ID: {} and election ID: {}", accountId, electionId);
            throw new ThedalException(ThedalError.RELIGIONS_NOT_FOUND, HttpStatus.NOT_FOUND);
        }

        List<Map<String, Object>> religionDetails = religions.stream()
                .map(religion -> {
                    Map<String, Object> religionData = new HashMap<>();
                    religionData.put("religionId", religion.getId());
                    religionData.put("religionName", religion.getReligionName());
                    religionData.put("religionImage", religion.getReligionImage());
                    religionData.put("religionColor", religion.getReligionColor());
                    religionData.put("orderIndex", religion.getOrderIndex());
                    return religionData;
                }).collect(Collectors.toList());

        return new ThedalResponse<>(ThedalSuccess.RELIGIONS_FETCHED, religionDetails);
    }
  


    @Operation(summary = "Update a religion", description = "Updates an existing religion") 
    @PutMapping(value = "/{religionId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ThedalResponse<ReligionResponseDTO> updateReligions(           
            @PathVariable("religionId") Long religionId,
            @RequestPart("religionName") String religionName,
            @RequestPart(value = "religionImage", required = false) MultipartFile religionImage,
            @RequestPart(value = "religionColor", required = false) String religionColor) {

        // Create a ReligionUpdateRequest object to pass the updated data
        ReligionUpdateRequest religionRequest = new ReligionUpdateRequest();
        religionRequest.setReligionName(religionName);
        religionRequest.setReligionImage(religionImage);
        religionRequest.setReligionColor(religionColor);
        Long electionId = 0L;
        Long accountId = 0L;

        // Call the service method to update religion
        ReligionResponseDTO updatedReligion = religionService.updateReligion(religionId, electionId,accountId,religionRequest);
        return new ThedalResponse<>(ThedalSuccess.RELIGION_UPDATED, updatedReligion);
    }

    // @Operation(summary = "Delete religion", description = "Delete a religion by ID and Election ID") 
    // @DeleteMapping("/religions/{religionId}")
    // public ThedalResponse<Void> deleteReligions(@PathVariable("religionId") Long religionId) {
    //     Long electionId = 0L;
    //     Long accountId = 0L;
    //     if (accountId == null) {
    //         throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
    //     }

    //    // log.info("Received request to delete religion with ID: {} for election ID: {} and account ID: {}", religionId, electionId, accountId);

    //     religionService.deleteReligionByIdAndAccountIdAndElectionId(religionId, accountId, electionId);
        
    //     return new ThedalResponse<>(ThedalSuccess.RELIGION_DELETED, null);
    // }
    @Operation(
    summary = "Delete multiple religions",
    description = "Delete multiple religions by IDs and Election ID"
)
    @DeleteMapping("/religions/bulk-delete")
    public ThedalResponse<Void> deleteMultipleReligions(@RequestParam(value = "religionIds", required = false) List<Long> religionIds) {
        Long electionId = 0L;
        Long accountId = 0L;

        if (accountId == null) {
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        religionService.deleteReligionsByIdsAndAccountIdAndElectionId(religionIds, accountId, electionId);

        return new ThedalResponse<>(ThedalSuccess.RELIGION_DELETED, null);
    }

    @Operation(summary = "Upload bulk Religion Names for cPanel", description = "Upload bulk Religion names for cPanel with electionId=0 and accountId=0 using xlsx or csv files. Images can be updated separately.")
    @PostMapping(value = "/religion-upload", consumes = "multipart/form-data")
    public ResponseEntity<ThedalResponse<SectionBulkUploadEntity>> uploadCpanelReligions(
            @RequestParam("file") MultipartFile file) throws IOException {

        if (file.getSize() > 100 * 1024 * 1024) { // 100 MB limit
            throw new ThedalException(ThedalError.FILE_TOO_LARGE, HttpStatus.BAD_REQUEST);
        }

        ThedalResponse<SectionBulkUploadEntity> response = religionService.uploadCpanelReligions(file);
        return ResponseEntity.ok(response);
    }


@Operation(summary = "Create voter history", description = "Adds voter history data with electionId=0 and accountId=0")
@PostMapping(value = "/voter-history/sample", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public ThedalResponse<VoterHistoryEntity> createSampleVoterHistory(
        @RequestPart("voterHistoryName") String voterHistoryName,
        @RequestPart("voterHistoryImage") MultipartFile voterHistoryImage) {

    // Create VoterHistoryRequest object
    VoterHistoryRequest voterHistoryRequest = new VoterHistoryRequest();
    voterHistoryRequest.setVoterHistoryName(voterHistoryName);
    voterHistoryRequest.setVoterHistoryImage(voterHistoryImage);
    Long accountId = 0L;

    // Call VoterHistoryService with fixed electionId = 0
    return voterHistoryService.createVoterHistory(voterHistoryRequest, 0L, 0L);
}
@Operation(summary = "Get all voter history records", description = "Fetches a list of all voter history records for a given account and election")
@GetMapping("/voter-history")
public ThedalResponse<List<Map<String, Object>>> getAllVoterHistory() {
    Long accountId = 0L;
    Long electionId = 0L;

    List<VoterHistoryEntity> voterHistories = voterHistoryService.getAllVoterHistories(accountId, electionId);

    if (voterHistories.isEmpty()) {
        log.error("No voter history records found for account ID: {} and election ID: {}", accountId, electionId);
        throw new ThedalException(ThedalError.VOTER_HISTORY_NOT_FOUND, HttpStatus.NOT_FOUND);
    }

    List<Map<String, Object>> voterHistoryDetails = voterHistories.stream()
            .map(voterHistory -> {
                Map<String, Object> voterHistoryData = new HashMap<>();
                voterHistoryData.put("voterHistoryId", voterHistory.getId());
                voterHistoryData.put("voterHistoryName", voterHistory.getVoterHistoryName());
                voterHistoryData.put("voterHistoryImage", voterHistory.getVoterHistoryImage());
                return voterHistoryData;
            }).collect(Collectors.toList());

    return new ThedalResponse<>(ThedalSuccess.VOTER_HISTORY_FETCHED, voterHistoryDetails);
}
@Operation(summary = "Update a voter history", description = "Updates an existing voter history record")
@PutMapping(value = "/voter-history/{voterHistoryId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public ThedalResponse<VoterHistoryEntity> updateVoterHistory(
        @PathVariable("voterHistoryId") Long voterHistoryId,
        @RequestPart("voterHistoryName") String voterHistoryName,
        @RequestPart(value = "voterHistoryImage", required = false) MultipartFile voterHistoryImage) {

    // Create a VoterHistoryRequest object to pass the updated data
    VoterHistoryRequest voterHistoryRequest = new VoterHistoryRequest();
    voterHistoryRequest.setVoterHistoryName(voterHistoryName);
    voterHistoryRequest.setVoterHistoryImage(voterHistoryImage);
    
    Long accountId = 0L;
    Long electionId = 0L;

    // Call the service method to update voter history
    VoterHistoryEntity updatedVoterHistory = voterHistoryService.updateVoterHistory(voterHistoryId, electionId, accountId, voterHistoryRequest);
    
    return new ThedalResponse<>(ThedalSuccess.VOTER_HISTORY_UPDATED, updatedVoterHistory);
}
@Operation(summary = "Delete multiple voter history records", description = "Deletes voter history records by IDs")
@DeleteMapping("/voter-history/bulk-delete")
public ThedalResponse<Void> deleteMultipleVoterHistory(
        @RequestParam(value = "voterHistoryIds", required = false) List<Long> voterHistoryIds) {

    Long accountId = 0L;
    Long electionId = 0L;

    if (accountId == null) {
        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
    }

    voterHistoryService.deleteVoterHistories(voterHistoryIds, accountId, electionId);

    return new ThedalResponse<>(ThedalSuccess.VOTER_HISTORY_DELETED, null);
}

@Operation(summary = "Get user statistics", description = "Retrieves total, active, and inactive user counts for dashboard")
@GetMapping("/dashboard/stats")
public ThedalResponse<UserStatisticsDTO> getUserStatistics() {
    log.info("Fetching user statistics for dashboard");
    
    // Count total users
    long totalUsers = userRepo.count();
    
    // Count active users
    long totalActiveUsers = userRepo.countByIsActive(true);
    
    // Count inactive users
    long totalInactiveUsers = userRepo.countByIsActive(false);
    
    // Count super admin users
    long totalSuperAdmins = userRepo.countByRoleRoleName("SUPER_ADMIN");
    
    UserStatisticsDTO stats = new UserStatisticsDTO(totalUsers, totalActiveUsers, totalInactiveUsers, totalSuperAdmins);
    
    log.info("User statistics - Total: {}, Active: {}, Inactive: {}, SuperAdmins: {}", 
            totalUsers, totalActiveUsers, totalInactiveUsers, totalSuperAdmins);
    
    return new ThedalResponse<>(ThedalSuccess.USER_STATISTICS_FETCHED, stats);
}

  
}

    


    

