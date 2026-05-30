package com.thedal.thedal_app.oauth2login;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.server.ResponseStatusException;

import com.amazonaws.services.finspacedata.model.UserStatus;
import com.thedal.thedal_app.account.AccountEntity;
import com.thedal.thedal_app.account.AccountOnBoardStatus;
import com.thedal.thedal_app.account.AccountRepository;
import com.thedal.thedal_app.auth.AuthService;
import com.thedal.thedal_app.auth.dtos.OAuthRequestDTO;
import com.thedal.thedal_app.auth.dtos.SignupRequestDto;
import com.thedal.thedal_app.general.RequestDetailsService;
import com.thedal.thedal_app.role.Role;
import com.thedal.thedal_app.role.Role;
import com.thedal.thedal_app.account.AccountOnBoardStatus;
import com.thedal.thedal_app.account.AccountRepository;
import com.thedal.thedal_app.role.RoleRepo;
import com.thedal.thedal_app.user.UserEntity;
import com.thedal.thedal_app.user.UserRepo;
import com.thedal.thedal_app.user.MongoUser;
import com.thedal.thedal_app.user.MongoUserRepository;

import jakarta.validation.Valid;

@Service
public class OAuthUserService {

    @Autowired 
    private AccountRepository accountRepository;

    @Autowired 
    private RequestDetailsService requestDetailsService;

     @Autowired
    AuthService authService;

    @Autowired
    private RoleRepo roleRepo;
    @Autowired
    private UserRepo userRepo;
    @Autowired
    private MongoUserRepository mongoUserRepository;

   
    // public UserEntity processOAuthPostLogin(String email, String name) {
    //     UserEntity existUser = userRepo.findByEmail(email);
    //     final Long SUPER_ADMIN_ID = 1L;
    //     Role userRole = roleRepo.findById(SUPER_ADMIN_ID).orElse(null);
        


    //     if (existUser == null) {
    //         UserEntity newUser = new UserEntity();
    //         newUser.getId();
    //         newUser.setEmail(email);
    //         newUser.setFirstName(name);
    //         newUser.setLastName(name);
    //         newUser.setIsEmailVerified(true);
    //         newUser.setCreatedAt(LocalDateTime.now());
    //         newUser.setIsMobileVerified(false);
    //         newUser.setRole(userRole);
    //         newUser.setIsActive(true);
    //         AccountEntity account=new AccountEntity();
    //         account.setOnBoardStatus(AccountOnBoardStatus.SIGNUP_COMPLETION.getValue());
    //         accountRepository.save(account);
    //         newUser.setAccountEntity(account);
    //        // newUser.setOnBoardStatus(1);


    //        userRepo.save(newUser);
    //        return newUser;
    //    } else {
    //        return existUser;
    //    }
    // }

    public UserEntity processOAuthPostLogin(String email, String name) {
        UserEntity existingUser = userRepo.findByEmail(email);
        final Long SUPER_ADMIN_ID = 1L;
        Role userRole = roleRepo.findById(SUPER_ADMIN_ID).orElse(null);
    
        if (existingUser == null) {
            // Split full name into first and last name
            String firstName = "";
            String lastName = "";
    
            if (name != null && name.contains(" ")) {
                int lastSpaceIndex = name.lastIndexOf(" ");
                firstName = name.substring(0, lastSpaceIndex); // Everything before the last space
                lastName = name.substring(lastSpaceIndex + 1); // Last word after the last space
            } else {
                firstName = name; // If no space, the whole name is firstName
                lastName = ""; // No last name
            }
    
            UserEntity newUser = new UserEntity();
            newUser.getId();
            newUser.setEmail(email);
            newUser.setFirstName(firstName);
            newUser.setLastName(lastName);
            newUser.setIsEmailVerified(true);
            newUser.setCreatedAt(LocalDateTime.now());
            newUser.setIsMobileVerified(false);
            newUser.setRole(userRole);
            newUser.setIsActive(false);
    
            AccountEntity account = new AccountEntity();
            account.setOnBoardStatus(AccountOnBoardStatus.OAUTH.getValue());
            accountRepository.save(account);
            newUser.setAccountEntity(account);
    
            // Save to PostgreSQL and MongoDB with dual-write pattern
            try {
                UserEntity savedUser = userRepo.save(newUser);
                try {
                    MongoUser mongoUser = new MongoUser(savedUser);
                    mongoUserRepository.save(mongoUser);
                } catch (Exception mongoEx) {
                    throw new RuntimeException("MongoDB save failed, triggering rollback", mongoEx);
                }
                return savedUser;
            } catch (Exception ex) {
                throw new RuntimeException("User creation failed", ex);
            }
        } else {
            return existingUser;
        }
    }
    
           // newUser.setOnBoardStatus(1);


    public UserEntity authsignUp(@Valid @RequestBody OAuthRequestDTO request) {
            // Find existing user by email (Google OAuth email)
           
        UserEntity existingUser = userRepo.findByEmail(request.getEmail());
         if (existingUser == null) {
         throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
         }    
            // Check if mobile number is already linked to another user
            Optional<UserEntity> userWithSameMobile = userRepo.findByMobileNumber(request.getMobile());
            if (userWithSameMobile.isPresent() && !userWithSameMobile.get().getEmail().equals(request.getEmail())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Mobile number is already in use by another user.");
            }
    
            // Update existing user's mobile number and password
            existingUser.setMobileNumber(request.getMobile());
            existingUser.setPassword(new BCryptPasswordEncoder().encode(request.getPassword()));
            existingUser.setIsActive(false);
            // Save the updated user
            userRepo.save(existingUser);
    
            // Send mobile verification OTP
            authService.sendMobileVerificationOtp(existingUser);
    
            // Update onboarding status
            AccountEntity account = existingUser.getAccountEntity();
           if (account != null) {
        account.setOnBoardStatus(AccountOnBoardStatus.SIGNUP_COMPLETION.getValue());
        accountRepository.save(account); // Save the updated account entity
        } else {
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "User account not found.");
        }

         
            return existingUser;
        }
    }
   

    

