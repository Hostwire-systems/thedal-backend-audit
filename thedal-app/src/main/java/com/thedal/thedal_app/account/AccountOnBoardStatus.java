package com.thedal.thedal_app.account;

import org.springframework.http.HttpStatus;

import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;

public enum AccountOnBoardStatus {

	OAUTH(1),
	SIGNUP_COMPLETION(2),
	OTP_VERIFICATION(3),
	PROFILE_SETUP_1(4),
	PROFILE_SETUP_2(5),
	PROFILE_ROLE_SETUP(6),
	CAMPAIGN_SETTINGS(7);
	
	private final Integer value;

	private AccountOnBoardStatus(Integer value) {
		this.value = value;
	}

	public Integer getValue() {
		return value;
	}
	
    public static AccountOnBoardStatus fromValue(Integer value) {
        for (AccountOnBoardStatus status : AccountOnBoardStatus.values()) {
            if (status.getValue().equals(value)) {
                return status;
            }
        }
        throw new ThedalException(ThedalError.ONBOARD_STATUS_NOT_FOUND,HttpStatus.NOT_FOUND);
    }
	
}
