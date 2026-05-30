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
public interface LanguageRepository extends  JpaRepository<Language, Long> {

    boolean existsByLanguageNameAndElectionId(String languageName, Long electionId);

    boolean existsByLanguageNameAndElectionIdAndIdNot(String languageName, Long electionId, Long id);

    boolean existsByLanguageNameAndElectionIdAndAccountId(String languageName, Long electionId, Long accountId);

    List<Language> findByAccountIdAndElectionId(Long accountId, Long electionId);

    Optional<Language> findByAccountIdAndElectionIdAndId(Long accountId, Long electionId, Long id);
    
    Optional<Language> findByLanguageNameAndAccountIdAndElectionId(String languageName,Long accountId, Long electionId);
    
    @Query("SELECT MIN(l.orderIndex) FROM Language l WHERE l.electionId = :electionId")
    Integer findMinOrderIndexByElectionId(@Param("electionId") Long electionId);
    

    @Query("SELECT MAX(l.orderIndex) FROM Language l WHERE l.electionId = :electionId")
    Integer findMaxOrderIndexByElectionId(@Param("electionId") Long electionId);

    List<Language> findByElectionIdAndAccountIdOrderByOrderIndexAsc(Long electionId, Long accountId);

	List<Language> findByAccountIdAndElectionIdOrderByOrderIndexAsc(Long accountId, Long electionId);

	List<Language> findByElectionIdOrderByOrderIndexAsc(Long electionId);

	Optional<Language> findByIdAndAccountIdAndElectionId(Long id, Long accountId, Long electionId);

	int deleteByAccountIdAndElectionId(Long accountId, Long electionId);

	//int deleteByAccountIdAndElectionIdAndIds(Long accountId, Long electionId, List<Long> languageIds);
	@Modifying
    @Query("DELETE FROM Language l WHERE l.accountId = :accountId AND l.electionId = :electionId AND l.id IN :languageIds")
    int deleteByAccountIdAndElectionIdAndIds(@Param("accountId") Long accountId, 
                                             @Param("electionId") Long electionId, 
                                             @Param("languageIds") List<Long> languageIds);
    
    // Add count method for migration validation and stats
    long countByAccountIdAndElectionId(Long accountId, Long electionId);
    
    List<Language> findByIdInAndAccountIdAndElectionId(List<Long> ids, Long accountId, Long electionId);

    List<Language> findByAccountIdAndElectionIdOrderByUpdatedAtDescCreatedAtDesc(Long accountId, Long electionId);

    @Query("SELECT l, COUNT(v.id) as voterCount " +
            "FROM Language l " +
            "LEFT JOIN l.voters v " +
            "WHERE l.accountId = :accountId AND l.electionId = :electionId " +
            "GROUP BY l")
     List<Object[]> findLanguagesWithVoterCount(Long accountId, Long electionId);
     
     @Query("SELECT l.id, COUNT(v) FROM Language l LEFT JOIN l.voters v WHERE l.id IN :languageIds AND l.accountId = :accountId AND l.electionId = :electionId GROUP BY l.id")
     List<Object[]> findVoterCountsByLanguageIds(@Param("accountId") Long accountId, @Param("electionId") Long electionId, @Param("languageIds") List<Long> languageIds);

     default Map<Long, Long> getVoterCountsByLanguageIds(Long accountId, Long electionId, List<Long> languageIds) {
         return findVoterCountsByLanguageIds(accountId, electionId, languageIds).stream()
                 .collect(Collectors.toMap(
                         result -> ((Number) result[0]).longValue(),
                         result -> ((Number) result[1]).longValue()
                 ));
     }
	
}
