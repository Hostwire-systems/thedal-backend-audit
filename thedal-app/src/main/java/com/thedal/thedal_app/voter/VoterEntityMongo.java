//package com.thedal.thedal_app.voter;
//
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//
//import org.springframework.data.annotation.Id;
//import org.springframework.data.annotation.Transient;
//import org.springframework.data.mongodb.core.index.Indexed;
//import org.springframework.data.mongodb.core.mapping.DBRef;
//import org.springframework.data.mongodb.core.mapping.Document;
//
//import com.fasterxml.jackson.annotation.JsonIgnore;
//import com.fasterxml.jackson.annotation.JsonProperty;
//import com.thedal.thedal_app.settings.electionsettings.Caste;
//import com.thedal.thedal_app.settings.electionsettings.ComplaintEntity;
//import com.thedal.thedal_app.settings.electionsettings.Religion;
//import com.thedal.thedal_app.settings.electionsettings.SubCaste;
//
//import jakarta.persistence.Column;
//import jakarta.persistence.JoinColumn;
//import jakarta.persistence.ManyToOne;
//import lombok.AllArgsConstructor;
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//import lombok.Setter;
//
//@Getter
//@Setter
//@NoArgsConstructor
//@AllArgsConstructor
//@Document(collection = "voters") // Specify the collection name in MongoDB
//public class VoterEntityMongo {
//
//    @Id
//    private String id; 
//    @Indexed(unique = true)
//    @JsonProperty("voter_id")              
//    private String voterId;
//
//    @JsonIgnore
//    @Column(name = "account_id")
//    private Long accountId;
//
//    @Indexed(unique = true)
//    @JsonProperty("epic_number")
//    private String epicNumber;
//
//    @JsonProperty("first_name")
//    private String firstName;
//
//    @JsonProperty("last_name")
//    private String lastName;
//
//    @JsonProperty("date_of_birth")
//    private LocalDate dateOfBirth;
//
//    private String gender;
//
//    @JsonProperty("address")
//    private Address address;
//
//    @JsonProperty("phone_number")
//    private String phoneNumber;
//
//    @JsonProperty("email")
//    private String email;
//
//    private Double latitude;
//    private Double longitude;
//
//    private String availability;
//
//    @JsonProperty("party_affiliation")
//    private String partyAffiliation;
//    
//
//    @JsonIgnore
//    @DBRef
//    private Religion religion;
//
//    @JsonIgnore
//    @DBRef
//    private Caste caste;
//    
//    @JsonIgnore
//    @DBRef
//    private ComplaintEntity complaint1;
//
//    @JsonProperty("third_party_id")
//    private String thirdPartyId;
//
//    @JsonProperty("photo_url")
//    private String photoUrl;
//
//    private String remarks;
//
//    private Long electionId;
//    private Integer boothNumber;
//
//    private Boolean hasVoted = false;
//    private LocalDateTime votedTimestamp;
//
//    private LocalDateTime createdTime;
//    private LocalDateTime modifiedTime;
//
//    // State Information
//    private String stateCode;
//    private String stateName;
//    private String stateNameL1;
//    private String stateNameL2;
//
//    // District Information
//    private String districtNo;
//    private String districtName;
//    private String districtNameL1;
//    private String districtNameL2;
//
//    // Parliament Information
//    private String parliamentNo;
//    private String parliamentName;
//    private String parliamentNameL1;
//    private String parliamentNameL2;
//
//    // Assembly Information
//    private String assemblyNo;
//    private String assemblyName;
//    private String assemblyNameL1;
//    private String assemblyNameL2;
//
//    // Local Body Information
//    private String localBody;
//
//    // Urban Details
//    private String urbanName;
//    private String urbanNameL1;
//    private String urbanNameL2;
//    private Integer urbanWardNo;
//
//    // Rural Details
//    private String districtUnionName;
//    private String districtUnionNameL1;
//    private String districtUnionNameL2;
//    private Integer districtUnionWardNo;
//
//    private String panchayatUnionName;
//    private String panchayatUnionNameL1;
//    private String panchayatUnionNameL2;
//    private Integer panchayatUnionWardNo;
//
//    private String villagePanchayatName;
//    private String villagePanchayatNameL1;
//    private String villagePanchayatNameL2;
//    private Integer villagePanchayatWardNo;
//
//    private String partNo;
//    private String partName;
//    private String partNameL1;
//    private String partLatLong;
//    private String pincode;
//
//    private Integer sectionNo;
//    private String sectionName;
//
//    private Integer serialNumber;
//    private String relationName;
//    private String relationNameL1;
//    private String relationNameL2;
//    private String relationType;
//    private Integer age;
//    private LocalDate anniversaryDate;
//    
//
//    @JsonIgnore
//    private String religionId;
//
//    @JsonIgnore
//    private String casteId;
//
//    //@JsonProperty("sub_caste")
//    @JsonIgnore
//    @DBRef
//    private SubCaste subCaste;
//
//   
//
//    public Boolean getHasVoted() {
//        return hasVoted != null ? hasVoted : false;
//    }
//
//    public void setHasVoted(Boolean hasVoted) {
//        this.hasVoted = hasVoted;
//    }
//
//
//}