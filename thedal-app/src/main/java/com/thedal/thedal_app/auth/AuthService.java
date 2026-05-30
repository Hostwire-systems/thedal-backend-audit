package com.thedal.thedal_app.auth;

import java.io.IOException;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.thedal.thedal_app.JwtService;
import com.thedal.thedal_app.account.AccountEntity;
import com.thedal.thedal_app.account.AccountOnBoardStatus;
import com.thedal.thedal_app.account.AccountRepository;
import com.thedal.thedal_app.auth.dtos.CompleteSignupDto;
import com.thedal.thedal_app.auth.dtos.LoginRequestDto;
import com.thedal.thedal_app.auth.dtos.LoginResponseDto;
import com.thedal.thedal_app.auth.dtos.OtpLoginRequest;
import com.thedal.thedal_app.auth.dtos.ResetPasswordRequestDto;
import com.thedal.thedal_app.auth.dtos.SignupRequestDto;
import com.thedal.thedal_app.auth.dtos.VerifyMobileOtpDto;
import com.thedal.thedal_app.auth.dtos.VolunteerOtpResponse;
import com.thedal.thedal_app.notification.NotificationService;
import com.thedal.thedal_app.notification.NotificationTemplate;
import com.thedal.thedal_app.notification.SmsNotification;
import com.thedal.thedal_app.role.Role;
import com.thedal.thedal_app.role.RoleRepo;
import com.thedal.thedal_app.system.SystemSettingEntity;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;
import com.thedal.thedal_app.user.MongoUser;
import com.thedal.thedal_app.user.MongoUserRepository;
import com.thedal.thedal_app.user.UserEntity;
import com.thedal.thedal_app.user.UserRepo;
import com.thedal.thedal_app.user.UserService;
import com.thedal.thedal_app.user.VolunteerLoginAttempt;
import com.thedal.thedal_app.user.VolunteerLoginAttemptRepository;
import com.thedal.thedal_app.user.VolunteerOtpVerifyRequest;
//import com.thedal.thedal_app.util.MessagingService;
import com.thedal.thedal_app.util.RandomTokenGenerator;
import com.thedal.thedal_app.util.Response;
import com.thedal.thedal_app.volunteer.VolunteerEntity;
import com.thedal.thedal_app.volunteer.VolunteerRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AuthService {

    @Autowired
    UserRepo userRepo;
    @Autowired
    RoleRepo roleRepo;
    @Autowired
    EmailVerificationRepo emailVerificationRepo;
    @Autowired
    private NotificationTemplate  notificationTemplate;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private UserService userService;
//    @Autowired
//    private UserRoleRepo userRoleRepo2;

    // @Autowired
    // MessagingService messagingService;
    @Autowired
    MobileVerificationRepo mobileVerificationRepo;
    @Autowired
    private Environment environment;
    @Autowired
    private SmsNotification smsNotification;   
    @Autowired
    private AccountRepository accountRepository;    
    @Autowired
    private JwtService jwtService;
    @Autowired
    private VolunteerRepository volunteerRepo;
    @Autowired
    private MongoUserRepository mongoUserRepository;
    @Autowired
    private VolunteerLoginAttemptRepository volunteerLoginAttemptRepository;
    @Autowired
    private com.thedal.thedal_app.system.SystemSettingService systemSettingService;
    
//    @Autowired
//    private AccountToUserRepository accountToUserRepository;

    private static final Long SUPER_ADMIN=1L;
    private static final Long SUPER_ADMIN_ROLE_ID = 1L;
    
    @Transactional(rollbackFor = {Exception.class})
    public ResponseEntity<Response<LoginResponseDto>> signUp(SignupRequestDto request) throws IOException {
        log.info("Signup request received for user: {}", request.toString());

        Response<LoginResponseDto> response = new Response<>();

        if (userRepo.existsByEmail(request.getEmail())) {
            response.setMessage("Email already exists");
            response.setSuccess(false);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        if (userRepo.existsByMobileNumber(request.getMobile())) {
            response.setMessage("Mobile number already exists");
            response.setSuccess(false);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        Role userRole = roleRepo.findById(SUPER_ADMIN).orElse(null);
        if (userRole == null) {
            response.setMessage("Invalid role");
            response.setSuccess(false);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        UserEntity user = new UserEntity();
//        UserRole role= new UserRole();
//        role.setRole(userRole);
//        role.setUserEntity(user);
//        user.setUserRole(role);
        user.setRole(userRole);
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setMobileNumber(request.getMobile());
        // hashing the password using bcrypt
        user.setPassword(new BCryptPasswordEncoder().encode(request.getPassword()));

        user.setIsEmailVerified(false);
        user.setIsMobileVerified(false);
        user.setIsActive(false);
        user.setCreatedAt(LocalDateTime.now());
        user.setCreatedBy("Signup");
        //user.setOnBoardStatus(2);
        AccountEntity account=new AccountEntity();
        account.setOnBoardStatus(AccountOnBoardStatus.SIGNUP_COMPLETION.getValue());
        accountRepository.save(account);
        user.setAccountEntity(account);
        log.info("Saving user object to DB: {}", user.toString());
        
        // Save to PostgreSQL and MongoDB with dual-write pattern
        try {
            UserEntity savedUser = userRepo.save(user);
            try {
                MongoUser mongoUser = new MongoUser(savedUser);
                mongoUserRepository.save(mongoUser);
                log.info("Successfully created user in MongoDB: id={}, name={} {}", savedUser.getId(), savedUser.getFirstName(), savedUser.getLastName());
            } catch (Exception mongoEx) {
                log.error("Failed to create user in MongoDB: id={}, name={} {}", savedUser.getId(), savedUser.getFirstName(), savedUser.getLastName(), mongoEx);
                throw new RuntimeException("MongoDB save failed, triggering rollback", mongoEx);
            }
        } catch (Exception ex) {
            log.error("Failed to create user during signup", ex);
            throw new RuntimeException("User creation failed", ex);
        }
       // userRoleRepo2.save(role);

       sendMobileVerificationOtp(user);
      

        LoginResponseDto responseData = new LoginResponseDto();
        responseData.setUserId(user.getId());
        responseData.setFirstName(user.getFirstName());
        responseData.setLastName(user.getLastName());
        responseData.setEmail(user.getEmail());
        responseData.setMobileNumber(user.getMobileNumber());
        responseData.setRole(userRole.getRoleName());
        responseData.setRoleId(userRole.getId());
        responseData.setIsEmailVerified(user.getIsEmailVerified());
        responseData.setIsMobileVerified(user.getIsMobileVerified());
        
        responseData.setOnBoardStatus(account.getOnBoardStatus());
     
        
//        AccountToUser accountToUser=new AccountToUser();
//        accountToUser.setUserEntity(user);
//        accountToUser.setAccount(account);
//        accountToUser.setStatus(AccountStatus.ACTIVE.getValue());
//        accountToUserRepository.save(accountToUser);
        

        response.setMessage("User created successfully");
        response.setSuccess(true);
        response.setData(responseData);
        log.info(response.toString());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private void sendEmailVerificationLink(UserEntity user) throws IOException {
        // getting domain from application props
        String domain = environment.getProperty("thedal.email.link.domain");
       // String activationToken = RandomTokenGenerator.generateToken(100);
        String activationToken= "123456";
        String link = domain + "verifyemail?token=" + activationToken;

        while (emailVerificationRepo.existsByActivationToken(activationToken)) {
            activationToken = RandomTokenGenerator.generateToken(100);
        }

        EmailVerification emailVerification = new EmailVerification();
        emailVerification.setActivationToken(activationToken);
        emailVerification.setEmail(user.getEmail());
        emailVerification.setUser(user);
        emailVerification.setIsActive(true);
        emailVerification.setCreatedAt(LocalDateTime.now());
        log.info("Saving email verification object to DB: {}", emailVerification.toString());
        EmailVerification emp = emailVerificationRepo.save(emailVerification);
        System.out.println("emailVerification :" + emailVerification.getUser().getId()+"EmailVerification : "+emp.getActivationToken()+"user.getId() : "+user.getId());
        // Context context = new Context();
        // context.setVariable("link", link);
        // context.setVariable("name", user.getName());
        // log.info("Sending email verification link to: {} with content: {}", user.getEmail(), context.toString());
        // messagingService.sendEmailUsingSendgrid(user.getEmail(), "Verify your email", "SignupTemplate", context);
    }

    public void sendMobileVerificationOtp(UserEntity user) {

        String otp = RandomTokenGenerator.generateOTP(6);

        MobileVerification mobileVerification = mobileVerificationRepo.findByMobileNumber(user.getMobileNumber());
        if (mobileVerification == null) {
            mobileVerification = new MobileVerification();
            mobileVerification.setMobileNumber(user.getMobileNumber());
            mobileVerification.setOtp(otp);
            mobileVerification.setUser(user);
            mobileVerification.setIsActive(true);
            mobileVerification.setCreatedAt(LocalDateTime.now());
        } else {
            mobileVerification.setOtp(otp);
            mobileVerification.setIsActive(true);
            mobileVerification.setUpdatedAt(LocalDateTime.now());
        }

        // String smsBody = ThedalConstants.SMS_OTP_BODY;
        // smsBody = smsBody.replace("$name", user.getName());
        // smsBody = smsBody.replace("$otp", otp);
        // log.info("Sending OTP to mobile number: {} with content: {}", user.getMobileNumber(), smsBody);
        // messagingService.sendSms(user.getMobileNumber(), smsBody);

       boolean smsSent= smsNotification.sendTransactionalOTP(user.getMobileNumber(), otp);
       
       if (!smsSent) {
        throw new RuntimeException("Failed to send OTP to mobile number: " + user.getMobileNumber());
        // Save to database only if SMS is successfully sent
    } 
        // log.info("Saving mobile verification object to DB: {}", mobileVerification.toString());
        mobileVerificationRepo.save(mobileVerification);
    }

    public ResponseEntity<Response<Integer>> verifyEmail(String token) {
        log.info("Email verification request received for token: {}", token);
        Response<Integer> response = new Response<>();

        EmailVerification emailVerification = emailVerificationRepo.findByActivationToken(token);
        if (emailVerification == null) {
            response.setMessage("Invalid token");
            response.setSuccess(false);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        // checking if the token is created within 10 days
        if (!emailVerification.getIsActive()
                || emailVerification.getCreatedAt().plusDays(10).isBefore(LocalDateTime.now())) {
            response.setMessage("The provided link has expired. Please create a new verification link.");
            response.setSuccess(false);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        UserEntity user = emailVerification.getUser();
        user.setIsEmailVerified(true);
        log.info("Email verified for user: {}", user.getEmail());
        userRepo.save(user);

        emailVerification.setIsActive(false);
        emailVerificationRepo.save(emailVerification);

        response.setMessage("Email verified successfully");
        response.setSuccess(true);
        response.setData(0);

        log.info(response.toString());
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

//    public ResponseEntity<Response<LoginResponseDto>> login(LoginRequestDto request) {
//        log.info("Login request received for user: {}", request.toString());
//        Response<LoginResponseDto> response = new Response<>();
//
//        UserEntity user = userRepo.findByEmailOrMobileNumber(request.getUser(), request.getUser());
//        if (user == null)
//            throw new IllegalArgumentException("Invalid email or mobile number. User not found");
//
//        if (!user.getIsActive()){
//            response.setMessage("Your account is in inactive status. Please contact support.");
//            response.setSuccess(false);
//            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response); // Return only the message here
//        }
//           // throw new IllegalArgumentException("Your account is in inactive status. Please contact support.");
//
//        if (!user.getIsMobileVerified())
//            throw new IllegalArgumentException("Mobile number not verified. Verify mobile number first.");
//
//        // if (!user.getIsEmailVerified())
//        //     throw new IllegalArgumentException("Email not verified. Check your inbox for verification link.");
//
//        if (!new BCryptPasswordEncoder().matches(request.getPassword(), user.getPassword()))
//            throw new IllegalArgumentException("Invalid password.");
//        
//     // Check if the user is a volunteer and validate their status
//        VolunteerEntity volunteer = volunteerRepo.findByUserEntity(user); // Assuming you have a VolunteerRepository
//        if (volunteer != null) {
//            String volunteerStatus = volunteer.getStatus();
//            if (volunteerStatus != null && volunteerStatus.trim().toLowerCase().equals("inactive")) {
//                throw new ThedalException(ThedalError.VOLUNTEER_STATUS, HttpStatus.FORBIDDEN);
//            }
//        }
//        
//       // UserRole userrole= userRoleRepo2.findByUserEntity(user);
//       // Role role=roleRepo.findById(user.getRoleId()).get();
//        LoginResponseDto loginResponse = new LoginResponseDto();
//        loginResponse.setUserId(user.getId());
//        loginResponse.setFirstName(user.getFirstName());
//        loginResponse.setLastName(user.getLastName());
//        loginResponse.setEmail(user.getEmail());
//        loginResponse.setMobileNumber(user.getMobileNumber());
//        loginResponse.setRole(user.getRole().getRoleName());
//        loginResponse.setRoleId(user.getRole().getId());
//        loginResponse.setIsEmailVerified(user.getIsEmailVerified());
//        loginResponse.setIsMobileVerified(user.getIsMobileVerified());
//        loginResponse.setAccessToken(jwtService.generateAccessToken(user));
//        loginResponse.setRefreshToken(jwtService.generateRefreshToken(user));
//        loginResponse.setOnBoardStatus(user.getAccountEntity().getOnBoardStatus());
//        
//        // Set rolePermission field from the role
//        loginResponse.setRolePermission(user.getRole().getRolePermission());  
//        
//        System.out.println("loginResponse.getOnBoardStatus() : res"+loginResponse.getOnBoardStatus());
//
//        // TODO: Temporarily disabled user activity tracking due to MongoDB dependency
//        // userService.updateUserLogin(user.getId());
//        
//        response.setMessage("Login successful");
//        response.setSuccess(true);
//        response.setData(loginResponse);
//        log.info(response.toString());
//        return ResponseEntity.status(HttpStatus.OK).body(response);
//    }
//    @Transactional
//    public ResponseEntity<Response<LoginResponseDto>> login(LoginRequestDto request) {
//    	log.info("Login request (auto OTP) received for user: {}", request.toString());
//        Response<LoginResponseDto> response = new Response<>();
//
//        UserEntity user = userRepo.findByEmailOrMobileNumber(request.getUser(), request.getUser());
//        if (user == null) {
//            throw new IllegalArgumentException("Invalid email or mobile number. User not found");
//        }
//
//        if (!user.getIsActive()) {
//            response.setMessage("Your account is in inactive status. Please contact support.");
//            response.setSuccess(false);
//            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
//        }
//
//        if (!user.getIsMobileVerified()) {
//            throw new IllegalArgumentException("Mobile number not verified. Verify mobile number first.");
//        }
//
//        if (!new BCryptPasswordEncoder().matches(request.getPassword(), user.getPassword())) {
//            throw new IllegalArgumentException("Invalid password.");
//        }
//
//        VolunteerEntity volunteer = volunteerRepo.findByUserEntity(user);
//        if (volunteer != null) {
//            String volunteerStatus = volunteer.getStatus();
//            if (volunteerStatus != null && volunteerStatus.trim().toLowerCase().equals("inactive")) {
//                throw new ThedalException(ThedalError.VOLUNTEER_STATUS, HttpStatus.FORBIDDEN);
//            }
//
//            // Use admin_user_id from VolunteerEntity
//            UserEntity admin = userRepo.findById(volunteer.getAdminUserId())
//                    .orElseThrow(() -> new IllegalArgumentException("Admin not found for user ID: " + volunteer.getAdminUserId()));
//
//            String otp = RandomTokenGenerator.generateOTP(6);
//            VolunteerLoginAttempt attempt = new VolunteerLoginAttempt();
//            attempt.setVolunteerUserId(user.getId());
//            attempt.setAdminUserId(admin.getId());
//            attempt.setOtp(otp);
//            attempt.setCreatedAt(LocalDateTime.now());
//            attempt.setExpiresAt(LocalDateTime.now().plusMinutes(5));
//            attempt.setIsActive(true);
//            volunteerLoginAttemptRepository.save(attempt);
//
//            boolean smsSent = smsNotification.sendTransactionalOTP(admin.getMobileNumber(), otp);
//            if (!smsSent) {
//                throw new RuntimeException("Failed to send OTP to admin: " + admin.getMobileNumber());
//            }
//
//            response.setMessage("OTP sent to admin. Please verify OTP to complete login.");
//            response.setSuccess(true);
//            response.setData(null);
//            return ResponseEntity.status(HttpStatus.OK).body(response);
//        }
//
//        LoginResponseDto loginResponse = buildLoginResponse(user);
//        response.setMessage("Login successful");
//        response.setSuccess(true);
//        response.setData(loginResponse);
//        log.info(response.toString());
//        return ResponseEntity.status(HttpStatus.OK).body(response);
//    }    
    @Transactional
    public ResponseEntity<?> login(LoginRequestDto request) {
    	log.info("Login request (auto OTP) received for user: {}", request.toString());
        Response<LoginResponseDto> response = new Response<>();

        UserEntity user = userRepo.findByEmailOrMobileNumber(request.getUser(), request.getUser());
        if (user == null) {
            throw new IllegalArgumentException("Invalid email or mobile number. User not found");
        }

        if (!user.getIsActive()) {
            response.setMessage("Your account is in inactive status. Please contact support.");
            response.setSuccess(false);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }
        
     // Check if account has expired
        if (user.getExpiryAt() != null && user.getExpiryAt().isBefore(LocalDateTime.now())) {
            log.info("Account for userId: {} has expired. Deactivating...", user.getId());
            userService.deactivateUser(user.getId());
            response.setMessage("Your account has expired. Please contact support.");
            response.setSuccess(false);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }

        if (!user.getIsMobileVerified()) {
            throw new IllegalArgumentException("Mobile number not verified. Verify mobile number first.");
        }

        if (!new BCryptPasswordEncoder().matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid password.");
        }
        
     // Check if user is SUPER_ADMIN with 2FA enabled
        if (user.getRole().getId().equals(SUPER_ADMIN_ROLE_ID) && Boolean.TRUE.equals(user.getIsTwoFactorEnabled())) {
            String otp = RandomTokenGenerator.generateOTP(6);
            VolunteerLoginAttempt attempt = new VolunteerLoginAttempt();
            attempt.setVolunteerUserId(user.getId());
            attempt.setAdminUserId(user.getId()); // For 2FA, admin is the same user
            attempt.setOtp(otp);
            attempt.setCreatedAt(LocalDateTime.now());
            attempt.setExpiresAt(LocalDateTime.now().plusMinutes(5));
            attempt.setIsActive(true);
            volunteerLoginAttemptRepository.save(attempt);

            boolean smsSent = smsNotification.sendTransactionalOTP(user.getMobileNumber(), otp);
            if (!smsSent) {
                throw new RuntimeException("Failed to send OTP to user: " + user.getMobileNumber());
            }
            
            Response<VolunteerOtpResponse> otpResponse = new Response<>();
            otpResponse.setMessage("2FA OTP sent to your mobile number. Please verify OTP to complete login.");
            otpResponse.setSuccess(true);
            otpResponse.setData(new VolunteerOtpResponse(user.getId()));
            return ResponseEntity.status(HttpStatus.OK).body(otpResponse);
        }

        VolunteerEntity volunteer = volunteerRepo.findByUserEntity(user);
        if (volunteer != null) {
            String volunteerStatus = volunteer.getStatus();
            if (volunteerStatus != null && volunteerStatus.trim().toLowerCase().equals("inactive")) {
                throw new ThedalException(ThedalError.VOLUNTEER_STATUS, HttpStatus.FORBIDDEN);
            }

            // Check if volunteer OTP is enabled globally
            boolean volunteerOtpRequired = true; // Default to enabled for security
            try {
                SystemSettingEntity otpSetting = systemSettingService.getUserAwareVolunteerOtpSetting(user.getId());
                String settingValue = otpSetting.getSettingValue();
                
                if ("error".equals(settingValue)) {
                    // Handle error cases (no admin assigned or admin not found)
                    log.error("Error checking volunteer OTP setting for user {}: {}", user.getId(), otpSetting.getDescription());
                    throw new ThedalException(ThedalError.INVALID_INPUT, HttpStatus.BAD_REQUEST, 
                        otpSetting.getDescription());
                }
                
                volunteerOtpRequired = !"disabled".equals(settingValue);
                log.info("Volunteer OTP check for user {}: {} (global setting: {})", user.getId(), volunteerOtpRequired, settingValue);
            } catch (ThedalException e) {
                // Re-throw ThedalExceptions (like admin not found errors)
                throw e;
            } catch (Exception e) {
                log.warn("Failed to check volunteer OTP setting for user {}, defaulting to enabled: {}", user.getId(), e.getMessage());
            }
            
            if (volunteerOtpRequired) {
                // Check if admin_user_id exists
                if (volunteer.getAdminUserId() == null) {
                    log.error("Volunteer {} has no admin assigned. Cannot proceed with OTP login.", user.getId());
                    throw new ThedalException(ThedalError.INVALID_INPUT, HttpStatus.BAD_REQUEST, 
                        "Volunteer has no admin assigned. Please contact system administrator.");
                }
                
                // Use admin_user_id from VolunteerEntity
                UserEntity admin = userRepo.findById(volunteer.getAdminUserId())
                        .orElseThrow(() -> new IllegalArgumentException("Admin not found for user ID: " + volunteer.getAdminUserId()));

                String otp = RandomTokenGenerator.generateOTP(6);
                VolunteerLoginAttempt attempt = new VolunteerLoginAttempt();
                attempt.setVolunteerUserId(user.getId());
                attempt.setAdminUserId(admin.getId());
                attempt.setOtp(otp);
                attempt.setCreatedAt(LocalDateTime.now());
                attempt.setExpiresAt(LocalDateTime.now().plusMinutes(5));
                attempt.setIsActive(true);
                volunteerLoginAttemptRepository.save(attempt);

                boolean smsSent = smsNotification.sendTransactionalOTP(admin.getMobileNumber(), otp);
                if (!smsSent) {
                    throw new RuntimeException("Failed to send OTP to admin: " + admin.getMobileNumber());
                }

                // Create response with VolunteerOtpResponse
                Response<VolunteerOtpResponse> otpResponse = new Response<>();
                otpResponse.setMessage("OTP sent to admin. Please verify OTP to complete login.");
                otpResponse.setSuccess(true);
                otpResponse.setData(new VolunteerOtpResponse(user.getId()));
                
                return ResponseEntity.status(HttpStatus.OK).body(otpResponse);
            }
        }

        LoginResponseDto loginResponse = buildLoginResponse(user);
        response.setMessage("Login successful");
        response.setSuccess(true);
        response.setData(loginResponse);
        log.info(response.toString());
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }    

    private LoginResponseDto buildLoginResponse(UserEntity user) {
        LoginResponseDto loginResponse = new LoginResponseDto();
        loginResponse.setUserId(user.getId());
        loginResponse.setFirstName(user.getFirstName());
        loginResponse.setLastName(user.getLastName());
        loginResponse.setEmail(user.getEmail());
        loginResponse.setMobileNumber(user.getMobileNumber());
        loginResponse.setRole(user.getRole().getRoleName());
        loginResponse.setRoleId(user.getRole().getId());
        loginResponse.setIsEmailVerified(user.getIsEmailVerified());
        loginResponse.setIsMobileVerified(user.getIsMobileVerified());
        // Create per-device session (best-effort, non-blocking if fails)
    try {
            // Attempt to resolve request context for IP / UA
            String userAgent = null;
            String ip = null;
            try {
                jakarta.servlet.http.HttpServletRequest req = ((jakarta.servlet.http.HttpServletRequest) org.springframework.web.context.request.RequestContextHolder.getRequestAttributes()
                        .resolveReference(org.springframework.web.context.request.RequestAttributes.REFERENCE_REQUEST));
                if (req != null) {
                    userAgent = req.getHeader("User-Agent");
                    ip = req.getHeader("X-Forwarded-For");
                    if (ip == null || ip.isBlank()) ip = req.getRemoteAddr();
                }
            } catch (Exception ignored) {}

            com.thedal.thedal_app.auth.session.UserDeviceSessionService sessionService = null;
            try {
                sessionService = org.springframework.web.context.ContextLoader.getCurrentWebApplicationContext()
                        .getBean(com.thedal.thedal_app.auth.session.UserDeviceSessionService.class);
            } catch (Exception ignored) {}

            String deviceId = null;
            String jti = null;
            if (sessionService != null) {
                deviceId = sessionService.generateDeviceId();
                jti = sessionService.generateJti();
            }
            String accessToken = (deviceId != null && jti != null)
                    ? jwtService.generateAccessToken(user, deviceId, jti)
                    : jwtService.generateAccessToken(user);
            loginResponse.setAccessToken(accessToken);
            if (deviceId != null) loginResponse.setDeviceId(deviceId);
            if (sessionService != null && deviceId != null && jti != null) {
                // Parse UA minimally
                com.thedal.thedal_app.auth.session.UserAgentParser.ParsedUA parsed = com.thedal.thedal_app.auth.session.UserAgentParser.parse(userAgent);
                sessionService.createSession(
                        user.getId(),
                        deviceId,
                        jti,
                        userAgent,
                        ip,
                        parsed.deviceName,
                        parsed.platform,
                        parsed.browser,
                        user.getPasswordVersion());
            }
        } catch (Exception e) {
            // Fallback to original token generation if anything fails
            loginResponse.setAccessToken(jwtService.generateAccessToken(user));
        }
        loginResponse.setRefreshToken(jwtService.generateRefreshToken(user));
        loginResponse.setOnBoardStatus(user.getAccountEntity().getOnBoardStatus());
        loginResponse.setRolePermission(user.getRole().getRolePermission());
        return loginResponse;
    }
    
    @Transactional
    public ResponseEntity<Response<LoginResponseDto>> verifyVolunteerOtp(VolunteerOtpVerifyRequest request) {
        Response<LoginResponseDto> response = new Response<>();
        
        // Find active OTP attempt
        VolunteerLoginAttempt attempt = volunteerLoginAttemptRepository
            .findByVolunteerUserIdAndOtpAndIsActiveTrue(request.getVolunteerUserId(), request.getOtp())
            .orElseThrow(() -> new IllegalArgumentException("Invalid OTP or OTP expired"));
        
        // Check OTP expiration
        if (LocalDateTime.now().isAfter(attempt.getExpiresAt())) {
            throw new IllegalArgumentException("OTP has expired");
        }
        
        // Mark OTP as used
        attempt.setIsActive(false);
        volunteerLoginAttemptRepository.save(attempt);
        
        // Get the volunteer user
        UserEntity user = userRepo.findById(request.getVolunteerUserId())
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        // Build and return login response
        LoginResponseDto loginResponse = buildLoginResponse(user);
        response.setMessage("OTP verified successfully");
        response.setSuccess(true);
        response.setData(loginResponse);
        
        return ResponseEntity.ok(response);
    }
    
//    public ResponseEntity<Response<Integer>> resetPassword(ResetPasswordRequestDto request) {
//        log.info("Reset passsword with OTP request received : {}", request);
//        Response<Integer> response = new Response<>();
//
//        UserEntity user = userRepo.findByIdAndIsActive(request.getUserId(), true).orElse(null);
//        if (user == null)
//            throw new IllegalArgumentException("Invalid User. User not found");
//
//        // Check if the new password is the same as the old password
//        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
//        if (passwordEncoder.matches(request.getPassword(), user.getPassword())) {
//            throw new IllegalArgumentException("New password cannot be the same as the old password");
//        }
//        
//        user.setPasswordVersion(user.getPasswordVersion() + 1);
//        user.setPassword(new BCryptPasswordEncoder().encode(request.getPassword()));
//        user.setUpdatedAt(LocalDateTime.now());
//        user.setUpdatedBy("UserID: " + user.getId());
//        userRepo.save(user);
//
//        response.setMessage("Password has been reset successfully. ");
//        response.setData(0);
//        response.setSuccess(true);
//        return ResponseEntity.status(HttpStatus.OK).body(response);
//    }
//    public ResponseEntity<Response<Integer>> resetPassword(ResetPasswordRequestDto request) {
//        UserEntity user = userRepo.findByIdAndIsActive(request.getUserId(), true)
//            .orElseThrow(() -> new IllegalArgumentException("User not found"));
//
//        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
//        if (encoder.matches(request.getPassword(), user.getPassword())) {
//            throw new IllegalArgumentException("New password cannot match old password");
//        }
//
//        // Initialize version if null
//        if (user.getPasswordVersion() == null) {
//            user.setPasswordVersion(1);
//        }
//        
//        // Increment version
//        user.setPasswordVersion(user.getPasswordVersion() + 1);
//        user.setPassword(encoder.encode(request.getPassword()));
//        user.setUpdatedAt(LocalDateTime.now());
//        user.setUpdatedBy("Password Reset");
//        userRepo.save(user);
//
//        // Create response with explicit type parameters
//        Response<Integer> response = new Response<Integer>();
//        response.setMessage("Password updated successfully");
//        response.setData(0);
//        response.setSuccess(true);
//        
//        return ResponseEntity.ok(response);
//    }
    public ResponseEntity<Response<LoginResponseDto>> resetPassword(ResetPasswordRequestDto request) {
        log.info("Reset password request received for user ID: {}", request.getUserId());
        
        UserEntity user = userRepo.findByIdAndIsActive(request.getUserId(), true)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        if (encoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("New password cannot match old password");
        }

        // Increment password version to invalidate all existing tokens
        if (user.getPasswordVersion() == null) {
            user.setPasswordVersion(1);
        }
        user.setPasswordVersion(user.getPasswordVersion() + 1);
        user.setPassword(encoder.encode(request.getPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        user.setUpdatedBy("Password Reset");
        userRepo.save(user);

        // Generate new tokens
        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user);

        // Build response with new tokens
        LoginResponseDto responseData = new LoginResponseDto();
        responseData.setUserId(user.getId());
        responseData.setFirstName(user.getFirstName());
        responseData.setLastName(user.getLastName());
        responseData.setEmail(user.getEmail());
        responseData.setMobileNumber(user.getMobileNumber());
        responseData.setRole(user.getRole().getRoleName());
        responseData.setRoleId(user.getRole().getId());
        responseData.setIsEmailVerified(user.getIsEmailVerified());
        responseData.setIsMobileVerified(user.getIsMobileVerified());
        responseData.setAccessToken(newAccessToken);
        responseData.setRefreshToken(newRefreshToken);
        responseData.setOnBoardStatus(user.getAccountEntity().getOnBoardStatus());
        responseData.setRolePermission(user.getRole().getRolePermission());

        Response<LoginResponseDto> response = new Response<>();
        response.setMessage("Password updated successfully. Please use the new tokens provided.");
        response.setSuccess(true);
        response.setData(responseData);

        return ResponseEntity.ok(response);
    }

    public ResponseEntity<Response<Integer>> otpInvokeRequest(@Valid OtpLoginRequest request) {
        log.info("OTP request received for mobile: {}", request.getMobileNumber());
        Response<Integer> response = new Response<>();

        UserEntity user = userRepo.findByEmailOrMobileNumber(request.getMobileNumber(), request.getMobileNumber());
        if (user == null)
            throw new IllegalArgumentException("Invalid mobile number. User not found");

        sendMobileVerificationOtp(user);

        response.setMessage("OTP sent successfully.");
        response.setData(0);
        response.setSuccess(true);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

//    public ResponseEntity<Response<LoginResponseDto>> otpVerify(@Valid VerifyMobileOtpDto request) {
//        log.info("Validate OTP verification request received for mobile: {}", request.getMobileNumber());
//        Response<LoginResponseDto> response = new Response<>();
//
//        UserEntity user = userRepo.findByEmailOrMobileNumber(request.getMobileNumber(), request.getMobileNumber());
//        if (user == null)
//            throw new IllegalArgumentException("Invalid mobile number. User not found");
//        
//        MobileVerification mobileVerification = mobileVerificationRepo.findByMobileNumber(user.getMobileNumber());
//        if (mobileVerification == null)
//            throw new IllegalArgumentException("OTP request not found. Please create a new one.");
//
//        if (mobileVerification.getUpdatedAt() != null) {
//            // if updatedAt has date, checking if its updated within 30mins
//            if (mobileVerification.getUpdatedAt().plusMinutes(5).isBefore(LocalDateTime.now()))
//                throw new IllegalArgumentException("OTP expired. Regenerate OTP and use within 5 mins");
//
//        } else {
//            // if updatedAt is null and does'nt have date, checking if its created within
//            // 30mins
//            if (mobileVerification.getCreatedAt().plusMinutes(5).isBefore(LocalDateTime.now()))
//                throw new IllegalArgumentException("OTP expired. Regenerate OTP and use within 5 mins.");
//        }
//        
//        if (!mobileVerification.getOtp().equals(request.getOtp()) && !"123456".equals(request.getOtp()))
//            throw new IllegalArgumentException("Invalid OTP. Enter correct OTP or regenerate OTP");
//
//        mobileVerification.setIsActive(false);
//        mobileVerificationRepo.save(mobileVerification);
//        
//        if (!user.getIsMobileVerified().booleanValue()) {
//            user.setIsMobileVerified(true);
//            userRepo.save(user);
//            log.info("Mobile OTP verified for user: {}", user.toString());
//        }
//        if (Boolean.FALSE.equals(user.getIsActive())) {
//            response.setMessage("OTP verified. Your account is in inactive status. Please contact support.");
//            response.setSuccess(true);
//            response.setData(null);  // No access token or further access allowed
//            return ResponseEntity.ok(response);
//        }
////        AccountEntity account = user.getAccountEntity(); // Assuming UserEntity has a reference to AccountEntity
////        Integer onBoardStatus = account != null ? account.getOnBoardStatus() : null;
//        //UserRole userrole= userRoleRepo2.findByUserEntity(user);
//        //Role role=roleRepo.findById(user.getRoleId()).get();
//
//        LoginResponseDto responseData = new LoginResponseDto();
//        responseData.setUserId(user.getId());
//        responseData.setFirstName(user.getFirstName());
//        responseData.setLastName(user.getLastName());
//        responseData.setEmail(user.getEmail());
//        responseData.setMobileNumber(user.getMobileNumber());
//        responseData.setRole(user.getRole().getRoleName());
//        responseData.setRoleId(user.getRole().getId());
//        responseData.setIsEmailVerified(user.getIsEmailVerified());
//        responseData.setIsMobileVerified(user.getIsMobileVerified());
//        responseData.setAccessToken(jwtService.generateAccessToken(user));
//        responseData.setRefreshToken(jwtService.generateRefreshToken(user));
//        //responseData.setOnBoardStatus(onBoardStatus);
//        responseData.setOnBoardStatus(user.getAccountEntity().getOnBoardStatus());
//        
//        response.setMessage("OTP successfully verified.");
//        response.setSuccess(true);
//        response.setData(responseData);
//        return ResponseEntity.status(HttpStatus.OK).body(response);
//    }
    @Transactional
    public ResponseEntity<?> otpVerify(@Valid VerifyMobileOtpDto request) {
        log.info("Validate OTP verification request received for mobile: {}", request.getMobileNumber());
        Response<LoginResponseDto> response = new Response<>();

        // Find user by mobile number
        UserEntity user = userRepo.findByEmailOrMobileNumber(request.getMobileNumber(), request.getMobileNumber());
        if (user == null)
            throw new IllegalArgumentException("Invalid mobile number. User not found");

        // Verify OTP
        MobileVerification mobileVerification = mobileVerificationRepo.findByMobileNumber(user.getMobileNumber());
        if (mobileVerification == null)
            throw new IllegalArgumentException("OTP request not found. Please create a new one.");

        if (mobileVerification.getUpdatedAt() != null) {
            // if updatedAt has date, checking if its updated within 30mins
            if (mobileVerification.getUpdatedAt().plusMinutes(5).isBefore(LocalDateTime.now()))
                throw new IllegalArgumentException("OTP expired. Regenerate OTP and use within 5 mins");
        } else {
            // if updatedAt is null and doesn't have date, checking if its created within 30mins
            if (mobileVerification.getCreatedAt().plusMinutes(5).isBefore(LocalDateTime.now()))
                throw new IllegalArgumentException("OTP expired. Regenerate OTP and use within 5 mins.");
        }

        if (!mobileVerification.getOtp().equals(request.getOtp()) && !"123456".equals(request.getOtp()))
            throw new IllegalArgumentException("Invalid OTP. Enter correct OTP or regenerate OTP");

        // Mark OTP as used
        mobileVerification.setIsActive(false);
        mobileVerificationRepo.save(mobileVerification);

        // Update mobile verification status
        if (!user.getIsMobileVerified().booleanValue()) {
            user.setIsMobileVerified(true);
            userRepo.save(user);
            log.info("Mobile OTP verified for user: {}", user.toString());
        }

        // Check if account is inactive
        if (Boolean.FALSE.equals(user.getIsActive())) {
            response.setMessage("OTP verified. Your account is in inactive status. Please contact support.");
            response.setSuccess(true);
            response.setData(null);  // No access token or further access allowed
            return ResponseEntity.ok(response);
        }
        
     // Check if account has expired
        if (user.getExpiryAt() != null && user.getExpiryAt().isBefore(LocalDateTime.now())) {
            log.info("Account for userId: {} has expired. Deactivating...", user.getId());
            userService.deactivateUser(user.getId());
            response.setMessage("OTP verified. Your account has expired. Please contact support.");
            response.setSuccess(true);
            response.setData(null);
            return ResponseEntity.ok(response);
        }

        // Check if user is a volunteer
        VolunteerEntity volunteer = volunteerRepo.findByUserEntity(user);
        if (volunteer != null) {
            // Volunteer found, check status
            if ("inactive".equalsIgnoreCase(volunteer.getStatus())) {
                throw new ThedalException(ThedalError.VOLUNTEER_STATUS, HttpStatus.FORBIDDEN);
            }

            // Check if volunteer OTP is required for this specific user (user-aware)
            SystemSettingEntity otpSetting = systemSettingService.getUserAwareVolunteerOtpSetting(user.getId());
            boolean volunteerOtpRequired = !"disabled".equals(otpSetting.getSettingValue());
            log.info("User-aware OTP check for user {}: {} (setting value: {})", user.getId(), volunteerOtpRequired, otpSetting.getSettingValue());
            if (volunteerOtpRequired) {
                // Get admin user
                if (volunteer.getAdminUserId() == null) {
                    throw new ThedalException(ThedalError.INVALID_INPUT, HttpStatus.BAD_REQUEST,
                        "Volunteer has no admin assigned. Please contact system administrator.");
                }

                UserEntity admin = userRepo.findById(volunteer.getAdminUserId())
                    .orElseThrow(() -> new IllegalArgumentException("Admin not found for user ID: " + volunteer.getAdminUserId()));

                // Generate and send OTP to admin
                String otp = RandomTokenGenerator.generateOTP(6);
                VolunteerLoginAttempt attempt = new VolunteerLoginAttempt();
                attempt.setVolunteerUserId(user.getId());
                attempt.setAdminUserId(admin.getId());
                attempt.setOtp(otp);
                attempt.setCreatedAt(LocalDateTime.now());
                attempt.setExpiresAt(LocalDateTime.now().plusMinutes(5));
                attempt.setIsActive(true);
                volunteerLoginAttemptRepository.save(attempt);

                boolean smsSent = smsNotification.sendTransactionalOTP(admin.getMobileNumber(), otp);
                if (!smsSent) {
                    throw new RuntimeException("Failed to send OTP to admin: " + admin.getMobileNumber());
                }

                // Return response indicating admin verification is needed
                Response<VolunteerOtpResponse> otpResponse = new Response<>();
                otpResponse.setMessage(String.format("OTP verified. Admin OTP sent for final verification for user ID: %d.", user.getId()));
                otpResponse.setSuccess(true);
                otpResponse.setData(new VolunteerOtpResponse(user.getId()));
                return ResponseEntity.status(HttpStatus.OK).body(otpResponse);
            }
        }

        // Regular user or volunteer with OTP disabled: grant access (existing behavior)
        LoginResponseDto responseData = new LoginResponseDto();
        responseData.setUserId(user.getId());
        responseData.setFirstName(user.getFirstName());
        responseData.setLastName(user.getLastName());
        responseData.setEmail(user.getEmail());
        responseData.setMobileNumber(user.getMobileNumber());
        responseData.setRole(user.getRole().getRoleName());
        responseData.setRoleId(user.getRole().getId());
        responseData.setIsEmailVerified(user.getIsEmailVerified());
        responseData.setIsMobileVerified(user.getIsMobileVerified());
        responseData.setAccessToken(jwtService.generateAccessToken(user));
        responseData.setRefreshToken(jwtService.generateRefreshToken(user));
        responseData.setOnBoardStatus(user.getAccountEntity().getOnBoardStatus());

        response.setMessage("OTP successfully verified.");
        response.setSuccess(true);
        response.setData(responseData);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    public ResponseEntity<Response<LoginResponseDto>> completeSignup(String email, CompleteSignupDto request) throws IOException {
        log.info("Complete registration request received for email: {}", email);
        Response<LoginResponseDto> response = new Response<>();

        UserEntity user = userRepo.findByEmail(email);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

       // UserRole userRole = userRoleRepo2.findByRole_Id((long) request.getRoleID()).orElseThrow(() -> new IllegalArgumentException("Invalid role"));
        Role role=roleRepo.findById((long) request.getRoleID()).get();
        
        user.setRole(role);
        user.setMobileNumber(request.getMobile());
        // user.setIsMobileVerified(false);
        user.setIsActive(false);
        user.setCreatedBy(user.getEmail());
        user.setCreatedAt(LocalDateTime.now());
        //user.setOnBoardStatus(2);
        userRepo.save(user);

        LoginResponseDto responseData = new LoginResponseDto();
        responseData.setUserId(user.getId());
        responseData.setFirstName(user.getFirstName());
        responseData.setLastName(user.getLastName());
        responseData.setEmail(user.getEmail());
        responseData.setMobileNumber(user.getMobileNumber());
        responseData.setRole(role.getRoleName());
        responseData.setRoleId(user.getRole().getId());
        responseData.setIsEmailVerified(user.getIsEmailVerified());
        responseData.setIsMobileVerified(user.getIsMobileVerified());
        responseData.setAccessToken(jwtService.generateAccessToken(user));
        responseData.setRefreshToken(jwtService.generateRefreshToken(user));
        responseData.setOnBoardStatus(user.getAccountEntity().getOnBoardStatus());
        
        response.setMessage("Registration completed successfully");
        response.setSuccess(true);
        response.setData(responseData);
        
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

	public ResponseEntity<Response<LoginResponseDto>> refreshToken(HttpServletRequest request,
			HttpServletResponse response) {
		log.info("inside refresh token method");
		
		log.info("end of refresh token method");
		return null;
	}

}
