package com.thedal.thedal_app.election;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "_dynamic_fields",
    indexes = {
        @Index(name = "idx_dynamic_field_election_account", columnList = "election_id, account_id, label")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DynamicFieldEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @NotBlank(message = "Label is mandatory")
    @Column(name = "label", nullable = false)
    @JsonProperty("label")
    private String label;
    
   // @NotBlank(message = "Name is mandatory")
    @Column(name = "name", nullable = false)
    @JsonProperty("name")
    private String name;

    @NotBlank(message = "Type is mandatory")
    @Column(name = "type", nullable = false)
    @JsonProperty("type")
    private String type;

    @NotNull(message = "Required field is mandatory")
    @Column(name = "required", nullable = false)
    @JsonProperty("required")
    private Boolean required;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "options", columnDefinition = "jsonb")
    @JsonProperty("options")
    private List<String> options;

    @Column(name = "order_index")
    @JsonProperty("orderIndex")
    private Integer orderIndex = 0;

    @JsonIgnore
    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "election_id", nullable = false)
    private Long electionId;

    @Column(name = "created_time", nullable = false, updatable = false)
    private LocalDateTime createdTime;

    @Column(name = "modified_time")
    private LocalDateTime modifiedTime;
       
    @Column(name = "status")
    @JsonProperty("status")
    private Boolean status = false;

    @PrePersist
    protected void onCreate() {
        if (createdTime == null) {
            createdTime = LocalDateTime.now();
        }
        if (required == null) {
            required = false;
        }
        if (orderIndex == null) {
            orderIndex = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        modifiedTime = LocalDateTime.now();
    }
}