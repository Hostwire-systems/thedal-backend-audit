package com.thedal.thedal_app.settings.electionsettings;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BenefitSchemesRepository extends JpaRepository<BenefitSchemes, Long> {

    List<BenefitSchemes> findByAccountIdAndElectionId(Long accountId, Long electionId);
    Optional<BenefitSchemes> findByIdAndAccountIdAndElectionId(Long id, Long accountId, Long electionId);
	List<BenefitSchemes> findByIdInAndAccountIdAndElectionId(List<Long> benefitSchemeIds, Long accountId, Long electionId);

	List<BenefitSchemes> findByElectionIdAndAccountIdOrderByOrderIndexAsc(Long electionId, Long accountId);
	List<BenefitSchemes> findByAccountIdAndElectionIdOrderByOrderIndexAsc(Long accountId, Long electionId);
	//Integer findMaxOrderIndexByElectionId(Long electionId, Long accountId);
	@Query("SELECT MAX(b.orderIndex) FROM BenefitSchemes b WHERE b.electionId = :electionId")
	Integer findMaxOrderIndexByElectionId(@Param("electionId") Long electionId);
	boolean existsBySchemeNameAndElectionId(String schemeName, Long electionId);
	Optional<BenefitSchemes> findBySchemeNameAndAccountIdAndElectionId(String schemeName, Long accountId, Long electionId);
	int deleteByAccountIdAndElectionId(Long accountId, Long electionId);

@Modifying
@Query("DELETE FROM BenefitSchemes bs WHERE bs.accountId = :accountId AND bs.electionId = :electionId AND bs.id IN :benefitSchemeIds")
int deleteByAccountIdAndElectionIdAndIds(@Param("accountId") Long accountId,
                                         @Param("electionId") Long electionId,
                                         @Param("benefitSchemeIds") List<Long> benefitSchemeIds);
@Query("SELECT MIN(b.orderIndex) FROM BenefitSchemes b WHERE b.electionId = :electionId")
Integer findMinOrderIndexByElectionId(@Param("electionId") Long electionId);

List<BenefitSchemes> findByAccountIdAndElectionIdOrderByCreatedAtDesc(Long accountId, Long electionId);
List<BenefitSchemes> findByElectionIdAndAccountIdOrderByCreatedAtDesc(Long electionId, Long accountId);

//@Query("SELECT b, COUNT(v.id) as voterCount " +
//        "FROM BenefitSchemes b LEFT JOIN b.voters v " +
//        "WHERE b.accountId = :accountId AND b.electionId = :electionId " +
//        "GROUP BY b.id")
// List<Object[]> findBenefitSchemesWithVoterCount(Long accountId, Long electionId);
// 
// @Query("SELECT bs FROM BenefitSchemes bs WHERE bs.accountId = :accountId AND bs.electionId = :electionId AND EXISTS " +
//         "(SELECT 1 FROM bs.voters v WHERE v.electionId = :electionId)")
//  List<BenefitSchemes> findLinkedBenefitSchemes(Long accountId, Long electionId);
//
//  @Query("SELECT bs FROM BenefitSchemes bs WHERE bs.id IN :schemeIds AND bs.accountId = :accountId AND bs.electionId = :electionId AND EXISTS " +
//         "(SELECT 1 FROM bs.voters v WHERE v.electionId = :electionId)")
//  List<BenefitSchemes> findLinkedBenefitSchemesByIds(List<Long> schemeIds, Long accountId, Long electionId);
// 
	// Add count method for migration validation and stats
	long countByAccountIdAndElectionId(Long accountId, Long electionId);
	
	
	///////////////////////////////////////////////////
	
	 // Updated query using the join table approach
    @Query("SELECT b, COUNT(vbs.id) as voterCount " +
           "FROM BenefitSchemes b LEFT JOIN VoterBenefitScheme vbs ON b.id = vbs.benefitScheme.id " +
           "WHERE b.accountId = :accountId AND b.electionId = :electionId " +
           "GROUP BY b.id")
    List<Object[]> findBenefitSchemesWithVoterCount(@Param("accountId") Long accountId, 
                                                  @Param("electionId") Long electionId);
     
    // Updated query using the join table approach
    @Query("SELECT bs FROM BenefitSchemes bs " +
           "WHERE bs.accountId = :accountId AND bs.electionId = :electionId AND " +
           "EXISTS (SELECT 1 FROM VoterBenefitScheme vbs WHERE vbs.benefitScheme.id = bs.id)")
    List<BenefitSchemes> findLinkedBenefitSchemes(@Param("accountId") Long accountId, 
                                                @Param("electionId") Long electionId);

    // Updated query using the join table approach
    @Query("SELECT bs FROM BenefitSchemes bs " +
           "WHERE bs.id IN :schemeIds AND bs.accountId = :accountId AND bs.electionId = :electionId AND " +
           "EXISTS (SELECT 1 FROM VoterBenefitScheme vbs WHERE vbs.benefitScheme.id = bs.id)")
    List<BenefitSchemes> findLinkedBenefitSchemesByIds(@Param("schemeIds") List<Long> schemeIds, 
                                                      @Param("accountId") Long accountId, 
                                                      @Param("electionId") Long electionId);
     
	
	
	
	
}
