package com.thedal.thedal_app.thedal_exception;


import org.springframework.http.HttpStatus;

public class ThedalException extends RuntimeException {
    private final ThedalError thedalError;
	private final HttpStatus httpStatus;
	private final String details;

	public ThedalException(ThedalError thedalError,HttpStatus httpStatus) {
		super(thedalError.getMessage());
		this.thedalError = thedalError;
		this.httpStatus=httpStatus;
		this.details = null;
	}
	
	
	public ThedalException(ThedalError error, HttpStatus status, String details) {
        //super(error.getMessage() + ": " + details);
		 super((error != null ? error.getMessage() : "Unknown Error") + 
	              (details != null ? ": " + details : ""));
		this.thedalError = null;
        this.httpStatus = status;
        this.details = details;
    }
//	public ThedalException(ThedalError thedalError, HttpStatus httpStatus, String details) {
//        super(details != null ? details : thedalError.getMessage());
//        this.thedalError = thedalError; // Retain the ThedalError object
//        this.httpStatus = httpStatus;
//        this.details = details;
//    }

	public ThedalError getThedalError() {
		return thedalError;
	}

	public HttpStatus getHttpStatus() {
		return httpStatus;
	}
//	public String getDetails() {
//        return details;
//    }

	public Object getErrorCode() {
		// TODO Auto-generated method stub
		return thedalError != null ? thedalError.getCode() : "UNKNOWN_ERROR";
	}


}
