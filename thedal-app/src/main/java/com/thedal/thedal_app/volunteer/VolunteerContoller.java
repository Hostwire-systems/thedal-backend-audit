package com.thedal.thedal_app.volunteer;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
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

import com.thedal.thedal_app.election.ElectionEntity;
import com.thedal.thedal_app.election.ElectionRepository;
import com.thedal.thedal_app.general.RequestDetailsService;
import com.thedal.thedal_app.response.ThedalResponse;
import com.thedal.thedal_app.response.ThedalSuccess;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;
import com.thedal.thedal_app.volunteer.dto.BoothUpdateRequest;
import com.thedal.thedal_app.volunteer.dto.MigrateVolunteerAdminResponseDTO;
import com.thedal.thedal_app.volunteer.dto.SaveVolunteerDetailsDTO;
import com.thedal.thedal_app.volunteer.dto.VolunteerActivityResponseDTO;
import com.thedal.thedal_app.volunteer.dto.VolunteerActivityTrackingDTO;
import com.thedal.thedal_app.volunteer.dto.VolunteerDetailsDTO;
import com.thedal.thedal_app.volunteer.dto.VolunteerDetailsUpdate;
import com.thedal.thedal_app.volunteer.dto.VolunteerExportResponse;
import com.thedal.thedal_app.volunteer.dto.VolunteerJobStatusResponse;
import com.thedal.thedal_app.volunteer.dto.VolunteerLocationDto;
import com.thedal.thedal_app.volunteer.dto.VolunteerUploadSummary;
import com.thedal.thedal_app.session.VolunteerSessionEnhancementService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/volunteers")
public class VolunteerContoller {
	
	@Autowired
    private VolunteerService volunteerSer;
	@Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private ElectionRepository electionRepository;
    @Autowired
    private RequestDetailsService requestDetails;
    @Autowired
    private VolunteerSessionEnhancementService sessionEnhancementService;
	

	@PostMapping("/{electionId}")
	public ThedalResponse<Void> createVolunteer(@Valid @RequestBody SaveVolunteerDetailsDTO volunteerDto , @PathVariable Long electionId) {
	    return volunteerSer.saveVolunteer(volunteerDto,electionId);
	}
	
	@PostMapping("/add/{electionId}")
	public ThedalResponse<Void> addVolunteerToElection(@Valid @RequestBody SaveVolunteerDetailsDTO volunteerDto , @PathVariable Long electionId) {
	    return volunteerSer.addVolunteerToElection(volunteerDto,electionId);
	}

	//@GetMapping("/{volunteerId}")
    @GetMapping("/election/{electionId}/user/{userId}")
	//@GetMapping("/{volunteerId}/{userId}/{electionId}")
    public ThedalResponse<VolunteerDetailsDTO> getVolunteerDetails(
    		@PathVariable("electionId") Long electionId,
    		@PathVariable("userId") Long userId,
    		@RequestParam(defaultValue = "false") boolean includeDeviceCount
    		           
    		) {
        ThedalResponse<VolunteerDetailsDTO> response = volunteerSer.getVolunteerByUserId(userId, electionId);
        
        // Enhance with device count if requested and response is successful
        if (includeDeviceCount && "success".equals(response.getStatus()) && response.getData() != null) {
            sessionEnhancementService.enrichVolunteerWithDeviceCount(response.getData(), includeDeviceCount);
        }
        
        return response;
    }	
//	//@GetMapping("/by-booth-and-mobile")
//    public ThedalResponse<VolunteerDetailsDTO> getVolunteerByBoothAndMobileNumber(@RequestParam String mobileNumber,@RequestParam String boothNumber) {
//        return volunteerSer.getVolunteerByBoothAndMobileNumber(boothNumber,mobileNumber);
//    }
////////--------------
    @GetMapping("/election/{electionId}/by-booth-and-mobile-and-user")
    public ThedalResponse<org.springframework.data.domain.Page<VolunteerDetailsDTO>> getVolunteerByAssignedBoothsAndMobileNumber(
        @PathVariable Long electionId,
        @RequestParam(required = false) String mobileNumber,
        @RequestParam(required = false) String assignedBooths,
        @RequestParam(required = false) Long userId,
    @RequestParam(required = false) String roleName,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size,
        @RequestParam(defaultValue = "firstName") String sortBy,
        @RequestParam(defaultValue = "asc") String direction,
        @RequestParam(defaultValue = "false") boolean includeDeviceCount) {

        // Convert comma separated booths
        List<Long> assignedBoothList = null;
        if (assignedBooths != null && !assignedBooths.isEmpty()) {
            assignedBoothList = Arrays.stream(assignedBooths.split(","))
                    .map(String::trim)
                    .map(Long::valueOf)
                    .collect(Collectors.toList());
        }
        // If page/size not provided keep backwards behavior by using large size
        int effectivePage = page == null ? 0 : page;
        int effectiveSize = size == null ? 10000 : size; // large upper bound for legacy full list
        
        ThedalResponse<org.springframework.data.domain.Page<VolunteerDetailsDTO>> response = 
            volunteerSer.getVolunteerPageByAssignedBoothsAndMobileNumber(electionId, assignedBoothList, mobileNumber, userId, roleName, effectivePage, effectiveSize, sortBy, direction);
        
        // Enhance with device count if requested and response is successful
        if (includeDeviceCount && "success".equals(response.getStatus()) && response.getData() != null) {
            response.getData().getContent().forEach(volunteer -> 
                sessionEnhancementService.enrichVolunteerWithDeviceCount(volunteer, includeDeviceCount));
        }
        
        return response;
    }

    @GetMapping("/election/{electionId}/by-booth-and-mobile-and-user/search")
    public ThedalResponse<org.springframework.data.domain.Page<VolunteerDetailsDTO>> getVolunteerByAssignedBoothsAndMobileNumberWithSearch(
        @PathVariable Long electionId,
        @RequestParam(required = false) String mobileNumber,
        @RequestParam(required = false) String assignedBooths,
        @RequestParam(required = false) Long userId,
    @RequestParam(required = false) String roleName,
        @RequestParam(required = false) String searchTerm,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size,
        @RequestParam(defaultValue = "firstName") String sortBy,
        @RequestParam(defaultValue = "asc") String direction,
        @RequestParam(defaultValue = "false") boolean includeDeviceCount) {

        // Clean up search term - remove quotes and trim whitespace
        String cleanedSearchTerm = null;
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            cleanedSearchTerm = searchTerm.trim().replaceAll("^\"|\"$", "").trim();
            // If search term becomes empty after cleaning, set to null
            if (cleanedSearchTerm.isEmpty()) {
                cleanedSearchTerm = null;
            }
        }

        // Convert comma separated booths
        List<Long> assignedBoothList = null;
        if (assignedBooths != null && !assignedBooths.isEmpty()) {
            assignedBoothList = Arrays.stream(assignedBooths.split(","))
                    .map(String::trim)
                    .map(Long::valueOf)
                    .collect(Collectors.toList());
        }
        // If page/size not provided keep backwards behavior by using large size
        int effectivePage = page == null ? 0 : page;
        int effectiveSize = size == null ? 10000 : size; // large upper bound for legacy full list
        
        ThedalResponse<org.springframework.data.domain.Page<VolunteerDetailsDTO>> response = 
            volunteerSer.getVolunteerPageByAssignedBoothsAndMobileNumberWithSearch(electionId, assignedBoothList, mobileNumber, userId, roleName, cleanedSearchTerm, effectivePage, effectiveSize, sortBy, direction);
        
        // Enhance with device count if requested and response is successful
        if (includeDeviceCount && "success".equals(response.getStatus()) && response.getData() != null) {
            response.getData().getContent().forEach(volunteer -> 
                sessionEnhancementService.enrichVolunteerWithDeviceCount(volunteer, includeDeviceCount));
        }
        
        return response;
    }



	
	@SuppressWarnings("unchecked")
	//@PutMapping("/{volunteerId}")
//	@PutMapping("/{volunteerId}/{userId}/{electionId}")
	@PutMapping("/election/{electionId}/user/{userId}")
    public ThedalResponse<Void> updateVolunteerDetails(
    		@PathVariable("electionId") Long electionId,
    		@PathVariable("userId") Long userId,        
            @RequestBody VolunteerDetailsUpdate volunteerDetailsUpdate) {
        return volunteerSer.updateVolunteer(userId, electionId, volunteerDetailsUpdate);
    }
	
	//@DeleteMapping("/{volunteerId}")
//	@DeleteMapping("/{volunteerId}/{userId}/{electionId}")
	@DeleteMapping("/election/{electionId}/user/{userId}")
    public ThedalResponse<Void> deleteVolunteer(
    		@PathVariable("electionId") Long electionId,
    		@PathVariable("userId") Long userId
    		) {
        return volunteerSer.deleteVolunteer(userId, electionId);
    }
	
	// Temporary debug endpoint to check what role names exist in database
	@GetMapping("/debug/election/{electionId}/role-names")
    public ThedalResponse<List<String>> getUniqueRoleNames(@PathVariable("electionId") Long electionId) {
        return volunteerSer.getUniqueRoleNames(electionId);
    }
	
//	@PostMapping("/{volunteerId}/activities")
//    public ThedalResponse<Void> addActivities(@PathVariable String volunteerId, @RequestBody ActivityDto activityDto) {
//        log.info("Adding activity for volunteer ID: {}", volunteerId);
//        return volunteerSer.addActivities(volunteerId, activityDto);
//    }
	
//	@GetMapping("/{volunteerId}/tracking-activities")
//	public ThedalResponse<List<ActivityDto>> trackVolunteerActivities(
//	        @PathVariable Long volunteerId,
//	        @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
//	        @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
//
//	    log.info("Tracking activities for volunteer ID: {} between {} and {}", volunteerId, startDate, endDate);
//	    return volunteerSer.getVolunteersActivities(volunteerId, startDate, endDate);
//	}


//	@GetMapping("/volunteer-activity-locations/{volunteerId}")
//	public ThedalResponse<TrackingDataResponse> trackVolunteerActivitiesLocation(
//	        @PathVariable String volunteerId,
//	        @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
//	        @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
//
//	    return volunteerSer.getVolunteerTrackingData(volunteerId, startDate, endDate);
//	}

		
//	@PutMapping("/{volunteerId}/location-route-hours")
//    public ThedalResponse<Void> updateVolunteerLocation(
//            @PathVariable("volunteerId") String volunteerId, 
//            @RequestBody UpdateVolunteerDto updateVolunteerDto) {
//		System.out.println("Received request to update location for volunteer: " + volunteerId); 
//		return volunteerSer.updateVolunteerLocation(volunteerId, updateVolunteerDto);
//    }
	
	//@GetMapping("/all/{electionId}")
	@GetMapping("/all/election/{electionId}/user/{userId}")
    public ThedalResponse<Page<VolunteerLocationDto>> getAllVolunteersWithLocationData(
    		@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @PathVariable Long electionId,
            @PathVariable Long userId,
            @RequestParam(required = false) Long role
             ) {
       
        return (ThedalResponse<Page<VolunteerLocationDto>>) volunteerSer.getAllVolunteersWithLocationData(page, size, userId, electionId, role);
    }
	
	@PostMapping("/{electionId}/check-in")
	public ThedalResponse<Void> activityCheckIn(@PathVariable("electionId") Long electionId){
		return volunteerSer.activityCheckIn(electionId);
	}
	
	@PostMapping("/{electionId}/check-out")
	public ThedalResponse<Void> activityCheckOut(@PathVariable("electionId") Long electionId){
		return volunteerSer.activityCheckOut(electionId);
	}
	
	@PostMapping("/{electionId}/activity/track")
	public ThedalResponse<Void> activityTrack(@RequestBody VolunteerActivityTrackingDTO volunteerActivityTrackingDTO,@PathVariable("electionId") Long electionId){
		return volunteerSer.activityTrack(volunteerActivityTrackingDTO,electionId);
	}
	
	@Operation(summary = "CheckIn status of a Volunteer", description = "If userId is 0, it will use the loggedIn user or else it will use the give userId and"
			+ "returns whether the volunteer is loggedIn or not in reponse as boolean.ElectionId needs to be correct one or else it will give exception")        
	@GetMapping("/{userId}/{electionId}/check-in/status")
	public ThedalResponse<Boolean> isCheckedIn(@PathVariable("userId") Long userId,@PathVariable("electionId") Long electionId){
		return volunteerSer.isCheckedIn(userId,electionId);
	}
	
	@Operation(summary = "Track Volunteer Location", description = "If userId is 0, it will use the loggedIn user or else it will use the give userId . It "
			+ "returns the paginated response of volunteer location data within the given date range.ElectionId needs to be correct one or else it will give exception")        
	@GetMapping("/activity/{userId}/{electionId}")
	public ThedalResponse<Page<VolunteerActivityResponseDTO>> trackVolunteerActivitiesLocation(
			@PathVariable("userId") Long userId,@PathVariable("electionId") Long electionId,
	        @RequestParam(value = "startDate", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
	        @RequestParam(value = "endDate", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
	    	@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

	    return volunteerSer.getVolunteerActivity(userId,electionId,startDate, endDate,page,size);
	}
	
//	@Operation(summary = "Upload bulk Volunteer Data", description = "Upload bulk Volunteer data using xlsx or csv files.")
//	@PostMapping(value = "/election/{electionId}/upload", consumes = "multipart/form-data")
//	public ResponseEntity<ThedalResponse<Void>> uploadVotersFromXlsxOrCsv(
//			@RequestParam("file") MultipartFile file,@RequestParam Long electionId)throws IOException {
//	    ThedalResponse<Void> response = volunteerSer.uploadVolunteerFromXlsxOrCsv(file,electionId);
//	    return ResponseEntity.ok(response);
//	}
	@Operation(summary = "Upload bulk Volunteer Data", description = "Upload bulk Volunteer data using xlsx or csv files.")
	@PostMapping(value = "/election/{electionId}/upload", consumes = "multipart/form-data")
	public ResponseEntity<ThedalResponse<VolunteerUploadSummary>> uploadVotersFromXlsxOrCsv(
	        @RequestParam("file") MultipartFile file, @RequestParam Long electionId) throws IOException {
	    ThedalResponse<VolunteerUploadSummary> response = volunteerSer.uploadVolunteerFromXlsxOrCsv(file, electionId);
	    return ResponseEntity.ok(response);
	}
	
//	@Operation(summary = "upload Cadre image", description = "upload Cadre image.Supported formats are JPEG,JPG,GIF,PNG.Maximum image size:2MB")
//	@PostMapping(value = "/image", consumes = { "multipart/form-data" })
//	public ThedalResponse<String> uploadCadreImage(@RequestParam("image") MultipartFile multipartFile) {
//		return volunteerSer.uploadCadreImage(multipartFile);
//	}
	
//	@PatchMapping("/volunteer/{volunteerId}/booths")
//	public ThedalResponse<Void> updateAssignedBooths(
//	        @PathVariable Long volunteerId,
//	        @RequestBody BoothUpdateRequest boothUpdateRequest) {
//	    return volunteerSer.updateAssignedBooths(volunteerId, boothUpdateRequest);
//	}
	@PutMapping("/volunteer/{electionId}/{userId}/booths")
	public ThedalResponse<Void> updateAssignedBooths(
	        @PathVariable Long electionId,
	        @PathVariable Long userId,
	        @RequestBody BoothUpdateRequest boothUpdateRequest) {
	    return volunteerSer.updateAssignedBooths(electionId, userId, boothUpdateRequest);
	}


	@DeleteMapping("/{volunteerId}/election/{electionId}")
    public ResponseEntity<ThedalResponse<Void>> deleteVolunteerFromElection(
    @PathVariable Long volunteerId,
    @PathVariable Long electionId
) {
    ThedalResponse<Void> response = volunteerSer.deleteVolunteerFromElection(volunteerId, electionId);
    return ResponseEntity.ok(response);
}
	
	
	// New combined API to delete all or multiple volunteers
    @DeleteMapping("/election/{electionId}")
    public ThedalResponse<Void> deleteVolunteers(
            @PathVariable("electionId") Long electionId,
            @RequestParam(value = "userIds", required = false) List<Long> userIds) {
        // Convert null userIds to empty list
        List<Long> userIdList = (userIds != null && !userIds.isEmpty()) 
                ? userIds 
                : Collections.emptyList();
        return volunteerSer.deleteVolunteers(electionId, userIdList);
    }
	
    @Operation(summary = "Update cadres admin_user_id", description = "admin_user_id uses for cadre login")
    @PostMapping("/migrate-admin-user-id")
    @Transactional
    public ThedalResponse<MigrateVolunteerAdminResponseDTO> migrateVolunteerAdminUserId() {
        // SQL query to update admin_user_id for all volunteers
        String sql = "UPDATE volunteers " +
                     "SET admin_user_id = (" +
                     "    SELECT u.user_id " +
                     "    FROM _user u " +
                     "    WHERE u.account_entity_id = volunteers.account_id " +
                     "      AND u.role_id = 1 " +
                     "    LIMIT 1" +
                     ") " +
                     "WHERE admin_user_id IS NULL";

        try {
            // Execute the update query
            int rowsAffected = jdbcTemplate.update(sql);
            log.info("Updated admin_user_id for {} volunteer records across the entire volunteers table", rowsAffected);

            String message;
            if (rowsAffected == 0) {
                message = "No records updated in the volunteers table. Possible reasons: No NULL admin_user_id or no matching admin users found.";
                log.warn("No volunteer records updated. Possible reasons: No NULL admin_user_id or no admin users found.");
            } else {
                message = "Successfully updated admin_user_id for " + rowsAffected + " volunteer records in the volunteers table";
            }

            MigrateVolunteerAdminResponseDTO responseDTO = new MigrateVolunteerAdminResponseDTO(message, rowsAffected);
            return new ThedalResponse<>(ThedalSuccess.VOLUNTEER_ADMIN_UPDATED, responseDTO);
        } catch (Exception e) {
            log.error("Failed to update admin_user_id for volunteers table: {}", e.getMessage());
            throw new ThedalException(ThedalError.DATABASE_UPDATE_FAILED, HttpStatus.INTERNAL_SERVER_ERROR, 
                    "Failed to update admin_user_id: " + e.getMessage());
        }
    }
    
    @Operation(summary = "Export Volunteer Data to Excel with Comprehensive Filters", 
            description = "Export volunteer data to Excel with comprehensive filters (same as GET volunteers API) and store in AWS S3 or local."
           )
  @PostMapping("/{electionId}/export")
  public ResponseEntity<ThedalResponse<VolunteerExportResponse>> exportVolunteersToExcel(
          @PathVariable("electionId") Long electionId,
          @RequestParam(value = "mobileNumber", required = false) String mobileNumber,
          @RequestParam(value = "assignedBooths", required = false) String assignedBooths,
          @RequestParam(value = "userId", required = false) Long userId,
          @RequestParam(value = "searchTerm", required = false) String searchTerm,
          @RequestParam(value = "gender", required = false) String gender,
          @RequestParam(value = "status", required = false) String status,
          @RequestParam(value = "limit", required = false) Integer limit) {

      Long accountId = requestDetails.getCurrentAccountId();
      validateElectionOwnership(electionId, accountId);
      
      // Convert comma separated booths to list
      List<Long> assignedBoothList = null;
      if (assignedBooths != null && !assignedBooths.trim().isEmpty()) {
          try {
              assignedBoothList = Arrays.stream(assignedBooths.split(","))
                      .map(String::trim)
                      .filter(s -> !s.isEmpty())
                      .map(Long::parseLong)
                      .collect(Collectors.toList());
          } catch (NumberFormatException e) {
              throw new ThedalException(ThedalError.INVALID_REQUEST, HttpStatus.BAD_REQUEST);
          }
      }
      
      // Validate request parameters
      if (gender != null && !gender.matches("(?i)^(male|female|other)$")) {
          throw new ThedalException(ThedalError.INVALID_GENDER, HttpStatus.BAD_REQUEST);
      }
      if (status != null && !status.matches("(?i)^(active|inactive)$")) {
          throw new ThedalException(ThedalError.INVALID_STATUS, HttpStatus.BAD_REQUEST);
      }

      VolunteerExportResponse response = volunteerSer.initiateVolunteerExportWithFilters(
          accountId, electionId, mobileNumber, assignedBoothList, userId, searchTerm, 
          gender, status, limit
      );
      return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.VOLUNTEER_EXPORT_INITIATED, response));
  }


    private void validateElectionOwnership(Long electionId, Long accountId) {
         Optional<ElectionEntity> electionOpt = electionRepository.findByIdAndAccountId(electionId, accountId);
       if (!electionOpt.isPresent()) {
           log.error("Election ID {} does not belong to Account ID {}", electionId, accountId);
           throw new ThedalException(ThedalError.INVALID_ELECTION, HttpStatus.FORBIDDEN);  
        }
    }
    
    @Operation(summary = "Check Volunteer Export Job Status",
            description = "Retrieve the status of a volunteer export job by its job ID."
            )
  @GetMapping("/{electionId}/export/status/{jobId}")
  public ResponseEntity<ThedalResponse<VolunteerJobStatusResponse>> getVolunteerExportJobStatus(
          @PathVariable("electionId") Long electionId,
          @PathVariable("jobId") Long jobId) {

      Long accountId = requestDetails.getCurrentAccountId();
      validateElectionOwnership(electionId, accountId);

      VolunteerJobStatusResponse response = volunteerSer.getVolunteerExportJobStatus(jobId, electionId, accountId);
      return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.VOLUNTEER_JOB_STATUS_RETRIEVED, response));
  }
	
}