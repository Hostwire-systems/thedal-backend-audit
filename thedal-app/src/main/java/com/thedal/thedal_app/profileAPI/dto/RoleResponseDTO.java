package com.thedal.thedal_app.profileAPI.dto;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoleResponseDTO {

	private Long id;

    private String roleName;

    private Integer permission;
    private Map<String, List<String>> rolePermission;   

    private String description;
    
    //private Long accountId;
    
    //private boolean isCurrenRole;
    
    public RoleResponseDTO(Long id, String roleName, Integer permission, String description, Map<String, List<String>> rolePermission) {
        this.id = id;
        this.roleName = roleName;
        this.permission = permission;
        this.description = description;
        this.rolePermission = rolePermission;
    }
    
    
}
