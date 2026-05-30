package com.thedal.thedal_app.volunteer;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mongodb.lang.Nullable;
import com.thedal.thedal_app.election.ElectionEntity;
import com.thedal.thedal_app.role.Role;
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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "volunteers", indexes = {
        @Index(name = "idx_volunteer_id", columnList = "volunteer_id", unique = true),
        @Index(name = "idx_election_account", columnList = "election_id, account_id"),
        @Index(name = "idx_mobile_number", columnList = "mobile_number")
    })
@Getter
@Setter
@RequiredArgsConstructor
public class VolunteerEntity {
	
//	@Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    @Column(name = "id", nullable = false, updatable = false)
//    private Long id;
//	
//	@NotBlank
//    @Column(name = "volunteer_id", nullable = false, unique = true)
//    @JsonProperty("volunteer_id")
//    private String volunteerId;
	
	@Id
    //@JsonProperty("volunteer_id")
	 @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

//    @Column(name = "first_name")
//    @JsonProperty("first_name")
//    private String firstName;
//
        @Column(name = "last_name",nullable=true)
        @JsonProperty("last_name")
        private String lastName;

//    @Column(name = "email",nullable = true)
//    @JsonProperty("email")
//    @Nullable
//    private String email;
        @Column(name = "email", nullable = true)
        @JsonProperty("email")
//        @Pattern(regexp = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", 
//                 message = "Invalid email format")
        private String email;

//    @NotBlank(message = "40319")
//    @Pattern(
//        regexp = "^[0-9]{10}$",
//        message = "40352"
//    )
    //@Pattern(regexp = "^[6-9]\\d{9}$", message = "Mobile number must be a valid 10-digit number starting with 6-9")
    @Column(name = "mobile_number")
    @JsonProperty("mobile_number")
    private String mobileNumber;

    @Embedded
    @JsonProperty("address")
    private Address volunteerAddress;

//    @Column(name = "assigned_booth")
//    @JsonProperty("assigned_booth")
//    private String assignedBooth;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "volunteer_entity_assigned_booth",
        joinColumns = @JoinColumn(name = "volunteer_entity_id")
    )
    @Column(name = "assigned_booth")
    @JsonProperty("assigned_booth")
    private List<Long> assignedBooth;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "volunteer_entity_assigned_families",
        joinColumns = @JoinColumn(name = "volunteer_entity_id")
    )
    @Column(name = "assigned_families")
    @JsonProperty("assigned_families")
    private List<Long> assignedFamilies;

    @Column(name = "status")
    @JsonProperty("status")
    private String status;

    @Column(name = "photo_url")
    @JsonProperty("photo_url")
    private String photoUrl;

    @Column(name = "remarks")
    @JsonProperty("remarks")
    private String remarks;

//    @OneToMany(mappedBy = "volunteer", cascade = CascadeType.ALL, orphanRemoval = true)
//    private List<ActivityEntity> activities = new ArrayList<>(); 
//
//    private double currentLatitude;
//    private double currentLongitude;
//    private String currentRoute;
//    
//    @Column(name = "hours_worked_today")
//    private int hoursWorkedToday;
//    @Column(name = "hours_worked_week")
//    private int hoursWorkedWeek;
    
    @JsonIgnore
    @Column(name = "account_id")
    private Long accountId;
//    @JsonIgnore
//    @Column(name = "election_id")
//    private Long electionId;
    
    @Column(name = "created_time", updatable = false)
    private LocalDateTime createdTime;

    @Column(name = "modified_time")
    private LocalDateTime modifiedTime;

    @ManyToOne
    //@JoinColumn(nullable = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    //@JoinColumn(name = "user_entity_user_id", referencedColumnName = "id", insertable = false, updatable = false)
    private UserEntity userEntity;
    
    @ManyToOne
    @JoinColumn(name = "election_id", nullable = false)  
//    @JoinColumn(name = "election_id", insertable = false, updatable = false)
    private ElectionEntity electionEntity;

    @Column(name = "whats_app_number")
    @JsonProperty("whats_app_number")
    private String whatsAppNumber;
    
    private String gender;
    
    @Column(name = "role_id")
    private Long roleId;
    
    // JPA relationship to Role entity for proper role-based filtering
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", insertable = false, updatable = false)
    private Role roleEntity;
    
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