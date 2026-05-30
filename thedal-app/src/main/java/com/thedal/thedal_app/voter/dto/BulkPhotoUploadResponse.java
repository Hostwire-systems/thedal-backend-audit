package com.thedal.thedal_app.voter.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.thedal.thedal_app.voter.BulkUploadStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkPhotoUploadResponse {
    
    private Long bulkUploadId;
    private BulkUploadStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer totalPhotos;
    private Integer processedPhotos;
    private Integer successfulUploads;
    private Integer failedUploads;
    private List<PhotoUploadError> errors = new ArrayList<>();
    private String message;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PhotoUploadError {
        private String filename;
        private String epicNumber;
        private String errorMessage;
        private String errorType; // VOTER_NOT_FOUND, UPLOAD_FAILED, INVALID_FORMAT, etc.
    }
    
    public BulkPhotoUploadResponse(Long bulkUploadId, String message) {
        this.bulkUploadId = bulkUploadId;
        this.message = message;
        this.status = BulkUploadStatus.IN_PROGRESS;
        this.startTime = LocalDateTime.now();
        this.totalPhotos = 0;
        this.successfulUploads = 0;
        this.failedUploads = 0;
        this.processedPhotos = 0;
    }
}
