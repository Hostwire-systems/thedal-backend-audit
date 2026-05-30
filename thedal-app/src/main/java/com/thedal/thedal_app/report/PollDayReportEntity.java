package com.thedal.thedal_app.report;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "poll_day_reports")
public class PollDayReportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long electionId;

    @Column(nullable = false)
    private Long accountId;

    @Column
    private String boothNumber; // Optional field

    @Column
    private String activeTab; // Expected values: "vote", "performance", "demographics", "timing"

    @ElementCollection
    @CollectionTable(name = "poll_day_report_votes", joinColumns = @JoinColumn(name = "report_id"))
    @Column(name = "vote")
    private List<String> vote;

    @ElementCollection
    @CollectionTable(name = "poll_day_report_performances", joinColumns = @JoinColumn(name = "report_id"))
    @Column(name = "performance")
    private List<String> performance;

    @ElementCollection
    @CollectionTable(name = "poll_day_report_demographics", joinColumns = @JoinColumn(name = "report_id"))
    @Column(name = "demographic")
    private List<String> demographics;

    @ElementCollection
    @CollectionTable(name = "poll_day_report_timings", joinColumns = @JoinColumn(name = "report_id"))
    @Column(name = "timing")
    private List<String> timing;

    @Column
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}