package com.thedal.thedal_app.volunteer;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VolunteerElectionBoothRepo extends JpaRepository<VolunteerElectionBooth, Long>{

	Optional<VolunteerElectionBooth> findByVolunteerId(Long id);

	void deleteByVolunteerId(Long id);

	void save(VolunteerBulkUploadEntity bulkUploadEntity);

	
	void deleteByVolunteerIdAndElectionId(Long volunteerId, Long electionId);
	

}
