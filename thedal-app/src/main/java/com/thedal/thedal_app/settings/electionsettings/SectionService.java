package com.thedal.thedal_app.settings.electionsettings;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.thedal.thedal_app.account.AccountEntity;
import com.thedal.thedal_app.account.AccountRepository;
import com.thedal.thedal_app.awsfilestore.AwsFileUpload;
import com.thedal.thedal_app.election.ElectionEntity;
import com.thedal.thedal_app.election.ElectionRepository;
import com.thedal.thedal_app.files.Files;
import com.thedal.thedal_app.files.FilesRepository;
import com.thedal.thedal_app.files.HandlerType;
import com.thedal.thedal_app.general.RequestDetailsService;
import com.thedal.thedal_app.response.ThedalResponse;
import com.thedal.thedal_app.response.ThedalSuccess;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;
import com.thedal.thedal_app.voter.BulkUploadStatus;
import com.thedal.thedal_app.voter.VoterRepo;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SectionService {

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private RequestDetailsService requestDetails;   

    @Autowired
    private ElectionRepository electionRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private AwsFileUpload awsFileUpload;
    @Autowired
    private SectionBulkUploadRepository sectionBulkUploadRepository;
    @Autowired
    private FilesRepository filesRepository;
    @Autowired
    private SectionFileUploadService sectionFileUploadService;
    @Autowired
    private VoterRepo voterRepository;   
    @Value("${aws.s3.files.bucket}")
	private String s3Filesbucket;
    @Autowired
    private SectionMongoRepository sectionMongoRepository;
    
    @Transactional
    public ThedalResponse<SectionResponseDTO> createSection(SectionDTO sectionDTO, Long electionId) {
        // Get the current account ID
        Long accountId = requestDetails.getCurrentAccountId();

        // Validate account ID
        if (accountId == null) {
            log.error("Account ID not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        // Check if the combination of sectionNo and partNo already exists for the given electionId
        if (sectionRepository.existsBySectionNoAndPartNoAndElectionIdAndAccountId(
        sectionDTO.getSectionNo(), sectionDTO.getPartNo(), electionId, accountId)) {
    
    log.error("Section with section number '{}' and part number '{}' already exists in election '{}'.",
              sectionDTO.getSectionNo(), sectionDTO.getPartNo(), electionId);
    
    throw new ThedalException(ThedalError.SECTION_ALREADY_EXISTS, HttpStatus.CONFLICT);
}

        // Create a new Section entity
        SectionEntity section = new SectionEntity();
        section.setPartNo(sectionDTO.getPartNo());
        section.setSectionNo(sectionDTO.getSectionNo());
        section.setSectionNameEn(sectionDTO.getSectionNameEn());
        section.setSectionNameL1(sectionDTO.getSectionNameL1());
        section.setElection(new ElectionEntity(electionId));
        section.setAccountId(accountId);

        // Fetch the ElectionEntity from the database using electionId
ElectionEntity election = electionRepository.findById(electionId)
.orElseThrow(() -> new ThedalException(ThedalError.SECTION_NOT_FOUND, HttpStatus.NOT_FOUND));

// Set the ElectionEntity to the SectionEntity
section.setElection(election);
        
        log.info("Saving new section: {}", section);

        // Dual write implementation
        try {
            SectionEntity savedSection = sectionRepository.save(section);
            
            if (savedSection.getId() == null) {
                log.error("Failed to save section. Entity: {}", section);
                throw new ThedalException(ThedalError.SECTION_NOT_SAVED, HttpStatus.INTERNAL_SERVER_ERROR);
            }
            
            try {
                SectionMongo sectionMongo = new SectionMongo(savedSection);
                sectionMongoRepository.save(sectionMongo);
                log.info("Successfully saved section to MongoDB: id={}, partNo={}, sectionNo={}", 
                        savedSection.getId(), savedSection.getPartNo(), savedSection.getSectionNo());
            } catch (Exception mongoEx) {
                log.error("Failed to save section to MongoDB: id={}, partNo={}, sectionNo={}", 
                        savedSection.getId(), savedSection.getPartNo(), savedSection.getSectionNo(), mongoEx);
                throw new RuntimeException("MongoDB save failed, triggering rollback", mongoEx);
            }

            log.info("Section saved successfully with ID: {}", savedSection.getId());

            // Return the saved section details as DTO
            SectionResponseDTO sectionResponseDTO = new SectionResponseDTO(
                savedSection.getId(),
                savedSection.getPartNo(),
                savedSection.getSectionNo(),
                savedSection.getSectionNameEn(),
                savedSection.getSectionNameL1()
            );

            return new ThedalResponse<>(ThedalSuccess.SECTION_CREATED, sectionResponseDTO);
        } catch (Exception ex) {
            log.error("Failed to create section: partNo={}, sectionNo={}", sectionDTO.getPartNo(), sectionDTO.getSectionNo(), ex);
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional
    public ThedalResponse<List<SectionResponseDTO>> getAllSections(Long electionId, Long accountId) {
        log.info("Fetching all sections from PostgreSQL for electionId: {} and accountId: {}", electionId, accountId);
        
        List<SectionEntity> sections = sectionRepository.findByElectionIdAndAccountId(electionId, accountId);
        
        if (sections.isEmpty()) {
            log.warn("No sections found in PostgreSQL for electionId: {} and accountId: {}", electionId, accountId);
            throw new ThedalException(ThedalError.SECTION_NOT_FOUND, HttpStatus.NOT_FOUND);
        }

        // Convert PostgreSQL entities to DTOs
        List<SectionResponseDTO> sectionDTOs = sections.stream()
                .map(section -> new SectionResponseDTO(
                        section.getId(),
                        section.getPartNo(),
                        section.getSectionNo(),
                        section.getSectionNameEn(),
                        section.getSectionNameL1()
                ))
                .collect(Collectors.toList());

        log.info("Successfully fetched {} sections from PostgreSQL for electionId: {}", sectionDTOs.size(), electionId);
        return new ThedalResponse<>(ThedalSuccess.SUCCESS, sectionDTOs);
    }

@Transactional
public ThedalResponse<SectionResponseDTO> getSectionById(Long electionId, Long accountId, Long id) {
    log.info("Fetching section from PostgreSQL: id={}, electionId={}, accountId={}", id, electionId, accountId);
    
    SectionEntity section = sectionRepository.findByIdAndElectionIdAndAccountId(id, electionId, accountId)
            .orElseThrow(() -> new ThedalException(ThedalError.SECTION_NOT_FOUND, HttpStatus.NOT_FOUND));

    SectionResponseDTO sectionDTO = new SectionResponseDTO(
            section.getId(),
            section.getPartNo(),
            section.getSectionNo(),
            section.getSectionNameEn(),
            section.getSectionNameL1()
    );

    log.info("Successfully fetched section from PostgreSQL: id={}", id);
    return new ThedalResponse<>(ThedalSuccess.SUCCESS, sectionDTO);
}

@Transactional
public ThedalResponse<SectionResponseDTO> updateSection(Long electionId, Long accountId, Long id, SectionDTO sectionDTO) {
    // Find the section by ID
	SectionEntity section = sectionRepository.findByIdAndElectionIdAndAccountId(id, electionId, accountId)
            .orElseThrow(() -> new ThedalException(ThedalError.SECTION_NOT_FOUND, HttpStatus.NOT_FOUND));
	
    boolean exists = sectionRepository.existsBySectionNoAndPartNoAndElectionIdAndAccountIdAndIdNot(
            sectionDTO.getSectionNo(), sectionDTO.getPartNo(), electionId, accountId, id);

    if (exists) { 
         log.error("Section with section number '{}' already exists under partNo '{}' in election '{}'.", 
                sectionDTO.getSectionNo(), sectionDTO.getPartNo(), electionId);
        
        throw new ThedalException(ThedalError.SECTION_ALREADY_EXISTS, HttpStatus.CONFLICT);
    }
    
    // Update section details
    section.setPartNo(sectionDTO.getPartNo());
    section.setSectionNo(sectionDTO.getSectionNo());
    section.setSectionNameEn(sectionDTO.getSectionNameEn());
    section.setSectionNameL1(sectionDTO.getSectionNameL1());

    // Dual write implementation
    try {
        SectionEntity updatedSection = sectionRepository.save(section);
        
        try {
            SectionMongo sectionMongo = new SectionMongo(updatedSection);
            sectionMongoRepository.save(sectionMongo);
            log.info("Successfully updated section in MongoDB: id={}, partNo={}, sectionNo={}", 
                    id, updatedSection.getPartNo(), updatedSection.getSectionNo());
        } catch (Exception mongoEx) {
            log.error("Failed to update section in MongoDB: id={}, partNo={}, sectionNo={}", 
                    id, updatedSection.getPartNo(), updatedSection.getSectionNo(), mongoEx);
            throw new RuntimeException("MongoDB update failed, triggering rollback", mongoEx);
        }

        // Convert to DTO
        SectionResponseDTO sectionResponseDTO = new SectionResponseDTO(
                updatedSection.getId(),
                updatedSection.getPartNo(),
                updatedSection.getSectionNo(),
                updatedSection.getSectionNameEn(),
                updatedSection.getSectionNameL1()
        );

        log.info("Section updated successfully: id={}", id);
        return new ThedalResponse<>(ThedalSuccess.SECTION_UPDATED, sectionResponseDTO);
    } catch (Exception ex) {
        log.error("Failed to update section: id={}", id, ex);
        throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}


@Transactional
public void deleteSection(Long electionId, Long accountId, Long id) {
    // Find the section by ID
    SectionEntity section = sectionRepository.findByIdAndElectionIdAndAccountId(id, electionId, accountId)
            .orElseThrow(() -> new ThedalException(ThedalError.SECTION_NOT_FOUND, HttpStatus.NOT_FOUND));

    try {
        // Delete from PostgreSQL
        sectionRepository.delete(section);
        sectionRepository.flush();
        
        try {
            // Delete from MongoDB
            sectionMongoRepository.deleteById(id);
            log.info("Successfully deleted section from MongoDB: id={}", id);
        } catch (Exception mongoEx) {
            log.error("Failed to delete section from MongoDB: id={}", id, mongoEx);
            throw new RuntimeException("MongoDB deletion failed, triggering rollback", mongoEx);
        }
        
        log.info("Section deleted successfully: id={}", id);
    } catch (Exception ex) {
        log.error("Failed to delete section: id={}", id, ex);
        throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

@Transactional
public void deleteSections(Long electionId, Long accountId, List<Long> sectionIds) {
    try {
        if (sectionIds == null || sectionIds.isEmpty()) {
            log.info("Deleting all sections for electionId: {}, accountId: {}", electionId, accountId);
            int deletedCount = sectionRepository.deleteByElectionIdAndAccountId(electionId, accountId);
            if (deletedCount == 0) {
                throw new ThedalException(ThedalError.SECTION_NOT_FOUND, HttpStatus.NOT_FOUND);
            }
            
            try {
                sectionMongoRepository.deleteByAccountIdAndElectionId(accountId, electionId);
                log.info("Deleted all sections from MongoDB for accountId: {}, electionId: {}", accountId, electionId);
            } catch (Exception mongoEx) {
                log.error("Failed to delete all sections from MongoDB for accountId: {}, electionId: {}", accountId, electionId, mongoEx);
                throw new RuntimeException("MongoDB deletion failed, triggering rollback", mongoEx);
            }
        } else {
            log.info("Deleting selected sections for electionId: {}, accountId: {}, sectionIds: {}", electionId, accountId, sectionIds);
            int deletedCount = sectionRepository.deleteByElectionIdAndAccountIdAndIdIn(electionId, accountId, sectionIds);
            if (deletedCount == 0) {
                throw new ThedalException(ThedalError.SECTION_NOT_FOUND, HttpStatus.NOT_FOUND);
            }
            
            try {
                sectionMongoRepository.deleteByIdIn(sectionIds);
                log.info("Deleted sections from MongoDB: ids={}", sectionIds);
            } catch (Exception mongoEx) {
                log.error("Failed to delete sections from MongoDB: ids={}", sectionIds, mongoEx);
                throw new RuntimeException("MongoDB deletion failed, triggering rollback", mongoEx);
            }
        }
        
        log.info("Sections deleted successfully: ids={}", sectionIds);
    } catch (Exception ex) {
        log.error("Failed to delete sections: ids={}", sectionIds, ex);
        throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

//@Transactional
//public ThedalResponse<Void> uploadSectionsFromXlsxOrCsv(MultipartFile file, Long electionId) {
//
//    requestDetails.checkUserRolePermission(RolePermission.CADRE_MANAGEMENT);
//
//    ElectionEntity election = electionRepository.findById(electionId)
//            .orElseThrow(() -> new ThedalException(ThedalError.ELECTION_NOT_FOUND, HttpStatus.NOT_FOUND));
//
//    long startTime = System.currentTimeMillis();
//    Long accountId = requestDetails.getCurrentAccountId();
//
//    if (accountId == null) {
//        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
//    }
//
//    // Fetch the account entity
//    AccountEntity accountEntity = accountRepository.findById(accountId)
//            .orElseThrow(() -> new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.BAD_REQUEST));
//
//    if (!isSupportedFormat(file.getOriginalFilename()) || file.isEmpty()) {
//        throw new ThedalException(ThedalError.INVALID_FILE_FORMAT, HttpStatus.BAD_REQUEST);
//    }
//
//    String folder = "section_uploads";
//    String uniqueId = UUID.randomUUID().toString().substring(0, 8);
//    String originalFileName = file.getOriginalFilename();
//    String fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
//    String uniqueFileName = folder + "/section_" + System.currentTimeMillis() + "_" + uniqueId + fileExtension;
//
//    String fileUrl = null;
//    Long bulkUploadId = null;
//
//    try {
//        fileUrl = awsFileUpload.uploadMultipartFile(file, uniqueFileName, s3Filesbucket);
//        log.info("File uploaded to S3 at: {}", fileUrl);
//
//        // Create and save the bulk upload entry
//        SectionBulkUploadEntity sectionBulkUploadEntity = new SectionBulkUploadEntity();
//        sectionBulkUploadEntity.setAccountId(accountId);
//        sectionBulkUploadEntity.setElectionId(electionId);
//        sectionBulkUploadEntity.setStartTime(LocalDateTime.now());
//        sectionBulkUploadEntity.setStatus(BulkUploadStatus.IN_PROGRESS);
//        sectionBulkUploadRepository.save(sectionBulkUploadEntity);
//
//        bulkUploadId = sectionBulkUploadEntity.getId();
//
//        // Save file metadata
//        Files fileEntity = new Files(HandlerType.BULKUPLOAD_FILES, sectionBulkUploadEntity.getId(), originalFileName, fileUrl);
//        filesRepository.save(fileEntity);
//
//        // Process the file asynchronously
//        if (fileExtension.equalsIgnoreCase(".xlsx")) {
//            sectionFileUploadService.processSectionExcelFileAsync(bulkUploadId, accountEntity, fileUrl, election, sectionBulkUploadEntity);
//        } else if (fileExtension.equalsIgnoreCase(".csv")) {
//            sectionFileUploadService.processSectionCsvFileAsync(bulkUploadId, accountEntity, fileUrl, election, sectionBulkUploadEntity);
//        }
//
//        sectionBulkUploadEntity.setStatus(BulkUploadStatus.COMPLETED);
//        sectionBulkUploadEntity.setEndTime(LocalDateTime.now());
//        sectionBulkUploadRepository.save(sectionBulkUploadEntity);
//
//    } catch (IOException e) {
//        log.error("Error uploading file to S3", e);
//        throw new ThedalException(ThedalError.FILE_PROCESSING_FAILED, HttpStatus.INTERNAL_SERVER_ERROR, "File upload to S3 failed.");
//    } finally {
//        long endTime = System.currentTimeMillis();
//        log.info("Time taken to process file: {} ms", (endTime - startTime));
//    }
//
//    return new ThedalResponse<>(ThedalSuccess.BULK_SECTIONS_UPLOADED);
//}
//
//private boolean isSupportedFormat(String originalFileName) {
//    return originalFileName != null && (originalFileName.endsWith(".xlsx") || originalFileName.endsWith(".csv"));
//}

@Transactional
public ThedalResponse<SectionBulkUploadEntity> uploadSectionsFromXlsxOrCsv(MultipartFile file, Long electionId) {
    long startTime = System.currentTimeMillis();
    Long accountId = requestDetails.getCurrentAccountId();

    if (accountId == null) {
        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
    }

    AccountEntity accountEntity = accountRepository.findById(accountId)
            .orElseThrow(() -> new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.BAD_REQUEST));

    ElectionEntity election = electionRepository.findById(electionId)
            .orElseThrow(() -> new ThedalException(ThedalError.ELECTION_NOT_FOUND, HttpStatus.NOT_FOUND));

    if (!isSupportedFormat(file.getOriginalFilename()) || file.isEmpty()) {
        throw new ThedalException(ThedalError.INVALID_SECTION_FILE_FORMAT, HttpStatus.BAD_REQUEST);
    }

    Map<String, Integer> headerMapping;
    try {
        if (file.getOriginalFilename().endsWith(".xlsx")) {
            Workbook workbook = new XSSFWorkbook(file.getInputStream());
            Sheet sheet = workbook.getSheetAt(0);
            headerMapping = sectionFileUploadService.buildHeaderMapping(sheet.getRow(0));
        } else {
            BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream()));
            String[] headers = br.readLine().split(",");
            headerMapping = sectionFileUploadService.buildCsvHeaderMapping(headers);
        }
        List<String> headerErrors = validateMandatoryHeaders(headerMapping);
        if (!headerErrors.isEmpty()) {
            throw new ThedalException(ThedalError.INVALID_SECTION_FILE_FORMAT, HttpStatus.BAD_REQUEST,
                "Missing mandatory headers: " + String.join(", ", headerErrors));
        }
    } catch (IOException e) {
        throw new ThedalException(ThedalError.FILE_PROCESSING_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    String folder = "section_uploads";
    String uniqueId = UUID.randomUUID().toString().substring(0, 8);
    String originalFileName = file.getOriginalFilename();
    String fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
    String uniqueFileName = folder + "/section_" + System.currentTimeMillis() + "_" + uniqueId + fileExtension;

    String fileUrl = null;
    SectionBulkUploadEntity sectionBulkUploadEntity = null;

    try {
        fileUrl = awsFileUpload.uploadMultipartFile(file, uniqueFileName, s3Filesbucket);
        log.info("File uploaded to S3 at: {}", fileUrl);

        sectionBulkUploadEntity = new SectionBulkUploadEntity();
        sectionBulkUploadEntity.setAccountId(accountId);
        sectionBulkUploadEntity.setElectionId(electionId);
        sectionBulkUploadEntity.setStartTime(LocalDateTime.now());
        sectionBulkUploadEntity.setStatus(BulkUploadStatus.IN_PROGRESS);
        sectionBulkUploadEntity.setTotalRecords(0L);
        sectionBulkUploadEntity.setTotalProcessedRecords(0L);
        sectionBulkUploadEntity.setTotalSuccessRecords(0L);
        sectionBulkUploadEntity.setTotalFailedRecords(0L);
        sectionBulkUploadRepository.save(sectionBulkUploadEntity);

        Files fileEntity = new Files(HandlerType.BULKUPLOAD_FILES, sectionBulkUploadEntity.getId(), originalFileName, fileUrl);
        filesRepository.save(fileEntity);
        sectionBulkUploadEntity.setFile(fileEntity);

        if (fileExtension.equalsIgnoreCase(".xlsx")) {
            sectionFileUploadService.processSectionExcelFile(sectionBulkUploadEntity, accountEntity, fileUrl, election);
        } else if (fileExtension.equalsIgnoreCase(".csv")) {
            sectionFileUploadService.processSectionCsvFile(sectionBulkUploadEntity, accountEntity, fileUrl, election);
        }

        long endTime = System.currentTimeMillis();
        sectionBulkUploadEntity.setTotalTimeTaken(endTime - startTime);
        sectionBulkUploadEntity.setEndTime(LocalDateTime.now());
        sectionBulkUploadEntity.setStatus(BulkUploadStatus.COMPLETED);
        sectionBulkUploadRepository.save(sectionBulkUploadEntity);

        return new ThedalResponse<>(ThedalSuccess.BULK_SECTIONS_UPLOADED, sectionBulkUploadEntity);

    } catch (IOException e) {
        log.error("Error uploading file to S3", e);
        if (sectionBulkUploadEntity != null) {
            sectionBulkUploadEntity.setStatus(BulkUploadStatus.FAILED);
            sectionBulkUploadEntity.setEndTime(LocalDateTime.now());
            sectionBulkUploadEntity.setTotalTimeTaken(System.currentTimeMillis() - startTime);
            sectionBulkUploadRepository.save(sectionBulkUploadEntity);
        }
        throw new ThedalException(ThedalError.FILE_PROCESSING_FAILED, HttpStatus.INTERNAL_SERVER_ERROR, "File upload to S3 failed.");
    } catch (Exception e) {
        log.error("Unexpected error processing file '{}': {}", originalFileName, e.getMessage(), e);
        if (sectionBulkUploadEntity != null) {
            sectionBulkUploadEntity.setStatus(BulkUploadStatus.FAILED);
            sectionBulkUploadEntity.setEndTime(LocalDateTime.now());
            sectionBulkUploadEntity.setTotalTimeTaken(System.currentTimeMillis() - startTime);
            sectionBulkUploadRepository.save(sectionBulkUploadEntity);
        }
        throw new ThedalException(ThedalError.FILE_PROCESSING_FAILED, HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred: " + e.getMessage());
    }
}

private List<String> validateMandatoryHeaders(Map<String, Integer> headerMapping) {
    List<String> missingHeaders = new ArrayList<>();
    String[] mandatoryHeaders = {"part_no", "section_no", "section_name_en"};
    for (String header : mandatoryHeaders) {
        if (!headerMapping.containsKey(header)) {
            missingHeaders.add(header);
        }
    }
    return missingHeaders;
}

private boolean isSupportedFormat(String originalFileName) {
    return originalFileName != null && (originalFileName.endsWith(".xlsx") || originalFileName.endsWith(".csv"));
}



}
