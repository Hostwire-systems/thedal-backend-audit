package com.thedal.thedal_app.session;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSessionEntity, Long> {

    /**
     * Find active sessions for a specific user
     */
    @Query("SELECT s FROM UserSessionEntity s WHERE s.userId = :userId AND s.isActive = true")
    List<UserSessionEntity> findActiveSessionsByUserId(@Param("userId") Long userId);

    /**
     * Count active sessions for a specific user
     */
    @Query("SELECT COUNT(s) FROM UserSessionEntity s WHERE s.userId = :userId AND s.isActive = true")
    int countActiveSessionsByUserId(@Param("userId") Long userId);

    /**
     * Find session by token hash
     */
    Optional<UserSessionEntity> findBySessionTokenHashAndIsActive(String sessionTokenHash, Boolean isActive);

    /**
     * Find active sessions with pagination
     */
    Page<UserSessionEntity> findByUserIdAndIsActive(Long userId, Boolean isActive, Pageable pageable);

    /**
     * Find expired sessions that are still marked as active
     */
    @Query("SELECT s FROM UserSessionEntity s WHERE s.isActive = true AND s.expiresAt < :currentTime")
    List<UserSessionEntity> findExpiredActiveSessions(@Param("currentTime") LocalDateTime currentTime);

    /**
     * Deactivate session by token hash
     */
    @Modifying
    @Query("UPDATE UserSessionEntity s SET s.isActive = false, s.updatedAt = :updateTime WHERE s.sessionTokenHash = :tokenHash")
    int deactivateSessionByTokenHash(@Param("tokenHash") String tokenHash, @Param("updateTime") LocalDateTime updateTime);

    /**
     * Deactivate all sessions for a user
     */
    @Modifying
    @Query("UPDATE UserSessionEntity s SET s.isActive = false, s.updatedAt = :updateTime WHERE s.userId = :userId AND s.isActive = true")
    int deactivateAllUserSessions(@Param("userId") Long userId, @Param("updateTime") LocalDateTime updateTime);

    /**
     * Update last access time for a session
     */
    @Modifying
    @Query("UPDATE UserSessionEntity s SET s.lastAccessTime = :accessTime, s.updatedAt = :updateTime WHERE s.sessionTokenHash = :tokenHash AND s.isActive = true")
    int updateLastAccessTime(@Param("tokenHash") String tokenHash, 
                           @Param("accessTime") LocalDateTime accessTime, 
                           @Param("updateTime") LocalDateTime updateTime);

    /**
     * Bulk deactivate expired sessions
     */
    @Modifying
    @Query("UPDATE UserSessionEntity s SET s.isActive = false, s.updatedAt = :updateTime WHERE s.isActive = true AND s.expiresAt < :currentTime")
    int deactivateExpiredSessions(@Param("currentTime") LocalDateTime currentTime, @Param("updateTime") LocalDateTime updateTime);

    /**
     * Find sessions by IP address (for security monitoring)
     */
    List<UserSessionEntity> findByIpAddressAndIsActive(String ipAddress, Boolean isActive);

    /**
     * Find recent sessions for a user (last N days)
     */
    @Query("SELECT s FROM UserSessionEntity s WHERE s.userId = :userId AND s.loginTime >= :fromDate ORDER BY s.loginTime DESC")
    List<UserSessionEntity> findRecentSessionsByUserId(@Param("userId") Long userId, @Param("fromDate") LocalDateTime fromDate);

    /**
     * Delete old inactive sessions (cleanup)
     */
    @Modifying
    @Query("DELETE FROM UserSessionEntity s WHERE s.isActive = false AND s.updatedAt < :beforeDate")
    int deleteOldInactiveSessions(@Param("beforeDate") LocalDateTime beforeDate);
    
    /**
     * Delete all sessions for a user (used when deleting user)
     */
    @Modifying
    @Query("DELETE FROM UserSessionEntity s WHERE s.userId = :userId")
    int deleteAllSessionsByUserId(@Param("userId") Long userId);
}