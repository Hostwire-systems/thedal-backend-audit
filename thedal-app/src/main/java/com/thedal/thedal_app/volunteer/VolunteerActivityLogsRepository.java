package com.thedal.thedal_app.volunteer;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VolunteerActivityLogsRepository extends JpaRepository<VolunteerActivityLogsEntity, Long> {

	Optional<VolunteerActivityLogsEntity> findTopByVolunteerDailyActivityEntityOrderByCurrentTimeStampDesc(
			VolunteerDailyActivityEntity existingVolunteerDailyActivity);
	
	Page<VolunteerActivityLogsEntity> findByVolunteerIdAndActivityDateBetweenOrderByCurrentTimeStampDesc(
            Long volunteerId, LocalDate startDate, LocalDate endDate,Pageable pageable);

	Optional<VolunteerActivityLogsEntity> findTopByVolunteerIdOrderByCurrentTimeStampDesc(Long volunteerId);

}
