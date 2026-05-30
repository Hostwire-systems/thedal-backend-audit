package com.thedal.thedal_app.profileAPI;

//import java.time.LocalDateTime;

import com.thedal.thedal_app.profileAPI.dtos.SubscriptionType;
import com.thedal.thedal_app.user.UserEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
//@Table(name = "profile_entity")
//@Table(name = "profile_entity", uniqueConstraints = {
//        @UniqueConstraint(columnNames = "email"),
//        @UniqueConstraint(columnNames = "mobileNumber")
//})
@Table(name = "profile_entity")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ProfileEntity {
	
	@Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    // @NotBlank(message = "Full name cannot be empty")
     @Column(name = "full_name", nullable = false)
    private String fullName;
    
    // @NotBlank(message = "Email cannot be empty")
     @Column(name = "email", nullable = false)
    // @Email(message = "Email should be valid")
    private String email;

    // @NotBlank(message = "Mobile number cannot be empty")
    // @Pattern(regexp = "^[0-9]{10}$", message = "Mobile number must be 10 digits")
    @Column(name = "mobile_number", nullable = false)
    private String mobileNumber;

    // @NotBlank(message = "Email cannot be empty")
    @Column(name = "Alternate_Email_ID")
    // @Email(message = "Email should be valid")
    private String alternateEmailId;

    // @NotBlank(message = "Mobile number cannot be empty")
    // @Pattern(regexp = "^[0-9]{10}$", message = "Mobile number must be 10 digits")
    @Column(name = "Alternate_Mobile_Number")
    private String alternateMobileNumber;

    @Column(name = "organization_name", nullable = true)
    private String organizationName;

    // Billing Address Fields
    @Column(name = "billing_address")
    private String billingAddress;

    @Column(name = "state")
    private String state;

    //@Pattern(regexp = "^[0-9]{6}$", message = "Pincode must be 6 digits")
    @Column(name = "pincode")
    private String Pincode;

    @Column(name = "country_code")
    private String countryCode;

    @Column(name = "gst")
    private String gst; // Optional
    
    @Enumerated(EnumType.STRING)
    @Column(name = "subscription")
    private SubscriptionType subscription;
    
    @Column(name = "account_id")
    private Long accountId;

    @OneToOne
    @JoinColumn(name = "user_id")
    private UserEntity user;

   // private LocalDateTime expiryDate;
    
//    private String profilePicture;

}
