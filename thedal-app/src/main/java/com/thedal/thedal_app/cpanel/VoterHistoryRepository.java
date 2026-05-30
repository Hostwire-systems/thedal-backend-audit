package com.thedal.thedal_app.cpanel;


import com.thedal.thedal_app.cpanel.dtos.VoterHistoryEntity;

import org.springframework.data.domain.Sort.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.util.Streamable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository 
public interface VoterHistoryRepository extends JpaRepository<VoterHistoryEntity, Long>{

    Optional<VoterHistoryEntity> findByVoterHistoryNameAndAccountIdAndElectionId(String voterHistoryName, Long accountId, Long electionId);
    List<VoterHistoryEntity> findByAccountIdAndElectionId(Long accountId, Long electionId);
    Optional<VoterHistoryEntity> findByIdAndAccountIdAndElectionId(Long id, Long accountId, Long electionId);
    List<VoterHistoryEntity> findByIdInAndAccountIdAndElectionId(List<Long> ids, Long accountId, Long electionId);
    @Query("SELECT COALESCE(MAX(v.orderIndex), 0) FROM VoterHistoryEntity v WHERE v.electionId = :electionId")
    Integer findMaxOrderIndexByElectionId(@Param("electionId") Long electionId);
    List<VoterHistoryEntity> findByElectionIdAndAccountIdOrderByOrderIndexAsc(Long electionId, Long accountId);
    @Query("SELECT MIN(v.orderIndex) FROM VoterHistoryEntity v WHERE v.electionId = :electionId")
Integer findMinOrderIndexByElectionId(@Param("electionId") Long electionId);
List<VoterHistoryEntity> findByAccountIdAndElectionIdOrderByOrderIndexAsc(Long accountId, Long electionId);


@Query("SELECT vh, COUNT(v.id) as voterCount " +
        "FROM VoterHistoryEntity vh " +
        "LEFT JOIN vh.voters v " +
        "WHERE vh.accountId = :accountId AND vh.electionId = :electionId " +
        "GROUP BY vh")
 List<Object[]> findVoterHistoriesWithVoterCount(Long accountId, Long electionId);
 
 @Query("SELECT vh FROM VoterHistoryEntity vh WHERE vh.accountId = :accountId AND vh.electionId = :electionId AND EXISTS " +
         "(SELECT 1 FROM vh.voters v WHERE v.electionId = :electionId)")
  List<VoterHistoryEntity> findLinkedHistories(@Param("accountId") Long accountId, @Param("electionId") Long electionId);

  @Query("SELECT vh FROM VoterHistoryEntity vh WHERE vh.id IN :voterHistoryIds AND vh.accountId = :accountId AND vh.electionId = :electionId AND EXISTS " +
         "(SELECT 1 FROM vh.voters v WHERE v.electionId = :electionId)")
  List<VoterHistoryEntity> findLinkedHistoriesByIds(@Param("voterHistoryIds") List<Long> voterHistoryIds, @Param("accountId") Long accountId, @Param("electionId") Long electionId);
  

@Query("SELECT vh FROM VoterHistoryEntity vh WHERE LOWER(vh.voterHistoryName) IN :names AND vh.accountId = :accountId AND vh.electionId = :electionId")
List<VoterHistoryEntity> findByVoterHistoryNamesAndAccountIdAndElectionId(@Param("names") List<String> names, @Param("accountId") Long accountId, @Param("electionId") Long electionId);
}
