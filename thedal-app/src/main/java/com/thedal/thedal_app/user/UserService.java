package com.thedal.thedal_app.user;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.thedal.thedal_app.account.AccountEntity;
import com.thedal.thedal_app.account.AccountService;
import com.thedal.thedal_app.auth.MobileVerificationRepo;
import com.thedal.thedal_app.awsfilestore.ImageUpload;
import com.thedal.thedal_app.election.OtpService;
import com.thedal.thedal_app.general.RequestDetailsService;
import com.thedal.thedal_app.notification.NotificationService;
import com.thedal.thedal_app.notification.NotificationTemplate;
import com.thedal.thedal_app.response.ThedalResponse;
import com.thedal.thedal_app.response.ThedalSuccess;
import com.thedal.thedal_app.session.UserSessionRepository;
import com.thedal.thedal_app.notification.NotificationsLogRepository;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;
import com.thedal.thedal_app.util.Response;
import com.thedal.thedal_app.volunteer.MongoVolunteerRepository;
import com.thedal.thedal_app.volunteer.VolunteerEntity;
import com.thedal.thedal_app.volunteer.VolunteerRepository;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class UserService {

	@Value("${aws.s3.image.bucket}") 
    private String s3Bucket;

	@Autowired
	private NotificationTemplate notificationTemplate;

	@Autowired
	private UserActivityRepository userActivityRepository;

	@Autowired
    private NotificationService notificationService;

	@Autowired
	private ImageUpload imageUpload;

    @Autowired
    private UserRepo userRepository;

    @Autowired
    private AccountService accountService;
    
    @Autowired
    private RequestDetailsService requestDetails;

    @Autowired
    MobileVerificationRepo mobileVerificationRepo;
    @Autowired
    private VolunteerRepository volunteerRepository;
    @Autowired
    private TwoFactorStatusChangeAttemptRepository twoFactorStatusChangeAttemptRepository;

    @Autowired
    private OtpService otpService;
    
    @Autowired
    private MongoUserRepository mongoUserRepository;
    @Autowired
    private OtpRequiredStatusChangeAttemptRepository otpRequiredStatusChangeAttemptRepository;
    @Autowired
    private MongoVolunteerRepository mongoVolunteerRepository;
    
    @Autowired
    private UserSessionRepository userSessionRepository;
    
    @Autowired
    private NotificationsLogRepository notificationsLogRepository;
    
    private static final Long SUPER_ADMIN_ROLE_ID = 1L;

    public UserDetailsDto getUserById(Long userId) {
		AccountEntity account = accountService.getCurrentAccountFromRequest();
		if (account == null) {
			log.error("Account not found, unauthorized access.");
			throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
		}
		
		// Handle potential duplicates in MongoDB - use findFirst to get one result
		try {
		    MongoUser mongoUser = mongoUserRepository.findFirstByUserId(userId);
		    if (mongoUser == null) {
		        throw new IllegalArgumentException("User not found");
		    }
		    
		    // Check for duplicates and log warning
		    List<MongoUser> allMatches = mongoUserRepository.findAllByUserId(userId);
		    if (allMatches.size() > 1) {
		        log.warn("Found {} duplicate users with userId={}. Using first match. Consider running data cleanup.", 
		                allMatches.size(), userId);
		    }
		    
		    return convertMongoUserToUserDetailsDto(mongoUser);
		} catch (Exception e) {
		    log.error("Error finding user with userId={}: {}", userId, e.getMessage());
		    throw new IllegalArgumentException("User not found");
		}
    }

    private UserDetailsDto convertToUserDetailsDto(UserEntity user) {
    	//Role role = roleRepository.findById(user.getRoleId()).get();
        UserDetailsDto userDetailsDto = new UserDetailsDto();
        userDetailsDto.setId(user.getId());
        userDetailsDto.setCreatedAt(user.getCreatedAt());
        userDetailsDto.setFirstName(user.getFirstName());
        userDetailsDto.setLastName(user.getLastName());
        userDetailsDto.setEmail(user.getEmail());
        userDetailsDto.setMobileNumber(user.getMobileNumber());
        userDetailsDto.setRole(user.getRole().getRoleName());
        userDetailsDto.setOnBoardStatus(user.getAccountEntity().getOnBoardStatus());
        userDetailsDto.setAccountId(user.getAccountEntity().getId());
		userDetailsDto.setProfilePicture(user.getProfilePicture());
		userDetailsDto.setSlipBox(user.getSlipBox());
		userDetailsDto.setIsTwoFactorEnabled(user.getIsTwoFactorEnabled());
		userDetailsDto.setIsOtpRequired(user.getIsOtpRequired());
		userDetailsDto.setExpiryAt(user.getExpiryAt());
        
        return userDetailsDto;
    }


//    @Transactional
//    public Response<UserDetailsDto> updateUser(Long userId, UpdateUsersDTO updateRequest) {
//        log.debug("Updating user id={}", userId);
//        
//        UserEntity user = userRepository.findById(userId)
//            .orElseThrow(() -> new IllegalArgumentException("User not found"));
//
//		if (updateRequest.getFirstName() != null) {
//			user.setFirstName(updateRequest.getFirstName());
//		}
//		if (updateRequest.getLastName() != null) {
//			user.setLastName(updateRequest.getLastName());
//		}
//		if (updateRequest.getEmail() != null) {
//			user.setEmail(updateRequest.getEmail());
//		}
//		if (updateRequest.getMobileNumber() != null) {
//			user.setMobileNumber(updateRequest.getMobileNumber());
//		}
//		if (updateRequest.getSlipBox() != null) {
//	        user.setSlipBox(updateRequest.getSlipBox());
//	    }
//		userRepository.save(user);
//		UserDetailsDto userDetailsDto = convertToUserDetailsDto(user);
//
//		MongoUser mongoUser = new MongoUser();
//		mongoUser.setId(user.getId().toString());
//		mongoUser.setUsername(user.getFirstName() + " " + user.getLastName());
//		mongoUser.setEmail(user.getEmail());
//		mongoUser.setFirstName(user.getFirstName());
//		mongoUser.setLastName(user.getLastName());
//		mongoUser.setMobileNumber(user.getMobileNumber());
//		mongoUser.setIsActive(user.getIsActive());
//		mongoUser.setProfilePicture(user.getProfilePicture());
//		mongoUser.setSlipBox(user.getSlipBox()); 
//		mongoUserRepository.save(mongoUser);
//
//	    //UserDetailsDto userDetailsDto = convertToUserDetailsDto(user);
//		Response<UserDetailsDto> response = new Response<>();
//		response.setMessage("User updated successfully");
//		response.setSuccess(true);
//		response.setData(userDetailsDto);
//
//		return response;
//		
//        if (updateRequest.getFirstName() != null) {
//            user.setFirstName(updateRequest.getFirstName());
//        }
//        if (updateRequest.getLastName() != null) {
//            user.setLastName(updateRequest.getLastName());
//        }
//        if (updateRequest.getEmail() != null) {
//            user.setEmail(updateRequest.getEmail());
//        }
//        if (updateRequest.getMobileNumber() != null) {
//            user.setMobileNumber(updateRequest.getMobileNumber());
//        }
//        
//        // Save to PostgreSQL and MongoDB with dual-write pattern
//        try {
//            UserEntity savedUser = userRepository.save(user);
//            try {
//                // Find existing MongoDB document to preserve the MongoDB _id
//                MongoUser existingMongoUser = mongoUserRepository.findFirstByUserId(userId);
//                if (existingMongoUser != null) {
//                    // Update existing document
//                    existingMongoUser.setFirstName(savedUser.getFirstName());
//                    existingMongoUser.setLastName(savedUser.getLastName());
//                    existingMongoUser.setEmail(savedUser.getEmail());
//                    existingMongoUser.setMobileNumber(savedUser.getMobileNumber());
//                    existingMongoUser.setUpdatedAt(savedUser.getUpdatedAt());
//                    existingMongoUser.setUpdatedBy(savedUser.getUpdatedBy());
//                    // Update computed username field
//                    existingMongoUser.setUsername((savedUser.getFirstName() != null ? savedUser.getFirstName() : "") + " " + 
//                                                 (savedUser.getLastName() != null ? savedUser.getLastName() : ""));
//                    mongoUserRepository.save(existingMongoUser);
//                    log.info("Successfully updated existing user in MongoDB: id={}, mongoId={}, name={} {}", 
//                             userId, existingMongoUser.getId(), savedUser.getFirstName(), savedUser.getLastName());
//                } else {
//                    // Create new document if none exists
//                    MongoUser mongoUser = new MongoUser(savedUser);
//                    mongoUserRepository.save(mongoUser);
//                    log.info("Created new user in MongoDB: id={}, name={} {}", userId, savedUser.getFirstName(), savedUser.getLastName());
//                }
//            } catch (Exception mongoEx) {
//                log.error("Failed to update user in MongoDB: id={}, name={} {}", userId, savedUser.getFirstName(), savedUser.getLastName(), mongoEx);
//                throw new RuntimeException("MongoDB update failed, triggering rollback", mongoEx);
//            }
//            
//            UserDetailsDto userDetailsDto = convertToUserDetailsDto(savedUser);
//            Response<UserDetailsDto> response = new Response<>();
//            response.setMessage("User updated successfully");
//            response.setSuccess(true);
//            response.setData(userDetailsDto);
//            return response;
//            
//        } catch (Exception ex) {
//            log.error("Failed to update user: id={}", userId, ex);
//            throw new RuntimeException("User update failed", ex);
//        }
//
//  }
    @Transactional
    public Response<UserDetailsDto> updateUser(Long userId, UpdateUsersDTO updateRequest) {
        log.debug("Updating user id={}", userId);
        
        UserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (updateRequest.getFirstName() != null) {
            user.setFirstName(updateRequest.getFirstName());
        }
        if (updateRequest.getLastName() != null) {
            user.setLastName(updateRequest.getLastName());
        }
        if (updateRequest.getEmail() != null) {
            user.setEmail(updateRequest.getEmail());
        }
        if (updateRequest.getMobileNumber() != null) {
            user.setMobileNumber(updateRequest.getMobileNumber());
        }
//        if (updateRequest.getSlipBox() != null) {
//        	user.setSlipBox(updateRequest.getSlipBox());
//        }
        if (updateRequest.getSlipBox() != null) {
            user.setSlipBox(updateRequest.getSlipBox());
        } else {
            user.setSlipBox(true); 
        }
        if (updateRequest.getExpiryAt() != null) {
            user.setExpiryAt(updateRequest.getExpiryAt());
        }
        
        // Save to PostgreSQL and MongoDB with dual-write pattern
        try {
            UserEntity savedUser = userRepository.save(user);
            try {
                // Find existing MongoDB document to preserve the MongoDB _id
                MongoUser existingMongoUser = mongoUserRepository.findFirstByUserId(userId);
                if (existingMongoUser != null) {
                    // Update existing document
                    existingMongoUser.setFirstName(savedUser.getFirstName());
                    existingMongoUser.setLastName(savedUser.getLastName());
                    existingMongoUser.setEmail(savedUser.getEmail());
                    existingMongoUser.setMobileNumber(savedUser.getMobileNumber());
                    existingMongoUser.setSlipBox(savedUser.getSlipBox());
                    existingMongoUser.setExpiryAt(savedUser.getExpiryAt());
                    existingMongoUser.setUpdatedAt(savedUser.getUpdatedAt());
                    existingMongoUser.setUpdatedBy(savedUser.getUpdatedBy());
                    // Update computed username field
                    existingMongoUser.setUsername((savedUser.getFirstName() != null ? savedUser.getFirstName() : "") + " " + 
                                                 (savedUser.getLastName() != null ? savedUser.getLastName() : ""));
                    mongoUserRepository.save(existingMongoUser);
                    log.info("Successfully updated existing user in MongoDB: id={}, mongoId={}, name={} {}", 
                             userId, existingMongoUser.getId(), savedUser.getFirstName(), savedUser.getLastName());
                } else {
                    // Create new document if none exists
                    MongoUser mongoUser = new MongoUser(savedUser);
                    mongoUserRepository.save(mongoUser);
                    log.info("Created new user in MongoDB: id={}, name={} {}", userId, savedUser.getFirstName(), savedUser.getLastName());
                }
            } catch (Exception mongoEx) {
                log.error("Failed to update user in MongoDB: id={}, name={} {}", userId, savedUser.getFirstName(), savedUser.getLastName(), mongoEx);
                throw new RuntimeException("MongoDB update failed, triggering rollback", mongoEx);
            }
            
            UserDetailsDto userDetailsDto = convertToUserDetailsDto(savedUser);
            Response<UserDetailsDto> response = new Response<>();
            response.setMessage("User updated successfully");
            response.setSuccess(true);
            response.setData(userDetailsDto);
            return response;
            
        } catch (Exception ex) {
            log.error("Failed to update user: id={}", userId, ex);
            throw new RuntimeException("User update failed", ex);
        }
  }

 

//  @Transactional
//  public Response<Void> deactivateUser(Long userId) {
//      log.debug("Deactivating user id={}", userId);
//      
//      UserEntity user = userRepository.findById(userId)
//          .orElseThrow(() -> new IllegalArgumentException("User not found or cannot be deactivated"));
//
//      // Set the user's status to inactive
//      user.setIsActive(false);
//
//      // Save to PostgreSQL and MongoDB with dual-write pattern
//      try {
//          UserEntity savedUser = userRepository.save(user);
//          try {
//              // Find existing MongoDB document to preserve the MongoDB _id
//              MongoUser existingMongoUser = mongoUserRepository.findFirstByUserId(userId);
//              if (existingMongoUser != null) {
//                  // Update existing document
//                  existingMongoUser.setIsActive(savedUser.getIsActive());
//                  existingMongoUser.setUpdatedAt(savedUser.getUpdatedAt());
//                  existingMongoUser.setUpdatedBy(savedUser.getUpdatedBy());
//                  mongoUserRepository.save(existingMongoUser);
//                  log.info("Successfully deactivated existing user in MongoDB: id={}, mongoId={}", 
//                           userId, existingMongoUser.getId());
//              } else {
//                  // Create new document if none exists
//                  MongoUser mongoUser = new MongoUser(savedUser);
//                  mongoUserRepository.save(mongoUser);
//                  log.info("Created new deactivated user in MongoDB: id={}", userId);
//              }
//          } catch (Exception mongoEx) {
//              log.error("Failed to deactivate user in MongoDB: id={}", userId, mongoEx);
//              throw new RuntimeException("MongoDB update failed, triggering rollback", mongoEx);
//          }
//          
//          Response<Void> response = new Response<>();
//          response.setMessage("User deactivated successfully");
//          response.setSuccess(true);
//          return response;
//          
//      } catch (Exception ex) {
//          log.error("Failed to deactivate user: id={}", userId, ex);
//          throw new RuntimeException("User deactivation failed", ex);
//      }
//    }
    @Transactional
    public Response<Void> deactivateUser(Long userId) {
        log.debug("Deactivating user id={}", userId);
        
        UserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found or cannot be deactivated"));

        // Skip if already inactive
        if (!user.getIsActive()) {
            Response<Void> response = new Response<>();
            response.setMessage("User is already inactive");
            response.setSuccess(true);
            return response;
        }

    // Set the user's status to inactive and clear expiryAt (optional)
    user.setIsActive(false);
    user.setExpiryAt(null); // Optional: Clear expiryAt to indicate deactivation
    user.setUpdatedAt(LocalDateTime.now());
    user.setUpdatedBy("System");

        // Save to PostgreSQL and MongoDB with dual-write pattern
        try {
            UserEntity savedUser = userRepository.save(user);
            try {
                MongoUser existingMongoUser = mongoUserRepository.findFirstByUserId(userId);
                if (existingMongoUser != null) {
                    existingMongoUser.setIsActive(savedUser.getIsActive());
                    existingMongoUser.setExpiryAt(savedUser.getExpiryAt());
                    existingMongoUser.setUpdatedAt(savedUser.getUpdatedAt());
                    existingMongoUser.setUpdatedBy(savedUser.getUpdatedBy());
                    mongoUserRepository.save(existingMongoUser);
                    log.info("Successfully deactivated existing user in MongoDB: id={}, mongoId={}", 
                            userId, existingMongoUser.getId());
                } else {
                    MongoUser mongoUser = new MongoUser(savedUser);
                    mongoUserRepository.save(mongoUser);
                    log.info("Created new deactivated user in MongoDB: id={}", userId);
                }
            } catch (Exception mongoEx) {
                log.error("Failed to deactivate user in MongoDB: id={}", userId, mongoEx);
                throw new RuntimeException("MongoDB update failed, triggering rollback", mongoEx);
            }
            
            // --- Option 1 Cascade: first-level children ---
            List<Long> childIds = new ArrayList<>();
            try {
                // Collect direct user children by createdBy (email match)
                if (user.getEmail() != null) {
                    List<UserEntity> createdChildren = userRepository.findByCreatedBy(user.getEmail());
                    for (UserEntity c : createdChildren) {
                        if (Boolean.TRUE.equals(c.getIsActive()) || c.getIsActive() == null) {
                            childIds.add(c.getId());
                        }
                    }
                }
                // Collect volunteer-linked user accounts
                List<VolunteerEntity> adminVolunteers = volunteerRepository.findByAdminUserId(userId);
                for (VolunteerEntity v : adminVolunteers) {
                    UserEntity vu = v.getUserEntity();
                    if (vu != null && (Boolean.TRUE.equals(vu.getIsActive()) || vu.getIsActive() == null) && !vu.getId().equals(userId)) {
                        childIds.add(vu.getId());
                    }
                }
                // Deduplicate
                if (!childIds.isEmpty()) {
                    childIds = childIds.stream().distinct().collect(java.util.stream.Collectors.toList());
                    int updated = userRepository.bulkDeactivateByIds(childIds, LocalDateTime.now(), "CascadeDeactivation");
                    log.info("Cascade deactivation: parent={}, childrenCandidates={}, updatedChildren={}", userId, childIds.size(), updated);
                    // Mirror to Mongo
                    for (Long cid : childIds) {
                        try {
                            MongoUser mu = mongoUserRepository.findFirstByUserId(cid);
                            if (mu != null && Boolean.TRUE.equals(mu.getIsActive())) {
                                mu.setIsActive(false);
                                mu.setUpdatedAt(LocalDateTime.now());
                                mu.setUpdatedBy("CascadeDeactivation");
                                mongoUserRepository.save(mu);
                            }
                        } catch (Exception me) {
                            log.error("Failed to cascade deactivate Mongo user id={}", cid, me);
                        }
                    }
                }
            } catch (Exception cascadeEx) {
                // Log but do not fail the primary deactivation.
                log.error("Cascade deactivation encountered an error for parent id={}: {}", userId, cascadeEx.getMessage(), cascadeEx);
            }

            Response<Void> response = new Response<>();
            response.setMessage("User deactivated successfully (cascade applied to " + childIds.size() + " children)");
            response.setSuccess(true);
            return response;
            
        } catch (Exception ex) {
            log.error("Failed to deactivate user: id={}", userId, ex);
            throw new RuntimeException("User deactivation failed", ex);
        }
    }
    
	
//  public ThedalResponse<String> uploadProfilePicture(MultipartFile multipartFile) {
//	    // Retrieve the current account ID using RequestDetailsService
//	    Long accountId = requestDetails.getCurrentAccountId();
//	    if (accountId == null) {
//	        log.error("Account id not found, unauthorized access.");
//	        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
//	    }
//
//	    log.info("Uploading profile picture for account ID: {}", accountId);
//
//	    String awsUrl = imageUpload.uploadProfileImageToAWS(multipartFile);
//
//	    // Find or create the user associated with the current account
//	    UserEntity user = userRepository.findByAccountEntityId(accountId).orElseGet(() -> {
//	        UserEntity newProfile = new UserEntity();
//	        newProfile.setAccountEntity(new AccountEntity(accountId, null)); // Link the account entity
//	        // newProfile.setRole(defaultRole); // Ensure a default role is set, if applicable
//	        return newProfile;
//	    });
//
//	    // Update the user's profile picture
//	    user.setProfilePicture(awsUrl);
//	    user.setProfilePictureName(multipartFile.getOriginalFilename()); // Store the name separately
//	    user.setUpdatedAt(LocalDateTime.now());
//	    userRepository.save(user); // Save the updated user to the database
//
//	    // Prepare the response
//	    log.info("Profile picture uploaded successfully for account ID: {}", accountId);
//	    return new ThedalResponse<>(ThedalSuccess.PROFILE_PICTURE_UPLOADED, awsUrl);
//	}
  
  
  public ThedalResponse<String> uploadProfilePicture(MultipartFile multipartFile) {
	    // Retrieve the current user ID using RequestDetailsService
	    Long userId = requestDetails.getCurrentUserFromRequest().getId();
	    if (userId == null) {
	        log.error("user id not found, unauthorized access.");
	        throw new ThedalException(ThedalError.USER_NOT_FOUND, HttpStatus.UNAUTHORIZED);
	    }

	    log.info("Uploading profile picture for user ID: {}", userId);

	    String awsUrl = imageUpload.uploadProfileImageToAWS(multipartFile);

	    // Find or create the user associated with the current account
	    UserEntity user = userRepository.findById(userId).orElseThrow(()-> new ThedalException(ThedalError.USER_NOT_FOUND, HttpStatus.UNAUTHORIZED));

	    // Update the user's profile picture
	    user.setProfilePicture(awsUrl);
	    user.setProfilePictureName(multipartFile.getOriginalFilename()); // Store the name separately
	    user.setUpdatedAt(LocalDateTime.now());
	    userRepository.save(user); // Save the updated user to the database

	    // Sync to MongoDB
	    // Find existing MongoDB document to preserve the MongoDB _id
	    MongoUser existingMongoUser = mongoUserRepository.findFirstByUserId(userId);
	    if (existingMongoUser != null) {
	        // Update existing document
	        existingMongoUser.setProfilePicture(user.getProfilePicture());
	        existingMongoUser.setProfilePictureName(user.getProfilePictureName());
	        existingMongoUser.setUpdatedAt(user.getUpdatedAt());
	        mongoUserRepository.save(existingMongoUser);
	        log.info("Successfully updated profile picture in MongoDB for user ID: {}, mongoId: {}", userId, existingMongoUser.getId());
	    } else {
	        // Create new document if none exists
	        MongoUser mongoUser = new MongoUser(user);
	        mongoUserRepository.save(mongoUser);
	        log.info("Created new user in MongoDB with profile picture for user ID: {}", userId);
	    }

	    // Prepare the response
	    log.info("Profile picture uploaded successfully for user ID: {}", userId);
	    return new ThedalResponse<>(ThedalSuccess.PROFILE_PICTURE_UPLOADED, awsUrl);
	}

	// public List<UserListDto> getAllSuperAppAdminUsers() {
	// 	return userRepository.findUsersForSuperAdmin();
	// }

//  public Page<UserListDto> getAllUsers(int page, int size, String sortBy, String sortDirection, Boolean isActive,
//		   String firstName, String lastName) {
//	  Sort sort = Sort.unsorted();
//      if (sortBy != null && !sortBy.isEmpty()) {
//          Sort.Direction direction = sortDirection.equalsIgnoreCase("desc") 
//              ? Sort.Direction.DESC 
//              : Sort.Direction.ASC;
//
//          // Split sortBy into individual fields (e.g., "firstName,lastName" -> ["firstName", "lastName"])
//          List<String> sortFields = Arrays.stream(sortBy.split(","))
//              .map(String::trim)
//              .map(String::toLowerCase)
//              .collect(Collectors.toList());
//
//          // Create Sort.Order for each valid field
//          List<Sort.Order> orders = sortFields.stream()
//              .map(field -> {
//                  switch (field) {
//                      case "createdat":
//                          return new Sort.Order(direction, "createdAt");
//                      case "firstname":
//                          return new Sort.Order(direction, "firstName");
//                      case "lastname":
//                          return new Sort.Order(direction, "lastName");
//                      default:
//                          return null; // Ignore invalid fields
//                  }
//              })
//              .filter(order -> order != null)
//              .collect(Collectors.toList());
//
//          if (!orders.isEmpty()) {
//              sort = Sort.by(orders);
//          }
//      }
//
//      Pageable pageable = PageRequest.of(page, size, sort);
//      
//      Page<UserEntity> userPage;
////      if (isActive != null) {
////          userPage = userRepository.findByIsActive(isActive, pageable);
////      } else {
////          userPage = userRepository.findAll(pageable);
////      }
//   // Apply filters based on provided parameters
//      if (isActive != null && firstName != null && lastName != null) {
//          userPage = userRepository.findByIsActiveAndFirstNameContainingIgnoreCaseAndLastNameContainingIgnoreCase(
//                  isActive, firstName, lastName, pageable);
//      } else if (isActive != null && firstName != null) {
//          userPage = userRepository.findByIsActiveAndFirstNameContainingIgnoreCase(
//                  isActive, firstName, pageable);
//      } else if (isActive != null && lastName != null) {
//          userPage = userRepository.findByIsActiveAndLastNameContainingIgnoreCase(
//                  isActive, lastName, pageable);
//      } else if (firstName != null && lastName != null) {
//          userPage = userRepository.findByFirstNameContainingIgnoreCaseAndLastNameContainingIgnoreCase(
//                  firstName, lastName, pageable);
//      } else if (firstName != null) {
//          userPage = userRepository.findByFirstNameContainingIgnoreCase(firstName, pageable);
//      } else if (lastName != null) {
//          userPage = userRepository.findByLastNameContainingIgnoreCase(lastName, pageable);
//      } else if (isActive != null) {
//          userPage = userRepository.findByIsActive(isActive, pageable);
//      } else {
//          userPage = userRepository.findAll(pageable);
//      }
//
//      return userPage.map(user -> new UserListDto(
//          user.getId(),
//          user.getCreatedAt(),
//          user.getFirstName(),
//          user.getLastName(),
//          user.getEmail(),
//          user.getMobileNumber(),
//          user.getProfile() != null ? user.getProfile().getSubscription() : null,
//          user.getIsActive(),
//          user.getRole() != null ? user.getRole().getRoleName() : null
//      ));
//  }
  
  public Page<UserListDto> getAllUsers(int page, int size, String sortBy, String sortDirection, Boolean isActive, String name) {
	  log.info("Received request with name: '{}', isActive: {}", name, isActive);
	  
	  // Read from MongoDB for better performance
	  return getAllUsersFromMongo(page, size, sortBy, sortDirection, isActive, name, null, null);
  }
  
	public void updateUserLogin(Long userId) {
        LocalDateTime now = LocalDateTime.now();

        UserActivity userActivity = userActivityRepository.findByUserId(userId)
            .orElse(new UserActivity(userId, now, 0));

        userActivity.setLastLogin(now);
        userActivity.setLoginCount(userActivity.getLoginCount() + 1);

        userActivityRepository.save(userActivity);
    }
	
	public UserActivityDto getUserActivity(Long userId) {
			Optional<UserActivity> userActivityOpt = userActivityRepository.findByUserId(userId);
			if (userActivityOpt.isEmpty()) {
				throw new IllegalArgumentException("No activity found for user ID: " + userId);
			}
			UserActivity userActivity = userActivityOpt.get();
			return new UserActivityDto(userActivity.getLastLogin(), userActivity.getLoginCount());
		}




	// public String activateUser(Long userId) {
	// 		UserEntity user = userRepository.findById(userId)
	// 				.orElseThrow(() -> new RuntimeException("User not found"));
	
	// 		if (user.getIsActive()) {
	// 			return "User is already active";
	// 		}
	
	// 		user.setIsActive(true);
	// 		userRepository.save(user);
	// 		return "User activated successfully";
	// 	}
	@Transactional
public Response<Void> deleteUser(Long userId) {
    log.debug("Deleting user id={}", userId);
    
    UserEntity user = userRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("User not found"));
       
    // Delete from PostgreSQL with dual-delete pattern
    try {
        // Delete all related entities first
        userSessionRepository.deleteAllSessionsByUserId(userId);
        notificationsLogRepository.deleteByUserId(userId);
        volunteerRepository.deleteByUserEntityId(userId);
        mobileVerificationRepo.deleteByUserId(userId);
        userRepository.delete(user);
        
        // Delete from MongoDB
        try {
            mongoUserRepository.deleteByUserId(userId);
            log.info("Successfully deleted user from MongoDB: id={}", userId);
        } catch (Exception mongoEx) {
            log.error("Failed to delete user from MongoDB: id={}", userId, mongoEx);
            throw new RuntimeException("MongoDB delete failed, triggering rollback", mongoEx);
        }
        
        Response<Void> response = new Response<>();
        response.setMessage("User deleted successfully");
        response.setSuccess(true);
        return response;
        
    } catch (Exception ex) {
        log.error("Failed to delete user: id={}", userId, ex);
        throw new RuntimeException("User deletion failed", ex);
    }
}

public MongoUser getUserFromMongo(String username) {
    return mongoUserRepository.findByUsername(username);
}

// Bulk migration method
public void migrateAllUsersToMongo() {
    log.info("Starting migration of all users from PostgreSQL to MongoDB");
    
    List<UserEntity> allUsers = userRepository.findAll();
    log.info("Found {} users to migrate", allUsers.size());
    
    List<MongoUser> mongoUsers = allUsers.stream()
        .map(user -> new MongoUser(user))
        .collect(Collectors.toList());
    
    mongoUserRepository.saveAll(mongoUsers);
    log.info("Successfully migrated {} users to MongoDB", mongoUsers.size());
}

/**
 * Get migration status - count of users in PostgreSQL vs MongoDB
 */
public Response<String> getMigrationStatus() {
    long postgresCount = userRepository.count();
    long mongoCount = mongoUserRepository.count();
    
    String status = String.format("PostgreSQL Users: %d, MongoDB Users: %d", 
                                 postgresCount, mongoCount);
    
    Response<String> response = new Response<>();
    response.setMessage("Migration status retrieved successfully");
    response.setSuccess(true);
    response.setData(status);
    
    return response;
}

public MongoUser getMongoUserById(String userId) {
    return mongoUserRepository.findById(userId).orElse(null);
}
public List<MongoUser> getAllMongoUsers() {
    return mongoUserRepository.findAll();
}
public UserActivityDto getMongoUserActivity(String userId) {
    MongoUser mongoUser = mongoUserRepository.findByUserId(Long.valueOf(userId)).orElse(null);
    if (mongoUser == null) return null;
    UserEntity user = userRepository.findById(Long.valueOf(userId)).orElse(null);
    if (user == null) return null;
    return getUserActivity(user.getId());
}

public Page<MongoUser> getAllMongoUsers(int page, int size, String sortBy, String sortDirection, Boolean isActive, String name
		, String username, String mobileNumber) {
    Sort sort = Sort.unsorted();
    if (sortBy != null && !sortBy.isEmpty()) {
        Sort.Direction direction = sortDirection.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        sort = Sort.by(direction, sortBy);
    }
    
    Pageable pageable = PageRequest.of(page, size, sort);
    if (username != null && !username.trim().isEmpty()) {
        if (isActive != null) {
            return mongoUserRepository.findByUsernameContainingIgnoreCaseAndIsActive(username, isActive, pageable);
        } else {
            return mongoUserRepository.findByUsernameContainingIgnoreCase(username, pageable);
        }
    }

    if (mobileNumber != null && !mobileNumber.trim().isEmpty()) {
        return mongoUserRepository.findByMobileNumberContainingIgnoreCase(mobileNumber, pageable);
    }

    
    if (name != null && !name.trim().isEmpty() && isActive != null) {
        return mongoUserRepository.findByUsernameContainingIgnoreCaseAndIsActive(name, isActive, pageable);
    } else if (name != null && !name.trim().isEmpty()) {
        return mongoUserRepository.findByUsernameContainingIgnoreCase(name, pageable);
    } else if (isActive != null) {
        return mongoUserRepository.findByIsActive(isActive, pageable);
    } else {
        return mongoUserRepository.findAll(pageable);
    }
}

/**
 * Convert MongoUser to UserDetailsDto for compatibility with existing APIs
 */
private UserDetailsDto convertMongoUserToUserDetailsDto(MongoUser mongoUser) {
    if (mongoUser == null) {
        return null;
    }
    
    UserDetailsDto dto = new UserDetailsDto();
    dto.setId(mongoUser.getUserId()); // Use userId, not the MongoDB _id
    dto.setFirstName(mongoUser.getFirstName());
    dto.setLastName(mongoUser.getLastName());
    dto.setEmail(mongoUser.getEmail());
    dto.setMobileNumber(mongoUser.getMobileNumber());
    dto.setProfilePicture(mongoUser.getProfilePicture());
    dto.setAccountId(mongoUser.getAccountId());
    dto.setCreatedAt(mongoUser.getCreatedAt());
    dto.setRole(mongoUser.getRoleName());
    dto.setSlipBox(mongoUser.getSlipBox());
    dto.setExpiryAt(mongoUser.getExpiryAt());
    dto.setOnBoardStatus(null); // This might not be available in MongoUser, would need to be added if needed
    
    return dto;
}

/**
 * Convert MongoUser to UserListDto for compatibility with existing APIs
 */
private UserListDto convertMongoUserToUserListDto(MongoUser mongoUser) {
    if (mongoUser == null) {
        return null;
    }
    
    UserListDto dto = new UserListDto();
    dto.setUserId(mongoUser.getUserId()); // Use userId, not the MongoDB _id
    dto.setFirstName(mongoUser.getFirstName());
    dto.setLastName(mongoUser.getLastName());
    dto.setEmail(mongoUser.getEmail());
    dto.setMobileNumber(mongoUser.getMobileNumber());
    dto.setIsActive(mongoUser.getIsActive());

    dto.setSlipBox(mongoUser.getSlipBox());
    // Note: MongoDB doesn't have all fields, setting defaults
    dto.setCreatedAt(null); // Not available in MongoDB
    dto.setSubscription(null); // Not available in MongoDB
    dto.setRoleName(null); // Not available in MongoDB

    dto.setCreatedAt(mongoUser.getCreatedAt());
    dto.setSubscription(null); // This would need to be added to MongoUser if needed
    dto.setRoleName(mongoUser.getRoleName());
    dto.setSlipBox(mongoUser.getSlipBox());
    
    return dto;
}

/**
 * Get user by ID from MongoDB with conversion to PostgreSQL DTO format
 */
public UserDetailsDto getUserByIdFromMongo(Long userId) {
    try {
        // Read from PostgreSQL instead of MongoDB
        Optional<UserEntity> userEntity = userRepository.findById(userId);
        if (!userEntity.isPresent()) {
            return null;
        }
        
        return convertToUserDetailsDto(userEntity.get());
    } catch (Exception e) {
        log.error("Error finding user with userId={}: {}", userId, e.getMessage());
        return null;
    }
}

/**
 * Get all users from MongoDB with conversion to PostgreSQL DTO format
 */
public Page<UserListDto> getAllUsersFromMongo(int page, int size, String sortBy,
		String sortDirection, Boolean isActive, String name,
		String username, String mobileNumber) {
    // Read from PostgreSQL instead of MongoDB - simplified version
    Sort sort = Sort.unsorted();
    if (sortBy != null && !sortBy.isEmpty()) {
        Sort.Direction direction = sortDirection.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        sort = Sort.by(direction, sortBy);
    }
    
    Pageable pageable = PageRequest.of(page, size, sort);
    Page<UserEntity> userPage;
    
//    if (name != null && !name.trim().isEmpty()) {
//        if (isActive != null) {
//            userPage = userRepository.findByIsActiveAndNameContainingIgnoreCase(isActive, name, pageable);
//        } else {
//            userPage = userRepository.findByNameContainingIgnoreCase(name, pageable);
//        }
//    } else if (isActive != null) {
//        userPage = userRepository.findByIsActive(isActive, pageable);
//    } else {
//        userPage = userRepository.findAll(pageable);
//    }
    if (mobileNumber != null && !mobileNumber.trim().isEmpty()) {
        if (name != null && !name.trim().isEmpty()) {
            if (isActive != null) {
                userPage = userRepository.findByIsActiveAndFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseAndMobileNumberContaining(
                    isActive, name, name, mobileNumber, pageable);
            } else {
                userPage = userRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseAndMobileNumberContaining(
                    name, name, mobileNumber, pageable);
            }
        } else if (isActive != null) {
            userPage = userRepository.findByIsActiveAndMobileNumberContaining(
                isActive, mobileNumber, pageable);
        } else {
            userPage = userRepository.findByMobileNumberContaining(mobileNumber, pageable);
        }
    } else if (name != null && !name.trim().isEmpty()) {
        if (isActive != null) {
            userPage = userRepository.findByIsActiveAndFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
                isActive, name, name, pageable);
        } else {
            userPage = userRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
                name, name, pageable);
        }
    } else if (isActive != null) {
        userPage = userRepository.findByIsActive(isActive, pageable);
    } else {
        userPage = userRepository.findAll(pageable);
    }
    
    return userPage.map(user -> new UserListDto(
        user.getId(),
        user.getCreatedAt(),
        user.getFirstName(),
        user.getLastName(),
        user.getEmail(),
        user.getMobileNumber(),
        user.getProfile() != null ? user.getProfile().getSubscription() : null,
        user.getIsActive(),
        user.getRole() != null ? user.getRole().getRoleName() : null,
        //false // slipBox - assuming false as default
        user.getSlipBox() != null ? user.getSlipBox() : false,
        user.getIsTwoFactorEnabled() != null ? user.getIsTwoFactorEnabled() : false,
        user.getIsOtpRequired() != null ? user.getIsOtpRequired() : false,
        user.getExpiryAt()

    ));
}

////////////////////////////////////

//// New method for consolidated filter
//public Page<UserListDto> getAllUsersWithFilterFromMongo(int page, int size, String sortBy,
//        String sortDirection, Boolean isActive, String filter) {
//    Page<MongoUser> mongoUsers = getAllMongoUsersWithFilter(page, size, sortBy, sortDirection, isActive, filter);
//    List<UserListDto> userListDtos = mongoUsers.getContent().stream()
//            .map(this::convertMongoUserToUserListDto)
//            .collect(Collectors.toList());
//    return new PageImpl<>(userListDtos, mongoUsers.getPageable(), mongoUsers.getTotalElements());
//}
//
//// New method to handle filter logic
//public Page<MongoUser> getAllMongoUsersWithFilter(int page, int size, String sortBy, String sortDirection,
//        Boolean isActive, String filter) {
//    Sort sort = Sort.unsorted();
//    if (sortBy != null && !sortBy.isEmpty()) {
//        Sort.Direction direction = sortDirection.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
//        sort = Sort.by(direction, sortBy);
//    }
//    Pageable pageable = PageRequest.of(page, size, sort);
//
//    if (filter != null && !filter.trim().isEmpty()) {
//        // Try matching as username
//        if (isActive != null) {
//            Page<MongoUser> users = mongoUserRepository.findByUsernameContainingIgnoreCaseAndIsActive(filter, isActive, pageable);
//            if (!users.isEmpty()) {
//                return users;
//            }
//        } else {
//            Page<MongoUser> users = mongoUserRepository.findByUsernameContainingIgnoreCase(filter, pageable);
//            if (!users.isEmpty()) {
//                return users;
//            }
//        }
//
//        // Try matching as mobileNumber
//        Page<MongoUser> users = mongoUserRepository.findByMobileNumberContainingIgnoreCase(filter, pageable);
//        if (!users.isEmpty()) {
//            return users;
//        }
//
//        // Try matching as firstName or lastName
//        if (isActive != null) {
//            return mongoUserRepository.findByFirstNameOrLastNameContainingIgnoreCaseAndIsActive(filter, isActive, pageable);
//        } else {
//            return mongoUserRepository.findByFirstNameOrLastNameContainingIgnoreCase(filter, pageable);
//        }
//    }
//
//    if (isActive != null) {
//        return mongoUserRepository.findByIsActive(isActive, pageable);
//    }
//
//    return mongoUserRepository.findAll(pageable);
//}


/**
     * Clean up duplicate users in MongoDB - keep only the most recent one for each userId
     */
    @Transactional
    public Response<String> cleanupDuplicateUsers() {
        log.info("Starting cleanup of duplicate users in MongoDB");
        
        try {
            List<MongoUser> allUsers = mongoUserRepository.findAll();
            Map<Long, List<MongoUser>> userGroups = allUsers.stream()
                .filter(user -> user.getUserId() != null)
                .collect(Collectors.groupingBy(MongoUser::getUserId));
            
            int duplicatesRemoved = 0;
            int totalDuplicateGroups = 0;
            
            for (Map.Entry<Long, List<MongoUser>> entry : userGroups.entrySet()) {
                List<MongoUser> duplicates = entry.getValue();
                if (duplicates.size() > 1) {
                    totalDuplicateGroups++;
                    log.info("Found {} duplicates for userId={}", duplicates.size(), entry.getKey());
                    
                    // Sort by creation time (keep the most recent) or by MongoDB ID as fallback
                    duplicates.sort((u1, u2) -> {
                        if (u1.getCreatedAt() != null && u2.getCreatedAt() != null) {
                            return u2.getCreatedAt().compareTo(u1.getCreatedAt()); // Most recent first
                        }
                        return u2.getId().compareTo(u1.getId()); // Fallback to MongoDB ID
                    });
                    
                    // Keep the first (most recent), delete the rest
                    for (int i = 1; i < duplicates.size(); i++) {
                        MongoUser toDelete = duplicates.get(i);
                        log.info("Deleting duplicate user: userId={}, mongoId={}", toDelete.getUserId(), toDelete.getId());
                        mongoUserRepository.delete(toDelete);
                        duplicatesRemoved++;
                    }
                }
            }
            
            String message = String.format("Cleanup completed. Removed %d duplicate users from %d groups.", 
                    duplicatesRemoved, totalDuplicateGroups);
            log.info(message);
            
            Response<String> response = new Response<>();
            response.setMessage(message);
            response.setSuccess(true);
            response.setData(message);
            return response;
            
        } catch (Exception e) {
            String errorMessage = "Failed to cleanup duplicate users: " + e.getMessage();
            log.error(errorMessage, e);
            
            Response<String> errorResponse = new Response<>();
            errorResponse.setMessage(errorMessage);
            errorResponse.setSuccess(false);
            return errorResponse;
        }
    }
    
//    @Transactional
//    public Response<?> initiateTwoFactorToggle(Long userId, Boolean isEnabled) {
//        log.debug("Initiating 2FA toggle for user id={}, enabled={}", userId, isEnabled);
//
//        UserEntity user = userRepository.findById(userId)
//                .orElseThrow(() -> new IllegalArgumentException("User not found"));
//        
//        log.info("User found: {} with mobile {} (verified: {})", 
//                user.getId(), user.getMobileNumber(), user.getIsMobileVerified());
//        
//        if (!user.getRole().getId().equals(SUPER_ADMIN_ROLE_ID)) {
//            log.warn("Non-SUPER_ADMIN user {} attempted 2FA toggle", userId);
//            throw new ThedalException(ThedalError.ACCESS_DENIED, HttpStatus.FORBIDDEN, 
//                "Only SUPER_ADMIN users can toggle 2FA");
//        }
//
//        if (isEnabled && !Boolean.TRUE.equals(user.getIsMobileVerified())) {
//            throw new IllegalArgumentException("Mobile number must be verified to enable 2FA");
//        }
//        
//        if (!Boolean.TRUE.equals(user.getIsMobileVerified()) || user.getMobileNumber() == null) {
//            log.error("Mobile not verified or missing for user {}", userId);
//            throw new IllegalArgumentException("Mobile number must be verified to perform this action");
//        }
//        
//        // Generate and send OTP
//        String otp = String.valueOf((int) ((Math.random() * 900000) + 100000)); // 6-digit OTP
//        
//        TwoFactorStatusChangeAttempt attempt = new TwoFactorStatusChangeAttempt();
//        attempt.setUserId(userId);
//        attempt.setOtp(otp);
//        attempt.setIsActive(true);
//        attempt.setCreatedAt(LocalDateTime.now());
//        attempt.setExpiresAt(LocalDateTime.now().plusMinutes(5));
//        attempt.setNewTwoFactorStatus(isEnabled);
//        
//        // Save the attempt
//        twoFactorStatusChangeAttemptRepository.save(attempt);
//        
//        // Send OTP to user's mobile
//        otpService.sendOtp(user.getMobileNumber(), otp);
//        
//        // Return OTP response
//        TwoFactorOtpResponse response = new TwoFactorOtpResponse();
//        response.setUserId(userId);
//        response.setMessage("OTP sent to registered mobile number");
//        
//        Response<TwoFactorOtpResponse> otpResponse = new Response<>();
//        otpResponse.setMessage("OTP required for 2FA status change");
//        otpResponse.setSuccess(true);
//        otpResponse.setData(response);
//        return otpResponse;
//    }
    @Transactional
    public Response<TwoFactorOtpResponse> initiateTwoFactorToggle(Long userId, Boolean isEnabled) {
        log.info("Initiating 2FA toggle for userId={}, enabled={}", userId, isEnabled);

        // Validate user existence
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found for userId={}", userId);
                    return new IllegalArgumentException("User not found");
                });

        log.info("User found: id={}, mobile={}, verified={}",
                user.getId(), user.getMobileNumber(), user.getIsMobileVerified());

        // Validate user permissions
        if (!user.getRole().getId().equals(SUPER_ADMIN_ROLE_ID)) {
            log.warn("Non-SUPER_ADMIN user {} attempted 2FA toggle", userId);
            throw new ThedalException(ThedalError.ACCESS_DENIED, HttpStatus.FORBIDDEN,
                    "Only SUPER_ADMIN users can toggle 2FA");
        }

        // Validate mobile verification
        if (isEnabled && !Boolean.TRUE.equals(user.getIsMobileVerified())) {
            log.error("Mobile number not verified for userId={}", userId);
            throw new IllegalArgumentException("Mobile number must be verified to enable 2FA");
        }

        if (!Boolean.TRUE.equals(user.getIsMobileVerified()) || user.getMobileNumber() == null) {
            log.error("Mobile not verified or missing for userId={}", userId);
            throw new IllegalArgumentException("Mobile number must be verified to perform this action");
        }

        // Check for existing attempt
        TwoFactorStatusChangeAttempt attempt = twoFactorStatusChangeAttemptRepository
                .findByUserId(userId)
                .orElse(new TwoFactorStatusChangeAttempt());

        // Update or initialize attempt
        attempt.setUserId(userId);
        attempt.setOtp(String.format("%06d", (int) (Math.random() * 900000 + 100000)));
        attempt.setIsActive(true);
        attempt.setCreatedAt(LocalDateTime.now());
        attempt.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        attempt.setNewTwoFactorStatus(isEnabled);

        // Save the attempt (updates if exists, creates if new)
        twoFactorStatusChangeAttemptRepository.save(attempt);
        log.info("Saved/Updated OTP attempt for userId={}", userId);

        // Send OTP to user's mobile
        try {
            otpService.sendOtp(user.getMobileNumber(), attempt.getOtp());
            log.info("OTP sent to mobile {} for userId={}", user.getMobileNumber(), userId);
        } catch (Exception e) {
            log.error("Failed to send OTP for userId={}: {}", userId, e.getMessage());
            throw new RuntimeException("Failed to send OTP. Please try again.");
        }

        // Prepare response
        TwoFactorOtpResponse otpResponseData = new TwoFactorOtpResponse();
        otpResponseData.setUserId(userId);
        otpResponseData.setMessage("OTP sent to registered mobile number");

        Response<TwoFactorOtpResponse> response = new Response<>();
        response.setMessage("OTP required for 2FA status change");
        response.setSuccess(true);
        response.setData(otpResponseData);

        return response;
    }


//    @Transactional
//    public Response<Void> verifyTwoFactorOtp(Long userId, String otp) {
//        log.debug("Verifying OTP for 2FA toggle: userId={}", userId);
//        try {
//            log.info("Querying OTP attempt for userId={}", userId);
//            TwoFactorStatusChangeAttempt attempt = twoFactorStatusChangeAttemptRepository
//                .findByUserIdAndIsActiveTrue(userId)
//                .orElseThrow(() -> new IllegalArgumentException("No active 2FA change request found"));
//            log.info("Found OTP attempt: id={}, otp={}, expiresAt={}", attempt.getId(), attempt.getOtp(), attempt.getExpiresAt());
//            log.info("Checking expiry: now={}, expiresAt={}", LocalDateTime.now(), attempt.getExpiresAt());
//            if (LocalDateTime.now().isAfter(attempt.getExpiresAt())) {
//                throw new IllegalArgumentException("OTP has expired");
//            }
//            log.info("Verifying OTP: stored={}, provided={}", attempt.getOtp(), otp);
//            if (!attempt.getOtp().equals(otp)) {
//                throw new IllegalArgumentException("Invalid OTP");
//            }
//            log.info("Retrieving user: userId={}", userId);
//            UserEntity user = userRepository.findById(userId)
//                .orElseThrow(() -> new IllegalArgumentException("User not found"));
//            log.info("Updating OTP attempt to inactive: id={}", attempt.getId());
//            attempt.setIsActive(false);
//            twoFactorStatusChangeAttemptRepository.save(attempt);
//            log.info("Toggling 2FA for userId={}", userId);
//            return performTwoFactorToggle(user, attempt.getNewTwoFactorStatus());
//        } catch (Exception e) {
//            log.error("Error verifying OTP for user {}: {}", userId, e.getMessage(), e);
//            throw e; // Rethrow to preserve original exception
//        }
//    }
//
//    private Response<Void> performTwoFactorToggle(UserEntity user, Boolean isEnabled) {
//        user.setIsTwoFactorEnabled(isEnabled);
//        user.setUpdatedAt(LocalDateTime.now());
//        user.setUpdatedBy("2FA Toggle");
//        
//        // Save to PostgreSQL
//        UserEntity savedUser = userRepository.save(user);
//        
//        // Update MongoDB
//        try {
//            MongoUser existingMongoUser = mongoUserRepository.findFirstByUserId(user.getId());
//            if (existingMongoUser != null) {
//                existingMongoUser.setIsTwoFactorEnabled(savedUser.getIsTwoFactorEnabled());
//                existingMongoUser.setUpdatedAt(savedUser.getUpdatedAt());
//                existingMongoUser.setUpdatedBy(savedUser.getUpdatedBy());
//                mongoUserRepository.save(existingMongoUser);
//            } else {
//                MongoUser mongoUser = new MongoUser(savedUser);
//                mongoUserRepository.save(mongoUser);
//            }
//        } catch (Exception mongoEx) {
//            log.error("Failed to update MongoDB for user: {}", user.getId(), mongoEx);
//            // Continue with response since PostgreSQL update succeeded
//        }
//        
//        Response<Void> response = new Response<>();
//        response.setMessage("2FA " + (isEnabled ? "enabled" : "disabled") + " successfully");
//        response.setSuccess(true);
//        return response;
//    }
    @Transactional
    public Response<Void> verifyTwoFactorOtp(Long userId, String otp) {
        log.info("Verifying OTP for 2FA toggle: userId={}", userId);

        // Fetch the attempt
        TwoFactorStatusChangeAttempt attempt = twoFactorStatusChangeAttemptRepository
                .findByUserId(userId)
                .orElseThrow(() -> {
                    log.error("No 2FA change request found for userId={}", userId);
                    return new IllegalArgumentException("No 2FA change request found");
                });

        // Check if attempt is active
        if (!attempt.getIsActive()) {
            log.error("No active 2FA change request found for userId={}", userId);
            throw new IllegalArgumentException("No active 2FA change request found");
        }

        // Check if OTP is expired
        if (LocalDateTime.now().isAfter(attempt.getExpiresAt())) {
            attempt.setIsActive(false);
            twoFactorStatusChangeAttemptRepository.save(attempt);
            log.error("OTP expired for userId={}", userId);
            throw new IllegalArgumentException("OTP has expired");
        }

        // Validate OTP
        if (!attempt.getOtp().equals(otp)) {
            log.error("Invalid OTP provided for userId={}. Provided: {}, Expected: {}", userId, otp, attempt.getOtp());
            throw new IllegalArgumentException("Invalid OTP");
        }

        // Fetch user
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found during OTP verification for userId={}", userId);
                    return new IllegalArgumentException("User not found");
                });

        // Deactivate the attempt
        attempt.setIsActive(false);
        twoFactorStatusChangeAttemptRepository.save(attempt);
        log.info("Deactivated OTP attempt for userId={}", userId);

        // Perform 2FA toggle
        return performTwoFactorToggle(user, attempt.getNewTwoFactorStatus());
    }

    private Response<Void> performTwoFactorToggle(UserEntity user, Boolean isEnabled) {
        log.info("Toggling 2FA for userId={} to enabled={}", user.getId(), isEnabled);

        user.setIsTwoFactorEnabled(isEnabled);
        user.setUpdatedAt(LocalDateTime.now());
        user.setUpdatedBy("SYSTEM_2FA_TOGGLE");

        // Save to PostgreSQL
        UserEntity savedUser = userRepository.save(user);
        log.info("Updated PostgreSQL user: id={}, isTwoFactorEnabled={}", savedUser.getId(), savedUser.getIsTwoFactorEnabled());

        // Update MongoDB
        try {
            MongoUser existingMongoUser = mongoUserRepository.findFirstByUserId(user.getId());
            if (existingMongoUser != null) {
                existingMongoUser.setIsTwoFactorEnabled(savedUser.getIsTwoFactorEnabled());
                existingMongoUser.setUpdatedAt(savedUser.getUpdatedAt());
                existingMongoUser.setUpdatedBy(savedUser.getUpdatedBy());
                mongoUserRepository.save(existingMongoUser);
                log.info("Updated MongoDB user: id={}", existingMongoUser.getUserId());
            } else {
                MongoUser mongoUser = new MongoUser(savedUser);
                mongoUserRepository.save(mongoUser);
                log.info("Created new MongoDB user: id={}", mongoUser.getUserId());
            }
        } catch (Exception mongoEx) {
            log.error("Failed to update MongoDB for userId={}: {}", user.getId(), mongoEx.getMessage());
            // Continue since PostgreSQL update succeeded
        }

        Response<Void> response = new Response<>();
        response.setMessage("2FA " + (isEnabled ? "enabled" : "disabled") + " successfully");
        response.setSuccess(true);
        return response;
    }
    
//    @Transactional
//    public Response<?> initiateOtpRequiredToggle(Long userId, Boolean isOtpRequired) {
//        log.debug("Initiating OTP requirement toggle for user id={}, enabled={}", userId, isOtpRequired);
//
//        UserEntity user = userRepository.findById(userId)
//                .orElseThrow(() -> new IllegalArgumentException("User not found"));
//
//        log.info("User found: {} with mobile {} (verified: {})",
//                user.getId(), user.getMobileNumber(), user.getIsMobileVerified());
//
//        if (isOtpRequired && !Boolean.TRUE.equals(user.getIsMobileVerified())) {
//            throw new IllegalArgumentException("Mobile number must be verified to enable OTP requirement");
//        }
//
//        if (!Boolean.TRUE.equals(user.getIsMobileVerified()) || user.getMobileNumber() == null) {
//            log.error("Mobile not verified or missing for user {}", userId);
//            throw new IllegalArgumentException("Mobile number must be verified to perform this action");
//        }
//
//        // Generate and send OTP
//        String otp = String.valueOf((int) ((Math.random() * 900000) + 100000)); // 6-digit OTP
//
//        OtpRequiredStatusChangeAttempt attempt = new OtpRequiredStatusChangeAttempt();
//        attempt.setUserId(userId);
//        attempt.setOtp(otp);
//        attempt.setIsActive(true);
//        attempt.setCreatedAt(LocalDateTime.now());
//        attempt.setExpiresAt(LocalDateTime.now().plusMinutes(5));
//        attempt.setNewOtpRequiredStatus(isOtpRequired);
//
//        // Save the attempt
//        otpRequiredStatusChangeAttemptRepository.save(attempt);
//
//        // Send OTP to user's mobile
//        otpService.sendOtp(user.getMobileNumber(), otp);
//
//        // Return OTP response
//        TwoFactorOtpResponse response = new TwoFactorOtpResponse();
//        response.setUserId(userId);
//        response.setMessage("OTP sent to registered mobile number");
//
//        Response<TwoFactorOtpResponse> otpResponse = new Response<>();
//        otpResponse.setMessage("OTP required for OTP requirement status change");
//        otpResponse.setSuccess(true);
//        otpResponse.setData(response);
//        return otpResponse;
//    }
    @Transactional
    public Response<TwoFactorOtpResponse> initiateOtpRequiredToggle(Long userId, Boolean isOtpRequired) {
        log.info("Initiating OTP requirement toggle for userId={}, enabled={}", userId, isOtpRequired);

        // Validate user existence
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found for userId={}", userId);
                    return new IllegalArgumentException("User not found");
                });

        log.info("User found: id={}, mobile={}, verified={}",
                user.getId(), user.getMobileNumber(), user.getIsMobileVerified());

        // Validate mobile verification
        if (isOtpRequired && !Boolean.TRUE.equals(user.getIsMobileVerified())) {
            log.error("Mobile number not verified for userId={}", userId);
            throw new IllegalArgumentException("Mobile number must be verified to enable OTP requirement");
        }

        if (!Boolean.TRUE.equals(user.getIsMobileVerified()) || user.getMobileNumber() == null) {
            log.error("Mobile not verified or missing for userId={}", userId);
            throw new IllegalArgumentException("Mobile number must be verified to perform this action");
        }

        // Check for existing attempt
        OtpRequiredStatusChangeAttempt attempt = otpRequiredStatusChangeAttemptRepository
                .findByUserId(userId)
                .orElse(new OtpRequiredStatusChangeAttempt());

        // Update or initialize attempt
        attempt.setUserId(userId);
        attempt.setOtp(String.format("%06d", (int) (Math.random() * 900000 + 100000)));
        attempt.setIsActive(true);
        attempt.setCreatedAt(LocalDateTime.now());
        attempt.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        attempt.setNewOtpRequiredStatus(isOtpRequired);

        // Save the attempt (updates if exists, creates if new)
        otpRequiredStatusChangeAttemptRepository.save(attempt);
        log.info("Saved/Updated OTP attempt for userId={}", userId);

        // Send OTP to user's mobile
        try {
            otpService.sendOtp(user.getMobileNumber(), attempt.getOtp());
            log.info("OTP sent to mobile {} for userId={}", user.getMobileNumber(), userId);
        } catch (Exception e) {
            log.error("Failed to send OTP for userId={}: {}", userId, e.getMessage());
            throw new RuntimeException("Failed to send OTP. Please try again.");
        }

        // Prepare response
        TwoFactorOtpResponse otpResponseData = new TwoFactorOtpResponse();
        otpResponseData.setUserId(userId);
        otpResponseData.setMessage("OTP sent to registered mobile number");

        Response<TwoFactorOtpResponse> response = new Response<>();
        response.setMessage("OTP required for OTP requirement status change");
        response.setSuccess(true);
        response.setData(otpResponseData);

        return response;
    }
    
//    @Transactional
//    public Response<Void> verifyOtpRequiredOtp(Long userId, String otp) {
//        log.debug("Verifying OTP for OTP requirement toggle: userId={}", userId);
//        try {
//            log.info("Querying OTP attempt for userId={}", userId);
//            OtpRequiredStatusChangeAttempt attempt = otpRequiredStatusChangeAttemptRepository
//                    .findByUserIdAndIsActiveTrue(userId)
//                    .orElseThrow(() -> new IllegalArgumentException("No active OTP requirement change request found"));
//            log.info("Found OTP attempt: id={}, otp={}, expiresAt={}", attempt.getId(), attempt.getOtp(), attempt.getExpiresAt());
//            log.info("Checking expiry: now={}, expiresAt={}", LocalDateTime.now(), attempt.getExpiresAt());
//            if (LocalDateTime.now().isAfter(attempt.getExpiresAt())) {
//                throw new IllegalArgumentException("OTP has expired");
//            }
//            log.info("Verifying OTP: stored={}, provided={}", attempt.getOtp(), otp);
//            if (!attempt.getOtp().equals(otp)) {
//                throw new IllegalArgumentException("Invalid OTP");
//            }
//            log.info("Retrieving user: userId={}", userId);
//            UserEntity user = userRepository.findById(userId)
//                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
//            log.info("Updating OTP attempt to inactive: id={}", attempt.getId());
//            attempt.setIsActive(false);
//            otpRequiredStatusChangeAttemptRepository.save(attempt);
//            log.info("Toggling OTP requirement for userId={}", userId);
//            return performOtpRequiredToggle(user, attempt.getNewOtpRequiredStatus());
//        } catch (Exception e) {
//            log.error("Error verifying OTP for user {}: {}", userId, e.getMessage(), e);
//            throw e; // Rethrow to preserve original exception
//        }
//    }
//    
//    private Response<Void> performOtpRequiredToggle(UserEntity user, Boolean isOtpRequired) {
//        user.setIsOtpRequired(isOtpRequired);
//        user.setUpdatedAt(LocalDateTime.now());
//        user.setUpdatedBy("OTP Requirement Toggle");
//
//        // Save to PostgreSQL
//        UserEntity savedUser = userRepository.save(user);
//
//        // Update MongoDB
//        try {
//            MongoUser existingMongoUser = mongoUserRepository.findFirstByUserId(user.getId());
//            if (existingMongoUser != null) {
//                existingMongoUser.setIsOtpRequired(savedUser.getIsOtpRequired());
//                existingMongoUser.setUpdatedAt(savedUser.getUpdatedAt());
//                existingMongoUser.setUpdatedBy(savedUser.getUpdatedBy());
//                mongoUserRepository.save(existingMongoUser);
//            } else {
//                MongoUser mongoUser = new MongoUser(savedUser);
//                mongoUserRepository.save(mongoUser);
//            }
//        } catch (Exception mongoEx) {
//            log.error("Failed to update MongoDB for user: {}", user.getId(), mongoEx);
//            // Continue with response since PostgreSQL update succeeded
//        }
//
//        Response<Void> response = new Response<>();
//        response.setMessage("OTP requirement " + (isOtpRequired ? "enabled" : "disabled") + " successfully");
//        response.setSuccess(true);
//        return response;
//    }
    @Transactional
    public Response<Void> verifyOtpRequiredOtp(Long userId, String otp) {
        log.info("Verifying OTP for OTP requirement toggle: userId={}", userId);

        // Fetch the attempt
        OtpRequiredStatusChangeAttempt attempt = otpRequiredStatusChangeAttemptRepository
                .findByUserId(userId)
                .orElseThrow(() -> {
                    log.error("No OTP requirement change request found for userId={}", userId);
                    return new IllegalArgumentException("No OTP requirement change request found");
                });

        // Check if attempt is active
        if (!attempt.getIsActive()) {
            log.error("No active OTP requirement change request found for userId={}", userId);
            throw new IllegalArgumentException("No active OTP requirement change request found");
        }

        // Check if OTP is expired
        if (LocalDateTime.now().isAfter(attempt.getExpiresAt())) {
            attempt.setIsActive(false);
            otpRequiredStatusChangeAttemptRepository.save(attempt);
            log.error("OTP expired for userId={}", userId);
            throw new IllegalArgumentException("OTP has expired");
        }

        // Validate OTP
        if (!attempt.getOtp().equals(otp)) {
            log.error("Invalid OTP provided for userId={}. Provided: {}, Expected: {}", userId, otp, attempt.getOtp());
            throw new IllegalArgumentException("Invalid OTP");
        }

        // Fetch user
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found during OTP verification for userId={}", userId);
                    return new IllegalArgumentException("User not found");
                });

        // Deactivate the attempt
        attempt.setIsActive(false);
        otpRequiredStatusChangeAttemptRepository.save(attempt);
        log.info("Deactivated OTP attempt for userId={}", userId);

        // Perform OTP requirement toggle
        return performOtpRequiredToggle(user, attempt.getNewOtpRequiredStatus());
    }

    private Response<Void> performOtpRequiredToggle(UserEntity user, Boolean isOtpRequired) {
        log.info("Toggling OTP requirement for userId={} to enabled={}", user.getId(), isOtpRequired);

        user.setIsOtpRequired(isOtpRequired);
        user.setUpdatedAt(LocalDateTime.now());
        user.setUpdatedBy("SYSTEM_OTP_TOGGLE");

        // Save to PostgreSQL
        UserEntity savedUser = userRepository.save(user);
        log.info("Updated PostgreSQL user: id={}, isOtpRequired={}", savedUser.getId(), savedUser.getIsOtpRequired());

        // Update MongoDB
        try {
            MongoUser existingMongoUser = mongoUserRepository.findFirstByUserId(user.getId());
            if (existingMongoUser != null) {
                existingMongoUser.setIsOtpRequired(savedUser.getIsOtpRequired());
                existingMongoUser.setUpdatedAt(savedUser.getUpdatedAt());
                existingMongoUser.setUpdatedBy(savedUser.getUpdatedBy());
                mongoUserRepository.save(existingMongoUser);
                log.info("Updated MongoDB user: id={}", existingMongoUser.getUserId());
            } else {
                MongoUser mongoUser = new MongoUser(savedUser);
                mongoUserRepository.save(mongoUser);
                log.info("Created new MongoDB user: id={}", mongoUser.getUserId());
            }
        } catch (Exception mongoEx) {
            log.error("Failed to update MongoDB for userId={}: {}", user.getId(), mongoEx.getMessage());
            // Continue since PostgreSQL update succeeded
        }

        Response<Void> response = new Response<>();
        response.setMessage("OTP requirement " + (isOtpRequired ? "enabled" : "disabled") + " successfully");
        response.setSuccess(true);
        return response;
    }
    
    @Transactional
    public Response<String> migrateTwoFactorEnabled() {
        log.info("Starting migration to set isTwoFactorEnabled for existing users");

        try {
            // Update PostgreSQL
            List<UserEntity> allUsers = userRepository.findAll();
            int updatedPostgresCount = 0;
            for (UserEntity user : allUsers) {
                if (user.getIsTwoFactorEnabled() == null) {
                    user.setIsTwoFactorEnabled(false);
                    user.setUpdatedAt(LocalDateTime.now());
                    user.setUpdatedBy("2FA Migration");
                    userRepository.save(user);
                    updatedPostgresCount++;
                }
            }
            log.info("Updated {} users in PostgreSQL", updatedPostgresCount);

            // Update MongoDB
            List<MongoUser> allMongoUsers = mongoUserRepository.findAll();
            int updatedMongoCount = 0;
            for (MongoUser mongoUser : allMongoUsers) {
                if (mongoUser.getIsTwoFactorEnabled() == null) {
                    mongoUser.setIsTwoFactorEnabled(false);
                    mongoUser.setUpdatedAt(LocalDateTime.now());
                    mongoUser.setUpdatedBy("2FA Migration");
                    mongoUserRepository.save(mongoUser);
                    updatedMongoCount++;
                }
            }
            log.info("Updated {} users in MongoDB", updatedMongoCount);

            String message = String.format("Migration completed: Updated %d users in PostgreSQL and %d users in MongoDB", 
                                          updatedPostgresCount, updatedMongoCount);
            Response<String> response = new Response<>();
            response.setMessage(message);
            response.setSuccess(true);
            response.setData(message);
            return response;

        } catch (Exception ex) {
            log.error("Failed to migrate isTwoFactorEnabled: {}", ex.getMessage(), ex);
            Response<String> errorResponse = new Response<>();
            errorResponse.setMessage("Migration failed: " + ex.getMessage());
            errorResponse.setSuccess(false);
            return errorResponse;
        }
    }
    
    @Transactional
    public Response<String> migrateSlipBoxStatus() {
        log.info("Starting migration to set slipBox to true for existing users");

        try {
            // Update PostgreSQL
            List<UserEntity> allUsers = userRepository.findAll();
            int updatedPostgresCount = 0;
            for (UserEntity user : allUsers) {
                if (user.getSlipBox() == null || !user.getSlipBox()) {
                    user.setSlipBox(true);
                    user.setUpdatedAt(LocalDateTime.now());
                    user.setUpdatedBy("SlipBox Migration");
                    userRepository.save(user);
                    updatedPostgresCount++;
                }
            }
            log.info("Updated {} users in PostgreSQL", updatedPostgresCount);

            // Update MongoDB
            List<MongoUser> allMongoUsers = mongoUserRepository.findAll();
            int updatedMongoCount = 0;
            for (MongoUser mongoUser : allMongoUsers) {
                if (mongoUser.getSlipBox() == null || !mongoUser.getSlipBox()) {
                    mongoUser.setSlipBox(true);
                    mongoUser.setUpdatedAt(LocalDateTime.now());
                    mongoUser.setUpdatedBy("SlipBox Migration");
                    mongoUserRepository.save(mongoUser);
                    updatedMongoCount++;
                }
            }
            log.info("Updated {} users in MongoDB", updatedMongoCount);

            String message = String.format("SlipBox migration completed: Updated %d users in PostgreSQL and %d users in MongoDB",
                                          updatedPostgresCount, updatedMongoCount);
            Response<String> response = new Response<>();
            response.setMessage(message);
            response.setSuccess(true);
            response.setData(message);
            return response;

        } catch (Exception ex) {
            log.error("Failed to migrate slipBox status: {}", ex.getMessage(), ex);
            Response<String> errorResponse = new Response<>();
            errorResponse.setMessage("SlipBox migration failed: " + ex.getMessage());
            errorResponse.setSuccess(false);
            return errorResponse;
        }
    }
    
    public Page<UserListDto> getUsersCreatedByParent(Long parentUserId, int page, int size) {
        // Get the parent user first
        UserEntity parentUser = userRepository.findById(parentUserId)
                .orElseThrow(() -> new IllegalArgumentException("Parent user not found"));
        
        // Get users created by this parent (including the parent themselves)
        Page<UserEntity> userEntities = userRepository.findByCreatedByOrId(parentUser.getEmail(), parentUserId, PageRequest.of(page, size));
        
        // Get volunteers assigned to this parent
        List<VolunteerEntity> volunteers = volunteerRepository.findByAdminUserId(parentUserId);
        
        // Combine and convert to DTOs
        List<UserListDto> combinedList = new ArrayList<>();
        
        // Add parent user
        combinedList.add(convertToUserListDto(parentUser));
        
        // Add created users
        userEntities.getContent().stream()
                .filter(user -> !user.getId().equals(parentUserId)) // exclude parent if already added
                .map(this::convertToUserListDto)
                .forEach(combinedList::add);
        
        // Add volunteers as special user entries
        volunteers.stream()
                .map(this::convertVolunteerToUserListDto)
                .forEach(combinedList::add);
        
        // Sort the combined list by firstName ascending, then by lastName ascending
        combinedList.sort((u1, u2) -> {
            // Handle null values for firstName
            String firstName1 = u1.getFirstName() != null ? u1.getFirstName().toLowerCase() : "";
            String firstName2 = u2.getFirstName() != null ? u2.getFirstName().toLowerCase() : "";
            
            int firstNameComparison = firstName1.compareTo(firstName2);
            if (firstNameComparison != 0) {
                return firstNameComparison;
            }
            
            // If first names are equal, compare by lastName
            String lastName1 = u1.getLastName() != null ? u1.getLastName().toLowerCase() : "";
            String lastName2 = u2.getLastName() != null ? u2.getLastName().toLowerCase() : "";
            
            return lastName1.compareTo(lastName2);
        });
        
        // Create a custom page implementation
        return new PageImpl<>(combinedList, PageRequest.of(page, size), 
                userEntities.getTotalElements() + volunteers.size());
    }

    private UserListDto convertToUserListDto(UserEntity user) {
        UserListDto dto = new UserListDto();
        dto.setUserId(user.getId());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setEmail(user.getEmail());
        dto.setMobileNumber(user.getMobileNumber());
        dto.setIsActive(user.getIsActive());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setRoleName(user.getRole() != null ? user.getRole().getRoleName() : null);
        dto.setSlipBox(user.getSlipBox());
        dto.setIsTwoFactorEnabled(user.getIsTwoFactorEnabled());
        dto.setIsOtpRequired(user.getIsOtpRequired());
        return dto;
    }

    private UserListDto convertVolunteerToUserListDto(VolunteerEntity volunteer) {
        UserListDto dto = new UserListDto();
        dto.setUserId(volunteer.getUserEntity().getId()); // Use the linked user's ID
        dto.setFirstName(volunteer.getUserEntity().getFirstName());
        dto.setLastName(volunteer.getUserEntity().getLastName());
        dto.setEmail(volunteer.getUserEntity().getEmail());
        dto.setMobileNumber(volunteer.getUserEntity().getMobileNumber());
        dto.setIsActive(volunteer.getUserEntity().getIsActive());
        dto.setCreatedAt(volunteer.getCreatedTime()); // Use volunteer creation time
        dto.setRoleName("VOLUNTEER"); // Or fetch from role if available
        dto.setSlipBox(volunteer.getUserEntity().getSlipBox());
        dto.setIsTwoFactorEnabled(volunteer.getUserEntity().getIsTwoFactorEnabled());
        dto.setIsOtpRequired(volunteer.getUserEntity().getIsOtpRequired());
        return dto;
    }

}