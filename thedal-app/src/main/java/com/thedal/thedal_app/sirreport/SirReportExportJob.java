package com.thedal.thedal_app.sirreport;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "sir_report_export_jobs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SirReportExportJob {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sir_report_export_jobs_seq_gen")
    @SequenceGenerator(name = "sir_report_export_jobs_seq_gen", sequenceName = "sir_report_export_jobs_seq", allocationSize = 1)
    private Long id;
    
    @Column(nullable = false)
    private UUID jobId; // Reference to the SIR comparison job
    
    @Column(nullable = false)
    private Long accountId;
    
    private Long electionId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SirReportExportType exportType; // ADDITIONS, DELETIONS, SHIFTS
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SirReportExportFormat format; // EXCEL, PDF
    
    @Column(nullable = false)
    private String status; // PROCESSING, COMPLETED, FAILED
    
    private String message;
    
    @Column(name = "aws_s3_download_url")
    private String awsS3DownloadUrl;
    
    private Integer recordCount;
    
    @Column(nullable = false)
    private LocalDateTime timeStarted;
    
    private LocalDateTime timeCompleted;
    
    @Column(nullable = false)
    private LocalDateTime expiresAt; // 24 hours from completion
}
