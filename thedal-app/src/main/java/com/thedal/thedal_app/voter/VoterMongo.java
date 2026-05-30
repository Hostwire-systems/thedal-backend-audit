package com.thedal.thedal_app.voter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thedal.thedal_app.cpanel.dtos.VoterHistoryEntity;
import com.thedal.thedal_app.settings.electionsettings.FeedbackIssue;
import com.thedal.thedal_app.settings.electionsettings.Language;
import com.thedal.thedal_app.voter.dto.VoterBenefitSchemeMongo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;


@Getter
@Setter
@NoArgsConstructor
@Document(collection = "_voters")
@Slf4j
@CompoundIndexes({
    @CompoundIndex(name = "idx_account_election_epic", def = "{'accountId': 1, 'electionId': 1, 'epicNumber': 1}"),
    @CompoundIndex(name = "idx_account_election_voter_id", def = "{'accountId': 1, 'electionId': 1, 'voterId': 1}", unique = true),
    @CompoundIndex(name = "idx_account_election_booth", def = "{'accountId': 1, 'electionId': 1, 'boothNumber': 1}"),
    @CompoundIndex(name = "idx_account_election_family", def = "{'accountId': 1, 'electionId': 1, 'familyId': 1}"),
    @CompoundIndex(name = "idx_account_election_fname_en", def = "{'accountId': 1, 'electionId': 1, 'voterFnameEn': 1}"),
    @CompoundIndex(name = "idx_account_election_lname_en", def = "{'accountId': 1, 'electionId': 1, 'voterLnameEn': 1}"),
    @CompoundIndex(name = "idx_account_election_fname_l1", def = "{'accountId': 1, 'electionId': 1, 'voterFnameL1': 1}"),
    @CompoundIndex(name = "idx_account_election_fname_l2", def = "{'accountId': 1, 'electionId': 1, 'voterFnameL2': 1}"),
    @CompoundIndex(name = "idx_account_election_rln_fname", def = "{'accountId': 1, 'electionId': 1, 'rlnFnameEn': 1}"),
    @CompoundIndex(name = "idx_account_election_rln_lname", def = "{'accountId': 1, 'electionId': 1, 'rlnLnameEn': 1}"),
    @CompoundIndex(name = "idx_account_election_gender", def = "{'accountId': 1, 'electionId': 1, 'gender': 1}"),
    @CompoundIndex(name = "idx_account_election_age", def = "{'accountId': 1, 'electionId': 1, 'age': 1}"),
    @CompoundIndex(name = "idx_account_election_dob", def = "{'accountId': 1, 'electionId': 1, 'dob': 1}"),
    @CompoundIndex(name = "idx_account_election_star", def = "{'accountId': 1, 'electionId': 1, 'starNumber': 1}"),
    @CompoundIndex(name = "idx_account_election_aadhaar_verified", def = "{'accountId': 1, 'electionId': 1, 'aadhaarVerified': 1}"),
    @CompoundIndex(name = "idx_account_election_member_verified", def = "{'accountId': 1, 'electionId': 1, 'memberVerified': 1}"),
    @CompoundIndex(name = "idx_aadhaar_election", def = "{'aadhaarNumber': 1, 'electionId': 1}"),
    @CompoundIndex(name = "idx_account_election_booth_voted", def = "{'accountId': 1, 'electionId': 1, 'boothNumber': 1, 'hasVoted': 1}"),
    @CompoundIndex(name = "idx_election_booth_serial", def = "{'electionId': 1, 'boothNumber': 1, 'serialNo': 1}"),
    @CompoundIndex(name = "idx_election_part_serial", def = "{'electionId': 1, 'partNo': 1, 'serialNo': 1}"),
    @CompoundIndex(name = "idx_optimized_filters", def = "{'accountId': 1, 'electionId': 1, 'boothNumber': 1, 'gender': 1, 'age': 1, 'starNumber': 1}")
})
public class VoterMongo {
	
	private static final ObjectMapper objectMapper = new ObjectMapper();
	static {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
    @Id
    private String id;

    @Indexed
    private String voterId;

    private Long religionId;
    private Long casteId;
    private Long subCasteId;
    private Long casteCategoryId;
    private String photoUrl;
    private Long accountId;
    private Long electionId;
    private Integer boothNumber;
    private Boolean hasVoted;
    private LocalDateTime votedTimestamp;
    private LocalDateTime createdTime;
    private LocalDateTime modifiedTime;

    // Part Information
    private Integer partNo;
    private Integer sectionNo;
    private Long serialNo;
    private String houseNoEn;
    private String houseNoL1;
    private String houseNoL2;
    private String voterFnameEn;
    private String voterLnameEn;
    private String voterFnameL1;
    private String voterFnameL2;
    private String voterLnameL1;
    private String voterLnameL2;
    private String rlnType;
    private String rlnFnameEn;
    private String rlnLnameEn;
    private String rlnFnameL1;
    private String rlnFnameL2;
    private String rlnLnameL1;
    private String rlnLnameL2;
    private String epicNumber;
    private String gender;
    private String sectionNameEn;
    private String sectionNameL1;
    private String sectionNameL2;
    private String fullAddress;
    private String partNameEn;
    private String partNameL1;
    private String partNameL2;
    private String pincode;
    private Double partLati;
    private Double partLong;
    private Integer age;
    private LocalDate dob;
    private String mobileNo;
    private String whatsappNo;
    private String eMail;
    private Double voterLati;
    private Double voterLongi;

    // State and Location Details
    private String stateCode;
    private String stateNameEn;
    private String stateNameL1;
    private String stateNameL2;
    private String districtCode;
    private String districtNameEn;
    private String districtNameL1;
    private String districtNameL2;
    private String pcNo;
    private String pcNameEn;
    private String pcNameL1;
    private String pcNameL2;
    private String acNo;
    private String acNameEn;
    private String acNameL1;
    private String acNameL2;
    private String urbanNo;
    private String urbanNameEn;
    private String urbanNameL1;
    private Integer urbanWardNo;
    private String rurDistrictUnionNo;
    private String rurDistrictUnionNameEn;
    private String rurDistrictUnionNameL1;
    private String rurDistrictUnionNameL2;
    private String rurDistrictUnionWardNo;
    private String panUnionNo;
    private String panUnionNameEn;
    private String panUnionNameL1;
    private String panUnionNameL2;
    private String panUnionWardNo;
    private String villPanNo;
    private String villPanNameEn;
    private String villPanNameL1;
    private String villPanWardNo;

    private String availability;
    private String partyAffiliation;
    private Boolean starNumber;
    private String aadhaarNumber;
    private String panNumber;
    private String partyRegistrationNumber;

    @JsonIgnore
    private Map<String, String> dynamicFields = new HashMap<>();
    //private UUID familyId;
    @Field("familyId")
    private UUID familyId;
    private Integer familyCount = 1;
    @Field("friendId")
    private UUID friendId;
    private Integer friendCount = 0;
//    @Field("friendsDetails")
//    private String friendsDetails;
    @Field("friendsDetails")
    @JsonProperty("friendsDetails")
    private String friendsDetails;

    private Set<Long> languageIds = new HashSet<>();
    //private List<Long> benefitSchemeIds = new ArrayList<>();
    @Field("voterBenefitSchemes")
    private List<VoterBenefitSchemeMongo> voterBenefitSchemes = new ArrayList<>();
    
    private String scheme;
    private Long availabilityId;
    private Long partyId;
    private Integer pageNumber;
    private String remarks;
    private String videoUrl;
    private String otp;
    private LocalDateTime otpCreatedAt;
    private Boolean mobileVerified = false;
    private Boolean aadhaarVerified = false;
    private Boolean memberVerified = false;
    private Set<Long> feedbackIssueIds = new HashSet<>();
    private Set<Long> voterHistoryIds = new HashSet<>();
    private Long partManagerId;
    private Long createdByUserId;

    // Constructor to map from VoterEntity
    public VoterMongo(VoterEntity voter) {
        this.id = voter.getId() != null ? voter.getId().toString() : null;
        this.voterId = voter.getVoterId();
        this.religionId = voter.getReligion() != null ? voter.getReligion().getId() : null;
        this.casteId = voter.getCaste() != null ? voter.getCaste().getId() : null;
        this.subCasteId = voter.getSubCaste() != null ? voter.getSubCaste().getId() : null;
        this.casteCategoryId = voter.getCasteCategory() != null ? voter.getCasteCategory().getId() : null;
        this.photoUrl = voter.getPhotoUrl();
        this.accountId = voter.getAccountId();
        this.electionId = voter.getElectionId();
        this.boothNumber = voter.getBoothNumber();
        this.hasVoted = voter.getHasVoted();
        this.votedTimestamp = voter.getVotedTimestamp();
        this.createdTime = voter.getCreatedTime();
        this.modifiedTime = voter.getModifiedTime();
        this.partNo = voter.getPartNo();
        this.sectionNo = voter.getSectionNo();
        this.serialNo = voter.getSerialNo();
        this.houseNoEn = voter.getHouseNoEn();
        this.houseNoL1 = voter.getHouseNoL1();
        this.houseNoL2 = voter.getHouseNoL2();
        this.voterFnameEn = voter.getVoterFnameEn();
        this.voterLnameEn = voter.getVoterLnameEn();
        this.voterFnameL1 = voter.getVoterFnameL1();
        this.voterFnameL2 = voter.getVoterFnameL2();
        this.voterLnameL1 = voter.getVoterLnameL1();
        this.voterLnameL2 = voter.getVoterLnameL2();
        this.rlnType = voter.getRlnType();
        this.rlnFnameEn = voter.getRlnFnameEn();
        this.rlnLnameEn = voter.getRlnLnameEn();
        this.rlnFnameL1 = voter.getRlnFnameL1();
        this.rlnFnameL2 = voter.getRlnFnameL2();
        this.rlnLnameL1 = voter.getRlnLnameL1();
        this.rlnLnameL2 = voter.getRlnLnameL2();
        this.epicNumber = voter.getEpicNumber();
        this.gender = voter.getGender();
        this.sectionNameEn = voter.getSectionNameEn();
        this.sectionNameL1 = voter.getSectionNameL1();
        this.sectionNameL2 = voter.getSectionNameL2();
        this.fullAddress = voter.getFullAddress();
        this.partNameEn = voter.getPartNameEn();
        this.partNameL1 = voter.getPartNameL1();
        this.partNameL2 = voter.getPartNameL2();
        this.pincode = voter.getPincode();
        this.partLati = voter.getPartLati();
        this.partLong = voter.getPartLong();
        this.age = voter.getAge();
        this.dob = voter.getDob();
        this.mobileNo = voter.getMobileNo();
        this.whatsappNo = voter.getWhatsappNo();
        this.eMail = voter.getEMail();
        this.voterLati = voter.getVoterLati();
        this.voterLongi = voter.getVoterLongi();
        this.stateCode = voter.getStateCode();
        this.stateNameEn = voter.getStateNameEn();
        this.stateNameL1 = voter.getStateNameL1();
        this.stateNameL2 = voter.getStateNameL2();
        this.districtCode = voter.getDistrictCode();
        this.districtNameEn = voter.getDistrictNameEn();
        this.districtNameL1 = voter.getDistrictNameL1();
        this.districtNameL2 = voter.getDistrictNameL2();
        this.pcNo = voter.getPcNo();
        this.pcNameEn = voter.getPcNameEn();
        this.pcNameL1 = voter.getPcNameL1();
        this.pcNameL2 = voter.getPcNameL2();
        this.acNo = voter.getAcNo();
        this.acNameEn = voter.getAcNameEn();
        this.acNameL1 = voter.getAcNameL1();
        this.acNameL2 = voter.getAcNameL2();
        this.urbanNo = voter.getUrbanNo();
        this.urbanNameEn = voter.getUrbanNameEn();
        this.urbanNameL1 = voter.getUrbanNameL1();
        this.urbanWardNo = voter.getUrbanWardNo();
        this.rurDistrictUnionNo = voter.getRurDistrictUnionNo();
        this.rurDistrictUnionNameEn = voter.getRurDistrictUnionNameEn();
        this.rurDistrictUnionNameL1 = voter.getRurDistrictUnionNameL1();
        this.rurDistrictUnionNameL2 = voter.getRurDistrictUnionNameL2();
        this.rurDistrictUnionWardNo = voter.getRurDistrictUnionWardNo();
        this.panUnionNo = voter.getPanUnionNo();
        this.panUnionNameEn = voter.getPanUnionNameEn();
        this.panUnionNameL1 = voter.getPanUnionNameL1();
        this.panUnionNameL2 = voter.getPanUnionNameL2();
        this.panUnionWardNo = voter.getPanUnionWardNo();
        this.villPanNo = voter.getVillPanNo();
        this.villPanNameEn = voter.getVillPanNameEn();
        this.villPanNameL1 = voter.getVillPanNameL1();
        this.villPanWardNo = voter.getVillPanWardNo();
        this.availability = voter.getAvailability();
        this.partyAffiliation = voter.getPartyAffiliation();
        this.starNumber = voter.getStarNumber();
        this.aadhaarNumber = voter.getAadhaarNumber();
        this.panNumber = voter.getPanNumber();
        this.partyRegistrationNumber = voter.getPartyRegistrationNumber();
        this.dynamicFields = voter.getDynamicFields() != null ? new HashMap<>(voter.getDynamicFields()) : new HashMap<>();
        this.familyId = voter.getFamilyId();
        this.familyCount = voter.getFamilyCount();
        this.friendId = voter.getFriendId();
        this.friendCount = voter.getFriendCount();
        try {
            this.friendsDetails = voter.getFriendsDetails() != null ? objectMapper.writeValueAsString(voter.getFriendsDetails()) : null;
        } catch (Exception e) {
            log.error("Failed to serialize friendsDetails for voterId: {}, error: {}", voter.getVoterId(), e.getMessage());
            throw new RuntimeException("Error serializing friendsDetails", e);
        }
        //this.setFriendsDetails(voter.getFriendsDetails());
        this.languageIds = voter.getLanguages() != null ? voter.getLanguages().stream().map(Language::getId).collect(Collectors.toSet()) : new HashSet<>();
       // this.benefitSchemeIds = voter.getBenefitSchemes() != null ? voter.getBenefitSchemes().stream().map(BenefitSchemes::getId).collect(Collectors.toList()) : new ArrayList<>();
        this.voterBenefitSchemes = voter.getVoterBenefitSchemes() != null ?
                voter.getVoterBenefitSchemes().stream()
                    .map(vbs -> new VoterBenefitSchemeMongo(vbs))
                    .collect(Collectors.toList()) : new ArrayList<>();       
        this.scheme = voter.getScheme();
        this.availabilityId = voter.getAvailability1() != null ? voter.getAvailability1().getId() : null;
        this.partyId = voter.getParty() != null ? voter.getParty().getId() : null;
        this.pageNumber = voter.getPageNumber();
        this.remarks = voter.getRemarks();
        this.videoUrl = voter.getVideoUrl();
        this.otp = voter.getOtp();
        this.otpCreatedAt = voter.getOtpCreatedAt();
        this.mobileVerified = voter.getMobileVerified();
        this.aadhaarVerified = voter.getAadhaarVerified();
        this.memberVerified = voter.getMemberVerified();
        this.feedbackIssueIds = voter.getFeedbackIssues() != null ? voter.getFeedbackIssues().stream().map(FeedbackIssue::getId).collect(Collectors.toSet()) : new HashSet<>();
        this.voterHistoryIds = voter.getVoterHistories() != null ? voter.getVoterHistories().stream().map(VoterHistoryEntity::getId).collect(Collectors.toSet()) : new HashSet<>();
        this.partManagerId = voter.getPartManager() != null ? voter.getPartManager().getId() : null;
        this.createdByUserId = voter.getCreatedByUserId();
    
    
    
    }

    public void setPartNoAndBoothNumber(Integer partNoValue) {
        this.partNo = partNoValue;
        this.boothNumber = partNoValue;
    }

    public Integer getPartNoAndBoothNumber() {
        return partNo;
    }
    
    public List<FriendDetail> getFriendsDetails() {
        try {
            if (friendsDetails == null || friendsDetails.trim().isEmpty() || 
                friendsDetails.equals("[]") || friendsDetails.equals("null")) {
                return new ArrayList<>();
            }
            
            // Try to parse as array first
            try {
                return objectMapper.readValue(friendsDetails, 
                    new TypeReference<List<FriendDetail>>(){});
            } catch (Exception e) {
                // If array parsing fails, try as single object
                FriendDetail singleFriend = objectMapper.readValue(friendsDetails, FriendDetail.class);
                return Collections.singletonList(singleFriend);
            }
        } catch (Exception e) {
            log.error("Error deserializing friendsDetails: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    public void setFriendsDetails(List<FriendDetail> friends) {
        try {
            this.friendsDetails = (friends == null || friends.isEmpty()) ? 
                null : objectMapper.writeValueAsString(friends);
        } catch (Exception e) {
            log.error("Error serializing friendsDetails: {}", e.getMessage());
            this.friendsDetails = null;
        }
    }
    
    /**
     * Get the raw friendsDetails string for MongoDB storage
     * @return Raw JSON string of friends details
     */
    public String getRawFriendsDetails() {
        return this.friendsDetails;
    }
    
   
    
}