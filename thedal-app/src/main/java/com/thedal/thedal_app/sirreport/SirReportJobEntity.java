package com.thedal.thedal_app.sirreport;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "sir_report_job", indexes = {
    @Index(name = "idx_sir_job_account_election", columnList = "account_id, election_id"),
    @Index(name = "idx_sir_job_id", columnList = "job_id")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SirReportJobEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "job_id", nullable = false, unique = true)
    private UUID jobId;
    
    @Column(name = "account_id", nullable = false)
    private Long accountId;
    
    @Column(name = "election_id")
    private Long electionId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SirReportStatus status;
    
    @Column(name = "total_base_records")
    private Integer totalBaseRecords;
    
    @Column(name = "total_new_records")
    private Integer totalNewRecords;
    
    @Column(name = "additions_count")
    private Integer additionsCount;
    
    @Column(name = "deletions_count")
    private Integer deletionsCount;
    
    @Column(name = "shifts_count")
    private Integer shiftsCount;
    
    @Column(name = "base_file_name")
    private String baseFileName;
    
    @Column(name = "new_file_name")
    private String newFileName;
    
    @Column(name = "progress")
    private Integer progress; // 0-100
    
    @Column(name = "message")
    private String message;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @PrePersist
    protected void onCreate() {
        if (jobId == null) {
            jobId = UUID.randomUUID();
        }
        if (status == null) {
            status = SirReportStatus.PROCESSING;
        }
        if (progress == null) {
            progress = 0;
        }
    }
}
