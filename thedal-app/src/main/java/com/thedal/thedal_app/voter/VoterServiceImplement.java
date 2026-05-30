//package com.thedal.thedal_app.voter;
//
//import java.io.IOException;
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.time.Period;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Optional;
//import java.util.Set;
//import java.util.UUID;
//import java.util.stream.Collectors;
//
//import org.springframework.beans.BeanUtils;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.dao.DataIntegrityViolationException;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.domain.Sort;
//import org.springframework.http.HttpStatus;
//import org.springframework.stereotype.Service;
//import org.springframework.web.client.RestTemplate;
//import org.springframework.web.multipart.MultipartFile;
//
//import com.thedal.thedal_app.awsfilestore.AwsFileUpload;
//import com.thedal.thedal_app.election.ElectionBooth;
//import com.thedal.thedal_app.election.ElectionBoothRepository;
//import com.thedal.thedal_app.election.ElectionEntity;
//import com.thedal.thedal_app.election.ElectionRepository;
//import com.thedal.thedal_app.files.Files;
//import com.thedal.thedal_app.files.FilesRepository;
//import com.thedal.thedal_app.files.HandlerType;
//import com.thedal.thedal_app.general.RequestDetailsService;
//import com.thedal.thedal_app.quartz.JobSchedulerService;
//import com.thedal.thedal_app.report.ReportService;
//import com.thedal.thedal_app.report.dto.ElectionOverviewDTO;
//import com.thedal.thedal_app.report.dto.VotersHavingContactsDTO;
//import com.thedal.thedal_app.response.ThedalResponse;
//import com.thedal.thedal_app.response.ThedalSuccess;
//import com.thedal.thedal_app.role.RolePermission;
//import com.thedal.thedal_app.settings.electionsettings.Caste;
//import com.thedal.thedal_app.settings.electionsettings.CasteMongorepository;
//import com.thedal.thedal_app.settings.electionsettings.CasteRepository;
//import com.thedal.thedal_app.settings.electionsettings.Complain;
//import com.thedal.thedal_app.settings.electionsettings.ComplainMongoRepository;
//import com.thedal.thedal_app.settings.electionsettings.Religion;
//import com.thedal.thedal_app.settings.electionsettings.ReligionMongoRepository;
//import com.thedal.thedal_app.settings.electionsettings.SubCaste;
//import com.thedal.thedal_app.settings.electionsettings.SubCasteRepo;
//import com.thedal.thedal_app.settings.electionsettings.SubCasteRepository;
//import com.thedal.thedal_app.sqs.SqsConsumerService;
////import com.thedal.thedal_app.sqs.SqsService;
//import com.thedal.thedal_app.thedal_exception.ThedalError;
//import com.thedal.thedal_app.thedal_exception.ThedalException;
//import com.thedal.thedal_app.volunteer.VolunteerEntity;
//import com.thedal.thedal_app.volunteer.VolunteerRepository;
//import com.thedal.thedal_app.voter.dto.BulkUploadDto;
//import com.thedal.thedal_app.voter.dto.BulkUploadResponse;
//import com.thedal.thedal_app.voter.dto.BulkUploadStatusDto;
//import com.thedal.thedal_app.voter.dto.VoterDTO;
//import com.thedal.thedal_app.voter.dto.VoterLocationDTO;
//import com.thedal.thedal_app.voter.dto.VoterUpdateDTO;
//import com.thedal.thedal_app.voter.dto.VoterVoteRequest;
////import com.amazonaws.services.sqs.model.SendMessageRequest;
//
//import jakarta.persistence.EntityManager;
//import jakarta.persistence.PersistenceContext;
//import jakarta.transaction.Transactional;
//import lombok.extern.slf4j.Slf4j;
//
//
//@Slf4j
//@Service
//public class VoterServiceImplement implements VoterMongoService {
//
//    @Autowired
//    private VoterRepo voterRepository;
//    
//    @Autowired
//    private RequestDetailsService requestDetails;
//    
//    @Autowired
//    private VoterFileUploadService voterFileUploadService;
//    
//    @Autowired
//    private BulkUploadRepo bulkUploadRepo;
//    @Autowired
//    private FilesRepository filesRepo;
//    
//    @Autowired
//    private FilesRepository filesRepository;
//    
//    @Autowired
//    private JobSchedulerService jobSchedulerService;
//
//	@Autowired 
//	private ComplainMongoRepository complainMongoRepository;
//    
//    @Autowired
//    private AwsFileUpload awsFileUpload;
//    @Value("${aws.s3.files.bucket}")
//	private String s3bucket;
//    
//    @Autowired
//    private ElectionBoothRepository electionBoothRepository;
//    @Autowired
//    private ElectionRepository electionRepository;
//    @Autowired
//    private ElectionBoothService electionBoothService;
//    @PersistenceContext
//    private EntityManager entityManager;
//    @Autowired
//    private ReligionMongoRepository religionMongoRepository;
//    @Autowired
//    private CasteRepository casteRepository;
//    @Autowired
//    private SubCasteRepository subCasteRepository;
////    @Autowired
////    private DynamicFieldRepository dynamicFieldRepository;
////    @Autowired
////    private VoterDynamicFieldRepository voterDynamicFieldRepository;
//    @Autowired
//    private DynamicFieldMappingRepository dynamicFieldMappingRepository;
//    @Autowired
//    private VoterMongoRepo voterMongoRepo;
//    @Autowired 
//    private CasteMongorepository casteMongorepository ;
//    @Autowired 
//    private SubCasteRepo subCasteRepo;	
//
//	@Autowired
//	private SqsConsumerService  sqsService;
//
//	@Value("${lambda.enabled}")
//    private boolean isLambdaEnabled;
//
//	@Autowired
//    private VoterService voterPostgresService;
//
//    @Autowired
//    private ReportService reportService;
//	
//    @Autowired
//    private VolunteerRepository volunteerRepository;
//
//	
////   private final String queueUrl = "https://sqs.us-east-1.amazonaws.com/123456789012/voter-queue"; 
//
////   public VoterServiceImplement(SqsClient sqsClient) {
//// 	this.sqsClient = sqsClient;
//// }
//    
////    static {
////        // Increase the maximum array size to avoid the error
////        IOUtils.setByteArrayMaxOverride(500_000_000); // Set it to a higher value, e.g., 500MB
////    }
//    
//    /**
//	 * Saves a new voter to the repository.
//	 *
//	 * @param voter the voter entity containing voter information to be saved.
//	 * @return a ThedalResponse indicating the success of the operation.
//	 * @throws ThedalException if saving the voter fails.
//	 */
// 
//    @Transactional
//    @Override
//    public ThedalResponse<VoterDTO> saveVoter(VoterDTO voterDto) throws DataIntegrityViolationException {
//        Long accountId = requestDetails.getCurrentAccountId();
//        if (accountId == null) {
//            log.error("Account ID not found, unauthorized access.");
//            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
//        }
//
//        if (voterDto.getVoterId() == null || voterDto.getElectionId() == null || voterDto.getEpicNumber() == null || voterDto.getBoothNumber() == null) {
//            log.error("Missing mandatory fields: voterId or electionId for account ID: {}", accountId);
//            throw new ThedalException(ThedalError.MISSING_REQUIRED_FIELDS, HttpStatus.BAD_REQUEST);
//        }
//
//		Optional<VoterEntityMongo> existingVoterOpt = voterMongoRepo.findByEpicNumberAndElectionIdAndAccountId(
//			voterDto.getEpicNumber(), voterDto.getElectionId(), accountId);
//
//		VoterEntityMongo voter;	
//        if (existingVoterOpt.isPresent()) {
//            // Update existing record
//            voter = existingVoterOpt.get();
//            log.info("Updating existing voter with EPIC number: {}", voterDto.getEpicNumber());
//        } else {
//        voter = new VoterEntityMongo();
//		voter.setAccountId(accountId);
//		log.info("Creating new voter record for EPIC number: {}", voterDto.getEpicNumber());
//		}
//        BeanUtils.copyProperties(voterDto, voter, "id", "accountId");
//		
////    // Handle ReligionEntity
////        Optional<Religion> religionOptional = religionMongoRepository.findByReligionNameAndAccountId(voterDto.getReligion(), accountId);
////        Religion religion = religionOptional.orElseGet(() -> {
////            Religion newReligion = new Religion();
////            newReligion.setReligionName(voterDto.getReligion());
////            newReligion.setAccountId(accountId);
////            return religionMongoRepository.save(newReligion);
////        });
////
////      //  Handle CasteEntity
////        Optional<Caste> casteOptional = casteMongorepository.findByCasteNameAndReligionAndAccountId(voterDto.getCaste(), religion, accountId);
////        Caste caste = casteOptional.orElseGet(() -> {
////            Caste newCaste = new Caste();
////            newCaste.setCasteName(voterDto.getCaste());
////            newCaste.setReligion(religion);
////            newCaste.setAccountId(accountId);
////            return casteMongorepository.save(newCaste);
////        });
////        
////     //Handle SubCasteEntity
////       Optional<SubCaste> subCasteOptional = subCasteRepo.findBySubCasteNameAndCasteAndReligionAndAccountId(
////                voterDto.getSubCaste(), caste, religion, accountId);
////
////        SubCaste subCaste = subCasteOptional.orElseGet(() -> {
////            SubCaste newSubCaste = new SubCaste();
////            newSubCaste.setSubCasteName(voterDto.getSubCaste());
////            newSubCaste.setCaste(caste);
////            newSubCaste.setReligion(religion); 
////            newSubCaste.setAccountId(accountId);
////            return subCasteRepo.save(newSubCaste);
////        });
//        
////		Optional<Complain> compalinOptional=complainMongoRepository.findByComplaintNameAndAccountId(voterDto.getComplaint(), accountId);
////         Complain complain=compalinOptional.orElseGet(() -> {
////			Complain newComplain= new Complain();
////			newComplain.setComplaintName(voterDto.getComplaint());
////			newComplain.setAccountId(accountId);
////			return complainMongoRepository.save(newComplain);
////		 });
//		
//        
//       // Set religionId and casteId in voter entity
////        voter.setReligion(religion);
////        voter.setCaste(caste);
////        voter.setSubCaste(subCaste);
//        
//
//        // // Save or update voter
//        // Optional<VoterEntityMongo> existingVoterOpt = voterMongoRepo.findByAccountIdAndElectionIdAndVoterId(
//        //         accountId, voter.getElectionId(), voter.getVoterId());
//
//        // if (existingVoterOpt.isPresent()) {
//        //     VoterEntityMongo existingVoter = existingVoterOpt.get();
//        //     // Update existing voter fields
//        //     BeanUtils.copyProperties(voterDto, existingVoter, "id", "accountId");
//        //     voter = existingVoter;
//        // }
//
//        try {
//            VoterEntityMongo savedVoter = voterMongoRepo.save(voter);
//            log.info("Voter successfully saved or updated: {}", savedVoter);
//
//            // Save booth information
//            try {
//                electionBoothService.saveBooth(voter.getElectionId(), voter.getBoothNumber(), accountId);
//            } catch (Exception boothException) {
//                log.warn("Failed to save booth information for election ID {}: {}", voter.getElectionId(), boothException.getMessage());
//            }
//
//            // Prepare response
//            VoterDTO savedVoterDto = new VoterDTO();
//            BeanUtils.copyProperties(savedVoter, savedVoterDto);
////            savedVoterDto.setReligion(religion.getReligionName());
////           savedVoterDto.setCaste(caste.getCasteName());
////         savedVoterDto.setReligion(religion.getReligionName());
////        // savedVoterDto.setReligionId(religion.getId());
////         savedVoterDto.setCaste(caste.getCasteName());
////        //savedVoterDto.setCasteId(caste.getId());
////         savedVoterDto.setSubCaste(subCaste.getSubCasteName());
////         //savedVoterDto.setSubCasteId(subCaste.getId());
//		 if (isLambdaEnabled) {
//			// Call AWS Lambda
//		   sqsService.sendVoterToSQS(savedVoterDto);
//		} else {
//			voterPostgresService.saveVoter(voterDto);
//		}
//
//		    Optional<VolunteerEntity> optionalVolunteer = volunteerRepository.findByUserEntityIdAndElectionEntityIdAndAccountId(requestDetails.getCurrentUserFromRequest().getId(), voter.getElectionId(),accountId);
//
//	        
//			// if (optionalVolunteer.isPresent()) {
//			// 	log.info(
//			// 			"inside saveVoter: current user is a volunteer:id:{}: saveOrUpdateVolunteerVsVoterReport service will be called",
//			// 			optionalVolunteer.get().getId());
//			// 	reportService.saveOrUpdateVolunteerVsVoterReport(accountId, voter.getElectionId(),
//			// 			optionalVolunteer.get().getId(), savedVoter.getPhoneNumber() != null,
//			// 			savedVoter.getReligion() != null, savedVoter.getCaste() != null,
//			// 			savedVoter.getDateOfBirth() != null, savedVoter.getPartyAffiliation() != null, true);
//			// }
//		 List<ElectionOverviewDTO> electionOverviewDTOList = new ArrayList<>();
//         ElectionOverviewDTO dto= new ElectionOverviewDTO();
//         dto.setGender(voterDto.getGender());
//         dto.setMobileNumber(voterDto.getMobileNo());
//         dto.setNewVoter(true);
//         dto.setPincode(voterDto.getPincode());
//         electionOverviewDTOList.add(dto);
//         reportService.saveElectionOverview(voter.getElectionId(),accountId,electionOverviewDTOList);
//         
////         List<Integer> ageList = new ArrayList<>();
////         ageList.add(voterDto.getAge());
//         List<Integer> ageList = Arrays.asList(Period.between(voterDto.getDob(), LocalDate.now()).getYears());
//         reportService.votersBasedOnAge(ageList, voter.getElectionId(), accountId);
//         
//         List<VotersHavingContactsDTO> votersHavingContactsDTOList = new ArrayList<>();
//         VotersHavingContactsDTO votersHavingContactsDTO = new VotersHavingContactsDTO();
//         votersHavingContactsDTO.setBoothNumber(voterDto.getBoothNumber());
//         votersHavingContactsDTO.setMobileNumber(voterDto.getMobileNo());  
//         votersHavingContactsDTOList.add(votersHavingContactsDTO);
//         reportService.votersHavingContacts(votersHavingContactsDTOList,voterDto.getElectionId(),accountId);
//            return new ThedalResponse<>(ThedalSuccess.VOTER_CREATED, savedVoterDto);
//
//        } catch (DataIntegrityViolationException e) {
//            log.error("Data integrity violation while saving voter: {}", e.getMessage());
//            throw new ThedalException(ThedalError.DATA_INTEGRITY_VIOLATION, HttpStatus.CONFLICT);
//        } catch (Exception e) {
//            log.error("Failed to save voter: {}", e.getMessage());
//            throw new ThedalException(ThedalError.VOTER_SAVE_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//    }
// 
//	
//	
//	//  public void sendVoterToSqs(VoterDTO voterDto) {
//    //     try {
//    //         String message = new ObjectMapper().writeValueAsString(voterDto); // Convert voterDTO to JSON string
//    //         SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
//    //                 .queueUrl(queueUrl)
//    //                 .messageBody(message)
//    //                 .build();
//    //         sqsClient.sendMessage(sendMessageRequest);
//    //         log.info("Voter data successfully sent to SQS: {}", message);
//    //     } catch (JsonProcessingException e) {
//    //         log.error("Failed to serialize voter DTO to JSON: {}", e.getMessage());
//    //     } catch (SqsException e) {
//    //         log.error("Failed to send message to SQS: {}", e.getMessage());
//    //     }
//	
//    // public void sendVoterToSqs(VoterDTO voterDto) {
//	// 	SqsClient sqsClient = SqsClient.create(); // Or inject it as a Bean in your application
//	// 	try {
//	// 		String messageBody = new ObjectMapper().writeValueAsString(voterDto); // Convert to JSON
//	
//	// 		SendMessageRequest request = SendMessageRequest.builder()
//	// 				.queueUrl(queueUrl) // Replace with your SQS Queue URL
//	// 				.messageBody(messageBody)
//	// 				.build();
//	
//	// 		SendMessageResponse response = sqsClient.sendMessage(request);
//	// 		log.info("Message sent to SQS. Message ID: {}", response.messageId());
//	// 	} catch (JsonProcessingException e) {
//	// 		log.error("Failed to serialize voter DTO to JSON: {}", e.getMessage());
//	// 	} catch (SqsException e) {
//	// 		log.error("Failed to send message to SQS: {}", e.getMessage());
//	// 	} finally {
//	// 		sqsClient.close(); // Close the client to release resources
//	// 	}
//	// }
//    
//    
//       
//
////    @Transactional
////    public void saveDynamicFields(Long accountId, Long electionId, List<DynamicFieldEntity> fields) {
////        fields.forEach(field -> {
////            field.setAccountId(accountId);
////            field.setElectionId(electionId);
////        });
////        dynamicFieldRepository.saveAll(fields);
////    }
//    
////    @Transactional
////    public ThedalResponse<List<VoterDynamicFieldDTO>> addDynamicFields(String voterId, List<VoterDynamicFieldDTO> dynamicFields) {
////        Long accountId = requestDetails.getCurrentAccountId();
////        if (accountId == null) {
////            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
////        }
////
////        // Check if the voter exists for the given voterId and accountId
////        Optional<VoterEntity> existingVoter = voterRepository.findByVoterIdAndAccountId(voterId, accountId);
////        if (existingVoter.isEmpty()) {
////            throw new ThedalException(ThedalError.VOTER_NOT_FOUND, HttpStatus.NOT_FOUND);
////        }
////
////        VoterEntity voter = existingVoter.get();
////
////        Long electionId = voter.getElectionId();
////        if (electionId == null) {
////            throw new ThedalException(ThedalError.MISSING_REQUIRED_FIELDS, HttpStatus.BAD_REQUEST);
////        }
////
////        // Save the dynamic fields
////        List<VoterDynamicField> savedFields = new ArrayList<>();
////        for (VoterDynamicFieldDTO fieldDto : dynamicFields) {
////            if (fieldDto.getFieldName() == null || fieldDto.getFieldValue() == null) {
////                throw new ThedalException(ThedalError.MISSING_REQUIRED_FIELDS, HttpStatus.BAD_REQUEST);
////            }
////
////            VoterDynamicField dynamicField = new VoterDynamicField();
////            dynamicField.setVoter(voter);
////            dynamicField.setFieldName(fieldDto.getFieldName());
////            dynamicField.setFieldValue(fieldDto.getFieldValue());
////            dynamicField.setAccountId(accountId); 
////            dynamicField.setElectionId(electionId); 
////            savedFields.add(voterDynamicFieldRepository.save(dynamicField));
////        }
////
////        // Convert saved fields to DTOs and return
////        List<VoterDynamicFieldDTO> savedFieldDtos = savedFields.stream()
////            .map(field -> {
////                VoterDynamicFieldDTO dto = new VoterDynamicFieldDTO();
////                dto.setFieldName(field.getFieldName());
////                dto.setFieldValue(field.getFieldValue());
////                return dto;
////            })
////            .collect(Collectors.toList());
////
////        return new ThedalResponse<>(ThedalSuccess.DYNAMIC_FIELD_CREATED, savedFieldDtos);
////    }
//
//
//    
//    /**
//     * Retrieves a paginated list of voters based on the provided filters.
//     * 
//     * @param voterId The ID of the voter to search for (optional).
//     * @param electionId The ID of the election to filter by (optional).
//     * @param boothNumber The booth number to filter by (optional).
//     * @param pageable The pagination information.
//     * @return A paginated list of VoterEntity that match the specified criteria.
//     * @throws ThedalException if an error occurs while retrieving the voters.
//     */
//	@Override
//	public Page<VoterEntityMongo> getVoters(Long accountId, String epicNumber,String voterId, Long electionId, Integer boothNumber, Pageable pageable) {
//		try {
//			// Check role permissions for CADRE_MANAGEMENT
//	       // requestDetails.checkUserRolePermission(RolePermission.CADRE_MANAGEMENT);
//			Page<VoterEntityMongo> result;
//	
//			if (voterId != null && electionId != null) {
//				log.info("inside getVoters:voterId:{},election:{}",voterId,electionId);
//				result = voterMongoRepo.findByAccountIdAndVoterIdAndElectionId(accountId, voterId, electionId, pageable);
//				// Handle duplicate voterId across multiple elections
//				if (result.getTotalElements() > 1) {
//					log.error("Voter ID: {} found in multiple elections", voterId);
//					throw new ThedalException(ThedalError.VOTER_DUPLICATE_IN_ELECTIONS, HttpStatus.BAD_REQUEST);
//				} else if (result.isEmpty()) {
//					throw new ThedalException(ThedalError.VOTER_NOT_FOUND, HttpStatus.NOT_FOUND);
//				}
//				return result;
//			}
//	
//			if (boothNumber != null) {
//				log.info("inside getVoters:booth number:{}",boothNumber);
//				result = voterMongoRepo.findByAccountIdAndElectionIdAndBoothNumber(accountId, electionId, boothNumber, pageable);
//				if (result.isEmpty()) {
//					throw new ThedalException(ThedalError.VOTER_NOT_FOUND, HttpStatus.NOT_FOUND);
//				}
//				return result;
//			}
//
//			if (epicNumber != null) {
//				log.info("inside getVoters:epic no:{}",epicNumber);
//	            result = voterMongoRepo.findByAccountIdAndElectionIdAndEpicNumber(accountId, electionId,epicNumber ,pageable);
//	            if (result.isEmpty()) {
//	                throw new ThedalException(ThedalError.VOTER_NOT_FOUND, HttpStatus.NOT_FOUND);
//	            }
//	            return result;
//	        }
//
//	
//			// Default: Retrieve all voters associated with accountId and electionId
//			result = voterMongoRepo.findByAccountIdAndElectionId(accountId, electionId, pageable);
//			if (result.isEmpty()) {
//				throw new ThedalException(ThedalError.VOTER_NOT_FOUND, HttpStatus.NOT_FOUND);
//			}
//			return result;
//	
//		} catch (ThedalException e) {
//			log.error("Voter retrieval failed: {}", e.getMessage());
//			throw e; // Re-throw the custom exception
//		} catch (Exception e) {
//			log.error("An unexpected error occurred while retrieving voters: {}", e.getMessage());
//			throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
//		}
//	}
//	
// 
//    /**
//	 * Updates the details of an existing voter.
//	 *
//	 * @param voterId the ID of the voter to be updated.
//	 * @param voter the new voter details.
//	 * @return the updated voter entity.
//	 * @throws ThedalException if the voter is not found or an error occurs during the update.
//	 */  
////	@Transactional
////	@Override
////	public VoterUpdateDTO updateVoter(String voterId, Long electionId, VoterUpdateDTO voterUpdateDTO) {
////	    Long accountId = requestDetails.getCurrentAccountId();
////	    if (accountId == null) {
////	        log.error("Account id not found, unauthorized access.");
////	        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
////	    }
////
////	    //Long electionId = voterUpdateDTO.getElectionId(); 
////
////	    try {
////	        log.info("Updating voter with ID: {}, Election ID: {}, Account ID: {}", voterId, electionId, accountId);
////
////	        VoterEntityMongo existingVoter = voterMongoRepo.findByVoterIdAndElectionIdAndAccountId(voterId, electionId, accountId)
////	            .orElseThrow(() -> {
////	                log.warn("Voter not found with voterId: {}, electionId: {}, accountId: {}", voterId, electionId, accountId);
////	                return new ThedalException(ThedalError.VOTER_NOT_FOUND, HttpStatus.NOT_FOUND);
////	            });
////
////				if (voterUpdateDTO.getReligion() != null) {
////					religionMongoRepository.findByReligionName(voterUpdateDTO.getReligion())
////						.ifPresentOrElse(
////							existingVoter::setReligion,
////							() -> log.warn("Religion with name '{}' not found", voterUpdateDTO.getReligion())
////						);
////				}
////		
////				// Update Caste
////				if (voterUpdateDTO.getCaste() != null) {
////					casteMongorepository.findByCasteName(voterUpdateDTO.getCaste())
////						.ifPresentOrElse(
////							existingVoter::setCaste,
////							() -> log.warn("Caste with name '{}' not found", voterUpdateDTO.getCaste())
////						);
////				}
////
////				if (voterUpdateDTO.getSubCaste() != null) {
////					subCasteRepo.findBySubCasteName(voterUpdateDTO.getSubCaste())
////						.ifPresentOrElse(
////							existingVoter::setSubCaste,
////							() -> log.warn("SubCaste with name '{}' not found", voterUpdateDTO.getSubCaste())
////						);
////				}
////
////	        
////	        // Update the fields as before
//////	        if (voterUpdateDTO.getEpicNumber() != null) existingVoter.setEpicNumber(voterUpdateDTO.getEpicNumber());
////	        //if (voterUpdateDTO.getVoterId() != null) existingVoter.setVoterId(voterUpdateDTO.getVoterId());
////	        if (voterUpdateDTO.getFirstName() != null) existingVoter.setFirstName(voterUpdateDTO.getFirstName());
////	        if (voterUpdateDTO.getLastName() != null) existingVoter.setLastName(voterUpdateDTO.getLastName());
////	        if (voterUpdateDTO.getDateOfBirth() != null) existingVoter.setDateOfBirth(voterUpdateDTO.getDateOfBirth());
////	        if (voterUpdateDTO.getGender() != null) existingVoter.setGender(voterUpdateDTO.getGender());
////	        if (voterUpdateDTO.getEmail() != null) existingVoter.setEmail(voterUpdateDTO.getEmail());
////	        if (voterUpdateDTO.getPhoneNumber() != null) existingVoter.setPhoneNumber(voterUpdateDTO.getPhoneNumber());
////	        if (voterUpdateDTO.getAddress() != null) existingVoter.setAddress(voterUpdateDTO.getAddress());
////	        if (voterUpdateDTO.getLatitude() != null) existingVoter.setLatitude(voterUpdateDTO.getLatitude());
////	        if (voterUpdateDTO.getLongitude() != null) existingVoter.setLongitude(voterUpdateDTO.getLongitude());
////	        if (voterUpdateDTO.getAvailability() != null) existingVoter.setAvailability(voterUpdateDTO.getAvailability());
////	        if (voterUpdateDTO.getPartyAffiliation() != null) existingVoter.setPartyAffiliation(voterUpdateDTO.getPartyAffiliation());
////	        //if (voterUpdateDTO.getReligion() != null) existingVoter.setReligion(voterUpdateDTO.getReligion());
////	       // if (voterUpdateDTO.getCaste() != null) existingVoter.setCaste(voterUpdateDTO.getCaste());
////	        if (voterUpdateDTO.getThirdPartyId() != null) existingVoter.setThirdPartyId(voterUpdateDTO.getThirdPartyId());
////	        if (voterUpdateDTO.getPhotoUrl() != null) existingVoter.setPhotoUrl(voterUpdateDTO.getPhotoUrl());
////	        if (voterUpdateDTO.getRemarks() != null) existingVoter.setRemarks(voterUpdateDTO.getRemarks());
////	        if (voterUpdateDTO.getBoothNumber() != null) existingVoter.setBoothNumber(voterUpdateDTO.getBoothNumber());	        
////	        
////	        // New fields
////	        if (voterUpdateDTO.getStateCode() != null) existingVoter.setStateCode(voterUpdateDTO.getStateCode());
////	        if (voterUpdateDTO.getStateName() != null) existingVoter.setStateName(voterUpdateDTO.getStateName());
////	        if (voterUpdateDTO.getStateNameL1() != null) existingVoter.setStateNameL1(voterUpdateDTO.getStateNameL1());
////	        if (voterUpdateDTO.getStateNameL2() != null) existingVoter.setStateNameL2(voterUpdateDTO.getStateNameL2());
////	        if (voterUpdateDTO.getDistrictNo() != null) existingVoter.setDistrictNo(voterUpdateDTO.getDistrictNo());
////	        if (voterUpdateDTO.getDistrictName() != null) existingVoter.setDistrictName(voterUpdateDTO.getDistrictName());
////	        if (voterUpdateDTO.getDistrictNameL1() != null) existingVoter.setDistrictNameL1(voterUpdateDTO.getDistrictNameL1());
////	        if (voterUpdateDTO.getDistrictNameL2() != null) existingVoter.setDistrictNameL2(voterUpdateDTO.getDistrictNameL2());
////	        if (voterUpdateDTO.getParliamentNo() != null) existingVoter.setParliamentNo(voterUpdateDTO.getParliamentNo());
////	        if (voterUpdateDTO.getParliamentName() != null) existingVoter.setParliamentName(voterUpdateDTO.getParliamentName());
////	        if (voterUpdateDTO.getParliamentNameL1() != null) existingVoter.setParliamentNameL1(voterUpdateDTO.getParliamentNameL1());
////	        if (voterUpdateDTO.getParliamentNameL2() != null) existingVoter.setParliamentNameL2(voterUpdateDTO.getParliamentNameL2());
////	        if (voterUpdateDTO.getAssemblyNo() != null) existingVoter.setAssemblyNo(voterUpdateDTO.getAssemblyNo());
////	        if (voterUpdateDTO.getAssemblyName() != null) existingVoter.setAssemblyName(voterUpdateDTO.getAssemblyName());
////	        if (voterUpdateDTO.getAssemblyNameL1() != null) existingVoter.setAssemblyNameL1(voterUpdateDTO.getAssemblyNameL1());
////	        if (voterUpdateDTO.getAssemblyNameL2() != null) existingVoter.setAssemblyNameL2(voterUpdateDTO.getAssemblyNameL2());
////	        if (voterUpdateDTO.getLocalBody() != null) existingVoter.setLocalBody(voterUpdateDTO.getLocalBody());
////	        if (voterUpdateDTO.getUrbanName() != null) existingVoter.setUrbanName(voterUpdateDTO.getUrbanName());
////	        if (voterUpdateDTO.getUrbanNameL1() != null) existingVoter.setUrbanNameL1(voterUpdateDTO.getUrbanNameL1());
////	        if (voterUpdateDTO.getUrbanNameL2() != null) existingVoter.setUrbanNameL2(voterUpdateDTO.getUrbanNameL2());
////	        if (voterUpdateDTO.getUrbanWardNo() != null) existingVoter.setUrbanWardNo(voterUpdateDTO.getUrbanWardNo());
////	        if (voterUpdateDTO.getDistrictUnionName() != null) existingVoter.setDistrictUnionName(voterUpdateDTO.getDistrictUnionName());
////	        if (voterUpdateDTO.getDistrictUnionNameL1() != null) existingVoter.setDistrictUnionNameL1(voterUpdateDTO.getDistrictUnionNameL1());
////	        if (voterUpdateDTO.getDistrictUnionNameL2() != null) existingVoter.setDistrictUnionNameL2(voterUpdateDTO.getDistrictUnionNameL2());
////	        if (voterUpdateDTO.getDistrictUnionWardNo() != null) existingVoter.setDistrictUnionWardNo(voterUpdateDTO.getDistrictUnionWardNo());
////	        if (voterUpdateDTO.getPanchayatUnionName() != null) existingVoter.setPanchayatUnionName(voterUpdateDTO.getPanchayatUnionName());
////	        if (voterUpdateDTO.getPanchayatUnionNameL1() != null) existingVoter.setPanchayatUnionNameL1(voterUpdateDTO.getPanchayatUnionNameL1());
////	        if (voterUpdateDTO.getPanchayatUnionNameL2() != null) existingVoter.setPanchayatUnionNameL2(voterUpdateDTO.getPanchayatUnionNameL2());
////	        if (voterUpdateDTO.getPanchayatUnionWardNo() != null) existingVoter.setPanchayatUnionWardNo(voterUpdateDTO.getPanchayatUnionWardNo());
////	        if (voterUpdateDTO.getVillagePanchayatName() != null) existingVoter.setVillagePanchayatName(voterUpdateDTO.getVillagePanchayatName());
////	        if (voterUpdateDTO.getVillagePanchayatNameL1() != null) existingVoter.setVillagePanchayatNameL1(voterUpdateDTO.getVillagePanchayatNameL1());
////	        if (voterUpdateDTO.getVillagePanchayatNameL2() != null) existingVoter.setVillagePanchayatNameL2(voterUpdateDTO.getVillagePanchayatNameL2());
////	        if (voterUpdateDTO.getVillagePanchayatWardNo() != null) existingVoter.setVillagePanchayatWardNo(voterUpdateDTO.getVillagePanchayatWardNo());
////	        
////	        //if (voterUpdateDTO.getPartNo() != null) existingVoter.setPartNo(voterUpdateDTO.getPartNo());
////	        if (voterUpdateDTO.getPartName() != null) existingVoter.setPartName(voterUpdateDTO.getPartName());
////	        if (voterUpdateDTO.getPartNameL1() != null) existingVoter.setPartNameL1(voterUpdateDTO.getPartNameL1());
////	        if (voterUpdateDTO.getPartLatLong() != null) existingVoter.setPartLatLong(voterUpdateDTO.getPartLatLong());
////	        if (voterUpdateDTO.getPincode() != null) existingVoter.setPincode(voterUpdateDTO.getPincode());
////	        if (voterUpdateDTO.getSectionNo() != null) existingVoter.setSectionNo(voterUpdateDTO.getSectionNo());
////	        
////	        if (voterUpdateDTO.getSectionName() != null) existingVoter.setSectionName(voterUpdateDTO.getSectionName());
////	        if (voterUpdateDTO.getSerialNumber() != null) existingVoter.setSerialNumber(voterUpdateDTO.getSerialNumber());
////	        if (voterUpdateDTO.getRelationName() != null) existingVoter.setRelationName(voterUpdateDTO.getRelationName());
////	        if (voterUpdateDTO.getRelationNameL1() != null) existingVoter.setRelationNameL1(voterUpdateDTO.getRelationNameL1());
////	        if (voterUpdateDTO.getRelationNameL2() != null) existingVoter.setRelationNameL2(voterUpdateDTO.getRelationNameL2());
////	        if (voterUpdateDTO.getRelationType() != null) existingVoter.setRelationType(voterUpdateDTO.getRelationType());
////	        
////	        if (voterUpdateDTO.getAge() != null) existingVoter.setAge(voterUpdateDTO.getAge());
////	        if (voterUpdateDTO.getAnniversaryDate() != null) existingVoter.setAnniversaryDate(voterUpdateDTO.getAnniversaryDate());
////	       // if (voterUpdateDTO.getSubCaste() != null) existingVoter.setSubCaste(voterUpdateDTO.getSubCaste());
////	     
////	        // Flush to force synchronization before saving
////            //entityManager.flush();
////	        VoterEntityMongo updatedVoter = voterMongoRepo.save(existingVoter);
////	        log.info("Voter with ID: {} successfully updated", voterId);
////
////			voterMongoRepo.save(existingVoter);
////	        log.info("Voter with ID: {} successfully updated", voterId);
////
////			voterPostgresService.updateVoter(voterId, electionId, voterUpdateDTO);
////
////    Optional<VolunteerEntity> optionalVolunteer = volunteerRepository.findByUserEntityIdAndElectionEntityIdAndAccountId(requestDetails.getCurrentUserFromRequest().getId(), electionId,accountId);
////
////	        
////	if (optionalVolunteer.isPresent()) {
////		log.info("inside updateVoter: current user is a volunteer:id:{}: saveOrUpdateVolunteerVsVoterReport service will be called",optionalVolunteer.get().getId());
////		reportService.saveOrUpdateVolunteerVsVoterReport(accountId, electionId, optionalVolunteer.get().getId(),
////				voterUpdateDTO.getPhoneNumber() != null, voterUpdateDTO.getReligion() != null,
////				voterUpdateDTO.getCaste() != null, voterUpdateDTO.getDateOfBirth() != null,
////				voterUpdateDTO.getPartyAffiliation() != null, false);
////	}
////	        return mapEntityToDto(existingVoter);
////
////	    } catch (Exception e) {
////	        log.error("Error updating voter with ID: {}: {}", voterId, e.getMessage());
////	        throw new ThedalException(ThedalError.VOTER_UPDATE_FAILED, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
////	    }
////	}
////
////	private VoterUpdateDTO mapEntityToDto(VoterEntityMongo voterEntity) {
////	    VoterUpdateDTO dto = new VoterUpdateDTO();
////	    //dto.setVoterId(voterEntity.getVoterId());
////	    dto.setFirstName(voterEntity.getFirstName());
////	    dto.setLastName(voterEntity.getLastName());
////	    //dto.setEpicNumber(voterEntity.getEpicNumber());
////	    dto.setDateOfBirth(voterEntity.getDateOfBirth());
////	    dto.setGender(voterEntity.getGender());
////	    dto.setEmail(voterEntity.getEmail());
////	    dto.setPhoneNumber(voterEntity.getPhoneNumber());
////	    dto.setAddress(voterEntity.getAddress());
////	    dto.setLatitude(voterEntity.getLatitude());
////	    dto.setLongitude(voterEntity.getLongitude());
////	    dto.setAvailability(voterEntity.getAvailability());
////	    dto.setPartyAffiliation(voterEntity.getPartyAffiliation());
////	   // dto.setReligion(voterEntity.getReligion());
////	   // dto.setCaste(voterEntity.getCaste());
////	    dto.setThirdPartyId(voterEntity.getThirdPartyId());
////	    dto.setPhotoUrl(voterEntity.getPhotoUrl());
////	    dto.setRemarks(voterEntity.getRemarks());
////	    dto.setBoothNumber(voterEntity.getBoothNumber());
////	    
////	    
////	 // Mapping new fields as well
////	    dto.setStateCode(voterEntity.getStateCode());
////	    dto.setStateName(voterEntity.getStateName());
////	    dto.setStateNameL1(voterEntity.getStateNameL1());
////	    dto.setStateNameL2(voterEntity.getStateNameL2());
////	    dto.setDistrictNo(voterEntity.getDistrictNo());
////	    dto.setDistrictName(voterEntity.getDistrictName());
////	    dto.setDistrictNameL1(voterEntity.getDistrictNameL1());
////	    dto.setDistrictNameL2(voterEntity.getDistrictNameL2());
////	    dto.setParliamentNo(voterEntity.getParliamentNo());
////	    dto.setParliamentName(voterEntity.getParliamentName());
////	    dto.setParliamentNameL1(voterEntity.getParliamentNameL1());
////	    dto.setParliamentNameL2(voterEntity.getParliamentNameL2());
////	    dto.setAssemblyNo(voterEntity.getAssemblyNo());
////	    dto.setAssemblyName(voterEntity.getAssemblyName());
////	    dto.setAssemblyNameL1(voterEntity.getAssemblyNameL1());
////	    dto.setAssemblyNameL2(voterEntity.getAssemblyNameL2());
////	    dto.setLocalBody(voterEntity.getLocalBody());
////	    dto.setUrbanName(voterEntity.getUrbanName());
////	    dto.setUrbanNameL1(voterEntity.getUrbanNameL1());
////	    dto.setUrbanNameL2(voterEntity.getUrbanNameL2());
////	    dto.setUrbanWardNo(voterEntity.getUrbanWardNo());
////	    dto.setDistrictUnionName(voterEntity.getDistrictUnionName());
////	    dto.setDistrictUnionNameL1(voterEntity.getDistrictUnionNameL1());
////	    dto.setDistrictUnionNameL2(voterEntity.getDistrictUnionNameL2());
////	    dto.setDistrictUnionWardNo(voterEntity.getDistrictUnionWardNo());
////	    dto.setPanchayatUnionName(voterEntity.getPanchayatUnionName());
////	    dto.setPanchayatUnionNameL1(voterEntity.getPanchayatUnionNameL1());
////	    dto.setPanchayatUnionNameL2(voterEntity.getPanchayatUnionNameL2());
////	    dto.setPanchayatUnionWardNo(voterEntity.getPanchayatUnionWardNo());
////	    dto.setVillagePanchayatName(voterEntity.getVillagePanchayatName());
////	    dto.setVillagePanchayatNameL1(voterEntity.getVillagePanchayatNameL1());
////	    dto.setVillagePanchayatNameL2(voterEntity.getVillagePanchayatNameL2());
////		dto.setDistrictUnionWardNo(voterEntity.getDistrictUnionWardNo());
////	    dto.setPanchayatUnionName(voterEntity.getPanchayatUnionName());
////	    dto.setPanchayatUnionNameL1(voterEntity.getPanchayatUnionNameL1());
////	    dto.setPanchayatUnionNameL2(voterEntity.getPanchayatUnionNameL2());
////	    dto.setPanchayatUnionWardNo(voterEntity.getPanchayatUnionWardNo());
////	    dto.setVillagePanchayatName(voterEntity.getVillagePanchayatName());
////	    dto.setVillagePanchayatNameL1(voterEntity.getVillagePanchayatNameL1());
////	    dto.setVillagePanchayatNameL2(voterEntity.getVillagePanchayatNameL2());
////	    dto.setVillagePanchayatWardNo(voterEntity.getVillagePanchayatWardNo());
////	    //dto.setPartNo(voterEntity.getPartNo());
////	    dto.setPartName(voterEntity.getPartName());
////	    dto.setPartNameL1(voterEntity.getPartNameL1());
////	    dto.setPartLatLong(voterEntity.getPartLatLong());
////	    dto.setPincode(voterEntity.getPincode());
////	    dto.setSectionNo(voterEntity.getSectionNo());
////	    dto.setSectionName(voterEntity.getSectionName());
////	    dto.setSerialNumber(voterEntity.getSerialNumber());
////	    dto.setRelationName(voterEntity.getRelationName());
////	    dto.setRelationNameL1(voterEntity.getRelationNameL1());
////	    dto.setRelationNameL2(voterEntity.getRelationNameL2());
////	    dto.setRelationType(voterEntity.getRelationType());
////	    dto.setAge(voterEntity.getAge());
////	    dto.setAnniversaryDate(voterEntity.getAnniversaryDate());
////	  //  dto.setSubCaste(voterEntity.get());
////		return dto;
////	}
//	
//    /**
//	 * Deletes a voter by their ID.
//	 *
//	 * @param voterId the ID of the voter to be deleted.
//	 * @return a ThedalResponse indicating the success of the deletion.
//	 * @throws ThedalException if the voter is not found or an error occurs during deletion.
//	 */
////    @Override
////    @Transactional
////    public ThedalResponse<Void> deleteById(String voterId, Long electionId) {
////        Long accountId = requestDetails.getCurrentAccountId();
////
////        if (accountId == null) {
////            log.error("Account ID not found, unauthorized access.");
////            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
////        }
////
////        try {
////        	log.info("Deleting voter with ID: {}, for accountId: {}, electionId: {}", voterId, accountId, electionId);
////            
////            VoterEntity existingVoter = voterRepository.findByAccountIdAndElectionIdAndVoterId(accountId, electionId, voterId)
////                    .orElseThrow(() -> {
////                        log.warn("Voter not found with accountId: {}, electionId: {}, voterId: {}", accountId, electionId, voterId);
////                        return new ThedalException(ThedalError.VOTER_NOT_FOUND, HttpStatus.NOT_FOUND);
////                    });
////
////            //voterRepository.deleteById(voterId);
////            voterRepository.delete(existingVoter);
////            log.info("Voter with ID: {} successfully deleted", voterId);
////            return new ThedalResponse<>(ThedalSuccess.VOTER_DELETED);
////        } catch (Exception e) {
////            log.error("Error deleting voter with ID: {}: {}", voterId, e.getMessage());
////            throw new ThedalException(ThedalError.ACCESS_DENIED, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
////        }
////    }
//	@Override
//	@Transactional
//	public ThedalResponse<Void> deleteById(String voterId, Long electionId) {
//	    Long accountId = requestDetails.getCurrentAccountId();
//
//	    if (accountId == null) {
//	        log.error("Account ID not found, unauthorized access.");
//	        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
//	    }
//
//	    try {
//	        log.info("Deleting voter with ID: {}, for accountId: {}, electionId: {}", voterId, accountId, electionId);
//
//	        // Fetch the voter entity
//	        VoterEntityMongo existingVoter = voterMongoRepo.findByAccountIdAndElectionIdAndVoterId(accountId, electionId, voterId)
//	                .orElseThrow(() -> {
//	                    log.warn("Voter not found with accountId: {}, electionId: {}, voterId: {}", accountId, electionId, voterId);
//	                    return new ThedalException(ThedalError.VOTER_NOT_FOUND, HttpStatus.NOT_FOUND);
//	                });
//
//	        // Delete associated records in voter_dynamic_fields
//	        //voterDynamicFieldRepository.deleteByVoterId(existingVoter.getVoterId());
//	        log.info("Associated dynamic fields for voter ID: {} successfully deleted", voterId);
//
//	        // Delete the voter
//	        voterMongoRepo.delete(existingVoter);
//	        log.info("Voter with ID: {} successfully deleted", voterId);
//			voterPostgresService.deleteById(voterId, electionId);
//
//	        return new ThedalResponse<>(ThedalSuccess.VOTER_DELETED);
//	    }catch (ThedalException e) {
//	        // Handle application-specific exceptions, like not found or unauthorized access
//	        log.error("Error while deleting voter with ID: {}: {}", voterId, e.getMessage());
//	        throw e;
//	    } catch (Exception e) {
//	        log.error("Error deleting voter with ID: {}: {}", voterId, e.getMessage());
//	        throw new ThedalException(ThedalError.ACCESS_DENIED, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
//	    }
//	}
//
//
//    /**
//	 * Retrieves all voter locations for mapping purposes.
//	 *
//	 * @return a ThedalResponse containing a list of voter locations (latitude and longitude).
//	 * @throws ThedalException if an error occurs during retrieval.
//	 */	
//    @Override
//    public ThedalResponse<Page<VoterLocationDTO>> getAllVoterLocations(Long electionId, Long boothNumber, int page, int size) {
//    	Long accountId = requestDetails.getCurrentAccountId();
//
//        if (accountId == null) {
//            log.error("Account ID not found, unauthorized access.");
//            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
//        }
//    	
//        log.info("Attempting to retrieve voter locations with accountId: {}, electionId: {}, boothNumber: {}, page: {}, size={}", 
//                accountId, electionId, boothNumber, page, size);
//        Pageable pageable = PageRequest.of(page, size);
//        
//        try {
//            Page<VoterEntityMongo> voterPage;
//
//            // If boothNumber is provided, try to retrieve voters with both filters
//            if (boothNumber != null) {
//                voterPage = voterMongoRepo.findByElectionIdAndBoothNumberAndAccountId(electionId, boothNumber, accountId, pageable);
//                if (voterPage.isEmpty()) {
//                	throw new ThedalException(ThedalError.VOTER_LOCATIONS_BOOTH_NUMBER, HttpStatus.NOT_FOUND);
//                }
//            } else {
//                // If only electionId is provided, retrieve voters by electionId
//                voterPage = voterMongoRepo.findByElectionIdAndAccountId(electionId, accountId, pageable);
//                if (voterPage.isEmpty()) {
//                	throw new ThedalException(ThedalError.VOTER_LOCATIONS_ELECTION_ID, HttpStatus.NOT_FOUND);
//                }
//            }
//
//            Page<VoterLocationDTO> voterLocationDTOPage = voterPage.map(voter -> 
//                new VoterLocationDTO(
//                    voter.getVoterId(),
//                    voter.getLatitude(),
//                    voter.getLongitude()
//                )
//            );
//
//            log.info("Successfully retrieved {} voter locations for accountId: {}, electionId: {}, boothNumber: {}", 
//                    voterLocationDTOPage.getTotalElements(), accountId, electionId, boothNumber);
//            return new ThedalResponse<>(ThedalSuccess.MAP_VOTER_LOCATIONS_SUCCESS, voterLocationDTOPage);
//
//        } catch (ThedalException e) {
//            log.error("Error retrieving voter locations: {}", e.getMessage());
//            throw e; 
//        } catch (Exception e) {
//        	log.error("Error retrieving voter locations for accountId: {}, electionId: {}, boothNumber: {}: {}", 
//                    accountId, electionId, boothNumber, e.getMessage(), e);
//            throw new ThedalException(ThedalError.VOTER_LOCATIONS_RETRIEVAL_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//    }
//
//    
//    /**
//     * Retrieves the voter details based on the provided EPIC number by making an external API call.
//     *
//     * @param epicNumber the EPIC number for which to retrieve voter details
//     * @return a JSON string containing the voter details retrieved from the external API
//     */
//	@Override
//	public String getVoterDetails(String epicNumber) {
//      String apiUrl = "https://voter-api-qno3.onrender.com/get-epic-details?epicNumber=" + epicNumber;
//	        
//	    // Use RestTemplate to make the external API call
//	    RestTemplate restTemplate = new RestTemplate();
//	    String response = restTemplate.getForObject(apiUrl, String.class);
//	        
//	     return response;
//	}	
//
//		/**
//		 * Retrieves a paginated list of bulk uploads based on the specified election ID and optional status.
//		 * The results can be sorted by ID or start time as specified by the sortBy parameter.
//		 *
//		 * @param electionId the ID of the election for which to retrieve bulk uploads
//		 * @param status the status to filter bulk uploads (can be null to retrieve all)
//		 * @param page the page number to retrieve (0-indexed)
//		 * @param size the number of results per page
//		 * @param sortBy the field to sort by (default is "startTime", can be "id" to sort by ID)
//		 * @return a list of BulkUploadDto objects representing the bulk uploads
//		 */
//		@Override
//		//@Transactional
//		public Page<BulkUploadDto> getBulkUploads(Long electionId, String status, Integer page, Integer size, String sortBy) {
//			Long accountId = requestDetails.getCurrentAccountId();
//		    if (accountId == null) {
//		    	log.error("Account ID not found, unauthorized access attempt.");
//		        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
//		    }
//		    try {
//		    
//		    	Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, sortBy.equals("id") ? "id" : "startTime"));
//		        Page<BulkUploadEntity> bulkUploads;
//
//	            if (status != null) {
//	                // Filtering by status
//	                bulkUploads = bulkUploadRepo.findByAccountIdAndElectionIdAndStatus(accountId, electionId, BulkUploadStatus.valueOf(status.toUpperCase()), pageable);
//	            } else {
//	                // Sort by ID or Start Time based on the sortBy parameter
//	                 if ("id".equalsIgnoreCase(sortBy)) {
//	                    bulkUploads = bulkUploadRepo.findByAccountIdAndElectionIdOrderByIdDesc(accountId, electionId, pageable);
//	                 } else {
//	                bulkUploads = bulkUploadRepo.findByAccountIdAndElectionIdOrderByStartTimeDesc(accountId, electionId, pageable);
//	                }
//	             }
//	            
//	            if (bulkUploads.isEmpty()) {
//	                log.error("No bulk upload found for Election ID {}", electionId);
//	                throw new ThedalException(ThedalError.BULK_UPLOAD_NOT_FOUND_ELECTION, HttpStatus.NOT_FOUND);
//	            }
//
//	         // Map BulkUploadEntity to BulkUploadDto for each element in the page
//	            Page<BulkUploadDto> bulkUploadDtos = bulkUploads.map(this::convertToDto);
//	            return bulkUploadDtos;
//	                
//		    } catch (IllegalArgumentException e) {
//		        log.error("Invalid status provided: {}", status, e);
//		        throw new ThedalException(ThedalError.INVALID_STATUS, HttpStatus.BAD_REQUEST);
//		    } catch (ThedalException e) {
//		        throw e;
//		    } 
//	        
//	    }
//		
//		private BulkUploadDto convertToDto(BulkUploadEntity entity) {
//	    BulkUploadDto dto = new BulkUploadDto(entity.getId(), entity.getElectionId(),
//	                                          entity.getStartTime(), entity.getEndTime(), entity.getStatus());
//	    log.info("Converted BulkUploadEntity to BulkUploadDto: {}", dto);
//	    return dto;
//	}
//		
//		/**
//		 * Retrieves the status of a specific bulk upload identified by its ID.
//		 *
//		 * @param bulkUploadId the ID of the bulk upload whose status is to be retrieved
//		 * @return a BulkUploadStatusDto containing the status and timing details of the bulk upload
//		 * @throws ThedalException if the bulk upload with the specified ID is not found
//		 */
//		@Override
//		public BulkUploadStatusDto getBulkUploadStatus(Long bulkUploadId) {
//			Long accountId = requestDetails.getCurrentAccountId();
//		    if (accountId == null) {
//		        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
//		    }
//			BulkUploadEntity bulkUploadEntity = bulkUploadRepo.findByIdAndAccountId(bulkUploadId, accountId)
//		            .orElseThrow(() -> new ThedalException(ThedalError.BULK_UPLOAD_NOT_FOUND, HttpStatus.NOT_FOUND));
//		    
//		   // return new BulkUploadStatusDto(bulkUploadEntity.getId(), bulkUploadEntity.getStatus(), bulkUploadEntity.getStartTime(), bulkUploadEntity.getEndTime());
//			return new BulkUploadStatusDto(
//			        bulkUploadEntity.getId(),
//			        bulkUploadEntity.getStatus(),
//			        bulkUploadEntity.getStartTime(),
//			        bulkUploadEntity.getEndTime(),
//			        bulkUploadEntity.getTotalProcessedVoters(), 
//			        bulkUploadEntity.getTotalFailedVoters(),    
//			        bulkUploadEntity.getTotalRecords(),
//			        bulkUploadEntity.getTotalSuccessVoters() 
//			    );
//		}
//		
////		@Override
////		@Transactional
////		public ThedalResponse<BulkUploadResponse> uploadVotersFromXlsxOrCsv(MultipartFile file, Long electionId) {
////		    long startTime = System.currentTimeMillis();
////		    Long accountId = requestDetails.getCurrentAccountId();
////
////		    if (accountId == null) {
////		        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
////		    }
////
////		    if (!isSupportedFormat(file.getOriginalFilename()) || file.isEmpty()) {
////		        throw new ThedalException(ThedalError.INVALID_FILE_FORMAT, HttpStatus.BAD_REQUEST);
////		    }
////
////		    String folder = "voter_uploads";
////		    String uniqueId = UUID.randomUUID().toString().substring(0, 8);
////		    String originalFileName = file.getOriginalFilename();
////		    String fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
////		    String uniqueFileName = folder + "/voter_" + System.currentTimeMillis() + "_" + uniqueId + fileExtension;
////
////		    String fileUrl = null;
////		    
////		    Long bulkUploadId = null;
////		    
////		    try {
////		        fileUrl = awsFileUpload.uploadMultipartFile(file, uniqueFileName, s3bucket);
////		        log.info("File uploaded to S3 at: {}", fileUrl);
////		        
////		        BulkUploadEntity bulkUploadEntity = new BulkUploadEntity();
////		        bulkUploadEntity.setAccountId(accountId);
////		        bulkUploadEntity.setElectionId(electionId);
////		        bulkUploadEntity.setStartTime(LocalDateTime.now());
////		        bulkUploadEntity.setStatus(BulkUploadStatus.IN_PROGRESS);
////		        bulkUploadRepo.save(bulkUploadEntity);
////		        
////		        bulkUploadId = bulkUploadEntity.getId();
////		        
////		        Files fileEntity = new Files(HandlerType.BULKUPLOAD_FILES, electionId, originalFileName, fileUrl);
////			    fileEntity.setBulkUpload(bulkUploadEntity);
////			    Files files = filesRepo.save(fileEntity);
////			   
//////		        if (fileExtension.equalsIgnoreCase(".xlsx")) {
//////		            voterFileUploadService.processExcelFileAsync(bulkUploadId, accountId, electionId, fileUrl);
//////		        } else if (fileExtension.equalsIgnoreCase(".csv")) {
//////		            voterFileUploadService.processCsvFileAsync(bulkUploadId, accountId, electionId, fileUrl);
//////		        }
////			    log.info("uploadVotersFromXlsxOrCsv: fileId:{}",files.getId());
////			   jobSchedulerService.scheduleVoterFileUploadJob(bulkUploadId, accountId, electionId, files.getId());
////			   
////		        bulkUploadEntity.setStatus(BulkUploadStatus.COMPLETED);
////		        bulkUploadEntity.setEndTime(LocalDateTime.now());
////		        bulkUploadRepo.save(bulkUploadEntity);
////
////		    } catch (IOException e) {
////		        log.error("Error uploading file to S3", e);
////		        throw new ThedalException(ThedalError.FILE_PROCESSING_FAILED, HttpStatus.INTERNAL_SERVER_ERROR, "File upload to S3 failed.");
////		    } catch (Exception e) {
////		        log.error("Error processing file", e);
////		        throw new ThedalException(ThedalError.FILE_PROCESSING_FAILED, HttpStatus.INTERNAL_SERVER_ERROR, "File processing failed.");
////		    } finally {
////		        long endTime = System.currentTimeMillis();
////		        log.info("Time taken to process file: {} ms", (endTime - startTime));
////		    }
////
////		    BulkUploadResponse bulkUploadResponse = new BulkUploadResponse(bulkUploadId);
////		    return new ThedalResponse<>(ThedalSuccess.BULK_VOTERS_UPLOAD_IN_QUEUE, bulkUploadResponse);
////		    //return new ThedalResponse<>(ThedalSuccess.BULK_VOTERS_UPLOAD_IN_QUEUE);
////		}
////
////		private boolean isSupportedFormat(String originalFileName) {
////		    return originalFileName != null && (originalFileName.endsWith(".xlsx") || originalFileName.endsWith(".csv"));
////		}
////	
////
////		@Transactional
////		public ThedalResponse<Void> startUploadVotersFromXlsxOrCsv(Long accountId, Long bulkUploadId, Long electionId, Long fileId) {
////		    long startTime = System.currentTimeMillis();
////		    log.info("inside startUploadVotersFromXlsxOrCsv:fileID:{}", fileId);
////
////		    Files fileMetadata = filesRepository.findById(fileId)
////		        .orElseThrow(() -> new ThedalException(ThedalError.FILE_NOT_FOUND, HttpStatus.NOT_FOUND));
////
////		    String fileUrl = fileMetadata.getUrl();
////		    String fileName = fileMetadata.getFileName();
////		    
////		    Set<Integer> boothNumbersSet = new HashSet<>();
////
////		    try {
////		        if (fileName.endsWith(".xlsx")) {
////		            voterFileUploadService.processExcelFileAsync(bulkUploadId, accountId, electionId, fileUrl);
////		        } else if (fileName.endsWith(".csv")) {
////		            voterFileUploadService.processCsvFileAsync(bulkUploadId, accountId, electionId, fileUrl);
////		        }
////
////		        // Retrieve the voters after they are saved
////		        List<VoterEntity> savedVoters = voterRepository.findByElectionIdAndAccountId(electionId, accountId);
////
////		        // Collect booth numbers from the saved voters
////		        for (VoterEntity voter : savedVoters) {
////		            Integer boothNumber = voter.getBoothNumber();
////		            if (boothNumber != null) {
////		                boothNumbersSet.add(boothNumber);
////		            }
////		        }
////
////		        log.info("Booth numbers from file for electionId {}: {}", electionId, boothNumbersSet);
////
////		        Set<Integer> existingBoothNumbers = electionBoothRepository.findBoothNumbersByElectionId(electionId);
////		        log.info("Existing booth numbers in the database for electionId {}: {}", electionId, existingBoothNumbers);
////
////		        // Process and update booth numbers
////		        for (Integer boothNumber : boothNumbersSet) {
////		            if (existingBoothNumbers.contains(boothNumber)) {
////		                // Update existing booth number
////		                ElectionBooth existingBooth = electionBoothRepository.findByElectionIdAndBoothNumber(electionId, boothNumber)
////		                        .orElseThrow(() -> new ThedalException(ThedalError.ELECTION_BOOTH_NOT_FOUND, HttpStatus.NOT_FOUND));
////		                
////		                // Update any necessary fields, e.g., status, etc.
////		                existingBooth.setBoothNumber(boothNumber); 
////		                electionBoothRepository.save(existingBooth);
////		                log.info("Updated existing booth number {} for electionId {}", boothNumber, electionId);
////		            } else {
////		                // Insert new booth number if it doesn't exist
////		                ElectionEntity electionEntity = electionRepository.findById(electionId)
////		                        .orElseThrow(() -> new ThedalException(ThedalError.ELECTION_NOT_FOUND, HttpStatus.NOT_FOUND));
////
////		                ElectionBooth electionBooth = new ElectionBooth();
////		                electionBooth.setElection(electionEntity);
////		                electionBooth.setBoothNumber(boothNumber);
////		                electionBoothRepository.save(electionBooth);
////		                log.info("Inserted new booth number {} for electionId {}", boothNumber, electionId);
////		            }
////		        }
////
////		    } catch (IOException e) {
////		        log.error("Error processing the file", e);
////		        throw new ThedalException(ThedalError.FILE_PROCESSING_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
////		    }
////
////		    long endTime = System.currentTimeMillis();
////		    long duration = endTime - startTime;
////		    log.info("Time taken to process file: {} ms", duration);
////		    log.info("end of startUploadVotersFromXlsxOrCsv:fileID:{}", fileId);
////
////		    return new ThedalResponse<>(ThedalSuccess.BULK_VOTERS_CREATED);
////		}
//
////		  @Transactional
////		  @Override
////		    public ThedalResponse<VoterEntityMongo> markVoterAsVoted(Long electionId, String voterId) {
////			  Long accountId = requestDetails.getCurrentAccountId();
////			  if (accountId == null) {
////		            log.error("Account ID not found, unauthorized access.");
////		            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
////		        }  
////			  
////			  Optional<VoterEntityMongo> voterOpt = voterMongoRepo.findByVoterIdAndElectionId(voterId, electionId);
////
////		        if (voterOpt.isEmpty()) {
////		            throw new ThedalException(ThedalError.VOTER_NOT_FOUND, HttpStatus.NOT_FOUND);
////		        }
////
////		        VoterEntityMongo voter = voterOpt.get();
////
////		        if (Boolean.TRUE.equals(voter.getHasVoted())) {
////		            throw new ThedalException(ThedalError.ALREADY_VOTED, HttpStatus.CONFLICT);
////		        }
////
////		        voter.setHasVoted(true);
////		       // voter.setVotedTimestamp(System.currentTimeMillis());
////		        voter.setVotedTimestamp(LocalDateTime.now());
////		        
////		        VoterEntityMongo updatedVoter = voterMongoRepo.save(voter);
////		        voterPostgresService.markVoterAsVoted(electionId, voterId);
////		        return new ThedalResponse<>(ThedalSuccess.VOTING_STATUS_UPDATED, updatedVoter);
////		    }
////		  
////		  @Transactional
////		  @Override
////		  public ThedalResponse<BulkVoterUpdateResponses> markMultipleVotersAsVoted(Long electionId, List<VoterVoteRequest> voterVoteRequests) {
////		      Long accountId = requestDetails.getCurrentAccountId();
////		      if (accountId == null) {
////		          log.error("Account ID not found, unauthorized access.");
////		          throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
////		      }
////
////		      List<VoterEntityMongo> updatedVoters = new ArrayList<>();
////		      List<String> updatedVotersIds = new ArrayList<>();
////		      List<String> alreadyVotedVotersIds = new ArrayList<>();
////		      List<String> notFoundVotersIds = new ArrayList<>();
////
////		      int alreadyVotedCount = 0;
////		      int updatedVotersCount = 0;
////
////		      // Extract voter IDs from the request for batch fetching
////		      List<String> voterIds = voterVoteRequests.stream()
////		              .map(VoterVoteRequest::getVoterId)
////		              .toList();
////
////		      List<VoterEntityMongo> voters = voterMongoRepo.findAllByVoterIdInAndElectionId(voterIds, electionId);
////		      Set<String> foundVoterIds = voters.stream().map(VoterEntityMongo::getVoterId).collect(Collectors.toSet());
////
////		      // Process each request
////		      for (VoterVoteRequest request : voterVoteRequests) {
////		          String voterId = request.getVoterId();
////		          //Long voteTimestamp = request.getVoteTimestamp();
////		          LocalDateTime voteTimestamp = request.getVoteTimestamp();
////
////		          if (!foundVoterIds.contains(voterId)) {
////		              notFoundVotersIds.add(voterId);
////		              continue;
////		          }
////
////		          VoterEntityMongo voter = voters.stream()
////		                                    .filter(v -> v.getVoterId().equals(voterId))
////		                                    .findFirst()
////		                                    .orElse(null);
////
////		          if (voter == null) {
////		              notFoundVotersIds.add(voterId);
////		              continue;
////		          }
////
////		          if (Boolean.TRUE.equals(voter.getHasVoted())) {
////		              alreadyVotedVotersIds.add(voterId);
////		              alreadyVotedCount++;
////		              continue;
////		          }
////
////		          // Mark as voted and set the provided timestamp
////		          voter.setHasVoted(true);
////		          //voter.setVotedTimestamp(voteTimestamp != null ? voteTimestamp : System.currentTimeMillis());
////		          voter.setVotedTimestamp(voteTimestamp != null ? voteTimestamp : LocalDateTime.now());
////		          updatedVoters.add(voter);
////		          updatedVotersIds.add(voterId);
////		          updatedVotersCount++;
////		      }
////
////		      // Batch save updated voters
////		      if (!updatedVoters.isEmpty()) {
////		          voterMongoRepo.saveAll(updatedVoters);
////		      }
////
////		      // Prepare the response
////		      BulkVoterUpdateResponses response = new BulkVoterUpdateResponses(
////		              updatedVoters,
////		              updatedVotersIds,
////		              alreadyVotedVotersIds,
////		              notFoundVotersIds,
////		              voterVoteRequests.size(),
////		              alreadyVotedCount,
////		              updatedVotersCount
////		      );
////		      voterPostgresService.markMultipleVotersAsVoted(electionId, voterVoteRequests);
////		      return new ThedalResponse<>(ThedalSuccess.VOTING_STATUS_UPDATED, response);
////		  }
//
//		  
//}
//
//		
//		
//
//
//
//
