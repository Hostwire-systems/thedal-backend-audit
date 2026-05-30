package com.thedal.thedal_app.report.election;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VoterPartyReportRepository extends JpaRepository<VoterPartyReportEntity, Long> {

	Optional<VoterPartyReportEntity> findByElectionIdAndAccountIdAndPartyId(
			Long electionId, Long accountId, Long partyId);

	List<VoterPartyReportEntity> findByElectionIdAndAccountId(Long electionId, Long accountId);

}
