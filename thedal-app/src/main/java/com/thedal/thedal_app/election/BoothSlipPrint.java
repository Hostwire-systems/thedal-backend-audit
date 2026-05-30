package com.thedal.thedal_app.election;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Entity
//@Table(name = "booth_slip_print")
@Table(
	    name = "booth_slip_print",
	    indexes = {
	        @Index(name = "idx_account_election_voter", columnList = "account_id, election_id, voter_id")
	    }
	)
@Data
@Getter
@Setter
public class BoothSlipPrint {

	@Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "election_id")
    private Long electionId;

//    @Column(name = "volunteer_id", nullable = false)
//    private Long volunteerId;

    @Column(name = "voter_id")
    private String voterId;

    @Column(name = "printed_time")
    private LocalDateTime printedTime;

    @Column(name = "template_id")
    private Long templateId;
	
}
