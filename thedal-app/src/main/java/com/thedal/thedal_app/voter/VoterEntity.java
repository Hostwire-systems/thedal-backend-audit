package com.thedal.thedal_app.voter;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.BatchSize;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.thedal.thedal_app.cpanel.dtos.VoterHistoryEntity;
import com.thedal.thedal_app.election.DynamicFieldEntity;
import com.thedal.thedal_app.election.PartManager;
import com.thedal.thedal_app.settings.electionsettings.Availability;
import com.thedal.thedal_app.settings.electionsettings.CasteCategoryEntity;
import com.thedal.thedal_app.settings.electionsettings.CasteEntity;
import com.thedal.thedal_app.settings.electionsettings.FeedbackIssue;
import com.thedal.thedal_app.settings.electionsettings.Language;
import com.thedal.thedal_app.settings.electionsettings.Party;
import com.thedal.thedal_app.settings.electionsettings.ReligionEntity;
import com.thedal.thedal_app.settings.electionsettings.SubCasteEntity;
import com.thedal.thedal_app.voter.dto.AvailabilitySerializer;
import com.thedal.thedal_app.voter.dto.CasteCategorySerializer;
import com.thedal.thedal_app.voter.dto.CasteSerializer;
import com.thedal.thedal_app.voter.dto.LanguageSerializer;
import com.thedal.thedal_app.voter.dto.PartManagerPostgresSerializer;
import com.thedal.thedal_app.voter.dto.PartySerializer;
import com.thedal.thedal_app.voter.dto.ReligionSerializer;
import com.thedal.thedal_app.voter.dto.SubCasteSerializer;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
// Replace the @Table annotation in VoterEntity.java with this optimized version
@Table(
    name = "_voters",
    indexes = {
        // Core lookup indexes
        @Index(name = "idx_account_election_epic", columnList = "account_id, election_id, epic_number"),
        @Index(name = "idx_account_election_voter_id", columnList = "account_id, election_id, voter_id"),
        @Index(name = "idx_account_election_booth", columnList = "account_id, election_id, booth_number"),
        @Index(name = "idx_account_election_family", columnList = "account_id, election_id, family_id"),
        @Index(name = "idx_account_election_family_sequence", columnList = "account_id, election_id, family_sequence_number"),
        @Index(name = "idx_account_election_family_display_part", columnList = "account_id, election_id, family_display_part"),
        
        // Name search indexes with text_pattern_ops equivalent
        @Index(name = "idx_account_election_fname_en", columnList = "account_id, election_id, voter_fname_en"),
        @Index(name = "idx_account_election_lname_en", columnList = "account_id, election_id, voter_lname_en"),
        @Index(name = "idx_account_election_fname_l1", columnList = "account_id, election_id, voter_fname_l1"),
        @Index(name = "idx_account_election_fname_l2", columnList = "account_id, election_id, voter_fname_l2"),
        @Index(name = "idx_account_election_rln_fname", columnList = "account_id, election_id, rln_fname_en"),
        @Index(name = "idx_account_election_rln_lname", columnList = "account_id, election_id, rln_lname_en"),
        
        // Filter indexes
        @Index(name = "idx_account_election_gender", columnList = "account_id, election_id, gender"),
        @Index(name = "idx_account_election_age", columnList = "account_id, election_id, age"),
        @Index(name = "idx_account_election_dob", columnList = "account_id, election_id, dob"),
        @Index(name = "idx_account_election_star", columnList = "account_id, election_id, star_number"),
        
        // Foreign key indexes for JOINs
        @Index(name = "idx_availability_id", columnList = "availability_id"),
        @Index(name = "idx_party_id", columnList = "party_id"),
        @Index(name = "idx_religion_id", columnList = "religion_id"),
        @Index(name = "idx_caste_id", columnList = "caste_id"),
        @Index(name = "idx_sub_caste_id", columnList = "sub_caste_id"),
        @Index(name = "idx_part_manager_id", columnList = "part_manager_id"),
        
        // Verification indexes
        @Index(name = "idx_account_election_aadhaar_verified", columnList = "account_id, election_id, aadhaar_verified"),
        @Index(name = "idx_account_election_member_verified", columnList = "account_id, election_id, member_verified"),
        @Index(name = "idx_aadhaar_election", columnList = "aadhaar_number, election_id"),
        
        // Voting status indexes
        @Index(name = "idx_account_election_booth_voted", columnList = "account_id, election_id, booth_number, has_voted"),
        
        // Sorting indexes
        @Index(name = "idx_election_booth_serial", columnList = "election_id, booth_number, serial_no"),
        @Index(name = "idx_election_part_serial", columnList = "election_id, part_no, serial_no"),
        
        // Composite index for common filter combinations
        @Index(name = "idx_optimized_filters", columnList = "account_id, election_id, booth_number, gender, age, star_number")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VoterEntity {
	
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;
	@JsonIgnore

	@Column(name = "voter_id")
    @JsonProperty("voter_id")
    private String voterId;
    
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "religion_id", referencedColumnName = "id")
	@JsonProperty("religion")
    @JsonSerialize(using = ReligionSerializer.class)
	private ReligionEntity religion;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "caste_id", referencedColumnName = "id")
	@JsonProperty("caste")
    @JsonSerialize(using = CasteSerializer.class)
	private CasteEntity caste;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "sub_caste_id", referencedColumnName = "id")
	@JsonProperty("subCaste")
    @JsonSerialize(using = SubCasteSerializer.class)
	private SubCasteEntity subCaste;
      
    //@URL(message = "Invalid photo URL")
    @Column(name = "photo_url")
    @JsonProperty("photo_url")
    private String photoUrl;

    @JsonIgnore
    @Column(name = "account_id")
    private Long accountId;
    
    @Column(nullable = false)
    private Long electionId;
    @JsonIgnore
    @Column(name = "booth_number", unique = false)
    private Integer boothNumber;
    
//    @Column(name = "has_voted", nullable = false)
//    private Boolean hasVoted = false;   
    @Column
    private Boolean hasVoted;
    @Column(columnDefinition = "TIMESTAMP")
    private LocalDateTime votedTimestamp;            
        
    @Column(name = "created_time", updatable = false)
    private LocalDateTime createdTime;
    @Column(name = "modified_time")
    private LocalDateTime modifiedTime;
     

 // Part Information
//    @NotNull(message = "Part Number or Booth Number is mandatory")
//    @Column(name = "part_no", nullable = false)
    @Column(name = "part_no")
    private Integer partNo;
    @Column(name = "section_no")
    private Integer sectionNo;
//    @NotNull(message = "Serial Number of Voter is mandatory")
//    @Column(name = "serial_no", nullable = false)
    @Column(name = "serial_no")
    private Long serialNo;
//    @NotNull(message = "House Number of Voter in English is mandatory")
//    @Column(name = "house_no_en", nullable = false)
    @Column(name = "house_no_en")
    private String houseNoEn;   
    @Column(name = "house_no_l1")
    private String houseNoL1;
    @Column(name = "house_no_l2")
    private String houseNoL2;
//    @NotNull(message = "Voter First Name in English is mandatory")
//    @Column(name = "voter_fname_en", nullable = false)
    @Column(name = "voter_fname_en")
    private String voterFnameEn;
    @Column(name = "voter_lname_en")
    private String voterLnameEn;
    @Column(name = "voter_fname_l1")
    private String voterFnameL1;
    @Column(name = "voter_fname_l2")
    private String voterFnameL2;
    @Column(name = "voter_lname_l1")
    private String voterLnameL1;
    @Column(name = "voter_lname_l2")
    private String voterLnameL2;
//    @NotNull(message = "Relationship Type like Father,Mother, Husband is mandatory")
//    @Column(name = "rln_type", nullable = false)
    @Column(name = "rln_type")
    private String rlnType;
//    @NotNull(message = "Relation First Name in English is mandatory")
//    @Column(name = "rln_fname_en", nullable = false)
    @Column(name = "rln_fname_en")
    private String rlnFnameEn;
    @Column(name = "rln_lname_en")
    private String rlnLnameEn;
    @Column(name = "rln_fname_l1")
    private String rlnFnameL1;
    @Column(name = "rln_fname_l2")
    private String rlnFnameL2;
    @Column(name = "rln_lname_l1")
    private String rlnLnameL1;
    @Column(name = "rln_lname_l2")
    private String rlnLnameL2;
    //------------
    @NotNull(message = "EPIC Number or Voter Id Number is mandatory")
    @Column(name = "epic_number", nullable = false)
    @JsonProperty("epic_number")
    private String epicNumber;

//	@NotBlank(message = "Gender is mandatory")
//	@Pattern(regexp = "(?i)^(male|female|other)$", message = "Gender must be male, female, or other (case-insensitive)")
//	@Column(name = "gender", nullable = false)
    @Column(name = "gender", columnDefinition = "TEXT")
	private String gender;
    @Column(name = "section_name_en")
    private String sectionNameEn;
    @Column(name = "section_name_l1")
    private String sectionNameL1;
    @Column(name = "section_name_l2")
    private String sectionNameL2;
    @Column(name = "full_address")
    private String fullAddress;
    @Column(name = "part_name_en")
    private String partNameEn;
    @Column(name = "part_name_l1")
    private String partNameL1;
    @Column(name = "part_name_l2")
    private String partNameL2;
    @Column(name = "pincode")
    private String pincode;
    @Column(name = "part_lati")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Double partLati;
    @Column(name = "part_long")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Double partLong;
    @Column(name = "age")
    private Integer age;
    @Column(name = "dob")
    private LocalDate dob;
    @Column(name = "mobile_no")
    private String mobileNo; 
    @Column(name = "whatsapp_no")
    private String whatsappNo; 
    @Column(name = "e_mail")
    private String eMail; 
    @Column(name = "voter_lati", columnDefinition = "DOUBLE PRECISION")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Double voterLati;

    @Column(name = "voter_longi", columnDefinition = "DOUBLE PRECISION")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Double voterLongi;
    
  /// state
    @Column(name = "state_code")
    private String stateCode; 
    @Column(name = "state_name_en")
    private String stateNameEn; 
    @Column(name = "state_name_l1")
    private String stateNameL1; 
    @Column(name = "state_name_l2")
    private String stateNameL2; 
    @Column(name = "district_code")
    private String districtCode;
    @Column(name = "district_name_en")
    private String districtNameEn; 
    @Column(name = "district_name_l1")
    private String districtNameL1; 
    @Column(name = "district_name_l2")
    private String districtNameL2; 
//    @Column(name = "whats_app_no")
//    private String whatsAppNo;
    @Column(name = "pc_no")
    private String pcNo; 
    @Column(name = "pc_name_en")
    private String pcNameEn; 
    @Column(name = "pc_name_l1")
    private String pcNameL1;
    @Column(name = "pc_name_l2")
    private String pcNameL2;
    @Column(name = "ac_no")
    private String acNo; 
    @Column(name = "ac_name_en")
    private String acNameEn; 
    @Column(name = "ac_name_l1")
    private String acNameL1; 
    @Column(name = "ac_name_l2")
    private String acNameL2; 
 // Urban Details
    @Column(name = "urban_no")
    private String urbanNo;
    @Column(name = "urban_name_en")
    private String urbanNameEn;
    @Column(name = "urban_name_l1")
    private String urbanNameL1; 
    @Column(name = "urban_ward_no")
    private Integer urbanWardNo;
    @Column(name = "rur_district_union_no")
    private String rurDistrictUnionNo;
    @Column(name = "rur_district_union_name_en")
    private String rurDistrictUnionNameEn;
    @Column(name = "rur_district_union_name_l1")
    private String rurDistrictUnionNameL1;
    @Column(name = "rur_district_union_name_l2")
    private String rurDistrictUnionNameL2;
    @Column(name = "rur_district_union_ward_no")
    private String rurDistrictUnionWardNo;
    @Column(name = "pan_union_no")
    private String panUnionNo;
    @Column(name = "pan_union_name_en")
    private String panUnionNameEn;
    @Column(name = "pan_union_name_l1")
    private String panUnionNameL1;
    @Column(name = "pan_union_name_l2")
    private String panUnionNameL2;
    @Column(name = "pan_union_ward_no")
    private String panUnionWardNo;
    @Column(name = "vill_pan_no")
    private String villPanNo;
    @Column(name = "vill_pan_name_en")
    private String villPanNameEn;
    @Column(name = "vill_pan_name_l1")
    private String villPanNameL1;
    @Column(name = "vill_pan_ward_no")
    private String villPanWardNo;
    
    @Column(name = "availability")
    private String availability;

    @Column(name = "party_affiliation")
    private String partyAffiliation;
    
    @Column(name = "star_number")
    private Boolean starNumber;
    

    private String aadhaarNumber; 

    @Column(name = "pan_number")
    private String panNumber;  
    @Column(name = "party_registration_number")
    private String partyRegistrationNumber;  

    
    @ElementCollection
    @CollectionTable(name = "voter_dynamic_fields", joinColumns = @JoinColumn(name = "voter_id"))
    @MapKeyColumn(name = "field_name")
    @Column(name = "field_value")
    private Map<String, String> dynamicFields = new HashMap<>();

    @Column(name = "family_id")
    private UUID familyId;   
    @Column(name = "family_count", nullable = false, columnDefinition = "INTEGER DEFAULT 1")
    private Integer familyCount = 1;   
    @Column(name = "family_sequence_number")
    private Integer familySequenceNumber;
    @Column(name = "family_display_part")
    private Integer familyDisplayPart;  // Override for manual family part assignment
    @Column(name = "is_family_head", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean isFamilyHead = false;  // Designates family head for ordering
    @Column(name = "friend_id")
    private UUID friendId;
    @Column(name = "friend_count", nullable = false, columnDefinition = "INTEGER DEFAULT 0")
    private Integer friendCount = 0;
 // New field to store friend details as JSON
    @Column(name = "friends_details", columnDefinition = "TEXT")
    private String friendsDetails;
    
    // Activity counters for performance optimization
    @Column(name = "voter_slip_print_count", nullable = false, columnDefinition = "INTEGER DEFAULT 0")
    private Integer voterSlipPrintCount = 0;
    
    @Column(name = "family_slip_print_count", nullable = false, columnDefinition = "INTEGER DEFAULT 0")
    private Integer familySlipPrintCount = 0;
    
    @Column(name = "benefit_slip_print_count", nullable = false, columnDefinition = "INTEGER DEFAULT 0")
    private Integer benefitSlipPrintCount = 0;
    
    @Column(name = "whatsapp_share_count", nullable = false, columnDefinition = "INTEGER DEFAULT 0")
    private Integer whatsappShareCount = 0;
    
    @Column(name = "sms_share_count", nullable = false, columnDefinition = "INTEGER DEFAULT 0")
    private Integer smsShareCount = 0;
    
    @Column(name = "voice_share_count", nullable = false, columnDefinition = "INTEGER DEFAULT 0")
    private Integer voiceShareCount = 0;
   
    //@ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE})
    //@ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "voter_language",
        joinColumns = @JoinColumn(name = "voter_id"),
        inverseJoinColumns = @JoinColumn(name = "language_id")
    )
    @BatchSize(size = 100)
    @JsonSerialize(contentUsing = LanguageSerializer.class)
    private Set<Language> languages = new HashSet<>();
    
    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "benefit_scheme_id", referencedColumnName = "id")
    // @JsonSerialize(using = BenefitSchemesSerializer.class)
    // private BenefitSchemes benefitSchemes;
//    @ManyToMany(fetch = FetchType.LAZY)
//    @JoinTable(
//    name = "voter_benefit_schemes",  
//    joinColumns = @JoinColumn(name = "voter_id"),  
//    inverseJoinColumns = @JoinColumn(name = "benefit_scheme_id")  
//    )
//    @BatchSize(size = 100)
//    @JsonSerialize(contentUsing = BenefitSchemesSerializer.class)  
//    private List<BenefitSchemes> benefitSchemes = new ArrayList<>();
//    
    @OneToMany(mappedBy = "voter", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    @BatchSize(size = 100)
    private List<VoterBenefitScheme> voterBenefitSchemes = new ArrayList<>();

    @Column(name = "scheme")
    private String scheme;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "availability_id", referencedColumnName = "id")
    @JsonSerialize(using = AvailabilitySerializer.class)
    private Availability availability1;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dynamic_field_id", referencedColumnName = "id")
    @JsonSerialize(using = DynamicFieldSerializer.class)
    private DynamicFieldEntity dynamicFieldEntity;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_id", referencedColumnName = "id")
    @JsonSerialize(using = PartySerializer.class)
    private Party party;
    //private List<String> validationErrors = new ArrayList<>();
    @Column(name = "page_number")
    private Integer pageNumber;   
    private String remarks;
    
    @Column(name = "video_url")
    @JsonProperty("video_url")
    private String videoUrl;
    @Column(name = "otp")
    private String otp;

    @Column(name = "otp_created_at")
    private LocalDateTime otpCreatedAt;

//    @Column(name = "otp_is_active", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
//    private Boolean otpIsActive = false;
    
    @Column(name = "mobile_verified", nullable = true, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean mobileVerified = false;
    
 // New fields added as per request
    @Column(name = "aadhaar_verified", nullable = true, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean aadhaarVerified = false;

    @Column(name = "member_verified", nullable = true, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean memberVerified = false;
    
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "voter_feedback_issues",
        joinColumns = @JoinColumn(name = "voter_id"),
        inverseJoinColumns = @JoinColumn(name = "feedback_issue_id")
    )
    @BatchSize(size = 100)
    private Set<FeedbackIssue> feedbackIssues = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "voter_voter_history",
        joinColumns = @JoinColumn(name = "voter_id"),
        inverseJoinColumns = @JoinColumn(name = "voter_history_id")
    )
    @BatchSize(size = 100)
    private Set<VoterHistoryEntity> voterHistories = new HashSet<>();
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "part_manager_id")
    @JsonSerialize(using = PartManagerPostgresSerializer.class)
    private PartManager partManager;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caste_category_id", referencedColumnName = "id")
    @JsonProperty("casteCategory")
    @JsonSerialize(using = CasteCategorySerializer.class)
    private CasteCategoryEntity casteCategory;
    
    @Column(name = "created_by_user_id")
    @JsonProperty("created_by_user_id")
    private Long createdByUserId;
    
 // Helper methods for JSON serialization/deserialization of friendsDetails
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public List<FriendDetail> getFriendsDetails() {
        if (friendsDetails == null || friendsDetails.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(friendsDetails, new TypeReference<List<FriendDetail>>(){});
        } catch (IOException e) {
            throw new RuntimeException("Error deserializing friendsDetails", e);
        }
    }

    public void setFriendsDetails(List<FriendDetail> friends) {
        try {
            this.friendsDetails = friends != null ? objectMapper.writeValueAsString(friends) : null;
        } catch (IOException e) {
            throw new RuntimeException("Error serializing friendsDetails", e);
        }
    }
    
    
    @PrePersist
    protected void onCreate() {
        if (voterFnameEn != null) {
            voterFnameEn = voterFnameEn.trim();
        }
        if (voterLnameEn != null) {
            voterLnameEn = voterLnameEn.trim();
        }
        if (createdTime == null) {
            createdTime = LocalDateTime.now();
        }
        if (voterFnameL1 != null) {
            voterFnameL1 = voterFnameL1.trim();
        }
        if (voterFnameL2 != null) {
            voterFnameL2 = voterFnameL2.trim();
        }
        if (voterLnameL1 != null) {
            voterLnameL1 = voterLnameL1.trim();
        }
        if (voterLnameL2 != null) {
            voterLnameL2 = voterLnameL2.trim();
        }
        if (familyCount == null) {
            familyCount = 1;
        }
        modifiedTime = LocalDateTime.now(); // Set modifiedTime on create as well
    }

    @PreUpdate
    protected void onUpdate() {
        if (voterFnameEn != null) {
            voterFnameEn = voterFnameEn.trim();
        }
        if (voterLnameEn != null) {
            voterLnameEn = voterLnameEn.trim();
        }
        if (voterFnameL1 != null) {
            voterFnameL1 = voterFnameL1.trim();
        }
        if (voterFnameL2 != null) {
            voterFnameL2 = voterFnameL2.trim();
        }
        if (voterLnameL1 != null) {
            voterLnameL1 = voterLnameL1.trim();
        }
        if (voterLnameL2 != null) {
            voterLnameL2 = voterLnameL2.trim();
        }
        modifiedTime = LocalDateTime.now();
    }

//    @PrePersist
//    protected void onCreate() {
//        if (createdTime == null) {
//            createdTime = LocalDateTime.now();
//        }
//        if (familyCount == null) {
//            familyCount = 1;
//        }
//    }
//    @PreUpdate
//    protected void onUpdate() {
//        modifiedTime = LocalDateTime.now();
//    }
    public Boolean getHasVoted() {
        return hasVoted != null ? hasVoted : false;
    }
    public Boolean getMobileVerified() {
        return mobileVerified != null ? mobileVerified : false;
    }
    public Boolean getAadhaarVerified() {
        return aadhaarVerified != null ? aadhaarVerified : false;
    }

    public Boolean getMemberVerified() {
        return memberVerified != null ? memberVerified : false;
    }

//    public Boolean getOtpIsActive() {
//        return otpIsActive != null ? otpIsActive : false;
//    }
    
	public void setPartNoAndBoothNumber(Integer partNoValue) {
		// TODO Auto-generated method stub
		this.partNo = partNoValue;
	    this.boothNumber = partNoValue;
	}
	    
}