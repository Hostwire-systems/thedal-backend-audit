package com.thedal.thedal_app.subscription.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateModuleRequest {
    
    @NotBlank(message = "Module key is required")
    private String moduleKey;
    
    @NotBlank(message = "Module name is required")
    private String moduleName;
    
    private String moduleDescription;
    private Long parentModuleId;
    
    @NotNull(message = "Display order is required")
    private Integer displayOrder;
    
    private String iconName;
    private String routePath;
    private Boolean isActive = true;
}
