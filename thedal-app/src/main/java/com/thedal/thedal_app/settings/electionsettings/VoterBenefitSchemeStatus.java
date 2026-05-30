package com.thedal.thedal_app.settings.electionsettings;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.thedal.thedal_app.voter.VoterEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "voter_benefit_scheme_status")
@Getter @Setter
public class VoterBenefitSchemeStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "voter_id")
    @JsonIgnore
    private VoterEntity voter;
    //@JsonIgnore
    @ManyToOne
    @JoinColumn(name = "benefit_scheme_id")
    private BenefitSchemes benefitScheme;
    
    private boolean selected;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}