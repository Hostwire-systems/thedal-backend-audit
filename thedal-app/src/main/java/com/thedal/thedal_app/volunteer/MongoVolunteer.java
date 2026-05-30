package com.thedal.thedal_app.volunteer;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.List;
import com.thedal.thedal_app.voter.Address;
import com.thedal.thedal_app.user.UserEntity;
import com.thedal.thedal_app.election.ElectionEntity;

@Document(collection = "volunteers")
public class MongoVolunteer {
    @Id
    private String id;
    private String lastName;
    private String email;
    private String mobileNumber;
    private List<Long> assignedBooth;
    private String status;
    private String photoUrl;
    private String remarks;
    private Address volunteerAddress;
    private Long accountId;
    private LocalDateTime createdTime;
    private LocalDateTime modifiedTime;
    private UserEntity userEntity;
    private ElectionEntity electionEntity;
    private String whatsAppNumber;
    private String gender;
    private Long roleId;
    private Long adminUserId;

    // id
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    // lastName
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    // email
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    // mobileNumber
    public String getMobileNumber() { return mobileNumber; }
    public void setMobileNumber(String mobileNumber) { this.mobileNumber = mobileNumber; }
    // assignedBooth
    public List<Long> getAssignedBooth() { return assignedBooth; }
    public void setAssignedBooth(List<Long> assignedBooth) { this.assignedBooth = assignedBooth; }
    // status
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    // photoUrl
    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
    // remarks
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    // volunteerAddress
    public Address getVolunteerAddress() { return volunteerAddress; }
    public void setVolunteerAddress(Address volunteerAddress) { this.volunteerAddress = volunteerAddress; }
    // accountId
    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
    // createdTime
    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }
    // modifiedTime
    public LocalDateTime getModifiedTime() { return modifiedTime; }
    public void setModifiedTime(LocalDateTime modifiedTime) { this.modifiedTime = modifiedTime; }
    // userEntity
    public UserEntity getUserEntity() { return userEntity; }
    public void setUserEntity(UserEntity userEntity) { this.userEntity = userEntity; }
    // electionEntity
    public ElectionEntity getElectionEntity() { return electionEntity; }
    public void setElectionEntity(ElectionEntity electionEntity) { this.electionEntity = electionEntity; }
    // whatsAppNumber
    public String getWhatsAppNumber() { return whatsAppNumber; }
    public void setWhatsAppNumber(String whatsAppNumber) { this.whatsAppNumber = whatsAppNumber; }
    // gender
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    // roleId
    public Long getRoleId() { return roleId; }
    public void setRoleId(Long roleId) { this.roleId = roleId; }
    
    public Long getAdminUserId() { return adminUserId; }
    public void setAdminUserId(Long adminUserId) { this.adminUserId = adminUserId; }
}
