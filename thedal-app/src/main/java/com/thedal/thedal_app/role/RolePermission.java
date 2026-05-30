package com.thedal.thedal_app.role;

import org.springframework.http.HttpStatus;

import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;

public enum RolePermission {

	BOOTH_MANAGEMENT(1<<0),
	CADRE_MANAGEMENT(1<<1),
	POLLING_MANAGEMENT(1<<2),
	VOTER_MANAGEMENT(1<<3),
	SETTINGS_MANAGEMENT(1<<4), 
	PARTMANAGER_MANAGEMENT(1<<5);
	
	private final int value;

	private RolePermission(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}
	
	public static RolePermission getRolePermissionFromValue(int value) {//TODO change
        for (RolePermission status : RolePermission.values()) {
            if (status.getValue() == value) {
                return status;
            }
        }
        throw new ThedalException(ThedalError.ROLE_PERMISSION_NOT_FOUND, HttpStatus.BAD_REQUEST);
    }

	public boolean hasPermission(Integer userPermission) {
        return (this.value & userPermission) > 0;
    }
	
	
	
	
	
	
}