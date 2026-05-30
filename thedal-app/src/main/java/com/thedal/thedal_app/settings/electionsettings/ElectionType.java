package com.thedal.thedal_app.settings.electionsettings;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "election_type")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ElectionType {
	
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //	@Column(nullable = false)
    private String electionType;
    @JsonIgnore
    private Long accountId;

}
