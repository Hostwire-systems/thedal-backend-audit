package com.thedal.thedal_app.cpanel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.thedal.thedal_app.awsfilestore.AwsFileUpload;
import com.thedal.thedal_app.cpanel.dtos.VoterHistoryEntity;
import com.thedal.thedal_app.response.ThedalResponse;
import com.thedal.thedal_app.response.ThedalSuccess;
import com.thedal.thedal_app.settings.electionsettings.VoterHistoryMongo;
import com.thedal.thedal_app.settings.electionsettings.VoterHistoryMongoRepository;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;
import com.thedal.thedal_app.util.RandomTokenGenerator;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class VoterHistoryService {

    @Autowired
    private VoterHistoryRepository voterHistoryRepository;

    @Autowired
    private AwsFileUpload awsFileUpload;

    @Value("${aws.s3.files.bucket}")
    private String s3FilesBucket;
    @Autowired
    private VoterHistoryMongoRepository voterHistoryMongoRepository;

    @Transactional
    public ThedalResponse<VoterHistoryEntity>createVoterHistory(VoterHistoryRequest voterHistoryRequest, Long electionId, Long accountId) {
        // Validate the request
        validateVoterHistoryRequest(voterHistoryRequest);

        // Check for duplicate voter history
        checkDuplicateVoterHistory(voterHistoryRequest.getVoterHistoryName(), accountId, electionId);

        // Upload image to AWS S3
        String imageUrl = null;
        if (voterHistoryRequest.getVoterHistoryImage() != null) {
            imageUrl = uploadVoterHistoryImageToAWS(voterHistoryRequest.getVoterHistoryImage());
        }

        // Integer maxOrderIndex = voterHistoryRepository.findMaxOrderIndexByElectionId(electionId);
        // int nextOrderIndex = (maxOrderIndex != null) ? maxOrderIndex + 1 : 0;
        Integer minOrderIndex = voterHistoryRepository.findMinOrderIndexByElectionId(electionId);
    int newOrderIndex = (minOrderIndex != null) ? minOrderIndex - 1 : 0;

        // Create and save voter history entity
        VoterHistoryEntity voterHistoryEntity = new VoterHistoryEntity();
        voterHistoryEntity.setVoterHistoryName(voterHistoryRequest.getVoterHistoryName());
        voterHistoryEntity.setVoterHistoryImage(imageUrl);
        voterHistoryEntity.setAccountId(accountId);
        voterHistoryEntity.setElectionId(electionId);
        voterHistoryEntity.setOrderIndex(newOrderIndex);
//        voterHistoryRepository.saveAndFlush(voterHistoryEntity);
//
//        return new ThedalResponse<>(ThedalSuccess.VOTER_HISTORY_CREATED, voterHistoryEntity);
        try {
            VoterHistoryEntity saved = voterHistoryRepository.saveAndFlush(voterHistoryEntity);
            try {
                voterHistoryMongoRepository.save(new VoterHistoryMongo(saved));
                log.info("Saved voter history to MongoDB: id={}", saved.getId());
            } catch (Exception mongoEx) {
                log.error("Failed to save voter history to MongoDB: id={}", saved.getId(), mongoEx);
                throw new RuntimeException("MongoDB save failed, triggering rollback", mongoEx);
            }
            return new ThedalResponse<>(ThedalSuccess.VOTER_HISTORY_CREATED, saved);
        } catch (Exception ex) {
            log.error("Failed to create voter history: {}", voterHistoryRequest.getVoterHistoryName(), ex);
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        
        
    }

    private void validateVoterHistoryRequest(VoterHistoryRequest voterHistoryRequest) {
        if (voterHistoryRequest.getVoterHistoryName() == null || voterHistoryRequest.getVoterHistoryName().isEmpty()) {
            throw new ThedalException(ThedalError.MISSING_REQUIRED_FIELDS, HttpStatus.BAD_REQUEST);
        }
    }

    private void checkDuplicateVoterHistory(String voterHistoryName, Long accountId, Long electionId) {
        // Check in PostgreSQL first
        Optional<VoterHistoryEntity> existingVoterHistory = voterHistoryRepository.findByVoterHistoryNameAndAccountIdAndElectionId(voterHistoryName, accountId, electionId);
        if (existingVoterHistory.isPresent()) {
            throw new ThedalException(ThedalError.DUPLICATE_ENTRY, HttpStatus.CONFLICT, "Voter history with this name already exists.");
        }
        
        // Also check in MongoDB for consistency
        try {
            boolean existsInMongo = voterHistoryMongoRepository.existsByVoterHistoryNameAndAccountIdAndElectionId(voterHistoryName, accountId, electionId);
            if (existsInMongo) {
                log.warn("Duplicate voter history found in MongoDB but not in PostgreSQL: {}", voterHistoryName);
                throw new ThedalException(ThedalError.DUPLICATE_ENTRY, HttpStatus.CONFLICT, "Voter history with this name already exists.");
            }
        } catch (Exception e) {
            log.warn("Failed to check duplicates in MongoDB: {}", e.getMessage());
            // Continue with PostgreSQL check only
        }
    }

    private String uploadVoterHistoryImageToAWS(MultipartFile imageFile) {
        String contentType = imageFile.getContentType();
        if (!(MediaType.IMAGE_JPEG_VALUE.equals(contentType) || MediaType.IMAGE_PNG_VALUE.equals(contentType))) {
            throw new ThedalException(ThedalError.INVALID_IMAGE_FORMAT, HttpStatus.BAD_REQUEST);
        }

        long maxFileSize = 5 * 1024 * 1024; // 5MB
        if (imageFile.getSize() > maxFileSize) {
            throw new ThedalException(ThedalError.INVALID_IMAGE_FORMAT, HttpStatus.BAD_REQUEST);
        }

        String fileExtension = "." + awsFileUpload.getFileExtension(imageFile.getOriginalFilename());
        String fileName = "voter_history_" + System.currentTimeMillis() + "_" + RandomTokenGenerator.generateToken(10) + fileExtension;

        try {
            File tempFile = File.createTempFile("temp", fileExtension);
            try (FileOutputStream fileOutputStream = new FileOutputStream(tempFile)) {
                fileOutputStream.write(imageFile.getBytes());
            }

            String awsUrl = awsFileUpload.uploadToAWS(tempFile, fileName, s3FilesBucket);

            if (!tempFile.delete()) {
                System.err.println("Failed to delete temporary file: " + tempFile.getName());
            }

            return awsUrl;
        } catch (IOException e) {
            throw new ThedalException(ThedalError.UPLOAD_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public List<VoterHistoryEntity> getAllVoterHistories(Long accountId, Long electionId) {
        log.info("Fetching all voter history records for account ID: {} and election ID: {}", accountId, electionId);
    
        List<VoterHistoryEntity> voterHistories = voterHistoryRepository.findByAccountIdAndElectionIdOrderByOrderIndexAsc(accountId, electionId);
    
        return voterHistories;
    }
    
    @Transactional
    public List<Map<String, Object>> getAllVoterHistories1(Long accountId, Long electionId) {
        log.info("Fetching voter histories with voter count for accountId: {}, electionId: {}", accountId, electionId);

        List<Object[]> results = voterHistoryRepository.findVoterHistoriesWithVoterCount(accountId, electionId);
        if (results.isEmpty()) {
            log.warn("No voter histories found for accountId: {}, electionId: {}", accountId, electionId);
            throw new ThedalException(ThedalError.VOTER_HISTORY_NOT_FOUND, HttpStatus.NOT_FOUND);
        }

        List<Map<String, Object>> voterHistoryDetails = results.stream()
                .map(result -> {
                    VoterHistoryEntity voterHistory = (VoterHistoryEntity) result[0];
                    Long voterCount = (Long) result[1];
                    Map<String, Object> voterHistoryData = new HashMap<>();
                    voterHistoryData.put("voterHistoryId", voterHistory.getId());
                    voterHistoryData.put("voterHistoryName", voterHistory.getVoterHistoryName() != null ? voterHistory.getVoterHistoryName() : "");
                    voterHistoryData.put("voterHistoryImage", voterHistory.getVoterHistoryImage() != null ? voterHistory.getVoterHistoryImage() : "");
                    voterHistoryData.put("orderIndex", voterHistory.getOrderIndex() != null ? voterHistory.getOrderIndex() : 0);
                    voterHistoryData.put("voterCount", voterCount);
                    return voterHistoryData;
                })
                .sorted(Comparator.comparingInt(voterHistory -> (Integer) voterHistory.get("orderIndex")))
                .collect(Collectors.toList());

        log.info("Successfully fetched {} voter histories for electionId: {}", voterHistoryDetails.size(), electionId);
        return voterHistoryDetails;
    }
    
//    @Transactional
//public VoterHistoryEntity updateVoterHistory(Long voterHistoryId, Long electionId, Long accountId, VoterHistoryRequest voterHistoryRequest) {
//
//    // Fetch voter history entity by ID, accountId, and electionId
//    VoterHistoryEntity voterHistory = voterHistoryRepository.findByIdAndAccountIdAndElectionId(voterHistoryId, accountId, electionId)
//            .orElseThrow(() -> new ThedalException(ThedalError.VOTER_HISTORY_NOT_FOUND, HttpStatus.NOT_FOUND));
//
//    // Update voter history name if provided
//    if (voterHistoryRequest.getVoterHistoryName() != null && !voterHistoryRequest.getVoterHistoryName().isEmpty()) {
//        // Check for duplicate voter history before updating
//        checkDuplicateVoterHistory(voterHistoryRequest.getVoterHistoryName(), accountId, electionId);
//        voterHistory.setVoterHistoryName(voterHistoryRequest.getVoterHistoryName());
//    }
//    // Integer maxOrderIndex = voterHistoryRepository.findMaxOrderIndexByElectionId(electionId);
//    // int nextOrderIndex = (maxOrderIndex != null) ? maxOrderIndex + 1 : 0;
//    // voterHistory.setOrderIndex(nextOrderIndex);
//    Integer minOrderIndex = voterHistoryRepository.findMinOrderIndexByElectionId(electionId);
//    int newOrderIndex = (minOrderIndex != null) ? minOrderIndex - 1 : 0;
//    voterHistory.setOrderIndex(newOrderIndex);
//
//    // Update voter history image if provided
//    if (voterHistoryRequest.getVoterHistoryImage() != null && !voterHistoryRequest.getVoterHistoryImage().isEmpty()) {
//        String updatedImageUrl = uploadVoterHistoryImageToAWS(voterHistoryRequest.getVoterHistoryImage());
//        voterHistory.setVoterHistoryImage(updatedImageUrl);
//    }
//
//    // Save the updated voter history
//    voterHistory = voterHistoryRepository.save(voterHistory);
//    voterHistoryRepository.flush();
//
//    return voterHistory;
//}
    @Transactional
    public VoterHistoryEntity updateVoterHistory(Long voterHistoryId, Long electionId, Long accountId, VoterHistoryRequest voterHistoryRequest) {
        VoterHistoryEntity voterHistory = voterHistoryRepository.findByIdAndAccountIdAndElectionId(voterHistoryId, accountId, electionId)
                .orElseThrow(() -> new ThedalException(ThedalError.VOTER_HISTORY_NOT_FOUND, HttpStatus.NOT_FOUND));

        if (voterHistoryRequest.getVoterHistoryName() != null && !voterHistoryRequest.getVoterHistoryName().isEmpty()) {
            if (!voterHistoryRequest.getVoterHistoryName().equals(voterHistory.getVoterHistoryName())) {
                checkDuplicateVoterHistory(voterHistoryRequest.getVoterHistoryName(), accountId, electionId);
            }
            voterHistory.setVoterHistoryName(voterHistoryRequest.getVoterHistoryName());
        }

        Integer minOrderIndex = voterHistoryRepository.findMinOrderIndexByElectionId(electionId);
        int newOrderIndex = (minOrderIndex != null) ? minOrderIndex - 1 : 0;
        voterHistory.setOrderIndex(newOrderIndex);

        if (voterHistoryRequest.getVoterHistoryImage() != null && !voterHistoryRequest.getVoterHistoryImage().isEmpty()) {
            String updatedImageUrl = uploadVoterHistoryImageToAWS(voterHistoryRequest.getVoterHistoryImage());
            voterHistory.setVoterHistoryImage(updatedImageUrl);
        }

        try {
            VoterHistoryEntity updated = voterHistoryRepository.save(voterHistory);
            voterHistoryRepository.flush();
            try {
                voterHistoryMongoRepository.save(new VoterHistoryMongo(updated));
                log.info("Updated voter history in MongoDB: id={}", voterHistoryId);
            } catch (Exception mongoEx) {
                log.error("Failed to update voter history in MongoDB: id={}", voterHistoryId, mongoEx);
                throw new RuntimeException("MongoDB update failed, triggering rollback", mongoEx);
            }
            return updated;
        } catch (Exception ex) {
            log.error("Failed to update voter history: id={}", voterHistoryId, ex);
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    
    
//@Transactional
//public void deleteVoterHistories(List<Long> voterHistoryIds, Long accountId, Long electionId) {
//    log.info("Deleting voter history records with IDs: {} for account ID: {} and election ID: {}", voterHistoryIds, accountId, electionId);
//
//    List<VoterHistoryEntity> voterHistories;
//
//    if (voterHistoryIds == null || voterHistoryIds.isEmpty()) {
//        // Fetch all voter history records (even if accountId and electionId are 0)
//        voterHistories = voterHistoryRepository.findByAccountIdAndElectionId(accountId, electionId);
//    } else {
//        // Fetch only the specified voter history records
//        voterHistories = voterHistoryRepository.findByIdInAndAccountIdAndElectionId(voterHistoryIds, accountId, electionId);
//    }
//
//    if (voterHistories.isEmpty()) {
//        throw new ThedalException(ThedalError.VOTER_HISTORY_NOT_FOUND, HttpStatus.NOT_FOUND);
//    }
//
//    
//
//    // Delete voter history records
//    voterHistoryRepository.deleteAll(voterHistories);
//
//    log.info("Successfully deleted voter history records: {}", voterHistoryIds);
//}
    @Transactional
    public void deleteVoterHistories(List<Long> voterHistoryIds, Long accountId, Long electionId) {
        log.info("Deleting voter history records with IDs: {} for account ID: {} and election ID: {}", voterHistoryIds, accountId, electionId);

        List<VoterHistoryEntity> voterHistories;

        if (voterHistoryIds == null || voterHistoryIds.isEmpty()) {
            voterHistories = voterHistoryRepository.findByAccountIdAndElectionId(accountId, electionId);
        } else {
            voterHistories = voterHistoryRepository.findByIdInAndAccountIdAndElectionId(voterHistoryIds, accountId, electionId);
        }

        if (voterHistories.isEmpty()) {
            throw new ThedalException(ThedalError.VOTER_HISTORY_NOT_FOUND, HttpStatus.NOT_FOUND);
        }

        // Check for linked voter histories
        List<VoterHistoryEntity> linkedHistories = (voterHistoryIds == null || voterHistoryIds.isEmpty())
                ? voterHistoryRepository.findLinkedHistories(accountId, electionId)
                : voterHistoryRepository.findLinkedHistoriesByIds(voterHistoryIds, accountId, electionId);

        if (!linkedHistories.isEmpty()) {
            List<String> linkedDetails = linkedHistories.stream()
                    .map((VoterHistoryEntity history) -> {
                        String name = Optional.ofNullable(history.getVoterHistoryName()).orElse("Unnamed");
                        return String.format("[ID: %d, Name: %s]", history.getId(), name);
                    })
                    .collect(Collectors.toList());

            String errorMessage = String.format(
                    "Voter History associated with voters: Cannot delete the following voter histories as they are linked to voters: %s",
                    String.join(", ", linkedDetails));

            log.error(errorMessage);
            throw new ThedalException(ThedalError.VOTER_HISTORY_LINKED_TO_VOTER, HttpStatus.BAD_REQUEST, errorMessage);
        }

        // Proceed with deletion from both databases
        List<Long> idsToDelete = voterHistories.stream().map(VoterHistoryEntity::getId).collect(Collectors.toList());
        try {
            // Delete from PostgreSQL first
            voterHistoryRepository.deleteAll(voterHistories);
            log.info("Deleted voter history records from PostgreSQL: {}", idsToDelete);
            
            // Delete from MongoDB
            try {
                voterHistoryMongoRepository.deleteByIdIn(idsToDelete);
                log.info("Deleted voter history records from MongoDB: {}", idsToDelete);
            } catch (Exception mongoEx) {
                log.error("Failed to delete voter history records from MongoDB: {}", idsToDelete, mongoEx);
                throw new RuntimeException("MongoDB deletion failed, triggering rollback", mongoEx);
            }
        } catch (Exception ex) {
            log.error("Failed to delete voter history records: {}", idsToDelete, ex);
            throw new RuntimeException("Deletion failed, triggering rollback", ex);
        }
    }


    
   
    
    
@Transactional
    public void updateVoterHistoryOrder(List<VoterHistoryReorderRequest> reorderRequests, Long accountId, Long electionId) {
        log.info("Updating voter history order for accountId: {}, electionId: {}, requests: {}", accountId, electionId, reorderRequests.size());
        
        List<VoterHistoryEntity> voterHistories = voterHistoryRepository
                .findByElectionIdAndAccountIdOrderByOrderIndexAsc(electionId, accountId);

        if (voterHistories.isEmpty()) {
            throw new ThedalException(ThedalError.VOTER_HISTORY_NOT_FOUND, HttpStatus.NOT_FOUND);
        }

        // Create a map from voterHistoryId to new order index
        Map<Long, Integer> newOrderMap = reorderRequests.stream()
                .collect(Collectors.toMap(VoterHistoryReorderRequest::getVoterHistoryId, VoterHistoryReorderRequest::getNewOrderIndex));

        // Sort the reorder requests by new order index for a conflict free insertion
        reorderRequests.sort(Comparator.comparingInt(VoterHistoryReorderRequest::getNewOrderIndex));

        // Create a mutable list copy to reassemble ordering
        List<VoterHistoryEntity> remainingHistories = new ArrayList<>(voterHistories);
        // Remove the histories that are being reordered
        remainingHistories.removeIf(history -> newOrderMap.containsKey(history.getId()));

        // A temporary list that will hold the newly ordered voter histories
        List<VoterHistoryEntity> reorderedHistories = new ArrayList<>(remainingHistories);

        // Insert each record from the reorder requests at the specified new index
        for (VoterHistoryReorderRequest request : reorderRequests) {
            VoterHistoryEntity history = voterHistories.stream()
                    .filter(h -> h.getId().equals(request.getVoterHistoryId()))
                    .findFirst()
                    .orElseThrow(() -> new ThedalException(ThedalError.VOTER_HISTORY_NOT_FOUND, HttpStatus.NOT_FOUND));

            // Ensure the new index is within the bounds
            int newIndex = Math.min(request.getNewOrderIndex(), reorderedHistories.size());
            reorderedHistories.add(newIndex, history);
        }

        // Update the orderIndex for all the histories based on their new order
        for (int i = 0; i < reorderedHistories.size(); i++) {
            reorderedHistories.get(i).setOrderIndex(i);
        }

        // Save the reordered voter histories to PostgreSQL
        try {
            List<VoterHistoryEntity> saved = voterHistoryRepository.saveAll(reorderedHistories);
            log.info("Updated voter history order in PostgreSQL for {} records", saved.size());
            
            // Update MongoDB with the new order
            try {
                List<VoterHistoryMongo> mongoHistories = saved.stream()
                        .map(VoterHistoryMongo::new)
                        .collect(Collectors.toList());
                voterHistoryMongoRepository.saveAll(mongoHistories);
                log.info("Updated voter history order in MongoDB for {} records", mongoHistories.size());
            } catch (Exception mongoEx) {
                log.error("Failed to update voter history order in MongoDB for accountId: {}, electionId: {}", accountId, electionId, mongoEx);
                throw new RuntimeException("MongoDB order update failed, triggering rollback", mongoEx);
            }
        } catch (Exception ex) {
            log.error("Failed to update voter history order for accountId: {}, electionId: {}", accountId, electionId, ex);
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


//@Transactional
//public List<Map<String, Object>> getAllVoterHistoriesFromMongo(Long accountId, Long electionId) {
//    log.info("Fetching voter histories from MongoDB for accountId: {}, electionId: {}", accountId, electionId);
//
//    List<VoterHistoryMongo> historiesMongo = voterHistoryMongoRepository.findByAccountIdAndElectionId(accountId, electionId);
//
//    if (historiesMongo.isEmpty()) {
//        log.warn("No voter histories found in MongoDB for accountId: {}, electionId: {}", accountId, electionId);
//        // Fallback to PostgreSQL
//        List<Map<String, Object>> historiesPg = getAllVoterHistories1(accountId, electionId);
//        if (historiesPg.isEmpty()) {
//            throw new ThedalException(ThedalError.VOTER_HISTORY_NOT_FOUND, HttpStatus.NOT_FOUND);
//        }
//        log.debug("Fallback to PostgreSQL: Found {} voter histories", historiesPg.size());
//        return historiesPg;
//    }
//
//    log.debug("Found {} voter histories in MongoDB: {}", historiesMongo.size(), 
//              historiesMongo.stream().map(h -> "id=" + h.getId() + ", name=" + h.getVoterHistoryName()).collect(Collectors.joining("; ")));
//
//    // Fetch voterCount from PostgreSQL
//    List<Long> historyIds = historiesMongo.stream().map(VoterHistoryMongo::getId).collect(Collectors.toList());
//    List<Object[]> results = voterHistoryRepository.findVoterHistoriesWithVoterCount(accountId, electionId);
//    Map<Long, Long> voterCountMap = results.stream()
//            .filter(r -> historyIds.contains(((VoterHistoryEntity) r[0]).getId()))
//            .collect(Collectors.toMap(r -> ((VoterHistoryEntity) r[0]).getId(), r -> (Long) r[1], (a, b) -> a));
//
//    List<Map<String, Object>> voterHistoryDetails = historiesMongo.stream()
//            .map(history -> {
//                Map<String, Object> historyData = new HashMap<>();
//                historyData.put("voterHistoryId", history.getId());
//                historyData.put("voterHistoryName", history.getVoterHistoryName() != null ? history.getVoterHistoryName() : "");
//                historyData.put("voterHistoryImage", history.getVoterHistoryImage() != null ? history.getVoterHistoryImage() : "");
//                historyData.put("orderIndex", history.getOrderIndex() != null ? history.getOrderIndex() : 0);
//                historyData.put("voterCount", voterCountMap.getOrDefault(history.getId(), 0L));
//                historyData.put("electionId", history.getElectionId());
//                log.debug("Mapped voter history id={} with voterCount={}", history.getId(), voterCountMap.getOrDefault(history.getId(), 0L));
//                return historyData;
//            })
//            .sorted(Comparator.comparingInt(history -> (Integer) history.get("orderIndex")))
//            .collect(Collectors.toList());
//
//    log.info("Successfully fetched {} voter histories from MongoDB for electionId: {}", voterHistoryDetails.size(), electionId);
//    ThedalResponse<List<Map<String, Object>>> response = new ThedalResponse<>(ThedalSuccess.VOTER_HISTORIES_FETCHED, voterHistoryDetails);
//    response.setStatus("success");
//    return voterHistoryDetails;
//}

@Transactional
public List<Map<String, Object>> getAllVoterHistoriesFromMongo(Long accountId, Long electionId) {
    log.info("Fetching voter histories from PostgreSQL for accountId: {}, electionId: {}", accountId, electionId);

    List<VoterHistoryEntity> histories = voterHistoryRepository.findByAccountIdAndElectionId(accountId, electionId);

    if (histories.isEmpty()) {
        log.warn("No voter histories found in PostgreSQL for accountId: {}, electionId: {}", accountId, electionId);
        throw new ThedalException(ThedalError.VOTER_HISTORY_NOT_FOUND, HttpStatus.NOT_FOUND);
    }

    List<Map<String, Object>> voterHistoryDetails = histories.stream()
            .map(history -> {
                Map<String, Object> map = new HashMap<>();
                map.put("voterHistoryId", history.getId());
                map.put("voterHistoryName", Optional.ofNullable(history.getVoterHistoryName()).orElse(""));
                map.put("voterHistoryImage", Optional.ofNullable(history.getVoterHistoryImage()).orElse(""));
                map.put("orderIndex", Optional.ofNullable(history.getOrderIndex()).orElse(0));
                map.put("electionId", history.getElectionId());
                return map;
            })
            .sorted(Comparator.comparingInt(m -> (Integer) m.get("orderIndex")))
            .collect(Collectors.toList());

    log.info("Successfully fetched {} voter histories from PostgreSQL for electionId: {}", voterHistoryDetails.size(), electionId);
    return voterHistoryDetails;
}



}