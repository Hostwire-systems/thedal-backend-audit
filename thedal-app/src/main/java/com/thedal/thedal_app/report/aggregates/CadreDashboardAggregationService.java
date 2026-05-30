package com.thedal.thedal_app.report.aggregates;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thedal.thedal_app.report.cadre.VolunteerVsVoterReportRepository;
import com.thedal.thedal_app.volunteer.VolunteerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CadreDashboardAggregationService {

    private final CadreDashboardStatsRepository repository;
    private final VolunteerVsVoterReportRepository volunteerVsVoterReportRepository;
    private final VolunteerRepository volunteerRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Transactional
    public CadreDashboardStats aggregateOne(Long accountId, Long electionId) {
        log.info("Starting cadre dashboard aggregation for accountId={}, electionId={}", accountId, electionId);
        
        CadreDashboardStats stats = repository.findByAccountIdAndElectionId(accountId, electionId)
                .orElse(new CadreDashboardStats());
        
        stats.setAccountId(accountId);
        stats.setElectionId(electionId);
        
        try {
            // Get total cadres for this election and account
            Integer totalCadres = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM volunteers WHERE election_id = ? AND account_id = ?",
                Integer.class, electionId, accountId);
            stats.setTotalCadres(totalCadres != null ? totalCadres : 0);
            
            // Get cadres logged (users who have created/updated at least one voter)
            Integer cadresLogged = 0;
            try {
                cadresLogged = jdbcTemplate.queryForObject(
                    "SELECT COUNT(DISTINCT user_id) FROM volunteer_vs_voter_report " +
                    "WHERE election_id = ? AND account_id = ? AND (total_voter_created > 0 OR total_voter_updated > 0)",
                    Integer.class, electionId, accountId);
            } catch (Exception e) {
                log.warn("Could not query volunteer_vs_voter_report table, setting cadresLogged to 0", e);
            }
            stats.setCadresLogged(cadresLogged != null ? cadresLogged : 0);
            stats.setCadresNotLogged(stats.getTotalCadres() - stats.getCadresLogged());
            
            // Get booths assigned (count unique booths assigned to volunteers)
            Integer boothsAssigned = 0;
            try {
                boothsAssigned = jdbcTemplate.queryForObject(
                    "SELECT COUNT(DISTINCT veab.assigned_booth) FROM volunteer_entity_assigned_booth veab " +
                    "INNER JOIN volunteers v ON v.id = veab.volunteer_entity_id " +
                    "WHERE v.election_id = ? AND v.account_id = ?",
                    Integer.class, electionId, accountId);
            } catch (Exception e) {
                log.warn("Could not query volunteer booth assignments, setting boothsAssigned to 0", e);
            }
            stats.setBoothsAssigned(boothsAssigned != null ? boothsAssigned : 0);
            
            // Get aggregated update counts from volunteer_vs_voter_report
            Map<String, Object> updateCounts = null;
            try {
                updateCounts = jdbcTemplate.queryForMap(
                    "SELECT " +
                    "COALESCE(SUM(total_mobile_number_updated), 0) as total_mobile_updated, " +
                    "COALESCE(SUM(total_dob_updated), 0) as total_dob_updated, " +
                    "COALESCE(SUM(total_party_updated), 0) as total_party_updated, " +
                    "COALESCE(SUM(total_caste_updated), 0) as total_caste_updated, " +
                    "COALESCE(SUM(total_religion_updated), 0) as total_religion_updated, " +
                    "COALESCE(SUM(total_language_updated), 0) as total_language_updated " +
                    "FROM volunteer_vs_voter_report " +
                    "WHERE election_id = ? AND account_id = ?",
                    electionId, accountId);
            } catch (Exception e) {
                log.warn("Could not query update counts from volunteer_vs_voter_report, using defaults", e);
            }
            
            if (updateCounts != null) {
                stats.setTotalMobileUpdated(((Number) updateCounts.get("total_mobile_updated")).intValue());
                stats.setTotalDobUpdated(((Number) updateCounts.get("total_dob_updated")).intValue());
                stats.setTotalPartyUpdated(((Number) updateCounts.get("total_party_updated")).intValue());
                stats.setTotalCasteUpdated(((Number) updateCounts.get("total_caste_updated")).intValue());
                stats.setTotalReligionUpdated(((Number) updateCounts.get("total_religion_updated")).intValue());
                stats.setTotalLanguageUpdated(((Number) updateCounts.get("total_language_updated")).intValue());
            } else {
                stats.setTotalMobileUpdated(0);
                stats.setTotalDobUpdated(0);
                stats.setTotalPartyUpdated(0);
                stats.setTotalCasteUpdated(0);
                stats.setTotalReligionUpdated(0);
                stats.setTotalLanguageUpdated(0);
            }
            
            // Get top 10 performing cadres
            List<Map<String, Object>> top10 = new ArrayList<>();
            try {
                top10 = jdbcTemplate.queryForList(
                    "SELECT vvr.user_id, COALESCE(vvr.total_voter_created, 0) as value " +
                    "FROM volunteer_vs_voter_report vvr " +
                    "INNER JOIN volunteers v ON vvr.user_id = v.user_id AND v.election_id = vvr.election_id " +
                    "WHERE vvr.election_id = ? AND vvr.account_id = ? " +
                    "ORDER BY vvr.total_voter_created DESC NULLS LAST " +
                    "LIMIT 10",
                    electionId, accountId);
            } catch (Exception e) {
                log.warn("Could not query top performing cadres from reports, trying volunteers table", e);
                try {
                    // Fallback: get volunteer info even without performance data
                    top10 = jdbcTemplate.queryForList(
                        "SELECT v.user_id, 0 as value " +
                        "FROM volunteers v " +
                        "WHERE v.election_id = ? AND v.account_id = ? " +
                        "LIMIT 10",
                        electionId, accountId);
                } catch (Exception e2) {
                    log.warn("Could not query volunteer details either", e2);
                }
            }
            stats.setTop10Cadres(convertToJson(top10));
            
            // Get least 10 performing cadres
            List<Map<String, Object>> least10 = new ArrayList<>();
            try {
                least10 = jdbcTemplate.queryForList(
                    "SELECT vvr.user_id, COALESCE(vvr.total_voter_created, 0) as value " +
                    "FROM volunteer_vs_voter_report vvr " +
                    "INNER JOIN volunteers v ON vvr.user_id = v.user_id AND v.election_id = vvr.election_id " +
                    "WHERE vvr.election_id = ? AND vvr.account_id = ? " +
                    "ORDER BY vvr.total_voter_created ASC NULLS FIRST " +
                    "LIMIT 10",
                    electionId, accountId);
            } catch (Exception e) {
                log.warn("Could not query least performing cadres from reports, trying volunteers table", e);
                try {
                    // Fallback: get volunteer info even without performance data  
                    least10 = jdbcTemplate.queryForList(
                        "SELECT v.user_id, 0 as value " +
                        "FROM volunteers v " +
                        "WHERE v.election_id = ? AND v.account_id = ? " +
                        "LIMIT 10",
                        electionId, accountId);
                } catch (Exception e2) {
                    log.warn("Could not query volunteer details either", e2);
                }
            }
            stats.setLeast10Cadres(convertToJson(least10));
            
            stats.setComputedAt(OffsetDateTime.now());
            stats.setRefreshedAt(OffsetDateTime.now());
            
            CadreDashboardStats saved = repository.save(stats);
            log.info("Successfully aggregated cadre dashboard stats: totalCadres={}, cadresLogged={}, boothsAssigned={}", 
                    saved.getTotalCadres(), saved.getCadresLogged(), saved.getBoothsAssigned());
            
            return saved;
            
        } catch (Exception e) {
            log.error("Error aggregating cadre dashboard stats for accountId={}, electionId={}", accountId, electionId, e);
            throw new RuntimeException("Failed to aggregate cadre dashboard stats", e);
        }
    }

    private String convertToJson(List<Map<String, Object>> data) {
        try {
            // Convert to the format expected: [{"userId": <id>, "value": <count>, "name": <userId or name>}]
            List<Map<String, Object>> formatted = data.stream()
                    .map(row -> {
                        Map<String, Object> result = new HashMap<>();
                        result.put("userId", row.get("user_id"));
                        result.put("value", row.get("value"));
                        
                        // Try to build full name from first_name and last_name if available
                        String firstName = (String) row.get("first_name");
                        String lastName = (String) row.get("last_name");
                        String fullName = "";
                        
                        if (firstName != null) fullName += firstName;
                        if (lastName != null) {
                            if (!fullName.isEmpty()) fullName += " ";
                            fullName += lastName;
                        }
                        
                        // If no name available, use User ID as display name
                        if (fullName.isEmpty()) {
                            Object userId = row.get("user_id");
                            fullName = "User " + (userId != null ? userId.toString() : "Unknown");
                        }
                        
                        result.put("name", fullName);
                        return result;
                    })
                    .toList();
            return objectMapper.writeValueAsString(formatted);
        } catch (JsonProcessingException e) {
            log.warn("Failed to convert performance data to JSON, returning empty array", e);
            return "[]";
        }
    }
}
