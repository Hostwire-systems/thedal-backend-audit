package com.thedal.thedal_app.voter;

import java.time.Instant;
import java.time.LocalDateTime;

import com.thedal.thedal_app.files.Files;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "bulk_upload")
@AllArgsConstructor
public class BulkUploadEntity {
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	
	@Column(name = "account_id")
    private Long accountId;
    
    @Column(nullable = true)
    private Long electionId;

    @Column(name = "start_time")
    private LocalDateTime startTime;
    
    @Column(name = "end_time")
    private LocalDateTime endTime;
    
    @Enumerated(EnumType.STRING)
    private BulkUploadStatus status;
    
    @OneToOne(mappedBy = "bulkUpload", cascade = CascadeType.ALL, orphanRemoval = true)
    private Files file;
    
//    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
//    @JoinColumn(name = "bulk_upload_id") 
//    private List<Files> files;
    
    private int totalProcessedVoters;
    private int totalFailedVoters;
    private long totalRecords;
    private int totalSuccessVoters;
    
    @Column(name = "last_updated_time")
    private LocalDateTime lastUpdatedTime; 
    
    public BulkUploadEntity() {}

    // Constructor to fix the error
    public BulkUploadEntity(Long accountId, Long electionId, LocalDateTime startTime, BulkUploadStatus status) {
        this.accountId = accountId;
        this.electionId = electionId;
        this.startTime = startTime;
        this.status = status;
    }
}
