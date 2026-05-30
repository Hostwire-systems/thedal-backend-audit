package com.thedal.thedal_app.photoprocessing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.thedal.thedal_app.awsfilestore.AwsFileUpload;
import com.thedal.thedal_app.voter.VoterEntity;
import com.thedal.thedal_app.voter.VoterRepo;
import com.thedal.thedal_app.election.ElectionRepository;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class PhotoProcessingService {

    @Autowired
    private PythonOcrIntegrationService ocrIntegrationService;

    @Autowired
    private VoterRepo voterRepository;

    @Autowired
    private ElectionRepository electionRepository;

    @Autowired
    private AwsFileUpload awsFileUpload;
    
    @Autowired
    private ApplicationContext applicationContext;    @Value("${aws.s3.image.bucket}")
    private String s3ImageBucket;
    
    // In-memory storage for job status tracking
    private final Map<String, PhotoProcessingStatus> jobStatusMap = new ConcurrentHashMap<>();

    /**
     * Start PDF processing asynchronously and return job ID immediately
     */
    public PhotoProcessingResult processPdfAndExtractPhotos(
            MultipartFile pdfFile, 
            String partNo, 
            Long electionId, 
            Long accountId,
            String requestedBy,
            Integer startPage,
            Integer endPage) {

        String jobId = UUID.randomUUID().toString();
        
        try {
            log.info("Starting PDF photo processing - Job: {} Part: {} Election: {} Pages: {} to {}", 
                    jobId, partNo, electionId, startPage, endPage);

            // Validate inputs
            if (pdfFile.isEmpty()) {
                throw new IllegalArgumentException("PDF file is empty");
            }

            if (!pdfFile.getOriginalFilename().toLowerCase().endsWith(".pdf")) {
                throw new IllegalArgumentException("File must be a PDF");
            }

            // Validate page parameters
            if (startPage != null && startPage < 1) {
                throw new IllegalArgumentException("Start page must be at least 1");
            }

            if (endPage != null && endPage < 1) {
                throw new IllegalArgumentException("End page must be at least 1");
            }

            if (startPage != null && endPage != null && startPage >= endPage) {
                throw new IllegalArgumentException("Start page must be less than end page");
            }

            // Validate election exists
            if (!electionRepository.existsById(electionId)) {
                throw new IllegalArgumentException("Election not found: " + electionId);
            }

            // Create initial status
            PhotoProcessingStatus status = new PhotoProcessingStatus();
            status.setJobId(jobId);
            status.setStatus("STARTED");
            status.setMessage("PDF processing started");
            status.setStartTime(LocalDateTime.now());
            status.setProgressPercentage(0.0);
            
            jobStatusMap.put(jobId, status);
            
            // Start async processing
            processPhotosAsync(pdfFile, partNo, electionId, accountId, requestedBy, jobId, startPage, endPage);
            
            // Return immediate response
            return PhotoProcessingResult.builder()
                    .success(true)
                    .jobId(jobId)
                    .message("PDF processing started successfully")
                    .build();

        } catch (Exception e) {
            log.error("Error starting PDF processing for job: {}", jobId, e);
            return PhotoProcessingResult.failure(jobId, "Failed to start processing: " + e.getMessage());
        }
    }
    
    /**
     * Start PDF processing asynchronously and return job ID immediately (true async)
     */
    public String startAsyncPhotoExtraction(
            MultipartFile pdfFile, 
            String partNo, 
            Long electionId, 
            Long accountId,
            String requestedBy,
            Integer startPage,
            Integer endPage) {

        String jobId = UUID.randomUUID().toString();
        
        try {
            log.info("Starting async PDF photo processing - Job: {} Part: {} Election: {} Pages: {} to {}", 
                    jobId, partNo, electionId, startPage, endPage);

            // Validate inputs
            if (pdfFile.isEmpty()) {
                throw new IllegalArgumentException("PDF file is empty");
            }

            if (!pdfFile.getOriginalFilename().toLowerCase().endsWith(".pdf")) {
                throw new IllegalArgumentException("File must be a PDF");
            }

            // Validate election exists
            if (!electionRepository.existsById(electionId)) {
                throw new IllegalArgumentException("Election not found: " + electionId);
            }

            // Create initial status
            PhotoProcessingStatus status = new PhotoProcessingStatus();
            status.setJobId(jobId);
            status.setStatus("STARTED");
            status.setMessage("PDF processing started");
            status.setStartTime(LocalDateTime.now());
            status.setProgressPercentage(0.0);
            
            jobStatusMap.put(jobId, status);
            
            // Start async processing using ApplicationContext to ensure @Async works
            PhotoProcessingService asyncService = applicationContext.getBean(PhotoProcessingService.class);
            asyncService.processPhotosAsync(pdfFile, partNo, electionId, accountId, requestedBy, jobId, startPage, endPage);
            
            // Return job ID immediately
            return jobId;

        } catch (Exception e) {
            log.error("Error starting PDF processing for job: {}", jobId, e);
            
            // Update status with error
            PhotoProcessingStatus errorStatus = new PhotoProcessingStatus();
            errorStatus.setJobId(jobId);
            errorStatus.setStatus("FAILED");
            errorStatus.setMessage("Failed to start processing: " + e.getMessage());
            errorStatus.setStartTime(LocalDateTime.now());
            errorStatus.setEndTime(LocalDateTime.now());
            errorStatus.setProgressPercentage(0.0);
            jobStatusMap.put(jobId, errorStatus);
            
            throw new RuntimeException("Failed to start processing: " + e.getMessage());
        }
    }
    
    /**
     * Process photos asynchronously
     */
    @Async
    public CompletableFuture<Void> processPhotosAsync(
            MultipartFile pdfFile, 
            String partNo, 
            Long electionId, 
            Long accountId,
            String requestedBy,
            String jobId,
            Integer startPage,
            Integer endPage) {
        
        PhotoProcessingStatus status = jobStatusMap.get(jobId);
        
        try {
            status.setStatus("PROCESSING");
            status.setMessage("Extracting photos from PDF");
            status.setProgressPercentage(10.0);

            // Create extraction request
            PhotoExtractionRequest extractionRequest = new PhotoExtractionRequest();
            extractionRequest.setJobId(jobId);
            extractionRequest.setPartNo(partNo);
            extractionRequest.setElectionId(electionId);
            extractionRequest.setFilename(pdfFile.getOriginalFilename());
            extractionRequest.setPdfData(pdfFile.getBytes());
            extractionRequest.setRequestTime(LocalDateTime.now());
            extractionRequest.setRequestedBy(requestedBy);
            extractionRequest.setAccountId(accountId);
            extractionRequest.setStartPage(startPage);
            extractionRequest.setEndPage(endPage);

            // Extract photos using Python service
            PhotoExtractionResponse extractionResponse = ocrIntegrationService.extractPhotosFromPdf(extractionRequest);

            if (!extractionResponse.isSuccess()) {
                throw new RuntimeException("Photo extraction failed: " + extractionResponse.getError());
            }

            status.setTotalPhotos(extractionResponse.getPhotos().size());
            status.setMessage("Uploading photos to S3 and updating voter records");
            status.setProgressPercentage(30.0);

            // Process each extracted photo with S3 upload
            PhotoUpdateResult updateResult = updateVoterPhotosWithS3Upload(
                    extractionResponse, partNo, electionId, accountId, jobId, status);

            // Update final status
            status.setStatus("COMPLETED");
            status.setMessage("Photo processing completed successfully");
            status.setEndTime(LocalDateTime.now());
            status.setSuccessfulUpdates(updateResult.getSuccessfulUpdates().size());
            status.setFailedUpdates(updateResult.getFailedUpdates().size());
            status.setProgressPercentage(100.0);

            log.info("Photo processing completed - Job: {}, Success: {}, Failed: {}", 
                    jobId, updateResult.getSuccessfulUpdates().size(), updateResult.getFailedUpdates().size());

        } catch (Exception e) {
            log.error("Error in async photo processing for job: " + jobId, e);
            status.setStatus("FAILED");
            status.setMessage("Processing failed: " + e.getMessage());
            status.setEndTime(LocalDateTime.now());
            status.setProgressPercentage(0.0);
        }
        
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Update voter records with extracted photo information
     */
    private PhotoUpdateResult updateVoterPhotos(
            PhotoExtractionResponse extractionResponse, 
            String partNo, 
            Long electionId, 
            Long accountId) {

        log.info("Updating voter photos for {} extracted photos", 
                extractionResponse.getPhotos().size());

        List<VoterPhotoUpdate> successfulUpdates = new ArrayList<>();
        List<VoterPhotoUpdate> failedUpdates = new ArrayList<>();

        for (ExtractedPhoto photo : extractionResponse.getPhotos()) {
            try {
                // Find voter by serial number, part number, and election
                Optional<VoterEntity> voterOpt = voterRepository
                        .findByElectionIdAndPartNoAndSerialNoAndAccountId(
                                electionId, Integer.parseInt(partNo), photo.getSerialNo(), accountId);

                if (voterOpt.isPresent()) {
                    VoterEntity voter = voterOpt.get();
                    
                    // Generate photo URL/path
                    String photoUrl = generatePhotoUrl(extractionResponse.getJobId(), photo.getSerialNo());
                    
                    // Update voter photo
                    voter.setPhotoUrl(photoUrl);
                    voter.setModifiedTime(LocalDateTime.now());
                    
                    voterRepository.save(voter);
                    
                    successfulUpdates.add(new VoterPhotoUpdate(
                            voter.getId(),
                            voter.getVoterFnameEn() + " " + voter.getVoterLnameEn(),
                            photo.getSerialNo(),
                            photoUrl,
                            photo.getConfidence(),
                            "SUCCESS",
                            null
                    ));

                    log.debug("Updated photo for voter: {} (Serial: {})", 
                             voter.getVoterFnameEn(), photo.getSerialNo());

                } else {
                    String error = String.format("Voter not found - Election: %d, Part: %s, Serial: %d", 
                                                electionId, partNo, photo.getSerialNo());
                    
                    failedUpdates.add(new VoterPhotoUpdate(
                            null, null, photo.getSerialNo(), null, photo.getConfidence(),
                            "VOTER_NOT_FOUND", error
                    ));

                    log.warn(error);
                }

            } catch (Exception e) {
                String error = "Error updating voter serial " + photo.getSerialNo() + ": " + e.getMessage();
                
                failedUpdates.add(new VoterPhotoUpdate(
                        null, null, photo.getSerialNo(), null, photo.getConfidence(),
                        "UPDATE_ERROR", error
                ));

                log.error(error, e);
            }
        }

        log.info("Photo update completed - Success: {}, Failed: {}", 
                successfulUpdates.size(), failedUpdates.size());

        return new PhotoUpdateResult(successfulUpdates, failedUpdates);
    }

    /**
     * Update voter records with extracted photo information and S3 upload
     */
    private PhotoUpdateResult updateVoterPhotosWithS3Upload(
            PhotoExtractionResponse extractionResponse, 
            String partNo, 
            Long electionId, 
            Long accountId,
            String jobId,
            PhotoProcessingStatus status) {

        log.info("Updating voter photos with S3 upload for {} extracted photos", 
                extractionResponse.getPhotos().size());

        List<VoterPhotoUpdate> successfulUpdates = new ArrayList<>();
        List<VoterPhotoUpdate> failedUpdates = new ArrayList<>();
        
        int processedCount = 0;
        int totalPhotos = extractionResponse.getPhotos().size();

        for (ExtractedPhoto photo : extractionResponse.getPhotos()) {
            try {
                processedCount++;
                double progress = 30.0 + (60.0 * processedCount / totalPhotos); // 30-90%
                status.setProgressPercentage(progress);
                status.setProcessedPhotos(processedCount);
                
                // Find voter by serial number, part number, and election
                Optional<VoterEntity> voterOpt = voterRepository
                        .findByElectionIdAndPartNoAndSerialNoAndAccountId(
                                electionId, Integer.parseInt(partNo), photo.getSerialNo(), accountId);

                if (voterOpt.isPresent()) {
                    VoterEntity voter = voterOpt.get();
                    
                    // Get photo file from OCR service
                    String localPhotoPath = ocrIntegrationService.getLocalPhotoPath(jobId, photo.getSerialNo());
                    File photoFile = new File(localPhotoPath);
                    
                    if (photoFile.exists()) {
                        // Generate S3 key for the photo
                        String s3Key = generateS3PhotoKey(electionId, accountId, partNo, photo.getSerialNo());
                        
                        // Upload to S3
                        String s3Url = awsFileUpload.uploadToAWS(photoFile, s3Key, s3ImageBucket);
                        
                        if (s3Url != null && !s3Url.isEmpty()) {
                            // Update voter with S3 URL
                            voter.setPhotoUrl(s3Url);
                            voter.setModifiedTime(LocalDateTime.now());
                            
                            voterRepository.save(voter);
                            
                            successfulUpdates.add(new VoterPhotoUpdate(
                                    voter.getId(),
                                    voter.getVoterFnameEn() + " " + voter.getVoterLnameEn(),
                                    photo.getSerialNo(),
                                    s3Url,
                                    photo.getConfidence(),
                                    "SUCCESS",
                                    null
                            ));

                            log.debug("Updated photo for voter: {} (Serial: {}) with S3 URL: {}", 
                                     voter.getVoterFnameEn(), photo.getSerialNo(), s3Url);
                                     
                            // Clean up local file after successful upload
                            photoFile.delete();
                        } else {
                            throw new RuntimeException("Failed to upload photo to S3");
                        }
                    } else {
                        throw new RuntimeException("Photo file not found: " + localPhotoPath);
                    }

                } else {
                    String error = String.format("Voter not found - Election: %d, Part: %s, Serial: %d", 
                                                electionId, partNo, photo.getSerialNo());
                    
                    failedUpdates.add(new VoterPhotoUpdate(
                            null, null, photo.getSerialNo(), null, photo.getConfidence(),
                            "VOTER_NOT_FOUND", error
                    ));

                    log.warn(error);
                }

            } catch (Exception e) {
                String error = String.format("Failed to process photo for serial %d: %s", 
                                            photo.getSerialNo(), e.getMessage());
                
                failedUpdates.add(new VoterPhotoUpdate(
                        null, null, photo.getSerialNo(), null, photo.getConfidence(),
                        "PROCESSING_ERROR", error
                ));
                
                log.error("Error processing photo for serial {}: {}", photo.getSerialNo(), e.getMessage());
            }
        }

        log.info("Photo update completed - Success: {}, Failed: {}", 
                successfulUpdates.size(), failedUpdates.size());

        return new PhotoUpdateResult(successfulUpdates, failedUpdates);
    }
    
    /**
     * Generate S3 key for voter photo
     */
    private String generateS3PhotoKey(Long electionId, Long accountId, String partNo, Long serialNo) {
        return String.format("elections/%d/accounts/%d/parts/%s/voters/voter_%03d.jpg", 
                electionId, accountId, partNo, serialNo);
    }
    
    /**
     * Get job status
     */
    public PhotoProcessingStatus getJobStatus(String jobId) {
        return jobStatusMap.get(jobId);
    }

    /**
     * Generate photo URL for voter
     */
    private String generatePhotoUrl(String jobId, Long serialNo) {
        // This could be a file path, URL, or identifier depending on your storage strategy
        return String.format("/api/voter-photos/%s/%d", jobId, serialNo);
    }

    /**
     * Get voter photo as byte array
     */
    public byte[] getVoterPhoto(String jobId, Long serialNo) throws IOException {
        return ocrIntegrationService.getPhotoBytes(jobId, serialNo);
    }

    /**
     * Check OCR service health
     */
    public boolean isOcrServiceHealthy() {
        return ocrIntegrationService.isOcrServiceHealthy();
    }

    /**
     * Cleanup old photo processing jobs
     */
    public void cleanupOldJobs() {
        ocrIntegrationService.cleanupOldJobs(24); // Keep for 24 hours
    }
}

// Supporting classes
class PhotoProcessingResult {
    private final String jobId;
    private final boolean success;
    private final String error;
    private final String message;
    private final PhotoExtractionResponse extractionResponse;
    private final PhotoUpdateResult updateResult;
    private final Integer totalPhotosExtracted;
    private final Integer successfulUpdates;
    private final Integer failedUpdates;

    private PhotoProcessingResult(String jobId, boolean success, String error, String message,
                                 PhotoExtractionResponse extractionResponse, PhotoUpdateResult updateResult,
                                 Integer totalPhotosExtracted, Integer successfulUpdates, Integer failedUpdates) {
        this.jobId = jobId;
        this.success = success;
        this.error = error;
        this.message = message;
        this.extractionResponse = extractionResponse;
        this.updateResult = updateResult;
        this.totalPhotosExtracted = totalPhotosExtracted;
        this.successfulUpdates = successfulUpdates;
        this.failedUpdates = failedUpdates;
    }

    public static PhotoProcessingResult success(String jobId, PhotoExtractionResponse extractionResponse, 
                                              PhotoUpdateResult updateResult) {
        return new PhotoProcessingResult(jobId, true, null, "Processing completed", 
                extractionResponse, updateResult, null, null, null);
    }

    public static PhotoProcessingResult failure(String jobId, String error) {
        return new PhotoProcessingResult(jobId, false, error, null, null, null, null, null, null);
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String jobId;
        private boolean success;
        private String error;
        private String message;
        private PhotoExtractionResponse extractionResponse;
        private PhotoUpdateResult updateResult;
        private Integer totalPhotosExtracted;
        private Integer successfulUpdates;
        private Integer failedUpdates;
        
        public Builder jobId(String jobId) { this.jobId = jobId; return this; }
        public Builder success(boolean success) { this.success = success; return this; }
        public Builder error(String error) { this.error = error; return this; }
        public Builder message(String message) { this.message = message; return this; }
        public Builder extractionResponse(PhotoExtractionResponse extractionResponse) { 
            this.extractionResponse = extractionResponse; return this; 
        }
        public Builder updateResult(PhotoUpdateResult updateResult) { 
            this.updateResult = updateResult; return this; 
        }
        public Builder totalPhotosExtracted(Integer totalPhotosExtracted) { 
            this.totalPhotosExtracted = totalPhotosExtracted; return this; 
        }
        public Builder successfulUpdates(Integer successfulUpdates) { 
            this.successfulUpdates = successfulUpdates; return this; 
        }
        public Builder failedUpdates(Integer failedUpdates) { 
            this.failedUpdates = failedUpdates; return this; 
        }
        
        public PhotoProcessingResult build() {
            return new PhotoProcessingResult(jobId, success, error, message, extractionResponse, 
                    updateResult, totalPhotosExtracted, successfulUpdates, failedUpdates);
        }
    }

    // Getters
    public String getJobId() { return jobId; }
    public boolean isSuccess() { return success; }
    public String getError() { return error; }
    public String getMessage() { return message; }
    public PhotoExtractionResponse getExtractionResponse() { return extractionResponse; }
    public PhotoUpdateResult getUpdateResult() { return updateResult; }
    public Integer getTotalPhotosExtracted() { return totalPhotosExtracted; }
    public Integer getSuccessfulUpdates() { return successfulUpdates; }
    public Integer getFailedUpdates() { return failedUpdates; }
}

class PhotoUpdateResult {
    private final List<VoterPhotoUpdate> successfulUpdates;
    private final List<VoterPhotoUpdate> failedUpdates;

    public PhotoUpdateResult(List<VoterPhotoUpdate> successfulUpdates, List<VoterPhotoUpdate> failedUpdates) {
        this.successfulUpdates = successfulUpdates;
        this.failedUpdates = failedUpdates;
    }

    public List<VoterPhotoUpdate> getSuccessfulUpdates() { return successfulUpdates; }
    public List<VoterPhotoUpdate> getFailedUpdates() { return failedUpdates; }
    
    public int getTotalSuccessful() { return successfulUpdates.size(); }
    public int getTotalFailed() { return failedUpdates.size(); }
    public int getTotalProcessed() { return successfulUpdates.size() + failedUpdates.size(); }
}

class VoterPhotoUpdate {
    private final Long voterId;
    private final String voterName;
    private final Long serialNo;
    private final String photoUrl;
    private final Double confidence;
    private final String status;
    private final String error;

    public VoterPhotoUpdate(Long voterId, String voterName, Long serialNo, String photoUrl, 
                           Double confidence, String status, String error) {
        this.voterId = voterId;
        this.voterName = voterName;
        this.serialNo = serialNo;
        this.photoUrl = photoUrl;
        this.confidence = confidence;
        this.status = status;
        this.error = error;
    }

    // Getters
    public Long getVoterId() { return voterId; }
    public String getVoterName() { return voterName; }
    public Long getSerialNo() { return serialNo; }
    public String getPhotoUrl() { return photoUrl; }
    public Double getConfidence() { return confidence; }
    public String getStatus() { return status; }
    public String getError() { return error; }
}
