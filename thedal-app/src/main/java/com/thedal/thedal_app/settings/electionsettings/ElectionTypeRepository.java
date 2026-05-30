package com.thedal.thedal_app.settings.electionsettings;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.thedal.thedal_app.election.ElectionEntity;

@Repository
public interface ElectionTypeRepository extends JpaRepository<ElectionType, Long> {

	Optional<ElectionType> findByIdAndAccountId(Long id, Long accountId);

	List<ElectionType> findAllByAccountId(Long accountId);

	ElectionType findByElectionType(String electionType);


}
