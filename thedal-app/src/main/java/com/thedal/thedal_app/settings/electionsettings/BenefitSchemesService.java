package com.thedal.thedal_app.settings.electionsettings;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.repository.Query;
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
import com.thedal.thedal_app.settings.electionsettings.dto.BenefitSchemeReorderRequest;
import com.thedal.thedal_app.settings.electionsettings.dto.BenefitSchemesDTO;
import com.thedal.thedal_app.settings.electionsettings.dto.BenefitSchemesUpdateDTO;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;
import com.thedal.thedal_app.util.RandomTokenGenerator;
import com.thedal.thedal_app.voter.VoterBenefitScheme;
import com.thedal.thedal_app.voter.VoterBenefitSchemeRepository;
import com.thedal.thedal_app.voter.VoterEntity;
import com.thedal.thedal_app.voter.VoterRepo;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;


@Service
@Slf4j
public class BenefitSchemesService {

   @Autowired
    private RequestDetailsService requestDetails;   
    @Autowired
    private BenefitSchemesRepository benefitSchemesRepository;
    @Autowired
    private VoterRepo voterRepository;
    @Value("${aws.s3.banner.bucket}")
	private String s3bucket;
    @Autowired
    private AwsFileUpload awsFileUpload;
    @Autowired
    private BenefitSchemesMongoRepository benefitSchemesMongoRepository;
    @Autowired
    private ElectionRepository electionRepository;
    @Autowired
    private VoterBenefitSchemeRepository voterBenefitSchemeRepository;
    @Autowired
    private EntityManager entityManager;
    

    private void validateElectionOwnership(Long electionId, Long accountId) {
        Optional<ElectionEntity> electionOpt = electionRepository.findByIdAndAccountId(electionId, accountId);
        if (!electionOpt.isPresent()) {
            log.error("Election ID {} does not belong to Account ID {}", electionId, accountId);
            throw new ThedalException(ThedalError.INVALID_ELECTION, HttpStatus.FORBIDDEN);
        }
    }
    
    public ThedalResponse<BenefitSchemesDTO> createBenefitScheme(BenefitSchemesUpdateDTO benefitSchemesDTO,Long electionId,MultipartFile file) {
    	
        Long accountId = requestDetails.getCurrentAccountId();

        if (accountId == null) {
            log.error("Account ID not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }
        
        Optional<BenefitSchemes> existingScheme = benefitSchemesRepository
            .findBySchemeNameAndAccountIdAndElectionId(benefitSchemesDTO.getSchemeName(), accountId, electionId);

        if (existingScheme.isPresent()) {
            throw new ThedalException(
                    ThedalError.SCHEME_ALREADY_EXISTS,
                    HttpStatus.CONFLICT,
                    "Benefit Scheme with name '" + benefitSchemesDTO.getSchemeName() + "' already exists for the given election."
              );
          }
    
        // Check for duplicate scheme in MongoDB
        if (benefitSchemesMongoRepository.existsBySchemeNameAndElectionId(benefitSchemesDTO.getSchemeName(), electionId)) {
            String errorMessage = String.format("Benefit Scheme with name '%s' already exists in MongoDB for election '%d'.", 
                    benefitSchemesDTO.getSchemeName(), electionId);
            log.error(errorMessage);
            throw new ThedalException(ThedalError.SCHEME_ALREADY_EXISTS, HttpStatus.CONFLICT, errorMessage);
        }
        
        SchemeBy schemeBy;
        try {
            schemeBy = SchemeBy.valueOf(benefitSchemesDTO.getSchemeBy().toUpperCase());
        } catch (IllegalArgumentException e) {
            log.error("Invalid schemeBy value: {}", benefitSchemesDTO.getSchemeBy());
            throw new ThedalException(ThedalError.INVALID_SCHEME_BY, HttpStatus.BAD_REQUEST);
        }
        
        String uploadUrl = null;
        if(file!=null && !file.isEmpty()) { 
         try {
             uploadUrl = uploadAvaiablityImageToAWS(file);
         } catch (Exception e) {
             log.error("Error uploading image: ", e);
             throw new ThedalException(ThedalError.IMAGE_UPLOAD_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
         }
        }	

        // Increment order index for existing schemes to make room at position 0
        List<BenefitSchemes> existingSchemes = benefitSchemesRepository.findByElectionIdAndAccountIdOrderByOrderIndexAsc(electionId, accountId);
        for (BenefitSchemes existingSchemeItem : existingSchemes) {
            existingSchemeItem.setOrderIndex(existingSchemeItem.getOrderIndex() + 1);
        }
        
        BenefitSchemes benefitSchemes = new BenefitSchemes();
        benefitSchemes.setSchemeName(benefitSchemesDTO.getSchemeName());
        benefitSchemes.setImageUrl(uploadUrl);
        benefitSchemes.setSchemeBy(schemeBy);
        benefitSchemes.setUserSelection(benefitSchemesDTO.getUserSelection());
        benefitSchemes.setAccountId(accountId);
        benefitSchemes.setElectionId(electionId);
        benefitSchemes.setOrderIndex(0);
        benefitSchemes.setSchemeValue(benefitSchemesDTO.getSchemeValue());

        try {
            // Save existing schemes with updated order first
            benefitSchemesRepository.saveAll(existingSchemes);
            
            // Save new scheme to PostgreSQL
            BenefitSchemes savedBenefitScheme = benefitSchemesRepository.saveAndFlush(benefitSchemes);
            
            try {
                // Save to MongoDB
                BenefitSchemesMongo benefitSchemesMongo = new BenefitSchemesMongo(savedBenefitScheme);
                benefitSchemesMongoRepository.save(benefitSchemesMongo);
                log.info("Benefit scheme saved to MongoDB with ID: {}", savedBenefitScheme.getId());
            } catch (Exception mongoEx) {
                log.error("Failed to save benefit scheme to MongoDB: id={}, name={}", savedBenefitScheme.getId(), savedBenefitScheme.getSchemeName(), mongoEx);
                throw new RuntimeException("MongoDB save failed, triggering rollback", mongoEx);
            }
            
            BenefitSchemesDTO benefitSchemesDTOResponse = new BenefitSchemesDTO(
                savedBenefitScheme.getId(),
                savedBenefitScheme.getSchemeName(),
                savedBenefitScheme.getImageUrl(),
                savedBenefitScheme.getSchemeBy().getValue(),
                savedBenefitScheme.getOrderIndex(),
                savedBenefitScheme.getSchemeValue(),
                savedBenefitScheme.getUserSelection()
            );
            
            log.info("Benefit scheme created successfully: {}", savedBenefitScheme.getSchemeName());
            return new ThedalResponse<>(ThedalSuccess.BENEFITSCHEME_CREATED, benefitSchemesDTOResponse);
            
        } catch (Exception ex) {
            log.error("Failed to create benefit scheme: {}", benefitSchemesDTO.getSchemeName(), ex);
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

//    public ThedalResponse<List<BenefitSchemesDTO>> getAll(Long accountId,Long electionId) {
//    	
//        //List<BenefitSchemes> benefitSchemesList = benefitSchemesRepository.findByAccountIdAndElectionId(accountId, electionId);
//    	List<BenefitSchemes> benefitSchemesList = benefitSchemesRepository
//        .findByAccountIdAndElectionIdOrderByCreatedAtDesc(accountId, electionId);
//
//        if (benefitSchemesList.isEmpty()) {
//            return new ThedalResponse<>(ThedalSuccess.BENEFITSCHEMES_FETCHED, Collections.emptyList());
//        }
//    
//        // Sort schemes: updatedAt DESC, then createdAt DESC
//        benefitSchemesList.sort(Comparator
//            .comparing(BenefitSchemes::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
//            .thenComparing(BenefitSchemes::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
//        );
//    
//    	
//        List<BenefitSchemesDTO> benefitSchemesDTOList = benefitSchemesList.stream()
//        .map(benefitSchemes -> new BenefitSchemesDTO(
//            benefitSchemes.getId(),
//            benefitSchemes.getSchemeName(),
//            benefitSchemes.getImageUrl(),
//            benefitSchemes.getSchemeBy().getValue(),
//            benefitSchemes.getOrderIndex()
//        ))
//        .collect(Collectors.toList());
//
//    log.info("Successfully fetched {} benefit schemes for accountId: {}, electionId: {}", 
//        benefitSchemesDTOList.size(), accountId, electionId);
//
//
//       return new ThedalResponse<>(ThedalSuccess.BENEFITSCHEMES_FETCHED, benefitSchemesDTOList);
//    }

    @Transactional
    public ThedalResponse<List<Map<String, Object>>> getAll(Long accountId, Long electionId) {
        log.info("Fetching benefit schemes from MongoDB for accountId: {}, electionId: {}", accountId, electionId);

        List<BenefitSchemesMongo> schemes = benefitSchemesMongoRepository.findByAccountIdAndElectionId(accountId, electionId);
        if (schemes.isEmpty()) {
            log.info("No benefit schemes found in MongoDB for accountId: {}, electionId: {}", accountId, electionId);
            return new ThedalResponse<>(ThedalSuccess.BENEFITSCHEMES_FETCHED, Collections.emptyList());
        }

        List<Map<String, Object>> benefitSchemesDetails = schemes.stream()
                .map(scheme -> {
                    Map<String, Object> schemeData = new HashMap<>();
                    schemeData.put("id", scheme.getId());
                    schemeData.put("schemeName", scheme.getSchemeName() != null ? scheme.getSchemeName() : "");
                    schemeData.put("imageUrl", scheme.getImageUrl() != null ? scheme.getImageUrl() : "");
                    schemeData.put("schemeBy", scheme.getSchemeBy() != null ? scheme.getSchemeBy() : "");
                    schemeData.put("orderIndex", scheme.getOrderIndex() != null ? scheme.getOrderIndex() : 0);
                    schemeData.put("schemeValue", scheme.getSchemeValue());
                    schemeData.put("userSelection", scheme.getUserSelection());
                    // Voter count not available in MongoDB; set to 0 for performance
                    schemeData.put("voterCount", 0L);
                    return schemeData;
                })
                .sorted(Comparator.comparingInt(scheme -> (Integer) scheme.get("orderIndex")))
                .collect(Collectors.toList());

        log.info("Successfully fetched {} benefit schemes from MongoDB for accountId: {}, electionId: {}", 
                benefitSchemesDetails.size(), accountId, electionId);
        return new ThedalResponse<>(ThedalSuccess.BENEFITSCHEMES_FETCHED, benefitSchemesDetails);
    }
    
  public ThedalResponse<BenefitSchemesDTO> updateBenefitScheme(
        Long benefitSchemeId, Long electionId, BenefitSchemesUpdateDTO benefitSchemesDTO,MultipartFile file) {
    	
    Long accountId = requestDetails.getCurrentAccountId();

    if (accountId == null) {
        log.error("Account ID not found, unauthorized access.");
        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
    }
    
    Optional<BenefitSchemes> existingScheme = benefitSchemesRepository
    .findBySchemeNameAndAccountIdAndElectionId(benefitSchemesDTO.getSchemeName(), accountId, electionId);

     // If an existing scheme is found and it's not the same scheme being updated (based on ID)
     if (existingScheme.isPresent() && !existingScheme.get().getId().equals(benefitSchemeId)) {
        String errorMessage = String.format("Benefit Scheme with name '%s' already exists for election '%d' and account '%d'.", 
                                        benefitSchemesDTO.getSchemeName(), electionId, accountId);
       log.error(errorMessage);
       throw new ThedalException(ThedalError.SCHEME_ALREADY_EXISTS, HttpStatus.CONFLICT, errorMessage);
      }
     
     // Check for duplicate scheme in MongoDB (excluding current scheme)
     Optional<BenefitSchemesMongo> mongoScheme = benefitSchemesMongoRepository
             .findBySchemeNameAndElectionId(benefitSchemesDTO.getSchemeName(), electionId);
     if (mongoScheme.isPresent() && !mongoScheme.get().getId().equals(benefitSchemeId)) {
         String errorMessage = String.format("Benefit Scheme with name '%s' already exists in MongoDB for election '%d'.", 
                 benefitSchemesDTO.getSchemeName(), electionId);
         log.error(errorMessage);
         throw new ThedalException(ThedalError.SCHEME_ALREADY_EXISTS, HttpStatus.CONFLICT, errorMessage);
     }
    
       BenefitSchemes existingBenefitScheme = benefitSchemesRepository
            .findByIdAndAccountIdAndElectionId(benefitSchemeId, accountId, electionId)
            .orElseThrow(() -> {
                log.error("BenefitScheme not found with ID: {}, AccountID: {}, ElectionID: {}",
                        benefitSchemeId, accountId, electionId);
                return new ThedalException(ThedalError.BENEFITSCHEME_NOT_FOUND, HttpStatus.NOT_FOUND);
            });

   
    SchemeBy schemeBy;
    try {
        schemeBy = SchemeBy.valueOf(benefitSchemesDTO.getSchemeBy().toUpperCase());
    } catch (IllegalArgumentException e) {
        log.error("Invalid schemeBy value: {}", benefitSchemesDTO.getSchemeBy());
        throw new ThedalException(ThedalError.INVALID_SCHEME_BY, HttpStatus.BAD_REQUEST);
    }    
  
    String uploadUrl = null;
    if (file != null && !file.isEmpty()) {
        try {
            uploadUrl = uploadAvaiablityImageToAWS(file);
        } catch (Exception e) {
            log.error("Error uploading image: ", e);
            throw new ThedalException(ThedalError.IMAGE_UPLOAD_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    // Store old values for rollback
    String oldSchemeName = existingBenefitScheme.getSchemeName();
    String oldImageUrl = existingBenefitScheme.getImageUrl();
    SchemeBy oldSchemeBy = existingBenefitScheme.getSchemeBy();
    Integer oldOrderIndex = existingBenefitScheme.getOrderIndex();
    Double oldSchemeValue = existingBenefitScheme.getSchemeValue();
    Boolean oldUserSelection = existingBenefitScheme.getUserSelection();
    
    existingBenefitScheme.setSchemeName(benefitSchemesDTO.getSchemeName());
    existingBenefitScheme.setSchemeBy(schemeBy);
    existingBenefitScheme.setImageUrl(uploadUrl);
    existingBenefitScheme.setSchemeValue(benefitSchemesDTO.getSchemeValue());
    existingBenefitScheme.setUserSelection(benefitSchemesDTO.getUserSelection());
    
    // Move to top by updating order indexes
    List<BenefitSchemes> allSchemes = benefitSchemesRepository
        .findByElectionIdAndAccountIdOrderByOrderIndexAsc(electionId, accountId);

    for (BenefitSchemes scheme : allSchemes) {
        if (!scheme.getId().equals(benefitSchemeId)) {
            scheme.setOrderIndex(scheme.getOrderIndex() + 1);
        }
    }

    existingBenefitScheme.setOrderIndex(0);

    try {
        // Save updated order to PostgreSQL
        benefitSchemesRepository.saveAll(allSchemes);
        BenefitSchemes savedBenefitScheme = benefitSchemesRepository.saveAndFlush(existingBenefitScheme);

        try {
            // Update MongoDB
            BenefitSchemesMongo benefitSchemesMongo = benefitSchemesMongoRepository
                    .findByIdAndAccountIdAndElectionId(benefitSchemeId, accountId, electionId)
                    .orElse(new BenefitSchemesMongo(savedBenefitScheme));
            
            benefitSchemesMongo.setId(savedBenefitScheme.getId());
            benefitSchemesMongo.setSchemeName(savedBenefitScheme.getSchemeName());
            benefitSchemesMongo.setSchemeBy(savedBenefitScheme.getSchemeBy().getValue());
            benefitSchemesMongo.setImageUrl(savedBenefitScheme.getImageUrl());
            benefitSchemesMongo.setOrderIndex(savedBenefitScheme.getOrderIndex());
            benefitSchemesMongo.setSchemeValue(savedBenefitScheme.getSchemeValue());
            benefitSchemesMongo.setUserSelection(savedBenefitScheme.getUserSelection());
            benefitSchemesMongo.setAccountId(savedBenefitScheme.getAccountId());
            benefitSchemesMongo.setElectionId(savedBenefitScheme.getElectionId());
            benefitSchemesMongo.setUpdatedAt(LocalDateTime.now());
            
            benefitSchemesMongoRepository.save(benefitSchemesMongo);
            log.info("Benefit scheme updated in MongoDB with ID: {}", benefitSchemesMongo.getId());
        } catch (Exception mongoEx) {
            log.error("Failed to update benefit scheme in MongoDB: id={}, name={}", benefitSchemeId, savedBenefitScheme.getSchemeName(), mongoEx);
            
            // Rollback PostgreSQL changes
            try {
                existingBenefitScheme.setSchemeName(oldSchemeName);
                existingBenefitScheme.setImageUrl(oldImageUrl);
                existingBenefitScheme.setSchemeBy(oldSchemeBy);
                existingBenefitScheme.setOrderIndex(oldOrderIndex);
                existingBenefitScheme.setSchemeValue(oldSchemeValue);
                existingBenefitScheme.setUserSelection(oldUserSelection);
                benefitSchemesRepository.saveAndFlush(existingBenefitScheme);
                
                // Restore old order for other schemes
                for (BenefitSchemes scheme : allSchemes) {
                    if (!scheme.getId().equals(benefitSchemeId)) {
                        scheme.setOrderIndex(scheme.getOrderIndex() - 1);
                    }
                }
                benefitSchemesRepository.saveAll(allSchemes);
                log.info("Rolled back benefit scheme update in PostgreSQL for ID: {}", benefitSchemeId);
            } catch (Exception rollbackEx) {
                log.error("Failed to rollback benefit scheme update for ID: {}", benefitSchemeId, rollbackEx);
            }
            
            throw new RuntimeException("MongoDB update failed, triggering rollback", mongoEx);
        }
        
        BenefitSchemesDTO benefitSchemesDTOResponse = new BenefitSchemesDTO(
                savedBenefitScheme.getId(),
                savedBenefitScheme.getSchemeName(),
                savedBenefitScheme.getImageUrl(),
                savedBenefitScheme.getSchemeBy().getValue(),
                savedBenefitScheme.getOrderIndex(),
                savedBenefitScheme.getSchemeValue(),
                savedBenefitScheme.getUserSelection()
        );

        log.info("BenefitScheme with ID {} successfully updated by account ID {}", benefitSchemeId, accountId);
        return new ThedalResponse<>(ThedalSuccess.BENEFITSCHEME_UPDATED, benefitSchemesDTOResponse);
        
    } catch (Exception ex) {
        log.error("Failed to update benefit scheme: id={}", benefitSchemeId, ex);
        throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

//public void deleteBenefitScheme(Long accountId, Long electionId, List<Long> benefitSchemeIds) {
//    List<BenefitSchemes> benefitSchemesList;
//
//    if (benefitSchemeIds == null || benefitSchemeIds.isEmpty()) {
//        // Delete all benefit schemes for the given account and election
//        benefitSchemesList = benefitSchemesRepository.findByAccountIdAndElectionId(accountId, electionId);
//        log.info("Deleting all benefit schemes for accountId: {}, electionId: {}", accountId, electionId);
//    } else {
//        // Delete only the specified benefit schemes
//        benefitSchemesList = benefitSchemesRepository.findByIdInAndAccountIdAndElectionId(benefitSchemeIds, accountId, electionId);
//        log.info("Deleting specific benefit schemes for accountId: {}, electionId: {}, schemeIds: {}", 
//                 accountId, electionId, benefitSchemeIds);
//    }
//
//    if (benefitSchemesList.isEmpty()) {
//        throw new ThedalException(ThedalError.BENEFITSCHEME_NOT_FOUND, HttpStatus.NOT_FOUND);
//    }
//
//    // Check if any scheme is linked to a voter
//    for (BenefitSchemes scheme : benefitSchemesList) {
//        boolean isLinked = voterRepository.existsBySchemeAndElectionId(scheme.getSchemeName(), electionId);
//    
//        if (isLinked) {
//            log.error("Cannot delete scheme '{}': It is linked to a voter.", scheme.getSchemeName());
//            throw new ThedalException(ThedalError.BENEFIT_SCHEME_LINKED_TO_VOTER, HttpStatus.BAD_REQUEST);
//        }
//    }
//
//    // Delete all valid benefit schemes
//    benefitSchemesRepository.deleteAll(benefitSchemesList);
//    log.info("Successfully deleted {} benefit schemes", benefitSchemesList.size());
//}
  @Transactional
  public void deleteBenefitScheme(Long accountId, Long electionId, List<Long> benefitSchemeIds) {
      List<BenefitSchemes> benefitSchemesList;
      
      if (benefitSchemeIds == null || benefitSchemeIds.isEmpty()) {
          log.info("Deleting all benefit schemes for accountId: {}, electionId: {}", accountId, electionId);
          benefitSchemesList = benefitSchemesRepository.findByAccountIdAndElectionId(accountId, electionId);
          if (benefitSchemesList.isEmpty()) {
              log.warn("No benefit schemes found for accountId: {}, electionId: {}", accountId, electionId);
              throw new ThedalException(ThedalError.BENEFITSCHEME_NOT_FOUND, HttpStatus.NOT_FOUND);
          }

          // Check for linked voters
          List<BenefitSchemes> linkedSchemes = benefitSchemesRepository.findLinkedBenefitSchemes(accountId, electionId);
          if (!linkedSchemes.isEmpty()) {
              List<String> linkedDetails = linkedSchemes.stream()
                      .map(scheme -> String.format("ID: %d, SchemeName: %s", scheme.getId(), scheme.getSchemeName()))
                      .collect(Collectors.toList());
              String errorMessage = String.format("Cannot delete the following benefit schemes as they are associated with voters: %s",
                      String.join("; ", linkedDetails));
              log.error(errorMessage);
              throw new ThedalException(ThedalError.BENEFIT_SCHEME_LINKED_TO_VOTER, HttpStatus.BAD_REQUEST, errorMessage);
          }

          // Proceed with deletion
          try {
              benefitSchemesRepository.deleteAll(benefitSchemesList);
              benefitSchemesRepository.flush();
              
              try {
                  benefitSchemesMongoRepository.deleteByAccountIdAndElectionId(accountId, electionId);
                  log.info("Deleted all benefit schemes from both PostgreSQL and MongoDB for accountId: {}, electionId: {}", 
                          accountId, electionId);
              } catch (Exception mongoEx) {
                  log.error("Failed to delete benefit schemes from MongoDB for accountId: {}, electionId: {}", accountId, electionId, mongoEx);
                  throw new RuntimeException("MongoDB deletion failed, triggering rollback", mongoEx);
              }
              
          } catch (Exception ex) {
              log.error("Failed to delete benefit schemes for accountId: {}, electionId: {}", accountId, electionId, ex);
              throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
          }
      } else {
          log.info("Deleting specific benefit schemes for accountId: {}, electionId: {}, schemeIds: {}", 
                  accountId, electionId, benefitSchemeIds);
          benefitSchemesList = benefitSchemesRepository.findByIdInAndAccountIdAndElectionId(benefitSchemeIds, accountId, electionId);
          if (benefitSchemesList.isEmpty()) {
              log.warn("No benefit schemes found for IDs: {}, accountId: {}, electionId: {}", 
                      benefitSchemeIds, accountId, electionId);
              throw new ThedalException(ThedalError.BENEFITSCHEME_NOT_FOUND, HttpStatus.NOT_FOUND);
          }

          // Check for linked voters
          List<BenefitSchemes> linkedSchemes = benefitSchemesRepository.findLinkedBenefitSchemesByIds(benefitSchemeIds, accountId, electionId);
          if (!linkedSchemes.isEmpty()) {
              List<String> linkedDetails = linkedSchemes.stream()
                      .map(scheme -> String.format("ID: %d, SchemeName: %s", scheme.getId(), scheme.getSchemeName()))
                      .collect(Collectors.toList());
              String errorMessage = String.format("Cannot delete the following benefit schemes as they are associated with voters: %s",
                      String.join("; ", linkedDetails));
              log.error(errorMessage);
              throw new ThedalException(ThedalError.BENEFIT_SCHEME_LINKED_TO_VOTER, HttpStatus.BAD_REQUEST, errorMessage);
          }

          // Proceed with deletion
          try {
              benefitSchemesRepository.deleteAll(benefitSchemesList);
              benefitSchemesRepository.flush();
              
              try {
                  benefitSchemesMongoRepository.deleteByIdIn(benefitSchemeIds);
                  log.info("Deleted benefit schemes from both PostgreSQL and MongoDB: {}", benefitSchemeIds);
              } catch (Exception mongoEx) {
                  log.error("Failed to delete benefit schemes from MongoDB: {}", benefitSchemeIds, mongoEx);
                  throw new RuntimeException("MongoDB deletion failed, triggering rollback", mongoEx);
              }
              
          } catch (Exception ex) {
              log.error("Failed to delete benefit schemes: {}", benefitSchemeIds, ex);
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

        long maxFileSize = 512 * 1024; //less than 500KB
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

     @Transactional
     public void updateBenefitSchemeOrder(List<BenefitSchemeReorderRequest> reorderRequests, Long accountId, Long electionId) {
         List<BenefitSchemes> benefitSchemes = benefitSchemesRepository.findByElectionIdAndAccountIdOrderByOrderIndexAsc(electionId, accountId);

         if (benefitSchemes.isEmpty()) {
             log.error("No benefit schemes found for election ID {} and account ID {}", electionId, accountId);
             throw new ThedalException(ThedalError.BENEFITSCHEME_NOT_FOUND, HttpStatus.NOT_FOUND);
         }

         // Create a map of schemeId -> newOrderIndex
         Map<Long, Integer> newOrderMap = reorderRequests.stream()
                 .collect(Collectors.toMap(BenefitSchemeReorderRequest::getSchemeId, BenefitSchemeReorderRequest::getNewOrderIndex));

         // Sort existing benefit schemes before modifying
         benefitSchemes.sort(Comparator.comparing(BenefitSchemes::getOrderIndex));

         // Remove benefit schemes that are being reordered
         List<BenefitSchemes> reorderedSchemes = new ArrayList<>(benefitSchemes);
         reorderedSchemes.removeIf(scheme -> newOrderMap.containsKey(scheme.getId()));

         // Insert schemes at their new positions
         for (BenefitSchemeReorderRequest request : reorderRequests) {
             BenefitSchemes scheme = benefitSchemes.stream()
                     .filter(s -> s.getId().equals(request.getSchemeId()))
                     .findFirst()
                     .orElseThrow(() -> new ThedalException(ThedalError.BENEFITSCHEME_NOT_FOUND, HttpStatus.NOT_FOUND));

             reorderedSchemes.add(request.getNewOrderIndex(), scheme);
         }

         // Update `orderIndex` for all benefit schemes
         for (int i = 0; i < reorderedSchemes.size(); i++) {
             reorderedSchemes.get(i).setOrderIndex(i);
             log.info("Updated benefit scheme order: {} -> {}", reorderedSchemes.get(i).getSchemeName(), i);
         }

         try {
             // Save updated order to PostgreSQL
             List<BenefitSchemes> savedSchemes = benefitSchemesRepository.saveAll(reorderedSchemes);
             
             try {
                 // Update MongoDB as well
                 List<BenefitSchemesMongo> mongoSchemes = savedSchemes.stream()
                         .map(scheme -> {
                             BenefitSchemesMongo mongoScheme = benefitSchemesMongoRepository
                                     .findByIdAndAccountIdAndElectionId(scheme.getId(), accountId, electionId)
                                     .orElse(new BenefitSchemesMongo(scheme));
                             mongoScheme.setOrderIndex(scheme.getOrderIndex());
                             mongoScheme.setUpdatedAt(LocalDateTime.now());
                             return mongoScheme;
                         })
                         .collect(Collectors.toList());
                 
                 benefitSchemesMongoRepository.saveAll(mongoSchemes);
                 log.info("Updated benefit scheme order in MongoDB for electionId: {}", electionId);
             } catch (Exception mongoEx) {
                 log.error("Failed to update benefit scheme order in MongoDB for electionId: {}", electionId, mongoEx);
                 throw new RuntimeException("MongoDB reorder failed, triggering rollback", mongoEx);
             }
             
             log.info("Benefit scheme order updated successfully for electionId: {}", electionId);
         } catch (Exception ex) {
             log.error("Failed to update benefit scheme order for electionId: {}", electionId, ex);
             throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
         }
     }

//     @Transactional
//     public ThedalResponse<List<Map<String, Object>>> getAllFromMongo(Long accountId, Long electionId) {
//         log.info("Fetching benefit schemes from PostgreSQL for accountId: {}, electionId: {}", accountId, electionId);
//
//         List<BenefitSchemes> schemes = benefitSchemesRepository.findByAccountIdAndElectionId(accountId, electionId);
//         if (schemes.isEmpty()) {
//             log.warn("No benefit schemes found in PostgreSQL for accountId: {}, electionId: {}", accountId, electionId);
//             throw new ThedalException(ThedalError.BENEFITSCHEME_NOT_FOUND, HttpStatus.NOT_FOUND);
//         }
//
//         List<Map<String, Object>> schemeDetails = schemes.stream()
//                 .map(scheme -> {
//                     Map<String, Object> schemeData = new HashMap<>();
//                     schemeData.put("id", scheme.getId());
//                     schemeData.put("schemeName", scheme.getSchemeName() != null ? scheme.getSchemeName() : "");
//                     schemeData.put("imageUrl", scheme.getImageUrl() != null ? scheme.getImageUrl() : "");
//                     schemeData.put("schemeBy", scheme.getSchemeBy() != null ? scheme.getSchemeBy() : "");
//                     schemeData.put("orderIndex", scheme.getOrderIndex() != null ? scheme.getOrderIndex() : 0);
//                     schemeData.put("schemeValue", scheme.getSchemeValue());
//                     schemeData.put("userSelection", scheme.getUserSelection());
//                     // Calculate voter count from association
////                     Long voterCount = (scheme.getVoters() != null) ? (long) scheme.getVoters().size() : 0L;
////                     schemeData.put("voterCount", voterCount);
//                     return schemeData;
//                 })
//                 .sorted(Comparator.comparingInt(scheme -> (Integer) scheme.get("orderIndex")))
//                 .collect(Collectors.toList());
//
//         log.info("Successfully fetched {} benefit schemes from PostgreSQL for electionId: {}", schemeDetails.size(), electionId);
//         return new ThedalResponse<>(ThedalSuccess.BENEFITSCHEMES_FETCHED, schemeDetails);
//     }
     @Transactional
     public ThedalResponse<List<Map<String, Object>>> getAllFromMongo(Long accountId, Long electionId) {
         log.info("Fetching benefit schemes from PostgreSQL for accountId: {}, electionId: {}", accountId, electionId);

         List<BenefitSchemes> schemes = benefitSchemesRepository.findByAccountIdAndElectionId(accountId, electionId);
         if (schemes.isEmpty()) {
             log.warn("No benefit schemes found in PostgreSQL for accountId: {}, electionId: {}", accountId, electionId);
             throw new ThedalException(ThedalError.BENEFITSCHEME_NOT_FOUND, HttpStatus.NOT_FOUND);
         }

         List<Map<String, Object>> schemeDetails = schemes.stream()
                 .map(scheme -> {
                     Map<String, Object> schemeData = new HashMap<>();
                     schemeData.put("id", scheme.getId());
                     schemeData.put("schemeName", scheme.getSchemeName() != null ? scheme.getSchemeName() : "");
                     schemeData.put("imageUrl", scheme.getImageUrl() != null ? scheme.getImageUrl() : "");
                     schemeData.put("schemeBy", scheme.getSchemeBy() != null ? scheme.getSchemeBy().toString() : "");
                     schemeData.put("orderIndex", scheme.getOrderIndex() != null ? scheme.getOrderIndex() : 0);
                     schemeData.put("schemeValue", scheme.getSchemeValue());
                     schemeData.put("userSelection", scheme.getUserSelection());
                     
                     // Calculate voter count from voter_benefit_schemes table
                     Long voterCount = voterBenefitSchemeRepository.countVotersByBenefitSchemeId(
                             scheme.getId(), accountId, electionId);
                     schemeData.put("voterCount", voterCount != null ? voterCount : 0L);

                     return schemeData;
                 })
                 .sorted(Comparator.comparingInt(scheme -> (Integer) scheme.get("orderIndex")))
                 .collect(Collectors.toList());

         log.info("Successfully fetched {} benefit schemes from PostgreSQL for electionId: {}", schemeDetails.size(), electionId);
         return new ThedalResponse<>(ThedalSuccess.BENEFITSCHEMES_FETCHED, schemeDetails);
     }

//     @Transactional
//     public int migrateVoterBenefitSchemes() {
//         Long accountId = requestDetails.getCurrentAccountId();
//         if (accountId == null) {
//             log.error("Account ID not found, unauthorized access.");
//             throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
//         }
//
//         // Native SQL query to fetch voter_id and benefit_scheme_id
//         jakarta.persistence.Query query = entityManager.createNativeQuery(
//             "SELECT id, benefit_scheme_id FROM _voters WHERE account_id = :accountId AND benefit_scheme_id IS NOT NULL"
//         );
//         query.setParameter("accountId", accountId);
//         List<Object[]> results = query.getResultList();
//
//         int migratedCount = 0;
//         for (Object[] result : results) {
//             Long voterId = ((Number) result[0]).longValue();
//             Long benefitSchemeId = ((Number) result[1]).longValue();
//
//             // Fetch the BenefitSchemes entity
//             BenefitSchemes benefitScheme = entityManager.find(BenefitSchemes.class, benefitSchemeId);
//             if (benefitScheme == null) {
//                 log.warn("Invalid benefit_scheme_id {} for voter ID {}", benefitSchemeId, voterId);
//                 continue; // Skip invalid schemes
//             }
//
//             // Fetch the VoterEntity
//             VoterEntity voter = entityManager.find(VoterEntity.class, voterId);
//             if (voter == null) {
//                 log.warn("Invalid voter ID {}", voterId);
//                 continue; // Skip invalid voters
//             }
//
//             // Check if the mapping already exists in voter_benefit_schemes
//             jakarta.persistence.Query checkQuery = entityManager.createQuery(
//                 "SELECT COUNT(vbs) FROM VoterBenefitScheme vbs WHERE vbs.voter.id = :voterId AND vbs.benefitScheme.id = :schemeId"
//             );
//             checkQuery.setParameter("voterId", voterId);
//             checkQuery.setParameter("schemeId", benefitSchemeId);
//             Long existingCount = (Long) checkQuery.getSingleResult();
//
//             if (existingCount > 0) {
//                 log.info("VoterBenefitScheme already exists for voter ID {} and scheme ID {}", voterId, benefitSchemeId);
//                 continue; // Skip if already migrated
//             }
//
//             // Create a new VoterBenefitScheme entry
//             VoterBenefitScheme voterBenefitScheme = new VoterBenefitScheme();
//             voterBenefitScheme.setVoter(voter);
//             voterBenefitScheme.setBenefitScheme(benefitScheme);
//             voterBenefitScheme.setSelected(false); // Set selected to false as per requirement
//
//             // Save the new record
//             entityManager.persist(voterBenefitScheme);
//             migratedCount++;
//         }
//
//         log.info("Successfully migrated {} voter benefit scheme records for accountId {}", migratedCount, accountId);
//         return migratedCount;
//     }
     

}
