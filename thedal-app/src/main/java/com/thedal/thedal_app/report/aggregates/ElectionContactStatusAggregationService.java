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
public class ElectionContactStatusAggregationService {

    private final NamedParameterJdbcTemplate primaryJdbcTemplate;
    private final ElectionDashboardContactStatusRepository repo;

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
            log.error("[CONTACT] failure enumerating elections", ex);
        }
    }

    @Transactional
    public void forceAggregate(Long accountId, Long electionId) {
        try { aggregateOne(accountId, electionId); } catch (Exception ex) {
            log.error("[CONTACT] manual trigger failed account={} election={}", accountId, electionId, ex); throw ex; }
    }

    private void aggregateOne(Long accountId, Long electionId) {
        // Build counts for verification/contact related flags.
        // We'll produce keys: has_mobile, no_mobile, mobile_verified_true, mobile_verified_false,
        // aadhaar_verified_true/false, member_verified_true/false
        String sql = "SELECT " +
                "SUM(CASE WHEN COALESCE(mobile_no,'')<>'' THEN 1 ELSE 0 END) AS has_mobile, " +
                "SUM(CASE WHEN COALESCE(mobile_no,'')='' THEN 1 ELSE 0 END) AS no_mobile, " +
                "SUM(CASE WHEN COALESCE(mobile_verified,false)=true THEN 1 ELSE 0 END) AS mobile_verified_true, " +
                "SUM(CASE WHEN COALESCE(mobile_verified,false)=false THEN 1 ELSE 0 END) AS mobile_verified_false, " +
                "SUM(CASE WHEN COALESCE(aadhaar_verified,false)=true THEN 1 ELSE 0 END) AS aadhaar_verified_true, " +
                "SUM(CASE WHEN COALESCE(aadhaar_verified,false)=false THEN 1 ELSE 0 END) AS aadhaar_verified_false, " +
                "SUM(CASE WHEN COALESCE(member_verified,false)=true THEN 1 ELSE 0 END) AS member_verified_true, " +
                "SUM(CASE WHEN COALESCE(member_verified,false)=false THEN 1 ELSE 0 END) AS member_verified_false " +
                "FROM _voters v WHERE v.account_id=:a AND v.election_id=:e";

        Map<String, Object> row = primaryJdbcTemplate.queryForMap(sql, Map.of("a", accountId, "e", electionId));
        String json = row.entrySet().stream()
                .map(e -> "\"" + e.getKey() + "\":" + ((Number) e.getValue()).longValue())
                .collect(Collectors.joining(","));
        json = "{" + json + "}";

        OffsetDateTime now = OffsetDateTime.now();
        Optional<ElectionDashboardContactStatus> existing = repo.findByAccountIdAndElectionId(accountId, electionId);
        ElectionDashboardContactStatus cs = existing.orElseGet(ElectionDashboardContactStatus::new);
        cs.setAccountId(accountId);
        cs.setElectionId(electionId);
        cs.setContactStatusJson(json);
        if (cs.getComputedAt() == null) cs.setComputedAt(now);
        cs.setRefreshedAt(now);
        repo.save(cs);
        log.debug("[CONTACT] updated account={} election={} json={}", accountId, electionId, json);
    }
}
