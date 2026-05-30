package com.thedal.thedal_app.voter.duplicate;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoterDuplicateGroupRepository extends JpaRepository<VoterDuplicateGroup, Long> {
    List<VoterDuplicateGroup> findByRunIdOrderBySizeDesc(Long runId);
}
