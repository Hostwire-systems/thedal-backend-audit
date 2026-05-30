package com.thedal.thedal_app.election;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "_survey_forms",
    indexes = {
        @Index(name = "idx_election_account_form", columnList = "election_id, account_id, form_name")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SurveyFormEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @NotBlank(message = "Form name is mandatory")
    @Column(name = "form_name", nullable = false)
    @JsonProperty("formName")
    private String formName;
    
    @Column(name = "form_description", columnDefinition = "TEXT")
    @JsonProperty("formDescription")
    private String formDescription;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "election_id", nullable = false)
    private Long electionId;

    @Column(name = "is_active", nullable = false)
    @JsonProperty("isActive")
    private Boolean isActive = true;
    
    @Column(name = "order_index")
    private Integer orderIndex = 0; 

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "custom_fields", columnDefinition = "jsonb")
    @JsonProperty("customFields")
    private List<Map<String, Object>> customFields;

    @Column(name = "created_time", nullable = false, updatable = false)
    private LocalDateTime createdTime;

    @Column(name = "modified_time")
    private LocalDateTime modifiedTime;

    @PrePersist
    protected void onCreate() {
        if (createdTime == null) {
            createdTime = LocalDateTime.now();
        }
        if (isActive == null) {
            isActive = true;
        }
        if (orderIndex == null) {
            orderIndex = 0; // Default order index
        }
        
    }

    @PreUpdate
    protected void onUpdate() {
        modifiedTime = LocalDateTime.now();
    }
}