package com.thedal.thedal_app.role;

import java.util.List;

import com.thedal.thedal_app.profileAPI.dto.AddRoleDTO;
import com.thedal.thedal_app.profileAPI.dto.RoleResponseDTO;
import com.thedal.thedal_app.response.ThedalResponse;

public interface RoleService {

	ThedalResponse<Void> addRole(AddRoleDTO addRoleDTO);
	ThedalResponse<List<RoleResponseDTO>> getRoles();
	ThedalResponse<Void> updateRole(Long roleId, AddRoleDTO addRoleDTO);
	ThedalResponse<Void> deleteRoleById(Long roleId);
}
