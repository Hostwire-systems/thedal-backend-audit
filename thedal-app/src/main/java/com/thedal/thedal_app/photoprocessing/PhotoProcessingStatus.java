package com.thedal.thedal_app.photoprocessing;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PhotoProcessingStatus {
    private String jobId;
    private String status; // STARTED, PROCESSING, COMPLETED, FAILED
    private String message;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer totalPhotos;
    private Integer processedPhotos;
    private Integer successfulUpdates;
    private Integer failedUpdates;
    private List<String> errors;
    private double progressPercentage;
}
