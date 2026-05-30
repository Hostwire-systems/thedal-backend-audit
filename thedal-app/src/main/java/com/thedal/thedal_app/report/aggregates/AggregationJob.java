package com.thedal.thedal_app.report.aggregates;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "aggregation_job",
    indexes = {
        @Index(name = "idx_job_id", columnList = "job_id"),
        @Index(name = "idx_account_election", columnList = "account_id, election_id"),
        @Index(name = "idx_status_started", columnList = "status, started_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AggregationJob {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "job_id", nullable = false, unique = true, length = 50)
    private String jobId;
    
    @Column(name = "account_id", nullable = false)
    private Long accountId;
    
    @Column(name = "election_id", nullable = false)
    private Long electionId;
    
    @Column(name = "job_type", nullable = false, length = 30)
    private String jobType; // 'ELECTION_STATS', 'DEMOGRAPHICS', etc.
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AggregationJobStatus status;
    
    @Column(name = "part_number", length = 10)
    private String partNumber;
    
    @Column(name = "total_parts")
    private Integer totalParts;
    
    @Column(name = "completed_parts")
    private Integer completedParts = 0;
    
    @Column(name = "started_at")
    private LocalDateTime startedAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "cancelled")
    private Boolean cancelled = false;
    
    @PrePersist
    protected void onCreate() {
        if (startedAt == null && status == AggregationJobStatus.IN_PROGRESS) {
            startedAt = LocalDateTime.now();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        if (completedAt == null && (status == AggregationJobStatus.COMPLETED || 
            status == AggregationJobStatus.FAILED || 
            status == AggregationJobStatus.CANCELLED)) {
            completedAt = LocalDateTime.now();
        }
    }
    
    public Long getElapsedSeconds() {
        if (startedAt == null) return 0L;
        LocalDateTime endTime = completedAt != null ? completedAt : LocalDateTime.now();
        return java.time.Duration.between(startedAt, endTime).getSeconds();
    }
}
