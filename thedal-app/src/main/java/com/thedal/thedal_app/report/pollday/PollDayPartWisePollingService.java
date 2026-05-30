package com.thedal.thedal_app.report.pollday;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thedal.thedal_app.report.pollday.dto.PartWisePollingData;
import com.thedal.thedal_app.report.pollday.dto.PartWisePollingResponse;
import com.thedal.thedal_app.report.pollday.dto.PartWisePollingSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PollDayPartWisePollingService {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final PollDayPartWisePollingRepository repository;
    private final ObjectMapper objectMapper;

    @Transactional
    public PartWisePollingResponse recomputePartWisePolling(
            Long accountId, Long electionId, LocalDate pollingDate,
            List<String> partyList, List<String> religionList, List<String> casteCategoryList,
            List<String> casteList, List<String> subCasteList, List<String> languageList,
            List<String> schemeList, List<String> genderList, Integer minAge, Integer maxAge,
            Boolean includeUnknownAge) {
        try {
            // Check if any filters are provided
            boolean hasFilters = hasAnyFilters(partyList, religionList, casteCategoryList, casteList,
                subCasteList, languageList, schemeList, genderList, minAge, maxAge, includeUnknownAge);
            
            log.info("Computing part-wise polling for accountId={}, electionId={}, pollingDate={}, hasFilters={}", 
                accountId, electionId, pollingDate, hasFilters);
            
            // Get current year and previous year
            int currentYear = pollingDate != null ? pollingDate.getYear() : LocalDate.now(ZoneId.of("Asia/Kolkata")).getYear();
            int previousYear = currentYear - 1;
            
            // Build SQL query with optional filters
            String sql = buildPartWiseQuery(pollingDate, hasFilters,
                partyList, religionList, casteCategoryList, casteList, subCasteList,
                genderList, minAge, maxAge);
            Map<String, Object> params = buildQueryParams(accountId, electionId, pollingDate, currentYear, previousYear,
                partyList, religionList, casteCategoryList, casteList, subCasteList, languageList, schemeList,
                genderList, minAge, maxAge, includeUnknownAge, hasFilters);
            
            List<Map<String, Object>> partData = jdbcTemplate.queryForList(sql, params);
            
            // Build response with part-wise data
            List<PartWisePollingData> parts = new ArrayList<>();
            OffsetDateTime now = OffsetDateTime.now();
            
            long summaryTotalVoters = 0;
            long summaryTotalPolled2025 = 0;
            long summaryTotalPolled2024 = 0;
            
            for (Map<String, Object> row : partData) {
                Integer partNumber = ((Number) row.get("part_number")).intValue();
                Long totalVoters = ((Number) row.get("total_voters")).longValue();
                Long polledCurrentYear = ((Number) row.get("polled_current_year")).longValue();
                Long polledPreviousYear = ((Number) row.get("polled_previous_year")).longValue();
                Long didNotVote = totalVoters - polledCurrentYear;
                Double turnoutPercentage = totalVoters > 0 ? (polledCurrentYear * 100.0 / totalVoters) : 0.0;
                
                // Round to 2 decimal places
                turnoutPercentage = Math.round(turnoutPercentage * 100.0) / 100.0;
                
                PartWisePollingData partPolling = new PartWisePollingData(
                    partNumber,
                    totalVoters,
                    polledCurrentYear,
                    polledPreviousYear,
                    didNotVote,
                    turnoutPercentage,
                    now
                );
                
                parts.add(partPolling);
                
                summaryTotalVoters += totalVoters;
                summaryTotalPolled2025 += polledCurrentYear;
                summaryTotalPolled2024 += polledPreviousYear;
            }
            
            // Calculate overall turnout percentage
            Double overallTurnoutPercentage = summaryTotalVoters > 0 
                ? (summaryTotalPolled2025 * 100.0 / summaryTotalVoters) 
                : 0.0;
            overallTurnoutPercentage = Math.round(overallTurnoutPercentage * 100.0) / 100.0;
            
            // Build summary
            PartWisePollingSummary summary = new PartWisePollingSummary(
                parts.size(),
                summaryTotalVoters,
                summaryTotalPolled2025,
                summaryTotalPolled2024,
                overallTurnoutPercentage,
                now
            );
            
            // Create response
            PartWisePollingResponse response = new PartWisePollingResponse(parts, summary);
            
            // Only save to cache if NO filters were applied (to avoid cache pollution)
            if (!hasFilters) {
                // Save to database
                String jsonData = objectMapper.writeValueAsString(response);
                
                // Use sentinel date (1900-01-01) for all-time data when pollingDate is null
                LocalDate dbDate = pollingDate != null ? pollingDate : LocalDate.of(1900, 1, 1);
                
                Optional<PollDayPartWisePolling> existing = repository
                    .findByAccountIdAndElectionIdAndPollingDate(accountId, electionId, dbDate);
                PollDayPartWisePolling entity = existing.orElse(new PollDayPartWisePolling());
                
                entity.setAccountId(accountId);
                entity.setElectionId(electionId);
                entity.setPollingDate(dbDate);
                entity.setPartWiseDataJson(jsonData);
                if (entity.getComputedAt() == null) {
                    entity.setComputedAt(now);
                }
                entity.setRefreshedAt(now);
                
                repository.save(entity);
                log.info("Successfully computed and cached part-wise polling for accountId={}, electionId={}", 
                    accountId, electionId);
            } else {
                log.info("Successfully computed part-wise polling with filters (not cached) for accountId={}, electionId={}", 
                    accountId, electionId);
            }
            
            return response;
            
        } catch (Exception e) {
            log.error("Error computing part-wise polling for accountId={}, electionId={}, pollingDate={}", 
                accountId, electionId, pollingDate, e);
            throw new RuntimeException("Failed to compute part-wise polling data", e);
        }
    }
    
    @Transactional(readOnly = true)
    public PartWisePollingResponse getPartWisePolling(
            Long accountId, Long electionId, LocalDate pollingDate,
            List<String> partyList, List<String> religionList, List<String> casteCategoryList,
            List<String> casteList, List<String> subCasteList, List<String> languageList,
            List<String> schemeList, List<String> genderList, Integer minAge, Integer maxAge,
            Boolean includeUnknownAge) {
        try {
            // Check if any filters are provided
            boolean hasFilters = hasAnyFilters(partyList, religionList, casteCategoryList, casteList,
                subCasteList, languageList, schemeList, genderList, minAge, maxAge, includeUnknownAge);
            
            // If filters are provided, always recompute (bypass cache)
            if (hasFilters) {
                log.info("Filters provided - computing part-wise polling from DB for accountId={}, electionId={}", 
                    accountId, electionId);
                return recomputePartWisePolling(accountId, electionId, pollingDate,
                    partyList, religionList, casteCategoryList, casteList, subCasteList,
                    languageList, schemeList, genderList, minAge, maxAge, includeUnknownAge);
            }
            
            // No filters - use cached data if available
            // Use sentinel date (1900-01-01) for all-time data when pollingDate is null
            LocalDate dbDate = pollingDate != null ? pollingDate : LocalDate.of(1900, 1, 1);
            
            Optional<PollDayPartWisePolling> existing = repository
                .findByAccountIdAndElectionIdAndPollingDate(accountId, electionId, dbDate);
            
            if (existing.isPresent()) {
                String jsonData = existing.get().getPartWiseDataJson();
                return objectMapper.readValue(jsonData, PartWisePollingResponse.class);
            }
            
            // If not found, compute it
            return recomputePartWisePolling(accountId, electionId, pollingDate,
                partyList, religionList, casteCategoryList, casteList, subCasteList,
                languageList, schemeList, genderList, minAge, maxAge, includeUnknownAge);
            
        } catch (Exception e) {
            log.error("Error retrieving part-wise polling for accountId={}, electionId={}, pollingDate={}", 
                accountId, electionId, pollingDate, e);
            throw new RuntimeException("Failed to retrieve part-wise polling data", e);
        }
    }
    
    // Helper methods
    
    private boolean hasAnyFilters(List<String> partyList, List<String> religionList, List<String> casteCategoryList,
            List<String> casteList, List<String> subCasteList, List<String> languageList,
            List<String> schemeList, List<String> genderList, Integer minAge, Integer maxAge,
            Boolean includeUnknownAge) {
        return (partyList != null && !partyList.isEmpty()) ||
               (religionList != null && !religionList.isEmpty()) ||
               (casteCategoryList != null && !casteCategoryList.isEmpty()) ||
               (casteList != null && !casteList.isEmpty()) ||
               (subCasteList != null && !subCasteList.isEmpty()) ||
               (languageList != null && !languageList.isEmpty()) ||
               (schemeList != null && !schemeList.isEmpty()) ||
               (genderList != null && !genderList.isEmpty()) ||
               minAge != null ||
               maxAge != null ||
               (includeUnknownAge != null && includeUnknownAge);
    }
    
    private String buildPartWiseQuery(LocalDate pollingDate, boolean hasFilters,
                                       List<String> partyList, List<String> religionList,
                                       List<String> casteCategoryList, List<String> casteList,
                                       List<String> subCasteList, List<String> genderList,
                                       Integer minAge, Integer maxAge) {
        StringBuilder sql = new StringBuilder();
        
        sql.append("""
            SELECT 
                v.part_no as part_number,
                COUNT(*) as total_voters,
                COUNT(CASE 
                    WHEN v.has_voted = true 
                    AND EXTRACT(YEAR FROM v.voted_timestamp AT TIME ZONE 'UTC' AT TIME ZONE 'Asia/Kolkata') = :currentYear
            """);
        
        if (pollingDate != null) {
            sql.append("""
                    AND v.voted_timestamp >= :startTime::timestamp
                    AND v.voted_timestamp < :endTime::timestamp 
                """);
        }
        
        sql.append("""
                THEN 1 END) as polled_current_year,
                COUNT(CASE 
                    WHEN v.has_voted = true 
                    AND EXTRACT(YEAR FROM v.voted_timestamp AT TIME ZONE 'UTC' AT TIME ZONE 'Asia/Kolkata') = :previousYear
                THEN 1 END) as polled_previous_year
            FROM _voters v
            """);
        
        // Add JOINs only if filters are present
        if (hasFilters) {
            sql.append("""
                LEFT JOIN parties p ON v.party_id = p.id
                LEFT JOIN religion r ON v.religion_id = r.id
                LEFT JOIN caste c ON v.caste_id = c.id
                LEFT JOIN caste_category cc ON v.caste_category_id = cc.id
                LEFT JOIN sub_caste sc ON v.sub_caste_id = sc.id
                """);
        }
        
        sql.append("""
            WHERE v.account_id = :accountId 
                AND v.election_id = :electionId
                AND v.part_no IS NOT NULL
            """);
        
        // Add filter conditions individually based on what's provided
        if (partyList != null && !partyList.isEmpty()) {
            sql.append("AND LOWER(TRIM(p.party_name)) IN (:parties)\n");
        }
        if (religionList != null && !religionList.isEmpty()) {
            sql.append("AND LOWER(TRIM(r.religion_name)) IN (:religions)\n");
        }
        if (casteCategoryList != null && !casteCategoryList.isEmpty()) {
            sql.append("AND LOWER(TRIM(cc.caste_category_name)) IN (:casteCategories)\n");
        }
        if (casteList != null && !casteList.isEmpty()) {
            sql.append("AND LOWER(TRIM(c.caste_name)) IN (:castes)\n");
        }
        if (subCasteList != null && !subCasteList.isEmpty()) {
            sql.append("AND LOWER(TRIM(sc.sub_caste_name)) IN (:subCastes)\n");
        }
        if (genderList != null && !genderList.isEmpty()) {
            sql.append("AND LOWER(TRIM(v.gender)) IN (:genders)\n");
        }
        if (minAge != null) {
            sql.append("AND v.age >= :minAge\n");
        }
        if (maxAge != null) {
            sql.append("AND v.age <= :maxAge\n");
        }
        
        sql.append("""
            GROUP BY v.part_no
            ORDER BY v.part_no
            """);
        
        return sql.toString();
    }
    
    private Map<String, Object> buildQueryParams(Long accountId, Long electionId, LocalDate pollingDate,
            int currentYear, int previousYear, List<String> partyList, List<String> religionList,
            List<String> casteCategoryList, List<String> casteList, List<String> subCasteList,
            List<String> languageList, List<String> schemeList, List<String> genderList,
            Integer minAge, Integer maxAge, Boolean includeUnknownAge, boolean hasFilters) {
        
        Map<String, Object> params = new HashMap<>();
        params.put("accountId", accountId);
        params.put("electionId", electionId);
        params.put("currentYear", currentYear);
        params.put("previousYear", previousYear);
        
        if (pollingDate != null) {
            LocalDateTime startOfDay = pollingDate.atStartOfDay();
            LocalDateTime endOfDay = pollingDate.plusDays(1).atStartOfDay();
            params.put("startTime", startOfDay.toString());
            params.put("endTime", endOfDay.toString());
        }
        
        if (hasFilters) {
            // Only add parameters that are actually used in the query
            if (partyList != null && !partyList.isEmpty()) {
                params.put("parties", partyList.stream().map(String::toLowerCase).toList());
            }
            if (religionList != null && !religionList.isEmpty()) {
                params.put("religions", religionList.stream().map(String::toLowerCase).toList());
            }
            if (casteCategoryList != null && !casteCategoryList.isEmpty()) {
                params.put("casteCategories", casteCategoryList.stream().map(String::toLowerCase).toList());
            }
            if (casteList != null && !casteList.isEmpty()) {
                params.put("castes", casteList.stream().map(String::toLowerCase).toList());
            }
            if (subCasteList != null && !subCasteList.isEmpty()) {
                params.put("subCastes", subCasteList.stream().map(String::toLowerCase).toList());
            }
            if (genderList != null && !genderList.isEmpty()) {
                params.put("genders", genderList.stream().map(String::toLowerCase).toList());
            }
            if (minAge != null) {
                params.put("minAge", minAge);
            }
            if (maxAge != null) {
                params.put("maxAge", maxAge);
            }
        }
        
        return params;
    }
}
