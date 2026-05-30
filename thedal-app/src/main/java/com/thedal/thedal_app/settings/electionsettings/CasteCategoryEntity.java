package com.thedal.thedal_app.settings.electionsettings;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

@Entity
@Table(
    name = "caste_category",
    indexes = {
        @Index(name = "idx_caste_category_account_id", columnList = "casteCategoryName, accountId")
    }
)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CasteCategoryEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "caste_category_name")
    private String casteCategoryName;
    
    @JsonIgnore
    private Long accountId;
    
    @Column(name = "election_id")
    private Long electionId;
    
    @Column(name = "order_index", nullable = false)
    private Integer orderIndex = 0;
    
    @JsonIgnore
    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;
    
    @JsonIgnore
    @LastModifiedDate
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    private void ensureDefaults() {
        if (orderIndex == null) {
            orderIndex = 0; // prevents NPE during serialization
        }
    }

    // Defensive getter in case existing DB rows loaded before lifecycle hooks or detached objects bypass ensureDefaults
    public Integer getOrderIndex() {
        return orderIndex == null ? 0 : orderIndex;
    }
}