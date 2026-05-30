package com.thedal.thedal_app.util;

import lombok.Data;

@Data
public class Response<T> {

    private String message;
    private T data;
    private boolean success;
}
