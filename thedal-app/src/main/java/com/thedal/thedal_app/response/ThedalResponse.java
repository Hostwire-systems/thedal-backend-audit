package com.thedal.thedal_app.response;

import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.thedal.thedal_app.thedal_exception.ThedalError;

@JsonInclude(JsonInclude.Include.NON_NULL)

public class ThedalResponse<T> {
     private String status; // 'success', 'error', or 'warn'
 
    private Integer code;
    private String message;

  // private List<ThedalResponse> statuses;
    private T data;

//    private static final Map<Integer, String> ERROR_MAP = new HashMap<>();
//    private static final Map<Integer, String> SUCCESS_MAP = new HashMap<>();
    
//    static {
//        for (ThedalError error : ThedalError.values()) {
//            ERROR_MAP.put(error.getCode(), error.getMessage());
//        }
//        for (ThedalSuccess success : ThedalSuccess.values()) {
//            SUCCESS_MAP.put(success.getCode(), success.getMessage());
//        }
//    }

//    public ThedalResponse(String status, Integer code, String message) {
//        this.status = status;
//        this.code = code;
//        this.message = message;
//        // statuses.add(this);
//    }

    public ThedalResponse() {
    }

    // Constructor for success responses using ThedalSuccess
    public ThedalResponse(ThedalSuccess success,T data) {
    	this.status = ThedalResponseStatus.SUCCESS.name().toLowerCase();
        this.code = success.getCode();
        this.message = success.getMessage();
        this.data = data;
    }
    
    public ThedalResponse(ThedalSuccess success) {
    	this(success, null);
    }
    
    // Constructor for error responses with data
    public ThedalResponse(ThedalError error, T data) {
        this.status = ThedalResponseStatus.ERROR.name().toLowerCase();
        this.code = error.getCode();
        this.message = error.getMessage();
        this.data = data;
    }

    // Constructor for error responses without data
    public ThedalResponse(ThedalError error) {
        this(error, null);
    }
    
    // Constructor for simple messages with success/error status
    public ThedalResponse(String message, T data, boolean success) {
        this.message = message;
        this.data = data;
        this.status = success ? "success" : "error";
        this.code = success ? 200000 : 500000;
    }
    
//    public ThedalResponse(ThedalError error, HttpStatus status, T data) {
//        this.status = ThedalResponseStatus.ERROR.name().toLowerCase();
//        this.code = error.getCode();
//        this.message = error.getMessage();
//        this.data = data;
//    }

   
//    public static ThedalResponse get(Object responseCode) {
//        if (responseCode instanceof ThedalSuccess) {
//            ThedalSuccess successCode = (ThedalSuccess) responseCode;
//            Integer code = successCode.getCode();
//            String message = SUCCESS_MAP.getOrDefault(code, "Unknown code");
//            return createSingleResponse("success", code, message);
//        } else if (responseCode instanceof ThedalError) {
//            ThedalError errorCode = (ThedalError) responseCode;
//            Integer code = errorCode.getCode();
//            String message = ERROR_MAP.getOrDefault(code, "Unknown code");
//            return createSingleResponse("error", code, message);
//        }
//        
//        else {
//            return createSingleResponse("error", 500, "Invalid request");
//        }
//    }
//
//
//    public static ThedalResponse createSingleResponse(String status, Integer code, String message) {
//        ThedalResponse response = new ThedalResponse();
//        response.statuses = Collections.singletonList(new ThedalResponse(status, code, message));
//        return response;
//    }
//
//    
//    public static ThedalResponse createListResponse(List<ThedalResponse> statuses) {
//        ThedalResponse response = new ThedalResponse();
//        response.statuses = statuses;
//        return response;
//    }
//    
//
//    public static ThedalResponse createDataResponse(String status, Integer code, String message, Object data) {
//        ThedalResponse response = new ThedalResponse();
//        response.status = status;
//        response.code = code;
//        response.message = message;
//        response.data = data;
//        return response;
//    }
//
//    public List<ThedalResponse> getstatuses() {
//        return statuses;
//    }
//
//    public void setstatuses(List<ThedalResponse> statuses) {
//        this.statuses = statuses;
//    }

   

    public String getStatus() {
        return status;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

	public T getData() {
		return data;
	}

	public void setData(T data) {
		this.data = data;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public void setCode(Integer code) {
		this.code = code;
	}

	public void setMessage(String message) {
		this.message = message;
	}

    public void setResponse(ThedalSuccess success) {
    	this.status = ThedalResponseStatus.SUCCESS.name().toLowerCase();
        this.code = success.getCode();
        this.message = success.getMessage();
    }

    public void setResponse(ThedalSuccess success, T data) {
    	this.status = ThedalResponseStatus.SUCCESS.name().toLowerCase();
        this.code = success.getCode();
        this.message = success.getMessage();
        this.data = data;
    }

    public void setResponse(ThedalError error) {
        this.status = ThedalResponseStatus.ERROR.name().toLowerCase();
        this.code = error.getCode();
        this.message = error.getMessage();
    }

    public void setResponse(ThedalError error, T data) {
        this.status = ThedalResponseStatus.ERROR.name().toLowerCase();
        this.code = error.getCode();
        this.message = error.getMessage();
        this.data = data;
    }

	public void setSuccess(boolean b) {
		// TODO Auto-generated method stub
		
	}

}




