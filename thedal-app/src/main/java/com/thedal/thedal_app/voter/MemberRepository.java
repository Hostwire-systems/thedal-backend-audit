package com.thedal.thedal_app.voter;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.thedal.thedal_app.election.ElectionEntity;

import jakarta.transaction.Transactional;

public interface MemberRepository extends JpaRepository<MemberEntity, Long>{

	Optional<MemberEntity> findByMembershipNoAndElectionIdAndAccountId(String membershipNo, Long electionId,
			Long accountId);

	List<MemberEntity> findByElectionIdAndAccountId(Long electionId, Long accountId);

	Optional<MemberEntity> findByElectionIdAndIdAndAccountId(Long electionId, Long id, Long accountId);

//	 int deleteByElectionIdAndAccountId(Long electionId, Long accountId);
//	 int deleteByElectionIdAndAccountIdAndIdIn(Long electionId, Long accountId, List<Long> memberIds);
	@Modifying
    @Transactional
    @Query("DELETE FROM MemberEntity m WHERE m.electionId = :electionId AND m.accountId = :accountId")
    int deleteByElectionIdAndAccountId(@Param("electionId") Long electionId, @Param("accountId") Long accountId);

    @Modifying
    @Transactional
    @Query("DELETE FROM MemberEntity m WHERE m.electionId = :electionId AND m.accountId = :accountId AND m.id IN :memberIds")
    int deleteByElectionIdAndAccountIdAndIdIn(@Param("electionId") Long electionId, @Param("accountId") Long accountId, @Param("memberIds") List<Long> memberIds);

    Optional<MemberEntity> findByElectionIdAndMembershipNoAndAccountId(Long electionId, String membershipNo, Long accountId);

	List<MemberEntity> findByElectionIdAndAccountIdAndEpicNumber(Long electionId, Long accountId, String epicNumber);

	Optional<MemberEntity> findByElectionIdAndMembershipNoAndAccountIdAndEpicNumber(Long electionId,
			String membershipNo, Long accountId, String epicNumber);

	Optional<MemberEntity> findByElectionIdAndEpicNumberAndAccountId(Long electionId, String epicNumber,
			Long accountId);

	Optional<MemberEntity> findByMobileNumberAndElectionIdAndAccountId(String mobileNumber, Long electionId, Long accountId);
	

}
