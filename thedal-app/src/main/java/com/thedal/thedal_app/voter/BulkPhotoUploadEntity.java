package com.thedal.thedal_app.voter;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "bulk_photo_uploads")
@AllArgsConstructor
@NoArgsConstructor
public class BulkPhotoUploadEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "account_id", nullable = false)
    private Long accountId;
    
    @Column(name = "election_id", nullable = false)
    private Long electionId;
    
    @Column(name = "start_time")
    private LocalDateTime startTime;
    
    @Column(name = "end_time")
    private LocalDateTime endTime;
    
    @Enumerated(EnumType.STRING)
    private BulkUploadStatus status;
    
    @Column(name = "total_photos")
    private Integer totalPhotos = 0;
    
    @Column(name = "processed_photos")
    private Integer processedPhotos = 0;
    
    @Column(name = "successful_uploads")
    private Integer successfulUploads = 0;
    
    @Column(name = "failed_uploads")
    private Integer failedUploads = 0;
    
    @Column(name = "filename")
    private String zipFileName;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorDetails; // JSON string of errors
    
    @Column(name = "created_date")
    private LocalDateTime createdDate;
    
    @Column(name = "updated_date")
    private LocalDateTime updatedDate;
    
    public BulkPhotoUploadEntity(Long accountId, Long electionId, String zipFileName) {
        this.accountId = accountId;
        this.electionId = electionId;
        this.zipFileName = zipFileName;
        this.startTime = LocalDateTime.now();
        this.createdDate = LocalDateTime.now();
        this.updatedDate = LocalDateTime.now();
        this.status = BulkUploadStatus.IN_PROGRESS;
    }
}
