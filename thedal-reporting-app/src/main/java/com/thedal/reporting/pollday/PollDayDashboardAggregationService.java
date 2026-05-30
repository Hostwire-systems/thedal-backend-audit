package com.thedal.reporting.pollday;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PollDayDashboardAggregationService {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final PollDayHourlyTurnoutRepository hourlyRepository;
    private final PollDayAgeGroupTurnoutRepository ageGroupRepository;
    private final PollDayBoothSummaryRepository boothSummaryRepository;
    private final PollDayWardAgeGroupTurnoutRepository wardAgeGroupRepository;

    @Transactional
    public PollDayHourlyTurnout recomputeHourly(Long accountId, Long electionId, LocalDate pollingDate) {
        try {
            log.info("Computing hourly turnout for accountId={}, electionId={}, pollingDate={}", accountId, electionId, pollingDate);
            
            // Get start and end of polling date in IST
            ZoneId istZone = ZoneId.of("Asia/Kolkata");
            LocalDateTime startOfDay = pollingDate.atStartOfDay();
            LocalDateTime endOfDay = pollingDate.plusDays(1).atStartOfDay();
            
            // Query hourly vote counts (using IST hour extraction)
            String sql = """
                SELECT 
                    EXTRACT(HOUR FROM voted_timestamp AT TIME ZONE 'UTC' AT TIME ZONE 'Asia/Kolkata') as hour,
                    COUNT(*) as voted_count
                FROM _voters v 
                WHERE v.account_id = :accountId 
                    AND v.election_id = :electionId 
                    AND v.has_voted = true 
                    AND v.voted_timestamp IS NOT NULL
                    AND v.voted_timestamp >= :startTime::timestamp
                    AND v.voted_timestamp < :endTime::timestamp
                GROUP BY EXTRACT(HOUR FROM voted_timestamp AT TIME ZONE 'UTC' AT TIME ZONE 'Asia/Kolkata')
                ORDER BY hour
                """;
                
            Map<String, Object> params = Map.of(
                "accountId", accountId,
                "electionId", electionId,
                "startTime", startOfDay.toString(),
                "endTime", endOfDay.toString()
            );
            
            List<Map<String, Object>> hourlyData = jdbcTemplate.queryForList(sql, params);
            
            // Build JSON with all 24 hours initialized to 0
            StringBuilder jsonBuilder = new StringBuilder("{");
            for (int hour = 0; hour < 24; hour++) {
                if (hour > 0) jsonBuilder.append(",");
                String hourKey = String.format("%02d", hour);
                
                // Find count for this hour
                final int currentHour = hour; // Make variable final for lambda
                long votedCount = hourlyData.stream()
                    .filter(row -> ((Number) row.get("hour")).intValue() == currentHour)
                    .mapToLong(row -> ((Number) row.get("voted_count")).longValue())
                    .findFirst()
                    .orElse(0L);
                    
                jsonBuilder.append("\"").append(hourKey).append("\":{\"voted\":").append(votedCount).append("}");
            }
            jsonBuilder.append("}");
            
            String hourlyJson = jsonBuilder.toString();
            
            // Save or update entity
            Optional<PollDayHourlyTurnout> existing = hourlyRepository.findByAccountIdAndElectionIdAndPollingDate(accountId, electionId, pollingDate);
            PollDayHourlyTurnout hourly = existing.orElse(new PollDayHourlyTurnout());
            
            hourly.setAccountId(accountId);
            hourly.setElectionId(electionId);
            hourly.setPollingDate(pollingDate);
            hourly.setHourlyJson(hourlyJson);
            if (hourly.getComputedAt() == null) {
                hourly.setComputedAt(OffsetDateTime.now());
            }
            hourly.setRefreshedAt(OffsetDateTime.now());
            
            hourly = hourlyRepository.save(hourly);
            log.info("Successfully computed hourly turnout for accountId={}, electionId={}", accountId, electionId);
            
            return hourly;
            
        } catch (Exception e) {
            log.error("Error computing hourly turnout for accountId={}, electionId={}, pollingDate={}", accountId, electionId, pollingDate, e);
            throw e;
        }
    }

    @Transactional
    public PollDayAgeGroupTurnout recomputeAgeGroups(Long accountId, Long electionId, LocalDate pollingDate) {
        try {
            log.info("Computing age group turnout for accountId={}, electionId={}, pollingDate={}", accountId, electionId, pollingDate);
            
            LocalDateTime startOfDay = pollingDate.atStartOfDay();
            LocalDateTime endOfDay = pollingDate.plusDays(1).atStartOfDay();
            
            // Query age group statistics
            String sql = """
                SELECT 
                    CASE 
                        WHEN age BETWEEN 18 AND 30 THEN '18_30'
                        WHEN age BETWEEN 31 AND 40 THEN '30_40' 
                        WHEN age BETWEEN 41 AND 50 THEN '40_50'
                        WHEN age BETWEEN 51 AND 60 THEN '50_60'
                        WHEN age BETWEEN 61 AND 70 THEN '60_70'
                        WHEN age > 70 THEN 'gt_70'
                        ELSE 'unknown'
                    END as age_group,
                    COUNT(*) as registered,
                    COUNT(CASE WHEN has_voted = true 
                              AND voted_timestamp IS NOT NULL
                              AND voted_timestamp >= :startTime::timestamp
                              AND voted_timestamp < :endTime::timestamp 
                          THEN 1 END) as voted
                FROM _voters v
                WHERE v.account_id = :accountId 
                    AND v.election_id = :electionId
                GROUP BY age_group
                ORDER BY age_group
                """;
                
            Map<String, Object> params = Map.of(
                "accountId", accountId,
                "electionId", electionId,
                "startTime", startOfDay.toString(),
                "endTime", endOfDay.toString()
            );
            
            List<Map<String, Object>> ageGroupData = jdbcTemplate.queryForList(sql, params);
            
            // Build JSON for age groups
            StringBuilder jsonBuilder = new StringBuilder("{");
            boolean first = true;
            
            for (Map<String, Object> row : ageGroupData) {
                if (!first) jsonBuilder.append(",");
                first = false;
                
                String ageGroup = (String) row.get("age_group");
                long registered = ((Number) row.get("registered")).longValue();
                long voted = ((Number) row.get("voted")).longValue();
                double percentage = registered > 0 ? (voted * 100.0 / registered) : 0.0;
                
                jsonBuilder.append("\"").append(ageGroup).append("\":{")
                    .append("\"registered\":").append(registered).append(",")
                    .append("\"voted\":").append(voted).append(",")
                    .append("\"pct\":").append(String.format("%.1f", percentage))
                    .append("}");
            }
            jsonBuilder.append("}");
            
            String ageGroupsJson = jsonBuilder.toString();
            
            // Save or update entity
            Optional<PollDayAgeGroupTurnout> existing = ageGroupRepository.findByAccountIdAndElectionIdAndPollingDate(accountId, electionId, pollingDate);
            PollDayAgeGroupTurnout ageGroup = existing.orElse(new PollDayAgeGroupTurnout());
            
            ageGroup.setAccountId(accountId);
            ageGroup.setElectionId(electionId);
            ageGroup.setPollingDate(pollingDate);
            ageGroup.setAgeGroupsJson(ageGroupsJson);
            if (ageGroup.getComputedAt() == null) {
                ageGroup.setComputedAt(OffsetDateTime.now());
            }
            ageGroup.setRefreshedAt(OffsetDateTime.now());
            
            ageGroup = ageGroupRepository.save(ageGroup);
            log.info("Successfully computed age group turnout for accountId={}, electionId={}", accountId, electionId);
            
            return ageGroup;
            
        } catch (Exception e) {
            log.error("Error computing age group turnout for accountId={}, electionId={}, pollingDate={}", accountId, electionId, pollingDate, e);
            throw e;
        }
    }

    @Transactional
    public PollDayBoothSummary recomputeBoothSummary(Long accountId, Long electionId, LocalDate pollingDate) {
        try {
            log.info("Computing booth summary for accountId={}, electionId={}, pollingDate={}", accountId, electionId, pollingDate);
            
            LocalDateTime startOfDay = pollingDate.atStartOfDay();
            LocalDateTime endOfDay = pollingDate.plusDays(1).atStartOfDay();
            
            // Query booth-wise statistics
            String sql = """
                SELECT 
                    booth_number,
                    COUNT(*) as total_registered,
                    COUNT(CASE WHEN has_voted = true 
                              AND voted_timestamp IS NOT NULL
                              AND voted_timestamp >= :startTime::timestamp
                              AND voted_timestamp < :endTime::timestamp 
                          THEN 1 END) as total_voted,
                    MAX(CASE WHEN has_voted = true 
                             AND voted_timestamp IS NOT NULL
                             AND voted_timestamp >= :startTime::timestamp
                             AND voted_timestamp < :endTime::timestamp 
                        THEN voted_timestamp END) as last_vote_time
                FROM _voters v
                WHERE v.account_id = :accountId 
                    AND v.election_id = :electionId
                    AND v.booth_number IS NOT NULL
                GROUP BY booth_number
                ORDER BY booth_number
                """;
                
            Map<String, Object> params = Map.of(
                "accountId", accountId,
                "electionId", electionId,
                "startTime", startOfDay.toString(),
                "endTime", endOfDay.toString()
            );
            
            List<Map<String, Object>> boothData = jdbcTemplate.queryForList(sql, params);
            
            // Build JSON for booth summary
            StringBuilder jsonBuilder = new StringBuilder("{");
            boolean first = true;
            
            for (Map<String, Object> row : boothData) {
                if (!first) jsonBuilder.append(",");
                first = false;
                
                Object boothNumber = row.get("booth_number");
                long totalRegistered = ((Number) row.get("total_registered")).longValue();
                long totalVoted = ((Number) row.get("total_voted")).longValue();
                Object lastVoteTime = row.get("last_vote_time");
                double percentage = totalRegistered > 0 ? (totalVoted * 100.0 / totalRegistered) : 0.0;
                
                jsonBuilder.append("\"").append(boothNumber).append("\":{")
                    .append("\"total\":").append(totalRegistered).append(",")
                    .append("\"voted\":").append(totalVoted).append(",")
                    .append("\"pct\":").append(String.format("%.1f", percentage));
                    
                if (lastVoteTime != null) {
                    jsonBuilder.append(",\"lastVote\":\"").append(lastVoteTime).append("\"");
                }
                
                jsonBuilder.append("}");
            }
            jsonBuilder.append("}");
            
            String boothSummaryJson = jsonBuilder.toString();
            
            // Save or update entity
            Optional<PollDayBoothSummary> existing = boothSummaryRepository.findByAccountIdAndElectionIdAndPollingDate(accountId, electionId, pollingDate);
            PollDayBoothSummary boothSummary = existing.orElse(new PollDayBoothSummary());
            
            boothSummary.setAccountId(accountId);
            boothSummary.setElectionId(electionId);
            boothSummary.setPollingDate(pollingDate);
            boothSummary.setBoothSummaryJson(boothSummaryJson);
            if (boothSummary.getComputedAt() == null) {
                boothSummary.setComputedAt(OffsetDateTime.now());
            }
            boothSummary.setRefreshedAt(OffsetDateTime.now());
            
            boothSummary = boothSummaryRepository.save(boothSummary);
            log.info("Successfully computed booth summary for accountId={}, electionId={}", accountId, electionId);
            
            return boothSummary;
            
        } catch (Exception e) {
            log.error("Error computing booth summary for accountId={}, electionId={}, pollingDate={}", accountId, electionId, pollingDate, e);
            throw e;
        }
    }

    @Transactional
    public PollDayWardAgeGroupTurnout recomputeWardAgeGroups(Long accountId, Long electionId, String partNumber, LocalDate pollingDate) {
        try {
            log.info("Computing ward age group turnout for accountId={}, electionId={}, partNumber={}, pollingDate={}", 
                accountId, electionId, partNumber, pollingDate);
            
            LocalDateTime startOfDay = pollingDate.atStartOfDay();
            LocalDateTime endOfDay = pollingDate.plusDays(1).atStartOfDay();
            
            // Get current year and previous year
            int currentYear = pollingDate.getYear();
            int previousYear = currentYear - 1;
            
            // Query age group statistics with historical comparison and voter categorization
            String sql = """
                WITH age_groups AS (
                    SELECT 
                        v.voter_id,
                        v.age,
                        CASE 
                            WHEN v.age BETWEEN 18 AND 21 THEN '18_21'
                            WHEN v.age BETWEEN 22 AND 25 THEN '22_25'
                            WHEN v.age BETWEEN 26 AND 35 THEN '26_35'
                            WHEN v.age BETWEEN 36 AND 45 THEN '36_45'
                            WHEN v.age BETWEEN 46 AND 59 THEN '46_59'
                            WHEN v.age >= 60 THEN 'expired'
                            ELSE 'unknown'
                        END as age_group,
                        v.has_voted,
                        v.voted_timestamp,
                        EXTRACT(YEAR FROM v.voted_timestamp AT TIME ZONE 'UTC' AT TIME ZONE 'Asia/Kolkata') as vote_year
                    FROM _voters v
                    WHERE v.account_id = :accountId 
                        AND v.election_id = :electionId
                        AND v.part_no = :partNumber
                )
                SELECT 
                    age_group,
                    COUNT(*) as total_voters,
                    COUNT(CASE WHEN has_voted = true AND vote_year = :currentYear
                              AND voted_timestamp >= :startTime::timestamp
                              AND voted_timestamp < :endTime::timestamp 
                          THEN 1 END) as polled_current_year,
                    COUNT(CASE WHEN has_voted = true AND vote_year = :previousYear
                          THEN 1 END) as polled_previous_year,
                    COUNT(CASE WHEN has_voted = false OR has_voted IS NULL OR voted_timestamp IS NULL 
                               OR NOT (voted_timestamp >= :startTime::timestamp AND voted_timestamp < :endTime::timestamp)
                          THEN 1 END) as did_not_vote
                FROM age_groups
                WHERE age_group != 'unknown'
                GROUP BY age_group
                ORDER BY 
                    CASE age_group
                        WHEN '18_21' THEN 1
                        WHEN '22_25' THEN 2
                        WHEN '26_35' THEN 3
                        WHEN '36_45' THEN 4
                        WHEN '46_59' THEN 5
                        WHEN 'expired' THEN 6
                    END
                """;
                
            Map<String, Object> params = Map.of(
                "accountId", accountId,
                "electionId", electionId,
                "partNumber", Integer.parseInt(partNumber),
                "currentYear", currentYear,
                "previousYear", previousYear,
                "startTime", startOfDay.toString(),
                "endTime", endOfDay.toString()
            );
            
            List<Map<String, Object>> ageGroupData = jdbcTemplate.queryForList(sql, params);
            
            // Build JSON for ward age groups with historical comparison
            StringBuilder jsonBuilder = new StringBuilder("{");
            boolean first = true;
            
            long overallTotalVoters = 0;
            long overallPolledCurrent = 0;
            
            for (Map<String, Object> row : ageGroupData) {
                if (!first) jsonBuilder.append(",");
                first = false;
                
                String ageGroup = (String) row.get("age_group");
                long totalVoters = ((Number) row.get("total_voters")).longValue();
                long polledCurrentYear = ((Number) row.get("polled_current_year")).longValue();
                long polledPreviousYear = ((Number) row.get("polled_previous_year")).longValue();
                long didNotVote = ((Number) row.get("did_not_vote")).longValue();
                boolean isFirstTime = "18_21".equals(ageGroup);
                
                overallTotalVoters += totalVoters;
                overallPolledCurrent += polledCurrentYear;
                
                jsonBuilder.append("\"").append(ageGroup).append("\":{")
                    .append("\"total_voters\":").append(totalVoters).append(",")
                    .append("\"polled_").append(currentYear).append("\":").append(polledCurrentYear).append(",")
                    .append("\"polled_").append(previousYear).append("\":").append(polledPreviousYear).append(",")
                    .append("\"did_not_vote\":").append(didNotVote).append(",")
                    .append("\"is_first_time\":").append(isFirstTime)
                    .append("}");
            }
            
            // Add overall summary
            double overallPercentage = overallTotalVoters > 0 ? (overallPolledCurrent * 100.0 / overallTotalVoters) : 0.0;
            if (!first) jsonBuilder.append(",");
            jsonBuilder.append("\"overall\":{")
                .append("\"total_voters\":").append(overallTotalVoters).append(",")
                .append("\"polled_").append(currentYear).append("\":").append(overallPolledCurrent).append(",")
                .append("\"percentage\":").append(String.format("%.1f", overallPercentage))
                .append("}");
            
            jsonBuilder.append("}");
            
            String ageGroupBreakdownJson = jsonBuilder.toString();
            
            // Save or update entity
            Optional<PollDayWardAgeGroupTurnout> existing = wardAgeGroupRepository
                .findByAccountIdAndElectionIdAndPartNumberAndPollingDate(accountId, electionId, partNumber, pollingDate);
            PollDayWardAgeGroupTurnout wardAgeGroup = existing.orElse(new PollDayWardAgeGroupTurnout());
            
            wardAgeGroup.setAccountId(accountId);
            wardAgeGroup.setElectionId(electionId);
            wardAgeGroup.setPartNumber(partNumber);
            wardAgeGroup.setPollingDate(pollingDate);
            wardAgeGroup.setAgeGroupBreakdownJson(ageGroupBreakdownJson);
            if (wardAgeGroup.getComputedAt() == null) {
                wardAgeGroup.setComputedAt(OffsetDateTime.now());
            }
            wardAgeGroup.setRefreshedAt(OffsetDateTime.now());
            
            wardAgeGroup = wardAgeGroupRepository.save(wardAgeGroup);
            log.info("Successfully computed ward age group turnout for accountId={}, electionId={}, partNumber={}", 
                accountId, electionId, partNumber);
            
            return wardAgeGroup;
            
        } catch (Exception e) {
            log.error("Error computing ward age group turnout for accountId={}, electionId={}, partNumber={}, pollingDate={}", 
                accountId, electionId, partNumber, pollingDate, e);
            throw e;
        }
    }
}
