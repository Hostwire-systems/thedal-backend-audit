package com.thedal.thedal_app.voter;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "members")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MemberEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @JsonIgnore
    @Column(name = "account_id")
    private Long accountId;
    @JsonIgnore
    @Column(name = "election_id")
    private Long electionId;
    @Column(name = "member_name")
    private String memberName;
    @Column(name = "relation_name")
    private String relationName;
    @Column(name = "relation_type")
    private String relationType;
    @Column(name = "gender")
    private String gender;
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;
    @Column(name = "age")
    private Integer age;
    @Column(name = "occupation")
    private String occupation;
    @Column(name = "education")
    private String education;
    @Column(name = "full_address")
    private String fullAddress;
    @Column(name = "mobile_number")
    private String mobileNumber;
    @Column(name = "member_since_year")
    private Integer memberSinceYear;
    @Column(name = "membership_no")
    private String membershipNo;
    // State & District Info
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
    // PC & AC Information
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
    // Urban Local Body Information
    @Column(name = "urban_no")
    private String urbanNo;
    @Column(name = "urban_name_en")
    private String urbanNameEn;
    @Column(name = "urban_name_l1")
    private String urbanNameL1;
    @Column(name = "urban_ward_no")
    private String urbanWardNo;
    // Rural Local Body Information
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
    
    @Column(name = "epic_number")
  	private String epicNumber;

    @Column(name = "state_code")
    private String stateCode;
   
    // Timestamps
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdTime;
    
    @UpdateTimestamp
    private LocalDateTime modifiedTime;

}
