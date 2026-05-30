package com.thedal.thedal_app.election;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ElectionDeleteOtpRepository extends JpaRepository<ElectionDeleteOtp, Long> {
    Optional<ElectionDeleteOtp> findByElectionIdAndMobileNumberAndOtpAndIsActiveTrue(Long electionId, String mobileNumber, String otp);
    
    Optional<ElectionDeleteOtp> findByUserIdAndElectionIdAndOtpAndIsActiveTrue(Long userId, Long electionId, String otp);
    Optional<ElectionDeleteOtp> findByUserIdAndOtpAndIsActiveTrue(Long userId, String otp);
    
    
}