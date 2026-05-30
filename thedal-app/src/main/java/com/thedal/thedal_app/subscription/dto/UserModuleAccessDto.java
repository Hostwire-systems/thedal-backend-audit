package com.thedal.thedal_app.subscription.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserModuleAccessDto {
    
    private Long userId;
    private List<String> accessibleModuleKeys;
    private List<SubscriptionModuleDto> accessibleModules;
}
