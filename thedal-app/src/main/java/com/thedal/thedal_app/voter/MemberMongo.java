package com.thedal.thedal_app.voter;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Document(collection = "members")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MemberMongo {

    @Id
    private String id;
    
    @Field("member_id")
    private Long memberId; // Reference to PostgreSQL ID
    
    @Field("account_id")
    private Long accountId;
    
    @Field("election_id")
    private Long electionId;
    
    @Field("member_name")
    private String memberName;
    
    @Field("relation_name")
    private String relationName;
    
    @Field("relation_type")
    private String relationType;
    
    @Field("gender")
    private String gender;
    
    @Field("date_of_birth")
    private LocalDate dateOfBirth;
    
    @Field("age")
    private Integer age;
    
    @Field("occupation")
    private String occupation;
    
    @Field("education")
    private String education;
    
    @Field("full_address")
    private String fullAddress;
    
    @Field("mobile_number")
    private String mobileNumber;
    
    @Field("member_since_year")
    private Integer memberSinceYear;
    
    @Field("membership_no")
    private String membershipNo;
    
    // State & District Info
    @Field("state_name_en")
    private String stateNameEn;
    
    @Field("state_name_l1")
    private String stateNameL1;
    
    @Field("state_name_l2")
    private String stateNameL2;
    
    @Field("district_code")
    private String districtCode;
    
    @Field("district_name_en")
    private String districtNameEn;
    
    @Field("district_name_l1")
    private String districtNameL1;
    
    @Field("district_name_l2")
    private String districtNameL2;
    
    // PC & AC Information
    @Field("pc_no")
    private String pcNo;
    
    @Field("pc_name_en")
    private String pcNameEn;
    
    @Field("pc_name_l1")
    private String pcNameL1;
    
    @Field("pc_name_l2")
    private String pcNameL2;
    
    @Field("ac_no")
    private String acNo;
    
    @Field("ac_name_en")
    private String acNameEn;
    
    @Field("ac_name_l1")
    private String acNameL1;
    
    @Field("ac_name_l2")
    private String acNameL2;
    
    // Urban Local Body Information
    @Field("urban_no")
    private String urbanNo;
    
    @Field("urban_name_en")
    private String urbanNameEn;
    
    @Field("urban_name_l1")
    private String urbanNameL1;
    
    @Field("urban_ward_no")
    private String urbanWardNo;
    
    // Rural Local Body Information
    @Field("rur_district_union_no")
    private String rurDistrictUnionNo;
    
    @Field("rur_district_union_name_en")
    private String rurDistrictUnionNameEn;
    
    @Field("rur_district_union_name_l1")
    private String rurDistrictUnionNameL1;
    
    @Field("rur_district_union_name_l2")
    private String rurDistrictUnionNameL2;
    
    @Field("rur_district_union_ward_no")
    private String rurDistrictUnionWardNo;
    
    @Field("pan_union_no")
    private String panUnionNo;
    
    @Field("pan_union_name_en")
    private String panUnionNameEn;
    
    @Field("pan_union_name_l1")
    private String panUnionNameL1;
    
    @Field("pan_union_name_l2")
    private String panUnionNameL2;
    
    @Field("pan_union_ward_no")
    private String panUnionWardNo;
    
    @Field("vill_pan_no")
    private String villPanNo;
    
    @Field("vill_pan_name_en")
    private String villPanNameEn;
    
    @Field("vill_pan_name_l1")
    private String villPanNameL1;
    
    @Field("vill_pan_ward_no")
    private String villPanWardNo;
    
    @Field("epic_number")
    private String epicNumber;
    
    @Field("state_code")
    private String stateCode;
    
    // Timestamps
    @Field("created_time")
    private LocalDateTime createdTime;
    
    @Field("modified_time")
    private LocalDateTime modifiedTime;
    
    // Constructor to create from PostgreSQL entity
    public MemberMongo(MemberEntity entity) {
        this.memberId = entity.getId();
        this.accountId = entity.getAccountId();
        this.electionId = entity.getElectionId();
        this.memberName = entity.getMemberName();
        this.relationName = entity.getRelationName();
        this.relationType = entity.getRelationType();
        this.gender = entity.getGender();
        this.dateOfBirth = entity.getDateOfBirth();
        this.age = entity.getAge();
        this.occupation = entity.getOccupation();
        this.education = entity.getEducation();
        this.fullAddress = entity.getFullAddress();
        this.mobileNumber = entity.getMobileNumber();
        this.memberSinceYear = entity.getMemberSinceYear();
        this.membershipNo = entity.getMembershipNo();
        this.stateNameEn = entity.getStateNameEn();
        this.stateNameL1 = entity.getStateNameL1();
        this.stateNameL2 = entity.getStateNameL2();
        this.districtCode = entity.getDistrictCode();
        this.districtNameEn = entity.getDistrictNameEn();
        this.districtNameL1 = entity.getDistrictNameL1();
        this.districtNameL2 = entity.getDistrictNameL2();
        this.pcNo = entity.getPcNo();
        this.pcNameEn = entity.getPcNameEn();
        this.pcNameL1 = entity.getPcNameL1();
        this.pcNameL2 = entity.getPcNameL2();
        this.acNo = entity.getAcNo();
        this.acNameEn = entity.getAcNameEn();
        this.acNameL1 = entity.getAcNameL1();
        this.acNameL2 = entity.getAcNameL2();
        this.urbanNo = entity.getUrbanNo();
        this.urbanNameEn = entity.getUrbanNameEn();
        this.urbanNameL1 = entity.getUrbanNameL1();
        this.urbanWardNo = entity.getUrbanWardNo();
        this.rurDistrictUnionNo = entity.getRurDistrictUnionNo();
        this.rurDistrictUnionNameEn = entity.getRurDistrictUnionNameEn();
        this.rurDistrictUnionNameL1 = entity.getRurDistrictUnionNameL1();
        this.rurDistrictUnionNameL2 = entity.getRurDistrictUnionNameL2();
        this.rurDistrictUnionWardNo = entity.getRurDistrictUnionWardNo();
        this.panUnionNo = entity.getPanUnionNo();
        this.panUnionNameEn = entity.getPanUnionNameEn();
        this.panUnionNameL1 = entity.getPanUnionNameL1();
        this.panUnionNameL2 = entity.getPanUnionNameL2();
        this.panUnionWardNo = entity.getPanUnionWardNo();
        this.villPanNo = entity.getVillPanNo();
        this.villPanNameEn = entity.getVillPanNameEn();
        this.villPanNameL1 = entity.getVillPanNameL1();
        this.villPanWardNo = entity.getVillPanWardNo();
        this.epicNumber = entity.getEpicNumber();
        this.stateCode = entity.getStateCode();
        this.createdTime = entity.getCreatedTime();
        this.modifiedTime = entity.getModifiedTime();
    }
}
