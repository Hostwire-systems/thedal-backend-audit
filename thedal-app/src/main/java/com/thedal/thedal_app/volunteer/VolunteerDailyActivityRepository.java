package com.thedal.thedal_app.volunteer;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.thedal.thedal_app.role.Role;

public interface VolunteerDailyActivityRepository extends JpaRepository<VolunteerDailyActivityEntity, Long> {

//	boolean existsByVolunteerIdAndCheckInTimeBetween(Long volunteerId, LocalDateTime startOfDay,
//			LocalDateTime endOfDay);

	Optional<VolunteerDailyActivityEntity> findByVolunteerIdAndCheckInTimeBetween(Long volunteerId,
			LocalDateTime startOfDay, LocalDateTime endOfDay);

	Optional<VolunteerDailyActivityEntity> findByVolunteerIdAndCheckInTimeBetweenAndIsChecked(Long userId, LocalDateTime startOfDay,
			LocalDateTime endOfDay, boolean b);

//	boolean existsByUserIdAndCheckInTimeBetween(Long currentUserId, LocalDateTime startOfDay, LocalDateTime endOfDay);

	boolean existsByVolunteerIdAndCheckInTimeBetweenAndIsChecked(Long volunteerId, LocalDateTime startOfDay,
			LocalDateTime endOfDay, boolean b);
	
	//long countByVolunteerIdAndCheckedIsTrueAndCheckInTimeIsToday(Long volunteerId);

	@Query(value = """
		    SELECT COUNT(vda.id)
		    FROM volunteer_daily_activitity vda
		    INNER JOIN (
		        SELECT id
		        FROM volunteers
		        WHERE election_id = :electionId
		    ) v ON v.id = vda.volunteer_id
		    WHERE vda.is_checked = true
		      AND CAST(vda.check_in_time AS DATE) = CURRENT_DATE
		""", nativeQuery = true)
		int countCheckedInVolunteersFilterFirst(@Param("electionId") Long electionId);
}
