package com.thedal.thedal_app.election.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class PartManagerDTO {
    
    public PartManagerDTO() {
        // Default no-arg constructor for Jackson
    }
    
    private String partNo;
    private String partNameEnglish;
    private String partNameL1;
    private String partType;
    private String schoolName;
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Double partLat;
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Double partLong;
    
    private String pincode;
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Double schoolLat;
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Double schoolLong; 
    private String boothVulnerability;
    private Integer orderIndex;
    
    private String partCaptainName;
    private String captainDesignation;
    private String captainMobileNo;
    
    private String bloName;
    private String bloDesignation;
    private String bloMobileNumber;
    
    private String bla2Name;
    private String bla2Designation;
    private String bla2MobileNumber;
    
    private String partImageUrl;
    
    private java.util.List<BoothCommitteeMemberDTO> boothCommitteeMembers;
   

    public String getPartNo() {
        return partNo;
    }

    public void setPartNo(String partNo) {
        this.partNo = partNo;
    }

    public String getPartNameEnglish() {
        return partNameEnglish;
    }

    public void setPartNameEnglish(String partNameEnglish) {
        this.partNameEnglish = partNameEnglish;
    }

    public String getPartNameL1() {
        return partNameL1;
    }

    public void setPartNameL1(String partNameL1) {
        this.partNameL1 = partNameL1;
    }

    public String getPartType() {
        return partType;
    }

    public void setPartType(String partType) {
        this.partType = partType;
    }

    public String getSchoolName() {
        return schoolName;
    }

    public void setSchoolName(String schoolName) {
        this.schoolName = schoolName;
    }

    public Double getPartLat() {
        return partLat;
    }

    public void setPartLat(Double partLat) {
        this.partLat = partLat;
    }

    public Double getPartLong() {
        return partLong;
    }

    public void setPartLong(Double partLong) {
        this.partLong = partLong;
    }

    public String getPincode() {
        return pincode;
    }

    public void setPincode(String pincode) {
        this.pincode = pincode;
    }

    public Double getSchoolLat() {
        return schoolLat;
    }

    public void setSchoolLat(Double schoolLat) {
        this.schoolLat = schoolLat;
    }

    public Double getSchoolLong() {
        return schoolLong;
    }

    public void setSchoolLong(Double schoolLong) {
        this.schoolLong = schoolLong;
    }

    public String getBoothVulnerability() {
        return boothVulnerability;
    }

    public void setBoothVulnerability(String boothVulnerability) {
        this.boothVulnerability = boothVulnerability;
    }
    
    public PartManagerDTO(String partNo, String partNameEnglish, String partNameL1, String schoolName, 
    Double partLat, Double partLong, String pincode, Double schoolLat, Double schoolLong) {
        this.partNo = partNo;
        this.partNameEnglish = partNameEnglish;
        this.partNameL1 = partNameL1;
        this.schoolName = schoolName;
        this.partLat = partLat;
        this.partLong = partLong;
        this.pincode = pincode;
        this.schoolLat = schoolLat;
        this.schoolLong = schoolLong;
        }
    

}


