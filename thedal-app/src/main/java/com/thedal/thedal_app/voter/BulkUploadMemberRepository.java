package com.thedal.thedal_app.voter;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BulkUploadMemberRepository extends JpaRepository<BulkUploadMemberEntity, Long> {

	Optional<BulkUploadMemberEntity> findByIdAndAccountIdAndElectionId(Long id, Long accountId, Long electionId);
}
