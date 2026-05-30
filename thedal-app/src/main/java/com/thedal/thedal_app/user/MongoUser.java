package com.thedal.thedal_app.user;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Document(collection = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MongoUser {
    
    @Id
    private String id;
    
    @Field("user_id")
    @Indexed(unique = true)
    private Long userId; // Reference to PostgreSQL ID
    
    @Field("role_id")
    private Long roleId;
    
    @Field("role_name")
    private String roleName;
    
    @Field("account_id")
    private Long accountId;
    
    @Field("first_name")
    private String firstName;
    
    @Field("last_name")
    private String lastName;
    
    @Field("email_address")
    private String email;
    
    @Field("mobile_number")
    private String mobileNumber;
    
    @Field("password")
    private String password;
    
    @Field("is_email_verified")
    private Boolean isEmailVerified;
    
    @Field("is_mobile_verified")
    private Boolean isMobileVerified;
    
    @Field("is_active")
    private Boolean isActive;
    
    @Field("created_by")
    private String createdBy;
    
    @Field("updated_by")
    private String updatedBy;
    
    @Field("created_at")
    private LocalDateTime createdAt;
    
    @Field("updated_at")
    private LocalDateTime updatedAt;
    
    @Field("profile_picture")
    private String profilePicture;
    
    @Field("profile_picture_name")
    private String profilePictureName;
    
    @Field("slip_box")
    private Boolean slipBox = true;
    
    @Field("is_two_factor_enabled")
    private Boolean isTwoFactorEnabled = false;
    
    @Field("is_otp_required")
    private Boolean isOtpRequired = true;
    
    @Field("expiry_at")
    private LocalDateTime expiryAt;
    
    // Legacy fields for backward compatibility
    private String username; // Computed field: firstName + " " + lastName
    
    // Constructor to create from PostgreSQL entity
    public MongoUser(UserEntity entity) {
        this.userId = entity.getId();
        this.roleId = entity.getRole() != null ? entity.getRole().getId() : null;
        this.roleName = entity.getRole() != null ? entity.getRole().getRoleName() : null;
        this.accountId = entity.getAccountEntity() != null ? entity.getAccountEntity().getId() : entity.getAccountId();
        this.firstName = entity.getFirstName();
        this.lastName = entity.getLastName();
        this.username = (entity.getFirstName() != null ? entity.getFirstName() : "") + " " + 
                        (entity.getLastName() != null ? entity.getLastName() : "");
        this.email = entity.getEmail();
        this.mobileNumber = entity.getMobileNumber();
        this.password = entity.getPassword();
        this.isEmailVerified = entity.getIsEmailVerified();
        this.isMobileVerified = entity.getIsMobileVerified();
        this.isActive = entity.getIsActive();
        this.expiryAt = entity.getExpiryAt();
        this.createdBy = entity.getCreatedBy();
        this.updatedBy = entity.getUpdatedBy();
        this.createdAt = entity.getCreatedAt();
        this.updatedAt = entity.getUpdatedAt();
        this.profilePicture = entity.getProfilePicture();
        this.profilePictureName = entity.getProfilePictureName();
        this.slipBox = entity.getSlipBox();
        this.isTwoFactorEnabled = entity.getIsTwoFactorEnabled();
        this.isOtpRequired = entity.getIsOtpRequired();
    }
}
