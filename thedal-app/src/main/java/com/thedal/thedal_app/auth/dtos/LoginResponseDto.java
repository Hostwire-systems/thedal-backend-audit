package com.thedal.thedal_app.auth.dtos;

import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class LoginResponseDto {
	private Long userId;
    private String firstName;
    private String lastName;
    private String email;
    private String mobileNumber;
    private String role;
    private Long roleId;
    private Boolean isEmailVerified;
    private Boolean isMobileVerified;
    private String accessToken;
    private String refreshToken;
    private Integer onBoardStatus;
    
    private Map<String, List<String>> rolePermission;
    private String deviceId; // optional: identifier of the created session
    
    public LoginResponseDto() {
    }

    public LoginResponseDto(String jwt) {
        this.accessToken = jwt;
    }

}
