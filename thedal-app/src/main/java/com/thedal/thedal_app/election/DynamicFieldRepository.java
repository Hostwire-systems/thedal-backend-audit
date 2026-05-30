package com.thedal.thedal_app.election;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.thedal.thedal_app.voter.DynamicFieldMapping;

@Repository
public interface DynamicFieldRepository extends JpaRepository<DynamicFieldEntity, Long> {

    Optional<DynamicFieldEntity> findByIdAndAccountIdAndElectionId(Long id, Long accountId, Long electionId);

    List<DynamicFieldEntity> findByElectionIdAndAccountIdOrderByOrderIndexAsc(Long electionId, Long accountId);

    @Query("SELECT COALESCE(MAX(orderIndex), -1) FROM DynamicFieldEntity WHERE electionId = :electionId AND accountId = :accountId")
    Integer findMaxOrderIndexByElectionIdAndAccountId(Long electionId, Long accountId);

    int countByElectionIdAndAccountId(Long electionId, Long accountId);

    List<DynamicFieldEntity> findByIdInAndAccountIdAndElectionId(List<Long> ids, Long accountId, Long electionId);

    int deleteByAccountIdAndElectionId(Long accountId, Long electionId);

    List<DynamicFieldEntity> findByElectionIdAndAccountId(Long electionId, Long accountId);

	List<DynamicFieldEntity> findByAccountIdAndElectionId(Long accountId, Long electionId);

    // Active only ordered list (used for voter dynamic field validation/display)
    List<DynamicFieldEntity> findByAccountIdAndElectionIdAndStatusTrueOrderByOrderIndexAsc(Long accountId, Long electionId);
}