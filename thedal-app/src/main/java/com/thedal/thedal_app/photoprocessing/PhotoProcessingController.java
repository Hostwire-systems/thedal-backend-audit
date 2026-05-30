package com.thedal.thedal_app.photoprocessing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/photo-processing")
@Tag(name = "Photo Processing", description = "Voter photo extraction and processing")
@Slf4j
public class PhotoProcessingController {

    @Autowired
    private PhotoProcessingService photoProcessingService;

    @PostMapping("/extract-photos")
    @Operation(summary = "Extract voter photos from PDF", 
              description = "Upload a PDF file and extract voter photos using OCR service")
    @ApiResponse(responseCode = "200", description = "Photos extracted successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request or file format")
    @ApiResponse(responseCode = "500", description = "Processing error")
    public ResponseEntity<Map<String, Object>> extractPhotosFromPdf(
            @Parameter(description = "PDF file containing voter photos", required = true)
            @RequestParam("file") MultipartFile pdfFile,
            
            @Parameter(description = "Part number", required = true)
            @RequestParam("partNo") String partNo,
            
            @Parameter(description = "Election ID", required = true)
            @RequestParam("electionId") Long electionId,
            
            @Parameter(description = "Account ID", required = true)
            @RequestParam("accountId") Long accountId,
            
            @Parameter(description = "Starting page number for extraction (1-based, optional, defaults to 3)")
            @RequestParam(value = "startPage", required = false) Integer startPage,
            
            @Parameter(description = "Ending page number for extraction (1-based, optional, defaults to second-to-last page)")
            @RequestParam(value = "endPage", required = false) Integer endPage,
            
            HttpServletRequest request) {

        try {
            log.info("Received photo extraction request - Part: {}, Election: {}, File: {}, Pages: {} to {}", 
                    partNo, electionId, pdfFile.getOriginalFilename(), startPage, endPage);

            // Get requester info
            String requestedBy = request.getRemoteUser(); // Or get from security context
            if (requestedBy == null) {
                requestedBy = "system";
            }

            // Validate inputs
            if (pdfFile.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("PDF file is required"));
            }

            if (partNo == null || partNo.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Part number is required"));
            }

            if (electionId == null) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Election ID is required"));
            }

            if (accountId == null) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Account ID is required"));
            }

            // Validate page parameters
            if (startPage != null && startPage < 1) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Start page must be at least 1"));
            }

            if (endPage != null && endPage < 1) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("End page must be at least 1"));
            }

            if (startPage != null && endPage != null && startPage >= endPage) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Start page must be less than end page"));
            }

            // Process PDF and extract photos
            PhotoProcessingResult result = photoProcessingService.processPdfAndExtractPhotos(
                    pdfFile, partNo, electionId, accountId, requestedBy, startPage, endPage);

            if (result.isSuccess()) {
                return ResponseEntity.ok(createSuccessResponse(result));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(createErrorResponse(result.getError()));
            }

        } catch (IllegalArgumentException e) {
            log.error("Invalid input for photo extraction: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error during photo extraction", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Internal server error: " + e.getMessage()));
        }
    }

    @PostMapping("/extract-photos-async")
    @Operation(summary = "Extract voter photos from PDF (Async)", 
              description = "Upload a PDF file and start async photo extraction. Returns immediately with job ID.")
    @ApiResponse(responseCode = "200", description = "Processing started successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request or file format")
    @ApiResponse(responseCode = "500", description = "Failed to start processing")
    public ResponseEntity<Map<String, Object>> extractPhotosFromPdfAsync(
            @Parameter(description = "PDF file containing voter photos", required = true)
            @RequestParam("file") MultipartFile pdfFile,
            
            @Parameter(description = "Part number", required = true)
            @RequestParam("partNo") String partNo,
            
            @Parameter(description = "Election ID", required = true)
            @RequestParam("electionId") Long electionId,
            
            @Parameter(description = "Account ID", required = true)
            @RequestParam("accountId") Long accountId,
            
            @Parameter(description = "Starting page number for extraction (1-based, optional, defaults to 3)")
            @RequestParam(value = "startPage", required = false) Integer startPage,
            
            @Parameter(description = "Ending page number for extraction (1-based, optional, defaults to second-to-last page)")
            @RequestParam(value = "endPage", required = false) Integer endPage,
            
            HttpServletRequest request) {

        try {
            log.info("Received async photo extraction request - Part: {}, Election: {}, File: {}, Pages: {} to {}", 
                    partNo, electionId, pdfFile.getOriginalFilename(), startPage, endPage);

            // Get requester info
            String requestedBy = request.getRemoteUser();
            if (requestedBy == null) {
                requestedBy = "system";
            }

            // Validate inputs
            if (pdfFile.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("PDF file is required"));
            }

            if (partNo == null || partNo.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Part number is required"));
            }

            if (electionId == null) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Election ID is required"));
            }

            if (accountId == null) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Account ID is required"));
            }

            // Validate page parameters
            if (startPage != null && startPage < 1) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Start page must be at least 1"));
            }

            if (endPage != null && endPage < 1) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("End page must be at least 1"));
            }

            if (startPage != null && endPage != null && startPage >= endPage) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Start page must be less than end page"));
            }

            // Start async processing and get job ID immediately
            String jobId = photoProcessingService.startAsyncPhotoExtraction(
                    pdfFile, partNo, electionId, accountId, requestedBy, startPage, endPage);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("jobId", jobId);
            response.put("message", "PDF processing started successfully");
            response.put("statusUrl", "/api/photo-processing/processing-status/" + jobId);
            
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Invalid input for async photo extraction: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error during async photo extraction", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Internal server error: " + e.getMessage()));
        }
    }

    @GetMapping("/job-status/{jobId}")
    @Operation(summary = "Get job status", description = "Get the status of a photo extraction job")
    public ResponseEntity<Map<String, Object>> getJobStatus(
            @Parameter(description = "Job ID", required = true)
            @PathVariable String jobId) {

        try {
            PhotoProcessingStatus status = photoProcessingService.getJobStatus(jobId);
            
            if (status != null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("jobId", jobId);
                response.put("status", status);
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("jobId", jobId);
                response.put("error", "Job not found");
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            log.error("Error getting job status for: {}", jobId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error getting job status: " + e.getMessage()));
        }
    }

    @GetMapping("/processing-status/{jobId}")
    @Operation(summary = "Get processing status", description = "Get detailed processing status for frontend polling")
    public ResponseEntity<Map<String, Object>> getProcessingStatus(
            @Parameter(description = "Job ID", required = true)
            @PathVariable String jobId) {

        try {
            PhotoProcessingStatus status = photoProcessingService.getJobStatus(jobId);
            
            Map<String, Object> response = new HashMap<>();
            
            if (status != null) {
                response.put("success", true);
                response.put("jobId", jobId);
                response.put("status", status.getStatus());
                response.put("message", status.getMessage());
                response.put("progress", status.getProgressPercentage());
                response.put("totalPhotos", status.getTotalPhotos());
                response.put("processedPhotos", status.getProcessedPhotos());
                response.put("successfulUpdates", status.getSuccessfulUpdates());
                response.put("failedUpdates", status.getFailedUpdates());
                response.put("startTime", status.getStartTime());
                response.put("endTime", status.getEndTime());
                
                // Add completion flag for frontend
                response.put("isCompleted", "COMPLETED".equals(status.getStatus()) || "FAILED".equals(status.getStatus()));
                
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("jobId", jobId);
                response.put("error", "Job not found");
                response.put("isCompleted", true); // Job not found means it's "completed" in some sense
                return ResponseEntity.ok(response); // Return 200 but with error flag
            }

        } catch (Exception e) {
            log.error("Error getting processing status for: {}", jobId, e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("jobId", jobId);
            response.put("error", "Error getting status: " + e.getMessage());
            response.put("isCompleted", true);
            return ResponseEntity.ok(response);
        }
    }

    @GetMapping("/voter-photo/{jobId}/{serialNo}")
    @Operation(summary = "Get voter photo", description = "Download a specific voter photo")
    public ResponseEntity<byte[]> getVoterPhoto(
            @Parameter(description = "Job ID", required = true)
            @PathVariable String jobId,
            
            @Parameter(description = "Voter serial number", required = true)
            @PathVariable Long serialNo) {

        try {
            byte[] photoBytes = photoProcessingService.getVoterPhoto(jobId, serialNo);
            
            if (photoBytes != null) {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.IMAGE_JPEG);
                headers.setContentLength(photoBytes.length);
                headers.add(HttpHeaders.CONTENT_DISPOSITION, 
                           String.format("attachment; filename=voter_%03d.jpg", serialNo));
                
                return new ResponseEntity<>(photoBytes, headers, HttpStatus.OK);
            } else {
                return ResponseEntity.notFound().build();
            }

        } catch (IOException e) {
            log.error("Error reading photo file for job: {} serial: {}", jobId, serialNo, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            log.error("Unexpected error getting voter photo for job: {} serial: {}", jobId, serialNo, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/health-check")
    @Operation(summary = "Health check", description = "Check the health of photo processing services")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        
        Map<String, Object> health = new HashMap<>();
        health.put("timestamp", System.currentTimeMillis());
        health.put("service", "Photo Processing Service");
        
        // Check OCR service health
        boolean ocrHealthy = photoProcessingService.isOcrServiceHealthy();
        health.put("ocrServiceHealthy", ocrHealthy);
        
        if (ocrHealthy) {
            health.put("status", "UP");
            health.put("message", "All services are healthy");
            return ResponseEntity.ok(health);
        } else {
            health.put("status", "DOWN");
            health.put("message", "OCR service is unavailable");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(health);
        }
    }

    @PostMapping("/cleanup")
    @Operation(summary = "Cleanup old jobs", description = "Cleanup old photo processing jobs and files")
    public ResponseEntity<Map<String, Object>> cleanupOldJobs() {
        
        try {
            photoProcessingService.cleanupOldJobs();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Cleanup completed successfully");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error during cleanup", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Cleanup error: " + e.getMessage()));
        }
    }

    // Helper methods
    private Map<String, Object> createSuccessResponse(PhotoProcessingResult result) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("jobId", result.getJobId());
        response.put("message", "Photos processed successfully");
        
        if (result.getExtractionResponse() != null) {
            response.put("extraction", Map.of(
                "totalPhotosExtracted", result.getExtractionResponse().getPhotos().size(),
                "metadata", result.getExtractionResponse().getMetadata()
            ));
        }
        
        if (result.getUpdateResult() != null) {
            response.put("voterUpdates", Map.of(
                "totalProcessed", result.getUpdateResult().getTotalProcessed(),
                "successful", result.getUpdateResult().getTotalSuccessful(),
                "failed", result.getUpdateResult().getTotalFailed(),
                "successfulUpdates", result.getUpdateResult().getSuccessfulUpdates(),
                "failedUpdates", result.getUpdateResult().getFailedUpdates()
            ));
        }
        
        return response;
    }
    
    private Map<String, Object> createErrorResponse(String error) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", error);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }
}
