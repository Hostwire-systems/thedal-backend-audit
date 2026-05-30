package com.thedal.thedal_app.voter;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.transaction.Transactional;

@Repository
public interface VoterBenefitSchemeRepository extends JpaRepository<VoterBenefitScheme, Long> {
   
	@Query("SELECT COUNT(vbs) FROM VoterBenefitScheme vbs WHERE vbs.benefitScheme.id = :schemeId")
	long countVotersBySchemeId(@Param("schemeId") Long schemeId);
	
	@Query("SELECT vbs.voter FROM VoterBenefitScheme vbs WHERE vbs.benefitScheme.id = :schemeId")
	List<VoterEntity> findVotersBySchemeId(@Param("schemeId") Long schemeId);
	
	Optional<VoterBenefitScheme> findByVoterIdAndBenefitSchemeId(Long voterId, Long benefitSchemeId);

    @Query("SELECT COUNT(vbs) FROM VoterBenefitScheme vbs " +
           "WHERE vbs.benefitScheme.id = :benefitSchemeId " +
           "AND vbs.voter.accountId = :accountId " +
           "AND vbs.voter.electionId = :electionId")
    Long countVotersByBenefitSchemeId(Long benefitSchemeId, Long accountId, Long electionId);

    // Optional: Count only selected schemes
    @Query("SELECT COUNT(vbs) FROM VoterBenefitScheme vbs " +
           "WHERE vbs.benefitScheme.id = :benefitSchemeId " +
           "AND vbs.voter.accountId = :accountId " +
           "AND vbs.voter.electionId = :electionId " +
           "AND vbs.selected = true")
    Long countSelectedVotersByBenefitSchemeId(Long benefitSchemeId, Long accountId, Long electionId);

    @Modifying
    @Transactional
    @Query("DELETE FROM VoterBenefitScheme vbs WHERE vbs.voter.id = :voterId")
    void deleteByVoterId(@Param("voterId") Long voterId);

	
}