package com.thedal.thedal_app.report.pollday.export;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "poll_day_export_job")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PollDayExportJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "election_id", nullable = false)
    private Long electionId;

    @Column(name = "format", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private ExportFormat format;

    @Column(name = "chart_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ChartType chartType;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ExportStatus status = ExportStatus.PENDING;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "selected_parts", nullable = false, columnDefinition = "jsonb")
    private int[] selectedParts;

    @Column(name = "polling_date")
    private LocalDate pollingDate;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "filters", nullable = false, columnDefinition = "jsonb")
    private ExportFilters filters;

    @Column(name = "s3_url")
    private String s3Url;

    @Column(name = "row_count")
    private Integer rowCount;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = ExportStatus.PENDING;
        }
    }

    public enum ExportFormat {
        excel, pdf
    }

    public enum ChartType {
        voterCount, familyCount
    }

    public enum ExportStatus {
        PENDING, RUNNING, COMPLETED, FAILED
    }
}
