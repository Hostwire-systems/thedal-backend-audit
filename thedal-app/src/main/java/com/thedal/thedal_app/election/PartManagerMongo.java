package com.thedal.thedal_app.election;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "part_managers")
@CompoundIndexes({
    @CompoundIndex(name = "idx_partmgr_election_account_part", def = "{'electionId': 1, 'accountId': 1, 'partNo': 1}"),
    @CompoundIndex(name = "idx_part_no_name", def = "{'partNo': 1, 'partNameEnglish': 1}")
})
public class PartManagerMongo {

    @Id
    private Long id;
    
    @Indexed
    private String partNo;
    
    private String partNameEnglish;
    private String partNameL1;
    private String schoolName;
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Double partLat;
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Double partLong;
    
    private String pincode;
    
    @Indexed
    private Long accountId;
    
    @Indexed
    private Long electionId;
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Double schoolLat;
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Double schoolLong;
    private String boothVulnerability;
    private Integer orderIndex;
    
    private String partCaptainName;
    private String captainDesignation;
    private String captainMobileNo;

    // Constructor from JPA entity
    public PartManagerMongo(PartManager partManager) {
        this.id = partManager.getId();
        this.partNo = partManager.getPartNo();
        this.partNameEnglish = partManager.getPartNameEnglish();
        this.partNameL1 = partManager.getPartNameL1();
        this.schoolName = partManager.getSchoolName();
        this.partLat = partManager.getPartLat();
        this.partLong = partManager.getPartLong();
        this.pincode = partManager.getPincode();
        this.accountId = partManager.getAccountId();
        this.electionId = partManager.getElectionId();
        this.schoolLat = partManager.getSchoolLat();
        this.schoolLong = partManager.getSchoolLong();
        this.boothVulnerability = partManager.getBoothVulnerability();
        this.orderIndex = partManager.getOrderIndex();
        this.partCaptainName = partManager.getPartCaptainName();
        this.captainDesignation = partManager.getCaptainDesignation();
        this.captainMobileNo = partManager.getCaptainMobileNo();
    }
}
