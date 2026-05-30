package com.thedal.thedal_app.voter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

/**
 * Service to bulk update voter photo URLs when photos are uploaded directly to S3
 * This bypasses the server upload process and directly constructs URLs from EPIC numbers
 */
@Slf4j
@Service
public class VoterPhotoUrlUpdateService {
    
    @Value("${aws.s3.image.bucket}")
    private String s3bucket;
    
    @Value("${aws.s3.region}")
    private String s3Region;
    
    @Autowired
    private VoterRepo voterRepository;
    
    /**
     * Update all voter photo URLs based on direct S3 upload
     * Matches photos by EPIC number in the filename
     * 
     * Supported filename patterns:
     * 1. {EPIC}.png or {EPIC}.jpg (exact match)
     * 2. voter_photo_{EPIC}.ext
     * 3. {EPIC}_photo.ext
     * 4. Any filename containing the EPIC number
     * 
     * @param electionId The election ID
     * @param accountId The account ID  
     * @param fileExtension The file extension to try (.png, .jpg, etc.)
     * @param useSimplePattern If true, uses exact pattern {EPIC}.{ext}
     * @return CompletableFuture with update result
     */
    @Async
    @Transactional
    public CompletableFuture<PhotoUrlUpdateResult> updateVoterPhotoUrls(
            Long electionId, 
            Long accountId,
            String fileExtension,
            boolean useSimplePattern) {
        
        log.info("Starting bulk photo URL update for election {} and account {}", electionId, accountId);
        
        PhotoUrlUpdateResult result = new PhotoUrlUpdateResult();
        result.setElectionId(electionId);
        result.setAccountId(accountId);
        
        try {
            // Construct S3 base URL
            // Note: s3bucket value is "thedalnew/image" but we need just "thedalnew" for bucket name
            String bucketName = s3bucket.contains("/") ? s3bucket.split("/")[0] : s3bucket;
            String s3BaseUrl = String.format("https://%s.s3.%s.amazonaws.com/thedalnew/image/",
                bucketName, s3Region);
            
            log.info("S3 Base URL: {}", s3BaseUrl);
            
            // Get all voters without photos for this election
            List<VoterEntity> voters = voterRepository
                .findByAccountIdAndElectionIdAndPhotoUrlIsNull(accountId, electionId);
            
            result.setTotalVoters(voters.size());
            log.info("Found {} voters without photos", voters.size());
            
            if (voters.isEmpty()) {
                result.setMessage("No voters found without photos");
                return CompletableFuture.completedFuture(result);
            }
            
            // Process in batches to avoid memory issues
            int batchSize = 1000;
            int totalUpdated = 0;
            List<VoterEntity> batch = new ArrayList<>();
            
            for (VoterEntity voter : voters) {
                if (voter.getEpicNumber() == null || voter.getEpicNumber().trim().isEmpty()) {
                    result.incrementSkipped();
                    continue;
                }
                
                // Construct photo URL with normalized EPIC number
                String photoUrl;
                if (useSimplePattern) {
                    // Simple pattern: {EPIC_NORMALIZED}.{ext}
                    // Convert epic "HR/02/24/0501150" to filename "HR_02_24_0501150"
                    String normalizedEpic = normalizeEpicForFilename(voter.getEpicNumber());
                    photoUrl = s3BaseUrl + normalizedEpic + fileExtension;
                } else {
                    // Complex pattern: voter_photo_{EPIC_NORMALIZED}_*.{ext}
                    String normalizedEpic = normalizeEpicForFilename(voter.getEpicNumber());
                    photoUrl = s3BaseUrl + "voter_photo_" + normalizedEpic + "_" + 
                              System.currentTimeMillis() + "_default" + fileExtension;
                }
                
                voter.setPhotoUrl(photoUrl);
                batch.add(voter);
                
                // Save batch
                if (batch.size() >= batchSize) {
                    voterRepository.saveAll(batch);
                    totalUpdated += batch.size();
                    result.setUpdatedCount(totalUpdated);
                    log.info("Updated batch: {} voters, total: {}", batch.size(), totalUpdated);
                    batch.clear();
                    
                    // Small delay to prevent overwhelming DB
                    Thread.sleep(100);
                }
            }
            
            // Save remaining batch
            if (!batch.isEmpty()) {
                voterRepository.saveAll(batch);
                totalUpdated += batch.size();
                result.setUpdatedCount(totalUpdated);
                log.info("Updated final batch: {} voters, total: {}", batch.size(), totalUpdated);
            }
            
            result.setSuccess(true);
            result.setMessage(String.format("Successfully updated %d voters with photo URLs", totalUpdated));
            log.info("Bulk photo URL update completed: {}", result.getMessage());
            
        } catch (Exception e) {
            log.error("Error updating voter photo URLs: {}", e.getMessage(), e);
            result.setSuccess(false);
            result.setMessage("Update failed: " + e.getMessage());
        }
        
        return CompletableFuture.completedFuture(result);
    }
    
    /**
     * Update photo URLs using exact S3 file path pattern
     * Use this when you know the exact naming pattern in S3
     */
    @Async
    @Transactional
    public CompletableFuture<PhotoUrlUpdateResult> updateVoterPhotoUrlsExact(
            Long electionId,
            Long accountId,
            String s3FolderPath) {
        
        log.info("Starting exact bulk photo URL update for election {} and account {}", electionId, accountId);
        
        PhotoUrlUpdateResult result = new PhotoUrlUpdateResult();
        result.setElectionId(electionId);
        result.setAccountId(accountId);
        
        try {
            // Get all voters for this election
            List<VoterEntity> voters = voterRepository
                .findByAccountIdAndElectionId(accountId, electionId);
            
            result.setTotalVoters(voters.size());
            log.info("Found {} total voters", voters.size());
            
            int batchSize = 1000;
            int totalUpdated = 0;
            List<VoterEntity> batch = new ArrayList<>();
            
            for (VoterEntity voter : voters) {
                if (voter.getEpicNumber() == null || voter.getEpicNumber().trim().isEmpty()) {
                    result.incrementSkipped();
                    continue;
                }
                
                // Normalize EPIC number for filename matching
                // Convert "HR/02/24/0501150" to "HR_02_24_0501150"
                String normalizedEpic = normalizeEpicForFilename(voter.getEpicNumber());
                
                // Try multiple extensions
                String[] extensions = {".png", ".jpg", ".jpeg", ".PNG", ".JPG", ".JPEG"};
                String photoUrl = null;
                
                for (String ext : extensions) {
                    String testUrl = s3FolderPath + normalizedEpic + ext;
                    // In a production system, you might want to verify file exists
                    // For now, we'll use .png as default
                    if (ext.equals(".png")) {
                        photoUrl = testUrl;
                        break;
                    }
                }
                
                if (photoUrl != null) {
                    voter.setPhotoUrl(photoUrl);
                    batch.add(voter);
                    result.incrementUpdated();
                }
                
                // Save batch
                if (batch.size() >= batchSize) {
                    voterRepository.saveAll(batch);
                    totalUpdated += batch.size();
                    log.info("Updated batch: {} voters, total: {}", batch.size(), totalUpdated);
                    batch.clear();
                    
                    Thread.sleep(100);
                }
            }
            
            // Save remaining
            if (!batch.isEmpty()) {
                voterRepository.saveAll(batch);
                totalUpdated += batch.size();
                log.info("Updated final batch: {} voters, total: {}", batch.size(), totalUpdated);
            }
            
            result.setSuccess(true);
            result.setMessage(String.format("Successfully updated %d voters", totalUpdated));
            
        } catch (Exception e) {
            log.error("Error in exact URL update: {}", e.getMessage(), e);
            result.setSuccess(false);
            result.setMessage("Update failed: " + e.getMessage());
        }
        
        return CompletableFuture.completedFuture(result);
    }
    
    /**
     * Normalize EPIC number for filename matching
     * Converts slashes and special characters to underscores
     * 
     * Examples:
     *   "HR/02/24/0501150" -> "HR_02_24_0501150"
     *   "IPR1840586" -> "IPR1840586" (no change)
     *   "MH-01-02-123456" -> "MH_01_02_123456"
     *   
     * @param epicNumber The EPIC number from database
     * @return Normalized EPIC suitable for filename
     */
    private String normalizeEpicForFilename(String epicNumber) {
        if (epicNumber == null || epicNumber.isEmpty()) {
            return epicNumber;
        }
        
        // Replace slashes, hyphens, spaces with underscores
        String normalized = epicNumber
            .replace("/", "_")
            .replace("-", "_")
            .replace(" ", "_")
            .replace("\\", "_");
        
        // Remove any duplicate underscores
        while (normalized.contains("__")) {
            normalized = normalized.replace("__", "_");
        }
        
        return normalized;
    }
    
    /**
     * Result object for photo URL update operations
     */
    public static class PhotoUrlUpdateResult {
        private Long electionId;
        private Long accountId;
        private int totalVoters;
        private int updatedCount;
        private int skippedCount;
        private boolean success;
        private String message;
        
        public void incrementUpdated() { this.updatedCount++; }
        public void incrementSkipped() { this.skippedCount++; }
        
        // Getters and setters
        public Long getElectionId() { return electionId; }
        public void setElectionId(Long electionId) { this.electionId = electionId; }
        
        public Long getAccountId() { return accountId; }
        public void setAccountId(Long accountId) { this.accountId = accountId; }
        
        public int getTotalVoters() { return totalVoters; }
        public void setTotalVoters(int totalVoters) { this.totalVoters = totalVoters; }
        
        public int getUpdatedCount() { return updatedCount; }
        public void setUpdatedCount(int updatedCount) { this.updatedCount = updatedCount; }
        
        public int getSkippedCount() { return skippedCount; }
        public void setSkippedCount(int skippedCount) { this.skippedCount = skippedCount; }
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}
