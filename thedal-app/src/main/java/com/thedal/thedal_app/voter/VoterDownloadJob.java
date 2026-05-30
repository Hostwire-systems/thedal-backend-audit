package com.thedal.thedal_app.voter;

import java.time.LocalDateTime;

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
@Table(name = "voter_download_jobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VoterDownloadJob {
	
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long accountId;
    private Long electionId;
    private LocalDateTime timeStarted;
    private LocalDateTime timeCompleted;
    private String status;
    private String awsS3DownloadUrl;
    private String errorMessage;
    
}