package com.thedal.thedal_app.report.election;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "voter_party_report")
public class VoterPartyReportEntity {
	
		@Id
	    @GeneratedValue(strategy = GenerationType.IDENTITY)
	    private Long id;

		@Column(nullable = false)
	    private Long electionId;

		@Column(nullable = false)
	    private Long accountId;

		@Column(nullable = false)
	    private Long partyId;

		@Column(nullable = false)
	    private Integer votersCount;
}

