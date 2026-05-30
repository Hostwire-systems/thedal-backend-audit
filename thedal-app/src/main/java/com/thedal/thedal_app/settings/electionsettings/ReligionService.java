package com.thedal.thedal_app.settings.electionsettings;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
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
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.thedal.thedal_app.awsfilestore.AwsFileUpload;
import com.thedal.thedal_app.cpanel.ReligionFileUploadService;
import com.thedal.thedal_app.election.ElectionRepository;
import com.thedal.thedal_app.files.Files;
import com.thedal.thedal_app.files.FilesRepository;
import com.thedal.thedal_app.files.HandlerType;
import com.thedal.thedal_app.general.RequestDetailsService;
import com.thedal.thedal_app.response.ThedalResponse;
import com.thedal.thedal_app.response.ThedalSuccess;
import com.thedal.thedal_app.settings.electionsettings.dto.ReligionReorderRequest;
import com.thedal.thedal_app.settings.electionsettings.dto.ReligionRequest;
import com.thedal.thedal_app.settings.electionsettings.dto.ReligionResponseDTO;
import com.thedal.thedal_app.settings.electionsettings.dto.ReligionUpdateRequest;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;
import com.thedal.thedal_app.util.RandomTokenGenerator;
import com.thedal.thedal_app.voter.BulkUploadStatus;
import com.thedal.thedal_app.voter.VoterRepo;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ReligionService {

    @Autowired
    private ReligionRepository religionRepository;  
    @Autowired
    private RequestDetailsService requestDetails;
    @Autowired
    private CasteRepository casteRepository;
    @Autowired
    private SubCasteRepository subCasteRepository;
    @Autowired
	private ElectionRepository electionRepository;
    @Autowired
    private AwsFileUpload awsFileUpload;
    @Value("${aws.s3.banner.bucket}")
	private String s3bucket;
    @Autowired
    private VoterRepo voterRepository; 
    @Autowired
    private SectionBulkUploadRepository sectionBulkUploadRepository;
    @Autowired
    private FilesRepository filesRepository;
    @Autowired
    private ReligionFileUploadService religionFileUploadService;
    @Value("${aws.s3.files.bucket}")
	private String s3Filesbucket;
    @Autowired
    private ReligionMongoRepository religionMongoRepository;
    

    @Transactional
    public ThedalResponse<ReligionEntity> createReligion(ReligionRequest religionRequest, Long electionId, Long accountId) {
        validateReligionRequest(religionRequest);
        checkDuplicate(religionRequest.getReligionName(), accountId, electionId);

        String imageUrl;
        try {
            imageUrl = uploadReligionImageToAWS(religionRequest.getReligionImage());
        } catch (Exception ex) {
            throw new ThedalException(ThedalError.UPLOAD_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // Increment orderIndex for all existing records
        religionRepository.incrementOrderIndexes(electionId);

        // Create new entity with orderIndex = 0
        ReligionEntity religionEntity = new ReligionEntity();
        religionEntity.setReligionName(religionRequest.getReligionName());
        religionEntity.setReligionImage(imageUrl);
        religionEntity.setReligionColor(religionRequest.getReligionColor());
        religionEntity.setAccountId(accountId);
        religionEntity.setElectionId(electionId);
        religionEntity.setOrderIndex(0);

//        religionRepository.saveAndFlush(religionEntity);
//        log.info("Religion created successfully at top position: {}", religionEntity.getReligionName());
//
//        return new ThedalResponse<>(ThedalSuccess.RELIGION_CREATED, religionEntity);
        try {
            ReligionEntity savedReligion = religionRepository.saveAndFlush(religionEntity);
            // MongoDB write disabled to prevent rollback on connection failure
            // try {
            //     ReligionMongo religionMongo = new ReligionMongo(savedReligion);
            //     religionMongoRepository.save(religionMongo);
            //     log.info("Successfully saved religion to MongoDB: id={}, name={}", savedReligion.getId(), savedReligion.getReligionName());
            // } catch (Exception mongoEx) {
            //     log.error("Failed to save religion to MongoDB: id={}, name={}", savedReligion.getId(), savedReligion.getReligionName(), mongoEx);
            //     throw new RuntimeException("MongoDB save failed, triggering rollback", mongoEx);
            // }
            log.info("Religion created successfully at top position: {}", savedReligion.getReligionName());
            return new ThedalResponse<>(ThedalSuccess.RELIGION_CREATED, savedReligion);
        } catch (Exception ex) {
            log.error("Failed to create religion: {}", religionRequest.getReligionName(), ex);
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        
    }


    private void validateReligionRequest(ReligionRequest religionRequest) {
        if (religionRequest.getReligionName() == null || religionRequest.getReligionName().isEmpty()) {
            throw new ThedalException(ThedalError.MISSING_REQUIRED_FIELDS, HttpStatus.BAD_REQUEST);
        }
        if (religionRequest.getReligionImage() == null || !isValidImageFormat(religionRequest.getReligionImage())) {
            throw new ThedalException(ThedalError.INVALID_IMAGE_FORMAT, HttpStatus.BAD_REQUEST);
        }
    }

    private boolean isValidImageFormat(MultipartFile image) {
        String contentType = image.getContentType();
        return contentType != null && (contentType.equals("image/jpeg") || contentType.equals("image/png"));
    }

    // private void checkDuplicateReligion(String religionName, Long accountId, Long electionId) {
    //     // Check if the religion already exists within the scope of the current election and account
    //     boolean exists = religionRepository.existsByReligionNameAndAccountIdAndElectionId(religionName, accountId, electionId);
    //     if (exists) {
    //         throw new ThedalException(ThedalError.DUPLICATE_RELIGION, HttpStatus.CONFLICT);
    //     }
    // }
    // private void checkDuplicateReligion(String religionName, Long accountId, Long electionId) {
    //     // Find the existing religion with the same name, accountId, and electionId
    //     Optional<ReligionEntity> existingReligion = religionRepository
    //             .findByReligionNameAndAccountIdAndElectionId(religionName, accountId, electionId);
    
    //             if (existingReligion.isPresent()) {
    //                 String errorMessage = String.format("Religion with name '%s' already exists in election '%d'.", religionName, electionId);
    //                 log.error(errorMessage);
    //                 throw new ThedalException(ThedalError.DUPLICATE_RELIGION, HttpStatus.CONFLICT, errorMessage);
    //             }
    // } 
    private void checkDuplicate(String religionName, Long accountId, Long electionId) {
        Optional<ReligionEntity> existingReligion = religionRepository
            .findByReligionNameAndAccountIdAndElectionId(religionName, accountId, electionId);
    
        if (existingReligion.isPresent()) {
            String errorMessage = String.format("Religion with name '%s' already exists in election '%d'.", religionName, electionId);
            log.error(errorMessage);
            throw new ThedalException(ThedalError.DUPLICATE_RELIGION, HttpStatus.CONFLICT, errorMessage);
        }
    }
    
    private void checkDuplicateReligion(String religionName, Long accountId, Long electionId, Long religionIdToExclude) {
        Optional<ReligionEntity> existingReligion = religionRepository
            .findByReligionNameAndAccountIdAndElectionIdAndIdNot(religionName, accountId, electionId, religionIdToExclude);
    
        if (existingReligion.isPresent()) {
            String errorMessage = String.format("Religion with name '%s' already exists in election '%d'.", religionName, electionId);
            log.error(errorMessage);
            throw new ThedalException(ThedalError.DUPLICATE_RELIGION, HttpStatus.CONFLICT, errorMessage);
        }
    }

    private String uploadReligionImageToAWS(MultipartFile imageFile) {
        String contentType = imageFile.getContentType();
        if (!(MediaType.IMAGE_JPEG_VALUE.equals(contentType) ||
                MediaType.IMAGE_PNG_VALUE.equals(contentType))) {
            throw new ThedalException(ThedalError.INVALID_IMAGE_FORMAT, HttpStatus.BAD_REQUEST);
        }

        long maxFileSize = 5 * 1024 * 1024; // 5MB
        if (imageFile.getSize() > maxFileSize) {
            throw new ThedalException(ThedalError.INVALID_IMAGE_FORMAT, HttpStatus.BAD_REQUEST);
        }

        // Generate a unique file name
        String fileExtension = "." + awsFileUpload.getFileExtension(imageFile.getOriginalFilename());
        String fileName = "religion_" + System.currentTimeMillis() + "_" + RandomTokenGenerator.generateToken(10) + fileExtension;

        // Upload to AWS S3
        try {
            File tempFile = File.createTempFile("temp", fileExtension);
            try (FileOutputStream fileOutputStream = new FileOutputStream(tempFile)) {
                fileOutputStream.write(imageFile.getBytes());
            }

            String awsUrl = awsFileUpload.uploadToAWS(tempFile, fileName, s3bucket);

            if (!tempFile.delete()) {
                log.warn("Temporary file deletion failed: {}", tempFile.getName());
            }

            return awsUrl;
        } catch (IOException e) {
            log.error("Error uploading image to AWS S3", e);
            throw new ThedalException(ThedalError.UPLOAD_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    


    public List<ReligionEntity> getAllReligions(Long accountId, Long electionId) {
    	
    	log.info("Fetching all religions for account ID: {} and election ID: {}", accountId, electionId);

//        // Fetch religions specific to the account and election
//        return religionRepository.findByAccountIdAndElectionId(accountId, electionId);
    	// List<ReligionEntity> religions = religionRepository.findByAccountIdAndElectionId(accountId, electionId);

        // // Sort by orderIndex to ensure correct ordering in GET response
        // religions.sort(Comparator.comparing(ReligionEntity::getOrderIndex));

        // return religions; 
        
    List<ReligionEntity> religions = religionRepository.findByAccountIdAndElectionId(accountId, electionId);

    // Sort by updatedAt (descending), fallback to createdAt
    religions.sort(Comparator
        .comparing(ReligionEntity::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
        .thenComparing(ReligionEntity::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
    );

    return religions;
        
    }
    
    @Transactional
    public ThedalResponse<List<Map<String, Object>>> getAllReligionsWithVoterCount(Long accountId, Long electionId) {
        log.info("Fetching all religions with voter count for account ID: {} and election ID: {}", accountId, electionId);

        List<Object[]> results = religionRepository.findReligionsWithVoterCount(accountId, electionId);
        if (results.isEmpty()) {
            log.warn("No religions found for account ID: {} and election ID: {}", accountId, electionId);
            throw new ThedalException(ThedalError.RELIGIONS_NOT_FOUND, HttpStatus.NOT_FOUND);
        }

        List<Map<String, Object>> religionDetails = results.stream()
                .map(result -> {
                    ReligionEntity religion = (ReligionEntity) result[0];
                    Long voterCount = (Long) result[1];
                    Map<String, Object> religionData = new HashMap<>();
                    religionData.put("religionId", religion.getId());
                    religionData.put("religionName", religion.getReligionName() != null ? religion.getReligionName() : "");
                    religionData.put("religionImage", religion.getReligionImage() != null ? religion.getReligionImage() : "");
                    religionData.put("religionColor", religion.getReligionColor() != null ? religion.getReligionColor() : "");
                    religionData.put("orderIndex", religion.getOrderIndex() != null ? religion.getOrderIndex() : 0);
                    religionData.put("voterCount", voterCount);
                    religionData.put("updatedAt", religion.getUpdatedAt()); // For sorting
                    religionData.put("createdAt", religion.getCreatedAt()); // For sorting
                    return religionData;
                })
                .sorted(Comparator
                        .comparing((Map<String, Object> m) -> (LocalDateTime) m.get("updatedAt"), Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing((Map<String, Object> m) -> (LocalDateTime) m.get("createdAt"), Comparator.nullsLast(Comparator.reverseOrder())))
                .map(map -> {
                    // Remove temporary sorting fields
                    map.remove("updatedAt");
                    map.remove("createdAt");
                    return map;
                })
                .collect(Collectors.toList());

        log.info("Successfully fetched {} religions for electionId: {}", religionDetails.size(), electionId);
        return new ThedalResponse<>(ThedalSuccess.RELIGIONS_FETCHED, religionDetails);
    }
     

//@Transactional
//public void deleteReligionsByIdsAndAccountIdAndElectionId(List<Long> religionIds, Long accountId, Long electionId) {
//    log.info("Deleting religions with IDs: {} for account ID: {} and election ID: {}", religionIds, accountId, electionId);
//
//    List<ReligionEntity> religions;
//
//    if (religionIds == null || religionIds.isEmpty()) {
//        // Fetch all religions for the given account and election
//        religions = religionRepository.findByAccountIdAndElectionId(accountId, electionId);
//    } else {
//        // Fetch only the given religions
//        religions = religionRepository.findByIdInAndAccountIdAndElectionId(religionIds, accountId, electionId);
//    }
//
//    if (religions.isEmpty()) {
//        throw new ThedalException(ThedalError.RELIGION_NOT_FOUND, HttpStatus.NOT_FOUND);
//    }
//
//    for (ReligionEntity religion : religions) {
//        Long religionId = religion.getId();
//        String religionName = religion.getReligionName();
//        // Check if linked to voters
//        if (voterRepository.existsByReligionIdAndAccountIdAndElectionId(religionId, accountId, electionId)) {
//            throw new ThedalException(ThedalError.RELIGION_LINKED_TO_VOTERS, HttpStatus.CONFLICT);
//        }
//
//        // // Check if linked to castes
//        // if (casteRepository.existsByReligionIdAndAccountIdAndElectionId(religionId, accountId, electionId)) {
//        //     throw new ThedalException(ThedalError.RELIGION_LINKED_TO_CASTE_OR_SUBCASTE, HttpStatus.CONFLICT);
//        // }
//
//        // // Check if linked to subcastes
//        // if (subCasteRepository.existsByReligionIdAndAccountIdAndElectionId(religionId, accountId, electionId)) {
//        //     throw new ThedalException(ThedalError.RELIGION_LINKED_TO_CASTE_OR_SUBCASTE, HttpStatus.CONFLICT);
//        // }
//        // Check if linked to castes
//        List<CasteEntity> linkedCastes = casteRepository.findByReligionIdAndAccountIdAndElectionId(religionId, accountId, electionId);
//        if (!linkedCastes.isEmpty()) {
//            String casteNames = linkedCastes.stream()
//                    .map(CasteEntity::getCasteName)
//                    .collect(Collectors.joining(", "));
//            throw new ThedalException(ThedalError.RELIGION_LINKED_TO_CASTE_OR_SUBCASTE, HttpStatus.CONFLICT, 
//                    String.format("Cannot delete religion '%s' because it is linked to castes: %s.", religion.getReligionName(), casteNames));
//        }
//
//        // Check if linked to subcastes
//        List<SubCasteEntity> linkedSubCastes = subCasteRepository.findByReligionIdAndAccountIdAndElectionId(religionId, accountId, electionId);
//        if (!linkedSubCastes.isEmpty()) {
//            String subCasteNames = linkedSubCastes.stream()
//                    .map(SubCasteEntity::getSubCasteName)
//                    .collect(Collectors.joining(", "));
//            throw new ThedalException(ThedalError.RELIGION_LINKED_TO_CASTE_OR_SUBCASTE, HttpStatus.CONFLICT, 
//                    String.format("Cannot delete religion '%s' because it is linked to subcastes: %s.", religion.getReligionName(), subCasteNames));
//        }
//    }
//
//    // Delete religions
//    religionRepository.deleteAll(religions);
//}
    @Transactional
    public void deleteReligionsByIdsAndAccountIdAndElectionId(List<Long> religionIds, Long accountId, Long electionId) {
        log.info("Deleting religions with IDs: {} for account ID: {} and election ID: {}", religionIds, accountId, electionId);

        List<ReligionEntity> religions;

        if (religionIds == null || religionIds.isEmpty()) {
            religions = religionRepository.findByAccountIdAndElectionId(accountId, electionId);
        } else {
            religions = religionRepository.findByIdInAndAccountIdAndElectionId(religionIds, accountId, electionId);
        }

        if (religions.isEmpty()) {
            throw new ThedalException(ThedalError.RELIGION_NOT_FOUND, HttpStatus.NOT_FOUND);
        }

        for (ReligionEntity religion : religions) {
            Long religionId = religion.getId();
            String religionName = religion.getReligionName();

            if (voterRepository.existsByReligionIdAndAccountIdAndElectionId(religionId, accountId, electionId)) {
                throw new ThedalException(ThedalError.RELIGION_LINKED_TO_VOTERS, HttpStatus.CONFLICT,
                        String.format("Cannot delete religion '%s' because it is linked to voters.", religionName));
            }

            List<CasteEntity> linkedCastes = casteRepository.findByReligionIdAndAccountIdAndElectionId(religionId, accountId, electionId);
            if (!linkedCastes.isEmpty()) {
                String casteNames = linkedCastes.stream()
                        .map(CasteEntity::getCasteName)
                        .collect(Collectors.joining(", "));
                throw new ThedalException(ThedalError.RELIGION_LINKED_TO_CASTE_OR_SUBCASTE, HttpStatus.CONFLICT,
                        String.format("Cannot delete religion '%s' because it is linked to castes: %s.", religionName, casteNames));
            }

            List<SubCasteEntity> linkedSubCastes = subCasteRepository.findByReligionIdAndAccountIdAndElectionId(religionId, accountId, electionId);
            if (!linkedSubCastes.isEmpty()) {
                String subCasteNames = linkedSubCastes.stream()
                        .map(SubCasteEntity::getSubCasteName)
                        .collect(Collectors.joining(", "));
                throw new ThedalException(ThedalError.RELIGION_LINKED_TO_CASTE_OR_SUBCASTE, HttpStatus.CONFLICT,
                        String.format("Cannot delete religion '%s' because it is linked to subcastes: %s.", religionName, subCasteNames));
            }

            if (religion.getReligionImage() != null && !religion.getReligionImage().isEmpty()) {
                String objectKey = AwsFileUpload.getKeyFromUrl(religion.getReligionImage());
                if (objectKey != null && !objectKey.isEmpty()) {
                    awsFileUpload.deleteS3Object(s3bucket, objectKey);
                    log.info("Deleted image for religion ID: {} from AWS S3", religionId);
                }
            }
        }

        try {
            religionRepository.deleteAll(religions);
            religionRepository.flush();
            // MongoDB write disabled to prevent rollback on connection failure
            // try {
            //     if (religionIds == null || religionIds.isEmpty()) {
            //         religionMongoRepository.deleteByAccountIdAndElectionId(accountId, electionId);
            //         log.info("Deleted all religions from MongoDB for accountId: {}, electionId: {}", accountId, electionId);
            //     } else {
            //         religionMongoRepository.deleteByIdIn(religionIds);
            //         log.info("Deleted religions from MongoDB: ids={}", religionIds);
            //     }
            // } catch (Exception mongoEx) {
            //     log.error("Failed to delete religions from MongoDB: ids={}", religionIds, mongoEx);
            //     throw new RuntimeException("MongoDB deletion failed, triggering rollback", mongoEx);
            // }
            log.info("Religions deleted successfully: ids={}", religionIds);
        } catch (Exception ex) {
            log.error("Failed to delete religions: ids={}", religionIds, ex);
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


@Transactional
public ReligionResponseDTO updateReligion(Long religionId, Long electionId, Long accountId, ReligionUpdateRequest religionRequest) {
    ReligionEntity religion = religionRepository.findByIdAndAccountIdAndElectionId(religionId, accountId, electionId)
            .orElseThrow(() -> new ThedalException(ThedalError.RELIGION_NOT_FOUND, HttpStatus.NOT_FOUND));

     if (religionRequest.getReligionName() != null && !religionRequest.getReligionName().isEmpty()) {
        if (!religionRequest.getReligionName().equalsIgnoreCase(religion.getReligionName())) {
                checkDuplicateReligion(religionRequest.getReligionName(), accountId, electionId, religion.getId()); 
        
         }
            religion.setReligionName(religionRequest.getReligionName());
    }

    if (religionRequest.getReligionImage() != null && !religionRequest.getReligionImage().isEmpty()) {
        String updatedImageUrl = uploadReligionImageToAWS(religionRequest.getReligionImage());
        religion.setReligionImage(updatedImageUrl);
    }

    if (religionRequest.getReligionColor() != null && !religionRequest.getReligionColor().isEmpty()) {
        religion.setReligionColor(religionRequest.getReligionColor());
    }

    // Increment orderIndex for all existing records
    religionRepository.incrementOrderIndexes(electionId);

    // Move updated record to the top by setting orderIndex = 0
    religion.setOrderIndex(0);

    try {
        ReligionEntity updatedReligion = religionRepository.saveAndFlush(religion);
        // MongoDB write disabled to prevent rollback on connection failure
        // try {
        //     ReligionMongo religionMongo = new ReligionMongo(updatedReligion);
        //     religionMongoRepository.save(religionMongo);
        //     log.info("Successfully updated religion in MongoDB: id={}, name={}", religionId, updatedReligion.getReligionName());
        // } catch (Exception mongoEx) {
        //     log.error("Failed to update religion in MongoDB: id={}, name={}", religionId, updatedReligion.getReligionName(), mongoEx);
        //     throw new RuntimeException("MongoDB update failed, triggering rollback", mongoEx);
        // }
        log.info("Religion updated and moved to top: {}", updatedReligion.getReligionName());
        return new ReligionResponseDTO(updatedReligion.getId(), updatedReligion.getReligionName(), updatedReligion.getReligionImage(), updatedReligion.getReligionColor());
    } catch (Exception ex) {
        log.error("Failed to update religion: id={}", religionId, ex);
        throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

   
//    @Transactional
//    public List<ReligionEntity> updateReligionOrder(Long electionId, Long accountId, Long religionId, int newIndex) {
//        // Fetch all religions for the given election and account
//        List<ReligionEntity> religions = religionRepository.findByElectionIdAndAccountId(electionId, accountId);
//
//        // Sort them by the existing orderIndex
//        religions.sort(Comparator.comparing(ReligionEntity::getOrderIndex));
//
//        // Find the religion to move
//        ReligionEntity movedReligion = religions.stream()
//            .filter(r -> r.getId().equals(religionId))
//            .findFirst()
//            .orElseThrow(() -> new ThedalException(ThedalError.RELIGION_NOT_FOUND, HttpStatus.NOT_FOUND));
//
//        // Remove it from the list
//        religions.remove(movedReligion);
//
//        // Ensure newIndex is within bounds
//        newIndex = Math.max(0, Math.min(newIndex, religions.size()));
//
//        // Insert it at the new index
//        religions.add(newIndex, movedReligion);
//
//        // **Fix: Update all order indexes properly**
//        for (int i = 0; i < religions.size(); i++) {
//            religions.get(i).setOrderIndex(i);
//        }
//
//        // **Fix: Save all religions again in the correct order**
//        List<ReligionEntity> updatedReligions = religionRepository.saveAll(religions);
//
//        log.info("Religion order updated successfully. New order: {}", updatedReligions);
//
//        return updatedReligions;
//    }
    @Transactional
    public List<ReligionEntity> updateReligionOrder(List<ReligionReorderRequest> reorderRequests, Long electionId, Long accountId) {
        // Fetch all religions for the given election and account
        List<ReligionEntity> religions = religionRepository.findByElectionIdAndAccountId(electionId, accountId);

        if (religions.isEmpty()) {
            log.error("No religions found for election ID {} and account ID {}", electionId, accountId);
            throw new ThedalException(ThedalError.RELIGION_NOT_FOUND, HttpStatus.NOT_FOUND);
        }

        // Create a map of the new order based on request data
        Map<Long, Integer> newOrderMap = reorderRequests.stream()
                .collect(Collectors.toMap(ReligionReorderRequest::getReligionId, ReligionReorderRequest::getNewOrderIndex));

        // Sort the religions by orderIndex
        religions.sort(Comparator.comparing(ReligionEntity::getOrderIndex));

        // List to store reordered religions
        List<ReligionEntity> reorderedReligions = new ArrayList<>(religions);

        // Remove the religions from reorderedReligions if they are present in newOrderMap
        reorderedReligions.removeIf(religion -> newOrderMap.containsKey(religion.getId()));

        // Reorder religions based on the new order index
        for (ReligionReorderRequest request : reorderRequests) {
            // Find the religion to reorder
            ReligionEntity religion = religions.stream()
                    .filter(r -> r.getId().equals(request.getReligionId()))
                    .findFirst()
                    .orElseThrow(() -> new ThedalException(ThedalError.RELIGION_NOT_FOUND, HttpStatus.NOT_FOUND));

            // Set the new order index
            religion.setOrderIndex(request.getNewOrderIndex());

            // Add religion to the list at the new order index
            reorderedReligions.add(request.getNewOrderIndex(), religion);
        }

        // Update orderIndex for all religions and ensure full update for each
        for (int i = 0; i < reorderedReligions.size(); i++) {
            ReligionEntity religion = reorderedReligions.get(i);
            religion.setOrderIndex(i); // Set the new order index
            log.info("Updated religion order: {} -> {}", religion.getReligionName(), i);
        }

        try {
            List<ReligionEntity> savedReligions = religionRepository.saveAll(reorderedReligions);
            
            // MongoDB write disabled to prevent rollback on connection failure
            // try {
            //     List<ReligionMongo> mongoReligions = savedReligions.stream()
            //             .map(ReligionMongo::new)
            //             .collect(Collectors.toList())
            //     religionMongoRepository.saveAll(mongoReligions);
            //     log.info("Updated religion order in MongoDB for electionId: {}", electionId);
            // } catch (Exception mongoEx) {
            //     log.error("Failed to update religion order in MongoDB for electionId: {}", electionId, mongoEx);
            //     throw new RuntimeException("MongoDB reorder failed, triggering rollback", mongoEx);
            // }
            
            log.info("Religion order updated successfully for electionId: {}", electionId);
            return savedReligions;
        } catch (Exception ex) {
            log.error("Failed to update religion order for electionId: {}", electionId, ex);
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @Transactional
    public ThedalResponse<SectionBulkUploadEntity> uploadCpanelReligions(MultipartFile file) {
        long startTime = System.currentTimeMillis();
        Long electionId = 0L; // Hardcoded for cPanel
        Long accountId = 0L;  // Hardcoded for cPanel

        if (!isSupportedFormat(file.getOriginalFilename()) || file.isEmpty()) {
            throw new ThedalException(ThedalError.INVALID_FILE_FORMAT, HttpStatus.BAD_REQUEST);
        }

        Map<String, Integer> headerMapping;
        try {
            if (file.getOriginalFilename().endsWith(".xlsx")) {
                Workbook workbook = new XSSFWorkbook(file.getInputStream());
                Sheet sheet = workbook.getSheetAt(0);
                headerMapping = religionFileUploadService.buildHeaderMapping(sheet.getRow(0));
            } else {
                BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream()));
                String[] headers = br.readLine().split(",");
                headerMapping = religionFileUploadService.buildCsvHeaderMapping(headers);
            }
            List<String> headerErrors = validateMandatoryHeaders(headerMapping);
            if (!headerErrors.isEmpty()) {
                throw new ThedalException(ThedalError.INVALID_FILE_FORMAT, HttpStatus.BAD_REQUEST,
                        "Missing mandatory headers: " + String.join(", ", headerErrors));
            }
        } catch (IOException e) {
            throw new ThedalException(ThedalError.FILE_PROCESSING_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        String folder = "cpanel_religion_uploads";
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        String originalFileName = file.getOriginalFilename();
        String fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        String uniqueFileName = folder + "/religion_" + System.currentTimeMillis() + "_" + uniqueId + fileExtension;

        String fileUrl = null;
        SectionBulkUploadEntity bulkUploadEntity = null;

        try {
            fileUrl = awsFileUpload.uploadMultipartFile(file, uniqueFileName, s3Filesbucket);
            log.info("File uploaded to S3 at: {}", fileUrl);

            bulkUploadEntity = new SectionBulkUploadEntity();
            bulkUploadEntity.setAccountId(accountId);
            bulkUploadEntity.setElectionId(electionId);
            bulkUploadEntity.setStartTime(LocalDateTime.now());
            bulkUploadEntity.setStatus(BulkUploadStatus.IN_PROGRESS);
            bulkUploadEntity.setTotalRecords(0L);
            bulkUploadEntity.setTotalProcessedRecords(0L);
            bulkUploadEntity.setTotalSuccessRecords(0L);
            bulkUploadEntity.setTotalFailedRecords(0L);
            sectionBulkUploadRepository.save(bulkUploadEntity);

            Files fileEntity = new Files(HandlerType.BULKUPLOAD_FILES, bulkUploadEntity.getId(), originalFileName, fileUrl);
            filesRepository.save(fileEntity);
            bulkUploadEntity.setFile(fileEntity);

            if (fileExtension.equalsIgnoreCase(".xlsx")) {
                religionFileUploadService.processCpanelReligionExcelFile(bulkUploadEntity, fileUrl);
            } else if (fileExtension.equalsIgnoreCase(".csv")) {
                religionFileUploadService.processCpanelReligionCsvFile(bulkUploadEntity, fileUrl);
            }

            long endTime = System.currentTimeMillis();
            bulkUploadEntity.setTotalTimeTaken(endTime - startTime);
            bulkUploadEntity.setEndTime(LocalDateTime.now());
            bulkUploadEntity.setStatus(BulkUploadStatus.COMPLETED);
            sectionBulkUploadRepository.save(bulkUploadEntity);

            return new ThedalResponse<>(ThedalSuccess.BULK_RELIGIONS_UPLOADED, bulkUploadEntity);

        } catch (IOException e) {
            log.error("Error uploading file to S3", e);
            if (bulkUploadEntity != null) {
                bulkUploadEntity.setStatus(BulkUploadStatus.FAILED);
                bulkUploadEntity.setEndTime(LocalDateTime.now());
                bulkUploadEntity.setTotalTimeTaken(System.currentTimeMillis() - startTime);
                sectionBulkUploadRepository.save(bulkUploadEntity);
            }
            throw new ThedalException(ThedalError.FILE_PROCESSING_FAILED, HttpStatus.INTERNAL_SERVER_ERROR, "File upload to S3 failed.");
        } catch (Exception e) {
            log.error("Unexpected error processing file '{}': {}", originalFileName, e.getMessage(), e);
            if (bulkUploadEntity != null) {
                bulkUploadEntity.setStatus(BulkUploadStatus.FAILED);
                bulkUploadEntity.setEndTime(LocalDateTime.now());
                bulkUploadEntity.setTotalTimeTaken(System.currentTimeMillis() - startTime);
                sectionBulkUploadRepository.save(bulkUploadEntity);
            }
            throw new ThedalException(ThedalError.FILE_PROCESSING_FAILED, HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred: " + e.getMessage());
        }
    }

    private List<String> validateMandatoryHeaders(Map<String, Integer> headerMapping) {
        List<String> missingHeaders = new ArrayList<>();
        String[] mandatoryHeaders = {"religion_name"};
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
    
    
    @Transactional
    public ThedalResponse<List<Map<String, Object>>> getAllReligionsWithVoterCountFromMongo(Long accountId, Long electionId) {
        log.info("Fetching all religions from PostgreSQL for account ID: {} and election ID: {}", accountId, electionId);

        // Read from PostgreSQL instead of MongoDB
        List<ReligionEntity> religions = religionRepository.findByElectionIdAndAccountIdOrderByOrderIndex(electionId, accountId);
        if (religions.isEmpty()) {
            log.warn("No religions found in PostgreSQL for account ID: {} and election ID: {}", accountId, electionId);
            throw new ThedalException(ThedalError.RELIGIONS_NOT_FOUND, HttpStatus.NOT_FOUND);
        }

        List<Map<String, Object>> religionDetails = religions.stream()
                .map(religion -> {
                    Map<String, Object> religionData = new HashMap<>();
                    religionData.put("religionId", religion.getId());
                    religionData.put("religionName", religion.getReligionName() != null ? religion.getReligionName() : "");
                    religionData.put("religionImage", religion.getReligionImage() != null ? religion.getReligionImage() : "");
                    religionData.put("religionColor", religion.getReligionColor() != null ? religion.getReligionColor() : "");
                    religionData.put("orderIndex", religion.getOrderIndex() != null ? religion.getOrderIndex() : 0);
                    religionData.put("voterCount", 0L); // No voter count in this method
                    return religionData;
                })
                .sorted(Comparator.comparing(m -> (Integer) m.get("orderIndex")))
                .collect(Collectors.toList());

        log.info("Successfully fetched {} religions from PostgreSQL for electionId: {}", religionDetails.size(), electionId);
        return new ThedalResponse<>(ThedalSuccess.RELIGIONS_FETCHED, religionDetails);
    }
  
}
