package com.thedal.thedal_app.election;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SurveyDownloadJobRepository extends JpaRepository<SurveyDownloadJob, Long> {
	
	List<SurveyDownloadJob> findByElectionIdAndAccountId(Long electionId, Long accountId);
    Optional<SurveyDownloadJob> findByIdAndAccountIdAndElectionId(Long id, Long accountId, Long electionId);
	List<SurveyDownloadJob> findAll(Specification<SurveyDownloadJob> surveySpec);
	Page<SurveyDownloadJob> findAll(Specification<SurveyDownloadJob> surveySpec, Pageable pageable);
	List<SurveyDownloadJob> findByStatusAndTimeCompletedBefore(String string, LocalDateTime threshold);
	int deleteByElectionIdAndAccountId(Long electionId, Long accountId);
	List<SurveyDownloadJob> findByIdInAndElectionIdAndAccountId(List<Long> jobIds, Long electionId, Long accountId);
	
}