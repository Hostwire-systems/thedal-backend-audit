package com.thedal.thedal_app.util;

import org.springframework.http.HttpStatus;

import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ValidationUtil {
	
    public static final String EMAIL_REGEX = "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$";
    public static final String MOBILE_REGEX = "^[0-9]{10}$";

    // Private constructor to prevent instantiation
    private ValidationUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    // Validate email format
    public static void validateEmail(String email) {
        if (email == null || !email.matches(EMAIL_REGEX)) {
            log.error("Invalid email format: {}", email);
            throw new ThedalException(ThedalError.INVALID_EMAIL, HttpStatus.BAD_REQUEST);
        }
    }

    // Validate mobile number format
    public static void validateMobileNumber(String mobileNumber) {
        if (mobileNumber == null || !mobileNumber.matches(MOBILE_REGEX)) {
            log.error("Invalid mobile number format: {}", mobileNumber);
            throw new ThedalException(ThedalError.INVALID_MOBILE_NUMBER, HttpStatus.BAD_REQUEST);
        }
    }

}
