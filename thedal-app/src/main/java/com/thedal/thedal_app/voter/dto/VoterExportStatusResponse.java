package com.thedal.thedal_app.voter.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VoterExportStatusResponse {
    private Long jobId;
    private String status; 
    private String awsS3DownloadUrl; 
    private String message; 
    private LocalDateTime timeStarted;
    private LocalDateTime timeCompleted;
}