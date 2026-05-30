//package com.thedal.thedal_app.report;
//
//import jakarta.persistence.Column;
//import jakarta.persistence.Entity;
//import jakarta.persistence.GeneratedValue;
//import jakarta.persistence.GenerationType;
//import jakarta.persistence.Id;
//import jakarta.persistence.Table;
//import lombok.AllArgsConstructor;
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//import lombok.Setter;
//
//@NoArgsConstructor
//@AllArgsConstructor
//@Setter
//@Getter
//@Entity
//@Table(name = "election_report")
//public class ElectionReportEntity {
//
//	@Id
//	@GeneratedValue(strategy = GenerationType.IDENTITY)
//	private Long id;
//	
//	@Column(nullable = false)
//	private Long electionId;
//	
//	@Column(nullable = false)
//	private Long accountId;
//	
//	private String gender;
//	
//	private String city;
//	
//	private String caste;
//	
//	private String religion;
//	
//	private String party;
//	
//	private int count;
//	
//}
//
