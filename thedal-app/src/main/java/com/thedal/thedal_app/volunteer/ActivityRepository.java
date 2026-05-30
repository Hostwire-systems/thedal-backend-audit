package com.thedal.thedal_app.volunteer;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityRepository extends JpaRepository<ActivityEntity, Long>{

	//List<ActivityEntity> findByVolunteerId(String volunteerId); // Custom method to fetch activities by volunteer ID

//	List<ActivityEntity> findByVolunteer(VolunteerEntity volunteer);
//
//	//List<ActivityEntity> findByVolunteer(String volunteerId);
//
//	List<ActivityEntity> findByVolunteerId(String volunteerId);
//	 List<ActivityEntity> findByVolunteer_VolunteerId(Long volunteerId);
//
//	List<ActivityEntity> findByVolunteer_VolunteerIdAndDateBetween(Long volunteerId, LocalDate startDate,
//			LocalDate endDate);

}
