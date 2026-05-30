package com.thedal.thedal_app.voter.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BulkUploadErrorResponseDTO {
	
	private Long id;
    private Long bulkUploadId;
    
    @JsonProperty("headerErrors")
    private List<String> headerErrors; // Deserialized list of missing headers
    
    @JsonProperty("rowErrors")
    private List<RowError> rowErrors; // Deserialized row errors
    
    private LocalDateTime createdAt;

    public BulkUploadErrorResponseDTO(Long id, Long bulkUploadId, List<String> headerErrors, 
                                      List<RowError> rowErrors, LocalDateTime createdAt) {
        this.id = id;
        this.bulkUploadId = bulkUploadId;
        this.headerErrors = headerErrors;
        this.rowErrors = rowErrors;
        this.createdAt = createdAt;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getBulkUploadId() { return bulkUploadId; }
    public void setBulkUploadId(Long bulkUploadId) { this.bulkUploadId = bulkUploadId; }
    public List<String> getHeaderErrors() { return headerErrors; }
    public void setHeaderErrors(List<String> headerErrors) { this.headerErrors = headerErrors; }
    public List<RowError> getRowErrors() { return rowErrors; }
    public void setRowErrors(List<RowError> rowErrors) { this.rowErrors = rowErrors; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static class RowError {
        private int rowNumber;
        private List<Map<String, Object>> errors;

        public RowError(int rowNumber, List<Map<String, Object>> errors) {
            this.rowNumber = rowNumber;
            this.errors = errors;
        }

        public int getRowNumber() { return rowNumber; }
        public void setRowNumber(int rowNumber) { this.rowNumber = rowNumber; }
        public List<Map<String, Object>> getErrors() { return errors; }
        public void setErrors(List<Map<String, Object>> errors) { this.errors = errors; }
    }

}
