package com.thedal.thedal_app.user;

import lombok.Data;

@Data
public class TwoFactorOtpResponse {
    private Long userId;
    private String message;

    
}
