package com.thedal.thedal_app.volunteer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.thedal.thedal_app.account.AccountEntity;
import com.thedal.thedal_app.account.AccountService;
import com.thedal.thedal_app.election.BoothService;
import com.thedal.thedal_app.election.ElectionBoothRepository;
import com.thedal.thedal_app.election.ElectionEntity;
import com.thedal.thedal_app.election.ElectionRepository;
import com.thedal.thedal_app.election.PartManager;
import com.thedal.thedal_app.election.PartManagerRepository;
import com.thedal.thedal_app.role.Role;
import com.thedal.thedal_app.role.RoleRepo;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;
import com.thedal.thedal_app.user.UserEntity;
import com.thedal.thedal_app.user.UserRepo;
import com.thedal.thedal_app.volunteer.dto.VolunteerUploadSummary;
import com.thedal.thedal_app.voter.Address;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class VounteerFileUploadService {
	
private static final int BATCH_SIZE = 1000;
	
	private final ExecutorService executorService = Executors.newFixedThreadPool(8);
	private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
	private final VolunteerRepository volunteerRepository;
	
	public VounteerFileUploadService(VolunteerRepository volunteerRepository) {
		this.volunteerRepository = volunteerRepository;
	}

	@Autowired
	private RoleRepo roleRepo;

	@Autowired
	private UserRepo userRepo;
	@Autowired
	private ElectionRepository electionRepository;
	@Autowired
	private VolunteerElectionBoothRepo volunteerElectionBoothRepo;
	@Autowired
    private ElectionBoothRepository electionBoothRepository;
    @Autowired
    private PartManagerRepository partManagerRepository;
@Autowired
private BoothService boothService;

//	@Autowired
//	private AccountRepository accountRepository;
	
	@Autowired
	private AccountService accountService;

    private static final Long VOLUNTEER=4L;


    @Transactional
    public void processExcelFileAsync(Long bulkUploadId, AccountEntity account, String fileUrl, ElectionEntity election, 
                                      VolunteerBulkUploadEntity bulkUploadEntity, VolunteerUploadSummary summary, Long adminUserId) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(new URL(fileUrl).openStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Map<String, Integer> headerMapping = buildHeaderMapping(sheet.getRow(0));
            processFileAsync(sheet.iterator(), headerMapping, account, true, election, bulkUploadEntity, summary, adminUserId);
        }
    }
    
    
    /**
     * @param br The BufferedReader for the uploaded CSV file.
     */ 
    @Transactional
    public void processCsvFileAsync(Long bulkUploadId, AccountEntity account, String fileUrl, ElectionEntity election, 
                                    VolunteerBulkUploadEntity bulkUploadEntity, VolunteerUploadSummary summary, Long adminUserId) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new URL(fileUrl).openStream()))) {
            String[] headers = br.readLine().split(",");
            Map<String, Integer> headerMapping = buildCsvHeaderMapping(headers);
            processFileAsync(br.lines().iterator(), headerMapping, account, false, election, bulkUploadEntity, summary, adminUserId);
        }
    }
    

    
    /**
     * 
     * @param dataIterator Iterator over the rows of the file (either Excel or CSV).
     * @param headerMapping A map containing the header names and their column indices.
     * @param isExcel True if the file is Excel, false if it is CSV.
     */
//    @Async
//    @Transactional
//    private void processFileAsync(Iterator<?> dataIterator, Map<String, Integer> headerMapping, AccountEntity account, boolean isExcel, 
//                                  ElectionEntity election, VolunteerBulkUploadEntity bulkUploadEntity, VolunteerUploadSummary summary) throws IOException {
//        long startTime = System.currentTimeMillis();
//        List<VolunteerEntity> volunteerEntities = new ArrayList<>();
//        List<UserEntity> userEntities = new ArrayList<>();
//        int rowNumber = isExcel ? 1 : 0;
//
//        if (isExcel) {
//            dataIterator.next(); // Skip header row for Excel
//            rowNumber++;
//        }
//
//        while (dataIterator.hasNext()) {
//            Object next = dataIterator.next();
//        //    String rawData = isExcel ? rowToString((Row) next) : (String) next;
//            VolunteerEntity volunteerEntity = isExcel
//                    ? mapToVolunteerEntityDynamic((Row) next, headerMapping, account, election, summary) // Pass summary here
//                    : mapToVolunteerFromCsv(((String) next).split(","), headerMapping, account, election); // Update CSV method too if needed
//
//            if (volunteerEntity == null) {
//                // Error already logged and added to summary in mapToVolunteerEntityDynamic
//            } else {
//                volunteerEntity.setElectionEntity(election);
//                UserEntity userEntity = volunteerEntity.getUserEntity();
//                userEntities.add(userEntity);
//                volunteerEntities.add(volunteerEntity);
//                summary.incrementSuccess();
//            }
//
//            rowNumber++;
//
//            if (volunteerEntities.size() >= BATCH_SIZE) {
//                saveVolunteersInBatchAsync(volunteerEntities, userEntities);
//                userEntities.clear();
//                volunteerEntities.clear();
//            }
//        }
//
//        if (!volunteerEntities.isEmpty()) {
//            saveVolunteersInBatchAsync(volunteerEntities, userEntities);
//        }
//
//        long endTime = System.currentTimeMillis();
//        long totalTimeTaken = endTime - startTime;
//        log.info("Start time: {} | End time: {} | Total time taken: {} ms", startTime, endTime, totalTimeTaken);
//
//        bulkUploadEntity.setTotalRecords(summary.getTotalRecords());
//        bulkUploadEntity.setTotalTimeTaken(totalTimeTaken);
//        volunteerElectionBoothRepo.save(bulkUploadEntity);
//    }
//
//    // Helper method to convert Excel row to string
//    private String rowToString(Row row) {
//        StringBuilder sb = new StringBuilder();
//        for (int i = 0; i < row.getLastCellNum(); i++) {
//            sb.append(getCellValue(row, i)).append(",");
//        }
//        return sb.length() > 0 ? sb.substring(0, sb.length() - 1) : "";
//    }
    @Async
    @Transactional
    private void processFileAsync(Iterator<?> dataIterator, Map<String, Integer> headerMapping, 
                                AccountEntity account, boolean isExcel, ElectionEntity election, 
                                VolunteerBulkUploadEntity bulkUploadEntity, VolunteerUploadSummary summary, Long adminUserId) 
                                throws IOException {
        long startTime = System.currentTimeMillis();
        List<VolunteerEntity> volunteerEntities = new ArrayList<>();
        List<UserEntity> userEntities = new ArrayList<>();
        
        // Initialize row counter
        int rowNumber = 0;
        
        if (isExcel) {
            dataIterator.next(); // Skip header row
            rowNumber = 1; // First data row is row 1 (Excel row 2)
        }

        while (dataIterator.hasNext()) {
            Object next = dataIterator.next();
            rowNumber++; // Increment before processing
            
            try {
                VolunteerEntity volunteerEntity = isExcel
                        ? mapToVolunteerEntityDynamic((Row) next, headerMapping, account, election, summary, adminUserId)
                        : mapToVolunteerFromCsv(((String) next).split(","), headerMapping, account, election, adminUserId);

                if (volunteerEntity == null) {
                    // Error already logged in mapping method
                } else {
                    volunteerEntity.setElectionEntity(election);
                    userEntities.add(volunteerEntity.getUserEntity());
                    volunteerEntities.add(volunteerEntity);
                    summary.incrementSuccess();
                }

                if (volunteerEntities.size() >= BATCH_SIZE) {
                    saveVolunteersInBatchAsync(volunteerEntities, userEntities);
                    userEntities.clear();
                    volunteerEntities.clear();
                }
            } catch (Exception e) {
                String rawData = isExcel ? rowToString((Row) next) : (String) next;
                log.error("Error processing row {}: {}", rowNumber, e.getMessage());
                summary.incrementFailed(rowNumber, "Processing error: " + e.getMessage(), rawData);
            }
        }

        if (!volunteerEntities.isEmpty()) {
            saveVolunteersInBatchAsync(volunteerEntities, userEntities);
        }

        long endTime = System.currentTimeMillis();
        long totalTimeTaken = endTime - startTime;
        log.info("Time taken to process file: {} ms", totalTimeTaken);

        bulkUploadEntity.setTotalRecords(summary.getTotalRecords());
        bulkUploadEntity.setTotalTimeTaken(totalTimeTaken);
        volunteerElectionBoothRepo.save(bulkUploadEntity);
    }

    private String rowToString(Row row) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < row.getLastCellNum(); i++) {
            String value = getCellValue(row, i);
            sb.append(value != null ? value : "null").append(",");
        }
        return sb.length() > 0 ? sb.substring(0, sb.length() - 1) : "";
    }
    
    /**
     * Retrieves the value of a cell in a row. Handles both String and numeric types, including date values.
     * 
     * @param row       The row from which to retrieve the cell value.
     * @param cellIndex The index of the cell in the row.
     * @return The value of the cell as a String, or null if the cell is empty or of an unsupported type.
     */
      private String getCellValue(Row row, int cellIndex) {
          Cell cell = row.getCell(cellIndex);
          if (cell == null) {
              return null;
          }
          switch (cell.getCellType()) {
              case STRING:
                  return cell.getStringCellValue();
              case NUMERIC:
                  if (DateUtil.isCellDateFormatted(cell)) {
                      return new SimpleDateFormat("yyyy-MM-dd").format(cell.getDateCellValue());
                  } else {
                    //   return String.valueOf((long) cell.getNumericCellValue());
                    return BigDecimal.valueOf(cell.getNumericCellValue()).toPlainString();

                  }
              default:
                  return null;
          }
      }
      
      /**
       * 
       * @param volunteerEntities List of voter entities to save.
       * @return 
       */
    @Transactional
    private void saveVolunteersInBatchAsync(List<VolunteerEntity> volunteerEntities , List<UserEntity> userEntities) {
    	
    	userRepo.saveAll(userEntities);
        volunteerRepository.saveAll(volunteerEntities);

    }
    
    /**
     * Builds a header mapping for an Excel file. Maps header names to their column indices.
     * 
     * @param headerRow The row containing the headers.
     * @return A map of header names to column indices.
     */
    private Map<String, Integer> buildHeaderMapping(Row headerRow) {
        Map<String, Integer> headerMapping = new HashMap<>();
        
        for (Cell cell : headerRow) {
            String normalizedHeader = normalizeHeader(cell.getStringCellValue());
            headerMapping.put(normalizedHeader, cell.getColumnIndex());
        }

        return headerMapping;
    }
    
    
    /**
     * Builds a header mapping for a CSV file. Maps header names to their column indices.
     * 
     * @param headers The array of header names from the CSV file.
     * @return A map of header names to column indices.
     */
    private Map<String, Integer> buildCsvHeaderMapping(String[] headers) {
        Map<String, Integer> headerMapping = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            String normalizedHeader = normalizeHeader(headers[i]);
            headerMapping.put(normalizedHeader, i);
        }
        
        return headerMapping;
    }
    
    /**
     * Normalizes a header name by trimming whitespace, replacing special characters with underscores, and converting to lowercase.    
     * 
     * @param header The header name to normalize.
     * @return The normalized header name.
     */
    private String normalizeHeader(String header) {
    	//return header.trim().replaceAll("\\s+", "_").toLowerCase();
    	if (header == null) {
            return ""; 
        }
        
        String normalized = header.trim()
                                 .replaceAll("[^a-zA-Z0-9]", "_")
                                 .replaceAll("_+", "_")
                                 .toLowerCase();
                                 
        log.debug("Normalized header: {} -> {}", header, normalized);
        return normalized;
    
    
    }
    
    
    private List<Long> parseAssignedBooth(String assignedBoothStr) {
        List<Long> assignedBooths = new ArrayList<>();
        if (assignedBoothStr != null && !assignedBoothStr.isEmpty()) {
            // Assuming booths are separated by commas (or space)
            String[] boothArray = assignedBoothStr.split(",");  // Modify this if the separator is different
            for (String booth : boothArray) {
                 try {
                    String boothTrimmed = booth.trim();

                    // If booth contains a decimal, remove the decimal part
                    if (boothTrimmed.contains(".")) {
                        // Remove the decimal part using BigDecimal to ensure precision
                        boothTrimmed = new BigDecimal(boothTrimmed).toBigInteger().toString();
                    }

                    // Log the booth value after trimming and converting
                    log.debug("Booth Trimmed: {}", boothTrimmed);

                    // Parse the boothTrimmed as a Long
                    assignedBooths.add(Long.parseLong(boothTrimmed));
                } catch (NumberFormatException e) {
                    // Handle invalid number format if necessary, maybe log or throw a custom exception
                    log.error("Invalid assigned booth value: {}", booth);
                }
            }
        }
        return assignedBooths;
    }
    // private List<Long> parseAssignedBooth(String assignedBoothStr) {
    //     List<Long> assignedBooths = new ArrayList<>();
    //     if (assignedBoothStr != null && !assignedBoothStr.isEmpty()) {
    //         String[] boothArray = assignedBoothStr.split(",");  // Split by commas if booths are separated by commas
    //         for (String booth : boothArray) {
    //             try {
    //                 // Strip any potential decimal part by trimming and parsing the number
    //                 String boothTrimmed = booth.trim();
                    
    //                 // If there's a decimal part, remove it and parse the integer value
    //                 if (boothTrimmed.contains(".")) {
    //                     boothTrimmed = boothTrimmed.split("\\.")[0]; // Remove the decimal part
    //                 }
    
    //                 // Now parse it as a Long integer (since booth numbers are large, Long is more appropriate)
    //                 assignedBooths.add(Long.parseLong(boothTrimmed));
    //             } catch (NumberFormatException e) {
    //                 log.error("Invalid assigned booth value: {}", booth);
    //             }
    //         }
    //     }
    //     return assignedBooths;
    // }
    
    
    /**
     * Maps a row from an Excel file to a VolunteerEntity based on the header mapping.
     * 
     * @param row            The row to map.
     * @param headerMapping  The header mapping with column indices.
     * @param accountId      The account ID to associate with the VoterEntity.
     * @return A VolunteerEntity object populated with data from the row.
     */
    private VolunteerEntity mapToVolunteerEntityDynamic(Row row, Map<String, Integer> headerMapping, AccountEntity account, 
            ElectionEntity election, VolunteerUploadSummary summary, Long adminUserId) {

    	// Check if row is empty
        boolean isEmptyRow = true;
        for (Cell cell : row) {
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                isEmptyRow = false;
                break;
            }
        }
        if (isEmptyRow) {
            return null; // Skip empty rows
        }      
    	
    	VolunteerEntity volunteerEntity = new VolunteerEntity();
        UserEntity user = new UserEntity();
        StringBuilder errorReason = new StringBuilder();
        boolean skipRow = false;

        Integer roleNameIndex = headerMapping.get("role_name");
        if (roleNameIndex == null || getCellValue(row, roleNameIndex) == null || getCellValue(row, roleNameIndex).isEmpty()) {
            errorReason.append("Role name is missing or invalid; ");
            skipRow = true;
        } else {
            String roleName = getCellValue(row, roleNameIndex);
            Role userRole = roleRepo.findByRoleNameAndAccountId(roleName, account.getId()).orElse(null);
            if (userRole == null) {
                errorReason.append("Invalid role name: ").append(roleName).append("; ");
                skipRow = true;
            } else {
                volunteerEntity.setRoleId(userRole.getId());
            }
        }

        Integer mobileNumberIndex = headerMapping.get("mobile_number");
        String mobileNumber = (mobileNumberIndex != null) ? getCellValue(row, mobileNumberIndex) : null;
        if (mobileNumber == null || mobileNumber.isEmpty()) {
            errorReason.append("Mobile number is missing; ");
            skipRow = true;
        } else if (!isValidMobileNumber(mobileNumber)) {
            errorReason.append("Invalid mobile number format: ").append(mobileNumber).append("; ");
            skipRow = true;
        } else if (userRepo.existsByMobileNumber(mobileNumber)) {
            errorReason.append("Duplicate mobile number: ").append(mobileNumber).append("; ");
            skipRow = true;
        } else {
            user.setMobileNumber(mobileNumber);
        }

//        Integer emailIndex = headerMapping.get("email");
//        String email = (emailIndex != null) ? getCellValue(row, emailIndex) : null;
//        if (email == null || email.isEmpty()) {
//            errorReason.append("Email is missing; ");
//            skipRow = true;
//        } else if (!isValidEmail(email)) {
//            errorReason.append("Invalid email format: ").append(email).append("; ");
//            skipRow = true;
//        } else if (userRepo.existsByEmail(email)) {
//            errorReason.append("Duplicate email: ").append(email).append("; ");
//            skipRow = true;
//        } else {
//            user.setEmail(email);
//        }
///////////////////////////
     // Validate email and check for duplicates
//        Integer emailIndex = headerMapping.get("email");
//        String email = (emailIndex != null) ? getCellValue(row, emailIndex) : null;
//        if (email != null && !email.isEmpty()) {
//            if (!EMAIL_PATTERN.matcher(email).matches()) {
//                errorReason.append("Invalid email format: ").append(email).append("; ");
//                skipRow = true;
//            } else if (userRepo.existsByEmail(email)) {
//                errorReason.append("Duplicate email: ").append(email).append("; ");
//                skipRow = true;
//            } else {
//                user.setEmail(email);
//            }
//        } else {
//            errorReason.append("Email is missing; ");
//            skipRow = true;
//        }
     // Email is optional, no validation or uniqueness check
        Integer emailIndex = headerMapping.get("email");
        String email = (emailIndex != null) ? getCellValue(row, emailIndex) : null;
        if (email != null && !email.isEmpty()) {
            user.setEmail(email); // Set email if provided, no checks
        }


        Integer assignedBoothColumn = headerMapping.get("assigned_booth");
        if (assignedBoothColumn == null || getCellValue(row, assignedBoothColumn) == null || getCellValue(row, assignedBoothColumn).isEmpty()) {
            errorReason.append("Assigned booth is missing; ");
            skipRow = true;
        } else {
            String assignedBoothStr = getCellValue(row, assignedBoothColumn);
            List<Long> assignedBooths = parseAssignedBooth(assignedBoothStr);
            if (assignedBooths.isEmpty()) {
                errorReason.append("Assigned booth is empty or invalid; ");
                skipRow = true;
            } else if (!isValidAssignedBooth(assignedBooths, election.getId(), account.getId())) {
                errorReason.append("Invalid assigned booth(s): ").append(assignedBoothStr).append("; ");
                skipRow = true;
            } else {
                volunteerEntity.setAssignedBooth(assignedBooths);
            }
        }

        if (skipRow) {
            String rawData = rowToString(row);
            log.warn("Skipping row {} due to: {}. Raw data: {}", row.getRowNum(), errorReason.toString(), rawData);
            summary.incrementFailed(row.getRowNum(), errorReason.toString(), rawData); // Add to summary
            return null;
        }

        // Set account and election details
        volunteerEntity.setAccountId(account.getId());
        volunteerEntity.setElectionEntity(election);
        volunteerEntity.setAdminUserId(adminUserId);

        // Map other fields (unchanged logic)
        Integer firstNameIndex = headerMapping.get("first_name");
        if (firstNameIndex != null && getCellValue(row, firstNameIndex) != null) {
            user.setFirstName(getCellValue(row, firstNameIndex));
        }

        Integer lastNameIndex = headerMapping.get("last_name");
        if (lastNameIndex != null && getCellValue(row, lastNameIndex) != null) {
            user.setLastName(getCellValue(row, lastNameIndex));
        }

        Address address = new Address();
        Integer streetIndex = headerMapping.get("street");
        if (streetIndex != null && getCellValue(row, streetIndex) != null) {
            address.setStreet(getCellValue(row, streetIndex));
        }
        Integer cityIndex = headerMapping.get("city");
        if (cityIndex != null && getCellValue(row, cityIndex) != null) {
            address.setCity(getCellValue(row, cityIndex));
        }
        Integer stateIndex = headerMapping.get("state");
        if (stateIndex != null && getCellValue(row, stateIndex) != null) {
            address.setState(getCellValue(row, stateIndex));
        }
        Integer postalCodeIndex = headerMapping.get("postal_code");
        if (postalCodeIndex != null && getCellValue(row, postalCodeIndex) != null) {
            address.setPostalCode(getCellValue(row, postalCodeIndex));
        }
        Integer countryIndex = headerMapping.get("country");
        if (countryIndex != null && getCellValue(row, countryIndex) != null) {
            address.setCountry(getCellValue(row, countryIndex));
        }
        volunteerEntity.setVolunteerAddress(address);

        Integer statusIndex = headerMapping.get("status");
        if (statusIndex != null && getCellValue(row, statusIndex) != null) {
            volunteerEntity.setStatus(getCellValue(row, statusIndex));
        }

        Integer remarksIndex = headerMapping.get("remarks");
        if (remarksIndex != null && getCellValue(row, remarksIndex) != null) {
            volunteerEntity.setRemarks(getCellValue(row, remarksIndex));
        }

        Integer whatsAppIndex = headerMapping.get("whats_app_number");
        if (whatsAppIndex != null && getCellValue(row, whatsAppIndex) != null) {
            volunteerEntity.setWhatsAppNumber(getCellValue(row, whatsAppIndex));
        }

        Integer genderIndex = headerMapping.get("gender");
        if (genderIndex != null && getCellValue(row, genderIndex) != null) {
            volunteerEntity.setGender(getCellValue(row, genderIndex));
        }

        Integer passwordIndex = headerMapping.get("password");
        if (passwordIndex != null && getCellValue(row, passwordIndex) != null) {
            user.setPassword(new BCryptPasswordEncoder().encode(getCellValue(row, passwordIndex)));
        }

        user.setRole(roleRepo.findById(volunteerEntity.getRoleId()).orElseThrow(() -> new ThedalException(ThedalError.ROLE_NOT_FOUND, HttpStatus.BAD_REQUEST)));
        user.setIsEmailVerified(true);
        user.setIsMobileVerified(true);
        user.setIsActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setCreatedBy("volunteer create");
        user.setAccountEntity(account);
        volunteerEntity.setUserEntity(user);

        return volunteerEntity;
}
    
//    private boolean isValidAssignedBooth(List<Long> assignedBooths, Long electionId) {
//        // Validate assigned booth by checking if booth_number exists in election_booth table
//        for (Long booth : assignedBooths) {
//            if (!boothExistsInSystem(booth, electionId)) {
//                return false; // Invalid booth number for this election
//            }
//        }
//        return true;
//    }
//
//    private boolean boothExistsInSystem(Long boothNumber, Long electionId) {
//        // Check if booth exists in the election_booth table for the given election_id and booth_number
//        Optional<ElectionBooth> electionBooth = electionBoothRepository.findByBoothNumberAndElectionId(boothNumber, electionId);
//        return electionBooth.isPresent();
//    }
    private boolean isValidAssignedBooth(List<Long> assignedBooths, Long electionId, Long accountId) {
        for (Long booth : assignedBooths) {
            if (!boothExistsInSystem(booth, electionId, accountId)) {
                return false;
            }
        }
        return true;
    }

//    private boolean boothExistsInSystem(Long boothNumber, Long electionId, Long accountId) {
//        Optional<ElectionBooth> electionBooth = electionBoothRepository
//            .findByBoothNumberAndElectionIdAndAccountId(boothNumber, electionId, accountId);
//        return electionBooth.isPresent();
//    }
    private boolean boothExistsInSystem(Long boothNumber, Long electionId, Long accountId) {
        // Check if booth exists in part_manager table using booth partNo
        Optional<PartManager> partManager = partManagerRepository
            .findByPartNoAndElectionIdAndAccountId(String.valueOf(boothNumber), electionId, accountId);
        return partManager.isPresent();
    }

    private boolean isValidMobileNumber(String mobileNumber) {
        if (mobileNumber == null || mobileNumber.trim().isEmpty()) {
            return false;
        }
        // Example: Validates a 10-digit mobile number starting with 6, 7, 8, or 9
        String mobilePattern = "^[6-9]\\d{9}$";
        return mobileNumber.matches(mobilePattern);
    }
    
    private boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        // Basic email regex pattern
        String emailPattern = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        return email.matches(emailPattern);
    }
  


    // /**
    //  * Maps a row from a CSV file to a VolunteerEntity based on the header mapping.
    //  * 
    //  * @param fields         The array of field values from the CSV row.
    //  * @param headerMapping  The header mapping with column indices.
    //  * @param accountId      The account ID to associate with the VoterEntity.
    //  * @return A VoterEntity object populated with data from the CSV row.
    //  */
//     private VolunteerEntity mapToVolunteerFromCsv(String[] fields, Map<String, Integer> headerMapping, AccountEntity account,ElectionEntity election) {
//        try {
//     	   VolunteerEntity volunteerEntity = new VolunteerEntity();
//         //volunteerEntity.setAccountId(accountId);
//         volunteerEntity.setElectionEntity(election);
// //     	Role userRole = roleRepo.findById(VOLUNTEER).orElse(null);
// //        if (userRole == null)
// //        	throw new ThedalException(ThedalError.ROLE_NOT_FOUND, HttpStatus.BAD_REQUEST);
// //     // Map role_id field
// //        Integer roleIdIndex = headerMapping.get("role_id");
// //        if (roleIdIndex != null && fields[roleIdIndex] != null) {
// //            Long roleId = Long.parseLong(fields[roleIdIndex]);
// //            Role userRole = roleRepo.findById(roleId)
// //                .orElseThrow(() -> new ThedalException(ThedalError.ROLE_NOT_FOUND, HttpStatus.BAD_REQUEST));
// //            volunteerEntity.setRoleId(roleId);
// //        } else {
// //            log.warn("Role ID missing. Skipping this record.");
// //            return null;
// //        }
//         Integer roleNameIndex = headerMapping.get("role_name");
//         if (roleNameIndex != null && roleNameIndex < fields.length && fields[roleNameIndex] != null && !fields[roleNameIndex].isEmpty()) {
//             String roleName = fields[roleNameIndex];
//             Role userRole = roleRepo.findByRoleNameAndAccountId(roleName, account.getId())
//                 .orElseThrow(() -> {
//                     log.warn("Invalid or missing roleName: {} for accountId: {} in row. Skipping this record.", roleName, account.getId());
//                     return new ThedalException(ThedalError.ROLE_NOT_FOUND, HttpStatus.BAD_REQUEST);
//                 });
//             volunteerEntity.setRoleId(userRole.getId());
//         } else {
//             log.warn("Role name missing or invalid in CSV row. Skipping this record: {}", Arrays.toString(fields));
//             return null; 
//         }
        
//     	 UserEntity user = new UserEntity();

//         // Ensure the length of fields matches the expected number of headers
//         if (fields.length < headerMapping.size()) {
//             log.warn("CSV row has fewer fields than expected. Skipping this record: {}", Arrays.toString(fields));
//             return null; // Skip this record
//         }

//      // If election_id exists in the CSV header mapping
//     	Integer electionIdIndex = headerMapping.get("election_id");
//     	if (electionIdIndex != null && fields[electionIdIndex] != null) {
//     	    Long electionId = Long.parseLong(fields[electionIdIndex]);
//     	    ElectionEntity election1 = electionRepository.findById(electionId).orElse(null);
//     	    if (election1 != null) {
//     	        volunteerEntity.setElectionEntity(election1);
//     	    } else {
//     	        log.error("Election with ID {} not found. Volunteer creation failed.", electionId);
//     	        throw new ThedalException(ThedalError.ELECTION_NOT_FOUND, HttpStatus.BAD_REQUEST);
//     	    }
//     	}

        
//         // Map first_name field
//         Integer firstNameIndex = headerMapping.get("first_name");
//         if (firstNameIndex != null && fields[firstNameIndex] != null) {
//             //volunteerEntity.setFirstName(fields[firstNameIndex]);
// 	    	user.setFirstName(fields[firstNameIndex]);
//         }

//         // Map last_name field
//         Integer lastNameIndex = headerMapping.get("last_name");
//         if (lastNameIndex != null && fields[lastNameIndex] != null) {
//             //volunteerEntity.setLastName(fields[lastNameIndex]);
//         	user.setLastName(fields[lastNameIndex]);
//         }

//         // Map mobile_number field
//         Integer mobileNumberIndex = headerMapping.get("mobile_number");
//         if (mobileNumberIndex != null && fields[mobileNumberIndex] != null) {
//             //volunteerEntity.setMobileNumber(fields[mobileNumberIndex]);
        	
// 	        if (userRepo.existsByMobileNumber(fields[mobileNumberIndex])) {
// 	        	log.error("duplicate mobile number found for user: {} and mobile: {}",fields[firstNameIndex] +" "+fields[lastNameIndex],fields[mobileNumberIndex]);
// 	        	throw new ThedalException(ThedalError.DUPLICATE_MOBILE_NUMBER, HttpStatus.BAD_REQUEST);
// 	        }
//         	user.setMobileNumber(fields[mobileNumberIndex]);
//         }

//         // Map role field
// //        Integer roleIndex = headerMapping.get("role");
// //        if (roleIndex != null && fields[roleIndex] != null) {
// //            volunteerEntity.setRole(fields[roleIndex]);
// //        }


//         // Map address fields (embedded Address object)
//         Address address = new Address();
//         Integer streetIndex = headerMapping.get("street");
//         if (streetIndex != null && fields[streetIndex] != null) {
//             address.setStreet(fields[streetIndex]);
//         }

//         Integer cityIndex = headerMapping.get("city");
//         if (cityIndex != null && fields[cityIndex] != null) {
//             address.setCity(fields[cityIndex]);
//         }

//         Integer stateIndex = headerMapping.get("state");
//         if (stateIndex != null && fields[stateIndex] != null) {
//             address.setState(fields[stateIndex]);
//         }

//         Integer postalCodeIndex = headerMapping.get("postal_code");
//         if (postalCodeIndex != null && fields[postalCodeIndex] != null) {
//             address.setPostalCode(fields[postalCodeIndex]);
//         }

//         Integer countryIndex = headerMapping.get("country");
//         if (countryIndex != null && fields[countryIndex] != null) {
//             address.setCountry(fields[countryIndex]);
//         }

//         volunteerEntity.setVolunteerAddress(address);

//         // Map email field
//         Integer emailIndex = headerMapping.get("email");
//         if (emailIndex != null && fields[emailIndex] != null) {
//            // volunteerEntity.setEmail(fields[emailIndex]);
        	
//         	if (userRepo.existsByEmail(fields[emailIndex])) {
//         		log.error("duplicate email if found for user: {} and email: {}",fields[firstNameIndex] +" "+fields[lastNameIndex],fields[emailIndex]);
// 	        	throw new ThedalException(ThedalError.DUPLICATE_EMAIL, HttpStatus.BAD_REQUEST);
//         	}
//         	user.setEmail(fields[emailIndex]);
//         }

// //        // Map assigned_booth field
// //        Integer assignedBoothIndex = headerMapping.get("assigned_booth");
// //        if (assignedBoothIndex != null && fields[assignedBoothIndex] != null) {
// //            volunteerEntity.setAssignedBooth(fields[assignedBoothIndex]);
// //        }
        
//      // Map assigned_booth field and validate booth_number
//         Integer assignedBoothColumn = headerMapping.get("assigned_booth");
//         if (assignedBoothColumn != null && fields.length > assignedBoothColumn && fields[assignedBoothColumn] != null) {
//             String assignedBoothStr = fields[assignedBoothColumn];
//             List<Long> assignedBooths = parseAssignedBooth(assignedBoothStr);

//             // Validate the assigned booths
// //            if (!isValidAssignedBooth(assignedBooths, election.getId())) {
// //                log.warn("Invalid assigned booth found for volunteer. Skipping this record.");
// //                return null; // Skip this record
// //            }
//             if (!isValidAssignedBooth(assignedBooths, election.getId(), account.getId())) {
//                 log.warn("Invalid assigned booth found for volunteer. Skipping this record.");
//                 return null;
//             }

//             volunteerEntity.setAssignedBooth(assignedBooths);
//         }
        
// //        Integer assignedBoothIndex = headerMapping.get("assigned_booth");
// //        if (assignedBoothIndex != null && fields[assignedBoothIndex] != null) {
// //            String assignedBoothStr = fields[assignedBoothIndex];
// //            List<Long> assignedBooths = parseAssignedBooth(assignedBoothStr);
// //            volunteerEntity.setAssignedBooth(assignedBooths);
// //        }


//         // Map status field
//         Integer statusIndex = headerMapping.get("status");
//         if (statusIndex != null && fields[statusIndex] != null) {
//             volunteerEntity.setStatus(fields[statusIndex]);
//         }


//         // Map remarks field
//         Integer remarksIndex = headerMapping.get("remarks");
//         if (remarksIndex != null && fields[remarksIndex] != null) {
//             volunteerEntity.setRemarks(fields[remarksIndex]);
//         }
        
//         // Map password field
//         Integer passwordIndex = headerMapping.get("password");
//         if (passwordIndex != null && fields[passwordIndex] != null) {
//             //volunteerEntity.setRemarks(fields[remarksIndex]);
//             user.setPassword(new BCryptPasswordEncoder().encode(fields[passwordIndex]));
//         }

//         user.setRole(roleRepo.findById(volunteerEntity.getRoleId()).orElseThrow(() -> new ThedalException(ThedalError.ROLE_NOT_FOUND, HttpStatus.BAD_REQUEST)));
//         //user.setRole(userRole);
// 	    user.setIsEmailVerified(true);
//         user.setIsMobileVerified(true);
//         user.setIsActive(true);
//         user.setCreatedAt(LocalDateTime.now());
//         user.setCreatedBy("volunteer create");
//         //user.setOnBoardStatus(2); 
// //        AccountEntity account=new AccountEntity();
// //        account.setOnBoardStatus(AccountOnBoardStatus.SIGNUP_COMPLETION.getValue());
//         //accountRepository.save(account);
//         user.setAccountEntity(account);
//         log.info("Saving user object to DB: {}", user.toString());
//         //UserEntity userEntity = userRepo.save(user);
//         volunteerEntity.setUserEntity(user);
//         volunteerEntity.setAccountId(account.getId());
//         return volunteerEntity;
    
//     }catch(ArrayIndexOutOfBoundsException e){
//     	log.error("Error while mapping CSV row. Row data: {}. Error: {}", Arrays.toString(fields), e.getMessage());
//         return null; // Skip this row
//     }
    	
//     }
 /**
     * Maps a row from a CSV file to a VolunteerEntity based on the header mapping.
     * 
     * @param fields         The array of field values from the CSV row.
     * @param headerMapping  The header mapping with column indices.
     * @param accountId      The account ID to associate with the VoterEntity.
     * @return A VoterEntity object populated with data from the CSV row.
     */
    private VolunteerEntity mapToVolunteerFromCsv(String[] fields, Map<String, Integer> headerMapping, AccountEntity account, ElectionEntity election, Long adminUserId) {
    try {
          Set<String> existingMobileNumbersSet = new HashSet<>(userRepo.findAllMobileNumbers());
        Set<String> existingEmailsSet = new HashSet<>(userRepo.findAllEmails());
      
      

        VolunteerEntity volunteerEntity = new VolunteerEntity();
        volunteerEntity.setElectionEntity(election);

        Integer roleNameIndex = headerMapping.get("role_name");
        if (roleNameIndex != null && roleNameIndex < fields.length && fields[roleNameIndex] != null && !fields[roleNameIndex].isEmpty()) {
            String roleName = fields[roleNameIndex];
            Role userRole = roleRepo.findByRoleNameAndAccountId(roleName, account.getId())
                .orElseThrow(() -> {
                    log.warn("Invalid or missing roleName: {} for accountId: {} in row. Skipping this record.", roleName, account.getId());
                    return new ThedalException(ThedalError.ROLE_NOT_FOUND, HttpStatus.BAD_REQUEST);
                });
            volunteerEntity.setRoleId(userRole.getId());
        } else {
            log.warn("Role name missing or invalid in CSV row. Skipping this record: {}", Arrays.toString(fields));
            return null; 
        }

        UserEntity user = new UserEntity();

        // Ensure the length of fields matches the expected number of headers
        if (fields.length < headerMapping.size()) {
            log.warn("CSV row has fewer fields than expected. Skipping this record: {}", Arrays.toString(fields));
            return null; // Skip this record
        }

        // If election_id exists in the CSV header mapping
        Integer electionIdIndex = headerMapping.get("election_id");
        if (electionIdIndex != null && fields[electionIdIndex] != null) {
            Long electionId = Long.parseLong(fields[electionIdIndex]);
            ElectionEntity election1 = electionRepository.findById(electionId).orElse(null);
            if (election1 != null) {
                volunteerEntity.setElectionEntity(election1);
            } else {
                log.error("Election with ID {} not found. Volunteer creation failed.", electionId);
                throw new ThedalException(ThedalError.ELECTION_NOT_FOUND, HttpStatus.BAD_REQUEST);
            }
        }

        // Map user fields
        Integer firstNameIndex = headerMapping.get("first_name");
        if (firstNameIndex != null && fields[firstNameIndex] != null) {
            user.setFirstName(fields[firstNameIndex]);
        }

        Integer lastNameIndex = headerMapping.get("last_name");
        if (lastNameIndex != null && fields[lastNameIndex] != null) {
            user.setLastName(fields[lastNameIndex]);
        }

        Integer mobileNumberIndex = headerMapping.get("mobile_number");
        if (mobileNumberIndex != null && fields[mobileNumberIndex] != null) {
            String mobileNumber = fields[mobileNumberIndex];
            // Check duplicates in batch before proceeding
            if (existingMobileNumbersSet.contains(mobileNumber)) {
                log.error("Duplicate mobile number found for user: {} and mobile: {}", fields[firstNameIndex] + " " + fields[lastNameIndex], mobileNumber);
                throw new ThedalException(ThedalError.DUPLICATE_MOBILE_NUMBER, HttpStatus.BAD_REQUEST);
            }
            user.setMobileNumber(mobileNumber);
        }

        Integer emailIndex = headerMapping.get("email");
        if (emailIndex != null && fields[emailIndex] != null) {
            String email = fields[emailIndex];
            // Check duplicates in batch before proceeding
            if (existingEmailsSet.contains(email)) {
                log.error("Duplicate email found for user: {} and email: {}", fields[firstNameIndex] + " " + fields[lastNameIndex], email);
                throw new ThedalException(ThedalError.DUPLICATE_EMAIL, HttpStatus.BAD_REQUEST);
            }
            user.setEmail(email);
        }

        // Map address fields (embedded Address object)
        Address address = new Address();
        Integer streetIndex = headerMapping.get("street");
        if (streetIndex != null && fields[streetIndex] != null) {
            address.setStreet(fields[streetIndex]);
        }

        Integer cityIndex = headerMapping.get("city");
        if (cityIndex != null && fields[cityIndex] != null) {
            address.setCity(fields[cityIndex]);
        }

        Integer stateIndex = headerMapping.get("state");
        if (stateIndex != null && fields[stateIndex] != null) {
            address.setState(fields[stateIndex]);
        }

        Integer postalCodeIndex = headerMapping.get("postal_code");
        if (postalCodeIndex != null && fields[postalCodeIndex] != null) {
            address.setPostalCode(fields[postalCodeIndex]);
        }

        Integer countryIndex = headerMapping.get("country");
        if (countryIndex != null && fields[countryIndex] != null) {
            address.setCountry(fields[countryIndex]);
        }

        volunteerEntity.setVolunteerAddress(address);

        
       // Map assigned_booth field and validate booth_number
Integer assignedBoothColumn = headerMapping.get("assigned_booth");
if (assignedBoothColumn != null && fields.length > assignedBoothColumn && fields[assignedBoothColumn] != null && !fields[assignedBoothColumn].trim().isEmpty()) {
    String assignedBoothStr = fields[assignedBoothColumn];
    List<Long> assignedBooths = parseAssignedBooth(assignedBoothStr);

    // Validate the assigned booths with election and account context
    if (!isValidAssignedBooth(assignedBooths, election.getId(), account.getId())) {
        log.warn("Invalid assigned booth(s) found for volunteer. Skipping this record. Assigned booths: {}", assignedBooths);
        return null;
    }

    volunteerEntity.setAssignedBooth(assignedBooths);
}

        
        // Map status field
        Integer statusIndex = headerMapping.get("status");
        if (statusIndex != null && fields[statusIndex] != null) {
            volunteerEntity.setStatus(fields[statusIndex]);
        }

        // Map remarks field
        Integer remarksIndex = headerMapping.get("remarks");
        if (remarksIndex != null && fields[remarksIndex] != null) {
            volunteerEntity.setRemarks(fields[remarksIndex]);
        }

        // Map password field
        Integer passwordIndex = headerMapping.get("password");
        if (passwordIndex != null && fields[passwordIndex] != null) {
            user.setPassword(new BCryptPasswordEncoder().encode(fields[passwordIndex]));
        }

        user.setRole(roleRepo.findById(volunteerEntity.getRoleId()).orElseThrow(() -> new ThedalException(ThedalError.ROLE_NOT_FOUND, HttpStatus.BAD_REQUEST)));
        user.setIsEmailVerified(true);
        user.setIsMobileVerified(true);
        user.setIsActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setCreatedBy("volunteer create");
        user.setAccountEntity(account);

        volunteerEntity.setUserEntity(user);
        volunteerEntity.setAccountId(account.getId());
        volunteerEntity.setAdminUserId(adminUserId);

        return volunteerEntity;
    } catch (ArrayIndexOutOfBoundsException e) {
        log.error("Error while mapping CSV row. Row data: {}. Error: {}", Arrays.toString(fields), e.getMessage());
        return null; // Skip this row
    }
}
}