package com.thedal.reporting.pollday;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.OffsetDateTime;
import java.time.LocalDate;

@Entity
@Table(name = "poll_day_ward_age_group_turnout", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"account_id","election_id","part_number","polling_date"}))
@Getter
@Setter
public class PollDayWardAgeGroupTurnout {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "account_id", nullable = false) 
    private Long accountId;
    
    @Column(name = "election_id", nullable = false) 
    private Long electionId;
    
    @Column(name = "part_number", nullable = false) 
    private String partNumber;
    
    @Column(name = "polling_date", nullable = false) 
    private LocalDate pollingDate;
    
    /**
     * JSON structure for age group breakdown with historical comparison:
     * {
     *   "18_21": {
     *     "total_voters": 150,
     *     "polled_2024": 120,
     *     "polled_2025": 135,
     *     "did_not_vote": 15,
     *     "is_first_time": true
     *   },
     *   "22_25": {...},
     *   "26_35": {...},
     *   "36_45": {...},
     *   "46_59": {...},
     *   "expired": {...},
     *   "overall": {
     *     "total_voters": 1500,
     *     "polled_2025": 1125,
     *     "percentage": 75.0
     *   }
     * }
     */
    @Column(name = "age_group_breakdown_json", columnDefinition = "JSONB NOT NULL DEFAULT '{}'::jsonb") 
    private String ageGroupBreakdownJson;
    
    @Column(name = "computed_at", columnDefinition = "TIMESTAMPTZ") 
    private OffsetDateTime computedAt;
    
    @Column(name = "refreshed_at", columnDefinition = "TIMESTAMPTZ") 
    private OffsetDateTime refreshedAt;
}
