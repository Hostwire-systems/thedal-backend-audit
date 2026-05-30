package com.thedal.thedal_app.election;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PartManagerBulkUploadRepository  extends JpaRepository<PartManagerBulkUploadEntity, Long> {

	Optional<PartManagerBulkUploadEntity> findByIdAndElectionId(Long id, Long electionId);

}
