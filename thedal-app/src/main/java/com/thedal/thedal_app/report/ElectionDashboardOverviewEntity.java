//package com.thedal.thedal_app.report;
//
//import java.time.LocalDateTime;
//
//import jakarta.persistence.Column;
//import jakarta.persistence.Entity;
//import jakarta.persistence.GeneratedValue;
//import jakarta.persistence.GenerationType;
//import jakarta.persistence.Id;
//import jakarta.persistence.PrePersist;
//import lombok.AllArgsConstructor;
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//import lombok.Setter;
//import jakarta.persistence.Table;
//
//@NoArgsConstructor
//@AllArgsConstructor
//@Setter
//@Getter
//@Entity
//@Table(name = "election_dashboard_overview")
//public class ElectionDashboardOverviewEntity {
//
//	@Id
//	@GeneratedValue(strategy = GenerationType.IDENTITY)
//	private Long id;
//	
//	@Column(nullable = false)
//	private Long electionId;
//	
//	private int totalBooth;
//	
//	private int totalVoters;
//	
//	private int noOfPincode;
//	
//	private int male;
//	
//	private int female;
//	
//	private int transgender;
//	
//	private int firstTimeVoters;
//	
//	private int seniorCitizens;
//	
//	private int superCitizens;
//
//	@PrePersist
//	public void onSave() {
//		this.totalBooth = 0;
//		this.totalVoters = 0;
//		this.noOfPincode = 0;
//		this.male = 0;
//		this.female = 0;
//		this.transgender = 0;
//		this.firstTimeVoters = 0;
//		this.seniorCitizens = 0;
//		this.superCitizens = 0;
//	}
//
//}
