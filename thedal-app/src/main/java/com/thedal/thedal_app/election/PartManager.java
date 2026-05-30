package com.thedal.thedal_app.election;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
	    name = "part_manager",
	    indexes = {
	        @Index(name = "idx_partmgr_election_account_part", columnList = "election_id, account_id, part_no"),
	        @Index(name = "idx_part_no_name", columnList = "part_no,part_name_english")
	    },
	    		uniqueConstraints = {
	    		        @UniqueConstraint(name = "uk_election_account_part_no", columnNames = {"election_id", "account_id", "part_no"})
	    		    }
	)
public class PartManager {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "part_no")
    private String partNo;
    private String partNameEnglish;
    private String partNameL1;
    
    @Column(name = "part_type", nullable = true, length = 20)
    private String partType;
    
    private String schoolName;
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Column(columnDefinition = "DOUBLE PRECISION DEFAULT 0.0")
    private Double partLat = 0.0;
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Column(columnDefinition = "DOUBLE PRECISION DEFAULT 0.0")
    private Double partLong = 0.0;
    
    private String pincode;
    @Column(name = "account_id", nullable = false)
    private Long accountId;
    @Column(name = "election_id", nullable = false)
    private Long electionId;
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Column(columnDefinition = "DOUBLE PRECISION DEFAULT 0.0")
    private Double schoolLat = 0.0;
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Column(columnDefinition = "DOUBLE PRECISION DEFAULT 0.0")
    private Double schoolLong = 0.0;
    
    @Column(nullable = true)
    private String boothVulnerability;
    private Integer orderIndex;
    
    @Column(nullable = true)
    private String partCaptainName;
    
    @Column(nullable = true)
    private String captainDesignation;
    
    @Column(nullable = true)
    private String captainMobileNo;
    
    @Column(name = "blo_name", nullable = true)
    private String bloName;
    
    @Column(name = "blo_designation", nullable = true)
    private String bloDesignation;
    
    @Column(name = "blo_mobile_number", nullable = true)
    private String bloMobileNumber;
    
    @Column(name = "bla2_name", nullable = true)
    private String bla2Name;
    
    @Column(name = "bla2_designation", nullable = true)
    private String bla2Designation;
    
    @Column(name = "bla2_mobile_number", nullable = true)
    private String bla2MobileNumber;
    
    @Column(name = "part_image_url", nullable = true, length = 500)
    private String partImageUrl;
    
    @Column(name = "booth_committee_members", columnDefinition = "TEXT")
    private String boothCommitteeMembers;
    
    // Custom setters to prevent NULL values for coordinates
    public void setPartLat(Double partLat) {
        this.partLat = (partLat != null) ? partLat : 0.0;
    }
    
    public void setPartLong(Double partLong) {
        this.partLong = (partLong != null) ? partLong : 0.0;
    }
    
    public void setSchoolLat(Double schoolLat) {
        this.schoolLat = (schoolLat != null) ? schoolLat : 0.0;
    }
    
    public void setSchoolLong(Double schoolLong) {
        this.schoolLong = (schoolLong != null) ? schoolLong : 0.0;
    }
    
    // JPA lifecycle callbacks to ensure no NULL coordinates are saved
    @PrePersist
    @PreUpdate
    private void validateCoordinates() {
        if (this.partLat == null) {
            this.partLat = 0.0;
        }
        if (this.partLong == null) {
            this.partLong = 0.0;
        }
        if (this.schoolLat == null) {
            this.schoolLat = 0.0;
        }
        if (this.schoolLong == null) {
            this.schoolLong = 0.0;
        }
    }

}
