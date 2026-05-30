package com.thedal.thedal_app.voter;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BulkUploadErrorRepository extends JpaRepository<BulkUploadErrorEntity, Long> {

	List<BulkUploadErrorEntity> findByBulkUploadIdAndElectionIdAndAccountId(Long bulkUploadId, Long electionId,
			Long accountId);

	Optional<BulkUploadErrorEntity> findByBulkUploadId(Long id);

}