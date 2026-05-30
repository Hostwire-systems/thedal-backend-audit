package com.thedal.thedal_app.voter.activity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "voter_activity_log",
    indexes = {
        // Composite index for most common query pattern
        @Index(name = "idx_activity_voter_lookup", columnList = "account_id, election_id, voter_id, activity_type"),
        // Index for time-based queries
        @Index(name = "idx_activity_time", columnList = "activity_time"),
        // Index for election-wide statistics
        @Index(name = "idx_activity_election_type", columnList = "account_id, election_id, activity_type"),
        // Index for voter-specific queries
        @Index(name = "idx_activity_voter_time", columnList = "voter_id, activity_time")
    }
)
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VoterActivityLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "account_id", nullable = false)
    private Long accountId;
    
    @Column(name = "election_id", nullable = false)
    private Long electionId;
    
    @Column(name = "voter_id", nullable = false, length = 50)
    private String voterId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 30)
    private ActivityType activityType;
    
    @Column(name = "activity_time", nullable = false)
    private LocalDateTime activityTime;
    
    @Column(name = "volunteer_id")
    private Long volunteerId;
    
    @Column(name = "template_id")
    private Long templateId;
    
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;
    
    @PrePersist
    protected void onCreate() {
        if (activityTime == null) {
            activityTime = LocalDateTime.now();
        }
    }
}
