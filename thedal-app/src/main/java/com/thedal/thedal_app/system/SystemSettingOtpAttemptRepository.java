package com.thedal.thedal_app.system;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SystemSettingOtpAttemptRepository extends JpaRepository<SystemSettingOtpAttempt, Long> {
    //Optional<SystemSettingOtpAttempt> findByUserIdAndSettingKeyAndIsActiveTrue(Long userId, String settingKey);

	Optional<SystemSettingOtpAttempt> findByUserIdAndSettingKey(Long userId, String settingKey);

}