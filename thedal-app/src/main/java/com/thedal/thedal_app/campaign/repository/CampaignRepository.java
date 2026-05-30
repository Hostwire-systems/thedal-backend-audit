package com.thedal.thedal_app.campaign.repository;

import com.thedal.thedal_app.campaign.entity.CampaignEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CampaignRepository extends JpaRepository<CampaignEntity, String> {
    
    // Find campaigns by channel using simple method naming (avoids PostgreSQL type issues)
    List<CampaignEntity> findByChannelIgnoreCaseOrderByCreatedAtDesc(String channel);
    
    // Find campaigns by status using simple method naming
    List<CampaignEntity> findByStatusIgnoreCaseOrderByCreatedAtDesc(String status);
    
    // Find campaigns by channel and status using simple method naming
    List<CampaignEntity> findByChannelIgnoreCaseAndStatusIgnoreCaseOrderByCreatedAtDesc(String channel, String status);
    
    // Find campaigns with title containing search term using simple method naming
    List<CampaignEntity> findByTitleContainingIgnoreCaseOrderByCreatedAtDesc(String title);
    
    // Find campaigns with title containing search term using native SQL to avoid PostgreSQL type issues
    @Query(value = "SELECT * FROM campaigns c WHERE LOWER(c.title::text) LIKE LOWER(CONCAT('%', :searchTerm::text, '%'))", nativeQuery = true)
    List<CampaignEntity> findByTitleContainingIgnoreCase(@Param("searchTerm") String searchTerm);
    
    // Find campaigns with combined filters using native SQL to avoid PostgreSQL type issues
    @Query(value = "SELECT * FROM campaigns c WHERE " +
           "(:channel IS NULL OR LOWER(c.channel::text) = LOWER(:channel::text)) AND " +
           "(:status IS NULL OR LOWER(c.status::text) = LOWER(:status::text)) AND " +
           "(:searchTerm IS NULL OR LOWER(c.title::text) LIKE LOWER(CONCAT('%', :searchTerm::text, '%'))) " +
           "ORDER BY c.created_at DESC", nativeQuery = true)
    List<CampaignEntity> findWithFilters(
        @Param("channel") String channel,
        @Param("status") String status,
        @Param("searchTerm") String searchTerm
    );
    
    // Find campaigns with pagination using native SQL to avoid PostgreSQL type issues
    @Query(value = "SELECT * FROM campaigns c WHERE " +
           "(:channel IS NULL OR LOWER(c.channel::text) = LOWER(:channel::text)) AND " +
           "(:status IS NULL OR LOWER(c.status::text) = LOWER(:status::text)) AND " +
           "(:searchTerm IS NULL OR LOWER(c.title::text) LIKE LOWER(CONCAT('%', :searchTerm::text, '%'))) " +
           "ORDER BY c.created_at DESC", 
           countQuery = "SELECT count(*) FROM campaigns c WHERE " +
           "(:channel IS NULL OR LOWER(c.channel::text) = LOWER(:channel::text)) AND " +
           "(:status IS NULL OR LOWER(c.status::text) = LOWER(:status::text)) AND " +
           "(:searchTerm IS NULL OR LOWER(c.title::text) LIKE LOWER(CONCAT('%', :searchTerm::text, '%')))", 
           nativeQuery = true)
    Page<CampaignEntity> findWithFiltersPageable(
        @Param("channel") String channel,
        @Param("status") String status,
        @Param("searchTerm") String searchTerm,
        Pageable pageable
    );
    
    // Find campaigns by status for monitoring
    List<CampaignEntity> findByStatusOrderByCreatedAtDesc(String status);
    
    // Count campaigns by status
    long countByStatus(String status);
    
    // Count campaigns by channel
    long countByChannel(String channel);
}