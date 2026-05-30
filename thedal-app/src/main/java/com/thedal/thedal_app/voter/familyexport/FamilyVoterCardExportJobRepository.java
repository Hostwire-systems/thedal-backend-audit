package com.thedal.thedal_app.voter.familyexport;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FamilyVoterCardExportJobRepository extends JpaRepository<FamilyVoterCardExportJob, Long> {
    Page<FamilyVoterCardExportJob> findByAccountIdAndElectionIdAndFamilyId(Long accountId, Long electionId, UUID familyId, Pageable pageable);
    
    // List all jobs for an account and election (both PDF and Excel)
    Page<FamilyVoterCardExportJob> findByAccountIdAndElectionId(Long accountId, Long electionId, Pageable pageable);
    
    // List jobs by export type
    Page<FamilyVoterCardExportJob> findByAccountIdAndElectionIdAndExportType(Long accountId, Long electionId, FamilyVoterCardExportJob.ExportType exportType, Pageable pageable);
    
    // List jobs by part number
    Page<FamilyVoterCardExportJob> findByAccountIdAndElectionIdAndPartNo(Long accountId, Long electionId, Integer partNo, Pageable pageable);
}
