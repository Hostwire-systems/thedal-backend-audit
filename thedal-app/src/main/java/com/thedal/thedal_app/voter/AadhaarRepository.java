package com.thedal.thedal_app.voter;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AadhaarRepository extends JpaRepository<AadhaarEntity, Long> {

	Optional<AadhaarEntity> findByIdAndElectionIdAndAccountId(Long id, Long electionId, Long accountId);

    void deleteByIdAndElectionIdAndAccountId(Long id, Long electionId, Long accountId);

	List<AadhaarEntity> findAllByElectionIdAndAccountId(Long electionId, Long accountId);
	@Query(value = "SELECT * FROM aadhaar_data " +
               "WHERE aadhaar_data ->> 'aadhaarNumber' = :aadhaarNumber " +
               "AND election_id = :electionId " +
               "AND account_id = :accountId " +
               "LIMIT 1", nativeQuery = true)
	Optional<AadhaarEntity> findByAadhaarNumberInJson(@Param("aadhaarNumber") String aadhaarNumber,
                                                  @Param("electionId") Long electionId,
                                                  @Param("accountId") Long accountId);

}
