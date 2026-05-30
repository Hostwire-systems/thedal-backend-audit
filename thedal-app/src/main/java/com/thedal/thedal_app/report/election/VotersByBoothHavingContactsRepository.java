package com.thedal.thedal_app.report.election;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.thedal.thedal_app.volunteer.VolunteerEntity;

public interface VotersByBoothHavingContactsRepository extends JpaRepository<VotersByBoothHavingContactsEntity, Long> {

	Optional<VotersByBoothHavingContactsEntity> findByElectionIdAndAccountIdAndBoothNumber(Long electionId, Long accountId,
			Integer boothNumber);

	List<VotersByBoothHavingContactsEntity> findByElectionIdAndAccountId(Long electionId, Long accountId);

	Optional<VotersByBoothHavingContactsEntity> findByElectionIdAndAccountIdAndBoothNumberAndMobileNumber(Long electionId, Long accountId,
			Integer boothNumber, String mobileNumber);

}

