package com.thedal.thedal_app.election;

import java.time.LocalDateTime;
import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.thedal.thedal_app.election.dtos.ElectionBody;
import com.thedal.thedal_app.election.dtos.ElectionCategory;
import com.thedal.thedal_app.election.dtos.ElectionType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "elections")
@CompoundIndexes({
    @CompoundIndex(name = "account_election_order_idx", def = "{'accountId': 1, 'orderIndex': 1}"),
    @CompoundIndex(name = "account_election_name_idx", def = "{'accountId': 1, 'electionName': 1}"),
    @CompoundIndex(name = "account_deleted_idx", def = "{'accountId': 1, 'isDeleted': 1}")
})
public class ElectionMongo {

    @Id
    private Long id;

    @Indexed
    private Long accountId;

    @Indexed
    private String electionName;

    @Indexed
    private Boolean isDeleted = false;

    private Boolean isFrozen = false;

    private String electionType;
    private Date startDate;
    private Date endDate;
    private String imageUrl;
    private String category;
    private String stateName;
    private String year;
    private String month;
    private String status;
    private Long numberOfPollingStations;
    private Long numberOfPhases;
    private Long numberOfPinkBooths;
    private Long numberOfVoters;
    private Long numberOfMaleVoters;
    private Long numberOfFemaleVoters;
    private Long numberOfTransgenderVoters;
    private Integer boothCount;
    private String remarks;
    
    // Poll Event Dates
    private Date notificationDate;
    private Date lastDateForFillingNomination;
    private Date dateOfPoll;
    private Date scrutinyNominationDate;
    private Date lastDateForWithdrawalOfNomination;
    private Date dateOfCountingOfVotes;
    
    private String electionDescription;
    private ElectionCategory electionCategory;
    private ElectionType type;
    private String pcName;
    private String acName;
    private String urbanName;
    private String ruralName;
    private Integer phaseNo;
    private String country;
    private String state;
    private Date gazetteNotificationDate;
    private Date completionDeadlineDate;
    private Date electoralReleaseDate;
    private ElectionBody body;
    private String bodyString;
    private String templates;
    
    @Indexed
    private Integer orderIndex;
    
    private String whatsappFooter;
    
    private Long defaultPartyId;
    
    // MongoDB specific fields
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructor from ElectionEntity
    public ElectionMongo(ElectionEntity election) {
        this.id = election.getId();
        this.accountId = election.getAccountId();
        this.electionName = election.getElectionName();
        this.isDeleted = election.getIsDeleted();
        this.isFrozen = election.getIsFrozen();
        this.electionType = election.getElectionType();
        this.startDate = election.getStartDate();
        this.endDate = election.getEndDate();
        this.imageUrl = election.getImageUrl();
        this.category = election.getCategory();
        this.stateName = election.getStateName();
        this.year = election.getYear();
        this.month = election.getMonth();
        this.status = election.getStatus();
        this.numberOfPollingStations = election.getNumberOfPollingStations();
        this.numberOfPhases = election.getNumberOfPhases();
        this.numberOfPinkBooths = election.getNumberOfPinkBooths();
        this.numberOfVoters = election.getNumberOfVoters();
        this.numberOfMaleVoters = election.getNumberOfMaleVoters();
        this.numberOfFemaleVoters = election.getNumberOfFemaleVoters();
        this.numberOfTransgenderVoters = election.getNumberOfTransgenderVoters();
        this.boothCount = election.getBoothCount();
        this.remarks = election.getRemarks();
        this.notificationDate = election.getNotificationDate();
        this.lastDateForFillingNomination = election.getLastDateForFillingNomination();
        this.dateOfPoll = election.getDateOfPoll();
        this.scrutinyNominationDate = election.getScrutinyNominationDate();
        this.lastDateForWithdrawalOfNomination = election.getLastDateForWithdrawalOfNomination();
        this.dateOfCountingOfVotes = election.getDateOfCountingOfVotes();
        this.electionDescription = election.getElectionDescription();
        this.electionCategory = election.getElectionCategory();
        this.type = election.getType();
        this.pcName = election.getPcName();
        this.acName = election.getAcName();
        this.urbanName = election.getUrbanName();
        this.ruralName = election.getRuralName();
        this.phaseNo = election.getPhaseNo();
        this.country = election.getCountry();
        this.state = election.getState();
        this.gazetteNotificationDate = election.getGazetteNotificationDate();
        this.completionDeadlineDate = election.getCompletionDeadlineDate();
        this.electoralReleaseDate = election.getElectoralReleaseDate();
        this.body = election.getBody();
        this.bodyString = election.getBodyString();
        this.templates = election.getTemplates();
        this.orderIndex = election.getOrderIndex();
        this.whatsappFooter = election.getWhatsappFooter();
        this.defaultPartyId = election.getDefaultPartyId();
        
        // Set timestamps
        this.createdAt = election.getCreatedAt() != null ? 
            election.getCreatedAt().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() : 
            LocalDateTime.now();
        this.updatedAt = election.getModifiedAt() != null ? 
            election.getModifiedAt().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() : 
            LocalDateTime.now();
    }
}
