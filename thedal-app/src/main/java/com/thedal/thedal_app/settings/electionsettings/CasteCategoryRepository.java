package com.thedal.thedal_app.settings.electionsettings;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface CasteCategoryRepository extends JpaRepository<CasteCategoryEntity, Long> {

    // Find by caste category name, account ID, and election ID for duplicate checks
    Optional<CasteCategoryEntity> findByCasteCategoryNameAndAccountIdAndElectionId(String casteCategoryName, Long accountId, Long electionId);

    // Find by caste category name, account ID, and election ID excluding specific ID (for update checks)
    Optional<CasteCategoryEntity> findByCasteCategoryNameAndAccountIdAndElectionIdAndIdNot(String casteCategoryName, Long accountId, Long electionId, Long id);

    // Find all caste categories by account ID and election ID
    List<CasteCategoryEntity> findByAccountIdAndElectionId(Long accountId, Long electionId);

    // Find caste categories by account ID and election ID, ordered by orderIndex
    List<CasteCategoryEntity> findByAccountIdAndElectionIdOrderByOrderIndexAsc(Long accountId, Long electionId);

    // Find caste categories by ID list, account ID, and election ID
    List<CasteCategoryEntity> findByIdInAndAccountIdAndElectionId(List<Long> casteCategoryIds, Long accountId, Long electionId);

    // Find a single caste category by ID, account ID, and election ID
    Optional<CasteCategoryEntity> findByIdAndAccountIdAndElectionId(Long id, Long accountId, Long electionId);

    // Find max order index for a given election ID
    @Query("SELECT MAX(c.orderIndex) FROM CasteCategoryEntity c WHERE c.electionId = :electionId")
    Integer findMaxOrderIndexByElectionId(Long electionId);

    // Find min order index for a given election ID
    @Query("SELECT MIN(c.orderIndex) FROM CasteCategoryEntity c WHERE c.electionId = :electionId")
    Integer findMinOrderIndexByElectionId(Long electionId);

    // Delete caste categories by account ID and election ID
    @Modifying
    @Query("DELETE FROM CasteCategoryEntity c WHERE c.accountId = :accountId AND c.electionId = :electionId")
    int deleteByAccountIdAndElectionId(Long accountId, Long electionId);

    // Delete specific caste categories by IDs, account ID, and election ID
    @Modifying
    @Query("DELETE FROM CasteCategoryEntity c WHERE c.accountId = :accountId AND c.electionId = :electionId AND c.id IN :casteCategoryIds")
    int deleteByAccountIdAndElectionIdAndIds(Long accountId, Long electionId, List<Long> casteCategoryIds);

    // Added for merge name lookups
    boolean existsByCasteCategoryNameAndAccountIdAndElectionId(String casteCategoryName, Long accountId, Long electionId);
}