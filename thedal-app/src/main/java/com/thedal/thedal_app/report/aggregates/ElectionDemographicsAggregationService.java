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
public class ElectionDemographicsAggregationService {

    private final NamedParameterJdbcTemplate primaryJdbcTemplate;
    private final ElectionDashboardDemographicsRepository repo;

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
            log.error("[DEMOGRAPHICS] failure enumerating elections", ex);
        }
    }

    @Transactional
    public void forceAggregate(Long accountId, Long electionId) {
        try {
            aggregateOne(accountId, electionId, null);
        } catch (Exception ex) {
            log.error("[DEMOGRAPHICS] manual trigger failed account={} election={}", accountId, electionId, ex);
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
                log.error("[DEMOGRAPHICS] manual trigger failed account={} election={} part=null", accountId, electionId, ex);
            }
            
            // Compute each part in separate try-catch to allow partial success
            for (String partNo : allParts) {
                try {
                    aggregateOne(accountId, electionId, partNo);
                } catch (Exception ex) {
                    log.error("[DEMOGRAPHICS] failed for account={} election={} part={}", accountId, electionId, partNo, ex);
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

        String casteCategoryJson = buildJson("SELECT COALESCE(lower(cc.caste_category_name),'unknown') AS k, COUNT(DISTINCT v.id) c FROM _voters v LEFT JOIN caste_category cc ON v.caste_category_id=cc.id WHERE v.account_id=:a AND v.election_id=:e" + partFilter + " GROUP BY 1", params);
        String casteJson = buildJson("SELECT COALESCE(lower(c.caste_name),'unknown') AS k, COUNT(DISTINCT v.id) c FROM _voters v LEFT JOIN caste c ON v.caste_id=c.id WHERE v.account_id=:a AND v.election_id=:e" + partFilter + " GROUP BY 1", params);
        String subCasteJson = buildJson("SELECT COALESCE(lower(sc.sub_caste_name),'unknown') AS k, COUNT(DISTINCT v.id) c FROM _voters v LEFT JOIN sub_caste sc ON v.sub_caste_id=sc.id WHERE v.account_id=:a AND v.election_id=:e" + partFilter + " GROUP BY 1", params);
        String religionJson = buildJson("SELECT COALESCE(lower(r.religion_name),'unknown') AS k, COUNT(DISTINCT v.id) c FROM _voters v LEFT JOIN religion r ON v.religion_id=r.id WHERE v.account_id=:a AND v.election_id=:e" + partFilter + " GROUP BY 1", params);
        String languageJson = buildJson("SELECT COALESCE(lower(l.language_name),'unknown') AS k, COUNT(DISTINCT v.id) c FROM _voters v LEFT JOIN voter_language vl ON v.id=vl.voter_id LEFT JOIN language l ON vl.language_id=l.id WHERE v.account_id=:a AND v.election_id=:e" + partFilter + " GROUP BY 1", params);
        String availabilityJson = buildJson("SELECT COALESCE(lower(av.category_name),'unknown') AS k, COUNT(DISTINCT v.id) c FROM _voters v LEFT JOIN availability av ON v.availability_id=av.id WHERE v.account_id=:a AND v.election_id=:e" + partFilter + " GROUP BY 1", params);
        String schemesJson = buildJson("SELECT COALESCE(lower(bs.scheme_name),'unknown') AS k, COUNT(DISTINCT vbs.voter_id) c FROM _voters v LEFT JOIN voter_benefit_schemes vbs ON v.id=vbs.voter_id LEFT JOIN benefit_schemes bs ON vbs.benefit_scheme_id=bs.id WHERE v.account_id=:a AND v.election_id=:e" + partFilter + " GROUP BY 1", params);
        String relationJson = buildJson("SELECT COALESCE(lower(v.rln_type),'unknown') AS k, COUNT(*) c FROM _voters v WHERE v.account_id=:a AND v.election_id=:e" + partFilter + " GROUP BY 1", params);

        OffsetDateTime now = OffsetDateTime.now();
        Optional<ElectionDashboardDemographics> existing = partNumber != null ?
            repo.findByAccountIdAndElectionIdAndPartNo(accountId, electionId, partNumber) :
            repo.findByAccountIdAndElectionId(accountId, electionId);
        ElectionDashboardDemographics d = existing.orElseGet(ElectionDashboardDemographics::new);
        d.setAccountId(accountId);
        d.setElectionId(electionId);
        d.setPartNo(partNumber);
        d.setCasteCategoryJson(casteCategoryJson);
        d.setCasteJson(casteJson);
        d.setSubCasteJson(subCasteJson);
        d.setReligionJson(religionJson);
        d.setLanguageJson(languageJson);
        d.setAvailabilityJson(availabilityJson);
        d.setSchemesJson(schemesJson);
        d.setRelationJson(relationJson);
        if (d.getComputedAt() == null) {
            d.setComputedAt(now);
        }
        d.setRefreshedAt(now);
        repo.save(d);
        log.debug("[DEMOGRAPHICS] updated account={} election={} part={} casteKeys={} religionKeys={}", 
            accountId, electionId, partNumber, casteJson.length(), religionJson.length());
    }

    private List<String> getPartNumbersForElection(Long accountId, Long electionId) {
        // Only fetch numeric part numbers since we need to CAST to INTEGER for _voters.part_no comparison
        String sql = "SELECT DISTINCT part_no FROM part_manager WHERE account_id=:a AND election_id=:e AND part_no IS NOT NULL AND part_no ~ '^\\d+$' ORDER BY part_no";
        return primaryJdbcTemplate.queryForList(sql, Map.of("a", accountId, "e", electionId), String.class);
    }

    private String buildJson(String sql, Map<String, Object> params) {
        List<Map<String, Object>> rows = primaryJdbcTemplate.queryForList(sql, params);
        String body = rows.stream()
                .map(r -> "\"" + escape((String) r.get("k")) + "\":" + ((Number) r.get("c")).longValue())
                .collect(Collectors.joining(","));
        return "{" + body + "}";
    }

    @Deprecated
    private String buildJson(String sql, Long accountId, Long electionId) {
        return buildJson(sql, Map.of("a", accountId, "e", electionId));
    }

    private String escape(String in) {
        if (in == null) return "unknown";
        return in.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t")
                .replace("\b", "\\b")
                .replace("\f", "\\f");
    }
}