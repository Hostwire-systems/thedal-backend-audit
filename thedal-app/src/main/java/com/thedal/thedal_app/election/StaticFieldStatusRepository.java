package com.thedal.thedal_app.election;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface StaticFieldStatusRepository extends JpaRepository<StaticFieldStatusEntity, Long> {

    /**
     * Find static field status by account, election, and field name
     */
    Optional<StaticFieldStatusEntity> findByAccountIdAndElectionIdAndFieldName(
            Long accountId, Long electionId, String fieldName);

    /**
     * Get all static field statuses for a specific election
     */
    List<StaticFieldStatusEntity> findByAccountIdAndElectionId(Long accountId, Long electionId);

    /**
     * Get all enabled static fields for a specific election
     */
    @Query("SELECT s FROM StaticFieldStatusEntity s WHERE s.accountId = :accountId AND s.electionId = :electionId AND s.status = true")
    List<StaticFieldStatusEntity> findEnabledFieldsByAccountIdAndElectionId(
            @Param("accountId") Long accountId, @Param("electionId") Long electionId);

    /**
     * Get all disabled static fields for a specific election
     */
    @Query("SELECT s FROM StaticFieldStatusEntity s WHERE s.accountId = :accountId AND s.electionId = :electionId AND s.status = false")
    List<StaticFieldStatusEntity> findDisabledFieldsByAccountIdAndElectionId(
            @Param("accountId") Long accountId, @Param("electionId") Long electionId);

    /**
     * Check if a specific field is enabled for an election
     */
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN s.status ELSE true END FROM StaticFieldStatusEntity s WHERE s.accountId = :accountId AND s.electionId = :electionId AND s.fieldName = :fieldName")
    Boolean isFieldEnabled(@Param("accountId") Long accountId, @Param("electionId") Long electionId, @Param("fieldName") String fieldName);

    /**
     * Get static field statuses by category for a specific election
     */
    @Query("SELECT s FROM StaticFieldStatusEntity s WHERE s.accountId = :accountId AND s.electionId = :electionId AND s.fieldCategory = :category")
    List<StaticFieldStatusEntity> findByAccountIdAndElectionIdAndFieldCategory(
            @Param("accountId") Long accountId, @Param("electionId") Long electionId, @Param("category") String category);

    /**
     * Delete all static field statuses for a specific election (cleanup)
     */
    void deleteByAccountIdAndElectionId(Long accountId, Long electionId);
}