package com.thedal.thedal_app.settings.electionsettings;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.thedal.thedal_app.election.ElectionEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "complaints")
@Getter
@Setter
public class ComplaintEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "complaint_name")
    private String complaintName;

    @JsonIgnore
    @Column(name = "account_id")
    private Long accountId;
    
    @OneToOne
    @JoinColumn(name = "election_id")
    private ElectionEntity election;

}