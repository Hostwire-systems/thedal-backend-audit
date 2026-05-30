package com.thedal.thedal_app.voter.dto;

import java.util.List;
import java.util.UUID;

public class FamilySummaryDTO {
    private UUID familyId;
    private Integer familySequenceNumber;  // NEW: For family numbering (Family 1, Family 2, etc.)
    private Integer memberCount;
    private FirstMemberDTO firstMember;
    
    public FamilySummaryDTO() {}
    
    // Backward compatibility constructor (without sequence number)
    public FamilySummaryDTO(UUID familyId, Integer memberCount, FirstMemberDTO firstMember) {
        this.familyId = familyId;
        this.familySequenceNumber = null;  // Will be null for existing families without numbering
        this.memberCount = memberCount;
        this.firstMember = firstMember;
    }
    
    // New constructor with sequence number
    public FamilySummaryDTO(UUID familyId, Integer familySequenceNumber, Integer memberCount, FirstMemberDTO firstMember) {
        this.familyId = familyId;
        this.familySequenceNumber = familySequenceNumber;
        this.memberCount = memberCount;
        this.firstMember = firstMember;
    }
    
    // Getters and Setters
    public UUID getFamilyId() { return familyId; }
    public void setFamilyId(UUID familyId) { this.familyId = familyId; }
    
    public Integer getFamilySequenceNumber() { return familySequenceNumber; }
    public void setFamilySequenceNumber(Integer familySequenceNumber) { this.familySequenceNumber = familySequenceNumber; }
    
    public Integer getMemberCount() { return memberCount; }
    public void setMemberCount(Integer memberCount) { this.memberCount = memberCount; }
    
    public FirstMemberDTO getFirstMember() { return firstMember; }
    public void setFirstMember(FirstMemberDTO firstMember) { this.firstMember = firstMember; }
    
    public static class FirstMemberDTO {
        private String name;
        private String epicNumber;
        private Integer age;
        private String gender;
        private Integer partNo;
        // Additional fields requested by frontend
        private Long serialNo;
        private String mobileNo;
        private String rlnType;
        private String voterFnameEn;
        private String voterLnameEn;
        private String voterFnameL1;
        private String voterLnameL1;
        private String rlnFnameEn;
        private String rlnLnameEn;
        private String rlnFnameL1;
        private String rlnLnameL1;
        private Boolean memberVerified;
        private Boolean aadhaarVerified;
    // Newly added fields for first member details in family summary
    private Long availabilityId;
    private Long partyId;
    private String aadhaarNumber;
    private String panNumber;
    private String photoUrl;
    // Display names (added without changing constructor)
    private String availabilityName;
    private String partyName;
    // Voting history - list of historical elections this voter participated in
    private List<VoterHistoryDTO> votingHistory;
        
        public FirstMemberDTO() {}
        
        // Backward compatibility constructor
        public FirstMemberDTO(String name, String epicNumber, Integer age, String gender, Integer partNo) {
            this.name = name;
            this.epicNumber = epicNumber;
            this.age = age;
            this.gender = gender;
            this.partNo = partNo;
        }
        
        // Enhanced constructor with all fields
    public FirstMemberDTO(String name, String epicNumber, Integer age, String gender, Integer partNo,
                 Long serialNo, String mobileNo, String rlnType, String voterFnameEn, String voterLnameEn,
                 String voterFnameL1, String voterLnameL1, String rlnFnameEn, String rlnLnameEn,
                 String rlnFnameL1, String rlnLnameL1, Boolean memberVerified, Boolean aadhaarVerified,
                 Long availabilityId, Long partyId, String aadhaarNumber, String panNumber, String photoUrl) {
            this.name = name;
            this.epicNumber = epicNumber;
            this.age = age;
            this.gender = gender;
            this.partNo = partNo;
            this.serialNo = serialNo;
            this.mobileNo = mobileNo;
            this.rlnType = rlnType;
            this.voterFnameEn = voterFnameEn;
            this.voterLnameEn = voterLnameEn;
            this.voterFnameL1 = voterFnameL1;
            this.voterLnameL1 = voterLnameL1;
            this.rlnFnameEn = rlnFnameEn;
            this.rlnLnameEn = rlnLnameEn;
            this.rlnFnameL1 = rlnFnameL1;
            this.rlnLnameL1 = rlnLnameL1;
            this.memberVerified = memberVerified;
            this.aadhaarVerified = aadhaarVerified;
        this.availabilityId = availabilityId;
        this.partyId = partyId;
        this.aadhaarNumber = aadhaarNumber;
        this.panNumber = panNumber;
        this.photoUrl = photoUrl;
        }
        
        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getEpicNumber() { return epicNumber; }
        public void setEpicNumber(String epicNumber) { this.epicNumber = epicNumber; }
        
        public Integer getAge() { return age; }
        public void setAge(Integer age) { this.age = age; }
        
        public String getGender() { return gender; }
        public void setGender(String gender) { this.gender = gender; }
        
        public Integer getPartNo() { return partNo; }
        public void setPartNo(Integer partNo) { this.partNo = partNo; }
        
        public Long getSerialNo() { return serialNo; }
        public void setSerialNo(Long serialNo) { this.serialNo = serialNo; }
        
        public String getMobileNo() { return mobileNo; }
        public void setMobileNo(String mobileNo) { this.mobileNo = mobileNo; }
        
        public String getRlnType() { return rlnType; }
        public void setRlnType(String rlnType) { this.rlnType = rlnType; }
        
        public String getVoterFnameEn() { return voterFnameEn; }
        public void setVoterFnameEn(String voterFnameEn) { this.voterFnameEn = voterFnameEn; }
        
        public String getVoterLnameEn() { return voterLnameEn; }
        public void setVoterLnameEn(String voterLnameEn) { this.voterLnameEn = voterLnameEn; }
        
        public String getVoterFnameL1() { return voterFnameL1; }
        public void setVoterFnameL1(String voterFnameL1) { this.voterFnameL1 = voterFnameL1; }
        
        public String getVoterLnameL1() { return voterLnameL1; }
        public void setVoterLnameL1(String voterLnameL1) { this.voterLnameL1 = voterLnameL1; }
        
        public String getRlnFnameEn() { return rlnFnameEn; }
        public void setRlnFnameEn(String rlnFnameEn) { this.rlnFnameEn = rlnFnameEn; }
        
        public String getRlnLnameEn() { return rlnLnameEn; }
        public void setRlnLnameEn(String rlnLnameEn) { this.rlnLnameEn = rlnLnameEn; }
        
        public String getRlnFnameL1() { return rlnFnameL1; }
        public void setRlnFnameL1(String rlnFnameL1) { this.rlnFnameL1 = rlnFnameL1; }
        
        public String getRlnLnameL1() { return rlnLnameL1; }
        public void setRlnLnameL1(String rlnLnameL1) { this.rlnLnameL1 = rlnLnameL1; }
        
        public Boolean getMemberVerified() { return memberVerified; }
        public void setMemberVerified(Boolean memberVerified) { this.memberVerified = memberVerified; }
        
        public Boolean getAadhaarVerified() { return aadhaarVerified; }
        public void setAadhaarVerified(Boolean aadhaarVerified) { this.aadhaarVerified = aadhaarVerified; }

    public Long getAvailabilityId() { return availabilityId; }
    public void setAvailabilityId(Long availabilityId) { this.availabilityId = availabilityId; }

    public Long getPartyId() { return partyId; }
    public void setPartyId(Long partyId) { this.partyId = partyId; }

    public String getAadhaarNumber() { return aadhaarNumber; }
    public void setAadhaarNumber(String aadhaarNumber) { this.aadhaarNumber = aadhaarNumber; }

    public String getPanNumber() { return panNumber; }
    public void setPanNumber(String panNumber) { this.panNumber = panNumber; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public String getAvailabilityName() { return availabilityName; }
    public void setAvailabilityName(String availabilityName) { this.availabilityName = availabilityName; }

    public String getPartyName() { return partyName; }
    public void setPartyName(String partyName) { this.partyName = partyName; }
    
    public List<VoterHistoryDTO> getVotingHistory() { return votingHistory; }
    public void setVotingHistory(List<VoterHistoryDTO> votingHistory) { this.votingHistory = votingHistory; }
    }
}
