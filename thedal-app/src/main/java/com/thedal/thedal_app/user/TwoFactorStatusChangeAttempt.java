package com.thedal.thedal_app.user;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "two_factor_status_change_attempt")
@Getter
@Setter
public class TwoFactorStatusChangeAttempt {
    
//    @Id
//    @GeneratedValue(strategy = GenerationType.AUTO)
//    private Long id;
    
	@Id
    @Column(name = "user_id")
    private Long userId;
    
    @Column(name = "otp")
    private String otp;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    @Column(name = "new_two_factor_status")
    private Boolean newTwoFactorStatus;
}
