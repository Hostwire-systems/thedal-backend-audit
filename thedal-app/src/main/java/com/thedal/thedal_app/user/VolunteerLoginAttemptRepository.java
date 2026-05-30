package com.thedal.thedal_app.user;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.thedal.thedal_app.role.Role;

public interface VolunteerLoginAttemptRepository extends JpaRepository<VolunteerLoginAttempt, Long> {
    VolunteerLoginAttempt findByVolunteerUserIdAndIsActive(Long volunteerUserId, boolean isActive);

    Optional<VolunteerLoginAttempt> findByVolunteerUserIdAndAdminUserIdAndIsActive(Long volunteerUserId, Long adminUserId, boolean isActive);
    
    @Query("SELECT v FROM VolunteerLoginAttempt v " +
            "WHERE v.volunteerUserId = :volunteerUserId " +
            "AND v.isActive = true " +
            "AND v.expiresAt > :currentTime " +
            "ORDER BY v.createdAt DESC")
     Optional<VolunteerLoginAttempt> findLatestActiveAttempt(
         @Param("volunteerUserId") Long volunteerUserId,
         @Param("currentTime") LocalDateTime currentTime);

	Optional<VolunteerLoginAttempt> findByVolunteerUserIdAndOtpAndIsActiveTrue(Long volunteerUserId, String otp);
    
}