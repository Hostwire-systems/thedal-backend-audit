package com.thedal.thedal_app.migration;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response object for global migration job operations
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GlobalMigrationJobResponse {
    private String jobId;
    private String status;
    private String description;
    private long totalEstimatedRecords;
    private int batchSize;
    private long startTime;
    private String message;
    
    public GlobalMigrationJobResponse(String jobId, String status, String description, 
                                    long totalEstimatedRecords, int batchSize) {
        this.jobId = jobId;
        this.status = status;
        this.description = description;
        this.totalEstimatedRecords = totalEstimatedRecords;
        this.batchSize = batchSize;
        this.startTime = System.currentTimeMillis();
        this.message = "Global migration job started successfully";
    }
    
    public GlobalMigrationJobResponse(String jobId, String status, String message) {
        this.jobId = jobId;
        this.status = status;
        this.message = message;
        this.startTime = System.currentTimeMillis();
    }
}
