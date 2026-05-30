package com.thedal.thedal_app.photoprocessing;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;

import lombok.extern.slf4j.Slf4j;
import java.util.UUID;
import java.time.LocalDateTime;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@Slf4j
public class PythonOcrIntegrationService {

    @Value("${thedal.ocr.service.url:http://localhost:5000}")
    private String ocrServiceUrl;

    @Value("${thedal.ocr.service.timeout:300000}")
    private int ocrServiceTimeout;

    @Value("${thedal.photo.storage.path:${java.io.tmpdir}/thedal-photos}")
    private String photoStoragePath;

    private final RestTemplate restTemplate;

    public PythonOcrIntegrationService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Extract photos from PDF using Python OCR service
     */
    public PhotoExtractionResponse extractPhotosFromPdf(PhotoExtractionRequest request) {
        try {
            log.info("Starting photo extraction for job: {} part: {} election: {}", 
                    request.getJobId(), request.getPartNo(), request.getElectionId());

            // Validate OCR service availability
            if (!isOcrServiceHealthy()) {
                return createErrorResponse(request.getJobId(), "OCR service is not available");
            }

            // Prepare multipart request
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            
            // Add PDF file
            ByteArrayResource pdfResource = new ByteArrayResource(request.getPdfData()) {
                @Override
                public String getFilename() {
                    return request.getFilename();
                }
            };
            body.add("file", pdfResource);

            // Add metadata
            body.add("part_no", request.getPartNo());
            body.add("election_id", request.getElectionId().toString());
            body.add("job_id", request.getJobId());
            
            // Add page range parameters if specified
            if (request.getStartPage() != null) {
                body.add("start_page", request.getStartPage().toString());
                log.info("Setting start page: {}", request.getStartPage());
            }
            
            if (request.getEndPage() != null) {
                body.add("end_page", request.getEndPage().toString());
                log.info("Setting end page: {}", request.getEndPage());
            }

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // Call Python OCR service
            log.info("Calling Python OCR service at: {}/extract-photos", ocrServiceUrl);
            ResponseEntity<PhotoExtractionResponse> response = restTemplate.exchange(
                    ocrServiceUrl + "/extract-photos",
                    HttpMethod.POST,
                    requestEntity,
                    PhotoExtractionResponse.class
            );

            PhotoExtractionResponse extractionResponse = response.getBody();
            
            if (extractionResponse != null && extractionResponse.isSuccess()) {
                log.info("Photo extraction successful for job: {} - {} photos extracted", 
                        request.getJobId(), extractionResponse.getPhotos().size());
                
                // Download and store photos locally
                downloadAndStorePhotos(extractionResponse);
                
                return extractionResponse;
            } else {
                log.error("Photo extraction failed for job: {} - {}", 
                        request.getJobId(), extractionResponse != null ? extractionResponse.getError() : "Unknown error");
                return extractionResponse != null ? extractionResponse : 
                       createErrorResponse(request.getJobId(), "Unknown extraction error");
            }

        } catch (ResourceAccessException e) {
            log.error("Failed to connect to OCR service: {}", e.getMessage());
            return createErrorResponse(request.getJobId(), "OCR service connection failed: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during photo extraction for job: {}", request.getJobId(), e);
            return createErrorResponse(request.getJobId(), "Extraction error: " + e.getMessage());
        }
    }

    /**
     * Download photos from Python service and store locally
     */
    private void downloadAndStorePhotos(PhotoExtractionResponse extractionResponse) {
        try {
            String jobId = extractionResponse.getJobId();
            
            // Create local storage directory
            Path jobStoragePath = Paths.get(photoStoragePath, jobId);
            Files.createDirectories(jobStoragePath);

            log.info("Downloading {} photos for job: {}", 
                    extractionResponse.getPhotos().size(), jobId);

            for (ExtractedPhoto photo : extractionResponse.getPhotos()) {
                try {
                    downloadPhoto(jobId, photo.getSerialNo().intValue(), jobStoragePath);
                } catch (Exception e) {
                    log.error("Failed to download photo for serial: {} in job: {}", 
                             photo.getSerialNo(), jobId, e);
                }
            }

        } catch (Exception e) {
            log.error("Error downloading photos for job: {}", extractionResponse.getJobId(), e);
        }
    }

    /**
     * Download a specific photo from the Python service
     */
    private void downloadPhoto(String jobId, int serialNo, Path storagePath) throws IOException {
        String downloadUrl = String.format("%s/download-photo/%s/%d", 
                                          ocrServiceUrl, jobId, serialNo);
        
        log.debug("Downloading photo from: {}", downloadUrl);
        
        ResponseEntity<byte[]> response = restTemplate.exchange(
                downloadUrl,
                HttpMethod.GET,
                null,
                byte[].class
        );

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            String filename = String.format("voter_%03d.jpg", serialNo);
            Path photoPath = storagePath.resolve(filename);
            
            Files.write(photoPath, response.getBody());
            log.debug("Saved photo: {}", photoPath);
        } else {
            throw new IOException("Failed to download photo for serial: " + serialNo);
        }
    }

    /**
     * Get job status from Python service
     */
    public PhotoExtractionResponse getJobStatus(String jobId) {
        try {
            String statusUrl = String.format("%s/job-status/%s", ocrServiceUrl, jobId);
            
            ResponseEntity<PhotoExtractionResponse> response = restTemplate.getForEntity(
                    statusUrl, PhotoExtractionResponse.class);
            
            return response.getBody();
            
        } catch (Exception e) {
            log.error("Error getting job status for: {}", jobId, e);
            return createErrorResponse(jobId, "Failed to get job status: " + e.getMessage());
        }
    }

    /**
     * Check if OCR service is healthy
     */
    public boolean isOcrServiceHealthy() {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    ocrServiceUrl + "/health", String.class);
            
            return response.getStatusCode() == HttpStatus.OK;
            
        } catch (Exception e) {
            log.error("OCR service health check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get local photo file path for a voter
     */
    public String getLocalPhotoPath(String jobId, Long serialNo) {
        Path photoPath = Paths.get(photoStoragePath, jobId, String.format("voter_%03d.jpg", serialNo));
        return Files.exists(photoPath) ? photoPath.toString() : null;
    }

    /**
     * Get photo file as byte array
     */
    public byte[] getPhotoBytes(String jobId, Long serialNo) throws IOException {
        String photoPath = getLocalPhotoPath(jobId, serialNo);
        if (photoPath != null) {
            return Files.readAllBytes(Paths.get(photoPath));
        }
        return null;
    }

    /**
     * Cleanup old photo extraction jobs
     */
    public void cleanupOldJobs(int maxAgeHours) {
        try {
            Path storageRoot = Paths.get(photoStoragePath);
            
            if (Files.exists(storageRoot)) {
                Files.list(storageRoot)
                    .filter(Files::isDirectory)
                    .filter(path -> isJobDirectoryOld(path, maxAgeHours))
                    .forEach(this::deleteJobDirectory);
            }
            
            // Also trigger cleanup on Python service
            restTemplate.postForEntity(ocrServiceUrl + "/cleanup", null, String.class);
            
        } catch (Exception e) {
            log.error("Error during cleanup: {}", e.getMessage());
        }
    }

    private boolean isJobDirectoryOld(Path jobDir, int maxAgeHours) {
        try {
            long ageHours = (System.currentTimeMillis() - Files.getLastModifiedTime(jobDir).toMillis()) 
                           / (1000 * 60 * 60);
            return ageHours > maxAgeHours;
        } catch (Exception e) {
            return false;
        }
    }

    private void deleteJobDirectory(Path jobDir) {
        try {
            Files.walk(jobDir)
                .sorted((a, b) -> b.compareTo(a)) // Delete files before directories
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (Exception e) {
                        log.warn("Failed to delete: {}", path);
                    }
                });
        } catch (Exception e) {
            log.error("Error deleting job directory: {}", jobDir, e);
        }
    }

    private PhotoExtractionResponse createErrorResponse(String jobId, String error) {
        PhotoExtractionResponse response = new PhotoExtractionResponse();
        response.setJobId(jobId);
        response.setSuccess(false);
        response.setError(error);
        response.setMessage("Photo extraction failed");
        return response;
    }
}
