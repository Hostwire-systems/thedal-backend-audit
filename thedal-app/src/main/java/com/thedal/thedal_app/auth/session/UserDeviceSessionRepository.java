package com.thedal.thedal_app.auth.session;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserDeviceSessionRepository extends JpaRepository<UserDeviceSession, Long> {
    Optional<UserDeviceSession> findByUserIdAndDeviceIdAndRevokedAtIsNull(Long userId, String deviceId);
    List<UserDeviceSession> findByUserIdAndRevokedAtIsNullOrderByLastActiveAtDesc(Long userId);
    Optional<UserDeviceSession> findByJti(String jti);

    @Modifying
    @Query("update UserDeviceSession s set s.revokedAt = CURRENT_TIMESTAMP where s.userId = :userId and s.id <> :keepId and s.revokedAt is null")
    int revokeAllOtherActive(@Param("userId") Long userId, @Param("keepId") Long keepId);

    @Modifying
    @Query("update UserDeviceSession s set s.revokedAt = CURRENT_TIMESTAMP where s.userId = :userId and s.revokedAt is null")
    int revokeAllActive(@Param("userId") Long userId);
}
