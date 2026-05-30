package com.thedal.thedal_app.system;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SystemSettingRepository extends JpaRepository<SystemSettingEntity, Long> {
    Optional<SystemSettingEntity> findBySettingKey(String settingKey);
    
    @Query("SELECT s.settingValue FROM SystemSettingEntity s WHERE s.settingKey = :key")
    Optional<String> findValueByKey(@Param("key") String key);
}
