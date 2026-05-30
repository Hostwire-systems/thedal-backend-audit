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
@Table(name = "cadre_reports")
public class CadreReportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long electionId;

    @Column(nullable = false)
    private Long accountId;

    @Column
    private String activeTab;

    @ElementCollection
    @CollectionTable(name = "cadre_report_top_performers", joinColumns = @JoinColumn(name = "report_id"))
    @Column(name = "top_performer")
    private List<String> topPerformers;

    @ElementCollection
    @CollectionTable(name = "cadre_report_low_performers", joinColumns = @JoinColumn(name = "report_id"))
    @Column(name = "low_performer")
    private List<String> lowPerformers;

    @ElementCollection
    @CollectionTable(name = "cadre_report_activities", joinColumns = @JoinColumn(name = "report_id"))
    @Column(name = "activity")
    private List<String> activity;

    @ElementCollection
    @CollectionTable(name = "cadre_report_demographics", joinColumns = @JoinColumn(name = "report_id"))
    @Column(name = "demographic")
    private List<String> demographics;

    @ElementCollection
    @CollectionTable(name = "cadre_report_updates", joinColumns = @JoinColumn(name = "report_id"))
    @Column(name = "update")
    private List<String> updates;

    @Column
    private Integer selectedBooth; // Optional field

    @Column
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}