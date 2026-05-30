package com.thedal.thedal_app.migration;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.CompletableFuture;

/**
 * Represents a migration job with its status and progress
 */
@Getter
@Setter
public class MigrationJob {
    private String jobId;
    private Long accountId;
    private Long electionId;
    private String description;
    private MigrationJobStatus status;
    private long totalRecords;
    private long processedRecords;
    private long failedRecords;
    private long startTime;
    private Long endTime;
    private LocalDateTime completedTime;
    private String errorMessage;
    private volatile boolean cancelled = false;
    private CompletableFuture<Void> future;

    public MigrationJob(String jobId, Long accountId, Long electionId, String description) {
        this.jobId = jobId;
        this.accountId = accountId;
        this.electionId = electionId;
        this.description = description;
        this.status = MigrationJobStatus.RUNNING;
        this.startTime = System.currentTimeMillis();
    }

    // Alternative constructor for compatibility
    public MigrationJob(String jobId, Long accountId, Long electionId) {
        this(jobId, accountId, electionId, "Migration job for account " + accountId);
    }

    public double getProgressPercentage() {
        if (totalRecords == 0) return 0.0;
        return (processedRecords * 100.0) / totalRecords;
    }

    public long getDuration() {
        if (endTime == null) {
            return System.currentTimeMillis() - startTime;
        }
        return endTime - startTime;
    }

    public void cancel() {
        this.cancelled = true;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public LocalDateTime getStartTime() {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(startTime), ZoneId.systemDefault());
    }

    public LocalDateTime getEndTime() {
        if (endTime == null) return null;
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(endTime), ZoneId.systemDefault());
    }

    // Additional methods for compatibility
    public LocalDateTime getCompletedTime() {
        return completedTime;
    }

    public void setCompletedTime(LocalDateTime completedTime) {
        this.completedTime = completedTime;
        if (completedTime != null && this.endTime == null) {
            this.endTime = completedTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        }
    }

    public CompletableFuture<Void> getFuture() {
        return future;
    }

    public void setFuture(CompletableFuture<Void> future) {
        this.future = future;
    }
}
