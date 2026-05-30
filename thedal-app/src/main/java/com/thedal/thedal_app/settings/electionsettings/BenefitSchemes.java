package com.thedal.thedal_app.settings.electionsettings;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.thedal.thedal_app.voter.VoterEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter 
@Setter
public class BenefitSchemes {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String schemeName;
    
    @Column(nullable = true)
    private Double schemeValue;
    
    private String imageUrl;

    @Column(name = "scheme_by", nullable = false)
    @Enumerated(EnumType.STRING) 
    private SchemeBy schemeBy;

    @JsonIgnore
	private Long accountId;

	@JsonIgnore
	@Column(name = "election_id")
    private Long electionId;
	
	private Integer orderIndex;
    @JsonIgnore
    @CreationTimestamp
@Column(updatable = false)
private LocalDateTime createdAt;
@JsonIgnore
@UpdateTimestamp
private LocalDateTime updatedAt;

//@ManyToMany(mappedBy = "benefitSchemes", fetch = FetchType.LAZY)
//@JsonIgnore
//private List<VoterEntity> voters = new ArrayList<>();

@Column(name = "user_selection")
private Boolean userSelection;

}
