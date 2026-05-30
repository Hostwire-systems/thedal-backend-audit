package com.thedal.thedal_app.election;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VoterFieldOrderRepository extends JpaRepository<VoterFieldOrderEntity, Long> {
	
    Optional<VoterFieldOrderEntity> findByElectionIdAndAccountId(Long electionId, Long accountId);
    boolean existsByElectionIdAndAccountId(Long electionId, Long accountId);

}