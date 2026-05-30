package com.thedal.thedal_app.report.aggregates;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.OffsetDateTime;

@Entity
@Table(name = "election_dashboard_feedback_issues", uniqueConstraints = @UniqueConstraint(columnNames = {"account_id", "election_id"}))
@Getter
@Setter
@ToString
public class ElectionDashboardFeedbackIssues {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "election_id", nullable = false)
    private Long electionId;

    @Column(name = "issues_json", columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private String issuesJson;

    @Column(name = "computed_at", nullable = false)
    private OffsetDateTime computedAt;

    @Column(name = "refreshed_at", nullable = false)
    private OffsetDateTime refreshedAt;
}