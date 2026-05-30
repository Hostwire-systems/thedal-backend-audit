package com.thedal.thedal_app.thedal_exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.thedal.thedal_app.response.ThedalResponse;
import com.thedal.thedal_app.response.ThedalResponseStatus;

@RestControllerAdvice
public class ThedalExceptionHandler {
	
	
	   // Handle generic exceptions
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ThedalResponse<Void>> handleGenericException(Exception ex) {
        ThedalResponse<Void> response = new ThedalResponse<>();
        response.setStatus(ThedalResponseStatus.ERROR.name().toLowerCase());
        response.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
        response.setMessage("An unexpected error occurred: " + ex.getMessage());
       //response.setData(null);
        
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
 // Handle validation errors from @Valid annotations (MethodArgumentNotValidException)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ThedalResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        FieldError firstError = ex.getBindingResult().getFieldErrors().get(0);  // Get the first error

        String errorCodeStr = firstError.getDefaultMessage();
        int errorCode = 400;  // Default error code
        String errorMessage = "Unknown error";  // Default error message

        try {
            // Convert the message (which is actually an error code) to an integer
            errorCode = Integer.parseInt(errorCodeStr);

            // Get the corresponding error from ThedalError enum
            ThedalError thedalError = ThedalError.fromErrorCode(errorCode);

            if (thedalError != null) {
                errorMessage = thedalError.getMessage();
            }
        } catch (NumberFormatException e) {
            // In case the message is not a valid error code, use the message directly
            errorMessage = firstError.getDefaultMessage();
        }

        // Build the response
        ThedalResponse<Void> response = new ThedalResponse<>();
        response.setStatus(ThedalResponseStatus.ERROR.name().toLowerCase());
        response.setCode(errorCode);
        response.setMessage(errorMessage);
        response.setData(null);  // Set data as null

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
    
    @ExceptionHandler(ThedalException.class)
    public ResponseEntity<ThedalResponse<Void>> handleThedalException(ThedalException ex) {
        ThedalError thedalError = ex.getThedalError();
        HttpStatus status = ex.getHttpStatus();

        // Build the response based on ThedalError
        ThedalResponse<Void> response = new ThedalResponse<>();
        response.setStatus(ThedalResponseStatus.ERROR.name().toLowerCase());
//        response.setCode(thedalError.getCode());
//        response.setMessage(thedalError.getMessage());
//        //response.setData(null);  // Set data as null for error cases
        if (thedalError != null) {
            response.setCode(thedalError.getCode());
            response.setMessage(thedalError.getMessage());
        } else {
            response.setCode(status.value());  // Fallback to HTTP status code
            response.setMessage(ex.getMessage());  // Use the exception message
        }

        return new ResponseEntity<>(response, status);
    }
//    @ExceptionHandler(ThedalException.class)
//    public ResponseEntity<ThedalResponse<Void>> handleThedalException(ThedalException ex) {
//        ThedalError thedalError = ex.getThedalError();
//        HttpStatus status = ex.getHttpStatus();
//
//        ThedalResponse<Void> response = new ThedalResponse<>();
//        response.setStatus(ThedalResponseStatus.ERROR.name().toLowerCase());
//        response.setCode(thedalError != null ? thedalError.getCode() : status.value());
//        response.setMessage(ex.getMessage());  // Prioritizes details if provided
//
//        return new ResponseEntity<>(response, status);
//    }

 

//     @ExceptionHandler(ThedalException.class)
//public ResponseEntity<ThedalResponse> handleMetiegrowException(ThedalException e) {
//    ThedalResponse response = ThedalResponse.get(e.getThedalError());
//    return new ResponseEntity<>(response, e.getHttpStatus());
//}
//    @ExceptionHandler(MethodArgumentNotValidException.class)
//    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
//    public ResponseEntity<ThedalResponse> handleInvalidArgument(MethodArgumentNotValidException ex) {
//        List<ThedalResponse> responses = new ArrayList<>();
//        for (org.springframework.validation.FieldError error : ex.getBindingResult().getFieldErrors()) {
//            ThedalError metiegrowError = ThedalError.fromErrorCode(Integer.parseInt(error.getDefaultMessage()));
//            if (metiegrowError != null) {
//                ThedalResponse metiegrowResponse = ThedalResponse.get(metiegrowError);
//                responses.add((ThedalResponse) metiegrowResponse.getStatus().get(0));
//            } else {
//                ThedalResponse thedalResponse = new ThedalResponse();
//                responses.add(thedalResponse);
//            }
//        }
//        ThedalResponse response = ThedalResponse.createListResponse(responses);
//        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
//    }

//     @ExceptionHandler(ThedalException.class)
//public ResponseEntity<ThedalResponse> handleMetiegrowException(ThedalException e) {
//    ThedalResponse response = ThedalResponse.get(e.getThedalError());
//    return new ResponseEntity<>(response, e.getHttpStatus());
//}
//    @ExceptionHandler(MethodArgumentNotValidException.class)
//    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
//    public ResponseEntity<ThedalResponse> handleInvalidArgument(MethodArgumentNotValidException ex) {
//        List<ThedalResponse> responses = new ArrayList<>();
//        for (org.springframework.validation.FieldError error : ex.getBindingResult().getFieldErrors()) {
//            ThedalError metiegrowError = ThedalError.fromErrorCode(Integer.parseInt(error.getDefaultMessage()));
//            if (metiegrowError != null) {
//                ThedalResponse metiegrowResponse = ThedalResponse.get(metiegrowError);
//                responses.add(metiegrowResponse.getstatuses().get(0));
//            } else {
//                ThedalResponse thedalResponse = new ThedalResponse("error",
//                        Integer.parseInt(error.getDefaultMessage()), "Unknown error");
//                responses.add(thedalResponse);
//            }
//        }
//        ThedalResponse response = ThedalResponse.createListResponse(responses);
//        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
//    }

//    @ExceptionHandler(AccessDeniedException.class)
//    public ThedalResponse handleAccessDeniedException(AccessDeniedException ex) {
//        return new ThedalResponse(
//                ThedalResponseStatus.ERROR.name(),
//                ThedalError.ACCESS_DENIED.getCode(),
//                ThedalError.ACCESS_DENIED.getMessage());
//    }

    // @ExceptionHandler(SQLIntegrityConstraintViolationException.class)
    // public ThedalResponse handleSQLIntegrityConstraintViolationException(
    //       SQLIntegrityConstraintViolationException ex) {
    //     return new ThedalResponse(
    //             ThedalResponseStatus.ERROR.name(),
    //             ThedalError.ERROR_JOB_DETAIL_DUPLICATES.getCode(),
    //             ThedalError.ERROR_JOB_DETAIL_DUPLICATES.getMessage());
    // }
}


