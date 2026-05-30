package com.thedal.thedal_app.user;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserDetailsDto {
    private Long id;   
    private Long accountId;
    private LocalDateTime createdAt;
    private String firstName;
    private String lastName;
    private String email;
    private String mobileNumber;
    private String role;
    private Integer onBoardStatus;
    private String profilePicture;
    
    private Boolean slipBox;
    private Boolean isTwoFactorEnabled;
    private Boolean isOtpRequired;
    private LocalDateTime expiryAt;
}
