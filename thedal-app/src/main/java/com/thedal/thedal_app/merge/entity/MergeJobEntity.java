package com.thedal.thedal_app.merge.entity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.thedal.thedal_app.merge.MergeJobStatus;
import com.thedal.thedal_app.merge.MergeField;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "merge_jobs")
@Getter
@Setter
@NoArgsConstructor
public class MergeJobEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "source_election_id", nullable = false)
    private Long sourceElectionId;

    @Column(name = "target_election_id", nullable = false)
    private Long targetElectionId;

    @ElementCollection
    @CollectionTable(name = "merge_job_fields", joinColumns = @JoinColumn(name = "job_id"))
    @Column(name = "field")
    @Enumerated(EnumType.STRING)
    private List<MergeField> fields;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private MergeJobStatus status = MergeJobStatus.PENDING;

    @Column(name = "total_voters")
    private Long totalVoters; // voters matched by EPIC

    @Column(name = "processed_voters")
    private Long processedVoters = 0L;

    @Column(name = "result_stats", columnDefinition = "TEXT")
    private String resultStatsJson; // JSON blob of final stats (stored as TEXT)

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;
}
