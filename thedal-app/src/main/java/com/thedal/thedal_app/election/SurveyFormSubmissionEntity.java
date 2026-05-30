package com.thedal.thedal_app.election;

import java.time.LocalDateTime;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "_survey_form_submissions",
    indexes = {
        @Index(name = "idx_submission_form_election_account", columnList = "form_id, election_id, account_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SurveyFormSubmissionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

//    @Column(name = "form_id", nullable = false)
    @Column(name = "form_id")
    private Long formId;

    @Column(name = "election_id", nullable = false)
    private Long electionId;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @JdbcTypeCode(SqlTypes.JSON)
//    @Column(name = "submission_data", columnDefinition = "jsonb", nullable = false)
    @Column(name = "submission_data", columnDefinition = "jsonb")
    private Map<String, Object> submissionData;

//    @Column(name = "submitted_at", nullable = false, updatable = false)
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @PrePersist
    protected void onCreate() {
        if (submittedAt == null) {
            submittedAt = LocalDateTime.now();
        }
    }
}