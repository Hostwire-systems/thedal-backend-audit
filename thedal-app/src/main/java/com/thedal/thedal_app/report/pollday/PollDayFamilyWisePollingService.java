package com.thedal.thedal_app.report.pollday;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thedal.thedal_app.report.pollday.dto.FamilyWisePollingData;
import com.thedal.thedal_app.report.pollday.dto.FamilyWisePollingResponse;
import com.thedal.thedal_app.report.pollday.dto.FamilyWisePollingSummary;
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
public class PollDayFamilyWisePollingService {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final PollDayFamilyWisePollingRepository repository;
    private final ObjectMapper objectMapper;

    @Transactional
    public FamilyWisePollingResponse recomputeFamilyWisePolling(
            Long accountId, Long electionId, LocalDate pollingDate, List<Integer> partNumbers,
            List<String> partyList, List<String> religionList, List<String> casteCategoryList,
            List<String> casteList, List<String> subCasteList, List<String> languageList,
            List<String> schemeList, List<String> genderList, Integer minAge, Integer maxAge,
            Boolean includeUnknownAge) {
        try {
            // Check if any filters are provided
            boolean hasFilters = hasAnyFilters(partyList, religionList, casteCategoryList, casteList,
                subCasteList, languageList, schemeList, genderList, minAge, maxAge, includeUnknownAge);
            
            log.info("Computing family-wise polling for accountId={}, electionId={}, pollingDate={}, partNumbers={}, hasFilters={}", 
                accountId, electionId, pollingDate, partNumbers, hasFilters);
            
            // Build optimized SQL query with filters
            // A family is considered "voted" if at least one member (matching filters) has voted
            String sql = buildFamilyWiseQuery(pollingDate, partNumbers, hasFilters,
                partyList, religionList, casteCategoryList, casteList, subCasteList,
                genderList, minAge, maxAge);
            Map<String, Object> params = buildFamilyQueryParams(accountId, electionId, pollingDate, partNumbers,
                partyList, religionList, casteCategoryList, casteList, subCasteList, languageList, schemeList,
                genderList, minAge, maxAge, includeUnknownAge, hasFilters);
            
            List<Map<String, Object>> partData = jdbcTemplate.queryForList(sql, params);
            
            log.info("Query returned {} parts for electionId={}", partData.size(), electionId);
            if (!partData.isEmpty()) {
                log.debug("First row data: {}", partData.get(0));
            }
            
            // Build response with part-wise family data
            List<FamilyWisePollingData> parts = new ArrayList<>();
            OffsetDateTime now = OffsetDateTime.now();
            
            long summaryTotalFamilies = 0;
            long summaryFullyVotedFamilies = 0;
            long summaryPartiallyVotedFamilies = 0;
            long summaryNotVotedFamilies = 0;
            
            for (Map<String, Object> row : partData) {
                Integer partNumber = ((Number) row.get("part_number")).intValue();
                Long totalFamilies = ((Number) row.get("total_families")).longValue();
                
                // Handle potential null values from COUNT CASE WHEN
                Object fullyVotedObj = row.get("fully_voted_families");
                Object partiallyVotedObj = row.get("partially_voted_families");
                Object notVotedObj = row.get("not_voted_families");
                
                Long fullyVotedFamilies = fullyVotedObj != null ? ((Number) fullyVotedObj).longValue() : 0L;
                Long partiallyVotedFamilies = partiallyVotedObj != null ? ((Number) partiallyVotedObj).longValue() : 0L;
                Long notVotedFamilies = notVotedObj != null ? ((Number) notVotedObj).longValue() : 0L;
                
                log.debug("Part {}: total={}, fully={}, partially={}, notVoted={}", 
                    partNumber, totalFamilies, fullyVotedFamilies, partiallyVotedFamilies, notVotedFamilies);
                
                // Voting percentage based on fully voted families
                Double votingPercentage = totalFamilies > 0 ? (fullyVotedFamilies * 100.0 / totalFamilies) : 0.0;
                
                // Round to 2 decimal places
                votingPercentage = Math.round(votingPercentage * 100.0) / 100.0;
                
                FamilyWisePollingData familyPolling = new FamilyWisePollingData(
                    partNumber,
                    totalFamilies,
                    fullyVotedFamilies,
                    partiallyVotedFamilies,
                    notVotedFamilies,
                    votingPercentage,
                    now
                );
                
                parts.add(familyPolling);
                
                summaryTotalFamilies += totalFamilies;
                summaryFullyVotedFamilies += fullyVotedFamilies;
                summaryPartiallyVotedFamilies += partiallyVotedFamilies;
                summaryNotVotedFamilies += notVotedFamilies;
            }
            
            // Calculate overall voting percentage based on fully voted families
            Double overallVotingPercentage = summaryTotalFamilies > 0 
                ? (summaryFullyVotedFamilies * 100.0 / summaryTotalFamilies) 
                : 0.0;
            overallVotingPercentage = Math.round(overallVotingPercentage * 100.0) / 100.0;
            
            // Build summary
            FamilyWisePollingSummary summary = new FamilyWisePollingSummary(
                parts.size(),
                summaryTotalFamilies,
                summaryFullyVotedFamilies,
                summaryPartiallyVotedFamilies,
                summaryNotVotedFamilies,
                overallVotingPercentage,
                now
            );
            
            // Create response
            FamilyWisePollingResponse response = new FamilyWisePollingResponse(parts, summary);
            
            // Only save to cache if NO filters were applied
            if (!hasFilters && (partNumbers == null || partNumbers.isEmpty())) {
                // Save to database for caching
                String jsonData = objectMapper.writeValueAsString(response);
                
                // Use sentinel date (1900-01-01) for all-time data when pollingDate is null
                LocalDate dbDate = pollingDate != null ? pollingDate : LocalDate.of(1900, 1, 1);
                
                Optional<PollDayFamilyWisePolling> existing = repository
                    .findByAccountIdAndElectionIdAndPollingDate(accountId, electionId, dbDate);
                PollDayFamilyWisePolling entity = existing.orElse(new PollDayFamilyWisePolling());
                
                entity.setAccountId(accountId);
                entity.setElectionId(electionId);
                entity.setPollingDate(dbDate);
                entity.setFamilyWiseDataJson(jsonData);
                if (entity.getComputedAt() == null) {
                    entity.setComputedAt(now);
                }
                entity.setRefreshedAt(now);
                
                repository.save(entity);
                log.info("Successfully computed and cached family-wise polling for accountId={}, electionId={}", 
                    accountId, electionId);
            } else {
                log.info("Successfully computed family-wise polling with filters (not cached) for accountId={}, electionId={}", 
                    accountId, electionId);
            }
            
            return response;
            
        } catch (Exception e) {
            log.error("Error computing family-wise polling for accountId={}, electionId={}, pollingDate={}", 
                accountId, electionId, pollingDate, e);
            throw new RuntimeException("Failed to compute family-wise polling data", e);
        }
    }
    
    @Transactional(readOnly = true)
    public FamilyWisePollingResponse getFamilyWisePolling(
            Long accountId, Long electionId, LocalDate pollingDate, List<Integer> partNumbers,
            List<String> partyList, List<String> religionList, List<String> casteCategoryList,
            List<String> casteList, List<String> subCasteList, List<String> languageList,
            List<String> schemeList, List<String> genderList, Integer minAge, Integer maxAge,
            Boolean includeUnknownAge) {
        try {
            // Check if any filters are provided
            boolean hasFilters = hasAnyFilters(partyList, religionList, casteCategoryList, casteList,
                subCasteList, languageList, schemeList, genderList, minAge, maxAge, includeUnknownAge);
            
            // If filters or part numbers provided, always recompute (bypass cache)
            if (hasFilters || (partNumbers != null && !partNumbers.isEmpty())) {
                log.info("Filters or part numbers provided - computing family-wise polling from DB");
                return recomputeFamilyWisePolling(accountId, electionId, pollingDate, partNumbers,
                    partyList, religionList, casteCategoryList, casteList, subCasteList,
                    languageList, schemeList, genderList, minAge, maxAge, includeUnknownAge);
            }
            
            // No filters - use cached data if available
            // Use sentinel date (1900-01-01) for all-time data when pollingDate is null
            LocalDate dbDate = pollingDate != null ? pollingDate : LocalDate.of(1900, 1, 1);
            
            Optional<PollDayFamilyWisePolling> existing = repository
                .findByAccountIdAndElectionIdAndPollingDate(accountId, electionId, dbDate);
            
            if (existing.isPresent()) {
                String jsonData = existing.get().getFamilyWiseDataJson();
                return objectMapper.readValue(jsonData, FamilyWisePollingResponse.class);
            }
            
            // If not found, compute it
            return recomputeFamilyWisePolling(accountId, electionId, pollingDate, partNumbers,
                partyList, religionList, casteCategoryList, casteList, subCasteList,
                languageList, schemeList, genderList, minAge, maxAge, includeUnknownAge);
            
        } catch (Exception e) {
            log.error("Error retrieving family-wise polling for accountId={}, electionId={}, pollingDate={}, partNumbers={}", 
                accountId, electionId, pollingDate, partNumbers, e);
            throw new RuntimeException("Failed to retrieve family-wise polling data", e);
        }
    }
    
    @Transactional
    public void clearCache(Long accountId, Long electionId, LocalDate pollingDate) {
        try {
            // Use sentinel date (1900-01-01) for all-time data when pollingDate is null
            LocalDate dbDate = pollingDate != null ? pollingDate : LocalDate.of(1900, 1, 1);
            
            int deleted = repository.deleteByAccountIdAndElectionIdAndPollingDate(accountId, electionId, dbDate);
            log.info("Cleared cache for accountId={}, electionId={}, pollingDate={} - deleted {} records", 
                accountId, electionId, pollingDate, deleted);
        } catch (Exception e) {
            log.error("Error clearing cache for accountId={}, electionId={}, pollingDate={}", 
                accountId, electionId, pollingDate, e);
            throw new RuntimeException("Failed to clear cache", e);
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
    
    private String buildFamilyWiseQuery(LocalDate pollingDate, List<Integer> partNumbers, boolean hasFilters,
                                         List<String> partyList, List<String> religionList,
                                         List<String> casteCategoryList, List<String> casteList,
                                         List<String> subCasteList, List<String> genderList,
                                         Integer minAge, Integer maxAge) {
        StringBuilder sql = new StringBuilder();
        
        // Build a CTE to calculate family voting status
        sql.append("""
            WITH family_voting_status AS (
                SELECT 
                    v.part_no,
                    v.family_id,
                    COUNT(*) as total_members,
            """);
        
        if (pollingDate != null) {
            sql.append("""
                    COUNT(CASE 
                        WHEN v.has_voted = true 
                        AND v.voted_timestamp >= :startTime::timestamp
                        AND v.voted_timestamp < :endTime::timestamp 
                    THEN 1 END) as voted_members
                """);
        } else {
            sql.append("""
                    COUNT(CASE WHEN v.has_voted = true THEN 1 END) as voted_members
                """);
        }
        
        sql.append("""
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
                    AND v.family_id IS NOT NULL
            """);
        
        // Add part number filter if provided
        if (partNumbers != null && !partNumbers.isEmpty()) {
            sql.append("    AND v.part_no IN (:partNumbers)\n");
        }
        
        // Add filter conditions individually based on what's provided
        if (partyList != null && !partyList.isEmpty()) {
            sql.append("    AND LOWER(TRIM(p.party_name)) IN (:parties)\n");
        }
        if (religionList != null && !religionList.isEmpty()) {
            sql.append("    AND LOWER(TRIM(r.religion_name)) IN (:religions)\n");
        }
        if (casteCategoryList != null && !casteCategoryList.isEmpty()) {
            sql.append("    AND LOWER(TRIM(cc.caste_category_name)) IN (:casteCategories)\n");
        }
        if (casteList != null && !casteList.isEmpty()) {
            sql.append("    AND LOWER(TRIM(c.caste_name)) IN (:castes)\n");
        }
        if (subCasteList != null && !subCasteList.isEmpty()) {
            sql.append("    AND LOWER(TRIM(sc.sub_caste_name)) IN (:subCastes)\n");
        }
        if (genderList != null && !genderList.isEmpty()) {
            sql.append("    AND LOWER(TRIM(v.gender)) IN (:genders)\n");
        }
        if (minAge != null) {
            sql.append("    AND v.age >= :minAge\n");
        }
        if (maxAge != null) {
            sql.append("    AND v.age <= :maxAge\n");
        }
        
        sql.append("""
                GROUP BY v.part_no, v.family_id
            )
            SELECT 
                part_no as part_number,
                COUNT(*) as total_families,
                COALESCE(SUM(CASE WHEN voted_members = total_members AND voted_members > 0 THEN 1 ELSE 0 END), 0) as fully_voted_families,
                COALESCE(SUM(CASE WHEN voted_members > 0 AND voted_members < total_members THEN 1 ELSE 0 END), 0) as partially_voted_families,
                COALESCE(SUM(CASE WHEN voted_members = 0 THEN 1 ELSE 0 END), 0) as not_voted_families
            FROM family_voting_status
            GROUP BY part_no
            ORDER BY part_no
            """);
        
        return sql.toString();
    }
    
    private Map<String, Object> buildFamilyQueryParams(Long accountId, Long electionId, LocalDate pollingDate,
            List<Integer> partNumbers, List<String> partyList, List<String> religionList,
            List<String> casteCategoryList, List<String> casteList, List<String> subCasteList,
            List<String> languageList, List<String> schemeList, List<String> genderList,
            Integer minAge, Integer maxAge, Boolean includeUnknownAge, boolean hasFilters) {
        
        Map<String, Object> params = new HashMap<>();
        params.put("accountId", accountId);
        params.put("electionId", electionId);
        
        if (pollingDate != null) {
            LocalDateTime startOfDay = pollingDate.atStartOfDay();
            LocalDateTime endOfDay = pollingDate.plusDays(1).atStartOfDay();
            params.put("startTime", startOfDay.toString());
            params.put("endTime", endOfDay.toString());
        }
        
        if (partNumbers != null && !partNumbers.isEmpty()) {
            params.put("partNumbers", partNumbers);
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
