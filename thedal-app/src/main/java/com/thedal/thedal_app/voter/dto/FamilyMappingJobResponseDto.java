package com.thedal.thedal_app.voter.dto;

import java.time.LocalDateTime;

import com.thedal.thedal_app.voter.BulkUploadStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FamilyMappingJobResponseDto {

    private Long jobId;
    private Long accountId;
    private Long electionId;
    private BulkUploadStatus status;
    private Boolean run;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long totalVoters;
    private Long processedVoters;
    private Long familiesCreated;
    private Long votersWithFamilies;
    private Long uniqueHouseNumbers;
    private Double progressPercentage;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Helper methods
    public boolean isCompleted() {
        return status == BulkUploadStatus.COMPLETED;
    }

    public boolean isFailed() {
        return status == BulkUploadStatus.FAILED;
    }

    public boolean isInProgress() {
        return status == BulkUploadStatus.IN_PROGRESS;
    }

    public String getFormattedDuration() {
        if (startTime == null) {
            return "Not started";
        }
        
        LocalDateTime endDateTime = endTime != null ? endTime : LocalDateTime.now();
        long durationSeconds = java.time.Duration.between(startTime, endDateTime).toSeconds();
        
        long hours = durationSeconds / 3600;
        long minutes = (durationSeconds % 3600) / 60;
        long seconds = durationSeconds % 60;
        
        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }
}
