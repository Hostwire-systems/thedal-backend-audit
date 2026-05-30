package com.thedal.thedal_app.voter;

import java.time.LocalDateTime;

public class ExportJobDTO {
    private Long jobId;
    private Long electionId;
    private String type; // "VOTER", "SURVEY", or "POLL_DAY"
    private Long formId; // Nullable for voter/poll day jobs
    private String formName; // Nullable for voter jobs
    private String chartType; // For poll day: "voterCount" or "familyCount"
    private String format; // For poll day: "excel" or "pdf"
    private Integer rowCount; // For poll day: number of rows exported
    private String status;
    private LocalDateTime timeStarted;
    private LocalDateTime timeCompleted;
    private String downloadUrl;
    private String message;

    // Constructor for voter/survey exports
    public ExportJobDTO(Long jobId, Long electionId, String type, Long formId, String formName,
                        String status, LocalDateTime timeStarted, LocalDateTime timeCompleted,
                        String downloadUrl, String message) {
        this.jobId = jobId;
        this.electionId = electionId;
        this.type = type;
        this.formId = formId;
        this.formName = formName;
        this.status = status;
        this.timeStarted = timeStarted;
        this.timeCompleted = timeCompleted;
        this.downloadUrl = downloadUrl;
        this.message = message;
    }

    // Constructor for poll day exports
    public ExportJobDTO(Long jobId, Long electionId, String type, String chartType, String format,
                        Integer rowCount, String status, LocalDateTime timeStarted, LocalDateTime timeCompleted,
                        String downloadUrl, String message) {
        this.jobId = jobId;
        this.electionId = electionId;
        this.type = type;
        this.chartType = chartType;
        this.format = format;
        this.rowCount = rowCount;
        this.status = status;
        this.timeStarted = timeStarted;
        this.timeCompleted = timeCompleted;
        this.downloadUrl = downloadUrl;
        this.message = message;
    }

    // Getters and Setters
    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }
    public Long getElectionId() { return electionId; }
    public void setElectionId(Long electionId) { this.electionId = electionId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Long getFormId() { return formId; }
    public void setFormId(Long formId) { this.formId = formId; }
    public String getFormName() { return formName; }
    public void setFormName(String formName) { this.formName = formName; }
    public String getChartType() { return chartType; }
    public void setChartType(String chartType) { this.chartType = chartType; }
    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }
    public Integer getRowCount() { return rowCount; }
    public void setRowCount(Integer rowCount) { this.rowCount = rowCount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getTimeStarted() { return timeStarted; }
    public void setTimeStarted(LocalDateTime timeStarted) { this.timeStarted = timeStarted; }
    public LocalDateTime getTimeCompleted() { return timeCompleted; }
    public void setTimeCompleted(LocalDateTime timeCompleted) { this.timeCompleted = timeCompleted; }
    public String getDownloadUrl() { return downloadUrl; }
    public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}