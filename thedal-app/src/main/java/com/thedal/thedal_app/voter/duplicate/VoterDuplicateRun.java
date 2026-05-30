package com.thedal.thedal_app.voter.duplicate;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "voter_duplicate_run", indexes = {
    @Index(name = "idx_dup_run_account_election_started", columnList = "account_id, election_id, started_at DESC"),
    @Index(name = "idx_dup_run_scope_status", columnList = "scope, status, started_at DESC")
})
@Getter
@Setter
@NoArgsConstructor
public class VoterDuplicateRun {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "election_id", nullable = false)
    private Long electionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false)
    private Scope scope; // BATCH or ELECTION

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status; // PENDING, RUNNING, COMPLETED, FAILED

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt = LocalDateTime.now();

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "triggered_by")
    private Long triggeredBy; // user id (optional)

    @Column(name = "bulk_upload_id")
    private Long bulkUploadId; // when scope=BATCH

    @Column(name = "cooldown_until")
    private LocalDateTime cooldownUntil;

    public enum Scope { BATCH, ELECTION }
    public enum Status { PENDING, RUNNING, COMPLETED, FAILED }
}
