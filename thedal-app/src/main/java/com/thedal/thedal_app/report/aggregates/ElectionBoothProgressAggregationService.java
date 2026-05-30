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
public class ElectionBoothProgressAggregationService {

    private final NamedParameterJdbcTemplate primaryJdbcTemplate;
    private final ElectionDashboardBoothProgressRepository repo;

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
            log.error("[BOOTH] failure enumerating elections", ex);
        }
    }

    @Transactional
    public void forceAggregate(Long accountId, Long electionId) {
        try {
            aggregateOne(accountId, electionId);
        } catch (Exception ex) {
            log.error("[BOOTH] manual trigger failed account={} election={}", accountId, electionId, ex);
            throw ex;
        }
    }

    private void aggregateOne(Long accountId, Long electionId) {
        String sql = "SELECT booth_number AS b, COUNT(*) total, SUM(CASE WHEN has_voted THEN 1 ELSE 0 END) voted " +
                "FROM _voters v WHERE v.account_id=:a AND v.election_id=:e GROUP BY booth_number";
        List<Map<String, Object>> rows = primaryJdbcTemplate.queryForList(sql, Map.of("a", accountId, "e", electionId));
        String json = rows.stream()
                .map(r -> { String booth = String.valueOf(r.get("b")); long total = ((Number) r.get("total")).longValue(); long voted = ((Number) r.get("voted")).longValue();
                    return "\"" + escape(booth) + "\":{\"total\":" + total + ",\"voted\":" + voted + "}"; })
                .collect(Collectors.joining(","));
        json = "{" + json + "}";

        OffsetDateTime now = OffsetDateTime.now();
        Optional<ElectionDashboardBoothProgress> existing = repo.findByAccountIdAndElectionId(accountId, electionId);
        ElectionDashboardBoothProgress bp = existing.orElseGet(ElectionDashboardBoothProgress::new);
        bp.setAccountId(accountId);
        bp.setElectionId(electionId);
        bp.setBoothProgressJson(json);
        if (bp.getComputedAt() == null) bp.setComputedAt(now);
        bp.setRefreshedAt(now);
        repo.save(bp);
        log.debug("[BOOTH] updated account={} election={} booths={}", accountId, electionId, rows.size());
    }

    private String escape(String in) { if (in == null) return "unknown"; return in.replace("\\", "\\\\").replace("\"", "\\\""); }
}