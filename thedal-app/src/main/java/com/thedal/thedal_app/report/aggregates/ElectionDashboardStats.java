package com.thedal.thedal_app.report.aggregates;

import java.time.OffsetDateTime;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "election_dashboard_stats", uniqueConstraints = {
        @UniqueConstraint(name = "uq_election_dashboard_stats", columnNames = {"account_id", "election_id", "part_no"})
})
@Getter
@Setter
@NoArgsConstructor
public class ElectionDashboardStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "election_id", nullable = false)
    private Long electionId;

    @Column(name = "part_no")
    private String partNo;

    @Column(name = "total_booth", nullable = false)
    private int totalBooth;
    @Column(name = "total_voters", nullable = false)
    private int totalVoters;
    @Column(name = "total_family", nullable = false)
    private int totalFamily;
    @Column(name = "distinct_pincode_count", nullable = false)
    private int distinctPincodeCount;
    @Column(name = "distinct_mobile_count", nullable = false)
    private int distinctMobileCount;

    @Column(nullable = false)
    private int male;
    @Column(nullable = false)
    private int female;
    @Column(nullable = false)
    private int transgender;

    @Column(name = "age_18_30", nullable = false)
    private int age18To30;
    @Column(name = "age_30_40", nullable = false)
    private int age30To40;
    @Column(name = "age_40_50", nullable = false)
    private int age40To50;
    @Column(name = "age_50_60", nullable = false)
    private int age50To60;
    @Column(name = "age_60_70", nullable = false)
    private int age60To70;
    @Column(name = "age_gt_70", nullable = false)
    private int ageGreaterThan70;

    @Column(name = "first_time_voters", nullable = false)
    private int firstTimeVoters;
    @Column(name = "senior_citizens", nullable = false)
    private int seniorCitizens;
    @Column(name = "super_seniors", nullable = false)
    private int superSeniors;

    // New fields
    @Column(name = "date_of_birth_count", nullable = false)
    private int dateOfBirth;
    @Column(name = "star_voters_count", nullable = false)
    private int starVoters;
    @Column(name = "religion_count", nullable = false)
    private int religionCount;
    @Column(name = "caste_count", nullable = false)
    private int casteCount;
    @Column(name = "total_mobile_count", nullable = false)
    private int totalMobileCount;
    @Column(name = "male_mobile_count", nullable = false)
    private int maleMobileCount;
    @Column(name = "female_mobile_count", nullable = false)
    private int femaleMobileCount;
    @Column(name = "transgender_mobile_count", nullable = false)
    private int transgenderMobileCount;
    @Column(name = "male_date_of_birth_count", nullable = false)
    private int maleDateOfBirthCount;
    @Column(name = "female_date_of_birth_count", nullable = false)
    private int femaleDateOfBirthCount;
    @Column(name = "transgender_date_of_birth_count", nullable = false)
    private int transgenderDateOfBirthCount;

    // Additional aggregate fields
    @Column(name = "total_school", nullable = false)
    private int totalSchool;
    @Column(name = "cross_booth_family", nullable = false)
    private int crossBoothFamily;
    @Column(name = "one_voter_family", nullable = false)
    private int oneVoterFamily;
    @Column(name = "caste_category_count", nullable = false)
    private int casteCategoryCount;
    @Column(name = "sub_caste_count", nullable = false)
    private int subCasteCount;
    @Column(name = "language_count", nullable = false)
    private int languageCount;
    @Column(name = "party_affiliation_count", nullable = false)
    private int partyAffiliationCount;
    @Column(name = "schemes_count", nullable = false)
    private int schemesCount;

    @Column(name = "computed_at", nullable = false)
    private OffsetDateTime computedAt;

    @Column(name = "refreshed_at", nullable = false)
    private OffsetDateTime refreshedAt;
}
