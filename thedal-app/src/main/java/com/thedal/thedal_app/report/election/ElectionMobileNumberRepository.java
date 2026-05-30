package com.thedal.thedal_app.report.election;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ElectionMobileNumberRepository extends JpaRepository<ElectionMobileNumberEntity, Long> {

	boolean existsByElectionIdAndMobileAndAccountId(Long electionId, String mobileNumber, Long accountId);

}
