package com.thedal.thedal_app.voter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.thedal.thedal_app.awsfilestore.AwsFileUpload;

import lombok.extern.slf4j.Slf4j;

/**
 * Service to upload photos directly to S3 from ZIP file
 * Preserves original filenames for later URL matching
 */
@Slf4j
@Service
public class VoterPhotoDirectS3Service {
    
    @Value("${aws.s3.image.bucket}")
    private String s3bucket;
    
    @Autowired
    private AwsFileUpload awsFileUpload;
    
    private static final long MAX_PHOTO_SIZE = 5 * 1024 * 1024; // 5MB per photo
    private static final java.util.Set<String> ALLOWED_EXTENSIONS = 
        java.util.Set.of(".jpg", ".jpeg", ".png", ".JPG", ".JPEG", ".PNG");
    
    /**
     * Upload all photos from ZIP directly to S3 with original filenames
     * 
     * @param zipFile The ZIP file containing photos
     * @param electionId The election ID (for folder organization)
     * @return Upload result with success/failure counts
     */
    public DirectUploadResult uploadPhotosDirectlyToS3(MultipartFile zipFile, Long electionId) 
            throws IOException {
        
        log.info("Starting direct S3 upload from ZIP: {} for election {}", 
                zipFile.getOriginalFilename(), electionId);
        
        DirectUploadResult result = new DirectUploadResult();
        result.setElectionId(electionId);
        result.setZipFilename(zipFile.getOriginalFilename());
        
        List<String> uploadedFiles = new ArrayList<>();
        List<String> failedFiles = new ArrayList<>();
        
        try (InputStream is = zipFile.getInputStream();
             ZipInputStream zis = new ZipInputStream(is)) {
            
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                
                if (entry.isDirectory()) {
                    continue;
                }
                
                // Get filename without path
                String filename = entry.getName();
                if (filename.contains("/")) {
                    filename = filename.substring(filename.lastIndexOf("/") + 1);
                }
                if (filename.contains("\\")) {
                    filename = filename.substring(filename.lastIndexOf("\\") + 1);
                }
                
                // Skip hidden files and non-image files
                if (filename.startsWith(".") || filename.startsWith("__MACOSX")) {
                    log.debug("Skipping system file: {}", filename);
                    continue;
                }
                
                // Check if it's an image file
                String extension = getFileExtension(filename);
                if (!ALLOWED_EXTENSIONS.contains(extension)) {
                    log.debug("Skipping non-image file: {}", filename);
                    result.incrementSkipped();
                    continue;
                }
                
                // Check file size
                if (entry.getSize() > MAX_PHOTO_SIZE) {
                    log.warn("File {} exceeds max size: {} bytes", filename, entry.getSize());
                    failedFiles.add(filename + " (too large)");
                    result.incrementFailed();
                    continue;
                }
                
                try {
                    // Read file content
                    byte[] content = zis.readAllBytes();
                    
                    if (content.length == 0) {
                        log.warn("Empty file: {}", filename);
                        failedFiles.add(filename + " (empty)");
                        result.incrementFailed();
                        continue;
                    }
                    
                    // Upload directly to S3 with ORIGINAL filename
                    String contentType = extension.toLowerCase().endsWith(".png") ? 
                        MediaType.IMAGE_PNG_VALUE : MediaType.IMAGE_JPEG_VALUE;
                    
                    String s3Url = awsFileUpload.uploadBytesToAWS(
                        content,
                        filename,  // IMPORTANT: Keep original filename!
                        s3bucket,
                        contentType
                    );
                    
                    uploadedFiles.add(filename);
                    result.incrementSuccess();
                    
                    log.debug("Uploaded {} to S3: {}", filename, s3Url);
                    
                } catch (Exception e) {
                    log.error("Failed to upload {}: {}", filename, e.getMessage());
                    failedFiles.add(filename + " (" + e.getMessage() + ")");
                    result.incrementFailed();
                }
                
                zis.closeEntry();
            }
        }
        
        result.setUploadedFiles(uploadedFiles);
        result.setFailedFiles(failedFiles);
        result.setMessage(String.format(
            "Upload complete: %d successful, %d failed, %d skipped",
            result.getSuccessCount(), result.getFailedCount(), result.getSkippedCount()
        ));
        
        log.info("Direct S3 upload completed: {}", result.getMessage());
        
        return result;
    }
    
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
    
    /**
     * Result object for direct S3 upload
     */
    public static class DirectUploadResult {
        private Long electionId;
        private String zipFilename;
        private int successCount;
        private int failedCount;
        private int skippedCount;
        private String message;
        private List<String> uploadedFiles;
        private List<String> failedFiles;
        
        public void incrementSuccess() { this.successCount++; }
        public void incrementFailed() { this.failedCount++; }
        public void incrementSkipped() { this.skippedCount++; }
        
        // Getters and setters
        public Long getElectionId() { return electionId; }
        public void setElectionId(Long electionId) { this.electionId = electionId; }
        
        public String getZipFilename() { return zipFilename; }
        public void setZipFilename(String zipFilename) { this.zipFilename = zipFilename; }
        
        public int getSuccessCount() { return successCount; }
        public void setSuccessCount(int successCount) { this.successCount = successCount; }
        
        public int getFailedCount() { return failedCount; }
        public void setFailedCount(int failedCount) { this.failedCount = failedCount; }
        
        public int getSkippedCount() { return skippedCount; }
        public void setSkippedCount(int skippedCount) { this.skippedCount = skippedCount; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public List<String> getUploadedFiles() { return uploadedFiles; }
        public void setUploadedFiles(List<String> uploadedFiles) { this.uploadedFiles = uploadedFiles; }
        
        public List<String> getFailedFiles() { return failedFiles; }
        public void setFailedFiles(List<String> failedFiles) { this.failedFiles = failedFiles; }
    }
}
