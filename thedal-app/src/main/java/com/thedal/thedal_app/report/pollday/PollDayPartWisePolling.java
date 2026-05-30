package com.thedal.thedal_app.report.pollday;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.OffsetDateTime;
import java.time.LocalDate;

@Entity
@Table(name = "poll_day_part_wise_polling")
@Getter
@Setter
public class PollDayPartWisePolling {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "account_id", nullable = false) 
    private Long accountId;
    
    @Column(name = "election_id", nullable = false) 
    private Long electionId;
    
    @Column(name = "polling_date", nullable = false) 
    private LocalDate pollingDate;
    
    /**
     * JSON structure for part-wise polling data:
     * {
     *   "parts": [
     *     {
     *       "partNumber": 1,
     *       "totalVoters": 6500,
     *       "polled2025": 4200,
     *       "polled2024": 3800,
     *       "didNotVote": 2300,
     *       "turnoutPercentage": 64.62,
     *       "lastUpdated": "2025-10-14T10:30:00Z"
     *     }
     *   ],
     *   "summary": {
     *     "totalParts": 4,
     *     "totalVoters": 25600,
     *     "totalPolled2025": 17500,
     *     "totalPolled2024": 15600,
     *     "overallTurnoutPercentage": 68.36,
     *     "computedAt": "2025-10-14T10:30:00Z"
     *   }
     * }
     */
    @Column(name = "part_wise_data_json", columnDefinition = "JSONB NOT NULL DEFAULT '{}'::jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String partWiseDataJson;
    
    @Column(name = "computed_at", columnDefinition = "TIMESTAMPTZ") 
    private OffsetDateTime computedAt;
    
    @Column(name = "refreshed_at", columnDefinition = "TIMESTAMPTZ") 
    private OffsetDateTime refreshedAt;
}
