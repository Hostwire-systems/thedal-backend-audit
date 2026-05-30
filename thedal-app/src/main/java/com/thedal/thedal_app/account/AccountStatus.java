package com.thedal.thedal_app.account;

import org.springframework.http.HttpStatus;

import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;

public enum AccountStatus {

	INVITED(1),
	ACTIVE(2),
	REQUESTED(4),
	REJECTED(8);
	
	private final int value;

	private AccountStatus(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}
	
	public static AccountStatus getStatusFromValue(int value) {
        for (AccountStatus status : AccountStatus.values()) {
            if (status.getValue() == value) {
                return status;
            }
        }
        throw new ThedalException(ThedalError.ACCOUNT_STATUS_NOT_FOUND, HttpStatus.BAD_REQUEST);
    }
	
}
