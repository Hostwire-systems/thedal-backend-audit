package com.thedal.thedal_app.volunteer;

import com.thedal.thedal_app.election.ElectionBooth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "volunteer_election_booth")
@Getter
@Setter
public class VolunteerElectionBooth {
	
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //@Column(name = "volunteer_id", nullable = false)
	@Column(name = "volunteer_id")
    private Long volunteerId;

//    @Column(name = "election_booth_id", nullable = false)
//    private Long electionBoothId;
    private Integer boothNumber;

    //@Column(name = "election_id", nullable = false)
    @Column(name = "election_id")
    private Long electionId;

    //@Column(name = "account_id", nullable = false)
    @Column(name = "account_id")
    private Long accountId;

}
