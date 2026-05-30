//package com.thedal.thedal_app.user;
//
//import jakarta.validation.constraints.NotBlank;
//import jakarta.validation.constraints.NotNull;
//import jakarta.validation.constraints.Pattern;
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//import lombok.Setter;
//
//@Getter
//@Setter
//@NoArgsConstructor
//public class CadreOtpVerifyRequest {
//    @NotNull(message = "Volunteer user ID is required")
//    private Long volunteerUserId;
//    
//    @NotBlank(message = "OTP is required")
//    @Pattern(regexp = "\\d{6}", message = "OTP must be 6 digits")
//    private String otp;
//}