package com.thedal.thedal_app.subscription.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSubscriptionDto {
    
    private Long id;
    private Long userId;
    private Long moduleId;
    private String moduleKey;
    private String moduleName;
    private Boolean hasAccess;
    private LocalDateTime grantedAt;
    private LocalDateTime expiresAt;
    private String grantedBy;
    private LocalDateTime revokedAt;
    private String revokedBy;
}
