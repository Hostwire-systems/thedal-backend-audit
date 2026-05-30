package com.thedal.thedal_app.subscription.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GrantSubscriptionRequest {
    
    @NotNull(message = "User ID is required")
    private Long userId;
    
    @NotEmpty(message = "At least one module ID is required")
    private List<Long> moduleIds;
    
    private LocalDateTime expiresAt;
}
