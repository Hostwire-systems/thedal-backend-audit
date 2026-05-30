package com.thedal.thedal_app.settings.electionsettings;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.thedal.thedal_app.awsfilestore.AwsFileUpload;
import com.thedal.thedal_app.awsfilestore.ImageUpload;
import com.thedal.thedal_app.election.ElectionRepository;
import com.thedal.thedal_app.general.RequestDetailsService;
import com.thedal.thedal_app.response.ThedalResponse;
import com.thedal.thedal_app.response.ThedalSuccess;
import com.thedal.thedal_app.settings.electionsettings.dto.PartyReorderRequest;
import com.thedal.thedal_app.settings.electionsettings.dto.PartyRequest;
import com.thedal.thedal_app.settings.electionsettings.dto.PartyResponseDTO;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;
import com.thedal.thedal_app.util.RandomTokenGenerator;
import com.thedal.thedal_app.voter.VoterRepo;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PartyService {
	
	@Autowired
	private PartyRepository partyRepository;
	@Autowired
    private VoterRepo voterRepository; 
	@Autowired
    private RequestDetailsService requestDetails;
	@Autowired
    private ImageUpload imageUpload;
	@Autowired
    private AwsFileUpload awsFileUpload;
	@Value("${aws.s3.banner.bucket}")
	private String s3bucket;
	@Autowired
	private ElectionRepository electionRepository;
	@Autowired
    private PartyMongoRepository partyMongoRepository;
	

	@Transactional
	public ThedalResponse<Party> createParty(PartyRequest partyRequest) {
		
		Long accountId = requestDetails.getCurrentAccountId();	    
	    if (accountId == null) {
	        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
	    }
	    // Validate the party request
	    validatePartyRequest(partyRequest);

	    if (!electionRepository.existsById(partyRequest.getElectionId())) {
	        throw new ThedalException(ThedalError.ELECTION_ID_NOT_FOUND, HttpStatus.NOT_FOUND);
	    }

	    // Upload the image to AWS S3
	    String imageUrl;
	    try {
	        imageUrl = uploadPartyImageToAWS(partyRequest.getPartyImage());
	    } catch (Exception ex) {
	        throw new ThedalException(ThedalError.UPLOAD_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
	    }
	    
	 // Find the highest order index for this election
        Integer maxOrderIndex = partyRepository.findMaxOrderIndexByElectionId(partyRequest.getElectionId());
        int newOrderIndex = (maxOrderIndex != null) ? maxOrderIndex + 1 : 0;
        
	    // Create and save the party
	    Party party = new Party();
	    party.setPartyName(partyRequest.getPartyName());
	    party.setPartyShortName(partyRequest.getPartyShortName());
	    party.setPartyImage(imageUrl);
	    party.setPartyColor(partyRequest.getPartyColor());
	    party.setAllianceName(partyRequest.getAllianceName());
	    party.setAccountId(accountId);
	    party.setElectionId(partyRequest.getElectionId());
	    party.setOrderIndex(newOrderIndex); 

	    try {
	        //partyRepository.saveAndFlush(party);
	    	Party savedParty = partyRepository.saveAndFlush(party);
	        try {
                partyMongoRepository.save(new PartyMongo(savedParty));
                log.info("Saved party to MongoDB: id={}", savedParty.getId());
            } catch (Exception mongoEx) {
                log.error("Failed to save party to MongoDB: id={}", savedParty.getId(), mongoEx);
                throw new RuntimeException("MongoDB save failed, triggering rollback", mongoEx);
            }
            return new ThedalResponse<>(ThedalSuccess.PARTY_CREATED, savedParty);
	    } catch (DataIntegrityViolationException ex) {
	        //throw new ThedalException(ThedalError.DUPLICATE_PARTY_NAME, HttpStatus.CONFLICT);
				String errorMessage = "Party with name '" + partyRequest.getPartyName() + "' already exists in election '"
						+ partyRequest.getElectionId() + "'.";
				throw new ThedalException(ThedalError.DUPLICATE_PARTY_NAME, HttpStatus.CONFLICT, errorMessage);

	    }

	    //return new ThedalResponse<>(ThedalSuccess.PARTY_CREATED, party);
	}

	private void validatePartyRequest(PartyRequest partyRequest) {
	    if (partyRequest.getPartyName() == null || partyRequest.getPartyName().isEmpty()) {
	        throw new ThedalException(ThedalError.PARTY_NAME_NOT_FOUND, HttpStatus.BAD_REQUEST);
	    }
	    if (partyRequest.getPartyImage() == null || !isValidImageFormat(partyRequest.getPartyImage())) {
	        throw new ThedalException(ThedalError.INVALID_IMAGE_FORMAT, HttpStatus.BAD_REQUEST);
	    }
	    if (partyRequest.getElectionId() == null) {
	        throw new ThedalException(ThedalError.ELECTION_ID_NOT_FOUND, HttpStatus.BAD_REQUEST);
	    }
	    // if (partyRepository.existsByPartyNameAndElectionId(partyRequest.getPartyName(), partyRequest.getElectionId())) {
	    //     throw new ThedalException(ThedalError.DUPLICATE_PARTY_NAME, HttpStatus.CONFLICT);
	    // }
		if (partyRepository.existsByPartyNameAndElectionId(partyRequest.getPartyName(), partyRequest.getElectionId())) {
			String errorMessage = "Party with name '" + partyRequest.getPartyName() + "' already exists in election '"
					+ partyRequest.getElectionId() + "'.";
			throw new ThedalException(ThedalError.DUPLICATE_PARTY_NAME, HttpStatus.CONFLICT, errorMessage);
		}
	}

	private boolean isValidImageFormat(MultipartFile image) {
	    String contentType = image.getContentType();
	    return contentType != null && (contentType.equals("image/jpeg") || contentType.equals("image/png"));
	}

	

	private String uploadPartyImageToAWS(MultipartFile imageFile) {
	    String contentType = imageFile.getContentType();
	    if (!(MediaType.IMAGE_JPEG_VALUE.equals(contentType) ||
	            MediaType.IMAGE_PNG_VALUE.equals(contentType))) {
	        throw new ThedalException(ThedalError.INVALID_IMAGE_FORMAT, HttpStatus.BAD_REQUEST);
	    }

	    long maxFileSize = 5 * 1024 * 1024; // 5MB
	    if (imageFile.getSize() > maxFileSize) {
	        throw new ThedalException(ThedalError.INVALID_IMAGE_FORMAT, HttpStatus.BAD_REQUEST);
	    }

	    // Generate unique file name
	    String fileExtension = "." + awsFileUpload.getFileExtension(imageFile.getOriginalFilename());
	    String fileName = "party_" + System.currentTimeMillis() + "_" + RandomTokenGenerator.generateToken(10) + fileExtension;

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
	        throw new ThedalException(ThedalError.INVALID_IMAGE_FORMAT, HttpStatus.INTERNAL_SERVER_ERROR);
	    }
	}


////	@Transactional(readOnly = true)
//	@Transactional
//	public ThedalResponse<Party> getPartyById(Long partyId, Long electionId) {
//		
//		Long accountId = requestDetails.getCurrentAccountId();
//	    
//	    if (accountId == null) {
//	        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
//	    }
//
//	    //Party party = partyRepository.findByIdAndAccountId(id, accountId)
//	    Party party = partyRepository.findByIdAndElectionIdAndAccountId(partyId, electionId, accountId)
//	            .orElseThrow(() -> new ThedalException(ThedalError.PARTY_NOT_FOUND, HttpStatus.NOT_FOUND));
//
//	    log.info("Successfully fetched party with ID: {} for accountId: {}", partyId, accountId);
//	    return new ThedalResponse<>(ThedalSuccess.PARTY_FETCHED, party);
//	}
//	
//	public ThedalResponse<List<Party>> getPartiesByElectionId(Long electionId) {
//		
//		Long accountId = requestDetails.getCurrentAccountId();
//	    log.debug("Fetching parties for electionId: {} and accountId: {}", electionId, accountId);
//
//	    if (accountId == null) {
//	        log.error("Account ID not found for the current user.");
//	        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
//	    }
//
//	    //List<Party> parties = partyRepository.findAllByElectionIdAndAccountId(electionId, accountId);
//	    //List<Party> parties = partyRepository.findByElectionIdAndAccountIdOrderByOrderIndexAsc(electionId, accountId);
//		List<Party> parties = partyRepository.findByElectionIdAndAccountId(electionId, accountId);
//
//	    if (parties == null || parties.isEmpty()) {
//	        log.warn("No parties found for electionId: {} and accountId: {}", electionId, accountId);
//	        throw new ThedalException(ThedalError.ELECTION_NOT_FOUND, HttpStatus.NOT_FOUND);
//	    }
//		parties.sort(Comparator
//		.comparing(Party::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
//		.thenComparing(Party::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));
//		
//	    log.info("Successfully fetched {} parties for electionId: {}", parties.size(), electionId);
//	    return new ThedalResponse<>(ThedalSuccess.PARTIES_FETCHED, parties);
//	}    @Transactional
    public ThedalResponse<List<PartyResponseDTO>> getPartiesByElectionId(Long electionId) {
        Long accountId = requestDetails.getCurrentAccountId();
        log.debug("Fetching parties from PostgreSQL for electionId: {} and accountId: {}", electionId, accountId);

        if (accountId == null) {
            log.error("Account ID not found for the current user.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        // Read from PostgreSQL instead of MongoDB
        List<Party> parties = partyRepository.findByAccountIdAndElectionIdOrderByOrderIndexAsc(accountId, electionId);
        if (parties.isEmpty()) {
            log.warn("No parties found in PostgreSQL for electionId: {} and accountId: {}", electionId, accountId);
            throw new ThedalException(ThedalError.PARTY_NOT_FOUND, HttpStatus.NOT_FOUND);
        }

        List<PartyResponseDTO> partyDTOs = parties.stream().map(party -> new PartyResponseDTO(
            party.getId(),
            party.getPartyName() != null ? party.getPartyName() : "",
            party.getPartyShortName() != null ? party.getPartyShortName() : "",
            party.getPartyImage() != null ? party.getPartyImage() : "",
            party.getPartyColor() != null ? party.getPartyColor() : "",
            party.getAllianceName() != null ? party.getAllianceName() : "",
            party.getOrderIndex() != null ? party.getOrderIndex() : 0,
            party.getCreatedAt(),
            party.getUpdatedAt(),
            0L // Voter count not available
        )).collect(Collectors.toList());

        log.info("Successfully fetched {} parties from PostgreSQL for electionId: {}", partyDTOs.size(), electionId);
        return new ThedalResponse<>(ThedalSuccess.PARTIES_FETCHED, partyDTOs);
    }

    @Transactional
    public ThedalResponse<PartyResponseDTO> getPartyById(Long partyId, Long electionId) {
        Long accountId = requestDetails.getCurrentAccountId();
        log.debug("Fetching party from MongoDB with ID: {} for electionId: {} and accountId: {}", partyId, electionId, accountId);

        if (accountId == null) {
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        // Use MongoDB to fetch party
        PartyMongo party = partyMongoRepository.findById(partyId)
                .filter(p -> p.getAccountId().equals(accountId) && p.getElectionId().equals(electionId))
                .orElseThrow(() -> new ThedalException(ThedalError.PARTY_NOT_FOUND, HttpStatus.NOT_FOUND));

        PartyResponseDTO partyDTO = new PartyResponseDTO(
            party.getId(),
            party.getPartyName() != null ? party.getPartyName() : "",
            party.getPartyShortName() != null ? party.getPartyShortName() : "",
            party.getPartyImage() != null ? party.getPartyImage() : "",
            party.getPartyColor() != null ? party.getPartyColor() : "",
            party.getAllianceName() != null ? party.getAllianceName() : "",
            party.getOrderIndex() != null ? party.getOrderIndex() : 0,
            party.getCreatedAt(),
            party.getUpdatedAt(),
            0L // Voter count not available in MongoDB
        );

        log.info("Successfully fetched party from MongoDB with ID: {} for accountId: {}", partyId, accountId);
        return new ThedalResponse<>(ThedalSuccess.PARTY_FETCHED, partyDTO);
    }
    
	@Transactional
	public ThedalResponse<Party> updateParty(Long partyId, Long electionId, PartyRequest partyRequest) {
		
		Long accountId = requestDetails.getCurrentAccountId();

	    if (accountId == null) {
	        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
	    }

	    // Validate the party exists with the provided partyId, accountId, and electionId
	    Party party = partyRepository.findByIdAndAccountIdAndElectionId(partyId, accountId, electionId)
	            .orElseThrow(() -> new ThedalException(ThedalError.PARTY_NOT_FOUND, HttpStatus.NOT_FOUND));
		
		if (partyRequest.getPartyName() != null && !partyRequest.getPartyName().equals(party.getPartyName())) {
			boolean exists = partyRepository.existsByPartyNameAndElectionIdAndIdNot(partyRequest.getPartyName(), electionId, partyId);
			if (exists) {
					throw new ThedalException(ThedalError.DUPLICATE_PARTY_NAME, HttpStatus.CONFLICT, 
							"Party with name '" + partyRequest.getPartyName() + "' already exists in election " + electionId);
					}
					party.setPartyName(partyRequest.getPartyName()); // Update only if it's a new, unique name
				}
	
	    // Update party name if provided
	    if (partyRequest.getPartyName() != null && !partyRequest.getPartyName().isEmpty()) {
	        party.setPartyName(partyRequest.getPartyName());
	    }

	    // Update party short name if provided
	    if (partyRequest.getPartyShortName() != null && !partyRequest.getPartyShortName().isEmpty()) {
	        party.setPartyShortName(partyRequest.getPartyShortName());
	    }

	    // Update party image if provided
	    if (partyRequest.getPartyImage() != null && !partyRequest.getPartyImage().isEmpty()) {
	        String newImageUrl = uploadPartyImageToAWS(partyRequest.getPartyImage());
	        party.setPartyImage(newImageUrl);
	    }
	    
	    // Update party color if provided
	    if (partyRequest.getPartyColor() != null && !partyRequest.getPartyColor().isEmpty()) {
	        party.setPartyColor(partyRequest.getPartyColor());
	    }
	    
	    // Update alliance name if provided
	    if (partyRequest.getAllianceName() != null && !partyRequest.getAllianceName().isEmpty()) {
	        party.setAllianceName(partyRequest.getAllianceName());
	    }

//	    // Save the updated party
//	    partyRepository.save(party);
//	    partyRepository.flush();
//
//	    return new ThedalResponse<>(ThedalSuccess.PARTY_UPDATED, party);
	    
	    try {
            Party updatedParty = partyRepository.saveAndFlush(party);
            try {
                partyMongoRepository.save(new PartyMongo(updatedParty));
                log.info("Updated party in MongoDB: id={}", partyId);
            } catch (Exception mongoEx) {
                log.error("Failed to update party in MongoDB: id={}", partyId, mongoEx);
                throw new RuntimeException("MongoDB update failed, triggering rollback", mongoEx);
            }
            return new ThedalResponse<>(ThedalSuccess.PARTY_UPDATED, updatedParty);
        } catch (DataIntegrityViolationException ex) {
            throw new ThedalException(ThedalError.DUPLICATE_PARTY_NAME, HttpStatus.CONFLICT,
                    "Party with name '" + partyRequest.getPartyName() + "' already exists in election " + electionId);
        }
	    	    
	}


//	@Transactional
//public ThedalResponse<Void> deleteParty(List<Long> partyIds, Long electionId) {
//    
//    log.info("Inside deleteParty method: party IDs: {} for election ID: {}", partyIds, electionId);
//
//    Long accountId = requestDetails.getCurrentAccountId();
//
//    if (accountId == null) {
//        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
//    }
//
//	List<Party> parties;
//
//    if (partyIds == null || partyIds.isEmpty()) {
//        // If no party IDs are provided, delete all parties for the given election and account
//        parties = partyRepository.findByAccountIdAndElectionId(accountId, electionId);
//        log.info("No specific party IDs provided. Fetching all parties for deletion.");
//    } else {
//        // Fetch parties by given party IDs
//        parties = partyRepository.findByIdInAndAccountIdAndElectionId(partyIds, accountId, electionId);
//    }
//
//
//    if (parties.isEmpty()) {
//        throw new ThedalException(ThedalError.PARTY_NOT_FOUND, HttpStatus.NOT_FOUND);
//    }
//	for (Party party : parties) {
//		boolean hasLinkedVoters = voterRepository.existsByPartyId(party.getId());
//		if (hasLinkedVoters) {
//			throw new ThedalException(ThedalError.PARTY_LINKED_TO_VOTER, HttpStatus.CONFLICT, 
//    "Party '" + party.getPartyName() + "' (ID: " + party.getId() + ") cannot be deleted because it is associated with one or more voters.");
//		}
//	}
//	 // Check if the electionId exists
////	    Party party = partyRepository.findByIdAndAccountIdAndElectionId(partyId, accountId, electionId)
////	            .orElseThrow(() -> {
////	                // Check if electionId or partyId is missing
////	                Party tempParty = partyRepository.findByIdAndAccountId(partyId, accountId)
////	                        .orElse(null);
////
////	                if (tempParty == null) {
////	                    throw new ThedalException(ThedalError.PARTY_NOT_FOUND, HttpStatus.NOT_FOUND);
////	                } else if (!tempParty.getElectionId().equals(electionId)) {
////	                    throw new ThedalException(ThedalError.ELECTION_NOT_FOUND, HttpStatus.NOT_FOUND);
////	                }
////
////	                return new ThedalException(ThedalError.PARTY_NOT_FOUND, HttpStatus.NOT_FOUND);
////	            });
//	   // Delete images from AWS S3 if they exist
//	   for (Party party : parties) {
//        if (party.getPartyImage() != null && !party.getPartyImage().isEmpty()) {
//            String objectKey = AwsFileUpload.getKeyFromUrl(party.getPartyImage());
//            if (objectKey != null && !objectKey.isEmpty()) {
//                awsFileUpload.deleteS3Object(s3bucket, objectKey);
//                log.info("Deleted image for party ID: {} from AWS S3.", party.getId());
//            }
//        }
//    }
//
//	    // Delete the party from the repository
//	    partyRepository.deleteAll(parties);
//    partyRepository.flush();
//
//    log.info("Parties with IDs {} for account ID {} and election ID {} have been deleted.", partyIds, accountId, electionId);
//
//    return new ThedalResponse<>(ThedalSuccess.PARTY_DELETED);
//	}
	@Transactional
    public ThedalResponse<Void> deleteParty(List<Long> partyIds, Long electionId) {
        log.info("Inside deleteParty method: party IDs: {} for election ID: {}", partyIds, electionId);
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        List<Party> parties;
        if (partyIds == null || partyIds.isEmpty()) {
            parties = partyRepository.findByAccountIdAndElectionId(accountId, electionId);
            log.info("No specific party IDs provided. Fetching all parties for deletion.");
        } else {
            parties = partyRepository.findByIdInAndAccountIdAndElectionId(partyIds, accountId, electionId);
        }

        if (parties.isEmpty()) {
            throw new ThedalException(ThedalError.PARTY_NOT_FOUND, HttpStatus.NOT_FOUND);
        }

        for (Party party : parties) {
            boolean hasLinkedVoters = voterRepository.existsByPartyId(party.getId());
            if (hasLinkedVoters) {
                throw new ThedalException(ThedalError.PARTY_LINKED_TO_VOTER, HttpStatus.CONFLICT,
                        "Party '" + party.getPartyName() + "' (ID: " + party.getId() + ") cannot be deleted because it is associated with one or more voters.");
            }
        }

        for (Party party : parties) {
            if (party.getPartyImage() != null && !party.getPartyImage().isEmpty()) {
                String objectKey = AwsFileUpload.getKeyFromUrl(party.getPartyImage());
                if (objectKey != null && !objectKey.isEmpty()) {
                    awsFileUpload.deleteS3Object(s3bucket, objectKey);
                    log.info("Deleted image for party ID: {} from AWS S3.", party.getId());
                }
            }
        }

        try {
            partyRepository.deleteAll(parties);
            partyRepository.flush();
            try {
                if (partyIds == null || partyIds.isEmpty()) {
                    partyMongoRepository.deleteByAccountIdAndElectionId(accountId, electionId);
                    log.info("Deleted all parties from MongoDB for accountId: {}, electionId: {}", accountId, electionId);
                } else {
                    partyMongoRepository.deleteByIdIn(partyIds);
                    log.info("Deleted parties from MongoDB: ids={}", partyIds);
                }
            } catch (Exception mongoEx) {
                log.error("Failed to delete parties from MongoDB: ids={}", partyIds, mongoEx);
                throw new RuntimeException("MongoDB deletion failed, triggering rollback", mongoEx);
            }
            log.info("Parties with IDs {} for account ID {} and election ID {} have been deleted.", partyIds, accountId, electionId);
            return new ThedalResponse<>(ThedalSuccess.PARTY_DELETED);
        } catch (Exception ex) {
            log.error("Failed to delete parties: ids={}", partyIds, ex);
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to delete parties due to an unexpected error.");
        }
    }

//	@Transactional
//	public void updatePartyOrder(List<PartyReorderRequest> reorderRequests, Long accountId, Long electionId) {
//	    // Fetch all parties for the given election and account
//	    List<Party> parties = partyRepository.findByElectionIdAndAccountIdOrderByOrderIndexAsc(electionId, accountId);
//
//	    if (parties.isEmpty()) {
//	        log.error("No parties found for election ID {} and account ID {}", electionId, accountId);
//	        throw new ThedalException(ThedalError.PARTY_NOT_FOUND, HttpStatus.NOT_FOUND);
//	    }
//
//	    // Create a map of partyId -> newOrderIndex
//	    Map<Long, Integer> newOrderMap = reorderRequests.stream()
//	            .collect(Collectors.toMap(PartyReorderRequest::getPartyId, PartyReorderRequest::getNewOrderIndex));
//
//	    // Sort the reorderRequests by newOrderIndex to avoid conflicts
//	    reorderRequests.sort(Comparator.comparingInt(PartyReorderRequest::getNewOrderIndex));
//
//	    // Collect parties that are being reordered
//	    List<Party> reorderedParties = new ArrayList<>();
//	    List<Party> remainingParties = new ArrayList<>(parties);
//
//	    for (PartyReorderRequest request : reorderRequests) {
//	        Party party = parties.stream()
//	                .filter(p -> p.getId().equals(request.getPartyId()))
//	                .findFirst()
//	                .orElseThrow(() -> new ThedalException(ThedalError.PARTY_NOT_FOUND, HttpStatus.NOT_FOUND));
//	        
//	        reorderedParties.add(party);
//	        remainingParties.remove(party);
//	    }
//
//	    // Insert reordered parties at their new positions
//	    for (PartyReorderRequest request : reorderRequests) {
//	        Party party = reorderedParties.stream()
//	                .filter(p -> p.getId().equals(request.getPartyId()))
//	                .findFirst()
//	                .orElseThrow(() -> new ThedalException(ThedalError.PARTY_NOT_FOUND, HttpStatus.NOT_FOUND));
//
//	        // Ensure the new index is within bounds
//	        int newIndex = Math.min(request.getNewOrderIndex(), remainingParties.size());
//	        remainingParties.add(newIndex, party);
//	    }
//
//	    // Update `orderIndex` for all parties
//	    for (int i = 0; i < remainingParties.size(); i++) {
//	        remainingParties.get(i).setOrderIndex(i);
//	        log.info("Updated party order: {} -> {}", remainingParties.get(i).getPartyName(), i);
//	    }
//
//	    // Save updated order to DB
//	    partyRepository.saveAll(remainingParties);
//	    log.info("Party order updated successfully for electionId: {}", electionId);
//	}
	@Transactional
	public void updatePartyOrder(List<PartyReorderRequest> reorderRequests, Long accountId, Long electionId) {
	    // Validate input
	    if (reorderRequests == null || reorderRequests.isEmpty()) {
	        throw new ThedalException(ThedalError.INVALID_REQUEST, HttpStatus.BAD_REQUEST, 
	            "Reorder requests cannot be empty");
	    }

	    // Get all parties for this election in current order
	    List<Party> allParties = partyRepository.findByElectionIdAndAccountIdOrderByOrderIndexAsc(electionId, accountId);
	    if (allParties.isEmpty()) {
	        throw new ThedalException(ThedalError.PARTY_NOT_FOUND, HttpStatus.NOT_FOUND);
	    }

	    // Create map of partyId to party for quick lookup
	    Map<Long, Party> partyMap = allParties.stream()
	        .collect(Collectors.toMap(Party::getId, Function.identity()));

	    // Verify all requested parties exist
	    for (PartyReorderRequest request : reorderRequests) {
	        if (!partyMap.containsKey(request.getPartyId())) {
	            throw new ThedalException(ThedalError.PARTY_NOT_FOUND, HttpStatus.NOT_FOUND,
	                "Party with ID " + request.getPartyId() + " not found");
	        }
	    }

	    // Create a list to hold the new order
	    List<Party> newOrder = new ArrayList<>(allParties);

	    // Process each reorder request
	    for (PartyReorderRequest request : reorderRequests) {
	        Party partyToMove = partyMap.get(request.getPartyId());
	        
	        // Remove from current position
	        newOrder.remove(partyToMove);
	        
	        // Add to new position (clamp to valid range)
	        int newPosition = Math.min(Math.max(0, request.getNewOrderIndex()), newOrder.size());
	        newOrder.add(newPosition, partyToMove);
	    }

	    // Update order indices based on new positions
	    for (int i = 0; i < newOrder.size(); i++) {
	        newOrder.get(i).setOrderIndex(i);
	    }

	    // Save to PostgreSQL
	    List<Party> savedParties = partyRepository.saveAll(newOrder);
	    partyRepository.flush();

	    try {
	        // Sync to MongoDB
	        List<PartyMongo> mongoParties = savedParties.stream()
	            .map(PartyMongo::new)
	            .collect(Collectors.toList());
	        
	        partyMongoRepository.saveAll(mongoParties);
	        log.info("Updated party order in MongoDB for electionId: {}", electionId);
	    } catch (Exception mongoEx) {
	        log.error("Failed to update party order in MongoDB", mongoEx);
	        throw new ThedalException(ThedalError.MONGO_SYNC_FAILED, HttpStatus.INTERNAL_SERVER_ERROR,
	            "Party order updated but failed to sync with MongoDB");
	    }

	    log.info("Successfully updated party order for electionId: {}", electionId);
	}
	
	@Transactional
    public ThedalResponse<List<PartyResponseDTO>> getPartiesByElectionIdFromMongo(Long electionId) {
        Long accountId = requestDetails.getCurrentAccountId();
        log.debug("Fetching parties from PostgreSQL for electionId: {} and accountId: {}", electionId, accountId);

        if (accountId == null) {
            log.error("Account ID not found for the current user.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        // Read from PostgreSQL instead of MongoDB
        List<Party> parties = partyRepository.findByAccountIdAndElectionIdOrderByOrderIndexAsc(accountId, electionId);
        if (parties.isEmpty()) {
            log.warn("No parties found in PostgreSQL for electionId: {} and accountId: {}", electionId, accountId);
            throw new ThedalException(ThedalError.PARTY_NOT_FOUND, HttpStatus.NOT_FOUND);
        }

        List<PartyResponseDTO> partyDTOs = parties.stream().map(party -> new PartyResponseDTO(
            party.getId(),
            party.getPartyName() != null ? party.getPartyName() : "",
            party.getPartyShortName() != null ? party.getPartyShortName() : "",
            party.getPartyImage() != null ? party.getPartyImage() : "",
            party.getPartyColor() != null ? party.getPartyColor() : "",
            party.getAllianceName() != null ? party.getAllianceName() : "",
            party.getOrderIndex() != null ? party.getOrderIndex() : 0,
            party.getCreatedAt(),
            party.getUpdatedAt(),
            0L // Voter count not available
        )).collect(Collectors.toList());

        log.info("Successfully fetched {} parties from PostgreSQL for electionId: {}", partyDTOs.size(), electionId);
        return new ThedalResponse<>(ThedalSuccess.PARTIES_FETCHED, partyDTOs);
    }
	
}