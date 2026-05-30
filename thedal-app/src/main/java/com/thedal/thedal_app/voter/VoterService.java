package com.thedal.thedal_app.voter;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.thedal.thedal_app.response.ServiceResponse;
import com.thedal.thedal_app.response.ThedalResponse;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.util.Response;
import com.thedal.thedal_app.voter.dto.BulkUploadDto;
import com.thedal.thedal_app.voter.dto.BulkPhotoUploadResponse;
import com.thedal.thedal_app.voter.dto.BulkUploadErrorResponseDTO;
import com.thedal.thedal_app.voter.dto.BulkUploadResponse;
import com.thedal.thedal_app.voter.dto.BulkUploadStatusDto;
import com.thedal.thedal_app.voter.dto.BulkVoterUpdateResponse;
import com.thedal.thedal_app.voter.dto.FamilyResponseDTO;
import com.thedal.thedal_app.voter.dto.FamilySummaryResponseDTO;
import com.thedal.thedal_app.voter.dto.FamilyMembersResponseDTO;
import com.thedal.thedal_app.voter.dto.FamilySequenceReorderRequest;
import com.thedal.thedal_app.voter.dto.FriendGroupResponseDTO;
import com.thedal.thedal_app.voter.dto.PartVoterStatsDTO;
import com.thedal.thedal_app.voter.dto.VoterDTO;
import com.thedal.thedal_app.voter.dto.VoterExportJobsResponse;
import com.thedal.thedal_app.voter.dto.VoterExportResponse;
import com.thedal.thedal_app.voter.dto.VoterExportStatusResponse;
import com.thedal.thedal_app.voter.dto.VoterMongoDTO;
import com.thedal.thedal_app.voter.dto.VoterOtpRequestDto;
import com.thedal.thedal_app.voter.dto.VoterOtpVerifyDto;
import com.thedal.thedal_app.voter.dto.VoterResponseDTO;
import com.thedal.thedal_app.voter.dto.VoterUpdateDTO;
import com.thedal.thedal_app.voter.dto.VoterVoteRequest;
import com.thedal.thedal_app.voter.dto.VoterVotingRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@Service
public interface VoterService {

	ThedalResponse<Void> deleteById(String epicNumber, Long electionId);
	//ThedalResponse<Void> deleteById(@NotBlank(message = "EPIC number is mandatory") String epicNumber, Long electionId);
	//ThedalResponse<Void> deleteById(String epicNumber, Long electionId);
	
	String getVoterDetails(String epicNumber);

	ThedalResponse<BulkUploadResponse> uploadVotersFromXlsxOrCsv(MultipartFile file, Long electionId);

	BulkUploadStatusDto getBulkUploadStatus(Long bulkUploadId, Long electionId);

	// ThedalResponse<Page<VoterLocationDTO>> getAllVoterLocations(Long electionId, Long boothNumber, int page, int size);
	ServiceResponse<String> getAllVoterLocations(Long electionId);

	//VoterEntity updateVoter(String voterId,Long electionId, @Valid VoterUpdateDTO voterUpdateDTO);

	Page<BulkUploadDto> getBulkUploads(Long electionId, String status, Integer page, Integer size, String sortBy);

	ThedalResponse<BulkVoterUpdateResponse> markMultipleVotersAsVoted(Long electionId,
			List<VoterVoteRequest> voterVoteRequests);

	ThedalResponse<VoterDTO> saveVoter(VoterDTO voterDto);

//	VoterUpdateDTO updateVoter(String voterId, Long electionId, @Valid VoterUpdateDTO voterUpdateDTO);
	VoterUpdateDTO updateVoter(String epicNumber, Long electionId, @Valid VoterUpdateDTO voterUpdateDTO);
	
	//ResponseEntity<ThedalResponse<String>> updateVoterImage(String epicNumber, Long electionId, MultipartFile file);
	ResponseEntity<ThedalResponse<String>> updateVoterImage(String epicNumber, Long electionId, MultipartFile file);
	
//	Page<VoterEntity> getVoters(Long accountId, String voterId, String epicNumber, Long electionId,
//			List<Integer> boothNumberList, UUID familyId, String voterFnameEn, Pageable pageable);
	
	ThedalResponse<String> mapFamily(Long electionId, String epicNumber, String otherEpicNumber, UUID familyId);

	ThedalResponse<String> deleteFamilyId(Long electionId, String epicNumber);

	ThedalResponse<Map<String, Object>> updateVoterVotingStatus(Long electionId, String epicNumber,
			VoterVotingRequest request);

	//ThedalResponse<Void> deleteAllByElectionId(Long electionId);

	//VoterExportResponse initiateVoterExport(Long accountId, Long electionId, Integer limit);
//	VoterExportResponse initiateVoterExport(Long accountId, Long electionId, Integer limit, 
//		    Integer partNo, String gender, Integer ageMin, Integer ageMax);
//	
//	void processVoterExport(Long jobId, Long accountId, Long electionId, Integer limit,
//	        Integer partNo, String gender, Integer ageMin, Integer ageMax);
//	

	ThedalResponse<Object> deleteVoters(Long electionId, List<String> epicNumberList);

	List<BulkUploadErrorResponseDTO> getBulkUploadErrors(Long bulkUploadId, Long electionId);

	//ThedalResponse<GenderStatsDTO> getGenderStatistics(Long electionId);

//	Page<VoterEntity> getVoters(Long accountId, String voterId, String epicNumber, Long electionId,
//			List<Integer> boothNumberList, UUID familyId, String voterFnameEn, String partyName, String religionName,
//			Integer age, String gender, Pageable pageable);
//	VoterResponseDTO getVoters(Long accountId, String voterId, String epicNumber, Long electionId, 
//		    List<Integer> boothNumbers, UUID familyId, String voterFnameEn, String partyName, String religionName, 
//		    Integer age, Integer minAge, Integer maxAge, String gender, Pageable pageable);

	VoterExportResponse initiateVoterExport(Long accountId, Long electionId, List<Integer> partNos, String gender,
			Integer minAge, Integer maxAge, Integer limit);

	// Comprehensive export method with all filters from GET voters API
	VoterExportResponse initiateVoterExportWithFilters(Long accountId, Long userId, Long electionId,
			String voterId, String epicNumber, List<Integer> boothNumberList, UUID familyId, UUID friendId,
			String voterName, String voterFirstName, String voterLastName, String voterFnameEn, String voterLnameEn,
			String voterFnameL1, String voterFnameL2, String voterLnameL1, String voterLnameL2,
			String relationName, String relationFirstName, String relationLastName, String relationFirstNameEn, String relationLastNameEn,
			String partyName, String religionName, String voterHistoryName, Integer age, Integer minAge, Integer maxAge, Boolean includeUnknownAge,
			String gender, String hasDob, Boolean starNumber, String description, String categoryName, String casteCategoryName,
			String casteName, String subCaste, String duplicate, Long serialNo, Boolean overseas, Boolean fatherless, Boolean guardian,
			Integer birthdayMonth, Integer birthdayDay, Boolean hasMobileNo, String mobileNo, Boolean singleVoterFamily, List<String> columns, Integer limit);

//	VoterResponseDTO getVoters(Long accountId, String voterId, String epicNumber, Long electionId,
//			List<Integer> boothNumberList, UUID familyId, String voterFnameEn, String partyName, String religionName,
//			Integer age, Integer minAge, Integer maxAge, String gender, Boolean dobFilter, Pageable pageable);


	ResponseEntity<ThedalResponse<String>> uploadVoterVideo(String epicNumber, Long electionId, MultipartFile file);

	ResponseEntity<Response<String>> sendVoterOtp(Long electionId, String mobileNo, @Valid VoterOtpRequestDto request);

	ResponseEntity<Response<VoterDTO>> verifyVoterOtp(Long electionId, String mobileNo,
			@Valid VoterOtpVerifyDto request);

	/**
	 * Analyzes a specific bulk upload for debugging issues
	 * Provides comprehensive analysis of failed bulk uploads including constraint violations,
	 * transaction issues, and database state verification
	 * 
	 * @param bulkUploadId ID of the bulk upload to analyze
	 * @param electionId ID of the election
	 */
	void analyzeBulkUpload(Long bulkUploadId, Long electionId);

	VoterExportStatusResponse getExportJobStatus(Long accountId, Long electionId, Long jobId);
	
	VoterExportStatusResponse getExportJobStatusByJobId(Long accountId, Long jobId);
	//VoterExportStatusResponse getExportJobStatus(Long jobId);
	
	ResponseEntity<Resource> downloadExportFile(Long jobId, Long accountId, Long electionId);
	
	// Utility method to fix existing jobs with relative URLs
	int fixExistingJobUrls(Long accountId, Long electionId);

	//List<VoterExportStatusResponse> getExportJobsByElection(Long accountId, Long electionId);
//
//	Page<VoterExportStatusResponse> getExportJobsByElectionPaginated(Long accountId, Long electionId,
//			Pageable pageable);

//	VoterResponseDTO getVoters(
//	        Long accountId, String voterId, String epicNumber, Long electionId,
//	        List<Integer> boothNumberList, UUID familyId, UUID friendId, 
//	        List<String> voterFnameEnList, List<String> voterLnameEnList, 
//	        List<String> voterFnameL1List, List<String> voterFnameL2List,
//	        List<String> voterLnameL1List, List<String> voterLnameL2List,
//	        List<String> relationFirstNameEnList, List<String> relationLastNameEnList,
//	        List<String> rlnFnameL1List, List<String> rlnFnameL2List,
//	        List<String> rlnLnameL1List, List<String> rlnLnameL2List,
//	        List<String> partyNameList, List<String> voterHistoryNameList,
//	        String religionName, Integer age, Integer minAge, Integer maxAge, 
//	        Boolean includeUnknownAge, List<String> genderList,
//	        Boolean dobFilter, Integer todayMonth, Integer todayDay,
//	        Integer tomorrowMonth, Integer tomorrowDay, 
//	        Boolean starNumber, String description, String categoryName, String casteCategoryNameTrimmed, 
//	        String casteNameTrimmed, String subCasteNameTrimmed,
//	        Boolean findDuplicates, Long serialNo, Boolean overseas, Boolean fatherless, Boolean guardian, 
//	        Boolean hasMobileNo,  Boolean singleVoterFamily, Pageable pageable);
//	
	
	VoterResponseDTO searchVotersByName(Long accountId, Long electionId, String searchQuery, Boolean isFamily, Pageable pageable);

	Page<VoterExportStatusResponse> getExportJobsByElectionPaginated(
            Long accountId, Long electionId, String status, 
            LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
	
//	List<VoterExportStatusResponse> getExportJobsByElection(Long accountId, Long electionId, String status,
//			LocalDateTime defaultStartDate, LocalDateTime defaultEndDate);
	VoterExportJobsResponse getExportJobsByElection(
            Long accountId, Long electionId, String status, LocalDateTime startDate, LocalDateTime endDate);

	void deleteExportJob(Long accountId, Long electionId, Long jobId);

    void updateFamilyCount(UUID familyId, Long accountId, Long electionId);

//	FamilyResponseDTO getFamilyVoters(Long accountId, Long electionId, UUID familyId, List<Integer> boothNumberList,
//			List<String> voterFnameEnList, List<String> voterLnameEnList, Pageable pageable);

//    void updateFamilyCount(UUID familyId, Long accountId, Long electionId);
	void validateResultNotEmpty(Page<?> result, ThedalError error);

//	FamilyResponseDTO getFamilyVotersByElection(Long accountId, Long electionId, List<Integer> boothNumberList,
//			boolean groupByHouseNumber, Pageable pageable);

	Object getBoothVoterStatuses(Long electionId, Integer boothNumber, String pollStatus, int page, int size, String sortDirection);

	PartVoterStatsDTO getPartVoterStats(Long electionId, Integer partNo);

	VoterMongoDTO getVoterFromMongo(String epicNumber, Long electionId);

	Page<VoterMongoDTO> getAllVotersFromMongo(Long electionId, Pageable pageable);

	ThedalResponse<String> mapFriendId(Long electionId,
			@NotBlank(message = "EPIC number is mandatory") String epicNumber, String friendEpicNumber, UUID friendId);

	ThedalResponse<String> deleteFriendId(Long electionId,
			@NotBlank(message = "EPIC number is mandatory") String epicNumber);

	void mapFamiliesByHouseNumber(Long electionId, Long accountId, Long jobId);
	void mapFamiliesByHouseNumber(Long electionId, Long accountId);

//	ThedalResponse<String> deleteFriend(Long electionId,
//			@NotBlank(message = "EPIC number is mandatory") String epicNumber,
//			@NotBlank(message = "Friend EPIC number is mandatory") String friendEpicNumber);

	ThedalResponse<String> deleteFriends(Long electionId,
			@NotBlank(message = "EPIC number is mandatory") String epicNumber, List<String> friendEpicNumbers);

	FamilyResponseDTO getFamilyVotersByElection(Long accountId, Long electionId, List<Integer> boothNumberList,
			Pageable pageable);

	// NEW FAST FAMILY APIs
	// Fast family summary for initial loading
	FamilySummaryResponseDTO getFamilySummary(Long accountId, Long electionId, List<Integer> boothNumbers, List<Integer> partNumbers, String nameFilter, Boolean crossFamily, Pageable pageable);
	
	// Fast family members for detailed loading with pagination
	FamilyMembersResponseDTO getFamilyMembers(Long accountId, Long electionId, String familyId, String sortBy, String order, Pageable pageable);

	FriendGroupResponseDTO getFriendVotersByElection(Long accountId, Long electionId, UUID friendId, List<Integer> boothNumberList,
			Pageable pageable);

	ResponseEntity<ThedalResponse<String>> removeVoterImage(String epicNumber, Long electionId);

	// Bulk photo upload methods
	ThedalResponse<BulkPhotoUploadResponse> uploadVoterPhotosFromZip(MultipartFile zipFile, Long electionId);
	VoterResponseDTO getVoters(Long accountId, String voterId, String epicNumber, Long electionId,
			List<Integer> boothNumberList, UUID familyId, UUID friendId, List<String> voterFnameEnList,
			List<String> voterLnameEnList, List<String> voterFnameL1List, List<String> voterFnameL2List,
			List<String> voterLnameL1List, List<String> voterLnameL2List, List<String> relationFirstNameEnList,
			List<String> relationLastNameEnList, List<String> rlnFnameL1List, List<String> rlnFnameL2List,
			List<String> rlnLnameL1List, List<String> rlnLnameL2List, List<String> partyNameList,
			List<String> voterHistoryNameList, List<String> religionNameList, Integer age, Integer minAge, Integer maxAge,
			Boolean includeUnknownAge, List<String> genderList, Boolean filterToday, Boolean filterTomorrow,
			Integer todayMonth, Integer todayDay, Integer tomorrowMonth, Integer tomorrowDay, Integer birthdayMonth, Integer birthdayDay, Boolean starNumber,
			List<String> descriptionList, List<String> categoryNameList, List<String> casteCategoryNameList, List<String> casteNameList,
			List<String> subCasteNameList, Boolean findDuplicates, Long serialNo, Boolean overseas, Boolean fatherless,
			Boolean guardian, Boolean hasMobileNo, String mobileNo, Boolean singleVoterFamily, String pollStatus, Boolean isFamily, Pageable pageable);

	// Convenience method for no-family voters API with simplified parameters
	VoterResponseDTO getVoters(Long electionId, String voterId, String epicNumber, String boothNumbers,
			UUID familyId, UUID friendId, String voterName, String voterFirstName, String voterLastName, 
			String voterFnameEn, String voterLnameEn, String voterFnameL1, String voterFnameL2, 
			String voterLnameL1, String voterLnameL2, String relationName, String relationFirstName, 
			String relationLastName, String relationFirstNameEn, String relationLastNameEn,
			String partyName, String religionName, String voterHistoryName, Integer age, Integer minAge, 
			Integer maxAge, Boolean includeUnknownAge, String gender, Boolean filterToday, Boolean filterTomorrow, 
			Boolean starNumber, String description, String categoryName, String casteCategoryName, 
			String casteName, String subCaste, String duplicate, Long serialNo, Boolean overseas, 
			Boolean fatherless, Boolean guardian, Integer todayMonth, Integer todayDay, 
			Integer tomorrowMonth, Integer tomorrowDay, Integer customBirthdayMonth, Integer customBirthdayDay, 
			Boolean hasMobileNo, String mobileNo, Boolean singleVoterFamily, String pollStatus, int page, int size, 
			List<String> mappedSortFields, String orderLower, Boolean isFamily, Boolean noFamilyOnly);

	// Overloaded method with noFamilyOnly parameter for enhanced no-family voters filtering
	VoterResponseDTO getVoters(Long accountId, String voterId, String epicNumber, Long electionId,
			List<Integer> boothNumberList, UUID familyId, UUID friendId, List<String> voterFnameEnList,
			List<String> voterLnameEnList, List<String> voterFnameL1List, List<String> voterFnameL2List,
			List<String> voterLnameL1List, List<String> voterLnameL2List, List<String> relationFirstNameEnList,
			List<String> relationLastNameEnList, List<String> rlnFnameL1List, List<String> rlnFnameL2List,
			List<String> rlnLnameL1List, List<String> rlnLnameL2List, List<String> partyNameList,
			List<String> voterHistoryNameList, List<String> religionNameList, Integer age, Integer minAge, Integer maxAge,
			Boolean includeUnknownAge, List<String> genderList, Boolean filterToday, Boolean filterTomorrow,
			Integer todayMonth, Integer todayDay, Integer tomorrowMonth, Integer tomorrowDay, Integer birthdayMonth, Integer birthdayDay, Boolean starNumber,
			List<String> descriptionList, List<String> categoryNameList, List<String> casteCategoryNameList, List<String> casteNameList,
			List<String> subCasteNameList, Boolean findDuplicates, Long serialNo, Boolean overseas, Boolean fatherless,
			Boolean guardian, Boolean hasMobileNo, String mobileNo, Boolean singleVoterFamily, String pollStatus, Boolean isFamily, Pageable pageable, Boolean noFamilyOnly);

	
	ThedalResponse<String> generateFamilyId(Long electionId, String epicNumber);

	// Family sequence number management methods
	
	ThedalResponse<String> renumberFamilies(Long electionId, String strategy, Integer startNumber);
	
	ThedalResponse<String> updateSingleFamilyNumber(Long electionId, UUID familyId, Integer sequenceNumber);
	
	ThedalResponse<String> reorderFamilySequences(Long electionId, List<FamilySequenceReorderRequest> reorderRequests);

	// Family part override methods
	ThedalResponse<String> setFamilyPartOverride(Long electionId, UUID familyId, Integer partNumber);
	
	ThedalResponse<String> removeFamilyPartOverride(Long electionId, UUID familyId);
	
	// Family head management methods
	ThedalResponse<String> setFamilyHead(Long electionId, UUID familyId, String epicNumber);
	
	ThedalResponse<String> removeFamilyHead(Long electionId, UUID familyId);

	ThedalResponse<BulkPhotoUploadEntity> getBulkPhotoUploadStatus(Long bulkUploadId);
	
	ThedalResponse<List<BulkPhotoUploadEntity>> getAllBulkPhotoUploads(Long electionId, int page, int size);
	
	// Helper method for voter export with eager collection loading
	Page<VoterEntity> fetchVotersWithEagerCollections(
			Long accountId, Long electionId, String voterId, String epicNumber, List<Integer> effectiveBoothNumbers,
			UUID familyId, UUID friendId, List<String> voterFnameEnList, List<String> voterLnameEnList,
			List<String> voterFnameL1List, List<String> voterFnameL2List, List<String> voterLnameL1List,
			List<String> voterLnameL2List, List<String> relationFirstNameEnList, List<String> relationLastNameEnList,
			List<String> partyNameList, List<String> voterHistoryNameList, List<String> religionNameList,
			Integer age, Integer minAge, Integer maxAge, Boolean includeUnknownAge, List<String> genderList,
			Integer birthdayMonth, Integer birthdayDay, Boolean starNumber, List<String> descriptionList,
			List<String> categoryNameList, List<String> casteCategoryNameList, List<String> casteNameList,
			List<String> subCasteList, Long serialNo, Boolean overseas, Boolean fatherless,
			Boolean guardian, Boolean hasMobileNo, String mobileNo, Boolean singleVoterFamily, Pageable pageable);

	// Election statistics methods
	com.thedal.thedal_app.voter.dto.ElectionVoterStatsDTO getElectionVoterStats(Long electionId);
	
	com.thedal.thedal_app.voter.dto.WinningProbabilityDTO getWinningProbability(Long electionId);

	
		
}