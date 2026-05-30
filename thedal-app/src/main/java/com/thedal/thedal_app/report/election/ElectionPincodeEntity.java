package com.thedal.thedal_app.report.election;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Entity
@Table(name = "election_pincode",uniqueConstraints = @UniqueConstraint(columnNames = {"accountId","electionId","pincode"}))
public class ElectionPincodeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(nullable = false)
	private Long accountId;
	
	@Column(nullable = false)
	private Long electionId;
	
	@Column(nullable = false)
	private String pincode;

}

