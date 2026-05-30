package com.thedal.thedal_app.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

@Repository
public interface MobileVerificationRepo extends JpaRepository<MobileVerification, Long> {

    MobileVerification findByMobileNumber(String mobileNumber);
    
    @Modifying
    void deleteByUserId(Long userId);
}
