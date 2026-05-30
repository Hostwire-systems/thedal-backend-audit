package com.thedal.thedal_app.election.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.thedal.thedal_app.election.PartManager;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PartManagerResponseDTO {


    private Long id;
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

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public PartManagerResponseDTO(Long id, String partNo, String partNameEnglish, String partNameL1, String partType, String schoolName, Double schoolLat, Double schoolLong,
            Double partLat, Double partLong, String pincode,
            String partCaptainName, String captainDesignation, String captainMobileNo) {
        this.id = id;
        this.partNo = partNo;
        this.partNameEnglish = partNameEnglish;
        this.partNameL1 = partNameL1;
        this.partType = partType;
        this.schoolName = schoolName;
        this.schoolLat = schoolLat;
        this.schoolLong = schoolLong;
        this.partLat = partLat;
        this.partLong = partLong;
        this.pincode = pincode;
        this.partCaptainName = partCaptainName;
        this.captainDesignation = captainDesignation;
        this.captainMobileNo = captainMobileNo;
    }


    public PartManagerResponseDTO(PartManager partManager) {
        this.id = partManager.getId();
        this.partNo = partManager.getPartNo();
        this.partNameEnglish = partManager.getPartNameEnglish();
        this.partNameL1 = partManager.getPartNameL1();
        this.partType = partManager.getPartType();
        this.schoolName = partManager.getSchoolName();
        this.schoolLat = partManager.getSchoolLat(); 
        this.schoolLong = partManager.getSchoolLong();
        this.partLat = partManager.getPartLat();
        this.partLong = partManager.getPartLong();
        this.pincode = partManager.getPincode();
        this.boothVulnerability = partManager.getBoothVulnerability();
        this.partCaptainName = partManager.getPartCaptainName();
        this.captainDesignation = partManager.getCaptainDesignation();
        this.captainMobileNo = partManager.getCaptainMobileNo();
        this.bloName = partManager.getBloName();
        this.bloDesignation = partManager.getBloDesignation();
        this.bloMobileNumber = partManager.getBloMobileNumber();
        this.bla2Name = partManager.getBla2Name();
        this.bla2Designation = partManager.getBla2Designation();
        this.bla2MobileNumber = partManager.getBla2MobileNumber();
        this.partImageUrl = partManager.getPartImageUrl();
        this.boothCommitteeMembers = convertJsonToCommitteeMembers(partManager.getBoothCommitteeMembers());
    }
    
    /**
     * Convert JSON string to List of BoothCommitteeMemberDTO
     */
    private java.util.List<BoothCommitteeMemberDTO> convertJsonToCommitteeMembers(String json) {
        if (json == null || json.trim().isEmpty() || "[]".equals(json.trim())) {
            return new java.util.ArrayList<>();
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<java.util.List<BoothCommitteeMemberDTO>>() {});
        } catch (Exception e) {
            return new java.util.ArrayList<>();
        }
    }

    
}
