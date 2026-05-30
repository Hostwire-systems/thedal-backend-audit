package com.thedal.thedal_app.voter.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.thedal.thedal_app.voter.MemberEntity;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberDTO {
	
	
    private Long id;
	@JsonIgnore
    private Long accountId;
	@JsonIgnore
    private Long electionId;
    private String memberName;
    private String relationName;
    private String relationType;
    private String gender;
    private LocalDate dateOfBirth;
    private Integer age;
    private String occupation;
    private String education;
    private String fullAddress;
    private String mobileNumber;
    private Integer memberSinceYear;
    private String membershipNo;
    // State & District Info
    private String stateNameEn;
    private String stateNameL1;
    private String stateNameL2;
    private String districtCode;
    private String districtNameEn;
    private String districtNameL1;
    private String districtNameL2;
    // PC & AC Information
    private String pcNo;
    private String pcNameEn;
    private String pcNameL1;
    private String pcNameL2;
    private String acNo;
    private String acNameEn;
    private String acNameL1;
    private String acNameL2;
    // Urban Local Body Information
    private String urbanNo;
    private String urbanNameEn;
    private String urbanNameL1;
    private String urbanWardNo;
    // Rural Local Body Information
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

  	private String epicNumber;
    private String stateCode;
    
    public MemberDTO(MemberEntity memberEntity) {
        this.id = memberEntity.getId();
        this.accountId = memberEntity.getAccountId();
        this.electionId = memberEntity.getElectionId();
        this.memberName = memberEntity.getMemberName();
        this.relationName = memberEntity.getRelationName();
        this.relationType = memberEntity.getRelationType();
        this.gender = memberEntity.getGender();
        this.dateOfBirth = memberEntity.getDateOfBirth();
        this.age = memberEntity.getAge();
        this.occupation = memberEntity.getOccupation();
        this.education = memberEntity.getEducation();
        this.fullAddress = memberEntity.getFullAddress();
        this.mobileNumber = memberEntity.getMobileNumber();
        this.memberSinceYear = memberEntity.getMemberSinceYear();
        this.membershipNo = memberEntity.getMembershipNo();
        
        // State & District Info
        this.stateNameEn = memberEntity.getStateNameEn();
        this.stateNameL1 = memberEntity.getStateNameL1();
        this.stateNameL2 = memberEntity.getStateNameL2();
        this.districtCode = memberEntity.getDistrictCode();
        this.districtNameEn = memberEntity.getDistrictNameEn();
        this.districtNameL1 = memberEntity.getDistrictNameL1();
        this.districtNameL2 = memberEntity.getDistrictNameL2();

        // PC & AC Information
        this.pcNo = memberEntity.getPcNo();
        this.pcNameEn = memberEntity.getPcNameEn();
        this.pcNameL1 = memberEntity.getPcNameL1();
        this.pcNameL2 = memberEntity.getPcNameL2();
        this.acNo = memberEntity.getAcNo();
        this.acNameEn = memberEntity.getAcNameEn();
        this.acNameL1 = memberEntity.getAcNameL1();
        this.acNameL2 = memberEntity.getAcNameL2();

        // Urban Local Body Information
        this.urbanNo = memberEntity.getUrbanNo();
        this.urbanNameEn = memberEntity.getUrbanNameEn();
        this.urbanNameL1 = memberEntity.getUrbanNameL1();
        this.urbanWardNo = memberEntity.getUrbanWardNo();

        // Rural Local Body Information
        this.rurDistrictUnionNo = memberEntity.getRurDistrictUnionNo();
        this.rurDistrictUnionNameEn = memberEntity.getRurDistrictUnionNameEn();
        this.rurDistrictUnionNameL1 = memberEntity.getRurDistrictUnionNameL1();
        this.rurDistrictUnionNameL2 = memberEntity.getRurDistrictUnionNameL2();
        this.rurDistrictUnionWardNo = memberEntity.getRurDistrictUnionWardNo();
        this.panUnionNo = memberEntity.getPanUnionNo();
        this.panUnionNameEn = memberEntity.getPanUnionNameEn();
        this.panUnionNameL1 = memberEntity.getPanUnionNameL1();
        this.panUnionNameL2 = memberEntity.getPanUnionNameL2();
        this.panUnionWardNo = memberEntity.getPanUnionWardNo();
        this.villPanNo = memberEntity.getVillPanNo();
        this.villPanNameEn = memberEntity.getVillPanNameEn();
        this.villPanNameL1 = memberEntity.getVillPanNameL1();
        this.villPanWardNo = memberEntity.getVillPanWardNo();
        this.epicNumber = memberEntity.getEpicNumber();
        this.stateCode = memberEntity.getStateCode();


    }


}
