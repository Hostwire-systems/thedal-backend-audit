package com.thedal.thedal_app.election;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "_survey_download_jobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SurveyDownloadJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "election_id", nullable = false)
    private Long electionId;

    @Column(name = "form_id", nullable = false)
    private Long formId;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "time_started", nullable = false)
    private LocalDateTime timeStarted;

    @Column(name = "time_completed")
    private LocalDateTime timeCompleted;

    @Column(name = "aws_s3_download_url")
    private String awsS3DownloadUrl;

    @Column(name = "error_message")
    private String errorMessage;
}