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

@Service
@Slf4j
@RequiredArgsConstructor
public class ElectionStatsAggregationService {

    private final NamedParameterJdbcTemplate primaryJdbcTemplate;
    private final ElectionDashboardStatsRepository statsRepository;

    @Transactional
    public void aggregateAllDistinctElections(String distinctSql) {
        try {
            List<Map<String, Object>> elections = primaryJdbcTemplate.queryForList(distinctSql, Map.of());
            for (Map<String, Object> row : elections) {
                Long accountId = ((Number) row.get("account_id")).longValue();
                Long electionId = ((Number) row.get("election_id")).longValue();
                aggregateOne(accountId, electionId);
            }
        } catch (Exception ex) {
            log.error("[AGGREGATION] failure enumerating elections", ex);
        }
    }

    public void forceAggregate(Long accountId, Long electionId) {
        try {
            aggregateOne(accountId, electionId, null);
        } catch (Exception ex) {
            log.error("[AGGREGATION] manual trigger failed account={} election={}", accountId, electionId, ex);
            throw ex;
        }
    }

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
                log.error("[AGGREGATION] manual trigger failed account={} election={} part=null", accountId, electionId, ex);
            }
            
            // Compute each part in separate try-catch to allow partial success
            for (String partNo : allParts) {
                try {
                    aggregateOne(accountId, electionId, partNo);
                } catch (Exception ex) {
                    log.error("[AGGREGATION] failed for account={} election={} part={}", accountId, electionId, partNo, ex);
                    // Continue processing other parts
                }
            }
        }
    }

    private void aggregateOne(Long accountId, Long electionId) {
        aggregateOne(accountId, electionId, null);
    }

    @Transactional
    private void aggregateOne(Long accountId, Long electionId, String partNumber) {
        String partFilter = partNumber != null ? " AND v.part_no=CAST(:p AS INTEGER)" : "";
        Map<String, Object> params = partNumber != null ? 
            Map.of("a", accountId, "e", electionId, "p", partNumber) : 
            Map.of("a", accountId, "e", electionId);

        int totalVoters = q("SELECT COUNT(*) FROM _voters v WHERE v.account_id=:a AND v.election_id=:e" + partFilter, params);
        int male = q("SELECT COUNT(*) FROM _voters v WHERE v.account_id=:a AND v.election_id=:e AND upper(trim(v.gender)) IN ('M','MALE')" + partFilter, params);
        int female = q("SELECT COUNT(*) FROM _voters v WHERE v.account_id=:a AND v.election_id=:e AND upper(trim(v.gender)) IN ('F','FEMALE')" + partFilter, params);
        int transgender = q("SELECT COUNT(*) FROM _voters v WHERE v.account_id=:a AND v.election_id=:e AND upper(trim(v.gender)) NOT IN ('M','MALE','F','FEMALE')" + partFilter, params);

        int a18_30 = q("SELECT COUNT(*) FROM _voters v WHERE v.account_id=:a AND v.election_id=:e AND v.age BETWEEN 18 AND 29" + partFilter, params);
        int a30_40 = q("SELECT COUNT(*) FROM _voters v WHERE v.account_id=:a AND v.election_id=:e AND v.age BETWEEN 30 AND 39" + partFilter, params);
        int a40_50 = q("SELECT COUNT(*) FROM _voters v WHERE v.account_id=:a AND v.election_id=:e AND v.age BETWEEN 40 AND 49" + partFilter, params);
        int a50_60 = q("SELECT COUNT(*) FROM _voters v WHERE v.account_id=:a AND v.election_id=:e AND v.age BETWEEN 50 AND 59" + partFilter, params);
        int a60_70 = q("SELECT COUNT(*) FROM _voters v WHERE v.account_id=:a AND v.election_id=:e AND v.age BETWEEN 60 AND 69" + partFilter, params);
        int agt70 = q("SELECT COUNT(*) FROM _voters v WHERE v.account_id=:a AND v.election_id=:e AND v.age >= 70" + partFilter, params);

        int distinctPincode = q("SELECT COUNT(DISTINCT pincode) FROM _voters v WHERE v.account_id=:a AND v.election_id=:e AND pincode IS NOT NULL" + partFilter, params);
        int distinctMobile = q("SELECT COUNT(DISTINCT mobile_no) FROM _voters v WHERE v.account_id=:a AND v.election_id=:e AND mobile_no IS NOT NULL AND length(trim(mobile_no))>0" + partFilter, params);
        
        // For part-level, booth count is always 1 (the part itself). For election-wide, count all booths.
        int totalBooth = partNumber != null ? 1 : q("SELECT COUNT(*) FROM part_manager pm WHERE pm.account_id=:a AND pm.election_id=:e", params);
        int totalFamily = q("SELECT COUNT(DISTINCT family_id) FROM _voters v WHERE v.account_id=:a AND v.election_id=:e AND family_id IS NOT NULL" + partFilter, params);

        int firstTime = q("SELECT COUNT(*) FROM _voters v WHERE v.account_id=:a AND v.election_id=:e AND v.age BETWEEN 18 AND 21" + partFilter, params);
        int senior = a60_70 + agt70;
        int superSenior = agt70;

        // New fields computation
        int dateOfBirth = q("SELECT COUNT(*) FROM _voters v WHERE v.account_id=:a AND v.election_id=:e AND v.dob IS NOT NULL" + partFilter, params);
        int starVoters = q("SELECT COUNT(*) FROM _voters v WHERE v.account_id=:a AND v.election_id=:e AND v.star_number = true" + partFilter, params);
        int religionCount = q("SELECT COUNT(DISTINCT religion_id) FROM _voters v WHERE v.account_id=:a AND v.election_id=:e AND religion_id IS NOT NULL" + partFilter, params);
        int casteCount = q("SELECT COUNT(DISTINCT caste_id) FROM _voters v WHERE v.account_id=:a AND v.election_id=:e AND caste_id IS NOT NULL" + partFilter, params);
        int totalMobileCount = q("SELECT COUNT(*) FROM _voters v WHERE v.account_id=:a AND v.election_id=:e AND mobile_no IS NOT NULL AND length(trim(mobile_no))>0" + partFilter, params);
        
        // Gender-wise mobile counts
        int maleMobileCount = q("SELECT COUNT(*) FROM _voters v WHERE v.account_id=:a AND v.election_id=:e AND upper(trim(v.gender)) IN ('M','MALE') AND mobile_no IS NOT NULL AND length(trim(mobile_no))>0" + partFilter, params);
        int femaleMobileCount = q("SELECT COUNT(*) FROM _voters v WHERE v.account_id=:a AND v.election_id=:e AND upper(trim(v.gender)) IN ('F','FEMALE') AND mobile_no IS NOT NULL AND length(trim(mobile_no))>0" + partFilter, params);
        int transgenderMobileCount = q("SELECT COUNT(*) FROM _voters v WHERE v.account_id=:a AND v.election_id=:e AND upper(trim(v.gender)) NOT IN ('M','MALE','F','FEMALE') AND mobile_no IS NOT NULL AND length(trim(mobile_no))>0" + partFilter, params);
        
        // Gender-wise DOB counts
        int maleDateOfBirthCount = q("SELECT COUNT(*) FROM _voters v WHERE v.account_id=:a AND v.election_id=:e AND upper(trim(v.gender)) IN ('M','MALE') AND v.dob IS NOT NULL" + partFilter, params);
        int femaleDateOfBirthCount = q("SELECT COUNT(*) FROM _voters v WHERE v.account_id=:a AND v.election_id=:e AND upper(trim(v.gender)) IN ('F','FEMALE') AND v.dob IS NOT NULL" + partFilter, params);
        int transgenderDateOfBirthCount = q("SELECT COUNT(*) FROM _voters v WHERE v.account_id=:a AND v.election_id=:e AND upper(trim(v.gender)) NOT IN ('M','MALE','F','FEMALE') AND v.dob IS NOT NULL" + partFilter, params);

        // Additional aggregate fields
        int totalSchool = partNumber != null ? 
            q("SELECT COUNT(DISTINCT school_name) FROM part_manager pm WHERE pm.account_id=:a AND pm.election_id=:e AND pm.part_no=:p AND school_name IS NOT NULL", params) :
            q("SELECT COUNT(DISTINCT school_name) FROM part_manager pm WHERE pm.account_id=:a AND pm.election_id=:e AND school_name IS NOT NULL", params);
        int crossBoothFamily = q("SELECT COUNT(DISTINCT family_id) FROM (SELECT family_id FROM _voters WHERE account_id=:a AND election_id=:e" + partFilter + " AND family_id IS NOT NULL GROUP BY family_id HAVING COUNT(DISTINCT booth_number) > 1) sub", params);
        int oneVoterFamily = q("SELECT COUNT(DISTINCT family_id) FROM _voters v WHERE v.account_id=:a AND v.election_id=:e" + partFilter + " AND family_id IS NOT NULL AND family_count = 1", params);
        int casteCategoryCount = q("SELECT COUNT(*) FROM caste_category WHERE account_id=:a AND election_id=:e", params);
        int subCasteCount = q("SELECT COUNT(*) FROM sub_caste WHERE account_id=:a AND election_id=:e", params);
        int languageCount = q("SELECT COUNT(*) FROM language WHERE account_id=:a AND election_id=:e", params);
        int partyAffiliationCount = q("SELECT COUNT(*) FROM parties WHERE account_id=:a AND election_id=:e", params);
        int schemesCount = q("SELECT COUNT(*) FROM benefit_schemes WHERE account_id=:a AND election_id=:e", params);

        OffsetDateTime now = OffsetDateTime.now();
        Optional<ElectionDashboardStats> existing = partNumber != null ? 
            statsRepository.findByAccountIdAndElectionIdAndPartNo(accountId, electionId, partNumber) :
            statsRepository.findByAccountIdAndElectionId(accountId, electionId);
        ElectionDashboardStats stats = existing.orElseGet(ElectionDashboardStats::new);
        stats.setAccountId(accountId);
        stats.setElectionId(electionId);
        stats.setPartNo(partNumber);
        stats.setTotalBooth(totalBooth);
        stats.setTotalVoters(totalVoters);
        stats.setTotalFamily(totalFamily);
        stats.setDistinctPincodeCount(distinctPincode);
        stats.setDistinctMobileCount(distinctMobile);
        stats.setMale(male);
        stats.setFemale(female);
        stats.setTransgender(transgender);
        stats.setAge18To30(a18_30);
        stats.setAge30To40(a30_40);
        stats.setAge40To50(a40_50);
        stats.setAge50To60(a50_60);
        stats.setAge60To70(a60_70);
        stats.setAgeGreaterThan70(agt70);
        stats.setFirstTimeVoters(firstTime);
        stats.setSeniorCitizens(senior);
        stats.setSuperSeniors(superSenior);
        
        // Set new fields
        stats.setDateOfBirth(dateOfBirth);
        stats.setStarVoters(starVoters);
        stats.setReligionCount(religionCount);
        stats.setCasteCount(casteCount);
        stats.setTotalMobileCount(totalMobileCount);
        stats.setMaleMobileCount(maleMobileCount);
        stats.setFemaleMobileCount(femaleMobileCount);
        stats.setTransgenderMobileCount(transgenderMobileCount);
        stats.setMaleDateOfBirthCount(maleDateOfBirthCount);
        stats.setFemaleDateOfBirthCount(femaleDateOfBirthCount);
        stats.setTransgenderDateOfBirthCount(transgenderDateOfBirthCount);
        
        // Set additional aggregate fields
        stats.setTotalSchool(totalSchool);
        stats.setCrossBoothFamily(crossBoothFamily);
        stats.setOneVoterFamily(oneVoterFamily);
        stats.setCasteCategoryCount(casteCategoryCount);
        stats.setSubCasteCount(subCasteCount);
        stats.setLanguageCount(languageCount);
        stats.setPartyAffiliationCount(partyAffiliationCount);
        stats.setSchemesCount(schemesCount);
        
        if (stats.getComputedAt() == null) {
            stats.setComputedAt(now);
        }
        stats.setRefreshedAt(now);
        statsRepository.save(stats);
        log.debug("[AGGREGATION] updated account={} election={} part={} totalVoters={}", 
            accountId, electionId, partNumber, totalVoters);
    }

    private List<String> getPartNumbersForElection(Long accountId, Long electionId) {
        // Only fetch numeric part numbers since we need to CAST to INTEGER for _voters.part_no comparison
        String sql = "SELECT DISTINCT part_no FROM part_manager WHERE account_id=:a AND election_id=:e AND part_no IS NOT NULL AND part_no ~ '^\\d+$' ORDER BY part_no";
        return primaryJdbcTemplate.queryForList(sql, Map.of("a", accountId, "e", electionId), String.class);
    }

    private int q(String sql, Map<String, Object> params) {
        Integer val = primaryJdbcTemplate.queryForObject(sql, params, Integer.class);
        return val == null ? 0 : val;
    }

    @Deprecated
    private int q(String sql, Long accountId, Long electionId) {
        return q(sql, Map.of("a", accountId, "e", electionId));
    }
}
