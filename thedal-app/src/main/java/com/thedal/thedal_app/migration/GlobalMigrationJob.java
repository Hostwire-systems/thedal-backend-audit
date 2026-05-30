package com.thedal.thedal_app.migration;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents a global migration job with advanced tracking and management capabilities
 */
@Getter
@Setter
public class GlobalMigrationJob {
    
    private String jobId;
    private String description;
    private GlobalMigrationJobType type;
    private GlobalMigrationJobStatus status;
    private long totalEstimatedRecords;
    private AtomicLong processedRecords;
    private AtomicLong failedRecords;
    private long startTime;
    private Long endTime;
    private LocalDateTime completedTime;
    private String errorMessage;
    private volatile boolean cancelled = false;
    private CompletableFuture<Void> future;
    
    // Configuration options
    private int batchSize;
    private int parallelism;
    private boolean overwriteExisting;
    private boolean skipValidation;
    private boolean useParallelProcessing;
    private List<String> includeModules;
    private List<String> excludeModules;
    private List<Long> accountIds;
    
    // Performance tracking
    private long estimatedTimeRemaining;
    private double averageRecordsPerSecond;
    private long memoryUsageAtStart;
    private long memoryUsageCurrent;
    
    // Phase tracking
    private String currentPhase;
    private int totalPhases;
    private int completedPhases;
    
    public GlobalMigrationJob(String jobId, String description) {
        this.jobId = jobId;
        this.description = description;
        this.type = GlobalMigrationJobType.COMPLETE_GLOBAL;
        this.status = GlobalMigrationJobStatus.PENDING;
        this.startTime = System.currentTimeMillis();
        this.processedRecords = new AtomicLong(0);
        this.failedRecords = new AtomicLong(0);
        this.totalPhases = 4; // settings, users, voters, elections
        this.completedPhases = 0;
        
        // Record initial memory usage
        Runtime runtime = Runtime.getRuntime();
        this.memoryUsageAtStart = runtime.totalMemory() - runtime.freeMemory();
    }
    
    public GlobalMigrationJob(String jobId, GlobalMigrationJobType type, List<Long> accountIds, int batchSize, int parallelism, boolean clearExisting) {
        this.jobId = jobId;
        this.type = type;
        this.description = type.name() + " Migration";
        this.status = GlobalMigrationJobStatus.PENDING;
        this.startTime = System.currentTimeMillis();
        this.processedRecords = new AtomicLong(0);
        this.failedRecords = new AtomicLong(0);
        this.totalPhases = 4;
        this.completedPhases = 0;
        this.accountIds = accountIds;
        this.batchSize = batchSize;
        this.parallelism = parallelism;
        this.overwriteExisting = clearExisting;
        
        // Record initial memory usage
        Runtime runtime = Runtime.getRuntime();
        this.memoryUsageAtStart = runtime.totalMemory() - runtime.freeMemory();
    }
    
    public double getProgressPercentage() {
        if (totalEstimatedRecords == 0) return 0.0;
        return (processedRecords.get() * 100.0) / totalEstimatedRecords;
    }
    
    public double getPhaseProgressPercentage() {
        if (totalPhases == 0) return 0.0;
        return (completedPhases * 100.0) / totalPhases;
    }
    
    public long getDuration() {
        if (endTime == null) {
            return System.currentTimeMillis() - startTime;
        }
        return endTime - startTime;
    }
    
    public String getFormattedDuration() {
        long duration = getDuration();
        long hours = duration / (1000 * 60 * 60);
        long minutes = (duration % (1000 * 60 * 60)) / (1000 * 60);
        long seconds = (duration % (1000 * 60)) / 1000;
        
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
    
    public void incrementProcessedRecords(long count) {
        processedRecords.addAndGet(count);
        updatePerformanceMetrics();
    }
    
    public void incrementFailedRecords(long count) {
        failedRecords.addAndGet(count);
    }
    
    public void completePhase(String phaseName) {
        this.completedPhases++;
        log("Completed phase: " + phaseName + " (" + completedPhases + "/" + totalPhases + ")");
        updatePerformanceMetrics();
    }
    
    public void setCurrentPhase(String phaseName) {
        this.currentPhase = phaseName;
        log("Starting phase: " + phaseName);
    }
    
    public void updatePerformanceMetrics() {
        long elapsed = getDuration();
        if (elapsed > 0) {
            averageRecordsPerSecond = (processedRecords.get() * 1000.0) / elapsed;
            
            if (averageRecordsPerSecond > 0) {
                long remainingRecords = totalEstimatedRecords - processedRecords.get();
                estimatedTimeRemaining = (long) (remainingRecords / averageRecordsPerSecond * 1000);
            }
        }
        
        // Update current memory usage
        Runtime runtime = Runtime.getRuntime();
        memoryUsageCurrent = runtime.totalMemory() - runtime.freeMemory();
    }
    
    public String getFormattedEstimatedTimeRemaining() {
        if (estimatedTimeRemaining == 0) return "Unknown";
        
        long hours = estimatedTimeRemaining / (1000 * 60 * 60);
        long minutes = (estimatedTimeRemaining % (1000 * 60 * 60)) / (1000 * 60);
        long seconds = (estimatedTimeRemaining % (1000 * 60)) / 1000;
        
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
    
    public String getFormattedMemoryUsage() {
        return String.format("%.2f MB", memoryUsageCurrent / (1024.0 * 1024.0));
    }
    
    public String getFormattedMemoryUsageIncrease() {
        long increase = memoryUsageCurrent - memoryUsageAtStart;
        return String.format("%.2f MB", increase / (1024.0 * 1024.0));
    }
    
    public void cancel() {
        this.cancelled = true;
        this.status = GlobalMigrationJobStatus.CANCELLED;
        if (future != null) {
            future.cancel(true);
        }
    }
    
    public boolean isCancelled() {
        return cancelled;
    }
    
    public boolean isRunning() {
        return status == GlobalMigrationJobStatus.RUNNING;
    }
    
    public boolean isCompleted() {
        return status == GlobalMigrationJobStatus.COMPLETED;
    }
    
    public boolean isFailed() {
        return status == GlobalMigrationJobStatus.FAILED;
    }
    
    public boolean isPaused() {
        return status == GlobalMigrationJobStatus.PAUSED;
    }
    
    public LocalDateTime getStartTime() {
        return LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(startTime),
            java.time.ZoneId.systemDefault()
        );
    }
    
    public LocalDateTime getEndTime() {
        if (endTime == null) return null;
        return LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(endTime),
            java.time.ZoneId.systemDefault()
        );
    }
    
    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedTime = completedAt;
        this.endTime = completedAt.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
    
    public void setError(String error) {
        this.errorMessage = error;
    }
    
    private void log(String message) {
        System.out.println(String.format("[%s] %s: %s", 
            LocalDateTime.now().toString(), jobId, message));
    }
    
    @Override
    public String toString() {
        return String.format("GlobalMigrationJob{id='%s', status=%s, progress=%.2f%%, phase=%s, duration=%s}",
            jobId, status, getProgressPercentage(), currentPhase, getFormattedDuration());
    }
}
