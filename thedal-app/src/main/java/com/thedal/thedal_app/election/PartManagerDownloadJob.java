package com.thedal.thedal_app.election;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "part_manager_download_job")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PartManagerDownloadJob {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "account_id", nullable = false)
    private Long accountId;
    
    @Column(name = "election_id", nullable = false)
    private Long electionId;
    
    @Column(nullable = false)
    private String status; // PENDING, IN_PROGRESS, COMPLETED, FAILED
    
    @Column(nullable = false)
    private String format; // PDF or EXCEL
    
    @Column(name = "time_started")
    private LocalDateTime timeStarted;
    
    @Column(name = "time_completed")
    private LocalDateTime timeCompleted;
    
    @Column(name = "aws_s3_download_url", length = 1000)
    private String awsS3DownloadUrl;
    
    @Column(name = "local_file_path", length = 500)
    private String localFilePath;
    
    @Column(length = 1000)
    private String message;
    
    @Column(name = "total_records")
    private Integer totalRecords;
}
