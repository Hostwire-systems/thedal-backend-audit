package com.thedal.thedal_app.report.election;


import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Entity
@Table(name = "voters_by_booth_having_contacts")
public class VotersByBoothHavingContactsEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(nullable = false)
	private Long electionId;
	
	@Column(nullable = false)
	private Long accountId;
	
	@Column(nullable = false)
	private Integer boothNumber;
	
	@Column(nullable = false)
	private String mobileNumber;
	
	@Column(nullable = false)
	private Integer voterCount;
	
	@Column(nullable = false)
	private LocalDateTime createdAt;
	
	@Column(nullable = false)
	private LocalDateTime updatedAt;
	
	  @PrePersist
	    public void onSave() {
		  createdAt= LocalDateTime.now();
		  updatedAt= LocalDateTime.now();
	  }
	  
	  @PreUpdate
	  public void onUpdate() {
		  updatedAt= LocalDateTime.now();
	  }

	public VotersByBoothHavingContactsEntity(Long electionId, Integer boothNumber, String mobileNumber,Integer voterCount) {
		this.electionId = electionId;
		this.boothNumber = boothNumber;
		this.voterCount = voterCount;
		this.mobileNumber = mobileNumber;
	}
	  
	  
}

