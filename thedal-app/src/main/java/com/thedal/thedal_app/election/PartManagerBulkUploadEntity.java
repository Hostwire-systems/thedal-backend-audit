package com.thedal.thedal_app.election;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.thedal.thedal_app.files.Files;
import com.thedal.thedal_app.voter.BulkUploadStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "part_manager_bulk_upload")
@NoArgsConstructor
@AllArgsConstructor
public class PartManagerBulkUploadEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @JsonIgnore
    @Column(name = "account_id", nullable = false)
    private Long accountId;
    @JsonIgnore
    @Column(name = "election_id", nullable = false)
    private Long electionId;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    private BulkUploadStatus status;

    @JsonIgnore
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    private Files file; // Stores file metadata

    private Long totalRecords;
    private Long totalProcessedRecords; 
    private Long totalSuccessRecords;  
    private Long totalFailedRecords;
    private Long totalTimeTaken;
    
 // Transient fields for response-specific error details
    @Transient
    private List<String> headerErrors = new ArrayList<>();

    @Transient
    private List<RowError> rowErrors = new ArrayList<>();

    // Nested class for row errors
    @Getter
    @Setter
    public static class RowError {
        private long rowNumber;
        private List<Map<String, String>> errors;

        public RowError(long rowNumber) {
            this.rowNumber = rowNumber;
            this.errors = new ArrayList<>();
        }

        public void addError(String field, String error) {
            Map<String, String> errorDetail = new HashMap<>();
            errorDetail.put("field", field);
            errorDetail.put("error", error);
            this.errors.add(errorDetail);
        }
    }
    
}
