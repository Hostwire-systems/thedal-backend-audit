package com.thedal.thedal_app.user;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TwoFactorStatusChangeAttemptRepository extends JpaRepository<TwoFactorStatusChangeAttempt, Long> {
	 Optional<TwoFactorStatusChangeAttempt> findByUserIdAndOtpAndIsActiveTrue(Long userId, String otp);
	 Optional<TwoFactorStatusChangeAttempt> findByUserId(Long userId);
	    
//	Optional<TwoFactorStatusChangeAttempt> findByUserIdAndOtpAndIsActiveTrue(Long userId, String otp);
//    Optional<TwoFactorStatusChangeAttempt> findByUserIdAndIsActiveTrue(Long userId);
//    List<TwoFactorStatusChangeAttempt> findAllByUserIdAndIsActiveTrue(Long userId);
	
	
}