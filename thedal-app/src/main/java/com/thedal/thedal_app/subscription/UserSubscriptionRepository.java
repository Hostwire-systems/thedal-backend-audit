package com.thedal.thedal_app.subscription;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, Long> {
    
    List<UserSubscription> findByUserId(Long userId);
    
    @Query("SELECT us FROM UserSubscription us WHERE us.user.id = :userId AND us.hasAccess = true " +
           "AND (us.expiresAt IS NULL OR us.expiresAt > :currentTime)")
    List<UserSubscription> findActiveSubscriptionsByUserId(@Param("userId") Long userId, 
                                                            @Param("currentTime") LocalDateTime currentTime);
    
    Optional<UserSubscription> findByUserIdAndModuleId(Long userId, Long moduleId);
    
    @Query("SELECT us FROM UserSubscription us WHERE us.user.id = :userId AND us.module.moduleKey = :moduleKey " +
           "AND us.hasAccess = true AND (us.expiresAt IS NULL OR us.expiresAt > :currentTime)")
    Optional<UserSubscription> findActiveSubscriptionByUserIdAndModuleKey(@Param("userId") Long userId, 
                                                                           @Param("moduleKey") String moduleKey,
                                                                           @Param("currentTime") LocalDateTime currentTime);
    
    @Query("SELECT COUNT(us) FROM UserSubscription us WHERE us.module.id = :moduleId AND us.hasAccess = true")
    Long countActiveSubscriptionsByModuleId(@Param("moduleId") Long moduleId);
    
    void deleteByUserIdAndModuleId(Long userId, Long moduleId);
    
    @Query("SELECT us FROM UserSubscription us WHERE us.expiresAt IS NOT NULL AND us.expiresAt <= :currentTime AND us.hasAccess = true")
    List<UserSubscription> findExpiredSubscriptions(@Param("currentTime") LocalDateTime currentTime);
}
