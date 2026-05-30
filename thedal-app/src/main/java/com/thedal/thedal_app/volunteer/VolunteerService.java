package com.thedal.thedal_app.volunteer;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.thedal.thedal_app.response.ThedalResponse;
import com.thedal.thedal_app.volunteer.dto.BoothUpdateRequest;
import com.thedal.thedal_app.volunteer.dto.SaveVolunteerDetailsDTO;
import com.thedal.thedal_app.volunteer.dto.VolunteerActivityResponseDTO;
import com.thedal.thedal_app.volunteer.dto.VolunteerActivityTrackingDTO;
import com.thedal.thedal_app.volunteer.dto.VolunteerDetailsDTO;
import com.thedal.thedal_app.volunteer.dto.VolunteerDetailsUpdate;
import com.thedal.thedal_app.volunteer.dto.VolunteerExportResponse;
import com.thedal.thedal_app.volunteer.dto.VolunteerJobStatusResponse;
import com.thedal.thedal_app.volunteer.dto.VolunteerLocationDto;
import com.thedal.thedal_app.volunteer.dto.VolunteerUploadSummary;

import jakarta.validation.Valid;

@Service
public interface VolunteerService {

	ThedalResponse<Void> saveVolunteer(SaveVolunteerDetailsDTO volunteerDetailsDto,Long electionId);

	
	//ThedalResponse<Page<VolunteerLocationDto>> getAllVolunteersWithLocationData(int page, int size,Long electionId);
	ThedalResponse<Page<VolunteerLocationDto>> getAllVolunteersWithLocationData(int page, int size, Long userId, Long electionId, Long role);
	
	ThedalResponse<Void> activityCheckIn(Long electionId);

	ThedalResponse<Void> activityCheckOut(Long electionId);

	ThedalResponse<Void> activityTrack(VolunteerActivityTrackingDTO volunteerActivityTrackingDTO,Long electionId);

	
	ThedalResponse<Boolean> isCheckedIn(Long userId,Long electionId);

	ThedalResponse<Page<VolunteerActivityResponseDTO>> getVolunteerActivity(Long userId,Long electionId, LocalDate startDate,LocalDate endDate,int page,int size);

//	ThedalResponse<TrackingDataResponse> getVolunteerTrackingData(String volunteerId, LocalDate startDate,
//			LocalDate endDate);

	
	//ThedalResponse<Void> uploadVolunteerFromXlsxOrCsv(MultipartFile file,Long electionId);

	ThedalResponse<Void> addVolunteerToElection(@Valid SaveVolunteerDetailsDTO volunteerDto, Long electionId);

	//ThedalResponse<VolunteerDetailsDTO> getVolunteerByBoothAndMobileNumber(String boothNumber, String mobileNumber);

	ThedalResponse<VolunteerDetailsDTO> getVolunteerByUserId(Long userId, Long electionId);

	ThedalResponse<Void> updateVolunteer(Long userId, Long electionId,
			VolunteerDetailsUpdate volunteerDetailsUpdate);

	ThedalResponse<Void> deleteVolunteer(Long userId, Long electionId);

//	ThedalResponse<List<VolunteerDetailsDTO>> getVolunteerByMobileNumberAndUser(Long electionId, String mobileNumber,
//			Long userId);

	ThedalResponse<Void> updateAssignedBooths(Long electionId, Long userId, BoothUpdateRequest boothUpdateRequest);

	@Deprecated // replaced by paginated variant
	ThedalResponse<List<VolunteerDetailsDTO>> getVolunteerByAssignedBoothsAndMobileNumber(Long electionId,
			List<Long> assignedBooths, String mobileNumber, Long userId);

	// New paginated & sortable variant
	ThedalResponse<Page<VolunteerDetailsDTO>> getVolunteerPageByAssignedBoothsAndMobileNumber(Long electionId,
		List<Long> assignedBooths, String mobileNumber, Long userId, String roleName, int page, int size, String sortBy, String direction);

	// Enhanced variant with search functionality
	ThedalResponse<Page<VolunteerDetailsDTO>> getVolunteerPageByAssignedBoothsAndMobileNumberWithSearch(Long electionId,
		List<Long> assignedBooths, String mobileNumber, Long userId, String roleName, String searchTerm, int page, int size, String sortBy, String direction);

			ThedalResponse<Void>deleteVolunteerFromElection(Long volunteerId, Long electionId);


			ThedalResponse<VolunteerUploadSummary> uploadVolunteerFromXlsxOrCsv(MultipartFile file, Long electionId);


			ThedalResponse<Void> deleteVolunteers(Long electionId, List<Long> userIdList);


		VolunteerExportResponse initiateVolunteerExport(Long accountId, Long electionId, List<Long> assignedBooths,
				String gender, String status, Integer limit);

	// Comprehensive export method with all filters from GET volunteers API
	VolunteerExportResponse initiateVolunteerExportWithFilters(Long accountId, Long electionId,
			String mobileNumber, List<Long> assignedBooths, Long userId, String searchTerm,
			String gender, String status, Integer limit);
			VolunteerJobStatusResponse getVolunteerExportJobStatus(Long jobId, Long electionId, Long accountId);

			// MongoDB filtering for assigned booths and mobile number
			//public List<MongoVolunteer> getMongoVolunteersByAssignedBoothsAndMobileNumber(Long electionId, List<Long> assignedBooths, String mobileNumber, Long userId);

			// Debug method to get unique role names
			ThedalResponse<List<String>> getUniqueRoleNames(Long electionId);

}