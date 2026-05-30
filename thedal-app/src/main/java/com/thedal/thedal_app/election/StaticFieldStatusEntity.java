package com.thedal.thedal_app.election;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity to manage on/off status for static voter fields per election.
 * This allows election-specific configuration of which static fields
 * should be displayed/processed in the system.
 */
@Entity
@Table(
    name = "static_field_status",
    indexes = {
        @Index(name = "idx_static_field_election_account", columnList = "election_id, account_id, field_name")
    },
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"account_id", "election_id", "field_name"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StaticFieldStatusEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @NotBlank(message = "Field name is mandatory")
    @Column(name = "field_name", nullable = false)
    @JsonProperty("fieldName")
    private String fieldName;

    @Column(name = "field_label")
    @JsonProperty("fieldLabel")
    private String fieldLabel;

    @Column(name = "field_category")
    @JsonProperty("fieldCategory") 
    private String fieldCategory; // e.g., "basic", "address", "contact", "family", etc.

    @Column(name = "status", nullable = false)
    @JsonProperty("status")
    private Boolean status = true; // Default to true (enabled)

    @Column(name = "mandatory", nullable = false)
    @JsonProperty("mandatory")
    private Boolean mandatory = false; // Default to false (optional)

    @JsonIgnore
    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "election_id", nullable = false)
    private Long electionId;

    @Column(name = "created_time", nullable = false, updatable = false)
    private LocalDateTime createdTime;

    @Column(name = "modified_time")
    private LocalDateTime modifiedTime;

    @PrePersist
    protected void onCreate() {
        if (createdTime == null) {
            createdTime = LocalDateTime.now();
        }
        if (status == null) {
            status = true; // Default to enabled
        }
        if (mandatory == null) {
            mandatory = false; // Default to optional
        }
    }

    @PreUpdate
    protected void onUpdate() {
        modifiedTime = LocalDateTime.now();
    }
}