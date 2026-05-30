package com.thedal.thedal_app.report;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Entity
@Table(name = "polling_age_wise")
public class PollingAgeWiseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long electionId;

    @Column(nullable = false)
    private Long accountId;

    @Column(nullable = false)
    private Integer voteCountType;

    private long ageGroup18To30;

    private long ageGroup30To40;

    private long ageGroup40To50;

    private long ageGroup50To60;

    private long ageGroup60To70;

    private long overallPolledCount;

    @Column(nullable = false)
    private LocalDateTime timestamp;
}