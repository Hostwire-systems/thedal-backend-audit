package com.thedal.thedal_app.voter.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.thedal.thedal_app.election.PartManagerMongo;
import com.thedal.thedal_app.settings.electionsettings.AvailabilityMongo;
import com.thedal.thedal_app.settings.electionsettings.CasteCategoryMongo;
import com.thedal.thedal_app.settings.electionsettings.CasteMongo;
import com.thedal.thedal_app.settings.electionsettings.FeedbackIssueMongo;
import com.thedal.thedal_app.settings.electionsettings.LanguageMongo;
import com.thedal.thedal_app.settings.electionsettings.PartyMongo;
import com.thedal.thedal_app.settings.electionsettings.ReligionMongo;
import com.thedal.thedal_app.settings.electionsettings.SubCasteMongo;
import com.thedal.thedal_app.settings.electionsettings.VoterHistoryMongo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VoterResponseDTO1 {
    
    private String id;
    private String voterId;
    private String epicNumber;
    private Long electionId;
    private Long accountId;
    
    // Voter Names
    private String voterFnameEn;
    private String voterLnameEn;
    private String voterFnameL1;
    private String voterFnameL2;
    private String voterLnameL1;
    private String voterLnameL2;
    
    // Relation Details
    private String rlnType;
    private String rlnFnameEn;
    private String rlnLnameEn;
    private String rlnFnameL1;
    private String rlnFnameL2;
    private String rlnLnameL1;
    private String rlnLnameL2;
    
    // Basic Info
    private String gender;
    private Integer age;
    private LocalDate dob;
    private String mobileNo;
    private String whatsappNo;
    private String eMail;
    private String photoUrl;
    
    // Booth and Part Info
    private Integer boothNumber;
    private Integer partNo;
    private Integer sectionNo;
    private Long serialNo;
    
    // Address Details
    private String houseNoEn;
    private String houseNoL1;
    private String houseNoL2;
    private String sectionNameEn;
    private String sectionNameL1;
    private String sectionNameL2;
    private String fullAddress;
    private String partNameEn;
    private String partNameL1;
    private String partNameL2;
    private String pincode;
    
    // Location Coordinates
    private Double partLati;
    private Double partLong;
    private Double voterLati;
    private Double voterLongi;
    
    // State Information
    private String stateCode;
    private String stateNameEn;
    private String stateNameL1;
    private String stateNameL2;
    
    // District Information
    private String districtCode;
    private String districtNameEn;
    private String districtNameL1;
    private String districtNameL2;
    
    // Parliamentary Constituency
    private String pcNo;
    private String pcNameEn;
    private String pcNameL1;
    private String pcNameL2;
    
    // Assembly Constituency
    private String acNo;
    private String acNameEn;
    private String acNameL1;
    private String acNameL2;
    
    // Urban Details
    private String urbanNo;
    private String urbanNameEn;
    private String urbanNameL1;
    private Integer urbanWardNo;
    
    // Rural District Union Details
    private String rurDistrictUnionNo;
    private String rurDistrictUnionNameEn;
    private String rurDistrictUnionNameL1;
    private String rurDistrictUnionNameL2;
    private String rurDistrictUnionWardNo;
    
    // Panchayat Union Details
    private String panUnionNo;
    private String panUnionNameEn;
    private String panUnionNameL1;
    private String panUnionNameL2;
    private String panUnionWardNo;
    
    // Village Panchayat Details
    private String villPanNo;
    private String villPanNameEn;
    private String villPanNameL1;
    private String villPanWardNo;
    
    // Other Fields
    private String availability;
    private String scheme;
    private String partyAffiliation;
    private Boolean starNumber;
    private String aadhaarNumber;
    private String panNumber;
    private String partyRegistrationNumber;
    private UUID familyId;
    private Integer familyCount;
    private UUID friendId;
    private Integer friendCount;
    private String friendsDetails;
    private Integer pageNumber;
    private String remarks;
    private String videoUrl;
    private String otp;
    private LocalDateTime otpCreatedAt;
    
    // Verification Status
    private Boolean mobileVerified;
    private Boolean aadhaarVerified;
    private Boolean memberVerified;
    
    // Voting Status
    private Boolean hasVoted;
    private LocalDateTime votedTimestamp;
    
    // Timestamps
    private LocalDateTime createdTime;
    private LocalDateTime modifiedTime;
    
    // Relationship IDs
    private Long religionId;
    private Long casteId;
    private Long subCasteId;
    private Long casteCategoryId;
    private Long partyId;
    private Long availabilityId;
    private Long partManagerId;
    
    // Relationship Objects (for full entity mapping)
    private Set<LanguageMongo> languages;
    //private List<BenefitSchemesMongo> benefitSchemes;
    private List<VoterBenefitSchemeMongo> voterBenefitSchemes;
    private Set<FeedbackIssueMongo> feedbackIssues;
    private Set<VoterHistoryMongo> voterHistories;
    private PartyMongo party;
    private ReligionMongo religion;
    private CasteMongo caste;
    private SubCasteMongo subCaste;
    private CasteCategoryMongo casteCategory;
    private AvailabilityMongo availability1;
    private PartManagerMongo partManager;
}
