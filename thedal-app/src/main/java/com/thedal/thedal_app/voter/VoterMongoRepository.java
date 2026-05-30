package com.thedal.thedal_app.voter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bson.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.thedal.thedal_app.voter.dto.AadhaarStatsProjection;
import com.thedal.thedal_app.voter.dto.BoothAadhaarStatsProjection;
import com.thedal.thedal_app.voter.dto.BoothGenderStatsProjection;
import com.thedal.thedal_app.voter.dto.BoothMembershipStatsProjection;
import com.thedal.thedal_app.voter.dto.GenderStatsProjection;
import com.thedal.thedal_app.voter.dto.MembershipStatsProjection;

public interface VoterMongoRepository extends MongoRepository<VoterMongo, String>, VoterMongoRepositoryCustom {
    Optional<VoterMongo> findByAadhaarNumberAndElectionIdAndAccountId(String aadhaarNumber, Long electionId, Long accountId);
        
    //Optional<VoterMongo> findByEpicNumberAndElectionIdAndAccountId(String epicNumber, Long electionId, Long accountId);
    @Query("{ 'epicNumber': { $regex: ?0, $options: 'i' }, 'electionId': ?1, 'accountId': ?2 }")
    Optional<VoterMongo> findByEpicNumberAndElectionIdAndAccountId(String epicNumber, Long electionId, Long accountId);
    
    @Query("{ 'voterId': { $regex: ?0, $options: 'i' }, 'electionId': ?1, 'accountId': ?2 }")
    Optional<VoterMongo> findByVoterIdAndElectionIdAndAccountId(String voterId, Long electionId, Long accountId);
    //Optional<VoterMongo> findByVoterIdAndElectionIdAndAccountId(String epicNumber, Long electionId, Long accountId);
    boolean existsByEpicNumberAndElectionIdAndAccountId(String epicNumber, Long electionId, Long accountId);
    
    // Simple method for basic listing without complex filters
    @Query(value = "{ 'accountId': ?0, 'electionId': ?1 }", sort = "{ 'partNo': 1, 'serialNo': 1 }")
    Slice<VoterMongo> findByAccountIdAndElectionId(Long accountId, Long electionId, Pageable pageable);
    
        @Query("{ 'voterId': { $in: ?0 }, 'accountId': ?1, 'electionId': ?2 }")
        List<Object[]> findBenefitSchemesByVoterIds(List<Long> voterIds, Long accountId, Long electionId);

        @Query("{ 'voterId': { $in: ?0 }, 'accountId': ?1, 'electionId': ?2 }")
        List<Object[]> findFeedbackIssuesByVoterIds(List<Long> voterIds, Long accountId, Long electionId);

        @Query("{ 'voterId': { $in: ?0 }, 'accountId': ?1, 'electionId': ?2 }")
        List<Object[]> findVoterHistoriesByVoterIds(List<Long> voterIds, Long accountId, Long electionId);

        @Query("{ 'voterId': { $in: ?0 } }")
        List<Object[]> findLanguagesByVoterIds(List<Long> voterIds); 
        
        @Query("{ '_id': { $in: ?0 }, 'languageIds': { $ne: [] } }")
        List<VoterMongo> findVotersWithLanguages(List<String> voterIds);

        @Query("{ '_id': { $in: ?0 }, 'accountId': ?1, 'electionId': ?2, 'benefitSchemeIds': { $ne: [] } }")
        List<VoterMongo> findVotersWithBenefitSchemes(List<String> voterIds, Long accountId, Long electionId);

        @Query("{ '_id': { $in: ?0 }, 'accountId': ?1, 'electionId': ?2, 'feedbackIssueIds': { $ne: [] } }")
        List<VoterMongo> findVotersWithFeedbackIssues(List<String> voterIds, Long accountId, Long electionId);

        @Query("{ '_id': { $in: ?0 }, 'accountId': ?1, 'electionId': ?2, 'voterHistoryIds': { $ne: [] } }")
        
        List<VoterMongo> findVotersWithVoterHistories(List<String> voterIds, Long accountId, Long electionId);
        
        @Aggregation(pipeline = {
                "{ $match: { "
                    + "accountId: ?0, "
                    + "electionId: ?1, "
                    + "voterId: { $in: [ ?2, '' ] }, "
                    + "epicNumber: { $in: [ ?3, '' ] }, "
                    + "boothNumber: { $in: ?4 }, "
                    + "familyId: { $in: [ ?5, null ] }, "
                    + "voterFnameEn: { $in: ?6 }, "
                    + "voterLnameEn: { $in: ?7 }, "
                    + "voterFnameL1: { $in: ?8 }, "
                    + "voterFnameL2: { $in: ?9 }, "
                    + "rlnFnameEn: { $in: ?10 }, "
                    + "rlnLnameEn: { $in: ?11 }, "
                    + "partyId: { $in: ?12 }, "
                    + "religionId: { $in: ?13 }, "
                    + "voterHistoryIds: { $in: ?14 }, "
                    + "$or: [ "
                    + "  { age: ?15 }, "
                    + "  { age: null, ?15: -1 } "
                    + "], "
                    + "$or: [ "
                    + "  { ?16: false }, "
                    + "  { $and: [ { ?16: true }, { age: null } ] }, "
                    + "  { $and: [ { age: { $ne: null } }, { age: { $gte: ?17 } }, { age: { $lte: ?18 } } ] } "
                    + "], "
                    + "gender: { $in: ?19 }, "
                    + "$or: [ "
                    + "  { ?20: false }, "
                    + "  { $and: [ { ?20: true }, { dob: { $ne: null } } ] } "
                    + "], "
                    + "$or: [ "
                    + "  { ?21: false }, "
                    + "  { $and: [ { ?21: true }, { starNumber: true } ] } "
                    + "], "
                    + "availabilityId: { $in: ?22 } "
                + "} }",
                "{ $sort: ?23 }",
                "{ $skip: ?#{#pageable.offset} }",
                "{ $limit: ?#{#pageable.pageSize} }"
        })
        Slice<VoterMongo> findByAccountIdAndElectionIdAndFilters(
                Long accountId, Long electionId, String voterId, String epicNumber,
                List<Integer> boothNumbers, UUID familyId, List<String> voterFnameEn,
                List<String> voterLnameEn, List<String> voterFnameL1, List<String> voterFnameL2,
                List<String> rlnFnameEn, List<String> rlnLnameEn, List<Long> partyIds,
                List<Long> religionIds, List<Long> voterHistoryIds, Integer age,
                Boolean includeUnknownAge, Integer minAge, Integer maxAge, List<String> genders,
                Boolean hasDob, Boolean starNumber, List<Long> availabilityIds, Pageable pageable,
                Document sort
        );
            
        @Aggregation(pipeline = {
                "{ $match: { "
                    + "accountId: ?0, "
                    + "electionId: ?1, "
                    + "voterId: { $in: [ ?2, '' ] }, "
                    + "epicNumber: { $in: [ ?3, '' ] }, "
                    + "boothNumber: { $in: ?4 }, "
                    + "familyId: { $in: [ ?5, null ] }, "
                    + "voterFnameEn: { $in: ?6 }, "
                    + "voterLnameEn: { $in: ?7 }, "
                    + "voterFnameL1: { $in: ?8 }, "
                    + "voterFnameL2: { $in: ?9 }, "
                    + "rlnFnameEn: { $in: ?10 }, "
                    + "rlnLnameEn: { $in: ?11 }, "
                    + "partyId: { $in: ?12 }, "
                    + "religionId: { $in: ?13 }, "
                    + "$or: [ "
                    + "  { age: ?14 }, "
                    + "  { age: null, ?14: -1 } "
                    + "], "
                    + "$or: [ "
                    + "  { ?15: false }, "
                    + "  { $and: [ { ?15: true }, { age: null } ] }, "
                    + "  { $and: [ { age: { $ne: null } }, { age: { $gte: ?16 } }, { age: { $lte: ?17 } } ] } "
                    + "], "
                    + "gender: { $in: ?18 }, "
                    + "$or: [ "
                    + "  { ?19: false }, "
                    + "  { $and: [ { ?19: true }, { dob: { $ne: null } } ] } "
                    + "], "
                    + "$or: [ "
                    + "  { ?20: false }, "
                    + "  { $and: [ { ?20: true }, { starNumber: true } ] } "
                    + "], "
                    + "availabilityId: { $in: ?21 } "
                + "} }",
                "{ $group: { "
                    + "_id: null, "
                    + "maleCount: { $sum: { $cond: [ { $eq: [ '$gender', 'male' ] }, 1, 0 ] } }, "
                    + "femaleCount: { $sum: { $cond: [ { $eq: [ '$gender', 'female' ] }, 1, 0 ] } }, "
                    + "otherCount: { $sum: { $cond: [ { $eq: [ '$gender', 'other' ] }, 1, 0 ] } }, "
                    + "totalCount: { $sum: 1 } "
                + "} }"
        })
        GenderStatsProjection getFilteredGenderStats(
                Long accountId, Long electionId, String voterId, String epicNumber,
                List<Integer> boothNumbers, UUID familyId, List<String> voterFnameEn,
                List<String> voterLnameEn, List<String> voterFnameL1, List<String> voterFnameL2,
                List<String> rlnFnameEn, List<String> rlnLnameEn, List<Long> partyIds,
                List<Long> religionIds, Integer age, Boolean includeUnknownAge,
                Integer minAge, Integer maxAge, List<String> genders, Boolean hasDob,
                Boolean starNumber, List<Long> availabilityIds
        );


        // Booth-wise gender stats aggregation
        @Aggregation(pipeline = {
                "{ $match: { accountId: ?0, electionId: ?1, boothNumber: { $in: ?2 } } }",
                "{ $group: { " +
                    "_id: '$boothNumber', " +
                    "maleCount: { $sum: { $cond: [ { $eq: [ '$gender', 'male' ] }, 1, 0 ] } }, " +
                    "femaleCount: { $sum: { $cond: [ { $eq: [ '$gender', 'female' ] }, 1, 0 ] } }, " +
                    "otherCount: { $sum: { $cond: [ { $eq: [ '$gender', 'other' ] }, 1, 0 ] } }, " +
                    "totalCount: { $sum: 1 } " +
                "} }"
        })
        List<BoothGenderStatsProjection> getBoothGenderStats(Long accountId, Long electionId, List<Integer> boothNumbers);

        // Aadhaar verification stats aggregation
        @Aggregation(pipeline = {
                "{ $match: { accountId: ?0, electionId: ?1 } }",
                "{ $group: { " +
                    "_id: null, " +
                    "verifiedCount: { $sum: { $cond: [ { $eq: [ '$aadhaarVerified', true ] }, 1, 0 ] } }, " +
                    "unverifiedCount: { $sum: { $cond: [ { $eq: [ '$aadhaarVerified', false ] }, 1, 0 ] } }, " +
                    "totalCount: { $sum: 1 } " +
                "} }"
        })
        AadhaarStatsProjection getAadhaarStats(Long accountId, Long electionId);

        // Membership verification stats aggregation
        @Aggregation(pipeline = {
                "{ $match: { accountId: ?0, electionId: ?1 } }",
                "{ $group: { " +
                    "_id: null, " +
                    "verifiedCount: { $sum: { $cond: [ { $eq: [ '$memberVerified', true ] }, 1, 0 ] } }, " +
                    "unverifiedCount: { $sum: { $cond: [ { $eq: [ '$memberVerified', false ] }, 1, 0 ] } }, " +
                    "totalCount: { $sum: 1 } " +
                "} }"
        })
        MembershipStatsProjection getMembershipStats(Long accountId, Long electionId);

        // Booth-wise Aadhaar verification stats aggregation
        @Aggregation(pipeline = {
                "{ $match: { accountId: ?0, electionId: ?1, boothNumber: { $in: ?2 } } }",
                "{ $group: { " +
                    "_id: '$boothNumber', " +
                    "verifiedCount: { $sum: { $cond: [ { $eq: [ '$aadhaarVerified', true ] }, 1, 0 ] } }, " +
                    "unverifiedCount: { $sum: { $cond: [ { $eq: [ '$aadhaarVerified', false ] }, 1, 0 ] } }, " +
                    "totalCount: { $sum: 1 } " +
                "} }"
        })
        List<BoothAadhaarStatsProjection> getBoothAadhaarStats(Long accountId, Long electionId, List<Integer> boothNumbers);

        // Booth-wise Membership verification stats aggregation
        @Aggregation(pipeline = {
                "{ $match: { accountId: ?0, electionId: ?1, boothNumber: { $in: ?2 } } }",
                "{ $group: { " +
                    "_id: '$boothNumber', " +
                    "verifiedCount: { $sum: { $cond: [ { $eq: [ '$memberVerified', true ] }, 1, 0 ] } }, " +
                    "unverifiedCount: { $sum: { $cond: [ { $eq: [ '$memberVerified', false ] }, 1, 0 ] } }, " +
                    "totalCount: { $sum: 1 } " +
                "} }"
        })
        List<BoothMembershipStatsProjection> getBoothMembershipStats(Long accountId, Long electionId, List<Integer> boothNumbers);
     
		Page<VoterMongo> findByElectionIdAndAccountId(Long electionId, Long accountId, Pageable pageable);
		
		long countByAccountIdAndElectionId(Long accountId, Long electionId);
		
		void deleteByAccountIdAndElectionIdAndEpicNumber(Long accountId, Long electionId, String epicNumber);
		
//		 @Query("{'accountId': ?0, 'electionId': ?1, 'epicNumber': { $in: ?2 }}")
//		 void deleteByAccountIdAndElectionIdAndEpicNumberIn(Long accountId, Long electionId, List<String> epicNumbers);
//		
		void deleteByAccountIdAndElectionIdAndEpicNumberIn(Long accountId, Long electionId, List<String> epicNumber);
		 void deleteByAccountIdAndElectionId(Long accountId, Long electionId);

    
    // Family mapping migration methods
    VoterMongo findByAccountIdAndElectionIdAndEpicNumber(Long accountId, Long electionId, String epicNumber);
    
    // Count queries for family mapping statistics
    @Query("{ 'familyId': { $ne: null } }")
    long countVotersWithFamilyMappings();

	Optional<VoterMongo> findByEpicNumber(String epicNumber);
    
}