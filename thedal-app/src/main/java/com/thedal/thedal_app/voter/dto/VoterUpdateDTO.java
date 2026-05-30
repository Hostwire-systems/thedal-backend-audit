package com.thedal.thedal_app.voter.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class VoterUpdateDTO {
	
//	@NotBlank(message = "Voter ID is mandatory")
//    @Column(name = "voter_id")
//    private String voterId;
	private String epicNumber;
 
    @JsonProperty("photo_url")
    private String photoUrl;
    @JsonIgnore
    @Column(name = "account_id")
    private Long accountId;   
    @JsonIgnore
    @Column(name = "election_id")
    private Long electionId;    
    @JsonProperty("booth_number")
    private Integer boothNumber;
    
/////////////////////////////
    
 // Part Information
    //@NotNull(message = "Part Number or Booth Number is mandatory")
    @Column(name = "part_no")
    private Integer partNo;
    @Column(name = "section_no")
    private Integer sectionNo;
    //@NotNull(message = "Serial Number of Voter is mandatory")
    @Column(name = "serial_no")
    private Long serialNo;
    //@NotNull(message = "House Number of Voter in English is mandatory")
    @Column(name = "house_no_en")
    private String houseNoEn;   
    @Column(name = "house_no_l1")
    private String houseNoL1;
    @Column(name = "house_no_l2")
    private String houseNoL2;
    //@NotNull(message = "Voter First Name in English is mandatory")
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
    //@NotNull(message = "Relationship Type like Father,Mother, Husband is mandatory")
    @Column(name = "rln_type")
    private String rlnType;
    //@NotNull(message = "Relation First Name in English is mandatory")
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
//    @JsonIgnore
//	//@NotNull(message = "EPIC Number or Voter Id Number is mandatory")
////  @Column(name = "epic_number", nullable = false, unique = true)
//	@Column(name = "epic_number", nullable = false)
//    @JsonProperty("epic_number")
//	private String epicNumber;
    
    @Column(name = "gender")
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
//    @Column(name = "part_lati")
//    private String partLati;
//    @Column(name = "part_long")
//    private String partLong;
    @Column(name = "part_lati")
    private Double partLati;
    @Column(name = "part_long")
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
    @Column(name = "voter_lati")
    private Double voterLati;
    @Column(name = "voter_longi")
    private Double voterLongi;
    @Column(name = "religion")
    private String religion;
    @Column(name = "caste")
    private String caste;   
    @Column(name = "sub_caste")
    @JsonProperty("sub_caste")
    private String subCaste;
    
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
    
    //private List<String> languages;
//    @Column(name = "availability")
//    private String availability;
//
//    @Column(name = "scheme")
//    private String scheme;
//
//    @Column(name = "party_affiliation")
//    private String partyAffiliation;
   

    // private Long benefitSchemeId;
    //private List<Long> benefitSchemeIds;
    private List<BenefitSchemeStatusDTO> benefitSchemeStatuses;

    
    private Long languageId;
    // private List<Long> languageIds;

    private Long availabilityId;
    @JsonProperty("dynamic_field_id")
    private Long dynamicFieldId;
    private Long partyId;
    private Long religionId;
    private Long casteId;	    
    private Long subCasteId;
    private Integer pageNumber;
    private String remarks;
    private Boolean starNumber;

    private String aadhaarNumber;

    private String panNumber; 
    private String partyRegistrationNumber;
    private Boolean mobileVerified;
    private Boolean aadhaarVerified;
    private Boolean memberVerified;
    private String partyAffiliation;

    private List<Long> feedbackIssueIds;
    private List<Long> voterHistoryIds;
    private Long casteCategoryId;

    // New: dynamic field values (key = definition name, value = user input)
    private Map<String,Object> dynamicFields;

    @Getter @Setter
    public static class BenefitSchemeStatusDTO {
        private Long schemeId;
        private boolean selected;
    }
    
}

