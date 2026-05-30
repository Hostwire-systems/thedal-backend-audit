package com.thedal.thedal_app.volunteer;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VolunteerDownloadJob {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private Long accountId;
    private Long electionId;
    private LocalDateTime timeStarted;
    private LocalDateTime timeCompleted;
    private String status;
    private String errorMessage;
    private String awsS3DownloadUrl;
    
}