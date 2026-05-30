package com.thedal.thedal_app.role;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thedal.thedal_app.account.AccountEntity;
import com.thedal.thedal_app.account.AccountOnBoardStatus;
import com.thedal.thedal_app.account.AccountService;
import com.thedal.thedal_app.profileAPI.dto.AddRoleDTO;
import com.thedal.thedal_app.profileAPI.dto.RoleResponseDTO;
import com.thedal.thedal_app.response.ThedalResponse;
import com.thedal.thedal_app.response.ThedalSuccess;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class RoleServiceImpl implements RoleService {

	private static final Long PROTECTED_ROLES = -1L;
	
	private final RoleRepo roleRepo;
	
    private final AccountService accountService;
    
    public RoleServiceImpl(RoleRepo roleRepo, AccountService accountService) {
        this.roleRepo = roleRepo;
        this.accountService = accountService;
        
    }
    
    @Override 
    @Transactional 
    public ThedalResponse<Void> addRole(@Valid AddRoleDTO addRoleDTO) {
    	
    	log.info("inside addRole method");

        AccountEntity account = accountService.getCurrentAccountFromRequest();
        String roleName = addRoleDTO.getRoleName().trim().toUpperCase();
        log.info("inside addRole method: role name:{}, account id:{}", roleName, account.getId());

        // Check for existing roles with the same name and account ID
        List<Role> optionalRole = roleRepo.findByRoleNameAndAccountIdOrAccountId(roleName, account.getId(), PROTECTED_ROLES);

        if (optionalRole.stream().anyMatch(role -> role.getRoleName().equals(roleName))) {
            throw new ThedalException(ThedalError.DUPLICATE_ROLE_FOUND, HttpStatus.BAD_REQUEST);
        }

        log.info("rolePermission before save: {}", addRoleDTO.getRolePermission());

        // Calculate the permission value
        int permissionValue = addRoleDTO.getPermission().stream()
                .map(RolePermission::valueOf)
                .mapToInt(RolePermission::getValue)
                .reduce(0, (a, b) -> a | b);

        log.info("rolePermission before save: {}", addRoleDTO.getRolePermission());

        try {
            // Convert rolePermission map to JSON string
            ObjectMapper objectMapper = new ObjectMapper();
            String rolePermissionJson = objectMapper.writeValueAsString(addRoleDTO.getRolePermission());

            log.info("rolePermission JSON before insertion: {}", rolePermissionJson);  

            // Insert the role using the custom query
            roleRepo.insertRole(
                    roleName,
                    permissionValue,
                    rolePermissionJson,  
                    addRoleDTO.getDescription(),
                    account.getId()
            );

            log.info("end of addRole method");

            boolean updateAccountOnBoardStatus = accountService.updateAccountOnBoardStatus(AccountOnBoardStatus.PROFILE_ROLE_SETUP);
            log.info("onboard status:{}", updateAccountOnBoardStatus);

            return new ThedalResponse<>(ThedalSuccess.PROFILE_ROLE_ADDED);
        } catch (Exception e) {
            log.error("Error saving role: {}", e.getMessage(), e);
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


	@Override
	public ThedalResponse<List<RoleResponseDTO>> getRoles() {
		
		AccountEntity account=accountService.getCurrentAccountFromRequest();
		 log.info("inside getRoles method: got account:{}",account.getId());
		List<Role> roleList = roleRepo.findByAccountIdOrAccountIdOrderByIdAsc(account.getId(), PROTECTED_ROLES);
		
		// find my current role id
		//UserEntity userEntity = userRepo.findByAccountEntity(account).orElseThrow(()-> new RuntimeException("No roles found"));
		//Long currentRoleId = userEntity.getRole().getId();
		//log.info("current onboard status:{}",AccountUtil.getCurrentAccountOnBoardStatus());
		List<RoleResponseDTO> myRoleResponseDTO = roleList.stream()
				.map(role -> new RoleResponseDTO(
						role.getId(),
						role.getRoleName(),
						role.getPermission(),
						role.getDescription(),
						role.getRolePermission()))
				.toList();
		log.info("end of getRoles method");
		return new ThedalResponse<>(ThedalSuccess.PROFILE_ROLE_FETCHED, myRoleResponseDTO);
	}

	@Override
	@Transactional
	public ThedalResponse<Void> updateRole(@PathVariable Long roleId, @Valid AddRoleDTO addRoleDTO) {
		
		log.info("inside updateRole method");

	    AccountEntity account = accountService.getCurrentAccountFromRequest();
	    String roleName = addRoleDTO.getRoleName().trim().toUpperCase();
	    log.info("inside updateRole method: role name:{}, account id:{}", roleName, account.getId());

	    // Check for existing role with the provided roleId
	    Role existingRole = roleRepo.findById(roleId).orElseThrow(() -> 
	        new ThedalException(ThedalError.ROLE_NOT_FOUND, HttpStatus.NOT_FOUND));

	    // Check for conflicting role names
	    List<Role> accountRoles = roleRepo.findByAccountId(account.getId());
	    boolean isConflict = accountRoles.stream()
	            .filter(role -> !role.getId().equals(roleId))  // Exclude the role being updated
	            .anyMatch(role -> role.getRoleName().equals(roleName));

	    if (isConflict) {
	        throw new ThedalException(ThedalError.DUPLICATE_ROLE_FOUND, HttpStatus.BAD_REQUEST);
	    }

	    // Calculate the permission value
	    int permissionValue = addRoleDTO.getPermission().stream()
	            .map(RolePermission::valueOf)
	            .mapToInt(RolePermission::getValue)
	            .reduce(0, (a, b) -> a | b);

	    log.info("rolePermission before update: {}", addRoleDTO.getRolePermission());

	    try {
	        // Convert rolePermission Map to JSON string
	        ObjectMapper objectMapper = new ObjectMapper();
	        String rolePermissionJson = objectMapper.writeValueAsString(addRoleDTO.getRolePermission());

	        log.info("rolePermission JSON before update: {}", rolePermissionJson);

	        // Update the role using the custom query
	        roleRepo.updateRole(
	                roleId,
	                roleName,
	                permissionValue,
	                rolePermissionJson,
	                addRoleDTO.getDescription(),
	                account.getId()
	        );

	        log.info("end of updateRole method");

	        return new ThedalResponse<>(ThedalSuccess.PROFILE_ROLE_UPDATED);
	    } catch (Exception e) {
	        log.error("Error updating role: {}", e.getMessage(), e);
	        throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
	    }
	}




//	@Override
//	public ThedalResponse<Void> deleteRoleById(Long roleId) {
//		
//		AccountEntity account=accountService.getCurrentAccountFromRequest();
//        log.info("inside deleteRoleById method: got account:{}",account.getId());
//    
//		roleRepo.findByIdAndAccountId(roleId,account.getId()).orElseThrow(()-> new ThedalException(ThedalError.ROLE_NOT_FOUND_OR_UNAUTHORIZED_ROLE_EDIT, HttpStatus.UNAUTHORIZED));
//		roleRepo.deleteById(roleId);
//		log.info("end of deleteRoleById method");
//		return new ThedalResponse<>(ThedalSuccess.PROFILE_ROLE_DELETED);
//	}
	@Override
	public ThedalResponse<Void> deleteRoleById(Long roleId) {
	    AccountEntity account = accountService.getCurrentAccountFromRequest();
	    log.info("inside deleteRoleById method: got account:{}", account.getId());

	    // Check if the role exists for the given account
	    Role role = roleRepo.findByIdAndAccountId(roleId, account.getId())
	            .orElseThrow(() -> new ThedalException(ThedalError.ROLE_NOT_FOUND_OR_UNAUTHORIZED_ROLE_EDIT, HttpStatus.UNAUTHORIZED));

	    try {
	        roleRepo.deleteById(roleId);
	        log.info("end of deleteRoleById method");
	        return new ThedalResponse<>(ThedalSuccess.PROFILE_ROLE_DELETED);
	    } catch (DataIntegrityViolationException e) {
	        log.error("Failed to delete role {}: Role is still assigned to users", roleId);
	        throw new ThedalException(ThedalError.ROLE_CANNOT_BE_DELETED_DUE_TO_DEPENDENCIES, HttpStatus.CONFLICT);
	    }
	}


	
}