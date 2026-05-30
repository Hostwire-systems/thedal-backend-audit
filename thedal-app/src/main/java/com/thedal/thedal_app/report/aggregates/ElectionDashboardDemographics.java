package com.thedal.thedal_app.report.aggregates;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.OffsetDateTime;

@Entity
@Table(name = "election_dashboard_demographics", uniqueConstraints = @UniqueConstraint(columnNames = {"account_id", "election_id", "part_no"}))
@Getter
@Setter
@ToString
public class ElectionDashboardDemographics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "election_id", nullable = false)
    private Long electionId;

    @Column(name = "part_no")
    private String partNo;

    @Column(name = "caste_category_json", columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private String casteCategoryJson;

    @Column(name = "caste_json", columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private String casteJson;

    @Column(name = "sub_caste_json", columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private String subCasteJson;

    @Column(name = "religion_json", columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private String religionJson;

    @Column(name = "language_json", columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private String languageJson;

    @Column(name = "availability_json", columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private String availabilityJson;

    @Column(name = "schemes_json", columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private String schemesJson;

    @Column(name = "relation_json", columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private String relationJson;

    @Column(name = "computed_at", nullable = false)
    private OffsetDateTime computedAt;

    @Column(name = "refreshed_at", nullable = false)
    private OffsetDateTime refreshedAt;
}