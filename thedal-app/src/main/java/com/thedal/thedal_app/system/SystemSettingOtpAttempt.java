package com.thedal.thedal_app.system;

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
@Table(name = "system_setting_otp_attempt")
@Getter
@Setter
public class SystemSettingOtpAttempt {
    
//    @Id
//    @GeneratedValue(strategy = GenerationType.AUTO)
//    private Long id;
	@Id
    @Column(name = "user_id")
    private Long userId;
    
    @Column(name = "otp")
    private String otp;
    
    @Column(name = "is_active")
    private Boolean isActive;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    @Column(name = "setting_key")
    private String settingKey;
    
    @Column(name = "new_setting_value")
    private String newSettingValue;
}