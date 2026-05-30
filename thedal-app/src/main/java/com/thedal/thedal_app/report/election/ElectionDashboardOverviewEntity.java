package com.thedal.thedal_app.report.election;


import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.persistence.Table;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Entity
@Table(name = "election_dashboard_overview")
public class ElectionDashboardOverviewEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(nullable = false)
	private Long electionId;
	
	@Column(nullable = false)
	private Long accountId;
	
	private int totalBooth;
	
	private int totalVoters;
	
	private int noOfPincode;
	
	private int noOfMobileNumber;
	
	private int male;
	
	private int female;
	
	private int transgender;
	
//	private int firstTimeVoters;
//	
//	private int seniorCitizens;
//	
//	private int superCitizens;
	
	@Column(nullable = false)
	private Integer ageGroup18To21;
	
	@Column(nullable = false)
	private Integer ageGroup18To30;
	
	@Column(nullable = false)
	private Integer ageGroup30To40;
	
	@Column(nullable = false)
	private Integer ageGroup40To50;
	
	@Column(nullable = false)
	private Integer ageGroup50To60;
	
	@Column(nullable = false)
	private Integer ageGroup60To70;
	
	@Column(nullable = false)
	private Integer above70;
	
	@Column(nullable = false)
	private Integer ageGroup60To80;
	
	@Column(nullable = false)
	private Integer above80;
	
//	@Column(nullable = false)
//	private Integer totalVoters;
	
	@Column(nullable = false)
	private LocalDateTime createdAt;
	
	@Column(nullable = false)
	private LocalDateTime updatedAt;
	

	@PrePersist
	public void onSave() {
//		this.totalBooth = 0;
//		this.totalVoters = 0;
//		this.noOfPincode = 0;
//		this.male = 0;
//		this.female = 0;
//		this.transgender = 0;
//		this.firstTimeVoters = 0;
//		this.seniorCitizens = 0;
//		this.superCitizens = 0;
//		this.noOfMobileNumber = 0;
		createdAt = LocalDateTime.now();
		updatedAt = LocalDateTime.now();
	}
	
	  @PreUpdate
	  public void onUpdate() {
		  updatedAt= LocalDateTime.now();
	  }

}


