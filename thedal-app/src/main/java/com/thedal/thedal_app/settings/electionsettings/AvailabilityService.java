package com.thedal.thedal_app.settings.electionsettings;



import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
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
import org.springframework.web.multipart.MultipartFile;

import com.thedal.thedal_app.awsfilestore.AwsFileUpload;
import com.thedal.thedal_app.election.ElectionEntity;
import com.thedal.thedal_app.election.ElectionRepository;
import com.thedal.thedal_app.general.RequestDetailsService;
import com.thedal.thedal_app.response.ThedalResponse;
import com.thedal.thedal_app.response.ThedalSuccess;
import com.thedal.thedal_app.settings.electionsettings.dto.AvailabilityDTO;
import com.thedal.thedal_app.settings.electionsettings.dto.AvailabilityReorderRequest;
import com.thedal.thedal_app.settings.electionsettings.dto.AvailabilityReponseDTO;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;
import com.thedal.thedal_app.util.RandomTokenGenerator;
import com.thedal.thedal_app.voter.VoterRepo;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AvailabilityService {

     @Autowired
    private RequestDetailsService requestDetails;   
    @Autowired
    private AvailabilityRepository availabilityRepository;
    @Autowired 
    private VoterRepo voterRepo;
    @Autowired
    private ElectionRepository electionRepository;
    @Autowired
    private AvailabilityMongoRepository availabilityMongoRepository;

    @Autowired
    private AwsFileUpload awsFileUpload;
    @Value("${aws.s3.banner.bucket}")
	private String s3bucket;

    private void validateElectionOwnership(Long electionId, Long accountId) {
        Optional<ElectionEntity> electionOpt = electionRepository.findByIdAndAccountId(electionId, accountId);
        if (!electionOpt.isPresent()) {
            log.error("Election ID {} does not belong to Account ID {}", electionId, accountId);
            throw new ThedalException(ThedalError.INVALID_ELECTION, HttpStatus.FORBIDDEN);
        }
    }

    @Transactional
    public ThedalResponse<AvailabilityReponseDTO> createAvailability(AvailabilityDTO availabilityDTO, Long electionId, MultipartFile file) {
        // Get the current account ID
        Long accountId = requestDetails.getCurrentAccountId();
        
        // Validate account ID
        if (accountId == null) {
            log.error("Account ID not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }
        
        validateElectionOwnership(electionId, accountId);
        
        try {
            // Check for duplicates in PostgreSQL
            if (availabilityRepository.existsByCategoryNameAndElectionId(availabilityDTO.getCategoryName(), electionId)) {
                log.error("Availability with category name '{}' already exists.", availabilityDTO.getCategoryName());
                throw new ThedalException(
                    ThedalError.AVAILABILITY_CATEGORY_ALREADY_EXIST, 
                    HttpStatus.CONFLICT, 
                    "Availability with category name '" + availabilityDTO.getCategoryName() + "' already exists."
                );
            }
            if (availabilityRepository.existsByDescriptionAndElectionId(availabilityDTO.getDescription(), electionId)) {
                log.error("Availability with description '{}' already exists.", availabilityDTO.getDescription());
                throw new ThedalException(
                    ThedalError.AVAILABILITY_DESCRIPTION_ALREADY_EXIST,
                    HttpStatus.CONFLICT,
                    "Availability with description '" + availabilityDTO.getDescription() + "' already exists."
                );
            }
            
            // Check for duplicates in MongoDB
            if (availabilityMongoRepository.existsByCategoryNameAndElectionId(availabilityDTO.getCategoryName(), electionId)) {
                log.error("Availability with category name '{}' already exists in MongoDB.", availabilityDTO.getCategoryName());
                throw new ThedalException(
                    ThedalError.AVAILABILITY_CATEGORY_ALREADY_EXIST,
                    HttpStatus.CONFLICT,
                    "Availability with category name '" + availabilityDTO.getCategoryName() + "' already exists in MongoDB."
                );
            }
            if (availabilityMongoRepository.existsByDescriptionAndElectionId(availabilityDTO.getDescription(), electionId)) {
                log.error("Availability with description '{}' already exists in MongoDB.", availabilityDTO.getDescription());
                throw new ThedalException(
                    ThedalError.AVAILABILITY_DESCRIPTION_ALREADY_EXIST,
                    HttpStatus.CONFLICT,
                    "Availability with description '" + availabilityDTO.getDescription() + "' already exists in MongoDB."
                );
            }

            // Update existing availabilities' order index
            List<Availability> existingAvailabilities = availabilityRepository.findByElectionIdAndAccountIdOrderByOrderIndexAsc(electionId, accountId);
            for (Availability a : existingAvailabilities) {
                a.setOrderIndex(a.getOrderIndex() + 1);
            }
            availabilityRepository.saveAll(existingAvailabilities);

            // Handle file upload
            String uploadUrl = null;
            if (file != null && !file.isEmpty()) { 
                try {
                    uploadUrl = uploadAvaiablityImageToAWS(file);
                } catch (Exception e) {
                    log.error("Error uploading image: ", e);
                    throw new ThedalException(ThedalError.IMAGE_UPLOAD_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }
            
            // Create a new Availability entity
            Availability availability = new Availability();
            availability.setCategoryName(availabilityDTO.getCategoryName()); 
            availability.setDescription(availabilityDTO.getDescription());
            availability.setAccountId(accountId);
            availability.setElectionId(electionId);
            availability.setAvailabilityImage(uploadUrl);
            availability.setOrderIndex(0);
            
            log.info("Creating new availability: category={}, description={}, electionId={}", 
                     availabilityDTO.getCategoryName(), availabilityDTO.getDescription(), electionId);
            
            // Dual write implementation with error handling
            Availability savedAvailability = availabilityRepository.save(availability);
            
            if (savedAvailability.getId() == null) {
                log.error("Failed to save availability to PostgreSQL. Entity: {}", availability);
                throw new ThedalException(ThedalError.AVAILABILITY_NOT_FOUND, HttpStatus.INTERNAL_SERVER_ERROR);
            }
            
            // Save to MongoDB with error handling and rollback
            try {
                AvailabilityMongo availabilityMongo = new AvailabilityMongo(savedAvailability);
                availabilityMongoRepository.save(availabilityMongo);
                log.info("Successfully created availability in MongoDB: id={}, category={}, description={}", 
                         availabilityMongo.getId(), availabilityMongo.getCategoryName(), availabilityMongo.getDescription());
            } catch (Exception mongoEx) {
                log.error("Failed to create availability in MongoDB: category={}, description={}. Error: {}", 
                          savedAvailability.getCategoryName(), savedAvailability.getDescription(), mongoEx.getMessage(), mongoEx);
                
                // Rollback PostgreSQL by throwing exception
                throw new RuntimeException("MongoDB save failed for availability: " + savedAvailability.getCategoryName() + 
                                          ". Rolling back PostgreSQL transaction.", mongoEx);
            }
            
            log.info("Availability created successfully in both PostgreSQL and MongoDB: id={}, category={}, description={}", 
                     savedAvailability.getId(), savedAvailability.getCategoryName(), savedAvailability.getDescription());
            
            // Create the response DTO
            AvailabilityReponseDTO availabilityDTOResponse = new AvailabilityReponseDTO(
                savedAvailability.getId(),
                savedAvailability.getCategoryName(),
                savedAvailability.getDescription(),
                savedAvailability.getAvailabilityImage(),
                savedAvailability.getOrderIndex() 
            );
            
            return new ThedalResponse<>(ThedalSuccess.AVAILABILITY_CREATED, availabilityDTOResponse);
            
        } catch (Exception ex) {
            log.error("Failed to create availability: category={}, description={}, electionId={}", 
                      availabilityDTO.getCategoryName(), availabilityDTO.getDescription(), electionId, ex);
            
            if (ex instanceof ThedalException) {
                throw ex;
            } else {
                throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
    }
    

    private String uploadAvaiablityImageToAWS(MultipartFile imageFile) {
        String contentType = imageFile.getContentType();
        if (!(MediaType.IMAGE_JPEG_VALUE.equals(contentType) ||
                MediaType.IMAGE_PNG_VALUE.equals(contentType))) {
            throw new ThedalException(ThedalError.INVALID_IMAGE_FORMAT, HttpStatus.BAD_REQUEST);
        }

        long maxFileSize = 5 * 1024 * 1024;  //less than 500KB
        if (imageFile.getSize() > maxFileSize) {
            throw new ThedalException(ThedalError.INVALID_IMAGE_FORMAT, HttpStatus.BAD_REQUEST);
        }

        // try {
        //     BufferedImage image = ImageIO.read(imageFile.getInputStream());
        //     if (image == null || image.getWidth() != 512 || image.getHeight() != 512) {
        //         throw new ThedalException(ThedalError.INVALID_IMAGE_DIMENSIONS, HttpStatus.BAD_REQUEST);
        //     }
        // } catch (IOException e) {
        //     throw new ThedalException(ThedalError.UPLOAD_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
        // }

        // Generate a unique file name
        String fileExtension = "." + awsFileUpload.getFileExtension(imageFile.getOriginalFilename());
        String fileName = "availabilty_" + System.currentTimeMillis() + "_" + RandomTokenGenerator.generateToken(10) + fileExtension;

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

//    public List<AvailabilityResponse> getAvailability(Long accountId, Long electionId) {
//    	
//    	//List<Availability> availabilities = availabilityRepository.findByAccountIdAndElectionId(accountId, electionId);
//    	List<Availability> availabilities = availabilityRepository.findByAccountIdAndElectionIdOrderByOrderIndexAsc(accountId, electionId);
//    	
//        if (availabilities.isEmpty()) {
//            throw new ThedalException(ThedalError.AVAILABILITY_NOT_FOUND, HttpStatus.NOT_FOUND);
//        }
//    //     availabilities.sort(Comparator
//    //     .comparing(Availability::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
//    //     .thenComparing(Availability::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
//    // );
//
//        return availabilities.stream()
//                .map(availability -> new AvailabilityResponse(
//                		availability.getId(),
//                        availability.getCategoryName(), 
//                		availability.getDescription(),
//                		availability.getAvailabilityImage(),
//                		availability.getOrderIndex()))
//                .collect(Collectors.toList());
//               
//    }

    @Transactional
    public ThedalResponse<List<Map<String, Object>>> getAvailability(Long accountId, Long electionId) {
        log.info("Fetching availabilities with voter count for accountId: {}, electionId: {}", accountId, electionId);

        validateElectionOwnership(electionId, accountId);
        
        List<Object[]> results = availabilityRepository.findAvailabilitiesWithVoterCount(accountId, electionId);
        if (results.isEmpty()) {
            log.warn("No availabilities found for accountId: {}, electionId: {}", accountId, electionId);
            throw new ThedalException(ThedalError.AVAILABILITY_NOT_FOUND, HttpStatus.NOT_FOUND);
        }

        List<Map<String, Object>> availabilityDetails = results.stream()
                .map(result -> {
                    Availability availability = (Availability) result[0];
                    Long voterCount = (Long) result[1];
                    Map<String, Object> availabilityData = new HashMap<>();
                    availabilityData.put("id", availability.getId());
                    availabilityData.put("categoryName", availability.getCategoryName() != null ? availability.getCategoryName() : "");
//                    availabilityData.put("availabilityName", availability.getAvailabilityName() != null ? availability.getAvailabilityName() : "");
                    availabilityData.put("description", availability.getDescription() != null ? availability.getDescription() : "");
                    availabilityData.put("availabilityImage", availability.getAvailabilityImage() != null ? availability.getAvailabilityImage() : "");
                    availabilityData.put("orderIndex", availability.getOrderIndex() != null ? availability.getOrderIndex() : 0);
                    availabilityData.put("voterCount", voterCount);
                    return availabilityData;
                })
                .sorted(Comparator.comparingInt(availability -> (Integer) availability.get("orderIndex")))
                .collect(Collectors.toList());

        log.info("Successfully fetched {} availabilities for electionId: {}", availabilityDetails.size(), electionId);
        return new ThedalResponse<>(ThedalSuccess.AVAILABILITY_FOUND, availabilityDetails);
    }


    @Transactional
    public AvailabilityReponseDTO updateAvailability(Long accountId, Long electionId, Long availabilityId, AvailabilityDTO availabilityDTO, MultipartFile file) {
        validateElectionOwnership(electionId, accountId);
        
        try {
            Availability availability = availabilityRepository.findByAccountIdAndElectionIdAndId(accountId, electionId, availabilityId)
                    .orElseThrow(() -> new ThedalException(ThedalError.AVAILABILITY_NOT_FOUND, HttpStatus.NOT_FOUND));
            
            // Check if category name already exists (excluding current availability)
            boolean categoryExists = availabilityRepository.existsByCategoryNameAndElectionId(availabilityDTO.getCategoryName(), electionId);
            if (categoryExists && !availability.getCategoryName().equals(availabilityDTO.getCategoryName())) {
                log.error("Availability with category name '{}' already exists for election '{}'.", 
                          availabilityDTO.getCategoryName(), electionId);
                throw new ThedalException(
                    ThedalError.AVAILABILITY_CATEGORY_ALREADY_EXIST,
                    HttpStatus.CONFLICT,
                    "Availability with category name '" + availabilityDTO.getCategoryName() + "' already exists for election " + electionId + "."
                );
            }
            
            // Check if description already exists (excluding current availability)
            boolean descriptionExists = availabilityRepository.existsByDescriptionAndElectionId(availabilityDTO.getDescription(), electionId);
            if (descriptionExists && !availability.getDescription().equals(availabilityDTO.getDescription())) {
                log.error("Availability with description '{}' already exists for election '{}'.", 
                          availabilityDTO.getDescription(), electionId);
                throw new ThedalException(
                    ThedalError.AVAILABILITY_DESCRIPTION_ALREADY_EXIST,
                    HttpStatus.CONFLICT,
                    "Availability with description '" + availabilityDTO.getDescription() + "' already exists for election " + electionId + "."
                );
            }
            
            // Check for duplicates in MongoDB
            if (availabilityMongoRepository.existsByCategoryNameAndElectionId(availabilityDTO.getCategoryName(), electionId) &&
                !availability.getCategoryName().equals(availabilityDTO.getCategoryName())) {
                log.error("Availability with category name '{}' already exists in MongoDB for election '{}'.", 
                          availabilityDTO.getCategoryName(), electionId);
                throw new ThedalException(
                    ThedalError.AVAILABILITY_CATEGORY_ALREADY_EXIST,
                    HttpStatus.CONFLICT,
                    "Availability with category name '" + availabilityDTO.getCategoryName() + "' already exists in MongoDB."
                );
            }
            if (availabilityMongoRepository.existsByDescriptionAndElectionId(availabilityDTO.getDescription(), electionId) &&
                !availability.getDescription().equals(availabilityDTO.getDescription())) {
                log.error("Availability with description '{}' already exists in MongoDB for election '{}'.", 
                          availabilityDTO.getDescription(), electionId);
                throw new ThedalException(
                    ThedalError.AVAILABILITY_DESCRIPTION_ALREADY_EXIST,
                    HttpStatus.CONFLICT,
                    "Availability with description '" + availabilityDTO.getDescription() + "' already exists in MongoDB."
                );
            }
            
            // Update availability fields
            availability.setCategoryName(availabilityDTO.getCategoryName()); 
            availability.setDescription(availabilityDTO.getDescription());
            
            // Handle file upload if provided
            if (file != null && !file.isEmpty()) {
                try {
                    log.info("Uploading file: {}", file.getOriginalFilename());
                    String uploadUrl = uploadAvaiablityImageToAWS(file);
                    log.info("File uploaded successfully, URL: {}", uploadUrl);
                    availability.setAvailabilityImage(uploadUrl);
                } catch (Exception e) {
                    log.error("Error uploading image: ", e);
                    throw new ThedalException(ThedalError.IMAGE_UPLOAD_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }
            
            // Update order index logic - move updated availability to top
            List<Availability> allAvailabilities = availabilityRepository.findByElectionIdAndAccountIdOrderByOrderIndexAsc(electionId, accountId);
            allAvailabilities.removeIf(a -> a.getId().equals(availabilityId)); 
            for (Availability a : allAvailabilities) {
                a.setOrderIndex(a.getOrderIndex() + 1); 
            }
            availabilityRepository.saveAll(allAvailabilities); 
            availability.setOrderIndex(0);
            
            log.info("Updating availability: id={}, category={}, description={}", 
                     availabilityId, availabilityDTO.getCategoryName(), availabilityDTO.getDescription());
            
            // Dual write implementation with error handling
            Availability savedAvailability = availabilityRepository.save(availability);
            
            try {
                // Update MongoDB entity
                AvailabilityMongo availabilityMongo = availabilityMongoRepository.findById(availabilityId)
                        .orElse(new AvailabilityMongo(savedAvailability));
                availabilityMongo.setCategoryName(availabilityDTO.getCategoryName());
                availabilityMongo.setDescription(availabilityDTO.getDescription());
                availabilityMongo.setAvailabilityImage(savedAvailability.getAvailabilityImage());
                availabilityMongo.setOrderIndex(savedAvailability.getOrderIndex());
                availabilityMongo.setUpdatedAt(LocalDateTime.now());
                availabilityMongoRepository.save(availabilityMongo);
                
                log.info("Successfully updated availability in MongoDB: id={}, category={}, description={}", 
                         availabilityId, availabilityDTO.getCategoryName(), availabilityDTO.getDescription());
            } catch (Exception mongoEx) {
                log.error("Failed to update availability in MongoDB: id={}, category={}, description={}. Error: {}", 
                          availabilityId, availabilityDTO.getCategoryName(), availabilityDTO.getDescription(), mongoEx.getMessage(), mongoEx);
                
                // Rollback PostgreSQL by throwing exception
                throw new RuntimeException("MongoDB update failed for availability: " + availabilityDTO.getCategoryName() + 
                                          ". Rolling back PostgreSQL transaction.", mongoEx);
            }
            
            log.info("Availability updated successfully in both PostgreSQL and MongoDB: id={}, category={}, description={}", 
                     availabilityId, availabilityDTO.getCategoryName(), availabilityDTO.getDescription());
            
            return new AvailabilityReponseDTO(
                savedAvailability.getId(),
                savedAvailability.getCategoryName(), 
                savedAvailability.getDescription(),
                savedAvailability.getAvailabilityImage(),
                savedAvailability.getOrderIndex()
            );
            
        } catch (Exception ex) {
            log.error("Failed to update availability: id={}, category={}, description={}", 
                      availabilityId, availabilityDTO.getCategoryName(), availabilityDTO.getDescription(), ex);
            
            if (ex instanceof ThedalException) {
                throw ex;
            } else {
                throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
    }
    
//    @Transactional
//    public void deleteAvailability(Long accountId, Long electionId, List<Long> availabilityIds) {
//    if (availabilityIds == null || availabilityIds.isEmpty()) {
//        // Delete all availabilities for the given account and election
//        int deletedCount = availabilityRepository.deleteByAccountIdAndElectionId(accountId, electionId);
//        if (deletedCount == 0) {
//            log.warn("No availabilities found to delete for accountId: {}, electionId: {}", accountId, electionId);
//            throw new ThedalException(ThedalError.AVAILABILITY_NOT_FOUND, HttpStatus.NOT_FOUND);
//        }
//        log.info("Deleted all availabilities for accountId: {}, electionId: {}", accountId, electionId);
//    } else {
//        // Delete specific availabilities by ID
//        List<Availability> availabilities = availabilityRepository.findByIdInAndAccountIdAndElectionId(availabilityIds, accountId, electionId);
//        
//        if (availabilities.isEmpty()) {
//            throw new ThedalException(ThedalError.AVAILABILITY_NOT_FOUND, HttpStatus.NOT_FOUND);
//        }
//
//        // Check if any availability is linked to a voter
//        for (Availability availability : availabilities) {
//            boolean isLinked = voterRepo.existsByAvailabilityAndElectionId(availability.getDescription(), electionId);
//            
//            if (isLinked) {
//                log.error("Cannot delete availability '{}': It is linked to a voter.", availability.getDescription());
//                throw new ThedalException(ThedalError.AVAILABILITY_LINKED_TO_VOTER, HttpStatus.BAD_REQUEST);
//            }
//        }
//
//        // Delete all valid availabilities
//        availabilityRepository.deleteAll(availabilities);
//        log.info("Deleted specific availabilities: {}", availabilityIds);
//    }
//}
//    @Transactional
//    public void deleteAvailability(Long accountId, Long electionId, List<Long> availabilityIds) {
//        
//    	validateElectionOwnership(electionId, accountId);
//    	if (availabilityIds == null || availabilityIds.isEmpty()) {
//            log.info("Deleting all availabilities for accountId: {}, electionId: {}", accountId, electionId);
//            int deletedCount = availabilityRepository.deleteByAccountIdAndElectionId(accountId, electionId);
//            if (deletedCount == 0) {
//            	throw new ThedalException(ThedalError.AVAILABILITY_NOT_FOUND, HttpStatus.NOT_FOUND);
//            }
//            try {
//                availabilityMongoRepository.deleteByAccountIdAndElectionId(accountId, electionId);
//                log.info("Deleted all availabilities from MongoDB for accountId: {}, electionId: {}", accountId, electionId);
//            } catch (Exception mongoEx) {
//                log.error("Failed to delete availabilities from MongoDB for accountId: {}, electionId: {}", accountId, electionId, mongoEx);
//                throw new RuntimeException("MongoDB deletion failed, triggering rollback", mongoEx);
//            }
//            
//            return;
//        }
//
//        List<Availability> availabilities = availabilityRepository.findByIdInAndAccountIdAndElectionId(availabilityIds, accountId, electionId);
//        if (availabilities.isEmpty()) {
//        	throw new ThedalException(ThedalError.AVAILABILITY_NOT_FOUND, HttpStatus.NOT_FOUND);
//        }
//
//        for (Availability availability : availabilities) {
//            boolean isLinked = voterRepo.existsByAvailability1IdAndElectionId(availability.getId(), electionId);
//            if (isLinked) {
//                log.error("Cannot delete availability '{}': Linked to voters.", availability.getDescription());
//                throw new ThedalException(ThedalError.AVAILABILITY_LINKED_TO_VOTER, HttpStatus.BAD_REQUEST);
//            }
//        }
//
//        availabilityRepository.deleteAll(availabilities);
//        try {
//            availabilityMongoRepository.deleteByIdIn(availabilityIds);
//            log.info("Deleted availabilities from MongoDB: {}", availabilityIds);
//        } catch (Exception mongoEx) {
//            log.error("Failed to delete availabilities from MongoDB: {}", availabilityIds, mongoEx);
//            throw new RuntimeException("MongoDB deletion failed, triggering rollback", mongoEx);
//        }
//        log.info("Deleted availabilities: {}", availabilityIds);
//    }
    @Transactional
    public void deleteAvailability(Long accountId, Long electionId, List<Long> availabilityIds) {
        validateElectionOwnership(electionId, accountId);

        try {
            if (availabilityIds == null || availabilityIds.isEmpty()) {
                log.info("Deleting all availabilities for accountId: {}, electionId: {}", accountId, electionId);
                
                // Check if any voters are linked to *any* availabilities for this election
                boolean hasLinkedVoters = voterRepo.existsByElectionIdAndAvailability1IsNotNull(electionId);
                if (hasLinkedVoters) {
                    List<Availability> linkedAvailabilities = availabilityRepository.findByAccountIdAndElectionId(accountId, electionId);
                    List<String> linkedDetails = linkedAvailabilities.stream()
                        .filter(avail -> voterRepo.existsByAvailability1IdAndElectionId(avail.getId(), electionId))
                        .map(avail -> String.format("ID: %d, Description: %s", avail.getId(), avail.getDescription()))
                        .collect(Collectors.toList());
                    if (!linkedDetails.isEmpty()) {
                        String errorMessage = String.format("Cannot delete availabilities as they are associated with voters: %s", 
                            String.join("; ", linkedDetails));
                        log.error(errorMessage);
                        throw new ThedalException(ThedalError.AVAILABILITY_LINKED_TO_VOTER, HttpStatus.BAD_REQUEST, errorMessage);
                    }
                }
                
                // Dual write deletion with error handling
                int deletedCount = availabilityRepository.deleteByAccountIdAndElectionId(accountId, electionId);
                if (deletedCount == 0) {
                    throw new ThedalException(ThedalError.AVAILABILITY_NOT_FOUND, HttpStatus.NOT_FOUND);
                }
                
                try {
                    long mongoDeletedCount = availabilityMongoRepository.deleteByAccountIdAndElectionId(accountId, electionId);
                    log.info("Deleted {} availabilities from PostgreSQL and {} from MongoDB for accountId: {}, electionId: {}", 
                             deletedCount, mongoDeletedCount, accountId, electionId);
                } catch (Exception mongoEx) {
                    log.error("Failed to delete availabilities from MongoDB for accountId: {}, electionId: {}", 
                              accountId, electionId, mongoEx);
                    throw new RuntimeException("MongoDB deletion failed, triggering rollback", mongoEx);
                }
                
                log.info("Successfully deleted all availabilities for accountId: {}, electionId: {}", accountId, electionId);
                return;
            }

            // Delete specific availabilities
            List<Availability> availabilities = availabilityRepository.findByIdInAndAccountIdAndElectionId(availabilityIds, accountId, electionId);
            if (availabilities.isEmpty()) {
                log.error("No availabilities found for deletion with IDs: {}, accountId: {}, electionId: {}", 
                          availabilityIds, accountId, electionId);
                throw new ThedalException(ThedalError.AVAILABILITY_NOT_FOUND, HttpStatus.NOT_FOUND);
            }

            // Check for linked voters
            List<String> linkedAvailabilityDetails = new ArrayList<>();
            for (Availability availability : availabilities) {
                boolean isLinked = voterRepo.existsByAvailability1IdAndElectionId(availability.getId(), electionId);
                log.info("Checking Availability ID: {}, Election ID: {}, Is Linked: {}", 
                    availability.getId(), electionId, isLinked);
                if (isLinked) {
                    linkedAvailabilityDetails.add(String.format("ID: %d, Description: %s", 
                        availability.getId(), availability.getDescription()));
                }
            }

            // Throw exception if any availabilities are linked
            if (!linkedAvailabilityDetails.isEmpty()) {
                String errorMessage = String.format("Cannot delete the following availabilities as they are associated with voters: %s",
                    String.join("; ", linkedAvailabilityDetails));
                log.error(errorMessage);
                throw new ThedalException(ThedalError.AVAILABILITY_LINKED_TO_VOTER, HttpStatus.BAD_REQUEST, errorMessage);
            }

            // Dual write deletion with error handling
            log.info("Deleting {} availabilities for accountId: {}, electionId: {}", 
                     availabilities.size(), accountId, electionId);
            
            availabilityRepository.deleteAll(availabilities);
            
            try {
                availabilityMongoRepository.deleteByIdIn(availabilityIds);
                log.info("Successfully deleted availabilities from both PostgreSQL and MongoDB: {}", availabilityIds);
            } catch (Exception mongoEx) {
                log.error("Failed to delete availabilities from MongoDB: {}", availabilityIds, mongoEx);
                throw new RuntimeException("MongoDB deletion failed, triggering rollback", mongoEx);
            }
            
            log.info("Successfully deleted availabilities: {}", availabilityIds);
            
        } catch (Exception ex) {
            log.error("Failed to delete availabilities: accountId={}, electionId={}, availabilityIds={}", 
                      accountId, electionId, availabilityIds, ex);
            
            if (ex instanceof ThedalException) {
                throw ex;
            } else {
                throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, 
                    "Failed to delete availabilities due to an unexpected error.");
            }
        }
    }
    

   
    
//    @Transactional
//    public void updateAvailabilityOrder(List<AvailabilityReorderRequest> reorderRequests, Long accountId, Long electionId) {
//        
//    	validateElectionOwnership(electionId, accountId);
//    	List<Availability> availabilities = availabilityRepository.findByElectionIdAndAccountIdOrderByOrderIndexAsc(electionId, accountId);
//
//        if (availabilities.isEmpty()) {
//            log.error("No availabilities found for election ID {} and account ID {}", electionId, accountId);
//            throw new ThedalException(ThedalError.AVAILABILITY_NOT_FOUND, HttpStatus.NOT_FOUND);
//        }
//
//        // Create a map of availabilityId -> newOrderIndex
//        Map<Long, Integer> newOrderMap = reorderRequests.stream()
//                .collect(Collectors.toMap(AvailabilityReorderRequest::getAvailabilityId, AvailabilityReorderRequest::getNewOrderIndex));
//
//        // Sort the existing availabilities list before modifying
//        availabilities.sort(Comparator.comparing(Availability::getOrderIndex));
//
//        // Remove availabilities that are being reordered
//        List<Availability> reorderedAvailabilities = new ArrayList<>(availabilities);
//        reorderedAvailabilities.removeIf(a -> newOrderMap.containsKey(a.getId()));
//
//        // Insert availabilities at their new positions
//        for (AvailabilityReorderRequest request : reorderRequests) {
//            Availability availability = availabilities.stream()
//                    .filter(a -> a.getId().equals(request.getAvailabilityId()))
//                    .findFirst()
//                    .orElseThrow(() -> new ThedalException(ThedalError.AVAILABILITY_NOT_FOUND, HttpStatus.NOT_FOUND));
//
//            reorderedAvailabilities.add(request.getNewOrderIndex(), availability);
//        }
//
//        // Update `orderIndex` for all availabilities
//        for (int i = 0; i < reorderedAvailabilities.size(); i++) {
//            reorderedAvailabilities.get(i).setOrderIndex(i);
//            log.info("Updated availability order: {} -> {}", reorderedAvailabilities.get(i).getDescription(), i);
//        }
//
//        // Save updated order to DB
//        availabilityRepository.saveAll(reorderedAvailabilities);
//        log.info("Availability order updated successfully for electionId: {}", electionId);
//    }
    @Transactional
    public void updateAvailabilityOrder(List<AvailabilityReorderRequest> reorderRequests, Long accountId, Long electionId) {
        validateElectionOwnership(electionId, accountId);
        
        try {
            // Validate reorder requests
            if (reorderRequests == null || reorderRequests.isEmpty()) {
                log.error("Reorder requests cannot be null or empty");
                throw new ThedalException(ThedalError.AVAILABILITY_ORDER_UPDATE_FAILED, HttpStatus.BAD_REQUEST);
            }
            
            // Check for null or invalid IDs in requests
            for (AvailabilityReorderRequest request : reorderRequests) {
                if (request.getAvailabilityId() == null) {
                    log.error("AvailabilityId cannot be null in reorder request");
                    throw new ThedalException(ThedalError.AVAILABILITY_ORDER_UPDATE_FAILED, HttpStatus.BAD_REQUEST);
                }
                if (request.getNewOrderIndex() == null || request.getNewOrderIndex() < 0) {
                    log.error("NewOrderIndex cannot be null or negative in reorder request");
                    throw new ThedalException(ThedalError.AVAILABILITY_ORDER_UPDATE_FAILED, HttpStatus.BAD_REQUEST);
                }
            }
            
            List<Availability> availabilities = availabilityRepository.findByElectionIdAndAccountIdOrderByOrderIndexAsc(electionId, accountId);
            
            if (availabilities.isEmpty()) {
                log.error("No availabilities found for election ID {} and account ID {}", electionId, accountId);
                throw new ThedalException(ThedalError.AVAILABILITY_NOT_FOUND, HttpStatus.NOT_FOUND);
            }
            
            // Create a map of availabilityId -> newOrderIndex
            Map<Long, Integer> newOrderMap = reorderRequests.stream()
                    .collect(Collectors.toMap(AvailabilityReorderRequest::getAvailabilityId, AvailabilityReorderRequest::getNewOrderIndex));
            
            // Sort the reorderRequests by newOrderIndex to avoid conflicts
            reorderRequests.sort(Comparator.comparingInt(AvailabilityReorderRequest::getNewOrderIndex));
            
            // Collect availabilities that are being reordered
            List<Availability> reorderedAvailabilities = new ArrayList<>();
            List<Availability> remainingAvailabilities = new ArrayList<>(availabilities);
            
            for (AvailabilityReorderRequest request : reorderRequests) {
                Availability availability = availabilities.stream()
                        .filter(a -> a.getId().equals(request.getAvailabilityId()))
                        .findFirst()
                        .orElseThrow(() -> new ThedalException(ThedalError.AVAILABILITY_NOT_FOUND, HttpStatus.NOT_FOUND));
                
                reorderedAvailabilities.add(availability);
                remainingAvailabilities.remove(availability);
            }
            
            // Insert reordered availabilities at their new positions
            for (AvailabilityReorderRequest request : reorderRequests) {
                Availability availability = reorderedAvailabilities.stream()
                        .filter(a -> a.getId().equals(request.getAvailabilityId()))
                        .findFirst()
                        .orElseThrow(() -> new ThedalException(ThedalError.AVAILABILITY_NOT_FOUND, HttpStatus.NOT_FOUND));
                
                // Ensure the new index is within bounds
                int newIndex = Math.min(request.getNewOrderIndex(), remainingAvailabilities.size());
                remainingAvailabilities.add(newIndex, availability);
            }
            
            // Update `orderIndex` for all availabilities
            for (int i = 0; i < remainingAvailabilities.size(); i++) {
                remainingAvailabilities.get(i).setOrderIndex(i);
                log.info("Updated availability order: {} -> {}", remainingAvailabilities.get(i).getDescription(), i);
            }
            
            log.info("Reordering availabilities for electionId: {}, count: {}", electionId, remainingAvailabilities.size());
            
            // Dual write implementation with error handling
            List<Availability> savedAvailabilities = availabilityRepository.saveAll(remainingAvailabilities);
            
            try {
                // Update MongoDB entities
                List<AvailabilityMongo> mongoAvailabilities = savedAvailabilities.stream()
                        .map(availability -> {
                            AvailabilityMongo mongoAvailability = availabilityMongoRepository
                                    .findById(availability.getId())
                                    .orElse(new AvailabilityMongo(availability));
                            mongoAvailability.setOrderIndex(availability.getOrderIndex());
                            mongoAvailability.setUpdatedAt(LocalDateTime.now());
                            return mongoAvailability;
                        })
                        .collect(Collectors.toList());
                
                availabilityMongoRepository.saveAll(mongoAvailabilities);
                log.info("Successfully updated availability order in MongoDB for electionId: {}", electionId);
                
            } catch (Exception mongoEx) {
                log.error("Failed to update availability order in MongoDB for electionId: {}", electionId, mongoEx);
                throw new RuntimeException("MongoDB reorder failed, triggering rollback", mongoEx);
            }
            
            log.info("Availability order updated successfully in both PostgreSQL and MongoDB for electionId: {}", electionId);
            
        } catch (Exception ex) {
            log.error("Failed to update availability order for electionId: {}", electionId, ex);
            
            if (ex instanceof ThedalException) {
                throw ex;
            } else {
                throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
    }
    
    
    @Transactional
    public ThedalResponse<List<Map<String, Object>>> getAvailabilityFromMongo(Long accountId, Long electionId) {
        log.info("Fetching availabilities from PostgreSQL for accountId: {}, electionId: {}", accountId, electionId);

        validateElectionOwnership(electionId, accountId);

        // Read from PostgreSQL instead of MongoDB
        List<Availability> availabilities = availabilityRepository.findByAccountIdAndElectionIdOrderByOrderIndexAsc(accountId, electionId);
        if (availabilities.isEmpty()) {
            log.warn("No availabilities found in PostgreSQL for accountId: {}, electionId: {}", accountId, electionId);
            throw new ThedalException(ThedalError.AVAILABILITY_NOT_FOUND, HttpStatus.NOT_FOUND);
        }
        
        List<Map<String, Object>> availabilityDetails = availabilities.stream()
                .map(availability -> {
                    Map<String, Object> availabilityData = new HashMap<>();
                    availabilityData.put("id", availability.getId());
                    availabilityData.put("categoryName", availability.getCategoryName() != null ? availability.getCategoryName() : "");
                    availabilityData.put("description", availability.getDescription() != null ? availability.getDescription() : "");
                    availabilityData.put("availabilityImage", availability.getAvailabilityImage() != null ? availability.getAvailabilityImage() : "");
                    availabilityData.put("orderIndex", availability.getOrderIndex() != null ? availability.getOrderIndex() : 0);
                    return availabilityData;
                })
                .sorted(Comparator.comparingInt(availability -> (Integer) availability.get("orderIndex")))
                .collect(Collectors.toList());

        log.info("Successfully fetched {} availabilities from PostgreSQL for electionId: {}", availabilityDetails.size(), electionId);
        return new ThedalResponse<>(ThedalSuccess.AVAILABILITY_FOUND, availabilityDetails);
    }

     
}
