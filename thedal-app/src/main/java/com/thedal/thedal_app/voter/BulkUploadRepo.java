package com.thedal.thedal_app.voter;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BulkUploadRepo extends JpaRepository<BulkUploadEntity, Long>{

	List<BulkUploadEntity> findByElectionIdOrderByStartTimeDesc(Long electionId);

//	 Page<BulkUploadEntity> findByElectionId(Long electionId, Pageable pageable);
//
//	 Page<BulkUploadEntity> findByElectionIdAndStatus(Long electionId, String status, Pageable pageable);
//
//	 Page<BulkUploadEntity> findAllByOrderByStartTimeDesc(Pageable pageable);
//
//	 Page<BulkUploadEntity> findAllByOrderByIdDesc(Pageable pageable);
	// Fetch all bulk uploads sorted by ID
    List<BulkUploadEntity> findAllByOrderByIdDesc();

    // Fetch all bulk uploads sorted by start time
    List<BulkUploadEntity> findAllByOrderByStartTimeDesc();

    // Fetch all bulk uploads for a specific election ID and filter by status
    Page<BulkUploadEntity> findByElectionIdAndStatus(Long electionId, BulkUploadStatus status, Pageable pageable);

//	Page<BulkUploadEntity> findAllByOrderByIdDesc(Pageable pageable);
//
//	Page<BulkUploadEntity> findAllByOrderByStartTimeDesc(Pageable pageable);
    @Query("SELECT b FROM BulkUploadEntity b ORDER BY b.id DESC")
    Page<BulkUploadEntity> findAllByOrderByIdDesc(Pageable pageable);

    @Query("SELECT b FROM BulkUploadEntity b ORDER BY b.startTime DESC")
    Page<BulkUploadEntity> findAllByOrderByStartTimeDesc(Pageable pageable);

	Page<BulkUploadEntity> findByAccountIdAndElectionIdAndStatus(Long accountId, Long electionId,
			BulkUploadStatus valueOf, Pageable pageable);

	Page<BulkUploadEntity> findByAccountIdAndElectionIdOrderByIdDesc(Long accountId, Long electionId,
			Pageable pageable);

	Page<BulkUploadEntity> findByAccountIdAndElectionIdOrderByStartTimeDesc(Long accountId, Long electionId,
			Pageable pageable);

	//Optional<VoterEntity> findByIdAndAccountId(Long bulkUploadId, Long accountId);
	Optional<BulkUploadEntity> findByIdAndAccountId(Long bulkUploadId, Long accountId);

	Optional<BulkUploadEntity> findByElectionId(Long electionId);

	Optional<BulkUploadEntity> findByAccountIdAndElectionId(Long accountId, Long electionId);

	Optional<BulkUploadEntity> findByIdAndAccountIdAndElectionId(Long bulkUploadId, Long accountId, Long electionId);
	
}
