package com.thedal.thedal_app.report.aggregates;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.OffsetDateTime;

@Entity
@Table(name = "election_dashboard_party_polling", uniqueConstraints = @UniqueConstraint(columnNames = {"account_id", "election_id", "part_no"}))
@Getter
@Setter
@ToString
public class ElectionDashboardPartyPolling {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "election_id", nullable = false)
    private Long electionId;

    @Column(name = "part_no")
    private String partNo;

    @Column(name = "party_counts_json", columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private String partyCountsJson;

    @Column(name = "computed_at", nullable = false)
    private OffsetDateTime computedAt;

    @Column(name = "refreshed_at", nullable = false)
    private OffsetDateTime refreshedAt;
}