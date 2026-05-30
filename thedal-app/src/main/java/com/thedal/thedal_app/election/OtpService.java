package com.thedal.thedal_app.election;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.thedal.thedal_app.notification.SmsNotification;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;
import com.thedal.thedal_app.user.UserEntity;
import com.thedal.thedal_app.util.RandomTokenGenerator;
import com.thedal.thedal_app.voter.FamilyMappingOtp;
import com.thedal.thedal_app.voter.FamilyMappingOtpRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class OtpService {

    @Autowired
    private ElectionDeleteOtpRepository otpRepository;

    @Autowired
    private FamilyMappingOtpRepository familyMappingOtpRepository;

    @Autowired
    private SmsNotification smsNotification;

    public ElectionDeleteOtp generateOtp(Long userId, Long electionId, String mobileNumber) {
        String otp = RandomTokenGenerator.generateOTP(6);
        ElectionDeleteOtp otpEntity = new ElectionDeleteOtp();
        UserEntity user = new UserEntity();
        user.setId(userId);
        otpEntity.setUser(user);
        otpEntity.setElectionId(electionId);
        otpEntity.setMobileNumber(mobileNumber);
        otpEntity.setOtp(otp);
        otpEntity.setIsActive(true);
        otpRepository.save(otpEntity);
        log.info("Generated OTP for userId: {}, electionId: {}", userId, electionId);
        return otpEntity;
    }

    public void sendOtp(String mobileNumber, String otp) {
        try {
            boolean smsSent = smsNotification.sendTransactionalOTP(mobileNumber, otp);
            if (!smsSent) {
                log.error("Failed to send OTP to mobile: {}", mobileNumber);
                throw new ThedalException(ThedalError.OTP_SEND_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
            }
            log.info("OTP sent to mobile: {}", mobileNumber);
        } catch (Exception e) {
            log.error("Failed to send OTP to mobile: {}", mobileNumber, e);
            throw new ThedalException(ThedalError.OTP_SEND_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public Long verifyOtp(Long userId, String otp) {
        Optional<ElectionDeleteOtp> otpEntityOpt = otpRepository.findByUserIdAndOtpAndIsActiveTrue(userId, otp);
        if (!otpEntityOpt.isPresent()) {
            log.warn("Invalid OTP for userId: {}", userId);
            throw new ThedalException(ThedalError.INVALID_OTP, HttpStatus.BAD_REQUEST);
        }
        ElectionDeleteOtp otpEntity = otpEntityOpt.get();
        if (otpEntity.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("Expired OTP for userId: {}", userId);
            throw new ThedalException(ThedalError.OTP_EXPIRED, HttpStatus.BAD_REQUEST);
        }
        otpEntity.setIsActive(false);
        otpRepository.save(otpEntity);
        log.info("OTP verified for userId: {}, electionId: {}", userId, otpEntity.getElectionId());
        return otpEntity.getElectionId();
    }

    // Family Mapping OTP Methods
    public FamilyMappingOtp generateFamilyMappingOtp(Long userId, Long electionId, String mobileNumber) {
        // Deactivate any existing OTPs for this election and mobile number
        familyMappingOtpRepository.deactivateAllByElectionIdAndMobileNumber(electionId, mobileNumber);
        
        String otp = RandomTokenGenerator.generateOTP(6);
        FamilyMappingOtp otpEntity = new FamilyMappingOtp();
        UserEntity user = new UserEntity();
        user.setId(userId);
        otpEntity.setUser(user);
        otpEntity.setElectionId(electionId);
        otpEntity.setMobileNumber(mobileNumber);
        otpEntity.setOtp(otp);
        otpEntity.setIsActive(true);
        familyMappingOtpRepository.save(otpEntity);
        log.info("Generated Family Mapping OTP for userId: {}, electionId: {}", userId, electionId);
        return otpEntity;
    }

    public Long verifyFamilyMappingOtp(Long electionId, String mobileNumber, String otp) {
        Optional<FamilyMappingOtp> otpEntityOpt = familyMappingOtpRepository.findLatestActiveByElectionIdAndMobileNumber(electionId, mobileNumber);
        if (!otpEntityOpt.isPresent()) {
            log.warn("Invalid Family Mapping OTP for electionId: {}, mobileNumber: {}", electionId, mobileNumber);
            throw new ThedalException(ThedalError.INVALID_OTP, HttpStatus.BAD_REQUEST);
        }
        FamilyMappingOtp otpEntity = otpEntityOpt.get();
        
        if (!otpEntity.getOtp().equals(otp)) {
            log.warn("OTP mismatch for electionId: {}, mobileNumber: {}", electionId, mobileNumber);
            throw new ThedalException(ThedalError.INVALID_OTP, HttpStatus.BAD_REQUEST);
        }
        
        if (otpEntity.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("Expired Family Mapping OTP for electionId: {}, mobileNumber: {}", electionId, mobileNumber);
            throw new ThedalException(ThedalError.OTP_EXPIRED, HttpStatus.BAD_REQUEST);
        }
        
        otpEntity.setIsActive(false);
        familyMappingOtpRepository.save(otpEntity);
        log.info("Family Mapping OTP verified for electionId: {}, mobileNumber: {}", electionId, mobileNumber);
        return otpEntity.getElectionId();
    }
}