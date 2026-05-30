package com.thedal.thedal_app.report;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PollingAgeWiseRepository extends JpaRepository<PollingAgeWiseEntity, Long> {

    List<PollingAgeWiseEntity> findByElectionIdAndAccountId(Long electionId, Long accountId);

    PollingAgeWiseEntity findByElectionIdAndVoteCountType(Long electionId, Integer voteCountType);
}