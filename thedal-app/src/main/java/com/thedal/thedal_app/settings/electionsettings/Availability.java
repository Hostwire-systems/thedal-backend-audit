package com.thedal.thedal_app.settings.electionsettings;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.thedal.thedal_app.voter.VoterEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "availability", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"description", "election_id"}))
public class Availability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String categoryName;

    @Column(nullable = false)
    private String description;

     @JsonIgnore
	private Long accountId;

	@JsonIgnore
	@Column(name = "election_id",nullable = false)
    private Long electionId;

    private String availabilityImage;
    private String availabilityName;
    private Integer orderIndex;
    @CreatedDate
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "availability1", fetch = jakarta.persistence.FetchType.LAZY)
    @JsonIgnore
    private Set<VoterEntity> voters = new HashSet<>();

}
