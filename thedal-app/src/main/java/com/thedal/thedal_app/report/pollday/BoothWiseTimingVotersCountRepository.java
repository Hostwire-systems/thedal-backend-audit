package com.thedal.thedal_app.report.pollday;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface BoothWiseTimingVotersCountRepository extends JpaRepository<BoothWiseTimingVotersCount, Long>{

	Optional<BoothWiseTimingVotersCount> findByElectionIdAndBoothNumberAndAccountId(Long electionId, Long boothNumber, Long accountId);
//    void deleteByElectionIdAndAccountId(Long electionId, Long accountId);
//	void deleteByElectionIdAndBoothNumberAndAccountId(Long electionId, Long boothNumber, Long accountId);
	
	  @Modifying
	    @Query("DELETE FROM BoothWiseTimingVotersCount b WHERE b.electionId = :electionId AND b.accountId = :accountId")
	    void deleteByElectionIdAndAccountId(Long electionId, Long accountId);

	    @Modifying
	    @Query("DELETE FROM BoothWiseTimingVotersCount b WHERE b.electionId = :electionId AND b.boothNumber = :boothNumber AND b.accountId = :accountId")
	    void deleteByElectionIdAndBoothNumberAndAccountId(Long electionId, Long boothNumber, Long accountId);
	
}
