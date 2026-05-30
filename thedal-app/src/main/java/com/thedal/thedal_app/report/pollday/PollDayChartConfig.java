package com.thedal.thedal_app.report.pollday;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.OffsetDateTime;

@Entity
@Table(name = "poll_day_chart_configs",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_poll_day_chart_config_account_election", 
                         columnNames = {"account_id", "election_id"})
    }
)
@Getter
@Setter
public class PollDayChartConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "account_id", nullable = false)
    private Long accountId;
    
    @Column(name = "election_id", nullable = false)
    private Long electionId;
    
    /**
     * JSON structure for chart configurations:
     * [
     *   {
     *     "chartId": "1697123456789",
     *     "selectedParts": [1, 2, 3]
     *   },
     *   {
     *     "chartId": "1697123456790",
     *     "selectedParts": []
     *   }
     * ]
     */
    @Column(name = "charts", columnDefinition = "JSONB NOT NULL")
    @JdbcTypeCode(SqlTypes.JSON)
    private String charts;
    
    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
