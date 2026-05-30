package com.thedal.thedal_app.report.cadre;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.thedal.thedal_app.election.ElectionEntity;
import com.thedal.thedal_app.report.dto.CadreElectionOverviewResponseDTO;
import com.thedal.thedal_app.report.dto.CadrePerformanceDto;

public interface VolunteerVsVoterReportRepository extends JpaRepository<VolunteerVsVoterReportEntity, Long> {

//	 @Query("SELECT new com.thedal.thedal_app.report.dto.CadreElectionOverviewResponseDTO(" +
//	            "COUNT(e.totalMobileNumberUpdated), COUNT(e.totalReligionUpdated), COUNT(e.totalCasteUpdated), COUNT(e.totalDobUpdated), COUNT(e.totalPartyUpdated))" +
//	            " FROM VolunteerVsVoterReportEntity e" +
//	            " WHERE e.electionId = :electionId")
//	    CadreElectionOverviewResponseDTO getTotalUpdatedSummaryByElectionId(@Param("electionId") Long electionId);
//	 
	@Query("SELECT new com.thedal.thedal_app.report.dto.CadreElectionOverviewResponseDTO(" +
		       "COALESCE(SUM(e.totalWhatsAppNumberUpdated), 0), " +
		       "COALESCE(SUM(e.totalRolesUpdated), 0), " +
		       "COALESCE(SUM(e.totalBoothsUpdated), 0), " +
		       "COALESCE(SUM(e.totalAddressUpdated), 0)) " +
		       "FROM VolunteerVsVoterReportEntity e " +
		       "WHERE e.electionId = :electionId")
		CadreElectionOverviewResponseDTO getTotalUpdatedSummaryByElectionId(@Param("electionId") Long electionId);
	
//	 @Query("SELECT new com.thedal.thedal_app.report.dto.CadrePerformanceDto(e.volunteerId, e.totalVoterCreated) " +
//	            "FROM VolunteerVsVoterReportEntity e " +
//	            "WHERE e.electionId = :electionId " +
//	            "ORDER BY e.totalVoterCreated ASC " +
//	            "LIMIT 10")
//	    List<CadrePerformanceDto> findTop10ByElectionIdOrderByTotalVoterCreated(@Param("electionId") Long electionId);
//	 
//	 @Query("SELECT new com.thedal.thedal_app.report.dto.CadrePerformanceDto(e.volunteerId, e.totalVoterCreated) " +
//	           "FROM VolunteerVsVoterReportEntity e " +
//	           "WHERE e.electionId = :electionId " +
//	           "ORDER BY e.totalVoterCreated ASC")
//	    List<CadrePerformanceDto> findLeast10ByElectionIdOrderByTotalVoterCreated(@Param("electionId") Long electionId, Pageable pageable);
//	 
	
	@Query("SELECT new com.thedal.thedal_app.report.dto.CadrePerformanceDto(e.userId, e.totalVoterCreated) " +
	           "FROM VolunteerVsVoterReportEntity e " +
	           "WHERE e.electionId = :electionId " +
	           //"WHERE e.electionId = :electionId AND e.totalVoterCreated > 0 " +
	           "ORDER BY e.totalVoterCreated DESC " +
	           "LIMIT 10")
	    List<CadrePerformanceDto> findTop10ByElectionIdOrderByTotalVoterCreated(@Param("electionId") Long electionId);

	    @Query("SELECT new com.thedal.thedal_app.report.dto.CadrePerformanceDto(e.userId, e.totalVoterCreated) " +
	           "FROM VolunteerVsVoterReportEntity e " +
	           "WHERE e.electionId = :electionId " +
	           //"WHERE e.electionId = :electionId AND e.totalVoterCreated > 0 " +
	           "ORDER BY e.totalVoterCreated ASC")
	    List<CadrePerformanceDto> findLeast10ByElectionIdOrderByTotalVoterCreated(@Param("electionId") Long electionId, Pageable pageable);
	
	Optional<VolunteerVsVoterReportEntity> findByElectionIdAndVolunteerId(Long electionId, Long volunteerId);

	Optional<VolunteerVsVoterReportEntity> findByElectionId(Long electionId);

	Optional<VolunteerVsVoterReportEntity> findByElectionIdAndUserId(Long electionId, Long userId);

	@Query("SELECT new com.thedal.thedal_app.report.dto.CadrePerformanceDto(e.userId, COALESCE(SUM(e.totalVoterCreated), 0)) " +
	           "FROM VolunteerVsVoterReportEntity e " +
	           "WHERE e.accountId = :accountId " +
	           "GROUP BY e.userId " +
	           "ORDER BY e.userId")
	    List<CadrePerformanceDto> findByAccountIdOrderByUserId(@Param("accountId") Long accountId);
	
	//	 @Query("SELECT e.volunteerId FROM VolunteerVsVoterReportEntity e WHERE e.electionId = :electionId ORDER BY e.totalVoterCreated DESC LIMIT 10")
//	    List<Long> findLeast10ByElectionIdOrderByTotalVoterCreated(@Param("electionId") Long electionId);

}
