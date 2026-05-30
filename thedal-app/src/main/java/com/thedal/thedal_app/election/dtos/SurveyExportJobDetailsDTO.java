package com.thedal.thedal_app.election.dtos;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SurveyExportJobDetailsDTO {
    private Long jobId;
    private Long accountId;
    private Long electionId;
    private Long formId;
    private String status;
    private LocalDateTime timeStarted;
    private LocalDateTime timeCompleted;
    private String awsS3DownloadUrl;
    private String errorMessage;
}