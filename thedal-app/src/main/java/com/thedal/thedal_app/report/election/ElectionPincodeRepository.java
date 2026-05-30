package com.thedal.thedal_app.report.election;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ElectionPincodeRepository extends JpaRepository<ElectionPincodeEntity, Long> {

	boolean existsByElectionIdAndPincodeAndAccountId(Long electionId, String pincode, Long accountId);

}
