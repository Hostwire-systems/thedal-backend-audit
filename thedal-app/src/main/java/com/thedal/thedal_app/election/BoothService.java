package com.thedal.thedal_app.election;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.thedal.thedal_app.account.AccountEntity;
import com.thedal.thedal_app.account.AccountRepository;
import com.thedal.thedal_app.awsfilestore.AwsFileUpload;
import com.thedal.thedal_app.election.dtos.BoothReorderRequest;
import com.thedal.thedal_app.election.dtos.BoothSlipPrintRequest;
import com.thedal.thedal_app.election.dtos.BoothSlipPrintResponse;
import com.thedal.thedal_app.election.dtos.ElectionBoothRequest;
import com.thedal.thedal_app.election.dtos.ElectionBoothResponse;
import com.thedal.thedal_app.files.Files;
import com.thedal.thedal_app.files.FilesRepository;
import com.thedal.thedal_app.files.HandlerType;
import com.thedal.thedal_app.general.RequestDetailsService;
import com.thedal.thedal_app.response.ThedalResponse;
import com.thedal.thedal_app.response.ThedalSuccess;
import com.thedal.thedal_app.settings.electionsettings.BoothVulnerabilityRepo;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;
import com.thedal.thedal_app.user.UserEntity;
import com.thedal.thedal_app.user.UserRepo;
import com.thedal.thedal_app.volunteer.VolunteerEntity;
import com.thedal.thedal_app.volunteer.VolunteerRepository;
import com.thedal.thedal_app.voter.BulkUploadStatus;
import com.thedal.thedal_app.voter.VoterEntity;
import com.thedal.thedal_app.voter.VoterRepo;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class BoothService {

    @Autowired
    private FilesRepository filesRepository;
    @Autowired
    private ElectionBoothRepository boothRepository;
    @Autowired
    private ElectionRepository electionRepository;
    @Autowired
    private BoothBulkUploadRepository boothBulkUploadRepository;
    @Autowired
    private BoothFileUploadService boothFileUploadService;
    @Autowired
    private AwsFileUpload awsFileUpload;   
    @Autowired
    private RequestDetailsService requestDetails;
    @Autowired
    private BoothVulnerabilityRepo boothVulnerabilityRepository;
    @Autowired
    private BoothSlipPrintRepository boothSlipPrintRepository;
    @Autowired
    private VoterRepo voterRepository;
    @Autowired
    private UserRepo userRepo;
    @Autowired
    private VolunteerRepository volunteerRepo;
    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    private AccountRepository accountRepository;

    @Value("${aws.s3.files.bucket}")
	private String s3Filesbucket;
    
    private void validateElectionOwnership(Long electionId, Long accountId) {
        Optional<ElectionEntity> electionOpt = electionRepository.findByIdAndAccountId(electionId, accountId);
        if (!electionOpt.isPresent()) {
            log.error("Election ID {} does not belong to Account ID {}", electionId, accountId);
            throw new ThedalException(ThedalError.INVALID_ELECTION, HttpStatus.FORBIDDEN);  
        }
    }   
    
    public ElectionBoothResponse createBooth(Long accountId, Long electionId, ElectionBoothRequest boothRequest) {
    	//requestDetails.getCurrentUserFromRequest();  	
    	if (accountId == null) {
            log.error("Account ID not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }
    	validateElectionOwnership(electionId, accountId);
    	// Check if the election exists
        ElectionEntity election = electionRepository.findById(electionId)
                .orElseThrow(() -> new ThedalException(ThedalError.ELECTION_NOT_FOUND, HttpStatus.NOT_FOUND));
    	
        // Check if the booth already exists
        Optional<ElectionBooth> existingBooth = boothRepository.findByElectionIdAndAccountIdAndBoothNumber(
                electionId, accountId, boothRequest.getBoothNumber());
        if (existingBooth.isPresent()) {
            log.warn("Duplicate booth creation attempted for election ID {}, account ID {}, booth number {}",
                    electionId, accountId, boothRequest.getBoothNumber());
            throw new ThedalException(ThedalError.BOOTH_ALREADY_EXISTS, HttpStatus.CONFLICT);
        }
        
     // Get the highest order index for booths under the same account
        Integer maxOrderIndex = boothRepository.findMaxOrderIndexByElectionId(electionId);
        int newOrderIndex = (maxOrderIndex != null) ? maxOrderIndex + 1 : 0;
        
    	try {
    	ElectionBooth booth = new ElectionBooth();
    	booth.setAccountId(accountId); 
        booth.setElection(new ElectionEntity(electionId));
        booth.setBoothNumber(boothRequest.getBoothNumber());
        booth.setBoothVulnerability(boothRequest.getBoothVulnerability());
        booth.setOrderIndex(newOrderIndex);

        ElectionBooth savedBooth = boothRepository.save(booth);

//        return new ElectionBoothResponse(
//                savedBooth.getElection().getId(),
//                savedBooth.getId(),
//                savedBooth.getBoothNumber(),
//                savedBooth.getAccountId(),
//                savedBooth.getBoothVulnerability()  
//        );
        return new ElectionBoothResponse(          
                savedBooth.getBoothNumber(),
                savedBooth.getBoothVulnerability(),
                savedBooth.getOrderIndex()
        );
     }catch(Exception e){
    	 log.error("Error occurred while creating booth for election ID {}: {}", electionId, e.getMessage());
         throw new ThedalException(ThedalError.BOOTH_SAVE_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
     }
}
    
       
//    public Page<ElectionBoothResponse> findAllByElectionIdAndAccountId(Long electionId, Long accountId, Pageable pageable) {
//        try {
//            Long userId = requestDetails.getCurrentUserId(); // Get current user ID
//            log.debug("Fetching election booths for userId: {}", userId);
//
//            UserEntity currentUser = userRepo.findById(userId)
//                    .orElseThrow(() -> new RuntimeException("User not found"));
//            Role userRole = currentUser.getRole();
//            log.debug("Fetching election booths for accountId: {}, userRole: {}", accountId, userRole);
//
//            Page<ElectionBooth> booths;
//
//            if ("VOLUNTEER".equalsIgnoreCase(userRole.getRoleName())) {
//                VolunteerEntity volunteer = volunteerRepo.findByUserEntity_Id(userId)
//                        .orElseThrow(() -> new ThedalException(ThedalError.VOLUNTEER_NOT_FOUND, HttpStatus.NOT_FOUND));
//
//                List<Integer> assignedBooths = volunteer.getAssignedBooth()
//                        .stream()
//                        .map(Long::intValue)
//                        .collect(Collectors.toList());
//
//                if (assignedBooths.isEmpty()) {
//                    log.error("Volunteer {} has no assigned booths", userId);
//                    throw new ThedalException(ThedalError.BOOTH_NOT_FOUND, HttpStatus.NOT_FOUND);
//                }
//
//                log.debug("Volunteer {} assigned booths: {}", userId, assignedBooths);
//
//                booths = boothRepository.findByElectionIdAndAccountIdAndBoothNumberIn(electionId, accountId, assignedBooths, pageable);
//
//            } else {
////                booths = boothRepository.findByElectionIdAndAccountId(electionId, accountId, pageable);
//            	booths = boothRepository.findByElectionIdAndAccountIdOrderByOrderIndexAsc(electionId, accountId, pageable);
//
//            }
//
//            if (booths.isEmpty()) {
//                log.error("No booths found for election ID {} and account ID {}", electionId, accountId);
//                throw new ThedalException(ThedalError.BOOTH_NOT_FOUND, HttpStatus.NOT_FOUND);
//            }
//
////            return booths.map(booth -> new ElectionBoothResponse(
////                    booth.getBoothNumber(),
////                    booth.getBoothVulnerability(),
////                    booth.getOrderIndex()
////            ));
//            // Filter out empty booths before mapping to response
//            List<ElectionBoothResponse> filteredResponses = booths.stream()
//                    .filter(booth -> booth.getBoothNumber() != null) // Ensures booth number is not null
//                    .map(booth -> new ElectionBoothResponse(
//                            booth.getBoothNumber(),
//                            booth.getBoothVulnerability(),
//                            booth.getOrderIndex()
//                    ))
//                    .collect(Collectors.toList());
//
//            if (filteredResponses.isEmpty()) {
//                log.error("All booths are empty for election ID {} and account ID {}", electionId, accountId);
//                throw new ThedalException(ThedalError.BOOTH_NOT_FOUND, HttpStatus.NOT_FOUND);
//            }
//
//            return new PageImpl<>(filteredResponses, pageable, filteredResponses.size());
//
//        } catch (ThedalException e) {
//            throw e;
//        } catch (Exception e) {
//            log.error("Error occurred while fetching booths for election ID {}: {}", electionId, e.getMessage());
//            throw new ThedalException(ThedalError.BOOTH_FETCH_FAILED, HttpStatus.NO_CONTENT);
//        }
//    }
    
    public Page<ElectionBoothResponse> findAllByElectionIdAndAccountId(Long electionId, Long accountId, Pageable pageable) {
        try {
            Long userId = requestDetails.getCurrentUserId();
            validateElectionOwnership(electionId, accountId);
            log.debug("Fetching election booths for userId: {}", userId);

            UserEntity currentUser = userRepo.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            log.debug("Fetching election booths for accountId: {}", accountId);

            Page<ElectionBooth> booths;

            Optional<VolunteerEntity> volunteerOpt = volunteerRepo.findByUserEntity_Id(userId);
            if (volunteerOpt.isPresent()) {
                VolunteerEntity volunteer = volunteerOpt.get();

                if (!volunteer.getElectionEntity().getId().equals(electionId)) {
                    log.warn("Volunteer {} is not associated with electionId: {}", userId, electionId);
                    throw new ThedalException(ThedalError.UNAUTHORIZED_ELECTION_ACCESS, HttpStatus.FORBIDDEN);
                }

                List<Integer> assignedBooths = volunteer.getAssignedBooth()
                        .stream()
                        .map(Long::intValue)
                        .collect(Collectors.toList());

                if (assignedBooths.isEmpty()) {
                    log.warn("Volunteer {} has no assigned booths for electionId: {}", userId, electionId);
                    throw new ThedalException(ThedalError.BOOTH_NOT_FOUND, HttpStatus.NOT_FOUND);
                }

                log.debug("Volunteer {} assigned booths: {}", userId, assignedBooths);

                booths = boothRepository.findByElectionIdAndAccountIdAndBoothNumberInWithVoters(
                        electionId, accountId, assignedBooths, pageable);
            } else {
                booths = boothRepository.findByElectionIdAndAccountIdWithVoters(
                        electionId, accountId, pageable);
            }

            if (booths.isEmpty()) {
                log.warn("No booths with voters found for election ID {} and account ID {}", electionId, accountId);
                throw new ThedalException(ThedalError.BOOTH_NOT_FOUND, HttpStatus.NOT_FOUND);
            }

            List<ElectionBoothResponse> filteredResponses = booths.stream()
                    .map(booth -> new ElectionBoothResponse(
                            booth.getBoothNumber(),
                            booth.getBoothVulnerability(),
                            booth.getOrderIndex()
                    ))
                    .collect(Collectors.toList());

            return new PageImpl<>(filteredResponses, pageable, booths.getTotalElements());

        } catch (ThedalException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error occurred while fetching booths for election ID {}: {}", electionId, e.getMessage());
            throw new ThedalException(ThedalError.BOOTH_FETCH_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
//    public Page<ElectionBoothResponse> findAllByElectionIdAndAccountId(Long electionId, Long accountId, Pageable pageable) {
//        try {
//            Long userId = requestDetails.getCurrentUserId();
//            validateElectionOwnership(electionId, accountId);
//            log.debug("Fetching election booths for electionId: {}, accountId: {}, userId: {}", electionId, accountId, userId);
//
//            UserEntity currentUser = userRepo.findById(userId)
//                    .orElseThrow(() -> new ThedalException(ThedalError.USER_NOT_FOUND, HttpStatus.NOT_FOUND));
//            log.debug("Current user: {}", currentUser);
//
//            Page<ElectionBooth> booths;
//
//            Optional<VolunteerEntity> volunteerOpt = volunteerRepo.findByUserEntity_Id(userId);
//            if (volunteerOpt.isPresent()) {
//                VolunteerEntity volunteer = volunteerOpt.get();
//
//                if (!volunteer.getElectionEntity().getId().equals(electionId)) {
//                    log.warn("Volunteer {} is not associated with electionId: {}", userId, electionId);
//                    throw new ThedalException(ThedalError.UNAUTHORIZED_ELECTION_ACCESS, HttpStatus.FORBIDDEN);
//                }
//
//                List<Integer> assignedBooths = volunteer.getAssignedBooth()
//                        .stream()
//                        .map(Long::intValue)
//                        .collect(Collectors.toList());
//
//                log.debug("Volunteer {} assigned booths: {}", userId, assignedBooths);
//
//                if (assignedBooths.isEmpty()) {
//                    log.info("Volunteer {} has no assigned booths for electionId: {}. Returning empty page.", userId, electionId);
//                    return new PageImpl<>(Collections.emptyList(), pageable, 0);
//                }
//
//                booths = boothRepository.findByElectionIdAndAccountIdAndBoothNumberIn(electionId, accountId, assignedBooths, pageable);
//            } else {
//                log.debug("User {} is not a volunteer. Fetching all booths for electionId: {}, accountId: {}", userId, electionId, accountId);
//                booths = boothRepository.findByElectionIdAndAccountId(electionId, accountId, pageable);
//            }
//
//            log.debug("Found {} booths for electionId: {}, accountId: {}", booths.getTotalElements(), electionId, accountId);
//
//            List<ElectionBoothResponse> responses = booths.stream()
//                    .map(booth -> new ElectionBoothResponse(
//                            booth.getBoothNumber(),
//                            booth.getBoothVulnerability(),
//                            booth.getOrderIndex()
//                    ))
//                    .collect(Collectors.toList());
//
//            return new PageImpl<>(responses, pageable, booths.getTotalElements());
//
//        } catch (ThedalException e) {
//            throw e;
//        } catch (Exception e) {
//            log.error("Error fetching booths for electionId: {}, accountId: {}: {}", electionId, accountId, e.getMessage(), e);
//            throw new ThedalException(ThedalError.BOOTH_FETCH_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//    }



    public ElectionBoothResponse findByElectionIdAndBoothNumberAndAccountId(Long electionId, Integer boothNumber, Long accountId) {
        try {
            Long userId = requestDetails.getCurrentUserId();
            validateElectionOwnership(electionId, accountId);
            log.debug("Fetching booth {} for electionId: {}, accountId: {}", boothNumber, electionId, accountId);

            UserEntity currentUser = userRepo.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            ElectionBooth booth;
            Optional<VolunteerEntity> volunteerOpt = volunteerRepo.findByUserEntity_Id(userId);

            if (volunteerOpt.isPresent()) {
                VolunteerEntity volunteer = volunteerOpt.get();

                if (!volunteer.getElectionEntity().getId().equals(electionId)) {
                    log.warn("Volunteer {} is not associated with electionId: {}", userId, electionId);
                    throw new ThedalException(ThedalError.UNAUTHORIZED_ELECTION_ACCESS, HttpStatus.FORBIDDEN);
                }

                List<Integer> assignedBooths = volunteer.getAssignedBooth()
                        .stream()
                        .map(Long::intValue)
                        .collect(Collectors.toList());

                if (!assignedBooths.contains(boothNumber)) {
                    log.warn("Volunteer {} is not assigned to booth {} for electionId: {}", userId, boothNumber, electionId);
                    throw new ThedalException(ThedalError.BOOTH_NOT_FOUND, HttpStatus.NOT_FOUND);
                }

                booth = boothRepository.findByElectionIdAndBoothNumberAndAccountIdWithVoters(electionId, accountId, boothNumber)
                        .orElseThrow(() -> new ThedalException(ThedalError.BOOTH_NOT_FOUND_FOR_ELECTION, HttpStatus.NOT_FOUND));
            } else {
                booth = boothRepository.findByElectionIdAndBoothNumberAndAccountIdWithVoters(electionId, accountId, boothNumber)
                        .orElseThrow(() -> new ThedalException(ThedalError.BOOTH_NOT_FOUND_FOR_ELECTION, HttpStatus.NOT_FOUND));
            }

            return new ElectionBoothResponse(
                    booth.getBoothNumber(),
                    booth.getBoothVulnerability(),
                    booth.getOrderIndex()
            );

        } catch (ThedalException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error occurred while fetching booth with number {} for election ID {}: {}", boothNumber, electionId, e.getMessage());
            throw new ThedalException(ThedalError.BOOTH_FETCH_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

//    public ElectionBoothResponse findByElectionIdAndBoothNumberAndAccountId(Long electionId, Integer boothNumber, Long accountId) {
//        try {
//            Long userId = requestDetails.getCurrentUserId();
//            validateElectionOwnership(electionId, accountId);
//            log.debug("Fetching booth {} for electionId: {}, accountId: {}, userId: {}", boothNumber, electionId, accountId, userId);
//
//            UserEntity currentUser = userRepo.findById(userId)
//                    .orElseThrow(() -> new ThedalException(ThedalError.USER_NOT_FOUND, HttpStatus.NOT_FOUND));
//
//            ElectionBooth booth;
//            Optional<VolunteerEntity> volunteerOpt = volunteerRepo.findByUserEntity_Id(userId);
//
//            if (volunteerOpt.isPresent()) {
//                VolunteerEntity volunteer = volunteerOpt.get();
//
//                if (!volunteer.getElectionEntity().getId().equals(electionId)) {
//                    log.warn("Volunteer {} is not associated with electionId: {}", userId, electionId);
//                    throw new ThedalException(ThedalError.UNAUTHORIZED_ELECTION_ACCESS, HttpStatus.FORBIDDEN);
//                }
//
//                List<Integer> assignedBooths = volunteer.getAssignedBooth()
//                        .stream()
//                        .map(Long::intValue)
//                        .collect(Collectors.toList());
//
//                if (!assignedBooths.contains(boothNumber)) {
//                    log.warn("Volunteer {} is not assigned to booth {} for electionId: {}", userId, boothNumber, electionId);
//                    throw new ThedalException(ThedalError.BOOTH_NOT_FOUND, HttpStatus.NOT_FOUND);
//                }
//
//                booth = boothRepository.findByElectionIdAndAccountIdAndBoothNumber(electionId, accountId, boothNumber)
//                        .orElseThrow(() -> new ThedalException(ThedalError.BOOTH_NOT_FOUND_FOR_ELECTION, HttpStatus.NOT_FOUND));
//            } else {
//                booth = boothRepository.findByElectionIdAndAccountIdAndBoothNumber(electionId, accountId, boothNumber)
//                        .orElseThrow(() -> new ThedalException(ThedalError.BOOTH_NOT_FOUND_FOR_ELECTION, HttpStatus.NOT_FOUND));
//            }
//
//            log.debug("Found booth: {}", booth);
//
//            return new ElectionBoothResponse(
//                    booth.getBoothNumber(),
//                    booth.getBoothVulnerability(),
//                    booth.getOrderIndex()
//            );
//
//        } catch (ThedalException e) {
//            throw e;
//        } catch (Exception e) {
//            log.error("Error fetching booth {} for electionId: {}, accountId: {}: {}", boothNumber, electionId, accountId, e.getMessage(), e);
//            throw new ThedalException(ThedalError.BOOTH_FETCH_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//    }

    public void deleteBoothByElectionIdAndBoothNumberAndAccountId(Long electionId, Integer boothNumber, Long accountId) {
    	
    	try {
            ElectionBooth booth = boothRepository.findByElectionIdAndBoothNumberAndAccountId(electionId, boothNumber, accountId)
                    .orElseThrow(() -> new ThedalException(ThedalError.BOOTH_NOT_FOUND, HttpStatus.NOT_FOUND));

            boothRepository.delete(booth);
        } catch (Exception e) {
            log.error("Error occurred while deleting booth with election ID {} and booth number {} for account ID {}: {}",
                    electionId, boothNumber, accountId, e.getMessage());
            throw new ThedalException(ThedalError.BOOTH_DELETION_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
  
    public ElectionBoothResponse updateBoothByElectionIdAndBoothNumber(Long electionId, Integer boothNumber, ElectionBoothRequest boothRequest, Long accountId) {
    	
    	ElectionBooth booth = boothRepository.findByElectionIdAndBoothNumberAndAccountId(electionId, boothNumber, accountId)
                .orElseThrow(() -> new ThedalException(ThedalError.BOOTH_NOT_FOUND, HttpStatus.NOT_FOUND));

        //booth.setBoothNumber(boothRequest.getBoothNumber());
        if (boothRequest.getBoothNumber() != null) {
            booth.setBoothNumber(boothRequest.getBoothNumber());
        }
        
        if (boothRequest.getBoothVulnerability() != null) {
            booth.setBoothVulnerability(boothRequest.getBoothVulnerability());
        }

        ElectionBooth updatedBooth = boothRepository.save(booth);

        return new ElectionBoothResponse(
                updatedBooth.getBoothNumber(),
                updatedBooth.getBoothVulnerability(),
                updatedBooth.getOrderIndex()
        );
    }

    
    
    public ThedalResponse<BoothSlipPrintResponse> printBoothSlip(Long electionId, BoothSlipPrintRequest request) {
    	
    	Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account ID is missing in the request header.");
            throw new ThedalException(ThedalError.UNAUTHORIZED_ACCESS, HttpStatus.UNAUTHORIZED);
        }
        
//        // Validate input values
//        if (accountId == null || electionId == null || volunteerId == null) {
//            log.error("Account ID, Election ID, or Volunteer ID missing, unauthorized access.");
//            throw new ThedalException(ThedalError.UNAUTHORIZED_ACCESS, HttpStatus.UNAUTHORIZED);
//        }

        // Find the voter (simulate this for now)
        //VoterEntity voter = voterRepository.findByVoterIdAndAccountId(request.getVoterId(), accountId)
        VoterEntity voter = voterRepository.findByAccountIdAndElectionIdAndVoterId(accountId, electionId, request.getVoterId())
                .orElseThrow(() -> new ThedalException(ThedalError.VOTER_NOT_FOUND, HttpStatus.NOT_FOUND));

        try {
            BoothSlipPrint boothSlipPrint = new BoothSlipPrint();
            boothSlipPrint.setAccountId(accountId);
            boothSlipPrint.setElectionId(electionId);
            //boothSlipPrint.setVolunteerId(volunteerId);
            boothSlipPrint.setVoterId(voter.getVoterId());
            boothSlipPrint.setPrintedTime(LocalDateTime.now());
            boothSlipPrint.setTemplateId(request.getTemplateId());

            BoothSlipPrint savedBoothSlip = boothSlipPrintRepository.save(boothSlipPrint);

            BoothSlipPrintResponse response = new BoothSlipPrintResponse(
                    savedBoothSlip.getVoterId(),
                    savedBoothSlip.getPrintedTime(),
                    savedBoothSlip.getTemplateId()
            );

            return new ThedalResponse<>(ThedalSuccess.BOOTH_SLIP_PRINTED, response);
        } catch (Exception e) {
            log.error("Error occurred while printing booth slip: {}", e.getMessage());
            throw new ThedalException(ThedalError.BOOTH_SLIP_PRINT_FAILED, HttpStatus.BAD_REQUEST);
        }
    }
    
    public ThedalResponse<List<BoothSlipPrintResponse>> getBoothSlip(Long electionId, String voterId) {
    	
    	Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account ID is missing in the request header.");
            throw new ThedalException(ThedalError.UNAUTHORIZED_ACCESS, HttpStatus.UNAUTHORIZED);
        }

        List<BoothSlipPrint> boothSlipPrints = boothSlipPrintRepository
                .findByElectionIdAndVoterIdAndAccountId(electionId, voterId, accountId);

        if (boothSlipPrints.isEmpty()) {
            log.error("No booth slips found for Election ID: {} and Voter ID: {}", electionId, voterId);
            throw new ThedalException(ThedalError.BOOTH_SLIP_NOT_FOUND, HttpStatus.NOT_FOUND);
        }
        
        List<BoothSlipPrintResponse> responses = boothSlipPrints.stream()
                .map(boothSlipPrint -> new BoothSlipPrintResponse(
                        boothSlipPrint.getVoterId(),
                        boothSlipPrint.getPrintedTime(),
                        boothSlipPrint.getTemplateId()
                ))
                .toList();

        return new ThedalResponse<>(ThedalSuccess.BOOTH_SLIPS_RETRIEVED, responses);
    }

//    @Transactional
//    public void reorderBooths(Long electionId, Long accountId, List<BoothReorderRequest> reorderRequests) {
//        List<ElectionBooth> booths = boothRepository.findByElectionIdAndAccountIdOrderByOrderIndex(electionId, accountId);
//
//        if (booths.isEmpty()) {
//            log.error("No booths found for election ID {} and account ID {}", electionId, accountId);
//            throw new ThedalException(ThedalError.BOOTH_NOT_FOUND, HttpStatus.NOT_FOUND);
//        }
//
//        // Create a map of boothId to new orderIndex
//        Map<Long, Integer> newOrderMap = reorderRequests.stream()
//                .collect(Collectors.toMap(BoothReorderRequest::getBoothId, BoothReorderRequest::getNewOrderIndex));
//
//        // Update orderIndex for each booth
//        for (ElectionBooth booth : booths) {
//            if (newOrderMap.containsKey(booth.getId())) {
//                booth.setOrderIndex(newOrderMap.get(booth.getId()));
//            }
//        }
//
//        // Save updated booths
//        boothRepository.saveAll(booths);
//    }
    @Transactional
    public void updateBoothOrder(List<BoothReorderRequest> reorderRequests, Long accountId, Long electionId) {
        List<ElectionBooth> booths = boothRepository.findByElectionIdAndAccountIdOrderByOrderIndex(electionId, accountId);

        if (booths.isEmpty()) {
            log.error("No booths found for election ID {} and account ID {}", electionId, accountId);
            throw new ThedalException(ThedalError.BOOTH_NOT_FOUND, HttpStatus.NOT_FOUND);
        }

        // Sort booths by orderIndex before modifying
        booths.sort(Comparator.comparing(ElectionBooth::getOrderIndex));

        // Create a map of boothNumber -> newOrderIndex
        Map<Integer, Integer> newOrderMap = reorderRequests.stream()
                .collect(Collectors.toMap(BoothReorderRequest::getBoothNumber, BoothReorderRequest::getNewOrderIndex));

        // Remove booths that are being moved
        List<ElectionBooth> reorderedBooths = new ArrayList<>(booths);
        reorderedBooths.removeIf(b -> newOrderMap.containsKey(b.getBoothNumber()));

        // Insert booths at their new positions
        for (BoothReorderRequest request : reorderRequests) {
            ElectionBooth booth = booths.stream()
                    .filter(b -> b.getBoothNumber().equals(request.getBoothNumber()))
                    .findFirst()
                    .orElseThrow(() -> new ThedalException(ThedalError.BOOTH_NOT_FOUND, HttpStatus.NOT_FOUND));

            reorderedBooths.add(request.getNewOrderIndex(), booth);
        }

        // Update orderIndex for all booths
        for (int i = 0; i < reorderedBooths.size(); i++) {
            reorderedBooths.get(i).setOrderIndex(i);
        }

        // Save the updated order
        boothRepository.saveAll(reorderedBooths);
        log.info("Booth order updated successfully for electionId: {}", electionId);
    }

@Transactional
public ThedalResponse<Void> uploadBoothFromXlsxOrCsv(MultipartFile file, Long electionId) {

    //requestDetails.checkUserRolePermission(RolePermission.CADRE_MANAGEMENT);
    
    ElectionEntity election = electionRepository.findById(electionId)
            .orElseThrow(() -> new ThedalException(ThedalError.ELECTION_NOT_FOUND, HttpStatus.NOT_FOUND));

    long startTime = System.currentTimeMillis();
    Long accountId = requestDetails.getCurrentAccountId();

    if (accountId == null) {
        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
    }

    // Fetch the account entity
    AccountEntity accountEntity = accountRepository.findById(accountId)
            .orElseThrow(() -> new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.BAD_REQUEST));

    if (!isSupportedFormat(file.getOriginalFilename()) || file.isEmpty()) {
        throw new ThedalException(ThedalError.INVALID_FILE_FORMAT, HttpStatus.BAD_REQUEST);
    }

    String folder = "booth_uploads";
    String uniqueId = UUID.randomUUID().toString().substring(0, 8);
    String originalFileName = file.getOriginalFilename();
    String fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
    String uniqueFileName = folder + "/booth_" + System.currentTimeMillis() + "_" + uniqueId + fileExtension;

    String fileUrl = null;
    Long bulkUploadId = null;

    try {
        fileUrl = awsFileUpload.uploadMultipartFile(file, uniqueFileName, s3Filesbucket);
        log.info("File uploaded to S3 at: {}", fileUrl);

        // Create and save the bulk upload entry
        BoothBulkUploadEntity boothBulkUploadEntity = new BoothBulkUploadEntity();
        boothBulkUploadEntity.setAccountId(accountId);
        boothBulkUploadEntity.setElectionId(electionId);
        boothBulkUploadEntity.setStartTime(LocalDateTime.now());
        boothBulkUploadEntity.setStatus(BulkUploadStatus.IN_PROGRESS);
        boothBulkUploadRepository.save(boothBulkUploadEntity);

        bulkUploadId = boothBulkUploadEntity.getId();

        // Save file metadata
        Files fileEntity = new Files(HandlerType.BULKUPLOAD_FILES, boothBulkUploadEntity.getId(), originalFileName, fileUrl);
        filesRepository.save(fileEntity);

        // Process the file asynchronously
        if (fileExtension.equalsIgnoreCase(".xlsx")) {
            boothFileUploadService.processBoothExcelFileAsync(bulkUploadId, accountEntity, fileUrl, election, boothBulkUploadEntity);
        } else if (fileExtension.equalsIgnoreCase(".csv")) {
            boothFileUploadService.processBoothCsvFileAsync(bulkUploadId, accountEntity, fileUrl, election, boothBulkUploadEntity);
        }

        boothBulkUploadEntity.setStatus(BulkUploadStatus.COMPLETED);
        boothBulkUploadEntity.setEndTime(LocalDateTime.now());
        boothBulkUploadRepository.save(boothBulkUploadEntity);

    } catch (IOException e) {
        log.error("Error uploading file to S3", e);
        throw new ThedalException(ThedalError.FILE_PROCESSING_FAILED, HttpStatus.INTERNAL_SERVER_ERROR, "File upload to S3 failed.");
    } finally {
        long endTime = System.currentTimeMillis();
        log.info("Time taken to process file: {} ms", (endTime - startTime));
    }

    return new ThedalResponse<>(ThedalSuccess.BULK_BOOTHS_UPLOADED);
}

private boolean isSupportedFormat(String originalFileName) {
    return originalFileName != null && (originalFileName.endsWith(".xlsx") || originalFileName.endsWith(".csv"));
}

 


    
}