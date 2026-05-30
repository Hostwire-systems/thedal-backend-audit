package com.thedal.thedal_app.familycaptain.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class FamilyCaptainUploadSummary {

    @JsonProperty("total_rows")
    private int totalRows;

    @JsonProperty("successful_uploads")
    private int successfulUploads;

    @JsonProperty("failed_uploads")
    private int failedUploads;

    @JsonProperty("upload_time")
    private LocalDateTime uploadTime;

    @JsonProperty("file_name")
    private String fileName;

    @JsonProperty("error_details")
    private String errorDetails;

    public FamilyCaptainUploadSummary() {
        this.uploadTime = LocalDateTime.now();
    }

    public FamilyCaptainUploadSummary(int totalRows, int successfulUploads, int failedUploads) {
        this.totalRows = totalRows;
        this.successfulUploads = successfulUploads;
        this.failedUploads = failedUploads;
        this.uploadTime = LocalDateTime.now();
    }
}
