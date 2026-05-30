package com.thedal.thedal_app.notification;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.thedal.thedal_app.user.UserEntity;

public interface NotificationsLogRepository  extends JpaRepository<NotificationsEntity, Long> {

    List<NotificationsEntity> findByCreatedAtAfterAndUserIdOrderByCreatedAtDesc(LocalDateTime createdAt,UserEntity user, Pageable pageable);

    void deleteByCreatedAtBefore(Date date);
    
    @Modifying
    @Query("DELETE FROM NotificationsEntity n WHERE n.userId.id = :userId")
    int deleteByUserId(@Param("userId") Long userId);
}
 
