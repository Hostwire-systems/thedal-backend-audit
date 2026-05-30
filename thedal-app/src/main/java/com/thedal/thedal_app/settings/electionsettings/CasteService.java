package com.thedal.thedal_app.settings.electionsettings;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import com.thedal.thedal_app.cpanel.CasteFileUploadService;
import com.thedal.thedal_app.election.ElectionEntity;
import com.thedal.thedal_app.election.ElectionRepository;
import com.thedal.thedal_app.files.Files;
import com.thedal.thedal_app.files.FilesRepository;
import com.thedal.thedal_app.files.HandlerType;
import com.thedal.thedal_app.general.RequestDetailsService;
import com.thedal.thedal_app.notification.NotificationService;
import com.thedal.thedal_app.notification.NotificationTemplate;
import com.thedal.thedal_app.notification.NotificationType;
import com.thedal.thedal_app.response.ThedalResponse;
import com.thedal.thedal_app.response.ThedalSuccess;
import com.thedal.thedal_app.settings.electionsettings.dto.CasteReorderRequest;
import com.thedal.thedal_app.settings.electionsettings.dto.CasteRequest;
import com.thedal.thedal_app.settings.electionsettings.dto.CasteResponseDTO;
import com.thedal.thedal_app.settings.electionsettings.dto.CasteUpdateRequest;
import com.thedal.thedal_app.settings.electionsettings.dto.ReligionResponseDTO;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;
import com.thedal.thedal_app.voter.VoterRepo;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CasteService {

    @Autowired
    private CasteRepository casteRepository;    
    @Autowired
    private RequestDetailsService requestDetails;
    @Autowired
    private ReligionRepository religionRepository;
    @Autowired
    private SubCasteRepository subCasteRepository;
    @Autowired
	private ElectionRepository electionRepository;
    @Autowired
    private VoterRepo voterRepository;
    @Autowired
    private CasteMongoRepository casteMongoRepository;
    @Autowired
    private ReligionMongoRepository religionMongoRepository;
    @Autowired
    private SectionBulkUploadRepository sectionBulkUploadRepository;
    @Autowired
    private CasteFileUploadServiceMethod casteFileUploadService;
    @Value("${aws.s3.files.bucket}")
	private String s3Filesbucket;
    @Autowired
    private AwsFileUpload awsFileUpload;
    @Autowired
    private FilesRepository filesRepository;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private NotificationTemplate notificationTemplate;
    @Autowired
    private AccountRepository accountRepository;
   

    @Transactional
    public CasteEntity createCaste(CasteRequest casteRequest, Long electionId) {
    	
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account ID not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }
        
        boolean electionExists = electionRepository.existsByIdAndAccountId(electionId, accountId);
        if (!electionExists) {
            log.error("Election ID {} not found for Account ID {}", electionId, accountId);
            throw new ThedalException(ThedalError.ELECTION_NOT_FOUND, HttpStatus.NOT_FOUND);
        }
        
        if (casteRequest.getCasteName() == null || casteRequest.getReligionId() == null) {
            log.error("Missing required fields: casteName or religionId for account ID: {}", accountId);
            throw new ThedalException(ThedalError.MISSING_REQUIRED_FIELDS, HttpStatus.BAD_REQUEST);
        }

        log.info("Creating caste with name: {} under religion ID: {} for election ID: {}", 
                  casteRequest.getCasteName(), casteRequest.getReligionId(), electionId);
        
        ReligionEntity religionEntity = religionRepository.findById(casteRequest.getReligionId())
                .orElseThrow(() -> {
                    log.error("Religion not found for ID: {}", casteRequest.getReligionId());
                    return new ThedalException(ThedalError.RELIGION_NOT_FOUND, HttpStatus.BAD_REQUEST);
                });
        
        // **Check for Duplicate Caste (including electionId)**
        // Optional<CasteEntity> existingCaste = casteRepository.findByCasteNameAndReligion_IdAndAccountIdAndElectionId(
        //         casteRequest.getCasteName(), casteRequest.getReligionId(), accountId, electionId);
        // if (existingCaste.isPresent()) {
        //     log.error("Duplicate caste detected: '{}' for religion ID: {} and election ID: {}", 
        //               casteRequest.getCasteName(), casteRequest.getReligionId(), electionId);
        //     throw new ThedalException(ThedalError.DUPLICATE_CASTE, HttpStatus.CONFLICT);
        // }
        Optional<CasteEntity> existingCaste = casteRepository.findByCasteNameAndReligion_IdAndAccountIdAndElectionId(
            casteRequest.getCasteName(), casteRequest.getReligionId(), accountId, electionId);
    
    if (existingCaste.isPresent()) {
        log.error("Duplicate caste detected: '{}' for religion ID: {} and election ID: {}", 
                  casteRequest.getCasteName(), casteRequest.getReligionId(), electionId);
        throw new ThedalException(
                ThedalError.DUPLICATE_CASTE, 
                HttpStatus.CONFLICT, 
                "Caste with name '" + casteRequest.getCasteName() + "' already exists."
        );
    } 
//        Integer maxOrderIndex = casteRepository.findMaxOrderIndexByReligionIdAndElectionId(
//                casteRequest.getReligionId(), electionId);
//        int newOrderIndex = (maxOrderIndex != null) ? maxOrderIndex + 1 : 0;     
     // Find the max orderIndex for existing castes within this religion and election
        Integer maxOrderIndex = casteRepository.findMaxOrderIndexByReligionIdAndElectionId(
                casteRequest.getReligionId(), electionId);
        
        // Declare newOrderIndex and calculate it
        int newOrderIndex = (maxOrderIndex != null) ? maxOrderIndex + 1 : 0;

        
        // Create a new CasteEntity and set properties
        CasteEntity casteEntity = new CasteEntity();
        casteEntity.setCasteName(casteRequest.getCasteName());
        casteEntity.setReligion(religionEntity);
        casteEntity.setAccountId(accountId);
        casteEntity.setElectionId(electionId);  
        casteEntity.setOrderIndex(newOrderIndex);
        
//        casteRepository.save(casteEntity);
////        log.info("Caste created successfully: {}", casteEntity.getCasteName());
//        log.info("Caste created successfully with orderIndex {}: {}", newOrderIndex, casteEntity.getCasteName());
//        return casteEntity;
        try {
            CasteEntity savedCaste = casteRepository.save(casteEntity);
            try {
                casteMongoRepository.save(new CasteMongo(savedCaste));
                log.info("Saved caste to MongoDB: id={}", savedCaste.getId());
            } catch (Exception mongoEx) {
                log.error("Failed to save caste to MongoDB: id={}", savedCaste.getId(), mongoEx);
                throw new RuntimeException("MongoDB save failed, triggering rollback", mongoEx);
            }
            log.info("Caste created successfully with orderIndex {}: {}", newOrderIndex, casteEntity.getCasteName());
            return savedCaste;
        } catch (Exception ex) {
            log.error("Failed to create caste: {}", casteRequest.getCasteName(), ex);
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
               
    }


    @Transactional
    public ThedalResponse<List<Map<String, Object>>> getCasteByReligion(Long religionId, Long electionId) {
        Long accountId = requestDetails.getCurrentAccountId();

        if (accountId == null) {
            log.error("Account ID not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        log.info("Fetching castes with voter count for religionId: {}, accountId: {}, and electionId: {}", 
                 religionId, accountId, electionId);

        List<Object[]> results = casteRepository.findCastesWithVoterCount(accountId, electionId, religionId);
        if (results.isEmpty()) {
            log.warn("No castes found for religionId: {}, accountId: {}, and electionId: {}", 
                     religionId, accountId, electionId);
            throw new ThedalException(ThedalError.CASTES_NOT_FOUND, HttpStatus.NOT_FOUND);
        }

        List<Map<String, Object>> casteDetails = results.stream()
                .map(result -> {
                    CasteEntity caste = (CasteEntity) result[0];
                    Long voterCount = (Long) result[1];
                    Map<String, Object> casteData = new HashMap<>();
                    casteData.put("casteId", caste.getId());
                    casteData.put("religionId", caste.getReligion().getId());
                    casteData.put("religionName", caste.getReligion().getReligionName() != null ? caste.getReligion().getReligionName() : "");
                    casteData.put("casteName", caste.getCasteName() != null ? caste.getCasteName() : "");
                    casteData.put("electionId", caste.getElectionId());
                    casteData.put("orderIndex", caste.getOrderIndex() != null ? caste.getOrderIndex() : 0);
                    casteData.put("voterCount", voterCount);
                    casteData.put("updatedAt", caste.getUpdatedAt()); // For sorting
                    casteData.put("createdAt", caste.getCreatedAt()); // For sorting
                    casteData.put("id", caste.getId()); // For sorting
                    return casteData;
                })
                .sorted(Comparator
                        .comparing((Map<String, Object> m) -> (LocalDateTime) m.get("updatedAt"), Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing((Map<String, Object> m) -> (LocalDateTime) m.get("createdAt"), Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing((Map<String, Object> m) -> (Long) m.get("id"), Comparator.nullsLast(Comparator.reverseOrder())))
                .map(map -> {
                    // Remove temporary sorting fields
                    map.remove("updatedAt");
                    map.remove("createdAt");
                    map.remove("id");
                    return map;
                })
                .collect(Collectors.toList());

        log.info("Successfully fetched {} castes for electionId: {}", casteDetails.size(), electionId);
        return new ThedalResponse<>(ThedalSuccess.CASTES_FETCHED, casteDetails);
    }

	
//    @Transactional
//public void deleteCasteByIdAndAccountId(List<Long> casteIds, Long accountId, Long electionId) {
//
//    List<CasteEntity> castes;
//
//    if (casteIds == null || casteIds.isEmpty()) {
//        // Fetch all castes if no specific IDs are provided
//        log.info("No caste IDs provided, fetching all castes for deletion.");
//        castes = casteRepository.findByAccountIdAndElectionId(accountId, electionId);
//    } else {
//        castes = casteRepository.findByIdInAndAccountIdAndElectionId(casteIds, accountId, electionId);
//    }
//
//    if (castes.isEmpty()) {
//        throw new ThedalException(ThedalError.CASTE_NOT_FOUND, HttpStatus.NOT_FOUND);
//    }
//
//    // Check if any caste is linked to a voter
//    for (CasteEntity caste : castes) {
//        boolean hasLinkedVoters = voterRepository.existsByCaste_IdAndAccountIdAndElectionId(caste.getId(), accountId, electionId);
//        if (hasLinkedVoters) {
//            throw new ThedalException(ThedalError.CASTE_LINKED_TO_VOTERS, HttpStatus.CONFLICT);
//        }
//    }
//
//    log.info("Deleting {} subcastes for electionId: {}", castes.size(), electionId);
//    // Delete related subcastes before deleting castes
//    subCasteRepository.deleteByCasteIdInAndElectionId(
//        castes.stream().map(CasteEntity::getId).collect(Collectors.toList()), 
//        electionId
//    );
//
//    log.info("Deleting {} castes for electionId: {}", castes.size(), electionId);
//    // Bulk delete castes
//    casteRepository.deleteAll(castes);
//}
    
    @Transactional
    public void deleteCasteByIdAndAccountId(List<Long> casteIds, Long accountId, Long electionId) {
        List<CasteEntity> castes;

        if (casteIds == null || casteIds.isEmpty()) {
            log.info("No caste IDs provided, fetching all castes for deletion.");
            castes = casteRepository.findByAccountIdAndElectionId(accountId, electionId);
        } else {
            castes = casteRepository.findByIdInAndAccountIdAndElectionId(casteIds, accountId, electionId);
        }

        if (castes.isEmpty()) {
            throw new ThedalException(ThedalError.CASTE_NOT_FOUND, HttpStatus.NOT_FOUND);
        }

        for (CasteEntity caste : castes) {
            boolean hasLinkedVoters = voterRepository.existsByCaste_IdAndAccountIdAndElectionId(caste.getId(), accountId, electionId);
            if (hasLinkedVoters) {
                throw new ThedalException(ThedalError.CASTE_LINKED_TO_VOTERS, HttpStatus.CONFLICT,
                        String.format("Cannot delete caste '%s' because it is linked to voters.", caste.getCasteName()));
            }
        }

        log.info("Deleting {} subcastes for electionId: {}", castes.size(), electionId);
        subCasteRepository.deleteByCasteIdInAndElectionId(
                castes.stream().map(CasteEntity::getId).collect(Collectors.toList()), 
                electionId
        );

        try {
            log.info("Deleting {} castes for electionId: {}", castes.size(), electionId);
            casteRepository.deleteAll(castes);
            casteRepository.flush();
            try {
                if (casteIds == null || casteIds.isEmpty()) {
                    casteMongoRepository.deleteByAccountIdAndElectionId(accountId, electionId);
                    log.info("Deleted all castes from MongoDB for accountId: {}, electionId: {}", accountId, electionId);
                } else {
                    casteMongoRepository.deleteByIdIn(casteIds);
                    log.info("Deleted castes from MongoDB: ids={}", casteIds);
                }
            } catch (Exception mongoEx) {
                log.error("Failed to delete castes from MongoDB: ids={}", casteIds, mongoEx);
                throw new RuntimeException("MongoDB deletion failed, triggering rollback", mongoEx);
            }
        } catch (Exception ex) {
            log.error("Failed to delete castes: ids={}", casteIds, ex);
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    
    public CasteResponseDTO updateCaste(Long casteId, Long accountId, CasteUpdateRequest request, Long electionId) {
    	
    	CasteEntity caste = casteRepository.findByIdAndAccountIdAndElectionId(casteId, accountId, electionId)
            .orElseThrow(() -> new ThedalException(ThedalError.CASTE_NOT_FOUND, HttpStatus.NOT_FOUND));

    	// Update caste name if provided
        // if (request.getCasteName() != null) {
        //     caste.setCasteName(request.getCasteName());
        // }
         // Update religion if provided
         if (request.getReligionId() != null) {
            ReligionEntity religionEntity = religionRepository.findById(request.getReligionId())
                .orElseThrow(() -> new ThedalException(ThedalError.RELIGION_NOT_FOUND, HttpStatus.BAD_REQUEST));

            caste.setReligion(religionEntity);
        }
        if (request.getCasteName() != null && !request.getCasteName().isEmpty()) {
            Optional<CasteEntity> existingCaste = casteRepository.findByCasteNameAndReligion_IdAndElectionId(
                request.getCasteName(), caste.getReligion().getId(), electionId
            );
        
            // if (existingCaste.isPresent()) 
            if (existingCaste.isPresent() && !existingCaste.get().getId().equals(casteId)){
                Long existingCasteId = existingCaste.get().getId();
        
                log.info("Found existing caste with ID: {}", existingCasteId);
                log.info("Current caste being updated ID: {}", casteId);
        
                throw new ThedalException(
                    ThedalError.DUPLICATE_CASTE, 
                    HttpStatus.CONFLICT, 
                    "Caste with name '" + request.getCasteName() + "' already exists for religion ID: " + caste.getReligion().getId()
                );
            }
        
            // If no duplicate was found, update the caste name
            caste.setCasteName(request.getCasteName());
        }
        
    
        // // Update religion if provided
        // if (request.getReligionId() != null) {
        //     ReligionEntity religionEntity = religionRepository.findById(request.getReligionId())
        //         .orElseThrow(() -> new ThedalException(ThedalError.RELIGION_NOT_FOUND, HttpStatus.BAD_REQUEST));

        //     caste.setReligion(religionEntity);
        // }
//        caste = casteRepository.save(caste);
//        
//        ReligionResponseDTO religionDTO = new ReligionResponseDTO(
//                caste.getReligion().getId(),
//                caste.getReligion().getReligionName(),
//                caste.getReligion().getReligionImage()
//            );
//
//        return new CasteResponseDTO(caste.getId(), caste.getCasteName(), religionDTO);
        if (request.getReligionId() != null) {
            ReligionEntity religionEntity = religionRepository.findById(request.getReligionId())
                    .orElseThrow(() -> new ThedalException(ThedalError.RELIGION_NOT_FOUND, HttpStatus.BAD_REQUEST));
            caste.setReligion(religionEntity);
        }

        try {
            CasteEntity updatedCaste = casteRepository.save(caste);
            try {
                casteMongoRepository.save(new CasteMongo(updatedCaste));
                log.info("Updated caste in MongoDB: id={}", casteId);
            } catch (Exception mongoEx) {
                log.error("Failed to update caste in MongoDB: id={}", casteId, mongoEx);
                throw new RuntimeException("MongoDB update failed, triggering rollback", mongoEx);
            }
            ReligionResponseDTO religionDTO = new ReligionResponseDTO(
                    updatedCaste.getReligion().getId(),
                    updatedCaste.getReligion().getReligionName(),
                    updatedCaste.getReligion().getReligionImage(),
                    updatedCaste.getReligion().getReligionColor()
            );
            return new CasteResponseDTO(updatedCaste.getId(), updatedCaste.getCasteName(), religionDTO);
        } catch (Exception ex) {
            log.error("Failed to update caste: id={}", casteId, ex);
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
               
    }

    @Transactional
    public void updateCasteOrder(List<CasteReorderRequest> reorderRequests, Long electionId, Long accountId) {
        // Fetch all castes for the given election and account
        List<CasteEntity> castes = casteRepository.findByReligion_ElectionIdAndReligion_AccountIdOrderByOrderIndexAsc(electionId, accountId);

        if (castes.isEmpty()) {
            log.error("No castes found for election ID {} and account ID {}", electionId, accountId);
            throw new ThedalException(ThedalError.CASTE_NOT_FOUND, HttpStatus.NOT_FOUND);
        }

        // Create a map of the new order based on request data
        Map<Long, Integer> newOrderMap = reorderRequests.stream()
                .collect(Collectors.toMap(CasteReorderRequest::getCasteId, CasteReorderRequest::getNewOrderIndex));

        // Sort the castes by orderIndex
        castes.sort(Comparator.comparing(CasteEntity::getOrderIndex));

        // List to store reordered castes
        List<CasteEntity> reorderedCastes = new ArrayList<>(castes);

        // Remove the castes from reorderedCastes if they are present in newOrderMap
        reorderedCastes.removeIf(caste -> newOrderMap.containsKey(caste.getId()));

        // Reorder castes based on the new order index
        for (CasteReorderRequest request : reorderRequests) {
            // Find the caste to reorder
            CasteEntity caste = castes.stream()
                    .filter(c -> c.getId().equals(request.getCasteId()))
                    .findFirst()
                    .orElseThrow(() -> new ThedalException(ThedalError.CASTE_NOT_FOUND, HttpStatus.NOT_FOUND));

            // Set the new order index
            caste.setOrderIndex(request.getNewOrderIndex());

            // Add caste to the list at the new order index
            reorderedCastes.add(request.getNewOrderIndex(), caste);
        }

        // Update orderIndex for all castes and ensure full update for each
        for (int i = 0; i < reorderedCastes.size(); i++) {
            CasteEntity caste = reorderedCastes.get(i);
            caste.setOrderIndex(i); // Set the new order index
            log.info("Updated caste order: {} -> {}", caste.getCasteName(), i);
        }

        try {
            List<CasteEntity> savedCastes = casteRepository.saveAll(reorderedCastes);
            
            // Update MongoDB as well
            try {
                List<CasteMongo> mongoCastes = savedCastes.stream()
                        .map(CasteMongo::new)
                        .collect(Collectors.toList());
                casteMongoRepository.saveAll(mongoCastes);
                log.info("Updated caste order in MongoDB for electionId: {}", electionId);
            } catch (Exception mongoEx) {
                log.error("Failed to update caste order in MongoDB for electionId: {}", electionId, mongoEx);
                throw new RuntimeException("MongoDB reorder failed, triggering rollback", mongoEx);
            }
            
            log.info("Caste order updated successfully for electionId: {}", electionId);
        } catch (Exception ex) {
            log.error("Failed to update caste order for electionId: {}", electionId, ex);
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional
    public ThedalResponse<List<Map<String, Object>>> getCasteByReligionFromMongo(Long religionId, Long electionId) {
        Long accountId = requestDetails.getCurrentAccountId();

        if (accountId == null) {
            log.error("Account ID not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        log.info("Fetching castes from PostgreSQL for religionId: {}, accountId: {}, and electionId: {}", 
                 religionId, accountId, electionId);

        // Read from PostgreSQL instead of MongoDB
        List<CasteEntity> castes = religionId == null
                ? casteRepository.findByAccountIdAndElectionIdOrderByOrderIndexAsc(accountId, electionId)
                : casteRepository.findByReligion_IdAndReligion_AccountIdAndElectionId(religionId, accountId, electionId);

        if (castes.isEmpty()) {
            log.warn("No castes found in PostgreSQL for religionId: {}, accountId: {}, and electionId: {}", 
                     religionId, accountId, electionId);
            throw new ThedalException(ThedalError.CASTES_NOT_FOUND, HttpStatus.NOT_FOUND);
        }

        log.debug("Found {} castes in PostgreSQL: {}", castes.size(), 
                  castes.stream().map(c -> "id=" + c.getId() + ", religionId=" + (c.getReligion() != null ? c.getReligion().getId() : "null")).collect(Collectors.joining("; ")));

        List<Map<String, Object>> casteDetails = castes.stream()
                .map(caste -> {
                    Map<String, Object> casteData = new HashMap<>();
                    casteData.put("electionId", caste.getElectionId());
                    casteData.put("voterCount", 0L);
                    casteData.put("casteId", caste.getId());
                    casteData.put("orderIndex", caste.getOrderIndex() != null ? caste.getOrderIndex() : 0);
                    casteData.put("casteName", caste.getCasteName() != null ? caste.getCasteName() : "");
                    String religionName = caste.getReligion() != null && caste.getReligion().getReligionName() != null 
                                        ? caste.getReligion().getReligionName() : "";
                    casteData.put("religionName", religionName);
                    casteData.put("religionId", caste.getReligion() != null ? caste.getReligion().getId() : null);
                    log.debug("Mapped caste id={} with religionName={}", caste.getId(), religionName);
                    return casteData;
                })
                .sorted(Comparator.comparing(m -> (Integer) m.get("orderIndex")))
                .collect(Collectors.toList());

        log.info("Successfully fetched {} castes from PostgreSQL for electionId: {}", casteDetails.size(), electionId);
        ThedalResponse<List<Map<String, Object>>> response = new ThedalResponse<>(ThedalSuccess.CASTES_FETCHED, casteDetails);
        response.setStatus("success");
        return response;
    }


    @Transactional
    public ThedalResponse<Map<String, Object>> bulkUploadCastesFromXlsxOrCsv(MultipartFile file, Long electionId) {
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
            throw new ThedalException(ThedalError.INVALID_CASTE_FILE_FORMAT, HttpStatus.BAD_REQUEST);
        }

        // Initial header validation
        Map<String, Integer> headerMapping;
        List<String> headerErrors = new ArrayList<>();
        try {
            if (file.getOriginalFilename().endsWith(".xlsx")) {
                Workbook workbook = new XSSFWorkbook(file.getInputStream());
                Sheet sheet = workbook.getSheetAt(0);
                headerMapping = casteFileUploadService.buildHeaderMapping(sheet.getRow(0));
            } else {
                BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream()));
                String[] headers = br.readLine().split(",");
                headerMapping = casteFileUploadService.buildCsvHeaderMapping(headers);
            }

            headerErrors = validateMandatoryHeaders(headerMapping);
            if (!headerErrors.isEmpty()) {
                throw new ThedalException(ThedalError.INVALID_CASTE_FILE_FORMAT, HttpStatus.BAD_REQUEST,
                        "Missing mandatory headers: " + String.join(", ", headerErrors));
            }
        } catch (IOException e) {
            throw new ThedalException(ThedalError.FILE_PROCESSING_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        String folder = "caste_uploads";
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        String originalFileName = file.getOriginalFilename();
        String fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        String uniqueFileName = folder + "/caste_" + System.currentTimeMillis() + "_" + uniqueId + fileExtension;

        Files fileEntity = null;
        String fileUrl = null;

        try {
            fileUrl = awsFileUpload.uploadMultipartFile(file, uniqueFileName, s3Filesbucket);
            log.info("File uploaded to S3 at: {}", fileUrl);

            fileEntity = new Files(HandlerType.BULKUPLOAD_FILES, null, originalFileName, fileUrl);
            filesRepository.save(fileEntity);

            NotificationType startNotification = notificationTemplate.bulkUploadStarted(
                    fileEntity.getFileName(), electionId, null);
            notificationService.saveNotification(true, startNotification);

            // Process file synchronously and get results
            Map<String, Object> result = fileExtension.equalsIgnoreCase(".xlsx") ?
                    casteFileUploadService.processCasteExcelFile(accountEntity, fileUrl, election, headerMapping) :
                    casteFileUploadService.processCasteCsvFile(accountEntity, fileUrl, election, headerMapping);

            long endTime = System.currentTimeMillis();
            result.put("totalTimeTaken", endTime - startTime);
            result.put("fileUrl", fileUrl);

            NotificationType completedNotification = notificationTemplate.bulkUploadCompleted(
                    fileEntity.getFileName(), electionId, null);
            notificationService.saveNotification(true, completedNotification);

            return new ThedalResponse<>(ThedalSuccess.BULK_CASTES_UPLOADED, result);

        } catch (IOException e) {
            log.error("Error uploading file to S3", e);
            NotificationType failedNotification = notificationTemplate.bulkUploadFailed(
                    fileEntity != null ? fileEntity.getFileName() : originalFileName, electionId, null);
            notificationService.saveNotification(true, failedNotification);
            throw new ThedalException(ThedalError.FILE_PROCESSING_FAILED, HttpStatus.INTERNAL_SERVER_ERROR, "File upload to S3 failed.");
        } catch (Exception e) {
            log.error("Unexpected error processing file '{}': {}", originalFileName, e.getMessage(), e);
            NotificationType failedNotification = notificationTemplate.bulkUploadFailed(
                    fileEntity != null ? fileEntity.getFileName() : originalFileName, electionId, null);
            notificationService.saveNotification(true, failedNotification);
            throw new ThedalException(ThedalError.FILE_PROCESSING_FAILED, HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred: " + e.getMessage());
        }
    }

    private List<String> validateMandatoryHeaders(Map<String, Integer> headerMapping) {
        List<String> missingHeaders = new ArrayList<>();
        
        // caste_name is always mandatory
        if (!headerMapping.containsKey("caste_name")) {
            missingHeaders.add("caste_name");
        }
        
        // Either religion_id OR religion_name is required (support both old and new formats)
        if (!headerMapping.containsKey("religion_id") && !headerMapping.containsKey("religion_name")) {
            missingHeaders.add("religion_id or religion_name");
        }
        
        return missingHeaders;
    }

    private boolean isSupportedFormat(String originalFileName) {
        return originalFileName != null && (originalFileName.endsWith(".xlsx") || originalFileName.endsWith(".csv"));
    }

    
}