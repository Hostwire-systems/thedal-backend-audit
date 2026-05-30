package com.thedal.thedal_app.election;

import java.time.LocalDateTime;
import java.util.List;

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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "_voter_field_orders",
    indexes = {
        @Index(name = "idx_voter_field_order_election_account", columnList = "election_id, account_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VoterFieldOrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "election_id", nullable = false)
    private Long electionId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ordered_fields", columnDefinition = "jsonb", nullable = false)
    @JsonProperty("orderedFields")
    private List<FieldOrderItem> orderedFields; // Stores [{name, orderIndex}, ...]

    @Column(name = "created_time", nullable = false, updatable = false)
    private LocalDateTime createdTime;

    @Column(name = "modified_time")
    private LocalDateTime modifiedTime;

    @PrePersist
    protected void onCreate() {
        if (createdTime == null) {
            createdTime = LocalDateTime.now();
        }
        if (modifiedTime == null) {
            modifiedTime = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        modifiedTime = LocalDateTime.now();
    }

    // Inner class to represent field order item
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldOrderItem {
        private String name;
        private Integer orderIndex;
    }
}