package com.thedal.thedal_app.election.dtos;


import java.util.Date;
import java.util.List;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class ElectionDTO {
	
	@NotNull(message = "Election name is required")
    private String electionName;
    
    @NotNull(message = "Election type is required")
    private String electionType; 

    @NotNull(message = "Start date is required")
    // @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private Date startDate;
    
    @NotNull(message = "End date is required")
    // @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private Date endDate;

    //private List<String> states;
    //private List<Integer> booths;
    //private String imageUrl;
	
	//private Long accountId;
//    private String electionName;
//    private String electionType;
//    private Date startDate;
//    private Date endDate;
//    private String imageUrl;
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
    private Integer boothCount;    
 // Poll Event Dates
    private Date notificationDate;
    private Date lastDateForFillingNomination;
    private Date dateOfPoll;
    private Date scrutinyNominationDate;
    private Date lastDateForWithdrawalOfNomination;
    private Date dateOfCountingOfVotes;   
    private String electionDescription;    
 // new fields
    //@NotNull(message = "Election body is required")
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
    private String complaint;  
    private String country;
    private String state;
    //@NotNull(message = "Gazette notification date is required")
    private Date gazetteNotificationDate;
    //@NotNull(message = "Completion deadline date is required")
    private Date completionDeadlineDate;
    private String bodyString; 
    private Date electoralReleaseDate;
    
//    @NotNull(message = "Template ID is required")
//    private Long templateId;

    
}
