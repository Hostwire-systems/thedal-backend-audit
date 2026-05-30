package com.thedal.thedal_app.volunteer.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VolunteerJobStatusResponse {

    @JsonProperty("job_id")
    private Long jobId;

    @JsonProperty("status")
    private String status;

    @JsonProperty("time_started")
    private LocalDateTime timeStarted;

    @JsonProperty("time_completed")
    private LocalDateTime timeCompleted;

    @JsonProperty("error_message")
    private String errorMessage;

    @JsonProperty("download_url")
    private String downloadUrl;
}
