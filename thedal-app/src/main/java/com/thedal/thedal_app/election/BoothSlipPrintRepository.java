package com.thedal.thedal_app.election;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BoothSlipPrintRepository extends JpaRepository<BoothSlipPrint, Long> {

	 List<BoothSlipPrint> findByElectionIdAndVoterIdAndAccountId(Long electionId, String voterId, Long accountId);
}