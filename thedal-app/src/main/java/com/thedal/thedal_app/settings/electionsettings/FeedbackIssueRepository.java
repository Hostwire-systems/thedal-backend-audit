package com.thedal.thedal_app.settings.electionsettings;


import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.transaction.Transactional;

@Repository
public interface FeedbackIssueRepository extends JpaRepository<FeedbackIssue, Long> {

    // Check if issue already exists with the same name under same election
    boolean existsByIssueNameAndElectionId(String issueName, Long electionId);

    // Added for merge name lookups
    boolean existsByIssueNameAndAccountIdAndElectionId(String issueName, Long accountId, Long electionId);

    // Get all issues by account & election
    List<FeedbackIssue> findByAccountIdAndElectionId(Long accountId, Long electionId);

    // Get specific issue by account, election, and ID
    Optional<FeedbackIssue> findByAccountIdAndElectionIdAndId(Long accountId, Long electionId, Long id);

//    // Delete all issues under an account + election
//    void deleteByAccountIdAndElectionId(Long accountId, Long electionId);
//
//    // Delete specific issues by IDs
//    void deleteByAccountIdAndElectionIdAndIdIn(Long accountId, Long electionId, List<Long> ids);

    Optional<FeedbackIssue> findByIdAndAccountIdAndElectionId(Long id, Long accountId, Long electionId);

    List<FeedbackIssue> findByAccountIdAndElectionIdOrderByOrderIndexAsc(Long accountId, Long electionId);

    List<FeedbackIssue> findByElectionIdAndAccountIdOrderByOrderIndexAsc(Long electionId, Long accountId);
    
    @Modifying
    @Transactional
    @Query("DELETE FROM FeedbackIssue fi WHERE fi.accountId = :accountId AND fi.electionId = :electionId")
    int deleteByAccountIdAndElectionId(Long accountId, Long electionId);

    @Modifying
    @Transactional
    @Query("DELETE FROM FeedbackIssue fi WHERE fi.accountId = :accountId AND fi.electionId = :electionId AND fi.id IN :ids")
    int deleteByAccountIdAndElectionIdAndIdIn(Long accountId, Long electionId, List<Long> ids);

    @Query("SELECT new com.thedal.thedal_app.settings.electionsettings.FeedbackIssueResponseDTO(" +
            "fi.id, fi.issueName, fi.orderIndex, COUNT(vfi.voterId)) " +
            "FROM FeedbackIssue fi " +
            "LEFT JOIN fi.voters vfi " +
            "WHERE fi.accountId = :accountId AND fi.electionId = :electionId " +
            "GROUP BY fi.id, fi.issueName, fi.orderIndex")
     List<FeedbackIssueResponseDTO> findFeedbackIssuesWithVoterCount(Long accountId, Long electionId);
    
	// Add count method for migration validation and stats
	long countByAccountIdAndElectionId(Long accountId, Long electionId);
	
}

