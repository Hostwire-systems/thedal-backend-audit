package com.thedal.thedal_app.proxy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Collections;
import java.util.Enumeration;

/**
 * Proxy controller to route reporting requests to the dedicated reporting service
 * 
 * DISABLED: All reporting endpoints are now in the main application.
 * This proxy is kept for potential future use if we split reporting into a separate microservice.
 * 
 * Local reporting controllers:
 * - ElectionDashboardStatsController
 * - ElectionDemographicsController  
 * - ElectionPartyPollingController
 * - ElectionFeedbackIssuesController
 * - ElectionBoothProgressController
 * - ElectionContactStatusController
 * - CadreDashboardController
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@Order(Ordered.LOWEST_PRECEDENCE) // Process this controller last
public class ReportingProxyController {

    private final RestTemplate restTemplate;
    
    @Value("${reporting.service.url:http://localhost:8081}")
    private String reportingServiceUrl;

    // DISABLED: All reporting APIs are in main app now
    // If you need to proxy to a separate reporting service, change the pattern below
    // Currently set to a pattern that will never match to disable proxying
    @RequestMapping(value = "/THIS_PROXY_IS_DISABLED/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<?> proxyApiReporting(HttpServletRequest request, @RequestBody(required = false) Object body) {
        String requestURI = request.getRequestURI();
        
        // Skip proxy for poll-day endpoints that are handled by main app controllers
        if (requestURI.matches(".*/api/reporting/poll-day/.*")) {
            log.debug("Skipping proxy for local poll-day endpoint: {}", requestURI);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Endpoint handled locally");
        }
        
        String path = requestURI.substring("/api/reporting".length());
        return forwardRequest(request, body, "/api" + path);
    }

    // DISABLED: All reporting APIs are in main app now  
    // @RequestMapping(value = "/reporting/api/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<?> proxyReportingApi(HttpServletRequest request, @RequestBody(required = false) Object body) throws org.springframework.web.HttpRequestMethodNotSupportedException {
        String requestURI = request.getRequestURI();
        
        // DO NOT proxy aggregates or poll-day endpoints - they are handled by local controllers
        if (requestURI.startsWith("/reporting/api/aggregates/") || 
            requestURI.contains("/reporting/poll-day/")) {
            log.debug("Skipping proxy for local endpoint: {} - will be handled by local @RestController", requestURI);
            // We MUST throw an exception or return NOT_FOUND to let Spring retry with other handlers
            throw new org.springframework.web.HttpRequestMethodNotSupportedException(request.getMethod());
        }
        
        String path = requestURI.substring("/reporting".length());
        return forwardRequest(request, body, path);
    }

    private ResponseEntity<?> forwardRequest(HttpServletRequest request, Object body, String targetPath) {
        try {
            // Build target URL with query parameters
            UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(reportingServiceUrl + targetPath);
            
            if (request.getQueryString() != null) {
                builder.query(request.getQueryString());
            }
            
            URI targetUri = builder.build(true).toUri();
            
            // Copy headers
            HttpHeaders headers = new HttpHeaders();
            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                if (!"host".equalsIgnoreCase(headerName) && !"content-length".equalsIgnoreCase(headerName)) {
                    headers.put(headerName, Collections.list(request.getHeaders(headerName)));
                }
            }
            
            // Create request entity
            HttpEntity<?> entity = new HttpEntity<>(body, headers);
            
            // Forward request
            HttpMethod method = HttpMethod.valueOf(request.getMethod());
            log.info("Forwarding {} request to: {}", method, targetUri);
            
            ResponseEntity<Object> response = restTemplate.exchange(
                targetUri, 
                method, 
                entity, 
                Object.class
            );
            
            return ResponseEntity.status(response.getStatusCode())
                .headers(response.getHeaders())
                .body(response.getBody());
                
        } catch (Exception e) {
            log.error("Error forwarding request to reporting service: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("Reporting service is currently unavailable");
        }
    }
}
