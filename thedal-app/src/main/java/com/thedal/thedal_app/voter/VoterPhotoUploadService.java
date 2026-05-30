package com.thedal.thedal_app.voter;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thedal.thedal_app.awsfilestore.AwsFileUpload;
import com.thedal.thedal_app.util.RandomTokenGenerator;
import com.thedal.thedal_app.voter.dto.BulkPhotoUploadResponse;
import com.thedal.thedal_app.voter.dto.BulkPhotoUploadResponse.PhotoUploadError;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class VoterPhotoUploadService {
    
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
        MediaType.IMAGE_JPEG_VALUE, 
        MediaType.IMAGE_PNG_VALUE
    );
    
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    
    @Value("${aws.s3.image.bucket}")
    private String s3bucket;
    
    @Value("${thedal.photo.bulk.batch-size:500}")
    private int batchSize;
    
    @Value("${thedal.photo.bulk.parallel-threads:30}")
    private int parallelThreads;
    
    @Autowired
    private VoterRepo voterRepository;
    
    @Autowired
    private BulkPhotoUploadRepository bulkPhotoUploadRepository;
    
    @Autowired
    private AwsFileUpload awsFileUpload;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    // Thread pool for parallel S3 uploads - initialized lazily based on config
    private java.util.concurrent.ExecutorService uploadExecutor;
    
    private java.util.concurrent.ExecutorService getUploadExecutor() {
        if (uploadExecutor == null) {
            synchronized (this) {
                if (uploadExecutor == null) {
                    uploadExecutor = java.util.concurrent.Executors.newFixedThreadPool(parallelThreads);
                    log.info("Initialized S3 upload thread pool with {} threads", parallelThreads);
                }
            }
        }
        return uploadExecutor;
    }
    
    /**
     * Cleanup thread pool on shutdown
     */
    @jakarta.annotation.PreDestroy
    public void cleanup() {
        if (uploadExecutor != null && !uploadExecutor.isShutdown()) {
            log.info("Shutting down S3 upload thread pool...");
            uploadExecutor.shutdown();
            try {
                if (!uploadExecutor.awaitTermination(30, java.util.concurrent.TimeUnit.SECONDS)) {
                    uploadExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                uploadExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
    
    /**
     * Initiates bulk photo upload from ZIP file
     * NOTE: No @Transactional - each batch commits independently to avoid rollback on crashes
     */
    @Async
    public CompletableFuture<BulkPhotoUploadResponse> processBulkPhotoUpload(
            byte[] zipFileContent, 
            String originalFilename,
            Long electionId, 
            Long accountId,
            String uploadedBy) {
        
        log.info("Starting bulk photo upload for election {} and account {}", electionId, accountId);
        
        // Create tracking entity
        BulkPhotoUploadEntity bulkUpload = new BulkPhotoUploadEntity(
            accountId, electionId, originalFilename);
        bulkUpload = bulkPhotoUploadRepository.save(bulkUpload);
        
        BulkPhotoUploadResponse response = new BulkPhotoUploadResponse(
            bulkUpload.getId(), "Processing bulk photo upload...");
        
        List<PhotoUploadError> errors = new ArrayList<>();
        
        try {
            // Extract and process ZIP file
            List<PhotoFile> photoFiles = extractPhotosFromZip(zipFileContent);
            bulkUpload.setTotalPhotos(photoFiles.size());
            response.setTotalPhotos(photoFiles.size());
            
            log.info("Extracted {} photos from ZIP file", photoFiles.size());
            
            if (photoFiles.isEmpty()) {
                throw new RuntimeException("No valid photo files found in ZIP");
            }
            
            // Process photos in batches
            int successCount = 0;
            int failureCount = 0;
            int notFoundCount = 0;
            
            for (int i = 0; i < photoFiles.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, photoFiles.size());
                List<PhotoFile> batch = photoFiles.subList(i, endIndex);
                
                log.info("Processing batch {}-{} of {}", i + 1, endIndex, photoFiles.size());
                
                BatchResult batchResult = processBatch(batch, electionId, accountId);
                
                successCount += batchResult.successCount;
                failureCount += batchResult.failureCount;
                notFoundCount += batchResult.notFoundCount;
                errors.addAll(batchResult.errors);
                
                int processedCount = successCount + failureCount;
                
                // Update progress
                bulkUpload.setSuccessfulUploads(successCount);
                bulkUpload.setFailedUploads(failureCount);
                bulkUpload.setProcessedPhotos(processedCount);
                bulkPhotoUploadRepository.save(bulkUpload);
                
                log.info("Batch completed. Current totals - Success: {}, Failed: {}, Processed: {}", 
                    successCount, failureCount, processedCount);
                
                // SAFETY: Add 1 second delay between batches to prevent system overload
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.warn("Batch delay interrupted");
                }
            }
            
            int processedCount = successCount + failureCount;
            
            // Final update
            bulkUpload.setStatus(BulkUploadStatus.COMPLETED);
            bulkUpload.setEndTime(LocalDateTime.now());
            bulkUpload.setErrorDetails(convertErrorsToJson(errors));
            bulkPhotoUploadRepository.save(bulkUpload);
            
            // Prepare response
            response.setStatus(BulkUploadStatus.COMPLETED);
            response.setEndTime(LocalDateTime.now());
            response.setSuccessfulUploads(successCount);
            response.setFailedUploads(failureCount);
            response.setProcessedPhotos(processedCount);
            response.setErrors(errors);
            response.setMessage(String.format(
                "Bulk upload completed. Success: %d, Failed: %d, Processed: %d", 
                successCount, failureCount, processedCount));
            
            log.info("Bulk photo upload completed for upload ID {}", bulkUpload.getId());
            
        } catch (Exception e) {
            log.error("Error during bulk photo upload: {}", e.getMessage(), e);
            
            // Update status to failed
            bulkUpload.setStatus(BulkUploadStatus.FAILED);
            bulkUpload.setEndTime(LocalDateTime.now());
            bulkUpload.setErrorDetails("Upload failed: " + e.getMessage());
            bulkPhotoUploadRepository.save(bulkUpload);
            
            response.setStatus(BulkUploadStatus.FAILED);
            response.setMessage("Upload failed: " + e.getMessage());
        }
        
        return CompletableFuture.completedFuture(response);
    }

    /**
     * Initiates bulk photo upload from a ZIP file on disk (streaming, low-memory).
     * NOTE: No @Transactional - each batch commits independently to avoid rollback on crashes
     */
    @Async
    public CompletableFuture<BulkPhotoUploadResponse> processBulkPhotoUploadFromFile(
            File zipFileOnDisk,
            String originalFilename,
            Long electionId,
            Long accountId,
            String uploadedBy) {

        log.info("Starting streaming bulk photo upload for election {} and account {} from file {}",
            electionId, accountId, zipFileOnDisk.getAbsolutePath());

        BulkPhotoUploadEntity bulkUpload = new BulkPhotoUploadEntity(
            accountId, electionId, originalFilename);
        bulkUpload = bulkPhotoUploadRepository.save(bulkUpload);

        BulkPhotoUploadResponse response = new BulkPhotoUploadResponse(
            bulkUpload.getId(), "Processing bulk photo upload...");

        List<PhotoUploadError> errors = new ArrayList<>();

        try (InputStream fis = new java.io.BufferedInputStream(new java.io.FileInputStream(zipFileOnDisk));
             ZipInputStream zis = new ZipInputStream(fis)) {

            List<PhotoFile> batch = new ArrayList<>(batchSize);
            int successCount = 0;
            int failureCount = 0;
            int notFoundCount = 0;
            int totalPhotos = 0;

            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;

                String filename = entry.getName();
                if (filename.contains("/") || filename.contains("\\")) {
                    filename = new File(filename).getName();
                }
                if (!isImageFile(filename)) {
                    continue;
                }

                String epicNumber = extractEpicNumberFromFilename(filename);
                if (epicNumber == null) {
                    errors.add(new PhotoUploadError(filename, null, "Invalid filename (EPIC missing)", "INVALID_FILENAME"));
                    continue;
                }

                // Stream the entry into a temporary file to avoid holding large arrays in memory
                String ext = getFileExtension(filename);
                File tempImage = File.createTempFile("voter_photo_stream_", ext);
                long written = copyToFile(zis, tempImage);
                if (written > MAX_FILE_SIZE) {
                    // Too large; record error and delete temp
                    if (!tempImage.delete()) log.warn("Failed to delete temp {}", tempImage);
                    errors.add(new PhotoUploadError(filename, epicNumber, "Image exceeds max size", "IMAGE_TOO_LARGE"));
                    continue;
                }

                // Convert temp file to PhotoFile by reading bytes (small images only)
                byte[] content = java.nio.file.Files.readAllBytes(tempImage.toPath());
                if (!tempImage.delete()) log.warn("Failed to delete temp {}", tempImage);

                batch.add(new PhotoFile(filename, epicNumber, content));
                totalPhotos++;

                if (batch.size() >= batchSize) {
                    BatchResult br = processBatch(batch, electionId, accountId);
                    successCount += br.successCount;
                    failureCount += br.failureCount;
                    notFoundCount += br.notFoundCount;
                    errors.addAll(br.errors);
                    batch.clear();

                    int processedCount = successCount + failureCount;
                    bulkUpload.setSuccessfulUploads(successCount);
                    bulkUpload.setFailedUploads(failureCount);
                    bulkUpload.setProcessedPhotos(processedCount);
                    bulkUpload.setTotalPhotos(totalPhotos);
                    bulkPhotoUploadRepository.save(bulkUpload);
                    
                    // SAFETY: Add 1 second delay between batches
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }

            // Process remaining
            if (!batch.isEmpty()) {
                BatchResult br = processBatch(batch, electionId, accountId);
                successCount += br.successCount;
                failureCount += br.failureCount;
                notFoundCount += br.notFoundCount;
                errors.addAll(br.errors);
            }

            int processedCount = successCount + failureCount;
            bulkUpload.setStatus(BulkUploadStatus.COMPLETED);
            bulkUpload.setEndTime(LocalDateTime.now());
            bulkUpload.setErrorDetails(convertErrorsToJson(errors));
            bulkUpload.setTotalPhotos(totalPhotos);
            bulkUpload.setProcessedPhotos(processedCount);
            bulkUpload.setSuccessfulUploads(successCount);
            bulkUpload.setFailedUploads(failureCount);
            bulkPhotoUploadRepository.save(bulkUpload);

            response.setStatus(BulkUploadStatus.COMPLETED);
            response.setEndTime(LocalDateTime.now());
            response.setSuccessfulUploads(successCount);
            response.setFailedUploads(failureCount);
            response.setProcessedPhotos(processedCount);
            response.setTotalPhotos(totalPhotos);
            response.setErrors(errors);
            response.setMessage(String.format(
                "Bulk upload completed. Success: %d, Failed: %d, Processed: %d",
                successCount, failureCount, processedCount));

        } catch (Exception e) {
            log.error("Streaming bulk photo upload failed: {}", e.getMessage(), e);
            bulkUpload.setStatus(BulkUploadStatus.FAILED);
            bulkUpload.setEndTime(LocalDateTime.now());
            bulkUpload.setErrorDetails("Upload failed: " + e.getMessage());
            bulkPhotoUploadRepository.save(bulkUpload);

            response.setStatus(BulkUploadStatus.FAILED);
            response.setMessage("Upload failed: " + e.getMessage());
        }

        return CompletableFuture.completedFuture(response);
    }

    private long copyToFile(InputStream in, File target) throws IOException {
        long total = 0;
        try (FileOutputStream out = new FileOutputStream(target)) {
            byte[] buf = new byte[8192];
            int r;
            while ((r = in.read(buf)) != -1) {
                out.write(buf, 0, r);
                total += r;
                if (total > MAX_FILE_SIZE) {
                    // We can stop early; caller handles
                    break;
                }
            }
        }
        return total;
    }
    
    /**
     * Extract photos from ZIP file
     */
    private List<PhotoFile> extractPhotosFromZip(byte[] zipFileContent) throws IOException {
        List<PhotoFile> photoFiles = new ArrayList<>();
        
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipFileContent))) {
            ZipEntry entry;
            
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                
                String filename = entry.getName();
                
                // Skip files in subdirectories or with paths
                if (filename.contains("/") || filename.contains("\\")) {
                    filename = new File(filename).getName();
                }
                
                // Check if it's an image file
                if (!isImageFile(filename)) {
                    log.debug("Skipping non-image file: {}", filename);
                    continue;
                }
                
                // Extract EPIC number from filename
                String epicNumber = extractEpicNumberFromFilename(filename);
                if (epicNumber == null) {
                    log.warn("Could not extract EPIC number from filename: {}", filename);
                    continue;
                }
                
                // Read file content
                byte[] content = zis.readAllBytes();
                
                // Validate file size
                if (content.length > MAX_FILE_SIZE) {
                    log.warn("File {} exceeds maximum size limit of {}MB", filename, MAX_FILE_SIZE / (1024 * 1024));
                    continue;
                }
                
                photoFiles.add(new PhotoFile(filename, epicNumber, content));
                log.debug("Extracted photo: {} for EPIC: {}", filename, epicNumber);
            }
        }
        
        return photoFiles;
    }
    
    /**
     * Process a batch of photos with parallel S3 uploads and batch DB updates
     * SAFETY: Limited parallelism to prevent system overload
     * Each batch commits independently so progress is saved even if server crashes
     */
    @Transactional
    private BatchResult processBatch(List<PhotoFile> batch, Long electionId, Long accountId) {
        BatchResult result = new BatchResult();
        
        // SAFETY CHECK: Limit batch size to prevent memory overflow
        if (batch.size() > 1000) {
            log.warn("Batch size {} exceeds safety limit of 1000, processing first 1000 only", batch.size());
            batch = batch.subList(0, 1000);
        }
        
        // Get EPIC numbers for this batch
        List<String> epicNumbers = batch.stream()
            .map(PhotoFile::getEpicNumber)
            .collect(Collectors.toList());
        
        // Find existing voters - wrapped in try-catch to prevent DB crashes
        Map<String, VoterEntity> voterMap;
        try {
            voterMap = voterRepository
                .findByAccountIdAndElectionIdAndEpicNumberIn(accountId, electionId, epicNumbers)
                .stream()
                .collect(Collectors.toMap(VoterEntity::getEpicNumber, voter -> voter));
        } catch (Exception e) {
            log.error("Database query failed for batch: {}", e.getMessage(), e);
            result.failureCount = batch.size();
            batch.forEach(pf -> result.errors.add(new PhotoUploadError(
                pf.getFilename(), pf.getEpicNumber(), 
                "Database error: " + e.getMessage(), "DB_ERROR")));
            return result;
        }
        
        // Separate photos into found and not found
        List<PhotoFile> validPhotos = new ArrayList<>();
        for (PhotoFile photoFile : batch) {
            VoterEntity voter = voterMap.get(photoFile.getEpicNumber());
            if (voter == null) {
                result.notFoundCount++;
                result.errors.add(new PhotoUploadError(
                    photoFile.getFilename(), 
                    photoFile.getEpicNumber(),
                    "Voter not found with this EPIC number",
                    "VOTER_NOT_FOUND"
                ));
            } else {
                validPhotos.add(photoFile);
            }
        }
        
        if (validPhotos.isEmpty()) {
            return result;
        }
        
        // Upload photos to S3 in parallel with timeout protection
        List<CompletableFuture<PhotoUploadResult>> uploadFutures = validPhotos.stream()
            .map(photoFile -> CompletableFuture.supplyAsync(() -> {
                try {
                    String photoUrl = uploadPhotoToS3(photoFile);
                    return new PhotoUploadResult(photoFile.getEpicNumber(), photoFile.getFilename(), photoUrl, null);
                } catch (Exception e) {
                    log.error("S3 upload failed for {}: {}", photoFile.getEpicNumber(), e.getMessage());
                    return new PhotoUploadResult(photoFile.getEpicNumber(), photoFile.getFilename(), null, e.getMessage());
                }
            }, getUploadExecutor()))
            .collect(Collectors.toList());
        
        // Wait for all uploads to complete with timeout protection
        CompletableFuture<Void> allUploads = CompletableFuture.allOf(
            uploadFutures.toArray(new CompletableFuture[0]));
        
        try {
            // SAFETY: 5 minute timeout per batch to prevent hanging
            allUploads.get(5, java.util.concurrent.TimeUnit.MINUTES);
            
            // Collect results and prepare batch update
            List<VoterEntity> votersToUpdate = new ArrayList<>();
            
            for (CompletableFuture<PhotoUploadResult> future : uploadFutures) {
                PhotoUploadResult uploadResult = future.get();
                
                if (uploadResult.error != null) {
                    result.failureCount++;
                    result.errors.add(new PhotoUploadError(
                        uploadResult.filename,
                        uploadResult.epicNumber,
                        "Upload failed: " + uploadResult.error,
                        "UPLOAD_FAILED"
                    ));
                } else {
                    VoterEntity voter = voterMap.get(uploadResult.epicNumber);
                    voter.setPhotoUrl(uploadResult.photoUrl);
                    votersToUpdate.add(voter);
                    result.successCount++;
                }
            }
            
            // Batch update voters in database with transaction safety
            if (!votersToUpdate.isEmpty()) {
                try {
                    // SAFETY: Use separate transaction to prevent connection exhaustion
                    voterRepository.saveAll(votersToUpdate);
                    log.info("Batch updated {} voters with photo URLs", votersToUpdate.size());
                } catch (Exception dbError) {
                    log.error("Database batch update failed: {}", dbError.getMessage(), dbError);
                    // Mark all as failed if DB update fails
                    result.failureCount += votersToUpdate.size();
                    result.successCount -= votersToUpdate.size();
                    votersToUpdate.forEach(v -> result.errors.add(new PhotoUploadError(
                        v.getEpicNumber(), v.getEpicNumber(),
                        "Database update failed: " + dbError.getMessage(), "DB_UPDATE_FAILED")));
                }
            }
            
        } catch (java.util.concurrent.TimeoutException e) {
            log.error("Batch processing timeout after 5 minutes: {}", e.getMessage());
            result.failureCount += validPhotos.size() - result.successCount;
            result.errors.add(new PhotoUploadError("batch", "batch", 
                "Upload timeout - system may be overloaded", "TIMEOUT"));
        } catch (Exception e) {
            log.error("Error processing photo batch: {}", e.getMessage(), e);
            result.failureCount += validPhotos.size() - result.successCount;
        }
        
        return result;
    }
    
    /**
     * Upload photo to S3 directly from byte array (no temp file)
     */
    private String uploadPhotoToS3(PhotoFile photoFile) throws IOException {
        String fileExtension = getFileExtension(photoFile.getFilename());
        String fileName = "voter_photo_" + photoFile.getEpicNumber() + "_" + 
            System.currentTimeMillis() + "_" + RandomTokenGenerator.generateToken(6) + fileExtension;
        
        // Determine content type
        String contentType = fileExtension.equalsIgnoreCase(".png") ? 
            MediaType.IMAGE_PNG_VALUE : MediaType.IMAGE_JPEG_VALUE;
        
        // Upload directly from byte array (no temp file)
        String s3Url = awsFileUpload.uploadBytesToAWS(
            photoFile.getContent(), 
            fileName, 
            s3bucket,
            contentType
        );
        
        log.debug("Uploaded {} to S3: {}", fileName, s3Url);
        return s3Url;
    }
    
    /**
     * Extract EPIC number from filename
     */
    private String extractEpicNumberFromFilename(String filename) {
        // Remove file extension
        String nameWithoutExt = filename.substring(0, filename.lastIndexOf('.'));
        
        // EPIC numbers are typically alphanumeric, 8-12 characters
        // Adjust this regex based on your EPIC number format
        if (nameWithoutExt.matches("[A-Za-z0-9]{6,15}")) {
            return nameWithoutExt.toUpperCase();
        }
        
        return null;
    }
    
    /**
     * Check if file is an image
     */
    private boolean isImageFile(String filename) {
        String ext = getFileExtension(filename).toLowerCase();
        return ext.equals(".jpg") || ext.equals(".jpeg") || ext.equals(".png");
    }
    
    /**
     * Get file extension
     */
    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot) : "";
    }
    
    /**
     * Convert errors to JSON string
     */
    private String convertErrorsToJson(List<PhotoUploadError> errors) {
        try {
            return objectMapper.writeValueAsString(errors);
        } catch (JsonProcessingException e) {
            log.error("Failed to convert errors to JSON", e);
            return "[]";
        }
    }
    
    /**
     * Get bulk upload status
     */
    public Optional<BulkPhotoUploadEntity> getBulkUploadStatus(Long bulkUploadId) {
        return bulkPhotoUploadRepository.findById(bulkUploadId);
    }
    
    // Helper classes
    private static class PhotoFile {
        private final String filename;
        private final String epicNumber;
        private final byte[] content;
        
        public PhotoFile(String filename, String epicNumber, byte[] content) {
            this.filename = filename;
            this.epicNumber = epicNumber;
            this.content = content;
        }
        
        public String getFilename() { return filename; }
        public String getEpicNumber() { return epicNumber; }
        public byte[] getContent() { return content; }
    }
    
    private static class BatchResult {
        int successCount = 0;
        int failureCount = 0;
        int notFoundCount = 0;
        List<PhotoUploadError> errors = new ArrayList<>();
    }
    
    private static class PhotoUploadResult {
        String epicNumber;
        String filename;
        String photoUrl;
        String error;
        
        public PhotoUploadResult(String epicNumber, String filename, String photoUrl, String error) {
            this.epicNumber = epicNumber;
            this.filename = filename;
            this.photoUrl = photoUrl;
            this.error = error;
        }
    }
}
