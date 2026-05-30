package com.thedal.thedal_app.voter;

import java.time.LocalDateTime;

import com.thedal.thedal_app.voter.BulkUploadStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "family_mapping_jobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FamilyMappingJobEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "election_id", nullable = false)
    private Long electionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BulkUploadStatus status;

    @Column(name = "run", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean run = false;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "total_voters", nullable = false, columnDefinition = "BIGINT DEFAULT 0")
    private Long totalVoters = 0L;

    @Column(name = "processed_voters", nullable = false, columnDefinition = "BIGINT DEFAULT 0")
    private Long processedVoters = 0L;

    @Column(name = "families_created", nullable = false, columnDefinition = "BIGINT DEFAULT 0")
    private Long familiesCreated = 0L;

    @Column(name = "voters_with_families", nullable = false, columnDefinition = "BIGINT DEFAULT 0")
    private Long votersWithFamilies = 0L;

    @Column(name = "unique_house_numbers", nullable = false, columnDefinition = "BIGINT DEFAULT 0")
    private Long uniqueHouseNumbers = 0L;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.startTime == null) {
            this.startTime = now;
        }
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public double getProgressPercentage() {
        if (totalVoters == null || totalVoters == 0) {
            return 0.0;
        }
        return (processedVoters.doubleValue() / totalVoters.doubleValue()) * 100.0;
    }

    public boolean isCompleted() {
        return status == BulkUploadStatus.COMPLETED;
    }

    public boolean isFailed() {
        return status == BulkUploadStatus.FAILED;
    }

    public boolean isInProgress() {
        return status == BulkUploadStatus.IN_PROGRESS;
    }

    public boolean isRunning() {
        return Boolean.TRUE.equals(run);
    }
}
