package com.thedal.thedal_app.voter.dto;

import java.time.LocalDateTime;

import com.thedal.thedal_app.voter.BulkUploadStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BulkUploadStatusDto {

	private Long bulkUploadId;
    private BulkUploadStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    
    private int totalProcessedVoters;
    private int totalFailedVoters;
    private long totalRecords;
    private int totalSuccessVoters;

    
}
