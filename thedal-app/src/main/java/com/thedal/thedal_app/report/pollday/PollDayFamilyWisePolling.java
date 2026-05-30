package com.thedal.thedal_app.report.pollday;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "poll_day_family_wise_polling", uniqueConstraints = {
    @UniqueConstraint(name = "uq_poll_day_family_wise", 
                     columnNames = {"account_id", "election_id", "polling_date"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PollDayFamilyWisePolling {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "account_id", nullable = false)
    private Long accountId;
    
    @Column(name = "election_id", nullable = false)
    private Long electionId;
    
    @Column(name = "polling_date", nullable = false)
    private LocalDate pollingDate;
    
    @Column(name = "family_wise_data_json", columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private String familyWiseDataJson;
    
    @Column(name = "computed_at")
    private OffsetDateTime computedAt;
    
    @Column(name = "refreshed_at")
    private OffsetDateTime refreshedAt;
}
