package com.thedal.thedal_app.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailVerificationRepo extends JpaRepository<EmailVerification, Long> {

    boolean existsByActivationToken(String activationToken);

    EmailVerification findByActivationToken(String token);
    
}
