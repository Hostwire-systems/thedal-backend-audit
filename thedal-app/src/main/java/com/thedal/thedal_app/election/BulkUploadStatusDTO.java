package com.thedal.thedal_app.election;

import java.time.LocalDateTime;

import com.thedal.thedal_app.voter.BulkUploadStatus;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BulkUploadStatusDTO {
    private Long bulkUploadId;
    private BulkUploadType type;
    private BulkUploadStatus status; 
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int totalRecords;
    private int processedRecords;
    private int successRecords;
    private int failedRecords;
}