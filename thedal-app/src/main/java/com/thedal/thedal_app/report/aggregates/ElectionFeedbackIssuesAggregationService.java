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
public class ElectionFeedbackIssuesAggregationService {

    private final NamedParameterJdbcTemplate primaryJdbcTemplate;
    private final ElectionDashboardFeedbackIssuesRepository repo;

    private static final String DISTINCT_ELECTIONS_SQL = "SELECT DISTINCT account_id, election_id FROM feedback_issues";

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
            log.error("[ISSUES] failure enumerating elections", ex);
        }
    }

    @Transactional
    public void forceAggregate(Long accountId, Long electionId) {
        try { aggregateOne(accountId, electionId); } catch (Exception ex) {
            log.error("[ISSUES] manual trigger failed account={} election={}", accountId, electionId, ex); throw ex; }
    }

    private void aggregateOne(Long accountId, Long electionId) {
        String sql = "SELECT fi.issue_name issue, COUNT(vfi.voter_id) c FROM feedback_issues fi " +
                "LEFT JOIN voter_feedback_issues vfi ON fi.id=vfi.feedback_issue_id " +
                "WHERE fi.account_id=:a AND fi.election_id=:e GROUP BY fi.issue_name";
        List<Map<String, Object>> rows = primaryJdbcTemplate.queryForList(sql, Map.of("a", accountId, "e", electionId));
        String json = rows.stream()
                .map(r -> "\"" + escape(String.valueOf(r.get("issue"))) + "\":" + ((Number) r.get("c")).longValue())
                .collect(Collectors.joining(","));
        json = "{" + json + "}";

        OffsetDateTime now = OffsetDateTime.now();
        Optional<ElectionDashboardFeedbackIssues> existing = repo.findByAccountIdAndElectionId(accountId, electionId);
        ElectionDashboardFeedbackIssues fi = existing.orElseGet(ElectionDashboardFeedbackIssues::new);
        fi.setAccountId(accountId);
        fi.setElectionId(electionId);
        fi.setIssuesJson(json);
        if (fi.getComputedAt() == null) fi.setComputedAt(now);
        fi.setRefreshedAt(now);
        repo.save(fi);
        log.debug("[ISSUES] updated account={} election={} issues={}", accountId, electionId, rows.size());
    }

    private String escape(String in) { if (in == null) return "unknown"; return in.replace("\\", "\\\\").replace("\"", "\\\""); }
}