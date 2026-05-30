package com.thedal.thedal_app.user;
import java.time.LocalDateTime;

import com.thedal.thedal_app.profileAPI.dtos.SubscriptionType;

import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;


@NoArgsConstructor
@Getter
@Setter
public class UserListDto {
	private Long userId;
	private LocalDateTime createdAt;
    private String firstName;
    private String lastName;
    private String email;
    private String mobileNumber;
    private SubscriptionType subscription;
    private Boolean isActive;
    //private LocalDateTime loginExpiryDate;
    private String roleName;
    private Boolean slipBox;
    private Boolean isTwoFactorEnabled;
    private Boolean isOtpRequired;
    private LocalDateTime expiryAt;


    public UserListDto(Long userId, LocalDateTime createdAt, String firstName, String lastName,String email,
    		String mobileNumber, SubscriptionType subscription, Boolean isActive,
    		String roleName, Boolean slipBox, Boolean isTwoFactorEnabled, Boolean isOtpRequired,
    		LocalDateTime expiryAt) {
    	this.userId = userId;
    	this.createdAt=createdAt;
    	this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.mobileNumber = mobileNumber;
        this.subscription = subscription;
        this.isActive = isActive;
       // this.loginExpiryDate = loginExpiryDate;
        this.roleName = roleName;
        this.slipBox = slipBox;
        this.isTwoFactorEnabled = isTwoFactorEnabled;
        this.isOtpRequired = isOtpRequired;
        this.expiryAt = expiryAt;
    }

}
