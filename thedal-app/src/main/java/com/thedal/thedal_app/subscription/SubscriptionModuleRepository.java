package com.thedal.thedal_app.subscription;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SubscriptionModuleRepository extends JpaRepository<SubscriptionModule, Long> {
    
    Optional<SubscriptionModule> findByModuleKey(String moduleKey);
    
    List<SubscriptionModule> findByParentModuleIdIsNull();
    
    List<SubscriptionModule> findByParentModuleId(Long parentModuleId);
    
    List<SubscriptionModule> findByIsActiveOrderByDisplayOrderAsc(Boolean isActive);
    
    @Query("SELECT sm FROM SubscriptionModule sm WHERE sm.isActive = true ORDER BY sm.displayOrder ASC")
    List<SubscriptionModule> findAllActiveModules();
    
    @Query("SELECT sm FROM SubscriptionModule sm WHERE sm.parentModuleId = :parentId AND sm.isActive = true ORDER BY sm.displayOrder ASC")
    List<SubscriptionModule> findActiveSubmodulesByParentId(@Param("parentId") Long parentId);
    
    boolean existsByModuleKey(String moduleKey);
}
