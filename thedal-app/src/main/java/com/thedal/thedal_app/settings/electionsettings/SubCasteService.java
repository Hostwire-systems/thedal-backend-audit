package com.thedal.thedal_app.settings.electionsettings;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
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
import com.thedal.thedal_app.settings.electionsettings.dto.CasteResponseDTO;
import com.thedal.thedal_app.settings.electionsettings.dto.ReligionResponseDTO;
import com.thedal.thedal_app.settings.electionsettings.dto.SubCasteReorderRequest;
import com.thedal.thedal_app.settings.electionsettings.dto.SubCasteRequest;
import com.thedal.thedal_app.settings.electionsettings.dto.SubCasteResponseDTO;
import com.thedal.thedal_app.settings.electionsettings.dto.SubCasteUpdateRequest;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;
import com.thedal.thedal_app.voter.VoterRepo;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SubCasteService {

    @Autowired
    private SubCasteRepository subCasteRepository;
    @Autowired
    private CasteRepository casteRepository;   
    @Autowired
    private RequestDetailsService requestDetails;
    @Autowired
    private ReligionRepository religionRepository;
    @Autowired
    private VoterRepo voterRepository;
    @Autowired
	private ElectionRepository electionRepository;
    @Autowired
    private SubCasteMongoRepository subCasteMongoRepository;
    @Autowired
    private CasteMongoRepository casteMongoRepository;
    @Autowired
    private ReligionMongoRepository religionMongoRepository;
    @Autowired
    private SubCasteFileUploadMethod subCasteFileUploadMethod;
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
    public ThedalResponse<SubCasteEntity> createSubCaste(SubCasteRequest subCasteRequest, Long electionId) {
    	
    	Long accountId = requestDetails.getCurrentAccountId();
        
        if (accountId == null) {
            log.error("Account ID not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        if (subCasteRequest.getSubCasteName() == null || subCasteRequest.getCasteId() == null || subCasteRequest.getReligionId() == null) {
            log.error("Missing required fields: subCasteName, casteId, or religionId for account ID: {}", accountId);
            throw new ThedalException(ThedalError.MISSING_REQUIRED_FIELDS, HttpStatus.BAD_REQUEST);
        }

        log.info("Creating sub-caste with name: {} under caste ID: {} and religion ID: {} for election ID: {}",
                subCasteRequest.getSubCasteName(), subCasteRequest.getCasteId(), subCasteRequest.getReligionId(), electionId);

        boolean electionExists = electionRepository.existsByIdAndAccountId(electionId, accountId);
        if (!electionExists) {
            log.error("Election ID: {} does not exist or is not associated with Account ID: {}", electionId, accountId);
            throw new ThedalException(ThedalError.ELECTION_NOT_FOUND, HttpStatus.BAD_REQUEST);
        }
        CasteEntity casteEntity = casteRepository.findById(subCasteRequest.getCasteId())
                .orElseThrow(() -> {
                    log.error("Caste not found for ID: {}", subCasteRequest.getCasteId());
                    return new ThedalException(ThedalError.CASTE_NOT_FOUND, HttpStatus.BAD_REQUEST);
                });

        ReligionEntity religionEntity = religionRepository.findById(subCasteRequest.getReligionId())
                .orElseThrow(() -> {
                    log.error("Religion not found for ID: {}", subCasteRequest.getReligionId());
                    return new ThedalException(ThedalError.RELIGION_NOT_FOUND, HttpStatus.BAD_REQUEST);
                });
        
     // **Find Max Order Index for SubCaste**
        Integer maxOrderIndex = subCasteRepository.findMaxOrderIndexByCasteIdAndElectionId(
            subCasteRequest.getCasteId(), electionId);
        int newOrderIndex = (maxOrderIndex == null) ? 1 : maxOrderIndex + 1;
        
        // **Duplicate Sub-Caste Validation**
        // Optional<SubCasteEntity> existingSubCaste = subCasteRepository.findBySubCasteNameAndCaste_IdAndReligion_IdAndAccountId(
        //     subCasteRequest.getSubCasteName(), subCasteRequest.getCasteId(), subCasteRequest.getReligionId(), accountId);

        // if (existingSubCaste.isPresent()) {
        //     log.error("Duplicate sub_caste detected: '{}' for caste ID: {} and religion ID: {}",
        //             subCasteRequest.getSubCasteName(), subCasteRequest.getCasteId(), subCasteRequest.getReligionId());
        //     throw new ThedalException(ThedalError.DUPLICATE_SUB_CASTE, HttpStatus.CONFLICT);
        // }
        Optional<SubCasteEntity> existingSubCaste = subCasteRepository.findBySubCasteNameAndCaste_IdAndReligion_IdAndAccountId(
        subCasteRequest.getSubCasteName(), subCasteRequest.getCasteId(), subCasteRequest.getReligionId(), accountId);

    if (existingSubCaste.isPresent()) {
        log.error("Duplicate sub-caste detected: '{}' for caste ID: {} and religion ID: {}",
                subCasteRequest.getSubCasteName(), subCasteRequest.getCasteId(), subCasteRequest.getReligionId());
        throw new ThedalException(
                ThedalError.DUPLICATE_SUB_CASTE,
                HttpStatus.CONFLICT,
                "Sub-caste with name '" + subCasteRequest.getSubCasteName() + "' already exists."
        );
    }

        SubCasteEntity subCasteEntity = new SubCasteEntity();
        subCasteEntity.setSubCasteName(subCasteRequest.getSubCasteName());
        subCasteEntity.setCaste(casteEntity);
        subCasteEntity.setReligion(religionEntity);
        subCasteEntity.setAccountId(accountId);
        subCasteEntity.setElectionId(electionId);  
        subCasteEntity.setOrderIndex(newOrderIndex);
        
//        subCasteRepository.save(subCasteEntity);
//        log.info("Sub-caste created successfully: {}", subCasteEntity.getSubCasteName());
//        return new ThedalResponse<>(ThedalSuccess.SUB_CASTE_CREATED, subCasteEntity);
        try {
            SubCasteEntity savedSubCaste = subCasteRepository.save(subCasteEntity);
            log.info("Successfully saved sub-caste to PostgreSQL: id={}, name={}", savedSubCaste.getId(), savedSubCaste.getSubCasteName());
            
            try {
                // Create MongoDB document
                SubCasteMongo mongoDoc = new SubCasteMongo(savedSubCaste);
                subCasteMongoRepository.save(mongoDoc);
                log.info("Successfully saved sub-caste to MongoDB: id={}, name={}", savedSubCaste.getId(), savedSubCaste.getSubCasteName());
            } catch (Exception mongoEx) {
                log.error("Failed to save sub-caste to MongoDB: id={}, name={}. Error: {}", 
                    savedSubCaste.getId(), savedSubCaste.getSubCasteName(), mongoEx.getMessage(), mongoEx);
                
                // Rollback PostgreSQL by throwing exception
                throw new RuntimeException("MongoDB save failed for sub-caste: " + savedSubCaste.getSubCasteName() + 
                    ". Rolling back PostgreSQL transaction.", mongoEx);
            }
            
            log.info("Sub-caste created successfully in both PostgreSQL and MongoDB: {}", savedSubCaste.getSubCasteName());
            return new ThedalResponse<>(ThedalSuccess.SUB_CASTE_CREATED, savedSubCaste);
            
        } catch (Exception ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("MongoDB save failed")) {
                // Re-throw MongoDB sync errors
                throw ex;
            }
            log.error("Failed to create sub-caste: {}. Error: {}", subCasteRequest.getSubCasteName(), ex.getMessage(), ex);
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
               
    }


    
//    public List<SubCasteEntity> getSubCasteByReligionAndCaste(Long religionId, Long casteId, Long electionId) {
//    
//        Long accountId = requestDetails.getCurrentAccountId();
//    
//        if (accountId == null) {
//            log.error("Account ID not found, unauthorized access.");
//            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
//        }
//    
//        List<SubCasteEntity> subcastes;
//    
//        if (religionId == null && casteId == null) {
//            log.info("Fetching all subcastes for electionId: {} and accountId: {}", electionId, accountId);
//            subcastes = subCasteRepository
//                .findAllByReligion_AccountIdAndElectionIdOrderByUpdatedAtDescCreatedAtDesc(accountId, electionId);
//    
//        } else if (religionId != null && casteId == null) {
//            log.info("Fetching subcastes for religionId: {}, electionId: {}, and accountId: {}", religionId, electionId, accountId);
//            subcastes = subCasteRepository
//                .findByReligionIdAndReligion_AccountIdAndElectionIdOrderByUpdatedAtDescCreatedAtDesc(religionId, accountId, electionId);
//    
//        } else if (religionId == null && casteId != null) {
//            log.info("Fetching subcastes for casteId: {}, electionId: {}, and accountId: {}", casteId, electionId, accountId);
//            subcastes = subCasteRepository
//                .findByCasteIdAndCaste_Religion_AccountIdAndElectionIdOrderByUpdatedAtDescCreatedAtDesc(casteId, accountId, electionId);
//    
//        } else {
//            log.info("Fetching subcastes for religionId: {}, casteId: {}, electionId: {}, and accountId: {}", religionId, casteId, electionId, accountId);
//            subcastes = subCasteRepository
//                .findByReligionIdAndCasteIdAndReligion_AccountIdAndElectionIdOrderByUpdatedAtDescCreatedAtDesc(religionId, casteId, accountId, electionId);
//        }
//    
//        if (subcastes.isEmpty()) {
//            throw new ThedalException(ThedalError.SUBCASTES_NOT_FOUND, HttpStatus.NOT_FOUND);
//        }
//    
//        return subcastes;
//    }
    
    @Transactional
    public ThedalResponse<List<Map<String, Object>>> getSubCasteByReligionAndCaste(Long religionId, Long casteId, Long electionId) {
        Long accountId = requestDetails.getCurrentAccountId();

        if (accountId == null) {
            log.error("Account ID not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        log.info("Fetching subcastes with voter count for religionId: {}, casteId: {}, electionId: {}, accountId: {}", 
                 religionId, casteId, electionId, accountId);

        List<Object[]> results = subCasteRepository.findSubCastesWithVoterCount(accountId, electionId, religionId, casteId);
        if (results.isEmpty()) {
            log.warn("No subcastes found for religionId: {}, casteId: {}, electionId: {}, accountId: {}", 
                     religionId, casteId, electionId, accountId);
            return new ThedalResponse<>(ThedalSuccess.SUBCASTES_FETCHED, Collections.emptyList());
        }

        List<Map<String, Object>> subCasteDetails = results.stream()
                .map(result -> {
                    SubCasteEntity subCaste = (SubCasteEntity) result[0];
                    Long voterCount = (Long) result[1];
                    Map<String, Object> subCasteData = new HashMap<>();
                    subCasteData.put("subCasteId", subCaste.getId());
                    subCasteData.put("subCasteName", subCaste.getSubCasteName() != null ? subCaste.getSubCasteName() : "");
                    subCasteData.put("casteId", subCaste.getCaste() != null ? subCaste.getCaste().getId() : null);
                    subCasteData.put("casteName", subCaste.getCaste() != null && subCaste.getCaste().getCasteName() != null ? subCaste.getCaste().getCasteName() : "");
                    subCasteData.put("religionId", subCaste.getReligion() != null ? subCaste.getReligion().getId() : null);
                    subCasteData.put("religionName", subCaste.getReligion() != null && subCaste.getReligion().getReligionName() != null ? subCaste.getReligion().getReligionName() : "");
                    subCasteData.put("orderIndex", subCaste.getOrderIndex() != null ? subCaste.getOrderIndex() : 0);
                    subCasteData.put("voterCount", voterCount);
                    return subCasteData;
                })
                .sorted(Comparator.comparingInt(subCaste -> (Integer) subCaste.get("orderIndex")))
                .collect(Collectors.toList());

        log.info("Successfully fetched {} subcastes for electionId: {}", subCasteDetails.size(), electionId);
        return new ThedalResponse<>(ThedalSuccess.SUBCASTES_FETCHED, subCasteDetails);
    }

     
//    @Transactional
//public void deleteSubCasteByIdAndAccountIdAndElectionId(List<Long> subCasteIds, Long accountId, Long electionId) {
//
//    List<SubCasteEntity> subCastes;
//    
//    if (subCasteIds == null || subCasteIds.isEmpty()) {
//        // If no IDs provided, fetch all subcastes for the account and election
//        subCastes = subCasteRepository.findByAccountIdAndElectionId(accountId, electionId);
//        log.info("No specific subcaste IDs provided, deleting all subcastes for Account ID: {} and Election ID: {}", accountId, electionId);
//    } else {
//        // Fetch subcastes based on given IDs
//        subCastes = subCasteRepository.findAllByIdAndAccountIdAndElectionId(subCasteIds, accountId, electionId);
//        log.info("Deleting specific subcastes: {} for Account ID: {} and Election ID: {}", subCasteIds, accountId, electionId);
//    }
//
//    if (subCastes.isEmpty()) {
//        log.error("No SubCastes found for IDs: {}, Account ID: {}, Election ID: {}", subCasteIds, accountId, electionId);
//        throw new ThedalException(ThedalError.SUBCASTE_NOT_FOUND, HttpStatus.NOT_FOUND);
//    }
//
//    // Check if any subcaste is linked to voters
//    for (SubCasteEntity subCaste : subCastes) {
//        boolean hasLinkedVoters = voterRepository.existsBySubCasteAndAccountId(subCaste.getId(), accountId);
//        if (hasLinkedVoters) {
//            log.warn("SubCaste ID: {} is linked to voters. Skipping deletion.", subCaste.getId());
//            throw new ThedalException(ThedalError.SUBCASTE_LINKED_TO_VOTERS, HttpStatus.CONFLICT);
//        }
//    }
//
//    log.info("Deleting {} subcastes for Account ID: {} and Election ID: {}", subCastes.size(), accountId, electionId);
//
//    // Delete all fetched subcastes
//    subCasteRepository.deleteAll(subCastes);
//}
    
    @Transactional
    public void deleteSubCasteByIdAndAccountIdAndElectionId(List<Long> subCasteIds, Long accountId, Long electionId) {
        List<SubCasteEntity> subCastes;

        if (subCasteIds == null || subCasteIds.isEmpty()) {
            subCastes = subCasteRepository.findByAccountIdAndElectionId(accountId, electionId);
            log.info("No specific subcaste IDs provided, deleting all subcastes for Account ID: {} and Election ID: {}", accountId, electionId);
        } else {
            subCastes = subCasteRepository.findAllByIdAndAccountIdAndElectionId(subCasteIds, accountId, electionId);
            log.info("Deleting specific subcastes: {} for Account ID: {} and Election ID: {}", subCasteIds, accountId, electionId);
        }

        if (subCastes.isEmpty()) {
            log.error("No SubCastes found for IDs: {}, Account ID: {}, Election ID: {}", subCasteIds, accountId, electionId);
            throw new ThedalException(ThedalError.SUBCASTE_NOT_FOUND, HttpStatus.NOT_FOUND);
        }

        for (SubCasteEntity subCaste : subCastes) {
            boolean hasLinkedVoters = voterRepository.existsBySubCasteAndAccountId(subCaste.getId(), accountId);
            if (hasLinkedVoters) {
                log.warn("SubCaste ID: {} is linked to voters. Skipping deletion.", subCaste.getId());
                throw new ThedalException(ThedalError.SUBCASTE_LINKED_TO_VOTERS, HttpStatus.CONFLICT);
            }
        }

        try {
            log.info("Deleting {} subcastes for Account ID: {} and Election ID: {}", subCastes.size(), accountId, electionId);
            subCasteRepository.deleteAll(subCastes);
            subCasteRepository.flush();
            try {
                if (subCasteIds == null || subCasteIds.isEmpty()) {
                    subCasteMongoRepository.deleteByAccountIdAndElectionId(accountId, electionId);
                    log.info("Deleted all subcastes from MongoDB for accountId: {}, electionId: {}", accountId, electionId);
                } else {
                    subCasteMongoRepository.deleteByIdIn(subCasteIds);
                    log.info("Deleted subcastes from MongoDB: ids={}", subCasteIds);
                }
            } catch (Exception mongoEx) {
                log.error("Failed to delete subcastes from MongoDB: ids={}", subCasteIds, mongoEx);
                throw new RuntimeException("MongoDB deletion failed, triggering rollback", mongoEx);
            }
        } catch (Exception ex) {
            log.error("Failed to delete subcastes: ids={}", subCasteIds, ex);
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

	
    public SubCasteResponseDTO updateSubCaste(Long subCasteId, Long accountId, Long electionId, SubCasteUpdateRequest request) {

        SubCasteEntity subCaste = subCasteRepository.findByIdAndAccountIdAndElectionId(subCasteId, accountId, electionId)
            .orElseThrow(() -> new ThedalException(ThedalError.SUBCASTE_NOT_FOUND, HttpStatus.NOT_FOUND));

        Long casteId = subCaste.getCaste().getId();
        Long religionId = subCaste.getReligion().getId();
        
        // if (request.getSubCasteName() != null) {
        //     subCaste.setSubCasteName(request.getSubCasteName());
        // }
        if (request.getSubCasteName() != null && !request.getSubCasteName().isEmpty()) {
            Optional<SubCasteEntity> existingSubCaste = subCasteRepository.findBySubCasteNameAndCaste_IdAndReligion_IdAndAccountId(
                    request.getSubCasteName(), casteId, religionId, accountId);
    
            if (existingSubCaste.isPresent() && !existingSubCaste.get().getId().equals(subCasteId)) {
                Long existingSubCasteId = existingSubCaste.get().getId();
    
                log.info("Found existing sub-caste with ID: {}", existingSubCasteId);
                log.info("Current sub-caste being updated ID: {}", subCasteId);
    
                throw new ThedalException(
                        ThedalError.DUPLICATE_SUB_CASTE,
                        HttpStatus.CONFLICT,
                        "Sub-caste with name '" + request.getSubCasteName() + "' already exists for caste ID: " + casteId
                );
            }
    
            // If no duplicate found, update sub-caste name
            subCaste.setSubCasteName(request.getSubCasteName());
        }
    
        if (request.getCasteId() != null) {
            CasteEntity casteEntity = casteRepository.findById(request.getCasteId())
                .orElseThrow(() -> new ThedalException(ThedalError.CASTE_NOT_FOUND, HttpStatus.BAD_REQUEST));
            subCaste.setCaste(casteEntity);
        }
    
        if (request.getReligionId() != null) {
            ReligionEntity religionEntity = religionRepository.findById(request.getReligionId())
                .orElseThrow(() -> new ThedalException(ThedalError.RELIGION_NOT_FOUND, HttpStatus.BAD_REQUEST));
            subCaste.setReligion(religionEntity);
        }
        
        
//        subCaste = subCasteRepository.save(subCaste);
//
//        return new SubCasteResponseDTO(subCaste.getId(), subCaste.getSubCasteName(), 
//            new CasteResponseDTO(subCaste.getCaste().getId(), subCaste.getCaste().getCasteName(), 
//                new ReligionResponseDTO(subCaste.getReligion().getId(), subCaste.getReligion().getReligionName(), subCaste.getReligion().getReligionImage())));
        try {
            SubCasteEntity updatedSubCaste = subCasteRepository.save(subCaste);
            log.info("Successfully updated sub-caste in PostgreSQL: id={}, name={}", updatedSubCaste.getId(), updatedSubCaste.getSubCasteName());
            
            try {
                SubCasteMongo mongoDoc = new SubCasteMongo(updatedSubCaste);
                subCasteMongoRepository.save(mongoDoc);
                log.info("Successfully updated sub-caste in MongoDB: id={}, name={}", updatedSubCaste.getId(), updatedSubCaste.getSubCasteName());
            } catch (Exception mongoEx) {
                log.error("Failed to update sub-caste in MongoDB: id={}, name={}. Error: {}", 
                    updatedSubCaste.getId(), updatedSubCaste.getSubCasteName(), mongoEx.getMessage(), mongoEx);
                
                // Rollback PostgreSQL by throwing exception
                throw new RuntimeException("MongoDB update failed for sub-caste: " + updatedSubCaste.getSubCasteName() + 
                    ". Rolling back PostgreSQL transaction.", mongoEx);
            }
            
            return new SubCasteResponseDTO(
                    updatedSubCaste.getId(), 
                    updatedSubCaste.getSubCasteName(), 
                    new CasteResponseDTO(
                            updatedSubCaste.getCaste().getId(), 
                            updatedSubCaste.getCaste().getCasteName(), 
                            new ReligionResponseDTO(
                                    updatedSubCaste.getReligion().getId(), 
                                    updatedSubCaste.getReligion().getReligionName(), 
                                    updatedSubCaste.getReligion().getReligionImage(),
                                    updatedSubCaste.getReligion().getReligionColor()
                            )
                    )
            );
        } catch (Exception ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("MongoDB")) {
                // Re-throw MongoDB sync errors
                throw ex;
            }
            log.error("Failed to update sub-caste: id={}. Error: {}", subCasteId, ex.getMessage(), ex);
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    
    
    }


    @Transactional
    public void updateSubCasteOrder(Long electionId, Long accountId, List<SubCasteReorderRequest> reorderRequests) {
        // Fetch all sub-castes for the given election and account
        List<SubCasteEntity> subCastes = subCasteRepository.findByCaste_ElectionIdAndCaste_Religion_AccountId(electionId, accountId);

        if (subCastes.isEmpty()) {
            throw new ThedalException(ThedalError.SUBCASTE_NOT_FOUND, HttpStatus.NOT_FOUND);
        }

        // Create a reordered list
        List<SubCasteEntity> reorderedSubCastes = new ArrayList<>(subCastes);

        // Process each reorder request
        for (SubCasteReorderRequest request : reorderRequests) {
            SubCasteEntity movedSubCaste = reorderedSubCastes.stream()
                    .filter(sc -> sc.getId().equals(request.getSubCasteId()))
                    .findFirst()
                    .orElseThrow(() -> new ThedalException(ThedalError.SUBCASTE_NOT_FOUND, HttpStatus.NOT_FOUND));

            reorderedSubCastes.remove(movedSubCaste);
            
            // Ensure new index is within bounds
            int newIndex = Math.max(0, Math.min(request.getNewOrderIndex(), reorderedSubCastes.size()));
            
            reorderedSubCastes.add(newIndex, movedSubCaste);
        }

        // Reassign orderIndex values
        for (int i = 0; i < reorderedSubCastes.size(); i++) {
            reorderedSubCastes.get(i).setOrderIndex(i);
        }

        try {
            List<SubCasteEntity> savedSubCastes = subCasteRepository.saveAll(reorderedSubCastes);
            
            // Update MongoDB as well
            try {
                List<SubCasteMongo> mongoSubCastes = savedSubCastes.stream()
                        .map(SubCasteMongo::new)
                        .collect(Collectors.toList());
                subCasteMongoRepository.saveAll(mongoSubCastes);
                log.info("Updated sub-caste order in MongoDB for electionId: {}", electionId);
            } catch (Exception mongoEx) {
                log.error("Failed to update sub-caste order in MongoDB for electionId: {}", electionId, mongoEx);
                throw new RuntimeException("MongoDB reorder failed, triggering rollback", mongoEx);
            }
            
            log.info("Sub-caste order updated successfully for electionId: {}", electionId);
        } catch (Exception ex) {
            log.error("Failed to update sub-caste order for electionId: {}", electionId, ex);
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    
    @Transactional
    public ThedalResponse<List<Map<String, Object>>> getSubCasteByReligionAndCasteFromMongo(Long religionId, Long casteId, Long electionId) {
        Long accountId = requestDetails.getCurrentAccountId();

        if (accountId == null) {
            log.error("Account ID not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        log.info("Fetching subcastes from PostgreSQL for religionId: {}, casteId: {}, electionId: {}, accountId: {}", 
                 religionId, casteId, electionId, accountId);

        // Read from PostgreSQL instead of MongoDB
        List<SubCasteEntity> subCastes;
        if (religionId == null && casteId == null) {
            // Get all subcastes for the election
            subCastes = subCasteRepository.findByCaste_ElectionIdAndCaste_Religion_AccountId(electionId, accountId);
        } else if (religionId != null && casteId != null) {
            // Both parameters provided
            subCastes = subCasteRepository.findByReligionIdAndCasteIdAndReligion_AccountIdAndElectionIdOrderByUpdatedAtDescCreatedAtDesc(religionId, casteId, accountId, electionId);
        } else if (casteId != null) {
            // Only casteId provided
            subCastes = subCasteRepository.findByCasteIdAndCaste_Religion_AccountIdAndElectionIdOrderByUpdatedAtDescCreatedAtDesc(casteId, accountId, electionId);
        } else {
            // Only religionId provided
            subCastes = subCasteRepository.findByReligionIdAndReligion_AccountIdAndElectionIdOrderByUpdatedAtDescCreatedAtDesc(religionId, accountId, electionId);
        }

        if (subCastes.isEmpty()) {
            log.warn("No subcastes found in PostgreSQL for religionId: {}, casteId: {}, electionId: {}, accountId: {}", 
                     religionId, casteId, electionId, accountId);
            throw new ThedalException(ThedalError.SUBCASTES_NOT_FOUND, HttpStatus.NOT_FOUND);
        }

        log.debug("Found {} subcastes in PostgreSQL", subCastes.size());

        List<Map<String, Object>> subCasteDetails = subCastes.stream()
                .map(subCaste -> {
                    Map<String, Object> subCasteData = new HashMap<>();
                    subCasteData.put("subCasteId", subCaste.getId());
                    subCasteData.put("subCasteName", subCaste.getSubCasteName() != null ? subCaste.getSubCasteName() : "");
                    subCasteData.put("casteId", subCaste.getCaste() != null ? subCaste.getCaste().getId() : null);
                    subCasteData.put("casteName", subCaste.getCaste() != null && subCaste.getCaste().getCasteName() != null ? subCaste.getCaste().getCasteName() : "");
                    subCasteData.put("religionId", subCaste.getReligion() != null ? subCaste.getReligion().getId() : null);
                    subCasteData.put("religionName", subCaste.getReligion() != null && subCaste.getReligion().getReligionName() != null ? subCaste.getReligion().getReligionName() : "");
                    subCasteData.put("orderIndex", subCaste.getOrderIndex() != null ? subCaste.getOrderIndex() : 0);
                    subCasteData.put("voterCount", 0L);
                    subCasteData.put("electionId", subCaste.getElectionId());
                    return subCasteData;
                })
                .sorted(Comparator.comparingInt(subCaste -> (Integer) subCaste.get("orderIndex")))
                .collect(Collectors.toList());

        log.info("Successfully fetched {} subcastes from PostgreSQL for electionId: {}", subCasteDetails.size(), electionId);
        ThedalResponse<List<Map<String, Object>>> response = new ThedalResponse<>(ThedalSuccess.SUBCASTES_FETCHED, subCasteDetails);
        response.setStatus("success");
        return response;
    }



    @Transactional
    public ThedalResponse<Map<String, Object>> bulkUploadSubCastesFromXlsxOrCsv(MultipartFile file, Long electionId) {
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
            throw new ThedalException(ThedalError.INVALID_SUBCASTE_FILE_FORMAT, HttpStatus.BAD_REQUEST);
        }

        // Initial header validation
        Map<String, Integer> headerMapping;
        List<String> headerErrors = new ArrayList<>();
        try {
            if (file.getOriginalFilename().endsWith(".xlsx")) {
                Workbook workbook = new XSSFWorkbook(file.getInputStream());
                Sheet sheet = workbook.getSheetAt(0);
                headerMapping = subCasteFileUploadMethod.buildHeaderMapping(sheet.getRow(0));
            } else {
                BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream()));
                String[] headers = br.readLine().split(",");
                headerMapping = subCasteFileUploadMethod.buildCsvHeaderMapping(headers);
            }

            headerErrors = validateMandatoryHeaders(headerMapping);
            if (!headerErrors.isEmpty()) {
                throw new ThedalException(ThedalError.INVALID_SUBCASTE_FILE_FORMAT, HttpStatus.BAD_REQUEST,
                        "Missing mandatory headers: " + String.join(", ", headerErrors));
            }
        } catch (IOException e) {
            throw new ThedalException(ThedalError.FILE_PROCESSING_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        String folder = "subcaste_uploads";
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        String originalFileName = file.getOriginalFilename();
        String fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        String uniqueFileName = folder + "/subcaste_" + System.currentTimeMillis() + "_" + uniqueId + fileExtension;

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
            		subCasteFileUploadMethod.processSubCasteExcelFile(accountEntity, fileUrl, election, headerMapping) :
            			subCasteFileUploadMethod.processSubCasteCsvFile(accountEntity, fileUrl, election, headerMapping);

            long endTime = System.currentTimeMillis();
            result.put("totalTimeTaken", endTime - startTime);
            result.put("fileUrl", fileUrl);

            NotificationType completedNotification = notificationTemplate.bulkUploadCompleted(
                    fileEntity.getFileName(), electionId, null);
            notificationService.saveNotification(true, completedNotification);

            return new ThedalResponse<>(ThedalSuccess.BULK_SUBCASTES_UPLOADED, result);

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
        
        // sub_caste_name is always mandatory
        if (!headerMapping.containsKey("sub_caste_name")) {
            missingHeaders.add("sub_caste_name");
        }
        
        // Either caste_id or caste_name must be present
        if (!headerMapping.containsKey("caste_id") && !headerMapping.containsKey("caste_name")) {
            missingHeaders.add("caste_id or caste_name");
        }
        
        // Either religion_id or religion_name must be present  
        if (!headerMapping.containsKey("religion_id") && !headerMapping.containsKey("religion_name")) {
            missingHeaders.add("religion_id or religion_name");
        }
        
        return missingHeaders;
    }

    private boolean isSupportedFormat(String originalFileName) {
        return originalFileName != null && (originalFileName.endsWith(".xlsx") || originalFileName.endsWith(".csv"));
    }




	
}