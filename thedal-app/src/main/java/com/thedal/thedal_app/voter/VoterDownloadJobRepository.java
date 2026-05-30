package com.thedal.thedal_app.voter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface VoterDownloadJobRepository extends JpaRepository<VoterDownloadJob, Long>,
                                                JpaSpecificationExecutor<VoterDownloadJob> {

	Optional<VoterDownloadJob> findByIdAndAccountIdAndElectionId(Long jobId, Long accountId, Long electionId);
	Optional<VoterDownloadJob> findByIdAndAccountId(Long jobId, Long accountId);
    List<VoterDownloadJob> findAllByAccountIdAndElectionIdOrderByTimeStartedDesc(Long accountId, Long electionId);

    Page<VoterDownloadJob> findByAccountIdAndElectionId(Long accountId, Long electionId, Pageable pageable);
	List<VoterDownloadJob> findByStatusAndTimeCompletedBefore(String string, LocalDateTime threshold);
	List<VoterDownloadJob> findByAccountIdAndElectionIdAndStatus(Long accountId, Long electionId, String status);
}
