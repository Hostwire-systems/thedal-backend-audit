package com.thedal.thedal_app.auth;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.thedal.thedal_app.auth.dtos.CompleteSignupDto;
import com.thedal.thedal_app.auth.dtos.LoginRequestDto;
import com.thedal.thedal_app.auth.dtos.LoginResponseDto;
import com.thedal.thedal_app.auth.dtos.OAuthRequestDTO;
import com.thedal.thedal_app.auth.dtos.OtpLoginRequest;
import com.thedal.thedal_app.auth.dtos.ResetPasswordRequestDto;
import com.thedal.thedal_app.auth.dtos.SignupRequestDto;
import com.thedal.thedal_app.auth.dtos.VerifyMobileOtpDto;
import com.thedal.thedal_app.oauth2login.OAuthUserService;
import com.thedal.thedal_app.user.UserEntity;
import com.thedal.thedal_app.user.VolunteerOtpVerifyRequest;
import com.thedal.thedal_app.util.Response;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/auth")
@Slf4j
public class AuthController {

    @Autowired
    AuthService authService;

    @Autowired
    OAuthUserService oAuthUserService;


    @Operation(summary = "Ping",description="Check the server status")
    @GetMapping("/ping")
    public String ping() {
        return "Pong !!";
    }

    // @GetMapping("/google-login")
    // public void loginWithGoogle(HttpServletResponse response) throws IOException {
    //     response.sendRedirect("/oauth2/authorization/google"); // This will redirect to Google
    // }

    @Operation(summary = "Signup",description="The users can signup with this API")
    @PostMapping("/signup")
    public ResponseEntity<Response<LoginResponseDto>> signUp(@Valid @RequestBody SignupRequestDto request)
            throws IOException {
        return authService.signUp(request);
    }

    @Operation(summary = "Signup",description="The users can signup with this API")
    @PutMapping("oauth/signup")
    public UserEntity authsignUp(@Valid @RequestBody OAuthRequestDTO request) {
        return oAuthUserService.authsignUp(request);
    }

    @Operation(summary = "Complete Signup",description="Complete the remaining signup after OAuth.")
    @PostMapping("/signup/oauth-complete")
    public ResponseEntity<Response<LoginResponseDto>> completeSignup(
            @RequestParam String email,
            @Valid @RequestBody CompleteSignupDto request) throws IOException {
        return authService.completeSignup(email, request);
    }

//    @Operation(summary = "User Login",description="Authenticates a user (including Volunteers) and generates a JWT token for session management.")
//    @PostMapping("/login")
//    public ResponseEntity<Response<LoginResponseDto>> login(@Valid @RequestBody LoginRequestDto request) {
//        return authService.login(request);
//    }
    @Operation(summary = "User Login",description="Authenticates a user (including Volunteers) and generates a JWT token for session management.")
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDto request) {
        return authService.login(request);
    }   
    
    @Operation(summary = "Verify Volunteer Login OTP", description = "Verifies OTP sent to admin for volunteer login")
    @PostMapping("/verify-volunteer-otp")
    public ResponseEntity<Response<LoginResponseDto>> verifyVolunteerOtp(
        @Valid @RequestBody VolunteerOtpVerifyRequest request) {
        return authService.verifyVolunteerOtp(request);
    }
    
    @Operation(summary = "Generate refresh token",description="")
    @PostMapping("/refresh-token")
    public ResponseEntity<Response<LoginResponseDto>> refreshToken(HttpServletRequest httpServletRequest,HttpServletResponse httpServletResponse) {
        return authService.refreshToken(httpServletRequest,httpServletResponse);
    }

    @Operation(summary = "OTP Request",description="Sends an OTP to the user’s registered mobile number for authentication.")
    @PostMapping("/two-factor/otp/invoke")
    public ResponseEntity<Response<Integer>> otpInvokeRequest(@Valid @RequestBody OtpLoginRequest request) {
        return authService.otpInvokeRequest(request);
    }

    @Operation(summary = "Mobile Number Verification By OTP",description="Verifies the OTP sent to the user’s mobile number and generates a JWT token, along with user details. This API also checks if the user's account is active.")
    @PostMapping("/two-factor/otp/verify")
    public  ResponseEntity<?> otpVerify(@Valid @RequestBody VerifyMobileOtpDto request) {
        return authService.otpVerify(request);
    }

    @Operation(summary = "Email Verification",description="Verifies the Email of the user’s mobile number and generates a JWT token, along with user details. This API also checks if the user's account is active.")
    @GetMapping("/two-factor/email-verify/{token}")
    public ResponseEntity<Response<Integer>> verifyEmail(@PathVariable String token) {
        return authService.verifyEmail(token);
    }


    @Operation(summary = "Reset Password via OTP",description="Reset the password after verifying the OTP.")
    @PostMapping("/reset-password")
//    public ResponseEntity<Response<Integer>> resetPassword(
//            @Valid @RequestBody ResetPasswordRequestDto request) {
//        return authService.resetPassword(request);
//    }
    public ResponseEntity<Response<LoginResponseDto>> resetPassword(
            @Valid @RequestBody ResetPasswordRequestDto request) {
        return authService.resetPassword(request);
    }
    
}
