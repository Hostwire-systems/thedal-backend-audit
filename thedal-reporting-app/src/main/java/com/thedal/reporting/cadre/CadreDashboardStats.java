package com.thedal.reporting.cadre;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.OffsetDateTime;

@Entity
@Table(name = "cadre_dashboard_stats", uniqueConstraints = {
        @UniqueConstraint(name = "uq_cadre_dashboard_stats", columnNames = {"account_id", "election_id"})
})
@Getter
@Setter
public class CadreDashboardStats {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "election_id", nullable = false)
    private Long electionId;

    @Column(name = "total_cadres", nullable = false)
    private Integer totalCadres = 0;

    @Column(name = "cadres_logged", nullable = false)
    private Integer cadresLogged = 0;

    @Column(name = "cadres_not_logged", nullable = false)
    private Integer cadresNotLogged = 0;

    @Column(name = "booths_assigned", nullable = false)
    private Integer boothsAssigned = 0;

    @Column(name = "total_mobile_updated", nullable = false)
    private Integer totalMobileUpdated = 0;
    @Column(name = "total_dob_updated", nullable = false)
    private Integer totalDobUpdated = 0;
    @Column(name = "total_party_updated", nullable = false)
    private Integer totalPartyUpdated = 0;
    @Column(name = "total_caste_updated", nullable = false)
    private Integer totalCasteUpdated = 0;
    @Column(name = "total_religion_updated", nullable = false)
    private Integer totalReligionUpdated = 0;
    @Column(name = "total_language_updated", nullable = false)
    private Integer totalLanguageUpdated = 0;

    @Column(name = "top_10_cadres", columnDefinition = "jsonb")
    private String top10Cadres = "[]";

    @Column(name = "least_10_cadres", columnDefinition = "jsonb")
    private String least10Cadres = "[]";

    @Column(name = "computed_at", nullable = false)
    private OffsetDateTime computedAt = OffsetDateTime.now();

    @Column(name = "refreshed_at", nullable = false)
    private OffsetDateTime refreshedAt = OffsetDateTime.now();
}
