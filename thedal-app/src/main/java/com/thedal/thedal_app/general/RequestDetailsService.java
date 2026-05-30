package com.thedal.thedal_app.general;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import com.thedal.thedal_app.role.RolePermission;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;
import com.thedal.thedal_app.user.UserEntity;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class RequestDetailsService {
	public static final String USER_ATTRIBUTE = "user";

	// Common method to retrieve the current account entity from the request attributes
	public UserEntity getCurrentUserFromRequest() {
		log.info(" inside getCurrentUserFromRequest method");
		RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
		UserEntity user = (UserEntity) attributes.getAttribute(USER_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
		log.info(" inside getCurrentUserFromRequest method: Got User Object:{}",user.getId());
		
//		if (user == null) {
//            log.error("User not found in the request context.");
//            throw new ThedalException(ThedalError.USER_NOT_FOUND, HttpStatus.UNAUTHORIZED);
//        }
//
//        log.info("Retrieved User Object: {}", user.getId());
//
//        //Check if user is active before proceeding
//        if (Boolean.FALSE.equals(user.getIsActive())) {
//            log.error("User {} is inactive. Access denied.", user.getId());
//            throw new ThedalException(ThedalError.USER_INACTIVE, HttpStatus.FORBIDDEN);
//        }
		
		return user;
	}

    public Long getCurrentAccountId() {
        UserEntity user = getCurrentUserFromRequest();
        if (user.getAccountEntity() == null) {
            log.error("AccountEntity is null for user ID: {}. User may not be properly associated with an account.", user.getId());
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }
        return user.getAccountEntity().getId();
    }

	public Long getCurrentUserId() {
        UserEntity user = getCurrentUserFromRequest();
        return user != null ? user.getId() : null;
    }

	// Common method to retrieve the current permission from the request attributes
	public boolean hasPermissionForCurrentUser(RolePermission rolePermission) {
		UserEntity user = getCurrentUserFromRequest();
		log.info(" inside getCurrentUserPermissionFromRequest method: Got Permission:{}",user.getRole().getPermission());
		Integer permission = user.getRole().getPermission();
		return rolePermission.hasPermission(permission);
		//return false;
	}
//	public boolean hasPermissionForCurrentUser(RolePermission rolePermission) {
//        UserEntity user = getCurrentUserFromRequest();
//        Map<String, List<String>> permissions = user.getRole().getPermission(); // Now a Map
//
//        log.info("inside hasPermissionForCurrentUser method: Got Permission Map: {}", permissions);
//
//        if (permissions == null || permissions.isEmpty()) {
//            return false;
//        }
//
//        // Check if the rolePermission exists in the user's permission map
//        return permissions.containsKey(rolePermission.getCategory()) &&
//               permissions.get(rolePermission.getCategory()).contains(rolePermission.getAction());
//    }

    public void checkUserRolePermission(RolePermission rolePermission) {
        if (!hasPermissionForCurrentUser(rolePermission)) {
            log.error("User is not allowed to do the operation. Unauthorized access.");
            throw new ThedalException(ThedalError.ROLE_PERMISSION_REQUIRED, HttpStatus.UNAUTHORIZED);
        }
    }
    
 // Check if the current user has a specific permission for a resource
    public boolean hasPermissionForCurrentUser(String resource, String operation) {
        UserEntity user = getCurrentUserFromRequest();
        Map<String, List<String>> rolePermissions = user.getRole().getRolePermission();
        log.info("inside hasPermissionForCurrentUser method: Got Role Permissions: {}", rolePermissions);

        if (rolePermissions == null || !rolePermissions.containsKey(resource)) {
            log.warn("No permissions found for resource: {}", resource);
            return false;
        }

        List<String> operations = rolePermissions.get(resource);
        boolean hasPermission = operations != null && operations.contains(operation);
        log.info("Permission check for resource: {}, operation: {} - Result: {}", resource, operation, hasPermission);
        return hasPermission;
    }
    
 // Check permission and throw exception if not authorized
    public void checkUserRolePermission(String resource, String operation) {
        if (!hasPermissionForCurrentUser(resource, operation)) {
            log.error("User is not allowed to perform operation {} on resource {}. Unauthorized access.", operation, resource);
            throw new ThedalException(ThedalError.ROLE_PERMISSION_REQUIRED, HttpStatus.UNAUTHORIZED);
        }
    }

}