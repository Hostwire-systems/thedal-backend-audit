package com.thedal.thedal_app.election;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ElectionStateRepository extends JpaRepository<ElectionState, Long>{

    List<ElectionState> findByElection(ElectionEntity election);

	Optional<ElectionEntity> findByElectionIdAndState(Long id, String stateName);

}
