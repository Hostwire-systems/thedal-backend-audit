package com.thedal.thedal_app.voter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thedal.thedal_app.election.ElectionRepository;
import com.thedal.thedal_app.election.PartManagerMongo;
import com.thedal.thedal_app.election.PartManagerMongoRepository;
import com.thedal.thedal_app.general.RequestDetailsService;
import com.thedal.thedal_app.role.Role;
import com.thedal.thedal_app.settings.electionsettings.AvailabilityMongo;
import com.thedal.thedal_app.settings.electionsettings.AvailabilityMongoRepository;
import com.thedal.thedal_app.settings.electionsettings.BenefitSchemesMongo;
import com.thedal.thedal_app.settings.electionsettings.BenefitSchemesMongoRepository;
import com.thedal.thedal_app.settings.electionsettings.CasteMongo;
import com.thedal.thedal_app.settings.electionsettings.CasteMongoRepository;
import com.thedal.thedal_app.settings.electionsettings.CasteCategoryMongo;
import com.thedal.thedal_app.settings.electionsettings.CasteCategoryMongoRepository;
import com.thedal.thedal_app.settings.electionsettings.FeedbackIssueMongo;
import com.thedal.thedal_app.settings.electionsettings.FeedbackIssueMongoRepository;
import com.thedal.thedal_app.settings.electionsettings.LanguageMongo;
import com.thedal.thedal_app.settings.electionsettings.LanguageMongoRepository;
import com.thedal.thedal_app.settings.electionsettings.PartyMongo;
import com.thedal.thedal_app.settings.electionsettings.PartyMongoRepository;
import com.thedal.thedal_app.settings.electionsettings.ReligionMongo;
import com.thedal.thedal_app.settings.electionsettings.ReligionMongoRepository;
import com.thedal.thedal_app.settings.electionsettings.SubCasteMongo;
import com.thedal.thedal_app.settings.electionsettings.SubCasteMongoRepository;
import com.thedal.thedal_app.settings.electionsettings.VoterHistoryMongo;
import com.thedal.thedal_app.settings.electionsettings.VoterHistoryMongoRepository;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;
import com.thedal.thedal_app.user.UserRepo;
import com.thedal.thedal_app.volunteer.VolunteerEntity;
import com.thedal.thedal_app.volunteer.VolunteerRepository;
import com.thedal.thedal_app.voter.dto.BoothGenderStatsDTO;
import com.thedal.thedal_app.voter.dto.BoothVerificationStatsDTO;
import com.thedal.thedal_app.voter.dto.GenderStatsDTO;
import com.thedal.thedal_app.voter.dto.VerificationStatsDTO;
import com.thedal.thedal_app.voter.dto.AddressedVoterStatsDTO;
import com.thedal.thedal_app.voter.dto.VoterResponseDTO1;
import com.thedal.thedal_app.voter.dto.VoterResponseMongoDTO;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class VoterMongoService {

    @Autowired
    private VoterMongoRepository voterMongoRepository;

    @Autowired
    private ElectionRepository electionRepository;

    @Autowired
    private VolunteerRepository volunteerRepository;
    
    @Autowired
    private UserRepo userRepo;
    
    @Autowired
    private RequestDetailsService requestDetails;
    
    @Autowired
    private LanguageMongoRepository languageMongoRepository; 
    
    @Autowired
    private BenefitSchemesMongoRepository benefitSchemesMongoRepository; 
    
    @Autowired
    private FeedbackIssueMongoRepository feedbackIssueMongoRepository; 
    
    @Autowired
    private VoterHistoryMongoRepository voterHistoryMongoRepository;
    
    private final ObjectMapper objectMapper = new ObjectMapper(); 
    
    @Autowired
    private PartyMongoRepository partyMongoRepository; 
    
    @Autowired
    private ReligionMongoRepository religionMongoRepository; 
    
    @Autowired
    private AvailabilityMongoRepository availabilityMongoRepository; 
    
    @Autowired
    private PartManagerMongoRepository partManagerMongoRepository;
    
    @Autowired
    private CasteMongoRepository casteMongoRepository;
    
    @Autowired
    private SubCasteMongoRepository subCasteMongoRepository;
    
    @Autowired
    private CasteCategoryMongoRepository casteCategoryMongoRepository;
    
    @Autowired
    private MongoTemplate mongoTemplate;

    // Validation cache to avoid repeated DB hits
    private final Map<String, Boolean> validationCache = new ConcurrentHashMap<>();
    private final Map<String, Long> validationCacheTimes = new ConcurrentHashMap<>();
    private static final long VALIDATION_CACHE_TTL = 300_000; // 5 minutes
    
    @Transactional(readOnly = true)
    public VoterResponseMongoDTO getVoters(
            Long accountId, String voterId, String epicNumber, Long electionId,
            List<Integer> boothNumberList, UUID familyId, List<String> voterFnameEnList,
            List<String> voterLnameEn, List<String> voterFnameL1, List<String> voterFnameL2,
            List<String> relationFirstNameEn, List<String> relationLastNameEn,
            List<String> voterHistoryNameList, List<String> partyNameList, String religionName,
            Integer age, Integer minAge, Integer maxAge, Boolean includeUnknownAge,
            List<String> genderList, Boolean dobFilter, Boolean starNumber, String description,
            String casteCategoryName, Boolean findDuplicates,Long serialNo, Pageable pageable, Document sortDocument) {

        long startTime = System.currentTimeMillis();
        
        try {
            log.debug("Starting MongoDB getVoters - electionId={}, booths={}", electionId, boothNumberList);
            
            // Fast validation with caching
            validateElectionOwnershipFast(electionId, accountId);
            
            // Role-based booth number filtering
            Long userId = requestDetails.getCurrentUserId();
            Role userRole = userRepo.findById(userId)
                    .orElseThrow(() -> new ThedalException(ThedalError.USER_NOT_FOUND, HttpStatus.NOT_FOUND))
                    .getRole();
            List<Integer> effectiveBoothNumbers = getEffectiveBoothNumbersFast(boothNumberList, userRole, userId);

            // Build dynamic query criteria
            Document matchCriteria = buildMatchCriteria(accountId, electionId, voterId, epicNumber, 
                    effectiveBoothNumbers, familyId, voterFnameEnList, voterLnameEn, voterFnameL1, 
                    voterFnameL2, relationFirstNameEn, relationLastNameEn, voterHistoryNameList, 
                    partyNameList, religionName, age, minAge, maxAge, includeUnknownAge, 
                    genderList, dobFilter, starNumber, description, casteCategoryName, serialNo);
            
            log.debug("effectiveBoothNumbers = {}", effectiveBoothNumbers);
            log.debug("Final matchCriteria = {}", matchCriteria.toJson()); 
            
         // Add duplicate criteria if requested
            matchCriteria = addDuplicateCriteria(matchCriteria, findDuplicates);
            
            // Execute aggregation with count and data
            List<VoterMongo> voters = executeVoterQuery(matchCriteria, sortDocument, pageable);
            long totalCount = getTotalCount(matchCriteria);

            // Map VoterMongo to VoterResponseDTO
            List<VoterResponseDTO1> voterDTOs = new ArrayList<>();
            if (!voters.isEmpty()) {
                voterDTOs = mapToVoterDTOs(voters, accountId, electionId);
            }

            // Calculate all stats using the same match criteria for accuracy
            GenderStatsDTO genderStats = calculateGenderStats(matchCriteria);
            
            // Calculate verification stats using the same filtered criteria
            VerificationStatsDTO aadhaarStats = calculateAadhaarStats(matchCriteria);
            VerificationStatsDTO membershipStats = calculateMembershipStats(matchCriteria);
            AddressedVoterStatsDTO addressedVoterStats = calculateAddressedStats(matchCriteria);
            
            // Only calculate booth stats when specific booths are requested
            List<Integer> statsBoothNumbers = effectiveBoothNumbers;
            // Don't get all booth numbers - only use specific ones requested
            
            // Calculate booth-wise stats using filtered criteria + booth restrictions
            List<BoothGenderStatsDTO> boothGenderStats = calculateBoothGenderStats(matchCriteria, statsBoothNumbers);
            List<BoothVerificationStatsDTO> boothAadhaarStats = calculateBoothAadhaarStats(matchCriteria, statsBoothNumbers);
            List<BoothVerificationStatsDTO> boothMembershipStats = calculateBoothMembershipStats(matchCriteria, statsBoothNumbers);

            // Create response with proper pagination info
            VoterResponseMongoDTO response = new VoterResponseMongoDTO(
                    voterDTOs, genderStats, boothGenderStats, aadhaarStats, membershipStats,
                    addressedVoterStats, boothAadhaarStats, boothMembershipStats, totalCount, 
                    pageable.getPageSize(), pageable.getPageNumber());

            long totalTime = System.currentTimeMillis() - startTime;
            log.info("MongoDB getVoters completed: {} ms, found {} voters out of {} total", 
                    totalTime, voters.size(), totalCount);

            return response;

        } catch (Exception e) {
            log.error("MongoDB getVoters failed: {}", e.getMessage(), e);
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    private Document addDuplicateCriteria(Document matchCriteria, Boolean findDuplicates) {
        if (findDuplicates != null && findDuplicates) {
            // Group by the duplicate fields and find groups with count > 1
            Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(convertDocumentToCriteria(matchCriteria)),
                Aggregation.group("voterFnameEn", "voterLnameEn", "rlnFnameEn", "rlnLnameEn", "age")
                    .count().as("count"),
                Aggregation.match(Criteria.where("count").gt(1)),
                Aggregation.project().andExclude("_id")
                    .and("$_id.voterFnameEn").as("voterFnameEn")
                    .and("$_id.voterLnameEn").as("voterLnameEn")
                    .and("$_id.rlnFnameEn").as("rlnFnameEn")
                    .and("$_id.rlnLnameEn").as("rlnLnameEn")
                    .and("$_id.age").as("age")
            );
            
            List<Document> duplicateGroups = mongoTemplate.aggregate(aggregation, "_voters", Document.class)
                .getMappedResults();
                
            if (!duplicateGroups.isEmpty()) {
                // Build $or query for all duplicate groups
                List<Document> orConditions = new ArrayList<>();
                for (Document group : duplicateGroups) {
                    Document condition = new Document();
                    condition.put("voterFnameEn", group.getString("voterFnameEn"));
                    condition.put("voterLnameEn", group.getString("voterLnameEn"));
                    condition.put("rlnFnameEn", group.getString("rlnFnameEn"));
                    condition.put("rlnLnameEn", group.getString("rlnLnameEn"));
                    condition.put("age", group.getInteger("age"));
                    orConditions.add(condition);
                }
                matchCriteria.put("$or", orConditions);
            }
        }
        return matchCriteria;
    }
    
    public VoterResponseMongoDTO searchVotersByName(Long accountId, Long electionId, String searchQuery, Pageable pageable, Document sortDocument) {
        long startTime = System.currentTimeMillis();
        
        log.info("Searching voters by name: query={}, electionId={}, accountId={}", searchQuery, electionId, accountId);
        
        // Validate election ownership
        validateElectionOwnershipFast(electionId, accountId);
        
        // For a comprehensive search, try EPIC number first, then names
        // If search query looks like an EPIC number (alphanumeric), prioritize EPIC number search
        String trimmedQuery = searchQuery.trim();
        
        // First try searching by EPIC number (exact match with case-insensitive)
        VoterResponseMongoDTO response = getVoters(
                accountId, null, trimmedQuery, electionId,       // 1-4: Search by EPIC number
                null, null, null, null,                         // 5-8: No name filters for EPIC search
                null, null, null, null,                         // 9-12: No relation filters
                null, null, null,                               // 13-15: No party/religion filters
                null, null, null, false,                        // 16-19: No age filters
                null, null, null, null,                         // 20-23: No other filters
                null,null,null,                                           // 24: No caste category filter
                pageable, sortDocument);                        // 25-26: Pagination/sorting
        
        // If no results found with EPIC number search and query looks like it could be a name,
        // fallback to name search
        if ((response.getVoters() == null || response.getVoters().isEmpty()) && 
            trimmedQuery.matches(".*[a-zA-Z].*")) { // Contains letters, could be a name
            
            List<String> searchTerms = Arrays.asList(trimmedQuery.toLowerCase().split("\\s+"));
            List<String> voterFnameEnList = searchTerms;
            List<String> voterLnameEnList = searchTerms;
            
            response = getVoters(
                    accountId, null, null, electionId,               // 1-4: No EPIC search in fallback
                    null, null, voterFnameEnList, voterLnameEnList,  // 5-8: Name search
                    searchTerms, null, searchTerms, searchTerms,     // 9-12: Relation name search
                    null, null, null,                                // 13-15: No party/religion filters
                    null, null, null, false,                        // 16-19: No age filters
                    null, null, null, null,                         // 20-23: No other filters
                    null,null,null,                                           // 24: No caste category filter
                    pageable, sortDocument);                         // 25-26: Pagination/sorting
        }
        
        long totalTime = System.currentTimeMillis() - startTime;
        log.info("searchVotersByName completed in {} ms", totalTime);
        
        return response;
    }
    
    // Add this method to your VoterMongoService class
private List<BoothVerificationStatsDTO> calculateBoothMembershipStats(Document matchCriteria, List<Integer> boothNumbers) {
    try {
        // Only calculate booth stats if specific booths are requested
        if (boothNumbers == null || boothNumbers.isEmpty()) {
            log.debug("No specific booth numbers requested, skipping booth membership stats");
            return new ArrayList<>();
        }
        
        Criteria mongoCriteria = convertDocumentToCriteria(matchCriteria);
        // DON'T ADD ANOTHER BOOTH FILTER - it's already in matchCriteria
        // mongoCriteria.and("boothNumber").in(boothNumbers); // REMOVE THIS LINE
        
        Aggregation aggregation = Aggregation.newAggregation(
            Aggregation.match(mongoCriteria),
            Aggregation.group("boothNumber")
                .sum(ConditionalOperators.when(Criteria.where("memberVerified").is(true)).then(1).otherwise(0)).as("verifiedCount")
                .sum(ConditionalOperators.when(Criteria.where("memberVerified").is(false)).then(1).otherwise(0)).as("unverifiedCount")
                .count().as("totalCount"),
            Aggregation.sort(Sort.by(Sort.Direction.ASC, "_id"))
        );
        
        List<Document> results = mongoTemplate.aggregate(aggregation, "_voters", Document.class).getMappedResults();
        
        List<BoothVerificationStatsDTO> stats = results.stream()
            .map(doc -> new BoothVerificationStatsDTO(
                doc.getInteger("_id"),
                getLongValue(doc, "verifiedCount"),
                getLongValue(doc, "unverifiedCount"),
                getLongValue(doc, "totalCount")
            ))
            .collect(Collectors.toList());
            
        log.debug("Booth membership stats calculated for {} booths with filters applied", stats.size());
        return stats;
    } catch (Exception e) {
        log.warn("Booth membership stats calculation failed: {}", e.getMessage());
        return new ArrayList<>();
    }
}



    // EFFICIENT DTO mapping that loads relationships separately
    private List<VoterResponseDTO1> mapToVoterDTOs(List<VoterMongo> voters, Long accountId, Long electionId) {
        try {
            log.info("mapToVoterDTOs received {} voters to map", voters.size());
            log.debug("Starting DTO mapping for {} voters", voters.size());
            
            ObjectMapper objectMapper = new ObjectMapper(); 
            // Load relationships efficiently for all voters at once
            List<String> voterIds = voters.stream().map(VoterMongo::getId).collect(Collectors.toList());
            
            Map<String, Set<LanguageMongo>> languagesMap = loadLanguagesSafe(voterIds);
            //Map<String, List<BenefitSchemesMongo>> benefitSchemesMap = loadBenefitSchemesSafe(voterIds, accountId, electionId);
            Map<String, Set<FeedbackIssueMongo>> feedbackIssuesMap = loadFeedbackIssuesSafe(voterIds, accountId, electionId);
            Map<String, Set<VoterHistoryMongo>> voterHistoriesMap = loadVoterHistoriesSafe(voterIds, accountId, electionId);
            Map<Long, PartyMongo> partyMap = loadPartiesSafe(voters);
            Map<Long, ReligionMongo> religionMap = loadReligionsSafe(voters);
            Map<Long, CasteMongo> casteMap = loadCastesSafe(voters);
            Map<Long, SubCasteMongo> subCasteMap = loadSubCastesSafe(voters);
            Map<Long, CasteCategoryMongo> casteCategoryMap = loadCasteCategoriesSafe(voters);
            Map<Long, AvailabilityMongo> availabilityMap = loadAvailabilitiesSafe(voters);
            Map<Long, PartManagerMongo> partManagerMap = loadPartManagersSafe(voters);
            
            return voters.stream().map(voter -> {
                try {
                    VoterResponseDTO1 dto = new VoterResponseDTO1();
                    
                    // Copy all scalar fields
                    dto.setId(voter.getId());
                    dto.setVoterId(voter.getVoterId());
                    dto.setEpicNumber(voter.getEpicNumber());
                    dto.setElectionId(voter.getElectionId());
                    dto.setAccountId(voter.getAccountId());
                    dto.setVoterFnameEn(voter.getVoterFnameEn());
                    dto.setVoterLnameEn(voter.getVoterLnameEn());
                    dto.setVoterFnameL1(voter.getVoterFnameL1());
                    dto.setVoterFnameL2(voter.getVoterFnameL2());
                    dto.setVoterLnameL1(voter.getVoterLnameL1());
                    dto.setVoterLnameL2(voter.getVoterLnameL2());
                    dto.setRlnType(voter.getRlnType());
                    dto.setRlnFnameEn(voter.getRlnFnameEn());
                    dto.setRlnLnameEn(voter.getRlnLnameEn());
                    dto.setRlnFnameL1(voter.getRlnFnameL1());
                    dto.setRlnFnameL2(voter.getRlnFnameL2());
                    dto.setRlnLnameL1(voter.getRlnLnameL1());
                    dto.setRlnLnameL2(voter.getRlnLnameL2());
                    dto.setGender(voter.getGender());
                    dto.setAge(voter.getAge());
                    dto.setDob(voter.getDob());
                    dto.setMobileNo(voter.getMobileNo());
                    dto.setWhatsappNo(voter.getWhatsappNo());
                    dto.setEMail(voter.getEMail());
                    dto.setPhotoUrl(voter.getPhotoUrl());
                    dto.setBoothNumber(voter.getBoothNumber());
                    dto.setPartNo(voter.getPartNo());
                    dto.setSectionNo(voter.getSectionNo());
                    dto.setSerialNo(voter.getSerialNo());
                    dto.setHouseNoEn(voter.getHouseNoEn());
                    dto.setHouseNoL1(voter.getHouseNoL1());
                    dto.setHouseNoL2(voter.getHouseNoL2());
                    dto.setSectionNameEn(voter.getSectionNameEn());
                    dto.setSectionNameL1(voter.getSectionNameL1());
                    dto.setSectionNameL2(voter.getSectionNameL2());
                    dto.setFullAddress(voter.getFullAddress());
                    dto.setPartNameEn(voter.getPartNameEn());
                    dto.setPartNameL1(voter.getPartNameL1());
                    dto.setPartNameL2(voter.getPartNameL2());
                    dto.setPincode(voter.getPincode());
                    dto.setPartLati(voter.getPartLati());
                    dto.setPartLong(voter.getPartLong());
                    dto.setVoterLati(voter.getVoterLati());
                    dto.setVoterLongi(voter.getVoterLongi());
                    
                    // Set all location fields
                    dto.setStateCode(voter.getStateCode());
                    dto.setStateNameEn(voter.getStateNameEn());
                    dto.setStateNameL1(voter.getStateNameL1());
                    dto.setStateNameL2(voter.getStateNameL2());
                    dto.setDistrictCode(voter.getDistrictCode());
                    dto.setDistrictNameEn(voter.getDistrictNameEn());
                    dto.setDistrictNameL1(voter.getDistrictNameL1());
                    dto.setDistrictNameL2(voter.getDistrictNameL2());
                    dto.setPcNo(voter.getPcNo());
                    dto.setPcNameEn(voter.getPcNameEn());
                    dto.setPcNameL1(voter.getPcNameL1());
                    dto.setPcNameL2(voter.getPcNameL2());
                    dto.setAcNo(voter.getAcNo());
                    dto.setAcNameEn(voter.getAcNameEn());
                    dto.setAcNameL1(voter.getAcNameL1());
                    dto.setAcNameL2(voter.getAcNameL2());
                    dto.setUrbanNo(voter.getUrbanNo());
                    dto.setUrbanNameEn(voter.getUrbanNameEn());
                    dto.setUrbanNameL1(voter.getUrbanNameL1());
                    dto.setUrbanWardNo(voter.getUrbanWardNo());
                    dto.setRurDistrictUnionNo(voter.getRurDistrictUnionNo());
                    dto.setRurDistrictUnionNameEn(voter.getRurDistrictUnionNameEn());
                    dto.setRurDistrictUnionNameL1(voter.getRurDistrictUnionNameL1());
                    dto.setRurDistrictUnionNameL2(voter.getRurDistrictUnionNameL2());
                    dto.setRurDistrictUnionWardNo(voter.getRurDistrictUnionWardNo());
                    dto.setPanUnionNo(voter.getPanUnionNo());
                    dto.setPanUnionNameEn(voter.getPanUnionNameEn());
                    dto.setPanUnionNameL1(voter.getPanUnionNameL1());
                    dto.setPanUnionNameL2(voter.getPanUnionNameL2());
                    dto.setPanUnionWardNo(voter.getPanUnionWardNo());
                    dto.setVillPanNo(voter.getVillPanNo());
                    dto.setVillPanNameEn(voter.getVillPanNameEn());
                    dto.setVillPanNameL1(voter.getVillPanNameL1());
                    dto.setVillPanWardNo(voter.getVillPanWardNo());
                    
                    // Set other fields
                    dto.setAvailability(voter.getAvailability());
                    dto.setScheme(voter.getScheme());
                    dto.setPartyAffiliation(voter.getPartyAffiliation());
                    dto.setStarNumber(voter.getStarNumber());
                    dto.setAadhaarNumber(voter.getAadhaarNumber());
                    dto.setPanNumber(voter.getPanNumber());
                    dto.setPartyRegistrationNumber(voter.getPartyRegistrationNumber());
                    dto.setFamilyId(voter.getFamilyId());
                    dto.setFamilyCount(voter.getFamilyCount());
                    dto.setFriendId(voter.getFriendId());
                    dto.setFriendCount(voter.getFriendCount());
                    // CRITICAL FIX: Include friendsDetails in response DTO
//                    try {
//                        dto.setFriendsDetails(voter.getFriendsDetails() != null ? 
//                            objectMapper.writeValueAsString(voter.getFriendsDetails()) : "[]");
//                    } catch (Exception e) {
//                        log.error("Failed to serialize friendsDetails for DTO mapping, voterId={}: {}", 
//                            voter.getVoterId(), e.getMessage());
//                        dto.setFriendsDetails("[]"); // Fallback to empty array
//                    }
//                    log.debug("Mapping voterId={}, friendId={}, friendCount={}, friendsDetails={}", 
//                        voter.getVoterId(), voter.getFriendId(), voter.getFriendCount(), dto.getFriendsDetails());
//                    try {
//                        String friendsJson = voter.getFriendsDetails() != null ? 
//                            objectMapper.writeValueAsString(voter.getFriendsDetails()) : "[]";
//                        dto.setFriendsDetails(friendsJson);
//                        log.debug("Mapped friendsDetails for voter {}: {}", voter.getId(), friendsJson);
//                    } catch (Exception e) {
//                        log.error("Failed to serialize friendsDetails for voter {}: {}", voter.getId(), e.getMessage());
//                        dto.setFriendsDetails("[]");
//                    }   
                    try {
                        List<FriendDetail> friends = voter.getFriendsDetails();
                        dto.setFriendsDetails(friends != null ? objectMapper.writeValueAsString(friends) : "[]");
                        log.debug("Serialized friendsDetails for voter {}: {}", voter.getId(), dto.getFriendsDetails());
                    } catch (Exception e) {
                        log.error("Failed to serialize friendsDetails for voter {}: {}", voter.getId(), e.getMessage());
                        dto.setFriendsDetails("[]");
                    }
                    dto.setPageNumber(voter.getPageNumber());
                    dto.setRemarks(voter.getRemarks());
                    dto.setVideoUrl(voter.getVideoUrl());
                    dto.setOtp(voter.getOtp());
                    dto.setOtpCreatedAt(voter.getOtpCreatedAt());
                    dto.setMobileVerified(voter.getMobileVerified());
                    dto.setAadhaarVerified(voter.getAadhaarVerified());
                    dto.setMemberVerified(voter.getMemberVerified());
                    dto.setCreatedTime(voter.getCreatedTime());
                    dto.setModifiedTime(voter.getModifiedTime());
                    dto.setHasVoted(voter.getHasVoted());
                    dto.setVotedTimestamp(voter.getVotedTimestamp());

                    // Set relationship IDs
                    dto.setReligionId(voter.getReligionId());
                    dto.setCasteId(voter.getCasteId());
                    dto.setSubCasteId(voter.getSubCasteId());
                    dto.setCasteCategoryId(voter.getCasteCategoryId());
                    dto.setPartyId(voter.getPartyId());
                    dto.setAvailabilityId(voter.getAvailabilityId());
                    dto.setPartManagerId(voter.getPartManagerId());

                    // Set relationships from the loaded maps
                    dto.setLanguages(languagesMap.getOrDefault(voter.getId(), new HashSet<>()));
                    //dto.setBenefitSchemes(benefitSchemesMap.getOrDefault(voter.getId(), new ArrayList<>()));
                    dto.setVoterBenefitSchemes(voter.getVoterBenefitSchemes() != null ? 
                            voter.getVoterBenefitSchemes() : Collections.emptyList());
                    dto.setFeedbackIssues(feedbackIssuesMap.getOrDefault(voter.getId(), new HashSet<>()));
                    dto.setVoterHistories(voterHistoriesMap.getOrDefault(voter.getId(), new HashSet<>()));
                    dto.setParty(partyMap.get(voter.getPartyId()));
                    dto.setReligion(religionMap.get(voter.getReligionId()));
                    
                    // Add caste and subcaste with null safety
                    if (voter.getCasteId() != null) {
                        dto.setCaste(casteMap.get(voter.getCasteId()));
                    }
                    if (voter.getSubCasteId() != null) {
                        dto.setSubCaste(subCasteMap.get(voter.getSubCasteId()));
                    }
                    if (voter.getCasteCategoryId() != null) {
                        dto.setCasteCategory(casteCategoryMap.get(voter.getCasteCategoryId()));
                    }
                    
                    dto.setAvailability1(availabilityMap.get(voter.getAvailabilityId()));
                    dto.setPartManager(partManagerMap.get(voter.getPartManagerId()));

                    return dto;
                } catch (Exception e) {
                    log.error("Error mapping voter {}: {}", voter.getId(), e.getMessage(), e);
                    throw new RuntimeException("Failed to map voter " + voter.getId(), e);
                }
            }).collect(Collectors.toList());
            
        } catch (Exception e) {
            log.error("Error in DTO mapping: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to map voters to DTOs", e);
        }
    }
    
    // Helper methods for loading relationships efficiently
    private Map<String, Set<LanguageMongo>> loadLanguagesSafe(List<String> voterIds) {
        try {
            List<VoterMongo> voters = voterMongoRepository.findVotersWithLanguages(voterIds);
            Map<String, Set<LanguageMongo>> voterLanguageMap = new HashMap<>();
            for (VoterMongo voter : voters) {
                List<Long> languageIds = voter.getLanguageIds() != null ? new ArrayList<>(voter.getLanguageIds()) : Collections.emptyList();
                if (!languageIds.isEmpty()) {
                    List<LanguageMongo> languages = languageMongoRepository.findAllById(languageIds);
                    voterLanguageMap.put(voter.getId(), new HashSet<>(languages));
                }
            }
            return voterLanguageMap;
        } catch (Exception e) {
            log.error("Failed to load languages: {}", e.getMessage(), e);
            return new HashMap<>();
        }
    }

//    private Map<String, List<BenefitSchemesMongo>> loadBenefitSchemesSafe(List<String> voterIds, Long accountId, Long electionId) {
//        try {
//            List<VoterMongo> voters = voterMongoRepository.findVotersWithBenefitSchemes(voterIds, accountId, electionId);
//            Map<String, List<BenefitSchemesMongo>> voterSchemeMap = new HashMap<>();
//            for (VoterMongo voter : voters) {
//                List<Long> schemeIds = voter.getBenefitSchemeIds() != null ? voter.getBenefitSchemeIds() : Collections.emptyList();
//                if (!schemeIds.isEmpty()) {
//                    List<BenefitSchemesMongo> schemes = benefitSchemesMongoRepository.findAllById(schemeIds);
//                    voterSchemeMap.put(voter.getId(), schemes);
//                }
//            }
//            return voterSchemeMap;
//        } catch (Exception e) {
//            log.error("Failed to load benefit schemes: {}", e.getMessage(), e);
//            return new HashMap<>();
//        }
//    }

    private Map<String, Set<FeedbackIssueMongo>> loadFeedbackIssuesSafe(List<String> voterIds, Long accountId, Long electionId) {
        try {
            List<VoterMongo> voters = voterMongoRepository.findVotersWithFeedbackIssues(voterIds, accountId, electionId);
            Map<String, Set<FeedbackIssueMongo>> voterIssueMap = new HashMap<>();
            for (VoterMongo voter : voters) {
                List<Long> issueIds = voter.getFeedbackIssueIds() != null ? new ArrayList<>(voter.getFeedbackIssueIds()) : Collections.emptyList();
                if (!issueIds.isEmpty()) {
                    List<FeedbackIssueMongo> issues = feedbackIssueMongoRepository.findAllById(issueIds);
                    voterIssueMap.put(voter.getId(), new HashSet<>(issues));
                }
            }
            return voterIssueMap;
        } catch (Exception e) {
            log.error("Failed to load feedback issues: {}", e.getMessage(), e);
            return new HashMap<>();
        }
    }

    private Map<String, Set<VoterHistoryMongo>> loadVoterHistoriesSafe(List<String> voterIds, Long accountId, Long electionId) {
        try {
            List<VoterMongo> voters = voterMongoRepository.findVotersWithVoterHistories(voterIds, accountId, electionId);
            Map<String, Set<VoterHistoryMongo>> voterHistoryMap = new HashMap<>();
            for (VoterMongo voter : voters) {
                List<Long> historyIds = voter.getVoterHistoryIds() != null ? new ArrayList<>(voter.getVoterHistoryIds()) : Collections.emptyList();
                if (!historyIds.isEmpty()) {
                    List<VoterHistoryMongo> histories = voterHistoryMongoRepository.findAllById(historyIds);
                    voterHistoryMap.put(voter.getId(), new HashSet<>(histories));
                }
            }
            return voterHistoryMap;
        } catch (Exception e) {
            log.error("Failed to load voter histories: {}", e.getMessage(), e);
            return new HashMap<>();
        }
    }

    private Map<Long, PartyMongo> loadPartiesSafe(List<VoterMongo> voters) {
        try {
            Set<Long> partyIds = voters.stream()
                    .filter(v -> v.getPartyId() != null)
                    .map(VoterMongo::getPartyId)
                    .collect(Collectors.toSet());
            List<PartyMongo> parties = partyMongoRepository.findAllById(partyIds);
            return parties.stream().collect(Collectors.toMap(PartyMongo::getId, p -> p));
        } catch (Exception e) {
            log.error("Failed to load parties: {}", e.getMessage(), e);
            return new HashMap<>();
        }
    }

    private Map<Long, ReligionMongo> loadReligionsSafe(List<VoterMongo> voters) {
        try {
            Set<Long> religionIds = voters.stream()
                    .filter(v -> v.getReligionId() != null)
                    .map(VoterMongo::getReligionId)
                    .collect(Collectors.toSet());
            List<ReligionMongo> religions = religionMongoRepository.findAllById(religionIds);
            return religions.stream().collect(Collectors.toMap(ReligionMongo::getId, r -> r));
        } catch (Exception e) {
            log.error("Failed to load religions: {}", e.getMessage(), e);
            return new HashMap<>();
        }
    }

    private Map<Long, CasteMongo> loadCastesSafe(List<VoterMongo> voters) {
        try {
            Set<Long> casteIds = voters.stream()
                    .filter(v -> v.getCasteId() != null)
                    .map(VoterMongo::getCasteId)
                    .collect(Collectors.toSet());
                    
            if (casteIds.isEmpty()) {
                return new HashMap<>();
            }
            
            List<CasteMongo> castes = casteMongoRepository.findAllById(casteIds);
            return castes.stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toMap(CasteMongo::getId, c -> c));
        } catch (Exception e) {
            log.error("Failed to load castes: {}", e.getMessage(), e);
            return new HashMap<>();
        }
    }

    private Map<Long, SubCasteMongo> loadSubCastesSafe(List<VoterMongo> voters) {
        try {
            Set<Long> subCasteIds = voters.stream()
                    .filter(v -> v.getSubCasteId() != null)
                    .map(VoterMongo::getSubCasteId)
                    .collect(Collectors.toSet());
                    
            if (subCasteIds.isEmpty()) {
                return new HashMap<>();
            }
            
            List<SubCasteMongo> subCastes = subCasteMongoRepository.findAllById(subCasteIds);
            return subCastes.stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toMap(SubCasteMongo::getId, sc -> sc));
        } catch (Exception e) {
            log.error("Failed to load sub-castes: {}", e.getMessage(), e);
            return new HashMap<>();
        }
    }

    private Map<Long, CasteCategoryMongo> loadCasteCategoriesSafe(List<VoterMongo> voters) {
        try {
            Set<Long> casteCategoryIds = voters.stream()
                    .filter(v -> v.getCasteCategoryId() != null)
                    .map(VoterMongo::getCasteCategoryId)
                    .collect(Collectors.toSet());
                    
            if (casteCategoryIds.isEmpty()) {
                return new HashMap<>();
            }
            
            List<CasteCategoryMongo> casteCategories = casteCategoryMongoRepository.findAllById(casteCategoryIds);
            return casteCategories.stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toMap(CasteCategoryMongo::getId, cc -> cc));
        } catch (Exception e) {
            log.error("Failed to load caste categories: {}", e.getMessage(), e);
            return new HashMap<>();
        }
    }

    private Map<Long, AvailabilityMongo> loadAvailabilitiesSafe(List<VoterMongo> voters) {
        try {
            Set<Long> availabilityIds = voters.stream()
                    .filter(v -> v.getAvailabilityId() != null)
                    .map(VoterMongo::getAvailabilityId)
                    .collect(Collectors.toSet());
            List<AvailabilityMongo> availabilities = availabilityMongoRepository.findAllById(availabilityIds);
            return availabilities.stream().collect(Collectors.toMap(AvailabilityMongo::getId, a -> a));
        } catch (Exception e) {
            log.error("Failed to load availabilities: {}", e.getMessage(), e);
            return new HashMap<>();
        }
    }

    private Map<Long, PartManagerMongo> loadPartManagersSafe(List<VoterMongo> voters) {
        try {
            Set<Long> partManagerIds = voters.stream()
                    .filter(v -> v.getPartManagerId() != null)
                    .map(VoterMongo::getPartManagerId)
                    .collect(Collectors.toSet());
            List<PartManagerMongo> partManagers = partManagerMongoRepository.findAllById(partManagerIds);
            return partManagers.stream().collect(Collectors.toMap(PartManagerMongo::getId, pm -> pm));
        } catch (Exception e) {
            log.error("Failed to load part managers: {}", e.getMessage(), e);
            return new HashMap<>();
        }
    }

    // ID resolution methods
    private List<Long> resolvePartyIds(List<String> partyNames) {
        try {
            if (partyNames == null || partyNames.isEmpty()) {
                return Collections.emptyList();
            }
            Collection<PartyMongo> partiesCollection = partyMongoRepository.findByPartyNameIn(
                partyNames.stream().map(String::toLowerCase).collect(Collectors.toList())
            );
            return partiesCollection.stream()
                    .filter(Objects::nonNull)
                    .map(PartyMongo::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to resolve party IDs: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private List<Long> resolveReligionIds(String religionName) {
        try {
            if (religionName == null || religionName.isEmpty()) {
                return Collections.emptyList();
            }
            return religionMongoRepository.findByReligionNameIgnoreCase(religionName)
                    .map(r -> Collections.singletonList(r.getId()))
                    .orElse(Collections.emptyList());
        } catch (Exception e) {
            log.error("Failed to resolve religion IDs: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private List<Long> resolveVoterHistoryIds(List<String> voterHistoryNames) {
        try {
            if (voterHistoryNames == null || voterHistoryNames.isEmpty()) {
                return Collections.emptyList();
            }
            Collection<VoterHistoryMongo> historiesCollection = voterHistoryMongoRepository.findByVoterHistoryNameIn(
                voterHistoryNames.stream().map(String::toLowerCase).collect(Collectors.toList())
            );
            return historiesCollection.stream()
                    .filter(Objects::nonNull)
                    .map(VoterHistoryMongo::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to resolve voter history IDs: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private List<Long> resolveAvailabilityIds(String description) {
        try {
            if (description == null || description.isEmpty()) {
                return Collections.emptyList();
            }
            return availabilityMongoRepository.findByDescription(description)
                    .map(a -> Collections.singletonList(a.getId()))
                    .orElse(Collections.emptyList());
        } catch (Exception e) {
            log.error("Failed to resolve availability IDs: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private List<Long> resolveCasteCategoryIds(String casteCategoryName, Long accountId, Long electionId) {
        try {
            if (casteCategoryName == null || casteCategoryName.isEmpty()) {
                return Collections.emptyList();
            }
            return casteCategoryMongoRepository.findByCasteCategoryNameAndAccountIdAndElectionId(casteCategoryName, accountId, electionId)
                    .map(cc -> Collections.singletonList(cc.getId()))
                    .orElse(Collections.emptyList());
        } catch (Exception e) {
            log.error("Failed to resolve caste category IDs: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    // Helper validation methods (from PostgreSQL implementation)
    private void validateElectionOwnershipFast(Long electionId, Long accountId) {
        String cacheKey = "election_" + electionId + "_account_" + accountId;
        
        Long cacheTime = validationCacheTimes.get(cacheKey);
        if (cacheTime != null && (System.currentTimeMillis() - cacheTime) < VALIDATION_CACHE_TTL) {
            return; // Cache hit - skip DB query
        }
        
        // Only hit DB if not in cache
        if (!electionRepository.existsByIdAndAccountId(electionId, accountId)) {
            throw new ThedalException(ThedalError.INVALID_ELECTION, HttpStatus.FORBIDDEN);
        }
        
        // Cache the result
        validationCache.put(cacheKey, true);
        validationCacheTimes.put(cacheKey, System.currentTimeMillis());
    }

    private List<Integer> getEffectiveBoothNumbersFast(List<Integer> boothNumbers, Role userRole, Long userId) {
        // For READ operations: Allow all roles to see all booths (no restrictions)
        // Volunteers can now view all voters regardless of their assigned booths
        return boothNumbers;
    }

    private void validateResultNotEmpty(Slice<?> result, ThedalError error) {
        if (result.isEmpty()) {
            throw new ThedalException(error, HttpStatus.NOT_FOUND);
        }
    }
    
    // Helper method to build dynamic match criteria
    private Document buildMatchCriteria(Long accountId, Long electionId, String voterId, String epicNumber,
            List<Integer> boothNumbers, UUID familyId, List<String> voterFnameEn, List<String> voterLnameEn,
            List<String> voterFnameL1, List<String> voterFnameL2, List<String> relationFirstNameEn,
            List<String> relationLastNameEn, List<String> voterHistoryNameList, List<String> partyNameList,
            String religionName, Integer age, Integer minAge, Integer maxAge, Boolean includeUnknownAge,
            List<String> genderList, Boolean dobFilter, Boolean starNumber, String description, String casteCategoryName,  Long serialNo) {
        
        Document criteria = new Document();
        criteria.put("accountId", accountId);
        criteria.put("electionId", electionId);
        
        log.debug("Building match criteria with voterId={}, boothNumbers={}", voterId, boothNumbers);
        
        // Add filters only if they have meaningful values
        if (voterId != null && !voterId.trim().isEmpty()) {
            criteria.put("voterId", voterId.trim());
            log.debug("Added voterId filter: {}", voterId.trim());
        }
        
        if (epicNumber != null && !epicNumber.trim().isEmpty()) {
            // Use case-insensitive regex matching for EPIC number search
            criteria.put("epicNumber", new Document("$regex", epicNumber.trim()).append("$options", "i"));
            log.debug("Added epicNumber regex filter: {}", epicNumber.trim());
        }
        
        if (boothNumbers != null && !boothNumbers.isEmpty()) {
            criteria.put("boothNumber", new Document("$in", boothNumbers));
            log.info("DEBUG: Added boothNumber filter: {}", boothNumbers);
            log.info("DEBUG: Criteria now: {}", criteria.toJson());
            log.debug("Added boothNumber filter: {}", boothNumbers);
        }
        
        if (familyId != null) {
            criteria.put("familyId", familyId);
            log.debug("Added familyId filter: {}", familyId);
        }
        
        // Name filters with regex for partial matching - use $or for better matching
        List<Document> nameOrConditions = new ArrayList<>();
        
        // Voter first name in English
        if (voterFnameEn != null && !voterFnameEn.isEmpty()) {
            for (String name : voterFnameEn) {
                if (name != null && !name.trim().isEmpty()) {
                    nameOrConditions.add(new Document("voterFnameEn", 
                        new Document("$regex", name.trim()).append("$options", "i")));
                }
            }
        }
        
        // Voter last name in English
        if (voterLnameEn != null && !voterLnameEn.isEmpty()) {
            for (String name : voterLnameEn) {
                if (name != null && !name.trim().isEmpty()) {
                    nameOrConditions.add(new Document("voterLnameEn", 
                        new Document("$regex", name.trim()).append("$options", "i")));
                }
            }
        }
        
        // Voter first name in Local Language 1
        if (voterFnameL1 != null && !voterFnameL1.isEmpty()) {
            for (String name : voterFnameL1) {
                if (name != null && !name.trim().isEmpty()) {
                    nameOrConditions.add(new Document("voterFnameL1", 
                        new Document("$regex", name.trim()).append("$options", "i")));
                    log.debug("Added voter first name L1 filter: {}", name.trim());
                }
            }
        }
        
        // Voter first name in Local Language 2
        if (voterFnameL2 != null && !voterFnameL2.isEmpty()) {
            for (String name : voterFnameL2) {
                if (name != null && !name.trim().isEmpty()) {
                    nameOrConditions.add(new Document("voterFnameL2", 
                        new Document("$regex", name.trim()).append("$options", "i")));
                    log.debug("Added voter first name L2 filter: {}", name.trim());
                }
            }
        }
        
        // Relation first name in English (previously missing)
        if (relationFirstNameEn != null && !relationFirstNameEn.isEmpty()) {
            for (String name : relationFirstNameEn) {
                if (name != null && !name.trim().isEmpty()) {
                    nameOrConditions.add(new Document("rlnFnameEn", 
                        new Document("$regex", name.trim()).append("$options", "i")));
                    log.debug("Added relation first name filter: {}", name.trim());
                }
            }
        }
        
        // Relation last name in English (previously missing)
        if (relationLastNameEn != null && !relationLastNameEn.isEmpty()) {
            for (String name : relationLastNameEn) {
                if (name != null && !name.trim().isEmpty()) {
                    nameOrConditions.add(new Document("rlnLnameEn", 
                        new Document("$regex", name.trim()).append("$options", "i")));
                    log.debug("Added relation last name filter: {}", name.trim());
                }
            }
        }
        
        if (!nameOrConditions.isEmpty()) {
            criteria.put("$or", nameOrConditions);
            log.debug("Added name filters with {} conditions", nameOrConditions.size());
        }
        
        // Voter History filter (previously missing)
        if (voterHistoryNameList != null && !voterHistoryNameList.isEmpty()) {
            List<Long> voterHistoryIds = resolveVoterHistoryIds(voterHistoryNameList);
            if (!voterHistoryIds.isEmpty()) {
                criteria.put("voterHistoryIds", new Document("$in", voterHistoryIds));
                log.debug("Added voter history filter: {}", voterHistoryIds);
            }
        }
        
        // Gender filter - handle multiple variations (male, m, Male, female, f, Female, etc.)
        if (genderList != null && !genderList.isEmpty()) {
            List<String> normalizedGenders = genderList.stream()
                .filter(g -> g != null && !g.trim().isEmpty())
                .map(g -> {
                    String lower = g.toLowerCase().trim();
                    if (lower.startsWith("m")) return "male";
                    if (lower.startsWith("f")) return "female";
                    if (lower.startsWith("o")) return "other";
                    return lower;
                })
                .distinct()
                .collect(Collectors.toList());
            
            if (!normalizedGenders.isEmpty()) {
                // Create regex pattern for case-insensitive matching
                String regexPattern = normalizedGenders.stream()
                    .map(gender -> {
                        if ("male".equals(gender)) return "(male|m)";
                        if ("female".equals(gender)) return "(female|f)";
                        if ("other".equals(gender)) return "(other|o)";
                        return gender;
                    })
                    .collect(Collectors.joining("|", "^(", ")$"));
                
                criteria.put("gender", new Document("$regex", regexPattern).append("$options", "i"));
                log.debug("Added gender filter: {}", regexPattern);
            }
        }
        
        // Age filters with proper includeUnknownAge logic
        if (age != null && age > 0) {
            if (includeUnknownAge != null && includeUnknownAge) {
                // Include exact age OR null age - use $and with nested $or to avoid conflicts
                List<Document> ageOrConditions = Arrays.asList(
                    new Document("age", age),
                    new Document("age", new Document("$eq", null))
                );
                
                // If there's already an $or for names, we need to wrap both in $and
                if (criteria.containsKey("$or")) {
                    Document existingOr = (Document) criteria.remove("$or");
                    criteria.put("$and", Arrays.asList(
                        new Document("$or", existingOr),
                        new Document("$or", ageOrConditions)
                    ));
                } else {
                    criteria.put("$or", ageOrConditions);
                }
                log.debug("Added exact age filter with unknown age: {}", age);
            } else {
                criteria.put("age", age);
                log.debug("Added exact age filter: {}", age);
            }
        } else if (minAge != null || maxAge != null) {
            Document ageCondition = new Document();
            if (minAge != null && minAge > 0) {
                ageCondition.put("$gte", minAge);
            }
            if (maxAge != null && maxAge < 150) {
                ageCondition.put("$lte", maxAge);
            }
            
            if (!ageCondition.isEmpty()) {
                if (includeUnknownAge != null && includeUnknownAge) {
                    // Include age range OR null age - use $and with nested $or to avoid conflicts
                    List<Document> ageOrConditions = Arrays.asList(
                        new Document("age", ageCondition),
                        new Document("age", new Document("$eq", null))
                    );
                    
                    // If there's already an $or for names, we need to wrap both in $and
                    if (criteria.containsKey("$or")) {
                        Document existingOr = (Document) criteria.remove("$or");
                        criteria.put("$and", Arrays.asList(
                            new Document("$or", existingOr),
                            new Document("$or", ageOrConditions)
                        ));
                    } else {
                        criteria.put("$or", ageOrConditions);
                    }
                    log.debug("Added age range filter with unknown age: {}", ageCondition);
                } else {
                    // Only include non-null ages within range
                    ageCondition.put("$ne", null);
                    criteria.put("age", ageCondition);
                    log.debug("Added age range filter excluding unknown age: {}", ageCondition);
                }
            }
        } else if (includeUnknownAge != null && includeUnknownAge) {
            // Only include unknown/null ages when no other age criteria
            criteria.put("age", new Document("$eq", null));
            log.debug("Added filter to include only unknown age");
        }
        
        // DOB filter
        if (dobFilter != null && dobFilter) {
            criteria.put("dob", new Document("$ne", null));
            log.debug("Added DOB filter");
        }
        
        // Star number filter
        if (starNumber != null && starNumber) {
            criteria.put("starNumber", true);
            log.debug("Added star number filter");
        }
        
        // Resolve and add party/religion filters
        if (partyNameList != null && !partyNameList.isEmpty()) {
            List<Long> partyIds = resolvePartyIds(partyNameList);
            if (!partyIds.isEmpty()) {
                criteria.put("partyId", new Document("$in", partyIds));
                log.debug("Added party filter: {}", partyIds);
            }
        }
        
        if (religionName != null && !religionName.trim().isEmpty()) {
            List<Long> religionIds = resolveReligionIds(religionName);
            if (!religionIds.isEmpty()) {
                criteria.put("religionId", new Document("$in", religionIds));
                log.debug("Added religion filter: {}", religionIds);
            }
        }
        
        // Description filter (availability/category filter)
        if (description != null && !description.trim().isEmpty()) {
            List<Long> availabilityIds = resolveAvailabilityIds(description.trim());
            if (!availabilityIds.isEmpty()) {
                criteria.put("availabilityId", new Document("$in", availabilityIds));
                log.debug("Added description/availability filter: {}", availabilityIds);
            } else {
                log.debug("Description filter requested but no availability IDs resolved: {}", description);
            }
        }
        
        // Caste Category filter
        if (casteCategoryName != null && !casteCategoryName.trim().isEmpty()) {
            List<Long> casteCategoryIds = resolveCasteCategoryIds(casteCategoryName.trim(), accountId, electionId);
            if (!casteCategoryIds.isEmpty()) {
                criteria.put("casteCategoryId", new Document("$in", casteCategoryIds));
                log.debug("Added caste category filter: {}", casteCategoryIds);
            } else {
                log.debug("Caste category filter requested but no caste category IDs resolved: {}", casteCategoryName);
            }
        }
        
        if (serialNo != null) {
            criteria.put("serialNo", serialNo);
            log.debug("Added serialNo filter: {}", serialNo);
        }     
        
        log.debug("Final match criteria: {}", criteria.toJson());
        return criteria;
    }
    
    
    // Fixed getTotalCount method that properly handles all criteria including $or conditions
    private long getTotalCount(Document matchCriteria) {
        try {
            log.info("Getting total count with criteria: {}", matchCriteria.toJson());
            
            // Use the same convertDocumentToCriteria method that stats calculations use
            // This properly handles $or conditions (including fname filters)
            Criteria mongoCriteria = convertDocumentToCriteria(matchCriteria);
            Query query = new Query(mongoCriteria);
            
            log.info("Count query object: {}", query.getQueryObject().toJson());
            
            long count = mongoTemplate.count(query, VoterMongo.class);
            log.info("Total count result: {}", count);
            
            return count;
        } catch (Exception e) {
            log.error("Error getting total count: {}", e.getMessage(), e);
            return 0L;
        }
    }
    
    // Get all booth numbers for an account/election
    private List<Integer> getAllBoothNumbers(Long accountId, Long electionId) {
        try {
            Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("accountId").is(accountId).and("electionId").is(electionId)),
                Aggregation.group("boothNumber"),
                Aggregation.sort(Sort.by(Sort.Direction.ASC, "_id"))
            );
            
            List<Document> results = mongoTemplate.aggregate(aggregation, "_voters", Document.class).getMappedResults();
            return results.stream()
                .map(doc -> doc.getInteger("_id"))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Failed to get booth numbers: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
    
    private GenderStatsDTO calculateGenderStats(Document matchCriteria) {
    try {
        Criteria mongoCriteria = convertDocumentToCriteria(matchCriteria);
        
        Aggregation aggregation = Aggregation.newAggregation(
            Aggregation.match(mongoCriteria),
            Aggregation.group()
                .sum(ConditionalOperators.when(
                    new Criteria().orOperator(
                        Criteria.where("gender").regex("^(?i)male$"),
                        Criteria.where("gender").regex("^(?i)m$")
                    )).then(1).otherwise(0)).as("maleCount")
                .sum(ConditionalOperators.when(
                    new Criteria().orOperator(
                        Criteria.where("gender").regex("^(?i)female$"),
                        Criteria.where("gender").regex("^(?i)f$")
                    )).then(1).otherwise(0)).as("femaleCount")
                .sum(ConditionalOperators.when(
                    new Criteria().andOperator(
                        Criteria.where("gender").exists(true),
                        Criteria.where("gender").ne(null),
                        Criteria.where("gender").not().regex("^(?i)(male|m|female|f)$")
                    )).then(1).otherwise(0)).as("otherCount")
                .count().as("totalCount")
        );
        
        List<Document> results = mongoTemplate.aggregate(aggregation, "_voters", Document.class).getMappedResults();
        
        if (!results.isEmpty()) {
            Document result = results.get(0);
            Long maleCount = getLongValue(result, "maleCount");
            Long femaleCount = getLongValue(result, "femaleCount");
            Long otherCount = getLongValue(result, "otherCount");
            Long totalCount = getLongValue(result, "totalCount");
            
            log.info("Gender stats - Male: {}, Female: {}, Other: {}, Total: {}", 
                maleCount, femaleCount, otherCount, totalCount);
            
            return new GenderStatsDTO(maleCount, femaleCount, otherCount, totalCount);
        }
        
        return new GenderStatsDTO(0L, 0L, 0L, 0L);
    } catch (Exception e) {
        log.warn("Gender stats calculation failed: {}", e.getMessage(), e);
        return new GenderStatsDTO(0L, 0L, 0L, 0L);
    }
}


   private List<BoothGenderStatsDTO> calculateBoothGenderStats(Document matchCriteria, List<Integer> boothNumbers) {
    try {
        // Only calculate booth stats if specific booths are requested
        if (boothNumbers == null || boothNumbers.isEmpty()) {
            log.debug("No specific booth numbers requested, skipping booth gender stats");
            return new ArrayList<>();
        }
        
        Criteria mongoCriteria = convertDocumentToCriteria(matchCriteria);
        
        Aggregation aggregation = Aggregation.newAggregation(
            Aggregation.match(mongoCriteria),
            Aggregation.group("boothNumber")
                .sum(ConditionalOperators.when(
                    new Criteria().orOperator(
                        Criteria.where("gender").regex("^(?i)male$"),
                        Criteria.where("gender").regex("^(?i)m$")
                    )).then(1).otherwise(0)).as("maleCount")
                .sum(ConditionalOperators.when(
                    new Criteria().orOperator(
                        Criteria.where("gender").regex("^(?i)female$"),
                        Criteria.where("gender").regex("^(?i)f$")
                    )).then(1).otherwise(0)).as("femaleCount")
                .sum(ConditionalOperators.when(
                    new Criteria().andOperator(
                        Criteria.where("gender").exists(true),
                        Criteria.where("gender").ne(null),
                        Criteria.where("gender").not().regex("^(?i)(male|m|female|f)$")
                    )).then(1).otherwise(0)).as("otherCount")
                .count().as("totalCount"),
            Aggregation.sort(Sort.by(Sort.Direction.ASC, "_id"))
        );
        
        List<Document> results = mongoTemplate.aggregate(aggregation, "_voters", Document.class).getMappedResults();
        
        List<BoothGenderStatsDTO> stats = results.stream()
            .map(doc -> {
                Integer boothNumber = doc.getInteger("_id");
                Long maleCount = getLongValue(doc, "maleCount");
                Long femaleCount = getLongValue(doc, "femaleCount");
                Long otherCount = getLongValue(doc, "otherCount");
                Long totalCount = getLongValue(doc, "totalCount");
                
                log.debug("Booth {} gender stats - Male: {}, Female: {}, Other: {}, Total: {}", 
                          boothNumber, maleCount, femaleCount, otherCount, totalCount);
                
                return new BoothGenderStatsDTO(boothNumber, maleCount, femaleCount, otherCount, totalCount);
            })
            .collect(Collectors.toList());
            
        log.debug("Booth gender stats calculated for {} booths with filters applied", stats.size());
        return stats;
    } catch (Exception e) {
        log.warn("Booth gender stats calculation failed: {}", e.getMessage());
        return new ArrayList<>();
    }
}

    
    // Calculate Aadhaar verification stats using filtered criteria
    private VerificationStatsDTO calculateAadhaarStats(Document matchCriteria) {
        try {
            Criteria mongoCriteria = convertDocumentToCriteria(matchCriteria);
            
            Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(mongoCriteria),
                Aggregation.group()
                    .sum(ConditionalOperators.when(Criteria.where("aadhaarVerified").is(true)).then(1).otherwise(0)).as("verifiedCount")
                    .sum(ConditionalOperators.when(Criteria.where("aadhaarVerified").is(false)).then(1).otherwise(0)).as("unverifiedCount")
                    .count().as("totalCount")
            );
            
            List<Document> results = mongoTemplate.aggregate(aggregation, "_voters", Document.class).getMappedResults();
            
            if (!results.isEmpty()) {
                Document result = results.get(0);
                Long verifiedCount = getLongValue(result, "verifiedCount");
                Long unverifiedCount = getLongValue(result, "unverifiedCount");
                Long totalCount = getLongValue(result, "totalCount");
                
                log.debug("Aadhaar stats (filtered) - Verified: {}, Unverified: {}, Total: {}", 
                    verifiedCount, unverifiedCount, totalCount);
                
                return new VerificationStatsDTO(verifiedCount, unverifiedCount, totalCount);
            }
            
            return new VerificationStatsDTO(0L, 0L, 0L);
        } catch (Exception e) {
            log.warn("Aadhaar stats calculation failed: {}", e.getMessage());
            return new VerificationStatsDTO(0L, 0L, 0L);
        }
    }
    
    // Calculate membership verification stats using filtered criteria
    private VerificationStatsDTO calculateMembershipStats(Document matchCriteria) {
        try {
            Criteria mongoCriteria = convertDocumentToCriteria(matchCriteria);
            
            Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(mongoCriteria),
                Aggregation.group()
                    .sum(ConditionalOperators.when(Criteria.where("memberVerified").is(true)).then(1).otherwise(0)).as("verifiedCount")
                    .sum(ConditionalOperators.when(Criteria.where("memberVerified").is(false)).then(1).otherwise(0)).as("unverifiedCount")
                    .count().as("totalCount")
            );
            
            List<Document> results = mongoTemplate.aggregate(aggregation, "_voters", Document.class).getMappedResults();
            
            if (!results.isEmpty()) {
                Document result = results.get(0);
                Long verifiedCount = getLongValue(result, "verifiedCount");
                Long unverifiedCount = getLongValue(result, "unverifiedCount");
                Long totalCount = getLongValue(result, "totalCount");
                
                log.debug("Membership stats (filtered) - Verified: {}, Unverified: {}, Total: {}", 
                    verifiedCount, unverifiedCount, totalCount);
                
                return new VerificationStatsDTO(verifiedCount, unverifiedCount, totalCount);
            }
            
            return new VerificationStatsDTO(0L, 0L, 0L);
        } catch (Exception e) {
            log.warn("Membership stats calculation failed: {}", e.getMessage());
            return new VerificationStatsDTO(0L, 0L, 0L);
        }
    }
    
    private AddressedVoterStatsDTO calculateAddressedStats(Document matchCriteria) {
        try {
            Criteria mongoCriteria = convertDocumentToCriteria(matchCriteria);
            
            Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(mongoCriteria),
                Aggregation.group()
                    .sum(ConditionalOperators.when(Criteria.where("availability").ne(null)).then(1).otherwise(0)).as("addressedCount")
                    .sum(ConditionalOperators.when(Criteria.where("availability").is(null)).then(1).otherwise(0)).as("notAddressedCount")
                    .count().as("totalCount")
            );
            
            List<Document> results = mongoTemplate.aggregate(aggregation, "_voters", Document.class).getMappedResults();
            
            if (!results.isEmpty()) {
                Document result = results.get(0);
                Long addressedCount = getLongValue(result, "addressedCount");
                Long notAddressedCount = getLongValue(result, "notAddressedCount");
                Long totalCount = getLongValue(result, "totalCount");
                
                log.debug("Addressed voter stats (filtered) - Addressed: {}, Not Addressed: {}, Total: {}", 
                    addressedCount, notAddressedCount, totalCount);
                
                return new AddressedVoterStatsDTO(addressedCount, notAddressedCount, totalCount);
            }
            
            return new AddressedVoterStatsDTO(0L, 0L, 0L);
        } catch (Exception e) {
            log.warn("Addressed voter stats calculation failed: {}", e.getMessage());
            return new AddressedVoterStatsDTO(0L, 0L, 0L);
        }
    }
    
   private List<BoothVerificationStatsDTO> calculateBoothAadhaarStats(Document matchCriteria, List<Integer> boothNumbers) {
    try {
        // Only calculate booth stats if specific booths are requested
        if (boothNumbers == null || boothNumbers.isEmpty()) {
            log.debug("No specific booth numbers requested, skipping booth Aadhaar stats");
            return new ArrayList<>();
        }
        
        Criteria mongoCriteria = convertDocumentToCriteria(matchCriteria);
        // DON'T ADD ANOTHER BOOTH FILTER - it's already in matchCriteria
        // mongoCriteria.and("boothNumber").in(boothNumbers); // REMOVE THIS LINE
        
        Aggregation aggregation = Aggregation.newAggregation(
            Aggregation.match(mongoCriteria),
            Aggregation.group("boothNumber")
                .sum(ConditionalOperators.when(Criteria.where("aadhaarVerified").is(true)).then(1).otherwise(0)).as("verifiedCount")
                .sum(ConditionalOperators.when(Criteria.where("aadhaarVerified").is(false)).then(1).otherwise(0)).as("unverifiedCount")
                .count().as("totalCount"),
            Aggregation.sort(Sort.by(Sort.Direction.ASC, "_id"))
        );
        
        List<Document> results = mongoTemplate.aggregate(aggregation, "_voters", Document.class).getMappedResults();
        
        List<BoothVerificationStatsDTO> stats = results.stream()
            .map(doc -> new BoothVerificationStatsDTO(
                doc.getInteger("_id"),
                getLongValue(doc, "verifiedCount"),
                getLongValue(doc, "unverifiedCount"),
                getLongValue(doc, "totalCount")
            ))
            .collect(Collectors.toList());
            
        log.debug("Booth Aadhaar stats calculated for {} booths with filters applied", stats.size());
        return stats;
    } catch (Exception e) {
        log.warn("Booth Aadhaar stats calculation failed: {}", e.getMessage());
        return new ArrayList<>();
    }
}

    // Simplified executeVoterQuery using the same convertDocumentToCriteria method for consistency
    private List<VoterMongo> executeVoterQuery(Document matchCriteria, Document sortDocument, Pageable pageable) {
        try {
            log.info("Executing direct MongoDB query with criteria: {}", matchCriteria.toJson());
            
            // Use the same convertDocumentToCriteria method for consistency
            Criteria mongoCriteria = convertDocumentToCriteria(matchCriteria);
            Query query = new Query(mongoCriteria);
            
            // Build sort
            Sort sort = Sort.by(Sort.Direction.ASC, "partNo", "serialNo"); // default
            if (sortDocument != null && !sortDocument.isEmpty()) {
                List<Sort.Order> orders = new ArrayList<>();
                for (String key : sortDocument.keySet()) {
                    Object value = sortDocument.get(key);
                    Sort.Direction direction = (value instanceof Number && ((Number) value).intValue() == -1) 
                        ? Sort.Direction.DESC : Sort.Direction.ASC;
                    orders.add(new Sort.Order(direction, key));
                }
                if (!orders.isEmpty()) {
                    sort = Sort.by(orders);
                }
            }
            
            query.with(sort);
            query.skip(pageable.getOffset());
            query.limit(pageable.getPageSize());
            
            log.info("Final query object: {}", query.getQueryObject().toJson());
            log.info("Final sort object: {}", query.getSortObject().toJson());
            
            List<VoterMongo> voters = mongoTemplate.find(query, VoterMongo.class);
            log.info("executeVoterQuery returned {} voters", voters.size());
            
            // Debug: Print some voter details
            voters.forEach(voter -> 
                log.debug("Found voter: id={}, boothNumber={}, name={}", 
                    voter.getId(), voter.getBoothNumber(), voter.getVoterFnameEn())
            );
            
            return voters;
        
    } catch (Exception e) {
        log.error("Error executing voter query: {}", e.getMessage(), e);
        throw new RuntimeException("Failed to execute voter query", e);
    }
}

    // Helper method to convert Document criteria to MongoDB Criteria
    private Criteria convertDocumentToCriteria(Document matchCriteria) {
        Criteria mongoCriteria = new Criteria();
        for (String key : matchCriteria.keySet()) {
            Object value = matchCriteria.get(key);
            if ("$or".equals(key)) {
                // Handle $or conditions
                @SuppressWarnings("unchecked")
                List<Document> orConditions = (List<Document>) value;
                List<Criteria> orCriteriaList = orConditions.stream()
                    .map(doc -> {
                        Criteria orCrit = new Criteria();
                        for (String orKey : doc.keySet()) {
                            Object orValue = doc.get(orKey);
                            if (orValue instanceof Document) {
                                Document orDocValue = (Document) orValue;
                                if (orDocValue.containsKey("$regex")) {
                                    orCrit.and(orKey).regex(orDocValue.getString("$regex"), 
                                        orDocValue.getString("$options"));
                                }
                            } else {
                                orCrit.and(orKey).is(orValue);
                            }
                        }
                        return orCrit;
                    })
                    .collect(Collectors.toList());
                mongoCriteria.orOperator(orCriteriaList.toArray(new Criteria[0]));
            } else if (value instanceof Document) {
                Document docValue = (Document) value;
                if (docValue.containsKey("$in")) {
                    mongoCriteria.and(key).in((List<?>) docValue.get("$in"));
                } else if (docValue.containsKey("$regex")) {
                    mongoCriteria.and(key).regex(docValue.getString("$regex"), 
                        docValue.getString("$options"));
                } else if (docValue.containsKey("$gte") || docValue.containsKey("$lte")) {
                    if (docValue.containsKey("$gte")) {
                        mongoCriteria.and(key).gte(docValue.get("$gte"));
                    }
                    if (docValue.containsKey("$lte")) {
                        mongoCriteria.and(key).lte(docValue.get("$lte"));
                    }
                } else if (docValue.containsKey("$ne")) {
                    mongoCriteria.and(key).ne(docValue.get("$ne"));
                }
            } else {
                mongoCriteria.and(key).is(value);
            }
        }
        return mongoCriteria;
    }
    
    // Helper method to safely extract Long values from Document
    private Long getLongValue(Document doc, String key) {
        Object value = doc.get(key);
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
    

    
}