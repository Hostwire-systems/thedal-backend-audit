//package com.thedal.thedal_app;
//
//import java.util.HashMap;
//import java.util.LinkedHashMap;
//import java.util.Map;
//
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.MethodArgumentNotValidException;
//import org.springframework.web.bind.annotation.ControllerAdvice;
//import org.springframework.web.bind.annotation.ExceptionHandler;
//
//import lombok.extern.slf4j.Slf4j;
//
//@Slf4j
//@ControllerAdvice
//public class GlobalExceptionHandler {
//
//    @ExceptionHandler(value = IllegalArgumentException.class)
//    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException e) {
//        log.error("Handled excpetion: {}", e.getMessage());
//
//        Map<String, Object> response = new LinkedHashMap<>();
//        response.put("message", e.getMessage());
//        response.put("success", false);
//        response.put("data", 0);
//
//        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
//    }
//
//
//    @ExceptionHandler(MethodArgumentNotValidException.class)
//    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
//        log.error("Handled excpetion: {}", ex.getMessage());
//
//        Map<String, Object> response = new HashMap<>();
//        StringBuilder errors = new StringBuilder();
//        ex.getBindingResult().getAllErrors().forEach((error) -> {
//            String errorMessage = error.getDefaultMessage();
//            errors.append(errorMessage).append(", ");
//        });
//
//        response.put("message", errors.toString().substring(0, errors.toString().length() - 2));
//        response.put("success", false);
//        response.put("data", 0);
//        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
//    }
//}