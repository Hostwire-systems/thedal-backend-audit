package com.thedal.thedal_app.familycaptain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.thedal.thedal_app.election.ElectionEntity;
import com.thedal.thedal_app.user.UserEntity;
import com.thedal.thedal_app.voter.Address;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "family_captains", indexes = {
        @Index(name = "idx_family_captain_id", columnList = "id"),
        @Index(name = "idx_fc_election_account", columnList = "election_id, account_id"),
        @Index(name = "idx_fc_mobile_number", columnList = "mobile_number"),
        @Index(name = "idx_fc_user_election", columnList = "user_id, election_id")
    })
@Getter
@Setter
@RequiredArgsConstructor
public class FamilyCaptainEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name")
    @JsonProperty("first_name")
    private String firstName;

    @Column(name = "last_name", nullable = true)
    @JsonProperty("last_name")
    private String lastName;

    @Column(name = "email", nullable = true)
    @JsonProperty("email")
    private String email;

    @Column(name = "mobile_number")
    @JsonProperty("mobile_number")
    private String mobileNumber;

    @Embedded
    @JsonProperty("address")
    private Address familyCaptainAddress;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "family_captain_assigned_families",
        joinColumns = @JoinColumn(name = "family_captain_id")
    )
    @Column(name = "family_id")
    @JsonProperty("assigned_families")
    private List<UUID> assignedFamilies;

    @Column(name = "status")
    @JsonProperty("status")
    private String status;

    @Column(name = "photo_url")
    @JsonProperty("photo_url")
    private String photoUrl;

    @Column(name = "remarks")
    @JsonProperty("remarks")
    private String remarks;

    @JsonIgnore
    @Column(name = "account_id")
    private Long accountId;
    
    @Column(name = "created_time", updatable = false)
    private LocalDateTime createdTime;

    @Column(name = "modified_time")
    private LocalDateTime modifiedTime;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private UserEntity userEntity;
    
    @ManyToOne
    @JoinColumn(name = "election_id", nullable = false)  
    private ElectionEntity electionEntity;

    @Column(name = "whats_app_number")
    @JsonProperty("whats_app_number")
    private String whatsAppNumber;
    
    @Column(name = "gender")
    private String gender;
    
    @Column(name = "admin_user_id")
    private Long adminUserId;
    
    @PrePersist
    protected void onCreate() {
        createdTime = LocalDateTime.now();
        modifiedTime = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        modifiedTime = LocalDateTime.now();
    }
    
    public void setStatus(String status) {
        if (status != null) {
            this.status = status.trim().toLowerCase(); 
        } else {
            this.status = null;
        }
    }
}
