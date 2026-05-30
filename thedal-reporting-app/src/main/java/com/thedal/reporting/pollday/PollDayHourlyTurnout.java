package com.thedal.reporting.pollday;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.OffsetDateTime;
import java.time.LocalDate;

@Entity
@Table(name = "poll_day_hourly_turnout", uniqueConstraints = @UniqueConstraint(columnNames = {"account_id","election_id","polling_date"}))
@Getter
@Setter
public class PollDayHourlyTurnout {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "account_id", nullable = false) private Long accountId;
    @Column(name = "election_id", nullable = false) private Long electionId;
    @Column(name = "polling_date", nullable = false) private LocalDate pollingDate;
    @Column(name = "hourly_json", columnDefinition = "JSONB NOT NULL DEFAULT '{}'::jsonb") private String hourlyJson;
    @Column(name = "computed_at", columnDefinition = "TIMESTAMPTZ") private OffsetDateTime computedAt;
    @Column(name = "refreshed_at", columnDefinition = "TIMESTAMPTZ") private OffsetDateTime refreshedAt;
}
