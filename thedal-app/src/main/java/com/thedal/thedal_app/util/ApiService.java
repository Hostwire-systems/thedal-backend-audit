package com.thedal.thedal_app.util;

import java.net.URI;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ApiService {
    private final RestTemplate restTemplate;

    public ApiService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // GET request
    public String getRequest(String url) {
        try {
            log.info("OTP sending with URL: " + url);
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            log.info("OTP sent for URL: " + url);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            // Handles HTTP error (4xx and 5xx errors)
            log.error("OTP error for URL: " + url);
            log.error("HTTP error occurred: {}", e.getStatusCode());
            throw e;
        } catch (RestClientException e) {
            // Handles other client-side errors (e.g., connection timeout)
            log.error("OTP error for URL: " + url);
            log.error("Client error occurred: {}", e.getMessage());
            throw e;
        }
    }

    
    public ResponseEntity<String> makePostRequestWithParams(String url, HttpEntity<Map<String, Object>> entity) {
        log.info("Constructed API URL: {}", url);
    
        try {
            // Make the POST request
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
    
            log.info("API Response Status: {}", response.getStatusCode());
            log.info("API Response Body: {}", response.getBody());
    
            if (response.getStatusCode().is2xxSuccessful()) {
                return response; // Return the success response
            } else {
                log.error("Transactional OTP error with status code: {}", response.getStatusCode());
                log.error("Transactional OTP error with response: {}", response.getBody());
                return ResponseEntity.status(response.getStatusCode()).body(response.getBody()); // Return response instead of throwing exception
            }
        } catch (Exception e) {
            log.error("Exception while making POST request: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error while calling OTP API");
        }
    }
    
}
