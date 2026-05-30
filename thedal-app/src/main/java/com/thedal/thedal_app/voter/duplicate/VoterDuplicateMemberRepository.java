package com.thedal.thedal_app.voter.duplicate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoterDuplicateMemberRepository extends JpaRepository<VoterDuplicateMember, Long> {
    List<VoterDuplicateMember> findByGroupId(Long groupId);

    // Paginated query for duplicate voters (ordered by group size descending, then by voter details)
    @Query("SELECT new com.thedal.thedal_app.voter.duplicate.DuplicateVoterDTO(v.id, v.voterFnameEn, v.voterLnameEn, v.rlnFnameEn, v.rlnLnameEn, m.partNo, m.serialNo, v.epicNumber, v.age, v.gender, v.sectionNo) " +
        "FROM VoterDuplicateMember m " +
        "JOIN m.group g " +
        "JOIN m.voter v " +
        "WHERE g.run.id = :runId " +
        "ORDER BY g.size DESC, v.voterFnameEn, v.voterLnameEn")
    Page<DuplicateVoterDTO> findDuplicateVotersByRunId(@Param("runId") Long runId, Pageable pageable);

    // Paginated query with part number filter
    @Query("SELECT new com.thedal.thedal_app.voter.duplicate.DuplicateVoterDTO(v.id, v.voterFnameEn, v.voterLnameEn, v.rlnFnameEn, v.rlnLnameEn, m.partNo, m.serialNo, v.epicNumber, v.age, v.gender, v.sectionNo) " +
        "FROM VoterDuplicateMember m " +
        "JOIN m.group g " +
        "JOIN m.voter v " +
        "WHERE g.run.id = :runId AND m.partNo IN :parts " +
        "ORDER BY g.size DESC, v.voterFnameEn, v.voterLnameEn")
    Page<DuplicateVoterDTO> findDuplicateVotersByRunIdAndParts(@Param("runId") Long runId, @Param("parts") java.util.Set<Integer> parts, Pageable pageable);

    // Export projection for all duplicates in a run
    @Query("SELECT new com.thedal.thedal_app.voter.duplicate.DuplicateVoterExportRow(g.id, g.size, v.id, v.voterFnameEn, v.voterLnameEn, v.rlnFnameEn, v.rlnLnameEn, m.partNo, m.serialNo, v.epicNumber, v.age, v.gender, v.sectionNo) " +
        "FROM VoterDuplicateMember m " +
        "JOIN m.group g " +
        "JOIN m.voter v " +
        "WHERE g.run.id = :runId")
    List<DuplicateVoterExportRow> findExportRowsByRunId(@Param("runId") Long runId);

    // Export projection filtered by part number
    @Query("SELECT new com.thedal.thedal_app.voter.duplicate.DuplicateVoterExportRow(g.id, g.size, v.id, v.voterFnameEn, v.voterLnameEn, v.rlnFnameEn, v.rlnLnameEn, m.partNo, m.serialNo, v.epicNumber, v.age, v.gender, v.sectionNo) " +
        "FROM VoterDuplicateMember m " +
        "JOIN m.group g " +
        "JOIN m.voter v " +
        "WHERE g.run.id = :runId AND m.partNo = :partNo")
    List<DuplicateVoterExportRow> findExportRowsByRunIdAndPartNo(@Param("runId") Long runId, @Param("partNo") Integer partNo);

    // Export projection filtered by multiple parts
    @Query("SELECT new com.thedal.thedal_app.voter.duplicate.DuplicateVoterExportRow(g.id, g.size, v.id, v.voterFnameEn, v.voterLnameEn, v.rlnFnameEn, v.rlnLnameEn, m.partNo, m.serialNo, v.epicNumber, v.age, v.gender, v.sectionNo) " +
        "FROM VoterDuplicateMember m " +
        "JOIN m.group g " +
        "JOIN m.voter v " +
        "WHERE g.run.id = :runId AND m.partNo IN :parts")
    List<DuplicateVoterExportRow> findExportRowsByRunIdAndParts(@Param("runId") Long runId, @Param("parts") java.util.Set<Integer> parts);
}
