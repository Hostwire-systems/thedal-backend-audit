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
public class BulkUploadDto {
	private Long bulkUploadId;
    //private Long accountId;
    private Long electionId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BulkUploadStatus status;
    
    @Override
    public String toString() {
        return "BulkUploadDto{" +
                "id=" + bulkUploadId +
//                ", accountId=" + accountId +
                ", electionId=" + electionId +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", status=" + status +
                '}';
    }

	
}
