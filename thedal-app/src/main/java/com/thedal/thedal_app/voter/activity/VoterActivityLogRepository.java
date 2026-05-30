package com.thedal.thedal_app.voter.activity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface VoterActivityLogRepository extends JpaRepository<VoterActivityLog, Long> {
    
    // Optimized count query for a specific voter and activity type
    @Query(value = "SELECT COUNT(*) FROM voter_activity_log " +
                   "WHERE account_id = :accountId AND election_id = :electionId " +
                   "AND voter_id = :voterId AND activity_type = :activityType", 
           nativeQuery = true)
    Long countByVoterAndActivityType(
        @Param("accountId") Long accountId,
        @Param("electionId") Long electionId,
        @Param("voterId") String voterId,
        @Param("activityType") String activityType
    );
    
    // Get all activity counts for a voter in one query (performance optimized)
    @Query(value = "SELECT activity_type, COUNT(*) as count " +
                   "FROM voter_activity_log " +
                   "WHERE account_id = :accountId AND election_id = :electionId AND voter_id = :voterId " +
                   "GROUP BY activity_type", 
           nativeQuery = true)
    List<Object[]> getActivityCountsByVoter(
        @Param("accountId") Long accountId,
        @Param("electionId") Long electionId,
        @Param("voterId") String voterId
    );
    
    // Get activity history for a voter with pagination
    @Query("SELECT a FROM VoterActivityLog a " +
           "WHERE a.accountId = :accountId AND a.electionId = :electionId " +
           "AND a.voterId = :voterId " +
           "AND (:activityType IS NULL OR a.activityType = :activityType) " +
           "ORDER BY a.activityTime DESC")
    Page<VoterActivityLog> getActivityHistory(
        @Param("accountId") Long accountId,
        @Param("electionId") Long electionId,
        @Param("voterId") String voterId,
        @Param("activityType") ActivityType activityType,
        Pageable pageable
    );
    
    // Get election-wide activity summary (aggregated counts)
    @Query(value = "SELECT activity_type, COUNT(*) as count, COUNT(DISTINCT voter_id) as unique_voters " +
                   "FROM voter_activity_log " +
                   "WHERE account_id = :accountId AND election_id = :electionId " +
                   "GROUP BY activity_type", 
           nativeQuery = true)
    List<Object[]> getElectionActivitySummary(
        @Param("accountId") Long accountId,
        @Param("electionId") Long electionId
    );
    
    // Get most active voters for an election
    @Query(value = "SELECT voter_id, COUNT(*) as activity_count " +
                   "FROM voter_activity_log " +
                   "WHERE account_id = :accountId AND election_id = :electionId " +
                   "AND (:activityType IS NULL OR activity_type = :activityType) " +
                   "GROUP BY voter_id " +
                   "ORDER BY activity_count DESC " +
                   "LIMIT :limit", 
           nativeQuery = true)
    List<Object[]> getMostActiveVoters(
        @Param("accountId") Long accountId,
        @Param("electionId") Long electionId,
        @Param("activityType") String activityType,
        @Param("limit") int limit
    );
    
    // Get activity trends (time-based aggregation)
    @Query(value = "SELECT DATE(activity_time) as date, activity_type, COUNT(*) as count " +
                   "FROM voter_activity_log " +
                   "WHERE account_id = :accountId AND election_id = :electionId " +
                   "AND activity_time >= :fromDate " +
                   "GROUP BY DATE(activity_time), activity_type " +
                   "ORDER BY date DESC", 
           nativeQuery = true)
    List<Object[]> getActivityTrends(
        @Param("accountId") Long accountId,
        @Param("electionId") Long electionId,
        @Param("fromDate") LocalDateTime fromDate
    );
    
    // Bulk insert optimization - check if activity already exists within time window
    @Query(value = "SELECT COUNT(*) > 0 FROM voter_activity_log " +
                   "WHERE account_id = :accountId AND election_id = :electionId " +
                   "AND voter_id = :voterId AND activity_type = :activityType " +
                   "AND activity_time >= :sinceTime", 
           nativeQuery = true)
    boolean hasRecentActivity(
        @Param("accountId") Long accountId,
        @Param("electionId") Long electionId,
        @Param("voterId") String voterId,
        @Param("activityType") String activityType,
        @Param("sinceTime") LocalDateTime sinceTime
    );
}
