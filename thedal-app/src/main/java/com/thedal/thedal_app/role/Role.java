package com.thedal.thedal_app.role;

import java.util.List;
import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter
@Setter
@RequiredArgsConstructor
@Entity 
public class Role {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

    @Column(name = "role_name")
    private String roleName;

    @Column(name = "permission", nullable = false)
    private Integer permission;
    @Column(name = "role_permission", columnDefinition = "jsonb", nullable = false)
    @Convert(converter = PermissionConverter.class)
    private Map<String, List<String>> rolePermission;

    @Column(name = "description", length = 120)
    private String description;
    
    @Column(name = "account_id")
    private Long accountId;


/// new role permission is created	
    
}