package com.thedal.thedal_app.user;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OtpRequiredStatusChangeAttemptRepository extends JpaRepository<OtpRequiredStatusChangeAttempt, Long> {
    Optional<OtpRequiredStatusChangeAttempt> findByUserIdAndIsActiveTrue(Long userId);
    
    Optional<OtpRequiredStatusChangeAttempt> findByUserId(Long userId);
   
}