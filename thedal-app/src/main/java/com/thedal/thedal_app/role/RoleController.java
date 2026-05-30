package com.thedal.thedal_app.role;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.thedal.thedal_app.profileAPI.dto.AddRoleDTO;
import com.thedal.thedal_app.profileAPI.dto.RoleResponseDTO;
import com.thedal.thedal_app.response.ThedalResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/role")
public class RoleController {

	private final RoleService roleService;
	
    public RoleController(RoleService roleService) {
		this.roleService = roleService;
	}

	@PostMapping
    public ThedalResponse<Void> addRole(@Valid @RequestBody AddRoleDTO addRoleDTO){
    	return roleService.addRole(addRoleDTO);
    }
    
    @GetMapping
    public ThedalResponse<List<RoleResponseDTO>> getRoles(){
    	return roleService.getRoles();
    }
    
    @PutMapping("/{roleId}")
    public ThedalResponse<Void> updateRole(@PathVariable Long roleId, @RequestBody AddRoleDTO addRoleDTO){
    	return roleService.updateRole(roleId,addRoleDTO);
    }
    
    @DeleteMapping("/{roleId}")
    public ThedalResponse<Void> deleteRoleById(@PathVariable Long roleId){
    	return roleService.deleteRoleById(roleId);
    }

}