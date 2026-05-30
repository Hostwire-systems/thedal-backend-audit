package com.thedal.thedal_app.report.pollday;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Entity
public class PollingAgePercentage {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(nullable = false)
	private Long electionId;
	
	//private Long wardNo;
	
	@Enumerated(EnumType.STRING)
	private VoteCountType voteCountType;
	
	@Column(nullable = false)
	private double ageGroup18To21;
	
	@Column(nullable = false)
	private double ageGroup22To25;
	
	@Column(nullable = false)
	private double ageGroup26To35;
	
	@Column(nullable = false)
	private double ageGroup36To45;
	
	@Column(nullable = false)
	private double ageGroup46To59;
	
	@Column(nullable = false)
	private double overallPolledPercentage;
	
	@Column(nullable = false)
	private LocalDateTime timestamp;
	
	  @PrePersist
	    public void onSave() {
		  ageGroup18To21=0.0;
		  ageGroup22To25=0.0;
		  ageGroup26To35=0.0;
		  ageGroup36To45=0.0;
		  ageGroup46To59=0.0;
		  overallPolledPercentage=0.0;
		  timestamp= LocalDateTime.now();
	    }
}

