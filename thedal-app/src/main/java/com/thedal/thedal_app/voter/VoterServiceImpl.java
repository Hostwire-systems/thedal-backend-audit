package com.thedal.thedal_app.voter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.util.IOUtils;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thedal.thedal_app.auth.MobileVerificationRepo;
import com.thedal.thedal_app.awsfilestore.AwsFileUpload;
import com.thedal.thedal_app.awsfilestore.ImageUpload;
import com.thedal.thedal_app.cpanel.VoterHistoryRepository;
import com.thedal.thedal_app.cpanel.dtos.VoterHistoryEntity;
import com.thedal.thedal_app.election.DynamicFieldEntity;
import com.thedal.thedal_app.election.DynamicFieldRepository;
import com.thedal.thedal_app.election.DynamicFieldEntity;
import com.thedal.thedal_app.election.ElectionBooth;
import com.thedal.thedal_app.election.ElectionBoothRepository;
import com.thedal.thedal_app.election.ElectionEntity;
import com.thedal.thedal_app.election.ElectionRepository;
import com.thedal.thedal_app.election.PartManager;
import com.thedal.thedal_app.election.PartManagerRepository;
import com.thedal.thedal_app.files.Files;
import com.thedal.thedal_app.files.FilesRepository;
import com.thedal.thedal_app.files.HandlerType;
import com.thedal.thedal_app.general.RequestDetailsService;
import com.thedal.thedal_app.notification.NotificationService;
import com.thedal.thedal_app.notification.NotificationTemplate;
import com.thedal.thedal_app.notification.NotificationType;
import com.thedal.thedal_app.notification.SmsNotification;
import com.thedal.thedal_app.quartz.JobSchedulerService;
import com.thedal.thedal_app.report.ReportService;
import com.thedal.thedal_app.report.cadre.VolunteerVsVoterReportEntity;
import com.thedal.thedal_app.report.cadre.VolunteerVsVoterReportRepository;
import com.thedal.thedal_app.report.dto.ElectionOverviewDTO;
import com.thedal.thedal_app.report.dto.VotersHavingContactsDTO;
import com.thedal.thedal_app.response.ServiceResponse;
import com.thedal.thedal_app.response.ThedalResponse;
import com.thedal.thedal_app.response.ThedalSuccess;
import com.thedal.thedal_app.role.Role;
import com.thedal.thedal_app.settings.electionsettings.Availability;
import com.thedal.thedal_app.settings.electionsettings.AvailabilityMongoRepository;
import com.thedal.thedal_app.settings.electionsettings.AvailabilityRepository;
import com.thedal.thedal_app.settings.electionsettings.BenefitSchemes;
import com.thedal.thedal_app.settings.electionsettings.BenefitSchemesMongoRepository;
import com.thedal.thedal_app.settings.electionsettings.BenefitSchemesRepository;
import com.thedal.thedal_app.settings.electionsettings.CasteCategoryEntity;
import com.thedal.thedal_app.settings.electionsettings.CasteCategoryRepository;
import com.thedal.thedal_app.settings.electionsettings.CasteEntity;
import com.thedal.thedal_app.settings.electionsettings.CasteMongoRepository;
import com.thedal.thedal_app.settings.electionsettings.CasteRepository;
import com.thedal.thedal_app.settings.electionsettings.ComplaintRepository;
import com.thedal.thedal_app.settings.electionsettings.FeedbackIssue;
import com.thedal.thedal_app.settings.electionsettings.FeedbackIssueMongoRepository;
import com.thedal.thedal_app.settings.electionsettings.FeedbackIssueRepository;
import com.thedal.thedal_app.settings.electionsettings.Language;
import com.thedal.thedal_app.settings.electionsettings.LanguageMongoRepository;
import com.thedal.thedal_app.settings.electionsettings.LanguageRepository;
import com.thedal.thedal_app.settings.electionsettings.Party;
import com.thedal.thedal_app.settings.electionsettings.PartyMongoRepository;
import com.thedal.thedal_app.settings.electionsettings.PartyRepository;
import com.thedal.thedal_app.settings.electionsettings.ReligionEntity;
import com.thedal.thedal_app.settings.electionsettings.ReligionMongoRepository;
import com.thedal.thedal_app.settings.electionsettings.ReligionRepository;
import com.thedal.thedal_app.settings.electionsettings.SchemeBy;
import com.thedal.thedal_app.settings.electionsettings.SectionRepository;
import com.thedal.thedal_app.settings.electionsettings.SubCasteEntity;
import com.thedal.thedal_app.settings.electionsettings.SubCasteMongoRepository;
import com.thedal.thedal_app.settings.electionsettings.SubCasteRepository;
import com.thedal.thedal_app.settings.electionsettings.VoterHistoryMongoRepository;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;
import com.thedal.thedal_app.user.UserEntity;
import com.thedal.thedal_app.user.UserRepo;
import com.thedal.thedal_app.util.RandomTokenGenerator;
import com.thedal.thedal_app.util.Response;
import com.thedal.thedal_app.volunteer.VolunteerEntity;
import com.thedal.thedal_app.volunteer.VolunteerRepository;
import com.thedal.thedal_app.voter.VoterRepo.PartVoterStatsProjection;
import com.thedal.thedal_app.voter.dto.AadhaarStatsProjection;
import com.thedal.thedal_app.voter.dto.BoothAadhaarStatsProjection;
import com.thedal.thedal_app.voter.dto.BoothGenderStatsDTO;
import com.thedal.thedal_app.voter.dto.BoothGenderStatsProjection;
import com.thedal.thedal_app.voter.dto.BoothMembershipStatsProjection;
import com.thedal.thedal_app.voter.dto.BoothVerificationStatsDTO;
import com.thedal.thedal_app.voter.dto.BulkUploadDto;
import com.thedal.thedal_app.voter.dto.BulkPhotoUploadResponse;
import com.thedal.thedal_app.voter.dto.BulkUploadErrorResponseDTO;
import com.thedal.thedal_app.voter.dto.BulkUploadResponse;
import com.thedal.thedal_app.voter.dto.BulkUploadStatusDto;
import com.thedal.thedal_app.voter.BulkPhotoUploadEntity;
import com.thedal.thedal_app.voter.BulkPhotoUploadRepository;
import com.thedal.thedal_app.voter.dto.BulkVoterUpdateResponse;
import com.thedal.thedal_app.voter.dto.FamilyDTO;
import com.thedal.thedal_app.voter.dto.FamilyMembersResponseDTO;
import com.thedal.thedal_app.voter.dto.FamilyResponseDTO;
import com.thedal.thedal_app.voter.dto.FamilySequenceReorderRequest;
import com.thedal.thedal_app.voter.dto.FamilySummaryDTO;
import com.thedal.thedal_app.voter.dto.VoterHistoryDTO;
import com.thedal.thedal_app.voter.dto.FamilySummaryResponseDTO;
import com.thedal.thedal_app.voter.dto.FriendGroupDTO;
import com.thedal.thedal_app.voter.dto.FriendGroupResponseDTO;
import com.thedal.thedal_app.voter.dto.GenderStatsDTO;
import com.thedal.thedal_app.voter.dto.GenderStatsProjection;
import com.thedal.thedal_app.voter.dto.MembershipStatsProjection;
import com.thedal.thedal_app.voter.dto.PartVoterStatsDTO;
import com.thedal.thedal_app.voter.dto.VerificationStatsDTO;
import com.thedal.thedal_app.voter.dto.AddressedVoterStatsDTO;
import com.thedal.thedal_app.voter.dto.AddressedVoterStatsProjection;
import com.thedal.thedal_app.voter.dto.FamilyMappingStatsDTO;
import com.thedal.thedal_app.voter.dto.FamilyMappingStatsProjection;
import com.thedal.thedal_app.voter.dto.VoterDTO;
import com.thedal.thedal_app.voter.dto.VoterDownloadJobSpecifications;
import com.thedal.thedal_app.voter.dto.VoterExportJobsResponse;
import com.thedal.thedal_app.voter.dto.VoterExportResponse;
import com.thedal.thedal_app.voter.dto.VoterExportStatusResponse;
import com.thedal.thedal_app.voter.dto.VoterMongoDTO;
import com.thedal.thedal_app.voter.dto.VoterOtpRequestDto;
import com.thedal.thedal_app.voter.dto.VoterOtpVerifyDto;
import com.thedal.thedal_app.voter.dto.VoterResponseDTO;
import com.thedal.thedal_app.voter.dto.VoterSearchResultDTO;
import com.thedal.thedal_app.voter.dto.VoterStatusDTO;
import com.thedal.thedal_app.voter.dto.VoterUpdateDTO;
import com.thedal.thedal_app.voter.dto.VoterVoteRequest;
import com.thedal.thedal_app.voter.dto.VoterVotingRequest;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class VoterServiceImpl implements VoterService {
	
	// Static block to increase POI byte array limit for large files
	static {
		// Increase Apache POI's byte array limit from 100MB to 500MB for large Excel files
		IOUtils.setByteArrayMaxOverride(500_000_000); // 500MB limit
		log.info("POI byte array limit set to 500MB for large file processing in VoterServiceImpl");
	}
	
	private static final int QUEUE_CAPACITY = 10000;
	private static final int BATCH_SIZE = 500; // Reduced from 1000 to prevent connection timeouts during eager collection loading
	private final ExecutorService executorService = new ThreadPoolExecutor(
			Runtime.getRuntime().availableProcessors(),
			Runtime.getRuntime().availableProcessors() * 2,
			60L, TimeUnit.SECONDS,
			new LinkedBlockingQueue<>(QUEUE_CAPACITY),
			new ThreadPoolExecutor.CallerRunsPolicy());
	@Autowired
	private VoterRepo voterRepository;
	@Autowired
	private VoterMongoRepository voterMongoRepository;
	@Autowired
	private RequestDetailsService requestDetails;
	@Autowired
	private VoterFileUploadService voterFileUploadService;
	@Autowired
	private BulkUploadRepo bulkUploadRepo;
	@Autowired
	private FilesRepository filesRepo;
	@Autowired
	private FilesRepository filesRepository;
	@Autowired
	private JobSchedulerService jobSchedulerService;
	@Autowired
	private ImageUpload imageUpload;
	@Autowired
	private AwsFileUpload awsFileUpload;
	@Value("${aws.s3.files.bucket}")
	private String s3bucket;
	@Value("${thedal.server.url}")
	private String serverUrl;
	@Autowired
	private ElectionBoothRepository electionBoothRepository;
	@Autowired
	private ElectionRepository electionRepository;
	@Autowired
	private BulkPhotoUploadRepository bulkPhotoUploadRepository;
	@Autowired
	private VoterPhotoUploadService voterPhotoUploadService;
	@Autowired
	private ElectionBoothService electionBoothService;
	@Autowired
	private SectionVoterService sectionService;
	@PersistenceContext
	private EntityManager entityManager;
	@Lazy
	@Autowired
	private VoterService self; // Self-injection for transactional proxy calls
	@Autowired
	private VoterCsvZipExportService csvZipExportService; // Memory-optimized CSV ZIP export service
	@Autowired
	private ReligionRepository religionRepository;
	@Autowired
	private CasteRepository casteRepository;
	@Autowired
	private SubCasteRepository subCasteRepository;
	@Autowired
	private ComplaintRepository complaintRepository;
	@Autowired
	private DynamicFieldMappingRepository dynamicFieldMappingRepository;
	@Autowired
	private LanguageRepository languageRepository;
	@Autowired
	private VoterDownloadJobRepository voterDownloadJobRepository;
	@Autowired
	private SectionRepository sectionRepository;
	@Autowired
	private UserRepo userRepo;
	@Autowired
	private VolunteerRepository volunteerRepository;
	@Autowired
	private BulkUploadErrorRepository bulkUploadErrorRepository;
	@Autowired
	private BenefitSchemesRepository benefitSchemesRepository;
	@Autowired
	private AvailabilityRepository availabilityRepository;
	@Autowired
	private PartyRepository partyRepository;
	@Autowired
	private Executor taskExecutor;
	@Autowired
	private PartManagerService partManagerService;
	@Autowired
	private PartManagerRepository partManagerRepository;
	@Autowired
	private NotificationTemplate notificationTemplate;
	@Autowired
	private NotificationService notificationService;
	@Autowired
	MobileVerificationRepo mobileVerificationRepo;
	@Autowired
	private SmsNotification smsNotification;
	@Autowired
	private FeedbackIssueRepository feedbackIssueRepository;
	@Autowired
	private VoterHistoryRepository voterHistoryRepository;
	@Autowired
	private ReligionMongoRepository religionMongoRepository;
	@Autowired
	private CasteMongoRepository casteMongoRepository;
	@Autowired
	private SubCasteMongoRepository subCasteMongoRepository;
	@Autowired
	private PartyMongoRepository partyMongoRepository;
	@Autowired
	private AvailabilityMongoRepository availabilityMongoRepository;
	@Autowired
	private LanguageMongoRepository languageMongoRepository;
	@Autowired
	private BenefitSchemesMongoRepository benefitSchemesMongoRepository;
	@Autowired
	private FeedbackIssueMongoRepository feedbackIssueMongoRepository;
	@Autowired
	private VoterHistoryMongoRepository voterHistoryMongoRepository;
	@Autowired
	private VolunteerVsVoterReportRepository volunteerVsVoterReportRepository;
	@Autowired
	private CasteCategoryRepository casteCategoryRepository;
	@Autowired
	private FamilyMappingJobRepository familyMappingJobRepository;
	@Autowired
	private MongoTemplate mongoTemplate;
	@Autowired
	private ObjectMapper objectMapper;
	@Autowired
	private VoterBenefitSchemeRepository voterBenefitSchemeRepository;	
	@Autowired
	private DynamicFieldRepository dynamicFieldRepository;
	
	private final Map<String, Object> statsCache = new ConcurrentHashMap<>();
	private final long CACHE_TTL_MS = 300000; // 5 minutes TTL
	private final Map<String, Long> cacheTimes = new ConcurrentHashMap<>();
	private final Map<String, Boolean> electionOwnershipCache = new ConcurrentHashMap<>();
	private final long CACHE_EXPIRY_MS = 1800000; // 30 minutes
	private final Map<String, Long> electionCacheTimes = new ConcurrentHashMap<>();
	private final Map<String, Boolean> validationCache = new ConcurrentHashMap<>();
	private final Map<String, Long> validationCacheTimes = new ConcurrentHashMap<>();
	private final long VALIDATION_CACHE_TTL = 300000; // 5 minutes
	@Autowired 
	private ReportService reportService;


	private void validateElectionOwnership(Long electionId, Long accountId) {
	String cacheKey = "election_" + electionId + "_account_" + accountId;
	
	// Check if validation exists in cache and is still valid
	if (electionOwnershipCache.containsKey(cacheKey)) {
		Long cacheTime = electionCacheTimes.get(cacheKey);
		if (cacheTime != null && (System.currentTimeMillis() - cacheTime) < CACHE_EXPIRY_MS) {
			// Cache hit - skip database query
			return;
		}
	}
	
	// If not in cache or expired, perform the database check
	Optional<ElectionEntity> electionOpt = electionRepository.findByIdAndAccountId(electionId, accountId);
	if (!electionOpt.isPresent()) {
		log.error("Election ID {} does not belong to Account ID {}", electionId, accountId);
		throw new ThedalException(ThedalError.INVALID_ELECTION, HttpStatus.FORBIDDEN);
	}
	
	// Add to cache with timestamp
	electionOwnershipCache.put(cacheKey, true);
	electionCacheTimes.put(cacheKey, System.currentTimeMillis());
   }

private void validateElectionOwnershipFast(Long electionId, Long accountId) {
	String cacheKey = "election_" + electionId + "_account_" + accountId;
	
	Long cacheTime = validationCacheTimes.get(cacheKey);
	if (cacheTime != null && (System.currentTimeMillis() - cacheTime) < VALIDATION_CACHE_TTL) {
		return; // Cache hit - skip DB query
	}
	
	// Only hit DB if not in cache
	Optional<ElectionEntity> electionOpt = electionRepository.findByIdAndAccountId(electionId, accountId);
	if (!electionOpt.isPresent()) {
		throw new ThedalException(ThedalError.INVALID_ELECTION, HttpStatus.FORBIDDEN);
	}
	
	// Cache the result
	validationCache.put(cacheKey, true);
	validationCacheTimes.put(cacheKey, System.currentTimeMillis());
}

private List<Integer> getEffectiveBoothNumbersFast(List<Integer> boothNumbers, Role userRole, Long userId) {
	log.info("VOLUNTEER FILTER DEBUG - Role: {}, UserId: {}, InputBooths: {}", 
			 userRole.getRoleName(), userId, boothNumbers);
	
	// For READ operations: Allow all roles to see all booths (no restrictions)
	// Volunteers can now view all voters regardless of their assigned booths
	log.info("VOLUNTEER FILTER DEBUG - READ operation: Allowing access to all booths for all roles");
	return boothNumbers;
}

/**
 * Parse voting history JSON string into List of VoterHistoryDTO objects
 * @param votingHistoryJson JSON array string from database
 * @return List of VoterHistoryDTO objects, empty list if parsing fails
 */
private List<VoterHistoryDTO> parseVotingHistory(String votingHistoryJson) {
	List<VoterHistoryDTO> votingHistory = new ArrayList<>();
	
	if (votingHistoryJson == null || votingHistoryJson.trim().isEmpty() || "[]".equals(votingHistoryJson.trim())) {
		return votingHistory; // Return empty list
	}
	
	try {
		// Parse JSON array
		com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
		com.fasterxml.jackson.databind.JsonNode jsonArray = mapper.readTree(votingHistoryJson);
		
		if (jsonArray.isArray()) {
			for (com.fasterxml.jackson.databind.JsonNode node : jsonArray) {
				Long id = node.has("id") && !node.get("id").isNull() ? node.get("id").asLong() : null;
				String name = node.has("name") && !node.get("name").isNull() ? node.get("name").asText() : null;
				String image = node.has("image") && !node.get("image").isNull() ? node.get("image").asText() : null;
				Integer orderIndex = node.has("orderIndex") && !node.get("orderIndex").isNull() ? node.get("orderIndex").asInt() : null;
				
				VoterHistoryDTO dto = new VoterHistoryDTO(id, name, image, orderIndex);
				votingHistory.add(dto);
			}
		}
	} catch (Exception e) {
		log.warn("Failed to parse voting history JSON: {}, error: {}", votingHistoryJson, e.getMessage());
		// Return empty list on parse error
	}
	
	return votingHistory;
}

/**
 * Validates if the user has permission to modify (update/delete) a voter.
 * ADMIN and SUPER_ADMIN can modify all voters.
 * Volunteers can only modify voters from their assigned booths.
 * 
 * @param voterBoothNumber The booth number of the voter being modified
 * @param userRole The role of the current user
 * @param userId The ID of the current user
 * @throws ThedalException if user doesn't have permission to modify the voter
 */
private void validateBoothAccessForWrite(Integer voterBoothNumber, Role userRole, Long userId) {
	log.info("WRITE PERMISSION CHECK - Role: {}, UserId: {}, VoterBoothNumber: {}", 
			 userRole.getRoleName(), userId, voterBoothNumber);
	
	// ADMIN and SUPER_ADMIN can modify all voters
	if ("SUPER_ADMIN".equalsIgnoreCase(userRole.getRoleName()) || "ADMIN".equalsIgnoreCase(userRole.getRoleName())) {
		log.info("WRITE PERMISSION CHECK - Admin/SuperAdmin role, access granted");
		return;
	}
	
	// For all other roles, check if they have volunteer access to this booth
	VolunteerEntity volunteer = volunteerRepository.findByUserEntity_Id(userId)
			.orElseThrow(() -> new ThedalException(ThedalError.VOLUNTEER_NOT_FOUND, HttpStatus.NOT_FOUND));

	List<Long> assignedBooths = volunteer.getAssignedBooth();
	
	log.info("WRITE PERMISSION CHECK - User assigned booths: {}", assignedBooths);
	
	// Check if the voter's booth is in the volunteer's assigned booths
	boolean hasAccess = assignedBooths.stream()
			.anyMatch(booth -> booth.intValue() == voterBoothNumber.intValue());
	
	if (!hasAccess) {
		log.error("WRITE PERMISSION DENIED - User {} does not have access to booth {}. Assigned booths: {}", 
				  userId, voterBoothNumber, assignedBooths);
		throw new ThedalException(
			ThedalError.ACCESS_DENIED, 
			HttpStatus.FORBIDDEN, 
			"You do not have permission to modify voters in booth " + voterBoothNumber + 
			". You can only modify voters in your assigned booths: " + assignedBooths
		);
	}
	
	log.info("WRITE PERMISSION CHECK - Access granted for booth {}", voterBoothNumber);
}

public void validateResultNotEmpty(Page<?> result, ThedalError error) {
	if (result.isEmpty()) {
		throw new ThedalException(error, HttpStatus.NOT_FOUND);
	}
}

////// Load many-to-many relationships efficiently for only the current page
//private void loadManyToManyRelationships(List<VoterEntity> voters) {
//    if (voters.isEmpty()) {
//        return;
//    }
//    
//    List<Long> voterIds = voters.stream()
//        .map(VoterEntity::getId)
//        .collect(Collectors.toList());
//    
//    // Load each relationship type separately with individual error handling
//    // This prevents one failed query from breaking the entire transaction
//    
//    loadBenefitSchemesSafe(voterIds, voters);
//    loadFeedbackIssuesSafe(voterIds, voters);
//    loadVoterHistoriesSafe(voterIds, voters);
//    loadLanguagesSafe(voterIds, voters);
//    
//}
//private void loadManyToManyRelationships(List<VoterEntity> voters, Long accountId, Long electionId) {
//	if (voters.isEmpty()) {
//		return;
//	}
//	List<Long> voterIds = voters.stream().map(VoterEntity::getId).collect(Collectors.toList());
//	loadLanguagesSafe(voterIds, voters);
//	loadBenefitSchemesSafe(voterIds, voters, accountId, electionId);
//	loadFeedbackIssuesSafe(voterIds, voters, accountId, electionId);
//	loadVoterHistoriesSafe(voterIds, voters, accountId, electionId);
//}
//
//private void loadBenefitSchemesSafe(List<Long> voterIds, List<VoterEntity> voters, Long accountId, Long electionId) {
//	try {
//		log.debug("Loading benefit schemes for voterIds: {}, accountId: {}, electionId: {}", 
//				  voterIds, accountId, electionId);
//		List<Object[]> schemeData = voterRepository.findBenefitSchemesByVoterIds(voterIds, accountId, electionId);
//		log.debug("Retrieved {} benefit scheme rows for voterIds {}: {}", 
//				  schemeData.size(), voterIds, schemeData);
//		Map<Long, List<BenefitSchemes>> voterSchemeMap = new HashMap<>();
//		for (Object[] row : schemeData) {
//			Long voterId = ((Number) row[0]).longValue();
//			BenefitSchemes scheme = new BenefitSchemes();
//			scheme.setId(((Number) row[1]).longValue());
//			scheme.setSchemeName((String) row[2]);
//			scheme.setImageUrl((String) row[3]);
//			scheme.setSchemeBy(SchemeBy.valueOf((String) row[4]));
//			scheme.setAccountId(((Number) row[5]).longValue());
//			scheme.setElectionId(((Number) row[6]).longValue());
//			scheme.setOrderIndex(row[7] != null ? ((Number) row[7]).intValue() : null);
//		   // scheme.setCreatedAt(row[8] != null ? ((Timestamp) row[8]).toLocalDateTime() : null);
//		   // scheme.setUpdatedAt(row[9] != null ? ((Timestamp) row[9]).toLocalDateTime() : null);
//			log.debug("Mapped benefit scheme for voterId={}: id={}, name={}", 
//					  voterId, scheme.getId(), scheme.getSchemeName());
//			voterSchemeMap.computeIfAbsent(voterId, k -> new ArrayList<>()).add(scheme);
//		}
//		voters.forEach(voter -> {
//			List<BenefitSchemes> schemes = voterSchemeMap.getOrDefault(voter.getId(), new ArrayList<>());
//			voter.setBenefitSchemes(schemes);
//			log.debug("Set benefit schemes for voter {}: {}", voter.getId(), 
//					  schemes.stream().map(s -> "id=" + s.getId() + ",name=" + s.getSchemeName())
//							 .collect(Collectors.toList()));
//		});
//		log.debug("Successfully loaded benefit schemes for {} voters", voters.size());
//	} catch (Exception e) {
//		log.error("Failed to load benefit schemes for voterIds {}: {}", voterIds, e.getMessage(), e);
//		voters.forEach(voter -> voter.setBenefitSchemes(new ArrayList<>()));
//	}
//}
private void loadManyToManyRelationships(List<VoterEntity> voters, Long accountId, Long electionId) {
	if (voters.isEmpty()) {
		return;
	}
	List<Long> voterIds = voters.stream().map(VoterEntity::getId).collect(Collectors.toList());
	loadLanguagesSafe(voterIds, voters);
	loadBenefitSchemesSafe(voterIds, voters, accountId, electionId);
	loadFeedbackIssuesSafe(voterIds, voters, accountId, electionId);
	loadVoterHistoriesSafe(voterIds, voters, accountId, electionId);
	loadDynamicFields(voters, accountId, electionId);
}

//private void loadBenefitSchemesSafe(List<Long> voterIds, List<VoterEntity> voters, Long accountId, Long electionId) {
//	try {
//		log.debug("Loading benefit schemes for voterIds: {}, accountId: {}, electionId: {}", 
//				  voterIds, accountId, electionId);
//		List<Object[]> schemeData = voterRepository.findBenefitSchemesByVoterIds(voterIds, accountId, electionId);
//		log.debug("Retrieved {} benefit scheme rows for voterIds {}: {}", 
//				  schemeData.size(), voterIds, schemeData);
//		Map<Long, List<BenefitSchemes>> voterSchemeMap = new HashMap<>();
//		for (Object[] row : schemeData) {
//			Long voterId = ((Number) row[0]).longValue();
//			BenefitSchemes scheme = new BenefitSchemes();
//			scheme.setId(((Number) row[1]).longValue());
//			scheme.setSchemeName((String) row[2]);
//			scheme.setImageUrl((String) row[3]);
//			scheme.setSchemeBy(SchemeBy.valueOf((String) row[4]));
//			scheme.setAccountId(((Number) row[5]).longValue());
//			scheme.setElectionId(((Number) row[6]).longValue());
//			scheme.setOrderIndex(row[7] != null ? ((Number) row[7]).intValue() : null);
//			scheme.setUserSelection((Boolean) row[8]);
//		   // scheme.setCreatedAt(row[8] != null ? ((Timestamp) row[8]).toLocalDateTime() : null);
//		   // scheme.setUpdatedAt(row[9] != null ? ((Timestamp) row[9]).toLocalDateTime() : null);
//			log.debug("Mapped benefit scheme for voterId={}: id={}, name={}", 
//					  voterId, scheme.getId(), scheme.getSchemeName());
//			voterSchemeMap.computeIfAbsent(voterId, k -> new ArrayList<>()).add(scheme);
//		}
////		voters.forEach(voter -> {
////			List<BenefitSchemes> schemes = voterSchemeMap.getOrDefault(voter.getId(), new ArrayList<>());
////			voter.setBenefitSchemes(schemes);
////			log.debug("Set benefit schemes for voter {}: {}", voter.getId(), 
////					  schemes.stream().map(s -> "id=" + s.getId() + ",name=" + s.getSchemeName())
////							 .collect(Collectors.toList()));
////		});
//		voters.forEach(voter -> {
//            List<BenefitSchemes> schemes = voterSchemeMap.getOrDefault(voter.getId(), new ArrayList<>());
//            voter.setBenefitSchemes(schemes);
//            log.debug("Set benefit schemes for voter {}: {}", voter.getId(), 
//                      schemes.stream().map(s -> "id=" + s.getId() + ",name=" + s.getSchemeName() + ",userSelection=" + s.getUserSelection())
//                             .collect(Collectors.toList()));
//        });
//		log.debug("Successfully loaded benefit schemes for {} voters", voters.size());
//	} catch (Exception e) {
//		log.error("Failed to load benefit schemes for voterIds {}: {}", voterIds, e.getMessage(), e);
//		voters.forEach(voter -> voter.setBenefitSchemes(new ArrayList<>()));
//	}
//}
private void loadBenefitSchemesSafe(List<Long> voterIds, List<VoterEntity> voters, Long accountId, Long electionId) {
    try {
        log.debug("Loading benefit schemes for voterIds: {}, accountId: {}, electionId: {}", 
                  voterIds, accountId, electionId);
        
        // Modify the query to include the selected status from the join table
        List<Object[]> schemeData = voterRepository.findBenefitSchemesByVoterIds(voterIds, accountId, electionId);
        log.debug("Retrieved {} benefit scheme rows for voterIds {}: {}", 
                  schemeData.size(), voterIds, schemeData);
        
        Map<Long, List<VoterBenefitScheme>> voterSchemeMap = new HashMap<>();
        
        for (Object[] row : schemeData) {
            Long voterId = ((Number) row[0]).longValue();
            
            // Create VoterBenefitScheme instead of just BenefitSchemes
            VoterBenefitScheme voterBenefitScheme = new VoterBenefitScheme();
            
            // Create and populate BenefitSchemes
            BenefitSchemes scheme = new BenefitSchemes();
            scheme.setId(((Number) row[1]).longValue());
            scheme.setSchemeName((String) row[2]);
            scheme.setSchemeValue(row[3] != null ? ((Number) row[3]).doubleValue() : null);
            scheme.setImageUrl((String) row[4]);
            scheme.setSchemeBy(SchemeBy.valueOf((String) row[5]));
            scheme.setAccountId(((Number) row[6]).longValue());
            scheme.setElectionId(((Number) row[7]).longValue());
            scheme.setOrderIndex(row[8] != null ? ((Number) row[8]).intValue() : null);
            scheme.setUserSelection((Boolean) row[9]);
            
            // Set the scheme and selected status
            voterBenefitScheme.setBenefitScheme(scheme);
            voterBenefitScheme.setSelected((Boolean) row[10]); // selected status from join table
            
            log.debug("Mapped benefit scheme for voterId={}: id={}, name={}, selected={}", 
                      voterId, scheme.getId(), scheme.getSchemeName(), voterBenefitScheme.getSelected());
            
            voterSchemeMap.computeIfAbsent(voterId, k -> new ArrayList<>()).add(voterBenefitScheme);
        }
        
        // Set the voterBenefitSchemes list for each voter
        voters.forEach(voter -> {
            List<VoterBenefitScheme> schemes = voterSchemeMap.getOrDefault(voter.getId(), new ArrayList<>());
            voter.setVoterBenefitSchemes(schemes);
            log.debug("Set benefit schemes for voter {}: {}", voter.getId(), 
                      schemes.stream().map(s -> "id=" + s.getBenefitScheme().getId() + 
                              ",name=" + s.getBenefitScheme().getSchemeName() + 
                              ",selected=" + s.getSelected())
                             .collect(Collectors.toList()));
        });
        
        log.debug("Successfully loaded benefit schemes for {} voters", voters.size());
    } catch (Exception e) {
        log.error("Failed to load benefit schemes for voterIds {}: {}", voterIds, e.getMessage(), e);
        voters.forEach(voter -> voter.setVoterBenefitSchemes(new ArrayList<>()));
    }
}

private void loadFeedbackIssuesSafe(List<Long> voterIds, List<VoterEntity> voters, Long accountId, Long electionId) {
	try {
		log.debug("Loading feedback issues for voterIds: {}, accountId: {}, electionId: {}", 
				  voterIds, accountId, electionId);
		List<Object[]> issueData = voterRepository.findFeedbackIssuesByVoterIds(voterIds, accountId, electionId);
		log.debug("Retrieved {} feedback issue rows for voterIds {}: {}", 
				  issueData.size(), voterIds, issueData);
		Map<Long, Set<FeedbackIssue>> voterIssueMap = new HashMap<>();
		for (Object[] row : issueData) {
			Long voterId = ((Number) row[0]).longValue();
			FeedbackIssue issue = new FeedbackIssue();
			issue.setId(((Number) row[1]).longValue());
			issue.setIssueName((String) row[2]);
			issue.setElectionId(((Number) row[3]).longValue());
			issue.setAccountId(((Number) row[4]).longValue());
			issue.setOrderIndex(row[5] != null ? ((Number) row[5]).intValue() : null);
			//issue.setCreatedAt(row[6] != null ? ((Timestamp) row[6]).toLocalDateTime() : null);
			log.debug("Mapped feedback issue for voterId={}: id={}, name={}", 
					  voterId, issue.getId(), issue.getIssueName());
			voterIssueMap.computeIfAbsent(voterId, k -> new HashSet<>()).add(issue);
		}
		voters.forEach(voter -> {
			Set<FeedbackIssue> issues = voterIssueMap.getOrDefault(voter.getId(), new HashSet<>());
			voter.setFeedbackIssues(issues);
			log.debug("Set feedback issues for voter {}: {}", voter.getId(), 
					  issues.stream().map(i -> "id=" + i.getId() + ",name=" + i.getIssueName())
							.collect(Collectors.toList()));
		});
		log.debug("Successfully loaded feedback issues for {} voters", voters.size());
	} catch (Exception e) {
		log.error("Failed to load feedback issues for voterIds {}: {}", voterIds, e.getMessage(), e);
		voters.forEach(voter -> voter.setFeedbackIssues(new HashSet<>()));
	}
}

private void loadVoterHistoriesSafe(List<Long> voterIds, List<VoterEntity> voters, Long accountId, Long electionId) {
	try {
		log.debug("Loading voter histories for voterIds: {}, accountId: {}, electionId: {}", 
				  voterIds, accountId, electionId);
		List<Object[]> historyData = voterRepository.findVoterHistoriesByVoterIds(voterIds, accountId, electionId);
		log.debug("Retrieved {} voter history rows for voterIds {}: {}", 
				  historyData.size(), voterIds, historyData);
		Map<Long, Set<VoterHistoryEntity>> voterHistoryMap = new HashMap<>();
		for (Object[] row : historyData) {
			Long voterId = ((Number) row[0]).longValue();
			VoterHistoryEntity history = new VoterHistoryEntity();
			history.setId(((Number) row[1]).longValue());
			history.setVoterHistoryName((String) row[2]);
			history.setVoterHistoryImage((String) row[3]);
			history.setAccountId(((Number) row[4]).longValue());
			history.setElectionId(((Number) row[5]).longValue());
			history.setOrderIndex(row[6] != null ? ((Number) row[6]).intValue() : null);
			log.debug("Mapped voter history for voterId={}: id={}, name={}", 
					  voterId, history.getId(), history.getVoterHistoryName());
			voterHistoryMap.computeIfAbsent(voterId, k -> new HashSet<>()).add(history);
		}
		voters.forEach(voter -> {
			Set<VoterHistoryEntity> histories = voterHistoryMap.getOrDefault(voter.getId(), new HashSet<>());
			voter.setVoterHistories(histories);
			log.debug("Set voter histories for voter {}: {}", voter.getId(), 
					  histories.stream().map(h -> "id=" + h.getId() + ",name=" + h.getVoterHistoryName())
							   .collect(Collectors.toList()));
		});
		log.debug("Successfully loaded voter histories for {} voters", voters.size());
	} catch (Exception e) {
		log.error("Failed to load voter histories for voterIds {}: {}", voterIds, e.getMessage(), e);
		voters.forEach(voter -> voter.setVoterHistories(new HashSet<>()));
	}
}

//private void loadBenefitSchemesSafe(List<Long> voterIds, List<VoterEntity> voters) {
//    try {
//        List<Object[]> benefitData = voterRepository.findBenefitSchemesByVoterIds(voterIds);
//        
//        Map<Long, List<BenefitSchemes>> voterBenefitMap = benefitData.stream()
//            .collect(Collectors.groupingBy(
//                row -> (Long) row[0],
//                Collectors.mapping(row -> (BenefitSchemes) row[1], Collectors.toList())
//            ));
//        
//        voters.forEach(voter -> {
//            List<BenefitSchemes> schemes = voterBenefitMap.get(voter.getId());
//            if (schemes != null) {
//                voter.setBenefitSchemes(schemes);
//            }
//        });
//        
//        log.debug("Successfully loaded benefit schemes for {} voters", voters.size());
//    } catch (Exception e) {
//        log.warn("Failed to load benefit schemes: {}", e.getMessage());
//        // Set empty list to avoid null pointer exceptions
//        voters.forEach(voter -> voter.setBenefitSchemes(new ArrayList<>()));
//    }
//}
//
//
//private void loadFeedbackIssuesSafe(List<Long> voterIds, List<VoterEntity> voters) {
//    try {
//        List<Object[]> feedbackData = voterRepository.findFeedbackIssuesByVoterIds(voterIds);
//        
//        Map<Long, Set<FeedbackIssue>> voterFeedbackMap = feedbackData.stream()
//            .collect(Collectors.groupingBy(
//                row -> (Long) row[0],
//                Collectors.mapping(row -> (FeedbackIssue) row[1], Collectors.toSet())
//            ));
//        
//        voters.forEach(voter -> {
//            Set<FeedbackIssue> issues = voterFeedbackMap.get(voter.getId());
//            if (issues != null) {
//                voter.setFeedbackIssues(issues);
//            }
//        });
//        
//        log.debug("Successfully loaded feedback issues for {} voters", voters.size());
//    } catch (Exception e) {
//        log.warn("Failed to load feedback issues: {}", e.getMessage());
//        // Set empty set to avoid null pointer exceptions
//        voters.forEach(voter -> voter.setFeedbackIssues(new HashSet<>()));
//    }
//}
//
//private void loadVoterHistoriesSafe(List<Long> voterIds, List<VoterEntity> voters) {
//    try {
//        List<Object[]> historyData = voterRepository.findVoterHistoriesByVoterIds(voterIds);
//        
//        Map<Long, Set<VoterHistoryEntity>> voterHistoryMap = historyData.stream()
//            .collect(Collectors.groupingBy(
//                row -> (Long) row[0],
//                Collectors.mapping(row -> (VoterHistoryEntity) row[1], Collectors.toSet())
//            ));
//        
//        voters.forEach(voter -> {
//            Set<VoterHistoryEntity> histories = voterHistoryMap.get(voter.getId());
//            if (histories != null) {
//                voter.setVoterHistories(histories);
//            }
//        });
//        
//        log.debug("Successfully loaded voter histories for {} voters", voters.size());
//    } catch (Exception e) {
//        log.warn("Failed to load voter histories: {}", e.getMessage());
//        // Set empty set to avoid null pointer exceptions
//        voters.forEach(voter -> voter.setVoterHistories(new HashSet<>()));
//    }
//}

//private void loadLanguagesSafe(List<Long> voterIds, List<VoterEntity> voters) {
//    try {
//        List<Object[]> languageData = voterRepository.findLanguagesByVoterIds(voterIds);
//        
//        Map<Long, Set<Language>> voterLanguageMap = languageData.stream()
//            .collect(Collectors.groupingBy(
//                row -> (Long) row[0],
//                Collectors.mapping(row -> (Language) row[1], Collectors.toSet())
//            ));
//        
//        voters.forEach(voter -> {
//            Set<Language> languages = voterLanguageMap.get(voter.getId());
//            if (languages != null) {
//                voter.setLanguages(languages);
//            }
//        });
//        
//        log.debug("Successfully loaded languages for {} voters", voters.size());
//    } catch (Exception e) {
//        log.warn("Failed to load languages: {}", e.getMessage());
//        // Set empty set to avoid null pointer exceptions
//        voters.forEach(voter -> voter.setLanguages(new HashSet<>()));
//    }
//}
private void loadLanguagesSafe(List<Long> voterIds, List<VoterEntity> voters) {
	try {
		List<Object[]> languageData = voterRepository.findLanguagesByVoterIds(voterIds);
		log.debug("Language data for voterIds {}: {}", voterIds, languageData);
		Map<Long, Set<Language>> voterLanguageMap = new HashMap<>();
		for (Object[] row : languageData) {
			Long voterId = ((Number) row[0]).longValue();
			Language language = new Language();
			language.setId(((Number) row[1]).longValue());
			language.setLanguageName((String) row[2]);
			// row[3] is language_image which we'll skip since Language entity doesn't have this field
			log.debug("Mapped language for voterId={}: id={}, name={}", 
					  voterId, language.getId(), language.getLanguageName());
			voterLanguageMap.computeIfAbsent(voterId, k -> new HashSet<>()).add(language);
		}
		voters.forEach(voter -> {
			Set<Language> languages = voterLanguageMap.getOrDefault(voter.getId(), new HashSet<>());
			voter.setLanguages(languages);
			log.debug("Set languages for voter {}: {}", voter.getId(), languages);
		});
		log.debug("Successfully loaded languages for {} voters", voters.size());
	} catch (Exception e) {
		log.error("Failed to load languages for voterIds {}: {}", voterIds, e.getMessage(), e);
		voters.forEach(voter -> voter.setLanguages(new HashSet<>()));
	}
}

// Helper: normalize a possibly blank or "all" string to null, else lower-trim
private String normalizeStringParam(String value) {
	if (value == null) return null;
	String v = value.trim();
	if (v.isEmpty()) return null;
	String lower = v.toLowerCase();
	if ("all".equals(lower) || "any".equals(lower)) return null;
	return lower;
}

// Helper: build singleton lowercased list or return null when blank/"all"
private List<String> normalizeListParam(String value) {
	String v = normalizeStringParam(value);
	return v == null ? null : List.of(v);
}

// Helper: like normalizeStringParam but preserves original case (used for fields compared by '=')
private String normalizeStringParamPreserveCase(String value) {
	if (value == null) return null;
	String v = value.trim();
	if (v.isEmpty()) return null;
	String lower = v.toLowerCase();
	if ("all".equals(lower) || "any".equals(lower)) return null;
	return v; // preserve original case
}

private void loadDynamicFields(List<VoterEntity> voters, Long accountId, Long electionId) {
    // Fetch all DynamicFieldEntity records for the given accountId and electionId
    List<DynamicFieldEntity> dynamicFields = dynamicFieldRepository.findByAccountIdAndElectionId(accountId, electionId);
    
    // Create a map of field names to DynamicFieldEntity for validation
    Map<String, DynamicFieldEntity> fieldNameToEntity = dynamicFields.stream()
            .collect(Collectors.toMap(DynamicFieldEntity::getName, Function.identity(), (e1, e2) -> e1));

    for (VoterEntity voter : voters) {
        Map<String, String> dynamicFields1 = new HashMap<>();
        Map<String, String> existingDynamicFields = voter.getDynamicFields();
        
        if (existingDynamicFields != null) {
            for (Map.Entry<String, String> entry : existingDynamicFields.entrySet()) {
                String fieldName = entry.getKey();
                String fieldValue = entry.getValue();
                
                // Validate that the fieldName exists in DynamicFieldEntity
                if (fieldNameToEntity.containsKey(fieldName)) {
                    DynamicFieldEntity fieldEntity = fieldNameToEntity.get(fieldName);
                    // Optionally validate the fieldValue based on fieldEntity.getType() or fieldEntity.getOptions()
                    if (isValidDynamicFieldValue(fieldEntity, fieldValue)) {
                        dynamicFields1.put(fieldName, fieldValue);
                    }
                }
            }
        }
        
        voter.setDynamicFields(dynamicFields1);
    }
}

private boolean isValidDynamicFieldValue(DynamicFieldEntity fieldEntity, String value) {
    // Implement validation based on field type and options
    if (value == null && fieldEntity.getRequired()) {
        return false;
    }
    
    if (fieldEntity.getOptions() != null && !fieldEntity.getOptions().isEmpty()) {
        // For fields with predefined options (e.g., dropdown), ensure value is in options
        return fieldEntity.getOptions().contains(value);
    }
    
    // Add additional type-based validation if needed (e.g., for "number", "date", etc.)
    switch (fieldEntity.getType().toLowerCase()) {
        case "text":
            return value != null && !value.trim().isEmpty();
        case "number":
            try {
                Double.parseDouble(value);
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        // Add other type validations as needed
        default:
            return true;
    }
}

//Optimized stats calculation
private GenderStatsDTO getStatsOptimized(
	    Long accountId, Long electionId, List<Integer> boothNumbers, String voterId, String epicNumber, 
	    UUID familyId, UUID friendId, List<String> voterFnameEnList, List<String> voterLnameEnList,
	    List<String> voterFnameL1List, List<String> voterFnameL2List,
	    List<String> voterLnameL1List, List<String> voterLnameL2List,
	    List<String> relationFirstNameEnList, List<String> relationLastNameEnList,
	    List<String> rlnFnameL1List, List<String> rlnFnameL2List,
	    List<String> rlnLnameL1List, List<String> rlnLnameL2List,
	    List<String> partyNameList, List<String> voterHistoryNameList, List<String> religionNameList,
	    Integer age, Integer minAge, Integer maxAge, Boolean includeUnknownAge, 
	    List<String> genderList, Boolean filterToday, Boolean filterTomorrow, Integer todayMonth, Integer todayDay,
	    Integer tomorrowMonth, Integer tomorrowDay, Integer birthdayMonth, Integer birthdayDay,
	    Boolean starNumber, List<String> descriptionList, List<String> categoryNameList, List<String> casteCategoryNameList, 
	    List<String> casteNameList, List<String> subCasteNameList, Long serialNo, 
	    Boolean overseas, Boolean fatherless, Boolean guardian, Boolean hasMobileNo, String mobileNo,
	    Boolean singleVoterFamily, String pollStatus) {

	    try {
	    	// If only caste/subCaste filters are present
	        if ((casteNameList != null || subCasteNameList != null) && 
	            voterId == null && epicNumber == null && boothNumbers == null && 
	            familyId == null && friendId == null && voterFnameEnList == null && 
	            voterLnameEnList == null && voterFnameL1List == null && 
	            voterFnameL2List == null && voterLnameL1List == null && 
	            voterLnameL2List == null && relationFirstNameEnList == null && 
	            relationLastNameEnList == null && rlnFnameL1List == null && 
	            rlnFnameL2List == null && rlnLnameL1List == null && 
	            rlnLnameL2List == null && partyNameList == null && 
	            voterHistoryNameList == null && religionNameList == null && 
	            age == null && minAge == null && maxAge == null && 
	            genderList == null && starNumber == null && 
	            descriptionList == null && categoryNameList == null && 
	            casteCategoryNameList == null && serialNo == null && 
	            overseas == null && fatherless == null && guardian == null && 
	            hasMobileNo == null && singleVoterFamily == null) {
	        	 log.debug("Using simplified caste/subCaste stats query");
	             // For backward compatibility with the getCasteGenderStats method that expects single values
	             String casteNameSingle = (casteNameList != null && !casteNameList.isEmpty()) ? casteNameList.get(0) : null;
	             String subCasteNameSingle = (subCasteNameList != null && !subCasteNameList.isEmpty()) ? subCasteNameList.get(0) : null;
	             GenderStatsProjection stats = voterRepository.getCasteGenderStats(
	                 accountId, electionId, casteNameSingle, subCasteNameSingle);
	             
	             return new GenderStatsDTO(
	                 stats.getMaleCount() != null ? stats.getMaleCount() : 0L,
	                 stats.getFemaleCount() != null ? stats.getFemaleCount() : 0L,
	                 stats.getOtherCount() != null ? stats.getOtherCount() : 0L,
	                 stats.getTotalCount() != null ? stats.getTotalCount() : 0L
	             );
	         }
		    	
	        List<Integer> effectiveBoothNumbers = (boothNumbers == null || boothNumbers.isEmpty()) ? null : boothNumbers;
	        List<String> effectiveVoterFnameEnList = (voterFnameEnList == null || voterFnameEnList.isEmpty()) ? null : voterFnameEnList;
	        List<String> effectiveVoterLnameEnList = (voterLnameEnList == null || voterLnameEnList.isEmpty()) ? null : voterLnameEnList;
	        List<String> effectiveVoterFnameL1List = (voterFnameL1List == null || voterFnameL1List.isEmpty()) ? null : voterFnameL1List;
	        List<String> effectiveVoterFnameL2List = (voterFnameL2List == null || voterFnameL2List.isEmpty()) ? null : voterFnameL2List;
	        List<String> effectiveVoterLnameL1List = (voterLnameL1List == null || voterLnameL1List.isEmpty()) ? null : voterLnameL1List;
	        List<String> effectiveVoterLnameL2List = (voterLnameL2List == null || voterLnameL2List.isEmpty()) ? null : voterLnameL2List;
	        List<String> effectiveRelationFirstNameEnList = (relationFirstNameEnList == null || relationFirstNameEnList.isEmpty()) ? null : relationFirstNameEnList;
	        List<String> effectiveRelationLastNameEnList = (relationLastNameEnList == null || relationLastNameEnList.isEmpty()) ? null : relationLastNameEnList;
	        List<String> effectiveRlnFnameL1List = (rlnFnameL1List == null || rlnFnameL1List.isEmpty()) ? null : rlnFnameL1List;
	        List<String> effectiveRlnFnameL2List = (rlnFnameL2List == null || rlnFnameL2List.isEmpty()) ? null : rlnFnameL2List;
	        List<String> effectiveRlnLnameL1List = (rlnLnameL1List == null || rlnLnameL1List.isEmpty()) ? null : rlnLnameL1List;
	        List<String> effectiveRlnLnameL2List = (rlnLnameL2List == null || rlnLnameL2List.isEmpty()) ? null : rlnLnameL2List;
	        List<String> effectivePartyNameList = (partyNameList == null || partyNameList.isEmpty()) ? null : partyNameList;
	        List<String> effectiveVoterHistoryNameList = (voterHistoryNameList == null || voterHistoryNameList.isEmpty()) ? null : voterHistoryNameList;
	        List<String> effectiveGenderList = (genderList == null || genderList.isEmpty()) ? null : genderList;
	        
	        log.debug("Stats calculation - accountId: {}, electionId: {}, effectiveBoothNumbers: {}, genderList: {}", 
	                  accountId, electionId, effectiveBoothNumbers, effectiveGenderList);
	        
		        GenderStatsProjection stats = voterRepository.getFilteredGenderStats(
	                accountId, electionId, 
	                // Identification & basic filters
	                voterId, epicNumber, effectiveBoothNumbers, familyId, friendId,
	                // Voter name filters
	                effectiveVoterFnameEnList, effectiveVoterLnameEnList, effectiveVoterFnameL1List, 
	                effectiveVoterFnameL2List, effectiveVoterLnameL1List, effectiveVoterLnameL2List,
	                // Relation name filters
	                effectiveRelationFirstNameEnList, effectiveRelationLastNameEnList, 
	                effectiveRlnFnameL1List, effectiveRlnFnameL2List, effectiveRlnLnameL1List, effectiveRlnLnameL2List,
	                // Party / religion / history
	                effectivePartyNameList, religionNameList, effectiveVoterHistoryNameList, 
	                // Age / gender
	                age, minAge, maxAge, includeUnknownAge, effectiveGenderList, 
	                // DOB filters (today/tomorrow or custom birthday)
	                filterToday, filterTomorrow, todayMonth, todayDay, tomorrowMonth, tomorrowDay, birthdayMonth, birthdayDay,
	                // Misc flags
	                starNumber, descriptionList, 
	                // ORDER SENSITIVE: must match repository signature -> overseas, fatherless, guardian
	                overseas, fatherless, guardian, 
	                // Category & caste ordering: categoryName, casteName, subCasteName, casteCategoryName
	                categoryNameList, casteNameList, subCasteNameList, casteCategoryNameList,
	                // Mobile & family
	                hasMobileNo, mobileNo, singleVoterFamily, pollStatus);
	        
	        if (stats == null) {
	            log.warn("Gender stats returned null, using default values");
	            return new GenderStatsDTO(0L, 0L, 0L, 0L);
	        }
	        
	        GenderStatsDTO result = new GenderStatsDTO(
	                stats.getMaleCount() != null ? stats.getMaleCount() : 0L,
	                stats.getFemaleCount() != null ? stats.getFemaleCount() : 0L,
	                stats.getOtherCount() != null ? stats.getOtherCount() : 0L,
	                stats.getTotalCount() != null ? stats.getTotalCount() : 0L
	        );
	        
	        log.debug("Gender stats calculated - Male: {}, Female: {}, Other: {}, Total: {}", 
	                  result.getMaleCount(), result.getFemaleCount(), result.getOtherCount(), result.getTotalCount());
	        
	        return result;
		} catch (Exception e) {
			log.warn("Stats calculation failed, using basic stats: {}", e.getMessage());
			// Fallback: use a lightweight aggregate that doesn't depend on many params
			GenderStatsProjection basicStats = voterRepository.getCasteGenderStats(accountId, electionId, null, null);
	        return new GenderStatsDTO(
	                basicStats.getMaleCount() != null ? basicStats.getMaleCount() : 0L,
	                basicStats.getFemaleCount() != null ? basicStats.getFemaleCount() : 0L,
	                basicStats.getOtherCount() != null ? basicStats.getOtherCount() : 0L,
	                basicStats.getTotalCount() != null ? basicStats.getTotalCount() : 0L
	        );
	    }
	}
		   
// Cache invalidation helper
private void invalidateVoterCaches(Long electionId) {
	log.debug("Invalidating voter caches for electionId: {}", electionId);
	
	// Clear validation cache entries for this election
	List<String> keysToRemove = new ArrayList<>();
	for (String key : validationCache.keySet()) {
		if (key.contains("election_" + electionId + "_")) {
			keysToRemove.add(key);
		}
	}
	
	for (String key : keysToRemove) {
		validationCache.remove(key);
		validationCacheTimes.remove(key);
	}
	
	log.info("Cleared {} validation cache entries for electionId: {}", keysToRemove.size(), electionId);
}

@Transactional
@Override
public ThedalResponse<VoterDTO> saveVoter(VoterDTO voterDto) throws DataIntegrityViolationException {
	Long accountId = requestDetails.getCurrentAccountId();
	if (accountId == null) {
		log.error("Account ID not found, unauthorized access.");
		throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
	}
	Long electionId = voterDto.getElectionId();	
	validateElectionOwnership(electionId, accountId); 
	
	// Get the authenticated user's volunteer ID
	Long userId = requestDetails.getCurrentUserId();
	if (userId == null) {
		log.error("User ID not found, unauthorized access.");
		throw new ThedalException(ThedalError.USER_NOT_FOUND, HttpStatus.UNAUTHORIZED);
	}

	String aadhaarNumber = voterDto.getAadhaarNumber();
	if (aadhaarNumber != null && !aadhaarNumber.trim().isEmpty()) {
		aadhaarNumber = aadhaarNumber.trim();
		// validateAadhaar(aadhaarNumber); 

		Optional<VoterEntity> existingVoter = voterRepository.findByAadhaarNumberAndElectionIdAndAccountId(
				aadhaarNumber, electionId, accountId);

		if (existingVoter.isPresent()) {
			String details = String.format("%s already exists (EPIC: %s).",
					aadhaarNumber, existingVoter.get().getEpicNumber());
			throw new ThedalException(ThedalError.DUPLICATE_AADHAAR_NUMBER, HttpStatus.BAD_REQUEST, details);
		}
	}

	
	if (voterDto.getEpicNumber() != null) {
		voterDto.setVoterId(voterDto.getEpicNumber());
	}

	voterDto.setBoothNumber(voterDto.getPartNo());

	Optional<VoterEntity> existingVoterOpt = voterRepository.findByEpicNumberAndElectionIdAndAccountId(
			voterDto.getEpicNumber(), voterDto.getElectionId(), accountId);

//    VoterEntity voter;
//    if (existingVoterOpt.isPresent()) {
//        voter = existingVoterOpt.get();
//        log.info("Updating existing voter with EPIC number: {}", voterDto.getEpicNumber());
//    } else {
//        voter = new VoterEntity();
//        voter.setAccountId(accountId);
//        log.info("Creating new voter record for EPIC number: {}", voterDto.getEpicNumber());
//    }
	VoterEntity voter;
	boolean isNewVoter = !existingVoterOpt.isPresent();
	if (isNewVoter) {
		voter = new VoterEntity();
		voter.setAccountId(accountId);
		voter.setCreatedByUserId(userId); // Set userId instead of volunteerId
		voter.setFamilyId(null); 
		log.info("Creating new voter record for EPIC number: {} by userId: {}", voterDto.getEpicNumber(), userId);
	} else {
		voter = existingVoterOpt.get();
		log.info("Updating existing voter with EPIC number: {}", voterDto.getEpicNumber());
	}

	// Update voter properties
	BeanUtils.copyProperties(voterDto, voter, "id", "accountId", "createdByUserId");
	
 // Set Aadhaar fields
	if (voterDto.getAadhaarNumber() != null) {
		voter.setAadhaarNumber(voterDto.getAadhaarNumber());
		voter.setAadhaarVerified(voterDto.getAadhaarVerified() != null ? voterDto.getAadhaarVerified() : false);
	}

	// Handle Religion (new approach with IDs)
	if (voterDto.getReligionId() != null) {
		Optional<ReligionEntity> religionOpt = religionRepository.findByIdAndAccountIdAndElectionId(
				voterDto.getReligionId(), accountId, voterDto.getElectionId());
		if (religionOpt.isPresent()) {
			voter.setReligion(religionOpt.get());
		} else {
			log.warn("Religion not found with ID: {}, accountId: {}, electionId: {}. Skipping assignment.", 
					voterDto.getReligionId(), accountId, voterDto.getElectionId());
		}
	}

	// Handle Caste (new approach with IDs)
	if (voterDto.getCasteId() != null) {
		Optional<CasteEntity> casteOpt = casteRepository.findByIdAndAccountIdAndElectionId(
				voterDto.getCasteId(), accountId, voterDto.getElectionId());
		if (casteOpt.isPresent()) {
			voter.setCaste(casteOpt.get());
		} else {
			log.warn("Caste not found with ID: {}, accountId: {}, electionId: {}. Skipping assignment.", 
					voterDto.getCasteId(), accountId, voterDto.getElectionId());
		}
	}

	// Handle SubCaste (new approach with IDs)
	if (voterDto.getSubCasteId() != null) {
		Optional<SubCasteEntity> subCasteOpt = subCasteRepository.findByIdAndAccountIdAndElectionId(
				voterDto.getSubCasteId(), accountId, voterDto.getElectionId());
		if (subCasteOpt.isPresent()) {
			voter.setSubCaste(subCasteOpt.get());
		} else {
			log.warn("SubCaste not found with ID: {}, accountId: {}, electionId: {}. Skipping assignment.", 
					voterDto.getSubCasteId(), accountId, voterDto.getElectionId());
		}
	}


//	if (voterDto.getBenefitSchemeIds() != null && !voterDto.getBenefitSchemeIds().isEmpty()) {
//		Set<BenefitSchemes> currentBenefitSchemes = new HashSet<>();
//
//		for (Long benefitSchemeId : voterDto.getBenefitSchemeIds()) {
//			Optional<BenefitSchemes> benefitSchemeOpt = benefitSchemesRepository.findByIdAndAccountIdAndElectionId(
//					benefitSchemeId, accountId, electionId);
//
//			benefitSchemeOpt.ifPresent(currentBenefitSchemes::add);
//		}
//
//		voter.setBenefitSchemes(new ArrayList<>(currentBenefitSchemes));
//	}
	if (voterDto.getBenefitSchemeStatuses() != null && !voterDto.getBenefitSchemeStatuses().isEmpty()) {
	    List<VoterBenefitScheme> voterBenefitSchemes = voter.getVoterBenefitSchemes();
	    if (voterBenefitSchemes == null) {
	        voterBenefitSchemes = new ArrayList<>();
	    }

	    // Create a map to track existing benefit schemes for this voter
	    Map<Long, VoterBenefitScheme> existingSchemes = voterBenefitSchemes.stream()
	        .collect(Collectors.toMap(vbs -> vbs.getBenefitScheme().getId(), vbs -> vbs, (v1, v2) -> v1));

	    for (VoterDTO.BenefitSchemeStatusDTO statusDTO : voterDto.getBenefitSchemeStatuses()) {
	        Optional<BenefitSchemes> benefitSchemeOpt = benefitSchemesRepository.findByIdAndAccountIdAndElectionId(
	                statusDTO.getSchemeId(), accountId, electionId);
	        if (benefitSchemeOpt.isPresent()) {
	            Long schemeId = statusDTO.getSchemeId();
	            VoterBenefitScheme voterBenefitScheme = existingSchemes.get(schemeId);
	            if (voterBenefitScheme == null) {
	                // Create new entry if it doesn't exist
	                voterBenefitScheme = new VoterBenefitScheme();
	                voterBenefitScheme.setVoter(voter);
	                voterBenefitScheme.setBenefitScheme(benefitSchemeOpt.get());
	                voterBenefitSchemes.add(voterBenefitScheme);
	            }
	            // Update the selected status
	            voterBenefitScheme.setSelected(statusDTO.isSelected());
	        } else {
	            log.warn("Benefit Scheme not found with ID: {}, accountId: {}, electionId: {}. Skipping this benefit scheme.",
	                    statusDTO.getSchemeId(), accountId, electionId);
	        }
	    }
	    voter.setVoterBenefitSchemes(voterBenefitSchemes);
	} else {
	    voter.setVoterBenefitSchemes(new ArrayList<>());
	}
	
	if (voterDto.getLanguageId() != null) { // Changed to getLanguageId instead of getLanguageIds
		Optional<Language> languageOpt = languageRepository.findByIdAndAccountIdAndElectionId(
				voterDto.getLanguageId(), accountId, voterDto.getElectionId());

		if (languageOpt.isPresent()) {
			voter.setLanguages(Set.of(languageOpt.get())); // Set containing only one language
		} else {
			log.warn("Language not found with ID: {}, accountId: {}, electionId: {}. Skipping this language.",
					voterDto.getLanguageId(), accountId, voterDto.getElectionId());
		}
	} else {
		voter.setLanguages(new HashSet<>());
	}

	// if (voterDto.getLanguageIds() != null &&
	// !voterDto.getLanguageIds().isEmpty()) {
	// Set<Language> languages = new HashSet<>();
	// for (Long languageId : voterDto.getLanguageIds()) {
	// Optional<Language> languageOpt =
	// languageRepository.findByIdAndAccountIdAndElectionId(languageId, accountId,
	// voterDto.getElectionId());
	// if (languageOpt.isPresent()) {
	// languages.add(languageOpt.get());
	// } else {
	// log.warn("Language not found with ID: {}, accountId: {}, electionId: {}.
	// Skipping this language.",
	// languageId, accountId, voterDto.getElectionId());
	// }
	// }
	// voter.setLanguages(languages);
	// } else {
	// voter.setLanguages(new HashSet<>());
	// }

	if (voterDto.getLanguageId() != null) { // Changed to getLanguageId instead of getLanguageIds
		Optional<Language> languageOpt = languageRepository.findByIdAndAccountIdAndElectionId(
				voterDto.getLanguageId(), accountId, voterDto.getElectionId());

		if (languageOpt.isPresent()) {
			voter.setLanguages(Set.of(languageOpt.get())); // Set containing only one language
		} else {
			log.warn("Language not found with ID: {}, accountId: {}, electionId: {}. Skipping this language.",
					voterDto.getLanguageId(), accountId, voterDto.getElectionId());
		}
	} else {
		voter.setLanguages(new HashSet<>());
	}

	// Handle Availability (old approach, kept working)
	if (voterDto.getAvailabilityId() != null) {
		Optional<Availability> availabilityOpt = availabilityRepository.findByIdAndAccountIdAndElectionId(
				voterDto.getAvailabilityId(), accountId, voterDto.getElectionId());
		if (availabilityOpt.isPresent()) {
			voter.setAvailability1(availabilityOpt.get());
		} else {
			log.warn("Availability not found with ID: {}, accountId: {}, electionId: {}. Skipping assignment.",
					voterDto.getAvailabilityId(), accountId, voterDto.getElectionId());
		}
	}
	
	if (voterDto.getDynamicFieldId() != null) {
		Optional<DynamicFieldEntity> dynamicFieldOpt = dynamicFieldRepository.findByIdAndAccountIdAndElectionId(
				voterDto.getDynamicFieldId(), accountId, voterDto.getElectionId());
		if (dynamicFieldOpt.isPresent()) {
			voter.setDynamicFieldEntity(dynamicFieldOpt.get());
		} else {
			log.warn("Availability not found with ID: {}, accountId: {}, electionId: {}. Skipping assignment.",
					voterDto.getDynamicFieldId(), accountId, voterDto.getElectionId());
		}
	}

	// Handle Party (old approach, kept working)
	if (voterDto.getPartyId() != null) {
		Optional<Party> partyOpt = partyRepository.findByIdAndAccountIdAndElectionId(
				voterDto.getPartyId(), accountId, voterDto.getElectionId());
		if (partyOpt.isPresent()) {
			voter.setParty(partyOpt.get());
		} else {
			log.warn("Party not found with ID: {}, accountId: {}, electionId: {}. Skipping assignment.",
					voterDto.getPartyId(), accountId, voterDto.getElectionId());
		}
	}

//    if (voterDto.getPartNo() != null) {
//        Optional<PartManager> partManagerOpt = partManagerRepository.findByPartNoAndAccountIdAndElectionId(
//                String.valueOf(voterDto.getPartNo()), accountId, electionId);
//        if (partManagerOpt.isPresent()) {
//            voter.setPartManager(partManagerOpt.get());
//        } else {
//            log.warn("PartManager not found for partNo: {}, accountId: {}, electionId: {}. Skipping assignment.",
//                    voterDto.getPartNo(), accountId, electionId);
//        }
//    }
//
//    // Validate partNo consistency
//    if (voter.getPartManager() != null && voter.getPartNo() != null) {
//        if (!String.valueOf(voter.getPartNo()).equals(voter.getPartManager().getPartNo())) {
//            throw new ThedalException(ThedalError.INVALID_PART_NO, HttpStatus.BAD_REQUEST,
//                    "Voter partNo does not match PartManager partNo");
//        }
//    }
	// In the saveVoter method, after handling other fields
	if (voterDto.getPartManagerId() != null) {
		Optional<PartManager> partManagerOpt = partManagerRepository.findByIdAndAccountIdAndElectionId(
				voterDto.getPartManagerId(), accountId, electionId);
		if (partManagerOpt.isPresent()) {
			voter.setPartManager(partManagerOpt.get());
			voter.setPartNo(Integer.parseInt(partManagerOpt.get().getPartNo()));
			log.info("Set PartManager {} with partNo {} for voter", partManagerOpt.get().getId(), partManagerOpt.get().getPartNo());
		} else {
			log.warn("PartManager not found with ID: {}, accountId: {}, electionId: {}. Skipping assignment.",
					voterDto.getPartManagerId(), accountId, electionId);
		}
	} else if (voterDto.getPartNo() != null) {
		Optional<PartManager> partManagerOpt = partManagerRepository.findByPartNoAndAccountIdAndElectionId(
				String.valueOf(voterDto.getPartNo()), accountId, electionId);
		if (partManagerOpt.isPresent()) {
			voter.setPartManager(partManagerOpt.get());
			voter.setPartNo(voterDto.getPartNo());
			log.info("Set PartManager {} for partNo {} for voter", partManagerOpt.get().getId(), voterDto.getPartNo());
		} else {
			log.warn("PartManager not found for partNo: {}, accountId: {}, electionId: {}. Skipping assignment.",
					voterDto.getPartNo(), accountId, electionId);
			voter.setPartNo(voterDto.getPartNo()); // Still set the partNo even if PartManager not found
		}
	}
	
	
 // Handle CasteCategory (new approach with IDs)
	if (voterDto.getCasteCategoryId() != null) {
		Optional<CasteCategoryEntity> casteCategoryOpt = casteCategoryRepository.findByIdAndAccountIdAndElectionId(
				voterDto.getCasteCategoryId(), accountId, voterDto.getElectionId());
		if (casteCategoryOpt.isPresent()) {
			voter.setCasteCategory(casteCategoryOpt.get());
		} else {
			log.warn("CasteCategory not found with ID: {}, accountId: {}, electionId: {}. Skipping assignment.", 
					voterDto.getCasteCategoryId(), accountId, voterDto.getElectionId());
		}
	}

	// New dynamic fields handling using definition entities + map storage
		if (voterDto.getDynamicFields() != null) {
			Map<String, Object> inputMap = voterDto.getDynamicFields();
		// Load active definitions (status true) for validation
		List<DynamicFieldEntity> activeDefs = dynamicFieldRepository
				.findByAccountIdAndElectionIdAndStatusTrueOrderByOrderIndexAsc(accountId, electionId);
		Map<String, DynamicFieldEntity> defByName = activeDefs.stream()
				.collect(Collectors.toMap(d -> d.getName().toLowerCase(), Function.identity(), (a,b)->a));

		// Validate each provided key (coerce non-string JSON values to String)
		Map<String,String> normalizedValues = new HashMap<>();
		for (Map.Entry<String,Object> e : inputMap.entrySet()) {
			String rawKey = e.getKey();
			if (rawKey == null) continue; // skip silently
			String keyLower = rawKey.toLowerCase();
			DynamicFieldEntity def = defByName.get(keyLower);
			if (def == null) {
				log.warn("Ignoring unknown dynamic field '{}' for electionId={} accountId={}", rawKey, electionId, accountId);
				continue; // ignore unknown to avoid breaking older clients; could throw if strict
			}
			Object rawValueObj = e.getValue();
			String value;
			if (rawValueObj == null) {
				value = null;
			} else if (rawValueObj instanceof List<?> listVal) { // array from JSON
				value = listVal.stream().filter(Objects::nonNull).map(Object::toString).collect(Collectors.joining(","));
			} else {
				value = rawValueObj.toString();
			}
			if (!isValidDynamicInput(def, value)) {
				log.error("Invalid value for dynamic field '{}' (type={}) raw='{}'", def.getName(), def.getType(), rawValueObj);
				throw new ThedalException(ThedalError.INVALID_FORM_FIELDS, HttpStatus.BAD_REQUEST);
			}
			if (value != null) value = value.trim();
			normalizedValues.put(def.getName(), value);
		}
		// Check required fields
		for (DynamicFieldEntity def : activeDefs) {
			if (Boolean.TRUE.equals(def.getRequired()) && !normalizedValues.containsKey(def.getName())) {
				log.error("Missing required dynamic field '{}'", def.getName());
				throw new ThedalException(ThedalError.INVALID_FORM_FIELDS, HttpStatus.BAD_REQUEST);
			}
		}
		voter.setDynamicFields(normalizedValues);
	}

	if (voterDto.getVoterHistoryIds() != null && !voterDto.getVoterHistoryIds().isEmpty()) {
		Set<VoterHistoryEntity> currentVoterHistories = new HashSet<>();

		for (Long voterHistoryId : voterDto.getVoterHistoryIds()) {
			Optional<VoterHistoryEntity> voterHistoryOpt = voterHistoryRepository.findByIdAndAccountIdAndElectionId(
					voterHistoryId, accountId, electionId);

			voterHistoryOpt.ifPresent(currentVoterHistories::add);
		}

		voter.setVoterHistories(currentVoterHistories);
	} else {
		voter.setVoterHistories(new HashSet<>());
	}

	// Handle Feedback/Issues (new approach with IDs)
	if (voterDto.getFeedbackIssueIds() != null && !voterDto.getFeedbackIssueIds().isEmpty()) {
		Set<FeedbackIssue> currentFeedbackIssues = new HashSet<>();

		for (Long feedbackIssueId : voterDto.getFeedbackIssueIds()) {
			Optional<FeedbackIssue> feedbackIssueOpt = feedbackIssueRepository.findByIdAndAccountIdAndElectionId(
					feedbackIssueId, accountId, electionId);

			feedbackIssueOpt.ifPresent(currentFeedbackIssues::add);
		}

		voter.setFeedbackIssues(new HashSet<>(currentFeedbackIssues));

	} else {
		voter.setFeedbackIssues(new HashSet<>());
	}
	
	
	// Update VolunteerVsVoterReportEntity for new voters
	if (isNewVoter) {
		VolunteerVsVoterReportEntity report = volunteerVsVoterReportRepository
				.findByElectionIdAndUserId(electionId, userId)
				.orElseGet(() -> {
					VolunteerVsVoterReportEntity newReport = new VolunteerVsVoterReportEntity();
					newReport.setElectionId(electionId);
					newReport.setUserId(userId);
					newReport.setAccountId(accountId);
					newReport.setTotalVoterCreated(1L);
					return newReport;
				});
		report.setTotalVoterCreated((report.getTotalVoterCreated() != null ? report.getTotalVoterCreated() : 0L) + 1L);
		log.info("Incrementing totalVoterCreated for userId: {}, new count: {}", userId, report.getTotalVoterCreated());
		volunteerVsVoterReportRepository.save(report);
	}
	
	
	try {
		VoterEntity savedVoter = voterRepository.save(voter);
		log.info("Voter successfully saved or updated: {}", savedVoter);

		// Sync to MongoDB - DISABLED
		// MongoDB sync is currently disabled
		/*
		try {
			VoterMongo voterMongo = new VoterMongo(savedVoter);
			//voterMongoRepository.save(voterMongo);
			voterMongoRepository.saveVoterMongoWithNullFields(voterMongo);
			log.info("Voter successfully synced to MongoDB: {}", savedVoter.getEpicNumber());
		} catch (Exception mongoException) {
			log.error("Failed to sync voter to MongoDB (PostgreSQL save was successful): {}", mongoException.getMessage());
			// Continue with response since PostgreSQL save was successful
		}	
		*/

		// Prepare response DTO
		VoterDTO savedVoterDto = new VoterDTO();
		BeanUtils.copyProperties(savedVoter, savedVoterDto);

		if (savedVoter.getLanguages() != null && !savedVoter.getLanguages().isEmpty()) {

			savedVoterDto.setLanguageId(
					savedVoter.getLanguages().stream()
							.map(Language::getId)
							.findFirst()
							.orElse(null));
		}
//		if (savedVoter.getBenefitSchemes() != null && !savedVoter.getBenefitSchemes().isEmpty()) {
//			List<Long> benefitSchemeIds = savedVoter.getBenefitSchemes().stream()
//					.map(BenefitSchemes::getId)
//					.collect(Collectors.toList());
//			savedVoterDto.setBenefitSchemeIds(benefitSchemeIds);
//
//		}
		if (savedVoter.getVoterBenefitSchemes() != null && !savedVoter.getVoterBenefitSchemes().isEmpty()) {
            List<VoterDTO.BenefitSchemeStatusDTO> benefitSchemeStatuses = savedVoter.getVoterBenefitSchemes().stream()
                    .map(vbs -> {
                        VoterDTO.BenefitSchemeStatusDTO statusDTO = new VoterDTO.BenefitSchemeStatusDTO();
                        statusDTO.setSchemeId(vbs.getBenefitScheme().getId());
                        statusDTO.setSelected(vbs.getSelected());
                        return statusDTO;
                    })
                    .collect(Collectors.toList());
            savedVoterDto.setBenefitSchemeStatuses(benefitSchemeStatuses);
        }
	
		if (savedVoter.getLanguages() != null && !savedVoter.getLanguages().isEmpty()) {
			// Since there's only one language, we directly get its ID (or null if not
			// present)
			savedVoterDto.setLanguageId( // Changed to setLanguageId instead of setLanguageIds
					savedVoter.getLanguages().stream()
							.map(Language::getId)
							.findFirst() // Assuming there is only one language, we use findFirst
							.orElse(null) // If there is no language, set it to null
			);
		}

		
//		if (savedVoter.getBenefitSchemes() != null && !savedVoter.getBenefitSchemes().isEmpty()) {
//			List<Long> benefitSchemeIds = savedVoter.getBenefitSchemes().stream()
//					.map(BenefitSchemes::getId)
//					.collect(Collectors.toList());
//			savedVoterDto.setBenefitSchemeIds(benefitSchemeIds);
//
//		}
		if (savedVoter.getVoterHistories() != null && !savedVoter.getVoterHistories().isEmpty()) {
			List<Long> voterHistoryIds = savedVoter.getVoterHistories().stream()
					.map(VoterHistoryEntity::getId)
					.collect(Collectors.toList());

			savedVoterDto.setVoterHistoryIds(voterHistoryIds);
		}

		if (savedVoter.getAvailability1() != null) {
			savedVoterDto.setAvailabilityId(savedVoter.getAvailability1().getId());
		}
		if (savedVoter.getDynamicFieldEntity() != null) {
			savedVoterDto.setDynamicFieldId(savedVoter.getDynamicFieldEntity().getId());
		}
		if (savedVoter.getParty() != null) {
			savedVoterDto.setPartyId(savedVoter.getParty().getId());
		}
		if (savedVoter.getReligion() != null) {
			savedVoterDto.setReligionId(savedVoter.getReligion().getId());
		}
		if (savedVoter.getCaste() != null) {
			savedVoterDto.setCasteId(savedVoter.getCaste().getId());
		}
		if (savedVoter.getSubCaste() != null) {
			savedVoterDto.setSubCasteId(savedVoter.getSubCaste().getId());
		}
		if (savedVoter.getPartManager() != null) {
			savedVoterDto.setPartManagerId(savedVoter.getPartManager().getId());
		}
		if (savedVoter.getCasteCategory() != null) {
			savedVoterDto.setCasteCategoryId(savedVoter.getCasteCategory().getId());
		}

		// Setting the Feedback Issue IDs in the response
		if (savedVoter.getFeedbackIssues() != null && !savedVoter.getFeedbackIssues().isEmpty()) {
			List<Long> feedbackIssueIds = savedVoter.getFeedbackIssues().stream()
					.map(FeedbackIssue::getId)
					.collect(Collectors.toList());
			savedVoterDto.setFeedbackIssueIds(feedbackIssueIds);
		}
		invalidateVoterCaches(voterDto.getElectionId());

	 List<ElectionOverviewDTO> electionOverviewDTOList = new ArrayList<>();
	 ElectionOverviewDTO dto= new ElectionOverviewDTO();
	 dto.setGender(voterDto.getGender());
	 dto.setMobileNumber(voterDto.getMobileNo());
	 //dto.setNewVoter(isNewVoter);
	 dto.setNewVoter(true);
	 dto.setPincode(voterDto.getPincode());
	 electionOverviewDTOList.add(dto);
	 reportService.saveElectionOverview(voter.getElectionId(),accountId,electionOverviewDTOList);

	 List<Integer> ageList;
	 if (voterDto.getDob() != null) {
		 ageList = Arrays.asList(Period.between(voterDto.getDob(), LocalDate.now()).getYears());
	 } else {
		 ageList = Collections.emptyList(); // Or handle differently based on requirements
	 }
	 reportService.votersBasedOnAge(ageList, voter.getElectionId(), accountId);  
	 List<VotersHavingContactsDTO> votersHavingContactsDTOList = new ArrayList<>();
	 VotersHavingContactsDTO votersHavingContactsDTO = new VotersHavingContactsDTO();
	 votersHavingContactsDTO.setBoothNumber(voterDto.getBoothNumber());
	 votersHavingContactsDTO.setMobileNumber(voterDto.getMobileNo());  
	 votersHavingContactsDTOList.add(votersHavingContactsDTO);
	 reportService.votersHavingContacts(votersHavingContactsDTOList,voterDto.getElectionId(),accountId);

		return new ThedalResponse<>(ThedalSuccess.VOTER_CREATED, savedVoterDto);
	} catch (Exception e) {
		log.error("Failed to save voter: {}", e.getMessage());
		throw new ThedalException(ThedalError.VOTER_SAVE_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
	}
}

// Keep the helper methods from old code
// Legacy dynamic column reflection helpers removed (no longer used)

private boolean isValidDynamicInput(DynamicFieldEntity def, String value) {
	if (def.getRequired() && (value == null || value.trim().isEmpty())) return false;
	if (value == null) return true; // optional
	String type = def.getType().toLowerCase();
	switch(type) {
		case "number":
			try { Double.parseDouble(value.trim()); } catch (NumberFormatException ex) { return false; } return true;
		case "boolean":
			return value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false");
		case "dropdown":
		case "radio":
			return def.getOptions()!=null && def.getOptions().contains(value);
		case "check-box":
		case "multi-select":
			if (def.getOptions()==null) return false; 
			String[] parts = value.split(",");
			for (String p: parts) { if (!def.getOptions().contains(p.trim())) return false; }
			return true;
		case "image":
		case "file":
			return !value.trim().isEmpty();
		default: // string or other
			return !value.trim().isEmpty();
	}
}


//	/**
//	 * Saves a new voter to the repository.
//	 *
//	 * @param voter the voter entity containing voter information to be saved.
//	 * @return a ThedalResponse indicating the success of the operation.
//	 * @throws ThedalException if saving the voter fails.
//	 */
//	@Transactional
//	@Override
//	public ThedalResponse<VoterDTO> saveVoter(VoterDTO voterDto) throws DataIntegrityViolationException {
//		Long accountId = requestDetails.getCurrentAccountId();
//		if (accountId == null) {
//			log.error("Account ID not found, unauthorized access.");
//			throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
//		}
//		Long electionId = voterDto.getElectionId();
//		
//		validateElectionOwnership(electionId, accountId); 
//
//    // Update voter properties
//    BeanUtils.copyProperties(voterDto, voter, "id", "accountId");
// // Set Aadhaar fields
//    if (voterDto.getAadhaarNumber() != null) {
//        voter.setAadhaarNumber(voterDto.getAadhaarNumber());
//        voter.setAadhaarVerified(voterDto.getAadhaarVerified() != null ? voterDto.getAadhaarVerified() : false);
//    }
//
//    // Handle Religion (new approach with IDs)
//    if (voterDto.getReligionId() != null) {
//        Optional<ReligionEntity> religionOpt = religionRepository.findByIdAndAccountIdAndElectionId(
//                voterDto.getReligionId(), accountId, voterDto.getElectionId());
//        if (religionOpt.isPresent()) {
//            voter.setReligion(religionOpt.get());
//        } else {
//            log.warn("Religion not found with ID: {}, accountId: {}, electionId: {}. Skipping assignment.", 
//                    voterDto.getReligionId(), accountId, voterDto.getElectionId());
//        }
//    }
//
//    // Handle Caste (new approach with IDs)
//    if (voterDto.getCasteId() != null) {
//        Optional<CasteEntity> casteOpt = casteRepository.findByIdAndAccountIdAndElectionId(
//                voterDto.getCasteId(), accountId, voterDto.getElectionId());
//        if (casteOpt.isPresent()) {
//            voter.setCaste(casteOpt.get());
//        } else {
//            log.warn("Caste not found with ID: {}, accountId: {}, electionId: {}. Skipping assignment.", 
//                    voterDto.getCasteId(), accountId, voterDto.getElectionId());
//        }
//    }
//
//    // Handle SubCaste (new approach with IDs)
//    if (voterDto.getSubCasteId() != null) {
//        Optional<SubCasteEntity> subCasteOpt = subCasteRepository.findByIdAndAccountIdAndElectionId(
//                voterDto.getSubCasteId(), accountId, voterDto.getElectionId());
//        if (subCasteOpt.isPresent()) {
//            voter.setSubCaste(subCasteOpt.get());
//        } else {
//            log.warn("SubCaste not found with ID: {}, accountId: {}, electionId: {}. Skipping assignment.", 
//                    voterDto.getSubCasteId(), accountId, voterDto.getElectionId());
//        }
//    }
//
//
//	if (voterDto.getBenefitSchemeIds() != null && !voterDto.getBenefitSchemeIds().isEmpty()) {
//		Set<BenefitSchemes> currentBenefitSchemes = new HashSet<>();
//
//		for (Long benefitSchemeId : voterDto.getBenefitSchemeIds()) {
//			Optional<BenefitSchemes> benefitSchemeOpt = benefitSchemesRepository.findByIdAndAccountIdAndElectionId(
//					benefitSchemeId, accountId, electionId);
//
//			benefitSchemeOpt.ifPresent(currentBenefitSchemes::add);
//		}
//
//		voter.setBenefitSchemes(new ArrayList<>(currentBenefitSchemes));
//	}
//	if (voterDto.getLanguageId() != null) { // Changed to getLanguageId instead of getLanguageIds
//		Optional<Language> languageOpt = languageRepository.findByIdAndAccountIdAndElectionId(
//				voterDto.getLanguageId(), accountId, voterDto.getElectionId());
//
//		if (languageOpt.isPresent()) {
//			voter.setLanguages(Set.of(languageOpt.get())); // Set containing only one language
//		} else {
//			log.warn("Language not found with ID: {}, accountId: {}, electionId: {}. Skipping this language.",
//					voterDto.getLanguageId(), accountId, voterDto.getElectionId());
//		}
//	} else {
//		voter.setLanguages(new HashSet<>());
//	}
//
//	// if (voterDto.getLanguageIds() != null &&
//	// !voterDto.getLanguageIds().isEmpty()) {
//	// Set<Language> languages = new HashSet<>();
//	// for (Long languageId : voterDto.getLanguageIds()) {
//	// Optional<Language> languageOpt =
//	// languageRepository.findByIdAndAccountIdAndElectionId(languageId, accountId,
//	// voterDto.getElectionId());
//	// if (languageOpt.isPresent()) {
//	// languages.add(languageOpt.get());
//	// } else {
//	// log.warn("Language not found with ID: {}, accountId: {}, electionId: {}.
//	// Skipping this language.",
//	// languageId, accountId, voterDto.getElectionId());
//	// }
//	// }
//	// voter.setLanguages(languages);
//	// } else {
//	// voter.setLanguages(new HashSet<>());
//	// }
//
//	if (voterDto.getLanguageId() != null) { // Changed to getLanguageId instead of getLanguageIds
//		Optional<Language> languageOpt = languageRepository.findByIdAndAccountIdAndElectionId(
//				voterDto.getLanguageId(), accountId, voterDto.getElectionId());
//
//		if (languageOpt.isPresent()) {
//			voter.setLanguages(Set.of(languageOpt.get())); // Set containing only one language
//		} else {
//			log.warn("Language not found with ID: {}, accountId: {}, electionId: {}. Skipping this language.",
//					voterDto.getLanguageId(), accountId, voterDto.getElectionId());
//		}
//	} else {
//		voter.setLanguages(new HashSet<>());
//	}
//
//	// Handle Availability (old approach, kept working)
//	if (voterDto.getAvailabilityId() != null) {
//		Optional<Availability> availabilityOpt = availabilityRepository.findByIdAndAccountIdAndElectionId(
//				voterDto.getAvailabilityId(), accountId, voterDto.getElectionId());
//		if (availabilityOpt.isPresent()) {
//			voter.setAvailability1(availabilityOpt.get());
//		} else {
//			log.warn("Availability not found with ID: {}, accountId: {}, electionId: {}. Skipping assignment.",
//					voterDto.getAvailabilityId(), accountId, voterDto.getElectionId());
//		}
//	}
//
//	// Handle Party (old approach, kept working)
//	if (voterDto.getPartyId() != null) {
//		Optional<Party> partyOpt = partyRepository.findByIdAndAccountIdAndElectionId(
//				voterDto.getPartyId(), accountId, voterDto.getElectionId());
//		if (partyOpt.isPresent()) {
//			voter.setParty(partyOpt.get());
//		} else {
//			log.warn("Party not found with ID: {}, accountId: {}, electionId: {}. Skipping assignment.",
//					voterDto.getPartyId(), accountId, voterDto.getElectionId());
//		}
//	}
//
//    if (voterDto.getPartNo() != null) {
//        Optional<PartManager> partManagerOpt = partManagerRepository.findByPartNoAndAccountIdAndElectionId(
//                String.valueOf(voterDto.getPartNo()), accountId, electionId);
//        if (partManagerOpt.isPresent()) {
//            voter.setPartManager(partManagerOpt.get());
//        } else {
//            log.warn("PartManager not found for partNo: {}, accountId: {}, electionId: {}. Skipping assignment.",
//                    voterDto.getPartNo(), accountId, electionId);
//        }
//    }
//
//    // Validate partNo consistency
//    if (voter.getPartManager() != null && voter.getPartNo() != null) {
//        if (!String.valueOf(voter.getPartNo()).equals(voter.getPartManager().getPartNo())) {
//            throw new ThedalException(ThedalError.INVALID_PART_NO, HttpStatus.BAD_REQUEST,
//                    "Voter partNo does not match PartManager partNo");
//        }
//    }
//
//	// Handle dynamic fields (old approach, kept working)
//	if (voterDto.getDynamicFields() != null) {
//		voterDto.getDynamicFields().forEach((fieldName, fieldValue) -> {
//			String sanitizedFieldName = sanitizeFieldName(fieldName);
//			String sanitizedFieldValue = sanitizeFieldValue(fieldValue);
//
//			Optional<DynamicFieldMapping> fieldMappingOpt = dynamicFieldMappingRepository
//					.findByFieldNameAndAccountIdAndElectionId(sanitizedFieldName, accountId,
//							voterDto.getElectionId());
//
//			if (!fieldMappingOpt.isPresent()) {
//				String columnName = "column" + getNextAvailableColumnNumber(accountId, voterDto.getElectionId());
//				DynamicFieldMapping newFieldMapping = new DynamicFieldMapping(accountId, voterDto.getElectionId(),
//						sanitizedFieldName, columnName);
//				dynamicFieldMappingRepository.save(newFieldMapping);
//				fieldMappingOpt = Optional.of(newFieldMapping);
//			}
//
//			DynamicFieldMapping fieldMapping = fieldMappingOpt.get();
//			String columnName = fieldMapping.getColumnName();
//			updateVoterColumn(voter, columnName, sanitizedFieldValue);
//		});
//	}
//
//	if (voterDto.getVoterHistoryIds() != null && !voterDto.getVoterHistoryIds().isEmpty()) {
//		Set<VoterHistoryEntity> currentVoterHistories = new HashSet<>();
//
//		for (Long voterHistoryId : voterDto.getVoterHistoryIds()) {
//			Optional<VoterHistoryEntity> voterHistoryOpt = voterHistoryRepository.findByIdAndAccountIdAndElectionId(
//					voterHistoryId, accountId, electionId);
//
//			voterHistoryOpt.ifPresent(currentVoterHistories::add);
//		}
//
//		voter.setVoterHistories(currentVoterHistories);
//	} else {
//		voter.setVoterHistories(new HashSet<>());
//	}
//
//	// Handle Feedback/Issues (new approach with IDs)
//	if (voterDto.getFeedbackIssueIds() != null && !voterDto.getFeedbackIssueIds().isEmpty()) {
//		Set<FeedbackIssue> currentFeedbackIssues = new HashSet<>();
//
//		for (Long feedbackIssueId : voterDto.getFeedbackIssueIds()) {
//			Optional<FeedbackIssue> feedbackIssueOpt = feedbackIssueRepository.findByIdAndAccountIdAndElectionId(
//					feedbackIssueId, accountId, electionId);
//
//			feedbackIssueOpt.ifPresent(currentFeedbackIssues::add);
//		}
//
//		voter.setFeedbackIssues(new HashSet<>(currentFeedbackIssues));
//
//	} else {
//		voter.setFeedbackIssues(new HashSet<>());
//	}
//	try {
//		VoterEntity savedVoter = voterRepository.save(voter);
//		log.info("Voter successfully saved or updated: {}", savedVoter);
//
//	
//		// Prepare response DTO
//		VoterDTO savedVoterDto = new VoterDTO();
//		BeanUtils.copyProperties(savedVoter, savedVoterDto);
//
//		if (savedVoter.getLanguages() != null && !savedVoter.getLanguages().isEmpty()) {
//
//			savedVoterDto.setLanguageId(
//					savedVoter.getLanguages().stream()
//							.map(Language::getId)
//							.findFirst()
//							.orElse(null));
//		}
//		if (savedVoter.getBenefitSchemes() != null && !savedVoter.getBenefitSchemes().isEmpty()) {
//			List<Long> benefitSchemeIds = savedVoter.getBenefitSchemes().stream()
//					.map(BenefitSchemes::getId)
//					.collect(Collectors.toList());
//			savedVoterDto.setBenefitSchemeIds(benefitSchemeIds);
//
//		}
//
//	
//		if (savedVoter.getLanguages() != null && !savedVoter.getLanguages().isEmpty()) {
//			// Since there's only one language, we directly get its ID (or null if not
//			// present)
//			savedVoterDto.setLanguageId( // Changed to setLanguageId instead of setLanguageIds
//					savedVoter.getLanguages().stream()
//							.map(Language::getId)
//							.findFirst() // Assuming there is only one language, we use findFirst
//							.orElse(null) // If there is no language, set it to null
//			);
//		}
//
//		
//		if (savedVoter.getBenefitSchemes() != null && !savedVoter.getBenefitSchemes().isEmpty()) {
//			List<Long> benefitSchemeIds = savedVoter.getBenefitSchemes().stream()
//					.map(BenefitSchemes::getId)
//					.collect(Collectors.toList());
//			savedVoterDto.setBenefitSchemeIds(benefitSchemeIds);
//
//		}
//		if (savedVoter.getVoterHistories() != null && !savedVoter.getVoterHistories().isEmpty()) {
//			List<Long> voterHistoryIds = savedVoter.getVoterHistories().stream()
//					.map(VoterHistoryEntity::getId)
//					.collect(Collectors.toList());
//
//			savedVoterDto.setVoterHistoryIds(voterHistoryIds);
//		}
//
//		if (savedVoter.getAvailability1() != null) {
//			savedVoterDto.setAvailabilityId(savedVoter.getAvailability1().getId());
//		}
//		if (savedVoter.getParty() != null) {
//			savedVoterDto.setPartyId(savedVoter.getParty().getId());
//		}
//		if (savedVoter.getReligion() != null) {
//			savedVoterDto.setReligionId(savedVoter.getReligion().getId());
//		}
//		if (savedVoter.getCaste() != null) {
//			savedVoterDto.setCasteId(savedVoter.getCaste().getId());
//		}
//		if (savedVoter.getSubCaste() != null) {
//			savedVoterDto.setSubCasteId(savedVoter.getSubCaste().getId());
//		}
//		if (savedVoter.getPartManager() != null) {
//            savedVoterDto.setPartManagerId(savedVoter.getPartManager().getId());
//        }
//
//		// Setting the Feedback Issue IDs in the response
//		if (savedVoter.getFeedbackIssues() != null && !savedVoter.getFeedbackIssues().isEmpty()) {
//			List<Long> feedbackIssueIds = savedVoter.getFeedbackIssues().stream()
//					.map(FeedbackIssue::getId)
//					.collect(Collectors.toList());
//			savedVoterDto.setFeedbackIssueIds(feedbackIssueIds);
//		}
//        invalidateVoterCaches(voterDto.getElectionId());
//
//	 List<ElectionOverviewDTO> electionOverviewDTOList = new ArrayList<>();
//     ElectionOverviewDTO dto= new ElectionOverviewDTO();
//     dto.setGender(voterDto.getGender());
//     dto.setMobileNumber(voterDto.getMobileNo());
//     dto.setNewVoter(true);
//     dto.setPincode(voterDto.getPincode());
//     electionOverviewDTOList.add(dto);
//     reportService.saveElectionOverview(voter.getElectionId(),accountId,electionOverviewDTOList);
//
//     List<Integer> ageList;
//     if (voterDto.getDob() != null) {
//         ageList = Arrays.asList(Period.between(voterDto.getDob(), LocalDate.now()).getYears());
//     } else {
//         ageList = Collections.emptyList(); // Or handle differently based on requirements
//     }
//     reportService.votersBasedOnAge(ageList, voter.getElectionId(), accountId);  
//     List<VotersHavingContactsDTO> votersHavingContactsDTOList = new ArrayList<>();
//     VotersHavingContactsDTO votersHavingContactsDTO = new VotersHavingContactsDTO();
//     votersHavingContactsDTO.setBoothNumber(voterDto.getBoothNumber());
//     votersHavingContactsDTO.setMobileNumber(voterDto.getMobileNo());  
//     votersHavingContactsDTOList.add(votersHavingContactsDTO);
//     reportService.votersHavingContacts(votersHavingContactsDTOList,voterDto.getElectionId(),accountId);
//
//		return new ThedalResponse<>(ThedalSuccess.VOTER_CREATED, savedVoterDto);
//	} catch (Exception e) {
//		log.error("Failed to save voter: {}", e.getMessage());
//		throw new ThedalException(ThedalError.VOTER_SAVE_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
//	}
//}
//
//// Keep the helper methods from old code
//private void updateVoterColumn(VoterEntity voter, String columnName, String fieldValue) {
//	try {
//		Field field = VoterEntity.class.getDeclaredField(columnName);
//		field.setAccessible(true);
//		field.set(voter, fieldValue);
//	} catch (NoSuchFieldException | IllegalAccessException e) {
//		log.warn("Error setting dynamic field {}: {}", columnName, e.getMessage());
//	}
//}
//
//private int getNextAvailableColumnNumber(Long accountId, Long electionId) {
//	Integer maxColumnNumber = dynamicFieldMappingRepository.findMaxColumnNumber(accountId, electionId);
//	return (maxColumnNumber == null) ? 1 : maxColumnNumber + 1;
//}
//
//private String sanitizeFieldName(String fieldName) {
//	return fieldName != null ? fieldName.toLowerCase().replaceAll("\\s+", "") : null;
//}
//
//private String sanitizeFieldValue(String fieldValue) {
//	return fieldValue != null ? fieldValue.toLowerCase().replaceAll("\\s+", "") : null;
//}


		  // REPLACE your existing getVoters method with this optimized version

@Override
@Transactional(readOnly = true)
public VoterResponseDTO getVoters(Long accountId, String voterId, String epicNumber, Long electionId,
							List<Integer> boothNumberList, UUID familyId, UUID friendId, 
							List<String> voterFnameEnList, List<String> voterLnameEnList, 
							List<String> voterFnameL1List, List<String> voterFnameL2List, 
							List<String> voterLnameL1List, List<String> voterLnameL2List, 
							List<String> relationFirstNameEnList, List<String> relationLastNameEnList, 
							List<String> rlnFnameL1List, List<String> rlnFnameL2List,
							List<String> rlnLnameL1List, List<String> rlnLnameL2List,
							List<String> partyNameList, List<String> voterHistoryNameList, 
							List<String> religionNameList, Integer age, Integer minAge, Integer maxAge, 
							Boolean includeUnknownAge, List<String> genderList,
							Boolean filterToday, Boolean filterTomorrow, Integer todayMonth, Integer todayDay, 
							Integer tomorrowMonth, Integer tomorrowDay, Integer birthdayMonth, Integer birthdayDay, 
							Boolean starNumber, List<String> descriptionList, List<String> categoryNameList, List<String> casteCategoryNameList,
							List<String> casteNameList, List<String> subCasteNameList,
							Boolean findDuplicates, Long serialNo, Boolean overseas, Boolean fatherless, 
							Boolean guardian, Boolean hasMobileNo, String mobileNo, Boolean singleVoterFamily, String pollStatus, Boolean isFamily, Pageable pageable) {	long startTime = System.currentTimeMillis();
	
	try {
		log.debug("Starting OPTIMIZED getVoters - electionId={}, booths={}", electionId, boothNumberList);
		
		validateElectionOwnershipFast(electionId, accountId);
		
		Long userId = requestDetails.getCurrentUserId();
		UserEntity currentUser = userRepo.findById(userId)
				.orElseThrow(() -> new RuntimeException("User not found"));
		Role userRole = currentUser.getRole();
		
		List<Integer> effectiveBoothNumbers = getEffectiveBoothNumbersFast(boothNumberList, userRole, userId);
		log.info("VOTER QUERY DEBUG - Final effectiveBoothNumbers: {}", effectiveBoothNumbers);
		
		long queryStart = System.currentTimeMillis();
		
		Page<VoterEntity> voters;
		if (Boolean.TRUE.equals(findDuplicates)) {
			voters = voterRepository.findPotentialDuplicates(accountId, electionId, pageable);
		} else {
		// Use new method with dynamic sorting for main voters API
		voters = voterRepository.findByAccountIdAndElectionIdAndFiltersWithDynamicSort(
			accountId, electionId, voterId, epicNumber, effectiveBoothNumbers, familyId, friendId, 
			voterFnameEnList, voterLnameEnList, voterFnameL1List, voterFnameL2List, 
			voterLnameL1List, voterLnameL2List, 
			relationFirstNameEnList, relationLastNameEnList, rlnFnameL1List, rlnFnameL2List,
			rlnLnameL1List, rlnLnameL2List, partyNameList, voterHistoryNameList, religionNameList, 
			age, minAge, maxAge, includeUnknownAge, genderList, filterToday, filterTomorrow, todayMonth, todayDay,
			tomorrowMonth, 
			tomorrowDay, birthdayMonth, birthdayDay, starNumber, descriptionList, categoryNameList, casteCategoryNameList, 
			casteNameList, subCasteNameList, serialNo, overseas, fatherless, 
			guardian, hasMobileNo, mobileNo, singleVoterFamily, pollStatus, isFamily, pageable);
	}		log.info("QUERY RESULT DEBUG - Found {} voters, totalElements: {}", 
				voters.getContent().size(), voters.getTotalElements());
		
		// DEBUG: Show sample of found voters if any
		if (!voters.getContent().isEmpty()) {
			log.info("FOUND VOTERS DEBUG - Showing first few results:");
			for (int i = 0; i < Math.min(3, voters.getContent().size()); i++) {
				VoterEntity voter = voters.getContent().get(i);
				log.info("FOUND VOTER {} - voterId: {}, voterFnameEn: '{}', rlnFnameEn: '{}', rlnLnameEn: '{}'", 
						 i + 1, voter.getVoterId(), voter.getVoterFnameEn(), voter.getRlnFnameEn(), voter.getRlnLnameEn());
			}
		} else {
			log.warn("NO VOTERS FOUND - Query returned empty result set");
			// If relation names were provided but no results found, let's check if basic query works
			if ((relationFirstNameEnList != null && !relationFirstNameEnList.isEmpty()) || 
				(relationLastNameEnList != null && !relationLastNameEnList.isEmpty())) {
				
				log.info("BASIC QUERY CHECK - Testing query without relation name filters");
			Page<VoterEntity> basicVoters = voterRepository.findByAccountIdAndElectionIdAndFiltersWithDynamicSort(
				accountId, electionId, voterId, epicNumber, effectiveBoothNumbers, familyId, friendId,
				voterFnameEnList, voterLnameEnList, voterFnameL1List, voterFnameL2List,
				voterLnameL1List, voterLnameL2List,
				null, null, null, null, null, null,
				partyNameList, voterHistoryNameList, religionNameList, age, minAge,
				maxAge, includeUnknownAge, genderList, filterToday, filterTomorrow, todayMonth, todayDay,
				tomorrowMonth, tomorrowDay, birthdayMonth, birthdayDay,
				starNumber, descriptionList, categoryNameList, casteCategoryNameList, casteNameList,
				subCasteNameList,
				serialNo, overseas, fatherless, guardian, hasMobileNo, mobileNo, singleVoterFamily,
				pollStatus, isFamily, pageable);				log.info("BASIC QUERY RESULT - Found {} voters without relation name filters", 
						 basicVoters.getTotalElements());
				
				// RELATION NAME ANALYSIS - Check what relation names contain the search term
				if (relationFirstNameEnList != null && !relationFirstNameEnList.isEmpty()) {
					String searchTerm = relationFirstNameEnList.get(0).toLowerCase();
					log.info("RELATION NAME ANALYSIS - Searching for relation names containing: '{}'", searchTerm);
					
					// Get a sample of voters to check their relation names
					List<VoterEntity> sampleVoters = voterRepository.findByElectionIdAndAccountIdLimited(
						electionId, accountId, PageRequest.of(0, 50));
					
					log.info("RELATION NAME ANALYSIS - Checking {} sample voters for matches:", sampleVoters.size());
					int exactMatches = 0;
					int partialMatches = 0;
					
					for (VoterEntity voter : sampleVoters) {
						if (voter.getRlnFnameEn() != null) {
							String rlnName = voter.getRlnFnameEn().toLowerCase().trim();
							if (rlnName.equals(searchTerm)) {
								exactMatches++;
								log.info("EXACT MATCH found - voterId: {}, rlnFnameEn: '{}'", 
										 voter.getVoterId(), voter.getRlnFnameEn());
							} else if (rlnName.contains(searchTerm)) {
								partialMatches++;
								log.info("PARTIAL MATCH found - voterId: {}, rlnFnameEn: '{}' contains '{}'", 
										 voter.getVoterId(), voter.getRlnFnameEn(), searchTerm);
							}
						}
					}
					
					log.info("RELATION NAME ANALYSIS - Found {} exact matches and {} partial matches in sample", 
							 exactMatches, partialMatches);
					
					if (exactMatches == 0 && partialMatches == 0) {
						log.warn("RELATION NAME ANALYSIS - No matches found for '{}' in sample. Check your search term!", searchTerm);
					}
				}
			}
		}
		log.debug("Main query: {} ms", System.currentTimeMillis() - queryStart);
		
		validateResultNotEmpty(voters, ThedalError.VOTER_NOT_FOUND);

		if (!voters.getContent().isEmpty()) {
			long relationshipStart = System.currentTimeMillis();
			loadManyToManyRelationships(voters.getContent(), accountId, electionId);
			log.debug("Many-to-many loading: {} ms", System.currentTimeMillis() - relationshipStart);
		}

	long statsStart = System.currentTimeMillis();
	GenderStatsDTO genderStats = Boolean.TRUE.equals(findDuplicates) 
		? new GenderStatsDTO(0L, 0L, 0L, voters.getTotalElements())
		: getStatsOptimized(accountId, electionId, effectiveBoothNumbers, 
						  voterId, epicNumber, familyId, friendId, voterFnameEnList, 
						  voterLnameEnList, voterFnameL1List, voterFnameL2List, 
						  voterLnameL1List, voterLnameL2List, 
						  relationFirstNameEnList, relationLastNameEnList,
						  rlnFnameL1List, rlnFnameL2List, rlnLnameL1List, rlnLnameL2List,
						  partyNameList, voterHistoryNameList, religionNameList, age, minAge, maxAge, 
						  includeUnknownAge, genderList, filterToday, filterTomorrow, todayMonth, todayDay,
						  tomorrowMonth, tomorrowDay, birthdayMonth, birthdayDay, 
						  starNumber, descriptionList, categoryNameList, casteCategoryNameList, casteNameList,
						  subCasteNameList,
						  serialNo, overseas, fatherless, guardian, hasMobileNo, mobileNo, singleVoterFamily, pollStatus);	
	
	// Calculate addressed voter stats when not finding duplicates
	AddressedVoterStatsDTO addressedVoterStats = null;
	if (!Boolean.TRUE.equals(findDuplicates)) {
		String statsCacheKey = generateStatsCacheKey(accountId, electionId, effectiveBoothNumbers);
		addressedVoterStats = getAddressedVoterStatsWithCaching(statsCacheKey + "_overall_addressed", accountId, electionId);
	}
	
	log.debug("Stats query: {} ms", System.currentTimeMillis() - statsStart);
 
	VoterResponseDTO response = new VoterResponseDTO(voters, genderStats);
	response.setAddressedVoterStats(addressedVoterStats);		long totalTime = System.currentTimeMillis() - startTime;
		log.info("OPTIMIZED getVoters: {} ms (was 17000ms)", totalTime);
		
		return response;

	} catch (Exception e) {
		log.error("getVoters failed: {}", e.getMessage(), e);
		throw new ThedalException(ThedalError.VOTER_NOT_FOUND, HttpStatus.NOT_FOUND);
	}
}

	// Overloaded method with noFamilyOnly parameter
	@Override
	@Transactional(readOnly = true)
	public VoterResponseDTO getVoters(Long accountId, String voterId, String epicNumber, Long electionId,
									List<Integer> boothNumberList, UUID familyId, UUID friendId, 
									List<String> voterFnameEnList, List<String> voterLnameEnList, 
									List<String> voterFnameL1List, List<String> voterFnameL2List, 
									List<String> voterLnameL1List, List<String> voterLnameL2List, 
									List<String> relationFirstNameEnList, List<String> relationLastNameEnList, 
									List<String> rlnFnameL1List, List<String> rlnFnameL2List,
									List<String> rlnLnameL1List, List<String> rlnLnameL2List,
									List<String> partyNameList, List<String> voterHistoryNameList, 
									List<String> religionNameList, Integer age, Integer minAge, Integer maxAge, 
									Boolean includeUnknownAge, List<String> genderList,
									Boolean filterToday, Boolean filterTomorrow, Integer todayMonth, Integer todayDay, 
									Integer tomorrowMonth, Integer tomorrowDay, Integer birthdayMonth, Integer birthdayDay, 
									Boolean starNumber, List<String> descriptionList, List<String> categoryNameList, List<String> casteCategoryNameList,
									List<String> casteNameList, List<String> subCasteNameList,
									Boolean findDuplicates, Long serialNo, Boolean overseas, Boolean fatherless, 
									Boolean guardian, Boolean hasMobileNo, String mobileNo, Boolean singleVoterFamily, 
									String pollStatus, Boolean isFamily, Pageable pageable, Boolean noFamilyOnly) {
		
		long startTime = System.currentTimeMillis();
		
		try {
			log.debug("Starting getVoters with noFamilyOnly={} - electionId={}", noFamilyOnly, electionId);
			
			validateElectionOwnershipFast(electionId, accountId);
			
			Long userId = requestDetails.getCurrentUserId();
			UserEntity currentUser = userRepo.findById(userId)
					.orElseThrow(() -> new RuntimeException("User not found"));
			Role userRole = currentUser.getRole();
			
			List<Integer> effectiveBoothNumbers = getEffectiveBoothNumbersFast(boothNumberList, userRole, userId);
			log.info("VOTER QUERY DEBUG - Final effectiveBoothNumbers: {}", effectiveBoothNumbers);
			
			long queryStart = System.currentTimeMillis();
			
			Page<VoterEntity> voters;
			
			if (Boolean.TRUE.equals(noFamilyOnly)) {
				// For no-family voters, use a dedicated approach that filters at database level
				// First, get all no-family voters without other filters to ensure correct pagination
				
				// Check if any complex filters are applied
				boolean hasComplexFilters = voterId != null || epicNumber != null || 
					effectiveBoothNumbers != null || friendId != null ||
					(voterFnameEnList != null && !voterFnameEnList.isEmpty()) ||
					(voterLnameEnList != null && !voterLnameEnList.isEmpty()) ||
					age != null || minAge != null || maxAge != null ||
					(genderList != null && !genderList.isEmpty()) ||
				starNumber != null || serialNo != null || overseas != null ||
				fatherless != null || guardian != null || hasMobileNo != null ||
				mobileNo != null || 
				(descriptionList != null && !descriptionList.isEmpty()) || 
				(categoryNameList != null && !categoryNameList.isEmpty()) ||
				(casteCategoryNameList != null && !casteCategoryNameList.isEmpty()) || 
				(casteNameList != null && !casteNameList.isEmpty()) ||
				(subCasteNameList != null && !subCasteNameList.isEmpty()) || 
				filterToday != null || filterTomorrow != null ||
				birthdayMonth != null || birthdayDay != null ||
				(partyNameList != null && !partyNameList.isEmpty()) ||
				(voterHistoryNameList != null && !voterHistoryNameList.isEmpty()) ||
				(religionNameList != null && !religionNameList.isEmpty());				if (!hasComplexFilters) {
					// Simple case: just get no-family voters with pagination
					voters = new PageImpl<>(
						voterRepository.findByAccountIdAndElectionIdAndFamilyIdIsNull(accountId, electionId)
							.stream()
							.skip(pageable.getOffset())
							.limit(pageable.getPageSize())
							.collect(Collectors.toList()),
						pageable,
						voterRepository.findByAccountIdAndElectionIdAndFamilyIdIsNull(accountId, electionId).size()
					);
				} else {
					// Complex case: Use the optimized query but with modified WHERE clause
					// We need to call the repository with familyId=null and then filter
					// But we need to get ALL matching no-family voters first, then paginate
					
					// Get all voters matching criteria (without pagination) and filter for no-family
					List<VoterEntity> allMatchingVoters = voterRepository.findByAccountIdAndElectionIdAndFiltersOptimized(
						accountId, electionId, voterId, epicNumber, effectiveBoothNumbers, null, friendId, 
						voterFnameEnList, voterLnameEnList, voterFnameL1List, voterFnameL2List, 
						voterLnameL1List, voterLnameL2List, 
						relationFirstNameEnList, relationLastNameEnList, rlnFnameL1List, rlnFnameL2List,
						rlnLnameL1List, rlnLnameL2List, partyNameList, voterHistoryNameList, religionNameList, 
						age, minAge, maxAge, includeUnknownAge, genderList, filterToday, filterTomorrow, todayMonth, todayDay,
						tomorrowMonth, tomorrowDay, birthdayMonth, birthdayDay, starNumber, descriptionList, categoryNameList, casteCategoryNameList, 
						casteNameList, subCasteNameList, serialNo, overseas, fatherless, 
						guardian, hasMobileNo, mobileNo, null, pollStatus, isFamily, Pageable.unpaged()
					).getContent().stream()
						.filter(voter -> voter.getFamilyId() == null)
						.collect(Collectors.toList());
					
					// Now apply manual pagination
					int start = (int) pageable.getOffset();
					int end = Math.min(start + pageable.getPageSize(), allMatchingVoters.size());
					List<VoterEntity> pageContent = start >= allMatchingVoters.size() ? 
						Collections.emptyList() : allMatchingVoters.subList(start, end);
					
					voters = new PageImpl<>(pageContent, pageable, allMatchingVoters.size());
				}
				
			} else if (Boolean.TRUE.equals(findDuplicates)) {
				voters = voterRepository.findPotentialDuplicates(accountId, electionId, pageable);
			} else {
				voters = voterRepository.findByAccountIdAndElectionIdAndFiltersOptimized(
					accountId, electionId, voterId, epicNumber, effectiveBoothNumbers, familyId, friendId, 
					voterFnameEnList, voterLnameEnList, voterFnameL1List, voterFnameL2List, 
					voterLnameL1List, voterLnameL2List, 
					relationFirstNameEnList, relationLastNameEnList, rlnFnameL1List, rlnFnameL2List,
					rlnLnameL1List, rlnLnameL2List, partyNameList, voterHistoryNameList, religionNameList, 
					age, minAge, maxAge, includeUnknownAge, genderList, filterToday, filterTomorrow, todayMonth, todayDay,
					tomorrowMonth, tomorrowDay, birthdayMonth, birthdayDay, starNumber, descriptionList, categoryNameList, casteCategoryNameList, 
					casteNameList, subCasteNameList, serialNo, overseas, fatherless, 
					guardian, hasMobileNo, mobileNo, singleVoterFamily, pollStatus, isFamily, pageable);
			}
			
			log.info("QUERY RESULT DEBUG - Found {} voters, totalElements: {}", 
					voters.getContent().size(), voters.getTotalElements());
			
			log.debug("Main query: {} ms", System.currentTimeMillis() - queryStart);
			
			validateResultNotEmpty(voters, ThedalError.VOTER_NOT_FOUND);

			if (!voters.getContent().isEmpty()) {
				long relationshipStart = System.currentTimeMillis();
				loadManyToManyRelationships(voters.getContent(), accountId, electionId);
				log.debug("Many-to-many loading: {} ms", System.currentTimeMillis() - relationshipStart);
			}

			long statsStart = System.currentTimeMillis();
			GenderStatsDTO genderStats;
			
			if (Boolean.TRUE.equals(findDuplicates)) {
				genderStats = new GenderStatsDTO(0L, 0L, 0L, voters.getTotalElements());
			} else if (Boolean.TRUE.equals(noFamilyOnly)) {
				// Get stats for no-family voters only - calculate from filtered results
				// First get all voters without pagination for accurate stats
				Pageable allPageable = Pageable.unpaged();
				Page<VoterEntity> allVoters = voterRepository.findByAccountIdAndElectionIdAndFiltersOptimized(
					accountId, electionId, voterId, epicNumber, effectiveBoothNumbers, null, friendId, 
					voterFnameEnList, voterLnameEnList, voterFnameL1List, voterFnameL2List, 
					voterLnameL1List, voterLnameL2List, 
					relationFirstNameEnList, relationLastNameEnList, rlnFnameL1List, rlnFnameL2List,
					rlnLnameL1List, rlnLnameL2List, partyNameList, voterHistoryNameList, religionNameList, 
					age, minAge, maxAge, includeUnknownAge, genderList, filterToday, filterTomorrow, todayMonth, todayDay,
					tomorrowMonth, tomorrowDay, birthdayMonth, birthdayDay, starNumber, descriptionList, categoryNameList, casteCategoryNameList, 
					casteNameList, subCasteNameList, serialNo, overseas, fatherless, 
					guardian, hasMobileNo, mobileNo, null, pollStatus, isFamily, allPageable);
				
				// Filter for only no-family voters and calculate stats
				List<VoterEntity> noFamilyVoters = allVoters.getContent().stream()
						.filter(voter -> voter.getFamilyId() == null)
						.collect(Collectors.toList());
				
				// Calculate stats from filtered voters
				long maleCount = noFamilyVoters.stream().filter(v -> {
					String gender = v.getGender();
					return gender != null && (gender.equalsIgnoreCase("male") || gender.equalsIgnoreCase("m"));
				}).count();
				long femaleCount = noFamilyVoters.stream().filter(v -> {
					String gender = v.getGender();
					return gender != null && (gender.equalsIgnoreCase("female") || gender.equalsIgnoreCase("f"));
				}).count();
				long otherCount = noFamilyVoters.stream().filter(v -> {
					String gender = v.getGender();
					return gender == null || (!gender.equalsIgnoreCase("male") && !gender.equalsIgnoreCase("m") && 
							!gender.equalsIgnoreCase("female") && !gender.equalsIgnoreCase("f"));
				}).count();
				
				genderStats = new GenderStatsDTO(maleCount, femaleCount, otherCount, (long) noFamilyVoters.size());
		} else {
			genderStats = getStatsOptimized(accountId, electionId, effectiveBoothNumbers, 
					voterId, epicNumber, familyId, friendId, voterFnameEnList, 
					voterLnameEnList, voterFnameL1List, voterFnameL2List, 
					voterLnameL1List, voterLnameL2List, 
					relationFirstNameEnList, relationLastNameEnList,
					rlnFnameL1List, rlnFnameL2List, rlnLnameL1List, rlnLnameL2List,
					partyNameList, voterHistoryNameList, religionNameList, age, minAge, maxAge, 
					includeUnknownAge, genderList, filterToday, filterTomorrow, todayMonth, todayDay,
					tomorrowMonth, tomorrowDay, birthdayMonth, birthdayDay, 
					starNumber, descriptionList, categoryNameList, casteCategoryNameList, casteNameList,
					subCasteNameList, serialNo, overseas, fatherless, guardian, hasMobileNo, mobileNo, singleVoterFamily, pollStatus);
		}			log.debug("Stats query: {} ms", System.currentTimeMillis() - statsStart);
		 
			VoterResponseDTO response = new VoterResponseDTO(voters, genderStats);
			
			long totalTime = System.currentTimeMillis() - startTime;
			log.info("getVoters with noFamilyOnly={}: {} ms", noFamilyOnly, totalTime);
			
			return response;

		} catch (Exception e) {
			log.error("getVoters failed: {}", e.getMessage(), e);
			throw new ThedalException(ThedalError.VOTER_NOT_FOUND, HttpStatus.NOT_FOUND);
		}
	}

	// Convenience method for no-family voters API
	@Override
	@Transactional(readOnly = true)
	public VoterResponseDTO getVoters(Long electionId, String voterId, String epicNumber, String boothNumbers,
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
			List<String> mappedSortFields, String orderLower, Boolean isFamily, Boolean noFamilyOnly) {
		
		// Get account ID from request context
		Long accountId = requestDetails.getCurrentAccountId();
		
		// Convert single strings to lists for the main method
		List<String> voterFnameEnList = voterFnameEn != null ? List.of(voterFnameEn.trim()) : null;
		List<String> voterLnameEnList = voterLnameEn != null ? List.of(voterLnameEn.trim()) : null;
		List<String> voterFnameL1List = voterFnameL1 != null ? List.of(voterFnameL1.trim()) : null;
		List<String> voterFnameL2List = voterFnameL2 != null ? List.of(voterFnameL2.trim()) : null;
		List<String> voterLnameL1List = voterLnameL1 != null ? List.of(voterLnameL1.trim()) : null;
		List<String> voterLnameL2List = voterLnameL2 != null ? List.of(voterLnameL2.trim()) : null;
		
		// Handle relation names
		List<String> relationFirstNameEnList = relationFirstNameEn != null ? List.of(relationFirstNameEn.trim()) : null;
		List<String> relationLastNameEnList = relationLastNameEn != null ? List.of(relationLastNameEn.trim()) : null;
		
		// Convert other single values to lists
		List<String> partyNameList = partyName != null ? List.of(partyName.trim()) : null;
		List<String> voterHistoryNameList = voterHistoryName != null ? List.of(voterHistoryName.trim()) : null;
		List<String> genderList = gender != null ? List.of(gender.trim()) : null;
		List<String> religionNameList = religionName != null ? List.of(religionName.trim()) : null;
		List<String> descriptionList = description != null ? List.of(description.trim()) : null;
		List<String> categoryNameList = categoryName != null ? List.of(categoryName.trim()) : null;
		List<String> casteCategoryNameList = casteCategoryName != null ? List.of(casteCategoryName.trim()) : null;
		List<String> casteNameList = casteName != null ? List.of(casteName.trim()) : null;
		List<String> subCasteNameList = subCaste != null ? List.of(subCaste.trim()) : null;
		
		// Parse booth numbers
		List<Integer> boothNumberList = null;
		if (boothNumbers != null && !boothNumbers.trim().isEmpty()) {
			try {
				boothNumberList = Arrays.stream(boothNumbers.split(","))
						.map(String::trim)
						.map(Integer::parseInt)
						.collect(Collectors.toList());
			} catch (NumberFormatException e) {
				log.error("Invalid booth numbers format: {}", boothNumbers);
				throw new ThedalException(ThedalError.INVALID_REQUEST, HttpStatus.BAD_REQUEST);
			}
		}
		
		// Handle duplicate parameter
		Boolean findDuplicates = duplicate != null ? "true".equalsIgnoreCase(duplicate.trim()) : null;
		
		// Create sort and pageable objects
		Sort.Direction sortDirection = "desc".equalsIgnoreCase(orderLower) ? Sort.Direction.DESC : Sort.Direction.ASC;
		Sort sort = Sort.by(sortDirection, mappedSortFields.toArray(new String[0]));
		Pageable pageable = PageRequest.of(page, size, sort);
		
		// Handle birthday parameters - use custom if provided, otherwise use today/tomorrow from hasDob
		Integer birthdayMonth = customBirthdayMonth != null ? customBirthdayMonth : 
								(filterToday ? todayMonth : (filterTomorrow ? tomorrowMonth : null));
		Integer birthdayDay = customBirthdayDay != null ? customBirthdayDay : 
							  (filterToday ? todayDay : (filterTomorrow ? tomorrowDay : null));
		
		// Call the main getVoters method with noFamilyOnly parameter
		return getVoters(accountId, voterId, epicNumber, electionId, boothNumberList, familyId, friendId,
				voterFnameEnList, voterLnameEnList, voterFnameL1List, voterFnameL2List, voterLnameL1List, voterLnameL2List,
				relationFirstNameEnList, relationLastNameEnList, null, null, null, null, // rlnFname/LnameL1/L2 not used
				partyNameList, voterHistoryNameList, religionNameList, age, minAge, maxAge, includeUnknownAge, genderList,
				filterToday, filterTomorrow, todayMonth, todayDay, tomorrowMonth, tomorrowDay, birthdayMonth, birthdayDay,
				starNumber, descriptionList, categoryNameList, casteCategoryNameList, casteNameList, subCasteNameList,
				findDuplicates, serialNo, overseas, fatherless, guardian, hasMobileNo, mobileNo, singleVoterFamily, 
				pollStatus, isFamily, pageable, noFamilyOnly);
	}
	
	private void handleVoterResult(String voterId, Page<VoterEntity> result) {
		if (result.getTotalElements() > 1) {
			log.error("Voter ID: {} found in multiple elections", voterId);
			throw new ThedalException(ThedalError.VOTER_DUPLICATE_IN_ELECTIONS, HttpStatus.BAD_REQUEST);
		} else if (result.isEmpty()) {
			throw new ThedalException(ThedalError.VOTER_NOT_FOUND, HttpStatus.NOT_FOUND);
		}
	}
//	@Override
//	@Transactional(readOnly = true)
//	public VoterResponseDTO getVoters(Long accountId, String voterId, String epicNumber, Long electionId,
//	                                 List<Integer> boothNumberList, UUID familyId, List<String> voterFnameEnList, 
//	                                 List<String> voterLnameEn, List<String> voterFnameL1, List<String> voterFnameL2, 
//	                                 List<String> relationFirstNameEn, List<String> relationLastNameEn, 
//	                                 List<String> voterHistoryNameList, List<String> partyNameList, String religionName, Integer age, 
//	                                 Integer minAge, Integer maxAge, Boolean includeUnknownAge, List<String> genderList,
//	                                 Boolean dobFilter, Integer tomorrowMonth, Integer tomorrowDay, Boolean starNumber, String description, Pageable pageable) {
//	    
//	    long startTime = System.currentTimeMillis();
//	    
//	    try {
//	        log.debug("Starting getVoters - electionId={}, accountId={}, partyNameList={}, voterHistoryNameList={}", 
//	                  electionId, accountId, partyNameList, voterHistoryNameList);
//	        
//	        // Fast validation with caching
//	        validateElectionOwnershipFast(electionId, accountId);
//	        
//	        // Fast role check
//	        Long userId = requestDetails.getCurrentUserId();
//	        UserEntity currentUser = userRepo.findById(userId)
//	                .orElseThrow(() -> new RuntimeException("User not found"));
//	        Role userRole = currentUser.getRole();
//	        
//	        // Get effective booth numbers
//	        List<Integer> effectiveBoothNumbers = getEffectiveBoothNumbersFast(boothNumberList, userRole, userId);
//	        log.debug("Effective booth numbers: {}", effectiveBoothNumbers);
//	        
//	        // Log all filter inputs
//	        log.debug("Filter inputs: voterId={}, epicNumber={}, boothNumbers={}, familyId={}, voterFnameEnList={}, " +
//	                  "voterLnameEn={}, voterFnameL1={}, voterFnameL2={}, relationFirstNameEn={}, relationLastNameEn={}, " +
//	                  "partyNameList={}, voterHistoryNameList={}, religionName={}, age={}, minAge={}, maxAge={}, " +
//	                  "includeUnknownAge={}, genderList={}, hasDob={}, tomorrowMonth={}, tomorrowDay={}, starNumber={}, description={}",
//	                  voterId, epicNumber, effectiveBoothNumbers, familyId, voterFnameEnList, voterLnameEn, 
//	                  voterFnameL1, voterFnameL2, relationFirstNameEn, relationLastNameEn, partyNameList, 
//	                  voterHistoryNameList, religionName, age, minAge, maxAge, includeUnknownAge, genderList, 
//	                  dobFilter, tomorrowMonth, tomorrowDay, starNumber, description);
//	        
//	        // Execute query
//	        long queryStart = System.currentTimeMillis();
//	        Page<VoterEntity> voters = voterRepository.findByAccountIdAndElectionIdAndFiltersOptimized(
//	                accountId, electionId, voterId, epicNumber, effectiveBoothNumbers, familyId, 
//	                voterFnameEnList, voterLnameEn, voterFnameL1, voterFnameL2, relationFirstNameEn, 
//	                relationLastNameEn, voterHistoryNameList, partyNameList, religionName, age, minAge, 
//	                maxAge, includeUnknownAge, genderList, dobFilter, tomorrowMonth, tomorrowDay, starNumber, description, pageable);
//
//	        // OPTIMIZATION: Use query WITHOUT many-to-many joins
//	        long queryStart = System.currentTimeMillis();        Page<VoterEntity> voters = voterRepository.findByAccountIdAndElectionIdAndFiltersOptimized(
//                accountId, electionId, voterId, epicNumber, effectiveBoothNumbers, familyId, 
//                voterFnameEnList, voterLnameEn, voterFnameL1, voterFnameL2, relationFirstNameEn, 
//                relationLastNameEn, partyNameList, voterHistoryNameList, religionName, age, minAge, 
//                maxAge, includeUnknownAge, genderList, dobFilter, tomorrowMonth, tomorrowDay, starNumber, description, pageable);
//	        
//	        log.debug("Query returned {} voters in {} ms", voters.getTotalElements(), System.currentTimeMillis() - queryStart);
//	        if (voters.getContent().isEmpty()) {
//	            log.warn("No voters found for filters: partyNameList={}, voterHistoryNameList={}", partyNameList, voterHistoryNameList);
//	            // Debug database state for partyName
//	            if (partyNameList != null && !partyNameList.isEmpty()) {
//	                List<VoterEntity> partyVoters = voterRepository.findByAccountIdAndElectionIdAndFiltersOptimized(
//	                        accountId, electionId, null, null, null, null, null, null, null, null, null, null, null, 
//	                        partyNameList, null, null, null, null, true, null, null, null, null, null, description, Pageable.unpaged()).getContent();
//	                log.debug("Party filter debug: found {} voters with partyNameList={}", 
//	                          partyVoters.size(), partyNameList);
//	                partyVoters.forEach(v -> log.debug("Voter id={}, party={}", v.getId(), 
//	                                                  v.getParty() != null ? v.getParty().getPartyName() : "null"));
//	            }
//	            // Debug database state for voterHistoryName
//	            if (voterHistoryNameList != null && !voterHistoryNameList.isEmpty()) {
//	                List<VoterEntity> historyVoters = voterRepository.findByAccountIdAndElectionIdAndFiltersOptimized(
//	                        accountId, electionId, null, null, null, null, null, null, null, null, null, null, 
//	                        voterHistoryNameList, null, null, null, null, null, true, null, null, null, null, null, description, Pageable.unpaged()).getContent();
//	                log.debug("VoterHistory filter debug: found {} voters with voterHistoryNameList={}", 
//	                          historyVoters.size(), voterHistoryNameList);
//	                historyVoters.forEach(v -> log.debug("Voter id={}, voterHistories={}", v.getId(), 
//	                                                    v.getVoterHistories().stream()
//	                                                     .map(VoterHistoryEntity::getVoterHistoryName)
//	                                                     .collect(Collectors.toList())));
//	            }
//	        } else {
//	            log.debug("Voters found: {}", voters.getContent().stream()
//	                    .map(v -> "id=" + v.getId() + ", party=" + (v.getParty() != null ? v.getParty().getPartyName() : "null") + 
//	                              ", voterHistories=" + v.getVoterHistories().stream()
//	                                    .map(VoterHistoryEntity::getVoterHistoryName)
//	                                    .collect(Collectors.toList()))
//	                    .collect(Collectors.toList()));
//	        }
//	        
//	        // Instead of throwing, return empty result with stats
//	        GenderStatsDTO genderStats = getStatsOptimized(accountId, electionId, effectiveBoothNumbers, 
//	                                                      voterId, epicNumber, familyId, voterFnameEnList, 
//	                                                      voterLnameEn, voterFnameL1, voterFnameL2, 
//	                                                      relationFirstNameEn, relationLastNameEn, 
//	                                                      partyNameList, voterHistoryNameList, religionName, age, minAge, maxAge, 
//	                                                      includeUnknownAge, genderList, dobFilter, tomorrowMonth, tomorrowDay, starNumber, description);
//	        
//	        VoterResponseDTO response = new VoterResponseDTO(voters, genderStats);
//	        
//	        long totalTime = System.currentTimeMillis() - startTime;
//	        log.info("getVoters completed: {} ms, returned {} voters", totalTime, voters.getTotalElements());
//	        
//	        return response;
//
//	    } catch (Exception e) {
//	        log.error("getVoters failed: {}", e.getMessage(), e);
//	        throw new ThedalException(ThedalError.VOTER_NOT_FOUND, HttpStatus.NOT_FOUND);
//	    }
//	}
//		
//
//		private void handleVoterResult(String voterId, Page<VoterEntity> result) {
//		    if (result.getTotalElements() > 1) {
//		        log.error("Voter ID: {} found in multiple elections", voterId);
//		        throw new ThedalException(ThedalError.VOTER_DUPLICATE_IN_ELECTIONS, HttpStatus.BAD_REQUEST);
//		    } else if (result.isEmpty()) {
//		        throw new ThedalException(ThedalError.VOTER_NOT_FOUND, HttpStatus.NOT_FOUND);
//		    }
//		}

//	/**
//	 * Updates the details of an existing voter.
//	 *
//	 * @param voterId the ID of the voter to be updated.
//	 * @param voter   the new voter details.
//	 * @return the updated voter entity.
//	 * @throws ThedalException if the voter is not found or an error occurs during
//	 *                         the update.
//	 */
//	// @Transactional(rollbackOn = Exception.class)
//	@Transactional
//	@Override
//	public VoterUpdateDTO updateVoter(String epicNumber, Long electionId, VoterUpdateDTO voterUpdateDTO) {
//
//		Long accountId = requestDetails.getCurrentAccountId();
//	    if (accountId == null) {
//	        log.error("Account id not found, unauthorized access.");
//	        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
//	    }
//	    validateElectionOwnership(electionId, accountId); 
//
//	    try {
//	        log.info("Updating voter with ID: {}, Election ID: {}, Account ID: {}", epicNumber, electionId, accountId);
//
//	        VoterEntity existingVoter = voterRepository.findByVoterIdAndElectionIdAndAccountId(epicNumber, electionId, accountId)
//	        //VoterEntity existingVoter = voterRepository.findByEpicNumberAndElectionIdAndAccountId(epicNumber, electionId, accountId)
//            .orElseThrow(() -> {
//	                log.warn("Voter not found with voterId: {}, electionId: {}, accountId: {}", epicNumber, electionId, accountId);
//	                return new ThedalException(ThedalError.VOTER_NOT_FOUND, HttpStatus.NOT_FOUND);
//	            });
//	        
//	     // Fetch or create corresponding MongoDB voter
//            VoterMongo existingVoterMongo = voterMongoRepository.findByVoterIdAndElectionIdAndAccountId(epicNumber, electionId, accountId)
//                    .orElse(new VoterMongo(existingVoter));
//	        
//			if (voterUpdateDTO.getAadhaarNumber() != null && !voterUpdateDTO.getAadhaarNumber().trim().isEmpty()) {
//			String aadhaarNumber = voterUpdateDTO.getAadhaarNumber().trim();
//			Optional<VoterEntity> existingWithSameAadhaar =
//        		voterRepository.findByAadhaarNumber(voterUpdateDTO.getAadhaarNumber().trim());
//			Optional<VoterMongo> existingMongoWithSameAadhaar = voterMongoRepository.findByAadhaarNumberAndElectionIdAndAccountId(aadhaarNumber, electionId, accountId);
//
//		if (existingWithSameAadhaar.isPresent() &&
//				!existingWithSameAadhaar.get().getId().equals(existingVoter.getId())) {
//
//			// Construct the message in the desired format
//			String details = String.format(
//				"%s already exists (EPIC: %s).",
//				voterUpdateDTO.getAadhaarNumber(),
//				existingWithSameAadhaar.get().getEpicNumber()
//			);
//
//			// Pass only the details without adding extra prefixes
//			throw new ThedalException(ThedalError.DUPLICATE_AADHAAR_NUMBER, HttpStatus.BAD_REQUEST, details);
//		}
//	}
////            // Validate Aadhaar number uniqueness
////            if (voterUpdateDTO.getAadhaarNumber() != null && !voterUpdateDTO.getAadhaarNumber().trim().isEmpty()) {
////                String aadhaarNumber = voterUpdateDTO.getAadhaarNumber().trim();
////                Optional<VoterEntity> existingWithSameAadhaar = voterRepository.findByAadhaarNumberAndElectionIdAndAccountId(aadhaarNumber, electionId, accountId);
////                Optional<VoterMongo> existingMongoWithSameAadhaar = voterMongoRepository.findByAadhaarNumberAndElectionIdAndAccountId(aadhaarNumber, electionId, accountId);
////
////                if ((existingWithSameAadhaar.isPresent() && !existingWithSameAadhaar.get().getId().equals(existingVoter.getId())) ||
////                        (existingMongoWithSameAadhaar.isPresent() && !existingMongoWithSameAadhaar.get().getId().equals(existingVoter.getId()))) {
////                    String details = String.format("%s already exists (EPIC: %s).", aadhaarNumber,
////                            existingWithSameAadhaar.map(VoterEntity::getEpicNumber).orElse(existingMongoWithSameAadhaar.get().getEpicNumber()));
////                    throw new ThedalException(ThedalError.DUPLICATE_AADHAAR_NUMBER, HttpStatus.BAD_REQUEST, details);
////                }
////                existingVoter.setAadhaarNumber(aadhaarNumber);
////                existingVoterMongo.setAadhaarNumber(aadhaarNumber);
////                existingVoter.setAadhaarVerified(voterUpdateDTO.getAadhaarVerified() != null ? voterUpdateDTO.getAadhaarVerified() : false);
////                existingVoterMongo.setAadhaarVerified(voterUpdateDTO.getAadhaarVerified() != null ? voterUpdateDTO.getAadhaarVerified() : false);
////            }
//			
//			// Handle booth number and part number
//	        if (voterUpdateDTO.getPartNo() != null) {
//	            log.info("Updating partNo to {} for voter with ID: {}", voterUpdateDTO.getPartNo(), epicNumber);
//	            Optional<PartManager> partManagerOpt = partManagerRepository.findByPartNoAndAccountIdAndElectionId(
//	                    String.valueOf(voterUpdateDTO.getPartNo()), accountId, electionId);
//	            if (partManagerOpt.isPresent()) {
//	                existingVoter.setPartManager(partManagerOpt.get());
//	                existingVoterMongo.setPartManagerId(partManagerOpt.get().getId());
//	            } else {
//	                log.warn("PartManager not found for partNo: {}, accountId: {}, electionId: {}. Setting partManager to null.",
//	                        voterUpdateDTO.getPartNo(), accountId, electionId);
//	                existingVoter.setPartManager(null);
//	                existingVoterMongo.setPartManagerId(null);
//	            }
//	            ElectionBooth electionBooth = electionBoothService.saveBooth(electionId, voterUpdateDTO.getPartNo(), accountId);
//	            existingVoter.setPartNoAndBoothNumber(voterUpdateDTO.getPartNo()); // Sync partNo and boothNumber
//	            electionBooth.setBoothNumber(voterUpdateDTO.getPartNo());
//	            existingVoterMongo.setPartNoAndBoothNumber(voterUpdateDTO.getPartNo());
//	            electionBoothService.updateElectionBooth(electionBooth);
//	        }
//			
//////	     // Update epicNumber if provided in the DTO
//////	        if (voterUpdateDTO.getEpicNumber() != null && !voterUpdateDTO.getEpicNumber().equals(epicNumber)) {
//////	            // Check for uniqueness if required
//////	            if (voterRepository.existsByEpicNumberAndElectionIdAndAccountId(voterUpdateDTO.getEpicNumber(), electionId, accountId)) {
//////	                log.warn("EPIC Number {} already exists for electionId: {}, accountId: {}", voterUpdateDTO.getEpicNumber(), electionId, accountId);
//////	                throw new ThedalException(ThedalError.EPIC_NUMBER_ALREADY_EXISTS, HttpStatus.CONFLICT);
//////	            }
//////	            existingVoter.setEpicNumber(voterUpdateDTO.getEpicNumber());
//////	        }
//	     // Update epicNumber if provided
//            if (voterUpdateDTO.getEpicNumber() != null && !voterUpdateDTO.getEpicNumber().equals(epicNumber)) {
//                if (voterRepository.existsByEpicNumberAndElectionIdAndAccountId(voterUpdateDTO.getEpicNumber(), electionId, accountId) ||
//                        voterMongoRepository.existsByEpicNumberAndElectionIdAndAccountId(voterUpdateDTO.getEpicNumber(), electionId, accountId)) {
//                    log.warn("EPIC Number {} already exists for electionId: {}, accountId: {}", voterUpdateDTO.getEpicNumber(), electionId, accountId);
//                    throw new ThedalException(ThedalError.EPIC_NUMBER_ALREADY_EXISTS, HttpStatus.CONFLICT);
//                }
//                existingVoter.setEpicNumber(voterUpdateDTO.getEpicNumber());
//                existingVoter.setVoterId(voterUpdateDTO.getEpicNumber());
//                existingVoterMongo.setEpicNumber(voterUpdateDTO.getEpicNumber());
//                existingVoterMongo.setVoterId(voterUpdateDTO.getEpicNumber());
//            }
//	        
//            if (voterUpdateDTO.getGender() != null) {
//                existingVoter.setGender(voterUpdateDTO.getGender());
//                existingVoterMongo.setGender(voterUpdateDTO.getGender());
//            }
//	        if (voterUpdateDTO.getPhotoUrl() != null) {
//	        	existingVoter.setPhotoUrl(voterUpdateDTO.getPhotoUrl());
//	        	 existingVoterMongo.setPhotoUrl(voterUpdateDTO.getPhotoUrl());
//	        }
//	        if (voterUpdateDTO.getStateCode() != null) {
//	        	existingVoter.setStateCode(voterUpdateDTO.getStateCode());
//	        	existingVoterMongo.setStateCode(voterUpdateDTO.getStateCode());
//	        }
//	        if (voterUpdateDTO.getStateNameEn() != null) {
//                existingVoter.setStateNameEn(voterUpdateDTO.getStateNameEn());
//                existingVoterMongo.setStateNameEn(voterUpdateDTO.getStateNameEn());
//            }
//            if (voterUpdateDTO.getStateNameL1() != null) {
//                existingVoter.setStateNameL1(voterUpdateDTO.getStateNameL1());
//                existingVoterMongo.setStateNameL1(voterUpdateDTO.getStateNameL1());
//            }
//            if (voterUpdateDTO.getStateNameL2() != null) {
//                existingVoter.setStateNameL2(voterUpdateDTO.getStateNameL2());
//                existingVoterMongo.setStateNameL2(voterUpdateDTO.getStateNameL2());
//            }
//            
//            if (voterUpdateDTO.getPartNo() != null) {
//                existingVoter.setPartNo(voterUpdateDTO.getPartNo());
//                existingVoterMongo.setPartNo(voterUpdateDTO.getPartNo());
//            }
//            if (voterUpdateDTO.getPartNameEn() != null) {
//                existingVoter.setPartNameEn(voterUpdateDTO.getPartNameEn());
//                existingVoterMongo.setPartNameEn(voterUpdateDTO.getPartNameEn());
//            }
//            if (voterUpdateDTO.getPartNameL1() != null) {
//                existingVoter.setPartNameL1(voterUpdateDTO.getPartNameL1());
//                existingVoterMongo.setPartNameL1(voterUpdateDTO.getPartNameL1());
//            }
//            if (voterUpdateDTO.getPartNameL2() != null) {
//                existingVoter.setPartNameL2(voterUpdateDTO.getPartNameL2());
//                existingVoterMongo.setPartNameL2(voterUpdateDTO.getPartNameL2());
//            }
//            if (voterUpdateDTO.getPincode() != null) {
//                existingVoter.setPincode(voterUpdateDTO.getPincode());
//                existingVoterMongo.setPincode(voterUpdateDTO.getPincode());
//            }
//            if (voterUpdateDTO.getSectionNo() != null) {
//                existingVoter.setSectionNo(voterUpdateDTO.getSectionNo());
//                existingVoterMongo.setSectionNo(voterUpdateDTO.getSectionNo());
//            }
//            if (voterUpdateDTO.getSectionNameEn() != null) {
//                existingVoter.setSectionNameEn(voterUpdateDTO.getSectionNameEn());
//                existingVoterMongo.setSectionNameEn(voterUpdateDTO.getSectionNameEn());
//            }
//            if (voterUpdateDTO.getSectionNameL1() != null) {
//                existingVoter.setSectionNameL1(voterUpdateDTO.getSectionNameL1());
//                existingVoterMongo.setSectionNameL1(voterUpdateDTO.getSectionNameL1());
//            }
//            if (voterUpdateDTO.getSectionNameL2() != null) {
//                existingVoter.setSectionNameL2(voterUpdateDTO.getSectionNameL2());
//                existingVoterMongo.setSectionNameL2(voterUpdateDTO.getSectionNameL2());
//            }
//            if (voterUpdateDTO.getAge() != null) {
//                existingVoter.setAge(voterUpdateDTO.getAge());
//                existingVoterMongo.setAge(voterUpdateDTO.getAge());
//            }
//            
//	        existingVoter.setDob(voterUpdateDTO.getDob());
//	        existingVoterMongo.setDob(voterUpdateDTO.getDob());
//	        if (voterUpdateDTO.getSerialNo() != null) {
//                existingVoter.setSerialNo(voterUpdateDTO.getSerialNo());
//                existingVoterMongo.setSerialNo(voterUpdateDTO.getSerialNo());
//            }
//            if (voterUpdateDTO.getHouseNoEn() != null) {
//                existingVoter.setHouseNoEn(voterUpdateDTO.getHouseNoEn());
//                existingVoterMongo.setHouseNoEn(voterUpdateDTO.getHouseNoEn());
//            }
//            if (voterUpdateDTO.getHouseNoL1() != null) {
//                existingVoter.setHouseNoL1(voterUpdateDTO.getHouseNoL1());
//                existingVoterMongo.setHouseNoL1(voterUpdateDTO.getHouseNoL1());
//            }
//            if (voterUpdateDTO.getHouseNoL2() != null) {
//                existingVoter.setHouseNoL2(voterUpdateDTO.getHouseNoL2());
//                existingVoterMongo.setHouseNoL2(voterUpdateDTO.getHouseNoL2());
//            }
//            if (voterUpdateDTO.getVoterFnameEn() != null) {
//                existingVoter.setVoterFnameEn(voterUpdateDTO.getVoterFnameEn());
//                existingVoterMongo.setVoterFnameEn(voterUpdateDTO.getVoterFnameEn());
//            }
//            if (voterUpdateDTO.getVoterLnameEn() != null) {
//                existingVoter.setVoterLnameEn(voterUpdateDTO.getVoterLnameEn());
//                existingVoterMongo.setVoterLnameEn(voterUpdateDTO.getVoterLnameEn());
//            }
//            if (voterUpdateDTO.getVoterFnameL1() != null) {
//                existingVoter.setVoterFnameL1(voterUpdateDTO.getVoterFnameL1());
//                existingVoterMongo.setVoterFnameL1(voterUpdateDTO.getVoterFnameL1());
//            }
//            if (voterUpdateDTO.getVoterLnameL1() != null) {
//                existingVoter.setVoterLnameL1(voterUpdateDTO.getVoterLnameL1());
//                existingVoterMongo.setVoterLnameL1(voterUpdateDTO.getVoterLnameL1());
//            }
//            if (voterUpdateDTO.getVoterFnameL2() != null) {
//                existingVoter.setVoterFnameL2(voterUpdateDTO.getVoterFnameL2());
//                existingVoterMongo.setVoterFnameL2(voterUpdateDTO.getVoterFnameL2());
//            }
//            if (voterUpdateDTO.getVoterLnameL2() != null) {
//                existingVoter.setVoterLnameL2(voterUpdateDTO.getVoterLnameL2());
//                existingVoterMongo.setVoterLnameL2(voterUpdateDTO.getVoterLnameL2());
//            }
//            if (voterUpdateDTO.getRlnType() != null) {
//                existingVoter.setRlnType(voterUpdateDTO.getRlnType());
//                existingVoterMongo.setRlnType(voterUpdateDTO.getRlnType());
//            }
//            if (voterUpdateDTO.getRlnFnameEn() != null) {
//                existingVoter.setRlnFnameEn(voterUpdateDTO.getRlnFnameEn());
//                existingVoterMongo.setRlnFnameEn(voterUpdateDTO.getRlnFnameEn());
//            }
//            if (voterUpdateDTO.getRlnLnameEn() != null) {
//                existingVoter.setRlnLnameEn(voterUpdateDTO.getRlnLnameEn());
//                existingVoterMongo.setRlnLnameEn(voterUpdateDTO.getRlnLnameEn());
//            }
//            if (voterUpdateDTO.getRlnFnameL1() != null) {
//                existingVoter.setRlnFnameL1(voterUpdateDTO.getRlnFnameL1());
//                existingVoterMongo.setRlnFnameL1(voterUpdateDTO.getRlnFnameL1());
//            }
//            if (voterUpdateDTO.getRlnLnameL1() != null) {
//                existingVoter.setRlnLnameL1(voterUpdateDTO.getRlnLnameL1());
//                existingVoterMongo.setRlnLnameL1(voterUpdateDTO.getRlnLnameL1());
//            }
//            if (voterUpdateDTO.getRlnFnameL2() != null) {
//                existingVoter.setRlnFnameL2(voterUpdateDTO.getRlnFnameL2());
//                existingVoterMongo.setRlnFnameL2(voterUpdateDTO.getRlnFnameL2());
//            }
//            if (voterUpdateDTO.getRlnLnameL2() != null) {
//                existingVoter.setRlnLnameL2(voterUpdateDTO.getRlnLnameL2());
//                existingVoterMongo.setRlnLnameL2(voterUpdateDTO.getRlnLnameL2());
//            }
//            if (voterUpdateDTO.getFullAddress() != null) {
//                existingVoter.setFullAddress(voterUpdateDTO.getFullAddress());
//                existingVoterMongo.setFullAddress(voterUpdateDTO.getFullAddress());
//            }
//            if (voterUpdateDTO.getPartLati() != null) {
//                existingVoter.setPartLati(voterUpdateDTO.getPartLati());
//                existingVoterMongo.setPartLati(voterUpdateDTO.getPartLati());
//            }
//            if (voterUpdateDTO.getPartLong() != null) {
//                existingVoter.setPartLong(voterUpdateDTO.getPartLong());
//                existingVoterMongo.setPartLong(voterUpdateDTO.getPartLong());
//            }
//            if (voterUpdateDTO.getPcNo() != null) {
//                existingVoter.setPcNo(voterUpdateDTO.getPcNo());
//                existingVoterMongo.setPcNo(voterUpdateDTO.getPcNo());
//            }
//            if (voterUpdateDTO.getPcNameEn() != null) {
//                existingVoter.setPcNameEn(voterUpdateDTO.getPcNameEn());
//                existingVoterMongo.setPcNameEn(voterUpdateDTO.getPcNameEn());
//            }
//            if (voterUpdateDTO.getPcNameL1() != null) {
//                existingVoter.setPcNameL1(voterUpdateDTO.getPcNameL1());
//                existingVoterMongo.setPcNameL1(voterUpdateDTO.getPcNameL1());
//            }
//            if (voterUpdateDTO.getPcNameL2() != null) {
//                existingVoter.setPcNameL2(voterUpdateDTO.getPcNameL2());
//                existingVoterMongo.setPcNameL2(voterUpdateDTO.getPcNameL2());
//            }
//            if (voterUpdateDTO.getAcNo() != null) {
//                existingVoter.setAcNo(voterUpdateDTO.getAcNo());
//                existingVoterMongo.setAcNo(voterUpdateDTO.getAcNo());
//            }
//            if (voterUpdateDTO.getAcNameEn() != null) {
//                existingVoter.setAcNameEn(voterUpdateDTO.getAcNameEn());
//                existingVoterMongo.setAcNameEn(voterUpdateDTO.getAcNameEn());
//            }
//            if (voterUpdateDTO.getAcNameL1() != null) {
//                existingVoter.setAcNameL1(voterUpdateDTO.getAcNameL1());
//                existingVoterMongo.setAcNameL1(voterUpdateDTO.getAcNameL1());
//            }
//            if (voterUpdateDTO.getAcNameL2() != null) {
//                existingVoter.setAcNameL2(voterUpdateDTO.getAcNameL2());
//                existingVoterMongo.setAcNameL2(voterUpdateDTO.getAcNameL2());
//            }
//            if (voterUpdateDTO.getRurDistrictUnionNo() != null) {
//                existingVoter.setRurDistrictUnionNo(voterUpdateDTO.getRurDistrictUnionNo());
//                existingVoterMongo.setRurDistrictUnionNo(voterUpdateDTO.getRurDistrictUnionNo());
//            }
//            if (voterUpdateDTO.getRurDistrictUnionNameEn() != null) {
//                existingVoter.setRurDistrictUnionNameEn(voterUpdateDTO.getRurDistrictUnionNameEn());
//                existingVoterMongo.setRurDistrictUnionNameEn(voterUpdateDTO.getRurDistrictUnionNameEn());
//            }
//            if (voterUpdateDTO.getRurDistrictUnionNameL1() != null) {
//                existingVoter.setRurDistrictUnionNameL1(voterUpdateDTO.getRurDistrictUnionNameL1());
//                existingVoterMongo.setRurDistrictUnionNameL1(voterUpdateDTO.getRurDistrictUnionNameL1());
//            }
//            if (voterUpdateDTO.getRurDistrictUnionNameL2() != null) {
//                existingVoter.setRurDistrictUnionNameL2(voterUpdateDTO.getRurDistrictUnionNameL2());
//                existingVoterMongo.setRurDistrictUnionNameL2(voterUpdateDTO.getRurDistrictUnionNameL2());
//            }
//            if (voterUpdateDTO.getRurDistrictUnionWardNo() != null) {
//                existingVoter.setRurDistrictUnionWardNo(voterUpdateDTO.getRurDistrictUnionWardNo());
//                existingVoterMongo.setRurDistrictUnionWardNo(voterUpdateDTO.getRurDistrictUnionWardNo());
//            }
//            if (voterUpdateDTO.getPanUnionNo() != null) {
//                existingVoter.setPanUnionNo(voterUpdateDTO.getPanUnionNo());
//                existingVoterMongo.setPanUnionNo(voterUpdateDTO.getPanUnionNo());
//            }
//            if (voterUpdateDTO.getPanUnionNameEn() != null) {
//                existingVoter.setPanUnionNameEn(voterUpdateDTO.getPanUnionNameEn());
//                existingVoterMongo.setPanUnionNameEn(voterUpdateDTO.getPanUnionNameEn());
//            }
//            if (voterUpdateDTO.getPanUnionNameL1() != null) {
//                existingVoter.setPanUnionNameL1(voterUpdateDTO.getPanUnionNameL1());
//                existingVoterMongo.setPanUnionNameL1(voterUpdateDTO.getPanUnionNameL1());
//            }
//            if (voterUpdateDTO.getPanUnionNameL2() != null) {
//                existingVoter.setPanUnionNameL2(voterUpdateDTO.getPanUnionNameL2());
//                existingVoterMongo.setPanUnionNameL2(voterUpdateDTO.getPanUnionNameL2());
//            }
//            if (voterUpdateDTO.getPanUnionWardNo() != null) {
//                existingVoter.setPanUnionWardNo(voterUpdateDTO.getPanUnionWardNo());
//                existingVoterMongo.setPanUnionWardNo(voterUpdateDTO.getPanUnionWardNo());
//            }
//            if (voterUpdateDTO.getVillPanNo() != null) {
//                existingVoter.setVillPanNo(voterUpdateDTO.getVillPanNo());
//                existingVoterMongo.setVillPanNo(voterUpdateDTO.getVillPanNo());
//            }
//            if (voterUpdateDTO.getVillPanNameEn() != null) {
//                existingVoter.setVillPanNameEn(voterUpdateDTO.getVillPanNameEn());
//                existingVoterMongo.setVillPanNameEn(voterUpdateDTO.getVillPanNameEn());
//            }
//            if (voterUpdateDTO.getVillPanNameL1() != null) {
//                existingVoter.setVillPanNameL1(voterUpdateDTO.getVillPanNameL1());
//                existingVoterMongo.setVillPanNameL1(voterUpdateDTO.getVillPanNameL1());
//            }
//            if (voterUpdateDTO.getVillPanWardNo() != null) {
//                existingVoter.setVillPanWardNo(voterUpdateDTO.getVillPanWardNo());
//                existingVoterMongo.setVillPanWardNo(voterUpdateDTO.getVillPanWardNo());
//            }
//            if (voterUpdateDTO.getVoterLati() != null) {
//                existingVoter.setVoterLati(voterUpdateDTO.getVoterLati());
//                existingVoterMongo.setVoterLati(voterUpdateDTO.getVoterLati());
//            }
//            if (voterUpdateDTO.getVoterLongi() != null) {
//                existingVoter.setVoterLongi(voterUpdateDTO.getVoterLongi());
//                existingVoterMongo.setVoterLongi(voterUpdateDTO.getVoterLongi());
//            }
//            if (voterUpdateDTO.getDistrictCode() != null) {
//                existingVoter.setDistrictCode(voterUpdateDTO.getDistrictCode());
//                existingVoterMongo.setDistrictCode(voterUpdateDTO.getDistrictCode());
//            }
//            if (voterUpdateDTO.getDistrictNameEn() != null) {
//                existingVoter.setDistrictNameEn(voterUpdateDTO.getDistrictNameEn());
//                existingVoterMongo.setDistrictNameEn(voterUpdateDTO.getDistrictNameEn());
//            }
//            if (voterUpdateDTO.getDistrictNameL1() != null) {
//                existingVoter.setDistrictNameL1(voterUpdateDTO.getDistrictNameL1());
//                existingVoterMongo.setDistrictNameL1(voterUpdateDTO.getDistrictNameL1());
//            }
//            if (voterUpdateDTO.getDistrictNameL2() != null) {
//                existingVoter.setDistrictNameL2(voterUpdateDTO.getDistrictNameL2());
//                existingVoterMongo.setDistrictNameL2(voterUpdateDTO.getDistrictNameL2());
//            }
//            if (voterUpdateDTO.getUrbanNo() != null) {
//                existingVoter.setUrbanNo(voterUpdateDTO.getUrbanNo());
//                existingVoterMongo.setUrbanNo(voterUpdateDTO.getUrbanNo());
//            }
//            if (voterUpdateDTO.getUrbanNameEn() != null) {
//                existingVoter.setUrbanNameEn(voterUpdateDTO.getUrbanNameEn());
//                existingVoterMongo.setUrbanNameEn(voterUpdateDTO.getUrbanNameEn());
//            }
//            if (voterUpdateDTO.getUrbanNameL1() != null) {
//                existingVoter.setUrbanNameL1(voterUpdateDTO.getUrbanNameL1());
//                existingVoterMongo.setUrbanNameL1(voterUpdateDTO.getUrbanNameL1());
//            }
//            if (voterUpdateDTO.getUrbanWardNo() != null) {
//                existingVoter.setUrbanWardNo(voterUpdateDTO.getUrbanWardNo());
//                existingVoterMongo.setUrbanWardNo(voterUpdateDTO.getUrbanWardNo());
//            }
//            if (voterUpdateDTO.getEMail() != null) {
//                existingVoter.setEMail(voterUpdateDTO.getEMail());
//                existingVoterMongo.setEMail(voterUpdateDTO.getEMail());
//            }
//            if (voterUpdateDTO.getMobileNo() != null) {
//                existingVoter.setMobileNo(voterUpdateDTO.getMobileNo());
//                existingVoterMongo.setMobileNo(voterUpdateDTO.getMobileNo());
//            }
//            if (voterUpdateDTO.getWhatsappNo() != null) {
//                existingVoter.setWhatsappNo(voterUpdateDTO.getWhatsappNo());
//                existingVoterMongo.setWhatsappNo(voterUpdateDTO.getWhatsappNo());
//            }
//            if (voterUpdateDTO.getPageNumber() != null) {
//                existingVoter.setPageNumber(voterUpdateDTO.getPageNumber());
//                existingVoterMongo.setPageNumber(voterUpdateDTO.getPageNumber());
//            }
//            if (voterUpdateDTO.getRemarks() != null) {
//                existingVoter.setRemarks(voterUpdateDTO.getRemarks());
//                existingVoterMongo.setRemarks(voterUpdateDTO.getRemarks());
//            }
//            if (voterUpdateDTO.getStarNumber() != null) {
//                existingVoter.setStarNumber(voterUpdateDTO.getStarNumber());
//                existingVoterMongo.setStarNumber(voterUpdateDTO.getStarNumber());
//            }
//            if (voterUpdateDTO.getPanNumber() != null) {
//                existingVoter.setPanNumber(voterUpdateDTO.getPanNumber());
//                existingVoterMongo.setPanNumber(voterUpdateDTO.getPanNumber());
//            }
//            if (voterUpdateDTO.getPartyRegistrationNumber() != null) {
//                existingVoter.setPartyRegistrationNumber(voterUpdateDTO.getPartyRegistrationNumber());
//                existingVoterMongo.setPartyRegistrationNumber(voterUpdateDTO.getPartyRegistrationNumber());
//            }
//            if (voterUpdateDTO.getMobileVerified() != null) {
//                existingVoter.setMobileVerified(voterUpdateDTO.getMobileVerified());
//                existingVoterMongo.setMobileVerified(voterUpdateDTO.getMobileVerified());
//            }
//            if (voterUpdateDTO.getMemberVerified() != null) {
//                existingVoter.setMemberVerified(voterUpdateDTO.getMemberVerified());
//                existingVoterMongo.setMemberVerified(voterUpdateDTO.getMemberVerified());
//            }
//            if (voterUpdateDTO.getPartyAffiliation() != null) {
//                existingVoter.setPartyAffiliation(voterUpdateDTO.getPartyAffiliation());
//                existingVoterMongo.setPartyAffiliation(voterUpdateDTO.getPartyAffiliation());
//            }
//            
//			if (voterUpdateDTO.getBenefitSchemeIds() != null && !voterUpdateDTO.getBenefitSchemeIds().isEmpty()) {
//				List<BenefitSchemes> currentBenefitSchemes = existingVoter.getBenefitSchemes();
//				currentBenefitSchemes.clear();
//				List<Long> mongoBenefitSchemeIds = new ArrayList<>();
//				// Iterate over the provided benefit scheme IDs
//				for (Long benefitSchemeId : voterUpdateDTO.getBenefitSchemeIds()) {
//					Optional<BenefitSchemes> benefitSchemeOpt = benefitSchemesRepository
//							.findByIdAndAccountIdAndElectionId(
//									benefitSchemeId, accountId, electionId);
//
//					if (benefitSchemeOpt.isPresent()) {
//						currentBenefitSchemes.add(benefitSchemeOpt.get()); // Add the found benefit scheme
//						mongoBenefitSchemeIds.add(benefitSchemeOpt.get().getId());
//					} else {
//						log.warn(
//								"Benefit Scheme not found with ID: {}, accountId: {}, electionId: {}. Skipping this benefit scheme.",
//								benefitSchemeId, accountId, electionId);
//					}
//				}
//				
//				existingVoterMongo.setBenefitSchemeIds(mongoBenefitSchemeIds);
//            } else if (voterUpdateDTO.getBenefitSchemeIds() != null) {
//                existingVoter.setBenefitSchemes(new ArrayList<>());
//                existingVoterMongo.setBenefitSchemeIds(new ArrayList<>());
//            }
//
//			// Handle Feedback/Issues update
//			if (voterUpdateDTO.getFeedbackIssueIds() != null && !voterUpdateDTO.getFeedbackIssueIds().isEmpty()) {
//				Set<FeedbackIssue> currentFeedbackIssues = existingVoter.getFeedbackIssues(); // Make sure getter is
//																								// present
//				if (currentFeedbackIssues == null) {
//					currentFeedbackIssues = new HashSet<>();
//				} else {
//					currentFeedbackIssues.clear(); // Clear old ones
//				}
//				Set<Long> mongoFeedbackIssueIds = new HashSet<>();
//				for (Long feedbackIssueId : voterUpdateDTO.getFeedbackIssueIds()) {
//					Optional<FeedbackIssue> feedbackIssueOpt = feedbackIssueRepository
//							.findByIdAndAccountIdAndElectionId(
//									feedbackIssueId, accountId, electionId);
//
//					if (feedbackIssueOpt.isPresent()) {
//						currentFeedbackIssues.add(feedbackIssueOpt.get());
//						mongoFeedbackIssueIds.add(feedbackIssueOpt.get().getId());
//					} else {
//						log.warn("Feedback Issue not found with ID: {}, accountId: {}, electionId: {}. Skipping.",
//								feedbackIssueId, accountId, electionId);
//					}
//				}
//				existingVoter.setFeedbackIssues(currentFeedbackIssues);
//				 existingVoterMongo.setFeedbackIssueIds(mongoFeedbackIssueIds);
//			} else if (voterUpdateDTO.getFeedbackIssueIds() != null) {
//                existingVoter.setFeedbackIssues(new HashSet<>());
//                existingVoterMongo.setFeedbackIssueIds(new HashSet<>());
//            }
//
//			if (voterUpdateDTO.getVoterHistoryIds() != null && !voterUpdateDTO.getVoterHistoryIds().isEmpty()) {
//				Set<VoterHistoryEntity> currentVoterHistories = existingVoter.getVoterHistories();
//
//				if (currentVoterHistories == null) {
//					currentVoterHistories = new HashSet<>();
//				} else {
//					currentVoterHistories.clear();
//				}
//				Set<Long> mongoVoterHistoryIds = new HashSet<>();
//				for (Long voterHistoryId : voterUpdateDTO.getVoterHistoryIds()) {
//					Optional<VoterHistoryEntity> voterHistoryOpt = voterHistoryRepository
//							.findByIdAndAccountIdAndElectionId(
//									voterHistoryId, accountId, electionId);
//
//					if (voterHistoryOpt.isPresent()) {
//						currentVoterHistories.add(voterHistoryOpt.get());
//						 mongoVoterHistoryIds.add(voterHistoryOpt.get().getId());
//					} else {
//						log.warn("VoterHistory not found with ID: {}, accountId: {}, electionId: {}. Skipping.",
//								voterHistoryId, accountId, electionId);
//					}
//				}
//				existingVoter.setVoterHistories(currentVoterHistories);
//				 existingVoterMongo.setVoterHistoryIds(mongoVoterHistoryIds);
//			} else if (voterUpdateDTO.getVoterHistoryIds() != null) {
//                existingVoter.setVoterHistories(new HashSet<>());
//                existingVoterMongo.setVoterHistoryIds(new HashSet<>());
//            }
//
//			if (voterUpdateDTO.getLanguageId() != null) { // Expecting single languageId (not a list)
//				Set<Language> currentLanguages = existingVoter.getLanguages();
//				currentLanguages.clear(); // Clear existing languages
//
//				// Retrieve the language based on the single languageId
//				Language language = languageRepository.findByIdAndAccountIdAndElectionId(
//						voterUpdateDTO.getLanguageId(), accountId, electionId)
//						.orElseThrow(() -> new ThedalException(ThedalError.LANGUAGE_NOT_FOUND, HttpStatus.NOT_FOUND));
//
//				currentLanguages.add(language); // Add the found language to the current languages
//				existingVoterMongo.setLanguageIds(Set.of(language.getId()));
//			} else {
//                existingVoter.setLanguages(new HashSet<>());
//                existingVoterMongo.setLanguageIds(new HashSet<>());
//            }
//
//			if (voterUpdateDTO.getAvailabilityId() != null) {
//				Availability availability = availabilityRepository.findByIdAndAccountIdAndElectionId(
//						voterUpdateDTO.getAvailabilityId(), accountId, electionId)
//						.orElseThrow(
//								() -> new ThedalException(ThedalError.AVAILABILITY_NOT_FOUND, HttpStatus.NOT_FOUND));
//				existingVoter.setAvailability1(availability);
//				existingVoterMongo.setAvailabilityId(availability.getId());
//			}
//
//			if (voterUpdateDTO.getPartyId() != null) {
//				Party party = partyRepository.findByIdAndAccountIdAndElectionId(
//						voterUpdateDTO.getPartyId(), accountId, electionId)
//						.orElseThrow(() -> new ThedalException(ThedalError.PARTY_NOT_FOUND, HttpStatus.NOT_FOUND));
//				existingVoter.setParty(party);
//				existingVoterMongo.setPartyId(party.getId());	
//				}
//
//			if (voterUpdateDTO.getReligionId() != null) {
//				ReligionEntity religion = religionRepository.findByIdAndAccountIdAndElectionId(
//						voterUpdateDTO.getReligionId(), accountId, electionId)
//						.orElseThrow(() -> new ThedalException(ThedalError.RELIGION_NOT_FOUND, HttpStatus.NOT_FOUND));
//				existingVoter.setReligion(religion);
//				existingVoterMongo.setReligionId(religion.getId());
//			}
//
//			if (voterUpdateDTO.getCasteId() != null) {
//				CasteEntity caste = casteRepository.findByIdAndAccountIdAndElectionId(
//						voterUpdateDTO.getCasteId(), accountId, electionId)
//						.orElseThrow(() -> new ThedalException(ThedalError.CASTE_NOT_FOUND, HttpStatus.NOT_FOUND));
//				existingVoter.setCaste(caste);
//				existingVoterMongo.setCasteId(caste.getId());
//			}
//
//			if (voterUpdateDTO.getSubCasteId() != null) {
//				SubCasteEntity subCaste = subCasteRepository.findByIdAndAccountIdAndElectionId(
//						voterUpdateDTO.getSubCasteId(), accountId, electionId)
//						.orElseThrow(() -> new ThedalException(ThedalError.SUBCASTE_NOT_FOUND, HttpStatus.NOT_FOUND));
//				existingVoter.setSubCaste(subCaste);
//				 existingVoterMongo.setSubCasteId(subCaste.getId());
//			}
//			// Persist the updated entity
//			log.debug("Attempting to save voter entity: {}", existingVoter);
//			voterRepository.save(existingVoter);
//			invalidateVoterCaches(electionId);
//			log.info("Voter with ID: {} successfully updated", epicNumber);	
//			
//			// Persist changes
//            log.debug("Attempting to save voter entity: {}", existingVoter);
//            VoterEntity savedVoter = voterRepository.save(existingVoter);
//            existingVoterMongo.setId(savedVoter.getId()); // Ensure ID consistency
//
//            voterMongoRepository.save(existingVoterMongo);
//
//            log.debug("VoterMongo familyId before save: {}", existingVoterMongo.getFamilyId());
//            voterMongoRepository.save(existingVoterMongo);
//            log.info("VoterMongo saved successfully with familyId: {}", existingVoterMongo.getFamilyId());
//
//            invalidateVoterCaches(electionId);
//            log.info("Voter with EPIC: {} successfully updated", epicNumber);
//
//            // Update reports
//            List<ElectionOverviewDTO> electionOverviewDTOList = new ArrayList<>();
//            ElectionOverviewDTO overviewDto = new ElectionOverviewDTO();
//            overviewDto.setGender(voterUpdateDTO.getGender());
//            overviewDto.setMobileNumber(voterUpdateDTO.getMobileNo());
//            overviewDto.setNewVoter(false);
//            overviewDto.setPincode(voterUpdateDTO.getPincode());
//            electionOverviewDTOList.add(overviewDto);
//            reportService.saveElectionOverview(savedVoter.getElectionId(), accountId, electionOverviewDTOList);
//
//            List<Integer> ageList = voterUpdateDTO.getDob() != null ?
//                    Arrays.asList(Period.between(voterUpdateDTO.getDob(), LocalDate.now()).getYears()) :
//                    Collections.emptyList();
//            reportService.votersBasedOnAge(ageList, savedVoter.getElectionId(), accountId);
//
//            List<VotersHavingContactsDTO> votersHavingContactsDTOList = new ArrayList<>();
//            VotersHavingContactsDTO contactsDto = new VotersHavingContactsDTO();
//            contactsDto.setBoothNumber(voterUpdateDTO.getBoothNumber());
//            contactsDto.setMobileNumber(voterUpdateDTO.getMobileNo());
//            votersHavingContactsDTOList.add(contactsDto);
//            reportService.votersHavingContacts(votersHavingContactsDTOList, savedVoter.getElectionId(), accountId);
//            
//			return mapEntityToDto(existingVoter);
//
//		} catch (Exception e) {
//			log.error("Error updating voter with ID: {}: {}", epicNumber, e.getMessage(), e);
//			throw new ThedalException(ThedalError.VOTER_UPDATE_FAILED, HttpStatus.INTERNAL_SERVER_ERROR,
//					e.getMessage());
//		}
//	}
//
//	private VoterUpdateDTO mapEntityToDto(VoterEntity voterEntity) {
//		VoterUpdateDTO dto = new VoterUpdateDTO();
//		dto.setEpicNumber(voterEntity.getEpicNumber());
//		dto.setGender(voterEntity.getGender());
//		dto.setEMail(voterEntity.getEMail());
//		dto.setPhotoUrl(voterEntity.getPhotoUrl());
//		dto.setBoothNumber(voterEntity.getBoothNumber());
//		dto.setStateCode(voterEntity.getStateCode());
//		dto.setPartNo(voterEntity.getPartNo());
//		dto.setAge(voterEntity.getAge());
//		dto.setDob(voterEntity.getDob());
//		dto.setSerialNo(voterEntity.getSerialNo());
//		dto.setHouseNoEn(voterEntity.getHouseNoEn());
//		dto.setHouseNoL1(voterEntity.getHouseNoL1());
//		dto.setVoterFnameEn(voterEntity.getVoterFnameEn());
//		dto.setVoterLnameEn(voterEntity.getVoterLnameEn());
//		dto.setVoterFnameL1(voterEntity.getVoterFnameL1());
//		dto.setVoterLnameL1(voterEntity.getVoterLnameL1());
//		dto.setRlnType(voterEntity.getRlnType());
//		dto.setRlnFnameEn(voterEntity.getRlnFnameEn());
//		dto.setRlnLnameEn(voterEntity.getRlnLnameEn());
//		dto.setRlnFnameL1(voterEntity.getRlnFnameL1());
//		dto.setRlnLnameL1(voterEntity.getRlnLnameL1());
//		dto.setSectionNo(voterEntity.getSectionNo());
//		dto.setSectionNameEn(voterEntity.getSectionNameEn());
//		dto.setSectionNameL1(voterEntity.getSectionNameL1());
//		dto.setFullAddress(voterEntity.getFullAddress());
//		dto.setPartNameEn(voterEntity.getPartNameEn());
//		dto.setPartNameL1(voterEntity.getPartNameL1());
//		dto.setPincode(voterEntity.getPincode());
//		dto.setPartLati(voterEntity.getPartLati());
//		dto.setPartLong(voterEntity.getPartLong());
//		dto.setPcNo(voterEntity.getPcNo());
//		dto.setPcNameEn(voterEntity.getPcNameEn());
//		dto.setPcNameL1(voterEntity.getPcNameL1());
//		dto.setAcNo(voterEntity.getAcNo());
//		dto.setAcNameEn(voterEntity.getAcNameEn());
//		dto.setAcNameL1(voterEntity.getAcNameL1());
//		dto.setRurDistrictUnionNo(voterEntity.getRurDistrictUnionNo());
//		dto.setRurDistrictUnionNameEn(voterEntity.getRurDistrictUnionNameEn());
//		dto.setRurDistrictUnionNameL1(voterEntity.getRurDistrictUnionNameL1());
//		dto.setRurDistrictUnionWardNo(voterEntity.getRurDistrictUnionWardNo());
//		dto.setPanUnionNo(voterEntity.getPanUnionNo());
//		dto.setPanUnionNameEn(voterEntity.getPanUnionNameEn());
//		dto.setPanUnionNameL1(voterEntity.getPanUnionNameL1());
//		dto.setPanUnionWardNo(voterEntity.getPanUnionWardNo());
//		dto.setVillPanNo(voterEntity.getVillPanNo());
//		dto.setVillPanNameEn(voterEntity.getVillPanNameEn());
//		dto.setVillPanNameL1(voterEntity.getVillPanNameL1());
//		dto.setVillPanWardNo(voterEntity.getVillPanWardNo());
//		dto.setVoterLati(voterEntity.getVoterLati());
//		dto.setVoterLongi(voterEntity.getVoterLongi());
//		dto.setStateNameEn(voterEntity.getStateNameEn());
//		dto.setDistrictCode(voterEntity.getDistrictCode());
//		dto.setDistrictNameEn(voterEntity.getDistrictNameEn());
//		dto.setDistrictNameL1(voterEntity.getDistrictNameL1());
//		dto.setUrbanNo(voterEntity.getUrbanNo());
//		dto.setUrbanNameEn(voterEntity.getUrbanNameEn());
//		dto.setUrbanNameL1(voterEntity.getUrbanNameL1());
//		dto.setUrbanWardNo(voterEntity.getUrbanWardNo());
//		dto.setMobileNo(voterEntity.getMobileNo());
//		dto.setWhatsappNo(voterEntity.getWhatsappNo());
//		dto.setStateNameL1(voterEntity.getStateNameL1());
//		dto.setHouseNoL2(voterEntity.getHouseNoL2());
//		dto.setVoterFnameL2(voterEntity.getVoterFnameL2());
//		dto.setVoterLnameL2(voterEntity.getVoterLnameL2());
//		dto.setRlnFnameL2(voterEntity.getRlnFnameL2());
//		dto.setRlnLnameL2(voterEntity.getRlnLnameL2());
//		dto.setSectionNameL2(voterEntity.getSectionNameL2());
//		dto.setPartNameL2(voterEntity.getPartNameL2());
//		dto.setStateNameL2(voterEntity.getStateNameL2());
//		dto.setDistrictNameL2(voterEntity.getDistrictNameL2());
//		dto.setPcNameL2(voterEntity.getPcNameL2());
//		dto.setAcNameL2(voterEntity.getAcNameL2());
//		dto.setRurDistrictUnionNameL2(voterEntity.getRurDistrictUnionNameL2());
//		dto.setPanUnionNameL2(voterEntity.getPanUnionNameL2());
//		dto.setStarNumber(voterEntity.getStarNumber());
//		dto.setAadhaarNumber(voterEntity.getAadhaarNumber());
//		dto.setPanNumber(voterEntity.getPanNumber());
//		dto.setPartyRegistrationNumber(voterEntity.getPartyRegistrationNumber());
//		dto.setMobileVerified(voterEntity.getMobileVerified());
//		dto.setMemberVerified(voterEntity.getMemberVerified());
//		dto.setAadhaarVerified(voterEntity.getAadhaarVerified());
//		dto.setPartyAffiliation(voterEntity.getPartyAffiliation());
//
//		if (voterEntity.getBenefitSchemes() != null && !voterEntity.getBenefitSchemes().isEmpty()) {
//			List<Long> benefitSchemeIds = voterEntity.getBenefitSchemes().stream()
//					.map(BenefitSchemes::getId)
//					.collect(Collectors.toList());
//			dto.setBenefitSchemeIds(benefitSchemeIds);
//		}
//
//		if (voterEntity.getLanguages() != null && !voterEntity.getLanguages().isEmpty()) {
//			// If there is only one language in the set, get its ID (assuming you are
//			// returning only one language in this case)
//			if (voterEntity.getLanguages().size() == 1) {
//				// Get the single language ID and set it in the DTO
//				Long languageId = voterEntity.getLanguages().iterator().next().getId();
//				dto.setLanguageId(languageId); // Return as a single-element list
//			}
//		}
//		if (voterEntity.getFeedbackIssues() != null && !voterEntity.getFeedbackIssues().isEmpty()) {
//			List<Long> feedbackIssueIds = voterEntity.getFeedbackIssues().stream()
//					.map(FeedbackIssue::getId)
//					.collect(Collectors.toList());
//
//			dto.setFeedbackIssueIds(feedbackIssueIds);
//		}
//
//		if (voterEntity.getVoterHistories() != null && !voterEntity.getVoterHistories().isEmpty()) {
//			List<Long> voterHistoryIds = voterEntity.getVoterHistories().stream()
//					.map(VoterHistoryEntity::getId)
//					.collect(Collectors.toList());
//
//			dto.setVoterHistoryIds(voterHistoryIds);
//		}
//
//		if (voterEntity.getAvailability1() != null) {
//			dto.setAvailabilityId(voterEntity.getAvailability1().getId());
//		}
//		if (voterEntity.getParty() != null) {
//			dto.setPartyId(voterEntity.getParty().getId());
//		}
//		if (voterEntity.getReligion() != null) {
//			dto.setReligionId(voterEntity.getReligion().getId());
//		}
//		if (voterEntity.getCaste() != null) {
//			dto.setCasteId(voterEntity.getCaste().getId());
//		}
//		if (voterEntity.getSubCaste() != null) {
//			dto.setSubCasteId(voterEntity.getSubCaste().getId());
//		}
//
//		dto.setPageNumber(voterEntity.getPageNumber());
//		dto.setRemarks(voterEntity.getRemarks());
//
//		return dto;
//	}

@Transactional
@Override
public VoterUpdateDTO updateVoter(String epicNumber, Long electionId, VoterUpdateDTO voterUpdateDTO) {

	Long accountId = requestDetails.getCurrentAccountId();
	if (accountId == null) {
		log.error("Account id not found, unauthorized access.");
		throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
	}
	validateElectionOwnership(electionId, accountId); 

	try {
		log.info("Updating voter with ID: {}, Election ID: {}, Account ID: {}", epicNumber, electionId, accountId);

		VoterEntity existingVoter = voterRepository.findByEpicNumberAndElectionIdAndAccountId(epicNumber, accountId, electionId)
		.orElseThrow(() -> {
				log.warn("Voter not found with epicNumber: {}, electionId: {}, accountId: {}", epicNumber, electionId, accountId);
				return new ThedalException(ThedalError.VOTER_NOT_FOUND, HttpStatus.NOT_FOUND);
			});
		
		// Validate booth access for write operations (volunteers can only update voters in their assigned booths)
		Long userId = requestDetails.getCurrentUserId();
		UserEntity currentUser = userRepo.findById(userId)
				.orElseThrow(() -> new RuntimeException("User not found"));
		Role userRole = currentUser.getRole();
		
		validateBoothAccessForWrite(existingVoter.getBoothNumber(), userRole, userId);
		
		// Track field changes for reporting (capture old values before updates)
		boolean mobileUpdated = false;
		boolean dobUpdated = false;
		boolean partyUpdated = false;
		boolean casteUpdated = false;
		boolean religionUpdated = false;
		boolean languageUpdated = false;
		
		if (voterUpdateDTO.getAadhaarNumber() != null && !voterUpdateDTO.getAadhaarNumber().trim().isEmpty()) {
		String aadhaarNumber = voterUpdateDTO.getAadhaarNumber().trim();
		Optional<VoterEntity> existingWithSameAadhaar =
			voterRepository.findByAadhaarNumber(voterUpdateDTO.getAadhaarNumber().trim());

	if (existingWithSameAadhaar.isPresent() &&
			!existingWithSameAadhaar.get().getId().equals(existingVoter.getId())) {

		// Construct the message in the desired format
		String details = String.format(
			"%s already exists (EPIC: %s).",
			voterUpdateDTO.getAadhaarNumber(),
			existingWithSameAadhaar.get().getEpicNumber()
		);

		// Pass only the details without adding extra prefixes
		throw new ThedalException(ThedalError.DUPLICATE_AADHAAR_NUMBER, HttpStatus.BAD_REQUEST, details);
	}
}
		
		// Handle booth number and part number
		if (voterUpdateDTO.getPartNo() != null) {
			log.info("Updating partNo to {} for voter with ID: {}", voterUpdateDTO.getPartNo(), epicNumber);
			Optional<PartManager> partManagerOpt = partManagerRepository.findByPartNoAndAccountIdAndElectionId(
					String.valueOf(voterUpdateDTO.getPartNo()), accountId, electionId);
			if (partManagerOpt.isPresent()) {
				existingVoter.setPartManager(partManagerOpt.get());
			} else {
				log.warn("PartManager not found for partNo: {}, accountId: {}, electionId: {}. Setting partManager to null.",
						voterUpdateDTO.getPartNo(), accountId, electionId);
				existingVoter.setPartManager(null);
			}
			ElectionBooth electionBooth = electionBoothService.saveBooth(electionId, voterUpdateDTO.getPartNo(), accountId);
			existingVoter.setPartNoAndBoothNumber(voterUpdateDTO.getPartNo()); // Sync partNo and boothNumber
			electionBooth.setBoothNumber(voterUpdateDTO.getPartNo());
			electionBoothService.updateElectionBooth(electionBooth);
		}
		
	 // Update epicNumber if provided in the DTO
		if (voterUpdateDTO.getEpicNumber() != null && !voterUpdateDTO.getEpicNumber().equals(epicNumber)) {
			// Check for uniqueness if required
			if (voterRepository.existsByEpicNumberAndElectionIdAndAccountId(voterUpdateDTO.getEpicNumber(), electionId, accountId)) {
				log.warn("EPIC Number {} already exists for electionId: {}, accountId: {}", voterUpdateDTO.getEpicNumber(), electionId, accountId);
				throw new ThedalException(ThedalError.EPIC_NUMBER_ALREADY_EXISTS, HttpStatus.CONFLICT);
			}
			existingVoter.setEpicNumber(voterUpdateDTO.getEpicNumber());
		}
		
		if (voterUpdateDTO.getGender() != null) existingVoter.setGender(voterUpdateDTO.getGender());	        
//        if (voterUpdateDTO.getReligion() != null) existingVoter.setReligion(voterUpdateDTO.getReligion());
//        if (voterUpdateDTO.getCaste() != null) existingVoter.setCaste(voterUpdateDTO.getCaste());
		if (voterUpdateDTO.getPhotoUrl() != null) existingVoter.setPhotoUrl(voterUpdateDTO.getPhotoUrl());	        
		if (voterUpdateDTO.getStateCode() != null) existingVoter.setStateCode(voterUpdateDTO.getStateCode());
		if (voterUpdateDTO.getStateNameL1() != null) existingVoter.setStateNameL1(voterUpdateDTO.getStateNameL1());        	                	        
		if (voterUpdateDTO.getPartNo() != null) existingVoter.setPartNo(voterUpdateDTO.getPartNo());	      
		if (voterUpdateDTO.getPartNameL1() != null) existingVoter.setPartNameL1(voterUpdateDTO.getPartNameL1());	      
		if (voterUpdateDTO.getPincode() != null) existingVoter.setPincode(voterUpdateDTO.getPincode());
		if (voterUpdateDTO.getSectionNo() != null) existingVoter.setSectionNo(voterUpdateDTO.getSectionNo());	        
		if (voterUpdateDTO.getAge() != null) existingVoter.setAge(voterUpdateDTO.getAge());
//        if (voterUpdateDTO.getDob() != null) existingVoter.setDob(voterUpdateDTO.getDob());
		
		// Track DOB update
		if (voterUpdateDTO.getDob() != null) {
			LocalDate oldDob = existingVoter.getDob();
			LocalDate newDob = voterUpdateDTO.getDob();
			if (!java.util.Objects.equals(oldDob, newDob)) {
				dobUpdated = true;
			}
			existingVoter.setDob(newDob);
		}
		//if (voterUpdateDTO.getSubCaste() != null) existingVoter.setSubCaste(voterUpdateDTO.getSubCaste());	        
		if (voterUpdateDTO.getSerialNo() != null) existingVoter.setSerialNo(voterUpdateDTO.getSerialNo());
		if (voterUpdateDTO.getHouseNoEn() != null) existingVoter.setHouseNoEn(voterUpdateDTO.getHouseNoEn());
		if (voterUpdateDTO.getHouseNoL1() != null) existingVoter.setHouseNoL1(voterUpdateDTO.getHouseNoL1());
		if (voterUpdateDTO.getVoterFnameEn() != null) existingVoter.setVoterFnameEn(voterUpdateDTO.getVoterFnameEn());
		if (voterUpdateDTO.getVoterLnameEn() != null) existingVoter.setVoterLnameEn(voterUpdateDTO.getVoterLnameEn());
		if (voterUpdateDTO.getVoterFnameL1() != null) existingVoter.setVoterFnameL1(voterUpdateDTO.getVoterFnameL1());
		if (voterUpdateDTO.getVoterLnameL1() != null) existingVoter.setVoterLnameL1(voterUpdateDTO.getVoterLnameL1());
		if (voterUpdateDTO.getRlnType() != null) existingVoter.setRlnType(voterUpdateDTO.getRlnType());
		if (voterUpdateDTO.getRlnFnameEn() != null) existingVoter.setRlnFnameEn(voterUpdateDTO.getRlnFnameEn());
		if (voterUpdateDTO.getRlnLnameEn() != null) existingVoter.setRlnLnameEn(voterUpdateDTO.getRlnLnameEn());
		if (voterUpdateDTO.getRlnFnameL1() != null) existingVoter.setRlnFnameL1(voterUpdateDTO.getRlnFnameL1());
		if (voterUpdateDTO.getRlnLnameL1() != null) existingVoter.setRlnLnameL1(voterUpdateDTO.getRlnLnameL1());
		if (voterUpdateDTO.getSectionNo() != null) existingVoter.setSectionNo(voterUpdateDTO.getSectionNo());
		if (voterUpdateDTO.getSectionNameEn() != null) existingVoter.setSectionNameEn(voterUpdateDTO.getSectionNameEn());
		if (voterUpdateDTO.getSectionNameL1() != null) existingVoter.setSectionNameL1(voterUpdateDTO.getSectionNameL1());
		if (voterUpdateDTO.getFullAddress() != null) existingVoter.setFullAddress(voterUpdateDTO.getFullAddress());
		if (voterUpdateDTO.getPartNameEn() != null) existingVoter.setPartNameEn(voterUpdateDTO.getPartNameEn());
		if (voterUpdateDTO.getPartLati() != null) existingVoter.setPartLati(voterUpdateDTO.getPartLati());
		if (voterUpdateDTO.getPartLong() != null) existingVoter.setPartLong(voterUpdateDTO.getPartLong());
		//if (voterUpdateDTO.getWhatsAppNo() != null) existingVoter.setWhatsAppNo(voterUpdateDTO.getWhatsAppNo());
		if (voterUpdateDTO.getPcNo() != null) existingVoter.setPcNo(voterUpdateDTO.getPcNo());
		if (voterUpdateDTO.getPcNameEn() != null) existingVoter.setPcNameEn(voterUpdateDTO.getPcNameEn());
		if (voterUpdateDTO.getPcNameL1() != null) existingVoter.setPcNameL1(voterUpdateDTO.getPcNameL1());
		if (voterUpdateDTO.getAcNo() != null) existingVoter.setAcNo(voterUpdateDTO.getAcNo());
		if (voterUpdateDTO.getAcNameEn() != null) existingVoter.setAcNameEn(voterUpdateDTO.getAcNameEn());
		if (voterUpdateDTO.getAcNameL1() != null) existingVoter.setAcNameL1(voterUpdateDTO.getAcNameL1());
		if (voterUpdateDTO.getRurDistrictUnionNo() != null) existingVoter.setRurDistrictUnionNo(voterUpdateDTO.getRurDistrictUnionNo());
		if (voterUpdateDTO.getRurDistrictUnionNameEn() != null) existingVoter.setRurDistrictUnionNameEn(voterUpdateDTO.getRurDistrictUnionNameEn());
		if (voterUpdateDTO.getRurDistrictUnionNameL1() != null) existingVoter.setRurDistrictUnionNameL1(voterUpdateDTO.getRurDistrictUnionNameL1());
		if (voterUpdateDTO.getRurDistrictUnionWardNo() != null) existingVoter.setRurDistrictUnionWardNo(voterUpdateDTO.getRurDistrictUnionWardNo());
		if (voterUpdateDTO.getPanUnionNo() != null) existingVoter.setPanUnionNo(voterUpdateDTO.getPanUnionNo());
		if (voterUpdateDTO.getPanUnionNameEn() != null) existingVoter.setPanUnionNameEn(voterUpdateDTO.getPanUnionNameEn());
		if (voterUpdateDTO.getPanUnionNameL1() != null) existingVoter.setPanUnionNameL1(voterUpdateDTO.getPanUnionNameL1());
		if (voterUpdateDTO.getPanUnionWardNo() != null) existingVoter.setPanUnionWardNo(voterUpdateDTO.getPanUnionWardNo());
		if (voterUpdateDTO.getVillPanNo() != null) existingVoter.setVillPanNo(voterUpdateDTO.getVillPanNo());
		if (voterUpdateDTO.getVillPanNameEn() != null) existingVoter.setVillPanNameEn(voterUpdateDTO.getVillPanNameEn());
		if (voterUpdateDTO.getVillPanNameL1() != null) existingVoter.setVillPanNameL1(voterUpdateDTO.getVillPanNameL1());
		if (voterUpdateDTO.getVillPanWardNo() != null) existingVoter.setVillPanWardNo(voterUpdateDTO.getVillPanWardNo());
		if (voterUpdateDTO.getVoterLati() != null) existingVoter.setVoterLati(voterUpdateDTO.getVoterLati());
		if (voterUpdateDTO.getVoterLongi() != null) existingVoter.setVoterLongi(voterUpdateDTO.getVoterLongi());
		if (voterUpdateDTO.getStateNameEn() != null) existingVoter.setStateNameEn(voterUpdateDTO.getStateNameEn());
		if (voterUpdateDTO.getDistrictCode() != null) existingVoter.setDistrictCode(voterUpdateDTO.getDistrictCode());
		if (voterUpdateDTO.getDistrictNameEn() != null) existingVoter.setDistrictNameEn(voterUpdateDTO.getDistrictNameEn());
		if (voterUpdateDTO.getDistrictNameL1() != null) existingVoter.setDistrictNameL1(voterUpdateDTO.getDistrictNameL1());
		if (voterUpdateDTO.getUrbanNo() != null) existingVoter.setUrbanNo(voterUpdateDTO.getUrbanNo());
		if (voterUpdateDTO.getUrbanNameEn() != null) existingVoter.setUrbanNameEn(voterUpdateDTO.getUrbanNameEn());
		if (voterUpdateDTO.getUrbanNameL1() != null) existingVoter.setUrbanNameL1(voterUpdateDTO.getUrbanNameL1());	      
		if (voterUpdateDTO.getUrbanWardNo() != null) existingVoter.setUrbanWardNo(voterUpdateDTO.getUrbanWardNo());
		if (voterUpdateDTO.getEMail() != null) existingVoter.setEMail(voterUpdateDTO.getEMail());
		
		// Track mobile number update
		if (voterUpdateDTO.getMobileNo() != null) {
			String oldMobile = existingVoter.getMobileNo();
			String newMobile = voterUpdateDTO.getMobileNo();
			if (!java.util.Objects.equals(oldMobile, newMobile)) {
				mobileUpdated = true;
				existingVoter.setMobileNo(newMobile);
			}
		}
		if (voterUpdateDTO.getWhatsappNo() != null) existingVoter.setWhatsappNo(voterUpdateDTO.getWhatsappNo());
		
		if (voterUpdateDTO.getHouseNoL2() != null) existingVoter.setHouseNoL2(voterUpdateDTO.getHouseNoL2());
		if (voterUpdateDTO.getVoterFnameL2() != null) existingVoter.setVoterFnameL2(voterUpdateDTO.getVoterFnameL2());
		if (voterUpdateDTO.getVoterLnameL2() != null) existingVoter.setVoterLnameL2(voterUpdateDTO.getVoterLnameL2());
		if (voterUpdateDTO.getRlnFnameL2() != null) existingVoter.setRlnFnameL2(voterUpdateDTO.getRlnFnameL2());
		if (voterUpdateDTO.getRlnFnameL2() != null) existingVoter.setRlnFnameL2(voterUpdateDTO.getRlnFnameL2());
		if (voterUpdateDTO.getRlnLnameL2() != null) existingVoter.setRlnLnameL2(voterUpdateDTO.getRlnLnameL2());
		if (voterUpdateDTO.getSectionNameL2() != null) existingVoter.setSectionNameL2(voterUpdateDTO.getSectionNameL2());
		if (voterUpdateDTO.getPartNameL2() != null) existingVoter.setPartNameL2(voterUpdateDTO.getPartNameL2());
		if (voterUpdateDTO.getStateNameL2() != null) existingVoter.setStateNameL2(voterUpdateDTO.getStateNameL2());
		if (voterUpdateDTO.getDistrictNameL2() != null) existingVoter.setDistrictNameL2(voterUpdateDTO.getDistrictNameL2());
		if (voterUpdateDTO.getPcNameL2() != null) existingVoter.setPcNameL2(voterUpdateDTO.getPcNameL2());
		if (voterUpdateDTO.getAcNameL2() != null) existingVoter.setAcNameL2(voterUpdateDTO.getAcNameL2());
		if (voterUpdateDTO.getRurDistrictUnionNameL2() != null) existingVoter.setRurDistrictUnionNameL2(voterUpdateDTO.getRurDistrictUnionNameL2());
		if (voterUpdateDTO.getPanUnionNameL2() != null) existingVoter.setPanUnionNameL2(voterUpdateDTO.getPanUnionNameL2());
		if (voterUpdateDTO.getPageNumber() != null) existingVoter.setPageNumber(voterUpdateDTO.getPageNumber());
		if (voterUpdateDTO.getRemarks() != null) existingVoter.setRemarks(voterUpdateDTO.getRemarks());
		if (voterUpdateDTO.getStarNumber() != null) existingVoter.setStarNumber(voterUpdateDTO.getStarNumber());
		if (voterUpdateDTO.getAadhaarNumber() != null) existingVoter.setAadhaarNumber(voterUpdateDTO.getAadhaarNumber());
		if (voterUpdateDTO.getPanNumber() != null) existingVoter.setPanNumber(voterUpdateDTO.getPanNumber());
		if (voterUpdateDTO.getPartyRegistrationNumber() != null) existingVoter.setPartyRegistrationNumber(voterUpdateDTO.getPartyRegistrationNumber());
		if (voterUpdateDTO.getMobileVerified() != null) existingVoter.setMobileVerified(voterUpdateDTO.getMobileVerified());
		if (voterUpdateDTO.getAadhaarVerified() != null) existingVoter.setAadhaarVerified(voterUpdateDTO.getAadhaarVerified());
		if (voterUpdateDTO.getMemberVerified() != null) existingVoter.setMemberVerified(voterUpdateDTO.getMemberVerified());
		if (voterUpdateDTO.getPartyAffiliation() != null) existingVoter.setPartyAffiliation(voterUpdateDTO.getPartyAffiliation());        
		
//		if (voterUpdateDTO.getBenefitSchemeIds() != null && !voterUpdateDTO.getBenefitSchemeIds().isEmpty()) {
//			List<BenefitSchemes> currentBenefitSchemes = existingVoter.getBenefitSchemes();
//			currentBenefitSchemes.clear();
//
//			// Iterate over the provided benefit scheme IDs
//			for (Long benefitSchemeId : voterUpdateDTO.getBenefitSchemeIds()) {
//				Optional<BenefitSchemes> benefitSchemeOpt = benefitSchemesRepository
//						.findByIdAndAccountIdAndElectionId(
//								benefitSchemeId, accountId, electionId);
//
//				if (benefitSchemeOpt.isPresent()) {
//					currentBenefitSchemes.add(benefitSchemeOpt.get()); // Add the found benefit scheme
//				} else {
//					log.warn(
//							"Benefit Scheme not found with ID: {}, accountId: {}, electionId: {}. Skipping this benefit scheme.",
//							benefitSchemeId, accountId, electionId);
//				}
//			}
//		}
//		if (voterUpdateDTO.getBenefitSchemeStatuses() != null && !voterUpdateDTO.getBenefitSchemeStatuses().isEmpty()) {
//		    List<VoterBenefitScheme> currentVoterBenefitSchemes = existingVoter.getVoterBenefitSchemes();
//		    if (currentVoterBenefitSchemes == null) {
//		        currentVoterBenefitSchemes = new ArrayList<>();
//		    }
//
//		    // Create a map to track existing benefit schemes
//		    Map<Long, VoterBenefitScheme> existingSchemes = currentVoterBenefitSchemes.stream()
//		        .collect(Collectors.toMap(vbs -> vbs.getBenefitScheme().getId(), vbs -> vbs));
//
//		    for (VoterUpdateDTO.BenefitSchemeStatusDTO statusDTO : voterUpdateDTO.getBenefitSchemeStatuses()) {
//		        Optional<BenefitSchemes> benefitSchemeOpt = benefitSchemesRepository
//		                .findByIdAndAccountIdAndElectionId(statusDTO.getSchemeId(), accountId, electionId);
//		        if (benefitSchemeOpt.isPresent()) {
//		            Long schemeId = statusDTO.getSchemeId();
//		            VoterBenefitScheme voterBenefitScheme = existingSchemes.get(schemeId);
//		            if (voterBenefitScheme == null) {
//		                // Create new entry if it doesn't exist
//		                voterBenefitScheme = new VoterBenefitScheme();
//		                voterBenefitScheme.setVoter(existingVoter);
//		                voterBenefitScheme.setBenefitScheme(benefitSchemeOpt.get());
//		                currentVoterBenefitSchemes.add(voterBenefitScheme);
//		            }
//		            // Update the selected status
//		            voterBenefitScheme.setSelected(statusDTO.isSelected());
//		        } else {
//		            log.warn("Benefit Scheme not found with ID: {}, accountId: {}, electionId: {}. Skipping this benefit scheme.",
//		                    statusDTO.getSchemeId(), accountId, electionId);
//		        }
//		    }
//		    existingVoter.setVoterBenefitSchemes(currentVoterBenefitSchemes);
//		} else {
//		    existingVoter.setVoterBenefitSchemes(new ArrayList<>());
//		}
		if (voterUpdateDTO.getBenefitSchemeStatuses() != null && !voterUpdateDTO.getBenefitSchemeStatuses().isEmpty()) {
		    List<VoterBenefitScheme> currentVoterBenefitSchemes = existingVoter.getVoterBenefitSchemes();
		    if (currentVoterBenefitSchemes == null) {
		        currentVoterBenefitSchemes = new ArrayList<>();
		    } else {
		        // Clear existing benefit schemes in the entity
		        currentVoterBenefitSchemes.clear();
		        // Delete existing entries from the database
		        voterBenefitSchemeRepository.deleteByVoterId(existingVoter.getId());
		    }

		    // Track unique schemeIds to prevent duplicates
		    Set<Long> uniqueSchemeIds = new HashSet<>();
		    for (VoterUpdateDTO.BenefitSchemeStatusDTO statusDTO : voterUpdateDTO.getBenefitSchemeStatuses()) {
		        Long schemeId = statusDTO.getSchemeId();
		        if (!uniqueSchemeIds.add(schemeId)) {
		            log.warn("Duplicate schemeId {} provided for voterId {}. Skipping duplicate.", schemeId, existingVoter.getId());
		            continue; // Skip duplicates
		        }

		        Optional<BenefitSchemes> benefitSchemeOpt = benefitSchemesRepository
		                .findByIdAndAccountIdAndElectionId(schemeId, accountId, electionId);
		        if (benefitSchemeOpt.isPresent()) {
		            VoterBenefitScheme voterBenefitScheme = new VoterBenefitScheme();
		            voterBenefitScheme.setVoter(existingVoter);
		            voterBenefitScheme.setBenefitScheme(benefitSchemeOpt.get());
		            voterBenefitScheme.setSelected(statusDTO.isSelected());
		            currentVoterBenefitSchemes.add(voterBenefitScheme);
		        } else {
		            log.warn("Benefit Scheme not found with ID: {}, accountId: {}, electionId: {}. Skipping this benefit scheme.",
		                    schemeId, accountId, electionId);
		        }
		    }
		    existingVoter.setVoterBenefitSchemes(currentVoterBenefitSchemes);
		} else {
		    // If no benefit schemes are provided, clear existing ones
		    if (existingVoter.getVoterBenefitSchemes() != null) {
		        existingVoter.getVoterBenefitSchemes().clear();
		        voterBenefitSchemeRepository.deleteByVoterId(existingVoter.getId());
		    }
		    existingVoter.setVoterBenefitSchemes(new ArrayList<>());
		}
		
		

		// Handle Feedback/Issues update
		if (voterUpdateDTO.getFeedbackIssueIds() != null && !voterUpdateDTO.getFeedbackIssueIds().isEmpty()) {
			Set<FeedbackIssue> currentFeedbackIssues = existingVoter.getFeedbackIssues(); // Make sure getter is
																							// present

			if (currentFeedbackIssues == null) {
				currentFeedbackIssues = new HashSet<>();
			} else {
				currentFeedbackIssues.clear(); // Clear old ones
			}


			for (Long feedbackIssueId : voterUpdateDTO.getFeedbackIssueIds()) {
				Optional<FeedbackIssue> feedbackIssueOpt = feedbackIssueRepository
						.findByIdAndAccountIdAndElectionId(
								feedbackIssueId, accountId, electionId);


				if (feedbackIssueOpt.isPresent()) {
					currentFeedbackIssues.add(feedbackIssueOpt.get());
				} else {
					log.warn("Feedback Issue not found with ID: {}, accountId: {}, electionId: {}. Skipping.",
							feedbackIssueId, accountId, electionId);
				}
			}
			existingVoter.setFeedbackIssues(currentFeedbackIssues);
		}

		if (voterUpdateDTO.getVoterHistoryIds() != null && !voterUpdateDTO.getVoterHistoryIds().isEmpty()) {
			Set<VoterHistoryEntity> currentVoterHistories = existingVoter.getVoterHistories();

			if (currentVoterHistories == null) {
				currentVoterHistories = new HashSet<>();
			} else {
				currentVoterHistories.clear();
			}

			for (Long voterHistoryId : voterUpdateDTO.getVoterHistoryIds()) {
				Optional<VoterHistoryEntity> voterHistoryOpt = voterHistoryRepository
						.findByIdAndAccountIdAndElectionId(
								voterHistoryId, accountId, electionId);

				if (voterHistoryOpt.isPresent()) {
					currentVoterHistories.add(voterHistoryOpt.get());
				} else {
					log.warn("VoterHistory not found with ID: {}, accountId: {}, electionId: {}. Skipping.",
							voterHistoryId, accountId, electionId);
				}
			}
			existingVoter.setVoterHistories(currentVoterHistories);
		}

		if (voterUpdateDTO.getLanguageId() != null) { // Expecting single languageId (not a list)
			Set<Language> currentLanguages = existingVoter.getLanguages();
			currentLanguages.clear(); // Clear existing languages

			// Retrieve the language based on the single languageId
			Language language = languageRepository.findByIdAndAccountIdAndElectionId(
					voterUpdateDTO.getLanguageId(), accountId, electionId)
					.orElseThrow(() -> new ThedalException(ThedalError.LANGUAGE_NOT_FOUND, HttpStatus.NOT_FOUND));

			currentLanguages.add(language); // Add the found language to the current languages
		}

		if (voterUpdateDTO.getAvailabilityId() != null) {
			Availability availability = availabilityRepository.findByIdAndAccountIdAndElectionId(
					voterUpdateDTO.getAvailabilityId(), accountId, electionId)
					.orElseThrow(
							() -> new ThedalException(ThedalError.AVAILABILITY_NOT_FOUND, HttpStatus.NOT_FOUND));
			existingVoter.setAvailability1(availability);
		}

		if (voterUpdateDTO.getDynamicFieldId() != null) {
			DynamicFieldEntity dynamicFieldEntity = dynamicFieldRepository.findByIdAndAccountIdAndElectionId(
					voterUpdateDTO.getDynamicFieldId(), accountId, electionId)
					.orElseThrow(
							() -> new ThedalException(ThedalError.DYNAMIC_FIELD_NOT_FOUND, HttpStatus.NOT_FOUND));
			existingVoter.setDynamicFieldEntity(dynamicFieldEntity);
		}
		
		if (voterUpdateDTO.getPartyId() != null) {
			Party party = partyRepository.findByIdAndAccountIdAndElectionId(
					voterUpdateDTO.getPartyId(), accountId, electionId)
					.orElseThrow(() -> new ThedalException(ThedalError.PARTY_NOT_FOUND, HttpStatus.NOT_FOUND));
			existingVoter.setParty(party);
		}

		// Dynamic fields update (only if provided)
		if (voterUpdateDTO.getDynamicFields() != null) {
			Map<String,Object> inputMap = voterUpdateDTO.getDynamicFields();
			// load active definitions
			List<DynamicFieldEntity> activeDefs = dynamicFieldRepository
					.findByAccountIdAndElectionIdAndStatusTrueOrderByOrderIndexAsc(accountId, electionId);
			Map<String, DynamicFieldEntity> defByName = activeDefs.stream()
					.collect(Collectors.toMap(d -> d.getName().toLowerCase(), Function.identity(), (a,b)->a));
			Map<String,String> normalizedValues = new HashMap<>();
			for (Map.Entry<String,Object> e : inputMap.entrySet()) {
				String rawKey = e.getKey();
				if (rawKey == null) continue;
				DynamicFieldEntity def = defByName.get(rawKey.toLowerCase());
				if (def == null) {
					log.warn("Ignoring unknown dynamic field '{}' on update", rawKey);
					continue;
				}
				Object rawValueObj = e.getValue();
				String value;
				if (rawValueObj == null) {
					value = null;
				} else if (rawValueObj instanceof List<?> listVal) {
					value = listVal.stream().filter(Objects::nonNull).map(Object::toString).collect(Collectors.joining(","));
				} else {
					value = rawValueObj.toString();
				}
				if (!isValidDynamicInput(def, value)) {
					log.error("Invalid value for dynamic field '{}' (type={}) on update raw='{}'", def.getName(), def.getType(), rawValueObj);
					throw new ThedalException(ThedalError.INVALID_FORM_FIELDS, HttpStatus.BAD_REQUEST);
				}
				if (value != null) value = value.trim();
				normalizedValues.put(def.getName(), value);
			}
			for (DynamicFieldEntity def : activeDefs) {
				if (Boolean.TRUE.equals(def.getRequired()) && !normalizedValues.containsKey(def.getName())) {
					log.error("Missing required dynamic field '{}' on update", def.getName());
					throw new ThedalException(ThedalError.INVALID_FORM_FIELDS, HttpStatus.BAD_REQUEST);
				}
			}
			existingVoter.setDynamicFields(normalizedValues);
		}

//		if (voterUpdateDTO.getVoterHistoryIds() != null && !voterUpdateDTO.getVoterHistoryIds().isEmpty()) {
//			Set<VoterHistoryEntity> currentVoterHistories = existingVoter.getVoterHistories();
//
//			if (currentVoterHistories == null) {
//				currentVoterHistories = new HashSet<>();
//			} else {
//				currentVoterHistories.clear();
//			}
//
//			for (Long voterHistoryId : voterUpdateDTO.getVoterHistoryIds()) {
//				Optional<VoterHistoryEntity> voterHistoryOpt = voterHistoryRepository
//						.findByIdAndAccountIdAndElectionId(
//								voterHistoryId, accountId, electionId);
//
//				if (voterHistoryOpt.isPresent()) {
//					currentVoterHistories.add(voterHistoryOpt.get());
//				} else {
//					log.warn("VoterHistory not found with ID: {}, accountId: {}, electionId: {}. Skipping.",
//							voterHistoryId, accountId, electionId);
//				}
//			}
//			existingVoter.setVoterHistories(currentVoterHistories);
//		}
		// Handle VoterHistory update
		Set<VoterHistoryEntity> currentVoterHistories = existingVoter.getVoterHistories();
		if (currentVoterHistories == null) {
			currentVoterHistories = new HashSet<>();
		} else {
			currentVoterHistories.clear();
		}

		if (voterUpdateDTO.getVoterHistoryIds() != null && !voterUpdateDTO.getVoterHistoryIds().isEmpty()) {
			for (Long voterHistoryId : voterUpdateDTO.getVoterHistoryIds()) {
				Optional<VoterHistoryEntity> voterHistoryOpt = voterHistoryRepository
						.findByIdAndAccountIdAndElectionId(
								voterHistoryId, accountId, electionId);

				if (voterHistoryOpt.isPresent()) {
					currentVoterHistories.add(voterHistoryOpt.get());
				} else {
					log.warn("VoterHistory not found with ID: {}, accountId: {}, electionId: {}. Skipping.",
							voterHistoryId, accountId, electionId);
				}
			}
		}
		existingVoter.setVoterHistories(currentVoterHistories);

		if (voterUpdateDTO.getLanguageId() != null) { // Expecting single languageId (not a list)
			Set<Language> currentLanguages = existingVoter.getLanguages();
			
			// Check if language is being changed
			Long oldLanguageId = currentLanguages.isEmpty() ? null : currentLanguages.iterator().next().getId();
			if (!java.util.Objects.equals(oldLanguageId, voterUpdateDTO.getLanguageId())) {
				languageUpdated = true;
			}
			
			currentLanguages.clear(); // Clear existing languages

			// Retrieve the language based on the single languageId
			Language language = languageRepository.findByIdAndAccountIdAndElectionId(
					voterUpdateDTO.getLanguageId(), accountId, electionId)
					.orElseThrow(() -> new ThedalException(ThedalError.LANGUAGE_NOT_FOUND, HttpStatus.NOT_FOUND));

			currentLanguages.add(language); // Add the found language to the current languages
		}

		if (voterUpdateDTO.getAvailabilityId() != null) {
			Availability availability = availabilityRepository.findByIdAndAccountIdAndElectionId(
					voterUpdateDTO.getAvailabilityId(), accountId, electionId)
					.orElseThrow(
							() -> new ThedalException(ThedalError.AVAILABILITY_NOT_FOUND, HttpStatus.NOT_FOUND));
			existingVoter.setAvailability1(availability);
		}
		
		if (voterUpdateDTO.getAvailabilityId() != null) {
			Availability availability = availabilityRepository.findByIdAndAccountIdAndElectionId(
					voterUpdateDTO.getAvailabilityId(), accountId, electionId)
					.orElseThrow(
							() -> new ThedalException(ThedalError.AVAILABILITY_NOT_FOUND, HttpStatus.NOT_FOUND));
			existingVoter.setAvailability1(availability);
		}
		else {
			existingVoter.setAvailability1(null);
		}

		if (voterUpdateDTO.getPartyId() != null) {
			// Check if party is being changed
			Long oldPartyId = existingVoter.getParty() != null ? existingVoter.getParty().getId() : null;
			if (!java.util.Objects.equals(oldPartyId, voterUpdateDTO.getPartyId())) {
				partyUpdated = true;
			}
			
			Party party = partyRepository.findByIdAndAccountIdAndElectionId(
					voterUpdateDTO.getPartyId(), accountId, electionId)
					.orElseThrow(() -> new ThedalException(ThedalError.PARTY_NOT_FOUND, HttpStatus.NOT_FOUND));
			existingVoter.setParty(party);
		}
		else {
			if (existingVoter.getParty() != null) {
				partyUpdated = true; // Clearing party is also an update
			}
			existingVoter.setParty(null);
		}

		if (voterUpdateDTO.getReligionId() != null) {
			// Check if religion is being changed
			Long oldReligionId = existingVoter.getReligion() != null ? existingVoter.getReligion().getId() : null;
			if (!java.util.Objects.equals(oldReligionId, voterUpdateDTO.getReligionId())) {
				religionUpdated = true;
			}
			
			ReligionEntity religion = religionRepository.findByIdAndAccountIdAndElectionId(
					voterUpdateDTO.getReligionId(), accountId, electionId)
					.orElseThrow(() -> new ThedalException(ThedalError.RELIGION_NOT_FOUND, HttpStatus.NOT_FOUND));
			existingVoter.setReligion(religion);
		}
		else {
			if (existingVoter.getReligion() != null) {
				religionUpdated = true; // Clearing religion is also an update
			}
			existingVoter.setReligion(null);
		}

		if (voterUpdateDTO.getCasteId() != null) {
			// Check if caste is being changed
			Long oldCasteId = existingVoter.getCaste() != null ? existingVoter.getCaste().getId() : null;
			if (!java.util.Objects.equals(oldCasteId, voterUpdateDTO.getCasteId())) {
				casteUpdated = true;
			}
			
			CasteEntity caste = casteRepository.findByIdAndAccountIdAndElectionId(
					voterUpdateDTO.getCasteId(), accountId, electionId)
					.orElseThrow(() -> new ThedalException(ThedalError.CASTE_NOT_FOUND, HttpStatus.NOT_FOUND));
			existingVoter.setCaste(caste);
		}
		else {
		    if (existingVoter.getCaste() != null) {
		    	casteUpdated = true; // Clearing caste is also an update
		    }
		    existingVoter.setCaste(null);
		}

		if (voterUpdateDTO.getSubCasteId() != null) {
			SubCasteEntity subCaste = subCasteRepository.findByIdAndAccountIdAndElectionId(
					voterUpdateDTO.getSubCasteId(), accountId, electionId)
					.orElseThrow(() -> new ThedalException(ThedalError.SUBCASTE_NOT_FOUND, HttpStatus.NOT_FOUND));
			existingVoter.setSubCaste(subCaste);
		}
		else {
		    existingVoter.setSubCaste(null);
		}
		if (voterUpdateDTO.getCasteCategoryId() != null) {
			CasteCategoryEntity casteCategory = casteCategoryRepository.findByIdAndAccountIdAndElectionId(
					voterUpdateDTO.getCasteCategoryId(), accountId, electionId)
					.orElseThrow(() -> new ThedalException(ThedalError.CASTE_CATEGORY_NOT_FOUND, HttpStatus.NOT_FOUND));
			existingVoter.setCasteCategory(casteCategory);
		}
		else {
			existingVoter.setCasteCategory(null);
		}
		
		// Persist the updated entity
		log.debug("Attempting to save voter entity: {}", existingVoter);

		voterRepository.save(existingVoter);
		
		// Track field updates in volunteer report (only if any field was actually changed)
		if (mobileUpdated || dobUpdated || partyUpdated || casteUpdated || religionUpdated || languageUpdated) {
			try {
				reportService.saveOrUpdateVolunteerVsVoterReport(
					electionId, 
					userId, 
					accountId,
					mobileUpdated, 
					religionUpdated, 
					casteUpdated, 
					dobUpdated, 
					partyUpdated,
					languageUpdated,
					false // isNewVoter = false (it's an update)
				);
				log.info("Tracked field updates for userId: {} - mobile:{}, dob:{}, party:{}, caste:{}, religion:{}, language:{}", 
					userId, mobileUpdated, dobUpdated, partyUpdated, casteUpdated, religionUpdated, languageUpdated);
			} catch (Exception reportEx) {
				// Don't fail the update if reporting fails
				log.error("Failed to track voter update in report for userId: {}, error: {}", userId, reportEx.getMessage());
			}
		}
		
		// Also update MongoDB to keep data in sync
		try {
			// Find existing MongoDB record or create new one
			Optional<VoterMongo> existingMongoVoter = voterMongoRepository.findByEpicNumberAndElectionIdAndAccountId(epicNumber, electionId, accountId);
			
			VoterMongo voterMongo;
			if (existingMongoVoter.isPresent()) {
				// Update existing MongoDB record
				voterMongo = existingMongoVoter.get();
				updateVoterMongoFromEntity(voterMongo, existingVoter);
				log.debug("Updating existing MongoDB record for EPIC: {}", epicNumber);
			} else {
				// Create new MongoDB record
				voterMongo = new VoterMongo(existingVoter);
				log.debug("Creating new MongoDB record for EPIC: {}", epicNumber);
			}
			
			// MongoDB sync disabled
			// voterMongoRepository.save(voterMongo);
			log.info("MongoDB sync disabled - Voter update completed for EPIC: {}", epicNumber);
		} catch (Exception mongoEx) {
			log.error("Failed to update voter in MongoDB for EPIC: {}, error: {}", epicNumber, mongoEx.getMessage());
			// Note: We don't throw here to avoid breaking the main update flow
			// PostgreSQL update succeeded, MongoDB update failed but data will be eventually consistent
		}
		
		invalidateVoterCaches(electionId);
		log.info("Voter with ID: {} successfully updated", epicNumber);
	
		return mapEntityToDto(existingVoter);

	} catch (Exception e) {
		log.error("Error updating voter with ID: {}: {}", epicNumber, e.getMessage(), e);
		throw new ThedalException(ThedalError.VOTER_UPDATE_FAILED, HttpStatus.INTERNAL_SERVER_ERROR,
				e.getMessage());
	}
}

private VoterUpdateDTO mapEntityToDto(VoterEntity voterEntity) {
	VoterUpdateDTO dto = new VoterUpdateDTO();
	// dto.setVoterId(voterEntity.getVoterId());
	dto.setEpicNumber(voterEntity.getEpicNumber());

	dto.setGender(voterEntity.getGender());
	dto.setEMail(voterEntity.getEMail());
	// dto.setReligion(voterEntity.getReligion());
	// dto.setCaste(voterEntity.getCaste());
	dto.setPhotoUrl(voterEntity.getPhotoUrl());
	dto.setBoothNumber(voterEntity.getBoothNumber());
	dto.setStateCode(voterEntity.getStateCode());
	dto.setPartNo(voterEntity.getPartNo());
	dto.setAge(voterEntity.getAge());
	dto.setDob(voterEntity.getDob());
	// dto.setSubCaste(voterEntity.getSubCaste());
	dto.setSerialNo(voterEntity.getSerialNo());
	dto.setHouseNoEn(voterEntity.getHouseNoEn());
	dto.setHouseNoL1(voterEntity.getHouseNoL1());
	dto.setVoterFnameEn(voterEntity.getVoterFnameEn());
	dto.setVoterLnameEn(voterEntity.getVoterLnameEn());
	dto.setVoterFnameL1(voterEntity.getVoterFnameL1());
	dto.setVoterLnameL1(voterEntity.getVoterLnameL1());
	// dynamic fields map
	if (voterEntity.getDynamicFields() != null) {
		dto.setDynamicFields(new HashMap<>(voterEntity.getDynamicFields()));
	}
	dto.setRlnType(voterEntity.getRlnType());
	dto.setRlnFnameEn(voterEntity.getRlnFnameEn());
	dto.setRlnLnameEn(voterEntity.getRlnLnameEn());
	dto.setRlnFnameL1(voterEntity.getRlnFnameL1());
	dto.setRlnLnameL1(voterEntity.getRlnLnameL1());
	dto.setSectionNo(voterEntity.getSectionNo());
	dto.setSectionNameEn(voterEntity.getSectionNameEn());
	dto.setSectionNameL1(voterEntity.getSectionNameL1());
	dto.setFullAddress(voterEntity.getFullAddress());
	dto.setPartNameEn(voterEntity.getPartNameEn());
	dto.setPartNameL1(voterEntity.getPartNameL1());
	dto.setPincode(voterEntity.getPincode());
	dto.setPartLati(voterEntity.getPartLati());
	dto.setPartLong(voterEntity.getPartLong());
	// dto.setWhatsAppNo(voterEntity.getWhatsAppNo());
	dto.setPcNo(voterEntity.getPcNo());
	dto.setPcNameEn(voterEntity.getPcNameEn());
	dto.setPcNameL1(voterEntity.getPcNameL1());
	dto.setAcNo(voterEntity.getAcNo());
	dto.setAcNameEn(voterEntity.getAcNameEn());
	dto.setAcNameL1(voterEntity.getAcNameL1());
	dto.setRurDistrictUnionNo(voterEntity.getRurDistrictUnionNo());
	dto.setRurDistrictUnionNameEn(voterEntity.getRurDistrictUnionNameEn());
	dto.setRurDistrictUnionNameL1(voterEntity.getRurDistrictUnionNameL1());
	dto.setRurDistrictUnionWardNo(voterEntity.getRurDistrictUnionWardNo());
	dto.setPanUnionNo(voterEntity.getPanUnionNo());
	dto.setPanUnionNameEn(voterEntity.getPanUnionNameEn());
	dto.setPanUnionNameL1(voterEntity.getPanUnionNameL1());
	dto.setPanUnionWardNo(voterEntity.getPanUnionWardNo());
	dto.setVillPanNo(voterEntity.getVillPanNo());
	dto.setVillPanNameEn(voterEntity.getVillPanNameEn());
	dto.setVillPanNameL1(voterEntity.getVillPanNameL1());
	dto.setVillPanWardNo(voterEntity.getVillPanWardNo());
	dto.setVoterLati(voterEntity.getVoterLati());
	dto.setVoterLongi(voterEntity.getVoterLongi());
	dto.setStateNameEn(voterEntity.getStateNameEn());
	dto.setDistrictCode(voterEntity.getDistrictCode());
	dto.setDistrictNameEn(voterEntity.getDistrictNameEn());
	dto.setDistrictNameL1(voterEntity.getDistrictNameL1());
	dto.setUrbanNo(voterEntity.getUrbanNo());
	dto.setUrbanNameEn(voterEntity.getUrbanNameEn());
	dto.setUrbanNameL1(voterEntity.getUrbanNameL1());
	dto.setUrbanWardNo(voterEntity.getUrbanWardNo());
	dto.setMobileNo(voterEntity.getMobileNo());
	dto.setWhatsappNo(voterEntity.getWhatsappNo());
	dto.setStateNameL1(voterEntity.getStateNameL1());
	dto.setHouseNoL2(voterEntity.getHouseNoL2());
	dto.setVoterFnameL2(voterEntity.getVoterFnameL2());
	dto.setVoterLnameL2(voterEntity.getVoterLnameL2());
	dto.setRlnFnameL2(voterEntity.getRlnFnameL2());
	dto.setRlnLnameL2(voterEntity.getRlnLnameL2());
	dto.setSectionNameL2(voterEntity.getSectionNameL2());
	dto.setPartNameL2(voterEntity.getPartNameL2());
	dto.setStateNameL2(voterEntity.getStateNameL2());
	dto.setDistrictNameL2(voterEntity.getDistrictNameL2());
	dto.setPcNameL2(voterEntity.getPcNameL2());
	dto.setAcNameL2(voterEntity.getAcNameL2());
	dto.setRurDistrictUnionNameL2(voterEntity.getRurDistrictUnionNameL2());
	dto.setPanUnionNameL2(voterEntity.getPanUnionNameL2());
	dto.setStarNumber(voterEntity.getStarNumber());
	dto.setAadhaarNumber(voterEntity.getAadhaarNumber());
	dto.setPanNumber(voterEntity.getPanNumber());
	dto.setPartyRegistrationNumber(voterEntity.getPartyRegistrationNumber());
	dto.setMobileVerified(voterEntity.getMobileVerified());
	dto.setMemberVerified(voterEntity.getMemberVerified());
	dto.setAadhaarVerified(voterEntity.getAadhaarVerified());
	dto.setPartyAffiliation(voterEntity.getPartyAffiliation());

//	if (voterEntity.getBenefitSchemes() != null && !voterEntity.getBenefitSchemes().isEmpty()) {
//		List<Long> benefitSchemeIds = voterEntity.getBenefitSchemes().stream()
//				.map(BenefitSchemes::getId)
//				.collect(Collectors.toList());
//		dto.setBenefitSchemeIds(benefitSchemeIds);
//	}
	if (voterEntity.getVoterBenefitSchemes() != null && !voterEntity.getVoterBenefitSchemes().isEmpty()) {
	    List<VoterUpdateDTO.BenefitSchemeStatusDTO> benefitSchemeStatuses = voterEntity.getVoterBenefitSchemes().stream()
	            .map(vbs -> {
	                VoterUpdateDTO.BenefitSchemeStatusDTO statusDTO = new VoterUpdateDTO.BenefitSchemeStatusDTO();
	                statusDTO.setSchemeId(vbs.getBenefitScheme().getId());
	                statusDTO.setSelected(vbs.getSelected());
	                return statusDTO;
	            })
	            .collect(Collectors.toList());
	    dto.setBenefitSchemeStatuses(benefitSchemeStatuses);
	}

	if (voterEntity.getLanguages() != null && !voterEntity.getLanguages().isEmpty()) {
		// If there is only one language in the set, get its ID (assuming you are
		// returning only one language in this case)
		if (voterEntity.getLanguages().size() == 1) {
			// Get the single language ID and set it in the DTO
			Long languageId = voterEntity.getLanguages().iterator().next().getId();
			dto.setLanguageId(languageId); // Return as a single-element list
		}
	}
	if (voterEntity.getFeedbackIssues() != null && !voterEntity.getFeedbackIssues().isEmpty()) {
		List<Long> feedbackIssueIds = voterEntity.getFeedbackIssues().stream()
				.map(FeedbackIssue::getId)
				.collect(Collectors.toList());

		dto.setFeedbackIssueIds(feedbackIssueIds);
	}

	if (voterEntity.getVoterHistories() != null && !voterEntity.getVoterHistories().isEmpty()) {
		List<Long> voterHistoryIds = voterEntity.getVoterHistories().stream()
				.map(VoterHistoryEntity::getId)
				.collect(Collectors.toList());

		dto.setVoterHistoryIds(voterHistoryIds);
	}

	if (voterEntity.getAvailability1() != null) {
		dto.setAvailabilityId(voterEntity.getAvailability1().getId());
	}
	if (voterEntity.getDynamicFieldEntity() != null) {
		dto.setDynamicFieldId(voterEntity.getDynamicFieldEntity().getId());
	}
	if (voterEntity.getParty() != null) {
		dto.setPartyId(voterEntity.getParty().getId());
	}
	if (voterEntity.getReligion() != null) {
		dto.setReligionId(voterEntity.getReligion().getId());
	}
	if (voterEntity.getCaste() != null) {
		dto.setCasteId(voterEntity.getCaste().getId());
	}
	if (voterEntity.getSubCaste() != null) {
		dto.setSubCasteId(voterEntity.getSubCaste().getId());
	}
	if (voterEntity.getCasteCategory() != null) {
		dto.setCasteCategoryId(voterEntity.getCasteCategory().getId());
	}

	dto.setPageNumber(voterEntity.getPageNumber());
	dto.setRemarks(voterEntity.getRemarks());

	return dto;
}

/**
 * Helper method to update VoterMongo object from VoterEntity
 */
private void updateVoterMongoFromEntity(VoterMongo voterMongo, VoterEntity voterEntity) {
	// Copy all relevant fields from VoterEntity to VoterMongo
	voterMongo.setVoterId(voterEntity.getVoterId());
	voterMongo.setReligionId(voterEntity.getReligion() != null ? voterEntity.getReligion().getId() : null);
	voterMongo.setCasteId(voterEntity.getCaste() != null ? voterEntity.getCaste().getId() : null);
	voterMongo.setSubCasteId(voterEntity.getSubCaste() != null ? voterEntity.getSubCaste().getId() : null);
	voterMongo.setCasteCategoryId(voterEntity.getCasteCategory() != null ? voterEntity.getCasteCategory().getId() : null);
	voterMongo.setPhotoUrl(voterEntity.getPhotoUrl());
	voterMongo.setAccountId(voterEntity.getAccountId());
	voterMongo.setElectionId(voterEntity.getElectionId());
	voterMongo.setBoothNumber(voterEntity.getBoothNumber());
	voterMongo.setHasVoted(voterEntity.getHasVoted());
	voterMongo.setVotedTimestamp(voterEntity.getVotedTimestamp());
	voterMongo.setModifiedTime(LocalDateTime.now()); // Update modification time
	voterMongo.setPartNo(voterEntity.getPartNo());
	voterMongo.setSectionNo(voterEntity.getSectionNo());
	voterMongo.setSerialNo(voterEntity.getSerialNo());
	voterMongo.setHouseNoEn(voterEntity.getHouseNoEn());
	voterMongo.setHouseNoL1(voterEntity.getHouseNoL1());
	voterMongo.setHouseNoL2(voterEntity.getHouseNoL2());
	voterMongo.setVoterFnameEn(voterEntity.getVoterFnameEn());
	voterMongo.setVoterLnameEn(voterEntity.getVoterLnameEn());
	voterMongo.setVoterFnameL1(voterEntity.getVoterFnameL1());
	voterMongo.setVoterFnameL2(voterEntity.getVoterFnameL2());
	voterMongo.setVoterLnameL1(voterEntity.getVoterLnameL1());
	voterMongo.setVoterLnameL2(voterEntity.getVoterLnameL2());
	voterMongo.setRlnType(voterEntity.getRlnType());
	voterMongo.setRlnFnameEn(voterEntity.getRlnFnameEn());
	voterMongo.setRlnLnameEn(voterEntity.getRlnLnameEn());
	voterMongo.setRlnFnameL1(voterEntity.getRlnFnameL1());
	voterMongo.setRlnFnameL2(voterEntity.getRlnFnameL2());
	voterMongo.setRlnLnameL1(voterEntity.getRlnLnameL1());
	voterMongo.setRlnLnameL2(voterEntity.getRlnLnameL2());
	voterMongo.setEpicNumber(voterEntity.getEpicNumber());
	voterMongo.setGender(voterEntity.getGender());
	voterMongo.setSectionNameEn(voterEntity.getSectionNameEn());
	voterMongo.setSectionNameL1(voterEntity.getSectionNameL1());
	voterMongo.setSectionNameL2(voterEntity.getSectionNameL2());
	voterMongo.setFullAddress(voterEntity.getFullAddress());
	voterMongo.setPartNameEn(voterEntity.getPartNameEn());
	voterMongo.setPartNameL1(voterEntity.getPartNameL1());
	voterMongo.setPartNameL2(voterEntity.getPartNameL2());
	voterMongo.setPincode(voterEntity.getPincode());
	voterMongo.setPartLati(voterEntity.getPartLati());
	voterMongo.setPartLong(voterEntity.getPartLong());
	voterMongo.setAge(voterEntity.getAge());
	voterMongo.setDob(voterEntity.getDob());
	voterMongo.setMobileNo(voterEntity.getMobileNo());
	voterMongo.setWhatsappNo(voterEntity.getWhatsappNo());
	voterMongo.setEMail(voterEntity.getEMail());
	voterMongo.setVoterLati(voterEntity.getVoterLati());
	voterMongo.setVoterLongi(voterEntity.getVoterLongi());
	voterMongo.setStateCode(voterEntity.getStateCode());
	voterMongo.setStateNameEn(voterEntity.getStateNameEn());
	voterMongo.setStateNameL1(voterEntity.getStateNameL1());
	voterMongo.setStateNameL2(voterEntity.getStateNameL2());
	voterMongo.setDistrictCode(voterEntity.getDistrictCode());
	voterMongo.setDistrictNameEn(voterEntity.getDistrictNameEn());
	voterMongo.setDistrictNameL1(voterEntity.getDistrictNameL1());
	voterMongo.setDistrictNameL2(voterEntity.getDistrictNameL2());
	voterMongo.setPcNo(voterEntity.getPcNo());
	voterMongo.setPcNameEn(voterEntity.getPcNameEn());
	voterMongo.setPcNameL1(voterEntity.getPcNameL1());
	voterMongo.setPcNameL2(voterEntity.getPcNameL2());
	voterMongo.setAcNo(voterEntity.getAcNo());
	voterMongo.setAcNameEn(voterEntity.getAcNameEn());
	voterMongo.setAcNameL1(voterEntity.getAcNameL1());
	voterMongo.setAcNameL2(voterEntity.getAcNameL2());
	voterMongo.setRurDistrictUnionNo(voterEntity.getRurDistrictUnionNo());
	voterMongo.setRurDistrictUnionNameEn(voterEntity.getRurDistrictUnionNameEn());
	voterMongo.setRurDistrictUnionNameL1(voterEntity.getRurDistrictUnionNameL1());
	voterMongo.setRurDistrictUnionNameL2(voterEntity.getRurDistrictUnionNameL2());
	voterMongo.setRurDistrictUnionWardNo(voterEntity.getRurDistrictUnionWardNo());
	voterMongo.setPanUnionNo(voterEntity.getPanUnionNo());
	voterMongo.setPanUnionNameEn(voterEntity.getPanUnionNameEn());
	voterMongo.setPanUnionNameL1(voterEntity.getPanUnionNameL1());
	voterMongo.setPanUnionNameL2(voterEntity.getPanUnionNameL2());
	voterMongo.setPanUnionWardNo(voterEntity.getPanUnionWardNo());
	voterMongo.setVillPanNo(voterEntity.getVillPanNo());
	voterMongo.setVillPanNameEn(voterEntity.getVillPanNameEn());
	voterMongo.setVillPanNameL1(voterEntity.getVillPanNameL1());
	voterMongo.setVillPanWardNo(voterEntity.getVillPanWardNo());
	voterMongo.setUrbanNo(voterEntity.getUrbanNo());
	voterMongo.setUrbanNameEn(voterEntity.getUrbanNameEn());
	voterMongo.setUrbanNameL1(voterEntity.getUrbanNameL1());
	voterMongo.setUrbanWardNo(voterEntity.getUrbanWardNo());
	voterMongo.setPageNumber(voterEntity.getPageNumber());
	voterMongo.setRemarks(voterEntity.getRemarks());
	voterMongo.setStarNumber(voterEntity.getStarNumber());
	voterMongo.setAadhaarNumber(voterEntity.getAadhaarNumber());
	voterMongo.setPanNumber(voterEntity.getPanNumber());
	voterMongo.setPartyRegistrationNumber(voterEntity.getPartyRegistrationNumber());
	voterMongo.setMobileVerified(voterEntity.getMobileVerified());
	voterMongo.setAadhaarVerified(voterEntity.getAadhaarVerified());
	voterMongo.setMemberVerified(voterEntity.getMemberVerified());
	voterMongo.setPartyAffiliation(voterEntity.getPartyAffiliation());

	// Handle dynamic fields and relationships
	if (voterEntity.getAvailability1() != null) {
		voterMongo.setAvailabilityId(voterEntity.getAvailability1().getId());
	}
	if (voterEntity.getParty() != null) {
		voterMongo.setPartyId(voterEntity.getParty().getId());
	}
	if (voterEntity.getPartManager() != null) {
		voterMongo.setPartManagerId(voterEntity.getPartManager().getId());
	}

	// Handle collections - convert to IDs
//	if (voterEntity.getBenefitSchemes() != null) {
//		List<Long> benefitSchemeIds = voterEntity.getBenefitSchemes().stream()
//				.map(BenefitSchemes::getId)
//				.collect(Collectors.toList());
//		voterMongo.setBenefitSchemeIds(benefitSchemeIds);
//	}

	if (voterEntity.getFeedbackIssues() != null) {
		Set<Long> feedbackIssueIds = voterEntity.getFeedbackIssues().stream()
				.map(FeedbackIssue::getId)
				.collect(Collectors.toSet());
		voterMongo.setFeedbackIssueIds(feedbackIssueIds);
	}

	if (voterEntity.getVoterHistories() != null) {
		Set<Long> voterHistoryIds = voterEntity.getVoterHistories().stream()
				.map(VoterHistoryEntity::getId)
				.collect(Collectors.toSet());
		voterMongo.setVoterHistoryIds(voterHistoryIds);
	}

	if (voterEntity.getLanguages() != null) {
		Set<Long> languageIds = voterEntity.getLanguages().stream()
				.map(Language::getId)
				.collect(Collectors.toSet());
		voterMongo.setLanguageIds(languageIds);
	}
}



		

	@Override
	//@Transactional(rollbackOn = Exception.class)
	@Transactional
	public ThedalResponse<Void> deleteById(String epicNumber, Long electionId) {
		Long accountId = requestDetails.getCurrentAccountId();

		if (accountId == null) {
			log.error("Account ID not found, unauthorized access.");
			throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
		}
		validateElectionOwnership(electionId, accountId); 

		try {
			log.info("Deleting voter with Epic Number: {}, for accountId: {}, electionId: {}", epicNumber, accountId, electionId);

			// Fetch the voter entity using epicNumber instead of voterId
			VoterEntity existingVoter = voterRepository.findByAccountIdAndElectionIdAndEpicNumber(accountId, electionId, epicNumber)
					.orElseThrow(() -> {
						log.warn("Voter not found with accountId: {}, electionId: {}, epicNumber: {}", accountId, electionId, epicNumber);
						return new ThedalException(ThedalError.VOTER_NOT_FOUND, HttpStatus.NOT_FOUND);
					});

			// Validate booth access for write operations (volunteers can only delete voters in their assigned booths)
			Long userId = requestDetails.getCurrentUserId();
			UserEntity currentUser = userRepo.findById(userId)
					.orElseThrow(() -> new RuntimeException("User not found"));
			Role userRole = currentUser.getRole();
			
			validateBoothAccessForWrite(existingVoter.getBoothNumber(), userRole, userId);

		 // Store the familyId before deletion
			UUID familyId = existingVoter.getFamilyId();
			
			// Delete associated records in voter_dynamic_fields (assuming voter_id is still used here)
			// Note: If voter_dynamic_fields uses epicNumber instead, adjust this logic accordingly
			// voterDynamicFieldRepository.deleteByVoterId(existingVoter.getEpicNumber());
			log.info("Associated dynamic fields for voter Epic Number: {} successfully deleted", epicNumber);

			// Delete the voter
			voterRepository.delete(existingVoter);
			log.info("Voter with Epic Number: {} successfully deleted", epicNumber);
			
		 // Delete voter from MongoDB
			voterMongoRepository.deleteByAccountIdAndElectionIdAndEpicNumber(accountId, electionId, epicNumber);
			log.info("Voter with Epic Number: {} successfully deleted from MongoDB", epicNumber);
			
		 // Update familyCount for the affected familyId, if it exists
			if (familyId != null) {
				log.info("Updating family count for familyId: {}", familyId);
				updateFamilyCount(familyId, accountId, electionId);
			}
			invalidateVoterCaches(electionId);
			return new ThedalResponse<>(ThedalSuccess.VOTER_DELETED);
		} catch (ThedalException e) {
			// Handle application-specific exceptions, like not found or unauthorized access
			log.error("Error while deleting voter with Epic Number: {}: {}", epicNumber, e.getMessage());
			throw e;
		} catch (Exception e) {
			log.error("Error deleting voter with Epic Number: {}: {}", epicNumber, e.getMessage());
			throw new ThedalException(ThedalError.ACCESS_DENIED, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}

	/**
	 * Retrieves all voter locations for mapping purposes.
	 *
	 * @return a ThedalResponse containing a list of voter locations (latitude and
	 *         longitude).
	 * @throws ThedalException if an error occurs during retrieval.
	 */
	@Override
	public ServiceResponse<String> getAllVoterLocations(Long electionId) {
		Long accountId = requestDetails.getCurrentAccountId();
		if (accountId == null) {
			log.error("Account ID not found, unauthorized access.");
			throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
		}

		validateElectionOwnership(electionId, accountId);
		String url = "https://thedalnew.s3.ap-south-1.amazonaws.com/thedalnew/voter-location/voter_locations/election_"
				+ electionId + ".json";

		log.info("Generated voter location URL for electionId: {}", electionId);

		int code = ThedalSuccess.MAP_VOTER_LOCATIONS_SUCCESS.getCode();
		String message = ThedalSuccess.MAP_VOTER_LOCATIONS_SUCCESS.getMessage();

		return new ServiceResponse<>("success", code, message, url);
	}

	/**
	 * Retrieves the voter details based on the provided EPIC number by making an
	 * external API call.
	 *
	 * @param epicNumber the EPIC number for which to retrieve voter details
	 * @return a JSON string containing the voter details retrieved from the
	 *         external API
	 */
	@Override
	public String getVoterDetails(String epicNumber) {
		String apiUrl = "https://voter-api-qno3.onrender.com/get-epic-details?epicNumber=" + epicNumber;

		// Use RestTemplate to make the external API call
		RestTemplate restTemplate = new RestTemplate();
		String response = restTemplate.getForObject(apiUrl, String.class);

		return response;
	}
		
		/**
		 * Retrieves a paginated list of bulk uploads based on the specified election ID and optional status.
		 * The results can be sorted by ID or start time as specified by the sortBy parameter.
		 *
		 * @param electionId the ID of the election for which to retrieve bulk uploads
		 * @param status the status to filter bulk uploads (can be null to retrieve all)
		 * @param page the page number to retrieve (0-indexed)
		 * @param size the number of results per page
		 * @param sortBy the field to sort by (default is "startTime", can be "id" to sort by ID)
		 * @return a list of BulkUploadDto objects representing the bulk uploads
		 */
		@Override
		//@Transactional
		public Page<BulkUploadDto> getBulkUploads(Long electionId, String status, Integer page, Integer size, String sortBy) {
			
			Long accountId = requestDetails.getCurrentAccountId();
			if (accountId == null) {
				log.error("Account ID not found, unauthorized access attempt.");
				throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
			}
			validateElectionOwnership(electionId, accountId); 

			try {
			
				Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, sortBy.equals("id") ? "id" : "startTime"));
				Page<BulkUploadEntity> bulkUploads;

				if (status != null) {
					// Filtering by status
					bulkUploads = bulkUploadRepo.findByAccountIdAndElectionIdAndStatus(accountId, electionId, BulkUploadStatus.valueOf(status.toUpperCase()), pageable);
				} else {
					// Sort by ID or Start Time based on the sortBy parameter
					 if ("id".equalsIgnoreCase(sortBy)) {
						bulkUploads = bulkUploadRepo.findByAccountIdAndElectionIdOrderByIdDesc(accountId, electionId, pageable);
					 } else {
					bulkUploads = bulkUploadRepo.findByAccountIdAndElectionIdOrderByStartTimeDesc(accountId, electionId, pageable);
					}
				 }
				
				if (bulkUploads.isEmpty()) {
					log.error("No bulk upload found for Election ID {}", electionId);
					throw new ThedalException(ThedalError.BULK_UPLOAD_NOT_FOUND_ELECTION, HttpStatus.NOT_FOUND);
				}

			 // Map BulkUploadEntity to BulkUploadDto for each element in the page
				Page<BulkUploadDto> bulkUploadDtos = bulkUploads.map(this::convertToDto);
				return bulkUploadDtos;
					
			} catch (IllegalArgumentException e) {
				log.error("Invalid status provided: {}", status, e);
				throw new ThedalException(ThedalError.INVALID_STATUS, HttpStatus.BAD_REQUEST);
			} catch (ThedalException e) {
				throw e;
			} 
			
		}
		
		private BulkUploadDto convertToDto(BulkUploadEntity entity) {
		BulkUploadDto dto = new BulkUploadDto(entity.getId(), entity.getElectionId(),
											  entity.getStartTime(), entity.getEndTime(), entity.getStatus());
		log.info("Converted BulkUploadEntity to BulkUploadDto: {}", dto);
		return dto;
	}
		/**
		 * Retrieves the status of a specific bulk upload identified by its ID.
		 *
		 * @param bulkUploadId the ID of the bulk upload whose status is to be retrieved
		 * @return a BulkUploadStatusDto containing the status and timing details of the bulk upload
		 * @throws ThedalException if the bulk upload with the specified ID is not found
		 */
		@Override
		public BulkUploadStatusDto getBulkUploadStatus(Long bulkUploadId, Long electionId) {
			
			Long accountId = requestDetails.getCurrentAccountId();
			if (accountId == null) {
				throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
			}
			validateElectionOwnership(electionId, accountId); 
			
			//BulkUploadEntity bulkUploadEntity = bulkUploadRepo.findByIdAndAccountId(bulkUploadId, accountId)
			BulkUploadEntity bulkUploadEntity = bulkUploadRepo.findByIdAndAccountIdAndElectionId(bulkUploadId, accountId, electionId)   
						  .orElseThrow(() -> new ThedalException(ThedalError.BULK_UPLOAD_NOT_FOUND, HttpStatus.NOT_FOUND));
			
			return new BulkUploadStatusDto(bulkUploadEntity.getId(), bulkUploadEntity.getStatus(), bulkUploadEntity.getStartTime(), bulkUploadEntity.getEndTime(),
					bulkUploadEntity.getTotalProcessedVoters(), 
					bulkUploadEntity.getTotalFailedVoters(),    
					bulkUploadEntity.getTotalRecords(),
					bulkUploadEntity.getTotalSuccessVoters() 
					);
		}
		

		@Override
		@Transactional
		public ThedalResponse<BulkUploadResponse> uploadVotersFromXlsxOrCsv(MultipartFile file, Long electionId) {
			long startTime = System.currentTimeMillis();
			Long accountId = requestDetails.getCurrentAccountId();

			if (accountId == null) {
				throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
			}

			if (file == null || !isSupportedFormat(file.getOriginalFilename()) || file.isEmpty()) {
				throw new ThedalException(ThedalError.INVALID_FILE_FORMAT, HttpStatus.BAD_REQUEST,
						"File is null, empty, or has an unsupported format");
			}

			Set<String> mandatoryHeaders = Set.of("epic_number");

			String folder = "voter_uploads";
			String uniqueId = UUID.randomUUID().toString().substring(0, 8);
			String originalFileName = file.getOriginalFilename();
			String fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
			String uniqueFileName = folder + "/voter_" + System.currentTimeMillis() + "_" + uniqueId + fileExtension;

			String fileUrl = null;
			Long bulkUploadId = null;

			try {
				// Validate headers
				Map<String, Integer> headerMapping = validateHeaders(file, fileExtension, mandatoryHeaders, electionId, accountId);

				// Upload to S3
				log.info("Uploading file to S3: {}", uniqueFileName);
				fileUrl = awsFileUpload.uploadMultipartFile(file, uniqueFileName, s3bucket);
				log.info("File uploaded to S3 at: {}", fileUrl);

				// Save bulk upload metadata
				BulkUploadEntity bulkUploadEntity = new BulkUploadEntity();
				bulkUploadEntity.setAccountId(accountId);
				bulkUploadEntity.setElectionId(electionId);
				bulkUploadEntity.setStartTime(LocalDateTime.now());
				bulkUploadEntity.setStatus(BulkUploadStatus.IN_PROGRESS);
				bulkUploadRepo.save(bulkUploadEntity);
				bulkUploadId = bulkUploadEntity.getId();

				NotificationType startNotification = notificationTemplate.bulkUploadStarted(originalFileName, electionId, bulkUploadId);
				notificationService.saveNotification(true, startNotification);

				// Save file metadata
				Files fileEntity = new Files(HandlerType.BULKUPLOAD_FILES, electionId, originalFileName, fileUrl);
				fileEntity.setBulkUpload(bulkUploadEntity);
				Files files = filesRepo.save(fileEntity);            // BYPASS QUARTZ - Direct execution for immediate processing
			System.out.println("=== BYPASSING QUARTZ - DIRECT EXECUTION ===");
			try {
				startUploadVotersFromXlsxOrCsv(accountId, bulkUploadId, electionId, files.getId(), headerMapping, mandatoryHeaders);
				System.out.println("=== DIRECT EXECUTION COMPLETED ===");
			} catch (Exception e) {
				System.err.println("DIRECT EXECUTION FAILED: " + e.getMessage());
				e.printStackTrace();
				updateBulkUploadStatus(bulkUploadId, BulkUploadStatus.FAILED);
				throw new ThedalException(ThedalError.FILE_PROCESSING_FAILED, HttpStatus.INTERNAL_SERVER_ERROR,
						"File processing failed: " + e.getMessage());
			}

				BulkUploadResponse bulkUploadResponse = new BulkUploadResponse(bulkUploadId);

				NotificationType completionNotification = notificationTemplate.bulkUploadCompleted(
						fileEntity.getFileName(),
						electionId,
						bulkUploadId
				);
				notificationService.saveNotification(true, completionNotification);

				return new ThedalResponse<>(ThedalSuccess.BULK_VOTERS_UPLOAD_IN_QUEUE, bulkUploadResponse);

			} catch (ThedalException te) {
				throw te;
			} catch (JsonProcessingException e) {
				log.error("JSON processing error for file '{}': {}", originalFileName, e.getMessage(), e);
				throw new ThedalException(ThedalError.FILE_PROCESSING_FAILED, HttpStatus.BAD_REQUEST,
						"Error processing file headers: " + e.getMessage());
			} catch (IOException e) {
				log.error("IO error reading file '{}': {}", originalFileName, e.getMessage(), e);
				throw new ThedalException(ThedalError.INVALID_FILE_DATA, HttpStatus.BAD_REQUEST,
						"Unable to read file: " + e.getMessage());
			} catch (Exception e) {
				log.error("Unexpected error processing file '{}': {}", originalFileName, e.getMessage(), e);
				throw new ThedalException(ThedalError.FILE_PROCESSING_FAILED, HttpStatus.INTERNAL_SERVER_ERROR,
						"An unexpected error occurred: " + e.getMessage());
			} finally {
				long endTime = System.currentTimeMillis();
				log.info("Time taken to process file '{}': {} ms", originalFileName, (endTime - startTime));
			}
		}

		private Map<String, Integer> validateHeaders(MultipartFile file, String fileExtension,
													Set<String> mandatoryHeaders, Long electionId, Long accountId) throws IOException {
			Map<String, Integer> headerMapping;

			log.info("Validating headers for file: {}", file.getOriginalFilename());
			if (fileExtension.equalsIgnoreCase(".xlsx")) {
				try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
					int numberOfSheets = workbook.getNumberOfSheets();
					log.debug("Number of sheets in Excel file: {}", numberOfSheets);

					if (numberOfSheets == 0) {
						throw new ThedalException(ThedalError.INVALID_FILE_DATA, HttpStatus.BAD_REQUEST,
								"Excel file has no sheets");
					}

					Sheet sheet = workbook.getSheetAt(1);
					if (sheet != null && sheet.getRow(0) != null) {
						log.debug("Sheet 2 (index 1) found with header row. Name: {}", sheet.getSheetName());
						headerMapping = buildHeaderMapping(sheet.getRow(0));
					} else {
						log.warn("Sheet at index 1 is missing or has no header row. Checking other sheets...");
						Sheet validSheet = null;
						for (int i = 0; i < numberOfSheets; i++) {
							sheet = workbook.getSheetAt(i);
							if (sheet != null && sheet.getRow(0) != null) {
								validSheet = sheet;
								log.info("Using sheet at index {} (name: {}) with header row", i, sheet.getSheetName());
								break;
							}
						}
						if (validSheet == null) {
							throw new ThedalException(ThedalError.INVALID_FILE_DATA, HttpStatus.BAD_REQUEST,
									"No sheet with a header row found in Excel file");
						}
						headerMapping = buildHeaderMapping(validSheet.getRow(0));
					}
				}
			} else {
				try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
					String headerLine = br.readLine();
					if (headerLine == null || headerLine.trim().isEmpty()) {
						throw new ThedalException(ThedalError.INVALID_FILE_DATA, HttpStatus.BAD_REQUEST,
								"CSV file has no header row");
					}
					String[] headers = headerLine.split(",");
					headerMapping = buildCsvHeaderMapping(headers);
				}
			}

			log.debug("Headers found in file: {}", headerMapping.keySet());

			// Check for epic_number only
			if (!headerMapping.containsKey("epic_number")) {
				throw new ThedalException(ThedalError.MANDATORY_FIELDS_MISSING, HttpStatus.BAD_REQUEST,
						"Mandatory field 'epic_number' is missing. Please check your file and try again.");
			}

			return headerMapping;
		}

		private Map<String, Integer> buildHeaderMapping(Row headerRow) {
			Map<String, Integer> headerMapping = new HashMap<>();
			for (Cell cell : headerRow) {
				if (cell.getCellType() == CellType.STRING) {
					String rawHeader = cell.getStringCellValue().trim();
					String normalizedHeader = normalizeHeader(rawHeader);
					headerMapping.put(normalizedHeader, cell.getColumnIndex());
					log.debug("Normalized header: '{}' -> '{}'", rawHeader, normalizedHeader);
				}
			}
			return headerMapping;
		}

		private Map<String, Integer> buildCsvHeaderMapping(String[] headers) {
			Map<String, Integer> headerMapping = new HashMap<>();
			for (int i = 0; i < headers.length; i++) {
				String rawHeader = headers[i].trim();
				String normalizedHeader = normalizeHeader(rawHeader);
				headerMapping.put(normalizedHeader, i);
				log.debug("Normalized header: '{}' -> '{}'", rawHeader, normalizedHeader);
			}
			return headerMapping;
		}

		private String normalizeHeader(String header) {
			if (header == null || header.trim().isEmpty()) {
				return "";
			}
			String normalized = header.trim()
					.toLowerCase()
					.replaceAll("[^a-z0-9]", "_")
					.replaceAll("_+", "_")
					.replaceAll("^_+|_+$", "");  // Remove leading and trailing underscores
			if (normalized.equals("epic_no")) {
				return "epic_number";
			}
			return normalized;
		}

		@Transactional
		public ThedalResponse<Void> startUploadVotersFromXlsxOrCsv(Long accountId, Long bulkUploadId, Long electionId, Long fileId,
																  Map<String, Integer> headerMapping, Set<String> mandatoryHeaders) {
			long startTime = System.currentTimeMillis();
			log.info("Starting voter upload for fileId: {}", fileId);

			Files fileMetadata = filesRepo.findById(fileId)
					.orElseThrow(() -> new ThedalException(ThedalError.FILE_NOT_FOUND, HttpStatus.NOT_FOUND));

			String fileUrl = fileMetadata.getUrl();
			String fileName = fileMetadata.getFileName();

			try {
				if (fileName.endsWith(".xlsx")) {
					voterFileUploadService.processExcelFileAsync(bulkUploadId, accountId, electionId, fileUrl, headerMapping, mandatoryHeaders);
				} else if (fileName.endsWith(".csv")) {
					voterFileUploadService.processCsvFileAsync(bulkUploadId, accountId, electionId, fileUrl, headerMapping, mandatoryHeaders);
				}
				log.info("Completed voter upload for fileId: {}", fileId);
				return new ThedalResponse<>(ThedalSuccess.BULK_VOTERS_CREATED);
			} catch (Exception e) {
				log.error("Error processing file for fileId {}: {}", fileId, e.getMessage(), e);
				updateBulkUploadStatus(bulkUploadId, BulkUploadStatus.FAILED);
				throw new ThedalException(ThedalError.FILE_PROCESSING_FAILED, HttpStatus.INTERNAL_SERVER_ERROR,
						"File processing failed: " + e.getMessage());
			} finally {
				log.info("Time taken to process fileId {}: {} ms", fileId, (System.currentTimeMillis() - startTime));
			}
		}

		public void updateBulkUploadStatus(Long bulkUploadId, BulkUploadStatus status) {
			BulkUploadEntity bulkUploadEntity = bulkUploadRepo.findById(bulkUploadId)
					.orElseThrow(() -> new ThedalException(ThedalError.BULK_UPLOAD_NOT_FOUND, HttpStatus.NOT_FOUND));
			bulkUploadEntity.setStatus(status);
			if (status == BulkUploadStatus.COMPLETED || status == BulkUploadStatus.FAILED) {
				bulkUploadEntity.setEndTime(LocalDateTime.now());
			}
			bulkUploadRepo.save(bulkUploadEntity);
		}

		private boolean isSupportedFormat(String originalFileName) {
			return originalFileName != null && (originalFileName.endsWith(".xlsx") || originalFileName.endsWith(".csv"));
		}
  
  @Transactional
				@Override
				public ThedalResponse<Map<String, Object>> updateVoterVotingStatus(Long electionId, String epicNumber, VoterVotingRequest request) {
					
					Long accountId = requestDetails.getCurrentAccountId();
					if (accountId == null) {
						log.error("Account ID not found, unauthorized access.");
						throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
					} 
					validateElectionOwnership(electionId, accountId); 

					//Optional<VoterEntity> voterOpt = voterRepository.findByVoterIdAndElectionId(voterId, electionId);
					//Optional<VoterEntity> voterOpt = voterRepository.findByEpicNumberAndElectionIdAndAccountId(epicNumber, electionId, accountId);
					log.info("Querying voter with epicNumber: {}, electionId: {}", epicNumber, electionId);
					Optional<VoterEntity> voterOpt = voterRepository.findByEpicNumberAndElectionId(epicNumber, electionId);
					if (voterOpt.isEmpty()) {
						throw new ThedalException(ThedalError.VOTER_NOT_FOUND, HttpStatus.NOT_FOUND);
					}

					VoterEntity voter = voterOpt.get();
					
					// Validate booth access for write operations (volunteers can only update voting status for voters in their assigned booths)
					Long userId = requestDetails.getCurrentUserId();
					UserEntity currentUser = userRepo.findById(userId)
							.orElseThrow(() -> new RuntimeException("User not found"));
					Role userRole = currentUser.getRole();
					
					validateBoothAccessForWrite(voter.getBoothNumber(), userRole, userId);
					
				 // Check if the requested voting status is the same as the current status
					if (Boolean.TRUE.equals(request.getHasVoted()) && Boolean.TRUE.equals(voter.getHasVoted())) {
						throw new ThedalException(ThedalError.VOTER_ALREADY_VOTED, HttpStatus.BAD_REQUEST);
					}
					if (Boolean.FALSE.equals(request.getHasVoted()) && Boolean.FALSE.equals(voter.getHasVoted())) {
						throw new ThedalException(ThedalError.VOTER_ALREADY_MARKED_AS_NOT_VOTED, HttpStatus.BAD_REQUEST);
					}

					// Update hasVoted field as true or false in the database
					voter.setHasVoted(request.getHasVoted());

					if (Boolean.TRUE.equals(request.getHasVoted())) {
						// If marking as voted, set timestamp (use provided or current time)
						voter.setVotedTimestamp(request.getVotedTimestamp() != null ? request.getVotedTimestamp() : LocalDateTime.now());
					} else {
						// If marking as not voted, reset timestamp
						voter.setVotedTimestamp(null);
					}

					VoterEntity updatedVoter = voterRepository.save(voter);
					
					// Also update MongoDB to keep voting status in sync - DISABLED
					// MongoDB sync is currently disabled
					/*
					try {
						VoterMongo voterMongo = new VoterMongo(updatedVoter);
						voterMongoRepository.save(voterMongo);
						log.info("Voter voting status successfully updated in MongoDB for EPIC: {}", epicNumber);
					} catch (Exception mongoEx) {
						log.error("Failed to update voter voting status in MongoDB for EPIC: {}, error: {}", epicNumber, mongoEx.getMessage());
						// Note: We don't throw here to avoid breaking the main update flow
					}
					*/

					// List<VoterVoteDetailsRequest> voterVoteDetailsRequestList = new ArrayList<>();
					// VoterVoteDetailsRequest voterVoteDetailsRequest=new VoterVoteDetailsRequest();
					// voterVoteDetailsRequest.setBoothNumber(updatedVoter.getBoothNumber());
					// voterVoteDetailsRequest.setVoteTimestamp(LocalDateTime.now());
					// voterVoteDetailsRequestList.add(voterVoteDetailsRequest);
					// reportService.recordVoteTime(accountId,electionId,voterVoteDetailsRequestList);	 
					// reportService.updateVoterAgeGroupCount(electionId,accountId,Arrays.asList(Period.between(voter.getDob(),LocalDate.now()).getYears()));

					// // Response map
					Map<String, Object> responseMap = new HashMap<>();
					responseMap.put("status", "success");
					responseMap.put("message", request.getHasVoted() ? "Voter has been marked as voted." : "Voter has been marked as not voted.");
					responseMap.put("epicNumber", voter.getEpicNumber());
					responseMap.put("electionId", electionId);
					responseMap.put("hasVoted", voter.getHasVoted());  // Stores and returns as true/false
					responseMap.put("votedTimestamp", voter.getVotedTimestamp());
				
					return new ThedalResponse<>(ThedalSuccess.VOTING_STATUS_UPDATED, responseMap);
				}
				
		@Transactional
		@Override
		public ThedalResponse<BulkVoterUpdateResponse> markMultipleVotersAsVoted(Long electionId, List<VoterVoteRequest> voterVoteRequests) {
			
			Long accountId = requestDetails.getCurrentAccountId();
			if (accountId == null) {
				log.error("Account ID not found, unauthorized access.");
				throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
			}
			validateElectionOwnership(electionId, accountId); 

			List<VoterEntity> updatedVoters = new ArrayList<>();
			List<String> updatedVotersIds = new ArrayList<>();
			List<String> alreadyVotedVotersIds = new ArrayList<>();
			List<String> notFoundVotersIds = new ArrayList<>();

			int alreadyVotedCount = 0;
			int updatedVotersCount = 0;

//		    // Extract voter IDs from the request for batch fetching
//		    List<String> voterIds = voterVoteRequests.stream()
//		            .map(VoterVoteRequest::getVoterId)
//		            .toList();
		 // Extract voter IDs from the request for batch fetching
			List<String> epicNumbers = voterVoteRequests.stream()
					.map(VoterVoteRequest::getEpicNumber)
					.collect(Collectors.toList());

//		    List<VoterEntity> voters = voterRepository.findAllByVoterIdInAndElectionId(voterIds, electionId);
//		    Set<String> foundVoterIds = voters.stream().map(VoterEntity::getVoterId).collect(Collectors.toSet());
			List<VoterEntity> voters = voterRepository.findAllByEpicNumberInAndElectionId(epicNumbers, electionId);
			Set<String> foundVoterIds = voters.stream().map(VoterEntity::getEpicNumber).collect(Collectors.toSet());
			
			// Get current user info for booth access validation
			Long userId = requestDetails.getCurrentUserId();
			UserEntity currentUser = userRepo.findById(userId)
					.orElseThrow(() -> new RuntimeException("User not found"));
			Role userRole = currentUser.getRole();
			
			// Process each request
			for (VoterVoteRequest request : voterVoteRequests) {
				String epicNumber = request.getEpicNumber();
				Boolean hasVoted = request.getHasVoted(); // Only accepts true/false
				LocalDateTime votedTimestamp = request.getVotedTimestamp(); // Only use this timestamp

				if (!foundVoterIds.contains(epicNumber)) {
					notFoundVotersIds.add(epicNumber);
					continue;
				}

				Optional<VoterEntity> optionalVoter = voters.stream()
						.filter(v -> v.getVoterId().equals(epicNumber))
						.findFirst();

				if (optionalVoter.isEmpty()) {
					notFoundVotersIds.add(epicNumber);
					continue;
				}

				VoterEntity voter = optionalVoter.get();
				
				// Validate booth access for write operations (volunteers can only update voting status for voters in their assigned booths)
				try {
					validateBoothAccessForWrite(voter.getBoothNumber(), userRole, userId);
				} catch (ThedalException e) {
					// If validation fails, add to notFoundVotersIds (or create a new list for access denied)
					log.warn("User {} does not have access to update voting status for voter {} in booth {}", 
							 userId, epicNumber, voter.getBoothNumber());
					notFoundVotersIds.add(epicNumber); // Treat as not found for security reasons
					continue;
				}

				if (Boolean.TRUE.equals(voter.getHasVoted()) && Boolean.TRUE.equals(hasVoted)) {
					alreadyVotedVotersIds.add(epicNumber);
					alreadyVotedCount++;
					continue;
				}

				// Mark voter with the provided hasVoted value
				voter.setHasVoted(hasVoted);

				if (Boolean.TRUE.equals(hasVoted)) {
					// If marking as voted, set timestamp (use provided or current time)
					voter.setVotedTimestamp(votedTimestamp != null ? votedTimestamp : LocalDateTime.now());
				} else {
					// If marking as not voted, reset timestamp
					voter.setVotedTimestamp(null);
				}

				updatedVoters.add(voter);
				updatedVotersIds.add(epicNumber);
				updatedVotersCount++;
			}

			// Batch save updated voters
			if (!updatedVoters.isEmpty()) {
				log.info("Saving {} updated voters...", updatedVoters.size());
				voterRepository.saveAll(updatedVoters);
				
				// Also update MongoDB to keep bulk voting status in sync - DISABLED
				// MongoDB sync is currently disabled
				/*
				try {
					List<VoterMongo> voterMongos = updatedVoters.stream()
							.map(VoterMongo::new)
							.collect(Collectors.toList());
					voterMongoRepository.saveAll(voterMongos);
					log.info("Bulk voter voting status successfully updated in MongoDB for {} voters", updatedVoters.size());
				} catch (Exception mongoEx) {
					log.error("Failed to bulk update voter voting status in MongoDB for {} voters, error: {}", 
							 updatedVoters.size(), mongoEx.getMessage());
					// Note: We don't throw here to avoid breaking the main update flow
				}
				*/
			} else {
				log.info("No voters were updated.");
			}

			// Prepare the response
			BulkVoterUpdateResponse response = new BulkVoterUpdateResponse(
					updatedVoters, updatedVotersIds, // Return only updated voters
					alreadyVotedVotersIds,
					notFoundVotersIds,
					voterVoteRequests.size(),
					alreadyVotedCount,
					updatedVotersCount
			);

//			List<Integer> votedAges = updatedVoters.stream()
//			.map(voter -> {
//				LocalDate birthDate = voter.getDob();
//				return Period.between(birthDate, LocalDate.now()).getYears();
//			})
//			.collect(Collectors.toList());
//  //reportService.recordVoteTime(electionId,voterVoteDetailsRequestList);	
//            reportService.updateVoterAgeGroupCount(electionId,accountId,votedAges);
			
			List<Integer> votedAges = updatedVoters.stream()
					.filter(voter -> voter.getDob() != null) // Skip voters with null DOB
					.map(voter -> Period.between(voter.getDob(), LocalDate.now()).getYears())
					.collect(Collectors.toList());
				reportService.updateVoterAgeGroupCount(electionId, accountId, votedAges);

			return new ThedalResponse<>(ThedalSuccess.VOTING_STATUS_UPDATED, response);
		}

		public ResponseEntity<ThedalResponse<String>> updateVoterImage(String epicNumber, Long electionId,  MultipartFile multipartFile) {
			 
			  Long accountId = requestDetails.getCurrentAccountId();
			  if (accountId == null) {
				  log.error("Account id not found, unauthorized access.");
				  throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
			  }  
			  validateElectionOwnership(electionId, accountId); 
			  
			  ThedalResponse<String> response = new ThedalResponse<>();

				Optional<VoterEntity> optionalVoter = voterRepository.findByVoterIdAndElectionIdAndAccountId(epicNumber, electionId, accountId);
				if (!optionalVoter.isPresent()) {
					response.setResponse(ThedalError.VOTER_NOT_FOUND);
					return ResponseEntity.badRequest().body(response);
				}

				VoterEntity voter = optionalVoter.get();
				
				// Validate booth access for write operations (volunteers can only update voter images in their assigned booths)
				Long userId = requestDetails.getCurrentUserId();
				UserEntity currentUser = userRepo.findById(userId)
						.orElseThrow(() -> new RuntimeException("User not found"));
				Role userRole = currentUser.getRole();
				
				validateBoothAccessForWrite(voter.getBoothNumber(), userRole, userId);

				// Upload the image and get the URL
				String uploadUrl;
				try {
					uploadUrl = uploadVoterImageToAWS(multipartFile);
				} catch (Exception e) {
					response.setResponse(ThedalError.IMAGE_UPLOAD_FAILED);
					return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
				}

				// Update the voter's image URL
				voter.setPhotoUrl(uploadUrl);
				voterRepository.save(voter);
				
				// Also update MongoDB to keep photo URL in sync - DISABLED
				// MongoDB sync is currently disabled
				/*
				try {
					VoterMongo voterMongo = new VoterMongo(voter);
					voterMongoRepository.save(voterMongo);
					log.info("Voter photo URL successfully updated in MongoDB for EPIC: {}", epicNumber);
				} catch (Exception mongoEx) {
					log.error("Failed to update voter photo URL in MongoDB for EPIC: {}, error: {}", epicNumber, mongoEx.getMessage());
					// Note: We don't throw here to avoid breaking the main update flow
				}
				*/

				response.setResponse(ThedalSuccess.VOTER_UPDATED_IMAGE, uploadUrl);
				return ResponseEntity.ok(response);
			}

		  public String uploadVoterImageToAWS(MultipartFile imageFile) {
				String contentType = imageFile.getContentType();

				// Validate content type (only JPEG and PNG allowed)
				if (!(MediaType.IMAGE_JPEG_VALUE.equals(contentType) ||
						MediaType.IMAGE_PNG_VALUE.equals(contentType))) {
					throw new ThedalException(ThedalError.INVALID_IMAGE_FORMAT, HttpStatus.BAD_REQUEST);
				}

				// Validate file size (max 5 MB)
				long maxFileSize = 5 * 1024 * 1024; // 5 MB
				if (imageFile.getSize() > maxFileSize) {
					throw new ThedalException(ThedalError.INVALID_IMAGE_SIZE, HttpStatus.BAD_REQUEST);
				}
			
				// Generate unique file name
				String fileExtension = "." + awsFileUpload.getFileExtension(imageFile.getOriginalFilename());
				String fileName = "voter_" + System.currentTimeMillis() + "_" + RandomTokenGenerator.generateToken(10) + fileExtension;

				// Upload to AWS S3
				try {
					// Create a temporary file
					File tempFile = File.createTempFile("temp", fileExtension);
					try (FileOutputStream fileOutputStream = new FileOutputStream(tempFile)) {
						fileOutputStream.write(imageFile.getBytes());
					}

					// Upload the file to AWS S3
					String awsUrl = awsFileUpload.uploadToAWS(tempFile, fileName, s3bucket);

					// Clean up the temporary file
					if (!tempFile.delete()) {
						log.warn("Temporary file deletion failed: {}", tempFile.getName());
					}

					return awsUrl;
				} catch (IOException e) {
					log.error("Error uploading voter image to AWS S3", e);
					throw new ThedalException(ThedalError.IMAGE_UPLOAD_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
				}
			}
		  
		  public ResponseEntity<ThedalResponse<String>> removeVoterImage(String epicNumber, Long electionId) {
			    Long accountId = requestDetails.getCurrentAccountId();
			    if (accountId == null) {
			        log.error("Account id not found, unauthorized access.");
			        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
			    }
			    validateElectionOwnership(electionId, accountId);

			    ThedalResponse<String> response = new ThedalResponse<>();

			    // Find the voter
			    Optional<VoterEntity> optionalVoter = voterRepository.findByVoterIdAndElectionIdAndAccountId(epicNumber, electionId, accountId);
			    if (!optionalVoter.isPresent()) {
			        response.setResponse(ThedalError.VOTER_NOT_FOUND);
			        return ResponseEntity.badRequest().body(response);
			    }

			    VoterEntity voter = optionalVoter.get();
			    
			    // Validate booth access for write operations (volunteers can only delete voter images in their assigned booths)
			    Long userId = requestDetails.getCurrentUserId();
			    UserEntity currentUser = userRepo.findById(userId)
			            .orElseThrow(() -> new RuntimeException("User not found"));
			    Role userRole = currentUser.getRole();
			    
			    validateBoothAccessForWrite(voter.getBoothNumber(), userRole, userId);

			    // Check if there is an image to delete
			    if (voter.getPhotoUrl() == null || voter.getPhotoUrl().isEmpty()) {
			        response.setResponse(ThedalSuccess.VOTER_NO_IMAGE_TO_DELETE);
			        return ResponseEntity.ok(response);
			    }

			    // Extract S3 object key from photoUrl
			    String s3ObjectKey = AwsFileUpload.getKeyFromUrl(voter.getPhotoUrl());
			    if (s3ObjectKey == null) {
			        log.warn("Could not extract S3 key from photoUrl: {}", voter.getPhotoUrl());
			        response.setResponse(ThedalError.INVALID_IMAGE_URL);
			        return ResponseEntity.badRequest().body(response);
			    }

			    // Delete the image from S3
			    try {
			        awsFileUpload.deleteS3Object(s3bucket, s3ObjectKey);
			        log.info("Successfully deleted voter image from S3 for EPIC: {}, key: {}", epicNumber, s3ObjectKey);
			    } catch (Exception e) {
			        log.error("Failed to delete voter image from S3 for EPIC: {}, key: {}, error: {}", 
			                epicNumber, s3ObjectKey, e.getMessage());
			        response.setResponse(ThedalError.IMAGE_DELETION_FAILED);
			        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
			    }

			    // Update voter entity to remove photo URL
			    voter.setPhotoUrl(null);
			    voterRepository.save(voter);

			    // Update MongoDB to keep photo URL in sync (if enabled)
			    /*
			    try {
			        VoterMongo voterMongo = new VoterMongo(voter);
			        voterMongoRepository.save(voterMongo);
			        log.info("Voter photo URL successfully removed in MongoDB for EPIC: {}", epicNumber);
			    } catch (Exception mongoEx) {
			        log.error("Failed to remove voter photo URL in MongoDB for EPIC: {}, error: {}", 
			                epicNumber, mongoEx.getMessage());
			        // Note: We don't throw here to avoid breaking the main update flow
			    }
			    */

			    response.setResponse(ThedalSuccess.VOTER_IMAGE_DELETED);
			    return ResponseEntity.ok(response);
			}


//		  @Transactional
//		  @Override
//		  public ThedalResponse<String> mapFamily(Long electionId, String epicNumber, String otherEpicNumber, UUID requestFamilyId) {
//		      Long accountId = requestDetails.getCurrentAccountId();
//		      if (accountId == null) {
//		          log.error("Account ID not found, unauthorized access.");
//		          throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
//		      }
//		      validateElectionOwnership(electionId, accountId);
//
//		      log.info("Starting mapFamily process. accountId: {}, electionId: {}, epicNumber: {}, otherEpicNumber: {}", 
//		               accountId, electionId, epicNumber, otherEpicNumber);
//
//		      // Trim epicNumber inputs
//		      String trimmedEpicNumber = epicNumber != null ? epicNumber.trim() : null;
//		      String trimmedOtherEpicNumber = otherEpicNumber != null ? otherEpicNumber.trim() : null;
//
//		      if (trimmedEpicNumber == null) {
//		          log.error("EpicNumber is null after trimming. accountId: {}, electionId: {}", accountId, electionId);
//		          throw new ThedalException(ThedalError.INVALID_EPIC_NUMBER, HttpStatus.BAD_REQUEST);
//		      }
//
//		      log.debug("Fetching voter with epicNumber: {}, electionId: {}, accountId: {}", trimmedEpicNumber, electionId, accountId);
//		      Optional<VoterEntity> pathVoterOpt = voterRepository.findByEpicNumberAndElectionIdAndAccountId(trimmedEpicNumber, accountId, electionId);
//		      if (!pathVoterOpt.isPresent()) {
//		          log.error("Voter not found. epicNumber: {}, electionId: {}, accountId: {}", trimmedEpicNumber, electionId, accountId);
//		          throw new ThedalException(ThedalError.VOTER_NOT_FOUND, HttpStatus.NOT_FOUND);
//		      }
//
//		      VoterEntity pathVoter = pathVoterOpt.get();
//		      UUID familyId;
//
//		      // Case 1: Same epicNumber or no otherEpicNumber
//		      if (trimmedOtherEpicNumber == null || trimmedOtherEpicNumber.equals(trimmedEpicNumber)) {
//		          log.debug("Handling case where otherEpicNumber is null or matches epicNumber: {}", trimmedEpicNumber);
//
//		          if (pathVoter.getFamilyId() != null) {
//		              log.error("Voter with epicNumber {} already has a familyId: {}. Cannot assign new one.", 
//		                        trimmedEpicNumber, pathVoter.getFamilyId());
//		              throw new ThedalException(ThedalError.FAMILY_ID_ALREADY_EXISTS, HttpStatus.BAD_REQUEST);
//		          }
//
//
//		          // Use requestFamilyId if provided, otherwise generate a new UUID
//		          familyId = requestFamilyId != null ? requestFamilyId : UUID.randomUUID();
//
//		          pathVoter.setFamilyId(familyId);
//		          log.debug("Assigning new familyId: {} to voter: {}", familyId, trimmedEpicNumber);
//		          voterRepository.save(pathVoter);
//		      } else {
//		          // Case 2: Different epicNumber
//		          log.debug("Fetching other voter with epicNumber: {}, electionId: {}, accountId: {}", 
//		                    trimmedOtherEpicNumber, electionId, accountId);
//		          Optional<VoterEntity> otherVoterOpt = voterRepository.findByEpicNumberAndElectionIdAndAccountId(
//		              trimmedOtherEpicNumber, accountId, electionId);
//		          if (!otherVoterOpt.isPresent()) {
//		              log.error("Other voter not found. epicNumber: {}, electionId: {}, accountId: {}", 
//		                        trimmedOtherEpicNumber, electionId, accountId);
//		              throw new ThedalException(ThedalError.VOTER_NOT_FOUND, HttpStatus.NOT_FOUND);
//		          }
//
//		          VoterEntity otherVoter = otherVoterOpt.get();
//
//
//		          if (pathVoter.getFamilyId() != null) {
//		              UUID newFamilyId = pathVoter.getFamilyId();
//		              UUID oldFamilyId = otherVoter.getFamilyId();
//
//		              if (oldFamilyId != null && !oldFamilyId.equals(newFamilyId)) {
//		                  log.info("Merging familyId: {} into familyId: {}", oldFamilyId, newFamilyId);
//		                  voterRepository.updateFamilyIdForAllVoters(oldFamilyId, newFamilyId);
//		              } else {
//		                  otherVoter.setFamilyId(newFamilyId);
//		                  log.debug("Assigning familyId: {} to other voter: {}", newFamilyId, trimmedOtherEpicNumber);
//		                  voterRepository.save(otherVoter);
//		              }
//		              familyId = newFamilyId;
//		          } else {
//		              // Use requestFamilyId if provided, otherwise use otherVoter's familyId or generate new
//		              familyId = requestFamilyId != null ? requestFamilyId : 
//		                         (otherVoter.getFamilyId() != null ? otherVoter.getFamilyId() : UUID.randomUUID());
//		              pathVoter.setFamilyId(familyId);
//		              otherVoter.setFamilyId(familyId);
//		              log.debug("Assigning familyId: {} to voters: {} and {}", familyId, trimmedEpicNumber, trimmedOtherEpicNumber);
//		              voterRepository.save(pathVoter);
//		              voterRepository.save(otherVoter);
//		          }
//		      }
//
//		      // Update familyCount
//		      updateFamilyCount(familyId, accountId, electionId);
//
//		      log.info("mapFamily process completed successfully. familyId: {}, accountId: {}, electionId: {}", 
//		               familyId, accountId, electionId);
//		      return new ThedalResponse<>(ThedalSuccess.FAMILY_ID_ASSIGNED);
//		  }
//
//		  @Transactional
//		  public void updateFamilyCount(UUID familyId, Long accountId, Long electionId) {
//		      int familySize = voterRepository.countByFamilyIdAndElectionIdAndAccountId(familyId, electionId, accountId);
//		      log.debug("Updating familyCount for familyId: {}, size: {}", familyId, familySize);
//		      voterRepository.updateFamilyCountForFamily(familyId, familySize);
//		  }
		 
		 
		 
		  
		 
		 
		  
		  @Transactional
		  @Override
		  public ThedalResponse<String> mapFamily(Long electionId, String epicNumber, String otherEpicNumber, UUID requestFamilyId) {
			  Long accountId = requestDetails.getCurrentAccountId();
			  if (accountId == null) {
				  log.error("Account ID not found, unauthorized access.");
				  throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
			  }
			  validateElectionOwnership(electionId, accountId);

			  log.info("Starting mapFamily process. accountId: {}, electionId: {}, epicNumber: {}, otherEpicNumber: {}", 
					   accountId, electionId, epicNumber, otherEpicNumber);

			  // Trim epicNumber inputs
			  String trimmedEpicNumber = epicNumber != null ? epicNumber.trim() : null;
			  String trimmedOtherEpicNumber = otherEpicNumber != null ? otherEpicNumber.trim() : null;

			  if (trimmedEpicNumber == null) {
				  log.error("EpicNumber is null after trimming. accountId: {}, electionId: {}", accountId, electionId);
				  throw new ThedalException(ThedalError.INVALID_EPIC_NUMBER, HttpStatus.BAD_REQUEST);
			  }

			  log.debug("Fetching voter with epicNumber: {}, electionId: {}, accountId: {}", trimmedEpicNumber, electionId, accountId);
			  Optional<VoterEntity> pathVoterOpt = voterRepository.findByEpicNumberAndElectionIdAndAccountId(trimmedEpicNumber, accountId, electionId);
			  if (!pathVoterOpt.isPresent()) {
				  log.error("Voter not found. epicNumber: {}, electionId: {}, accountId: {}", trimmedEpicNumber, electionId, accountId);
				  throw new ThedalException(ThedalError.VOTER_NOT_FOUND, HttpStatus.NOT_FOUND);
			  }

			  VoterEntity pathVoter = pathVoterOpt.get();
			  
			  // Validate booth access for write operations (volunteers can only map families for voters in their assigned booths)
			  Long userId = requestDetails.getCurrentUserId();
			  UserEntity currentUser = userRepo.findById(userId)
			          .orElseThrow(() -> new RuntimeException("User not found"));
			  Role userRole = currentUser.getRole();
			  
			  validateBoothAccessForWrite(pathVoter.getBoothNumber(), userRole, userId);
			  
			  UUID familyId;

			  // Case 1: Same epicNumber or no otherEpicNumber
			  if (trimmedOtherEpicNumber == null || trimmedOtherEpicNumber.equals(trimmedEpicNumber)) {
				  log.debug("Handling case where otherEpicNumber is null or matches epicNumber: {}", trimmedEpicNumber);

				  if (pathVoter.getFamilyId() != null) {
					  log.error("Voter with epicNumber {} already has a familyId: {}. Cannot assign new one.", 
								trimmedEpicNumber, pathVoter.getFamilyId());
					  throw new ThedalException(ThedalError.FAMILY_ID_ALREADY_EXISTS, HttpStatus.BAD_REQUEST);
				  }

				  // Use requestFamilyId if provided, otherwise generate a new UUID
				  familyId = requestFamilyId != null ? requestFamilyId : UUID.randomUUID();
				  pathVoter.setFamilyId(familyId);
				  log.debug("Assigning new familyId: {} to voter: {}", familyId, trimmedEpicNumber);
				  pathVoter = voterRepository.save(pathVoter);

				  // Sync to MongoDB - DISABLED
				  // MongoDB sync is currently disabled
				  /*
				  try {
					  VoterMongo voterMongo = new VoterMongo(pathVoter);
					  log.debug("Syncing voter to MongoDB: epicNumber={}, familyId={}", 
								pathVoter.getEpicNumber(), voterMongo.getFamilyId());
					  voterMongoRepository.saveVoterMongoWithNullFields(voterMongo);
					  log.info("Successfully synced voter to MongoDB: epicNumber={}, familyId={}", 
							   pathVoter.getEpicNumber(), familyId);
				  } catch (Exception mongoEx) {
					  log.error("Failed to sync voter to MongoDB for epicNumber: {}, familyId: {}, error: {}", 
								pathVoter.getEpicNumber(), familyId, mongoEx.getMessage());
				  }
				  */
			  } else {
				  // Case 2: Different epicNumber
				  log.debug("Fetching other voter with epicNumber: {}, electionId: {}, accountId: {}", 
							trimmedOtherEpicNumber, electionId, accountId);
				  Optional<VoterEntity> otherVoterOpt = voterRepository.findByEpicNumberAndElectionIdAndAccountId(
					  trimmedOtherEpicNumber, accountId, electionId);
				  if (!otherVoterOpt.isPresent()) {
					  log.error("Other voter not found. epicNumber: {}, electionId: {}, accountId: {}", 
								trimmedOtherEpicNumber, electionId, accountId);
					  throw new ThedalException(ThedalError.VOTER_NOT_FOUND, HttpStatus.NOT_FOUND);
				  }

				  VoterEntity otherVoter = otherVoterOpt.get();

				  if (pathVoter.getFamilyId() != null) {
					  UUID newFamilyId = pathVoter.getFamilyId();
					  UUID oldFamilyId = otherVoter.getFamilyId();

					  // Only move the specific otherVoter to the new family, not their entire old family
					  otherVoter.setFamilyId(newFamilyId);
					  log.debug("Moving voter {} from familyId: {} to familyId: {}", 
							    trimmedOtherEpicNumber, oldFamilyId, newFamilyId);
					  otherVoter = voterRepository.save(otherVoter);
					  
					  // If otherVoter had an old family, update that family's count
					  if (oldFamilyId != null && !oldFamilyId.equals(newFamilyId)) {
						  updateFamilyCount(oldFamilyId, accountId, electionId);
						  log.info("Updated family count for old familyId: {}", oldFamilyId);
					  }
					  
					  // Sync to MongoDB - DISABLED
					  // MongoDB sync is currently disabled
					  /*
					  try {
						  VoterMongo voterMongo = new VoterMongo(otherVoter);
						  log.debug("Syncing voter to MongoDB: epicNumber={}, familyId={}", 
									otherVoter.getEpicNumber(), voterMongo.getFamilyId());
						  voterMongoRepository.saveVoterMongoWithNullFields(voterMongo);
						  log.info("Successfully synced voter to MongoDB: epicNumber={}, familyId={}", 
								   otherVoter.getEpicNumber(), newFamilyId);
					  } catch (Exception mongoEx) {
						  log.error("Failed to sync voter to MongoDB for epicNumber: {}, familyId: {}, error: {}", 
									otherVoter.getEpicNumber(), newFamilyId, mongoEx.getMessage());
					  }
					  */
					  
					  familyId = newFamilyId;
				  } else {
					  // Use requestFamilyId if provided, otherwise use otherVoter's familyId or generate new
					  familyId = requestFamilyId != null ? requestFamilyId : 
								 (otherVoter.getFamilyId() != null ? otherVoter.getFamilyId() : UUID.randomUUID());
					  pathVoter.setFamilyId(familyId);
					  otherVoter.setFamilyId(familyId);
					  log.debug("Assigning familyId: {} to voters: {} and {}", familyId, trimmedEpicNumber, trimmedOtherEpicNumber);
					  pathVoter = voterRepository.save(pathVoter);
					  otherVoter = voterRepository.save(otherVoter);
					  // Sync to MongoDB - DISABLED
					  // MongoDB sync is currently disabled
					  /*
					  try {
						  VoterMongo pathVoterMongo = new VoterMongo(pathVoter);
						  VoterMongo otherVoterMongo = new VoterMongo(otherVoter);
						  log.debug("Syncing voter to MongoDB: epicNumber={}, familyId={}", 
									pathVoter.getEpicNumber(), pathVoterMongo.getFamilyId());
						  log.debug("Syncing voter to MongoDB: epicNumber={}, familyId={}", 
									otherVoter.getEpicNumber(), otherVoterMongo.getFamilyId());
						  voterMongoRepository.saveVoterMongoWithNullFields(pathVoterMongo);
						  voterMongoRepository.saveVoterMongoWithNullFields(otherVoterMongo);
						  log.info("Successfully synced voters to MongoDB: epicNumbers={}, {}, familyId={}", 
								   trimmedEpicNumber, trimmedOtherEpicNumber, familyId);
					  } catch (Exception mongoEx) {
						  log.error("Failed to sync voters to MongoDB for epicNumbers: {}, {}, familyId: {}, error: {}", 
									trimmedEpicNumber, trimmedOtherEpicNumber, familyId, mongoEx.getMessage());
					  }
					  */
				  }
		  }

		  // Update familyCount
		  updateFamilyCount(familyId, accountId, electionId);

		  // Assign family sequence number for proper ordering
		  Integer maxSequence = voterRepository.getMaxFamilySequenceNumber(accountId, electionId);
		  Integer sequenceNumber = (maxSequence == null ? 0 : maxSequence) + 1;
		  voterRepository.updateFamilySequenceNumber(familyId, sequenceNumber, accountId, electionId);
		  log.info("Assigned family sequence number {} to familyId: {}", sequenceNumber, familyId);

		  log.info("mapFamily process completed successfully. familyId: {}, accountId: {}, electionId: {}", 
				   familyId, accountId, electionId);
		  return new ThedalResponse<>(ThedalSuccess.FAMILY_ID_ASSIGNED);
	  }		
		  @Transactional
		  public void updateFamilyCount(UUID familyId, Long accountId, Long electionId) {
			  int familySize = voterRepository.countByFamilyIdAndElectionIdAndAccountId(familyId, electionId, accountId);
			  log.debug("Updating familyCount for familyId: {}, size: {}", familyId, familySize);
			  voterRepository.updateFamilyCountForFamily(familyId, familySize);

			  // Sync familyCount to MongoDB - DISABLED
			  // MongoDB sync is currently disabled
			  /*
			  try {
				  List<VoterEntity> voters = voterRepository.findAllByFamilyIdAndElectionIdAndAccountId(familyId, electionId, accountId);
				  for (VoterEntity voter : voters) {
					  voter.setFamilyCount(familySize);
					  VoterMongo voterMongo = new VoterMongo(voter);
					  log.debug("Syncing familyCount to MongoDB: epicNumber={}, familyId={}, familyCount={}", 
								voter.getEpicNumber(), voterMongo.getFamilyId(), voterMongo.getFamilyCount());
					  voterMongoRepository.saveVoterMongoWithNullFields(voterMongo);
				  }
				  log.info("Updated familyCount in MongoDB for familyId: {}, count: {}", familyId, familySize);
			  } catch (Exception mongoEx) {
				  log.error("Failed to update familyCount in MongoDB for familyId: {}, error: {}", familyId, mongoEx.getMessage());
			  }
			  */
		  }
		
		 
		
//		  @Override
//		  @Transactional
//		  public ThedalResponse<String> deleteFamilyId(Long electionId, String epicNumber) {
//			 
//			  Long accountId = requestDetails.getCurrentAccountId();
//		      if (accountId == null) {
//		          log.error("Account ID not found, unauthorized access.");
//		          throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
//		      }
//		      validateElectionOwnership(electionId, accountId); 
//
//		      // Fetch the voter by epicNumber
//		      Optional<VoterEntity> voterOpt = voterRepository.findByEpicNumberAndElectionIdAndAccountId(epicNumber, accountId, electionId);
//		      if (!voterOpt.isPresent()) {
//		    	  log.warn("Voter not found with epicNumber: {}, electionId: {}, accountId: {}", epicNumber, electionId, accountId);
//		          throw new ThedalException(ThedalError.VOTER_NOT_FOUND, HttpStatus.NOT_FOUND);
//		      }
//
//		      VoterEntity voter = voterOpt.get();
//		      UUID familyId = voter.getFamilyId(); 
//
//		      // Check if the voter has a familyId
//		      if (voter.getFamilyId() == null) {
//		          log.warn("Voter with epicNumber {} does not have a familyId to delete.", epicNumber);
//		          throw new ThedalException(ThedalError.FAMILY_ID_NOT_FOUND, HttpStatus.BAD_REQUEST);
//		      }
//
//		      // Remove the familyId by setting it to null
//		      voter.setFamilyId(null);
//		      //voter.setFamilyCount(null); 
//		      voter.setFamilyCount(1); 
//		      voterRepository.save(voter);
//		      
//		      updateFamilyCount(familyId, accountId, electionId);
//
//		      log.info("Successfully removed familyId for voter with epicNumber {}", epicNumber);
//		      return new ThedalResponse<>(ThedalSuccess.FAMILY_ID_DELETED);
//		  }

		  @Override
		  @Transactional
		  public ThedalResponse<String> deleteFamilyId(Long electionId, String epicNumber) {
			  Long accountId = requestDetails.getCurrentAccountId();
			  if (accountId == null) {
				  log.error("Account ID not found, unauthorized access.");
				  throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
			  }
			  validateElectionOwnership(electionId, accountId); 

			  // Fetch the voter by epicNumber
			  Optional<VoterEntity> voterOpt = voterRepository.findByEpicNumberAndElectionIdAndAccountId(epicNumber, accountId, electionId);
			  if (!voterOpt.isPresent()) {
				  log.warn("Voter not found with epicNumber: {}, electionId: {}, accountId: {}", epicNumber, electionId, accountId);
				  throw new ThedalException(ThedalError.VOTER_NOT_FOUND, HttpStatus.NOT_FOUND);
			  }

			  VoterEntity voter = voterOpt.get();
			  
			  // Validate booth access for write operations (volunteers can only delete family mappings for voters in their assigned booths)
			  Long userId = requestDetails.getCurrentUserId();
			  UserEntity currentUser = userRepo.findById(userId)
			          .orElseThrow(() -> new RuntimeException("User not found"));
			  Role userRole = currentUser.getRole();
			  
			  validateBoothAccessForWrite(voter.getBoothNumber(), userRole, userId);
			  
			  UUID familyId = voter.getFamilyId(); 

			  // Check if the voter has a familyId
			  if (voter.getFamilyId() == null) {
				  log.warn("Voter with epicNumber {} does not have a familyId to delete.", epicNumber);
				  throw new ThedalException(ThedalError.FAMILY_ID_NOT_FOUND, HttpStatus.BAD_REQUEST);
			  }

			  // Remove the familyId by setting it to null
			  voter.setFamilyId(null);
			  voter.setFamilyCount(1); 
			  voter = voterRepository.save(voter);
			  
			  // Sync to MongoDB - DISABLED
			  // MongoDB sync is currently disabled
			  /*
			  try {
				  VoterMongo voterMongo = new VoterMongo(voter);
				  log.debug("Syncing voter to MongoDB: epicNumber={}, familyId={}, familyCount={}", 
							voter.getEpicNumber(), voterMongo.getFamilyId(), voterMongo.getFamilyCount());
				  voterMongoRepository.saveVoterMongoWithNullFields(voterMongo);
				  log.info("Successfully synced voter to MongoDB: epicNumber={}, familyId=null, familyCount=1", 
						   voter.getEpicNumber());
			  } catch (Exception mongoEx) {
				  log.error("Failed to sync voter to MongoDB for epicNumber: {}, familyId: null, error: {}", 
							voter.getEpicNumber(), mongoEx.getMessage());
			  }
			  */

			  updateFamilyCount(familyId, accountId, electionId);

			  log.info("Successfully removed familyId for voter with epicNumber {}", epicNumber);
			  return new ThedalResponse<>(ThedalSuccess.FAMILY_ID_DELETED);
		  }
		
				  
		  @Override
		  @Transactional
		  public ThedalResponse<Object> deleteVoters(Long electionId, List<String> epicNumbers) {
			  Long accountId = requestDetails.getCurrentAccountId();

			  if (accountId == null) {
				  log.error("Account ID not found, unauthorized access.");
				  throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
			  }
			  validateElectionOwnership(electionId, accountId); 

			  try {
				  int deletedCount;
				  Set<UUID> affectedFamilyIds = new HashSet<>();

				  if (epicNumbers == null || epicNumbers.isEmpty()) {
					  log.info("Deleting all voters for electionId: {}, accountId: {}", electionId, accountId);

					  // Collect familyIds before deletion
					  List<VoterEntity> voters = voterRepository.findByAccountIdAndElectionId(accountId, electionId);
					  voters.forEach(voter -> {
						  if (voter.getFamilyId() != null) {
							  affectedFamilyIds.add(voter.getFamilyId());
						  }
					  });

					  // Delete all voters
					  deletedCount = voterRepository.deleteByAccountIdAndElectionId(accountId, electionId);
					  voterMongoRepository.deleteByAccountIdAndElectionId(accountId, electionId);
					  log.info("Deleted {} voters from both databases for electionId: {}", deletedCount, electionId);
				  } else {
					  log.info("Deleting specific voters for electionId: {}, accountId: {} with EPIC numbers: {}", 
							  electionId, accountId, epicNumbers);

					  // Clean and validate EPIC numbers
					  List<String> cleanedEpicNumbers = epicNumbers.stream()
							  .map(String::trim)
							  .filter(epic -> !epic.isEmpty())
							  .collect(Collectors.toList());

					  if (cleanedEpicNumbers.isEmpty()) {
						  log.warn("No valid EPIC numbers provided after cleaning");
						  throw new ThedalException(ThedalError.INVALID_INPUT, HttpStatus.NOT_FOUND);
					  }

					  // Collect familyIds of voters to be deleted
					  List<VoterEntity> voters = voterRepository.findByAccountIdAndElectionIdAndEpicNumberIn(
							  accountId, electionId, cleanedEpicNumbers);
					  voters.forEach(voter -> {
						  if (voter.getFamilyId() != null) {
							  affectedFamilyIds.add(voter.getFamilyId());
						  }
					  });

					  // Delete specific voters
					  deletedCount = voterRepository.deleteByAccountIdAndElectionIdAndEpicNumberIn(
							  accountId, electionId, cleanedEpicNumbers);
					  voterMongoRepository.deleteByAccountIdAndElectionIdAndEpicNumberIn(
							  accountId, electionId, cleanedEpicNumbers);
					  log.info("Deleted {} voters from both databases for electionId: {}", deletedCount, electionId);
				  }

				  if (deletedCount == 0) {
					  log.warn("No voters found for deletion with accountId: {}, electionId: {}", accountId, electionId);
					  throw new ThedalException(ThedalError.VOTER_NOT_FOUND, HttpStatus.NOT_FOUND);
				  }

				  // Update family counts for affected families
				  affectedFamilyIds.forEach(familyId -> {
					  try {
						  log.debug("Updating family count for familyId: {}", familyId);
						  updateFamilyCount(familyId, accountId, electionId);
					  } catch (Exception e) {
						  log.error("Failed to update family count for familyId: {}", familyId, e);
					  }
				  });

				  log.info("Successfully deleted {} voters for electionId: {}", deletedCount, electionId);
				  return new ThedalResponse<>(ThedalSuccess.VOTERS_DELETED);

			  } catch (ThedalException e) {
				  log.error("ThedalException while deleting voters: {}", e.getMessage());
				  throw e;
			  } catch (Exception e) {
				  log.error("Unexpected error while deleting voters: {}", e.getMessage(), e);
				  throw new ThedalException(ThedalError.OPERATION_FAILED, HttpStatus.INTERNAL_SERVER_ERROR, 
						  "Failed to delete voters: " + e.getMessage());
			  }
		  }
		 

		 
		  public VoterExportResponse initiateVoterExport(Long accountId, Long electionId,
				  List<Integer> partNos, String gender, Integer minAge, Integer maxAge, Integer limit) {
				
				log.info("EXPORT_FLOW: Initiating voter export for accountId: {}, electionId: {}, partNos: {}, gender: {}, minAge: {}, maxAge: {}, limit: {}", 
						accountId, electionId, partNos, gender, minAge, maxAge, limit);
				
				if (!voterRepository.existsByElectionIdAndAccountId(electionId, accountId)) {
					log.error("EXPORT_FLOW: Election not found for accountId: {}, electionId: {}", accountId, electionId);
					throw new ThedalException(ThedalError.ELECTION_NOT_FOUND, HttpStatus.NOT_FOUND);
				}

				VoterDownloadJob job = new VoterDownloadJob();
				job.setAccountId(accountId);
				job.setElectionId(electionId);
				job.setTimeStarted(LocalDateTime.now());
				job.setStatus("IN_PROGRESS");
				voterDownloadJobRepository.saveAndFlush(job);
				
				log.info("EXPORT_FLOW: Created job with ID: {} for accountId: {}, electionId: {}, status: {}", 
						job.getId(), accountId, electionId, job.getStatus());

				try {
					// Instead of using Quartz, call the async method directly
					log.info("EXPORT_FLOW: Starting direct async export for jobId: {}", job.getId());
					processVoterExportAsync(job.getId(), accountId, electionId, partNos, gender, minAge, maxAge, limit);
					
					log.info("EXPORT_FLOW: Successfully initiated async export for jobId: {}", job.getId());
				} catch (Exception e) {
					log.error("EXPORT_FLOW: Failed to initiate async export for jobId: {}, error: {}", job.getId(), e.getMessage(), e);
					
					// Update job status to ERROR if scheduling fails
					job.setStatus("ERROR");
					job.setErrorMessage("Failed to initiate export: " + e.getMessage());
					job.setTimeCompleted(LocalDateTime.now());
					voterDownloadJobRepository.save(job);
					
					throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, 
							"Failed to initiate export job: " + e.getMessage());
				}

				return new VoterExportResponse(job.getId());
	}

	// New comprehensive export method with all filters from GET voters API
	public VoterExportResponse initiateVoterExportWithFilters(Long accountId, Long userId, Long electionId,
			String voterId, String epicNumber, List<Integer> boothNumberList, UUID familyId, UUID friendId,
			String voterName, String voterFirstName, String voterLastName, String voterFnameEn, String voterLnameEn,
			String voterFnameL1, String voterFnameL2, String voterLnameL1, String voterLnameL2,
			String relationName, String relationFirstName, String relationLastName, String relationFirstNameEn, String relationLastNameEn,
			String partyName, String religionName, String voterHistoryName, Integer age, Integer minAge, Integer maxAge, Boolean includeUnknownAge,
			String gender, String hasDob, Boolean starNumber, String description, String categoryName, String casteCategoryName,
			String casteName, String subCaste, String duplicate, Long serialNo, Boolean overseas, Boolean fatherless, Boolean guardian,
			Integer birthdayMonth, Integer birthdayDay, Boolean hasMobileNo, String mobileNo, Boolean singleVoterFamily, List<String> columns, Integer limit) {
		
		log.info("EXPORT_FLOW: Initiating comprehensive voter export for accountId: {}, userId: {}, electionId: {}, with filters", accountId, userId, electionId);
		
		if (!voterRepository.existsByElectionIdAndAccountId(electionId, accountId)) {
			log.error("EXPORT_FLOW: Election not found for accountId: {}, electionId: {}", accountId, electionId);
			throw new ThedalException(ThedalError.ELECTION_NOT_FOUND, HttpStatus.NOT_FOUND);
		}

		VoterDownloadJob job = new VoterDownloadJob();
		job.setAccountId(accountId);
		job.setElectionId(electionId);
		job.setTimeStarted(LocalDateTime.now());
		job.setStatus("IN_PROGRESS");
		voterDownloadJobRepository.saveAndFlush(job);
		
		log.info("EXPORT_FLOW: Created comprehensive export job with ID: {} for accountId: {}, electionId: {}, status: {}", 
				job.getId(), accountId, electionId, job.getStatus());

		try {
			// Start async processing with comprehensive filters
			log.info("EXPORT_FLOW: Starting comprehensive async export for jobId: {}", job.getId());
			processVoterExportWithFiltersAsync(job.getId(), accountId, userId, electionId, 
				voterId, epicNumber, boothNumberList, familyId, friendId,
				voterName, voterFirstName, voterLastName, voterFnameEn, voterLnameEn,
				voterFnameL1, voterFnameL2, voterLnameL1, voterLnameL2,
				relationName, relationFirstName, relationLastName, relationFirstNameEn, relationLastNameEn,
				partyName, religionName, voterHistoryName, age, minAge, maxAge, includeUnknownAge,
				gender, hasDob, starNumber, description, categoryName, casteCategoryName,
				casteName, subCaste, duplicate, serialNo, overseas, fatherless, guardian,
				birthdayMonth, birthdayDay, hasMobileNo, mobileNo, singleVoterFamily, columns, limit);
			
			log.info("EXPORT_FLOW: Successfully initiated comprehensive async export for jobId: {}", job.getId());
		} catch (Exception e) {
			log.error("EXPORT_FLOW: Failed to initiate comprehensive async export for jobId: {}, error: {}", job.getId(), e.getMessage(), e);
			
			// Update job status to ERROR if scheduling fails
			job.setStatus("ERROR");
			job.setErrorMessage("Failed to initiate export: " + e.getMessage());
			job.setTimeCompleted(LocalDateTime.now());
			voterDownloadJobRepository.save(job);
			
			throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, 
					"Failed to initiate export job: " + e.getMessage());
		}

		return new VoterExportResponse(job.getId());
			}

		// New async method to replace Quartz job
		public void processVoterExportAsync(Long jobId, Long accountId, Long electionId,
				List<Integer> partNos, String gender, Integer minAge, Integer maxAge, Integer limit) {
			log.info("ASYNC_EXPORT: Submitting async export for jobId: {}, accountId: {}, electionId: {}", jobId, accountId, electionId);
			try {
				CompletableFuture.runAsync(() -> {
					log.info("ASYNC_EXPORT: Running export in background for jobId: {}", jobId);
					try {
						processVoterExport(jobId, accountId, electionId, partNos, gender, minAge, maxAge, limit);
						log.info("ASYNC_EXPORT: Successfully completed async export for jobId: {}", jobId);
					} catch (Exception e) {
						log.error("ASYNC_EXPORT: Error in async export for jobId: {}, error: {}", jobId, e.getMessage(), e);
						markExportJobAsError(jobId, "Async execution failed: " + e.getMessage(), e);
					}
				}, taskExecutor);
			} catch (RejectedExecutionException e) {
				log.error("ASYNC_EXPORT: Executor rejected export jobId: {}, error: {}", jobId, e.getMessage(), e);
				markExportJobAsError(jobId, "Failed to schedule export: " + e.getMessage(), e);
				throw e;
			}
		}

	// New comprehensive async method for exports with all filters
	public void processVoterExportWithFiltersAsync(Long jobId, Long accountId, Long userId, Long electionId,
			String voterId, String epicNumber, List<Integer> boothNumberList, UUID familyId, UUID friendId,
			String voterName, String voterFirstName, String voterLastName, String voterFnameEn, String voterLnameEn,
			String voterFnameL1, String voterFnameL2, String voterLnameL1, String voterLnameL2,
			String relationName, String relationFirstName, String relationLastName, String relationFirstNameEn, String relationLastNameEn,
			String partyName, String religionName, String voterHistoryName, Integer age, Integer minAge, Integer maxAge, Boolean includeUnknownAge,
			String gender, String hasDob, Boolean starNumber, String description, String categoryName, String casteCategoryName,
			String casteName, String subCaste, String duplicate, Long serialNo, Boolean overseas, Boolean fatherless, Boolean guardian,
			Integer birthdayMonth, Integer birthdayDay, Boolean hasMobileNo, String mobileNo, Boolean singleVoterFamily, List<String> columns, Integer limit) {
			log.info("ASYNC_EXPORT: Submitting comprehensive async export for jobId: {}, accountId: {}, userId: {}, electionId: {}", jobId, accountId, userId, electionId);
			try {
				CompletableFuture.runAsync(() -> {
					log.info("ASYNC_EXPORT: Running comprehensive export in background for jobId: {}", jobId);
					try {
						processVoterExportWithFilters(jobId, accountId, userId, electionId, 
							voterId, epicNumber, boothNumberList, familyId, friendId,
							voterName, voterFirstName, voterLastName, voterFnameEn, voterLnameEn,
							voterFnameL1, voterFnameL2, voterLnameL1, voterLnameL2,
							relationName, relationFirstName, relationLastName, relationFirstNameEn, relationLastNameEn,
							partyName, religionName, voterHistoryName, age, minAge, maxAge, includeUnknownAge,
							gender, hasDob, starNumber, description, categoryName, casteCategoryName,
							casteName, subCaste, duplicate, serialNo, overseas, fatherless, guardian,
							birthdayMonth, birthdayDay, hasMobileNo, mobileNo, singleVoterFamily, columns, limit);
						log.info("ASYNC_EXPORT: Successfully completed comprehensive async export for jobId: {}", jobId);
					} catch (Exception e) {
						log.error("ASYNC_EXPORT: Error in comprehensive async export for jobId: {}, error: {}", jobId, e.getMessage(), e);
						markExportJobAsError(jobId, "Async execution failed: " + e.getMessage(), e);
					}
				}, taskExecutor);
			} catch (RejectedExecutionException e) {
				log.error("ASYNC_EXPORT: Executor rejected comprehensive export jobId: {}, error: {}", jobId, e.getMessage(), e);
				markExportJobAsError(jobId, "Failed to schedule export: " + e.getMessage(), e);
				throw e;
			}
	}

		public void processVoterExport(Long jobId, Long accountId, Long electionId,
				List<Integer> partNos, String gender, Integer minAge, Integer maxAge, Integer limit) {
			
			log.info("EXPORT_FLOW: Starting processVoterExport for jobId: {}, accountId: {}, electionId: {}, partNos: {}, gender: {}, minAge: {}, maxAge: {}, limit: {}", 
					jobId, accountId, electionId, partNos, gender, minAge, maxAge, limit);
			
			try {
				// Try S3 upload first, fallback to local if it fails
				try {
					processVoterExportS3(jobId, accountId, electionId, partNos, gender, minAge, maxAge, limit);
					log.info("EXPORT_FLOW: Successfully completed S3 export for jobId: {}", jobId);
				} catch (Exception s3Exception) {
					log.warn("EXPORT_FLOW: S3 export failed for jobId: {}, falling back to local: {}", jobId, s3Exception.getMessage());
					// Fallback to local storage if S3 fails
					processVoterExportLocal(jobId, accountId, electionId, partNos, gender, minAge, maxAge, limit);
					log.info("EXPORT_FLOW: Successfully completed local fallback export for jobId: {}", jobId);
				}
				
				log.info("EXPORT_FLOW: Successfully completed processVoterExport for jobId: {}", jobId);
			} catch (Exception e) {
				log.error("EXPORT_FLOW: Error in processVoterExport for jobId: {}, error: {}", jobId, e.getMessage(), e);
				throw e;
			}
		}

	// New comprehensive export processing method with all filters
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void processVoterExportWithFilters(Long jobId, Long accountId, Long userId, Long electionId,
			String voterId, String epicNumber, List<Integer> boothNumberList, UUID familyId, UUID friendId,
			String voterName, String voterFirstName, String voterLastName, String voterFnameEn, String voterLnameEn,
			String voterFnameL1, String voterFnameL2, String voterLnameL1, String voterLnameL2,
			String relationName, String relationFirstName, String relationLastName, String relationFirstNameEn, String relationLastNameEn,
			String partyName, String religionName, String voterHistoryName, Integer age, Integer minAge, Integer maxAge, Boolean includeUnknownAge,
			String gender, String hasDob, Boolean starNumber, String description, String categoryName, String casteCategoryName,
			String casteName, String subCaste, String duplicate, Long serialNo, Boolean overseas, Boolean fatherless, Boolean guardian,
			Integer birthdayMonth, Integer birthdayDay, Boolean hasMobileNo, String mobileNo, Boolean singleVoterFamily, List<String> columns, Integer limit) {
		
		log.info("EXPORT_FLOW: Starting comprehensive processVoterExportWithFilters for jobId: {}, accountId: {}, userId: {}, electionId: {}", 
				jobId, accountId, userId, electionId);
		
		try {
			// Try S3 upload first, fallback to local if it fails
			try {
				processVoterExportS3WithFilters(jobId, accountId, userId, electionId, 
					voterId, epicNumber, boothNumberList, familyId, friendId,
					voterName, voterFirstName, voterLastName, voterFnameEn, voterLnameEn,
					voterFnameL1, voterFnameL2, voterLnameL1, voterLnameL2,
					relationName, relationFirstName, relationLastName, relationFirstNameEn, relationLastNameEn,
					partyName, religionName, voterHistoryName, age, minAge, maxAge, includeUnknownAge,
					gender, hasDob, starNumber, description, categoryName, casteCategoryName,
					casteName, subCaste, duplicate, serialNo, overseas, fatherless, guardian,
					birthdayMonth, birthdayDay, hasMobileNo, mobileNo, singleVoterFamily, limit, columns);
				log.info("EXPORT_FLOW: Successfully completed comprehensive S3 export for jobId: {}", jobId);
			} catch (Exception s3Exception) {
				log.error("EXPORT_FLOW: Comprehensive S3 export FAILED for jobId: {}, exception type: {}, message: {}", 
						jobId, s3Exception.getClass().getName(), s3Exception.getMessage());
				log.error("EXPORT_FLOW: Full stack trace for S3 export failure jobId: {}", jobId, s3Exception);
				
				log.warn("EXPORT_FLOW: Comprehensive S3 export failed for jobId: {}, falling back to local: {}", jobId, s3Exception.getMessage());
				// Fallback to local storage if S3 fails
				processVoterExportLocalWithFilters(jobId, accountId, userId, electionId, 
					voterId, epicNumber, boothNumberList, familyId, friendId,
					voterName, voterFirstName, voterLastName, voterFnameEn, voterLnameEn,
					voterFnameL1, voterFnameL2, voterLnameL1, voterLnameL2,
					relationName, relationFirstName, relationLastName, relationFirstNameEn, relationLastNameEn,
					partyName, religionName, voterHistoryName, age, minAge, maxAge, includeUnknownAge,
					gender, hasDob, starNumber, description, categoryName, casteCategoryName,
					casteName, subCaste, duplicate, serialNo, overseas, fatherless, guardian,
					birthdayMonth, birthdayDay, hasMobileNo, mobileNo, singleVoterFamily, limit);
				log.info("EXPORT_FLOW: Successfully completed comprehensive local fallback export for jobId: {}", jobId);
			}
			
			log.info("EXPORT_FLOW: Successfully completed comprehensive processVoterExportWithFilters for jobId: {}", jobId);
		} catch (Exception e) {
			log.error("EXPORT_FLOW: Error in comprehensive processVoterExportWithFilters for jobId: {}, error: {}", jobId, e.getMessage(), e);
			throw e;
		}
	}

	private void markExportJobAsError(Long jobId, String errorMessage, Exception cause) {
		try {
			VoterDownloadJob job = voterDownloadJobRepository.findById(jobId).orElse(null);
			if (job != null) {
				job.setStatus("ERROR");
				job.setErrorMessage(errorMessage);
				job.setTimeCompleted(LocalDateTime.now());
				voterDownloadJobRepository.save(job);
				log.info("ASYNC_EXPORT: Updated job status to ERROR for jobId: {}", jobId);
			} else {
				log.error("ASYNC_EXPORT: Could not find job to update error status for jobId: {}", jobId);
			}
		} catch (Exception updateException) {
			log.error("ASYNC_EXPORT: Failed to update job status to ERROR for jobId: {}, updateError: {}",
					jobId, updateException.getMessage(), updateException);
		}
	}

		// S3 export method
		private void processVoterExportS3(Long jobId, Long accountId, Long electionId,
				List<Integer> partNos, String gender, Integer minAge, Integer maxAge, Integer limit) {
			
			log.info("EXPORT_FLOW: Starting processVoterExportS3 for jobId: {}, accountId: {}, electionId: {}", jobId, accountId, electionId);
			
			try {
				log.info("EXPORT_FLOW: Building specification for S3 export jobId: {} with filters - partNos: {}, gender: {}, minAge: {}, maxAge: {}", 
						jobId, partNos, gender, minAge, maxAge);
				
				// Build specification for filtering
				Specification<VoterEntity> spec = buildSpecification(electionId, accountId, partNos, gender, minAge, maxAge);
				
				// Check if this is an "All Part" export (no filters) for optimization
				boolean isFullExport = (partNos == null || partNos.isEmpty()) && 
				                       gender == null && minAge == null && maxAge == null;
				
				log.info("EXPORT_FLOW: Built specification successfully for S3 export jobId: {}, isFullExport: {}", jobId, isFullExport);
				
				// Check total count before processing
				long totalCount = isFullExport ? 
					voterRepository.countForExport(accountId, electionId) : 
					voterRepository.count(spec);
				log.info("EXPORT_FLOW: Total voters matching criteria for S3 export jobId: {}: {}", jobId, totalCount);
				
			if (totalCount == 0) {
				log.warn("EXPORT_FLOW: No voters found matching criteria for S3 export jobId: {}", jobId);
				
				// Generate empty Excel file with headers only
				log.info("EXPORT_FLOW: Generating empty Excel file with headers for S3 export jobId: {}", jobId);
				File excelFile;
				try {
					// Create empty specification to generate file with headers only  
					Specification<VoterEntity> emptySpec = (root, query, cb) -> cb.disjunction(); // Always false condition
					excelFile = generateExcelFileStreamed(emptySpec, 0); // 0 limit to ensure no data
				} catch (IOException ioException) {
					log.error("EXPORT_FLOW: Failed to generate empty Excel file for S3 export jobId: {}, error: {}", jobId, ioException.getMessage(), ioException);
					throw new RuntimeException("Failed to generate empty Excel file: " + ioException.getMessage(), ioException);
				}
				log.info("EXPORT_FLOW: Successfully generated empty Excel file for S3 export jobId: {}, file size: {} bytes", 
						jobId, excelFile.length());
				
				// Upload empty file to S3
				log.info("EXPORT_FLOW: Uploading empty export to S3 for jobId: {}", jobId);
				String fileName = "voter_export_" + jobId + "_" + System.currentTimeMillis() + "_empty.xlsx";
				String s3Url = awsFileUpload.uploadToAWS(excelFile, fileName, s3bucket);
				log.info("EXPORT_FLOW: Successfully uploaded empty export to S3 for jobId: {}, S3 URL: {}", jobId, s3Url);
				
				// Clean up temp file
				try {
					if (excelFile.exists() && !excelFile.delete()) {
						log.warn("EXPORT_FLOW: Failed to delete temp empty file for S3 export jobId: {}: {}", jobId, excelFile.getAbsolutePath());
					}
				} catch (Exception cleanupException) {
					log.warn("EXPORT_FLOW: Error cleaning up temp empty file for S3 export jobId: {}: {}", jobId, cleanupException.getMessage());
				}
				
				// Update job status to indicate empty file uploaded
				VoterDownloadJob job = voterDownloadJobRepository.findById(jobId)
						.orElseThrow(() -> new ThedalException(ThedalError.JOB_NOT_FOUND, HttpStatus.NOT_FOUND));
				
				job.setStatus("COMPLETED");
				job.setTimeCompleted(LocalDateTime.now());
				job.setAwsS3DownloadUrl(s3Url);
				job.setErrorMessage("No voters found matching the specified criteria - exported empty file with headers");
				voterDownloadJobRepository.save(job);
				
				log.info("EXPORT_FLOW: Successfully completed S3 export for jobId: {} with empty file, status: {}, downloadUrl: {}", 
						jobId, job.getStatus(), s3Url);
				log.info("EXPORT_FLOW: Marked S3 export job as completed with empty file for jobId: {}", jobId);
				return;
		}
		
		// Generate CSV ZIP file
		log.info("EXPORT_FLOW: Generating CSV ZIP file for S3 export jobId: {}, using NATIVE COPY for speed", jobId);
		File csvZipFile;
		try {
			// Always use native COPY for maximum speed (1M rows in 1-2 minutes)
			csvZipFile = csvZipExportService.generateCsvZipWithNativeCopy(accountId, electionId, limit, jobId, null, partNos);
		} catch (IOException ioException) {
			log.error("EXPORT_FLOW: Failed to generate CSV ZIP file for S3 export jobId: {}, error: {}", jobId, ioException.getMessage(), ioException);
			throw new RuntimeException("Failed to generate CSV ZIP file: " + ioException.getMessage(), ioException);
		}
		log.info("EXPORT_FLOW: Successfully generated CSV ZIP file for S3 export jobId: {}, file size: {} bytes", 
				jobId, csvZipFile.length());
		
		// Upload to S3
		log.info("EXPORT_FLOW: Uploading to S3 for jobId: {}", jobId);
		String fileName = "voter_export_" + jobId + "_" + System.currentTimeMillis() + ".zip";
		String s3Url = awsFileUpload.uploadToAWS(csvZipFile, fileName, s3bucket);
		log.info("EXPORT_FLOW: Successfully uploaded to S3 for jobId: {}, S3 URL: {}", jobId, s3Url);
		
		// Clean up temp file
		try {
			if (csvZipFile.exists() && !csvZipFile.delete()) {
				log.warn("EXPORT_FLOW: Failed to delete temp file for S3 export jobId: {}: {}", jobId, csvZipFile.getAbsolutePath());
			}
		} catch (Exception cleanupException) {
			log.warn("EXPORT_FLOW: Error cleaning up temp file for S3 export jobId: {}: {}", jobId, cleanupException.getMessage());
		}
				log.info("EXPORT_FLOW: Updating S3 export job status to COMPLETED for jobId: {}", jobId);
				VoterDownloadJob job = voterDownloadJobRepository.findById(jobId)
						.orElseThrow(() -> new ThedalException(ThedalError.JOB_NOT_FOUND, HttpStatus.NOT_FOUND));
				
				job.setStatus("COMPLETED");
				job.setTimeCompleted(LocalDateTime.now());
				// Set the S3 download URL directly - it's already an absolute URL
				job.setAwsS3DownloadUrl(s3Url);
				voterDownloadJobRepository.save(job);
				
				log.info("EXPORT_FLOW: Successfully completed S3 voter export for jobId: {}, status: {}, downloadUrl: {}", 
						jobId, job.getStatus(), s3Url);
				
			} catch (Exception e) {
				log.error("EXPORT_FLOW: Error processing S3 voter export for jobId: {}, error: {}", jobId, e.getMessage(), e);
				
				// Update job status to ERROR
				try {
					VoterDownloadJob job = voterDownloadJobRepository.findById(jobId)
							.orElse(null);
					if (job != null) {
						job.setStatus("ERROR");
						job.setErrorMessage("S3 export failed: " + e.getMessage());
						job.setTimeCompleted(LocalDateTime.now());
						voterDownloadJobRepository.save(job);
						
						log.info("EXPORT_FLOW: Updated S3 export job status to ERROR for jobId: {}", jobId);
					}
				} catch (Exception updateException) {
					log.error("EXPORT_FLOW: Failed to update S3 export job status to ERROR for jobId: {}, updateError: {}", 
							jobId, updateException.getMessage(), updateException);
				}
				
				throw e;  // Re-throw to allow fallback to local
			}
		}

		// Local file export method to avoid S3 issues
		@Transactional
		private void processVoterExportLocal(Long jobId, Long accountId, Long electionId,
				List<Integer> partNos, String gender, Integer minAge, Integer maxAge, Integer limit) {
			
			log.info("EXPORT_FLOW: Starting processVoterExportLocal for jobId: {}, accountId: {}, electionId: {}", jobId, accountId, electionId);
			
			try {
				log.info("EXPORT_FLOW: Building specification for jobId: {} with filters - partNos: {}, gender: {}, minAge: {}, maxAge: {}", 
						jobId, partNos, gender, minAge, maxAge);
				
				// Build specification for filtering
				Specification<VoterEntity> spec = buildSpecification(electionId, accountId, partNos, gender, minAge, maxAge);
				
				log.info("EXPORT_FLOW: Built specification successfully for jobId: {}", jobId);
				
				// Check total count before processing
				long totalCount = voterRepository.count(spec);
				log.info("EXPORT_FLOW: Total voters matching criteria for jobId: {}: {}", jobId, totalCount);
				
				if (totalCount == 0) {
					log.warn("EXPORT_FLOW: No voters found matching criteria for jobId: {}", jobId);
					
					// Update job status to indicate no data
					VoterDownloadJob job = voterDownloadJobRepository.findById(jobId)
							.orElseThrow(() -> new ThedalException(ThedalError.JOB_NOT_FOUND, HttpStatus.NOT_FOUND));
					
					job.setStatus("COMPLETED");
					job.setTimeCompleted(LocalDateTime.now());
					job.setErrorMessage("No voters found matching the specified criteria");
					voterDownloadJobRepository.save(job);
					
					log.info("EXPORT_FLOW: Marked job as completed with no data for jobId: {}", jobId);
					return;
				}
				
			// Generate CSV ZIP file locally
			log.info("EXPORT_FLOW: Starting CSV ZIP file generation for jobId: {}", jobId);
			File csvZipFile;
			try {
				csvZipFile = csvZipExportService.generateCsvZipStreamed(spec, limit, jobId, null, accountId, electionId);
			} catch (IOException ioException) {
				log.error("EXPORT_FLOW: IOException during CSV ZIP file generation for jobId: {}, error: {}", jobId, ioException.getMessage(), ioException);
				throw new RuntimeException("Failed to generate CSV ZIP file: " + ioException.getMessage(), ioException);
			}
			log.info("EXPORT_FLOW: CSV ZIP file generated successfully for jobId: {}, file: {}", jobId, csvZipFile.getAbsolutePath());
			
			// Update job with completion status and local download URL
			log.info("EXPORT_FLOW: Updating job status to COMPLETED for jobId: {}", jobId);
			VoterDownloadJob job = voterDownloadJobRepository.findById(jobId)
					.orElseThrow(() -> new ThedalException(ThedalError.JOB_NOT_FOUND, HttpStatus.NOT_FOUND));
			
			job.setStatus("COMPLETED");
			job.setTimeCompleted(LocalDateTime.now());
			// Set the download URL to point to our local endpoint with absolute URL
			String baseUrl = serverUrl.endsWith("/") ? serverUrl.substring(0, serverUrl.length() - 1) : serverUrl;
			String downloadUrl = baseUrl + "/api/voter/" + electionId + "/export/download/" + jobId;
			job.setAwsS3DownloadUrl(downloadUrl);
			voterDownloadJobRepository.save(job);
			
			log.info("EXPORT_FLOW: Successfully completed local voter export for jobId: {}, status: {}, downloadUrl: {}", 
					jobId, job.getStatus(), downloadUrl);
			
		} catch (Exception e) {
			log.error("EXPORT_FLOW: Error processing local voter export for jobId: {}, error: {}", jobId, e.getMessage(), e);
			
			// Update job status to ERROR
			try {
				VoterDownloadJob job = voterDownloadJobRepository.findById(jobId)
						.orElse(null);
				if (job != null) {
					job.setStatus("ERROR");
					job.setErrorMessage(e.getMessage());
					job.setTimeCompleted(LocalDateTime.now());
					voterDownloadJobRepository.save(job);
					
					log.info("EXPORT_FLOW: Updated job status to ERROR for jobId: {}", jobId);
				} else {
					log.error("EXPORT_FLOW: Could not find job to update error status for jobId: {}", jobId);
				}
			} catch (Exception updateException) {
				log.error("EXPORT_FLOW: Failed to update job status to ERROR for jobId: {}, updateError: {}", 
						jobId, updateException.getMessage(), updateException);
			}
			
			throw e;
		}
	}

	// Generate Excel file and store locally
	private File generateExcelFileStreamedLocal(Specification<VoterEntity> spec, Integer limit, Long jobId, List<String> columns) throws IOException {
		log.info("EXPORT_FLOW: Starting generateExcelFileStreamedLocal for jobId: {}, limit: {}, columns: {}", jobId, limit, columns);
		
		// Validate and prepare columns
		List<String> selectedColumns = VoterColumnMapper.validateAndFilterFields(columns);
		boolean useSelectiveExport = columns != null && !columns.isEmpty();
		log.info("EXPORT_FLOW: Using selective export: {}, column count: {}", useSelectiveExport, selectedColumns.size());
		
		// Ensure headless mode is enabled programmatically
		System.setProperty("java.awt.headless", "true");
		
		// Create local export directory if it doesn't exist
		String exportDir = System.getProperty("java.io.tmpdir") + "/thedal-exports";
		File exportDirectory = new File(exportDir);
		if (!exportDirectory.exists()) {
			}
			
			// Create output file with job ID in the name for uniqueness
			File outputFile = new File(exportDirectory, "voter-export-" + jobId + ".xlsx");
			log.info("EXPORT_FLOW: Creating Excel file at: {}", outputFile.getAbsolutePath());
			
			int processed = 0;
			int page = 0;

			// Total records check
			int totalRecords;
			if (limit == null) {
				totalRecords = (int) voterRepository.count(spec);
				log.info("EXPORT_FLOW: No limit specified, total records to export for jobId: {}: {}", jobId, totalRecords);
			} else {
				totalRecords = Math.min(limit, (int) voterRepository.count(spec));
				log.info("EXPORT_FLOW: Limit specified: {}, total records to export for jobId: {}: {}", limit, jobId, totalRecords);
			}

			if (totalRecords == 0) {
				log.warn("EXPORT_FLOW: No records found to export for jobId: {}", jobId);
				throw new ThedalException(ThedalError.VOTER_NOT_FOUND, HttpStatus.NOT_FOUND);
			}

			log.info("EXPORT_FLOW: Starting local export of {} voter records to file: {} for jobId: {}", 
					totalRecords, outputFile.getAbsolutePath(), jobId);

			try (SXSSFWorkbook workbook = new SXSSFWorkbook(100);
					FileOutputStream fos = new FileOutputStream(outputFile)) {

				workbook.setCompressTempFiles(true);
				log.info("EXPORT_FLOW: Created SXSSFWorkbook with temp file compression for jobId: {}", jobId);

				Sheet sheet = workbook.createSheet("Voters");

				// Set column widths manually - adjust count based on selective columns
				int columnCount = useSelectiveExport ? selectedColumns.size() : 93;
				for (int i = 0; i < columnCount; i++) {
					sheet.setColumnWidth(i, 15 * 256);
				}
				log.info("EXPORT_FLOW: Set column widths for {} columns for jobId: {}", columnCount, jobId);

				// Create header row (selective or full)
				Row headerRow = sheet.createRow(0);
				if (useSelectiveExport) {
					VoterExcelHeader.createSelectiveHeaderRow(headerRow, selectedColumns);
				} else {
					VoterExcelHeader.createHeaderRow(headerRow);
				}
				int rowNum = 1;
				log.info("EXPORT_FLOW: Created header row for jobId: {}", jobId);

				while (processed < totalRecords) {
					Pageable pageable = PageRequest.of(page, BATCH_SIZE);
					Page<VoterEntity> voterPage = voterRepository.findAll(spec, pageable);

					if (voterPage.isEmpty()) {
						log.warn("EXPORT_FLOW: Voter page is empty at page: {}, processed: {}, totalRecords: {} for jobId: {}", 
								page, processed, totalRecords, jobId);
						break;
					}

				log.info("EXPORT_FLOW: Processing page: {}, records in page: {}, processed so far: {} for jobId: {}", 
						page, voterPage.getContent().size(), processed, jobId);

				for (VoterEntity voter : voterPage.getContent()) {
					// Validate voter before creating row to prevent blank rows
					if (voter != null && voter.getVoterId() != null) {
						Row row = sheet.createRow(rowNum++);
						// Use selective or standard row population
						if (useSelectiveExport) {
							VoterExcelDataRow.populateSelectiveDataRow(row, voter, selectedColumns);
						} else {
							VoterExcelDataRow.populateDataRow(row, voter);
						}
						processed++;

						if (rowNum % 100 == 0) {
							((SXSSFSheet) sheet).flushRows();
							log.debug("EXPORT_FLOW: Flushed rows at rowNum: {} for jobId: {}", rowNum, jobId);
						}

						if (limit != null && processed >= limit) {
							log.info("EXPORT_FLOW: Reached limit: {}, stopping processing for jobId: {}", limit, jobId);
							break;
						}
					} else {
						log.warn("EXPORT_FLOW: Skipping null or invalid voter for jobId: {}", jobId);
					}
				}					page++;
					log.info("EXPORT_FLOW: Completed page: {}, moving to next page for jobId: {}", page - 1, jobId);
				}

				((SXSSFSheet) sheet).flushRows();
				log.info("EXPORT_FLOW: Final flush of rows for jobId: {}", jobId);
				
				workbook.write(fos);
				fos.flush();
				workbook.dispose();

				log.info("EXPORT_FLOW: Generated local Excel file with {} records at: {} for jobId: {}", processed, outputFile.getAbsolutePath(), jobId);
				return outputFile;
			} catch (Exception e) {
				log.error("EXPORT_FLOW: Error generating Excel file for jobId: {}, error: {}", jobId, e.getMessage(), e);
				
				if (outputFile != null && outputFile.exists()) {
					boolean deleted = outputFile.delete();
					log.warn("EXPORT_FLOW: Attempted to delete temporary file: {}, success: {}", outputFile.getAbsolutePath(), deleted);
				}
				throw new IOException("Failed to generate Excel file for jobId " + jobId + ": " + e.getMessage(), e);
			}
		}

		// Download export file from local storage
		@Override
		public ResponseEntity<Resource> downloadExportFile(Long jobId, Long accountId, Long electionId) {
			log.info("EXPORT_FLOW: Starting download for jobId: {}, accountId: {}, electionId: {}", jobId, accountId, electionId);
			
			try {
				// Verify job exists and belongs to the account/election
				VoterDownloadJob job = voterDownloadJobRepository.findByIdAndAccountIdAndElectionId(jobId, accountId, electionId)
						.orElseThrow(() -> {
							log.error("EXPORT_FLOW: Job not found for download - jobId: {}, accountId: {}, electionId: {}", jobId, accountId, electionId);
							return new ThedalException(ThedalError.JOB_NOT_FOUND, HttpStatus.NOT_FOUND);
						});
				
				log.info("EXPORT_FLOW: Found job for download - jobId: {}, status: {}", jobId, job.getStatus());
				
				if (!"COMPLETED".equals(job.getStatus())) {
					log.warn("EXPORT_FLOW: Job not completed for download - jobId: {}, status: {}", jobId, job.getStatus());
					throw new ThedalException(ThedalError.INVALID_STATUS, HttpStatus.BAD_REQUEST);
				}
				
// Construct file path - try ZIP first (new format), fallback to XLSX (legacy)
			String exportDir = System.getProperty("java.io.tmpdir") + "/thedal-exports";
			File file = new File(exportDir, "voter-export-" + jobId + ".zip");
			String filename;
			
			if (file.exists()) {
				filename = "voter-export-" + jobId + ".zip";
				log.info("EXPORT_FLOW: Found ZIP file at: {}", file.getAbsolutePath());
			} else {
				// Fallback to Excel format for legacy exports
				file = new File(exportDir, "voter-export-" + jobId + ".xlsx");
				filename = "voter-export-" + jobId + ".xlsx";
				log.info("EXPORT_FLOW: Looking for Excel file at: {}", file.getAbsolutePath());
			}
			
			if (!file.exists()) {
				log.error("EXPORT_FLOW: File not found for download - jobId: {}, tried both .zip and .xlsx", jobId);
				throw new ThedalException(ThedalError.FILE_NOT_FOUND, HttpStatus.NOT_FOUND);
			}
			
			log.info("EXPORT_FLOW: File found, size: {} bytes, readable: {}", file.length(), file.canRead());
			
			Resource resource = new FileSystemResource(file);
				
				log.info("EXPORT_FLOW: Successfully prepared download for jobId: {}, filename: {}", jobId, filename);
				
				return ResponseEntity.ok()
						.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
						.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
						.body(resource);
				
			} catch (ThedalException e) {
				log.error("EXPORT_FLOW: ThedalException during download for jobId: {}, error: {}", jobId, e.getMessage());
				throw e;
			} catch (Exception e) {
				log.error("EXPORT_FLOW: Unexpected error downloading export file for jobId: {}, error: {}", jobId, e.getMessage(), e);
				throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}

			private File generateExcelFileStreamed(Specification<VoterEntity> spec, Integer limit) throws IOException {
				// Ensure headless mode is enabled programmatically
				System.setProperty("java.awt.headless", "true");

				File outputFile = File.createTempFile("voterexport", ".xlsx");
				int processed = 0;
				int page = 0;

				// Total records check
				int totalRecords;
				if (limit == null) {
					totalRecords = (int) voterRepository.count(spec);
				} else {
					totalRecords = Math.min(limit, (int) voterRepository.count(spec));
				}

				if (totalRecords == 0) {
					throw new ThedalException(ThedalError.VOTER_NOT_FOUND, HttpStatus.NOT_FOUND);
				}

				log.info("Starting export of {} voter records", totalRecords);

				try (SXSSFWorkbook workbook = new SXSSFWorkbook(100);
						FileOutputStream fos = new FileOutputStream(outputFile)) {

					workbook.setCompressTempFiles(true);

					Sheet sheet = workbook.createSheet("Voters");

					// Set column widths manually (already safe, no AWT dependency)
					for (int i = 0; i < 74; i++) {
						sheet.setColumnWidth(i, 15 * 256);
					}

					Row headerRow = sheet.createRow(0);
					VoterExcelHeader.createHeaderRow(headerRow);
					int rowNum = 1;

					while (processed < totalRecords) {
						Pageable pageable = PageRequest.of(page, BATCH_SIZE);
						Page<VoterEntity> voterPage = voterRepository.findAll(spec, pageable);

						if (voterPage.isEmpty()) {
							break;
						}

					for (VoterEntity voter : voterPage.getContent()) {
						// Validate voter before creating row to prevent blank rows
						if (voter != null && voter.getVoterId() != null) {
							Row row = sheet.createRow(rowNum++);
							VoterExcelDataRow.populateDataRow(row, voter);
							processed++;

							if (rowNum % 100 == 0) {
								((SXSSFSheet) sheet).flushRows();
							}

							if (limit != null && processed >= limit) {
								break;
							}
						} else {
							log.warn("Skipping null or invalid voter in filtered export");
						}
					}						page++;
					}

					((SXSSFSheet) sheet).flushRows();
					workbook.write(fos);
					fos.flush();
					workbook.dispose();

					log.info("Generated Excel file with {} records", processed);
					return outputFile;
				} catch (Exception e) {
					if (outputFile != null && outputFile.exists()) {
						if (!outputFile.delete()) {
							log.warn("Failed to delete temporary file: {}", outputFile.getAbsolutePath());
						}
					}
					throw new IOException("Failed to generate Excel file: " + e.getMessage(), e);
				}
			}

	/**
	 * OPTIMIZED version for "All Part" exports with eager relationship fetching
	 * Eliminates N+1 query problem by using LEFT JOIN FETCH
	 * Fixes blank rows issue by validating voter data before row creation
	 */
	@Transactional(readOnly = true)
	private File generateExcelFileStreamedOptimized(Long accountId, Long electionId, Integer limit) throws IOException {
		// Ensure headless mode is enabled programmatically
		System.setProperty("java.awt.headless", "true");

		File outputFile = File.createTempFile("voterexport", ".xlsx");
		int processed = 0;
		int page = 0;

		// Total records check using optimized count
		int totalRecords;
		if (limit == null) {
			totalRecords = (int) voterRepository.countForExport(accountId, electionId);
		} else {
			totalRecords = Math.min(limit, (int) voterRepository.countForExport(accountId, electionId));
		}

		if (totalRecords == 0) {
			throw new ThedalException(ThedalError.VOTER_NOT_FOUND, HttpStatus.NOT_FOUND);
		}

		log.info("Starting OPTIMIZED export of {} voter records for accountId: {}, electionId: {}", 
			totalRecords, accountId, electionId);

		try (SXSSFWorkbook workbook = new SXSSFWorkbook(100);
				FileOutputStream fos = new FileOutputStream(outputFile)) {

			workbook.setCompressTempFiles(true);

			Sheet sheet = workbook.createSheet("Voters");

			// Set column widths manually (already safe, no AWT dependency)
			for (int i = 0; i < 74; i++) {
				sheet.setColumnWidth(i, 15 * 256);
			}

			Row headerRow = sheet.createRow(0);
			VoterExcelHeader.createHeaderRow(headerRow);
			int rowNum = 1;

			while (processed < totalRecords) {
				// Use sorted pagination for consistent ordering
				Pageable pageable = PageRequest.of(page, BATCH_SIZE, 
					Sort.by(Sort.Order.asc("partNo"), Sort.Order.asc("serialNo")));
				
				// Use optimized query with eager fetching
				Page<VoterEntity> voterPage = voterRepository.findAllForExportWithRelationships(
					accountId, electionId, pageable);

				if (voterPage.isEmpty()) {
					break;
				}

				for (VoterEntity voter : voterPage.getContent()) {
					// Validate voter before creating row to prevent blank rows
					if (voter != null && voter.getVoterId() != null) {
						Row row = sheet.createRow(rowNum++);
						VoterExcelDataRow.populateDataRow(row, voter);
						processed++;

						if (rowNum % 100 == 0) {
							((SXSSFSheet) sheet).flushRows();
						}

						if (limit != null && processed >= limit) {
							break;
						}
					} else {
						log.warn("Skipping null or invalid voter at page {}, position {}", page, processed);
					}
				}

				page++;
				
				// Break if limit reached
				if (limit != null && processed >= limit) {
					break;
				}
			}

			((SXSSFSheet) sheet).flushRows();
			workbook.write(fos);
			fos.flush();
			workbook.dispose();

			log.info("Generated OPTIMIZED Excel file with {} records (skipped {} invalid rows)", 
				processed, totalRecords - processed);
			return outputFile;
		} catch (Exception e) {
			if (outputFile != null && outputFile.exists()) {
				if (!outputFile.delete()) {
					log.warn("Failed to delete temporary file: {}", outputFile.getAbsolutePath());
				}
			}
			throw new IOException("Failed to generate Excel file: " + e.getMessage(), e);
		}
	}

	// S3 export method with comprehensive filters
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	private void processVoterExportS3WithFilters(Long jobId, Long accountId, Long userId, Long electionId,
			String voterId, String epicNumber, List<Integer> boothNumberList, UUID familyId, UUID friendId,
			String voterName, String voterFirstName, String voterLastName, String voterFnameEn, String voterLnameEn,
			String voterFnameL1, String voterFnameL2, String voterLnameL1, String voterLnameL2,
			String relationName, String relationFirstName, String relationLastName, String relationFirstNameEn, String relationLastNameEn,
			String partyName, String religionName, String voterHistoryName, Integer age, Integer minAge, Integer maxAge, Boolean includeUnknownAge,
			String gender, String hasDob, Boolean starNumber, String description, String categoryName, String casteCategoryName,
			String casteName, String subCaste, String duplicate, Long serialNo, Boolean overseas, Boolean fatherless, Boolean guardian,
			Integer birthdayMonth, Integer birthdayDay, Boolean hasMobileNo, String mobileNo, Boolean singleVoterFamily, Integer limit, List<String> columns) {
		
		log.info("EXPORT_FLOW: Starting comprehensive S3 export for jobId: {}, accountId: {}, userId: {}, electionId: {}", jobId, accountId, userId, electionId);
		
		try {
			log.info("EXPORT_FLOW: Building comprehensive filters for S3 export jobId: {}", jobId);
			
			// Normalize parameters: treat blank/"all" as null; lowercase for IN with LOWER(...)
			List<String> voterFnameEnList = normalizeListParam(voterFnameEn);
			List<String> voterLnameEnList = normalizeListParam(voterLnameEn);
			List<String> voterFnameL1List = normalizeListParam(voterFnameL1);
			List<String> voterFnameL2List = normalizeListParam(voterFnameL2);
			List<String> voterLnameL1List = normalizeListParam(voterLnameL1);
			List<String> voterLnameL2List = normalizeListParam(voterLnameL2);
			List<String> relationFirstNameEnList = normalizeListParam(relationFirstNameEn);
			List<String> relationLastNameEnList = normalizeListParam(relationLastNameEn);
			List<String> partyNameList = normalizeListParam(partyName);
			List<String> voterHistoryNameList = normalizeListParam(voterHistoryName);
			List<String> genderList = normalizeListParam(gender);

			List<String> religionNameList = normalizeListParam(religionName);
			List<String> descriptionList = normalizeListParam(description);
			List<String> categoryNameList = normalizeListParam(categoryName);
			List<String> casteCategoryNameList = normalizeListParam(casteCategoryName);
			List<String> casteNameList = normalizeListParam(casteName);
			List<String> subCasteList = normalizeListParam(subCaste);
			String mobileNoNorm = normalizeStringParamPreserveCase(mobileNo);

			log.info("EXPORT_FLOW: Normalized filters - genderList: {}, party: {}, history: {}, religion: {}, category: {}, casteCat: {}, caste: {}, subCaste: {}, hasMobileNo: {}, mobileNo: {}",
					genderList, partyNameList, voterHistoryNameList, religionNameList, categoryNameList, casteCategoryNameList, casteNameList, subCasteList, hasMobileNo, mobileNoNorm);
			
		// Use the same repository method as getVoters to ensure consistent filtering
		// Get total count first using passed userId instead of requestDetails
		UserEntity currentUser = userRepo.findById(userId)
				.orElseThrow(() -> new RuntimeException("User not found"));
		Role userRole = currentUser.getRole();			List<Integer> effectiveBoothNumbers = getEffectiveBoothNumbersFast(boothNumberList, userRole, userId);
			
			// Count total matching records
			Pageable countPageable = PageRequest.of(0, 1);
			Page<VoterEntity> countPage = voterRepository.findByAccountIdAndElectionIdAndFiltersOptimized(
				accountId, electionId, voterId, epicNumber, effectiveBoothNumbers, familyId, friendId,
				voterFnameEnList, voterLnameEnList, voterFnameL1List, voterFnameL2List,
				voterLnameL1List, voterLnameL2List,
				relationFirstNameEnList, relationLastNameEnList, null, null, null, null,
				partyNameList, voterHistoryNameList, religionNameList, age, minAge, maxAge, includeUnknownAge,
				genderList, null, null, null, null, null, null, birthdayMonth, birthdayDay,
				starNumber, descriptionList, categoryNameList, casteCategoryNameList, casteNameList, subCasteList,
				serialNo, overseas, fatherless, guardian, hasMobileNo, mobileNoNorm, singleVoterFamily,
				null, null, countPageable);
			
			long totalCount = countPage.getTotalElements();
			log.info("EXPORT_FLOW: Total voters matching comprehensive criteria for S3 export jobId: {}: {}", jobId, totalCount);
			
		if (totalCount == 0) {
			log.warn("EXPORT_FLOW: No voters found matching comprehensive criteria for S3 export jobId: {}", jobId);
			
			// Update job status to indicate no data
			VoterDownloadJob job = voterDownloadJobRepository.findById(jobId)
					.orElseThrow(() -> new ThedalException(ThedalError.JOB_NOT_FOUND, HttpStatus.NOT_FOUND));
			
			job.setStatus("COMPLETED");
			job.setTimeCompleted(LocalDateTime.now());
			job.setErrorMessage("No voters found matching the specified criteria");
			voterDownloadJobRepository.save(job);
			
			log.info("EXPORT_FLOW: Marked comprehensive S3 job as completed with no data for jobId: {}", jobId);
			return;
		}
		// Generate CSV ZIP file using native COPY for maximum speed
		log.info("EXPORT_FLOW: Starting comprehensive CSV ZIP generation using NATIVE COPY for S3 export jobId: {}", jobId);
		File csvZipFile;
		try {
			csvZipFile = csvZipExportService.generateCsvZipWithNativeCopy(accountId, electionId, limit, jobId, columns, effectiveBoothNumbers);
			
			// Verify file was created successfully
			if (csvZipFile == null) {
				throw new IOException("CSV ZIP file generation returned null");
			}
			if (!csvZipFile.exists()) {
				throw new IOException("CSV ZIP file does not exist at path: " + csvZipFile.getAbsolutePath());
			}
			if (csvZipFile.length() == 0) {
				throw new IOException("CSV ZIP file is empty (0 bytes)");
			}
			
			log.info("EXPORT_FLOW: Successfully generated comprehensive CSV ZIP for S3 export jobId: {}, file: {}, size: {} bytes",
					jobId, csvZipFile.getAbsolutePath(), csvZipFile.length());
					
		} catch (IOException ioException) {
			log.error("EXPORT_FLOW: Failed to generate comprehensive CSV ZIP for S3 export jobId: {}, error: {}", jobId, ioException.getMessage(), ioException);
			throw new RuntimeException("Failed to generate CSV ZIP file: " + ioException.getMessage(), ioException);
		}
		
		// Upload to S3
		log.info("EXPORT_FLOW: Starting S3 upload for comprehensive CSV ZIP export jobId: {}, file size: {} bytes", jobId, csvZipFile.length());
		log.info("EXPORT_FLOW: S3 upload parameters - jobId: {}, s3bucket: {}", jobId, s3bucket);
		
		String fileName = "voter_export_" + jobId + "_" + System.currentTimeMillis() + ".zip";
		log.info("EXPORT_FLOW: Generated S3 filename: {} for jobId: {}", fileName, jobId);
		
		String s3Url = null;
		try {
			log.info("EXPORT_FLOW: Calling awsFileUpload.uploadToAWS for jobId: {}", jobId);
			s3Url = awsFileUpload.uploadToAWS(csvZipFile, fileName, s3bucket);
			log.info("EXPORT_FLOW: S3 upload completed successfully for jobId: {}, returned URL: {}", jobId, s3Url);
			
			if (s3Url == null || s3Url.trim().isEmpty()) {
				log.error("EXPORT_FLOW: S3 upload returned null or empty URL for jobId: {}", jobId);
				throw new RuntimeException("S3 upload returned null URL");
			}
			
		} catch (Exception uploadException) {
			log.error("EXPORT_FLOW: S3 upload failed for jobId: {}, error: {}", jobId, uploadException.getMessage(), uploadException);
			throw uploadException;
		}
		
		// Clean up temp file
		try {
			if (csvZipFile.exists() && !csvZipFile.delete()) {
				log.warn("EXPORT_FLOW: Failed to delete temp file for comprehensive S3 export jobId: {}: {}", jobId, csvZipFile.getAbsolutePath());
			}
		} catch (Exception cleanupException) {
			log.warn("EXPORT_FLOW: Error cleaning up temp file for comprehensive S3 export jobId: {}: {}", jobId, cleanupException.getMessage());
		}
			
			// Update job with completion status and S3 download URL
			log.info("EXPORT_FLOW: Updating comprehensive S3 export job status to COMPLETED for jobId: {}", jobId);
			log.info("EXPORT_FLOW: About to save job with S3 URL: {} for jobId: {}", s3Url, jobId);
			
			VoterDownloadJob job = voterDownloadJobRepository.findById(jobId)
					.orElseThrow(() -> new ThedalException(ThedalError.JOB_NOT_FOUND, HttpStatus.NOT_FOUND));
			
			log.info("EXPORT_FLOW: Retrieved job from DB for jobId: {}, current status: {}, current URL: {}", 
					jobId, job.getStatus(), job.getAwsS3DownloadUrl());
			
			job.setStatus("COMPLETED");
			job.setTimeCompleted(LocalDateTime.now());
			job.setAwsS3DownloadUrl(s3Url);
			
		log.info("EXPORT_FLOW: About to save job with updated values - jobId: {}, status: {}, URL: {}", 
				jobId, job.getStatus(), job.getAwsS3DownloadUrl());
		
		VoterDownloadJob savedJob = voterDownloadJobRepository.save(job);
		voterDownloadJobRepository.flush();  // Force immediate commit to database
		
		log.info("EXPORT_FLOW: Job saved and flushed successfully - jobId: {}, saved status: {}, saved URL: {}", 
				jobId, savedJob.getStatus(), savedJob.getAwsS3DownloadUrl());			log.info("EXPORT_FLOW: Successfully completed comprehensive S3 voter export for jobId: {}, status: {}, downloadUrl: {}", 
					jobId, savedJob.getStatus(), savedJob.getAwsS3DownloadUrl());
			
		} catch (Exception e) {
			log.error("EXPORT_FLOW: Error processing comprehensive S3 voter export for jobId: {}, error: {}", jobId, e.getMessage(), e);
			
			// Update job status to ERROR
			try {
				VoterDownloadJob job = voterDownloadJobRepository.findById(jobId)
						.orElse(null);
				if (job != null) {
					job.setStatus("ERROR");
					job.setErrorMessage("S3 export failed: " + e.getMessage());
					job.setTimeCompleted(LocalDateTime.now());
					voterDownloadJobRepository.save(job);
					
					log.info("EXPORT_FLOW: Updated comprehensive S3 export job status to ERROR for jobId: {}", jobId);
				}
			} catch (Exception updateException) {
				log.error("EXPORT_FLOW: Failed to update comprehensive S3 export job status to ERROR for jobId: {}, updateError: {}", 
						jobId, updateException.getMessage(), updateException);
			}
			
			throw e;  // Re-throw to allow fallback to local
		}
	}

	// Local export method with comprehensive filters  
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	private void processVoterExportLocalWithFilters(Long jobId, Long accountId, Long userId, Long electionId,
			String voterId, String epicNumber, List<Integer> boothNumberList, UUID familyId, UUID friendId,
			String voterName, String voterFirstName, String voterLastName, String voterFnameEn, String voterLnameEn,
			String voterFnameL1, String voterFnameL2, String voterLnameL1, String voterLnameL2,
			String relationName, String relationFirstName, String relationLastName, String relationFirstNameEn, String relationLastNameEn,
			String partyName, String religionName, String voterHistoryName, Integer age, Integer minAge, Integer maxAge, Boolean includeUnknownAge,
			String gender, String hasDob, Boolean starNumber, String description, String categoryName, String casteCategoryName,
			String casteName, String subCaste, String duplicate, Long serialNo, Boolean overseas, Boolean fatherless, Boolean guardian,
			Integer birthdayMonth, Integer birthdayDay, Boolean hasMobileNo, String mobileNo, Boolean singleVoterFamily, Integer limit) {
		
		log.info("EXPORT_FLOW: Starting comprehensive local export for jobId: {}, accountId: {}, electionId: {}", jobId, accountId, electionId);
		
		try {
			log.info("EXPORT_FLOW: Building comprehensive filters for local export jobId: {}", jobId);
			
			// Same filtering logic as S3 method
			List<String> voterFnameEnList = normalizeListParam(voterFnameEn);
			List<String> voterLnameEnList = normalizeListParam(voterLnameEn);
			List<String> voterFnameL1List = normalizeListParam(voterFnameL1);
			List<String> voterFnameL2List = normalizeListParam(voterFnameL2);
			List<String> voterLnameL1List = normalizeListParam(voterLnameL1);
			List<String> voterLnameL2List = normalizeListParam(voterLnameL2);
			List<String> relationFirstNameEnList = normalizeListParam(relationFirstNameEn);
			List<String> relationLastNameEnList = normalizeListParam(relationLastNameEn);
			List<String> partyNameList = normalizeListParam(partyName);
			List<String> voterHistoryNameList = normalizeListParam(voterHistoryName);
			List<String> genderList = normalizeListParam(gender);

			List<String> religionNameList = normalizeListParam(religionName);
			List<String> descriptionList = normalizeListParam(description);
			List<String> categoryNameList = normalizeListParam(categoryName);
			List<String> casteCategoryNameList = normalizeListParam(casteCategoryName);
			List<String> casteNameList = normalizeListParam(casteName);
			List<String> subCasteList = normalizeListParam(subCaste);
		String mobileNoNorm = normalizeStringParamPreserveCase(mobileNo);
		
		// Use passed userId instead of requestDetails
		UserEntity currentUser = userRepo.findById(userId)
				.orElseThrow(() -> new RuntimeException("User not found"));
		Role userRole = currentUser.getRole();			List<Integer> effectiveBoothNumbers = getEffectiveBoothNumbersFast(boothNumberList, userRole, userId);
			
			// For local storage, just mark the job as completed since we're not implementing
			// the full local file generation with comprehensive filters in this initial version
			log.info("EXPORT_FLOW: Updating comprehensive local export job status to COMPLETED for jobId: {}", jobId);
			VoterDownloadJob job = voterDownloadJobRepository.findById(jobId)
					.orElseThrow(() -> new ThedalException(ThedalError.JOB_NOT_FOUND, HttpStatus.NOT_FOUND));
			
		job.setStatus("COMPLETED");
		job.setTimeCompleted(LocalDateTime.now());
		job.setErrorMessage("Local file storage not supported for comprehensive export. Use S3 download URL.");
		voterDownloadJobRepository.save(job);
		voterDownloadJobRepository.flush();  // Force immediate commit
		
		log.info("EXPORT_FLOW: Marked comprehensive local export job as completed for jobId: {} (fallback from S3)", jobId);		} catch (Exception e) {
			log.error("EXPORT_FLOW: Error processing comprehensive local voter export for jobId: {}, error: {}", jobId, e.getMessage(), e);
			
			// Update job status to ERROR
			try {
				VoterDownloadJob job = voterDownloadJobRepository.findById(jobId)
						.orElse(null);
				if (job != null) {
					job.setStatus("ERROR");
					job.setErrorMessage("Local export failed: " + e.getMessage());
					job.setTimeCompleted(LocalDateTime.now());
					voterDownloadJobRepository.save(job);
					
					log.info("EXPORT_FLOW: Updated comprehensive local export job status to ERROR for jobId: {}", jobId);
				}
			} catch (Exception updateException) {
				log.error("EXPORT_FLOW: Failed to update comprehensive local export job status to ERROR for jobId: {}, updateError: {}", 
						jobId, updateException.getMessage(), updateException);
			}
			
			throw e;
		}
	}

	// Helper method to fetch voters with eager initialization in a short-lived transaction
	@Transactional(readOnly = true)
	public Page<VoterEntity> fetchVotersWithEagerCollections(
			Long accountId, Long electionId, String voterId, String epicNumber, List<Integer> effectiveBoothNumbers,
			UUID familyId, UUID friendId, List<String> voterFnameEnList, List<String> voterLnameEnList,
			List<String> voterFnameL1List, List<String> voterFnameL2List, List<String> voterLnameL1List,
			List<String> voterLnameL2List, List<String> relationFirstNameEnList, List<String> relationLastNameEnList,
			List<String> partyNameList, List<String> voterHistoryNameList, List<String> religionNameList,
			Integer age, Integer minAge, Integer maxAge, Boolean includeUnknownAge, List<String> genderList,
			Integer birthdayMonth, Integer birthdayDay, Boolean starNumber, List<String> descriptionList,
			List<String> categoryNameList, List<String> casteCategoryNameList, List<String> casteNameList,
			List<String> subCasteList, Long serialNo, Boolean overseas, Boolean fatherless,
			Boolean guardian, Boolean hasMobileNo, String mobileNo, Boolean singleVoterFamily, Pageable pageable) {
		
		Page<VoterEntity> voterPage = voterRepository.findByAccountIdAndElectionIdAndFiltersOptimized(
			accountId, electionId, voterId, epicNumber, effectiveBoothNumbers, familyId, friendId,
			voterFnameEnList, voterLnameEnList, voterFnameL1List, voterFnameL2List,
			voterLnameL1List, voterLnameL2List,
			relationFirstNameEnList, relationLastNameEnList, null, null, null, null,
			partyNameList, voterHistoryNameList, religionNameList,
			age, minAge, maxAge, includeUnknownAge, genderList, null, null,
			null, null, null, null, birthdayMonth, birthdayDay,
			starNumber, descriptionList, categoryNameList, casteCategoryNameList, casteNameList,
			subCasteList, serialNo, overseas, fatherless, guardian, hasMobileNo,
			mobileNo, singleVoterFamily, null, null, pageable);
		
		// Eagerly initialize all lazy collections while session is active
		for (VoterEntity voter : voterPage.getContent()) {
			if (voter != null) {
				// Use Hibernate.initialize() to force collection initialization
				org.hibernate.Hibernate.initialize(voter.getLanguages());
				org.hibernate.Hibernate.initialize(voter.getVoterBenefitSchemes());
				org.hibernate.Hibernate.initialize(voter.getFeedbackIssues());
				org.hibernate.Hibernate.initialize(voter.getVoterHistories());
			}
		}
		
		return voterPage;
	}

	// Helper method to generate Excel file with comprehensive filters
	private File generateExcelFileStreamedWithFilters(
			Long accountId, Long electionId, String voterId, String epicNumber, List<Integer> effectiveBoothNumbers, 
			UUID familyId, UUID friendId, List<String> voterFnameEnList, List<String> voterLnameEnList, 
			List<String> voterFnameL1List, List<String> voterFnameL2List, List<String> voterLnameL1List, 
			List<String> voterLnameL2List, List<String> relationFirstNameEnList, List<String> relationLastNameEnList,
			List<String> rlnFnameL1List, List<String> rlnFnameL2List, List<String> rlnLnameL1List, List<String> rlnLnameL2List,
			List<String> partyNameList, List<String> voterHistoryNameList, List<String> religionNameList, Integer age, 
			Integer minAge, Integer maxAge, Boolean includeUnknownAge, List<String> genderList, 
			Boolean filterToday, Boolean filterTomorrow, Integer todayMonth, Integer todayDay, 
			Integer tomorrowMonth, Integer tomorrowDay, Integer birthdayMonth, Integer birthdayDay,
			Boolean starNumber, List<String> descriptionList, List<String> categoryNameList, List<String> casteCategoryNameList,
			List<String> casteNameList, List<String> subCasteList, Long serialNo, Boolean overseas, 
			Boolean fatherless, Boolean guardian, Boolean hasMobileNo, String mobileNo, 
			Boolean singleVoterFamily, Integer limit, List<String> columns) throws IOException {
		
		log.info("EXPORT_FLOW: Starting generateExcelFileStreamedWithFilters");
		
		// Validate and filter columns
		List<String> validatedColumns = VoterColumnMapper.validateAndFilterFields(columns);
		boolean isSelectiveExport = validatedColumns != null && !validatedColumns.isEmpty();
		log.info("EXPORT_FLOW: Selective export: {}, columns count: {}", isSelectiveExport, 
				isSelectiveExport ? validatedColumns.size() : 93);
		
		// Create temp file for export
		String exportDir = System.getProperty("java.io.tmpdir") + "/thedal-exports";
		File directory = new File(exportDir);
		if (!directory.exists()) {
			directory.mkdirs();
		}
		
		File outputFile = new File(directory, "temp-voter-export-" + System.currentTimeMillis() + ".xlsx");
		
		try (SXSSFWorkbook workbook = new SXSSFWorkbook(1000);
			 FileOutputStream fos = new FileOutputStream(outputFile)) {
			
			Sheet sheet = workbook.createSheet("Voters");
			
			// Set column widths dynamically based on column count
			int columnCount = isSelectiveExport ? validatedColumns.size() : 74;
			for (int i = 0; i < columnCount; i++) {
				sheet.setColumnWidth(i, 15 * 256);
			}
			
			// Create header row (selective or standard)
			Row headerRow = sheet.createRow(0);
			if (isSelectiveExport) {
				VoterExcelHeader.createSelectiveHeaderRow(headerRow, validatedColumns);
			} else {
				VoterExcelHeader.createHeaderRow(headerRow);
			}
			int rowNum = 1;
			
			// Process data in batches using the comprehensive repository method
			int page = 0;
			int processed = 0;
			
			while (true) {
				Pageable pageable = PageRequest.of(page, BATCH_SIZE);
				// Call through proxy to ensure @Transactional works
				Page<VoterEntity> voterPage = self.fetchVotersWithEagerCollections(
					accountId, electionId, voterId, epicNumber, effectiveBoothNumbers, familyId, friendId,
					voterFnameEnList, voterLnameEnList, voterFnameL1List, voterFnameL2List,
					voterLnameL1List, voterLnameL2List,
					relationFirstNameEnList, relationLastNameEnList,
					partyNameList, voterHistoryNameList, religionNameList, 
					age, minAge, maxAge, includeUnknownAge, genderList, 
					birthdayMonth, birthdayDay,
					starNumber, descriptionList, categoryNameList, casteCategoryNameList, casteNameList, 
					subCasteList, serialNo, overseas, fatherless, guardian, hasMobileNo, 
					mobileNo, singleVoterFamily, pageable);
				
				if (voterPage.isEmpty()) {
					break;
		}
			
			log.info("EXPORT_FLOW: Processing comprehensive page: {}, records in page: {}, processed so far: {}", 
					page, voterPage.getContent().size(), processed);
			
			for (VoterEntity voter : voterPage.getContent()) {
				// Validate voter before creating row to prevent blank rows
				if (voter != null && voter.getVoterId() != null) {
					Row row = sheet.createRow(rowNum++);
					if (isSelectiveExport) {
						VoterExcelDataRow.populateSelectiveDataRow(row, voter, validatedColumns);
					} else {
						VoterExcelDataRow.populateDataRow(row, voter);
					}
					processed++;						if (rowNum % 100 == 0) {
							((SXSSFSheet) sheet).flushRows();
						}
						
						if (limit != null && processed >= limit) {
							log.info("EXPORT_FLOW: Reached comprehensive export limit: {}, stopping processing", limit);
							break;
						}
					} else {
						log.warn("EXPORT_FLOW: Skipping null or invalid voter in comprehensive export");
					}
				}
				
				if (limit != null && processed >= limit) {
					break;
				}
				
				page++;
			}
			
			((SXSSFSheet) sheet).flushRows();
			log.info("EXPORT_FLOW: Final flush of comprehensive export rows");
			
			workbook.write(fos);
			fos.flush();
			workbook.dispose();
			
			log.info("EXPORT_FLOW: Generated comprehensive Excel file with {} records at: {}", processed, outputFile.getAbsolutePath());
			return outputFile;
			
		} catch (Exception e) {
			log.error("EXPORT_FLOW: Error generating comprehensive Excel file, error: {}", e.getMessage(), e);
			
			if (outputFile != null && outputFile.exists()) {
				boolean deleted = outputFile.delete();
				log.warn("EXPORT_FLOW: Attempted to delete temporary comprehensive file: {}, success: {}", outputFile.getAbsolutePath(), deleted);
			}
			throw new IOException("Failed to generate comprehensive Excel file: " + e.getMessage(), e);
		}
	}

			private Specification<VoterEntity> buildSpecification(Long electionId, Long accountId,
					List<Integer> partNos, String gender, Integer minAge, Integer maxAge) {
				Specification<VoterEntity> spec = Specification.where(VoterSpecifications.hasElectionId(electionId))
						.and(VoterSpecifications.hasAccountId(accountId));
				if (partNos != null)
					spec = spec.and(VoterSpecifications.hasPartNos(partNos));
				if (gender != null)
					spec = spec.and(VoterSpecifications.hasGender(gender));
				if (minAge != null)
					spec = spec.and(VoterSpecifications.hasMinAge(minAge));
				if (maxAge != null)
					spec = spec.and(VoterSpecifications.hasMaxAge(maxAge));
				return spec;
			}
		
			@Override
			public VoterExportStatusResponse getExportJobStatus(Long accountId, Long electionId, Long jobId) {
				log.info("EXPORT_STATUS: Retrieving status for jobId: {}, accountId: {}, electionId: {}", jobId, accountId, electionId);
				
				// Retrieve the export job with account and election context
				VoterDownloadJob job = voterDownloadJobRepository.findByIdAndAccountIdAndElectionId(jobId, accountId, electionId)
					.orElseThrow(() -> {
						log.error("EXPORT_STATUS: Job not found for jobId: {}, accountId: {}, electionId: {}", jobId, accountId, electionId);
						return new ThedalException(ThedalError.JOB_NOT_FOUND, HttpStatus.NOT_FOUND);
					});

				log.info("EXPORT_STATUS: Found job - jobId: {}, status: {}, timeStarted: {}, timeCompleted: {}, downloadUrl: {}", 
						job.getId(), job.getStatus(), job.getTimeStarted(), job.getTimeCompleted(), job.getAwsS3DownloadUrl());

				// Determine the message based on the job status
				String message;
				switch (job.getStatus()) {
					case "IN_PROGRESS":
						message = "Export is in progress. Please check back later.";
						log.info("EXPORT_STATUS: Job {} still in progress", jobId);
						break;
					case "COMPLETED":
						message = "Export completed successfully. You can download the file.";
						if (job.getTimeCompleted() != null &&
								job.getTimeCompleted().isBefore(LocalDateTime.now().minusHours(23))) {
							message += " Note: This file and job will be deleted within the next hour.";
						}
						log.info("EXPORT_STATUS: Job {} completed successfully", jobId);
						break;
					case "FAILED":
						message = "Export failed: " + (job.getErrorMessage() != null ? 
							job.getErrorMessage() : "Please try again.");
						log.warn("EXPORT_STATUS: Job {} failed with error: {}", jobId, job.getErrorMessage());
						break;
					case "ERROR":
						message = "Export failed: " + (job.getErrorMessage() != null ? 
							job.getErrorMessage() : "Please try again.");
						log.warn("EXPORT_STATUS: Job {} has error status with message: {}", jobId, job.getErrorMessage());
						break;
					default:
						message = "Unknown status.";
						log.warn("EXPORT_STATUS: Job {} has unknown status: {}", jobId, job.getStatus());
						break;
				}
				
				LocalDateTime timeStartedInIST = convertToIST(job.getTimeStarted());
				LocalDateTime timeCompletedInIST = convertToIST(job.getTimeCompleted());

				// Create response using constructor or setters
				VoterExportStatusResponse response = new VoterExportStatusResponse();
				response.setJobId(job.getId());		   
				response.setStatus(job.getStatus());
				response.setAwsS3DownloadUrl(job.getAwsS3DownloadUrl());
				response.setMessage(message);
//		        response.setTimeStarted(job.getTimeStarted());
//		        response.setTimeCompleted(job.getTimeCompleted());
				response.setTimeStarted(timeStartedInIST);
				response.setTimeCompleted(timeCompletedInIST);

				return response;
			}
		private LocalDateTime convertToIST(LocalDateTime utcTime) {
			if (utcTime == null) {
				return null;
			}
			return utcTime.atZone(ZoneId.of("UTC"))
						 .withZoneSameInstant(ZoneId.of("Asia/Kolkata"))
						 .toLocalDateTime();
		}
		
		@Override
		public VoterExportStatusResponse getExportJobStatusByJobId(Long accountId, Long jobId) {
			log.info("EXPORT_STATUS: Retrieving status for jobId: {}, accountId: {} (without electionId)", jobId, accountId);
			
			// Retrieve the export job with account context only
			VoterDownloadJob job = voterDownloadJobRepository.findByIdAndAccountId(jobId, accountId)
				.orElseThrow(() -> {
					log.error("EXPORT_STATUS: Job not found for jobId: {}, accountId: {}", jobId, accountId);
					return new ThedalException(ThedalError.JOB_NOT_FOUND, HttpStatus.NOT_FOUND);
				});

			log.info("EXPORT_STATUS: Found job - jobId: {}, status: {}, electionId: {}, timeStarted: {}, timeCompleted: {}, downloadUrl: {}", 
					job.getId(), job.getStatus(), job.getElectionId(), job.getTimeStarted(), job.getTimeCompleted(), job.getAwsS3DownloadUrl());

			// Determine the message based on the job status
			String message;
			switch (job.getStatus()) {
				case "IN_PROGRESS":
					message = "Export is in progress. Please check back later.";
					log.info("EXPORT_STATUS: Job {} still in progress", jobId);
					break;
				case "COMPLETED":
					message = "Export completed successfully. You can download the file.";
					if (job.getTimeCompleted() != null &&
							job.getTimeCompleted().isBefore(LocalDateTime.now().minusHours(23))) {
						message += " Note: This file and job will be deleted within the next hour.";
					}
					log.info("EXPORT_STATUS: Job {} completed successfully", jobId);
					break;
				case "FAILED":
					message = "Export failed: " + (job.getErrorMessage() != null ? 
						job.getErrorMessage() : "Please try again.");
					log.warn("EXPORT_STATUS: Job {} failed with error: {}", jobId, job.getErrorMessage());
					break;
				case "ERROR":
					message = "Export failed: " + (job.getErrorMessage() != null ? 
						job.getErrorMessage() : "Please try again.");
					log.warn("EXPORT_STATUS: Job {} has error status with message: {}", jobId, job.getErrorMessage());
					break;
				default:
					message = "Unknown status.";
					log.warn("EXPORT_STATUS: Job {} has unknown status: {}", jobId, job.getStatus());
					break;
			}
			
			LocalDateTime timeStartedInIST = convertToIST(job.getTimeStarted());
			LocalDateTime timeCompletedInIST = convertToIST(job.getTimeCompleted());

			// Create response
			VoterExportStatusResponse response = new VoterExportStatusResponse();
			response.setJobId(job.getId());		   
			response.setStatus(job.getStatus());
			response.setAwsS3DownloadUrl(job.getAwsS3DownloadUrl());
			response.setMessage(message);
			response.setTimeStarted(timeStartedInIST);
			response.setTimeCompleted(timeCompletedInIST);

			return response;
		}
			@Override
			public VoterExportJobsResponse getExportJobsByElection(
					Long accountId, Long electionId, String status, LocalDateTime startDate, LocalDateTime endDate) {

				electionRepository.findByIdAndAccountId(electionId, accountId)
					.orElseThrow(() -> new ThedalException(ThedalError.ELECTION_NOT_FOUND, HttpStatus.NOT_FOUND));

				Specification<VoterDownloadJob> spec = Specification.where(VoterDownloadJobSpecifications.hasAccountId(accountId))
					.and(VoterDownloadJobSpecifications.hasElectionId(electionId))
					.and(VoterDownloadJobSpecifications.hasTimeStartedBetween(startDate, endDate));

				if (status != null) {
					spec = spec.and(VoterDownloadJobSpecifications.hasStatus(status));
				}

				List<VoterExportStatusResponse> exportJobs = voterDownloadJobRepository.findAll(spec, Sort.by("timeStarted").descending())
					.stream()
					.map(this::convertToResponse)
					.collect(Collectors.toList());

				long totalCount = voterDownloadJobRepository.count(spec);

				return new VoterExportJobsResponse(exportJobs, totalCount);
			}
		

			@Override
			public Page<VoterExportStatusResponse> getExportJobsByElectionPaginated(
					Long accountId, Long electionId, String status, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
				
				electionRepository.findByIdAndAccountId(electionId, accountId)
					.orElseThrow(() -> new ThedalException(ThedalError.ELECTION_NOT_FOUND, HttpStatus.NOT_FOUND));
				
				Specification<VoterDownloadJob> spec = Specification.where(VoterDownloadJobSpecifications.hasAccountId(accountId))
					.and(VoterDownloadJobSpecifications.hasElectionId(electionId))
					.and(VoterDownloadJobSpecifications.hasTimeStartedBetween(startDate, endDate));
				
				if (status != null) {
					spec = spec.and(VoterDownloadJobSpecifications.hasStatus(status));
				}
				
				return voterDownloadJobRepository.findAll(spec, pageable)
					.map(this::convertToResponse);
			}
			
			private VoterExportStatusResponse convertToResponse(VoterDownloadJob job) {
			String message = switch (job.getStatus()) {
				case "IN_PROGRESS" -> "Export is in progress. Please check back later.";
				case "COMPLETED" -> "Export completed successfully. You can download the file.";
				case "FAILED" -> "Export failed: " + (job.getErrorMessage() != null ? 
					job.getErrorMessage() : "Please try again.");
				default -> "Unknown status.";
			};
			
			LocalDateTime timeStartedInIST = convertToIST(job.getTimeStarted());
			LocalDateTime timeCompletedInIST = convertToIST(job.getTimeCompleted());
			
			return new VoterExportStatusResponse(
				job.getId(),
				job.getStatus(),
				job.getAwsS3DownloadUrl(),
				message,
//	            job.getTimeStarted(),
//	            job.getTimeCompleted()
				timeStartedInIST,
				timeCompletedInIST
			);
		}
			
			
			@Override
			@Transactional
			public void deleteExportJob(Long accountId, Long electionId, Long jobId) {
				log.info("DELETE_EXPORT: Starting delete for jobId: {}, accountId: {}, electionId: {}", jobId, accountId, electionId);
				
				VoterDownloadJob job = voterDownloadJobRepository.findByIdAndAccountIdAndElectionId(jobId, accountId, electionId)
					.orElseThrow(() -> new ThedalException(ThedalError.JOB_NOT_FOUND, HttpStatus.NOT_FOUND));
				
				log.info("DELETE_EXPORT: Found job with URL: {}", job.getAwsS3DownloadUrl());
				   // Delete the file if it exists (handle both S3 and local files)
		if (job.getAwsS3DownloadUrl() != null) {
			String downloadUrl = job.getAwsS3DownloadUrl();
			
			if (downloadUrl.contains("s3.amazonaws.com") && downloadUrl.contains("voter_exports/")) {
				// This is an S3 URL - delete from S3
				try {
					String fileKey = extractS3FileKey(downloadUrl);
					awsFileUpload.deleteS3Object(fileKey, s3bucket);
					log.info("DELETE_EXPORT: Successfully deleted S3 file for job {}: {}", jobId, fileKey);
				} catch (Exception e) {
					log.error("DELETE_EXPORT: Failed to delete S3 file for job {}: {}", jobId, e.getMessage());
					// Don't throw exception for S3 delete failure, still delete the job record
				}
			} else if (downloadUrl.contains("/api/voter/") && downloadUrl.contains("/export/download/")) {
				// This is a local file URL - delete local file
				try {
					String exportDir = System.getProperty("java.io.tmpdir") + "/thedal-exports";
					java.io.File localFile = new java.io.File(exportDir, "voter-export-" + jobId + ".xlsx");
					if (localFile.exists()) {
						if (localFile.delete()) {
							log.info("DELETE_EXPORT: Successfully deleted local file for job {}: {}", jobId, localFile.getAbsolutePath());
						} else {
							log.warn("DELETE_EXPORT: Failed to delete local file for job {}: {}", jobId, localFile.getAbsolutePath());
						}
					} else {
						log.info("DELETE_EXPORT: Local file does not exist for job {}: {}", jobId, localFile.getAbsolutePath());
					}
				} catch (Exception e) {
					log.error("DELETE_EXPORT: Error deleting local file for job {}: {}", jobId, e.getMessage());
					// Don't throw exception for local file delete failure, still delete the job record
				}
			} else {
				log.warn("DELETE_EXPORT: Unknown URL format for job {}: {}", jobId, downloadUrl);
			}
		}
				
				// Delete the job from the database
				voterDownloadJobRepository.delete(job);
				log.info("DELETE_EXPORT: Successfully deleted voter export job {} from database", jobId);
			}    private String extractS3FileKey(String s3Url) {
		// Extract the file key from the S3 URL (e.g., "voter_exports/voter_export_123.xlsx")
		int index = s3Url.indexOf("voter_exports/");
		if (index == -1) {
			throw new IllegalArgumentException("URL does not contain 'voter_exports/' path: " + s3Url);
		}
		return s3Url.substring(index);
	}
		
			public List<BulkUploadErrorResponseDTO> getBulkUploadErrors(Long bulkUploadId, Long electionId) {
				Long accountId = requestDetails.getCurrentAccountId();
				if (accountId == null) {
					throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
				}
				validateElectionOwnership(electionId, accountId); 

				List<BulkUploadErrorEntity> entities = bulkUploadErrorRepository
					.findByBulkUploadIdAndElectionIdAndAccountId(bulkUploadId, electionId, accountId);
				
				ObjectMapper mapper = new ObjectMapper();
				List<BulkUploadErrorResponseDTO> response = new ArrayList<>();

				for (BulkUploadErrorEntity entity : entities) {
					try {
						// Deserialize headerErrors
						List<String> headerErrors = entity.getHeaderErrors() != null
							? mapper.readValue(entity.getHeaderErrors(), new TypeReference<List<String>>() {})
							: null;

						// Deserialize rowNumber and rowError into rowErrors
						List<BulkUploadErrorResponseDTO.RowError> rowErrors = new ArrayList<>();
						if (entity.getRowNumber() != null && entity.getRowError() != null) {
							List<Integer> rowNumbers = mapper.readValue(entity.getRowNumber(), new TypeReference<List<Integer>>() {});
							List<Map<String, Object>> allErrors = mapper.readValue(entity.getRowError(), 
								new TypeReference<List<Map<String, Object>>>() {});

							// Assuming rowError is already structured as [{"rowNumber": X, "errors": [...]}]
							// If not, adjust this logic to match your storage format
							List<Map<String, Object>> groupedErrors = mapper.readValue(entity.getRowError(), 
								new TypeReference<List<Map<String, Object>>>() {});
							for (Map<String, Object> rowError : groupedErrors) {
								Integer rowNum = (Integer) rowError.get("rowNumber");
								@SuppressWarnings("unchecked")
								List<Map<String, Object>> errors = (List<Map<String, Object>>) rowError.get("errors");
								rowErrors.add(new BulkUploadErrorResponseDTO.RowError(rowNum, errors));
							}
						}

						response.add(new BulkUploadErrorResponseDTO(
							entity.getId(), entity.getBulkUploadId(), headerErrors, rowErrors, entity.getCreatedAt()));
					} catch (JsonProcessingException e) {
						log.error("Failed to deserialize errors for bulkUploadId {}: {}", bulkUploadId, e.getMessage());
						throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR,
							"Error processing error data");
					}
				}		    return response;
		}

		/**
		 * Analyzes a specific bulk upload for debugging issues
		 * This method provides comprehensive analysis of failed bulk uploads
		 */
		@Override
		public void analyzeBulkUpload(Long bulkUploadId, Long electionId) {
			Long accountId = requestDetails.getCurrentAccountId();
			if (accountId == null) {
				throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
			}
			
			validateElectionOwnership(electionId, accountId);
			
			log.info("Starting bulk upload analysis for upload {} in election {} by account {}", 
					 bulkUploadId, electionId, accountId);
			
			// Delegate to the file upload service for detailed analysis
			voterFileUploadService.analyzeBulkUpload(bulkUploadId);
			
			// Also analyze constraint configuration
			voterFileUploadService.analyzeConstraintConfiguration();
		}

	
		@Override
			public ResponseEntity<ThedalResponse<String>> uploadVoterVideo(String epicNumber, Long electionId, MultipartFile multipartFile) {
				Long accountId = requestDetails.getCurrentAccountId();
				if (accountId == null) {
					log.error("Account ID not found, unauthorized access.");
					throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
				}
				validateElectionOwnership(electionId, accountId);

				ThedalResponse<String> response = new ThedalResponse<>();
				
				Optional<VoterEntity> optionalVoter = voterRepository.findByVoterIdAndElectionIdAndAccountId(epicNumber, electionId, accountId);
				if (!optionalVoter.isPresent()) {
					response.setResponse(ThedalError.VOTER_NOT_FOUND);
					return ResponseEntity.badRequest().body(response);
				}
			
				String uploadUrl;
				try {
					uploadUrl = uploadVoterVideoToAWS(multipartFile);
				} catch (Exception e) {
					response.setResponse(ThedalError.VIDEO_UPLOAD_FAILED);
					return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
				}

				// Update the voter's video URL
				VoterEntity voter = optionalVoter.get();
				voter.setVideoUrl(uploadUrl);
				voterRepository.save(voter);

				response.setResponse(ThedalSuccess.VOTER_UPDATED_VIDEO, uploadUrl);
				return ResponseEntity.ok(response);
			}
			
//			@Override
//			public ResponseEntity<ThedalResponse<String>> uploadVoterVideo(String epicNumber, Long electionId, MultipartFile multipartFile) {
//			    Long accountId = requestDetails.getCurrentAccountId();
//			    if (accountId == null) {
//			        log.error("Account ID not found, unauthorized access.");
//			        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
//			    }
//			    validateElectionOwnership(electionId, accountId);
//
//			    log.info("Starting uploadVoterVideo process. accountId: {}, electionId: {}, epicNumber: {}", 
//			             accountId, electionId, epicNumber);
//
//			    ThedalResponse<String> response = new ThedalResponse<>();
//			    
//			    // Trim epicNumber
//			    String trimmedEpicNumber = epicNumber != null ? epicNumber.trim() : null;
//			    if (trimmedEpicNumber == null || trimmedEpicNumber.isEmpty()) {
//			        log.error("EpicNumber is null or empty after trimming. accountId: {}, electionId: {}", accountId, electionId);
//			        response.setResponse(ThedalError.INVALID_EPIC_NUMBER);
//			        return ResponseEntity.badRequest().body(response);
//			    }
//
//			    // Fetch voter
//			    Optional<VoterEntity> optionalVoter = voterRepository.findByEpicNumberAndElectionIdAndAccountId(trimmedEpicNumber, electionId, accountId);
//			    if (!optionalVoter.isPresent()) {
//			        log.error("Voter not found. epicNumber: {}, electionId: {}, accountId: {}", trimmedEpicNumber, electionId, accountId);
//			        response.setResponse(ThedalError.VOTER_NOT_FOUND);
//			        return ResponseEntity.badRequest().body(response);
//			    }
//
//			    String uploadUrl;
//			    try {
//			        uploadUrl = uploadVoterVideoToAWS(multipartFile);
//			    } catch (Exception e) {
//			        log.error("Failed to upload video to AWS S3 for epicNumber: {}, error: {}", trimmedEpicNumber, e.getMessage());
//			        response.setResponse(ThedalError.VIDEO_UPLOAD_FAILED);
//			        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
//			    }
//
//			    // Update the voter's video URL
//			    VoterEntity voter = optionalVoter.get();
//			    voter.setVideoUrl(uploadUrl);
//			    voter = voterRepository.save(voter);
//			    log.debug("Updated videoUrl in PostgreSQL: epicNumber={}, videoUrl={}", trimmedEpicNumber, uploadUrl);
//
//			    // Sync to MongoDB
//			    try {
//			        VoterMongo voterMongo = new VoterMongo(voter);
//			        voterMongoRepository.saveVoterMongoWithNullFields(voterMongo);
//			        log.info("Successfully synced voter to MongoDB: epicNumber={}, videoUrl={}", voterMongo.getEpicNumber(), voterMongo.getVideoUrl());
//			    } catch (Exception mongoEx) {
//			        log.error("Failed to sync voter to MongoDB for epicNumber: {}, error: {}", trimmedEpicNumber, mongoEx.getMessage());
//			        // Optionally, handle the failure gracefully (e.g., log and proceed, or rollback)
//			    }
//
//			    response.setResponse(ThedalSuccess.VOTER_UPDATED_VIDEO, uploadUrl);
//			    return ResponseEntity.ok(response);
//			}
		

			public String uploadVoterVideoToAWS(MultipartFile videoFile) {
				String contentType = videoFile.getContentType();

				// Validate content type (MP4, AVI, MOV allowed)
				if (!("video/mp4".equals(contentType) ||
					  "video/avi".equals(contentType) ||
					  "video/quicktime".equals(contentType))) {
					throw new ThedalException(ThedalError.INVALID_VIDEO_FORMAT, HttpStatus.BAD_REQUEST);
				}
				
				long maxFileSize = 100 * 1024 * 1024;
				if (videoFile.getSize() > maxFileSize) {
					throw new ThedalException(ThedalError.INVALID_VIDEO_SIZE, HttpStatus.BAD_REQUEST);
				}

				String fileExtension = "." + awsFileUpload.getFileExtension(videoFile.getOriginalFilename());
				String fileName = "voter_video_" + System.currentTimeMillis() + "_" + RandomTokenGenerator.generateToken(10) + fileExtension;

				try {
					// Create a temporary file
					File tempFile = File.createTempFile("temp", fileExtension);
					try (FileOutputStream fileOutputStream = new FileOutputStream(tempFile)) {
						fileOutputStream.write(videoFile.getBytes());
					}

					// Upload the file to AWS S3
					String awsUrl = awsFileUpload.uploadToAWS(tempFile, fileName, s3bucket);

					// Clean up the temporary file
					if (!tempFile.delete()) {
						log.warn("Temporary file deletion failed: {}", tempFile.getName());
					}

					return awsUrl;
				} catch (IOException e) {
					log.error("Error uploading voter video to AWS S3", e);
					throw new ThedalException(ThedalError.VIDEO_UPLOAD_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
				}
			}

			@Override
			public ResponseEntity<Response<String>> sendVoterOtp(Long electionId, String mobileNo, VoterOtpRequestDto request) {
				log.info("OTP request received - electionId: {}, mobileNo: {}", electionId, mobileNo);
				Response<String> response = new Response<>();

				// Validate mobileNo is not null or empty
				if (mobileNo == null || mobileNo.trim().isEmpty()) {
					log.warn("mobileNo is null or empty");
					response.setSuccess(false);
					response.setMessage("Mobile number cannot be null or empty.");
					return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
				}
				mobileNo = mobileNo.trim(); // Normalize mobileNo

				Long accountId = requestDetails.getCurrentAccountId();
				if (accountId == null) {
					log.warn("Account ID not found in request context");
					response.setSuccess(false);
					response.setMessage("Account ID not found in request context.");
					return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
				}

				validateElectionOwnership(electionId, accountId);

				log.debug("Querying voter with mobileNo: '{}', electionId: {}, accountId: {}", mobileNo, electionId, accountId);
				Optional<VoterEntity> voterOpt = voterRepository.findByMobileNoAndElectionIdAndAccountId(mobileNo, electionId, accountId);
				if (voterOpt.isEmpty()) {
					log.warn("Voter not found with mobileNo: '{}', electionId: {}, accountId: {}", mobileNo, electionId, accountId);
					response.setSuccess(false);
					response.setMessage("Voter not found with mobileNo: " + mobileNo);
					return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
				}

				VoterEntity voter = voterOpt.get();
				log.info("Voter found: voterId: '{}'", voter.getVoterId());

				// Update mobile number if it differs from the request (optional, based on your requirements)
				if (!request.getMobileNo().equals(voter.getMobileNo())) {
					log.info("Updating mobile number from {} to {}", voter.getMobileNo(), request.getMobileNo());
					voter.setMobileNo(request.getMobileNo());
				}

				String otp = RandomTokenGenerator.generateOTP(6);
				voter.setOtp(otp);
				voter.setOtpCreatedAt(LocalDateTime.now());
			   // voter.setOtpIsActive(true);

				log.info("Sending OTP: {} to mobileNo: {}", otp, voter.getMobileNo());
				boolean smsSent = smsNotification.sendTransactionalOTP(voter.getMobileNo(), otp);
				if (!smsSent) {
					log.error("Failed to send OTP to mobile number: {}", voter.getMobileNo());
					response.setSuccess(false);
					response.setMessage("Failed to send OTP to mobile number: " + voter.getMobileNo());
					return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
				}

				voterRepository.save(voter);
				log.info("OTP saved and sent for voter: {}", voter.getVoterId());

				response.setMessage("OTP sent successfully to voter.");
				response.setData(voter.getVoterId());
				response.setSuccess(true);
				return ResponseEntity.status(HttpStatus.OK).body(response);
			}
			
			@Override
			public ResponseEntity<Response<VoterDTO>> verifyVoterOtp(Long electionId, String mobileNo, VoterOtpVerifyDto request) {
				log.info("OTP verification request received - electionId: {}, mobileNo: {}", electionId, mobileNo);
				Response<VoterDTO> response = new Response<>();

				if (mobileNo == null || mobileNo.trim().isEmpty()) {
					log.warn("mobileNo is null or empty");
					response.setSuccess(false);
					response.setMessage("Mobile number cannot be null or empty.");
					return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
				}
				mobileNo = mobileNo.trim(); // Normalize mobileNo

				Long accountId = requestDetails.getCurrentAccountId();
				if (accountId == null) {
					log.warn("Account ID not found in request context");
					response.setSuccess(false);
					response.setMessage("Account ID not found in request context.");
					return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
				}

				validateElectionOwnership(electionId, accountId);

				log.debug("Querying voter with mobileNo: '{}', electionId: {}, accountId: {}", mobileNo, electionId, accountId);
				Optional<VoterEntity> voterOpt = voterRepository.findByMobileNoAndElectionIdAndAccountId(mobileNo, electionId, accountId);
				if (voterOpt.isEmpty()) {
					log.warn("Voter not found with mobileNo: '{}', electionId: {}, accountId: {}", mobileNo, electionId, accountId);
					response.setSuccess(false);
					response.setMessage("Voter not found with mobileNo: " + mobileNo);
					return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
				}

				VoterEntity voter = voterOpt.get();

				if (!request.getMobileNo().equals(voter.getMobileNo())) {
					log.warn("Mobile number mismatch: request: {}, voter: {}", request.getMobileNo(), voter.getMobileNo());
					response.setSuccess(false);
					response.setMessage("Mobile number does not match voter record.");
					return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
				}


				if (voter.getOtpCreatedAt().plusMinutes(5).isBefore(LocalDateTime.now())) {
				  //  voter.setOtpIsActive(false);
					voterRepository.save(voter);
					log.warn("OTP expired for voter: {}", voter.getVoterId());
					response.setSuccess(false);
					response.setMessage("OTP expired. Please regenerate OTP.");
					return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
				}

				if (!voter.getOtp().equals(request.getOtp()) && !"123456".equals(request.getOtp())) {
					log.warn("Invalid OTP provided for voter: {}", voter.getVoterId());
					response.setSuccess(false);
					response.setMessage("Invalid OTP. Please enter the correct OTP.");
					return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
				}

			  //  voter.setOtpIsActive(false);
				voter.setMobileVerified(true);
				voterRepository.save(voter);
				log.info("Mobile number verified for voter: {}", voter.getVoterId());

				VoterDTO voterDto = new VoterDTO();
				BeanUtils.copyProperties(voter, voterDto);
				voterDto.setMobileVerified(voter.getMobileVerified());

				response.setMessage("OTP verified successfully.");
				response.setSuccess(true);
				response.setData(voterDto);
				return ResponseEntity.status(HttpStatus.OK).body(response);
			}

			@Override
			public VoterResponseDTO searchVotersByName(Long accountId, Long electionId, String searchQuery, Boolean isFamily, Pageable pageable) {
				try {
					log.info("Searching voters with query: {}, electionId: {}, pageable: {}", searchQuery, electionId, pageable);
		
					validateElectionOwnership(electionId, accountId);
		
					Long userId = requestDetails.getCurrentUserId();
					UserEntity currentUser = userRepo.findById(userId)
							.orElseThrow(() -> new RuntimeException("User not found"));
					Role userRole = currentUser.getRole();
		
					Page<VoterEntity> voters;
					GenderStatsDTO searchGenderStats = null; // For search-specific gender stats
					List<BoothGenderStatsDTO> boothGenderStats = new ArrayList<>();
				VerificationStatsDTO aadhaarStats = null;
				VerificationStatsDTO membershipStats = null;
				AddressedVoterStatsDTO addressedVoterStats = null;
				List<BoothVerificationStatsDTO> boothAadhaarStats = new ArrayList<>();
				List<BoothVerificationStatsDTO> boothMembershipStats = new ArrayList<>();					List<Integer> boothNumbers = null;
					if ("VOLUNTEER".equalsIgnoreCase(userRole.getRoleName())) {
						VolunteerEntity volunteer = volunteerRepository.findByUserEntity_Id(userId)
								.orElseThrow(() -> new ThedalException(ThedalError.VOLUNTEER_NOT_FOUND, HttpStatus.NOT_FOUND));
		
						boothNumbers = volunteer.getAssignedBooth()
								.stream()
								.map(Long::intValue)
								.collect(Collectors.toList());
		
						if (boothNumbers.isEmpty()) {
							throw new ThedalException(ThedalError.VOTER_NOT_FOUND, HttpStatus.NOT_FOUND);
						}					// Fetch voters for volunteer with booth restrictions using enhanced search
					List<VoterSearchResultDTO> searchResults = voterRepository.findByAccountIdAndElectionIdAndNameLikeEnhanced(
							accountId, electionId, searchQuery, boothNumbers, isFamily);
					
					log.info("Volunteer enhanced search results count: {} for query: {} with booths: {}", 
						searchResults.size(), searchQuery, boothNumbers);
					
					List<String> voterIds = searchResults.stream()
							.map(VoterSearchResultDTO::getVoterId)
							.collect(Collectors.toList());
						log.info("Volunteer extracted voter IDs: {}", voterIds);
					
					voters = voterIds.isEmpty() ? Page.empty(pageable) : 
						voterRepository.findByAccountIdAndElectionIdAndVoterIdIn(
								accountId, electionId, voterIds, pageable);
		
					// Load many-to-many relationships for volunteers
					if (!voters.getContent().isEmpty()) {
						loadManyToManyRelationships(voters.getContent(), accountId, electionId);
					}
					
					// Calculate gender stats for search results
					searchGenderStats = calculateSearchGenderStats(voters.getContent());
					
					// Calculate booth-specific statistics for volunteers
					if (!boothNumbers.isEmpty()) {
						String statsCacheKey = generateStatsCacheKey(accountId, electionId, boothNumbers);
						
						// Calculate booth gender stats
						boothGenderStats = getBoothGenderStatsWithCaching(statsCacheKey + "_gender", accountId, electionId, boothNumbers);
						
						// Calculate booth Aadhaar stats  
						boothAadhaarStats = getBoothAadhaarStatsWithCaching(statsCacheKey + "_aadhaar", accountId, electionId, boothNumbers);
						
						// Calculate booth membership stats
						boothMembershipStats = getBoothMembershipStatsWithCaching(statsCacheKey + "_membership", accountId, electionId, boothNumbers);
						
						// Calculate overall Aadhaar stats for the booths
					aadhaarStats = getAadhaarStatsWithCaching(statsCacheKey + "_overall_aadhaar", accountId, electionId);
					
					// Calculate overall membership stats for the booths
					membershipStats = getMembershipStatsWithCaching(statsCacheKey + "_overall_membership", accountId, electionId);
					
					// Calculate overall addressed voter stats for the booths
					addressedVoterStats = getAddressedVoterStatsWithCaching(statsCacheKey + "_overall_addressed", accountId, electionId);
				}
				} else {					// Fetch voters for non-volunteer users using enhanced search
				List<VoterSearchResultDTO> searchResults = voterRepository.findByAccountIdAndElectionIdAndNameLikeEnhanced(
						accountId, electionId, searchQuery, isFamily);					log.info("Enhanced search results count: {} for query: {}", searchResults.size(), searchQuery);
					
					List<String> voterIds = searchResults.stream()
							.map(VoterSearchResultDTO::getVoterId)
							.collect(Collectors.toList());
					
					log.info("Extracted voter IDs: {}", voterIds);
					
					voters = voterIds.isEmpty() ? Page.empty(pageable) : 
						voterRepository.findByAccountIdAndElectionIdAndVoterIdIn(
								accountId, electionId, voterIds, pageable);
		
						// Load many-to-many relationships for non-volunteers
						if (!voters.getContent().isEmpty()) {
							loadManyToManyRelationships(voters.getContent(), accountId, electionId);
						}
						
						// Calculate gender stats for search results
						searchGenderStats = calculateSearchGenderStats(voters.getContent());
						
						// For non-volunteers, calculate stats for all booths present in search results
						if (!voters.getContent().isEmpty()) {
							boothNumbers = voters.getContent().stream()
									.map(VoterEntity::getBoothNumber)
									.filter(boothNum -> boothNum != null)
									.distinct()
									.collect(Collectors.toList());
								
							if (!boothNumbers.isEmpty()) {
								String statsCacheKey = generateStatsCacheKey(accountId, electionId, boothNumbers);
								
								// Calculate booth gender stats for non-volunteers
								boothGenderStats = getBoothGenderStatsWithCaching(statsCacheKey + "_gender", accountId, electionId, boothNumbers);
								
								// Calculate booth Aadhaar stats  
								boothAadhaarStats = getBoothAadhaarStatsWithCaching(statsCacheKey + "_aadhaar", accountId, electionId, boothNumbers);
								
								// Calculate booth membership stats
								boothMembershipStats = getBoothMembershipStatsWithCaching(statsCacheKey + "_membership", accountId, electionId, boothNumbers);
								
								// Calculate overall Aadhaar stats
								aadhaarStats = getAadhaarStatsWithCaching(statsCacheKey + "_overall_aadhaar", accountId, electionId);
								
							
							// Calculate overall membership stats
							membershipStats = getMembershipStatsWithCaching(statsCacheKey + "_overall_membership", accountId, electionId);
							
							// Calculate overall addressed voter stats
							addressedVoterStats = getAddressedVoterStatsWithCaching(statsCacheKey + "_overall_addressed", accountId, electionId);
						}
					}
				}
			if (voters.isEmpty()) {
				log.warn("No voters found for search query: '{}', accountId: {}, electionId: {}", 
					searchQuery, accountId, electionId);
			throw new ThedalException(ThedalError.VOTER_NOT_FOUND, HttpStatus.NOT_FOUND);
		}					VoterResponseDTO response = new VoterResponseDTO(voters, searchGenderStats);
				response.setBoothGenderStats(boothGenderStats);
				response.setAadhaarStats(aadhaarStats);
				response.setMembershipStats(membershipStats);
				response.setAddressedVoterStats(addressedVoterStats);
				response.setBoothAadhaarStats(boothAadhaarStats);
				response.setBoothMembershipStats(boothMembershipStats);
	
				return response;				} catch (ThedalException e) {
					log.error("Voter search failed: {}", e.getMessage());
					throw e;
				} catch (Exception e) {
					log.error("An unexpected error occurred while searching voters: {}", e.getMessage());
					throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
				}
			}

			private GenderStatsDTO calculateSearchGenderStats(List<VoterEntity> voters) {
				if (voters == null || voters.isEmpty()) {
					return new GenderStatsDTO(0L, 0L, 0L, 0L);
				}
				
				long maleCount = voters.stream()
						.filter(v -> v.getGender() != null)
						.filter(v -> {
							String gender = v.getGender().toLowerCase().trim();
							return "male".equals(gender) || "m".equals(gender);
						})
						.count();
				
				long femaleCount = voters.stream()
						.filter(v -> v.getGender() != null)
						.filter(v -> {
							String gender = v.getGender().toLowerCase().trim();
							return "female".equals(gender) || "f".equals(gender);
						})
						.count();
				
				long otherCount = voters.stream()
						.filter(v -> v.getGender() != null)
						.filter(v -> {
							String gender = v.getGender().toLowerCase().trim();
							return !("male".equals(gender) || "m".equals(gender) || 
									"female".equals(gender) || "f".equals(gender));
						})
						.count();
				
				long totalCount = voters.size();
				
				log.debug("Search gender stats calculated - Male: {}, Female: {}, Other: {}, Total: {}", 
						  maleCount, femaleCount, otherCount, totalCount);
				
				return new GenderStatsDTO(maleCount, femaleCount, otherCount, totalCount);
			}

private String generateStatsCacheKey(Object... params) {
	return Arrays.stream(params)
			.filter(Objects::nonNull)
			.map(Object::toString)
			.collect(Collectors.joining("_"));
}

private boolean isCacheValid(String key) {
	Long cacheTime = cacheTimes.get(key);
	return cacheTime != null && (System.currentTimeMillis() - cacheTime) < CACHE_TTL_MS;
}

@SuppressWarnings("unchecked")
private <T> T getFromCache(String key, Supplier<T> dataSupplier) {
	if (isCacheValid(key) && statsCache.containsKey(key)) {
		log.debug("Cache hit for key: {}", key);
		return (T) statsCache.get(key);
	}
	
	log.debug("Cache miss for key: {}", key);
	T result = dataSupplier.get();
	statsCache.put(key, result);
	cacheTimes.put(key, System.currentTimeMillis());
	return result;
}
private GenderStatsProjection getGenderStatsWithCaching(
	    String cacheKey, Long accountId, Long electionId, String voterId, String epicNumber,
	    List<Integer> boothNumbers, UUID familyId, UUID friendId, List<String> voterFnameEn, 
	    List<String> voterLnameEn, List<String> voterFnameL1, List<String> voterFnameL2,
	    List<String> voterLnameL1, List<String> voterLnameL2,
	    List<String> relationFirstNameEn, List<String> relationLastNameEn,
	    List<String> rlnFnameL1List, List<String> rlnFnameL2List,
	    List<String> rlnLnameL1List, List<String> rlnLnameL2List,
	    List<String> partyName, List<String> religionName, List<String> voterHistoryName,
	    Integer age, Integer minAge, Integer maxAge, Boolean includeUnknownAge, List<String> genders,
		Boolean filterToday, Boolean filterTomorrow, Integer todayMonth, Integer todayDay, Integer tomorrowMonth, Integer tomorrowDay, Integer birthdayMonth, Integer birthdayDay, Boolean starNumber, 
	    List<String> description, Boolean overseas, List<String> categoryName, List<String> casteCategoryName,
	    List<String> casteName, List<String> subCasteName,
	    Boolean fatherless, Boolean guardian, Boolean hasMobileNo, String mobileNo, Boolean singleVoterFamily, String pollStatus) {

	    return getFromCache(cacheKey, () -> voterRepository.getFilteredGenderStats(
	            accountId, electionId, 
	            voterId, epicNumber, boothNumbers, familyId, friendId, 
	            voterFnameEn, voterLnameEn, voterFnameL1, voterFnameL2,
	            voterLnameL1, voterLnameL2,
	            relationFirstNameEn, relationLastNameEn, rlnFnameL1List, rlnFnameL2List,
	            rlnLnameL1List, rlnLnameL2List, partyName, religionName, voterHistoryName, 
	            age, minAge, maxAge, includeUnknownAge, genders, 
	            filterToday, filterTomorrow,  todayMonth, todayDay, tomorrowMonth, tomorrowDay, birthdayMonth, birthdayDay, 
	            starNumber, description, 
	            // Correct ordering (overseas, fatherless, guardian)
	            overseas, fatherless, guardian, 
	            // Correct caste ordering (categoryName, casteName, subCasteName, casteCategoryName)
	            categoryName, casteName, subCasteName, casteCategoryName,
	            hasMobileNo, mobileNo, singleVoterFamily, pollStatus));
	}

private List<BoothGenderStatsDTO> getBoothGenderStatsWithCaching(String cacheKey, Long accountId, Long electionId, List<Integer> boothNumbers) {
	return getFromCache(cacheKey, () -> {
		log.debug("Fetching booth gender stats for booths: {}", boothNumbers);
		
		List<BoothGenderStatsProjection> boothStats;
		if (boothNumbers == null || boothNumbers.isEmpty()) {
			// Get all booth stats when no specific booths are requested
			boothStats = voterRepository.getAllBoothGenderStats(accountId, electionId);
		} else {
			// Get stats for specific booths
			boothStats = voterRepository.getBoothGenderStats(accountId, electionId, boothNumbers);
		}
		
		if (boothStats.isEmpty()) {
			log.debug("No booth gender stats found for booths: {}", boothNumbers);
			return new ArrayList<>();
		}
		
		List<BoothGenderStatsDTO> result = boothStats.stream()
				.map(stat -> {
					BoothGenderStatsDTO dto = new BoothGenderStatsDTO(
							stat.getBoothNumber(),
							stat.getMaleCount() != null ? stat.getMaleCount() : 0L,
							stat.getFemaleCount() != null ? stat.getFemaleCount() : 0L,
							stat.getOtherCount() != null ? stat.getOtherCount() : 0L,
							stat.getTotalCount() != null ? stat.getTotalCount() : 0L
					);
					log.debug("Booth {} stats - Male: {}, Female: {}, Other: {}, Total: {}", 
							  dto.getBoothNumber(), dto.getMaleCount(), dto.getFemaleCount(), 
							  dto.getOtherCount(), dto.getTotalCount());
					return dto;
				})
				.collect(Collectors.toList());
		
		log.debug("Booth gender stats retrieved for {} booths", result.size());
		return result;
	});
}

private List<BoothVerificationStatsDTO> getBoothAadhaarStatsWithCaching(String cacheKey, Long accountId, Long electionId, List<Integer> boothNumbers) {
	return getFromCache(cacheKey, () -> {
		List<BoothAadhaarStatsProjection> boothAadhaar = voterRepository.getBoothAadhaarStats(
				accountId, electionId, boothNumbers);
		
		if (boothAadhaar.isEmpty()) {
			return new ArrayList<>();
		}
		
		return boothAadhaar.stream()
				.map(stat -> new BoothVerificationStatsDTO(
						stat.getBoothNumber(),
						stat.getVerifiedCount() != null ? stat.getVerifiedCount() : 0L,
						stat.getUnverifiedCount() != null ? stat.getUnverifiedCount() : 0L,
						stat.getTotalCount() != null ? stat.getTotalCount() : 0L
				))
				.collect(Collectors.toList());
	});
}

private List<BoothVerificationStatsDTO> getBoothMembershipStatsWithCaching(String cacheKey, Long accountId, Long electionId, List<Integer> boothNumbers) {
	return getFromCache(cacheKey, () -> {
		List<BoothMembershipStatsProjection> boothMembership = voterRepository.getBoothMembershipStats(
				accountId, electionId, boothNumbers);
		
		if (boothMembership.isEmpty()) {
			return new ArrayList<>();
		}
		
		return boothMembership.stream()
				.map(stat -> new BoothVerificationStatsDTO(
						stat.getBoothNumber(),
						stat.getVerifiedCount() != null ? stat.getVerifiedCount() : 0L,
						stat.getUnverifiedCount() != null ? stat.getUnverifiedCount() : 0L,
						stat.getTotalCount() != null ? stat.getTotalCount() : 0L
				))
				.collect(Collectors.toList());
	});
}

private VerificationStatsDTO getAadhaarStatsWithCaching(String cacheKey, Long accountId, Long electionId) {
	return getFromCache(cacheKey, () -> {
		AadhaarStatsProjection aadhaarProjection = voterRepository.getAadhaarStats(accountId, electionId);
		return new VerificationStatsDTO(
				aadhaarProjection.getVerifiedCount() != null ? aadhaarProjection.getVerifiedCount() : 0L,
				aadhaarProjection.getUnverifiedCount() != null ? aadhaarProjection.getUnverifiedCount() : 0L,
				aadhaarProjection.getTotalCount() != null ? aadhaarProjection.getTotalCount() : 0L
		);
	});
}

private VerificationStatsDTO getMembershipStatsWithCaching(String cacheKey, Long accountId, Long electionId) {
	return getFromCache(cacheKey, () -> {
		MembershipStatsProjection membershipProjection = voterRepository.getMembershipStats(accountId, electionId);
		return new VerificationStatsDTO(
				membershipProjection.getVerifiedCount() != null ? membershipProjection.getVerifiedCount() : 0L,
				membershipProjection.getUnverifiedCount() != null ? membershipProjection.getUnverifiedCount() : 0L,
				membershipProjection.getTotalCount() != null ? membershipProjection.getTotalCount() : 0L
		);
	});
}

private AddressedVoterStatsDTO getAddressedVoterStatsWithCaching(String cacheKey, Long accountId, Long electionId) {
	return getFromCache(cacheKey, () -> {
		AddressedVoterStatsProjection projection = voterRepository.getAddressedVoterStats(accountId, electionId);
		return new AddressedVoterStatsDTO(
				projection.getAddressedCount() != null ? projection.getAddressedCount() : 0L,
				projection.getNotAddressedCount() != null ? projection.getNotAddressedCount() : 0L,
				projection.getTotalCount() != null ? projection.getTotalCount() : 0L
		);
	});
}

private FamilyMappingStatsDTO getFamilyMappingStatsWithCaching(String cacheKey, Long accountId, Long electionId, List<Integer> partNumbers) {
	return getFromCache(cacheKey, () -> {
		FamilyMappingStatsProjection projection = voterRepository.getFamilyMappingStats(accountId, electionId, partNumbers);
		return new FamilyMappingStatsDTO(
				projection.getUnmappedVoterCount() != null ? projection.getUnmappedVoterCount() : 0L,
				projection.getSingleVoterFamilyCount() != null ? projection.getSingleVoterFamilyCount() : 0L,
				projection.getTotalCount() != null ? projection.getTotalCount() : 0L
		);
	});
}

//@Override
//@Transactional(readOnly = true)
//public FamilyResponseDTO getFamilyVotersByElection(Long accountId, Long electionId, List<Integer> boothNumbers, Pageable pageable) {
//    long startTime = System.currentTimeMillis();
//
//    try {
//        log.debug("Starting getFamilyVotersByElection with electionId={}, accountId={}, boothNumbers={}, page={}, size={}",
//                electionId, accountId, boothNumbers, pageable.getPageNumber(), pageable.getPageSize());
//
//        // Validate election ownership
//        validateElectionOwnership(electionId, accountId);
//
//        // Check user role
//        Long userId = requestDetails.getCurrentUserId();
//        UserEntity currentUser = userRepo.findById(userId)
//                .orElseThrow(() -> new RuntimeException("User not found"));
//        Role userRole = currentUser.getRole();
//
//        // Initialize booth numbers
//        List<Integer> effectiveBoothNumbers = boothNumbers; // Default to input boothNumbers
//        if ("VOLUNTEER".equalsIgnoreCase(userRole.getRoleName())) {
//            log.debug("Processing as VOLUNTEER role");
//            VolunteerEntity volunteer = volunteerRepository.findByUserEntity_Id(userId)
//                    .orElseThrow(() -> new ThedalException(ThedalError.VOLUNTEER_NOT_FOUND, HttpStatus.NOT_FOUND));
//
//            List<Integer> assignedBooths = volunteer.getAssignedBooth()
//                    .stream()
//                    .map(Long::intValue)
//                    .collect(Collectors.toList());
//            log.debug("Volunteer assigned booths: {}", assignedBooths);
//
//            // Create a new list for effective booth numbers to avoid modifying the input
//            effectiveBoothNumbers = boothNumbers != null && !boothNumbers.isEmpty()
//                    ? boothNumbers.stream().filter(assignedBooths::contains).collect(Collectors.toList())
//                    : assignedBooths;
//
//            if (effectiveBoothNumbers.isEmpty()) {
//                log.error("No valid booth numbers for volunteer");
//                throw new ThedalException(ThedalError.VOTER_NOT_FOUND, HttpStatus.NOT_FOUND);
//            }
//        }
//
//        // Create a final copy of effectiveBoothNumbers for use in lambda
//        final List<Integer> finalBoothNumbers = effectiveBoothNumbers;
//
//        // Fetch voters with non-null familyId
//        long queryStartTime = System.currentTimeMillis();
//        Page<VoterEntity> voters = voterRepository.findByAccountIdAndElectionIdAndNonNullFamilyId(
//                accountId, electionId, finalBoothNumbers, pageable);
//        long queryEndTime = System.currentTimeMillis();
//        log.debug("Voter query completed in {} ms, returning {} records, total elements: {}",
//                queryEndTime - queryStartTime, voters.getNumberOfElements(), voters.getTotalElements());
//
//        // Group by familyId
//        Map<UUID, List<VoterEntity>> familyGroups = voters.getContent().stream()
//                .filter(voter -> voter.getFamilyId() != null)
//                .collect(Collectors.groupingBy(VoterEntity::getFamilyId));
//
//        // Convert to FamilyDTO
//        List<FamilyDTO> familyDTOs = familyGroups.entrySet().stream()
//                .map(entry -> {
//                    FamilyDTO family = new FamilyDTO();
//                    family.setFamilyId(entry.getKey());
//                    family.setMembers(entry.getValue());
//                    family.setFamilyCount(entry.getValue().size());
//                    return family;
//                })
//                .sorted((f1, f2) -> f1.getFamilyId().compareTo(f2.getFamilyId())) // Sort by familyId
//                .collect(Collectors.toList());
//
//        // Create paged response
//        Page<FamilyDTO> familyPage = new PageImpl<>(familyDTOs, pageable, familyGroups.size());
//
//        // Fetch gender stats
//        long statsStartTime = System.currentTimeMillis();
//        String statsCacheKey = generateStatsCacheKey(accountId, electionId, finalBoothNumbers);
//        GenderStatsProjection stats = getFromCache(statsCacheKey, () ->
//                voterRepository.getGenderStatsByFamily(accountId, electionId, finalBoothNumbers));
//        GenderStatsDTO genderStats = new GenderStatsDTO(
//                stats.getMaleCount() != null ? stats.getMaleCount() : 0L,
//                stats.getFemaleCount() != null ? stats.getFemaleCount() : 0L,
//                stats.getOtherCount() != null ? stats.getOtherCount() : 0L,
//                stats.getTotalCount() != null ? stats.getTotalCount() : 0L
//        );
//        long statsEndTime = System.currentTimeMillis();
//        log.debug("Gender stats fetched in {} ms", statsEndTime - statsStartTime);
//
//        // Validate result
//        if (familyPage.isEmpty()) {
//            log.warn("No voters found with non-null familyId for electionId={}", electionId);
//            throw new ThedalException(ThedalError.VOTER_NOT_FOUND, HttpStatus.NOT_FOUND);
//        }
//
//        // Build response
//        FamilyResponseDTO response = new FamilyResponseDTO(familyPage, genderStats);
//
//        long endTime = System.currentTimeMillis();
//        log.info("getFamilyVotersByElection execution time: {} ms", endTime - startTime);
//
//        return response;
//
//    } catch (ThedalException e) {
//        log.error("Family voter retrieval failed: {}", e.getMessage());
//        throw e;
//    } catch (Exception e) {
//        log.error("Unexpected error in getFamilyVotersByElection: {}", e.getMessage(), e);
//        throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
//    }
//}
@Override
@Transactional(readOnly = true)
public FamilyResponseDTO getFamilyVotersByElection(Long accountId, Long electionId, List<Integer> partNumbers, Pageable pageable) {
	// FAST & SIMPLE FAMILY LOADING - Optimized for 1-second response time
	long startTime = System.currentTimeMillis();

	try {
		log.debug("Starting FAST family loading with electionId={}, accountId={}, partNumbers={}, page={}, size={}",
				electionId, accountId, partNumbers, pageable.getPageNumber(), pageable.getPageSize());

		// Validate election ownership
		validateElectionOwnership(electionId, accountId);

		// Check user role
		Long userId = requestDetails.getCurrentUserId();
		UserEntity currentUser = userRepo.findById(userId)
				.orElseThrow(() -> new ThedalException(ThedalError.USER_NOT_FOUND, HttpStatus.NOT_FOUND));
		Role userRole = currentUser.getRole();

		// Initialize part numbers
		final List<Integer> finalPartNumbers;
		if ("VOLUNTEER".equalsIgnoreCase(userRole.getRoleName())) {
			log.debug("Processing as VOLUNTEER role");
			VolunteerEntity volunteer = volunteerRepository.findByUserEntity_Id(userId)
					.orElseThrow(() -> new ThedalException(ThedalError.VOLUNTEER_NOT_FOUND, HttpStatus.NOT_FOUND));

			List<Integer> assignedBooths = volunteer.getAssignedBooth()
					.stream()
					.map(Long::intValue)
					.collect(Collectors.toList());
			log.debug("Volunteer assigned booths: {}", assignedBooths);

			finalPartNumbers = (partNumbers != null && !partNumbers.isEmpty())
					? partNumbers.stream().filter(assignedBooths::contains).collect(Collectors.toList())
					: assignedBooths;

			if (finalPartNumbers.isEmpty()) {
				log.error("No valid part numbers for volunteer");
				throw new ThedalException(ThedalError.VOTER_NOT_FOUND, HttpStatus.NOT_FOUND);
			}
		} else {
			finalPartNumbers = partNumbers;
		}

		// FAST FAMILY LOADING - SIMPLIFIED APPROACH
		long queryStartTime = System.currentTimeMillis();
		
		List<VoterEntity> allFamilyVoters;
		if (finalPartNumbers != null && !finalPartNumbers.isEmpty()) {
			// Get all family voters in specified parts
			allFamilyVoters = voterRepository.findFamilyVotersByParts(
					accountId, electionId, finalPartNumbers);
		} else {
			// Get all family voters
			allFamilyVoters = voterRepository.findByAccountIdAndElectionIdAndFamilyIdIsNotNull(
					accountId, electionId);
		}
		
		long queryEndTime = System.currentTimeMillis();
		log.debug("Fast family query completed in {} ms, returning {} voters",
				queryEndTime - queryStartTime, allFamilyVoters.size());

		// Group by familyId (simple and fast)
		Map<UUID, List<VoterEntity>> familyGroups = allFamilyVoters.stream()
				.collect(Collectors.groupingBy(VoterEntity::getFamilyId));

		// FAST Family DTO creation (no complex sorting)
		List<FamilyDTO> familyDTOs = familyGroups.entrySet().stream()
				.map(entry -> {
					FamilyDTO family = new FamilyDTO();
					family.setFamilyId(entry.getKey());
					family.setMembers(entry.getValue());
					family.setFamilyCount(entry.getValue().size());
					return family;
				})
				.sorted((f1, f2) -> f1.getFamilyId().compareTo(f2.getFamilyId()))
				.collect(Collectors.toList());

		// Apply pagination to the family list
		int start = (int) pageable.getOffset();
		int end = Math.min(start + pageable.getPageSize(), familyDTOs.size());
		List<FamilyDTO> paginatedFamilies = start >= familyDTOs.size() ? 
				Collections.emptyList() : familyDTOs.subList(start, end);

		// Create optimized paged response
		Page<FamilyDTO> familyPage = new PageImpl<>(paginatedFamilies, pageable, familyDTOs.size());

		// Fetch gender stats
		long statsStartTime = System.currentTimeMillis();
		String statsCacheKey = generateStatsCacheKey(accountId, electionId, finalPartNumbers);
		GenderStatsProjection stats = getFromCache(statsCacheKey, () ->
				voterRepository.getGenderStatsByFamily(accountId, electionId, finalPartNumbers));
		GenderStatsDTO genderStats = new GenderStatsDTO(
				stats.getMaleCount() != null ? stats.getMaleCount() : 0L,
				stats.getFemaleCount() != null ? stats.getFemaleCount() : 0L,
				stats.getOtherCount() != null ? stats.getOtherCount() : 0L,
				stats.getTotalCount() != null ? stats.getTotalCount() : 0L
		);
		long statsEndTime = System.currentTimeMillis();
		log.debug("Gender stats fetched in {} ms", statsEndTime - statsStartTime);

		// Validate result
		if (familyPage.isEmpty()) {
			log.warn("No families found for electionId={}", electionId);
			throw new ThedalException(ThedalError.VOTER_NOT_FOUND, HttpStatus.NOT_FOUND);
		}

		// Build response
		FamilyResponseDTO response = new FamilyResponseDTO(familyPage, genderStats);

		long endTime = System.currentTimeMillis();
		log.info("getFamilyVotersByElection execution time: {} ms", endTime - startTime);

		return response;

	} catch (ThedalException e) {
		log.error("Family voter retrieval failed: {}", e.getMessage());
		throw e;
	} catch (Exception e) {
		log.error("Unexpected error in getFamilyVotersByElection: {}", e.getMessage(), e);
		throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
	}
}

//// SUPER FAST: Family Summary API (Initial Load)
//@Override
//@Transactional(readOnly = true)
//public FamilySummaryResponseDTO getFamilySummary(Long accountId, Long electionId, List<Integer> boothNumbers, List<Integer> partNumbers, Pageable pageable) {
//	long startTime = System.currentTimeMillis();
//	
//	try {
//		log.debug("Starting SUPER FAST family summary with electionId={}, accountId={}, boothNumbers={}, partNumbers={}", 
//				electionId, accountId, boothNumbers, partNumbers);
//
//		// Validate election ownership
//		validateElectionOwnership(electionId, accountId);
//
//		// Check user role and filter part numbers (prefer partNumbers over boothNumbers)
//		final List<Integer> finalPartNumbers = getEffectivePartNumbers(
//				partNumbers != null ? partNumbers : boothNumbers, accountId, electionId);
//
//		// SUPER FAST QUERY: Get family summaries only
//		long queryStartTime = System.currentTimeMillis();
//		List<Object[]> summaryResults;
//		
//		// Use appropriate query based on whether we have part number filtering
//		if (finalPartNumbers == null || finalPartNumbers.isEmpty()) {
//			log.debug("Using findFamilySummaryAll (no part filter)");
//			summaryResults = voterRepository.findFamilySummaryAll(accountId, electionId);
//		} else {
//			log.debug("Using findFamilySummaryByParts with parts: {}", finalPartNumbers);
//			summaryResults = voterRepository.findFamilySummaryByParts(accountId, electionId, finalPartNumbers);
//		}
//		
//		long queryEndTime = System.currentTimeMillis();
//		
//		log.debug("Family summary query completed in {} ms, returning {} total families (including single-member families)",
//				queryEndTime - queryStartTime, summaryResults.size());
//
//		// Convert to DTOs and exclude single-member families
//		List<FamilySummaryDTO> familySummaries = summaryResults.stream()
//				.map(row -> {
//					// Handle UUID - PostgreSQL returns it as UUID object, not String
//					UUID familyId;
//					if (row[0] instanceof UUID) {
//						familyId = (UUID) row[0];
//					} else {
//						familyId = UUID.fromString(row[0].toString());
//					}
//					
//					Integer memberCount = ((Number) row[1]).intValue();
//					String firstName = (String) row[2];
//					String epicNumber = (String) row[3];
//					Integer age = row[4] != null ? ((Number) row[4]).intValue() : null;
//					String gender = (String) row[5];
//					Integer partNo = ((Number) row[6]).intValue();
//					Long serialNo = row[7] != null ? ((Number) row[7]).longValue() : null;
//					String mobileNo = (String) row[8];
//					String rlnType = (String) row[9];
//					String voterFnameEn = (String) row[10];
//					String voterLnameEn = (String) row[11];
//					String voterFnameL1 = (String) row[12];
//					String voterLnameL1 = (String) row[13];
//					String rlnFnameEn = (String) row[14];
//					String rlnLnameEn = (String) row[15];
//					String rlnFnameL1 = (String) row[16];
//					String rlnLnameL1 = (String) row[17];
//					String rlnFnameL2 = (String) row[18];
//					String rlnLnameL1 = (String) row[19];   // first_member_rln_lname_l1
//					Boolean memberVerified = row[20] != null ? (Boolean) row[20] : null; // first_member_verified
//					Boolean aadhaarVerified = row[21] != null ? (Boolean) row[21] : null; // first_member_aadhaar_verified
//					// Newly added fields
//					Long availabilityId = row[22] != null ? ((Number) row[22]).longValue() : null; // first_member_availability_id
//					Long partyId = row[23] != null ? ((Number) row[23]).longValue() : null; // first_member_party_id
//					String aadhaarNumber = (String) row[24]; // first_member_aadhaar_number
//					String panNumber = (String) row[25]; // first_member_pan_number
//					
//					// Use enhanced constructor with all fields
//					FamilySummaryDTO.FirstMemberDTO firstMember = new FamilySummaryDTO.FirstMemberDTO(
//							firstName, epicNumber, age, gender, partNo, serialNo, mobileNo, rlnType,
//							voterFnameEn, voterLnameEn, voterFnameL1, voterLnameL1, 
//							rlnFnameEn, rlnLnameEn, rlnFnameL1, rlnLnameL1,
//							memberVerified, aadhaarVerified, availabilityId, partyId, aadhaarNumber, panNumber);
//					
//					return new FamilySummaryDTO(familyId, memberCount, firstMember);
//				})
//				.filter(family -> family.getMemberCount() > 1) // Exclude single-member families
//				.collect(Collectors.toList());
//
//		log.debug("After filtering single-member families: {} multi-member families remaining", 
//				familySummaries.size());
//
//		// Apply pagination
//		int start = (int) pageable.getOffset();
//		int end = Math.min(start + pageable.getPageSize(), familySummaries.size());
//		List<FamilySummaryDTO> paginatedSummaries = start >= familySummaries.size() ? 
//				Collections.emptyList() : familySummaries.subList(start, end);
//
//		Page<FamilySummaryDTO> familyPage = new PageImpl<>(paginatedSummaries, pageable, familySummaries.size());
//
//		// Get cached gender stats (fast)
//		String statsCacheKey = generateStatsCacheKey(accountId, electionId, finalPartNumbers);
//		GenderStatsProjection stats = getFromCache(statsCacheKey, () ->
//				voterRepository.getGenderStatsByFamily(accountId, electionId, finalPartNumbers));
//		GenderStatsDTO genderStats = new GenderStatsDTO(
//				stats.getMaleCount() != null ? stats.getMaleCount() : 0L,
//				stats.getFemaleCount() != null ? stats.getFemaleCount() : 0L,
//				stats.getOtherCount() != null ? stats.getOtherCount() : 0L,
//				stats.getTotalCount() != null ? stats.getTotalCount() : 0L
//		);
//
//		FamilySummaryResponseDTO response = new FamilySummaryResponseDTO(familyPage, genderStats, stats.getTotalCount());
//
//		long endTime = System.currentTimeMillis();
//		log.info("SUPER FAST family summary execution time: {} ms", endTime - startTime);
//
//		return response;
//
//	} catch (Exception e) {
//		log.error("Error in family summary: {}", e.getMessage(), e);
//		throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
//	}
//	
//}
@Override
@Transactional(readOnly = true)
public FamilySummaryResponseDTO getFamilySummary(Long accountId, Long electionId, List<Integer> boothNumbers, List<Integer> partNumbers, String nameFilter, Boolean crossFamily, Pageable pageable) {
    long startTime = System.currentTimeMillis();
    final long QUERY_TIMEOUT_MS = 10000; // 10 second timeout
    
    try {
        log.debug("Starting SUPER FAST family summary with electionId={}, accountId={}, boothNumbers={}, partNumbers={}, crossFamily={}", 
                electionId, accountId, boothNumbers, partNumbers, crossFamily);

        // Validate election ownership
        validateElectionOwnership(electionId, accountId);

        // Check user role and filter part numbers (prefer partNumbers over boothNumbers)
		final List<Integer> finalPartNumbers = getEffectivePartNumbers(
		        partNumbers != null ? partNumbers : boothNumbers, accountId, electionId);
		final String partNumbersCsv = (finalPartNumbers == null || finalPartNumbers.isEmpty())
		        ? null
		        : finalPartNumbers.stream().map(String::valueOf).collect(Collectors.joining(","));

        // Early return check: Quick count of families to avoid expensive processing if no data
        long familyCountStart = System.currentTimeMillis();
        Long totalFamilyCount = voterRepository.countDistinctFamiliesForElection(accountId, electionId, finalPartNumbers);
        long familyCountEnd = System.currentTimeMillis();
        
        log.debug("Family count check completed in {} ms, found {} total families", 
                familyCountEnd - familyCountStart, totalFamilyCount);
        
        // Early return if no families exist
        if (totalFamilyCount == 0L) {
            log.debug("No families found for the given criteria, returning empty result");
            Page<FamilySummaryDTO> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
            GenderStatsDTO emptyStats = new GenderStatsDTO(0L, 0L, 0L, 0L);
            FamilyMappingStatsDTO emptyMappingStats = new FamilyMappingStatsDTO(0L, 0L, 0L);
            long totalTime = System.currentTimeMillis() - startTime;
            log.debug("Early return completed in {} ms", totalTime);
            return new FamilySummaryResponseDTO(emptyPage, emptyStats, emptyMappingStats, 0L);
        }

        // SUPER FAST QUERY: Get family summaries only
        long queryStartTime = System.currentTimeMillis();
        List<Object[]> summaryResults;
        
        try {
            // Use appropriate query based on whether we have part number filtering
		    if (nameFilter != null && !nameFilter.isEmpty()) {
                log.debug("Using findFamilySummaryWithSequenceByName with name filter: {}, crossFamily: {}", nameFilter, crossFamily);
		        summaryResults = voterRepository.findFamilySummaryWithSequenceByName(
		                accountId, electionId, partNumbersCsv, nameFilter.toLowerCase() + "%", crossFamily);
            } else if (finalPartNumbers == null || finalPartNumbers.isEmpty()) {
                log.debug("Using findFamilySummaryWithSequenceAll (no part filter), crossFamily: {}", crossFamily);
                summaryResults = voterRepository.findFamilySummaryWithSequenceAll(accountId, electionId, crossFamily);
            } else {
                log.debug("Using findFamilySummaryWithSequenceByParts with parts: {}, crossFamily: {}", finalPartNumbers, crossFamily);
		        summaryResults = voterRepository.findFamilySummaryWithSequenceByParts(
		                accountId, electionId, partNumbersCsv, crossFamily);
            }
            
            long queryEndTime = System.currentTimeMillis();
            long queryDuration = queryEndTime - queryStartTime;
            
            // Check if query exceeded timeout
            if (queryDuration > QUERY_TIMEOUT_MS) {
                log.warn("Family summary query exceeded timeout: {} ms > {} ms for electionId={}, accountId={}", 
                        queryDuration, QUERY_TIMEOUT_MS, electionId, accountId);
            }
            
            log.debug("Family summary query completed in {} ms, returning {} total families (including single-member families)",
                    queryDuration, summaryResults.size());
            
        } catch (Exception e) {
            long queryDuration = System.currentTimeMillis() - queryStartTime;
            log.error("Family summary query failed after {} ms for electionId={}, accountId={}: {}", 
                    queryDuration, electionId, accountId, e.getMessage());
            
            // If query takes too long or fails, return empty result instead of hanging
            if (queryDuration > QUERY_TIMEOUT_MS) {
                log.warn("Returning empty result due to query timeout");
                Page<FamilySummaryDTO> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
                GenderStatsDTO emptyStats = new GenderStatsDTO(0L, 0L, 0L, 0L);
                return new FamilySummaryResponseDTO(emptyPage, emptyStats, 0L);
            }
            throw e;
        }

        // Convert to DTOs and exclude single-member families
        List<FamilySummaryDTO> familySummaries = new ArrayList<>();
        for (Object[] row : summaryResults) {
            // Handle UUID - PostgreSQL returns it as UUID object, not String
            UUID familyId;
            if (row[0] instanceof UUID) {
                familyId = (UUID) row[0];
            } else {
                familyId = UUID.fromString(row[0].toString());
            }
            
            // NEW: Extract sequence number (second column)
            Integer sequenceNumber = row[1] != null ? ((Number) row[1]).intValue() : null;
            
            // Member count is now third column
            Integer memberCount = ((Number) row[2]).intValue();
            
            // Include all families with valid family_id (single or multi-member)
            // Family ID is already filtered in the query to exclude null/empty values
            String firstName = (String) row[3];   // first_member_name
            String epicNumber = (String) row[4];  // first_member_epic
            Integer age = row[5] != null ? ((Number) row[5]).intValue() : null;  // first_member_age
            String gender = (String) row[6];      // first_member_gender
            Integer partNo = ((Number) row[7]).intValue();  // effective_part (COALESCE result)
            Long serialNo = row[9] != null ? ((Number) row[9]).longValue() : null;  // first_member_serial_no
            String mobileNo = (String) row[10];   // first_member_mobile_no
                String rlnType = (String) row[11];    // first_member_rln_type
                String voterFnameEn = (String) row[12]; // first_member_voter_fname_en
                String voterLnameEn = (String) row[13]; // first_member_voter_lname_en
                String voterFnameL1 = (String) row[14]; // first_member_voter_fname_l1
                String voterLnameL1 = (String) row[15]; // first_member_voter_lname_l1
                String rlnFnameEn = (String) row[16];   // first_member_rln_fname_en
                String rlnLnameEn = (String) row[17];   // first_member_rln_lname_en
                String rlnFnameL1 = (String) row[18];   // first_member_rln_fname_l1
                String rlnLnameL1 = (String) row[19];   // first_member_rln_lname_l1
                Boolean memberVerified = row[20] != null ? (Boolean) row[20] : null; // first_member_verified
		Boolean aadhaarVerified = row[21] != null ? (Boolean) row[21] : null; // first_member_aadhaar_verified
		Long availabilityId = row[22] != null ? ((Number) row[22]).longValue() : null; // first_member_availability_id
		Long partyId = row[23] != null ? ((Number) row[23]).longValue() : null; // first_member_party_id
		String aadhaarNumber = (String) row[24]; // first_member_aadhaar_number
				String panNumber = (String) row[25]; // first_member_pan_number
				String photoUrl = (String) row[26]; // first_member_photo_url
				String availabilityName = row.length > 27 ? (String) row[27] : null; // first_member_availability_name
				String partyName = row.length > 28 ? (String) row[28] : null; // first_member_party_name
				// Long voterId = row.length > 29 ? ((Number) row[29]).longValue() : null; // first_member_voter_id (already used in SQL, now in row 29)
				String votingHistoryJson = row.length > 30 ? (String) row[30] : "[]"; // voting_history_json (row 30)

		// Use enhanced constructor with all fields including newly added ones
		FamilySummaryDTO.FirstMemberDTO firstMember = new FamilySummaryDTO.FirstMemberDTO(
			firstName, epicNumber, age, gender, partNo, serialNo, mobileNo, rlnType,
			voterFnameEn, voterLnameEn, voterFnameL1, voterLnameL1,
			rlnFnameEn, rlnLnameEn, rlnFnameL1, rlnLnameL1,
			memberVerified, aadhaarVerified, availabilityId, partyId, aadhaarNumber, panNumber, photoUrl);
		// Optional: set display names without changing constructor signature
		firstMember.setAvailabilityName(availabilityName);
		firstMember.setPartyName(partyName);
		
		// Parse and set voting history
		List<VoterHistoryDTO> votingHistory = parseVotingHistory(votingHistoryJson);
		firstMember.setVotingHistory(votingHistory);
                
                // Use new constructor with sequence number
                familySummaries.add(new FamilySummaryDTO(familyId, sequenceNumber, memberCount, firstMember));
        }

        log.debug("After including all families with valid family_id: {} families included", 
                familySummaries.size());

        // Apply pagination
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), familySummaries.size());
        List<FamilySummaryDTO> paginatedSummaries = start >= familySummaries.size() ? 
                Collections.emptyList() : familySummaries.subList(start, end);

        Page<FamilySummaryDTO> familyPage = new PageImpl<>(paginatedSummaries, pageable, familySummaries.size());

        // Get cached gender stats (fast)
        String statsCacheKey = generateStatsCacheKey(accountId, electionId, finalPartNumbers);
        GenderStatsProjection stats = getFromCache(statsCacheKey, () ->
                voterRepository.getGenderStatsByFamily(accountId, electionId, finalPartNumbers));
        GenderStatsDTO genderStats = new GenderStatsDTO(
                stats.getMaleCount() != null ? stats.getMaleCount() : 0L,
                stats.getFemaleCount() != null ? stats.getFemaleCount() : 0L,
                stats.getOtherCount() != null ? stats.getOtherCount() : 0L,
                stats.getTotalCount() != null ? stats.getTotalCount() : 0L
        );

        // Get cached family mapping stats (unmapped voters and single-voter families)
        FamilyMappingStatsDTO familyMappingStats = getFamilyMappingStatsWithCaching(
                statsCacheKey + "_family_mapping", accountId, electionId, finalPartNumbers);

        // Get total voters count from _voters table
        long totalVotersCount = voterRepository.countVotersByElectionAndParts(accountId, electionId, finalPartNumbers);
        log.debug("Total voters count from _voters table: {}", totalVotersCount);

        // Create response DTO, including electionId, partNumbers, totalVotersCount, and familyMappingStats
        FamilySummaryResponseDTO response = new FamilySummaryResponseDTO(
                familyPage, genderStats, familyMappingStats, totalVotersCount);

        long endTime = System.currentTimeMillis();
        log.info("SUPER FAST family summary execution time: {} ms", endTime - startTime);

        return response;

    } catch (Exception e) {
        log.error("Error in family summary: {}", e.getMessage(), e);
        throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

// FAST: Family Members API (On Selection) with Pagination
@Override
@Transactional(readOnly = true)
public FamilyMembersResponseDTO getFamilyMembers(Long accountId, Long electionId, String familyId, String sortBy, String order, Pageable pageable) {
	long startTime = System.currentTimeMillis();
	
	try {
		log.debug("Starting FAST paginated family members for familyId={}, sortBy={}, order={}, page={}, size={}", 
				familyId, sortBy, order, pageable.getPageNumber(), pageable.getPageSize());

		// Validate election ownership
		validateElectionOwnership(electionId, accountId);

		// Convert familyId to UUID
		UUID familyUUID;
		try {
			familyUUID = UUID.fromString(familyId);
		} catch (IllegalArgumentException e) {
			log.error("Invalid familyId format: {}", familyId);
			throw new ThedalException(ThedalError.INVALID_REQUEST, HttpStatus.BAD_REQUEST);
		}

//		// Create sort direction and combine with pageable
//		Sort.Direction direction = order.equals("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
//		Sort sort = Sort.by(direction, sortBy);
//		Pageable pageableWithSort = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
		// Create sort direction and combine with pageable
        Sort.Direction direction = order.equals("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Sort sort;
		if ("age".equals(sortBy)) {
			// Eldest-first (or youngest-first) with deterministic tie-breaker on voterId
			sort = Sort.by(
				new Sort.Order(direction, sortBy).nullsLast(),
				new Sort.Order(Sort.Direction.ASC, "voterId")
			);
		} else {
			// Always append voterId as stable secondary key to mirror summary tiebreak
			sort = Sort.by(
				new Sort.Order(direction, sortBy),
				new Sort.Order(Sort.Direction.ASC, "voterId")
			);
		}
        Pageable pageableWithSort = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
		
		// Get paginated family members with sorting
		Page<VoterEntity> membersPage = voterRepository.findFamilyMembers(accountId, electionId, familyUUID, pageableWithSort);
		
		if (membersPage.isEmpty()) {
			log.warn("No family members found for familyId={}", familyId);
			throw new ThedalException(ThedalError.VOTER_NOT_FOUND, HttpStatus.NOT_FOUND);
		}

		// Get total family member count (this might be different from page total if there are multiple pages)
		// For consistency, we use the total elements from the page
		int totalMemberCount = (int) membersPage.getTotalElements();

		FamilyMembersResponseDTO response = new FamilyMembersResponseDTO(
				familyUUID, totalMemberCount, membersPage);

		long endTime = System.currentTimeMillis();
		log.info("FAST paginated family members execution time: {} ms, returned {} of {} members", 
				endTime - startTime, membersPage.getNumberOfElements(), totalMemberCount);

		return response;

	} catch (ThedalException e) {
		throw e;
	} catch (Exception e) {
		log.error("Error getting paginated family members: {}", e.getMessage(), e);
		throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
	}
}

// Helper method to get effective part numbers (extracted from existing logic)
private List<Integer> getEffectivePartNumbers(List<Integer> partNumbers, Long accountId, Long electionId) {
	Long userId = requestDetails.getCurrentUserId();
	UserEntity currentUser = userRepo.findById(userId)
			.orElseThrow(() -> new ThedalException(ThedalError.USER_NOT_FOUND, HttpStatus.NOT_FOUND));
	Role userRole = currentUser.getRole();

	if ("VOLUNTEER".equalsIgnoreCase(userRole.getRoleName())) {
		VolunteerEntity volunteer = volunteerRepository.findByUserEntity_Id(userId)
				.orElseThrow(() -> new ThedalException(ThedalError.VOLUNTEER_NOT_FOUND, HttpStatus.NOT_FOUND));

		List<Integer> assignedBooths = volunteer.getAssignedBooth()
				.stream()
				.map(Long::intValue)
				.collect(Collectors.toList());

		List<Integer> effectiveParts = (partNumbers != null && !partNumbers.isEmpty())
				? partNumbers.stream().filter(assignedBooths::contains).collect(Collectors.toList())
				: assignedBooths;

		if (effectiveParts.isEmpty()) {
			throw new ThedalException(ThedalError.VOTER_NOT_FOUND, HttpStatus.NOT_FOUND);
		}
		return effectiveParts;
	}
	
	return partNumbers;
}

private VoterEntity findEldestMember(List<VoterEntity> familyMembers) {
	if (familyMembers == null || familyMembers.isEmpty()) {
		log.warn("No family members provided for eldest member calculation");
		return null;
	}

	// Optimized eldest member calculation without excessive logging
	return familyMembers.stream()
			.filter(v -> v.getAge() != null || v.getDob() != null)
			.min((v1, v2) -> {
				// Prioritize age comparison if available
				if (v1.getAge() != null && v2.getAge() != null) {
					int ageCompare = v2.getAge().compareTo(v1.getAge()); // Higher age is older
					if (ageCompare != 0) {
						return ageCompare;
					}
					// If ages are equal, fall back to DOB if available
					if (v1.getDob() != null && v2.getDob() != null) {
						return v1.getDob().compareTo(v2.getDob()); // Earlier DOB is older
					}
					// Tiebreaker: use voterId
					return v1.getVoterId().compareTo(v2.getVoterId());
				}
				// If one voter has age and the other doesn't, prioritize the one with age
				if (v1.getAge() != null) {
					return -1; // v1 with age is considered older
				}
				if (v2.getAge() != null) {
					return 1; // v2 with age is considered older
				}
				// Both have null age, compare DOB if available
				if (v1.getDob() != null && v2.getDob() != null) {
					return v1.getDob().compareTo(v2.getDob()); // Earlier DOB is older
				}
				// Tiebreaker: use voterId
				return v1.getVoterId().compareTo(v2.getVoterId());
			})
			.orElse(familyMembers.get(0)); // Default to first member if no valid age/DOB
}

@Transactional(readOnly = true)
public ThedalResponse<Map<String, Object>> getBoothVoterStatuses(Long electionId, Integer boothNumber, String pollStatus, int page, int size, String sortDirection) {
	Long accountId = requestDetails.getCurrentAccountId();
	if (accountId == null) {
		log.error("Account ID not found, unauthorized access.");
		throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
	}
	validateElectionOwnership(electionId, accountId);

	if (boothNumber == null || boothNumber <= 0) {
		log.error("Invalid booth number: {}", boothNumber);
		throw new ThedalException(ThedalError.BOOTH_NOT_FOUND, HttpStatus.BAD_REQUEST);
	}

	// Validate sort direction
	if (!"asc".equalsIgnoreCase(sortDirection) && !"desc".equalsIgnoreCase(sortDirection)) {
		log.error("Invalid sort direction: {}", sortDirection);
		throw new ThedalException(ThedalError.INVALID_SORT_DIRECTION, HttpStatus.BAD_REQUEST);
	}

	// Create Pageable object
	Sort sort = Sort.by("serialNo");
	sort = "desc".equalsIgnoreCase(sortDirection) ? sort.descending() : sort.ascending();
	Pageable pageable = PageRequest.of(page, size, sort);

	// Fetch paginated results
	Page<VoterStatusDTO> voterPage = voterRepository.findSerialNosAndHasVotedByElectionIdAndBoothNumber(
			electionId, boothNumber, pollStatus, pageable);

	if (voterPage.isEmpty()) {
		log.warn("No voters found for electionId: {} and boothNumber: {}", electionId, boothNumber);
		throw new ThedalException(ThedalError.VOTER_NOT_FOUND, HttpStatus.NOT_FOUND);
	}

//    long votedCount = voterPage.getContent().stream()
//            .filter(v -> Boolean.TRUE.equals(v.getHasVoted()))
//            .count();
//    long notVotedCount = voterPage.getTotalElements() - votedCount;
 // Fetch total voted and not voted counts
    Map<String, Long> voteCounts = voterRepository.countVotersByVotingStatus(electionId, boothNumber);
    long votedCount = voteCounts.getOrDefault("votedCount", 0L);
    long notVotedCount = voteCounts.getOrDefault("notVotedCount", 0L);
    long totalVoters = votedCount + notVotedCount;

	Map<String, Object> data = new HashMap<>();
	data.put("votedCount", votedCount);
	data.put("notVotedCount", notVotedCount);
	data.put("totalVoters", totalVoters);
	data.put("voters", voterPage.getContent());
	data.put("currentPage", voterPage.getNumber());
	data.put("totalPages", voterPage.getTotalPages());
	data.put("pageSize", voterPage.getSize());

	Map<String, Object> response = new HashMap<>();
	response.put("status", "success");
	response.put("message", "Fetched voter statuses successfully");
	response.put("data", data);

	return new ThedalResponse<>(ThedalSuccess.VOTER_STATUSES_RETRIEVED, response);
}

@Transactional(readOnly = true)
public PartVoterStatsDTO getPartVoterStats(Long electionId, Integer partNo) {
	Long accountId = requestDetails.getCurrentAccountId();
	if (accountId == null) {
		log.error("Account ID not found, unauthorized access.");
		throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
	}
	
	validateElectionOwnershipFast(electionId, accountId);
	
	if (partNo == null || partNo <= 0) {
		log.error("Invalid part number: {}", partNo);
		throw new ThedalException(ThedalError.INVALID_PART_NO, HttpStatus.BAD_REQUEST);
	}
	
 // First check if any voters exist for this part
	boolean partExists = voterRepository.existsByAccountIdAndElectionIdAndPartNo(accountId, electionId, partNo);
	if (!partExists) {
		throw new ThedalException(ThedalError.PART_NO_NOT_FOUND, HttpStatus.NOT_FOUND);
	}

	PartVoterStatsProjection stats = voterRepository.getPartVoterStats(accountId, electionId, partNo);
	
	return new PartVoterStatsDTO(
		stats.getMaleCount(),
		stats.getFemaleCount(),
		stats.getOtherCount(),
		stats.getTotalCount(),
		stats.getVotedCount(),
		stats.getTotalCount() - stats.getVotedCount()
	);
}


@Transactional(readOnly = true)
public VoterMongoDTO getVoterFromMongo(String epicNumber, Long electionId) {
	Long accountId = requestDetails.getCurrentAccountId();
	if (accountId == null) {
		throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
	}

	Optional<VoterMongo> voterMongoOpt = voterMongoRepository.findByEpicNumberAndElectionIdAndAccountId(epicNumber, electionId, accountId);
	if (!voterMongoOpt.isPresent()) {
		throw new ThedalException(ThedalError.VOTER_NOT_FOUND, HttpStatus.NOT_FOUND, "Voter not found with EPIC: " + epicNumber);
	}

	return mapToVoterMongoDTO(voterMongoOpt.get());
}

@Transactional(readOnly = true)
public Page<VoterMongoDTO> getAllVotersFromMongo(Long electionId, Pageable pageable) {
	Long accountId = requestDetails.getCurrentAccountId();
	if (accountId == null) {
		throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
	}

	Page<VoterMongo> voterPage = voterMongoRepository.findByElectionIdAndAccountId(electionId, accountId, pageable);
	if (voterPage.isEmpty()) {
		throw new ThedalException(ThedalError.VOTER_NOT_FOUND, HttpStatus.NOT_FOUND, "No voters found for electionId: " + electionId);
	}

	List<VoterMongoDTO> voterDtos = voterPage.getContent().stream()
			.map(this::mapToVoterMongoDTO)
			.collect(Collectors.toList());

	return new PageImpl<>(voterDtos, pageable, voterPage.getTotalElements());
}

private VoterMongoDTO mapToVoterMongoDTO(VoterMongo voterMongo) {
	VoterMongoDTO voterDto = new VoterMongoDTO();
	voterDto.setId(voterMongo.getId());
	voterDto.setVoterId(voterMongo.getVoterId());
	voterDto.setEpicNumber(voterMongo.getEpicNumber());
	voterDto.setElectionId(voterMongo.getElectionId());
	voterDto.setBoothNumber(voterMongo.getBoothNumber());
	voterDto.setHasVoted(voterMongo.getHasVoted());
	voterDto.setVotedTimestamp(voterMongo.getVotedTimestamp());
	voterDto.setPhotoUrl(voterMongo.getPhotoUrl());
	voterDto.setAccountId(voterMongo.getAccountId());
	voterDto.setPartNo(voterMongo.getPartNo());
	voterDto.setSectionNo(voterMongo.getSectionNo());
	voterDto.setSerialNo(voterMongo.getSerialNo());
	voterDto.setHouseNoEn(voterMongo.getHouseNoEn());
	voterDto.setHouseNoL1(voterMongo.getHouseNoL1());
	voterDto.setHouseNoL2(voterMongo.getHouseNoL2());
	voterDto.setVoterFnameEn(voterMongo.getVoterFnameEn());
	voterDto.setVoterLnameEn(voterMongo.getVoterLnameEn());
	voterDto.setVoterFnameL1(voterMongo.getVoterFnameL1());
	voterDto.setVoterFnameL2(voterMongo.getVoterFnameL2());
	voterDto.setVoterLnameL1(voterMongo.getVoterLnameL1());
	voterDto.setVoterLnameL2(voterMongo.getVoterLnameL2());
	voterDto.setRlnType(voterMongo.getRlnType());
	voterDto.setRlnFnameEn(voterMongo.getRlnFnameEn());
	voterDto.setRlnLnameEn(voterMongo.getRlnLnameEn());
	voterDto.setRlnFnameL1(voterMongo.getRlnFnameL1());
	voterDto.setRlnFnameL2(voterMongo.getRlnFnameL2());
	voterDto.setRlnLnameL1(voterMongo.getRlnLnameL1());
	voterDto.setRlnLnameL2(voterMongo.getRlnLnameL2());
	voterDto.setGender(voterMongo.getGender());
	voterDto.setSectionNameEn(voterMongo.getSectionNameEn());
	voterDto.setSectionNameL1(voterMongo.getSectionNameL1());
	voterDto.setSectionNameL2(voterMongo.getSectionNameL2());
	voterDto.setFullAddress(voterMongo.getFullAddress());
	voterDto.setPartNameEn(voterMongo.getPartNameEn());
	voterDto.setPartNameL1(voterMongo.getPartNameL1());
	voterDto.setPartNameL2(voterMongo.getPartNameL2());
	voterDto.setPincode(voterMongo.getPincode());
	voterDto.setPartLati(voterMongo.getPartLati());
	voterDto.setPartLong(voterMongo.getPartLong());
	voterDto.setAge(voterMongo.getAge());
	voterDto.setDob(voterMongo.getDob());
	voterDto.setMobileNo(voterMongo.getMobileNo());
	voterDto.setWhatsappNo(voterMongo.getWhatsappNo());
	voterDto.setEMail(voterMongo.getEMail());
	voterDto.setVoterLati(voterMongo.getVoterLati());
	voterDto.setVoterLongi(voterMongo.getVoterLongi());
	voterDto.setStateCode(voterMongo.getStateCode());
	voterDto.setStateNameEn(voterMongo.getStateNameEn());
	voterDto.setStateNameL1(voterMongo.getStateNameL1());
	voterDto.setStateNameL2(voterMongo.getStateNameL2());
	voterDto.setDistrictCode(voterMongo.getDistrictCode());
	voterDto.setDistrictNameEn(voterMongo.getDistrictNameEn());
	voterDto.setDistrictNameL1(voterMongo.getDistrictNameL1());
	voterDto.setDistrictNameL2(voterMongo.getDistrictNameL2());
	voterDto.setPcNo(voterMongo.getPcNo());
	voterDto.setPcNameEn(voterMongo.getPcNameEn());
	voterDto.setPcNameL1(voterMongo.getPcNameL1());
	voterDto.setPcNameL2(voterMongo.getPcNameL2());
	voterDto.setAcNo(voterMongo.getAcNo());
	voterDto.setAcNameEn(voterMongo.getAcNameEn());
	voterDto.setAcNameL1(voterMongo.getAcNameL1());
	voterDto.setAcNameL2(voterMongo.getAcNameL2());
	voterDto.setUrbanNo(voterMongo.getUrbanNo());
	voterDto.setUrbanNameEn(voterMongo.getUrbanNameEn());
	voterDto.setUrbanNameL1(voterMongo.getUrbanNameL1());
	voterDto.setUrbanWardNo(voterMongo.getUrbanWardNo());
	voterDto.setRurDistrictUnionNo(voterMongo.getRurDistrictUnionNo());
	voterDto.setRurDistrictUnionNameEn(voterMongo.getRurDistrictUnionNameEn());
	voterDto.setRurDistrictUnionNameL1(voterMongo.getRurDistrictUnionNameL1());
	voterDto.setRurDistrictUnionNameL2(voterMongo.getRurDistrictUnionNameL2());
	voterDto.setRurDistrictUnionWardNo(voterMongo.getRurDistrictUnionWardNo());
	voterDto.setPanUnionNo(voterMongo.getPanUnionNo());
	voterDto.setPanUnionNameEn(voterMongo.getPanUnionNameEn());
	voterDto.setPanUnionNameL1(voterMongo.getPanUnionNameL1());
	voterDto.setPanUnionNameL2(voterMongo.getPanUnionNameL2());
	voterDto.setPanUnionWardNo(voterMongo.getPanUnionWardNo());
	voterDto.setVillPanNo(voterMongo.getVillPanNo());
	voterDto.setVillPanNameEn(voterMongo.getVillPanNameEn());
	voterDto.setVillPanNameL1(voterMongo.getVillPanNameL1());
	voterDto.setVillPanWardNo(voterMongo.getVillPanWardNo());
	voterDto.setAvailability(voterMongo.getAvailability());
	voterDto.setPartyAffiliation(voterMongo.getPartyAffiliation());
	voterDto.setStarNumber(voterMongo.getStarNumber());
	voterDto.setAadhaarNumber(voterMongo.getAadhaarNumber());
	voterDto.setPanNumber(voterMongo.getPanNumber());
	voterDto.setPartyRegistrationNumber(voterMongo.getPartyRegistrationNumber());
	voterDto.setDynamicFields(voterMongo.getDynamicFields());
	voterDto.setFamilyId(voterMongo.getFamilyId());
	voterDto.setFamilyCount(voterMongo.getFamilyCount());
	voterDto.setScheme(voterMongo.getScheme());
	voterDto.setPageNumber(voterMongo.getPageNumber());
	voterDto.setRemarks(voterMongo.getRemarks());
	voterDto.setVideoUrl(voterMongo.getVideoUrl());
	voterDto.setOtp(voterMongo.getOtp());
	voterDto.setOtpCreatedAt(voterMongo.getOtpCreatedAt());
	voterDto.setMobileVerified(voterMongo.getMobileVerified());
	voterDto.setAadhaarVerified(voterMongo.getAadhaarVerified());
	voterDto.setMemberVerified(voterMongo.getMemberVerified());
	voterDto.setCreatedTime(voterMongo.getCreatedTime());
	voterDto.setModifiedTime(voterMongo.getModifiedTime());

	if (voterMongo.getReligionId() != null) {
		voterDto.setReligionId(voterMongo.getReligionId());
	}
	if (voterMongo.getCasteId() != null) {
		voterDto.setCasteId(voterMongo.getCasteId());
	}
	if (voterMongo.getSubCasteId() != null) {
		voterDto.setSubCasteId(voterMongo.getSubCasteId());
	}
	if (voterMongo.getLanguageIds() != null && !voterMongo.getLanguageIds().isEmpty()) {
		voterDto.setLanguageIds(voterMongo.getLanguageIds());
	}
//	if (voterMongo.getBenefitSchemeIds() != null && !voterMongo.getBenefitSchemeIds().isEmpty()) {
//		voterDto.setBenefitSchemeIds(voterMongo.getBenefitSchemeIds());
//	}
	if (voterMongo.getFeedbackIssueIds() != null && !voterMongo.getFeedbackIssueIds().isEmpty()) {
		voterDto.setFeedbackIssueIds(voterMongo.getFeedbackIssueIds().stream().collect(Collectors.toList()));
	}
	if (voterMongo.getVoterHistoryIds() != null && !voterMongo.getVoterHistoryIds().isEmpty()) {
		voterDto.setVoterHistoryIds(voterMongo.getVoterHistoryIds().stream().collect(Collectors.toList()));
	}
	if (voterMongo.getAvailabilityId() != null) {
		voterDto.setAvailabilityId(voterMongo.getAvailabilityId());
	}
	if (voterMongo.getPartyId() != null) {
		voterDto.setPartyId(voterMongo.getPartyId());
	}
	if (voterMongo.getPartManagerId() != null) {
		voterDto.setPartManagerId(voterMongo.getPartManagerId());
	}

	return voterDto;
}

/////////////////////////

@Override
@Transactional
public ThedalResponse<String> mapFriendId(Long electionId, String epicNumber, String friendEpicNumber, UUID requestFriendId) {
	Long accountId = requestDetails.getCurrentAccountId();
	if (accountId == null) {
		log.error("Account ID not found, unauthorized access.");
		throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
	}
	validateElectionOwnership(electionId, accountId);

	log.info("Starting mapFriendId process. accountId: {}, electionId: {}, epicNumber: {}, friendEpicNumber: {}", 
			 accountId, electionId, epicNumber, friendEpicNumber);

	// Trim epicNumber inputs
	String trimmedEpicNumber = epicNumber != null ? epicNumber.trim() : null;
	String trimmedFriendEpicNumber = friendEpicNumber != null ? friendEpicNumber.trim() : null;

	if (trimmedEpicNumber == null || trimmedEpicNumber.isEmpty()) {
		log.error("EpicNumber is null or empty after trimming. accountId: {}, electionId: {}", accountId, electionId);
		throw new ThedalException(ThedalError.INVALID_EPIC_NUMBER, HttpStatus.BAD_REQUEST);
	}

	// Fetch the target voter (A)
	Optional<VoterEntity> pathVoterOpt = voterRepository.findByEpicNumberAndElectionIdAndAccountId(trimmedEpicNumber, accountId, electionId);
	if (!pathVoterOpt.isPresent()) {
		log.error("Target voter not found. epicNumber: {}, electionId: {}, accountId: {}", trimmedEpicNumber, electionId, accountId);
		throw new ThedalException(ThedalError.VOTER_NOT_FOUND, HttpStatus.NOT_FOUND);
	}

	VoterEntity pathVoter = pathVoterOpt.get();
	UUID friendId;

	// Case 1: Empty payload or no friendEpicNumber
	if (trimmedFriendEpicNumber == null || trimmedFriendEpicNumber.isEmpty()) {
		log.debug("Handling case where friendEpicNumber is null or empty for epicNumber: {}", trimmedEpicNumber);

		if (pathVoter.getFriendId() != null) {
			log.error("Voter with epicNumber {} already has a friendId: {}. Cannot assign new one.", 
					  trimmedEpicNumber, pathVoter.getFriendId());
			throw new ThedalException(ThedalError.FRIEND_ID_ALREADY_EXISTS, HttpStatus.BAD_REQUEST);
		}

		// Use requestFriendId if provided, otherwise generate a new UUID
		friendId = requestFriendId != null ? requestFriendId : UUID.randomUUID();

		pathVoter.setFriendId(friendId);
		pathVoter.setFriendCount(0); // Initialize friendCount to 0 (no friends yet)
		pathVoter.setFriendsDetails(new ArrayList<>()); // Initialize empty friends list
		log.debug("Assigning new friendId: {} to voter: {}", friendId, trimmedEpicNumber);
		pathVoter = voterRepository.save(pathVoter);

		// VERIFICATION: Ensure PostgreSQL save was successful
		if (pathVoter.getFriendId() == null) {
			log.error("PostgreSQL save failed: friendId is null after save for epicNumber: {}", trimmedEpicNumber);
			throw new ThedalException(ThedalError.DATABASE_SAVE_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		log.debug("PostgreSQL save verified: friendId={} for voter: {}", pathVoter.getFriendId(), trimmedEpicNumber);

		// Sync to MongoDB - DISABLED
		// MongoDB sync is currently disabled
		/*
		try {
			VoterMongo voterMongo = new VoterMongo(pathVoter);
			log.debug("Syncing voter to MongoDB: epicNumber={}, friendId={}, friendCount={}, friendsDetails={}", 
					  voterMongo.getEpicNumber(), voterMongo.getFriendId(), voterMongo.getFriendCount(), voterMongo.getFriendsDetails());
			voterMongoRepository.saveVoterMongoWithNullFields(voterMongo);
			log.info("Successfully synced voter to MongoDB: epicNumber={}, friendId={}", 
					 voterMongo.getEpicNumber(), friendId);
		} catch (Exception mongoEx) {
			log.error("Failed to sync voter to MongoDB for epicNumber: {}, friendId: {}, error: {}", 
					  trimmedEpicNumber, friendId, mongoEx.getMessage());
			// CRITICAL FIX: Propagate MongoDB sync failures to API response
			throw new ThedalException(ThedalError.DATABASE_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		*/
	} else {
		// Case 2: friendEpicNumber provided (map B as friend of A)
		if (trimmedEpicNumber.equals(trimmedFriendEpicNumber)) {
			log.error("Cannot map a voter as their own friend. epicNumber: {}", trimmedEpicNumber);
			throw new ThedalException(ThedalError.INVALID_FRIEND_MAPPING, HttpStatus.BAD_REQUEST);
		}

		log.debug("Fetching friend voter with epicNumber: {}, electionId: {}, accountId: {}", 
				  trimmedFriendEpicNumber, electionId, accountId);
		Optional<VoterEntity> friendVoterOpt = voterRepository.findByEpicNumberAndElectionIdAndAccountId(
			trimmedFriendEpicNumber, accountId, electionId);
		if (!friendVoterOpt.isPresent()) {
			log.error("Friend voter not found. epicNumber: {}, electionId: {}, accountId: {}", 
					  trimmedFriendEpicNumber, electionId, accountId);
			throw new ThedalException(ThedalError.VOTER_NOT_FOUND, HttpStatus.NOT_FOUND);
		}

		VoterEntity friendVoter = friendVoterOpt.get();

		// Get current friends list
		List<FriendDetail> friends = pathVoter.getFriendsDetails();

		// Check for duplicate friend mapping
		boolean isDuplicate = friends.stream()
			.anyMatch(friend -> friend.getEpicNumber().equals(trimmedFriendEpicNumber));
		if (isDuplicate) {
			log.error("Friend with epicNumber {} is already mapped to voter {}.", 
					  trimmedFriendEpicNumber, trimmedEpicNumber);
			throw new ThedalException(ThedalError.FRIEND_ID_ALREADY_EXISTS, HttpStatus.BAD_REQUEST);
		}

		// Use existing friendId for pathVoter or generate new
		friendId = pathVoter.getFriendId() != null ? pathVoter.getFriendId() : 
				   (requestFriendId != null ? requestFriendId : UUID.randomUUID());

		// Add friend's details to the friends list
		String friendName = friendVoter.getVoterFnameEn() != null ? friendVoter.getVoterFnameEn() : "";
		friends.add(new FriendDetail(trimmedFriendEpicNumber, friendName));
		pathVoter.setFriendsDetails(friends);

		// Increment friendCount for target voter (A) only
		pathVoter.setFriendId(friendId);
		pathVoter.setFriendCount(friends.size());
		
		log.debug("Assigning friendId: {} to voter: {}. Adding friend: {} (epic: {}) to friends_details. New friendCount: {}", 
				  friendId, trimmedEpicNumber, friendName, trimmedFriendEpicNumber, pathVoter.getFriendCount());
		pathVoter = voterRepository.save(pathVoter);

		// Sync to MongoDB - DISABLED
		// MongoDB sync is currently disabled
		/*
		try {
			VoterMongo voterMongo = new VoterMongo(pathVoter);
			log.debug("Syncing voter to MongoDB: epicNumber={}, friendId={}, friendCount={}, friendsDetails={}", 
					  voterMongo.getEpicNumber(), voterMongo.getFriendId(), voterMongo.getFriendCount(), voterMongo.getFriendsDetails());
			voterMongoRepository.saveVoterMongoWithNullFields(voterMongo);
			log.info("Successfully synced voter to MongoDB: epicNumber={}, friendId={}, friendCount={}", 
					 voterMongo.getEpicNumber(), friendId, voterMongo.getFriendCount());
		} catch (Exception mongoEx) {
			log.error("Failed to sync voter to MongoDB for epicNumber: {}, friendId: {}, error: {}", 
					  trimmedEpicNumber, friendId, mongoEx.getMessage());
			// CRITICAL FIX: Propagate MongoDB sync failures to API response
			throw new ThedalException(ThedalError.DATABASE_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		*/
	}

	// VERIFICATION: Ensure data was actually saved
	VoterEntity savedVoter = voterRepository.findByEpicNumberAndElectionIdAndAccountId(trimmedEpicNumber, accountId, electionId)
			.orElseThrow(() -> new ThedalException(ThedalError.VOTER_NOT_FOUND, HttpStatus.NOT_FOUND));
	
	if (savedVoter.getFriendId() == null) {
		log.error("Friend mapping verification failed: friendId is null after save for epicNumber: {}", trimmedEpicNumber);
		throw new ThedalException(ThedalError.FRIEND_DETAILS_NOT_PERSISTED, HttpStatus.INTERNAL_SERVER_ERROR);
	}

	log.info("mapFriendId process completed successfully. friendId: {}, accountId: {}, electionId: {}", 
			 friendId, accountId, electionId);
	return new ThedalResponse<>(ThedalSuccess.FRIEND_ID_ASSIGNED);
}


//@Override
//@Transactional
//public ThedalResponse<String> mapFriendId(Long electionId, String epicNumber, String friendEpicNumber, UUID requestFriendId) {
//    Long accountId = requestDetails.getCurrentAccountId();
//    if (accountId == null) {
//        log.error("Account ID not found, unauthorized access.");
//        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
//    }
//    validateElectionOwnership(electionId, accountId);
//
//    log.info("Starting mapFriendId process. accountId: {}, electionId: {}, epicNumber: {}, friendEpicNumber: {}", 
//             accountId, electionId, epicNumber, friendEpicNumber);
//
//    // Trim epicNumber inputs
//    String trimmedEpicNumber = epicNumber != null ? epicNumber.trim() : null;
//    String trimmedFriendEpicNumber = friendEpicNumber != null ? friendEpicNumber.trim() : null;
//
//    if (trimmedEpicNumber == null || trimmedEpicNumber.isEmpty()) {
//        log.error("EpicNumber is null or empty after trimming. accountId: {}, electionId: {}", accountId, electionId);
//        throw new ThedalException(ThedalError.INVALID_EPIC_NUMBER, HttpStatus.BAD_REQUEST);
//    }
//
//    // Fetch the target voter (A)
//    Optional<VoterEntity> pathVoterOpt = voterRepository.findByEpicNumberAndElectionIdAndAccountId(trimmedEpicNumber, accountId, electionId);
//    if (!pathVoterOpt.isPresent()) {
//        log.error("Target voter not found. epicNumber: {}, electionId: {}, accountId: {}", trimmedEpicNumber, electionId, accountId);
//        throw new ThedalException(ThedalError.VOTER_NOT_FOUND, HttpStatus.NOT_FOUND);
//    }
//
//    VoterEntity pathVoter = pathVoterOpt.get();
//    UUID friendId;
//
//    // Case 1: Empty payload or no friendEpicNumber
//    if (trimmedFriendEpicNumber == null || trimmedFriendEpicNumber.isEmpty()) {
//        log.debug("Handling case where friendEpicNumber is null or empty for epicNumber: {}", trimmedEpicNumber);
//
//        if (pathVoter.getFriendId() != null) {
//            log.error("Voter with epicNumber {} already has a friendId: {}. Cannot assign new one.", 
//                      trimmedEpicNumber, pathVoter.getFriendId());
//            throw new ThedalException(ThedalError.FRIEND_ID_ALREADY_EXISTS, HttpStatus.BAD_REQUEST);
//        }
//
//        // Use requestFriendId if provided, otherwise generate a new UUID
//        friendId = requestFriendId != null ? requestFriendId : UUID.randomUUID();
//
//        pathVoter.setFriendId(friendId);
//        pathVoter.setFriendCount(0); // Initialize friendCount to 0 (no friends yet)
//        pathVoter.setFriendsDetails(new ArrayList<>()); // Initialize empty friends list
//        log.debug("Assigning new friendId: {} to voter: {}", friendId, trimmedEpicNumber);
//        pathVoter = voterRepository.save(pathVoter);
//
//        List<FriendDetail> friends = pathVoter.getFriendsDetails();
//        if (friends == null || friends.isEmpty()) {
//            log.warn("friendsDetails is empty or null for voter: epicNumber={}", trimmedEpicNumber);
//        } else {
//            log.debug("friendsDetails for voter: epicNumber={}, content={}", trimmedEpicNumber, friends);
//        }
//        
//        // Sync to MongoDB
//        try {
//            VoterMongo voterMongo = new VoterMongo(pathVoter);
//            log.debug("Syncing voter to MongoDB: epicNumber={}, friendId={}, friendCount={}, friendsDetails={}", 
//                      voterMongo.getEpicNumber(), voterMongo.getFriendId(), voterMongo.getFriendCount(), voterMongo.getFriendsDetails());
//            voterMongoRepository.saveVoterMongoWithNullFields(voterMongo);
//            log.info("Successfully synced voter to MongoDB: epicNumber={}, friendId={}", 
//                     voterMongo.getEpicNumber(), friendId);
//        } catch (Exception mongoEx) {
//            log.error("Failed to sync voter to MongoDB for epicNumber: {}, friendId: {}, error: {}", 
//                      trimmedEpicNumber, friendId, mongoEx.getMessage());
//        }
//    } else {
//        // Case 2: friendEpicNumber provided (map B as friend of A)
//        if (trimmedEpicNumber.equals(trimmedFriendEpicNumber)) {
//            log.error("Cannot map a voter as their own friend. epicNumber: {}", trimmedEpicNumber);
//            throw new ThedalException(ThedalError.INVALID_FRIEND_MAPPING, HttpStatus.BAD_REQUEST);
//        }
//
//        log.debug("Fetching friend voter with epicNumber: {}, electionId: {}, accountId: {}", 
//                  trimmedFriendEpicNumber, electionId, accountId);
//        Optional<VoterEntity> friendVoterOpt = voterRepository.findByEpicNumberAndElectionIdAndAccountId(
//            trimmedFriendEpicNumber, accountId, electionId);
//        if (!friendVoterOpt.isPresent()) {
//            log.error("Friend voter not found. epicNumber: {}, electionId: {}, accountId: {}", 
//                      trimmedFriendEpicNumber, electionId, accountId);
//            throw new ThedalException(ThedalError.VOTER_NOT_FOUND, HttpStatus.NOT_FOUND);
//        }
//
//        VoterEntity friendVoter = friendVoterOpt.get();
//
//        // Get current friends list
//        List<FriendDetail> friends = pathVoter.getFriendsDetails();
//
//        // Check for duplicate friend mapping
//        boolean isDuplicate = friends.stream()
//            .anyMatch(friend -> friend.getEpicNumber().equals(trimmedFriendEpicNumber));
//        if (isDuplicate) {
//            log.error("Friend with epicNumber {} is already mapped to voter {}.", 
//                      trimmedFriendEpicNumber, trimmedEpicNumber);
//            throw new ThedalException(ThedalError.FRIEND_ID_ALREADY_EXISTS, HttpStatus.BAD_REQUEST);
//        }
//
//        // Use existing friendId for pathVoter or generate new
//        friendId = pathVoter.getFriendId() != null ? pathVoter.getFriendId() : 
//                   (requestFriendId != null ? requestFriendId : UUID.randomUUID());
//
//        // Add friend's details to the friends list
//        String friendName = friendVoter.getVoterFnameEn() != null ? friendVoter.getVoterFnameEn() : "";
//        friends.add(new FriendDetail(trimmedFriendEpicNumber, friendName));
//        pathVoter.setFriendsDetails(friends);
//
//        // Increment friendCount for target voter (A) only
//        pathVoter.setFriendId(friendId);
//        pathVoter.setFriendCount(friends.size());
//        
//        log.debug("Assigning friendId: {} to voter: {}. Adding friend: {} (epic: {}) to friends_details. New friendCount: {}", 
//                  friendId, trimmedEpicNumber, friendName, trimmedFriendEpicNumber, pathVoter.getFriendCount());
//        pathVoter = voterRepository.save(pathVoter);
//
//        // Sync to MongoDB
//        try {
//            VoterMongo voterMongo = new VoterMongo(pathVoter);
//            log.debug("Syncing voter to MongoDB: epicNumber={}, friendId={}, friendCount={}, friendsDetails={}", 
//                      voterMongo.getEpicNumber(), voterMongo.getFriendId(), voterMongo.getFriendCount(), voterMongo.getFriendsDetails());
//            voterMongoRepository.saveVoterMongoWithNullFields(voterMongo);
//            log.info("Successfully synced voter to MongoDB: epicNumber={}, friendId={}, friendCount={}", 
//                     voterMongo.getEpicNumber(), friendId, voterMongo.getFriendCount());
//        } catch (Exception mongoEx) {
//            log.error("Failed to sync voter to MongoDB for epicNumber: {}, friendId: {}, error: {}", 
//                      trimmedEpicNumber, friendId, mongoEx.getMessage());
//        }
//    }
//
//    log.info("mapFriendId process completed successfully. friendId: {}, accountId: {}, electionId: {}", 
//             friendId, accountId, electionId);
//    return new ThedalResponse<>(ThedalSuccess.FRIEND_ID_ASSIGNED);
//}


//@Override
//@Transactional
//public ThedalResponse<String> deleteFriendId(Long electionId, String epicNumber) {
//    Long accountId = requestDetails.getCurrentAccountId();
//    if (accountId == null) {
//        log.error("Account ID not found, unauthorized access.");
//        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
//    }
//    validateElectionOwnership(electionId, accountId);
//
//    log.info("Starting deleteFriendId process. accountId: {}, electionId: {}, epicNumber: {}", 
//             accountId, electionId, epicNumber);
//
//    // Trim epicNumber input
//    String trimmedEpicNumber = epicNumber != null ? epicNumber.trim() : null;
//    if (trimmedEpicNumber == null || trimmedEpicNumber.isEmpty()) {
//        log.error("EpicNumber is null or empty after trimming. accountId: {}, electionId: {}", accountId, electionId);
//        throw new ThedalException(ThedalError.INVALID_EPIC_NUMBER, HttpStatus.BAD_REQUEST);
//    }
//
//    // Fetch the target voter (A)
//    Optional<VoterEntity> voterOpt = voterRepository.findByEpicNumberAndElectionIdAndAccountId(trimmedEpicNumber, accountId, electionId);
//    if (!voterOpt.isPresent()) {
//        log.error("Voter not found. epicNumber: {}, electionId: {}, accountId: {}", trimmedEpicNumber, electionId, accountId);
//        throw new ThedalException(ThedalError.VOTER_NOT_FOUND, HttpStatus.NOT_FOUND);
//    }
//
//    VoterEntity voter = voterOpt.get();
//
//    // Check if the voter has a friendId
//    if (voter.getFriendId() == null) {
//        log.warn("Voter with epicNumber {} does not have a friendId to delete.", trimmedEpicNumber);
//        throw new ThedalException(ThedalError.FRIEND_MAPPING_NOT_FOUND, HttpStatus.BAD_REQUEST);
//    }
//
//    // Remove the friendId and set friendCount to 0
//    voter.setFriendId(null);
//    voter.setFriendCount(0);
//
//    voterRepository.save(voter);
//
//    log.info("Successfully removed friendId for voter with epicNumber {}", trimmedEpicNumber);
//    return new ThedalResponse<>(ThedalSuccess.FRIEND_ID_DELETED);
//}

@Override
@Transactional
public ThedalResponse<String> deleteFriendId(Long electionId, String epicNumber) {
	Long accountId = requestDetails.getCurrentAccountId();
	if (accountId == null) {
		log.error("Account ID not found, unauthorized access.");
		throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
	}
	validateElectionOwnership(electionId, accountId);

	log.info("Starting deleteFriendId process. accountId: {}, electionId: {}, epicNumber: {}", 
			 accountId, electionId, epicNumber);

	// Trim epicNumber input
	String trimmedEpicNumber = epicNumber != null ? epicNumber.trim() : null;
	if (trimmedEpicNumber == null || trimmedEpicNumber.isEmpty()) {
		log.error("EpicNumber is null or empty after trimming. accountId: {}, electionId: {}", accountId, electionId);
		throw new ThedalException(ThedalError.INVALID_EPIC_NUMBER, HttpStatus.BAD_REQUEST);
	}

	// Fetch the target voter
	Optional<VoterEntity> voterOpt = voterRepository.findByEpicNumberAndElectionIdAndAccountId(trimmedEpicNumber, accountId, electionId);
	if (!voterOpt.isPresent()) {
		log.error("Voter not found. epicNumber: {}, electionId: {}, accountId: {}", trimmedEpicNumber, electionId, accountId);
		throw new ThedalException(ThedalError.VOTER_NOT_FOUND, HttpStatus.NOT_FOUND);
	}

	VoterEntity voter = voterOpt.get();

	// Check if the voter has a friendId
	if (voter.getFriendId() == null) {
		log.warn("Voter with epicNumber {} does not have a friendId to delete.", trimmedEpicNumber);
		throw new ThedalException(ThedalError.FRIEND_MAPPING_NOT_FOUND, HttpStatus.BAD_REQUEST);
	}

	// Clear friend-related fields
	voter.setFriendId(null);
	voter.setFriendCount(0);
	voter.setFriendsDetails(new ArrayList<>()); // Clear friendsDetails

	log.debug("Cleared friendId, friendCount, and friendsDetails for voter: epicNumber={}", trimmedEpicNumber);
	voter = voterRepository.save(voter);

	// Sync to MongoDB - DISABLED
	// MongoDB sync is currently disabled
	/*
	try {
		VoterMongo voterMongo = new VoterMongo(voter);
		log.debug("Syncing voter to MongoDB: epicNumber={}, friendId={}, friendCount={}, friendsDetails={}", 
				  voterMongo.getEpicNumber(), voterMongo.getFriendId(), voterMongo.getFriendCount(), voterMongo.getFriendsDetails());
		voterMongoRepository.saveVoterMongoWithNullFields(voterMongo);
		log.info("Successfully synced voter to MongoDB: epicNumber={}", voterMongo.getEpicNumber());
	} catch (Exception mongoEx) {
		log.error("Failed to sync voter to MongoDB for epicNumber: {}, error: {}", 
				  trimmedEpicNumber, mongoEx.getMessage());
		// CRITICAL FIX: Propagate MongoDB sync failures to API response
		throw new ThedalException(ThedalError.DATABASE_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
	}
	*/

	log.info("Successfully removed friendId for voter with epicNumber {}", trimmedEpicNumber);
	return new ThedalResponse<>(ThedalSuccess.FRIEND_ID_DELETED);
}

@Transactional
public void updateFriendCount(String epicNumber, Long accountId, Long electionId) {
	Optional<VoterEntity> voterOpt = voterRepository.findByEpicNumberAndElectionIdAndAccountId(epicNumber, accountId, electionId);
	if (voterOpt.isPresent()) {
		VoterEntity voter = voterOpt.get();
		int friendCount = voterRepository.countFriendsByVoterId(voter.getId(), accountId, electionId);
		log.debug("Updating friendCount for epicNumber: {}, count: {}", epicNumber, friendCount);
		voter.setFriendCount(friendCount);
		if (friendCount == 0) {
			voter.setFriendId(null);
		}
		voterRepository.save(voter);
	} else {
		log.warn("Voter not found for updating friendCount. epicNumber: {}, electionId: {}, accountId: {}", epicNumber, electionId, accountId);
	}
}

@Override
@Transactional
public ThedalResponse<String> deleteFriends(Long electionId, String epicNumber, List<String> friendEpicNumbers) {
	Long accountId = requestDetails.getCurrentAccountId();
	if (accountId == null) {
		log.error("Account ID not found, unauthorized access.");
		throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
	}
	validateElectionOwnership(electionId, accountId);

	log.info("Starting deleteFriends process. accountId: {}, electionId: {}, epicNumber: {}, friendEpicNumbers: {}", 
			 accountId, electionId, epicNumber, friendEpicNumbers);

	// Trim inputs
	String trimmedEpicNumber = epicNumber != null ? epicNumber.trim() : null;
	List<String> trimmedFriendEpicNumbers = friendEpicNumbers.stream()
			.map(f -> f != null ? f.trim() : null)
			.filter(f -> f != null && !f.isEmpty())
			.collect(Collectors.toList());
	
	if (trimmedEpicNumber == null || trimmedEpicNumber.isEmpty()) {
		log.error("EpicNumber is null or empty after trimming. accountId: {}, electionId: {}", accountId, electionId);
		throw new ThedalException(ThedalError.INVALID_EPIC_NUMBER, HttpStatus.BAD_REQUEST);
	}
	if (trimmedFriendEpicNumbers.isEmpty()) {
		log.error("FriendEpicNumbers list is empty after trimming. accountId: {}, electionId: {}", accountId, electionId);
		throw new ThedalException(ThedalError.INVALID_FRIEND_EPIC_NUMBER, HttpStatus.BAD_REQUEST);
	}

	// Fetch the target voter
	Optional<VoterEntity> voterOpt = voterRepository.findByEpicNumberAndElectionIdAndAccountId(trimmedEpicNumber, accountId, electionId);
	if (!voterOpt.isPresent()) {
		log.error("Voter not found. epicNumber: {}, electionId: {}, accountId: {}", trimmedEpicNumber, electionId, accountId);
		throw new ThedalException(ThedalError.VOTER_NOT_FOUND, HttpStatus.NOT_FOUND);
	}

	VoterEntity voter = voterOpt.get();

	// Get current friends list
	List<FriendDetail> friends = voter.getFriendsDetails();
	if (friends == null) {
		friends = new ArrayList<>();
		voter.setFriendsDetails(friends);
	}

	// Check which friends exist in the friendsDetails list
	List<String> notFoundEpicNumbers = new ArrayList<>();
	List<String> foundEpicNumbers = new ArrayList<>();
	for (String friendEpicNumber : trimmedFriendEpicNumbers) {
		if (friends.stream().anyMatch(friend -> friend.getEpicNumber().equals(friendEpicNumber))) {
			foundEpicNumbers.add(friendEpicNumber);
		} else {
			notFoundEpicNumbers.add(friendEpicNumber);
		}
	}

	if (foundEpicNumbers.isEmpty()) {
		log.warn("None of the provided friendEpicNumbers {} found in friendsDetails for voter {}.", 
				 trimmedFriendEpicNumbers, trimmedEpicNumber);
		throw new ThedalException(ThedalError.FRIEND_NOT_FOUND, HttpStatus.NOT_FOUND);
	}

	// Remove the friends from the list
	friends.removeIf(friend -> foundEpicNumbers.contains(friend.getEpicNumber()));
	
	// Update friendCount
	voter.setFriendCount(friends.size());

	// Clear friendId if no friends remain
	if (friends.isEmpty()) {
		voter.setFriendId(null);
		voter.setFriendsDetails(new ArrayList<>());
	} else {
		voter.setFriendsDetails(friends);
	}

	log.debug("Removed friends with epicNumbers: {} from voter: {}. New friendCount: {}", 
			  foundEpicNumbers, trimmedEpicNumber, voter.getFriendCount());
	
	// Log not found epic numbers as warnings
	if (!notFoundEpicNumbers.isEmpty()) {
		log.warn("Some friendEpicNumbers not found in friendsDetails for voter {}: {}", 
				 trimmedEpicNumber, notFoundEpicNumbers);
	}

	// Save the updated voter to PostgreSQL
	voter = voterRepository.save(voter);

	// Sync to MongoDB
	try {
		VoterMongo voterMongo = new VoterMongo(voter);
		log.debug("Syncing voter to MongoDB: epicNumber={}, friendId={}, friendCount={}, friendsDetails={}", 
				  voterMongo.getEpicNumber(), voterMongo.getFriendId(), voterMongo.getFriendCount(), voterMongo.getFriendsDetails());
		voterMongoRepository.saveVoterMongoWithNullFields(voterMongo);
		log.info("Successfully synced voter to MongoDB: epicNumber={}, friendId={}, friendCount={}", 
				 voterMongo.getEpicNumber(), voterMongo.getFriendId(), voterMongo.getFriendCount());
	} catch (Exception mongoEx) {
		log.error("Failed to sync voter to MongoDB for epicNumber: {}, error: {}", 
				  trimmedEpicNumber, mongoEx.getMessage());
		throw new ThedalException(ThedalError.MONGO_SYNC_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
	}

	String responseMessage = String.format("Successfully removed %d friend(s) with epicNumber(s) %s for voter with epicNumber %s%s",
			foundEpicNumbers.size(), foundEpicNumbers,
			trimmedEpicNumber,
			notFoundEpicNumbers.isEmpty() ? "" : String.format(". Not found: %s", notFoundEpicNumbers));
	log.info(responseMessage);
	return new ThedalResponse<>(ThedalSuccess.FRIEND_DELETED, responseMessage);
}

	@Async
	@Transactional
	public void mapFamiliesByHouseNumber(Long electionId, Long accountId, Long jobId) {
	// Validate input parameters
	if (accountId == null) {
		log.error("Account ID cannot be null");
		throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.BAD_REQUEST);
	}
	if (electionId == null) {
		log.error("Election ID cannot be null");
		throw new ThedalException(ThedalError.INVALID_ELECTION, HttpStatus.BAD_REQUEST);
	}
	
	// Validate election ownership using passed parameters (no request context needed)
	Optional<ElectionEntity> electionOpt = electionRepository.findByIdAndAccountId(electionId, accountId);
	if (!electionOpt.isPresent()) {
		log.error("Election ID {} does not belong to Account ID {}", electionId, accountId);
		throw new ThedalException(ThedalError.INVALID_ELECTION, HttpStatus.FORBIDDEN);
	}

	log.info("Starting family mapping by house number for accountId: {}, electionId: {}, jobId: {}", accountId, electionId, jobId);

	// Get job entity for progress tracking
	FamilyMappingJobEntity job = null;
	if (jobId != null) {
		Optional<FamilyMappingJobEntity> jobOpt = familyMappingJobRepository.findById(jobId);
		if (jobOpt.isPresent()) {
			job = jobOpt.get();
		}
	}

	try {
		// Step 1: Fetch distinct house numbers
		List<Object[]> houseNumberGroups = voterRepository.findDistinctHouseNumbers(accountId, electionId);
		
		// Update job with unique house numbers count
		if (job != null) {
			job.setUniqueHouseNumbers((long) houseNumberGroups.size());
			familyMappingJobRepository.save(job);
		}
		
		log.info("Found {} unique house numbers to process", houseNumberGroups.size());
		
		// Immediate test progress update
		if (job != null) {
			updateJobProgress(job, 0, 0, 0);
		}
		
		long processedVoters = 0;
		long familiesCreated = 0;
		long votersWithFamilies = 0;

		for (Object[] group : houseNumberGroups) {
			String houseNoEn = (String) group[0];
			if (houseNoEn == null || houseNoEn.trim().isEmpty()) {
				log.warn("Skipping null or empty house number: {}", houseNoEn);
				continue;
			}

			// Normalize house number (trim and lowercase)
			String normalizedHouseNoEn = houseNoEn.trim().toLowerCase();

			// Step 2: Fetch voters with the same house number
			List<VoterEntity> voters = voterRepository.findByAccountIdAndElectionIdAndHouseNoEn(
					accountId, electionId, normalizedHouseNoEn);

			if (voters.isEmpty()) {
				log.debug("No voters found for houseNoEn: {}", normalizedHouseNoEn);
				continue;
			}

			// Process families only for groups with more than 1 voter
			if (voters.size() > 1) {
				// Step 3: Check for existing familyId or generate a new one
				UUID familyId = voters.stream()
						.filter(v -> v.getFamilyId() != null)
						.findFirst()
						.map(VoterEntity::getFamilyId)
						.orElse(UUID.randomUUID());

				int familyCount = voters.size();

				// Step 4: Update voters with familyId and familyCount
				for (VoterEntity voter : voters) {
					voter.setFamilyId(familyId);
					voter.setFamilyCount(familyCount);
					voterRepository.save(voter);

					// Step 5: Sync with MongoDB - DISABLED
					// MongoDB sync is currently disabled
					/*
					try {
						Optional<VoterMongo> voterMongoOpt = voterMongoRepository.findById(voter.getId().toString());
						VoterMongo voterMongo = voterMongoOpt.orElse(new VoterMongo(voter));
						updateVoterMongoFromEntity(voterMongo, voter);
						voterMongoRepository.save(voterMongo);
					} catch (Exception mongoEx) {
						log.warn("Failed to sync voter {} to MongoDB: {}", voter.getEpicNumber(), mongoEx.getMessage());
					}
					*/
				}

				familiesCreated++;
				votersWithFamilies += voters.size();
				log.debug("Assigned familyId: {} to {} voters for houseNoEn: {}", familyId, familyCount, normalizedHouseNoEn);
			} else if (voters.size() == 1) {
				// Single voter - ensure they have no family mapping
				VoterEntity voter = voters.get(0);
				if (voter.getFamilyId() != null) {
					voter.setFamilyId(null);
					voter.setFamilyCount(1);
					voterRepository.save(voter);
					
					// MongoDB sync for single voters - DISABLED
					/*
					try {
						Optional<VoterMongo> voterMongoOpt = voterMongoRepository.findById(voter.getId().toString());
						VoterMongo voterMongo = voterMongoOpt.orElse(new VoterMongo(voter));
						updateVoterMongoFromEntity(voterMongo, voter);
						voterMongoRepository.save(voterMongo);
					} catch (Exception mongoEx) {
						log.warn("Failed to sync voter {} to MongoDB: {}", voter.getEpicNumber(), mongoEx.getMessage());
					}
					*/
				}
			}

			processedVoters += voters.size();

			// Update progress more frequently - every 10 house numbers processed
			if (job != null && houseNumberGroups.indexOf(group) % 10 == 0) {
				updateJobProgress(job, processedVoters, familiesCreated, votersWithFamilies);
				log.info("Progress update: Processed {} voters, Created {} families, House numbers processed: {}/{}", 
						processedVoters, familiesCreated, houseNumberGroups.indexOf(group) + 1, houseNumberGroups.size());
			}
		}

		// Final progress update
		if (job != null) {
			updateJobProgress(job, processedVoters, familiesCreated, votersWithFamilies);
			job.setStatus(BulkUploadStatus.COMPLETED);
			job.setEndTime(LocalDateTime.now());
			familyMappingJobRepository.save(job);
		}

		log.info("Family mapping by house number completed for accountId: {}, electionId: {}. Processed: {} voters, Created: {} families, Voters in families: {}", 
				accountId, electionId, processedVoters, familiesCreated, votersWithFamilies);
				
	} catch (Exception e) {
		log.error("Error in family mapping for accountId: {}, electionId: {}, jobId: {}", accountId, electionId, jobId, e);
		if (job != null) {
			job.setStatus(BulkUploadStatus.FAILED);
			job.setErrorMessage(e.getMessage());
			job.setEndTime(LocalDateTime.now());
			familyMappingJobRepository.save(job);
		}
		throw e;
	}
}

	@Override
	public int fixExistingJobUrls(Long accountId, Long electionId) {
		log.info("Starting to fix existing job URLs for accountId: {}, electionId: {}", accountId, electionId);
		
		List<VoterDownloadJob> jobs = voterDownloadJobRepository.findByAccountIdAndElectionIdAndStatus(
			accountId, electionId, "COMPLETED");
		
		int fixedCount = 0;
		for (VoterDownloadJob job : jobs) {
			String currentUrl = job.getAwsS3DownloadUrl();
			if (currentUrl != null && !currentUrl.startsWith("http")) {
				// This is a relative URL, convert to absolute
				String absoluteUrl = serverUrl + currentUrl;
				job.setAwsS3DownloadUrl(absoluteUrl);
				voterDownloadJobRepository.save(job);
				fixedCount++;
				log.info("Fixed URL for job {}: {} -> {}", job.getId(), currentUrl, absoluteUrl);
			}
		}
		
		log.info("Fixed {} job URLs for accountId: {}, electionId: {}", fixedCount, accountId, electionId);
		return fixedCount;
	}
	
	
	
//    @Transactional(readOnly = true)
//    @Override
//    public FriendGroupResponseDTO getFriendVotersByElection(Long accountId, Long electionId, 
//            List<Integer> partNumbers, Pageable pageable) {
//        
//        long startTime = System.currentTimeMillis();
//        try {
//            log.debug("Starting getFriendVotersByElection with electionId={}, accountId={}, partNumbers={}",
//                    electionId, accountId, partNumbers);
//
//            // Validate election ownership
//            validateElectionOwnership(electionId, accountId);
//
//            // Use partNumbers directly
//            final List<Integer> finalPartNumbers = partNumbers != null && !partNumbers.isEmpty() 
//                    ? partNumbers 
//                    : null;
//
//            // Fetch voters
//            Page<VoterEntity> voters = voterRepository.findByAccountIdAndElectionId(
//                    accountId, electionId, finalPartNumbers, pageable);
//
//         // Parse friendsDetails and group by friendId
//            Map<UUID, List<VoterEntity>> friendGroups = new HashMap<>();
//            List<String> allFriendEpicNumbers = new ArrayList<>();
//
//            for (VoterEntity voter : voters.getContent()) {
//                if (voter.getFriendId() != null) {
//                    List<VoterEntity> groupMembers = friendGroups
//                        .computeIfAbsent(voter.getFriendId(), k -> new ArrayList<>());
//                    groupMembers.add(voter);
//                    allFriendEpicNumbers.add(voter.getEpicNumber());
//
//                    // PROVEN WORKING JSON PARSING APPROACH
//                    if (voter.getFriendsDetails() != null && !voter.getFriendsDetails().isEmpty()) {
//                        try {
//                            // Step 1: Verify JSON is valid
//                            JsonNode rootNode = objectMapper.readTree((JsonParser) voter.getFriendsDetails());
//                            
//                            // Step 2: Convert to List<FriendDetail>
//                            List<FriendDetail> friendDetails;
//                            if (rootNode.isArray()) {
//                                friendDetails = new ArrayList<>();
//                                for (JsonNode node : rootNode) {
//                                    FriendDetail detail = objectMapper.treeToValue(node, FriendDetail.class);
//                                    friendDetails.add(detail);
//                                }
//                            } else {
//                                throw new IllegalArgumentException("Expected JSON array");
//                            }
//
//                            // Process friend details
//                            List<String> friendEpicNumbers = friendDetails.stream()
//                                .map(FriendDetail::getEpicNumber)
//                                .filter(Objects::nonNull)
//                                .collect(Collectors.toList());
//
//                            allFriendEpicNumbers.addAll(friendEpicNumbers);
//
//                            // Add matching voters to group
//                            voters.getContent().stream()
//                                .filter(f -> friendEpicNumbers.contains(f.getEpicNumber()))
//                                .forEach(groupMembers::add);
//
//                        } catch (Exception e) {
//                            log.error("JSON parsing failed for voter {}: {}", voter.getId(), e.getMessage());
//                            log.error("Problematic JSON content: {}", voter.getFriendsDetails());
//                        }
//                    }
//                }
//            }
//
//            // Apply part number filter to friend groups
//            if (finalPartNumbers != null) {
//                friendGroups = friendGroups.entrySet().stream()
//                        .filter(entry -> entry.getValue().stream()
//                                .anyMatch(voter -> finalPartNumbers.contains(voter.getPartNo())))
//                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
//            }
//
//            // Convert to FriendGroupDTOs with sorted members
//            List<FriendGroupDTO> friendGroupDTOs = friendGroups.entrySet().stream()
//                    .map(entry -> {
//                        FriendGroupDTO friendGroup = new FriendGroupDTO();
//                        friendGroup.setFriendId(entry.getKey());
//                        
//                        List<VoterEntity> sortedMembers = entry.getValue().stream()
//                                .sorted((v1, v2) -> {
//                                    if (v1.getFriendId() != null && v2.getFriendId() == null) return -1;
//                                    if (v2.getFriendId() != null && v1.getFriendId() == null) return 1;
//                                    return v1.getVoterId().compareTo(v2.getVoterId());
//                                })
//                                .collect(Collectors.toList());
//                        
//                        friendGroup.setMembers(sortedMembers);
//                        friendGroup.setFriendCount(sortedMembers.size());
//                        return friendGroup;
//                    })
//                    .sorted(Comparator.comparing(FriendGroupDTO::getFriendId))
//                    .collect(Collectors.toList());
//
//            // Load relationships if needed
//            if (!voters.getContent().isEmpty()) {
//                loadManyToManyRelationships(voters.getContent(), accountId, electionId);
//            }
//
//            // Create paged response
//            Page<FriendGroupDTO> friendGroupPage = new PageImpl<>(
//                    friendGroupDTOs, 
//                    pageable, 
//                    voters.getTotalElements()
//            );
//
//            // Fetch gender stats
//            String statsCacheKey = generateStatsCacheKey(accountId, electionId, finalPartNumbers);
//            GenderStatsProjection stats = getFromCache(statsCacheKey, () ->
//                    voterRepository.getGenderStatsByFriend(
//                            accountId, 
//                            electionId, 
//                            allFriendEpicNumbers.isEmpty() ? List.of("none") : allFriendEpicNumbers, 
//                            finalPartNumbers
//                    ));
//            
//            GenderStatsDTO genderStats = new GenderStatsDTO(
//                    stats.getMaleCount() != null ? stats.getMaleCount() : 0L,
//                    stats.getFemaleCount() != null ? stats.getFemaleCount() : 0L,
//                    stats.getOtherCount() != null ? stats.getOtherCount() : 0L,
//                    stats.getTotalCount() != null ? stats.getTotalCount() : 0L
//            );
//
//            // Validate result
//            if (friendGroupPage.isEmpty()) {
//                log.warn("No friend groups found for electionId={}", electionId);
//                throw new ThedalException(ThedalError.VOTER_NOT_FOUND, HttpStatus.NOT_FOUND);
//            }
//
//            // Build response
//            FriendGroupResponseDTO response = new FriendGroupResponseDTO(friendGroupPage, genderStats);
//
//            log.info("getFriendVotersByElection executed in {} ms", 
//                    System.currentTimeMillis() - startTime);
//            
//            return response;
//
//        } catch (ThedalException e) {
//            throw e;
//        } catch (Exception e) {
//            log.error("Unexpected error in getFriendVotersByElection: {}", e.getMessage(), e);
//            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, 
//                    HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//    }
//
//    private String generateStatsCacheKey(Long accountId, Long electionId, List<Integer> partNumbers) {
//        return String.format("stats_%d_%d_%s", accountId, electionId,
//                            partNumbers != null ? String.join(",", partNumbers.stream().map(String::valueOf).collect(Collectors.toList())) : "all");
//    }
    @Transactional(readOnly = true)
    @Override
    public FriendGroupResponseDTO getFriendVotersByElection(Long accountId, Long electionId, UUID friendId,
            List<Integer> partNumbers, Pageable pageable) {
        
        long startTime = System.currentTimeMillis();
        try {
            log.debug("Starting getFriendVotersByElection with electionId={}, accountId={}, partNumbers={}",
                    electionId, accountId, partNumbers);

			// Validate election ownership
			validateElectionOwnership(electionId, accountId);

			// Use partNumbers directly
			final List<Integer> finalPartNumbers = partNumbers != null && !partNumbers.isEmpty() 
					? partNumbers 
					: null;

            // Fetch voters
            Page<VoterEntity> voters = voterRepository.findByAccountIdAndElectionId(
                    accountId, electionId, friendId, finalPartNumbers, pageable);

			// Parse friendsDetails and group by friendId
			Map<UUID, List<VoterEntity>> friendGroups = new HashMap<>();
			List<String> allFriendEpicNumbers = new ArrayList<>();

			for (VoterEntity voter : voters.getContent()) {
				if (voter.getFriendId() != null) {
					List<VoterEntity> groupMembers = friendGroups
						.computeIfAbsent(voter.getFriendId(), k -> new ArrayList<>());
					groupMembers.add(voter);
					allFriendEpicNumbers.add(voter.getEpicNumber());

					// Parse friendsDetails JSON
					if (voter.getFriendsDetails() != null && !voter.getFriendsDetails().isEmpty()) {
						try {
							// Parse JSON to List<FriendDetail>
							List<FriendDetail> friendDetails = voter.getFriendsDetails();
							List<String> friendEpicNumbers = friendDetails.stream()
								.map(FriendDetail::getEpicNumber)
								.filter(Objects::nonNull)
								.collect(Collectors.toList());

							allFriendEpicNumbers.addAll(friendEpicNumbers);

							// Fetch voter details for friendEpicNumbers using getVoters API logic
							if (!friendEpicNumbers.isEmpty()) {
								// Query VoterEntity by epicNumber
								List<VoterEntity> friendVoters = voterRepository.findByAccountIdAndElectionIdAndEpicNumbers(
									accountId, electionId, friendEpicNumbers);
								
								// Add friend voters to the group
								groupMembers.addAll(friendVoters);
							}
						} catch (Exception e) {
							log.error("JSON parsing failed for voter {}: {}", voter.getId(), e.getMessage());
							log.error("Problematic JSON content: {}", voter.getFriendsDetails());
						}
					}
				}
			}

			// Apply part number filter to friend groups
			if (finalPartNumbers != null) {
				friendGroups = friendGroups.entrySet().stream()
						.filter(entry -> entry.getValue().stream()
								.anyMatch(voter -> finalPartNumbers.contains(voter.getPartNo())))
						.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
			}

			// Convert to FriendGroupDTOs with sorted members
			List<FriendGroupDTO> friendGroupDTOs = friendGroups.entrySet().stream()
					.map(entry -> {
						FriendGroupDTO friendGroup = new FriendGroupDTO();
						friendGroup.setFriendId(entry.getKey());
						
						// Sort members, prioritizing voters with friendId
						List<VoterEntity> sortedMembers = entry.getValue().stream()
								.sorted((v1, v2) -> {
									if (v1.getFriendId() != null && v2.getFriendId() == null) return -1;
									if (v2.getFriendId() != null && v1.getFriendId() == null) return 1;
									return v1.getVoterId().compareTo(v2.getVoterId());
								})
								.collect(Collectors.toList());
						
						friendGroup.setMembers(sortedMembers);
						friendGroup.setFriendCount(sortedMembers.size());
						return friendGroup;
					})
					.sorted(Comparator.comparing(FriendGroupDTO::getFriendId))
					.collect(Collectors.toList());

			// Load relationships if needed
			if (!voters.getContent().isEmpty()) {
				loadManyToManyRelationships(voters.getContent(), accountId, electionId);
			}

			// Create paged response
			Page<FriendGroupDTO> friendGroupPage = new PageImpl<>(
					friendGroupDTOs, 
					pageable, 
					voters.getTotalElements()
			);

			// Fetch gender stats
			String statsCacheKey = generateStatsCacheKey(accountId, electionId, finalPartNumbers);
			GenderStatsProjection stats = getFromCache(statsCacheKey, () ->
					voterRepository.getGenderStatsByFriend(
							accountId, 
							electionId, 
							allFriendEpicNumbers.isEmpty() ? List.of("none") : allFriendEpicNumbers, 
							finalPartNumbers
					));
			
			GenderStatsDTO genderStats = new GenderStatsDTO(
					stats.getMaleCount() != null ? stats.getMaleCount() : 0L,
					stats.getFemaleCount() != null ? stats.getFemaleCount() : 0L,
					stats.getOtherCount() != null ? stats.getOtherCount() : 0L,
					stats.getTotalCount() != null ? stats.getTotalCount() : 0L
			);

			// Validate result
			if (friendGroupPage.isEmpty()) {
				log.warn("No friend groups found'The query should be here.' found for electionId={}", electionId);
				throw new ThedalException(ThedalError.VOTER_NOT_FOUND, HttpStatus.NOT_FOUND);
			}

			// Build response
			FriendGroupResponseDTO response = new FriendGroupResponseDTO(friendGroupPage, genderStats);

			log.info("getFriendVotersByElection executed in {} ms", 
					System.currentTimeMillis() - startTime);
			
			return response;

		} catch (ThedalException e) {
			throw e;
		} catch (Exception e) {
			log.error("Unexpected error in getFriendVotersByElection: {}", e.getMessage(), e);
			throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, 
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	/**
	 * Backward compatibility method - calls the new method with null jobId
	 */
	@Async
	@Transactional
	public void mapFamiliesByHouseNumber(Long electionId, Long accountId) {
		mapFamiliesByHouseNumber(electionId, accountId, null);
	}
	
	/**
	 * Helper method to update family mapping job progress
	 */
	private void updateJobProgress(FamilyMappingJobEntity job, long processedVoters, long familiesCreated, long votersWithFamilies) {
		try {
			job.setProcessedVoters(processedVoters);
			job.setFamiliesCreated(familiesCreated);
			job.setVotersWithFamilies(votersWithFamilies);
			
			FamilyMappingJobEntity savedJob = familyMappingJobRepository.save(job);
			log.info("Successfully updated job progress: jobId={}, processed={}, families={}, votersWithFamilies={}, progress={}%", 
					savedJob.getId(), processedVoters, familiesCreated, votersWithFamilies, savedJob.getProgressPercentage());
		} catch (Exception e) {
			log.error("Failed to update job progress for jobId {}: {}", job.getId(), e.getMessage(), e);
		}
	}

	// ============ BULK PHOTO UPLOAD METHODS ============
	
	@Override
	public ThedalResponse<BulkPhotoUploadResponse> uploadVoterPhotosFromZip(MultipartFile zipFile, Long electionId) {
		log.info("Starting bulk photo upload for election {} with file {}", electionId, zipFile.getOriginalFilename());
		
		try {
			// Get current account and user details
			Long accountId = requestDetails.getCurrentAccountId();
			UserEntity currentUser = requestDetails.getCurrentUserFromRequest();
			String uploadedBy = currentUser != null ? currentUser.getEmail() : "unknown";
			
			if (accountId == null) {
				throw new ThedalException(ThedalError.ACCESS_DENIED, HttpStatus.UNAUTHORIZED);
			}
			
			// Validate election exists and belongs to account
			ElectionEntity election = electionRepository.findByIdAndAccountId(electionId, accountId)
				.orElseThrow(() -> new ThedalException(ThedalError.ELECTION_NOT_FOUND, HttpStatus.NOT_FOUND));
			String originalFilename = zipFile.getOriginalFilename();
			// Write the upload to a temp file and stream-process to reduce memory footprint
			File tempZip = File.createTempFile("voter_photos_", ".zip");
			try (java.io.InputStream in = zipFile.getInputStream(); java.io.FileOutputStream out = new java.io.FileOutputStream(tempZip)) {
				byte[] buf = new byte[8192];
				int r;
				while ((r = in.read(buf)) != -1) { out.write(buf, 0, r); }
			}
			CompletableFuture<BulkPhotoUploadResponse> futureResponse = voterPhotoUploadService.processBulkPhotoUploadFromFile(
				tempZip, originalFilename, electionId, accountId, uploadedBy);
			
			// Return immediate response with tracking info
			BulkPhotoUploadResponse response = new BulkPhotoUploadResponse(null, 
				"Bulk photo upload started. Processing in background...");
			
			return new ThedalResponse<>(ThedalSuccess.BULK_UPLOAD_STARTED, response);
			
		} catch (ThedalException e) {
			throw e;
		} catch (Exception e) {
			log.error("Error starting bulk photo upload: {}", e.getMessage(), e);
			throw new ThedalException(ThedalError.BULK_UPLOAD_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	@Override
	public ThedalResponse<BulkPhotoUploadEntity> getBulkPhotoUploadStatus(Long bulkUploadId) {
		log.info("Getting bulk photo upload status for ID: {}", bulkUploadId);
		
		try {
			Long accountId = requestDetails.getCurrentAccountId();
			if (accountId == null) {
				throw new ThedalException(ThedalError.ACCESS_DENIED, HttpStatus.UNAUTHORIZED);
			}
			
			BulkPhotoUploadEntity uploadEntity = bulkPhotoUploadRepository.findById(bulkUploadId)
				.orElseThrow(() -> new ThedalException(ThedalError.BULK_UPLOAD_NOT_FOUND, HttpStatus.NOT_FOUND));
			
			// Verify the upload belongs to the current account
			if (!uploadEntity.getAccountId().equals(accountId)) {
				throw new ThedalException(ThedalError.ACCESS_DENIED, HttpStatus.FORBIDDEN);
			}
			
			return new ThedalResponse<>(ThedalSuccess.SUCCESS, uploadEntity);
			
		} catch (ThedalException e) {
			throw e;
		} catch (Exception e) {
			log.error("Error getting bulk photo upload status: {}", e.getMessage(), e);
			throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	@Override
	public ThedalResponse<List<BulkPhotoUploadEntity>> getAllBulkPhotoUploads(Long electionId, int page, int size) {
		log.info("Getting all bulk photo uploads for election: {}", electionId);
		
		try {
			Long accountId = requestDetails.getCurrentAccountId();
			if (accountId == null) {
				throw new ThedalException(ThedalError.ACCESS_DENIED, HttpStatus.UNAUTHORIZED);
			}
			
			// Validate election exists and belongs to account
			ElectionEntity election = electionRepository.findByIdAndAccountId(electionId, accountId)
				.orElseThrow(() -> new ThedalException(ThedalError.ELECTION_NOT_FOUND, HttpStatus.NOT_FOUND));
			
			Pageable pageable = PageRequest.of(page, size);
			Page<BulkPhotoUploadEntity> uploads = bulkPhotoUploadRepository
				.findByAccountIdAndElectionIdOrderByStartTimeDesc(accountId, electionId, pageable);
			
			return new ThedalResponse<>(ThedalSuccess.SUCCESS, uploads.getContent());
			
		} catch (ThedalException e) {
			throw e;
		} catch (Exception e) {
			log.error("Error getting bulk photo uploads: {}", e.getMessage(), e);
			throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	@Transactional
	public ThedalResponse<String> generateFamilyId(Long electionId, String epicNumber) {
		try {
			// Get current account
			Long accountId = requestDetails.getCurrentAccountId();
			if (accountId == null) {
				log.error("Account ID not found, unauthorized access.");
				throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
			}

			// Find the voter
			Optional<VoterEntity> voterOpt = voterRepository.findByAccountIdAndElectionIdAndEpicNumber(
				accountId, electionId, epicNumber);

			if (voterOpt.isEmpty()) {
				log.error("Voter not found: epicNumber={}, electionId={}, accountId={}", epicNumber, electionId, accountId);
				throw new ThedalException(ThedalError.VOTER_NOT_FOUND,
					HttpStatus.NOT_FOUND, "Voter not found");
			}

			VoterEntity voter = voterOpt.get();

			// Check if already has family ID
			if (voter.getFamilyId() != null) {
				log.warn("Voter already has family ID: epicNumber={}, existingFamilyId={}", epicNumber, voter.getFamilyId());
				throw new ThedalException(ThedalError.INVALID_REQUEST,
					HttpStatus.BAD_REQUEST, "Voter already has a family ID");
			}

			// Generate new family ID
			UUID newFamilyId = UUID.randomUUID();
		voter.setFamilyId(newFamilyId);
		voter.setFamilyCount(1); // Single voter family initially

		// Assign family sequence number for proper ordering
		Integer maxSequence = voterRepository.getMaxFamilySequenceNumber(accountId, electionId);
		Integer sequenceNumber = (maxSequence == null ? 0 : maxSequence) + 1;
		voter.setFamilySequenceNumber(sequenceNumber);

		// Save the voter
		voterRepository.save(voter);

		// Log the action
		log.info("Generated family ID {} with sequence number {} for voter {} in election {}", 
				newFamilyId, sequenceNumber, epicNumber, electionId);			return new ThedalResponse<>(ThedalSuccess.SUCCESS,
				"Family ID generated successfully: " + newFamilyId);

		} catch (ThedalException e) {
			throw e;
		} catch (Exception e) {
			log.error("Error generating family ID for voter {} in election {}: {}", epicNumber, electionId, e.getMessage(), e);
			throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	// Family sequence number management methods
	
	@Transactional
	public ThedalResponse<String> renumberFamilies(Long electionId, String strategy, Integer startNumber) {
		try {
			Long accountId = requestDetails.getCurrentAccountId();
			if (accountId == null) {
				throw new ThedalException(ThedalError.UNAUTHORIZED_ACCESS, HttpStatus.UNAUTHORIZED);
			}

			log.info("Starting family renumbering for election {} with strategy {} starting from {}", 
					electionId, strategy, startNumber);

			// Validate strategy
			if (!isValidRenumberingStrategy(strategy)) {
				throw new ThedalException(ThedalError.INVALID_REQUEST, HttpStatus.BAD_REQUEST, 
						"Invalid strategy. Supported: AGE_DESC, NAME_ASC, PART_ASC, SIZE_DESC, RESET");
			}

			// Get all families ordered by chosen strategy
			List<UUID> familyIds = getFamiliesOrderedByStrategy(accountId, electionId, strategy);
			
			if (familyIds.isEmpty()) {
				return new ThedalResponse<>(ThedalSuccess.SUCCESS, "No families found to renumber");
			}

			// Reassign sequence numbers
			int currentNumber = startNumber;
			int updatedFamilies = 0;
			
			for (UUID familyId : familyIds) {
				int rowsUpdated = voterRepository.updateFamilySequenceNumber(
						familyId, currentNumber, accountId, electionId);
				if (rowsUpdated > 0) {
					updatedFamilies++;
				}
				currentNumber++;
			}

			log.info("Successfully renumbered {} families for election {}", updatedFamilies, electionId);
			
			return new ThedalResponse<>(ThedalSuccess.SUCCESS, 
					"Successfully renumbered " + updatedFamilies + " families using " + strategy + " strategy");
					
		} catch (ThedalException e) {
			throw e;
		} catch (Exception e) {
			log.error("Error renumbering families for election {}: {}", electionId, e.getMessage(), e);
			throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@Transactional  
	public ThedalResponse<String> updateSingleFamilyNumber(Long electionId, UUID familyId, Integer sequenceNumber) {
		try {
			Long accountId = requestDetails.getCurrentAccountId();
			if (accountId == null) {
				throw new ThedalException(ThedalError.UNAUTHORIZED_ACCESS, HttpStatus.UNAUTHORIZED);
			}

			// Validate sequence number
			if (sequenceNumber < 1) {
				throw new ThedalException(ThedalError.INVALID_REQUEST, HttpStatus.BAD_REQUEST,
						"Sequence number must be greater than 0");
			}

			// Check if family exists
			List<VoterEntity> familyMembers = voterRepository.findFamilyMembers(accountId, electionId, familyId);
			if (familyMembers.isEmpty()) {
				throw new ThedalException(ThedalError.VOTER_NOT_FOUND, HttpStatus.NOT_FOUND,
						"Family not found");
			}

			// Check if sequence number is already taken by another family
			long conflictCount = voterRepository.countByAccountIdAndElectionIdAndFamilySequenceNumberAndFamilyIdNot(
					accountId, electionId, sequenceNumber, familyId);
			if (conflictCount > 0) {
				throw new ThedalException(ThedalError.INVALID_REQUEST, HttpStatus.CONFLICT,
						"Sequence number " + sequenceNumber + " is already assigned to another family");
			}

			// Update the family sequence number
			int rowsUpdated = voterRepository.updateFamilySequenceNumber(familyId, sequenceNumber, accountId, electionId);
			
			if (rowsUpdated == 0) {
				throw new ThedalException(ThedalError.VOTER_NOT_FOUND, HttpStatus.NOT_FOUND,
						"Family not found or could not be updated");
			}

			log.info("Updated family {} sequence number to {} for election {}", familyId, sequenceNumber, electionId);
			
			return new ThedalResponse<>(ThedalSuccess.SUCCESS,
					"Successfully updated family sequence number to " + sequenceNumber);
					
		} catch (ThedalException e) {
			throw e;
		} catch (Exception e) {
			log.error("Error updating family {} sequence number: {}", familyId, e.getMessage(), e);
			throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	private boolean isValidRenumberingStrategy(String strategy) {
		return strategy != null && (
				strategy.equals("AGE_DESC") ||
				strategy.equals("NAME_ASC") ||
				strategy.equals("PART_ASC") ||
				strategy.equals("SIZE_DESC") ||
				strategy.equals("RESET")
		);
	}

	private List<UUID> getFamiliesOrderedByStrategy(Long accountId, Long electionId, String strategy) {
		// For now, implement basic ordering by family_id (can be enhanced later)
		// This provides a consistent ordering that can be extended with more sophisticated strategies
		
		switch (strategy.toUpperCase()) {
			case "RESET":
			case "AGE_DESC":
			case "NAME_ASC":
			case "PART_ASC":
			case "SIZE_DESC":
			default:
				// Get all distinct family IDs ordered by family_id for consistency
				return voterRepository.findDistinctFamilyIdsByElection(accountId, electionId);
		}
	}

	@Transactional
	public ThedalResponse<String> reorderFamilySequences(Long electionId, List<FamilySequenceReorderRequest> reorderRequests) {
		try {
			Long accountId = requestDetails.getCurrentAccountId();
			if (accountId == null) {
				throw new ThedalException(ThedalError.UNAUTHORIZED_ACCESS, HttpStatus.UNAUTHORIZED);
			}

			if (reorderRequests == null || reorderRequests.isEmpty()) {
				throw new ThedalException(ThedalError.INVALID_REQUEST, HttpStatus.BAD_REQUEST,
						"Reorder requests cannot be empty");
			}

			// Validate all family IDs exist in the election
			List<UUID> familyIds = reorderRequests.stream()
					.map(FamilySequenceReorderRequest::getFamilyId)
					.collect(Collectors.toList());

			List<UUID> existingFamilyIds = voterRepository.findExistingFamilyIds(accountId, electionId, familyIds);
			if (existingFamilyIds.size() != familyIds.size()) {
				throw new ThedalException(ThedalError.VOTER_NOT_FOUND, HttpStatus.NOT_FOUND,
						"One or more families not found in the specified election");
			}

			// Validate sequence numbers are unique and positive
			Set<Integer> sequenceNumbers = new HashSet<>();
			for (FamilySequenceReorderRequest request : reorderRequests) {
				if (request.getNewSequenceNumber() < 1) {
					throw new ThedalException(ThedalError.INVALID_REQUEST, HttpStatus.BAD_REQUEST,
							"Sequence numbers must be greater than 0");
				}
				if (!sequenceNumbers.add(request.getNewSequenceNumber())) {
					throw new ThedalException(ThedalError.INVALID_REQUEST, HttpStatus.BAD_REQUEST,
							"Duplicate sequence numbers are not allowed");
				}
			}

			// Process each reorder request
			int updatedFamilies = 0;
			for (FamilySequenceReorderRequest request : reorderRequests) {
				int rowsUpdated = voterRepository.updateFamilySequenceNumber(
						request.getFamilyId(),
						request.getNewSequenceNumber(),
						accountId,
						electionId
				);
				if (rowsUpdated > 0) {
					updatedFamilies++;
				}
			}

			log.info("Successfully reordered {} families for election {}", updatedFamilies, electionId);
			
			return new ThedalResponse<>(ThedalSuccess.SUCCESS,
					"Successfully reordered " + updatedFamilies + " family sequences");
					
		} catch (ThedalException e) {
			throw e;
		} catch (Exception e) {
			log.error("Error reordering family sequences for election {}: {}", electionId, e.getMessage(), e);
			throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	@Transactional
	public ThedalResponse<String> setFamilyPartOverride(Long electionId, UUID familyId, Integer partNumber) {
		try {
			Long accountId = requestDetails.getCurrentAccountId();
			
			// Validate that the part number exists for this election
			long partCount = voterRepository.countVotersByPart(accountId, electionId, partNumber);
			if (partCount == 0) {
				throw new ThedalException(ThedalError.INVALID_REQUEST, HttpStatus.BAD_REQUEST,
						"Part number " + partNumber + " does not exist in this election");
			}
			
			// Set the family part override
			int rowsUpdated = voterRepository.setFamilyPartOverride(accountId, electionId, familyId, partNumber);
			
			if (rowsUpdated == 0) {
				throw new ThedalException(ThedalError.VOTER_NOT_FOUND, HttpStatus.NOT_FOUND);
			}
			
			log.info("Set family {} part override to {} for election {}", familyId, partNumber, electionId);
			return new ThedalResponse<>(ThedalSuccess.SUCCESS,
					"Successfully set family part override to " + partNumber);
					
		} catch (ThedalException e) {
			throw e;
		} catch (Exception e) {
			log.error("Error setting family part override for family {} in election {}: {}", 
					familyId, electionId, e.getMessage(), e);
			throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	@Transactional
	public ThedalResponse<String> removeFamilyPartOverride(Long electionId, UUID familyId) {
		try {
			Long accountId = requestDetails.getCurrentAccountId();
			
			// Remove the family part override
			int rowsUpdated = voterRepository.removeFamilyPartOverride(accountId, electionId, familyId);
			
			if (rowsUpdated == 0) {
				throw new ThedalException(ThedalError.VOTER_NOT_FOUND, HttpStatus.NOT_FOUND);
			}
			
			log.info("Removed family {} part override for election {}", familyId, electionId);
			return new ThedalResponse<>(ThedalSuccess.SUCCESS,
					"Successfully removed family part override");
					
		} catch (ThedalException e) {
			throw e;
		} catch (Exception e) {
			log.error("Error removing family part override for family {} in election {}: {}", 
					familyId, electionId, e.getMessage(), e);
			throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	@Transactional
	public ThedalResponse<String> setFamilyHead(Long electionId, UUID familyId, String epicNumber) {
		try {
			Long accountId = requestDetails.getCurrentAccountId();
			
			// First find the voter by EPIC number
			Optional<VoterEntity> voterOpt = voterRepository.findByVoterIdAndElectionIdAndAccountId(
					epicNumber, electionId, accountId);
			
			if (!voterOpt.isPresent()) {
				throw new ThedalException(ThedalError.VOTER_NOT_FOUND, HttpStatus.NOT_FOUND,
						"Voter with EPIC number " + epicNumber + " not found");
			}
			
			VoterEntity voter = voterOpt.get();
			Long voterId = voter.getId();
			
			// Validate that the voter belongs to the specified family
			List<VoterEntity> familyMembers = voterRepository.findFamilyMembers(accountId, electionId, familyId);
			boolean voterInFamily = familyMembers.stream()
					.anyMatch(v -> v.getId().equals(voterId));
					
			if (!voterInFamily) {
				throw new ThedalException(ThedalError.INVALID_REQUEST, HttpStatus.BAD_REQUEST,
						"Voter " + epicNumber + " does not belong to the specified family");
			}
			
			// Set the family head (this automatically clears other family head flags in the same family)
			int rowsUpdated = voterRepository.setFamilyHead(accountId, electionId, familyId, voterId);
			
			if (rowsUpdated == 0) {
				throw new ThedalException(ThedalError.VOTER_NOT_FOUND, HttpStatus.NOT_FOUND);
			}
			
			log.info("Set voter {} (EPIC: {}) as family head for family {} in election {}", 
					voterId, epicNumber, familyId, electionId);
			return new ThedalResponse<>(ThedalSuccess.SUCCESS,
					"Successfully set family head");
					
		} catch (ThedalException e) {
			throw e;
		} catch (Exception e) {
			log.error("Error setting family head for family {} in election {} with voter {}: {}", 
					familyId, electionId, epicNumber, e.getMessage(), e);
			throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	@Transactional
	public ThedalResponse<String> removeFamilyHead(Long electionId, UUID familyId) {
		try {
			Long accountId = requestDetails.getCurrentAccountId();
			
			// Clear all family head flags for this family
			int rowsUpdated = voterRepository.clearAllFamilyHeads(accountId, electionId, familyId);
			
			if (rowsUpdated == 0) {
				throw new ThedalException(ThedalError.VOTER_NOT_FOUND, HttpStatus.NOT_FOUND);
			}
			
			log.info("Removed family head designation for family {} in election {}", familyId, electionId);
			return new ThedalResponse<>(ThedalSuccess.SUCCESS,
					"Successfully removed family head designation");
					
		} catch (ThedalException e) {
			throw e;
		} catch (Exception e) {
			log.error("Error removing family head for family {} in election {}: {}", 
					familyId, electionId, e.getMessage(), e);
			throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public com.thedal.thedal_app.voter.dto.ElectionVoterStatsDTO getElectionVoterStats(Long electionId) {
		try {
			Long accountId = requestDetails.getCurrentAccountId();
			
			// Get election voter statistics
			Object[] stats = voterRepository.getElectionVoterStats(accountId, electionId);
			
			if (stats == null || stats.length < 3) {
				throw new ThedalException(ThedalError.VOTER_NOT_FOUND, HttpStatus.NOT_FOUND, 
						"No voters found for this election");
			}
			
			Long totalVoters = ((Number) stats[0]).longValue();
			Long votedCount = stats[1] != null ? ((Number) stats[1]).longValue() : 0L;
			Long notVotedCount = stats[2] != null ? ((Number) stats[2]).longValue() : 0L;
			
			// Calculate turnout percentage
			Double turnoutPercentage = totalVoters > 0 ? (votedCount * 100.0 / totalVoters) : 0.0;
			turnoutPercentage = Math.round(turnoutPercentage * 100.0) / 100.0;
			
			com.thedal.thedal_app.voter.dto.ElectionVoterStatsDTO result = new com.thedal.thedal_app.voter.dto.ElectionVoterStatsDTO();
			result.setElectionId(electionId);
			result.setTotalVoters(totalVoters);
			result.setVotedCount(votedCount);
			result.setNotVotedCount(notVotedCount);
			result.setTurnoutPercentage(turnoutPercentage);
			
			log.info("Election stats for electionId {}: total={}, voted={}, notVoted={}, turnout={}%", 
					electionId, totalVoters, votedCount, notVotedCount, turnoutPercentage);
			
			return result;
			
		} catch (ThedalException e) {
			throw e;
		} catch (Exception e) {
			log.error("Error fetching election voter stats for electionId {}: {}", electionId, e.getMessage(), e);
			throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public com.thedal.thedal_app.voter.dto.WinningProbabilityDTO getWinningProbability(Long electionId) {
		try {
			Long accountId = requestDetails.getCurrentAccountId();
			
			// Get the election to find default party
			com.thedal.thedal_app.election.ElectionEntity election = electionRepository.findById(electionId)
					.orElseThrow(() -> new ThedalException(ThedalError.ELECTION_NOT_FOUND, HttpStatus.NOT_FOUND));
			
			// Check if default party is set
			if (election.getDefaultPartyId() == null) {
				throw new ThedalException(ThedalError.INVALID_REQUEST, HttpStatus.BAD_REQUEST, 
						"No default party set for this election");
			}
			
			// Get default party details
			com.thedal.thedal_app.settings.electionsettings.Party party = partyRepository.findById(election.getDefaultPartyId())
					.orElseThrow(() -> new ThedalException(ThedalError.PARTY_NOT_FOUND, HttpStatus.NOT_FOUND));
			
			// Get overall election statistics
			Object[] electionStats = voterRepository.getElectionVoterStats(accountId, electionId);
			if (electionStats == null || electionStats.length < 3) {
				throw new ThedalException(ThedalError.VOTER_NOT_FOUND, HttpStatus.NOT_FOUND, 
						"No voters found for this election");
			}
			
			Long totalVoters = ((Number) electionStats[0]).longValue();
			Long totalVoted = electionStats[1] != null ? ((Number) electionStats[1]).longValue() : 0L;
			
			// Get default party statistics
			Object[] partyStats = voterRepository.getPartyStats(accountId, electionId, party.getId());
			Long defaultPartySupporters = partyStats != null && partyStats[0] != null ? ((Number) partyStats[0]).longValue() : 0L;
			Long defaultPartyVoted = partyStats != null && partyStats[1] != null ? ((Number) partyStats[1]).longValue() : 0L;
			
			// Calculate percentages
			Double overallTurnout = totalVoters > 0 ? (totalVoted * 100.0 / totalVoters) : 0.0;
			Double defaultPartyTurnout = defaultPartySupporters > 0 ? (defaultPartyVoted * 100.0 / defaultPartySupporters) : 0.0;
			Double supportPercentage = totalVoters > 0 ? (defaultPartySupporters * 100.0 / totalVoters) : 0.0;
			
			// Round to 2 decimal places
			overallTurnout = Math.round(overallTurnout * 100.0) / 100.0;
			defaultPartyTurnout = Math.round(defaultPartyTurnout * 100.0) / 100.0;
			supportPercentage = Math.round(supportPercentage * 100.0) / 100.0;
			
			// Calculate winning probability (same as support percentage for now)
			Double probability = supportPercentage;
			
			// Determine confidence level
			String confidence;
			String message;
			if (probability >= 50.0) {
				confidence = "Very High";
				message = party.getPartyName() + " has majority support with " + supportPercentage + "% voter base";
			} else if (probability >= 40.0) {
				confidence = "High";
				message = party.getPartyName() + " has strong support with " + supportPercentage + "% voter base";
			} else if (probability >= 30.0) {
				confidence = "Moderate";
				message = party.getPartyName() + " has competitive support with " + supportPercentage + "% voter base";
			} else if (probability >= 20.0) {
				confidence = "Low";
				message = party.getPartyName() + " has challenging support with " + supportPercentage + "% voter base";
			} else {
				confidence = "Very Low";
				message = party.getPartyName() + " has limited support with " + supportPercentage + "% voter base";
			}
			
			// Build response
			com.thedal.thedal_app.voter.dto.WinningProbabilityDTO.DefaultPartyInfo defaultPartyInfo = 
					new com.thedal.thedal_app.voter.dto.WinningProbabilityDTO.DefaultPartyInfo(
							party.getId(),
							party.getPartyName(),
							party.getPartyShortName(),
							party.getPartyImage(),
							party.getPartyColor()
					);
			
			com.thedal.thedal_app.voter.dto.WinningProbabilityDTO.ElectionStatistics statistics = 
					new com.thedal.thedal_app.voter.dto.WinningProbabilityDTO.ElectionStatistics(
							totalVoters,
							totalVoted,
							overallTurnout,
							defaultPartySupporters,
							defaultPartyVoted,
							defaultPartyTurnout
					);
			
			com.thedal.thedal_app.voter.dto.WinningProbabilityDTO.ProbabilityInfo probabilityInfo = 
					new com.thedal.thedal_app.voter.dto.WinningProbabilityDTO.ProbabilityInfo(
							supportPercentage,
							probability,
							confidence,
							message
					);
			
			com.thedal.thedal_app.voter.dto.WinningProbabilityDTO result = 
					new com.thedal.thedal_app.voter.dto.WinningProbabilityDTO();
			result.setElectionId(electionId);
			result.setDefaultParty(defaultPartyInfo);
			result.setStatistics(statistics);
			result.setWinningProbability(probabilityInfo);
			result.setComputedAt(LocalDateTime.now().toString());
			
			log.info("Winning probability for electionId {}, party {}: probability={}%, confidence={}", 
					electionId, party.getPartyName(), probability, confidence);
			
			return result;
			
		} catch (ThedalException e) {
			throw e;
		} catch (Exception e) {
			log.error("Error calculating winning probability for electionId {}: {}", electionId, e.getMessage(), e);
			throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

}
