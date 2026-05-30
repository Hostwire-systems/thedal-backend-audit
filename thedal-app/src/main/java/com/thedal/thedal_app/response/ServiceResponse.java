package com.thedal.thedal_app.response;

import com.fasterxml.jackson.annotation.JsonInclude;

// Exclude null fields from JSON serialization
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ServiceResponse<T> {
    private String status;  // e.g., "success" or "error"
    private Integer code;   // e.g., 200 for success, 404 for not found
    private String message; // e.g., "Successfully retrieved voter locations URL"
    private T data;         // The actual data, like the URL string

    // Constructor to initialize all fields
    public ServiceResponse(String status, Integer code, String message, T data) {
        this.status = status;
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // Getters and setters
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}