package com.thedal.thedal_app.election.dtos;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ElectionResponseDTO {

//    private Long id;
//    private String electionName;
//    private String electionType;
//    private Date startDate;
//    private Date endDate;
//    private List<String> states;
//    private List<Integer> booths;
//    private String imageUrl;
    private Long id;
    
    @JsonProperty("electionName")
    private String electionName;
    
    private String electionType;
    
    private Date startDate;
    
    private Date endDate;
    
    private String imageUrl;
    
    private String category;
    
    private String stateName;
    
    private String year;
    
    private String month;
    
    private String status;
    
    private Boolean isFrozen;
    
    private Long numberOfPollingStations;
    
    private Long numberOfPhases;
    
    private Long numberOfPinkBooths;
    
    private Long numberOfVoters;
    
    private Long numberOfMaleVoters;
    
    private Long numberOfFemaleVoters;
    
    private Long numberOfTransgenderVoters;
    
    private String remarks;
    
    private Date createdAt;
    
    private Date modifiedAt;

    //private List<String> states;
    
    //private List<Integer> booths;
    private Integer boothCount;
    
    //private Integer boothNumber;
    // Poll Event Dates
    private Date notificationDate;
    private Date lastDateForFillingNomination;
    private Date dateOfPoll;
    private Date scrutinyNominationDate;
    private Date lastDateForWithdrawalOfNomination;
    private Date dateOfCountingOfVotes;
    
    private String electionDescription;
    
 // new fields
    @NotNull(message = "Election body is required")
    @Enumerated(EnumType.STRING)
    private ElectionBody body;

    @NotNull(message = "Election category is required")
    @Enumerated(EnumType.STRING)
    private ElectionCategory electionCategory;

    @NotNull(message = "Election type is required")
    @Enumerated(EnumType.STRING)
    private ElectionType type;

    private String pcName;  // Parliamentary Constituency Name
    private String acName;  // Assembly Constituency Name
    private String urbanName; 
    private String ruralName;  
    private Integer phaseNo;
    
    private String country; 
    private String state;
    private Date gazetteNotificationDate;
    private Date completionDeadlineDate; 
    private String bodyString;    
    private String templates; 
    
    private Integer orderIndex;
    private Date electoralReleaseDate;
    
}
