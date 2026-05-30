package com.thedal.reporting.election;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ElectionStatsAggregationService {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ElectionDashboardStatsRepository repository;

    @Transactional
    public ElectionDashboardStats recompute(Long accountId, Long electionId) {
        log.info("Recomputing election stats for accountId={}, electionId={}", accountId, electionId);
        
        try {
            int totalVoters = q("SELECT COUNT(*) FROM _voters v WHERE v.account_id=:a AND v.election_id=:e", accountId, electionId);
            int male = q("SELECT COUNT(*) FROM _voters v WHERE v.account_id=:a AND v.election_id=:e AND lower(v.gender)='male'", accountId, electionId);
            int female = q("SELECT COUNT(*) FROM _voters v WHERE v.account_id=:a AND v.election_id=:e AND lower(v.gender)='female'", accountId, electionId);
            int transgender = q("SELECT COUNT(*) FROM _voters v WHERE v.account_id=:a AND v.election_id=:e AND lower(v.gender) NOT IN ('male','female')", accountId, electionId);

            int a18_30 = q("SELECT COUNT(*) FROM _voters v WHERE v.account_id=:a AND v.election_id=:e AND v.age BETWEEN 18 AND 29", accountId, electionId);
            int a30_40 = q("SELECT COUNT(*) FROM _voters v WHERE v.account_id=:a AND v.election_id=:e AND v.age BETWEEN 30 AND 39", accountId, electionId);
            int a40_50 = q("SELECT COUNT(*) FROM _voters v WHERE v.account_id=:a AND v.election_id=:e AND v.age BETWEEN 40 AND 49", accountId, electionId);
            int a50_60 = q("SELECT COUNT(*) FROM _voters v WHERE v.account_id=:a AND v.election_id=:e AND v.age BETWEEN 50 AND 59", accountId, electionId);
            int a60_70 = q("SELECT COUNT(*) FROM _voters v WHERE v.account_id=:a AND v.election_id=:e AND v.age BETWEEN 60 AND 69", accountId, electionId);
            int agt70 = q("SELECT COUNT(*) FROM _voters v WHERE v.account_id=:a AND v.election_id=:e AND v.age >= 70", accountId, electionId);

            int distinctPincode = q("SELECT COUNT(DISTINCT pincode) FROM _voters v WHERE v.account_id=:a AND v.election_id=:e AND pincode IS NOT NULL", accountId, electionId);
            int distinctMobile = q("SELECT COUNT(DISTINCT mobile_no) FROM _voters v WHERE v.account_id=:a AND v.election_id=:e AND mobile_no IS NOT NULL AND length(trim(mobile_no))>0", accountId, electionId);
            int totalBooth = q("SELECT COUNT(*) FROM part_manager pm WHERE pm.account_id=:a AND pm.election_id=:e", accountId, electionId);
            int totalFamily = q("SELECT COUNT(DISTINCT family_id) FROM _voters v WHERE v.account_id=:a AND v.election_id=:e AND family_id IS NOT NULL", accountId, electionId);

            int firstTime = q("SELECT COUNT(*) FROM _voters v WHERE v.account_id=:a AND v.election_id=:e AND v.age BETWEEN 18 AND 21", accountId, electionId);
            int senior = a60_70 + agt70;  // Senior citizens include both 60-69 and 70+ age groups
            int superSenior = agt70;

            // New fields computation
            int dateOfBirth = q("SELECT COUNT(*) FROM _voters v WHERE v.account_id=:a AND v.election_id=:e AND v.dob IS NOT NULL", accountId, electionId);
            int starVoters = q("SELECT COUNT(*) FROM _voters v WHERE v.account_id=:a AND v.election_id=:e AND v.star_number = true", accountId, electionId);
            int religionCount = q("SELECT COUNT(DISTINCT religion_id) FROM _voters v WHERE v.account_id=:a AND v.election_id=:e AND religion_id IS NOT NULL", accountId, electionId);
            int casteCount = q("SELECT COUNT(DISTINCT caste_id) FROM _voters v WHERE v.account_id=:a AND v.election_id=:e AND caste_id IS NOT NULL", accountId, electionId);
            int totalMobileCount = q("SELECT COUNT(*) FROM _voters v WHERE v.account_id=:a AND v.election_id=:e AND mobile_no IS NOT NULL AND length(trim(mobile_no))>0", accountId, electionId);

            // Additional aggregate fields
            int totalSchool = q("SELECT COUNT(DISTINCT school_name) FROM part_manager pm WHERE pm.account_id=:a AND pm.election_id=:e AND school_name IS NOT NULL", accountId, electionId);
            int crossBoothFamily = q("SELECT COUNT(DISTINCT family_id) FROM (SELECT family_id FROM _voters WHERE account_id=:a AND election_id=:e AND family_id IS NOT NULL GROUP BY family_id HAVING COUNT(DISTINCT booth_number) > 1) sub", accountId, electionId);
            int oneVoterFamily = q("SELECT COUNT(DISTINCT family_id) FROM _voters v WHERE v.account_id=:a AND v.election_id=:e AND family_id IS NOT NULL AND family_count = 1", accountId, electionId);
            int casteCategoryCount = q("SELECT COUNT(*) FROM caste_category WHERE account_id=:a AND election_id=:e", accountId, electionId);
            int subCasteCount = q("SELECT COUNT(*) FROM sub_caste WHERE account_id=:a AND election_id=:e", accountId, electionId);
            int languageCount = q("SELECT COUNT(*) FROM language WHERE account_id=:a AND election_id=:e", accountId, electionId);
            int partyAffiliationCount = q("SELECT COUNT(*) FROM parties WHERE account_id=:a AND election_id=:e", accountId, electionId);
            int schemesCount = q("SELECT COUNT(*) FROM benefit_schemes WHERE account_id=:a AND election_id=:e", accountId, electionId);

            OffsetDateTime now = OffsetDateTime.now();
            Optional<ElectionDashboardStats> existing = repository.findByAccountIdAndElectionId(accountId, electionId);
            ElectionDashboardStats stats = existing.orElseGet(ElectionDashboardStats::new);
            
            stats.setAccountId(accountId);
            stats.setElectionId(electionId);
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
            
            ElectionDashboardStats savedStats = repository.save(stats);
            log.info("Successfully recomputed election stats: accountId={}, electionId={}, totalBooth={}, totalVoters={}", 
                    accountId, electionId, totalBooth, totalVoters);
            return savedStats;
            
        } catch (Exception ex) {
            log.error("Failed to recompute election stats for accountId={}, electionId={}", accountId, electionId, ex);
            throw new RuntimeException("Election stats recomputation failed", ex);
        }
    }

    @Transactional
    public void forceAggregate(Long accountId, Long electionId) {
        log.info("Force aggregating election stats for accountId={}, electionId={}", accountId, electionId);
        recompute(accountId, electionId);
    }

    private int q(String sql, Long accountId, Long electionId) {
        Integer val = jdbcTemplate.queryForObject(sql, Map.of("a", accountId, "e", electionId), Integer.class);
        return val == null ? 0 : val;
    }
}
