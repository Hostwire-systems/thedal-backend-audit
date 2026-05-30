package com.thedal.thedal_app.voter;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DynamicFieldMappingRepository extends JpaRepository<DynamicFieldMapping, Long> {

//    @Query("SELECT d FROM DynamicFieldMapping d WHERE d.accountId = :accountId AND d.electionId = :electionId")
//    List<DynamicFieldMapping> findByAccountIdAndElectionId(@Param("accountId") Long accountId, @Param("electionId") Long electionId);

	Optional<DynamicFieldMapping> findByFieldNameAndAccountIdAndElectionId(String fieldName, Long accountId,
			Long electionId);

	@Query("SELECT MAX(CAST(SUBSTRING(dfm.columnName, 7) AS int)) FROM DynamicFieldMapping dfm WHERE dfm.accountId = :accountId AND dfm.electionId = :electionId")
    Integer findMaxColumnNumber(Long accountId, Long electionId);

	DynamicFieldMapping findByAccountIdAndElectionIdAndFieldName(Long accountId, Long electionId, String fieldName);

}