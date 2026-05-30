package com.thedal.thedal_app.volunteer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.thedal.thedal_app.account.AccountEntity;
import com.thedal.thedal_app.account.AccountRepository;
import com.thedal.thedal_app.account.AccountService;
import com.thedal.thedal_app.awsfilestore.AwsFileUpload;
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
import com.thedal.thedal_app.report.cadre.VolunteerVsVoterReportEntity;
import com.thedal.thedal_app.report.cadre.VolunteerVsVoterReportRepository;
import com.thedal.thedal_app.response.ThedalResponse;
import com.thedal.thedal_app.response.ThedalSuccess;
import com.thedal.thedal_app.role.Role;
import com.thedal.thedal_app.role.RolePermission;
import com.thedal.thedal_app.role.RoleRepo;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;
import com.thedal.thedal_app.user.MongoUser;
import com.thedal.thedal_app.user.MongoUserRepository;
import com.thedal.thedal_app.user.UserEntity;
import com.thedal.thedal_app.user.UserRepo;
import com.thedal.thedal_app.volunteer.dto.AddressDTO;
import com.thedal.thedal_app.volunteer.dto.BoothUpdateRequest;
import com.thedal.thedal_app.volunteer.dto.LocationDto;
import com.thedal.thedal_app.volunteer.dto.SaveVolunteerDetailsDTO;
import com.thedal.thedal_app.volunteer.dto.VolunteerActivityResponseDTO;
import com.thedal.thedal_app.volunteer.dto.VolunteerActivityTrackingDTO;
import com.thedal.thedal_app.volunteer.dto.VolunteerDetailsDTO;
import com.thedal.thedal_app.volunteer.dto.VolunteerDetailsUpdate;
import com.thedal.thedal_app.volunteer.dto.VolunteerExportResponse;
import com.thedal.thedal_app.volunteer.dto.VolunteerJobStatusResponse;
import com.thedal.thedal_app.volunteer.dto.VolunteerLocationDto;
import com.thedal.thedal_app.volunteer.dto.VolunteerUploadSummary;
import com.thedal.thedal_app.voter.Address;
import com.thedal.thedal_app.voter.BulkUploadStatus;

import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Service
public class VolunteerServiceImpl implements VolunteerService{
	
	@Autowired
    private VolunteerRepository volunteerRepo;
	
	@Autowired
    private ActivityRepository activityRepository;
	
	@Autowired
    private RequestDetailsService requestDetails;
	
	@Autowired
	private AccountService accountService;
	
	@Autowired
	private VolunteerDailyActivityRepository volunteerDailyActivityRepository;
	
	@Autowired
	private VolunteerActivityLogsRepository volunteerActivityLogsRepository;
	
    @Autowired
    private AwsFileUpload awsFileUpload;
    
    @Autowired
    private VolunteerBulkUploadRepository volunteerBulkUploadRepository;
    
    @Autowired
    private VounteerFileUploadService volunteerFileUploadService;
    
    @Autowired
    private FilesRepository filesRepository;

	@Autowired
	private RoleRepo roleRepo;
	
    @Autowired
    private UserRepo userRepo;
    @Autowired
    private MongoUserRepository mongoUserRepository;
    
    @Autowired
    private AccountRepository accountRepository;
    
    @Autowired
    private ElectionRepository electionRepository;
    @Autowired
    private ElectionBoothRepository electionBoothRepository;
    @Autowired
    private PartManagerRepository partManagerRepository;
    @Autowired
    private VolunteerElectionBoothRepo volunteerElectionBoothRepo;
    @Autowired
	private VolunteerVsVoterReportRepository volunteerVsVoterReportRepository;
    
    @Value("${aws.s3.files.bucket}")
	private String s3Filesbucket;
    @Value("${thedal.server.url}")
	private String serverUrl;
    
    @Value("${aws.s3.banner.bucket}")
   	private String s3Bannerbucket;
    
    private static final Long VOLUNTEER=4L;
	
	@Autowired
    private MongoVolunteerService mongoVolunteerService;
    @Autowired
    private MongoVolunteerRepository mongoVolunteerRepository;
    @Autowired
    private VolunteerDownloadJobRepository volunteerDownloadJobRepository;
    
    private static final int BATCH_SIZE = 1000;
    
    // Method to map VolunteerEntity to MongoVolunteer (for new records)
    private MongoVolunteer mapToMongoVolunteer(VolunteerEntity volunteerEntity) {
        MongoVolunteer mongoVolunteer = new MongoVolunteer();
        return mapToMongoVolunteer(volunteerEntity, mongoVolunteer);
    }

    // Method to map VolunteerEntity to existing MongoVolunteer (for updates)
    private MongoVolunteer mapToMongoVolunteer(VolunteerEntity volunteerEntity, MongoVolunteer mongoVolunteer) {
        mongoVolunteer.setId(volunteerEntity.getId().toString());
        mongoVolunteer.setAccountId(volunteerEntity.getAccountId());
        mongoVolunteer.setStatus(volunteerEntity.getStatus());
        mongoVolunteer.setPhotoUrl(volunteerEntity.getPhotoUrl());
        mongoVolunteer.setRemarks(volunteerEntity.getRemarks());
        mongoVolunteer.setGender(volunteerEntity.getGender());
        mongoVolunteer.setWhatsAppNumber(volunteerEntity.getWhatsAppNumber());
        mongoVolunteer.setAssignedBooth(volunteerEntity.getAssignedBooth());
        mongoVolunteer.setVolunteerAddress(volunteerEntity.getVolunteerAddress());
        mongoVolunteer.setCreatedTime(LocalDateTime.now());
        mongoVolunteer.setModifiedTime(LocalDateTime.now());
        
        // Map UserEntity
        if (volunteerEntity.getUserEntity() != null) {
            mongoVolunteer.setUserEntity(volunteerEntity.getUserEntity());
            mongoVolunteer.setEmail(volunteerEntity.getUserEntity().getEmail());
            mongoVolunteer.setMobileNumber(volunteerEntity.getUserEntity().getMobileNumber());
            mongoVolunteer.setLastName(volunteerEntity.getUserEntity().getLastName());
        }
        
        // Map ElectionEntity
        if (volunteerEntity.getElectionEntity() != null) {
            mongoVolunteer.setElectionEntity(volunteerEntity.getElectionEntity());
        }
        
        // Map Role ID
        if (volunteerEntity.getRoleId() != null) {
            mongoVolunteer.setRoleId(volunteerEntity.getRoleId());
        }
        
        return mongoVolunteer;
    }

    // Method to map VolunteerDailyActivityEntity to MongoVolunteerDailyActivity
    private MongoVolunteerDailyActivity mapToMongoVolunteerDailyActivity(VolunteerDailyActivityEntity entity) {
        MongoVolunteerDailyActivity mongoActivity = new MongoVolunteerDailyActivity();
        mongoActivity.setId(entity.getId().toString());
        mongoActivity.setVolunteerId(entity.getVolunteerId());
        mongoActivity.setAccountId(entity.getAccountId());
        mongoActivity.setCheckInTime(entity.getCheckInTime());
        mongoActivity.setCheckOutTime(entity.getCheckOutTime());
        mongoActivity.setHoursWorked(entity.getHoursWorked());
        mongoActivity.setChecked(entity.isChecked());
        mongoActivity.setCreatedTime(LocalDateTime.now());
        mongoActivity.setModifiedTime(LocalDateTime.now());
        return mongoActivity;
    }

    // Method to map VolunteerActivityLogsEntity to MongoVolunteerActivityLog
    private MongoVolunteerActivityLog mapToMongoVolunteerActivityLog(VolunteerActivityLogsEntity entity) {
        MongoVolunteerActivityLog mongoLog = new MongoVolunteerActivityLog();
        mongoLog.setId(entity.getId().toString());
        mongoLog.setVolunteerId(entity.getVolunteerId());
        mongoLog.setAccountId(entity.getAccountId());
        mongoLog.setActivityDate(entity.getActivityDate());
        mongoLog.setCurrentTimeStamp(entity.getCurrentTimeStamp());
        mongoLog.setLatitude(entity.getLatitude());
        mongoLog.setLongitude(entity.getLongitude());
        mongoLog.setDistanceFromPreviousLocation(entity.getDistanceFromPreviousLocation());
        mongoLog.setCreatedTime(LocalDateTime.now());
        return mongoLog;
    }

    // Implementation of the missing uploadVolunteerFromXlsxOrCsv method
    @Override
    @Transactional
    public ThedalResponse<VolunteerUploadSummary> uploadVolunteerFromXlsxOrCsv(MultipartFile file, Long electionId) {
        //requestDetails.checkUserRolePermission(RolePermission.CADRE_MANAGEMENT);
    	requestDetails.checkUserRolePermission("cadreList", "C");
        Long currentUserAccountId = requestDetails.getCurrentAccountId();
        Long currentUserId = requestDetails.getCurrentUserId();
        if (currentUserAccountId == null) {
            log.error("Account id not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }
        
        validateElectionOwnership(electionId, currentUserAccountId);
        ElectionEntity election = electionRepository.findById(electionId)
                .orElseThrow(() -> new ThedalException(ThedalError.ELECTION_NOT_FOUND, HttpStatus.NOT_FOUND));

        long startTime = System.currentTimeMillis();
        Long accountId = requestDetails.getCurrentAccountId();

        if (accountId == null) {
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        AccountEntity accountEntity = accountRepository.findById(accountId)
                .orElseThrow(() -> new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.BAD_REQUEST));

        if (!isSupportedFormat(file.getOriginalFilename()) || file.isEmpty()) {
            throw new ThedalException(ThedalError.INVALID_FILE_FORMAT, HttpStatus.BAD_REQUEST);
        }

        String folder = "volunteer_uploads";
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        String originalFileName = file.getOriginalFilename();
        String fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        String uniqueFileName = folder + "/volunteer_" + System.currentTimeMillis() + "_" + uniqueId + fileExtension;

        String fileUrl = null;
        Long bulkUploadId = null;
        VolunteerUploadSummary summary = new VolunteerUploadSummary();

        try {
            fileUrl = awsFileUpload.uploadMultipartFile(file, uniqueFileName, s3Filesbucket);
            log.info("File uploaded to S3 at: {}", fileUrl);

            VolunteerBulkUploadEntity volunteerBulkUploadEntity = new VolunteerBulkUploadEntity();
            volunteerBulkUploadEntity.setAccountId(accountId);
            volunteerBulkUploadEntity.setStartTime(LocalDateTime.now());
            volunteerBulkUploadEntity.setStatus(BulkUploadStatus.IN_PROGRESS);
            volunteerBulkUploadRepository.save(volunteerBulkUploadEntity);

            bulkUploadId = volunteerBulkUploadEntity.getId();

            Files fileEntity = new Files(HandlerType.BULKUPLOAD_FILES, volunteerBulkUploadEntity.getId(), originalFileName, fileUrl);
            Files files = filesRepository.save(fileEntity);

            if (fileExtension.equalsIgnoreCase(".xlsx")) {
                volunteerFileUploadService.processExcelFileAsync(bulkUploadId, accountEntity, fileUrl, election, volunteerBulkUploadEntity, summary, currentUserId);
            } else if (fileExtension.equalsIgnoreCase(".csv")) {
                volunteerFileUploadService.processCsvFileAsync(bulkUploadId, accountEntity, fileUrl, election, volunteerBulkUploadEntity, summary, currentUserId);
            }
            log.info("uploadVotersFromXlsxOrCsv: fileId:{}", files.getId());

            volunteerBulkUploadEntity.setStatus(BulkUploadStatus.COMPLETED);
            volunteerBulkUploadEntity.setEndTime(LocalDateTime.now());
            volunteerBulkUploadRepository.save(volunteerBulkUploadEntity);

            // Process volunteers after file upload is completed
            processUploadedVolunteersAndSaveBoothMappings(electionId, accountId);

        } catch (IOException e) {
            log.error("Error uploading file to S3", e);
            throw new ThedalException(ThedalError.FILE_PROCESSING_FAILED, HttpStatus.INTERNAL_SERVER_ERROR, "File upload to S3 failed.");
        } finally {
            long endTime = System.currentTimeMillis();
            log.info("Time taken to process file: {} ms", (endTime - startTime));
            summary.setTotalRecords(summary.getSuccessCount() + summary.getFailedCount());
        }

        return new ThedalResponse<>(ThedalSuccess.BULK_VOLUNTEERS_UPLOADED, summary);
    }

    private boolean isSupportedFormat(String originalFileName) {
        return originalFileName != null && (originalFileName.endsWith(".xlsx") || originalFileName.endsWith(".csv"));
    }
    
    // Method to process the uploaded volunteers and save booth mappings
    private void processUploadedVolunteersAndSaveBoothMappings(Long electionId, Long accountId) {
        Long currentUserAccountId = requestDetails.getCurrentAccountId();
        if (currentUserAccountId == null) {
            log.error("Account id not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }
        
        validateElectionOwnership(electionId, currentUserAccountId);
        List<VolunteerEntity> volunteers = volunteerRepo.findByElectionEntityIdAndAccountId(electionId, accountId);

        List<ElectionBooth> allBooths = electionBoothRepository.findByElectionIdAndAccountId(electionId, accountId);
        Map<Long, ElectionBooth> boothMap = allBooths.stream()
            .collect(Collectors.toMap(
                b -> Long.valueOf(b.getBoothNumber()), 
                b -> b
            ));

        // Step 2: Collect mappings
        List<VolunteerElectionBooth> mappingsToSave = new ArrayList<>();

        for (VolunteerEntity volunteer : volunteers) {
            List<Long> assignedBooths = volunteer.getAssignedBooth();
            if (assignedBooths != null && !assignedBooths.isEmpty()) {
                for (Long boothNumber : assignedBooths) {
                    ElectionBooth booth = boothMap.get(boothNumber);
                    if (booth == null) {
                        log.error("Booth {} not found", boothNumber);
                        continue;
                    }

                    VolunteerElectionBooth mapping = new VolunteerElectionBooth();
                    mapping.setVolunteerId(volunteer.getId());
                    mapping.setBoothNumber(booth.getBoothNumber());
                    mapping.setElectionId(electionId);
                    mapping.setAccountId(accountId);
                    mappingsToSave.add(mapping);
                }
            }
        }

        // Step 3: Bulk insert
        volunteerElectionBoothRepo.saveAll(mappingsToSave); 
    }

	private void validateElectionOwnership(Long electionId, Long accountId) {
        Optional<ElectionEntity> electionOpt = electionRepository.findByIdAndAccountId(electionId, accountId);
        if (!electionOpt.isPresent()) {
            log.error("Election ID {} does not belong to Account ID {}", electionId, accountId);
            throw new ThedalException(ThedalError.INVALID_ELECTION, HttpStatus.FORBIDDEN);  
        }
    }
	@Override
    @Transactional(rollbackFor = {Exception.class})
	public ThedalResponse<Void> saveVolunteer(SaveVolunteerDetailsDTO volunteerDetailsDto,Long electionId) {
		
		//requestDetails.checkUserRolePermission(RolePermission.CADRE_MANAGEMENT);
		requestDetails.checkUserRolePermission("cadreList", "C");
    	
	    Long currentUserAccountId = requestDetails.getCurrentAccountId();
	    Long currentUserId = requestDetails.getCurrentUserId();
	    if (currentUserAccountId == null) {
	        log.error("Account id not found, unauthorized access.");
	        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
	    }
		validateElectionOwnership(electionId, currentUserAccountId); 
	        log.info("Attempting to save volunteer by current account ID: {}",currentUserAccountId);
	        
	        String roleName = volunteerDetailsDto.getRoleName();
	        Role userRole = roleRepo.findByRoleNameAndAccountId(roleName, currentUserAccountId)
	            .orElseThrow(() -> {
	                log.error("Role {} not found for account ID: {}", roleName, currentUserAccountId);
	                return new ThedalException(ThedalError.ROLE_NOT_FOUND, HttpStatus.BAD_REQUEST);
	            });    

	        if (userRepo.existsByMobileNumber(volunteerDetailsDto.getMobileNumber())) 
	        	throw new ThedalException(ThedalError.DUPLICATE_MOBILE_NUMBER, HttpStatus.BAD_REQUEST);
	        
	        ElectionEntity election = electionRepository.findById(electionId).orElseThrow(()-> new ThedalException(ThedalError.ELECTION_NOT_FOUND, HttpStatus.NOT_FOUND));
	        
	        UserEntity user = new UserEntity();
	        user.setRole(userRole);
	        user.setFirstName(volunteerDetailsDto.getFirstName());
	        user.setLastName(volunteerDetailsDto.getLastName());
	        user.setEmail(volunteerDetailsDto.getEmail());
	        user.setMobileNumber(volunteerDetailsDto.getMobileNumber());
	        // hashing the password using bcrypt
	        user.setPassword(new BCryptPasswordEncoder().encode(volunteerDetailsDto.getPassword()));

	        user.setIsEmailVerified(true);
	        user.setIsMobileVerified(true);
	        user.setIsActive(true);
	        user.setCreatedAt(LocalDateTime.now());
	        user.setCreatedBy("volunteer create");
		user.setAccountEntity(accountRepository.findById(currentUserAccountId).orElseThrow(
				() -> new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.BAD_REQUEST)));
		log.info("Saving user object to DB: {}", user.toString());
		
		// Save to PostgreSQL and MongoDB with dual-write pattern
		UserEntity userEntity;
		try {
			userEntity = userRepo.save(user);
			try {
				MongoUser mongoUser = new MongoUser(userEntity);
				mongoUserRepository.save(mongoUser);
				log.info("Successfully created user in MongoDB: id={}, name={} {}", userEntity.getId(), userEntity.getFirstName(), userEntity.getLastName());
			} catch (Exception mongoEx) {
				log.error("Failed to create user in MongoDB: id={}, name={} {}", userEntity.getId(), userEntity.getFirstName(), userEntity.getLastName(), mongoEx);
				throw new RuntimeException("MongoDB save failed, triggering rollback", mongoEx);
			}
		} catch (Exception ex) {
			log.error("Failed to create user for volunteer", ex);
			throw new RuntimeException("User creation failed", ex);
		}
        
        // Map VolunteerDetailsDTO to VolunteerEntity
        VolunteerEntity volunteer = new VolunteerEntity();

	        // Mapping AddressDTO to the Address embedded field in VolunteerEntity
	        Address volunteerAddress = new Address();
	        if (volunteerDetailsDto.getAddress() != null) {
	            AddressDTO addressDto = volunteerDetailsDto.getAddress();
	            volunteerAddress.setStreet(addressDto.getStreet());
	            volunteerAddress.setCity(addressDto.getCity());
	            volunteerAddress.setState(addressDto.getState());
	            volunteerAddress.setPostalCode(addressDto.getPostalCode());
	            volunteerAddress.setCountry(addressDto.getCountry());
	        }
	        volunteer.setVolunteerAddress(volunteerAddress);
			volunteer.setWhatsAppNumber(volunteerDetailsDto.getWhatsAppNumber());

	        // Check and log the assignedBooth to ensure it's set
	        if (volunteerDetailsDto.getAssignedBooth() != null && !volunteerDetailsDto.getAssignedBooth().isEmpty()) {
	            log.info("Assigned booths: {}", volunteerDetailsDto.getAssignedBooth());
	            volunteer.setAssignedBooth(volunteerDetailsDto.getAssignedBooth()); // Ensure this is set correctly
	        }
	        
	        // Check and log the assignedFamilies to ensure it's set
	        if (volunteerDetailsDto.getAssignedFamilies() != null && !volunteerDetailsDto.getAssignedFamilies().isEmpty()) {
	            log.info("Assigned families: {}", volunteerDetailsDto.getAssignedFamilies());
	            volunteer.setAssignedFamilies(volunteerDetailsDto.getAssignedFamilies());
	        }
	        
	        volunteer.setStatus(volunteerDetailsDto.getStatus());
	        
	        // Automatically manage user activation based on volunteer status during creation
	        if ("inactive".equalsIgnoreCase(volunteerDetailsDto.getStatus().trim())) {
	            log.info("Setting user {} as inactive because volunteer is being created with inactive status", userEntity.getId());
	            userEntity.setIsActive(false);
	            userRepo.save(userEntity); // Save the user activation change
	        }
	        volunteer.setRemarks(volunteerDetailsDto.getRemarks());
	        volunteer.setAccountId(currentUserAccountId);
	        volunteer.setAdminUserId(currentUserId);
	        volunteer.setUserEntity(userEntity);
	        volunteer.setElectionEntity(election);
	        volunteer.setGender(volunteerDetailsDto.getGender());
	        	        
	        VolunteerEntity volunteerEntity = volunteerRepo.save(volunteer);
	        log.info("Volunteer with ID: {} successfully saved", volunteerEntity.getId());
	        
	     // Update VolunteerVsVoterReportEntity counts
	        VolunteerVsVoterReportEntity report = volunteerVsVoterReportRepository
	        	    .findByElectionIdAndVolunteerId(electionId, volunteerEntity.getId())
	        	    .orElseGet(() -> {
	        	        VolunteerVsVoterReportEntity newReport = new VolunteerVsVoterReportEntity();
	        	        newReport.setElectionId(electionId);
	        	        newReport.setVolunteerId(volunteerEntity.getId());
	        	        newReport.setAccountId(currentUserAccountId);
	        	        return newReport;
	        	    });

	        	// Log input values for debugging
	        	log.info("Input values - WhatsAppNumber: {}, RoleName: {}, AssignedBooth: {}, Address: {}",
	        	    volunteerDetailsDto.getWhatsAppNumber(),
	        	    volunteerDetailsDto.getRoleName(),
	        	    volunteerDetailsDto.getAssignedBooth(),
	        	    volunteerDetailsDto.getAddress());

	        	if (volunteerDetailsDto.getWhatsAppNumber() != null && !volunteerDetailsDto.getWhatsAppNumber().trim().isEmpty()) {
	        	    log.info("Incrementing WhatsAppNumberUpdated for volunteerId: {}", volunteerEntity.getId());
	        	    report.setTotalWhatsAppNumberUpdated((report.getTotalWhatsAppNumberUpdated() != null ? report.getTotalWhatsAppNumberUpdated() : 0L) + 1L);
	        	}
	        	if (volunteerDetailsDto.getRoleName() != null && !volunteerDetailsDto.getRoleName().trim().isEmpty()) {
	        	    log.info("Incrementing RolesUpdated for volunteerId: {}", volunteerEntity.getId());
	        	    report.setTotalRolesUpdated((report.getTotalRolesUpdated() != null ? report.getTotalRolesUpdated() : 0L) + 1L);
	        	}
	        	if (volunteerDetailsDto.getAssignedBooth() != null && !volunteerDetailsDto.getAssignedBooth().isEmpty()) {
	        	    log.info("Incrementing BoothsUpdated for volunteerId: {}", volunteerEntity.getId());
	        	    report.setTotalBoothsUpdated((report.getTotalBoothsUpdated() != null ? report.getTotalBoothsUpdated() : 0L) + 1L);
	        	}
	        	if (volunteerDetailsDto.getAddress() != null &&
	        	    (volunteerDetailsDto.getAddress().getStreet() != null ||
	        	     volunteerDetailsDto.getAddress().getCity() != null ||
	        	     volunteerDetailsDto.getAddress().getState() != null ||
	        	     volunteerDetailsDto.getAddress().getPostalCode() != null ||
	        	     volunteerDetailsDto.getAddress().getCountry() != null)) {
	        	    log.info("Incrementing AddressUpdated for volunteerId: {}", volunteerEntity.getId());
	        	    report.setTotalAddressUpdated((report.getTotalAddressUpdated() != null ? report.getTotalAddressUpdated() : 0L) + 1L);
	        	}

	        	log.info("Saving report with WhatsApp: {}, Roles: {}, Booths: {}, Address: {}",
	        	    report.getTotalWhatsAppNumberUpdated(),
	        	    report.getTotalRolesUpdated(),
	        	    report.getTotalBoothsUpdated(),
	        	    report.getTotalAddressUpdated());
	        	volunteerVsVoterReportRepository.save(report);

	        if (volunteerDetailsDto.getAssignedBooth() != null && !volunteerDetailsDto.getAssignedBooth().isEmpty()) { 
	            for (Long boothNumber : volunteerDetailsDto.getAssignedBooth()) {
	                // Check if booth exists in part_manager table using booth partNo
	                Optional<PartManager> partManagerOpt = partManagerRepository.findByPartNoAndElectionIdAndAccountId(
	                    String.valueOf(boothNumber), electionId, currentUserAccountId);
	                
	                if (!partManagerOpt.isPresent()) {
	                    log.error("Booth not found with partNo: {} in part_manager table for electionId: {} and accountId: {}", 
	                        boothNumber, electionId, currentUserAccountId);
	                    throw new ThedalException(ThedalError.BOOTH_NOT_FOUND, HttpStatus.NOT_FOUND);
	                }

	                VolunteerElectionBooth volunteerBooth = new VolunteerElectionBooth();
	                volunteerBooth.setVolunteerId(volunteerEntity.getId());
	                volunteerBooth.setBoothNumber(boothNumber.intValue()); // Convert Long to Integer
	                volunteerBooth.setElectionId(electionId);
	                volunteerBooth.setAccountId(currentUserAccountId);

	                volunteerElectionBoothRepo.save(volunteerBooth);
	            }
	        }  
	                
	        // --- Dual-write to MongoDB ---
	        MongoVolunteer mongoVolunteer = mapToMongoVolunteer(volunteerEntity);
	        mongoVolunteerRepository.save(mongoVolunteer);
	        // --- End dual-write ---
	        
	        return new ThedalResponse<>(ThedalSuccess.VOLUNTEER_CREATED);
	}
    
    
    private boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        Pattern pattern = Pattern.compile(emailRegex);
        return pattern.matcher(email).matches();
    }

    @Override
    public ThedalResponse<VolunteerDetailsDTO> getVolunteerByUserId(Long userId, Long electionId) {
    	Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account id not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }
		
        log.info("Fetching volunteer by user ID: {} and election ID: {}", userId, electionId);
		validateElectionOwnership(electionId, accountId);
        VolunteerEntity volunteerEntity = volunteerRepo.findByUserEntityIdAndElectionEntityId(userId, electionId)
                .orElseThrow(() -> {
                    log.warn("No volunteer found for user ID: {} and election ID: {}", userId, electionId);
                    return new ThedalException(ThedalError.VOLUNTEER_NOT_FOUND, HttpStatus.NOT_FOUND);
                });

        log.info("Volunteer found for user ID: {} and election ID: {}", userId, electionId);
        VolunteerDetailsDTO volunteerDetailsDTO = mapToVolunteerDetailsDTO(volunteerEntity, accountId);
        return new ThedalResponse<>(ThedalSuccess.VOLUNTEER_FOUND, volunteerDetailsDTO);
    }

private VolunteerDetailsDTO mapToVolunteerDetailsDTO(VolunteerEntity volunteerEntity, Long accountId) {
    
    Address address = volunteerEntity.getVolunteerAddress();

    AddressDTO addressDTO = null;
    if (address != null) {
        addressDTO = new AddressDTO(
            address.getStreet(),
            address.getCity(),
            address.getState(),
            address.getPostalCode(),
            address.getCountry()
        );
    }

    List<Long> assignedBooths = (volunteerEntity.getAssignedBooth() != null)
            ? volunteerEntity.getAssignedBooth()
            : Collections.emptyList();

    List<Long> assignedFamilies = (volunteerEntity.getAssignedFamilies() != null)
            ? volunteerEntity.getAssignedFamilies()
            : Collections.emptyList();

    
    String roleName = volunteerEntity.getUserEntity().getRole().getRoleName();

    return new VolunteerDetailsDTO(
        volunteerEntity.getId(),
        volunteerEntity.getUserEntity().getId(),
        volunteerEntity.getUserEntity().getFirstName(),
        volunteerEntity.getUserEntity().getLastName(),
        volunteerEntity.getUserEntity().getEmail(),
        volunteerEntity.getUserEntity().getMobileNumber(),
        addressDTO,
        assignedBooths,
        assignedFamilies,
        volunteerEntity.getStatus(),
        volunteerEntity.getUserEntity().getProfilePicture(),
        volunteerEntity.getRemarks(),
        accountId,
        volunteerEntity.getGender(),
        volunteerEntity.getWhatsAppNumber(),
        roleName,
        null // activeDeviceCount - will be populated separately when requested
    );
}

//    @Override
//	public ThedalResponse<Void> updateVolunteer(Long userId, Long electionId, VolunteerDetailsUpdate volunteerDetailsUpdate) {
//
//	    requestDetails.checkUserRolePermission(RolePermission.CADRE_MANAGEMENT);
//	    Long accountId = requestDetails.getCurrentAccountId();
//	    if (accountId == null) {
//	        log.error("Account id not found, unauthorized access.");
//	        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
//	    }
//		
//	    log.info("Updating volunteer with userId: {}, electionId: {}", userId, electionId);
//	    validateElectionOwnership(electionId, accountId);
//	    Optional<VolunteerEntity> optionalVolunteer = volunteerRepo.findByUserEntityIdAndElectionEntityId(userId, electionId);
//	    
//	    if (!optionalVolunteer.isPresent()) {
//	        throw new ThedalException(ThedalError.VOLUNTEER_NOT_FOUND, HttpStatus.NOT_FOUND);
//	    }
//
//	    VolunteerEntity volunteerEntity = optionalVolunteer.get();
//	    UserEntity userEntity = volunteerEntity.getUserEntity();
//	    
//	    if (volunteerDetailsUpdate.getRoleName() != null) {
//	        Role role = roleRepo.findByRoleNameAndAccountId(volunteerDetailsUpdate.getRoleName(), accountId)
//	            .orElseThrow(() -> {
//	                log.error("Role {} not found for account ID: {}", volunteerDetailsUpdate.getRoleName(), accountId);
//	                return new ThedalException(ThedalError.ROLE_NOT_FOUND, HttpStatus.BAD_REQUEST);
//	            });
//	        userEntity.setRole(role);
//	        volunteerEntity.setRoleId(role.getId());
//	    }
//	    
//	 // Email handling: Email is optional and reusable
//	    String currentEmail = userEntity.getEmail(); // May be null
//	    String newEmail = volunteerDetailsUpdate.getEmail(); // May be null
//
//	    if (newEmail != null && !newEmail.trim().isEmpty()) {
//	        // Trim the email to remove leading/trailing spaces
//	        newEmail = newEmail.trim();
//	        // Validate email format if provided
//	        if (!isValidEmail(newEmail)) {
//	            throw new ThedalException(ThedalError.INVALID_EMAIL, HttpStatus.BAD_REQUEST,
//	                "The email address " + newEmail + " is invalid.");
//	        }
//	        // No duplicate check since email can be reused
//	        userEntity.setEmail(newEmail);
//	    } else if (newEmail != null && newEmail.trim().isEmpty()) {
//	        // Allow setting email to null if an empty string is provided
//	        userEntity.setEmail(null);
//	    }
//
//	    // Mobile number validation: If the mobile number has changed, check for duplicates
//	    if (volunteerDetailsUpdate.getMobileNumber() != null && !volunteerEntity.getUserEntity().getMobileNumber().equals(volunteerDetailsUpdate.getMobileNumber())) {
//	        if (userRepo.existsByMobileNumber(volunteerDetailsUpdate.getMobileNumber())) {
//	            throw new ThedalException(ThedalError.DUPLICATE_MOBILE_NUMBER, HttpStatus.BAD_REQUEST);
//	        }
//	    }
//	    
//	    // Only update if the value is provided in the DTO
//	    if (volunteerDetailsUpdate.getFirstName() != null) {
//	        userEntity.setFirstName(volunteerDetailsUpdate.getFirstName());
//	    }
//	    
//	    if (volunteerDetailsUpdate.getLastName() != null) {
//	        userEntity.setLastName(volunteerDetailsUpdate.getLastName());
//	    }
//	    
//	    if (volunteerDetailsUpdate.getEmail() != null) {
//	        userEntity.setEmail(volunteerDetailsUpdate.getEmail());
//	    }
//	    
//	    if (volunteerDetailsUpdate.getMobileNumber() != null) {
//	        userEntity.setMobileNumber(volunteerDetailsUpdate.getMobileNumber());
//	    }
//	    
//	    if (volunteerDetailsUpdate.getStatus() != null) {
//	        volunteerEntity.setStatus(volunteerDetailsUpdate.getStatus());
//	    }
//	    
//	    if (volunteerDetailsUpdate.getPhotoUrl() != null) {
//	        volunteerEntity.setPhotoUrl(volunteerDetailsUpdate.getPhotoUrl());
//	    }
//	    
//	    if (volunteerDetailsUpdate.getRemarks() != null) {
//	        volunteerEntity.setRemarks(volunteerDetailsUpdate.getRemarks());
//	    }
//
//		if (volunteerDetailsUpdate.getWhatsAppNumber() != null) {
//	        volunteerEntity.setWhatsAppNumber(volunteerDetailsUpdate.getWhatsAppNumber());
//	    }
//
//		if (volunteerDetailsUpdate.getGender() != null) {
//	        volunteerEntity.setGender(volunteerDetailsUpdate.getGender());
//	    }
//
//	    // Update address details only if provided
//	    AddressDTO addressDTO = volunteerDetailsUpdate.getAddress();
//	    if (addressDTO != null) {
//	        Address volunteerAddress = volunteerEntity.getVolunteerAddress();
//	        
//	        // Update each field in the address if provided
//	        if (addressDTO.getStreet() != null) {
//	            volunteerAddress.setStreet(addressDTO.getStreet());
//	        }
//	        if (addressDTO.getCity() != null) {
//	            volunteerAddress.setCity(addressDTO.getCity());
//	        }
//	        if (addressDTO.getState() != null) {
//	            volunteerAddress.setState(addressDTO.getState());
//	        }
//	        if (addressDTO.getPostalCode() != null) {
//	            volunteerAddress.setPostalCode(addressDTO.getPostalCode());
//	        }
//	        if (addressDTO.getCountry() != null) {
//	            volunteerAddress.setCountry(addressDTO.getCountry());
//	        }
//	    }
//
//	    // Save the updated volunteer entity
//	    userRepo.save(userEntity);
//	    volunteerRepo.save(volunteerEntity);
//	    log.info("Volunteer with userId: {} successfully updated", userId);
//
//	    // --- Dual-write to MongoDB ---
//        Optional<MongoVolunteer> mongoOpt = mongoVolunteerRepository.findAll().stream()
//            .filter(v -> v.getUserEntity() != null && v.getUserEntity().getId().equals(userId)
//                && v.getElectionEntity() != null && v.getElectionEntity().getId().equals(electionId))
//            .findFirst();
//        MongoVolunteer mongoVolunteer = mongoOpt.orElseGet(() -> new MongoVolunteer());
//        mapToMongoVolunteer(volunteerEntity, mongoVolunteer);
//        mongoVolunteerRepository.save(mongoVolunteer);
//        // --- End dual-write ---
//	    
//	    return new ThedalResponse<>(ThedalSuccess.VOLUNTEER_UPDATED);
//	}

@Override
@Transactional(rollbackFor = {Exception.class})
public ThedalResponse<Void> updateVolunteer(Long userId, Long electionId, VolunteerDetailsUpdate volunteerDetailsUpdate) {
    //requestDetails.checkUserRolePermission(RolePermission.CADRE_MANAGEMENT);
	requestDetails.checkUserRolePermission("cadreList", "U");
    Long accountId = requestDetails.getCurrentAccountId();
    if (accountId == null) {
        log.error("Account id not found, unauthorized access.");
        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
    }

    log.info("Updating volunteer with userId: {}, electionId: {}", userId, electionId);
    validateElectionOwnership(electionId, accountId);
    Optional<VolunteerEntity> optionalVolunteer = volunteerRepo.findByUserEntityIdAndElectionEntityId(userId, electionId);

    if (!optionalVolunteer.isPresent()) {
        log.error("Volunteer not found for userId: {} and electionId: {}", userId, electionId);
        throw new ThedalException(ThedalError.VOLUNTEER_NOT_FOUND, HttpStatus.NOT_FOUND);
    }

    VolunteerEntity volunteerEntity = optionalVolunteer.get();
    UserEntity userEntity = volunteerEntity.getUserEntity();

    VolunteerVsVoterReportEntity report = volunteerVsVoterReportRepository
            .findByElectionIdAndVolunteerId(electionId, volunteerEntity.getId())
            .orElseGet(() -> {
                VolunteerVsVoterReportEntity newReport = new VolunteerVsVoterReportEntity();
                newReport.setElectionId(electionId);
                newReport.setVolunteerId(volunteerEntity.getId());
                newReport.setAccountId(accountId);
                return newReport;
            });

    // Log input values for debugging
    log.info("Input values - WhatsAppNumber: {}, RoleName: {}, Address: {}, FirstName: {}, LastName: {}, Status: {}, Gender: {}, PhotoUrl: {}, Remarks: {}",
            volunteerDetailsUpdate.getWhatsAppNumber(),
            volunteerDetailsUpdate.getRoleName(),
            volunteerDetailsUpdate.getAddress(),
            volunteerDetailsUpdate.getFirstName(),
            volunteerDetailsUpdate.getLastName(),
            volunteerDetailsUpdate.getStatus(),
            volunteerDetailsUpdate.getGender(),
            volunteerDetailsUpdate.getPhotoUrl(),
            volunteerDetailsUpdate.getRemarks());

    // Update Role
    if (volunteerDetailsUpdate.getRoleName() != null && !volunteerDetailsUpdate.getRoleName().trim().isEmpty()) {
        Role role = roleRepo.findByRoleNameAndAccountId(volunteerDetailsUpdate.getRoleName(), accountId)
                .orElseThrow(() -> {
                    log.error("Role {} not found for account ID: {}", volunteerDetailsUpdate.getRoleName(), accountId);
                    return new ThedalException(ThedalError.ROLE_NOT_FOUND, HttpStatus.BAD_REQUEST);
                });
        if (!role.getId().equals(volunteerEntity.getRoleId())) {
            log.info("Updating role from {} to {} for volunteerId: {}", 
                    volunteerEntity.getRoleId(), role.getId(), volunteerEntity.getId());
            userEntity.setRole(role);
            volunteerEntity.setRoleId(role.getId());
            report.setTotalRolesUpdated((report.getTotalRolesUpdated() != null ? report.getTotalRolesUpdated() : 0L) + 1L);
        }
    }

    // Update Email
    String currentEmail = userEntity.getEmail();
    String newEmail = volunteerDetailsUpdate.getEmail();
    if (newEmail != null && !newEmail.trim().isEmpty()) {
        newEmail = newEmail.trim();
        if (!isValidEmail(newEmail)) {
            throw new ThedalException(ThedalError.INVALID_EMAIL, HttpStatus.BAD_REQUEST,
                    "The email address " + newEmail + " is invalid.");
        }
        if (!newEmail.equals(currentEmail)) {
            log.info("Updating email from {} to {} for userId: {}", currentEmail, newEmail, userId);
            userEntity.setEmail(newEmail);
        }
    } else if (newEmail != null && newEmail.trim().isEmpty() && currentEmail != null) {
        log.info("Clearing email for userId: {}", userId);
        userEntity.setEmail(null);
    }

    // Update Mobile Number
    if (volunteerDetailsUpdate.getMobileNumber() != null &&
            !volunteerDetailsUpdate.getMobileNumber().equals(userEntity.getMobileNumber())) {
        if (userRepo.existsByMobileNumber(volunteerDetailsUpdate.getMobileNumber())) {
            log.error("Duplicate mobile number: {}", volunteerDetailsUpdate.getMobileNumber());
            throw new ThedalException(ThedalError.DUPLICATE_MOBILE_NUMBER, HttpStatus.BAD_REQUEST);
        }
        log.info("Updating mobile number from {} to {} for userId: {}", 
                userEntity.getMobileNumber(), volunteerDetailsUpdate.getMobileNumber(), userId);
        userEntity.setMobileNumber(volunteerDetailsUpdate.getMobileNumber());
        report.setTotalMobileNumberUpdated((report.getTotalMobileNumberUpdated() != null ? report.getTotalMobileNumberUpdated() : 0L) + 1L);
    }

    // Update WhatsApp Number
    if (volunteerDetailsUpdate.getWhatsAppNumber() != null &&
            !volunteerDetailsUpdate.getWhatsAppNumber().equals(volunteerEntity.getWhatsAppNumber())) {
        log.info("Updating WhatsAppNumber from {} to {} for volunteerId: {}", 
                volunteerEntity.getWhatsAppNumber(), volunteerDetailsUpdate.getWhatsAppNumber(), volunteerEntity.getId());
        volunteerEntity.setWhatsAppNumber(volunteerDetailsUpdate.getWhatsAppNumber());
        if (!volunteerDetailsUpdate.getWhatsAppNumber().trim().isEmpty()) {
            report.setTotalWhatsAppNumberUpdated((report.getTotalWhatsAppNumberUpdated() != null ? report.getTotalWhatsAppNumberUpdated() : 0L) + 1L);
        }
    }

    // Update Address
    AddressDTO addressDTO = volunteerDetailsUpdate.getAddress();
    if (addressDTO != null) {
        Address volunteerAddress = volunteerEntity.getVolunteerAddress();
        if (volunteerAddress == null) {
            volunteerAddress = new Address();
            volunteerEntity.setVolunteerAddress(volunteerAddress);
        }
        boolean addressChanged = false;

        if (addressDTO.getStreet() != null && !addressDTO.getStreet().equals(volunteerAddress.getStreet())) {
            log.info("Updating street from {} to {} for volunteerId: {}", 
                    volunteerAddress.getStreet(), addressDTO.getStreet(), volunteerEntity.getId());
            volunteerAddress.setStreet(addressDTO.getStreet());
            addressChanged = true;
        }
        if (addressDTO.getCity() != null && !addressDTO.getCity().equals(volunteerAddress.getCity())) {
            log.info("Updating city from {} to {} for volunteerId: {}", 
                    volunteerAddress.getCity(), addressDTO.getCity(), volunteerEntity.getId());
            volunteerAddress.setCity(addressDTO.getCity());
            addressChanged = true;
        }
        if (addressDTO.getState() != null && !addressDTO.getState().equals(volunteerAddress.getState())) {
            log.info("Updating state from {} to {} for volunteerId: {}", 
                    volunteerAddress.getState(), addressDTO.getState(), volunteerEntity.getId());
            volunteerAddress.setState(addressDTO.getState());
            addressChanged = true;
        }
        if (addressDTO.getPostalCode() != null && !addressDTO.getPostalCode().equals(volunteerAddress.getPostalCode())) {
            log.info("Updating postalCode from {} to {} for volunteerId: {}", 
                    volunteerAddress.getPostalCode(), addressDTO.getPostalCode(), volunteerEntity.getId());
            volunteerAddress.setPostalCode(addressDTO.getPostalCode());
            addressChanged = true;
        }
        if (addressDTO.getCountry() != null && !addressDTO.getCountry().equals(volunteerAddress.getCountry())) {
            log.info("Updating country from {} to {} for volunteerId: {}", 
                    volunteerAddress.getCountry(), addressDTO.getCountry(), volunteerEntity.getId());
            volunteerAddress.setCountry(addressDTO.getCountry());
            addressChanged = true;
        }

        if (addressChanged) {
            log.info("Incrementing AddressUpdated for volunteerId: {}", volunteerEntity.getId());
            report.setTotalAddressUpdated((report.getTotalAddressUpdated() != null ? report.getTotalAddressUpdated() : 0L) + 1L);
        }
    }

    // Update First Name
    if (volunteerDetailsUpdate.getFirstName() != null &&
            !volunteerDetailsUpdate.getFirstName().equals(userEntity.getFirstName())) {
        log.info("Updating firstName from {} to {} for userId: {}", 
                userEntity.getFirstName(), volunteerDetailsUpdate.getFirstName(), userId);
        userEntity.setFirstName(volunteerDetailsUpdate.getFirstName());
    }

    // Update Last Name
    if (volunteerDetailsUpdate.getLastName() != null &&
            !volunteerDetailsUpdate.getLastName().equals(userEntity.getLastName())) {
        log.info("Updating lastName from {} to {} for userId: {}", 
                userEntity.getLastName(), volunteerDetailsUpdate.getLastName(), userId);
        userEntity.setLastName(volunteerDetailsUpdate.getLastName());
    }

    // Update Status
    if (volunteerDetailsUpdate.getStatus() != null &&
            !volunteerDetailsUpdate.getStatus().equals(volunteerEntity.getStatus())) {
        log.info("Updating status from {} to {} for volunteerId: {}", 
                volunteerEntity.getStatus(), volunteerDetailsUpdate.getStatus(), volunteerEntity.getId());
        volunteerEntity.setStatus(volunteerDetailsUpdate.getStatus());
        
        // Automatically manage user activation based on volunteer status
        if ("inactive".equalsIgnoreCase(volunteerDetailsUpdate.getStatus().trim())) {
            // Deactivate user when any volunteer status becomes "inactive"
            log.info("Deactivating user {} because volunteer status changed to inactive", userId);
            userEntity.setIsActive(false);
        } else if ("active".equalsIgnoreCase(volunteerDetailsUpdate.getStatus().trim())) {
            // Reactivate user if they're currently inactive and this volunteer becomes active
            if (!userEntity.getIsActive()) {
                log.info("Reactivating user {} because volunteer status changed to active", userId);
                userEntity.setIsActive(true);
            }
        }
    }

    // Update Photo URL
    if (volunteerDetailsUpdate.getPhotoUrl() != null &&
            !volunteerDetailsUpdate.getPhotoUrl().equals(volunteerEntity.getPhotoUrl())) {
        log.info("Updating photoUrl from {} to {} for volunteerId: {}", 
                volunteerEntity.getPhotoUrl(), volunteerDetailsUpdate.getPhotoUrl(), volunteerEntity.getId());
        volunteerEntity.setPhotoUrl(volunteerDetailsUpdate.getPhotoUrl());
    }

    // Update Remarks
    if (volunteerDetailsUpdate.getRemarks() != null &&
            !volunteerDetailsUpdate.getRemarks().equals(volunteerEntity.getRemarks())) {
        log.info("Updating remarks from {} to {} for volunteerId: {}", 
                volunteerEntity.getRemarks(), volunteerDetailsUpdate.getRemarks(), volunteerEntity.getId());
        volunteerEntity.setRemarks(volunteerDetailsUpdate.getRemarks());
    }

    // Update Gender
    if (volunteerDetailsUpdate.getGender() != null &&
            !volunteerDetailsUpdate.getGender().equals(volunteerEntity.getGender())) {
        log.info("Updating gender from {} to {} for volunteerId: {}", 
                volunteerEntity.getGender(), volunteerDetailsUpdate.getGender(), volunteerEntity.getId());
        volunteerEntity.setGender(volunteerDetailsUpdate.getGender());
    }

    // Update Assigned Families
    if (volunteerDetailsUpdate.getAssignedFamilies() != null) {
        log.info("Updating assignedFamilies from {} to {} for volunteerId: {}", 
                volunteerEntity.getAssignedFamilies(), volunteerDetailsUpdate.getAssignedFamilies(), volunteerEntity.getId());
        volunteerEntity.setAssignedFamilies(volunteerDetailsUpdate.getAssignedFamilies());
    }

    // Save updated entities
    log.info("Saving updated UserEntity for userId: {}", userId);
    userRepo.save(userEntity);
    log.info("Saving updated VolunteerEntity for volunteerId: {}", volunteerEntity.getId());
    volunteerRepo.save(volunteerEntity);
    log.info("Saving updated VolunteerVsVoterReportEntity: WhatsApp: {}, Roles: {}, Booths: {}, Address: {}",
            report.getTotalWhatsAppNumberUpdated(),
            report.getTotalRolesUpdated(),
            report.getTotalBoothsUpdated(),
            report.getTotalAddressUpdated());
    volunteerVsVoterReportRepository.save(report);

    // Sync with MongoDB
    Optional<MongoVolunteer> mongoOpt = mongoVolunteerRepository.findAll().stream()
            .filter(v -> v.getUserEntity() != null && v.getUserEntity().getId().equals(userId)
                    && v.getElectionEntity() != null && v.getElectionEntity().getId().equals(electionId))
            .findFirst();
    MongoVolunteer mongoVolunteer = mongoOpt.orElseGet(() -> new MongoVolunteer());
    mapToMongoVolunteer(volunteerEntity, mongoVolunteer);
    log.info("Saving updated MongoVolunteer for volunteerId: {}", volunteerEntity.getId());
    mongoVolunteerRepository.save(mongoVolunteer);

    log.info("Volunteer with userId: {} successfully updated", userId);
    return new ThedalResponse<>(ThedalSuccess.VOLUNTEER_UPDATED);
}

	@Override
	@Transactional(rollbackFor = {Exception.class})
	public ThedalResponse<Void> deleteVolunteer(Long userId, Long electionId) {
		
		//requestDetails.checkUserRolePermission(RolePermission.CADRE_MANAGEMENT);
		requestDetails.checkUserRolePermission("cadreList", "D");
		
		Long accountId = requestDetails.getCurrentAccountId();
	    if (accountId == null) {
	        log.error("Account id not found, unauthorized access.");
	        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
	    }
	    
	    log.info("Attempting to delete volunteer with userId: {}, electionId: {}", userId, electionId);
		validateElectionOwnership(electionId, accountId);
	    Optional<VolunteerEntity> optionalVolunteer = volunteerRepo.findByUserEntityIdAndElectionEntityId(userId, electionId);
	    
            if (optionalVolunteer.isPresent()) {
            	VolunteerEntity volunteerEntity = optionalVolunteer.get();
            	
            	// Get user entity before deleting volunteer
            	UserEntity userEntity = volunteerEntity.getUserEntity();
            	
            	// Step 1: Delete volunteer entity
            	volunteerRepo.delete(volunteerEntity);
            	log.info("Volunteer entity deleted for userId: {}, electionId: {}", userId, electionId);
            	
            	// Step 2: Delete from MongoDB volunteer collection
            	try {
	            	mongoVolunteerRepository.findAll().stream()
	            		.filter(v -> v.getUserEntity() != null && v.getUserEntity().getId().equals(userId)
	            				&& v.getElectionEntity() != null && v.getElectionEntity().getId().equals(electionId))
	            		.findFirst()
	            		.ifPresent(mv -> {
	            			mongoVolunteerRepository.deleteById(mv.getId());
	            			log.info("Deleted volunteer from MongoDB: userId={}, electionId={}", userId, electionId);
	            		});
            	} catch (Exception mongoEx) {
            		log.error("Failed to delete volunteer from MongoDB: userId={}, electionId={}", userId, electionId, mongoEx);
            		// Continue with user deletion even if MongoDB fails
            	}
            	
            	// Step 3: Check if user has any other volunteers across elections
            	List<VolunteerEntity> otherVolunteers = volunteerRepo.findAllByUserEntityId(userId);
            	
            	if (otherVolunteers.isEmpty() && userEntity != null) {
            		// User has no other volunteer records, safe to delete user
            		log.info("User has no other volunteer records. Deleting user entity: userId={}", userId);
            		
            		try {
            			// Delete user from PostgreSQL
            			userRepo.delete(userEntity);
            			log.info("User entity deleted from PostgreSQL: userId={}", userId);
            			
            			// Delete user from MongoDB
            			mongoUserRepository.findById(userId.toString()).ifPresent(mongoUser -> {
            				mongoUserRepository.delete(mongoUser);
            				log.info("User deleted from MongoDB: userId={}", userId);
            			});
            		} catch (Exception ex) {
            			log.error("Error deleting user entity: userId={}", userId, ex);
            			throw new RuntimeException("Failed to delete user entity, triggering rollback", ex);
            		}
            	} else {
            		log.info("User has {} other volunteer record(s). User entity retained: userId={}", 
            				otherVolunteers.size(), userId);
            	}
            	          	
            	return new ThedalResponse<>(ThedalSuccess.VOLUNTEER_DELETED);
            } else {
                log.warn("Volunteer not found with userId: {}, electionId: {}", userId, electionId);
                throw new ThedalException(ThedalError.VOLUNTEER_NOT_FOUND, HttpStatus.NOT_FOUND);
            }
	}

	@Override
	public ThedalResponse<Page<VolunteerLocationDto>> getAllVolunteersWithLocationData(int page, int size, Long userId, Long electionId, Long role) {
		
		ElectionEntity election = electionRepository.findById(electionId)
				.orElseThrow(() -> new ThedalException(ThedalError.ELECTION_NOT_FOUND, HttpStatus.NOT_FOUND));
		
		Long accountId = requestDetails.getCurrentAccountId(); 
	    if (accountId == null) {
	        log.error("Account ID not found, unauthorized access.");
	        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
	    }
		validateElectionOwnership(electionId, accountId);
	    Pageable pageable = PageRequest.of(page, size);
		
	    Page<VolunteerEntity> volunteerPage;
	    if (role != null) {
	        volunteerPage = volunteerRepo.findByAccountIdAndUserEntityIdAndElectionEntityAndRoleId(accountId, userId, election, role, pageable);
	    } else {
	        volunteerPage = volunteerRepo.findByAccountIdAndUserEntityIdAndElectionEntity(accountId, userId, election, pageable);
	    }
	    
        Page<VolunteerLocationDto> volunteerLocationDtos = volunteerPage.map(this::mapToVolunteerLocationDto);
        
        return new ThedalResponse<>(ThedalSuccess.VOLUNTEER_GET_SUCCESS, volunteerLocationDtos);
        
	}
	
    private VolunteerLocationDto mapToVolunteerLocationDto(VolunteerEntity volunteer) {
        VolunteerLocationDto dto = new VolunteerLocationDto();
        dto.setVolunteerId(volunteer.getId());
        dto.setUserId(volunteer.getUserEntity().getId());
        dto.setFirstName(volunteer.getUserEntity().getFirstName());
        dto.setLastName(volunteer.getUserEntity().getLastName());
        dto.setMobileNumber(volunteer.getUserEntity().getMobileNumber());
        
        List<Long> assignedBooths = volunteer.getAssignedBooth() != null && !volunteer.getAssignedBooth().isEmpty()
                ? volunteer.getAssignedBooth()  // Directly use List<Long> from entity
                : Collections.emptyList();

        Optional<VolunteerActivityLogsEntity> lastActivityLogOptional = volunteerActivityLogsRepository
				.findTopByVolunteerIdOrderByCurrentTimeStampDesc(volunteer.getId());
		if (lastActivityLogOptional.isPresent()) {
			log.info("mapToVolunteerLocationDto method:lastActivityLogOptional:true ");
			VolunteerActivityLogsEntity lastActivityLog = lastActivityLogOptional.get();
			double lastLatitude = lastActivityLog.getLatitude();
			double lastLongitude = lastActivityLog.getLongitude();
	
        LocationDto locationDto = new LocationDto();
        locationDto.setLatitude(lastLatitude);
        locationDto.setLongitude(lastLongitude);
        dto.setLocation(locationDto);
		}
		dto.setAssignedBooth(assignedBooths);
        dto.setStatus(volunteer.getStatus());
        return dto;
    }
    
    @Override
	public ThedalResponse<Void> activityCheckIn(Long electionId) {
		log.info("inside activityCheckIn: user id:{}",getCurrentUserId());
		Long accountId = requestDetails.getCurrentAccountId(); 
	    if (accountId == null) {
	        log.error("Account ID not found, unauthorized access.");
	        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
	    }
		validateElectionOwnership(electionId, accountId);
		ElectionEntity election = electionRepository.findById(electionId)
				.orElseThrow(() -> new ThedalException(ThedalError.ELECTION_NOT_FOUND, HttpStatus.NOT_FOUND));
		VolunteerEntity volunteer = volunteerRepo.findByElectionEntityAndUserEntity_Id(election,getCurrentUserId()).orElseThrow(()-> new ThedalException(ThedalError.VOLUNTEER_NOT_FOUND, HttpStatus.NOT_FOUND));
	    
		 LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
	     LocalDateTime endOfDay = startOfDay.toLocalDate().atTime(LocalTime.MAX);
	     
	    
	     if(volunteerDailyActivityRepository.existsByVolunteerIdAndCheckInTimeBetweenAndIsChecked(volunteer.getId(), startOfDay, endOfDay,true))
	    	 throw new ThedalException(ThedalError.ALREADY_CHECKEDIN, HttpStatus.BAD_REQUEST);
	     
	     VolunteerDailyActivityEntity volunteerDailyActivity = volunteerDailyActivityRepository
					.findByVolunteerIdAndCheckInTimeBetween(volunteer.getId(), startOfDay, endOfDay)
					.orElse(new VolunteerDailyActivityEntity());
	     
	 
		volunteerDailyActivity.setAccountId(accountService.getCurrentAccountFromRequest().getId());
		if(volunteerDailyActivity.getCheckInTime() == null) {
		volunteerDailyActivity.setCheckInTime(LocalDateTime.now().isAfter(endOfDay) ? endOfDay:LocalDateTime.now());
		}
		volunteerDailyActivity.setVolunteerId(volunteer.getId());
		volunteerDailyActivity.setChecked(true);
	    VolunteerDailyActivityEntity savedActivity = volunteerDailyActivityRepository.save(volunteerDailyActivity);
	    
	    // --- Dual-write to MongoDB ---
	    try {
	        MongoVolunteerDailyActivity mongoActivity = mapToMongoVolunteerDailyActivity(savedActivity);
	        mongoVolunteerService.saveVolunteerDailyActivity(mongoActivity);
	        log.info("Successfully synced check-in activity to MongoDB for volunteer: {}", volunteer.getId());
	    } catch (Exception e) {
	        log.error("Failed to sync check-in activity to MongoDB for volunteer: {}, error: {}", volunteer.getId(), e.getMessage());
	        // Continue with the operation even if MongoDB sync fails
	    }
	    // --- End dual-write ---
	    
		log.info("end of activityCheckIn: user id:{}",getCurrentUserId());
		return new ThedalResponse<>(ThedalSuccess.VOLUNTEER_CHECKIN);
	}

	@Override
	public ThedalResponse<Void> activityCheckOut(Long electionId) {
		Long currentUserId = getCurrentUserId();
		log.info("inside activityCheckOut: user id:{}", currentUserId);
		Long accountId = requestDetails.getCurrentAccountId(); 
	    if (accountId == null) {
	        log.error("Account ID not found, unauthorized access.");
	        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
	    }
		validateElectionOwnership(electionId, accountId);
	
		ElectionEntity election = electionRepository.findById(electionId)
				.orElseThrow(() -> new ThedalException(ThedalError.ELECTION_NOT_FOUND, HttpStatus.NOT_FOUND));
	
		VolunteerEntity volunteer = volunteerRepo.findByElectionEntityAndUserEntity_Id(election,currentUserId)
				.orElseThrow(() -> new ThedalException(ThedalError.VOLUNTEER_NOT_FOUND, HttpStatus.NOT_FOUND));

		LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
		LocalDateTime endOfDay = startOfDay.toLocalDate().atTime(LocalTime.MAX);

		LocalDateTime checkOutTime = LocalDateTime.now().isAfter(endOfDay) ? endOfDay : LocalDateTime.now();

		VolunteerDailyActivityEntity existingVolunteerDailyActivity = volunteerDailyActivityRepository
				.findByVolunteerIdAndCheckInTimeBetween(volunteer.getId(), startOfDay, endOfDay)
				.orElseThrow(() -> new ThedalException(ThedalError.NOT_CHECKEDIN, HttpStatus.BAD_REQUEST));

		existingVolunteerDailyActivity.setCheckOutTime(checkOutTime);
		existingVolunteerDailyActivity.setHoursWorked(Duration.between(existingVolunteerDailyActivity.getCheckInTime(), checkOutTime));
		existingVolunteerDailyActivity.setChecked(false);
		VolunteerDailyActivityEntity savedActivity = volunteerDailyActivityRepository.save(existingVolunteerDailyActivity);
		
		// --- Dual-write to MongoDB ---
		try {
		    MongoVolunteerDailyActivity mongoActivity = mapToMongoVolunteerDailyActivity(savedActivity);
		    mongoVolunteerService.saveVolunteerDailyActivity(mongoActivity);
		    log.info("Successfully synced check-out activity to MongoDB for volunteer: {}", volunteer.getId());
		} catch (Exception e) {
		    log.error("Failed to sync check-out activity to MongoDB for volunteer: {}, error: {}", volunteer.getId(), e.getMessage());
		    // Continue with the operation even if MongoDB sync fails
		}
		// --- End dual-write ---
		
		log.info("end of activityCheckOut: user id:{}", currentUserId);
		return new ThedalResponse<>(ThedalSuccess.VOLUNTEER_CHECKOUT);
	}

	@Override
	public ThedalResponse<Void> activityTrack(VolunteerActivityTrackingDTO volunteerActivityTrackingDTO,Long electionId) {
		log.info("inside activityTrack: dto:{}", volunteerActivityTrackingDTO);
		Long accountId = requestDetails.getCurrentAccountId(); 
	    if (accountId == null) {
	        log.error("Account ID not found, unauthorized access.");
	        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
	    }
		validateElectionOwnership(electionId, accountId);
		ElectionEntity election = electionRepository.findById(electionId)
				.orElseThrow(() -> new ThedalException(ThedalError.ELECTION_NOT_FOUND, HttpStatus.NOT_FOUND));
		
		VolunteerEntity volunteer = volunteerRepo.findByElectionEntityAndUserEntity_Id(election,getCurrentUserId())
				.orElseThrow(() -> new ThedalException(ThedalError.VOLUNTEER_NOT_FOUND, HttpStatus.NOT_FOUND));

		LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
		LocalDateTime endOfDay = startOfDay.toLocalDate().atTime(LocalTime.MAX);

		VolunteerDailyActivityEntity existingVolunteerDailyActivity = volunteerDailyActivityRepository
				.findByVolunteerIdAndCheckInTimeBetweenAndIsChecked(volunteer.getId(), startOfDay, endOfDay,true)
				.orElseThrow(() -> new ThedalException(ThedalError.NOT_CHECKEDIN, HttpStatus.BAD_REQUEST));
		

		LocalDateTime localDateTime = LocalDateTime.now();
		VolunteerActivityLogsEntity volunteerActivityLogs = new VolunteerActivityLogsEntity();
		volunteerActivityLogs.setAccountId(accountService.getCurrentAccountFromRequest().getId());
		volunteerActivityLogs.setActivityDate(localDateTime.toLocalDate());
		volunteerActivityLogs.setCurrentTimeStamp(localDateTime);
		volunteerActivityLogs.setLatitude(volunteerActivityTrackingDTO.getLatitude());
		volunteerActivityLogs.setLongitude(volunteerActivityTrackingDTO.getLongitude());

		Optional<VolunteerActivityLogsEntity> lastActivityLogOptional = volunteerActivityLogsRepository
				.findTopByVolunteerDailyActivityEntityOrderByCurrentTimeStampDesc(existingVolunteerDailyActivity);
		if (lastActivityLogOptional.isPresent()) {
			log.info("activityTrack method:lastActivityLogOptional:true ");
			VolunteerActivityLogsEntity lastActivityLog = lastActivityLogOptional.get();
			double lastLatitude = lastActivityLog.getLatitude();
			double lastLongitude = lastActivityLog.getLongitude();

			// Calculate distance (implement your own method for this)
			BigDecimal distance = calculateDistance(lastLatitude, lastLongitude,
					volunteerActivityTrackingDTO.getLatitude(), volunteerActivityTrackingDTO.getLongitude());
			volunteerActivityLogs.setDistanceFromPreviousLocation(distance);
		} else {
			// First record, set distance as zero
			log.info("activityTrack method:lastActivityLogOptional:false ");
			volunteerActivityLogs.setDistanceFromPreviousLocation(BigDecimal.ZERO);
		}
	
		volunteerActivityLogs.setVolunteerDailyActivityEntity(existingVolunteerDailyActivity);
		volunteerActivityLogs.setVolunteerId(volunteer.getId());
		log.info("Final distanceFromPreviousLocation value before save: {}", volunteerActivityLogs.getDistanceFromPreviousLocation());

		VolunteerActivityLogsEntity savedActivityLog = volunteerActivityLogsRepository.save(volunteerActivityLogs);
		
		// --- Dual-write to MongoDB ---
		try {
		    MongoVolunteerActivityLog mongoActivityLog = mapToMongoVolunteerActivityLog(savedActivityLog);
		    mongoVolunteerService.saveVolunteerActivityLog(mongoActivityLog);
		    log.info("Successfully synced activity tracking log to MongoDB for volunteer: {}", volunteer.getId());
		} catch (Exception e) {
		    log.error("Failed to sync activity tracking log to MongoDB for volunteer: {}, error: {}", volunteer.getId(), e.getMessage());
		    // Continue with the operation even if MongoDB sync fails
		}
		// --- End dual-write ---
		
		log.info("inside activityTrack: dto:{}", volunteerActivityTrackingDTO);
		return new ThedalResponse<>(ThedalSuccess.ACTIVITY_TRACKED);
	}

	private BigDecimal calculateDistance(double lat1, double lon1, double lat2, double lon2) {
		log.info("inside calculateDistance");
		final double R = 6371000; // Radius of the Earth in meters

		double latDistance = Math.toRadians(lat2 - lat1);
		double lonDistance = Math.toRadians(lon2 - lon1);

		double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) + Math.cos(Math.toRadians(lat1))
				* Math.cos(Math.toRadians(lat2)) * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

		double distance = R * c; // Convert to meters
		log.info("end of calculateDistance: calculated distance:{}", distance);

		return BigDecimal.valueOf(distance).round(new MathContext(10)); // Return distance as BigDecimal with 10 digits						
	}

	@Override
	public ThedalResponse<Boolean> isCheckedIn(Long userId,Long electionId) {
		Long accountId = requestDetails.getCurrentAccountId(); 
	    if (accountId == null) {
	        log.error("Account ID not found, unauthorized access.");
	        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
	    }
		validateElectionOwnership(electionId, accountId);
		ElectionEntity election = electionRepository.findById(electionId)
				.orElseThrow(() -> new ThedalException(ThedalError.ELECTION_NOT_FOUND, HttpStatus.NOT_FOUND));
		Long user = userId == 0 ? getCurrentUserId() : userId;
		
		VolunteerEntity volunteer = volunteerRepo.findByElectionEntityAndUserEntity_Id(election,user)
				.orElseThrow(() -> new ThedalException(ThedalError.VOLUNTEER_NOT_FOUND, HttpStatus.NOT_FOUND));
		
		LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
		LocalDateTime endOfDay = startOfDay.toLocalDate().atTime(LocalTime.MAX);
		
		VolunteerDailyActivityEntity existingVolunteerDailyActivity = volunteerDailyActivityRepository
				.findByVolunteerIdAndCheckInTimeBetween(volunteer.getId(), startOfDay, endOfDay)
				.orElseThrow(() -> new ThedalException(ThedalError.NOT_CHECKEDIN, HttpStatus.BAD_REQUEST));
		return new ThedalResponse<>(ThedalSuccess.VOLUNTEER_CHECKED_IN_OR_OUT_INFO_FETCHED,existingVolunteerDailyActivity.isChecked());
	}
	
    public Long getCurrentUserId() {
    	return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

	@Override
	public ThedalResponse<Page<VolunteerActivityResponseDTO>> getVolunteerActivity(Long userId,Long electionId, LocalDate startDate,
			LocalDate endDate,int page,int size) {
		log.info("inside getVolunteerActivity:userId:{}",userId);
		Long accountId = requestDetails.getCurrentAccountId(); 
	    if (accountId == null) {
	        log.error("Account ID not found, unauthorized access.");
	        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
	    }
		validateElectionOwnership(electionId, accountId);
		ElectionEntity election = electionRepository.findById(electionId)
				.orElseThrow(() -> new ThedalException(ThedalError.ELECTION_NOT_FOUND, HttpStatus.NOT_FOUND));
		Long user = userId == 0 ? getCurrentUserId() : userId;
		
		VolunteerEntity volunteer = volunteerRepo.findByElectionEntityAndUserEntity_Id(election,user)
				.orElseThrow(() -> new ThedalException(ThedalError.VOLUNTEER_NOT_FOUND, HttpStatus.NOT_FOUND));

		List<Long> assignedBooths = volunteer.getAssignedBooth() != null && !volunteer.getAssignedBooth().isEmpty()
	            ? volunteer.getAssignedBooth()  // Directly use List<Long>
	            : Collections.emptyList();

		
		Pageable pageable = PageRequest.of(page, size);
		Page<VolunteerActivityResponseDTO> activityResponses = volunteerActivityLogsRepository
	            .findByVolunteerIdAndActivityDateBetweenOrderByCurrentTimeStampDesc(volunteer.getId(), startDate, endDate,pageable)
	            .map(activity -> new VolunteerActivityResponseDTO(
	                    activity.getId(),
	                    assignedBooths,
	                    activity.getLatitude(),
	                    activity.getLongitude(),
	                    activity.getActivityDate(),
	                    activity.getCurrentTimeStamp(),
	                    activity.getDistanceFromPreviousLocation()
	            ));
		
		log.info("end of getVolunteerActivity:userId:{}",userId);
		return new ThedalResponse<>(ThedalSuccess.ACTIVITY_TRACKED,activityResponses);

	}

	@Override
	@Transactional(rollbackFor = { Exception.class })
	public ThedalResponse<Void> addVolunteerToElection(SaveVolunteerDetailsDTO volunteerDetailsDto,
			Long electionId) {
		log.info("inside addVolunteerToElection:electionId:{},mobileNumber:{}", electionId,
				volunteerDetailsDto.getMobileNumber());

		Long currentUserAccountId = requestDetails.getCurrentAccountId();
		if (currentUserAccountId == null) {
			log.error("Account id not found, unauthorized access.");
			throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
		}
		
		validateElectionOwnership(electionId, currentUserAccountId);
		ElectionEntity election = electionRepository.findById(electionId)
				.orElseThrow(() -> new ThedalException(ThedalError.ELECTION_NOT_FOUND, HttpStatus.NOT_FOUND));
		Optional<UserEntity> optionalUser = userRepo.findByMobileNumber(volunteerDetailsDto.getMobileNumber());
		if (optionalUser.isPresent()) {
			UserEntity userEntity = optionalUser.get();
			log.info("inside addVolunteerToElection: User Already Present :electionId:{},mobileNumber:{}", electionId,
					volunteerDetailsDto.getMobileNumber());
			
			if(userEntity.getAccountEntity().getId() != currentUserAccountId)
				throw new ThedalException(ThedalError.ACCESS_DENIED, HttpStatus.BAD_REQUEST);
			
			if (volunteerRepo.existsByUserEntityAndElectionEntity(userEntity, election))
				throw new ThedalException(ThedalError.VOLUNTEER_ALREADY_ADDED, HttpStatus.BAD_REQUEST);
			
			addVolunteer(volunteerDetailsDto, election, currentUserAccountId, userEntity);

		} else {

			log.info("inside addVolunteerToElection: User Not Present :electionId:{},mobileNumber:{}", electionId,
					volunteerDetailsDto.getMobileNumber());

			log.info("Attempting to save volunteer by current account ID: {}", currentUserAccountId);

			Role userRole = roleRepo.findById(VOLUNTEER).orElse(null);
			if (userRole == null)
				throw new ThedalException(ThedalError.ROLE_NOT_FOUND, HttpStatus.BAD_REQUEST);

			if (userRepo.existsByMobileNumber(volunteerDetailsDto.getMobileNumber()))
				throw new ThedalException(ThedalError.DUPLICATE_MOBILE_NUMBER, HttpStatus.BAD_REQUEST);

			UserEntity user = new UserEntity();

			user.setRole(userRole);
			user.setFirstName(volunteerDetailsDto.getFirstName());
			user.setLastName(volunteerDetailsDto.getLastName());
			user.setEmail(volunteerDetailsDto.getEmail());
			user.setMobileNumber(volunteerDetailsDto.getMobileNumber());
			// hashing the password using bcrypt
			user.setPassword(new BCryptPasswordEncoder().encode(volunteerDetailsDto.getPassword()));

			user.setIsEmailVerified(true);
			user.setIsMobileVerified(true);
			user.setIsActive(true);
			user.setCreatedAt(LocalDateTime.now());
			user.setCreatedBy("volunteer create");
		user.setAccountEntity(accountRepository.findById(currentUserAccountId).orElseThrow(
				() -> new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.BAD_REQUEST)));
		log.info("Saving user object to DB: {}", user.toString());
		
		// Save to PostgreSQL and MongoDB with dual-write pattern
		UserEntity userEntity;
		try {
			userEntity = userRepo.save(user);
			try {
				MongoUser mongoUser = new MongoUser(userEntity);
				mongoUserRepository.save(mongoUser);
				log.info("Successfully created user in MongoDB: id={}, name={} {}", userEntity.getId(), userEntity.getFirstName(), userEntity.getLastName());
			} catch (Exception mongoEx) {
				log.error("Failed to create user in MongoDB: id={}, name={} {}", userEntity.getId(), userEntity.getFirstName(), userEntity.getLastName(), mongoEx);
				throw new RuntimeException("MongoDB save failed, triggering rollback", mongoEx);
			}
		} catch (Exception ex) {
			log.error("Failed to create user for volunteer", ex);
			throw new RuntimeException("User creation failed", ex);
		}

		if (volunteerRepo.existsByUserEntityAndElectionEntity(userEntity, election))
			throw new ThedalException(ThedalError.VOLUNTEER_ALREADY_ADDED, HttpStatus.BAD_REQUEST);

			addVolunteer(volunteerDetailsDto, election, currentUserAccountId, userEntity);

		}

		log.info("inside addVolunteerToElection:electionId:{},mobileNumber:{}", electionId,
				volunteerDetailsDto.getMobileNumber());
		return new ThedalResponse<>(ThedalSuccess.VOLUNTEER_ADDED);
	}

	private void addVolunteer(SaveVolunteerDetailsDTO volunteerDetailsDto, ElectionEntity election,
			Long currentUserAccountId, UserEntity userEntity) {
		VolunteerEntity volunteer = new VolunteerEntity();

		Long accountId = requestDetails.getCurrentAccountId();
		if (accountId  == null) {
			log.error("Account id not found, unauthorized access.");
			throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
		}
		Long electionId = volunteerDetailsDto.getElectionId();

		validateElectionOwnership(electionId, accountId);
		Address volunteerAddress = new Address();
		volunteerAddress.setStreet(volunteerDetailsDto.getAddress().getStreet());
		volunteerAddress.setCity(volunteerDetailsDto.getAddress().getCity());
		volunteerAddress.setState(volunteerDetailsDto.getAddress().getState());
		volunteerAddress.setPostalCode(volunteerDetailsDto.getAddress().getPostalCode());
		volunteerAddress.setCountry(volunteerDetailsDto.getAddress().getCountry());
		volunteer.setVolunteerAddress(volunteerAddress);

		volunteer.setAssignedBooth(volunteerDetailsDto.getAssignedBooth());
		volunteer.setAssignedFamilies(volunteerDetailsDto.getAssignedFamilies());
		volunteer.setStatus(volunteerDetailsDto.getStatus());
		
		// Automatically manage user activation based on volunteer status during creation
		if ("inactive".equalsIgnoreCase(volunteerDetailsDto.getStatus().trim())) {
		    log.info("Setting user {} as inactive because volunteer is being created with inactive status", userEntity.getId());
		    userEntity.setIsActive(false);
		    userRepo.save(userEntity); // Save the user activation change
		}
		volunteer.setRemarks(volunteerDetailsDto.getRemarks());
		volunteer.setAccountId(currentUserAccountId);
		volunteer.setUserEntity(userEntity);
		volunteer.setElectionEntity(election);
		
		VolunteerEntity volunteerEntity = volunteerRepo.save(volunteer);
		log.info("Volunteer with ID: {} successfully saved", volunteerEntity.getId());
				
		 // New code to handle assigned booths
	    if (volunteerDetailsDto.getAssignedBooth() != null && !volunteerDetailsDto.getAssignedBooth().isEmpty()) {
	        for (Long boothNumber : volunteerDetailsDto.getAssignedBooth()) {
	            ElectionBooth booth = electionBoothRepository.findByBoothNumberAndElectionId(boothNumber, election.getId())
	                    .orElseThrow(() -> new ThedalException(ThedalError.BOOTH_NOT_FOUND, HttpStatus.NOT_FOUND));

	            VolunteerElectionBooth volunteerBooth = new VolunteerElectionBooth();
	            volunteerBooth.setVolunteerId(volunteerEntity.getId());
	            volunteerBooth.setBoothNumber(booth.getBoothNumber());
	            volunteerBooth.setElectionId(election.getId());
	            volunteerBooth.setAccountId(currentUserAccountId);

	            volunteerElectionBoothRepo.save(volunteerBooth);
	        }
	    }
		
	}

	@Override
	public ThedalResponse<List<VolunteerDetailsDTO>> getVolunteerByAssignedBoothsAndMobileNumber(
	        Long electionId, List<Long> assignedBooths, String mobileNumber, Long userId) {

	    log.info("Inside getVolunteerByAssignedBoothsAndMobileNumber, electionId: {}, mobileNumber: {}, userId: {}, assignedBooths: {}",
	             electionId, mobileNumber, userId, assignedBooths);

	    Long accountId = requestDetails.getCurrentAccountId();
	    if (accountId == null) {
	        log.error("Account id not found, unauthorized access.");
	        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
	    }
		 
		validateElectionOwnership(electionId, accountId);
	    // Fetch volunteers matching the criteria
	    List<VolunteerEntity> volunteers = volunteerRepo.findVolunteerByElectionIdAndAssignedBoothsAndMobileNumberAndUserId(
	        electionId, assignedBooths, mobileNumber, userId);

	    if (volunteers.isEmpty()) {
	        log.error("No volunteers found with the provided parameters.");
	        throw new ThedalException(ThedalError.VOLUNTEER_NOT_FOUND, HttpStatus.NOT_FOUND);
	    }

	    List<VolunteerDetailsDTO> volunteerDetailsDTOList = volunteers.stream()
	        .map(volunteer -> mapToVolunteerDetailsDTO(volunteer, accountId))
	        .collect(Collectors.toList());

	    log.info("Successfully fetched active volunteers for electionId: {}, mobileNumber: {}, userId: {}, assignedBooths: {}",
	             electionId, mobileNumber, userId, assignedBooths);

	    return new ThedalResponse<>(ThedalSuccess.VOLUNTEER_FOUND, volunteerDetailsDTOList);
	}

    @Override
    public ThedalResponse<Page<VolunteerDetailsDTO>> getVolunteerPageByAssignedBoothsAndMobileNumber(Long electionId,
        List<Long> assignedBooths, String mobileNumber, Long userId, String roleName, int page, int size, String sortBy, String direction) {
    log.info("Paginated volunteer fetch electionId: {}, mobileNumber: {}, userId: {}, assignedBooths: {}, roleName: {}, page: {}, size: {}, sortBy: {}, direction: {}",
        electionId, mobileNumber, userId, assignedBooths, roleName, page, size, sortBy, direction);
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }
        validateElectionOwnership(electionId, accountId);
        if (page < 0) page = 0;
        if (size <= 0 || size > 200) size = 50; // sane limits
        // Map client sort fields to entity attributes (allow only whitelisted)
        String normalizedSort = (sortBy == null || sortBy.isBlank()) ? "firstName" : sortBy;
        String sortProperty;
        switch (normalizedSort.toLowerCase()) {
        case "firstname":
            sortProperty = "userEntity.firstName";
            break;
        case "lastname":
            sortProperty = "userEntity.lastName";
            break;
        case "mobilenumber":
            sortProperty = "userEntity.mobileNumber";
            break;
        case "status":
            sortProperty = "status";
            break;
        default:
            sortProperty = "userEntity.firstName";
        }
        org.springframework.data.domain.Sort sort = "desc".equalsIgnoreCase(direction)
                ? org.springframework.data.domain.Sort.by(sortProperty).descending()
                : org.springframework.data.domain.Sort.by(sortProperty).ascending();
        Pageable pageable = PageRequest.of(page, size, sort.and(org.springframework.data.domain.Sort.by("userEntity.lastName").ascending()));
    Page<VolunteerEntity> volunteerPage;
    if (roleName != null && !roleName.isEmpty()) {
        volunteerPage = volunteerRepo.findVolunteerPageByElectionIdAndAssignedBoothsAndMobileNumberAndUserIdAndRoleName(
            electionId, assignedBooths, mobileNumber, userId, roleName, pageable);
    } else {
        volunteerPage = volunteerRepo.findVolunteerPageByElectionIdAndAssignedBoothsAndMobileNumberAndUserId(
            electionId, assignedBooths, mobileNumber, userId, pageable);
    }
        Page<VolunteerDetailsDTO> dtoPage = volunteerPage.map(v -> mapToVolunteerDetailsDTO(v, accountId));
        return new ThedalResponse<>(ThedalSuccess.VOLUNTEER_FOUND, dtoPage);
    }

    @Override
    public ThedalResponse<Page<VolunteerDetailsDTO>> getVolunteerPageByAssignedBoothsAndMobileNumberWithSearch(Long electionId,
        List<Long> assignedBooths, String mobileNumber, Long userId, String roleName, String searchTerm, int page, int size, String sortBy, String direction) {
    log.info("Paginated volunteer fetch with search electionId: {}, mobileNumber: {}, userId: {}, assignedBooths: {}, roleName: {}, searchTerm: {}, page: {}, size: {}, sortBy: {}, direction: {}",
        electionId, mobileNumber, userId, assignedBooths, roleName, searchTerm, page, size, sortBy, direction);
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }
        validateElectionOwnership(electionId, accountId);
        if (page < 0) page = 0;
        if (size <= 0 || size > 200) size = 50; // sane limits
        // Map client sort fields to entity attributes (allow only whitelisted)
        String normalizedSort = (sortBy == null || sortBy.isBlank()) ? "firstName" : sortBy;
        String sortProperty;
        switch (normalizedSort.toLowerCase()) {
        case "firstname":
            sortProperty = "userEntity.firstName";
            break;
        case "lastname":
            sortProperty = "userEntity.lastName";
            break;
        case "mobilenumber":
            sortProperty = "userEntity.mobileNumber";
            break;
        case "status":
            sortProperty = "status";
            break;
        default:
            sortProperty = "userEntity.firstName";
        }
        org.springframework.data.domain.Sort sort = "desc".equalsIgnoreCase(direction)
                ? org.springframework.data.domain.Sort.by(sortProperty).descending()
                : org.springframework.data.domain.Sort.by(sortProperty).ascending();
        Pageable pageable = PageRequest.of(page, size, sort.and(org.springframework.data.domain.Sort.by("userEntity.lastName").ascending()));
        Page<VolunteerEntity> volunteerPage;
        try {
            if (roleName != null && !roleName.isEmpty()) {
                volunteerPage = volunteerRepo.findVolunteerPageByElectionIdAndAssignedBoothsAndMobileNumberAndUserIdAndRoleNameWithSearch(
                        electionId, assignedBooths, mobileNumber, userId, roleName, searchTerm, pageable);
            } else {
                volunteerPage = volunteerRepo.findVolunteerPageByElectionIdAndAssignedBoothsAndMobileNumberAndUserId(
                        electionId, assignedBooths, mobileNumber, userId, searchTerm, pageable);
            }
        } catch (Exception ex) {
            String msg = ex.getMessage();
            if (msg != null && (msg.toLowerCase().contains("upper(bytea)") || msg.toLowerCase().contains("character varying ~~ bytea"))) {
                log.warn("VOLUNTEER_GET: Falling back to mobile-only search due to bytea error");
                try {
                    volunteerPage = volunteerRepo.findVolunteerPageByElectionIdAndAssignedBoothsAndMobileNumberAndUserIdMobileOnly(
                            electionId, assignedBooths, mobileNumber, userId, searchTerm, pageable);
                } catch (Exception ex2) {
                    String msg2 = ex2.getMessage();
                    if (msg2 != null && msg2.toLowerCase().contains("character varying ~~ bytea")) {
                        log.warn("VOLUNTEER_GET: Mobile-only also failed with bytea error, using completely bytea-safe query");
                        try {
                            volunteerPage = volunteerRepo.findVolunteerPageByElectionIdAndAssignedBoothsAndMobileNumberAndUserIdByteasafe(
                                    electionId, assignedBooths, mobileNumber, userId, searchTerm, pageable);
                        } catch (Exception ex3) {
                            String msg3 = ex3.getMessage();
                            if (msg3 != null && msg3.toLowerCase().contains("character varying ~~ bytea")) {
                                log.warn("VOLUNTEER_GET: Even simple query failed - using most basic query (election ID only)");
                                volunteerPage = volunteerRepo.findBasicVolunteersByElectionId(electionId, pageable);
                            } else {
                                throw ex3;
                            }
                        }
                    } else {
                        throw ex2;
                    }
                }
            } else {
                throw ex;
            }
        }
        Page<VolunteerDetailsDTO> dtoPage = volunteerPage.map(v -> mapToVolunteerDetailsDTO(v, accountId));
        return new ThedalResponse<>(ThedalSuccess.VOLUNTEER_FOUND, dtoPage);
    }

//@Override
//@Transactional
//public ThedalResponse<Void> updateAssignedBooths(Long electionId, Long userId, BoothUpdateRequest boothUpdateRequest) {
//    log.info("Searching for volunteer with electionId: {}, userId: {}", electionId, userId);
//	Long accountId = requestDetails.getCurrentAccountId();
//	if (accountId == null) {
//		log.error("Account ID not found, unauthorized access.");
//		throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
//	}
//	
//	validateElectionOwnership(electionId, accountId);  
//
//    // Retrieve volunteer based on electionId and userId
//    Optional<VolunteerEntity> optionalVolunteer = volunteerRepo.findByUserEntityIdAndElectionEntityId(userId, electionId);
//    
//    if (optionalVolunteer.isEmpty()) {
//        log.error("Volunteer not found for electionId: {}, userId: {}", electionId, userId);
//        throw new ThedalException(ThedalError.VOLUNTEER_NOT_FOUND, HttpStatus.NOT_FOUND);
//    }
//
//    VolunteerEntity volunteerEntity = optionalVolunteer.get();
//
//    // Replace the assigned booths with new ones
//    volunteerEntity.getAssignedBooth().clear(); // Clear existing booths
//
//    // Validate booth numbers and keep them as Integer
//    List<Integer> newBoothNumbers = boothUpdateRequest.getBooths().stream()
//        .map(boothNumber -> {
//            // Find booth by booth number and electionId
//            return electionBoothRepository.findByBoothNumberAndElectionId(boothNumber, electionId)
//                .map(booth -> booth.getBoothNumber())  // Directly use Integer booth number
//                .orElseThrow(() -> {
//                    log.error("Booth not found with boothNumber: {} for electionId: {}", boothNumber, electionId);
//                    return new ThedalException(ThedalError.BOOTH_NOT_FOUND, HttpStatus.NOT_FOUND);
//                });
//        })
//        .collect(Collectors.toList());
//
//    // Save new booth assignments in volunteer_election_booth table
//    volunteerElectionBoothRepo.deleteByVolunteerId(volunteerEntity.getId()); // Clear existing mappings
//
//    for (Integer boothNumber : newBoothNumbers) {
//        VolunteerElectionBooth volunteerElectionBooth = new VolunteerElectionBooth();
//        volunteerElectionBooth.setVolunteerId(volunteerEntity.getId());
//        volunteerElectionBooth.setBoothNumber(boothNumber);
//        volunteerElectionBooth.setAccountId(volunteerEntity.getAccountId());
//        volunteerElectionBooth.setElectionId(electionId);
//        
//        volunteerElectionBoothRepo.save(volunteerElectionBooth);
//    }
//
//    // Convert List<Integer> to List<Long> and set new assigned booths
//    List<Long> newBoothNumbersAsLong = newBoothNumbers.stream()
//        .map(Integer::longValue)
//        .collect(Collectors.toList());
//
//    volunteerEntity.setAssignedBooth(newBoothNumbersAsLong); // Replace with new booths
//
//    log.info("Assigned booths fully updated for electionId: {}, userId: {}", electionId, userId);
//    return new ThedalResponse<>(ThedalSuccess.BOOTHS_UPDATED);
//}
	
	@Override
    @Transactional(rollbackFor = {Exception.class})
    public ThedalResponse<Void> updateAssignedBooths(Long electionId, Long userId, BoothUpdateRequest boothUpdateRequest) {
        log.info("Updating assigned booths for electionId: {}, userId: {}", electionId, userId);
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account ID not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        validateElectionOwnership(electionId, accountId);

        // Retrieve volunteer based on electionId and userId
        Optional<VolunteerEntity> optionalVolunteer = volunteerRepo.findByUserEntityIdAndElectionEntityId(userId, electionId);
        if (optionalVolunteer.isEmpty()) {
            log.error("Volunteer not found for electionId: {}, userId: {}", electionId, userId);
            throw new ThedalException(ThedalError.VOLUNTEER_NOT_FOUND, HttpStatus.NOT_FOUND);
        }

        VolunteerEntity volunteerEntity = optionalVolunteer.get();

        // Retrieve or create VolunteerVsVoterReportEntity
        VolunteerVsVoterReportEntity report = volunteerVsVoterReportRepository
                .findByElectionIdAndVolunteerId(electionId, volunteerEntity.getId())
                .orElseGet(() -> {
                    VolunteerVsVoterReportEntity newReport = new VolunteerVsVoterReportEntity();
                    newReport.setElectionId(electionId);
                    newReport.setVolunteerId(volunteerEntity.getId());
                    newReport.setAccountId(accountId);
                    return newReport;
                });

        // Log input booth numbers
        log.info("Input booth numbers: {}", boothUpdateRequest.getBooths());

//        // Validate booth numbers and keep them as Integer
//        List<Integer> newBoothNumbers = boothUpdateRequest.getBooths().stream()
//                .map(boothNumber -> {
//                    return electionBoothRepository.findByBoothNumberAndElectionId(boothNumber, electionId)
//                            .map(booth -> booth.getBoothNumber())
//                            .orElseThrow(() -> {
//                                log.error("Booth not found with boothNumber: {} for electionId: {}", boothNumber, electionId);
//                                return new ThedalException(ThedalError.BOOTH_NOT_FOUND, HttpStatus.NOT_FOUND);
//                            });
//                })
//                .collect(Collectors.toList());
        // Validate booth numbers and keep them as Integer
        List<Integer> newBoothNumbers = boothUpdateRequest.getBooths().stream()
                .map(boothNumber -> {
                    // Check if booth exists in part_manager table using booth partNo
                    Optional<PartManager> partManagerOpt = partManagerRepository.findByPartNoAndElectionIdAndAccountId(
                        String.valueOf(boothNumber), electionId, accountId);
                    
                    if (!partManagerOpt.isPresent()) {
                        log.error("Booth not found with partNo: {} in part_manager table for electionId: {} and accountId: {}", 
                            boothNumber, electionId, accountId);
                        throw new ThedalException(ThedalError.BOOTH_NOT_FOUND, HttpStatus.NOT_FOUND);
                    }
                    
                    return boothNumber.intValue(); // Convert Long to Integer
                })
                .collect(Collectors.toList());

        // Convert List<Integer> to List<Long> for comparison and storage
        List<Long> newBoothNumbersAsLong = newBoothNumbers.stream()
                .map(Integer::longValue)
                .collect(Collectors.toList());

        // Check if booth assignments have changed
        if (!newBoothNumbersAsLong.equals(volunteerEntity.getAssignedBooth())) {
            log.info("Booth assignments changed from {} to {} for volunteerId: {}", 
                    volunteerEntity.getAssignedBooth(), newBoothNumbersAsLong, volunteerEntity.getId());

            // Clear existing booths
            volunteerEntity.getAssignedBooth().clear();
            volunteerEntity.setAssignedBooth(newBoothNumbersAsLong);

            // Update VolunteerElectionBooth records
            volunteerElectionBoothRepo.deleteByVolunteerIdAndElectionId(volunteerEntity.getId(), electionId);
            for (Integer boothNumber : newBoothNumbers) {
                VolunteerElectionBooth volunteerElectionBooth = new VolunteerElectionBooth();
                volunteerElectionBooth.setVolunteerId(volunteerEntity.getId());
                volunteerElectionBooth.setBoothNumber(boothNumber);
                volunteerElectionBooth.setAccountId(accountId);
                volunteerElectionBooth.setElectionId(electionId);
                volunteerElectionBoothRepo.save(volunteerElectionBooth);
            }

            // Increment totalBoothsUpdated if new booths are non-empty
            if (!newBoothNumbersAsLong.isEmpty()) {
                log.info("Incrementing totalBoothsUpdated for volunteerId: {}", volunteerEntity.getId());
                report.setTotalBoothsUpdated((report.getTotalBoothsUpdated() != null ? report.getTotalBoothsUpdated() : 0L) + 1L);
            }
        } else {
            log.info("No change in booth assignments for volunteerId: {}", volunteerEntity.getId());
        }

        // Save updated entities
        log.info("Saving updated VolunteerEntity for volunteerId: {}", volunteerEntity.getId());
        volunteerRepo.save(volunteerEntity);
        log.info("Saving updated VolunteerVsVoterReportEntity: BoothsUpdated: {}", report.getTotalBoothsUpdated());
        volunteerVsVoterReportRepository.save(report);

        // Sync with MongoDB
        Optional<MongoVolunteer> mongoOpt = mongoVolunteerRepository.findAll().stream()
                .filter(v -> v.getUserEntity() != null && v.getUserEntity().getId().equals(userId)
                        && v.getElectionEntity() != null && v.getElectionEntity().getId().equals(electionId))
                .findFirst();
        MongoVolunteer mongoVolunteer = mongoOpt.orElseGet(() -> new MongoVolunteer());
        mapToMongoVolunteer(volunteerEntity, mongoVolunteer);
        log.info("Saving updated MongoVolunteer for volunteerId: {}", volunteerEntity.getId());
        mongoVolunteerRepository.save(mongoVolunteer);

        log.info("Assigned booths fully updated for electionId: {}, userId: {}", electionId, userId);
        return new ThedalResponse<>(ThedalSuccess.BOOTHS_UPDATED);
    }

@Override
@Transactional(rollbackFor = {Exception.class})
public ThedalResponse<Void> deleteVolunteerFromElection(Long volunteerId, Long electionId) {
    requestDetails.checkUserRolePermission(RolePermission.CADRE_MANAGEMENT);
    
    Long currentUserAccountId = requestDetails.getCurrentAccountId();
    if (currentUserAccountId == null) {
        log.error("Account id not found, unauthorized access.");
        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
    }
	
	validateElectionOwnership(electionId, currentUserAccountId);  

    VolunteerEntity volunteer = volunteerRepo.findById(volunteerId)
        .orElseThrow(() -> new ThedalException(ThedalError.VOLUNTEER_NOT_FOUND, HttpStatus.NOT_FOUND));

    if (!volunteer.getElectionEntity().getId().equals(electionId)) {
        throw new ThedalException(ThedalError.VOLUNTEER_NOT_IN_ELECTION, HttpStatus.BAD_REQUEST);
    }

    // Delete volunteer's booth assignments first
    volunteerElectionBoothRepo.deleteByVolunteerIdAndElectionId(volunteerId, electionId);

    // Delete the volunteer entity
    volunteerRepo.delete(volunteer);

    // --- Dual-delete from MongoDB ---
    mongoVolunteerRepository.findAll().stream()
        .filter(v -> v.getId().equals(volunteerId.toString()))
        .findFirst()
        .ifPresent(mv -> mongoVolunteerRepository.deleteById(mv.getId()));
    // --- End dual-delete ---

    return new ThedalResponse<>(ThedalSuccess.VOLUNTEER_DELETED);
}

   
@Transactional(rollbackFor = {Exception.class})
public ThedalResponse<Void> deleteVolunteers(Long electionId, List<Long> userIds) {
    //requestDetails.checkUserRolePermission(RolePermission.CADRE_MANAGEMENT);
	requestDetails.checkUserRolePermission("cadreList", "D");
    
    Long accountId = requestDetails.getCurrentAccountId();
    if (accountId == null) {
        log.error("Account id not found, unauthorized access.");
        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
    }

    try {
        int deletedCount;
        validateElectionOwnership(electionId, accountId);

        if (userIds.isEmpty()) {
            log.info("Deleting all volunteers for electionId: {}, accountId: {}", electionId, accountId);
            // Delete all volunteers from relational DB
            deletedCount = volunteerRepo.deleteByAccountIdAndElectionEntityId(accountId, electionId);
            
            // Dual-delete from MongoDB
            mongoVolunteerRepository.findAll().stream()
                .filter(v -> v.getElectionEntity() != null && v.getElectionEntity().getId().equals(electionId)
                    && v.getAccountId().equals(accountId))
                .forEach(mv -> mongoVolunteerRepository.deleteById(mv.getId()));
            
            log.info("Successfully deleted all volunteers for accountId: {}, electionId: {}", accountId, electionId);
        } else {
            log.info("Deleting specific volunteers for electionId: {}, accountId: {} with userIds: {}", 
                    electionId, accountId, userIds);
            // Delete specific volunteers from relational DB
            deletedCount = volunteerRepo.deleteByAccountIdAndElectionEntityIdAndUserEntityIdIn(accountId, electionId, userIds);
            
            // Dual-delete from MongoDB
            mongoVolunteerRepository.findAll().stream()
                .filter(v -> v.getUserEntity() != null && userIds.contains(v.getUserEntity().getId())
                    && v.getElectionEntity() != null && v.getElectionEntity().getId().equals(electionId)
                    && v.getAccountId().equals(accountId))
                .forEach(mv -> mongoVolunteerRepository.deleteById(mv.getId()));
            
            // Delete associated users
            userIds.forEach(userId -> {
                userRepo.findById(userId).ifPresent(userRepo::delete);
                mongoUserRepository.findById(userId.toString()).ifPresent(mongoUserRepository::delete);
            });
            
            log.info("Successfully deleted volunteers: userIds={}", userIds);
        }

        if (deletedCount == 0) {
            log.warn("No volunteers found to delete for electionId: {}, userIds: {}", electionId, userIds);
            throw new ThedalException(ThedalError.VOLUNTEER_NOT_FOUND, HttpStatus.NOT_FOUND);
        }

        return new ThedalResponse<>(ThedalSuccess.VOLUNTEER_DELETED);
    } catch (Exception ex) {
        log.error("Failed to delete volunteers: electionId={}, userIds={}", electionId, userIds, ex);
        throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}


   public VolunteerExportResponse initiateVolunteerExport(Long accountId, Long electionId,
        List<Long> assignedBooths, String gender, String status, Integer limit) {
      
      if (!volunteerRepo.existsByElectionIdAndAccountId(electionId, accountId)) {
          throw new ThedalException(ThedalError.ELECTION_NOT_FOUND, HttpStatus.NOT_FOUND);
      }

      VolunteerDownloadJob job = new VolunteerDownloadJob();
      job.setAccountId(accountId);
      job.setElectionId(electionId);
      job.setTimeStarted(LocalDateTime.now());
      job.setStatus("IN_PROGRESS");
      volunteerDownloadJobRepository.saveAndFlush(job);
      
      try {
          processVolunteerExportAsync(job.getId(), accountId, electionId, assignedBooths, gender, status, limit);
      } catch (Exception e) {
          log.error("Failed to initiate volunteer export for jobId: {}, error: {}", job.getId(), e.getMessage(), e);
          
          job.setStatus("ERROR");
          job.setErrorMessage("Failed to initiate export: " + e.getMessage());
          job.setTimeCompleted(LocalDateTime.now());
          volunteerDownloadJobRepository.save(job);
          
          throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, 
                  "Failed to initiate export job: " + e.getMessage());
      }
      
      return new VolunteerExportResponse(job.getId());
  }

	// New comprehensive export method with all filters from GET volunteers API
	public VolunteerExportResponse initiateVolunteerExportWithFilters(Long accountId, Long electionId,
			String mobileNumber, List<Long> assignedBooths, Long userId, String searchTerm,
			String gender, String status, Integer limit) {
		
		log.info("Initiating comprehensive volunteer export for accountId: {}, electionId: {}, with filters", accountId, electionId);
		
		if (!volunteerRepo.existsByElectionIdAndAccountId(electionId, accountId)) {
			log.error("Election not found for accountId: {}, electionId: {}", accountId, electionId);
			throw new ThedalException(ThedalError.ELECTION_NOT_FOUND, HttpStatus.NOT_FOUND);
		}

		VolunteerDownloadJob job = new VolunteerDownloadJob();
		job.setAccountId(accountId);
		job.setElectionId(electionId);
		job.setTimeStarted(LocalDateTime.now());
		job.setStatus("IN_PROGRESS");
		volunteerDownloadJobRepository.saveAndFlush(job);

		try {
			// Start async processing with comprehensive filters
			processVolunteerExportWithFiltersAsync(job.getId(), accountId, electionId, 
				mobileNumber, assignedBooths, userId, searchTerm, gender, status, limit);
		} catch (Exception e) {
			log.error("Failed to initiate comprehensive export for jobId: {}, error: {}", job.getId(), e.getMessage(), e);
			
			// Update job status to ERROR if scheduling fails
			job.setStatus("ERROR");
			job.setErrorMessage("Failed to initiate export: " + e.getMessage());
			job.setTimeCompleted(LocalDateTime.now());
			volunteerDownloadJobRepository.save(job);
			
			throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, 
					"Failed to initiate export job: " + e.getMessage());
		}

		return new VolunteerExportResponse(job.getId());
	}
   
   @Async
   public void processVolunteerExportAsync(Long jobId, Long accountId, Long electionId,
                                           List<Long> assignedBooths, String gender, String status, Integer limit) {
       
       log.info("ASYNC_EXPORT: Starting async export for jobId: {}, accountId: {}, electionId: {}", jobId, accountId, electionId);
       
       try {
           processVolunteerExport(jobId, accountId, electionId, assignedBooths, gender, status, limit);
           
           log.info("ASYNC_EXPORT: Successfully completed async export for jobId: {}", jobId);
       } catch (Exception e) {
           log.error("ASYNC_EXPORT: Error in async export for jobId: {}, error: {}", jobId, e.getMessage(), e);
           
           try {
               VolunteerDownloadJob job = volunteerDownloadJobRepository.findById(jobId).orElse(null);
               if (job != null) {
                   job.setStatus("ERROR");
                   job.setErrorMessage("Async execution failed: " + e.getMessage());
                   job.setTimeCompleted(LocalDateTime.now());
                   volunteerDownloadJobRepository.save(job);
                   
                   log.info("ASYNC_EXPORT: Updated job status to ERROR for jobId: {}", jobId);
               } else {
                   log.error("ASYNC_EXPORT: Could not find job to update error status for jobId: {}", jobId);
               }
           } catch (Exception updateException) {
               log.error("ASYNC_EXPORT: Failed to update job status to ERROR for jobId: {}, updateError: {}", 
                       jobId, updateException.getMessage(), updateException);
           }
       }
   }

	// New comprehensive async method for exports with all filters
	@Async
	public void processVolunteerExportWithFiltersAsync(Long jobId, Long accountId, Long electionId,
			String mobileNumber, List<Long> assignedBooths, Long userId, String searchTerm,
			String gender, String status, Integer limit) {
		
		log.info("ASYNC_EXPORT: Starting comprehensive async export for jobId: {}, accountId: {}, electionId: {}", jobId, accountId, electionId);
		
		try {
			// Call the comprehensive export method
			processVolunteerExportWithFilters(jobId, accountId, electionId, 
				mobileNumber, assignedBooths, userId, searchTerm, gender, status, limit);
			
			log.info("ASYNC_EXPORT: Successfully completed comprehensive async export for jobId: {}", jobId);
		} catch (Exception e) {
			log.error("ASYNC_EXPORT: Error in comprehensive async export for jobId: {}, error: {}", jobId, e.getMessage(), e);
			
			// Update job status to ERROR
			try {
				VolunteerDownloadJob job = volunteerDownloadJobRepository.findById(jobId)
						.orElse(null);
				if (job != null) {
					job.setStatus("ERROR");
					job.setErrorMessage("Async execution failed: " + e.getMessage());
					job.setTimeCompleted(LocalDateTime.now());
					volunteerDownloadJobRepository.save(job);
					
					log.info("ASYNC_EXPORT: Updated job status to ERROR for comprehensive export jobId: {}", jobId);
				} else {
					log.error("ASYNC_EXPORT: Could not find job to update error status for comprehensive export jobId: {}", jobId);
				}
			} catch (Exception updateException) {
				log.error("ASYNC_EXPORT: Failed to update job status to ERROR for comprehensive export jobId: {}, updateError: {}", 
						jobId, updateException.getMessage(), updateException);
			}
		}
	}
   
   @Transactional
   public void processVolunteerExport(Long jobId, Long accountId, Long electionId,
                                      List<Long> assignedBooths, String gender, String status, Integer limit) {
       
       log.info("Starting processVolunteerExport for jobId: {}, accountId: {}, electionId: {}, assignedBooths: {}, gender: {}, status: {}, limit: {}", 
               jobId, accountId, electionId, assignedBooths, gender, status, limit);
       
       try {
           try {
               processVolunteerExportS3(jobId, accountId, electionId, assignedBooths, gender, status, limit);
           } catch (Exception s3Exception) {
               log.warn("S3 export failed for jobId: {}, falling back to local: {}", jobId, s3Exception.getMessage());
               processVolunteerExportLocal(jobId, accountId, electionId, assignedBooths, gender, status, limit);
           }
       } catch (Exception e) {
           log.error("Error in processVolunteerExport for jobId: {}, error: {}", jobId, e.getMessage(), e);
           throw e;
       }
   }

	// Comprehensive export processing method with all filters
	@Transactional
	public void processVolunteerExportWithFilters(Long jobId, Long accountId, Long electionId,
			String mobileNumber, List<Long> assignedBooths, Long userId, String searchTerm,
			String gender, String status, Integer limit) {
		
		try {
			// Try S3 upload first, fallback to local if it fails
			try {
				processVolunteerExportS3WithFilters(jobId, accountId, electionId, 
					mobileNumber, assignedBooths, userId, searchTerm, gender, status, limit);
			} catch (Exception s3Exception) {
				log.warn("Comprehensive S3 export failed for jobId: {}, falling back to local: {}", jobId, s3Exception.getMessage());
				processVolunteerExportLocalWithFilters(jobId, accountId, electionId, 
					mobileNumber, assignedBooths, userId, searchTerm, gender, status, limit);
				log.info("Successfully completed comprehensive local fallback export for jobId: {}", jobId);
			}
			
			log.info("Successfully completed comprehensive processVolunteerExport for jobId: {}", jobId);
		} catch (Exception e) {
			log.error("Error in comprehensive processVolunteerExport for jobId: {}, error: {}", jobId, e.getMessage(), e);
			throw e;
		}
	}
   
   @Transactional
   private void processVolunteerExportS3(Long jobId, Long accountId, Long electionId,
                                         List<Long> assignedBooths, String gender, String status, Integer limit) {
       
       log.info("Starting processVolunteerExportS3 for jobId: {}, accountId: {}, electionId: {}", 
                jobId, accountId, electionId);
       
       try {
           log.info("Building specification for S3 export jobId: {} with filters - assignedBooths: {}, gender: {}, status: {}", 
                   jobId, assignedBooths, gender, status);
           
           // Build specification for filtering
           Specification<VolunteerEntity> spec = buildSpecification(electionId, accountId, assignedBooths, gender, status);
           
           log.info("Built specification successfully for S3 export jobId: {}", jobId);
           
           // Check total count before processing
           long totalCount = volunteerRepo.count(spec);
           log.info("Total volunteers matching criteria for S3 export jobId: {}: {}", jobId, totalCount);
           
           if (totalCount == 0) {
               log.warn("No volunteers found matching criteria for S3 export jobId: {}", jobId);
               
               // Update job status to indicate no data
               VolunteerDownloadJob job = volunteerDownloadJobRepository.findById(jobId)
                       .orElseThrow(() -> new ThedalException(ThedalError.JOB_NOT_FOUND, HttpStatus.NOT_FOUND));
               
               job.setStatus("COMPLETED");
               job.setTimeCompleted(LocalDateTime.now());
               job.setErrorMessage("No volunteers found matching the specified criteria");
               volunteerDownloadJobRepository.save(job);
               
               log.info("Marked S3 export job as completed with no data for jobId: {}", jobId);
               return;
           }
           
           // Generate Excel file
           log.info("Generating Excel file for S3 export jobId: {}", jobId);
           File excelFile;
           try {
               excelFile = generateExcelFileStreamedLocal(spec, limit, jobId);
           } catch (IOException ioException) {
               log.error("Failed to generate Excel file for S3 export jobId: {}, error: {}", 
                         jobId, ioException.getMessage(), ioException);
               throw new RuntimeException("Failed to generate Excel file: " + ioException.getMessage(), ioException);
           }
           log.info("Successfully generated Excel file for S3 export jobId: {}, file size: {} bytes", 
                    jobId, excelFile.length());
           
           // Upload to S3
           log.info("Uploading to S3 for jobId: {}", jobId);
           String fileName = "volunteer_export_" + jobId + "_" + System.currentTimeMillis() + ".xlsx";
           String s3Url = awsFileUpload.uploadToAWS(excelFile, fileName, s3Filesbucket);
           log.info("Successfully uploaded to S3 for jobId: {}, S3 URL: {}", jobId, s3Url);
           
           // Clean up temp file
           try {
               if (excelFile.exists() && !excelFile.delete()) {
                   log.warn("Failed to delete temp file for S3 export jobId: {}: {}", 
                            jobId, excelFile.getAbsolutePath());
               }
           } catch (Exception cleanupException) {
               log.warn("Error cleaning up temp file for S3 export jobId: {}: {}", 
                        jobId, cleanupException.getMessage());
           }
           
           // Update job with completion status and S3 download URL
           log.info("Updating S3 export job status to COMPLETED for jobId: {}", jobId);
           VolunteerDownloadJob job = volunteerDownloadJobRepository.findById(jobId)
                   .orElseThrow(() -> new ThedalException(ThedalError.JOB_NOT_FOUND, HttpStatus.NOT_FOUND));
           
           job.setStatus("COMPLETED");
           job.setTimeCompleted(LocalDateTime.now());
           job.setAwsS3DownloadUrl(s3Url);
           volunteerDownloadJobRepository.save(job);
           
           log.info("Successfully completed S3 volunteer export for jobId: {}, status: {}, downloadUrl: {}", 
                    jobId, job.getStatus(), s3Url);
           
       } catch (Exception e) {
           log.error("Error processing S3 volunteer export for jobId: {}, error: {}", 
                     jobId, e.getMessage(), e);
           
           // Update job status to ERROR
           try {
               VolunteerDownloadJob job = volunteerDownloadJobRepository.findById(jobId).orElse(null);
               if (job != null) {
                   job.setStatus("ERROR");
                   job.setErrorMessage("S3 export failed: " + e.getMessage());
                   job.setTimeCompleted(LocalDateTime.now());
                   volunteerDownloadJobRepository.save(job);
                   
                   log.info("Updated S3 export job status to ERROR for jobId: {}", jobId);
               }
           } catch (Exception updateException) {
               log.error("Failed to update S3 export job status to ERROR for jobId: {}, updateError: {}", 
                         jobId, updateException.getMessage(), updateException);
           }
           
           throw e; // Re-throw to allow fallback to local
       }
   }

   @Transactional
   private void processVolunteerExportLocal(Long jobId, Long accountId, Long electionId,
                                            List<Long> assignedBooths, String gender, String status, Integer limit) {
       
       log.info("Starting processVolunteerExportLocal for jobId: {}, accountId: {}, electionId: {}", 
                jobId, accountId, electionId);
       
       try {
           log.info("Building specification for jobId: {} with filters - assignedBooths: {}, gender: {}, status: {}", 
                   jobId, assignedBooths, gender, status);
           
           // Build specification for filtering
           Specification<VolunteerEntity> spec = buildSpecification(electionId, accountId, assignedBooths, gender, status);
           
           log.info("Built specification successfully for jobId: {}", jobId);
           
           // Check total count before processing
           long totalCount = volunteerRepo.count(spec);
           log.info("Total volunteers matching criteria for jobId: {}: {}", jobId, totalCount);
           
           if (totalCount == 0) {
               log.warn("No volunteers found matching criteria for jobId: {}", jobId);
               
               // Update job status to indicate no data
               VolunteerDownloadJob job = volunteerDownloadJobRepository.findById(jobId)
                       .orElseThrow(() -> new ThedalException(ThedalError.JOB_NOT_FOUND, HttpStatus.NOT_FOUND));
               
               job.setStatus("COMPLETED");
               job.setTimeCompleted(LocalDateTime.now());
               job.setErrorMessage("No volunteers found matching the specified criteria");
               volunteerDownloadJobRepository.save(job);
               
               log.info("Marked job as completed with no data for jobId: {}", jobId);
               return;
           }
           
           // Generate Excel file locally
           log.info("Starting Excel file generation for jobId: {}", jobId);
           File excelFile;
           try {
               excelFile = generateExcelFileStreamedLocal(spec, limit, jobId);
           } catch (IOException ioException) {
               log.error("IOException during Excel file generation for jobId: {}, error: {}", 
                         jobId, ioException.getMessage(), ioException);
               throw new RuntimeException("Failed to generate Excel file: " + ioException.getMessage(), ioException);
           }
           log.info("Excel file generated successfully for jobId: {}, file: {}", 
                    jobId, excelFile.getAbsolutePath());
           
           // Update job with completion status and local download URL
           log.info("Updating job status to COMPLETED for jobId: {}", jobId);
           VolunteerDownloadJob job = volunteerDownloadJobRepository.findById(jobId)
                   .orElseThrow(() -> new ThedalException(ThedalError.JOB_NOT_FOUND, HttpStatus.NOT_FOUND));
           
           job.setStatus("COMPLETED");
           job.setTimeCompleted(LocalDateTime.now());
           String baseUrl = serverUrl.endsWith("/") ? serverUrl.substring(0, serverUrl.length() - 1) : serverUrl;
           String downloadUrl = baseUrl + "/api/volunteer/" + electionId + "/export/download/" + jobId;
           job.setAwsS3DownloadUrl(downloadUrl);
           volunteerDownloadJobRepository.save(job);
           
           log.info("Successfully completed local volunteer export for jobId: {}, status: {}, downloadUrl: {}", 
                    jobId, job.getStatus(), downloadUrl);
           
       } catch (Exception e) {
           log.error("Error processing local volunteer export for jobId: {}, error: {}", 
                     jobId, e.getMessage(), e);
           
           // Update job status to ERROR
           try {
               VolunteerDownloadJob job = volunteerDownloadJobRepository.findById(jobId).orElse(null);
               if (job != null) {
                   job.setStatus("ERROR");
                   job.setErrorMessage(e.getMessage());
                   job.setTimeCompleted(LocalDateTime.now());
                   volunteerDownloadJobRepository.save(job);
                   
                   log.info("Updated job status to ERROR for jobId: {}", jobId);
               } else {
                   log.error("Could not find job to update error status for jobId: {}", jobId);
               }
           } catch (Exception updateException) {
               log.error("Failed to update job status to ERROR for jobId: {}, updateError: {}", 
                         jobId, updateException.getMessage(), updateException);
           }
           
           throw e;
       }
   }

	// S3 export method with comprehensive filters
	@Transactional
	private void processVolunteerExportS3WithFilters(Long jobId, Long accountId, Long electionId,
			String mobileNumber, List<Long> assignedBooths, Long userId, String searchTerm,
			String gender, String status, Integer limit) {
		
		log.info("Starting comprehensive S3 export for jobId: {}, accountId: {}, electionId: {}", jobId, accountId, electionId);
		log.info("Export parameters - electionId: {}, assignedBooths: {}, mobileNumber: {}, userId: {}, searchTerm: {}, gender: {}, status: {}, limit: {}", 
		    electionId, assignedBooths, mobileNumber, userId, searchTerm, gender, status, limit);
		
		try {
			log.info("Building comprehensive filters for S3 export jobId: {}", jobId);
			
			// Count total matching records using the same repository method as GET API
            Pageable countPageable = PageRequest.of(0, 1);
            Page<VolunteerEntity> countPage;
            try {
                log.info("Calling count query with electionId: {}, assignedBooths: {}, mobileNumber: {}, userId: {}", 
                    electionId, assignedBooths, mobileNumber, userId);
                // Use the same 5-parameter method as the working GET API (without searchTerm)
                countPage = volunteerRepo.findVolunteerPageByElectionIdAndAssignedBoothsAndMobileNumberAndUserId(
                    electionId, assignedBooths, mobileNumber, userId, countPageable);
                log.info("Count query successful, total elements: {}", countPage.getTotalElements());
            } catch (Exception ex) {
                String msg = ex.getMessage();
                log.error("Count query failed with error: {}", msg, ex);
                if (msg != null && (msg.toLowerCase().contains("upper(bytea)") || msg.toLowerCase().contains("character varying ~~ bytea"))) {
                    log.warn("Using emergency basic query for count due to bytea error");
                    countPage = volunteerRepo.findBasicVolunteersByElectionId(electionId, countPageable);
                } else {
                    log.warn("Using emergency basic query for count due to other error");
                    countPage = volunteerRepo.findBasicVolunteersByElectionId(electionId, countPageable);
                }
                log.info("Emergency count query result, total elements: {}", countPage.getTotalElements());
            }
			
			long totalCount = countPage.getTotalElements();
			log.info("Total volunteers matching comprehensive criteria for S3 export jobId: {}: {}", jobId, totalCount);
			
			if (totalCount == 0) {
				log.warn("No volunteers found matching comprehensive criteria for S3 export jobId: {}", jobId);
				
				// Update job status to indicate no data
				VolunteerDownloadJob job = volunteerDownloadJobRepository.findById(jobId)
						.orElseThrow(() -> new ThedalException(ThedalError.JOB_NOT_FOUND, HttpStatus.NOT_FOUND));
				
				job.setStatus("COMPLETED");
				job.setTimeCompleted(LocalDateTime.now());
				job.setErrorMessage("No volunteers found matching the specified criteria");
				volunteerDownloadJobRepository.save(job);
				
				log.info("Marked comprehensive S3 job as completed with no data for jobId: {}", jobId);
				return;
			}
			
			// Generate Excel file using comprehensive filters
			log.info("Starting comprehensive Excel file generation for S3 export jobId: {}", jobId);
			File excelFile;
			try {
				excelFile = generateExcelFileStreamedWithFilters(
					electionId, mobileNumber, assignedBooths, userId, searchTerm, gender, status, limit, jobId);
			} catch (IOException ioException) {
				log.error("Failed to generate comprehensive Excel file for S3 export jobId: {}, error: {}", jobId, ioException.getMessage(), ioException);
				throw new RuntimeException("Failed to generate Excel file: " + ioException.getMessage(), ioException);
			}
			log.info("Successfully generated comprehensive Excel file for S3 export jobId: {}, file size: {} bytes", 
					jobId, excelFile.length());
			
			// Upload to S3
			log.info("Uploading comprehensive export to S3 for jobId: {}", jobId);
			String fileName = "volunteer_export_" + jobId + "_" + System.currentTimeMillis() + ".xlsx";
			String s3Url = awsFileUpload.uploadToAWS(excelFile, fileName, s3Filesbucket);
			log.info("Successfully uploaded comprehensive export to S3 for jobId: {}, S3 URL: {}", jobId, s3Url);
			
			// Clean up temp file
			try {
				if (excelFile.exists() && !excelFile.delete()) {
					log.warn("Failed to delete temp file for comprehensive S3 export jobId: {}: {}", jobId, excelFile.getAbsolutePath());
				}
			} catch (Exception cleanupException) {
				log.warn("Error cleaning up temp file for comprehensive S3 export jobId: {}: {}", jobId, cleanupException.getMessage());
			}
			
			// Update job with completion status and S3 download URL
			log.info("Updating comprehensive S3 export job status to COMPLETED for jobId: {}", jobId);
			VolunteerDownloadJob job = volunteerDownloadJobRepository.findById(jobId)
					.orElseThrow(() -> new ThedalException(ThedalError.JOB_NOT_FOUND, HttpStatus.NOT_FOUND));
			
			job.setStatus("COMPLETED");
			job.setTimeCompleted(LocalDateTime.now());
			job.setAwsS3DownloadUrl(s3Url);
			volunteerDownloadJobRepository.save(job);
			
			log.info("Successfully completed comprehensive S3 volunteer export for jobId: {}, status: {}, downloadUrl: {}", 
					jobId, job.getStatus(), s3Url);
			
		} catch (Exception e) {
			log.error("Error processing comprehensive S3 volunteer export for jobId: {}, error: {}", jobId, e.getMessage(), e);
			
			// Update job status to ERROR
			try {
				VolunteerDownloadJob job = volunteerDownloadJobRepository.findById(jobId)
						.orElse(null);
				if (job != null) {
					job.setStatus("ERROR");
					job.setErrorMessage("S3 export failed: " + e.getMessage());
					job.setTimeCompleted(LocalDateTime.now());
					volunteerDownloadJobRepository.save(job);
					
					log.info("Updated comprehensive S3 export job status to ERROR for jobId: {}", jobId);
				}
			} catch (Exception updateException) {
				log.error("Failed to update comprehensive S3 export job status to ERROR for jobId: {}, updateError: {}", 
						jobId, updateException.getMessage(), updateException);
			}
			
			throw e;  // Re-throw to allow fallback to local
		}
	}

	// Local export method with comprehensive filters  
	@Transactional
	private void processVolunteerExportLocalWithFilters(Long jobId, Long accountId, Long electionId,
			String mobileNumber, List<Long> assignedBooths, Long userId, String searchTerm,
			String gender, String status, Integer limit) {
		
		log.info("Starting comprehensive local export for jobId: {}, accountId: {}, electionId: {}", jobId, accountId, electionId);
		
		try {
			log.info("Building comprehensive filters for local export jobId: {}", jobId);
			
			// Count total matching records using the same repository method as GET API
            Pageable countPageable = PageRequest.of(0, 1);
            Page<VolunteerEntity> countPage;
            try {
                // Use the same 5-parameter method as the working GET API (without searchTerm)
                countPage = volunteerRepo.findVolunteerPageByElectionIdAndAssignedBoothsAndMobileNumberAndUserId(
                    electionId, assignedBooths, mobileNumber, userId, countPageable);
            } catch (Exception ex) {
                String msg = ex.getMessage();
                log.warn("All complex queries failed for local count - using emergency basic query (electionId only)");
                countPage = volunteerRepo.findBasicVolunteersByElectionId(electionId, countPageable);
            }
			
			long totalCount = countPage.getTotalElements();
			log.info("Total volunteers matching comprehensive criteria for local export jobId: {}: {}", jobId, totalCount);
			
			if (totalCount == 0) {
				log.warn("No volunteers found matching comprehensive criteria for local export jobId: {}", jobId);
				
				// Update job status to indicate no data
				VolunteerDownloadJob job = volunteerDownloadJobRepository.findById(jobId)
						.orElseThrow(() -> new ThedalException(ThedalError.JOB_NOT_FOUND, HttpStatus.NOT_FOUND));
				
				job.setStatus("COMPLETED");
				job.setTimeCompleted(LocalDateTime.now());
				job.setErrorMessage("No volunteers found matching the specified criteria");
				volunteerDownloadJobRepository.save(job);
				
				log.info("Marked comprehensive local job as completed with no data for jobId: {}", jobId);
				return;
			}
			
			// Generate Excel file using comprehensive filters
			log.info("Starting comprehensive Excel file generation for local export jobId: {}", jobId);
			File excelFile;
			try {
				excelFile = generateExcelFileStreamedWithFilters(
					electionId, mobileNumber, assignedBooths, userId, searchTerm, gender, status, limit, jobId);
			} catch (IOException ioException) {
				log.error("Failed to generate comprehensive Excel file for local export jobId: {}, error: {}", jobId, ioException.getMessage(), ioException);
				throw new RuntimeException("Failed to generate Excel file: " + ioException.getMessage(), ioException);
			}
			log.info("Successfully generated comprehensive Excel file for local export jobId: {}, file size: {} bytes", 
					jobId, excelFile.length());
			
			// For local storage, just update job status to COMPLETED
			log.info("Updating comprehensive local export job status to COMPLETED for jobId: {}", jobId);
			VolunteerDownloadJob job = volunteerDownloadJobRepository.findById(jobId)
					.orElseThrow(() -> new ThedalException(ThedalError.JOB_NOT_FOUND, HttpStatus.NOT_FOUND));
			
			job.setStatus("COMPLETED");
			job.setTimeCompleted(LocalDateTime.now());
			// For local files, we don't set S3 URL
			volunteerDownloadJobRepository.save(job);
			
			log.info("Successfully completed comprehensive local volunteer export for jobId: {}, file: {}", 
					jobId, excelFile.getAbsolutePath());
			
		} catch (Exception e) {
			log.error("Error processing comprehensive local volunteer export for jobId: {}, error: {}", jobId, e.getMessage(), e);
			
			// Update job status to ERROR
			try {
				VolunteerDownloadJob job = volunteerDownloadJobRepository.findById(jobId)
						.orElse(null);
				if (job != null) {
					job.setStatus("ERROR");
					job.setErrorMessage("Local export failed: " + e.getMessage());
					job.setTimeCompleted(LocalDateTime.now());
					volunteerDownloadJobRepository.save(job);
					
					log.info("Updated comprehensive local export job status to ERROR for jobId: {}", jobId);
				}
			} catch (Exception updateException) {
				log.error("Failed to update comprehensive local export job status to ERROR for jobId: {}, updateError: {}", 
						jobId, updateException.getMessage(), updateException);
			}
			
			throw e;
		}
	}

	// Helper method to generate Excel file with comprehensive filters
	private File generateExcelFileStreamedWithFilters(
			Long electionId, String mobileNumber, List<Long> assignedBooths, Long userId, String searchTerm,
			String gender, String status, Integer limit, Long jobId) throws IOException {
		
		log.info("Starting generateExcelFileStreamedWithFilters for jobId: {}", jobId);
		
		// Create temp file for export
		String exportDir = System.getProperty("java.io.tmpdir") + "/thedal-exports";
		File directory = new File(exportDir);
		if (!directory.exists()) {
			directory.mkdirs();
		}
		
		File outputFile = new File(directory, "temp-volunteer-export-" + System.currentTimeMillis() + ".xlsx");
		
		try (SXSSFWorkbook workbook = new SXSSFWorkbook(1000);
			 FileOutputStream fos = new FileOutputStream(outputFile)) {
			
			workbook.setCompressTempFiles(true);
			Sheet sheet = workbook.createSheet("Volunteers");
			
			// Set column widths
			for (int i = 0; i < 20; i++) {
				sheet.setColumnWidth(i, 15 * 256);
			}
			
			// Create header row
			Row headerRow = sheet.createRow(0);
			VolunteerExcelHeader.createHeaderRow(headerRow);
			int rowNum = 1;
			
			// Process data in batches using the comprehensive repository method
			int page = 0;
			int processed = 0;
			final int BATCH_SIZE = 1000;
			
			while (true) {
				log.info("Starting data fetch for page: {}, BATCH_SIZE: {}, processed so far: {}", page, BATCH_SIZE, processed);
				Pageable pageable = PageRequest.of(page, BATCH_SIZE);
                Page<VolunteerEntity> volunteerPage;
                try {
                    log.info("Calling data query with electionId: {}, assignedBooths: {}, mobileNumber: {}, userId: {}", 
                        electionId, assignedBooths, mobileNumber, userId);
                    // Use the same 5-parameter method as the working GET API (without searchTerm)
                    volunteerPage = volunteerRepo.findVolunteerPageByElectionIdAndAssignedBoothsAndMobileNumberAndUserId(
                        electionId, assignedBooths, mobileNumber, userId, pageable);
                    log.info("Data query successful for page: {}, records returned: {}, total elements: {}", 
                        page, volunteerPage.getContent().size(), volunteerPage.getTotalElements());
                } catch (Exception ex) {
                    String msg = ex.getMessage();
                    log.error("Data query failed for page: {} with error: {}", page, msg, ex);
                    log.warn("Complex query failed for page {} - using emergency basic query (electionId only)", page);
                    volunteerPage = volunteerRepo.findBasicVolunteersByElectionId(electionId, pageable);
                    log.info("Emergency data query result for page: {}, records returned: {}, total elements: {}", 
                        page, volunteerPage.getContent().size(), volunteerPage.getTotalElements());
				}
				
				if (volunteerPage.isEmpty()) {
					log.info("Empty page returned for page: {}, breaking out of loop", page);
					break;
				}
				
				log.info("Processing comprehensive page: {}, records in page: {}, processed so far: {}", 
						page, volunteerPage.getContent().size(), processed);
				
				int recordsInThisPage = 0;
				for (VolunteerEntity volunteer : volunteerPage.getContent()) {
					try {
						log.debug("Processing volunteer ID: {}, firstName: {}, lastName: {}", 
						    volunteer.getId(), 
						    volunteer.getUserEntity() != null ? volunteer.getUserEntity().getFirstName() : "null", 
						    volunteer.getUserEntity() != null ? volunteer.getUserEntity().getLastName() : "null");
						    
						Row row = sheet.createRow(rowNum++);
						log.debug("Created Excel row: {}", rowNum - 1);
						
						VolunteerExcelDataRow.populateDataRow(row, volunteer);
						log.debug("Populated Excel row: {} with volunteer ID: {}", rowNum - 1, volunteer.getId());
						
						processed++;
						recordsInThisPage++;
						
						if (rowNum % 100 == 0) {
							((SXSSFSheet) sheet).flushRows();
						}
						
						if (limit != null && processed >= limit) {
							log.info("Reached comprehensive export limit: {}, stopping processing", limit);
							break;
						}
					} catch (Exception e) {
						log.error("Error processing volunteer ID: {} in Excel row: {}, error: {}", 
						    volunteer.getId(), rowNum - 1, e.getMessage(), e);
						// Continue processing other volunteers
					}
				}
				log.info("Completed processing page: {}, records processed in this page: {}, total processed: {}", 
				    page, recordsInThisPage, processed);				if (limit != null && processed >= limit) {
					break;
				}
				
				page++;
				log.info("Moving to next page: {}", page);
			}
			
			((SXSSFSheet) sheet).flushRows();
			log.info("Final flush of comprehensive export rows, total rows processed: {}", processed);
			
			workbook.write(fos);
			fos.flush();
			workbook.dispose();
			
			log.info("Generated comprehensive Excel file with {} records at: {}", processed, outputFile.getAbsolutePath());
			log.info("Excel file size: {} bytes, exists: {}", outputFile.length(), outputFile.exists());
			return outputFile;
			
		} catch (Exception e) {
			log.error("Error generating comprehensive Excel file, error: {}", e.getMessage(), e);
			
			if (outputFile != null && outputFile.exists()) {
				boolean deleted = outputFile.delete();
				log.warn("Attempted to delete temporary comprehensive file: {}, success: {}", outputFile.getAbsolutePath(), deleted);
			}
			throw new IOException("Failed to generate comprehensive Excel file: " + e.getMessage(), e);
		}
	}
   
   private Specification<VolunteerEntity> buildSpecification(Long electionId, Long accountId, List<Long> assignedBooths, String gender, String status) {
       return (root, query, cb) -> {
           List<Predicate> predicates = new ArrayList<>();
           predicates.add(cb.equal(root.get("electionEntity").get("id"), electionId));
           predicates.add(cb.equal(root.get("accountId"), accountId));
           if (assignedBooths != null) {
               predicates.add(root.join("assignedBooth").in(assignedBooths));
           }
           if (gender != null) {
               predicates.add(cb.equal(cb.lower(root.get("gender")), gender.toLowerCase()));
           }
           if (status != null) {
               predicates.add(cb.equal(cb.lower(root.get("status")), status.toLowerCase()));
           }
           return cb.and(predicates.toArray(new Predicate[0]));
       };
   }
   
   private File generateExcelFileStreamedLocal(Specification<VolunteerEntity> spec, Integer limit, Long jobId) throws IOException {
       // Mirror voter generation but for volunteers
       log.info("Starting generateExcelFileStreamedLocal for jobId: {}, limit: {}", jobId, limit);
       
       System.setProperty("java.awt.headless", "true");
       
       String exportDir = System.getProperty("java.io.tmpdir") + "/thedal-exports";
       File exportDirectory = new File(exportDir);
       if (!exportDirectory.exists()) {
           exportDirectory.mkdirs();
       }
       
       File outputFile = new File(exportDirectory, "volunteer-export-" + jobId + ".xlsx");
       
       int processed = 0;
       int page = 0;

       int totalRecords = limit == null ? (int) volunteerRepo.count(spec) : Math.min(limit, (int) volunteerRepo.count(spec));

       if (totalRecords == 0) {
           throw new ThedalException(ThedalError.VOLUNTEER_NOT_FOUND, HttpStatus.NOT_FOUND);
       }

       try (SXSSFWorkbook workbook = new SXSSFWorkbook(100);
            FileOutputStream fos = new FileOutputStream(outputFile)) {

           workbook.setCompressTempFiles(true);

           Sheet sheet = workbook.createSheet("Volunteers");

           for (int i = 0; i < 20; i++) {  // Adjust for number of columns
               sheet.setColumnWidth(i, 15 * 256);
           }

           Row headerRow = sheet.createRow(0);
           VolunteerExcelHeader.createHeaderRow(headerRow);
           int rowNum = 1;

           while (processed < totalRecords) {
               Pageable pageable = PageRequest.of(page, BATCH_SIZE);
               Page<VolunteerEntity> volunteerPage = volunteerRepo.findAll(spec, pageable);

               if (volunteerPage.isEmpty()) {
                   break;
               }

               for (VolunteerEntity volunteer : volunteerPage.getContent()) {
                   Row row = sheet.createRow(rowNum++);
                   VolunteerExcelDataRow.populateDataRow(row, volunteer);
                   processed++;

                   if (rowNum % 100 == 0) {
                       ((SXSSFSheet) sheet).flushRows();
                   }

                   if (limit != null && processed >= limit) {
                       break;
                   }
               }

               page++;
           }
           
           ((SXSSFSheet) sheet).flushRows();
           workbook.write(fos);
           fos.flush();
           workbook.dispose();

           return outputFile;
       } catch (Exception e) {
           if (outputFile != null && outputFile.exists()) {
               outputFile.delete();
           }
           throw new IOException("Failed to generate Excel file for jobId " + jobId, e);
       }
   }

   @Transactional(readOnly = true)
   public VolunteerJobStatusResponse getVolunteerExportJobStatus(Long jobId, Long electionId, Long accountId) {
       log.info("Retrieving status for jobId: {}, electionId: {}, accountId: {}", jobId, electionId, accountId);

       VolunteerDownloadJob job = volunteerDownloadJobRepository.findById(jobId)
               .orElseThrow(() -> {
                   log.error("Job not found for jobId: {}", jobId);
                   return new ThedalException(ThedalError.JOB_NOT_FOUND, HttpStatus.NOT_FOUND);
               });

       if (!job.getElectionId().equals(electionId) || !job.getAccountId().equals(accountId)) {
           log.error("Unauthorized access to jobId: {} by accountId: {} for electionId: {}",
                    jobId, accountId, electionId);
           throw new ThedalException(ThedalError.UNAUTHORIZED_ACCESS, HttpStatus.FORBIDDEN);
       }

       VolunteerJobStatusResponse response = new VolunteerJobStatusResponse();
       response.setJobId(job.getId());
       response.setStatus(job.getStatus());
       response.setTimeStarted(job.getTimeStarted());
       response.setTimeCompleted(job.getTimeCompleted());
       response.setErrorMessage(job.getErrorMessage());
       response.setDownloadUrl(job.getAwsS3DownloadUrl());

       log.info("Successfully retrieved status for jobId: {}, status: {}", jobId, job.getStatus());
       return response;
   }

   @Override
   public ThedalResponse<List<String>> getUniqueRoleNames(Long electionId) {
       log.info("Getting unique role names for electionId: {}", electionId);
       
       Long accountId = requestDetails.getCurrentAccountId();
       if (accountId == null) {
           throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
       }
       
       validateElectionOwnership(electionId, accountId);
       
       List<String> roleNames = volunteerRepo.findUniqueRoleNamesByElectionId(electionId);
       log.info("Found {} unique role names: {}", roleNames.size(), roleNames);
       
       return new ThedalResponse<>(ThedalSuccess.VOLUNTEER_FOUND, roleNames);
   }


}
