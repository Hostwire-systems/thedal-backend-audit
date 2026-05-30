package com.thedal.thedal_app.election;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.thedal.thedal_app.election.dtos.ElectionBody;
import com.thedal.thedal_app.election.dtos.ElectionCategory;
import com.thedal.thedal_app.election.dtos.ElectionType;
import com.thedal.thedal_app.settings.electionsettings.ComplaintEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(name="election")
public class ElectionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    //@JsonProperty("election_id")
    private Long id;

    @Column(nullable = false)
    private Long accountId;

    @Column(nullable = true)
    @JsonProperty("electionName")
    private String electionName;

    @Column(nullable = false)
    private Boolean isDeleted = false; 

    @Column(name = "is_frozen", nullable = false)
    private Boolean isFrozen = false;

    @Column(nullable = true)
    private String electionType;
//    @ManyToOne
//    @JoinColumn(name = "election_type_id", nullable = true)
//    private ElectionType electionTypeEntity;

    @Column(nullable = true)
    @Temporal(TemporalType.DATE)
    private Date startDate;

    @Column(nullable = true)
    @Temporal(TemporalType.DATE)
    private Date endDate;

    @Column(nullable = true)
    private String imageUrl;
    
    @Column(nullable = true)
    private String category;
    
    @Column(nullable = true)
    private String stateName; 
    
    @Column(nullable = true)
    private String year;
    @Column(nullable = true)
    private String month;
    
    @Column(nullable = true)
    private String status; 
    
    @Column(nullable = true)
    private Long numberOfPollingStations;
    
    @Column(nullable = true)
    private Long numberOfPhases;
    
    @Column(nullable = true)
    private Long numberOfPinkBooths; 

    @Column(nullable = true)
    private Long numberOfVoters;

    @Column(nullable = true)
    private Long numberOfMaleVoters; 

    @Column(nullable = true)
    private Long numberOfFemaleVoters; 
    
    @Column(nullable = true)
    private Long numberOfTransgenderVoters; 
    
    @Column(nullable = true)
    private Integer boothCount;
    
    @Column(nullable = true)
    private String remarks;
    
    @Column(nullable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;
    
    @Column(nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date modifiedAt;
       
    // Poll Event Dates
    @Column(nullable = true)
    @Temporal(TemporalType.DATE)
    private Date notificationDate;

    @Column(nullable = true)
    @Temporal(TemporalType.DATE)
    private Date lastDateForFillingNomination;

    @Column(nullable = true)
    @Temporal(TemporalType.DATE)
    private Date dateOfPoll;

    @Column(nullable = true)
    @Temporal(TemporalType.DATE)
    private Date scrutinyNominationDate;

    @Column(nullable = true)
    @Temporal(TemporalType.DATE)
    private Date lastDateForWithdrawalOfNomination;

    @Column(nullable = true)
    @Temporal(TemporalType.DATE)
    private Date dateOfCountingOfVotes;
    

//    @OneToMany(mappedBy = "election", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
//    private List<ElectionState> states;
//
//    @OneToMany(mappedBy = "election", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
//    private List<ElectionBooth> booths;

//    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
//    private ElectionBooth booth;
    
    private String electionDescription;
    
    @Column(name = "election_category", nullable = false)
    @Enumerated(EnumType.STRING)
    private ElectionCategory electionCategory;

    
    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    private ElectionType type;
    
    @Column(nullable = true)
    private String pcName; 

    @Column(nullable = true)
    private String acName; 

    @Column(nullable = true)
    private String urbanName; 

    @Column(nullable = true)
    private String ruralName; 

    @Column(nullable = true)
    private Integer phaseNo; 

//    @OneToOne(mappedBy = "election", cascade = CascadeType.ALL, orphanRemoval = true)
    @OneToOne
    @JoinColumn(name = "complaint_id")
    private ComplaintEntity complaint;
    
    private String country;
    private String state; 
    
    @Column(nullable = true)
    @Temporal(TemporalType.DATE)
    private Date gazetteNotificationDate;

    @Column(nullable = true)
    @Temporal(TemporalType.DATE)
    private Date completionDeadlineDate;
    
    @Column(nullable = true)
    @Temporal(TemporalType.DATE)
    private Date electoralReleaseDate;
    
//    @Column(name = "body", nullable = false)
    @Column(name = "body", nullable = true)
    @Enumerated(EnumType.STRING) 
    private ElectionBody body;
    
    @Column(name = "body_string")
    private String bodyString; 
    
    @Column(name = "templates", columnDefinition = "TEXT")
    //@Column(name = "templates")
    private String templates;
    
    @OneToMany
    @JoinColumn(name = "election_id")
    private List<TemplateEntity> template;
    
//    @Column(name = "order_index")
//    private Integer orderIndex;
    @Column(nullable = false)
    private Integer orderIndex;

    
 // Custom setter to manage ElectionBody based on ElectionCategory
    public void setElectionBody(Object body) {
        if (electionCategory == ElectionCategory.POLITICAL) {
            // Set the body as enum if the category is POLITICAL
            if (body instanceof ElectionBody) {
                this.body = (ElectionBody) body;
                this.bodyString = null;  // Clear the string field
            } else {
                throw new IllegalArgumentException("For POLITICAL category, body should be an ElectionBody enum.");
            }
        } else if (electionCategory == ElectionCategory.NON_POLITICAL) {
            // Set the body as String if the category is NON_POLITICAL
            if (body instanceof String) {
                this.body = null;  // Set enum to null for non-political category
                this.bodyString = (String) body;  // Store body as a String
            } else {
                throw new IllegalArgumentException("For NON_POLITICAL category, body should be a String.");
            }
        }
    }

    // Getter for body
    public Object getElectionBody() {
        return electionCategory == ElectionCategory.POLITICAL ? body : bodyString;
    }
    
    @PrePersist
    protected void onCreate() {
        Date now = new Date();
        this.createdAt = now;
        this.modifiedAt = now;
        
       
    }
    @PreUpdate
    protected void onUpdate() {
        this.modifiedAt = new Date();
    }
    
 // Constructor to initialize with an id
    public ElectionEntity(Long id) {
        this.id = id;
    }
    
    @Column(nullable = true, columnDefinition = "TEXT")
    private String whatsappFooter;
    
    @Column(name = "default_party_id", nullable = true)
    private Long defaultPartyId;

    
}
