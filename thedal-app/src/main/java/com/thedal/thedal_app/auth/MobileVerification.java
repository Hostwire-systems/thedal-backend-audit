package com.thedal.thedal_app.auth;

import java.time.LocalDateTime;

import com.thedal.thedal_app.user.UserEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
@Entity
@Table(name = "mobile_verification")
public class MobileVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "mobile_verification_id")
    private Long id;

    @Column(name = "mobile_number")
    private String mobileNumber;

    @Column(name = "otp")
    private String otp;

    @OneToOne
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

}
