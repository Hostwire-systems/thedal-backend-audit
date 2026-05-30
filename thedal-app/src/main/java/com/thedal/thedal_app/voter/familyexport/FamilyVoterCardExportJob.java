package com.thedal.thedal_app.voter.familyexport;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "family_voter_card_export_job", indexes = {
        @Index(name = "idx_family_export_account", columnList = "account_id, election_id, family_id"),
        @Index(name = "idx_family_export_status", columnList = "status, created_at DESC")
})
@Getter
@Setter
@NoArgsConstructor
public class FamilyVoterCardExportJob {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "election_id", nullable = false)
    private Long electionId;

    @Column(name = "family_id", nullable = false)
    private UUID familyId;

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

    // Optional parameters
    @Column(name = "part_no")
    private Integer partNo; // when provided, export all families in this part instead of single familyId

    @Column(name = "columns")
    private Integer columns = 2; // number of columns in PDF layout (2 or 3)

    @Column(name = "order_by", length = 20)
    private String orderBy = "family"; // ordering preference: "family" (by family_sequence_number) or "serial" (by serialNo)

    @Enumerated(EnumType.STRING)
    @Column(name = "export_type", nullable = false, length = 20)
    private ExportType exportType = ExportType.PDF; // type of export: PDF or EXCEL

    public enum Status { PENDING, RUNNING, COMPLETED, FAILED }
    public enum ExportType { PDF, EXCEL }
}
