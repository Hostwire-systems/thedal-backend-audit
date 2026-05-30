package com.thedal.thedal_app.subscription.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionModuleDto {
    
    private Long id;
    private String moduleKey;
    private String moduleName;
    private String moduleDescription;
    private Long parentModuleId;
    private Integer displayOrder;
    private Boolean isActive;
    private String iconName;
    private String routePath;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // For hierarchical display
    private List<SubscriptionModuleDto> submodules;
}
