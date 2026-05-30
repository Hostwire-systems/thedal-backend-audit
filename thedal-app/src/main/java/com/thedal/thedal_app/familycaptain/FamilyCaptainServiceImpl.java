package com.thedal.thedal_app.familycaptain;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.thedal.thedal_app.account.AccountRepository;
import com.thedal.thedal_app.election.ElectionEntity;
import com.thedal.thedal_app.election.ElectionRepository;
import com.thedal.thedal_app.familycaptain.dto.FamilyCaptainDetailsDTO;
import com.thedal.thedal_app.familycaptain.dto.FamilyCaptainDetailsUpdate;
import com.thedal.thedal_app.familycaptain.dto.FamilyCaptainUploadSummary;
import com.thedal.thedal_app.familycaptain.dto.FamilyDetailsDTO;
import com.thedal.thedal_app.familycaptain.dto.FamilyUpdateRequest;
import com.thedal.thedal_app.familycaptain.dto.SaveFamilyCaptainDetailsDTO;
import com.thedal.thedal_app.general.RequestDetailsService;
import com.thedal.thedal_app.response.ThedalResponse;
import com.thedal.thedal_app.response.ThedalSuccess;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;
import com.thedal.thedal_app.user.MongoUser;
import com.thedal.thedal_app.user.MongoUserRepository;
import com.thedal.thedal_app.user.UserEntity;
import com.thedal.thedal_app.user.UserRepo;
import com.thedal.thedal_app.voter.Address;
import com.thedal.thedal_app.voter.VoterRepo;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class FamilyCaptainServiceImpl implements FamilyCaptainService {

    @Autowired
    private FamilyCaptainRepository familyCaptainRepository;

    @Autowired
    private UserRepo userRepository;

    @Autowired
    private ElectionRepository electionRepository;

    @Autowired
    private RequestDetailsService requestDetails;

    @Autowired
    private VoterRepo voterRepository;

    @Autowired
    private MongoUserRepository mongoUserRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Override
    @Transactional
    public ThedalResponse<Void> saveFamilyCaptain(SaveFamilyCaptainDetailsDTO familyCaptainDto, Long electionId) {
        try {
            // Get current account
            Long accountId = requestDetails.getCurrentAccountId();
            if (accountId == null) {
                log.error("Account ID not found, unauthorized access.");
                throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
            }

            // Validate election exists and belongs to account
            ElectionEntity election = electionRepository.findByIdAndAccountId(electionId, accountId)
                .orElseThrow(() -> new ThedalException(ThedalError.INVALID_ELECTION, HttpStatus.NOT_FOUND));

            // Create or find user
            UserEntity user = createOrFindUser(familyCaptainDto, accountId);

            // Check if user is already a family captain in this election
            if (familyCaptainRepository.existsByUserEntity_IdAndElectionEntity_IdAndAccountId(user.getId(), electionId, accountId)) {
                throw new ThedalException(ThedalError.USER_ALREADY_EXISTS, HttpStatus.CONFLICT, 
                    "User is already a family captain in this election");
            }

            // Validate assigned families exist
            if (familyCaptainDto.getAssignedFamilies() != null && !familyCaptainDto.getAssignedFamilies().isEmpty()) {
                validateFamiliesExist(familyCaptainDto.getAssignedFamilies(), electionId, accountId);
            }

            // Create family captain
            FamilyCaptainEntity familyCaptain = new FamilyCaptainEntity();
            mapDtoToEntity(familyCaptainDto, familyCaptain);
            familyCaptain.setUserEntity(user);
            familyCaptain.setElectionEntity(election);
            familyCaptain.setAccountId(accountId);
            familyCaptain.setAdminUserId(requestDetails.getCurrentUserId());

            familyCaptainRepository.save(familyCaptain);

            log.info("Family captain created successfully for user {} in election {}", user.getId(), electionId);
            return new ThedalResponse<Void>(ThedalSuccess.SUCCESS, null);

        } catch (ThedalException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error creating family captain: {}", e.getMessage(), e);
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ThedalResponse<FamilyCaptainDetailsDTO> getFamilyCaptainByUserId(Long userId, Long electionId) {
        try {
            Long accountId = requestDetails.getCurrentAccountId();
            if (accountId == null) {
                throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
            }

            FamilyCaptainEntity familyCaptain = familyCaptainRepository
                .findByUserEntity_IdAndElectionEntity_IdAndAccountId(userId, electionId, accountId)
                .orElseThrow(() -> new ThedalException(ThedalError.VOLUNTEER_NOT_FOUND, HttpStatus.NOT_FOUND,
                    "Family captain not found"));

            FamilyCaptainDetailsDTO dto = mapEntityToDto(familyCaptain);
            return new ThedalResponse<FamilyCaptainDetailsDTO>(ThedalSuccess.SUCCESS, dto);

        } catch (ThedalException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error retrieving family captain: {}", e.getMessage(), e);
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    @Transactional
    public ThedalResponse<Void> updateFamilyCaptain(Long userId, Long electionId, FamilyCaptainDetailsUpdate update) {
        try {
            Long accountId = requestDetails.getCurrentAccountId();
            if (accountId == null) {
                throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
            }

            FamilyCaptainEntity familyCaptain = familyCaptainRepository
                .findByUserEntity_IdAndElectionEntity_IdAndAccountId(userId, electionId, accountId)
                .orElseThrow(() -> new ThedalException(ThedalError.VOLUNTEER_NOT_FOUND, HttpStatus.NOT_FOUND));

            // Update fields
            if (update.getFirstName() != null) familyCaptain.setFirstName(update.getFirstName());
            if (update.getLastName() != null) familyCaptain.setLastName(update.getLastName());
            if (update.getEmail() != null) familyCaptain.setEmail(update.getEmail());
            if (update.getMobileNumber() != null) familyCaptain.setMobileNumber(update.getMobileNumber());
            if (update.getFamilyCaptainAddress() != null) familyCaptain.setFamilyCaptainAddress(update.getFamilyCaptainAddress());
            if (update.getStatus() != null) familyCaptain.setStatus(update.getStatus());
            if (update.getPhotoUrl() != null) familyCaptain.setPhotoUrl(update.getPhotoUrl());
            if (update.getRemarks() != null) familyCaptain.setRemarks(update.getRemarks());
            if (update.getWhatsAppNumber() != null) familyCaptain.setWhatsAppNumber(update.getWhatsAppNumber());
            if (update.getGender() != null) familyCaptain.setGender(update.getGender());

            familyCaptainRepository.save(familyCaptain);

            log.info("Family captain updated successfully for user {} in election {}", userId, electionId);
            return new ThedalResponse<Void>(ThedalSuccess.SUCCESS, null);

        } catch (ThedalException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error updating family captain: {}", e.getMessage(), e);
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    @Transactional
    public ThedalResponse<Void> deleteFamilyCaptain(Long userId, Long electionId) {
        try {
            Long accountId = requestDetails.getCurrentAccountId();
            if (accountId == null) {
                throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
            }

            // Check if family captain exists
            if (!familyCaptainRepository.existsByUserEntity_IdAndElectionEntity_IdAndAccountId(userId, electionId, accountId)) {
                throw new ThedalException(ThedalError.VOLUNTEER_NOT_FOUND, HttpStatus.NOT_FOUND);
            }

            familyCaptainRepository.deleteByUserIdAndElectionIdAndAccountId(userId, electionId, accountId);

            log.info("Family captain deleted successfully for user {} in election {}", userId, electionId);
            return new ThedalResponse<Void>(ThedalSuccess.SUCCESS, null);

        } catch (ThedalException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error deleting family captain: {}", e.getMessage(), e);
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    @Transactional
    public ThedalResponse<Void> deleteFamilyCaptains(Long electionId, List<Long> userIdList) {
        try {
            Long accountId = requestDetails.getCurrentAccountId();
            if (accountId == null) {
                throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
            }

            if (userIdList == null || userIdList.isEmpty()) {
                throw new ThedalException(ThedalError.INVALID_REQUEST, HttpStatus.BAD_REQUEST, "User ID list cannot be empty");
            }

            familyCaptainRepository.deleteByUserIdsAndElectionIdAndAccountId(userIdList, electionId, accountId);

            log.info("Deleted {} family captains from election {}", userIdList.size(), electionId);
            return new ThedalResponse<Void>(ThedalSuccess.SUCCESS, null);

        } catch (ThedalException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error deleting family captains: {}", e.getMessage(), e);
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Helper methods will continue in next part...
    
    private UserEntity createOrFindUser(SaveFamilyCaptainDetailsDTO dto, Long accountId) {
        // Find existing user by mobile number
        Optional<UserEntity> existingUser = userRepository.findByMobileNumber(dto.getMobileNumber());
        
        if (existingUser.isPresent()) {
            return existingUser.get();
        }

        // Create new user
        UserEntity newUser = new UserEntity();
        newUser.setFirstName(dto.getFirstName());
        newUser.setLastName(dto.getLastName());
        newUser.setEmail(dto.getEmail());
        newUser.setMobileNumber(dto.getMobileNumber());
        // Hash the password using BCrypt
        newUser.setPassword(new BCryptPasswordEncoder().encode(dto.getPassword()));
        newUser.setAccountId(accountId);
        newUser.setIsActive(true);
        newUser.setIsEmailVerified(true);
        newUser.setIsMobileVerified(true);
        newUser.setPasswordVersion(1);

        return userRepository.save(newUser);
    }

    private void validateFamiliesExist(List<UUID> familyIds, Long electionId, Long accountId) {
        for (UUID familyId : familyIds) {
            boolean familyExists = voterRepository.existsByFamilyIdAndElectionIdAndAccountId(familyId, electionId, accountId);
            if (!familyExists) {
                throw new ThedalException(ThedalError.INVALID_REQUEST, HttpStatus.BAD_REQUEST, 
                    "Family with ID " + familyId + " does not exist");
            }
        }
    }

    private void mapDtoToEntity(SaveFamilyCaptainDetailsDTO dto, FamilyCaptainEntity entity) {
        entity.setFirstName(dto.getFirstName());
        entity.setLastName(dto.getLastName());
        entity.setEmail(dto.getEmail());
        entity.setMobileNumber(dto.getMobileNumber());
        entity.setFamilyCaptainAddress(dto.getFamilyCaptainAddress());
        entity.setAssignedFamilies(dto.getAssignedFamilies());
        entity.setStatus(dto.getStatus());
        entity.setPhotoUrl(dto.getPhotoUrl());
        entity.setRemarks(dto.getRemarks());
        entity.setWhatsAppNumber(dto.getWhatsAppNumber());
        entity.setGender(dto.getGender());
    }

    private FamilyCaptainDetailsDTO mapEntityToDto(FamilyCaptainEntity entity) {
        FamilyCaptainDetailsDTO dto = new FamilyCaptainDetailsDTO();
        dto.setId(entity.getId());
        dto.setUserId(entity.getUserEntity().getId());
        dto.setFirstName(entity.getFirstName());
        dto.setLastName(entity.getLastName());
        dto.setEmail(entity.getEmail());
        dto.setMobileNumber(entity.getMobileNumber());
        dto.setFamilyCaptainAddress(entity.getFamilyCaptainAddress());
        dto.setAssignedFamilies(entity.getAssignedFamilies());
        dto.setStatus(entity.getStatus());
        dto.setPhotoUrl(entity.getPhotoUrl());
        dto.setRemarks(entity.getRemarks());
        dto.setWhatsAppNumber(entity.getWhatsAppNumber());
        dto.setGender(entity.getGender());
        dto.setCreatedTime(entity.getCreatedTime());
        dto.setModifiedTime(entity.getModifiedTime());
        dto.setElectionId(entity.getElectionEntity().getId());
        dto.setAccountId(entity.getAccountId());

        // Load family details
        if (entity.getAssignedFamilies() != null && !entity.getAssignedFamilies().isEmpty()) {
            dto.setAssignedFamilyDetails(loadFamilyDetails(entity.getAssignedFamilies(), entity.getElectionEntity().getId(), entity.getAccountId()));
        }

        return dto;
    }

    private List<FamilyDetailsDTO> loadFamilyDetails(List<UUID> familyIds, Long electionId, Long accountId) {
        List<FamilyDetailsDTO> familyDetails = new ArrayList<>();
        
        if (familyIds == null || familyIds.isEmpty()) {
            log.debug("No family IDs provided for loading family details");
            return familyDetails;
        }
        
        log.debug("Loading family details for {} family IDs: {} (electionId: {}, accountId: {})", 
                 familyIds.size(), familyIds, electionId, accountId);
        
        // Get family details from voter repository (family heads only)
        List<Object[]> familyData = voterRepository.findFamilyDetailsByIds(accountId, electionId, familyIds);
        
        log.debug("Found {} family head records from repository", familyData.size());
        
        // If no results found with primary query, use fallback that still ensures family head only
        if (familyData.isEmpty()) {
            log.debug("No family data found with primary query, using eldest member fallback");
            familyData = voterRepository.findFamilyDetailsByIdsWithoutFamilyHeadFilter(accountId, electionId, familyIds);
            log.debug("Found {} family head records using eldest member fallback", familyData.size());
        }
        
        // Additional validation to ensure we only have one record per family
        Map<UUID, Object[]> uniqueFamilyData = new HashMap<>();
        for (Object[] data : familyData) {
            UUID familyId = (UUID) data[0];
            if (!uniqueFamilyData.containsKey(familyId)) {
                uniqueFamilyData.put(familyId, data);
            }
        }
        familyData = new ArrayList<>(uniqueFamilyData.values());
        log.debug("After deduplication, processing {} unique family head records", familyData.size());
        
        for (Object[] data : familyData) {
            FamilyDetailsDTO familyDetail = new FamilyDetailsDTO();
            
            if (data[0] != null) {
                familyDetail.setFamilyId((UUID) data[0]);
            }
            if (data[1] != null) {
                familyDetail.setFamilySequenceNumber(((Number) data[1]).intValue());
            }
            if (data[2] != null) {
                familyDetail.setFamilyHeadName(data[2].toString());
            }
            if (data[3] != null) {
                familyDetail.setFamilyHeadEpic(data[3].toString());
            }
            if (data[4] != null) {
                familyDetail.setFamilyCount(((Number) data[4]).intValue());
            }
            if (data[5] != null) {
                familyDetail.setPartNumber(((Number) data[5]).intValue());
            }
            
            familyDetails.add(familyDetail);
        }
        
        log.debug("Successfully processed {} family details", familyDetails.size());
        
        // Validate that we have family head information only
        validateFamilyHeadData(familyDetails, familyIds);
        
        return familyDetails;
    }
    
    /**
     * Validates that the returned family details contain only family head information
     * and logs any discrepancies for monitoring
     */
    private void validateFamilyHeadData(List<FamilyDetailsDTO> familyDetails, List<UUID> requestedFamilyIds) {
        Set<UUID> returnedFamilyIds = familyDetails.stream()
            .map(FamilyDetailsDTO::getFamilyId)
            .collect(Collectors.toSet());
        
        // Log if some families are missing from the response
        Set<UUID> missingFamilies = new HashSet<>(requestedFamilyIds);
        missingFamilies.removeAll(returnedFamilyIds);
        
        if (!missingFamilies.isEmpty()) {
            log.warn("Family head data not found for {} families: {}", 
                    missingFamilies.size(), missingFamilies);
        }
        
        // Ensure each family has exactly one entry (family head only)
        if (familyDetails.size() != returnedFamilyIds.size()) {
            log.error("Duplicate family entries detected! Expected {} unique families, got {} entries", 
                     returnedFamilyIds.size(), familyDetails.size());
        } else {
            log.debug("Validation passed: {} unique family head records returned", familyDetails.size());
        }
    }

    @Override
    @Transactional
    public ThedalResponse<Void> updateAssignedFamilies(Long electionId, Long userId, FamilyUpdateRequest familyUpdateRequest) {
        try {
            Long accountId = requestDetails.getCurrentAccountId();
            if (accountId == null) {
                throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
            }

            FamilyCaptainEntity familyCaptain = familyCaptainRepository
                .findByUserEntity_IdAndElectionEntity_IdAndAccountId(userId, electionId, accountId)
                .orElseThrow(() -> new ThedalException(ThedalError.VOLUNTEER_NOT_FOUND, HttpStatus.NOT_FOUND));

            // Validate assigned families exist
            if (familyUpdateRequest.getAssignedFamilies() != null && !familyUpdateRequest.getAssignedFamilies().isEmpty()) {
                validateFamiliesExist(familyUpdateRequest.getAssignedFamilies(), electionId, accountId);
            }

            // Update assigned families
            familyCaptain.setAssignedFamilies(familyUpdateRequest.getAssignedFamilies());
            familyCaptainRepository.save(familyCaptain);

            log.info("Updated assigned families for family captain {} in election {}", userId, electionId);
            return new ThedalResponse<Void>(ThedalSuccess.SUCCESS, null);

        } catch (ThedalException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error updating assigned families: {}", e.getMessage(), e);
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ThedalResponse<Page<FamilyCaptainDetailsDTO>> getFamilyCaptainsByAssignedFamiliesAndMobileNumber(
            Long electionId, List<UUID> assignedFamilies, String mobileNumber, String searchTerm, 
            int page, int size, String sortBy, String direction) {
        try {
            Long accountId = requestDetails.getCurrentAccountId();
            if (accountId == null) {
                throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
            }

            // Create sort object
            Sort.Direction sortDirection = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
            Sort sort = Sort.by(sortDirection, sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);

            // Get family captains with filters
            Page<FamilyCaptainEntity> familyCaptainsPage = familyCaptainRepository.findWithFilters(
                electionId, accountId, assignedFamilies, mobileNumber, searchTerm, pageable);

            // Convert to DTOs
            Page<FamilyCaptainDetailsDTO> dtoPage = familyCaptainsPage.map(this::mapEntityToDto);

            return new ThedalResponse<Page<FamilyCaptainDetailsDTO>>(ThedalSuccess.SUCCESS, dtoPage);

        } catch (ThedalException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error retrieving family captains: {}", e.getMessage(), e);
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ThedalResponse<List<FamilyCaptainDetailsDTO>> getFamilyCaptainsByAssignedFamily(UUID familyId, Long electionId) {
        try {
            Long accountId = requestDetails.getCurrentAccountId();
            if (accountId == null) {
                throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
            }

            List<FamilyCaptainEntity> familyCaptains = familyCaptainRepository.findByAssignedFamilyId(familyId, electionId, accountId);
            List<FamilyCaptainDetailsDTO> dtos = familyCaptains.stream()
                .map(this::mapEntityToDto)
                .collect(Collectors.toList());

            return new ThedalResponse<List<FamilyCaptainDetailsDTO>>(ThedalSuccess.SUCCESS, dtos);

        } catch (ThedalException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error retrieving family captains by family: {}", e.getMessage(), e);
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    @Transactional
    public ThedalResponse<FamilyCaptainUploadSummary> uploadFamilyCaptainsFromXlsxOrCsv(MultipartFile file, Long electionId) {
        try {
            Long accountId = requestDetails.getCurrentAccountId();
            if (accountId == null) {
                throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
            }

            // Validate file format and size
            if (file == null || file.isEmpty()) {
                throw new ThedalException(ThedalError.INVALID_FILE_FORMAT, HttpStatus.BAD_REQUEST);
            }

            String fileName = file.getOriginalFilename();
            if (!isSupportedFormat(fileName)) {
                throw new ThedalException(ThedalError.INVALID_FILE_FORMAT, HttpStatus.BAD_REQUEST, "Only .xlsx and .csv files are supported");
            }

            // Validate election exists
            ElectionEntity election = electionRepository.findByIdAndAccountId(electionId, accountId)
                .orElseThrow(() -> new ThedalException(ThedalError.INVALID_ELECTION, HttpStatus.NOT_FOUND));

            // Define mandatory headers for family captain bulk upload
            Set<String> mandatoryHeaders = Set.of("first_name", "mobile_number", "email", "password");

            // Validate headers and extract data
            Map<String, Integer> headerMapping = validateHeaders(file, fileName, mandatoryHeaders);
            
            // Process the file and return summary
            FamilyCaptainUploadSummary summary = processFile(file, fileName, headerMapping, electionId, accountId);
            summary.setFileName(fileName);

            log.info("Family captain bulk upload completed for election {} - {} successful, {} failed", 
                electionId, summary.getSuccessfulUploads(), summary.getFailedUploads());
            
            return new ThedalResponse<FamilyCaptainUploadSummary>(ThedalSuccess.SUCCESS, summary);

        } catch (ThedalException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error in family captain bulk upload: {}", e.getMessage(), e);
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private boolean isSupportedFormat(String fileName) {
        return fileName != null && (fileName.toLowerCase().endsWith(".xlsx") || fileName.toLowerCase().endsWith(".csv"));
    }

    private Map<String, Integer> validateHeaders(MultipartFile file, String fileName, Set<String> mandatoryHeaders) throws IOException {
        Map<String, Integer> headerMapping;

        if (fileName.toLowerCase().endsWith(".xlsx")) {
            try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
                Sheet sheet = workbook.getSheetAt(0);
                if (sheet == null || sheet.getRow(0) == null) {
                    throw new ThedalException(ThedalError.INVALID_FILE_DATA, HttpStatus.BAD_REQUEST, "File is empty or header row is missing");
                }
                headerMapping = buildHeaderMapping(sheet.getRow(0));
            }
        } else { // CSV
            try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
                String headerLine = br.readLine();
                if (headerLine == null || headerLine.trim().isEmpty()) {
                    throw new ThedalException(ThedalError.INVALID_FILE_DATA, HttpStatus.BAD_REQUEST, "File is empty or header row is missing");
                }
                String[] headers = headerLine.split(",");
                headerMapping = buildCsvHeaderMapping(headers);
            }
        }

        // Check for missing mandatory headers
        List<String> missingHeaders = mandatoryHeaders.stream()
                .filter(header -> !headerMapping.containsKey(header))
                .collect(Collectors.toList());

        if (!missingHeaders.isEmpty()) {
            throw new ThedalException(ThedalError.MANDATORY_FIELDS_MISSING, HttpStatus.BAD_REQUEST,
                    "Missing mandatory headers: " + String.join(", ", missingHeaders));
        }

        return headerMapping;
    }

    private Map<String, Integer> buildHeaderMapping(Row headerRow) {
        Map<String, Integer> headerMapping = new HashMap<>();
        for (Cell cell : headerRow) {
            if (cell.getCellType() == CellType.STRING) {
                String normalizedHeader = normalizeHeader(cell.getStringCellValue());
                headerMapping.put(normalizedHeader, cell.getColumnIndex());
            }
        }
        return headerMapping;
    }

    private Map<String, Integer> buildCsvHeaderMapping(String[] headers) {
        Map<String, Integer> headerMapping = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            String normalizedHeader = normalizeHeader(headers[i]);
            headerMapping.put(normalizedHeader, i);
        }
        return headerMapping;
    }

    private String normalizeHeader(String header) {
        return header.trim().toLowerCase().replaceAll("[^a-z0-9]", "_");
    }

    private FamilyCaptainUploadSummary processFile(MultipartFile file, String fileName, Map<String, Integer> headerMapping, 
                                                  Long electionId, Long accountId) throws IOException {
        FamilyCaptainUploadSummary summary = new FamilyCaptainUploadSummary();
        int totalRows = 0;
        int successfulUploads = 0;
        int failedUploads = 0;
        List<String> errorDetails = new ArrayList<>();

        if (fileName.toLowerCase().endsWith(".xlsx")) {
            try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
                Sheet sheet = workbook.getSheetAt(0);
                totalRows = sheet.getPhysicalNumberOfRows() - 1; // Exclude header row
                
                for (int i = 1; i <= sheet.getLastRowNum(); i++) { // Skip header row
                    Row row = sheet.getRow(i);
                    if (row == null) continue;
                    
                    try {
                        Map<String, String> rowData = extractRowDataFromExcel(row, headerMapping);
                        createFamilyCaptain(rowData, electionId, accountId);
                        successfulUploads++;
                    } catch (Exception e) {
                        failedUploads++;
                        errorDetails.add("Row " + (i + 1) + ": " + e.getMessage());
                        log.warn("Failed to process row {}: {}", i + 1, e.getMessage());
                    }
                }
            }
        } else { // CSV
            try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
                String line;
                int rowNumber = 0;
                br.readLine(); // Skip header row
                
                while ((line = br.readLine()) != null) {
                    rowNumber++;
                    if (line.trim().isEmpty()) continue;
                    totalRows++;
                    
                    try {
                        String[] values = line.split(",");
                        Map<String, String> rowData = extractRowDataFromCsv(values, headerMapping);
                        createFamilyCaptain(rowData, electionId, accountId);
                        successfulUploads++;
                    } catch (Exception e) {
                        failedUploads++;
                        errorDetails.add("Row " + (rowNumber + 1) + ": " + e.getMessage());
                        log.warn("Failed to process row {}: {}", rowNumber + 1, e.getMessage());
                    }
                }
            }
        }

        summary.setTotalRows(totalRows);
        summary.setSuccessfulUploads(successfulUploads);
        summary.setFailedUploads(failedUploads);
        
        if (!errorDetails.isEmpty() && errorDetails.size() <= 10) {
            summary.setErrorDetails(String.join("; ", errorDetails));
        } else if (errorDetails.size() > 10) {
            List<String> firstTenErrors = errorDetails.subList(0, 10);
            summary.setErrorDetails(String.join("; ", firstTenErrors) + "; ... and " + (errorDetails.size() - 10) + " more errors");
        }

        return summary;
    }

    private Map<String, String> extractRowDataFromExcel(Row row, Map<String, Integer> headerMapping) {
        Map<String, String> rowData = new HashMap<>();
        
        for (Map.Entry<String, Integer> entry : headerMapping.entrySet()) {
            String header = entry.getKey();
            int columnIndex = entry.getValue();
            
            Cell cell = row.getCell(columnIndex);
            String value = "";
            
            if (cell != null) {
                switch (cell.getCellType()) {
                    case STRING:
                        value = cell.getStringCellValue().trim();
                        break;
                    case NUMERIC:
                        // Handle numeric values (especially for mobile numbers)
                        if (header.equals("mobile_number")) {
                            value = String.valueOf((long) cell.getNumericCellValue());
                        } else {
                            value = String.valueOf(cell.getNumericCellValue());
                        }
                        break;
                    case BOOLEAN:
                        value = String.valueOf(cell.getBooleanCellValue());
                        break;
                    default:
                        value = "";
                }
            }
            
            rowData.put(header, value);
        }
        
        return rowData;
    }

    private Map<String, String> extractRowDataFromCsv(String[] values, Map<String, Integer> headerMapping) {
        Map<String, String> rowData = new HashMap<>();
        
        for (Map.Entry<String, Integer> entry : headerMapping.entrySet()) {
            String header = entry.getKey();
            int columnIndex = entry.getValue();
            
            String value = "";
            if (columnIndex < values.length) {
                value = values[columnIndex].trim().replaceAll("\"", ""); // Remove quotes if any
            }
            
            rowData.put(header, value);
        }
        
        return rowData;
    }

    private void createFamilyCaptain(Map<String, String> rowData, Long electionId, Long accountId) {
        // Validate required fields
        validateRowData(rowData);
        
        String firstName = rowData.get("first_name");
        String lastName = rowData.getOrDefault("last_name", "");
        String email = rowData.get("email");
        String mobileNumber = rowData.get("mobile_number");
        String password = rowData.get("password");
        String gender = rowData.getOrDefault("gender", "");
        String whatsAppNumber = rowData.getOrDefault("whats_app_number", "");
        String status = rowData.getOrDefault("status", "active");
        String remarks = rowData.getOrDefault("remarks", "");
        
        // Parse assigned families if provided
        List<UUID> assignedFamilies = new ArrayList<>();
        String assignedFamiliesStr = rowData.getOrDefault("assigned_families", "");
        if (!assignedFamiliesStr.isEmpty()) {
            try {
                String[] familyIds = assignedFamiliesStr.split(";");
                for (String familyId : familyIds) {
                    if (!familyId.trim().isEmpty()) {
                        assignedFamilies.add(UUID.fromString(familyId.trim()));
                    }
                }
            } catch (IllegalArgumentException e) {
                log.warn("Invalid UUID format in assigned families: {}", assignedFamiliesStr);
            }
        }

        // Check if mobile number already exists
        Optional<UserEntity> existingUser = userRepository.findByMobileNumber(mobileNumber);
        if (existingUser.isPresent()) {
            throw new RuntimeException("Mobile number " + mobileNumber + " already exists");
        }

        // Check if email already exists
        if (email != null && !email.isEmpty()) {
            UserEntity existingEmailUser = userRepository.findByEmail(email);
            if (existingEmailUser != null) {
                throw new RuntimeException("Email " + email + " already exists");
            }
        }

        // Create UserEntity (following same pattern as manual creation - no role assignment)
        UserEntity user = new UserEntity();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setMobileNumber(mobileNumber);
        user.setPassword(new BCryptPasswordEncoder().encode(password));
        user.setAccountId(accountId);
        user.setIsEmailVerified(true);
        user.setIsMobileVerified(true);
        user.setIsActive("active".equalsIgnoreCase(status));
        user.setPasswordVersion(1);
        user.setCreatedAt(LocalDateTime.now());
        user.setCreatedBy("bulk_upload");
        user.setAccountEntity(accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found")));

        // Save user to PostgreSQL and MongoDB
        UserEntity savedUser;
        try {
            savedUser = userRepository.save(user);
            try {
                MongoUser mongoUser = new MongoUser(savedUser);
                mongoUserRepository.save(mongoUser);
            } catch (Exception mongoEx) {
                log.error("Failed to save user to MongoDB: {}", mongoEx.getMessage());
                throw new RuntimeException("MongoDB save failed, triggering rollback", mongoEx);
            }
        } catch (Exception ex) {
            throw new RuntimeException("Failed to create user: " + ex.getMessage());
        }

        // Create FamilyCaptainEntity
        FamilyCaptainEntity familyCaptain = new FamilyCaptainEntity();
        familyCaptain.setFirstName(firstName);
        familyCaptain.setLastName(lastName);
        familyCaptain.setEmail(email);
        familyCaptain.setMobileNumber(mobileNumber);
        familyCaptain.setWhatsAppNumber(whatsAppNumber.isEmpty() ? mobileNumber : whatsAppNumber);
        familyCaptain.setGender(gender);
        familyCaptain.setStatus(status);
        familyCaptain.setRemarks(remarks);
        familyCaptain.setAssignedFamilies(assignedFamilies);
        familyCaptain.setAccountId(accountId);
        familyCaptain.setUserEntity(savedUser);
        familyCaptain.setElectionEntity(electionRepository.findById(electionId)
                .orElseThrow(() -> new RuntimeException("Election not found")));

        // Set address if provided
        Address address = new Address();
        String street = rowData.getOrDefault("street", "");
        String city = rowData.getOrDefault("city", "");
        String state = rowData.getOrDefault("state", "");
        String postalCode = rowData.getOrDefault("postal_code", "");
        String country = rowData.getOrDefault("country", "");
        
        if (!street.isEmpty() || !city.isEmpty() || !state.isEmpty() || !postalCode.isEmpty() || !country.isEmpty()) {
            address.setStreet(street);
            address.setCity(city);
            address.setState(state);
            address.setPostalCode(postalCode);
            address.setCountry(country);
            familyCaptain.setFamilyCaptainAddress(address);
        }

        // Save Family Captain
        try {
            familyCaptainRepository.save(familyCaptain);
            log.info("Successfully created family captain: {} {} (Mobile: {})", firstName, lastName, mobileNumber);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to save family captain: " + ex.getMessage());
        }
    }

    private void validateRowData(Map<String, String> rowData) {
        String firstName = rowData.get("first_name");
        String mobileNumber = rowData.get("mobile_number");
        String email = rowData.get("email");
        String password = rowData.get("password");

        if (firstName == null || firstName.trim().isEmpty()) {
            throw new RuntimeException("First name is required");
        }

        if (mobileNumber == null || mobileNumber.trim().isEmpty()) {
            throw new RuntimeException("Mobile number is required");
        }

        if (!mobileNumber.matches("\\d{10}")) {
            throw new RuntimeException("Mobile number must be exactly 10 digits");
        }

        if (email == null || email.trim().isEmpty()) {
            throw new RuntimeException("Email is required");
        }

        if (!email.matches("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$")) {
            throw new RuntimeException("Invalid email format");
        }

        if (password == null || password.trim().isEmpty()) {
            throw new RuntimeException("Password is required");
        }

        if (password.length() < 8) {
            throw new RuntimeException("Password must be at least 8 characters long");
        }
    }

    @Override
    public ThedalResponse<Page<FamilyDetailsDTO>> getFamilyOptions(Long electionId, String searchTerm, int page, int size) {
        try {
            Long accountId = requestDetails.getCurrentAccountId();
            if (accountId == null) {
                throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
            }

            // Validate election exists
            ElectionEntity election = electionRepository.findByIdAndAccountId(electionId, accountId)
                .orElseThrow(() -> new ThedalException(ThedalError.INVALID_ELECTION, HttpStatus.NOT_FOUND));

            Pageable pageable = PageRequest.of(page, size, Sort.by("familyNo").ascending());
            
            // Get families with search capability
            List<Object[]> familyData = voterRepository.findFamilyOptionsForDropdown(accountId, electionId, searchTerm);
            
            List<FamilyDetailsDTO> familyDTOs = familyData.stream()
                .map(data -> {
                    FamilyDetailsDTO dto = new FamilyDetailsDTO();
                    dto.setFamilyId((UUID) data[0]);
                    dto.setFamilyNo(((Number) data[1]).longValue());
                    dto.setHeadName((String) data[2]);
                    dto.setDisplayText("Family " + data[1] + " - " + data[2]);
                    return dto;
                })
                .collect(Collectors.toList());
            
            // Apply pagination manually since we're using custom query
            int start = page * size;
            int end = Math.min(start + size, familyDTOs.size());
            List<FamilyDetailsDTO> pageContent = start < familyDTOs.size() ? 
                familyDTOs.subList(start, end) : new ArrayList<>();
            
            Page<FamilyDetailsDTO> familyPage = new PageImpl<>(pageContent, pageable, familyDTOs.size());
            
            log.info("Retrieved {} family options for election {}", familyDTOs.size(), electionId);
            return new ThedalResponse<Page<FamilyDetailsDTO>>(ThedalSuccess.SUCCESS, familyPage);
            
        } catch (ThedalException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error retrieving family options for election {}: {}", electionId, e.getMessage(), e);
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
