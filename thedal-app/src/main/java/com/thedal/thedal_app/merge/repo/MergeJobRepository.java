package com.thedal.thedal_app.merge.repo;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.thedal.thedal_app.merge.MergeJobStatus;
import com.thedal.thedal_app.merge.entity.MergeJobEntity;

import java.util.List;

public interface MergeJobRepository extends JpaRepository<MergeJobEntity, UUID> {
    boolean existsByTargetElectionIdAndStatusIn(Long targetElectionId, Iterable<MergeJobStatus> statuses);
    Optional<MergeJobEntity> findFirstByTargetElectionIdOrderByCreatedAtDesc(Long targetElectionId);
    
    // Find active jobs for debugging
    List<MergeJobEntity> findByTargetElectionIdAndStatusIn(Long targetElectionId, Iterable<MergeJobStatus> statuses);

    @EntityGraph(attributePaths = {"fields"})
    Optional<MergeJobEntity> findWithFieldsById(UUID id);

    Page<MergeJobEntity> findByTargetElectionIdOrderByCreatedAtDesc(Long targetElectionId, Pageable pageable);

    Optional<MergeJobEntity> findByIdAndTargetElectionId(UUID id, Long targetElectionId);
}
