package com.thedal.thedal_app.voter.duplicate;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "duplicate_voter_export_job", indexes = {
        @Index(name = "idx_dup_export_run", columnList = "run_id"),
        @Index(name = "idx_dup_export_status", columnList = "status, created_at DESC")
})
@Getter
@Setter
@NoArgsConstructor
public class DuplicateVoterExportJob {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "run_id", nullable = false)
    private Long runId;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "election_id", nullable = false)
    private Long electionId;

    @Column(name = "part_no")
    private Integer partNo; // null = all parts

    // Optional CSV of multiple parts to export in a single sheet; takes precedence over partNo when present
    @Column(name = "part_nos")
    private String partNos;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status = Status.PENDING;

    @Column(name = "s3_key")
    private String s3Key;

    @Column(name = "s3_url")
    private String s3Url;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "row_count")
    private Long rowCount;

    public enum Status { PENDING, RUNNING, COMPLETED, FAILED }

    // Convenience getters
    public boolean hasMultipleParts() {
        return partNos != null && !partNos.trim().isEmpty();
    }

    public java.util.Set<Integer> parsedPartNos() {
        if (!hasMultipleParts()) return java.util.Collections.emptySet();
        java.util.Set<Integer> set = new java.util.LinkedHashSet<>();
        for (String s : partNos.split(",")) {
            String t = s.trim();
            if (t.isEmpty()) continue;
            try { set.add(Integer.parseInt(t)); } catch (NumberFormatException ignored) {}
        }
        return set;
    }
}
