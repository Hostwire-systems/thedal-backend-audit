package com.thedal.thedal_app.user;

import java.time.LocalDateTime;

import com.thedal.thedal_app.account.AccountEntity;
import com.thedal.thedal_app.profileAPI.ProfileEntity;
import com.thedal.thedal_app.role.Role;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
@Entity
@Table(name = "_user") // "user" to "_user" because "user" is a reserved keyword in PostgreSQL
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "user_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "role_id")
    private Role role;
    
    @ManyToOne
    private AccountEntity accountEntity;
    
    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;
    
    @Column(name = "email_address")
    private String email;

    @Column(name = "mobile_number")
    private String mobileNumber;

    @Column(name = "password")
    private String password;

    @Column(name = "is_email_verified")
    private Boolean isEmailVerified;

    @Column(name = "is_mobile_verified")
    private Boolean isMobileVerified;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "profile_picture")
    private String profilePicture;
    
    @Column(name = "profile_picture_name")
    private String profilePictureName;

    @Column(name = "account_id")
    private Long accountId;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private ProfileEntity profile;
    @Column(name = "slip_box")
    private Boolean slipBox = true;
    
    @Column(name = "is_two_factor_enabled")
    private Boolean isTwoFactorEnabled = false;
    
    @Column(name = "password_version")
    private Integer passwordVersion = 1;
    
    @Column(name = "is_otp_required")
    private Boolean isOtpRequired = true;
    
    @Column(name = "expiry_at")
    private LocalDateTime expiryAt;

}
