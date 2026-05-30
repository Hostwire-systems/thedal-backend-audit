package com.thedal.thedal_app.user;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.thedal.thedal_app.election.dtos.UpdateOtpRequiredRequest;
import com.thedal.thedal_app.response.ThedalResponse;
import com.thedal.thedal_app.response.ThedalSuccess;
import com.thedal.thedal_app.util.Response;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    UserService userService;
    
    @Autowired
    private UserRepo userRepository;


    @GetMapping("/{userId}")
    public ResponseEntity<Response<UserDetailsDto>> getUserById(@PathVariable("userId") Long userId) {
        try {
            // Use MongoDB service instead of PostgreSQL service
            UserDetailsDto userDetailsDto = userService.getUserByIdFromMongo(userId);

            Response<UserDetailsDto> response = new Response<>();
            response.setMessage("User details retrieved successfully");
            response.setSuccess(true);
            response.setData(userDetailsDto);

            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (IllegalArgumentException e) {
            Response<UserDetailsDto> errorResponse = new Response<>();
            errorResponse.setMessage(e.getMessage());
            errorResponse.setSuccess(false);

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
    }


    @PutMapping("/{userId}")
    public ResponseEntity<Response<UserDetailsDto>> updateUser(
        @PathVariable("userId") Long userId, 
        @RequestBody UpdateUsersDTO updateRequest
    ) {
        try {
            
            Response<UserDetailsDto> response = userService.updateUser(userId, updateRequest);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
    
            Response<UserDetailsDto> errorResponse = new Response<>();
            errorResponse.setMessage(e.getMessage());
            errorResponse.setSuccess(false);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            Response<UserDetailsDto> errorResponse = new Response<>();
            errorResponse.setMessage("An error occurred while updating the user.");
            errorResponse.setSuccess(false);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }


    @PutMapping("/{user_id}/deactivate")
    public ResponseEntity<Response<Void>> deactivateUser(
            @PathVariable("user_id") Long userId,
            @RequestHeader("Authorization") String authorizationHeader) {
        
        
        Response<Void> response = userService.deactivateUser(userId);
        
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

//    @PostMapping("/image-upload")
//     public ResponseEntity<Response<String>> imageUpload(
//     @RequestParam("file") MultipartFile multipartFile) throws IOException {
//    return ResponseEntity.ok(userService.imageUploaded(multipartFile));
//   }
    
    @Operation(summary = "Profile picture", description = "Upload profile picture from profile settings")
    @PostMapping(value = "/profile-picture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ThedalResponse<String> uploadProfilePicture(
            @RequestParam("file") MultipartFile multipartFile) throws IOException{
        return userService.uploadProfilePicture(multipartFile);
    }

//    @GetMapping("/app/users")
//    public ResponseEntity<ThedalResponse<Page<UserListDto>>> getAllUsers(
//        @RequestParam(defaultValue = "0") int page,
//        @RequestParam(defaultValue = "10") int size,
//        @RequestParam(required = false) String sortBy,
//        @RequestParam(required = false, defaultValue = "asc") String sortDirection,
//        @RequestParam(required = false) Boolean isActive,
//        @RequestParam(required = false) String firstName,
//        @RequestParam(required = false) String lastName
//    		) {
//
//    Page<UserListDto> users = userService.getAllUsers(page, size, sortBy, sortDirection, isActive, firstName, lastName); 
//   
//    ThedalResponse<Page<UserListDto>> response = new ThedalResponse<>(
//            ThedalSuccess.USERS_RETRIEVED, users
//    );
//
//    return ResponseEntity.ok(response);
//    }
    
    @GetMapping("/app/users")
    public ResponseEntity<ThedalResponse<Page<UserListDto>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false, defaultValue = "asc") String sortDirection,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String mobileNumber 
    ) {
        // Use MongoDB service instead of PostgreSQL service
        Page<UserListDto> users = userService.getAllUsersFromMongo(page, size, sortBy, sortDirection, isActive, name, username, mobileNumber);
       
        ThedalResponse<Page<UserListDto>> response = new ThedalResponse<>(
                ThedalSuccess.USERS_RETRIEVED, users
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/app/users/{userId}/activity")
    public ResponseEntity<Response<UserActivityDto>> getUserActivity(@PathVariable("userId") Long userId) {
    try {
        // Use MongoDB service instead of PostgreSQL service
        UserActivityDto activityDto = userService.getMongoUserActivity(userId.toString());
        Response<UserActivityDto> response = new Response<>();
        response.setMessage("User activity retrieved successfully");
        response.setSuccess(true);
        response.setData(activityDto);

        return ResponseEntity.ok(response);
    } catch (IllegalArgumentException e) {
        Response<UserActivityDto> errorResponse = new Response<>();
        errorResponse.setMessage(e.getMessage());
        errorResponse.setSuccess(false);

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    
}

//       @PutMapping("/{userId}/activate")
//       public ResponseEntity<String> activateUser(@PathVariable Long userId) {
//         String response = userService.activateUser(userId);
//         return ResponseEntity.ok(response);
// }
    
    @DeleteMapping("/{userId}")
public ResponseEntity<Response<Void>> deleteUser(@PathVariable("userId") Long userId) {
    try {
        Response<Void> response = userService.deleteUser(userId);
        return ResponseEntity.ok(response);
    } catch (IllegalArgumentException e) {
        Response<Void> errorResponse = new Response<>();
        errorResponse.setMessage(e.getMessage());
        errorResponse.setSuccess(false);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    } catch (Exception e) {
        Response<Void> errorResponse = new Response<>();
        errorResponse.setMessage("An error occurred while deleting the user.");
        errorResponse.setSuccess(false);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
    // Endpoint to get user info from MongoDB by username
    @GetMapping("/mongo/app/users/username/{username}")
    public ResponseEntity<MongoUser> getMongoUserByUsername(@PathVariable String username) {
        MongoUser mongoUser = userService.getUserFromMongo(username);
        if (mongoUser == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        return ResponseEntity.ok(mongoUser);
    }

    // Endpoint to trigger migration from Postgres to MongoDB (admin use only)
    @PostMapping("/migrate-users-to-mongo")
    public ResponseEntity<String> migrateUsersToMongo() {
        userService.migrateAllUsersToMongo();
        return ResponseEntity.ok("Migration to MongoDB completed.");
    }
    
    @GetMapping("/mongo/app/users/{userId}")
    public ResponseEntity<MongoUser> getMongoUserById(@PathVariable String userId) {
        MongoUser mongoUser = userService.getMongoUserById(userId);
        if (mongoUser == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        return ResponseEntity.ok(mongoUser);
    }

    @GetMapping("/mongo/app/users")
    public ResponseEntity<ThedalResponse<Page<MongoUser>>> getAllMongoUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false, defaultValue = "asc") String sortDirection,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String mobileNumber 
    ) {
        Page<MongoUser> users = userService.getAllMongoUsers(page, size, sortBy, sortDirection, isActive, name, username, mobileNumber);
        ThedalResponse<Page<MongoUser>> response = new ThedalResponse<>(ThedalSuccess.USERS_RETRIEVED, users);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/mongo/app/users/{userId}/activity")
    public ResponseEntity<UserActivityDto> getMongoUserActivity(@PathVariable String userId) {
        UserActivityDto activity = userService.getMongoUserActivity(userId);
        if (activity == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        return ResponseEntity.ok(activity);
    }
    
    /**
     * Check migration status - compare user counts between PostgreSQL and MongoDB
     */
    @GetMapping("/migration-status")
    @Operation(summary = "Check migration status - user counts in PostgreSQL vs MongoDB")
    public ResponseEntity<Response<String>> getMigrationStatus() {
        try {
            Response<String> response = userService.getMigrationStatus();
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (Exception e) {
            Response<String> errorResponse = new Response<>();
            errorResponse.setMessage("Failed to get migration status: " + e.getMessage());
            errorResponse.setSuccess(false);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @Operation(summary = "Toggle 2FA Status", description = "Enable or disable two-factor authentication with OTP verification")
    @PutMapping("/{userId}/two-factor")
    public ResponseEntity<Response<?>> toggleTwoFactor(
            @PathVariable("userId") Long userId,
            @RequestBody TwoFactorToggleRequest toggleRequest) {
        try {
            // Modified to return either OTP response or direct success
            return ResponseEntity.ok(userService.initiateTwoFactorToggle(userId, toggleRequest.isEnabled()));
        } catch (IllegalArgumentException e) {
            Response<Void> errorResponse = new Response<>();
            errorResponse.setMessage(e.getMessage());
            errorResponse.setSuccess(false);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
        	 Response<Void> errorResponse = new Response<>();
             errorResponse.setMessage("An error occurred while toggling 2FA status.");
             errorResponse.setSuccess(false);
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
         }
     }
    
    @Operation(summary = "Verify OTP for 2FA Toggle", description = "Verify OTP to complete two-factor authentication status change")
    @PostMapping("/{userId}/verify-two-factor-otp")
    public ResponseEntity<Response<Void>> verifyTwoFactorOtp(
            @PathVariable("userId") Long userId,
            @RequestParam String otp) {
        try {
            Response<Void> response = userService.verifyTwoFactorOtp(userId, otp);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Response<Void> errorResponse = new Response<>();
            errorResponse.setMessage(e.getMessage());
            errorResponse.setSuccess(false);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            Response<Void> errorResponse = new Response<>();
            errorResponse.setMessage("An error occurred while verifying OTP.");
            errorResponse.setSuccess(false);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
       
    
//    @Operation(summary = "Toggle OTP", description = "Enable or disable OTP requirement for a user")
//    @PutMapping("/{userId}/otp-required")
//    public ResponseEntity<Response<Void>> toggleOtpRequired(
//            @PathVariable("userId") Long userId,
//            @RequestBody UpdateOtpRequiredRequest toggleRequest) {
//        try {
//            Response<Void> response = userService.toggleOtpRequired(userId, toggleRequest.getIsOtpRequired());
//            return ResponseEntity.ok(response);
//        } catch (IllegalArgumentException e) {
//            Response<Void> errorResponse = new Response<>();
//            errorResponse.setMessage(e.getMessage());
//            errorResponse.setSuccess(false);
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
//        } catch (Exception e) {
//            Response<Void> errorResponse = new Response<>();
//            errorResponse.setMessage("An error occurred while toggling OTP requirement.");
//            errorResponse.setSuccess(false);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
//        }
//    }
    @Operation(summary = "Toggle OTP", description = "Initiate enabling or disabling OTP requirement with OTP verification")
    @PutMapping("/{userId}/otp-required")
    public ResponseEntity<Response<?>> initiateOtpRequiredToggle(
            @PathVariable("userId") Long userId,
            @RequestBody UpdateOtpRequiredRequest toggleRequest) {
        try {
            return ResponseEntity.ok(userService.initiateOtpRequiredToggle(userId, toggleRequest.getIsOtpRequired()));
        } catch (IllegalArgumentException e) {
            Response<Void> errorResponse = new Response<>();
            errorResponse.setMessage(e.getMessage());
            errorResponse.setSuccess(false);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            Response<Void> errorResponse = new Response<>();
            errorResponse.setMessage("An error occurred while initiating OTP requirement toggle.");
            errorResponse.setSuccess(false);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    @Operation(summary = "Verify OTP for OTP Requirement Toggle", description = "Verify OTP to complete OTP requirement status change")
    @PostMapping("/{userId}/verify-otp-required-otp")
    public ResponseEntity<Response<Void>> verifyOtpRequiredOtp(
            @PathVariable("userId") Long userId,
            @RequestParam String otp) {
        try {
            Response<Void> response = userService.verifyOtpRequiredOtp(userId, otp);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Response<Void> errorResponse = new Response<>();
            errorResponse.setMessage(e.getMessage());
            errorResponse.setSuccess(false);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            Response<Void> errorResponse = new Response<>();
            errorResponse.setMessage("An error occurred while verifying OTP.");
            errorResponse.setSuccess(false);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @Operation(summary = "Migrate 2FA Status", description = "Set isTwoFactorEnabled to false for all users where it is null (admin only)")
    @PostMapping("/migrate-two-factor")
    public ResponseEntity<Response<String>> migrateTwoFactorEnabled() {
        Response<String> response = userService.migrateTwoFactorEnabled();
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "Migrate SlipBox Status", description = "Set slipBox to true for all users where it is null or false (admin only)")
    @PostMapping("/migrate-slip-box")
    public ResponseEntity<Response<String>> migrateSlipBoxStatus() {
        Response<String> response = userService.migrateSlipBoxStatus();
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "Get users/cadres created by parent user", 
            description = "Returns all users and volunteers created by the specified parent user in a unified list")
 @GetMapping("/created-by/{parentUserId}")
 public ResponseEntity<ThedalResponse<Page<UserListDto>>> getUsersCreatedByParent(
         @PathVariable("parentUserId") Long parentUserId,
         @RequestParam(defaultValue = "0") int page,
         @RequestParam(defaultValue = "10") int size) {
     
     Page<UserListDto> response = userService.getUsersCreatedByParent(parentUserId, page, size);
     return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.USERS_RETRIEVED, response));
 }
    
}
