package com.thedal.thedal_app.voter;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "bulk_upload_errors")
@Data
public class BulkUploadErrorEntity {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bulk_upload_id")
    private Long bulkUploadId;

    @JsonIgnore
    @Column(name = "election_id")
    private Long electionId;
    @JsonIgnore
    @Column(name = "account_id")
    private Long accountId;

//    @Column(name = "header_errors")
//    private String headerErrors; 
    @Column(name = "header_errors", columnDefinition = "TEXT")
    @JsonProperty("headerErrors")
    private String headerErrors;

//    @Column(name = "row_number")
//    private Integer rowNumber;
    @Column(name = "row_number", columnDefinition = "TEXT")
    @JsonProperty("rowNumber")
    private String rowNumber;

//    @Column(name = "row_error")
//    private String rowError;
    @Column(name = "row_error", columnDefinition = "TEXT")
    @JsonProperty("rowError")
    private String rowError;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
