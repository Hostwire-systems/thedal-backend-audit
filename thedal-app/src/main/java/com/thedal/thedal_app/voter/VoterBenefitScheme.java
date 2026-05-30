package com.thedal.thedal_app.voter;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.thedal.thedal_app.settings.electionsettings.BenefitSchemes;

import jakarta.persistence.Column;
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
@Table(name = "voter_benefit_schemes")
@Getter
@Setter
public class VoterBenefitScheme {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    private Long id;

    @ManyToOne
    @JoinColumn(name = "voter_id")
    @JsonIgnore
    private VoterEntity voter;

    @ManyToOne
    @JoinColumn(name = "benefit_scheme_id")
    private BenefitSchemes benefitScheme;

    @Column(name = "selected")
    private Boolean selected = true;
}