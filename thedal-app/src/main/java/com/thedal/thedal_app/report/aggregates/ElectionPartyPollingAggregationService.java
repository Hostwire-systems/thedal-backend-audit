package com.thedal.thedal_app.report.aggregates;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ElectionPartyPollingAggregationService {

    private final NamedParameterJdbcTemplate primaryJdbcTemplate;
    private final ElectionDashboardPartyPollingRepository repo;

    private static final String DISTINCT_ELECTIONS_SQL = "SELECT DISTINCT account_id, election_id FROM _voters";

    @Transactional
    public void aggregateAll() {
        try {
            List<Map<String, Object>> elections = primaryJdbcTemplate.queryForList(DISTINCT_ELECTIONS_SQL, Map.of());
            for (Map<String, Object> row : elections) {
                Long accountId = ((Number) row.get("account_id")).longValue();
                Long electionId = ((Number) row.get("election_id")).longValue();
                aggregateOne(accountId, electionId);
            }
        } catch (Exception ex) {
            log.error("[PARTY] failure enumerating elections", ex);
        }
    }

    @Transactional
    public void forceAggregate(Long accountId, Long electionId) {
        try {
            aggregateOne(accountId, electionId, null);
        } catch (Exception ex) {
            log.error("[PARTY] manual trigger failed account={} election={}", accountId, electionId, ex);
            throw ex;
        }
    }

    @Transactional
    public void forceAggregate(Long accountId, Long electionId, String partNumber) {
        if (partNumber != null) {
            // Compute only this part
            aggregateOne(accountId, electionId, partNumber);
        } else {
            // Fetch part numbers OUTSIDE transaction to avoid rollback issues
            List<String> allParts = getPartNumbersForElection(accountId, electionId);
            
            // Compute election-wide in separate transaction
            try {
                aggregateOne(accountId, electionId, null);
            } catch (Exception ex) {
                log.error("[PARTY_POLLING] manual trigger failed account={} election={} part=null", accountId, electionId, ex);
            }
            
            // Compute each part in separate try-catch to allow partial success
            for (String partNo : allParts) {
                try {
                    aggregateOne(accountId, electionId, partNo);
                } catch (Exception ex) {
                    log.error("[PARTY_POLLING] failed for account={} election={} part={}", accountId, electionId, partNo, ex);
                    // Continue processing other parts
                }
            }
        }
    }

    private void aggregateOne(Long accountId, Long electionId) {
        aggregateOne(accountId, electionId, null);
    }

    private void aggregateOne(Long accountId, Long electionId, String partNumber) {
        String partFilter = partNumber != null ? " AND v.part_no=CAST(:p AS INTEGER)" : "";
        Map<String, Object> params = partNumber != null ?
            Map.of("a", accountId, "e", electionId, "p", partNumber) :
            Map.of("a", accountId, "e", electionId);

        String sql = "SELECT COALESCE(CAST(party_id AS TEXT),'unknown') pid, COUNT(*) c FROM _voters v " +
                "WHERE v.account_id=:a AND v.election_id=:e" + partFilter + " GROUP BY 1";
        List<Map<String, Object>> rows = primaryJdbcTemplate.queryForList(sql, params);
        String json = rows.stream()
                .map(r -> "\"" + escape(String.valueOf(r.get("pid"))) + "\":" + ((Number) r.get("c")).longValue())
                .collect(Collectors.joining(","));
        json = "{" + json + "}";

        OffsetDateTime now = OffsetDateTime.now();
        Optional<ElectionDashboardPartyPolling> existing = partNumber != null ?
            repo.findByAccountIdAndElectionIdAndPartNo(accountId, electionId, partNumber) :
            repo.findByAccountIdAndElectionId(accountId, electionId);
        ElectionDashboardPartyPolling pp = existing.orElseGet(ElectionDashboardPartyPolling::new);
        pp.setAccountId(accountId);
        pp.setElectionId(electionId);
        pp.setPartNo(partNumber);
        pp.setPartyCountsJson(json);
        if (pp.getComputedAt() == null) pp.setComputedAt(now);
        pp.setRefreshedAt(now);
        repo.save(pp);
        log.debug("[PARTY] updated account={} election={} part={} parties={}", accountId, electionId, partNumber, rows.size());
    }

    private List<String> getPartNumbersForElection(Long accountId, Long electionId) {
        // Only fetch numeric part numbers since we need to CAST to INTEGER for _voters.part_no comparison
        String sql = "SELECT DISTINCT part_no FROM part_manager WHERE account_id=:a AND election_id=:e AND part_no IS NOT NULL AND part_no ~ '^\\d+$' ORDER BY part_no";
        return primaryJdbcTemplate.queryForList(sql, Map.of("a", accountId, "e", electionId), String.class);
    }

    private String escape(String in) { if (in == null) return "unknown"; return in.replace("\\", "\\\\").replace("\"", "\\\""); }
}